package com.xlythe.math;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Translates an equation typed in the default locale back into English
 *
 * This assumes the app has English translations
 */
public class Localizer {
    private final Map<String, String> mMap = new HashMap<String, String>();
    private boolean mUseDegrees = false;

    public Localizer(Context context, Class r) {
        buildResourceMap(context, r);
    }

    @SuppressWarnings("rawtypes")
    public void buildResourceMap(Context context, Class r) {
        try {
            Log.d("Localizer", "Building resource map");
            Class color = Class.forName(r.getName() + "$string");
            for (Field f : color.getFields()) {
                if(detect(context, f, "asin"));
                else if(detect(context, f, "acos"));
                else if(detect(context, f, "atan"));
                else if(detect(context, f, "sin"));
                else if(detect(context, f, "cos"));
                else if(detect(context, f, "tan"));
                else if(detect(context, f, "log"));
                else if(detect(context, f, "ln"));
                else if(detect(context, f, "det"));
                else if(detect(context, f, "cbrt"));
                else if(f.getName().toLowerCase().contains("dot") || f.getName().toLowerCase().contains("decimal")) {
                    mMap.put(".", context.getString(f.getInt(null)));
                }
                else if(f.getName().toLowerCase().contains("matrix") && f.getName().toLowerCase().contains("separator")) {
                    mMap.put(",", context.getString(f.getInt(null)));
                }
            }
            Log.d("Localizer", "strings loaded");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // Do nothing. Happens if no Strings are found.
        }
    }

    private boolean detect(Context context, Field f, String phrase) throws IllegalAccessException {
        if(f.getName().toLowerCase().contains(phrase)) {
            mMap.put(phrase, context.getString(f.getInt(null)));
            return true;
        }
        return false;
    }

    /**
     * Localize the input into English
     *
     * Used because the math library only understands English.
     * */
    public String localize(String input) {
        // Delocalize functions (e.g. Spanish localizes "sin" as "sen").
        // Order matters for arc functions
        input = translate(input, "asin");
        input = translate(input, "acos");
        input = translate(input, "atan");
        input = translate(input, "sin");
        input = translate(input, "cos");
        input = translate(input, "tan");
        if(mUseDegrees) {
            input = input.replace("sin", "sind");
            input = input.replace("cos", "cosd");
            input = input.replace("tan", "tand");
        }
        input = translate(input, "log");
        input = translate(input, "ln");
        input = translate(input, "det");
        input = translate(input, "sqrt");
        input = translate(input, "cbrt");
        input = translate(input, ".");
        input = translate(input, ",");
        return input;
    }

    /**
     * Localize the input to the user's original locale
     *
     * We only care about comas and periods because, by now, the math problem should be solved.
     * */
    String relocalize(String input) {
        input = retranslate(input, ",");
        input = retranslate(input, ".");
        return input;
    }

    /**
     * Checks if a word has a translation.
     * If so, replaces the sentence with the English word.
     * */
    private String translate(String sentence, String word) {
        if(mMap.get(word) != null) {
            return sentence.replace(mMap.get(word), word);
        }
        return sentence;
    }

    /**
     * Checks if a word has a translation.
     * If so, replaces the sentence with the localized word.
     * */
    private String retranslate(String sentence, String word) {
        if(mMap.get(word) != null) {
            return sentence.replace(word, mMap.get(word));
        }
        return sentence;
    }
}
