package com.android2.calculator3;

import android.app.Activity;
import android.os.Bundle;
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
        if(customTheme != 0) {
            super.setTheme(customTheme);
        }

        setContentView(R.layout.activity_store_info);

        TextView text = (TextView) findViewById(R.id.text);
        text.setText(R.string.store_info_text);
    }
}
