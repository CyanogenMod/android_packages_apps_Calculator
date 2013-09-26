package com.android.calculator2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import com.android.calculator2.view.PreferencesFragment;

/**
 * @author Will Harmon
 **/
public class Preferences extends Activity {

    public static final String EXTRA_LIST_POSITION = "list_position";
    public static final String EXTRA_LIST_VIEW_OFFSET = "list_view_top";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(CalculatorSettings.useLightTheme(this)) {
            super.setTheme(R.style.Theme_Settings_Calculator_Light);
        }

        if(savedInstanceState == null) {
            PreferencesFragment fragment = new PreferencesFragment();
            fragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            startActivity(new Intent(this, Calculator.class));
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.activity_open_enter, R.anim.activity_close_exit);
    }
}
