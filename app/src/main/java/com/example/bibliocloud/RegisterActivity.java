package com.example.bibliocloud;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bibliocloud.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword, etPhone;
    private MaterialButton btnRegister, btnGoToLogin;
    private android.widget.CheckBox checkboxTerms;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Toast.makeText(this,
                    "Debes cerrar sesión antes de registrar una nueva cuenta",
                    Toast.LENGTH_LONG).show();

            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        }
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
        btnRegister.setOnClickListener(v -> attemptRegister());
        btnGoToLogin.setOnClickListener(v -> goToLogin());
    }

    private void attemptRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (!validateInputs(name, email, password, confirmPassword, phone)) {
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Registrando...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            updateUserProfile(user, name, email, phone);
                        }
                    } else {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Registrarse");
                        showError("Error al registrar usuario");
                    }
                });
    }

    private void updateUserProfile(FirebaseUser user, String name, String email, String phone) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveUserToFirestore(user.getUid(), name, email, phone);
                    } else {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Registrarse");
                        showError("Error al actualizar perfil");
                    }
                });
    }

    private void saveUserToFirestore(String uid, String name, String email, String phone) {

        // FORZAR SIEMPRE rol = "usuario"
        User user = new User(uid, name, email, "usuario");
        user.setPhone(phone);
        user.setNotificationsEnabled(true);

        db.collection("usuarios").document(uid)
                .set(user)
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
                    btnRegister.setText("Registrarse");
                    showError("Error al guardar datos: " + e.getMessage());
                });
    }

    private boolean validateInputs(String name, String email, String password,
                                   String confirmPassword, String phone) {

        if (TextUtils.isEmpty(name)) {
            etName.setError("Ingresa tu nombre completo");
            etName.requestFocus();
            return false;
        }

        if (name.length() < 3) {
            etName.setError("Debe tener al menos 3 caracteres");
            etName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Correo inválido");
            etEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Mínimo 6 caracteres");
            etPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Las contraseñas no coinciden");
            etConfirmPassword.requestFocus();
            return false;
        }

        if (phone.length() < 8) {
            etPhone.setError("Teléfono inválido");
            etPhone.requestFocus();
            return false;
        }

        if (!checkboxTerms.isChecked()) {
            Toast.makeText(this,
                    "Debes aceptar los términos",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void goToLogin() {
        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        finish();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
