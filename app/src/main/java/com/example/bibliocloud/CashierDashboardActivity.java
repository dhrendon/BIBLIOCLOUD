package com.example.bibliocloud;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CashierDashboardActivity extends AppCompatActivity {

    private static final String TAG = "CashierDashboard";

    private Button btnCobrarOrdenes, btnVerInventario, btnVerPagosDiarios, btnCerrarSesion;
    private TextView tvWelcome, tvBranchName, tvPendingOrders, tvTodayPayments;

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
        loadTodayPayments();
        setupListeners();
    }

    private void initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvBranchName = findViewById(R.id.tvBranchName);
        tvPendingOrders = findViewById(R.id.tvPendingOrders);
        tvTodayPayments = findViewById(R.id.tvTodayPayments);

        btnCobrarOrdenes = findViewById(R.id.btnCobrarOrdenes);
        btnVerInventario = findViewById(R.id.btnVerInventario);
        btnVerPagosDiarios = findViewById(R.id.btnVerPagosDiarios);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
    }

    private void loadCashierInfo() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        cashierName = prefs.getString("current_user_name", "Cajero");
        branchId = prefs.getString("branch_id", "");
        branchName = prefs.getString("branch_name", "Sucursal");

        tvWelcome.setText("Bienvenido, " + cashierName);
        tvBranchName.setText("📍 Sucursal: " + branchName);

        Log.d(TAG, "👤 Cajero: " + cashierName);
        Log.d(TAG, "🏢 Sucursal: " + branchName + " (ID: " + branchId + ")");
    }

    private void loadPendingOrdersCount() {
        db.collection("compras")
                .whereEqualTo("status", "Pendiente")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        Log.e(TAG, "Error cargando órdenes: " + (error != null ? error.getMessage() : "null"));
                        tvPendingOrders.setText("Órdenes pendientes: 0");
                        return;
                    }

                    int count = value.size();
                    tvPendingOrders.setText("Órdenes pendientes: " + count);
                    Log.d(TAG, "📋 Órdenes pendientes: " + count);
                });
    }

    private void loadTodayPayments() {
        // Obtener fecha actual en formato dd/MM/yyyy
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String today = dateFormat.format(new Date());

        Log.d(TAG, "🔍 Consultando pagos para fecha: " + today);
        Log.d(TAG, "🏢 Sucursal: " + branchName);

        db.collection("pagos")
                .whereEqualTo("estado", "Completado")
                .whereEqualTo("formattedDate", today)
                .whereEqualTo("nombre_sucursal", branchName)
                .addSnapshotListener((value, error) -> {

                    if (error != null) {
                        Log.e(TAG, "❌ Error en consulta: " + error.getMessage());
                        tvTodayPayments.setText("Pagos hoy: 0 | $0.00");
                        return;
                    }

                    if (value == null || value.isEmpty()) {
                        Log.w(TAG, "⚠️ No se encontraron pagos para hoy");
                        tvTodayPayments.setText("Pagos hoy: 0 | $0.00");
                        return;
                    }

                    int count = value.size();
                    double total = 0;

                    Log.d(TAG, "✅ Pagos encontrados: " + count);

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        // Intentar obtener el monto
                        Double amount = doc.getDouble("monto");
                        if (amount == null) {
                            amount = doc.getDouble("subtotal");
                        }

                        if (amount != null) {
                            total += amount;
                            Log.d(TAG, "💰 Pago ID: " + doc.getId() + " - $" + amount);
                        } else {
                            Log.w(TAG, "⚠️ Pago sin monto: " + doc.getId());
                        }
                    }

                    String displayText = String.format(Locale.getDefault(),
                            "Pagos hoy: %d | $%.2f", count, total);
                    tvTodayPayments.setText(displayText);

                    Log.d(TAG, "📊 Total calculado: $" + total);
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

        btnVerPagosDiarios.setOnClickListener(v -> {
            Intent intent = new Intent(this, CashierDailyPaymentsActivity.class);
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
        loadTodayPayments();
    }
}