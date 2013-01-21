package com.android2.calculator3.view;

import android.content.Context;
import android.text.Editable;
import android.text.Editable.Factory;
import android.text.method.KeyListener;
import android.widget.EditText;
import android.widget.LinearLayout;

public class MatrixEnabledDisplay extends LinearLayout {
    CalculatorEditText text;

    public MatrixEnabledDisplay(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        text = new CalculatorEditText(context, null);
        text.setSingleLine();
        addView(text);
    }

    public Editable getText() {
        return text.getInput();
    }

    public void clear() {

    }

    public void setText(CharSequence text) {
        this.text.setText(text);
    }

    public void setKeyListener(KeyListener input) {
        text.setKeyListener(input);
    }

    public void setEditableFactory(Factory factory) {
        text.setEditableFactory(factory);
    }

    public EditText getActiveEditText() {
        return text;
    }

    @Override
    public boolean performLongClick() {
        return getActiveEditText().performLongClick();// TODO Copy the
                                                      // copy/paste commands
                                                      // from CalcEditText into
                                                      // here
    }
}
