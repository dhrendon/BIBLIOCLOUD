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

public class AdminDashboardActivity extends AppCompatActivity {

    private Button btnGestionUsuarios, btnGestionLibros, btnGestionPrestamos,
            btnSugerencias, btnGestionSucursales, btnGestionCompras, btnCerrarSesion;
    private TextView tvTotalLibros, tvTotalUsuarios, tvTotalPrestamos,
            tvTotalSucursales, tvTotalCompras;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        inicializarVistas();
        cargarEstadisticasTiempoReal();
        configurarListeners();
    }

    private void inicializarVistas() {
        // Botones existentes
        btnGestionUsuarios = findViewById(R.id.btnGestionUsuarios);
        btnGestionLibros = findViewById(R.id.btnGestionLibros);
        btnGestionPrestamos = findViewById(R.id.btnGestionPrestamos);
        btnSugerencias = findViewById(R.id.btnSugerencias);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        // 🆕 Nuevos botones
        btnGestionSucursales = findViewById(R.id.btnGestionSucursales);
        btnGestionCompras = findViewById(R.id.btnGestionCompras);

        // Estadísticas existentes
        tvTotalLibros = findViewById(R.id.tvTotalLibros);
        tvTotalUsuarios = findViewById(R.id.tvTotalUsuarios);
        tvTotalPrestamos = findViewById(R.id.tvTotalPrestamos);

        // 🆕 Nuevas estadísticas
        tvTotalSucursales = findViewById(R.id.tvTotalSucursales);
        tvTotalCompras = findViewById(R.id.tvTotalCompras);
    }

    private void cargarEstadisticasTiempoReal() {
        // Libros
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

        // Usuarios
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

        // Préstamos
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

        // 🆕 Sucursales
        db.collection("sucursales")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        tvTotalSucursales.setText("0");
                        return;
                    }
                    if (value != null) {
                        tvTotalSucursales.setText(String.valueOf(value.size()));
                    }
                });

        // 🆕 Compras
        db.collection("compras")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        tvTotalCompras.setText("0");
                        return;
                    }
                    if (value != null) {
                        tvTotalCompras.setText(String.valueOf(value.size()));
                    }
                });
    }

    private void configurarListeners() {
        // Gestión de usuarios
        btnGestionUsuarios.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserManagementActivity.class);
            startActivity(intent);
        });

        // Gestión de libros
        btnGestionLibros.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookManagementActivity.class);
            startActivity(intent);
        });

        // Gestión de préstamos
        btnGestionPrestamos.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoanManagementActivity.class);
            startActivity(intent);
        });

        // Gestión de sugerencias
        btnSugerencias.setOnClickListener(v -> {
            Intent intent = new Intent(this, SuggestionsManagementActivity.class);
            startActivity(intent);
        });

        // 🆕 Gestión de sucursales
        btnGestionSucursales.setOnClickListener(v -> {
            Intent intent = new Intent(this, BranchManagementActivity.class);
            startActivity(intent);
        });

        // 🆕 Gestión de compras
        btnGestionCompras.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminPurchasesActivity.class);
            startActivity(intent);
        });

        // Cerrar sesión
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

        if (mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarEstadisticasTiempoReal();
    }
}