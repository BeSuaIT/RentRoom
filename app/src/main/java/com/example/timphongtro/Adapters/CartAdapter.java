package com.example.timphongtro.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.timphongtro.Models.Cart;
import com.example.timphongtro.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.SellerViewHolder> {
    private final Context context;
    private final Map<String, List<Cart>> sellerItemsMap;
    private final List<String> sellerIds;
    private final DatabaseReference cartRef;
    private final DecimalFormat decimalFormat;

    public CartAdapter(Context context, ArrayList<Cart> cartList) {
        this.context = context;
        this.decimalFormat = new DecimalFormat("#,###.###");
        this.decimalFormat.setDecimalSeparatorAlwaysShown(false);
        this.sellerItemsMap = new HashMap<>();
        this.sellerIds = new ArrayList<>();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.cartRef = FirebaseDatabase.getInstance()
                .getReference("Carts")
                .child(user.getUid());

        groupItemsBySeller(cartList);
    }

    public void updateData(List<Cart> newCartList) {
        sellerItemsMap.clear();
        sellerIds.clear();
        groupItemsBySeller(newCartList);
        notifyDataSetChanged();
    }

    private void groupItemsBySeller(List<Cart> cartList) {
        for (Cart item : cartList) {
            if (item.getId_seller() != null) {
                String sellerId = item.getId_seller();
                if (!sellerIds.contains(sellerId)) {
                    sellerIds.add(sellerId);
                }
                sellerItemsMap.computeIfAbsent(sellerId, k -> new ArrayList<>()).add(item);
            }
        }
    }

    private void updateOrRemoveItem(Cart cart, int delta) {
        String sellerId = cart.getId_seller();
        String serviceId = cart.getServiceId();

        if (delta == 0) { // Remove item
            cartRef.child(sellerId)
                    .child(serviceId)
                    .removeValue()
                    .addOnSuccessListener(aVoid -> {
                        List<Cart> sellerItems = sellerItemsMap.get(sellerId);
                        if (sellerItems != null) {
                            sellerItems.remove(cart);
                            if (sellerItems.isEmpty()) {
                                sellerItemsMap.remove(sellerId);
                                sellerIds.remove(sellerId);
                            }
                        }
                        notifyDataSetChanged();
                    });
        } else { // Update quantity
            int newAmount = cart.getAmount() + delta;
            if (newAmount > 0) {
                cartRef.child(sellerId)
                        .child(serviceId)
                        .setValue(newAmount)
                        .addOnSuccessListener(aVoid -> {
                            cart.setAmount(newAmount);
                            notifyDataSetChanged();
                        });
            } else {
                updateOrRemoveItem(cart, 0);
            }
        }
    }

    @NonNull
    @Override
    public SellerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.seller_cart_group, parent, false);
        return new SellerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SellerViewHolder holder, int position) {
        String sellerId = sellerIds.get(position);
        List<Cart> sellerItems = sellerItemsMap.get(sellerId);

        DatabaseReference sellerRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(sellerId);
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

        CartItemsAdapter itemsAdapter = new CartItemsAdapter(sellerItems);
        holder.itemsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        holder.itemsRecyclerView.setAdapter(itemsAdapter);
    }

    @Override
    public int getItemCount() {
        return sellerIds.size();
    }

    private void removeItem(String sellerId, String serviceId) {
        cartRef.child(sellerId).child(serviceId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Item removed successfully
                })
                .addOnFailureListener(e -> {
                    // Handle error
                });
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

    private class CartItemsAdapter extends RecyclerView.Adapter<CartItemsAdapter.ItemViewHolder> {
        private final List<Cart> items;

        CartItemsAdapter(List<Cart> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.cart_view_holder, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            Cart cart = items.get(position);
            holder.bindData(cart);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            final ImageView cartItemImage;
            final TextView cartItemName, itemPrice, quantity;
            final TextView increaseButton, decreaseButton;
            final Button removeButton;

            ItemViewHolder(@NonNull View itemView) {
                super(itemView);
                cartItemImage = itemView.findViewById(R.id.cartItemImage);
                cartItemName = itemView.findViewById(R.id.cartItemName);
                itemPrice = itemView.findViewById(R.id.itemPrice);
                quantity = itemView.findViewById(R.id.quantity);
                increaseButton = itemView.findViewById(R.id.increaseButton);
                decreaseButton = itemView.findViewById(R.id.decreaseButton);
                removeButton = itemView.findViewById(R.id.removeButton);
            }

            private void setupClickListeners(Cart cart) {
                decreaseButton.setOnClickListener(v -> updateOrRemoveItem(cart, -1));
                increaseButton.setOnClickListener(v -> updateOrRemoveItem(cart, 1));
                removeButton.setOnClickListener(v -> updateOrRemoveItem(cart, 0));
            }

            void bindData(Cart cart) {
                cartItemName.setText(cart.getTitle());
                itemPrice.setText(String.format("%s VNĐ", decimalFormat.format(cart.getPrice())));
                quantity.setText(String.valueOf(cart.getAmount()));

                // Load image
                if (cart.getImages() != null && !cart.getImages().isEmpty()) {
                    Glide.with(context)
                            .load(cart.getImages().get(0))
                            .placeholder(R.drawable.image1)
                            .error(R.drawable.image1)
                            .centerCrop()
                            .into(cartItemImage);
                }

                setupClickListeners(cart);
            }
        }
    }
}
