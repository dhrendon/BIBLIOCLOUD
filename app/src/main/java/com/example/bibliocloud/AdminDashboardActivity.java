package com.example.bibliocloud;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity extends AppCompatActivity {

    // Botones de gestión
    private Button btnGestionUsuarios, btnGestionLibros, btnGestionPrestamos,
            btnSugerencias, btnGestionSucursales, btnGestionCompras, btnCerrarSesion;

    // TextViews de estadísticas
    private TextView tvTotalLibros, tvTotalUsuarios, tvTotalPrestamos,
            tvTotalSucursales, tvTotalCompras;

    // Cards clickeables de estadísticas
    private CardView cardTotalLibros, cardTotalUsuarios, cardTotalPrestamos,
            cardTotalSucursales, cardTotalCompras;

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
        setupCardClicks(); // 🔥 Cards clickeables
    }

    private void inicializarVistas() {
        // === BOTONES ===
        btnGestionUsuarios = findViewById(R.id.btnGestionUsuarios);
        btnGestionLibros = findViewById(R.id.btnGestionLibros);
        btnGestionPrestamos = findViewById(R.id.btnGestionPrestamos);
        btnSugerencias = findViewById(R.id.btnSugerencias);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        btnGestionSucursales = findViewById(R.id.btnGestionSucursales);
        btnGestionCompras = findViewById(R.id.btnGestionCompras);

        // === TEXTVIEWS DE ESTADÍSTICAS ===
        tvTotalLibros = findViewById(R.id.tvTotalLibros);
        tvTotalUsuarios = findViewById(R.id.tvTotalUsuarios);
        tvTotalPrestamos = findViewById(R.id.tvTotalPrestamos);
        tvTotalSucursales = findViewById(R.id.tvTotalSucursales);
        tvTotalCompras = findViewById(R.id.tvTotalCompras);

        // === CARDS CLICKEABLES ===
        cardTotalLibros = findViewById(R.id.cardTotalLibros);
        cardTotalUsuarios = findViewById(R.id.cardTotalUsuarios);
        cardTotalPrestamos = findViewById(R.id.cardTotalPrestamos);
        cardTotalSucursales = findViewById(R.id.cardTotalSucursales);
        cardTotalCompras = findViewById(R.id.cardTotalCompras);
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

        // Sucursales
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

        // Compras
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

    // 🔥 CONFIGURAR LISTENERS DE BOTONES
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

        // 🔥🔥🔥 GESTIÓN DE PRÉSTAMOS - MOSTRAR MENÚ
        btnGestionPrestamos.setOnClickListener(v -> {
            mostrarMenuPrestamos();
        });

        // Gestión de sugerencias
        btnSugerencias.setOnClickListener(v -> {
            Intent intent = new Intent(this, SuggestionsManagementActivity.class);
            startActivity(intent);
        });

        // Gestión de sucursales
        btnGestionSucursales.setOnClickListener(v -> {
            Intent intent = new Intent(this, BranchManagementActivity.class);
            startActivity(intent);
        });

        // Gestión de compras
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

    // 🔥🔥🔥 NUEVO MÉTODO: Mostrar menú de opciones de préstamos
    private void mostrarMenuPrestamos() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📚 Gestión de Préstamos");

        String[] opciones = {
                "📋 Ver todos los préstamos",
                "🔧 Asignar préstamo a usuario",
                "❌ Cancelar"
        };

        builder.setItems(opciones, (dialog, which) -> {
            switch (which) {
                case 0: // Ver todos los préstamos
                    Intent intentVer = new Intent(this, LoanManagementActivity.class);
                    startActivity(intentVer);
                    break;

                case 1: // Asignar préstamo como admin
                    Intent intentAsignar = new Intent(this, AdminAssignLoanActivity.class);
                    startActivity(intentAsignar);
                    break;

                case 2: // Cancelar
                    dialog.dismiss();
                    break;
            }
        });

        builder.show();
    }

    // 🔥 CONFIGURAR CLICKS EN LAS CARDS DE ESTADÍSTICAS
    private void setupCardClicks() {
        // Card de Total Libros
        if (cardTotalLibros != null) {
            cardTotalLibros.setOnClickListener(v -> {
                Intent intent = new Intent(this, BookManagementActivity.class);
                startActivity(intent);
            });
        }

        // Card de Total Usuarios
        if (cardTotalUsuarios != null) {
            cardTotalUsuarios.setOnClickListener(v -> {
                Intent intent = new Intent(this, UserManagementActivity.class);
                startActivity(intent);
            });
        }

        // Card de Total Préstamos - TAMBIÉN ABRE EL MENÚ
        if (cardTotalPrestamos != null) {
            cardTotalPrestamos.setOnClickListener(v -> {
                mostrarMenuPrestamos();
            });
        }

        // Card de Total Sucursales
        if (cardTotalSucursales != null) {
            cardTotalSucursales.setOnClickListener(v -> {
                Intent intent = new Intent(this, BranchManagementActivity.class);
                startActivity(intent);
            });
        }

        // Card de Total Compras
        if (cardTotalCompras != null) {
            cardTotalCompras.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminPurchasesActivity.class);
                startActivity(intent);
            });
        }
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