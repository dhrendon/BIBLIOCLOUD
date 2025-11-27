package com.example.bibliocloud;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.bibliocloud.models.Branch;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.*;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class BranchManagementActivity extends AppCompatActivity {

    private static final String TAG = "BranchManagement";
    private static final int MAP_REQUEST_CODE = 100;

    private EditText etName, etPhone, etSchedule, etManagerName, etManagerEmail;
    private MaterialButton btnAddBranch, btnSelectLocation, btnBack;
    private TextView tvSelectedLocation, tvSelectedAddress, tvTotalBranches;
    private LinearLayout layoutBranchesList;

    private FirebaseFirestore db;
    private Geocoder geocoder;

    private double selectedLatitude = 0;
    private double selectedLongitude = 0;
    private String selectedAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branch_management);

        db = FirebaseFirestore.getInstance();
        geocoder = new Geocoder(this, Locale.getDefault());

        initializeViews();
        setupListeners();
        loadBranches();
    }

    private void initializeViews() {
        etName = findViewById(R.id.etBranchName);
        etPhone = findViewById(R.id.etBranchPhone);
        etSchedule = findViewById(R.id.etBranchSchedule);
        etManagerName = findViewById(R.id.etManagerName);
        etManagerEmail = findViewById(R.id.etManagerEmail);

        btnAddBranch = findViewById(R.id.btnAddBranch);
        btnSelectLocation = findViewById(R.id.btnSelectLocation);
        btnBack = findViewById(R.id.btnBack);

        tvSelectedLocation = findViewById(R.id.tvSelectedLocation);
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
        tvTotalBranches = findViewById(R.id.tvTotalBranches);
        layoutBranchesList = findViewById(R.id.layoutBranchesList);
    }

    private void setupListeners() {
        btnAddBranch.setOnClickListener(v -> addBranch());
        btnBack.setOnClickListener(v -> finish());

        btnSelectLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, BranchLocationPickerActivity.class);
            startActivityForResult(intent, MAP_REQUEST_CODE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MAP_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            selectedLatitude = data.getDoubleExtra("latitude", 0);
            selectedLongitude = data.getDoubleExtra("longitude", 0);

            Log.d(TAG, "📍 Ubicación seleccionada: " + selectedLatitude + ", " + selectedLongitude);

            // Mostrar coordenadas
            tvSelectedLocation.setText(String.format("📍 Coordenadas: %.6f, %.6f",
                    selectedLatitude, selectedLongitude));

            // Obtener dirección mediante Geocoding
            obtenerDireccionDesdeCoordenadas(selectedLatitude, selectedLongitude);
        }
    }

    private void obtenerDireccionDesdeCoordenadas(double latitude, double longitude) {
        // Ejecutar en segundo plano para no bloquear UI
        new Thread(() -> {
            try {
                Log.d(TAG, "🔍 Iniciando geocoding...");

                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);

                    // Construir dirección completa
                    StringBuilder fullAddress = new StringBuilder();

                    // Calle y número
                    if (address.getThoroughfare() != null) {
                        fullAddress.append(address.getThoroughfare());
                    }
                    if (address.getSubThoroughfare() != null) {
                        fullAddress.append(" ").append(address.getSubThoroughfare());
                    }

                    // Colonia/Barrio
                    if (address.getSubLocality() != null) {
                        if (fullAddress.length() > 0) fullAddress.append(", ");
                        fullAddress.append(address.getSubLocality());
                    }

                    // Ciudad
                    if (address.getLocality() != null) {
                        if (fullAddress.length() > 0) fullAddress.append(", ");
                        fullAddress.append(address.getLocality());
                    }

                    // Estado
                    if (address.getAdminArea() != null) {
                        if (fullAddress.length() > 0) fullAddress.append(", ");
                        fullAddress.append(address.getAdminArea());
                    }

                    // Código postal
                    if (address.getPostalCode() != null) {
                        if (fullAddress.length() > 0) fullAddress.append(", ");
                        fullAddress.append("C.P. " + address.getPostalCode());
                    }

                    // País
                    if (address.getCountryName() != null) {
                        if (fullAddress.length() > 0) fullAddress.append(", ");
                        fullAddress.append(address.getCountryName());
                    }

                    selectedAddress = fullAddress.toString();

                    Log.d(TAG, "✅ Dirección obtenida: " + selectedAddress);

                    // Actualizar UI en el hilo principal
                    runOnUiThread(() -> {
                        if (selectedAddress.isEmpty()) {
                            tvSelectedAddress.setText("⚠️ No se pudo obtener la dirección exacta");
                            tvSelectedAddress.setTextColor(getResources().getColor(R.color.orange));
                        } else {
                            tvSelectedAddress.setText("📍 " + selectedAddress);
                            tvSelectedAddress.setTextColor(getResources().getColor(R.color.green));
                        }
                        tvSelectedAddress.setVisibility(android.view.View.VISIBLE);
                    });

                } else {
                    Log.w(TAG, "⚠️ No se encontraron direcciones");
                    selectedAddress = String.format("Lat: %.6f, Lng: %.6f", latitude, longitude);

                    runOnUiThread(() -> {
                        tvSelectedAddress.setText("⚠️ Dirección no disponible. Usando coordenadas.");
                        tvSelectedAddress.setTextColor(getResources().getColor(R.color.orange));
                        tvSelectedAddress.setVisibility(android.view.View.VISIBLE);
                    });
                }

            } catch (IOException e) {
                Log.e(TAG, "❌ Error en geocoding: " + e.getMessage());
                selectedAddress = String.format("Lat: %.6f, Lng: %.6f", latitude, longitude);

                runOnUiThread(() -> {
                    tvSelectedAddress.setText("❌ Error obteniendo dirección. Usando coordenadas.");
                    tvSelectedAddress.setTextColor(getResources().getColor(R.color.red));
                    tvSelectedAddress.setVisibility(android.view.View.VISIBLE);
                    Toast.makeText(this, "Error de geocoding: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void addBranch() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String schedule = etSchedule.getText().toString().trim();
        String managerName = etManagerName.getText().toString().trim();
        String managerEmail = etManagerEmail.getText().toString().trim();

        // Validaciones
        if (name.isEmpty()) {
            etName.setError("⚠️ Ingresa el nombre de la sucursal");
            etName.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            etPhone.setError("⚠️ Ingresa el teléfono");
            etPhone.requestFocus();
            return;
        }

        if (selectedLatitude == 0 && selectedLongitude == 0) {
            Toast.makeText(this, "⚠️ Selecciona la ubicación en el mapa", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedAddress.isEmpty()) {
            Toast.makeText(this, "⚠️ Espera a que se obtenga la dirección", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear sucursal con dirección obtenida automáticamente
        Branch branch = new Branch(name, selectedAddress, phone, schedule,
                selectedLatitude, selectedLongitude);
        branch.setManagerName(managerName);
        branch.setManagerEmail(managerEmail);

        Log.d(TAG, "💾 Guardando sucursal: " + name);
        Log.d(TAG, "📍 Dirección: " + selectedAddress);

        db.collection("sucursales")
                .add(branch.toMap())
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "✅ Sucursal guardada con ID: " + docRef.getId());
                    Toast.makeText(this, "✅ Sucursal agregada exitosamente", Toast.LENGTH_SHORT).show();
                    clearForm();
                    loadBranches();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error guardando sucursal: " + e.getMessage());
                    Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadBranches() {
        layoutBranchesList.removeAllViews();
        Log.d(TAG, "📚 Cargando sucursales...");

        db.collection("sucursales")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Error cargando sucursales: " + error.getMessage());
                        Toast.makeText(this, "Error al cargar sucursales", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null) {
                        Log.w(TAG, "⚠️ Snapshots es null");
                        return;
                    }

                    List<Branch> branches = snapshots.toObjects(Branch.class);
                    int totalBranches = branches.size();

                    Log.d(TAG, "✅ Total sucursales: " + totalBranches);
                    tvTotalBranches.setText("Total de Sucursales: " + totalBranches);

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Branch branch = doc.toObject(Branch.class);
                        if (branch != null) {
                            branch.setId(doc.getId());
                            Log.d(TAG, "🏢 Sucursal: " + branch.getName() + " | " + branch.getAddress());
                            layoutBranchesList.addView(createBranchCard(branch));
                        }
                    }

                    if (branches.isEmpty()) {
                        TextView tvEmpty = new TextView(this);
                        tvEmpty.setText("📭 No hay sucursales registradas");
                        tvEmpty.setTextSize(16);
                        tvEmpty.setTextColor(getResources().getColor(R.color.colorTextSecondary));
                        tvEmpty.setGravity(android.view.Gravity.CENTER);
                        tvEmpty.setPadding(16, 32, 16, 16);
                        layoutBranchesList.addView(tvEmpty);
                    }
                });
    }

    private CardView createBranchCard(Branch branch) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setCardElevation(8);
        card.setRadius(12);
        card.setCardBackgroundColor(getResources().getColor(R.color.light_brown));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        // Nombre
        TextView tvName = new TextView(this);
        tvName.setText("🏢 " + branch.getName());
        tvName.setTextSize(20);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvName.setTextColor(getResources().getColor(R.color.colorPrimary));
        layout.addView(tvName);

        // Dirección completa
        TextView tvAddress = new TextView(this);
        tvAddress.setText("📍 " + branch.getAddress());
        tvAddress.setTextSize(14);
        tvAddress.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        tvAddress.setPadding(0, 8, 0, 0);
        layout.addView(tvAddress);

        // Coordenadas
        TextView tvCoordinates = new TextView(this);
        tvCoordinates.setText(String.format("🗺️ Coordenadas: %.6f, %.6f",
                branch.getLatitude(), branch.getLongitude()));
        tvCoordinates.setTextSize(12);
        tvCoordinates.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        layout.addView(tvCoordinates);

        // Teléfono
        TextView tvPhone = new TextView(this);
        tvPhone.setText("📞 " + branch.getPhone());
        tvPhone.setTextSize(14);
        tvPhone.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        layout.addView(tvPhone);

        // Horario
        TextView tvSchedule = new TextView(this);
        tvSchedule.setText("🕒 " + branch.getSchedule());
        tvSchedule.setTextSize(14);
        tvSchedule.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        layout.addView(tvSchedule);

        // Gerente (si existe)
        if (branch.getManagerName() != null && !branch.getManagerName().isEmpty()) {
            TextView tvManager = new TextView(this);
            tvManager.setText("👤 Gerente: " + branch.getManagerName());
            tvManager.setTextSize(14);
            tvManager.setTextColor(getResources().getColor(R.color.colorTextPrimary));
            layout.addView(tvManager);
        }

        // Estado
        TextView tvStatus = new TextView(this);
        tvStatus.setText(branch.isActive() ? "✅ Activa" : "❌ Inactiva");
        tvStatus.setTextSize(16);
        tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        tvStatus.setTextColor(getResources().getColor(
                branch.isActive() ? R.color.green : R.color.red));
        layout.addView(tvStatus);

        // Libros
        TextView tvBooks = new TextView(this);
        tvBooks.setText("📚 Libros: " + branch.getTotalBooks());
        tvBooks.setTextSize(14);
        tvBooks.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        layout.addView(tvBooks);

        // Botones
        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonLayoutParams.setMargins(0, 12, 0, 0);
        buttonsLayout.setLayoutParams(buttonLayoutParams);

        Button btnManageBooks = new Button(this);
        btnManageBooks.setText("GESTIONAR LIBROS");
        btnManageBooks.setTextSize(12);
        btnManageBooks.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        btnManageBooks.setTextColor(getResources().getColor(R.color.white));
        LinearLayout.LayoutParams manageBooksParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        manageBooksParams.setMargins(0, 0, 4, 0);
        btnManageBooks.setLayoutParams(manageBooksParams);
        btnManageBooks.setOnClickListener(v -> {
            Intent intent = new Intent(this, BranchBooksActivity.class);
            intent.putExtra("branchId", branch.getId());
            intent.putExtra("branchName", branch.getName());
            startActivity(intent);
        });

        Button btnToggleStatus = new Button(this);
        btnToggleStatus.setText(branch.isActive() ? "DESACTIVAR" : "ACTIVAR");
        btnToggleStatus.setTextSize(12);
        btnToggleStatus.setBackgroundColor(getResources().getColor(
                branch.isActive() ? R.color.orange : R.color.green));
        btnToggleStatus.setTextColor(getResources().getColor(R.color.white));
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        toggleParams.setMargins(4, 0, 0, 0);
        btnToggleStatus.setLayoutParams(toggleParams);
        btnToggleStatus.setOnClickListener(v -> toggleBranchStatus(branch));

        buttonsLayout.addView(btnManageBooks);
        buttonsLayout.addView(btnToggleStatus);
        layout.addView(buttonsLayout);

        card.addView(layout);
        return card;
    }

    private void toggleBranchStatus(Branch branch) {
        db.collection("sucursales").document(branch.getId())
                .update("active", !branch.isActive())
                .addOnSuccessListener(aVoid -> {
                    String message = branch.isActive() ? "Sucursal desactivada" : "Sucursal activada";
                    Toast.makeText(this, "✅ " + message, Toast.LENGTH_SHORT).show();
                    loadBranches();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void clearForm() {
        etName.setText("");
        etPhone.setText("");
        etSchedule.setText("Lun-Vie: 9:00-20:00, Sáb: 10:00-14:00");
        etManagerName.setText("");
        etManagerEmail.setText("");
        tvSelectedLocation.setText("📍 Ubicación no seleccionada");
        tvSelectedAddress.setText("");
        tvSelectedAddress.setVisibility(android.view.View.GONE);
        selectedLatitude = 0;
        selectedLongitude = 0;
        selectedAddress = "";
    }
}