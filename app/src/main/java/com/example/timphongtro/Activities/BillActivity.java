package com.example.timphongtro.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Adapters.BillAdapter;
import com.example.timphongtro.Models.Bill;
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
    private BillAdapter billAdapter;
    private ArrayList<Bill> billList;
    private View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill);

        initializeViews();
        setupRecyclerView();
        fetchBills();
    }

    private void initializeViews() {
        findViewById(R.id.imageView_back).setOnClickListener(v -> finish());
        rcvBills = findViewById(R.id.rcvBills);
        emptyView = findViewById(R.id.emptyView);
    }

    private void setupRecyclerView() {
        billList = new ArrayList<>();
        billAdapter = new BillAdapter(this);
        rcvBills.setLayoutManager(new LinearLayoutManager(this));
        rcvBills.setAdapter(billAdapter);
    }

    private void fetchBills() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference billsRef = FirebaseDatabase.getInstance().getReference("Bills");

        billsRef.orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        billList.clear();
                        for (DataSnapshot billSnapshot : snapshot.getChildren()) {
                            Bill bill = billSnapshot.getValue(Bill.class);
                            if (bill != null) {
                                bill.setId(billSnapshot.getKey());
                                billList.add(bill);
                            }
                        }

                        billList.sort((b1, b2) ->
                                Long.compare(b2.getOrderDate(), b1.getOrderDate()));

                        billAdapter.updateData(billList);
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
        emptyView.setVisibility(billList.isEmpty() ? View.VISIBLE : View.GONE);
        rcvBills.setVisibility(billList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}