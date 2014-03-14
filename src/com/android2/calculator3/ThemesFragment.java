package com.android2.calculator3;

import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android2.calculator3.dao.ThemesDataSource;
import com.xlythe.engine.theme.App;

/**
 * @author Will Harmon
 **/
public class ThemesFragment extends ListFragment {
    private List<App> mThemes;
    private ThemesStoreTask mTask;
    private ThemesDataSource mDataSource;

    @Override
    public void onResume() {
        super.onResume();
        ((StoreAdapter) getListAdapter()).notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        setRetainInstance(true);

        // Load the cache
        mDataSource = new ThemesDataSource(getActivity());
        mDataSource.open();
        mThemes = mDataSource.getAllApps();

        // Show ui
        setListAdapter(new StoreAdapter(getActivity(), mThemes));

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(mThemes.isEmpty()) setListShown(false);

        // Load from server (and update ui when finished)
        mTask = new ThemesStoreTask(getActivity()) {
            @Override
            protected void onPostExecute(List<App> result) {
                super.onPostExecute(result);
                if(result == null) return;
                mThemes.clear();
                for(App a : result) {
                    mThemes.add(a);
                }
                ((StoreAdapter) getListAdapter()).notifyDataSetChanged();
                setListShown(true);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                setListShown(true);
            }
        };
        mTask.executeAsync();

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + mThemes.get(position).getPackageName()));
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTask.cancel(true);
        mDataSource.close();
    }

    @Override
    public void setListShown(boolean shown) {
        try {
            super.setListShown(shown);
        }
        catch(IllegalStateException e) {
            e.printStackTrace();
        }
    }
}
