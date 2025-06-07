package com.student.portativestudentdiary;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final Map<Integer, String[]> LESSON_TIMES = new HashMap<>();
    static {
        LESSON_TIMES.put(1, new String[]{"08:30", "09:50"});
        LESSON_TIMES.put(2, new String[]{"10:00", "11:20"});
        LESSON_TIMES.put(3, new String[]{"11:30", "12:50"});
        LESSON_TIMES.put(4, new String[]{"13:10", "14:30"});
        LESSON_TIMES.put(5, new String[]{"14:40", "16:00"});
        LESSON_TIMES.put(6, new String[]{"16:10", "17:30"});
    }

    private ActivityResultLauncher<String[]> openFileLauncher;
    private ExecutorService executorService;
    private LessonDao lessonDao;
    private ScheduleMetaDao scheduleMetaDao;

    private RecyclerView recyclerViewLessons;
    private LessonAdapter lessonAdapter;
    private TextView textViewNoData;

    private long currentActiveScheduleId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonOpenFile = findViewById(R.id.button_open_file);
        recyclerViewLessons = findViewById(R.id.recyclerView_lessons);
        textViewNoData = findViewById(R.id.textView_no_data);

        executorService = Executors.newSingleThreadExecutor();

        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        lessonDao = db.lessonDao();
        scheduleMetaDao = db.scheduleMetaDao();

        setupRecyclerView();

        openFileLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        Log.d(TAG, "Выбран файл: " + uri.toString());
                        String fileName = getFileNameFromUri(uri);
                        Toast.makeText(MainActivity.this, "Файл '" + fileName + "' выбран, начинаю чтение...", Toast.LENGTH_SHORT).show();
                        readExcelFile(uri, fileName);
                    } else {
                        Log.d(TAG, "Файл не выбран");
                        Toast.makeText(MainActivity.this, "Файл не выбран", Toast.LENGTH_SHORT).show();
                    }
                });

        buttonOpenFile.setOnClickListener(v -> {
            String[] mimeTypes = {
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            };
            openFileLauncher.launch(mimeTypes);
        });

        loadLastActiveSchedule();
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка получения имени файла из URI: " + e.getMessage());
            }
        }
        if (fileName == null) {
            fileName = uri.getLastPathSegment();
            if (fileName != null) {
                // Попытка очистить имя файла, если это числовой ID
                try {
                    Long.parseLong(fileName); // Проверяем, является ли это числом
                    fileName = "Импортированный файл"; // Если да, то это, скорее всего, не имя
                } catch (NumberFormatException e) {
                    // Не число, вероятно, это нормальное имя или часть пути
                    File tempFile = new File(fileName);
                    fileName = tempFile.getName();
                }
            } else {
                fileName = "Импортированное расписание";
            }
        }
        return fileName;
    }


    private void setupRecyclerView() {
        recyclerViewLessons.setLayoutManager(new LinearLayoutManager(this));
        lessonAdapter = new LessonAdapter();
        recyclerViewLessons.setAdapter(lessonAdapter);
    }

    private void loadLastActiveSchedule() {
        executorService.execute(() -> {
            List<ScheduleMeta> allMetas = scheduleMetaDao.getAllSchedulesMeta();
            if (allMetas != null && !allMetas.isEmpty()) {
                currentActiveScheduleId = allMetas.get(0).getId();
                Log.d(TAG, "Загружено последнее активное расписание, ID: " + currentActiveScheduleId + ", Имя: " + allMetas.get(0).getName());
            } else {
                Log.d(TAG, "Нет сохраненных расписаний в БД.");
                currentActiveScheduleId = -1;
            }
            runOnUiThread(this::loadLessonsForToday);
        });
    }

    private void loadLessonsForToday() {
        if (currentActiveScheduleId == -1) {
            Log.d(TAG, "Нет активного расписания для загрузки уроков.");
            checkAndShowNoDataMessage(new ArrayList<>());
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        Log.d(TAG, "Загрузка занятий для расписания ID " + currentActiveScheduleId + " на дату: " + currentDate);
        loadLessonsByDate(currentDate);
    }

    private void loadLessonsByDate(final String dateToLoad) {
        if (currentActiveScheduleId == -1) {
            Log.d(TAG, "loadLessonsByDate: Нет активного расписания ID.");
            checkAndShowNoDataMessage(new ArrayList<>());
            return;
        }
        executorService.execute(() -> {
            List<Lesson> lessons = null;
            try {
                lessons = lessonDao.getLessonsByDateForSchedule(currentActiveScheduleId, dateToLoad);
                if (lessons == null) { // DAO может вернуть null, если произошла ошибка или запрос не выполнен
                    lessons = new ArrayList<>();
                }
                if (lessons.isEmpty()) {
                    Log.d(TAG, "Нет занятий для расписания ID " + currentActiveScheduleId + " на " + dateToLoad);
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке занятий из БД для расписания ID " + currentActiveScheduleId + ": " + e.getMessage());
                lessons = new ArrayList<>();
            }

            final List<Lesson> finalLessons = lessons; // lessons уже не null здесь
            runOnUiThread(() -> {
                lessonAdapter.setLessons(finalLessons);
                checkAndShowNoDataMessage(finalLessons);
            });
        });
    }

    private void checkAndShowNoDataMessage(List<Lesson> lessons) {
        if (lessons.isEmpty()) {
            recyclerViewLessons.setVisibility(View.GONE);
            textViewNoData.setVisibility(View.VISIBLE);
        } else {
            recyclerViewLessons.setVisibility(View.VISIBLE);
            textViewNoData.setVisibility(View.GONE);
        }
    }

    private void readExcelFile(final Uri uri, final String originalFileName) {
        executorService.execute(() -> {
            InputStream inputStream = null;
            Workbook workbook = null;
            List<Lesson> parsedLessons = new ArrayList<>();
            String currentDateStr = "";
            String currentDayOfWeekStr = "";

            String scheduleName = (originalFileName != null && !originalFileName.isEmpty() && !originalFileName.equals("Импортированный файл")) ?
                    originalFileName :
                    "Расписание от " + new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()).format(new Date());

            ScheduleMeta newScheduleMeta = new ScheduleMeta(scheduleName, System.currentTimeMillis(), originalFileName);
            long newScheduleId = -1;

            try {
                newScheduleId = scheduleMetaDao.insert(newScheduleMeta);
                currentActiveScheduleId = newScheduleId;
                Log.d(TAG, "Создана новая запись ScheduleMeta с ID: " + newScheduleId + " и именем: " + scheduleName);

                inputStream = getContentResolver().openInputStream(uri);
                workbook = WorkbookFactory.create(inputStream);
                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null) {
                    Log.e(TAG, "Первый лист не найден в файле!");
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Ошибка: Лист не найден", Toast.LENGTH_LONG).show());
                    return;
                }

                Log.d(TAG, "--- Начало ПАРСИНГА Excel файла (из readExcelFile) ---");
                int firstDataRowIndex = 8;

                for (int i = firstDataRowIndex; i < sheet.getPhysicalNumberOfRows(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    Cell dateCell = row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (dateCell != null && !dateCell.toString().trim().isEmpty()) {
                        String fullDateDay = dateCell.toString().trim();
                        String[] parts = fullDateDay.split("\\s+", 2);
                        currentDateStr = (parts.length > 0) ? parts[0].trim() : "";
                        currentDayOfWeekStr = (parts.length > 1) ? parts[1].trim() : "";
                    } else if (i == firstDataRowIndex && (currentDateStr == null || currentDateStr.isEmpty())) {
                        Log.e(TAG, "Строка " + i + ": не найдена дата в первой строке данных!");
                        // Возможно, стоит прервать парсинг, если формат совсем не тот
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Ошибка: не найдена дата в расписании.", Toast.LENGTH_LONG).show());
                        return; // Прерываем, если первая строка данных без даты
                    } else if (currentDateStr == null || currentDateStr.isEmpty()) {
                        Log.w(TAG, "Строка " + i + ": пустая дата, возможно конец таблицы.");
                        break;
                    }

                    Cell lessonNumberCell = row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (lessonNumberCell == null || lessonNumberCell.toString().trim().isEmpty()) continue;

                    int lessonNumber;
                    try {
                        lessonNumber = (int) Double.parseDouble(lessonNumberCell.toString().trim());
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Строка " + i + ": Не удалось распознать номер пары: " + lessonNumberCell.toString());
                        continue;
                    }

                    String[] times = LESSON_TIMES.get(lessonNumber);
                    if (times == null) {
                        Log.w(TAG, "Строка " + i + ": Нет времени для пары №" + lessonNumber);
                        times = new String[]{"??:??", "??:??"};
                    }

                    Cell shift1Cell = row.getCell(2, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (shift1Cell != null && !shift1Cell.toString().trim().isEmpty() && !shift1Cell.toString().trim().equalsIgnoreCase("пусто")) {
                        Lesson lesson = parseLessonDetails(shift1Cell.toString().trim(), currentDateStr, currentDayOfWeekStr, lessonNumber, 1, times);
                        if (lesson != null) {
                            lesson.setScheduleMetaId(newScheduleId);
                            parsedLessons.add(lesson);
                        }
                    }

                    Cell shift2Cell = row.getCell(3, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (shift2Cell != null && !shift2Cell.toString().trim().isEmpty() && !shift2Cell.toString().trim().equalsIgnoreCase("пусто")) {
                        Lesson lesson = parseLessonDetails(shift2Cell.toString().trim(), currentDateStr, currentDayOfWeekStr, lessonNumber, 2, times);
                        if (lesson != null) {
                            lesson.setScheduleMetaId(newScheduleId);
                            parsedLessons.add(lesson);
                        }
                    }
                }

                Log.d(TAG, "--- Конец ПАРСИНГА Excel файла (из readExcelFile) ---");
                Log.d(TAG, "Всего распознано занятий для сохранения: " + parsedLessons.size());

                if (!parsedLessons.isEmpty()) {
                    Log.d(TAG, "Начинаю сохранение в БД для scheduleMetaId: " + newScheduleId);
                    lessonDao.insertAll(parsedLessons);
                    int countInDb = lessonDao.getLessonsCountForSchedule(newScheduleId);
                    Log.d(TAG, "Данные сохранены в БД. Всего записей для этого расписания: " + countInDb);

                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this,
                                "Расписание '" + scheduleName + "' сохранено! Записей: " + countInDb,
                                Toast.LENGTH_LONG).show();
                        loadLessonsForToday();
                    });
                } else {
                    Log.d(TAG, "Нет занятий для сохранения.");
                    // Если нечего сохранять, но ScheduleMeta был создан, возможно, стоит его удалить?
                    // Или просто показать, что уроков нет.
                    if (newScheduleId != -1) { // Если мета запись была создана, но уроков нет
                        // scheduleMetaDao.deleteScheduleMetaById(newScheduleId); // Опционально - удалить пустую мета-запись
                        // Log.d(TAG, "Удалена пустая ScheduleMeta запись с ID: " + newScheduleId);
                    }
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Нет занятий для сохранения в файле.", Toast.LENGTH_SHORT).show();
                        loadLessonsForToday(); // Загрузить то, что могло быть активно ранее или ничего
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при ПАРСИНГЕ или СОХРАНЕНИИ Excel файла: " + e.getMessage(), e);
                e.printStackTrace();
                // Если произошла ошибка и newScheduleId был создан, можно его удалить
                if (newScheduleId != -1) {
                    // В реальном приложении здесь нужна более аккуратная логика отката транзакции
                    // scheduleMetaDao.deleteScheduleMetaById(newScheduleId);
                }
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Ошибка обработки файла: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                try {
                    if (workbook != null) workbook.close();
                    if (inputStream != null) inputStream.close();
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при закрытии ресурсов: " + e.getMessage());
                }
            }
        });
    }

    // Вспомогательный метод для парсинга строки с деталями занятия
    // (содержит отладочные логи, которые можно будет сократить)
    private Lesson parseLessonDetails(String details, String date, String dayOfWeek, int lessonNumber, int shift, String[] times) {
        // Ты можешь сократить или удалить эти логи, когда парсер будет полностью отлажен
        Log.d(TAG, "--- Parsing Details for: \"" + details + "\" ---");

        if (details == null || details.trim().isEmpty()) {
            return null;
        }
        Lesson lesson = new Lesson();
        lesson.setDate(date);
        lesson.setDayOfWeek(dayOfWeek);
        lesson.setLessonNumber(lessonNumber);
        lesson.setShiftNumber(shift);
        lesson.setStartTime(times[0]);
        lesson.setEndTime(times[1]);
        lesson.setRawDetails(details);

        String remainingDetails = details.trim();
        String subjectName = ""; String lessonType = ""; String classroom = ""; String teacherName = "";

        Pattern classroomPattern = Pattern.compile("\\(([^)]+)\\)$");
        Matcher classroomMatcher = classroomPattern.matcher(remainingDetails);
        if (classroomMatcher.find()) {
            classroom = classroomMatcher.group(1).trim();
            remainingDetails = classroomMatcher.replaceFirst("").trim();
        }
        lesson.setClassroom(classroom);
        Log.d(TAG, "parseLessonDetails: After Classroom ('" + classroom + "'): remainingDetails = \"" + remainingDetails + "\"");

        String forTeacherExtraction = remainingDetails;
        Pattern teacherPattern = Pattern.compile(
                "([А-ЯЁ][а-яё]+(?:-[А-ЯЁ][а-яё]+)?\\s+[А-ЯЁ]\\.\\s?[А-ЯЁ]\\.?)" +
                        "|" +
                        "([А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+)"
        );
        Matcher mTeacher = teacherPattern.matcher(forTeacherExtraction);
        String tempTeacher = null; int teacherStartIndex = -1; int teacherEndIndex = -1;

        while (mTeacher.find()) {
            if (mTeacher.group(1) != null) tempTeacher = mTeacher.group(1).trim();
            else if (mTeacher.group(2) != null) tempTeacher = mTeacher.group(2).trim();
            teacherStartIndex = mTeacher.start();
            teacherEndIndex = mTeacher.end();
        }

        if (tempTeacher != null) {
            boolean isTeacherAtVeryEnd = (teacherEndIndex == forTeacherExtraction.length());
            boolean isTeacherFollowedBySpaceIfNotEmpty = (teacherEndIndex < forTeacherExtraction.length() && Character.isWhitespace(forTeacherExtraction.charAt(teacherEndIndex)));
            boolean isTeacherAtVeryStart = (teacherStartIndex == 0);
            boolean isTeacherPrecededBySpaceIfNotAtStart = (teacherStartIndex > 0 && Character.isWhitespace(forTeacherExtraction.charAt(teacherStartIndex - 1)));

            if ((isTeacherAtVeryEnd || isTeacherFollowedBySpaceIfNotEmpty) && (isTeacherAtVeryStart || isTeacherPrecededBySpaceIfNotAtStart)) {
                if (forTeacherExtraction.substring(teacherStartIndex, teacherEndIndex).equals(tempTeacher)) {
                    teacherName = tempTeacher;
                    if (teacherStartIndex == 0 && teacherEndIndex == forTeacherExtraction.length()) {
                        remainingDetails = "";
                    } else if (teacherStartIndex == 0) {
                        remainingDetails = forTeacherExtraction.substring(teacherEndIndex).trim();
                    } else {
                        remainingDetails = forTeacherExtraction.substring(0, teacherStartIndex).trim();
                    }
                } else {
                    Log.d(TAG, "parseLessonDetails: Potential teacher '" + tempTeacher + "' substring mismatch.");
                }
            } else {
                Log.d(TAG, "parseLessonDetails: Potential teacher '" + tempTeacher + "' not used (position/spacing check failed).");
            }
        }
        lesson.setTeacherName(teacherName);
        Log.d(TAG, "parseLessonDetails: After Teacher ('" + teacherName + "'): remainingDetails = \"" + remainingDetails + "\"");

        String forTypeExtraction = remainingDetails;
        Pattern typeSuffixPattern = Pattern.compile("\\s+([лпс])\\.?([\\wА-Яа-яЁё.\\-]+)?$", Pattern.CASE_INSENSITIVE);
        Matcher typeSuffixMatcher = typeSuffixPattern.matcher(forTypeExtraction);
        Log.d(TAG, "parseLessonDetails: String for Type Suffix Pattern: \"" + forTypeExtraction + "\"");

        if (typeSuffixMatcher.find()) {
            Log.d(TAG, "parseLessonDetails: Type Suffix Pattern FOUND!");
            String typeAbbreviation = typeSuffixMatcher.group(1).toLowerCase();
            switch (typeAbbreviation) {
                case "л": lessonType = "лекция"; break;
                case "п": lessonType = "практика"; break;
                case "с": lessonType = "семинар"; break;
                default: lessonType = typeAbbreviation;
            }
            subjectName = forTypeExtraction.substring(0, typeSuffixMatcher.start()).trim();
            Log.d(TAG, "parseLessonDetails: Type by Suffix: '" + lessonType + "', Subject: '" + subjectName + "'");
        } else {
            Log.d(TAG, "parseLessonDetails: Type Suffix Pattern NOT FOUND.");
            String lowerRemaining = forTypeExtraction.toLowerCase();
            if (lowerRemaining.startsWith("лаб ") || lowerRemaining.contains("лабораторная")) {
                lessonType = "лабораторная";
                subjectName = forTypeExtraction.replaceFirst("(?i)лаб\\s*", "").trim();
                Log.d(TAG, "parseLessonDetails: Type by Prefix 'лаб': '" + lessonType + "', Subject: '" + subjectName + "'");
            } else {
                subjectName = forTypeExtraction.trim();
                Log.d(TAG, "parseLessonDetails: No specific type found. Subject: '" + subjectName + "'");
            }
        }
        lesson.setLessonType(lessonType);
        lesson.setSubjectName(subjectName.replaceAll("\\s+", " ").trim());

        Log.d(TAG, "--- Parsed Lesson: " + lesson.toString() + " ---");
        return lesson;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}