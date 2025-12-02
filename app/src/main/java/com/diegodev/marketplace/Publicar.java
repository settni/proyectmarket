package com.diegodev.marketplace;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;

import android.content.ClipData;
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
import com.diegodev.marketplace.adapter.GaleriaImagenesAdapter;

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

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Actividad para publicar o editar un producto con soporte para múltiples imágenes.
 */
public class Publicar extends AppCompatActivity
        implements GaleriaImagenesAdapter.OnImageInteractionListener {

    private static final String TAG = "Publicar";
    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText etNombre, etMarca, etPrecio, etDescripcion, etDireccion;
    private AutoCompleteTextView spCategoria, spCondicion;
    private MaterialButton btnPublicar;
    private ImageView ivImagenPrincipal;
    private RecyclerView rvGaleriaImagenes;

    private FirebaseAuth mAuth;
    private DatabaseReference productosRef;
    private StorageReference storageRef;

    private String productoIdEditar = null;
    private List<Uri> imagenesSeleccionadasUri = new ArrayList<>();
    private GaleriaImagenesAdapter galeriaAdapter;
    private List<String> urlsAnteriores = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publicar);

        inicializarFirebase();
        inicializarVistas();
        configurarSpinners();
        configurarGaleria();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

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

        ivImagenPrincipal.setOnClickListener(v -> abrirSelectorMultiplesImagenes());

        btnPublicar.setOnClickListener(v -> {
            if (productoIdEditar != null) {
                actualizarProducto();
            } else {
                publicarProducto();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void inicializarFirebase() {
        mAuth = FirebaseAuth.getInstance();
        productosRef = FirebaseDatabase.getInstance().getReference("productos");
        storageRef = FirebaseStorage.getInstance().getReference("imagenes_productos");
    }

    private void inicializarVistas() {
        ivImagenPrincipal = findViewById(R.id.iv_publicar_imagen_placeholder);
        rvGaleriaImagenes = findViewById(R.id.rv_galeria_imagenes);
        etNombre = findViewById(R.id.et_publicar_nombre);
        etMarca = findViewById(R.id.et_publicar_marca);
        etPrecio = findViewById(R.id.et_publicar_precio);
        etDescripcion = findViewById(R.id.et_publicar_descripcion);
        etDireccion = findViewById(R.id.et_publicar_direccion);
        spCondicion = findViewById(R.id.spinner_condicion);
        spCategoria = findViewById(R.id.spinner_categoria);
        btnPublicar = findViewById(R.id.btn_publicar_producto);
    }

    private void configurarSpinners() {
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

    private void configurarGaleria() {
        rvGaleriaImagenes.setLayoutManager(new GridLayoutManager(this, 4));
        galeriaAdapter = new GaleriaImagenesAdapter(this, imagenesSeleccionadasUri, this);
        rvGaleriaImagenes.setAdapter(galeriaAdapter);
    }

    @Override
    public void onRemoveImage(int position) {
        if (position >= 0 && position < imagenesSeleccionadasUri.size()) {
            imagenesSeleccionadasUri.remove(position);

            galeriaAdapter.notifyItemRemoved(position);
            galeriaAdapter.notifyItemRangeChanged(position, imagenesSeleccionadasUri.size());

            if (imagenesSeleccionadasUri.isEmpty()) {
                ivImagenPrincipal.setImageResource(R.drawable.agregar_img);
            } else if (position == 0) {
                onSelectMainImage(0);
            }
            Toast.makeText(this, "Imagen eliminada de la selección.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSelectMainImage(int position) {
        if (position >= 0 && position < imagenesSeleccionadasUri.size()) {
            Uri newMainUri = imagenesSeleccionadasUri.get(position);
            Glide.with(this).load(newMainUri).into(ivImagenPrincipal);
        }
    }

    private void abrirSelectorMultiplesImagenes() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Seleccionar Imágenes (Máx. 10)"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {

            imagenesSeleccionadasUri.clear();

            if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount() && i < 10; i++) {
                    imagenesSeleccionadasUri.add(clipData.getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                imagenesSeleccionadasUri.add(data.getData());
            }

            if (!imagenesSeleccionadasUri.isEmpty()) {
                onSelectMainImage(0);
                galeriaAdapter.notifyDataSetChanged();
            } else {
                ivImagenPrincipal.setImageResource(R.drawable.agregar_img);
            }
        }
    }

    private void publicarProducto() {
        if (mAuth.getCurrentUser() == null || imagenesSeleccionadasUri.isEmpty() || !validarCampos()) {
            Toast.makeText(Publicar.this, "Revisa la sesión, imágenes y campos obligatorios.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPublicar.setEnabled(false);
        btnPublicar.setText("Subiendo Imágenes...");

        subirTodasLasImagenesYGuardarProducto();
    }

    private void subirTodasLasImagenesYGuardarProducto() {
        List<Task<Uri>> uploadTasks = new ArrayList<>();

        for (Uri uri : imagenesSeleccionadasUri) {
            StorageReference fileReference = storageRef.child(System.currentTimeMillis() + "_" + uri.getLastPathSegment());

            Task<Uri> uploadTask = fileReference.putFile(uri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return fileReference.getDownloadUrl();
                    });
            uploadTasks.add(uploadTask);
        }

        Tasks.whenAllSuccess(uploadTasks)
                .addOnSuccessListener(results -> {
                    List<String> downloadUrls = new ArrayList<>();
                    for (Object result : results) {
                        if (result instanceof Uri) {
                            downloadUrls.add(((Uri) result).toString());
                        }
                    }
                    guardarProductoEnDatabase(downloadUrls);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al subir imágenes: " + e.getMessage());
                    Toast.makeText(Publicar.this, "Error al subir imágenes.", Toast.LENGTH_LONG).show();
                    btnPublicar.setEnabled(true);
                    btnPublicar.setText("Publicar Producto");
                });
    }

    private void guardarProductoEnDatabase(List<String> imageUrls) {
        String nombre = etNombre.getText().toString().trim();
        String marca = etMarca.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();
        double precio = Double.parseDouble(etPrecio.getText().toString().trim());
        String categoria = spCategoria.getText().toString();
        String condicion = spCondicion.getText().toString();
        String vendedorId = mAuth.getCurrentUser().getUid();

        Producto nuevoProducto = new Producto(
                nombre, marca, categoria, condicion,
                null,
                descripcion, direccion, precio, vendedorId, System.currentTimeMillis(), imageUrls
        );

        String productoId = productosRef.push().getKey();
        productosRef.child(productoId).setValue(nuevoProducto)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Publicar.this, "Producto publicado con éxito.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Publicar.this, "Error al publicar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnPublicar.setEnabled(true);
                    btnPublicar.setText("Publicar Producto");
                });
    }

    private void configurarModoEdicion(String id) {
        btnPublicar.setText("Guardar Cambios");

        productosRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Producto producto = snapshot.getValue(Producto.class);
                    if (producto != null) {
                        rellenarCampos(producto);

                        urlsAnteriores = producto.getImageUrls();
                        if (urlsAnteriores != null && !urlsAnteriores.isEmpty()) {
                            Glide.with(Publicar.this).load(urlsAnteriores.get(0)).into(ivImagenPrincipal);
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

    private void rellenarCampos(Producto producto) {
        etNombre.setText(producto.getNombre());
        etMarca.setText(producto.getMarca());
        etPrecio.setText(String.valueOf((int) producto.getPrecio()));
        etDescripcion.setText(producto.getDescripcion());
        etDireccion.setText(producto.getDireccion());
        spCategoria.setText(producto.getCategoria(), false);
        spCondicion.setText(producto.getCondicion(), false);
    }

    private void actualizarProducto() {
        if (productoIdEditar == null || !validarCampos()) {
            return;
        }

        btnPublicar.setEnabled(false);
        btnPublicar.setText("Guardando Cambios...");

        if (imagenesSeleccionadasUri.isEmpty()) {
            // No hay imágenes nuevas, mantiene las anteriores
            actualizarDatosProductoSoloTexto(urlsAnteriores);
        } else {
            // Hay imágenes nuevas, sube y reemplaza
            subirTodasLasImagenesYActualizarDatos();
        }
    }

    private void subirTodasLasImagenesYActualizarDatos() {
        List<Task<Uri>> uploadTasks = new ArrayList<>();

        for (Uri uri : imagenesSeleccionadasUri) {
            StorageReference fileReference = storageRef.child(System.currentTimeMillis() + "_" + uri.getLastPathSegment());
            Task<Uri> uploadTask = fileReference.putFile(uri).continueWithTask(task -> fileReference.getDownloadUrl());
            uploadTasks.add(uploadTask);
        }

        Tasks.whenAllSuccess(uploadTasks)
                .addOnSuccessListener(results -> {
                    List<String> downloadUrls = new ArrayList<>();
                    for (Object result : results) {
                        if (result instanceof Uri) {
                            downloadUrls.add(((Uri) result).toString());
                        }
                    }
                    actualizarDatosProductoSoloTexto(downloadUrls);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al subir imágenes en Edición: " + e.getMessage());
                    Toast.makeText(Publicar.this, "Error al subir imágenes para edición.", Toast.LENGTH_LONG).show();
                    btnPublicar.setEnabled(true);
                    btnPublicar.setText("Guardar Cambios");
                });
    }

    private void actualizarDatosProductoSoloTexto(List<String> nuevaImageUrls) {
        String nombre = etNombre.getText().toString().trim();
        String marca = etMarca.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();
        double precio = Double.parseDouble(etPrecio.getText().toString().trim());
        String categoria = spCategoria.getText().toString();
        String condicion = spCondicion.getText().toString();

        Map<String, Object> updates = new HashMap<>();
        updates.put("nombre", nombre);
        updates.put("marca", marca);
        updates.put("descripcion", descripcion);
        updates.put("direccion", direccion);
        updates.put("precio", precio);
        updates.put("categoria", categoria);
        updates.put("condicion", condicion);
        updates.put("imageUrls", nuevaImageUrls);

        productosRef.child(productoIdEditar).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Publicar.this, "Cambios guardados.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Publicar.this, "Error al guardar cambios.", Toast.LENGTH_LONG).show();
                    btnPublicar.setEnabled(true);
                    btnPublicar.setText("Guardar Cambios");
                });
    }

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
}