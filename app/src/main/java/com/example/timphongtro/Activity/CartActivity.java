package com.example.timphongtro.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Adapter.CartAdapter;
import com.example.timphongtro.Entity.Cart;
import com.example.timphongtro.Entity.Service;
import com.example.timphongtro.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class CartActivity extends AppCompatActivity {

    private TextView textView_total;
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
        imageView_back.setColorFilter(ContextCompat.getColor(this, R.color.white));

        Button btn_checkout = findViewById(R.id.button_checkout);
        textView_total = findViewById(R.id.Total_price);

        btn_checkout.setOnClickListener(v -> Toast.makeText(CartActivity.this,  "has been paid!", Toast.LENGTH_SHORT).show());
        imageView_back.setOnClickListener(v -> finish());
        UIproccess();
        fetchproductfromDB();
    }

    private void UIproccess() {
        rcvcart = findViewById(R.id.rcvcart);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rcvcart.setLayoutManager(linearLayoutManager);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        cartList = new ArrayList<>();
        cartAdapter = new CartAdapter(getApplicationContext(), cartList);
        rcvcart.setAdapter(cartAdapter);
    }

    private void fetchproductfromDB() {
        String userID = user.getUid();
        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("Carts/" + userID);

        cartRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cartList.clear();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    String serviceId = childSnapshot.getKey();
                    int amount = childSnapshot.getValue(Integer.class);
                    Cart cart = new Cart(serviceId, amount);
                    cartList.add(cart);
                }
                cartAdapter.notifyDataSetChanged();
                updateRecyclerViewVisibility(cartList, rcvcart, findViewById(R.id.nohistory));
                calculateTotalPrice(cartList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CartActivity.this, "Error fetching cart data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateTotalPrice(ArrayList<Cart> cartList) {
        if (cartList.isEmpty()) {
            textView_total.setText("0 VNĐ");
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
                                totalPrice[0] += serviceDetails.getPrice() * cartItem.getAmount();
                                found = true;
                                break;
                            }
                        }
                    }

                    itemsProcessed[0]++;
                    if (!found) {
                        // Service not found in any category
                        itemsProcessed[0]--;
                    }

                    if (itemsProcessed[0] == cartList.size()) {
                        // All items processed, update UI
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
}