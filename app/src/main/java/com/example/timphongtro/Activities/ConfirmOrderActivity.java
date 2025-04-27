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

        totalAmount.setText(decimalFormat.format(totalPriceValue) + " VNĐ");

        imageView_back.setOnClickListener(v -> finish());

        cityArrayList = new ArrayList<>();
        spinnerArrayList = new ArrayList<>();
        districtNames = new ArrayList<>();

        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerArrayList);
        districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, districtNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(spinnerAdapter);
        spinnerDistrict.setAdapter(districtAdapter);

        rcvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        OrderItemAdapter orderItemAdapter = new OrderItemAdapter(this, cartList);
        rcvOrderItems.setAdapter(orderItemAdapter);

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
                    }

                    @Override
                    public void onPaymentCanceled(String s, String s1) {
                    }

                    @Override
                    public void onPaymentError(ZaloPayError zaloPayError, String s, String s1) {
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
                final int[] successCount = {0};
                final int totalOrders = (int) cartSnapshot.getChildrenCount();

                for (DataSnapshot sellerSnapshot : cartSnapshot.getChildren()) {
                    String sellerId = sellerSnapshot.getKey();
                    if (sellerId == null) continue;

                    String orderId = generateOrderId();
                    Map<String, Object> orderData = createOrderData(user.getUid(), sellerId, status, 
                        detailAddress, sellerSnapshot);

                    ordersRef.child(orderId).setValue(orderData)
                        .addOnSuccessListener(aVoid -> {
                            successCount[0]++;
                            // Chỉ show notification và finish khi tất cả đơn hàng đã được tạo
                            if (successCount[0] == totalOrders) {
                                showPaymentNotification(selectedPaymentMethod, true);
                                cartRef.removeValue();
                                finish();
                            }
                        })
                        .addOnFailureListener(e -> {
                            showPaymentNotification(selectedPaymentMethod, false);
                            Toast.makeText(ConfirmOrderActivity.this, 
                                "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showPaymentNotification(selectedPaymentMethod, false);
                Toast.makeText(ConfirmOrderActivity.this, 
                    "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Map<String, Object> createOrderData(String userId, String sellerId, 
        int status, String detailAddress, DataSnapshot sellerSnapshot) {
        
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", userId);
        orderData.put("sellerId", sellerId);
        orderData.put("orderDate", ServerValue.TIMESTAMP);
        orderData.put("status", status);
        orderData.put("city", spinnerCity.getSelectedItem().toString());
        orderData.put("district", spinnerDistrict.getSelectedItem().toString());
        orderData.put("detailAddress", detailAddress);
        orderData.put("paymentMethod", selectedPaymentMethod);

        Map<String, Integer> items = new HashMap<>();
        long totalAmount = calculateTotalAmount(sellerSnapshot, items);

        orderData.put("items", items);
        orderData.put("totalAmount", totalAmount);

        return orderData;
    }

    private long calculateTotalAmount(DataSnapshot sellerSnapshot, Map<String, Integer> items) {
        long totalAmount = 0;
        for (DataSnapshot itemSnapshot : sellerSnapshot.getChildren()) {
            String serviceId = itemSnapshot.getKey();
            Integer amount = itemSnapshot.getValue(Integer.class);
            if (serviceId == null || amount == null) continue;

            items.put(serviceId, amount);

            for (Cart cartItem : cartList) {
                if (cartItem.getServiceId().equals(serviceId)) {
                    totalAmount += (long) cartItem.getPrice() * amount;
                    break;
                }
            }
        }
        return totalAmount;
    }

    private void showPaymentNotification(String paymentMethod, boolean isSuccess) {
        Intent intent = new Intent(this, PaymentNotificationActivity.class);
        intent.putExtra("payment_method", paymentMethod);
        intent.putExtra("is_success", isSuccess);
        startActivity(intent);
    }

    private String generateOrderId() {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyyMMdd");
        String currentDate = dateFormat.format(new java.util.Date());
        int randomNum = (int) ((Math.random() * 90000) + 10000);

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

        paymentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, paymentMethods);
        paymentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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