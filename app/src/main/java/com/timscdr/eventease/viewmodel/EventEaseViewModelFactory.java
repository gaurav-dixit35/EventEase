package com.timscdr.eventease.viewmodel;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.SavedStateHandleSupport;
import androidx.lifecycle.viewmodel.CreationExtras;
import android.os.Handler;
import android.os.Looper;
import com.timscdr.eventease.repository.AppRepositories;
import com.timscdr.eventease.util.AndroidEventReminderScheduler;
import com.timscdr.eventease.util.EventReminderScheduler;

/** Composition boundary; screen classes never acquire repositories or DAOs themselves. */
public final class EventEaseViewModelFactory implements ViewModelProvider.Factory {
    private final Context appContext;
    private final AppRepositories repositories;
    private final long studentId;
    private final long eventId;
    private final long ticketId;
    private final EventReminderScheduler reminderScheduler;

    public EventEaseViewModelFactory(Context context, long studentId) {
        this(context, studentId, 0);
    }

    public EventEaseViewModelFactory(Context context, long studentId, long eventId) {
        this(context, studentId, eventId, 0);
    }

    public EventEaseViewModelFactory(Context context, long studentId, long eventId, long ticketId) {
        appContext = context.getApplicationContext();
        repositories = AppRepositories.getInstance(context.getApplicationContext());
        reminderScheduler = new AndroidEventReminderScheduler(context.getApplicationContext());
        this.studentId = studentId;
        this.eventId = eventId;
        this.ticketId = ticketId;
    }

    @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> type) {
        if (type == EventDetailViewModel.class) return type.cast(new EventDetailViewModel(
                repositories.events(), repositories.registrations(), eventId, studentId, System::currentTimeMillis));
        if (type == RegistrationViewModel.class) return type.cast(new RegistrationViewModel(
                repositories.events(), repositories.profiles(), repositories.registrations(), repositories.settings(),
                reminderScheduler, eventId, studentId,
                command -> new Handler(Looper.getMainLooper()).post(command)));
        if (type == TicketViewModel.class) return type.cast(new TicketViewModel(repositories.events(),
                repositories.profiles(), repositories.registrations(), eventId, studentId, ticketId));
        if (type == MyEventsViewModel.class) return type.cast(new MyEventsViewModel(
                repositories.events(), repositories.registrations(), studentId, System::currentTimeMillis));
        if (type == SettingsViewModel.class) return type.cast(new SettingsViewModel(repositories.settings(),
                repositories.events(), repositories.registrations(), reminderScheduler, studentId));
        if (type == CreateEventViewModel.class) return type.cast(new CreateEventViewModel(appContext, repositories.events(),
                repositories.profiles(), studentId, command -> new Handler(Looper.getMainLooper()).post(command)));
        if (type == ScanTicketViewModel.class) return type.cast(new ScanTicketViewModel(repositories.registrations(),
                repositories.profiles(), repositories.events(), eventId,
                command -> new Handler(Looper.getMainLooper()).post(command)));
        if (type == AuthViewModel.class) return type.cast(new AuthViewModel(repositories.auth()));
        if (type == OrganizerAuthViewModel.class) return type.cast(new OrganizerAuthViewModel(repositories.organizerAuth()));
        if (type == OrganizerDashboardViewModel.class) return type.cast(new OrganizerDashboardViewModel(repositories.organizer()));
        if (type == OrganizerAttendeesViewModel.class) return type.cast(new OrganizerAttendeesViewModel(repositories.organizer(), eventId));
        if (type == ProfileViewModel.class) {
            return type.cast(new ProfileViewModel(repositories.profiles(), repositories.registrations(), studentId));
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + type.getName());
    }

    @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> type, @NonNull CreationExtras extras) {
        if (type == EventListViewModel.class) {
            Handler handler = new Handler(Looper.getMainLooper());
            return type.cast(new EventListViewModel(repositories.events(), repositories.registrations(), studentId,
                    SavedStateHandleSupport.createSavedStateHandle(extras), command -> handler.post(command), System::currentTimeMillis));
        }
        return create(type);
    }
}
