package com.example.timphongtro.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;

import com.example.timphongtro.BroadcastReceiver.NetworkChangeReceiver;
import com.example.timphongtro.Fragments.HomeFragment;
import com.example.timphongtro.Fragments.FollowFragment;
import com.example.timphongtro.Fragments.ProfileFragment;
import com.example.timphongtro.Fragments.ServiceFragment;
import com.example.timphongtro.R;
import com.example.timphongtro.Utils.AuthUtils;
import com.example.timphongtro.databinding.ActivityMainBinding;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.appcompat.app.AlertDialog;

public class MainActivity extends AppCompatActivity {
    private NetworkChangeReceiver networkChangeReceiver;
    private boolean isReceiverRegistered = false;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        networkChangeReceiver = new NetworkChangeReceiver();

        setupNavigation();
    }

    private void setupNavigation() {
        replaceFragment(new HomeFragment());
        binding.bottomNavigationView.setBackground(null);

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.home) {
                replaceFragment(new HomeFragment());
                return true;
            } else if (item.getItemId() == R.id.service) {
                replaceFragment(new ServiceFragment());
                return true;
            } else if (item.getItemId() == R.id.notification) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    replaceFragment(new FollowFragment());
                } else {
                    // Thay thế bằng AuthUtils
                    AuthUtils.showLoginRequiredDialog(this, "Theo dõi", "xem tính năng");
                    return false;
                }
                return true;
            } else if (item.getItemId() == R.id.profile) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    replaceFragment(new ProfileFragment());
                } else {
                    // Thay thế bằng AuthUtils
                    AuthUtils.showLoginRequiredDialog(this, "Hồ sơ", "xem tính năng");
                    return false;
                }
                return true;
            }
            return true;
        });

        binding.fab.setOnClickListener(v -> showBottomDialog());
    }

    private void showBottomDialog() {
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_bottom_fab);

        dialog.findViewById(R.id.house).setOnClickListener(v -> {
            dialog.dismiss();
            Intent search = new Intent(this, SearchActivity.class);
            startActivity(search);
        });
        dialog.findViewById(R.id.contract).setOnClickListener(v -> {
            dialog.dismiss();
            // Kiểm tra đăng nhập trước khi đăng bài
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                Intent post = new Intent(this, PostActivity.class);
                startActivity(post);
            } else {
                AuthUtils.showLoginRequiredDialog(this, "đăng tin phòng trọ", "đăng");
            }
        });
        dialog.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());

        // Các phần còn lại giữ nguyên
        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.setCancelable(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isReceiverRegistered) {
            registerReceiver(networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            isReceiverRegistered = true;
        }
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(binding.frameLayout.getId(), fragment);
        fragmentTransaction.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isReceiverRegistered) {
            unregisterReceiver(networkChangeReceiver);
            isReceiverRegistered = false;
        }
    }
}