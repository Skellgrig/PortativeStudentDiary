package com.student.portativestudentdiary;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ScheduleParserTest {

    private ScheduleParser parser;

    @Before
    public void setUp() {
        parser = new ScheduleParser();
    }

    @Test
    public void parseLesson_StandardCase_CorrectlyParsed() {
        String details = "Диск. мат. с эл.мат. лог. л.9 Рыбников М.С. (801)";
        Lesson result = parser.parseLessonDetails(details);

        assertNotNull(result);
        assertEquals("Диск. мат. с эл.мат. лог.", result.getSubjectName());
        assertEquals("лекция", result.getLessonType());
        assertEquals("Рыбников М.С.", result.getTeacherName());
        assertEquals("801", result.getClassroom());
    }

    @Test
    public void parseLesson_PracticeType_CorrectlyParsed() {
        String details = "Внедр. и поддерж. комп. сист п.7 Яковенко Л.В. (701б)";
        Lesson result = parser.parseLessonDetails(details);

        assertNotNull(result);
        assertEquals("Внедр. и поддерж. комп. сист", result.getSubjectName());
        assertEquals("практика", result.getLessonType());
        assertEquals("Яковенко Л.В.", result.getTeacherName());
        assertEquals("701б", result.getClassroom());
    }

    @Test
    public void parseLesson_SeminarType_CorrectlyParsed() {
        String details = "П/п деят. в сф. зем.-имущ. отн с.5 Алиева Э.С. (332)";
        Lesson result = parser.parseLessonDetails(details);

        assertNotNull(result);
        assertEquals("П/п деят. в сф. зем.-имущ. отн", result.getSubjectName());
        assertEquals("семинар", result.getLessonType());
        assertEquals("Алиева Э.С.", result.getTeacherName());
        assertEquals("332", result.getClassroom());
    }

    @Test
    public void parseLesson_NoTeacher_ParsedCorrectly() {
        String details = "Основы физической культуры п.1 (С/з)";
        Lesson result = parser.parseLessonDetails(details);

        assertNotNull(result);
        assertEquals("Основы физической культуры", result.getSubjectName());
        assertEquals("практика", result.getLessonType());
        assertNull("Teacher name should be null", result.getTeacherName());
        assertEquals("С/з", result.getClassroom());
    }

    @Test
    public void parseLesson_NoClassroom_ParsedCorrectly() {
        String details = "Иностранный язык в проф. деят. п.3 Яцкина Е.А.";
        Lesson result = parser.parseLessonDetails(details);

        assertNotNull(result);
        assertEquals("Иностранный язык в проф. деят.", result.getSubjectName());
        assertEquals("практика", result.getLessonType());
        assertEquals("Яцкина Е.А.", result.getTeacherName());
        assertTrue("Classroom should be null or empty", result.getClassroom() == null || result.getClassroom().isEmpty());
    }

    @Test
    public void parseLesson_NoType_ParsedCorrectly() {
        String details = "Консультация по ВКР Петров В.В. (404)";
        Lesson result = parser.parseLessonDetails(details);

        assertNotNull(result);
        assertEquals("Консультация по ВКР", result.getSubjectName());
        assertTrue("Lesson type should be null or empty", result.getLessonType() == null || result.getLessonType().isEmpty());
        assertEquals("Петров В.В.", result.getTeacherName());
        assertEquals("404", result.getClassroom());
    }
}