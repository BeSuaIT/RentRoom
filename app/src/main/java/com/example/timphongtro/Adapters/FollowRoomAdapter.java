package com.example.timphongtro.Adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.timphongtro.Activities.PostDetailActivity;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.R;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class FollowRoomAdapter extends RecyclerView.Adapter<FollowRoomAdapter.ViewHolder> {
    private final Context context;
    private final ArrayList<Room> rooms;
    private final DecimalFormat decimalFormat;

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
            Glide.with(context)
                    .load(room.getFirstImage())
                    .placeholder(R.drawable.img_no_image)
                    .into(holder.roomImage);
        }

        holder.roomTitle.setText(room.getTitle_room());
        holder.roomPrice.setText(String.format("%s đ/tháng",
                decimalFormat.format(room.getPrice_room())));
        holder.roomAddress.setText(room.getAddress().getAddress_combine());

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
            Intent intent = new Intent(context, PostDetailActivity.class);
            intent.putExtra("DataRoom", room.toString());
            context.startActivity(intent);
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
