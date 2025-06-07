package com.student.portativestudentdiary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import android.widget.ImageView;

public class FullScheduleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DATE_HEADER = 0;
    private static final int TYPE_LESSON_ITEM = 1;

    private List<Object> items = new ArrayList<>();
    private OnLessonClickListener lessonClickListener; // Слушатель нажатий на элемент Lesson

    // Интерфейс для обработки нажатий на элемент Lesson
    public interface OnLessonClickListener {
        void onLessonClick(Lesson lesson, int adapterPosition);
    }

    // Конструктор теперь принимает слушателя
    public FullScheduleAdapter(OnLessonClickListener listener) {
        this.lessonClickListener = listener;
    }

    // Метод для установки или обновления данных в адаптере
    public void setScheduleData(List<Object> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof String) {
            return TYPE_DATE_HEADER;
        } else if (items.get(position) instanceof Lesson) {
            return TYPE_LESSON_ITEM;
        }
        return super.getItemViewType(position); // На всякий случай, хотя этого не должно происходить
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_DATE_HEADER) {
            View view = inflater.inflate(R.layout.list_item_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.list_item_lesson, parent, false);
            // Передаем слушателя и текущий список items в ViewHolder элемента Lesson
            return new LessonItemViewHolder(view, lessonClickListener, items);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        if (viewType == TYPE_DATE_HEADER) {
            DateHeaderViewHolder dateHolder = (DateHeaderViewHolder) holder;
            String dateString = (String) items.get(position);
            dateHolder.bind(dateString);
        } else {
            LessonItemViewHolder lessonHolder = (LessonItemViewHolder) holder;
            Lesson currentLesson = (Lesson) items.get(position);
            lessonHolder.bind(currentLesson);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolder для заголовка даты
    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView textViewDateHeader;

        public DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDateHeader = itemView.findViewById(R.id.textView_date_header);
        }

        void bind(String dateText) {
            textViewDateHeader.setText(dateText);
        }
    }

    // ViewHolder для элемента занятия
    static class LessonItemViewHolder extends RecyclerView.ViewHolder {
        TextView textViewLessonTime;
        TextView textViewSubjectName;
        TextView textViewLessonType;
        TextView textViewClassroom;
        TextView textViewTeacherName;
        TextView textViewLessonNumberAndShift;
        TextView textViewLessonGroup;
        ImageView imageViewNoteIndicator;

        public LessonItemViewHolder(@NonNull View itemView, final OnLessonClickListener listener, final List<Object> adapterItems) {
            super(itemView);
            textViewLessonTime = itemView.findViewById(R.id.textView_lesson_time);
            textViewSubjectName = itemView.findViewById(R.id.textView_subject_name);
            textViewLessonType = itemView.findViewById(R.id.textView_lesson_type);
            textViewClassroom = itemView.findViewById(R.id.textView_classroom);
            textViewTeacherName = itemView.findViewById(R.id.textView_teacher_name);
            textViewLessonNumberAndShift = itemView.findViewById(R.id.textView_lesson_number_and_shift);

            textViewLessonGroup = itemView.findViewById(R.id.textView_lesson_group);

            imageViewNoteIndicator = itemView.findViewById(R.id.imageView_note_indicator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                // Проверяем, что позиция валидна и слушатель не null
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    Object item = adapterItems.get(position);
                    // Убеждаемся, что кликнули именно на элемент типа Lesson (а не на заголовок даты)
                    if (item instanceof Lesson) {
                        listener.onLessonClick((Lesson) item, position);
                    }
                }
            });
        }

        void bind(Lesson lesson) {
            textViewLessonTime.setText(lesson.getStartTime() + " - " + lesson.getEndTime());
            textViewSubjectName.setText(lesson.getSubjectName());

            String lessonType = lesson.getLessonType();
            if (lessonType != null && !lessonType.isEmpty()) {
                textViewLessonType.setText(lessonType);
                textViewLessonType.setVisibility(View.VISIBLE);
            } else {
                textViewLessonType.setVisibility(View.GONE);
            }

            textViewClassroom.setText("Ауд: " + (lesson.getClassroom() != null && !lesson.getClassroom().isEmpty() ? lesson.getClassroom() : "---"));
            textViewTeacherName.setText(lesson.getTeacherName() != null && !lesson.getTeacherName().isEmpty() ? lesson.getTeacherName() : "---");
            textViewLessonNumberAndShift.setText("Пара " + lesson.getLessonNumber() + " (" + lesson.getShiftNumber() + " см.)");

            // Отображение идентификатора группы (если соответствующий TextView есть в макете)
            if (textViewLessonGroup != null) {
                String groupIdentifier = lesson.getGroupIdentifier();
                if (groupIdentifier != null && !groupIdentifier.isEmpty()) {
                    textViewLessonGroup.setText("(Гр. " + groupIdentifier + ")");
                    textViewLessonGroup.setVisibility(View.VISIBLE);
                } else {
                    textViewLessonGroup.setVisibility(View.GONE);
                }
            }
            if (imageViewNoteIndicator != null) {
                if (lesson.getNote() != null && !lesson.getNote().trim().isEmpty()) {
                    imageViewNoteIndicator.setVisibility(View.VISIBLE);
                } else {
                    imageViewNoteIndicator.setVisibility(View.GONE);
                }
            }

        }
    }
}
