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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_cart);

        initializeViews();
        initializeData();
        retrieveCartItems();
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

        user = FirebaseAuth.getInstance().getCurrentUser();
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

                        // Lặp qua các sellerId
                        for (DataSnapshot sellerSnapshot : cartSnapshot.getChildren()) {
                            String sellerId = sellerSnapshot.getKey();

                            // Lặp qua các serviceId của mỗi seller
                            for (DataSnapshot serviceSnapshot : sellerSnapshot.getChildren()) {
                                String serviceId = serviceSnapshot.getKey();
                                Integer amount = serviceSnapshot.getValue(Integer.class);

                                if (serviceId == null || amount == null) continue;

                                // Tìm thông tin service từ Services
                                for (DataSnapshot category : servicesSnapshot.getChildren()) {
                                    DataSnapshot serviceSnap = category.child(serviceId);
                                    if (serviceSnap.exists()) {
                                        Service service = serviceSnap.getValue(Service.class);
                                        if (service != null) {
                                            Cart cart = new Cart(
                                                    serviceId,
                                                    service.getTitle(),
                                                    sellerId,
                                                    amount,
                                                    service.getPrice(),
                                                    service.getImages()
                                            );
                                            newCartList.add(cart);
                                            break;
                                        }
                                    }
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
                        Toast.makeText(CartActivity.this, "Lỗi tải giỏ hàng", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CartActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRecyclerViewVisibility() {
        View noHistoryView = findViewById(R.id.nohistory);
        if (cartList.isEmpty()) {
            rcvcart.setVisibility(View.GONE);
            noHistoryView.setVisibility(View.VISIBLE);
        } else {
            rcvcart.setVisibility(View.VISIBLE);
            noHistoryView.setVisibility(View.GONE);
        }
    }

    private void calculateTotalPrice() {
        totalPriceValue = 0;
        for (Cart cart : cartList) {
            totalPriceValue += (long) cart.getPrice() * cart.getAmount();
        }
        textView_total.setText(decimalFormat.format(totalPriceValue) + " VNĐ");
    }

    private void openConfirmOrder() {
        if (cartList.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        Gson gson = new Gson();
        String cartJson = gson.toJson(cartList);

        Intent intent = new Intent(this, ConfirmOrderActivity.class);
        intent.putExtra("cartList", cartJson);
        intent.putExtra("totalPrice", totalPriceValue);
        startActivity(intent);
    }
}