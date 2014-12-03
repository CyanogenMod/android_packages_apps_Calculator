package com.xlythe.math;

import java.text.DecimalFormatSymbols;

/**
 * Created by Will on 11/8/2014.
 */
public class Constants {
    public static final String INFINITY_UNICODE = "\u221e";
    // Double.toString() for Infinity
    public static final String INFINITY = "Infinity";
    // Double.toString() for NaN
    public static final String NAN = "NaN";
    public static final char MINUS = '\u2212';
    public static final char MUL = '\u00d7';
    public static final char PLUS = '+';
    public static final char DIV = '\u00f7';
    public static final char PLACEHOLDER = '\u200B';
    public static final char POWER = '^';
    public static final char EQUAL = '=';
    public static final char LEFT_PAREN = '(';
    public static final char RIGHT_PAREN = ')';

    // Values for decimals and comas
    private static final DecimalFormatSymbols DECIMAL_FORMAT = new DecimalFormatSymbols();
    public static final char DECIMAL_POINT;
    public static final char DECIMAL_SEPARATOR;
    public static final char BINARY_SEPARATOR;
    public static final char HEXADECIMAL_SEPARATOR;
    public static final char MATRIX_SEPARATOR;

    static {
        // These will already be known by Java
        DECIMAL_POINT = DECIMAL_FORMAT.getDecimalSeparator();
        DECIMAL_SEPARATOR = DECIMAL_FORMAT.getGroupingSeparator();

        // Use a space for Bin and Hex
        BINARY_SEPARATOR = ' ';
        HEXADECIMAL_SEPARATOR = ' ';

        // We have to be careful with the Matrix Separator.
        // It defaults to "," but that's a common decimal point.
        if(DECIMAL_POINT == ',') MATRIX_SEPARATOR = ' ';
        else MATRIX_SEPARATOR = ',';
    }
}
