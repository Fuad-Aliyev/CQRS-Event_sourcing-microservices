package com.eventsourcing.bankaccount.queries;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetBankAccountByIDQuery {
    private String aggregateId;
}
