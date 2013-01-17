package com.android2.calculator3.view;

import org.ejml.simple.SimpleMatrix;

import android.content.Context;
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

        for (int i = 0; i < columns; i++) {
            tr.addView(createEditText());
        }
    }

    public void removeRow() {
        rows--;
        removeViewAt(0);
    }

    public void addColumn() {
        columns++;

        for (int i = 0; i < rows; i++) {
            TableRow tr = (TableRow) getChildAt(i);
            tr.addView(createEditText());
        }
    }

    public void removeColumn() {
        columns--;

        for (int i = 0; i < rows; i++) {
            TableRow tr = (TableRow) getChildAt(i);
            tr.removeViewAt(0);
        }
    }

    public SimpleMatrix getSimpleMatrix() {
        double[][] data = {};
        SimpleMatrix sm = new SimpleMatrix(data);
        return sm;
    }

    private EditText createEditText() {
        EditText et = new EditText(getContext());
        et.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1));
        return et;
    }
}
