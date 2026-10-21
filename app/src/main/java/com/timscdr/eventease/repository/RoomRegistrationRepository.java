package com.timscdr.eventease.repository;

import androidx.lifecycle.LiveData;
import com.timscdr.eventease.data.dao.RegistrationDao;
import com.timscdr.eventease.data.entity.Registration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

final class RoomRegistrationRepository implements RegistrationRepository {
    private final RegistrationDao dao;
    private final Executor executor;

    RoomRegistrationRepository(RegistrationDao dao, Executor executor) {
        this.dao = dao;
        this.executor = executor;
    }

    @Override public LiveData<Registration> observeById(long registrationId) {
        return dao.observeById(registrationId);
    }

    @Override public LiveData<List<Registration>> observeForStudent(long studentId) {
        return dao.observeForStudent(studentId);
    }

    @Override public LiveData<Registration> observeForEventAndStudent(long eventId, long studentId) {
        return dao.observeForEventAndStudent(eventId, studentId);
    }

    @Override public LiveData<Integer> observeRegistrationCount(long studentId) {
        return dao.observeRegistrationCount(studentId);
    }

    @Override public LiveData<Integer> observeAttendanceCount(long studentId) {
        return dao.observeAttendanceCount(studentId);
    }

    @Override public CompletableFuture<Registration> findByCode(String code) {
        return RepositoryTasks.submit(executor, () -> dao.findByCode(code));
    }

    @Override public CompletableFuture<Integer> countForEvent(long eventId) {
        return RepositoryTasks.submit(executor, () -> dao.countForEvent(eventId));
    }

    @Override public LiveData<Integer> observeCountForEvent(long eventId) {
        return dao.observeCountForEvent(eventId);
    }

    @Override public CompletableFuture<Long> insert(Registration registration) {
        Registration snapshot = EntitySnapshots.copy(registration);
        return RepositoryTasks.submit(executor, () -> {
            RepositoryTasks.requireNew(snapshot.id);
            return dao.insert(snapshot);
        });
    }

    @Override public CompletableFuture<Long> registerIfCapacity(Registration registration, int capacity) {
        Registration snapshot = EntitySnapshots.copy(registration);
        return RepositoryTasks.submit(executor, () -> {
            RepositoryTasks.requireNew(snapshot.id);
            return dao.insertIfCapacity(snapshot, Math.max(0, capacity));
        });
    }

    @Override public CompletableFuture<Boolean> markCheckedIn(String code) {
        return RepositoryTasks.submit(executor, () -> dao.markCheckedIn(code) == 1);
    }
}
