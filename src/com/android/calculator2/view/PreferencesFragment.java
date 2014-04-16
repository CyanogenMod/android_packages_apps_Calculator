package com.android.calculator2.view;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListView;

import com.android.calculator2.CalculatorWidget;
import com.android.calculator2.FloatingCalculator;
import com.android.calculator2.Preferences;
import com.android.calculator2.R;
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
            panels.setIcon(useLightTheme ? R.drawable.settings_panels_icon_grey
                    : R.drawable.settings_panels_icon_white);
            panels.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    PageOrderFragment fragment = new PageOrderFragment();
                    getFragmentManager().beginTransaction().replace(R.id.content_view, fragment)
                            .addToBackStack(null).commit();
                    return true;
                }
            });
        }

        Preference actions = findPreference("actions");
        if(actions != null) {
            actions.setIcon(useLightTheme ? R.drawable.settings_actions_icon_grey
                    : R.drawable.settings_actions_icon_white);
            actions.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ActionsPreferencesFragment fragment = new ActionsPreferencesFragment();
                    getFragmentManager().beginTransaction().replace(R.id.content_view, fragment)
                            .addToBackStack(null).commit();
                    return true;
                }
            });
        }

        Preference units = findPreference("units");
        if(units != null) {
            units.setIcon(useLightTheme ? R.drawable.settings_units_icon_grey
                    : R.drawable.settings_units_icon_white);
            units.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    UnitsPreferencesFragment fragment = new UnitsPreferencesFragment();
                    getFragmentManager().beginTransaction().replace(R.id.content_view, fragment)
                            .addToBackStack(null).commit();
                    return true;
                }
            });
        }

        ThemeListPreference theme = (ThemeListPreference) findPreference("SELECTED_THEME");
        if(theme != null) {
            theme.setIcon(useLightTheme ? R.drawable.settings_theme_icon_grey
                    : R.drawable.settings_theme_icon_white);
            theme.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String appName = newValue.toString();

                    // Update theme
                    Theme.setPackageName(appName);

                    // Swap app icons - Removed because of poor support
                    // Intent appIconIntent = new Intent();
                    // appIconIntent.setAction(getActivity().getPackageName() + ".APP_ICON");
                    // appIconIntent.putExtra("app_name", appName);
                    // getActivity().sendBroadcast(appIconIntent);
                    // boolean hasAppIcon = Theme.getString(getActivity(), "calc") != null;
                    // String packageName = getActivity().getPackageName();
                    // int state = !hasAppIcon || packageName.equals(appName) ?
                    // PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    // PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                    // getActivity().getPackageManager().setComponentEnabledSetting(new
                    // ComponentName(packageName, packageName + ".Calculator"), state,
                    // PackageManager.DONT_KILL_APP);

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
                    return true;
                }
            });
        }

        Preference about = findPreference("ABOUT");
        if(about != null) {
            about.setIcon(useLightTheme ? R.drawable.settings_about_icon_grey
                    : R.drawable.settings_about_icon_white);
            String versionName = "";
            try {
                versionName = getActivity().getPackageManager().getPackageInfo(
                        getActivity().getPackageName(), 0).versionName;
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
            getListView().setSelectionFromTop(args.getInt(EXTRA_LIST_POSITION, 0),
                    args.getInt(EXTRA_LIST_VIEW_OFFSET, 0));
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

            Preference floatingCalc = findPreference("FLOATING_CALCULATOR");
            if(floatingCalc != null) {
                floatingCalc.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        Intent startServiceIntent = new Intent(getActivity(),
                                FloatingCalculator.class);
                        if((Boolean) newValue) {
                            // Start Floating Calc service if not up yet
                            getActivity().startService(startServiceIntent);
                        }
                        else {
                            // Stop Floating Calc service if up
                            getActivity().stopService(startServiceIntent);
                        }
                        return true;
                    }
                });
            }

            Preference widgetBg = findPreference("SHOW_WIDGET_BACKGROUND");
            if(widgetBg != null) {
                widgetBg.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                                .putBoolean("SHOW_WIDGET_BACKGROUND", (Boolean) newValue).commit();
                        final Intent intent = new Intent(getActivity(), CalculatorWidget.class);
                        intent.setAction("refresh");
                        getActivity().sendBroadcast(intent);
                        return true;
                    }
                });
            }

            Preference vibrateOnPress = findPreference("VIBRATE_ON_PRESS");
            if(vibrateOnPress != null) {
                Vibrator vi = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                if(!vi.hasVibrator()) removePreference(vibrateOnPress);
            }
        }

        protected void removePreference(Preference preference) {
            getPreferenceScreen().removePreference(preference);
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
