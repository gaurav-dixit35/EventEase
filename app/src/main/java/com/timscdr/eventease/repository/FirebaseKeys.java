package com.timscdr.eventease.repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class FirebaseKeys {
    private FirebaseKeys() { }
    static String rollKey(String roll) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((roll == null ? "" : roll.trim().toUpperCase(java.util.Locale.ROOT))
                            .getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder(hash.length * 2);
            for (byte current : hash) value.append(String.format(java.util.Locale.ROOT, "%02x", current & 0xff));
            return value.toString();
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
