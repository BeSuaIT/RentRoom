package com.example.timphongtro.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Activities.ScheduleVisitDetailActivity;
import com.example.timphongtro.Models.ScheduleVisitRoomClass;
import com.example.timphongtro.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class ScheduleVisitRoomSendAdapter extends RecyclerView.Adapter<ScheduleVisitRoomSendAdapter.ViewHolder> {
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;

    Context context;
    ArrayList<ScheduleVisitRoomClass> schedules;

    public ScheduleVisitRoomSendAdapter(Context context, ArrayList<ScheduleVisitRoomClass> schedules) {
        this.context = context;
        this.schedules = schedules;
    }

    @NonNull
    @Override
    public ScheduleVisitRoomSendAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.view_holder_schedule_visit_room, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleVisitRoomSendAdapter.ViewHolder holder, int position) {
        ScheduleVisitRoomClass schedule = schedules.get(position);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (schedule == null || currentUser == null) return;

        holder.tvName.setText(schedule.getName());
        holder.circleImageView.setText(getFirstLetter(schedule.getName()));
        holder.tvTime.setText(schedule.getTimeVisitRoom());
        holder.tvNote.setText(schedule.getNote());

        setupStatusView(holder.tvStatus, schedule, currentUser.getUid());

        if (shouldShowConfirmButtons(schedule, currentUser.getUid())) {
            holder.tvStatus.setOnClickListener(v -> showConfirmationDialog(schedule));
        } else {
            holder.tvStatus.setOnClickListener(null);
        }

        holder.itemView.setOnClickListener(v -> navigateToDetail(schedule));
    }

    private void setupStatusView(TextView statusView, ScheduleVisitRoomClass schedule, String userId) {
        int backgroundColor;
        String statusText;
        boolean isReceiver = userId.equals(schedule.getIdTo());

        switch (Integer.parseInt(schedule.getStatus())) {
            case STATUS_PENDING:
                backgroundColor = isReceiver ? R.color.status_pending : R.color.status_sent;
                statusText = context.getString(isReceiver ? R.string.confirm : R.string.send);
                break;
            case STATUS_APPROVED:
                backgroundColor = R.color.status_approved;
                statusText = context.getString(R.string.confirmed);
                break;
            case STATUS_REJECTED:
                backgroundColor = R.color.status_rejected;
                statusText = context.getString(R.string.refuse);
                break;
            default:
                return;
        }

        statusView.setBackgroundResource(backgroundColor);
        statusView.setText(statusText);
    }

    private boolean shouldShowConfirmButtons(ScheduleVisitRoomClass schedule, String userId) {
        return STATUS_PENDING == Integer.parseInt(schedule.getStatus()) 
               && userId.equals(schedule.getIdTo());
    }

    private void showConfirmationDialog(ScheduleVisitRoomClass schedule) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.confirmation)
                .setMessage(R.string.confirm_schedule_message)
                .setPositiveButton(R.string.confirm, (dialog, which) -> 
                    updateScheduleStatus(schedule, STATUS_APPROVED))
                .setNegativeButton(R.string.reject, (dialog, which) -> 
                    updateScheduleStatus(schedule, STATUS_REJECTED))
                .setNeutralButton(R.string.cancel, null)
                .show();
    }

    private void updateScheduleStatus(ScheduleVisitRoomClass schedule, int status) {
        FirebaseDatabase.getInstance()
                .getReference("MeetingSchedules")
                .child(schedule.getIdSchedule())
                .child("status")
                .setValue(String.valueOf(status))
                .addOnSuccessListener(unused -> {
                    int messageRes = status == STATUS_APPROVED ? 
                            R.string.schedule_approved : R.string.schedule_rejected;
                    Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToDetail(ScheduleVisitRoomClass schedule) {
        Intent intent = new Intent(context, ScheduleVisitDetailActivity.class);
        intent.putExtra("scheduleData", schedule.toString());
        intent.putExtra("showbtn", shouldShowConfirmButtons(schedule, 
                FirebaseAuth.getInstance().getCurrentUser().getUid()) ? 1 : 0);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTime, tvNote, tvStatus, circleImageView;

        public ViewHolder(@NonNull View view) {
            super(view);
            circleImageView = view.findViewById(R.id.circleImageView);
            tvName = view.findViewById(R.id.tvName);
            tvTime = view.findViewById(R.id.tvTime);
            tvNote = view.findViewById(R.id.tvNote);
            tvStatus = view.findViewById(R.id.tvStatus);
        }
    }

    public static String getFirstLetter(String input) {
        String[] words = input.split(" ");
        StringBuilder result = new StringBuilder();

        if (words.length == 1) {
            // Nếu chuỗi chỉ có 1 từ, lấy chữ đầu từ đó
            result.append(words[0].charAt(0));
        } else {
            // Nếu chuỗi có nhiều từ, lấy chữ cái đầu của từ thứ 1 và 2
            for (int i = 0; i < 2; i++) {
                result.append(words[i].charAt(0));
            }
        }

        return result.toString().toUpperCase();
    }
}
