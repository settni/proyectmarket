package com.diegodev.marketplace.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.diegodev.marketplace.R;

import java.util.List;

/**
 * Adaptador para mostrar la galería de imágenes de un producto
 * dentro de un ViewPager2 (Carrusel).
 */
public class SliderImagenAdapter extends RecyclerView.Adapter<SliderImagenAdapter.SliderViewHolder> {

    private final Context context;
    private final List<String> imageUrls;

    public SliderImagenAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_slider_imagen, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        String url = imageUrls.get(position);

        Glide.with(context)
                .load(url)
                .centerCrop()
                .placeholder(R.drawable.agregar_img)
                .error(R.drawable.error)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_slider_image);
        }
    }
}