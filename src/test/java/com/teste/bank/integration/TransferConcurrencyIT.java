package com.teste.bank.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.teste.bank.api.dto.TransferRequest;
import com.teste.bank.application.service.AccountService;
import com.teste.bank.domain.model.Account;
import com.teste.bank.domain.repository.AccountRepository;
import com.teste.bank.domain.repository.TransferRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransferConcurrencyIT {

    private static final int THREADS = 20;

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
    void shouldKeepTotalBalanceConsistentUnderConcurrentTransfers() throws Exception {
        Long first = accountService.create("Concorrência A", new BigDecimal("1000.00")).getId();
        Long second = accountService.create("Concorrência B", new BigDecimal("1000.00")).getId();

        List<Integer> statuses = runConcurrently(100, i -> i % 2 == 0
                ? postTransfer(first, second, "10.00", UUID.randomUUID().toString())
                : postTransfer(second, first, "7.00", UUID.randomUUID().toString()));

        assertThat(statuses).containsOnly(201);
        assertThat(balanceOf(first)).isEqualByComparingTo("850.00");
        assertThat(balanceOf(second)).isEqualByComparingTo("1150.00");
        assertThat(balanceOf(first).add(balanceOf(second))).isEqualByComparingTo("2000.00");
    }

    @Test
    void shouldNeverAllowNegativeBalanceUnderConcurrentDebits() throws Exception {
        Long source = accountService.create("Saldo curto", new BigDecimal("100.00")).getId();
        Long destination = accountService.create("Recebedor", BigDecimal.ZERO).getId();

        List<Integer> statuses = runConcurrently(50,
                i -> postTransfer(source, destination, "10.00", UUID.randomUUID().toString()));

        assertThat(statuses).filteredOn(status -> status == 201).hasSize(10);
        assertThat(statuses).filteredOn(status -> status == 422).hasSize(40);
        assertThat(balanceOf(source)).isEqualByComparingTo("0.00");
        assertThat(balanceOf(destination)).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldNotDuplicateTransferWithSameIdempotencyKeyInParallel() throws Exception {
        Long source = accountService.create("Idempotente A", new BigDecimal("500.00")).getId();
        Long destination = accountService.create("Idempotente B", BigDecimal.ZERO).getId();
        String key = UUID.randomUUID().toString();

        List<Integer> statuses = runConcurrently(20, i -> postTransfer(source, destination, "50.00", key));

        assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
        assertThat(statuses).allMatch(status -> status == 201 || status == 200 || status == 409);
        assertThat(transferRepository.findByIdempotencyKey(key)).isPresent();
        assertThat(balanceOf(source)).isEqualByComparingTo("450.00");
        assertThat(balanceOf(destination)).isEqualByComparingTo("50.00");
    }

    private List<Integer> runConcurrently(int tasks, IntFunction<Integer> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = IntStream.range(0, tasks)
                .mapToObj(index -> executor.submit(() -> {
                    start.await();
                    return task.apply(index);
                }))
                .toList();
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> future : futures) {
            statuses.add(future.get(60, TimeUnit.SECONDS));
        }
        executor.shutdown();
        return statuses;
    }

    private int postTransfer(Long from, Long to, String amount, String key) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", key);
        TransferRequest body = new TransferRequest(from, to, new BigDecimal(amount));
        return restTemplate.exchange("/transfers", HttpMethod.POST, new HttpEntity<>(body, headers), String.class)
                .getStatusCode()
                .value();
    }

    private BigDecimal balanceOf(Long accountId) {
        return accountRepository.findById(accountId).map(Account::getBalance).orElseThrow();
    }
}
