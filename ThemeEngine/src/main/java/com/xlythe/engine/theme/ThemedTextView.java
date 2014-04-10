package com.xlythe.engine.theme;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class ThemedTextView extends TextView {
    public ThemedTextView(Context context) {
        super(context);
        setup(context, null);
    }

    public ThemedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context, attrs);
    }

    public ThemedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup(context, attrs);
    }

    private void setup(Context context, AttributeSet attrs) {
        // Get font
        Typeface t = Theme.getFont(context);
        if(t != null) {
            setTypeface(t);
        }

        if(attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.theme);
            if(a != null) {
                // Get text color
                setTextColor(Theme.get(a.getResourceId(R.styleable.theme_textColor, 0)));

                // Get text hint color
                setHintTextColor(Theme.get(a.getResourceId(R.styleable.theme_textColorHint, 0)));

                // Get background
                setBackground(Theme.get(a.getResourceId(R.styleable.theme_themeBackground, 0)));

                // Get custom font
                setFont(a.getString(R.styleable.theme_font));

                a.recycle();
            }
        }
    }

    public void setFont(String font) {
        if(font != null) {
            Typeface t = Theme.getFont(getContext(), font);
            if(t != null) {
                setTypeface(t);
            }
        }
    }

    public void setTextColor(Theme.Res res) {
        if(res != null) {
            if(Theme.COLOR.equals(res.getType())) {
                setTextColor(Theme.getColorStateList(getContext(), res.getName()));
            }
        }
    }

    public void setHintTextColor(Theme.Res res) {
        if(res != null) {
            if(Theme.COLOR.equals(res.getType())) {
                setHintTextColor(Theme.getColorStateList(getContext(), res.getName()));
            }
        }
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public void setBackground(Theme.Res res) {
        if(res != null) {
            if(Theme.COLOR.equals(res.getType())) {
                setBackgroundColor(Theme.getColor(getContext(), res.getName()));
            }
            else if(Theme.DRAWABLE.equals(res.getType())) {
                if(android.os.Build.VERSION.SDK_INT < 16) {
                    setBackgroundDrawable(Theme.getDrawable(getContext(), res.getName()));
                }
                else {
                    setBackground(Theme.getDrawable(getContext(), res.getName()));
                }
            }
        }
    }
}
