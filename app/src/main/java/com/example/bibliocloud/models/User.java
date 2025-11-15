package com.example.bibliocloud.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import java.util.ArrayList;
import java.util.List;

public class User {
    @DocumentId
    private String id;

    @PropertyName("nombre")
    private String name;

    @PropertyName("correo")
    private String email;

    @PropertyName("rol")
    private String userType;

    @PropertyName("telefono")
    private String phone;

    @PropertyName("direccion")
    private String department;

    @PropertyName("id_sucursal")
    private String branchId;

    @PropertyName("nombre_sucursal")
    private String branchName;

    @PropertyName("libros_prestados")
    private int booksBorrowed;

    @PropertyName("sugerencias_realizadas")
    private int suggestionsMade;

    @PropertyName("categorias_favoritas")
    private List<String> favoriteCategories;

    @PropertyName("notificaciones_activas")
    private boolean notificationsEnabled;

    @PropertyName("token_fcm")
    private String fcmToken;

    @PropertyName("fecha_creacion")
    private long createdAt;

    // Constantes de roles EN ESPAÑOL
    public static final String ROLE_ADMIN = "administrador";
    public static final String ROLE_CASHIER = "cajero";
    public static final String ROLE_USER = "usuario";

    public User() {
        this.favoriteCategories = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.userType = ROLE_USER;
    }

    public User(String id, String name, String email, String userType) {
        this();
        this.id = id;
        this.name = name;
        this.email = email;
        this.userType = userType;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("nombre")
    public String getName() { return name; }
    @PropertyName("nombre")
    public void setName(String name) { this.name = name; }

    @PropertyName("correo")
    public String getEmail() { return email; }
    @PropertyName("correo")
    public void setEmail(String email) { this.email = email; }

    @PropertyName("rol")
    public String getUserType() { return userType; }
    @PropertyName("rol")
    public void setUserType(String userType) { this.userType = userType; }

    @PropertyName("telefono")
    public String getPhone() { return phone; }
    @PropertyName("telefono")
    public void setPhone(String phone) { this.phone = phone; }

    @PropertyName("direccion")
    public String getDepartment() { return department; }
    @PropertyName("direccion")
    public void setDepartment(String department) { this.department = department; }

    @PropertyName("id_sucursal")
    public String getBranchId() { return branchId; }
    @PropertyName("id_sucursal")
    public void setBranchId(String branchId) { this.branchId = branchId; }

    @PropertyName("nombre_sucursal")
    public String getBranchName() { return branchName; }
    @PropertyName("nombre_sucursal")
    public void setBranchName(String branchName) { this.branchName = branchName; }

    @PropertyName("libros_prestados")
    public int getBooksBorrowed() { return booksBorrowed; }
    @PropertyName("libros_prestados")
    public void setBooksBorrowed(int booksBorrowed) { this.booksBorrowed = booksBorrowed; }

    @PropertyName("sugerencias_realizadas")
    public int getSuggestionsMade() { return suggestionsMade; }
    @PropertyName("sugerencias_realizadas")
    public void setSuggestionsMade(int suggestionsMade) { this.suggestionsMade = suggestionsMade; }

    @PropertyName("categorias_favoritas")
    public List<String> getFavoriteCategories() { return favoriteCategories; }
    @PropertyName("categorias_favoritas")
    public void setFavoriteCategories(List<String> favoriteCategories) {
        this.favoriteCategories = favoriteCategories;
    }

    @PropertyName("notificaciones_activas")
    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    @PropertyName("notificaciones_activas")
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    @PropertyName("token_fcm")
    public String getFcmToken() { return fcmToken; }
    @PropertyName("token_fcm")
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    @PropertyName("fecha_creacion")
    public long getCreatedAt() { return createdAt; }
    @PropertyName("fecha_creacion")
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // Métodos de utilidad
    public void incrementBooksBorrowed() { this.booksBorrowed++; }
    public void incrementSuggestionsMade() { this.suggestionsMade++; }

    public String getFormattedUserType() {
        switch (userType) {
            case ROLE_ADMIN:
                return "Administrador";
            case ROLE_CASHIER:
                return "Cajero";
            case ROLE_USER:
                return "Usuario";
            default:
                return "Usuario";
        }
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(userType);
    }

    public boolean isCashier() {
        return ROLE_CASHIER.equals(userType);
    }

    public boolean isUser() {
        return ROLE_USER.equals(userType);
    }

    public boolean canManageOrders() {
        return isAdmin() || isCashier();
    }

    public boolean canAccessInventory() {
        return isAdmin() || isCashier();
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