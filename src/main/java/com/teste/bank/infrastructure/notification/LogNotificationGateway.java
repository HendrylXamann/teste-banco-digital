package com.teste.bank.infrastructure.notification;

import com.teste.bank.domain.gateway.NotificationGateway;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogNotificationGateway implements NotificationGateway {

    private static final String RESILIENCE_INSTANCE = "notification";
    private static final String SIMULATED_FAILURE_MESSAGE = "Falha simulada no provedor de notificação";

    private final double simulatedFailureRate;

    public LogNotificationGateway(@Value("${notification.simulated-failure-rate:0.0}") double simulatedFailureRate) {
        this.simulatedFailureRate = simulatedFailureRate;
    }

    @Override
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "notifyFallback")
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public void notify(Long accountId, String message) {
        if (ThreadLocalRandom.current().nextDouble() < simulatedFailureRate) {
            throw new NotificationDeliveryException(SIMULATED_FAILURE_MESSAGE);
        }
        log.info("Notificação enviada para a conta {}: {}", accountId, message);
    }

    public void notifyFallback(Long accountId, String message, Throwable cause) {
        log.warn("Notificação para a conta {} não entregue: {}", accountId, cause.getMessage());
    }
}
