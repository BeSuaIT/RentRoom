package com.example.timphongtro.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Adapter.ScheduleVisitRoomSendAdapter;
import com.example.timphongtro.Entity.Room;
import com.example.timphongtro.Entity.ScheduleVisitRoomClass;
import com.example.timphongtro.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class scheduleVisitRoomActivity extends AppCompatActivity {
    ImageView imageViewBack;
    TabLayout tablayout;
    RecyclerView rcvScheduleVisit;
    FirebaseDatabase database;
    DatabaseReference scheduleRef, roomRef;
    ArrayList<ScheduleVisitRoomClass> schedules;
    ArrayList<String> roomlist;
    ScheduleVisitRoomSendAdapter scheduleVisitRoomSendAdapter;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_visit_room);

        user = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        scheduleRef = database.getReference("scheduleVisitRoom");
        schedules = new ArrayList<>();
        imageViewBack = findViewById(R.id.imageViewBack);
        rcvScheduleVisit = findViewById(R.id.rcvScheduleVisit);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(scheduleVisitRoomActivity.this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rcvScheduleVisit.setLayoutManager(linearLayoutManager);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        roomlist = new ArrayList<>();
        roomRef = database.getReference("rooms");

        roomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                roomlist.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        String key = dataSnapshot.getKey();
                        if (key.equals("Tro") || key.equals("ChungCuMini")) {
                            for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                                Room room = childSnapshot.getValue(Room.class);
                                if (room != null && room.getStatus_room() != 1 && room.getStatus_room() != 1) {
                                    roomlist.add(room.getId_room());
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        scheduleRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                schedules.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        ScheduleVisitRoomClass scheduleVisitRoomClass = dataSnapshot.getValue(ScheduleVisitRoomClass.class);
                        if (scheduleVisitRoomClass != null) {
                            if (roomlist.contains(scheduleVisitRoomClass.getIdRoom()) && scheduleVisitRoomClass.getIdFrom().equals(user.getUid())) {
                                schedules.add(scheduleVisitRoomClass);
                            }
                        }
                    }
                }
                scheduleVisitRoomSendAdapter.notifyDataSetChanged();
                updateRecyclerViewVisibility(schedules,rcvScheduleVisit,findViewById(R.id.noHasLovePost));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        tablayout = findViewById(R.id.tablayout);
        if (user != null) {
            tablayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    int position = tab.getPosition();


                    switch (position) {
                        case 0:
                            schedules.clear();
                            scheduleRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    schedules.clear();
                                    if (snapshot.exists()) {
                                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                            ScheduleVisitRoomClass scheduleVisitRoomClass = dataSnapshot.getValue(ScheduleVisitRoomClass.class);
                                            if (scheduleVisitRoomClass != null) {
                                                if (roomlist.contains(scheduleVisitRoomClass.getIdRoom()) && scheduleVisitRoomClass.getIdFrom().equals(user.getUid())) {
                                                    schedules.add(scheduleVisitRoomClass);
                                                }
                                            }
                                        }
                                    }
                                    scheduleVisitRoomSendAdapter.notifyDataSetChanged();
                                    updateRecyclerViewVisibility(schedules,rcvScheduleVisit,findViewById(R.id.noHasLovePost));
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                            break;
                        case 1:
                            schedules.clear();
                            scheduleRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    schedules.clear();
                                    if (snapshot.exists()) {
                                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                            ScheduleVisitRoomClass scheduleVisitRoomClass = dataSnapshot.getValue(ScheduleVisitRoomClass.class);
                                            if (scheduleVisitRoomClass != null) {
                                                if (roomlist.contains(scheduleVisitRoomClass.getIdRoom()) && scheduleVisitRoomClass.getIdTo().equals(user.getUid()) && "0".equals(scheduleVisitRoomClass.getStatus())) {
                                                    schedules.add(scheduleVisitRoomClass);
                                                }
                                            }
                                        }
                                    }
                                    scheduleVisitRoomSendAdapter.notifyDataSetChanged();
                                    updateRecyclerViewVisibility(schedules,rcvScheduleVisit,findViewById(R.id.noHasLovePost));
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                            break;
                        case 2:
                            schedules.clear();
                            scheduleRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    schedules.clear();
                                    if (snapshot.exists()) {
                                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                            ScheduleVisitRoomClass scheduleVisitRoomClass = dataSnapshot.getValue(ScheduleVisitRoomClass.class);
                                            if (scheduleVisitRoomClass != null) {
                                                if (roomlist.contains(scheduleVisitRoomClass.getIdRoom()) && scheduleVisitRoomClass.getIdTo().equals(user.getUid()) && "1".equals(scheduleVisitRoomClass.getStatus())) {
                                                    schedules.add(scheduleVisitRoomClass);
                                                }
                                            }
                                        }
                                    }
                                    scheduleVisitRoomSendAdapter.notifyDataSetChanged();
                                    updateRecyclerViewVisibility(schedules,rcvScheduleVisit,findViewById(R.id.noHasLovePost));
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                            break;
                        case 3:
                            schedules.clear();
                            scheduleRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    schedules.clear();
                                    if (snapshot.exists()) {
                                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                            ScheduleVisitRoomClass scheduleVisitRoomClass = dataSnapshot.getValue(ScheduleVisitRoomClass.class);
                                            if (scheduleVisitRoomClass != null) {
                                                if (roomlist.contains(scheduleVisitRoomClass.getIdRoom()) && scheduleVisitRoomClass.getIdTo().equals(user.getUid()) && "2".equals(scheduleVisitRoomClass.getStatus())) {
                                                    schedules.add(scheduleVisitRoomClass);
                                                }
                                            }
                                        }
                                    }
                                    scheduleVisitRoomSendAdapter.notifyDataSetChanged();
                                    updateRecyclerViewVisibility(schedules,rcvScheduleVisit,findViewById(R.id.noHasLovePost));
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                            break;
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });
            scheduleVisitRoomSendAdapter = new ScheduleVisitRoomSendAdapter(scheduleVisitRoomActivity.this, schedules);
            rcvScheduleVisit.setAdapter(scheduleVisitRoomSendAdapter);
        }
    }
    private void updateRecyclerViewVisibility(ArrayList<ScheduleVisitRoomClass> rooms, RecyclerView rcvLovePost, View noHistoryView) {
        if (rooms.isEmpty()) {
            rcvLovePost.setVisibility(View.GONE);
            noHistoryView.setVisibility(View.VISIBLE);
        } else {
            rcvLovePost.setVisibility(View.VISIBLE);
            noHistoryView.setVisibility(View.GONE);
        }
    }
}