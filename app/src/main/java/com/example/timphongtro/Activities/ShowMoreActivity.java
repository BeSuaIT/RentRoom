package com.example.timphongtro.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Models.Room;
import com.example.timphongtro.Adapters.ShowmoreAdapter;
import com.example.timphongtro.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ShowMoreActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ShowmoreAdapter adapter;
    private ProgressBar progressBar;
    
    private final ArrayList<Room> roomList = new ArrayList<>();
    private DatabaseReference roomsRef;
    private ValueEventListener roomListener;

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
        progressBar = findViewById(R.id.progress_bar);
        
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        roomsRef = FirebaseDatabase.getInstance().getReference("Posts");
    }

    private void setupRecyclerView() {
        adapter = new ShowmoreAdapter(this, roomList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
    }

    private void loadRooms() {
        showLoading();
        if (roomListener != null) {
            roomsRef.removeEventListener(roomListener);
        }
        
        roomListener = new ValueEventListener() {
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
        };
        
        roomsRef.addValueEventListener(roomListener);
    }

    private void processRoomData(DataSnapshot snapshot) {
        for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
            Room room = roomSnapshot.getValue(Room.class);
            if (room != null) {
                roomList.add(room);
            }
        }
    }

    private void updateUI() {
        hideLoading();
        recyclerView.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    private void showLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void hideLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roomsRef != null && roomListener != null) {
            roomsRef.removeEventListener(roomListener);
        }
    }
}