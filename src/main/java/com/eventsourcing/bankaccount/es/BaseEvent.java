package com.eventsourcing.bankaccount.es;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
public abstract class BaseEvent {
    protected String aggregateId;

    public BaseEvent(String aggregateId) {
        Objects.requireNonNull(aggregateId);
        if (aggregateId.isEmpty()) throw new RuntimeException("BaseEvent aggregateId is required");
        this.aggregateId = aggregateId;
    }
}
