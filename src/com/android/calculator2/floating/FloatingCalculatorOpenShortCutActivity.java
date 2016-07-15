package com.android.calculator2.floating;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.os.Handler;
import android.provider.Settings;
import com.android.calculator2.R;

/**
 * Created by Will on 4/9/2014.
 */
public class FloatingCalculatorOpenShortCutActivity extends Activity {

    private static int REQUEST_SYSTEM_WINDOW_PERMISSION = 69;
    private Handler mHandler = new Handler();

    public void onCreate(Bundle state) {
        super.onCreate(state);

        if (!Settings.canDrawOverlays(this)) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    requestSystemWindowPermission();
                }
            });
        } else {
            startFloatingCalculator();
        }
    }

    private void startFloatingCalculator() {
        Intent intent = new Intent(this, FloatingCalculator.class);
        startService(intent);
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.blank, R.anim.blank);
    }

    private void requestSystemWindowPermission() {
        Intent intent = new Intent (Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_SYSTEM_WINDOW_PERMISSION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SYSTEM_WINDOW_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingCalculator();
            } else {
                finish();
            }
        }
    }
}
