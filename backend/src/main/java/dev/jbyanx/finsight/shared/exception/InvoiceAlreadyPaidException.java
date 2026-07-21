package dev.jbyanx.finsight.shared.exception;

public class InvoiceAlreadyPaidException extends RuntimeException {
    public InvoiceAlreadyPaidException(Integer periodMonth, Integer periodYear) {
        super("Invoice already paid for period " + periodMonth + "/" + periodYear);
    }
}
