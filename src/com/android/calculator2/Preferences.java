package com.android.calculator2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

/**
 * @author Will Harmon
 **/
public class Preferences extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferences);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent(this, Calculator.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }
}
