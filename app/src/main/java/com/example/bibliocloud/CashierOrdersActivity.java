package com.example.bibliocloud;

import android.os.Bundle;
import android.util.Log;
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

public class CashierOrdersActivity extends AppCompatActivity {

    private static final String TAG = "CashierOrders";

    private Spinner spinnerStatusFilter;
    private EditText etSearchOrder;
    private LinearLayout layoutOrdersList;
    private TextView tvTotalOrders, tvBranchInfo;
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

        Log.d(TAG, "🏢 Sucursal del cajero:");
        Log.d(TAG, "   - ID: " + branchId);
        Log.d(TAG, "   - Nombre: " + branchName);

        // ✅ Validar que tenga sucursal asignada
        if (branchName == null || branchName.isEmpty()) {
            Toast.makeText(this, "❌ Error: No hay sucursal asignada", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

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
        tvBranchInfo = findViewById(R.id.tvBranchInfo);
        btnBack = findViewById(R.id.btnBack);
        btnSearch = findViewById(R.id.btnSearch);

        allOrders = new ArrayList<>();

        // Mostrar información de la sucursal
        if (tvBranchInfo != null) {
            tvBranchInfo.setText("🏢 Sucursal: " + branchName);
        }
    }

    private void setupStatusFilter() {
        String[] statuses = {"Todos", "Pendiente", "Procesando", "Completado"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatusFilter.setAdapter(adapter);

        spinnerStatusFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedStatus = (String) parent.getItemAtPosition(position);
                if ("Todos".equals(selectedStatus)) {
                    displayOrders(allOrders);
                } else {
                    filterOrders(selectedStatus);
                }
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
        Log.d(TAG, "🔍 Cargando órdenes para sucursal: " + branchName);

        // ✅ CONSULTA FILTRADA: Solo órdenes de esta sucursal
        db.collection("compras")
                .whereEqualTo("branchName", branchName)  // ← FILTRO PRINCIPAL
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Error al cargar órdenes: " + error.getMessage());
                        Toast.makeText(this, "Error al cargar órdenes", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null) {
                        Log.w(TAG, "⚠️ Snapshots es null");
                        return;
                    }

                    allOrders.clear();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        PurchaseOrder order = doc.toObject(PurchaseOrder.class);
                        if (order != null) {
                            order.setId(doc.getId());

                            // ✅ Validación adicional por items
                            if (validateOrderForBranch(order)) {
                                allOrders.add(order);
                                Log.d(TAG, "📦 Orden agregada: " + order.getId());
                            }
                        }
                    }

                    Log.d(TAG, "✅ Total órdenes cargadas: " + allOrders.size());
                    tvTotalOrders.setText("Total de órdenes: " + allOrders.size());
                    displayOrders(allOrders);
                });
    }

    // ✅ Validar que la orden pertenezca a esta sucursal
    private boolean validateOrderForBranch(PurchaseOrder order) {
        // Si no hay items, aceptar por el branchName general
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return true;
        }

        // Verificar que al menos un item pertenezca a esta sucursal
        for (PurchaseOrder.PurchaseItem item : order.getItems()) {
            if (branchId != null && branchId.equals(item.getBranchId())) {
                return true;
            }
            if (branchName != null && branchName.equals(item.getBranchName())) {
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
        Log.d(TAG, "🔍 Filtrado por estado '" + status + "': " + filtered.size() + " órdenes");
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

        Log.d(TAG, "🔍 Búsqueda '" + searchText + "': " + filtered.size() + " resultados");
        displayOrders(filtered);
    }

    private void displayOrders(List<PurchaseOrder> orders) {
        layoutOrdersList.removeAllViews();

        if (orders.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No hay órdenes para esta sucursal");
            tvEmpty.setPadding(16, 32, 16, 16);
            tvEmpty.setTextSize(16);
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            tvEmpty.setTextColor(getResources().getColor(R.color.colorTextSecondary));
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
        tvOrderId.setText("📋 Orden: #" + order.getId().substring(0, Math.min(8, order.getId().length())).toUpperCase());
        tvOrderId.setTextSize(18);
        tvOrderId.setTypeface(null, android.graphics.Typeface.BOLD);
        tvOrderId.setTextColor(getResources().getColor(R.color.colorPrimary));
        layout.addView(tvOrderId);

        // Cliente
        TextView tvCustomer = new TextView(this);
        tvCustomer.setText("👤 " + order.getUserName() + " (" + order.getUserEmail() + ")");
        tvCustomer.setTextSize(14);
        tvCustomer.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        layout.addView(tvCustomer);

        // Fecha
        TextView tvDate = new TextView(this);
        tvDate.setText("📅 " + order.getFormattedOrderDate());
        tvDate.setTextSize(14);
        tvDate.setTextColor(getResources().getColor(R.color.colorTextSecondary));
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
        tvItems.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        layout.addView(tvItems);

        // Total
        TextView tvTotal = new TextView(this);
        tvTotal.setText(String.format(Locale.getDefault(), "💰 Total: $%.2f MXN", order.getTotal()));
        tvTotal.setTextSize(18);
        tvTotal.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTotal.setTextColor(getResources().getColor(R.color.green));
        layout.addView(tvTotal);

        // Método de pago
        TextView tvPayment = new TextView(this);
        tvPayment.setText("💳 " + order.getPaymentMethod());
        tvPayment.setTextSize(14);
        tvPayment.setTextColor(getResources().getColor(R.color.colorTextSecondary));
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
            btnCharge.setTextColor(getResources().getColor(R.color.white));
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
        details.append("Email: ").append(order.getUserEmail()).append("\n");
        details.append("Sucursal: ").append(branchName).append("\n\n");

        details.append("Libros:\n");
        for (PurchaseOrder.PurchaseItem item : order.getItems()) {
            details.append("  • ").append(item.getBookTitle()).append("\n");
            details.append("    Cantidad: ").append(item.getQuantity()).append("\n");
            details.append("    Precio: $").append(String.format("%.2f", item.getUnitPrice())).append("\n\n");
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
        builder.setMessage(String.format(Locale.getDefault(),
                "¿Confirmar el cobro de esta orden?\n\n" +
                        "Cliente: %s\n" +
                        "Total: $%.2f MXN\n" +
                        "Método: %s\n" +
                        "Sucursal: %s",
                order.getUserName(),
                order.getTotal(),
                order.getPaymentMethod(),
                branchName
        ));

        builder.setPositiveButton("✅ Confirmar Cobro", (dialog, which) -> {
            processPayment(order);
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void processPayment(PurchaseOrder order) {
        Log.d(TAG, "💳 Procesando pago de orden: " + order.getId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("paid", true);
        updates.put("status", "Completado");
        updates.put("paymentDate", new Date());

        db.collection("compras").document(order.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Orden actualizada a Completado");

                    // Registrar el pago
                    registerPayment(order);

                    Toast.makeText(this, "✅ Pago registrado exitosamente", Toast.LENGTH_LONG).show();

                    // Actualizar inventario
                    updateInventoryForOrder(order);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al procesar pago: " + e.getMessage());
                    Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void registerPayment(PurchaseOrder order) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String cashierId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String cashierName = prefs.getString("current_user_name", "Cajero");

        StringBuilder bookTitles = new StringBuilder();
        for (PurchaseOrder.PurchaseItem item : order.getItems()) {
            bookTitles.append("• ").append(item.getBookTitle())
                    .append(" (x").append(item.getQuantity()).append(")\n");
        }

        // ✅ Crear Map con todos los datos del pago
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("orden_id", order.getId());
        paymentData.put("user_id", order.getUserId());
        paymentData.put("user_name", order.getUserName());
        paymentData.put("user_email", order.getUserEmail());
        paymentData.put("cajero_id", cashierId);
        paymentData.put("cajero_name", cashierName);
        paymentData.put("sucursal_id", branchId);
        paymentData.put("nombre_sucursal", branchName);
        paymentData.put("metodo_pago", order.getPaymentMethod());
        paymentData.put("monto", order.getTotal());
        paymentData.put("subtotal", order.getSubtotal());
        paymentData.put("iva", order.getTax());
        paymentData.put("libros", bookTitles.toString());
        paymentData.put("estado", "Completado");
        paymentData.put("fecha_pago", new Date());

        // Formatos de fecha
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        Date now = new Date();

        paymentData.put("formattedDate", dateFormat.format(now));
        paymentData.put("formattedTime", timeFormat.format(now));
        paymentData.put("numero_ticket", "TKT-" + System.currentTimeMillis());

        Log.d(TAG, "💾 Guardando pago en Firestore");
        Log.d(TAG, "   - Sucursal: " + branchName);
        Log.d(TAG, "   - Cajero: " + cashierName);
        Log.d(TAG, "   - Monto: $" + order.getTotal());

        db.collection("pagos")
                .add(paymentData)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "✅ Pago registrado con ID: " + docRef.getId());
                    showPaymentTicket(paymentData);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al registrar pago: " + e.getMessage());
                    Toast.makeText(this, "Error al registrar historial de pago", Toast.LENGTH_SHORT).show();
                });
    }

    private void showPaymentTicket(Map<String, Object> paymentData) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✅ Pago Completado");

        StringBuilder ticket = new StringBuilder();
        ticket.append("═══════════════════════════\n");
        ticket.append("       BIBLIOCLOUD\n");
        ticket.append("       TICKET DE PAGO\n");
        ticket.append("═══════════════════════════\n\n");
        ticket.append("Sucursal: ").append(paymentData.get("nombre_sucursal")).append("\n");
        ticket.append("Ticket: #").append(paymentData.get("numero_ticket")).append("\n");
        ticket.append("Fecha: ").append(paymentData.get("formattedDate")).append("\n");
        ticket.append("Hora: ").append(paymentData.get("formattedTime")).append("\n\n");
        ticket.append("───────────────────────────\n");
        ticket.append("Cliente: ").append(paymentData.get("user_name")).append("\n");
        ticket.append("Email: ").append(paymentData.get("user_email")).append("\n\n");
        ticket.append("───────────────────────────\n");
        ticket.append("LIBROS COMPRADOS:\n");
        ticket.append(paymentData.get("libros")).append("\n");
        ticket.append("───────────────────────────\n");
        ticket.append("Subtotal: $").append(String.format("%.2f", paymentData.get("subtotal"))).append("\n");
        ticket.append("IVA (16%): $").append(String.format("%.2f", paymentData.get("iva"))).append("\n");
        ticket.append("TOTAL: $").append(String.format("%.2f", paymentData.get("monto"))).append("\n\n");
        ticket.append("───────────────────────────\n");
        ticket.append("Método: ").append(paymentData.get("metodo_pago")).append("\n");
        ticket.append("Atendió: ").append(paymentData.get("cajero_name")).append("\n\n");
        ticket.append("═══════════════════════════\n");
        ticket.append("   ¡Gracias por su compra!\n");
        ticket.append("      Vuelva pronto\n");
        ticket.append("═══════════════════════════\n");

        builder.setMessage(ticket.toString());
        builder.setPositiveButton("Cerrar", null);
        builder.show();
    }

    private void updateInventoryForOrder(PurchaseOrder order) {
        Log.d(TAG, "📦 Actualizando inventario para orden: " + order.getId());

        for (PurchaseOrder.PurchaseItem item : order.getItems()) {
            db.collection("inventario")
                    .whereEqualTo("bookId", item.getBookId())
                    .whereEqualTo("branchId", branchId)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        if (!snapshots.isEmpty()) {
                            DocumentSnapshot doc = snapshots.getDocuments().get(0);
                            Long currentStock = doc.getLong("availablePhysical");

                            if (currentStock != null && currentStock > 0) {
                                int newStock = currentStock.intValue() - item.getQuantity();
                                db.collection("inventario").document(doc.getId())
                                        .update("availablePhysical", Math.max(0, newStock))
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "✅ Inventario actualizado: " + item.getBookTitle());
                                        });
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Error al actualizar inventario: " + e.getMessage());
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "📱 onResume() - Recargando órdenes");
        loadOrders();
    }
}