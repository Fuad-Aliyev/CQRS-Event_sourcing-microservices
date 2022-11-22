package com.eventsourcing.bankaccount.es;

import com.eventsourcing.bankaccount.es.exceptions.AggregateNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.eventsourcing.bankaccount.es.Constants.*;

@Repository
@RequiredArgsConstructor
@Slf4j
public class EventStore implements EventStoreDB {
    public static final int SNAPSHOT_FREQUENCY = 3;
    private static final String HANDLE_CONCURRENCY_QUERY = "SELECT aggregate_id FROM events e WHERE e.aggregate_id = :aggregate_id LIMIT 1 FOR UPDATE";
    private static final String SAVE_EVENTS_QUERY = "INSERT INTO events (aggregate_id, aggregate_type, event_type, data, metadata, version, timestamp) values (:aggregate_id, :aggregate_type, :event_type, :data, :metadata, :version, now())";
    private static final String SAVE_SNAPSHOT_QUERY = "INSERT INTO snapshots (aggregate_id, aggregate_type, data, metadata, version, timestamp) VALUES (:aggregate_id, :aggregate_type, :data, :metadata, :version, now()) ON CONFLICT (aggregate_id) DO UPDATE SET data = :data, version = :version, timestamp = now()";
    private static final String LOAD_SNAPSHOT_QUERY = "SELECT aggregate_id, aggregate_type, data, metadata, version, timestamp FROM snapshots s WHERE s.aggregate_id = :aggregate_id";
    private static final String LOAD_EVENTS_QUERY = "SELECT event_id ,aggregate_id, aggregate_type, event_type, data, metadata, version, timestamp FROM events e WHERE e.aggregate_id = :aggregate_id AND e.version > :version ORDER BY e.version ASC";


    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EventBus eventBus;

    @Override
    @NewSpan
    public void saveEvents(@SpanTag("events") List<Event> events) {
        if (events.isEmpty()) return;

        final List<Event> changes = new ArrayList<>(events);
        if (changes.size() > 1) {
            eventsBatchInsert(changes);
            return;
        }

        final Event event = changes.get(0);
        int result = 0;//jdbcTemplate.update(SAVE_EVENTS_QUERY, mapFromEvent(event));
        log.info("(saveEvents) saved result: {}, event: {}", result, event);
    }

    @Override
    public List<Event> loadEvents(String aggregateId, long version) {
        return null;//jdbcTemplate.query(LOAD_EVENTS_QUERY, new HashMap<>());
    }

    @Override
    @Transactional
    @NewSpan
    public <T extends AggregateRoot> void save(@SpanTag("aggregate") T aggregate) {
        final List<Event> aggregateEvents = new ArrayList<>(aggregate.getChanges());
        if (aggregate.getVersion() > 1) {
            this.handleConcurrency(aggregate.getId());
        }

        this.saveEvents(aggregate.getChanges());
        if (aggregate.getVersion() % SNAPSHOT_FREQUENCY == 0) {
            this.saveSnapshot(aggregate);
        }
        eventBus.publish(aggregateEvents);
        log.info("(save) saved aggregate: {}", aggregate);
    }

    @Override
    @Transactional(readOnly = true)
    @NewSpan
    public <T extends AggregateRoot> T load(@SpanTag("aggregateId") String aggregateId, @SpanTag("aggregateType") Class<T> aggregateType) {
        final Optional<Snapshot> snapshot = this.loadSnapshot(aggregateId);

        final T aggregate = this.getSnapshotFromClass(snapshot, aggregateId, aggregateType);

        final List<Event> events = this.loadEvents(aggregateId, aggregate.getVersion());
        events.forEach(event -> {
            aggregate.raiseEvent(event);
            log.info("raise event version: {}", event.getVersion());
        });

        if (aggregate.getVersion() == 0) throw new AggregateNotFoundException(aggregateId);

        log.info("(load) loaded aggregate: {}", aggregate);
        return aggregate;
    }

    @Override
    public Boolean exists(String aggregateId) {
        return null;
    }

    @NewSpan
    private void eventsBatchInsert(@SpanTag("events") List<Event> events) {
        final List<List<Map<String, ?>>> args = events.stream().map(this::mapFromEvent).collect(Collectors.toList());
        final Map<String, ?>[] maps = args.toArray(new Map[0]);
        int[] ints = jdbcTemplate.batchUpdate(SAVE_EVENTS_QUERY, maps);
        log.info("(saveEvents) BATCH saved result: {}, event: {}", ints);
    }

    private List<Map<String, ?>> mapFromEvent(Event event) {
        List<Map<String, ?>> list = new ArrayList<>();

        Map<String, String> aggregateId = new HashMap<>();
        aggregateId.put(AGGREGATE_ID, event.getAggregateId());
        list.add(aggregateId);

        Map<String, String> aggregateType = new HashMap<>();
        aggregateId.put(AGGREGATE_TYPE, event.getAggregateType());
        list.add(aggregateType);

        Map<String, String> eventType = new HashMap<>();
        aggregateId.put(EVENT_TYPE, event.getEventType());
        list.add(eventType);

        Map<String, byte[]> eventData = new HashMap<>();
        eventData.put(DATA, Objects.isNull(event.getData()) ? new byte[]{} : event.getData());
        list.add(eventData);

        Map<String, byte[]> eventMetaData = new HashMap<>();
        eventData.put(METADATA, Objects.isNull(event.getMetaData()) ? new byte[]{} : event.getMetaData());
        list.add(eventMetaData);

        Map<String, Long> eventVersion = new HashMap<>();
        eventVersion.put(VERSION, event.getVersion());
        list.add(eventVersion);

        return list;
    }

    @NewSpan
    private void handleConcurrency(@SpanTag("aggregateId") String aggregateId) {
        try {
            Map<String, String> map = new HashMap<>();
            map.put(AGGREGATE_ID, aggregateId);
            String aggregateID = jdbcTemplate.queryForObject(HANDLE_CONCURRENCY_QUERY, map, String.class);
            log.info("(handleConcurrency) aggregateID for lock: {}", aggregateID);
        } catch (EmptyResultDataAccessException e) {
            log.info("(handleConcurrency) EmptyResultDataAccessException: {}", e.getMessage());
        }
        log.info("(handleConcurrency) aggregateID for lock: {}", aggregateId);
    }

    @NewSpan
    private <T extends AggregateRoot> void saveSnapshot(@SpanTag("aggregate") T aggregate) {
        aggregate.toSnapshot();
        final Snapshot snapshot = EventSourcingUtils.snapshotFromAggregate(aggregate);

        int updateResult = jdbcTemplate.update(SAVE_SNAPSHOT_QUERY, new HashMap<>());

        log.info("(saveSnapshot) updateResult: {}", updateResult);
    }

    @NewSpan
    private Optional<Snapshot> loadSnapshot(@SpanTag("aggregateId") String aggregateId) {
        return null;//jdbcTemplate.query(LOAD_SNAPSHOT_QUERY, new HashMap<>());
    }

    @NewSpan
    private <T extends AggregateRoot> T getSnapshotFromClass(@SpanTag("snapshot") Optional<Snapshot> snapshot,
                                                             @SpanTag("aggregateId") String aggregateId,
                                                             @SpanTag("aggregateType") Class<T> aggregateType) {
        if (snapshot.isPresent()) {
            return EventSourcingUtils.aggregateFromSnapshot(snapshot.get(), aggregateType);
        }
        Snapshot defaultSnapshot = EventSourcingUtils.snapshotFromAggregate(getAggregate(aggregateId, aggregateType));
        return EventSourcingUtils.aggregateFromSnapshot(defaultSnapshot, aggregateType);
    }

    @NewSpan
    private <T extends AggregateRoot> T getAggregate(@SpanTag("aggregateId") final String aggregateId,
                                                     @SpanTag("aggregateType") final Class<T> aggregateType) {
        try {
            return aggregateType.getConstructor(String.class).newInstance(aggregateId);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
