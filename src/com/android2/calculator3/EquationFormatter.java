package com.android2.calculator3;

import android.content.Context;

public class EquationFormatter {
    public final static char PLACEHOLDER = '\u200B';

    public final char power;
    public final char plus;
    public final char minus;
    public final char mul;
    public final char div;
    public final char equal;
    public final char leftParen;
    public final char rightParen;

    public EquationFormatter(Context context) {
        power = context.getString(R.string.power).charAt(0);
        plus = context.getString(R.string.plus).charAt(0);
        minus = context.getString(R.string.minus).charAt(0);
        mul = context.getString(R.string.mul).charAt(0);
        div = context.getString(R.string.div).charAt(0);
        equal = context.getString(R.string.equal).charAt(0);
        leftParen = context.getString(R.string.leftParen).charAt(0);
        rightParen = context.getString(R.string.rightParen).charAt(0);
    }

    public String appendParenthesis(String input) {
        final StringBuilder formattedInput = new StringBuilder(input);

        int unclosedParen = 0;
        for (int i = 0; i < formattedInput.length(); i++) {
            if(formattedInput.charAt(i) == leftParen) unclosedParen++;
            else if(formattedInput.charAt(i) == rightParen) unclosedParen--;
        }
        for (int i = 0; i < unclosedParen; i++) {
            formattedInput.append(rightParen);
        }
        return formattedInput.toString();
    }

    public String insertSupscripts(String input) {
        final StringBuilder formattedInput = new StringBuilder();

        int sub_open = 0;
        int sub_closed = 0;
        int paren_open = 0;
        int paren_closed = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if(c == power) {
                formattedInput.append("<sup>");
                sub_open++;
                if(i + 1 == input.length()) {
                    formattedInput.append(c);
                    sub_open--;
                }
                else {
                    formattedInput.append(PLACEHOLDER);
                }
                continue;
            }

            if(sub_open > sub_closed) {
                if(paren_open == paren_closed) {
                    // Decide when to break the <sup> started by ^
                    if(c == plus // 2^3+1
                            || (c == minus && input.charAt(i - 1) != power) // 2^3-1
                            || c == mul // 2^3*1
                            || c == div // 2^3/1
                            || c == equal // X^3=1
                            || (c == leftParen && (Character.isDigit(input.charAt(i - 1)) || input.charAt(i - 1) == rightParen)) // 2^3(1)
                                                                                                                                 // or
                                                                                                                                 // 2^(3-1)(0)
                            || (Character.isDigit(c) && input.charAt(i - 1) == rightParen) // 2^(3)1
                            || (!Character.isDigit(c) && Character.isDigit(input.charAt(i - 1)))) { // 2^3log(1)
                        while (sub_open > sub_closed) {
                            formattedInput.append("</sup>");
                            sub_closed++;
                        }
                        paren_open = 0;
                        paren_closed = 0;
                        if(c == leftParen) {
                            paren_open--;
                        }
                        else if(c == rightParen) {
                            paren_closed--;
                        }
                    }
                }
                if(c == leftParen) {
                    paren_open++;
                }
                else if(c == rightParen) {
                    paren_closed++;
                }
            }
            formattedInput.append(c);
        }
        return formattedInput.toString();
    }
}
