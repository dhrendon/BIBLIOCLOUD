package com.example.bibliocloud;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "MapActivity";

    // Mapa para almacenar información de bibliotecas
    private Map<String, BibliotecaInfo> bibliotecasInfo = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Cargar datos de bibliotecas desde JSON
        cargarDatosBibliotecas();

        // Inicializar Places API
        initializePlaces();
        initializeViews();
        checkLocationPermission();
    }

    private void initializePlaces() {
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyBTzm4kVIIjfgJoxYrOZmKze8vhLtpZhR4");
        }
        placesClient = Places.createClient(this);
    }

    private void initializeViews() {
        MaterialButton btnVolver = findViewById(R.id.btnVolverMapa);
        btnVolver.setOnClickListener(v -> finish());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void cargarDatosBibliotecas() {
        try {
            // Leer archivo JSON de assets
            InputStream is = getAssets().open("bibliotecas.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            JSONObject jsonObject = new JSONObject(json);
            JSONArray bibliotecasArray = jsonObject.getJSONArray("bibliotecas");

            for (int i = 0; i < bibliotecasArray.length(); i++) {
                JSONObject biblioteca = bibliotecasArray.getJSONObject(i);
                String nombre = biblioteca.getString("nombre");
                double latitud = biblioteca.getDouble("latitud");
                double longitud = biblioteca.getDouble("longitud");
                String direccion = biblioteca.getString("direccion");
                String horario = biblioteca.getString("horario");
                boolean disponible = biblioteca.getBoolean("disponible");

                // Libros disponibles
                JSONArray librosArray = biblioteca.getJSONArray("libros_disponibles");
                String[] libros = new String[librosArray.length()];
                for (int j = 0; j < librosArray.length(); j++) {
                    libros[j] = librosArray.getString(j);
                }

                bibliotecasInfo.put(nombre, new BibliotecaInfo(
                        nombre, latitud, longitud, direccion, horario, disponible, libros
                ));
            }

            Log.d(TAG, "Bibliotecas cargadas: " + bibliotecasInfo.size());

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error cargando datos de bibliotecas: " + e.getMessage());
            // Datos de ejemplo si hay error
            cargarDatosEjemplo();
        }
    }

    private void cargarDatosEjemplo() {
        // Datos de ejemplo para bibliotecas
        String[] librosEjemplo = {"Cien años de soledad", "1984", "El principito", "Crimen y castigo"};

        bibliotecasInfo.put("Biblioteca Central CDMX", new BibliotecaInfo(
                "Biblioteca Central CDMX", 19.432608, -99.133209,
                "Plaza de la Constitución S/N, Centro", "Lun-Vie: 9:00-20:00, Sáb: 10:00-14:00",
                true, librosEjemplo
        ));

        bibliotecasInfo.put("Biblioteca Vasconcelos", new BibliotecaInfo(
                "Biblioteca Vasconcelos", 19.4438, -99.1494,
                "Eje 1 Norte Mosqueta S/N, Buenavista", "Lun-Dom: 8:30-19:30",
                true, new String[]{"Don Quijote", "Rayuela", "La tregua"}
        ));

        Log.d(TAG, "Datos de ejemplo cargados: " + bibliotecasInfo.size());
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                setupMapWithDefaultLocation();
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Toast.makeText(this, "Obteniendo ubicación...", Toast.LENGTH_SHORT).show();

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            if (mMap != null) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12));
                                addUserMarker(currentLocation);
                                addBibliotecasMarkers();
                            }
                        } else {
                            Toast.makeText(MapActivity.this, "No se pudo obtener ubicación", Toast.LENGTH_SHORT).show();
                            setupMapWithDefaultLocation();
                        }
                    }
                });
    }

    private void setupMapWithDefaultLocation() {
        LatLng defaultLocation = new LatLng(19.432608, -99.133209);
        if (mMap != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
            addBibliotecasMarkers();
        }
    }

    private void addUserMarker(LatLng location) {
        if (mMap != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("📍 Tu ubicación actual")
                    .snippet("Estás aquí")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        }
    }

    private void addBibliotecasMarkers() {
        if (mMap == null || bibliotecasInfo.isEmpty()) {
            Log.e(TAG, "No hay bibliotecas para mostrar o mapa no está listo");
            Toast.makeText(this, "No se encontraron bibliotecas", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Agregando " + bibliotecasInfo.size() + " bibliotecas al mapa");

        for (BibliotecaInfo biblioteca : bibliotecasInfo.values()) {
            LatLng location = new LatLng(biblioteca.getLatitud(), biblioteca.getLongitud());

            // Elegir color según disponibilidad
            float colorHue = biblioteca.isDisponible() ?
                    BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_RED;

            String estado = biblioteca.isDisponible() ? "🟢 ABIERTA" : "🔴 CERRADA";

            MarkerOptions markerOptions = new MarkerOptions()
                    .position(location)
                    .title(biblioteca.getNombre())
                    .snippet(estado + " - " + biblioteca.getDireccion())
                    .icon(BitmapDescriptorFactory.defaultMarker(colorHue));

            mMap.addMarker(markerOptions);
        }

        Toast.makeText(this, bibliotecasInfo.size() + " bibliotecas cargadas", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // No procesar clic en el marcador del usuario
        if (marker.getTitle().equals("📍 Tu ubicación actual")) {
            return false;
        }

        String nombreBiblioteca = marker.getTitle();
        BibliotecaInfo biblioteca = bibliotecasInfo.get(nombreBiblioteca);

        if (biblioteca != null) {
            mostrarDialogoBiblioteca(biblioteca);
        } else {
            Toast.makeText(this, "Información no disponible", Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    private void mostrarDialogoBiblioteca(BibliotecaInfo biblioteca) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(biblioteca.getNombre());

        // Construir mensaje detallado
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("📍 ").append(biblioteca.getDireccion()).append("\n\n");
        mensaje.append("🕒 ").append(biblioteca.getHorario()).append("\n\n");

        if (biblioteca.isDisponible()) {
            mensaje.append("BIBLIOTECA ABIERTA\n\n");
            mensaje.append("Libros disponibles:\n");

            if (biblioteca.getLibrosDisponibles().length > 0) {
                for (String libro : biblioteca.getLibrosDisponibles()) {
                    mensaje.append("   • ").append(libro).append("\n");
                }
            } else {
                mensaje.append("   No hay información de libros disponibles\n");
            }

        } else {
            mensaje.append("BIBLIOTECA CERRADA TEMPORALMENTE\n\n");
            mensaje.append("ℹ️ No disponible para visitas o préstamos\n");
        }

        builder.setMessage(mensaje.toString());
        builder.setNegativeButton("Cerrar", null);

        // Solo agregar botones adicionales si está disponible
        if (biblioteca.isDisponible()) {
            builder.setPositiveButton("Ver Detalles", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    abrirDetallesBiblioteca(biblioteca);
                }
            });
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void abrirDetallesBiblioteca(BibliotecaInfo biblioteca) {
        try {
            Intent intent = new Intent(this, BibliotecaDetailActivity.class);
            intent.putExtra("nombre_biblioteca", biblioteca.getNombre());
            intent.putExtra("direccion", biblioteca.getDireccion());
            intent.putExtra("horario", biblioteca.getHorario());
            intent.putExtra("disponible", biblioteca.isDisponible());
            intent.putExtra("libros_disponibles", biblioteca.getLibrosDisponibles());
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error al abrir detalles", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error abriendo detalles: " + e.getMessage());
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Configurar el mapa
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Configurar listener para clics en marcadores
        mMap.setOnMarkerClickListener(this);

        // Habilitar ubicación
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                mMap.setMyLocationEnabled(true);
                getCurrentLocation();
            } catch (SecurityException e) {
                Log.e(TAG, "Error de permisos: " + e.getMessage());
                setupMapWithDefaultLocation();
            }
        } else {
            setupMapWithDefaultLocation();
        }
    }

    // Clase para almacenar información de bibliotecas
    private static class BibliotecaInfo {
        private String nombre;
        private double latitud;
        private double longitud;
        private String direccion;
        private String horario;
        private boolean disponible;
        private String[] librosDisponibles;

        public BibliotecaInfo(String nombre, double latitud, double longitud,
                              String direccion, String horario, boolean disponible,
                              String[] librosDisponibles) {
            this.nombre = nombre;
            this.latitud = latitud;
            this.longitud = longitud;
            this.direccion = direccion;
            this.horario = horario;
            this.disponible = disponible;
            this.librosDisponibles = librosDisponibles;
        }

        // Getters
        public String getNombre() { return nombre; }
        public double getLatitud() { return latitud; }
        public double getLongitud() { return longitud; }
        public String getDireccion() { return direccion; }
        public String getHorario() { return horario; }
        public boolean isDisponible() { return disponible; }
        public String[] getLibrosDisponibles() { return librosDisponibles; }
    }
}