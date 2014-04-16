/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

import com.android.calculator2.dao.ThemesDataSource;
import com.google.gson.Gson;
import com.xlythe.engine.theme.App;

public class ThemesStoreTask extends AsyncTask<String, String, List<App>> {
    private static final String THEME_URL =
            "https://raw.githubusercontent.com/CyanogenMod/android_packages_apps_Calculator/cm-11.0/themes.json";

    private final Context mContext;

    public ThemesStoreTask(Context context) {
        super();
        mContext = context.getApplicationContext();
    }

    @Override
    protected List<App> doInBackground(String... params) {
        // Grab data from server
        String result = "[]";
        try {
            URL url = new URL(THEME_URL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = urlConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(in);
            StringBuilder builder = new StringBuilder();

            int data = isr.read();
            while (data != -1) {
                char current = (char) data;
                data = isr.read();
                builder.append(current);
            }

            result = builder.toString();
        } catch(Exception e) {
            e.printStackTrace();
        }

        List<App> apps = null;
        try {
            // Parse result using gson
            App[] array = new Gson().fromJson(result, App[].class);
            apps = Arrays.asList(array);

            // Update database
            if (apps.size() > 0) {
                ThemesDataSource dataSource = new ThemesDataSource(mContext);
                dataSource.open();
                dataSource.deleteApps();
                dataSource.createApps(apps);
                dataSource.close();
            }
        } catch(Exception e) {
            // May have returned a 500, DB may be closed, or context may be null
            e.printStackTrace();
            cancel(true);
        }

        return apps;
    }

    @SuppressLint("NewApi")
    public void executeAsync() {
        if (android.os.Build.VERSION.SDK_INT < 11) {
            execute();
        } else {
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
}
