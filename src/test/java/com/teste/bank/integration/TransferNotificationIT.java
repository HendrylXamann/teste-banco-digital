package com.teste.bank.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.teste.bank.api.dto.NotificationResponse;
import com.teste.bank.api.dto.PageResponse;
import com.teste.bank.api.dto.TransferResponse;
import com.teste.bank.application.service.AccountService;
import com.teste.bank.domain.model.Notification;
import com.teste.bank.domain.model.NotificationStatus;
import com.teste.bank.domain.model.TransferDirection;
import com.teste.bank.domain.repository.NotificationRepository;
import com.teste.bank.fixture.AccountFixture;
import com.teste.bank.fixture.Amounts;
import com.teste.bank.fixture.TransferFixture;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransferNotificationIT {

    private static final String TRANSFERS_PATH = "/transfers";
    private static final String NOTIFICATIONS_PATH = "/accounts/{id}/notifications";
    private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration QUIET_PERIOD = Duration.ofSeconds(1);
    private static final ParameterizedTypeReference<PageResponse<NotificationResponse>> NOTIFICATION_PAGE =
            new ParameterizedTypeReference<>() {
            };

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountService accountService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void shouldPersistNotificationForPayerAndReceiverAfterCommit() {
        var source = createAccount(Amounts.BALANCE);
        var destination = createAccount(Amounts.ZERO);

        var transferId = transfer(source, destination, Amounts.TRANSFER).id();

        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            var notifications = notificationsOfTransfer(transferId);
            assertThat(notifications).extracting(Notification::getAccountId)
                    .containsExactlyInAnyOrder(source, destination);
            assertThat(notifications).allMatch(n -> n.getStatus() == NotificationStatus.SENT);
        });
    }

    @Test
    void shouldExposePersistedNotificationThroughApi() {
        var source = createAccount(Amounts.BALANCE);
        var destination = createAccount(Amounts.ZERO);

        var transferId = transfer(source, destination, Amounts.TRANSFER).id();

        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            var page = notificationPage(source);
            assertThat(page.totalElements()).isEqualTo(1);
            assertThat(page.content().getFirst().transferId()).isEqualTo(transferId);
            assertThat(page.content().getFirst().direction()).isEqualTo(TransferDirection.DEBIT);
        });
    }

    @Test
    void shouldNotNotifyWhenTransferIsRolledBack() {
        var source = createAccount(Amounts.ZERO);
        var destination = createAccount(Amounts.ZERO);

        var status = rejectedTransferStatus(source, destination, Amounts.TRANSFER);

        assertThat(status).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        await().during(QUIET_PERIOD).atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            assertThat(notificationPage(source).totalElements()).isZero();
            assertThat(notificationPage(destination).totalElements()).isZero();
        });
    }

    private Long createAccount(BigDecimal balance) {
        return accountService.create(AccountFixture.NAME, balance).getId();
    }

    private TransferResponse transfer(Long from, Long to, BigDecimal amount) {
        return restTemplate.exchange(transferRequest(from, to, amount), TransferResponse.class).getBody();
    }

    private int rejectedTransferStatus(Long from, Long to, BigDecimal amount) {
        return restTemplate.exchange(transferRequest(from, to, amount), String.class).getStatusCode().value();
    }

    private RequestEntity<?> transferRequest(Long from, Long to, BigDecimal amount) {
        return RequestEntity.post(TRANSFERS_PATH)
                .header(TransferFixture.IDEMPOTENCY_HEADER, TransferFixture.randomIdempotencyKey())
                .body(TransferFixture.request(from, to, amount));
    }

    private PageResponse<NotificationResponse> notificationPage(Long accountId) {
        var request = RequestEntity.get(NOTIFICATIONS_PATH, accountId).build();
        return restTemplate.exchange(request, NOTIFICATION_PAGE).getBody();
    }

    private List<Notification> notificationsOfTransfer(Long transferId) {
        return notificationRepository.findAll().stream()
                .filter(notification -> notification.getTransferId().equals(transferId))
                .toList();
    }
}
