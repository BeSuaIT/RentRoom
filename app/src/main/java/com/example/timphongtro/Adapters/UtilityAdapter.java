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
import com.example.timphongtro.Models.Utility;
import com.example.timphongtro.R;

import java.util.ArrayList; 

public class UtilityAdapter extends RecyclerView.Adapter<UtilityAdapter.ViewHolderExtension>{
    Context context;
    ArrayList<Utility> utilities;

    public UtilityAdapter(Context context, ArrayList<Utility> utilities) {
        this.context = context;
        this.utilities = utilities;
    }

    @NonNull
    @Override
    public ViewHolderExtension onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.view_holder_extension,parent,false);
        return new ViewHolderExtension(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderExtension holder, int position) {
        holder.titleExtension.setText(utilities.get(holder.getAdapterPosition()).getName());
        if(!"".equals(utilities.get(holder.getAdapterPosition()).getImg())){
            Glide.with(context)
                    .load(utilities.get(holder.getAdapterPosition()).getImg())
                    .apply(new RequestOptions()
                            .centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)) // để lưu ảnh trong bộ nhớ cache.
                    .into(holder.imageViewExtension);
        }
    }

    @Override
    public int getItemCount() {
        return utilities.size();
    }

    public static class ViewHolderExtension extends RecyclerView.ViewHolder {
        ImageView imageViewExtension;
        TextView titleExtension;

        public ViewHolderExtension(@NonNull View itemView) {
            super(itemView);
            imageViewExtension = itemView.findViewById(R.id.imageViewExtension);
            titleExtension = itemView.findViewById(R.id.titleExtension);
        }
    }
}
