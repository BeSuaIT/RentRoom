package com.example.timphongtro.Activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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

import com.example.timphongtro.Entity.Address;
import com.example.timphongtro.Entity.ExtensionRoom_class;
import com.example.timphongtro.Entity.ImagesRoomClass;
import com.example.timphongtro.Entity.Room;
import com.example.timphongtro.Entity.FurnitureClass;
import com.example.timphongtro.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostRoomActivity extends AppCompatActivity {
    private FirebaseUser userCurrent;
    private ImageView btnBack, uploadPicture1;
    private EditText edtTitleRoom, edtDeposit, edtPrice, edtInternet, edtElectric, edtWater,
            edtArea, edtPhone, edtFloor, edtPerson, edtDescriptionRoom, edtPark, edtAddress;
    private Button btn_create_room;
    private RadioGroup radioGroup;
    private ActivityResultLauncher<Intent> activityResultLauncher, cameraLauncher;
    private LinearLayout pickImgAlbum, pickImgCamera;
    private CheckBox checkboxtoilet, checkboxfloor, checkbox_time_flex, checkboxfingerprint,
            checkboxbacony, checkboxpet, checkbox_w_owner, checkbox_air_condition, checkbox_heater,
            checkbox_curtain, checkboxfridge, checkboxbed, checkboxwardrobe, checkbox_washing_machine,
            checkboxsofa, checkboxNam, checkboxNu;
    private String imageURL1;
    private Uri uri;
    private Bitmap photo;
    private Spinner spinnerCity, spinnerDistrict;
    private boolean isUploadImg1;
    private BottomSheetDialog dialog;
    private List<String> cities, districts;
    private String path;
    private ArrayList<FurnitureClass> furnitures;
    private ArrayList<ExtensionRoom_class> extensions_room;
    private Address address;
    private static final int PERMISSION_CODE = 1001;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_room);

        initView();
        isUploadImg1 = false;
        cities = new ArrayList<>();
        districts = new ArrayList<>();

        getDataForSpinnerCity();
        path = "city/HaNoi/district";
        spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedspinner = cities.get(position);
                if (selectedspinner.equals("Hà Nội")) {
                    path = "city/HaNoi/district";
                } else if (selectedspinner.equals("Hồ Chí Minh")) {
                    path = "city/HoChiMinh/district";
                }
                getDataForSpinnerDistrict();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        getDataForSpinnerDistrict();

        btnBack.setOnClickListener(v -> {
            Intent main = new Intent(PostRoomActivity.this, MainActivity.class);
            startActivity(main);
        });

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            try {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        uri = data.getData();
                        uri = copyImageToCache(uri);
                        uploadPicture1.setImageURI(uri);
                        isUploadImg1 = true;
                        dialog.dismiss();
                    }
                } else {
                    isUploadImg1 = false;
                    Toast.makeText(PostRoomActivity.this, "Không có ảnh nào được chọn", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e("ImagePicker", "Error handling image pick result", e);
                Toast.makeText(PostRoomActivity.this, "Error processing image", Toast.LENGTH_LONG).show();
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                photo = (Bitmap) data.getExtras().get("data");
                uploadPicture1.setImageBitmap(photo);
                isUploadImg1 = true;
                dialog.dismiss();
            } else {
                isUploadImg1 = false;
                Toast.makeText(PostRoomActivity.this, "No image selected", Toast.LENGTH_LONG).show();
            }
        });

        btn_create_room.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(PostRoomActivity.this);
            builder.setTitle("Xác nhận") // Thiết lập tiêu đề của Dialog
                    .setMessage("Bạn có muốn đăng bài không?")
                    .setPositiveButton("Có", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (isUploadImg1) saveImage();
                            else
                                Toast.makeText(getApplicationContext(), "Vui lòng chọn 1 tấm ảnh", Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton("Không", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Xử lý khi người dùng chọn No
                        }
                    });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        });

        uploadPicture1.setOnClickListener(v -> showBottomDialog());
    }

    void initView() {
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

        checkboxtoilet = this.findViewById(R.id.checkboxtoilet);
        checkboxfloor = this.findViewById(R.id.checkboxfloor);
        checkbox_time_flex = this.findViewById(R.id.checkbox_time_flex);
        checkboxfingerprint = this.findViewById(R.id.checkboxfingerprint);
        checkboxbacony = this.findViewById(R.id.checkboxbacony);
        checkboxpet = this.findViewById(R.id.checkboxpet);
        checkbox_w_owner = this.findViewById(R.id.checkbox_w_owner);

        checkbox_air_condition = this.findViewById(R.id.checkbox_air_condition);
        checkbox_heater = this.findViewById(R.id.checkbox_heater);
        checkbox_curtain = this.findViewById(R.id.checkbox_curtain);
        checkboxfridge = this.findViewById(R.id.checkboxfridge);
        checkboxbed = this.findViewById(R.id.checkboxbed);
        checkboxwardrobe = this.findViewById(R.id.checkboxwardrobe);
        checkbox_washing_machine = this.findViewById(R.id.checkbox_washing_machine);
        checkboxsofa = this.findViewById(R.id.checkboxsofa);

        checkboxNam = this.findViewById(R.id.checkboxNam);
        checkboxNu = this.findViewById(R.id.checkboxNu);

        btn_create_room = this.findViewById(R.id.btn_create_room);

        uploadPicture1 = findViewById(R.id.imageViewP1);

        spinnerCity = findViewById(R.id.spinnerCity);
        spinnerDistrict = findViewById(R.id.spinnerDistrict);

        edtAddress = findViewById(R.id.edtAddress);
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
            Log.e("ImageCopy", "Error copying image", e);
            return sourceUri;
        }
    }

    public void saveImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(PostRoomActivity.this);
        builder.setCancelable(false);
        builder.setView(R.layout.progress_layout);
        AlertDialog dialog = builder.create();
        dialog.show();

        if (uri != null) {
            StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                    .child("roomImgage")
                    .child(Objects.requireNonNull(uri.getLastPathSegment()));

            storageReference.putFile(uri).addOnSuccessListener(taskSnapshot ->
                    storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        imageURL1 = uri.toString();
                        onClickPushData();  // Chỉ gọi khi đã có URL ảnh
                        dialog.dismiss();
                    }).addOnFailureListener(e -> {
                        Log.e("Firebase", "Lỗi khi lấy URL ảnh", e);
                        dialog.dismiss();
                    })
            ).addOnFailureListener(e -> {
                Log.e("Firebase", "Lỗi khi upload ảnh", e);
                dialog.dismiss();
            });

        } else if (photo != null) {  // Kiểm tra ảnh từ Bitmap
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageData = baos.toByteArray();

            String uniqueImageName = "image_" + System.currentTimeMillis() + ".jpg";
            StorageReference storageRef = FirebaseStorage.getInstance().getReference("roomImgage").child(uniqueImageName);

            storageRef.putBytes(imageData).addOnSuccessListener(taskSnapshot ->
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        imageURL1 = uri.toString();
                        onClickPushData();
                        dialog.dismiss();
                    }).addOnFailureListener(e -> {
                        Log.e("Firebase", "Lỗi khi lấy URL ảnh", e);
                        dialog.dismiss();
                    })
            ).addOnFailureListener(e -> {
                Log.e("Firebase", "Lỗi khi upload ảnh", e);
                dialog.dismiss();
            });

        } else {
            Log.e("Firebase", "Không có ảnh để tải lên.");
            dialog.dismiss();
        }
    }

    void onClickPushData() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
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
        if (checkboxNam.isChecked()) {
            if (!checkboxNu.isChecked()) {
                gender_room = "Nam";
            } else {
                gender_room = "Nam/Nữ";
            }
        } else {
            if (!checkboxNam.isChecked()) {
                gender_room = "Nữ";
            } else {
                gender_room = "Nam/Nữ";
            }
        }

        String title_room = String.valueOf(edtTitleRoom.getText());
        boolean isValid = true;
        if (isEmpty(edtTitleRoom)) {
            edtTitleRoom.setError("Vui lòng nhập tiêu đề bài đăng");
            isValid = false;
        } else {
            title_room = edtTitleRoom.getText().toString();
        }

        long deposit_room = 0;
        if (isEmpty(edtDeposit)) {
            edtDeposit.setError("Vui lòng nhập tiền cọc");
            isValid = false;
        } else {
            deposit_room = Long.parseLong(edtDeposit.getText().toString());
        }

        long price_room = 0;
        if (isEmpty(edtPrice)) {
            edtPrice.setError("Vui lòng nhập tiền cọc");
            isValid = false;
        } else {
            price_room = Long.parseLong(edtPrice.getText().toString());
        }

        int type_room = 0;
        String path = "Tro";
        if (radioGroup.getCheckedRadioButtonId() == R.id.radiobtnChungCu) {
            path = "ChungCuMini";
            type_room = 1;
        } else if (radioGroup.getCheckedRadioButtonId() == R.id.radiobtnTro) {
            path = "Tro";
        } else {
            isValid = false;
            Toast.makeText(this, "Vui lòng chọn loại phòng", Toast.LENGTH_SHORT).show();
        }

        DatabaseReference myRef = database.getReference("rooms/" + path);
        userCurrent = FirebaseAuth.getInstance().getCurrentUser();

        String area_room = edtArea.getText().toString();
        if (isEmpty(edtArea)) {
            edtArea.setError("Vui lòng nhập diện tích");
            isValid = false;
        } else {
            area_room = edtArea.getText().toString();
        }

        String phone = "";
        if (isEmpty(edtPhone)) {
            edtPhone.setError("Vui lòng nhập số điện thoại");
            isValid = false;
        } else {
            String regex = "^\\d{10}$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(edtPhone.getText().toString());
            if (matcher.matches()) {
                phone = edtPhone.getText().toString();
            } else {
                edtPhone.setError("Vui lòng nhập đúng định dạng số điện thoại");
                isValid = false;
            }
        }

        int floor = 1;
        if (isEmpty(edtFloor)) {
            edtFloor.setError("Vui lòng nhập số tầng");
            isValid = false;
        } else {
            floor = Integer.parseInt(edtFloor.getText().toString());
        }

        int person_in_room = 1;
        if (isEmpty(edtPerson)) {
            edtPerson.setError("Vui lòng nhập số người/phòng");
            isValid = false;
        } else {
            person_in_room = Integer.parseInt(edtPerson.getText().toString());
        }

        String description_room = "";
        if (isEmpty(edtDescriptionRoom)) {
            edtDescriptionRoom.setError("Vui lòng nhập số mô tả phòng chi tiết");
            isValid = false;
        } else {
            description_room = edtDescriptionRoom.getText().toString();
        }

        int park_slot = 1;
        if (isEmpty(edtPark)) {
            edtPark.setError("Vui lòng nhập số chỗ để xe trong 1 phòng");
            isValid = false;
        } else {
            park_slot = Integer.parseInt(edtPark.getText().toString());
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

        furnitures = new ArrayList<>();
        handleDataFurniture();

        extensions_room = new ArrayList<>();
        handleDataExtensions();

        ImagesRoomClass images = new ImagesRoomClass(imageURL1, imageURL1, imageURL1, imageURL1, "");

        if (furnitures.isEmpty()) {
            isValid = false;
            checkbox_air_condition.setError("Vui lòng chọn 1 món nội thất");
        }
        if (extensions_room.isEmpty()) {
            isValid = false;
            checkboxtoilet.setError("Vui lòng chọn 1 tiện ích");
        }

        if (isValid) {
            Room room = new Room(id_own_post, id_room, title_room, price_room, address, area_room, deposit_room, description_room, gender_room, park_slot,
                    person_in_room, status_room, type_room, phone, floor, images, furnitures, extensions_room,
                    Long.parseLong(edtElectric.getText().toString()), Long.parseLong(edtWater.getText().toString()), Long.parseLong(edtInternet.getText().toString()));

            //Xu ly cho firebase
            myRef.child(id_room).setValue(room).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    Toast.makeText(PostRoomActivity.this, "Đăng thông tin phòng thành công", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(PostRoomActivity.this, "Đăng thông tin phòng thất bại", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(PostRoomActivity.this, "Vui lòng nhập đầy đủ các trường dữ liệu", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleDataFurniture() {
        if (checkbox_air_condition.isChecked()) {
            furnitures.add(new FurnitureClass("checkbox_air_condition", checkbox_air_condition.getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-air-condittion.png?alt=media&token=85d235e6-f4f4-44b1-89f4-05bed51050a6"));
        }
        if (checkbox_heater.isChecked()) {
            furnitures.add(new FurnitureClass("checkbox_heater", checkbox_heater.getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-heater.png?alt=media&token=4c4871ff-ef6f-42bc-a480-3b60336b802c"));
        }
        if (checkbox_curtain.isChecked()) {
            furnitures.add(new FurnitureClass("checkbox_curtain", checkbox_curtain.getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-curtain.png?alt=media&token=400eb929-952b-4051-acc2-a26db74251ac"));
        }
        if (checkboxfridge.isChecked()) {
            furnitures.add(new FurnitureClass("checkboxfridge", checkboxfridge.getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-fridge.png?alt=media&token=deb2cded-5a02-464e-8e93-2672d7bc9b89"));
        }
        if (checkboxbed.isChecked()) {
            furnitures.add(new FurnitureClass("checkboxbed", checkboxbed.getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-bed.png?alt=media&token=9ed19798-ba14-4604-87d6-5f0224584f42"));
        }
        if (checkboxwardrobe.isChecked()) {
            furnitures.add(new FurnitureClass("checkboxwardrobe", checkboxwardrobe.getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-Wardrobe.png?alt=media&token=944da04f-03dd-4b8f-b627-27f1f8f11c9a"));
        }
        if (checkbox_washing_machine.isChecked()) {
            furnitures.add(new FurnitureClass("checkbox_washing_machine", checkbox_washing_machine.getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-washing-machine.png?alt=media&token=ee166ffd-2cb6-4d76-85a8-0a587effb2af"));
        }
        if (checkboxsofa.isChecked()) {
            furnitures.add(new FurnitureClass("checkboxsofa", checkboxsofa.getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-sofa.png?alt=media&token=f9bba804-271b-4740-b28c-d7d89d083d6f"));
        }
    }

    private void handleDataExtensions() {
        if (checkboxtoilet.isChecked()) {
            extensions_room.add(new ExtensionRoom_class("checkboxtoilet", checkboxtoilet.getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-toilet.png?alt=media&token=426b6597-5dc4-4182-887e-fbeb37d5acc0"));
        }
        if (checkboxfloor.isChecked()) {
            extensions_room.add(new ExtensionRoom_class("checkboxfloor", checkboxfloor.getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-ladder.png?alt=media&token=96975838-2519-4637-87ef-1c966b0f5308"));
        }
        if (checkbox_time_flex.isChecked()) {
            extensions_room.add(new ExtensionRoom_class("checkbox_time_flex", checkbox_time_flex.getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-time-flex.png?alt=media&token=c3d87c64-086b-43c8-b896-4d2777c2e7e5"));
        }
        if (checkboxfingerprint.isChecked()) {
            extensions_room.add(new ExtensionRoom_class("checkboxfingerprint", checkboxfingerprint.getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-finger-print.png?alt=media&token=8dccd0ac-ff93-4d1d-9f44-6db70a315853"));
        }
        if (checkboxbacony.isChecked()) {
            extensions_room.add(new ExtensionRoom_class("checkboxbacony", checkboxbacony.getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-ladder.png?alt=media&token=96975838-2519-4637-87ef-1c966b0f5308"));
        }
        if (checkboxpet.isChecked()) {
            extensions_room.add(new ExtensionRoom_class("checkboxpet", checkboxpet.getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-paw-pet.png?alt=media&token=8a649047-04d9-4421-a064-fca84b7f8f0d"));
        }
        if (checkbox_w_owner.isChecked()) {
            extensions_room.add(new ExtensionRoom_class("checkbox_w_owner", checkbox_w_owner.getText().toString(), "https://firebasestorage.googleapis.com/v0/b/my-application-67ef3.appspot.com/o/icon_png%2Fic-user.png?alt=media&token=db7d94aa-1a03-42f3-834a-4a3aec4c3866"));
        }
    }

    public void getDataForSpinnerDistrict() {
        districts.clear();
        DatabaseReference databaseReferenceDistrict = FirebaseDatabase.getInstance().getReference();
        databaseReferenceDistrict.child(path).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot childSnap : snapshot.getChildren()) {
                    String DistrictName = childSnap.child("name").getValue(String.class);
                    districts.add(DistrictName);
                }
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(PostRoomActivity.this, android.R.layout.simple_spinner_item, districts);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDistrict.setAdapter(spinnerAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void getDataForSpinnerCity() {
        cities.clear();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        path = "city";
        databaseReference.child(path).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot childSnap : snapshot.getChildren()) {
                    String CityName = childSnap.child("name").getValue(String.class);
                    cities.add(CityName);
                }
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(PostRoomActivity.this, android.R.layout.simple_spinner_item, cities);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCity.setAdapter(spinnerAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    boolean isEmpty(EditText text) {
        CharSequence str = text.getText().toString();
        return TextUtils.isEmpty(str);
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
        dialog.setContentView(R.layout.dialog_choose_uploadimg);

        ImageView cancelButton;
        pickImgAlbum = dialog.findViewById(R.id.pickImgAlbum);
        pickImgCamera = dialog.findViewById(R.id.pickImgCamera);
        cancelButton = dialog.findViewById(R.id.cancelButton);

        pickImgAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissions();
                Intent photoPicker = new Intent(Intent.ACTION_PICK);
                photoPicker.setType("image/*");
                activityResultLauncher.launch(photoPicker);
            }
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

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.setCancelable(true);
    }
}