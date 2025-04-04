package com.example.timphongtro.Activities;

import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Adapters.OrderItemAdapter;
import com.example.timphongtro.Models.Cart;
import com.example.timphongtro.Models.City;
import com.example.timphongtro.Models.District;
import com.example.timphongtro.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConfirmOrderActivity extends AppCompatActivity {
    private Spinner spinnerCity, spinnerDistrict;
    private ImageView imageView_back;
    private ArrayList<City> cityArrayList;
    private ArrayList<String> spinnerArrayList;
    private ArrayAdapter<String> spinnerAdapter, districtAdapter;
    private RecyclerView rcvOrderItems;
    private ArrayList<Cart> cartList;
    private TextView totalAmount;
    private ArrayList<String> districtNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_order);

        String cartJson = getIntent().getStringExtra("cartList");
        if (cartJson != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Cart>>(){}.getType();
            cartList = gson.fromJson(cartJson, type);
        } else {
            cartList = new ArrayList<>();
        }

        initViews();
        loadUserInfo();
        fetchCityData();
        setupConfirmButton();
    }

    private void initViews() {
        spinnerCity = findViewById(R.id.spinner_city);
        spinnerDistrict = findViewById(R.id.spinner_district);
        rcvOrderItems = findViewById(R.id.rcv_orderItems);
        totalAmount = findViewById(R.id.textView_totalAmount);
        imageView_back = findViewById(R.id.imageView_back);

        imageView_back.setOnClickListener(v -> finish());

        cityArrayList = new ArrayList<>();
        spinnerArrayList = new ArrayList<>();
        districtNames = new ArrayList<>();

        // Setup spinners
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerArrayList);
        districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, districtNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(spinnerAdapter);
        spinnerDistrict.setAdapter(districtAdapter);

        // Setup RecyclerView
        rcvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        OrderItemAdapter orderItemAdapter = new OrderItemAdapter(this, cartList);
        rcvOrderItems.setAdapter(orderItemAdapter);

        String totalPrice = getIntent().getStringExtra("totalPrice");
        totalAmount.setText(totalPrice);

        // Setup spinner listener
        spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < cityArrayList.size()) {
                    City selectedCity = cityArrayList.get(position);
                    updateDistrictList(selectedCity.getDistricts());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupConfirmButton() {
        Button btnConfirm = findViewById(R.id.button_confirmOrder);
        btnConfirm.setOnClickListener(v -> {
            if (validateOrder()) {
                createOrder();
            }
        });
    }

    private boolean validateOrder() {
        String selectedCity = spinnerCity.getSelectedItem().toString();
        String selectedDistrict = spinnerDistrict.getSelectedItem().toString();

        if (selectedCity.isEmpty() || selectedDistrict.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (cartList.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void createOrder() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String orderId = FirebaseDatabase.getInstance().getReference("Orders").push().getKey();
        if (orderId == null) return;

        DatabaseReference orderRef = FirebaseDatabase.getInstance()
                .getReference("Orders")
                .child(orderId);

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", user.getUid());
        orderData.put("orderDate", ServerValue.TIMESTAMP);
        orderData.put("status", "pending");
        orderData.put("city", spinnerCity.getSelectedItem().toString());
        orderData.put("district", spinnerDistrict.getSelectedItem().toString());
        orderData.put("totalAmount", totalAmount.getText().toString());
        orderData.put("items", cartList);

        orderRef.setValue(orderData)
                .addOnSuccessListener(aVoid -> {
                    FirebaseDatabase.getInstance()
                            .getReference("Carts")
                            .child(user.getUid())
                            .removeValue();

                    Toast.makeText(this, "Đặt hàng thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(user.getUid());

            userRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    String customerName = snapshot.child("name").getValue(String.class);
                    TextView textViewCustomerName = findViewById(R.id.textView_customerName);
                    textViewCustomerName.setText(customerName);
                }
            });
        }
    }

    private void fetchCityData() {
        DatabaseReference citiesRef = FirebaseDatabase.getInstance().getReference("Cities");
        citiesRef.get().addOnSuccessListener(snapshot -> {
            cityArrayList.clear();
            spinnerArrayList.clear();

            for (DataSnapshot citySnapshot : snapshot.getChildren()) {
                String cityName = citySnapshot.child("name").getValue(String.class);
                String cityId = citySnapshot.child("id_city").getValue(String.class);

                DataSnapshot districtsSnapshot = citySnapshot.child("Districts");
                if (cityName != null && cityId != null && districtsSnapshot.exists()) {
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
            spinnerAdapter.notifyDataSetChanged();
        });
    }

    private void updateDistrictList(ArrayList<District> districts) {
        districtNames.clear();
        for (District district : districts) {
            districtNames.add(district.getName());
        }
        districtAdapter.notifyDataSetChanged();
    }
}