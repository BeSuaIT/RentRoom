package com.example.timphongtro.Adapters;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.timphongtro.Activities.ServiceDetailActivity;
import com.example.timphongtro.Models.Cart;
import com.example.timphongtro.Models.Service;
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
import java.util.Map;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

    private ArrayList<Service> serviceList;
    private Context context;
    private DecimalFormat decimalFormat;

    public ServiceAdapter(Context context, ArrayList<Service> serviceList) {
        this.context = context;
        this.serviceList = serviceList != null ? serviceList : new ArrayList<>();
        this.decimalFormat = new DecimalFormat("#,###");
        this.decimalFormat.setDecimalSeparatorAlwaysShown(false);
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_holder_service, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceAdapter.ServiceViewHolder holder, int position) {
        Service service = serviceList.get(position);
        if (service != null) {
            bindServiceData(holder, service);
            setupClickListeners(holder, service);
        }
    }

    private void bindServiceData(ServiceViewHolder holder, Service service) {
        holder.name.setText(service.getTitle() != null ? service.getTitle() : "Dịch vụ");
        holder.price.setText(decimalFormat.format(service.getPrice()) + " VNĐ");

        if (service.getImages() != null && !service.getImages().isEmpty()) {
            String firstImage = service.getImages().get(0);
            if (!TextUtils.isEmpty(firstImage)) {
                Glide.with(context)
                        .load(firstImage)
                        .placeholder(R.drawable.loading)
                        .error(R.drawable.img_no_image)
                        .centerCrop()
                        .into(holder.image);
            } else {
                holder.image.setImageResource(R.drawable.img_no_image);
            }
        } else {
            holder.image.setImageResource(R.drawable.img_no_image);
        }

        loadSellerName(holder, service.getId_seller());
    }

    private void loadSellerName(ServiceViewHolder holder, String sellerId) {
        if (TextUtils.isEmpty(sellerId)) {
            holder.seller.setText("Người bán không xác định");
            return;
        }

        holder.seller.setText("Đang tải...");

        DatabaseReference sellerRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(sellerId);
                
        sellerRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String sellerName = snapshot.getValue(String.class);
                    holder.seller.setText(!TextUtils.isEmpty(sellerName) ? sellerName : "Người bán");
                } else {
                    holder.seller.setText("Người bán không tìm thấy");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.seller.setText("Lỗi tải thông tin");
            }
        });
    }

    private void setupClickListeners(ServiceViewHolder holder, Service service) {
        holder.btn_add.setOnClickListener(v -> handleAddToCart(service));
        holder.cardService.setOnClickListener(v -> navigateToServiceDetail(service));
    }

    private void handleAddToCart(Service service) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        
        if (user != null) {
            AuthUtils.isUserExists(user, exists -> {
                if (!exists) {
                    AuthUtils.clearAllLoginData(context);
                    showLoginRequiredDialog();
                    return;
                }
                
                addServiceToCart(user.getUid(), service);
            });
        } else {
            showLoginRequiredDialog();
        }
    }

    private void addServiceToCart(String userId, Service service) {
        if (service == null || TextUtils.isEmpty(service.getServiceId()) || TextUtils.isEmpty(service.getId_seller())) {
            Toast.makeText(context, "Lỗi: Thông tin dịch vụ không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference cartsRef = FirebaseDatabase.getInstance().getReference("Carts");

        cartsRef.orderByChild("buyerId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Cart existingCart = null;
                String existingCartId = null;

                for (DataSnapshot cartSnapshot : dataSnapshot.getChildren()) {
                    Cart cart = cartSnapshot.getValue(Cart.class);
                    if (cart != null && service.getId_seller().equals(cart.getSellerId())) {
                        existingCart = cart;
                        existingCartId = cartSnapshot.getKey();
                        break;
                    }
                }

                if (existingCart != null && existingCartId != null) {
                    updateExistingCart(cartsRef, existingCartId, existingCart, service);
                } else {
                    createNewCart(cartsRef, userId, service);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(context, "Không thể thêm vào giỏ hàng: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateExistingCart(DatabaseReference cartsRef, String cartId, Cart existingCart, Service service) {
        Map<String, Integer> cartItems = existingCart.getCartItems();
        int currentAmount = cartItems.getOrDefault(service.getServiceId(), 0);
        cartItems.put(service.getServiceId(), currentAmount + 1);

        cartsRef.child(cartId).child("cartItems").setValue(cartItems)
                .addOnSuccessListener(aVoid -> {
                    String serviceName = service.getTitle() != null ? service.getTitle() : "Dịch vụ";
                    Toast.makeText(context, serviceName + " đã thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Lỗi thêm vào giỏ hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createNewCart(DatabaseReference cartsRef, String userId, Service service) {
        Cart newCart = new Cart(userId, service.getId_seller());
        newCart.addOrUpdateItem(service.getServiceId(), 1);

        cartsRef.child(newCart.getCartId()).setValue(newCart)
                .addOnSuccessListener(aVoid -> {
                    String serviceName = service.getTitle() != null ? service.getTitle() : "Dịch vụ";
                    Toast.makeText(context, serviceName + " đã thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Lỗi thêm vào giỏ hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToServiceDetail(Service service) {
        if (service == null) {
            Toast.makeText(context, "Lỗi: Không có thông tin dịch vụ", Toast.LENGTH_SHORT).show();
            return;
        }

        String serviceJson = GsonUtils.toJson(service);
        if (serviceJson != null) {
            Intent intent = new Intent(context, ServiceDetailActivity.class);
            intent.putExtra("ServiceData", serviceJson);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "Lỗi mở chi tiết dịch vụ", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoginRequiredDialog() {
        AuthUtils.showLoginRequiredDialog(context, "sản phẩm vào giỏ hàng", "thêm");
    }

    @Override
    public int getItemCount() {
        return serviceList != null ? serviceList.size() : 0;
    }

    public static class ServiceViewHolder extends RecyclerView.ViewHolder {
        TextView name, price, seller;
        ImageView image;
        Button btn_add;
        LinearLayout cardService;

        public ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.service_name);
            price = itemView.findViewById(R.id.service_price);
            seller = itemView.findViewById(R.id.service_seller);
            image = itemView.findViewById(R.id.service_img);
            btn_add = itemView.findViewById(R.id.btn_buynow);
            cardService = itemView.findViewById(R.id.cardService);
        }
    }
}