package com.example.bibliocloud.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.example.bibliocloud.R;
import com.example.bibliocloud.models.Book;

public class RatingDialog extends Dialog {

    private Book book;
    private Context context;
    private OnRatingSubmittedListener listener;

    public interface OnRatingSubmittedListener {
        void onRatingSubmitted(Book book, float rating);
    }

    private RatingBar ratingBar;
    private TextView tvBookTitle;
    private Button btnSubmit, btnCancel;

    public RatingDialog(@NonNull Context context, Book book, OnRatingSubmittedListener listener) {
        super(context);
        this.context = context;
        this.book = book;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_rating);

        initializeViews();
        setupListeners();
        loadPreviousRating();
    }

    private void initializeViews() {
        ratingBar = findViewById(R.id.ratingBar);
        tvBookTitle = findViewById(R.id.tvBookTitle);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnCancel = findViewById(R.id.btnCancel);

        if (tvBookTitle != null) {
            tvBookTitle.setText("Valorar: " + book.getTitle());
        }

        // Configurar rating bar
        if (ratingBar != null) {
            ratingBar.setStepSize(0.5f);
            ratingBar.setMax(5);
        }
    }

    private void setupListeners() {
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ratingBar != null) {
                        float rating = ratingBar.getRating();
                        if (rating > 0) {
                            submitRating(rating);
                        } else {
                            Toast.makeText(context, "Por favor selecciona una valoración", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }

        // Cambiar texto del botón según la valoración
        if (ratingBar != null && btnSubmit != null) {
            ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
                @Override
                public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                    updateSubmitButtonText(rating);
                }
            });
        }
    }

    private void updateSubmitButtonText(float rating) {
        if (btnSubmit != null) {
            String text = "Enviar Valoración";
            if (rating > 0) {
                text = String.format("Enviar (%.1f ⭐)", rating);
            }
            btnSubmit.setText(text);
        }
    }

    private void loadPreviousRating() {
        // Cargar valoración previa del usuario si existe
        SharedPreferences prefs = context.getSharedPreferences("Ratings", Context.MODE_PRIVATE);
        float userRating = prefs.getFloat(book.getId() + "_user", 0);
        if (userRating > 0 && ratingBar != null) {
            ratingBar.setRating(userRating);
            updateSubmitButtonText(userRating);
            if (btnSubmit != null) {
                btnSubmit.setText("Actualizar Valoración");
            }
        }
    }

    private void submitRating(float rating) {
        // Guardar valoración del usuario
        SharedPreferences prefs = context.getSharedPreferences("Ratings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(book.getId() + "_user", rating);
        editor.apply();

        // Actualizar valoración promedio del libro
        updateBookRating(rating);

        if (listener != null) {
            listener.onRatingSubmitted(book, rating);
        }

        dismiss();

        String message = String.format("¡Gracias! Valoraste \"%s\" con %.1f ⭐",
                book.getTitle(), rating);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    private void updateBookRating(float newRating) {
        SharedPreferences prefs = context.getSharedPreferences("BookRatings", Context.MODE_PRIVATE);

        // Obtener valoración actual
        float currentRating = prefs.getFloat(book.getId() + "_avg", 0);
        int ratingCount = prefs.getInt(book.getId() + "_count", 0);

        // Calcular nueva valoración promedio
        float totalRating = currentRating * ratingCount;
        ratingCount++;
        float newAverage = (totalRating + newRating) / ratingCount;

        // Guardar nueva valoración
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(book.getId() + "_avg", newAverage);
        editor.putInt(book.getId() + "_count", ratingCount);
        editor.apply();

        // Actualizar el objeto book
        book.setRating(newAverage);
        book.setRatingCount(ratingCount);
    }
}