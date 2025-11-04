package com.example.bibliocloud.repositories;

import com.example.bibliocloud.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserRepository {
    private final FirebaseFirestore db;
    private final CollectionReference usersRef;
    private final FirebaseAuth auth;

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");
        auth = FirebaseAuth.getInstance();
    }

    // Crear usuario
    public void createUser(User user, OnCompleteListener listener) {
        usersRef.document(user.getId())
                .set(user)
                .addOnSuccessListener(aVoid -> listener.onSuccess(user.getId()))
                .addOnFailureListener(listener::onFailure);
    }

    // Obtener usuario por ID
    public void getUserById(String userId, OnUserLoadedListener listener) {
        usersRef.document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        listener.onUserLoaded(user);
                    } else {
                        listener.onFailure(new Exception("Usuario no encontrado"));
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Obtener usuario actual
    public void getCurrentUser(OnUserLoadedListener listener) {
        String userId = auth.getCurrentUser().getUid();
        getUserById(userId, listener);
    }

    // Actualizar usuario
    public void updateUser(User user, OnCompleteListener listener) {
        usersRef.document(user.getId())
                .set(user)
                .addOnSuccessListener(aVoid -> listener.onSuccess(user.getId()))
                .addOnFailureListener(listener::onFailure);
    }

    // Actualizar FCM Token
    public void updateFcmToken(String userId, String token, OnCompleteListener listener) {
        usersRef.document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> listener.onSuccess(userId))
                .addOnFailureListener(listener::onFailure);
    }

    // Incrementar libros prestados
    public void incrementBooksBorrowed(String userId, OnCompleteListener listener) {
        usersRef.document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        user.incrementBooksBorrowed();
                        updateUser(user, listener);
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Incrementar sugerencias realizadas
    public void incrementSuggestionsMade(String userId, OnCompleteListener listener) {
        usersRef.document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        user.incrementSuggestionsMade();
                        updateUser(user, listener);
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Agregar categoría favorita
    public void addFavoriteCategory(String userId, String category, OnCompleteListener listener) {
        usersRef.document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        user.addFavoriteCategory(category);
                        updateUser(user, listener);
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Remover categoría favorita
    public void removeFavoriteCategory(String userId, String category, OnCompleteListener listener) {
        usersRef.document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        user.removeFavoriteCategory(category);
                        updateUser(user, listener);
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Interfaces
    public interface OnCompleteListener {
        void onSuccess(String id);
        void onFailure(Exception e);
    }

    public interface OnUserLoadedListener {
        void onUserLoaded(User user);
        void onFailure(Exception e);
    }
}