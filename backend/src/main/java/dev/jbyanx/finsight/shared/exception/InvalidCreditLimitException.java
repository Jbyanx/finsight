package dev.jbyanx.finsight.shared.exception;

import java.math.BigDecimal;

public class InvalidCreditLimitException extends RuntimeException {
    public InvalidCreditLimitException(BigDecimal requestedLimit, BigDecimal usedAmount) {
        super("Credit limit cannot be less than the amount already used. Requested: "
                + requestedLimit + ". Currently used: " + usedAmount);
    }
}
