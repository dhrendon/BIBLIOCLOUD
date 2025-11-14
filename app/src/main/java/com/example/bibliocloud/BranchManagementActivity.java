package com.example.bibliocloud;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.bibliocloud.models.Branch;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.List;

public class BranchManagementActivity extends AppCompatActivity {

    private EditText etName, etAddress, etPhone, etSchedule, etManagerName, etManagerEmail;
    private MaterialButton btnAddBranch, btnSelectLocation, btnBack;
    private TextView tvSelectedLocation, tvTotalBranches;
    private LinearLayout layoutBranchesList;

    private FirebaseFirestore db;
    private double selectedLatitude = 0;
    private double selectedLongitude = 0;
    private static final int MAP_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branch_management);

        db = FirebaseFirestore.getInstance();
        initializeViews();
        setupListeners();
        loadBranches();
    }

    private void initializeViews() {
        etName = findViewById(R.id.etBranchName);
        etAddress = findViewById(R.id.etBranchAddress);
        etPhone = findViewById(R.id.etBranchPhone);
        etSchedule = findViewById(R.id.etBranchSchedule);
        etManagerName = findViewById(R.id.etManagerName);
        etManagerEmail = findViewById(R.id.etManagerEmail);

        btnAddBranch = findViewById(R.id.btnAddBranch);
        btnSelectLocation = findViewById(R.id.btnSelectLocation);
        btnBack = findViewById(R.id.btnBack);

        tvSelectedLocation = findViewById(R.id.tvSelectedLocation);
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
            tvSelectedLocation.setText(String.format("📍 Ubicación: %.6f, %.6f",
                    selectedLatitude, selectedLongitude));
        }
    }

    private void addBranch() {
        String name = etName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String schedule = etSchedule.getText().toString().trim();
        String managerName = etManagerName.getText().toString().trim();
        String managerEmail = etManagerEmail.getText().toString().trim();

        if (!validateInputs(name, address, phone)) return;

        if (selectedLatitude == 0 && selectedLongitude == 0) {
            Toast.makeText(this, "Selecciona la ubicación en el mapa", Toast.LENGTH_SHORT).show();
            return;
        }

        Branch branch = new Branch(name, address, phone, schedule,
                selectedLatitude, selectedLongitude);
        branch.setManagerName(managerName);
        branch.setManagerEmail(managerEmail);

        db.collection("sucursales")
                .add(branch.toMap())
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "✅ Sucursal agregada exitosamente", Toast.LENGTH_SHORT).show();
                    clearForm();
                    loadBranches();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private boolean validateInputs(String name, String address, String phone) {
        if (name.isEmpty()) {
            etName.setError("Ingresa el nombre de la sucursal");
            return false;
        }
        if (address.isEmpty()) {
            etAddress.setError("Ingresa la dirección");
            return false;
        }
        if (phone.isEmpty()) {
            etPhone.setError("Ingresa el teléfono");
            return false;
        }
        return true;
    }

    private void loadBranches() {
        layoutBranchesList.removeAllViews();

        db.collection("sucursales")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        Toast.makeText(this, "Error al cargar sucursales", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Branch> branches = snapshots.toObjects(Branch.class);
                    tvTotalBranches.setText("Total de Sucursales: " + branches.size());

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Branch branch = doc.toObject(Branch.class);
                        if (branch != null) {
                            branch.setId(doc.getId());
                            layoutBranchesList.addView(createBranchCard(branch));
                        }
                    }

                    if (branches.isEmpty()) {
                        TextView tvEmpty = new TextView(this);
                        tvEmpty.setText("No hay sucursales registradas");
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
        card.setCardElevation(4);
        card.setRadius(8);
        card.setCardBackgroundColor(getResources().getColor(R.color.light_brown));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        // Nombre
        TextView tvName = new TextView(this);
        tvName.setText("🏢 " + branch.getName());
        tvName.setTextSize(18);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvName);

        // Dirección
        TextView tvAddress = new TextView(this);
        tvAddress.setText("📍 " + branch.getAddress());
        layout.addView(tvAddress);

        // Teléfono
        TextView tvPhone = new TextView(this);
        tvPhone.setText("📞 " + branch.getPhone());
        layout.addView(tvPhone);

        // Horario
        TextView tvSchedule = new TextView(this);
        tvSchedule.setText("🕒 " + branch.getSchedule());
        layout.addView(tvSchedule);

        // Estado
        TextView tvStatus = new TextView(this);
        tvStatus.setText(branch.isActive() ? "✅ Activa" : "❌ Inactiva");
        tvStatus.setTextColor(getResources().getColor(
                branch.isActive() ? R.color.green : R.color.red));
        layout.addView(tvStatus);

        // Libros
        TextView tvBooks = new TextView(this);
        tvBooks.setText("📚 Libros: " + branch.getTotalBooks());
        layout.addView(tvBooks);

        // Botones
        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setPadding(0, 12, 0, 0);

        Button btnManageBooks = new Button(this);
        btnManageBooks.setText("Gestionar Libros");
        btnManageBooks.setOnClickListener(v -> {
            Intent intent = new Intent(this, BranchBooksActivity.class);
            intent.putExtra("branchId", branch.getId());
            intent.putExtra("branchName", branch.getName());
            startActivity(intent);
        });

        Button btnToggleStatus = new Button(this);
        btnToggleStatus.setText(branch.isActive() ? "Desactivar" : "Activar");
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
                    Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show();
                    loadBranches();
                });
    }

    private void clearForm() {
        etName.setText("");
        etAddress.setText("");
        etPhone.setText("");
        etSchedule.setText("");
        etManagerName.setText("");
        etManagerEmail.setText("");
        tvSelectedLocation.setText("Ubicación no seleccionada");
        selectedLatitude = 0;
        selectedLongitude = 0;
    }
}