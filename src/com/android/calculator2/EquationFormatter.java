/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

public class EquationFormatter {
    public static final char PLACEHOLDER = '\u200B';
    public static final char POWER = '\u005E';
    public static final char PLUS = '\u002B';
    public static final char MINUS = '\u2212';
    public static final char MUL = '\u00d7';
    public static final char DIV = '\u00f7';
    public static final char EQUAL = '\u003D';
    public static final char LEFT_PAREN = '\u0028';
    public static final char RIGHT_PAREN = '\u0029';

    public String appendParenthesis(String input) {
        final StringBuilder formattedInput = new StringBuilder(input);

        int unclosedParen = 0;
        for (int i = 0; i < formattedInput.length(); i++) {
            if (formattedInput.charAt(i) == LEFT_PAREN) {
                unclosedParen++;
            } else if (formattedInput.charAt(i) == RIGHT_PAREN) {
                unclosedParen--;
            }
        }

        for (int i = 0; i < unclosedParen; i++) {
            formattedInput.append(RIGHT_PAREN);
        }

        return formattedInput.toString();
    }

    public String insertSupscripts(String input) {
        final StringBuilder formattedInput = new StringBuilder();

        int subOpen = 0;
        int subClosed = 0;
        int parenOpen = 0;
        int parenClosed = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == POWER) {
                formattedInput.append("<sup>");
                if (subOpen == 0) {
                    formattedInput.append("<small>");
                }
                subOpen++;

                if (i + 1 == input.length()) {
                    formattedInput.append(c);
                    if (subClosed == 0) {
                        formattedInput.append("</small>");
                    }

                    formattedInput.append("</sup>");
                    subClosed++;
                } else {
                    formattedInput.append(PLACEHOLDER);
                }

                continue;
            }

            if (subOpen > subClosed) {
                if (parenOpen == parenClosed) {
                    // Decide when to break the <sup> started by ^
                    if (c == PLUS // 2^3+1
                            || (c == MINUS && input.charAt(i - 1) != POWER) // 2^3-1
                            || c == MUL // 2^3*1
                            || c == DIV // 2^3/1
                            || c == EQUAL // X^3=1
                            || (c == LEFT_PAREN && (Character.isDigit(input.charAt(i - 1))
                                    || input.charAt(i - 1) == RIGHT_PAREN)) // 2^3(1) or 2^(3-1)(0)
                            || (Character.isDigit(c) && input.charAt(i - 1)
                                    == RIGHT_PAREN) // 2^(3)1
                            || (!Character.isDigit(c) && Character.isDigit(input.charAt(i - 1)))
                            && c != '.') { // 2^3log(1)
                        while (subOpen > subClosed) {
                            if (subClosed == 0) {
                                formattedInput.append("</small>");
                            }

                            formattedInput.append("</sup>");
                            subClosed++;
                        }

                        subOpen = 0;
                        subClosed = 0;
                        parenOpen = 0;
                        parenClosed = 0;
                        if (c == LEFT_PAREN) {
                            parenOpen--;
                        } else if (c == RIGHT_PAREN) {
                            parenClosed--;
                        }
                    }
                }

                if (c == LEFT_PAREN) {
                    parenOpen++;
                } else if (c == RIGHT_PAREN) {
                    parenClosed++;
                }
            }

            formattedInput.append(c);
        }

        while (subOpen > subClosed) {
            if (subClosed == 0) {
                formattedInput.append("</small>");
            }

            formattedInput.append("</sup>");
            subClosed++;
        }

        return formattedInput.toString();
    }
}
