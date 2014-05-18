package com.android2.calculator3;

import android.app.ActionBar;
import android.os.Bundle;
import android.widget.TextView;

import com.xlythe.engine.theme.Theme;

/**
 * Created by Will on 4/9/2014.
 */
public class StoreInfoActivity extends BaseActivity {
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Update theme (as needed)
        int customTheme = Theme.getSettingsTheme(this);
        if(customTheme != 0) {
            super.setTheme(customTheme);
        }

        setContentView(R.layout.activity_store_info);

        ActionBar mActionBar = getActionBar();
        if(mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }

        TextView text = (TextView) findViewById(R.id.text);
        text.setText(R.string.store_info_text);
    }
}
