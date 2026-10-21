package com.timscdr.eventease.repository;

public final class OrganizerStats {
    public final int eventCount;
    public final int registrationCount;
    public final int checkedInCount;

    public OrganizerStats(int eventCount, int registrationCount, int checkedInCount) {
        this.eventCount = eventCount;
        this.registrationCount = registrationCount;
        this.checkedInCount = checkedInCount;
    }
}
