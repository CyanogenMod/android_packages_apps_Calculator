/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.calculator2;

import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.Spanned;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ViewSwitcher;
import android.view.View.OnLongClickListener;

/**
 * Provides vertical scrolling for the input/result EditText.
 */
class CalculatorDisplay extends ViewSwitcher implements OnLongClickListener {

    private static final String ATTR_MAX_DIGITS = "maxDigits";
    private static final int DEFAULT_MAX_DIGITS = 10;

    // only these chars are accepted from keyboard
    private static final char[] ACCEPTED_CHARS =
        "0123456789.+-*/\u2212\u00d7\u00f7()!%^".toCharArray();

    private static final int ANIM_DURATION = 500;

    enum Scroll { UP, DOWN, NONE }

    TranslateAnimation inAnimUp;
    TranslateAnimation outAnimUp;
    TranslateAnimation inAnimDown;
    TranslateAnimation outAnimDown;

    private int mMaxDigits = DEFAULT_MAX_DIGITS;
    private List<String> keywords;

    public CalculatorDisplay(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMaxDigits = attrs.getAttributeIntValue(null, ATTR_MAX_DIGITS, DEFAULT_MAX_DIGITS);
        String sinString = context.getResources().getString(R.string.sin);
        String cosString = context.getResources().getString(R.string.cos);
        String tanString = context.getResources().getString(R.string.tan);
        String logString = context.getResources().getString(R.string.lg);
        String lnString = context.getResources().getString(R.string.ln);
        String modString = context.getResources().getString(R.string.mod);
        String dx = context.getResources().getString(R.string.dx);
        String dy = context.getResources().getString(R.string.dy);
        
        keywords = Arrays.asList(sinString + "(", 
                                              cosString + "(",
                                              tanString + "(",
                                              logString + "(",
                                              modString + "(",
                                              lnString + "(",
                                              dx,
                                              dy);
        setOnLongClickListener(this);
    }

    public int getMaxDigits() {
        return mMaxDigits;
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    protected void setLogic(Logic logic) {
        NumberKeyListener calculatorKeyListener =
            new NumberKeyListener() {
                public int getInputType() {
                    return EditorInfo.TYPE_CLASS_TEXT;
                }

                @Override
                protected char[] getAcceptedChars() {
                    return ACCEPTED_CHARS;
                }

                @Override
                public CharSequence filter(CharSequence source, int start, int end,
                                           Spanned dest, int dstart, int dend) {
                    /* the EditText should still accept letters (eg. 'sin')
                       coming from the on-screen touch buttons, so don't filter anything.
                    */
                    return null;
                }

                @Override
                public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_DEL) {
                        int selectionHandle = getSelectionStart();
                        String textBeforeInsertionHandle = getText().toString().substring(0, selectionHandle);
                        String textAfterInsertionHandle = getText().toString().substring(selectionHandle, getText().length());

                        for(String s : keywords) {
                            if(textBeforeInsertionHandle.endsWith(s)) {
                                int deletionLength = s.length();
                                String text = textBeforeInsertionHandle.substring(0, textBeforeInsertionHandle.length()-deletionLength) +
                                        textAfterInsertionHandle;
                                setText(text);
                                setSelection(selectionHandle-deletionLength);
                                return true;
                            }
                        }
                    }
                    return super.onKeyDown(view, content, keyCode, event);
                }
            };

        Editable.Factory factory = new CalculatorEditable.Factory(logic);
        for (int i = 0; i < 2; ++i) {
            EditText text = (EditText) getChildAt(i);
            if(android.os.Build.VERSION.SDK_INT < 16) {
                text.setBackgroundDrawable(null);
            }
            else {
                text.setBackground(null);
            }
            text.setEditableFactory(factory);
            text.setKeyListener(calculatorKeyListener);
            text.setSingleLine();
            text.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        }
    }

    @Override
    public void setOnKeyListener(OnKeyListener l) {
        getChildAt(0).setOnKeyListener(l);
        getChildAt(1).setOnKeyListener(l);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        inAnimUp = new TranslateAnimation(0, 0, h, 0);
        inAnimUp.setDuration(ANIM_DURATION);
        outAnimUp = new TranslateAnimation(0, 0, 0, -h);
        outAnimUp.setDuration(ANIM_DURATION);

        inAnimDown = new TranslateAnimation(0, 0, -h, 0);
        inAnimDown.setDuration(ANIM_DURATION);
        outAnimDown = new TranslateAnimation(0, 0, 0, h);
        outAnimDown.setDuration(ANIM_DURATION);
    }

    void insert(String delta) {
        EditText editor = (EditText) getCurrentView();
        int cursor = editor.getSelectionStart();
        editor.getText().insert(cursor, delta);
    }

    EditText getEditText() {
        return (EditText) getCurrentView();
    }

    Editable getText() {
        EditText text = (EditText) getCurrentView();
        return text.getText();
    }

    private void setText(String input) {
        EditText text = (EditText) getCurrentView();
        text.setText(input);
    }

    void setText(CharSequence text, Scroll dir) {
        if (getText().length() == 0) {
            dir = Scroll.NONE;
        }

        if (dir == Scroll.UP) {
            setInAnimation(inAnimUp);
            setOutAnimation(outAnimUp);
        } else if (dir == Scroll.DOWN) {
            setInAnimation(inAnimDown);
            setOutAnimation(outAnimDown);
        } else { // Scroll.NONE
            setInAnimation(null);
            setOutAnimation(null);
        }

        EditText editText = (EditText) getNextView();
        editText.setText(text);
        //Calculator.log("selection to " + text.length() + "; " + text);
        editText.setSelection(text.length());
        showNext();
    }

    int getSelectionStart() {
        EditText text = (EditText) getCurrentView();
        return text.getSelectionStart();
    }

    private void setSelection(int position) {
        EditText text = (EditText) getCurrentView();
        text.setSelection(position);
    }

    @Override
    protected void onFocusChanged(boolean gain, int direction, Rect prev) {
        //Calculator.log("focus " + gain + "; " + direction + "; " + prev);
        if (!gain) {
            requestFocus();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        return getCurrentView().performLongClick();
    }
}
