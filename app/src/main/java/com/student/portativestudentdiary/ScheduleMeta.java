package com.student.portativestudentdiary;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "schedule_meta_table")
public class ScheduleMeta {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "schedule_name")
    private String name;

    @ColumnInfo(name = "import_timestamp")
    private long importTimestamp; // Время импорта в миллисекундах

    @ColumnInfo(name = "source_file_name") // Опционально, для информации
    private String sourceFileName;

    // Конструкторы
    public ScheduleMeta(String name, long importTimestamp, String sourceFileName) {
        this.name = name;
        this.importTimestamp = importTimestamp;
        this.sourceFileName = sourceFileName;
    }

    // Геттеры и Сеттеры
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getImportTimestamp() {
        return importTimestamp;
    }

    public void setImportTimestamp(long importTimestamp) {
        this.importTimestamp = importTimestamp;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }
}
