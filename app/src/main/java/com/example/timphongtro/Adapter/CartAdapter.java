package com.example.timphongtro.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.timphongtro.Entity.Cart;
import com.example.timphongtro.Entity.Service;
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
import java.util.List;
import java.util.Map;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private ArrayList<Cart> cartList;
    private Context context;
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private FirebaseUser user = firebaseAuth.getCurrentUser();
    private String userID = user.getUid();
    private DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("Carts/" + userID);

    public CartAdapter(Context context, ArrayList<Cart> cartList) {
        this.context = context;
        this.cartList = cartList;
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
        DatabaseReference servicesRef = FirebaseDatabase.getInstance().getReference("Services");

        servicesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot servicesSnapshot) {
                for (DataSnapshot categorySnapshot : servicesSnapshot.getChildren()) {
                    DataSnapshot serviceSnapshot = categorySnapshot.child(cart.getServiceId());
                    if (serviceSnapshot.exists()) {
                        Service serviceDetails = serviceSnapshot.getValue(Service.class);
                        if (serviceDetails != null) {
                            DecimalFormat decimalFormat = new DecimalFormat("#,###.###");
                            decimalFormat.setDecimalSeparatorAlwaysShown(false);

                            holder.name.setText(serviceDetails.getTitle());
                            holder.price.setText(decimalFormat.format(serviceDetails.getPrice()) + " VNĐ");
                            holder.amount.setText(String.valueOf(cart.getAmount()));

                            // Handle images
                            if (serviceDetails.getImages() != null) {
                                Object imagesObj = serviceDetails.getImages();
                                String imageUrl = "";

                                if (imagesObj instanceof Map) {
                                    Map<String, String> imagesMap = (Map<String, String>) imagesObj;
                                    if (!imagesMap.isEmpty()) {
                                        imageUrl = imagesMap.values().iterator().next();
                                    }
                                } else if (imagesObj instanceof List) {
                                    List<String> imagesList = (List<String>) imagesObj;
                                    if (!imagesList.isEmpty()) {
                                        imageUrl = imagesList.get(0);
                                    }
                                }

                                if (!imageUrl.isEmpty()) {
                                    Glide.with(context).load(imageUrl).centerCrop().into(holder.image);
                                }
                            }

                            holder.minus.setOnClickListener(v -> updateAmount(cart, -1));
                            holder.plus.setOnClickListener(v -> updateAmount(cart, 1));
                            holder.btn_remove.setOnClickListener(v -> removeItem(cart.getServiceId()));
                            break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void updateAmount(Cart service, int change) {
        int newAmount = service.getAmount() + change;
        if (newAmount > 0) {
            cartRef.child(service.getServiceId()).setValue(newAmount);
        } else {
            removeItem(service.getServiceId());
        }
    }

    private void removeItem(String serviceId) {
        cartRef.child(serviceId).removeValue();
    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {

        TextView name, price, plus, minus;
        ImageView image;
        Button btn_remove;
        EditText amount;

        public CartViewHolder(View itemview) {
            super(itemview);
            plus = itemview.findViewById(R.id.textView_plus);
            minus = itemview.findViewById(R.id.textView_minus);
            name = itemview.findViewById(R.id.Cartname);
            price = itemview.findViewById(R.id.Price);
            image = itemview.findViewById(R.id.Cartimage);
            btn_remove = itemview.findViewById(R.id.button_remove);
            amount = itemview.findViewById(R.id.editText_amount);
        }
    }
}
