package com.example.timphongtro.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Adapters.ScheduleVisitRoomSendAdapter;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.Models.ScheduleVisitRoomClass;
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
    private ImageView backButton;
    private TabLayout scheduleTabLayout;
    private RecyclerView scheduleVisitRecyclerView;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference meetingSchedulesRef, availableRoomsRef;
    private ArrayList<ScheduleVisitRoomClass> scheduleVisitRoomClasses;
    private ArrayList<Room> availableRooms;
    private ScheduleVisitRoomSendAdapter scheduleVisitRoomSendAdapter;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_visit_room);

        initializeVariables();
        setupViews();
        loadRoomData();
        loadScheduleData();
        setupTabLayout();
    }

    private void initializeVariables() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        meetingSchedulesRef = firebaseDatabase.getReference("MeetingSchedules");
        availableRoomsRef = firebaseDatabase.getReference("Rooms");
        scheduleVisitRoomClasses = new ArrayList<>();
        availableRooms = new ArrayList<>();
    }

    private void setupViews() {
        backButton = findViewById(R.id.imageViewBack);
        scheduleVisitRecyclerView = findViewById(R.id.rcvScheduleVisit);
        scheduleTabLayout = findViewById(R.id.tablayout);
        
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        scheduleVisitRecyclerView.setLayoutManager(linearLayoutManager);
        
        backButton.setOnClickListener(v -> finish());
        
        scheduleVisitRoomSendAdapter = new ScheduleVisitRoomSendAdapter(this, scheduleVisitRoomClasses);
        scheduleVisitRecyclerView.setAdapter(scheduleVisitRoomSendAdapter);
    }

    private void loadRoomData() {
        availableRoomsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                availableRooms.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        processRoomCategory(dataSnapshot);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void processRoomCategory(DataSnapshot dataSnapshot) {
        String key = dataSnapshot.getKey();
        if ("Tro".equals(key) || "ChungCuMini".equals(key)) {
            for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                Room room = childSnapshot.getValue(Room.class);
                if (room != null && room.getStatus_room() != 1) {
                    availableRooms.add(room);
                }
            }
        }
    }

    private void loadScheduleData() {
        meetingSchedulesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateScheduleList(snapshot, currentUser.getUid(), "");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void setupTabLayout() {
        if (currentUser != null) {
            scheduleTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    handleTabSelection(tab.getPosition());
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        }
    }

    private void handleTabSelection(int position) {
        scheduleVisitRoomClasses.clear();
        switch (position) {
            case 0:
                loadSentSchedules();
                break;
            case 1:
                loadPendingSchedules();
                break;
            case 2:
                loadAcceptedSchedules();
                break;
            case 3:
                loadRejectedSchedules();
                break;
        }
    }

    private void loadSentSchedules() {
        meetingSchedulesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateScheduleList(snapshot, currentUser.getUid(), "");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadPendingSchedules() {
        meetingSchedulesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateScheduleList(snapshot, "", "0");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadAcceptedSchedules() {
        meetingSchedulesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateScheduleList(snapshot, "", "1");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadRejectedSchedules() {
        meetingSchedulesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateScheduleList(snapshot, "", "2");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateScheduleList(DataSnapshot snapshot, String fromId, String status) {
        scheduleVisitRoomClasses.clear();
        if (snapshot.exists()) {
            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                ScheduleVisitRoomClass schedule = dataSnapshot.getValue(ScheduleVisitRoomClass.class);
                if (isValidSchedule(schedule, fromId, status)) {
                    scheduleVisitRoomClasses.add(schedule);
                }
            }
        }
        scheduleVisitRoomSendAdapter.notifyDataSetChanged();
        updateRecyclerViewVisibility(scheduleVisitRoomClasses, scheduleVisitRecyclerView, findViewById(R.id.noHasLovePost));
    }

    private boolean isValidSchedule(ScheduleVisitRoomClass schedule, String fromId, String status) {
        if (schedule == null) return false;
        
        boolean roomExists = availableRooms.stream()
            .anyMatch(room -> room.getId_room().equals(schedule.getIdRoom()));
            
        if (!roomExists) return false;

        if (!fromId.isEmpty()) {
            return schedule.getIdFrom().equals(fromId);
        }

        if (!status.isEmpty()) {
            return schedule.getIdTo().equals(currentUser.getUid()) &&
                   status.equals(schedule.getStatus());
        }

        return true;
    }

    private void updateRecyclerViewVisibility(ArrayList<ScheduleVisitRoomClass> rooms, RecyclerView rcvLovePost, View noHistoryView) {
        rcvLovePost.setVisibility(rooms.isEmpty() ? View.GONE : View.VISIBLE);
        noHistoryView.setVisibility(rooms.isEmpty() ? View.VISIBLE : View.GONE);
    }
}