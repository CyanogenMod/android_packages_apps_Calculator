package com.android.calculator2.view;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.android.calculator2.R;
import com.android.calculator2.view.display.AdvancedDisplay;
import com.android.calculator2.view.display.AdvancedDisplayControls;
import com.android.calculator2.view.display.DisplayComponent;
import com.android.calculator2.view.display.EventListener;
import com.xlythe.math.Solver;

import org.ejml.simple.SimpleMatrix;
import org.javia.arity.SyntaxException;

import java.text.DecimalFormatSymbols;
import java.util.regex.Pattern;

public class MatrixView extends TableLayout implements AdvancedDisplayControls {
    private int mRows, mColumns = 0;
    private EventListener mListener;
    private Solver mSolver;
    private String mSeparator;

    public MatrixView(Context context) {
        super(context);
        setup();
    }

    private void setup() {
        mSeparator = getSeparator();
        setBackgroundResource(R.drawable.matrix_background);
        setFocusable(true);
    }

    private static String getSeparator() {
        return (getDecimal().equals(",") ? " " : ",");
    }

    public static String getPattern() {
        String separator = getSeparator();
        return "[[" + separator + "][" + separator + "]]";
    }

    public static String matrixToString(SimpleMatrix matrix) {
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

    private static boolean verify(String text) {
        String separator = getSeparator();
        String decimal = getDecimal();
        String validMatrix = "\\[(\\[[\u2212-]?[A-F0-9]*(" + Pattern.quote(decimal) + "[A-F0-9]*)?(" + Pattern.quote(separator) + "[\u2212-]?[A-F0-9]*(" + Pattern.quote(decimal) + "[A-F0-9]*)?)*\\])+\\].*";
        return text.matches(validMatrix);
    }

    private static String parseMatrix(String text) {
        int bracket_open = 0;
        int bracket_closed = 0;
        for(int i = 0; i < text.length(); i++) {
            if(text.charAt(i) == '[') {
                bracket_open++;
            } else if(text.charAt(i) == ']') {
                bracket_closed++;
            }
            if(bracket_open == bracket_closed) return text.substring(0, i + 1);
        }
        return "";
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

    public void addColumn() {
        mColumns++;

        for(int i = 0; i < mRows; i++) {
            TableRow tr = (TableRow) getChildAt(i);
            tr.addView(createEditText());
        }
    }

    private static String getDecimal() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        return dfs.getDecimalSeparator()+"";
    }

    private EditText createEditText() {
        final EditText et = new MatrixEditText(this, mListener);
        et.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1));
        return et;
    }

    public void removeRow() {
        mRows--;
        removeViewAt(getChildCount() - 1);

        if(mRows == 0 || mColumns == 0) mListener.onRemoveView(this);
    }

    public void removeColumn() {
        mColumns--;

        for(int i = 0; i < mRows; i++) {
            TableRow tr = (TableRow) getChildAt(i);
            tr.removeViewAt(tr.getChildCount() - 1);
        }

        if(mRows == 0 || mColumns == 0) mListener.onRemoveView(this);
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
                    if(mSolver != null) {
                        data[row][column] = Double.valueOf(stringify(mSolver.solve(input)));
                    }
                    else {
                        data[row][column] = Double.valueOf(stringify(input));
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    data[row][column] = Double.NaN;
                }
            }
        }
        return data;
    }

    private String stringify(String input) {
        if(input.isEmpty()) return "";
        else {
            input = convertToDecimal(input);
            if(input.charAt(0) == '\u2212') {
                if(input.length() == 1) input = "";
                else input = "-" + input.substring(1);
            }
            if(input.startsWith(".")) {
                input = "0" + input;
            } else if(input.startsWith("-.")) {
                input = "-0" + input.substring(1);
            }
            return input;
        }
    }

    String convertToDecimal(String input) {
        if(mSolver != null) {
            try {
                return mSolver.convertToDecimal(input);
            } catch(SyntaxException e) {
                e.printStackTrace();
            }
        }
        return input;
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

    View nextView(View currentView) {
        boolean foundCurrentView = false;
        for(int row = 0; row < mRows; row++) {
            TableRow tr = (TableRow) getChildAt(row);
            for(int column = 0; column < mColumns; column++) {
                if(foundCurrentView) return tr.getChildAt(column);
                else if(currentView == tr.getChildAt(column)) foundCurrentView = true;
            }
        }
        return mListener.nextView(currentView);
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
        return mListener.previousView(currentView);
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
    public boolean hasNext() {
        for(int row = 0; row < mRows; row++) {
            TableRow tr = (TableRow) getChildAt(row);
            for(int column = 0; column < mColumns; column++) {
                String input = ((EditText) tr.getChildAt(column)).getText().toString();
                if(input.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static class MVDisplayComponent implements DisplayComponent {
        @Override
        public View getView(Context context, Solver solver, String equation, EventListener listener) {
            int rows = TextUtil.countOccurrences(equation, '[') - 1;
            int columns = TextUtil.countOccurrences(equation, getSeparator().charAt(0)) / rows + 1;

            MatrixView mv = new MatrixView(context);
            mv.mSolver = solver;
            mv.mListener = listener;
            for(int i = 0; i < rows; i++) {
                mv.addRow();
            }
            for(int i = 0; i < columns; i++) {
                mv.addColumn();
            }
            String[] data = equation.split(Pattern.quote(getSeparator()) + "|\\]\\[");
            for(int order = 0, row = 0; row < rows; row++) {
                TableRow tr = (TableRow) mv.getChildAt(row);
                for(int column = 0; column < columns; column++) {
                    EditText input = (EditText) tr.getChildAt(column);
                    input.setText(data[order].replaceAll("[\\[\\]]", ""));
                    order++;
                }
            }
            AdvancedDisplay.LayoutParams params = new AdvancedDisplay.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER_VERTICAL;
            mv.setLayoutParams(params);

            return mv;
        }

        @Override
        public String parse(String equation) {
            if(MatrixView.verify(equation)) {
                return MatrixView.parseMatrix(equation);
            }
            else {
                return null;
            }
        }
    }
}
