package com.xlythe.floatingview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.android.calculator2.R;

/**
 * Created by Will on 4/9/2014.
 */
public class FloatingActivity extends Activity {
    public static final String EXTRA_HIDE_STATUS_BAR = "hide_status_bar";
    public static FloatingActivity ACTIVE_ACTIVITY;

    @SuppressLint("InlinedApi")
    public void onCreate(Bundle state) {
        super.onCreate(state);
        if(getIntent().getBooleanExtra(EXTRA_HIDE_STATUS_BAR, false)) {
            if(android.os.Build.VERSION.SDK_INT < 16) {
                requestWindowFeature(Window.FEATURE_NO_TITLE);
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
            else {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
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
        if(FloatingView.ACTIVE_VIEW != null) FloatingView.ACTIVE_VIEW.closeView();
        ACTIVE_ACTIVITY = null;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.blank, R.anim.blank);
    }
}
