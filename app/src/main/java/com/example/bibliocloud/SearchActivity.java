package com.example.bibliocloud;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bibliocloud.adapters.BookAdapter;
import com.example.bibliocloud.dialogs.RatingDialog;
import com.example.bibliocloud.models.Book;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private ImageButton btnSearch;
    private Spinner spinnerFilter;
    private Button btnClearFilters;
    private RecyclerView recyclerViewBooks;
    private TextView tvResultsCount;
    private LinearLayout layoutEmpty;

    private BookAdapter bookAdapter;
    private List<Book> allBooks = new ArrayList<>();
    private List<Book> filteredBooks = new ArrayList<>();

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupFilters();

        db = FirebaseFirestore.getInstance();
        loadBooksFromFirebase();
    }

    private void initializeViews() {
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        btnClearFilters = findViewById(R.id.btnClearFilters);
        recyclerViewBooks = findViewById(R.id.recyclerViewBooks);
        tvResultsCount = findViewById(R.id.tvResultsCount);
        layoutEmpty = findViewById(R.id.layoutEmpty);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Buscar Libros");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // Cargar libros desde Firestore
    private void loadBooksFromFirebase() {
        db.collection("libros").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", "Error al obtener libros", error);
                Toast.makeText(SearchActivity.this, "Error al cargar libros", Toast.LENGTH_SHORT).show();
                return;
            }

            allBooks.clear();

            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    try {
                        // DEBUG: Mostrar datos del documento
                        Log.d("FIREBASE_FIELDS", "Documento: " + doc.getId());
                        for (String field : doc.getData().keySet()) {
                            Log.d("FIREBASE_FIELDS", " - " + field + ": " + doc.get(field));
                        }

                        // Mapeo automático del documento a Book
                        Book book = doc.toObject(Book.class);
                        book.setId(doc.getId());

                        // 🔥 Aseguramos compatibilidad con el campo "foto" (Base64)
                        if (doc.contains("foto")) {
                            String base64 = doc.getString("foto");
                            book.setFotoBase64(base64 != null ? base64 : "");
                        }

                        // 🔥 Validar que los campos principales existan
                        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
                            book.setTitle("Título desconocido");
                        }
                        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
                            book.setAuthor("Autor desconocido");
                        }

                        allBooks.add(book);

                        Log.d("BOOK_MAPPED",
                                "✔ Libro cargado: " + book.getTitle() +
                                        " | Autor: " + book.getAuthor() +
                                        " | Año: " + book.getYear() +
                                        " | Foto: " + (book.getFotoBase64() != null && !book.getFotoBase64().isEmpty()));
                    } catch (Exception e) {
                        Log.e("Firestore", "Error mapeando documento: " + doc.getId(), e);
                    }
                }
            }

            Log.d("SearchActivity", "📚 Total libros cargados: " + allBooks.size());

            // Cargar valoraciones locales y ejecutar búsqueda
            loadSavedRatings();
            performSearch();
        });
    }


    private void setupRecyclerView() {
        bookAdapter = new BookAdapter(filteredBooks, new BookAdapter.OnBookClickListener() {
            @Override
            public void onBookClick(Book book) {
                Toast.makeText(SearchActivity.this, "Seleccionado: " + book.getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFavoriteClick(Book book) {
                book.setFavorite(!book.isFavorite());
                saveFavoriteState(book);
                bookAdapter.notifyDataSetChanged();
                String message = book.isFavorite() ? "Agregado a favoritos" : "Removido de favoritos";
                Toast.makeText(SearchActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBookLongClick(Book book) {
                showRatingDialog(book);
            }
        });

        recyclerViewBooks.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBooks.setAdapter(bookAdapter);
    }

    // Diálogo para valoraciones
    private void showRatingDialog(Book book) {
        RatingDialog ratingDialog = new RatingDialog(this, book, new RatingDialog.OnRatingSubmittedListener() {
            @Override
            public void onRatingSubmitted(Book book, float rating) {
                bookAdapter.notifyDataSetChanged();
                updateBookInAllBooks(book);
                Toast.makeText(SearchActivity.this,
                        "¡Gracias! Valoraste \"" + book.getTitle() + "\" con " + rating,
                        Toast.LENGTH_LONG).show();
            }
        });
        ratingDialog.show();
    }

    private void updateBookInAllBooks(Book updatedBook) {
        for (Book book : allBooks) {
            if (book.getId().equals(updatedBook.getId())) {
                book.setRating(updatedBook.getRating());
                book.setRatingCount(updatedBook.getRatingCount());
                break;
            }
        }
    }

    private void saveFavoriteState(Book book) {
        SharedPreferences prefs = getSharedPreferences("Favorites", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(book.getId(), book.isFavorite());
        editor.apply();
    }

    private void loadSavedRatings() {
        SharedPreferences prefs = getSharedPreferences("BookRatings", MODE_PRIVATE);
        for (Book book : allBooks) {
            float rating = prefs.getFloat(book.getId() + "_avg", 0);
            int ratingCount = prefs.getInt(book.getId() + "_count", 0);
            book.setRating(rating);
            book.setRatingCount(ratingCount);
        }
    }

    private void setupSearch() {
        btnSearch.setOnClickListener(v -> performSearch());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch();
            }
        });
    }

    private void setupFilters() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.search_filters, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                performSearch();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnClearFilters.setOnClickListener(v -> {
            etSearch.setText("");
            spinnerFilter.setSelection(0);
            performSearch();
            Toast.makeText(SearchActivity.this, "Filtros limpiados", Toast.LENGTH_SHORT).show();
        });
    }

    // === MÉTODO DE BÚSQUEDA GENERAL ===
    private void performSearch() {
        try {
            String query = etSearch.getText() != null ? etSearch.getText().toString() : "";
            if (query == null) query = "";

            String cleanQuery = query.trim().toLowerCase();
            String selectedFilter = spinnerFilter.getSelectedItem() != null ?
                    spinnerFilter.getSelectedItem().toString() : "Todos";

            filteredBooks.clear();

            for (Book book : allBooks) {
                boolean matchesSearch = cleanQuery.isEmpty() || matchesBook(book, cleanQuery);
                boolean matchesFilter = selectedFilter.equals("Todos") ||
                        (book.getCategory() != null &&
                                book.getCategory().equalsIgnoreCase(selectedFilter));

                if (matchesSearch && matchesFilter) {
                    filteredBooks.add(book);
                }
            }

            bookAdapter.updateBooks(filteredBooks);
            updateResultsCount();

        } catch (Exception e) {
            Log.e("SearchActivity", "Error en performSearch: " + e.getMessage(), e);
            filteredBooks.clear();
            filteredBooks.addAll(allBooks);
            bookAdapter.updateBooks(filteredBooks);
            updateResultsCount();
        }
    }

    // === NUEVA VERSIÓN DE matchesBook ===
    private boolean matchesBook(Book book, String cleanQuery) {
        try {
            return (book.getTitle() != null && book.getTitle().toLowerCase().contains(cleanQuery)) ||
                    (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(cleanQuery)) ||
                    (book.getCategory() != null && book.getCategory().toLowerCase().contains(cleanQuery)) ||
                    (book.getYear() != null && book.getYear().toLowerCase().contains(cleanQuery)) ||
                    (book.getIsbn() != null && book.getIsbn().toLowerCase().contains(cleanQuery)) ||
                    (book.getStatus() != null && book.getStatus().toLowerCase().contains(cleanQuery)) ||
                    (book.getDescription() != null && book.getDescription().toLowerCase().contains(cleanQuery));
        } catch (Exception e) {
            Log.e("SearchActivity", "Error en matchesBook: " + e.getMessage());
            return false;
        }
    }





    private void updateResultsCount() {
        if (tvResultsCount != null) {
            tvResultsCount.setText("Resultados: " + filteredBooks.size());
        }

        if (layoutEmpty != null && recyclerViewBooks != null) {
            if (filteredBooks.isEmpty()) {
                layoutEmpty.setVisibility(View.VISIBLE);
                recyclerViewBooks.setVisibility(View.GONE);
            } else {
                layoutEmpty.setVisibility(View.GONE);
                recyclerViewBooks.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedRatings();
        if (bookAdapter != null) {
            bookAdapter.notifyDataSetChanged();
        }
    }
}