package com.xlythe.math;

import android.util.Log;

import org.javia.arity.SyntaxException;

import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

public class BaseModule extends Module {
    private static final String TAG = "Calculator";

    // Used to keep a reference to the cursor in text
    public static final char SELECTION_HANDLE = '\u2620';

    // How many decimal places to approximate base changes
    private final static int PRECISION = 8;

    // Regex to strip out things like "90" from "sin(90)"
    private final String REGEX_NUMBER;
    private final String REGEX_NOT_NUMBER;

    // The current base. Defaults to decimal.
    private Base mBase = Base.DECIMAL;

    // A listener for when the base changes.
    private OnBaseChangeListener mBaseChangeListener;

    BaseModule(Solver solver) {
        super(solver);

        // Modify the constants to include a fake character, SELECTION_HANDLE
        REGEX_NUMBER = Constants.REGEX_NUMBER
                .substring(0, Constants.REGEX_NUMBER.length() - 1) + SELECTION_HANDLE + "]";
        REGEX_NOT_NUMBER = Constants.REGEX_NOT_NUMBER
                .substring(0, Constants.REGEX_NOT_NUMBER.length() - 1) + SELECTION_HANDLE + "]";
    }

    public Base getBase() {
        return mBase;
    }

    public void setBase(Base base) {
        mBase = base;
        if(mBaseChangeListener != null) mBaseChangeListener.onBaseChange(mBase);
    }

    public String setBase(String input, Base base) throws SyntaxException {
        String text = updateTextToNewMode(input, mBase, base);
        setBase(base);
        return text;
    }

    String updateTextToNewMode(final String originalText, final Base oldBase, final Base newBase) throws SyntaxException {
        if(oldBase.equals(newBase) || originalText.isEmpty() || originalText.matches(REGEX_NOT_NUMBER)) {
            return originalText;
        }

        String[] operations = originalText.split(REGEX_NUMBER);
        String[] numbers = originalText.split(REGEX_NOT_NUMBER);
        String[] translatedNumbers = new String[numbers.length];
        for(int i = 0; i < numbers.length; i++) {
            if(!numbers[i].isEmpty()) {
                switch(oldBase) {
                    case BINARY:
                        switch(newBase) {
                            case BINARY:
                                break;
                            case DECIMAL:
                                try {
                                    translatedNumbers[i] = newBase(numbers[i], 2, 10);
                                } catch(NumberFormatException e) {
                                    throw new SyntaxException();
                                }
                                break;
                            case HEXADECIMAL:
                                try {
                                    translatedNumbers[i] = newBase(numbers[i], 2, 16);
                                } catch(NumberFormatException e) {
                                    throw new SyntaxException();
                                }
                                break;
                        }
                        break;
                    case DECIMAL:
                        switch(newBase) {
                            case BINARY:
                                try {
                                    translatedNumbers[i] = newBase(numbers[i], 10, 2);
                                } catch(NumberFormatException e) {
                                    throw new SyntaxException();
                                }
                                break;
                            case DECIMAL:
                                break;
                            case HEXADECIMAL:
                                try {
                                    translatedNumbers[i] = newBase(numbers[i], 10, 16);
                                } catch(NumberFormatException e) {
                                    throw new SyntaxException();
                                }
                                break;
                        }
                        break;
                    case HEXADECIMAL:
                        switch(newBase) {
                            case BINARY:
                                try {
                                    translatedNumbers[i] = newBase(numbers[i], 16, 2);
                                } catch(NumberFormatException e) {
                                    throw new SyntaxException();
                                }
                                break;
                            case DECIMAL:
                                try {
                                    translatedNumbers[i] = newBase(numbers[i], 16, 10);
                                } catch(NumberFormatException e) {
                                    e.printStackTrace();
                                    throw new SyntaxException();
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
        } else {
            for(int i = 0; i < o.length && i < n.length; i++) {
                text += o[i];
                text += n[i];
            }
        }
        if(o.length > n.length) {
            text += o[o.length - 1];
        } else if(n.length > o.length) {
            text += n[n.length - 1];
        }
        return text;
    }

    private String newBase(String originalNumber, int originalBase, int base) throws SyntaxException {
        String[] split = originalNumber.split(Pattern.quote(getDecimalPoint()+""));
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

        // Catch overflow (it's a decimal, it can be (slightly) rounded
        if(split[1].length() > 13) {
            split[1] = split[1].substring(0, 13);
        }

        double decimal = 0;
        if(originalBase != 10) {
            String decimalFraction = Long.toString(Long.parseLong(split[1], originalBase)) + "/" + originalBase + "^" + split[1].length();
            decimal = getSolver().mSymbols.eval(decimalFraction);
        } else {
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
        return (wholeNumber + getDecimalPoint() + decimalNumber).toUpperCase(Locale.US);
    }

    private Object[] removeWhitespace(String[] strings) {
        ArrayList<String> formatted = new ArrayList<String>(strings.length);
        for(String s : strings) {
            if(s != null && !s.isEmpty()) formatted.add(s);
        }
        return formatted.toArray();
    }

    public String groupSentence(String originalText, int selectionHandle) {
        if(originalText.isEmpty() || originalText.matches(REGEX_NOT_NUMBER)) return originalText;

        if(selectionHandle >= 0) {
            originalText = originalText.substring(0, selectionHandle) +
                    SELECTION_HANDLE +
                    originalText.substring(selectionHandle);
        }
        String[] operations = originalText.split(REGEX_NUMBER);
        String[] numbers = originalText.split(REGEX_NOT_NUMBER);
        String[] translatedNumbers = new String[numbers.length];
        for(int i = 0; i < numbers.length; i++) {
            if(!numbers[i].isEmpty()) {
                translatedNumbers[i] = groupDigits(numbers[i], mBase);
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
        } else {
            for(int i = 0; i < o.length && i < n.length; i++) {
                text += o[i];
                text += n[i];
            }
        }
        if(o.length > n.length) {
            text += o[o.length - 1];
        } else if(n.length > o.length) {
            text += n[n.length - 1];
        }

        return text;
    }

    public String groupDigits(String number, Base base) {
        String sign = "";
        if(Solver.isNegative(number)) {
            sign = String.valueOf(Constants.MINUS);
            number = number.substring(1);
        }
        String wholeNumber = number;
        String remainder = "";
        // We only group the whole number
        if(number.contains(getDecimalPoint()+"")) {
            if(!number.startsWith(getDecimalPoint()+"")) {
                String[] temp = number.split(Pattern.quote(getDecimalPoint()+""));
                wholeNumber = temp[0];
                remainder = getDecimalPoint() + ((temp.length == 1) ? "" : temp[1]);
            } else {
                wholeNumber = "";
                remainder = number;
            }
        }

        String modifiedNumber = group(wholeNumber, getSeparatorDistance(base), getSeparator(base));

        return sign + modifiedNumber + remainder;
    }

    private String group(String wholeNumber, int spacing, char separator) {
        StringBuilder sb = new StringBuilder();
        int digitsSeen = 0;
        for (int i=wholeNumber.length()-1; i>=0; --i) {
            char curChar = wholeNumber.charAt(i);
            if (curChar != SELECTION_HANDLE) {
                if (digitsSeen > 0 && digitsSeen % spacing == 0) {
                    sb.insert(0, separator);
                }
                ++digitsSeen;
            }
            sb.insert(0, curChar);
        }
        return sb.toString();
    }

    public char getSeparator(Base base) {
        switch(base) {
            case DECIMAL:
                return getDecSeparator();
            case BINARY:
                return getBinSeparator();
            case HEXADECIMAL:
                return getHexSeparator();
            default:
                return 0;
        }
    }

    public char getSeparator() {
        return getSeparator(mBase);
    }

    private int getSeparatorDistance(Base base) {
        switch(base) {
            case DECIMAL:
                return getDecSeparatorDistance();
            case BINARY:
                return getBinSeparatorDistance();
            case HEXADECIMAL:
                return getHexSeparatorDistance();
            default:
                return -1;
        }
    }

    public OnBaseChangeListener getOnBaseChangeListener() {
        return mBaseChangeListener;
    }

    public void setOnBaseChangeListener(OnBaseChangeListener l) {
        mBaseChangeListener = l;
    }

    public static interface OnBaseChangeListener {
        public void onBaseChange(Base newBase);
    }
}
