package com.timscdr.eventease.viewmodel;

import com.timscdr.eventease.data.entity.Event;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EventListState {
    public final List<Event> events;
    public final Set<Long> registeredIds;
    public final boolean loading;
    public final boolean error;
    public final boolean catalogEmpty;

    public EventListState(List<Event> events, Set<Long> registeredIds, boolean loading, boolean error, boolean catalogEmpty) {
        this.events = Collections.unmodifiableList(new ArrayList<>(events));
        this.registeredIds = Collections.unmodifiableSet(new HashSet<>(registeredIds));
        this.loading = loading;
        this.error = error;
        this.catalogEmpty = catalogEmpty;
    }
}
