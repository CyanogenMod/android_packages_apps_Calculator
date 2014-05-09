package com.android2.calculator3;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.xlythe.engine.theme.Theme;

/**
 * Created by Will on 4/9/2014.
 */
public class StoreInfoActivity extends Activity {
	public void onCreate(Bundle state) {
		super.onCreate(state);

		// Update theme (as needed)
		Theme.buildResourceMap(com.android2.calculator3.R.class);
		Theme.setPackageName(CalculatorSettings.getTheme(this));
		int customTheme = Theme.getSettingsTheme(this);
		if (customTheme != 0) {
			super.setTheme(customTheme);
		}

		setContentView(R.layout.activity_store_info);

		ActionBar mActionBar = getActionBar();
		if (mActionBar != null) {
			mActionBar.setDisplayHomeAsUpEnabled(true);
		}

		TextView text = (TextView) findViewById(R.id.text);
		text.setText(R.string.store_info_text);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			startActivity(new Intent(this, Calculator.class));
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
