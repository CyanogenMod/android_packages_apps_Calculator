/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android2.calculator3.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.android2.calculator3.EquationFormatter;
import com.android2.calculator3.R;

public class CalculatorEditText extends EditText {
    private static final int BLINK = 500;

    private EquationFormatter mEquationFormatter;
    private AdvancedDisplay mDisplay;
    private long mShowCursor = SystemClock.uptimeMillis();
    private String input;

    public CalculatorEditText(Context context) {
        super(context);
    }

    public CalculatorEditText(final AdvancedDisplay display) {
        super(display.getContext());
        setCustomSelectionActionModeCallback(new NoTextSelectionMode());
        setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);

        mEquationFormatter = new EquationFormatter(getContext());
        mDisplay = display;

        addTextChangedListener(new TextWatcher() {
            boolean updating = false;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                if(updating) return;

                input = s.toString().replace(EquationFormatter.PLACEHOLDER, mEquationFormatter.power);

                updating = true;
                int selectionHandle = getSelectionStart();
                setText(Html.fromHtml(mEquationFormatter.insertSupscripts(input)));
                try {
                    setSelection(selectionHandle);
                }
                catch(IndexOutOfBoundsException e) {
                    setSelection(1);
                }
                updating = false;
            }
        });

        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) display.mActiveEditText = CalculatorEditText.this;
            }
        });
    }

    class NoTextSelectionMode implements ActionMode.Callback {
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Prevents the selection action mode on double tap.
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {}

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    }

    @Override
    public String toString() {
        return input;
    }

    @Override
    public View focusSearch(int direction) {
        switch(direction) {
        case View.FOCUS_FORWARD:
            return mDisplay.nextView(this);
        }
        return super.focusSearch(direction);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Doesn't draw the cursor if textLength is 0. Because we're an array of
        // EditTexts, we'd prefer that it did.
        if(getText().length() == 0 && getSelectionStart() == getSelectionEnd()) {
            if((SystemClock.uptimeMillis() - mShowCursor) % (2 * BLINK) < BLINK) {
                // if(mHighlightPathBogus) {
                // mHighlightPath.reset();
                // mLayout.getCursorPath(selStart, mHighlightPath, mText);
                // mHighlightPathBogus = false;
                // }

                // XXX should pass to skin instead of drawing directly
                Paint mHighlightPaint = new Paint();
                mHighlightPaint.setColor(getCurrentTextColor());
                mHighlightPaint.setStyle(Paint.Style.STROKE);

                // highlight = mHighlightPath;
            }
        }
    }

    public static String load(final AdvancedDisplay parent) {
        return CalculatorEditText.load("", parent);
    }

    public static String load(String text, final AdvancedDisplay parent) {
        return CalculatorEditText.load(text, parent, parent.getChildCount());
    }

    public static String load(String text, final AdvancedDisplay parent, final int pos) {
        final CalculatorEditText et = new CalculatorEditText(parent);
        et.setText(text);
        et.setSelection(0);
        et.setKeyListener(parent.mKeyListener);
        et.setEditableFactory(parent.mFactory);
        et.setBackgroundResource(android.R.color.transparent);
        et.setTextAppearance(parent.getContext(), R.style.display_style);
        et.setPadding(5, 0, 5, 0);
        parent.addView(et, pos);
        et.requestFocus();
        return "";
    }
}
