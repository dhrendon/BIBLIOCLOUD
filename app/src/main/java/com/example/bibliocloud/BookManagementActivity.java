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

import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import android.graphics.drawable.Drawable;

public class BookManagementActivity extends AppCompatActivity {

    // Campos obligatorios
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

    // Arrays actualizados
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

        // ✅ IMPORTANTE: Cargar datos de sugerencia ANTES de cargar la lista
        cargarDatosDesdeSugerencia();

        cargarListaLibros();
    }

    private void initializeViews() {
        // Campos de texto
        etTitulo = findViewById(R.id.etTitulo);
        etAutor = findViewById(R.id.etAutor);
        etAnio = findViewById(R.id.etAnio);
        etEditorial = findViewById(R.id.etEditorial);
        etNumeroEdicion = findViewById(R.id.etNumeroEdicion);
        etIsbn = findViewById(R.id.etIsbn);
        etNumeroPaginas = findViewById(R.id.etNumeroPaginas);
        etDescripcion = findViewById(R.id.etDescripcion);

        // Spinners
        spinnerCategoria = findViewById(R.id.spinnerCategoria);
        spinnerIdioma = findViewById(R.id.spinnerIdioma);
        spinnerEstado = findViewById(R.id.spinnerEstado);

        // Botones e imagen
        btnAgregarLibro = findViewById(R.id.btnAgregarLibro);
        btnVolver = findViewById(R.id.btnVolver);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        ivFotoLibro = findViewById(R.id.ivFotoLibro);
        layoutListaLibros = findViewById(R.id.layoutListaLibros);
        scrollFormulario = findViewById(R.id.scrollFormulario);
    }

    private void setupSpinners() {
        // Spinner de categorías
        ArrayAdapter<String> categoriaAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categorias);
        categoriaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(categoriaAdapter);

        // Spinner de idiomas
        ArrayAdapter<String> idiomaAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, idiomas);
        idiomaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIdioma.setAdapter(idiomaAdapter);

        // Spinner de estados
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
                case 0:
                    checkCameraPermission();
                    break;
                case 1:
                    abrirGaleria();
                    break;
                case 2:
                    dialog.dismiss();
                    break;
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
                Toast.makeText(this, "Se necesita permiso de cámara para tomar fotos", Toast.LENGTH_SHORT).show();
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
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
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

    // === GUARDAR LIBRO EN FIREBASE (ACTUALIZADO) ===
    private void agregarLibro() {
        // Obtener todos los campos
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

        // Validar campos obligatorios
        if (!validarDatosLibro(titulo, autor, anio, editorial, numeroEdicion, isbn, numeroPaginasStr, descripcion)) {
            return;
        }

        int numeroPaginas = Integer.parseInt(numeroPaginasStr);

        // Crear el mapa de datos
        Map<String, Object> libro = new HashMap<>();
        libro.put("titulo", titulo);
        libro.put("autor", autor);
        libro.put("anio", anio);
        libro.put("editorial", editorial);
        libro.put("numero_edicion", numeroEdicion);
        libro.put("isbn", isbn);
        libro.put("categoria", categoria);
        libro.put("numero_paginas", numeroPaginas);
        libro.put("idioma", idioma);
        libro.put("descripcion", descripcion);
        libro.put("estado", estado);
        libro.put("foto", fotoBase64);
        libro.put("rating", 0.0f);
        libro.put("ratingCount", 0);
        libro.put("createdAt", System.currentTimeMillis());

        // Guardar en Firestore
        db.collection("libros")
                .add(libro)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "✅ Libro agregado correctamente", Toast.LENGTH_SHORT).show();
                    limpiarFormulario();
                    cargarListaLibros();

                    // Scroll al inicio para ver la lista
                    scrollFormulario.post(() -> scrollFormulario.fullScroll(View.FOCUS_DOWN));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
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
            etAnio.setError("⚠️ Año inválido (formato: YYYY)");
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
            etNumeroPaginas.setError("⚠️ Número de páginas fuera de rango (1-10000)");
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

    // === CARGAR LIBROS DESDE FIREBASE (ACTUALIZADO) ===
    private void cargarListaLibros() {
        layoutListaLibros.removeAllViews();

        db.collection("libros")
                .orderBy("createdAt", Query.Direction.DESCENDING)
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

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String titulo = doc.getString("titulo");
                        String autor = doc.getString("autor");
                        String categoria = doc.getString("categoria");
                        String anio = doc.getString("anio");
                        String editorial = doc.getString("editorial");
                        String numeroEdicion = doc.getString("numero_edicion");
                        String isbn = doc.getString("isbn");
                        Long numeroPaginas = doc.getLong("numero_paginas");
                        String idioma = doc.getString("idioma");
                        String descripcion = doc.getString("descripcion");
                        String estado = doc.getString("estado");
                        String foto = doc.getString("foto");

                        CardView card = crearCardLibro(titulo, autor, categoria, anio, editorial,
                                numeroEdicion, isbn, numeroPaginas != null ? numeroPaginas.intValue() : 0,
                                idioma, descripcion, estado, foto);
                        layoutListaLibros.addView(card);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al cargar libros: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private CardView crearCardLibro(String titulo, String autor, String categoria, String anio,
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

        // Imagen del libro
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

        // Información del libro
        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Título
        TextView tvTitulo = new TextView(this);
        tvTitulo.setText("📖 " + titulo);
        tvTitulo.setTextSize(18);
        tvTitulo.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitulo.setTextColor(getResources().getColor(R.color.colorPrimary));
        infoLayout.addView(tvTitulo);

        // Autor
        TextView tvAutor = new TextView(this);
        tvAutor.setText("✍️ " + autor);
        tvAutor.setTextSize(15);
        infoLayout.addView(tvAutor);

        // Editorial y año
        TextView tvEditorialAnio = new TextView(this);
        tvEditorialAnio.setText("🏢 " + editorial + " • " + anio + " (Ed. " + numeroEdicion + ")");
        tvEditorialAnio.setTextSize(13);
        infoLayout.addView(tvEditorialAnio);

        // ISBN
        TextView tvIsbn = new TextView(this);
        tvIsbn.setText("📚 ISBN: " + isbn);
        tvIsbn.setTextSize(12);
        infoLayout.addView(tvIsbn);

        // Categoría, páginas e idioma
        TextView tvDetalles = new TextView(this);
        tvDetalles.setText("🔖 " + categoria + " • 📄 " + numeroPaginas + " págs • 🌐 " + idioma);
        tvDetalles.setTextSize(12);
        infoLayout.addView(tvDetalles);

        // Estado
        TextView tvEstado = new TextView(this);
        tvEstado.setText("📊 Estado: " + estado);
        tvEstado.setTextSize(13);
        tvEstado.setTypeface(null, android.graphics.Typeface.BOLD);
        int colorEstado = estado.equals("Disponible") ? R.color.green : R.color.orange;
        tvEstado.setTextColor(getResources().getColor(colorEstado));
        infoLayout.addView(tvEstado);

        // Descripción (truncada)
        if (descripcion != null && !descripcion.isEmpty()) {
            TextView tvDescripcion = new TextView(this);
            String descripcionCorta = descripcion.length() > 100 ?
                    descripcion.substring(0, 100) + "..." : descripcion;
            tvDescripcion.setText("📝 " + descripcionCorta);
            tvDescripcion.setTextSize(12);
            tvDescripcion.setMaxLines(2);
            infoLayout.addView(tvDescripcion);
        }

        cardLayout.addView(infoLayout);
        cardView.addView(cardLayout);

        return cardView;
    }

    private void cargarDatosDesdeSugerencia() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            // Datos básicos
            String tituloSugerencia = extras.getString("titulo_sugerencia");
            String autorSugerencia = extras.getString("autor_sugerencia");
            String categoriaSugerencia = extras.getString("categoria_sugerencia");

            // ✅ NUEVOS DATOS: edición, ISBN, año, imagen
            String edicionSugerencia = extras.getString("edicion_sugerencia");
            String isbnSugerencia = extras.getString("isbn_sugerencia");
            String yearSugerencia = extras.getString("year_sugerencia");
            String coverUrlSugerencia = extras.getString("cover_url_sugerencia");

            // ✅ Pre-llenar campos básicos
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

            // ✅ Pre-llenar EDICIÓN
            if (edicionSugerencia != null && !edicionSugerencia.isEmpty()) {
                etNumeroEdicion.setText(edicionSugerencia);
                Toast.makeText(this, "✅ Edición cargada: " + edicionSugerencia,
                        Toast.LENGTH_SHORT).show();
            }

            // ✅ Pre-llenar ISBN
            if (isbnSugerencia != null && !isbnSugerencia.isEmpty()) {
                etIsbn.setText(isbnSugerencia);
                Toast.makeText(this, "✅ ISBN cargado: " + isbnSugerencia,
                        Toast.LENGTH_SHORT).show();
            }

            // ✅ Pre-llenar AÑO
            if (yearSugerencia != null && !yearSugerencia.isEmpty()) {
                etAnio.setText(yearSugerencia);
                Toast.makeText(this, "✅ Año cargado: " + yearSugerencia,
                        Toast.LENGTH_SHORT).show();
            }

            // ✅ DESCARGAR Y MOSTRAR IMAGEN de la sugerencia
            if (coverUrlSugerencia != null && !coverUrlSugerencia.isEmpty()) {
                descargarImagenDeSugerencia(coverUrlSugerencia);
            }

            // Mensaje informativo
            Toast.makeText(this,
                    "📝 Datos cargados desde sugerencia. Completa los campos faltantes.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ✅ NUEVO MÉTODO: Descargar imagen desde Firebase Storage
    private void descargarImagenDeSugerencia(String imageUrl) {
        // Mostrar indicador de carga
        Toast.makeText(this, "⏳ Descargando imagen de portada...", Toast.LENGTH_SHORT).show();

        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .timeout(15000) // Timeout de 15 segundos
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                        // Imagen descargada exitosamente
                        ivFotoLibro.setImageBitmap(bitmap);
                        fotoBase64 = bitmapToBase64(bitmap);

                        Toast.makeText(BookManagementActivity.this,
                                "✅ Imagen de portada cargada",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLoadFailed(Drawable errorDrawable) {
                        // Error al descargar imagen
                        Toast.makeText(BookManagementActivity.this,
                                "⚠️ No se pudo cargar la imagen. Puedes agregar una nueva.",
                                Toast.LENGTH_LONG).show();

                        ivFotoLibro.setImageResource(R.drawable.ic_camera);
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {
                        // Cleanup si es necesario
                    }
                });
    }
}