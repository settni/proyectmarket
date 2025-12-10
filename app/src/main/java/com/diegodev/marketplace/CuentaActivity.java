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

public class CuentaActivity extends AppCompatActivity {

    private static final String TAG = "CuentaActivity";

    // Vistas de información
    private TextView tvValorNombres;
    private TextView tvValorEmail;
    private TextView tvValorMiembro;
    private TextView tvValorTelefono;
    private TextView tvValorEstado;
    private TextView tvValorNacimiento;

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
        tvValorNacimiento = findViewById(R.id.tv_valor_nacimiento);
    }

    /**
     * Carga los datos del perfil del usuario logueado desde Firebase Realtime Database.
     */
    private void cargarDatosPerfilFirebase() {
        if (currentUser == null) return;

        // --- Carga de datos de Firebase AUTH (siempre disponibles) ---

        tvValorEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "Email no disponible");

        // Determinamos el estado de cuenta a partir de si el email está verificado o no
        String estadoCuenta = currentUser.isEmailVerified() ? "Verificado" : "Pendiente de verificación";
        tvValorEstado.setText(estadoCuenta);


        // --- Carga de datos de Realtime Database (CORRECCIÓN: Usamos "users") ---

        // Referencia a la base de datos para obtener el resto de los datos (nombre, teléfono, etc.)
        // CORRECCIÓN: Apuntamos al nodo "users" para coincidir con la base de datos.
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Carga y establece los valores desde la base de datos
                    String nombre = snapshot.child("nombre").getValue(String.class);
                    // Usamos la clave de la base de datos que sí existe: fecha_creacion
                    Long creacionTimestampDB = snapshot.child("fecha_creacion").getValue(Long.class);

                    // Estos campos pueden faltar o no existir
                    String telefono = snapshot.child("telefono").getValue(String.class);
                    String fechaNac = snapshot.child("fechaNacimiento").getValue(String.class);

                    // 1. Actualiza Nombre
                    if (nombre != null) {
                        tvValorNombres.setText(nombre);
                    } else {
                        tvValorNombres.setText("Falta Nombre");
                    }

                    // 2. Actualiza Fecha de Miembro (Usando la DB como fallback si falla el Auth)
                    if (creacionTimestampDB != null) {
                        String fechaMiembro = android.text.format.DateFormat.format("dd/MM/yyyy", creacionTimestampDB).toString();
                        tvValorMiembro.setText(fechaMiembro);
                    } else {
                        // Si no está en DB, usa Auth como fallback (código original)
                        long creacionTimestampAuth = currentUser.getMetadata() != null ? currentUser.getMetadata().getCreationTimestamp() : 0;
                        String fechaMiembroAuth = android.text.format.DateFormat.format("dd/MM/yyyy", creacionTimestampAuth).toString();
                        tvValorMiembro.setText(fechaMiembroAuth);
                    }

                    // 3. Actualiza Teléfono y Fecha de Nacimiento (Aparecerán "No disponible" hasta que se editen)
                    tvValorTelefono.setText(telefono != null ? telefono : "No disponible");
                    tvValorNacimiento.setText(fechaNac != null ? fechaNac : "No especificado");

                } else {
                    // Si el nodo del perfil no existe, muestra valores por defecto en todos los campos
                    Log.w(TAG, "No se encontraron datos de perfil en el nodo de Firebase para UID: " + currentUser.getUid());

                    tvValorNombres.setText("Usuario Desconocido");
                    tvValorTelefono.setText("No disponible");
                    tvValorNacimiento.setText("No especificado");
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
            // Lógica de cierre de sesión
            firebaseAuth.signOut();
            Toast.makeText(this, "Sesión cerrada con éxito.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(CuentaActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}