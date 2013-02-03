package com.android2.calculator3.view;

import java.text.DecimalFormat;

import org.ejml.simple.SimpleMatrix;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.android2.calculator3.Logic;
import com.android2.calculator3.MutableString;
import com.android2.calculator3.R;

public class MatrixView extends TableLayout {
    private static String FORMAT = "#.######";
    int rows, columns = 0;
    AdvancedDisplay parent;

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

    public SimpleMatrix getSimpleMatrix() {
        SimpleMatrix sm = new SimpleMatrix(getData());
        return sm;
    }

    private double[][] getData() {
        double[][] data = new double[rows][columns];
        for(int row = 0; row < rows; row++) {
            TableRow tr = (TableRow) getChildAt(row);
            for(int column = 0; column < columns; column++) {
                String input = ((EditText) tr.getChildAt(column)).getText().toString();
                if(input.startsWith(getResources().getString(R.string.minus))) {
                    if(input.length() == 1) input = "";
                    else input = "-" + input.substring(1);
                }
                if(input.startsWith(".")) {
                    input = "0" + input;
                }
                if(input.startsWith("-.")) {
                    input = "-0" + input.substring(1);
                }
                if(input.isEmpty()) data[row][column] = 0;
                else if(input.equals("-" + Logic.INFINITY_UNICODE)) data[row][column] = Double.NEGATIVE_INFINITY;
                else if(input.equals(Logic.INFINITY_UNICODE)) data[row][column] = Double.POSITIVE_INFINITY;
                else {
                    try {
                        data[row][column] = Double.valueOf(input);
                    }
                    catch(Exception e) {
                        data[row][column] = Double.NaN;
                    }
                }
            }
        }
        return data;
    }

    private String[][] getDataAsString() {
        DecimalFormat formatter = new DecimalFormat(FORMAT);
        String[][] data = new String[rows][columns];
        for(int row = 0; row < rows; row++) {
            TableRow tr = (TableRow) getChildAt(row);
            for(int column = 0; column < columns; column++) {
                String input = ((EditText) tr.getChildAt(column)).getText().toString();
                if(input.isEmpty()) data[row][column] = "";
                else {
                    if(input.startsWith(getResources().getString(R.string.minus))) {
                        if(input.length() == 1) input = "";
                        else input = "-" + input.substring(1);
                    }
                    if(input.startsWith(".")) {
                        input = "0" + input;
                    }
                    if(input.startsWith("-.")) {
                        input = "-0" + input.substring(1);
                    }
                    try {
                        data[row][column] = formatter.format(Double.valueOf(input));
                    }
                    catch(Exception e) {
                        data[row][column] = "";
                    }
                }
            }
        }
        return data;
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
        DecimalFormat formatter = new DecimalFormat(FORMAT);
        int rows = matrix.numRows();
        int columns = matrix.numCols();
        String input = "[";
        for(int i = 0; i < rows; i++) {
            input += "[";
            for(int j = 0; j < columns; j++) {
                input += formatter.format(matrix.get(i, j)) + ",";
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
        parent.addView(mv, pos);

        return true;
    }

    private static boolean verify(MutableString text) {
        return text.startsWith("[[");
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
