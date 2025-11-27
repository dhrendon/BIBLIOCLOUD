package com.example.bibliocloud;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bibliocloud.adapters.SuggestionsAdapter;
import com.example.bibliocloud.models.Suggestion;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class SuggestionsActivity extends AppCompatActivity {

    private static final String TAG = "SuggestionsActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int MAX_IMAGE_DIMENSION = 600;
    private static final int COMPRESSION_QUALITY = 60;
    private static final int MAX_BASE64_SIZE = 800000;

    private EditText etTitle, etAuthor, etComments;
    private EditText etEdition, etIsbn, etYear;
    private Spinner spinnerCategory;
    private Button btnSubmitSuggestion, btnSelectImage;
    private ImageView ivCoverPreview;
    private RecyclerView recyclerViewSuggestions;
    private LinearLayout layoutEmpty;
    private CardView layoutForm;
    private TextView tvMySuggestions;

    private SuggestionsAdapter suggestionsAdapter;
    private List<Suggestion> userSuggestions;
    private String currentUserEmail;
    private FirebaseFirestore db;
    private String imageBase64;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestions);

        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "✅ Firebase inicializado");

        initializeViews();
        setupToolbar();
        setupUserInfo();
        setupCategories();
        setupRecyclerView();
        setupListeners();
        setupImageLaunchers();
        listenUserSuggestions();
    }

    private void initializeViews() {
        etTitle = findViewById(R.id.etTitle);
        etAuthor = findViewById(R.id.etAuthor);
        etComments = findViewById(R.id.etComments);
        etEdition = findViewById(R.id.etEdition);
        etIsbn = findViewById(R.id.etIsbn);
        etYear = findViewById(R.id.etYear);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSubmitSuggestion = findViewById(R.id.btnSubmitSuggestion);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        ivCoverPreview = findViewById(R.id.ivCoverPreview);
        recyclerViewSuggestions = findViewById(R.id.recyclerViewSuggestions);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        layoutForm = findViewById(R.id.layoutForm);
        tvMySuggestions = findViewById(R.id.tvMySuggestions);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Sugerencias de Libros");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupUserInfo() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        currentUserEmail = prefs.getString("current_user_email", "usuario@bibliocloud.com");
    }

    private void setupCategories() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.book_categories, android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupRecyclerView() {
        userSuggestions = new ArrayList<>();
        suggestionsAdapter = new SuggestionsAdapter(userSuggestions);
        recyclerViewSuggestions.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSuggestions.setAdapter(suggestionsAdapter);
    }

    private void setupListeners() {
        btnSubmitSuggestion.setOnClickListener(v -> submitSuggestion());
        btnSelectImage.setOnClickListener(v -> showImageSourceDialog());
    }

    private void setupImageLaunchers() {
        // CÁMARA
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "📸 Camera result: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        try {
                            Bundle extras = result.getData().getExtras();
                            Bitmap bitmap = (Bitmap) extras.get("data");

                            if (bitmap != null) {
                                Log.d(TAG, "✅ Bitmap de cámara: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                                processAndDisplayImage(bitmap);
                            } else {
                                Log.e(TAG, "❌ Bitmap null");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error cámara", e);
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // GALERÍA
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "🖼️ Gallery result: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();

                        if (imageUri != null) {
                            Log.d(TAG, "✅ URI: " + imageUri);
                            try {
                                Bitmap bitmap = loadBitmapFromUri(imageUri);
                                if (bitmap != null) {
                                    Log.d(TAG, "✅ Bitmap cargado: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                                    processAndDisplayImage(bitmap);
                                } else {
                                    Log.e(TAG, "❌ No se pudo cargar bitmap");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "❌ Error galería", e);
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private Bitmap loadBitmapFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error decodificando bitmap", e);
            return null;
        }
    }

    private void processAndDisplayImage(Bitmap bitmap) {
        Log.d(TAG, "🔄 ========== PROCESANDO IMAGEN ==========");
        try {
            // 1. Redimensionar
            Log.d(TAG, "📐 Redimensionando...");
            Bitmap resizedBitmap = resizeBitmap(bitmap);

            if (resizedBitmap == null) {
                Log.e(TAG, "❌ resizedBitmap es null");
                Toast.makeText(this, "Error al redimensionar", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Convertir a Base64
            Log.d(TAG, "🔄 Convirtiendo a Base64...");
            String base64 = bitmapToBase64(resizedBitmap);

            if (base64 == null) {
                Log.e(TAG, "❌ Base64 es null");
                Toast.makeText(this, "Error al convertir imagen", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. Validar tamaño
            int sizeKB = base64.length() / 1024;
            Log.d(TAG, "📊 Tamaño Base64: " + sizeKB + " KB");

            if (base64.length() <= MAX_BASE64_SIZE) {
                imageBase64 = base64;
                ivCoverPreview.setImageBitmap(resizedBitmap);
                ivCoverPreview.setVisibility(View.VISIBLE);

                Log.d(TAG, "✅ IMAGEN PROCESADA: " + sizeKB + " KB");
                Toast.makeText(this, "✅ Imagen lista (" + sizeKB + " KB)", Toast.LENGTH_SHORT).show();
            } else {
                imageBase64 = null;
                Log.e(TAG, "❌ Imagen muy grande: " + sizeKB + " KB");
                Toast.makeText(this, "❌ Imagen muy grande (" + sizeKB + " KB)", Toast.LENGTH_LONG).show();
            }

            if (resizedBitmap != bitmap) {
                bitmap.recycle();
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ EXCEPCIÓN procesando imagen", e);
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            imageBase64 = null;
        }
        Log.d(TAG, "========================================");
    }

    private Bitmap resizeBitmap(Bitmap original) {
        if (original == null) return null;

        int width = original.getWidth();
        int height = original.getHeight();

        if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
            return original;
        }

        float scale = width > height
                ? (float) MAX_IMAGE_DIMENSION / width
                : (float) MAX_IMAGE_DIMENSION / height;

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        Bitmap resized = Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
        Log.d(TAG, "📏 Redimensionado: " + width + "x" + height + " → " + newWidth + "x" + newHeight);

        return resized;
    }

    private String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, baos);
            byte[] byteArray = baos.toByteArray();
            baos.close();

            String base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP);

            int sizeKB = base64.length() / 1024;
            Log.d(TAG, "📊 Base64 generado: " + sizeKB + " KB");

            return base64;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error convirtiendo a Base64", e);
            return null;
        }
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar imagen");
        builder.setItems(new CharSequence[]{"Tomar foto", "Galería"},
                (dialog, which) -> {
                    if (which == 0) checkCameraPermissionAndOpen();
                    else openGallery();
                });
        builder.show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(cameraIntent);
        } else {
            Toast.makeText(this, "No hay cámara disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void submitSuggestion() {
        String title = etTitle.getText().toString().trim();
        String author = etAuthor.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String comments = etComments.getText().toString().trim();
        String edition = etEdition.getText().toString().trim();
        String isbn = etIsbn.getText().toString().trim();
        String year = etYear.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Ingresa el título");
            return;
        }

        if (author.isEmpty()) {
            etAuthor.setError("Ingresa el autor");
            return;
        }

        if (category.equals("Selecciona una categoría")) {
            Toast.makeText(this, "Selecciona categoría", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isbn.isEmpty()) {
            isbn = isbn.replaceAll("[\\s-]", "");
            if (isbn.length() != 10 && isbn.length() != 13) {
                etIsbn.setError("ISBN inválido");
                return;
            }
        }

        if (!year.isEmpty()) {
            try {
                int yearInt = Integer.parseInt(year);
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                if (yearInt < 1000 || yearInt > currentYear) {
                    etYear.setError("Año inválido");
                    return;
                }
            } catch (NumberFormatException e) {
                etYear.setError("Año debe ser numérico");
                return;
            }
        }

        btnSubmitSuggestion.setEnabled(false);
        btnSubmitSuggestion.setText("Enviando...");

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("author", author);
        data.put("category", category);
        data.put("comments", comments != null ? comments : "");
        data.put("edition", edition != null ? edition : "");
        data.put("isbn", isbn != null ? isbn : "");
        data.put("year", year != null ? year : "");
        data.put("userEmail", currentUserEmail);
        data.put("status", "Pendiente");
        data.put("suggestionDate", com.google.firebase.firestore.FieldValue.serverTimestamp());

        if (imageBase64 != null && !imageBase64.isEmpty()) {
            data.put("coverImageBase64", imageBase64);
            Log.d(TAG, "📸 Imagen incluida: " + (imageBase64.length() / 1024) + " KB");
        } else {
            data.put("coverImageBase64", "");
            Log.d(TAG, "📸 Sin imagen");
        }

        submitToFirestore(data);
    }

    private void submitToFirestore(Map<String, Object> data) {
        Log.d(TAG, "💾 ========== GUARDANDO EN FIRESTORE ==========");
        Log.d(TAG, "📋 Título: " + data.get("title"));
        Log.d(TAG, "👤 Autor: " + data.get("author"));

        db.collection("sugerencias")
                .add(data)
                .addOnSuccessListener(doc -> {
                    Log.d(TAG, "✅ ========================================");
                    Log.d(TAG, "✅ ÉXITO! ID: " + doc.getId());
                    Log.d(TAG, "✅ ========================================");
                    Toast.makeText(this, "✅ ¡Sugerencia enviada!", Toast.LENGTH_LONG).show();
                    limpiarFormulario();
                    btnSubmitSuggestion.setEnabled(true);
                    btnSubmitSuggestion.setText("Enviar Sugerencia");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ ========================================");
                    Log.e(TAG, "❌ ERROR FIRESTORE");
                    Log.e(TAG, "❌ Mensaje: " + e.getMessage());
                    Log.e(TAG, "❌ Clase: " + e.getClass().getName());
                    Log.e(TAG, "❌ ========================================");
                    e.printStackTrace();

                    String msg = e.getMessage();
                    if (msg != null) {
                        if (msg.contains("PERMISSION_DENIED")) {
                            Toast.makeText(this, "❌ Permisos denegados", Toast.LENGTH_LONG).show();
                        } else if (msg.contains("size")) {
                            Toast.makeText(this, "❌ Documento muy grande (>1MB)", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "❌ " + msg, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "❌ Error desconocido", Toast.LENGTH_LONG).show();
                    }

                    btnSubmitSuggestion.setEnabled(true);
                    btnSubmitSuggestion.setText("Enviar Sugerencia");
                });
    }

    private void limpiarFormulario() {
        etTitle.setText("");
        etAuthor.setText("");
        etComments.setText("");
        etEdition.setText("");
        etIsbn.setText("");
        etYear.setText("");
        spinnerCategory.setSelection(0);
        ivCoverPreview.setVisibility(View.GONE);
        ivCoverPreview.setImageDrawable(null);
        imageBase64 = null;
    }

    private void listenUserSuggestions() {
        db.collection("sugerencias")
                .whereEqualTo("userEmail", currentUserEmail)
                .orderBy("suggestionDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error cargando: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        userSuggestions.clear();
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            Suggestion s = dc.getDocument().toObject(Suggestion.class);
                            s.setId(dc.getDocument().getId());
                            userSuggestions.add(s);
                        }

                        suggestionsAdapter.updateSuggestions(userSuggestions);
                        updateEmptyState();
                    }
                });
    }

    private void updateEmptyState() {
        if (userSuggestions.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerViewSuggestions.setVisibility(View.GONE);
            tvMySuggestions.setText("Mis Sugerencias (0)");
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerViewSuggestions.setVisibility(View.VISIBLE);
            tvMySuggestions.setText("Mis Sugerencias (" + userSuggestions.size() + ")");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        listenUserSuggestions();
    }
}