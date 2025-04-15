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

import java.text.DecimalFormat;
import java.util.ArrayList;

public class ShowmoreAdapter extends RecyclerView.Adapter<ShowmoreAdapter.MyViewHolder> {
    Context context;
    ArrayList<Room> list;

    public ShowmoreAdapter(Context context, ArrayList<Room> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.view_holder_room,parent,false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        DecimalFormat decimalFormat = new DecimalFormat("#,###.###");
        decimalFormat.setDecimalSeparatorAlwaysShown(false);
        Room room = list.get(position);
        holder.title_room.setText(room.getTitle_room());
        holder.price_room.setText(decimalFormat.format(room.getPrice_room()));
        holder.area_room.setText(String.valueOf(room.getArea_room()));
        holder.people_room.setText(String.valueOf(room.getPerson_in_room()));

        Glide.with(context).load(room.getFirstImage()).centerCrop().into(holder.img_post);

        Address address = room.getAddress();
        holder.city.setText(address.getCity());
        holder.district.setText(address.getDistrict());
        holder.detail.setText(address.getDetail());
        holder.cardViewRoom.setOnClickListener(v -> {
            Intent detailRoom = new Intent(context, DetailRoomActivity.class);
            detailRoom.putExtra("DataRoom", room.toString());
            context.startActivity(detailRoom);
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{

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

