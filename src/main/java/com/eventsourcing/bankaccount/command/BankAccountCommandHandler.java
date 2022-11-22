package com.eventsourcing.bankaccount.command;

import com.eventsourcing.bankaccount.domain.BankAccountAggregate;
import com.eventsourcing.bankaccount.es.EventStoreDB;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class BankAccountCommandHandler implements BankAccountCommandService {

    private final EventStoreDB eventStoreDB;
    private static final String SERVICE_NAME = "microservice";

    @Override
    @NewSpan
    @Retry(name = SERVICE_NAME)
    @CircuitBreaker(name = SERVICE_NAME)
    public String handle(@SpanTag("command") CreateBankAccountCommand command) {
        final BankAccountAggregate aggregate = new BankAccountAggregate(command.getAggregateID());
        aggregate.createBankAccount(command.getEmail(), command.getAddress(), command.getUsername());
        eventStoreDB.save(aggregate);

        log.info("(CreateBankAccountCommand) aggregate: {}", aggregate);
        return aggregate.getId();
    }

    @Override
    @NewSpan
    @Retry(name = SERVICE_NAME)
    @CircuitBreaker(name = SERVICE_NAME)
    public void handle(@SpanTag("command") DepositAmountCommand command) {
        final BankAccountAggregate aggregate = eventStoreDB.load(command.getAggregateId(), BankAccountAggregate.class);
        aggregate.depositBalance(command.getAmount());
        eventStoreDB.save(aggregate);
        log.info("(DepositAmountCommand) aggregate: {}", aggregate);
    }
}
