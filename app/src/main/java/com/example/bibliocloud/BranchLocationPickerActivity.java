package com.example.bibliocloud;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class BranchLocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker selectedMarker;
    private LatLng selectedLocation;
    private Button btnConfirmLocation, btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branch_location_picker);

        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        btnCancel = findViewById(R.id.btnCancel);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupListeners();
    }

    private void setupListeners() {
        btnConfirmLocation.setOnClickListener(v -> {
            if (selectedLocation != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitude", selectedLocation.latitude);
                resultIntent.putExtra("longitude", selectedLocation.longitude);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Selecciona una ubicación en el mapa",
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> finish());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Ubicación inicial: Ciudad de México
        LatLng mexicoCity = new LatLng(19.432608, -99.133209);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mexicoCity, 12));

        // Configurar el mapa
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        // Listener para clic en el mapa
        mMap.setOnMapClickListener(latLng -> {
            // Remover marcador anterior
            if (selectedMarker != null) {
                selectedMarker.remove();
            }

            // Agregar nuevo marcador
            selectedMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("📍 Nueva Sucursal")
                    .snippet("Ubicación seleccionada"));

            selectedLocation = latLng;

            Toast.makeText(this,
                    String.format("Ubicación: %.6f, %.6f", latLng.latitude, latLng.longitude),
                    Toast.LENGTH_SHORT).show();
        });

        Toast.makeText(this, "Toca el mapa para seleccionar la ubicación",
                Toast.LENGTH_LONG).show();
    }
}