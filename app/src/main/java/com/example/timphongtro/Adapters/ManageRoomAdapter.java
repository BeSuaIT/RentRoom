package com.example.timphongtro.Adapters;

import android.app.AlertDialog;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.timphongtro.Activities.DetailRoomActivity;
import com.example.timphongtro.Activities.EditPostActivity;
import com.example.timphongtro.Models.Address;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class ManageRoomAdapter extends RecyclerView.Adapter<ManageRoomAdapter.MyViewHolder> {
    ArrayList<Room> list;
    Context context;

    public ManageRoomAdapter(ArrayList<Room> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public ManageRoomAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.view_holder_manage_room, parent, false);
        return new ManageRoomAdapter.MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ManageRoomAdapter.MyViewHolder holder, int position) {
        DecimalFormat decimalFormat = new DecimalFormat("#,###.###");
        decimalFormat.setDecimalSeparatorAlwaysShown(false);
        Room room = list.get(position);
        if (room != null) {
            holder.title_room.setText(room.getTitle_room());
            holder.price_room.setText(decimalFormat.format(room.getPrice_room()));
            holder.area_room.setText(String.valueOf(room.getArea_room()));
            holder.people_room.setText(String.valueOf(room.getPerson_in_room()));
            Glide.with(context).load(room.getFirstImage()).centerCrop().into(holder.img_post);

            Address address = room.getAddress();
            holder.city.setText(address.getCity());
            holder.district.setText(address.getDistrict());
            holder.detail.setText(address.getDetail());
            
            holder.constraintViewDetail.setOnClickListener(v -> {
                Intent detailRoom = new Intent(context, DetailRoomActivity.class);
                detailRoom.putExtra("DataRoom", room.toString());
                context.startActivity(detailRoom);
            });

            holder.textViewDelete.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Xác nhận")
                        .setMessage("Bạn chắc chắn muốn xóa không?")
                        .setPositiveButton("Có", (dialog, which) -> {
                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            DatabaseReference myRef = database.getReference("Posts/" + room.getId_room());
                            myRef.removeValue().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    int currentPosition = holder.getBindingAdapterPosition();
                                    if (currentPosition != RecyclerView.NO_POSITION) {
                                        list.remove(currentPosition);
                                        notifyItemRemoved(currentPosition);
                                        notifyItemRangeChanged(currentPosition, list.size());
                                    }
                                    Toast.makeText(context, "Xóa bài thành công", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, "Xóa bài thất bại", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }).setNegativeButton("Không", (dialog, which) -> {
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            });

            holder.textViewEdit.setOnClickListener(v -> {
                Intent updateRoom = new Intent(context, EditPostActivity.class);
                updateRoom.putExtra("DataRoom", room.toString());
                context.startActivity(updateRoom);
            });
        }
    }

    @Override
    public int getItemCount() {
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView people_room, price_room, area_room, city, district, detail, title_room, textViewEdit, textViewDelete;
        CardView cardViewRoom;
        ImageView img_post;
        ConstraintLayout constraintViewDetail;

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
            textViewEdit = itemView.findViewById(R.id.textViewEdit);
            textViewDelete = itemView.findViewById(R.id.textViewDelete);
            constraintViewDetail = itemView.findViewById(R.id.constraintViewDetail);
        }
    }
}
