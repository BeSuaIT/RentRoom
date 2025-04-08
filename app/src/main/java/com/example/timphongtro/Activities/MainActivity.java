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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.timphongtro.BroadcastReceiver.NetworkChangeReceiver;
import com.example.timphongtro.Fragments.HomeFragment;
import com.example.timphongtro.Fragments.FollowFragment;
import com.example.timphongtro.Fragments.ProfileFragment;
import com.example.timphongtro.Fragments.ServiceFragment;
import com.example.timphongtro.R;
import com.example.timphongtro.databinding.ActivityMainBinding;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class MainActivity extends AppCompatActivity {
    NetworkChangeReceiver networkChangeReceiver;
    private boolean isReceiverRegistered = false;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize view binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configure Google Sign-In
        new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        // Initialize network receiver
        networkChangeReceiver = new NetworkChangeReceiver();

        // Set initial fragment and bottom navigation setup
        setupNavigation();
    }

    private void setupNavigation() {
        // Set initial fragment
        replaceFragment(new HomeFragment());

        // Remove background from bottom navigation
        binding.bottomNavigationView.setBackground(null);

        // Handle bottom navigation item selection
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.home) {
                replaceFragment(new HomeFragment());
            } else if (item.getItemId() == R.id.service) {
                replaceFragment(new ServiceFragment());
            } else if (item.getItemId() == R.id.notification) {
                replaceFragment(new FollowFragment());
            } else if (item.getItemId() == R.id.profile) {
                replaceFragment(new ProfileFragment());
            }
            return true;
        });

        binding.fab.setOnClickListener(v -> showBottomDialog());
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

    private void showBottomDialog() {
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_bottom_fab);

        LinearLayout house = dialog.findViewById(R.id.house);
        LinearLayout groupusers = dialog.findViewById(R.id.pickImgAlbum);
        LinearLayout contract = dialog.findViewById(R.id.contract);
        ImageView cancelButton = dialog.findViewById(R.id.cancelButton);

        house.setOnClickListener(v -> {
            dialog.dismiss();
            Intent search = new Intent(this, SearchActivity.class);
            startActivity(search);
        });

        groupusers.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(MainActivity.this, "In Developing", Toast.LENGTH_SHORT).show();
        });

        contract.setOnClickListener(v -> {
            dialog.dismiss();
                Intent post = new Intent(this, PostRoomActivity.class);
                startActivity(post);
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.setCancelable(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister network receiver
        if (isReceiverRegistered) {
            unregisterReceiver(networkChangeReceiver);
            isReceiverRegistered = false;
        }
    }
}