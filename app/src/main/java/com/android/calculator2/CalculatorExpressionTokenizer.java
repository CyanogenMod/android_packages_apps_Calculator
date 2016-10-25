/*
* Copyright (C) 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the 'License');
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an 'AS IS' BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.android.calculator2;

import android.content.Context;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.xlythe.math.Constants;

import java.util.LinkedList;
import java.util.List;

public class CalculatorExpressionTokenizer {
    private final List<Localizer> mReplacements;
    private boolean mUseDegrees = false;

    public CalculatorExpressionTokenizer(Context context) {
        mReplacements = new LinkedList<Localizer>();

        Locale locale = context.getResources().getConfiguration().locale;
        if (!context.getResources().getBoolean(R.bool.use_localized_digits)) {
            locale = new Locale.Builder()
                .setLocale(locale)
                .setUnicodeLocaleKeyword("nu", "latn")
                .build();
        }

        final DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
        final char zeroDigit = symbols.getZeroDigit();

        for (int i = 0; i <= 9; ++i) {
            mReplacements.add(new Localizer(
                        Integer.toString(i), String.valueOf((char) (i + zeroDigit))));
        }

        mReplacements.add(new Localizer(",", String.valueOf(Constants.MATRIX_SEPARATOR)));
        mReplacements.add(new Localizer(".", String.valueOf(symbols.getDecimalSeparator())));
        mReplacements.add(new Localizer("/", context.getString(R.string.op_div)));
        mReplacements.add(new Localizer("*", context.getString(R.string.op_mul)));
        mReplacements.add(new Localizer("-", context.getString(R.string.op_sub)));
        mReplacements.add(new Localizer("asin", context.getString(R.string.arcsin)));
        mReplacements.add(new Localizer("acos", context.getString(R.string.arccos)));
        mReplacements.add(new Localizer("atan", context.getString(R.string.arctan)));
        mReplacements.add(new Localizer("sin", context.getString(R.string.fun_sin)));
        mReplacements.add(new Localizer("cos", context.getString(R.string.fun_cos)));
        mReplacements.add(new Localizer("tan", context.getString(R.string.fun_tan)));
        if(mUseDegrees) {
            mReplacements.add(new Localizer("sin", "sind"));
            mReplacements.add(new Localizer("cos", "cosd"));
            mReplacements.add(new Localizer("tan", "tand"));
        }
        mReplacements.add(new Localizer("ln", context.getString(R.string.fun_ln)));
        mReplacements.add(new Localizer("log", context.getString(R.string.fun_log)));
        mReplacements.add(new Localizer("det", context.getString(R.string.det)));
        mReplacements.add(new Localizer("Infinity", context.getString(R.string.inf)));
    }

    public String getNormalizedExpression(String expr) {
        for (Localizer replacement : mReplacements) {
            expr = expr.replace(replacement.local, replacement.english);
        }
        return expr;
    }

    public String getLocalizedExpression(String expr) {
        for (Localizer replacement : mReplacements) {
            expr = expr.replace(replacement.english, replacement.local);
        }
        return expr;
    }

    private class Localizer {
        String english;
        String local;

        Localizer(String english, String local) {
            this.english = english;
            this.local = local;
        }
    }
}
