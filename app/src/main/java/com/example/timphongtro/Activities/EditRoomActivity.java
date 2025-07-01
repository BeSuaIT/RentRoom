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
import com.example.timphongtro.Models.Room;
import com.example.timphongtro.Models.Furniture;
import com.example.timphongtro.R;
import com.example.timphongtro.Utils.GsonUtils;
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

public class EditRoomActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE = 1001;
    private FirebaseUser userCurrent;
    private FirebaseStorage storage;
    private DatabaseReference citiesRef;
    private EditText edtTitleRoom, edtDeposit, edtPrice, edtInternet, edtElectric, edtWater,
            edtArea, edtPhone, edtFloor, edtPerson, edtDescriptionRoom, edtPark, edtAddress;
    private RadioGroup radioGroupType, radioGroupState;
    private Spinner spinnerCity, spinnerDistrict;
    private CheckBox[] utilityCheckboxes, furnitureCheckboxes, genderCheckboxes;
    private List<String> cities, districts;
    private ArrayList<Furniture> furnitures;
    private ArrayList<Utility> extensions_room;
    private Address address;
    private ArrayList<Uri> selectedImages;
    private ArrayList<String> uploadedImageUrls;
    private RecyclerView recyclerViewImages;
    private ImageAdapter imageAdapter;
    private int uploadCount = 0;
    private Room roomData;
    private BottomSheetDialog dialog;
    private LinearLayout pickImgAlbum, pickImgCamera;
    private ActivityResultLauncher<Intent> activityResultLauncher, cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        loadRoomDataFromIntent();
        initializeFirebase();
        initializeViews();
        setupActivityResultLaunchers();
        setupCitySpinner();

        if (roomData != null) {
            populateDataFromRoom();
        } else {
            Toast.makeText(this, "Lỗi: Không có dữ liệu phòng để chỉnh sửa", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadRoomDataFromIntent() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String roomString = bundle.getString("DataRoom");
            if (roomString != null) {
                roomData = GsonUtils.fromJson(roomString, Room.class);
                
                if (roomData == null) {
                    Toast.makeText(this, "Lỗi: Dữ liệu phòng không hợp lệ", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(this, "Lỗi: Không có dữ liệu phòng", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Lỗi: Thiếu dữ liệu", Toast.LENGTH_SHORT).show();
            finish();
        }
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
        edtTitleRoom = findViewById(R.id.edtTitleRoom);
        edtPrice = findViewById(R.id.edtPrice);
        edtDeposit = findViewById(R.id.edtDeposit);
        edtInternet = findViewById(R.id.edtInternet);
        edtElectric = findViewById(R.id.edtElectric);
        edtWater = findViewById(R.id.edtWater);
        edtArea = findViewById(R.id.edtArea);
        edtPhone = findViewById(R.id.edtPhone);
        edtFloor = findViewById(R.id.edtFloor);
        edtPerson = findViewById(R.id.edtPerson);
        edtDescriptionRoom = findViewById(R.id.edtDescriptionRoom);
        edtPark = findViewById(R.id.edtPark);
        edtAddress = findViewById(R.id.edtAddress);

        radioGroupType = findViewById(R.id.radioGroupType);
        radioGroupState = findViewById(R.id.radioGroupState);

        spinnerCity = findViewById(R.id.spinnerCity);
        spinnerDistrict = findViewById(R.id.spinnerDistrict);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btn_create_room).setOnClickListener(v -> showConfirmationDialog());

        selectedImages = new ArrayList<>();
        uploadedImageUrls = new ArrayList<>();
        recyclerViewImages = findViewById(R.id.recyclerViewImages);
        recyclerViewImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        selectedImages = new ArrayList<>();
        imageAdapter = new ImageAdapter(this, selectedImages);
        recyclerViewImages.setAdapter(imageAdapter);
        recyclerViewImages.setHasFixedSize(true);

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

    private void populateDataFromRoom() {
        if (roomData == null) return;

        edtTitleRoom.setText(roomData.getTitle_room() != null ? roomData.getTitle_room() : "");
        edtPrice.setText(String.valueOf(roomData.getPrice_room()));
        edtDeposit.setText(String.valueOf(roomData.getDeposit_room()));
        edtArea.setText(String.valueOf(roomData.getArea_room()));
        edtPhone.setText(roomData.getPhone() != null ? roomData.getPhone() : "");
        edtFloor.setText(String.valueOf(roomData.getFloor()));
        edtPerson.setText(String.valueOf(roomData.getPerson_in_room()));
        edtDescriptionRoom.setText(roomData.getDescription_room() != null ? roomData.getDescription_room() : "");
        edtPark.setText(String.valueOf(roomData.getPark_slot()));
        edtElectric.setText(String.valueOf(roomData.getPrice_electric()));
        edtWater.setText(String.valueOf(roomData.getPrice_water()));
        edtInternet.setText(String.valueOf(roomData.getPrice_internet()));

        address = roomData.getAddress();
        if (address != null) {
            String detail = address.getDetail();
            edtAddress.setText(detail != null ? detail : "");
        } else {
            edtAddress.setText("");
        }

        String gender = roomData.getGender_room();
        if (gender != null) {
            genderCheckboxes[0].setChecked(gender.contains("Nam"));
            genderCheckboxes[1].setChecked(gender.contains("Nữ"));
        } else {
            genderCheckboxes[0].setChecked(true);
            genderCheckboxes[1].setChecked(true);
        }

        String roomType = roomData.getType_room();
        if ("Chung cư Mini".equals(roomType)) {
            radioGroupType.check(R.id.radiobtnChungCu);
        } else {
            radioGroupType.check(R.id.radiobtnTro);
        }

        radioGroupState.check(roomData.getStatus_room() == 1 ? R.id.radiobtnUnavailable : R.id.radiobtnAvailable);

        if (roomData.getImages() != null && !roomData.getImages().isEmpty()) {
            uploadedImageUrls = new ArrayList<>(roomData.getImages());
            selectedImages.clear();
            for (String imageUrl : uploadedImageUrls) {
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    selectedImages.add(Uri.parse(imageUrl));
                }
            }
            imageAdapter.notifyDataSetChanged();
        }

        setUtilityCheckboxes(roomData.getRoomUtilities());
        setFurnitureCheckboxes(roomData.getRoomFurniture());
    }

    private void setupCitySpinner() {
        Map<String, String> cityKeyMap = new HashMap<>();

        citiesRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
                        EditRoomActivity.this,
                        android.R.layout.simple_spinner_item,
                        cities
                );
                cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCity.setAdapter(cityAdapter);

                if (roomData != null && roomData.getAddress() != null && roomData.getAddress().getCity() != null) {
                    int cityPosition = cities.indexOf(roomData.getAddress().getCity());
                    if (cityPosition != -1) {
                        spinnerCity.setSelection(cityPosition);
                    }
                }

                spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position >= 0 && position < cities.size()) {
                            String selectedCityName = cities.get(position);
                            String selectedCityKey = cityKeyMap.get(selectedCityName);
                            if (selectedCityKey != null) {
                                loadDistrictsForCity(selectedCityKey);
                            }
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditRoomActivity.this, "Lỗi tải danh sách tỉnh thành", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDistrictsForCity(String cityKey) {
        if (cityKey == null) return;
        
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

                ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(EditRoomActivity.this, android.R.layout.simple_spinner_item, districts);
                districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDistrict.setAdapter(districtAdapter);

                if (roomData != null && roomData.getAddress() != null && roomData.getAddress().getDistrict() != null) {
                    int districtPosition = districts.indexOf(roomData.getAddress().getDistrict());
                    if (districtPosition != -1) {
                        spinnerDistrict.setSelection(districtPosition);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditRoomActivity.this, "Lỗi tải danh sách quận huyện", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setUtilityCheckboxes(ArrayList<Utility> utilities) {
        for (CheckBox checkbox : utilityCheckboxes) {
            checkbox.setChecked(false);
        }

        if (utilities != null) {
            for (Utility utility : utilities) {
                if (utility != null && utility.getId() != null) {
                    switch (utility.getId()) {
                        case "checkboxtoilet":
                            utilityCheckboxes[0].setChecked(true);
                            break;
                        case "checkboxfloor":
                            utilityCheckboxes[1].setChecked(true);
                            break;
                        case "checkbox_time_flex":
                            utilityCheckboxes[2].setChecked(true);
                            break;
                        case "checkboxfingerprint":
                            utilityCheckboxes[3].setChecked(true);
                            break;
                        case "checkboxbacony":
                            utilityCheckboxes[4].setChecked(true);
                            break;
                        case "checkboxpet":
                            utilityCheckboxes[5].setChecked(true);
                            break;
                        case "checkbox_w_owner":
                            utilityCheckboxes[6].setChecked(true);
                            break;
                    }
                }
            }
        }
    }

    private void setFurnitureCheckboxes(ArrayList<Furniture> furnitureList) {
        for (CheckBox checkbox : furnitureCheckboxes) {
            checkbox.setChecked(false);
        }

        if (furnitureList != null) {
            for (Furniture furniture : furnitureList) {
                if (furniture != null && furniture.getId() != null) {
                    switch (furniture.getId()) {
                        case "checkbox_air_condition":
                            furnitureCheckboxes[0].setChecked(true);
                            break;
                        case "checkbox_heater":
                            furnitureCheckboxes[1].setChecked(true);
                            break;
                        case "checkbox_curtain":
                            furnitureCheckboxes[2].setChecked(true);
                            break;
                        case "checkboxfridge":
                            furnitureCheckboxes[3].setChecked(true);
                            break;
                        case "checkboxbed":
                            furnitureCheckboxes[4].setChecked(true);
                            break;
                        case "checkboxwardrobe":
                            furnitureCheckboxes[5].setChecked(true);
                            break;
                        case "checkbox_washing_machine":
                            furnitureCheckboxes[6].setChecked(true);
                            break;
                        case "checkboxsofa":
                            furnitureCheckboxes[7].setChecked(true);
                            break;
                    }
                }
            }
        }
    }

    private void handleDataFurniture() {
        furnitures = new ArrayList<>();
        if (furnitureCheckboxes[0].isChecked()) {
            furnitures.add(new Furniture("checkbox_air_condition", furnitureCheckboxes[0].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-air-condittion.png?alt=media&token=20e32d92-eda5-40bc-9b80-70ce0353d545"));
        }
        if (furnitureCheckboxes[1].isChecked()) {
            furnitures.add(new Furniture("checkbox_heater", furnitureCheckboxes[1].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-heater.png?alt=media&token=aa5b60e5-5230-48e8-9d2f-a91b8e8f90cf"));
        }
        if (furnitureCheckboxes[2].isChecked()) {
            furnitures.add(new Furniture("checkbox_curtain", furnitureCheckboxes[2].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-curtain.png?alt=media&token=fdadffc2-621d-47c8-81e8-984361cb32e7"));
        }
        if (furnitureCheckboxes[3].isChecked()) {
            furnitures.add(new Furniture("checkboxfridge", furnitureCheckboxes[3].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-fridge.png?alt=media&token=0ef07211-32cb-4336-8ee4-e4da0f94b37d"));
        }
        if (furnitureCheckboxes[4].isChecked()) {
            furnitures.add(new Furniture("checkboxbed", furnitureCheckboxes[4].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-bed.png?alt=media&token=f941b225-29ef-4de4-a4e4-2a5fc4316e0c"));
        }
        if (furnitureCheckboxes[5].isChecked()) {
            furnitures.add(new Furniture("checkboxwardrobe", furnitureCheckboxes[5].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-Wardrobe.png?alt=media&token=7f20f8ef-03bd-475f-a822-7e08d8129bba"));
        }
        if (furnitureCheckboxes[6].isChecked()) {
            furnitures.add(new Furniture("checkbox_washing_machine", furnitureCheckboxes[6].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-washing-machine.png?alt=media&token=a755fb42-789a-4791-a520-d7e890e4f1a9"));
        }
        if (furnitureCheckboxes[7].isChecked()) {
            furnitures.add(new Furniture("checkboxsofa", furnitureCheckboxes[7].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-sofa.png?alt=media&token=2f25df7d-7466-467f-88c6-aaae6e9fc570"));
        }
    }

    private void handleDataExtensions() {
        extensions_room = new ArrayList<>();
        if (utilityCheckboxes[0].isChecked()) {
            extensions_room.add(new Utility("checkboxtoilet", utilityCheckboxes[0].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-toilet.png?alt=media&token=29ad52f7-37ec-44bf-aa01-a6d9dd7ec267"));
        }
        if (utilityCheckboxes[1].isChecked()) {
            extensions_room.add(new Utility("checkboxfloor", utilityCheckboxes[1].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-ladder.png?alt=media&token=fd039fdb-4a30-4f72-9821-dd38a5a39496"));
        }
        if (utilityCheckboxes[2].isChecked()) {
            extensions_room.add(new Utility("checkbox_time_flex", utilityCheckboxes[2].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-time-flex.png?alt=media&token=a6514c79-8e24-45cc-b685-c3d7c3970b15"));
        }
        if (utilityCheckboxes[3].isChecked()) {
            extensions_room.add(new Utility("checkboxfingerprint", utilityCheckboxes[3].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-finger-print.png?alt=media&token=709f1ae4-fe30-40f3-8b55-f0d145758ae6"));
        }
        if (utilityCheckboxes[4].isChecked()) {
            extensions_room.add(new Utility("checkboxbacony", utilityCheckboxes[4].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-ladder.png?alt=media&token=fd039fdb-4a30-4f72-9821-dd38a5a39496"));
        }
        if (utilityCheckboxes[5].isChecked()) {
            extensions_room.add(new Utility("checkboxpet", utilityCheckboxes[5].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-paw-pet.png?alt=media&token=33993aff-4371-4f12-b8e9-6f155bb22d9e"));
        }
        if (utilityCheckboxes[6].isChecked()) {
            extensions_room.add(new Utility("checkbox_w_owner", utilityCheckboxes[6].getText().toString(), "https://firebasestorage.googleapis.com/v0/b/tim-phong-tro-babbd.appspot.com/o/icon_png%2Fic-user.png?alt=media&token=21a5ecdf-8efd-4af5-9e73-ae9d5be0da1d"));
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
                Toast.makeText(this, "Lỗi mở camera", Toast.LENGTH_SHORT).show();
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
                                    if (cachedUri != null) {
                                        selectedImages.add(cachedUri);
                                    }
                                }
                            } else if (data.getData() != null) {
                                Uri imageUri = data.getData();
                                Uri cachedUri = copyImageToCache(imageUri);
                                if (cachedUri != null) {
                                    selectedImages.add(cachedUri);
                                }
                            }
                            imageAdapter.notifyDataSetChanged();
                        }
                        dialog.dismiss();
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            if (imageBitmap != null) {
                                Uri imageUri = saveImageToCache(imageBitmap);
                                if (imageUri != null) {
                                    selectedImages.add(imageUri);
                                    imageAdapter.notifyDataSetChanged();
                                }
                            }
                        }
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

    private void showConfirmationDialog() {
        AlertDialog confirmDialog = new AlertDialog.Builder(this)
                .setTitle("Xác nhận")
                .setMessage("Bạn có muốn cập nhật thông tin phòng không?")
                .setPositiveButton("Có", null)
                .setNegativeButton("Không", null)
                .create();

        confirmDialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                confirmDialog.dismiss();

                if (!validateInputs()) return;
                Room updatedRoom = createRoomObject();

                if (updatedRoom == null) {
                    Toast.makeText(this, "Lỗi tạo dữ liệu phòng", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!selectedImages.isEmpty()) {
                    AlertDialog progressDialog = new AlertDialog.Builder(this)
                            .setView(R.layout.progress_layout)
                            .setCancelable(false)
                            .create();
                    progressDialog.show();
                    uploadImagesAndUpdateRoom(progressDialog, updatedRoom);
                } else {
                    uploadRoomToFirebase(updatedRoom);
                }
            });
        });
        confirmDialog.show();
    }

    private void uploadImagesAndUpdateRoom(AlertDialog progressDialog, Room updatedRoom) {
        try {
            ArrayList<String> newUploadedUrls = new ArrayList<>();

            for (Uri imageUri : selectedImages) {
                String uriString = imageUri.toString();
                if (uriString.startsWith("https://")) {
                    newUploadedUrls.add(uriString);
                }
            }

            int imagesToUpload = (int) selectedImages.stream().filter(imageUri -> !imageUri.toString().startsWith("https://")).count();

            if (imagesToUpload == 0) {
                updatedRoom.setImages(newUploadedUrls);
                uploadRoomToFirebase(updatedRoom);
                progressDialog.dismiss();
                return;
            }

            uploadCount = 0;
            final boolean[] hasError = {false};

            for (Uri imageUri : selectedImages) {
                if (imageUri.toString().startsWith("https://")) {
                    continue;
                }

                String fileName = "room_" + System.currentTimeMillis() + "_" + uploadCount + ".jpg";
                StorageReference imageRef = storage.getReference("RoomImages").child(fileName);

                imageRef.putFile(imageUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                newUploadedUrls.add(uri.toString());
                                uploadCount++;

                                if (uploadCount == imagesToUpload && !hasError[0]) {
                                    updatedRoom.setImages(newUploadedUrls);
                                    uploadRoomToFirebase(updatedRoom);
                                    progressDialog.dismiss();
                                }
                            });
                        })
                        .addOnFailureListener(e -> {
                            hasError[0] = true;
                            if (!isFinishing()) {
                                progressDialog.dismiss();
                                Toast.makeText(EditRoomActivity.this,
                                        "Lỗi tải ảnh: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            if (roomData.getImages() != null) {
                for (String oldImageUrl : roomData.getImages()) {
                    if (!newUploadedUrls.contains(oldImageUrl)) {
                        try {
                            String filePath = oldImageUrl.substring(
                                    oldImageUrl.indexOf("o/") + 2,
                                    oldImageUrl.indexOf("?")
                            );
                            filePath = filePath.replace("%2F", "/");

                            StorageReference oldImageRef = storage.getReference().child(filePath);
                            oldImageRef.delete();
                        } catch (Exception ignored) {

                        }
                    }
                }
            }

        } catch (Exception e) {
            if (!isFinishing()) {
                progressDialog.dismiss();
                Toast.makeText(this, "Có lỗi xảy ra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;

        if (TextUtils.isEmpty(edtTitleRoom.getText())) {
            edtTitleRoom.setError("Vui lòng nhập tiêu đề");
            isValid = false;
        }

        if (TextUtils.isEmpty(edtPrice.getText())) {
            edtPrice.setError("Vui lòng nhập giá phòng");
            isValid = false;
        }

        if (TextUtils.isEmpty(edtDeposit.getText())) {
            edtDeposit.setError("Vui lòng nhập tiền cọc");
            isValid = false;
        }

        if (TextUtils.isEmpty(edtArea.getText())) {
            edtArea.setError("Vui lòng nhập diện tích");
            isValid = false;
        }

        if (TextUtils.isEmpty(edtPhone.getText())) {
            edtPhone.setError("Vui lòng nhập số điện thoại");
            isValid = false;
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

        String phone = edtPhone.getText().toString().trim();
        if (!phone.isEmpty() && !phone.matches("^\\d{10}$")) {
            edtPhone.setError("Số điện thoại phải có 10 chữ số");
            isValid = false;
        }

        try {
            if (!TextUtils.isEmpty(edtArea.getText())) {
                double area = Double.parseDouble(edtArea.getText().toString());
                if (area <= 0) {
                    edtArea.setError("Diện tích phải lớn hơn 0");
                    isValid = false;
                }
            }
            if (!TextUtils.isEmpty(edtPrice.getText())) {
                long price = Long.parseLong(edtPrice.getText().toString());
                if (price <= 0) {
                    edtPrice.setError("Giá phòng phải lớn hơn 0");
                    isValid = false;
                }
            }

            if (!TextUtils.isEmpty(edtDeposit.getText())) {
                long deposit = Long.parseLong(edtDeposit.getText().toString());
                if (deposit < 0) {
                    edtDeposit.setError("Tiền cọc không được âm");
                    isValid = false;
                }
            }

            if (!TextUtils.isEmpty(edtFloor.getText())) {
                int floor = Integer.parseInt(edtFloor.getText().toString());
                if (floor <= 0) {
                    edtFloor.setError("Tầng phải lớn hơn 0");
                    isValid = false;
                }
            }

            if (!TextUtils.isEmpty(edtPerson.getText())) {
                int person = Integer.parseInt(edtPerson.getText().toString());
                if (person <= 0) {
                    edtPerson.setError("Số người phải lớn hơn 0");
                    isValid = false;
                }
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Vui lòng nhập số hợp lệ cho các trường số", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private Room createRoomObject() {
        try {
            if (roomData == null) return null;

            String id_room = roomData.getId_room();
            String id_own_post = roomData.getId_own_post();

            String city = "";
            String district = "";
            
            if (spinnerCity.getSelectedItem() != null) {
                city = spinnerCity.getSelectedItem().toString();
            }
            
            if (spinnerDistrict.getSelectedItem() != null) {
                district = spinnerDistrict.getSelectedItem().toString();
            }
            
            String detail = edtAddress.getText().toString().trim();

            address = new Address(city, district, detail);

            int status_room = radioGroupState.getCheckedRadioButtonId() == R.id.radiobtnUnavailable ? 1 : 0;

            String gender_room;
            boolean isMaleChecked = genderCheckboxes[0].isChecked();
            boolean isFemaleChecked = genderCheckboxes[1].isChecked();
            
            if (isMaleChecked && isFemaleChecked) {
                gender_room = "Nam/Nữ";
            } else if (isMaleChecked) {
                gender_room = "Nam";
            } else if (isFemaleChecked) {
                gender_room = "Nữ";
            } else {
                gender_room = "Nam/Nữ";
            }

            String type_room = radioGroupType.getCheckedRadioButtonId() == R.id.radiobtnChungCu ? "Chung cư Mini" : "Trọ";

            handleDataFurniture();
            handleDataExtensions();

            ArrayList<String> images;
            if (!uploadedImageUrls.isEmpty()) {
                images = new ArrayList<>(uploadedImageUrls);
            } else if (roomData.getImages() != null) {
                images = new ArrayList<>(roomData.getImages());
            } else {
                images = new ArrayList<>();
            }

            long price, deposit, electric , water, internet;
            int park, person, floor, area;
            
            try {
                price = Long.parseLong(edtPrice.getText().toString());
                deposit = Long.parseLong(edtDeposit.getText().toString());
                electric = Long.parseLong(edtElectric.getText().toString());
                water = Long.parseLong(edtWater.getText().toString());
                internet = Long.parseLong(edtInternet.getText().toString());
                area = Integer.parseInt(edtArea.getText().toString());
                park = Integer.parseInt(edtPark.getText().toString());
                person = Integer.parseInt(edtPerson.getText().toString());
                floor = Integer.parseInt(edtFloor.getText().toString());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Lỗi: Vui lòng nhập số hợp lệ", Toast.LENGTH_SHORT).show();
                return null;
            }

            return new Room(
                    id_own_post,
                    id_room,
                    edtTitleRoom.getText().toString(),
                    price,
                    address,
                    area,
                    deposit,
                    edtDescriptionRoom.getText().toString(),
                    gender_room,
                    park,
                    person,
                    status_room,
                    type_room,
                    edtPhone.getText().toString(),
                    floor,
                    images,
                    furnitures,
                    extensions_room,
                    electric,
                    water,
                    internet
            );
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi tạo dữ liệu phòng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void uploadRoomToFirebase(Room room) {
        if (room == null) {
            Toast.makeText(this, "Lỗi: Dữ liệu phòng không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("Rooms");
        DatabaseReference roomRef = postsRef.child(room.getId_room());

        Map<String, Object> roomMap = new HashMap<>();
        roomMap.put("id_own_post", room.getId_own_post());
        roomMap.put("id_room", room.getId_room());
        roomMap.put("title_room", room.getTitle_room());
        roomMap.put("price_room", room.getPrice_room());
        roomMap.put("deposit_room", room.getDeposit_room());
        roomMap.put("area_room", room.getArea_room());
        roomMap.put("description_room", room.getDescription_room());
        roomMap.put("gender_room", room.getGender_room());
        roomMap.put("park_slot", room.getPark_slot());
        roomMap.put("person_in_room", room.getPerson_in_room());
        roomMap.put("status_room", room.getStatus_room());
        roomMap.put("type_room", room.getType_room());
        roomMap.put("phone", room.getPhone());
        roomMap.put("floor", room.getFloor());
        roomMap.put("price_electric", room.getPrice_electric());
        roomMap.put("price_water", room.getPrice_water());
        roomMap.put("price_internet", room.getPrice_internet());
        roomMap.put("address", room.getAddress());
        roomMap.put("roomFurniture", room.getRoomFurniture());
        roomMap.put("roomUtilities", room.getRoomUtilities());
        roomMap.put("images", room.getImages());
        roomMap.put("timestamp", System.currentTimeMillis());

        roomRef.child("userLovePost").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().getValue() != null) {
                roomMap.put("userLovePost", task.getResult().getValue());
            }

            roomRef.setValue(roomMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(EditRoomActivity.this, "Cập nhật thông tin phòng thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(EditRoomActivity.this, "Cập nhật thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }).addOnFailureListener(e -> {
            roomRef.setValue(roomMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(EditRoomActivity.this, "Cập nhật thông tin phòng thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(updateError -> {
                        Toast.makeText(EditRoomActivity.this, "Cập nhật thất bại: " + updateError.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }
}
