package com.example.timphongtro.Fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.timphongtro.Activity.CartActivity;
import com.example.timphongtro.Activity.LoginActivity;
import com.example.timphongtro.Activity.ServiceActivity;
import com.example.timphongtro.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ServiceFragment extends Fragment {

    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private FirebaseUser user = firebaseAuth.getCurrentUser();
    private LinearLayout chothuenoithat, tuvanthietkephong, suachuadiennuoc, giatla, doibinhnuoc, doibinhga;
    private ImageView button_cart;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_service, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        chothuenoithat = view.findViewById(R.id.chothuenoithat);
        tuvanthietkephong = view.findViewById(R.id.tuvanthietkephong);
        suachuadiennuoc = view.findViewById(R.id.suachuadiennuoc);
        giatla = view.findViewById(R.id.giatla);
        doibinhnuoc = view.findViewById(R.id.doibinhnuoc);
        doibinhga = view.findViewById(R.id.doibinhga);
        button_cart = view.findViewById(R.id.button_cart);

        button_cart.setOnClickListener(v -> {
            Intent intent;
            if (user != null){
                intent = new Intent(getContext(), CartActivity.class);
            } else {
                intent = new Intent(getContext(), LoginActivity.class);
            }
            startActivity(intent);

        });

        chothuenoithat.setOnClickListener(v -> openServiceActivity("chothuenoithat"));

        tuvanthietkephong.setOnClickListener(v -> openServiceActivity("tuvanthietkephong"));

        suachuadiennuoc.setOnClickListener(v -> openServiceActivity("suachuadiennuoc"));
        giatla.setOnClickListener(v -> openServiceActivity("giatla"));

        doibinhnuoc.setOnClickListener(v -> openServiceActivity("doibinhnuoc"));

        doibinhga.setOnClickListener(v -> openServiceActivity("doibinhga"));
    }

    private void openServiceActivity(String item) {
        Intent intent = new Intent(getContext(), ServiceActivity.class);
        intent.putExtra("item", item);
        startActivity(intent);
    }
}