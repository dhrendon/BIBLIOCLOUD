package com.example.bibliocloud;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class TicketsViewerActivity extends AppCompatActivity {

    private LinearLayout layoutTicketsList;
    private TextView tvTotalTickets;
    private LinearLayout layoutEmpty;
    private MaterialButton btnBack;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<TicketData> allTickets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tickets_viewer);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initializeViews();
        setupToolbar();
        loadTickets();
    }

    private void initializeViews() {
        layoutTicketsList = findViewById(R.id.layoutTicketsList);
        tvTotalTickets = findViewById(R.id.tvTotalTickets);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        btnBack = findViewById(R.id.btnBack);

        allTickets = new ArrayList<>();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("🎫 Mis Tickets");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadTickets() {
        String userId = auth.getCurrentUser().getUid();

        db.collection("tickets")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        Toast.makeText(this, "Error al cargar tickets", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allTickets.clear();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String orderId = doc.getString("orderId");
                        String ticketContent = doc.getString("ticketContent");
                        Long timestamp = doc.getLong("timestamp");

                        if (orderId != null && ticketContent != null && timestamp != null) {
                            allTickets.add(new TicketData(orderId, ticketContent, timestamp));
                        }
                    }

                    displayTickets();
                });
    }

    private void displayTickets() {
        layoutTicketsList.removeAllViews();

        if (allTickets.isEmpty()) {
            showEmptyState();
            return;
        }

        layoutEmpty.setVisibility(View.GONE);
        tvTotalTickets.setText("Total de tickets: " + allTickets.size());

        for (TicketData ticket : allTickets) {
            layoutTicketsList.addView(createTicketCard(ticket));
        }
    }

    private CardView createTicketCard(TicketData ticket) {
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
        tvOrderId.setText("🎫 Ticket #" + ticket.orderId.substring(0, 8).toUpperCase());
        tvOrderId.setTextSize(16);
        tvOrderId.setTypeface(null, android.graphics.Typeface.BOLD);
        tvOrderId.setTextColor(getResources().getColor(R.color.colorPrimary));
        layout.addView(tvOrderId);

        // Fecha
        TextView tvDate = new TextView(this);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        tvDate.setText("📅 " + sdf.format(new Date(ticket.timestamp)));
        tvDate.setTextSize(14);
        tvDate.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        layout.addView(tvDate);

        // Botones
        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setPadding(0, 12, 0, 0);

        Button btnViewTicket = new Button(this);
        btnViewTicket.setText("Ver Ticket");
        btnViewTicket.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        btnViewTicket.setTextColor(getResources().getColor(R.color.white));
        btnViewTicket.setOnClickListener(v -> mostrarTicketDialog(ticket));
        buttonsLayout.addView(btnViewTicket);

        Button btnShare = new Button(this);
        btnShare.setText("📤 Compartir");
        btnShare.setBackgroundColor(getResources().getColor(R.color.green));
        btnShare.setTextColor(getResources().getColor(R.color.white));
        LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        shareParams.setMargins(8, 0, 0, 0);
        btnShare.setLayoutParams(shareParams);
        btnShare.setOnClickListener(v -> compartirTicket(ticket.ticketContent));
        buttonsLayout.addView(btnShare);

        layout.addView(buttonsLayout);
        card.addView(layout);
        return card;
    }

    private void mostrarTicketDialog(TicketData ticket) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎫 Ticket de Compra");
        builder.setMessage(ticket.ticketContent);

        builder.setPositiveButton("✅ Cerrar", null);

        builder.setNeutralButton("📤 Compartir", (dialog, which) -> {
            compartirTicket(ticket.ticketContent);
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void compartirTicket(String ticketContent) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "BiblioCloud - Ticket de Compra");
        shareIntent.putExtra(Intent.EXTRA_TEXT, ticketContent);
        startActivity(Intent.createChooser(shareIntent, "Compartir ticket"));
    }

    private void showEmptyState() {
        layoutEmpty.setVisibility(View.VISIBLE);
        layoutTicketsList.removeAllViews();

        TextView tvEmpty = new TextView(this);
        tvEmpty.setText("📭 No tienes tickets guardados\n\nLos tickets se generan automáticamente al realizar una compra");
        tvEmpty.setTextSize(16);
        tvEmpty.setPadding(32, 32, 32, 32);
        tvEmpty.setGravity(android.view.Gravity.CENTER);
        tvEmpty.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        layoutTicketsList.addView(tvEmpty);
    }

    // Clase auxiliar para almacenar datos del ticket
    private static class TicketData {
        String orderId;
        String ticketContent;
        long timestamp;

        TicketData(String orderId, String ticketContent, long timestamp) {
            this.orderId = orderId;
            this.ticketContent = ticketContent;
            this.timestamp = timestamp;
        }
    }
}