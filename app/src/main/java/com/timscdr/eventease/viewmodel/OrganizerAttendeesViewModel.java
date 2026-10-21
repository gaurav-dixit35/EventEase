package com.timscdr.eventease.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.timscdr.eventease.repository.AttendeeRecord;
import com.timscdr.eventease.repository.OrganizerRepository;
import java.util.List;

public final class OrganizerAttendeesViewModel extends ViewModel {
    public final LiveData<List<AttendeeRecord>> attendees;
    public OrganizerAttendeesViewModel(OrganizerRepository repository, long eventId) {
        attendees = repository.observeAttendees(eventId);
    }
}
