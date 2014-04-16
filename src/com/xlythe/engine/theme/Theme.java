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

package com.xlythe.engine.theme;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.util.SparseArray;

public class Theme {
    public static final String COLOR = "color";
    public static final String RAW = "raw";
    public static final String DRAWABLE = "drawable";
    public static final String STRING = "string";
    public static final String BOOLEAN = "bool";
    private static String PACKAGE_NAME;

    private static SparseArray<Theme.Res> RES_MAP;
    private static final Map<String, Typeface> TYPEFACE_MAP =
            new HashMap<String, Typeface>();
    private static final LruCache<String, Drawable> DRAWABLE_MAP =
            new LruCache<String, Drawable>(4 * 1024 * 1024);
    private static final LruCache<String, Integer> COLOR_MAP =
            new LruCache<String, Integer>(1 * 1024 * 1024);
    private static final LruCache<String, ColorStateList> COLOR_STATE_LIST_MAP =
            new LruCache<String, ColorStateList>(1 * 1024 * 1024);

    public static class Res {
        private final String type;
        private final String name;

        private Res(String type, String name) {
            this.type = type;
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }
    }

    @SuppressWarnings("rawtypes")
    public static void buildResourceMap(Class r) {
        RES_MAP = new SparseArray<Theme.Res>();
        try {
            Log.d("Theme", "Building resource map");

            Class color = Class.forName(r.getName() + "$color");
            for (Field f : color.getFields()) {
                RES_MAP.put(f.getInt(null), new Res(COLOR, f.getName()));
            }
            Log.d("Theme", "color loaded");

            Class drawable = Class.forName(r.getName() + "$drawable");
            for (Field f : drawable.getFields()) {
                RES_MAP.put(f.getInt(null), new Res(DRAWABLE, f.getName()));
            }
            Log.d("Theme", "drawable loaded");

            Class bool = Class.forName(r.getName() + "$bool");
            for (Field f : bool.getFields()) {
                RES_MAP.put(f.getInt(null), new Res(BOOLEAN, f.getName()));
            }
            Log.d("Theme", "bool loaded");

            Class raw = Class.forName(r.getName() + "$raw");
            for (Field f : raw.getFields()) {
                RES_MAP.put(f.getInt(null), new Res(RAW, f.getName()));
            }
            Log.d("Theme", "raw loaded");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // Do nothing here
        }
    }

    public static Context getThemeContext(Context context) {
        try {
            return context.createPackageContext(getPackageName(), context.CONTEXT_INCLUDE_CODE
                    + Context.CONTEXT_IGNORE_SECURITY);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Grabs the Resources from packageName
     */
    public static Resources getResources(Context context) {
        try {
            return context.getPackageManager().getResourcesForApplication(getPackageName());
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return context.getResources();
        }
    }

    /**
     * Gets id from theme apk
     */
    public static int getId(Context context, String type, String name) {
        return getResources(context).getIdentifier(name, type, getPackageName());
    }

    /**
     * Gets string from theme apk
     */
    public static String getString(Context context, int resId) {
        return getString(context, Theme.get(resId));
    }

    /**
     * Gets string from theme apk
     */
    public static String getString(Context context, Res res) {
        return getString(context, res.getName());
    }

    /**
     * Gets string from theme apk
     */
    public static String getString(Context context, String name) {
        int id = getId(context, STRING, name);

        if (id == 0) {
            return null;
        }

        return getResources(context).getString(id);
    }

    /**
     * Gets string from theme apk
     */
    public static Boolean getBoolean(Context context, int resId) {
        return getBoolean(context, Theme.get(resId));
    }

    /**
     * Gets string from theme apk
     */
    public static Boolean getBoolean(Context context, Res res) {
        return getBoolean(context, res.getName());
    }

    /**
     * Gets string from theme apk
     */
    public static Boolean getBoolean(Context context, String name) {
        int id = getId(context, BOOLEAN, name);

        if (id == 0) {
            return null;
        }

        return getResources(context).getBoolean(id);
    }

    /**
     * Gets drawable from theme apk
     */
    public static Drawable getDrawable(Context context, int resId) {
        return getDrawable(context, Theme.get(resId));
    }

    /**
     * Gets drawable from theme apk
     */
    public static Drawable getDrawable(Context context, Res res) {
        return getDrawable(context, res.getName());
    }

    /**
     * Gets drawable from theme apk
     */
    public static Drawable getDrawable(Context context, String name) {
        if (DRAWABLE_MAP.get(getKey(context) + "_" + name) != null) {
            return DRAWABLE_MAP.get(getKey(context) + "_" + name).getConstantState().newDrawable();
        }

        int id = getId(context, DRAWABLE, name);
        if (id == 0) {
            id = context.getResources().getIdentifier(name, DRAWABLE, context.getPackageName());
            if (id != 0) {
                DRAWABLE_MAP.put(getKey(context) + "_" + name,
                    context.getResources().getDrawable(id));
                return DRAWABLE_MAP.get(getKey(context) + "_" + name);
            } else {
                return null;
            }
        }

        DRAWABLE_MAP.put(getKey(context) + "_" + name, getResources(context).getDrawable(id));
        return DRAWABLE_MAP.get(getKey(context) + "_" + name);
    }

    /**
     * Gets color from theme apk
     */
    public static int getColor(Context context, int resId) {
        return getColor(context, Theme.get(resId));
    }

    /**
     * Gets color from theme apk
     */
    public static int getColor(Context context, Res res) {
        return getColor(context, res.getName());
    }

    /**
     * Gets color from theme apk
     */
    public static int getColor(Context context, String name) {
        if (COLOR_MAP.get(getKey(context) + "_" + name) != null) {
            return COLOR_MAP.get(getKey(context) + "_" + name);
        }

        int id = getId(context, COLOR, name);
        if (id == 0) {
            id = context.getResources().getIdentifier(name, COLOR, context.getPackageName());
            COLOR_MAP.put(getKey(context) + "_" + name, context.getResources().getColor(id));
            return COLOR_MAP.get(getKey(context) + "_" + name);
        }

        COLOR_MAP.put(getKey(context) + "_" + name, getResources(context).getColor(id));
        return COLOR_MAP.get(getKey(context) + "_" + name);
    }

    /**
     * Gets color from theme apk
     */
    public static ColorStateList getColorStateList(Context context, int resId) {
        return getColorStateList(context, Theme.get(resId));
    }

    /**
     * Gets color from theme apk
     */
    public static ColorStateList getColorStateList(Context context, Res res) {
        return getColorStateList(context, res.getName());
    }

    /**
     * Gets color from theme apk
     */
    public static ColorStateList getColorStateList(Context context, String name) {
        if (COLOR_STATE_LIST_MAP.get(getKey(context) + "_" + name) != null) {
            return COLOR_STATE_LIST_MAP.get(getKey(context) + "_" + name);
        }

        int id = getId(context, COLOR, name);
        if (id == 0) {
            id = context.getResources().getIdentifier(name, COLOR, context.getPackageName());
            COLOR_STATE_LIST_MAP.put(getKey(context) + "_" + name, context.getResources()
                    .getColorStateList(id));
            return COLOR_STATE_LIST_MAP.get(getKey(context) + "_" + name);
        }

        COLOR_STATE_LIST_MAP.put(getKey(context) + "_" + name, getResources(context)
                .getColorStateList(id));
        return COLOR_STATE_LIST_MAP.get(getKey(context) + "_" + name);
    }

    /**
     * Gets android theme from theme apk. Can be 0 (no theme).
     */
    public static int getTheme(Context context) {
        int id = getId(context, "string", "app_theme");
        if (id == 0) {
            return 0;
        }

        String fieldName = getResources(context).getString(id).replace(".", "_");
        try {
            Field field = android.R.style.class.getField(fieldName);
            return field.getInt(null);
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Gets android theme from theme apk. Can be 0 (no theme). This is for apps that want an
     * actionbar in their Settings but not in their main app.
     */
    public static int getSettingsTheme(Context context) {
        int id = getId(context, "string", "app_settings_theme");
        if (id == 0) {
            return 0;
        }

        String fieldName = getResources(context).getString(id).replace(".", "_");
        try {
            Field field = android.R.style.class.getField(fieldName);
            return field.getInt(null);
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Returns whether the theme is light or dark. WARNING: Assumes dark if no resource is found.
     */
    public static boolean isLightTheme(Context context) {
        int id = getId(context, "string", "app_theme");
        if (id != 0) {
            String fieldName = getResources(context).getString(id).replace(".", "_");
            return fieldName.toLowerCase(Locale.US).contains("light");
        } else {
            return false;
        }
    }

    public static String getPackageName() {
        return PACKAGE_NAME;
    }

    public static void setPackageName(String packageName) {
        PACKAGE_NAME = packageName;
    }

    public static Res get(int resId) {
        return RES_MAP.get(resId);
    }

    public static String getSoundPath(Context context, Res res) {
        int id = getId(context, res.getType(), res.getName());
        if (id == 0) {
            id = context.getResources().getIdentifier(res.getName(), res.getType(),
                    context.getPackageName());
            return "android.resource://" + context.getPackageName() + "/" + id;
        }

        return "android.resource://" + getPackageName() + "/" + id;
    }

    public static int getSound(Context context, SoundPool soundPool, Res res) {
        int id = getId(context, res.getType(), res.getName());
        if (id == 0) {
            id = context.getResources().getIdentifier(res.getName(), res.getType(),
                    context.getPackageName());
            return soundPool.load(context, id, 1);
        }

        return soundPool.load(getThemeContext(context), id, 1);
    }

    public static long getDurationOfSound(Context context, Theme.Res res) {
        int millis = 0;
        MediaPlayer mp = new MediaPlayer();
        try {
            AssetFileDescriptor afd;
            int id = getId(context, res.getType(), res.getName());
            if (id == 0) {
                id = context.getResources().getIdentifier(res.getName(), res.getType(),
                        context.getPackageName());
                afd = context.getResources().openRawResourceFd(id);
            }

            afd = getThemeContext(context).getResources().openRawResourceFd(id);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.prepare();
            millis = mp.getDuration();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mp.release();
            mp = null;
        }

        return millis;
    }

    public static Typeface getFont(Context context) {
        return getFont(context, "font");
    }

    public static Typeface getFont(Context context, String name) {
        String key = getKey(context) + "_" + name;
        if (TYPEFACE_MAP.containsKey(key)) {
            return TYPEFACE_MAP.get(key);
        }

        String[] extensions = { ".ttf", ".otf" };
        for (String s : extensions) {
            try {
                // Use cursor loader to grab font
                String url = getPackageName() + ".FileProvider/" + name + s;
                Uri uri = Uri.parse("content://" + url);
                ContentResolver cr = context.getContentResolver();
                AssetFileDescriptor a = cr.openAssetFileDescriptor(uri, null);
                FileInputStream in = new FileInputStream(a.getFileDescriptor());
                in.skip(a.getStartOffset());
                File file = new File(context.getCacheDir(), name + s);
                file.createNewFile();
                FileOutputStream fOutput = new FileOutputStream(file);
                byte[] dataBuffer = new byte[1024];
                int readLength = 0;
                while ((readLength = in.read(dataBuffer)) != -1) {
                    fOutput.write(dataBuffer, 0, readLength);
                }
                in.close();
                fOutput.close();

                // Try/catch for broken fonts
                Typeface t = Typeface.createFromFile(file);
                TYPEFACE_MAP.put(key, t);
                return TYPEFACE_MAP.get(key);
            } catch (Exception e) {
                // Do nothing here
            }
        }

        TYPEFACE_MAP.put(key, null);
        return TYPEFACE_MAP.get(key);
    }

    /**
     * Returns a list of installed apps that are registered as themes
     */
    public static List<App> getApps(Context context) {
        LinkedList<App> apps = new LinkedList<App>();
        PackageManager manager = context.getPackageManager();

        Intent mainIntent = new Intent(context.getPackageName() + ".THEME", null);

        final List<ResolveInfo> infos;
        try {
            infos = manager.queryIntentActivities(mainIntent, 0);
        } catch (Exception e) {
            e.printStackTrace();
            return apps;
        }

        for (ResolveInfo info : infos) {
            App app = new App();
            apps.add(app);

            app.setName(info.loadLabel(manager).toString());
            app.setPackageName(info.activityInfo.applicationInfo.packageName);
        }

        return apps;
    }

    private static String getKey(Context context) {
        return context.getPackageName() + "_" + getPackageName();
    }
}
