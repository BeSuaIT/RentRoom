package com.example.timphongtro.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
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

public class ServiceDetailActivity extends AppCompatActivity {

    private TextView service_title, service_price, service_sold_count, service_description;
    private Button btn_add_to_cart;
    private ImageView button_cart, imageView_back;
    private Service service;
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private FirebaseUser user = firebaseAuth.getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_detail);

        DecimalFormat decimalFormat = new DecimalFormat("#,###.###");
        decimalFormat.setDecimalSeparatorAlwaysShown(false);
        Bundle bundle = getIntent().getExtras();
        ImageSlider imageSlider = findViewById(R.id.ServiceImage);
        ArrayList<SlideModel> slideModels = new ArrayList<>();
        service_title = findViewById(R.id.service_title);
        service_price = findViewById(R.id.service_price);
        service_sold_count = findViewById(R.id.service_sold_count);
        service_description = findViewById(R.id.service_description);
        btn_add_to_cart = findViewById(R.id.btn_add_to_cart);
        button_cart = findViewById(R.id.button_cart);
        imageView_back = findViewById(R.id.imageView_back);

        button_cart.setOnClickListener(v -> {
            Intent intent;
            if (user != null) {
                intent = new Intent(ServiceDetailActivity.this, CartActivity.class);
            } else {
                intent = new Intent(ServiceDetailActivity.this, LoginActivity.class);
            }
            startActivity(intent);
        });
        imageView_back.setOnClickListener(v -> finish());

        btn_add_to_cart.setOnClickListener(v -> {
            if (user != null) {
                String userID = user.getUid();
                DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("Carts/" + userID);

                cartRef.child(service.getServiceId()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            int currentAmount = dataSnapshot.getValue(Integer.class);
                            cartRef.child(service.getServiceId()).setValue(currentAmount + 1);
                        } else {
                            cartRef.child(service.getServiceId()).setValue(1);
                        }
                        Toast.makeText(ServiceDetailActivity.this, service.getTitle() + " added!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(ServiceDetailActivity.this, "Failed to add item", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Intent intent = new Intent(ServiceDetailActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        if (bundle != null){
            String serviceString = bundle.getString("ServiceData");
            Gson gson = new Gson();
            service = gson.fromJson(serviceString, Service.class);

            service_title.setText(service.getTitle());
            String price = decimalFormat.format(service.getPrice()) + " VNĐ";
            service_price.setText(price);
            service_sold_count.setText(String.valueOf(service.getSold()));
            service_description.setText(service.getDescription());

            if (service.getImages() != null && !service.getImages().isEmpty()) {
                for (String imageUrl : service.getImages()) {
                    slideModels.add(new SlideModel(imageUrl, ScaleTypes.FIT));
                }
            }
            imageSlider.setImageList(slideModels, ScaleTypes.FIT);
        }
    }
}