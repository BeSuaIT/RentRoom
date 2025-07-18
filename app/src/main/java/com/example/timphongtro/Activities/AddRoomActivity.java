package com.example.timphongtro.Activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.timphongtro.Adapters.ImageAdapter;
import com.example.timphongtro.Models.Address;
import com.example.timphongtro.Models.Utility;
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.Models.Furniture;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddRoomActivity extends AppCompatActivity {
    private static final int PERMISSION_CODE = 1001;
    private FirebaseUser userCurrent;
    private FirebaseStorage storage;
    private DatabaseReference citiesRef;
    private EditText edtTitleRoom, edtDeposit, edtPrice, edtInternet, edtElectric, edtWater,
            edtArea, edtPhone, edtFloor, edtPerson, edtDescriptionRoom, edtPark, edtAddress;
    private RadioGroup radioGroup;
    private Spinner spinnerCity, spinnerDistrict;
    private CheckBox[] utilityCheckboxes, furnitureCheckboxes, genderCheckboxes;
    private List<String> cities, districts;
    private ArrayList<Furniture> furnitureArrayList;
    private ArrayList<Utility> utilityArrayList;
    private Address address;
    private ArrayList<Uri> selectedImages;
    private ArrayList<String> uploadedImageUrls;
    private RecyclerView recyclerViewImages;
    private ImageAdapter imageAdapter;
    private int uploadCount = 0;
    private BottomSheetDialog dialog;
    private LinearLayout pickImgAlbum, pickImgCamera;
    private ActivityResultLauncher<Intent> activityResultLauncher, cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        initializeFirebase();
        initializeViews();
        setupActivityResultLaunchers();
        setupCitySpinner();
    }

    private void initializeFirebase() {
        userCurrent = FirebaseAuth.getInstance().getCurrentUser();
        if (userCurrent == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        storage = FirebaseStorage.getInstance();
        citiesRef = FirebaseDatabase.getInstance().getReference("Cities");
    }

    private void initializeViews() {
        edtTitleRoom = this.findViewById(R.id.edtTitleRoom);
        edtPrice = this.findViewById(R.id.edtPrice);
        edtDeposit = this.findViewById(R.id.edtDeposit);
        edtInternet = this.findViewById(R.id.edtInternet);
        edtElectric = this.findViewById(R.id.edtElectric);
        edtWater = this.findViewById(R.id.edtWater);
        radioGroup = this.findViewById(R.id.radioGroupType);
        edtArea = this.findViewById(R.id.edtArea);
        edtPhone = this.findViewById(R.id.edtPhone);
        edtFloor = this.findViewById(R.id.edtFloor);
        edtPerson = this.findViewById(R.id.edtPerson);
        edtDescriptionRoom = this.findViewById(R.id.edtDescriptionRoom);
        edtPark = this.findViewById(R.id.edtPark);
        edtAddress = findViewById(R.id.edtAddress);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btn_create_room).setOnClickListener(v -> showConfirmationDialog());
        findViewById(R.id.btnAddImage).setOnClickListener(v -> showBottomDialog());

        spinnerCity = findViewById(R.id.spinnerCity);
        spinnerDistrict = findViewById(R.id.spinnerDistrict);

        selectedImages = new ArrayList<>();
        uploadedImageUrls = new ArrayList<>();
        recyclerViewImages = findViewById(R.id.recyclerViewImages);
        recyclerViewImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ImageAdapter(this, selectedImages);
        recyclerViewImages.setAdapter(imageAdapter);

        genderCheckboxes = new CheckBox[2];
        genderCheckboxes[0] = findViewById(R.id.checkboxNam);
        genderCheckboxes[1] = findViewById(R.id.checkboxNu);

        furnitureCheckboxes = new CheckBox[8];
        furnitureCheckboxes[0] = findViewById(R.id.checkbox_air_condition);
        furnitureCheckboxes[1] = findViewById(R.id.checkbox_heater);
        furnitureCheckboxes[2] = findViewById(R.id.checkbox_curtain);
        furnitureCheckboxes[3] = findViewById(R.id.checkboxfridge);
        furnitureCheckboxes[4] = findViewById(R.id.checkboxbed);
        furnitureCheckboxes[5] = findViewById(R.id.checkboxwardrobe);
        furnitureCheckboxes[6] = findViewById(R.id.checkbox_washing_machine);
        furnitureCheckboxes[7] = findViewById(R.id.checkboxsofa);

        utilityCheckboxes = new CheckBox[7];
        utilityCheckboxes[0] = findViewById(R.id.checkboxtoilet);
        utilityCheckboxes[1] = findViewById(R.id.checkboxfloor);
        utilityCheckboxes[2] = findViewById(R.id.checkbox_time_flex);
        utilityCheckboxes[3] = findViewById(R.id.checkboxfingerprint);
        utilityCheckboxes[4] = findViewById(R.id.checkboxbacony);
        utilityCheckboxes[5] = findViewById(R.id.checkboxpet);
        utilityCheckboxes[6] = findViewById(R.id.checkbox_w_owner);

        cities = new ArrayList<>();
        districts = new ArrayList<>();
    }

    private void setupCitySpinner() {
        Map<String, String> cityKeyMap = new HashMap<>();

        citiesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cities.clear();
                cityKeyMap.clear();

                for (DataSnapshot citySnapshot : snapshot.getChildren()) {
                    String cityName = citySnapshot.child("name").getValue(String.class);
                    String cityKey = citySnapshot.getKey();

                    DataSnapshot districtsSnapshot = citySnapshot.child("Districts");
                    if (cityName != null && districtsSnapshot.exists() && districtsSnapshot.getChildrenCount() > 0) {
                        cities.add(cityName);
                        cityKeyMap.put(cityName, cityKey);
                    }
                }

                ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(
                        AddRoomActivity.this,
                        android.R.layout.simple_spinner_item,
                        cities
                );
                cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCity.setAdapter(cityAdapter);

                int defaultPosition = cities.indexOf("Hà Nội");
                if (defaultPosition != -1) {
                    spinnerCity.setSelection(defaultPosition);
                }

                spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String selectedCityName = cities.get(position);
                        String selectedCityKey = cityKeyMap.get(selectedCityName);
                        loadDistrictsForCity(selectedCityKey);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddRoomActivity.this, "Failed to load cities", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDistrictsForCity(String cityKey) {
        DatabaseReference districtRef = FirebaseDatabase.getInstance().getReference("Cities")
                .child(cityKey)
                .child("Districts");

        districtRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                districts.clear();
                for (DataSnapshot districtSnapshot : snapshot.getChildren()) {
                    String districtName = districtSnapshot.child("name").getValue(String.class);
                    if (districtName != null) {
                        districts.add(districtName);
                    }
                }

                ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(
                        AddRoomActivity.this,
                        android.R.layout.simple_spinner_item,
                        districts
                );
                districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDistrict.setAdapter(districtAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddRoomActivity.this, "Failed to load districts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupActivityResultLaunchers() {
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            if (data.getClipData() != null) {
                                int count = data.getClipData().getItemCount();
                                for (int i = 0; i < count; i++) {
                                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                                    Uri cachedUri = copyImageToCache(imageUri);
                                    selectedImages.add(cachedUri);
                                }
                            } else if (data.getData() != null) {
                                Uri imageUri = data.getData();
                                Uri cachedUri = copyImageToCache(imageUri);
                                selectedImages.add(cachedUri);
                            }
                            imageAdapter.notifyDataSetChanged();
                        }
                        dialog.dismiss();
                    }
                });

        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), 
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    Uri imageUri = saveImageToCache(imageBitmap);
                    selectedImages.add(imageUri);
                    imageAdapter.notifyDataSetChanged();
                    dialog.dismiss();
                }
            });
    }

    private Uri saveImageToCache(Bitmap bitmap) {
        try {
            File cacheDir = getCacheDir();
            File imageFile = new File(cacheDir, "image_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            return Uri.fromFile(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Uri copyImageToCache(Uri sourceUri) {
        try {
            InputStream input = getContentResolver().openInputStream(sourceUri);
            File cacheDir = getCacheDir();
            File outputFile = new File(cacheDir, "temp_image_" + System.currentTimeMillis() + ".jpg");
            OutputStream output = new FileOutputStream(outputFile);

            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }

            output.flush();
            output.close();
            input.close();

            return Uri.fromFile(outputFile);
        } catch (Exception e) {
            return sourceUri;
        }
    }

    private void uploadImages(AlertDialog progressDialog) {
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadCount = 0;
        uploadedImageUrls.clear();

        for (Uri imageUri : selectedImages) {
            String fileName = "room_" + System.currentTimeMillis() + "_" + uploadCount + ".jpg";
            StorageReference imageRef = storage.getReference()
                    .child("RoomImages")
                    .child(fileName);

            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            uploadedImageUrls.add(uri.toString());
                            uploadCount++;

                            if (uploadCount == selectedImages.size()) {
                                onClickPushData();
                                progressDialog.dismiss();
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddRoomActivity.this,
                                "Lỗi khi tải ảnh lên", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    });
        }
    }

    private void onClickPushData() {
        if (!validateInputs()) return;

        Room room = createRoomObject();
        uploadRoomToFirebase(room);
    }

    private Room createRoomObject() {
        String id_room = generateSimpleRoomId();
        String city = spinnerCity.getSelectedItem().toString();
        String district = spinnerDistrict.getSelectedItem().toString();
        String detail = edtAddress.getText().toString();
        int status_room = 0;
        String id_own_post = userCurrent.getUid();

        address = new Address(city, district, detail);
        String gender_room;
        if (genderCheckboxes[0].isChecked() && genderCheckboxes[1].isChecked()) {
            gender_room = "Nam/Nữ";
        } else if (genderCheckboxes[0].isChecked()) {
            gender_room = "Nam";
        } else if (genderCheckboxes[1].isChecked()) {
            gender_room = "Nữ";
        } else {
            gender_room = "Nam/Nữ";
        }

        String title_room = edtTitleRoom.getText().toString();
        long deposit_room = Long.parseLong(edtDeposit.getText().toString());
        long price_room = Long.parseLong(edtPrice.getText().toString());

        String type_room = "Trọ";
        if (radioGroup.getCheckedRadioButtonId() == R.id.radiobtnChungCu) {
            type_room = "Chung cư Mini";
        }

        int area_room = Integer.parseInt(edtArea.getText().toString());
        String phone = edtPhone.getText().toString();
        int floor = Integer.parseInt(edtFloor.getText().toString());
        int person_in_room = Integer.parseInt(edtPerson.getText().toString());
        String description_room = edtDescriptionRoom.getText().toString();
        int park_slot = Integer.parseInt(edtPark.getText().toString());

        furnitureArrayList = new ArrayList<>();
        handleDataFurniture();

        utilityArrayList = new ArrayList<>();
        handleDataExtensions();

        return new Room(id_own_post, id_room, title_room, price_room, address, area_room,
                deposit_room, description_room, gender_room, park_slot,
                person_in_room, status_room, type_room, phone, floor, uploadedImageUrls,
                furnitureArrayList, utilityArrayList,
                Long.parseLong(edtElectric.getText().toString()),
                Long.parseLong(edtWater.getText().toString()),
                Long.parseLong(edtInternet.getText().toString()));
    }

    private String generateSimpleRoomId() {
        // Format: ROOM_YYYYMMDD_HHMMSS_XXX
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        
        // Random 3 digits để tránh conflict
        int randomSuffix = (int)(Math.random() * 900) + 100; // 100-999
        
        return String.format("ROOM_%s_%03d", timestamp, randomSuffix);
    }

    private void uploadRoomToFirebase(Room room) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("Rooms");

        Map<String, Object> roomMap = new HashMap<>();
        roomMap.put("ownerID", room.getOwnerID());
        roomMap.put("roomID", room.getRoomID());
        roomMap.put("roomTitle", room.getRoomTitle());
        roomMap.put("roomPrice", room.getRoomPrice());
        roomMap.put("roomDeposit", room.getRoomDeposit());
        roomMap.put("roomSize", room.getRoomSize());
        roomMap.put("description", room.getDescription());
        roomMap.put("gender", room.getGender());
        roomMap.put("park_slot", room.getpark_slot());
        roomMap.put("people_in_room", room.getpeople_in_room());
        roomMap.put("roomStatus", room.getRoomStatus());
        roomMap.put("roomType", room.getRoomType());
        roomMap.put("phone", room.getPhone());
        roomMap.put("floor", room.getFloor());
        roomMap.put("electricPrice", room.getElectricPrice());
        roomMap.put("waterPrice", room.getWaterPrice());
        roomMap.put("internetPrice", room.getInternetPrice());
        roomMap.put("address", room.getAddress());
        roomMap.put("Furniture", room.getFurniture());
        roomMap.put("Utilities", room.getUtilities());
        roomMap.put("images", room.getImages());
        roomMap.put("timestamp", System.currentTimeMillis());

        myRef.child(room.getRoomID()).setValue(roomMap)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(AddRoomActivity.this, "Đăng thông tin phòng thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(AddRoomActivity.this, "Đăng thông tin phòng thất bại", Toast.LENGTH_SHORT).show()
                );
    }

    private void handleDataFurniture() {
        if (furnitureCheckboxes[0].isChecked()) {
            furnitureArrayList.add(new Furniture("checkbox_air_condition", furnitureCheckboxes[0].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-air-condittion.png?alt=media&token=20e32d92-eda5-40bc-9b80-70ce0353d545"));
        }
        if (furnitureCheckboxes[1].isChecked()) {
            furnitureArrayList.add(new Furniture("checkbox_heater", furnitureCheckboxes[1].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-heater.png?alt=media&token=aa5b60e5-5230-48e8-9d2f-a91b8e8f90cf"));
        }
        if (furnitureCheckboxes[2].isChecked()) {
            furnitureArrayList.add(new Furniture("checkbox_curtain", furnitureCheckboxes[2].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-curtain.png?alt=media&token=fdadffc2-621d-47c8-81e8-984361cb32e7"));
        }
        if (furnitureCheckboxes[3].isChecked()) {
            furnitureArrayList.add(new Furniture("checkboxfridge", furnitureCheckboxes[3].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-fridge.png?alt=media&token=0ef07211-32cb-4336-8ee4-e4da0f94b37d"));
        }
        if (furnitureCheckboxes[4].isChecked()) {
            furnitureArrayList.add(new Furniture("checkboxbed", furnitureCheckboxes[4].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-bed.png?alt=media&token=f941b225-29ef-4de4-a4e4-2a5fc4316e0c"));
        }
        if (furnitureCheckboxes[5].isChecked()) {
            furnitureArrayList.add(new Furniture("checkboxwardrobe", furnitureCheckboxes[5].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-Wardrobe.png?alt=media&token=7f20f8ef-03bd-475f-a822-7e08d8129bba"));
        }
        if (furnitureCheckboxes[6].isChecked()) {
            furnitureArrayList.add(new Furniture("checkbox_washing_machine", furnitureCheckboxes[6].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-washing-machine.png?alt=media&token=a755fb42-789a-4791-a520-d7e890e4f1a9"));
        }
        if (furnitureCheckboxes[7].isChecked()) {
            furnitureArrayList.add(new Furniture("checkboxsofa", furnitureCheckboxes[7].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-sofa.png?alt=media&token=2f25df7d-7466-467f-88c6-aaae6e9fc570"));
        }
    }

    private void handleDataExtensions() {
        if (utilityCheckboxes[0].isChecked()) {
            utilityArrayList.add(new Utility("checkboxtoilet", utilityCheckboxes[0].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-toilet.png?alt=media&token=29ad52f7-37ec-44bf-aa01-a6d9dd7ec267"));
        }
        if (utilityCheckboxes[1].isChecked()) {
            utilityArrayList.add(new Utility("checkboxfloor", utilityCheckboxes[1].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-ladder.png?alt=media&token=fd039fdb-4a30-4f72-9821-dd38a5a39496"));
        }
        if (utilityCheckboxes[2].isChecked()) {
            utilityArrayList.add(new Utility("checkbox_time_flex", utilityCheckboxes[2].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-time-flex.png?alt=media&token=a6514c79-8e24-45cc-b685-c3d7c3970b15"));
        }
        if (utilityCheckboxes[3].isChecked()) {
            utilityArrayList.add(new Utility("checkboxfingerprint", utilityCheckboxes[3].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-finger-print.png?alt=media&token=709f1ae4-fe30-40f3-8b55-f0d145758ae6"));
        }
        if (utilityCheckboxes[4].isChecked()) {
            utilityArrayList.add(new Utility("checkboxbacony", utilityCheckboxes[4].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-ladder.png?alt=media&token=fd039fdb-4a30-4f72-9821-dd38a5a39496"));
        }
        if (utilityCheckboxes[5].isChecked()) {
            utilityArrayList.add(new Utility("checkboxpet", utilityCheckboxes[5].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-paw-pet.png?alt=media&token=33993aff-4371-4f12-b8e9-6f155bb22d9e"));
        }
        if (utilityCheckboxes[6].isChecked()) {
            utilityArrayList.add(new Utility("checkbox_w_owner", utilityCheckboxes[6].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-user.png?alt=media&token=21a5ecdf-8efd-4af5-9e73-ae9d5be0da1d"));
        }
    }

    private boolean validateInputs() {
        return validateBasicInputs();
    }

    private boolean validateBasicInputs() {
        boolean isValid = true;

        if (isEmpty(edtTitleRoom)) {
            edtTitleRoom.setError("Vui lòng nhập tiêu đề bài đăng");
            isValid = false;
        }

        if (isEmpty(edtPrice)) {
            edtPrice.setError("Vui lòng nhập giá phòng");
            isValid = false;
        } else {
            try {
                long price = Long.parseLong(edtPrice.getText().toString());
                if (price <= 0) {
                    edtPrice.setError("Giá phòng phải lớn hơn 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                edtPrice.setError("Vui lòng nhập số hợp lệ cho giá phòng");
                isValid = false;
            }
        }

        if (isEmpty(edtDeposit)) {
            edtDeposit.setError("Vui lòng nhập tiền cọc");
            isValid = false;
        } else {
            try {
                long deposit = Long.parseLong(edtDeposit.getText().toString());
                if (deposit < 0) {
                    edtDeposit.setError("Tiền cọc không được âm");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                edtDeposit.setError("Vui lòng nhập số hợp lệ cho tiền cọc");
                isValid = false;
            }
        }

        if (radioGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Vui lòng chọn loại phòng", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (isEmpty(edtArea)) {
            edtArea.setError("Vui lòng nhập diện tích");
            isValid = false;
        }

        if (isEmpty(edtPhone)) {
            edtPhone.setError("Vui lòng nhập số điện thoại");
            isValid = false;
        } else {
            String phone = edtPhone.getText().toString().trim();
            if (!phone.matches("^\\d{10}$")) {
                edtPhone.setError("Số điện thoại phải có 10 chữ số");
                isValid = false;
            }
        }

        if (isEmpty(edtFloor)) {
            edtFloor.setError("Vui lòng nhập số tầng");
            isValid = false;
        } else {
            try {
                int floor = Integer.parseInt(edtFloor.getText().toString());
                if (floor <= 0) {
                    edtFloor.setError("Tầng phải lớn hơn 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                edtFloor.setError("Vui lòng nhập số hợp lệ cho tầng");
                isValid = false;
            }
        }

        if (isEmpty(edtPerson)) {
            edtPerson.setError("Vui lòng nhập số người/phòng");
            isValid = false;
        } else {
            try {
                int person = Integer.parseInt(edtPerson.getText().toString());
                if (person <= 0) {
                    edtPerson.setError("Số người phải lớn hơn 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                edtPerson.setError("Vui lòng nhập số hợp lệ cho số người");
                isValid = false;
            }
        }

        if (isEmpty(edtDescriptionRoom)) {
            edtDescriptionRoom.setError("Vui lòng nhập mô tả phòng chi tiết");
            isValid = false;
        } else {
            String description = edtDescriptionRoom.getText().toString().trim();
            if (description.length() < 10) {
                edtDescriptionRoom.setError("Mô tả phòng phải ít nhất 10 ký tự");
                isValid = false;
            }
        }

        if (isEmpty(edtPark)) {
            edtPark.setError("Vui lòng nhập số chỗ để xe trong 1 phòng");
            isValid = false;
        } else {
            try {
                int park = Integer.parseInt(edtPark.getText().toString());
                if (park < 0) {
                    edtPark.setError("Số chỗ để xe không được âm");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                edtPark.setError("Vui lòng nhập số hợp lệ cho chỗ để xe");
                isValid = false;
            }
        }

        if (spinnerCity.getSelectedItemPosition() == -1 ||
                spinnerCity.getSelectedItem() == null ||
                spinnerCity.getSelectedItem().toString().trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn thành phố", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (spinnerDistrict.getSelectedItemPosition() == -1 ||
                spinnerDistrict.getSelectedItem() == null ||
                spinnerDistrict.getSelectedItem().toString().trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn quận/huyện", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        String addressDetail = edtAddress.getText().toString().trim();
        if (!addressDetail.isEmpty() && addressDetail.length() < 5) {
            edtAddress.setError("Địa chỉ chi tiết phải ít nhất 5 ký tự");
            isValid = false;
        }

        if (!genderCheckboxes[0].isChecked() && !genderCheckboxes[1].isChecked()) {
            Toast.makeText(this, "Vui lòng chọn giới tính", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (isEmpty(edtElectric)) {
            edtElectric.setError("Vui lòng nhập giá điện");
            isValid = false;
        } else {
            try {
                long electric = Long.parseLong(edtElectric.getText().toString());
                if (electric < 0) {
                    edtElectric.setError("Giá điện không được âm");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                edtElectric.setError("Vui lòng nhập số hợp lệ cho giá điện");
                isValid = false;
            }
        }

        if (isEmpty(edtWater)) {
            edtWater.setError("Vui lòng nhập giá nước");
            isValid = false;
        } else {
            try {
                long water = Long.parseLong(edtWater.getText().toString());
                if (water < 0) {
                    edtWater.setError("Giá nước không được âm");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                edtWater.setError("Vui lòng nhập số hợp lệ cho giá nước");
                isValid = false;
            }
        }

        if (isEmpty(edtInternet)) {
            edtInternet.setError("Vui lòng nhập giá Internet");
            isValid = false;
        } else {
            try {
                long internet = Long.parseLong(edtInternet.getText().toString());
                if (internet < 0) {
                    edtInternet.setError("Giá Internet không được âm");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                edtInternet.setError("Vui lòng nhập số hợp lệ cho giá Internet");
                isValid = false;
            }
        }

        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 ảnh", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void validateTitleAndProceed() {
        String title = edtTitleRoom.getText().toString().trim();

        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setView(R.layout.progress_layout)
                .setCancelable(false)
                .create();
        progressDialog.show();

        DatabaseReference roomsRef = FirebaseDatabase.getInstance().getReference("Rooms");
        roomsRef.orderByChild("roomTitle").equalTo(title)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean isDuplicate = false;

                        for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                            String ownerID = roomSnapshot.child("ownerID").getValue(String.class);
                            if (ownerID != null && !ownerID.equals(userCurrent.getUid())) {
                                isDuplicate = true;
                                break;
                            }
                        }

                        if (isDuplicate) {
                            progressDialog.dismiss();
                            edtTitleRoom.setError("Tiêu đề này đã tồn tại, vui lòng chọn tiêu đề khác");
                            edtTitleRoom.requestFocus();
                        } else {
                            uploadImages(progressDialog);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressDialog.dismiss();
                        Toast.makeText(AddRoomActivity.this, "Lỗi kiểm tra dữ liệu", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isEmpty(EditText text) {
        return TextUtils.isEmpty(text.getText().toString());
    }

    private void showBottomDialog() {
        dialog = new BottomSheetDialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_choose_uploading);

        ImageView cancelButton;
        pickImgAlbum = dialog.findViewById(R.id.pickImgAlbum);
        pickImgCamera = dialog.findViewById(R.id.pickImgCamera);
        cancelButton = dialog.findViewById(R.id.cancelButton);

        pickImgAlbum.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_CODE);
                }
            } else {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_CODE);
                }
            }

            Intent photoPicker = new Intent(Intent.ACTION_PICK);
            photoPicker.setType("image/*");
            photoPicker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            photoPicker.setAction(Intent.ACTION_GET_CONTENT);
            activityResultLauncher.launch(photoPicker);
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

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.setCancelable(true);
    }

    private void showConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận")
                .setMessage("Bạn có muốn đăng bài không?")
                .setPositiveButton("Có", (dialog, which) -> {
                    if (validateBasicInputs()) {
                        validateTitleAndProceed();
                    }
                })
                .setNegativeButton("Không", null)
                .show();
    }
}