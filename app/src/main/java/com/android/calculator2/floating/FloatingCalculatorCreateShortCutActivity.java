package com.android.calculator2.floating;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.android.calculator2.R;

/**
 * Created by Will on 4/9/2014.
 */
public class FloatingCalculatorCreateShortCutActivity extends Activity {
    public void onCreate(Bundle state) {
        super.onCreate(state);

        if(Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            // create shortcut if requested
            Intent.ShortcutIconResource icon = Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher_floating);

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
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.blank, R.anim.blank);
    }
}
