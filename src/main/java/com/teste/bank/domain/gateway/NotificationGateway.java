package com.teste.bank.domain.gateway;

public interface NotificationGateway {

    void notify(Long accountId, String message);
}
