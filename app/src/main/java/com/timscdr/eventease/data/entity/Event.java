package com.timscdr.eventease.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "Event", indices = {@Index("dateTime")})
public class Event {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull public String title = "";
    @NonNull public String category = "";
    @NonNull public String description = "";
    @NonNull public String venue = "";
    /** UTC epoch milliseconds; format in the user's timezone in the UI. */
    public long dateTime;
    /** Local URI or compressed JPEG data URI; cloud repositories sync image bytes with Firestore. */
    @Nullable public String bannerImageUrl;
    public int capacity;
    @NonNull public String organizerName = "";
}
