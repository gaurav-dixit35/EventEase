package com.timscdr.eventease.repository;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

final class FirebaseIds {
    private FirebaseIds() { }
    static CompletableFuture<Long> next(FirebaseFirestore database, String counter) {
        DocumentReference ref = database.collection("system").document("ids");
        return FirebaseTask.future(database.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(ref);
            Long previous = snapshot.getLong(counter);
            long value = previous == null ? 1L : previous + 1L;
            transaction.set(ref, Collections.singletonMap(counter, value),
                    com.google.firebase.firestore.SetOptions.merge());
            return value;
        }));
    }
}
