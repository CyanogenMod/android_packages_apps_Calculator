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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.ListAdapter;

import com.android.calculator2.dao.ThemesDataSource;
import com.xlythe.engine.theme.App;
import com.xlythe.engine.theme.Theme;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class ThemesFragment extends Fragment implements OnItemClickListener,
        OnItemLongClickListener {
    private static final String EXTRA_LIST_POSITION = "list_position";
    private static final String EXTRA_LIST_VIEW_OFFSET = "list_view_top";

    private GridView mGridView;
    private List<App> mThemes;
    private ThemesStoreTask mTask;
    private ThemesDataSource mDataSource;

    @Override
    public void onResume() {
        super.onResume();
        ((StoreAdapter) getListAdapter()).notifyDataSetChanged();
    }

    @Override
    public View inflateView(Bundle savedInstanceState) {
        // Create the GridView
        mGridView = new GridView(getActivity());
        mGridView.setOnItemClickListener(this);
        mGridView.setOnItemLongClickListener(this);
        mGridView.setNumColumns(GridView.AUTO_FIT);
        mGridView.setGravity(Gravity.CENTER);
        mGridView.setColumnWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                125 + 30, getActivity().getResources().getDisplayMetrics()));
        mGridView.setStretchMode(GridView.STRETCH_SPACING_UNIFORM);

        // Load the cache
        mDataSource = new ThemesDataSource(getActivity());
        mDataSource.open();
        mThemes = mDataSource.getAllApps();

        // Show ui
        setListAdapter(new StoreAdapter(getActivity(), mThemes));

        return mGridView;
    }

    public ListAdapter getListAdapter() {
        return mGridView.getAdapter();
    }

    public void setListAdapter(ListAdapter adapter) {
        mGridView.setAdapter(adapter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mThemes.isEmpty()) {
            setViewShown(false);
        }

        // Load from server (and update ui when finished)
        mTask = new ThemesStoreTask(getActivity()) {
            @Override
            protected void onPostExecute(List<App> result) {
                super.onPostExecute(result);
                if (result == null) {
                    return;
                }

                mThemes.clear();
                for (App a : result) {
                    mThemes.add(a);
                }

                if (!isDetached()) {
                    ((StoreAdapter) getListAdapter()).notifyDataSetChanged();
                    setViewShown(true);
                }
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                try {
                    setViewShown(true);
                } catch(IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        };

        mTask.executeAsync();
    }

    public void onListItemClick(int position) {
        if (App.doesPackageExists(getContext(),  mThemes.get(position).getPackageName())) {
            String appName = mThemes.get(position).getPackageName();

            // Update theme
            CalculatorSettings.setTheme(getContext(), appName);
            Theme.setPackageName(appName);

            // Create a new intent to relaunch the store
            Intent intent = new Intent(getActivity(), getActivity().getClass());

            // Preserve the list offsets
            int itemPosition = mGridView.getFirstVisiblePosition();
            View child = mGridView.getChildAt(0);
            int itemOffset = child != null ? child.getTop() : 0;

            intent.putExtra(EXTRA_LIST_POSITION, itemPosition);
            intent.putExtra(EXTRA_LIST_VIEW_OFFSET, itemOffset);

            // Go
            startActivity(intent);
            getActivity().finish();

            // Set a smooth fade transition
            getActivity().overridePendingTransition(
                    android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(
                    "market://details?id=" + mThemes.get(position).getPackageName()));
            startActivity(intent);
        }
    }

    public boolean onListItemLongClick(int position) {
        if (App.doesPackageExists(getContext(),  mThemes.get(position).getPackageName())) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(
                    "market://details?id=" + mThemes.get(position).getPackageName()));
            startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        onListItemClick(position);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return onListItemLongClick(position);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Restore the scroll position, if any
        final Bundle args = getArguments();
        if (args != null) {
            mGridView.setSelection(args.getInt(EXTRA_LIST_POSITION, 0));

            // Hack to scroll to the previous offset
            mGridView.post(new Runnable() {
                @Override
                public void run() {
                    int offset = args.getInt(EXTRA_LIST_VIEW_OFFSET, 0);
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        mGridView.scrollListBy(-1 * offset);
                    }
                    else {
                        try {
                            Method m = AbsListView.class.getDeclaredMethod("trackMotionScroll",
                                    Integer.TYPE, Integer.TYPE);
                            m.setAccessible(true);
                            m.invoke(mGridView, offset, offset);
                        } catch(Exception e) {
                            // Do nothing here
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTask.cancel(true);
        mDataSource.close();
    }
}
