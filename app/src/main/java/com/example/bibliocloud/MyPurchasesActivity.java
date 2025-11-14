package com.example.bibliocloud;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import com.example.bibliocloud.models.PurchaseOrder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MyPurchasesActivity extends AppCompatActivity {

    private LinearLayout layoutPurchasesList;
    private TextView tvTotalPurchases, tvTotalSpent;
    private Spinner spinnerStatusFilter;
    private LinearLayout layoutEmpty;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<PurchaseOrder> purchasesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_purchases);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initializeViews();
        setupToolbar();
        setupStatusFilter();
        loadPurchases();
    }

    private void initializeViews() {
        layoutPurchasesList = findViewById(R.id.layoutPurchasesList);
        tvTotalPurchases = findViewById(R.id.tvTotalPurchases);
        tvTotalSpent = findViewById(R.id.tvTotalSpent);
        spinnerStatusFilter = findViewById(R.id.spinnerStatusFilter);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        purchasesList = new ArrayList<>();
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

    private void setupStatusFilter() {
        String[] statuses = {"Todas", "Pendiente", "Procesando", "Enviado", "Entregado", "Cancelado"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatusFilter.setAdapter(adapter);

        spinnerStatusFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedStatus = (String) parent.getItemAtPosition(position);
                filterPurchases(selectedStatus);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadPurchases() {
        String userId = auth.getCurrentUser().getUid();

        db.collection("compras")
                .whereEqualTo("userId", userId)
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error al cargar compras", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    purchasesList.clear();
                    double totalSpent = 0;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        PurchaseOrder order = doc.toObject(PurchaseOrder.class);
                        if (order != null) {
                            order.setId(doc.getId());
                            purchasesList.add(order);
                            totalSpent += order.getTotal();
                        }
                    }

                    updateStatistics(purchasesList.size(), totalSpent);
                    displayPurchases(purchasesList);
                });
    }

    private void filterPurchases(String status) {
        if (status.equals("Todas")) {
            displayPurchases(purchasesList);
        } else {
            List<PurchaseOrder> filtered = new ArrayList<>();
            for (PurchaseOrder order : purchasesList) {
                if (order.getStatus().equals(status)) {
                    filtered.add(order);
                }
            }
            displayPurchases(filtered);
        }
    }

    private void displayPurchases(List<PurchaseOrder> orders) {
        layoutPurchasesList.removeAllViews();

        if (orders.isEmpty()) {
            showEmptyState();
            return;
        }

        layoutEmpty.setVisibility(View.GONE);

        for (PurchaseOrder order : orders) {
            layoutPurchasesList.addView(createPurchaseCard(order));
        }
    }

    private CardView createPurchaseCard(PurchaseOrder order) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setCardElevation(4);
        card.setRadius(8);
        card.setCardBackgroundColor(getResources().getColor(R.color.light_brown));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        // Número de orden
        TextView tvOrderNumber = new TextView(this);
        tvOrderNumber.setText("📋 Orden: " + order.getId().substring(0, 8).toUpperCase());
        tvOrderNumber.setTextSize(16);
        tvOrderNumber.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvOrderNumber);

        // Fecha
        TextView tvDate = new TextView(this);
        tvDate.setText("📅 " + order.getFormattedOrderDate());
        layout.addView(tvDate);

        // Items
        TextView tvItems = new TextView(this);
        StringBuilder itemsText = new StringBuilder("📚 Libros:\n");
        for (PurchaseOrder.PurchaseItem item : order.getItems()) {
            itemsText.append("  • ").append(item.getBookTitle())
                    .append(" (x").append(item.getQuantity()).append(")\n");
        }
        tvItems.setText(itemsText.toString());
        layout.addView(tvItems);

        // Total
        TextView tvTotal = new TextView(this);
        tvTotal.setText(String.format("💰 Total: $%.2f MXN", order.getTotal()));
        tvTotal.setTextSize(15);
        tvTotal.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvTotal);

        // Estado
        TextView tvStatus = new TextView(this);
        tvStatus.setText(getStatusEmoji(order.getStatus()) + " " + order.getStatus());
        tvStatus.setTextColor(getStatusColor(order.getStatus()));
        tvStatus.setTextSize(14);
        tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvStatus);

        // Tracking (si existe)
        if (order.getTrackingNumber() != null && !order.getTrackingNumber().isEmpty()) {
            TextView tvTracking = new TextView(this);
            tvTracking.setText("📦 Rastreo: " + order.getTrackingNumber());
            layout.addView(tvTracking);
        }

        // Fecha estimada de entrega
        TextView tvDelivery = new TextView(this);
        tvDelivery.setText("🚚 Entrega estimada: " + order.getFormattedEstimatedDelivery());
        layout.addView(tvDelivery);

        // Botón de detalles
        Button btnDetails = new Button(this);
        btnDetails.setText("Ver Detalles");
        btnDetails.setOnClickListener(v -> showOrderDetails(order));
        layout.addView(btnDetails);

        card.addView(layout);
        return card;
    }

    private String getStatusEmoji(String status) {
        switch (status) {
            case "Pendiente": return "⏳";
            case "Procesando": return "⚙️";
            case "Enviado": return "📦";
            case "Entregado": return "✅";
            case "Cancelado": return "❌";
            default: return "📋";
        }
    }

    private int getStatusColor(String status) {
        switch (status) {
            case "Pendiente": return getResources().getColor(R.color.orange);
            case "Procesando": return getResources().getColor(R.color.colorPrimary);
            case "Enviado": return getResources().getColor(android.R.color.holo_blue_dark);
            case "Entregado": return getResources().getColor(R.color.green);
            case "Cancelado": return getResources().getColor(R.color.red);
            default: return getResources().getColor(R.color.colorTextSecondary);
        }
    }

    private void showOrderDetails(PurchaseOrder order) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Detalles de la Orden");

        StringBuilder details = new StringBuilder();
        details.append("Número de Orden: ").append(order.getId()).append("\n\n");
        details.append("Fecha: ").append(order.getFormattedOrderDate()).append("\n\n");

        details.append("Libros:\n");
        for (PurchaseOrder.PurchaseItem item : order.getItems()) {
            details.append("  • ").append(item.getBookTitle()).append("\n");
            details.append("    Autor: ").append(item.getBookAuthor()).append("\n");
            details.append("    Sucursal: ").append(item.getBranchName()).append("\n");
            details.append("    Cantidad: ").append(item.getQuantity()).append("\n");
            details.append("    Precio: $").append(item.getUnitPrice()).append("\n\n");
        }

        details.append("Subtotal: $").append(String.format("%.2f", order.getSubtotal())).append("\n");
        details.append("IVA: $").append(String.format("%.2f", order.getTax())).append("\n");
        details.append("Total: $").append(String.format("%.2f", order.getTotal())).append("\n\n");

        details.append("Método de Pago: ").append(order.getPaymentMethod()).append("\n\n");

        details.append("Dirección de Envío:\n");
        details.append(order.getFullShippingAddress()).append("\n");
        details.append("Teléfono: ").append(order.getShippingPhone()).append("\n\n");

        details.append("Estado: ").append(order.getStatus()).append("\n");
        details.append("Entrega estimada: ").append(order.getFormattedEstimatedDelivery());

        builder.setMessage(details.toString());
        builder.setPositiveButton("Cerrar", null);
        builder.show();
    }

    private void updateStatistics(int totalOrders, double totalSpent) {
        tvTotalPurchases.setText("Total de compras: " + totalOrders);
        tvTotalSpent.setText(String.format("Gasto total: $%.2f MXN", totalSpent));
    }

    private void showEmptyState() {
        layoutEmpty.setVisibility(View.VISIBLE);
        layoutPurchasesList.removeAllViews();

        TextView tvEmpty = new TextView(this);
        tvEmpty.setText("No tienes compras registradas\n\n" +
                "Explora nuestro catálogo y realiza tu primera compra");
        tvEmpty.setTextSize(16);
        tvEmpty.setPadding(32, 32, 32, 32);
        tvEmpty.setGravity(android.view.Gravity.CENTER);
        layoutPurchasesList.addView(tvEmpty);
    }
}