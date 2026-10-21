package com.timscdr.eventease.repository;

import com.timscdr.eventease.data.entity.StudentProfile;
import java.util.concurrent.CompletableFuture;

/** Publishes a local student profile to the shared backend. */
interface StudentProfilePublisher {
    CompletableFuture<Void> publish(StudentProfile profile);
}
