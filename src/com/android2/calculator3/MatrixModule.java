package com.android2.calculator3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ejml.simple.SimpleEVD;
import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;
import org.javia.arity.SyntaxException;

import android.content.Context;

import com.android2.calculator3.BaseModule.Mode;
import com.android2.calculator3.view.AdvancedDisplay;
import com.android2.calculator3.view.MatrixView;

public class MatrixModule {
    Logic mLogic;
    Context mContext;

    MatrixModule(Logic logic, Context context) {
        mLogic = logic;
        mContext = context.getApplicationContext();
    }

    static SimpleMatrix addScalar(SimpleMatrix mat, double scalar) {
        SimpleMatrix temp = mat.copy();
        int M = mat.numRows();
        int N = mat.numCols();
        for(int i = 0; i < M; i++) {
            for(int j = 0; j < N; j++)
                temp.set(i, j, mat.get(i, j) + scalar);
        }
        return temp;
    }

    double gatherScalar(String text) throws SyntaxException {
        if(!Character.isDigit(text.charAt(1))) throw new SyntaxException();
        return Double.parseDouble(text.substring(1));
    }

    private String calculate(String input) throws SyntaxException {
        input = mLogic.localize(input);

        // I never realized negative numbers could be so difficult.
        input = input.replaceAll("(\\d+(?:\\.\\d+)?|\\[.+\\]|\\^)\u2212(\\d+(?:\\.\\d+)?)", "$1-$2");
        // All remaining instances of U+2212 will be on negative numbers.
        // They will be counted as whole tokens.

        // Instantiate matrices first.
        Matcher m = Pattern.compile("\\[\\[.+\\]\\]").matcher(input);
        while(m.find()) {
            SimpleMatrix temp = parseMatrix(m.group());
            input = input.replace(m.group(), MatrixView.matrixToString(mContext, temp, mLogic));
        }

        // Get percentage.
        input = input.replaceAll("(?<=\\d)%(?!\\d)", "\u00d70.01");

        // Might as well get factorial too.
        m = Pattern.compile("(?<!\\.)([0-9]+)\\!").matcher(input);
        while(m.find()) {
            int temp = Integer.parseInt(m.group(1));
            input = input.replace(m.group(), fact(temp));
        }

        // How to handle parens? Recursion.
        int open = 0;
        int close = 0;
        int lastOpen = 0;
        for(int i = 0; i < input.length(); i++) {
            if(input.charAt(i) == '(') {
                // if(close != 0) throw new Exception();
                open++;
                if(lastOpen == 0) lastOpen = i;
            }
            else if(input.charAt(i) == ')') {
                if(open == 0) throw new SyntaxException();
                close++;
                if((close == open) && open != 0) {
                    // Balanced means one whole set
                    StringBuffer temp = new StringBuffer(input);
                    String res = calculate(temp.substring(lastOpen + 1, i).toString());
                    temp.replace(lastOpen, i + 1, res);
                    input = temp.toString();
                }
                else if(close >= open) throw new SyntaxException();
            }
            if((i == input.length() - 1) && (open != close)) input = input.concat(")");// Auto-close
                                                                                       // if
                                                                                       // at
                                                                                       // the
                                                                                       // end
                                                                                       // of
                                                                                       // the
                                                                                       // string
        }
        if(open != close) throw new SyntaxException();

        // Process transpositions.
        Matcher match = Pattern.compile("(\\[.+\\])\\^T").matcher(input);
        while(match.find()) {
            SimpleMatrix temp = parseMatrix(match.group(1)).transpose();
            input = input.replace(match.group(), MatrixView.matrixToString(mContext, temp, mLogic));
        }

        // Process inverses
        match = Pattern.compile("(\\[.+\\])\uFEFF\\^-1").matcher(input);
        while(match.find()) {
            SimpleMatrix temp = parseMatrix(match.group(1)).pseudoInverse();
            input = input.replace(match.group(), MatrixView.matrixToString(mContext, temp, mLogic));
        }

        // Handle functions.
        match = Pattern.compile("(\u221a|log|ln|sin\\^-1|cos\\^-1|tan\\^-1|sin|cos|tan|det)(\u2212?\\d+(?:\\.\\d+)?|\\[\\[.+\\]\\])").matcher(input);
        while(match.find()) {
            String res = applyFunc(match.group(1), match.group(2));
            input = input.replace(match.group(), res);
        }

        // Functions might generate NaN. Return error if so.
        if(input.contains("NaN")) return mLogic.mErrorString;

        // Substitute e
        input = input.replaceAll("(?<!\\d)e", "2.7182818284590452353");
        input = input.replaceAll("\\d?(e)(?!\\d|\\+|-)", "\u00d72.7182818284590452353");
        // Sub pi
        input = input.replace("\u03c0", "3.141592653589");

        // Split into seperate arrays of operators and operands.
        // Operator 0 applies to operands 0 and 1, and so on
        String[] parts = input.split("\u00d7|\\+|(?<=\\d|\\])(?<!e)-|\u00f7|\\^");
        char[] ops = opSplit(input);

        // This never changes, so no need to keep calling it
        int N = ops.length;

        // If there are no ops, there's nothing to do
        // Since we've already made substitutions and parsed parentheses
        if(N == 0) return input;

        // Fill in the pieces.
        // Store everything as Object, and cast out later
        Object[] pieces = new Object[parts.length];
        for(int i = 0; i < parts.length; i++) {
            if(parts[i].startsWith("[[")) pieces[i] = parseMatrix(parts[i]);
            else pieces[i] = Double.parseDouble(parts[i].replace('\u2212', '-'));
        }

        // Work on the operators in order of their precedence.
        for(int i = 0; i < N; i++) {
            int[] landr = null;
            if(ops[i] == '^') {
                landr = lookAfield(pieces, i);
                int l = landr[0];
                int r = landr[1];
                Object res = applyPow(pieces[l], pieces[r]);

                pieces[l] = res;// This is arbitrary (I think)
                pieces[r] = null;// I could also have put the result in right
                // and null in left
            }
        }

        // Yes, I'm doing a complete loop over all operators several times.
        // Realistically, there will only be a few of them.
        // For the purposes of this app, it's no big deal.
        for(int i = 0; i < N; i++) {
            int[] landr = null;
            if(ops[i] == '\u00d7' || ops[i] == '\u00f7' || ops[i] == '%') {
                landr = lookAfield(pieces, i);
                int l = landr[0];
                int r = landr[1];
                Object res = null;
                if(ops[i] == '\u00d7') res = applyMult(pieces[l], pieces[r]);
                else if(ops[i] == '\u00f7') res = applyDiv(pieces[l], pieces[r]);
                else res = applyMod(pieces[l], pieces[r]);

                pieces[l] = res;
                pieces[r] = null;
            }
        }

        for(int i = 0; i < N; i++) {
            int[] landr = null;
            if(ops[i] == '+' || ops[i] == '-') {
                landr = lookAfield(pieces, i);
                int l = landr[0];
                int r = landr[1];
                Object res = null;
                if(ops[i] == '+') res = applyPlus(pieces[l], pieces[r]);
                else res = applySub(pieces[l], pieces[r]);

                pieces[l] = res;
                pieces[r] = null;
            }
        }

        for(Object piece : pieces)
            if(piece != null) {
                if(piece instanceof Double) return numToString((Double) piece);
                else if(piece instanceof SimpleMatrix) return MatrixView.matrixToString(mContext, (SimpleMatrix) piece, mLogic);
                else throw new SyntaxException(); // Neither matrix nor double
                                                  // should never happen
            }
        throw new RuntimeException(); // Getting here should be impossible
    }// end main

    String evaluateMatrices(AdvancedDisplay display) throws SyntaxException {
        String text = display.getText();
        text = mLogic.convertToDecimal(text);
        String result = "";
        try {
            result = calculate(text).replace('-', Logic.MINUS);// Back to fancy
                                                               // minus
        }
        catch(Exception e) {
            result = mLogic.mErrorString;
        }

        result = mLogic.relocalize(result);
        return mLogic.mBaseModule.updateTextToNewMode(result, Mode.DECIMAL, mLogic.mBaseModule.getMode());
    }

    private static char[] opSplit(String str) {
        StringBuilder buffer = new StringBuilder();
        char c, prev;
        prev = str.charAt(0);
        for(int i = 0; i < str.length(); i++) {
            c = str.charAt(i);
            if(c == '^' || c == Logic.MUL || c == Logic.DIV || c == '+') buffer.append(c);
            else if(c == '-' && (Character.isDigit(prev) || prev == ']') && (prev != 'e')) buffer.append(c);
            prev = c;
        }

        return buffer.toString().toCharArray();
    }

    // Look for the nearest valid operand
    private static int[] lookAfield(Object[] field, int orig) {
        int left, right;

        // Start with the original position (of the operator)
        // Left operand is at the same index as its operator
        // But if it's null, look farther left
        int pos = orig;
        while(field[pos] == null)
            //
            pos--;
        left = pos;
        // Right operand is one greater than the operator index
        pos = orig + 1;
        while(field[pos] == null)
            // Look to the right if null
            pos++;
        right = pos;// Found it

        return new int[] { left, right };// Return the indices to allow later
                                         // sub-in of null
    }

    private static String fact(int n) {
        long m = n;
        for(int i = n - 1; i > 1; i--)
            m *= i;

        return Long.toString(m);
    }

    // The following have a lot of repeated boilerplate code.
    // Condensing it down would require language features/properties
    // that Java does not have.
    // In short, Java is not F#.

    private String applyFunc(String func, String arg) throws SyntaxException {
        arg = arg.replace('\u2212', '-');
        if(func.equals("\u221a"))// sqrt
        {
            if(arg.startsWith("[[")) {
                SimpleMatrix matrix = parseMatrix(arg);
                int m = matrix.numRows();
                int n = matrix.numCols();
                if(m != n) throw new SyntaxException();
                SimpleEVD decomp = new SimpleEVD(matrix.getMatrix());
                double[] evals = new double[m];
                for(int i1 = 0; i1 < m; i1++) {
                    evals[i1] = Math.sqrt(decomp.getEigenvalue(i1).getMagnitude());
                }
                SimpleMatrix D = SimpleMatrix.diag(evals);
                SimpleMatrix V = new SimpleMatrix(m, n);
                for(int k = 0; k < m; k++) {
                    SimpleMatrix col = decomp.getEigenVector(k);
                    for(int l = 0; l < n; l++) {
                        V.set(k, l, col.get(l, 0));
                    }
                }
                SimpleMatrix temp = V.mult(D);
                temp = temp.mult(V.invert());
                return MatrixView.matrixToString(mContext, temp, mLogic);
            }
            else return numToString(Math.sqrt(Double.parseDouble(arg)));
        }
        else if(func.equals("sin")) {
            if(arg.startsWith("[[")) {
                SimpleMatrix m = parseMatrix(arg);
                for(int i = 0; i < m.numRows(); i++)
                    for(int j = 0; j < m.numCols(); j++)
                        m.set(i, j, Math.sin(m.get(i, j)));
                return MatrixView.matrixToString(mContext, m, mLogic);
            }
            else return numToString(Math.sin(Double.parseDouble(arg)));
        }
        else if(func.equals("cos")) {
            if(arg.startsWith("[[")) {
                SimpleMatrix m = parseMatrix(arg);
                for(int i = 0; i < m.numRows(); i++)
                    for(int j = 0; j < m.numCols(); j++)
                        m.set(i, j, Math.cos(m.get(i, j)));
                return MatrixView.matrixToString(mContext, m, mLogic);
            }
            else return numToString(Math.cos(Double.parseDouble(arg)));
        }
        else if(func.equals("tan")) {
            if(arg.startsWith("[[")) {
                SimpleMatrix m = parseMatrix(arg);
                for(int i = 0; i < m.numRows(); i++)
                    for(int j = 0; j < m.numCols(); j++)
                        m.set(i, j, Math.tan(m.get(i, j)));
                return MatrixView.matrixToString(mContext, m, mLogic);
            }
            else return numToString(Math.tan(Double.parseDouble(arg)));
        }
        else if(func.equals("log")) {
            if(arg.startsWith("[[")) {
                SimpleMatrix m = parseMatrix(arg);
                for(int i = 0; i < m.numRows(); i++)
                    for(int j = 0; j < m.numCols(); j++)
                        m.set(i, j, Math.log10(m.get(i, j)));
                return MatrixView.matrixToString(mContext, m, mLogic);
            }
            else return numToString(Math.log10(Double.parseDouble(arg)));
        }
        else if(func.equals("ln")) {
            if(arg.startsWith("[[")) {
                SimpleMatrix m = parseMatrix(arg);
                for(int i = 0; i < m.numRows(); i++)
                    for(int j = 0; j < m.numCols(); j++)
                        m.set(i, j, Math.log(m.get(i, j)));
                return MatrixView.matrixToString(mContext, m, mLogic);
            }
            else return numToString(Math.log(Double.parseDouble(arg)));
        }
        else if(func.equals("sin^-1")) {
            if(arg.startsWith("[[")) {
                SimpleMatrix m = parseMatrix(arg);
                for(int i = 0; i < m.numRows(); i++)
                    for(int j = 0; j < m.numCols(); j++)
                        m.set(i, j, Math.asin(m.get(i, j)));
                return MatrixView.matrixToString(mContext, m, mLogic);
            }
            else return numToString(Math.asin(Double.parseDouble(arg)));
        }
        else if(func.equals("cos^-1")) {
            if(arg.startsWith("[[")) {
                SimpleMatrix m = parseMatrix(arg);
                for(int i = 0; i < m.numRows(); i++)
                    for(int j = 0; j < m.numCols(); j++)
                        m.set(i, j, Math.acos(m.get(i, j)));
                return MatrixView.matrixToString(mContext, m, mLogic);
            }
            else return numToString(Math.acos(Double.parseDouble(arg)));
        }
        else if(func.equals("tan^-1")) {
            if(arg.startsWith("[[")) {
                SimpleMatrix m = parseMatrix(arg);
                for(int i = 0; i < m.numRows(); i++)
                    for(int j = 0; j < m.numCols(); j++)
                        m.set(i, j, Math.atan(m.get(i, j)));
                return MatrixView.matrixToString(mContext, m, mLogic);
            }
            else return numToString(Math.atan(Double.parseDouble(arg)));
        }
        else if(func.equals("det")) {
            if(arg.startsWith("[[")) {
                SimpleMatrix m = parseMatrix(arg);
                if(m.numCols() != m.numRows()) throw new SyntaxException();
                double d = m.determinant();
                return numToString(d);
            }
            else return arg; // Determinant of a scalar is equivalent to det. of
                             // 1x1 matrix, which is the matrix' one element
        }
        else throw new SyntaxException();
    }

    private Object applyPow(Object l, Object r) throws SyntaxException {
        if(l instanceof SimpleMatrix && r instanceof SimpleMatrix) throw new SyntaxException();
        else if(l instanceof SimpleMatrix) {
            SimpleMatrix a = (SimpleMatrix) l;
            int m = a.numRows();
            int n = a.numCols();
            if(m != n) throw new SyntaxException();
            double b = (Double) r;
            if(b > Math.floor(b)) {
                SimpleSVD decomp = new SimpleSVD(a.getMatrix(), false);
                SimpleMatrix S = decomp.getW();
                for(int i1 = 0; i1 < m; i1++) {
                    for(int j = 0; j < n; j++) {
                        double arg = S.get(i1, j);
                        S.set(i1, j, Math.pow(arg, b));
                    }
                }
                SimpleMatrix matrix = decomp.getU().mult(S);
                matrix = matrix.mult(decomp.getV().transpose());
                return matrix;
            }
            else {
                long equiv = Math.round(b);
                for(long e = 1; e < equiv; e++)
                    a = a.mult(a);

                return a;
            }
        }
        else if(r instanceof SimpleMatrix) {
            SimpleMatrix a = (SimpleMatrix) r;
            int m = a.numRows();
            int n = a.numCols();
            if(m != n) throw new SyntaxException();
            double b = (Double) l;
            if(b > Math.floor(b)) {
                SimpleSVD decomp = new SimpleSVD(a.getMatrix(), false);
                SimpleMatrix S = decomp.getW();
                for(int i1 = 0; i1 < m; i1++) {
                    for(int j = 0; j < n; j++) {
                        double arg = S.get(i1, j);
                        S.set(i1, j, Math.pow(arg, b));
                    }
                }
                SimpleMatrix matrix = decomp.getU().mult(S);
                matrix = matrix.mult(decomp.getV().transpose());
                return matrix;
            }
            else {
                long equiv = Math.round(b);
                for(long e = 1; e < equiv; e++)
                    a = a.mult(a);

                return a;
            }
        }
        else {
            double a = (Double) l;
            double b = (Double) r;
            return Math.pow(a, b);
        }
    }

    private Object applyMult(Object l, Object r) throws SyntaxException {
        if(l instanceof SimpleMatrix && r instanceof SimpleMatrix) {
            SimpleMatrix a = (SimpleMatrix) l;
            SimpleMatrix b = (SimpleMatrix) r;
            return a.mult(b);
        }
        else if(l instanceof SimpleMatrix) {
            SimpleMatrix a = (SimpleMatrix) l;
            double b = (Double) r;
            return a.scale(b);
        }
        else if(r instanceof SimpleMatrix) {
            SimpleMatrix a = (SimpleMatrix) r;
            double b = (Double) l;
            return a.scale(b);
        }
        else {
            double a = (Double) l;
            double b = (Double) r;
            return a * b;
        }
    }

    private Object applyDiv(Object l, Object r) throws SyntaxException {
        if(l instanceof SimpleMatrix && r instanceof SimpleMatrix) {
            SimpleMatrix a = (SimpleMatrix) l;
            SimpleMatrix b = (SimpleMatrix) r;
            return a.mult(b.pseudoInverse());
        }
        else if(l instanceof SimpleMatrix) {
            SimpleMatrix a = (SimpleMatrix) l;
            double b = (Double) r;
            return a.scale(1.0 / b);
        }
        else if(r instanceof SimpleMatrix) {
            SimpleMatrix a = (SimpleMatrix) r;
            double b = (Double) l;
            return a.pseudoInverse().scale(b);
        }
        else {
            double a = (Double) l;
            double b = (Double) r;
            return a / b;
        }
    }

    private Object applyPlus(Object l, Object r) throws SyntaxException {
        if(l instanceof SimpleMatrix && r instanceof SimpleMatrix) {
            SimpleMatrix a = (SimpleMatrix) l;
            SimpleMatrix b = (SimpleMatrix) r;
            return a.plus(b);
        }
        else if(l instanceof SimpleMatrix) {
            SimpleMatrix a = (SimpleMatrix) l;
            double b = (Double) r;
            return addScalar(a, b);
        }
        else if(r instanceof SimpleMatrix) {
            SimpleMatrix a = (SimpleMatrix) r;
            double b = (Double) l;
            return addScalar(a, b);
        }
        else {
            double a = (Double) l;
            double b = (Double) r;
            return a + b;
        }
    }

    private Object applySub(Object l, Object r) throws SyntaxException {
        if(l instanceof SimpleMatrix && r instanceof SimpleMatrix) {
            SimpleMatrix a = (SimpleMatrix) l;
            SimpleMatrix b = (SimpleMatrix) r;
            return a.minus(b);
        }
        else if(l instanceof SimpleMatrix) {
            SimpleMatrix a = (SimpleMatrix) l;
            double b = (Double) r;
            return addScalar(a, -b);
        }
        else if(r instanceof SimpleMatrix) {
            SimpleMatrix a = (SimpleMatrix) r;
            double b = (Double) l;
            return addScalar(a, -b);
        }
        else {
            double a = (Double) l;
            double b = (Double) r;
            return a - b;
        }
    }

    private Object applyMod(Object object, Object object2) throws SyntaxException {
        if(object instanceof Double && object2 instanceof Double) {
            double arg1 = (Double) object;
            double arg2 = (Double) object2;
            return arg1 % arg2;
        }
        else throw new SyntaxException();
    }

    private SimpleMatrix parseMatrix(String text) throws SyntaxException {
        // Count rows & cols
        String interior = text.substring(2, text.length() - 2);
        String[] rows = interior.split("\\]\\[");

        SimpleMatrix temp = new SimpleMatrix(rows.length, rows[0].split(",").length);

        for(int i = 0; i < rows.length; i++) {
            String[] cols = rows[i].split(",");
            if(cols.length == 0) throw new SyntaxException();
            for(int j = 0; j < cols.length; j++) {
                if(cols[j].isEmpty()) throw new SyntaxException();
                temp.set(i, j, Double.parseDouble(calculate(cols[j])));
            }
        }

        return temp;
    }

    private static String numToString(double arg) {
        if(Math.floor(arg) < arg) // Thus arg has non-zero fractional part
        return Double.toString(arg);
        else return Long.toString(Math.round(arg));
    }
}
