package com.android2.calculator3;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Will on 4/9/2014.
 */
public class FloatingCalculatorActivity extends Activity {
	public static FloatingCalculatorActivity ACTIVE_ACTIVITY;

	public void onCreate(Bundle state) {
		super.onCreate(state);

		View v = new View(this);
		v.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				finish();
				return false;
			}
		});
		setContentView(v);
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
