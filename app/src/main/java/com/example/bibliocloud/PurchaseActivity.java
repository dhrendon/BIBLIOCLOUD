package com.example.bibliocloud;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.bibliocloud.models.PurchaseOrder;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.Date;



public class PurchaseActivity extends AppCompatActivity {

    private TextView tvBookTitle, tvPrice, tvBranch, tvSubtotal, tvTax, tvTotal;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        getIntentData();
        initializeViews();
        setupToolbar();
        setupQuantitySpinner();
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
    }

    private void initializeViews() {
        tvBookTitle = findViewById(R.id.tvBookTitle);
        tvPrice = findViewById(R.id.tvPrice);
        tvBranch = findViewById(R.id.tvBranch);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvTax = findViewById(R.id.tvTax);
        tvTotal = findViewById(R.id.tvTotal);

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

        // Mostrar información del libro
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

    private void setupQuantitySpinner() {

        String[] quantities = {"1", "2", "3", "4", "5"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                quantities
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerQuantity.setAdapter(adapter);

        spinnerQuantity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                quantity = Integer.parseInt(quantities[position]);
                updatePriceDisplay();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
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
        if (!validateInputs()) return;

        String address = etAddress.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String state = etState.getText().toString().trim();
        String zipCode = etZipCode.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        int selectedPaymentId = rgPaymentMethod.getCheckedRadioButtonId();
        RadioButton rbPayment = findViewById(selectedPaymentId);
        String paymentMethod = rbPayment != null ? rbPayment.getText().toString() : "Tarjeta";

        // Crear orden de compra
        PurchaseOrder order = new PurchaseOrder(
                auth.getCurrentUser().getUid(),
                auth.getCurrentUser().getEmail(),
                auth.getCurrentUser().getDisplayName()
        );

        // Agregar item
        PurchaseOrder.PurchaseItem item = new PurchaseOrder.PurchaseItem(
                bookId, bookTitle, bookAuthor, price, quantity, "Físico"
        );
        item.setBranchId(branchId);
        item.setBranchName(branchName);
        order.addItem(item);

        // Configurar dirección
        order.setShippingAddress(address);
        order.setShippingCity(city);
        order.setShippingState(state);
        order.setShippingZipCode(zipCode);
        order.setShippingPhone(phone);
        order.setPaymentMethod(paymentMethod);
        order.setStatus("Pendiente");

        // Fecha estimada de entrega (5-7 días)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 7);
        order.setEstimatedDelivery(calendar.getTime());

        // Guardar en Firestore
        savePurchaseOrder(order);
    }

    private void savePurchaseOrder(PurchaseOrder order) {
        btnConfirmPurchase.setEnabled(false);
        btnConfirmPurchase.setText("Procesando...");

        db.collection("compras")
                .add(order)
                .addOnSuccessListener(docRef -> {
                    // Actualizar inventario
                    updateInventory();

                    // Guardar dirección para futuras compras
                    saveAddressForFuture();

                    Toast.makeText(this,
                            "✅ ¡Compra realizada exitosamente!\nRecibirás un email de confirmación",
                            Toast.LENGTH_LONG).show();

                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirmPurchase.setEnabled(true);
                    btnConfirmPurchase.setText("Confirmar Compra");
                    Toast.makeText(this, "❌ Error al procesar la compra: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void updateInventory() {
        // Disminuir el stock disponible
        db.collection("inventario").document(inventoryId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long currentStock = doc.getLong("availablePhysical");
                        if (currentStock != null && currentStock > 0) {
                            int newStock = currentStock.intValue() - quantity;
                            db.collection("inventario").document(inventoryId)
                                    .update("availablePhysical", Math.max(0, newStock));
                        }
                    }
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

        return true;
    }
}