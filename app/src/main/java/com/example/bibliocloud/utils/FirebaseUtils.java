package com.example.bibliocloud.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class FirebaseUtils {

    private static final String TAG = "FirebaseUtils";

    // Configurar Firestore para modo offline
    public static void enableOfflinePersistence() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        db.setFirestoreSettings(settings);
        Log.d(TAG, "Persistencia offline habilitada");
    }

    // Verificar conexión a internet
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    // Manejar errores de Firebase
    public static String getFirebaseErrorMessage(Exception exception) {
        String errorMessage = exception.getMessage();

        if (errorMessage == null) {
            return "Error desconocido";
        }

        // Personalizar mensajes de error comunes
        if (errorMessage.contains("PERMISSION_DENIED")) {
            return "No tienes permisos para realizar esta acción";
        } else if (errorMessage.contains("NOT_FOUND")) {
            return "El recurso solicitado no existe";
        } else if (errorMessage.contains("ALREADY_EXISTS")) {
            return "Este recurso ya existe";
        } else if (errorMessage.contains("UNAVAILABLE")) {
            return "Servicio no disponible. Verifica tu conexión a internet";
        } else if (errorMessage.contains("UNAUTHENTICATED")) {
            return "Debes iniciar sesión para continuar";
        } else if (errorMessage.contains("INVALID_ARGUMENT")) {
            return "Datos inválidos proporcionados";
        } else if (errorMessage.contains("DEADLINE_EXCEEDED")) {
            return "La operación tardó demasiado tiempo";
        } else if (errorMessage.contains("email-already-in-use")) {
            return "Este correo electrónico ya está registrado";
        } else if (errorMessage.contains("invalid-email")) {
            return "Correo electrónico inválido";
        } else if (errorMessage.contains("weak-password")) {
            return "La contraseña debe tener al menos 6 caracteres";
        } else if (errorMessage.contains("user-not-found")) {
            return "Usuario no encontrado";
        } else if (errorMessage.contains("wrong-password")) {
            return "Contraseña incorrecta";
        }

        return errorMessage;
    }

    // Validar campos de entrada
    public static boolean isValidEmail(String email) {
        return email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty() && name.length() >= 2;
    }
}