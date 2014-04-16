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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ListAdapter;

import com.android.calculator2.dao.ThemesDataSource;
import com.xlythe.engine.theme.App;

import java.util.List;

public class ThemesFragment extends Fragment implements OnItemClickListener {
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
                if (!isDetached()) {
                    setViewShown(true);
                }
            }
        };

        mTask.executeAsync();
    }

    public void onListItemClick(GridView g, View v, int position, long id) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + mThemes.get(position).getPackageName()));
        startActivity(intent);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        onListItemClick(mGridView, mGridView.getChildAt(position), position,
                mGridView.getChildAt(position).getId());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTask.cancel(true);
        mDataSource.close();
    }
}
