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
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;

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
import com.google.firebase.auth.FirebaseAuth.AuthStateListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private NetworkChangeReceiver networkChangeReceiver;
    private boolean isReceiverRegistered = false;
    private ActivityMainBinding binding;
    private AuthStateListener authStateListener;

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

        checkCurrentUserAndSetup();
        setupAuthListener();
        handleIncomingIntent();
    }

    private void handleIncomingIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("selected_tab")) {
            String selectedTab = intent.getStringExtra("selected_tab");
            
            if ("service".equals(selectedTab)) {
                replaceFragment(new ServiceFragment());
                binding.bottomNavigationView.setSelectedItemId(R.id.service);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent();
    }

    private void checkCurrentUserAndSetup() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            AuthUtils.checkUserExistsOnStartup(this, currentUser, exists -> {
                setupNavigation();
            });
        } else {
            setupNavigation();
        }
    }

    private void setupAuthListener() {
        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                runOnUiThread(() -> {
                    replaceFragment(new HomeFragment());
                    binding.bottomNavigationView.setSelectedItemId(R.id.home);
                });
            }
        };
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
                    AuthUtils.isUserExists(currentUser, exists -> {
                        if (exists) {
                            replaceFragment(new FollowFragment());
                        } else {
                            AuthUtils.clearAllLoginData(this);
                            AuthUtils.showLoginRequiredDialog(this, "Theo dõi", "xem tính năng");
                        }
                    });
                } else {
                    AuthUtils.showLoginRequiredDialog(this, "Theo dõi", "xem tính năng");
                    return false;
                }
                return true;
            } else if (item.getItemId() == R.id.profile) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    AuthUtils.isUserExists(currentUser, exists -> {
                        if (exists) {
                            replaceFragment(new ProfileFragment());
                        } else {
                            AuthUtils.clearAllLoginData(this);
                            AuthUtils.showLoginRequiredDialog(this, "Hồ sơ", "xem tính năng");
                        }
                    });
                } else {
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

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        LinearLayout guestSection = dialog.findViewById(R.id.guest_section);
        LinearLayout tenantSection = dialog.findViewById(R.id.tenant_section);
        LinearLayout landlordSection = dialog.findViewById(R.id.landlord_section);

        if (currentUser != null) {
            AuthUtils.isUserExists(currentUser, exists -> {
                if (exists) {
                    guestSection.setVisibility(View.GONE);
                    getUserRoleAndSetupUI(currentUser.getUid(), dialog, tenantSection, landlordSection);
                } else {
                    AuthUtils.clearAllLoginData(this);
                    guestSection.setVisibility(View.VISIBLE);
                    tenantSection.setVisibility(View.GONE);
                    landlordSection.setVisibility(View.GONE);
                    setupGuestClickListeners(dialog);
                }
            });
        } else {
            guestSection.setVisibility(View.VISIBLE);
            tenantSection.setVisibility(View.GONE);
            landlordSection.setVisibility(View.GONE);
            setupGuestClickListeners(dialog);
        }

        dialog.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.setCancelable(true);
    }

    private void getUserRoleAndSetupUI(String userId, BottomSheetDialog dialog, 
                                      LinearLayout tenantSection, LinearLayout landlordSection) {
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String userRole = snapshot.child("role").getValue(String.class);
                    
                    runOnUiThread(() -> {
                        if ("Chủ trọ".equals(userRole)) {
                            tenantSection.setVisibility(View.VISIBLE);
                            landlordSection.setVisibility(View.VISIBLE);
                            setupLandlordClickListeners(dialog);
                        } else {
                            tenantSection.setVisibility(View.VISIBLE);
                            landlordSection.setVisibility(View.GONE);
                            setupTenantClickListeners(dialog);
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        tenantSection.setVisibility(View.VISIBLE);
                        landlordSection.setVisibility(View.GONE);
                        setupTenantClickListeners(dialog);
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                runOnUiThread(() -> {
                    tenantSection.setVisibility(View.VISIBLE);
                    landlordSection.setVisibility(View.GONE);
                    setupTenantClickListeners(dialog);
                });
            }
        });
    }

    private void setupTenantClickListeners(BottomSheetDialog dialog) {
        dialog.findViewById(R.id.btn_cart).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, CartManagementActivity.class));
        });

        dialog.findViewById(R.id.btn_tenant_contracts).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, ContractManagementActivity.class);
            startActivity(intent);
        });
    }

    private void setupLandlordClickListeners(BottomSheetDialog dialog) {
        dialog.findViewById(R.id.btn_cart).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, CartManagementActivity.class));
        });
        dialog.findViewById(R.id.btn_tenant_contracts).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, ContractManagementActivity.class);
            startActivity(intent);
        });
        dialog.findViewById(R.id.btn_post_room).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, AddPostActivity.class));
        });
        dialog.findViewById(R.id.btn_manage_posts).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, PostManagementActivity.class));
        });
        dialog.findViewById(R.id.btn_add_contract).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, AddContractActivity.class));
        });
    }

    private void setupGuestClickListeners(BottomSheetDialog dialog) {
        dialog.findViewById(R.id.btn_login).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, LoginActivity.class));
        });

        dialog.findViewById(R.id.btn_register).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            AuthUtils.checkUserExistsOnStartup(this, currentUser, exists -> {
                if (!exists) {
                    runOnUiThread(() -> {
                        replaceFragment(new HomeFragment());
                        binding.bottomNavigationView.setSelectedItemId(R.id.home);
                    });
                }
            });
        }
        
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