package com.timscdr.eventease.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "StudentProfile")
public class StudentProfile {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull public String name = "";
    @NonNull public String rollNo = "";
    @NonNull public String course = "";
    public int year;
    @NonNull public String email = "";
    @NonNull public String collegeName = "TIMSCDR";
}
