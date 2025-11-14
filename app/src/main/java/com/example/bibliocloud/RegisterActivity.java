// RegisterActivity.java
package com.example.bibliocloud;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword, etPhone;
    private MaterialButton btnRegister, btnGoToLogin;
    private CheckBox checkboxTerms;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Inicializar vistas
        initializeViews();

        // Configurar listeners
        setupListeners();
    }

    private void initializeViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etPhone = findViewById(R.id.etPhone);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);
        checkboxTerms = findViewById(R.id.checkboxTerms);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegister();
            }
        });

        btnGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToLogin();
            }
        });
    }

    private void attemptRegister() {
        // Obtener valores de los campos
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

        // Validar datos
        if (!validateInputs(name, email, password, confirmPassword, phone)) {
            return;
        }

        // Mostrar mensaje de carga
        btnRegister.setEnabled(false);
        btnRegister.setText("Registrando...");

        // 🔹 Crear usuario en Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Actualizar perfil con el nombre
                            updateUserProfile(user, name, email, phone);
                        }
                    } else {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("📝 Registrarse");

                        String errorMessage = "Error al registrar usuario";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        showError(errorMessage);
                    }
                });
    }

    private void updateUserProfile(FirebaseUser user, String name, String email, String phone) {
        // Actualizar nombre en Firebase Authentication
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Guardar información adicional en Firestore
                        saveUserToFirestore(user.getUid(), name, email, phone);
                    } else {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("📝 Registrarse");
                        showError("Error al actualizar perfil");
                    }
                });
    }

    private void saveUserToFirestore(String uid, String name, String email, String phone) {

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("nombre", name);
        userData.put("correo", email);
        userData.put("telefono", phone);

        // Tipo de usuario predeterminado
        userData.put("tipoUsuario", "Usuario");

        // Contadores iniciales
        userData.put("librosPrestados", 0);
        userData.put("sugerenciasRealizadas", 0);

        // Notificaciones activas
        userData.put("notificacionesActivas", true);

        // Guardar fecha en español: dd/MM/yyyy
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        userData.put("fechaRegistro", currentDate);

        // Timestamp para ordenamiento
        userData.put("timestampRegistro", System.currentTimeMillis());

        // Colección renombrada en español (si deseas conservar "usuarios")
        db.collection("usuarios").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this,
                            "¡Registro exitoso! Bienvenido " + name,
                            Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.putExtra("USER_EMAIL", email);
                    intent.putExtra("IS_ADMIN", false);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("📝 Registrarse");
                    showError("Error al guardar datos: " + e.getMessage());
                });
    }


    private boolean validateInputs(String name, String email, String password,
                                   String confirmPassword, String phone) {
        // Validar nombre
        if (TextUtils.isEmpty(name)) {
            etName.setError("Ingresa tu nombre completo");
            etName.requestFocus();
            return false;
        }

        if (name.length() < 3) {
            etName.setError("El nombre debe tener al menos 3 caracteres");
            etName.requestFocus();
            return false;
        }

        // Validar email
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Ingresa tu correo electrónico");
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Ingresa un correo válido");
            etEmail.requestFocus();
            return false;
        }

        // Validar contraseña
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Ingresa una contraseña");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            etPassword.requestFocus();
            return false;
        }

        // Validar confirmación de contraseña
        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Confirma tu contraseña");
            etConfirmPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Las contraseñas no coinciden");
            etConfirmPassword.requestFocus();
            return false;
        }

        // Validar teléfono
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Ingresa tu número de teléfono");
            etPhone.requestFocus();
            return false;
        }

        if (phone.length() < 8) {
            etPhone.setError("Ingresa un teléfono válido (mínimo 8 dígitos)");
            etPhone.requestFocus();
            return false;
        }

        // Validar términos y condiciones
        if (!checkboxTerms.isChecked()) {
            Toast.makeText(this,
                    "Debes aceptar los términos y condiciones",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void goToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}