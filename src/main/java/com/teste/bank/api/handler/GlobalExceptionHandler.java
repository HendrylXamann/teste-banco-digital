package com.teste.bank.api.handler;

import com.teste.bank.domain.exception.BusinessException;
import com.teste.bank.domain.exception.ErrorCode;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String IDEMPOTENCY_CONSTRAINT = "uk_transfers_idempotency_key";
    private static final String ERRORS_PROPERTY = "errors";

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex) {
        return ProblemDetailFactory.create(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        String cause = String.valueOf(ex.getMostSpecificCause().getMessage());
        log.warn("Violação de integridade: {}", cause);
        return cause.contains(IDEMPOTENCY_CONSTRAINT)
                ? ProblemDetailFactory.create(ErrorCode.IDEMPOTENCY_KEY_CONFLICT)
                : ProblemDetailFactory.create(ErrorCode.DATA_INTEGRITY_VIOLATION);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ProblemDetail handleLockFailure(PessimisticLockingFailureException ex) {
        log.warn("Timeout ao bloquear conta: {}", ex.getMessage());
        return ProblemDetailFactory.create(ErrorCode.ACCOUNT_LOCKED);
    }

    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class})
    public ProblemDetail handleDatabaseUnavailable(Exception ex) {
        log.error("Banco de dados indisponível", ex);
        return ProblemDetailFactory.create(ErrorCode.DATABASE_UNAVAILABLE);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Erro não tratado", ex);
        return ProblemDetailFactory.create(ErrorCode.INTERNAL_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), String.valueOf(error.getDefaultMessage())));
        decorateInvalidFields(ex.getBody(), errors);
        return super.handleMethodArgumentNotValid(ex, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            @NonNull HandlerMethodValidationException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> collectErrors(result, errors));
        decorateInvalidFields(ex.getBody(), errors);
        return super.handleHandlerMethodValidationException(ex, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(
            @NonNull ServletRequestBindingException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        if (ex instanceof MissingRequestHeaderException missingHeader) {
            ErrorCode code = ErrorCode.MISSING_HEADER;
            ProblemDetailFactory.decorate(ex.getBody(), code, code.message(missingHeader.getHeaderName()));
        }
        return super.handleServletRequestBindingException(ex, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            @NonNull HttpMessageNotReadableException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        ProblemDetail body = ProblemDetailFactory.create(ErrorCode.MALFORMED_REQUEST);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            @NonNull TypeMismatchException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        ErrorCode code = ErrorCode.INVALID_PARAMETER;
        ProblemDetail body = ProblemDetailFactory.create(code, code.message(ex.getPropertyName()));
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    private void decorateInvalidFields(ProblemDetail body, Map<String, String> errors) {
        ErrorCode code = ErrorCode.INVALID_FIELDS;
        ProblemDetailFactory.decorate(body, code, code.message());
        body.setProperty(ERRORS_PROPERTY, errors);
    }

    private void collectErrors(ParameterValidationResult result, Map<String, String> errors) {
        if (result instanceof ParameterErrors parameterErrors) {
            parameterErrors.getFieldErrors().forEach(error ->
                    errors.putIfAbsent(error.getField(), String.valueOf(error.getDefaultMessage())));
            return;
        }
        String parameterName = String.valueOf(result.getMethodParameter().getParameterName());
        result.getResolvableErrors().forEach(error ->
                errors.putIfAbsent(parameterName, String.valueOf(error.getDefaultMessage())));
    }
}
