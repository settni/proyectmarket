package com.diegodev.marketplace;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.diegodev.marketplace.adapter.MensajeAdapter;
import com.diegodev.marketplace.model.Mensaje;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    // Vistas de la interfaz
    private RecyclerView recyclerView;
    private EditText etMensaje;
    private ImageButton btnEnviarMensaje;
    private ImageButton btnEnviarImagen;
    private TextView tvNombreContacto;
    private TextView tvEstadoContacto;
    private ImageButton btnBack;

    // Firebase
    private FirebaseAuth firebaseAuth;
    private DatabaseReference chatRef;
    private DatabaseReference userRef;

    // Datos del Chat
    private String currentUserId;
    private String otroUserId; // El ID de la persona con la que se habla
    private String productoId; // Referencia al producto sobre el que se conversa

    // Adaptador y lista de mensajes
    private MensajeAdapter mensajeAdapter;
    private final List<Mensaje> listaMensajes = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 1. Inicializar Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        // 2. Validar sesión e IDs
        if (currentUser == null) {
            Toast.makeText(this, "Debe iniciar sesión para usar el chat.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Obtener IDs pasados por el Intent
        currentUserId = currentUser.getUid();
        otroUserId = getIntent().getStringExtra("OTRO_USUARIO_ID");
        productoId = getIntent().getStringExtra("PRODUCTO_ID");

        // CORRECCIÓN: Si el productoId es nulo, le asignamos un valor por defecto
        if (productoId == null) {
            productoId = "no_producto";
        }

        // *******************************************************************
        // VALIDACIÓN CLAVE
        if (otroUserId == null || otroUserId.isEmpty()) {
            Toast.makeText(this, "Error: El ID del otro usuario es obligatorio para iniciar el chat.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // *******************************************************************

        // 3. Inicialización de Vistas
        inicializarVistas();

        // 4. Configuración de la Cabecera (cargando datos reales)
        configurarCabecera();

        // 5. Configuración del RecyclerView y listener de Firebase
        configurarRecyclerView();
        escucharMensajes();

        // 6. Configuración del Listener del botón de enviar
        configurarListeners();
    }

    private void inicializarVistas() {
        tvNombreContacto = findViewById(R.id.tvContactName);
        tvEstadoContacto = findViewById(R.id.tvContactStatus);
        recyclerView = findViewById(R.id.recyclerViewChat);
        etMensaje = findViewById(R.id.editTextMensaje);
        btnEnviarMensaje = findViewById(R.id.btnEnviar);
        btnEnviarImagen = findViewById(R.id.btnAttachImage);
        btnBack = findViewById(R.id.btnBack);
    }

    private void configurarCabecera() {
        // Listener para el botón de retroceso
        btnBack.setOnClickListener(v -> finish());

        // CORRECCIÓN: Usamos "users" en lugar de "usuarios"
        userRef = FirebaseDatabase.getInstance().getReference("users").child(otroUserId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.child("nombre").exists()) {
                    String nombre = snapshot.child("nombre").getValue(String.class);
                    tvNombreContacto.setText(nombre != null ? nombre : "Usuario Desconocido");
                } else {
                    tvNombreContacto.setText("Usuario Desconocido");
                }
                tvEstadoContacto.setText("Conectado"); // Estado por defecto
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar datos del contacto: " + error.getMessage());
            }
        });
    }

    private void configurarRecyclerView() {
        mensajeAdapter = new MensajeAdapter(this, listaMensajes, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Muestra los mensajes desde abajo

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mensajeAdapter);
    }

    private void configurarListeners() {
        btnEnviarMensaje.setOnClickListener(v -> enviarMensaje());

        btnEnviarImagen.setOnClickListener(v -> {
            Toast.makeText(this, "Funcionalidad de imagen no implementada.", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Crea un ID de chat canónico, ordenando los IDs de usuario.
     * ID de Chat: {idMenor}_{idMayor}_{productoId}
     */
    private String generarChatId(String uid1, String uid2, String pId) {
        String idMenor = uid1.compareTo(uid2) < 0 ? uid1 : uid2;
        String idMayor = uid1.compareTo(uid2) < 0 ? uid2 : uid1;
        // Usamos el producto ID para evitar que chats sobre diferentes productos se mezclen
        return idMenor + "_" + idMayor + "_" + pId;
    }

    /**
     * Envía el mensaje y lo guarda en Firebase Realtime Database.
     */
    private void enviarMensaje() {
        String texto = etMensaje.getText().toString().trim();

        if (texto.isEmpty()) {
            Toast.makeText(this, "Escriba un mensaje.", Toast.LENGTH_SHORT).show();
            return;
        }

        String chatId = generarChatId(currentUserId, otroUserId, productoId);

        // 1. Obtener referencia a la colección de mensajes para este chat
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("mensajes");

        // 2. Crear el objeto Mensaje
        Mensaje nuevoMensaje = new Mensaje();
        nuevoMensaje.setRemitenteId(currentUserId);
        nuevoMensaje.setContenido(texto);
        nuevoMensaje.setChatId(chatId);
        nuevoMensaje.setProductoId(productoId);
        // Usaremos ServerValue.TIMESTAMP para la hora exacta del servidor en el modelo.

        // 3. Empujar el nuevo mensaje a Firebase
        String mensajeId = chatRef.push().getKey();
        if (mensajeId != null) {
            chatRef.child(mensajeId).setValue(nuevoMensaje.toMap())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            etMensaje.setText(""); // Limpiar input al enviar
                            // **********************************************
                            // PASO CLAVE: Guardar la conversación en la lista de ambos usuarios
                            guardarConversacionEnLista(currentUserId, otroUserId, productoId, texto);
                            guardarConversacionEnLista(otroUserId, currentUserId, productoId, texto);
                            // **********************************************
                        } else {
                            Toast.makeText(ChatActivity.this, "Error al enviar.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Fallo al enviar mensaje: " + task.getException().getMessage());
                        }
                    });
        }
    }

    /**
     * Guarda un registro de la conversación en el nodo 'ListaChats' para ambos usuarios.
     * Este es el índice que se usará para mostrar la bandeja de entrada.
     */
    private void guardarConversacionEnLista(String usuarioPrincipalId, String companeroId, String pId, String ultimoMensaje) {
        DatabaseReference listaChatsRef = FirebaseDatabase.getInstance().getReference("ListaChats")
                .child(usuarioPrincipalId)
                .child(companeroId + "_" + pId); // Clave única para la conversación

        Map<String, Object> conversacionData = new HashMap<>();
        conversacionData.put("otroUserId", companeroId);
        conversacionData.put("productoId", pId);
        conversacionData.put("ultimoMensaje", ultimoMensaje);
        conversacionData.put("timestamp", ServerValue.TIMESTAMP); // Sello de tiempo de la última actividad

        listaChatsRef.updateChildren(conversacionData)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fallo al indexar conversación para ListaChats: " + e.getMessage());
                });
    }

    /**
     * Escucha nuevos mensajes en tiempo real usando ChildEventListener.
     */
    private void escucharMensajes() {
        String chatId = generarChatId(currentUserId, otroUserId, productoId);
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("mensajes");

        chatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                Mensaje mensaje = snapshot.getValue(Mensaje.class);
                if (mensaje != null) {
                    // Asegurar que el ID del mensaje se guarda localmente
                    mensaje.setMensajeId(snapshot.getKey());
                    listaMensajes.add(mensaje);
                    mensajeAdapter.notifyItemInserted(listaMensajes.size() - 1);
                    // Desplazar al último elemento
                    recyclerView.scrollToPosition(listaMensajes.size() - 1);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) { /* ... */ }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) { /* ... */ }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) { /* ... */ }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error de Firebase en el chat: " + error.getMessage());
                Toast.makeText(ChatActivity.this, "Error de conexión al chat.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}