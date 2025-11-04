// AdminDashboardActivity.java
package com.example.bibliocloud;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class AdminDashboardActivity extends AppCompatActivity {

    private Button btnGestionUsuarios, btnGestionLibros, btnGestionPrestamos, btnSugerencias, btnCerrarSesion;
    private TextView tvTotalLibros, tvTotalUsuarios, tvTotalPrestamos;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // === Inicializar Firebase ===
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // === Inicializar vistas ===
        inicializarVistas();

        // === Cargar estadísticas ===
        cargarEstadisticasTiempoReal();

        // === Configurar listeners ===
        configurarListeners();
    }

    private void inicializarVistas() {
        // Botones
        btnGestionUsuarios = findViewById(R.id.btnGestionUsuarios);
        btnGestionLibros = findViewById(R.id.btnGestionLibros);
        btnGestionPrestamos = findViewById(R.id.btnGestionPrestamos);
        btnSugerencias = findViewById(R.id.btnSugerencias);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        // TextViews para estadísticas
        tvTotalLibros = findViewById(R.id.tvTotalLibros);
        tvTotalUsuarios = findViewById(R.id.tvTotalUsuarios);
        tvTotalPrestamos = findViewById(R.id.tvTotalPrestamos);
    }

    private void cargarEstadisticasTiempoReal() {
        // Listener en tiempo real para libros
        db.collection("libros")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        tvTotalLibros.setText("0");
                        return;
                    }
                    if (value != null) {
                        tvTotalLibros.setText(String.valueOf(value.size()));
                    }
                });

        // Listener en tiempo real para usuarios
        db.collection("usuarios")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        tvTotalUsuarios.setText("0");
                        return;
                    }
                    if (value != null) {
                        tvTotalUsuarios.setText(String.valueOf(value.size()));
                    }
                });

        // Listener en tiempo real para préstamos
        db.collection("prestamos")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        tvTotalPrestamos.setText("0");
                        return;
                    }
                    if (value != null) {
                        tvTotalPrestamos.setText(String.valueOf(value.size()));
                    }
                });
    }

    private void configurarListeners() {
        // === Abrir gestión de usuarios ===
        btnGestionUsuarios.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserManagementActivity.class);
            startActivity(intent);
        });

        // === Abrir gestión de libros ===
        btnGestionLibros.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookManagementActivity.class);
            startActivity(intent);
        });

        // === Abrir gestión de préstamos ===
        btnGestionPrestamos.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoanManagementActivity.class);
            startActivity(intent);
        });

        // === Abrir gestión de sugerencias ===
        btnSugerencias.setOnClickListener(v -> {
            Intent intent = new Intent(this, SuggestionsManagementActivity.class);
            startActivity(intent);
        });

        // === Cerrar sesión con Firebase ===
        btnCerrarSesion.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // 🔹 Verificar si hay sesión activa
        if (mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar estadísticas cuando la actividad se reanude
        cargarEstadisticasTiempoReal();
    }
}