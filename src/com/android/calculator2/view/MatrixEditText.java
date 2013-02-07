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
import android.text.Editable;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.NumberKeyListener;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.android.calculator2.Logic;
import com.android.calculator2.R;

public class MatrixEditText extends EditText implements OnFocusChangeListener {
    private static final char[] ACCEPTED_CHARS = "0123456789.-\u2212".toCharArray();

    private MatrixView parent;
    private AdvancedDisplay display;

    public MatrixEditText(Context context) {
        super(context);
    }

    public MatrixEditText(final AdvancedDisplay display, final MatrixView parent) {
        super(display.getContext());
        setCustomSelectionActionModeCallback(new NoTextSelectionMode());
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
        int padding = getContext().getResources().getDimensionPixelSize(R.dimen.matrix_edit_text_padding);
        setPadding(padding, 0, padding, 0);
        this.parent = parent;
        this.display = display;
        setKeyListener(new MatrixKeyListener());
        setOnFocusChangeListener(this);
        setGravity(Gravity.CENTER);
    }

    class MatrixKeyListener extends NumberKeyListener {
        @Override
        public int getInputType() {
            return EditorInfo.TYPE_CLASS_NUMBER | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        }

        @Override
        protected char[] getAcceptedChars() {
            return ACCEPTED_CHARS;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            for(int i = start; i < end; i++) {
                String text = dest.toString().substring(0, dstart) + source.toString() + dest.toString().substring(dend);
                if(!text.matches(Logic.NUMBER)) {
                    return "";
                }
            }
            return null;
        }

        @Override
        public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event) {
            if(keyCode == KeyEvent.KEYCODE_DEL) {
                if(parent.isEmpty()) display.removeView(parent);
            }
            return super.onKeyDown(view, content, keyCode, event);
        }
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
    public void onFocusChange(View v, boolean hasFocus) {
        if(hasFocus) {
            display.mActiveEditText = MatrixEditText.this;
            if(getText().toString().equals(Logic.NAN)) {
                setText("");
            }
        }
    }

    @Override
    public String toString() {
        return getText().toString();
    }

    @Override
    public View focusSearch(int direction) {
        switch(direction) {
        case View.FOCUS_FORWARD:
            return parent.nextView(this);
        }
        return super.focusSearch(direction);
    }

    public MatrixView getMatrixView() {
        return parent;
    }
}
