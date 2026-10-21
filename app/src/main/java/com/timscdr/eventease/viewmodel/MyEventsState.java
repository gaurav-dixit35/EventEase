package com.timscdr.eventease.viewmodel;

import com.timscdr.eventease.data.entity.Event;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MyEventsState {
    public final List<Event> upcoming;
    public final List<Event> past;
    public final Map<Long, Long> registrationIds;
    public final boolean loading;

    public MyEventsState(List<Event> upcoming, List<Event> past, Map<Long, Long> registrationIds, boolean loading) {
        this.upcoming = Collections.unmodifiableList(new ArrayList<>(upcoming));
        this.past = Collections.unmodifiableList(new ArrayList<>(past));
        this.registrationIds = Collections.unmodifiableMap(new HashMap<>(registrationIds));
        this.loading = loading;
    }
}
