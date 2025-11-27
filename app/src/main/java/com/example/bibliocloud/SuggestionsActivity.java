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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class SuggestionsActivity extends AppCompatActivity {

    private static final String TAG = "SuggestionsActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    // 🔄 NUEVO: Tamaño máximo más pequeño para Base64 (aprox 200KB comprimido)
    private static final int MAX_IMAGE_DIMENSION = 800; // Ancho/Alto máximo
    private static final int COMPRESSION_QUALITY = 70;  // Calidad de compresión

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

    private Uri selectedImageUri;
    private Bitmap capturedImageBitmap;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestions);

        // 🔥 INICIALIZAR FIREBASE FIRESTORE
        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "✅ Firebase Firestore inicializado");

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
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        capturedImageBitmap = (Bitmap) extras.get("data");

                        // 🔄 Redimensionar imagen
                        capturedImageBitmap = resizeBitmap(capturedImageBitmap);

                        ivCoverPreview.setImageBitmap(capturedImageBitmap);
                        ivCoverPreview.setVisibility(View.VISIBLE);
                        selectedImageUri = null;

                        Log.d(TAG, "✅ Imagen capturada y redimensionada desde cámara");
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();

                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                    getContentResolver(), selectedImageUri);

                            // 🔄 Redimensionar imagen
                            bitmap = resizeBitmap(bitmap);

                            ivCoverPreview.setImageBitmap(bitmap);
                            ivCoverPreview.setVisibility(View.VISIBLE);
                            capturedImageBitmap = bitmap;
                            selectedImageUri = null;

                            Log.d(TAG, "✅ Imagen seleccionada y redimensionada desde galería");

                        } catch (IOException e) {
                            Log.e(TAG, "❌ Error cargando imagen: " + e.getMessage());
                            Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // 🔄 NUEVO: Método para redimensionar bitmap
    private Bitmap resizeBitmap(Bitmap original) {
        if (original == null) return null;

        int width = original.getWidth();
        int height = original.getHeight();

        // Si la imagen ya es pequeña, devolverla sin cambios
        if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
            return original;
        }

        float scale;
        if (width > height) {
            scale = (float) MAX_IMAGE_DIMENSION / width;
        } else {
            scale = (float) MAX_IMAGE_DIMENSION / height;
        }

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        Bitmap resized = Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
        Log.d(TAG, "📐 Imagen redimensionada: " + width + "x" + height + " → " + newWidth + "x" + newHeight);

        return resized;
    }

    // 🔄 NUEVO: Convertir Bitmap a Base64
    private String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, baos);
        byte[] byteArray = baos.toByteArray();

        String base64 = Base64.encodeToString(byteArray, Base64.DEFAULT);

        Log.d(TAG, "📊 Tamaño Base64: " + (base64.length() / 1024) + " KB");

        return base64;
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar imagen de portada");
        builder.setItems(new CharSequence[]{"Tomar foto", "Seleccionar de galería"},
                (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndOpen();
                    } else {
                        openGallery();
                    }
                });
        builder.show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(cameraIntent);
        } else {
            Toast.makeText(this, "No hay aplicación de cámara disponible", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
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

        // Validaciones
        if (title.isEmpty()) {
            etTitle.setError("Ingresa el título del libro");
            etTitle.requestFocus();
            return;
        }

        if (title.length() < 3) {
            etTitle.setError("El título debe tener al menos 3 caracteres");
            etTitle.requestFocus();
            return;
        }

        if (author.isEmpty()) {
            etAuthor.setError("Ingresa el autor del libro");
            etAuthor.requestFocus();
            return;
        }

        if (author.length() < 3) {
            etAuthor.setError("El nombre del autor debe tener al menos 3 caracteres");
            etAuthor.requestFocus();
            return;
        }

        if (category.equals("Selecciona una categoría") || category.isEmpty()) {
            Toast.makeText(this, "⚠️ Selecciona una categoría válida", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isbn.isEmpty()) {
            isbn = isbn.replaceAll("[\\s-]", "");
            if (isbn.length() != 10 && isbn.length() != 13) {
                etIsbn.setError("ISBN debe tener 10 o 13 dígitos");
                etIsbn.requestFocus();
                return;
            }
            if (!isbn.matches("\\d+")) {
                etIsbn.setError("ISBN debe contener solo números");
                etIsbn.requestFocus();
                return;
            }
        }

        if (!year.isEmpty()) {
            try {
                int yearInt = Integer.parseInt(year);
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                if (yearInt < 1000 || yearInt > currentYear) {
                    etYear.setError("Año inválido (1000-" + currentYear + ")");
                    etYear.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                etYear.setError("Año debe ser numérico");
                etYear.requestFocus();
                return;
            }
        }

        btnSubmitSuggestion.setEnabled(false);
        btnSubmitSuggestion.setText("Enviando...");

        Suggestion suggestion = new Suggestion(title, author, category, comments, currentUserEmail);
        suggestion.setStatus("Pendiente");
        suggestion.setEdition(edition);
        suggestion.setIsbn(isbn);
        suggestion.setYear(year);

        // 🔄 Convertir imagen a Base64 si existe
        if (capturedImageBitmap != null) {
            Log.d(TAG, "📸 Convirtiendo imagen a Base64...");
            String base64Image = bitmapToBase64(capturedImageBitmap);
            suggestion.setCoverImageBase64(base64Image);
        }

        submitSuggestionToFirestore(suggestion);
    }

    private void submitSuggestionToFirestore(Suggestion suggestion) {
        Log.d(TAG, "💾 Guardando sugerencia en Firestore...");

        Map<String, Object> suggestionData = new HashMap<>();
        suggestionData.put("title", suggestion.getTitle());
        suggestionData.put("author", suggestion.getAuthor());
        suggestionData.put("category", suggestion.getCategory());
        suggestionData.put("comments", suggestion.getComments());
        suggestionData.put("edition", suggestion.getEdition());
        suggestionData.put("isbn", suggestion.getIsbn());
        suggestionData.put("year", suggestion.getYear());
        suggestionData.put("coverImageBase64", suggestion.getCoverImageBase64()); // 🔄 Base64
        suggestionData.put("userEmail", currentUserEmail);
        suggestionData.put("status", "Pendiente");
        suggestionData.put("suggestionDate", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("sugerencias")
                .add(suggestionData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "✅ Sugerencia guardada con ID: " + documentReference.getId());
                    Toast.makeText(this, "✅ ¡Sugerencia enviada exitosamente!", Toast.LENGTH_LONG).show();
                    limpiarFormulario();
                    btnSubmitSuggestion.setEnabled(true);
                    btnSubmitSuggestion.setText("Enviar Sugerencia");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error guardando sugerencia: " + e.getMessage());
                    Toast.makeText(this, "❌ Error al enviar sugerencia: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
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
        selectedImageUri = null;
        capturedImageBitmap = null;
    }

    private void listenUserSuggestions() {
        db.collection("sugerencias")
                .whereEqualTo("userEmail", currentUserEmail)
                .orderBy("suggestionDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error al cargar sugerencias", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        userSuggestions.clear();
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            Suggestion suggestion = dc.getDocument().toObject(Suggestion.class);
                            userSuggestions.add(suggestion);
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