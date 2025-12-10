package com.diegodev.marketplace;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.diegodev.marketplace.adapter.ConversacionAdapter;
import com.diegodev.marketplace.model.Conversacion;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ListaChatsActivity extends AppCompatActivity {

    private static final String TAG = "ListaChatsActivity";

    private RecyclerView recyclerView;
    private ConversacionAdapter adapter;
    private final List<Conversacion> listaConversaciones = new ArrayList<>();

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private DatabaseReference listaChatsRef;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_chats); // Layout principal de la bandeja de entrada

        // 1. Inicializar Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Debe iniciar sesión.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Referencias de Firebase
        String currentUserId = currentUser.getUid();
        // APUNTAMOS AL NODO DE INDICE CREADO EN CHATACTIVITY
        listaChatsRef = FirebaseDatabase.getInstance().getReference("ListaChats").child(currentUserId);
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // 2. Configurar RecyclerView
        recyclerView = findViewById(R.id.recyclerViewListaChats);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ConversacionAdapter(this, listaConversaciones);
        recyclerView.setAdapter(adapter);

        // 3. Cargar la lista de conversaciones
        cargarListaConversaciones();
    }

    private void cargarListaConversaciones() {
        listaChatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaConversaciones.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    // Carga el objeto Conversacion desde el índice de Firebase
                    Conversacion conversacion = dataSnapshot.getValue(Conversacion.class);

                    if (conversacion != null) {
                        listaConversaciones.add(conversacion);
                        // Cargar el nombre del compañero por separado
                        cargarNombreCompanero(conversacion);
                    }
                }
                // Ordenar la lista por el timestamp (más reciente arriba)
                ordenarLista();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar la lista de chats: " + error.getMessage());
                Toast.makeText(ListaChatsActivity.this, "Error al conectar con la bandeja de entrada.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Consulta la base de datos de usuarios para obtener el nombre del compañero de chat.
     */
    private void cargarNombreCompanero(Conversacion conversacion) {
        usersRef.child(conversacion.getOtroUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nombre = snapshot.child("nombre").getValue(String.class);
                if (nombre != null) {
                    conversacion.setNombreCompanero(nombre);
                } else {
                    conversacion.setNombreCompanero("ID: " + conversacion.getOtroUserId().substring(0, 4) + "...");
                }
                ordenarLista(); // Reordenar después de cargar el nombre
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Fallo al obtener nombre del compañero: " + error.getMessage());
                conversacion.setNombreCompanero("[Error Nombre]");
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void ordenarLista() {
        // Ordena la lista de mayor a menor timestamp (el chat más reciente primero)
        Collections.sort(listaConversaciones, (c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));
    }
}