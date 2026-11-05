package com.teste.bank.application.service;

import com.teste.bank.domain.model.Notification;
import org.springframework.data.domain.Page;

public interface NotificationQueryService {

    Page<Notification> findByAccount(Long accountId, int page, int size);
}
