package com.example.timphongtro.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
 
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.timphongtro.Models.District;
import com.example.timphongtro.Activities.SearchActivity;
import com.example.timphongtro.R;

import java.util.ArrayList;

public class DistrictAdapter extends RecyclerView.Adapter<DistrictAdapter.MyViewHolder> {

    Context context; 

    ArrayList<District> list;

    public DistrictAdapter(Context context, ArrayList<District> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(context).inflate(R.layout.district_view_holder,parent,false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        District district = list.get(position);
        Glide.with(context).load(district.getImg_district()).centerCrop().into(holder.img_district);
        holder.name.setText(district.getName());
        holder.cardViewDistrict.setOnClickListener(v -> {
            Intent searchview = new Intent(context, SearchActivity.class);
            searchview.putExtra("District",holder.name.getText());
            context.startActivity(searchview);

        });
    }



    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{

        TextView name;
        ImageView img_district;
        CardView cardViewDistrict;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            img_district = itemView.findViewById(R.id.img_district);
            name = itemView.findViewById(R.id.textviewholder);
            cardViewDistrict = itemView.findViewById(R.id.cardViewDistrict);
        }
    }
}

