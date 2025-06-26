package com.example.timphongtro.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.timphongtro.Activities.PostDetailActivity;
import com.example.timphongtro.Models.Address;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.R;
import com.example.timphongtro.Utils.GsonUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.MyViewHolder> {

    Context context;
    ArrayList<Room> list;
    int maxitemcount = 10;
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    public RoomAdapter(Context context, ArrayList<Room> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.view_holder_room, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        DecimalFormat decimalFormat = new DecimalFormat("#,###.###");
        decimalFormat.setDecimalSeparatorAlwaysShown(false);
        
        Room room = list.get(position);

        holder.title_room.setText(room.getTitle_room() != null ? room.getTitle_room() : "Phòng trọ");
        holder.price_room.setText(decimalFormat.format(room.getPrice_room()));
        holder.area_room.setText(String.valueOf(room.getArea_room()));
        holder.people_room.setText(String.valueOf(room.getPerson_in_room()));

        ArrayList<String> images = room.getImages();
        if (images != null && !images.isEmpty()) {
            String firstImage = images.get(0);
            if (firstImage != null && !firstImage.isEmpty()) {
                Glide.with(context.getApplicationContext())
                        .load(firstImage)
                        .placeholder(R.drawable.loading)
                        .error(R.drawable.img_no_image)
                        .centerCrop()
                        .into(holder.img_post);
            } else {
                holder.img_post.setImageResource(R.drawable.img_no_image);
            }
        } else {
            holder.img_post.setImageResource(R.drawable.img_no_image);
        }

        Address address = room.getAddress();
        if (address != null) {
            holder.city.setText(address.getCity() != null ? address.getCity() : "");
            holder.district.setText(address.getDistrict() != null ? address.getDistrict() : "");
            holder.detail.setText(address.getDetail() != null ? address.getDetail() : "");
        } else {
            holder.city.setText("");
            holder.district.setText("");
            holder.detail.setText("Địa chỉ không xác định");
        }

        holder.cardViewRoom.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null) {
                saveToUserHistory(currentUser.getUid(), room.getId_room());
            }

            Intent detailRoom = new Intent(context, PostDetailActivity.class);
            String roomJson = GsonUtils.toJson(room);
            if (roomJson != null) {
                detailRoom.putExtra("DataRoom", roomJson);
                context.startActivity(detailRoom);
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

        userHistoryRef.setValue(currentTimestamp);
    }

    @Override
    public int getItemCount() {
        return Math.min(list.size(), maxitemcount);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView people_room, price_room, area_room, city, district, detail, title_room;
        CardView cardViewRoom;
        ImageView img_post;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            title_room = itemView.findViewById(R.id.PostTitle);
            price_room = itemView.findViewById(R.id.RoomCost);
            city = itemView.findViewById(R.id.CityName);
            area_room = itemView.findViewById(R.id.DienTich);
            district = itemView.findViewById(R.id.DistrictName);
            detail = itemView.findViewById(R.id.DetailName);
            people_room = itemView.findViewById(R.id.Size);
            cardViewRoom = itemView.findViewById(R.id.cardViewRoom);
            img_post = itemView.findViewById(R.id.img_post);
        }
    }
}

