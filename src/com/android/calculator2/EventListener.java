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

import com.android.calculator2.Calculator.Panel;
import com.android.calculator2.Logic.Mode;

import android.content.Context;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

class EventListener implements View.OnKeyListener,
                               View.OnClickListener,
                               View.OnLongClickListener {
    Context mContext;
    Logic mHandler;
    ViewPager mPager;

    private String mErrorString;
    private String mSinString;
    private String mCosString;
    private String mTanString;
    private String mLogString;
    private String mLnString;
    private String mModString;
    private String mX;
    private String mY;
    private String mDX;
    private String mDY;
    private String solveForX;
    private String solveForY;
    
    void setHandler(Context context, Logic handler, ViewPager pager) {
        mContext = context;
        mHandler = handler;
        mPager = pager;
        
        mErrorString = context.getResources().getString(R.string.error);
        mSinString = context.getResources().getString(R.string.sin);
        mCosString = context.getResources().getString(R.string.cos);
        mTanString = context.getResources().getString(R.string.tan);
        mLogString = context.getResources().getString(R.string.lg);
        mLnString = context.getResources().getString(R.string.ln);
        mModString = context.getResources().getString(R.string.mod);
        mX = context.getResources().getString(R.string.X);
        mY = context.getResources().getString(R.string.Y);
        mDX = context.getResources().getString(R.string.dx);
        mDY = context.getResources().getString(R.string.dy);
        solveForX = mContext.getResources().getString(R.string.solveForX);
        solveForY = mContext.getResources().getString(R.string.solveForY);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
        case R.id.del:
            mHandler.onDelete();
            break;

        case R.id.clear:
            mHandler.onClear();
            break;

        case R.id.equal:
            if (mHandler.getText().contains(mX) || 
                mHandler.getText().contains(mY)) {
                if (!mHandler.getText().contains("=")) {
                    mHandler.insert("=");
                }
                break;
            }
            mHandler.onEnter();
            break;

        case R.id.eigenvalue:
            mHandler.findEigenvalue();
            break;

        case R.id.determinant:
            mHandler.findDeterminant();
            break;

        case R.id.solve:
            mHandler.solveMatrix();
            break;

        case R.id.solveForX:
            WolframAlpha.solve(mHandler.getText() + ", " + solveForX, new Handler(), 
                    new WolframAlpha.ResultsRunnable() {
                        @Override
                        public void run() {
                            String text = "";
                            for(String s : results) {
                                text += s + ", ";
                            }
                            if(text.length()>2) text = text.substring(0, text.length()-2);
                            mHandler.setText(text);
                        }
                    }, 
                    new Runnable() {
                        @Override
                        public void run() {
                            mHandler.setText(mErrorString);
                        }
                    });
            break;

        case R.id.solveForY:
            WolframAlpha.solve(mHandler.getText() + ", " + solveForY, new Handler(), 
                    new WolframAlpha.ResultsRunnable() {
                        @Override
                        public void run() {
                            String text = "";
                            for(String s : results) {
                                text += s + ", ";
                            }
                            if(text.length()>2) text = text.substring(0, text.length()-2);
                            mHandler.setText(text);
                        }
                    }, 
                    new Runnable() {
                        @Override
                        public void run() {
                            mHandler.setText(mErrorString);
                        }
                    });
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

        case R.id.parentheses:
            if(mHandler.getText().equals(mErrorString)) mHandler.setText("");
            if(mHandler.getText().contains("=")) {
                mHandler.setText(mHandler.getText().split("=", 1)[0] + "=(" + mHandler.getText().split("=", 1)[1] + ")");
            }
            else{
                mHandler.setText("(" + mHandler.getText() + ")");
            }
            break;

        case R.id.mod:
            if(mHandler.getText().equals(mErrorString)) mHandler.setText("");
            if(mHandler.getText().contains("=")) {
                if(mHandler.getText().split("=", 1).length>1) {
                    mHandler.setText(mHandler.getText().split("=", 1)[0] + "=" + mModString + "(" + mHandler.getText().split("=", 1)[1] + ",");
                }
                else{
                    mHandler.insert(mModString + "(");
                }
            }
            else{
                if(mHandler.getText().length()>0) {
                    mHandler.setText(mModString + "(" + mHandler.getText() + ",");
                }
                else{
                    mHandler.insert(mModString + "(");
                }
            }
            break;

        default:
            if (view instanceof Button) {
                if(mHandler.getText().equals(mErrorString)) mHandler.setText("");
                String text = ((Button) view).getText().toString();
                if (text.equals(mDX) || text.equals(mDY)) {
                    // Do nothing
                }
                else if (text.length() >= 2) {
                    // Add paren after sin, cos, ln, etc. from buttons
                    text += "(";
                }
                mHandler.insert(text);
                if (mPager != null && mPager.getCurrentItem() != Panel.BASIC.getOrder()) {
                    mPager.setCurrentItem(Panel.BASIC.getOrder());
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
        case R.id.determinant:
            Toast.makeText(mContext, R.string.determinantDesc, Toast.LENGTH_SHORT).show();
            return true;
        case R.id.eigenvalue:
            Toast.makeText(mContext, R.string.eigenvalueDesc, Toast.LENGTH_SHORT).show();
            return true;
        case R.id.dec:
            Toast.makeText(mContext, R.string.decDesc, Toast.LENGTH_SHORT).show();
            return true;
        case R.id.bin:
            Toast.makeText(mContext, R.string.binDesc, Toast.LENGTH_SHORT).show();
            return true;
        case R.id.hex:
            Toast.makeText(mContext, R.string.hexDesc, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        int action = keyEvent.getAction();

        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
            keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            boolean eat = mHandler.eatHorizontalMove(keyCode == KeyEvent.KEYCODE_DPAD_LEFT);
            return eat;
        }

        //Work-around for spurious key event from IME, bug #1639445
        if (action == KeyEvent.ACTION_MULTIPLE && keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            return true; // eat it
        }
        
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            if(mHandler.getText().endsWith(mSinString + "(")) {
                String text = mHandler.getText().substring(0, mHandler.getText().length()-(mSinString.length()+1));
                mHandler.setText(text);
                return true;
            }
            else if(mHandler.getText().endsWith(mCosString + "(")) {
                String text = mHandler.getText().substring(0, mHandler.getText().length()-(mCosString.length()+1));
                mHandler.setText(text);
                return true;
            }
            else if(mHandler.getText().endsWith(mTanString + "(")) {
                String text = mHandler.getText().substring(0, mHandler.getText().length()-(mTanString.length()+1));
                mHandler.setText(text);
                return true;
            }
            else if(mHandler.getText().endsWith(mLogString + "(")) {
                String text = mHandler.getText().substring(0, mHandler.getText().length()-(mLogString.length()+1));
                mHandler.setText(text);
                return true;
            }
            else if(mHandler.getText().endsWith(mModString + "(")) {
                String text = mHandler.getText().substring(0, mHandler.getText().length()-(mModString.length()+1));
                mHandler.setText(text);
                return true;
            }
            else if(mHandler.getText().endsWith(mLnString + "(")) {
                String text = mHandler.getText().substring(0, mHandler.getText().length()-(mLnString.length()+1));
                mHandler.setText(text);
                return true;
            }
            else if(mHandler.getText().endsWith(mDX)) {
                String text = mHandler.getText().substring(0, mHandler.getText().length()-mDX.length());
                mHandler.setText(text);
                return true;
            }
            else if(mHandler.getText().endsWith(mDY)) {
                String text = mHandler.getText().substring(0, mHandler.getText().length()-mDY.length());
                mHandler.setText(text);
                return true;
            }
            return false;
        }

        //Calculator.log("KEY " + keyCode + "; " + action);

        if (keyEvent.getUnicodeChar() == '=') {
            if (action == KeyEvent.ACTION_UP) {
                mHandler.onEnter();
            }
            return true;
        }

        if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER &&
            keyCode != KeyEvent.KEYCODE_DPAD_UP &&
            keyCode != KeyEvent.KEYCODE_DPAD_DOWN &&
            keyCode != KeyEvent.KEYCODE_ENTER) {
            if (keyEvent.isPrintingKey() && action == KeyEvent.ACTION_UP) {
                // Tell the handler that text was updated.
                mHandler.onTextChanged();
            }
            return false;
        }

        /*
           We should act on KeyEvent.ACTION_DOWN, but strangely
           sometimes the DOWN event isn't received, only the UP.
           So the workaround is to act on UP...
           http://b/issue?id=1022478
         */

        if (action == KeyEvent.ACTION_UP) {
            switch (keyCode) {
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
}
