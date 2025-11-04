package com.example.bibliocloud.models;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

public class Book {

    @DocumentId
    private String id;

    @PropertyName("titulo")
    private String title;

    @PropertyName("autor")
    private String author;

    @PropertyName("categoria")
    private String category;

    @PropertyName("anio")
    private String year;

    @PropertyName("isbn")
    private String isbn;

    @PropertyName("estado")
    private String status;

    @PropertyName("descripcion")
    private String description;

    @PropertyName("foto")
    private String fotoBase64; // 🔥 Se cambió a fotoBase64 para claridad

    // Campos locales
    private boolean isFavorite;
    private float rating;
    private int ratingCount;
    private long createdAt;

    // Constructor vacío (Firebase lo necesita)
    public Book() {
        this.createdAt = System.currentTimeMillis();
    }

    public Book(String id, String title, String author, String category, String year,
                String status, String description, String fotoBase64) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.year = year;
        this.status = status;
        this.description = description;
        this.fotoBase64 = fotoBase64;
        this.isFavorite = false;
        this.rating = 0.0f;
        this.ratingCount = 0;
        this.createdAt = System.currentTimeMillis();
    }

    // === Getters y Setters ===
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("titulo")
    public String getTitle() { return title != null ? title : "Título no disponible"; }
    @PropertyName("titulo")
    public void setTitle(String title) { this.title = title; }

    @PropertyName("autor")
    public String getAuthor() { return author != null ? author : "Autor no disponible"; }
    @PropertyName("autor")
    public void setAuthor(String author) { this.author = author; }

    @PropertyName("categoria")
    public String getCategory() { return category != null ? category : "Sin categoría"; }
    @PropertyName("categoria")
    public void setCategory(String category) { this.category = category; }

    @PropertyName("anio")
    public String getYear() { return year != null ? year : "Año no disponible"; }
    @PropertyName("anio")
    public void setYear(String year) { this.year = year; }

    @PropertyName("isbn")
    public String getIsbn() { return isbn != null ? isbn : "Sin ISBN"; }
    @PropertyName("isbn")
    public void setIsbn(String isbn) { this.isbn = isbn; }

    @PropertyName("estado")
    public String getStatus() { return status != null ? status : "Disponible"; }
    @PropertyName("estado")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("descripcion")
    public String getDescription() { return description != null ? description : "Sin descripción"; }
    @PropertyName("descripcion")
    public void setDescription(String description) { this.description = description; }

    // === Imagen en Base64 ===
    @PropertyName("foto")
    public String getFotoBase64() { return fotoBase64; }

    @PropertyName("foto")
    public void setFotoBase64(String fotoBase64) { this.fotoBase64 = fotoBase64; }

    // 🔥 Convierte la cadena Base64 a Bitmap (para mostrar en ImageView)
    @Exclude
    public Bitmap getFotoAsBitmap() {
        try {
            if (fotoBase64 == null || fotoBase64.isEmpty()) return null;
            byte[] decodedBytes = Base64.decode(fotoBase64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    // === Campos locales ===
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    @Exclude
    public int getYearAsInt() {
        try { return Integer.parseInt(year); }
        catch (NumberFormatException e) { return 0; }
    }

    public void addRating(float newRating) {
        float totalRating = this.rating * this.ratingCount;
        this.ratingCount++;
        this.rating = (totalRating + newRating) / this.ratingCount;
    }
}
