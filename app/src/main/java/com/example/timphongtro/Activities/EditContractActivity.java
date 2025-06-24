package com.example.timphongtro.Activities;

import android.Manifest;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
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
import com.example.timphongtro.Utils.ContractUtils;
import com.example.timphongtro.Utils.GsonUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
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
import java.util.Date;
import java.util.Locale;

public class EditContractActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE = 1001;
    private static final String CCCD_FRONT = "front";
    private static final String CCCD_BACK = "back";

    private ImageView backButton;
    private TextView contractInfoTv;
    private Spinner roomSpinner;
    private EditText landlordNameEdt, landlordPhoneEdt;
    private EditText tenantNameEdt, tenantPhoneEdt, tenantCCCDEdt;
    private EditText startDateEdt, endDateEdt;
    private RecyclerView cccdFrontRecyclerView, cccdBackRecyclerView;
    private MaterialButton selectCCCDFrontBtn, selectCCCDBackBtn;
    private MaterialButton updateContractBtn;
    private ProgressBar progressBar;
    private Contract contract;
    private Room selectedRoom;
    private ArrayList<Room> roomsList;
    private ArrayAdapter<String> roomAdapter;
    private ArrayList<Uri> cccdFrontImages, cccdBackImages;
    private ImageAdapter cccdFrontAdapter, cccdBackAdapter;
    private String currentImageType;
    private DatabaseReference contractsRef, roomsRef;
    private StorageReference storageRef;
    private FirebaseAuth firebaseAuth;
    private ActivityResultLauncher<Intent> galleryLauncher, cameraLauncher;
    private BottomSheetDialog imagePickerDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_contract);

        initializeViews();
        initializeFirebase();
        setupRecyclerViews();
        setupActivityResultLaunchers();
        getContractData();
        setupListeners();
        loadRooms();
        displayContractInfo();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.back_button);
        contractInfoTv = findViewById(R.id.contract_info_tv);
        roomSpinner = findViewById(R.id.room_spinner);
        landlordNameEdt = findViewById(R.id.landlord_name_edt);
        landlordPhoneEdt = findViewById(R.id.landlord_phone_edt);
        tenantNameEdt = findViewById(R.id.tenant_name_edt);
        tenantPhoneEdt = findViewById(R.id.tenant_phone_edt);
        tenantCCCDEdt = findViewById(R.id.tenant_cccd_edt);
        startDateEdt = findViewById(R.id.start_date_edt);
        endDateEdt = findViewById(R.id.end_date_edt);
        cccdFrontRecyclerView = findViewById(R.id.cccd_front_recycler_view);
        cccdBackRecyclerView = findViewById(R.id.cccd_back_recycler_view);
        selectCCCDFrontBtn = findViewById(R.id.add_cccd_front_btn);
        selectCCCDBackBtn = findViewById(R.id.add_cccd_back_btn);
        updateContractBtn = findViewById(R.id.update_contract_btn);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        contractsRef = FirebaseDatabase.getInstance().getReference("Contracts");
        roomsRef = FirebaseDatabase.getInstance().getReference("Posts");
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

    private void getContractData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("contractJson")) {
            String contractJson = intent.getStringExtra("contractJson");
            contract = GsonUtils.fromJson(contractJson, Contract.class);
            
            if (contract == null) {
                Toast.makeText(this, "Lỗi tải thông tin hợp đồng", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin hợp đồng", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
        startDateEdt.setOnClickListener(v -> showDatePicker(startDateEdt));
        endDateEdt.setOnClickListener(v -> showDatePicker(endDateEdt));
        updateContractBtn.setOnClickListener(v -> updateContract());

        selectCCCDFrontBtn.setOnClickListener(v -> {
            currentImageType = CCCD_FRONT;
            showImagePickerDialog();
        });

        selectCCCDBackBtn.setOnClickListener(v -> {
            currentImageType = CCCD_BACK;
            showImagePickerDialog();
        });

        setupRoomSpinner();
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

    private void loadRooms() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;

        ArrayList<String> roomTitles = new ArrayList<>();
        roomTitles.add("Chọn phòng cho thuê");

        roomsRef.orderByChild("id_own_post")
                .equalTo(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        roomsList.clear();
                        roomTitles.clear();
                        roomTitles.add("Chọn phòng cho thuê");

                        for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                            Room room = roomSnapshot.getValue(Room.class);
                            if (room != null) {
                                roomsList.add(room);
                                String displayText = room.getTitle_room() + " - " +
                                        room.getAddress().getAddress_combine();
                                roomTitles.add(displayText);

                                // Select current contract room
                                if (contract != null && room.getId_room().equals(contract.getRoomId())) {
                                    selectedRoom = room;
                                }
                            }
                        }

                        roomAdapter = new ArrayAdapter<>(EditContractActivity.this,
                                android.R.layout.simple_spinner_item, roomTitles);
                        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        roomSpinner.setAdapter(roomAdapter);

                        // Set spinner selection to current contract room
                        if (selectedRoom != null) {
                            int position = roomsList.indexOf(selectedRoom) + 1;
                            roomSpinner.setSelection(position);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(EditContractActivity.this,
                                "Lỗi tải danh sách phòng: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayContractInfo() {
        if (contract == null) return;

        ContractUtils.syncContractStatusWithDatabase(contract);

        int currentStatus = ContractUtils.getCurrentStatus(contract);
        String statusInfo = "";
        
        switch (currentStatus) {
            case 0:
                statusInfo = "Hợp đồng này đang ở trạng thái nháp. Bạn có thể chỉnh sửa và kích hoạt.";
                break;
            case 1:
                statusInfo = "Hợp đồng này đang hiệu lực. Bạn có thể gia hạn hoặc cập nhật thông tin.";
                break;
            case 2:
                statusInfo = "Hợp đồng này đã hết hạn. Bạn có thể gia hạn hoặc cập nhật thông tin để tái kích hoạt.";
                break;
        }
        contractInfoTv.setText(statusInfo);
        landlordNameEdt.setText(contract.getLandlordName());
        landlordPhoneEdt.setText(contract.getLandlordPhone());
        tenantNameEdt.setText(contract.getTenantName());
        tenantPhoneEdt.setText(contract.getTenantPhone());
        tenantCCCDEdt.setText(contract.getTenantCCCD());
        startDateEdt.setText(formatTimestamp(contract.getStartDate()));
        endDateEdt.setText(formatTimestamp(contract.getEndDate()));

        loadExistingCCCDImages();
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp <= 0) return "Chưa xác định";
        
        try {
            Date date = new Date(timestamp);
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
            return formatter.format(date);
        } catch (Exception e) {
            return "Thời gian không hợp lệ";
        }
    }

    private void loadExistingCCCDImages() {
        // Load CCCD Front Image
        if (contract.getCccdFrontImage() != null && !contract.getCccdFrontImage().isEmpty()) {
            cccdFrontImages.add(Uri.parse(contract.getCccdFrontImage()));
            cccdFrontAdapter.notifyDataSetChanged();
            cccdFrontRecyclerView.setVisibility(View.VISIBLE);
        }

        // Load CCCD Back Image
        if (contract.getCccdBackImage() != null && !contract.getCccdBackImage().isEmpty()) {
            cccdBackImages.add(Uri.parse(contract.getCccdBackImage()));
            cccdBackAdapter.notifyDataSetChanged();
            cccdBackRecyclerView.setVisibility(View.VISIBLE);
        }
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

    private void showDatePicker(EditText dateEditText) {
        Calendar calendar = Calendar.getInstance();

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
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

    private void updateContract() {
        if (!validateInputs()) return;

        progressBar.setVisibility(View.VISIBLE);
        updateContractBtn.setEnabled(false);

        // Check if images need to be uploaded
        boolean needsImageUpload = hasNewImages();

        if (needsImageUpload) {
            uploadImagesAndUpdateContract();
        } else {
            updateContractData(contract.getCccdFrontImage(), contract.getCccdBackImage());
        }
    }

    // Check if images are new
    private boolean hasNewImages() {
        boolean frontChanged = false;
        boolean backChanged = false;

        if (!cccdFrontImages.isEmpty()) {
            String currentFrontUri = cccdFrontImages.get(0).toString();
            frontChanged = !currentFrontUri.equals(contract.getCccdFrontImage());
        }

        if (!cccdBackImages.isEmpty()) {
            String currentBackUri = cccdBackImages.get(0).toString();
            backChanged = !currentBackUri.equals(contract.getCccdBackImage());
        }

        return frontChanged || backChanged;
    }

    private void uploadImagesAndUpdateContract() {
        Uri frontUri = cccdFrontImages.isEmpty() ? null : cccdFrontImages.get(0);
        Uri backUri = cccdBackImages.isEmpty() ? null : cccdBackImages.get(0);

        String frontImageUrl = contract.getCccdFrontImage(); // Default to existing
        String backImageUrl = contract.getCccdBackImage();   // Default to existing

        // Upload front image if new
        if (frontUri != null && !frontUri.toString().equals(contract.getCccdFrontImage())) {
            StorageReference frontRef = storageRef.child("ContractCCCD/" + contract.getContractId() + "/front.jpg");
            frontRef.putFile(frontUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        frontRef.getDownloadUrl().addOnSuccessListener(frontUrl -> {
                            // Upload back image if new
                            if (backUri != null && !backUri.toString().equals(contract.getCccdBackImage())) {
                                StorageReference backRef = storageRef.child("ContractCCCD/" + contract.getContractId() + "/back.jpg");
                                backRef.putFile(backUri)
                                        .addOnSuccessListener(taskSnapshot2 -> {
                                            backRef.getDownloadUrl().addOnSuccessListener(backUrl -> {
                                                updateContractData(frontUrl.toString(), backUrl.toString());
                                            });
                                        })
                                        .addOnFailureListener(e -> {
                                            progressBar.setVisibility(View.GONE);
                                            updateContractBtn.setEnabled(true);
                                            Toast.makeText(this, "Lỗi upload ảnh mặt sau: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                updateContractData(frontUrl.toString(), backImageUrl);
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        updateContractBtn.setEnabled(true);
                        Toast.makeText(this, "Lỗi upload ảnh mặt trước: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else if (backUri != null && !backUri.toString().equals(contract.getCccdBackImage())) {
            // Only upload back image
            StorageReference backRef = storageRef.child("ContractCCCD/" + contract.getContractId() + "/back.jpg");
            backRef.putFile(backUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        backRef.getDownloadUrl().addOnSuccessListener(backUrl -> {
                            updateContractData(frontImageUrl, backUrl.toString());
                        });
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        updateContractBtn.setEnabled(true);
                        Toast.makeText(this, "Lỗi upload ảnh mặt sau: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            updateContractData(frontImageUrl, backImageUrl);
        }
    }

    private void updateContractData(String frontImageUrl, String backImageUrl) {
        long startTimestamp = parseDateToTimestamp(startDateEdt.getText().toString().trim());
        long endTimestamp = parseDateToTimestamp(endDateEdt.getText().toString().trim());

        contract.setLandlordName(landlordNameEdt.getText().toString().trim());
        contract.setLandlordPhone(landlordPhoneEdt.getText().toString().trim());
        contract.setTenantName(tenantNameEdt.getText().toString().trim());
        contract.setTenantPhone(tenantPhoneEdt.getText().toString().trim());
        contract.setTenantCCCD(tenantCCCDEdt.getText().toString().trim());
        contract.setStartDate(startTimestamp);
        contract.setEndDate(endTimestamp);
        contract.setStatus(1);
        contract.setCccdFrontImage(frontImageUrl);
        contract.setCccdBackImage(backImageUrl);

        if (selectedRoom != null) {
            contract.setRoomId(selectedRoom.getId_room());
        }

        contractsRef.child(contract.getContractId()).setValue(contract)
                .addOnSuccessListener(aVoid -> {
                    updateRoomStatusAfterUpdate();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    updateContractBtn.setEnabled(true);
                    Toast.makeText(this, "Lỗi cập nhật hợp đồng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateRoomStatusAfterUpdate() {
        if (selectedRoom == null) {
            finishUpdate();
            return;
        }

        DatabaseReference roomRef = FirebaseDatabase.getInstance()
                .getReference("Posts")
                .child(selectedRoom.getId_room())
                .child("status_room");

        roomRef.setValue(1)
                .addOnSuccessListener(aVoid -> finishUpdate())
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Hợp đồng đã cập nhật nhưng không thể cập nhật trạng thái phòng",
                            Toast.LENGTH_LONG).show();
                    finishUpdate();
                });
    }

    private void finishUpdate() {
        progressBar.setVisibility(View.GONE);
        updateContractBtn.setEnabled(true);
        Toast.makeText(this, "Cập nhật hợp đồng thành công!", Toast.LENGTH_SHORT).show();
        finish();
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

        long startTimestamp = parseDateToTimestamp(startDateEdt.getText().toString().trim());
        long endTimestamp = parseDateToTimestamp(endDateEdt.getText().toString().trim());

        if (startTimestamp >= endTimestamp) {
            Toast.makeText(this, "Ngày kết thúc phải sau ngày bắt đầu", Toast.LENGTH_SHORT).show();
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

    private long parseDateToTimestamp(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = sdf.parse(dateString);
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            return 0;
        }
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