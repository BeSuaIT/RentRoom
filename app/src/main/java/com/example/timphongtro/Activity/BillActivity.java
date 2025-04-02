package com.example.timphongtro.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Entity.Bill;
import com.example.timphongtro.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class BillActivity extends AppCompatActivity {
    private RecyclerView rcvBills;
    private ArrayList<Bill> billList;
    private FirebaseAuth firebaseAuth;
    private String userID;
    private View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill);

        initializeViews();
        setupFirebase();
        setupRecyclerView();
        fetchBills();
    }

    private void initializeViews() {
        ImageView imageView_back = findViewById(R.id.imageView_back);
        imageView_back.setOnClickListener(v -> finish());

        rcvBills = findViewById(R.id.rcvBills);
        emptyView = findViewById(R.id.emptyView);
    }

    private void setupFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        userID = firebaseAuth.getCurrentUser().getUid();
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        rcvBills.setLayoutManager(layoutManager);

        billList = new ArrayList<>();
    }

    private void fetchBills() {
        DatabaseReference billsRef = FirebaseDatabase.getInstance()
                .getReference("Bills");

        billsRef.orderByChild("userId").equalTo(userID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        billList.clear();
                        for (DataSnapshot billSnapshot : snapshot.getChildren()) {
                            Bill bill = billSnapshot.getValue(Bill.class);
                            if (bill != null) {
                                billList.add(bill);
                            }
                        }

                        // Sort by timestamp descending (newest first)
                        billList.sort((b1, b2) ->
                                Long.compare(b2.getTimestamp(), b1.getTimestamp()));
                        updateEmptyView();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(BillActivity.this,
                                "Lỗi: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateEmptyView() {
        if (billList.isEmpty()) {
            rcvBills.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            rcvBills.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
}