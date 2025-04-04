package com.example.timphongtro.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private ArrayList<Cart> cartList;
    private Context context;
    private DatabaseReference cartRef;

    public CartAdapter(Context context, ArrayList<Cart> cartList) {
        this.context = context;
        this.cartList = cartList;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            this.cartRef = FirebaseDatabase.getInstance()
                    .getReference("Carts")
                    .child(user.getUid());
        }
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.cart_view_holder, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        Cart cart = cartList.get(position);
        DecimalFormat decimalFormat = new DecimalFormat("#,###.###");
        decimalFormat.setDecimalSeparatorAlwaysShown(false);

        // Bind data directly from Cart object
        holder.cartItemName.setText(cart.getTitle());
        holder.itemPrice.setText(decimalFormat.format(cart.getPrice()) + " VNĐ");
        holder.quantity.setText(String.valueOf(cart.getAmount()));

        DatabaseReference sellerRef = FirebaseDatabase.getInstance().getReference("Users/" + cart.getId_seller());
        sellerRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String sellerName = snapshot.getValue(String.class);
                    holder.cartItemSeller.setText(sellerName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.cartItemSeller.setText("Unknown Seller");
            }
        });

        if (cart.getImages() != null) {
            Glide.with(context)
                    .load(cart.getImages().get(0))
                    .centerCrop()
                    .into(holder.cartItemImage);
        }

        holder.decreaseButton.setOnClickListener(v -> {
            int newAmount = cart.getAmount() - 1;
            if (newAmount > 0) {
                cart.setAmount(newAmount);
                holder.quantity.setText(String.valueOf(newAmount));
                cartRef.child(cart.getServiceId()).setValue(newAmount);
            } else {
                removeItem(cart.getServiceId());
            }
        });

        holder.increaseButton.setOnClickListener(v -> {
            int newAmount = cart.getAmount() + 1;
            cart.setAmount(newAmount);
            holder.quantity.setText(String.valueOf(newAmount));
            cartRef.child(cart.getServiceId()).setValue(newAmount);
        });

        holder.removeButton.setOnClickListener(v -> removeItem(cart.getServiceId()));
    }

    private void removeItem(String serviceId) {
        cartRef.child(serviceId).removeValue();
    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {

        TextView cartItemName, itemPrice, increaseButton, decreaseButton, quantity, cartItemSeller;
        ImageView cartItemImage;
        Button removeButton;

        public CartViewHolder(View itemview) {
            super(itemview);
            increaseButton = itemview.findViewById(R.id.increaseButton);
            decreaseButton = itemview.findViewById(R.id.decreaseButton);
            cartItemName = itemview.findViewById(R.id.cartItemName);
            cartItemSeller = itemview.findViewById(R.id.cartItemSeller);
            itemPrice = itemview.findViewById(R.id.itemPrice);
            cartItemImage = itemview.findViewById(R.id.cartItemImage);
            removeButton = itemview.findViewById(R.id.removeButton);
            quantity = itemview.findViewById(R.id.quantity);
        }
    }
}
