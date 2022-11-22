package com.eventsourcing.bankaccount.command;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateBankAccountCommand {
    private String aggregateID;
    private String email;
    private String username;
    private String address;
}
