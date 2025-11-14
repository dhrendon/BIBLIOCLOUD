package com.example.bibliocloud;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.bibliocloud.models.PurchaseOrder;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.*;
import java.util.*;

public class AdminPurchasesActivity extends AppCompatActivity {

    private Spinner spinnerStatusFilter;
    private LinearLayout layoutPurchasesList;
    private TextView tvTotalOrders, tvTotalRevenue, tvPendingOrders;
    private MaterialButton btnBack;

    private FirebaseFirestore db;
    private List<PurchaseOrder> allOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_purchases);

        db = FirebaseFirestore.getInstance();
        initializeViews();
        setupStatusFilter();
        loadOrders();
    }

    private void initializeViews() {
        spinnerStatusFilter = findViewById(R.id.spinnerStatusFilter);
        layoutPurchasesList = findViewById(R.id.layoutPurchasesList);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvPendingOrders = findViewById(R.id.tvPendingOrders);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        allOrders = new ArrayList<>();
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
                filterOrders(selectedStatus);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadOrders() {
        db.collection("compras")
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        Toast.makeText(this, "Error al cargar órdenes", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allOrders.clear();
                    double totalRevenue = 0;
                    int pendingCount = 0;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        PurchaseOrder order = doc.toObject(PurchaseOrder.class);
                        if (order != null) {
                            order.setId(doc.getId());
                            allOrders.add(order);
                            totalRevenue += order.getTotal();

                            if (order.getStatus().equals("Pendiente")) {
                                pendingCount++;
                            }
                        }
                    }

                    updateStatistics(allOrders.size(), totalRevenue, pendingCount);
                    displayOrders(allOrders);
                });
    }

    private void filterOrders(String status) {
        if (status.equals("Todas")) {
            displayOrders(allOrders);
        } else {
            List<PurchaseOrder> filtered = new ArrayList<>();
            for (PurchaseOrder order : allOrders) {
                if (order.getStatus().equals(status)) {
                    filtered.add(order);
                }
            }
            displayOrders(filtered);
        }
    }

    private void displayOrders(List<PurchaseOrder> orders) {
        layoutPurchasesList.removeAllViews();

        if (orders.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No hay órdenes con este estado");
            tvEmpty.setPadding(16, 32, 16, 16);
            layoutPurchasesList.addView(tvEmpty);
            return;
        }

        for (PurchaseOrder order : orders) {
            layoutPurchasesList.addView(createOrderCard(order));
        }
    }

    private CardView createOrderCard(PurchaseOrder order) {
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

        // ID de orden
        TextView tvOrderId = new TextView(this);
        tvOrderId.setText("📋 Orden: " + order.getId().substring(0, 8).toUpperCase());
        tvOrderId.setTextSize(16);
        tvOrderId.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvOrderId);

        // Cliente
        TextView tvCustomer = new TextView(this);
        tvCustomer.setText("👤 Cliente: " + order.getUserName() + "\n" +
                "   (" + order.getUserEmail() + ")");
        layout.addView(tvCustomer);

        // Fecha
        TextView tvDate = new TextView(this);
        tvDate.setText("📅 " + order.getFormattedOrderDate());
        layout.addView(tvDate);

        // Items
        TextView tvItems = new TextView(this);
        tvItems.setText("📦 Items: " + order.getTotalItems());
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

        // Método de pago
        TextView tvPayment = new TextView(this);
        tvPayment.setText("💳 Pago: " + order.getPaymentMethod() +
                (order.isPaid() ? " ✅ Pagado" : " ⏳ Pendiente"));
        layout.addView(tvPayment);

        // Botones de acción
        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setPadding(0, 12, 0, 0);

        Button btnDetails = new Button(this);
        btnDetails.setText("Ver Detalles");
        btnDetails.setOnClickListener(v -> showOrderDetails(order));

        Button btnUpdateStatus = new Button(this);
        btnUpdateStatus.setText("Actualizar Estado");
        btnUpdateStatus.setOnClickListener(v -> showUpdateStatusDialog(order));

        buttonsLayout.addView(btnDetails);
        buttonsLayout.addView(btnUpdateStatus);
        layout.addView(buttonsLayout);

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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Detalles de la Orden");

        StringBuilder details = new StringBuilder();
        details.append("Orden: ").append(order.getId()).append("\n\n");
        details.append("Cliente:\n");
        details.append("  Nombre: ").append(order.getUserName()).append("\n");
        details.append("  Email: ").append(order.getUserEmail()).append("\n\n");

        details.append("Libros:\n");
        for (PurchaseOrder.PurchaseItem item : order.getItems()) {
            details.append("  • ").append(item.getBookTitle()).append("\n");
            details.append("    Autor: ").append(item.getBookAuthor()).append("\n");
            details.append("    Sucursal: ").append(item.getBranchName()).append("\n");
            details.append("    Cantidad: ").append(item.getQuantity()).append("\n");
            details.append("    Precio: $").append(item.getUnitPrice()).append("\n\n");
        }

        details.append("Costos:\n");
        details.append("  Subtotal: $").append(String.format("%.2f", order.getSubtotal())).append("\n");
        details.append("  IVA: $").append(String.format("%.2f", order.getTax())).append("\n");
        details.append("  Total: $").append(String.format("%.2f", order.getTotal())).append("\n\n");

        details.append("Pago: ").append(order.getPaymentMethod());
        details.append(order.isPaid() ? " (Pagado)" : " (Pendiente)").append("\n\n");

        details.append("Dirección de Envío:\n");
        details.append(order.getFullShippingAddress()).append("\n");
        details.append("Teléfono: ").append(order.getShippingPhone()).append("\n\n");

        details.append("Estado: ").append(order.getStatus()).append("\n");
        details.append("Fecha: ").append(order.getFormattedOrderDate()).append("\n");
        details.append("Entrega estimada: ").append(order.getFormattedEstimatedDelivery());

        builder.setMessage(details.toString());
        builder.setPositiveButton("Cerrar", null);
        builder.show();
    }

    private void showUpdateStatusDialog(PurchaseOrder order) {
        String[] statuses = {"Pendiente", "Procesando", "Enviado", "Entregado", "Cancelado"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Actualizar Estado de la Orden");
        builder.setItems(statuses, (dialog, which) -> {
            String newStatus = statuses[which];
            updateOrderStatus(order.getId(), newStatus);
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void updateOrderStatus(String orderId, String newStatus) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);

        // Si se marca como enviado, generar número de rastreo
        if (newStatus.equals("Enviado")) {
            String trackingNumber = "BCL" + System.currentTimeMillis();
            updates.put("trackingNumber", trackingNumber);
        }

        // Si se marca como pagado
        if (newStatus.equals("Procesando") || newStatus.equals("Enviado")) {
            updates.put("paid", true);
        }

        db.collection("compras").document(orderId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Estado actualizado a: " + newStatus,
                            Toast.LENGTH_SHORT).show();
                    loadOrders();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void updateStatistics(int totalOrders, double totalRevenue, int pendingOrders) {
        tvTotalOrders.setText("Total de órdenes: " + totalOrders);
        tvTotalRevenue.setText(String.format("Ingresos totales: $%.2f MXN", totalRevenue));
        tvPendingOrders.setText("Órdenes pendientes: " + pendingOrders);
    }
}