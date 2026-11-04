package com.teste.bank.api.handler;

import com.teste.bank.domain.exception.AccountNotFoundException;
import com.teste.bank.domain.exception.InsufficientBalanceException;
import com.teste.bank.domain.exception.InvalidTransferException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound(AccountNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Conta não encontrada", ex.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ProblemDetail handleInsufficientBalance(InsufficientBalanceException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Saldo insuficiente", ex.getMessage());
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ProblemDetail handleInvalidTransfer(InvalidTransferException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Transferência inválida", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Conflito",
                "A operação conflita com outra já registrada, ex.: Idempotency-Key em uso simultâneo");
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ProblemDetail handleLockFailure(PessimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Conta ocupada",
                "A conta está sendo movimentada por outra operação, tente novamente");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        ex.getBody().setDetail("Um ou mais campos são inválidos");
        ex.getBody().setProperty("errors", fieldErrors(ex));
        return super.handleMethodArgumentNotValid(ex, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            @NonNull HandlerMethodValidationException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        ex.getBody().setDetail("Um ou mais campos são inválidos");
        ex.getBody().setProperty("errors", parameterErrors(ex));
        return super.handleHandlerMethodValidationException(ex, headers, status, request);
    }

    private Map<String, String> parameterErrors(HandlerMethodValidationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> collectErrors(result, errors));
        return errors;
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

    private Map<String, String> fieldErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> String.valueOf(error.getDefaultMessage()),
                        (first, second) -> first));
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        return problemDetail;
    }
}
