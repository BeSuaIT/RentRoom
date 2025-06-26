package com.example.timphongtro.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.timphongtro.Adapters.FollowRoomAdapter;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.R;
import com.example.timphongtro.Activities.LoginActivity;
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

public class FollowFragment extends Fragment {
    private RecyclerView rcvFollowPost;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyView;
    private TextView emptyTitle, emptyMessage;
    private ArrayList<Room> followedRooms;
    private FollowRoomAdapter roomAdapter;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_follow, container, false);
        
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return view;
        }
        
        initViews(view);
        setupListeners();
        loadFollowedRooms();
        return view;
    }

    private void navigateToLogin() {
        Intent loginIntent = new Intent(requireActivity(), LoginActivity.class);
        loginIntent.putExtra("previous_activity", requireActivity().getClass().getName());
        startActivity(loginIntent);
        requireActivity().getSupportFragmentManager().beginTransaction()
            .replace(R.id.frame_layout, new HomeFragment())
            .commit();
    }

    private void initViews(View view) {
        rcvFollowPost = view.findViewById(R.id.rcvFollowPost);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        emptyView = view.findViewById(R.id.emptyView);
        emptyTitle = view.findViewById(R.id.emptyTitle);
        emptyMessage = view.findViewById(R.id.emptyMessage);
        
        followedRooms = new ArrayList<>();
        roomAdapter = new FollowRoomAdapter(requireContext(), followedRooms);
        rcvFollowPost.setLayoutManager(new LinearLayoutManager(getContext()));
        rcvFollowPost.setAdapter(roomAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUser.getUid());
        }
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadFollowedRooms);
    }

    private void loadFollowedRooms() {
        if (currentUser == null || userRef == null) {
            showEmptyState("Chưa đăng nhập", "Vui lòng đăng nhập để xem danh sách theo dõi");
            return;
        }
        
        if (!swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(true);
        }

        cleanupInvalidFollowPosts(() -> {
            // Load data sau khi cleanup xong
            loadValidFollowedRooms();
        });
    }

    private void cleanupInvalidFollowPosts(Runnable onComplete) {
        userRef.child("followPosts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    onComplete.run();
                    return;
                }

                Map<String, Boolean> followedRoomIds = new HashMap<>();
                for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                    String roomId = roomSnapshot.getKey();
                    Boolean isFollowed = roomSnapshot.getValue(Boolean.class);
                    if (roomId != null && Boolean.TRUE.equals(isFollowed)) {
                        followedRoomIds.put(roomId, true);
                    }
                }

                if (followedRoomIds.isEmpty()) {
                    onComplete.run();
                    return;
                }
                
                DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("Posts");
                postsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot postsSnapshot) {
                        Map<String, Object> updates = new HashMap<>();

                        for (String roomId : followedRoomIds.keySet()) {
                            if (!postsSnapshot.hasChild(roomId)) {
                                // Room không tồn tại - xóa khỏi follow list
                                updates.put("followPosts/" + roomId, null);
                            }
                        }

                        if (!updates.isEmpty()) {
                            userRef.updateChildren(updates)
                                    .addOnSuccessListener(aVoid -> onComplete.run())
                                    .addOnFailureListener(e -> onComplete.run());
                        } else {
                            onComplete.run();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        onComplete.run();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                onComplete.run();
            }
        });
    }

    private void loadValidFollowedRooms() {
        userRef.child("followPosts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                followedRooms.clear();
                
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    showEmptyState("Chưa có phòng theo dõi", "Bạn chưa theo dõi phòng nào.\nHãy tìm và theo dõi những phòng yêu thích!");
                    swipeRefresh.setRefreshing(false);
                    return;
                }

                Map<String, Boolean> followedRoomIds = new HashMap<>();
                for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                    String roomId = roomSnapshot.getKey();
                    Boolean isFollowed = roomSnapshot.getValue(Boolean.class);
                    if (roomId != null && Boolean.TRUE.equals(isFollowed)) {
                        followedRoomIds.put(roomId, true);
                    }
                }
                
                loadRoomDetails(followedRoomIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                swipeRefresh.setRefreshing(false);
                showEmptyState("Lỗi tải dữ liệu", "Không thể tải danh sách theo dõi: " + error.getMessage());
                Toast.makeText(getContext(), "Lỗi tải danh sách theo dõi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRoomDetails(Map<String, Boolean> followedRoomIds) {
        if (followedRoomIds.isEmpty()) {
            showEmptyState("Chưa có phòng theo dõi", "Bạn chưa theo dõi phòng nào.\nHãy tìm và theo dõi những phòng yêu thích!");
            swipeRefresh.setRefreshing(false);
            return;
        }

        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("Posts");
        
        postsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                followedRooms.clear();

                for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                    String roomId = roomSnapshot.getKey();
                    if (followedRoomIds.containsKey(roomId)) {
                        Room room = roomSnapshot.getValue(Room.class);
                        if (room != null) {
                            followedRooms.add(room);
                        }
                    }
                }

                roomAdapter.notifyDataSetChanged();
                updateUIState();
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                swipeRefresh.setRefreshing(false);
                showEmptyState("Lỗi tải dữ liệu", "Không thể tải thông tin phòng: " + error.getMessage());
                Toast.makeText(getContext(), "Lỗi tải thông tin phòng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmptyState(String title, String message) {
        rcvFollowPost.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        
        if (emptyTitle != null) emptyTitle.setText(title);
        if (emptyMessage != null) emptyMessage.setText(message);
    }

    private void updateUIState() {
        if (followedRooms.isEmpty()) {
            showEmptyState("Chưa có phòng theo dõi", "Bạn chưa theo dõi phòng nào.\nHãy tìm và theo dõi những phòng yêu thích!");
        } else {
            emptyView.setVisibility(View.GONE);
            rcvFollowPost.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUser.getUid());
            loadFollowedRooms();
        } else {
            navigateToLogin();
        }
    }
}