package com.android2.calculator3.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.xlythe.engine.theme.App;

import java.util.ArrayList;
import java.util.List;

public abstract class AppDataSource implements DataSource {
    private static StoreHelper instance;
    // Database fields
    private SQLiteDatabase database;
    private StoreHelper dbHelper;

    public AppDataSource(Context context) {
        dbHelper = getHelper(context);
    }

    public static synchronized StoreHelper getHelper(Context context) {
        if(instance == null) instance = new StoreHelper(context);

        return instance;
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public String[] getColumns() {
        String[] allColumns = { StoreHelper.COLUMN_ID, StoreHelper.COLUMN_NAME, StoreHelper.COLUMN_PACKAGE, StoreHelper.COLUMN_PRICE, StoreHelper.COLUMN_IMAGE_URL };
        return allColumns;
    }

    public Cursor getRows() {
        Cursor cursor = database.query(getTableName(), getColumns(), null, null, null, null, null);
        return cursor;
    }

    public abstract String getTableName();

    public void createApp(App app) {
        ContentValues values = new ContentValues();
        values.put(StoreHelper.COLUMN_NAME, app.getName());
        values.put(StoreHelper.COLUMN_PACKAGE, app.getPackageName());
        values.put(StoreHelper.COLUMN_PRICE, app.getPrice());
        values.put(StoreHelper.COLUMN_IMAGE_URL, app.getImageUrl());
        database.insert(getTableName(), null, values);
    }

    public void createApps(List<App> apps) {
        for(App a : apps) {
            createApp(a);
        }
    }

    public void updateApp(App app) {
        ContentValues values = new ContentValues();
        values.put(StoreHelper.COLUMN_NAME, app.getName());
        values.put(StoreHelper.COLUMN_PRICE, app.getPrice());
        values.put(StoreHelper.COLUMN_IMAGE_URL, app.getImageUrl());
        database.update(getTableName(), values, String.format("%s = ?", StoreHelper.COLUMN_PACKAGE), new String[] { app.getPackageName() });
    }

    public void deleteApp(App app) {
        System.out.println("App deleted with package: " + app.getPackageName());
        database.delete(getTableName(), StoreHelper.COLUMN_PACKAGE + " = " + app.getPackageName(), null);
    }

    public void deleteApps() {
        database.delete(getTableName(), null, null);
    }

    public List<App> getAllApps() {
        List<App> apps = new ArrayList<App>();

        Cursor cursor = null;
        try {
            cursor = database.query(getTableName(), getColumns(), null, null, null, null, null);
        }
        catch(IllegalStateException e) {
            e.printStackTrace();
            return apps;
        }

        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            App app = cursorToApp(cursor);
            apps.add(app);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return apps;
    }

    private App cursorToApp(Cursor cursor) {
        App app = new App();
        app.setName(cursor.getString(1));
        app.setPackageName(cursor.getString(2));
        app.setPrice(cursor.getFloat(3));
        app.setImageUrl(cursor.getString(4));
        return app;
    }
}
