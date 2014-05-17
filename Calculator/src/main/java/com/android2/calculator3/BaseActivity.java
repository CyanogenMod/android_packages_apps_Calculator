package com.android2.calculator3;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.widget.TextView;

import com.xlythe.engine.theme.Theme;

import java.util.List;

/**
 * Created by Will on 4/9/2014.
 */
public class BaseActivity extends FragmentActivity {
	private boolean mIsSwitchingActivities = false;

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);

		// Update theme (as needed)
		Theme.buildResourceMap(R.class);
		Theme.setPackageName(CalculatorSettings.getTheme(this));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			Intent i = getPackageManager().getLaunchIntentForPackage(getPackageName());
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(i);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResume() {
		super.onResume();
		// Kill floating calc (if exists)
		Intent serviceIntent = new Intent(this, FloatingCalculator.class);
		stopService(serviceIntent);
	}

	@Override
	public void onPause() {
		super.onPause();
		Intent serviceIntent = new Intent(this, FloatingCalculator.class);
		if (CalculatorSettings.floatingCalculator(this)) {
			// Start Floating Calc service if not up yet
			if(!mIsSwitchingActivities) {
			    startService(serviceIntent);
			}
		}
		mIsSwitchingActivities = false;
	}

	@Override
	public void startActivity(Intent intent) {
		super.startActivity(intent);
		mIsSwitchingActivities = intent.getComponent() != null && getPackageName().equals(intent.getComponent().getPackageName());
	}
}
