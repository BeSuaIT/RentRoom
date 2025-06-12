package com.example.timphongtro.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.interfaces.ItemClickListener;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.timphongtro.Adapters.UtilityAdapter;
import com.example.timphongtro.Adapters.FurnitureAdapter;
import com.example.timphongtro.Adapters.ZoomImageAdapter;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.Models.Meeting;
import com.example.timphongtro.Models.User;
import com.example.timphongtro.R;
import com.example.timphongtro.Utils.AuthUtils;
import com.example.timphongtro.Utils.GsonUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostDetailActivity extends AppCompatActivity {
    private static final int CALL_PHONE_PERMISSION_REQUEST_CODE = 1;
    private TextView roomTitleTextView, priceTextView, addressCombinedTextView, phoneTextView, roomTypeTextView,
            floorTextView, roomAreaTextView, depositTextView, capacityTextView, genderTextView,
            waterPriceTextView, internetPriceTextView, electricPriceTextView, roomDescriptionTextView,
            userNameTextView, scheduleTime, userProfileTextView, loveTextView, scheduleDate;
    private RecyclerView furnitureRecyclerView, utilityRecyclerView;
    private ImageView imageViewBack, imageViewLove;
    private Button callButton, scheduleVisitButton;
    private MaterialCardView userPostCard;
    private ImageSlider roomImageSlider;
    private MaterialButton confirmButton, followButton;
    private EditText nameEditText, phoneEditText, noteEditText;
    private BottomSheetDialog scheduleVisitDialog;
    private Dialog dialogZoomImg;
    private FirebaseUser currentUser;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference visitScheduleDatabaseRef, userPostReference, roomDatabaseRef;
    private Room room;
    private UUID uuid;
    private boolean isRoomLoved;
    private Calendar bookingDate, bookingTime;
    private FurnitureAdapter furnitureAdapter;
    private UtilityAdapter utilityAdapter;
    private ShapeableImageView userProfileImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_room);

        initializeViews();
        setupFirebaseReferences();
        loadRoomData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUserState();
    }

    private void refreshUserState() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            AuthUtils.checkUserExistsOnStartup(this, currentUser, exists -> {
                if (!exists) {
                    currentUser = null;
                    updateUIBasedOnLoginState();
                    
                    if (room != null) {
                        setFollowButtonToDefault();
                    }
                } else {
                    updateUIBasedOnLoginState();
                    
                    if (room != null) {
                        checkLoveRoom();
                        checkFollowStatus();
                    }
                }
            });
        } else {
            updateUIBasedOnLoginState();
            if (room != null) {
                setFollowButtonToDefault();
            }
        }
    }

    private void setFollowButtonToDefault() {
        followButton.setIcon(getDrawable(R.drawable.ic_follow));
    }

    private void initializeViews() {
        roomImageSlider = findViewById(R.id.roomImageSlider);
        roomTypeTextView = findViewById(R.id.roomTypeTextView);
        loveTextView = findViewById(R.id.loveTextView);
        roomTitleTextView = findViewById(R.id.roomTitleTextView);
        priceTextView = findViewById(R.id.priceTextView);
        addressCombinedTextView = findViewById(R.id.addressCombinedTextView);
        phoneTextView = findViewById(R.id.phoneTextView);
        floorTextView = findViewById(R.id.floorTextView);
        roomAreaTextView = findViewById(R.id.roomAreaTextView);
        depositTextView = findViewById(R.id.depositTextView);
        capacityTextView = findViewById(R.id.capacityTextView);
        genderTextView = findViewById(R.id.genderTextView);
        furnitureRecyclerView = findViewById(R.id.furnitureRecyclerView);
        utilityRecyclerView = findViewById(R.id.utilityRecyclerView);
        waterPriceTextView = findViewById(R.id.waterPriceTextView);
        internetPriceTextView = findViewById(R.id.internetPriceTextView);
        electricPriceTextView = findViewById(R.id.electricPriceTextView);
        roomDescriptionTextView = findViewById(R.id.roomDescriptionTextView);
        imageViewBack = findViewById(R.id.imageViewBack);
        imageViewLove = findViewById(R.id.imageViewLove);
        callButton = findViewById(R.id.callButton);
        scheduleVisitButton = findViewById(R.id.scheduleVisitButton);
        userPostCard = findViewById(R.id.userPostCard);
        userNameTextView = findViewById(R.id.userNameTextView);
        userProfileTextView = findViewById(R.id.userProfileTextView);
        followButton = findViewById(R.id.followButton);
        userProfileImageView = findViewById(R.id.userProfileImageView);

        imageViewBack.setOnClickListener(v -> finish());
        scheduleVisitButton.setOnClickListener(v -> showBottomDialog());
        imageViewLove.setOnClickListener(v -> handleLoveButtonClick());
        followButton.setOnClickListener(v -> handleFollowButtonClick());

        userPostCard.setOnClickListener(v -> navigateToUserProfile());
        
        phoneTextView.setOnClickListener(v -> {
            FirebaseUser realTimeUser = FirebaseAuth.getInstance().getCurrentUser();
            if (realTimeUser == null) {
                AuthUtils.showLoginRequiredDialog(this, "sao chép số điện thoại", "có thể");
                return;
            }
            copyToClipboard(phoneTextView.getText().toString(), "Đã sao chép số điện thoại");
        });
        addressCombinedTextView.setOnClickListener(v -> {
            FirebaseUser realTimeUser = FirebaseAuth.getInstance().getCurrentUser();
            if (realTimeUser == null) {
                AuthUtils.showLoginRequiredDialog(this,"sao chép địa chỉ", "có thể");
                return;
            }
            copyToClipboard(addressCombinedTextView.getText().toString(), "Đã sao chép địa chỉ");
        });
        callButton.setOnClickListener(v -> handleCallButtonClick());
    }

    private void navigateToUserProfile() {
        if (room == null) {
            Toast.makeText(this, "⚠️ Thông tin phòng chưa được tải", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(PostDetailActivity.this, UserActivity.class);
        intent.putExtra("id_own_post", room.getId_own_post());
        startActivity(intent);
    }

    private void setupFirebaseReferences() {
        firebaseDatabase = FirebaseDatabase.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        updateUIBasedOnLoginState();
    }

    private void updateUIBasedOnLoginState() {
        if (currentUser == null) {
            // Chưa đăng nhập: set click listeners để hiển thị dialog yêu cầu đăng nhập
            imageViewLove.setOnClickListener(v -> showLoginRequiredDialog("yêu thích"));
            followButton.setOnClickListener(v -> showLoginRequiredDialog("theo dõi"));
            scheduleVisitButton.setOnClickListener(v -> showLoginRequiredDialog("đặt lịch xem"));
            
            // Icon mặc định
            imageViewLove.setImageResource(R.drawable.ic_heart_thin_icon);
            followButton.setIcon(getDrawable(R.drawable.ic_follow));
        } else {
            // Đã đăng nhập: set click listeners cho các chức năng
            imageViewLove.setOnClickListener(v -> handleLoveButtonClick());
            followButton.setOnClickListener(v -> handleFollowButtonClick());
            scheduleVisitButton.setOnClickListener(v -> showBottomDialog());
        }
    }

    private void loadRoomData() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String roomString = bundle.getString("DataRoom");

            room = GsonUtils.fromJson(roomString, Room.class);
            
            if (room != null) {
                displayRoomDetails();
                setupImageSlider();
                setupAdapters();
                loadUserPostInfo();
                checkLoveRoom();

                if (currentUser != null) {
                    checkFollowStatus();
                } else {
                    setFollowButtonToDefault();
                }
            } else {
                Toast.makeText(this, "Lỗi tải dữ liệu phòng", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Không có dữ liệu phòng", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void displayRoomDetails() {
        if (room == null) return;

        roomTypeTextView.setText(room.getType_room() != null ? room.getType_room() : "Loại phòng");
        roomTitleTextView.setText(room.getTitle_room() != null ? room.getTitle_room() : "Tiêu đề phòng");
        
        long price = room.getPrice_room();
        DecimalFormat decimalFormat = new DecimalFormat("#,###.###");
        decimalFormat.setDecimalSeparatorAlwaysShown(false);
        String priceNumber = decimalFormat.format(price) + " đ/tháng";
        priceTextView.setText(priceNumber);

        if (room.getAddress() != null && room.getAddress().getAddress_combine() != null) {
            addressCombinedTextView.setText(room.getAddress().getAddress_combine());
        } else {
            addressCombinedTextView.setText("Địa chỉ không xác định");
        }
        
        phoneTextView.setText(maskPhoneNumber(room.getPhone()));
        floorTextView.setText(String.valueOf(room.getFloor()));
        roomAreaTextView.setText(room.getArea_room() != null ? room.getArea_room() : "0");
        depositTextView.setText(decimalFormat.format(room.getDeposit_room()));
        capacityTextView.setText(String.valueOf(room.getPerson_in_room()));
        genderTextView.setText(room.getGender_room() != null ? room.getGender_room() : "Không xác định");
        waterPriceTextView.setText(decimalFormat.format(room.getPrice_water()));
        internetPriceTextView.setText(decimalFormat.format(room.getPrice_internet()));
        electricPriceTextView.setText(decimalFormat.format(room.getPrice_electric()));
        roomDescriptionTextView.setText(room.getDescription_room() != null ? room.getDescription_room() : "Không có mô tả");
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return phoneNumber != null ? phoneNumber : "Chưa cập nhật";
        }
        
        String firstPart = phoneNumber.substring(0, 3);
        String lastPart = phoneNumber.substring(phoneNumber.length() - 2);
        String middlePart = "*".repeat(phoneNumber.length() - 5);
        
        return firstPart + middlePart + lastPart;
    }

    private void setupImageSlider() {
        ArrayList<SlideModel> slideModels = new ArrayList<>();
        ArrayList<String> allImages = room.getImages();
        if (allImages != null && !allImages.isEmpty()) {
            for (String imageUrl : allImages) {
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    slideModels.add(new SlideModel(imageUrl, ScaleTypes.CENTER_CROP));
                }
            }
            roomImageSlider.setImageList(slideModels);

            roomImageSlider.setItemClickListener(new ItemClickListener() {
                @Override
                public void doubleClick(int i) {
                }

                @Override
                public void onItemSelected(int position) {
                    showZoomImgDialog(position);
                }
            });
        }
    }

    private void setupAdapters() {
        if (room.getRoomFurniture() != null) {
            furnitureAdapter = new FurnitureAdapter(this, room.getRoomFurniture());
        } else {
            furnitureAdapter = new FurnitureAdapter(this, new ArrayList<>());
        }
        furnitureRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        furnitureRecyclerView.setAdapter(furnitureAdapter);

        if (room.getRoomUtilities() != null) {
            utilityAdapter = new UtilityAdapter(this, room.getRoomUtilities());
        } else {
            utilityAdapter = new UtilityAdapter(this, new ArrayList<>());
        }
        utilityRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        utilityRecyclerView.setAdapter(utilityAdapter);
    }

    private void handleLoveButtonClick() {
        FirebaseUser realTimeUser = FirebaseAuth.getInstance().getCurrentUser();
        if (realTimeUser == null) {
            showLoginRequiredDialog("yêu thích");
            return;
        }

        AuthUtils.isUserExists(realTimeUser, exists -> {
            if (!exists) {
                AuthUtils.clearAllLoginData(this);
                showLoginRequiredDialog("yêu thích");
                return;
            }
            
            currentUser = realTimeUser;
            
            isRoomLoved = true;
            roomDatabaseRef.child("userLovePost").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (isRoomLoved) {
                        int currentLoveCount = (int) snapshot.getChildrenCount();
                        toggleLoveStatus(snapshot, currentLoveCount);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        });
    }

    private void showLoginRequiredDialog(String feature) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Yêu cầu đăng nhập")
               .setMessage("Bạn cần đăng nhập để " + feature + " phòng này")
               .setPositiveButton("Đăng nhập", (dialog, which) -> {
                   Intent intent = new Intent(this, LoginActivity.class);
                   intent.putExtra("previous_activity", PostDetailActivity.class.getName());
                   startActivity(intent);
               })
               .setNegativeButton("Hủy", null)
               .show();
    }

    private void toggleLoveStatus(DataSnapshot snapshot, int currentLoveCount) {
        if (snapshot.hasChild(currentUser.getUid())) {
            removeLoveStatus(currentLoveCount - 1);
        } else {
            addLoveStatus(currentLoveCount + 1);
        }
        isRoomLoved = false;
    }

    private void removeLoveStatus(int newCount) {
        roomDatabaseRef.child("userLovePost").child(currentUser.getUid()).removeValue();
        imageViewLove.setImageResource(R.drawable.ic_heart_thin_icon);
        loveTextView.setText("Lượt yêu thích: " + newCount);
        Toast.makeText(PostDetailActivity.this, "Bỏ yêu thích thành công", Toast.LENGTH_SHORT).show();
    }

    private void addLoveStatus(int newCount) {
        roomDatabaseRef.child("userLovePost").child(currentUser.getUid()).setValue(true);
        imageViewLove.setImageResource(R.drawable.ic_love_fill);
        loveTextView.setText("Lượt yêu thích: " + newCount);
        Toast.makeText(PostDetailActivity.this, "Yêu thích thành công", Toast.LENGTH_SHORT).show();
    }

    private void checkLoveRoom() {
        roomDatabaseRef.child("userLovePost").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    int loveCount = (int) snapshot.getChildrenCount();
                    loveTextView.setText("Lượt yêu thích: " + loveCount);

                    if (currentUser != null && snapshot.hasChild(currentUser.getUid())) {
                        imageViewLove.setImageResource(R.drawable.ic_love_fill);
                    } else {
                        imageViewLove.setImageResource(R.drawable.ic_heart_thin_icon);
                    }
                } else {
                    loveTextView.setText("Chưa có lượt yêu thích");
                    imageViewLove.setImageResource(R.drawable.ic_heart_thin_icon);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loveTextView.setText("Chưa có lượt yêu thích");
                imageViewLove.setImageResource(R.drawable.ic_heart_thin_icon);
            }
        });
    }

    private void copyToClipboard(String text, String message) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Label", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showZoomImgDialog(int currentPosition) {
        dialogZoomImg = new Dialog(PostDetailActivity.this);
        dialogZoomImg.setContentView(R.layout.dialog_zoom_img);

        ViewPager2 viewPager = dialogZoomImg.findViewById(R.id.viewPagerZoom);
        ImageView imageViewBack = dialogZoomImg.findViewById(R.id.imageViewBack);

        ArrayList<String> allImages = room.getImages();
        if (allImages != null && !allImages.isEmpty()) {
            ZoomImageAdapter adapter = new ZoomImageAdapter(this, allImages);
            viewPager.setAdapter(adapter);
            viewPager.setCurrentItem(currentPosition, false);
        }

        imageViewBack.setOnClickListener(v -> {
            if (dialogZoomImg.isShowing()) {
                dialogZoomImg.dismiss();
            }
        });

        dialogZoomImg.show();
        Window window = dialogZoomImg.getWindow();
        if (window != null) {
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

    private void showBottomDialog() {
        FirebaseUser realTimeUser = FirebaseAuth.getInstance().getCurrentUser();
        if (realTimeUser == null) {
            showLoginRequiredDialog("đặt lịch xem");
            return;
        }

        AuthUtils.isUserExists(realTimeUser, exists -> {
            if (!exists) {
                AuthUtils.clearAllLoginData(this);
                showLoginRequiredDialog("đặt lịch xem");
                return;
            }
            
            currentUser = realTimeUser;
            openScheduleDialog();
        });
    }

    private void openScheduleDialog() {
        scheduleVisitDialog = new BottomSheetDialog(PostDetailActivity.this);
        scheduleVisitDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        scheduleVisitDialog.setContentView(R.layout.dialog_book_room);

        ImageView cancelButton = scheduleVisitDialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> {
            if (scheduleVisitDialog.isShowing() && scheduleVisitDialog != null) {
                scheduleVisitDialog.dismiss();
            }
        });

        nameEditText = scheduleVisitDialog.findViewById(R.id.edtYourName);
        phoneEditText = scheduleVisitDialog.findViewById(R.id.edtPhone);
        noteEditText = scheduleVisitDialog.findViewById(R.id.edtNote);
        scheduleDate = scheduleVisitDialog.findViewById(R.id.edtDate);
        scheduleTime = scheduleVisitDialog.findViewById(R.id.edtTime);

        loadCurrentUserInfo();

        bookingDate = Calendar.getInstance();
        bookingTime = Calendar.getInstance();

        DatePickerDialog.OnDateSetListener dateListener = (view, year, month, dayOfMonth) -> {
            bookingDate.set(Calendar.YEAR, year);
            bookingDate.set(Calendar.MONTH, month);
            bookingDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
            scheduleDate.setText(dateFormat.format(bookingDate.getTime()));

            scheduleTime.setText("Chọn giờ hẹn");
        };

        TimePickerDialog.OnTimeSetListener timeListener = (view, hourOfDay, minute) -> {
            if (!isValidTime(hourOfDay, minute)) {
                Toast.makeText(this, "Vui lòng chọn thời gian từ 8:00 - 18:00", Toast.LENGTH_SHORT).show();
                return;
            }
            
            bookingTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            bookingTime.set(Calendar.MINUTE, minute);
            
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            scheduleTime.setText(timeFormat.format(bookingTime.getTime()));
        };

        scheduleDate.setOnClickListener(v -> {
            DatePickerDialog dateDialog = new DatePickerDialog(
                PostDetailActivity.this, 
                dateListener, 
                bookingDate.get(Calendar.YEAR), 
                bookingDate.get(Calendar.MONTH), 
                bookingDate.get(Calendar.DAY_OF_MONTH)
            );

            dateDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

            Calendar maxDate = Calendar.getInstance();
            maxDate.add(Calendar.DAY_OF_MONTH, 30);
            dateDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());
            
            dateDialog.show();
        });

        scheduleTime.setOnClickListener(v -> {
            if (scheduleDate.getText().toString().equals("Chọn ngày hẹn")) {
                Toast.makeText(this, "Vui lòng chọn ngày trước", Toast.LENGTH_SHORT).show();
                return;
            }
            
            TimePickerDialog timeDialog = new TimePickerDialog(
                PostDetailActivity.this,
                timeListener,
                9, // Default 9:00 AM
                0,
                true // 24-hour format
            );
            timeDialog.show();
        });

        confirmButton = scheduleVisitDialog.findViewById(R.id.btnConfirm);

        MaterialButton btnCancel = scheduleVisitDialog.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> {
            if (scheduleVisitDialog.isShowing()) {
                scheduleVisitDialog.dismiss();
            }
        });

        visitScheduleDatabaseRef = null;
        uuid = UUID.randomUUID();
        visitScheduleDatabaseRef = firebaseDatabase.getReference("MeetingSchedules/" + uuid.toString());

        confirmButton.setOnClickListener(v -> scheduleVisitRoom());

        scheduleVisitDialog.show();
        scheduleVisitDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        scheduleVisitDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        scheduleVisitDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        scheduleVisitDialog.getWindow().setGravity(Gravity.BOTTOM);
        scheduleVisitDialog.setCancelable(true);
    }

    private void loadCurrentUserInfo() {
        if (currentUser == null) return;

        nameEditText.setEnabled(false);
        phoneEditText.setEnabled(false);
        nameEditText.setText("Đang tải...");
        phoneEditText.setText("Đang tải...");
        
        DatabaseReference currentUserRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUser.getUid());
        
        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                nameEditText.setEnabled(true);
                phoneEditText.setEnabled(true);
                
                if (snapshot.exists()) {
                    User userData = snapshot.getValue(User.class);
                    if (userData != null) {
                        String userName = userData.getName();
                        if (!TextUtils.isEmpty(userName)) {
                            nameEditText.setText(userName);
                        } else {
                            nameEditText.setText("");
                        }

                        String userPhone = userData.getPhone();
                        if (!TextUtils.isEmpty(userPhone) && !userPhone.equals("Chưa cập nhật")) {
                            phoneEditText.setText(userPhone);
                        } else {
                            phoneEditText.setText("");
                        }
                    } else {
                        clearUserFields();
                    }
                } else {
                    clearUserFields();
                    Toast.makeText(PostDetailActivity.this, 
                        "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                nameEditText.setEnabled(true);
                phoneEditText.setEnabled(true);
                clearUserFields();
                
                Toast.makeText(PostDetailActivity.this, 
                    "Lỗi tải thông tin: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearUserFields() {
        nameEditText.setText("");
        phoneEditText.setText("");
    }

    private void scheduleVisitRoom() {
        boolean isValid = true;

        String name = nameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("Vui lòng nhập tên");
            isValid = false;
        }

        if (TextUtils.isEmpty(scheduleDate.getText().toString()) || 
            scheduleDate.getText().toString().equals("Chọn ngày hẹn")) {
            Toast.makeText(this, "Vui lòng chọn ngày hẹn", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (TextUtils.isEmpty(scheduleTime.getText().toString()) || 
            scheduleTime.getText().toString().equals("Chọn giờ hẹn")) {
            Toast.makeText(this, "Vui lòng chọn giờ hẹn", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (isValid && isTimeInPast(bookingDate, bookingTime)) {
            Toast.makeText(this, "Không thể chọn thời gian trong quá khứ", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        String phone = phoneEditText.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            phoneEditText.setError("Vui lòng nhập số điện thoại");
            isValid = false;
        } else {
            String regex = "^0\\d{9}$"; // format: 0xxxxxxxxx
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(phone);
            if (!matcher.matches()) {
                phoneEditText.setError("Số điện thoại phải có 10 chữ số và bắt đầu bằng 0");
                isValid = false;
            }
        }

        if (isValid) {
            String combinedDateTime = scheduleDate.getText().toString() + " lúc " + scheduleTime.getText().toString();

            String note = noteEditText.getText().toString().trim();
            if (TextUtils.isEmpty(note)) {
                note = "Không có ghi chú";
            }
            
            Meeting schedule = new Meeting(
                uuid.toString(), 
                name,
                phone,
                note, 
                combinedDateTime,
                room.getId_own_post(), 
                currentUser.getUid(), 
                "0", 
                room.getId_room()
            );

            if (!currentUser.getUid().equals(room.getId_own_post())) {
                confirmButton.setEnabled(false);
                confirmButton.setText("Đang xử lý...");
                
                visitScheduleDatabaseRef.setValue(schedule)
                    .addOnSuccessListener(unused -> {
                        scheduleVisitDialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Đặt lịch thành công!", Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        confirmButton.setEnabled(true);
                        confirmButton.setText("Xác nhận");
                        Toast.makeText(getApplicationContext(), "Đặt lịch thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            } else {
                Toast.makeText(getApplicationContext(), "Bạn không thể đặt lịch hẹn với chính bài đăng của mình", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Vui lòng kiểm tra lại thông tin", Toast.LENGTH_LONG).show();
        }
    }

    private void loadUserPostInfo() {
        userPostReference = firebaseDatabase.getReference("Users/" + room.getId_own_post());
        
        userPostReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        userNameTextView.setText(user.getName() != null ? user.getName() : "Chưa cập nhật");
                        updateUserProfileImage(user);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                userNameTextView.setText("Lỗi tải thông tin");
            }
        });

        roomDatabaseRef = firebaseDatabase.getReference("Posts/" + room.getId_room());
    }

    private void updateUserProfileImage(User user) {
        String avatarUrl = user.getAvatarUrl();
        if (!TextUtils.isEmpty(avatarUrl)) {
            userProfileTextView.setVisibility(View.GONE);
            userProfileImageView.setVisibility(View.VISIBLE);

            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.avatar)
                .error(R.drawable.avatar)
                .into(userProfileImageView);
        } else {
            userProfileImageView.setVisibility(View.GONE);
            userProfileTextView.setVisibility(View.VISIBLE);
            userProfileTextView.setText(getFirstLetter(user.getName()));
        }
    }

    private void handleFollowButtonClick() {
        FirebaseUser realTimeUser = FirebaseAuth.getInstance().getCurrentUser();
        if (realTimeUser == null) {
            showLoginRequiredDialog("theo dõi");
            return;
        }

        AuthUtils.isUserExists(realTimeUser, exists -> {
            if (!exists) {
                AuthUtils.clearAllLoginData(this);
                showLoginRequiredDialog("theo dõi");
                return;
            }
            
            currentUser = realTimeUser;
            
            DatabaseReference followRef = FirebaseDatabase.getInstance()
                    .getReference("FollowPosts")
                    .child(currentUser.getUid())
                    .child(room.getId_room());

            followRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (task.getResult().exists()) {
                        followRef.removeValue()
                            .addOnSuccessListener(unused -> {
                                followButton.setIcon(getDrawable(R.drawable.ic_follow));
                                Toast.makeText(this, "Đã bỏ theo dõi", Toast.LENGTH_SHORT).show();
                            });
                    } else {
                        followRef.setValue(true)
                            .addOnSuccessListener(unused -> {
                                followButton.setIcon(getDrawable(R.drawable.ic_unfollow));
                                Toast.makeText(this, "Đã theo dõi phòng này", Toast.LENGTH_SHORT).show();
                            });
                    }
                }
            });
        });
    }

    private void checkFollowStatus() {
        if (currentUser != null) {
            DatabaseReference followRef = FirebaseDatabase.getInstance()
                    .getReference("FollowPosts")
                    .child(currentUser.getUid())
                    .child(room.getId_room());

            followRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        followButton.setIcon(getDrawable(R.drawable.ic_unfollow));
                    } else {
                        followButton.setIcon(getDrawable(R.drawable.ic_follow));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
    }

    public static String getFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return "?";
        }
        
        String[] words = input.split(" ");
        StringBuilder result = new StringBuilder();

        if (words.length == 1) {
            if (words[0].length() > 0) {
                result.append(words[0].charAt(0));
            }
        } else {
            for (int i = 0; i < Math.min(words.length, 2); i++) {
                if (words[i].length() > 0) {
                    result.append(words[i].charAt(0));
                }
            }
        }
        return result.toString().toUpperCase();
    }

    private void handleCallButtonClick() {
        FirebaseUser realTimeUser = FirebaseAuth.getInstance().getCurrentUser();
        if (realTimeUser == null) {
            AuthUtils.showLoginRequiredDialog(this,"gọi điện", "có thể");
            return;
        }

        AuthUtils.isUserExists(realTimeUser, exists -> {
            if (!exists) {
                AuthUtils.clearAllLoginData(this);
                AuthUtils.showLoginRequiredDialog(this,"gọi điện", "có thể");
                return;
            }
            
            currentUser = realTimeUser;
            checkCurrentUserPhoneAndMakeCall();
        });
    }

    private void checkCurrentUserPhoneAndMakeCall() {
        DatabaseReference currentUserRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUser.getUid());
        
        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User currentUserData = snapshot.getValue(User.class);
                    if (currentUserData != null && 
                        !TextUtils.isEmpty(currentUserData.getPhone()) && 
                        !currentUserData.getPhone().equals("Chưa cập nhật")) {

                        checkPermissionAndCall();
                    } else {
                        showUpdatePhoneDialog();
                    }
                } else {
                    Toast.makeText(PostDetailActivity.this,
                        "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PostDetailActivity.this,
                    "Lỗi kiểm tra thông tin người dùng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUpdatePhoneDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Cần cập nhật số điện thoại")
            .setMessage("Bạn cần cập nhật số điện thoại trong hồ sơ để có thể sử dụng tính năng gọi điện")
            .setPositiveButton("Cập nhật ngay", (dialog, which) -> {
                Intent intent = new Intent(this, EditProfileActivity.class);
                startActivity(intent);
            })
            .setNegativeButton("Để sau", null)
            .show();
    }

    private void checkPermissionAndCall() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CALL_PHONE},
                    CALL_PHONE_PERMISSION_REQUEST_CODE);
        } else {
            makePhoneCall();
        }
    }

    private void makePhoneCall() {
        String originalPhoneNumber = room.getPhone();
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + originalPhoneNumber));

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Không thể thực hiện cuộc gọi", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALL_PHONE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makePhoneCall();
            } else {
                Toast.makeText(this, "Quyền gọi điện bị từ chối", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isValidTime(int hour, int minute) {
        return hour >= 8 && hour < 18;
    }

    private boolean isTimeInPast(Calendar selectedDate, Calendar selectedTime) {
        Calendar now = Calendar.getInstance();
        Calendar selectedDateTime = Calendar.getInstance();

        selectedDateTime.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR));
        selectedDateTime.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH));
        selectedDateTime.set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH));
        selectedDateTime.set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY));
        selectedDateTime.set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE));
        selectedDateTime.set(Calendar.SECOND, 0);
        selectedDateTime.set(Calendar.MILLISECOND, 0);

        return selectedDateTime.before(now);
    }
}