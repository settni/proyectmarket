package com.diegodev.marketplace.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.diegodev.marketplace.DetalleProductoActivity;
import com.diegodev.marketplace.Publicar;
import com.diegodev.marketplace.R;
import com.diegodev.marketplace.model.Producto;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.Locale;

public class AnunciosAdapter extends RecyclerView.Adapter<AnunciosAdapter.ViewHolder> {

    private final List<Producto> productoList;
    private final Context context;
    private final DatabaseReference productosRef;

    // Constructor
    public AnunciosAdapter(Context context, List<Producto> productoList) {
        this.context = context;
        this.productoList = productoList;
        this.productosRef = FirebaseDatabase.getInstance().getReference("productos");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_anuncio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Producto producto = productoList.get(position);

        holder.tvTitulo.setText(producto.getNombre());

        // El precio ahora es double
        holder.tvPrecio.setText(String.format(Locale.getDefault(), "$ %,.0f CLP", producto.getPrecio()));

        List<String> imageUrls = producto.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            String urlPrincipal = imageUrls.get(0);
            Glide.with(context)
                    .load(urlPrincipal)
                    .placeholder(R.drawable.agregar_img)
                    .into(holder.ivImagen);
        } else {
            holder.ivImagen.setImageResource(R.drawable.agregar_img);
        }

        // Click para Editar
        holder.btnEditar.setOnClickListener(v -> {
            Intent intent = new Intent(context, Publicar.class);
            intent.putExtra("PRODUCTO_ID_EDITAR", producto.getId());
            context.startActivity(intent);
        });

        // Click para Eliminar
        holder.btnEliminar.setOnClickListener(v -> {
            mostrarDialogoEliminar(producto);
        });

        // Click en el ítem para ver el detalle
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetalleProductoActivity.class);
            intent.putExtra("productoId", producto.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productoList.size();
    }

    /**
     * Muestra un diálogo de confirmación antes de eliminar.
     */
    private void mostrarDialogoEliminar(Producto producto) {
        new AlertDialog.Builder(context)
                // USAMOS getNombre() AQUÍ TAMBIÉN
                .setTitle("Confirmar Eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar el anuncio '" + producto.getNombre() + "'? Esta acción es irreversible.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    eliminarProducto(producto);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Elimina el producto de Firebase Realtime Database.
     */
    private void eliminarProducto(Producto producto) {
        productosRef.child(producto.getId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // USAMOS getNombre() AQUÍ TAMBIÉN
                    Toast.makeText(context, "Anuncio '" + producto.getNombre() + "' eliminado con éxito.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error al eliminar anuncio: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ViewHolder: Contiene las referencias a las vistas del item_anuncio.xml
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivImagen;
        final TextView tvTitulo;
        final TextView tvPrecio;
        final ImageButton btnEditar;
        final ImageButton btnEliminar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImagen = itemView.findViewById(R.id.iv_imagen_anuncio);
            tvTitulo = itemView.findViewById(R.id.tv_titulo_anuncio);
            tvPrecio = itemView.findViewById(R.id.tv_precio_anuncio);
            btnEditar = itemView.findViewById(R.id.btn_editar_anuncio);
            btnEliminar = itemView.findViewById(R.id.btn_eliminar_anuncio);
        }
    }
}