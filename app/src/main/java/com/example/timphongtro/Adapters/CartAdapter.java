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
import com.example.timphongtro.Models.CartItem;
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
    private Map<String, List<CartItem>> sellerItemsMap;
    private List<String> sellerIds;
    private DatabaseReference cartsRef;
    private DecimalFormat decimalFormat;

    public CartAdapter(Context context, ArrayList<CartItem> cartItemList) {
        this.context = context;
        this.decimalFormat = new DecimalFormat("#,###.###");
        this.decimalFormat.setDecimalSeparatorAlwaysShown(false);
        this.sellerItemsMap = new HashMap<>();
        this.sellerIds = new ArrayList<>();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            this.cartsRef = FirebaseDatabase.getInstance()
                    .getReference("Carts");
        }

        groupItemsBySeller(cartItemList);
    }

    public void updateData(List<CartItem> newCartItemList) {
        sellerItemsMap.clear();
        sellerIds.clear();
        groupItemsBySeller(newCartItemList);
        notifyDataSetChanged();
    }

    private void groupItemsBySeller(List<CartItem> cartItemList) {
        if (cartItemList == null) return;
        
        for (CartItem item : cartItemList) {
            if (item != null && item.getSellerId() != null) {
                String sellerId = item.getSellerId();
                if (!sellerIds.contains(sellerId)) {
                    sellerIds.add(sellerId);
                }
                sellerItemsMap.computeIfAbsent(sellerId, k -> new ArrayList<>()).add(item);
            }
        }
    }

    private void updateOrRemoveItem(CartItem cartItem, int delta) {
        if (cartItem == null || cartsRef == null) return;
        
        String cartId = cartItem.getCartId();
        String serviceId = cartItem.getServiceId();

        if (TextUtils.isEmpty(cartId) || TextUtils.isEmpty(serviceId)) {
            Toast.makeText(context, "Lỗi: Thông tin sản phẩm không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference cartRef = cartsRef.child(cartId);

        if (delta == 0) {
            cartRef.child("cartItems").child(serviceId).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        cartRef.child("cartItems").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                                    cartRef.removeValue();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                        
                        // Remove from local data
                        String sellerId = cartItem.getSellerId();
                        List<CartItem> sellerItems = sellerItemsMap.get(sellerId);
                        if (sellerItems != null) {
                            sellerItems.remove(cartItem);
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
        } else {
            int newAmount = cartItem.getAmount() + delta;
            if (newAmount > 0) {
                cartRef.child("cartItems").child(serviceId).setValue(newAmount)
                        .addOnSuccessListener(aVoid -> {
                            cartItem.setAmount(newAmount);
                            notifyDataSetChanged();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Lỗi cập nhật số lượng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                updateOrRemoveItem(cartItem, 0);
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
        List<CartItem> sellerItems = sellerItemsMap.get(sellerId);

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
        private List<CartItem> items;

        CartItemsAdapter(List<CartItem> items) {
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
                CartItem cartItem = items.get(position);
                if (cartItem != null) {
                    holder.bindData(cartItem);
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

            private void setupClickListeners(CartItem cartItem) {
                decreaseButton.setOnClickListener(v -> updateOrRemoveItem(cartItem, -1));
                increaseButton.setOnClickListener(v -> updateOrRemoveItem(cartItem, 1));
                removeButton.setOnClickListener(v -> updateOrRemoveItem(cartItem, 0));
                itemView.setOnClickListener(v -> navigateToServiceDetail(cartItem));
            }

            private void navigateToServiceDetail(CartItem cartItem) {
                if (cartItem == null) {
                    Toast.makeText(context, "Lỗi: Không có thông tin sản phẩm", Toast.LENGTH_SHORT).show();
                    return;
                }
                loadFullServiceData(cartItem);
            }

            private void loadFullServiceData(CartItem cartItem) {
                if (cartItem == null || TextUtils.isEmpty(cartItem.getServiceId())) {
                    Toast.makeText(context, "Lỗi: Thông tin sản phẩm không hợp lệ", Toast.LENGTH_SHORT).show();
                    return;
                }

                DatabaseReference serviceRef = FirebaseDatabase.getInstance()
                        .getReference("Services")
                        .child(cartItem.getServiceId());
                
                serviceRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Service fullService = snapshot.getValue(Service.class);
                            if (fullService != null) {
                                fullService.setServiceId(cartItem.getServiceId());
                                navigateToServiceDetailWithFullData(fullService);
                            } else {
                                Service fallbackService = createServiceFromCartItem(cartItem);
                                if (fallbackService != null) {
                                    navigateToServiceDetailWithFullData(fallbackService);
                                } else {
                                    Toast.makeText(context, "Lỗi: Không thể tải thông tin sản phẩm", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Toast.makeText(context, "Sản phẩm không tồn tại hoặc đã bị xóa", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        Service fallbackService = createServiceFromCartItem(cartItem);
                        if (fallbackService != null) {
                            navigateToServiceDetailWithFullData(fallbackService);
                        }
                    }
                });
            }

            private void navigateToServiceDetailWithFullData(Service service) {
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

            private Service createServiceFromCartItem(CartItem cartItem) {
                if (cartItem == null) return null;
                
                try {
                    Service service = new Service();
                    service.setServiceId(cartItem.getServiceId());
                    service.setTitle(cartItem.getTitle());
                    service.setPrice(cartItem.getPrice());
                    service.setId_seller(cartItem.getSellerId());
                    service.setImages(cartItem.getImages());
                    service.setAmount(cartItem.getAmount());
                    service.setDescription("Đang tải mô tả...");
                    service.setSold(0);
                    service.setCreatedAt(System.currentTimeMillis());
                    service.setId_own_post(cartItem.getSellerId());
                    service.setCategory("Không xác định");
                    
                    return service;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            void bindData(CartItem cartItem) {
                if (cartItem == null) return;
                cartItemName.setText(cartItem.getTitle() != null ? cartItem.getTitle() : "Sản phẩm");
                itemPrice.setText(String.format("%s VNĐ", decimalFormat.format(cartItem.getPrice())));
                quantity.setText(String.valueOf(cartItem.getAmount()));

                if (cartItem.getImages() != null && !cartItem.getImages().isEmpty()) {
                    String firstImage = cartItem.getImages().get(0);
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

                setupClickListeners(cartItem);
            }
        }
    }
}
