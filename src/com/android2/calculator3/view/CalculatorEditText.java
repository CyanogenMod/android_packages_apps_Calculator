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
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.android2.calculator3.EquationFormatter;

public class CalculatorEditText extends EditText {
    private EquationFormatter mEquationFormatter;
    private String input;

    public CalculatorEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setCustomSelectionActionModeCallback(new NoTextSelectionMode());
        setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);

        mEquationFormatter = new EquationFormatter(context);

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

    public Editable getInput() {
        SpannableStringBuilder e = new SpannableStringBuilder();
        e.append(input);
        return e;
    }
}
