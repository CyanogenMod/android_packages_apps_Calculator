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

import java.util.List;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.calculator2.BaseModule.Mode;
import com.android.calculator2.Calculator.Panel;
import com.android.calculator2.view.MatrixEditText;
import com.android.calculator2.view.MatrixInverseView;
import com.android.calculator2.view.MatrixTransposeView;
import com.android.calculator2.view.MatrixView;

public class EventListener implements View.OnKeyListener, View.OnClickListener, View.OnLongClickListener {
    Context mContext;
    Logic mHandler;
    ViewPager mPager;
    ViewPager mSmallPager;
    ViewPager mLargePager;

    private String mErrorString;
    private String mModString;
    private String mX;
    private String mY;
    private String mDX;
    private String mDY;

    void setHandler(Context context, Logic handler, ViewPager pager) {
        setHandler(context, handler, pager, null, null);
    }

    void setHandler(Context context, Logic handler, ViewPager smallPager, ViewPager largePager) {
        setHandler(context, handler, null, smallPager, largePager);
    }

    private void setHandler(Context context, Logic handler, ViewPager pager, ViewPager smallPager, ViewPager largePager) {
        mContext = context;
        mHandler = handler;
        mPager = pager;
        mSmallPager = smallPager;
        mLargePager = largePager;

        mErrorString = mContext.getString(R.string.error);
        mModString = mContext.getString(R.string.mod);
        mX = mContext.getString(R.string.X);
        mY = mContext.getString(R.string.Y);
        mDX = mContext.getString(R.string.dx);
        mDY = mContext.getString(R.string.dy);
    }

    @Override
    public void onClick(View view) {
        View v;
        EditText active;
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
            mHandler.setText(mHandler.mBaseModule.setMode(Mode.HEXADECIMAL));
            view.setSelected(true);
            ((View) view.getParent()).findViewById(R.id.bin).setSelected(false);
            ((View) view.getParent()).findViewById(R.id.dec).setSelected(false);
            applyAllBannedResources(mHandler.mBaseModule, Mode.HEXADECIMAL);
            break;

        case R.id.bin:
            mHandler.setText(mHandler.mBaseModule.setMode(Mode.BINARY));
            view.setSelected(true);
            ((View) view.getParent()).findViewById(R.id.hex).setSelected(false);
            ((View) view.getParent()).findViewById(R.id.dec).setSelected(false);
            applyAllBannedResources(mHandler.mBaseModule, Mode.BINARY);
            break;

        case R.id.dec:
            mHandler.setText(mHandler.mBaseModule.setMode(Mode.DECIMAL));
            view.setSelected(true);
            ((View) view.getParent()).findViewById(R.id.bin).setSelected(false);
            ((View) view.getParent()).findViewById(R.id.hex).setSelected(false);
            applyAllBannedResources(mHandler.mBaseModule, Mode.DECIMAL);
            break;

        case R.id.matrix:
            mHandler.insert(MatrixView.getPattern(mContext));
            returnToBasic();
            break;

        case R.id.matrix_inverse:
            mHandler.insert(MatrixInverseView.PATTERN);
            returnToBasic();
            break;

        case R.id.matrix_transpose:
            mHandler.insert(MatrixTransposeView.PATTERN);
            returnToBasic();
            break;

        case R.id.plus_row:
            v = mHandler.mDisplay.getActiveEditText();
            if(v instanceof MatrixEditText) ((MatrixEditText) v).getMatrixView().addRow();
            break;

        case R.id.minus_row:
            v = mHandler.mDisplay.getActiveEditText();
            if(v instanceof MatrixEditText) ((MatrixEditText) v).getMatrixView().removeRow();
            break;

        case R.id.plus_col:
            v = mHandler.mDisplay.getActiveEditText();
            if(v instanceof MatrixEditText) ((MatrixEditText) v).getMatrixView().addColumn();
            break;

        case R.id.minus_col:
            v = mHandler.mDisplay.getActiveEditText();
            if(v instanceof MatrixEditText) ((MatrixEditText) v).getMatrixView().removeColumn();
            break;

        case R.id.next:
            if(mHandler.getText().equals(mErrorString)) mHandler.setText("");
            active = mHandler.mDisplay.getActiveEditText();
            if(active.getSelectionStart() == active.getText().length()) {
                v = mHandler.mDisplay.getActiveEditText().focusSearch(View.FOCUS_FORWARD);
                if(v != null) v.requestFocus();
                active = mHandler.mDisplay.getActiveEditText();
                active.setSelection(0);
            }
            else {
                active.setSelection(active.getSelectionStart() + 1);
            }
            break;

        // +/-, changes the sign of the current number. Might be useful later
        // (but removed for now)
        // case R.id.sign:
        // if(mHandler.getText().equals(mErrorString)) mHandler.setText("");
        // active = mHandler.mDisplay.getActiveEditText();
        // int selection = active.getSelectionStart();
        // if(active.getText().toString().matches(Logic.NUMBER)) {
        // if(active.getText().toString().startsWith(String.valueOf(Logic.MINUS)))
        // {
        // active.setText(active.getText().toString().substring(1));
        // selection--;
        // }
        // else {
        // active.setText(Logic.MINUS + active.getText().toString());
        // selection++;
        // }
        // if(selection > active.length()) selection--;
        // if(selection < 0) selection = 0;
        // active.setSelection(selection);
        // }
        // break;

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

        case R.id.easter:
            Toast.makeText(mContext, R.string.easter_egg, Toast.LENGTH_SHORT).show();
            break;

        default:
            if(view instanceof Button) {
                String text = ((Button) view).getText().toString();
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

    private void applyAllBannedResources(BaseModule base, Mode baseMode) {
        for(Mode key : base.mBannedResources.keySet()) {
            if(baseMode.compareTo(key) != 0) {
                applyBannedResources(base, key, true);
            }
        }
        applyBannedResources(base, baseMode, false);
    }

    private void applyBannedResources(BaseModule base, Mode baseMode, boolean enabled) {
        List<Integer> resources = base.mBannedResources.get(baseMode);
        ViewPager pager = mPager != null ? mPager : mSmallPager;
        for(Integer resource : resources) {
            final int resId = resource.intValue();
            // There are multiple views with the same id,
            // but the id is unique per page
            // Find ids on every page
            int count = pager.getAdapter().getCount();
            for(int i = 0; i < count; i++) {
                View child = ((CalculatorPageAdapter) pager.getAdapter()).getViewAt(i);
                View v = child.findViewById(resId);
                if(v != null) {
                    v.setEnabled(enabled);
                }
            }
            // An especial check when current pager is mLargePager
            if(mPager == null && mLargePager != null) {
                count = mLargePager.getAdapter().getCount();
                for(int i = 0; i < count; i++) {
                    View child = ((CalculatorPageAdapter) mLargePager.getAdapter()).getViewAt(i);
                    View v = child.findViewById(resId);
                    if(v != null) {
                        v.setEnabled(enabled);
                    }
                }
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        switch(view.getId()) {
        case R.id.del:
            mHandler.onClear();
            return true;

        case R.id.next:
            // Handle back
            EditText active = mHandler.mDisplay.getActiveEditText();
            if(active.getSelectionStart() == 0) {
                View v = mHandler.mDisplay.getActiveEditText().focusSearch(View.FOCUS_BACKWARD);
                if(v != null) v.requestFocus();
                active = mHandler.mDisplay.getActiveEditText();
                active.setSelection(active.getText().length());
            }
            else {
                active.setSelection(active.getSelectionStart() - 1);
            }
            return true;
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
            if(text.length() >= 2) {
                // Add paren after sin, cos, ln, etc. from buttons
                text += "(";
            }
            mHandler.insert(text);
            returnToBasic();
            return true;
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

    private boolean returnToBasic() {
        if(mPager != null && mPager.getCurrentItem() != Panel.BASIC.getOrder() && CalculatorSettings.returnToBasic(mContext)) {
            mPager.setCurrentItem(Panel.BASIC.getOrder());
            return true;
        }
        return false;
    }
}
