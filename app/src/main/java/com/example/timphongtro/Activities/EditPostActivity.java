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
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditPostActivity extends AppCompatActivity {

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

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String roomString = bundle.getString("DataRoom");
            Gson gson = new Gson();
            roomData = gson.fromJson(roomString, Room.class);
        }

        initializeFirebase();
        initializeViews();
        setupActivityResultLaunchers();
        setupCitySpinner();

        if (roomData != null) {
            populateDataFromRoom();
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
        edtTitleRoom.setText(roomData.getTitle_room());
        edtPrice.setText(String.valueOf(roomData.getPrice_room()));
        edtDeposit.setText(String.valueOf(roomData.getDeposit_room()));
        edtArea.setText(roomData.getArea_room());
        edtPhone.setText(roomData.getPhone());
        edtFloor.setText(String.valueOf(roomData.getFloor()));
        edtPerson.setText(String.valueOf(roomData.getPerson_in_room()));
        edtDescriptionRoom.setText(roomData.getDescription_room());
        edtPark.setText(String.valueOf(roomData.getPark_slot()));
        edtElectric.setText(String.valueOf(roomData.getPrice_electric()));
        edtWater.setText(String.valueOf(roomData.getPrice_water()));
        edtInternet.setText(String.valueOf(roomData.getPrice_internet()));

        address = roomData.getAddress();
        edtAddress.setText(address.getDetail());

        String gender = roomData.getGender_room();
        genderCheckboxes[0].setChecked(gender.contains("Nam"));
        genderCheckboxes[1].setChecked(gender.contains("Nữ"));

        radioGroupType.check(roomData.getType_room() == 1 ? R.id.radiobtnChungCu : R.id.radiobtnTro);
        radioGroupState.check(roomData.getStatus_room() == 1 ? R.id.radiobtnUnavailable : R.id.radiobtnAvailable);

        if (roomData.getImages() != null) {
            uploadedImageUrls = new ArrayList<>(roomData.getImages());
            selectedImages.clear();
            for (String imageUrl : uploadedImageUrls) {
                selectedImages.add(Uri.parse(imageUrl));
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
                        EditPostActivity.this,
                        android.R.layout.simple_spinner_item,
                        cities
                );
                cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCity.setAdapter(cityAdapter);

                if (roomData != null && roomData.getAddress() != null) {
                    int cityPosition = cities.indexOf(roomData.getAddress().getCity());
                    if (cityPosition != -1) {
                        spinnerCity.setSelection(cityPosition);
                    }
                }

                spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String selectedCityName = cities.get(position);
                        String selectedCityKey = cityKeyMap.get(selectedCityName);
                        loadDistrictsForCity(selectedCityKey);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditPostActivity.this, "Failed to load cities", Toast.LENGTH_SHORT).show();
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

                ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(EditPostActivity.this, android.R.layout.simple_spinner_item, districts);
                districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDistrict.setAdapter(districtAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditPostActivity.this, "Failed to load districts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setUtilityCheckboxes(ArrayList<Utility> utilities) {
        for (CheckBox checkbox : utilityCheckboxes) {
            checkbox.setChecked(false);
        }

        if (utilities != null) {
            for (Utility utility : utilities) {
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

    private void setFurnitureCheckboxes(ArrayList<Furniture> furnitureList) {
        for (CheckBox checkbox : furnitureCheckboxes) {
            checkbox.setChecked(false);
        }

        if (furnitureList != null) {
            for (Furniture furniture : furnitureList) {
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

    private void handleDataFurniture() {
        furnitures = new ArrayList<>();
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
        extensions_room = new ArrayList<>();
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

            if (selectedImages.isEmpty()) {
                updatedRoom.setImages(newUploadedUrls);
                uploadRoomToFirebase(updatedRoom);
                progressDialog.dismiss();
                return;
            }

            uploadCount = 0;
            final int totalImages = selectedImages.size();
            final boolean[] hasError = {false};

            for (Uri imageUri : selectedImages) {
                if (imageUri.toString().startsWith("https://")) {
                    uploadCount++;
                    if (uploadCount == totalImages) {
                        updatedRoom.setImages(newUploadedUrls);
                        uploadRoomToFirebase(updatedRoom);
                        progressDialog.dismiss();
                    }
                    continue;
                }

                String fileName = "room_" + System.currentTimeMillis() + "_" + uploadCount + ".jpg";
                StorageReference imageRef = storage.getReference("RoomImages").child(fileName);

                imageRef.putFile(imageUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                newUploadedUrls.add(uri.toString());
                                uploadCount++;

                                if (uploadCount == totalImages && !hasError[0]) {
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
                                Toast.makeText(EditPostActivity.this,
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

        if (TextUtils.isEmpty(edtAddress.getText())) {
            edtAddress.setError("Vui lòng nhập địa chỉ chi tiết");
            isValid = false;
        }

        if (!genderCheckboxes[0].isChecked() && !genderCheckboxes[1].isChecked()) {
            Toast.makeText(this, "Vui lòng chọn giới tính", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private Room createRoomObject() {
        String id_room = roomData.getId_room();
        String id_own_post = roomData.getId_own_post();

        String city = spinnerCity.getSelectedItem().toString();
        String district = spinnerDistrict.getSelectedItem().toString();
        String detail = edtAddress.getText().toString();
        String ward = "";
        String address_combine = detail + ", " + district + ", " + city;

        int status_room = radioGroupState.getCheckedRadioButtonId() == R.id.radiobtnUnavailable ? 1 : 0;

        if ("".equals(detail)) {
            address = new Address(city, district);
        } else {
            address = new Address(city, district, detail, ward, address_combine);
        }

        String gender_room;
        if (genderCheckboxes[0].isChecked() && genderCheckboxes[1].isChecked()) {
            gender_room = "Nam/Nữ";
        } else if (genderCheckboxes[0].isChecked()) {
            gender_room = "Nam";
        } else {
            gender_room = "Nữ";
        }

        int type_room = radioGroupType.getCheckedRadioButtonId() == R.id.radiobtnChungCu ? 1 : 0;

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

        return new Room(
                id_own_post,
                id_room,
                edtTitleRoom.getText().toString(),
                Long.parseLong(edtPrice.getText().toString()),
                address,
                edtArea.getText().toString(),
                Long.parseLong(edtDeposit.getText().toString()),
                edtDescriptionRoom.getText().toString(),
                gender_room,
                Integer.parseInt(edtPark.getText().toString()),
                Integer.parseInt(edtPerson.getText().toString()),
                status_room,
                type_room,
                edtPhone.getText().toString(),
                Integer.parseInt(edtFloor.getText().toString()),
                images,
                furnitures,
                extensions_room,
                Long.parseLong(edtElectric.getText().toString()),
                Long.parseLong(edtWater.getText().toString()),
                Long.parseLong(edtInternet.getText().toString())
        );
    }

    private void uploadRoomToFirebase(Room room) {
        // Lấy reference đến node hiện tại và node mới
        DatabaseReference currentRef = FirebaseDatabase.getInstance()
                .getReference("Rooms")
                .child(roomData.getType_room() == 1 ? "ChungCuMini" : "Tro")
                .child(roomData.getId_room());

        DatabaseReference newRef = FirebaseDatabase.getInstance()
                .getReference("Rooms")
                .child(room.getType_room() == 1 ? "ChungCuMini" : "Tro")
                .child(room.getId_room());

        // Nếu type_room thay đổi
        if (roomData.getType_room() != room.getType_room()) {
            AlertDialog progressDialog = new AlertDialog.Builder(this)
                    .setView(R.layout.progress_layout)
                    .setCancelable(false)
                    .create();
            progressDialog.show();

            currentRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    // Lấy toàn bộ dữ liệu của node cũ
                    Map<String, Object> oldData = (Map<String, Object>) task.getResult().getValue();
                    if (oldData != null && oldData.containsKey("userLovePost")) {
                        Object userLovePost = oldData.get("userLovePost");
                        
                        // Xóa node cũ sau khi đã lấy được dữ liệu
                        currentRef.removeValue().addOnSuccessListener(aVoid -> {
                            // Thêm userLovePost vào room mới
                            room.setUserLovePost(userLovePost);
                            
                            // Tạo Map dữ liệu mới để upload
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
                            roomMap.put("userLovePost", userLovePost);

                            // Upload lên node mới
                            newRef.setValue(roomMap)
                                    .addOnSuccessListener(aVoid2 -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(EditPostActivity.this, "Cập nhật thông tin phòng thành công", Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(EditPostActivity.this, "Cập nhật thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        });
                    }
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(EditPostActivity.this, "Không thể đọc dữ liệu hiện tại", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Nếu không thay đổi type_room, update bình thường
            uploadData(room, currentRef);
        }
    }

    private void uploadData(Room room, DatabaseReference ref) {
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setView(R.layout.progress_layout)
                .setCancelable(false)
                .create();
        progressDialog.show();

        ref.child("userLovePost").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Object userLovePost = task.getResult().getValue();

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

                if (userLovePost != null) {
                    roomMap.put("userLovePost", userLovePost);
                }

                ref.updateChildren(roomMap)
                        .addOnSuccessListener(aVoid -> {
                            progressDialog.dismiss();
                            Toast.makeText(EditPostActivity.this, "Cập nhật thông tin phòng thành công", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            Toast.makeText(EditPostActivity.this, "Cập nhật thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                progressDialog.dismiss();
                Toast.makeText(EditPostActivity.this, "Không thể đọc dữ liệu hiện tại", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
