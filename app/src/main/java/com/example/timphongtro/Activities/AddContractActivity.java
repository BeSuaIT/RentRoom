package com.example.timphongtro.Activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timphongtro.Adapters.ImageAdapter;
import com.example.timphongtro.Models.Contract;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class AddContractActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE = 1001;
    private static final String CCCD_FRONT = "front";
    private static final String CCCD_BACK = "back";
    
    private Spinner roomSpinner;
    private EditText landlordNameEdt, landlordPhoneEdt;
    private EditText tenantEmailEdt;
    private Button searchTenantBtn;
    private EditText tenantNameEdt, tenantPhoneEdt, tenantCCCDEdt;
    private EditText startDateEdt, endDateEdt;
    private Button selectCCCDFrontBtn, selectCCCDBackBtn, createContractBtn;
    private ProgressBar progressBar;
    private ImageView backButton;
    private RecyclerView cccdFrontRecyclerView, cccdBackRecyclerView;
    private ImageAdapter cccdFrontAdapter, cccdBackAdapter;
    private ArrayList<Uri> cccdFrontImages, cccdBackImages;
    private ArrayList<Room> roomsList;
    private ArrayAdapter<String> roomAdapter;
    private Room selectedRoom;
    private String currentImageType;
    private String foundTenantId = null;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference postsRef, usersRef, contractsRef;
    private StorageReference storageRef;
    private ActivityResultLauncher<Intent> galleryLauncher, cameraLauncher;
    private BottomSheetDialog imagePickerDialog;
    private String contractId;
    private ImageButton clearSearchBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contract);

        initializeViews();
        initializeFirebase();
        setupRecyclerViews();
        setupActivityResultLaunchers();
        setupListeners();
        loadUserInfo();
        loadAvailableRooms();
    }

    private void initializeViews() {
        roomSpinner = findViewById(R.id.room_spinner);
        landlordNameEdt = findViewById(R.id.landlord_name_edt);
        landlordPhoneEdt = findViewById(R.id.landlord_phone_edt);
        tenantEmailEdt = findViewById(R.id.tenant_email_edt);
        searchTenantBtn = findViewById(R.id.search_tenant_btn);
        tenantNameEdt = findViewById(R.id.tenant_name_edt);
        tenantPhoneEdt = findViewById(R.id.tenant_phone_edt);
        tenantCCCDEdt = findViewById(R.id.tenant_cccd_edt);
        startDateEdt = findViewById(R.id.start_date_edt);
        endDateEdt = findViewById(R.id.end_date_edt);
        selectCCCDFrontBtn = findViewById(R.id.select_cccd_front_btn);
        selectCCCDBackBtn = findViewById(R.id.select_cccd_back_btn);
        createContractBtn = findViewById(R.id.create_contract_btn);
        progressBar = findViewById(R.id.progress_bar);
        backButton = findViewById(R.id.back_button);
        cccdFrontRecyclerView = findViewById(R.id.cccd_front_recycler_view);
        cccdBackRecyclerView = findViewById(R.id.cccd_back_recycler_view);
        clearSearchBtn = findViewById(R.id.clear_search_btn);
    }

    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        postsRef = FirebaseDatabase.getInstance().getReference("Posts");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        contractsRef = FirebaseDatabase.getInstance().getReference("Contracts");
        storageRef = FirebaseStorage.getInstance().getReference();
    }

    private void setupRecyclerViews() {
        cccdFrontImages = new ArrayList<>();
        cccdBackImages = new ArrayList<>();

        cccdFrontAdapter = new ImageAdapter(this, cccdFrontImages);
        cccdFrontRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        cccdFrontRecyclerView.setAdapter(cccdFrontAdapter);

        cccdBackAdapter = new ImageAdapter(this, cccdBackImages);
        cccdBackRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        cccdBackRecyclerView.setAdapter(cccdBackAdapter);
    }

    private void setupActivityResultLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri imageUri = data.getData();
                            Uri cachedUri = copyImageToCache(imageUri);
                            if (cachedUri != null) {
                                addImageToCurrentType(cachedUri);
                            }
                        }
                        if (imagePickerDialog != null) {
                            imagePickerDialog.dismiss();
                        }
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getExtras() != null) {
                            Bundle extras = data.getExtras();
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            if (imageBitmap != null) {
                                Uri imageUri = saveImageToCache(imageBitmap);
                                if (imageUri != null) {
                                    addImageToCurrentType(imageUri);
                                }
                            }
                        }
                        if (imagePickerDialog != null) {
                            imagePickerDialog.dismiss();
                        }
                    }
                });
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
        searchTenantBtn.setOnClickListener(v -> searchTenantByEmail());
        selectCCCDFrontBtn.setOnClickListener(v -> {
            currentImageType = CCCD_FRONT;
            showImagePickerDialog();
        });
        selectCCCDBackBtn.setOnClickListener(v -> {
            currentImageType = CCCD_BACK;
            showImagePickerDialog();
        });
        startDateEdt.setOnClickListener(v -> showDatePicker(startDateEdt));
        endDateEdt.setOnClickListener(v -> showDatePicker(endDateEdt));
        createContractBtn.setOnClickListener(v -> createContract());
        clearSearchBtn.setOnClickListener(v -> clearTenantSearch());
        tenantEmailEdt.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && foundTenantId != null) {
                Toast.makeText(this, "Nhấn nút X để xóa kết quả tìm kiếm hiện tại", Toast.LENGTH_SHORT).show();
            }
        });

        setupRoomSpinner();
    }

    private void searchTenantByEmail() {
        String email = tenantEmailEdt.getText().toString().trim();
        if (email.isEmpty()) {
            tenantEmailEdt.setError("Vui lòng nhập email người thuê");
            return;
        }

        searchTenantBtn.setEnabled(false);
        searchTenantBtn.setText("Đang tìm...");

        usersRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        searchTenantBtn.setEnabled(true);
                        searchTenantBtn.setText("Tìm");

                        if (snapshot.exists()) {
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                foundTenantId = userSnapshot.getKey();
                                String name = userSnapshot.child("name").getValue(String.class);
                                String phone = userSnapshot.child("phone").getValue(String.class);
                                String role = userSnapshot.child("role").getValue(String.class);

                                handleTenantDataFill(name, phone);

                                String message = "Tìm thấy: " + (name != null ? name : email);
                                Toast.makeText(AddContractActivity.this, message, Toast.LENGTH_LONG).show();
                                return;
                            }
                        } else {
                            resetTenantFields();
                            Toast.makeText(AddContractActivity.this,
                                    "Không tìm thấy người dùng với email này.\nCó thể nhập thông tin thủ công.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        searchTenantBtn.setEnabled(true);
                        searchTenantBtn.setText("Tìm");
                        Toast.makeText(AddContractActivity.this, "Lỗi tìm kiếm: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleTenantDataFill(String name, String phone) {
        if (name != null && !name.trim().isEmpty()) {
            tenantNameEdt.setText(name.trim());
            tenantNameEdt.setEnabled(false);
            tenantNameEdt.setAlpha(0.7f);
        } else {
            tenantNameEdt.setText("");
            tenantNameEdt.setEnabled(true);
            tenantNameEdt.setAlpha(1.0f);
        }

        if (phone != null && !phone.trim().isEmpty() && !phone.equals("Chưa cập nhật") && !phone.equals("null")) {
            tenantPhoneEdt.setText(phone.trim());
            tenantPhoneEdt.setEnabled(false);
            tenantPhoneEdt.setAlpha(0.7f);
        } else {
            tenantPhoneEdt.setText("");
            tenantPhoneEdt.setEnabled(true);
            tenantPhoneEdt.setAlpha(1.0f);
        }
    }

    private void resetTenantFields() {
        foundTenantId = null;
        tenantNameEdt.setText("");
        tenantNameEdt.setEnabled(true);
        tenantNameEdt.setAlpha(1.0f);
        tenantPhoneEdt.setText("");
        tenantPhoneEdt.setEnabled(true);
        tenantPhoneEdt.setAlpha(1.0f);
    }

    private void clearTenantSearch() {
        tenantEmailEdt.setText("");
        resetTenantFields();
        Toast.makeText(this, "Đã xóa thông tin tìm kiếm. Có thể nhập thủ công.", Toast.LENGTH_SHORT).show();
    }

    private void showImagePickerDialog() {
        imagePickerDialog = new BottomSheetDialog(this);
        imagePickerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        imagePickerDialog.setContentView(R.layout.dialog_choose_uploading);

        LinearLayout pickImgAlbum = imagePickerDialog.findViewById(R.id.pickImgAlbum);
        LinearLayout pickImgCamera = imagePickerDialog.findViewById(R.id.pickImgCamera);
        ImageView cancelButton = imagePickerDialog.findViewById(R.id.cancelButton);

        pickImgAlbum.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_CODE);
                    return;
                }
            } else {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_CODE);
                    return;
                }
            }

            Intent photoPicker = new Intent(Intent.ACTION_PICK);
            photoPicker.setType("image/*");
            photoPicker.setAction(Intent.ACTION_GET_CONTENT);
            galleryLauncher.launch(photoPicker);
        });

        pickImgCamera.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_CODE);
                return;
            }
            try {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraLauncher.launch(cameraIntent);
            } catch (Exception e) {
                Log.e("Camera", "Error launching camera", e);
                Toast.makeText(this, "Error launching camera", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> imagePickerDialog.dismiss());

        imagePickerDialog.show();
        imagePickerDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        imagePickerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        imagePickerDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        imagePickerDialog.getWindow().setGravity(Gravity.BOTTOM);
        imagePickerDialog.setCancelable(true);
    }

    private void addImageToCurrentType(Uri imageUri) {
        if (CCCD_FRONT.equals(currentImageType)) {
            // Chỉ cho phép 1 ảnh mặt trước
            cccdFrontImages.clear();
            cccdFrontImages.add(imageUri);
            cccdFrontAdapter.notifyDataSetChanged();
            cccdFrontRecyclerView.setVisibility(View.VISIBLE);

        } else if (CCCD_BACK.equals(currentImageType)) {
            // Chỉ cho phép 1 ảnh mặt sau
            cccdBackImages.clear();
            cccdBackImages.add(imageUri);
            cccdBackAdapter.notifyDataSetChanged();
            cccdBackRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private Uri saveImageToCache(Bitmap bitmap) {
        FileOutputStream fos = null;
        try {
            File cacheDir = getCacheDir();
            File imageFile = new File(cacheDir, "cccd_" + currentImageType + "_" + System.currentTimeMillis() + ".jpg");
            fos = new FileOutputStream(imageFile);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            return Uri.fromFile(imageFile);

        } catch (IOException e) {
            return null;
        } finally {
            try {
                if (fos != null) fos.close();
            } catch (IOException e) {
            }
        }
    }

    private Uri copyImageToCache(Uri sourceUri) {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = getContentResolver().openInputStream(sourceUri);
            if (input == null) return null;

            File cacheDir = getCacheDir();
            File outputFile = new File(cacheDir, "cccd_" + currentImageType + "_" + System.currentTimeMillis() + ".jpg");
            output = new FileOutputStream(outputFile);

            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }

            output.flush();
            return Uri.fromFile(outputFile);

        } catch (Exception e) {
            return sourceUri;
        } finally {
            try {
                if (output != null) output.close();
                if (input != null) input.close();
            } catch (IOException e) {
            }
        }
    }

    private boolean validateInputs() {
        if (selectedRoom == null) {
            Toast.makeText(this, "Vui lòng chọn phòng cho thuê", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (landlordNameEdt.getText().toString().trim().isEmpty()) {
            landlordNameEdt.setError("Vui lòng nhập tên chủ trọ");
            return false;
        }

        if (landlordPhoneEdt.getText().toString().trim().isEmpty()) {
            landlordPhoneEdt.setError("Vui lòng nhập số điện thoại chủ trọ");
            return false;
        }

        if (tenantNameEdt.getText().toString().trim().isEmpty()) {
            tenantNameEdt.setError("Vui lòng nhập tên người thuê");
            return false;
        }

        if (tenantPhoneEdt.getText().toString().trim().isEmpty()) {
            tenantPhoneEdt.setError("Vui lòng nhập số điện thoại người thuê");
            return false;
        }

        if (tenantCCCDEdt.getText().toString().trim().isEmpty()) {
            tenantCCCDEdt.setError("Vui lòng nhập số CCCD");
            return false;
        }

        if (startDateEdt.getText().toString().trim().isEmpty()) {
            startDateEdt.setError("Vui lòng chọn ngày bắt đầu");
            return false;
        }

        if (endDateEdt.getText().toString().trim().isEmpty()) {
            endDateEdt.setError("Vui lòng chọn ngày kết thúc");
            return false;
        }

        if (cccdFrontImages.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ảnh CCCD mặt trước", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (cccdBackImages.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ảnh CCCD mặt sau", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void uploadCCCDImages() {
        contractId = UUID.randomUUID().toString();

        Uri frontUri = cccdFrontImages.get(0);
        Uri backUri = cccdBackImages.get(0);

        StorageReference frontRef = storageRef.child("ContractCCCD/" + contractId + "/front.jpg");
        frontRef.putFile(frontUri)
                .addOnSuccessListener(taskSnapshot -> {
                    frontRef.getDownloadUrl().addOnSuccessListener(frontUrl -> {
                        StorageReference backRef = storageRef.child("ContractCCCD/" + contractId + "/back.jpg");
                        backRef.putFile(backUri)
                                .addOnSuccessListener(taskSnapshot2 -> {
                                    backRef.getDownloadUrl().addOnSuccessListener(backUrl -> {
                                        saveContractToDatabase(contractId, frontUrl.toString(), backUrl.toString());
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    createContractBtn.setEnabled(true);
                                    Toast.makeText(this, "Lỗi upload ảnh mặt sau: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    createContractBtn.setEnabled(true);
                    Toast.makeText(this, "Lỗi upload ảnh mặt trước: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupRoomSpinner() {
        roomsList = new ArrayList<>();
        ArrayList<String> roomTitles = new ArrayList<>();
        roomTitles.add("Chọn phòng cho thuê");

        roomAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roomTitles);
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roomSpinner.setAdapter(roomAdapter);

        roomSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && position <= roomsList.size()) {
                    selectedRoom = roomsList.get(position - 1);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadUserInfo() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            usersRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String phone = snapshot.child("phone").getValue(String.class);

                        if (name != null) landlordNameEdt.setText(name);
                        if (phone != null && !phone.equals("Chưa cập nhật")) {
                            landlordPhoneEdt.setText(phone);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void loadAvailableRooms() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;

        postsRef.orderByChild("id_own_post")
                .equalTo(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        roomsList.clear();
                        ArrayList<String> roomTitles = new ArrayList<>();
                        roomTitles.add("Chọn phòng cho thuê");

                        for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                            Room room = roomSnapshot.getValue(Room.class);
                            if (room != null && room.getStatus_room() == 0) {
                                roomsList.add(room);
                                String displayText = room.getTitle_room() + " - " +
                                        room.getAddress().getAddress_combine();
                                roomTitles.add(displayText);
                            }
                        }

                        roomAdapter = new ArrayAdapter<>(AddContractActivity.this,
                                android.R.layout.simple_spinner_item, roomTitles);
                        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        roomSpinner.setAdapter(roomAdapter);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AddContractActivity.this,
                                "Lỗi tải danh sách phòng", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showDatePicker(EditText dateEditText) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    dateEditText.setText(sdf.format(selectedDate.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void createContract() {
        if (!validateInputs()) return;

        progressBar.setVisibility(View.VISIBLE);
        createContractBtn.setEnabled(false);

        uploadCCCDImages();
    }

    private void saveContractToDatabase(String contractId, String frontImageUrl, String backImageUrl) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;

        // ✅ Sử dụng constructor mới với tenantId
        Contract contract = new Contract(
                contractId,                                        // contractId
                selectedRoom.getId_room(),                         // roomId
                currentUser.getUid(),                             // landlordId
                landlordNameEdt.getText().toString().trim(),      // landlordName
                landlordPhoneEdt.getText().toString().trim(),     // landlordPhone
                foundTenantId,                                    // tenantId (có thể null)
                tenantNameEdt.getText().toString().trim(),        // tenantName
                tenantPhoneEdt.getText().toString().trim(),       // tenantPhone
                tenantCCCDEdt.getText().toString().trim(),        // tenantCCCD
                frontImageUrl,                                    // cccdFrontImage
                backImageUrl,                                     // cccdBackImage
                startDateEdt.getText().toString().trim(),         // startDate
                endDateEdt.getText().toString().trim(),           // endDate
                0,                                                // status (0: Nháp)
                System.currentTimeMillis()                        // createdAt
        );

        contractsRef.child(contractId).setValue(contract)
                .addOnSuccessListener(aVoid -> updateRoomStatus())
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    createContractBtn.setEnabled(true);
                    Toast.makeText(this, "Lỗi tạo hợp đồng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateRoomStatus() {
        postsRef.child(selectedRoom.getId_room())
                .child("status_room")
                .setValue(1)
                .addOnSuccessListener(aVoid -> {
                    contractsRef.child(contractId).child("status").setValue(1)
                            .addOnSuccessListener(unused -> {
                                progressBar.setVisibility(View.GONE);
                                createContractBtn.setEnabled(true);
                                Toast.makeText(this, "Tạo hợp đồng thành công!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                createContractBtn.setEnabled(true);
                                Toast.makeText(this, "Hợp đồng đã tạo nhưng không thể cập nhật trạng thái", 
                                        Toast.LENGTH_LONG).show();
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    createContractBtn.setEnabled(true);
                    Toast.makeText(this, "Hợp đồng đã tạo nhưng không thể cập nhật trạng thái phòng",
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Quyền đã được cấp, vui lòng thử lại", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cần cấp quyền để chọn ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }
}