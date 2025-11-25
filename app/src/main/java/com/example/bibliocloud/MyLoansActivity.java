package com.example.bibliocloud;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import com.example.bibliocloud.models.Loan;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

public class MyLoansActivity extends AppCompatActivity {

    private Spinner spinnerFilter;
    private LinearLayout layoutLoansList;
    private TextView tvTotalLoans, tvActiveLoans, tvOverdueLoans, tvAdminAssigned;
    private LinearLayout layoutEmpty;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<Loan> allLoans;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_loans);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initializeViews();
        setupToolbar();
        setupFilter();
        loadLoans();
    }

    private void initializeViews() {
        spinnerFilter = findViewById(R.id.spinnerFilter);
        layoutLoansList = findViewById(R.id.layoutLoansList);
        tvTotalLoans = findViewById(R.id.tvTotalLoans);
        tvActiveLoans = findViewById(R.id.tvActiveLoans);
        tvOverdueLoans = findViewById(R.id.tvOverdueLoans);
        tvAdminAssigned = findViewById(R.id.tvAdminAssigned);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        allLoans = new ArrayList<>();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mis Préstamos");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupFilter() {
        String[] filters = {"Todos", "Activos", "Devueltos", "Vencidos", "Reservas", "Asignados por Admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filters);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterLoans((String) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadLoans() {
        String userId = auth.getCurrentUser().getUid();

        // 🔥 CARGAR TODOS LOS PRÉSTAMOS DEL USUARIO (admin + propios)
        db.collection("prestamos")
                .whereEqualTo("id_usuario", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        Toast.makeText(this, "Error al cargar préstamos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allLoans.clear();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Loan loan = doc.toObject(Loan.class);
                        if (loan != null) {
                            loan.setId(doc.getId());
                            loan.calculateFine();

                            // 🔥 DETECTAR SI FUE ASIGNADO POR ADMIN
                            Boolean assignedByAdmin = doc.getBoolean("asignado_por_admin");
                            if (assignedByAdmin != null && assignedByAdmin) {
                                // Marcar en las notas si no está marcado
                                if (loan.getNotes() == null || !loan.getNotes().contains("[ADMIN]")) {
                                    loan.setNotes("[ADMIN] " + (loan.getNotes() != null ? loan.getNotes() : ""));
                                }
                            }

                            allLoans.add(loan);
                        }
                    }

                    updateStatistics();
                    displayLoans(allLoans);
                });
    }

    private void filterLoans(String filter) {
        List<Loan> filtered = new ArrayList<>();

        for (Loan loan : allLoans) {
            boolean matches = false;

            switch (filter) {
                case "Todos":
                    matches = true;
                    break;
                case "Activos":
                    matches = loan.getStatus().equals("Activo");
                    break;
                case "Devueltos":
                    matches = loan.getStatus().equals("Devuelto");
                    break;
                case "Vencidos":
                    matches = loan.isOverdue();
                    break;
                case "Reservas":
                    matches = loan.getType().equals("Reserva");
                    break;
                case "Asignados por Admin":
                    // 🔥 NUEVO FILTRO: Solo préstamos asignados por admin
                    matches = loan.getNotes() != null && loan.getNotes().contains("[ADMIN]");
                    break;
            }

            if (matches) {
                filtered.add(loan);
            }
        }

        displayLoans(filtered);
    }

    private void displayLoans(List<Loan> loans) {
        layoutLoansList.removeAllViews();

        if (loans.isEmpty()) {
            showEmptyState();
            return;
        }

        layoutEmpty.setVisibility(View.GONE);

        for (Loan loan : loans) {
            layoutLoansList.addView(createLoanCard(loan));
        }
    }

    private CardView createLoanCard(Loan loan) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setCardElevation(4);
        card.setRadius(8);

        // 🔥 DIFERENTE COLOR SI FUE ASIGNADO POR ADMIN
        boolean isAdminAssigned = loan.getNotes() != null && loan.getNotes().contains("[ADMIN]");

        int backgroundColor;
        if (loan.isOverdue()) {
            backgroundColor = getResources().getColor(android.R.color.holo_red_light);
        } else if (isAdminAssigned) {
            backgroundColor = getResources().getColor(android.R.color.holo_purple);
        } else {
            backgroundColor = getResources().getColor(R.color.light_brown);
        }
        card.setCardBackgroundColor(backgroundColor);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        // 🔥 BADGE: Indicador de asignación por admin
        if (isAdminAssigned) {
            TextView tvAdminBadge = new TextView(this);
            tvAdminBadge.setText("🔧 ASIGNADO POR ADMINISTRADOR");
            tvAdminBadge.setTextSize(12);
            tvAdminBadge.setTypeface(null, android.graphics.Typeface.BOLD);
            tvAdminBadge.setTextColor(getResources().getColor(R.color.white));
            tvAdminBadge.setBackgroundColor(getResources().getColor(android.R.color.holo_purple));
            tvAdminBadge.setPadding(8, 4, 8, 4);
            layout.addView(tvAdminBadge);
        }

        // Tipo
        TextView tvType = new TextView(this);
        String icon = loan.getType().equals("Préstamo") ? "📚" : "⏳";
        tvType.setText(icon + " " + loan.getType());
        tvType.setTextSize(16);
        tvType.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvType);

        // Título del libro
        TextView tvTitle = new TextView(this);
        tvTitle.setText(loan.getBookTitle());
        tvTitle.setTextSize(15);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvTitle);

        // Autor
        TextView tvAuthor = new TextView(this);
        tvAuthor.setText("por " + loan.getBookAuthor());
        tvAuthor.setTextSize(13);
        layout.addView(tvAuthor);

        // Sucursal
        TextView tvBranch = new TextView(this);
        tvBranch.setText("📍 " + loan.getBranchName());
        tvBranch.setTextSize(13);
        layout.addView(tvBranch);

        // Fechas
        if (loan.getType().equals("Préstamo")) {
            TextView tvDates = new TextView(this);
            tvDates.setText("📅 Préstamo: " + loan.getFormattedLoanDate() +
                    "\n📅 Devolución: " + loan.getFormattedReturnDate());
            tvDates.setTextSize(13);
            layout.addView(tvDates);

            // Días restantes o vencido
            if (loan.getStatus().equals("Activo")) {
                TextView tvDaysRemaining = new TextView(this);
                if (loan.isOverdue()) {
                    tvDaysRemaining.setText("⚠️ VENCIDO - " + loan.getDaysOverdue() + " días de retraso");
                    tvDaysRemaining.setTextColor(getResources().getColor(R.color.red));
                } else {
                    tvDaysRemaining.setText("⏰ " + loan.getDaysRemaining() + " días restantes");
                    tvDaysRemaining.setTextColor(getResources().getColor(R.color.green));
                }
                tvDaysRemaining.setTextSize(13);
                tvDaysRemaining.setTypeface(null, android.graphics.Typeface.BOLD);
                layout.addView(tvDaysRemaining);
            }

            // Multa
            if (loan.getFine() > 0) {
                TextView tvFine = new TextView(this);
                tvFine.setText("💰 Multa: $" + String.format("%.2f", loan.getFine()) + " MXN");
                tvFine.setTextSize(14);
                tvFine.setTextColor(getResources().getColor(R.color.red));
                tvFine.setTypeface(null, android.graphics.Typeface.BOLD);
                layout.addView(tvFine);
            }
        }

        // Estado
        TextView tvStatus = new TextView(this);
        tvStatus.setText("Estado: " + loan.getStatus());
        tvStatus.setTextSize(13);
        tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);

        int statusColor = loan.getStatus().equals("Activo") ?
                (loan.isOverdue() ? R.color.red : R.color.green) :
                R.color.colorTextSecondary;
        tvStatus.setTextColor(getResources().getColor(statusColor));
        layout.addView(tvStatus);

        // Botón de detalles
        Button btnDetails = new Button(this);
        btnDetails.setText("Ver Detalles");
        btnDetails.setOnClickListener(v -> showLoanDetails(loan));
        layout.addView(btnDetails);

        card.addView(layout);
        return card;
    }

    private void showLoanDetails(Loan loan) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Detalles del " + loan.getType());

        StringBuilder details = new StringBuilder();

        // 🔥 INDICADOR DE ASIGNACIÓN POR ADMIN
        boolean isAdminAssigned = loan.getNotes() != null && loan.getNotes().contains("[ADMIN]");
        if (isAdminAssigned) {
            details.append("🔧 ASIGNADO POR ADMINISTRADOR\n");
            details.append("═══════════════════════════\n\n");
        }

        details.append("Libro: ").append(loan.getBookTitle()).append("\n");
        details.append("Autor: ").append(loan.getBookAuthor()).append("\n\n");
        details.append("Sucursal: ").append(loan.getBranchName()).append("\n\n");

        if (loan.getType().equals("Préstamo")) {
            details.append("Fecha de préstamo: ").append(loan.getFormattedLoanDate()).append("\n");
            details.append("Fecha de devolución: ").append(loan.getFormattedReturnDate()).append("\n");

            if (loan.getActualReturnDate() != null) {
                details.append("Devuelto el: ").append(loan.getFormattedActualReturnDate()).append("\n");
            }

            if (loan.isOverdue()) {
                details.append("\n⚠️ PRÉSTAMO VENCIDO\n");
                details.append("Días de retraso: ").append(loan.getDaysOverdue()).append("\n");
                details.append("Multa: $").append(String.format("%.2f", loan.getFine())).append(" MXN\n");
            }
        }

        details.append("\nEstado: ").append(loan.getStatus()).append("\n");

        if (loan.getNotes() != null && !loan.getNotes().isEmpty()) {
            details.append("\nNotas: ").append(loan.getNotes());
        }

        builder.setMessage(details.toString());
        builder.setPositiveButton("Cerrar", null);
        builder.show();
    }

    private void updateStatistics() {
        int activeCount = 0;
        int overdueCount = 0;
        int adminAssignedCount = 0;

        for (Loan loan : allLoans) {
            if (loan.getStatus().equals("Activo")) {
                activeCount++;
                if (loan.isOverdue()) {
                    overdueCount++;
                }
            }

            // 🔥 CONTAR PRÉSTAMOS ASIGNADOS POR ADMIN
            if (loan.getNotes() != null && loan.getNotes().contains("[ADMIN]")) {
                adminAssignedCount++;
            }
        }

        tvTotalLoans.setText("Total: " + allLoans.size());
        tvActiveLoans.setText("Activos: " + activeCount);
        tvOverdueLoans.setText("Vencidos: " + overdueCount);
        tvAdminAssigned.setText("🔧 Admin: " + adminAssignedCount);
    }

    private void showEmptyState() {
        layoutEmpty.setVisibility(View.VISIBLE);
        layoutLoansList.removeAllViews();

        TextView tvEmpty = new TextView(this);
        tvEmpty.setText("No tienes préstamos registrados\n\nExplora el catálogo y solicita un préstamo");
        tvEmpty.setTextSize(16);
        tvEmpty.setPadding(32, 32, 32, 32);
        tvEmpty.setGravity(android.view.Gravity.CENTER);
        layoutLoansList.addView(tvEmpty);
    }
}