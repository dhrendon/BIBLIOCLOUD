package com.example.bibliocloud;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bibliocloud.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoToRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupLoginButton();
        setupRegisterButton();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);
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

        btnLogin.setEnabled(false);
        btnLogin.setText("Iniciando sesión...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            loadUserDataAndRedirect(user.getUid(), email);
                        }
                    } else {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Iniciar Sesión");
                        showError("Credenciales incorrectas");
                    }
                });
    }

    private void loadUserDataAndRedirect(String userId, String email) {
        db.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Log.d("LOGIN_DEBUG", "Documento existe");

                        // 🔥 Leer campos en español directamente
                        String rol = document.getString("rol");
                        String nombre = document.getString("nombre");
                        String correo = document.getString("correo");
                        String telefono = document.getString("telefono");
                        String direccion = document.getString("direccion");
                        String idSucursal = document.getString("id_sucursal");
                        String nombreSucursal = document.getString("nombre_sucursal");

                        Log.d("LOGIN_DEBUG", "rol: " + rol);
                        Log.d("LOGIN_DEBUG", "nombre: " + nombre);

                        // Crear objeto User manualmente
                        User user = new User(userId, nombre, correo, rol);
                        user.setPhone(telefono);
                        user.setDepartment(direccion);
                        user.setBranchId(idSucursal);
                        user.setBranchName(nombreSucursal);

                        Log.d("LOGIN_DEBUG", "isAdmin: " + user.isAdmin());
                        Log.d("LOGIN_DEBUG", "isCashier: " + user.isCashier());

                        saveUserInfo(user);
                        redirectByUserType(user);
                    } else {
                        Log.d("LOGIN_DEBUG", "Documento no existe, creando usuario por defecto");
                        createDefaultUserAndRedirect(userId, email);
                    }
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Iniciar Sesión");
                    Log.e("LOGIN_DEBUG", "Error: " + e.getMessage());
                    showError("Error al cargar datos del usuario");
                });
    }

    private void createDefaultUserAndRedirect(String userId, String email) {
        User user = new User(userId, "Usuario", email, User.ROLE_USER);

        db.collection("usuarios").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    saveUserInfo(user);
                    redirectByUserType(user);
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Iniciar Sesión");
                    showError("Error al crear perfil de usuario");
                });
    }

    private void saveUserInfo(User user) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("current_user_id", user.getId());
        editor.putString("current_user_name", user.getName());
        editor.putString("current_user_email", user.getEmail());
        editor.putString("user_type", user.getUserType());
        editor.putBoolean("is_admin", user.isAdmin());
        editor.putBoolean("is_cashier", user.isCashier());

        if (user.isCashier()) {
            editor.putString("branch_id", user.getBranchId() != null ? user.getBranchId() : "");
            editor.putString("branch_name", user.getBranchName() != null ? user.getBranchName() : "");
        }

        editor.apply();
    }

    private void redirectByUserType(User user) {
        Intent intent;
        String welcomeMessage;

        if (user.isAdmin()) {
            intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
            welcomeMessage = "Bienvenido Administrador";
            Log.d("LOGIN_DEBUG", "Redirigiendo a AdminDashboardActivity");
        } else if (user.isCashier()) {
            intent = new Intent(LoginActivity.this, CashierDashboardActivity.class);
            welcomeMessage = "Bienvenido Cajero " + user.getName();
            Log.d("LOGIN_DEBUG", "Redirigiendo a CashierDashboardActivity");
        } else {
            intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("USER_EMAIL", user.getEmail());
            welcomeMessage = "Bienvenido " + user.getName();
            Log.d("LOGIN_DEBUG", "Redirigiendo a MainActivity");
        }

        intent.putExtra("IS_ADMIN", user.isAdmin());
        Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show();

        startActivity(intent);
        finish();
    }

    private boolean validateInputs(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showError("Por favor, completa todos los campos");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            loadUserDataAndRedirect(currentUser.getUid(), currentUser.getEmail());
        }
    }
}