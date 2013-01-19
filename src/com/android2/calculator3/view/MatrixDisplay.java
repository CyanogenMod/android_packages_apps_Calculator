package com.android2.calculator3.view;

import android.content.Context;
import android.text.Editable;
import android.widget.LinearLayout;

public class MatrixDisplay extends LinearLayout {
    public MatrixDisplay(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
    }

    public Editable getText() {
        return null;
    }

    public void insert(String input) {

    }

    public void clear() {

    }

    public void setText(String input) {
        clear();
        insert(input);
    }
}
