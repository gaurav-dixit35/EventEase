package com.timscdr.eventease.repository;

import com.timscdr.eventease.data.entity.Event;

public final class OrganizerEventSummary {
    public final Event event;
    public final int registeredCount;
    public final int checkedInCount;

    public OrganizerEventSummary(Event event, int registeredCount, int checkedInCount) {
        this.event = event;
        this.registeredCount = registeredCount;
        this.checkedInCount = checkedInCount;
    }
}
