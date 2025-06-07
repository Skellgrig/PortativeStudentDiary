package com.student.portativestudentdiary;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "lessons_table",
        foreignKeys = @ForeignKey(entity = ScheduleMeta.class,
                parentColumns = "id", // Поле 'id' в ScheduleMeta
                childColumns = "schedule_meta_id", // Поле в Lesson для связи
                onDelete = ForeignKey.CASCADE), // При удалении ScheduleMeta, удалятся и связанные Lessons
        indices = {@Index("schedule_meta_id")}) // Индекс для ускорения запросов по schedule_meta_id
public class Lesson {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "schedule_meta_id")
    private long scheduleMetaId; // Внешний ключ для связи с ScheduleMeta

    @ColumnInfo(name = "lesson_date")
    private String date;          // Дата, например "30.09.24"

    @ColumnInfo(name = "day_of_week")
    private String dayOfWeek;     // День недели, например "Понедельник"

    @ColumnInfo(name = "lesson_number")
    private int lessonNumber;     // Номер пары (1, 2, 3...)

    @ColumnInfo(name = "start_time")
    private String startTime;     // Время начала, например "08:30"

    @ColumnInfo(name = "end_time")
    private String endTime;       // Время окончания, например "09:50"

    @ColumnInfo(name = "shift_number")
    private int shiftNumber;      // Номер смены (1 или 2)

    @ColumnInfo(name = "subject_name")
    private String subjectName;   // Название предмета

    @ColumnInfo(name = "lesson_type")
    private String lessonType;    // Тип занятия (лекция, практика, семинар)

    @ColumnInfo(name = "teacher_name")
    private String teacherName;   // Имя преподавателя

    @ColumnInfo(name = "classroom")
    private String classroom;     // Аудитория

    @ColumnInfo(name = "group_identifier")
    private String groupIdentifier;

    @ColumnInfo(name = "note")
    private String note;

    @ColumnInfo(name = "raw_details")
    private String rawDetails;

    @ColumnInfo(name = "is_reminder_active", defaultValue = "0")
    private boolean isReminderActive;

    @ColumnInfo(name = "reminder_offset_minutes", defaultValue = "-1")
    private int reminderOffsetMinutes;

    // Пустой конструктор необходим для Room
    public Lesson() {
    }

    // Геттеры и сеттеры для всех полей

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getScheduleMetaId() {
        return scheduleMetaId;
    }

    public void setScheduleMetaId(long scheduleMetaId) {
        this.scheduleMetaId = scheduleMetaId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getLessonNumber() {
        return lessonNumber;
    }

    public void setLessonNumber(int lessonNumber) {
        this.lessonNumber = lessonNumber;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getShiftNumber() {
        return shiftNumber;
    }

    public void setShiftNumber(int shiftNumber) {
        this.shiftNumber = shiftNumber;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getLessonType() {
        return lessonType;
    }

    public void setLessonType(String lessonType) {
        this.lessonType = lessonType;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getClassroom() {
        return classroom;
    }

    public void setClassroom(String classroom) {
        this.classroom = classroom;
    }

    public String getGroupIdentifier() {
        return groupIdentifier;
    }

    public void setGroupIdentifier(String groupIdentifier) {
        this.groupIdentifier = groupIdentifier;
    }

    public String getRawDetails() {
        return rawDetails;
    }

    public void setRawDetails(String rawDetails) {
        this.rawDetails = rawDetails;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isReminderActive() {
        return isReminderActive;
    }

    public void setReminderActive(boolean reminderActive) {
        isReminderActive = reminderActive;
    }

    public int getReminderOffsetMinutes() {
        return reminderOffsetMinutes;
    }

    public void setReminderOffsetMinutes(int reminderOffsetMinutes) {
        this.reminderOffsetMinutes = reminderOffsetMinutes;
    }

    @NonNull
    @Override
    public String toString() {
        return "Lesson{" +
                "id=" + id +
                ", scheduleMetaId=" + scheduleMetaId +
                ", date='" + date + '\'' +
                ", dayOfWeek='" + dayOfWeek + '\'' +
                ", lessonNumber=" + lessonNumber +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", shiftNumber=" + shiftNumber +
                ", subjectName='" + subjectName + '\'' +
                ", lessonType='" + lessonType + '\'' +
                ", teacherName='" + teacherName + '\'' +
                ", classroom='" + classroom + '\'' +
                ", groupIdentifier='" + groupIdentifier + '\'' +
                ", note='" + (note != null ? note : "null") + '\'' +
                ", reminderActive=" + isReminderActive +
                ", reminderOffset=" + reminderOffsetMinutes +
                ", rawDetails='" + rawDetails + '\'' +
                '}';
    }
}
