package com.example.bibliocloud;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bibliocloud.adapters.SuggestionsAdapter;
import com.example.bibliocloud.models.Suggestion;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class SuggestionsActivity extends AppCompatActivity {

    private EditText etTitle, etAuthor, etComments;
    private Spinner spinnerCategory;
    private Button btnSubmitSuggestion;
    private RecyclerView recyclerViewSuggestions;
    private LinearLayout layoutEmpty;
    private CardView layoutForm;
    private TextView tvMySuggestions;

    private SuggestionsAdapter suggestionsAdapter;
    private List<Suggestion> userSuggestions;
    private String currentUserEmail;

    private FirebaseFirestore db;

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

        db = FirebaseFirestore.getInstance();
        listenUserSuggestions();
    }

    private void initializeViews() {
        etTitle = findViewById(R.id.etTitle);
        etAuthor = findViewById(R.id.etAuthor);
        etComments = findViewById(R.id.etComments);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSubmitSuggestion = findViewById(R.id.btnSubmitSuggestion);
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
    }

    private void submitSuggestion() {
        String title = etTitle.getText().toString().trim();
        String author = etAuthor.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String comments = etComments.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Ingresa el título del libro");
            return;
        }

        if (author.isEmpty()) {
            etAuthor.setError("Ingresa el autor del libro");
            return;
        }

        Suggestion suggestion = new Suggestion(title, author, category, comments, currentUserEmail);
        suggestion.setStatus("Pendiente");

        db.collection("sugerencias")
                .add(suggestion)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "¡Sugerencia enviada exitosamente!", Toast.LENGTH_LONG).show();
                    limpiarFormulario();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al enviar sugerencia: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void limpiarFormulario() {
        etTitle.setText("");
        etAuthor.setText("");
        etComments.setText("");
        spinnerCategory.setSelection(0);
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
