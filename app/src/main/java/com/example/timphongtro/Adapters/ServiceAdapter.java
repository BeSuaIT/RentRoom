package com.example.timphongtro.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.timphongtro.Activities.LoginActivity;
import com.example.timphongtro.Activities.ServiceDetailActivity;
import com.example.timphongtro.Models.Service;
import com.example.timphongtro.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

    private ArrayList<Service> serviceList;
    private Context context;
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private FirebaseUser user = firebaseAuth.getCurrentUser();

    public ServiceAdapter(Context context, ArrayList<Service> serviceList) {
        this.context = context;
        this.serviceList = serviceList;

    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.service_view_holder, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceAdapter.ServiceViewHolder holder, int position) {
        DecimalFormat decimalFormat = new DecimalFormat("#,###.###");
        decimalFormat.setDecimalSeparatorAlwaysShown(false);

        Service service = serviceList.get(position);
        holder.name.setText(service.getTitle());
        holder.price.setText(decimalFormat.format(service.getPrice()) + " VNĐ");

        Glide.with(context).load(service.getImages().get(0)).centerCrop().into(holder.image);

        DatabaseReference sellerRef = FirebaseDatabase.getInstance().getReference("Users/" + service.getId_seller());
        sellerRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String sellerName = snapshot.getValue(String.class);
                    holder.seller.setText(sellerName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.seller.setText("Unknown Seller");
            }
        });

        holder.btn_add.setOnClickListener(v -> {
            if (user != null) {
                String userID = user.getUid();
                String sellerId = service.getId_seller();
                DatabaseReference cartRef = FirebaseDatabase.getInstance()
                        .getReference("Carts")
                        .child(userID)
                        .child(sellerId);  // Group by seller first

                cartRef.child(service.getServiceId()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            int currentAmount = dataSnapshot.getValue(Integer.class);
                            cartRef.child(service.getServiceId()).setValue(currentAmount + 1);
                        } else {
                            cartRef.child(service.getServiceId()).setValue(1);
                        }
                        Toast.makeText(context, service.getTitle() + " added!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(context, "Failed to add item", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Intent intent = new Intent(context, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });

        holder.cardService.setOnClickListener(v -> {
            Intent intent = new Intent(context, ServiceDetailActivity.class);
            intent.putExtra("ServiceData", service.toString());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return serviceList.size();
    }

    public static class ServiceViewHolder extends RecyclerView.ViewHolder {
        TextView name, price, seller;
        ImageView image;
        Button btn_add;
        LinearLayout cardService;

        public ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.service_name);
            price = itemView.findViewById(R.id.service_price);
            seller = itemView.findViewById(R.id.service_seller);
            image = itemView.findViewById(R.id.service_img);
            btn_add = itemView.findViewById(R.id.btn_buynow);
            cardService = itemView.findViewById(R.id.cardService);
        }
    }
}