package com.example.bibliocloud;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.gridlayout.widget.GridLayout;
import com.example.bibliocloud.repositories.BookRepository;
import com.example.bibliocloud.services.AuthService;
import com.example.bibliocloud.utils.FirebaseUtils;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView tvWelcome;
    private CardView cardAdminPanel;
    private boolean isAdmin = false;
    private String userEmail = "";
    private AuthService authService;
    private BookRepository bookRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvWelcome = findViewById(R.id.tvWelcome);
        cardAdminPanel = findViewById(R.id.cardAdminPanel);

        // Configurar toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FirebaseUtils.enableOfflinePersistence();
        authService = new AuthService();
        bookRepository = new BookRepository();

        if (!authService.isUserLoggedIn()) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        getFCMToken();

        Intent intent = getIntent();
        userEmail = intent.getStringExtra("USER_EMAIL");
        isAdmin = intent.getBooleanExtra("IS_ADMIN", false);

        if (userEmail == null && authService.getCurrentFirebaseUser() != null) {
            userEmail = authService.getCurrentFirebaseUser().getEmail();
        }

        String welcomeMessage = isAdmin
                ? "Bienvenido Administrador"
                : "Bienvenido " + (userEmail != null ? userEmail : "");
        tvWelcome.setText(welcomeMessage);

        // Mostrar/ocultar tarjeta de admin
        if (cardAdminPanel != null) {
            cardAdminPanel.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        }

        saveUserInfo();
        setupCardClicks();
    }

    private void getFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Error al obtener token FCM", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    Log.d(TAG, "Token FCM obtenido: " + token);
                });
    }

    private void saveUserInfo() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("current_user_email", userEmail);
        editor.putBoolean("is_admin", isAdmin);
        editor.apply();
    }

    private void setupCardClicks() {
        GridLayout gridLayout = findViewById(R.id.gridLayout);

        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View child = gridLayout.getChildAt(i);
            if (child instanceof CardView) {
                final int position = i;
                child.setOnClickListener(v -> handleMenuOptionClick(position));
            }
        }
    }

    private void handleMenuOptionClick(int position) {
        switch (position) {
            case 0: // Buscar Libros
                startActivity(new Intent(this, SearchActivity.class));
                break;
            case 1: // Mis Favoritos
                startActivity(new Intent(this, FavoritesActivity.class));
                break;
            case 2: // Sugerencias
                startActivity(new Intent(this, SuggestionsActivity.class));
                break;
            case 3: // Catálogo por Sucursal
                startActivity(new Intent(this, BranchBooksCatalogActivity.class));
                break;
            case 4: // Mapa de Bibliotecas
                startActivity(new Intent(this, MapActivity.class));
                break;
            case 5: // Mis Compras
                startActivity(new Intent(this, MyPurchasesActivity.class));
                break;
            case 6: // Panel Admin (solo admin)
                if (isAdmin) {
                    startActivity(new Intent(this, AdminDashboardActivity.class));
                } else {
                    Toast.makeText(this, "Acceso solo para administradores",
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case 7: // Mis Préstamos
                startActivity(new Intent(this, MyLoansActivity.class));
                break;
            case 8: // Acerca de
                Toast.makeText(this, "BiblioCloud v2.0 - Sistema de Gestión Bibliotecaria",
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // Mostrar opción de gestión solo para administradores
        MenuItem adminItem = menu.findItem(R.id.action_admin);
        if (adminItem != null) {
            adminItem.setVisible(isAdmin);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        } else if (id == R.id.action_settings) {
            Toast.makeText(this, "Configuración - Próximamente", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_admin && isAdmin) {
            startActivity(new Intent(this, AdminDashboardActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("current_user_email");
        editor.remove("is_admin");
        editor.apply();

        authService.logout();

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
    }
}