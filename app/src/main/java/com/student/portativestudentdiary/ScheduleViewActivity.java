package com.student.portativestudentdiary;

import android.content.DialogInterface;
import android.content.Intent; // Добавлен, если ранее отсутствовал
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat; // Убедись, что этот импорт есть
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date; // Убедись, что этот импорт есть
import java.util.HashSet;
import java.util.List;
import java.util.Locale; // Убедись, что этот импорт есть
import java.util.Map; // Убедись, что этот импорт есть (для LESSON_TIMES, если оно было бы здесь)
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScheduleViewActivity extends AppCompatActivity implements FullScheduleAdapter.OnLessonClickListener {

    private static final String TAG = "ScheduleViewActivity";
    public static final String EXTRA_SCHEDULE_ID = "com.student.portativestudentdiary.SCHEDULE_ID";
    public static final String EXTRA_SCHEDULE_NAME = "com.student.portativestudentdiary.SCHEDULE_NAME";
    private static final String ALL_GROUPS_FILTER = "Все группы";

    private RecyclerView recyclerViewFullSchedule;
    private FullScheduleAdapter fullScheduleAdapter;
    private TextView textViewNoLessonsInSchedule;
    private Toolbar toolbar;
    private Spinner groupSpinner;

    private LessonDao lessonDao;
    private ExecutorService executorService;

    private long scheduleId = -1;
    private String scheduleName = "Расписание";

    private List<Lesson> allLessonsForCurrentSchedule = new ArrayList<>();
    private List<String> groupSpinnerItems = new ArrayList<>();
    private ArrayAdapter<String> groupSpinnerAdapter;
    private String currentGroupFilter = ALL_GROUPS_FILTER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_view);

        toolbar = findViewById(R.id.toolbar_schedule_view);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        recyclerViewFullSchedule = findViewById(R.id.recyclerView_full_schedule);
        textViewNoLessonsInSchedule = findViewById(R.id.textView_no_lessons_in_schedule);
        groupSpinner = findViewById(R.id.spinner_group_filter);

        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        lessonDao = db.lessonDao();
        executorService = Executors.newSingleThreadExecutor();

        if (getIntent().hasExtra(EXTRA_SCHEDULE_ID)) {
            scheduleId = getIntent().getLongExtra(EXTRA_SCHEDULE_ID, -1);
        }
        if (getIntent().hasExtra(EXTRA_SCHEDULE_NAME)) {
            scheduleName = getIntent().getStringExtra(EXTRA_SCHEDULE_NAME);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(scheduleName);
        }

        setupRecyclerView();
        setupGroupSpinner();

        if (scheduleId != -1) {
            loadAllLessonsForSchedule(scheduleId);
        } else {
            Log.e(TAG, "Schedule ID не был передан в Activity.");
            Toast.makeText(this, "Ошибка: ID расписания не найден.", Toast.LENGTH_LONG).show();
            checkAndShowNoDataMessage(new ArrayList<>());
        }
    }

    private void setupRecyclerView() {
        recyclerViewFullSchedule.setLayoutManager(new LinearLayoutManager(this));
        fullScheduleAdapter = new FullScheduleAdapter(this); // Передаем this как слушателя
        recyclerViewFullSchedule.setAdapter(fullScheduleAdapter);
    }

    private void setupGroupSpinner() {
        groupSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, groupSpinnerItems);
        groupSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(groupSpinnerAdapter);

        groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedGroup = (String) parent.getItemAtPosition(position);
                if (!selectedGroup.equals(currentGroupFilter)) { // Фильтруем только если выбор изменился
                    currentGroupFilter = selectedGroup;
                    Log.d(TAG, "Выбрана группа для фильтрации: " + currentGroupFilter);
                    filterAndDisplayLessons();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не делаем
            }
        });
    }

    private void loadAllLessonsForSchedule(long scheduleMetaId) {
        Log.d(TAG, "Загрузка ВСЕХ данных для расписания с ID: " + scheduleMetaId);
        executorService.execute(() -> {
            List<Lesson> lessonsFromDb = lessonDao.getLessonsForSchedule(scheduleMetaId);
            if (lessonsFromDb == null) { // На случай если DAO вернет null
                lessonsFromDb = new ArrayList<>();
            }
            allLessonsForCurrentSchedule.clear();
            allLessonsForCurrentSchedule.addAll(lessonsFromDb);

            runOnUiThread(() -> {
                populateGroupSpinner(allLessonsForCurrentSchedule);
                // filterAndDisplayLessons() будет вызван после populateGroupSpinner,
                // т.к. populateGroupSpinner может изменить currentGroupFilter (установив его в "Все группы")
                // и триггернуть onItemSelected, который вызовет filterAndDisplayLessons.
                // Для явного первоначального отображения, можно вызвать его здесь, если Spinner не сработал.
                // Однако, setSelection в populateGroupSpinner должен вызвать onItemSelected.
                // Если нет, то раскомментировать следующую строку:
                // filterAndDisplayLessons();
            });
        });
    }

    private void populateGroupSpinner(List<Lesson> lessons) {
        Set<String> uniqueGroups = new HashSet<>();
        if (lessons != null) {
            for (Lesson lesson : lessons) {
                if (lesson.getGroupIdentifier() != null && !lesson.getGroupIdentifier().isEmpty()) {
                    uniqueGroups.add(lesson.getGroupIdentifier());
                }
            }
        }

        groupSpinnerItems.clear();
        groupSpinnerItems.add(ALL_GROUPS_FILTER);
        List<String> sortedGroups = new ArrayList<>(uniqueGroups);
        Collections.sort(sortedGroups);
        groupSpinnerItems.addAll(sortedGroups);

        groupSpinnerAdapter.notifyDataSetChanged();

        // Устанавливаем выбор Spinner на "Все группы" или на текущий фильтр, если он уже был установлен
        // Это должно вызвать onItemSelectedListener, который вызовет filterAndDisplayLessons
        int selectionIndex = groupSpinnerItems.indexOf(currentGroupFilter);
        if (selectionIndex >= 0) {
            if (groupSpinner.getSelectedItemPosition() != selectionIndex) { // Избегаем лишнего вызова, если уже выбрано
                groupSpinner.setSelection(selectionIndex);
            } else { // Если уже выбрано то, что нужно, но список мог быть пустым - просто фильтруем
                filterAndDisplayLessons();
            }
        } else { // Если текущий фильтр не найден (например, после удаления группы), сбрасываем на "Все группы"
            currentGroupFilter = ALL_GROUPS_FILTER;
            groupSpinner.setSelection(0);
        }
    }

    private void filterAndDisplayLessons() {
        List<Lesson> filteredLessons = new ArrayList<>();
        if (allLessonsForCurrentSchedule == null) { // Добавлена проверка на null
            allLessonsForCurrentSchedule = new ArrayList<>();
        }

        if (currentGroupFilter.equals(ALL_GROUPS_FILTER)) {
            filteredLessons.addAll(allLessonsForCurrentSchedule);
        } else {
            for (Lesson lesson : allLessonsForCurrentSchedule) {
                if (currentGroupFilter.equals(lesson.getGroupIdentifier())) {
                    filteredLessons.add(lesson);
                }
            }
        }
        Log.d(TAG, "Отфильтровано занятий: " + filteredLessons.size() + " для группы '" + currentGroupFilter + "'");

        final List<Object> processedListForAdapter = processLessonsForAdapter(filteredLessons);

        // Обновление адаптера всегда на UI потоке
        runOnUiThread(() -> {
            fullScheduleAdapter.setScheduleData(processedListForAdapter);
            checkAndShowNoDataMessage(processedListForAdapter);
        });
    }

    private List<Object> processLessonsForAdapter(List<Lesson> lessons) {
        List<Object> items = new ArrayList<>();
        if (lessons == null || lessons.isEmpty()) {
            return items;
        }
        String lastDate = null;
        for (Lesson lesson : lessons) {
            String currentDateHeader = lesson.getDate() + " " + lesson.getDayOfWeek();
            if (lastDate == null || !lastDate.equals(currentDateHeader)) {
                items.add(currentDateHeader);
                lastDate = currentDateHeader;
            }
            items.add(lesson);
        }
        return items;
    }

    private void checkAndShowNoDataMessage(List<Object> items) {
        if (items.isEmpty()) {
            recyclerViewFullSchedule.setVisibility(View.GONE);
            textViewNoLessonsInSchedule.setVisibility(View.VISIBLE);
            textViewNoLessonsInSchedule.setText(currentGroupFilter.equals(ALL_GROUPS_FILTER) ?
                    "В этом расписании нет занятий." :
                    "Нет занятий для группы «" + currentGroupFilter + "».");
        } else {
            recyclerViewFullSchedule.setVisibility(View.VISIBLE);
            textViewNoLessonsInSchedule.setVisibility(View.GONE);
        }
    }

    // Реализация OnLessonClickListener
    @Override
    public void onLessonClick(Lesson lesson, int adapterPosition) {
        Log.d(TAG, "Нажатие на занятие: " + lesson.getSubjectName() + " (ID: " + lesson.getId() + "), позиция в адаптере: " + adapterPosition);
        showNoteDialog(lesson, adapterPosition);
    }

    private void showNoteDialog(final Lesson lesson, final int adapterPosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String title = (lesson.getSubjectName() != null && !lesson.getSubjectName().isEmpty()) ? lesson.getSubjectName() : "Занятие";
        builder.setTitle("Заметка и Напоминание"); // Обновили заголовок диалога

        // Создаем LinearLayout для размещения EditText и кнопок напоминаний
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20); // Отступы

        final EditText inputNote = new EditText(this);
        inputNote.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        inputNote.setMinLines(3);
        inputNote.setHint("Введите текст заметки...");
        inputNote.setText(lesson.getNote() != null ? lesson.getNote() : "");
        layout.addView(inputNote);

        // Кнопка "Установить/Изменить напоминание"
        Button btnSetReminder = new Button(this);
        btnSetReminder.setText(lesson.isReminderActive() ?
                "Изменить напоминание (за " + lesson.getReminderOffsetMinutes() + " мин)" :
                "Установить напоминание");
        btnSetReminder.setOnClickListener(v -> {
            // Здесь будет логика вызова диалога выбора времени смещения
            Toast.makeText(this, "TODO: Открыть диалог выбора времени напоминания", Toast.LENGTH_SHORT).show();
            // dialog.dismiss(); // Возможно, нужно будет закрыть текущий диалог перед открытием нового
        });
        layout.addView(btnSetReminder);

        // Кнопка "Отменить напоминание" (видима, если напоминание установлено)
        Button btnCancelReminder = new Button(this);
        btnCancelReminder.setText("Отменить напоминание");
        if (lesson.isReminderActive()) {
            btnCancelReminder.setVisibility(View.VISIBLE);
        } else {
            btnCancelReminder.setVisibility(View.GONE);
        }
        btnCancelReminder.setOnClickListener(v -> {
            // Здесь будет логика отмены напоминания
            Toast.makeText(this, "TODO: Отменить напоминание", Toast.LENGTH_SHORT).show();
            // lesson.setReminderActive(false);
            // lesson.setReminderOffsetMinutes(-1);
            // updateLessonInDb(lesson, adapterPosition); // Обновить урок в БД
            // dialog.dismiss();
        });
        layout.addView(btnCancelReminder);

        builder.setView(layout); // Устанавливаем наш кастомный layout с EditText и кнопками

        // Кнопка "Сохранить заметку" (для заметки)
        builder.setPositiveButton("Сохранить заметку", (dialog, which) -> {
            String noteText = inputNote.getText().toString().trim();
            lesson.setNote(noteText.isEmpty() ? null : noteText);
            updateLessonInDb(lesson, adapterPosition); // Этот метод у нас уже есть для сохранения урока
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());

        // Кнопку "Удалить заметку" можно оставить или убрать, если диалог становится слишком загруженным
        if (lesson.getNote() != null && !lesson.getNote().isEmpty()){
            builder.setNeutralButton("Удалить заметку", (dialog, which) -> {
                lesson.setNote(null);
                updateLessonInDb(lesson, adapterPosition);
            });
        }
        builder.show();
    }

    private void updateLessonInDb(final Lesson lessonToUpdate, final int adapterPosition) {
        executorService.execute(() -> {
            lessonDao.update(lessonToUpdate);
            Log.d(TAG, "Заметка для занятия ID " + lessonToUpdate.getId() + " обновлена. Новая заметка: '" + lessonToUpdate.getNote() + "'");

            // Обновляем объект в нашем основном списке allLessonsForCurrentSchedule
            boolean foundAndUpdated = false;
            for (int i = 0; i < allLessonsForCurrentSchedule.size(); i++) {
                if (allLessonsForCurrentSchedule.get(i).getId() == lessonToUpdate.getId()) {
                    allLessonsForCurrentSchedule.set(i, lessonToUpdate); // Заменяем старый объект новым
                    foundAndUpdated = true;
                    break;
                }
            }
            if (!foundAndUpdated) {
                Log.w(TAG, "Обновленный урок (ID: " + lessonToUpdate.getId() +") не найден в кеше allLessonsForCurrentSchedule для обновления.");
            }

            runOnUiThread(() -> {
                Toast.makeText(ScheduleViewActivity.this, "Заметка сохранена", Toast.LENGTH_SHORT).show();
                // Перестраиваем и перерисовываем список, чтобы отразить изменения
                // (например, если бы у нас был индикатор заметки, он бы обновился)
                filterAndDisplayLessons();
            });
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
