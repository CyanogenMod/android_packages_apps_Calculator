package com.android2.calculator3.view;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.android2.calculator3.R;

public class PreferencesFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences_body);
    }
}
