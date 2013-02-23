package com.android.calculator2.view;

import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.android.calculator2.R;

public class PreferencesFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences_body);
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
