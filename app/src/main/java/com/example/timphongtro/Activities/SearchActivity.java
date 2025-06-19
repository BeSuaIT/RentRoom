package com.example.timphongtro.Activities;

import android.database.DataSetObserver;
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
    private String selectedRoomType = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initializeViews();
        setupSpinners();
        setupFilters();
        setupRecyclerView();

        roomRef = FirebaseDatabase.getInstance().getReference("Posts");
        citiesRef = FirebaseDatabase.getInstance().getReference("Cities");

        fetchCitiesData(); // ✅ Sẽ call handleIncomingSelection sau khi load xong
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

        // ✅ Thêm default options
        cityAdapter.add("Tất cả Thành phố");
        districtAdapter.add("Tất cả Quận/Huyện");

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
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDistrict = position > 0 ? parent.getItemAtPosition(position).toString() : "";
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupFilters() {
        radioGroupRoomType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioBtnMini) {
                selectedRoomType = "Chung cư Mini";
            } else if (checkedId == R.id.radioBtnRoom) {
                selectedRoomType = "Trọ";
            } else if (checkedId == R.id.radioBtnAll) {
                selectedRoomType = "";
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

                for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                    Room room = roomSnapshot.getValue(Room.class);
                    if (room != null) {
                        roomArrayList.add(room);
                    }
                }

                searchAdapter.searchDataList(roomArrayList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
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
                        if (district != null) {
                            districts.add(district);
                        }
                    }
                    city.setDistricts(districts);
                    cityList.add(city);
                    cityAdapter.add(city.getName());
                }

                // ✅ Sau khi load xong cities, handle incoming selection
                handleIncomingSelection();
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
            boolean matchesRoomType = selectedRoomType.isEmpty() || room.getType_room().equals(selectedRoomType);
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

    // ✅ Cập nhật method handleIncomingSelection
    private void handleIncomingSelection() {
        String incomingCity = getIntent().getStringExtra("selectedCity");
        String incomingDistrict = getIntent().getStringExtra("selectedDistrict");

        if (incomingCity != null || incomingDistrict != null) {
            // ✅ Đợi cities data load xong rồi mới set selection
            citiesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // ✅ Đợi UI setup xong
                    spinnerCity.post(() -> {
                        setCitySelection(incomingCity, incomingDistrict);
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
    }

    // ✅ Thêm method set city selection
    private void setCitySelection(String incomingCity, String incomingDistrict) {
        // ✅ Tìm và set city trước
        if (incomingCity != null) {
            for (int i = 0; i < cityAdapter.getCount(); i++) {
                if (cityAdapter.getItem(i).equals(incomingCity)) {
                    spinnerCity.setSelection(i);

                    // ✅ Đợi district adapter update xong rồi set district
                    if (incomingDistrict != null) {
                        setDistrictSelectionAfterCityChange(incomingDistrict);
                    }
                    return;
                }
            }
        }

        // ✅ Nếu không có city hoặc không tìm thấy city, chỉ set district
        if (incomingDistrict != null) {
            setDistrictSelectionDirectly(incomingDistrict);
        }
    }

    // ✅ Set district sau khi city đã thay đổi
    private void setDistrictSelectionAfterCityChange(String targetDistrict) {
        // ✅ Sử dụng DataSetObserver để đợi district adapter update
        districtAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                districtAdapter.unregisterDataSetObserver(this);

                // ✅ Tìm và set district selection
                spinnerDistrict.post(() -> {
                    for (int j = 0; j < districtAdapter.getCount(); j++) {
                        if (districtAdapter.getItem(j).equals(targetDistrict)) {
                            spinnerDistrict.setSelection(j);
                            break;
                        }
                    }
                });
            }
        });
    }

    // ✅ Set district trực tiếp (khi không có city)
    private void setDistrictSelectionDirectly(String targetDistrict) {
        // ✅ Tìm city chứa district này và set cả hai
        for (int cityIndex = 0; cityIndex < cityList.size(); cityIndex++) {
            City city = cityList.get(cityIndex);
            if (city.getDistricts() != null) {
                for (District district : city.getDistricts()) {
                    if (district.getName().equals(targetDistrict)) {
                        // ✅ Tìm thấy → set city trước
                        spinnerCity.setSelection(cityIndex + 1); // +1 vì có "Tất cả Thành phố"

                        // ✅ Rồi set district
                        setDistrictSelectionAfterCityChange(targetDistrict);
                        return;
                    }
                }
            }
        }
    }
}