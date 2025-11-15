package com.example.bibliocloud;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserEditActivity extends AppCompatActivity {

    private TextInputEditText inputNombre;
    private TextInputEditText inputCorreo;
    private MaterialAutoCompleteTextView dropdownRol;
    private MaterialAutoCompleteTextView dropdownSucursal;
    private LinearLayout layoutSucursal;

    private MaterialButton btnGuardar;

    private FirebaseFirestore db;
    private String userId;

    // Lista sucursales
    private ArrayList<String> listaSucursales = new ArrayList<>();
    private ArrayAdapter<String> sucursalAdapter;

    // Rol del usuario logueado (para protección)
    private String rolActualLogueado = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_edit);

        db = FirebaseFirestore.getInstance();

        // Obtener ID del usuario a editar
        userId = getIntent().getStringExtra("userId");
        rolActualLogueado = getIntent().getStringExtra("CURRENT_ROLE"); // ADMIN / CAJERO / USUARIO

        // Referencias UI
        inputNombre = findViewById(R.id.inputNombre);
        inputCorreo = findViewById(R.id.inputCorreo);
        dropdownRol = findViewById(R.id.dropdownRol);
        dropdownSucursal = findViewById(R.id.dropdownSucursal);
        layoutSucursal = findViewById(R.id.layoutSucursal);
        btnGuardar = findViewById(R.id.btnGuardarUsuario);

        // **Roles PERMITIDOS**
        String[] roles = {"usuario", "cajero", "administrador"};
        ArrayAdapter<String> rolAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, roles);
        dropdownRol.setAdapter(rolAdapter);

        cargarSucursales();
        cargarDatosUsuario();

        // Mostrar la sucursal solo si el rol es cajero
        dropdownRol.setOnItemClickListener((parent, view, position, id) -> {
            if (dropdownRol.getText().toString().equals("cajero")) {
                layoutSucursal.setVisibility(View.VISIBLE);
            } else {
                layoutSucursal.setVisibility(View.GONE);
            }
        });

        btnGuardar.setOnClickListener(v -> guardarCambios());
    }

    private void cargarSucursales() {
        db.collection("sucursales")
                .get()
                .addOnSuccessListener(query -> {
                    listaSucursales.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        String nombre = doc.getString("name");
                        if (nombre != null) listaSucursales.add(nombre);
                    }

                    sucursalAdapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_dropdown_item_1line, listaSucursales);

                    dropdownSucursal.setAdapter(sucursalAdapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al cargar sucursales", Toast.LENGTH_SHORT).show()
                );
    }

    private void cargarDatosUsuario() {
        db.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {

                        String nombre = doc.getString("nombre");
                        String correo = doc.getString("correo");
                        String rol = doc.getString("rol");
                        String sucursal = doc.getString("nombre_sucursal");

                        inputNombre.setText(nombre);
                        inputCorreo.setText(correo);
                        dropdownRol.setText(rol, false);

                        // Mostrar sucursal SOLO si es cajero
                        if ("cajero".equals(rol)) {
                            layoutSucursal.setVisibility(View.VISIBLE);
                            if (sucursal != null) dropdownSucursal.setText(sucursal, false);
                        } else {
                            layoutSucursal.setVisibility(View.GONE);
                        }

                        // PROTECCIÓN: si NO es administrador, no puede cambiar el rol
                        if (!"administrador".equals(rolActualLogueado)) {
                            dropdownRol.setEnabled(false);
                            dropdownSucursal.setEnabled(false);
                        }

                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al cargar usuario", Toast.LENGTH_SHORT).show()
                );
    }

    private void guardarCambios() {

        String nombre = inputNombre.getText().toString().trim();
        String correo = inputCorreo.getText().toString().trim();
        String rolSeleccionado = dropdownRol.getText().toString();
        String sucursalSeleccionada = dropdownSucursal.getText().toString();

        // Validaciones
        if (nombre.isEmpty()) {
            inputNombre.setError("Ingrese el nombre");
            return;
        }

        if (correo.isEmpty()) {
            inputCorreo.setError("Ingrese el correo");
            return;
        }

        if (rolSeleccionado.isEmpty()) {
            dropdownRol.setError("Seleccione un rol");
            return;
        }

        if (rolSeleccionado.equals("cajero") && sucursalSeleccionada.isEmpty()) {
            dropdownSucursal.setError("Seleccione una sucursal");
            return;
        }

        // Datos a actualizar
        Map<String, Object> data = new HashMap<>();
        data.put("nombre", nombre);
        data.put("correo", correo);
        data.put("rol", rolSeleccionado);

        if (rolSeleccionado.equals("cajero")) {
            data.put("nombre_sucursal", sucursalSeleccionada);
        } else {
            data.put("nombre_sucursal", null);
        }

        db.collection("usuarios").document(userId)
                .update(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Usuario actualizado correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al guardar cambios", Toast.LENGTH_SHORT).show()
                );
    }
}
