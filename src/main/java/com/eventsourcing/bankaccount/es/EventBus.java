package com.eventsourcing.bankaccount.es;

import java.util.List;

public interface EventBus {
    void publish(List<Event> events);
}
