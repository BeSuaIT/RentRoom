package com.example.timphongtro.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Adapters.CartAdapter;
import com.example.timphongtro.Models.Cart;
import com.example.timphongtro.Models.Service;
import com.example.timphongtro.R;
import com.example.timphongtro.Utils.AuthUtils;
import com.example.timphongtro.Utils.GsonUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CartManagementActivity extends AppCompatActivity {

    private TextView textView_total;
    private long totalPriceValue = 0;
    private RecyclerView rcvcart;
    private CartAdapter cartAdapter;
    private ArrayList<Cart> cartList;
    private DecimalFormat decimalFormat;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart_management);

        initializeViews();
        initializeData();
        checkUserAndRetrieveCart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserAndRetrieveCart();
    }

    private void checkUserAndRetrieveCart() {
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            AuthUtils.isUserExists(user, exists -> {
                if (!exists) {
                    AuthUtils.clearAllLoginData(this);
                    AuthUtils.showLoginRequiredDialog(this, "giỏ hàng", "xem");
                    return;
                }

                retrieveCartItems();
            });
        } else {
            AuthUtils.showLoginRequiredDialog(this, "giỏ hàng", "xem");
        }
    }

    private void initializeViews() {
        textView_total = findViewById(R.id.Total_price);
        rcvcart = findViewById(R.id.rcvcart);

        findViewById(R.id.imageView_back).setOnClickListener(v -> finish());
        findViewById(R.id.button_checkout).setOnClickListener(v -> openConfirmOrder());

        rcvcart.setLayoutManager(new LinearLayoutManager(this));
        rcvcart.setHasFixedSize(true);
    }

    private void initializeData() {
        decimalFormat = new DecimalFormat("#,###.###");
        decimalFormat.setDecimalSeparatorAlwaysShown(false);

        cartList = new ArrayList<>();
        cartAdapter = new CartAdapter(this, cartList);
        rcvcart.setAdapter(cartAdapter);
    }

    private void retrieveCartItems() {
        if (user == null) return;

        DatabaseReference servicesRef = FirebaseDatabase.getInstance().getReference("Services");
        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("Carts").child(user.getUid());

        servicesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot servicesSnapshot) {
                cartRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot cartSnapshot) {
                        List<Cart> newCartList = new ArrayList<>();

                        for (DataSnapshot sellerSnapshot : cartSnapshot.getChildren()) {
                            String sellerId = sellerSnapshot.getKey();
                            if (sellerId == null) continue;

                            for (DataSnapshot serviceSnapshot : sellerSnapshot.getChildren()) {
                                String serviceId = serviceSnapshot.getKey();
                                Integer amount = serviceSnapshot.getValue(Integer.class);

                                if (serviceId == null || amount == null || amount <= 0) continue;

                                Service foundService = findServiceInSnapshot(servicesSnapshot, serviceId);
                                if (foundService != null) {
                                    Cart cart = new Cart(
                                            serviceId,
                                            foundService.getTitle(),
                                            sellerId,
                                            amount,
                                            foundService.getPrice(),
                                            foundService.getImages()
                                    );
                                    newCartList.add(cart);
                                }
                            }
                        }

                        runOnUiThread(() -> {
                            cartList.clear();
                            cartList.addAll(newCartList);
                            cartAdapter.updateData(cartList);
                            updateRecyclerViewVisibility();
                            calculateTotalPrice();
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CartManagementActivity.this, "Lỗi tải giỏ hàng: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CartManagementActivity.this, "Lỗi tải dữ liệu dịch vụ: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Service findServiceInSnapshot(DataSnapshot servicesSnapshot, String serviceId) {
        DataSnapshot serviceSnap = servicesSnapshot.child(serviceId);
        if (serviceSnap.exists()) {
            Service service = serviceSnap.getValue(Service.class);
            if (service != null) {
                service.setServiceId(serviceId); // Đảm bảo có serviceId
            }
            return service;
        }
        return null;
    }

    private void updateRecyclerViewVisibility() {
        View noHistoryView = findViewById(R.id.nohistory);
        if (cartList.isEmpty()) {
            rcvcart.setVisibility(View.GONE);
            noHistoryView.setVisibility(View.VISIBLE);
            textView_total.setText("0 VNĐ");
        } else {
            rcvcart.setVisibility(View.VISIBLE);
            noHistoryView.setVisibility(View.GONE);
        }
    }

    private void calculateTotalPrice() {
        totalPriceValue = 0;
        for (Cart cart : cartList) {
            if (cart != null) {
                totalPriceValue += (long) cart.getPrice() * cart.getAmount();
            }
        }
        textView_total.setText(decimalFormat.format(totalPriceValue) + " VNĐ");
    }

    private void openConfirmOrder() {
        if (user == null) {
            AuthUtils.showLoginRequiredDialog(this, "đặt hàng", "có thể");
            return;
        }

        if (cartList.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        String cartJson = GsonUtils.toJson(cartList);
        if (cartJson != null) {
            Intent intent = new Intent(this, ConfirmOrderActivity.class);
            intent.putExtra("cartList", cartJson);
            intent.putExtra("totalPrice", totalPriceValue);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Lỗi xử lý dữ liệu giỏ hàng", Toast.LENGTH_SHORT).show();
        }
    }
}