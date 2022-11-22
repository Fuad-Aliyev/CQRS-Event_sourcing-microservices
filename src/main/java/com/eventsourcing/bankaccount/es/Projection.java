package com.eventsourcing.bankaccount.es;

public interface Projection {
    void when(Event event);
}
