package com.example.timphongtro.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
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
import java.util.Map;

public class ServiceDetailActivity extends AppCompatActivity {

    private TextView service_title, service_price, service_sold_count, service_description, service_seller_name;
    private Button btn_add_to_cart;
    private ImageView button_cart, imageView_back;
    private ImageSlider imageSlider;
    private Service service;
    private DecimalFormat decimalFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_detail);

        initializeViews();
        loadServiceData();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUserState();
    }

    private void refreshUserState() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser != null) {
            AuthUtils.checkUserExistsOnStartup(this, currentUser, exists -> {
                if (!exists) {
                    updateUIForLoggedOutState();
                }
            });
        } else {
            updateUIForLoggedOutState();
        }
    }

    private void updateUIForLoggedOutState() {
        btn_add_to_cart.setText("Đăng nhập để mua");
    }

    private void initializeViews() {
        decimalFormat = new DecimalFormat("#,###");
        decimalFormat.setDecimalSeparatorAlwaysShown(false);

        service_title = findViewById(R.id.service_title);
        service_price = findViewById(R.id.service_price);
        service_sold_count = findViewById(R.id.service_sold_count);
        service_description = findViewById(R.id.service_description);
        service_seller_name = findViewById(R.id.service_seller_name);
        btn_add_to_cart = findViewById(R.id.btn_add_to_cart);
        button_cart = findViewById(R.id.button_cart);
        imageView_back = findViewById(R.id.imageView_back);
        imageSlider = findViewById(R.id.ServiceImage);
    }

    private void setupClickListeners() {
        imageView_back.setOnClickListener(v -> finish());
        
        button_cart.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                AuthUtils.isUserExists(user, exists -> {
                    if (!exists) {
                        AuthUtils.clearAllLoginData(this);
                        AuthUtils.showLoginRequiredDialog(this, "giỏ hàng", "xem");
                        return;
                    }
                    
                    startActivity(new Intent(ServiceDetailActivity.this, CartManagementActivity.class));
                });
            } else {
                AuthUtils.showLoginRequiredDialog(this, "giỏ hàng", "xem");
            }
        });

        btn_add_to_cart.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                AuthUtils.isUserExists(user, exists -> {
                    if (!exists) {
                        AuthUtils.clearAllLoginData(this);
                        AuthUtils.showLoginRequiredDialog(this, "sản phẩm vào giỏ hàng", "thêm");
                        return;
                    }
                    
                    addServiceToCart(user);
                });
            } else {
                AuthUtils.showLoginRequiredDialog(this, "sản phẩm vào giỏ hàng", "thêm");
            }
        });

        service_seller_name.setOnClickListener(v -> {
            if (service != null && !TextUtils.isEmpty(service.getId_seller())) {
                navigateToSellerProfile();
            } else {
                Toast.makeText(this, "Không có thông tin người bán", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToSellerProfile() {
        if (service == null || TextUtils.isEmpty(service.getId_seller())) {
            Toast.makeText(this, "Không có thông tin người bán", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(service.getId_seller());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        User seller = snapshot.getValue(User.class);
                        if (seller != null) {
                            if (TextUtils.isEmpty(seller.getUid())) {
                                seller.setUid(service.getId_seller());
                            }

                            String sellerJson = GsonUtils.toJson(seller);
                            if (sellerJson != null) {
                                Intent intent = new Intent(ServiceDetailActivity.this, UserActivity.class);
                                intent.putExtra("userData", sellerJson);
                                startActivity(intent);
                            } else {
                                navigateToSellerProfileFallback();
                            }
                        } else {
                            navigateToSellerProfileFallback();
                        }
                    } catch (Exception e) {
                        Toast.makeText(ServiceDetailActivity.this, "Lỗi tải thông tin người bán", Toast.LENGTH_SHORT).show();
                        navigateToSellerProfileFallback();
                    }
                } else {
                    Toast.makeText(ServiceDetailActivity.this, "Không tìm thấy thông tin người bán", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ServiceDetailActivity.this, "Lỗi kết nối: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                navigateToSellerProfileFallback();
            }
        });
    }

    private void navigateToSellerProfileFallback() {
        Intent intent = new Intent(ServiceDetailActivity.this, UserActivity.class);
        intent.putExtra("id_own_post", service.getId_seller());
        intent.putExtra("fallbackMode", true);
        startActivity(intent);
    }

    private void addServiceToCart(FirebaseUser user) {
        if (service == null) {
            Toast.makeText(this, "Lỗi: Không có thông tin dịch vụ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(service.getServiceId()) || TextUtils.isEmpty(service.getId_seller())) {
            Toast.makeText(this, "Lỗi: Thông tin dịch vụ không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        btn_add_to_cart.setEnabled(false);
        btn_add_to_cart.setText("Đang thêm...");

        String userId = user.getUid();
        DatabaseReference cartsRef = FirebaseDatabase.getInstance().getReference("Carts");

        cartsRef.orderByChild("buyerId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Cart existingCart = null;
                String existingCartId = null;

                // Tìm cart có cùng sellerId
                for (DataSnapshot cartSnapshot : dataSnapshot.getChildren()) {
                    Cart cart = cartSnapshot.getValue(Cart.class);
                    if (cart != null && service.getId_seller().equals(cart.getSellerId())) {
                        existingCart = cart;
                        existingCartId = cartSnapshot.getKey();
                        break;
                    }
                }

                if (existingCart != null && existingCartId != null) {
                    updateExistingCart(cartsRef, existingCartId, existingCart);
                } else {
                    createNewCart(cartsRef, userId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ServiceDetailActivity.this, "Không thể thêm vào giỏ hàng: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                resetAddButton();
            }
        });
    }

    private void updateExistingCart(DatabaseReference cartsRef, String cartId, Cart existingCart) {
        Map<String, Integer> cartItems = existingCart.getCartItems();
        int currentAmount = cartItems.getOrDefault(service.getServiceId(), 0);
        cartItems.put(service.getServiceId(), currentAmount + 1);

        // Cập nhật cartItems trong Firebase
        cartsRef.child(cartId).child("cartItems").setValue(cartItems)
                .addOnSuccessListener(aVoid -> {
                    String serviceName = service.getTitle() != null ? service.getTitle() : "Dịch vụ";
                    Toast.makeText(ServiceDetailActivity.this, serviceName + " đã thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
                    resetAddButton();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ServiceDetailActivity.this, "Lỗi thêm vào giỏ hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetAddButton();
                });
    }

    private void createNewCart(DatabaseReference cartsRef, String userId) {
        // Tạo cart mới với ID unique
        Cart newCart = new Cart(userId, service.getId_seller());
        newCart.addOrUpdateItem(service.getServiceId(), 1);

        // Lưu với cartId tự generate
        cartsRef.child(newCart.getCartId()).setValue(newCart)
                .addOnSuccessListener(aVoid -> {
                    String serviceName = service.getTitle() != null ? service.getTitle() : "Dịch vụ";
                    Toast.makeText(ServiceDetailActivity.this, serviceName + " đã thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
                    resetAddButton();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ServiceDetailActivity.this, "Lỗi thêm vào giỏ hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetAddButton();
                });
    }

    private void resetAddButton() {
        btn_add_to_cart.setEnabled(true);
        btn_add_to_cart.setText("Thêm vào giỏ hàng");
    }

    private void loadServiceData() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String serviceString = bundle.getString("ServiceData");
            if (serviceString != null) {
                service = GsonUtils.fromJson(serviceString, Service.class);
                
                if (service != null) {
                    displayServiceDetails();
                    setupImageSlider();
                    loadSellerName();

                    if (isServiceDataIncomplete()) {
                        loadCompleteServiceData();
                    }
                } else {
                    Toast.makeText(this, "Lỗi: Dữ liệu dịch vụ không hợp lệ", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(this, "Lỗi: Không có dữ liệu dịch vụ", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Lỗi: Thiếu dữ liệu", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean isServiceDataIncomplete() {
        return service != null && 
               (TextUtils.isEmpty(service.getDescription()) || 
                service.getDescription().equals("Đang tải mô tả...") ||
                service.getDescription().equals("Chi tiết sản phẩm sẽ được tải từ cơ sở dữ liệu"));
    }

    private void loadCompleteServiceData() {
        if (service == null || TextUtils.isEmpty(service.getServiceId())) {
            return;
        }

        DatabaseReference servicesRef = FirebaseDatabase.getInstance().getReference("Services");
        
        servicesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Service completeService = null;
                
                // Duyệt qua tất cả categories để tìm service
                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                    DataSnapshot serviceSnapshot = categorySnapshot.child(service.getServiceId());
                    if (serviceSnapshot.exists()) {
                        completeService = serviceSnapshot.getValue(Service.class);
                        if (completeService != null) {
                            completeService.setServiceId(service.getServiceId());
                            break;
                        }
                    }
                }
                
                if (completeService != null) {
                    // Cập nhật service với dữ liệu đầy đủ
                    service = completeService;
                    runOnUiThread(() -> {
                        displayServiceDetails();
                        setupImageSlider();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void displayServiceDetails() {
        if (service == null) return;

        service_title.setText(service.getTitle() != null ? service.getTitle() : "Dịch vụ");
        
        String formattedPrice = decimalFormat.format(service.getPrice()) + " VNĐ";
        service_price.setText(formattedPrice);
        
        service_sold_count.setText(String.valueOf(service.getSold()));
        
        String description = service.getDescription();
        if (!TextUtils.isEmpty(description) && 
            !description.equals("Đang tải mô tả...") && 
            !description.equals("Chi tiết sản phẩm sẽ được tải từ cơ sở dữ liệu")) {
            service_description.setText(description);
        } else {
            service_description.setText("Đang tải mô tả...");
        }
    }

    private void loadSellerName() {
        if (service == null || TextUtils.isEmpty(service.getId_seller())) {
            service_seller_name.setText("Không xác định");
            service_seller_name.setClickable(false);
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(service.getId_seller());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String sellerName = snapshot.child("name").getValue(String.class);
                    if (!TextUtils.isEmpty(sellerName)) {
                        service_seller_name.setText(sellerName);
                        service_seller_name.setClickable(true);
                    } else {
                        service_seller_name.setText("Không xác định");
                        service_seller_name.setClickable(false);
                    }
                } else {
                    service_seller_name.setText("Không xác định");
                    service_seller_name.setClickable(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                service_seller_name.setText("Không xác định");
                service_seller_name.setClickable(false);
            }
        });
    }

    private void setupImageSlider() {
        if (service == null) return;

        ArrayList<SlideModel> slideModels = new ArrayList<>();
        
        if (service.getImages() != null && !service.getImages().isEmpty()) {
            for (String imageUrl : service.getImages()) {
                if (!TextUtils.isEmpty(imageUrl)) {
                    slideModels.add(new SlideModel(imageUrl, ScaleTypes.FIT));
                }
            }
        }
        
        if (slideModels.isEmpty()) {
            slideModels.add(new SlideModel(R.drawable.img_no_image, ScaleTypes.FIT));
        }
        
        imageSlider.setImageList(slideModels, ScaleTypes.FIT);
    }
}