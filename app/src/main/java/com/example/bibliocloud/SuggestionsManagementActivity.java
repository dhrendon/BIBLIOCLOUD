package com.example.bibliocloud;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SuggestionsManagementActivity extends AppCompatActivity {

    private MaterialButton btnVolver;
    private LinearLayout layoutListaSugerencias;
    private FirebaseFirestore db;
    private CollectionReference sugerenciasRef;

    private ArrayList<DocumentSnapshot> listaSugerencias = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestions_management);

        initializeViews();
        setupFirebase();
        setupButtonListeners();
        cargarListaSugerencias();
    }

    private void initializeViews() {
        btnVolver = findViewById(R.id.btnVolver);
        layoutListaSugerencias = findViewById(R.id.layoutListaSugerencias);
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        sugerenciasRef = db.collection("sugerencias");
    }

    private void setupButtonListeners() {
        btnVolver.setOnClickListener(v -> finish());
    }

    private void cargarListaSugerencias() {
        layoutListaSugerencias.removeAllViews();

        sugerenciasRef.get().addOnSuccessListener(query -> {
            listaSugerencias.clear();
            listaSugerencias.addAll(query.getDocuments());

            if (listaSugerencias.isEmpty()) {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("No hay sugerencias registradas");
                tvEmpty.setTextColor(getResources().getColor(R.color.colorTextSecondary));
                tvEmpty.setTextSize(16);
                tvEmpty.setPadding(0, 32, 0, 0);
                layoutListaSugerencias.addView(tvEmpty);
                return;
            }

            mostrarEstadisticas();

            for (DocumentSnapshot doc : listaSugerencias) {
                String id = doc.getId();
                String titulo = doc.getString("title");
                String autor = doc.getString("author");
                String categoria = doc.getString("category");
                String comentarios = doc.getString("comments");
                String estado = doc.getString("status");
                String fecha = doc.getString("fecha");
                String usuario = doc.getString("userName");

                CardView card = crearCardSugerencia(id, titulo, autor, categoria, comentarios, estado, fecha, usuario);
                layoutListaSugerencias.addView(card);
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Error al cargar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void mostrarEstadisticas() {
        int pendientes = 0, aprobadas = 0, rechazadas = 0;

        for (DocumentSnapshot doc : listaSugerencias) {
            String estado = doc.getString("status");
            if (estado == null) continue;

            switch (estado) {
                case "Pendiente": pendientes++; break;
                case "Aprobada": aprobadas++; break;
                case "Rechazada": rechazadas++; break;
            }
        }

        LinearLayout layoutEstadisticas = new LinearLayout(this);
        layoutEstadisticas.setOrientation(LinearLayout.HORIZONTAL);
        layoutEstadisticas.setPadding(0, 0, 0, 16);

        layoutEstadisticas.addView(crearTarjetaEstadistica("⏳", "Pendientes", String.valueOf(pendientes), R.color.orange));
        layoutEstadisticas.addView(crearTarjetaEstadistica("✅", "Aprobadas", String.valueOf(aprobadas), R.color.green));
        layoutEstadisticas.addView(crearTarjetaEstadistica("❌", "Rechazadas", String.valueOf(rechazadas), R.color.red));

        layoutListaSugerencias.addView(layoutEstadisticas);
    }

    private LinearLayout crearTarjetaEstadistica(String emoji, String texto, String numero, int colorRes) {
        LinearLayout tarjeta = new LinearLayout(this);
        tarjeta.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(4, 0, 4, 0);
        tarjeta.setLayoutParams(params);
        tarjeta.setPadding(16, 16, 16, 16);
        tarjeta.setBackgroundColor(getResources().getColor(R.color.light_brown));

        TextView tvEmoji = new TextView(this);
        tvEmoji.setText(emoji);
        tvEmoji.setTextSize(20);
        tvEmoji.setGravity(android.view.Gravity.CENTER);
        tarjeta.addView(tvEmoji);

        TextView tvNumero = new TextView(this);
        tvNumero.setText(numero);
        tvNumero.setTextSize(18);
        tvNumero.setTypeface(null, android.graphics.Typeface.BOLD);
        tvNumero.setTextColor(getResources().getColor(colorRes));
        tvNumero.setGravity(android.view.Gravity.CENTER);
        tarjeta.addView(tvNumero);

        TextView tvTexto = new TextView(this);
        tvTexto.setText(texto);
        tvTexto.setTextSize(12);
        tvTexto.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        tvTexto.setGravity(android.view.Gravity.CENTER);
        tarjeta.addView(tvTexto);

        return tarjeta;
    }

    private CardView crearCardSugerencia(String id, String titulo, String autor, String categoria, String comentarios, String estado, String fecha, String usuario) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, 16);
        cardView.setLayoutParams(layoutParams);
        cardView.setCardElevation(4);
        cardView.setRadius(8);
        cardView.setCardBackgroundColor(getResources().getColor(R.color.light_brown));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        TextView tvTitulo = new TextView(this);
        tvTitulo.setText(titulo);
        tvTitulo.setTextSize(18);
        tvTitulo.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvTitulo);

        TextView tvAutor = new TextView(this);
        tvAutor.setText("✍️ " + autor);
        layout.addView(tvAutor);

        TextView tvCategoria = new TextView(this);
        tvCategoria.setText("📚 " + categoria);
        layout.addView(tvCategoria);

        if (comentarios != null && !comentarios.isEmpty()) {
            TextView tvComentarios = new TextView(this);
            tvComentarios.setText("💬 " + comentarios);
            layout.addView(tvComentarios);
        }

        TextView tvInfo = new TextView(this);
        tvInfo.setText("👤 " + usuario + "   📅 " + (fecha != null ? fecha : "Sin fecha"));
        layout.addView(tvInfo);

        TextView tvEstado = new TextView(this);
        tvEstado.setText("🔄 Estado: " + estado);
        tvEstado.setTextSize(13);
        switch (estado) {
            case "Pendiente": tvEstado.setTextColor(getResources().getColor(R.color.orange)); break;
            case "Aprobada": tvEstado.setTextColor(getResources().getColor(R.color.green)); break;
            case "Rechazada": tvEstado.setTextColor(getResources().getColor(R.color.red)); break;
        }
        layout.addView(tvEstado);

        // Botones de acción
        if ("Pendiente".equals(estado)) {
            LinearLayout botones = new LinearLayout(this);
            botones.setOrientation(LinearLayout.HORIZONTAL);
            botones.setPadding(0, 12, 0, 0);

            Button btnAprobar = new Button(this);
            btnAprobar.setText("Aprobar");
            btnAprobar.setBackgroundColor(getResources().getColor(R.color.green));
            btnAprobar.setTextColor(getResources().getColor(R.color.white));
            btnAprobar.setOnClickListener(v -> actualizarEstado(id, "Aprobada"));

            Button btnRechazar = new Button(this);
            btnRechazar.setText("Rechazar");
            btnRechazar.setBackgroundColor(getResources().getColor(R.color.red));
            btnRechazar.setTextColor(getResources().getColor(R.color.white));
            btnRechazar.setOnClickListener(v -> actualizarEstado(id, "Rechazada"));

            botones.addView(btnAprobar);
            botones.addView(btnRechazar);
            layout.addView(botones);
        } else if ("Aprobada".equals(estado)) {
            Button btnAgregar = new Button(this);
            btnAgregar.setText("Agregar al Catálogo");
            btnAgregar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            btnAgregar.setTextColor(getResources().getColor(R.color.white));
            btnAgregar.setOnClickListener(v -> agregarLibroDesdeSugerencia(titulo, autor, categoria));
            layout.addView(btnAgregar);
        }

        cardView.addView(layout);
        return cardView;
    }

    private void actualizarEstado(String id, String nuevoEstado) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", nuevoEstado);

        sugerenciasRef.document(id).update(update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Sugerencia " + nuevoEstado.toLowerCase(), Toast.LENGTH_SHORT).show();
                    cargarListaSugerencias();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void agregarLibroDesdeSugerencia(String titulo, String autor, String categoria) {
        Intent intent = new Intent(this, BookManagementActivity.class);
        intent.putExtra("titulo_sugerencia", titulo);
        intent.putExtra("autor_sugerencia", autor);
        intent.putExtra("categoria_sugerencia", categoria);
        startActivity(intent);
    }
}
