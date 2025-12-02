package com.diegodev.marketplace;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.diegodev.marketplace.model.Producto;
import com.bumptech.glide.Glide;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Actividad para publicar o editar un producto en el Marketplace.
 * Gestiona la carga de la imagen principal y los datos del producto.
 */
public class Publicar extends AppCompatActivity {

    private static final String TAG = "Publicar";
    private static final int PICK_IMAGE_REQUEST = 1;

    // Vistas de la UI
    // NOTA: Todos los IDs aquí son de la clase R.id. Los nombres deben coincidir con tu XML.
    private EditText etNombre, etMarca, etPrecio, etDescripcion, etDireccion;
    private AutoCompleteTextView spCategoria, spCondicion;
    private MaterialButton btnPublicar;
    private ImageView ivImagenPrincipal;
    private RecyclerView rvGaleriaImagenes;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference productosRef;
    private StorageReference storageRef;

    // Variables de estado para edición
    private String productoIdEditar = null;
    private String urlImagenAnterior = null;
    private Uri imagenSeleccionadaUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publicar);

        // 1. Inicializar
        inicializarFirebase();
        inicializarVistas(); // <--- Aquí es donde se conectan los IDs del XML
        configurarSpinners();

        // 2. Configurar ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // 3. Revisar Modo Edición (configurarModoEdicion)
        Intent intent = getIntent();
        if (intent.hasExtra("PRODUCTO_ID_EDITAR")) {
            productoIdEditar = intent.getStringExtra("PRODUCTO_ID_EDITAR");
            configurarModoEdicion(productoIdEditar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Editar Anuncio");
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Nuevo Anuncio");
            }
            btnPublicar.setText("Publicar Producto");
        }

        // 4. Listeners (abrirSelectorImagen)
        ivImagenPrincipal.setOnClickListener(v -> abrirSelectorImagen());

        btnPublicar.setOnClickListener(v -> {
            if (productoIdEditar != null) {
                actualizarProducto();
            } else {
                publicarProducto();
            }
        });
    }

    /**
     * Maneja el clic en la flecha de regreso de la ActionBar.
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Inicializa las referencias a Firebase.
     */
    private void inicializarFirebase() {
        mAuth = FirebaseAuth.getInstance();
        productosRef = FirebaseDatabase.getInstance().getReference("productos");
        storageRef = FirebaseStorage.getInstance().getReference("imagenes_productos");
    }

    /**
     * Inicializa las vistas de la UI (findViewById).
     * ¡ESTA FUNCIÓN ESTÁ PERFECTAMENTE ALINEADA CON TU XML!
     */
    private void inicializarVistas() {
        // ImageView (Imagen principal)
        ivImagenPrincipal = findViewById(R.id.iv_publicar_imagen_placeholder);

        // RecyclerView (Galería)
        rvGaleriaImagenes = findViewById(R.id.rv_galeria_imagenes);

        // EditTexts (Dentro de los TextInputLayout)
        etNombre = findViewById(R.id.et_publicar_nombre);
        etMarca = findViewById(R.id.et_publicar_marca);
        etPrecio = findViewById(R.id.et_publicar_precio);
        etDescripcion = findViewById(R.id.et_publicar_descripcion);
        etDireccion = findViewById(R.id.et_publicar_direccion);

        // Exposed Dropdowns (AutoCompleteTextView)
        spCondicion = findViewById(R.id.spinner_condicion);
        spCategoria = findViewById(R.id.spinner_categoria);

        // Button
        btnPublicar = findViewById(R.id.btn_publicar_producto);
    }

    /**
     * Configura los adaptadores para los AutoCompleteTextView.
     */
    private void configurarSpinners() {
        // Esto requiere que exista el archivo res/values/arrays.xml
        try {
            String[] categorias = getResources().getStringArray(R.array.categorias_array);
            ArrayAdapter<String> categoriaAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, categorias);
            spCategoria.setAdapter(categoriaAdapter);

            String[] condiciones = getResources().getStringArray(R.array.condicion_array);
            ArrayAdapter<String> condicionAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, condiciones);
            spCondicion.setAdapter(condicionAdapter);
        } catch (Exception e) {
            Log.e(TAG, "Error: Verifique la existencia de arrays.xml con 'categorias_array' y 'condicion_array'.", e);
        }
    }

    // *******************************************************************
    // LÓGICA DE EDICIÓN Y CARGA (configurarModoEdicion)
    // *******************************************************************

    /**
     * Pone la UI en modo edición, carga datos del producto desde Firebase.
     */
    private void configurarModoEdicion(String id) {
        btnPublicar.setText("Guardar Cambios");

        // 1. Busco el producto por su ID en Firebase
        productosRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Producto producto = snapshot.getValue(Producto.class);
                    if (producto != null) {
                        rellenarCampos(producto);
                        // Almacenar la URL de la imagen existente
                        List<String> urls = producto.getImageUrls();
                        if (urls != null && !urls.isEmpty()) {
                            urlImagenAnterior = urls.get(0);
                        }
                    } else {
                        Toast.makeText(Publicar.this, "Producto no existe.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                } else {
                    Toast.makeText(Publicar.this, "Error al cargar datos.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Publicar.this, "Fallo de conexión: " + error.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    /**
     * Muestra los datos del producto en los campos de la UI.
     */
    private void rellenarCampos(Producto producto) {
        etNombre.setText(producto.getNombre());
        etMarca.setText(producto.getMarca());
        etPrecio.setText(String.valueOf((int) producto.getPrecio()));
        etDescripcion.setText(producto.getDescripcion());
        etDireccion.setText(producto.getDireccion());

        // Cargar la imagen principal con Glide
        List<String> urls = producto.getImageUrls();
        if (urls != null && !urls.isEmpty()) {
            Glide.with(Publicar.this).load(urls.get(0)).into(ivImagenPrincipal);
        }

        // Seleccionar los valores correctos en los AutoCompleteTextView
        spCategoria.setText(producto.getCategoria(), false);
        spCondicion.setText(producto.getCondicion(), false);
    }

    // *******************************************************************
    // LÓGICA DE ACTUALIZACIÓN (GUARDAR CAMBIOS)
    // *******************************************************************

    private void actualizarProducto() {
        if (productoIdEditar == null || !validarCampos()) {
            return;
        }

        if (imagenSeleccionadaUri != null) {
            // Caso 1: Hay nueva imagen - subirla, borrar la vieja y actualizar DB
            subirNuevaImagenYActualizar();
        } else {
            // Caso 2: No hay nueva imagen - solo actualizar los datos en DB
            actualizarDatosProducto(urlImagenAnterior);
        }
    }

    /**
     * Sube la nueva imagen, elimina la vieja y llama a actualizar la base de datos.
     */
    private void subirNuevaImagenYActualizar() {
        StorageReference fileReference = storageRef.child(System.currentTimeMillis() + "_" + mAuth.getCurrentUser().getUid());

        fileReference.putFile(imagenSeleccionadaUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return fileReference.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String nuevaUrl = task.getResult().toString();

                        if (urlImagenAnterior != null && !urlImagenAnterior.isEmpty()) {
                            eliminarImagenAnterior(urlImagenAnterior);
                        }

                        actualizarDatosProducto(nuevaUrl);

                    } else {
                        Toast.makeText(Publicar.this, "Error al subir la imagen.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Elimina el archivo de imagen de Firebase Storage usando su URL.
     */
    private void eliminarImagenAnterior(String url) {
        try {
            StorageReference oldRef = FirebaseStorage.getInstance().getReferenceFromUrl(url);
            oldRef.delete();
        } catch (Exception e) {
            Log.e(TAG, "Error al intentar eliminar la imagen vieja: " + e.getMessage());
        }
    }

    /**
     * Guarda los datos actualizados en Realtime Database.
     */
    private void actualizarDatosProducto(String urlImagenFinal) {
        // 1. Extraer los datos de la UI
        String nombre = etNombre.getText().toString().trim();
        String marca = etMarca.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();
        double precio = Double.parseDouble(etPrecio.getText().toString().trim());
        String categoria = spCategoria.getText().toString();
        String condicion = spCondicion.getText().toString();

        // 2. Crear un mapa con los campos que quiero actualizar
        Map<String, Object> updates = new HashMap<>();
        updates.put("nombre", nombre);
        updates.put("marca", marca);
        updates.put("descripcion", descripcion);
        updates.put("direccion", direccion);
        updates.put("precio", precio);
        updates.put("categoria", categoria);
        updates.put("condicion", condicion);

        List<String> imageUrls = new ArrayList<>();
        if (urlImagenFinal != null && !urlImagenFinal.isEmpty()) {
            imageUrls.add(urlImagenFinal);
        }
        updates.put("imageUrls", imageUrls);


        // 3. Uso updateChildren() para actualizar SOLO estos campos en Firebase
        productosRef.child(productoIdEditar).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Publicar.this, "Cambios guardados con éxito.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Publicar.this, "Error al guardar cambios.", Toast.LENGTH_LONG).show();
                });
    }

    // *******************************************************************
    // LÓGICA DE PUBLICACIÓN NORMAL
    // *******************************************************************

    private void publicarProducto() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(Publicar.this, "Debes iniciar sesión.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imagenSeleccionadaUri == null) {
            Toast.makeText(Publicar.this, "Selecciona una imagen principal.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validarCampos()) {
            return;
        }

        subirImagenYGuardarProducto();
    }

    /**
     * Valida que los campos obligatorios (nombre, precio) sean correctos.
     */
    private boolean validarCampos() {
        String nombre = etNombre.getText().toString().trim();
        String precioStr = etPrecio.getText().toString().trim();

        if (nombre.isEmpty() || precioStr.isEmpty()) {
            Toast.makeText(Publicar.this, "El nombre y el precio son obligatorios.", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            Double.parseDouble(precioStr);
        } catch (NumberFormatException e) {
            Toast.makeText(Publicar.this, "El precio debe ser un número válido.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * Sube la imagen seleccionada a Storage y luego llama a guardar el producto en la DB.
     */
    private void subirImagenYGuardarProducto() {
        StorageReference fileReference = storageRef.child(System.currentTimeMillis() + "_" + mAuth.getCurrentUser().getUid());

        fileReference.putFile(imagenSeleccionadaUri)
                .continueWithTask(task -> fileReference.getDownloadUrl())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String downloadUrl = task.getResult().toString();
                        guardarProductoEnDatabase(downloadUrl);
                    } else {
                        Toast.makeText(Publicar.this, "Error al subir imagen.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Crea el objeto Producto y lo guarda en Firebase Realtime Database.
     */
    private void guardarProductoEnDatabase(String urlImagen) {
        // 1. Extraer datos y crear objeto Producto
        String nombre = etNombre.getText().toString().trim();
        String marca = etMarca.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();
        double precio = Double.parseDouble(etPrecio.getText().toString().trim());
        String categoria = spCategoria.getText().toString();
        String condicion = spCondicion.getText().toString();
        String vendedorId = mAuth.getCurrentUser().getUid();
        long fechaPublicacion = System.currentTimeMillis();

        // Solo guardamos la imagen principal por ahora
        List<String> imageUrls = new ArrayList<>();
        imageUrls.add(urlImagen);

        Producto nuevoProducto = new Producto(
                nombre, marca, categoria, condicion,
                null,
                descripcion, direccion, precio, vendedorId, fechaPublicacion, imageUrls
        );

        // 2. Guardar en Firebase
        String productoId = productosRef.push().getKey();
        productosRef.child(productoId).setValue(nuevoProducto)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Publicar.this, "Producto publicado con éxito.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Publicar.this, "Error al publicar.", Toast.LENGTH_LONG).show();
                });
    }

    // *******************************************************************
    // LÓGICA DE SELECCIÓN DE IMAGEN (abrirSelectorImagen)
    // *******************************************************************

    /**
     * Abre el selector de archivos para seleccionar una imagen.
     */
    private void abrirSelectorImagen() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            imagenSeleccionadaUri = data.getData();
            // Cargo la imagen seleccionada en el ImageView principal
            ivImagenPrincipal.setImageURI(imagenSeleccionadaUri);
        }
    }
}

