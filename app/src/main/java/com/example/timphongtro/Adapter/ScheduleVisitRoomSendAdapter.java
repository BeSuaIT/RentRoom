package com.example.timphongtro.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Activity.ScheduleVisitDetailActivity;
import com.example.timphongtro.Entity.ScheduleVisitRoomClass;
import com.example.timphongtro.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.util.ArrayList;

public class ScheduleVisitRoomSendAdapter extends RecyclerView.Adapter<ScheduleVisitRoomSendAdapter.ViewHolder> {
    Context context;
    ArrayList<ScheduleVisitRoomClass> schedules;
    ScheduleVisitRoomClass scheduleData;
    Intent detailSchedule;

    public ScheduleVisitRoomSendAdapter(Context context, ArrayList<ScheduleVisitRoomClass> schedules) {
        this.context = context;
        this.schedules = schedules;
    }

    @NonNull
    @Override
    public ScheduleVisitRoomSendAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.view_holder_schedule_visit_room, parent, false);
        return new ScheduleVisitRoomSendAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleVisitRoomSendAdapter.ViewHolder holder, int position) {
        FirebaseUser user;
        user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference scheduleRef = FirebaseDatabase.getInstance().getReference("MeetingSchedules");
        ScheduleVisitRoomClass schedule = schedules.get(position);
        scheduleData = new ScheduleVisitRoomClass();
        detailSchedule = new Intent(context, ScheduleVisitDetailActivity.class);
        if (schedule != null && user != null) {
            holder.tvName.setText(schedule.getName());
            holder.circleImageView.setText(getFirstLetter(schedule.getName()));
            holder.tvTime.setText(schedule.getTimeVisitRoom());
            String note = schedule.getNote();
            if (schedule.getNote().length() >= 20) {
                note = schedule.getNote().substring(0, 20) + "...";
            }
            holder.tvNote.setText(note);
            if ("0".equals(schedule.getStatus()) && user.getUid().equals(schedule.getIdFrom())) {
                detailSchedule.putExtra("showbtn", 0);
                if ("0".equals(schedule.getStatus())) {
                    Drawable drawable = ContextCompat.getDrawable(context, R.drawable.send);
                    holder.tvStatus.setBackground(drawable);
                    holder.tvStatus.setText(R.string.send);
                } else if ("1".equals(schedule.getStatus())) {
                    Drawable drawable = ContextCompat.getDrawable(context, R.drawable.approved);
                    holder.tvStatus.setBackground(drawable);
                    holder.tvStatus.setText(R.string.confirmed);
                } else if ("2".equals(schedule.getStatus())) {
                    Drawable drawable = ContextCompat.getDrawable(context, R.drawable.avatar);
                    holder.tvStatus.setBackground(drawable);
                    holder.tvStatus.setText(R.string.refuse);
                } else {
                    //khong lam gi ca
                }
            } else if ("0".equals(schedule.getStatus()) && user.getUid().equals(schedule.getIdTo())) {
                detailSchedule.putExtra("showbtn", 1);
                Drawable drawable = ContextCompat.getDrawable(context, R.drawable.confirm_wait);
                holder.tvStatus.setBackground(drawable);
                holder.tvStatus.setText(R.string.confirm);
//            set onclick trong nay
                holder.tvStatus.setOnClickListener(v -> {
                    if ("0".equals(schedule.getStatus()) && user.getUid().equals(schedule.getIdTo())) {

                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Xác nhận");
                    builder.setMessage("Bạn có xác nhận lịch hẹn này không?");

                    // Thiết lập nút Có (Yes) và xử lý sự kiện khi người dùng nhấn nút Có
                    builder.setPositiveButton("Xác nhận", (dialog, which) -> {
                        // Xử lý khi người dùng chọn Có
                        // Thêm mã xử lý ở đây
                        scheduleRef.child(schedule.getIdSchedule()).child("status").setValue("1").addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Toast.makeText(context, "Bạn xác nhận thành công", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });

                    // Thiết lập nút Không (No) và xử lý sự kiện khi người dùng nhấn nút Không
                    builder.setNegativeButton("Từ chối", (dialog, which) -> {
                        // Xử lý khi người dùng chọn Không
                        // Thêm mã xử lý ở đây
                        scheduleRef.child(schedule.getIdSchedule()).child("status").setValue("2").addOnSuccessListener(unused -> Toast.makeText(context, "Bạn từ chối thành công", Toast.LENGTH_SHORT).show());
                    });

                    builder.setNeutralButton("Hủy", (dialog, which) -> {

                    });

                    // Tạo và hiển thị hộp thoại
                    AlertDialog dialog = builder.create();
                    dialog.show();
                });

            } else if ("1".equals(schedule.getStatus()) && user.getUid().equals(schedule.getIdTo())) {
                detailSchedule.putExtra("showbtn", 0);
                Drawable drawable = ContextCompat.getDrawable(context, R.drawable.approved);
                holder.tvStatus.setBackground(drawable);
                holder.tvStatus.setText(R.string.confirmed);
            } else if ("2".equals(schedule.getStatus()) && user.getUid().equals(schedule.getIdTo())) {
                detailSchedule.putExtra("showbtn", 0);
                Drawable drawable = ContextCompat.getDrawable(context, R.drawable.avatar);
                holder.tvStatus.setBackground(drawable);
                holder.tvStatus.setText(R.string.refuse);
            } else {
                detailSchedule.putExtra("showbtn", 0);
                //khong lam gi ca
                if ("0".equals(schedule.getStatus())) {
                    Drawable drawable = ContextCompat.getDrawable(context, R.drawable.send);
                    holder.tvStatus.setBackground(drawable);
                    holder.tvStatus.setText(R.string.send);
                } else if ("1".equals(schedule.getStatus())) {
                    Drawable drawable = ContextCompat.getDrawable(context, R.drawable.approved);
                    holder.tvStatus.setBackground(drawable);
                    holder.tvStatus.setText(R.string.confirmed);
                } else if ("2".equals(schedule.getStatus())) {
                    Drawable drawable = ContextCompat.getDrawable(context, R.drawable.avatar);
                    holder.tvStatus.setBackground(drawable);
                    holder.tvStatus.setText(R.string.refuse);
                } else {
                    //khong lam gi ca
                }
            }
            holder.detail_schedule.setOnClickListener(v -> {
                new Gson();
                String scheduleString = schedule.toString();
                detailSchedule.putExtra("scheduleData", scheduleString);
                context.startActivity(detailSchedule);
            });
        }
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTime, tvNote, tvStatus, circleImageView;
        LinearLayout detail_schedule;

        public ViewHolder(@NonNull View view) {
            super(view);
            detail_schedule = view.findViewById(R.id.detail_schedule);
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
