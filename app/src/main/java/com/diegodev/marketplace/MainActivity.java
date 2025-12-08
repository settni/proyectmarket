package com.diegodev.marketplace;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar Firebase Auth. Necesario para obtener el estado del usuario.
        mAuth = FirebaseAuth.getInstance();

        // No se llama a setContentView(R.layout.activity_main) porque esta actividad
        // solo sirve para redirigir y no debe mostrar un diseño.
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Obtener el usuario actualmente logueado
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Tomar la decisión de a dónde redirigir
        Class<?> destinationActivity;

        if (currentUser != null) {
            // Caso 1: Usuario ya autenticado. Ir a la pantalla principal.
            destinationActivity = HomeActivity.class;
        } else {
            // Caso 2: Usuario NO autenticado. Ir a la pantalla de inicio de sesión (LoginActivity).
            destinationActivity = LoginActivity.class;
        }

        // Crear y lanzar la Intención
        Intent intent = new Intent(MainActivity.this, destinationActivity);
        startActivity(intent);

        // Cerrar MainActivity para que no quede en el historial de navegación (back stack)
        finish();
    }
}