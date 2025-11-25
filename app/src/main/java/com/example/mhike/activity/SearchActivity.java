package com.example.mhike.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mhike.R;
import com.example.mhike.adapter.HikeAdapter;
import com.example.mhike.database.DatabaseHelper;
import com.example.mhike.database.HikeDAO;
import com.example.mhike.model.Hike;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity implements HikeAdapter.OnItemClickListener {

    private EditText searchNameInput, searchLocationInput, searchLengthInput, searchDateInput;
    private Button searchButton;
    private RecyclerView recyclerViewResults;
    private HikeDAO hikeDAO;
    private HikeAdapter searchAdapter;
    private List<Hike> searchResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // --- CODE TO ADD ---
        // Find the toolbar
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        // Set the back arrow to finish the activity
        toolbar.setNavigationOnClickListener(v -> {
            finish();
        });
        // --- END OF CODE TO ADD ---

        hikeDAO = new HikeDAO(this);
        initializeViews();

        // Setup RecyclerView for results
        searchResults = new ArrayList<>();
        recyclerViewResults.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new HikeAdapter(this, searchResults, this);
        recyclerViewResults.setAdapter(searchAdapter);

        searchButton.setOnClickListener(v -> performSearch());

        // You should also add the DatePicker setup for searchDateInput here, similar to AddHikeActivity.
    }

    private void initializeViews() {
        // We are getting the non-TextInputLayout version of the IDs here.
        // This assumes your activity_search.xml TextInputEditTexts have these IDs.
        searchNameInput = findViewById(R.id.editTextSearchName);
        searchLocationInput = findViewById(R.id.editTextSearchLocation);
        searchLengthInput = findViewById(R.id.editTextSearchLength);
        searchDateInput = findViewById(R.id.editTextSearchDate);
        searchButton = findViewById(R.id.buttonPerformSearch);
        recyclerViewResults = findViewById(R.id.recyclerViewSearchResults);
    }

    // ... (Rest of your methods remain the same) ...

    private void performSearch() {
        String name = searchNameInput.getText().toString().trim();
        String location = searchLocationInput.getText().toString().trim();
        String length = searchLengthInput.getText().toString().trim();
        String date = searchDateInput.getText().toString().trim();

        // Lists to build the dynamic SQL query
        List<String> selectionParts = new ArrayList<>();
        List<String> selectionArgs = new ArrayList<>();

        // 1. Search by Name (Supports partial matching: "enter the first few letters of the name")
        if (!name.isEmpty()) {
            selectionParts.add(DatabaseHelper.COLUMN_NAME + " LIKE ?");
            selectionArgs.add("%" + name + "%");
        }

        // 2. Search by Location
        if (!location.isEmpty()) {
            selectionParts.add(DatabaseHelper.COLUMN_LOCATION + " LIKE ?");
            selectionArgs.add("%" + location + "%");
        }

        // 3. Search by Length (Advanced search requirement)
        if (!length.isEmpty()) {
            try {
                // Search for hikes greater than or equal to the input length
                selectionParts.add(DatabaseHelper.COLUMN_LENGTH + " >= ?");
                selectionArgs.add(length);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid length value.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 4. Search by Date (Advanced search requirement - exact match)
        if (!date.isEmpty()) {
            selectionParts.add(DatabaseHelper.COLUMN_DATE + " = ?");
            selectionArgs.add(date);
        }

        if (selectionParts.isEmpty()) {
            Toast.makeText(this, "Please enter at least one search term.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Combine all parts into a single WHERE clause
        String selectionClause = String.join(" AND ", selectionParts);
        String[] argsArray = selectionArgs.toArray(new String[0]);

        // Execute the search
        List<Hike> foundHikes = hikeDAO.searchHikes(selectionClause, argsArray);

        // Update the RecyclerView
        searchResults.clear();
        searchResults.addAll(foundHikes);
        searchAdapter.notifyDataSetChanged();

        if (foundHikes.isEmpty()) {
            Toast.makeText(this, "No matching hikes found.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onItemClick(Hike hike) {
        // Feature D requirement: "select an item from the resulting search list and to display its full details"

        // Reuse the HikeDetailActivity
        Intent intent = new Intent(SearchActivity.this, HikeDetailActivity.class);
        intent.putExtra("HIKE_ID", hike.getId());
        startActivity(intent);
    }
}

