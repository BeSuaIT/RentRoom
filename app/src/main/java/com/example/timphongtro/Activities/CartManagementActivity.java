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
import com.example.timphongtro.Models.CartItem;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartManagementActivity extends AppCompatActivity {

    private TextView textView_total;
    private long totalPriceValue = 0;
    private RecyclerView rcvcart;
    private CartAdapter cartAdapter;
    private ArrayList<CartItem> cartItemList;
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

        cartItemList = new ArrayList<>();
        cartAdapter = new CartAdapter(this, cartItemList);
        rcvcart.setAdapter(cartAdapter);
    }

    private void retrieveCartItems() {
        if (user == null) return;

        cleanupInvalidCartItems(() -> {
            loadValidCartItems();
        });
    }

    private void cleanupInvalidCartItems(Runnable onComplete) {
        if (user == null) {
            onComplete.run();
            return;
        }

        DatabaseReference servicesRef = FirebaseDatabase.getInstance().getReference("Services");
        DatabaseReference cartsRef = FirebaseDatabase.getInstance().getReference("Carts");

        cartsRef.orderByChild("buyerId").equalTo(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot cartsSnapshot) {
                if (!cartsSnapshot.exists() || cartsSnapshot.getChildrenCount() == 0) {
                    onComplete.run();
                    return;
                }

                servicesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot servicesSnapshot) {
                        Map<String, Object> updates = new HashMap<>();

                        for (DataSnapshot cartSnapshot : cartsSnapshot.getChildren()) {
                            Cart cart = cartSnapshot.getValue(Cart.class);
                            if (cart == null || cart.getCartItems() == null) continue;

                            String cartId = cartSnapshot.getKey();
                            Map<String, Integer> cartItems = cart.getCartItems();
                            Map<String, Integer> validItems = new HashMap<>();

                            for (Map.Entry<String, Integer> entry : cartItems.entrySet()) {
                                String serviceId = entry.getKey();
                                Integer amount = entry.getValue();

                                if (amount != null && amount > 0 && servicesSnapshot.hasChild(serviceId)) {
                                    validItems.put(serviceId, amount);
                                }
                            }

                            if (validItems.isEmpty()) {
                                updates.put(cartId, null);
                            } else if (validItems.size() != cartItems.size()) {
                                updates.put(cartId + "/cartItems", validItems);
                            }
                        }

                        if (!updates.isEmpty()) {
                            cartsRef.updateChildren(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        new android.os.Handler().postDelayed(onComplete, 300);
                                    })
                                    .addOnFailureListener(e -> onComplete.run());
                        } else {
                            onComplete.run();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        onComplete.run();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                onComplete.run();
            }
        });
    }

    private void loadValidCartItems() {
        if (user == null) return;

        DatabaseReference servicesRef = FirebaseDatabase.getInstance().getReference("Services");
        DatabaseReference cartsRef = FirebaseDatabase.getInstance().getReference("Carts");

        servicesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot servicesSnapshot) {
                cartsRef.orderByChild("buyerId").equalTo(user.getUid())
                        .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot cartsSnapshot) {
                        List<CartItem> newCartItemList = new ArrayList<>();

                        for (DataSnapshot cartSnapshot : cartsSnapshot.getChildren()) {
                            Cart cart = cartSnapshot.getValue(Cart.class);
                            if (cart == null || cart.getCartItems() == null) continue;

                            String cartId = cartSnapshot.getKey();
                            String sellerId = cart.getSellerId();

                            for (Map.Entry<String, Integer> entry : cart.getCartItems().entrySet()) {
                                String serviceId = entry.getKey();
                                Integer amount = entry.getValue();

                                if (amount == null || amount <= 0) continue;

                                Service foundService = findServiceInSnapshot(servicesSnapshot, serviceId);
                                if (foundService != null && isValidService(foundService)) {
                                    CartItem cartItem = new CartItem(
                                            serviceId,
                                            foundService.getTitle(),
                                            sellerId,
                                            amount,
                                            foundService.getPrice(),
                                            foundService.getImages(),
                                            cartId
                                    );
                                    newCartItemList.add(cartItem);
                                }
                            }
                        }

                        runOnUiThread(() -> {
                            cartItemList.clear();
                            cartItemList.addAll(newCartItemList);
                            cartAdapter.updateData(cartItemList);
                            updateRecyclerViewVisibility();
                            calculateTotalPrice();
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        runOnUiThread(() -> {
                            updateRecyclerViewVisibility();
                        });
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> {
                    updateRecyclerViewVisibility();
                });
            }
        });
    }

    private void updateRecyclerViewVisibility() {
        View noHistoryView = findViewById(R.id.nohistory);
        if (cartItemList.isEmpty()) {
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
        for (CartItem cartItem : cartItemList) {
            if (cartItem != null) {
                totalPriceValue += (long) cartItem.getPrice() * cartItem.getAmount();
            }
        }
        textView_total.setText(decimalFormat.format(totalPriceValue) + " VNĐ");
    }

    private void openConfirmOrder() {
        if (user == null) {
            AuthUtils.showLoginRequiredDialog(this, "đặt hàng", "có thể");
            return;
        }

        if (cartItemList.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        String cartJson = GsonUtils.toJson(cartItemList);
        if (cartJson != null) {
            Intent intent = new Intent(this, ConfirmOrderActivity.class);
            intent.putExtra("cartList", cartJson);
            intent.putExtra("totalPrice", totalPriceValue);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Lỗi xử lý dữ liệu giỏ hàng", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidService(Service service) {
        if (service == null) return false;
        if (service.getServiceId() == null || service.getServiceId().trim().isEmpty()) return false;
        if (service.getTitle() == null || service.getTitle().trim().isEmpty()) return false;
        if (service.getPrice() <= 0) return false;
        return true;
    }

    private Service findServiceInSnapshot(DataSnapshot servicesSnapshot, String serviceId) {
        if (servicesSnapshot == null || serviceId == null) return null;

        DataSnapshot serviceSnap = servicesSnapshot.child(serviceId);
        if (serviceSnap.exists()) {
            Service service = serviceSnap.getValue(Service.class);
            if (service != null && isValidService(service)) {
                service.setServiceId(serviceId);
                return service;
            }
        }
        return null;
    }
}