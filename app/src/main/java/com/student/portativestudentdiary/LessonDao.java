package com.student.portativestudentdiary;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LessonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Lesson lesson); // Lesson уже будет содержать scheduleMetaId

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Lesson> lessons); // Список Lessons уже будет содержать scheduleMetaId

    // Получаем все занятия для КОНКРЕТНОГО расписания, отсортированные корректно
    @Query("SELECT * FROM lessons_table WHERE schedule_meta_id = :scheduleId " +
            "ORDER BY SUBSTR(lesson_date, 7, 2), " +  // Сначала сортируем по году (гг)
            "SUBSTR(lesson_date, 4, 2), " +  // Затем по месяцу (мм)
            "SUBSTR(lesson_date, 1, 2), " +  // Затем по дню (дд)
            "lesson_number, shift_number")     // И потом уже по номеру пары (lesson_number) и смене
    List<Lesson> getLessonsForSchedule(long scheduleId);

    // Получаем занятия на КОНКРЕТНУЮ дату для КОНКРЕТНОГО расписания
    @Query("SELECT * FROM lessons_table WHERE schedule_meta_id = :scheduleId AND lesson_date = :date ORDER BY lesson_number, shift_number")
    List<Lesson> getLessonsByDateForSchedule(long scheduleId, String date);

    // Количество занятий для КОНКРЕТНОГО расписания
    @Query("SELECT COUNT(*) FROM lessons_table WHERE schedule_meta_id = :scheduleId")
    int getLessonsCountForSchedule(long scheduleId);

    @Delete
    void delete(Lesson lesson); // Удаление конкретного занятия (по объекту)

    // Удаляем ВСЕ занятия для КОНКРЕТНОГО расписания
    @Query("DELETE FROM lessons_table WHERE schedule_meta_id = :scheduleId")
    void deleteLessonsForSchedule(long scheduleId);

    @Update // <<< НОВЫЙ МЕТОД для обновления занятия
    void update(Lesson lesson);
}
