package com.student.portativestudentdiary;

import android.content.ContentResolver;
import android.content.Intent;
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
import androidx.annotation.NonNull;
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

public class SchedulesListActivity extends AppCompatActivity implements ScheduleMetaAdapter.OnScheduleClickListener {

    private static final String TAG = "SchedulesListActivity";

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

    private RecyclerView recyclerViewSchedules;
    private ScheduleMetaAdapter scheduleMetaAdapter;
    private TextView textViewNoSchedules;
    private Button buttonImportNewSchedule;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedules_list);

        recyclerViewSchedules = findViewById(R.id.recyclerView_schedules_list);
        textViewNoSchedules = findViewById(R.id.textView_no_schedules);
        buttonImportNewSchedule = findViewById(R.id.button_import_new_schedule);

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
                        Toast.makeText(this, "Файл '" + fileName + "' выбран, начинаю импорт...", Toast.LENGTH_SHORT).show();

                        String scheduleName = (fileName != null && !fileName.isEmpty() && !fileName.equals("Импортированный файл") && !fileName.equals("Импортированное расписание")) ?
                                fileName :
                                "Расписание от " + new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()).format(new Date());
                        importExcelFile(uri, scheduleName, fileName);
                    } else {
                        Log.d(TAG, "Файл не выбран");
                        Toast.makeText(this, "Файл не выбран", Toast.LENGTH_SHORT).show();
                    }
                });

        buttonImportNewSchedule.setOnClickListener(v -> {
            String[] mimeTypes = {
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            };
            openFileLauncher.launch(mimeTypes);
        });

        loadSchedulesList();
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        String scheme = uri.getScheme();

        if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (displayNameIndex != -1) {
                        fileName = cursor.getString(displayNameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка получения имени файла из ContentResolver: " + e.getMessage());
            }
        }

        if (fileName == null) {
            fileName = uri.getLastPathSegment();
            if (fileName != null) {
                File tempFile = new File(fileName);
                fileName = tempFile.getName();
                if (fileName.matches("\\d+")) {
                    fileName = "Импортированный файл";
                }
            }
        }

        if (fileName == null || fileName.isEmpty()) {
            fileName = "Импортированное расписание";
        }
        return fileName;
    }


    private void setupRecyclerView() {
        recyclerViewSchedules.setLayoutManager(new LinearLayoutManager(this));
        scheduleMetaAdapter = new ScheduleMetaAdapter(this);
        recyclerViewSchedules.setAdapter(scheduleMetaAdapter);
    }

    private void loadSchedulesList() {
        executorService.execute(() -> {
            List<ScheduleMeta> schedules = scheduleMetaDao.getAllSchedulesMeta();
            runOnUiThread(() -> {
                scheduleMetaAdapter.setSchedules(schedules);
                if (schedules.isEmpty()) {
                    recyclerViewSchedules.setVisibility(View.GONE);
                    textViewNoSchedules.setVisibility(View.VISIBLE);
                } else {
                    recyclerViewSchedules.setVisibility(View.VISIBLE);
                    textViewNoSchedules.setVisibility(View.GONE);
                }
            });
        });
    }

    private void importExcelFile(final Uri uri, final String scheduleName, final String sourceFileName) {
        executorService.execute(() -> {
            InputStream inputStream = null;
            Workbook workbook = null;
            List<Lesson> parsedLessons = new ArrayList<>();
            String currentDateStr = "";
            String currentDayOfWeekStr = "";

            String group1Identifier = null;
            String group2Identifier = null;

            // Создаем экземпляр нашего нового парсера
            ScheduleParser parser = new ScheduleParser();

            ScheduleMeta newScheduleMeta = new ScheduleMeta(scheduleName, System.currentTimeMillis(), sourceFileName);
            long newScheduleId = -1;

            try {
                newScheduleId = scheduleMetaDao.insert(newScheduleMeta);
                if (newScheduleId == -1) {
                    Log.e(TAG, "Не удалось создать запись ScheduleMeta для: " + scheduleName);
                    runOnUiThread(() -> Toast.makeText(this, "Ошибка сохранения метаданных расписания.", Toast.LENGTH_LONG).show());
                    return;
                }
                Log.d(TAG, "Создана новая ScheduleMeta с ID: " + newScheduleId + " и именем: " + scheduleName);

                inputStream = getContentResolver().openInputStream(uri);
                workbook = WorkbookFactory.create(inputStream);
                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null) {
                    Log.e(TAG, "Первый лист не найден в файле!");
                    runOnUiThread(() -> Toast.makeText(this, "Ошибка: Лист не найден", Toast.LENGTH_LONG).show());
                    if (newScheduleId != -1) scheduleMetaDao.deleteScheduleMetaById(newScheduleId);
                    return;
                }

                Log.d(TAG, "--- Начало ПАРСИНГА Excel файла для '" + scheduleName + "' ---");

                int groupRowIndex = 7;
                int firstDataRowIndex = 8;

                Row groupInfoRow = sheet.getRow(groupRowIndex);
                if (groupInfoRow != null) {
                    Cell group1Cell = groupInfoRow.getCell(2, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (group1Cell != null && !group1Cell.toString().trim().isEmpty()) {
                        group1Identifier = group1Cell.toString().trim();
                        if (group1Identifier.endsWith(".0")) group1Identifier = group1Identifier.substring(0, group1Identifier.length() - 2);
                        Log.d(TAG, "Считан идентификатор группы для 1 смены (из строки " + groupRowIndex + "): " + group1Identifier);
                    }
                    Cell group2Cell = groupInfoRow.getCell(3, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (group2Cell != null && !group2Cell.toString().trim().isEmpty()) {
                        group2Identifier = group2Cell.toString().trim();
                        if (group2Identifier.endsWith(".0")) group2Identifier = group2Identifier.substring(0, group2Identifier.length() - 2);
                        Log.d(TAG, "Считан идентификатор группы для 2 смены (из строки " + groupRowIndex + "): " + group2Identifier);
                    }
                } else {
                    Log.w(TAG, "Строка с номерами групп (индекс " + groupRowIndex + ") не найдена или пуста.");
                }


                for (int i = firstDataRowIndex; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    Cell dateCell = row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (dateCell != null && !dateCell.toString().trim().isEmpty()) {
                        String fullDateDay = dateCell.toString().trim();
                        String[] parts = fullDateDay.split("\\s+", 2);
                        currentDateStr = (parts.length > 0) ? parts[0].trim() : "";
                        currentDayOfWeekStr = (parts.length > 1) ? parts[1].trim() : "";
                    } else if (i == firstDataRowIndex && (currentDateStr == null || currentDateStr.isEmpty())) {
                        Log.e(TAG, "Строка " + i + ": не найдена дата в первой строке данных! Файл: " + scheduleName);
                        runOnUiThread(() -> Toast.makeText(this, "Ошибка формата: не найдена дата в первой строке данных расписания.", Toast.LENGTH_LONG).show());
                        if (newScheduleId != -1) scheduleMetaDao.deleteScheduleMetaById(newScheduleId);
                        return;
                    } else if (currentDateStr == null || currentDateStr.isEmpty()) {
                        Log.w(TAG, "Строка " + i + ": пустая дата, возможно конец таблицы. Файл: " + scheduleName);
                        break;
                    }

                    Cell lessonNumberCell = row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (lessonNumberCell == null || lessonNumberCell.toString().trim().isEmpty()) continue;

                    int lessonNumber;
                    try {
                        lessonNumber = (int) Double.parseDouble(lessonNumberCell.toString().trim());
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Строка " + i + ": Не удалось распознать номер пары: '" + lessonNumberCell.toString() + "'");
                        continue;
                    }

                    String[] times = LESSON_TIMES.get(lessonNumber);
                    if (times == null) {
                        Log.w(TAG, "Строка " + i + ": Нет времени для пары №" + lessonNumber);
                        times = new String[]{"??:??", "??:??"};
                    }

                    // Парсинг занятия для 1-й смены
                    Cell shift1Cell = row.getCell(2, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (shift1Cell != null && !shift1Cell.toString().trim().isEmpty() && !shift1Cell.toString().trim().equalsIgnoreCase("пусто")) {
                        // <<< ВЫЗЫВАЕМ МЕТОД ИЗ КЛАССА ScheduleParser
                        Lesson lesson = parser.parseLessonDetails(shift1Cell.toString().trim());
                        if (lesson != null) {
                            // Дополняем объект данными, не полученными из парсинга строки
                            lesson.setDate(currentDateStr);
                            lesson.setDayOfWeek(currentDayOfWeekStr);
                            lesson.setLessonNumber(lessonNumber);
                            lesson.setShiftNumber(1);
                            lesson.setStartTime(times[0]);
                            lesson.setEndTime(times[1]);
                            lesson.setScheduleMetaId(newScheduleId);
                            if (group1Identifier != null && !group1Identifier.isEmpty()) {
                                lesson.setGroupIdentifier(group1Identifier);
                            }
                            parsedLessons.add(lesson);
                        }
                    }

                    // Парсинг занятия для 2-й смены
                    Cell shift2Cell = row.getCell(3, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (shift2Cell != null && !shift2Cell.toString().trim().isEmpty() && !shift2Cell.toString().trim().equalsIgnoreCase("пусто")) {
                        // <<< ВЫЗЫВАЕМ МЕТОД ИЗ КЛАССА ScheduleParser
                        Lesson lesson = parser.parseLessonDetails(shift2Cell.toString().trim());
                        if (lesson != null) {
                            // Дополняем объект данными
                            lesson.setDate(currentDateStr);
                            lesson.setDayOfWeek(currentDayOfWeekStr);
                            lesson.setLessonNumber(lessonNumber);
                            lesson.setShiftNumber(2);
                            lesson.setStartTime(times[0]);
                            lesson.setEndTime(times[1]);
                            lesson.setScheduleMetaId(newScheduleId);
                            if (group2Identifier != null && !group2Identifier.isEmpty()) {
                                lesson.setGroupIdentifier(group2Identifier);
                            }
                            parsedLessons.add(lesson);
                        }
                    }
                }
                Log.d(TAG, "--- Конец ПАРСИНГА Excel файла для '" + scheduleName + "' ---");
                Log.d(TAG, "Всего распознано занятий для сохранения: " + parsedLessons.size());

                if (!parsedLessons.isEmpty()) {
                    lessonDao.insertAll(parsedLessons);
                    Log.d(TAG, "Занятия для '" + scheduleName + "' (ID: "+ newScheduleId + ") сохранены в БД.");
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Расписание '" + scheduleName + "' импортировано!", Toast.LENGTH_LONG).show();
                        loadSchedulesList();
                    });
                } else {
                    Log.d(TAG, "Нет занятий для сохранения в файле '" + scheduleName + "'.");
                    if (newScheduleId != -1) {
                        scheduleMetaDao.deleteScheduleMetaById(newScheduleId);
                        Log.d(TAG, "Удалена ScheduleMeta запись (ID: " + newScheduleId + ") для '" + scheduleName + "', т.к. не было уроков.");
                    }
                    runOnUiThread(() -> {
                        Toast.makeText(this, "В файле не найдено занятий для импорта.", Toast.LENGTH_SHORT).show();
                        loadSchedulesList();
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при импорте Excel файла '" + scheduleName + "': " + e.getMessage(), e);
                e.printStackTrace();
                if (newScheduleId != -1) {
                    scheduleMetaDao.deleteScheduleMetaById(newScheduleId);
                    Log.d(TAG, "Удалена ScheduleMeta запись (ID: " + newScheduleId + ") из-за ошибки импорта.");
                }
                runOnUiThread(() -> Toast.makeText(this, "Ошибка импорта файла: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                try {
                    if (workbook != null) workbook.close();
                    if (inputStream != null) inputStream.close();
                } catch (Exception ex) {
                    Log.e(TAG, "Ошибка при закрытии ресурсов: " + ex.getMessage());
                }
            }
        });
    }

    //
    // МЕТОД parseLessonDetails ТЕПЕРЬ ОТСУТСТВУЕТ В ЭТОМ КЛАССЕ,
    // ТАК КАК МЫ ВЫНЕСЛИ ЕГО В ОТДЕЛЬНЫЙ КЛАСС ScheduleParser
    //

    @Override
    public void onScheduleClick(ScheduleMeta scheduleMeta) {
        Toast.makeText(this, "Выбрано расписание: " + scheduleMeta.getName() + " (ID: " + scheduleMeta.getId() + ")", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, ScheduleViewActivity.class);
        intent.putExtra(ScheduleViewActivity.EXTRA_SCHEDULE_ID, scheduleMeta.getId());
        intent.putExtra(ScheduleViewActivity.EXTRA_SCHEDULE_NAME, scheduleMeta.getName());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSchedulesList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
