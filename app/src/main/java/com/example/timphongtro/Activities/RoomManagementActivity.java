package com.example.timphongtro.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Adapters.ManageRoomAdapter;
import com.example.timphongtro.Models.Room;
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

public class RoomManagementActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private View emptyView;
    private TabLayout tabLayout;
    private ManageRoomAdapter adapter;
    private ArrayList<Room> roomList;
    private DatabaseReference roomsRef;
    private FirebaseUser currentUser;
    private int currentStatus = 0; // 0: phòng trống, 1: đã cho thuê

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_management);

        initializeViews();
        setupListeners();
        setupRecyclerView();
        loadRooms();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.rcvMyPost);
        emptyView = findViewById(R.id.noHasLovePost);
        tabLayout = findViewById(R.id.tabLayout);
        
        findViewById(R.id.imageViewBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAddPost).setOnClickListener(v -> startActivity(new Intent(this, AddRoomActivity.class)));
    }

    private void setupRecyclerView() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        roomList = new ArrayList<>();
        roomsRef = FirebaseDatabase.getInstance().getReference("Rooms");
        adapter = new ManageRoomAdapter(roomList, this);
        
        recyclerView.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentStatus = tab.getPosition();
                loadRooms();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadRooms() {
        roomsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                roomList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                        Room room = roomSnapshot.getValue(Room.class);
                        if (isValidRoom(room)) {
                            roomList.add(room);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                updateViewVisibility();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RoomManagementActivity.this,
                    "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isValidRoom(Room room) {
        return room != null 
            && currentUser.getUid().equals(room.getOwnerID())
            && room.getRoomStatus() == currentStatus;
    }

    private void updateViewVisibility() {
        recyclerView.setVisibility(roomList.isEmpty() ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(roomList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}