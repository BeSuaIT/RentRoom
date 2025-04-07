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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.timphongtro.Activities.BillActivity;
import com.example.timphongtro.Activities.CartActivity;
import com.example.timphongtro.Activities.EditProfileActivity;
import com.example.timphongtro.Activities.HistoryActivity;
import com.example.timphongtro.Activities.LoginActivity;
import com.example.timphongtro.Activities.ManagePostActivity;
import com.example.timphongtro.Activities.UserActivity;
import com.example.timphongtro.Activities.scheduleVisitRoomActivity;
import com.example.timphongtro.R;
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
    private String name;
    private DatabaseReference databaseReference;
    private TextView nameTextView, profileInitialTextView, emailTextView,
            signOutButton, manageRoomsButton, scheduleButton,
            historyButton, myProfileButton, billButton, cartButton;
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

        nameTextView = view.findViewById(R.id.txtviewInfo);
        emailTextView = view.findViewById(R.id.txtviewEmail);

        signOutButton = view.findViewById(R.id.signOutButton);
        manageRoomsButton = view.findViewById(R.id.manageRoomsButton);
        scheduleButton = view.findViewById(R.id.scheduleButton);
        historyButton = view.findViewById(R.id.historyButton);
        billButton = view.findViewById(R.id.billButton);
        cartButton = view.findViewById(R.id.cartButton);
        profileInitialTextView = view.findViewById(R.id.profileInitialTextView);
        profileCard = view.findViewById(R.id.profileCard);
        myProfileButton = view.findViewById(R.id.myProfileButton);

        if(firebaseUser != null){
            myProfileButton.setOnClickListener(v -> {
                Intent userProfileIntent = new Intent(getActivity().getApplicationContext(), UserActivity.class);
                userProfileIntent.putExtra("id_own_post", firebaseUser.getUid());
                startActivity(userProfileIntent);
            });
            historyButton.setOnClickListener(v -> {
                Intent historyIntent = new Intent(getActivity(), HistoryActivity.class);
                startActivity(historyIntent);
            });

            manageRoomsButton.setOnClickListener(v -> {
                if (firebaseUser != null) {
                    Intent managePostIntent = new Intent(getActivity(), ManagePostActivity.class);
                    startActivity(managePostIntent);
                } else {
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(intent);
                }
            });

            cartButton.setOnClickListener(v -> {
                Intent cartActivityIntent = new Intent(getActivity(), CartActivity.class);
                startActivity(cartActivityIntent);
            });

            billButton.setOnClickListener(v -> {
                Intent billActivityIntent = new Intent(getActivity(), BillActivity.class);
                startActivity(billActivityIntent);
            });

            scheduleButton.setOnClickListener(v -> {
                Intent scheduleIntent = new Intent(getActivity(), scheduleVisitRoomActivity.class);
                startActivity(scheduleIntent);
            });

            profileCard.setOnClickListener(v -> {
                Intent editProfileIntent = new Intent(getActivity(), EditProfileActivity.class);
                startActivity(editProfileIntent);
            });

            signOutButton.setOnClickListener(v -> {
                CredentialManager credentialManager = CredentialManager.create(requireContext());
                ClearCredentialStateRequest request = new ClearCredentialStateRequest();
                CancellationSignal cancellationSignal = new CancellationSignal();

                credentialManager.clearCredentialStateAsync(
                        request,
                        cancellationSignal,
                        getActivity().getMainExecutor(),
                        new CredentialManagerCallback<Void, ClearCredentialException>() {
                            @Override
                            public void onResult(Void unused) {
                                if (com.facebook.AccessToken.getCurrentAccessToken() != null) {
                                    com.facebook.login.LoginManager.getInstance().logOut();
                                }

                                firebaseAuth.signOut();

                                Intent loginActivityIntent = new Intent(getActivity(), LoginActivity.class);
                                loginActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(loginActivityIntent);
                                if (getActivity() != null) {
                                    getActivity().finish();
                                }
                            }

                            @Override
                            public void onError(@NonNull ClearCredentialException e) {
                                if (com.facebook.AccessToken.getCurrentAccessToken() != null) {
                                    com.facebook.login.LoginManager.getInstance().logOut();
                                }
                                firebaseAuth.signOut();

                                Intent loginActivityIntent = new Intent(getActivity(), LoginActivity.class);
                                loginActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(loginActivityIntent);
                                if (getActivity() != null) {
                                    getActivity().finish();
                                }
                            }
                        }
                );
            });

            firebaseDatabase = FirebaseDatabase.getInstance();

            databaseReference = firebaseDatabase.getReference("Users/" + firebaseUser.getUid());
            databaseReference.child("name").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        name = snapshot.getValue(String.class);
                        nameTextView.setText(name);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });


            if (firebaseUser != null) {
                String email = firebaseUser.getEmail();
                emailTextView.setText(email);
                databaseReference = firebaseDatabase.getReference("Users/" + firebaseUser.getUid());
                databaseReference.child("name").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            name = snapshot.getValue(String.class);
                            if (!"".equals(name)) {
                                nameTextView.setText(name);
                                profileInitialTextView.setText(getFirstLetter(name));
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }

    }

    public static String getFirstLetter(String input) {
        String[] words = input.split(" ");
        StringBuilder result = new StringBuilder();

        if (words.length == 1) {
            result.append(words[0].charAt(0));
        } else {
            for (int i = 0; i < 2; i++) {
                result.append(words[i].charAt(0));
            }
        }

        return result.toString().toUpperCase();
    }
}