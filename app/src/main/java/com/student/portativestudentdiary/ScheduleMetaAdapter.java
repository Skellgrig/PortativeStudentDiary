package com.student.portativestudentdiary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleMetaAdapter extends RecyclerView.Adapter<ScheduleMetaAdapter.ScheduleMetaViewHolder> {

    private List<ScheduleMeta> schedulesList = new ArrayList<>();
    private OnScheduleClickListener listener;

    public interface OnScheduleClickListener {
        void onScheduleClick(ScheduleMeta scheduleMeta);
        // Можно добавить onScheduleLongClick для удаления, например
    }

    public ScheduleMetaAdapter(OnScheduleClickListener listener) {
        this.listener = listener;
    }

    public void setSchedules(List<ScheduleMeta> schedules) {
        this.schedulesList = schedules;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScheduleMetaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_schedule_meta, parent, false);
        return new ScheduleMetaViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleMetaViewHolder holder, int position) {
        ScheduleMeta currentSchedule = schedulesList.get(position);
        holder.textViewScheduleName.setText(currentSchedule.getName());

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String importDate = sdf.format(new Date(currentSchedule.getImportTimestamp()));
        String details = "Импортировано: " + importDate;
        if (currentSchedule.getSourceFileName() != null && !currentSchedule.getSourceFileName().isEmpty()) {
            details += ", Файл: " + currentSchedule.getSourceFileName();
        }
        holder.textViewScheduleDetails.setText(details);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onScheduleClick(currentSchedule);
            }
        });
    }

    @Override
    public int getItemCount() {
        return schedulesList.size();
    }

    static class ScheduleMetaViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewScheduleName;
        private TextView textViewScheduleDetails;

        public ScheduleMetaViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewScheduleName = itemView.findViewById(R.id.textView_schedule_name);
            textViewScheduleDetails = itemView.findViewById(R.id.textView_schedule_details);
        }
    }
}
