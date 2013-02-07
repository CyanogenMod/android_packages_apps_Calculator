package com.android.calculator2.view;

import java.text.DecimalFormat;

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
    private static String FORMAT = "#.######";
    private static DecimalFormat FORMATTER = new DecimalFormat(FORMAT);
    private static final String VALID_MATRIX = "\\[(\\[[\u2212-]?[A-F0-9]*(\\.[A-F0-9]*)?(,[\u2212-]?[A-F0-9]*(\\.[A-F0-9]*)?)*\\])+\\].*";
    public final static String PATTERN = "[[,][,]]";

    private int rows, columns = 0;
    private AdvancedDisplay parent;
    private Logic logic;

    public MatrixView(Context context) {
        super(context);
    }

    public MatrixView(AdvancedDisplay parent) {
        super(parent.getContext());
        this.parent = parent;
        setup();
    }

    private void setup() {
        setBackgroundResource(R.drawable.matrix_background);
        setFocusable(true);
        logic = parent.mLogic;
    }

    public void addRow() {
        rows++;
        TableRow tr = new TableRow(getContext());
        tr.setLayoutParams(new MatrixView.LayoutParams(MatrixView.LayoutParams.WRAP_CONTENT, MatrixView.LayoutParams.WRAP_CONTENT, 1));
        addView(tr);

        for(int i = 0; i < columns; i++) {
            tr.addView(createEditText());
        }
    }

    public void removeRow() {
        rows--;
        removeViewAt(getChildCount() - 1);

        if(rows == 0 || columns == 0) parent.removeView(this);
    }

    public void addColumn() {
        columns++;

        for(int i = 0; i < rows; i++) {
            TableRow tr = (TableRow) getChildAt(i);
            tr.addView(createEditText());
        }
    }

    public void removeColumn() {
        columns--;

        for(int i = 0; i < rows; i++) {
            TableRow tr = (TableRow) getChildAt(i);
            tr.removeViewAt(tr.getChildCount() - 1);
        }

        if(rows == 0 || columns == 0) parent.removeView(this);
    }

    public SimpleMatrix getSimpleMatrix() throws SyntaxException {
        SimpleMatrix sm = new SimpleMatrix(getData());
        return sm;
    }

    private double[][] getData() throws SyntaxException {
        double[][] data = new double[rows][columns];
        for(int row = 0; row < rows; row++) {
            TableRow tr = (TableRow) getChildAt(row);
            for(int column = 0; column < columns; column++) {
                String input = ((EditText) tr.getChildAt(column)).getText().toString();
                input = stringify(input);
                if(input.isEmpty()) throw new SyntaxException();
                try {
                    data[row][column] = Double.valueOf(input);
                }
                catch(Exception e) {
                    data[row][column] = Double.NaN;
                }
            }
        }
        return data;
    }

    private String[][] getDataAsString() {
        String[][] data = new String[rows][columns];
        for(int row = 0; row < rows; row++) {
            TableRow tr = (TableRow) getChildAt(row);
            for(int column = 0; column < columns; column++) {
                String input = ((EditText) tr.getChildAt(column)).getText().toString();
                data[row][column] = input;
            }
        }
        return data;
    }

    private String stringify(String input) {
        if(input.isEmpty()) return "";
        else {
            input = logic.convertToDecimal(input);
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
        for(int row = 0; row < rows; row++) {
            TableRow tr = (TableRow) getChildAt(row);
            for(int column = 0; column < columns; column++) {
                String input = ((EditText) tr.getChildAt(column)).getText().toString();
                if(!input.isEmpty()) empty = false;
            }
        }
        return empty;
    }

    private EditText createEditText() {
        final EditText et = new MatrixEditText(parent, this);
        et.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1));
        return et;
    }

    View nextView(View currentView) {
        boolean foundCurrentView = false;
        for(int row = 0; row < rows; row++) {
            TableRow tr = (TableRow) getChildAt(row);
            for(int column = 0; column < columns; column++) {
                if(foundCurrentView) return tr.getChildAt(column);
                else if(currentView == tr.getChildAt(column)) foundCurrentView = true;
            }
        }
        return parent.getChildAt(parent.getChildIndex(this) + 1);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for(int row = 0; row < rows; row++) {
            TableRow tr = (TableRow) getChildAt(row);
            for(int column = 0; column < columns; column++) {
                tr.getChildAt(column).setEnabled(enabled);
            }
        }
    }

    @Override
    public String toString() {
        String input = "[";
        String[][] data = getDataAsString();
        for(int i = 0; i < rows; i++) {
            input += "[";
            for(int j = 0; j < columns; j++) {
                input += data[i][j] + ",";
            }
            // Remove trailing ,
            input = input.substring(0, input.length() - 1);
            input += "]";
        }
        input += "]";
        return input;
    }

    public static String matrixToString(SimpleMatrix matrix) {
        int rows = matrix.numRows();
        int columns = matrix.numCols();
        String input = "[";
        for(int i = 0; i < rows; i++) {
            input += "[";
            for(int j = 0; j < columns; j++) {
                input += FORMATTER.format(matrix.get(i, j)) + ",";
            }
            // Remove trailing ,
            input = input.substring(0, input.length() - 1);
            input += "]";
        }
        input += "]";
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
        if(!MatrixView.verify(text)) return false;

        String matrix = MatrixView.parseMatrix(text.getText());
        text.setText(text.substring(matrix.length()));
        int rows = MatrixView.countOccurrences(matrix, '[') - 1;
        int columns = MatrixView.countOccurrences(matrix, ',') / rows + 1;

        MatrixView mv = new MatrixView(parent);
        for(int i = 0; i < rows; i++) {
            mv.addRow();
        }
        for(int i = 0; i < columns; i++) {
            mv.addColumn();
        }
        String[] data = matrix.split(",|\\]\\[");
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

    private static boolean verify(MutableString text) {
        return text.getText().matches(VALID_MATRIX);
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
