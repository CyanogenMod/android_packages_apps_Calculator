package com.android2.calculator3;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Will on 4/9/2014.
 */
public class FloatingCalculatorActivity extends Activity {
	public static final String EXTRA_HIDE_STATUS_BAR = "hide_status_bar";
	public static FloatingCalculatorActivity ACTIVE_ACTIVITY;

	public void onCreate(Bundle state) {
		super.onCreate(state);
		if(getIntent().getBooleanExtra(EXTRA_HIDE_STATUS_BAR, false)) {
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		ACTIVE_ACTIVITY = this;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (FloatingCalculator.ACTIVE_CALCULATOR != null)
			FloatingCalculator.ACTIVE_CALCULATOR.closeCalculator();
		ACTIVE_ACTIVITY = null;
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.blank, R.anim.blank);
	}
}
