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

import java.io.Serializable;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

public class App implements Serializable {
    private static final long serialVersionUID = -7796311962836649402L;
    private String name;
    private String clazz;
    private transient Drawable image;
    private String packageName;
    private double price;
    private String imageUrl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClazz() {
        return name;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Drawable getImage() {
        return image;
    }

    public void setImage(Drawable image) {
        this.image = image;
    }

    public Intent getIntent(Context context) {
        Intent intent;
        if (clazz != null) {
            intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(new ComponentName(packageName, clazz));
        } else {
            intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        }

        int flags = Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED;

        intent.setFlags(flags);
        return intent;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public static boolean doesPackageExists(Context context, String targetPackage) {
        try {
            context.getPackageManager().getApplicationInfo(targetPackage, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static App getApp(Context context, String packageName) {
        App app = new App();

        try {
            PackageManager manager = context.getPackageManager();
            ResolveInfo info = manager.resolveActivity(
                    manager.getLaunchIntentForPackage(packageName), 0);

            app.name = info.loadLabel(manager).toString();
            app.image = info.loadIcon(manager);
            app.clazz = info.activityInfo.name;
            app.packageName = packageName;
        } catch (Exception e) {
            // Doesn't work on some older phones
            e.printStackTrace();

            // This does work, however. So we can get some basic information
            try {
                app.image = context.getPackageManager().getApplicationIcon(packageName);
            } catch (NameNotFoundException e1) {
                e1.printStackTrace();
            }

            app.packageName = packageName;
        }

        return app;
    }
}
