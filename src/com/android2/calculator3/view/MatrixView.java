package com.android2.calculator3.view;

import org.ejml.simple.SimpleMatrix;

import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.android2.calculator3.R;

public class MatrixView extends TableLayout {
    int rows, columns = 2;

    public MatrixView(Context context) {
        super(context);
        setup();
    }

    private void setup() {
        setBackgroundResource(R.drawable.matrix_background);
    }

    public void addRow() {
        rows++;
        TableRow tr = new TableRow(getContext());
        tr.setLayoutParams(new MatrixView.LayoutParams(MatrixView.LayoutParams.MATCH_PARENT, MatrixView.LayoutParams.MATCH_PARENT, 1));
        addView(tr);

        for(int i = 0; i < columns; i++) {
            tr.addView(createEditText());
        }
    }

    public void removeRow() {
        rows--;
        removeViewAt(0);
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
                EditText et = (EditText) tr.getChildAt(column);
                data[row][column] = Double.valueOf(et.getText().toString());
            }
        }
        return data;
    }

    private EditText createEditText() {
        EditText et = new EditText(getContext());
        et.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1));
        et.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
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

    public static String load(String text, MatrixEnabledDisplay parent) {
        return text;
    }
}
