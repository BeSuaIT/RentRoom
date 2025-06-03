package com.example.timphongtro.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Activities.ContractDetailActivity;
import com.example.timphongtro.Models.Contract;
import com.example.timphongtro.R;

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

        holder.roomTitleTv.setText(contract.getRoomTitle());
        holder.tenantNameTv.setText("Người thuê: " + contract.getTenantName());
        holder.tenantPhoneTv.setText("SĐT: " + contract.getTenantPhone());
        holder.contractPeriodTv.setText(contract.getStartDate() + " - " + contract.getEndDate());

        // Format giá phòng
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
        String formattedPrice = formatter.format(contract.getRoomPrice()) + " VNĐ/tháng";
        holder.roomPriceTv.setText(formattedPrice);

        // Hiển thị trạng thái
        String statusText = getStatusText(contract.getStatus());
        holder.statusTv.setText(statusText);
        holder.statusTv.setTextColor(getStatusColor(contract.getStatus()));

        // Format ngày tạo
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String createdDate = sdf.format(new Date(contract.getCreatedAt()));
        holder.createdAtTv.setText("Tạo: " + createdDate);
        
        // ✅ Set click listener để mở ContractDetailActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ContractDetailActivity.class);
            intent.putExtra("contract", contract);
            intent.putExtra("position", position);
            context.startActivity(intent);
        });
        
        // ✅ Visual feedback khi click
        holder.itemView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.setAlpha(0.7f);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.setAlpha(1.0f);
                    break;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return contractsList.size();
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
            case 0: return context.getResources().getColor(R.color.gray);
            case 1: return context.getResources().getColor(R.color.green);
            case 2: return context.getResources().getColor(R.color.red);
            case 3: return context.getResources().getColor(R.color.orange_100);
            default: return context.getResources().getColor(R.color.black);
        }
    }
    
    // ✅ Method để update data
    public void updateData(ArrayList<Contract> newContracts) {
        this.contractsList = newContracts;
        notifyDataSetChanged();
    }

    static class ContractViewHolder extends RecyclerView.ViewHolder {
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
    }
}