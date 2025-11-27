package com.example.bibliocloud;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserManagementActivity extends AppCompatActivity {

    private static final String TAG = "UserManagementActivity";

    private FirebaseFirestore db;
    private LinearLayout layoutListaUsuarios;

    private TextView tvTotalUsers;
    private TextView tvTotalCashiers;
    private TextView tvTotalAdmins;

    private com.google.android.material.textfield.TextInputEditText etName;
    private com.google.android.material.textfield.TextInputEditText etEmail;
    private com.google.android.material.textfield.TextInputEditText etPassword;
    private com.google.android.material.textfield.TextInputEditText etConfirmPassword;
    private com.google.android.material.textfield.TextInputEditText etPhone;
    private com.google.android.material.textfield.TextInputEditText etDireccion;
    private Spinner spinnerUserType;
    private LinearLayout layoutBranchSelector;
    private Spinner spinnerBranch;
    private android.widget.CheckBox checkboxTerms;
    private com.google.android.material.button.MaterialButton btnRegister;
    private com.google.android.material.button.MaterialButton btnGoToLogin;

    private ArrayList<String> listaSucursales = new ArrayList<>();
    private ArrayList<String> listaSucursalesIds = new ArrayList<>();
    private ArrayAdapter<String> branchAdapter;

    private String rolActualLogueado = "";

    private int totalUsuarios = 0;
    private int totalCajeros = 0;
    private int totalAdministradores = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        db = FirebaseFirestore.getInstance();

        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvTotalCashiers = findViewById(R.id.tvTotalCashiers);
        tvTotalAdmins = findViewById(R.id.tvTotalAdmins);

        layoutListaUsuarios = findViewById(R.id.layoutListaUsuarios);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etPhone = findViewById(R.id.etPhone);
        etDireccion = findViewById(R.id.etDireccion);
        spinnerUserType = findViewById(R.id.spinnerUserType);
        layoutBranchSelector = findViewById(R.id.layoutBranchSelector);
        spinnerBranch = findViewById(R.id.spinnerBranch);
        checkboxTerms = findViewById(R.id.checkboxTerms);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);

        layoutBranchSelector.setVisibility(View.GONE);

        ArrayAdapter<String> adapterRoles = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"cajero"});
        adapterRoles.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserType.setAdapter(adapterRoles);

        spinnerUserType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String tipo = parent.getItemAtPosition(position).toString().toLowerCase();
                if ("cajero".equals(tipo)) layoutBranchSelector.setVisibility(View.VISIBLE);
                else layoutBranchSelector.setVisibility(View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        cargarSucursales();
        verificarRolActual();

        btnRegister.setOnClickListener(v -> registrarCajero());

        if (btnGoToLogin != null) {
            btnGoToLogin.setOnClickListener(v -> finish());
        }

        // ✅ NO cargar usuarios aquí, onResume() lo hará
    }

    private void verificarRolActual() {
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current == null) {
            Toast.makeText(this, "No hay usuario autenticado", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String uid = current.getUid();
        db.collection("usuarios").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String rol = doc.getString("rol");
                        if (rol == null || !"administrador".equals(rol)) {
                            Toast.makeText(this, "Acceso denegado: se requiere administrador", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }
                        rolActualLogueado = rol;
                        Log.d(TAG, "✅ Usuario verificado como: " + rol);
                    } else {
                        Toast.makeText(this, "Perfil no encontrado. Acceso denegado.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al verificar rol", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void cargarSucursales() {
        db.collection("sucursales")
                .get()
                .addOnSuccessListener(query -> {
                    listaSucursales.clear();
                    listaSucursalesIds.clear();

                    for (DocumentSnapshot doc : query) {
                        String nombre = doc.getString("nombre");
                        if (nombre == null) nombre = doc.getString("name");

                        if (nombre != null) {
                            listaSucursales.add(nombre);
                            listaSucursalesIds.add(doc.getId());
                        }
                    }

                    if (listaSucursales.isEmpty()) {
                        listaSucursales.add("Sin sucursales");
                        listaSucursalesIds.add("");
                    }

                    branchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, listaSucursales);
                    branchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerBranch.setAdapter(branchAdapter);

                    Log.d(TAG, "✅ Sucursales cargadas: " + listaSucursales.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al cargar sucursales: " + e.getMessage());
                    listaSucursales.clear();
                    listaSucursales.add("Sin sucursales");
                    listaSucursalesIds.clear();
                    listaSucursalesIds.add("");
                    branchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, listaSucursales);
                    spinnerBranch.setAdapter(branchAdapter);
                });
    }

    private void registrarCajero() {
        String nombre = etName.getText() != null ? etName.getText().toString().trim() : "";
        String correo = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String pass = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirm = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";
        String telefono = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String direccion = etDireccion.getText() != null ? etDireccion.getText().toString().trim() : "";
        String rol = spinnerUserType.getSelectedItem() != null ? spinnerUserType.getSelectedItem().toString().toLowerCase() : "cajero";

        int sucursalPos = spinnerBranch.getSelectedItemPosition();
        String sucursalNombre = sucursalPos >= 0 && sucursalPos < listaSucursales.size() ?
                listaSucursales.get(sucursalPos) : "";
        String sucursalId = sucursalPos >= 0 && sucursalPos < listaSucursalesIds.size() ?
                listaSucursalesIds.get(sucursalPos) : "";

        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(correo) || TextUtils.isEmpty(pass) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pass.equals(confirm)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!checkboxTerms.isChecked()) {
            Toast.makeText(this, "Debes aceptar los términos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!"cajero".equals(rol)) {
            Toast.makeText(this, "Solo se pueden crear cajeros desde aquí", Toast.LENGTH_SHORT).show();
            return;
        }
        if (sucursalId.isEmpty()) {
            Toast.makeText(this, "⚠️ Selecciona una sucursal válida", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Registrando...");

        try {
            FirebaseApp defaultApp = FirebaseApp.getInstance();
            FirebaseOptions options = defaultApp.getOptions();
            String secondaryName = "secondary_" + System.currentTimeMillis();

            FirebaseApp secondaryApp = FirebaseApp.initializeApp(this, options, secondaryName);
            FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);

            secondaryAuth.createUserWithEmailAndPassword(correo, pass)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser newUser = authResult.getUser();
                        if (newUser == null) {
                            btnRegister.setEnabled(true);
                            btnRegister.setText("Registrar Usuario");
                            Toast.makeText(this, "Error: no se creó el usuario", Toast.LENGTH_SHORT).show();
                            secondaryAuth.signOut();
                            secondaryApp.delete();
                            return;
                        }

                        String newUid = newUser.getUid();
                        Map<String, Object> datos = new HashMap<>();
                        datos.put("nombre", nombre);
                        datos.put("correo", correo);
                        datos.put("telefono", telefono);
                        datos.put("direccion", direccion);
                        datos.put("rol", "cajero");
                        datos.put("nombre_sucursal", sucursalNombre);
                        datos.put("sucursal_id", sucursalId);

                        Log.d(TAG, "📝 Guardando cajero:");
                        Log.d(TAG, "   - Nombre: " + nombre);
                        Log.d(TAG, "   - Sucursal: " + sucursalNombre);
                        Log.d(TAG, "   - Sucursal ID: " + sucursalId);

                        db.collection("usuarios").document(newUid)
                                .set(datos)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "✅ Cajero registrado correctamente", Toast.LENGTH_LONG).show();
                                    secondaryAuth.signOut();
                                    secondaryApp.delete();
                                    cargarUsuarios();
                                    btnRegister.setEnabled(true);
                                    btnRegister.setText("Registrar Usuario");
                                    limpiarCamposFormulario();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    secondaryAuth.signOut();
                                    secondaryApp.delete();
                                    btnRegister.setEnabled(true);
                                    btnRegister.setText("Registrar Usuario");
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al crear usuario: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        try {
                            secondaryApp.delete();
                        } catch (Exception ignored) {
                        }
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Registrar Usuario");
                    });

        } catch (Exception ex) {
            btnRegister.setEnabled(true);
            btnRegister.setText("Registrar Usuario");
            Toast.makeText(this, "Error interno: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void limpiarCamposFormulario() {
        etName.setText("");
        etEmail.setText("");
        etPassword.setText("");
        etConfirmPassword.setText("");
        etPhone.setText("");
        etDireccion.setText("");
        checkboxTerms.setChecked(false);
        spinnerBranch.setSelection(0);
    }

    private void cargarUsuarios() {
        Log.d(TAG, "🔄 Cargando usuarios...");

        // ✅ Limpiar UI y contadores ANTES de la consulta
        layoutListaUsuarios.removeAllViews();
        totalUsuarios = 0;
        totalCajeros = 0;
        totalAdministradores = 0;

        db.collection("usuarios")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "✅ Usuarios obtenidos: " + queryDocumentSnapshots.size());

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> usuario = doc.getData();
                        if (usuario != null) {
                            String rol = (String) usuario.get("rol");
                            if (rol == null) rol = "usuario";

                            totalUsuarios++;

                            if ("cajero".equalsIgnoreCase(rol)) {
                                totalCajeros++;
                            } else if ("administrador".equalsIgnoreCase(rol)) {
                                totalAdministradores++;
                            }

                            CardView card = crearCardUsuario(doc.getId(), usuario);
                            layoutListaUsuarios.addView(card);
                        }
                    }

                    actualizarEstadisticas();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error cargando usuarios: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show();
                });
    }

    private void actualizarEstadisticas() {
        if (tvTotalUsers == null || tvTotalCashiers == null || tvTotalAdmins == null) {
            Log.e(TAG, "❌ TextViews de estadísticas son NULL");
            return;
        }

        Log.d(TAG, "📊 Actualizando estadísticas:");
        Log.d(TAG, "   - Total usuarios: " + totalUsuarios);
        Log.d(TAG, "   - Total cajeros: " + totalCajeros);
        Log.d(TAG, "   - Total administradores: " + totalAdministradores);

        tvTotalUsers.setText("👥 Usuarios: " + totalUsuarios);
        tvTotalCashiers.setText("👔 Cajeros: " + totalCajeros);
        tvTotalAdmins.setText("🔑 Administradores: " + totalAdministradores);

        tvTotalUsers.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        tvTotalCashiers.setTextColor(getResources().getColor(R.color.colorPrimary));
        tvTotalAdmins.setTextColor(getResources().getColor(R.color.dark_brown));
    }

    private CardView crearCardUsuario(String userId, Map<String, Object> user) {
        CardView card = new CardView(this);
        card.setRadius(16);
        card.setCardElevation(8);
        card.setCardBackgroundColor(getResources().getColor(R.color.light_brown));

        LinearLayout.LayoutParams cparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cparams.setMargins(0, 0, 0, 24);
        card.setLayoutParams(cparams);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        TextView tvNombre = new TextView(this);
        String nombre = (String) user.get("nombre");
        if (nombre == null) nombre = "Sin nombre";
        tvNombre.setText("👤 " + nombre);
        tvNombre.setTextSize(16);
        tvNombre.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        tvNombre.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvNombre);

        TextView tvCorreo = new TextView(this);
        String correo = (String) user.get("correo");
        if (correo == null) correo = "Sin correo";
        tvCorreo.setText("📧 " + correo);
        tvCorreo.setTextSize(14);
        tvCorreo.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        layout.addView(tvCorreo);

        String rol = (String) user.get("rol");
        if (rol == null) rol = "usuario";
        TextView tvRol = new TextView(this);
        String rolTexto = rol.equals("administrador") ? "Administrador" :
                rol.equals("cajero") ? "Cajero" : "Usuario";
        tvRol.setText("🔑 Rol: " + rolTexto);
        tvRol.setTextSize(14);
        tvRol.setTextColor(getResources().getColor(R.color.colorPrimary));
        tvRol.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvRol);

        if ("cajero".equals(rol)) {
            String sucursal = (String) user.get("nombre_sucursal");
            if (sucursal != null && !sucursal.isEmpty()) {
                TextView tvSucursal = new TextView(this);
                tvSucursal.setText("🏢 Sucursal: " + sucursal);
                tvSucursal.setTextSize(14);
                tvSucursal.setTextColor(getResources().getColor(R.color.colorTextSecondary));
                layout.addView(tvSucursal);
            }
        }

        card.addView(layout);

        card.setOnClickListener(v -> {
            Intent i = new Intent(this, UserEditActivity.class);
            i.putExtra("userId", userId);
            i.putExtra("CURRENT_ROLE", rolActualLogueado);
            startActivity(i);
        });

        return card;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "📱 onResume() - Cargando usuarios");
        cargarUsuarios();
    }
}