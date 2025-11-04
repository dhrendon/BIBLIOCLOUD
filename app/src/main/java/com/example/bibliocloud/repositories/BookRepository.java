package com.example.bibliocloud.repositories;

import android.net.Uri;
import android.util.Log;
import com.example.bibliocloud.models.Book;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BookRepository {
    private static final String TAG = "BookRepository";
    private final FirebaseFirestore db;
    private final CollectionReference booksRef;
    private final StorageReference storageRef;

    public BookRepository() {
        db = FirebaseFirestore.getInstance();
        booksRef = db.collection("books");
        storageRef = FirebaseStorage.getInstance().getReference();
    }

    // MÉTODO CORREGIDO: Crear libro
    public void addBook(Book book, OnCompleteListener listener) {
        // Generar ID único
        String bookId = booksRef.document().getId();
        book.setId(bookId);

        Log.d(TAG, "Intentando guardar libro: " + book.getTitle());

        // Convertir a Map para mejor control
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("id", book.getId());
        bookData.put("title", book.getTitle());
        bookData.put("author", book.getAuthor());
        bookData.put("category", book.getCategory());
        bookData.put("year", book.getYear());
        bookData.put("status", book.getStatus());
        bookData.put("isFavorite", book.isFavorite());
        bookData.put("imageUrl", book.getFotoBase64() != null ? book.getFotoBase64() : "");
        bookData.put("rating", book.getRating());
        bookData.put("ratingCount", book.getRatingCount());
        bookData.put("createdAt", System.currentTimeMillis());

        booksRef.document(bookId)
                .set(bookData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Libro guardado exitosamente: " + bookId);
                    listener.onSuccess(bookId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Error al guardar libro: " + e.getMessage(), e);
                    listener.onFailure(e);
                });
    }

    // MÉTODO ALTERNATIVO: Usando el objeto directamente
    public void addBookDirect(Book book, OnCompleteListener listener) {
        String bookId = booksRef.document().getId();
        book.setId(bookId);

        Log.d(TAG, "Guardando libro (método directo): " + book.getTitle());

        booksRef.document(bookId)
                .set(book)  // Firebase convierte automáticamente
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Libro guardado: " + bookId);
                    listener.onSuccess(bookId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Error: " + e.getMessage(), e);
                    listener.onFailure(e);
                });
    }

    // Obtener todos los libros con logs
    public void getAllBooks(OnBooksLoadedListener listener) {
        Log.d(TAG, "Cargando todos los libros...");

        booksRef.orderBy("title", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "✗ Error al cargar libros: " + error.getMessage(), error);
                        listener.onFailure(error);
                        return;
                    }

                    if (value != null) {
                        int count = value.size();
                        Log.d(TAG, "✓ Libros cargados: " + count);
                        listener.onBooksLoaded(value.toObjects(Book.class));
                    } else {
                        Log.w(TAG, "⚠ No se encontraron libros");
                        listener.onBooksLoaded(new java.util.ArrayList<>());
                    }
                });
    }

    // Obtener libros por categoría
    public void getBooksByCategory(String category, OnBooksLoadedListener listener) {
        Log.d(TAG, "Buscando libros en categoría: " + category);

        booksRef.whereEqualTo("category", category)
                .orderBy("title", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "✓ Encontrados " + querySnapshot.size() + " libros");
                    listener.onBooksLoaded(querySnapshot.toObjects(Book.class));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Error en búsqueda: " + e.getMessage(), e);
                    listener.onFailure(e);
                });
    }

    // Obtener libros por estado
    public void getBooksByStatus(String status, OnBooksLoadedListener listener) {
        Log.d(TAG, "Buscando libros con estado: " + status);

        booksRef.whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "✓ Encontrados " + querySnapshot.size() + " libros");
                    listener.onBooksLoaded(querySnapshot.toObjects(Book.class));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Error: " + e.getMessage(), e);
                    listener.onFailure(e);
                });
    }

    // Buscar libros
    public void searchBooks(String searchText, OnBooksLoadedListener listener) {
        Log.d(TAG, "Buscando: " + searchText);

        booksRef.orderBy("title")
                .startAt(searchText)
                .endAt(searchText + "\uf8ff")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "✓ Resultados: " + querySnapshot.size());
                    listener.onBooksLoaded(querySnapshot.toObjects(Book.class));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Error en búsqueda: " + e.getMessage(), e);
                    listener.onFailure(e);
                });
    }

    // Actualizar libro
    public void updateBook(Book book, OnCompleteListener listener) {
        Log.d(TAG, "Actualizando libro: " + book.getId());

        booksRef.document(book.getId())
                .set(book)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Libro actualizado");
                    listener.onSuccess(book.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Error al actualizar: " + e.getMessage(), e);
                    listener.onFailure(e);
                });
    }

    // Eliminar libro
    public void deleteBook(String bookId, OnCompleteListener listener) {
        Log.d(TAG, "Eliminando libro: " + bookId);

        booksRef.document(bookId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Libro eliminado");
                    listener.onSuccess(bookId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Error al eliminar: " + e.getMessage(), e);
                    listener.onFailure(e);
                });
    }

    // Actualizar estado del libro
    public void updateBookStatus(String bookId, String status, OnCompleteListener listener) {
        Log.d(TAG, "Actualizando estado del libro: " + bookId + " -> " + status);

        booksRef.document(bookId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Estado actualizado");
                    listener.onSuccess(bookId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Error: " + e.getMessage(), e);
                    listener.onFailure(e);
                });
    }

    // Marcar como favorito
    public void toggleFavorite(String bookId, boolean isFavorite, OnCompleteListener listener) {
        Log.d(TAG, "Marcando favorito: " + bookId + " = " + isFavorite);

        booksRef.document(bookId)
                .update("favorite", isFavorite)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Favorito actualizado");
                    listener.onSuccess(bookId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Error: " + e.getMessage(), e);
                    listener.onFailure(e);
                });
    }

    // Agregar valoración
    public void addRating(String bookId, float rating, OnCompleteListener listener) {
        Log.d(TAG, "Agregando valoración: " + bookId + " = " + rating);

        booksRef.document(bookId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Book book = documentSnapshot.toObject(Book.class);
                    if (book != null) {
                        book.addRating(rating);
                        updateBook(book, listener);
                    } else {
                        Log.e(TAG, "✗ Libro no encontrado");
                        listener.onFailure(new Exception("Libro no encontrado"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Error: " + e.getMessage(), e);
                    listener.onFailure(e);
                });
    }

    // Subir imagen a Storage
    public void uploadBookImage(Uri imageUri, OnImageUploadListener listener) {
        String fileName = "book_images/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storageRef.child(fileName);

        Log.d(TAG, "Subiendo imagen: " + fileName);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "✓ Imagen subida, obteniendo URL...");
                    imageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                Log.d(TAG, "✓ URL obtenida: " + uri.toString());
                                listener.onImageUploaded(uri);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "✗ Error al obtener URL: " + e.getMessage(), e);
                                listener.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Error al subir imagen: " + e.getMessage(), e);
                    listener.onFailure(e);
                });
    }

    // Interfaces
    public interface OnCompleteListener {
        void onSuccess(String id);
        void onFailure(Exception e);
    }

    public interface OnBooksLoadedListener {
        void onBooksLoaded(java.util.List<Book> books);
        void onFailure(Exception e);
    }

    public interface OnImageUploadListener {
        void onImageUploaded(Uri downloadUrl);
        void onFailure(Exception e);
    }
}