package com.example.timphongtro.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Models.Room;
import com.example.timphongtro.Adapters.ShowmoreAdapter;
import com.example.timphongtro.R;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ShowMoreActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ShowmoreAdapter adapter;
    private ShimmerFrameLayout shimmerLayout;
    private final ArrayList<Room> roomList = new ArrayList<>();
    private DatabaseReference roomsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_more);

        initializeViews();
        setupRecyclerView();
        loadRooms();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.rcv_showmore);
        shimmerLayout = findViewById(R.id.room_shimmer);
        
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        roomsRef = FirebaseDatabase.getInstance().getReference("Rooms");
    }

    private void setupRecyclerView() {
        adapter = new ShowmoreAdapter(this, roomList);
        recyclerView.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
    }

    private void loadRooms() {
        shimmerLayout.startShimmer();
        
        roomsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                roomList.clear();
                
                if (snapshot.exists()) {
                    processRoomData(snapshot);
                }
                
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ShowMoreActivity.this, 
                    "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                updateUI();
            }
        });
    }

    private void processRoomData(DataSnapshot snapshot) {
        for (DataSnapshot typeSnapshot : snapshot.getChildren()) {
            for (DataSnapshot roomSnapshot : typeSnapshot.getChildren()) {
                Room room = roomSnapshot.getValue(Room.class);
                if (room != null && room.getStatus_room() == 0) { // Only show available rooms
                    roomList.add(room);
                }
            }
        }
    }

    private void updateUI() {
        shimmerLayout.stopShimmer();
        shimmerLayout.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        shimmerLayout.startShimmer();
    }

    @Override
    protected void onPause() {
        shimmerLayout.stopShimmer();
        super.onPause();
    }
}