package com.example.mhike.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database constants
    private static final String DATABASE_NAME = "M_HIKE_DB";
    private static final int DATABASE_VERSION = 1;

    // --- Hike Table (Feature B) ---
    public static final String TABLE_HIKES = "hikes";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_LOCATION = "location";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_PARKING = "parking_available";
    public static final String COLUMN_LENGTH = "length";
    public static final String COLUMN_DIFFICULTY = "difficulty";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_CUSTOM_1 = "custom_field_1";
    public static final String COLUMN_CUSTOM_2 = "custom_field_2";

    // --- Observation Table (Feature C) ---
    public static final String TABLE_OBSERVATIONS = "observations";
    public static final String COLUMN_OBS_ID = "_obs_id";
    public static final String COLUMN_OBS_HIKE_ID = "hike_id";
    public static final String COLUMN_OBS_DETAIL = "observation_detail";
    public static final String COLUMN_OBS_TIME = "time_of_observation";
    public static final String COLUMN_OBS_COMMENT = "additional_comments";

    // [MỚI] Cột lưu đường dẫn ảnh cho Feature G
    public static final String COLUMN_OBS_IMAGE = "image_path";

    // SQL statement to create the HIKES table
    private static final String CREATE_TABLE_HIKES = "CREATE TABLE "
            + TABLE_HIKES + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_NAME + " TEXT NOT NULL, "
            + COLUMN_LOCATION + " TEXT NOT NULL, "
            + COLUMN_DATE + " TEXT NOT NULL, "
            + COLUMN_PARKING + " TEXT NOT NULL, "
            + COLUMN_LENGTH + " REAL NOT NULL, "
            + COLUMN_DIFFICULTY + " TEXT NOT NULL, "
            + COLUMN_DESCRIPTION + " TEXT, "
            + COLUMN_CUSTOM_1 + " TEXT, "
            + COLUMN_CUSTOM_2 + " TEXT"
            + ");";

    // SQL statement to create the OBSERVATIONS table
    // [CẬP NHẬT] Đã thêm cột image_path vào câu lệnh tạo bảng
    private static final String CREATE_TABLE_OBSERVATIONS = "CREATE TABLE "
            + TABLE_OBSERVATIONS + " ("
            + COLUMN_OBS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_OBS_HIKE_ID + " INTEGER NOT NULL, "
            + COLUMN_OBS_DETAIL + " TEXT NOT NULL, "
            + COLUMN_OBS_TIME + " TEXT NOT NULL, "
            + COLUMN_OBS_COMMENT + " TEXT, "
            + COLUMN_OBS_IMAGE + " TEXT, " // <--- Thêm dòng này
            + "FOREIGN KEY(" + COLUMN_OBS_HIKE_ID + ") REFERENCES " + TABLE_HIKES + "(" + COLUMN_ID + ") ON DELETE CASCADE"
            + ");";

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_HIKES);
        db.execSQL(CREATE_TABLE_OBSERVATIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_OBSERVATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HIKES);
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }
}