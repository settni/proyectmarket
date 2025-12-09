package com.diegodev.marketplace.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.cardview.widget.CardView; // Importación necesaria para trabajar con CardView
import androidx.recyclerview.widget.RecyclerView;

import com.diegodev.marketplace.R;
import com.diegodev.marketplace.model.Mensaje;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MensajeAdapter extends RecyclerView.Adapter<MensajeAdapter.MensajeViewHolder> {

    private final Context context;
    private final List<Mensaje> listaMensajes;
    private final String currentUserId; // ID del usuario actual, usado para alinear

    // Definición de las constantes de vista
    private static final int VIEW_TYPE_EMISOR = 1; // Mensaje enviado por el usuario actual (derecha)
    private static final int VIEW_TYPE_RECEPTOR = 2; // Mensaje recibido por el usuario actual (izquierda)

    public MensajeAdapter(Context context, List<Mensaje> listaMensajes, String currentUserId) {
        this.context = context;
        this.listaMensajes = listaMensajes;
        this.currentUserId = currentUserId;
    }

    // Método que actualiza la lista de mensajes y notifica los cambios.
    public void setMessages(List<Mensaje> newMessages) {
        listaMensajes.clear();
        listaMensajes.addAll(newMessages);
        notifyDataSetChanged();
    }


    //Determina que tipo de vista usar para una posicion dada: Emisor (propio) o Receptor (otro).
    @Override
    public int getItemViewType(int position) {
        Mensaje mensaje = listaMensajes.get(position);

        // Si el remitenteId del mensaje coincide con el ID del usuario actual, es un EMISOR.
        if (mensaje.getRemitenteId() != null && mensaje.getRemitenteId().equals(currentUserId)) {
            return VIEW_TYPE_EMISOR;
        } else {
            // De lo contrario, es un RECEPTOR.
            return VIEW_TYPE_RECEPTOR;
        }
    }

    @NonNull
    @Override
    public MensajeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Usa el mismo layout (item_mensaje.xml) para ambos tipos de vista.
        View view = LayoutInflater.from(context).inflate(R.layout.item_mensaje, parent, false);
        return new MensajeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MensajeViewHolder holder, int position) {
        Mensaje mensaje = listaMensajes.get(position);

        // 1. Mostrar el contenido del mensaje
        // Usamos .getContenido() que es lo que tenías, asumiendo que es el campo correcto.
        holder.tvContenido.setText(mensaje.getContenido());

        // 2. Mostrar la hora del mensaje (formato legible)
        // Usamos .getTimestamp() o .getTimestampLong() según lo que devuelva tu objeto Mensaje
        long timestamp = mensaje.getTimestampLong(); // Mantenemos tu método, pero si falla usa getTimestamp()

        if (timestamp == 0) {
            timestamp = System.currentTimeMillis();
        }

        String horaFormateada = formatTimestamp(timestamp);
        holder.tvHora.setText(horaFormateada);

        // 3. Aplicar estilos y alineación basados en el tipo de vista
        // La vista a la que aplicaremos el estilo es la CardView (contenedor principal)
        CardView cardView = holder.cardMensaje;

        if (getItemViewType(position) == VIEW_TYPE_EMISOR) {
            // Alinea la tarjeta de mensaje a la derecha (bias = 1.0f)
            ConstraintLayout.LayoutParams layoutParams =
                    (ConstraintLayout.LayoutParams) cardView.getLayoutParams();
            // El horizontalBias de ConstraintLayout es clave para mover la burbuja
            layoutParams.horizontalBias = 1.0f;
            cardView.setLayoutParams(layoutParams);

            // Usa el drawable de mensaje enviado
            // NOTA: EL FONDO SE APLICA AL LINEARLAYOUT INTERNO
            holder.llFondo.setBackgroundResource(R.drawable.bubble_sent_bg);

            // Establece el color del texto a blanco para contraste con el fondo.
            holder.tvContenido.setTextColor(Color.WHITE);
            holder.tvHora.setTextColor(Color.parseColor("#CCCCCC"));

        } else {
            // Alinea la tarjeta de mensaje a la izquierda (bias = 0.0f)
            ConstraintLayout.LayoutParams layoutParams =
                    (ConstraintLayout.LayoutParams) cardView.getLayoutParams();
            layoutParams.horizontalBias = 0.0f;
            cardView.setLayoutParams(layoutParams);

            // Usa el drawable de mensaje recibido
            holder.llFondo.setBackgroundResource(R.drawable.bubble_received_bg);

            // Restablece el color del texto a negro, ya que el fondo es claro.
            holder.tvContenido.setTextColor(Color.BLACK);
            holder.tvHora.setTextColor(Color.parseColor("#888888"));
        }
    }


    //Convierte el timestamp (long) en una cadena de hora legible
    private String formatTimestamp(long timestamp) {
        // Formato para hora y minutos (ej: 10:30 AM)
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return listaMensajes.size();
    }


    //ViewHolder: Contiene las referencias a las vistas del item_mensaje.xml
    public static class MensajeViewHolder extends RecyclerView.ViewHolder {

        public final TextView tvContenido;
        public final TextView tvHora;
        public final CardView cardMensaje; // Referencia a la CardView
        public final LinearLayout llFondo;

        public MensajeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContenido = itemView.findViewById(R.id.tv_mensaje_contenido);
            tvHora = itemView.findViewById(R.id.tv_mensaje_hora);

            // Referencia a la CardView (contenedor principal)
            cardMensaje = itemView.findViewById(R.id.card_mensaje);
            // Referencia al LinearLayout que tiene el drawable de fondo
            llFondo = itemView.findViewById(R.id.ll_bubble_background);
        }
    }
}