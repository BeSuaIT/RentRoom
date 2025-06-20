package com.example.timphongtro.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
 
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.timphongtro.Models.Furniture;
import com.example.timphongtro.R;

import java.util.ArrayList; 

public class FurnitureAdapter extends RecyclerView.Adapter<FurnitureAdapter.ViewHolderFurniture> {
    Context context;
    ArrayList<Furniture> list;

    public FurnitureAdapter(Context context, ArrayList<Furniture> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolderFurniture onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.view_holder_furniture, parent, false);
        return new FurnitureAdapter.ViewHolderFurniture(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderFurniture holder, int position) {
        holder.titleFurniture.setText(list.get(holder.getAdapterPosition()).getName());
        if(!"".equals(list.get(holder.getAdapterPosition()).getImg())){
            Glide.with(context)
                    .load(list.get(holder.getAdapterPosition()).getImg())
                    .apply(new RequestOptions()
                            .centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(holder.imageViewFuniture);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolderFurniture extends RecyclerView.ViewHolder {

        TextView titleFurniture;
        ImageView imageViewFuniture;

        public ViewHolderFurniture(@NonNull View itemView) {
            super(itemView);
            titleFurniture = itemView.findViewById(R.id.titleFurniture);
            imageViewFuniture = itemView.findViewById(R.id.imageViewFuniture);
        }
    }

}
