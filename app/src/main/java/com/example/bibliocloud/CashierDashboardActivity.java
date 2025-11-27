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
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cashier_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
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
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "❌ No hay usuario autenticado");
            tvWelcome.setText("Error: No autenticado");
            tvBranchName.setText("📍 Sin sesión");
            return;
        }

        userId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "🔑 User ID: " + userId);

        // Siempre cargar desde Firestore para tener datos actualizados
        loadCashierFromFirestore();
    }

    private void loadCashierFromFirestore() {
        db.collection("usuarios")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.e(TAG, "❌ No se encontró el documento del usuario");
                        tvWelcome.setText("Error: Usuario no encontrado");
                        tvBranchName.setText("📍 Sin datos");
                        return;
                    }

                    cashierName = documentSnapshot.getString("nombre");
                    branchId = documentSnapshot.getString("sucursal_id");
                    String branchNameFromUser = documentSnapshot.getString("nombre_sucursal");

                    Log.d(TAG, "✅ Datos del cajero cargados");
                    Log.d(TAG, "👤 Nombre: " + cashierName);
                    Log.d(TAG, "🏢 Sucursal ID: " + branchId);
                    Log.d(TAG, "🏢 Nombre sucursal (del usuario): " + branchNameFromUser);
                    Log.d(TAG, "📄 Documento completo: " + documentSnapshot.getData());

                    // Si tiene sucursal_id, cargar el nombre real desde sucursales
                    if (branchId != null && !branchId.isEmpty()) {
                        loadBranchName(branchId);
                    } else {
                        // Si no tiene sucursal_id pero sí nombre_sucursal, usar ese
                        if (branchNameFromUser != null && !branchNameFromUser.isEmpty()) {
                            Log.w(TAG, "⚠️ Usando nombre_sucursal del usuario (no hay sucursal_id)");
                            branchName = branchNameFromUser;
                            displayCashierInfo();
                            loadPendingOrdersCount();
                            loadTodayPayments();
                        } else {
                            Log.e(TAG, "❌ El cajero no tiene sucursal asignada");
                            tvBranchName.setText("📍 Sin sucursal asignada");
                            tvWelcome.setText("Bienvenido, " + (cashierName != null ? cashierName : "Cajero"));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al cargar cajero: " + e.getMessage(), e);
                    tvWelcome.setText("Error al cargar datos");
                    tvBranchName.setText("📍 Error de conexión");
                });
    }

    private void loadBranchName(String sucursalId) {
        Log.d(TAG, "🔍 Buscando sucursal con ID: " + sucursalId);

        db.collection("sucursales")
                .document(sucursalId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.e(TAG, "❌ No existe documento para sucursal ID: " + sucursalId);
                        tvBranchName.setText("📍 Sucursal no encontrada");
                        tvWelcome.setText("Bienvenido, " + (cashierName != null ? cashierName : "Cajero"));
                        return;
                    }

                    Log.d(TAG, "📄 Documento de sucursal encontrado: " + documentSnapshot.getData());

                    // Buscar el campo 'name' (según el modelo Branch)
                    branchName = documentSnapshot.getString("name");

                    // Fallback: intentar también 'nombre' por si hay inconsistencias
                    if (branchName == null || branchName.isEmpty()) {
                        branchName = documentSnapshot.getString("nombre");
                        Log.w(TAG, "⚠️ Campo 'name' no encontrado, usando 'nombre': " + branchName);
                    }

                    if (branchName != null && !branchName.isEmpty()) {
                        Log.d(TAG, "✅ Sucursal encontrada: " + branchName);

                        // Guardar en SharedPreferences
                        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("current_user_name", cashierName);
                        editor.putString("branch_id", branchId);
                        editor.putString("branch_name", branchName);
                        editor.apply();

                        // Mostrar información
                        displayCashierInfo();
                        loadPendingOrdersCount();
                        loadTodayPayments();
                    } else {
                        Log.e(TAG, "❌ El documento de sucursal no tiene campo 'name' o 'nombre'");
                        tvBranchName.setText("📍 Sucursal sin nombre");
                        tvWelcome.setText("Bienvenido, " + (cashierName != null ? cashierName : "Cajero"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al cargar sucursal: " + e.getMessage(), e);
                    tvBranchName.setText("📍 Error al cargar sucursal");
                    tvWelcome.setText("Bienvenido, " + (cashierName != null ? cashierName : "Cajero"));
                });
    }

    private void displayCashierInfo() {
        tvWelcome.setText("Bienvenido, " + (cashierName != null ? cashierName : "Cajero"));
        tvBranchName.setText("📍 Sucursal: " + (branchName != null ? branchName : "Sin asignar"));

        Log.d(TAG, "✅ Información mostrada:");
        Log.d(TAG, "👤 Cajero: " + cashierName);
        Log.d(TAG, "🏢 Sucursal: " + branchName + " (ID: " + branchId + ")");
    }

    private void loadPendingOrdersCount() {

        if (branchId == null || branchId.isEmpty()) {
            Log.e(TAG, "⚠️ No hay branchId asignado, imposible filtrar órdenes");
            tvPendingOrders.setText("Órdenes pendientes: 0");
            return;
        }

        db.collection("compras")
                .whereEqualTo("status", "Pendiente")
                .whereEqualTo("branchId", branchId)   // 🔥 FILTRO CORRECTO
                .addSnapshotListener((value, error) -> {

                    if (error != null) {
                        Log.e(TAG, "❌ Error cargando órdenes: " + error.getMessage());
                        tvPendingOrders.setText("Órdenes pendientes: 0");
                        return;
                    }

                    if (value == null || value.isEmpty()) {
                        Log.d(TAG, "📭 No hay órdenes pendientes para esta sucursal");
                        tvPendingOrders.setText("Órdenes pendientes: 0");
                        return;
                    }

                    int count = value.size();
                    tvPendingOrders.setText("Órdenes pendientes: " + count);

                    Log.d(TAG, "📦 Órdenes pendientes para la sucursal " + branchId + ": " + count);
                });
    }


    private void loadTodayPayments() {
        if (branchName == null || branchName.isEmpty()) {
            Log.w(TAG, "⚠️ No hay nombre de sucursal, no se pueden cargar pagos");
            tvTodayPayments.setText("Pagos hoy: 0 | $0.00");
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String today = dateFormat.format(new Date());

        Log.d(TAG, "🔍 Consultando pagos:");
        Log.d(TAG, "   - Fecha: " + today);
        Log.d(TAG, "   - Sucursal: " + branchName);

        db.collection("pagos")
                .whereEqualTo("estado", "Completado")
                .whereEqualTo("formattedDate", today)
                .whereEqualTo("nombre_sucursal", branchName)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Error en consulta de pagos: " + error.getMessage());
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
                        Double amount = doc.getDouble("monto");
                        if (amount == null) amount = doc.getDouble("subtotal");

                        if (amount != null) {
                            total += amount;
                            Log.d(TAG, "💰 Pago: $" + amount);
                        }
                    }

                    tvTodayPayments.setText(String.format(Locale.getDefault(),
                            "Pagos hoy: %d | $%.2f", count, total));
                    Log.d(TAG, "📊 Total: $" + total);
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
        Log.d(TAG, "📱 onResume() - Cargando información del cajero");
        loadCashierInfo();
    }
}