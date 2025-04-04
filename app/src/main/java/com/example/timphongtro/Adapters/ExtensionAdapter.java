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
import com.example.timphongtro.Models.ExtensionRoom_class;
import com.example.timphongtro.R;

import java.util.ArrayList; 

public class ExtensionAdapter extends RecyclerView.Adapter<ExtensionAdapter.ViewHolderExtension>{
    Context context;
    ArrayList<ExtensionRoom_class> list;

    public ExtensionAdapter(Context context, ArrayList<ExtensionRoom_class> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolderExtension onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.extentions_view_holder,parent,false);
        return new ViewHolderExtension(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderExtension holder, int position) {
        holder.titleExtension.setText(list.get(holder.getAdapterPosition()).getName());
        if(!"".equals(list.get(holder.getAdapterPosition()).getImg())){
            Glide.with(context)
                    .load(list.get(holder.getAdapterPosition()).getImg())
                    .apply(new RequestOptions()
                            .centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)) // để lưu ảnh trong bộ nhớ cache.
                    .into(holder.imageViewExtension);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
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
