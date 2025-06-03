package com.example.timphongtro.Activities;

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
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.timphongtro.Adapters.ZoomImageAdapter;
import com.example.timphongtro.Models.Contract;
import com.example.timphongtro.R;

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
    
    private Dialog dialogZoomImg;
    private Contract contract;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contract_detail);

        initializeViews();
        getContractData();
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
    }

    private void getContractData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("contract")) {
            contract = (Contract) intent.getSerializableExtra("contract");
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
        
        // ✅ Click để xem ảnh CCCD full screen với zoom
        cccdFrontImg.setOnClickListener(v -> {
            if (contract != null && contract.getCccdFrontImage() != null && !contract.getCccdFrontImage().isEmpty()) {
                showZoomImgDialog(0); // Index 0 cho ảnh mặt trước
            } else {
                Toast.makeText(this, "Không có ảnh CCCD mặt trước", Toast.LENGTH_SHORT).show();
            }
        });
        
        cccdBackImg.setOnClickListener(v -> {
            if (contract != null && contract.getCccdBackImage() != null && !contract.getCccdBackImage().isEmpty()) {
                showZoomImgDialog(1); // Index 1 cho ảnh mặt sau
            } else {
                Toast.makeText(this, "Không có ảnh CCCD mặt sau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayContractInfo() {
        if (contract == null) return;

        // Thông tin phòng
        roomTitleTv.setText(contract.getRoomTitle());
        roomAddressTv.setText(contract.getRoomAddress());
        
        // Format giá phòng
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
        String formattedPrice = formatter.format(contract.getRoomPrice()) + " VNĐ/tháng";
        roomPriceTv.setText(formattedPrice);

        // Thông tin chủ trọ
        landlordNameTv.setText(contract.getLandlordName());
        landlordPhoneTv.setText(contract.getLandlordPhone());

        // Thông tin người thuê
        tenantNameTv.setText(contract.getTenantName());
        tenantPhoneTv.setText(contract.getTenantPhone());
        tenantCCCDTv.setText(contract.getTenantCCCD());

        // Thời gian hợp đồng
        contractPeriodTv.setText(contract.getStartDate() + " - " + contract.getEndDate());

        // Trạng thái
        String statusText = getStatusText(contract.getStatus());
        statusTv.setText(statusText);
        statusTv.setTextColor(getStatusColor(contract.getStatus()));

        // Ngày tạo
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String createdDate = sdf.format(new Date(contract.getCreatedAt()));
        createdAtTv.setText("Tạo lúc: " + createdDate);

        // Load ảnh CCCD
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

    // ✅ Sử dụng ZoomImageAdapter giống DetailRoomActivity
    private void showZoomImgDialog(int currentPosition) {
        dialogZoomImg = new Dialog(ContractDetailActivity.this);
        dialogZoomImg.setContentView(R.layout.dialog_zoom_img);

        ViewPager2 viewPager = dialogZoomImg.findViewById(R.id.viewPagerZoom);
        ImageView imageViewBack = dialogZoomImg.findViewById(R.id.imageViewBack);

        // ✅ Tạo list ảnh CCCD để zoom
        ArrayList<String> cccdImages = new ArrayList<>();
        
        // Thêm ảnh mặt trước nếu có
        if (contract.getCccdFrontImage() != null && !contract.getCccdFrontImage().isEmpty()) {
            cccdImages.add(contract.getCccdFrontImage());
        }
        
        // Thêm ảnh mặt sau nếu có
        if (contract.getCccdBackImage() != null && !contract.getCccdBackImage().isEmpty()) {
            cccdImages.add(contract.getCccdBackImage());
        }

        if (!cccdImages.isEmpty()) {
            // ✅ Sử dụng ZoomImageAdapter
            ZoomImageAdapter adapter = new ZoomImageAdapter(this, cccdImages);
            viewPager.setAdapter(adapter);
            
            // ✅ Adjust position nếu chỉ có 1 ảnh
            int adjustedPosition = currentPosition;
            if (cccdImages.size() == 1) {
                adjustedPosition = 0;
            } else if (currentPosition >= cccdImages.size()) {
                adjustedPosition = cccdImages.size() - 1;
            }
            
            viewPager.setCurrentItem(adjustedPosition, false);
        }

        // ✅ Close button
        imageViewBack.setOnClickListener(v -> {
            if (dialogZoomImg.isShowing()) {
                dialogZoomImg.dismiss();
            }
        });

        // ✅ Show dialog với fullscreen style giống DetailRoomActivity
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
            case 3: return "Đã chấm dứt";
            default: return "Không xác định";
        }
    }

    private int getStatusColor(int status) {
        switch (status) {
            case 0: return getResources().getColor(R.color.gray);
            case 1: return getResources().getColor(R.color.green);
            case 2: return getResources().getColor(R.color.red);
            case 3: return getResources().getColor(R.color.orange_100);
            default: return getResources().getColor(R.color.black);
        }
    }
}