package com.example.bibliocloud.repositories;

import com.example.bibliocloud.models.Suggestion;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class SuggestionRepository {
    private final FirebaseFirestore db;
    private final CollectionReference suggestionsRef;

    public SuggestionRepository() {
        db = FirebaseFirestore.getInstance();
        suggestionsRef = db.collection("sugerencias"); // 🔄 Cambiado a "sugerencias" para coincidir con tu Activity
    }

    // Crear sugerencia
    public void addSuggestion(Suggestion suggestion, OnCompleteListener listener) {
        String suggestionId = suggestionsRef.document().getId();
        suggestion.setId(suggestionId);

        suggestionsRef.document(suggestionId)
                .set(suggestion)
                .addOnSuccessListener(aVoid -> listener.onSuccess(suggestionId))
                .addOnFailureListener(listener::onFailure);
    }

    // Obtener todas las sugerencias
    public void getAllSuggestions(OnSuggestionsLoadedListener listener) {
        suggestionsRef.orderBy("suggestionDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onFailure(error);
                        return;
                    }
                    if (value != null) {
                        listener.onSuggestionsLoaded(value.toObjects(Suggestion.class));
                    }
                });
    }

    // Obtener sugerencias por usuario
    public void getSuggestionsByUser(String userId, OnSuggestionsLoadedListener listener) {
        suggestionsRef.whereEqualTo("userId", userId)
                .orderBy("suggestionDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot ->
                        listener.onSuggestionsLoaded(querySnapshot.toObjects(Suggestion.class)))
                .addOnFailureListener(listener::onFailure);
    }

    // Obtener sugerencias por email (más común en tu caso)
    public void getSuggestionsByEmail(String userEmail, OnSuggestionsLoadedListener listener) {
        suggestionsRef.whereEqualTo("userEmail", userEmail)
                .orderBy("suggestionDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot ->
                        listener.onSuggestionsLoaded(querySnapshot.toObjects(Suggestion.class)))
                .addOnFailureListener(listener::onFailure);
    }

    // Obtener sugerencias por estado
    public void getSuggestionsByStatus(String status, OnSuggestionsLoadedListener listener) {
        suggestionsRef.whereEqualTo("status", status)
                .orderBy("suggestionDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot ->
                        listener.onSuggestionsLoaded(querySnapshot.toObjects(Suggestion.class)))
                .addOnFailureListener(listener::onFailure);
    }

    // Actualizar estado de sugerencia
    public void updateSuggestionStatus(String suggestionId, String status, OnCompleteListener listener) {
        suggestionsRef.document(suggestionId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> listener.onSuccess(suggestionId))
                .addOnFailureListener(listener::onFailure);
    }

    // Eliminar sugerencia
    public void deleteSuggestion(String suggestionId, OnCompleteListener listener) {
        suggestionsRef.document(suggestionId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess(suggestionId))
                .addOnFailureListener(listener::onFailure);
    }

    // ✅ Interfaces definidas correctamente dentro de la clase
    public interface OnCompleteListener {
        void onSuccess(String id);
        void onFailure(Exception e);
    }

    public interface OnSuggestionsLoadedListener {
        void onSuggestionsLoaded(java.util.List<Suggestion> suggestions);
        void onFailure(Exception e);
    }
}