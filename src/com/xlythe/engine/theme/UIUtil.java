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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Display;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListAdapter;
import android.widget.ListView;

public class UIUtil {
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static void flipBackground(View v) {
        Drawable flipped = flipDrawable(v.getContext(), v.getBackground());
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            v.setBackground(flipped);
        } else {
            v.setBackgroundDrawable(flipped);
        }
    }

    public static Drawable flipDrawable(Context context, Drawable original) {
        Bitmap orig = drawableToBitmap(original);

        // Create a matrix to be used to transform the bitmap
        Matrix mirrorMatrix = new Matrix();

        // Set the matrix to mirror the image in the x direction
        mirrorMatrix.preScale(-1.0f, 1.0f);

        // Create a flipped sprite using the transform matrix and the original sprite
        Bitmap fSprite = Bitmap.createBitmap(orig, 0, 0, orig.getWidth(),
                orig.getHeight(), mirrorMatrix, false);

        return bitmapToDrawable(context, fSprite);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static Drawable bitmapToDrawable(Context context, Bitmap bitmap) {
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static int getWindowHeight(Context context) {
        Display display = ((WindowManager) context.getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay();

        if (android.os.Build.VERSION.SDK_INT >= 13) {
            Point size = new Point();
            display.getSize(size);
            size.y -= getStatusBarHeight(context);
            return size.y;
        } else {
            return display.getHeight();
        }
    }

    private static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier(
            "status_bar_height", "dimen", "android");

        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }

        return result;
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static int getWindowWidth(Context context) {
        Display display = ((WindowManager) context.getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay();

        if (android.os.Build.VERSION.SDK_INT >= 13) {
            Point size = new Point();
            display.getSize(size);
            return size.x;
        } else {
            return display.getWidth();
        }
    }

    public static void reverseLayout(ViewGroup vg) {
        View[] views = new View[vg.getChildCount()];
        for (int x = 0; x < vg.getChildCount(); x++) {
            views[x] = vg.getChildAt(x);
        }

        vg.removeAllViews();
        for (int x = views.length - 1; x >= 0; x--) {
            vg.addView(views[x]);
        }
    }

    public static void measureView(View v) {
        if (v.getHeight() == 0) {
            v.measure(MeasureSpec.makeMeasureSpec(getWindowWidth(v.getContext()),
                    MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(
                    Integer.MAX_VALUE, MeasureSpec.AT_MOST));

            layoutAllViews(v, 0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        }
    }

    private static void layoutAllViews(View v, int l, int t, int r, int b) {
        v.layout(l, t, r, b);
        if (ViewGroup.class.isAssignableFrom(v.getClass())) {
            ViewGroup fl = (ViewGroup) v;
            for (int i = 0; i < fl.getChildCount(); i++) {
                View child = fl.getChildAt(i);
                layoutAllViews(child, child.getLeft(), child.getTop(),
                        child.getRight(), child.getBottom());
            }
        }
    }

    public static void adjustListViewHeight(ListView listView) {
        ListAdapter mAdapter = listView.getAdapter();
        int totalHeight = 0;

        for (int i = 0; i < mAdapter.getCount(); i++) {
            View view = mAdapter.getView(i, null, listView);
            UIUtil.measureView(view);

            totalHeight += view.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (mAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}
