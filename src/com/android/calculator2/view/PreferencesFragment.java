package com.android.calculator2.view;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.view.View;
import android.widget.ListView;

import com.android.calculator2.CalculatorSettings;
import com.android.calculator2.Preferences;
import com.android.calculator2.R;

public class PreferencesFragment extends PreferenceFragment {

    private static final String EXTRA_LIST_POSITION = "list_position";
    private static final String EXTRA_LIST_VIEW_OFFSET = "list_view_top";

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
                                new ComponentName("com.android.calculator2", "com.android.calculator2.Calculator-Light"), lightState,
                                PackageManager.DONT_KILL_APP);
                        getActivity().getPackageManager().setComponentEnabledSetting(
                                new ComponentName("com.android.calculator2", "com.android.calculator2.Calculator-Dark"), darkState,
                                PackageManager.DONT_KILL_APP);

                        // Create a new intent to relaunch the settings
                        Intent intent = new Intent(getActivity(), Preferences.class);

                        // Preserve the list offsets
                        int itemPosition = getListView().getFirstVisiblePosition();
                        View child = getListView().getChildAt(0);
                        int itemOffset = child != null ? child.getTop() : 0;

                        intent.putExtra(EXTRA_LIST_POSITION, itemPosition);
                        intent.putExtra(EXTRA_LIST_VIEW_OFFSET, itemOffset);

                        // Go
                        startActivity(intent);
                        getActivity().finish();

                        // Set a smooth fade transition
                        getActivity().overridePendingTransition(android.R.anim.fade_in,
                                android.R.anim.fade_out);
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

    @Override
    public void onStart() {
        super.onStart();

        // Restore the scroll position, if any
        final Bundle args = getArguments();
        if (args != null) {
            getListView().setSelectionFromTop(
                    args.getInt(EXTRA_LIST_POSITION, 0),
                    args.getInt(EXTRA_LIST_VIEW_OFFSET, 0)
            );
        }
    }

    public ListView getListView() {
        return (ListView) getView().findViewById(android.R.id.list);
    }

}
