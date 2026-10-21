package com.timscdr.eventease.repository;

/** A Firebase account that has been explicitly provisioned as an EventEase organizer. */
public final class OrganizerAccount {
    public final String uid;
    public final String name;
    public final String email;
    public final String collegeName;

    public OrganizerAccount(String uid, String name, String email, String collegeName) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.collegeName = collegeName;
    }
}
