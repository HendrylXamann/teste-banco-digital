package com.teste.bank.domain.repository;

import com.teste.bank.domain.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByAccountId(Long accountId, Pageable pageable);
}
