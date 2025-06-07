package com.student.portativestudentdiary;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// Добавляем ScheduleMeta.class в entities и УВЕЛИЧИВАЕМ ВЕРСИЮ БАЗЫ ДАННЫХ
@Database(entities = {Lesson.class, ScheduleMeta.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract LessonDao lessonDao();
    public abstract ScheduleMetaDao scheduleMetaDao(); // Добавляем абстрактный метод для нового DAO

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "student_diary_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
