package com.timscdr.eventease.viewmodel;

import android.content.Context;
import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.repository.EventRepository;
import com.timscdr.eventease.repository.ProfileRepository;
import com.timscdr.eventease.util.EventImageEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CreateEventViewModel extends ViewModel {
    public static final class State {
        public final boolean saving, created, updated;
        public final String error;
        public final long eventId;
        State(boolean saving, boolean created, boolean updated, String error, long eventId) {
            this.saving = saving; this.created = created; this.updated = updated;
            this.error = error; this.eventId = eventId;
        }
    }
    private static final Set<String> CATEGORIES = new HashSet<>(Arrays.asList(
            "Technical", "Cultural", "Sports", "Workshop", "Seminar"));
    private final EventRepository events;
    private final ProfileRepository profiles;
    private final Context appContext;
    private final long studentId;
    private final Executor main;
    private final AtomicBoolean busy = new AtomicBoolean();
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(false, false, false, null, 0));
    private volatile boolean cleared;

    public CreateEventViewModel(Context context, EventRepository events, ProfileRepository profiles, long studentId, Executor main) {
        this.appContext = context.getApplicationContext();
        this.events = events; this.profiles = profiles; this.studentId = studentId; this.main = main;
    }
    public LiveData<State> state() { return state; }

    public void create(String title, String category, String description, String venue,
            long dateTime, String bannerUri, String capacityText) {
        save(0, title, category, description, venue, dateTime, bannerUri, capacityText);
    }
    public void update(long eventId, String title, String category, String description, String venue,
            long dateTime, String bannerUri, String capacityText) {
        save(eventId, title, category, description, venue, dateTime, bannerUri, capacityText);
    }
    private void save(long eventId, String title, String category, String description, String venue,
            long dateTime, String bannerUri, String capacityText) {
        if (!busy.compareAndSet(false, true)) return;
        String cleanTitle = clean(title), cleanCategory = clean(category), cleanDescription = clean(description);
        String cleanVenue = clean(venue), cleanCapacity = clean(capacityText);
        int capacity;
        try { capacity = Integer.parseInt(cleanCapacity); }
        catch (NumberFormatException error) { invalid("capacity"); return; }
        if (cleanTitle.isEmpty()) { invalid("title"); return; }
        if (!CATEGORIES.contains(cleanCategory)) { invalid("category"); return; }
        if (cleanDescription.isEmpty()) { invalid("description"); return; }
        if (cleanVenue.isEmpty()) { invalid("venue"); return; }
        if (dateTime <= System.currentTimeMillis()) { invalid("date"); return; }
        if (capacity < 1) { invalid("capacity"); return; }
        state.setValue(new State(true, false, false, null, 0));

        String selectedImage = clean(bannerUri);
        boolean encodingImage = EventImageEncoder.needsCompression(selectedImage);
        java.util.concurrent.CompletableFuture<String> preparedImage;
        if (selectedImage.isEmpty()) preparedImage = java.util.concurrent.CompletableFuture.completedFuture(null);
        else if (!encodingImage) preparedImage = java.util.concurrent.CompletableFuture.completedFuture(selectedImage);
        else preparedImage = java.util.concurrent.CompletableFuture.supplyAsync(() -> selectedImage
                .startsWith(EventImageEncoder.DATA_URI_PREFIX)
                ? EventImageEncoder.compressDataUri(selectedImage)
                : EventImageEncoder.compressToDataUri(appContext, Uri.parse(selectedImage)));

        java.util.concurrent.CompletableFuture<String> name = studentId > 0
                ? profiles.findById(studentId).thenApply(profile -> profile == null ? null : profile.name)
                : events.currentOrganizerName();
        preparedImage.thenCombine(name, (image, organizerName) -> new String[]{image, organizerName})
                .thenCompose(values -> {
            String image = values[0], organizerName = values[1];
            if (organizerName == null || organizerName.trim().isEmpty()) {
                throw new IllegalStateException("Organizer profile is unavailable.");
            }
            if (eventId <= 0) {
                Event event = new Event();
                updateFields(event, organizerName, cleanTitle, cleanCategory, cleanDescription,
                        cleanVenue, dateTime, image, capacity);
                return events.insert(event).thenApply(id -> new SaveResult(id, false));
            }
            return events.findById(eventId).thenCompose(event -> {
                if (event == null) throw new IllegalStateException("Event not found.");
                updateFields(event, organizerName, cleanTitle, cleanCategory, cleanDescription,
                        cleanVenue, dateTime, image, capacity);
                if (image == null && EventImageEncoder.isOversizedDataUri(event.bannerImageUrl)) {
                    return java.util.concurrent.CompletableFuture.supplyAsync(() ->
                            EventImageEncoder.compressDataUri(event.bannerImageUrl)).thenCompose(compressed -> {
                        event.bannerImageUrl = compressed;
                        return events.update(event).thenApply(unused -> new SaveResult(eventId, true));
                    });
                }
                return events.update(event).thenApply(unused -> new SaveResult(eventId, true));
            });
        }).whenComplete((result, failure) -> main.execute(() -> {
            if (!cleared) state.setValue(failure == null
                    ? new State(false, true, result.updated, null, result.id)
                    : new State(false, false, false, encodingImage ? "image" : "save", 0));
            busy.set(false);
        }));
    }
    private static void updateFields(Event event, String name, String title, String category, String description,
            String venue, long dateTime, String bannerUri, int capacity) {
        event.title = title; event.category = category; event.description = description;
        event.venue = venue; event.dateTime = dateTime; event.capacity = capacity;
        event.organizerName = name;
        if (!clean(bannerUri).isEmpty()) event.bannerImageUrl = clean(bannerUri);
    }
    private void invalid(String field) {
        busy.set(false); state.setValue(new State(false, false, false, field, 0));
    }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    @Override protected void onCleared() { cleared = true; }
    private static final class SaveResult {
        final long id; final boolean updated;
        SaveResult(long id, boolean updated) { this.id = id; this.updated = updated; }
    }
}
