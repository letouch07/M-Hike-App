package com.example.mhike.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mhike.R;
import com.example.mhike.database.HikeDAO;
import com.example.mhike.model.Hike;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.Calendar;

public class AddHikeActivity extends AppCompatActivity {

    // UI Components
    private EditText nameInput, locationInput, dateInput, lengthInput, descriptionInput, custom1Input, custom2Input;
    private RadioGroup parkingGroup;
    private AutoCompleteTextView difficultySpinner;
    private Button saveButton, btnGetLocation;

    // Map Preview Components
    private MapView mapPreview;
    private View cardMapPreview;

    // Logic Components
    private HikeDAO hikeDAO;
    private ActivityResultLauncher<Intent> mapPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- MAP CONFIGURATION ---
        // FIX 1: Set User Agent to prevent OpenStreetMap servers from blocking the app.
        // This MUST be called before setContentView.
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue("com.example.mhike");

        setContentView(R.layout.activity_add_hike);

        // Setup Toolbar
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize Database Helper
        hikeDAO = new HikeDAO(this);

        // Initialize Views and Map Configuration
        initializeViews();
        setupMapPreviewConfig();
        setupMapPickerLauncher();

        // --- LISTENERS ---

        // Date Picker
        dateInput.setOnClickListener(v -> showDatePickerDialog());

        // Open Full Screen Map (PickLocationActivity) when clicking button or text field
        btnGetLocation.setOnClickListener(v -> openMapScreen());
        locationInput.setFocusable(false); // Disable keyboard for location input
        locationInput.setOnClickListener(v -> openMapScreen());

        // Save Button Logic
        saveButton.setOnClickListener(v -> {
            if (isInputValid()) {
                // Get selected Parking option
                int selectedId = parkingGroup.getCheckedRadioButtonId();
                String parkingStatus = (selectedId != -1) ?
                        ((RadioButton) findViewById(selectedId)).getText().toString() : "No";

                // Show confirmation dialog
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

        // Map Preview Views
        mapPreview = findViewById(R.id.mapView);
        cardMapPreview = findViewById(R.id.cardMapPreview);

        // Spinner Adapter
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.difficulty_levels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(adapter);
    }

    // --- MAP PREVIEW CONFIGURATION ---
    private void setupMapPreviewConfig() {
        // FIX 2: Disable Hardware Acceleration to prevent "White Screen" on Emulator
        mapPreview.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mapPreview.setTileSource(TileSourceFactory.MAPNIK);
        mapPreview.setMultiTouchControls(false); // Disable touch (static preview)
        mapPreview.getController().setZoom(16.0); // Set default zoom level
    }

    // --- HANDLE RESULT FROM MAP PICKER ACTIVITY ---
    private void setupMapPickerLauncher() {
        mapPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String address = data.getStringExtra("SELECTED_ADDRESS");
                        double lat = data.getDoubleExtra("SELECTED_LAT", 0);
                        double lon = data.getDoubleExtra("SELECTED_LON", 0);

                        // 1. Set the address text
                        locationInput.setText(address);

                        // 2. Show and Update Map Preview
                        if (lat != 0 && lon != 0) {
                            cardMapPreview.setVisibility(View.VISIBLE); // Show the card
                            GeoPoint point = new GeoPoint(lat, lon);

                            // Move camera to selected point
                            mapPreview.getController().setCenter(point);

                            // Add a marker to the preview map
                            mapPreview.getOverlays().clear();
                            Marker marker = new Marker(mapPreview);
                            marker.setPosition(point);
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            mapPreview.getOverlays().add(marker);

                            // Refresh map to ensure marker is drawn
                            mapPreview.invalidate();
                        }
                    }
                }
        );
    }

    private void openMapScreen() {
        Intent intent = new Intent(AddHikeActivity.this, PickLocationActivity.class);
        mapPickerLauncher.launch(intent);
    }

    // --- CRITICAL FIX 3: LIFECYCLE METHODS ---
    // Without these, the MapView will remain white/blank when returning from another activity.
    @Override
    protected void onResume() {
        super.onResume();
        if (mapPreview != null) {
            mapPreview.onResume(); // Resume map rendering
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapPreview != null) {
            mapPreview.onPause(); // Pause map rendering to save battery
        }
    }

    // --- HELPER METHODS (Date, Validation, Save) ---

    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, y, m, d) -> {
            dateInput.setText(String.format("%02d/%02d/%d", d, m + 1, y));
        }, year, month, day).show();
    }

    private boolean isInputValid() {
        boolean valid = true;
        if (nameInput.getText().toString().trim().isEmpty()) {
            nameInput.setError("Required");
            valid = false;
        }
        if (locationInput.getText().toString().trim().isEmpty()) {
            locationInput.setError("Required");
            valid = false;
        }
        if (dateInput.getText().toString().trim().isEmpty()) {
            dateInput.setError("Required");
            valid = false;
        }
        if (lengthInput.getText().toString().trim().isEmpty()) {
            lengthInput.setError("Required");
            valid = false;
        }
        if (parkingGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Parking required", Toast.LENGTH_SHORT).show();
            valid = false;
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
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid Length", Toast.LENGTH_SHORT).show();
            return;
        }

        hike.setDifficulty(difficultySpinner.getText().toString());
        hike.setParkingAvailable(parkingStatus);
        hike.setDescription(descriptionInput.getText().toString().trim());
        hike.setCustomField1(custom1Input.getText().toString().trim());
        hike.setCustomField2(custom2Input.getText().toString().trim());

        // Save to Database
        if (hikeDAO.addHike(hike) > 0) {
            Toast.makeText(this, "Hike Saved!", Toast.LENGTH_SHORT).show();
            finish(); // Return to previous screen
        } else {
            Toast.makeText(this, "Save Failed", Toast.LENGTH_SHORT).show();
        }
    }
}