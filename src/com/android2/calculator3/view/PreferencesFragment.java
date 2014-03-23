package com.android2.calculator3.view;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.ListView;

import com.android2.calculator3.Preferences;
import com.android2.calculator3.R;
import com.xlythe.engine.theme.Theme;
import com.xlythe.engine.theme.ThemeListPreference;

public class PreferencesFragment extends PreferenceFragment {
    private static final String EXTRA_LIST_POSITION = "list_position";
    private static final String EXTRA_LIST_VIEW_OFFSET = "list_view_top";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);

        // Update theme (as needed)
        boolean useLightTheme = Theme.isLightTheme(getContext());

        Preference panels = findPreference("panels");
        if(panels != null) {
            panels.setIcon(useLightTheme ? R.drawable.settings_panels_icon_grey : R.drawable.settings_panels_icon_white);
            panels.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    PageOrderFragment fragment = new PageOrderFragment();
                    getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).addToBackStack(null).commit();
                    return true;
                }
            });
        }

        Preference actions = findPreference("actions");
        if(actions != null) {
            actions.setIcon(useLightTheme ? R.drawable.settings_actions_icon_grey : R.drawable.settings_actions_icon_white);
            actions.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ActionsPreferencesFragment fragment = new ActionsPreferencesFragment();
                    getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).addToBackStack(null).commit();
                    return true;
                }
            });
        }

        Preference units = findPreference("units");
        if(units != null) {
            units.setIcon(useLightTheme ? R.drawable.settings_units_icon_grey : R.drawable.settings_units_icon_white);
            units.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    UnitsPreferencesFragment fragment = new UnitsPreferencesFragment();
                    getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).addToBackStack(null).commit();
                    return true;
                }
            });
        }

        ThemeListPreference theme = (ThemeListPreference) findPreference("SELECTED_THEME");
        if(theme != null) {
            theme.setIcon(useLightTheme ? R.drawable.settings_theme_icon_grey : R.drawable.settings_theme_icon_white);
            theme.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // Update theme
                    Theme.setPackageName(newValue.toString());

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
                    getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    return true;
                }
            });
        }

        Preference about = findPreference("ABOUT");
        if(about != null) {
            about.setIcon(useLightTheme ? R.drawable.settings_about_icon_grey : R.drawable.settings_about_icon_white);
            String versionName = "";
            try {
                versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
            }
            catch(NameNotFoundException e) {
                e.printStackTrace();
            }
            about.setTitle(about.getTitle() + " v" + versionName);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Restore the scroll position, if any
        final Bundle args = getArguments();
        if(args != null) {
            getListView().setSelectionFromTop(args.getInt(EXTRA_LIST_POSITION, 0), args.getInt(EXTRA_LIST_VIEW_OFFSET, 0));
        }
    }

    public ListView getListView() {
        return (ListView) getView().findViewById(android.R.id.list);
    }

    protected Context getContext() {
        return getActivity();
    }

    public static class ActionsPreferencesFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.layout.preferences_actions);
        }
    }

    public static class UnitsPreferencesFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.layout.preferences_units);
        }
    }
}
