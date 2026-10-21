package com.timscdr.eventease.repository;

public final class AttendeeRecord {
    public final String registrationCode;
    public final String name;
    public final String rollNo;
    public final String email;
    public final String collegeName;
    public final long registeredAt;
    public final boolean checkedIn;

    public AttendeeRecord(String registrationCode, String name, String rollNo, String email,
            String collegeName, long registeredAt, boolean checkedIn) {
        this.registrationCode = registrationCode;
        this.name = name;
        this.rollNo = rollNo;
        this.email = email;
        this.collegeName = collegeName;
        this.registeredAt = registeredAt;
        this.checkedIn = checkedIn;
    }
}
