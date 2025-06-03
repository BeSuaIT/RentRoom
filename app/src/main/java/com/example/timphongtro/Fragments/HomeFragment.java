package com.example.timphongtro.Fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.content.Intent;
import android.widget.Toast;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.timphongtro.Activities.CartManagementActivity;
import com.example.timphongtro.Activities.ContractManagementActivity;
import com.example.timphongtro.Activities.AddPostActivity;
import com.example.timphongtro.Activities.SearchActivity;
import com.example.timphongtro.Activities.ServiceActivity;
import com.example.timphongtro.Activities.ShowMoreActivity;
import com.example.timphongtro.Adapters.DistrictAdapter;
import com.example.timphongtro.Models.City;
import com.example.timphongtro.Models.District;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.Adapters.RoomAdapter;
import com.example.timphongtro.R;
import com.example.timphongtro.Utils.AuthUtils;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeFragment extends Fragment {
    private ShimmerFrameLayout districtShimmer, roomShimmer;
    private DatabaseReference spinnerRef;
    private DistrictAdapter districtAdapter;
    private Spinner spinner;
    private String selectedSpinner = "Hà Nội";
    private RoomAdapter roomAdapter;
    private ArrayList<District> districtArrayList;
    private ArrayList<Room> roomArrayList;
    private ArrayList<String> spinnerArrayList;
    private ArrayAdapter<String> spinnerAdapter;
    private ArrayList<City> cityArrayList;
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable cityTimeout, roomTimeout, imageTimeout;
    private boolean isInitialLoad = true;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupListeners(view);
        checkUserRoleAndUpdateUI(view);
        loadData();
    }

    private void initializeViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        districtShimmer = view.findViewById(R.id.district_shimmer);
        roomShimmer = view.findViewById(R.id.room_shimmer);
        spinner = view.findViewById(R.id.city_spinner);
        spinnerArrayList = new ArrayList<>();
        cityArrayList = new ArrayList<>();
        spinnerRef = FirebaseDatabase.getInstance().getReference("Cities");

        setupRecyclerViews(view);
        setupCitySpinner();
    }

    private void checkUserRoleAndUpdateUI(View view) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        View postRoomLayout = view.findViewById(R.id.tin_dang_cho_thue);
        
        if (currentUser == null) {
            postRoomLayout.setVisibility(View.GONE);
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUser.getUid());
                
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String userRole = snapshot.child("role").getValue(String.class);
                    
                    if ("Chủ trọ".equals(userRole)) {
                        postRoomLayout.setVisibility(View.VISIBLE);
                    } else {
                        postRoomLayout.setVisibility(View.GONE);
                    }
                } else {
                    postRoomLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                postRoomLayout.setVisibility(View.GONE);
                showToast("Lỗi kiểm tra quyền người dùng");
            }
        });
    }

    private void setupListeners(View view) {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            try {
                isInitialLoad = true;
                loadData();
            } catch (Exception e) {
                showToast("Lỗi refresh: " + e.getMessage());
                forceStopRefresh();
            }
        });

        view.findViewById(R.id.searchEditText).setOnClickListener(v -> 
            startActivity(new Intent(getContext(), SearchActivity.class)));
        view.findViewById(R.id.showMore).setOnClickListener(v -> 
            startActivity(new Intent(getContext(), ShowMoreActivity.class)));
        view.findViewById(R.id.cart).setOnClickListener(v -> {
            if (checkLoginRequired("Xem giỏ hàng")) {
                startActivity(new Intent(getContext(), CartManagementActivity.class));
            }
        });
        view.findViewById(R.id.contract).setOnClickListener(v -> {
            if (checkLoginRequired("Xem hợp đồng")) {
                startActivity(new Intent(getContext(), ContractManagementActivity.class));
            }
        });
        view.findViewById(R.id.tin_dang_cho_thue).setOnClickListener(v -> {
            if (checkLoginRequired("Đăng tin phòng trọ")) {
                checkUserRoleForPosting();
            }
        });
        
        setupServiceClickListeners(view);
    }

    private void checkUserRoleForPosting() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            AuthUtils.showLoginRequiredDialog(this, "Đăng tin phòng trọ");
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUser.getUid());
                
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String userRole = snapshot.child("role").getValue(String.class);
                    
                    if ("Chủ trọ".equals(userRole)) {
                        startActivity(new Intent(getContext(), AddPostActivity.class));
                    } else {
                        showToast("Chỉ có Chủ trọ mới được phép đăng tin cho thuê");
                    }
                } else {
                    showToast("Không thể xác thực thông tin người dùng");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Lỗi kiểm tra quyền người dùng");
            }
        });
    }

    private void loadData() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        
        setupImageSlider(getView());
        fetchCityData();
    }

    private boolean checkLoginRequired(String feature) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            AuthUtils.showLoginRequiredDialog(this, feature);
            return false;
        }
        return true;
    }

    private void setupServiceClickListeners(View view) {
        view.findViewById(R.id.doi_binh_ga).setOnClickListener(v -> openServiceActivity("doibinhga"));
        view.findViewById(R.id.doi_binh_nuoc).setOnClickListener(v -> openServiceActivity("doibinhnuoc"));
        view.findViewById(R.id.giat_la).setOnClickListener(v -> openServiceActivity("giatla"));
        view.findViewById(R.id.sua_chua_dien_nuoc).setOnClickListener(v -> openServiceActivity("suachuadiennuoc"));
        view.findViewById(R.id.tu_van_thiet_ke_phong).setOnClickListener(v -> openServiceActivity("tuvanthietkephong"));
        view.findViewById(R.id.cho_thue_noi_that).setOnClickListener(v -> openServiceActivity("chothuenoithat"));
    }

    private void setupImageSlider(View view) {
        if (getContext() == null || view == null) return;
        
        ImageSlider imageSlider = view.findViewById(R.id.imageSlider);
        StorageReference storageReference = storage.getReference().child("HomeImageSlider");

        imageTimeout = () -> {
            if (getContext() != null) {
                showToast("Không thể tải ảnh slider");
            }
        };
        timeoutHandler.postDelayed(imageTimeout, 8000);

        storageReference.listAll().addOnSuccessListener(listResult -> {
            timeoutHandler.removeCallbacks(imageTimeout);
            
            if (listResult.getItems().isEmpty()) {
                return;
            }
            
            ArrayList<SlideModel> slideModels = new ArrayList<>();
            AtomicInteger loadedCount = new AtomicInteger(0);
            int totalItems = listResult.getItems().size();

            for (StorageReference item : listResult.getItems()) {
                item.getDownloadUrl().addOnSuccessListener(uri -> {
                    slideModels.add(new SlideModel(uri.toString(), ScaleTypes.FIT));

                    if (loadedCount.incrementAndGet() == totalItems) {
                        if (getContext() != null) {
                            imageSlider.setImageList(slideModels, ScaleTypes.FIT);
                        }
                    }
                }).addOnFailureListener(e -> {
                    loadedCount.incrementAndGet();
                });
            }
        }).addOnFailureListener(e -> {
            timeoutHandler.removeCallbacks(imageTimeout);
        });
    }

    private void setupCitySpinner() {
        spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_style,
                spinnerArrayList
        );
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        spinner.setAdapter(spinnerAdapter);
    }

    private void setupRecyclerViews(View view) {
        // District RecyclerView
        RecyclerView rcv_district = view.findViewById(R.id.rcv_district);
        rcv_district.setHasFixedSize(true);
        rcv_district.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        districtArrayList = new ArrayList<>();
        districtAdapter = new DistrictAdapter(getContext(), districtArrayList);
        rcv_district.setAdapter(districtAdapter);

        // Room RecyclerView
        RecyclerView rcv_room = view.findViewById(R.id.rcv_room);
        rcv_room.setHasFixedSize(true);
        rcv_room.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        roomArrayList = new ArrayList<>();
        roomAdapter = new RoomAdapter(getContext(), roomArrayList);
        rcv_room.setAdapter(roomAdapter);
    }

    private void fetchCityData() {
        startDistrictShimmer();

        cityTimeout = () -> {
            showToast("Timeout loading cities");
            stopDistrictShimmer();
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
        };
        timeoutHandler.postDelayed(cityTimeout, 10000);
        
        spinnerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                timeoutHandler.removeCallbacks(cityTimeout);
                
                try {
                    spinnerArrayList.clear();
                    cityArrayList.clear();

                    for (DataSnapshot citySnapshot : snapshot.getChildren()) {
                        String cityName = citySnapshot.child("name").getValue(String.class);
                        String cityId = citySnapshot.child("id_city").getValue(String.class);

                        DataSnapshot districtsSnapshot = citySnapshot.child("Districts");
                        if (cityName != null && cityId != null &&
                                districtsSnapshot.exists() &&
                                districtsSnapshot.getChildrenCount() > 0) {

                            ArrayList<District> districts = new ArrayList<>();
                            for (DataSnapshot ds : districtsSnapshot.getChildren()) {
                                District district = ds.getValue(District.class);
                                if (district != null) districts.add(district);
                            }

                            if (!districts.isEmpty()) {
                                cityArrayList.add(new City(cityId, cityName, districts));
                                spinnerArrayList.add(cityName);
                            }
                        }
                    }
                    updateSpinnerUI();
                    
                } catch (Exception e) {
                    showToast("Lỗi không thể load thành phố: " + e.getMessage());
                    stopDistrictShimmer();
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                timeoutHandler.removeCallbacks(cityTimeout);
                showToast("Lỗi không xác định");
                stopDistrictShimmer();
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    private void fetchRoomDatabase() {
        if (selectedSpinner == null) {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }
        
        startRoomShimmer();

        roomTimeout = () -> {
            stopRoomShimmer();
            showToast("Timeout loading rooms");
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
        };
        timeoutHandler.postDelayed(roomTimeout, 15000);

        DatabaseReference roomRef = FirebaseDatabase.getInstance().getReference("Posts");

        roomRef.orderByChild("address/city")
               .equalTo(selectedSpinner)
               .limitToFirst(20)
               .addListenerForSingleValueEvent(new ValueEventListener() {
                   @Override
                   public void onDataChange(@NonNull DataSnapshot snapshot) {
                       timeoutHandler.removeCallbacks(roomTimeout);
                       
                       roomArrayList.clear();
                       if (snapshot.exists()) {
                           for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                               Room room = roomSnapshot.getValue(Room.class);
                               if (room != null && room.getAddress() != null) {
                                   roomArrayList.add(room);
                               }
                           }
                       }
                       
                       stopRoomShimmer();
                       roomAdapter.notifyDataSetChanged();

                       if (swipeRefreshLayout != null) {
                           swipeRefreshLayout.setRefreshing(false);
                       }
                   }

                   @Override
                   public void onCancelled(@NonNull DatabaseError error) {
                       timeoutHandler.removeCallbacks(roomTimeout);
                       stopRoomShimmer();
                       showToast("Lỗi không thể load bài đăng");
                       if (swipeRefreshLayout != null) {
                           swipeRefreshLayout.setRefreshing(false);
                       }
                   }
               });
    }

    private void updateSpinnerUI() {
        if (getContext() == null) return;

        spinnerAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_style, spinnerArrayList);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        spinner.setAdapter(spinnerAdapter);

        int defaultPosition = spinnerArrayList.indexOf("Hà Nội");
        if (defaultPosition != -1) {
            spinner.setSelection(defaultPosition);
            if (defaultPosition < cityArrayList.size()) {
                City defaultCity = cityArrayList.get(defaultPosition);
                updateDistrictList(defaultCity.getDistricts());
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < cityArrayList.size()) {
                    City selectedCity = cityArrayList.get(position);
                    String newSelectedSpinner = selectedCity.getName();

                    if (!newSelectedSpinner.equals(selectedSpinner) || isInitialLoad) {
                        selectedSpinner = newSelectedSpinner;
                        updateDistrictList(selectedCity.getDistricts());
                        fetchRoomDatabase();
                        isInitialLoad = false;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    private void updateDistrictList(ArrayList<District> districts) {
        districtArrayList.clear();
        districtArrayList.addAll(districts);
        
        stopDistrictShimmer();
        districtAdapter.notifyDataSetChanged();
    }

    private void startDistrictShimmer() {
        if (districtShimmer != null) {
            districtShimmer.setVisibility(View.VISIBLE);
            districtShimmer.startShimmer();
        }
    }

    private void stopDistrictShimmer() {
        if (districtShimmer != null) {
            districtShimmer.stopShimmer();
            districtShimmer.setVisibility(View.GONE);
        }
    }

    private void startRoomShimmer() {
        if (roomShimmer != null) {
            roomShimmer.setVisibility(View.VISIBLE);
            roomShimmer.startShimmer();
        }
    }

    private void stopRoomShimmer() {
        if (roomShimmer != null) {
            roomShimmer.stopShimmer();
            roomShimmer.setVisibility(View.GONE);
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void openServiceActivity(String item) {
        Intent intent = new Intent(getContext(), ServiceActivity.class);
        intent.putExtra("item", item);
        startActivity(intent);
    }

    private void forceStopRefresh() {
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        forceStopRefresh();
        
        if (timeoutHandler != null) {
            if (cityTimeout != null) {
                timeoutHandler.removeCallbacks(cityTimeout);
            }
            if (roomTimeout != null) {
                timeoutHandler.removeCallbacks(roomTimeout);
            }
            if (imageTimeout != null) {
                timeoutHandler.removeCallbacks(imageTimeout);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        forceStopRefresh();
    }
}
