package com.android.calculator2.view.display;

import android.view.View;
import android.widget.EditText;

/**
 * Created by Will on 12/13/2014.
 */
public interface EventListener {
    public void onEditTextChanged(EditText editText);

    public void onRemoveView(View view);

    public View nextView(View currentView);

    public View previousView(View currentView);
}
