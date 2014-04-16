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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.xlythe.engine.theme.Theme;

public class StoreInfoActivity extends Activity {
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Update theme (as needed)
        Theme.buildResourceMap(com.android.calculator2.R.class);
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
