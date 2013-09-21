package com.android2.calculator3;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import com.android2.calculator3.view.PreferencesFragment;

/**
 * @author Will Harmon
 **/
public class Preferences extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(CalculatorSettings.useLightTheme(this)) {
            super.setTheme(R.style.Theme_Calculator_Settings_Light);
        }

        if(savedInstanceState == null) {
            getFragmentManager().beginTransaction().add(android.R.id.content, new PreferencesFragment()).commit();
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
