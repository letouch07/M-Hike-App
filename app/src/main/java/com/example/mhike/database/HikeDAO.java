package com.example.mhike.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.mhike.model.Hike;
import com.example.mhike.model.Observation;

import java.util.ArrayList;
import java.util.List;

public class HikeDAO {

    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;

    // --- HIKE COLUMNS ---
    private String[] allHikeColumns = {
            DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_NAME,
            DatabaseHelper.COLUMN_LOCATION,
            DatabaseHelper.COLUMN_DATE,
            DatabaseHelper.COLUMN_PARKING,
            DatabaseHelper.COLUMN_LENGTH,
            DatabaseHelper.COLUMN_DIFFICULTY,
            DatabaseHelper.COLUMN_DESCRIPTION,
            DatabaseHelper.COLUMN_CUSTOM_1,
            DatabaseHelper.COLUMN_CUSTOM_2
    };

    // --- OBSERVATION COLUMNS ---
    private String[] allObservationColumns = {
            DatabaseHelper.COLUMN_OBS_ID,
            DatabaseHelper.COLUMN_OBS_HIKE_ID,
            DatabaseHelper.COLUMN_OBS_DETAIL,
            DatabaseHelper.COLUMN_OBS_TIME,
            DatabaseHelper.COLUMN_OBS_COMMENT,
            DatabaseHelper.COLUMN_OBS_IMAGE
    };

    public HikeDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    // =================================================================
    // SECTION A & B: HIKE CRUD OPERATIONS
    // =================================================================

    public long addHike(Hike hike) {
        open();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, hike.getName());
        values.put(DatabaseHelper.COLUMN_LOCATION, hike.getLocation());
        values.put(DatabaseHelper.COLUMN_DATE, hike.getDate());
        values.put(DatabaseHelper.COLUMN_PARKING, hike.getParkingAvailable());
        values.put(DatabaseHelper.COLUMN_LENGTH, hike.getLength());
        values.put(DatabaseHelper.COLUMN_DIFFICULTY, hike.getDifficulty());
        values.put(DatabaseHelper.COLUMN_DESCRIPTION, hike.getDescription());
        values.put(DatabaseHelper.COLUMN_CUSTOM_1, hike.getCustomField1());
        values.put(DatabaseHelper.COLUMN_CUSTOM_2, hike.getCustomField2());

        long insertId = database.insert(DatabaseHelper.TABLE_HIKES, null, values);
        close();
        return insertId;
    }

    public List<Hike> getAllHikes() {
        List<Hike> hikes = new ArrayList<>();
        open();
        Cursor cursor = database.query(DatabaseHelper.TABLE_HIKES, allHikeColumns, null, null, null, null, DatabaseHelper.COLUMN_ID + " DESC");
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Hike hike = cursorToHike(cursor);
            hikes.add(hike);
            cursor.moveToNext();
        }
        cursor.close();
        close();
        return hikes;
    }

    public Hike getHike(long hikeId) {
        Hike hike = null;
        open();
        Cursor cursor = database.query(DatabaseHelper.TABLE_HIKES, allHikeColumns, DatabaseHelper.COLUMN_ID + " = " + hikeId, null, null, null, null);
        if (cursor.moveToFirst()) {
            hike = cursorToHike(cursor);
        }
        cursor.close();
        close();
        return hike;
    }

    public int updateHike(Hike hike) {
        open();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, hike.getName());
        values.put(DatabaseHelper.COLUMN_LOCATION, hike.getLocation());
        values.put(DatabaseHelper.COLUMN_DATE, hike.getDate());
        values.put(DatabaseHelper.COLUMN_PARKING, hike.getParkingAvailable());
        values.put(DatabaseHelper.COLUMN_LENGTH, hike.getLength());
        values.put(DatabaseHelper.COLUMN_DIFFICULTY, hike.getDifficulty());
        values.put(DatabaseHelper.COLUMN_DESCRIPTION, hike.getDescription());
        values.put(DatabaseHelper.COLUMN_CUSTOM_1, hike.getCustomField1());
        values.put(DatabaseHelper.COLUMN_CUSTOM_2, hike.getCustomField2());

        int rowsAffected = database.update(DatabaseHelper.TABLE_HIKES, values, DatabaseHelper.COLUMN_ID + " = ?", new String[] { String.valueOf(hike.getId()) });
        close();
        return rowsAffected;
    }

    public int deleteHike(long hikeId) {
        open();
        // Also delete associated observations to keep DB clean (Cascade delete logic)
        database.delete(DatabaseHelper.TABLE_OBSERVATIONS, DatabaseHelper.COLUMN_OBS_HIKE_ID + " = " + hikeId, null);

        int rowsAffected = database.delete(DatabaseHelper.TABLE_HIKES, DatabaseHelper.COLUMN_ID + " = " + hikeId, null);
        close();
        return rowsAffected;
    }

    public void deleteAllHikes() {
        open();
        database.delete(DatabaseHelper.TABLE_OBSERVATIONS, null, null); // Clear observations first
        database.delete(DatabaseHelper.TABLE_HIKES, null, null);
        close();
    }

    // =================================================================
    // SECTION C: OBSERVATION CRUD OPERATIONS
    // =================================================================

    public long addObservation(Observation observation) {
        open();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_OBS_HIKE_ID, observation.getHikeId());
        values.put(DatabaseHelper.COLUMN_OBS_DETAIL, observation.getObservationDetail());
        values.put(DatabaseHelper.COLUMN_OBS_TIME, observation.getTimeOfObservation());
        values.put(DatabaseHelper.COLUMN_OBS_COMMENT, observation.getAdditionalComments());
        values.put(DatabaseHelper.COLUMN_OBS_IMAGE, observation.getImagePath());

        long insertId = database.insert(DatabaseHelper.TABLE_OBSERVATIONS, null, values);
        close();
        return insertId;
    }

    public List<Observation> getObservationsForHike(long hikeId) {
        List<Observation> observations = new ArrayList<>();
        open();

        // Critical Fix: Filter WHERE hike_id = ?
        String selection = DatabaseHelper.COLUMN_OBS_HIKE_ID + " = ?";
        String[] selectionArgs = { String.valueOf(hikeId) };

        Cursor cursor = database.query(DatabaseHelper.TABLE_OBSERVATIONS, allObservationColumns, selection, selectionArgs, null, null, DatabaseHelper.COLUMN_OBS_TIME + " DESC");

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Observation observation = cursorToObservation(cursor);
            observations.add(observation);
            cursor.moveToNext();
        }
        cursor.close();
        close();
        return observations;
    }

    // NEW: Update Observation Method
    public int updateObservation(Observation obs) {
        open();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_OBS_DETAIL, obs.getObservationDetail());
        values.put(DatabaseHelper.COLUMN_OBS_TIME, obs.getTimeOfObservation());
        values.put(DatabaseHelper.COLUMN_OBS_COMMENT, obs.getAdditionalComments());

        // Only update image path if it's not null (user might just edit text)
        if (obs.getImagePath() != null) {
            values.put(DatabaseHelper.COLUMN_OBS_IMAGE, obs.getImagePath());
        }

        int rows = database.update(DatabaseHelper.TABLE_OBSERVATIONS, values, DatabaseHelper.COLUMN_OBS_ID + " = ?", new String[]{String.valueOf(obs.getId())});
        close();
        return rows;
    }

    public int deleteObservation(long observationId) {
        open();
        int rowsAffected = database.delete(DatabaseHelper.TABLE_OBSERVATIONS, DatabaseHelper.COLUMN_OBS_ID + " = " + observationId, null);
        close();
        return rowsAffected;
    }

    // =================================================================
    // SECTION D: SEARCH OPERATIONS
    // =================================================================

    public List<Hike> searchHikes(String selection, String[] selectionArgs) {
        List<Hike> hikes = new ArrayList<>();
        open();
        Cursor cursor = database.query(DatabaseHelper.TABLE_HIKES, allHikeColumns, selection, selectionArgs, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Hike hike = cursorToHike(cursor);
            hikes.add(hike);
            cursor.moveToNext();
        }
        cursor.close();
        close();
        return hikes;
    }

    // =================================================================
    // HELPERS (Cursor Converters)
    // =================================================================

    private Hike cursorToHike(Cursor cursor) {
        Hike hike = new Hike();
        hike.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
        hike.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)));
        hike.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LOCATION)));
        hike.setDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE)));
        hike.setParkingAvailable(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PARKING)));
        hike.setLength(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LENGTH)));
        hike.setDifficulty(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DIFFICULTY)));
        hike.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DESCRIPTION)));
        hike.setCustomField1(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOM_1)));
        hike.setCustomField2(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOM_2)));
        return hike;
    }

    private Observation cursorToObservation(Cursor cursor) {
        Observation observation = new Observation();
        observation.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_OBS_ID)));
        observation.setHikeId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_OBS_HIKE_ID)));
        observation.setObservationDetail(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_OBS_DETAIL)));
        observation.setTimeOfObservation(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_OBS_TIME)));
        observation.setAdditionalComments(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_OBS_COMMENT)));

        // Handle potential null image path
        int imageIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_OBS_IMAGE);
        if (imageIndex != -1) {
            observation.setImagePath(cursor.getString(imageIndex));
        }

        return observation;
    }
}