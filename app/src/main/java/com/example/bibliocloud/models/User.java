package com.example.bibliocloud.models;

import com.google.firebase.firestore.DocumentId;
import java.util.ArrayList;
import java.util.List;

public class User {
    @DocumentId
    private String id;
    private String name;
    private String email;
    private String userType;
    private String phone;
    private String department;
    private int booksBorrowed;
    private int suggestionsMade;
    private List<String> favoriteCategories;
    private boolean notificationsEnabled;
    private String fcmToken; // Para notificaciones push
    private long createdAt;

    public User() {
        this.favoriteCategories = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public User(String id, String name, String email, String userType) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.userType = userType;
        this.phone = "";
        this.department = "";
        this.booksBorrowed = 0;
        this.suggestionsMade = 0;
        this.favoriteCategories = new ArrayList<>();
        this.notificationsEnabled = true;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters y Setters (todos los existentes más)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getBooksBorrowed() { return booksBorrowed; }
    public void setBooksBorrowed(int booksBorrowed) { this.booksBorrowed = booksBorrowed; }

    public int getSuggestionsMade() { return suggestionsMade; }
    public void setSuggestionsMade(int suggestionsMade) { this.suggestionsMade = suggestionsMade; }

    public List<String> getFavoriteCategories() { return favoriteCategories; }
    public void setFavoriteCategories(List<String> favoriteCategories) { this.favoriteCategories = favoriteCategories; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // Métodos utilitarios
    public void incrementBooksBorrowed() { this.booksBorrowed++; }
    public void incrementSuggestionsMade() { this.suggestionsMade++; }

    public String getFormattedUserType() {
        return userType.equals("admin") ? "Administrador" : "Usuario";
    }

    public void addFavoriteCategory(String category) {
        if (!favoriteCategories.contains(category)) {
            favoriteCategories.add(category);
        }
    }

    public void removeFavoriteCategory(String category) {
        favoriteCategories.remove(category);
    }
}