package com.example.bibliocloud.services;

import com.example.bibliocloud.models.User;
import com.example.bibliocloud.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthService {
    private final FirebaseAuth auth;
    private final UserRepository userRepository;

    public AuthService() {
        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
    }

    // Registrar usuario
    public void registerUser(String email, String password, String name, OnAuthCompleteListener listener) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        User user = new User(firebaseUser.getUid(), name, email, "user");
                        userRepository.createUser(user, new UserRepository.OnCompleteListener() {
                            @Override
                            public void onSuccess(String id) {
                                listener.onSuccess(user);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                listener.onFailure(e);
                            }
                        });
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Iniciar sesión
    public void loginUser(String email, String password, OnAuthCompleteListener listener) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        userRepository.getUserById(firebaseUser.getUid(), new UserRepository.OnUserLoadedListener() {
                            @Override
                            public void onUserLoaded(User user) {
                                listener.onSuccess(user);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                listener.onFailure(e);
                            }
                        });
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Cerrar sesión
    public void logout() {
        auth.signOut();
    }

    // Obtener usuario actual
    public FirebaseUser getCurrentFirebaseUser() {
        return auth.getCurrentUser();
    }

    // Verificar si hay usuario autenticado
    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    // Restablecer contraseña
    public void resetPassword(String email, OnPasswordResetListener listener) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    // Interfaces
    public interface OnAuthCompleteListener {
        void onSuccess(User user);
        void onFailure(Exception e);
    }

    public interface OnPasswordResetListener {
        void onSuccess();
        void onFailure(Exception e);
    }
}