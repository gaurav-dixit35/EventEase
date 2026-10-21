package com.timscdr.eventease.repository;

import com.timscdr.eventease.data.entity.Event;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Fictional opt-in demo content, never presented as an official college schedule. */
final class SampleEvents {
    private SampleEvents() { }

    static List<Event> create(long now) {
        String[] titles = {"Sample: Campus Hackathon", "Sample: Cultural Evening",
                "Sample: Sports Meet", "Sample: Android Workshop", "Sample: Careers Seminar"};
        String[] categories = {"Technical", "Cultural", "Sports", "Workshop", "Seminar"};
        String[] venues = {"Computer Lab", "Auditorium", "Sports Ground", "Computer Lab", "Seminar Hall"};
        List<Event> result = new ArrayList<>();
        for (int i = 0; i < titles.length; i++) {
            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(now);
            date.add(Calendar.DAY_OF_YEAR, i + 2);
            date.set(Calendar.HOUR_OF_DAY, 10 + i);
            date.set(Calendar.MINUTE, 0);
            date.set(Calendar.SECOND, 0);
            date.set(Calendar.MILLISECOND, 0);
            Event event = new Event();
            event.title = titles[i];
            event.category = categories[i];
            event.venue = venues[i];
            event.dateTime = date.getTimeInMillis();
            event.capacity = 50 + i * 20;
            event.organizerName = "Sample Student Club";
            event.description = "Fictional event for the EventEase mini-project demonstration. "
                    + "This is not an official TIMSCDR event announcement.\n\n"
                    + "Explore the event details, date, venue, and available seats. "
                    + "Bring your curiosity and connect with fellow students.";
            result.add(event);
        }
        return result;
    }
}
