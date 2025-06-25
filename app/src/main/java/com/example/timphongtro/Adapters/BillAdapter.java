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

import com.example.timphongtro.Models.Bill;
import com.example.timphongtro.Models.BillItem;
import com.example.timphongtro.Models.Service;
import com.example.timphongtro.R;
import com.google.android.material.button.MaterialButton;
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

    public BillAdapter(Context context) {
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
        holder.tvTotalAmount.setText(String.format("%,d VNĐ", bill.getTotalAmount()));
        String fullAddress = bill.getDetailAddress() + ", " +
                bill.getDistrict() + ", " +
                bill.getCity();
        holder.tvLocation.setText(fullAddress);

        setupStatusAndCancelButton(holder, bill, position);

        holder.itemView.setOnClickListener(v -> showBillDetail(bill));
    }

    private void setupStatusAndCancelButton(BillViewHolder holder, Bill bill, int position) {
        switch (bill.getStatus()) {
            case 0:
                holder.tvStatus.setText("Đang xử lý");
                holder.tvStatus.setBackgroundResource(R.drawable.status_pending);
                holder.tvStatus.setTextColor(context.getColor(R.color.orange_100));
                holder.btnCancelOrder.setVisibility(View.VISIBLE);
                holder.btnCancelOrder.setOnClickListener(v -> showCancelConfirmDialog(bill, position));
                break;
                
            case 1:
                holder.tvStatus.setText("Hoàn thành");
                holder.tvStatus.setBackgroundResource(R.drawable.status_completed);
                holder.tvStatus.setTextColor(context.getColor(R.color.green));
                holder.btnCancelOrder.setVisibility(View.GONE);
                break;
                
            case 2:
                holder.tvStatus.setText("Đã hủy");
                holder.tvStatus.setBackgroundResource(R.drawable.status_canceled);
                holder.tvStatus.setTextColor(context.getColor(R.color.red));
                holder.btnCancelOrder.setVisibility(View.GONE);
                break;
                
            default:
                holder.tvStatus.setText("Không xác định");
                holder.tvStatus.setBackgroundResource(R.drawable.status_background);
                holder.tvStatus.setTextColor(context.getColor(R.color.gray));
                holder.btnCancelOrder.setVisibility(View.GONE);
        }
    }

    private void showCancelConfirmDialog(Bill bill, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này không?\n\nMã đơn: " + bill.getId())
                .setPositiveButton("Hủy đơn", (dialog, which) -> {
                    cancelOrder(bill, position);
                })
                .setNegativeButton("Không", null)
                .show();
    }

    private void cancelOrder(Bill bill, int position) {
        if (bill == null || bill.getId() == null) {
            Toast.makeText(context, "Lỗi: Thông tin đơn hàng không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference billRef = FirebaseDatabase.getInstance()
                .getReference("Bills")
                .child(bill.getId());

        billRef.child("status").setValue(2)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();

                    bill.setStatus(2);
                    notifyItemChanged(position);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Lỗi hủy đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showBillDetail(Bill bill) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_bill_detail, null);

        TextView tvBillId = view.findViewById(R.id.tv_billId);
        TextView tvBillDate = view.findViewById(R.id.tv_billDate);
        TextView tvBillAddress = view.findViewById(R.id.tv_billAddress);
        TextView tvPayMethod = view.findViewById(R.id.tv_payMethod);
        TextView tvBillTotal = view.findViewById(R.id.tv_billTotal);
        RecyclerView rcvBillItems = view.findViewById(R.id.rcv_billItems);

        tvBillId.setText(bill.getId());
        tvBillDate.setText(formatDate(bill.getOrderDate()));
        String fullAddress = bill.getDetailAddress() + ", " +
                bill.getDistrict() + ", " +
                bill.getCity();
        tvBillAddress.setText("Địa chỉ: " + fullAddress);
        tvPayMethod.setText("Phương thức thanh toán: " + bill.getPaymentMethod());
        tvBillTotal.setText(String.format("Tổng tiền: %,d VNĐ", bill.getTotalAmount()));

        DatabaseReference servicesRef = FirebaseDatabase.getInstance().getReference("Services");
        servicesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<BillItem> items = new ArrayList<>();
                int totalItems = bill.getItems().size();
                int[] processedItems = {0}; // Counter để track progress

                for (Map.Entry<String, Integer> entry : bill.getItems().entrySet()) {
                    String serviceId = entry.getKey();
                    Integer quantity = entry.getValue();

                    if (serviceId == null || quantity == null) {
                        processedItems[0]++;
                        continue;
                    }

                    DataSnapshot serviceSnap = snapshot.child(serviceId);
                    if (serviceSnap.exists()) {
                        Service service = serviceSnap.getValue(Service.class);
                        if (service != null) {
                            items.add(new BillItem(
                                service.getTitle(),
                                quantity, 
                                service.getPrice()
                            ));
                        }
                    } else {
                        items.add(new BillItem(
                            "Sản phẩm đã bị xóa (ID: " + serviceId + ")",
                            quantity,
                            0 // Giá 0 vì không tìm thấy
                        ));
                    }

                    processedItems[0]++;

                    if (processedItems[0] == totalItems) {
                        setupBillItemsRecyclerView(rcvBillItems, items);
                    }
                }

                if (totalItems == 0) {
                    setupBillItemsRecyclerView(rcvBillItems, items);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                List<BillItem> fallbackItems = createFallbackBillItems(bill);
                setupBillItemsRecyclerView(rcvBillItems, fallbackItems);
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

    public static class BillViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvDateTime, tvTotalAmount, tvStatus, tvLocation;
        MaterialButton btnCancelOrder;

        public BillViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tv_orderId);
            tvDateTime = itemView.findViewById(R.id.tv_dateTime);
            tvTotalAmount = itemView.findViewById(R.id.tv_totalAmount);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvLocation = itemView.findViewById(R.id.tv_location);
            btnCancelOrder = itemView.findViewById(R.id.btn_cancel_order);
        }
    }

    private void setupBillItemsRecyclerView(RecyclerView rcvBillItems, List<BillItem> items) {
        BillItemAdapter adapter = new BillItemAdapter(items);
        rcvBillItems.setLayoutManager(new LinearLayoutManager(context));
        rcvBillItems.setAdapter(adapter);
    }

    // Fallback method khi không load được services
    private List<BillItem> createFallbackBillItems(Bill bill) {
        List<BillItem> fallbackItems = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : bill.getItems().entrySet()) {
            String serviceId = entry.getKey();
            Integer quantity = entry.getValue();
            
            if (serviceId != null && quantity != null) {
                fallbackItems.add(new BillItem(
                    "Sản phẩm (ID: " + serviceId + ")",
                    quantity,
                    0 // Không biết giá
                ));
            }
        }
        
        return fallbackItems;
    }
}
