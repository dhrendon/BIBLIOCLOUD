package com.example.bibliocloud;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SuggestionsActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private EditText etTitle, etAuthor, etComments;
    private EditText etEdition, etIsbn, etYear; // 🆕 Agregado etYear
    private Spinner spinnerCategory;
    private Button btnSubmitSuggestion, btnSelectImage; // 🆕 Botón para imagen
    private ImageView ivCoverPreview; // 🆕 Preview de imagen
    private RecyclerView recyclerViewSuggestions;
    private LinearLayout layoutEmpty;
    private CardView layoutForm;
    private TextView tvMySuggestions;

    private SuggestionsAdapter suggestionsAdapter;
    private List<Suggestion> userSuggestions;
    private String currentUserEmail;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    // 🆕 Variables para manejo de imagen
    private Uri selectedImageUri;
    private Bitmap capturedImageBitmap;

    // 🆕 Launchers para captura/selección de imagen
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestions);

        initializeViews();
        setupToolbar();
        setupUserInfo();
        setupCategories();
        setupRecyclerView();
        setupListeners();
        setupImageLaunchers(); // 🆕

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        listenUserSuggestions();
    }

    private void initializeViews() {
        etTitle = findViewById(R.id.etTitle);
        etAuthor = findViewById(R.id.etAuthor);
        etComments = findViewById(R.id.etComments);
        etEdition = findViewById(R.id.etEdition); // 🆕
        etIsbn = findViewById(R.id.etIsbn); // 🆕
        etYear = findViewById(R.id.etYear); // 🆕 Campo año
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSubmitSuggestion = findViewById(R.id.btnSubmitSuggestion);
        btnSelectImage = findViewById(R.id.btnSelectImage); // 🆕
        ivCoverPreview = findViewById(R.id.ivCoverPreview); // 🆕
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
        btnSelectImage.setOnClickListener(v -> showImageSourceDialog()); // 🆕
    }

    // 🆕 Configurar launchers para imagen
    private void setupImageLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        capturedImageBitmap = (Bitmap) extras.get("data");
                        ivCoverPreview.setImageBitmap(capturedImageBitmap);
                        ivCoverPreview.setVisibility(View.VISIBLE);
                        selectedImageUri = null; // Limpiar URI si había
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        ivCoverPreview.setImageURI(selectedImageUri);
                        ivCoverPreview.setVisibility(View.VISIBLE);
                        capturedImageBitmap = null; // Limpiar bitmap si había
                    }
                });
    }

    // 🆕 Mostrar diálogo para elegir entre cámara o galería
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

    // 🆕 Verificar permisos de cámara
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

    // 🆕 Abrir cámara
    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(cameraIntent);
        } else {
            Toast.makeText(this, "No hay aplicación de cámara disponible", Toast.LENGTH_SHORT).show();
        }
    }

    // 🆕 Abrir galería
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
        String edition = etEdition.getText().toString().trim(); // 🆕
        String isbn = etIsbn.getText().toString().trim(); // 🆕
        String year = etYear.getText().toString().trim(); // 🆕 Campo año

        if (title.isEmpty()) {
            etTitle.setError("Ingresa el título del libro");
            return;
        }

        if (author.isEmpty()) {
            etAuthor.setError("Ingresa el autor del libro");
            return;
        }

        // Deshabilitar botón mientras se sube
        btnSubmitSuggestion.setEnabled(false);
        btnSubmitSuggestion.setText("Enviando...");

        Suggestion suggestion = new Suggestion(title, author, category, comments, currentUserEmail);
        suggestion.setStatus("Pendiente");
        suggestion.setEdition(edition); // 🆕
        suggestion.setIsbn(isbn); // 🆕
        suggestion.setYear(year); // 🆕 Establecer año

        // 🆕 Si hay imagen, subirla primero
        if (selectedImageUri != null || capturedImageBitmap != null) {
            uploadImageAndSubmit(suggestion);
        } else {
            submitSuggestionToFirestore(suggestion);
        }
    }

    // 🆕 Subir imagen a Firebase Storage
    private void uploadImageAndSubmit(Suggestion suggestion) {
        String fileName = "suggestions/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storageRef.child(fileName);

        byte[] data;

        if (capturedImageBitmap != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            capturedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            data = baos.toByteArray();
        } else if (selectedImageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                data = baos.toByteArray();
            } catch (IOException e) {
                Toast.makeText(this, "Error al procesar imagen", Toast.LENGTH_SHORT).show();
                btnSubmitSuggestion.setEnabled(true);
                btnSubmitSuggestion.setText("Enviar Sugerencia");
                return;
            }
        } else {
            submitSuggestionToFirestore(suggestion);
            return;
        }

        imageRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            suggestion.setCoverImageUrl(uri.toString());
                            submitSuggestionToFirestore(suggestion);
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al subir imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSubmitSuggestion.setEnabled(true);
                    btnSubmitSuggestion.setText("Enviar Sugerencia");
                });
    }

    // 🆕 Enviar sugerencia a Firestore
    private void submitSuggestionToFirestore(Suggestion suggestion) {
        db.collection("sugerencias")
                .add(suggestion)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "¡Sugerencia enviada exitosamente!", Toast.LENGTH_LONG).show();
                    limpiarFormulario();
                    btnSubmitSuggestion.setEnabled(true);
                    btnSubmitSuggestion.setText("Enviar Sugerencia");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al enviar sugerencia: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSubmitSuggestion.setEnabled(true);
                    btnSubmitSuggestion.setText("Enviar Sugerencia");
                });
    }

    private void limpiarFormulario() {
        etTitle.setText("");
        etAuthor.setText("");
        etComments.setText("");
        etEdition.setText(""); // 🆕
        etIsbn.setText(""); // 🆕
        etYear.setText(""); // 🆕 Limpiar año
        spinnerCategory.setSelection(0);
        ivCoverPreview.setVisibility(View.GONE); // 🆕
        ivCoverPreview.setImageDrawable(null); // 🆕
        selectedImageUri = null; // 🆕
        capturedImageBitmap = null; // 🆕
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