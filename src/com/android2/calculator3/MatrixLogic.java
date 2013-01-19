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

import org.javia.arity.Complex;
import org.javia.arity.SyntaxException;

import android.content.Context;
import android.view.KeyEvent;

import com.android2.calculator3.view.CalculatorDisplay;
import com.android2.calculator3.view.MatrixDisplayFragment;

public class MatrixLogic extends Logic {
    private MatrixDisplayFragment mDisplay;

    MatrixLogic(Context context) {
        super(context);
    }

    @Override
    public String getText() {
        String text;
        try {
            text = mDisplay.layout.getText().toString();
        }
        catch(IndexOutOfBoundsException e) {
            text = "";
        }
        return text;
    }

    @Override
    void setText(String text) {
        clear();
        mDisplay.layout.insert(text);
        if(text.equals(mErrorString)) setDeleteMode(DELETE_MODE_CLEAR);
    }

    @Override
    void insert(String delta) {
        mDisplay.layout.insert(delta);
        setDeleteMode(DELETE_MODE_BACKSPACE);
    }

    private void clear() {
        mDisplay.layout.clear();
    }

    @Override
    void onDelete() {
        if(getText().equals(mResult) || mIsError) {
            clear();
        }
        else {
            mDisplay.layout.dispatchKeyEvent(new KeyEvent(0, KeyEvent.KEYCODE_DEL));
            mResult = "";
        }
    }

    void onEnter() {
        if(mDeleteMode == DELETE_MODE_CLEAR) {
            clear(); // clear after an Enter on result
        }
        else {
            evaluateAndShowResult(getText(), CalculatorDisplay.Scroll.UP);
        }
    }

    public void evaluateAndShowResult(String text) {
        try {
            String result = evaluate(text);
            if(!text.equals(result)) {
                mResult = result;
                mDisplay.layout.setText(mResult);
                setDeleteMode(DELETE_MODE_CLEAR);
            }
        }
        catch(SyntaxException e) {
            mIsError = true;
            mResult = mErrorString;
            mDisplay.layout.setText(mResult);
            setDeleteMode(DELETE_MODE_CLEAR);
        }
    }

    String evaluate(String input) throws SyntaxException {
        if(input.trim().isEmpty()) {
            return "";
        }

        // Drop final infix operators (they can only result in error)
        int size = input.length();
        while(size > 0 && isOperator(input.charAt(size - 1))) {
            input = input.substring(0, size - 1);
            --size;
        }

        input = localize(input);

        Complex value = mSymbols.evalComplex(input);

        String real = "";
        for(int precision = mLineLength; precision > 6; precision--) {
            real = tryFormattingWithPrecision(value.re, precision);
            if(real.length() <= mLineLength) {
                break;
            }
        }

        String imaginary = "";
        for(int precision = mLineLength; precision > 6; precision--) {
            imaginary = tryFormattingWithPrecision(value.im, precision);
            if(imaginary.length() <= mLineLength) {
                break;
            }
        }
        String result = "";
        if(value.re != 0 && value.im > 0) result = real + "+" + imaginary + "i";
        else if(value.re != 0 && value.im < 0) result = real + imaginary + "i"; // Implicit
                                                                                // -
        else if(value.re != 0 && value.im == 0) result = real;
        else if(value.re == 0 && value.im != 0) result = imaginary + "i";
        else if(value.re == 0 && value.im == 0) result = "0";
        return result.replace('-', MINUS).replace(INFINITY, INFINITY_UNICODE);
    }

    @Override
    void findEigenvalue() {}

    @Override
    void findDeterminant() {}

    @Override
    void solveMatrix() {}
}
