package com.example.bibliocloud;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.bibliocloud.models.Loan;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.*;

public class LoanRequestActivity extends AppCompatActivity {

    private TextView tvBookTitle, tvBookAuthor, tvBranch, tvAvailability, tvLoanDays, tvReturnDate;
    private RadioGroup rgLoanType;
    private RadioButton rbLoan, rbReservation;
    private EditText etNotes;
    private CheckBox cbAcceptTerms;
    private MaterialButton btnConfirmRequest, btnCancel;
    private LinearLayout layoutLoanDetails, layoutReservationDetails;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String bookId, inventoryId, bookTitle, bookAuthor, branchId, branchName;
    private int availableStock;
    private int loanDays = 14; // Días de préstamo por defecto

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_request);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        getIntentData();
        initializeViews();
        setupToolbar();
        setupListeners();
        updateReturnDate();
    }

    private void getIntentData() {
        bookId = getIntent().getStringExtra("bookId");
        inventoryId = getIntent().getStringExtra("inventoryId");
        bookTitle = getIntent().getStringExtra("bookTitle");
        bookAuthor = getIntent().getStringExtra("bookAuthor");
        branchId = getIntent().getStringExtra("branchId");
        branchName = getIntent().getStringExtra("branchName");
        availableStock = getIntent().getIntExtra("availableStock", 0);
    }

    private void initializeViews() {
        tvBookTitle = findViewById(R.id.tvBookTitle);
        tvBookAuthor = findViewById(R.id.tvBookAuthor);
        tvBranch = findViewById(R.id.tvBranch);
        tvAvailability = findViewById(R.id.tvAvailability);
        tvLoanDays = findViewById(R.id.tvLoanDays);
        tvReturnDate = findViewById(R.id.tvReturnDate);

        rgLoanType = findViewById(R.id.rgLoanType);
        rbLoan = findViewById(R.id.rbLoan);
        rbReservation = findViewById(R.id.rbReservation);

        etNotes = findViewById(R.id.etNotes);
        cbAcceptTerms = findViewById(R.id.cbAcceptTerms);

        btnConfirmRequest = findViewById(R.id.btnConfirmRequest);
        btnCancel = findViewById(R.id.btnCancel);

        layoutLoanDetails = findViewById(R.id.layoutLoanDetails);
        layoutReservationDetails = findViewById(R.id.layoutReservationDetails);

        // Mostrar información del libro
        tvBookTitle.setText(bookTitle);
        tvBookAuthor.setText("Autor: " + bookAuthor);
        tvBranch.setText("📍 Sucursal: " + branchName);

        if (availableStock > 0) {
            tvAvailability.setText("✅ Disponible (" + availableStock + " copias)");
            tvAvailability.setTextColor(getResources().getColor(R.color.green));
            rbLoan.setEnabled(true);
            rbLoan.setChecked(true);
        } else {
            tvAvailability.setText("⚠️ No disponible - Solo reservas");
            tvAvailability.setTextColor(getResources().getColor(R.color.orange));
            rbLoan.setEnabled(false);
            rbReservation.setChecked(true);
        }

        tvLoanDays.setText(loanDays + " días");
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Solicitar Préstamo");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnConfirmRequest.setOnClickListener(v -> processLoanRequest());
        btnCancel.setOnClickListener(v -> finish());

        rgLoanType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbLoan) {
                layoutLoanDetails.setVisibility(View.VISIBLE);
                layoutReservationDetails.setVisibility(View.GONE);
            } else {
                layoutLoanDetails.setVisibility(View.GONE);
                layoutReservationDetails.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateReturnDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, loanDays);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvReturnDate.setText("Fecha de devolución: " + sdf.format(cal.getTime()));
    }

    private void processLoanRequest() {
        // Validar términos y condiciones
        if (!cbAcceptTerms.isChecked()) {
            Toast.makeText(this, "Debes aceptar los términos y condiciones",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Determinar tipo de solicitud
        boolean isLoan = rbLoan.isChecked();
        String loanType = isLoan ? "Préstamo" : "Reserva";

        // Validar disponibilidad para préstamos
        if (isLoan && availableStock <= 0) {
            Toast.makeText(this, "⚠️ No hay copias disponibles para préstamo",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Mostrar diálogo de confirmación
        showConfirmationDialog(loanType);
    }

    private void showConfirmationDialog(String loanType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmar " + loanType);

        String message = "¿Confirmar solicitud de " + loanType.toLowerCase() + "?\n\n" +
                "Libro: " + bookTitle + "\n" +
                "Sucursal: " + branchName + "\n" +
                "Tipo: " + loanType;

        if (loanType.equals("Préstamo")) {
            message += "\nPeríodo: " + loanDays + " días";
        }

        builder.setMessage(message);

        builder.setPositiveButton("✅ Confirmar", (dialog, which) -> {
            createLoan(loanType);
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void createLoan(String loanType) {
        btnConfirmRequest.setEnabled(false);
        btnConfirmRequest.setText("Procesando...");

        // Obtener datos del usuario
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String userId = auth.getCurrentUser().getUid();
        String userName = prefs.getString("current_user_name", "Usuario");
        String userEmail = auth.getCurrentUser().getEmail();

        // Crear objeto Loan
        Loan loan = new Loan(userId, userName, userEmail, bookId, bookTitle, bookAuthor,
                inventoryId, branchId, branchName, loanType);

        loan.setLoanDays(loanDays);
        loan.setNotes(etNotes.getText().toString().trim());

        // Guardar en Firestore
        db.collection("prestamos")
                .add(loan)
                .addOnSuccessListener(docRef -> {
                    // Actualizar inventario si es préstamo
                    if (loanType.equals("Préstamo")) {
                        updateInventoryStock();
                    }

                    Toast.makeText(this,
                            "✅ " + loanType + " solicitado exitosamente",
                            Toast.LENGTH_LONG).show();

                    showSuccessDialog(loanType, loan);
                })
                .addOnFailureListener(e -> {
                    btnConfirmRequest.setEnabled(true);
                    btnConfirmRequest.setText("Confirmar Solicitud");
                    Toast.makeText(this,
                            "❌ Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void updateInventoryStock() {
        db.collection("inventario").document(inventoryId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long currentStock = doc.getLong("availablePhysical");
                        if (currentStock != null && currentStock > 0) {
                            int newStock = currentStock.intValue() - 1;
                            db.collection("inventario").document(inventoryId)
                                    .update("availablePhysical", Math.max(0, newStock));
                        }
                    }
                });
    }

    private void showSuccessDialog(String loanType, Loan loan) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✅ " + loanType + " Confirmado");

        StringBuilder message = new StringBuilder();
        message.append("Tu solicitud ha sido registrada exitosamente.\n\n");
        message.append("Detalles:\n");
        message.append("━━━━━━━━━━━━━━━━━━━━\n");
        message.append("Libro: ").append(bookTitle).append("\n");
        message.append("Tipo: ").append(loanType).append("\n");
        message.append("Sucursal: ").append(branchName).append("\n");

        if (loanType.equals("Préstamo")) {
            message.append("Fecha de préstamo: ").append(loan.getFormattedLoanDate()).append("\n");
            message.append("Fecha de devolución: ").append(loan.getFormattedReturnDate()).append("\n");
            message.append("━━━━━━━━━━━━━━━━━━━━\n\n");
            message.append("⚠️ Importante:\n");
            message.append("• Recoge el libro en la sucursal indicada\n");
            message.append("• Devuelve a tiempo para evitar multas\n");
            message.append("• Multa: $10 MXN por día de retraso");
        } else {
            message.append("━━━━━━━━━━━━━━━━━━━━\n\n");
            message.append("ℹ️ Tu reserva está pendiente.\n");
            message.append("Te notificaremos cuando el libro esté disponible.");
        }

        builder.setMessage(message.toString());
        builder.setPositiveButton("Aceptar", (dialog, which) -> finish());
        builder.setCancelable(false);
        builder.show();
    }
}