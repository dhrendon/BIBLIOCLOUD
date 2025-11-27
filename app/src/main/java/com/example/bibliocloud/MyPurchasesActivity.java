package com.example.bibliocloud;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.example.bibliocloud.models.PurchaseOrder;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class MyPurchasesActivity extends AppCompatActivity {

    private static final String TAG = "MyPurchasesActivity";

    private Spinner spinnerFilter;
    private LinearLayout layoutPurchasesList;
    private LinearLayout layoutEmpty;

    // ✅ Variables estadísticas corregidas
    private TextView tvTotalPurchases;
    private TextView tvTotalAmount;
    private TextView tvTotalPending;
    private TextView tvTotalCompleted;

    private MaterialButton btnViewAllTickets;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private List<PurchaseOrder> allPurchases = new ArrayList<>();

    // Variables para cálculos
    private int totalPurchasesCount = 0;
    private double totalAmountValue = 0;
    private int totalPendingCount = 0;
    private int totalCompletedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_purchases);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (allPurchases == null) {
            allPurchases = new ArrayList<>();
        }

        initializeViews();
        setupToolbar();
        setupFilter();
        loadPurchases();
    }

    private void initializeViews() {
        tvTotalPurchases = findViewById(R.id.tvTotalPurchases);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvTotalPending = findViewById(R.id.tvTotalPending);
        tvTotalCompleted = findViewById(R.id.tvTotalCompleted);

        spinnerFilter = findViewById(R.id.spinnerStatusFilter);
        layoutPurchasesList = findViewById(R.id.layoutPurchasesList);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        btnViewAllTickets = findViewById(R.id.btnViewAllTickets);

        if (btnViewAllTickets != null) {
            btnViewAllTickets.setOnClickListener(v -> abrirVisorDeTickets());
        }

        // ✅ Verificar que las vistas existen
        if (tvTotalPurchases == null || tvTotalAmount == null ||
                tvTotalPending == null || tvTotalCompleted == null) {
            Log.e(TAG, "❌ ERROR: Alguna TextView de estadísticas es NULL");
        } else {
            Log.d(TAG, "✅ Todas las TextViews inicializadas correctamente");
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mis Compras");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupFilter() {
        String[] filters = {"Todas", "Pendiente", "Procesando", "Enviada", "Entregada", "Cancelada"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                filters
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterPurchases((String) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadPurchases() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        db.collection("compras")
                .whereEqualTo("userId", userId)
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Error cargando compras: " + error.getMessage());
                        Toast.makeText(this, "Error al cargar compras", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null) {
                        Log.w(TAG, "⚠️ Snapshots es NULL");
                        return;
                    }

                    allPurchases.clear();

                    // ✅ Resetear contadores
                    totalPurchasesCount = 0;
                    totalAmountValue = 0;
                    totalPendingCount = 0;
                    totalCompletedCount = 0;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        PurchaseOrder purchase = doc.toObject(PurchaseOrder.class);
                        if (purchase != null) {
                            purchase.setId(doc.getId());
                            allPurchases.add(purchase);

                            // Incrementar totales
                            totalPurchasesCount++;
                            totalAmountValue += purchase.getTotal();

                            // Contar por estado
                            String status = purchase.getStatus();
                            if (status.equalsIgnoreCase("Pendiente")) {
                                totalPendingCount++;
                            } else if (status.equalsIgnoreCase("Entregada")) {
                                totalCompletedCount++;
                            }

                            Log.d(TAG, "📦 Compra: " + purchase.getId() +
                                    " | Total: $" + purchase.getTotal() +
                                    " | Estado: " + status);
                        }
                    }

                    Log.d(TAG, "📊 Total compras: " + totalPurchasesCount);
                    Log.d(TAG, "💰 Total monto: $" + totalAmountValue);
                    Log.d(TAG, "⏳ Pendientes: " + totalPendingCount);
                    Log.d(TAG, "✅ Completadas: " + totalCompletedCount);

                    updateStatistics();
                    displayPurchases(allPurchases);
                });
    }

    private void filterPurchases(String filter) {
        List<PurchaseOrder> filtered = new ArrayList<>();

        for (PurchaseOrder purchase : allPurchases) {
            if (filter.equals("Todas") ||
                    purchase.getStatus().equalsIgnoreCase(filter)) {
                filtered.add(purchase);
            }
        }

        displayPurchases(filtered);
    }

    private void displayPurchases(List<PurchaseOrder> purchases) {
        layoutPurchasesList.removeAllViews();

        if (purchases.isEmpty()) {
            showEmptyState();
            return;
        }

        layoutEmpty.setVisibility(View.GONE);

        for (PurchaseOrder purchase : purchases) {
            layoutPurchasesList.addView(createPurchaseCard(purchase));
        }
    }

    private CardView createPurchaseCard(PurchaseOrder purchase) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setCardElevation(4);
        card.setRadius(8);

        // ✅ Usar colores de colors.xml
        card.setCardBackgroundColor(getStatusColor(purchase.getStatus()));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 24);

        TextView tvOrderId = new TextView(this);
        tvOrderId.setText("🆔 Orden #" + purchase.getId().substring(0, Math.min(8, purchase.getId().length())));
        tvOrderId.setTextSize(16);
        tvOrderId.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        tvOrderId.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvOrderId);

        TextView tvDate = new TextView(this);
        tvDate.setText("📅 " + purchase.getFormattedOrderDate());
        tvDate.setTextSize(14);
        tvDate.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        layout.addView(tvDate);

        TextView tvTotal = new TextView(this);
        tvTotal.setText(String.format("💰 Total: $%.2f MXN", purchase.getTotal()));
        tvTotal.setTextSize(16);
        tvTotal.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        tvTotal.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvTotal);

        TextView tvStatus = new TextView(this);
        tvStatus.setText("📊 Estado: " + purchase.getStatus());
        tvStatus.setTextSize(14);
        tvStatus.setTextColor(getResources().getColor(getStatusTextColor(purchase.getStatus())));
        tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvStatus);

        // Botones
        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonLayoutParams.setMargins(0, 16, 0, 0);
        buttonsLayout.setLayoutParams(buttonLayoutParams);

        Button btnDetails = new Button(this);
        btnDetails.setText("DETALLES");
        btnDetails.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        btnDetails.setTextColor(getResources().getColor(R.color.white));
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        detailsParams.setMargins(0, 0, 8, 0);
        btnDetails.setLayoutParams(detailsParams);
        btnDetails.setOnClickListener(v -> showPurchaseDetails(purchase));
        buttonsLayout.addView(btnDetails);

        Button btnTicket = new Button(this);
        btnTicket.setText("🎫 TICKET");
        btnTicket.setBackgroundColor(getResources().getColor(R.color.green));
        btnTicket.setTextColor(getResources().getColor(R.color.white));
        LinearLayout.LayoutParams ticketParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        ticketParams.setMargins(8, 0, 0, 0);
        btnTicket.setLayoutParams(ticketParams);
        btnTicket.setOnClickListener(v -> cargarYMostrarTicket(purchase.getId()));
        buttonsLayout.addView(btnTicket);

        layout.addView(buttonsLayout);
        card.addView(layout);

        return card;
    }

    private void cargarYMostrarTicket(String orderId) {
        db.collection("tickets")
                .whereEqualTo("orderId", orderId)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        String ticketContent = query.getDocuments().get(0).getString("ticketContent");
                        if (ticketContent != null) {
                            mostrarTicketDialog(ticketContent);
                        }
                    } else {
                        Toast.makeText(this, "No hay ticket disponible", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void mostrarTicketDialog(String content) {
        new AlertDialog.Builder(this)
                .setTitle("🎫 Ticket de Compra")
                .setMessage(content)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void showPurchaseDetails(PurchaseOrder purchase) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📦 Detalles de Compra");

        String details = "🆔 Orden: " + purchase.getId() + "\n\n" +
                "📅 Fecha: " + purchase.getFormattedOrderDate() + "\n" +
                "💰 Total: $" + String.format("%.2f", purchase.getTotal()) + " MXN\n" +
                "📊 Estado: " + purchase.getStatus() + "\n" +
                "📦 Items: " + purchase.getTotalItems();

        builder.setMessage(details);
        builder.setPositiveButton("Cerrar", null);
        builder.show();
    }

    // ✅ MÉTODO ACTUALIZADO CORRECTAMENTE
    private void updateStatistics() {
        if (tvTotalPurchases == null || tvTotalAmount == null ||
                tvTotalPending == null || tvTotalCompleted == null) {
            Log.e(TAG, "❌ TextViews de estadísticas son NULL");
            return;
        }

        Log.d(TAG, "🔄 Actualizando estadísticas...");

        tvTotalPurchases.setText("Total de compras: " + totalPurchasesCount);
        tvTotalAmount.setText("Monto total: $" + String.format("%.2f", totalAmountValue));
        tvTotalPending.setText("Pendientes: " + totalPendingCount);
        tvTotalCompleted.setText("Completadas: " + totalCompletedCount);

        Log.d(TAG, "✅ Estadísticas actualizadas correctamente");
    }

    private void showEmptyState() {
        layoutEmpty.setVisibility(View.VISIBLE);
        layoutPurchasesList.removeAllViews();
    }

    // ✅ Colores actualizados según colors.xml
    private int getStatusColor(String status) {
        switch (status.toLowerCase()) {
            case "pendiente":
                return getResources().getColor(R.color.light_brown);
            case "procesando":
                return getResources().getColor(R.color.accent_brown);
            case "enviada":
                return getResources().getColor(R.color.light_brown);
            case "entregada":
                return getResources().getColor(R.color.light_brown);
            case "cancelada":
                return getResources().getColor(R.color.light_brown);
            default:
                return getResources().getColor(R.color.light_brown);
        }
    }

    // ✅ Colores de texto actualizados según colors.xml
    private int getStatusTextColor(String status) {
        switch (status.toLowerCase()) {
            case "pendiente":
                return R.color.orange;
            case "entregada":
                return R.color.green;
            case "cancelada":
                return R.color.red;
            case "procesando":
                return R.color.colorPrimary;
            default:
                return R.color.colorTextPrimary;
        }
    }

    private void abrirVisorDeTickets() {
        try {
            startActivity(new Intent(this, TicketsViewerActivity.class));
        } catch (Exception e) {
            Toast.makeText(this, "Visor de tickets no disponible", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error abriendo TicketsViewerActivity: " + e.getMessage());
        }
    }
}