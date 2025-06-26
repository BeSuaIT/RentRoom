package com.example.timphongtro.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Adapters.MeetingAdapter;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.Models.Meeting;
import com.example.timphongtro.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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

public class MeetingManagementActivity extends AppCompatActivity {

    private ImageView backButton, filterIcon;
    private HorizontalScrollView chipScrollView;
    private ChipGroup chipGroup;
    private RecyclerView scheduleVisitRecyclerView;
    private LinearLayout noDataLayout;
    private TextView noDataTitle, noDataMessage;
    private Chip chipAll, chipMyBookings, chipPending, chipApproved, chipRejected;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference meetingSchedulesRef, postsRef;
    private ArrayList<Meeting> meetings;
    private ArrayList<Room> availableRooms;
    private MeetingAdapter meetingAdapter;
    private FirebaseUser currentUser;
    private boolean isFilterVisible = true;
    private Map<String, Integer> countMap = new HashMap<>();
    private String currentFilter = "all";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_management);

        initializeVariables();
        setupViews();
        setupChipListeners();
        loadRoomData();
        loadAllSchedulesAndCount();
    }

    private void initializeVariables() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        meetingSchedulesRef = firebaseDatabase.getReference("MeetingSchedules");
        postsRef = firebaseDatabase.getReference("Posts");
        meetings = new ArrayList<>();
        availableRooms = new ArrayList<>();

        countMap.put("all", 0);
        countMap.put("my_bookings", 0);
        countMap.put("pending", 0);
        countMap.put("approved", 0);
        countMap.put("rejected", 0);
    }

    private void setupViews() {
        backButton = findViewById(R.id.imageViewBack);
        filterIcon = findViewById(R.id.filterIcon);
        chipScrollView = findViewById(R.id.chipScrollView);
        chipGroup = findViewById(R.id.chipGroup);
        chipAll = findViewById(R.id.chipAll);
        chipMyBookings = findViewById(R.id.chipMyBookings);
        chipPending = findViewById(R.id.chipPending);
        chipApproved = findViewById(R.id.chipApproved);
        chipRejected = findViewById(R.id.chipRejected);
        scheduleVisitRecyclerView = findViewById(R.id.rcvScheduleVisit);
        noDataLayout = findViewById(R.id.noHasLovePost);
        noDataTitle = findViewById(R.id.noDataTitle);
        noDataMessage = findViewById(R.id.noDataMessage);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        scheduleVisitRecyclerView.setLayoutManager(linearLayoutManager);

        meetingAdapter = new MeetingAdapter(this, meetings);
        scheduleVisitRecyclerView.setAdapter(meetingAdapter);

        backButton.setOnClickListener(v -> finish());
        filterIcon.setOnClickListener(v -> toggleFilterVisibility());

        chipAll.setChecked(true);
    }

    private void setupChipListeners() {
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            int checkedId = checkedIds.get(0);
            handleChipSelection(checkedId);
        });
    }

    private void handleChipSelection(int chipId) {
        meetings.clear();
        meetingAdapter.notifyDataSetChanged();
        
        if (chipId == R.id.chipAll) {
            currentFilter = "all";
            loadAllSchedules();
        } else if (chipId == R.id.chipMyBookings) {
            currentFilter = "my_bookings";
            loadMyBookings();
        } else if (chipId == R.id.chipPending) {
            currentFilter = "pending";
            loadPendingSchedules();
        } else if (chipId == R.id.chipApproved) {
            currentFilter = "approved";
            loadApprovedSchedules();
        } else if (chipId == R.id.chipRejected) {
            currentFilter = "rejected";
            loadRejectedSchedules();
        }
    }

    private void toggleFilterVisibility() {
        if (isFilterVisible) {
            chipScrollView.setVisibility(View.GONE);
            filterIcon.setRotation(180f);
        } else {
            chipScrollView.setVisibility(View.VISIBLE);
            filterIcon.setRotation(0f);
        }
        isFilterVisible = !isFilterVisible;
    }

    private void loadAllSchedulesAndCount() {
        meetingSchedulesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                calculateCounts(snapshot);
                updateChipCounts();

                switch (currentFilter) {
                    case "all":
                        loadAllSchedules();
                        break;
                    case "my_bookings":
                        loadMyBookings();
                        break;
                    case "pending":
                        loadPendingSchedules();
                        break;
                    case "approved":
                        loadApprovedSchedules();
                        break;
                    case "rejected":
                        loadRejectedSchedules();
                        break;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                updateRecyclerViewVisibility(false);
            }
        });
    }

    private void calculateCounts(DataSnapshot snapshot) {
        int allCount = 0;
        int myBookingsCount = 0;
        int pendingCount = 0;
        int approvedCount = 0;
        int rejectedCount = 0;
        
        if (snapshot.exists() && currentUser != null) {
            String currentUserId = currentUser.getUid();
            
            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                Meeting schedule = dataSnapshot.getValue(Meeting.class);
                if (schedule != null) {
                    // Kiểm tra meeting có liên quan đến user hiện tại không
                    boolean isFromUser = currentUserId.equals(schedule.getIdFrom());
                    boolean isToUser = currentUserId.equals(schedule.getIdTo());
                    boolean isRelated = isFromUser || isToUser;
                    
                    if (isRelated) {
                        allCount++;
                        if (isFromUser) {
                            myBookingsCount++;
                        }
                        if (isToUser && schedule.getStatus() == 0) {
                            pendingCount++;
                        }
                        if (schedule.getStatus() == 1) {
                            approvedCount++;
                        }
                        if (schedule.getStatus() == 2) {
                            rejectedCount++;
                        }
                    }
                }
            }
        }
        
        countMap.put("all", allCount);
        countMap.put("my_bookings", myBookingsCount);
        countMap.put("pending", pendingCount);
        countMap.put("approved", approvedCount);
        countMap.put("rejected", rejectedCount);
    }

    private void updateChipCounts() {
        chipAll.setText("Tất cả (" + countMap.get("all") + ")");
        chipMyBookings.setText("📅 Tôi đặt (" + countMap.get("my_bookings") + ")");
        chipPending.setText("⏳ Chờ duyệt (" + countMap.get("pending") + ")");
        chipApproved.setText("✅ Đã duyệt (" + countMap.get("approved") + ")");
        chipRejected.setText("❌ Từ chối (" + countMap.get("rejected") + ")");
    }

    private void loadAllSchedules() {
        meetingSchedulesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                meetings.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Meeting schedule = dataSnapshot.getValue(Meeting.class);
                        if (isValidSchedule(schedule) && isRelatedToCurrentUser(schedule)) {
                            meetings.add(schedule);
                        }
                    }
                }
                meetingAdapter.notifyDataSetChanged();
                updateRecyclerViewVisibility(!meetings.isEmpty());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                updateRecyclerViewVisibility(false);
            }
        });
    }

    private void loadMyBookings() {
        meetingSchedulesRef.orderByChild("idFrom")
                .equalTo(currentUser.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        updateScheduleList(snapshot);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        updateRecyclerViewVisibility(false);
                    }
                });
    }

    private void loadPendingSchedules() {
        meetingSchedulesRef.orderByChild("idTo")
                .equalTo(currentUser.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        updateScheduleListByStatus(snapshot, "0");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        updateRecyclerViewVisibility(false);
                    }
                });
    }

    private void loadApprovedSchedules() {
        meetingSchedulesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateScheduleListByStatusAndUser(snapshot, "1");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                updateRecyclerViewVisibility(false);
            }
        });
    }

    private void loadRejectedSchedules() {
        meetingSchedulesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateScheduleListByStatusAndUser(snapshot, "2");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                updateRecyclerViewVisibility(false);
            }
        });
    }

    private void loadRoomData() {
        postsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                availableRooms.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                        Room room = roomSnapshot.getValue(Room.class);
                        if (room != null) {
                            availableRooms.add(room);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void updateScheduleList(DataSnapshot snapshot) {
        meetings.clear();
        if (snapshot.exists()) {
            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                Meeting schedule = dataSnapshot.getValue(Meeting.class);
                if (isValidSchedule(schedule)) {
                    meetings.add(schedule);
                }
            }
        }
        meetingAdapter.notifyDataSetChanged();
        updateRecyclerViewVisibility(!meetings.isEmpty());
    }

    private void updateScheduleListByStatus(DataSnapshot snapshot, String targetStatus) {
        meetings.clear();
        if (snapshot.exists()) {
            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                Meeting schedule = dataSnapshot.getValue(Meeting.class);
                if (isValidSchedule(schedule) && targetStatus.equals(schedule.getStatus())) {
                    meetings.add(schedule);
                }
            }
        }
        meetingAdapter.notifyDataSetChanged();
        updateRecyclerViewVisibility(!meetings.isEmpty());
    }

    private void updateScheduleListByStatusAndUser(DataSnapshot snapshot, String targetStatus) {
        meetings.clear();
        if (snapshot.exists()) {
            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                Meeting schedule = dataSnapshot.getValue(Meeting.class);
                if (isValidSchedule(schedule) && 
                    targetStatus.equals(schedule.getStatus()) && 
                    isRelatedToCurrentUser(schedule)) {
                    meetings.add(schedule);
                }
            }
        }
        meetingAdapter.notifyDataSetChanged();
        updateRecyclerViewVisibility(!meetings.isEmpty());
    }

    private boolean isValidSchedule(Meeting schedule) {
        if (schedule == null) return false;
        
        return availableRooms.stream()
                .anyMatch(room -> room.getId_room().equals(schedule.getIdRoom()));
    }

    private boolean isRelatedToCurrentUser(Meeting schedule) {
        return schedule.getIdFrom().equals(currentUser.getUid()) || 
               schedule.getIdTo().equals(currentUser.getUid());
    }

    private void updateRecyclerViewVisibility(boolean hasData) {
        scheduleVisitRecyclerView.setVisibility(hasData ? View.VISIBLE : View.GONE);
        noDataLayout.setVisibility(hasData ? View.GONE : View.VISIBLE);
        
        if (!hasData) {
            updateNoDataText();
        }
    }

    private void updateNoDataText() {
        String title, message;
        
        switch (currentFilter) {
            case "all":
                title = "Chưa có lịch hẹn";
                message = "Bạn chưa có lịch hẹn xem phòng nào";
                break;
            case "my_bookings":
                title = "Chưa đặt lịch hẹn";
                message = "Bạn chưa đặt lịch hẹn xem phòng nào";
                break;
            case "pending":
                title = "Không có lịch chờ duyệt";
                message = "Hiện tại không có lịch hẹn nào cần bạn xác nhận";
                break;
            case "approved":
                title = "Chưa có lịch đã duyệt";
                message = "Chưa có lịch hẹn nào được xác nhận";
                break;
            case "rejected":
                title = "Chưa có lịch bị từ chối";
                message = "Chưa có lịch hẹn nào bị từ chối";
                break;
            default:
                title = "Không có dữ liệu";
                message = "Không tìm thấy lịch hẹn nào";
                break;
        }
        
        noDataTitle.setText(title);
        noDataMessage.setText(message);
    }
}