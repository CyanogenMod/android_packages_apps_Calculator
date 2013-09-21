package com.android2.calculator3.view;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
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
                        // Update app icon
                        int lightState = (Boolean) newValue ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                        int darkState = (Boolean) newValue ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                        getActivity().getPackageManager().setComponentEnabledSetting(
                                new ComponentName("com.android2.calculator3", "com.android2.calculator3.Calculator-Light"), lightState,
                                PackageManager.DONT_KILL_APP);
                        getActivity().getPackageManager().setComponentEnabledSetting(
                                new ComponentName("com.android2.calculator3", "com.android2.calculator3.Calculator-Dark"), darkState,
                                PackageManager.DONT_KILL_APP);

                        // Relaunch settings
                        startActivity(new Intent(getActivity(), Preferences.class));
                        getActivity().finish();
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
