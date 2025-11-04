package com.example.bibliocloud;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

    private Spinner spinnerUsuario, spinnerLibro;
    private EditText etFechaPrestamo, etFechaDevolucion;
    private MaterialButton btnRealizarPrestamo, btnVolver, btnSeleccionarFechaPrestamo, btnSeleccionarFechaDevolucion;
    private LinearLayout layoutListaPrestamos;

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
        btnRealizarPrestamo = findViewById(R.id.btnRealizarPrestamo);
        btnVolver = findViewById(R.id.btnVolver);
        btnSeleccionarFechaPrestamo = findViewById(R.id.btnSeleccionarFechaPrestamo);
        btnSeleccionarFechaDevolucion = findViewById(R.id.btnSeleccionarFechaDevolucion);
        layoutListaPrestamos = findViewById(R.id.layoutListaPrestamos);

        calendarPrestamo = Calendar.getInstance();
        calendarDevolucion = Calendar.getInstance();
        calendarDevolucion.add(Calendar.DAY_OF_YEAR, 14); // 2 semanas por defecto
        dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        etFechaPrestamo.setText(dateFormatter.format(calendarPrestamo.getTime()));
        etFechaDevolucion.setText(dateFormatter.format(calendarDevolucion.getTime()));
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        prestamosRef = db.collection("prestamos");
        usuariosRef = db.collection("usuarios");
        librosRef = db.collection("libros");
    }

    private void loadUsuarios() {
        usuariosRef.get().addOnSuccessListener(query -> {
            usuariosList.clear();
            for (DocumentSnapshot doc : query) {
                String nombre = doc.getString("nombre");
                if (nombre != null) usuariosList.add(nombre);
            }
            spinnerUsuario.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, usuariosList));
        });
    }

    private void loadLibrosDisponibles() {
        librosRef.whereEqualTo("estado", "Disponible").get().addOnSuccessListener(query -> {
            librosDisponiblesList.clear();
            for (DocumentSnapshot doc : query) {
                String titulo = doc.getString("titulo");
                String autor = doc.getString("autor");
                if (titulo != null && autor != null) {
                    librosDisponiblesList.add(titulo + " - " + autor);
                }
            }
            spinnerLibro.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, librosDisponiblesList));
        });
    }

    private void setupDatePickers() {
        btnSeleccionarFechaPrestamo.setOnClickListener(v ->
                showDatePickerDialog(etFechaPrestamo, calendarPrestamo));

        btnSeleccionarFechaDevolucion.setOnClickListener(v ->
                showDatePickerDialog(etFechaDevolucion, calendarDevolucion));
    }

    private void showDatePickerDialog(final EditText editText, final Calendar calendar) {
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
        btnRealizarPrestamo.setOnClickListener(v -> realizarPrestamo());
        btnVolver.setOnClickListener(v -> finish());
    }

    private void realizarPrestamo() {
        String usuario = spinnerUsuario.getSelectedItem() != null ? spinnerUsuario.getSelectedItem().toString() : "";
        String libro = spinnerLibro.getSelectedItem() != null ? spinnerLibro.getSelectedItem().toString() : "";
        String fechaPrestamo = etFechaPrestamo.getText().toString();
        String fechaDevolucion = etFechaDevolucion.getText().toString();

        if (usuario.isEmpty() || libro.isEmpty()) {
            Toast.makeText(this, "Seleccione usuario y libro", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> prestamo = new HashMap<>();
        prestamo.put("usuario", usuario);
        prestamo.put("libro", libro);
        prestamo.put("fechaPrestamo", fechaPrestamo);
        prestamo.put("fechaDevolucion", fechaDevolucion);
        prestamo.put("estado", "Activo");

        prestamosRef.add(prestamo)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Préstamo registrado correctamente", Toast.LENGTH_SHORT).show();
                    loadPrestamos();
                    updateLibroEstado(libro, "Prestado");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateLibroEstado(String libro, String nuevoEstado) {
        librosRef.whereEqualTo("titulo", libro.split(" - ")[0])
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query) {
                        librosRef.document(doc.getId()).update("estado", nuevoEstado);
                    }
                    loadLibrosDisponibles();
                });
    }

    private void loadPrestamos() {
        layoutListaPrestamos.removeAllViews();

        prestamosRef.get().addOnSuccessListener(query -> {
            if (query.isEmpty()) {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("No hay préstamos registrados");
                layoutListaPrestamos.addView(tvEmpty);
                return;
            }

            for (DocumentSnapshot doc : query) {
                String usuario = doc.getString("usuario");
                String libro = doc.getString("libro");
                String fechaPrestamo = doc.getString("fechaPrestamo");
                String fechaDevolucion = doc.getString("fechaDevolucion");
                String estado = doc.getString("estado");

                CardView card = crearCardPrestamo(doc.getId(), usuario, libro, fechaPrestamo, fechaDevolucion, estado);
                layoutListaPrestamos.addView(card);
            }
        });
    }

    private CardView crearCardPrestamo(String id, String usuario, String libro, String fechaPrestamo, String fechaDevolucion, String estado) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, 16);
        cardView.setLayoutParams(layoutParams);
        cardView.setCardElevation(4);
        cardView.setRadius(8);
        cardView.setCardBackgroundColor(getResources().getColor(R.color.light_brown));

        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setPadding(16, 16, 16, 16);

        TextView tvUsuario = new TextView(this);
        tvUsuario.setText("👤 " + usuario);
        tvUsuario.setTextSize(16);
        cardLayout.addView(tvUsuario);

        TextView tvLibro = new TextView(this);
        tvLibro.setText("📚 " + libro);
        cardLayout.addView(tvLibro);

        TextView tvFechas = new TextView(this);
        tvFechas.setText("📅 " + fechaPrestamo + " → " + fechaDevolucion);
        cardLayout.addView(tvFechas);

        TextView tvEstado = new TextView(this);
        tvEstado.setText("🔄 Estado: " + estado);
        cardLayout.addView(tvEstado);

        Button btnDevolver = new Button(this);
        btnDevolver.setText("Registrar Devolución");
        btnDevolver.setOnClickListener(v -> registrarDevolucion(id, libro));
        cardLayout.addView(btnDevolver);

        cardView.addView(cardLayout);
        return cardView;
    }

    private void registrarDevolucion(String idPrestamo, String libro) {
        prestamosRef.document(idPrestamo).update("estado", "Devuelto")
                .addOnSuccessListener(aVoid -> {
                    updateLibroEstado(libro, "Disponible");
                    Toast.makeText(this, "Devolución registrada", Toast.LENGTH_SHORT).show();
                    loadPrestamos();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
