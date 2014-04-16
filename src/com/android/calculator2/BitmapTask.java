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

package com.android.calculator2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

/**
 * A class for loading an image from a URL into an ImageView.
 */
public class BitmapTask extends AsyncTask<Void, Void, Bitmap> {
    private static final LruCache<String, Bitmap> LOADED_BITMAPS =
            new LruCache<String, Bitmap>(4 * 1024 * 1024);
    private static final String TAG = "BitmapTask";

    private final ImageView mImageView;
    private final String mURL;

    public BitmapTask(ImageView imageView, String url) {
        this.mImageView = imageView;
        mURL = url;
    }

    @Override
    protected Bitmap doInBackground(Void... args) {
        return getImageBitmap(mURL);
    }

    private Bitmap getImageBitmap(String url) {
        Bitmap bitmap = null;

        if (url != null && !"".equals(url)) {
            try {
                URL aURL = new URL(url);
                URLConnection conn = aURL.openConnection();
                conn.connect();

                InputStream is = conn.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                bitmap = BitmapFactory.decodeStream(bis);
                bis.close();
                is.close();

                cacheBitmap(mImageView.getContext(), bitmap, url);
            } catch (IOException e) {
                Log.e(TAG, "Error getting bitmap from url " + url, e);
            }
        }

        return bitmap;
    }

    private File getCacheFile(Context context, String url) {
        if (url != null && !"".equals(url)) {
            try {
                return new File(context.getCacheDir() + File.separator
                        + URLEncoder.encode(url, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Save bitmap to cache dir for quicker loading
     */
    private void cacheBitmap(Context context, Bitmap bitmap, String url) {
        if (url != null && !"".equals(url)) {
            Log.d(TAG, "Saving bitmap to memory.");
            if (bitmap != null) {
                LOADED_BITMAPS.put(url, bitmap);
            }

            File cache = getCacheFile(context, url);
            Log.d(TAG, "Got cache file at " + cache);

            if (!cache.isDirectory()) {
                Log.d(TAG, "Saving bitmap to disk now.");

                cache.delete();

                try {
                    FileOutputStream fos = new FileOutputStream(cache);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                    fos.close();
                    Log.d(TAG, "Bitmap saved to disk.");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Load bitmap from cache dir
     */
    private Bitmap loadCache(Context context, String url) {
        if (url != null && !"".equals(url)) {
            File cache = getCacheFile(context, url);

            if (!cache.isDirectory()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = BitmapFactory.decodeFile(cache.toString(), options);

                return bitmap;
            } else {
                return null;
            }
        }

        return null;
    }

    private Bitmap loadMemCache(String url) {
        if (url != null && !"".equals(url)) {
            Log.d(TAG, "Grabbing bitmap from memory with key: " + url);
            Bitmap bitmap = LOADED_BITMAPS.get(url);
            if (bitmap != null) {
                return bitmap;
            } else {
                Log.d(TAG, "But there was no bitmap in memory");
            }
        }

        return null;
    }

    @SuppressLint("NewApi")
    public void executeAsync(Void... args) {
        boolean pullFromOnline = true;
        System.out.println("I'm in async with url: " + mURL);

        Bitmap bitmap = loadMemCache(mURL);
        if (bitmap != null) {
            mImageView.setImageBitmap(bitmap);
            pullFromOnline = false;
        } else {
            bitmap = loadCache(mImageView.getContext(), mURL);
            if (bitmap != null) {
                mImageView.setImageBitmap(bitmap);
                System.out.println("I have loaded the bitmap");

                Log.d(TAG, "Saving bitmap to memory.");
                LOADED_BITMAPS.put(mURL, bitmap);
            }
        }

        if (pullFromOnline) {
            if (android.os.Build.VERSION.SDK_INT < 11) {
                execute(args);
            } else {
                executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
            }
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        super.onPostExecute(result);

        if (result != null) {
            mImageView.setImageBitmap(result);
        }
    }
}
