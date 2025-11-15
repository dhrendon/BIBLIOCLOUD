package com.example.bibliocloud;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserManagementActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout layoutListaUsuarios;

    // Form fields
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

    // Branches list
    private ArrayList<String> listaSucursales = new ArrayList<>();
    private ArrayAdapter<String> branchAdapter;

    // Role of logged user (to protect access)
    private String rolActualLogueado = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        db = FirebaseFirestore.getInstance();

        // UI refs
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

        // Inicialmente ocultar selector de sucursal
        layoutBranchSelector.setVisibility(View.GONE);

        // Configurar spinner de roles (para admin solo "Cajero")
        ArrayAdapter<String> adapterRoles = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"cajero"}); // Forzamos que el admin solo pueda crear cajeros
        adapterRoles.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserType.setAdapter(adapterRoles);

        // Listener para mostrar sucursales si es cajero
        spinnerUserType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String tipo = parent.getItemAtPosition(position).toString().toLowerCase();
                if ("cajero".equals(tipo)) layoutBranchSelector.setVisibility(View.VISIBLE);
                else layoutBranchSelector.setVisibility(View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Cargar sucursales (spinner)
        cargarSucursales();

        // Verificar rol del usuario logueado — si no es administrador, cerrar
        verificarRolActual();

        // Registrar cajero
        btnRegister.setOnClickListener(v -> registrarCajero());

        // Cargar lista de usuarios
        cargarUsuarios();
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
                    for (DocumentSnapshot doc : query) {
                        String nombre = doc.getString("name");
                        if (nombre != null) listaSucursales.add(nombre);
                    }
                    if (listaSucursales.isEmpty()) listaSucursales.add("Sin sucursales");

                    branchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, listaSucursales);
                    branchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerBranch.setAdapter(branchAdapter);
                })
                .addOnFailureListener(e -> {
                    listaSucursales.clear();
                    listaSucursales.add("Sin sucursales");
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
        String sucursal = spinnerBranch.getSelectedItem() != null ? spinnerBranch.getSelectedItem().toString() : "";

        // Validaciones
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

        btnRegister.setEnabled(false);
        btnRegister.setText("Registrando...");

        // Crear usuario usando una instancia secundaria de FirebaseAuth para NO desconectar al admin
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
                            // cleanup
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
                        datos.put("nombre_sucursal", sucursal);

                        db.collection("usuarios").document(newUid)
                                .set(datos)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Cajero registrado correctamente", Toast.LENGTH_LONG).show();
                                    // cleanup secondary
                                    secondaryAuth.signOut();
                                    secondaryApp.delete();
                                    // refrescar lista
                                    cargarUsuarios();
                                    btnRegister.setEnabled(true);
                                    btnRegister.setText("Registrar Usuario");
                                    // limpiar campos
                                    limpiarCamposFormulario();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error al guardar en Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    secondaryAuth.signOut();
                                    secondaryApp.delete();
                                    btnRegister.setEnabled(true);
                                    btnRegister.setText("Registrar Usuario");
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al crear usuario Auth: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        try { secondaryApp.delete(); } catch (Exception ignored) {}
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
        // Evitar views duplicadas
        layoutListaUsuarios.removeAllViews();

        db.collection("usuarios")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    layoutListaUsuarios.removeAllViews();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> usuario = doc.getData();
                        if (usuario != null) {
                            CardView card = crearCardUsuario(doc.getId(), usuario);
                            // asegurar que la card no bloquee eventos
                            card.setClickable(true);
                            card.setFocusable(true);
                            layoutListaUsuarios.addView(card);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show());
    }

    private CardView crearCardUsuario(String userId, Map<String, Object> user) {

        CardView card = new CardView(this);
        card.setRadius(16);
        card.setCardElevation(8);

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

        // Nombre
        TextView tvNombre = new TextView(this);
        String nombre = (String) user.get("nombre");
        if (nombre == null) nombre = "Sin nombre";
        tvNombre.setText("👤 " + nombre);
        layout.addView(tvNombre);

        // Correo
        TextView tvCorreo = new TextView(this);
        String correo = (String) user.get("correo");
        if (correo == null) correo = "Sin correo";
        tvCorreo.setText("📧 " + correo);
        layout.addView(tvCorreo);

        // Rol
        String rol = (String) user.get("rol");
        if (rol == null) rol = "usuario";
        TextView tvRol = new TextView(this);
        String rolTexto = rol.equals("administrador") ? "Administrador" :
                rol.equals("cajero") ? "Cajero" : "Usuario";
        tvRol.setText("🔑 Rol: " + rolTexto);
        layout.addView(tvRol);

        // Sucursal
        if ("cajero".equals(rol) && user.containsKey("nombre_sucursal")) {
            TextView tvSucursal = new TextView(this);
            tvSucursal.setText("🏢 Sucursal: " + user.get("nombre_sucursal"));
            layout.addView(tvSucursal);
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
        cargarUsuarios();
    }
}
