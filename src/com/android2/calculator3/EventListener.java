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

package com.android2.calculator3;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android2.calculator3.Calculator.CalculatorSettings;
import com.android2.calculator3.Calculator.Panel;
import com.android2.calculator3.Logic.Mode;
import com.android2.calculator3.view.MatrixView;

public class EventListener implements View.OnKeyListener, View.OnClickListener, View.OnLongClickListener {
    Context mContext;
    Logic mHandler;
    ViewPager mPager;

    private String mErrorString;
    private String mModString;
    private String mX;
    private String mY;
    private String mDX;
    private String mDY;
    private List<String> bannedInDecimal;
    private List<String> bannedInBinary;

    void setHandler(Context context, Logic handler, ViewPager pager) {
        mContext = context;
        mHandler = handler;
        mPager = pager;

        mErrorString = mContext.getString(R.string.error);
        mModString = mContext.getString(R.string.mod);
        mX = mContext.getString(R.string.X);
        mY = mContext.getString(R.string.Y);
        mDX = mContext.getString(R.string.dx);
        mDY = mContext.getString(R.string.dy);

        String digit2 = mContext.getString(R.string.digit2);
        String digit3 = mContext.getString(R.string.digit3);
        String digit4 = mContext.getString(R.string.digit4);
        String digit5 = mContext.getString(R.string.digit5);
        String digit6 = mContext.getString(R.string.digit6);
        String digit7 = mContext.getString(R.string.digit7);
        String digit8 = mContext.getString(R.string.digit8);
        String digit9 = mContext.getString(R.string.digit9);
        String A = mContext.getString(R.string.A);
        String B = mContext.getString(R.string.B);
        String C = mContext.getString(R.string.C);
        String D = mContext.getString(R.string.D);
        String E = mContext.getString(R.string.E);
        String F = mContext.getString(R.string.F);
        bannedInDecimal = Arrays.asList(A, B, C, D, E, F);
        bannedInBinary = Arrays.asList(A, B, C, D, E, F, digit2, digit3, digit4, digit5, digit6, digit7, digit8, digit9);
    }

    @Override
    public void onClick(View view) {
        View v;
        int id = view.getId();
        switch(id) {
        case R.id.del:
            mHandler.onDelete();
            break;

        case R.id.clear:
            mHandler.onClear();
            break;

        case R.id.equal:
            if(mHandler.getText().contains(mX) || mHandler.getText().contains(mY)) {
                if(!mHandler.getText().contains("=")) {
                    mHandler.insert("=");
                    returnToBasic();
                }
                break;
            }
            mHandler.onEnter();
            break;

        case R.id.hex:
            mHandler.setText(mHandler.setMode(Mode.HEXADECIMAL));
            view.setBackgroundResource(R.color.pressed_color);
            ((View) view.getParent()).findViewById(R.id.bin).setBackgroundResource(R.drawable.btn_function);
            ((View) view.getParent()).findViewById(R.id.dec).setBackgroundResource(R.drawable.btn_function);
            break;

        case R.id.bin:
            mHandler.setText(mHandler.setMode(Mode.BINARY));
            view.setBackgroundResource(R.color.pressed_color);
            ((View) view.getParent()).findViewById(R.id.hex).setBackgroundResource(R.drawable.btn_function);
            ((View) view.getParent()).findViewById(R.id.dec).setBackgroundResource(R.drawable.btn_function);
            break;

        case R.id.dec:
            mHandler.setText(mHandler.setMode(Mode.DECIMAL));
            view.setBackgroundResource(R.color.pressed_color);
            ((View) view.getParent()).findViewById(R.id.bin).setBackgroundResource(R.drawable.btn_function);
            ((View) view.getParent()).findViewById(R.id.hex).setBackgroundResource(R.drawable.btn_function);
            break;

        case R.id.matrix:
            mHandler.insert("[[,][,]]");
            returnToBasic();
            break;

        case R.id.matrix_inverse:
            mHandler.insert("^[-1]");
            returnToBasic();
            break;

        case R.id.matrix_transpose:
            mHandler.insert("^T");
            returnToBasic();
            break;

        case R.id.plus_row:
            v = (View) ((View) mHandler.mDisplay.getActiveEditText().getParent()).getParent();
            if(v instanceof MatrixView) ((MatrixView) v).addRow();
            break;

        case R.id.minus_row:
            v = (View) ((View) mHandler.mDisplay.getActiveEditText().getParent()).getParent();
            if(v instanceof MatrixView) ((MatrixView) v).removeRow();
            break;

        case R.id.plus_col:
            v = (View) ((View) mHandler.mDisplay.getActiveEditText().getParent()).getParent();
            if(v instanceof MatrixView) ((MatrixView) v).addColumn();
            break;

        case R.id.minus_col:
            v = (View) ((View) mHandler.mDisplay.getActiveEditText().getParent()).getParent();
            if(v instanceof MatrixView) ((MatrixView) v).removeColumn();
            break;

        case R.id.next:
            v = mHandler.mDisplay.getActiveEditText().focusSearch(View.FOCUS_FORWARD);
            if(v != null) v.requestFocus();
            break;

        case R.id.parentheses:
            if(mHandler.getText().equals(mErrorString)) mHandler.setText("");
            if(mHandler.getText().contains("=")) {
                String[] equation = mHandler.getText().split("=");
                if(equation.length > 1) {
                    mHandler.setText(equation[0] + "=(" + equation[1] + ")");
                }
                else {
                    mHandler.setText(equation[0] + "=()");
                }
            }
            else {
                mHandler.setText("(" + mHandler.getText() + ")");
            }
            returnToBasic();
            break;

        case R.id.mod:
            if(mHandler.getText().equals(mErrorString)) mHandler.setText("");
            if(mHandler.getText().contains("=")) {
                String[] equation = mHandler.getText().split("=");
                if(equation.length > 1) {
                    mHandler.setText(equation[0] + "=" + mModString + "(" + equation[1] + ",");
                }
                else {
                    mHandler.insert(mModString + "(");
                }
            }
            else {
                if(mHandler.getText().length() > 0) {
                    mHandler.setText(mModString + "(" + mHandler.getText() + ",");
                }
                else {
                    mHandler.insert(mModString + "(");
                }
            }
            returnToBasic();
            break;

        default:
            if(view instanceof Button) {
                if(mHandler.getText().equals(mErrorString)) mHandler.setText("");
                String text = ((Button) view).getText().toString();
                if(!acceptableKey(text)) {
                    break;
                }
                if(text.equals(mDX) || text.equals(mDY)) {
                    // Do nothing
                }
                else if(text.length() >= 2) {
                    // Add paren after sin, cos, ln, etc. from buttons
                    text += "(";
                }
                mHandler.insert(text);
                returnToBasic();
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        switch(view.getId()) {
        case R.id.del:
            mHandler.onClear();
        }
        if(view.getTag() != null) {
            String text = (String) view.getTag();
            if(!text.isEmpty()) {
                Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        if(view instanceof TextView && ((TextView) view).getHint() != null) {
            String text = ((TextView) view).getHint().toString();
            if(acceptableKey(text)) {
                if(text.length() >= 2) {
                    // Add paren after sin, cos, ln, etc. from buttons
                    text += "(";
                }
                mHandler.insert(text);
                returnToBasic();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        int action = keyEvent.getAction();

        // Work-around for spurious key event from IME, bug #1639445
        if(action == KeyEvent.ACTION_MULTIPLE && keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            return true; // eat it
        }

        if(keyEvent.getUnicodeChar() == '=') {
            if(action == KeyEvent.ACTION_UP) {
                mHandler.onEnter();
            }
            return true;
        }

        if(keyCode != KeyEvent.KEYCODE_DPAD_CENTER && keyCode != KeyEvent.KEYCODE_DPAD_UP && keyCode != KeyEvent.KEYCODE_DPAD_DOWN
                && keyCode != KeyEvent.KEYCODE_ENTER) {
            if(keyEvent.isPrintingKey() && action == KeyEvent.ACTION_UP) {
                // Tell the handler that text was updated.
                mHandler.onTextChanged();
            }
            return false;
        }

        /*
         * We should act on KeyEvent.ACTION_DOWN, but strangely sometimes the
         * DOWN event isn't received, only the UP. So the workaround is to act
         * on UP... http://b/issue?id=1022478
         */

        if(action == KeyEvent.ACTION_UP) {
            switch(keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                mHandler.onEnter();
                break;

            case KeyEvent.KEYCODE_DPAD_UP:
                mHandler.onUp();
                break;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                mHandler.onDown();
                break;
            }
        }
        return true;
    }

    private boolean acceptableKey(String text) {
        if(text.length() == 1) {
            // Disable ABCDEF in DEC/BIN and 23456789 in BIN
            if(mHandler.getMode().equals(Mode.DECIMAL)) {
                for(String s : bannedInDecimal) {
                    if(s.equals(text)) return false;
                }
            }
            if(mHandler.getMode().equals(Mode.BINARY)) {
                for(String s : bannedInBinary) {
                    if(s.equals(text)) return false;
                }
            }
        }
        return true;
    }

    private boolean returnToBasic() {
        if(mPager != null && mPager.getCurrentItem() != Panel.BASIC.getOrder() && CalculatorSettings.returnToBasic(mContext)) {
            mPager.setCurrentItem(Panel.BASIC.getOrder());
            return true;
        }
        return false;
    }
}
