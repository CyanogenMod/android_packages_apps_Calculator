package com.android.calculator2;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;

import com.xlythe.engine.theme.Theme;
import com.xlythe.engine.theme.Theme.Res;

public class StoreActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Update theme (as needed)
        Theme.buildResourceMap(com.android.calculator2.R.class);
        Theme.setPackageName(CalculatorSettings.getTheme(getContext()));
        int customTheme = Theme.getSettingsTheme(this);
        if(customTheme != 0) {
            super.setTheme(customTheme);
        }

        setContentView(R.layout.activity_store);

        SectionsPagerAdapter pagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(pagerAdapter);

        ActionBar mActionBar = getActionBar();
        if(mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            startActivity(new Intent(this, Calculator.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private int getDimensionPixelSize(int res) {
        return getContext().getResources().getDimensionPixelSize(res);
    }

    private int getTabHeight() {
        return getDimensionPixelSize(R.dimen.store_tab_height);
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private void setBackground(View v, Res res) {
        if(res != null) {
            if(Theme.COLOR.equals(res.getType())) {
                v.setBackgroundColor(Theme.getColor(getContext(), res.getName()));
            }
            else if(Theme.DRAWABLE.equals(res.getType())) {
                if(android.os.Build.VERSION.SDK_INT < 16) {
                    v.setBackgroundDrawable(Theme.getDrawable(getContext(), res.getName()));
                }
                else {
                    v.setBackground(Theme.getDrawable(getContext(), res.getName()));
                }
            }
        }
    }

    private Context getContext() {
        return this;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the
     * sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
            case 0:
                return new ThemesFragment();
            default:
                return null;
            }
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch(position) {
            case 0:
                return getString(R.string.store_tab_themes).toUpperCase(l);
            }
            return null;
        }
    }
}
