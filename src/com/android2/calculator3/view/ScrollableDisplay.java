package com.android2.calculator3.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
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

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // HorizontalScrollView is broken for Gravity.RIGHT. So we're fixing it.
        ScrollableDisplay.LayoutParams p = (LayoutParams) getView().getLayoutParams();
        int horizontalGravity = p.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        if(horizontalGravity == Gravity.RIGHT) {
            // TODO Fix layouts with Gravity.RIGHT
            super.onLayout(changed, left, top, right, bottom);
        }
        else {
            super.onLayout(changed, left, top, right, bottom);
        }
    }
}
