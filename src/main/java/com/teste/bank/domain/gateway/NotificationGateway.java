package com.teste.bank.domain.gateway;

public interface NotificationGateway {

    boolean notify(Long accountId, String message);
}
