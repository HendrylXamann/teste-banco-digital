package com.teste.bank.application.service;

import com.teste.bank.domain.model.Notification;

public interface NotificationService {

    Notification send(NotificationCommand command);
}
