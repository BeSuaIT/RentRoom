package com.example.timphongtro.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.timphongtro.Models.CartItem;
import com.example.timphongtro.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.SellerViewHolder> {
    private final Context context;
    private final Map<String, List<CartItem>> sellerItemsMap;
    private final List<String> sellerIds;
    private final DecimalFormat decimalFormat;

    public OrderItemAdapter(Context context, List<CartItem> orderItems) {
        this.context = context;
        this.decimalFormat = new DecimalFormat("#,###.###");
        this.decimalFormat.setDecimalSeparatorAlwaysShown(false);
        this.sellerItemsMap = new HashMap<>();
        this.sellerIds = new ArrayList<>();

        // Group by sellerId từ CartItem
        orderItems.forEach(item -> {
            String sellerId = item.getSellerId();
            sellerItemsMap.computeIfAbsent(sellerId, k -> new ArrayList<>()).add(item);
            if (!sellerIds.contains(sellerId)) {
                sellerIds.add(sellerId);
            }
        });
    }

    @NonNull
    @Override
    public SellerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.seller_items_layout, parent, false);
        return new SellerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SellerViewHolder holder, int position) {
        String sellerId = sellerIds.get(position);
        List<CartItem> sellerItems = sellerItemsMap.get(sellerId);

        DatabaseReference sellerRef = FirebaseDatabase.getInstance().getReference("Users").child(sellerId);
        sellerRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String sellerName = snapshot.exists() ? snapshot.getValue(String.class) : "Người bán không xác định";
                holder.sellerName.setText("Người bán: " + sellerName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.sellerName.setText("Người bán không xác định");
            }
        });

        SellerItemsAdapter itemsAdapter = new SellerItemsAdapter(sellerItems);
        holder.itemsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        holder.itemsRecyclerView.setAdapter(itemsAdapter);
    }

    @Override
    public int getItemCount() {
        return sellerIds.size();
    }

    private class SellerItemsAdapter extends RecyclerView.Adapter<SellerItemsAdapter.ItemViewHolder> {
        private final List<CartItem> items;

        SellerItemsAdapter(List<CartItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.view_holder_order_item, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            CartItem item = items.get(position);
            holder.bindData(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            final ImageView serviceImage;
            final TextView serviceName, amount, price;

            ItemViewHolder(@NonNull View itemView) {
                super(itemView);
                serviceImage = itemView.findViewById(R.id.imageView_service);
                serviceName = itemView.findViewById(R.id.textView_serviceName);
                amount = itemView.findViewById(R.id.textView_amount);
                price = itemView.findViewById(R.id.textView_price);
            }

            void bindData(CartItem item) {
                serviceName.setText(item.getTitle());
                amount.setText(String.format("Số lượng: %d x %s VNĐ",
                        item.getAmount(),
                        decimalFormat.format(item.getPrice())));
                price.setText(String.format("Thành tiền: %s VNĐ",
                        decimalFormat.format(item.getPrice() * item.getAmount())));

                if (item.getImages() != null && !item.getImages().isEmpty()) {
                    Glide.with(context)
                            .load(item.getImages().get(0))
                            .placeholder(R.drawable.loading)
                            .error(R.drawable.img_no_image)
                            .into(serviceImage);
                }
            }
        }
    }

    static class SellerViewHolder extends RecyclerView.ViewHolder {
        final TextView sellerName;
        final RecyclerView itemsRecyclerView;

        SellerViewHolder(@NonNull View itemView) {
            super(itemView);
            sellerName = itemView.findViewById(R.id.textView_sellerName);
            itemsRecyclerView = itemView.findViewById(R.id.recyclerView_sellerItems);
        }
    }
}