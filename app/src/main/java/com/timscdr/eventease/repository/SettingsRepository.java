package com.timscdr.eventease.repository;

/** Preferences that are independent of a particular screen or Activity. */
public interface SettingsRepository {
    String themeMode();
    void setThemeMode(String mode);
    boolean notificationsEnabled();
    void setNotificationsEnabled(boolean enabled);
}
