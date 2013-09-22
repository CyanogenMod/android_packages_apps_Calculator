package com.android.calculator2.view;

import java.util.regex.Pattern;

import org.ejml.simple.SimpleMatrix;
import org.javia.arity.SyntaxException;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.android.calculator2.Logic;
import com.android.calculator2.MutableString;
import com.android.calculator2.R;

public class MatrixView extends TableLayout {
    private int mRows, mColumns = 0;
    private AdvancedDisplay mParent;
    private Logic mLogic;
    private String mSeparator;

    public MatrixView(Context context) {
        super(context);
    }

    public MatrixView(AdvancedDisplay parent) {
        super(parent.getContext());
        this.mParent = parent;
        setup();
    }

    private void setup() {
        mSeparator = getSeparator(getContext());
        setBackgroundResource(R.drawable.matrix_background);
        setFocusable(true);
        mLogic = mParent.mLogic;
    }

    public static String getPattern(Context context) {
        String separator = getSeparator(context);
        return "[[" + separator + "][" + separator + "]]";
    }

    private static String getSeparator(Context context) {
        return context.getString(R.string.matrix_separator);
    }

    private static String getDecimal(Context context) {
        return context.getString(R.string.dot);
    }

    public void addRow() {
        mRows++;
        TableRow tr = new TableRow(getContext());
        tr.setLayoutParams(new MatrixView.LayoutParams(MatrixView.LayoutParams.WRAP_CONTENT, MatrixView.LayoutParams.WRAP_CONTENT, 1));
        addView(tr);

        for(int i = 0; i < mColumns; i++) {
            tr.addView(createEditText());
        }
    }

    public void removeRow() {
        mRows--;
        removeViewAt(getChildCount() - 1);

        if(mRows == 0 || mColumns == 0) mParent.removeView(this);
    }

    public void addColumn() {
        mColumns++;

        for(int i = 0; i < mRows; i++) {
            TableRow tr = (TableRow) getChildAt(i);
            tr.addView(createEditText());
        }
    }

    public void removeColumn() {
        mColumns--;

        for(int i = 0; i < mRows; i++) {
            TableRow tr = (TableRow) getChildAt(i);
            tr.removeViewAt(tr.getChildCount() - 1);
        }

        if(mRows == 0 || mColumns == 0) mParent.removeView(this);
    }

    public SimpleMatrix getSimpleMatrix() throws SyntaxException {
        SimpleMatrix sm = new SimpleMatrix(getData());
        return sm;
    }

    private double[][] getData() throws SyntaxException {
        double[][] data = new double[mRows][mColumns];
        for(int row = 0; row < mRows; row++) {
            TableRow tr = (TableRow) getChildAt(row);
            for(int column = 0; column < mColumns; column++) {
                String input = ((EditText) tr.getChildAt(column)).getText().toString();
                if(input.isEmpty()) throw new SyntaxException();
                try {
                    data[row][column] = Double.valueOf(stringify(mLogic.evaluate(input)));
                }
                catch(Exception e) {
                    e.printStackTrace();
                    data[row][column] = Double.NaN;
                }
            }
        }
        return data;
    }

    private String[][] getDataAsString() {
        String[][] data = new String[mRows][mColumns];
        for(int row = 0; row < mRows; row++) {
            TableRow tr = (TableRow) getChildAt(row);
            for(int column = 0; column < mColumns; column++) {
                String input = ((EditText) tr.getChildAt(column)).getText().toString();
                data[row][column] = input;
            }
        }
        return data;
    }

    private String stringify(String input) {
        if(input.isEmpty()) return "";
        else {
            input = mLogic.convertToDecimal(input);
            if(input.charAt(0) == '\u2212') {
                if(input.length() == 1) input = "";
                else input = "-" + input.substring(1);
            }
            if(input.startsWith(".")) {
                input = "0" + input;
            }
            else if(input.startsWith("-.")) {
                input = "-0" + input.substring(1);
            }
            return input;
        }
    }

    boolean isEmpty() {
        boolean empty = true;
        for(int row = 0; row < mRows; row++) {
            TableRow tr = (TableRow) getChildAt(row);
            for(int column = 0; column < mColumns; column++) {
                String input = ((EditText) tr.getChildAt(column)).getText().toString();
                if(!input.isEmpty()) empty = false;
            }
        }
        return empty;
    }

    private EditText createEditText() {
        final EditText et = new MatrixEditText(mParent, this);
        et.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1));
        return et;
    }

    View nextView(View currentView) {
        boolean foundCurrentView = false;
        for(int row = 0; row < mRows; row++) {
            TableRow tr = (TableRow) getChildAt(row);
            for(int column = 0; column < mColumns; column++) {
                if(foundCurrentView) return tr.getChildAt(column);
                else if(currentView == tr.getChildAt(column)) foundCurrentView = true;
            }
        }
        return mParent.getChildAt(mParent.getChildIndex(this) + 1);
    }

    View previousView(View currentView) {
        boolean foundCurrentView = false;
        for(int row = mRows - 1; row >= 0; row--) {
            TableRow tr = (TableRow) getChildAt(row);
            for(int column = mColumns - 1; column >= 0; column--) {
                if(foundCurrentView) return tr.getChildAt(column);
                else if(currentView == tr.getChildAt(column)) foundCurrentView = true;
            }
        }
        return mParent.getChildAt(mParent.getChildIndex(this) - 1);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for(int row = 0; row < mRows; row++) {
            TableRow tr = (TableRow) getChildAt(row);
            for(int column = 0; column < mColumns; column++) {
                tr.getChildAt(column).setEnabled(enabled);
            }
        }
    }

    @Override
    public String toString() {
        String input = "[";
        String[][] data = getDataAsString();
        for(int i = 0; i < mRows; i++) {
            input += "[";
            for(int j = 0; j < mColumns; j++) {
                input += data[i][j] + mSeparator;
            }
            // Remove trailing ,
            input = input.substring(0, input.length() - 1);
            input += "]";
        }
        input += "]";
        return input;
    }

    public static String matrixToString(SimpleMatrix matrix, Logic logic) throws SyntaxException {
        int rows = matrix.numRows();
        int columns = matrix.numCols();
        String input = "[";
        for(int i = 0; i < rows; i++) {
            input += "[";
            for(int j = 0; j < columns; j++) {
                input += strip(Double.toString(matrix.get(i, j))) + ",";
            }
            // Remove trailing ,
            input = input.substring(0, input.length() - 1);
            input += "]";
        }
        input += "]";
        return input;
    }

    private static String strip(String input) {
        if(input.endsWith(".0")) return input.substring(0, input.length() - 2);
        return input;
    }

    public static boolean load(final MutableString text, final AdvancedDisplay parent) {
        boolean changed = MatrixView.load(text, parent, parent.getChildCount());
        if(changed) {
            // Always append a trailing EditText
            CalculatorEditText.load(parent);
        }
        return changed;
    }

    public static boolean load(final MutableString text, final AdvancedDisplay parent, final int pos) {
        if(!MatrixView.verify(parent.getContext(), text)) return false;

        String matrix = MatrixView.parseMatrix(text.getText());
        text.setText(text.substring(matrix.length()));
        int rows = MatrixView.countOccurrences(matrix, '[') - 1;
        int columns = MatrixView.countOccurrences(matrix, getSeparator(parent.getContext()).charAt(0)) / rows + 1;

        MatrixView mv = new MatrixView(parent);
        for(int i = 0; i < rows; i++) {
            mv.addRow();
        }
        for(int i = 0; i < columns; i++) {
            mv.addColumn();
        }
        String[] data = matrix.split(Pattern.quote(getSeparator(parent.getContext())) + "|\\]\\[");
        for(int order = 0, row = 0; row < rows; row++) {
            TableRow tr = (TableRow) mv.getChildAt(row);
            for(int column = 0; column < columns; column++) {
                EditText input = (EditText) tr.getChildAt(column);
                input.setText(data[order].replaceAll("[\\[\\]]", ""));
                order++;
            }
        }
        AdvancedDisplay.LayoutParams params = new AdvancedDisplay.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        mv.setLayoutParams(params);
        parent.addView(mv, pos);

        return true;
    }

    private static boolean verify(Context context, MutableString text) {
        String separator = getSeparator(context);
        String decimal = getDecimal(context);
        String validMatrix = "\\[(\\[[\u2212-]?[A-F0-9]*(" + Pattern.quote(decimal) + "[A-F0-9]*)?(" + Pattern.quote(separator) + "[\u2212-]?[A-F0-9]*("
                + Pattern.quote(decimal) + "[A-F0-9]*)?)*\\])+\\].*";
        return text.getText().matches(validMatrix);
    }

    private static int countOccurrences(String haystack, char needle) {
        int count = 0;
        for(int i = 0; i < haystack.length(); i++) {
            if(haystack.charAt(i) == needle) {
                count++;
            }
        }
        return count;
    }

    private static String parseMatrix(String text) {
        int bracket_open = 0;
        int bracket_closed = 0;
        for(int i = 0; i < text.length(); i++) {
            if(text.charAt(i) == '[') {
                bracket_open++;
            }
            else if(text.charAt(i) == ']') {
                bracket_closed++;
            }
            if(bracket_open == bracket_closed) return text.substring(0, i + 1);
        }
        return "";
    }
}
