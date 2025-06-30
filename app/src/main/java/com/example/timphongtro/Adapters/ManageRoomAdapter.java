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
import com.example.timphongtro.Activities.RoomDetailActivity;
import com.example.timphongtro.Activities.EditPostActivity;
import com.example.timphongtro.Models.Address;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.R;
import com.example.timphongtro.Utils.GsonUtils;
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
                holder.detail.setText(address.getDetail() != null ? address.getDetail() : "Không có địa chỉ chi tiết");
            } else {
                holder.city.setText("");
                holder.district.setText("");
                holder.detail.setText("Địa chỉ không xác định");
            }

            holder.constraintViewDetail.setOnClickListener(v -> {
                String roomJson = GsonUtils.toJson(room);
                if (roomJson != null) {
                    Intent detailRoom = new Intent(context, RoomDetailActivity.class);
                    detailRoom.putExtra("DataRoom", roomJson);
                    context.startActivity(detailRoom);
                } else {
                    Toast.makeText(context, "Lỗi mở chi tiết phòng", Toast.LENGTH_SHORT).show();
                }
            });

            holder.textViewDelete.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Xác nhận xóa")
                        .setMessage("Bạn chắc chắn muốn xóa phòng \"" + room.getTitle_room() + "\" không?\n\nViệc này không thể hoàn tác.")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            deleteRoom(room, holder);
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });

            holder.textViewEdit.setOnClickListener(v -> {
                String roomJson = GsonUtils.toJson(room);
                if (roomJson != null) {
                    Intent updateRoom = new Intent(context, EditPostActivity.class);
                    updateRoom.putExtra("DataRoom", roomJson);
                    context.startActivity(updateRoom);
                } else {
                    Toast.makeText(context, "Lỗi mở chỉnh sửa phòng", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void deleteRoom(Room room, MyViewHolder holder) {
        if (room == null || room.getId_room() == null) {
            Toast.makeText(context, "Lỗi: Thông tin phòng không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        holder.textViewDelete.setEnabled(false);
        holder.textViewDelete.setText("Đang xóa...");
        
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference roomRef = database.getReference("Rooms/" + room.getId_room());
        
        roomRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    int currentPosition = holder.getBindingAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION && currentPosition < list.size()) {
                        list.remove(currentPosition);
                        notifyItemRemoved(currentPosition);
                        notifyItemRangeChanged(currentPosition, list.size());
                    }
                    
                    Toast.makeText(context, "Xóa phòng thành công", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    holder.textViewDelete.setEnabled(true);
                    holder.textViewDelete.setText("Xóa");
                    
                    Toast.makeText(context, "Xóa phòng thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
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
