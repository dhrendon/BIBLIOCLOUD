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

public class BookManagementActivity extends AppCompatActivity {

    private EditText etTitulo, etAutor, etAnio, etIsbn, etDescripcion;
    private Spinner spinnerCategoria, spinnerEstado;
    private MaterialButton btnAgregarLibro, btnVolver, btnTomarFoto;
    private LinearLayout layoutListaLibros;
    private ImageView ivFotoLibro;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 101;
    private static final int GALLERY_REQUEST_CODE = 102;
    private String fotoBase64 = "";

    private String[] categorias = {"Novela", "Ciencia Ficción", "Fábula", "Historia", "Poesía", "Biografía", "Ensayo", "Infantil"};
    private String[] estados = {"Disponible", "Prestado", "Reservado", "En reparación"};

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
        etIsbn = findViewById(R.id.etIsbn);
        etDescripcion = findViewById(R.id.etDescripcion);
        spinnerCategoria = findViewById(R.id.spinnerCategoria);
        spinnerEstado = findViewById(R.id.spinnerEstado);
        btnAgregarLibro = findViewById(R.id.btnAgregarLibro);
        btnVolver = findViewById(R.id.btnVolver);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        ivFotoLibro = findViewById(R.id.ivFotoLibro);
        layoutListaLibros = findViewById(R.id.layoutListaLibros);
    }

    private void setupSpinners() {
        ArrayAdapter<String> categoriaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categorias);
        categoriaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(categoriaAdapter);

        ArrayAdapter<String> estadoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, estados);
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

    // === GUARDAR LIBRO EN FIREBASE ===
    private void agregarLibro() {
        String titulo = etTitulo.getText().toString().trim();
        String autor = etAutor.getText().toString().trim();
        String categoria = spinnerCategoria.getSelectedItem().toString();
        String anio = etAnio.getText().toString().trim();
        String isbn = etIsbn.getText().toString().trim();
        String estado = spinnerEstado.getSelectedItem().toString();
        String descripcion = etDescripcion.getText().toString().trim();

        if (!validarDatosLibro(titulo, autor, anio, isbn)) return;

        Map<String, Object> libro = new HashMap<>();
        libro.put("titulo", titulo);
        libro.put("autor", autor);
        libro.put("categoria", categoria);
        libro.put("anio", anio);           // 🔥 CORREGIDO: de "anto" a "anio"
        libro.put("isbn", isbn);
        libro.put("estado", estado);
        libro.put("descripcion", descripcion); // 🔥 CORREGIDO: de "description" a "descripcion"
        libro.put("foto", fotoBase64);

        db.collection("libros")
                .add(libro)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Libro agregado correctamente", Toast.LENGTH_SHORT).show();
                    limpiarFormulario();
                    cargarListaLibros();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void limpiarFormulario() {
        etTitulo.setText("");
        etAutor.setText("");
        etAnio.setText("");
        etIsbn.setText("");
        etDescripcion.setText("");
        spinnerCategoria.setSelection(0);
        spinnerEstado.setSelection(0);
        ivFotoLibro.setImageResource(R.drawable.ic_camera);
        fotoBase64 = "";
    }

    private boolean validarDatosLibro(String titulo, String autor, String anio, String isbn) {
        if (titulo.isEmpty()) { etTitulo.setError("Ingrese el título"); return false; }
        if (autor.isEmpty()) { etAutor.setError("Ingrese el autor"); return false; }
        if (anio.isEmpty() || !anio.matches("\\d{4}")) { etAnio.setError("Año inválido"); return false; }
        if (isbn.isEmpty() || isbn.length() < 10) { etIsbn.setError("ISBN inválido"); return false; }
        return true;
    }

    // === CARGAR LIBROS DESDE FIREBASE ===
    private void cargarListaLibros() {
        layoutListaLibros.removeAllViews();

        db.collection("libros")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView tvEmpty = new TextView(this);
                        tvEmpty.setText("No hay libros en el catálogo");
                        layoutListaLibros.addView(tvEmpty);
                        return;
                    }
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String titulo = doc.getString("titulo");
                        String autor = doc.getString("autor");
                        String categoria = doc.getString("categoria");
                        String anio = doc.getString("anio");
                        String isbn = doc.getString("isbn");
                        String estado = doc.getString("estado");
                        String descripcion = doc.getString("descripcion");
                        String foto = doc.getString("foto");

                        CardView card = crearCardLibro(titulo, autor, categoria, anio, isbn, estado, descripcion, foto);
                        layoutListaLibros.addView(card);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al cargar libros", Toast.LENGTH_SHORT).show());
    }

    private CardView crearCardLibro(String titulo, String autor, String categoria, String anio,
                                    String isbn, String estado, String descripcion, String fotoBase64) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, 16);
        cardView.setLayoutParams(layoutParams);
        cardView.setCardElevation(4);
        cardView.setRadius(8);
        cardView.setCardBackgroundColor(getResources().getColor(R.color.light_brown));

        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.HORIZONTAL);
        cardLayout.setPadding(16, 16, 16, 16);

        ImageView ivMiniatura = new ImageView(this);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(120, 160);
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

        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitulo = new TextView(this);
        tvTitulo.setText(titulo);
        tvTitulo.setTextSize(16);
        infoLayout.addView(tvTitulo);

        TextView tvAutor = new TextView(this);
        tvAutor.setText("✍️ " + autor);
        infoLayout.addView(tvAutor);

        TextView tvCategoriaAnio = new TextView(this);
        tvCategoriaAnio.setText("📚 " + categoria + " • " + anio);
        infoLayout.addView(tvCategoriaAnio);

        TextView tvEstado = new TextView(this);
        tvEstado.setText("📊 Estado: " + estado);
        infoLayout.addView(tvEstado);

        cardLayout.addView(infoLayout);
        cardView.addView(cardLayout);

        return cardView;
    }

    private void cargarDatosDesdeSugerencia() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String tituloSugerencia = extras.getString("titulo_sugerencia");
            String autorSugerencia = extras.getString("autor_sugerencia");
            String categoriaSugerencia = extras.getString("categoria_sugerencia");

            if (tituloSugerencia != null) etTitulo.setText(tituloSugerencia);
            if (autorSugerencia != null) etAutor.setText(autorSugerencia);
            if (categoriaSugerencia != null) {
                for (int i = 0; i < categorias.length; i++) {
                    if (categorias[i].equals(categoriaSugerencia)) {
                        spinnerCategoria.setSelection(i);
                        break;
                    }
                }
            }
        }
    }
}
