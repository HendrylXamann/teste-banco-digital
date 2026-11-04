package com.teste.bank.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    ACCOUNT_NOT_FOUND("Conta não encontrada", "Conta não encontrada: %d"),
    INSUFFICIENT_BALANCE("Saldo insuficiente", "Saldo insuficiente na conta: %d"),
    SAME_ACCOUNT_TRANSFER("Transferência inválida", "Conta de origem e destino devem ser diferentes"),
    INVALID_AMOUNT("Transferência inválida", "O valor da transferência deve ser maior que zero"),
    INVALID_FIELDS("Requisição inválida", "Um ou mais campos são inválidos"),
    MALFORMED_REQUEST("Requisição inválida", "O corpo da requisição está ausente ou mal formatado"),
    INVALID_PARAMETER("Requisição inválida", "Parâmetro com valor inválido: %s"),
    MISSING_HEADER("Requisição inválida", "Header obrigatório ausente: %s"),
    IDEMPOTENCY_KEY_CONFLICT("Conflito de idempotência",
            "Esta Idempotency-Key está sendo processada por outra requisição, tente novamente"),
    ACCOUNT_LOCKED("Conta ocupada", "A conta está sendo movimentada por outra operação, tente novamente"),
    DATA_INTEGRITY_VIOLATION("Violação de integridade", "A operação viola uma regra de integridade dos dados"),
    DATABASE_UNAVAILABLE("Serviço indisponível",
            "Não foi possível acessar os dados no momento, tente novamente em instantes"),
    INTERNAL_ERROR("Erro interno", "Ocorreu um erro no servidor, tente novamente mais tarde");

    private final String title;
    private final String messageTemplate;

    public String message(Object... args) {
        return messageTemplate.formatted(args);
    }
}
