/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2.dao;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.xlythe.engine.theme.App;

public abstract class AppDataSource implements DataSource {
    private static StoreHelper instance;

    // Database fields
    private SQLiteDatabase database;
    private StoreHelper dbHelper;

    public AppDataSource(Context context) {
        dbHelper = getHelper(context);
    }

    public static synchronized StoreHelper getHelper(Context context) {
        if (instance == null) {
            instance = new StoreHelper(context);
        }

        return instance;
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public String[] getColumns() {
        String[] allColumns = { StoreHelper.COLUMN_ID, StoreHelper.COLUMN_NAME,
                StoreHelper.COLUMN_PACKAGE, StoreHelper.COLUMN_PRICE,
                StoreHelper.COLUMN_IMAGE_URL };

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
        for (App a : apps) {
            createApp(a);
        }
    }

    public void updateApp(App app) {
        ContentValues values = new ContentValues();
        values.put(StoreHelper.COLUMN_NAME, app.getName());
        values.put(StoreHelper.COLUMN_PRICE, app.getPrice());
        values.put(StoreHelper.COLUMN_IMAGE_URL, app.getImageUrl());
        database.update(getTableName(), values,
                String.format("%s = ?", StoreHelper.COLUMN_PACKAGE),
                new String[] { app.getPackageName() });
    }

    public void deleteApp(App app) {
        System.out.println("App deleted with package: " + app.getPackageName());
        database.delete(getTableName(), StoreHelper.COLUMN_PACKAGE
                + " = " + app.getPackageName(), null);
    }

    public void deleteApps() {
        database.delete(getTableName(), null, null);
    }

    public List<App> getAllApps() {
        List<App> apps = new ArrayList<App>();

        Cursor cursor = null;
        try {
            cursor = database.query(getTableName(), getColumns(), null, null, null, null, null);
        } catch(IllegalStateException e) {
            e.printStackTrace();
            return apps;
        }

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
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
