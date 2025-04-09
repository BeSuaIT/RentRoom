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
import android.widget.Button;
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
import com.example.timphongtro.Models.ImagesRoomClass;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostRoomActivity extends AppCompatActivity {
    // Constants
    private static final int PERMISSION_CODE = 1001;

    // Firebase instances
    private FirebaseUser userCurrent;
    private FirebaseStorage storage;
    private DatabaseReference citiesRef;

    // UI Components
    private ImageView btnBack;
    private EditText edtTitleRoom, edtDeposit, edtPrice, edtInternet, edtElectric, edtWater,
            edtArea, edtPhone, edtFloor, edtPerson, edtDescriptionRoom, edtPark, edtAddress;
    private Button btn_create_room;
    private RadioGroup radioGroup;
    private Spinner spinnerCity, spinnerDistrict;
    private CheckBox[] utilityCheckboxes, furnitureCheckboxes, genderCheckboxes;

    // Data holders
    private List<String> cities, districts;
    private ArrayList<Furniture> furnitures;
    private ArrayList<Utility> extensions_room;
    private Address address;
    private ArrayList<Uri> selectedImages;
    private ArrayList<String> uploadedImageUrls;
    private RecyclerView recyclerViewImages;
    private ImageAdapter imageAdapter;
    private int uploadCount = 0;

    // Dialog
    private BottomSheetDialog dialog;
    private LinearLayout pickImgAlbum, pickImgCamera;
    private ActivityResultLauncher<Intent> activityResultLauncher, cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_room);

        initializeFirebase();
        initializeViews();
        setupActivityResultLaunchers();
        setupClickListeners();
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
        btnBack = this.findViewById(R.id.btnBack);

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

        spinnerCity = findViewById(R.id.spinnerCity);
        spinnerDistrict = findViewById(R.id.spinnerDistrict);

        edtAddress = findViewById(R.id.edtAddress);

        btn_create_room = this.findViewById(R.id.btn_create_room);

        selectedImages = new ArrayList<>();
        uploadedImageUrls = new ArrayList<>();
        recyclerViewImages = findViewById(R.id.recyclerViewImages);
        recyclerViewImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ImageAdapter(this, selectedImages);
        recyclerViewImages.setAdapter(imageAdapter);
    
        Button btnAddImage = findViewById(R.id.btnAddImage);
        btnAddImage.setOnClickListener(v -> showBottomDialog());

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

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btn_create_room.setOnClickListener(v -> showConfirmationDialog());
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
                        PostRoomActivity.this,
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
                Toast.makeText(PostRoomActivity.this, "Failed to load cities", Toast.LENGTH_SHORT).show();
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
                        PostRoomActivity.this,
                        android.R.layout.simple_spinner_item,
                        districts
                );
                districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDistrict.setAdapter(districtAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PostRoomActivity.this, "Failed to load districts", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(PostRoomActivity.this,
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
        String id_room = UUID.randomUUID().toString();
        String city = spinnerCity.getSelectedItem().toString();
        String district = spinnerDistrict.getSelectedItem().toString();
        String detail = edtAddress.getText().toString();
        String ward = "";
        String address_combine = detail + ", " + district + ", " + city;
        int status_room = 0;
        String id_own_post = userCurrent.getUid();

        if ("".equals(detail)) {
            address = new Address(city, district);
        } else {
            address = new Address(city, district, detail, ward, address_combine);
        }

        String gender_room;
        if (genderCheckboxes[0].isChecked()) {
            if (!genderCheckboxes[1].isChecked()) {
                gender_room = "Nam";
            } else {
                gender_room = "Nam/Nữ";
            }
        } else {
            if (!genderCheckboxes[0].isChecked()) {
                gender_room = "Nữ";
            } else {
                gender_room = "Nam/Nữ";
            }
        }

        String title_room = edtTitleRoom.getText().toString();
        long deposit_room = Long.parseLong(edtDeposit.getText().toString());
        long price_room = Long.parseLong(edtPrice.getText().toString());

        int type_room = 0;
        if (radioGroup.getCheckedRadioButtonId() == R.id.radiobtnChungCu) {
            type_room = 1;
        }

        String area_room = edtArea.getText().toString();
        String phone = edtPhone.getText().toString();
        int floor = Integer.parseInt(edtFloor.getText().toString());
        int person_in_room = Integer.parseInt(edtPerson.getText().toString());
        String description_room = edtDescriptionRoom.getText().toString();
        int park_slot = Integer.parseInt(edtPark.getText().toString());

        furnitures = new ArrayList<>();
        handleDataFurniture();

        extensions_room = new ArrayList<>();
        handleDataExtensions();

        ImagesRoomClass images = new ImagesRoomClass(uploadedImageUrls);

        return new Room(id_own_post, id_room, title_room, price_room, address, area_room,
                deposit_room, description_room, gender_room, park_slot,
                person_in_room, status_room, type_room, phone, floor, images,
                furnitures, extensions_room,
                Long.parseLong(edtElectric.getText().toString()),
                Long.parseLong(edtWater.getText().toString()),
                Long.parseLong(edtInternet.getText().toString()));
    }

    private void uploadRoomToFirebase(Room room) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        String path_type = room.getType_room() == 1 ? "ChungCuMini" : "Tro";
        DatabaseReference myRef = database.getReference("Rooms/" + path_type);
        myRef.child(room.getId_room()).setValue(room)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(PostRoomActivity.this, "Đăng thông tin phòng thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(PostRoomActivity.this, "Đăng thông tin phòng thất bại", Toast.LENGTH_SHORT).show()
                );
    }

    private void handleDataFurniture() {
        if (furnitureCheckboxes[0].isChecked()) {
            furnitures.add(new Furniture("checkbox_air_condition", furnitureCheckboxes[0].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-air-condittion.png?alt=media&token=85d235e6-f4f4-44b1-89f4-05bed51050a6"));
        }
        if (furnitureCheckboxes[1].isChecked()) {
            furnitures.add(new Furniture("checkbox_heater", furnitureCheckboxes[1].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-heater.png?alt=media&token=4c4871ff-ef6f-42bc-a480-3b60336b802c"));
        }
        if (furnitureCheckboxes[2].isChecked()) {
            furnitures.add(new Furniture("checkbox_curtain", furnitureCheckboxes[2].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-curtain.png?alt=media&token=400eb929-952b-4051-acc2-a26db74251ac"));
        }
        if (furnitureCheckboxes[3].isChecked()) {
            furnitures.add(new Furniture("checkboxfridge", furnitureCheckboxes[3].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-fridge.png?alt=media&token=deb2cded-5a02-464e-8e93-2672d7bc9b89"));
        }
        if (furnitureCheckboxes[4].isChecked()) {
            furnitures.add(new Furniture("checkboxbed", furnitureCheckboxes[4].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-bed.png?alt=media&token=9ed19798-ba14-4604-87d6-5f0224584f42"));
        }
        if (furnitureCheckboxes[5].isChecked()) {
            furnitures.add(new Furniture("checkboxwardrobe", furnitureCheckboxes[5].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-Wardrobe.png?alt=media&token=944da04f-03dd-4b8f-b627-27f1f8f11c9a"));
        }
        if (furnitureCheckboxes[6].isChecked()) {
            furnitures.add(new Furniture("checkbox_washing_machine", furnitureCheckboxes[6].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-washing-machine.png?alt=media&token=ee166ffd-2cb6-4d76-85a8-0a587effb2af"));
        }
        if (furnitureCheckboxes[7].isChecked()) {
            furnitures.add(new Furniture("checkboxsofa", furnitureCheckboxes[7].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-sofa.png?alt=media&token=f9bba804-271b-4740-b28c-d7d89d083d6f"));
        }
    }

    private void handleDataExtensions() {
        if (utilityCheckboxes[0].isChecked()) {
            extensions_room.add(new Utility("checkboxtoilet", utilityCheckboxes[0].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-toilet.png?alt=media&token=426b6597-5dc4-4182-887e-fbeb37d5acc0"));
        }
        if (utilityCheckboxes[1].isChecked()) {
            extensions_room.add(new Utility("checkboxfloor", utilityCheckboxes[1].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-ladder.png?alt=media&token=96975838-2519-4637-87ef-1c966b0f5308"));
        }
        if (utilityCheckboxes[2].isChecked()) {
            extensions_room.add(new Utility("checkbox_time_flex", utilityCheckboxes[2].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-time-flex.png?alt=media&token=c3d87c64-086b-43c8-b896-4d2777c2e7e5"));
        }
        if (utilityCheckboxes[3].isChecked()) {
            extensions_room.add(new Utility("checkboxfingerprint", utilityCheckboxes[3].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-finger-print.png?alt=media&token=8dccd0ac-ff93-4d1d-9f44-6db70a315853"));
        }
        if (utilityCheckboxes[4].isChecked()) {
            extensions_room.add(new Utility("checkboxbacony", utilityCheckboxes[4].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-ladder.png?alt=media&token=96975838-2519-4637-87ef-1c966b0f5308"));
        }
        if (utilityCheckboxes[5].isChecked()) {
            extensions_room.add(new Utility("checkboxpet", utilityCheckboxes[5].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-paw-pet.png?alt=media&token=8a649047-04d9-4421-a064-fca84b7f8f0d"));
        }
        if (utilityCheckboxes[6].isChecked()) {
            extensions_room.add(new Utility("checkbox_w_owner", utilityCheckboxes[6].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-user.png?alt=media&token=db7d94aa-1a03-42f3-834a-4a3aec4c3866"));
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;

        if (isEmpty(edtTitleRoom)) {
            edtTitleRoom.setError("Vui lòng nhập tiêu đề bài đăng");
            isValid = false;
        }

        if (isEmpty(edtDeposit)) {
            edtDeposit.setError("Vui lòng nhập tiền cọc");
            isValid = false;
        }

        if (isEmpty(edtPrice)) {
            edtPrice.setError("Vui lòng nhập tiền cọc");
            isValid = false;
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
            String regex = "^\\d{10}$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(edtPhone.getText().toString());
            if (!matcher.matches()) {
                edtPhone.setError("Vui lòng nhập đúng định dạng số điện thoại");
                isValid = false;
            }
        }

        if (isEmpty(edtFloor)) {
            edtFloor.setError("Vui lòng nhập số tầng");
            isValid = false;
        }

        if (isEmpty(edtPerson)) {
            edtPerson.setError("Vui lòng nhập số người/phòng");
            isValid = false;
        }

        if (isEmpty(edtDescriptionRoom)) {
            edtDescriptionRoom.setError("Vui lòng nhập số mô tả phòng chi tiết");
            isValid = false;
        }

        if (isEmpty(edtPark)) {
            edtPark.setError("Vui lòng nhập số chỗ để xe trong 1 phòng");
            isValid = false;
        }

        if (isEmpty(edtElectric) || isEmpty(edtInternet) || isEmpty(edtWater)) {
            isValid = false;
            if (isEmpty(edtInternet)) {
                edtInternet.setError("Vui lòng nhập giá Internet");
            }
            if (isEmpty(edtElectric)) {
                edtElectric.setError("Vui lòng nhập giá điện");
            }
            if (isEmpty(edtWater)) {
                edtWater.setError("Vui lòng nhập giá nước");
            }
        }

        return isValid;
    }

    private boolean isEmpty(EditText text) {
        return TextUtils.isEmpty(text.getText().toString());
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_CODE);
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_CODE);
            }
        }
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
            checkPermissions();
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
                    if (!selectedImages.isEmpty()) {
                        AlertDialog progressDialog = new AlertDialog.Builder(this)
                                .setView(R.layout.progress_layout)
                                .setCancelable(false)
                                .create();
                        progressDialog.show();
                        uploadImages(progressDialog);
                    } else {
                        Toast.makeText(this, "Vui lòng chọn ít nhất 1 ảnh", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Không", null)
                .show();
    }
}