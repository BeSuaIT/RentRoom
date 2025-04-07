package com.example.timphongtro.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.timphongtro.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PaymentNotificationActivity extends AppCompatActivity {

    private TextView tvNotify, tvDate, tvTransactionId, tvAmount;
    private ImageView ivStatus;
    private Button btnHome, btnDetails;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_notification);

        initViews();
        setupListeners();
        loadOrderData();
    }

    private void initViews() {
        tvNotify = findViewById(R.id.textViewNotify);
        tvDate = findViewById(R.id.textViewDate);
        tvTransactionId = findViewById(R.id.textViewTransactionId);
        tvAmount = findViewById(R.id.textViewAmount);
        ivStatus = findViewById(R.id.imageViewStatus);
        btnHome = findViewById(R.id.buttonHome);
        btnDetails = findViewById(R.id.buttonDetails);

        // Lấy orderId từ intent trước đó
        orderId = getIntent().getStringExtra("orderId");
    }

    private void setupListeners() {
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        btnDetails.setOnClickListener(v -> {
            Intent intent = new Intent(this, BillActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadOrderData() {
        if (orderId == null) {
            showDefaultStatus();
            return;
        }

        DatabaseReference orderRef = FirebaseDatabase.getInstance()
                .getReference("Bills")
                .child(orderId);

        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String paymentMethod = snapshot.child("paymentMethod").getValue(String.class);
                    int status = snapshot.child("status").getValue(Integer.class);
                    long amount = snapshot.child("totalAmount").getValue(Long.class);
                    long orderDate = snapshot.child("orderDate").getValue(Long.class);

                    // Hiển thị trạng thái
                    if (paymentMethod.equals("zalopay") && status == 1) {
                        ivStatus.setImageResource(R.drawable.img_success);
                        tvNotify.setText("Thanh toán thành công");
                    } else if (paymentMethod.equals("cod")) {
                        ivStatus.setImageResource(R.drawable.img_pending);
                        tvNotify.setText("Đặt hàng thành công\n Chờ người bán xác nhận");
                    }

                    // Format và hiển thị ngày
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    String dateStr = sdf.format(new Date(orderDate));
                    tvDate.setText("Ngày: " + dateStr);

                    // Hiển thị mã giao dịch
                    tvTransactionId.setText("Mã đơn hàng: " + orderId);

                    // Format và hiển thị số tiền
                    DecimalFormat df = new DecimalFormat("#,###");
                    tvAmount.setText(df.format(amount) + " ₫");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PaymentNotificationActivity.this,
                        "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDefaultStatus() {
        String result = getIntent().getStringExtra("result");
        if (result != null) {
            tvNotify.setText(result);
            if (result.contains("thành công")) {
                ivStatus.setImageResource(R.drawable.img_success);
            } else {
                ivStatus.setImageResource(R.drawable.img_cancel);
            }
        }
    }
}