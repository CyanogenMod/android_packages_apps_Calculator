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

    private int getScrollRange() {
        int scrollRange = 0;
        if(getChildCount() > 0) {
            View child = getChildAt(0);
            scrollRange = Math.max(0, child.getWidth() - (getWidth() - getPaddingLeft() - getPaddingRight()));
        }
        return scrollRange;
    }

    private boolean gravityRight;

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // HorizontalScrollView is broken for Gravity.RIGHT. So we're fixing it.
        int childWidth = getView().getWidth();
        super.onLayout(changed, left, top, right, bottom);
        int delta = getView().getWidth() - childWidth;
        AdvancedDisplay view = getView();
        ScrollableDisplay.LayoutParams p = (LayoutParams) view.getLayoutParams();
        int horizontalGravity = p.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        int verticalGravity = p.gravity & Gravity.VERTICAL_GRAVITY_MASK;
        if(horizontalGravity == Gravity.RIGHT) {
            if(getScrollRange() > 0) {
                gravityRight = true;
                p.gravity = Gravity.LEFT | verticalGravity;
                view.setLayoutParams(p);
                removeViewAt(0);
                addView(view);
                view.getActiveEditText().requestFocus();
                super.onLayout(changed, left, top, right, bottom);
            }
        }
        else if(gravityRight) {
            if(getScrollRange() == 0) {
                gravityRight = false;
                p.gravity = Gravity.RIGHT | verticalGravity;
                view.setLayoutParams(p);
                removeViewAt(0);
                addView(view);
                view.getActiveEditText().requestFocus();
                super.onLayout(changed, left, top, right, bottom);
            }
        }
        if(gravityRight && delta > 0) scrollBy(delta, 0);
    }
}
