package com.example.timphongtro.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.timphongtro.Models.Cart;
import com.example.timphongtro.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.List;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder> {
    private Context context;
    private List<Cart> orderItems;
    private DecimalFormat decimalFormat;

    public OrderItemAdapter(Context context, List<Cart> orderItems) {
        this.context = context;
        this.orderItems = orderItems;
        this.decimalFormat = new DecimalFormat("#,###.###");
        this.decimalFormat.setDecimalSeparatorAlwaysShown(false);
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.activity_order_item_view_holder, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        Cart item = orderItems.get(position);

        holder.serviceName.setText(item.getTitle());
        holder.amount.setText(String.format("Số lượng: %d x %s VNĐ",
                item.getAmount(),
                decimalFormat.format(item.getPrice())
        ));
        holder.price.setText("Thành tiền: " + decimalFormat.format(item.getPrice() * item.getAmount()) + " VNĐ");

        DatabaseReference sellerRef = FirebaseDatabase.getInstance().getReference("Users/" + item.getId_seller());
        sellerRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String sellerName = snapshot.getValue(String.class);
                    holder.seller.setText(sellerName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.seller.setText("Unknown Seller");
            }
        });

        if (item.getImages() != null && !item.getImages().isEmpty()) {
            Glide.with(context)
                    .load(item.getImages().get(0))
                    .placeholder(R.drawable.image1)
                    .error(R.drawable.image1)
                    .into(holder.serviceImage);
        }
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        ImageView serviceImage;
        TextView serviceName, amount, price, seller;

        public OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            serviceImage = itemView.findViewById(R.id.imageView_service);
            serviceName = itemView.findViewById(R.id.textView_serviceName);
            seller = itemView.findViewById(R.id.textView_seller);
            amount = itemView.findViewById(R.id.textView_amount);
            price = itemView.findViewById(R.id.textView_price);
        }
    }
}