package com.diegodev.marketplace.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.diegodev.marketplace.ChatActivity;
import com.diegodev.marketplace.R; // Asegúrate de que el R.java esté en el paquete correcto
import com.diegodev.marketplace.model.Conversacion;

import java.util.List;
import java.util.Locale;

public class ConversacionAdapter extends RecyclerView.Adapter<ConversacionAdapter.ViewHolder> {

    private final Context context;
    private final List<Conversacion> listaConversaciones;

    public ConversacionAdapter(Context context, List<Conversacion> listaConversaciones) {
        this.context = context;
        this.listaConversaciones = listaConversaciones;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Asume que tienes un layout llamado list_item_conversacion
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_conversacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversacion conversacion = listaConversaciones.get(position);

        // Nombre del compañero (ya cargado desde la actividad)
        holder.tvNombre.setText(conversacion.getNombreCompanero() != null ? conversacion.getNombreCompanero() : "Cargando...");

        // Último mensaje y producto
        holder.tvUltimoMensaje.setText(conversacion.getUltimoMensaje());
        holder.tvProducto.setText("Producto ID: " + conversacion.getProductoId());

        // Timestamp
        String fecha = DateFormat.format("HH:mm", conversacion.getTimestamp()).toString();
        holder.tvTimestamp.setText(fecha);

        // Listener para abrir el chat
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("OTRO_USUARIO_ID", conversacion.getOtroUserId());
            intent.putExtra("PRODUCTO_ID", conversacion.getProductoId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return listaConversaciones.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvNombre;
        public TextView tvUltimoMensaje;
        public TextView tvTimestamp;
        public TextView tvProducto; // Opcional, para indicar el producto

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Asegúrate de que estos IDs coincidan con tu list_item_conversacion.xml
            tvNombre = itemView.findViewById(R.id.tv_nombre_companero);
            tvUltimoMensaje = itemView.findViewById(R.id.tv_ultimo_mensaje);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvProducto = itemView.findViewById(R.id.tv_producto_id);
        }
    }
}