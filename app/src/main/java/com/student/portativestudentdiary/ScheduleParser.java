package com.student.portativestudentdiary;

// import android.util.Log; // <<< Этот импорт больше не нужен
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleParser {

    // private static final String TAG = "ScheduleParser"; // <<< Это поле больше не нужно

    // Этот метод теперь публичный и не зависит от Activity и от Log
    public Lesson parseLessonDetails(String details) {
        // Log.d(TAG, "--- Parsing Details for: \"" + details + "\" ---"); // <<< УДАЛЯЕМ ЭТУ СТРОКУ

        if (details == null || details.trim().isEmpty()) {
            return null;
        }
        Lesson lesson = new Lesson();
        lesson.setRawDetails(details);

        String remainingDetails = details.trim();
        String subjectName = ""; String lessonType = ""; String classroom = ""; String teacherName = null;

        // 1. Извлечь аудиторию
        Pattern classroomPattern = Pattern.compile("\\(([^)]+)\\)$");
        Matcher classroomMatcher = classroomPattern.matcher(remainingDetails);
        if (classroomMatcher.find()) {
            classroom = classroomMatcher.group(1).trim();
            remainingDetails = classroomMatcher.replaceFirst("").trim();
        }
        lesson.setClassroom(classroom);

        // 2. Извлечь ФИО преподавателя
        Pattern teacherPattern = Pattern.compile("([А-ЯЁ][а-яё]+(?:-[А-ЯЁ][а-яё]+)?\\s+[А-ЯЁ]\\.\\s?[А-ЯЁ]\\.?)");
        Matcher mTeacher = teacherPattern.matcher(remainingDetails);
        String tempTeacher = null; int teacherStartIndex = -1;
        while (mTeacher.find()) {
            tempTeacher = mTeacher.group(1).trim();
            teacherStartIndex = mTeacher.start();
        }
        if (tempTeacher != null) {
            teacherName = tempTeacher;
            remainingDetails = remainingDetails.substring(0, teacherStartIndex).trim();
        }
        lesson.setTeacherName(teacherName);

        // 3. Определить тип занятия и название предмета
        String forTypeExtraction = remainingDetails;
        Pattern typeSuffixPattern = Pattern.compile("\\s+([лпс])\\.?([\\wА-Яа-яЁё.\\-]+)?$", Pattern.CASE_INSENSITIVE);
        Matcher typeSuffixMatcher = typeSuffixPattern.matcher(forTypeExtraction);
        if (typeSuffixMatcher.find()) {
            String typeAbbreviation = typeSuffixMatcher.group(1).toLowerCase();
            switch (typeAbbreviation) {
                case "л": lessonType = "лекция"; break;
                case "п": lessonType = "практика"; break;
                case "с": lessonType = "семинар"; break;
                default: lessonType = typeAbbreviation;
            }
            subjectName = forTypeExtraction.substring(0, typeSuffixMatcher.start()).trim();
        } else {
            subjectName = forTypeExtraction.trim();
        }
        lesson.setLessonType(lessonType);
        lesson.setSubjectName(subjectName.replaceAll("\\s+", " ").trim());

        // Log.d(TAG, "--- Parsed Lesson: " + lesson.getSubjectName() + " ---"); // <<< УДАЛЯЕМ ЭТУ СТРОКУ
        return lesson;
    }
}