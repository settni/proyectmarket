package com.diegodev.marketplace;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class CuentaActivity extends AppCompatActivity {

    private static final String TAG = "CuentaActivity";

    // Vistas de información
    private TextView tvValorNombres;
    private TextView tvValorEmail;
    private TextView tvValorMiembro;
    private TextView tvValorTelefono;
    private TextView tvValorEstado;
    private TextView tvValorNacimiento; // Agregamos la vista para la fecha de nacimiento

    // Botones
    private MaterialButton btnEditarPerfil;
    private MaterialButton btnCambiarPassword;
    private MaterialButton btnEliminarAnuncios;
    private MaterialButton btnCerrarSesion;

    // Firebase
    private FirebaseAuth firebaseAuth;
    private DatabaseReference userRef;
    private FirebaseUser currentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cuenta);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            // Si no hay usuario logueado, redirigir a la pantalla de inicio de sesión
            startActivity(new Intent(CuentaActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // 1. Configurar la Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_cuenta);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // 2. Inicializar Vistas de Información
        inicializarVistasInformacion();

        // 3. Cargar Datos REALES de Firebase
        cargarDatosPerfilFirebase();

        // 4. Inicializar y Configurar Listeners para los Botones
        inicializarBotonesOpciones();
        configurarListenersBotones();
    }

    private void inicializarVistasInformacion() {
        tvValorNombres = findViewById(R.id.tv_valor_nombres);
        tvValorEmail = findViewById(R.id.tv_valor_email);
        tvValorMiembro = findViewById(R.id.tv_valor_miembro);
        tvValorTelefono = findViewById(R.id.tv_valor_telefono);
        tvValorEstado = findViewById(R.id.tv_valor_estado);
        tvValorNacimiento = findViewById(R.id.tv_valor_nacimiento); // Inicializar la nueva vista
    }

    /**
     * Carga los datos del perfil del usuario logueado desde Firebase Realtime Database.
     */
    private void cargarDatosPerfilFirebase() {
        if (currentUser == null) return;

        // Mostrar el email del usuario logueado por Firebase Auth (siempre disponible)
        tvValorEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "Email no disponible");

        // La fecha de creación es la fecha de membresía
        long creacionTimestamp = currentUser.getMetadata() != null ? currentUser.getMetadata().getCreationTimestamp() : 0;
        String fechaMiembro = android.text.format.DateFormat.format("dd/MM/yyyy", creacionTimestamp).toString();
        tvValorMiembro.setText(fechaMiembro);

        // Referencia a la base de datos para obtener el resto de los datos (nombre, teléfono, etc.)
        userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(currentUser.getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Carga y establece los valores desde la base de datos
                    String nombre = snapshot.child("nombre").getValue(String.class);
                    String telefono = snapshot.child("telefono").getValue(String.class);
                    String fechaNac = snapshot.child("fechaNacimiento").getValue(String.class); // Asumiendo este campo

                    tvValorNombres.setText(nombre != null ? nombre : "Falta Nombre");
                    tvValorTelefono.setText(telefono != null ? telefono : "No disponible");
                    tvValorNacimiento.setText(fechaNac != null ? fechaNac : "No especificado");

                    // El estado se puede hardcodear si es solo "Verificado" al estar logueado
                    tvValorEstado.setText("Verificado");

                } else {
                    Log.w(TAG, "No se encontraron datos de perfil en el nodo de Firebase.");
                    tvValorNombres.setText("Usuario Desconocido");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar datos del perfil: " + error.getMessage());
                Toast.makeText(CuentaActivity.this, "Error al cargar datos.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void inicializarBotonesOpciones() {
        btnEditarPerfil = findViewById(R.id.btn_editar_perfil);
        btnCambiarPassword = findViewById(R.id.btn_cambiar_password);
        btnEliminarAnuncios = findViewById(R.id.btn_eliminar_anuncios);
        btnCerrarSesion = findViewById(R.id.btn_cerrar_sesion);
    }

    private void configurarListenersBotones() {

        btnEditarPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(CuentaActivity.this, EditarPerfilActivity.class);
            startActivity(intent);
        });

        btnCambiarPassword.setOnClickListener(v -> {
            Intent intent = new Intent(CuentaActivity.this, CambiarPasswordActivity.class);
            startActivity(intent);
        });

        btnEliminarAnuncios.setOnClickListener(v -> {
            Toast.makeText(this, "Mostrar diálogo de confirmación para eliminar anuncios", Toast.LENGTH_SHORT).show();
            // TODO: Implementar la eliminación de anuncios
        });

        btnCerrarSesion.setOnClickListener(v -> {
            // ********************************************
            // INICIO: LÓGICA DE CIERRE DE SESIÓN CORREGIDA
            // ********************************************

            // 1. Cerrar sesión en Firebase Auth
            firebaseAuth.signOut();

            // 2. Informar al usuario
            Toast.makeText(this, "Sesión cerrada con éxito.", Toast.LENGTH_LONG).show();

            // 3. Redirigir a la pantalla de inicio de sesión (Login/MainActivity)
            // Asumo que tu pantalla de inicio es LoginActivity
            Intent intent = new Intent(CuentaActivity.this, LoginActivity.class);

            // Flags para limpiar la pila de actividades y que el usuario no pueda volver atrás
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Finalizar la actividad actual
            finish();

            // ********************************************
            // FIN: LÓGICA DE CIERRE DE SESIÓN CORREGIDA
            // ********************************************
        });
    }
}