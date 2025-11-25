package com.example.mhike.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mhike.R;
import com.example.mhike.adapter.HikeAdapter;
import com.example.mhike.database.HikeDAO;
import com.example.mhike.model.Hike;

import java.util.ArrayList;
import java.util.List;

public class ListHikesActivity extends AppCompatActivity implements HikeAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private TextView noHikesTextView;
    private HikeDAO hikeDAO;
    private HikeAdapter hikeAdapter;
    private List<Hike> hikeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_hikes);

        // --- THIS IS THE CODE FOR THE BACK BUTTON ---
// Find the toolbar in the layout by its ID
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);

// Set a click listener on the navigation icon (the arrow)
        toolbar.setNavigationOnClickListener(v -> {
            finish(); // This command closes the current activity and goes back
        });
// --- END OF BACK BUTTON CODE ---

        // 1. Initialize Views and DAO
        recyclerView = findViewById(R.id.recyclerViewHikes);
        noHikesTextView = findViewById(R.id.textViewNoHikes);
        Button resetButton = findViewById(R.id.buttonResetDatabase);
        Button addButton = findViewById(R.id.buttonAddHike);

        hikeDAO = new HikeDAO(this);
        hikeList = new ArrayList<>();

        // 2. Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        hikeAdapter = new HikeAdapter(this, hikeList, this);
        recyclerView.setAdapter(hikeAdapter);

        // 3. Set up button listeners
        resetButton.setOnClickListener(v -> showResetConfirmationDialog());
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(ListHikesActivity.this, AddHikeActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load the hike list every time the activity is resumed (e.g., after returning from Add/Edit screen)
        loadHikesFromDatabase();
    }

    // --- Database Operations ---

    private void loadHikesFromDatabase() {
        // Use the DAO to fetch all hikes
        List<Hike> fetchedHikes = hikeDAO.getAllHikes();

        hikeList.clear();
        hikeList.addAll(fetchedHikes);
        hikeAdapter.notifyDataSetChanged();

        // Toggle 'No Hikes' message visibility
        if (hikeList.isEmpty()) {
            noHikesTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noHikesTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showResetConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Database Reset")
                .setMessage("Are you sure you want to delete ALL hike details? This action cannot be undone.")
                .setPositiveButton("DELETE ALL", (dialog, which) -> {
                    hikeDAO.deleteAllHikes();
                    Toast.makeText(this, "Database successfully reset.", Toast.LENGTH_SHORT).show();
                    loadHikesFromDatabase(); // Reload the empty list
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- HikeAdapter.OnItemClickListener Implementation ---

    @Override
    public void onItemClick(Hike hike) {
        // DEBUG: Log the ID to check its value before sending
        Log.d("ID_CHECK", "Hike ID is: " + hike.getId());

        // This handles the selection of a hike (Feature B: view/edit/delete individual hike)
        Intent intent = new Intent(ListHikesActivity.this, HikeDetailActivity.class);
        intent.putExtra("HIKE_ID", hike.getId());
        startActivity(intent);
    }
}
