package dev.jbyanx.finsight.shared.exception;

public class PaymentExceedsBalanceException extends RuntimeException {
    public PaymentExceedsBalanceException(String remaining) {
        super("Payment amount exceeds remaining balance: " + remaining);
    }
}