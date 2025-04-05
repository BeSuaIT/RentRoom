package com.example.timphongtro.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Adapters.CartAdapter;
import com.example.timphongtro.Models.Cart;
import com.example.timphongtro.Models.Service;
import com.example.timphongtro.R;
import com.example.timphongtro.Utils.CartDiffCallback;
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
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private FirebaseUser user = firebaseAuth.getCurrentUser();
    private DecimalFormat decimalFormat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        decimalFormat = new DecimalFormat("#,###.###");
        decimalFormat.setDecimalSeparatorAlwaysShown(false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        ImageView imageView_back = findViewById(R.id.imageView_back);
        Button btn_checkout = findViewById(R.id.button_checkout);
        textView_total = findViewById(R.id.Total_price);

        btn_checkout.setOnClickListener(v -> openConfirmOrder());
        imageView_back.setOnClickListener(v -> finish());
        initializeCartUI();
        retrieveCartItems();
    }

    private void initializeCartUI() {
        rcvcart = findViewById(R.id.rcvcart);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rcvcart.setLayoutManager(linearLayoutManager);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        cartList = new ArrayList<>();
        cartAdapter = new CartAdapter(getApplicationContext(), cartList);
        rcvcart.setAdapter(cartAdapter);
    }

    private void retrieveCartItems() {
        DatabaseReference servicesRef = FirebaseDatabase.getInstance().getReference("Services");
        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("Carts/" + user.getUid());

        servicesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot servicesSnapshot) {
                cartRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot cartSnapshot) {
                        List<Cart> newCartList = new ArrayList<>();

                        for (DataSnapshot cartItem : cartSnapshot.getChildren()) {
                            String serviceId = cartItem.getKey();
                            int amount = cartItem.getValue(Integer.class);

                            for (DataSnapshot category : servicesSnapshot.getChildren()) {
                                DataSnapshot serviceSnap = category.child(serviceId);
                                if (serviceSnap.exists()) {
                                    Service service = serviceSnap.getValue(Service.class);
                                    if (service != null) {
                                        Cart cart = new Cart(
                                                serviceId,
                                                service.getTitle(),
                                                service.getId_seller(),
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

                        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                                new CartDiffCallback(cartList, newCartList));

                        cartList.clear();
                        cartList.addAll(newCartList);
                        diffResult.dispatchUpdatesTo(cartAdapter);
                        updateRecyclerViewVisibility(cartList, rcvcart, findViewById(R.id.nohistory));
                        calculateTotalPrice(cartList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void calculateTotalPrice(ArrayList<Cart> cartList) {
        if (cartList.isEmpty()) {
            textView_total.setText("0 VNĐ");
            totalPriceValue = 0;
            return;
        }

        final long[] totalPrice = {0};
        final int[] itemsProcessed = {0};

        DatabaseReference servicesRef = FirebaseDatabase.getInstance().getReference("Services");
        servicesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot servicesSnapshot) {
                for (Cart cartItem : cartList) {
                    boolean found = false;
                    for (DataSnapshot categorySnapshot : servicesSnapshot.getChildren()) {
                        DataSnapshot serviceSnapshot = categorySnapshot.child(cartItem.getServiceId());
                        if (serviceSnapshot.exists()) {
                            Service serviceDetails = serviceSnapshot.getValue(Service.class);
                            if (serviceDetails != null) {
                                totalPrice[0] += (long) serviceDetails.getPrice() * cartItem.getAmount();
                                found = true;
                                break;
                            }
                        }
                    }

                    itemsProcessed[0]++;
                    if (!found) {
                        itemsProcessed[0]--;
                    }

                    if (itemsProcessed[0] == cartList.size()) {
                        totalPriceValue = totalPrice[0];
                        textView_total.setText(decimalFormat.format(totalPrice[0]) + " VNĐ");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CartActivity.this, "Error calculating total price", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRecyclerViewVisibility(ArrayList<Cart> cartList, RecyclerView rcvcart, View noHistoryView) {
        if (cartList.isEmpty()) {
            rcvcart.setVisibility(View.GONE);
            noHistoryView.setVisibility(View.VISIBLE);
        } else {
            rcvcart.setVisibility(View.VISIBLE);
            noHistoryView.setVisibility(View.GONE);
        }
    }

    private void openConfirmOrder() {
        Gson gson = new Gson();
        String cartJson = gson.toJson(cartList);

        Intent intent = new Intent(CartActivity.this, ConfirmOrderActivity.class);
        intent.putExtra("cartList", cartJson);
        intent.putExtra("totalPrice", totalPriceValue);
        startActivity(intent);
    }
}