package com.teste.bank.fixture;

import java.math.BigDecimal;

public final class Amounts {

    public static final BigDecimal ZERO = new BigDecimal("0.00");
    public static final BigDecimal SMALL = new BigDecimal("10.00");
    public static final BigDecimal TRANSFER = new BigDecimal("30.00");
    public static final BigDecimal BALANCE = new BigDecimal("100.00");
    public static final BigDecimal NEGATIVE = new BigDecimal("-1.00");
    public static final BigDecimal THREE_DECIMALS = new BigDecimal("10.123");
    public static final BigDecimal ONE_DECIMAL = new BigDecimal("10.5");
    public static final BigDecimal ONE_DECIMAL_NORMALIZED = new BigDecimal("10.50");

    private Amounts() {
    }
}
