package com.timscdr.eventease.repository;

import android.content.SharedPreferences;

public final class LocalSettingsRepository implements SettingsRepository {
    static final String SYSTEM = "system", LIGHT = "light", DARK = "dark";
    private static final String THEME = "theme_mode", NOTIFICATIONS = "notifications_enabled";
    private final SharedPreferences preferences;
    public LocalSettingsRepository(SharedPreferences preferences) { this.preferences = preferences; }
    @Override public String themeMode() {
        String value = preferences.getString(THEME, SYSTEM);
        return SYSTEM.equals(value) || LIGHT.equals(value) || DARK.equals(value) ? value : SYSTEM;
    }
    @Override public void setThemeMode(String mode) {
        if (!SYSTEM.equals(mode) && !LIGHT.equals(mode) && !DARK.equals(mode)) throw new IllegalArgumentException("Unknown theme mode.");
        preferences.edit().putString(THEME, mode).apply();
    }
    @Override public boolean notificationsEnabled() { return preferences.getBoolean(NOTIFICATIONS, false); }
    @Override public void setNotificationsEnabled(boolean enabled) {
        preferences.edit().putBoolean(NOTIFICATIONS, enabled).apply();
    }
}
