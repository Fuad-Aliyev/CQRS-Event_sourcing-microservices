package com.eventsourcing.bankaccount.domain;

import com.eventsourcing.bankaccount.es.AggregateRoot;
import com.eventsourcing.bankaccount.es.Event;
import com.eventsourcing.bankaccount.es.SerializerUtils;
import com.eventsourcing.bankaccount.es.exceptions.InvalidEventTypeException;
import com.eventsourcing.events.BalanceDepositedEvent;
import com.eventsourcing.events.BankAccountCreatedEvent;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BankAccountAggregate extends AggregateRoot {

    public static final String AGGREGATE_TYPE = "BankAccountAggregate";

    public BankAccountAggregate(String id) {
        super(id, AGGREGATE_TYPE);
    }

    private String email;
    private String userName;
    private String address;
    private BigDecimal balance;

    @Override
    public void when(Event event) {
        switch (event.getEventType()) {
            case BankAccountCreatedEvent.BANK_ACCOUNT_CREATED_V1 :
                    handle(SerializerUtils.deserializeFromJsonBytes(event.getData(), BankAccountCreatedEvent.class));
            case BalanceDepositedEvent.BALANCE_DEPOSITED :
                    handle(SerializerUtils.deserializeFromJsonBytes(event.getData(), BalanceDepositedEvent.class));
            default : throw new InvalidEventTypeException(event.getEventType());
        }
    }

    private void handle(final BankAccountCreatedEvent event) {
        this.email = event.getEmail();
        this.userName = event.getUserName();
        this.address = event.getAddress();
        this.balance = BigDecimal.valueOf(0);
    }

    private void handle(final BalanceDepositedEvent event) {
        Objects.requireNonNull(event.getAmount());
        this.balance = this.balance.add(event.getAmount());
    }

    public void createBankAccount(String email, String address, String userName) {
        final BankAccountCreatedEvent data = BankAccountCreatedEvent.builder()
                .aggregateId(id)
                .email(email)
                .address(address)
                .userName(userName)
                .build();

        final byte[] dataBytes = SerializerUtils.serializeToJsonBytes(data);
        final Event event = this.createEvent(BankAccountCreatedEvent.BANK_ACCOUNT_CREATED_V1, dataBytes, null);
        this.apply(event);
    }

    public void depositBalance(BigDecimal amount) {
        final BalanceDepositedEvent data = BalanceDepositedEvent.builder().aggregateId(id).amount(amount).build();
        final byte[] dataBytes = SerializerUtils.serializeToJsonBytes(data);
        final Event event = this.createEvent(BalanceDepositedEvent.BALANCE_DEPOSITED, dataBytes, null);
        apply(event);
    }
}
