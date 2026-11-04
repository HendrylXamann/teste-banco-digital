package com.teste.bank.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.teste.bank.fixture.AccountFixture;
import org.junit.jupiter.api.Test;

class ErrorCodeTest {

    @Test
    void shouldFormatMessageWithArguments() {
        assertThat(ErrorCode.ACCOUNT_NOT_FOUND.message(AccountFixture.UNKNOWN_ID))
                .isEqualTo("Conta não encontrada: " + AccountFixture.UNKNOWN_ID);
    }

    @Test
    void shouldExposeMessageThroughBusinessException() {
        var exception = new InsufficientBalanceException(AccountFixture.SOURCE_ID);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE.message(AccountFixture.SOURCE_ID));
    }
}
