package com.teste.bank.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teste.bank.application.service.AccountService;
import com.teste.bank.domain.model.Account;
import com.teste.bank.domain.repository.AccountRepository;
import com.teste.bank.domain.repository.TransferRepository;
import com.teste.bank.fixture.AccountFixture;
import com.teste.bank.fixture.Amounts;
import com.teste.bank.fixture.TransferFixture;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransferConcurrencyIT {

    private static final int THREADS = 20;
    private static final String TRANSFERS_PATH = "/transfers";
    private static final int CROSSED_TRANSFERS = 100;
    private static final int COMPETING_DEBITS = 50;
    private static final int DUPLICATED_REQUESTS = 20;
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");
    private static final BigDecimal FORWARD_AMOUNT = new BigDecimal("10.00");
    private static final BigDecimal BACKWARD_AMOUNT = new BigDecimal("7.00");
    private static final String DISTINCT_ACCOUNTS_CONSTRAINT = "ck_transfers_distinct_accounts";
    private static final String NEGATIVE_BALANCE_CONSTRAINT = "ck_accounts_balance_non_negative";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Test
    void shouldKeepTotalBalanceConsistentUnderCrossedConcurrentTransfers() throws Exception {
        var first = createAccount(INITIAL_BALANCE);
        var second = createAccount(INITIAL_BALANCE);

        var statuses = runConcurrently(CROSSED_TRANSFERS, i -> i % 2 == 0
                ? postTransfer(first, second, FORWARD_AMOUNT, TransferFixture.randomIdempotencyKey())
                : postTransfer(second, first, BACKWARD_AMOUNT, TransferFixture.randomIdempotencyKey()));

        var net = FORWARD_AMOUNT.subtract(BACKWARD_AMOUNT).multiply(BigDecimal.valueOf(CROSSED_TRANSFERS / 2));
        assertThat(statuses).containsOnly(HttpStatus.CREATED.value());
        assertThat(balanceOf(first)).isEqualByComparingTo(INITIAL_BALANCE.subtract(net));
        assertThat(balanceOf(second)).isEqualByComparingTo(INITIAL_BALANCE.add(net));
        assertThat(balanceOf(first).add(balanceOf(second))).isEqualByComparingTo(INITIAL_BALANCE.add(INITIAL_BALANCE));
    }

    @Test
    void shouldNeverAllowNegativeBalanceUnderConcurrentDebits() throws Exception {
        var source = createAccount(Amounts.BALANCE);
        var destination = createAccount(Amounts.ZERO);
        var expectedSuccesses = Amounts.BALANCE.divide(Amounts.SMALL).intValue();

        var statuses = runConcurrently(COMPETING_DEBITS,
                i -> postTransfer(source, destination, Amounts.SMALL, TransferFixture.randomIdempotencyKey()));

        assertThat(statuses).filteredOn(s -> s == HttpStatus.CREATED.value()).hasSize(expectedSuccesses);
        assertThat(statuses).filteredOn(s -> s == HttpStatus.UNPROCESSABLE_ENTITY.value())
                .hasSize(COMPETING_DEBITS - expectedSuccesses);
        assertThat(balanceOf(source)).isEqualByComparingTo(Amounts.ZERO);
        assertThat(balanceOf(destination)).isEqualByComparingTo(Amounts.BALANCE);
    }

    @Test
    void shouldNotDuplicateTransferWithSameIdempotencyKeyInParallel() throws Exception {
        var source = createAccount(Amounts.BALANCE);
        var destination = createAccount(Amounts.ZERO);
        var key = TransferFixture.randomIdempotencyKey();

        var statuses = runConcurrently(DUPLICATED_REQUESTS,
                i -> postTransfer(source, destination, Amounts.TRANSFER, key));

        assertThat(statuses).filteredOn(s -> s == HttpStatus.CREATED.value()).hasSize(1);
        assertThat(statuses).allMatch(s -> s == HttpStatus.CREATED.value()
                || s == HttpStatus.OK.value() || s == HttpStatus.CONFLICT.value());
        assertThat(balanceOf(source)).isEqualByComparingTo(Amounts.BALANCE.subtract(Amounts.TRANSFER));
        assertThat(balanceOf(destination)).isEqualByComparingTo(Amounts.TRANSFER);
    }

    @Test
    void shouldRejectSameAccountTransferAtDatabaseLevel() {
        var account = createAccount(Amounts.BALANCE);
        var transfer = TransferFixture.transfer(null, account, account, Amounts.SMALL);

        assertThatThrownBy(() -> transferRepository.saveAndFlush(transfer))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().hasMessageContaining(DISTINCT_ACCOUNTS_CONSTRAINT);
    }

    @Test
    void shouldRejectNegativeBalanceAtDatabaseLevel() {
        var account = accountRepository.findById(createAccount(Amounts.ZERO)).orElseThrow();
        account.setBalance(Amounts.NEGATIVE);

        assertThatThrownBy(() -> accountRepository.saveAndFlush(account))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().hasMessageContaining(NEGATIVE_BALANCE_CONSTRAINT);
    }

    private Long createAccount(BigDecimal balance) {
        return accountService.create(AccountFixture.NAME, balance).getId();
    }

    private List<Integer> runConcurrently(int tasks, IntFunction<Integer> task) throws Exception {
        var executor = Executors.newFixedThreadPool(THREADS);
        var start = new CountDownLatch(1);
        var futures = IntStream.range(0, tasks)
                .mapToObj(index -> executor.submit(() -> {
                    start.await();
                    return task.apply(index);
                }))
                .toList();
        start.countDown();
        var statuses = new ArrayList<Integer>();
        for (var future : futures) {
            statuses.add(future.get(60, TimeUnit.SECONDS));
        }
        executor.shutdown();
        return statuses;
    }

    private int postTransfer(Long from, Long to, BigDecimal amount, String key) {
        var request = RequestEntity.post(TRANSFERS_PATH)
                .header(TransferFixture.IDEMPOTENCY_HEADER, key)
                .body(TransferFixture.request(from, to, amount));
        return restTemplate.exchange(request, String.class).getStatusCode().value();
    }

    private BigDecimal balanceOf(Long accountId) {
        return accountRepository.findById(accountId).map(Account::getBalance).orElseThrow();
    }
}
