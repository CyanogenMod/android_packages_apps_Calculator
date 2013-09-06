package com.android2.calculator3.view;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import com.android2.calculator3.CalculatorSettings;
import com.android2.calculator3.Preferences;
import com.android2.calculator3.R;

public class PreferencesFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);

        SwitchPreference holo = (SwitchPreference) findPreference("THEME_STYLE");
        if(holo != null) {
            holo.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if((Boolean) newValue != CalculatorSettings.useLightTheme(getActivity())) {
                        Intent intent = new Intent(getActivity(), Preferences.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                    return true;
                }
            });
        }

        Preference about = findPreference("ABOUT");
        if(about != null) {
            String versionName = "";
            try {
                versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
            }
            catch(NameNotFoundException e) {
                e.printStackTrace();
            }
            about.setTitle(about.getTitle() + " " + versionName);
        }
    }
}
