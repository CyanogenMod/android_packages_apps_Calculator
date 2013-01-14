package com.android2.calculator3;

import android.content.Context;
import android.widget.TableLayout;

public class MatrixView extends TableLayout {

    public MatrixView(Context context) {
        super(context);
        setup();
    }

    private void setup() {
        setBackgroundResource(R.drawable.matrix_background);
    }

    public void addRow() {

    }

    public void removeRow() {

    }

    public void addColumn() {

    }

    public void removeColumn() {

    }
}
