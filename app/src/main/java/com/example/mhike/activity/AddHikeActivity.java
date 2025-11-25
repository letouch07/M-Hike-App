package com.example.mhike.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View; // Needed for LAYER_TYPE_SOFTWARE
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.mhike.R;
import com.example.mhike.database.HikeDAO;
import com.example.mhike.model.Hike;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddHikeActivity extends AppCompatActivity {

    private EditText nameInput, locationInput, dateInput, lengthInput, descriptionInput, custom1Input, custom2Input;
    private RadioGroup parkingGroup;
    private AutoCompleteTextView difficultySpinner;
    private Button saveButton, btnGetLocation;
    private DatePickerDialog datePickerDialog;
    private MapView mapView;
    private HikeDAO hikeDAO;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // FIX 1: Add User Agent to prevent server blocking
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue("com.example.mhike");

        setContentView(R.layout.activity_add_hike);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        hikeDAO = new HikeDAO(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initializeViews();

        dateInput.setOnClickListener(v -> showDatePickerDialog());
        btnGetLocation.setOnClickListener(v -> requestLocation());

        locationInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                String address = locationInput.getText().toString();
                if (!address.isEmpty()) searchLocationFromAddress(address);
                return true;
            }
            return false;
        });

        saveButton.setOnClickListener(v -> {
            if (isInputValid()) {
                int selectedId = parkingGroup.getCheckedRadioButtonId();
                String parkingStatus = (selectedId != -1) ?
                        ((RadioButton) findViewById(selectedId)).getText().toString() : "No";
                showConfirmationDialog(parkingStatus);
            }
        });
    }

    private void initializeViews() {
        nameInput = findViewById(R.id.editTextHikeName);
        locationInput = findViewById(R.id.editTextLocation);
        dateInput = findViewById(R.id.editTextDate);
        lengthInput = findViewById(R.id.editTextLength);
        descriptionInput = findViewById(R.id.editTextDescription);
        custom1Input = findViewById(R.id.editTextCustom1);
        custom2Input = findViewById(R.id.editTextCustom2);
        parkingGroup = findViewById(R.id.radioGroupParking);
        difficultySpinner = findViewById(R.id.spinnerDifficulty);
        saveButton = findViewById(R.id.buttonSaveHike);
        btnGetLocation = findViewById(R.id.btnGetLocation);

        // --- MAP SETUP ---
        mapView = findViewById(R.id.mapView);

        // FIX 2: THIS LINE FIXES THE WHITE SCREEN ON EMULATORS
        mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.difficulty_levels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(adapter);
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                updateMapMarker(location.getLatitude(), location.getLongitude(), "Current Location");
                try {
                    Geocoder geocoder = new Geocoder(AddHikeActivity.this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        locationInput.setText(addresses.get(0).getAddressLine(0));
                    } else {
                        locationInput.setText(location.getLatitude() + ", " + location.getLongitude());
                    }
                } catch (IOException e) {
                    locationInput.setText(location.getLatitude() + ", " + location.getLongitude());
                }
                Toast.makeText(AddHikeActivity.this, "Location Found!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AddHikeActivity.this, "GPS unavailable. Open Maps in Emulator to set location.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void searchLocationFromAddress(String addressString) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> list = geocoder.getFromLocationName(addressString, 1);
            if (list != null && !list.isEmpty()) {
                Address address = list.get(0);
                updateMapMarker(address.getLatitude(), address.getLongitude(), addressString);
            } else {
                Toast.makeText(this, "Address not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateMapMarker(double lat, double lon, String title) {
        GeoPoint point = new GeoPoint(lat, lon);
        mapView.getController().animateTo(point);
        mapView.getController().setZoom(16.0);
        mapView.getOverlays().clear();
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(title);
        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    // FIX 3: LIFECYCLE METHODS - CRITICAL FOR MAP DISPLAY
    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocation();
            }
        }
    }

    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String selectedDate = String.format("%02d/%02d/%d", dayOfMonth, monthOfYear + 1, year1);
                    dateInput.setText(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    private boolean isInputValid() {
        boolean valid = true;
        if (nameInput.getText().toString().trim().isEmpty()) {
            nameInput.setError("Required"); valid = false;
        }
        if (locationInput.getText().toString().trim().isEmpty()) {
            locationInput.setError("Required"); valid = false;
        }
        if (dateInput.getText().toString().trim().isEmpty()) {
            dateInput.setError("Required"); valid = false;
        }
        if (lengthInput.getText().toString().trim().isEmpty()) {
            lengthInput.setError("Required"); valid = false;
        }
        if (parkingGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Parking required", Toast.LENGTH_SHORT).show(); valid = false;
        }
        return valid;
    }

    private void showConfirmationDialog(String parkingStatus) {
        String msg = "Name: " + nameInput.getText() + "\nLocation: " + locationInput.getText() +
                "\nDate: " + dateInput.getText() + "\nLength: " + lengthInput.getText();
        new AlertDialog.Builder(this)
                .setTitle("Confirm Hike")
                .setMessage(msg)
                .setPositiveButton("Confirm", (dialog, which) -> saveHikeDetails(parkingStatus))
                .setNegativeButton("Edit", null)
                .show();
    }

    private void saveHikeDetails(String parkingStatus) {
        Hike hike = new Hike();
        hike.setName(nameInput.getText().toString().trim());
        hike.setLocation(locationInput.getText().toString().trim());
        hike.setDate(dateInput.getText().toString().trim());
        try {
            hike.setLength(Double.parseDouble(lengthInput.getText().toString().trim()));
        } catch (NumberFormatException e) { return; }

        hike.setDifficulty(difficultySpinner.getText().toString());
        hike.setParkingAvailable(parkingStatus);
        hike.setDescription(descriptionInput.getText().toString().trim());
        hike.setCustomField1(custom1Input.getText().toString().trim());
        hike.setCustomField2(custom2Input.getText().toString().trim());

        if (hikeDAO.addHike(hike) > 0) {
            Toast.makeText(this, "Hike Saved!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Save Failed", Toast.LENGTH_SHORT).show();
        }
    }
}