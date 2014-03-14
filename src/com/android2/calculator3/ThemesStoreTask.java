package com.android2.calculator3;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

import com.android2.calculator3.dao.ThemesDataSource;
import com.google.gson.Gson;
import com.xlythe.engine.theme.App;

public class ThemesStoreTask extends AsyncTask<String, String, List<App>> {
    private static final String THEME_URL = "http://xlythe.com/saolauncher/store/themes.html";

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
            while(data != -1) {
                char current = (char) data;
                data = isr.read();
                builder.append(current);
            }
            result = builder.toString();
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        List<App> apps = null;
        try {
            // Parse result using gson
            App[] array = new Gson().fromJson(result, App[].class);
            apps = Arrays.asList(array);

            // Update database
            ThemesDataSource dataSource = new ThemesDataSource(mContext);
            dataSource.open();
            dataSource.deleteApps();
            dataSource.createApps(apps);
        }
        catch(Exception e) {
            // May have returned a 500 DB may be closed or context may be null
            e.printStackTrace();
            cancel(true);
        }

        return apps;
    }

    @SuppressLint("NewApi")
    public void executeAsync() {
        if(android.os.Build.VERSION.SDK_INT < 11) {
            execute();
        }
        else {
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
}
