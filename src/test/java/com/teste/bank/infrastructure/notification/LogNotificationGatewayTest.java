package com.teste.bank.infrastructure.notification;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LogNotificationGatewayTest {

    @Test
    void shouldSendWhenProviderIsHealthy() {
        var gateway = new LogNotificationGateway(0.0);

        assertThatCode(() -> gateway.notify(1L, "mensagem")).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowWhenProviderFails() {
        var gateway = new LogNotificationGateway(1.0);

        assertThatThrownBy(() -> gateway.notify(1L, "mensagem"))
                .isInstanceOf(NotificationDeliveryException.class);
    }

    @Test
    void shouldSwallowFailureInFallback() {
        var gateway = new LogNotificationGateway(1.0);

        assertThatCode(() -> gateway.notifyFallback(1L, "mensagem", new NotificationDeliveryException("falha")))
                .doesNotThrowAnyException();
    }
}
