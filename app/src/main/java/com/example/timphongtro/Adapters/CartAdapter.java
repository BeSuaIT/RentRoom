package com.example.timphongtro.Adapters;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.timphongtro.Activities.ServiceDetailActivity;
import com.example.timphongtro.Activities.UserActivity;
import com.example.timphongtro.Models.Cart;
import com.example.timphongtro.Models.Service;
import com.example.timphongtro.Models.User;
import com.example.timphongtro.R;
import com.example.timphongtro.Utils.AuthUtils;
import com.example.timphongtro.Utils.GsonUtils;
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
    private Context context;
    private Map<String, List<Cart>> sellerItemsMap;
    private List<String> sellerIds;
    private DatabaseReference cartRef;
    private DecimalFormat decimalFormat;

    public CartAdapter(Context context, ArrayList<Cart> cartList) {
        this.context = context;
        this.decimalFormat = new DecimalFormat("#,###.###");
        this.decimalFormat.setDecimalSeparatorAlwaysShown(false);
        this.sellerItemsMap = new HashMap<>();
        this.sellerIds = new ArrayList<>();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            this.cartRef = FirebaseDatabase.getInstance()
                    .getReference("Carts")
                    .child(user.getUid());
        }

        groupItemsBySeller(cartList);
    }

    public void updateData(List<Cart> newCartList) {
        sellerItemsMap.clear();
        sellerIds.clear();
        groupItemsBySeller(newCartList);
        notifyDataSetChanged();
    }

    private void groupItemsBySeller(List<Cart> cartList) {
        if (cartList == null) return;
        
        for (Cart item : cartList) {
            if (item != null && item.getId_seller() != null) {
                String sellerId = item.getId_seller();
                if (!sellerIds.contains(sellerId)) {
                    sellerIds.add(sellerId);
                }
                sellerItemsMap.computeIfAbsent(sellerId, k -> new ArrayList<>()).add(item);
            }
        }
    }

    private void updateOrRemoveItem(Cart cart, int delta) {
        if (cart == null || cartRef == null) return;
        
        String sellerId = cart.getId_seller();
        String serviceId = cart.getServiceId();

        if (TextUtils.isEmpty(sellerId) || TextUtils.isEmpty(serviceId)) {
            Toast.makeText(context, "Lỗi: Thông tin sản phẩm không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

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
                        Toast.makeText(context, "Đã xóa sản phẩm khỏi giỏ hàng", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Lỗi xóa sản phẩm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Lỗi cập nhật số lượng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

        if (TextUtils.isEmpty(sellerId) || sellerItems == null) return;

        loadSellerInfo(holder, sellerId);

        CartItemsAdapter itemsAdapter = new CartItemsAdapter(sellerItems);
        holder.itemsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        holder.itemsRecyclerView.setAdapter(itemsAdapter);
    }

    private void loadSellerInfo(SellerViewHolder holder, String sellerId) {
        
        DatabaseReference sellerRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(sellerId);
                
        sellerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User seller = snapshot.getValue(User.class);
                if (seller != null) {
                    String sellerName = seller.getName() != null ? seller.getName() : "Người bán không xác định";
                    holder.sellerName.setText("Người bán: " + sellerName);
                    holder.sellerName.setOnClickListener(v -> navigateToSellerProfile(seller));
                } else {
                    holder.sellerName.setText("Người bán: Không xác định");
                    holder.sellerName.setOnClickListener(null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.sellerName.setText("Người bán: Lỗi tải thông tin");
                holder.sellerName.setOnClickListener(null);
            }
        });
    }

    private void navigateToSellerProfile(User seller) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            AuthUtils.showLoginRequiredDialog(context, "xem thông tin người bán", "có thể");
            return;
        }

        if (seller == null) {
            Toast.makeText(context, "Lỗi: Không có thông tin người bán", Toast.LENGTH_SHORT).show();
            return;
        }

        String sellerJson = GsonUtils.toJson(seller);
        if (sellerJson != null) {
            Intent intent = new Intent(context, UserActivity.class);
            intent.putExtra("userData", sellerJson);
            intent.putExtra("userType", "seller"); // Đánh dấu là người bán
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "Lỗi mở thông tin người bán", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return sellerIds != null ? sellerIds.size() : 0;
    }

    public static class SellerViewHolder extends RecyclerView.ViewHolder {
        TextView sellerName;
        RecyclerView itemsRecyclerView;

        SellerViewHolder(@NonNull View itemView) {
            super(itemView);
            sellerName = itemView.findViewById(R.id.textView_sellerName);
            itemsRecyclerView = itemView.findViewById(R.id.recyclerView_sellerItems);
        }
    }

    private class CartItemsAdapter extends RecyclerView.Adapter<CartItemsAdapter.ItemViewHolder> {
        private List<Cart> items;

        CartItemsAdapter(List<Cart> items) {
            this.items = items != null ? items : new ArrayList<>();
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.view_holder_cart, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            if (position >= 0 && position < items.size()) {
                Cart cart = items.get(position);
                if (cart != null) {
                    holder.bindData(cart);
                }
            }
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            ImageView cartItemImage;
            TextView cartItemName, itemPrice, quantity;
            TextView increaseButton, decreaseButton;
            Button removeButton;

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
                itemView.setOnClickListener(v -> navigateToServiceDetail(cart));
            }

            private void navigateToServiceDetail(Cart cart) {
                if (cart == null) {
                    Toast.makeText(context, "Lỗi: Không có thông tin sản phẩm", Toast.LENGTH_SHORT).show();
                    return;
                }

                Service service = createServiceFromCart(cart);
                if (service == null) {
                    Toast.makeText(context, "Lỗi: Không thể tải thông tin sản phẩm", Toast.LENGTH_SHORT).show();
                    return;
                }

                String serviceJson = GsonUtils.toJson(service);
                if (serviceJson != null) {
                    Intent intent = new Intent(context, ServiceDetailActivity.class);
                    intent.putExtra("ServiceData", serviceJson);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "Lỗi mở chi tiết sản phẩm", Toast.LENGTH_SHORT).show();
                }
            }

            private Service createServiceFromCart(Cart cart) {
                if (cart == null) return null;
                
                try {
                    Service service = new Service();
                    service.setServiceId(cart.getServiceId());
                    service.setTitle(cart.getTitle());
                    service.setPrice(cart.getPrice());
                    service.setId_seller(cart.getId_seller());
                    service.setImages(cart.getImages());
                    service.setAmount(cart.getAmount());
                    service.setDescription("Chi tiết sản phẩm sẽ được tải từ cơ sở dữ liệu");
                    service.setSold(0);
                    service.setCreateAt("");
                    service.setId_own_post("");
                    
                    return service;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            void bindData(Cart cart) {
                if (cart == null) return;
                cartItemName.setText(cart.getTitle() != null ? cart.getTitle() : "Sản phẩm");
                itemPrice.setText(String.format("%s VNĐ", decimalFormat.format(cart.getPrice())));
                quantity.setText(String.valueOf(cart.getAmount()));

                if (cart.getImages() != null && !cart.getImages().isEmpty()) {
                    String firstImage = cart.getImages().get(0);
                    if (!TextUtils.isEmpty(firstImage)) {
                        Glide.with(context)
                                .load(firstImage)
                                .placeholder(R.drawable.loading)
                                .error(R.drawable.img_no_image)
                                .centerCrop()
                                .into(cartItemImage);
                    } else {
                        cartItemImage.setImageResource(R.drawable.img_no_image);
                    }
                } else {
                    cartItemImage.setImageResource(R.drawable.img_no_image);
                }

                setupClickListeners(cart);
            }
        }
    }
}
