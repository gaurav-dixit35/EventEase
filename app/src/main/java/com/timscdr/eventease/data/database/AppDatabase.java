package com.timscdr.eventease.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.timscdr.eventease.data.dao.EventDao;
import com.timscdr.eventease.data.dao.ProfileDao;
import com.timscdr.eventease.data.dao.RegistrationDao;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.data.entity.StudentProfile;

@Database(entities = {Event.class, Registration.class, StudentProfile.class},
        version = 2, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE StudentProfile ADD COLUMN collegeName TEXT NOT NULL DEFAULT 'TIMSCDR'");
        }
    };
    private static volatile AppDatabase instance;

    public abstract EventDao eventDao();
    public abstract RegistrationDao registrationDao();
    public abstract ProfileDao profileDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "eventease.db").addMigrations(MIGRATION_1_2).build();
                }
            }
        }
        return instance;
    }
}
