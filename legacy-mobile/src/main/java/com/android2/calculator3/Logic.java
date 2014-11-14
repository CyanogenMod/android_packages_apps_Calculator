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
import android.content.res.Resources;
import android.view.KeyEvent;
import android.widget.EditText;

import com.android2.calculator3.view.CalculatorDisplay;
import com.android2.calculator3.view.CalculatorDisplay.Scroll;
import com.android2.calculator3.view.GraphView;
import com.android2.calculator3.view.MatrixInverseView;
import com.android2.calculator3.view.MatrixTransposeView;
import com.android2.calculator3.view.MatrixView;
import com.xlythe.math.BaseModule;
import com.xlythe.math.EquationFormatter;
import com.xlythe.math.Point;
import com.xlythe.math.Solver;
import com.xlythe.math.GraphModule.OnGraphUpdatedListener;

import org.javia.arity.Symbols;
import org.javia.arity.SyntaxException;

import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class Logic {
    public static final String INFINITY_UNICODE = "\u221e";
    // Double.toString() for Infinity
    public static final String INFINITY = "Infinity";
    // Double.toString() for NaN
    public static final String NAN = "NaN";
    public static final char MINUS = '\u2212';
    public static final String NUMBER = "[" + Logic.MINUS + "-]?[A-F0-9]+(\\.[A-F0-9]*)?";
    public static final String MARKER_EVALUATE_ON_RESUME = "?";
    public static final int DELETE_MODE_BACKSPACE = 0;
    int mDeleteMode = DELETE_MODE_BACKSPACE;
    public static final int DELETE_MODE_CLEAR = 1;
    public static final int ROUND_DIGITS = 1;
    public static final Symbols mSymbols = new Symbols();
    static final char MUL = '\u00d7';
    static final char PLUS = '+';
    static final char DIV = '\u00f7';
    static final char POW = '^';
    final String mErrorString;
    private final Context mContext;
    CalculatorDisplay mDisplay;
    GraphView mGraphView;
    String mResult = "";
    boolean mIsError = false;
    int mLineLength = 0;
    EquationFormatter mEquationFormatter;
    private History mHistory;
    private Graph mGraph;
    private Listener mListener;
    private final Solver mSolver = new Solver();
    private OnGraphUpdatedListener mOnGraphUpdateListener = new OnGraphUpdatedListener() {
        @Override
        public void onGraphUpdated(List<Point> result) {
            mGraph.setData(result);
        }
    };

    public Logic(Context context) {
        this(context, null);
    }

    Logic(Context context, CalculatorDisplay display) {
        final Resources r = context.getResources();
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        mContext = context.getApplicationContext();
        mErrorString = r.getString(R.string.error);

        mEquationFormatter = new EquationFormatter();
        mDisplay = display;
        if(mDisplay != null) mDisplay.setLogic(this);
    }

    public void setHistory(History history) {
        mHistory = history;
    }

    public void setGraphDisplay(GraphView graphView) {
        mGraphView = graphView;
    }

    public void setGraph(Graph graph) {
        mGraph = graph;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    void setLineLength(int nDigits) {
        mLineLength = nDigits;
    }

    void insert(String delta) {
        if(!acceptInsert(delta)) {
            clear(true);
        }
        mDisplay.insert(delta);
        setDeleteMode(DELETE_MODE_BACKSPACE);
        mSolver.graph(getText(), mOnGraphUpdateListener);
    }

    boolean acceptInsert(String delta) {
        if(mIsError || getText().equals(mErrorString)) {
            return false;
        }
        if(getDeleteMode() == DELETE_MODE_BACKSPACE || isOperator(delta) || isPostFunction(delta)) {
            return true;
        }

        EditText editText = mDisplay.getActiveEditText();
        int editLength = editText == null ? 0 : editText.getText().length();

        return mDisplay.getSelectionStart() != editLength;
    }

    public int getDeleteMode() {
        return mDeleteMode;
    }

    public static boolean isOperator(String text) {
        return text.length() == 1 && isOperator(text.charAt(0));
    }

    static boolean isPostFunction(String text) {
        return text.length() == 1 && isPostFunction(text.charAt(0));
    }

    static boolean isOperator(char c) {
        // plus minus times div
        return "+\u2212\u00d7\u00f7/*^".indexOf(c) != -1;
    }

    static boolean isPostFunction(char c) {
        // exponent, factorial, percent
        return "^!%".indexOf(c) != -1;
    }    public String getText() {
        return mDisplay.getText();
    }

    public void setDeleteMode(int mode) {
        if(mDeleteMode != mode) {
            mDeleteMode = mode;
            if(mListener != null) mListener.onDeleteModeChange();
        }
    }    void setText(String text) {
        clear(false);
        mDisplay.insert(text);
        if(text.equals(mErrorString)) setDeleteMode(DELETE_MODE_CLEAR);
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
        } else {
            mResult = "";
            mDisplay.setText(text, scroll ? CalculatorDisplay.Scroll.UP : CalculatorDisplay.Scroll.NONE);
            mIsError = false;
        }
    }

    public String convertToDecimal(String text) {
        try {
            return mSolver.convertToDecimal(text);
        } catch(SyntaxException e) {
            return mErrorString;
        }
    }

    public String evaluate(String text) {
        try {
            return mSolver.solve(text);
        } catch(SyntaxException e) {
            return mErrorString;
        }
    }

    public void evaluateAndShowResult(String text, Scroll scroll) {
        try {
            String result = mSolver.solve(text);
            if(!text.equals(result)) {
                mHistory.enter(mEquationFormatter.appendParenthesis(text), result);
                mResult = result;
                mDisplay.setText(mResult, scroll);
                setDeleteMode(DELETE_MODE_CLEAR);
            }
        } catch(SyntaxException e) {
            mIsError = true;
            mResult = mErrorString;
            mDisplay.setText(mResult, scroll);
            setDeleteMode(DELETE_MODE_CLEAR);
        }
    }

    boolean displayContainsMatrices() {
        boolean containsMatrices = false;
        for(int i = 0; i < mDisplay.getAdvancedDisplay().getChildCount(); i++) {
            if(mDisplay.getAdvancedDisplay().getChildAt(i) instanceof MatrixView)
                containsMatrices = true;
            if(mDisplay.getAdvancedDisplay().getChildAt(i) instanceof MatrixInverseView)
                containsMatrices = true;
            if(mDisplay.getAdvancedDisplay().getChildAt(i) instanceof MatrixTransposeView)
                containsMatrices = true;
        }
        return containsMatrices;
    }    private void clear(boolean scroll) {
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

    void onDelete() {
        if(getText().equals(mResult) || mIsError) {
            clear(false);
        } else {
            mDisplay.dispatchKeyEvent(new KeyEvent(0, KeyEvent.KEYCODE_DEL));
            mResult = "";
        }
        mSolver.graph(getText(), mOnGraphUpdateListener);
    }

    void onClear() {
        clear(mDeleteMode == DELETE_MODE_CLEAR);
        mSolver.graph(getText(), mOnGraphUpdateListener);
    }

    public void onEnter() {
        if(mDeleteMode == DELETE_MODE_CLEAR) {
            clearWithHistory(false); // clear after an Enter on result
        } else {
            evaluateAndShowResult(getText(), CalculatorDisplay.Scroll.UP);
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
    }    void updateHistory() {
        String text = getText();
        mHistory.update(text);
    }

    public boolean isError() {
        return getText().equals(mErrorString);
    }

    public Context getContext() {
        return mContext;
    }

    public interface Listener {
        void onDeleteModeChange();
    }

    public void setDomain(float min, float max) {
        mSolver.setDomain(min, max);
    }

    public void setZoomLevel(float level) {
        mSolver.setZoomLevel(level);
    }

    public void graph() {
        mSolver.graph(getText(), mOnGraphUpdateListener);
    }

    public BaseModule getBaseModule() {
        return mSolver.getBaseModule();
    }
}
