package com.example.timphongtro.Activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.timphongtro.Adapters.ZoomImageAdapter;
import com.example.timphongtro.Models.Contract;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.R;
import com.example.timphongtro.Utils.ContractUtils;
import com.example.timphongtro.Utils.GsonUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ContractDetailActivity extends AppCompatActivity {

    private ImageView backButton;
    private TextView roomTitleTv, roomAddressTv, roomPriceTv;
    private TextView landlordNameTv, landlordPhoneTv;
    private TextView tenantNameTv, tenantPhoneTv, tenantCCCDTv;
    private TextView contractPeriodTv, statusTv, createdAtTv;
    private ImageView cccdFrontImg, cccdBackImg;
    private MaterialButton editContractBtn, deleteContractBtn;
    private Dialog dialogZoomImg;
    private Contract contract;
    private DatabaseReference contractsRef, usersRef;
    private FirebaseAuth firebaseAuth;
    private String currentUserRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contract_detail);

        contractsRef = FirebaseDatabase.getInstance().getReference("Contracts");
        firebaseAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        
        initializeViews();
        getContractData();
        loadCurrentUserRole();
        setupListeners();
        displayContractInfo();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.back_button);
        roomTitleTv = findViewById(R.id.room_title_tv);
        roomAddressTv = findViewById(R.id.room_address_tv);
        roomPriceTv = findViewById(R.id.room_price_tv);
        landlordNameTv = findViewById(R.id.landlord_name_tv);
        landlordPhoneTv = findViewById(R.id.landlord_phone_tv);
        tenantNameTv = findViewById(R.id.tenant_name_tv);
        tenantPhoneTv = findViewById(R.id.tenant_phone_tv);
        tenantCCCDTv = findViewById(R.id.tenant_cccd_tv);
        contractPeriodTv = findViewById(R.id.contract_period_tv);
        statusTv = findViewById(R.id.status_tv);
        createdAtTv = findViewById(R.id.created_at_tv);
        cccdFrontImg = findViewById(R.id.cccd_front_img);
        cccdBackImg = findViewById(R.id.cccd_back_img);
        editContractBtn = findViewById(R.id.edit_contract_btn);
        deleteContractBtn = findViewById(R.id.delete_contract_btn);
    }

    private void loadCurrentUserRole() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            usersRef.child(currentUser.getUid())
                .child("role")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            currentUserRole = snapshot.getValue(String.class);
                            if (currentUserRole == null) currentUserRole = "";
                            setupActionButtons();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        currentUserRole = "";
                        setupActionButtons();
                    }
                });
        }
    }

    private void getContractData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("contractJson")) {
            String contractJson = intent.getStringExtra("contractJson");
            contract = GsonUtils.fromJson(contractJson, Contract.class);
            
            if (contract == null) {
                Toast.makeText(this, "Lỗi tải thông tin hợp đồng", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin hợp đồng", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
        
        cccdFrontImg.setOnClickListener(v -> {
            if (contract != null && contract.getCccdFrontImage() != null && !contract.getCccdFrontImage().isEmpty()) {
                showZoomImgDialog(0);
            } else {
                Toast.makeText(this, "Không có ảnh CCCD mặt trước", Toast.LENGTH_SHORT).show();
            }
        });
        
        cccdBackImg.setOnClickListener(v -> {
            if (contract != null && contract.getCccdBackImage() != null && !contract.getCccdBackImage().isEmpty()) {
                showZoomImgDialog(1);
            } else {
                Toast.makeText(this, "Không có ảnh CCCD mặt sau", Toast.LENGTH_SHORT).show();
            }
        });

        editContractBtn.setOnClickListener(v -> {
            Intent editIntent = new Intent(this, EditContractActivity.class);
            String contractJson = GsonUtils.toJson(contract);
            editIntent.putExtra("contractJson", contractJson);
            startActivity(editIntent);
            finish();
        });

        deleteContractBtn.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void displayContractInfo() {
        if (contract == null) return;

        loadRoomData();
        displayBasicContractInfo();
        loadCCCDImages();
        setupActionButtons();
    }

    private void displayBasicContractInfo() {
        landlordNameTv.setText(contract.getLandlordName());
        landlordPhoneTv.setText(contract.getLandlordPhone());
        tenantNameTv.setText(contract.getTenantName());
        tenantPhoneTv.setText(contract.getTenantPhone());
        tenantCCCDTv.setText(contract.getTenantCCCD());

        String startDate = formatTimestamp(contract.getStartDate());
        String endDate = formatTimestamp(contract.getEndDate());
        contractPeriodTv.setText(startDate + " - " + endDate);

        ContractUtils.syncContractStatusWithDatabase(contract);

        int currentStatus = ContractUtils.getCurrentStatus(contract);
        String statusText = getStatusText(currentStatus);
        statusTv.setText(statusText);
        statusTv.setTextColor(getStatusColor(currentStatus));

        String createdDate = formatTimestampWithTime(contract.getCreatedAt());
        createdAtTv.setText("Tạo lúc: " + createdDate);
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp <= 0) return "Chưa xác định";
        
        try {
            Date date = new Date(timestamp);
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
            return formatter.format(date);
        } catch (Exception e) {
            return "Thời gian không hợp lệ";
        }
    }

    private String formatTimestampWithTime(long timestamp) {
        if (timestamp <= 0) return "Chưa xác định";
        
        try {
            Date date = new Date(timestamp);
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
            return formatter.format(date);
        } catch (Exception e) {
            return "Thời gian không hợp lệ";
        }
    }

    private void setupActionButtons() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        
        if (currentUser != null && contract != null) {
            boolean isLandlord = currentUser.getUid().equals(contract.getLandlordId());
            boolean isLandlordRole = "Chủ trọ".equals(currentUserRole);
            boolean isExpired = ContractUtils.isExpired(contract);
            
            if (isLandlord && isLandlordRole && isExpired) {
                editContractBtn.setVisibility(View.VISIBLE);
                deleteContractBtn.setVisibility(View.VISIBLE);
                editContractBtn.setText("Chỉnh sửa / Gia hạn");
                editContractBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.orange_100));
                deleteContractBtn.setStrokeColor(ContextCompat.getColorStateList(this, R.color.red));
            } else {
                editContractBtn.setVisibility(View.GONE);
                deleteContractBtn.setVisibility(View.GONE);
            }
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa hợp đồng")
                .setMessage("Bạn có chắc chắn muốn xóa hợp đồng này?\n\n" +
                           "• Hành động này không thể hoàn tác\n" +
                           "• Phòng sẽ chuyển về trạng thái trống\n" +
                           "• Tất cả dữ liệu hợp đồng sẽ bị xóa vĩnh viễn")
                .setPositiveButton("Xóa hợp đồng", (dialog, which) -> deleteContract())
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteContract() {
        if (contract == null) return;

        contractsRef.child(contract.getContractId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    updateRoomStatusAfterDelete();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi xóa hợp đồng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateRoomStatusAfterDelete() {
        DatabaseReference roomRef = FirebaseDatabase.getInstance()
                .getReference("Posts")
                .child(contract.getRoomId())
                .child("status_room");

        roomRef.setValue(0)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa hợp đồng thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Hợp đồng đã xóa nhưng không thể cập nhật trạng thái phòng", 
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void loadRoomData() {
        if (contract.getRoomId() == null) {
            roomTitleTv.setText("Phòng không xác định");
            roomAddressTv.setText("Địa chỉ không xác định");
            roomPriceTv.setText("0 VNĐ/tháng");
            return;
        }

        DatabaseReference roomRef = FirebaseDatabase.getInstance()
                .getReference("Posts")
                .child(contract.getRoomId());

        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Room room = snapshot.getValue(Room.class);
                    if (room != null) {
                        roomTitleTv.setText(room.getTitle_room() != null ? 
                                          room.getTitle_room() : "Phòng trọ");

                        String address = "Địa chỉ không xác định";
                        if (room.getAddress() != null) {
                            address = room.getAddress().getAddress_combine();
                        }
                        roomAddressTv.setText(address);

                        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
                        String formattedPrice = formatter.format(room.getPrice_room()) + " VNĐ/tháng";
                        roomPriceTv.setText(formattedPrice);
                    }
                } else {
                    roomTitleTv.setText("Phòng đã bị xóa");
                    roomAddressTv.setText("Không tìm thấy thông tin");
                    roomPriceTv.setText("N/A");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                roomTitleTv.setText("Lỗi tải thông tin phòng");
                roomAddressTv.setText("Vui lòng thử lại");
                roomPriceTv.setText("N/A");
            }
        });
    }

    private void loadCCCDImages() {
        if (contract.getCccdFrontImage() != null && !contract.getCccdFrontImage().isEmpty()) {
            Glide.with(this)
                    .load(contract.getCccdFrontImage())
                    .placeholder(R.drawable.img_no_image)
                    .error(R.drawable.img_no_image)
                    .into(cccdFrontImg);
        }

        if (contract.getCccdBackImage() != null && !contract.getCccdBackImage().isEmpty()) {
            Glide.with(this)
                    .load(contract.getCccdBackImage())
                    .placeholder(R.drawable.img_no_image)
                    .error(R.drawable.img_no_image)
                    .into(cccdBackImg);
        }
    }

    private void showZoomImgDialog(int currentPosition) {
        dialogZoomImg = new Dialog(ContractDetailActivity.this);
        dialogZoomImg.setContentView(R.layout.dialog_zoom_img);

        ViewPager2 viewPager = dialogZoomImg.findViewById(R.id.viewPagerZoom);
        ImageView imageViewBack = dialogZoomImg.findViewById(R.id.imageViewBack);

        ArrayList<String> cccdImages = new ArrayList<>();
        
        if (contract.getCccdFrontImage() != null && !contract.getCccdFrontImage().isEmpty()) {
            cccdImages.add(contract.getCccdFrontImage());
        }
        
        if (contract.getCccdBackImage() != null && !contract.getCccdBackImage().isEmpty()) {
            cccdImages.add(contract.getCccdBackImage());
        }

        if (!cccdImages.isEmpty()) {
            ZoomImageAdapter adapter = new ZoomImageAdapter(this, cccdImages);
            viewPager.setAdapter(adapter);
            
            int adjustedPosition = currentPosition;
            if (cccdImages.size() == 1) {
                adjustedPosition = 0;
            } else if (currentPosition >= cccdImages.size()) {
                adjustedPosition = cccdImages.size() - 1;
            }
            
            viewPager.setCurrentItem(adjustedPosition, false);
        }

        imageViewBack.setOnClickListener(v -> {
            if (dialogZoomImg.isShowing()) {
                dialogZoomImg.dismiss();
            }
        });

        dialogZoomImg.show();
        Window window = dialogZoomImg.getWindow();
        if (window != null) {
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

    private String getStatusText(int status) {
        switch (status) {
            case 0: return "Nháp";
            case 1: return "Đang hiệu lực";
            case 2: return "Hết hạn";
            default: return "Không xác định";
        }
    }

    private int getStatusColor(int status) {
        switch (status) {
            case 0: return ContextCompat.getColor(this, R.color.gray);
            case 1: return ContextCompat.getColor(this, R.color.green);
            case 2: return ContextCompat.getColor(this, R.color.red);
            default: return ContextCompat.getColor(this, R.color.black);
        }
    }
}