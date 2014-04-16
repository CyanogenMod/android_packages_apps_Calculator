/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xlythe.engine.theme;

import java.util.List;

import android.content.Context;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import com.android.calculator2.R;

public class ThemeListPreference extends ListPreference {
    public ThemeListPreference(Context context) {
        super(context);
        setup();
    }

    public ThemeListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    private void setup() {
        // Grab all installed themes
        final List<App> themes = Theme.getApps(getContext());
        CharSequence[] themeEntry = new CharSequence[themes.size() + 1];
        CharSequence[] themeValue = new CharSequence[themes.size() + 1];

        // Set a default
        themeEntry[0] = getContext().getString(R.string.preferences_option_default);
        themeValue[0] = getContext().getPackageName();

        // Add the rest to the preference
        for (int i = 1; i < themeEntry.length; i++) {
            themeEntry[i] = themes.get(i - 1).getName();
            themeValue[i] = themes.get(i - 1).getPackageName();
        }

        // Set the values
        setEntries(themeEntry);
        setEntryValues(themeValue);
        setDefaultValue(null);

        // Update the UI to display the selected theme name
        setSummary(getThemeTitle(themes,
                PreferenceManager.getDefaultSharedPreferences(getContext()).getString(getKey(),
                getContext().getPackageName())));
    }

    private String getThemeTitle(List<App> themes, Object packageName) {
        for (App a : themes) {
            if (a.getPackageName().equals(packageName)) {
                return a.getName();
            }
        }

        return getContext().getString(R.string.preferences_option_default);
    }
}
