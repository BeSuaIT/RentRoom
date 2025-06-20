package com.example.timphongtro.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
 
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

public class ShowmoreAdapter extends RecyclerView.Adapter<ShowmoreAdapter.MyViewHolder> {
    Context context;
    ArrayList<Room> list;
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    FirebaseUser user = firebaseAuth.getCurrentUser();

    public ShowmoreAdapter(Context context, ArrayList<Room> list) {
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

        String firstImage = room.getFirstImage();
        if (firstImage != null && !firstImage.isEmpty()) {
            Glide.with(context)
                    .load(firstImage)
                    .placeholder(R.drawable.loading)
                    .error(R.drawable.img_no_image)
                    .centerCrop()
                    .into(holder.img_post);
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
            Intent detailRoom = new Intent(context, PostDetailActivity.class);

            String roomJson = GsonUtils.toJson(room);
            if (roomJson != null) {
                detailRoom.putExtra("DataRoom", roomJson);
                context.startActivity(detailRoom);
                saveToUserHistory(room.getId_room());
            } else {
                android.widget.Toast.makeText(context, "Lỗi mở chi tiết phòng", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveToUserHistory(String roomId) {
        if (user != null && roomId != null) {
            long timestamp = System.currentTimeMillis();
            
            DatabaseReference userHistoryRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(user.getUid())
                    .child("histories")
                    .child(roomId);
                    
            userHistoryRef.setValue(timestamp)
                    .addOnSuccessListener(unused -> {
                        android.util.Log.d("History", "Saved room " + roomId + " to user history");
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("History", "Failed to save history: " + e.getMessage());
                    });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView people_room, price_room, area_room, city, district, detail, title_room;
        ImageView img_post;
        CardView cardViewRoom;
        
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            title_room = itemView.findViewById(R.id.PostTitle);
            price_room = itemView.findViewById(R.id.RoomCost);
            city = itemView.findViewById(R.id.CityName);
            area_room = itemView.findViewById(R.id.DienTich);
            district = itemView.findViewById(R.id.DistrictName);
            detail = itemView.findViewById(R.id.DetailName);
            people_room = itemView.findViewById(R.id.Size);
            img_post = itemView.findViewById(R.id.img_post);
            cardViewRoom = itemView.findViewById(R.id.cardViewRoom);
        }
    }
}

