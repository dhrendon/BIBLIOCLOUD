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

    @PropertyName("anio")
    private String year;

    @PropertyName("editorial")
    private String publisher;

    @PropertyName("numero_edicion")
    private String editionNumber;

    @PropertyName("isbn")
    private String isbn;

    @PropertyName("categoria")
    private String category;

    @PropertyName("numero_paginas")
    private int pageCount;

    @PropertyName("idioma")
    private String language;

    @PropertyName("descripcion")
    private String description;

    @PropertyName("estado")
    private String status;

    @PropertyName("foto")
    private String fotoBase64;

    // Campos locales (no en Firestore)
    private boolean isFavorite;
    private float rating;
    private int ratingCount;
    private long createdAt;

    // Constructor vacío (Firebase lo necesita)
    public Book() {
        this.createdAt = System.currentTimeMillis();
        this.status = "Disponible";
        this.language = "Español";
        this.pageCount = 0;
    }

    // Constructor completo
    public Book(String id, String title, String author, String year, String publisher,
                String editionNumber, String isbn, String category, int pageCount,
                String language, String description, String status, String fotoBase64) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.year = year;
        this.publisher = publisher;
        this.editionNumber = editionNumber;
        this.isbn = isbn;
        this.category = category;
        this.pageCount = pageCount;
        this.language = language;
        this.description = description;
        this.status = status;
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

    @PropertyName("anio")
    public String getYear() { return year != null ? year : "Año no disponible"; }
    @PropertyName("anio")
    public void setYear(String year) { this.year = year; }

    @PropertyName("editorial")
    public String getPublisher() { return publisher != null ? publisher : "Editorial no disponible"; }
    @PropertyName("editorial")
    public void setPublisher(String publisher) { this.publisher = publisher; }

    @PropertyName("numero_edicion")
    public String getEditionNumber() { return editionNumber != null ? editionNumber : "1"; }
    @PropertyName("numero_edicion")
    public void setEditionNumber(String editionNumber) { this.editionNumber = editionNumber; }

    @PropertyName("isbn")
    public String getIsbn() { return isbn != null ? isbn : "Sin ISBN"; }
    @PropertyName("isbn")
    public void setIsbn(String isbn) { this.isbn = isbn; }

    @PropertyName("categoria")
    public String getCategory() { return category != null ? category : "Sin categoría"; }
    @PropertyName("categoria")
    public void setCategory(String category) { this.category = category; }

    @PropertyName("numero_paginas")
    public int getPageCount() { return pageCount; }
    @PropertyName("numero_paginas")
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }

    @PropertyName("idioma")
    public String getLanguage() { return language != null ? language : "Español"; }
    @PropertyName("idioma")
    public void setLanguage(String language) { this.language = language; }

    @PropertyName("descripcion")
    public String getDescription() { return description != null ? description : "Sin descripción"; }
    @PropertyName("descripcion")
    public void setDescription(String description) { this.description = description; }

    @PropertyName("estado")
    public String getStatus() { return status != null ? status : "Disponible"; }
    @PropertyName("estado")
    public void setStatus(String status) { this.status = status; }

    // === Imagen en Base64 ===
    @PropertyName("foto")
    public String getFotoBase64() { return fotoBase64; }

    @PropertyName("foto")
    public void setFotoBase64(String fotoBase64) { this.fotoBase64 = fotoBase64; }

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

    // Método para información completa del libro
    @Exclude
    public String getFullInfo() {
        return String.format("📖 %s\n✍️ %s\n📅 %s\n🏢 %s\n📄 Edición: %s\n📚 ISBN: %s\n🔖 %s\n📄 %d páginas\n🌐 %s",
                title, author, year, publisher, editionNumber, isbn, category, pageCount, language);
    }
}