package com.android2.calculator3.view;

import org.ejml.simple.SimpleMatrix;

import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.android2.calculator3.MutableString;
import com.android2.calculator3.R;

public class MatrixView extends TableLayout {
    int rows, columns = 0;
    MatrixEnabledDisplay parent;

    public MatrixView(MatrixEnabledDisplay parent) {
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
        removeViewAt(0);

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
            tr.removeViewAt(0);
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
                if(input.isEmpty()) data[row][column] = 0;
                else {
                    data[row][column] = Double.valueOf(input);
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

    @Override
    public String toString() {
        String input = "[";
        double[][] data = getData();
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

    public static boolean load(final MutableString text, final MatrixEnabledDisplay parent) {
        boolean changed = MatrixView.load(text, parent, parent.getChildCount());
        if(changed) {
            // Always append a trailing EditText
            CalculatorEditText.load("", parent);
        }
        return changed;
    }

    public static boolean load(final MutableString text, final MatrixEnabledDisplay parent, final int pos) {
        if(!MatrixView.verify(text)) return false;

        String matrix = MatrixView.parseMatrix(text.getText());
        text.setText(text.substring(matrix.length() + 1));
        int rows = MatrixView.countOccurrences(matrix, '[') - 1;
        int columns = MatrixView.countOccurrences(matrix, ',') / rows + 1;

        MatrixView mv = new MatrixView(parent);
        for(int i = 0; i < rows; i++) {
            mv.addRow();
        }
        for(int i = 0; i < columns; i++) {
            mv.addColumn();
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
            if(bracket_open == bracket_closed) return text.substring(0, i);
        }
        return "";
    }
}
