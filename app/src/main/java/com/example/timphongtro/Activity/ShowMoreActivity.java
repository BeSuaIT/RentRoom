package com.example.timphongtro.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Entity.Room;
import com.example.timphongtro.Adapter.ShowmoreAdapter;
import com.example.timphongtro.R;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ShowMoreActivity extends AppCompatActivity {
    RecyclerView roomrecyclerView;
    DatabaseReference roomRef;
    ShowmoreAdapter showmoreAdapter;
    ArrayList<Room> roomArrayList;
    ShimmerFrameLayout roomShimmer;
    ImageView btn_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_more);

        roomShimmer = findViewById(R.id.room_shimmer);
        btn_back = findViewById(R.id.btn_back);
        btn_back.setOnClickListener(v -> {
            finish();
        });

        roomShimmer.startShimmer();
        roomrecyclerView = findViewById(R.id.rcv_showmore);
        roomrecyclerView.setHasFixedSize(true);
        roomRef = FirebaseDatabase.getInstance().getReference("Rooms");
        roomrecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        roomArrayList = new ArrayList<>();
        showmoreAdapter = new ShowmoreAdapter(this, roomArrayList);
        roomrecyclerView.setAdapter(showmoreAdapter);
        fetchRoomDatabase();
    }

    private void fetchRoomDatabase() {
        roomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                roomArrayList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot roomType : snapshot.getChildren()) {
                        for (DataSnapshot roomSnapshot : roomType.getChildren()) {
                            Room room = roomSnapshot.getValue(Room.class);
                            if (room != null && room.getStatus_room() != 1) {
                                roomArrayList.add(room);
                            }
                        }
                    }
                }
                roomShimmer.stopShimmer();
                roomShimmer.setVisibility(View.GONE);
                showmoreAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}