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

package com.android.calculator2.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.android.calculator2.EquationFormatter;
import com.android.calculator2.R;

public class CalculatorEditText extends EditText {
    private static final int BLINK = 500;

    private EquationFormatter mEquationFormatter;
    private AdvancedDisplay mDisplay;
    private long mShowCursor = SystemClock.uptimeMillis();
    Paint mHighlightPaint = new Paint();
    Handler mHandler = new Handler();
    Runnable mRefresher = new Runnable() {
        @Override
        public void run() {
            CalculatorEditText.this.invalidate();
        }
    };
    private String input = "";

    public CalculatorEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setUp();
    }

    public CalculatorEditText(final AdvancedDisplay display) {
        super(display.getContext());
        setUp();
        mDisplay = display;
        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) display.mActiveEditText = CalculatorEditText.this;
            }
        });
    }

    private void setUp() {
        setCustomSelectionActionModeCallback(new NoTextSelectionMode());
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);

        mEquationFormatter = new EquationFormatter();

        addTextChangedListener(new TextWatcher() {
            boolean updating = false;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                if(updating) return;

                input = s.toString().replace(EquationFormatter.PLACEHOLDER, EquationFormatter.POWER);

                updating = true;
                int selectionHandle = getSelectionStart();
                setText(Html.fromHtml(mEquationFormatter.insertSupscripts(input)));
                setSelection(Math.min(selectionHandle, getText().length()));
                updating = false;
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
            View v = mDisplay.nextView(this);
            while(!v.isFocusable())
                v = mDisplay.nextView(v);
            return v;
        }
        return super.focusSearch(direction);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // TextViews don't draw the cursor if textLength is 0. Because we're an
        // array of TextViews, we'd prefer that it did.
        if(getText().length() == 0 && isEnabled() && (isFocused() || isPressed())) {
            if((SystemClock.uptimeMillis() - mShowCursor) % (2 * BLINK) < BLINK) {
                mHighlightPaint.setColor(getCurrentTextColor());
                mHighlightPaint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight(), mHighlightPaint);
                mHandler.postAtTime(mRefresher, SystemClock.uptimeMillis() + BLINK);
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
        if(parent.mKeyListener != null) et.setKeyListener(parent.mKeyListener);
        if(parent.mFactory != null) et.setEditableFactory(parent.mFactory);
        et.setBackgroundResource(android.R.color.transparent);
        et.setTextAppearance(parent.getContext(), R.style.display_style);
        et.setPadding(5, 0, 5, 0);
        et.setEnabled(parent.isEnabled());
        AdvancedDisplay.LayoutParams params = new AdvancedDisplay.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        et.setLayoutParams(params);
        parent.addView(et, pos);
        return "";
    }
}
