package com.example.bibliocloud;

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
import com.example.bibliocloud.repositories.UserRepository; // 👈 IMPORTANTE
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvUserType, tvBooksBorrowed, tvSuggestionsMade;
    private EditText etName, etPhone, etDepartment;
    private Switch switchNotifications;
    private Button btnSaveProfile, btnChangePassword;
    private LinearLayout layoutEdit, layoutView;

    private User currentUser;
    private UserRepository userRepository; // 👈 AÑADIDO
    private FirebaseAuth mAuth;
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Instanciar repositorio y Auth
        userRepository = new UserRepository();
        mAuth = FirebaseAuth.getInstance();

        initializeViews();
        setupToolbar();
        loadUserData(); // 👈 MODIFICADO
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

    // --- LÓGICA DE DATOS MODIFICADA ---

    private void loadUserData() {
        FirebaseUser fUser = mAuth.getCurrentUser();
        if (fUser == null) {
            Toast.makeText(this, "No se pudo cargar el usuario", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Usar el repositorio para cargar el usuario desde Firestore
        userRepository.getUserById(fUser.getUid(), new UserRepository.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                currentUser = user;
                refreshUI(); // Mostrar los datos cargados
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ProfileActivity.this, "Error al cargar perfil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        if (currentUser == null) {
            Toast.makeText(this, "Error: No hay datos de usuario para guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Ingresa tu nombre");
            return;
        }

        // Actualizar el objeto currentUser en memoria
        currentUser.setName(name);
        currentUser.setPhone(phone);
        currentUser.setDepartment(department);
        // (El switch de notificaciones se actualiza solo gracias al listener)

        // Guardar cambios en Firestore usando el repositorio
        userRepository.updateUser(currentUser, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess(String id) {
                Toast.makeText(ProfileActivity.this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                isEditing = false; // Salir del modo edición
                refreshUI();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ProfileActivity.this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- FIN LÓGICA DE DATOS MODIFICADA ---


    private void setupListeners() {
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnChangePassword.setOnClickListener(v -> changePassword());

        switchNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (currentUser != null) {
                    currentUser.setNotificationsEnabled(isChecked);
                }
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
        if (currentUser == null) return; // No hacer nada si el usuario aún no se ha cargado

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
        tvUserName.setText(currentUser.getName());
        tvUserEmail.setText(currentUser.getEmail());
        tvUserType.setText(currentUser.getFormattedUserType());
        tvBooksBorrowed.setText(String.valueOf(currentUser.getBooksBorrowed()));
        tvSuggestionsMade.setText(String.valueOf(currentUser.getSuggestionsMade()));
    }

    private void populateEditFields() {
        etName.setText(currentUser.getName());
        etPhone.setText(currentUser.getPhone());
        etDepartment.setText(currentUser.getDepartment());
        switchNotifications.setChecked(currentUser.isNotificationsEnabled());
    }

    private void changePassword() {
        Toast.makeText(this, "Función de cambio de contraseña - Próximamente", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar datos por si acaso, aunque la carga inicial en onCreate es usualmente suficiente
        if (currentUser == null) {
            loadUserData();
        } else {
            refreshUI();
        }
    }
}