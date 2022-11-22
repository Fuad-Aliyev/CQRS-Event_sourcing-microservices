package com.eventsourcing.bankaccount.repository;

import com.eventsourcing.bankaccount.domain.BankAccountDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface BankAccountMongoRepository extends MongoRepository<BankAccountDocument, String> {
    Optional<BankAccountDocument> findByAggregateId(String aggregateId);
    void deleteByAggregateId(String aggregateId);
}
