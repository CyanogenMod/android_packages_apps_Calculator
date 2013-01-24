package com.android2.calculator3.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

public class ScrollableDisplay extends HorizontalScrollView {
    public ScrollableDisplay(Context context, AttributeSet attrs) {
        super(context, attrs);
        addView(new MatrixEnabledDisplay(context, attrs));
    }

    public MatrixEnabledDisplay getView() {
        return (MatrixEnabledDisplay) getChildAt(0);
    }
}
