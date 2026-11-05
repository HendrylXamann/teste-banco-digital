package com.teste.bank.infrastructure.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teste.bank.fixture.AccountFixture;
import com.teste.bank.fixture.NotificationFixture;
import org.junit.jupiter.api.Test;

class LogNotificationGatewayTest {

    private static final double NEVER_FAILS = 0.0;
    private static final double ALWAYS_FAILS = 1.0;

    @Test
    void shouldReportDeliveryWhenProviderIsHealthy() {
        var gateway = new LogNotificationGateway(NEVER_FAILS);

        assertThat(gateway.notify(AccountFixture.SOURCE_ID, NotificationFixture.MESSAGE)).isTrue();
    }

    @Test
    void shouldThrowWhenProviderFails() {
        var gateway = new LogNotificationGateway(ALWAYS_FAILS);

        assertThatThrownBy(() -> gateway.notify(AccountFixture.SOURCE_ID, NotificationFixture.MESSAGE))
                .isInstanceOf(NotificationDeliveryException.class);
    }

    @Test
    void shouldReportFailureInFallbackInsteadOfThrowing() {
        var gateway = new LogNotificationGateway(ALWAYS_FAILS);
        var cause = new NotificationDeliveryException(NotificationFixture.MESSAGE);

        assertThat(gateway.notifyFallback(AccountFixture.SOURCE_ID, NotificationFixture.MESSAGE, cause)).isFalse();
    }
}
