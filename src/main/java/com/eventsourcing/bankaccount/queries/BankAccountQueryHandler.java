package com.eventsourcing.bankaccount.queries;

import com.eventsourcing.bankaccount.domain.BankAccountAggregate;
import com.eventsourcing.bankaccount.domain.BankAccountDocument;
import com.eventsourcing.bankaccount.dto.BankAccountResponseDTO;
import com.eventsourcing.bankaccount.es.EventStoreDB;
import com.eventsourcing.bankaccount.mappers.BankAccountMapper;
import com.eventsourcing.bankaccount.repository.BankAccountMongoRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class BankAccountQueryHandler implements BankAccountQueryService {
    private final EventStoreDB eventStoreDB;
    private final BankAccountMongoRepository mongoRepository;
    private static final String SERVICE_NAME = "microservice";

    @Override
    @NewSpan
    @Retry(name = SERVICE_NAME)
    @CircuitBreaker(name = SERVICE_NAME)
    public BankAccountResponseDTO handle(@SpanTag("query") GetBankAccountByIDQuery query) {
        Optional<BankAccountDocument> optionalDocument = mongoRepository.findByAggregateId(query.getAggregateId());
        if (optionalDocument.isPresent()) {
            return BankAccountMapper.bankAccountResponseDTOFromDocument(optionalDocument.get());
        }
        final BankAccountAggregate aggregate = eventStoreDB.load(query.getAggregateId(), BankAccountAggregate.class);
        final BankAccountDocument savedDocument = mongoRepository.save(BankAccountMapper.bankAccountDocumentFromAggregate(aggregate));
        log.info("(GetBankAccountByIDQuery) savedDocument: {}", savedDocument);

        final BankAccountResponseDTO bankAccountResponseDTO = BankAccountMapper.bankAccountResponseDTOFromAggregate(aggregate);
        log.info("(GetBankAccountByIDQuery) response: {}", bankAccountResponseDTO);
        return bankAccountResponseDTO;
    }
}
