package com.teste.bank.application.service.impl;

import com.teste.bank.application.service.AccountService;
import com.teste.bank.domain.exception.AccountNotFoundException;
import com.teste.bank.domain.model.Account;
import com.teste.bank.domain.repository.AccountRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public Account create(String name, BigDecimal initialBalance) {
        var account = Account.builder()
                .name(name.trim())
                .balance(initialBalance.setScale(2, RoundingMode.HALF_EVEN))
                .createdAt(LocalDateTime.now())
                .build();
        return accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Account findById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}
