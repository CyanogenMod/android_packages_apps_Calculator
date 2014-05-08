package com.android2.calculator3;

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

import com.android2.calculator3.dao.ThemesDataSource;
import com.xlythe.engine.theme.App;
import com.xlythe.engine.theme.Theme;

import java.util.List;

/**
 * @author Will Harmon
 */
public class ThemesFragment extends Fragment implements OnItemClickListener {
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
		mGridView.setNumColumns(GridView.AUTO_FIT);
		mGridView.setGravity(Gravity.CENTER);
		mGridView.setColumnWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 125 + 30, getActivity().getResources().getDisplayMetrics()));
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

		if (mThemes.isEmpty()) setViewShown(false);

		// Load from server (and update ui when finished)
		mTask = new ThemesStoreTask(getActivity()) {
			@Override
			protected void onPostExecute(List<App> result) {
				super.onPostExecute(result);
				if (result == null) return;
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
				} catch(IllegalStateException e){
					e.printStackTrace();
				}
			}
		};
		mTask.executeAsync();

	}

	public void onListItemClick(GridView g, View v, int position, long id) {
		if (App.doesPackageExists(getContext(),  mThemes.get(position).getPackageName())) {
			String appName = mThemes.get(position).getPackageName();

			// Update theme
			CalculatorSettings.setTheme(getContext(), appName);
			Theme.setPackageName(appName);

			// Create a new intent to relaunch the settings
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
			getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
		}
		else {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("market://details?id=" + mThemes.get(position).getPackageName()));
			startActivity(intent);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		onListItemClick(mGridView, mGridView.getChildAt(position), position, mGridView.getChildAt(position).getId());
	}

	@Override
	public void onStart() {
		super.onStart();

		// Restore the scroll position, if any
		final Bundle args = getArguments();
		if (args != null) {
			mGridView.setSelection(args.getInt(EXTRA_LIST_POSITION, 0));
			mGridView.scrollBy(0, -1*args.getInt(EXTRA_LIST_VIEW_OFFSET, 0));
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mTask.cancel(true);
		mDataSource.close();
	}
}
