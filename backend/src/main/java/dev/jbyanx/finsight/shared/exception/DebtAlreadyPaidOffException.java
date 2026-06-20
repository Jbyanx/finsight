package dev.jbyanx.finsight.shared.exception;

public class DebtAlreadyPaidOffException extends RuntimeException {
    public DebtAlreadyPaidOffException(String institution) {
        super("Debt with " + institution + " is already paid off");
    }
}