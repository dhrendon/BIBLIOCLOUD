package com.example.bibliocloud;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.*;
import com.google.firebase.FirebaseApp;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import android.graphics.drawable.Drawable;

public class BookManagementActivity extends AppCompatActivity {

    private static final String TAG = "BookManagementActivity";

    private EditText etTitulo, etAutor, etAnio, etEditorial, etNumeroEdicion;
    private EditText etIsbn, etNumeroPaginas, etDescripcion;
    private Spinner spinnerCategoria, spinnerIdioma, spinnerEstado;
    private MaterialButton btnAgregarLibro, btnVolver, btnTomarFoto;
    private LinearLayout layoutListaLibros;
    private ImageView ivFotoLibro;
    private ScrollView scrollFormulario;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 101;
    private static final int GALLERY_REQUEST_CODE = 102;
    private String fotoBase64 = "";

    private String[] categorias = {"Novela", "Ciencia Ficción", "Fábula", "Historia", "Poesía",
            "Biografía", "Ensayo", "Infantil", "Técnico", "Académico",
            "Autoayuda", "Filosofía", "Arte", "Ciencia", "Religión"};
    private String[] idiomas = {"Español", "Inglés", "Francés", "Alemán", "Italiano",
            "Portugués", "Catalán", "Otros"};
    private String[] estados = {"Disponible", "Prestado", "Reservado", "En reparación", "Extraviado"};

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_management);

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupSpinners();
        setupButtonListeners();
        cargarDatosDesdeSugerencia();
        cargarListaLibros();
    }

    private void initializeViews() {
        etTitulo = findViewById(R.id.etTitulo);
        etAutor = findViewById(R.id.etAutor);
        etAnio = findViewById(R.id.etAnio);
        etEditorial = findViewById(R.id.etEditorial);
        etNumeroEdicion = findViewById(R.id.etNumeroEdicion);
        etIsbn = findViewById(R.id.etIsbn);
        etNumeroPaginas = findViewById(R.id.etNumeroPaginas);
        etDescripcion = findViewById(R.id.etDescripcion);
        spinnerCategoria = findViewById(R.id.spinnerCategoria);
        spinnerIdioma = findViewById(R.id.spinnerIdioma);
        spinnerEstado = findViewById(R.id.spinnerEstado);
        btnAgregarLibro = findViewById(R.id.btnAgregarLibro);
        btnVolver = findViewById(R.id.btnVolver);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        ivFotoLibro = findViewById(R.id.ivFotoLibro);
        layoutListaLibros = findViewById(R.id.layoutListaLibros);
        scrollFormulario = findViewById(R.id.scrollFormulario);
    }

    private void setupSpinners() {
        ArrayAdapter<String> categoriaAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categorias);
        categoriaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(categoriaAdapter);

        ArrayAdapter<String> idiomaAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, idiomas);
        idiomaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIdioma.setAdapter(idiomaAdapter);

        ArrayAdapter<String> estadoAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, estados);
        estadoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEstado.setAdapter(estadoAdapter);
    }

    private void setupButtonListeners() {
        btnAgregarLibro.setOnClickListener(v -> agregarLibro());
        btnVolver.setOnClickListener(v -> finish());
        btnTomarFoto.setOnClickListener(v -> seleccionarFoto());
        ivFotoLibro.setOnClickListener(v -> seleccionarFoto());
    }

    private void seleccionarFoto() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar foto del libro");
        builder.setItems(new String[]{"Tomar foto", "Elegir de galería", "Cancelar"}, (dialog, which) -> {
            switch (which) {
                case 0: checkCameraPermission(); break;
                case 1: abrirGaleria(); break;
                case 2: dialog.dismiss(); break;
            }
        });
        builder.show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            abrirCamara();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirCamara();
            } else {
                Toast.makeText(this, "Se necesita permiso de cámara", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void abrirCamara() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        }
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Selecciona una imagen"), GALLERY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Bitmap imageBitmap = null;
            if (requestCode == CAMERA_REQUEST_CODE && data != null) {
                Bundle extras = data.getExtras();
                imageBitmap = (Bitmap) extras.get("data");
            } else if (requestCode == GALLERY_REQUEST_CODE && data != null) {
                Uri selectedImage = data.getData();
                try {
                    imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (imageBitmap != null) {
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, 800, 800, true);
                ivFotoLibro.setImageBitmap(resizedBitmap);
                fotoBase64 = bitmapToBase64(resizedBitmap);
            }
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] byteArray = baos.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    // ✅ GUARDAR LIBRO CON CAMPOS EN INGLÉS
    private void agregarLibro() {
        String titulo = etTitulo.getText().toString().trim();
        String autor = etAutor.getText().toString().trim();
        String anio = etAnio.getText().toString().trim();
        String editorial = etEditorial.getText().toString().trim();
        String numeroEdicion = etNumeroEdicion.getText().toString().trim();
        String isbn = etIsbn.getText().toString().trim();
        String categoria = spinnerCategoria.getSelectedItem().toString();
        String numeroPaginasStr = etNumeroPaginas.getText().toString().trim();
        String idioma = spinnerIdioma.getSelectedItem().toString();
        String descripcion = etDescripcion.getText().toString().trim();
        String estado = spinnerEstado.getSelectedItem().toString();

        if (!validarDatosLibro(titulo, autor, anio, editorial, numeroEdicion, isbn, numeroPaginasStr, descripcion)) {
            return;
        }

        int numeroPaginas = Integer.parseInt(numeroPaginasStr);

        // ✅ CAMPOS EN INGLÉS
        Map<String, Object> libro = new HashMap<>();
        libro.put("title", titulo);
        libro.put("author", autor);
        libro.put("year", anio);
        libro.put("publisher", editorial);
        libro.put("edition", numeroEdicion);
        libro.put("isbn", isbn);
        libro.put("category", categoria);
        libro.put("pages", numeroPaginas);
        libro.put("language", idioma);
        libro.put("description", descripcion);
        libro.put("status", estado);
        libro.put("fotoBase64", fotoBase64);
        libro.put("rating", 0.0f);
        libro.put("ratingCount", 0);
        libro.put("createdAt", System.currentTimeMillis());

        Log.d(TAG, "💾 Guardando libro: " + titulo + " con campos en inglés");

        db.collection("libros")
                .add(libro)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "✅ Libro guardado: " + documentReference.getId());
                    Toast.makeText(this, "✅ Libro agregado correctamente", Toast.LENGTH_SHORT).show();
                    limpiarFormulario();
                    cargarListaLibros();
                    scrollFormulario.post(() -> scrollFormulario.fullScroll(View.FOCUS_DOWN));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al guardar: " + e.getMessage());
                    Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void limpiarFormulario() {
        etTitulo.setText("");
        etAutor.setText("");
        etAnio.setText("");
        etEditorial.setText("");
        etNumeroEdicion.setText("1");
        etIsbn.setText("");
        etNumeroPaginas.setText("");
        etDescripcion.setText("");
        spinnerCategoria.setSelection(0);
        spinnerIdioma.setSelection(0);
        spinnerEstado.setSelection(0);
        ivFotoLibro.setImageResource(R.drawable.ic_camera);
        fotoBase64 = "";
    }

    private boolean validarDatosLibro(String titulo, String autor, String anio, String editorial,
                                      String numeroEdicion, String isbn, String numeroPaginas, String descripcion) {
        if (titulo.isEmpty()) {
            etTitulo.setError("⚠️ Campo obligatorio");
            etTitulo.requestFocus();
            return false;
        }
        if (autor.isEmpty()) {
            etAutor.setError("⚠️ Campo obligatorio");
            etAutor.requestFocus();
            return false;
        }
        if (anio.isEmpty() || !anio.matches("\\d{4}")) {
            etAnio.setError("⚠️ Año inválido (YYYY)");
            etAnio.requestFocus();
            return false;
        }
        int anioInt = Integer.parseInt(anio);
        int anioActual = Calendar.getInstance().get(Calendar.YEAR);
        if (anioInt < 1000 || anioInt > anioActual) {
            etAnio.setError("⚠️ Año fuera de rango (1000-" + anioActual + ")");
            etAnio.requestFocus();
            return false;
        }
        if (editorial.isEmpty()) {
            etEditorial.setError("⚠️ Campo obligatorio");
            etEditorial.requestFocus();
            return false;
        }
        if (numeroEdicion.isEmpty()) {
            etNumeroEdicion.setError("⚠️ Campo obligatorio");
            etNumeroEdicion.requestFocus();
            return false;
        }
        if (isbn.isEmpty() || isbn.length() < 10) {
            etIsbn.setError("⚠️ ISBN inválido (mínimo 10 caracteres)");
            etIsbn.requestFocus();
            return false;
        }
        if (numeroPaginas.isEmpty() || !numeroPaginas.matches("\\d+")) {
            etNumeroPaginas.setError("⚠️ Número de páginas inválido");
            etNumeroPaginas.requestFocus();
            return false;
        }
        int paginas = Integer.parseInt(numeroPaginas);
        if (paginas <= 0 || paginas > 10000) {
            etNumeroPaginas.setError("⚠️ Fuera de rango (1-10000)");
            etNumeroPaginas.requestFocus();
            return false;
        }
        if (descripcion.isEmpty()) {
            etDescripcion.setError("⚠️ Campo obligatorio");
            etDescripcion.requestFocus();
            return false;
        }
        return true;
    }

    // ✅ CARGAR LIBROS CON COMPATIBILIDAD INGLÉS/ESPAÑOL
    private void cargarListaLibros() {
        layoutListaLibros.removeAllViews();
        Log.d(TAG, "📚 Cargando libros desde Firestore...");

        db.collection("libros")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView tvEmpty = new TextView(this);
                        tvEmpty.setText("📚 No hay libros en el catálogo");
                        tvEmpty.setTextSize(16);
                        tvEmpty.setPadding(16, 32, 16, 16);
                        tvEmpty.setGravity(android.view.Gravity.CENTER);
                        layoutListaLibros.addView(tvEmpty);
                        return;
                    }

                    int totalLibros = queryDocumentSnapshots.size();
                    Log.d(TAG, "✅ Total de libros encontrados: " + totalLibros);
                    Toast.makeText(this, "📚 Total de libros: " + totalLibros, Toast.LENGTH_SHORT).show();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        // ✅ Leer campos en inglés primero, luego fallback a español
                        String titulo = doc.getString("title");
                        if (titulo == null) titulo = doc.getString("titulo");

                        String autor = doc.getString("author");
                        if (autor == null) autor = doc.getString("autor");

                        String categoria = doc.getString("category");
                        if (categoria == null) categoria = doc.getString("categoria");

                        String anio = doc.getString("year");
                        if (anio == null) anio = doc.getString("anio");

                        String editorial = doc.getString("publisher");
                        if (editorial == null) editorial = doc.getString("editorial");

                        String numeroEdicion = doc.getString("edition");
                        if (numeroEdicion == null) numeroEdicion = doc.getString("numero_edicion");

                        String isbn = doc.getString("isbn");

                        Long numeroPaginasLong = doc.getLong("pages");
                        if (numeroPaginasLong == null) numeroPaginasLong = doc.getLong("numero_paginas");
                        int numeroPaginas = numeroPaginasLong != null ? numeroPaginasLong.intValue() : 0;

                        String idioma = doc.getString("language");
                        if (idioma == null) idioma = doc.getString("idioma");

                        String descripcion = doc.getString("description");
                        if (descripcion == null) descripcion = doc.getString("descripcion");

                        String estado = doc.getString("status");
                        if (estado == null) estado = doc.getString("estado");

                        String foto = doc.getString("fotoBase64");
                        if (foto == null) foto = doc.getString("foto");

                        // Valores por defecto
                        if (titulo == null) titulo = "Sin título";
                        if (autor == null) autor = "Sin autor";
                        if (categoria == null) categoria = "Sin categoría";
                        if (anio == null) anio = "----";
                        if (editorial == null) editorial = "Sin editorial";
                        if (numeroEdicion == null) numeroEdicion = "1";
                        if (isbn == null) isbn = "Sin ISBN";
                        if (idioma == null) idioma = "Sin idioma";
                        if (descripcion == null) descripcion = "Sin descripción";
                        if (estado == null) estado = "Disponible";

                        Log.d(TAG, "📖 Libro: " + titulo + " | Autor: " + autor);

                        CardView card = crearCardLibro(doc.getId(), titulo, autor, categoria, anio, editorial,
                                numeroEdicion, isbn, numeroPaginas, idioma, descripcion, estado, foto);
                        layoutListaLibros.addView(card);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error cargando libros: " + e.getMessage());
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private CardView crearCardLibro(String docId, String titulo, String autor, String categoria, String anio,
                                    String editorial, String numeroEdicion, String isbn,
                                    int numeroPaginas, String idioma, String descripcion,
                                    String estado, String fotoBase64) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, 16);
        cardView.setLayoutParams(layoutParams);
        cardView.setCardElevation(8);
        cardView.setRadius(12);
        cardView.setCardBackgroundColor(getResources().getColor(R.color.light_brown));

        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.HORIZONTAL);
        cardLayout.setPadding(16, 16, 16, 16);

        // Imagen
        ImageView ivMiniatura = new ImageView(this);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(140, 200);
        imageParams.setMargins(0, 0, 16, 0);
        ivMiniatura.setLayoutParams(imageParams);
        ivMiniatura.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (fotoBase64 != null && !fotoBase64.isEmpty()) {
            Bitmap bitmap = base64ToBitmap(fotoBase64);
            if (bitmap != null) ivMiniatura.setImageBitmap(bitmap);
            else ivMiniatura.setImageResource(R.drawable.ic_book_placeholder);
        } else {
            ivMiniatura.setImageResource(R.drawable.ic_book_placeholder);
        }
        cardLayout.addView(ivMiniatura);

        // Información
        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView tvTitulo = new TextView(this);
        tvTitulo.setText("📖 " + titulo);
        tvTitulo.setTextSize(18);
        tvTitulo.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitulo.setTextColor(getResources().getColor(R.color.colorPrimary));
        infoLayout.addView(tvTitulo);

        TextView tvAutor = new TextView(this);
        tvAutor.setText("✍️ " + autor);
        tvAutor.setTextSize(15);
        tvAutor.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        infoLayout.addView(tvAutor);

        TextView tvEditorialAnio = new TextView(this);
        tvEditorialAnio.setText("🏢 " + editorial + " • " + anio + " (Ed. " + numeroEdicion + ")");
        tvEditorialAnio.setTextSize(13);
        tvEditorialAnio.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        infoLayout.addView(tvEditorialAnio);

        TextView tvIsbn = new TextView(this);
        tvIsbn.setText("📚 ISBN: " + isbn);
        tvIsbn.setTextSize(12);
        tvIsbn.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        infoLayout.addView(tvIsbn);

        TextView tvDetalles = new TextView(this);
        tvDetalles.setText("🔖 " + categoria + " • 📄 " + numeroPaginas + " págs • 🌐 " + idioma);
        tvDetalles.setTextSize(12);
        tvDetalles.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        infoLayout.addView(tvDetalles);

        TextView tvEstado = new TextView(this);
        tvEstado.setText("📊 Estado: " + estado);
        tvEstado.setTextSize(13);
        tvEstado.setTypeface(null, android.graphics.Typeface.BOLD);
        int colorEstado = estado.equals("Disponible") ? R.color.green : R.color.orange;
        tvEstado.setTextColor(getResources().getColor(colorEstado));
        infoLayout.addView(tvEstado);

        if (descripcion != null && !descripcion.isEmpty()) {
            TextView tvDescripcion = new TextView(this);
            String descripcionCorta = descripcion.length() > 100 ?
                    descripcion.substring(0, 100) + "..." : descripcion;
            tvDescripcion.setText("📝 " + descripcionCorta);
            tvDescripcion.setTextSize(12);
            tvDescripcion.setTextColor(getResources().getColor(R.color.colorTextSecondary));
            tvDescripcion.setMaxLines(2);
            infoLayout.addView(tvDescripcion);
        }

        // Botones
        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonLayoutParams.setMargins(0, 8, 0, 0);
        buttonsLayout.setLayoutParams(buttonLayoutParams);

        Button btnEliminar = new Button(this);
        btnEliminar.setText("ELIMINAR");
        btnEliminar.setTextSize(12);
        btnEliminar.setBackgroundColor(getResources().getColor(R.color.red));
        btnEliminar.setTextColor(getResources().getColor(R.color.white));
        btnEliminar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        btnEliminar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar libro")
                    .setMessage("¿Estás seguro de eliminar \"" + titulo + "\"?")
                    .setPositiveButton("Eliminar", (dialog, which) -> eliminarLibro(docId))
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
        buttonsLayout.addView(btnEliminar);

        infoLayout.addView(buttonsLayout);
        cardLayout.addView(infoLayout);
        cardView.addView(cardLayout);

        return cardView;
    }

    private void eliminarLibro(String docId) {
        db.collection("libros").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Libro eliminado", Toast.LENGTH_SHORT).show();
                    cargarListaLibros();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void cargarDatosDesdeSugerencia() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String tituloSugerencia = extras.getString("titulo_sugerencia");
            String autorSugerencia = extras.getString("autor_sugerencia");
            String categoriaSugerencia = extras.getString("categoria_sugerencia");
            String edicionSugerencia = extras.getString("edicion_sugerencia");
            String isbnSugerencia = extras.getString("isbn_sugerencia");
            String yearSugerencia = extras.getString("year_sugerencia");
            String coverBase64 = extras.getString("cover_base64_sugerencia");

            if (tituloSugerencia != null && !tituloSugerencia.isEmpty()) {
                etTitulo.setText(tituloSugerencia);
            }
            if (autorSugerencia != null && !autorSugerencia.isEmpty()) {
                etAutor.setText(autorSugerencia);
            }
            if (categoriaSugerencia != null && !categoriaSugerencia.isEmpty()) {
                for (int i = 0; i < categorias.length; i++) {
                    if (categorias[i].equals(categoriaSugerencia)) {
                        spinnerCategoria.setSelection(i);
                        break;
                    }
                }
            }
            if (edicionSugerencia != null && !edicionSugerencia.isEmpty()) {
                etNumeroEdicion.setText(edicionSugerencia);
            }
            if (isbnSugerencia != null && !isbnSugerencia.isEmpty()) {
                etIsbn.setText(isbnSugerencia);
            }
            if (yearSugerencia != null && !yearSugerencia.isEmpty()) {
                etAnio.setText(yearSugerencia);
            }
            if (coverBase64 != null && !coverBase64.isEmpty()) {
                Bitmap bitmap = base64ToBitmap(coverBase64);
                if (bitmap != null) {
                    ivFotoLibro.setImageBitmap(bitmap);
                    fotoBase64 = coverBase64;
                }
            }

            Toast.makeText(this, "📝 Datos cargados desde sugerencia", Toast.LENGTH_LONG).show();
        }
    }
}