package com.example.bibliocloud;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.bibliocloud.models.Payment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class CashierDailyPaymentsActivity extends AppCompatActivity {

    private Spinner spinnerDateFilter;
    private LinearLayout layoutPaymentsList;
    private TextView tvTotalPayments, tvTotalCash, tvTotalCard, tvTotalAmount;
    private MaterialButton btnBack, btnExportReport;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<Payment> allPayments;
    private String branchId;
    private String branchName;
    private String cashierId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cashier_daily_payments);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        branchId = getIntent().getStringExtra("branchId");
        branchName = getIntent().getStringExtra("branchName");
        cashierId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";

        initializeViews();
        setupDateFilter();
        loadTodayPayments();
    }

    private void initializeViews() {
        spinnerDateFilter = findViewById(R.id.spinnerDateFilter);
        layoutPaymentsList = findViewById(R.id.layoutPaymentsList);
        tvTotalPayments = findViewById(R.id.tvTotalPayments);
        tvTotalCash = findViewById(R.id.tvTotalCash);
        tvTotalCard = findViewById(R.id.tvTotalCard);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        btnBack = findViewById(R.id.btnBack);
        btnExportReport = findViewById(R.id.btnExportReport);

        allPayments = new ArrayList<>();

        btnBack.setOnClickListener(v -> finish());
        btnExportReport.setOnClickListener(v -> exportReport());
    }

    private void setupDateFilter() {
        String[] filters = {"Hoy", "Ayer", "Última semana", "Último mes"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filters);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDateFilter.setAdapter(adapter);

        spinnerDateFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadPaymentsByFilter(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadTodayPayments() {
        loadPaymentsByFilter(0); // Hoy
    }

    private void loadPaymentsByFilter(int filterPosition) {

        // 1. Obtener fechas en string (porque Firestore guarda "dd/MM/yyyy")
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        String today = dateFormat.format(cal.getTime());
        String startDateString = today;

        switch (filterPosition) {
            case 1: // Ayer
                cal.add(Calendar.DAY_OF_YEAR, -1);
                startDateString = dateFormat.format(cal.getTime());
                break;

            case 2: // Última semana
            case 3: // Último mes
                // Estos filtros requieren convertir fechas a clase Date después
                break;
        }

        CollectionReference pagosRef = db.collection("pagos");

        Query query = pagosRef
                .whereEqualTo("estado", "Completado")
                .whereEqualTo("nombre_sucursal", branchName);

        // 2. Filtro HOY o AYER (fecha EXACTA)
        if (filterPosition == 0 || filterPosition == 1) {
            query = query.whereEqualTo("formattedDate", startDateString);
        }

        // 3. Filtro SEMANA o MES (rango usando Date)
        if (filterPosition == 2 || filterPosition == 3) {

            Calendar calStart = Calendar.getInstance();
            if (filterPosition == 2)
                calStart.add(Calendar.DAY_OF_YEAR, -7);
            else
                calStart.add(Calendar.MONTH, -1);

            Date startDate = calStart.getTime();

            // Firestore NO guarda la fecha como Date,
            // así que debemos filtrar manualmente después de traer datos.
            query = query.whereGreaterThan("formattedDate", "00/00/0000");
        }

        query.addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) {
                Toast.makeText(this, "Error al cargar pagos", Toast.LENGTH_SHORT).show();
                return;
            }

            allPayments.clear();



            for (DocumentSnapshot doc : snapshots.getDocuments()) {

                String fecha = doc.getString("formattedDate");
                if (fecha == null) continue;

                // FILTRO semana / mes
                if (filterPosition == 2 || filterPosition == 3) {
                    try {
                        Date fechaPago = dateFormat.parse(fecha);

                        Calendar calStart = Calendar.getInstance();
                        if (filterPosition == 2)
                            calStart.add(Calendar.DAY_OF_YEAR, -7);
                        else
                            calStart.add(Calendar.MONTH, -1);

                        if (fechaPago.before(calStart.getTime()))
                            continue;
                    } catch (Exception ignored) {}
                }

                Payment payment = new Payment();
                payment.setId(doc.getId());
                payment.setAmount(doc.getDouble("monto"));
                payment.setSubtotal(doc.getDouble("subtotal"));
                payment.setTax(doc.getDouble("iva"));
                payment.setPaymentMethod(doc.getString("metodo_pago"));
                payment.setUserName(doc.getString("nombre_usuario"));
                payment.setUserEmail(doc.getString("correo_usuario"));
                payment.setTicketNumber(doc.getString("numero_ticket"));
                payment.setBookTitles(doc.getString("libros"));
                payment.setOrderId(doc.getString("id_orden"));
                payment.setCashierName(doc.getString("nombre_cajero"));

                // *** AGREGAR ESTO ***
                payment.setFormattedDate(doc.getString("formattedDate"));
                payment.setFormattedTime(doc.getString("formattedTime"));
                // *********************

                allPayments.add(payment);
            }



            displayPayments();
            updateStatistics();
        });
    }


    private void displayPayments() {
        layoutPaymentsList.removeAllViews();

        if (allPayments.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No hay pagos registrados en este período");
            tvEmpty.setPadding(16, 32, 16, 16);
            tvEmpty.setTextSize(16);
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            layoutPaymentsList.addView(tvEmpty);
            return;
        }

        for (Payment payment : allPayments) {
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

        int backgroundColor = payment.getPaymentMethod().equals("Efectivo") ?
                getResources().getColor(android.R.color.holo_green_light) :
                getResources().getColor(android.R.color.holo_blue_light);
        card.setCardBackgroundColor(backgroundColor);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        // Número de ticket
        TextView tvTicket = new TextView(this);
        tvTicket.setText("🎫 Ticket: #" + payment.getTicketNumber());
        tvTicket.setTextSize(18);
        tvTicket.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvTicket);

        // Hora
        TextView tvTime = new TextView(this);
        tvTime.setText("🕐 " + payment.getFormattedTime());
        tvTime.setTextSize(14);
        layout.addView(tvTime);

        // Cliente
        TextView tvCustomer = new TextView(this);
        tvCustomer.setText("👤 " + payment.getUserName());
        layout.addView(tvCustomer);

        // Orden
        TextView tvOrder = new TextView(this);
        tvOrder.setText("📋 Orden: " + payment.getOrderId().substring(0, 8).toUpperCase());
        layout.addView(tvOrder);

        // Libro(s)
        TextView tvBooks = new TextView(this);
        tvBooks.setText("📚 " + payment.getBookTitles());
        layout.addView(tvBooks);

        // Método de pago
        TextView tvMethod = new TextView(this);
        String methodIcon = payment.getPaymentMethod().equals("Efectivo") ? "💵" : "💳";
        tvMethod.setText(methodIcon + " " + payment.getPaymentMethod());
        tvMethod.setTextSize(16);
        tvMethod.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvMethod);

        // Monto
        TextView tvAmount = new TextView(this);
        tvAmount.setText(String.format("💰 $%.2f MXN", payment.getAmount()));
        tvAmount.setTextSize(18);
        tvAmount.setTypeface(null, android.graphics.Typeface.BOLD);
        tvAmount.setTextColor(getResources().getColor(R.color.colorPrimary));
        layout.addView(tvAmount);

        // Botón ver ticket
        Button btnViewTicket = new Button(this);
        btnViewTicket.setText("Ver Ticket");
        btnViewTicket.setOnClickListener(v -> showTicketDialog(payment));
        layout.addView(btnViewTicket);

        card.addView(layout);
        return card;
    }

    private void showTicketDialog(Payment payment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ticket de Pago");

        StringBuilder ticket = new StringBuilder();
        ticket.append("═══════════════════════════\n");
        ticket.append("       BIBLIOCLOUD\n");
        ticket.append("═══════════════════════════\n\n");
        ticket.append("Sucursal: ").append(branchName).append("\n");
        ticket.append("Ticket: #").append(payment.getTicketNumber()).append("\n");
        ticket.append("Fecha: ").append(payment.getFormattedDate()).append("\n");
        ticket.append("Hora: ").append(payment.getFormattedTime()).append("\n\n");
        ticket.append("───────────────────────────\n");
        ticket.append("Cliente: ").append(payment.getUserName()).append("\n");
        ticket.append("Email: ").append(payment.getUserEmail()).append("\n\n");
        ticket.append("───────────────────────────\n");
        ticket.append("LIBROS:\n");
        ticket.append(payment.getBookTitles()).append("\n\n");
        ticket.append("───────────────────────────\n");
        ticket.append("Subtotal: $").append(String.format("%.2f", payment.getSubtotal())).append("\n");
        ticket.append("IVA (16%): $").append(String.format("%.2f", payment.getTax())).append("\n");
        ticket.append("TOTAL: $").append(String.format("%.2f", payment.getAmount())).append("\n\n");
        ticket.append("───────────────────────────\n");
        ticket.append("Método de pago: ").append(payment.getPaymentMethod()).append("\n");
        ticket.append("Cajero: ").append(payment.getCashierName()).append("\n\n");
        ticket.append("═══════════════════════════\n");
        ticket.append("   ¡Gracias por su compra!\n");
        ticket.append("═══════════════════════════\n");

        builder.setMessage(ticket.toString());
        builder.setPositiveButton("Cerrar", null);
        builder.setNeutralButton("Compartir", (dialog, which) -> shareTicket(ticket.toString()));
        builder.show();
    }

    private void shareTicket(String ticketContent) {
        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, ticketContent);
        startActivity(android.content.Intent.createChooser(shareIntent, "Compartir ticket"));
    }

    private void updateStatistics() {
        int totalPayments = allPayments.size();
        double totalCash = 0;
        double totalCard = 0;
        double totalAmount = 0;

        for (Payment payment : allPayments) {
            totalAmount += payment.getAmount();
            if (payment.getPaymentMethod().equals("Efectivo")) {
                totalCash += payment.getAmount();
            } else {
                totalCard += payment.getAmount();
            }
        }

        tvTotalPayments.setText("Pagos: " + totalPayments);
        tvTotalCash.setText(String.format("💵 Efectivo: $%.2f", totalCash));
        tvTotalCard.setText(String.format("💳 Tarjeta: $%.2f", totalCard));
        tvTotalAmount.setText(String.format("Total: $%.2f MXN", totalAmount));
    }

    private void exportReport() {
        if (allPayments.isEmpty()) {
            Toast.makeText(this, "No hay pagos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder report = new StringBuilder();
        report.append("REPORTE DE PAGOS - ").append(branchName).append("\n");
        report.append("Fecha: ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date())).append("\n\n");

        double totalCash = 0, totalCard = 0, totalAmount = 0;
        for (Payment payment : allPayments) {
            report.append("Ticket: ").append(payment.getTicketNumber()).append("\n");
            report.append("Cliente: ").append(payment.getUserName()).append("\n");
            report.append("Método: ").append(payment.getPaymentMethod()).append("\n");
            report.append("Monto: $").append(String.format("%.2f", payment.getAmount())).append("\n\n");

            totalAmount += payment.getAmount();
            if (payment.getPaymentMethod().equals("Efectivo")) {
                totalCash += payment.getAmount();
            } else {
                totalCard += payment.getAmount();
            }
        }

        report.append("───────────────────\n");
        report.append("RESUMEN:\n");
        report.append("Total pagos: ").append(allPayments.size()).append("\n");
        report.append("Efectivo: $").append(String.format("%.2f", totalCash)).append("\n");
        report.append("Tarjeta: $").append(String.format("%.2f", totalCard)).append("\n");
        report.append("TOTAL: $").append(String.format("%.2f", totalAmount)).append("\n");

        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, report.toString());
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Reporte de Pagos - BiblioCloud");
        startActivity(android.content.Intent.createChooser(shareIntent, "Exportar reporte"));
    }
}