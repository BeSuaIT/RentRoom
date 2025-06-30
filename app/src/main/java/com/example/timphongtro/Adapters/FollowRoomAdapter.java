package com.example.timphongtro.Adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.timphongtro.Activities.RoomDetailActivity;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.R;
import com.example.timphongtro.Utils.GsonUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class FollowRoomAdapter extends RecyclerView.Adapter<FollowRoomAdapter.ViewHolder> {
    private final Context context;
    private final ArrayList<Room> rooms;
    private final DecimalFormat decimalFormat;
    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    public FollowRoomAdapter(Context context, ArrayList<Room> rooms) {
        this.context = context;
        this.rooms = rooms;
        this.decimalFormat = new DecimalFormat("#,###");
        this.decimalFormat.setDecimalSeparatorAlwaysShown(false);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_holder_follow_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Room room = rooms.get(position);

        if (room.getImages() != null && !room.getImages().isEmpty()) {
            try {
                Glide.with(context.getApplicationContext())
                        .load(room.getFirstImage())
                        .placeholder(R.drawable.img_no_image)
                        .error(R.drawable.img_no_image)
                        .into(holder.roomImage);
            } catch (Exception e) {
                holder.roomImage.setImageResource(R.drawable.img_no_image);
            }
        } else {
            holder.roomImage.setImageResource(R.drawable.img_no_image);
        }

        holder.roomTitle.setText(room.getTitle_room() != null ? room.getTitle_room() : "Phòng trọ");
        holder.roomPrice.setText(String.format("%s đ/tháng",
                decimalFormat.format(room.getPrice_room())));

        if (room.getAddress() != null && room.getAddress().getAddress_combine() != null) {
            holder.roomAddress.setText(room.getAddress().getAddress_combine());
        } else {
            holder.roomAddress.setText("Địa chỉ không xác định");
        }

        if (room.getStatus_room() == 0) {
            holder.roomStatus.setText("Còn trống");
            holder.roomStatus.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.status_available)));
        } else {
            holder.roomStatus.setText("Đã cho thuê");
            holder.roomStatus.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.status_unavailable)));
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RoomDetailActivity.class);

            String roomJson = GsonUtils.toJson(room);
            if (roomJson != null) {
                intent.putExtra("DataRoom", roomJson);
                context.startActivity(intent);

                FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                if (currentUser != null && room.getId_room() != null) {
                    saveToUserHistory(currentUser.getUid(), room.getId_room());
                }
            } else {
                Toast.makeText(context, "Lỗi mở chi tiết phòng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveToUserHistory(String userId, String roomId) {
        if (userId == null || userId.isEmpty() || roomId == null || roomId.isEmpty()) {
            return;
        }

        long currentTimestamp = System.currentTimeMillis();

        DatabaseReference userHistoryRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(userId)
                .child("histories")
                .child(roomId);

        userHistoryRef.setValue(currentTimestamp)
                .addOnSuccessListener(unused -> {
                })
                .addOnFailureListener(e -> {
                });
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView roomImage;
        TextView roomTitle, roomPrice, roomAddress, roomStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            roomImage = itemView.findViewById(R.id.roomImage);
            roomTitle = itemView.findViewById(R.id.roomTitle);
            roomPrice = itemView.findViewById(R.id.roomPrice);
            roomAddress = itemView.findViewById(R.id.roomAddress);
            roomStatus = itemView.findViewById(R.id.roomStatus);
        }
    }
}
