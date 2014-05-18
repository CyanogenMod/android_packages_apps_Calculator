package com.android2.calculator3;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import com.android2.calculator3.view.PreferencesFragment;
import com.xlythe.engine.theme.Theme;

/**
 * @author Will Harmon
 */
public class Preferences extends BaseActivity {
    Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int customTheme = Theme.getSettingsTheme(this);
        if(customTheme != 0) {
            super.setTheme(customTheme);
        }

        setContentView(R.layout.activity_preferences);

        if(savedInstanceState == null) {
            mFragment = new PreferencesFragment();
            mFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(R.id.content_view, mFragment).commit();
        }

        ActionBar mActionBar = getActionBar();
        if(mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.activity_open_enter, R.anim.activity_close_exit);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(getFragmentManager().findFragmentById(R.id.content_view) != mFragment) {
                try {
                    getFragmentManager().popBackStack();
                    return true;
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            startActivity(new Intent(this, Calculator.class));
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }
}
