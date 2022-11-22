package com.eventsourcing.bankaccount.configuration;

import com.eventsourcing.bankaccount.domain.BankAccountDocument;
import com.mongodb.client.MongoCollection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;

import javax.annotation.PostConstruct;
import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class MongoConfiguration {
    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void mongoInit() {
        final MongoCollection<Document> bankAccounts = mongoTemplate.getCollection("bankAccounts");
        final String aggregateId = mongoTemplate.indexOps(BankAccountDocument.class).ensureIndex(new Index("aggregateId", Sort.Direction.ASC).unique());
        final List<IndexInfo> indexInfo = mongoTemplate.indexOps(BankAccountDocument.class).getIndexInfo();
        log.info("MongoDB connected, bankAccounts aggregateId index created: {}", indexInfo);
    }
}
