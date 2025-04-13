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
import com.example.timphongtro.Activities.DetailRoomActivity;
import com.example.timphongtro.Models.Address;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.MyViewHolder> {
    private final Context context;
    private ArrayList<Room> list;
    private final FirebaseAuth firebaseAuth;
    private final DecimalFormat decimalFormat;

    public SearchAdapter(Context context, ArrayList<Room> list) {
        this.context = context;
        this.list = list;
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.decimalFormat = new DecimalFormat("#,###");
        this.decimalFormat.setDecimalSeparatorAlwaysShown(false);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.room_view_holder, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Room room = list.get(position);

        holder.title_room.setText(room.getTitle_room());
        holder.price_room.setText(decimalFormat.format(room.getPrice_room()));
        holder.area_room.setText(String.valueOf(room.getArea_room()));
        holder.people_room.setText(String.valueOf(room.getPerson_in_room()));

        if (room.getImages() != null && room.getFirstImage() != null) {
            Glide.with(context)
                    .load(room.getFirstImage())
                    .placeholder(R.drawable.img_no_image)
                    .error(R.drawable.img_no_image)
                    .centerCrop()
                    .into(holder.img_post);
        }

        Address address = room.getAddress();
        if (address != null) {
            holder.city.setText(address.getCity());
            holder.district.setText(address.getDistrict());
            holder.detail.setText(address.getDetail());
        }

        holder.cardViewRoom.setOnClickListener(v -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                saveToRecentlyRead(user.getUid(), room.getId_room());
            }
            navigateToDetail(room);
        });
    }

    private void saveToRecentlyRead(String userId, String roomId) {
        DatabaseReference historyRef = FirebaseDatabase.getInstance()
                .getReference("Histories")
                .child(userId)
                .child(roomId);

        historyRef.setValue(new Date().getTime())
                .addOnFailureListener(e -> {
                });
    }

    private void navigateToDetail(Room room) {
        Intent detailRoom = new Intent(context, DetailRoomActivity.class);
        detailRoom.putExtra("DataRoom", room.toString());
        context.startActivity(detailRoom);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void searchDataList(ArrayList<Room> searchList) {
        this.list = searchList;
        notifyDataSetChanged();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        final TextView title_room, price_room, area_room, city, district, detail, people_room;
        final ImageView img_post;
        final CardView cardViewRoom;

        MyViewHolder(@NonNull View itemView) {
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

