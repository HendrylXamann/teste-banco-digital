package com.teste.bank.application.service;

import com.teste.bank.domain.model.Account;
import java.math.BigDecimal;

public interface AccountService {

    Account create(String name, BigDecimal initialBalance);

    Account findById(Long id);
}
