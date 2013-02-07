package com.android.calculator2;

public class EquationFormatter {
    public static final char PLACEHOLDER = '\u200B';
    public static final char POWER = '^';
    public static final char PLUS = '+';
    public static final char MINUS = '\u2212';
    public static final char MUL = '\u00d7';
    public static final char DIV = '\u00f7';
    public static final char EQUAL = '=';
    public static final char LEFT_PAREN = '(';
    public static final char RIGHT_PAREN = ')';

    public String appendParenthesis(String input) {
        final StringBuilder formattedInput = new StringBuilder(input);

        int unclosedParen = 0;
        for(int i = 0; i < formattedInput.length(); i++) {
            if(formattedInput.charAt(i) == LEFT_PAREN) unclosedParen++;
            else if(formattedInput.charAt(i) == RIGHT_PAREN) unclosedParen--;
        }
        for(int i = 0; i < unclosedParen; i++) {
            formattedInput.append(RIGHT_PAREN);
        }
        return formattedInput.toString();
    }

    public String insertSupscripts(String input) {
        final StringBuilder formattedInput = new StringBuilder();

        int sub_open = 0;
        int sub_closed = 0;
        int paren_open = 0;
        int paren_closed = 0;
        for(int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if(c == POWER) {
                formattedInput.append("<sup>");
                sub_open++;
                if(i + 1 == input.length()) {
                    formattedInput.append(c);
                    formattedInput.append("</sup>");
                    sub_closed++;
                }
                else {
                    formattedInput.append(PLACEHOLDER);
                }
                continue;
            }

            if(sub_open > sub_closed) {
                if(paren_open == paren_closed) {
                    // Decide when to break the <sup> started by ^
                    if(c == PLUS // 2^3+1
                            || (c == MINUS && input.charAt(i - 1) != POWER) // 2^3-1
                            || c == MUL // 2^3*1
                            || c == DIV // 2^3/1
                            || c == EQUAL // X^3=1
                            || (c == LEFT_PAREN && (Character.isDigit(input.charAt(i - 1)) || input.charAt(i - 1) == RIGHT_PAREN)) // 2^3(1)
                                                                                                                                   // or
                                                                                                                                   // 2^(3-1)(0)
                            || (Character.isDigit(c) && input.charAt(i - 1) == RIGHT_PAREN) // 2^(3)1
                            || (!Character.isDigit(c) && Character.isDigit(input.charAt(i - 1)))) { // 2^3log(1)
                        while(sub_open > sub_closed) {
                            formattedInput.append("</sup>");
                            sub_closed++;
                        }
                        paren_open = 0;
                        paren_closed = 0;
                        if(c == LEFT_PAREN) {
                            paren_open--;
                        }
                        else if(c == RIGHT_PAREN) {
                            paren_closed--;
                        }
                    }
                }
                if(c == LEFT_PAREN) {
                    paren_open++;
                }
                else if(c == RIGHT_PAREN) {
                    paren_closed++;
                }
            }
            formattedInput.append(c);
        }
        while(sub_open > sub_closed) {
            formattedInput.append("</sup>");
            sub_closed++;
        }
        return formattedInput.toString();
    }
}
