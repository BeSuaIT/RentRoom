package com.example.timphongtro.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.timphongtro.Activities.CartActivity;
import com.example.timphongtro.Activities.ServiceActivity;
import com.example.timphongtro.R;
import com.example.timphongtro.Utils.AuthUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ServiceFragment extends Fragment {
    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    private LinearLayout chothuenoithat, tuvanthietkephong, suachuadiennuoc, 
                        giatla, doibinhnuoc, doibinhga;
    private ImageView button_cart;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_service, container, false);
        initViews(view);
        setupClickListeners();
        return view;
    }

    private void initViews(View view) {
        chothuenoithat = view.findViewById(R.id.chothuenoithat);
        tuvanthietkephong = view.findViewById(R.id.tuvanthietkephong);
        suachuadiennuoc = view.findViewById(R.id.suachuadiennuoc);
        giatla = view.findViewById(R.id.giatla);
        doibinhnuoc = view.findViewById(R.id.doibinhnuoc);
        doibinhga = view.findViewById(R.id.doibinhga);
        button_cart = view.findViewById(R.id.button_cart);
    }

    private void setupClickListeners() {
        button_cart.setOnClickListener(v -> {
            if (user != null) {
                startActivity(new Intent(requireContext(), CartActivity.class));
            } else {
                AuthUtils.showLoginRequiredDialog(this, "giỏ hàng", "xem");
            }
        });

        chothuenoithat.setOnClickListener(v -> openServiceActivity("chothuenoithat"));
        tuvanthietkephong.setOnClickListener(v -> openServiceActivity("tuvanthietkephong"));
        suachuadiennuoc.setOnClickListener(v -> openServiceActivity("suachuadiennuoc"));
        giatla.setOnClickListener(v -> openServiceActivity("giatla"));
        doibinhnuoc.setOnClickListener(v -> openServiceActivity("doibinhnuoc"));
        doibinhga.setOnClickListener(v -> openServiceActivity("doibinhga"));
    }

    private void openServiceActivity(String item) {
        if (isAdded()) {
            Intent intent = new Intent(requireContext(), ServiceActivity.class);
            intent.putExtra("item", item);
            startActivity(intent);
        }
    }
}