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

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import org.achartengine.GraphicalView;
import org.achartengine.model.XYSeries;
import org.achartengine.util.MathHelper;
import org.ejml.simple.SimpleMatrix;
import org.javia.arity.Complex;
import org.javia.arity.Symbols;
import org.javia.arity.SyntaxException;

import android.content.Context;
import android.content.res.Resources;
import android.view.KeyEvent;
import android.view.View;

import com.android.calculator2.view.AdvancedDisplay;
import com.android.calculator2.view.CalculatorDisplay;
import com.android.calculator2.view.CalculatorDisplay.Scroll;
import com.android.calculator2.view.MatrixInverseView;
import com.android.calculator2.view.MatrixTransposeView;
import com.android.calculator2.view.MatrixView;

public class Logic {
    private static final String REGEX_NUMBER = "[A-F0-9\\.]";
    private static final String REGEX_NOT_NUMBER = "[^A-F0-9\\.]";
    public static final String NUMBER = "[" + Logic.MINUS + "-]?[A-F0-9]+(\\.[A-F0-9]*)?";
    public static final String INFINITY_UNICODE = "\u221e";
    // Double.toString() for Infinity
    public static final String INFINITY = "Infinity";
    // Double.toString() for NaN
    public static final String NAN = "NaN";

    static final char MINUS = '\u2212';
    static final char MUL = '\u00d7';
    static final char PLUS = '+';

    public static final String MARKER_EVALUATE_ON_RESUME = "?";
    public static final int DELETE_MODE_BACKSPACE = 0;
    public static final int DELETE_MODE_CLEAR = 1;

    CalculatorDisplay mDisplay;
    private GraphicalView mGraphDisplay;
    Symbols mSymbols = new Symbols();
    private History mHistory;
    String mResult = "";
    boolean mIsError = false;
    int mLineLength = 0;
    private Graph mGraph;
    EquationFormatter mEquationFormatter;

    private boolean useRadians;

    final String mErrorString;
    private final String mSinString;
    private final String mCosString;
    private final String mTanString;
    private final String mArcsinString;
    private final String mArccosString;
    private final String mArctanString;
    private final String mLogString;
    private final String mLnString;
    private final String mX;
    private final String mY;

    int mDeleteMode = DELETE_MODE_BACKSPACE;
    private Mode mode = Mode.DECIMAL;

    public enum Mode {
        BINARY(0), DECIMAL(1), HEXADECIMAL(2);

        int quickSerializable;

        Mode(int num) {
            this.quickSerializable = num;
        }

        public int getQuickSerializable() {
            return quickSerializable;
        }
    }

    public interface Listener {
        void onDeleteModeChange();
    }

    private Listener mListener;

    Logic(Context context) {
        this(context, null, null);
    }

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
        mX = r.getString(R.string.X);
        mY = r.getString(R.string.Y);
        useRadians = CalculatorSettings.useRadians(context);

        mEquationFormatter = new EquationFormatter();
        mHistory = history;
        mDisplay = display;
        if(mDisplay != null) mDisplay.setLogic(this);
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
        mDisplay.insert(delta);
        setDeleteMode(DELETE_MODE_BACKSPACE);
        updateGraphCatchErrors(mGraph);
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
        String text = getText();
        return !mIsError && (!mResult.equals(text) || isOperator(delta) || mDisplay.getSelectionStart() != text.length());
    }

    void onDelete() {
        if(getText().equals(mResult) || mIsError) {
            clear(false);
        }
        else {
            mDisplay.dispatchKeyEvent(new KeyEvent(0, KeyEvent.KEYCODE_DEL));
            mResult = "";
        }
        updateGraphCatchErrors(mGraph);
    }

    void onClear() {
        clear(mDeleteMode == DELETE_MODE_CLEAR);
        updateGraphCatchErrors(mGraph);
    }

    void onEnter() {
        if(mDeleteMode == DELETE_MODE_CLEAR) {
            clearWithHistory(false); // clear after an Enter on result
        }
        else {
            evaluateAndShowResult(getText(), CalculatorDisplay.Scroll.UP);
        }
    }

    private boolean displayContainsMatrices() {
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
            String result;
            if(containsMatrices) result = evaluateMatrices(mDisplay.getAdvancedDisplay());
            else result = evaluate(text);
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
        String result = "";
        if(value.re != 0 && value.im > 0) result = real + "+" + imaginary + "i";
        else if(value.re != 0 && value.im < 0) result = real + imaginary + "i"; // Implicit
                                                                                // -
        else if(value.re != 0 && value.im == 0) result = real;
        else if(value.re == 0 && value.im != 0) result = imaginary + "i";
        else if(value.re == 0 && value.im == 0) result = "0";
        return updateTextToNewMode(result, Mode.DECIMAL, mode).replace('-', MINUS).replace(INFINITY, INFINITY_UNICODE);
    }

    public String convertToDecimal(String input) {
        return updateTextToNewMode(input, mode, Mode.DECIMAL);
    }

    String localize(String input) {
        // Delocalize functions (e.g. Spanish localizes "sin" as "sen"). Order
        // matters for arc functions
        input = input.replaceAll(Pattern.quote(mArcsinString), "asin");
        input = input.replaceAll(Pattern.quote(mArccosString), "acos");
        input = input.replaceAll(Pattern.quote(mArctanString), "atan");
        input = input.replaceAll(mSinString, "sin");
        input = input.replaceAll(mCosString, "cos");
        input = input.replaceAll(mTanString, "tan");
        if(!useRadians) {
            input = input.replaceAll("sin", "sind");
            input = input.replaceAll("cos", "cosd");
            input = input.replaceAll("tan", "tand");
        }
        input = input.replaceAll(mLogString, "log");
        input = input.replaceAll(mLnString, "ln");
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

    static boolean isOperator(String text) {
        return text.length() == 1 && isOperator(text.charAt(0));
    }

    static boolean isOperator(char c) {
        // plus minus times div
        return "+\u2212\u00d7\u00f7/*".indexOf(c) != -1;
    }

    private String evaluateMatrices(AdvancedDisplay display) throws SyntaxException {
        try {
            SimpleMatrix matrix = null;
            boolean add = false;
            boolean multiply = false;
            for(int i = 0; i < display.getChildCount(); i++) {
                View child = display.getChildAt(i);
                if(child instanceof MatrixView) {
                    if(!add && !multiply) {
                        matrix = ((MatrixView) child).getSimpleMatrix();
                    }
                    else if(add) {
                        add = false;
                        if(matrix == null) throw new SyntaxException();
                        matrix = matrix.plus(((MatrixView) child).getSimpleMatrix());
                    }
                    else if(multiply) {
                        multiply = false;
                        if(matrix == null) throw new SyntaxException();
                        matrix = matrix.mult(((MatrixView) child).getSimpleMatrix());
                    }
                }
                else if(child instanceof MatrixTransposeView) {
                    if(matrix == null) throw new SyntaxException();
                    matrix = matrix.transpose();
                }
                else if(child instanceof MatrixInverseView) {
                    if(matrix == null) throw new SyntaxException();
                    matrix = matrix.invert();
                }
                else {
                    String text = child.toString();
                    if(text.length() > 1) throw new SyntaxException();
                    else if(text.length() == 0) continue;
                    if(text.startsWith(String.valueOf(MUL))) multiply = true;
                    else if(text.startsWith(String.valueOf(PLUS))) add = true;
                    else throw new SyntaxException();
                }
            }
            return updateTextToNewMode(MatrixView.matrixToString(matrix), Mode.DECIMAL, mode);
        }
        catch(Exception e) {
            throw new SyntaxException();
        }
    }

    private boolean graphChanged(Graph graph, String equation, double minX, double maxX, double minY, double maxY) {
        return !equation.equals(getText()) || minY != graph.getRenderer().getYAxisMin() || maxY != graph.getRenderer().getYAxisMax()
                || minX != graph.getRenderer().getXAxisMin() || maxX != graph.getRenderer().getXAxisMax();
    }

    private boolean pointIsNaN(double lastV, double v, double max, double min) {
        return v == Double.NaN || v == Double.POSITIVE_INFINITY || v == Double.NEGATIVE_INFINITY || lastV > max && v < min || v > max && lastV < min;
    }

    void updateGraphCatchErrors(Graph g) {
        try {
            updateGraph(g);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    void updateGraph(final Graph g) {
        if(g == null) return;
        final String eq = getText();

        if(eq.isEmpty()) {
            XYSeries series = new XYSeries("");

            try {
                g.getDataset().removeSeries(g.getSeries());
                g.setSeries(series);
                g.getDataset().addSeries(series);
            }
            catch(NullPointerException e) {
                e.printStackTrace();
            }

            if(mGraphDisplay != null) mGraphDisplay.repaint();
            return;
        }

        if(isOperator(eq.charAt(eq.length() - 1)) || displayContainsMatrices() || eq.endsWith("(")) return;

        final String[] equation = eq.split("=");

        if(equation.length != 2) return;

        // Translate into decimal
        equation[0] = convertToDecimal(localize(equation[0]));
        equation[1] = convertToDecimal(localize(equation[1]));
        final double minY = g.getRenderer().getYAxisMin();
        final double maxY = g.getRenderer().getYAxisMax();
        final double minX = g.getRenderer().getXAxisMin();
        final double maxX = g.getRenderer().getXAxisMax();

        new Thread(new Runnable() {
            public void run() {
                try {
                    final XYSeries series = new XYSeries("");
                    double lastX = (maxX - minX) / 2 + minX;
                    double lastY = (maxY - minY) / 2 + minY;

                    if(equation[0].equals(mY) && !equation[1].contains(mY)) {
                        for(double x = minX; x <= maxX; x += (0.00125 * (maxX - minX))) {
                            if(graphChanged(g, eq, minX, maxX, minY, maxY)) return;

                            try {
                                mSymbols.define(mX, x);
                                double y = mSymbols.eval(equation[1]);

                                if(pointIsNaN(lastY, y, maxY, minY)) {
                                    series.add(x, MathHelper.NULL_VALUE);
                                }
                                else {
                                    series.add(x, y);
                                }
                                lastY = y;
                            }
                            catch(SyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else if(equation[0].equals(mX) && !equation[1].contains(mX)) {
                        for(double y = minY; y <= maxY; y += (0.00125 * (maxY - minY))) {
                            if(graphChanged(g, eq, minX, maxX, minY, maxY)) return;

                            try {
                                mSymbols.define(mY, y);
                                double x = mSymbols.eval(equation[1]);

                                if(pointIsNaN(lastX, x, maxX, minX)) {
                                    series.add(MathHelper.NULL_VALUE, y);
                                }
                                else {
                                    series.add(x, y);
                                }
                                lastX = x;
                            }
                            catch(SyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else if(equation[1].equals(mY) && !equation[0].contains(mY)) {
                        for(double x = minX; x <= maxX; x += (0.00125 * (maxX - minX))) {
                            if(graphChanged(g, eq, minX, maxX, minY, maxY)) return;

                            try {
                                mSymbols.define(mX, x);
                                double y = mSymbols.eval(equation[0]);

                                if(pointIsNaN(lastY, y, maxY, minY)) {
                                    series.add(x, MathHelper.NULL_VALUE);
                                }
                                else {
                                    series.add(x, y);
                                }
                                lastY = y;
                            }
                            catch(SyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else if(equation[1].equals(mX) && !equation[0].contains(mX)) {
                        for(double y = minY; y <= maxY; y += (0.00125 * (maxY - minY))) {
                            if(graphChanged(g, eq, minX, maxX, minY, maxY)) return;

                            try {
                                mSymbols.define(mY, y);
                                double x = mSymbols.eval(equation[0]);

                                if(pointIsNaN(lastX, x, maxX, minX)) {
                                    series.add(MathHelper.NULL_VALUE, y);
                                }
                                else {
                                    series.add(x, y);
                                }
                                lastX = x;
                            }
                            catch(SyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else {
                        for(double x = minX; x <= maxX; x += (0.01 * (maxX - minX))) {
                            for(double y = maxY; y >= minY; y -= (0.01 * (maxY - minY))) {
                                if(graphChanged(g, eq, minX, maxX, minY, maxY)) return;

                                try {
                                    mSymbols.define(mX, x);
                                    mSymbols.define(mY, y);
                                    Double leftSide = mSymbols.eval(equation[0]);
                                    Double rightSide = mSymbols.eval(equation[1]);
                                    if(leftSide < 0 && rightSide < 0) {
                                        if(leftSide * 0.97 >= rightSide && leftSide * 1.03 <= rightSide) {
                                            series.add(x, y);
                                            break;
                                        }
                                    }
                                    else {
                                        if(leftSide * 0.97 <= rightSide && leftSide * 1.03 >= rightSide) {
                                            series.add(x, y);
                                            break;
                                        }
                                    }
                                }
                                catch(SyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    try {
                        g.getDataset().removeSeries(g.getSeries());
                    }
                    catch(NullPointerException e) {
                        e.printStackTrace();
                    }
                    g.setSeries(series);
                    g.getDataset().addSeries(series);

                    if(mGraphDisplay != null) mGraphDisplay.repaint();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public Mode getMode() {
        return mode;
    }

    public String setMode(Mode mode) {
        String text = updateTextToNewMode(getText(), this.mode, mode);
        this.mode = mode;
        return text;
    }

    private String updateTextToNewMode(final String originalText, final Mode mode1, final Mode mode2) {
        if(mode1.equals(mode2) || originalText.equals(mErrorString) || originalText.isEmpty()) return originalText;

        System.out.println(originalText);
        String[] operations = originalText.split(REGEX_NUMBER);
        for(String s : operations) {
            System.out.println("Ops: " + s);
        }
        String[] numbers = originalText.split(REGEX_NOT_NUMBER);
        for(String s : numbers) {
            System.out.println("Nums: " + s);
        }
        String[] translatedNumbers = new String[numbers.length];
        for(int i = 0; i < numbers.length; i++) {
            if(!numbers[i].isEmpty()) {
                switch(mode1) {
                case BINARY:
                    switch(mode2) {
                    case BINARY:
                        break;
                    case DECIMAL:
                        try {
                            translatedNumbers[i] = newBase(numbers[i], 2, 10);
                        }
                        catch(NumberFormatException e) {
                            return mErrorString;
                        }
                        catch(SyntaxException e) {
                            return mErrorString;
                        }
                        break;
                    case HEXADECIMAL:
                        try {
                            translatedNumbers[i] = newBase(numbers[i], 2, 16);
                        }
                        catch(NumberFormatException e) {
                            return mErrorString;
                        }
                        catch(SyntaxException e) {
                            return mErrorString;
                        }
                        break;
                    }
                    break;
                case DECIMAL:
                    switch(mode2) {
                    case BINARY:
                        try {
                            translatedNumbers[i] = newBase(numbers[i], 10, 2);
                        }
                        catch(NumberFormatException e) {
                            return mErrorString;
                        }
                        catch(SyntaxException e) {
                            return mErrorString;
                        }
                        break;
                    case DECIMAL:
                        break;
                    case HEXADECIMAL:
                        try {
                            translatedNumbers[i] = newBase(numbers[i], 10, 16);
                        }
                        catch(NumberFormatException e) {
                            return mErrorString;
                        }
                        catch(SyntaxException e) {
                            return mErrorString;
                        }
                        break;
                    }
                    break;
                case HEXADECIMAL:
                    switch(mode2) {
                    case BINARY:
                        try {
                            translatedNumbers[i] = newBase(numbers[i], 16, 2);
                        }
                        catch(NumberFormatException e) {
                            return mErrorString;
                        }
                        catch(SyntaxException e) {
                            return mErrorString;
                        }
                        break;
                    case DECIMAL:
                        try {
                            translatedNumbers[i] = newBase(numbers[i], 16, 10);
                        }
                        catch(NumberFormatException e) {
                            e.printStackTrace();
                            return mErrorString;
                        }
                        catch(SyntaxException e) {
                            e.printStackTrace();
                            return mErrorString;
                        }
                        break;
                    case HEXADECIMAL:
                        break;
                    }
                    break;
                }
            }
        }
        String text = "";
        Object[] o = removeWhitespace(operations);
        Object[] n = removeWhitespace(translatedNumbers);
        if(originalText.substring(0, 1).matches(REGEX_NUMBER)) {
            for(int i = 0; i < o.length && i < n.length; i++) {
                text += n[i];
                text += o[i];
            }
        }
        else {
            for(int i = 0; i < o.length && i < n.length; i++) {
                text += o[i];
                text += n[i];
            }
        }
        if(o.length > n.length) {
            text += o[o.length - 1];
        }
        else if(n.length > o.length) {
            text += n[n.length - 1];
        }
        return text;
    }

    private Object[] removeWhitespace(String[] strings) {
        ArrayList<String> formatted = new ArrayList<String>(strings.length);
        for(String s : strings) {
            if(s != null && !s.isEmpty()) formatted.add(s);
        }
        return formatted.toArray();
    }

    private final static int PRECISION = 8;

    private String newBase(String originalNumber, int originalBase, int base) throws SyntaxException {
        String[] split = originalNumber.split("\\.");
        if(split.length == 0) {
            split = new String[1];
            split[0] = "0";
        }
        if(split[0].isEmpty()) {
            split[0] = "0";
        }
        if(originalBase != 10) {
            split[0] = Long.toString(Long.parseLong(split[0], originalBase));
        }

        String wholeNumber = "";
        switch(base) {
        case 2:
            wholeNumber = Long.toBinaryString(Long.parseLong(split[0]));
            break;
        case 10:
            wholeNumber = split[0];
            break;
        case 16:
            wholeNumber = Long.toHexString(Long.parseLong(split[0]));
            break;
        }
        if(split.length == 1) return wholeNumber.toUpperCase(Locale.US);

        double decimal = 0;
        if(originalBase != 10) {
            String decimalFraction = Long.toString(Long.parseLong(split[1], originalBase)) + "/" + originalBase + "^" + split[1].length();
            decimal = mSymbols.eval(decimalFraction);
        }
        else {
            decimal = Double.parseDouble("0." + split[1]);
        }
        if(decimal == 0) return wholeNumber.toUpperCase(Locale.US);

        String decimalNumber = "";
        for(int i = 0, id = 0; decimal != 0 && i <= PRECISION; i++) {
            decimal *= base;
            id = (int) Math.floor(decimal);
            decimal -= id;
            decimalNumber += Integer.toHexString(id);
        }
        return (wholeNumber + "." + decimalNumber).toUpperCase(Locale.US);
    }
}
