package com.example.timphongtro.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.timphongtro.R;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.List;

public class ZoomImageAdapter extends RecyclerView.Adapter<ZoomImageAdapter.ZoomImageViewHolder> {
    private List<String> images;
    private Context context;

    public ZoomImageAdapter(Context context, List<String> images) {
        this.context = context;
        this.images = images;
    }

    @NonNull
    @Override
    public ZoomImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_zoom_image, parent, false);
        return new ZoomImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ZoomImageViewHolder holder, int position) {
        String imageUrl = images.get(position);
        Glide.with(context)
            .load(imageUrl)
            .into(holder.photoView);
    }

    @Override
    public int getItemCount() {
        return images != null ? images.size() : 0;
    }

    public static class ZoomImageViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView;

        public ZoomImageViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.photoView);
        }
    }
}