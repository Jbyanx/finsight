package dev.jbyanx.finsight.shared.exception;

public class PaymentExceedsCardBalanceException extends RuntimeException {
    public PaymentExceedsCardBalanceException(String usedAmount) {
        super("Payment amount exceeds current used amount: " + usedAmount);
    }
}