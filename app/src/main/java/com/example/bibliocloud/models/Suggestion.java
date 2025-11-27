package com.example.bibliocloud.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Suggestion {
    @DocumentId
    private String id;
    private String title;
    private String author;
    private String category;
    private String comments;
    private String userEmail;
    private String userId;

    // 🆕 CAMPOS ADICIONALES
    private String edition;
    private String isbn;
    private String year;

    // 🔄 CAMBIO: Imagen en Base64 en lugar de URL
    private String coverImageBase64;  // Imagen en formato Base64

    @ServerTimestamp
    private Date suggestionDate;
    private String status;

    public Suggestion() {}

    public Suggestion(String title, String author, String category, String comments, String userEmail) {
        this.id = "suggestion_" + System.currentTimeMillis();
        this.title = title;
        this.author = author;
        this.category = category;
        this.comments = comments;
        this.userEmail = userEmail;
        this.suggestionDate = new Date();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Date getSuggestionDate() { return suggestionDate; }
    public void setSuggestionDate(Date suggestionDate) { this.suggestionDate = suggestionDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEdition() { return edition; }
    public void setEdition(String edition) { this.edition = edition; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    // 🔄 GETTER Y SETTER PARA BASE64
    public String getCoverImageBase64() { return coverImageBase64; }
    public void setCoverImageBase64(String coverImageBase64) { this.coverImageBase64 = coverImageBase64; }

    public String getFormattedDate() {
        if (suggestionDate == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(suggestionDate);
    }
}