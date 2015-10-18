package com.xlythe.math;

import android.content.Context;

import org.javia.arity.Complex;
import org.javia.arity.Symbols;
import org.javia.arity.SyntaxException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Solves math problems
 *
 * Supports:
 * Basic math + functions (trig, pi)
 * Matrices
 * Hex and Bin conversion
 */
public class Solver {
    // Used for solving basic math
    public Symbols mSymbols = new Symbols();
    private BaseModule mBaseModule;
    private MatrixModule mMatrixModule;
    private int mLineLength = 8;
    private Localizer mLocalizer;

    public Solver() {
        mBaseModule = new BaseModule(this);
        mMatrixModule = new MatrixModule(this);
    }

    /**
     * Input an equation as a string
     * ex: sin(150)
     * and get the result returned.
     * */
    public String solve(String input) throws SyntaxException {
        if(displayContainsMatrices(input)) {
            return mMatrixModule.evaluateMatrices(input);
        }

        if(input.trim().isEmpty()) {
            return "";
        }

        if(mLocalizer != null) input = mLocalizer.localize(input);

        // Drop final infix operators (they can only result in error)
        int size = input.length();
        while(size > 0 && isOperator(input.charAt(size - 1))) {
            input = input.substring(0, size - 1);
            --size;
        }

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

        real = mBaseModule.updateTextToNewMode(real, Base.DECIMAL, mBaseModule.getBase())
                .replace('-', Constants.MINUS)
                .replace(Constants.INFINITY, Constants.INFINITY_UNICODE);
        imaginary = mBaseModule.updateTextToNewMode(imaginary, Base.DECIMAL, mBaseModule.getBase())
                .replace('-', Constants.MINUS)
                .replace(Constants.INFINITY, Constants.INFINITY_UNICODE);

        String result = "";
        if(value.re != 0 && value.im == 1) result = real + "+" + "i";
        else if(value.re != 0 && value.im > 0) result = real + "+" + imaginary + "i";
        else if(value.re != 0 && value.im == -1) result = real + "-" + "i";
        else if(value.re != 0 && value.im < 0) result = real + imaginary + "i"; // Implicit -
        else if(value.re != 0 && value.im == 0) result = real;
        else if(value.re == 0 && value.im == 1) result = "i";
        else if(value.re == 0 && value.im == -1) result = "-i";
        else if(value.re == 0 && value.im != 0) result = imaginary + "i";
        else if(value.re == 0 && value.im == 0) result = "0";

        if(mLocalizer != null) result = mLocalizer.relocalize(result);

        return result;
    }

    public static boolean isOperator(char c) {
        return ("" +
                Constants.PLUS +
                Constants.MINUS +
                Constants.DIV +
                Constants.MUL +
                Constants.POWER).indexOf(c) != -1;
    }

    public static boolean isOperator(String c) {
        return isOperator(c.charAt(0));
    }

    public static boolean isNegative(String number) {
        return number.startsWith(String.valueOf(Constants.MINUS)) || number.startsWith("-");
    }

    public static boolean isDigit(char number) {
        return String.valueOf(number).matches(Constants.REGEX_NUMBER);
    }

    boolean displayContainsMatrices(String text) {
        return getMatrixModule().isMatrix(text);
    }

    public String convertToDecimal(String input) throws SyntaxException{
        return mBaseModule.updateTextToNewMode(input, mBaseModule.getBase(), Base.DECIMAL);
    }

    String tryFormattingWithPrecision(double value, int precision) throws SyntaxException {
        // The standard scientific formatter is basically what we need. We will
        // start with what it produces and then massage it a bit.
        boolean isNaN = (String.valueOf(value).trim().equalsIgnoreCase(Constants.NAN));
        if (isNaN) {
            throw new SyntaxException();
        }
        String result = String.format(Locale.US, "%" + mLineLength + "." + precision + "g", value);
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
        } else {
            result = mantissa;
        }
        return result;
    }

    public void enableLocalization(Context context, Class r) {
        mLocalizer = new Localizer(context, r);
    }

    public void setLineLength(int length) {
        mLineLength = length;
    }

    public void setBase(Base base) {
        mBaseModule.setBase(base);
    }

    public Base getBase() {
        return mBaseModule.getBase();
    }

    public BaseModule getBaseModule() {
        return mBaseModule;
    }

    public MatrixModule getMatrixModule() {
        return mMatrixModule;
    }
}
