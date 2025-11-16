package com.example.bibliocloud;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import com.example.bibliocloud.models.Payment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

public class UserPaymentHistoryActivity extends AppCompatActivity {

    private LinearLayout layoutPaymentsList;
    private TextView tvTotalPaid, tvTotalTransactions;
    private Spinner spinnerMonthFilter;
    private LinearLayout layoutEmpty;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<Payment> userPayments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_payment_history);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initializeViews();
        setupToolbar();
        setupMonthFilter();
        loadUserPayments();
    }

    private void initializeViews() {
        layoutPaymentsList = findViewById(R.id.layoutPaymentsList);
        tvTotalPaid = findViewById(R.id.tvTotalPaid);
        tvTotalTransactions = findViewById(R.id.tvTotalTransactions);
        spinnerMonthFilter = findViewById(R.id.spinnerMonthFilter);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        userPayments = new ArrayList<>();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Historial de Pagos");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupMonthFilter() {
        String[] months = {"Todos", "Este mes", "Último mes", "Últimos 3 meses", "Último año"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, months);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonthFilter.setAdapter(adapter);

        spinnerMonthFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterPaymentsByMonth(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadUserPayments() {
        String userId = auth.getCurrentUser().getUid();

        db.collection("pagos")
                .whereEqualTo("userId", userId)
                .orderBy("paymentDate", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error al cargar pagos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    userPayments.clear();
                    double totalPaid = 0;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Payment payment = doc.toObject(Payment.class);
                        if (payment != null) {
                            payment.setId(doc.getId());
                            userPayments.add(payment);
                            totalPaid += payment.getAmount();
                        }
                    }

                    updateStatistics(userPayments.size(), totalPaid);
                    displayPayments(userPayments);
                });
    }

    private void filterPaymentsByMonth(int filterPosition) {
        if (userPayments.isEmpty()) return;

        Calendar calendar = Calendar.getInstance();
        Date filterDate = null;

        switch (filterPosition) {
            case 1: // Este mes
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                filterDate = calendar.getTime();
                break;
            case 2: // Último mes
                calendar.add(Calendar.MONTH, -1);
                filterDate = calendar.getTime();
                break;
            case 3: // Últimos 3 meses
                calendar.add(Calendar.MONTH, -3);
                filterDate = calendar.getTime();
                break;
            case 4: // Último año
                calendar.add(Calendar.YEAR, -1);
                filterDate = calendar.getTime();
                break;
        }

        if (filterDate == null) {
            displayPayments(userPayments);
            return;
        }

        List<Payment> filtered = new ArrayList<>();
        double totalFiltered = 0;

        for (Payment payment : userPayments) {
            if (payment.getPaymentDate().after(filterDate)) {
                filtered.add(payment);
                totalFiltered += payment.getAmount();
            }
        }

        updateStatistics(filtered.size(), totalFiltered);
        displayPayments(filtered);
    }

    private void displayPayments(List<Payment> payments) {
        layoutPaymentsList.removeAllViews();
        layoutEmpty.setVisibility(View.GONE);

        if (payments.isEmpty()) {
            showEmptyState();
            return;
        }

        for (Payment payment : payments) {
            layoutPaymentsList.addView(createPaymentCard(payment));
        }
    }

    private CardView createPaymentCard(Payment payment) {
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

        // Ticket
        TextView tvTicket = new TextView(this);
        tvTicket.setText("🎫 Ticket: " + payment.getTicketNumber());
        tvTicket.setTextSize(16);
        tvTicket.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvTicket);

        // Fecha
        TextView tvDate = new TextView(this);
        tvDate.setText("📅 " + payment.getFormattedDateTime());
        layout.addView(tvDate);

        // Sucursal
        TextView tvBranch = new TextView(this);
        tvBranch.setText("🏢 " + payment.getBranchName());
        layout.addView(tvBranch);

        // Libros
        TextView tvBooks = new TextView(this);
        tvBooks.setText("📚 Libros:\n" + payment.getBookTitles());
        layout.addView(tvBooks);

        // Método de pago
        TextView tvMethod = new TextView(this);
        String methodIcon = payment.getPaymentMethod().equals("Efectivo") ? "💵" : "💳";
        tvMethod.setText(methodIcon + " " + payment.getPaymentMethod());
        layout.addView(tvMethod);

        // Monto
        TextView tvAmount = new TextView(this);
        tvAmount.setText(String.format("💰 Total pagado: $%.2f MXN", payment.getAmount()));
        tvAmount.setTextSize(16);
        tvAmount.setTypeface(null, android.graphics.Typeface.BOLD);
        tvAmount.setTextColor(getResources().getColor(R.color.colorPrimary));
        layout.addView(tvAmount);

        // Botón ver detalles
        Button btnDetails = new Button(this);
        btnDetails.setText("Ver Ticket Completo");
        btnDetails.setOnClickListener(v -> showFullTicket(payment));
        layout.addView(btnDetails);

        card.addView(layout);
        return card;
    }

    private void showFullTicket(Payment payment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ticket de Compra");

        StringBuilder ticket = new StringBuilder();
        ticket.append("═══════════════════════════\n");
        ticket.append("       BIBLIOCLOUD\n");
        ticket.append("═══════════════════════════\n\n");
        ticket.append("Sucursal: ").append(payment.getBranchName()).append("\n");
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

    private void updateStatistics(int totalTransactions, double totalPaid) {
        tvTotalTransactions.setText("Transacciones: " + totalTransactions);
        tvTotalPaid.setText(String.format("Total pagado: $%.2f MXN", totalPaid));
    }

    private void showEmptyState() {
        layoutEmpty.setVisibility(View.VISIBLE);
        layoutPaymentsList.removeAllViews();

        TextView tvEmpty = new TextView(this);
        tvEmpty.setText("No tienes pagos registrados\n\nTus compras aparecerán aquí una vez realizadas");
        tvEmpty.setTextSize(16);
        tvEmpty.setPadding(32, 32, 32, 32);
        tvEmpty.setGravity(android.view.Gravity.CENTER);
        layoutPaymentsList.addView(tvEmpty);
    }
}