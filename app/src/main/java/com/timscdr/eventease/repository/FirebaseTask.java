package com.timscdr.eventease.repository;

import com.google.android.gms.tasks.Task;
import java.util.concurrent.CompletableFuture;

final class FirebaseTask {
    private FirebaseTask() { }
    static <T> CompletableFuture<T> future(Task<T> task) {
        CompletableFuture<T> result = new CompletableFuture<>();
        task.addOnSuccessListener(result::complete).addOnFailureListener(result::completeExceptionally);
        return result;
    }
    static CompletableFuture<Void> voidFuture(Task<?> task) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        task.addOnSuccessListener(ignored -> result.complete(null)).addOnFailureListener(result::completeExceptionally);
        return result;
    }
}
