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
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.SpannableStringBuilder;
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
    private EquationFormatter mEquationFormatter;
    private String input;
    private View container;

    public CalculatorEditText(final MatrixEnabledDisplay display) {
        super(display.getContext());
        setCustomSelectionActionModeCallback(new NoTextSelectionMode());
        setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);

        mEquationFormatter = new EquationFormatter(getContext());

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

        setContainer(this);

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

    public void setContainer(View container) {
        this.container = container;
    }

    public View getContainer() {
        return container;
    }

    public Editable getInput() {
        SpannableStringBuilder e = new SpannableStringBuilder();
        e.append(input);
        return e;
    }

    @Override
    public String toString() {
        return input;
    }

    public static String load(String text, final MatrixEnabledDisplay parent) {
        return CalculatorEditText.load(text, parent, parent.getChildCount());
    }

    public static String load(String text, final MatrixEnabledDisplay parent, final int pos) {
        if(text.isEmpty()) return text;
        final CalculatorEditText et = new CalculatorEditText(parent);
        et.setText(text.substring(0, 1));
        et.setSelection(1);
        et.setKeyListener(parent.mKeyListener);
        et.setEditableFactory(parent.mFactory);
        et.setBackgroundResource(android.R.color.transparent);
        et.setTextAppearance(parent.getContext(), R.style.display_style);
        parent.addView(et, pos);
        et.requestFocus();
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() == 0) {
                    parent.removeView(et.getContainer());
                }
            }
        });
        return text.substring(1, text.length());
    }
}
