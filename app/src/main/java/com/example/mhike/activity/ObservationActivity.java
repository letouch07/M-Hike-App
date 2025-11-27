package com.example.mhike.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mhike.R;
import com.example.mhike.adapter.ObservationAdapter;
import com.example.mhike.database.HikeDAO;
import com.example.mhike.model.Hike;
import com.example.mhike.model.Observation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ObservationActivity extends AppCompatActivity implements ObservationAdapter.OnItemClickListener {

    // Unique ID of the Hike these observations belong to
    private long hikeId;

    // Database and List Adapter
    private HikeDAO hikeDAO;
    private ObservationAdapter adapter;
    private List<Observation> observationList;

    // UI Components (Input Form)
    private TextView parentHikeNameTextView;
    private EditText obsDetailInput, obsTimeInput, obsCommentInput;
    private Button addObservationButton;
    private RecyclerView recyclerViewObservations;
    private ImageView imageViewObservation;
    private Button btnAddPhoto;

    // Image Path Variables (For Camera/Gallery)
    private String currentPhotoPath; // Path of the final compressed image
    private Uri tempCameraUri;   // Temporary path for raw camera image

    // Activity Result Launchers (Replaces deprecated startActivityForResult)
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_observation);

        // Setup Toolbar with Back Navigation
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize Database Helper
        hikeDAO = new HikeDAO(this);

        // Initialize Views and Image Handling Logic
        initializeViews();
        setupImageLaunchers();

        // Retrieve Hike ID passed from the previous activity
        hikeId = getIntent().getLongExtra("HIKE_ID", -1);
        if (hikeId == -1) {
            Toast.makeText(this, "Error: No Hike selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load Initial Data
        loadParentHikeName();
        setupRecyclerView();
        setDefaultTime();
        loadObservations();

        // Set Click Listeners
        addObservationButton.setOnClickListener(v -> addObservation());
        btnAddPhoto.setOnClickListener(v -> showImagePickDialog());
    }

    private void initializeViews() {
        parentHikeNameTextView = findViewById(R.id.textViewParentHikeName);
        obsDetailInput = findViewById(R.id.editTextObsDetail);
        obsTimeInput = findViewById(R.id.editTextObsTime);
        obsCommentInput = findViewById(R.id.editTextObsComment);
        addObservationButton = findViewById(R.id.buttonAddObservation);
        recyclerViewObservations = findViewById(R.id.recyclerViewObservations);
        imageViewObservation = findViewById(R.id.imageViewObservation);
        btnAddPhoto = findViewById(R.id.buttonAddPhoto);
    }

    private void setupRecyclerView() {
        observationList = new ArrayList<>();
        recyclerViewObservations.setLayoutManager(new LinearLayoutManager(this));
        // Pass 'this' as the listener to handle item clicks
        adapter = new ObservationAdapter(this, observationList, this);
        recyclerViewObservations.setAdapter(adapter);
    }

    // --- CRUD: READ & DISPLAY DATA ---
    private void loadObservations() {
        // Fetch only observations linked to the current Hike ID
        List<Observation> fetchedObservations = hikeDAO.getObservationsForHike(hikeId);
        observationList.clear();
        observationList.addAll(fetchedObservations);
        adapter.notifyDataSetChanged();
    }

    private void loadParentHikeName() {
        Hike parentHike = hikeDAO.getHike(hikeId);
        if (parentHike != null) {
            parentHikeNameTextView.setText("Observations for: " + parentHike.getName());
        }
    }

    private void setDefaultTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        obsTimeInput.setText(sdf.format(new Date()));
    }

    // --- CRUD: CREATE (ADD NEW OBSERVATION) ---
    private void addObservation() {
        String detail = obsDetailInput.getText().toString().trim();
        String time = obsTimeInput.getText().toString().trim();
        String comment = obsCommentInput.getText().toString().trim();

        if (detail.isEmpty()) {
            obsDetailInput.setError("Detail is required.");
            return;
        }

        Observation observation = new Observation();
        observation.setHikeId((int) hikeId);
        observation.setObservationDetail(detail);
        observation.setTimeOfObservation(time);
        observation.setAdditionalComments(comment);

        // Save image path if a photo was taken/selected
        if (currentPhotoPath != null) {
            observation.setImagePath(currentPhotoPath);
        }

        // Insert into Database
        if (hikeDAO.addObservation(observation) > 0) {
            Toast.makeText(this, "Observation added successfully!", Toast.LENGTH_SHORT).show();
            // Clear form fields
            obsDetailInput.setText("");
            obsCommentInput.setText("");
            imageViewObservation.setImageResource(android.R.drawable.ic_menu_camera); // Reset image preview
            currentPhotoPath = null;
            setDefaultTime();
            loadObservations(); // Refresh the list
        } else {
            Toast.makeText(this, "Error adding observation.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- CRUD: UPDATE & DELETE (HANDLE LIST ITEM CLICKS) ---
    @Override
    public void onItemClick(Observation observation) {
        // Show a dialog asking the user to Edit or Delete
        String[] options = {"Edit Observation", "Delete Observation"};
        new AlertDialog.Builder(this)
                .setTitle("Manage Observation")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditDialog(observation); // Edit option
                    } else {
                        confirmDelete(observation);  // Delete option
                    }
                })
                .show();
    }

    // --- CRUD: UPDATE DIALOG LOGIC ---
    private void showEditDialog(Observation observation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Observation");

        // Create a custom layout for the dialog programmatically
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputDetail = new EditText(this);
        inputDetail.setHint("Observation Detail");
        inputDetail.setText(observation.getObservationDetail());
        layout.addView(inputDetail);

        final EditText inputTime = new EditText(this);
        inputTime.setHint("Time");
        inputTime.setText(observation.getTimeOfObservation());
        layout.addView(inputTime);

        final EditText inputComments = new EditText(this);
        inputComments.setHint("Comments");
        inputComments.setText(observation.getAdditionalComments());
        layout.addView(inputComments);

        builder.setView(layout);

        // Update Button Action
        builder.setPositiveButton("Update", (dialog, which) -> {
            String newDetail = inputDetail.getText().toString().trim();
            String newTime = inputTime.getText().toString().trim();
            String newComments = inputComments.getText().toString().trim();

            if (newDetail.isEmpty()) {
                Toast.makeText(this, "Detail is required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Set new values to the object
            observation.setObservationDetail(newDetail);
            observation.setTimeOfObservation(newTime);
            observation.setAdditionalComments(newComments);

            // Update Database
            int result = hikeDAO.updateObservation(observation);
            if (result > 0) {
                Toast.makeText(this, "Updated successfully!", Toast.LENGTH_SHORT).show();
                loadObservations(); // Refresh List
            } else {
                Toast.makeText(this, "Update failed.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // --- CRUD: DELETE LOGIC ---
    private void confirmDelete(Observation observation) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Observation")
                .setMessage("Are you sure you want to delete this observation?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Delete from Database
                    if (hikeDAO.deleteObservation(observation.getId()) > 0) {
                        Toast.makeText(this, "Deleted successfully.", Toast.LENGTH_SHORT).show();
                        loadObservations(); // Refresh List
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onDeleteClick(Observation observation) {
        confirmDelete(observation);
    }

    // --- FEATURE: CAMERA & GALLERY INTEGRATION ---
    private void setupImageLaunchers() {
        // Handle result from Camera
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result) {
                        try {
                            InputStream imageStream = getContentResolver().openInputStream(tempCameraUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                            processAndDisplayImage(bitmap);
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }
        );

        // Handle result from Gallery
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            InputStream imageStream = getContentResolver().openInputStream(uri);
                            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                            processAndDisplayImage(bitmap);
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }
        );
    }

    private void showImagePickDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openCamera(); else openGallery();
                }).show();
    }

    private void openCamera() {
        try {
            tempCameraUri = createImageFile();
            if(tempCameraUri != null) {
                cameraLauncher.launch(tempCameraUri);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    // --- IMAGE UTILITIES: COMPRESSION & STORAGE ---

    // Create a temporary file to store the raw image
    private Uri createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
        return FileProvider.getUriForFile(this, "com.example.mhike.fileprovider", imageFile);
    }

    // Compress the Bitmap to JPEG (50% quality) to save storage space and memory
    private File compressAndSaveImage(Bitmap bitmap) throws IOException {
        File file = createTempFileForCompression();
        FileOutputStream fos = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos); // 50% Quality
        fos.flush();
        fos.close();
        return file;
    }

    private File createTempFileForCompression() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("COMPRESSED_" + timeStamp + "_", ".jpg", storageDir);
    }

    // Display the processed image on the ImageView
    private void processAndDisplayImage(Bitmap bitmap) throws IOException {
        File compressedFile = compressAndSaveImage(bitmap);
        currentPhotoPath = compressedFile.getAbsolutePath(); // Save path for DB
        imageViewObservation.setImageBitmap(BitmapFactory.decodeFile(currentPhotoPath));
        Toast.makeText(this, "Image attached!", Toast.LENGTH_SHORT).show();
    }
}