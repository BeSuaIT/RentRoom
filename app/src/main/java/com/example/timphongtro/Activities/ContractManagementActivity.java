package com.example.timphongtro.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import java.util.Collections;

public class ContractManagementActivity extends AppCompatActivity {

    private RecyclerView contractsRecyclerView;
    private ContractAdapter contractAdapter;
    private ArrayList<Contract> contractsList;
    private FloatingActionButton fabAddContract;
    private ImageView backButton;
    private LinearLayout noDataLayout;
    private TextView noDataTitle, noDataMessage;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference contractsRef, usersRef;
    private String currentUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contract_management);

        initializeViews();
        initializeFirebase();
        setupListeners();
        setupRecyclerView();
        loadUserRoleAndSetup();
    }

    private void initializeViews() {
        contractsRecyclerView = findViewById(R.id.contracts_recycler_view);
        fabAddContract = findViewById(R.id.fab_add_contract);
        backButton = findViewById(R.id.back_button);
        noDataLayout = findViewById(R.id.no_data_layout);
        noDataTitle = findViewById(R.id.no_data_title);
        noDataMessage = findViewById(R.id.no_data_message);
    }

    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        contractsRef = FirebaseDatabase.getInstance().getReference("Contracts");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        contractsList = new ArrayList<>();
        contractAdapter = new ContractAdapter(this, contractsList);
        contractsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        contractsRecyclerView.setAdapter(contractAdapter);
    }

    private void loadUserRoleAndSetup() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            showNoDataState("Chưa đăng nhập", "Vui lòng đăng nhập để xem hợp đồng");
            fabAddContract.setVisibility(View.GONE);
            return;
        }

        usersRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentUserRole = snapshot.child("role").getValue(String.class);
                    setupUIBasedOnRole();
                    loadContracts();
                } else {
                    currentUserRole = null;
                    showNoDataState("Lỗi tài khoản", "Không tìm thấy thông tin tài khoản");
                    fabAddContract.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                currentUserRole = null;
                showNoDataState("Lỗi kết nối", "Không thể tải thông tin người dùng");
                fabAddContract.setVisibility(View.GONE);
            }
        });
    }

    private void setupUIBasedOnRole() {
        if ("Chủ trọ".equals(currentUserRole)) {
            fabAddContract.setVisibility(View.VISIBLE);
            fabAddContract.setOnClickListener(v -> {
                Intent intent = new Intent(ContractManagementActivity.this, AddContractActivity.class);
                startActivity(intent);
            });
        } else {
            fabAddContract.setVisibility(View.GONE);
        }
    }

    private void loadContracts() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null || currentUserRole == null) {
            showNoDataState("Chưa xác định quyền", "Không thể xác định quyền truy cập");
            return;
        }

        contractsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contractsList.clear();

                if (!snapshot.exists()) {
                    showNoDataState();
                    return;
                }

                for (DataSnapshot contractSnapshot : snapshot.getChildren()) {
                    Contract contract = contractSnapshot.getValue(Contract.class);
                    if (contract != null && shouldShowContract(contract, currentUser.getUid())) {
                        contractsList.add(contract);
                    }
                }

                Collections.sort(contractsList, (c1, c2) -> Long.compare(c2.getCreatedAt(), c1.getCreatedAt()));

                if (contractsList.isEmpty()) {
                    showNoDataState();
                } else {
                    showDataState();
                }

                contractAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showNoDataState("Lỗi tải dữ liệu", "Không thể tải danh sách hợp đồng");
                Toast.makeText(ContractManagementActivity.this,
                        "Lỗi tải danh sách hợp đồng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean shouldShowContract(Contract contract, String currentUserId) {
        switch (currentUserRole) {
            case "Chủ trọ":
                return currentUserId.equals(contract.getLandlordId());
            case "Người thuê":
                return currentUserId.equals(contract.getTenantId());
            default:
                return false;
        }
    }

    private void showNoDataState() {
        if ("Chủ trọ".equals(currentUserRole)) {
            showNoDataState("Chưa có hợp đồng", "Bạn chưa tạo hợp đồng nào.\nNhấn nút + để tạo hợp đồng mới.");
        } else if ("Người thuê".equals(currentUserRole)) {
            showNoDataState("Chưa có hợp đồng", "Bạn chưa có hợp đồng thuê nào.");
        } else if ("Admin".equals(currentUserRole)) {
            showNoDataState("Chưa có hợp đồng", "Hệ thống chưa có hợp đồng nào.");
        } else {
            showNoDataState("Không có quyền truy cập", "Bạn không có quyền xem hợp đồng.");
        }
    }

    private void showNoDataState(String title, String message) {
        contractsRecyclerView.setVisibility(View.GONE);
        if (noDataLayout != null) {
            noDataLayout.setVisibility(View.VISIBLE);
            if (noDataTitle != null) noDataTitle.setText(title);
            if (noDataMessage != null) noDataMessage.setText(message);
        } else {
            Toast.makeText(this, title + ": " + message, Toast.LENGTH_LONG).show();
        }
    }

    private void showDataState() {
        contractsRecyclerView.setVisibility(View.VISIBLE);
        if (noDataLayout != null) {
            noDataLayout.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUserRole != null) {
            loadContracts();
        }
    }
}