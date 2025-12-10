package com.diegodev.marketplace;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;

public class EditarPerfilActivity extends AppCompatActivity {

    private static final String TAG = "EditarPerfilActivity";

    // Vistas del Layout
    private Toolbar toolbar;
    private FloatingActionButton fabCambiarImagen;
    private TextInputEditText etNombres;
    private TextInputEditText etFechaNac;
    private TextInputEditText etTelefono;
    private MaterialButton btnActualizar;

    // Firebase
    private FirebaseAuth firebaseAuth;
    private DatabaseReference userRef;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        // Inicializar Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            // Manejar caso de usuario no logueado
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Referencia a la BD: users/[UID] (CORREGIDO para coincidir con tu estructura)
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        inicializarVistas();
        configurarToolbar();
        configurarListeners();

        // Cargar los datos actuales al iniciar la Activity
        cargarDatosActuales();
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar_editar_perfil);
        fabCambiarImagen = findViewById(R.id.fab_cambiar_imagen);
        etNombres = findViewById(R.id.et_nombres);
        etFechaNac = findViewById(R.id.et_fecha_nac);
        etTelefono = findViewById(R.id.et_telefono);
        btnActualizar = findViewById(R.id.btn_actualizar);
    }

    /**
     * Carga los datos del perfil actual del usuario para que se vean en los EditText.
     */
    private void cargarDatosActuales() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Cargar valores existentes
                    String nombre = snapshot.child("nombre").getValue(String.class);
                    String telefono = snapshot.child("telefono").getValue(String.class);
                    String fechaNac = snapshot.child("fechaNacimiento").getValue(String.class);

                    // Establecer valores en los EditText
                    if (nombre != null) etNombres.setText(nombre);
                    if (telefono != null) etTelefono.setText(telefono);
                    if (fechaNac != null) etFechaNac.setText(fechaNac);

                } else {
                    Log.w(TAG, "No se encontraron datos de perfil para cargar. Usando valores por defecto.");
                    // Los campos quedan vacíos, listos para ser llenados
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar datos del perfil: " + error.getMessage());
                Toast.makeText(EditarPerfilActivity.this, "Error al cargar datos previos.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Listener para el botón de retroceso de la Toolbar
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void configurarListeners() {
        // 1. Listener para abrir el selector de fecha al tocar el campo de Fecha de Nacimiento
        etFechaNac.setOnClickListener(v -> mostrarDatePickerDialog());

        // 2. Listener para el FloatingActionButton (Cambiar Imagen)
        fabCambiarImagen.setOnClickListener(v -> {
            Toast.makeText(this, "Abriendo selector de imágenes...", Toast.LENGTH_SHORT).show();
            // Lógica pendiente: Abrir intent para seleccionar imagen
        });

        // 3. Listener para el botón de Actualizar
        btnActualizar.setOnClickListener(v -> {
            guardarCambios();
        });
    }


    private void mostrarDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int anio = c.get(Calendar.YEAR);
        int mes = c.get(Calendar.MONTH);
        int dia = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    // Formatea la fecha seleccionada y la establece en el EditText
                    // Se usa (monthOfYear + 1) porque los meses van de 0 a 11
                    String fechaSeleccionada = String.format("%02d/%02d/%d", dayOfMonth, monthOfYear + 1, year);
                    etFechaNac.setText(fechaSeleccionada);
                }, anio, mes, dia);
        datePickerDialog.show();
    }

    /**
     * Guarda los cambios de nombres, teléfono y fecha de nacimiento en Firebase Realtime Database.
     */
    private void guardarCambios() {
        // Obtener valores
        String nombres = etNombres.getText().toString().trim();
        String fechaNac = etFechaNac.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();

        // PASO 1: Validación de campos
        if (nombres.isEmpty() || fechaNac.isEmpty() || telefono.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos para actualizar.", Toast.LENGTH_LONG).show();
            return;
        }

        // PASO 2: Creación del mapa de datos a actualizar
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("nombre", nombres);
        hashMap.put("telefono", telefono);
        hashMap.put("fechaNacimiento", fechaNac);

        // Opcional: Si quieres forzar la fecha de creación en la primera edición:
        // hashMap.put("fecha_creacion", System.currentTimeMillis());

        // PASO 3: Actualización en Firebase
        userRef.updateChildren(hashMap)
                .addOnSuccessListener(aVoid -> {
                    // Éxito al guardar
                    Toast.makeText(EditarPerfilActivity.this, "Perfil actualizado con éxito.", Toast.LENGTH_LONG).show();

                    // Finaliza la actividad para recargar los datos en CuentaActivity
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Error al guardar
                    Log.e(TAG, "Error al actualizar el perfil: " + e.getMessage());
                    Toast.makeText(EditarPerfilActivity.this, "Error: No se pudieron guardar los cambios. " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}