package com.teste.bank.api.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.teste.bank.domain.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

class ProblemDetailFactoryTest {

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void shouldBuildProblemDetailForEveryErrorCode(ErrorCode errorCode) {
        var problem = ProblemDetailFactory.create(errorCode, errorCode.getMessageTemplate());

        assertThat(problem.getStatus()).isEqualTo(ProblemDetailFactory.statusOf(errorCode).value());
        assertThat(problem.getTitle()).isEqualTo(errorCode.getTitle());
        assertThat(problem.getProperties()).containsEntry("code", errorCode.name());
    }

    @Test
    void shouldMapBusinessAndInfrastructureCodesToExpectedStatus() {
        assertThat(ProblemDetailFactory.statusOf(ErrorCode.ACCOUNT_NOT_FOUND)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ProblemDetailFactory.statusOf(ErrorCode.INSUFFICIENT_BALANCE))
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ProblemDetailFactory.statusOf(ErrorCode.IDEMPOTENCY_KEY_CONFLICT)).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ProblemDetailFactory.statusOf(ErrorCode.DATABASE_UNAVAILABLE))
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ProblemDetailFactory.statusOf(ErrorCode.INTERNAL_ERROR))
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
