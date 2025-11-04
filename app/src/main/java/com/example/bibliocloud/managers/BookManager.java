package com.example.bibliocloud.managers;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.bibliocloud.models.Book;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class BookManager {
    private static final String PREF_NAME = "BooksData";
    private SharedPreferences prefs;
    private FirebaseFirestore db;

    public BookManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();
    }

    // Método para obtener libros desde Firebase
    public void getBooks(BooksLoadListener listener) {
        db.collection("books")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Book> books = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Book book = document.toObject(Book.class);
                            book.setId(document.getId());

                            // Cargar valoraciones desde SharedPreferences como respaldo
                            loadSavedRatings(book);

                            books.add(book);
                        }
                        listener.onBooksLoaded(books);
                    } else {
                        // En caso de error, cargar libros por defecto
                        listener.onBooksLoaded(getDefaultBooks());
                    }
                });
    }

    // Método para obtener libros con filtro
    public void getBooksWithFilter(String filter, BooksLoadListener listener) {
        db.collection("books")
                .whereGreaterThanOrEqualTo("titulo", filter)
                .whereLessThanOrEqualTo("titulo", filter + "\uf8ff")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Book> books = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Book book = document.toObject(Book.class);
                            book.setId(document.getId());
                            loadSavedRatings(book);
                            books.add(book);
                        }
                        listener.onBooksLoaded(books);
                    } else {
                        listener.onBooksLoaded(new ArrayList<>());
                    }
                });
    }

    // Método para obtener un libro por ID
    public void getBookById(String bookId, BookLoadListener listener) {
        db.collection("books")
                .document(bookId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Book book = task.getResult().toObject(Book.class);
                        if (book != null) {
                            book.setId(task.getResult().getId());
                            loadSavedRatings(book);
                            listener.onBookLoaded(book);
                        } else {
                            listener.onBookLoaded(null);
                        }
                    } else {
                        listener.onBookLoaded(null);
                    }
                });
    }

    // Método para actualizar rating en Firebase
    public void updateBookRating(String bookId, float newRating) {
        // Primero obtener el libro actual para calcular el nuevo rating
        getBookById(bookId, new BookLoadListener() {
            @Override
            public void onBookLoaded(Book book) {
                if (book != null) {
                    // Calcular nuevo rating
                    float currentRating = book.getRating();
                    int currentCount = book.getRatingCount();

                    float totalRating = currentRating * currentCount;
                    int newCount = currentCount + 1;
                    float newAverage = (totalRating + newRating) / newCount;

                    // Actualizar en Firebase
                    db.collection("books")
                            .document(bookId)
                            .update(
                                    "rating", newAverage,
                                    "ratingCount", newCount
                            )
                            .addOnSuccessListener(aVoid -> {
                                // También guardar localmente como respaldo
                                saveRatingLocally(bookId, newAverage, newCount);
                            })
                            .addOnFailureListener(e -> {
                                // Si falla Firebase, guardar solo localmente
                                saveRatingLocally(bookId, newAverage, newCount);
                            });
                }
            }
        });
    }

    // Guardar rating localmente como respaldo
    private void saveRatingLocally(String bookId, float rating, int count) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(bookId + "_avg", rating);
        editor.putInt(bookId + "_count", count);
        editor.apply();
    }

    // Cargar ratings desde SharedPreferences
    private void loadSavedRatings(Book book) {
        float rating = prefs.getFloat(book.getId() + "_avg", 0);
        int count = prefs.getInt(book.getId() + "_count", 0);

        // Solo actualizar si Firebase no tiene datos de rating
        if (book.getRating() == 0 && rating > 0) {
            book.setRating(rating);
            book.setRatingCount(count);
        }
    }

    // Método para obtener libros por categoría
    public void getBooksByCategory(String category, BooksLoadListener listener) {
        db.collection("books")
                .whereEqualTo("categoria", category)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Book> books = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Book book = document.toObject(Book.class);
                            book.setId(document.getId());
                            loadSavedRatings(book);
                            books.add(book);
                        }
                        listener.onBooksLoaded(books);
                    } else {
                        listener.onBooksLoaded(new ArrayList<>());
                    }
                });
    }

    // Método para buscar libros por título o autor
    public void searchBooks(String query, BooksLoadListener listener) {
        // Buscar por título
        db.collection("books")
                .whereGreaterThanOrEqualTo("titulo", query)
                .whereLessThanOrEqualTo("titulo", query + "\uf8ff")
                .get()
                .addOnCompleteListener(task -> {
                    List<Book> results = new ArrayList<>();

                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Book book = document.toObject(Book.class);
                            book.setId(document.getId());
                            loadSavedRatings(book);
                            results.add(book);
                        }
                    }

                    // También buscar por autor
                    db.collection("books")
                            .whereGreaterThanOrEqualTo("autor", query)
                            .whereLessThanOrEqualTo("autor", query + "\uf8ff")
                            .get()
                            .addOnCompleteListener(authorTask -> {
                                if (authorTask.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : authorTask.getResult()) {
                                        Book book = document.toObject(Book.class);
                                        book.setId(document.getId());
                                        loadSavedRatings(book);

                                        // Evitar duplicados
                                        boolean exists = false;
                                        for (Book existing : results) {
                                            if (existing.getId().equals(book.getId())) {
                                                exists = true;
                                                break;
                                            }
                                        }
                                        if (!exists) {
                                            results.add(book);
                                        }
                                    }
                                }
                                listener.onBooksLoaded(results);
                            });
                });
    }

    // Libros por defecto (solo como respaldo)
    private List<Book> getDefaultBooks() {
        List<Book> defaultBooks = new ArrayList<>();
        defaultBooks.add(new Book("1", "Cien años de soledad", "Gabriel García Márquez",
                "Novela", "1967", "Disponible", "Novela clásica del realismo mágico", "libro_cien_anos"));
        defaultBooks.add(new Book("2", "1984", "George Orwell",
                "Ciencia Ficción", "1949", "Prestado", "Distopía política", "libro_1984"));
        defaultBooks.add(new Book("3", "El principito", "Antoine de Saint-Exupéry",
                "Fábula", "1943", "Disponible", "Fábula filosófica", "libro_principito"));
        defaultBooks.add(new Book("4", "Don Quijote de la Mancha", "Miguel de Cervantes",
                "Novela", "1605", "Disponible", "Clásico de la literatura española", "libro_quijote"));
        defaultBooks.add(new Book("5", "Orgullo y prejuicio", "Jane Austen",
                "Romance", "1813", "Reservado", "Novela romántica clásica", "libro_orgullo"));

        // Cargar valoraciones guardadas
        for (Book book : defaultBooks) {
            loadSavedRatings(book);
        }
        return defaultBooks;
    }

    // Interfaces para callbacks
    public interface BooksLoadListener {
        void onBooksLoaded(List<Book> books);
    }

    public interface BookLoadListener {
        void onBookLoaded(Book book);
    }
}