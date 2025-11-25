package com.example.mhike.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mhike.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnAddHike = findViewById(R.id.buttonGoToAddHike);
        Button btnViewHikes = findViewById(R.id.buttonGoToListHikes);
        Button btnSearchHikes = findViewById(R.id.buttonGoToSearch);

        btnAddHike.setOnClickListener(v ->
            startActivity(new Intent(MainActivity.this, AddHikeActivity.class)));

        btnViewHikes.setOnClickListener(v ->
            startActivity(new Intent(MainActivity.this, ListHikesActivity.class)));

        btnSearchHikes.setOnClickListener(v ->
            startActivity(new Intent(MainActivity.this, SearchActivity.class)));
    }
}