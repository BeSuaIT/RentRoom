package com.example.timphongtro.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Models.City;
import com.example.timphongtro.Models.District;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.Adapters.SearchAdapter;
import com.example.timphongtro.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class SearchActivity extends AppCompatActivity {
    private RecyclerView roomrecyclerView;
    private DatabaseReference roomRef, citiesRef;
    private SearchAdapter searchAdapter;
    private ArrayList<Room> roomArrayList, filteredList;
    private SearchView searchView;
    private Spinner spinnerCity, spinnerDistrict;
    private RadioGroup radioGroupRoomType;
    private CheckBox checkboxMale, checkboxFemale;
    private ArrayList<City> cityList;
    private ArrayAdapter<String> cityAdapter, districtAdapter;
    private String selectedCity = "", selectedDistrict = "";
    private int selectedRoomType = -1; // -1: all, 0: Phòng trọ, 1: Chung cư mini

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initializeViews();
        setupSpinners();
        setupFilters();
        setupRecyclerView();

        roomRef = FirebaseDatabase.getInstance().getReference("Rooms");
        citiesRef = FirebaseDatabase.getInstance().getReference("Cities");

        fetchCitiesData();
        fetchRoomDatabase();
    }

    private void initializeViews() {
        spinnerCity = findViewById(R.id.spinnerCity);
        spinnerDistrict = findViewById(R.id.spinnerDistrict);
        radioGroupRoomType = findViewById(R.id.radioGroupRoomType);
        checkboxMale = findViewById(R.id.checkboxMale);
        checkboxFemale = findViewById(R.id.checkboxFemale);
        searchView = findViewById(R.id.search_room);
        roomrecyclerView = findViewById(R.id.rcv_search);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        cityList = new ArrayList<>();
        roomArrayList = new ArrayList<>();
        filteredList = new ArrayList<>();
    }

    private void setupSpinners() {
        cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());

        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerCity.setAdapter(cityAdapter);
        spinnerDistrict.setAdapter(districtAdapter);

        spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedCity = parent.getItemAtPosition(position).toString();
                    updateDistrictSpinner(position - 1);
                } else {
                    selectedCity = "";
                    districtAdapter.clear();
                    districtAdapter.add("Tất cả Quận/Huyện");
                    selectedDistrict = "";
                }
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDistrict = position > 0 ? parent.getItemAtPosition(position).toString() : "";
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupFilters() {
        radioGroupRoomType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioBtnMini) {
                selectedRoomType = 1;
            } else if (checkedId == R.id.radioBtnRoom) {
                selectedRoomType = 0;
            } else if (checkedId == R.id.radioBtnAll) {
                selectedRoomType = -1;
            }
            applyFilters();
        });
        radioGroupRoomType.check(R.id.radioBtnAll);

        checkboxMale.setOnCheckedChangeListener((buttonView, isChecked) -> applyFilters());
        checkboxFemale.setOnCheckedChangeListener((buttonView, isChecked) -> applyFilters());

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                applyFilters();
                return true;
            }
        });
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        roomrecyclerView.setLayoutManager(layoutManager);
        searchAdapter = new SearchAdapter(this, roomArrayList);
        roomrecyclerView.setAdapter(searchAdapter);
    }

    private void fetchRoomDatabase() {
        roomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                roomArrayList.clear();

                DataSnapshot chungCuMiniSnapshot = snapshot.child("ChungCuMini");
                for (DataSnapshot roomSnapshot : chungCuMiniSnapshot.getChildren()) {
                    Room room = roomSnapshot.getValue(Room.class);
                    if (room != null && room.getStatus_room() == 0) {
                        roomArrayList.add(room);
                    }
                }

                DataSnapshot troSnapshot = snapshot.child("Tro");
                for (DataSnapshot roomSnapshot : troSnapshot.getChildren()) {
                    Room room = roomSnapshot.getValue(Room.class);
                    if (room != null && room.getStatus_room() == 0) {
                        roomArrayList.add(room);
                    }
                }

                searchAdapter.searchDataList(roomArrayList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchCitiesData() {
        citiesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cityList.clear();
                cityAdapter.clear();
                cityAdapter.add("Tất cả Thành phố");

                for (DataSnapshot citySnapshot : snapshot.getChildren()) {
                    City city = new City();
                    city.setName(citySnapshot.child("name").getValue(String.class));
                    city.setId_city(citySnapshot.child("id_city").getValue(String.class));

                    ArrayList<District> districts = new ArrayList<>();
                    DataSnapshot districtsSnapshot = citySnapshot.child("Districts");
                    for (DataSnapshot districtSnapshot : districtsSnapshot.getChildren()) {
                        District district = districtSnapshot.getValue(District.class);
                        districts.add(district);
                    }
                    city.setDistricts(districts);
                    cityList.add(city);
                    cityAdapter.add(city.getName());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateDistrictSpinner(int cityPosition) {
        districtAdapter.clear();
        districtAdapter.add("Tất cả Quận/Huyện");

        if (cityPosition >= 0 && cityPosition < cityList.size()) {
            City selectedCity = cityList.get(cityPosition);
            if (selectedCity.getDistricts() != null) {
                for (District district : selectedCity.getDistricts()) {
                    districtAdapter.add(district.getName());
                }
            }
        }
    }

    private void applyFilters() {
        filteredList.clear();
        String searchText = searchView.getQuery().toString().toLowerCase();
        boolean isMaleChecked = checkboxMale.isChecked();
        boolean isFemaleChecked = checkboxFemale.isChecked();

        for (Room room : roomArrayList) {
            boolean matchesSearch = room.getTitle_room().toLowerCase().contains(searchText) ||
                    room.getAddress().getDistrict().toLowerCase().contains(searchText);
            boolean matchesCity = selectedCity.isEmpty() || room.getAddress().getCity().equals(selectedCity);
            boolean matchesDistrict = selectedDistrict.isEmpty() || room.getAddress().getDistrict().equals(selectedDistrict);
            boolean matchesRoomType = selectedRoomType == -1 || room.getType_room() == selectedRoomType;
            boolean matchesGender;
            if (!isMaleChecked && !isFemaleChecked) {
                matchesGender = true;
            } else if (isMaleChecked && !isFemaleChecked) {
                matchesGender = room.getGender_room().equals("Nam");
            } else if (!isMaleChecked && isFemaleChecked) {
                matchesGender = room.getGender_room().equals("Nữ");
            } else {
                matchesGender = room.getGender_room().equals("Nam/Nữ");
            }

            if (matchesSearch && matchesCity && matchesDistrict && matchesRoomType && matchesGender) {
                filteredList.add(room);
            }
        }

        searchAdapter.searchDataList(filteredList);
    }
}