package com.android2.calculator3.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.HorizontalScrollView;

public class ScrollableDisplay extends HorizontalScrollView implements OnLongClickListener {
    public ScrollableDisplay(Context context, AttributeSet attrs) {
        super(context, attrs);
        addView(new AdvancedDisplay(context));
        setOnLongClickListener(this);
    }

    public AdvancedDisplay getView() {
        return (AdvancedDisplay) getChildAt(0);
    }

    @Override
    public boolean onLongClick(View v) {
        return getView().performLongClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        return false;
    }
}
