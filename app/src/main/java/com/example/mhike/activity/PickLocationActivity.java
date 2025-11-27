package com.example.mhike.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.mhike.R;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class PickLocationActivity extends AppCompatActivity {

    private MapView mapView;
    private TextView tvCurrentAddress;
    private Button btnConfirm;
    private FloatingActionButton btnMyLocation;

    private String selectedAddress = "";
    private double selectedLat = 0.0;
    private double selectedLon = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Cấu hình OSM (Bắt buộc)
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_pick_location);

        // 2. Ánh xạ View
        mapView = findViewById(R.id.mapView);
        tvCurrentAddress = findViewById(R.id.tvCurrentAddress);
        btnConfirm = findViewById(R.id.btnConfirmLocation);
        btnMyLocation = findViewById(R.id.btnMyLocation);

        // 3. Cài đặt Map
        setupMap();

        // 4. Xử lý nút Xác nhận
        btnConfirm.setOnClickListener(v -> {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("SELECTED_ADDRESS", selectedAddress);
            returnIntent.putExtra("SELECTED_LAT", selectedLat);
            returnIntent.putExtra("SELECTED_LON", selectedLon);
            setResult(RESULT_OK, returnIntent);
            finish();
        });

        // 5. Xử lý nút Vị trí của tôi
        btnMyLocation.setOnClickListener(v -> moveToCurrentLocation());
    }

    private void setupMap() {
        // Fix lỗi trắng màn hình trên Emulator
        mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(18.0);

        // Mặc định focus vào một điểm (ví dụ HCM) để map load
        GeoPoint startPoint = new GeoPoint(10.7769, 106.7009);
        mapView.getController().setCenter(startPoint);

        // Lắng nghe sự kiện kéo map
        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                updateAddressFromCenter();
                return true;
            }
            @Override
            public boolean onZoom(ZoomEvent event) {
                updateAddressFromCenter();
                return true;
            }
        });
    }

    // --- HÀM SỬA LỖI ANR (CHẠY TRÊN THREAD RIÊNG) ---
    private void updateAddressFromCenter() {
        GeoPoint center = (GeoPoint) mapView.getMapCenter();
        selectedLat = center.getLatitude();
        selectedLon = center.getLongitude();

        // Cập nhật UI tạm thời
        tvCurrentAddress.setText("Loading address...");

        // Chạy Geocoder ở luồng phụ (Background Thread)
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(PickLocationActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(selectedLat, selectedLon, 1);

                // Quay lại luồng chính để cập nhật giao diện (UI Thread)
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        selectedAddress = addresses.get(0).getAddressLine(0);
                        tvCurrentAddress.setText(selectedAddress);
                    } else {
                        selectedAddress = String.format("%.5f, %.5f", selectedLat, selectedLon);
                        tvCurrentAddress.setText(selectedAddress);
                    }
                });

            } catch (IOException e) {
                // Xử lý lỗi mạng
                runOnUiThread(() -> {
                    selectedAddress = String.format("%.5f, %.5f", selectedLat, selectedLon);
                    tvCurrentAddress.setText("Network Error: " + selectedAddress);
                });
            }
        }).start();
    }

    private void moveToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        LocationServices.getFusedLocationProviderClient(this).getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                mapView.getController().animateTo(new GeoPoint(location.getLatitude(), location.getLongitude()));
                mapView.getController().setZoom(18.0);
            } else {
                Toast.makeText(this, "Please set location in Emulator settings first!", Toast.LENGTH_LONG).show();
            }
        });
    }
}