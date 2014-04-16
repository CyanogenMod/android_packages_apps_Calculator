/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class FloatingCalculatorCreateShortCutActivity extends Activity {
    public static FloatingCalculatorCreateShortCutActivity ACTIVE_ACTIVITY;

    public void onCreate(Bundle state) {
        super.onCreate(state);

        if (Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            // Create shortcut if requested
            Intent.ShortcutIconResource icon = Intent.ShortcutIconResource.fromContext(
                    this, R.drawable.ic_launcher_floating);

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
        if (FloatingCalculator.ACTIVE_CALCULATOR != null) {
            FloatingCalculator.ACTIVE_CALCULATOR.closeCalculator();
        }

        ACTIVE_ACTIVITY = null;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.blank, R.anim.blank);
    }
}
