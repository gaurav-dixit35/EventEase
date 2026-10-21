package com.timscdr.eventease.repository;

import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.data.entity.StudentProfile;
import java.util.Objects;

/** Capture mutable form objects before they enter the background queue. */
final class EntitySnapshots {
    private EntitySnapshots() { }

    static Event copy(Event source) {
        Objects.requireNonNull(source, "event");
        Event result = new Event();
        result.id = source.id;
        result.title = source.title;
        result.category = source.category;
        result.description = source.description;
        result.venue = source.venue;
        result.dateTime = source.dateTime;
        result.bannerImageUrl = source.bannerImageUrl;
        result.capacity = source.capacity;
        result.organizerName = source.organizerName;
        return result;
    }

    static StudentProfile copy(StudentProfile source) {
        Objects.requireNonNull(source, "profile");
        StudentProfile result = new StudentProfile();
        result.id = source.id;
        result.name = source.name;
        result.rollNo = source.rollNo;
        result.course = source.course;
        result.year = source.year;
        result.email = source.email;
        result.collegeName = source.collegeName;
        return result;
    }

    static Registration copy(Registration source) {
        Objects.requireNonNull(source, "registration");
        Registration result = new Registration();
        result.id = source.id;
        result.eventId = source.eventId;
        result.studentId = source.studentId;
        result.registrationCode = source.registrationCode;
        result.attendeeName = source.attendeeName;
        result.attendeeRollNo = source.attendeeRollNo;
        result.attendeeEmail = source.attendeeEmail;
        result.attendeeCollegeName = source.attendeeCollegeName;
        result.registeredAt = source.registeredAt;
        result.checkedIn = source.checkedIn;
        return result;
    }
}
