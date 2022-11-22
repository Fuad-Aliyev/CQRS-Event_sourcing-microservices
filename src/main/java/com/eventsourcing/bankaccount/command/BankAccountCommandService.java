package com.eventsourcing.bankaccount.command;

public interface BankAccountCommandService {
    String handle(CreateBankAccountCommand command);

    void handle(DepositAmountCommand command);
}
