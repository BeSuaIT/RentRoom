package com.example.timphongtro.Fragments;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.fragment.app.Fragment;

import android.os.CancellationSignal;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.timphongtro.Activities.BillManagementActivity;
import com.example.timphongtro.Activities.CartManagementActivity;
import com.example.timphongtro.Activities.ContractManagementActivity;
import com.example.timphongtro.Activities.EditProfileActivity;
import com.example.timphongtro.Activities.HistoryManagementActivity;
import com.example.timphongtro.Activities.LoginActivity;
import com.example.timphongtro.Activities.MainActivity;
import com.example.timphongtro.Activities.PostManagementActivity;
import com.example.timphongtro.Activities.UserActivity;
import com.example.timphongtro.Activities.MeetingManagementActivity;
import com.example.timphongtro.R;
import com.example.timphongtro.Models.User;
import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private TextView nameTextView, emailTextView,
            signOutButton, manageRoomsButton, manageContractsButton, scheduleButton,
            historyButton, myProfileButton, billButton, cartButton;
    private TextView profileInitialTextView;
    private ShapeableImageView profileImageView;
    private CardView profileCard;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null) {
            navigateToLogin();
            return;
        }

        initializeViews(view);
        setupClickListeners();
        loadUserData();
    }

    private void initializeViews(View view) {
        nameTextView = view.findViewById(R.id.txtviewInfo);
        emailTextView = view.findViewById(R.id.txtviewEmail);
        signOutButton = view.findViewById(R.id.signOutButton);
        manageRoomsButton = view.findViewById(R.id.manageRoomsButton);
        manageContractsButton = view.findViewById(R.id.manageContractsButton);
        scheduleButton = view.findViewById(R.id.scheduleButton);
        historyButton = view.findViewById(R.id.historyButton);
        billButton = view.findViewById(R.id.billButton);
        cartButton = view.findViewById(R.id.cartButton);
        profileInitialTextView = view.findViewById(R.id.profileInitialTextView);
        profileImageView = view.findViewById(R.id.profileImageView);
        profileCard = view.findViewById(R.id.profileCard);
        myProfileButton = view.findViewById(R.id.myProfileButton);
    }

    private void setupClickListeners() {
        myProfileButton.setOnClickListener(v -> {
            Intent userProfileIntent = new Intent(requireActivity(), UserActivity.class);
            userProfileIntent.putExtra("id_own_post", firebaseUser.getUid());
            startActivity(userProfileIntent);
        });
        historyButton.setOnClickListener(v -> startActivity(new Intent(requireActivity(), HistoryManagementActivity.class)));
        manageRoomsButton.setOnClickListener(v -> startActivity(new Intent(requireActivity(), PostManagementActivity.class)));
        manageContractsButton.setOnClickListener(v -> startActivity(new Intent(requireActivity(), ContractManagementActivity.class)));
        cartButton.setOnClickListener(v -> startActivity(new Intent(requireActivity(), CartManagementActivity.class)));
        billButton.setOnClickListener(v -> startActivity(new Intent(requireActivity(), BillManagementActivity.class)));
        scheduleButton.setOnClickListener(v -> startActivity(new Intent(requireActivity(), MeetingManagementActivity.class)));
        profileCard.setOnClickListener(v -> startActivity(new Intent(requireActivity(), EditProfileActivity.class)));
        signOutButton.setOnClickListener(v -> handleSignOut());
    }

    private void navigateToLogin() {
        Intent loginIntent = new Intent(requireActivity(), LoginActivity.class);
        loginIntent.putExtra("previous_activity", requireActivity().getClass().getName());
        startActivity(loginIntent);
        requireActivity().getSupportFragmentManager().beginTransaction()
            .replace(R.id.frame_layout, new HomeFragment())
            .commit();
    }

    private void loadUserData() {
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users/" + firebaseUser.getUid());
        
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    nameTextView.setText(user.getName());
                    emailTextView.setText(user.getEmail());
                    updateProfileImage(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void handleSignOut() {
        CredentialManager credentialManager = CredentialManager.create(requireContext());
        ClearCredentialStateRequest request = new ClearCredentialStateRequest();
        CancellationSignal cancellationSignal = new CancellationSignal();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            credentialManager.clearCredentialStateAsync(
                    request,
                    cancellationSignal,
                    requireActivity().getMainExecutor(),
                    new CredentialManagerCallback<Void, ClearCredentialException>() {
                        @Override
                        public void onResult(Void unused) {
                            performSignOut();
                        }

                        @Override
                        public void onError(@NonNull ClearCredentialException e) {
                            performSignOut();
                        }
                    }
            );
        } else {
            performSignOut();
        }
    }

    private void performSignOut() {
        if (AccessToken.getCurrentAccessToken() != null) {
            LoginManager.getInstance().logOut();
        }

        firebaseAuth.signOut();

        Intent mainActivityIntent = new Intent(requireActivity(), MainActivity.class);
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainActivityIntent);
        requireActivity().finish();
    }

    private void updateProfileImage(User user) {
        String avatarUrl = user.getAvatarUrl();
        if (!TextUtils.isEmpty(avatarUrl)) {
            profileInitialTextView.setVisibility(View.GONE);
            profileImageView.setVisibility(View.VISIBLE);

            Glide.with(requireContext())
                    .load(avatarUrl)
                    .placeholder(R.drawable.avatar)
                    .error(R.drawable.avatar)
                    .into(profileImageView);
        } else {
            profileImageView.setVisibility(View.GONE);
            profileInitialTextView.setVisibility(View.VISIBLE);
            profileInitialTextView.setText(getFirstLetter(user.getName()));
        }
    }

    public static String getFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String[] words = input.split(" ");
        StringBuilder result = new StringBuilder();

        if (words.length == 1) {
            if (!words[0].isEmpty()) {
                result.append(words[0].charAt(0));
            }
        } else {
            for (int i = 0; i < Math.min(words.length, 2); i++) {
                if (!words[i].isEmpty()) {
                    result.append(words[i].charAt(0));
                }
            }
        }
        return result.toString().toUpperCase();
    }
}