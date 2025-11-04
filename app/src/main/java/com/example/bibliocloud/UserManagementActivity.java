// UserManagementActivity.java
package com.example.bibliocloud;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class UserManagementActivity extends AppCompatActivity {

    private EditText etNombre, etEmail, etPassword, etTelefono, etDireccion;
    private MaterialButton btnRegistrarUsuario, btnVolver;
    private LinearLayout layoutListaUsuarios;

    private FirebaseAuth mAuth;              // 🔹 Firebase Authentication
    private FirebaseFirestore db;            // 🔹 Firestore Database

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        // === Inicializar Firebase ===
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupButtonListeners();

        // === Cargar usuarios al iniciar ===
        cargarListaUsuarios();
    }

    private void initializeViews() {
        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etTelefono = findViewById(R.id.etTelefono);
        etDireccion = findViewById(R.id.etDireccion);
        btnRegistrarUsuario = findViewById(R.id.btnRegistrarUsuario);
        btnVolver = findViewById(R.id.btnVolver);
        layoutListaUsuarios = findViewById(R.id.layoutListaUsuarios);
    }

    private void setupButtonListeners() {
        btnRegistrarUsuario.setOnClickListener(v -> registrarUsuario());
        btnVolver.setOnClickListener(v -> finish());
    }

    private void registrarUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();

        if (!validarDatosUsuario(nombre, email, password, telefono)) return;

        // 🔹 Crear usuario en Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser nuevoUsuario = authResult.getUser();

                    // 🔹 Guardar datos adicionales en Firestore
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("uid", nuevoUsuario.getUid());
                    userMap.put("nombre", nombre);
                    userMap.put("email", email);
                    userMap.put("telefono", telefono);
                    userMap.put("direccion", direccion);
                    userMap.put("rol", "Usuario");

                    db.collection("usuarios").document(nuevoUsuario.getUid())
                            .set(userMap)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show();
                                limpiarFormulario();
                                cargarListaUsuarios();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error al guardar en Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );

                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al crear usuario: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void cargarListaUsuarios() {
        layoutListaUsuarios.removeAllViews();

        db.collection("usuarios")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView tvEmpty = new TextView(this);
                        tvEmpty.setText("No hay usuarios registrados");
                        tvEmpty.setTextSize(16);
                        tvEmpty.setPadding(0, 32, 0, 0);
                        layoutListaUsuarios.addView(tvEmpty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String uid = doc.getId();
                        String nombre = doc.getString("nombre");
                        String email = doc.getString("email");
                        String rol = doc.getString("rol");
                        String telefono = doc.getString("telefono");
                        String direccion = doc.getString("direccion");

                        layoutListaUsuarios.addView(
                                crearCardUsuario(uid, nombre, email, rol, telefono, direccion)
                        );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al cargar usuarios: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private CardView crearCardUsuario(String uid, String nombre, String email, String rol, String telefono, String direccion) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, 16);
        cardView.setLayoutParams(layoutParams);
        cardView.setCardElevation(4);
        cardView.setRadius(8);
        cardView.setCardBackgroundColor(getResources().getColor(R.color.light_brown));

        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setPadding(16, 16, 16, 16);

        // Nombre
        TextView tvNombre = new TextView(this);
        tvNombre.setText(nombre);
        tvNombre.setTextSize(18);
        tvNombre.setTypeface(null, android.graphics.Typeface.BOLD);
        cardLayout.addView(tvNombre);

        // Email
        TextView tvEmail = new TextView(this);
        tvEmail.setText(email);
        cardLayout.addView(tvEmail);

        // Teléfono
        TextView tvTelefono = new TextView(this);
        tvTelefono.setText("Tel: " + telefono);
        cardLayout.addView(tvTelefono);

        // Dirección
        TextView tvDireccion = new TextView(this);
        tvDireccion.setText("Dir: " + direccion);
        cardLayout.addView(tvDireccion);

        // Rol
        TextView tvRol = new TextView(this);
        tvRol.setText("Rol: " + rol);
        cardLayout.addView(tvRol);

        // === Botones de acción ===
        LinearLayout layoutBotones = new LinearLayout(this);
        layoutBotones.setOrientation(LinearLayout.HORIZONTAL);
        layoutBotones.setPadding(0, 12, 0, 0);

        Button btnEliminar = new Button(this);
        btnEliminar.setText("Eliminar");
        btnEliminar.setBackgroundColor(getResources().getColor(R.color.red));
        btnEliminar.setTextColor(getResources().getColor(R.color.white));
        btnEliminar.setOnClickListener(v -> eliminarUsuario(uid, email));

        layoutBotones.addView(btnEliminar);
        cardLayout.addView(layoutBotones);

        cardView.addView(cardLayout);
        return cardView;
    }

    private void eliminarUsuario(String uid, String email) {
        // 🔹 Eliminar de Firestore
        db.collection("usuarios").document(uid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Usuario eliminado de Firestore", Toast.LENGTH_SHORT).show();
                    cargarListaUsuarios();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al eliminar en Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );

        // 🔹 Nota: FirebaseAuth no permite borrar cuentas de otros usuarios directamente
        // El admin podría hacerlo desde el panel de Firebase o con Cloud Functions.
    }

    private boolean validarDatosUsuario(String nombre, String email, String password, String telefono) {
        if (nombre.isEmpty()) {
            etNombre.setError("Ingrese el nombre completo");
            return false;
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Ingrese un email válido");
            return false;
        }
        if (password.isEmpty() || password.length() < 6) {
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            return false;
        }
        if (telefono.isEmpty() || telefono.length() < 8) {
            etTelefono.setError("Ingrese un número de teléfono válido");
            return false;
        }
        return true;
    }

    private void limpiarFormulario() {
        etNombre.setText("");
        etEmail.setText("");
        etPassword.setText("");
        etTelefono.setText("");
        etDireccion.setText("");
        etNombre.requestFocus();
    }
}
