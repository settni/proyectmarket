package com.diegodev.marketplace;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.diegodev.marketplace.adapter.AnunciosAdapter;
import com.diegodev.marketplace.model.Producto;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AnunciosActivity extends AppCompatActivity {

    private static final String TAG = "AnunciosActivity";

    // Vistas y Componentes
    private RecyclerView rvMisAnuncios;
    private TextView tvNoAnuncios;
    private ImageButton btnBack;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference productosRef;

    private List<Producto> listaProductos;
    private AnunciosAdapter anunciosAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anuncios);

        // Inicializar Vistas
        rvMisAnuncios = findViewById(R.id.rv_mis_productos);
        tvNoAnuncios = findViewById(R.id.tv_no_anuncios);
        btnBack = findViewById(R.id.btnBack);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        productosRef = FirebaseDatabase.getInstance().getReference("productos");

        // Configurar RecyclerView
        listaProductos = new ArrayList<>();
        anunciosAdapter = new AnunciosAdapter(this, listaProductos);

        // Configuración del LayoutManager
        rvMisAnuncios.setLayoutManager(new LinearLayoutManager(this));
        rvMisAnuncios.setAdapter(anunciosAdapter);

        // Configurar el botón de retroceso
        btnBack.setOnClickListener(v -> finish());

        // Cargar los anuncios
        cargarMisAnuncios();
    }

    private void cargarMisAnuncios() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Verificar autenticación
        if (currentUser == null) {
            Log.w(TAG, "Usuario no autenticado. No se pueden cargar anuncios.");
            mostrarMensajeNoAnuncios(true);
            return;
        }

        final String userId = currentUser.getUid();
        Log.d(TAG, "Iniciando carga de anuncios para el usuario ID: " + userId);

        Query queryMisAnuncios = productosRef.orderByChild("vendedorId").equalTo(userId);

        queryMisAnuncios.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Limpiamos la lista al inicio para evitar duplicados
                listaProductos.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot productoSnapshot : snapshot.getChildren()) {
                        Producto producto = productoSnapshot.getValue(Producto.class);
                        if (producto != null) {
                            // Asigna la clave
                            producto.setId(productoSnapshot.getKey());
                            listaProductos.add(producto);
                        }
                    }
                }

                // Actualizar UI
                if (listaProductos.isEmpty()) {
                    mostrarMensajeNoAnuncios(true);
                } else {
                    mostrarMensajeNoAnuncios(false);
                }

                anunciosAdapter.notifyDataSetChanged();

                Log.d(TAG, "Anuncios cargados: " + listaProductos.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Fallo al leer anuncios: " + error.getMessage());
                Toast.makeText(AnunciosActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                mostrarMensajeNoAnuncios(true);
            }
        });
    }

    private void mostrarMensajeNoAnuncios(boolean mostrar) {
        if (mostrar) {
            tvNoAnuncios.setVisibility(View.VISIBLE);
            rvMisAnuncios.setVisibility(View.GONE);
        } else {
            tvNoAnuncios.setVisibility(View.GONE);
            rvMisAnuncios.setVisibility(View.VISIBLE);
        }
    }
}