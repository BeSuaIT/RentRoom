package com.example.timphongtro.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Models.BillItem;
import com.example.timphongtro.R;

import java.util.List;

public class BillItemAdapter extends RecyclerView.Adapter<BillItemAdapter.ViewHolder> {
    private List<BillItem> items;

    public BillItemAdapter(List<BillItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bill_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BillItem item = items.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvQuantity.setText("x" + item.getQuantity());
        holder.tvPrice.setText(String.format("%,d VNĐ", item.getTotal()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvQuantity, tvPrice;

        ViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tv_title);
            tvQuantity = view.findViewById(R.id.tv_quantity);
            tvPrice = view.findViewById(R.id.tv_price);
        }
    }
}
