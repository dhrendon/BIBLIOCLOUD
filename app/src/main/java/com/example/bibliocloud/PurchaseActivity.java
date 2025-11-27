package com.example.bibliocloud;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.bibliocloud.models.PurchaseOrder;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class PurchaseActivity extends AppCompatActivity {

    private static final String TAG = "PurchaseActivity";

    private TextView tvBookTitle, tvPrice, tvBranch, tvSubtotal, tvTax, tvTotal, tvStockAvailable;
    private EditText etAddress, etCity, etState, etZipCode, etPhone;
    private RadioGroup rgPaymentMethod;
    private Spinner spinnerQuantity;
    private CheckBox cbTerms;
    private MaterialButton btnConfirmPurchase, btnCancel;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String bookId, inventoryId, bookTitle, bookAuthor, branchId, branchName;
    private double price;
    private int quantity = 1;
    private int availableStock = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        getIntentData();
        initializeViews();
        setupToolbar();
        loadAvailableStock();
        setupListeners();
        updatePriceDisplay();
        loadUserAddress();
    }

    private void getIntentData() {
        bookId = getIntent().getStringExtra("bookId");
        inventoryId = getIntent().getStringExtra("inventoryId");
        bookTitle = getIntent().getStringExtra("bookTitle");
        bookAuthor = getIntent().getStringExtra("bookAuthor");
        price = getIntent().getDoubleExtra("price", 0.0);
        branchId = getIntent().getStringExtra("branchId");
        branchName = getIntent().getStringExtra("branchName");
        availableStock = getIntent().getIntExtra("availableStock", 0);
    }

    private void initializeViews() {
        tvBookTitle = findViewById(R.id.tvBookTitle);
        tvPrice = findViewById(R.id.tvPrice);
        tvBranch = findViewById(R.id.tvBranch);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvTax = findViewById(R.id.tvTax);
        tvTotal = findViewById(R.id.tvTotal);
        tvStockAvailable = findViewById(R.id.tvStockAvailable);

        etAddress = findViewById(R.id.etAddress);
        etCity = findViewById(R.id.etCity);
        etState = findViewById(R.id.etState);
        etZipCode = findViewById(R.id.etZipCode);
        etPhone = findViewById(R.id.etPhone);

        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        spinnerQuantity = findViewById(R.id.spinnerQuantity);
        cbTerms = findViewById(R.id.cbTerms);

        btnConfirmPurchase = findViewById(R.id.btnConfirmPurchase);
        btnCancel = findViewById(R.id.btnCancel);

        tvBookTitle.setText(bookTitle);
        tvPrice.setText(String.format("Precio unitario: $%.2f MXN", price));
        tvBranch.setText("📍 Sucursal: " + branchName);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Finalizar Compra");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadAvailableStock() {
        if (inventoryId == null || inventoryId.isEmpty()) {
            Toast.makeText(this, "⚠️ Error: Inventario no encontrado", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db.collection("inventario")
                .document(inventoryId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "❌ Error al verificar stock", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Long stock = documentSnapshot.getLong("availablePhysical");
                        availableStock = stock != null ? stock.intValue() : 0;

                        updateStockDisplay();
                        setupQuantitySpinner();
                    } else {
                        Toast.makeText(this, "⚠️ Producto no disponible", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }

    private void updateStockDisplay() {
        if (tvStockAvailable != null) {
            if (availableStock > 0) {
                tvStockAvailable.setText(String.format("📦 Stock disponible: %d unidades", availableStock));
                tvStockAvailable.setTextColor(getResources().getColor(R.color.green));
                tvStockAvailable.setVisibility(View.VISIBLE);
            } else {
                tvStockAvailable.setText("❌ Sin stock disponible");
                tvStockAvailable.setTextColor(getResources().getColor(R.color.red));
                tvStockAvailable.setVisibility(View.VISIBLE);

                btnConfirmPurchase.setEnabled(false);
                btnConfirmPurchase.setText("Sin stock disponible");

                Toast.makeText(this,
                        "⚠️ Este producto ya no está disponible",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupQuantitySpinner() {
        if (spinnerQuantity == null) return;

        if (availableStock <= 0) {
            String[] noStock = {"0 - Sin stock"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    noStock
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerQuantity.setAdapter(adapter);
            spinnerQuantity.setEnabled(false);
            return;
        }

        int maxQuantity = Math.min(availableStock, 5);
        String[] quantities = new String[maxQuantity];

        for (int i = 0; i < maxQuantity; i++) {
            quantities[i] = String.valueOf(i + 1);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                quantities
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerQuantity.setAdapter(adapter);
        spinnerQuantity.setEnabled(true);

        spinnerQuantity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    quantity = Integer.parseInt(quantities[position]);
                    updatePriceDisplay();
                } catch (Exception e) {
                    quantity = 1;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (availableStock < 5) {
            Toast.makeText(this,
                    String.format("⚠️ Solo quedan %d unidades disponibles", availableStock),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setupListeners() {
        btnConfirmPurchase.setOnClickListener(v -> processPurchase());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void updatePriceDisplay() {
        double subtotal = price * quantity;
        double tax = subtotal * 0.16;
        double total = subtotal + tax;

        tvSubtotal.setText(String.format("Subtotal: $%.2f MXN", subtotal));
        tvTax.setText(String.format("IVA (16%%): $%.2f MXN", tax));
        tvTotal.setText(String.format("Total: $%.2f MXN", total));
    }

    private void loadUserAddress() {
        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        etPhone.setText(prefs.getString("phone", ""));
    }

    private void processPurchase() {
        if (quantity > availableStock) {
            new AlertDialog.Builder(this)
                    .setTitle("⚠️ Stock insuficiente")
                    .setMessage(String.format(
                            "Solo hay %d unidades disponibles.\n\n" +
                                    "Has seleccionado %d unidades.\n\n" +
                                    "Por favor, ajusta la cantidad.",
                            availableStock, quantity))
                    .setPositiveButton("Entendido", null)
                    .show();
            return;
        }

        if (availableStock <= 0) {
            Toast.makeText(this,
                    "❌ Este producto ya no está disponible",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!validateInputs()) return;

        String address = etAddress.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String state = etState.getText().toString().trim();
        String zipCode = etZipCode.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        int selectedPaymentId = rgPaymentMethod.getCheckedRadioButtonId();
        RadioButton rbPayment = findViewById(selectedPaymentId);
        String paymentMethod = rbPayment != null ? rbPayment.getText().toString() : "Tarjeta";

        showPurchaseConfirmation(address, city, state, zipCode, phone, paymentMethod);
    }

    private void showPurchaseConfirmation(String address, String city, String state,
                                          String zipCode, String phone, String paymentMethod) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🛒 Confirmar Compra");

        String message = String.format(
                "¿Confirmar la compra de este producto?\n\n" +
                        "📚 Libro: %s\n" +
                        "📦 Cantidad: %d unidades\n" +
                        "💰 Total: $%.2f MXN\n\n" +
                        "📍 Dirección: %s\n" +
                        "💳 Método de pago: %s\n\n" +
                        "⚠️ Stock disponible: %d unidades",
                bookTitle, quantity, (price * quantity * 1.16),
                address, paymentMethod, availableStock
        );

        builder.setMessage(message);
        builder.setPositiveButton("✅ Confirmar", (dialog, which) -> {
            createPurchaseOrder(address, city, state, zipCode, phone, paymentMethod);
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void createPurchaseOrder(String address, String city, String state,
                                     String zipCode, String phone, String paymentMethod) {
        PurchaseOrder order = new PurchaseOrder(
                auth.getCurrentUser().getUid(),
                auth.getCurrentUser().getEmail(),
                auth.getCurrentUser().getDisplayName()
        );

        PurchaseOrder.PurchaseItem item = new PurchaseOrder.PurchaseItem(
                bookId, bookTitle, bookAuthor, price, quantity, "Físico"
        );
        item.setBranchId(branchId);
        item.setBranchName(branchName);
        order.addItem(item);

        order.setShippingAddress(address);
        order.setShippingCity(city);
        order.setShippingState(state);
        order.setShippingZipCode(zipCode);
        order.setShippingPhone(phone);
        order.setPaymentMethod(paymentMethod);
        order.setStatus("Pendiente");

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 7);
        order.setEstimatedDelivery(calendar.getTime());

        savePurchaseOrder(order);
    }

    private void savePurchaseOrder(PurchaseOrder order) {
        Log.d(TAG, "🔄 Iniciando guardado de orden...");

        btnConfirmPurchase.setEnabled(false);
        btnConfirmPurchase.setText("Procesando...");

        db.collection("compras")
                .add(order)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "✅ Orden guardada con ID: " + docRef.getId());

                    order.setId(docRef.getId());

                    // Actualizar inventario
                    updateInventoryStock();

                    // Guardar dirección
                    saveAddressForFuture();

                    // 🔥 GENERAR Y GUARDAR TICKET
                    Log.d(TAG, "🎫 Generando ticket...");
                    String ticketContent = generarTicketCompleto(order);

                    Log.d(TAG, "💾 Guardando ticket en Firestore...");
                    saveTicketToFirestore(order.getId(), ticketContent);

                    // Mostrar ticket al usuario
                    showPurchaseTicket(ticketContent);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error guardando orden: " + e.getMessage());
                    btnConfirmPurchase.setEnabled(true);
                    btnConfirmPurchase.setText("Confirmar Compra");
                    Toast.makeText(this, "❌ Error al procesar la compra: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // 🔥 GENERAR TICKET COMPLETO
    private String generarTicketCompleto(PurchaseOrder order) {
        StringBuilder ticket = new StringBuilder();
        ticket.append("╔═══════════════════════════════╗\n");
        ticket.append("       BIBLIOCLOUD\n");
        ticket.append("       TICKET DE COMPRA\n");
        ticket.append("╚═══════════════════════════════╝\n\n");

        ticket.append("📋 Orden: #").append(order.getId().substring(0, 8).toUpperCase()).append("\n");
        ticket.append("📅 Fecha: ").append(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",
                java.util.Locale.getDefault()).format(new java.util.Date())).append("\n\n");

        ticket.append("───────────────────────────────\n");
        ticket.append("👤 Cliente: ").append(order.getUserName()).append("\n");
        ticket.append("📧 Email: ").append(order.getUserEmail()).append("\n\n");

        ticket.append("───────────────────────────────\n");
        ticket.append("📚 PRODUCTOS:\n\n");

        for (PurchaseOrder.PurchaseItem item : order.getItems()) {
            ticket.append("• ").append(item.getBookTitle()).append("\n");
            ticket.append("  Autor: ").append(item.getBookAuthor()).append("\n");
            ticket.append("  Cantidad: ").append(item.getQuantity()).append("\n");
            ticket.append("  Precio: $").append(String.format("%.2f", item.getUnitPrice())).append("\n");
            ticket.append("  Subtotal: $").append(String.format("%.2f", item.getTotalPrice())).append("\n\n");
        }

        ticket.append("───────────────────────────────\n");
        ticket.append("Subtotal: $").append(String.format("%.2f", order.getSubtotal())).append("\n");
        ticket.append("IVA (16%): $").append(String.format("%.2f", order.getTax())).append("\n");
        ticket.append("TOTAL: $").append(String.format("%.2f", order.getTotal())).append("\n\n");

        ticket.append("───────────────────────────────\n");
        ticket.append("💳 Método de pago: ").append(order.getPaymentMethod()).append("\n");
        ticket.append("📦 Estado: ").append(order.getStatus()).append("\n\n");

        ticket.append("───────────────────────────────\n");
        ticket.append("📍 DIRECCIÓN DE ENVÍO:\n");
        ticket.append(order.getFullShippingAddress()).append("\n");
        ticket.append("📞 Teléfono: ").append(order.getShippingPhone()).append("\n\n");

        ticket.append("───────────────────────────────\n");
        ticket.append("🚚 Entrega estimada:\n");
        ticket.append(order.getFormattedEstimatedDelivery()).append("\n\n");

        ticket.append("───────────────────────────────\n");
        ticket.append("ℹ️  INFORMACIÓN IMPORTANTE:\n");
        ticket.append("• Recibirás un email de confirmación\n");
        ticket.append("• Tu pedido será procesado pronto\n");
        ticket.append("• Podrás rastrear tu envío desde\n");
        ticket.append("  la sección 'Mis Compras'\n\n");

        ticket.append("╔═══════════════════════════════╗\n");
        ticket.append("   ¡Gracias por tu compra!\n");
        ticket.append("      Vuelve pronto\n");
        ticket.append("╚═══════════════════════════════╝\n");

        Log.d(TAG, "✅ Ticket generado exitosamente");
        return ticket.toString();
    }

    // 🔥 GUARDAR TICKET EN FIRESTORE
    private void saveTicketToFirestore(String orderId, String ticketContent) {
        Log.d(TAG, "📝 Preparando datos del ticket para Firestore...");
        Log.d(TAG, "   - OrderID: " + orderId);
        Log.d(TAG, "   - UserID: " + auth.getCurrentUser().getUid());
        Log.d(TAG, "   - Longitud del contenido: " + ticketContent.length() + " caracteres");

        Map<String, Object> ticketData = new HashMap<>();
        ticketData.put("orderId", orderId);
        ticketData.put("userId", auth.getCurrentUser().getUid());
        ticketData.put("userEmail", auth.getCurrentUser().getEmail());
        ticketData.put("ticketContent", ticketContent);
        ticketData.put("timestamp", System.currentTimeMillis());

        Log.d(TAG, "💾 Guardando en colección 'tickets'...");

        db.collection("tickets")
                .add(ticketData)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "✅✅✅ TICKET GUARDADO EXITOSAMENTE ✅✅✅");
                    Log.d(TAG, "   - ID del ticket: " + docRef.getId());
                    Log.d(TAG, "   - Ruta: tickets/" + docRef.getId());

                    Toast.makeText(this,
                            "✅ Ticket guardado correctamente",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌❌❌ ERROR GUARDANDO TICKET ❌❌❌");
                    Log.e(TAG, "   - Error: " + e.getMessage());
                    Log.e(TAG, "   - Tipo: " + e.getClass().getSimpleName());
                    e.printStackTrace();

                    Toast.makeText(this,
                            "⚠️ Advertencia: Error guardando ticket",
                            Toast.LENGTH_LONG).show();
                });
    }

    // 🎫 MOSTRAR TICKET AL USUARIO
    private void showPurchaseTicket(String ticketContent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✅ Compra Realizada");
        builder.setCancelable(false);
        builder.setMessage(ticketContent);

        builder.setPositiveButton("✅ Aceptar", (dialog, which) -> {
            Toast.makeText(this, "✅ Compra completada exitosamente", Toast.LENGTH_SHORT).show();
            finish();
        });

        builder.setNeutralButton("📤 Compartir", (dialog, which) -> {
            shareTicket(ticketContent);
            finish();
        });

        builder.show();
    }

    // 📤 COMPARTIR TICKET
    private void shareTicket(String ticketContent) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "BiblioCloud - Ticket de Compra");
        shareIntent.putExtra(Intent.EXTRA_TEXT, ticketContent);
        startActivity(Intent.createChooser(shareIntent, "Compartir ticket"));
    }

    // 🔥 ACTUALIZAR INVENTARIO
    private void updateInventoryStock() {
        db.collection("inventario").document(inventoryId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long currentStock = doc.getLong("availablePhysical");
                        if (currentStock != null && currentStock >= quantity) {
                            int newStock = currentStock.intValue() - quantity;

                            db.collection("inventario").document(inventoryId)
                                    .update("availablePhysical", Math.max(0, newStock))
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, String.format("✅ Stock actualizado: %d -> %d (-%d)",
                                                currentStock.intValue(), newStock, quantity));
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "❌ Error actualizando stock: " + e.getMessage());
                                    });
                        } else {
                            Toast.makeText(this,
                                    "⚠️ Advertencia: Stock insuficiente al momento de la compra",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al obtener stock: " + e.getMessage());
                });
    }

    private void saveAddressForFuture() {
        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_address", etAddress.getText().toString().trim());
        editor.putString("last_city", etCity.getText().toString().trim());
        editor.putString("last_state", etState.getText().toString().trim());
        editor.putString("last_zipcode", etZipCode.getText().toString().trim());
        editor.apply();
    }

    private boolean validateInputs() {
        String address = etAddress.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String state = etState.getText().toString().trim();
        String zipCode = etZipCode.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (address.isEmpty()) {
            etAddress.setError("Ingresa tu dirección");
            return false;
        }

        if (city.isEmpty()) {
            etCity.setError("Ingresa tu ciudad");
            return false;
        }

        if (state.isEmpty()) {
            etState.setError("Ingresa tu estado");
            return false;
        }

        if (zipCode.isEmpty() || zipCode.length() != 5) {
            etZipCode.setError("Código postal inválido");
            return false;
        }

        if (phone.isEmpty() || phone.length() < 10) {
            etPhone.setError("Teléfono inválido");
            return false;
        }

        if (rgPaymentMethod.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Selecciona un método de pago", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Debes aceptar los términos y condiciones",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (quantity > availableStock) {
            Toast.makeText(this,
                    String.format("⚠️ Solo hay %d unidades disponibles", availableStock),
                    Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }
}