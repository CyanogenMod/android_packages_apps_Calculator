package com.android2.calculator3;

import java.util.ArrayList;
import java.util.Locale;

import org.javia.arity.SyntaxException;

public class BaseModule {
    private static final String REGEX_NUMBER = "[A-F0-9\\.]";
    private static final String REGEX_NOT_NUMBER = "[^A-F0-9\\.]";

    Logic logic;
    private Mode mode = Mode.DECIMAL;

    BaseModule(Logic logic) {
        this.logic = logic;
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
        return mode;
    }

    public String setMode(Mode mode) {
        String text = updateTextToNewMode(logic.getText(), this.mode, mode);
        this.mode = mode;
        return text;
    }

    String updateTextToNewMode(final String originalText, final Mode mode1, final Mode mode2) {
        if(mode1.equals(mode2) || originalText.equals(logic.mErrorString) || originalText.isEmpty()) return originalText;

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
                            return logic.mErrorString;
                        }
                        catch(SyntaxException e) {
                            return logic.mErrorString;
                        }
                        break;
                    case HEXADECIMAL:
                        try {
                            translatedNumbers[i] = newBase(numbers[i], 2, 16);
                        }
                        catch(NumberFormatException e) {
                            return logic.mErrorString;
                        }
                        catch(SyntaxException e) {
                            return logic.mErrorString;
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
                            return logic.mErrorString;
                        }
                        catch(SyntaxException e) {
                            return logic.mErrorString;
                        }
                        break;
                    case DECIMAL:
                        break;
                    case HEXADECIMAL:
                        try {
                            translatedNumbers[i] = newBase(numbers[i], 10, 16);
                        }
                        catch(NumberFormatException e) {
                            return logic.mErrorString;
                        }
                        catch(SyntaxException e) {
                            return logic.mErrorString;
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
                            return logic.mErrorString;
                        }
                        catch(SyntaxException e) {
                            return logic.mErrorString;
                        }
                        break;
                    case DECIMAL:
                        try {
                            translatedNumbers[i] = newBase(numbers[i], 16, 10);
                        }
                        catch(NumberFormatException e) {
                            e.printStackTrace();
                            return logic.mErrorString;
                        }
                        catch(SyntaxException e) {
                            e.printStackTrace();
                            return logic.mErrorString;
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
            decimal = logic.mSymbols.eval(decimalFraction);
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

    String groupDigits(String number, Mode mode) {
        String sign = "";
        if(number.startsWith(String.valueOf(Logic.MINUS)) || number.startsWith("-")) {
            sign = String.valueOf(Logic.MINUS);
            number = number.substring(1);
        }
        String wholeNumber = number;
        String remainder = "";
        // We only group the whole number
        if(number.contains(".")) {
            if(!number.startsWith(".")) {
                String[] temp = number.split("\\.");
                wholeNumber = temp[0];
                remainder = "." + ((temp.length == 1) ? "" : temp[1]);
            }
            else {
                wholeNumber = "";
                remainder = number;
            }
        }

        String modifiedNumber = wholeNumber;
        switch(mode) {
        case DECIMAL:
            modifiedNumber = "";
            // Add a coma every 3 chars, starting from the end.
            for(int i = 1; i <= wholeNumber.length(); i++) {
                char charFromEnd = wholeNumber.charAt(wholeNumber.length() - i);
                modifiedNumber = charFromEnd + modifiedNumber;
                if(i % 3 == 0 && i != wholeNumber.length()) {
                    modifiedNumber = "," + modifiedNumber;
                }
            }
            break;
        case BINARY:
            modifiedNumber = "";
            // Add a space every 4 chars, starting from the end.
            for(int i = 1; i <= wholeNumber.length(); i++) {
                char charFromEnd = wholeNumber.charAt(wholeNumber.length() - i);
                modifiedNumber = charFromEnd + modifiedNumber;
                if(i % 4 == 0 && i != wholeNumber.length()) {
                    modifiedNumber = " " + modifiedNumber;
                }
            }
            break;
        case HEXADECIMAL:
            modifiedNumber = "";
            // Add a space every 2 chars, starting from the end.
            for(int i = 1; i <= wholeNumber.length(); i++) {
                char charFromEnd = wholeNumber.charAt(wholeNumber.length() - i);
                modifiedNumber = charFromEnd + modifiedNumber;
                if(i % 2 == 0 && i != wholeNumber.length()) {
                    modifiedNumber = " " + modifiedNumber;
                }
            }
            break;
        }
        return sign + modifiedNumber + remainder;
    }
}
