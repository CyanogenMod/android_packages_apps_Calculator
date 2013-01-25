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
import android.text.Html;
import android.text.InputType;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.EditText;

import com.android2.calculator3.MutableString;
import com.android2.calculator3.R;

public class MatrixInverseView extends EditText {
    public MatrixInverseView(Context context) {
        super(context);
    }

    public MatrixInverseView(final AdvancedDisplay display) {
        super(display.getContext());
        setCustomSelectionActionModeCallback(new NoTextSelectionMode());
        setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        setText(Html.fromHtml("<sup>-1</sup>"));
        setBackgroundResource(android.R.color.transparent);
        setTextAppearance(display.getContext(), R.style.display_style);
        setPadding(0, 0, 0, 0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getActionMasked() == MotionEvent.ACTION_UP) {
            // Hack to prevent keyboard and insertion handle from showing.
            cancelLongPress();
        }
        return super.onTouchEvent(event);
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
        return "^[-1]";
    }

    public static boolean load(final MutableString text, final AdvancedDisplay parent) {
        boolean changed = MatrixInverseView.load(text, parent, parent.getChildCount());
        if(changed) {
            // Always append a trailing EditText
            CalculatorEditText.load("", parent);
        }
        return changed;
    }

    public static boolean load(final MutableString text, final AdvancedDisplay parent, final int pos) {
        if(!text.startsWith("^[-1]")) return false;

        text.setText(text.substring(5));

        MatrixInverseView mv = new MatrixInverseView(parent);
        parent.addView(mv, pos);

        return true;
    }
}
