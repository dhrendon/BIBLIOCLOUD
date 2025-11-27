package com.example.bibliocloud;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LoanManagementActivity extends AppCompatActivity {

    private static final String TAG = "LoanManagement";

    private Spinner spinnerUsuario, spinnerLibro;
    private EditText etFechaPrestamo, etFechaDevolucion;
    private MaterialButton btnRealizarPrestamo, btnVolver, btnSeleccionarFechaPrestamo, btnSeleccionarFechaDevolucion;
    private LinearLayout layoutListaPrestamos;
    private TextView tvTotalPrestamos;

    private Calendar calendarPrestamo, calendarDevolucion;
    private SimpleDateFormat dateFormatter;

    private FirebaseFirestore db;
    private CollectionReference prestamosRef, usuariosRef, librosRef;

    private ArrayList<String> usuariosList = new ArrayList<>();
    private ArrayList<String> librosDisponiblesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_management);

        initializeViews();
        setupToolbar();
        setupFirebase();
        loadUsuarios();
        loadLibrosDisponibles();
        setupDatePickers();
        setupButtonListeners();
        loadPrestamos();
    }

    private void initializeViews() {
        spinnerUsuario = findViewById(R.id.spinnerUsuario);
        spinnerLibro = findViewById(R.id.spinnerLibro);
        etFechaPrestamo = findViewById(R.id.etFechaPrestamo);
        etFechaDevolucion = findViewById(R.id.etFechaDevolucion);
        btnSeleccionarFechaPrestamo = findViewById(R.id.btnSeleccionarFechaPrestamo);
        btnSeleccionarFechaDevolucion = findViewById(R.id.btnSeleccionarFechaDevolucion);
        btnRealizarPrestamo = findViewById(R.id.btnRealizarPrestamo);
        btnVolver = findViewById(R.id.btnVolver);
        layoutListaPrestamos = findViewById(R.id.layoutListaPrestamos);
        tvTotalPrestamos = findViewById(R.id.tvTotalPrestamos);

        calendarPrestamo = Calendar.getInstance();
        calendarDevolucion = Calendar.getInstance();
        calendarDevolucion.add(Calendar.DAY_OF_YEAR, 14);
        dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        if (etFechaPrestamo != null) {
            etFechaPrestamo.setText(dateFormatter.format(calendarPrestamo.getTime()));
        }
        if (etFechaDevolucion != null) {
            etFechaDevolucion.setText(dateFormatter.format(calendarDevolucion.getTime()));
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Gestión de Préstamos");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        prestamosRef = db.collection("prestamos");
        usuariosRef = db.collection("usuarios");
        librosRef = db.collection("libros");
    }

    private void loadUsuarios() {
        usuariosRef.whereEqualTo("rol", "usuario")
                .get()
                .addOnSuccessListener(query -> {
                    usuariosList.clear();
                    usuariosList.add("Seleccionar usuario");

                    for (DocumentSnapshot doc : query) {
                        String nombre = doc.getString("nombre");
                        if (nombre != null && !nombre.isEmpty()) {
                            usuariosList.add(nombre);
                        }
                    }

                    if (spinnerUsuario != null) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_spinner_dropdown_item, usuariosList);
                        spinnerUsuario.setAdapter(adapter);
                    }

                    Log.d(TAG, "✅ Usuarios cargados: " + usuariosList.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error cargando usuarios: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadLibrosDisponibles() {
        // ✅ Buscar con campos en inglés Y español para compatibilidad
        librosRef.get()
                .addOnSuccessListener(query -> {
                    librosDisponiblesList.clear();
                    librosDisponiblesList.add("Seleccionar libro");

                    for (DocumentSnapshot doc : query) {
                        // ✅ Leer estado (inglés o español)
                        String estado = doc.getString("status");
                        if (estado == null) estado = doc.getString("estado");

                        // Solo agregar si está disponible
                        if ("Disponible".equalsIgnoreCase(estado) || "Available".equalsIgnoreCase(estado)) {
                            // ✅ Leer título y autor (inglés o español)
                            String titulo = doc.getString("title");
                            if (titulo == null) titulo = doc.getString("titulo");

                            String autor = doc.getString("author");
                            if (autor == null) autor = doc.getString("autor");

                            if (titulo != null && !titulo.isEmpty() && autor != null && !autor.isEmpty()) {
                                librosDisponiblesList.add(titulo + " - " + autor);
                            }
                        }
                    }

                    if (spinnerLibro != null) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_spinner_dropdown_item, librosDisponiblesList);
                        spinnerLibro.setAdapter(adapter);
                    }

                    Log.d(TAG, "✅ Libros disponibles: " + (librosDisponiblesList.size() - 1));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error cargando libros: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar libros", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupDatePickers() {
        if (btnSeleccionarFechaPrestamo != null) {
            btnSeleccionarFechaPrestamo.setOnClickListener(v ->
                    showDatePickerDialog(etFechaPrestamo, calendarPrestamo));
        }

        if (btnSeleccionarFechaDevolucion != null) {
            btnSeleccionarFechaDevolucion.setOnClickListener(v ->
                    showDatePickerDialog(etFechaDevolucion, calendarDevolucion));
        }
    }

    private void showDatePickerDialog(final EditText editText, final Calendar calendar) {
        if (editText == null) return;

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    editText.setText(dateFormatter.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    private void setupButtonListeners() {
        if (btnRealizarPrestamo != null) {
            btnRealizarPrestamo.setOnClickListener(v -> realizarPrestamo());
        }
        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> finish());
        }
    }

    private void realizarPrestamo() {
        if (spinnerUsuario == null || spinnerLibro == null) {
            Toast.makeText(this, "Error: Elementos de UI no encontrados", Toast.LENGTH_SHORT).show();
            return;
        }

        String usuario = spinnerUsuario.getSelectedItem() != null ?
                spinnerUsuario.getSelectedItem().toString() : "";
        String libro = spinnerLibro.getSelectedItem() != null ?
                spinnerLibro.getSelectedItem().toString() : "";
        String fechaPrestamo = etFechaPrestamo != null ?
                etFechaPrestamo.getText().toString() : dateFormatter.format(calendarPrestamo.getTime());
        String fechaDevolucion = etFechaDevolucion != null ?
                etFechaDevolucion.getText().toString() : dateFormatter.format(calendarDevolucion.getTime());

        if (usuario.isEmpty() || usuario.equals("Seleccionar usuario") ||
                libro.isEmpty() || libro.equals("Seleccionar libro")) {
            Toast.makeText(this, "⚠️ Seleccione usuario y libro", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> prestamo = new HashMap<>();
        prestamo.put("usuario", usuario);
        prestamo.put("libro", libro);
        prestamo.put("fechaPrestamo", fechaPrestamo);
        prestamo.put("fechaDevolucion", fechaDevolucion);
        prestamo.put("estado", "Activo");
        prestamo.put("timestamp", System.currentTimeMillis());

        Log.d(TAG, "💾 Guardando préstamo: " + usuario + " - " + libro);

        prestamosRef.add(prestamo)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "✅ Préstamo registrado: " + docRef.getId());
                    Toast.makeText(this, "✅ Préstamo registrado correctamente", Toast.LENGTH_SHORT).show();
                    updateLibroEstado(libro, "Prestado");
                    loadPrestamos();

                    if (spinnerUsuario != null) spinnerUsuario.setSelection(0);
                    if (spinnerLibro != null) spinnerLibro.setSelection(0);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error registrando préstamo: " + e.getMessage());
                    Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateLibroEstado(String libro, String nuevoEstado) {
        String tituloLibro = libro.split(" - ")[0];

        // ✅ Buscar por ambos campos (title y titulo)
        librosRef.get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query) {
                        String titulo = doc.getString("title");
                        if (titulo == null) titulo = doc.getString("titulo");

                        if (titulo != null && titulo.equals(tituloLibro)) {
                            // ✅ Actualizar ambos campos para compatibilidad
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("status", nuevoEstado);
                            updates.put("estado", nuevoEstado);

                            librosRef.document(doc.getId()).update(updates)
                                    .addOnSuccessListener(aVoid ->
                                            Log.d(TAG, "✅ Estado actualizado: " + tituloLibro + " → " + nuevoEstado))
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "❌ Error actualizando estado: " + e.getMessage()));
                            break;
                        }
                    }
                    loadLibrosDisponibles();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error buscando libro: " + e.getMessage());
                    Toast.makeText(this, "Error al actualizar estado", Toast.LENGTH_SHORT).show();
                });
    }

    // ✅ MÉTODO MEJORADO: Manejo de valores NULL
    private void loadPrestamos() {
        if (layoutListaPrestamos == null) return;

        layoutListaPrestamos.removeAllViews();
        Log.d(TAG, "📚 Cargando préstamos...");

        prestamosRef.get().addOnSuccessListener(query -> {
            if (query.isEmpty()) {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("📚 No hay préstamos registrados");
                tvEmpty.setTextSize(16);
                tvEmpty.setTextColor(getResources().getColor(R.color.colorTextSecondary));
                tvEmpty.setPadding(16, 32, 16, 32);
                tvEmpty.setGravity(android.view.Gravity.CENTER);
                layoutListaPrestamos.addView(tvEmpty);

                if (tvTotalPrestamos != null) {
                    tvTotalPrestamos.setText("Total de Préstamos: 0");
                }
                return;
            }

            int totalPrestamos = 0;
            int prestamosActivos = 0;
            int prestamosDevueltos = 0;

            for (DocumentSnapshot doc : query) {
                // ✅ MANEJAR VALORES NULL con valores por defecto
                String usuario = doc.getString("usuario");
                if (usuario == null || usuario.isEmpty()) usuario = "Usuario desconocido";

                String libro = doc.getString("libro");
                if (libro == null || libro.isEmpty()) libro = "Libro no especificado";

                String fechaPrestamo = doc.getString("fechaPrestamo");
                if (fechaPrestamo == null || fechaPrestamo.isEmpty()) fechaPrestamo = "Sin fecha";

                String fechaDevolucion = doc.getString("fechaDevolucion");
                if (fechaDevolucion == null || fechaDevolucion.isEmpty()) fechaDevolucion = "Sin fecha";

                String estado = doc.getString("estado");
                if (estado == null || estado.isEmpty()) estado = "Desconocido";

                // Contar estadísticas
                totalPrestamos++;
                if ("Activo".equalsIgnoreCase(estado)) {
                    prestamosActivos++;
                } else if ("Devuelto".equalsIgnoreCase(estado)) {
                    prestamosDevueltos++;
                }

                Log.d(TAG, "📖 Préstamo: " + usuario + " - " + libro + " | Estado: " + estado);

                CardView card = crearCardPrestamo(doc.getId(), usuario, libro,
                        fechaPrestamo, fechaDevolucion, estado);
                layoutListaPrestamos.addView(card);
            }

            // ✅ Actualizar estadísticas
            if (tvTotalPrestamos != null) {
                String estadisticas = String.format(
                        "Total: %d | Activos: %d | Devueltos: %d",
                        totalPrestamos, prestamosActivos, prestamosDevueltos
                );
                tvTotalPrestamos.setText(estadisticas);
            }

            Log.d(TAG, "✅ Préstamos cargados: " + totalPrestamos);
        });
    }

    private CardView crearCardPrestamo(String id, String usuario, String libro,
                                       String fechaPrestamo, String fechaDevolucion, String estado) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, 16);
        cardView.setLayoutParams(layoutParams);
        cardView.setCardElevation(8);
        cardView.setRadius(12);
        cardView.setCardBackgroundColor(getResources().getColor(R.color.light_brown));

        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setPadding(16, 16, 16, 16);

        // Usuario
        TextView tvUsuario = new TextView(this);
        tvUsuario.setText("👤 " + usuario);
        tvUsuario.setTextSize(18);
        tvUsuario.setTypeface(null, android.graphics.Typeface.BOLD);
        tvUsuario.setTextColor(getResources().getColor(R.color.colorPrimary));
        cardLayout.addView(tvUsuario);

        // Libro
        TextView tvLibro = new TextView(this);
        tvLibro.setText("📚 " + libro);
        tvLibro.setTextSize(15);
        tvLibro.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        cardLayout.addView(tvLibro);

        // Fechas
        TextView tvFechas = new TextView(this);
        tvFechas.setText("📅 Préstamo: " + fechaPrestamo + " → Devolución: " + fechaDevolucion);
        tvFechas.setTextSize(13);
        tvFechas.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        cardLayout.addView(tvFechas);

        // Estado
        TextView tvEstado = new TextView(this);
        tvEstado.setText("📊 Estado: " + estado);
        tvEstado.setTextSize(15);
        tvEstado.setTypeface(null, android.graphics.Typeface.BOLD);

        // ✅ Colores según estado
        int colorEstado;
        if ("Activo".equalsIgnoreCase(estado)) {
            colorEstado = R.color.green;
        } else if ("Devuelto".equalsIgnoreCase(estado)) {
            colorEstado = R.color.colorPrimary;
        } else {
            colorEstado = R.color.orange;
        }
        tvEstado.setTextColor(getResources().getColor(colorEstado));
        cardLayout.addView(tvEstado);

        // Botón de devolución solo para préstamos activos
        if ("Activo".equalsIgnoreCase(estado)) {
            Button btnDevolver = new Button(this);
            btnDevolver.setText("REGISTRAR DEVOLUCIÓN");
            btnDevolver.setTextSize(12);
            btnDevolver.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            btnDevolver.setTextColor(getResources().getColor(R.color.white));
            btnDevolver.setPadding(16, 12, 16, 12);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            btnParams.setMargins(0, 12, 0, 0);
            btnDevolver.setLayoutParams(btnParams);
            btnDevolver.setOnClickListener(v -> registrarDevolucion(id, libro));
            cardLayout.addView(btnDevolver);
        }

        cardView.addView(cardLayout);
        return cardView;
    }

    private void registrarDevolucion(String idPrestamo, String libro) {
        Log.d(TAG, "📤 Registrando devolución: " + idPrestamo);

        prestamosRef.document(idPrestamo).update("estado", "Devuelto")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Devolución registrada");
                    updateLibroEstado(libro, "Disponible");
                    Toast.makeText(this, "✅ Devolución registrada", Toast.LENGTH_SHORT).show();
                    loadPrestamos();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error en devolución: " + e.getMessage());
                    Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}