package com.teste.bank.application.service;

public interface TransferService {

    TransferResult transfer(TransferCommand command);
}
