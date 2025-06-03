package com.example.timphongtro.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Adapters.ContractAdapter;
import com.example.timphongtro.Models.Contract;
import com.example.timphongtro.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ContractManagementActivity extends AppCompatActivity {

    private RecyclerView contractsRecyclerView;
    private ContractAdapter contractAdapter;
    private ArrayList<Contract> contractsList;
    private FloatingActionButton fabAddContract;
    private ImageView backButton;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference contractsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contract_management);

        initializeViews();
        initializeFirebase();
        setupListeners();
        setupRecyclerView();
        loadContracts();
    }

    private void initializeViews() {
        contractsRecyclerView = findViewById(R.id.contracts_recycler_view);
        fabAddContract = findViewById(R.id.fab_add_contract);
        backButton = findViewById(R.id.back_button);
    }

    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        contractsRef = FirebaseDatabase.getInstance().getReference("Contracts");
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        fabAddContract.setOnClickListener(v -> {
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null) {
                Intent intent = new Intent(this, AddContractActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Vui lòng đăng nhập để tiếp tục", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        contractsList = new ArrayList<>();
        contractAdapter = new ContractAdapter(this, contractsList);
        contractsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        contractsRecyclerView.setAdapter(contractAdapter);
    }

    private void loadContracts() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;

        contractsRef.orderByChild("landlordId")
                .equalTo(currentUser.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        contractsList.clear();
                        for (DataSnapshot contractSnapshot : snapshot.getChildren()) {
                            Contract contract = contractSnapshot.getValue(Contract.class);
                            if (contract != null) {
                                contractsList.add(contract);
                            }
                        }
                        contractAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ContractManagementActivity.this,
                                "Lỗi tải danh sách hợp đồng", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadContracts(); // Refresh khi quay lại từ AddContractActivity
    }
}