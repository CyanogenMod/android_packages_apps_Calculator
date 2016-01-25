package com.android.calculator2.util;

import android.content.Context;
import android.content.res.Resources;
import com.android.calculator2.R;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class DigitLabelHelper {

    private static final String UNICODE_LOCALE_KEY = "nu";
    private static final String UNICODE_LOCALE_VALUE = "latn";
    private static DigitLabelHelper sInstance;
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
            R.id.dec_point
    };

    private int mCachedLocaleHash;
    private DecimalFormatSymbols mDecFormatSymbols;

    public interface DigitLabelHelperCallback {
        void setDigitText(int id, String text);
    }

    public synchronized static DigitLabelHelper getInstance() {
        if (sInstance == null) {
            sInstance = new DigitLabelHelper();
        }
        return sInstance;
    }

    private DecimalFormatSymbols getDecimalFormatForCurrentLocale(Context context) {
        Resources resources = context.getResources();
        Locale locale = resources.getConfiguration().locale;
        if (locale.hashCode() != mCachedLocaleHash) {
            if (!resources.getBoolean(R.bool.use_localized_digits)) {
                locale = new Locale.Builder()
                        .setLocale(locale)
                        .setUnicodeLocaleKeyword(UNICODE_LOCALE_KEY, UNICODE_LOCALE_VALUE)
                        .build();
            }
            mCachedLocaleHash = locale.hashCode();
            mDecFormatSymbols = DecimalFormatSymbols.getInstance(locale);
        }
        return mDecFormatSymbols;
    }

    public void getTextForDigits(Context context, DigitLabelHelperCallback callback) {
        final DecimalFormatSymbols symbols = getDecimalFormatForCurrentLocale(context);
        final char zeroDigit = symbols.getZeroDigit();
        for (int i = 0; i < sDigitIds.length; i++) {
            int id = sDigitIds[i];
            if (id == R.id.dec_point) {
                callback.setDigitText(id, String.valueOf(symbols.getDecimalSeparator()));
            } else {
                callback.setDigitText(id, String.valueOf((char) (zeroDigit + i)));
            }
        }
    }
}
