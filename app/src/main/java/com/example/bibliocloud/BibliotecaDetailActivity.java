package com.example.bibliocloud;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;

public class BibliotecaDetailActivity extends AppCompatActivity {

    private TextView tvNombre, tvDireccion, tvHorario, tvEstado, tvLibros;
    private MaterialButton btnVolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_biblioteca_detail);

        initializeViews();
        loadBibliotecaData();
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Detalles de Biblioteca");

        tvNombre = findViewById(R.id.tvNombre);
        tvDireccion = findViewById(R.id.tvDireccion);
        tvHorario = findViewById(R.id.tvHorario);
        tvEstado = findViewById(R.id.tvEstado);
        tvLibros = findViewById(R.id.tvLibros);
        btnVolver = findViewById(R.id.btnVolver);

        btnVolver.setOnClickListener(v -> finish());
    }

    private void loadBibliotecaData() {
        // Obtener datos del Intent
        String nombre = getIntent().getStringExtra("nombre_biblioteca");
        String direccion = getIntent().getStringExtra("direccion");
        String horario = getIntent().getStringExtra("horario");
        boolean disponible = getIntent().getBooleanExtra("disponible", false);
        String[] librosArray = getIntent().getStringArrayExtra("libros_disponibles");

        // Mostrar datos
        tvNombre.setText(nombre);
        tvDireccion.setText(direccion);
        tvHorario.setText(horario);

        if (disponible) {
            tvEstado.setText(" ABIERTA - Disponible para visitas");
            tvEstado.setTextColor(getResources().getColor(R.color.green));
        } else {
            tvEstado.setText(" CERRADA - No disponible temporalmente");
            tvEstado.setTextColor(getResources().getColor(R.color.red));
        }

        // Mostrar libros disponibles
        StringBuilder librosText = new StringBuilder();
        if (librosArray != null && librosArray.length > 0) {
            for (String libro : librosArray) {
                librosText.append("• ").append(libro).append("\n");
            }
        } else {
            librosText.append("No hay información de libros disponibles\n");
            librosText.append("Visita la biblioteca para consultar el catálogo completo");
        }
        tvLibros.setText(librosText.toString());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}