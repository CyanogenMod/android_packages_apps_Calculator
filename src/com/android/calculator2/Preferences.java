package com.android.calculator2;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;

import com.android.calculator2.view.PreferencesFragment;
import com.xlythe.engine.theme.Theme;

/**
 * @author Will Harmon
 */
public class Preferences extends Activity {
    Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int customTheme = Theme.getSettingsTheme(this);
        if (customTheme != 0) {
            super.setTheme(customTheme);
        }

        if (savedInstanceState == null) {
            mFragment = new PreferencesFragment();
            mFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(android.R.id.content, mFragment).commit();
        }

        ActionBar mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (getFragmentManager().findFragmentById(android.R.id.content) != mFragment) {
                try {
                    getFragmentManager().popBackStack();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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
