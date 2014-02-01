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

import java.util.Locale;

import org.achartengine.GraphicalView;
import org.javia.arity.Complex;
import org.javia.arity.Symbols;
import org.javia.arity.SyntaxException;

import android.content.Context;
import android.content.res.Resources;
import android.view.KeyEvent;
import android.widget.EditText;

import com.android.calculator2.BaseModule.Mode;
import com.android.calculator2.view.CalculatorDisplay;
import com.android.calculator2.view.CalculatorDisplay.Scroll;
import com.android.calculator2.view.MatrixInverseView;
import com.android.calculator2.view.MatrixTransposeView;
import com.android.calculator2.view.MatrixView;

public class Logic {
    public static final String NUMBER = "[" + Logic.MINUS + "-]?[A-F0-9]+(\\.[A-F0-9]*)?";
    public static final String INFINITY_UNICODE = "\u221e";
    // Double.toString() for Infinity
    public static final String INFINITY = "Infinity";
    // Double.toString() for NaN
    public static final String NAN = "NaN";

    public static final char MINUS = '\u2212';
    static final char MUL = '\u00d7';
    static final char PLUS = '+';
    static final char DIV = '\u00f7';
    static final char POW = '^';

    public static final String MARKER_EVALUATE_ON_RESUME = "?";
    public static final int DELETE_MODE_BACKSPACE = 0;
    public static final int DELETE_MODE_CLEAR = 1;

    CalculatorDisplay mDisplay;
    GraphicalView mGraphDisplay;
    Symbols mSymbols = new Symbols();
    private final History mHistory;
    String mResult = "";
    boolean mIsError = false;
    int mLineLength = 0;
    private Graph mGraph;
    EquationFormatter mEquationFormatter;
    public GraphModule mGraphModule;
    public BaseModule mBaseModule;
    public MatrixModule mMatrixModule;

    private final boolean mUseRadians;

    final String mErrorString;
    private final String mSinString;
    private final String mCosString;
    private final String mTanString;
    private final String mArcsinString;
    private final String mArccosString;
    private final String mArctanString;
    private final String mLogString;
    private final String mLnString;
    private final String mDetString;
    private final String mCbrtString;
    final String mDecSeparator;
    final String mBinSeparator;
    final String mHexSeparator;
    final String mDecimalPoint;
    final String mMatrixSeparator;
    final int mDecSeparatorDistance;
    final int mBinSeparatorDistance;
    final int mHexSeparatorDistance;
    final String mX;
    final String mY;

    int mDeleteMode = DELETE_MODE_BACKSPACE;

    public interface Listener {
        void onDeleteModeChange();
    }

    private Listener mListener;

    Logic(Context context, History history, CalculatorDisplay display) {
        final Resources r = context.getResources();
        mErrorString = r.getString(R.string.error);
        mSinString = r.getString(R.string.sin);
        mCosString = r.getString(R.string.cos);
        mTanString = r.getString(R.string.tan);
        mArcsinString = r.getString(R.string.arcsin);
        mArccosString = r.getString(R.string.arccos);
        mArctanString = r.getString(R.string.arctan);
        mLogString = r.getString(R.string.lg);
        mLnString = r.getString(R.string.ln);
        mDetString = r.getString(R.string.det);
        mCbrtString = r.getString(R.string.cbrt);
        mDecSeparator = r.getString(R.string.dec_separator);
        mBinSeparator = r.getString(R.string.bin_separator);
        mHexSeparator = r.getString(R.string.hex_separator);
        mDecSeparatorDistance = r.getInteger(R.integer.dec_separator_distance);
        mBinSeparatorDistance = r.getInteger(R.integer.bin_separator_distance);
        mHexSeparatorDistance = r.getInteger(R.integer.hex_separator_distance);
        mDecimalPoint = r.getString(R.string.dot);
        mMatrixSeparator = r.getString(R.string.matrix_separator);
        mX = r.getString(R.string.X);
        mY = r.getString(R.string.Y);
        mUseRadians = CalculatorSettings.useRadians(context);

        mEquationFormatter = new EquationFormatter();
        mHistory = history;
        mDisplay = display;
        if(mDisplay != null) mDisplay.setLogic(this);
        mGraphModule = new GraphModule(this);
        mBaseModule = new BaseModule(this);
        mMatrixModule = new MatrixModule(this);
    }

    public void setGraphDisplay(GraphicalView graphDisplay) {
        mGraphDisplay = graphDisplay;
    }

    public void setGraph(Graph graph) {
        mGraph = graph;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public void setDeleteMode(int mode) {
        if(mDeleteMode != mode) {
            mDeleteMode = mode;
            mListener.onDeleteModeChange();
        }
    }

    public int getDeleteMode() {
        return mDeleteMode;
    }

    void setLineLength(int nDigits) {
        mLineLength = nDigits;
    }

    public String getText() {
        return mDisplay.getText();
    }

    void setText(String text) {
        clear(false);
        mDisplay.insert(text);
        if(text.equals(mErrorString)) setDeleteMode(DELETE_MODE_CLEAR);
    }

    void insert(String delta) {
        if(!acceptInsert(delta)) {
            clear(true);
        }
        mDisplay.insert(delta);
        setDeleteMode(DELETE_MODE_BACKSPACE);
        mGraphModule.updateGraphCatchErrors(mGraph);
    }

    public void onTextChanged() {
        setDeleteMode(DELETE_MODE_BACKSPACE);
    }

    public void resumeWithHistory() {
        clearWithHistory(false);
    }

    private void clearWithHistory(boolean scroll) {
        String text = mHistory.getText();
        if(MARKER_EVALUATE_ON_RESUME.equals(text)) {
            if(!mHistory.moveToPrevious()) {
                text = "";
            }
            text = mHistory.getBase();
            evaluateAndShowResult(text, CalculatorDisplay.Scroll.NONE);
        }
        else {
            mResult = "";
            mDisplay.setText(text, scroll ? CalculatorDisplay.Scroll.UP : CalculatorDisplay.Scroll.NONE);
            mIsError = false;
        }
    }

    private void clear(boolean scroll) {
        mHistory.enter("", "");
        mDisplay.setText("", scroll ? CalculatorDisplay.Scroll.UP : CalculatorDisplay.Scroll.NONE);
        cleared();
    }

    void cleared() {
        mResult = "";
        mIsError = false;
        updateHistory();

        setDeleteMode(DELETE_MODE_BACKSPACE);
    }

    boolean acceptInsert(String delta) {
        if (mIsError || getText().equals(mErrorString)) {
            return false;
        }
        if (getDeleteMode() == DELETE_MODE_BACKSPACE || isOperator(delta) || isPostFunction(delta)) {
            return true;
        }

        EditText editText = mDisplay.getActiveEditText();
        int editLength = editText == null ? 0 : editText.getText().length();

        return mDisplay.getSelectionStart() != editLength;
    }

    void onDelete() {
        if(getText().equals(mResult) || mIsError) {
            clear(false);
        }
        else {
            mDisplay.dispatchKeyEvent(new KeyEvent(0, KeyEvent.KEYCODE_DEL));
            mResult = "";
        }
        mGraphModule.updateGraphCatchErrors(mGraph);
    }

    void onClear() {
        clear(mDeleteMode == DELETE_MODE_CLEAR);
        mGraphModule.updateGraphCatchErrors(mGraph);
    }

    public void onEnter() {
        if(mDeleteMode == DELETE_MODE_CLEAR) {
            clearWithHistory(false); // clear after an Enter on result
        }
        else {
            evaluateAndShowResult(getText(), CalculatorDisplay.Scroll.UP);
        }
    }

    boolean displayContainsMatrices() {
        boolean containsMatrices = false;
        for(int i = 0; i < mDisplay.getAdvancedDisplay().getChildCount(); i++) {
            if(mDisplay.getAdvancedDisplay().getChildAt(i) instanceof MatrixView) containsMatrices = true;
            if(mDisplay.getAdvancedDisplay().getChildAt(i) instanceof MatrixInverseView) containsMatrices = true;
            if(mDisplay.getAdvancedDisplay().getChildAt(i) instanceof MatrixTransposeView) containsMatrices = true;
        }
        return containsMatrices;
    }

    public void evaluateAndShowResult(String text, Scroll scroll) {
        boolean containsMatrices = displayContainsMatrices();
        try {
            String result = containsMatrices ? mMatrixModule.evaluateMatrices(mDisplay.getAdvancedDisplay()) : evaluate(text);
            if(!text.equals(result)) {
                mHistory.enter(mEquationFormatter.appendParenthesis(text), result);
                mResult = result;
                mDisplay.setText(mResult, scroll);
                setDeleteMode(DELETE_MODE_CLEAR);
            }
        }
        catch(SyntaxException e) {
            mIsError = true;
            mResult = mErrorString;
            mDisplay.setText(mResult, scroll);
            setDeleteMode(DELETE_MODE_CLEAR);
        }
    }

    void onUp() {
        if(mHistory.moveToPrevious()) {
            mDisplay.setText(mHistory.getText(), CalculatorDisplay.Scroll.DOWN);
        }
    }

    void onDown() {
        if(mHistory.moveToNext()) {
            mDisplay.setText(mHistory.getText(), CalculatorDisplay.Scroll.UP);
        }
    }

    void updateHistory() {
        String text = getText();
        mHistory.update(text);
    }

    public static final int ROUND_DIGITS = 1;

    public String evaluate(String input) throws SyntaxException {
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

        // Convert to decimal
        String decimalInput = convertToDecimal(input);

        Complex value = mSymbols.evalComplex(decimalInput);

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

        real = mBaseModule.updateTextToNewMode(real, Mode.DECIMAL, mBaseModule.getMode()).replace('-', MINUS).replace(INFINITY, INFINITY_UNICODE);
        imaginary = mBaseModule.updateTextToNewMode(imaginary, Mode.DECIMAL, mBaseModule.getMode()).replace('-', MINUS).replace(INFINITY, INFINITY_UNICODE);

        String result = "";
        if(value.re != 0 && value.im > 0) result = real + "+" + imaginary + "i";
        else if(value.re != 0 && value.im < 0) result = real + imaginary + "i"; // Implicit
                                                                                // -
        else if(value.re != 0 && value.im == 0) result = real;
        else if(value.re == 0 && value.im != 0) result = imaginary + "i";
        else if(value.re == 0 && value.im == 0) result = "0";

        result = relocalize(result);
        return result;
    }

    public String convertToDecimal(String input) {
        return mBaseModule.updateTextToNewMode(input, mBaseModule.getMode(), Mode.DECIMAL);
    }

    String localize(String input) {
        // Delocalize functions (e.g. Spanish localizes "sin" as "sen"). Order
        // matters for arc functions
        input = input.replace(mArcsinString, "asin");
        input = input.replace(mArccosString, "acos");
        input = input.replace(mArctanString, "atan");
        input = input.replace(mSinString, "sin");
        input = input.replace(mCosString, "cos");
        input = input.replace(mTanString, "tan");
        if(!mUseRadians) {
            input = input.replace("sin", "sind");
            input = input.replace("cos", "cosd");
            input = input.replace("tan", "tand");
        }
        input = input.replace(mLogString, "log");
        input = input.replace(mLnString, "ln");
        input = input.replace(mDetString, "det");
        input = input.replace(mDecimalPoint, ".");
        input = input.replace(mMatrixSeparator, ",");
        input = input.replace(mCbrtString, "cbrt");
        return input;
    }

    String relocalize(String input) {
        input = input.replace(",", mMatrixSeparator);
        input = input.replace(".", mDecimalPoint);
        return input;
    }

    String tryFormattingWithPrecision(double value, int precision) {
        // The standard scientific formatter is basically what we need. We will
        // start with what it produces and then massage it a bit.
        String result = String.format(Locale.US, "%" + mLineLength + "." + precision + "g", value);
        if(result.equals(NAN)) { // treat NaN as Error
            return mErrorString;
        }
        String mantissa = result;
        String exponent = null;
        int e = result.indexOf('e');
        if(e != -1) {
            mantissa = result.substring(0, e);

            // Strip "+" and unnecessary 0's from the exponent
            exponent = result.substring(e + 1);
            if(exponent.startsWith("+")) {
                exponent = exponent.substring(1);
            }
            exponent = String.valueOf(Integer.parseInt(exponent));
        }

        int period = mantissa.indexOf('.');
        if(period == -1) {
            period = mantissa.indexOf(',');
        }
        if(period != -1) {
            // Strip trailing 0's
            while(mantissa.length() > 0 && mantissa.endsWith("0")) {
                mantissa = mantissa.substring(0, mantissa.length() - 1);
            }
            if(mantissa.length() == period + 1) {
                mantissa = mantissa.substring(0, mantissa.length() - 1);
            }
        }

        if(exponent != null) {
            result = mantissa + 'e' + exponent;
        }
        else {
            result = mantissa;
        }
        return result;
    }

    public static boolean isOperator(String text) {
        return text.length() == 1 && isOperator(text.charAt(0));
    }

    static boolean isOperator(char c) {
        // plus minus times div
        return "+\u2212\u00d7\u00f7/*".indexOf(c) != -1;
    }

    static boolean isPostFunction(String text) {
        return text.length() == 1 && isPostFunction(text.charAt(0));
    }

    static boolean isPostFunction(char c) {
        // exponent, factorial, percent
        return "^!%".indexOf(c) != -1;
    }
}
