package com.example.bytedance.auth;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.content.ContentValues;

public class UserDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "user_db.sqlite";
    private static final int DB_VERSION = 1;

    public static final String TABLE_USERS = "users";
    public static final String COL_ID = "id";
    public static final String COL_EMAIL = "email";
    public static final String COL_PASSWORD = "password";
    public static final String COL_NAME = "name";

    public UserDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_EMAIL + " TEXT UNIQUE, " +
                COL_PASSWORD + " TEXT, " +
                COL_NAME + " TEXT" +
                ")");
        // 预埋一个测试账号
        ContentValues cv = new ContentValues();
        cv.put(COL_EMAIL, "test@example.com");
        cv.put(COL_PASSWORD, "123456");
        cv.put(COL_NAME, "用户A");
        db.insert(TABLE_USERS, null, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 简单场景直接清表重建
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    public boolean validateUser(String email, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, new String[]{COL_ID}, COL_EMAIL + "=? AND " + COL_PASSWORD + "=?",
                new String[]{email, password}, null, null, null);
        boolean ok = (c != null && c.moveToFirst());
        if (c != null) c.close();
        return ok;
    }

    public String getNameByEmail(String email) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, new String[]{COL_NAME}, COL_EMAIL + "=?",
                new String[]{email}, null, null, null);
        String name = null;
        if (c != null && c.moveToFirst()) {
            name = c.getString(0);
        }
        if (c != null) c.close();
        return name;
    }
}