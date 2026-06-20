package dev.jbyanx.finsight.shared.exception;

public class InvalidCreditLimitException extends RuntimeException {
    public InvalidCreditLimitException(String remaining) {
        super("Payment amount exceeds remaining balance: " + remaining);
    }
}