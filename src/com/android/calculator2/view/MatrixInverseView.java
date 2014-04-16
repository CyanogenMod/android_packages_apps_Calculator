/*
 * Copyright (C) 2014 The CyanogenMod Project
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

package com.android.calculator2.view;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;
import android.text.InputType;

import com.android.calculator2.MutableString;
import com.android.calculator2.R;
import com.xlythe.engine.theme.Theme;
import com.xlythe.engine.theme.ThemedTextView;

public class MatrixInverseView extends ThemedTextView {
    private final static char PLACEHOLDER = '\uFEFF';
    public final static String PATTERN = PLACEHOLDER + "^-1";

    public MatrixInverseView(Context context) {
        super(context);
    }

    public MatrixInverseView(final AdvancedDisplay display) {
        super(display.getContext());

        setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        setText(Html.fromHtml("<sup><small>-1</small></sup>"));
        setTextAppearance(display.getContext(), R.style.Theme_Calculator_Display);
        setPadding(0, 0, 0, 0);
        setTextColor(Theme.get(R.color.display_text_color));

        Typeface tf = Theme.getFont(getContext());
        if (tf != null) {
            setTypeface(tf);
        }

        setFont("display_font");
    }

    public static boolean load(final MutableString text, final AdvancedDisplay parent) {
        boolean changed = MatrixInverseView.load(text, parent, parent.getChildCount());
        if (changed) {
            // Always append a trailing EditText
            CalculatorEditText.load(parent);
        }

        return changed;
    }

    public static boolean load(final MutableString text, final AdvancedDisplay parent,
            final int pos) {
        if (!text.startsWith(PATTERN)) {
            return false;
        }

        text.setText(text.substring(PATTERN.length()));

        MatrixInverseView mv = new MatrixInverseView(parent);
        parent.addView(mv, pos);

        return true;
    }

    @Override
    public String toString() {
        return PATTERN;
    }
}
