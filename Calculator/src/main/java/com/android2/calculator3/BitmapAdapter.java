package com.android2.calculator3;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

/**
 * A simplified adapter. Handles caching of views as well as loading images from a URL efficiently.
 */
public abstract class BitmapAdapter<T extends Serializable> extends ArrayAdapter<T> {
    private final Map<View, BitmapTask> mAsyncTasks = new WeakHashMap<View, BitmapTask>();
    private final List<T> mList;

    public BitmapAdapter(Context context, List<T> objects) {
        super(context, 0, objects);
        mList = objects;
    }

    /**
     * Returns the list the adapter is querying for data.
     */
    public List<T> getList() {
        return mList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = inflateView();
        }
        updateView(convertView, getItem(position));

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = inflateDropdownView();
        }
        updateDropdownView(convertView, getItem(position));

        return convertView;
    }

    /**
     * Updates the data in the adapter.
     */
    public void updateAdapter(List<T> data) {
        // Cache can be null
        if(data == null) return;

        List<T> adapterList = getList();
        adapterList.clear();
        for(T t : data) {
            adapterList.add(t);
        }
        notifyDataSetChanged();
    }

    /**
     * A helper method for loading images from a URL. Pass the convertView because there is some handling in the background for canceling the call if the user is scrolling away.
     */
    protected void grabImage(View convertView, ImageView iv, String url) {
        // Kill the previous async tasks
        BitmapTask previousTask = mAsyncTasks.get(convertView);
        if(previousTask != null) {
            previousTask.cancel(true);
        }
        BitmapTask newTask = new BitmapTask(iv, url);
        newTask.executeAsync();
        System.out.println("Execute has been called");
        mAsyncTasks.put(convertView, newTask);
    }

    /**
     * Called as infrequently as possible. Load your view, usually from xml, here.
     */
    public abstract View inflateView();

    /**
     * Passes the view from inflateView. There's a chance views like TextViews already have data, so make sure to clear everything you aren't updating. Also, avoid permanent UI changes like adding or removing views to convertView.
     */
    public abstract void updateView(View convertView, T object);

    /**
     * Called as infrequently as possible. Load your dropdown view, usually from xml, here.
     */
    public View inflateDropdownView() {
        return null;
    }

    /**
     * Passes the view from inflateDropdownView. There's a chance views like TextViews already have data, so make sure to clear everything you aren't updating. Also, avoid permanent UI changes like adding or removing views to convertView.
     */
    public void updateDropdownView(View convertView, T object) {}
}
