package com.android2.calculator3;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by Will on 4/9/2014.
 */
public class FloatingCalculatorCreateShortCutActivity extends Activity {
    public static FloatingCalculatorCreateShortCutActivity ACTIVE_ACTIVITY;

    public void onCreate(Bundle state) {
        super.onCreate(state);

        if(Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            // create shortcut if requested
            Intent.ShortcutIconResource icon =
                    Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher_floating);

            Intent intent = new Intent();
            Intent launchIntent = new Intent(this, FloatingCalculatorOpenShortCutActivity.class);

            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);

            setResult(RESULT_OK, intent);
            finish();
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
