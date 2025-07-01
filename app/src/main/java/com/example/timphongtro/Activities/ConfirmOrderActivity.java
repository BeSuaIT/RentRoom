package com.example.timphongtro.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;
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
import com.example.timphongtro.Models.CartItem;
import com.example.timphongtro.Models.City;
import com.example.timphongtro.Models.District;
import com.example.timphongtro.R;
import com.example.timphongtro.Utils.GsonUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
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
    private ArrayList<CartItem> cartItemList;
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

        loadCartDataFromIntent();
        initViews();
        loadUserInfo();
        fetchCityData();
        setupConfirmButton();
        setupPaymentSpinner();
    }

    private void loadCartDataFromIntent() {
        String cartJson = getIntent().getStringExtra("cartList");
        if (!TextUtils.isEmpty(cartJson)) {
            cartItemList = GsonUtils.fromJson(cartJson, GsonUtils.getListType(CartItem.class));
            
            if (cartItemList == null) {
                cartItemList = new ArrayList<>();
                Toast.makeText(this, "Lỗi: Dữ liệu giỏ hàng không hợp lệ", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            cartItemList = new ArrayList<>();
            Toast.makeText(this, "Lỗi: Không có dữ liệu giỏ hàng", Toast.LENGTH_SHORT).show();
            finish();
        }
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
        OrderItemAdapter orderItemAdapter = new OrderItemAdapter(this, cartItemList);
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
                btnConfirm.setEnabled(false);
                btnConfirm.setText("Đang xử lý...");
                
                if (selectedPaymentMethod.equals("zalopay")) {
                    executeZaloPayPayment(btnConfirm);
                } else {
                    createOrder(0, btnConfirm);
                }
            }
        });
    }

    private boolean validateOrder() {
        if (spinnerCity.getSelectedItem() == null || spinnerDistrict.getSelectedItem() == null) {
            Toast.makeText(this, "Vui lòng chọn địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
            return false;
        }

        String selectedCity = spinnerCity.getSelectedItem().toString();
        String selectedDistrict = spinnerDistrict.getSelectedItem().toString();
        String detailAddress = ((EditText) findViewById(R.id.editText_address)).getText().toString().trim();

        if (TextUtils.isEmpty(selectedCity) || TextUtils.isEmpty(selectedDistrict)) {
            Toast.makeText(this, "Vui lòng chọn địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(detailAddress)) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ chi tiết", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (cartItemList.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void executeZaloPayPayment(Button btnConfirm) {
        CreateOrder orderApi = new CreateOrder();

        try {
            JSONObject data = orderApi.createOrder(String.valueOf(totalPriceValue));
            String code = data.getString("return_code");

            if (code.equals("1")) {
                String token = data.getString("zp_trans_token");
                ZaloPaySDK.getInstance().payOrder(ConfirmOrderActivity.this, token, "demozpdk://app", new PayOrderListener() {
                    @Override
                    public void onPaymentSucceeded(String s, String s1, String s2) {
                        createOrder(1, btnConfirm);
                    }

                    @Override
                    public void onPaymentCanceled(String s, String s1) {
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText("Xác nhận đặt hàng");
                        Toast.makeText(ConfirmOrderActivity.this, "Thanh toán đã bị hủy", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPaymentError(ZaloPayError zaloPayError, String s, String s1) {
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText("Xác nhận đặt hàng");
                        Toast.makeText(ConfirmOrderActivity.this, "Lỗi thanh toán: " + zaloPayError.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                btnConfirm.setEnabled(true);
                btnConfirm.setText("Xác nhận đặt hàng");
                Toast.makeText(this, "Lỗi tạo đơn thanh toán", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            btnConfirm.setEnabled(true);
            btnConfirm.setText("Xác nhận đặt hàng");
            Toast.makeText(this, "Lỗi xử lý thanh toán: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void createOrder(int status, Button btnConfirm) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            btnConfirm.setEnabled(true);
            btnConfirm.setText("Xác nhận đặt hàng");
            Toast.makeText(this, "Lỗi: Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText editTextAddress = findViewById(R.id.editText_address);
        String detailAddress = editTextAddress.getText().toString().trim();
        String city = spinnerCity.getSelectedItem().toString();
        String district = spinnerDistrict.getSelectedItem().toString();

        String fullAddress;
        if (detailAddress.isEmpty()) {
            fullAddress = district + ", " + city;
        } else {
            fullAddress = detailAddress + ", " + district + ", " + city;
        }

        DatabaseReference cartsRef = FirebaseDatabase.getInstance().getReference("Carts");

        cartsRef.orderByChild("buyerId").equalTo(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot cartsSnapshot) {
                        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("Bills");
                        final int[] successCount = {0};
                        final int totalOrders = (int) cartsSnapshot.getChildrenCount();

                        if (totalOrders == 0) {
                            btnConfirm.setEnabled(true);
                            btnConfirm.setText("Xác nhận đặt hàng");
                            Toast.makeText(ConfirmOrderActivity.this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        for (DataSnapshot cartSnapshot : cartsSnapshot.getChildren()) {
                            DataSnapshot sellerIdSnapshot = cartSnapshot.child("sellerId");
                            DataSnapshot cartItemsSnapshot = cartSnapshot.child("cartItems");

                            if (!sellerIdSnapshot.exists() || !cartItemsSnapshot.exists()) continue;

                            String sellerId = sellerIdSnapshot.getValue(String.class);
                            if (sellerId == null) continue;

                            String orderId = generateOrderId();
                            Map<String, Object> orderData = createOrderData(user.getUid(), sellerId, status,
                                    fullAddress, cartItemsSnapshot);

                            ordersRef.child(orderId).setValue(orderData)
                                    .addOnSuccessListener(aVoid -> {
                                        if (status == 1) {
                                            updateServiceSoldCount(cartItemsSnapshot);
                                        }

                                        successCount[0]++;
                                        if (successCount[0] == totalOrders) {
                                            showPaymentNotification(selectedPaymentMethod, true);
                                            clearUserCarts(user.getUid());
                                            finish();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        btnConfirm.setEnabled(true);
                                        btnConfirm.setText("Xác nhận đặt hàng");
                                        showPaymentNotification(selectedPaymentMethod, false);
                                        Toast.makeText(ConfirmOrderActivity.this,
                                                "Lỗi tạo đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText("Xác nhận đặt hàng");
                        showPaymentNotification(selectedPaymentMethod, false);
                        Toast.makeText(ConfirmOrderActivity.this,
                                "Lỗi truy cập giỏ hàng: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateServiceSoldCount(DataSnapshot cartItemsSnapshot) {
        DatabaseReference servicesRef = FirebaseDatabase.getInstance().getReference("Services");
        
        for (DataSnapshot itemSnapshot : cartItemsSnapshot.getChildren()) {
            String serviceId = itemSnapshot.getKey();
            Integer quantity = itemSnapshot.getValue(Integer.class);
            
            if (serviceId == null || quantity == null || quantity <= 0) continue;

            updateServiceSoldDirect(servicesRef, serviceId, quantity);
        }
    }

    private void updateServiceSoldDirect(DatabaseReference servicesRef, String serviceId, int quantity) {
        servicesRef.child(serviceId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer currentSold = snapshot.child("sold").getValue(Integer.class);
                    if (currentSold == null) currentSold = 0;
                    
                    int newSold = currentSold + quantity;
                    servicesRef.child(serviceId).child("sold").setValue(newSold);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private Map<String, Object> createOrderData(String userId, String sellerId, int status, String fullAddress, DataSnapshot cartItemsSnapshot) {

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", userId);
        orderData.put("sellerId", sellerId);
        orderData.put("orderDate", ServerValue.TIMESTAMP);
        orderData.put("status", status);
        orderData.put("address", fullAddress);
        orderData.put("paymentMethod", selectedPaymentMethod);

        Map<String, Integer> items = new HashMap<>();
        long totalAmount = calculateTotalAmount(cartItemsSnapshot, items);

        orderData.put("items", items);
        orderData.put("totalAmount", totalAmount);

        return orderData;
    }

    private long calculateTotalAmount(DataSnapshot cartItemsSnapshot, Map<String, Integer> items) {
        long totalAmount = 0;
        
        for (DataSnapshot itemSnapshot : cartItemsSnapshot.getChildren()) {
            String serviceId = itemSnapshot.getKey();
            Integer amount = itemSnapshot.getValue(Integer.class);
            if (serviceId == null || amount == null) continue;

            items.put(serviceId, amount);

            for (CartItem cartItem : cartItemList) {
                if (cartItem != null && cartItem.getServiceId().equals(serviceId)) {
                    totalAmount += (long) cartItem.getPrice() * amount;
                    break;
                }
            }
        }
        return totalAmount;
    }

    private void clearUserCarts(String userId) {
        DatabaseReference cartsRef = FirebaseDatabase.getInstance().getReference("Carts");
        cartsRef.orderByChild("buyerId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                for (DataSnapshot cartSnapshot : snapshot.getChildren()) {
                    String cartId = cartSnapshot.getKey();
                    if (cartId != null) {
                        updates.put(cartId, null); // Set to null to delete
                    }
                }
                
                if (!updates.isEmpty()) {
                    cartsRef.updateChildren(updates);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error if needed
            }
        });
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
                    textViewCustomerName.setText(customerName != null ? customerName : "Khách hàng");
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
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi tải danh sách tỉnh thành: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updateDistrictList(ArrayList<District> districts) {
        districtNames.clear();
        if (districts != null) {
            for (District district : districts) {
                if (district != null && district.getName() != null) {
                    districtNames.add(district.getName());
                }
            }
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