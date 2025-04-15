package com.example.timphongtro.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.timphongtro.Adapters.FollowRoomAdapter;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class FollowFragment extends Fragment {
    private RecyclerView rcvFollowPost;
    private ArrayList<Room> followedRooms;
    private FollowRoomAdapter roomAdapter;
    private FirebaseUser currentUser;
    private DatabaseReference followPostsRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_follow, container, false);
        initViews(view);
        loadFollowedRooms();
        return view;
    }

    private void initViews(View view) {
        rcvFollowPost = view.findViewById(R.id.rcvFollowPost);
        followedRooms = new ArrayList<>();
        roomAdapter = new FollowRoomAdapter(requireContext(), followedRooms); // Thay đổi adapter
        rcvFollowPost.setLayoutManager(new LinearLayoutManager(getContext()));
        rcvFollowPost.setAdapter(roomAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        followPostsRef = FirebaseDatabase.getInstance().getReference("FollowPosts");
    }

    private void loadFollowedRooms() {
        if (currentUser == null) return;

        followPostsRef.child(currentUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                followedRooms.clear();
                for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                    String roomId = roomSnapshot.getKey();
                    // Kiểm tra trong cả 2 loại phòng
                    checkRoomInType(roomId, "Tro");
                    checkRoomInType(roomId, "ChungCuMini");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Lỗi tải danh sách theo dõi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkRoomInType(String roomId, String type) {
        DatabaseReference roomsRef = FirebaseDatabase.getInstance()
                .getReference("Rooms")
                .child(type)
                .child(roomId);

        roomsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Room room = snapshot.getValue(Room.class);
                    if (room != null) {
                        // Set the type_room based on the node
                        room.setType_room(type.equals("ChungCuMini") ? 1 : 0);
                        followedRooms.add(room);
                        roomAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), 
                    "Lỗi tải thông tin phòng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}