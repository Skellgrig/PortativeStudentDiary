package com.student.portativestudentdiary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class LessonAdapter extends RecyclerView.Adapter<LessonAdapter.LessonViewHolder> {

    private List<Lesson> lessonsList = new ArrayList<>();

    // Метод для обновления списка занятий в адаптере
    public void setLessons(List<Lesson> lessons) {
        this.lessonsList = lessons;
        notifyDataSetChanged(); // Уведомляем адаптер, что данные изменились (для простоты)
        // В реальных приложениях лучше использовать DiffUtil для производительности
    }

    @NonNull
    @Override
    public LessonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // "Надуваем" (создаем) View из нашего XML-макета list_item_lesson.xml
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_lesson, parent, false);
        return new LessonViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull LessonViewHolder holder, int position) {
        // Получаем текущее занятие из списка
        Lesson currentLesson = lessonsList.get(position);

        // Заполняем View данными из объекта currentLesson
        holder.textViewLessonTime.setText(currentLesson.getStartTime() + " - " + currentLesson.getEndTime());
        holder.textViewSubjectName.setText(currentLesson.getSubjectName());

        String lessonType = currentLesson.getLessonType();
        if (lessonType != null && !lessonType.isEmpty()) {
            holder.textViewLessonType.setText(lessonType);
            holder.textViewLessonType.setVisibility(View.VISIBLE);
        } else {
            holder.textViewLessonType.setVisibility(View.GONE);
        }

        holder.textViewClassroom.setText("Ауд: " + (currentLesson.getClassroom() != null ? currentLesson.getClassroom() : "---"));
        holder.textViewTeacherName.setText(currentLesson.getTeacherName() != null ? currentLesson.getTeacherName() : "---");
        holder.textViewLessonNumberAndShift.setText("Пара " + currentLesson.getLessonNumber() + " (" + currentLesson.getShiftNumber() + " см.)");
    }

    @Override
    public int getItemCount() {
        // Возвращает общее количество элементов в списке
        return lessonsList.size();
    }

    // ViewHolder описывает один элемент списка и содержит ссылки на его View (текстовые поля и т.д.)
    static class LessonViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewLessonTime;
        private TextView textViewSubjectName;
        private TextView textViewLessonType;
        private TextView textViewClassroom;
        private TextView textViewTeacherName;
        private TextView textViewLessonNumberAndShift;

        public LessonViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewLessonTime = itemView.findViewById(R.id.textView_lesson_time);
            textViewSubjectName = itemView.findViewById(R.id.textView_subject_name);
            textViewLessonType = itemView.findViewById(R.id.textView_lesson_type);
            textViewClassroom = itemView.findViewById(R.id.textView_classroom);
            textViewTeacherName = itemView.findViewById(R.id.textView_teacher_name);
            textViewLessonNumberAndShift = itemView.findViewById(R.id.textView_lesson_number_and_shift);


        }
    }
}
