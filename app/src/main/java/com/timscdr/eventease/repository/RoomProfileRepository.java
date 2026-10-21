package com.timscdr.eventease.repository;

import androidx.lifecycle.LiveData;
import com.timscdr.eventease.data.dao.ProfileDao;
import com.timscdr.eventease.data.entity.StudentProfile;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

final class RoomProfileRepository implements ProfileRepository {
    private final ProfileDao dao;
    private final Executor executor;

    RoomProfileRepository(ProfileDao dao, Executor executor) {
        this.dao = dao;
        this.executor = executor;
    }

    @Override public LiveData<StudentProfile> observeById(long studentId) {
        return dao.observeById(studentId);
    }

    @Override public CompletableFuture<StudentProfile> findById(long studentId) {
        return RepositoryTasks.submit(executor, () -> dao.findById(studentId));
    }

    @Override public CompletableFuture<List<StudentProfile>> findByRollNo(String rollNo) {
        return RepositoryTasks.submit(executor, () -> dao.findByRollNo(rollNo));
    }

    @Override public CompletableFuture<Long> insert(StudentProfile profile) {
        StudentProfile snapshot = EntitySnapshots.copy(profile);
        return RepositoryTasks.submit(executor, () -> {
            RepositoryTasks.requireNew(snapshot.id);
            return dao.insert(snapshot);
        });
    }

    @Override public CompletableFuture<Void> update(StudentProfile profile) {
        StudentProfile snapshot = EntitySnapshots.copy(profile);
        return RepositoryTasks.submit(executor, () -> RepositoryTasks.requireUpdated(dao.update(snapshot)));
    }
}
