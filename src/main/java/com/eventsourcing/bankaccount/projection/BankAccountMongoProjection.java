package com.eventsourcing.bankaccount.projection;

import com.eventsourcing.bankaccount.domain.BankAccountAggregate;
import com.eventsourcing.bankaccount.domain.BankAccountDocument;
import com.eventsourcing.bankaccount.es.Event;
import com.eventsourcing.bankaccount.es.EventStoreDB;
import com.eventsourcing.bankaccount.es.Projection;
import com.eventsourcing.bankaccount.es.SerializerUtils;
import com.eventsourcing.bankaccount.exceptions.BankAccountDocumentNotFoundException;
import com.eventsourcing.bankaccount.mappers.BankAccountMapper;
import com.eventsourcing.bankaccount.repository.BankAccountMongoRepository;
import com.eventsourcing.events.BalanceDepositedEvent;
import com.eventsourcing.events.BankAccountCreatedEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.adapter.ConsumerRecordMetadata;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BankAccountMongoProjection implements Projection {
    private final BankAccountMongoRepository mongoRepository;
    private final EventStoreDB eventStoreDB;
    private static final String SERVICE_NAME = "microservice";

    @KafkaListener(topics = {"${microservice.kafka.topics.bank-account-event-store}"},
            groupId = "${microservice.kafka.groupId}",
            concurrency = "${microservice.kafka.default-concurrency}")
    public void bankAccountMongoProjectionListener(@Payload byte[] data,
                                                   ConsumerRecordMetadata meta,
                                                   Acknowledgment ack) {
        log.info("(BankAccountMongoProjection) topic: {}, offset: {}, partition: {}, timestamp: {}, data: {}",
                meta.topic(), meta.offset(), meta.partition(), meta.timestamp(), new String(data));

        try {
            final Event[] events = SerializerUtils.deserializeEventsFromJsonBytes(data);
            this.processEvents(Arrays.stream(events).collect(Collectors.toList()));
            ack.acknowledge();
            log.info("ack events: {}", Arrays.toString(events));
        } catch (Exception ex) {
            ack.nack(100);
            log.error("(BankAccountMongoProjection) topic: {}, offset: {}, partition: {}, timestamp: {}",
                    meta.topic(), meta.offset(), meta.partition(), meta.timestamp(), ex);
        }
    }

    @NewSpan
    private void processEvents(@SpanTag("events") List<Event> events) {
        if (events.isEmpty()) return;

        try {
            events.forEach(this::when);
        } catch (Exception ex) {
            mongoRepository.deleteByAggregateId(events.get(0).getAggregateId());
            final BankAccountAggregate aggregate = eventStoreDB.load(events.get(0).getAggregateId(), BankAccountAggregate.class);
            final BankAccountDocument document = BankAccountMapper.bankAccountDocumentFromAggregate(aggregate);
            final BankAccountDocument result = mongoRepository.save(document);
            log.info("(processEvents) saved document: {}", result);
        }
    }

    @Override
    @NewSpan
    @Retry(name = SERVICE_NAME)
    @CircuitBreaker(name = SERVICE_NAME)
    public void when(@SpanTag("event") Event event) {
        final String aggregateId = event.getAggregateId();
        log.info("(when) >>>>> aggregateId: {}", aggregateId);

        switch (event.getEventType()) {
            case BankAccountCreatedEvent.BANK_ACCOUNT_CREATED_V1 :
                    handle(SerializerUtils.deserializeFromJsonBytes(event.getData(), BankAccountCreatedEvent.class));
            case BalanceDepositedEvent.BALANCE_DEPOSITED :
                        handle(SerializerUtils.deserializeFromJsonBytes(event.getData(), BalanceDepositedEvent.class));
            default : log.error("unknown event type: {}", event.getEventType());
        }
    }

    @NewSpan
    private void handle(@SpanTag("event") BankAccountCreatedEvent event) {
        log.info("(when) BankAccountCreatedEvent: {}, aggregateID: {}", event, event.getAggregateId());

        final BankAccountDocument document = BankAccountDocument.builder()
                .aggregateId(event.getAggregateId())
                .email(event.getEmail())
                .address(event.getAddress())
                .userName(event.getUserName())
                .balance(BigDecimal.valueOf(0))
                .build();

        final BankAccountDocument insert = mongoRepository.insert(document);
        log.info("(BankAccountCreatedEvent) insert: {}", insert);
    }

    @NewSpan
    private void handle(@SpanTag("event") BalanceDepositedEvent event) {
        log.info("(when) BalanceDepositedEvent: {}, aggregateID: {}", event, event.getAggregateId());
        final Optional<BankAccountDocument> documentOptional = mongoRepository.findByAggregateId(event.getAggregateId());
        if (!documentOptional.isPresent())
            throw new BankAccountDocumentNotFoundException(event.getAggregateId());

        final BankAccountDocument document = documentOptional.get();
        final BigDecimal newBalance = document.getBalance().add(event.getAmount());
        document.setBalance(newBalance);
        mongoRepository.save(document);
    }
}
