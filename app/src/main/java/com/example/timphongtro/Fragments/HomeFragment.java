package com.example.timphongtro.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.example.timphongtro.Activities.PostRoomActivity;
import com.example.timphongtro.Activities.SearchActivity;
import com.example.timphongtro.Activities.ServiceActivity;
import com.example.timphongtro.Adapters.DistrictAdapter;
import com.example.timphongtro.Models.City;
import com.example.timphongtro.Models.District;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.Adapters.RoomAdapter;
import com.example.timphongtro.Activities.LoginActivity;
import com.example.timphongtro.R;
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

public class HomeFragment extends Fragment {
    private ShimmerFrameLayout districtShimmer, roomShimmer;
    private DatabaseReference spinnerRef;
    private DistrictAdapter districtAdapter;
    private Spinner spinner;
    private String selectedSpinner;
    private RoomAdapter roomAdapter;
    private ArrayList<District> districtArrayList;
    private ArrayList<Room> roomArrayList;
    private ArrayList<String> spinnerArrayList;
    private ArrayAdapter<String> spinnerAdapter;
    private ArrayList<City> cityArrayList;
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private final FirebaseUser user = firebaseAuth.getCurrentUser();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeUI(view);
        setupImageSlider(view);
        setupCitySpinner();
        setupDistrictRecyclerView(view);
        setupRoomRecyclerView(view);
        fetchCityData();
        fetchRoomDatabase();
    }

    // Xử lý UI
    private void initializeUI(View view) {
        districtShimmer = view.findViewById(R.id.district_shimmer);
        roomShimmer = view.findViewById(R.id.room_shimmer);

        view.findViewById(R.id.find_room).setOnClickListener(v -> startActivity(new Intent(getContext(), SearchActivity.class)));
        view.findViewById(R.id.tin_dang_cho_thue).setOnClickListener(v -> {
            Intent intent = (user != null) ? new Intent(getContext(), PostRoomActivity.class) : new Intent(getContext(), LoginActivity.class);
            startActivity(intent);
        });
        setupServiceClickListeners(view);
    }

    // Xử lý các sự kiện click cho các dịch vụ
    private void setupServiceClickListeners(View view) {
        view.findViewById(R.id.doi_binh_ga).setOnClickListener(v -> openServiceActivity("doibinhga"));
        view.findViewById(R.id.doi_binh_nuoc).setOnClickListener(v -> openServiceActivity("doibinhnuoc"));
        view.findViewById(R.id.giat_la).setOnClickListener(v -> openServiceActivity("giatla"));
        view.findViewById(R.id.sua_chua_dien_nuoc).setOnClickListener(v -> openServiceActivity("suachuadiennuoc"));
        view.findViewById(R.id.tu_van_thiet_ke_phong).setOnClickListener(v -> openServiceActivity("tuvanthietkephong"));
        view.findViewById(R.id.cho_thue_noi_that).setOnClickListener(v -> openServiceActivity("chothuenoithat"));
    }

    // Xử lý slider ảnh
    private void setupImageSlider(View view) {
        StorageReference storageReference = storage.getReference().child("HomeImageSlider");
        ImageSlider imageSlider = view.findViewById(R.id.imageSlider);

        storageReference.listAll().addOnSuccessListener(listResult -> {
            ArrayList<SlideModel> slideModels = new ArrayList<>();
            for (StorageReference item : listResult.getItems()) {
                item.getDownloadUrl().addOnSuccessListener(uri -> {
                    slideModels.add(new SlideModel(uri.toString(), ScaleTypes.FIT));
                    imageSlider.setImageList(slideModels, ScaleTypes.FIT);
                });
            }
        }).addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi không load được ảnh", Toast.LENGTH_SHORT).show());
    }

    // Xử lý spinner thành phố
    private void setupCitySpinner() {
        spinner = requireView().findViewById(R.id.city_spinner);

        spinnerArrayList = new ArrayList<>();
        cityArrayList = new ArrayList<>();
        spinnerRef = FirebaseDatabase.getInstance().getReference("Cities");

        spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_style,
                spinnerArrayList
        );
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        spinner.setAdapter(spinnerAdapter);
    }

    // Xử lý RecyclerView quận
    private void setupDistrictRecyclerView(View view) {
        RecyclerView rcv_district = view.findViewById(R.id.rcv_district);
        rcv_district.setHasFixedSize(true);
        rcv_district.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        districtArrayList = new ArrayList<>();
        districtAdapter = new DistrictAdapter(getContext(), districtArrayList);
        rcv_district.setAdapter(districtAdapter);
    }

    // Xử lý RecyclerView phòng
    private void setupRoomRecyclerView(View view) {
        RecyclerView rcv_room = view.findViewById(R.id.rcv_room);
        rcv_room.setHasFixedSize(true);
        rcv_room.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        roomArrayList = new ArrayList<>();
        roomAdapter = new RoomAdapter(getContext(), roomArrayList);
        rcv_room.setAdapter(roomAdapter);
    }

    // Lấy dữ liệu thành phố từ Firebase
    private void fetchCityData() {
        spinnerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    spinnerArrayList.clear();
                    cityArrayList.clear();

                    for (DataSnapshot citySnapshot : snapshot.getChildren()) {
                        String cityName = citySnapshot.child("name").getValue(String.class);
                        String cityId = citySnapshot.child("id_city").getValue(String.class);

                        // Kiểm tra xem thành phố có node Districts và có quận hay không
                        DataSnapshot districtsSnapshot = citySnapshot.child("Districts");
                        if (cityName != null && cityId != null &&
                                districtsSnapshot.exists() &&
                                districtsSnapshot.getChildrenCount() > 0) {

                            ArrayList<District> districts = new ArrayList<>();
                            for (DataSnapshot ds : districtsSnapshot.getChildren()) {
                                District district = ds.getValue(District.class);
                                if (district != null) districts.add(district);
                            }

                            // Chỉ thêm thành phố nếu có quận
                            if (!districts.isEmpty()) {
                                cityArrayList.add(new City(cityId, cityName, districts));
                                spinnerArrayList.add(cityName);
                            }
                        }
                    }

                    updateSpinnerUI();
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Lỗi không thể load thành phố: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Lỗi không xác định", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Cập nhật UI cho Spinner
    private void updateSpinnerUI() {
        if (getContext() == null) return;

        spinnerAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_style, spinnerArrayList);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        spinner.setAdapter(spinnerAdapter);

        int defaultPosition = spinnerArrayList.indexOf("Hà Nội");
        if (defaultPosition != -1) {
            spinner.setSelection(defaultPosition);
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < cityArrayList.size()) {
                    City selectedCity = cityArrayList.get(position);
                    selectedSpinner = selectedCity.getName();
                    updateDistrictList(selectedCity.getDistricts());
                    fetchRoomDatabase();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // Cập nhật danh sách quận
    private void updateDistrictList(ArrayList<District> districts) {
        districtArrayList.clear();
        districtArrayList.addAll(districts);
        districtShimmer.stopShimmer();
        districtShimmer.setVisibility(View.GONE);
        districtAdapter.notifyDataSetChanged();
    }

    // Lấy dữ liệu phòng từ Firebase
    private void fetchRoomDatabase() {
        DatabaseReference roomRef = FirebaseDatabase.getInstance().getReference("Rooms");
        roomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                roomArrayList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot roomType : snapshot.getChildren()) {
                        for (DataSnapshot roomSnapshot : roomType.getChildren()) {
                            Room room = roomSnapshot.getValue(Room.class);
                            if (room != null && room.getStatus_room() != 1 && room.getAddress().getCity().equals(selectedSpinner)) {
                                roomArrayList.add(room);
                            }
                        }
                    }
                }
                roomShimmer.stopShimmer();
                roomShimmer.setVisibility(View.GONE);
                roomAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Lỗi không thể load bài đăng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Mở activity dịch vụ
    private void openServiceActivity(String item) {
        Intent intent = new Intent(getContext(), ServiceActivity.class);
        intent.putExtra("item", item);
        startActivity(intent);
    }
}
