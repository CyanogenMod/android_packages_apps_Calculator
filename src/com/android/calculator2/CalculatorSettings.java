package com.android.calculator2;

import android.content.Context;
import android.preference.PreferenceManager;

import com.android.calculator2.Calculator.Panel;

public class CalculatorSettings {
    static boolean graphPanel(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Panel.GRAPH.toString(), context.getResources().getBoolean(R.bool.GRAPH));
    }

    static boolean hexPanel(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Panel.HEX.toString(), context.getResources().getBoolean(R.bool.HEX));
    }

    static boolean functionPanel(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Panel.FUNCTION.toString(), context.getResources().getBoolean(R.bool.FUNCTION));
    }

    static boolean basicPanel(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Panel.BASIC.toString(), context.getResources().getBoolean(R.bool.BASIC));
    }

    static boolean advancedPanel(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Panel.ADVANCED.toString(), context.getResources().getBoolean(R.bool.ADVANCED));
    }

    static boolean matrixPanel(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Panel.MATRIX.toString(), context.getResources().getBoolean(R.bool.MATRIX));
    }

    static boolean useRadians(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("USE_RADIANS", context.getResources().getBoolean(R.bool.USE_RADIANS));
    }

    static boolean returnToBasic(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("RETURN_TO_BASIC", context.getResources().getBoolean(R.bool.RETURN_TO_BASIC));
    }

    static boolean isDismissed(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, false);
    }

    static void saveKey(Context context, String key, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).commit();
    }
}
