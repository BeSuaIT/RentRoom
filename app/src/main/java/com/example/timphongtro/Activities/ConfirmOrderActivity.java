package com.example.timphongtro.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Adapters.OrderItemAdapter;
import com.example.timphongtro.Api.CreateOrder;
import com.example.timphongtro.Models.Cart;
import com.example.timphongtro.Models.City;
import com.example.timphongtro.Models.District;
import com.example.timphongtro.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.zalopay.sdk.Environment;
import vn.zalopay.sdk.ZaloPayError;
import vn.zalopay.sdk.ZaloPaySDK;
import vn.zalopay.sdk.listeners.PayOrderListener;

public class ConfirmOrderActivity extends AppCompatActivity {
    private Spinner spinnerCity, spinnerDistrict, spinnerPayment;
    private ImageView imageView_back;
    private ArrayList<City> cityArrayList;
    private ArrayList<String> spinnerArrayList;
    private ArrayAdapter<String> spinnerAdapter, districtAdapter, paymentAdapter;
    private RecyclerView rcvOrderItems;
    private ArrayList<Cart> cartList;
    private TextView totalAmount;
    private ArrayList<String> districtNames;
    private long totalPriceValue;
    private DecimalFormat decimalFormat;
    private String selectedPaymentMethod = "cod";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_order);

        StrictMode.ThreadPolicy policy = new
                StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // ZaloPay SDK Init
        ZaloPaySDK.init(2553, Environment.SANDBOX);

        decimalFormat = new DecimalFormat("#,###.###");
        decimalFormat.setDecimalSeparatorAlwaysShown(false);
        totalPriceValue = getIntent().getLongExtra("totalPrice", 0);

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
        setupPaymentSpinner();
    }

    private void initViews() {
        spinnerCity = findViewById(R.id.spinner_city);
        spinnerDistrict = findViewById(R.id.spinner_district);
        spinnerPayment = findViewById(R.id.spinner_payment);
        rcvOrderItems = findViewById(R.id.rcv_orderItems);
        totalAmount = findViewById(R.id.textView_totalAmount);
        imageView_back = findViewById(R.id.imageView_back);

        // Set total amount once with proper formatting
        totalAmount.setText(decimalFormat.format(totalPriceValue) + " VNĐ");

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
                if (selectedPaymentMethod.equals("zalopay")) {
                    executeZaloPayPayment();
                } else {
                    createOrder(0);
                }
            }
        });
    }

    private boolean validateOrder() {
        String selectedCity = spinnerCity.getSelectedItem().toString();
        String selectedDistrict = spinnerDistrict.getSelectedItem().toString();
        String detailAddress = ((EditText) findViewById(R.id.editText_address)).getText().toString().trim();

        if (selectedCity.isEmpty() || selectedDistrict.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (detailAddress.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ chi tiết", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (cartList.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void executeZaloPayPayment() {
        CreateOrder orderApi = new CreateOrder();

        try {
            JSONObject data = orderApi.createOrder(String.valueOf(totalPriceValue));
            String code = data.getString("return_code");

            if (code.equals("1")) {
                String token = data.getString("zp_trans_token");
                ZaloPaySDK.getInstance().payOrder(ConfirmOrderActivity.this, token, "demozpdk://app", new PayOrderListener() {
                    @Override
                    public void onPaymentSucceeded(String s, String s1, String s2) {
                        createOrder(1);
                        Intent intent1 = new Intent(ConfirmOrderActivity.this, PaymentNotificationActivity.class);
                        intent1.putExtra("result", "Thanh toán thành công");
                        startActivity(intent1);
                        finish();
                    }

                    @Override
                    public void onPaymentCanceled(String s, String s1) {
                        Intent intent1 = new Intent(ConfirmOrderActivity.this, PaymentNotificationActivity.class);
                        intent1.putExtra("result", "Thanh toán bị hủy");
                        startActivity(intent1);
                        finish();
                    }

                    @Override
                    public void onPaymentError(ZaloPayError zaloPayError, String s, String s1) {
                        Intent intent1 = new Intent(ConfirmOrderActivity.this, PaymentNotificationActivity.class);
                        intent1.putExtra("result", "Lỗi thanh toán: " + zaloPayError.toString());
                        startActivity(intent1);
                        finish();
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createOrder(int status) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        EditText editTextAddress = findViewById(R.id.editText_address);
        String detailAddress = editTextAddress.getText().toString().trim();

        DatabaseReference cartRef = FirebaseDatabase.getInstance()
                .getReference("Carts")
                .child(user.getUid());

        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot cartSnapshot) {
                DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("Bills");

                for (DataSnapshot sellerSnapshot : cartSnapshot.getChildren()) {
                    String sellerId = sellerSnapshot.getKey();
                    if (sellerId == null) continue;

                    String orderId = generateOrderId();
                    if (orderId == null) continue;

                    // Tạo order data tối giản
                    Map<String, Object> orderData = new HashMap<>();
                    orderData.put("userId", user.getUid());
                    orderData.put("sellerId", sellerId);
                    orderData.put("orderDate", ServerValue.TIMESTAMP);
                    orderData.put("status", status);
                    orderData.put("city", spinnerCity.getSelectedItem().toString());
                    orderData.put("district", spinnerDistrict.getSelectedItem().toString());
                    orderData.put("detailAddress", detailAddress);
                    orderData.put("paymentMethod", selectedPaymentMethod);

                    // Lưu items với cấu trúc tối giản
                    Map<String, Integer> items = new HashMap<>();
                    long totalAmount = 0;

                    for (DataSnapshot itemSnapshot : sellerSnapshot.getChildren()) {
                        String serviceId = itemSnapshot.getKey();
                        Integer amount = itemSnapshot.getValue(Integer.class);
                        if (serviceId == null || amount == null) continue;

                        // Chỉ lưu serviceId và amount
                        items.put(serviceId, amount);

                        // Tính tổng tiền
                        for (Cart cartItem : cartList) {
                            if (cartItem.getServiceId().equals(serviceId)) {
                                totalAmount += (long) cartItem.getPrice() * amount;
                                break;
                            }
                        }
                    }

                    orderData.put("items", items);
                    orderData.put("totalAmount", totalAmount);

                    // Lưu order vào database
                    ordersRef.child(orderId).setValue(orderData);
                }

                // Xóa cart sau khi tạo orders
                cartRef.removeValue().addOnSuccessListener(aVoid -> {
                    if (selectedPaymentMethod.equals("cod")) {
                        Toast.makeText(ConfirmOrderActivity.this,
                                "Đặt hàng thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ConfirmOrderActivity.this,
                        "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String generateOrderId() {
        // Get current date in format YYYYMMDD
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyyMMdd");
        String currentDate = dateFormat.format(new java.util.Date());

        // Generate random 5-digit number
        int randomNum = (int) ((Math.random() * 90000) + 10000);

        // Combine to create order ID: ORD-YYYYMMDD-XXXXX
        return String.format("ORD-%s-%05d", currentDate, randomNum);
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

    private void setupPaymentSpinner() {
        ArrayList<String> paymentMethods = new ArrayList<>();
        paymentMethods.add("Thanh toán khi nhận hàng");
        paymentMethods.add("Thanh toán ZaloPay");

        paymentAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                paymentMethods);
        paymentAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinnerPayment.setAdapter(paymentAdapter);

        spinnerPayment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPaymentMethod = position == 0 ? "cod" : "zalopay";
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedPaymentMethod = "cod";
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        ZaloPaySDK.getInstance().onResult(intent);
    }
}