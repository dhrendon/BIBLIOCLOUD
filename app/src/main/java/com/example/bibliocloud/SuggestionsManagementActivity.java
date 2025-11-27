package com.example.bibliocloud;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
        // ✅ NO cargar aquí, onResume() lo hará
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
        // ✅ CRÍTICO: Limpiar TODO antes de cargar
        layoutListaSugerencias.removeAllViews();
        listaSugerencias.clear();

        sugerenciasRef
                .orderBy("suggestionDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        TextView tvEmpty = new TextView(this);
                        tvEmpty.setText("No hay sugerencias registradas");
                        tvEmpty.setTextColor(getResources().getColor(R.color.colorTextSecondary));
                        tvEmpty.setTextSize(16);
                        tvEmpty.setPadding(0, 32, 0, 0);
                        layoutListaSugerencias.addView(tvEmpty);
                        return;
                    }

                    // Agregar documentos a la lista
                    listaSugerencias.addAll(query.getDocuments());

                    // Mostrar estadísticas
                    mostrarEstadisticas();

                    // Crear tarjetas
                    for (DocumentSnapshot doc : listaSugerencias) {
                        String id = doc.getId();
                        String titulo = doc.getString("title");
                        String autor = doc.getString("author");
                        String categoria = doc.getString("category");
                        String comentarios = doc.getString("comments");
                        String edicion = doc.getString("edition");
                        String isbn = doc.getString("isbn");
                        String year = doc.getString("year");
                        String coverImageBase64 = doc.getString("coverImageBase64");
                        String estado = doc.getString("status");
                        String usuario = doc.getString("userEmail");
                        Boolean agregadaCatalogo = doc.getBoolean("addedToCatalog");

                        CardView card = crearCardSugerencia(id, titulo, autor, categoria, comentarios,
                                edicion, isbn, year, coverImageBase64, estado, usuario, agregadaCatalogo);
                        layoutListaSugerencias.addView(card);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

    private CardView crearCardSugerencia(String id, String titulo, String autor, String categoria,
                                         String comentarios, String edicion, String isbn, String year,
                                         String coverImageBase64, String estado, String usuario,
                                         Boolean agregadaCatalogo) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, 16);
        cardView.setLayoutParams(layoutParams);
        cardView.setCardElevation(4);
        cardView.setRadius(8);
        cardView.setCardBackgroundColor(getResources().getColor(R.color.light_brown));

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.HORIZONTAL);
        mainLayout.setPadding(16, 16, 16, 16);

        // 📸 Imagen desde Base64
        if (coverImageBase64 != null && !coverImageBase64.isEmpty()) {
            ImageView ivPortada = new ImageView(this);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(120, 160);
            imgParams.setMargins(0, 0, 16, 0);
            ivPortada.setLayoutParams(imgParams);
            ivPortada.setScaleType(ImageView.ScaleType.CENTER_CROP);

            try {
                Bitmap bitmap = base64ToBitmap(coverImageBase64);
                if (bitmap != null) {
                    ivPortada.setImageBitmap(bitmap);
                } else {
                    ivPortada.setImageResource(R.drawable.ic_book_placeholder);
                }
            } catch (Exception e) {
                ivPortada.setImageResource(R.drawable.ic_book_placeholder);
            }

            mainLayout.addView(ivPortada);
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        layout.setLayoutParams(contentParams);

        TextView tvTitulo = new TextView(this);
        tvTitulo.setText(titulo != null && !titulo.isEmpty() ? titulo : "Sin título");
        tvTitulo.setTextSize(18);
        tvTitulo.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvTitulo);

        TextView tvAutor = new TextView(this);
        tvAutor.setText("✍️ " + (autor != null && !autor.isEmpty() ? autor : "Sin autor"));
        layout.addView(tvAutor);

        TextView tvCategoria = new TextView(this);
        tvCategoria.setText("📚 " + (categoria != null ? categoria : "Sin categoría"));
        layout.addView(tvCategoria);

        if (edicion != null && !edicion.isEmpty()) {
            TextView tvEdicion = new TextView(this);
            tvEdicion.setText("📖 Edición: " + edicion);
            tvEdicion.setTextSize(13);
            tvEdicion.setTextColor(getResources().getColor(R.color.colorTextSecondary));
            layout.addView(tvEdicion);
        }

        if (isbn != null && !isbn.isEmpty()) {
            TextView tvIsbn = new TextView(this);
            tvIsbn.setText("🔢 ISBN: " + isbn);
            tvIsbn.setTextSize(13);
            tvIsbn.setTextColor(getResources().getColor(R.color.colorTextSecondary));
            layout.addView(tvIsbn);
        }

        if (year != null && !year.isEmpty()) {
            TextView tvYear = new TextView(this);
            tvYear.setText("📅 Año: " + year);
            tvYear.setTextSize(13);
            tvYear.setTextColor(getResources().getColor(R.color.colorTextSecondary));
            layout.addView(tvYear);
        }

        if (comentarios != null && !comentarios.isEmpty()) {
            TextView tvComentarios = new TextView(this);
            tvComentarios.setText("💬 " + comentarios);
            tvComentarios.setTextSize(13);
            layout.addView(tvComentarios);
        }

        TextView tvInfo = new TextView(this);
        tvInfo.setText("👤 " + (usuario != null ? usuario : "Desconocido"));
        tvInfo.setTextSize(12);
        tvInfo.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        layout.addView(tvInfo);

        TextView tvEstado = new TextView(this);
        tvEstado.setText("📄 Estado: " + estado);
        tvEstado.setTextSize(13);
        switch (estado) {
            case "Pendiente": tvEstado.setTextColor(getResources().getColor(R.color.orange)); break;
            case "Aprobada": tvEstado.setTextColor(getResources().getColor(R.color.green)); break;
            case "Rechazada": tvEstado.setTextColor(getResources().getColor(R.color.red)); break;
        }
        layout.addView(tvEstado);

        // ✅ Mostrar mensaje si ya fue agregada al catálogo
        if (agregadaCatalogo != null && agregadaCatalogo) {
            TextView tvAgregada = new TextView(this);
            tvAgregada.setText("📖 ✅ Agregada al catálogo");
            tvAgregada.setTextSize(14);
            tvAgregada.setTypeface(null, android.graphics.Typeface.BOLD);
            tvAgregada.setTextColor(getResources().getColor(R.color.colorPrimary));
            tvAgregada.setPadding(0, 12, 0, 0);
            layout.addView(tvAgregada);
        } else {
            // Botones según estado (solo si NO ha sido agregada)
            if ("Pendiente".equals(estado)) {
                LinearLayout botones = new LinearLayout(this);
                botones.setOrientation(LinearLayout.HORIZONTAL);
                botones.setPadding(0, 12, 0, 0);

                Button btnAprobar = new Button(this);
                btnAprobar.setText("Aprobar");
                btnAprobar.setBackgroundColor(getResources().getColor(R.color.green));
                btnAprobar.setTextColor(getResources().getColor(R.color.white));
                btnAprobar.setOnClickListener(v -> {
                    if (isbn != null && !isbn.isEmpty()) {
                        validarISBNDuplicado(isbn, id, titulo, autor, categoria,
                                edicion, year, coverImageBase64);
                    } else {
                        confirmarAprobacion(id);
                    }
                });

                Button btnRechazar = new Button(this);
                btnRechazar.setText("Rechazar");
                btnRechazar.setBackgroundColor(getResources().getColor(R.color.red));
                btnRechazar.setTextColor(getResources().getColor(R.color.white));
                btnRechazar.setOnClickListener(v -> confirmarRechazo(id));

                botones.addView(btnAprobar);
                botones.addView(btnRechazar);
                layout.addView(botones);

            } else if ("Aprobada".equals(estado)) {
                Button btnAgregar = new Button(this);
                btnAgregar.setText("Agregar al Catálogo");
                btnAgregar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                btnAgregar.setTextColor(getResources().getColor(R.color.white));
                btnAgregar.setOnClickListener(v -> {
                    if (validarDatosParaCatalogo(titulo, autor, categoria)) {
                        agregarLibroDesdeSugerencia(id, titulo, autor, categoria,
                                edicion, isbn, year, coverImageBase64);
                    }
                });
                layout.addView(btnAgregar);
            }
        }

        mainLayout.addView(layout);
        cardView.addView(mainLayout);
        return cardView;
    }

    // 📸 Convertir Base64 a Bitmap
    private Bitmap base64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    private void validarISBNDuplicado(String isbn, String sugerenciaId, String titulo,
                                      String autor, String categoria, String edicion,
                                      String year, String coverImageBase64) {
        String isbnLimpio = isbn.replaceAll("[\\s-]", "");

        db.collection("libros")
                .whereEqualTo("isbn", isbnLimpio)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        new AlertDialog.Builder(this)
                                .setTitle("⚠️ ISBN Duplicado")
                                .setMessage("Ya existe un libro con este ISBN en el catálogo:\n\n" +
                                        "ISBN: " + isbn + "\n\n" +
                                        "¿Deseas aprobar la sugerencia de todas formas?")
                                .setPositiveButton("Sí, aprobar", (dialog, which) -> {
                                    confirmarAprobacion(sugerenciaId);
                                })
                                .setNegativeButton("Cancelar", null)
                                .show();
                    } else {
                        confirmarAprobacion(sugerenciaId);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al validar ISBN: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmarAprobacion(String id) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar aprobación")
                .setMessage("¿Marcar esta sugerencia como aprobada?")
                .setPositiveButton("✅ Aprobar", (dialog, which) -> {
                    actualizarEstado(id, "Aprobada");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarRechazo(String id) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar rechazo")
                .setMessage("¿Marcar esta sugerencia como rechazada?")
                .setPositiveButton("❌ Rechazar", (dialog, which) -> {
                    actualizarEstado(id, "Rechazada");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private boolean validarDatosParaCatalogo(String titulo, String autor, String categoria) {
        if (titulo == null || titulo.trim().isEmpty()) {
            Toast.makeText(this, "⚠️ Error: Título no válido", Toast.LENGTH_LONG).show();
            return false;
        }

        if (autor == null || autor.trim().isEmpty()) {
            Toast.makeText(this, "⚠️ Error: Autor no válido", Toast.LENGTH_LONG).show();
            return false;
        }

        if (categoria == null || categoria.trim().isEmpty()) {
            Toast.makeText(this, "⚠️ Error: Categoría no válida", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
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

    private void agregarLibroDesdeSugerencia(String sugerenciaId, String titulo, String autor,
                                             String categoria, String edicion, String isbn,
                                             String year, String coverImageBase64) {
        // Marcar en Firestore que ya fue agregada al catálogo
        Map<String, Object> update = new HashMap<>();
        update.put("addedToCatalog", true);

        sugerenciasRef.document(sugerenciaId).update(update)
                .addOnSuccessListener(aVoid -> {
                    // Navegar a BookManagementActivity con los datos
                    Intent intent = new Intent(this, BookManagementActivity.class);
                    intent.putExtra("titulo_sugerencia", titulo);
                    intent.putExtra("autor_sugerencia", autor);
                    intent.putExtra("categoria_sugerencia", categoria);
                    intent.putExtra("edicion_sugerencia", edicion);
                    intent.putExtra("isbn_sugerencia", isbn);
                    intent.putExtra("year_sugerencia", year);
                    intent.putExtra("cover_base64_sugerencia", coverImageBase64);
                    startActivity(intent);

                    Toast.makeText(this, "✅ Sugerencia marcada como agregada", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al marcar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Siempre recargar para actualizar el estado
        cargarListaSugerencias();
    }
}