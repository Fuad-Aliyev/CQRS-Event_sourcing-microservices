package com.eventsourcing.bankaccount.exceptions;

public class BankAccountDocumentNotFoundException extends RuntimeException {
    public BankAccountDocumentNotFoundException() {
    }

    public BankAccountDocumentNotFoundException(String id) {
        super("bank account document not found id:" + id);
    }
}
