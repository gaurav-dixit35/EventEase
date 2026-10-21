package com.timscdr.eventease.util;

import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;

public interface EventReminderScheduler {
    void schedule(Registration registration, Event event);
    void cancel(String registrationCode);
}
