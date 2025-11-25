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
import android.widget.LinearLayout; // Needed for Edit Dialog
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

    private long hikeId;
    private HikeDAO hikeDAO;
    private ObservationAdapter adapter;
    private List<Observation> observationList;

    // UI Components
    private TextView parentHikeNameTextView;
    private EditText obsDetailInput, obsTimeInput, obsCommentInput;
    private Button addObservationButton;
    private RecyclerView recyclerViewObservations;
    private ImageView imageViewObservation;
    private Button btnAddPhoto;

    // Image Paths
    private String currentPhotoPath;
    private String tempCameraPath;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_observation);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        hikeDAO = new HikeDAO(this);

        initializeViews();
        setupImageLaunchers();

        hikeId = getIntent().getLongExtra("HIKE_ID", -1);
        if (hikeId == -1) {
            Toast.makeText(this, "Error: No Hike selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadParentHikeName();
        setupRecyclerView();
        setDefaultTime();
        loadObservations();

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
        adapter = new ObservationAdapter(this, observationList, this); // 'this' listens for clicks
        recyclerViewObservations.setAdapter(adapter);
    }

    // --- 1. THIS IS THE MISSING LOGIC FOR EDIT/DELETE ---
    @Override
    public void onItemClick(Observation observation) {
        // When user taps a row, ask what they want to do
        String[] options = {"Edit Observation", "Delete Observation"};
        new AlertDialog.Builder(this)
                .setTitle("Manage Observation")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditDialog(observation); // CALL EDIT
                    } else {
                        confirmDelete(observation);  // CALL DELETE
                    }
                })
                .show();
    }

    // --- 2. SHOW THE EDIT POPUP FORM ---
    private void showEditDialog(Observation observation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Observation");

        // Create layout programmatically
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

        // Update Button Logic
        builder.setPositiveButton("Update", (dialog, which) -> {
            String newDetail = inputDetail.getText().toString().trim();
            String newTime = inputTime.getText().toString().trim();
            String newComments = inputComments.getText().toString().trim();

            if (newDetail.isEmpty()) {
                Toast.makeText(this, "Detail is required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Set new values
            observation.setObservationDetail(newDetail);
            observation.setTimeOfObservation(newTime);
            observation.setAdditionalComments(newComments);

            // Save to DB
            int result = hikeDAO.updateObservation(observation);
            if (result > 0) {
                Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show();
                loadObservations(); // REFRESH LIST
            } else {
                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void confirmDelete(Observation observation) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Observation")
                .setMessage("Are you sure?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    hikeDAO.deleteObservation(observation.getId());
                    loadObservations();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onDeleteClick(Observation observation) {
        // Interface requirement, can redirect to confirmDelete
        confirmDelete(observation);
    }

    // --- EXISTING METHODS (ADD, LOAD, CAMERA) ---

    private void addObservation() {
        String detail = obsDetailInput.getText().toString().trim();
        String time = obsTimeInput.getText().toString().trim();
        String comment = obsCommentInput.getText().toString().trim();

        if (detail.isEmpty()) {
            obsDetailInput.setError("Required");
            return;
        }

        Observation observation = new Observation();
        observation.setHikeId((int) hikeId);
        observation.setObservationDetail(detail);
        observation.setTimeOfObservation(time);
        observation.setAdditionalComments(comment);
        if (currentPhotoPath != null) {
            observation.setImagePath(currentPhotoPath);
        }

        if (hikeDAO.addObservation(observation) > 0) {
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
            obsDetailInput.setText("");
            obsCommentInput.setText("");
            imageViewObservation.setImageResource(android.R.drawable.ic_menu_camera);
            currentPhotoPath = null;
            setDefaultTime();
            loadObservations();
        }
    }

    private void loadObservations() {
        observationList.clear();
        observationList.addAll(hikeDAO.getObservationsForHike(hikeId));
        adapter.notifyDataSetChanged();
    }

    private void loadParentHikeName() {
        Hike parentHike = hikeDAO.getHike(hikeId);
        if (parentHike != null) parentHikeNameTextView.setText("For Hike: " + parentHike.getName());
    }

    private void setDefaultTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        obsTimeInput.setText(sdf.format(new Date()));
    }

    // --- CAMERA / GALLERY LAUNCHERS ---
    private void setupImageLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        try {
                            Bitmap bitmap = BitmapFactory.decodeFile(tempCameraPath);
                            processAndDisplayImage(bitmap);
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        try {
                            Uri selectedImage = result.getData().getData();
                            InputStream imageStream = getContentResolver().openInputStream(selectedImage);
                            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                            processAndDisplayImage(bitmap);
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }
        );
    }

    private void processAndDisplayImage(Bitmap bitmap) throws IOException {
        File compressedFile = compressAndSaveImage(bitmap);
        currentPhotoPath = compressedFile.getAbsolutePath();
        imageViewObservation.setImageBitmap(BitmapFactory.decodeFile(currentPhotoPath));
    }

    private void showImagePickDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(this).setItems(options, (dialog, which) -> {
            if (which == 0) openCamera(); else openGallery();
        }).show();
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try { photoFile = createTempFile(); tempCameraPath = photoFile.getAbsolutePath(); }
            catch (IOException ex) { }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.example.mhike.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private File createTempFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
    }

    private File compressAndSaveImage(Bitmap bitmap) throws IOException {
        File file = createTempFile();
        FileOutputStream fos = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos);
        fos.close();
        return file;
    }
}