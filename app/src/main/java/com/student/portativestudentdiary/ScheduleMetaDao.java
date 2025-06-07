package com.student.portativestudentdiary;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface ScheduleMetaDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE) // Игнорировать, если пытаемся вставить с существующим ID
    long insert(ScheduleMeta scheduleMeta); // Возвращает ID вставленной записи

    @Query("SELECT * FROM schedule_meta_table ORDER BY import_timestamp DESC")
    List<ScheduleMeta> getAllSchedulesMeta();

    @Query("SELECT * FROM schedule_meta_table WHERE id = :id")
    ScheduleMeta getScheduleMetaById(long id);

    @Delete
    void delete(ScheduleMeta scheduleMeta);

    @Query("DELETE FROM schedule_meta_table WHERE id = :scheduleId")
    void deleteScheduleMetaById(long scheduleId);
}
