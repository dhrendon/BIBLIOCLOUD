package com.example.bibliocloud;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.bibliocloud.models.User;
import com.google.gson.Gson;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvUserType, tvBooksBorrowed, tvSuggestionsMade;
    private EditText etName, etPhone, etDepartment;
    private Switch switchNotifications;
    private Button btnSaveProfile, btnChangePassword;
    private LinearLayout layoutEdit, layoutView;

    private User currentUser;
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initializeViews();
        setupToolbar();
        loadUserData();
        setupListeners();
        refreshUI();
    }

    private void initializeViews() {
        // TextViews informativos
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserType = findViewById(R.id.tvUserType);
        tvBooksBorrowed = findViewById(R.id.tvBooksBorrowed);
        tvSuggestionsMade = findViewById(R.id.tvSuggestionsMade);

        // Campos editables
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etDepartment = findViewById(R.id.etDepartment);

        // Switch y botones
        switchNotifications = findViewById(R.id.switchNotifications);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        // Layouts
        layoutEdit = findViewById(R.id.layoutEdit);
        layoutView = findViewById(R.id.layoutView);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mi Perfil");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        String userJson = prefs.getString("current_user", "");

        if (!userJson.isEmpty()) {
            Gson gson = new Gson();
            currentUser = gson.fromJson(userJson, User.class);
        } else {
            // Crear usuario por defecto
            createDefaultUser();
        }

        // Cargar estadísticas
        loadUserStatistics();
    }

    private void createDefaultUser() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String userEmail = prefs.getString("current_user_email", "usuario@bibliocloud.com");
        boolean isAdmin = prefs.getBoolean("is_admin", false);

        currentUser = new User(
                "user_1",
                "Usuario BiblioCloud",
                userEmail,
                isAdmin ? "admin" : "user"
        );
        saveUserData();
    }

    private void loadUserStatistics() {
        SharedPreferences prefs = getSharedPreferences("UserStats", MODE_PRIVATE);
        currentUser.setBooksBorrowed(prefs.getInt("books_borrowed", 0));
        currentUser.setSuggestionsMade(prefs.getInt("suggestions_made", 0));
    }

    private void setupListeners() {
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnChangePassword.setOnClickListener(v -> changePassword());

        switchNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                currentUser.setNotificationsEnabled(isChecked);
            }
        });

        // Hacer clic en la información para editar
        setupEditClickListeners();
    }

    private void setupEditClickListeners() {
        tvUserName.setOnClickListener(v -> toggleEditMode());
        tvUserEmail.setOnClickListener(v -> toggleEditMode());
    }

    private void toggleEditMode() {
        isEditing = !isEditing;
        refreshUI();
    }

    private void refreshUI() {
        if (isEditing) {
            // Modo edición
            layoutView.setVisibility(View.GONE);
            layoutEdit.setVisibility(View.VISIBLE);
            populateEditFields();
        } else {
            // Modo visualización
            layoutEdit.setVisibility(View.GONE);
            layoutView.setVisibility(View.VISIBLE);
            populateViewFields();
        }
    }

    private void populateViewFields() {
        if (currentUser != null) {
            tvUserName.setText(currentUser.getName());
            tvUserEmail.setText(currentUser.getEmail());
            tvUserType.setText(currentUser.getFormattedUserType());
            tvBooksBorrowed.setText(String.valueOf(currentUser.getBooksBorrowed()));
            tvSuggestionsMade.setText(String.valueOf(currentUser.getSuggestionsMade()));
        }
    }

    private void populateEditFields() {
        if (currentUser != null) {
            etName.setText(currentUser.getName());
            etPhone.setText(currentUser.getPhone());
            etDepartment.setText(currentUser.getDepartment());
            switchNotifications.setChecked(currentUser.isNotificationsEnabled());
        }
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Ingresa tu nombre");
            return;
        }

        // Actualizar usuario
        currentUser.setName(name);
        currentUser.setPhone(phone);
        currentUser.setDepartment(department);

        // Guardar cambios
        saveUserData();

        // Salir del modo edición
        isEditing = false;
        refreshUI();

        Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
    }

    private void changePassword() {
        // Por ahora solo un mensaje - luego implementaremos cambio real de contraseña
        Toast.makeText(this, "Función de cambio de contraseña - Próximamente", Toast.LENGTH_SHORT).show();
    }

    private void saveUserData() {
        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();
        String userJson = gson.toJson(currentUser);
        editor.putString("current_user", userJson);
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserStatistics();
        refreshUI();
    }
}