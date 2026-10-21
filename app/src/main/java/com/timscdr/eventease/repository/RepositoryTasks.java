package com.timscdr.eventease.repository;

import android.database.sqlite.SQLiteConstraintException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

final class RepositoryTasks {
    private RepositoryTasks() { }

    static <T> CompletableFuture<T> submit(Executor executor, Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            executor.execute(() -> {
                try {
                    future.complete(task.get());
                } catch (RepositoryException error) {
                    future.completeExceptionally(error);
                } catch (SQLiteConstraintException error) {
                    future.completeExceptionally(new RepositoryException(
                            RepositoryException.Reason.CONFLICT,
                            "The record conflicts with a database constraint.", error));
                } catch (RuntimeException error) {
                    future.completeExceptionally(new RepositoryException(
                            RepositoryException.Reason.STORAGE, "Database operation failed.", error));
                }
            });
        } catch (RejectedExecutionException error) {
            future.completeExceptionally(new RepositoryException(
                    RepositoryException.Reason.STORAGE, "Database executor is unavailable.", error));
        }
        return future;
    }

    static void requireNew(long id) {
        if (id != 0) {
            throw new RepositoryException(RepositoryException.Reason.INVALID_INPUT,
                    "New records must use ID 0.");
        }
    }

    static Void requireUpdated(int rows) {
        if (rows == 0) {
            throw new RepositoryException(RepositoryException.Reason.NOT_FOUND,
                    "The record no longer exists.");
        }
        return null;
    }
}
