package com.example.mhike.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.AutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mhike.R;
import com.example.mhike.database.HikeDAO;
import com.example.mhike.model.Hike;

import java.util.Calendar;

public class HikeDetailActivity extends AppCompatActivity {

    private EditText nameInput, locationInput, dateInput, lengthInput, descriptionInput, custom1Input, custom2Input;
    private RadioGroup parkingGroup;
    private AutoCompleteTextView difficultySpinner;
    private Button updateButton, deleteButton, viewObservationsButton;
    private HikeDAO hikeDAO;
    private Hike currentHike; // Stores the hike being edited
    private long hikeId;
    private DatePickerDialog datePickerDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hike_detail);

        // --- THIS IS THE CODE FOR THE BACK BUTTON ---
// Find the toolbar in the layout by its ID
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);

// Set a click listener on the navigation icon (the arrow)
        toolbar.setNavigationOnClickListener(v -> {
            finish(); // This command closes the current activity and goes back
        });
// --- END OF BACK BUTTON CODE ---

        // 1. Initialize Views and DAO
        initializeViews();
        hikeDAO = new HikeDAO(this);

        // 2. Get the Hike ID passed from the ListHikesActivity
        hikeId = (long) getIntent().getIntExtra("HIKE_ID", -1);

        if (hikeId != -1) {
            loadHikeDetails(hikeId);
        } else {
            Toast.makeText(this, "Error: Hike ID not found.", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if no ID is provided
        }

        // 3. Set up button listeners
        if (dateInput != null) {
            dateInput.setOnClickListener(v -> showDatePickerDialog());
        }
        updateButton.setOnClickListener(v -> updateHikeDetails());
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());
        viewObservationsButton.setOnClickListener(v -> {
            Intent intent = new Intent(HikeDetailActivity.this, ObservationActivity.class);
            intent.putExtra("HIKE_ID", hikeId);
            startActivity(intent);
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
        updateButton = findViewById(R.id.buttonUpdateHike);
        deleteButton = findViewById(R.id.buttonDeleteHike);
        viewObservationsButton = findViewById(R.id.buttonViewObservations);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.difficulty_levels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(adapter);
    }

    // --- Load Data (Feature B: View) ---
    private void loadHikeDetails(long id) {
        currentHike = hikeDAO.getHike(id); // You must add this method to HikeDAO

        if (currentHike != null) {
            // Fill the form fields with current data
            nameInput.setText(currentHike.getName());
            locationInput.setText(currentHike.getLocation());
            dateInput.setText(currentHike.getDate());
            lengthInput.setText(String.valueOf(currentHike.getLength()));
            descriptionInput.setText(currentHike.getDescription());
            custom1Input.setText(currentHike.getCustomField1());
            custom2Input.setText(currentHike.getCustomField2());

            // Set RadioGroup
            if ("Yes".equalsIgnoreCase(currentHike.getParkingAvailable())) {
                parkingGroup.check(R.id.radioYes);
            } else {
                parkingGroup.check(R.id.radioNo);
            }

            // Set Spinner Selection
            difficultySpinner.setText(currentHike.getDifficulty(), false);
        } else {
            Toast.makeText(this, "Hike record not found.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // --- Update Data (Feature B: Edit) ---
    private void updateHikeDetails() {
        // 1. Perform validation (can reuse isInputValid() logic from AddHikeActivity)
        // [Inference] Assuming a similar validation method is used here.

        // 2. Update the current Hike object with new data
        currentHike.setName(nameInput.getText().toString().trim());
        currentHike.setLocation(locationInput.getText().toString().trim());
        currentHike.setDate(dateInput.getText().toString().trim());

        try {
            currentHike.setLength(Double.parseDouble(lengthInput.getText().toString().trim()));
        } catch (NumberFormatException e) {
            lengthInput.setError("Invalid length value.");
            return;
        }

        currentHike.setDifficulty(difficultySpinner.getText().toString());

        int selectedParkingId = parkingGroup.getCheckedRadioButtonId();
        if (selectedParkingId != -1) {
            currentHike.setParkingAvailable(((RadioButton) findViewById(selectedParkingId)).getText().toString());
        } else {
            // Handle error if parking is somehow unchecked
            return;
        }

        currentHike.setDescription(descriptionInput.getText().toString().trim());
        currentHike.setCustomField1(custom1Input.getText().toString().trim());
        currentHike.setCustomField2(custom2Input.getText().toString().trim());

        // 3. Call the DAO method to update the database
        int rowsAffected = hikeDAO.updateHike(currentHike);

        if (rowsAffected > 0) {
            Toast.makeText(this, "Hike updated successfully!", Toast.LENGTH_SHORT).show();
            finish(); // Go back to the list activity to refresh
        } else {
            Toast.makeText(this, "Update failed.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Delete Data (Feature B: Delete) ---
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this hike? All related observations will also be deleted.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    int rowsAffected = hikeDAO.deleteHike(currentHike.getId());
                    if (rowsAffected > 0) {
                        Toast.makeText(this, "Hike deleted successfully.", Toast.LENGTH_SHORT).show();
                        finish(); // Go back to the list activity
                    } else {
                        Toast.makeText(this, "Deletion failed.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- Date Picker Logic (Reused) ---
    private void showDatePickerDialog() {
        // ... (Reused Date Picker code) ...
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String selectedDate = String.format("%02d/%02d/%d", dayOfMonth, monthOfYear + 1, year1);
                    dateInput.setText(selectedDate);
                }, year, month, day);

        datePickerDialog.show();
    }
}
