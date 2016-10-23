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

package com.android.calculator2.view.display;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.android.calculator2.R;
import com.android.calculator2.view.MatrixView;
import com.android.calculator2.view.TextUtil;
import com.xlythe.math.BaseModule;
import com.xlythe.math.Constants;
import com.xlythe.math.EquationFormatter;
import com.xlythe.math.Solver;

public class CalculatorEditText extends EditText {
    private static final int BLINK = 500;
    private static final String TAG = "CalculatorEditText";
    private final long mShowCursor = SystemClock.uptimeMillis();
    private final Paint mHighlightPaint = new Paint();
    private final Handler mHandler = new Handler();
    private final Runnable mRefresher = new Runnable() {
        @Override
        public void run() {
            invalidate();
        }
    };
    private EquationFormatter mEquationFormatter;
    private String mInput = "";
    private int mSelectionHandle = 0;
    private Solver mSolver;
    private EventListener mEventListener;

    public static CalculatorEditText getInstance(Context context, Solver solver, EventListener eventListener) {
        CalculatorEditText text = (CalculatorEditText) View.inflate(context, R.layout.view_edittext, null);
        text.mSolver = solver;
        text.mEventListener = eventListener;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        text.setLayoutParams(params);
        return text;
    }

    public CalculatorEditText(Context context) {
        super(context);
        setUp();
    }

    public CalculatorEditText(Context context, AttributeSet attr) {
        super(context, attr);
        setUp();
    }

    private void setUp() {
        setLongClickable(false);

        // Disable highlighting text
        setCustomSelectionActionModeCallback(new NoTextSelectionMode());

        // Display ^ , and other visual cues
        mEquationFormatter = new EquationFormatter();
        addTextChangedListener(new TextWatcher() {
            boolean updating = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if(updating) return;
                updating = true;

                mInput = s.toString()
                        .replace(Constants.PLACEHOLDER, Constants.POWER)
                        .replace(mSolver.getBaseModule().getSeparator() + "", "");

                // Get the selection handle, since we're setting text and that'll overwrite it
                mSelectionHandle = getSelectionStart();

                // Adjust the handle by removing any comas or spacing to the left
                String cs = s.subSequence(0, mSelectionHandle).toString();
                mSelectionHandle -= TextUtil.countOccurrences(cs, mSolver.getBaseModule().getSeparator());

                // Update the text with formatted (comas, etc) text
                setText(formatText(mInput));
                setSelection(Math.min(mSelectionHandle, getText().length()));

                updating = false;
            }
        });

        setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (i == keyEvent.KEYCODE_DEL) {
                    int sel = getSelectionStart();
                    String edit = getEditableText().toString();

                    // If we're trying to delete a separator shift the selector over
                    if (sel >= 1
                            && edit.charAt(sel - 1) == mSolver.getBaseModule().getSeparator()) {
                        setSelection(sel - 1);
                    }
                }

                return false;
            }
        });

        setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                    mEventListener.onEditTextChanged(CalculatorEditText.this);
            }
        });
    }

    private Spanned formatText(String input) {
        if(mSolver != null) {
            // Add grouping, and then split on the selection handle
            // which is saved as a unique char
            String grouped = mEquationFormatter.addComas(mSolver, input, mSelectionHandle);
            if (grouped.contains(String.valueOf(BaseModule.SELECTION_HANDLE))) {
                String[] temp = grouped.split(String.valueOf(BaseModule.SELECTION_HANDLE));
                mSelectionHandle = temp[0].length();
                input = "";
                for (String s : temp) {
                    input += s;
                }
            } else {
                input = grouped;
                mSelectionHandle = input.length();
            }
        }

        return Html.fromHtml(mEquationFormatter.insertSupScripts(input));
    }

    @Override
    public String toString() {
        return mInput;
    }

    @Override
    public View focusSearch(int direction) {
        ViewParent p = getParent();
        if (p instanceof AdvancedDisplay) {
            AdvancedDisplay parent = (AdvancedDisplay) p;
            View v;
            switch(direction) {
                case View.FOCUS_FORWARD:
                    v = parent.nextView(this);
                    while(!v.isFocusable())
                        v = parent.nextView(v);
                    return v;
                case View.FOCUS_BACKWARD:
                    v = parent.previousView(this);
                    while(!v.isFocusable())
                        v = parent.previousView(v);
                    if(MatrixView.class.isAssignableFrom(v.getClass())) {
                        v = ((ViewGroup) v).getChildAt(((ViewGroup) v).getChildCount() - 1);
                        v = ((ViewGroup) v).getChildAt(((ViewGroup) v).getChildCount() - 1);
                    }
                    return v;
            }
        } else {
            Log.d(TAG, "parent isn't an AdvancedDisplay");
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
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    mHighlightPaint.setStrokeWidth(6f);
                }
                canvas.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight(), mHighlightPaint);
                mHandler.postAtTime(mRefresher, SystemClock.uptimeMillis() + BLINK);
            }
        }
    }

    class NoTextSelectionMode implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Prevents the selection action mode on double tap.
            return false;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }
    }
}
