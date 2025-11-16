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
import java.text.SimpleDateFormat;
import java.util.*;
import android.content.SharedPreferences;
import com.example.bibliocloud.models.Payment;
import com.google.firebase.auth.FirebaseAuth;
import androidx.appcompat.app.AlertDialog;

public class CashierOrdersActivity extends AppCompatActivity {

    private Spinner spinnerStatusFilter;
    private EditText etSearchOrder;
    private LinearLayout layoutOrdersList;
    private TextView tvTotalOrders;
    private MaterialButton btnBack, btnSearch;

    private FirebaseFirestore db;
    private List<PurchaseOrder> allOrders;
    private String branchId;
    private String branchName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cashier_orders);

        db = FirebaseFirestore.getInstance();
        branchId = getIntent().getStringExtra("branchId");
        branchName = getIntent().getStringExtra("branchName");

        initializeViews();
        setupStatusFilter();
        setupListeners();
        loadOrders();
    }

    private void initializeViews() {
        spinnerStatusFilter = findViewById(R.id.spinnerStatusFilter);
        etSearchOrder = findViewById(R.id.etSearchOrder);
        layoutOrdersList = findViewById(R.id.layoutOrdersList);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        btnBack = findViewById(R.id.btnBack);
        btnSearch = findViewById(R.id.btnSearch);

        allOrders = new ArrayList<>();
    }

    private void setupStatusFilter() {
        String[] statuses = {"Pendiente", "Procesando", "Completado"};
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

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSearch.setOnClickListener(v -> {
            String searchText = etSearchOrder.getText().toString().trim();
            searchOrder(searchText);
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

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        PurchaseOrder order = doc.toObject(PurchaseOrder.class);
                        if (order != null) {
                            order.setId(doc.getId());

                            // Filtrar por sucursal si aplica
                            if (branchId.isEmpty() || orderBelongsToBranch(order, branchId)) {
                                allOrders.add(order);
                            }
                        }
                    }

                    tvTotalOrders.setText("Total de órdenes: " + allOrders.size());
                    displayOrders(allOrders);
                });
    }

    private boolean orderBelongsToBranch(PurchaseOrder order, String branchId) {
        // Verificar si algún item de la orden pertenece a esta sucursal
        for (PurchaseOrder.PurchaseItem item : order.getItems()) {
            if (branchId.equals(item.getBranchId())) {
                return true;
            }
        }
        return false;
    }

    private void filterOrders(String status) {
        List<PurchaseOrder> filtered = new ArrayList<>();
        for (PurchaseOrder order : allOrders) {
            if (order.getStatus().equals(status)) {
                filtered.add(order);
            }
        }
        displayOrders(filtered);
    }

    private void searchOrder(String searchText) {
        if (searchText.isEmpty()) {
            displayOrders(allOrders);
            return;
        }

        List<PurchaseOrder> filtered = new ArrayList<>();
        searchText = searchText.toLowerCase();

        for (PurchaseOrder order : allOrders) {
            if (order.getId().toLowerCase().contains(searchText) ||
                    order.getUserName().toLowerCase().contains(searchText) ||
                    order.getUserEmail().toLowerCase().contains(searchText)) {
                filtered.add(order);
            }
        }

        displayOrders(filtered);
    }

    private void displayOrders(List<PurchaseOrder> orders) {
        layoutOrdersList.removeAllViews();

        if (orders.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No hay órdenes pendientes");
            tvEmpty.setPadding(16, 32, 16, 16);
            tvEmpty.setTextSize(16);
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            layoutOrdersList.addView(tvEmpty);
            return;
        }

        for (PurchaseOrder order : orders) {
            layoutOrdersList.addView(createOrderCard(order));
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

        int backgroundColor = order.isPaid() ?
                getResources().getColor(R.color.light_brown) :
                getResources().getColor(R.color.white);
        card.setCardBackgroundColor(backgroundColor);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        // ID de orden
        TextView tvOrderId = new TextView(this);
        tvOrderId.setText("📋 Orden: #" + order.getId().substring(0, 8).toUpperCase());
        tvOrderId.setTextSize(18);
        tvOrderId.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvOrderId);

        // Cliente
        TextView tvCustomer = new TextView(this);
        tvCustomer.setText("👤 " + order.getUserName() + " (" + order.getUserEmail() + ")");
        tvCustomer.setTextSize(14);
        layout.addView(tvCustomer);

        // Fecha
        TextView tvDate = new TextView(this);
        tvDate.setText("📅 " + order.getFormattedOrderDate());
        tvDate.setTextSize(14);
        layout.addView(tvDate);

        // Items
        TextView tvItems = new TextView(this);
        StringBuilder itemsText = new StringBuilder("📚 Libros:\n");
        for (PurchaseOrder.PurchaseItem item : order.getItems()) {
            itemsText.append("  • ").append(item.getBookTitle())
                    .append(" (x").append(item.getQuantity()).append(")\n");
        }
        tvItems.setText(itemsText.toString());
        tvItems.setTextSize(14);
        layout.addView(tvItems);

        // Total
        TextView tvTotal = new TextView(this);
        tvTotal.setText(String.format("💰 Total: $%.2f MXN", order.getTotal()));
        tvTotal.setTextSize(18);
        tvTotal.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTotal.setTextColor(getResources().getColor(R.color.colorPrimary));
        layout.addView(tvTotal);

        // Método de pago
        TextView tvPayment = new TextView(this);
        tvPayment.setText("💳 " + order.getPaymentMethod());
        tvPayment.setTextSize(14);
        layout.addView(tvPayment);

        // Estado de pago
        TextView tvPaymentStatus = new TextView(this);
        if (order.isPaid()) {
            tvPaymentStatus.setText("✅ PAGADO");
            tvPaymentStatus.setTextColor(getResources().getColor(R.color.green));
        } else {
            tvPaymentStatus.setText("⏳ PENDIENTE DE PAGO");
            tvPaymentStatus.setTextColor(getResources().getColor(R.color.orange));
        }
        tvPaymentStatus.setTextSize(16);
        tvPaymentStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvPaymentStatus);

        // Botones
        if (!order.isPaid() && order.getStatus().equals("Pendiente")) {
            LinearLayout buttonsLayout = new LinearLayout(this);
            buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonsLayout.setPadding(0, 12, 0, 0);

            Button btnDetails = new Button(this);
            btnDetails.setText("Ver Detalles");
            btnDetails.setOnClickListener(v -> showOrderDetails(order));

            Button btnCharge = new Button(this);
            btnCharge.setText("💰 Cobrar");
            btnCharge.setBackgroundTintList(getResources().getColorStateList(R.color.green));
            btnCharge.setOnClickListener(v -> showChargeDialog(order));

            buttonsLayout.addView(btnDetails);
            buttonsLayout.addView(btnCharge);
            layout.addView(buttonsLayout);
        } else {
            Button btnDetails = new Button(this);
            btnDetails.setText("Ver Detalles");
            btnDetails.setOnClickListener(v -> showOrderDetails(order));
            layout.addView(btnDetails);
        }

        card.addView(layout);
        return card;
    }

    private void showOrderDetails(PurchaseOrder order) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Detalles de la Orden");

        StringBuilder details = new StringBuilder();
        details.append("Orden: #").append(order.getId()).append("\n\n");
        details.append("Cliente: ").append(order.getUserName()).append("\n");
        details.append("Email: ").append(order.getUserEmail()).append("\n\n");

        details.append("Libros:\n");
        for (PurchaseOrder.PurchaseItem item : order.getItems()) {
            details.append("  • ").append(item.getBookTitle()).append("\n");
            details.append("    Cantidad: ").append(item.getQuantity()).append("\n");
            details.append("    Precio: $").append(item.getUnitPrice()).append("\n\n");
        }

        details.append("Subtotal: $").append(String.format("%.2f", order.getSubtotal())).append("\n");
        details.append("IVA: $").append(String.format("%.2f", order.getTax())).append("\n");
        details.append("Total: $").append(String.format("%.2f", order.getTotal())).append("\n\n");

        details.append("Método de Pago: ").append(order.getPaymentMethod()).append("\n");
        details.append("Estado: ").append(order.isPaid() ? "PAGADO" : "PENDIENTE").append("\n\n");

        details.append("Dirección:\n").append(order.getFullShippingAddress());

        builder.setMessage(details.toString());
        builder.setPositiveButton("Cerrar", null);
        builder.show();
    }

    private void showChargeDialog(PurchaseOrder order) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmar Cobro");
        builder.setMessage(String.format(
                "¿Confirmar el cobro de esta orden?\n\n" +
                        "Cliente: %s\n" +
                        "Total: $%.2f MXN\n" +
                        "Método: %s",
                order.getUserName(),
                order.getTotal(),
                order.getPaymentMethod()
        ));

        builder.setPositiveButton("✅ Confirmar Cobro", (dialog, which) -> {
            processPayment(order);
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void processPayment(PurchaseOrder order) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("paid", true);
        updates.put("status", "Procesando");
        updates.put("paymentDate", new Date());

        db.collection("compras").document(order.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Registrar el pago en la colección "pagos"
                    registerPayment(order);

                    Toast.makeText(this,
                            "✅ Pago registrado exitosamente",
                            Toast.LENGTH_LONG).show();

                    // Actualizar inventario
                    updateInventoryForOrder(order);
                    loadOrders();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "❌ Error al registrar el pago: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void registerPayment(PurchaseOrder order) {
        // Obtener información del cajero actual
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String cashierId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String cashierName = prefs.getString("current_user_name", "Cajero");

        // Construir lista de libros
        StringBuilder bookTitles = new StringBuilder();
        for (PurchaseOrder.PurchaseItem item : order.getItems()) {
            bookTitles.append("• ").append(item.getBookTitle())
                    .append(" (x").append(item.getQuantity()).append(")\n");
        }

        // Crear objeto Payment usando el constructor de 13 parámetros
        Payment payment = new Payment(
                order.getId(),              // orderId
                order.getUserId(),          // userId
                order.getUserName(),        // userName
                order.getUserEmail(),       // userEmail
                cashierId,                  // cashierId
                cashierName,                // cashierName
                branchId,                   // branchId
                branchName,                 // branchName
                order.getPaymentMethod(),   // paymentMethod
                order.getTotal(),           // total (amount)
                order.getSubtotal(),        // subtotal
                order.getTax(),             // tax
                bookTitles.toString()       // bookTitles
        );

        // 🔥 IMPORTANTE: Crear un Map con TODOS los campos en español
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("id_orden", order.getId());
        paymentData.put("id_usuario", order.getUserId());
        paymentData.put("nombre_usuario", order.getUserName());
        paymentData.put("correo_usuario", order.getUserEmail());
        paymentData.put("id_cajero", cashierId);
        paymentData.put("nombre_cajero", cashierName);
        paymentData.put("id_sucursal", branchId);
        paymentData.put("nombre_sucursal", branchName);
        paymentData.put("numero_ticket", payment.getTicketNumber());
        paymentData.put("metodo_pago", order.getPaymentMethod());
        paymentData.put("monto", order.getTotal());
        paymentData.put("subtotal", order.getSubtotal());
        paymentData.put("iva", order.getTax());
        paymentData.put("libros", bookTitles.toString());
        paymentData.put("fecha_pago", new Date());
        paymentData.put("estado", "Completado");
        paymentData.put("timestamp", System.currentTimeMillis());

        // 🔥 AGREGAR CAMPOS FORMATEADOS PARA CONSULTAS
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        Date now = new Date();

        paymentData.put("formattedDate", dateFormat.format(now));
        paymentData.put("formattedTime", timeFormat.format(now));
        paymentData.put("formattedDateTime", dateFormat.format(now) + " " + timeFormat.format(now));

        // Guardar en Firestore usando el Map
        db.collection("pagos")
                .add(paymentData)
                .addOnSuccessListener(docRef -> {
                    // Actualizar el payment con el ID generado
                    payment.setId(docRef.getId());

                    // Mostrar ticket
                    showPaymentTicket(payment);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Error al registrar el pago en historial: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showPaymentTicket(Payment payment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✅ Pago Completado");

        StringBuilder ticket = new StringBuilder();
        ticket.append("═══════════════════════════\n");
        ticket.append("       BIBLIOCLOUD\n");
        ticket.append("       TICKET DE PAGO\n");
        ticket.append("═══════════════════════════\n\n");
        ticket.append("Sucursal: ").append(branchName).append("\n");
        ticket.append("Ticket: #").append(payment.getTicketNumber()).append("\n");
        ticket.append("Fecha: ").append(payment.getFormattedDate()).append("\n");
        ticket.append("Hora: ").append(payment.getFormattedTime()).append("\n\n");
        ticket.append("───────────────────────────\n");
        ticket.append("Cliente: ").append(payment.getUserName()).append("\n");
        ticket.append("Email: ").append(payment.getUserEmail()).append("\n\n");
        ticket.append("───────────────────────────\n");
        ticket.append("LIBROS COMPRADOS:\n");
        ticket.append(payment.getBookTitles()).append("\n");
        ticket.append("───────────────────────────\n");
        ticket.append("Subtotal: $").append(String.format("%.2f", payment.getSubtotal())).append("\n");
        ticket.append("IVA (16%): $").append(String.format("%.2f", payment.getTax())).append("\n");
        ticket.append("TOTAL: $").append(String.format("%.2f", payment.getAmount())).append("\n\n");
        ticket.append("───────────────────────────\n");
        ticket.append("Método de pago: ").append(payment.getPaymentMethod()).append("\n");
        ticket.append("Atendió: ").append(payment.getCashierName()).append("\n\n");
        ticket.append("═══════════════════════════\n");
        ticket.append("   ¡Gracias por su compra!\n");
        ticket.append("      Vuelva pronto\n");
        ticket.append("═══════════════════════════\n");

        builder.setMessage(ticket.toString());
        builder.setPositiveButton("Cerrar", null);
        builder.setNeutralButton("Compartir", (dialog, which) -> {
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, ticket.toString());
            startActivity(android.content.Intent.createChooser(shareIntent, "Compartir ticket"));
        });
        builder.show();
    }

    private void updateInventoryForOrder(PurchaseOrder order) {
        for (PurchaseOrder.PurchaseItem item : order.getItems()) {
            // Buscar el inventario correspondiente
            db.collection("inventario")
                    .whereEqualTo("bookId", item.getBookId())
                    .whereEqualTo("branchId", item.getBranchId())
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        if (!snapshots.isEmpty()) {
                            DocumentSnapshot doc = snapshots.getDocuments().get(0);
                            Long currentStock = doc.getLong("availablePhysical");

                            if (currentStock != null && currentStock > 0) {
                                int newStock = currentStock.intValue() - item.getQuantity();
                                db.collection("inventario").document(doc.getId())
                                        .update("availablePhysical", Math.max(0, newStock));
                            }
                        }
                    });
        }
    }
}