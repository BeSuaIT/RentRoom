package com.example.timphongtro.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.timphongtro.R;

public class PaymentNotificationActivity extends AppCompatActivity {
    private TextView tvNotify;
    private ImageView ivStatus;
    private String SUCCESS_ZALOPAY = "Thanh toán ZaloPay thành công\nCảm ơn bạn đã sử dụng dịch vụ!";
    private String SUCCESS_COD = "Đặt hàng thành công\nBạn sẽ thanh toán khi nhận hàng";
    private String FAILED_PAYMENT = "Thanh toán không thành công\nVui lòng thử lại sau";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_notification);

        initViews();
        showStatus();
    }

    private void initViews() {
        tvNotify = findViewById(R.id.textViewNotify);
        ivStatus = findViewById(R.id.imageViewStatus);

        findViewById(R.id.buttonHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        findViewById(R.id.buttonDetails).setOnClickListener(v -> {
            Intent intent = new Intent(this, BillActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void showStatus() {
        String paymentMethod = getIntent().getStringExtra("payment_method");
        boolean isSuccess = getIntent().getBooleanExtra("is_success", false);

        if (isSuccess) {
            ivStatus.setImageResource(R.drawable.img_success);
            if ("zalopay".equals(paymentMethod)) {
                tvNotify.setText(SUCCESS_ZALOPAY);
            } else if ("cod".equals(paymentMethod)) {
                tvNotify.setText(SUCCESS_COD);
            }
        } else {
            ivStatus.setImageResource(R.drawable.img_cancel);
            tvNotify.setText(FAILED_PAYMENT);
        }
    }
}