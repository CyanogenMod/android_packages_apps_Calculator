package com.android.calculator2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.javia.arity.SyntaxException;

public class BaseModule {
    public static final char SELECTION_HANDLE = '\u2620';
    public final String REGEX_NUMBER;
    public final String REGEX_NOT_NUMBER;

    Logic mLogic;
    private Mode mMode = Mode.DECIMAL;
    Map<Mode, List<Integer>> mBannedResources;

    BaseModule(Logic logic) {
        this.mLogic = logic;

        REGEX_NUMBER = "[A-F0-9" + Pattern.quote(mLogic.mDecimalPoint) + SELECTION_HANDLE + "]";
        REGEX_NOT_NUMBER = "[^A-F0-9" + Pattern.quote(mLogic.mDecimalPoint) + SELECTION_HANDLE + "]";

        mBannedResources = new HashMap<Mode, List<Integer>>(3);
        mBannedResources.put(Mode.DECIMAL, Arrays.asList(R.id.A, R.id.B, R.id.C, R.id.D, R.id.E, R.id.F));
        mBannedResources.put(Mode.BINARY, Arrays.asList(R.id.A, R.id.B, R.id.C, R.id.D, R.id.E, R.id.F, R.id.digit2, R.id.digit3, R.id.digit4, R.id.digit5,
                R.id.digit6, R.id.digit7, R.id.digit8, R.id.digit9));
        mBannedResources.put(Mode.HEXADECIMAL, new ArrayList<Integer>());
    }

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

    public Mode getMode() {
        return mMode;
    }

    public String setMode(Mode mode) {
        String text = updateTextToNewMode(mLogic.getText(), this.mMode, mode);
        this.mMode = mode;
        return text;
    }

    String updateTextToNewMode(final String originalText, final Mode mode1, final Mode mode2) {
        if(mode1.equals(mode2) || originalText.equals(mLogic.mErrorString) || originalText.isEmpty()) return originalText;

        String[] operations = originalText.split(REGEX_NUMBER);
        String[] numbers = originalText.split(REGEX_NOT_NUMBER);
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
                            return mLogic.mErrorString;
                        }
                        catch(SyntaxException e) {
                            return mLogic.mErrorString;
                        }
                        break;
                    case HEXADECIMAL:
                        try {
                            translatedNumbers[i] = newBase(numbers[i], 2, 16);
                        }
                        catch(NumberFormatException e) {
                            return mLogic.mErrorString;
                        }
                        catch(SyntaxException e) {
                            return mLogic.mErrorString;
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
                            return mLogic.mErrorString;
                        }
                        catch(SyntaxException e) {
                            return mLogic.mErrorString;
                        }
                        break;
                    case DECIMAL:
                        break;
                    case HEXADECIMAL:
                        try {
                            translatedNumbers[i] = newBase(numbers[i], 10, 16);
                        }
                        catch(NumberFormatException e) {
                            return mLogic.mErrorString;
                        }
                        catch(SyntaxException e) {
                            return mLogic.mErrorString;
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
                            return mLogic.mErrorString;
                        }
                        catch(SyntaxException e) {
                            return mLogic.mErrorString;
                        }
                        break;
                    case DECIMAL:
                        try {
                            translatedNumbers[i] = newBase(numbers[i], 16, 10);
                        }
                        catch(NumberFormatException e) {
                            e.printStackTrace();
                            return mLogic.mErrorString;
                        }
                        catch(SyntaxException e) {
                            e.printStackTrace();
                            return mLogic.mErrorString;
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
        String[] split = originalNumber.split(Pattern.quote(mLogic.mDecimalPoint));
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
            decimal = mLogic.mSymbols.eval(decimalFraction);
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
        return (wholeNumber + mLogic.mDecimalPoint + decimalNumber).toUpperCase(Locale.US);
    }

    public String groupSentence(String originalText, int selectionHandle) {
        if(originalText.equals(mLogic.mErrorString) || originalText.isEmpty()) return originalText;

        originalText = originalText.substring(0, selectionHandle) + SELECTION_HANDLE + originalText.substring(selectionHandle);
        String[] operations = originalText.split(REGEX_NUMBER);
        String[] numbers = originalText.split(REGEX_NOT_NUMBER);
        String[] translatedNumbers = new String[numbers.length];
        for(int i = 0; i < numbers.length; i++) {
            if(!numbers[i].isEmpty()) {
                translatedNumbers[i] = groupDigits(numbers[i], mMode);
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

    public String groupDigits(String number, Mode mode) {
        return groupDigits(number, mode, -1);
    }

    public String groupDigits(String number, Mode mode, int selectionHandle) {
        String sign = "";
        if(number.startsWith(String.valueOf(Logic.MINUS)) || number.startsWith("-")) {
            sign = String.valueOf(Logic.MINUS);
            number = number.substring(1);
        }
        String wholeNumber = number;
        String remainder = "";
        // We only group the whole number
        if(number.contains(mLogic.mDecimalPoint)) {
            if(!number.startsWith(mLogic.mDecimalPoint)) {
                String[] temp = number.split(Pattern.quote(mLogic.mDecimalPoint));
                wholeNumber = temp[0];
                remainder = mLogic.mDecimalPoint + ((temp.length == 1) ? "" : temp[1]);
            }
            else {
                wholeNumber = "";
                remainder = number;
            }
        }

        String modifiedNumber = wholeNumber;
        switch(mode) {
        case DECIMAL:
            modifiedNumber = group(wholeNumber, mLogic.mDecSeparatorDistance, mLogic.mDecSeparator);
            break;
        case BINARY:
            modifiedNumber = group(wholeNumber, mLogic.mBinSeparatorDistance, mLogic.mBinSeparator);
            break;
        case HEXADECIMAL:
            modifiedNumber = group(wholeNumber, mLogic.mHexSeparatorDistance, mLogic.mHexSeparator);
            break;
        }
        return sign + modifiedNumber + remainder;
    }

    private String group(String wholeNumber, int spacing, String separator) {
        String modifiedNumber = "";
        int offset = 0;
        for(int i = 1; i <= wholeNumber.length(); i++) {
            char charFromEnd = wholeNumber.charAt(wholeNumber.length() - i);
            modifiedNumber = charFromEnd + modifiedNumber;
            if(charFromEnd == SELECTION_HANDLE) {
                offset++;
                if(i == wholeNumber.length()) {
                    // Remove separator if we accidentally caused an extra one
                    if(modifiedNumber.startsWith(SELECTION_HANDLE + separator)) {
                        modifiedNumber = SELECTION_HANDLE + modifiedNumber.substring(2);
                    }
                }
            }
            else {
                if((i - offset) % spacing == 0 && i != wholeNumber.length() && (i - offset) != 0) {
                    modifiedNumber = separator + modifiedNumber;
                }
            }
        }
        return modifiedNumber;
    }
}
