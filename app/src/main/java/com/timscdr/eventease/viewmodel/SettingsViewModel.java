package com.timscdr.eventease.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.repository.EventRepository;
import com.timscdr.eventease.repository.RegistrationRepository;
import com.timscdr.eventease.repository.SettingsRepository;
import com.timscdr.eventease.util.EventReminderScheduler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SettingsViewModel extends ViewModel {
    public static final class State {
        public final String themeMode;
        public final boolean notificationsEnabled;
        State(String themeMode, boolean notificationsEnabled) {
            this.themeMode = themeMode; this.notificationsEnabled = notificationsEnabled;
        }
    }

    private final SettingsRepository settings;
    private final EventReminderScheduler scheduler;
    private final MediatorLiveData<State> state = new MediatorLiveData<>();
    private final Map<Long, Registration> registrationsByEvent = new HashMap<>();
    private final Map<Long, Event> eventsById = new HashMap<>();
    private final Map<Long, LiveData<Event>> eventSources = new HashMap<>();

    public SettingsViewModel(SettingsRepository settings, EventRepository events,
            RegistrationRepository registrations, EventReminderScheduler scheduler, long studentId) {
        this.settings = settings; this.scheduler = scheduler;
        state.addSource(registrations.observeForStudent(studentId), rows -> {
            Map<Long, Registration> next = new HashMap<>();
            if (rows != null) for (Registration row : rows) next.put(row.eventId, row);
            for (Map.Entry<Long, Registration> entry : registrationsByEvent.entrySet()) {
                if (!next.containsKey(entry.getKey())) scheduler.cancel(entry.getValue().registrationCode);
            }
            registrationsByEvent.clear(); registrationsByEvent.putAll(next);
            Set<Long> needed = new HashSet<>(next.keySet());
            for (Long eventId : new ArrayList<>(eventSources.keySet())) if (!needed.contains(eventId)) {
                state.removeSource(eventSources.remove(eventId)); eventsById.remove(eventId);
            }
            for (Long eventId : needed) if (!eventSources.containsKey(eventId)) {
                LiveData<Event> source = events.observeById(eventId);
                eventSources.put(eventId, source);
                state.addSource(source, event -> { eventsById.put(eventId, event); syncReminders(); });
            }
            publish(); syncReminders();
        });
        publish();
    }

    public LiveData<State> state() { return state; }
    public void setThemeMode(String value) { settings.setThemeMode(value); publish(); }
    public void setNotificationsEnabled(boolean enabled) {
        settings.setNotificationsEnabled(enabled); publish(); syncReminders();
    }

    private void syncReminders() {
        for (Map.Entry<Long, Registration> entry : registrationsByEvent.entrySet()) {
            Registration registration = entry.getValue();
            Event event = eventsById.get(entry.getKey());
            if (!settings.notificationsEnabled() || event == null) scheduler.cancel(registration.registrationCode);
            else scheduler.schedule(registration, event);
        }
    }

    private void publish() { state.setValue(new State(settings.themeMode(), settings.notificationsEnabled())); }
}
