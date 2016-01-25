package com.android.calculator2.util;

import android.content.Context;
import android.content.res.Resources;
import com.android.calculator2.R;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class CalculatorDigitHelper {

    private static final int[] sDigitIds = new int[] {
            R.id.digit0,
            R.id.digit1,
            R.id.digit2,
            R.id.digit3,
            R.id.digit4,
            R.id.digit5,
            R.id.digit6,
            R.id.digit7,
            R.id.digit8,
            R.id.digit9,
            R.id.dot
    };

    public interface DigitHelperCallback {
        void setDigitText(int id, String text);
    }

    private static DecimalFormatSymbols getDecimalFormatForCurrentLocale(Context context) {
        Resources resources = context.getResources();
        Locale locale = context.getResources().getConfiguration().locale;
        if (!resources.getBoolean(R.bool.use_localized_digits)) {
            locale = new Locale.Builder()
                    .setLocale(locale)
                    .setUnicodeLocaleKeyword("nu", "latn")
                    .build();
        }
        return DecimalFormatSymbols.getInstance(locale);
    }

    public static void getTextForDigits(Context context, DigitHelperCallback callback) {
        final DecimalFormatSymbols symbols = getDecimalFormatForCurrentLocale(context);
        final char zeroDigit = symbols.getZeroDigit();
        for (int i = 0; i < sDigitIds.length; i++) {
            int id = sDigitIds[i];
            if (id == R.id.dot) {
                callback.setDigitText(id, String.valueOf(symbols.getDecimalSeparator()));
            } else {
                callback.setDigitText(id, String.valueOf((char) (zeroDigit + i)));
            }
        }
    }
}
