package com.teste.bank.application.service.impl;

import com.teste.bank.application.service.NotificationQueryService;
import com.teste.bank.domain.exception.AccountNotFoundException;
import com.teste.bank.domain.model.Notification;
import com.teste.bank.domain.repository.AccountRepository;
import com.teste.bank.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final AccountRepository accountRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> findByAccount(Long accountId, int page, int size) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        return notificationRepository.findByAccountId(accountId, PageRequests.newestFirst(page, size));
    }
}
