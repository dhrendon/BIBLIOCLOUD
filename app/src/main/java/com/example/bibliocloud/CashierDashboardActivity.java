package com.example.bibliocloud;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class CashierDashboardActivity extends AppCompatActivity {

    private Button btnCobrarOrdenes, btnVerInventario, btnCerrarSesion;
    private TextView tvWelcome, tvBranchName, tvPendingOrders;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String cashierName;
    private String branchId;
    private String branchName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cashier_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        loadCashierInfo();
        loadPendingOrdersCount();
        setupListeners();
    }

    private void initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvBranchName = findViewById(R.id.tvBranchName);
        tvPendingOrders = findViewById(R.id.tvPendingOrders);
        btnCobrarOrdenes = findViewById(R.id.btnCobrarOrdenes);
        btnVerInventario = findViewById(R.id.btnVerInventario);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
    }

    private void loadCashierInfo() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        cashierName = prefs.getString("current_user_name", "Cajero");
        branchId = prefs.getString("branch_id", "");
        branchName = prefs.getString("branch_name", "Sucursal");

        tvWelcome.setText("Bienvenido, " + cashierName);
        tvBranchName.setText("📍 Sucursal: " + branchName);
    }

    private void loadPendingOrdersCount() {
        // Cargar órdenes pendientes de la sucursal del cajero
        db.collection("compras")
                .whereEqualTo("status", "Pendiente")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        tvPendingOrders.setText("Órdenes pendientes: 0");
                        return;
                    }

                    int count = 0;
                    // Filtrar por sucursal si es necesario
                    if (!branchId.isEmpty()) {
                        // Contar solo las órdenes de esta sucursal
                        for (var doc : value.getDocuments()) {
                            // Aquí puedes agregar lógica adicional para filtrar
                            count++;
                        }
                    } else {
                        count = value.size();
                    }

                    tvPendingOrders.setText("Órdenes pendientes: " + count);
                });
    }

    private void setupListeners() {
        btnCobrarOrdenes.setOnClickListener(v -> {
            Intent intent = new Intent(this, CashierOrdersActivity.class);
            intent.putExtra("branchId", branchId);
            intent.putExtra("branchName", branchName);
            startActivity(intent);
        });

        btnVerInventario.setOnClickListener(v -> {
            Intent intent = new Intent(this, CashierInventoryActivity.class);
            intent.putExtra("branchId", branchId);
            intent.putExtra("branchName", branchName);
            startActivity(intent);
        });

        btnCerrarSesion.setOnClickListener(v -> logout());
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        mAuth.signOut();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPendingOrdersCount();
    }
}