package com.timscdr.eventease.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import com.timscdr.eventease.data.dao.EventDao;
import com.timscdr.eventease.data.entity.Event;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

final class RoomEventRepository implements EventRepository {
    private final EventDao dao;
    private final Executor executor;

    RoomEventRepository(EventDao dao, Executor executor) {
        this.dao = dao;
        this.executor = executor;
    }

    @Override public CompletableFuture<List<Event>> findUpcoming(long now, @Nullable String category, String search) {
        return RepositoryTasks.submit(executor, () -> dao.findUpcoming(now, category, search == null ? "" : search));
    }

    @Override public CompletableFuture<Integer> countAll() {
        return RepositoryTasks.submit(executor, dao::countAll);
    }

    @Override public CompletableFuture<Void> loadSamples(long now) {
        return RepositoryTasks.submit(executor, () -> {
            dao.insertSamplesIfEmpty(SampleEvents.create(now));
            return null;
        });
    }

    @Override public LiveData<Event> observeById(long eventId) {
        return dao.observeById(eventId);
    }

    @Override public LiveData<List<Event>> observeUpcoming(long now, @Nullable String category, String search) {
        return dao.observeUpcoming(now, category, search == null ? "" : search);
    }

    @Override public LiveData<List<Event>> observeUpcomingForStudent(long studentId, long now) {
        return dao.observeUpcomingForStudent(studentId, now);
    }

    @Override public LiveData<List<Event>> observePastForStudent(long studentId, long now) {
        return dao.observePastForStudent(studentId, now);
    }

    @Override public CompletableFuture<Event> findById(long eventId) {
        return RepositoryTasks.submit(executor, () -> dao.findById(eventId));
    }

    @Override public CompletableFuture<Long> insert(Event event) {
        Event snapshot = EntitySnapshots.copy(event);
        return RepositoryTasks.submit(executor, () -> {
            RepositoryTasks.requireNew(snapshot.id);
            return dao.insert(snapshot);
        });
    }

    @Override public CompletableFuture<Void> update(Event event) {
        Event snapshot = EntitySnapshots.copy(event);
        return RepositoryTasks.submit(executor, () -> RepositoryTasks.requireUpdated(dao.update(snapshot)));
    }

    @Override public CompletableFuture<Void> delete(long eventId) {
        return RepositoryTasks.submit(executor, () -> {
            try {
                if (dao.deleteById(eventId) == 0) throw new RepositoryException(
                        RepositoryException.Reason.NOT_FOUND, "Event not found.");
                return null;
            } catch (RepositoryException error) { throw error; }
            catch (RuntimeException error) { throw new RepositoryException(
                    RepositoryException.Reason.STORAGE, "Could not delete event.", error); }
        });
    }
}
