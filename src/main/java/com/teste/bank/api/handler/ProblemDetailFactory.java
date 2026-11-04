package com.teste.bank.api.handler;

import com.teste.bank.domain.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ProblemDetailFactory {

    private static final String CODE_PROPERTY = "code";

    private ProblemDetailFactory() {
    }

    public static ProblemDetail create(ErrorCode errorCode, String detail) {
        return decorate(ProblemDetail.forStatus(statusOf(errorCode)), errorCode, detail);
    }

    public static ProblemDetail create(ErrorCode errorCode) {
        return create(errorCode, errorCode.message());
    }

    public static ProblemDetail decorate(ProblemDetail problemDetail, ErrorCode errorCode, String detail) {
        problemDetail.setTitle(errorCode.getTitle());
        problemDetail.setDetail(detail);
        problemDetail.setProperty(CODE_PROPERTY, errorCode.name());
        return problemDetail;
    }

    public static HttpStatus statusOf(ErrorCode errorCode) {
        return switch (errorCode) {
            case ACCOUNT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case SAME_ACCOUNT_TRANSFER, INVALID_AMOUNT, INVALID_FIELDS,
                 MALFORMED_REQUEST, INVALID_PARAMETER, MISSING_HEADER -> HttpStatus.BAD_REQUEST;
            case INSUFFICIENT_BALANCE, DATA_INTEGRITY_VIOLATION -> HttpStatus.UNPROCESSABLE_ENTITY;
            case IDEMPOTENCY_KEY_CONFLICT, ACCOUNT_LOCKED -> HttpStatus.CONFLICT;
            case DATABASE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
