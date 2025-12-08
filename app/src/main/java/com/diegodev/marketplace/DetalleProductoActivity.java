package com.diegodev.marketplace;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

// Importaciones de Firebase
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.auth.FirebaseAuth;

// Importaciones de UI y Modelos
import com.diegodev.marketplace.model.Producto;
import com.diegodev.marketplace.adapter.SliderImagenAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.List;
import java.util.ArrayList; // ESTA ES LA LÍNEA QUE FALTA//
import java.util.Locale;


public class DetalleProductoActivity extends AppCompatActivity {
    public static final String EXTRA_PRODUCTO_ID = "producto_id";

    // Vistas del Carrusel
    private ViewPager2 viewPagerGaleria;
    private TabLayout tabLayoutIndicador;

    // Vistas del Producto
    private TextView tvNombreDetalle;
    private TextView tvPrecioDetalle;
    private TextView tvDescripcionDetalle;
    private TextView tvMarcaDetalle;
    private TextView tvCategoriaDetalle;
    private TextView tvCondicionDetalle;
    private TextView tvDireccionDetalle;

    // VISTAS DEL VENDEDOR
    private TextView tvVendedorNombre;
    private TextView tvVendedorEmail;
    private TextView tvVendedorTelefono;

    // Botones de acción
    private MaterialButton fabContactar;
    private MaterialButton fabChat;

    // Variables de Firebase
    private DatabaseReference productosRef;
    private DatabaseReference usuariosRef;

    private String telefonoVendedor = null;

    // VARIABLES DE CLASE PARA CONTEXTO
    private String vendedorId = null;
    private String productoId = null;

    private static final String TAG = "Detalle_Producto";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_producto);

        // Configuración del Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_detalle);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Detalle del Producto");
        }

        // Inicialización de Firebase
        productosRef = FirebaseDatabase.getInstance().getReference("productos");
        usuariosRef = FirebaseDatabase.getInstance().getReference("users");

        // 1. Inicializar Vistas del Producto y Carrusel
        viewPagerGaleria = findViewById(R.id.view_pager_galeria);
        tabLayoutIndicador = findViewById(R.id.tab_layout_indicador);

        tvNombreDetalle = findViewById(R.id.tv_detalle_titulo);
        tvPrecioDetalle = findViewById(R.id.tv_detalle_precio);
        tvDescripcionDetalle = findViewById(R.id.tv_detalle_descripcion);
        tvMarcaDetalle = findViewById(R.id.tv_detalle_marca);
        tvCategoriaDetalle = findViewById(R.id.tv_detalle_categoria);
        tvCondicionDetalle = findViewById(R.id.tv_detalle_condicion);
        tvDireccionDetalle = findViewById(R.id.tv_detalle_direccion);

        // 2. Inicializar Vistas del Vendedor
        tvVendedorNombre = findViewById(R.id.tv_detalle_vendedor);
        tvVendedorEmail = findViewById(R.id.tv_vendedor_email);
        tvVendedorTelefono = findViewById(R.id.tv_vendedor_telefono);

        // Inicialización de los botones de acción
        fabContactar = findViewById(R.id.fab_contactar);
        fabChat = findViewById(R.id.fab_chat);

        // Placeholder mientras carga
        tvVendedorNombre.setText("Nombre: Cargando detalles...");


        // 3. Obtener datos del Intent y guardar el ID en variable de clase
        Intent intent = getIntent();
        productoId = intent.getStringExtra(EXTRA_PRODUCTO_ID);


        if (productoId != null) {
            Log.d(TAG, "Cargando detalles para el ID: " + productoId);
            cargarDatosProducto(productoId);
        } else {
            Toast.makeText(this, "Error: No se encontró el ID del producto.", Toast.LENGTH_LONG).show();
            finish();
        }

        // 4. Listeners de Botones
        fabContactar.setOnClickListener(v -> realizarLlamada());
        fabChat.setOnClickListener(v -> iniciarChat());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * *******************************************************************
     * MÉTODOS DE LECTURA DE DATOS DE FIREBASE
     * *******************************************************************
     */
    private void cargarDatosProducto(String productoId) {
        productosRef.child(productoId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        // Intenta mapear a la clase Producto
                        Producto producto = snapshot.getValue(Producto.class);

                        if (producto != null) {
                            mostrarDetallesProducto(producto);

                            vendedorId = producto.getVendedorId();
                            if (vendedorId != null) {
                                cargarDatosVendedor(vendedorId);
                            } else {
                                Log.e(TAG, "Producto sin VendedorId. No se pueden cargar los datos del usuario.");
                                mostrarDetallesVendedor("Vendedor Desconocido", "N/A", null);
                            }

                        } else {
                            mostrarDetallesProductoIndividual(snapshot);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error al mapear el objeto Producto. Intentando lectura individual: " + e.getMessage(), e);
                        mostrarDetallesProductoIndividual(snapshot);
                    }
                } else {
                    Toast.makeText(DetalleProductoActivity.this, "Producto no encontrado en la base de datos.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Fallo al leer el producto de DB: " + error.getMessage(), error.toException());
                Toast.makeText(DetalleProductoActivity.this, "Error de Firebase: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarDetallesProductoIndividual(@NonNull DataSnapshot snapshot) {
        // Lectura directa de campos
        String nombre = snapshot.child("nombre").getValue(String.class);
        Double precioDouble = snapshot.child("precio").getValue(Double.class);
        String descripcion = snapshot.child("descripcion").getValue(String.class);

        // Campos opcionales
        String marca = snapshot.child("marca").getValue(String.class);
        String condicion = snapshot.child("condicion").getValue(String.class);
        String categoria = snapshot.child("categoria").getValue(String.class);
        String direccion = snapshot.child("direccion").getValue(String.class);

        vendedorId = snapshot.child("vendedorId").getValue(String.class);

        // Asignar texto
        tvNombreDetalle.setText(nombre != null ? nombre : "N/A");
        tvDescripcionDetalle.setText(descripcion != null ? descripcion : "Sin descripción");

        // Formatear precio
        if (precioDouble != null) {
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
            String precioFormateado = currencyFormat.format(precioDouble);
            tvPrecioDetalle.setText(precioFormateado);
        } else {
            tvPrecioDetalle.setText("Precio: N/A");
        }

        // Asignar Texto a las Vistas de ESPECIFICACIONES
        tvMarcaDetalle.setText("Marca: " + (marca != null ? marca : "No especificada"));
        tvCondicionDetalle.setText("Condición: " + (condicion != null ? condicion : "N/A"));
        tvCategoriaDetalle.setText("Categoría: " + (categoria != null ? categoria : "N/A"));
        tvDireccionDetalle.setText("Retiro en: " + (direccion != null ? direccion : "No provista"));

        // ** Cargar Galería de Imágenes (Carrusel) **
        DataSnapshot imageUrlsSnapshot = snapshot.child("imageUrls");
        if (imageUrlsSnapshot.exists()) {

            // CORRECCIÓN PARA EL WARNING 'UNCHECKED': Usamos un ArrayList y un bucle
            // para leer de forma segura cada URL como String, garantizando el tipo.
            List<String> imageUrls = new ArrayList<>();
            for (DataSnapshot urlSnapshot : imageUrlsSnapshot.getChildren()) {
                String url = urlSnapshot.getValue(String.class);
                if (url != null) {
                    imageUrls.add(url);
                }
            }
            // FIN DE LA CORRECCIÓN

            if (!imageUrls.isEmpty()) {
                SliderImagenAdapter adapter = new SliderImagenAdapter(this, imageUrls);
                viewPagerGaleria.setAdapter(adapter);

                new TabLayoutMediator(tabLayoutIndicador, viewPagerGaleria,
                        (tab, position) -> {}
                ).attach();
            }
        }
        // ***************************************************************

        // LECTURA 2: Buscar datos del vendedor
        if (vendedorId != null) {
            cargarDatosVendedor(vendedorId);
        } else {
            Log.e(TAG, "Producto sin VendedorId. No se pueden cargar los datos del usuario.");
            mostrarDetallesVendedor("Vendedor Desconocido", "N/A", null);
        }
    }

    private void cargarDatosVendedor(String vendedorId) {
        usuariosRef.child(vendedorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Leer los valores directamente
                String nombre = snapshot.child("nombre").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);
                String codigo = snapshot.child("codigoTelefono").getValue(String.class);
                String numero = String.valueOf(snapshot.child("telefono").getValue());

                if (nombre != null || email != null || codigo != null || numero != null) {

                    String telefonoCompleto = combinarTelefono(codigo, numero);

                    telefonoVendedor = telefonoCompleto;

                    mostrarDetallesVendedor(nombre, email, telefonoCompleto);

                } else {
                    Log.w(TAG, "Usuario no encontrado o datos nulos para ID: " + vendedorId);
                    mostrarDetallesVendedor("Usuario No Encontrado", "No disponible", null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Fallo al cargar datos del vendedor: " + error.getMessage());
                mostrarDetallesVendedor("Error de Carga", "No disponible", null);
            }
        });
    }


    /**
     * *******************************************************************
     * MÉTODOS DE ACTUALIZACIÓN DE UI
     * *******************************************************************
     */
    private void mostrarDetallesProducto(Producto producto) {
        // ** 1. Cargar Galería de Imágenes (Carrusel) **
        if (producto.getImageUrls() != null && !producto.getImageUrls().isEmpty()) {
            List<String> imageUrls = producto.getImageUrls();

            SliderImagenAdapter adapter = new SliderImagenAdapter(this, imageUrls);
            viewPagerGaleria.setAdapter(adapter);

            new TabLayoutMediator(tabLayoutIndicador, viewPagerGaleria,
                    (tab, position) -> {}
            ).attach();
        }
        // ***************************************************************

        // 2. Asignar Texto a las Vistas Principales (Título, Precio, Descripción)
        tvNombreDetalle.setText(producto.getNombre());
        tvDescripcionDetalle.setText(producto.getDescripcion());

        // Formatear precio
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
        String precioFormateado = currencyFormat.format(producto.getPrecio());
        tvPrecioDetalle.setText(precioFormateado);

        // 3. Asignar Texto a las Vistas de ESPECIFICACIONES (Marca, Condición, Categoría, Dirección)
        tvMarcaDetalle.setText("Marca: " + (producto.getMarca() != null ? producto.getMarca() : "No especificada"));
        tvCondicionDetalle.setText("Condición: " + (producto.getCondicion() != null ? producto.getCondicion() : "N/A"));
        tvCategoriaDetalle.setText("Categoría: " + (producto.getCategoria() != null ? producto.getCategoria() : "N/A"));
        tvDireccionDetalle.setText("Retiro en: " + (producto.getDireccion() != null ? producto.getDireccion() : "No provista"));
    }

    /**
     * Muestra los detalles de contacto del vendedor y controla la disponibilidad de los botones de contacto.
     */
    private void mostrarDetallesVendedor(String nombre, String email, String telefono) {
        // Nombre
        String displayNombre = (nombre != null && !nombre.isEmpty()) ? nombre : "Usuario Desconocido";
        tvVendedorNombre.setText("Nombre: " + displayNombre);

        // Email
        String displayEmail = (email != null && !email.isEmpty()) ? email : "No proporcionado";
        tvVendedorEmail.setText("Correo: " + displayEmail);

        // Teléfono
        String displayTelefono = (telefono != null && !telefono.isEmpty()) ? telefono : "No disponible";
        tvVendedorTelefono.setText("Teléfono: " + displayTelefono);


        // Habilitar/Deshabilitar el botón de llamar
        boolean telefonoValido = telefono != null && !telefono.isEmpty() && telefono.matches("^\\+?[0-9\\s()-]*$");
        fabContactar.setEnabled(telefonoValido);

        // --- Lógica del Chat y Validación de Dueño ---

        // Habilitar el botón de chat si el ID del vendedor es válido
        boolean chatValido = vendedorId != null;
        fabChat.setEnabled(chatValido);

        // Obtener el ID del usuario actual para la validación
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        // Si el usuario actual es el vendedor, deshabilita ambos botones de contacto
        if (currentUserId != null && currentUserId.equals(vendedorId)) {
            fabContactar.setEnabled(false);
            fabChat.setEnabled(false);
            Toast.makeText(this, "Estás viendo tu propio producto.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * *******************************************************************
     * MÉTODOS DE ACCIÓN (DEFINICIÓN DE 'realizarLlamada' e 'iniciarChat')
     * *******************************************************************
     */
    private void realizarLlamada() {
        if (telefonoVendedor == null || telefonoVendedor.isEmpty()) {
            Toast.makeText(this, "El vendedor no ha proporcionado un número de contacto para llamar.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri telefonoUri = Uri.parse("tel:" + telefonoVendedor);
            Intent intentLlamada = new Intent(Intent.ACTION_DIAL, telefonoUri);

            if (intentLlamada.resolveActivity(getPackageManager()) != null) {
                startActivity(intentLlamada);
            } else {
                Toast.makeText(this, "No se encontró una aplicación para realizar llamadas.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al intentar iniciar el Intent de llamada: " + e.getMessage());
            Toast.makeText(this, "No se pudo iniciar la función de llamada.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Inicia la actividad de Chat.
     * Envía el ID del vendedor (receptor) y el ID del producto (para contexto).
     */
    private void iniciarChat() {
        if (vendedorId == null || productoId == null) {
            Toast.makeText(this, "Falta información del producto o vendedor para iniciar el chat.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Usamos Class.forName para verificar la existencia de ChatActivity dinámicamente
        try {
            // Asegúrate de que el paquete sea el correcto para tu proyecto
            Class<?> chatActivityClass = Class.forName("com.diegodev.marketplace.ChatActivity");
            Intent intentChat = new Intent(DetalleProductoActivity.this, chatActivityClass);

            intentChat.putExtra("OTRO_USUARIO_ID", vendedorId); // <-- Esta clave es la que espera ChatActivity
            intentChat.putExtra("PRODUCTO_ID", productoId);   // <-- Esta clave es correct

            startActivity(intentChat);

        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Error: ChatActivity no encontrada. Asegúrate de crearla con ese nombre.", e);
            Toast.makeText(this, "La función de chat no está disponible (ChatActivity no existe).", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * *******************************************************************
     * MÉTODO AUXILIAR
     * *******************************************************************
     */
    private String combinarTelefono(String codigo, String numero) {
        if (codigo != null && !codigo.isEmpty() && numero != null && !numero.isEmpty()) {
            String cleanedCodigo = codigo.replaceAll("[^0-9]", "");
            String cleanedNumero = numero.replaceAll("[^0-9]", "");

            // Asegura que el código tenga el prefijo '+'
            if (!cleanedCodigo.startsWith("+")) {
                return "+" + cleanedCodigo + cleanedNumero;
            }
            return cleanedCodigo + cleanedNumero;
        }
        return null;
    }
}