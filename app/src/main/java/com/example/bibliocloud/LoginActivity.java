package com.example.bibliocloud;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoToRegister;

    private FirebaseAuth mAuth;

    private final String ADMIN_EMAIL = "admin@bibliocloud.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        initializeViews();
        setupLoginButton();
        setupRegisterButton();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister); // <-- CORRECTO
    }

    private void setupLoginButton() {
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void setupRegisterButton() {
        btnGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (!validateInputs(email, password)) return;

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            redirectToAppropriateActivity(user.getEmail());
                        }
                    } else {
                        showError("Credenciales incorrectas o usuario no registrado");
                    }
                });
    }

    private boolean validateInputs(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showError("Por favor, completa todos los campos");
            return false;
        }
        return true;
    }

    private void redirectToAppropriateActivity(String email) {
        Intent intent;

        if (email.equalsIgnoreCase(ADMIN_EMAIL)) {
            intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
            Toast.makeText(this, "Bienvenido Administrador", Toast.LENGTH_SHORT).show();
        } else {
            intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("USER_EMAIL", email);
            Toast.makeText(this, "Bienvenido " + email, Toast.LENGTH_SHORT).show();
        }

        intent.putExtra("IS_ADMIN", email.equalsIgnoreCase(ADMIN_EMAIL));
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            redirectToAppropriateActivity(currentUser.getEmail());
        }
    }
}

