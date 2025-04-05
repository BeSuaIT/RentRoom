package com.example.timphongtro.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Activities.BillActivity;
import com.example.timphongtro.Models.Bill;
import com.example.timphongtro.Models.BillItem;
import com.example.timphongtro.Models.Service;
import com.example.timphongtro.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BillAdapter extends RecyclerView.Adapter<BillAdapter.BillViewHolder> {
    private List<Bill> bills;
    private Context context;

    public BillAdapter(Context context, BillActivity billActivity) {
        this.context = context;
        this.bills = new ArrayList<>();
    }

    @NonNull
    @Override
    public BillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_bill, parent, false);
        return new BillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BillViewHolder holder, int position) {
        Bill bill = bills.get(position);

        holder.tvOrderId.setText(bill.getId());
        holder.tvDateTime.setText(formatDate(bill.getOrderDate()));
        holder.tvTotalAmount.setText(String.format("%,d đ", bill.getTotalAmount()));
        String fullAddress = bill.getDetailAddress() + ", " +
                bill.getDistrict() + ", " +
                bill.getCity();
        holder.tvLocation.setText(fullAddress);

        // Cấu hình trạng thái với màu tương ứng
        switch (bill.getStatus()) {
            case 0:
                holder.tvStatus.setText("Đang xử lý");
                holder.tvStatus.setBackgroundResource(R.drawable.status_pending);
                holder.tvStatus.setTextColor(context.getColor(R.color.orange));
                break;
            case 1:
                holder.tvStatus.setText("Hoàn thành");
                holder.tvStatus.setBackgroundResource(R.drawable.status_completed);
                holder.tvStatus.setTextColor(context.getColor(R.color.green));
                break;
            case 2:
                holder.tvStatus.setText("Đã hủy");
                holder.tvStatus.setBackgroundResource(R.drawable.status_canceled);
                holder.tvStatus.setTextColor(context.getColor(R.color.red));
                break;
            default:
                holder.tvStatus.setText("Không xác định");
                holder.tvStatus.setBackgroundResource(R.drawable.status_background);
                holder.tvStatus.setTextColor(context.getColor(R.color.gray));
        }
        holder.itemView.setOnClickListener(v -> showBillDetail(bill));
    }

    private void showBillDetail(Bill bill) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_bill_detail, null);

        TextView tvBillId = view.findViewById(R.id.tv_billId);
        TextView tvBillDate = view.findViewById(R.id.tv_billDate);
        TextView tvBillAddress = view.findViewById(R.id.tv_billAddress);
        TextView tvBillTotal = view.findViewById(R.id.tv_billTotal);
        RecyclerView rcvBillItems = view.findViewById(R.id.rcv_billItems);

        tvBillId.setText(bill.getId());
        tvBillDate.setText(formatDate(bill.getOrderDate()));
        String fullAddress = bill.getDetailAddress() + ", " +
                bill.getDistrict() + ", " +
                bill.getCity();
        tvBillAddress.setText("Địa chỉ: " + fullAddress);
        tvBillTotal.setText(String.format("Tổng tiền: %,d đ", bill.getTotalAmount()));

        DatabaseReference servicesRef = FirebaseDatabase.getInstance().getReference("Services");
        servicesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<BillItem> items = new ArrayList<>();

                for (Map.Entry<String, Integer> entry : bill.getItems().entrySet()) {
                    String serviceId = entry.getKey();
                    Integer quantity = entry.getValue();

                    for (DataSnapshot categorySnap : snapshot.getChildren()) {
                        DataSnapshot serviceSnap = categorySnap.child(serviceId);
                        if (serviceSnap.exists()) {
                            Service service = serviceSnap.getValue(Service.class);
                            if (service != null) {
                                items.add(new BillItem(service.getTitle(),
                                        quantity, service.getPrice()));
                            }
                            break;
                        }
                    }
                }

                BillItemAdapter adapter = new BillItemAdapter(items);
                rcvBillItems.setLayoutManager(new LinearLayoutManager(context));
                rcvBillItems.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context,
                        "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setView(view)
                .setPositiveButton("Đóng", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return bills.size();
    }

    public void updateData(List<Bill> newBills) {
        this.bills = newBills;
        notifyDataSetChanged();
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    static class BillViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvDateTime, tvTotalAmount, tvStatus, tvLocation;

        public BillViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tv_orderId);
            tvDateTime = itemView.findViewById(R.id.tv_dateTime);
            tvTotalAmount = itemView.findViewById(R.id.tv_totalAmount);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvLocation = itemView.findViewById(R.id.tv_location);
        }
    }
}
