package com.android2.calculator3;

import android.content.Context;
import android.preference.PreferenceManager;

import com.android2.calculator3.Page.Panel;

public class CalculatorSettings {
    public static boolean isPageEnabled(Context context, Page page) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(page.getKey(), page.getDefaultValue());
    }

    public static boolean isPageEnabled(Context context, Panel panel) {
        Page page = new Page(context, panel);
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(page.getKey(), page.getDefaultValue());
    }

    public static void setPageEnabled(Context context, Page page, boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(page.getKey(), enabled).commit();
    }

    public static void setPageOrder(Context context, Page page, int order) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(page.getKey() + "_order", order).commit();
    }

    public static int getPageOrder(Context context, Page page) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(page.getKey() + "_order", Integer.MAX_VALUE);
    }

    static boolean useRadians(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("USE_RADIANS", context.getResources().getBoolean(R.bool.USE_RADIANS));
    }

    static boolean returnToBasic(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("RETURN_TO_BASIC", context.getResources().getBoolean(R.bool.RETURN_TO_BASIC));
    }

    public static boolean useInfiniteScrolling(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("INFINITE_SCROLLING", context.getResources().getBoolean(R.bool.RETURN_TO_BASIC));
    }

    static boolean clickToOpenHistory(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("CLICK_TO_OPEN_HISTORY", context.getResources().getBoolean(R.bool.CLICK_TO_OPEN_HISTORY));
    }

    public static boolean digitGrouping(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("DIGIT_GROUPING", context.getResources().getBoolean(R.bool.DIGIT_GROUPING));
    }

    public static boolean showDetails(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("SHOW_DETAILS", context.getResources().getBoolean(R.bool.SHOW_DETAILS));
    }

    static boolean isDismissed(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, false);
    }

    public static String getTheme(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("SELECTED_THEME", context.getPackageName());
    }

    static void saveKey(Context context, String key, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).commit();
    }

}
