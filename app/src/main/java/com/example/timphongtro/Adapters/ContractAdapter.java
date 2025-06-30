package com.example.timphongtro.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Activities.ContractDetailActivity;
import com.example.timphongtro.Models.Contract;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.R;
import com.example.timphongtro.Utils.ContractUtils;
import com.example.timphongtro.Utils.GsonUtils;
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

public class ContractAdapter extends RecyclerView.Adapter<ContractAdapter.ContractViewHolder> {

    private Context context;
    private ArrayList<Contract> contractsList;

    public ContractAdapter(Context context, ArrayList<Contract> contractsList) {
        this.context = context;
        this.contractsList = contractsList;
    }

    @NonNull
    @Override
    public ContractViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_holder_contract, parent, false);
        return new ContractViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContractViewHolder holder, int position) {
        Contract contract = contractsList.get(position);
        holder.bind(contract, position);
    }

    @Override
    public int getItemCount() {
        return contractsList.size();
    }

    class ContractViewHolder extends RecyclerView.ViewHolder {
        TextView roomTitleTv, tenantNameTv, tenantPhoneTv, contractPeriodTv,
                roomPriceTv, statusTv, createdAtTv;

        public ContractViewHolder(@NonNull View itemView) {
            super(itemView);
            roomTitleTv = itemView.findViewById(R.id.room_title_tv);
            tenantNameTv = itemView.findViewById(R.id.tenant_name_tv);
            tenantPhoneTv = itemView.findViewById(R.id.tenant_phone_tv);
            contractPeriodTv = itemView.findViewById(R.id.contract_period_tv);
            roomPriceTv = itemView.findViewById(R.id.room_price_tv);
            statusTv = itemView.findViewById(R.id.status_tv);
            createdAtTv = itemView.findViewById(R.id.created_at_tv);
        }

        public void bind(Contract contract, int position) {
            loadRoomData(contract);
            bindContractData(contract);
            setupClickListener(contract, position);
        }

        private void loadRoomData(Contract contract) {
            if (contract.getRoomId() == null) {
                roomTitleTv.setText("Phòng không xác định");
                roomPriceTv.setText("0 VNĐ/tháng");
                return;
            }

            DatabaseReference roomRef = FirebaseDatabase.getInstance()
                    .getReference("Rooms")
                    .child(contract.getRoomId());

            roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Room room = snapshot.getValue(Room.class);
                        if (room != null) {
                            roomTitleTv.setText(room.getTitle_room() != null ? 
                                              room.getTitle_room() : "Phòng trọ");
                            
                            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
                            String formattedPrice = formatter.format(room.getPrice_room()) + " VNĐ/tháng";
                            roomPriceTv.setText(formattedPrice);
                        }
                    } else {
                        roomTitleTv.setText("Phòng đã bị xóa");
                        roomPriceTv.setText("N/A");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    roomTitleTv.setText("Lỗi tải phòng");
                    roomPriceTv.setText("N/A");
                }
            });
        }

        private void bindContractData(Contract contract) {
            tenantNameTv.setText("Người thuê: " + contract.getTenantName());
            tenantPhoneTv.setText("SĐT: " + contract.getTenantPhone());

            String startDate = formatTimestamp(contract.getStartDate());
            String endDate = formatTimestamp(contract.getEndDate());
            contractPeriodTv.setText(startDate + " - " + endDate);

            ContractUtils.syncContractStatusWithDatabase(contract);

            int currentStatus = ContractUtils.getCurrentStatus(contract);
            String statusText = getStatusText(currentStatus);
            statusTv.setText(statusText);
            statusTv.setTextColor(getStatusColor(currentStatus));

            String createdDate = formatTimestampWithTime(contract.getCreatedAt());
            createdAtTv.setText("Tạo: " + createdDate);
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

        private void setupClickListener(Contract contract, int position) {
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ContractDetailActivity.class);
                String contractJson = GsonUtils.toJson(contract);
                intent.putExtra("contractJson", contractJson);
                intent.putExtra("position", position);
                
                context.startActivity(intent);
            });
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
            case 0: return ContextCompat.getColor(context, R.color.gray);
            case 1: return ContextCompat.getColor(context, R.color.green);
            case 2: return ContextCompat.getColor(context, R.color.red);
            default: return ContextCompat.getColor(context, R.color.black);
        }
    }
}