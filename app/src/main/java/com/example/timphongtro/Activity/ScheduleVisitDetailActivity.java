package com.example.timphongtro.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.timphongtro.Entity.Room;
import com.example.timphongtro.Entity.ScheduleVisitRoomClass;
import com.example.timphongtro.Entity.User;
import com.example.timphongtro.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

public class ScheduleVisitDetailActivity extends AppCompatActivity {
    ScheduleVisitRoomClass schedule;
    TextView tvName, tvTime, tvNote, PostTitle, RoomCost, CityName, DistrictName, DetailName, DienTich, Size, tvprofileDetail, textViewNameUser;
    ImageView img_post,imageViewBack;
    User user;
    Room room;
    LinearLayout userPost;
    CardView cardViewRoom;
    MaterialButton btnRefuse,btnAccept;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_visit_detail);

        //Schedule
        tvName = findViewById(R.id.tvName);
        tvTime = findViewById(R.id.tvTime);
        tvNote = findViewById(R.id.tvNote);
        btnRefuse = findViewById(R.id.btnRefuse);
        btnAccept = findViewById(R.id.btnAccept);
//        tvStatus = findViewById(R.id.tvStatus);

        //Room
        PostTitle = findViewById(R.id.PostTitle);
        RoomCost = findViewById(R.id.RoomCost);
        CityName = findViewById(R.id.CityName);
        DistrictName = findViewById(R.id.DistrictName);
        DetailName = findViewById(R.id.DetailName);
        DienTich = findViewById(R.id.DienTich);
        Size = findViewById(R.id.Size);
        img_post = findViewById(R.id.img_post);
        cardViewRoom = findViewById(R.id.cardViewRoom);
        //User
        tvprofileDetail = findViewById(R.id.tvprofileDetail);
        textViewNameUser = findViewById(R.id.textViewNameUser);
        userPost = findViewById(R.id.userPost);
        imageViewBack = findViewById(R.id.imageViewBack);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String scheduleString = bundle.getString("scheduleData");
            int showbtn = bundle.getInt("showbtn");
            Gson gson = new Gson();
            schedule = gson.fromJson(scheduleString, ScheduleVisitRoomClass.class);
            tvName.setText(schedule.getName());
            tvTime.setText(schedule.getTimeVisitRoom());
            tvNote.setText(schedule.getNote());
//            tvStatus.setText(schedule.getStatus());
            String typeRoom = "Tro";
            if (schedule.getTypeRoom() == 1) {
                typeRoom = "ChungCuMini";
            }

            if(showbtn == 0){
                btnRefuse.setVisibility(View.INVISIBLE);
                btnAccept.setVisibility(View.INVISIBLE);
            }

            DatabaseReference scheduleRef = FirebaseDatabase.getInstance().getReference("scheduleVisitRoom/");
            btnRefuse.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scheduleRef.child(schedule.getIdSchedule()).child("status").setValue("2").addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(ScheduleVisitDetailActivity.this, "Bạn từ chối thành công", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }
            });

            btnAccept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scheduleRef.child(schedule.getIdSchedule()).child("status").setValue("1").addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(ScheduleVisitDetailActivity.this, "Bạn xác nhận thành công", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }
            });

            DatabaseReference roomRef = FirebaseDatabase.getInstance().getReference("rooms/" + typeRoom + "/" + schedule.getIdRoom());
            roomRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        room = snapshot.getValue(Room.class);
                        if (room != null) {
                            PostTitle.setText(room.getTitle_room());
                            RoomCost.setText(String.valueOf(room.getPrice_room()));
                            DistrictName.setText(room.getAddress().getDistrict());
                            DetailName.setText(room.getAddress().getDetail());
                            DienTich.setText(room.getArea_room());
                            Size.setText(String.valueOf(room.getPerson_in_room()));
                            Glide.with(ScheduleVisitDetailActivity.this)
                                    .load(room.getImages().getImg1())
                                    .into(img_post);
                        }

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            DatabaseReference userOwnPostRef = FirebaseDatabase.getInstance().getReference("users/" + schedule.getIdFrom());
            userOwnPostRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        user = snapshot.getValue(User.class);
                        if (user != null) {
                            tvprofileDetail.setText(getFirstLetter(user.getName()));
                            textViewNameUser.setText(user.getName());
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            userPost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent userDetail = new Intent(ScheduleVisitDetailActivity.this, UserActivity.class);
                    userDetail.putExtra("id_own_post", schedule.getIdFrom());
                    startActivity(userDetail);
                }
            });


            cardViewRoom.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (room != null) {
                        String roomString = room.toString();
                        Intent roomDetail = new Intent(ScheduleVisitDetailActivity.this, DetailRoomActivity.class);
                        roomDetail.putExtra("DataRoom", roomString);
                        startActivity(roomDetail);
                    }
                }
            });

            imageViewBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
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