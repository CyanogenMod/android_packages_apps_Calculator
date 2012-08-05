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

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

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
    private String solve;
    private String eigenvalue;
    private String determinant;
    private String hex;
    private String bin;
    
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
        solveForX = context.getResources().getString(R.string.solveForX);
        solveForY = context.getResources().getString(R.string.solveForY);
        solve = mContext.getResources().getString(R.string.solve);
        eigenvalue = mContext.getResources().getString(R.string.eigenvalue);
        determinant = mContext.getResources().getString(R.string.determinant);
        hex = mContext.getResources().getString(R.string.hex);
        bin = mContext.getResources().getString(R.string.bin);
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

        default:
            if (view instanceof Button) {
                String text = ((Button) view).getText().toString();
                if(text.equals("( )")){
                    if(mHandler.getText().contains("=")){
                        text = mHandler.getText().split("=", 1)[0] + "=(" + mHandler.getText().split("=", 1)[1] + ")";
                    }
                    else{
                        text = "(" + mHandler.getText() + ")";
                    }
                    mHandler.clear(false);
                }
                else if(text.equals(mModString)){
                    if(mHandler.getText().contains("=")){
                        if(mHandler.getText().split("=", 1).length>1){
                            text = mHandler.getText().split("=", 1)[0] + "=" + mModString + "(" + mHandler.getText().split("=", 1)[1] + ",";
                            mHandler.clear(false);
                        }
                        else{
                            text = mModString + "(";
                        }
                    }
                    else{
                        if(mHandler.getText().length()>0){
                            text = mModString + "(" + mHandler.getText() + ",";
                            mHandler.clear(false);
                        }
                        else{
                            text = mModString + "(";
                        }
                    }
                }
                else if(text.equals(eigenvalue)){
                    mHandler.findEigenvalue();
                    return;
                }
                else if(text.equals(determinant)){
                    mHandler.findDeterminant();
                    return;
                }
                else if(text.equals(solve)){
                    mHandler.solveMatrix();
                    return;
                }
                else if(text.equals(hex)){
                    try{
                        text = Integer.toHexString(Integer.parseInt(mHandler.getText()));
                        mHandler.clear(false);
                    } catch(NumberFormatException e){
                        text = mErrorString;
                        mHandler.clear(false);
                    }
                }
                else if(text.equals(bin)){
                    try{
                        text = Integer.toBinaryString(Integer.parseInt(mHandler.getText()));
                        mHandler.clear(false);
                    } catch(NumberFormatException e){
                        text = mErrorString;
                        mHandler.clear(false);
                    }
                }
                else if(text.equals(solveForX) || text.equals(solveForY) || (text.equals(mDX)) || (text.equals(mDY))){
                    //Do nothing
                }
                else if (text.length() >= 2) {
                    // add paren after sin, cos, ln, etc. from buttons
                    text += '(';
                }
                mHandler.insert(text);
                if (mPager != null && (mPager.getCurrentItem() == Calculator.ADVANCED_PANEL || mPager.getCurrentItem() == Calculator.FUNCTION_PANEL)) {
                    mPager.setCurrentItem(Calculator.BASIC_PANEL);
                }
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        int id = view.getId();
        if (id == R.id.del) {
            mHandler.onClear();
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
            if(mHandler.getText().endsWith(mSinString + "(")){
                String text = mHandler.getText().substring(0, mHandler.getText().length()-(mSinString.length()+1));
                mHandler.clear(false);
                mHandler.insert(text);
                return true;
            }
            else if(mHandler.getText().endsWith(mCosString + "(")){
                String text = mHandler.getText().substring(0, mHandler.getText().length()-(mCosString.length()+1));
                mHandler.clear(false);
                mHandler.insert(text);
                return true;
            }
            else if(mHandler.getText().endsWith(mTanString + "(")){
                String text = mHandler.getText().substring(0, mHandler.getText().length()-(mTanString.length()+1));
                mHandler.clear(false);
                mHandler.insert(text);
                return true;
            }
            else if(mHandler.getText().endsWith(mLogString + "(")){
                String text = mHandler.getText().substring(0, mHandler.getText().length()-(mLogString.length()+1));
                mHandler.clear(false);
                mHandler.insert(text);
                return true;
            }
            else if(mHandler.getText().endsWith(mModString + "(")){
                String text = mHandler.getText().substring(0, mHandler.getText().length()-(mModString.length()+1));
                mHandler.clear(false);
                mHandler.insert(text);
                return true;
            }
            else if(mHandler.getText().endsWith(mLnString + "(")){
                String text = mHandler.getText().substring(0, mHandler.getText().length()-(mLnString.length()+1));
                mHandler.clear(false);
                mHandler.insert(text);
                return true;
            }
            else if(mHandler.getText().endsWith(mDX)){
                String text = mHandler.getText().substring(0, mHandler.getText().length()-mDX.length());
                mHandler.clear(false);
                mHandler.insert(text);
                return true;
            }
            else if(mHandler.getText().endsWith(mDY)){
                String text = mHandler.getText().substring(0, mHandler.getText().length()-mDY.length());
                mHandler.clear(false);
                mHandler.insert(text);
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
