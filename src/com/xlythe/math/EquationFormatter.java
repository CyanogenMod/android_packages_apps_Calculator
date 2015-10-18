package com.xlythe.math;

public class EquationFormatter {

    /**
     * Append parenthesis at the end of unclosed functions
     *
     * ie. sin(90 becomes sin(90)
     * */
    public String appendParenthesis(String input) {
        final StringBuilder formattedInput = new StringBuilder(input);

        int unclosedParen = 0;
        for(int i = 0; i < formattedInput.length(); i++) {
            if(formattedInput.charAt(i) == Constants.LEFT_PAREN) unclosedParen++;
            else if(formattedInput.charAt(i) == Constants.RIGHT_PAREN) unclosedParen--;
        }
        for(int i = 0; i < unclosedParen; i++) {
            formattedInput.append(Constants.RIGHT_PAREN);
        }
        return formattedInput.toString();
    }

    /**
     * Insert html superscripts so that exponents appear properly.
     *
     * ie. 2^3 becomes 2<sup>3</sup>
     * */
    public String insertSupScripts(String input) {
        final StringBuilder formattedInput = new StringBuilder();

        int sub_open = 0;
        int sub_closed = 0;
        int paren_open = 0;
        int paren_closed = 0;
        for(int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if(c == Constants.POWER) {
                formattedInput.append("<sup>");
                if(sub_open == 0) formattedInput.append("<small>");
                sub_open++;
                if(i + 1 == input.length()) {
                    formattedInput.append(c);
                    if(sub_closed == 0) formattedInput.append("</small>");
                    formattedInput.append("</sup>");
                    sub_closed++;
                } else {
                    formattedInput.append(Constants.PLACEHOLDER);
                }
                continue;
            }

            if(sub_open > sub_closed) {
                if(paren_open == paren_closed) {
                    // Decide when to break the <sup> started by ^
                    if(c == Constants.PLUS // 2^3+1
                            || (c == Constants.MINUS && input.charAt(i - 1) != Constants.POWER) // 2^3-1
                            || c == Constants.MUL // 2^3*1
                            || c == Constants.DIV // 2^3/1
                            || c == Constants.EQUAL // X^3=1
                            || (c == Constants.LEFT_PAREN && (Solver.isDigit(input.charAt(i - 1)) || input.charAt(i - 1) == Constants.RIGHT_PAREN)) // 2^3(1)
                            // or
                            // 2^(3-1)(0)
                            || (Solver.isDigit(c) && input.charAt(i - 1) == Constants.RIGHT_PAREN) // 2^(3)1
                            || (!Solver.isDigit(c) && Solver.isDigit(input.charAt(i - 1))) && c != Constants.DECIMAL_POINT) { // 2^3log(1)
                        while(sub_open > sub_closed) {
                            if(sub_closed == 0) formattedInput.append("</small>");
                            formattedInput.append("</sup>");
                            sub_closed++;
                        }
                        sub_open = 0;
                        sub_closed = 0;
                        paren_open = 0;
                        paren_closed = 0;
                        if(c == Constants.LEFT_PAREN) {
                            paren_open--;
                        } else if(c == Constants.RIGHT_PAREN) {
                            paren_closed--;
                        }
                    }
                }
                if(c == Constants.LEFT_PAREN) {
                    paren_open++;
                } else if(c == Constants.RIGHT_PAREN) {
                    paren_closed++;
                }
            }
            formattedInput.append(c);
        }
        while(sub_open > sub_closed) {
            if(sub_closed == 0) formattedInput.append("</small>");
            formattedInput.append("</sup>");
            sub_closed++;
        }
        return formattedInput.toString();
    }

    /**
     * Add comas to an equation or result
     *
     * 12345 becomes 12,345
     *
     * 10101010 becomes 1010 1010
     *
     * ABCDEF becomes AB CD EF
     * */
    public String addComas(Solver solver, String text) {
        return addComas(solver, text, -1).replace(BaseModule.SELECTION_HANDLE+"", "");
    }


    /**
     * Add comas to an equation or result.
     * A temp character (BaseModule.SELECTION_HANDLE) will be added
     * where the selection handle should be.
     *
     * 12345 becomes 12,345
     *
     * 10101010 becomes 1010 1010
     *
     * ABCDEF becomes AB CD EF
     * */
    public String addComas(Solver solver, String text, int selectionHandle) {
        return solver.getBaseModule().groupSentence(text, selectionHandle);
    }

    public String format(Solver solver, String text) {
        return appendParenthesis(insertSupScripts(addComas(solver, text)));
    }
}
