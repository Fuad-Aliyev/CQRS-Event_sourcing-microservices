package com.eventsourcing.events;

import com.eventsourcing.bankaccount.domain.BankAccountAggregate;
import com.eventsourcing.bankaccount.es.BaseEvent;
import lombok.Builder;
import lombok.Data;

@Data
public class BankAccountCreatedEvent extends BaseEvent {
    public static final String BANK_ACCOUNT_CREATED_V1 = "BANK_ACCOUNT_CREATED_V1";
    public static final String AGGREGATE_TYPE = BankAccountAggregate.AGGREGATE_TYPE;

    private String email;
    private String userName;
    private String address;

    @Builder
    public BankAccountCreatedEvent(String aggregateId, String email, String userName, String address) {
        super(aggregateId);
        this.email = email;
        this.userName = userName;
        this.address = address;
    }
}
