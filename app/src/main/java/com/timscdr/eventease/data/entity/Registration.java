package com.timscdr.eventease.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Ignore;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "Registration",
        foreignKeys = {
                @ForeignKey(entity = Event.class, parentColumns = "id",
                        childColumns = "eventId", onDelete = ForeignKey.RESTRICT),
                @ForeignKey(entity = StudentProfile.class, parentColumns = "id",
                        childColumns = "studentId", onDelete = ForeignKey.RESTRICT)
        },
        indices = {
                @Index(value = "registrationCode", unique = true),
                @Index(value = {"eventId", "studentId"}, unique = true),
                @Index("studentId")
        })
public class Registration {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long eventId;
    public long studentId;
    @NonNull public String registrationCode = "";
    @Ignore @Nullable public String attendeeName;
    @Ignore @Nullable public String attendeeRollNo;
    @Ignore @Nullable public String attendeeEmail;
    @Ignore @Nullable public String attendeeCollegeName;
    /** UTC epoch milliseconds. */
    public long registeredAt;
    public boolean checkedIn;
}
