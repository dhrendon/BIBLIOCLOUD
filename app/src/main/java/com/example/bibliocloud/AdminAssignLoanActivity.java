package com.example.bibliocloud;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.bibliocloud.models.Loan;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activity para que el ADMIN asigne préstamos a usuarios.
 * Requiere colecciones Firestore: usuarios, libros, inventario, sucursales, prestamos.
 */
public class AdminAssignLoanActivity extends AppCompatActivity {

    private Spinner spinnerUsuario;
    private Spinner spinnerLibro;
    private EditText etLoanDays;
    private EditText etNotes;
    private Spinner spinnerBranch;
    private Spinner spinnerLoanType;
    private TextView tvUserInfo;
    private TextView tvBookInfo;
    private TextView tvReturnDate;
    private MaterialButton btnAssignLoan;
    private MaterialButton btnCancel;
    private CheckBox cbSendNotification;

    private FirebaseFirestore db;
    private List<Map<String, String>> usersList;
    private List<Map<String, String>> booksList;
    private List<Map<String, String>> branchesList;

    private String selectedUserId;
    private String selectedUserName;
    private String selectedUserEmail;

    private String selectedBookId;
    private String selectedBookTitle;
    private String selectedBookAuthor;

    private String selectedInventoryId;
    private String selectedBranchId;
    private String selectedBranchName;

    private int loanDays = 14;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_assign_loan);

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupToolbar();
        setupListeners();
        loadUsers();
        loadBooks();
        loadBranches(); // carga genérica de sucursales (si aplica)
    }

    private void initializeViews() {
        spinnerUsuario = findViewById(R.id.spinnerUsuario);
        spinnerLibro = findViewById(R.id.spinnerLibro);
        etLoanDays = findViewById(R.id.etLoanDays);
        etNotes = findViewById(R.id.etNotes);
        spinnerBranch = findViewById(R.id.spinnerBranch);
        spinnerLoanType = findViewById(R.id.spinnerLoanType);
        tvUserInfo = findViewById(R.id.tvUserInfo);
        tvBookInfo = findViewById(R.id.tvBookInfo);
        tvReturnDate = findViewById(R.id.tvReturnDate);
        btnAssignLoan = findViewById(R.id.btnAssignLoan);
        btnCancel = findViewById(R.id.btnCancel);
        cbSendNotification = findViewById(R.id.cbSendNotification);

        usersList = new ArrayList<>();
        booksList = new ArrayList<>();
        branchesList = new ArrayList<>();

        if (etLoanDays != null) etLoanDays.setText(String.valueOf(loanDays));
        if (cbSendNotification != null) cbSendNotification.setChecked(true);

        // Setup loan type spinner
        String[] loanTypes = {"Préstamo", "Reserva"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, loanTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spinnerLoanType != null) spinnerLoanType.setAdapter(adapter);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Asignar Préstamo (Admin)");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupListeners() {
        if (btnAssignLoan != null) btnAssignLoan.setOnClickListener(v -> assignLoan());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> finish());

        if (etLoanDays != null) {
            etLoanDays.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateReturnDate();
                }
            });
        }

        if (spinnerLoanType != null) {
            spinnerLoanType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onNothingSelected(AdapterView<?> parent) {}
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // Si es Reserva, deshabilitar días de préstamo
                    if (position == 1) {
                        if (etLoanDays != null) {
                            etLoanDays.setText("0");
                            etLoanDays.setEnabled(false);
                        }
                    } else {
                        if (etLoanDays != null) {
                            etLoanDays.setEnabled(true);
                            if (etLoanDays.getText().toString().trim().isEmpty()) {
                                etLoanDays.setText("14");
                            }
                        }
                    }
                    updateReturnDate();
                }
            });
        }
    }

    private void loadUsers() {
        db.collection("usuarios")
                .whereEqualTo("rol", "usuario")
                .get()
                .addOnSuccessListener(snapshots -> {
                    usersList.clear();
                    List<String> userNames = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots) {
                        String id = doc.getId();
                        String name = doc.getString("nombre");
                        String email = doc.getString("correo");

                        if (name != null && email != null) {
                            Map<String, String> u = new HashMap<>();
                            u.put("id", id);
                            u.put("name", name);
                            u.put("email", email);
                            usersList.add(u);

                            userNames.add(name + " (" + email + ")");
                        }
                    }

                    if (spinnerUsuario != null) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this, android.R.layout.simple_spinner_item, userNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerUsuario.setAdapter(adapter);

                        spinnerUsuario.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override public void onNothingSelected(AdapterView<?> parent) {}
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                if (position >= 0 && position < usersList.size()) {
                                    Map<String, String> u = usersList.get(position);
                                    selectedUserId = u.get("id");
                                    selectedUserName = u.get("name");
                                    selectedUserEmail = u.get("email");
                                    if (tvUserInfo != null) {
                                        tvUserInfo.setText("Usuario: " + selectedUserName + "\nCorreo: " + selectedUserEmail);
                                        tvUserInfo.setVisibility(View.VISIBLE);
                                    }
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> Log.e("AdminAssignLoan", "Error loadUsers", e));
    }

    private void loadBooks() {
        db.collection("libros")
                .get()
                .addOnSuccessListener(snapshots -> {
                    booksList.clear();
                    List<String> bookNames = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots) {
                        String id = doc.getId();
                        String title = doc.getString("titulo");
                        String author = doc.getString("autor");

                        if (title != null && author != null) {
                            Map<String, String> b = new HashMap<>();
                            b.put("id", id);
                            b.put("title", title);
                            b.put("author", author);
                            booksList.add(b);

                            bookNames.add(title + " - " + author);
                        }
                    }

                    if (spinnerLibro != null) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this, android.R.layout.simple_spinner_item, bookNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerLibro.setAdapter(adapter);

                        spinnerLibro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override public void onNothingSelected(AdapterView<?> parent) {}
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                if (position >= 0 && position < booksList.size()) {
                                    Map<String, String> b = booksList.get(position);
                                    selectedBookId = b.get("id");
                                    selectedBookTitle = b.get("title");
                                    selectedBookAuthor = b.get("author");

                                    if (tvBookInfo != null) {
                                        tvBookInfo.setText("Libro: " + selectedBookTitle + "\nAutor: " + selectedBookAuthor);
                                        tvBookInfo.setVisibility(View.VISIBLE);
                                    }

                                    // Cargar sucursales donde esté disponible este libro
                                    loadBranchesForBook(selectedBookId);
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> Log.e("AdminAssignLoan", "Error loadBooks", e));
    }

    private void loadBranches() {
        db.collection("sucursales")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snapshots -> {
                    branchesList.clear();
                    List<String> branchNames = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots) {
                        String branchId = doc.getId();
                        String branchName = doc.getString("name");

                        if (branchName != null) {
                            Map<String, String> branch = new HashMap<>();
                            branch.put("id", branchId);
                            branch.put("name", branchName);
                            branchesList.add(branch);

                            branchNames.add(branchName);
                        }
                    }

                    if (spinnerBranch != null) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_spinner_item, branchNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerBranch.setAdapter(adapter);

                        spinnerBranch.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override public void onNothingSelected(AdapterView<?> parent) {}
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                if (position >= 0 && position < branchesList.size()) {
                                    Map<String, String> selectedBranch = branchesList.get(position);
                                    selectedBranchId = selectedBranch.get("id");
                                    selectedBranchName = selectedBranch.get("name");
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> Log.e("AdminAssignLoan", "Error loadBranches", e));
    }

    private void loadBranchesForBook(String bookId) {
        if (bookId == null) return;

        db.collection("inventario")
                .whereEqualTo("bookId", bookId)
                .whereGreaterThan("availablePhysical", 0)
                .get()
                .addOnSuccessListener(snapshots -> {
                    branchesList.clear();
                    List<String> branchNames = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots) {
                        String inventoryId = doc.getId();
                        String branchId = doc.getString("branchId");
                        String branchName = doc.getString("branchName");
                        Long availableStock = doc.getLong("availablePhysical");

                        if (branchName != null && availableStock != null) {
                            Map<String, String> branch = new HashMap<>();
                            branch.put("id", branchId);
                            branch.put("name", branchName);
                            branch.put("inventoryId", inventoryId);
                            branch.put("stock", String.valueOf(availableStock));
                            branchesList.add(branch);

                            branchNames.add(branchName + " (" + availableStock + " disponibles)");
                        }
                    }

                    if (branchNames.isEmpty()) {
                        branchNames.add("No disponible en ninguna sucursal");
                    }

                    if (spinnerBranch != null) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_spinner_item, branchNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerBranch.setAdapter(adapter);

                        spinnerBranch.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override public void onNothingSelected(AdapterView<?> parent) {}
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                if (!branchesList.isEmpty() && position >= 0 && position < branchesList.size()) {
                                    Map<String, String> selectedBranch = branchesList.get(position);
                                    selectedBranchId = selectedBranch.get("id");
                                    selectedBranchName = selectedBranch.get("name");
                                    selectedInventoryId = selectedBranch.get("inventoryId");
                                } else {
                                    selectedInventoryId = null;
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> Log.e("AdminAssignLoan", "Error loadBranchesForBook", e));
    }

    private void updateReturnDate() {
        try {
            if (etLoanDays == null || tvReturnDate == null) return;
            String daysText = etLoanDays.getText().toString().trim();
            if (daysText.isEmpty()) {
                tvReturnDate.setVisibility(View.GONE);
                return;
            }
            loanDays = Integer.parseInt(daysText);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, loanDays);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvReturnDate.setText("Fecha de devolución: " + sdf.format(cal.getTime()));
            tvReturnDate.setVisibility(View.VISIBLE);
        } catch (NumberFormatException e) {
            tvReturnDate.setVisibility(View.GONE);
        }
    }

    private void assignLoan() {
        // Validaciones básicas
        if (selectedUserId == null || selectedUserId.isEmpty()) {
            Toast.makeText(this, "Selecciona un usuario", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedBookId == null || selectedBookId.isEmpty()) {
            Toast.makeText(this, "Selecciona un libro", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedBranchId == null || selectedBranchId.isEmpty()) {
            Toast.makeText(this, "Selecciona una sucursal", Toast.LENGTH_SHORT).show();
            return;
        }

        String loanType = spinnerLoanType != null && spinnerLoanType.getSelectedItem() != null
                ? spinnerLoanType.getSelectedItem().toString()
                : "Préstamo";

        if (loanType.equals("Préstamo")) {
            String daysText = etLoanDays != null ? etLoanDays.getText().toString().trim() : "";
            if (daysText.isEmpty()) {
                Toast.makeText(this, "Ingresa días de préstamo válidos", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                loanDays = Integer.parseInt(daysText);
                if (loanDays <= 0) {
                    Toast.makeText(this, "Días de préstamo deben ser mayores a 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException ex) {
                Toast.makeText(this, "Días de préstamo inválidos", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        showConfirmationDialog(loanType);
    }

    private void showConfirmationDialog(String loanType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmar Asignación");

        String message = "Confirmar la asignación de este " + loanType.toLowerCase() + "?\n\n" +
                "Usuario: " + (selectedUserName != null ? selectedUserName : "") + "\n" +
                "Libro: " + (selectedBookTitle != null ? selectedBookTitle : "") + "\n" +
                "Sucursal: " + (selectedBranchName != null ? selectedBranchName : "") + "\n" +
                "Tipo: " + loanType;

        if (loanType.equals("Préstamo")) {
            message += "\nPeríodo: " + loanDays + " días";
        }

        builder.setMessage(message);
        builder.setPositiveButton("Confirmar", (dialog, which) -> createAdminLoan(loanType));
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void createAdminLoan(String loanType) {
        if (btnAssignLoan != null) {
            btnAssignLoan.setEnabled(false);
            btnAssignLoan.setText("Procesando...");
        }

        // Construir objeto Loan (usa tu modelo)
        Loan loan = new Loan(
                selectedUserId,
                selectedUserName,
                selectedUserEmail,
                selectedBookId,
                selectedBookTitle,
                selectedBookAuthor,
                selectedInventoryId,
                selectedBranchId,
                selectedBranchName,
                loanType
        );
        loan.setLoanDays(loanDays);

        String notasAdmin = (etNotes != null) ? etNotes.getText().toString().trim() : "";
        if (notasAdmin.isEmpty()) notasAdmin = "Préstamo asignado por administrador";
        loan.setNotes("[ADMIN] " + notasAdmin);

        // Map con campos para Firestore
        Map<String, Object> loanData = new HashMap<>();
        loanData.put("id_usuario", loan.getUserId());
        loanData.put("nombre_usuario", loan.getUserName());
        loanData.put("correo_usuario", loan.getUserEmail());
        loanData.put("id_libro", loan.getBookId());
        loanData.put("titulo_libro", loan.getBookTitle());
        loanData.put("autor_libro", loan.getBookAuthor());
        loanData.put("id_inventario", loan.getInventoryId());
        loanData.put("id_sucursal", loan.getBranchId());
        loanData.put("nombre_sucursal", loan.getBranchName());
        loanData.put("tipo", loan.getType());
        loanData.put("estado", loan.getStatus()); // espera que Loan devuelva "Activo" o similar
        loanData.put("fecha_prestamo", loan.getLoanDate());
        loanData.put("fecha_devolucion", loan.getReturnDate());
        loanData.put("fecha_devolucion_real", loan.getActualReturnDate());
        loanData.put("dias_prestamo", loan.getLoanDays());
        loanData.put("multa", loan.getFine());
        loanData.put("notas", loan.getNotes());
        loanData.put("timestamp", loan.getTimestamp());
        loanData.put("asignado_por_admin", true);

        Log.d("AdminAssignLoan", "Guardando préstamo: " + loanData);

        db.collection("prestamos")
                .add(loanData)
                .addOnSuccessListener(docRef -> {
                    Log.d("AdminAssignLoan", "Préstamo guardado con ID: " + docRef.getId());

                    if ("Préstamo".equals(loanType)) {
                        updateInventoryStock();
                    }

                    Toast.makeText(this, loanType + " asignado exitosamente", Toast.LENGTH_LONG).show();

                    if (cbSendNotification != null && cbSendNotification.isChecked()) {
                        sendNotificationToUser(loan);
                    }

                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminAssignLoan", "Error guardando préstamo", e);
                    if (btnAssignLoan != null) {
                        btnAssignLoan.setEnabled(true);
                        btnAssignLoan.setText("Asignar Préstamo");
                    }
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateInventoryStock() {
        if (selectedInventoryId == null) return;

        db.collection("inventario").document(selectedInventoryId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long currentStock = doc.getLong("availablePhysical");
                        if (currentStock != null && currentStock > 0) {
                            int newStock = currentStock.intValue() - 1;
                            db.collection("inventario").document(selectedInventoryId)
                                    .update("availablePhysical", Math.max(0, newStock));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("AdminAssignLoan", "Error updateInventoryStock", e));
    }

    private void sendNotificationToUser(Loan loan) {
        // Aquí puedes integrar FCM o correo si tienes la infraestructura.
        // Por ahora mostramos un Toast para confirmar la acción.
        if (selectedUserEmail != null) {
            Toast.makeText(this, "Notificación enviada a " + selectedUserEmail, Toast.LENGTH_SHORT).show();
        }
    }
}
