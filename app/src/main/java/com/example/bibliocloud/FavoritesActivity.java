package com.example.bibliocloud;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bibliocloud.adapters.BookAdapter;
import com.example.bibliocloud.dialogs.RatingDialog;
import com.example.bibliocloud.models.Book;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity implements BookAdapter.OnBookClickListener {

    private RecyclerView recyclerViewFavorites;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyMessage;
    private TextView tvFavoritesCount;

    private BookAdapter bookAdapter;
    private List<Book> favoriteBooks;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        initializeViews();
        setupToolbar();
        initializeFirebase();
        setupRecyclerView();
        loadFavorites();
    }

    private void initializeViews() {
        recyclerViewFavorites = findViewById(R.id.recyclerViewFavorites);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        tvFavoritesCount = findViewById(R.id.tvFavoritesCount);
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mis Favoritos");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        favoriteBooks = new ArrayList<>();

        bookAdapter = new BookAdapter(favoriteBooks, new BookAdapter.OnBookClickListener() {
            @Override
            public void onBookClick(Book book) {
                showBookOptions(book);
            }

            @Override
            public void onFavoriteClick(Book book) {
                removeFromFavorites(book);
            }

            @Override
            public void onBookLongClick(Book book) {
                showRatingDialog(book);
            }
        });

        recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFavorites.setAdapter(bookAdapter);
    }

    // 🔥 Adaptado a la colección "libros"
    private void loadFavorites() {
        favoriteBooks.clear();
        SharedPreferences prefs = getSharedPreferences("Favorites", MODE_PRIVATE);

        db.collection("libros") // 👈 Colección real en Firestore
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Book book = document.toObject(Book.class);
                            book.setId(document.getId());

                            // Verificar si es favorito (local)
                            boolean isFavorite = prefs.getBoolean(book.getId(), false);
                            book.setFavorite(isFavorite);

                            // Cargar valoraciones locales (respaldo)
                            loadSavedRatingsFromPrefs(book);

                            if (isFavorite) {
                                favoriteBooks.add(book);
                            }
                        }

                        bookAdapter.updateBooks(favoriteBooks);
                        updateEmptyState();
                        updateFavoritesCount();
                    } else {
                        Toast.makeText(this, "Error al cargar favoritos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadSavedRatingsFromPrefs(Book book) {
        SharedPreferences prefs = getSharedPreferences("BookRatings", MODE_PRIVATE);
        float rating = prefs.getFloat(book.getId() + "_avg", 0);
        int ratingCount = prefs.getInt(book.getId() + "_count", 0);

        // Si Firebase no tiene datos, usar respaldo local
        if (book.getRating() == 0 && rating > 0) {
            book.setRating(rating);
            book.setRatingCount(ratingCount);
        }
    }

    private void saveRatingToFirebase(Book book, float newRating) {
        float currentRating = book.getRating();
        int currentCount = book.getRatingCount();

        float totalRating = currentRating * currentCount;
        int newCount = currentCount + 1;
        float newAverage = (totalRating + newRating) / newCount;

        db.collection("libros") // 👈 Actualizado a "libros"
                .document(book.getId())
                .update(
                        "rating", newAverage,
                        "ratingCount", newCount
                )
                .addOnSuccessListener(aVoid -> {
                    book.setRating(newAverage);
                    book.setRatingCount(newCount);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al guardar valoración", Toast.LENGTH_SHORT).show()
                );
    }

    private void showRatingDialog(Book book) {
        RatingDialog ratingDialog = new RatingDialog(this, book, (bookRated, rating) -> {
            saveRatingToFirebase(bookRated, rating);
            bookAdapter.notifyDataSetChanged();
            Toast.makeText(this,
                    "¡Gracias! Valoraste \"" + bookRated.getTitle() + "\" con " + rating + " ⭐",
                    Toast.LENGTH_LONG).show();
        });
        ratingDialog.show();
    }

    private void removeFromFavorites(Book book) {
        book.setFavorite(false);
        saveFavoriteState(book);
        favoriteBooks.remove(book);
        bookAdapter.updateBooks(favoriteBooks);
        updateEmptyState();
        updateFavoritesCount();
        Toast.makeText(this, "Removido de favoritos: " + book.getTitle(), Toast.LENGTH_SHORT).show();
    }

    private void saveFavoriteState(Book book) {
        SharedPreferences prefs = getSharedPreferences("Favorites", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(book.getId(), book.isFavorite());
        editor.apply();
    }

    private void updateEmptyState() {
        if (favoriteBooks.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerViewFavorites.setVisibility(View.GONE);
            tvEmptyMessage.setText("No tienes libros favoritos\n\nAgrega libros a favoritos desde la búsqueda");
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerViewFavorites.setVisibility(View.VISIBLE);
        }
    }

    private void updateFavoritesCount() {
        if (tvFavoritesCount != null) {
            tvFavoritesCount.setText("Favoritos: " + favoriteBooks.size());
        }
    }

    private void showBookOptions(Book book) {
        Toast.makeText(this, "Opciones para: " + book.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    // Implementación de la interfaz del adaptador
    @Override
    public void onBookClick(Book book) {
        showBookOptions(book);
    }

    @Override
    public void onFavoriteClick(Book book) {
        removeFromFavorites(book);
    }

    @Override
    public void onBookLongClick(Book book) {
        showRatingDialog(book);
    }
}
