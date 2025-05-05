package com.example.timphongtro.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.timphongtro.Adapters.RoomAdapter;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private View emptyView;
    private RoomAdapter adapter;
    private ArrayList<Room> rooms;
    private DatabaseReference historyRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        
        initializeViews();
        setupListeners();
        loadHistory();
    }

    private void initializeViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh);
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);

        rooms = new ArrayList<>();
        adapter = new RoomAdapter(this, rooms);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            historyRef = FirebaseDatabase.getInstance()
                .getReference("Histories")
                .child(user.getUid());
        }
    }

    private void setupListeners() {
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.clearButton).setOnClickListener(v -> showClearDialog());
        swipeRefresh.setOnRefreshListener(this::loadHistory);
    }

    private void showClearDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Xóa lịch sử")
            .setMessage("Bạn có chắc muốn xóa tất cả lịch sử xem?")
            .setPositiveButton("Xóa", (dialog, which) -> clearHistory())
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void clearHistory() {
        if (historyRef == null) return;

        historyRef.removeValue()
            .addOnSuccessListener(unused -> {
                rooms.clear();
                adapter.notifyDataSetChanged();
                updateEmptyState();
                Toast.makeText(this, "Đã xóa lịch sử", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }

    private void loadHistory() {
        if (historyRef == null) return;
        
        swipeRefresh.setRefreshing(true);

        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Long> timestamps = new HashMap<>();
                
                for (DataSnapshot child : snapshot.getChildren()) {
                    String roomId = child.getKey();
                    Long timestamp = child.getValue(Long.class);
                    if (roomId != null && timestamp != null) {
                        timestamps.put(roomId, timestamp);
                    }
                }
                
                loadRoomDetails(timestamps);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(HistoryActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRoomDetails(Map<String, Long> timestamps) {
        if (timestamps.isEmpty()) {
            rooms.clear();
            adapter.notifyDataSetChanged();
            updateEmptyState();
            swipeRefresh.setRefreshing(false);
            return;
        }

        FirebaseDatabase.getInstance().getReference("Rooms")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    rooms.clear();
                    
                    for (DataSnapshot typeSnapshot : snapshot.getChildren()) {
                        for (DataSnapshot roomSnapshot : typeSnapshot.getChildren()) {
                            String roomId = roomSnapshot.getKey();
                            if (timestamps.containsKey(roomId)) {
                                Room room = roomSnapshot.getValue(Room.class);
                                if (room != null) {
                                    rooms.add(room);
                                }
                            }
                        }
                    }

                    // Sort by timestamp
                    rooms.sort((r1, r2) -> {
                        Long t1 = timestamps.get(r1.getId_room());
                        Long t2 = timestamps.get(r2.getId_room());
                        return t2.compareTo(t1);
                    });

                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    swipeRefresh.setRefreshing(false);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(HistoryActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void updateEmptyState() {
        recyclerView.setVisibility(rooms.isEmpty() ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(rooms.isEmpty() ? View.VISIBLE : View.GONE);
    }
}