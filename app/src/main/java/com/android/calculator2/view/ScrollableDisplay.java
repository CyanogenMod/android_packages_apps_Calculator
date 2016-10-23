package com.android.calculator2.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.HorizontalScrollView;

import com.android.calculator2.R;

public class ScrollableDisplay extends HorizontalScrollView {
    private int mMaxHeight;
    private boolean gravityRight = false;
    private boolean autoScrolling = false;

    public ScrollableDisplay(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollableDisplay(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ScrollableDisplay, defStyle, 0);
        setMaxHeight(a.getDimensionPixelSize(R.styleable.ScrollableDisplay_max_height, -1));
        a.recycle();
    }

    public void setMaxHeight(int maxHeight) {
        mMaxHeight = maxHeight;
    }

    public View getView() {
        return getChildAt(0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if(mMaxHeight != -1) {
            int width = getMeasuredWidth();
            int height = Math.min(getMeasuredHeight(), mMaxHeight);

            setMeasuredDimension(width, height);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        autoScrolling = false;
        super.onTouchEvent(ev);
        return false;
    }

    @Override
    public void computeScroll() {
        if(autoScrolling) return;
        super.computeScroll();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // HorizontalScrollView is broken for Gravity.RIGHT. So we're fixing it.
        autoScrolling = false;
        int childWidth = getView().getWidth();
        super.onLayout(changed, left, top, right, bottom);
        int delta = getView().getWidth() - childWidth;
        View view = getView();
        LayoutParams p = (LayoutParams) view.getLayoutParams();
        int verticalGravity = p.gravity & Gravity.VERTICAL_GRAVITY_MASK;
        if(getScrollRange() > 0) {
            gravityRight = true;
            p.gravity = Gravity.LEFT | verticalGravity;
            view.setLayoutParams(p);
            super.onLayout(changed, left, top, right, bottom);
        }
        if(gravityRight) {
            if(getScrollRange() == 0) {
                gravityRight = false;
                p.gravity = Gravity.RIGHT | verticalGravity;
                view.setLayoutParams(p);
                super.onLayout(changed, left, top, right, bottom);
            }
        }
        if(gravityRight && delta > 0) {
            scrollBy(delta, 0);
            autoScrolling = true;
        }
    }

    private int getScrollRange() {
        int scrollRange = 0;
        if(getChildCount() > 0) {
            View child = getChildAt(0);
            scrollRange = Math.max(0, child.getWidth() - (getWidth() - getPaddingLeft() - getPaddingRight()));
        }
        return scrollRange;
    }

    @Override
    public void scrollTo(int x, int y) {
        if(autoScrolling) return;
        super.scrollTo(x, y);
    }
}
