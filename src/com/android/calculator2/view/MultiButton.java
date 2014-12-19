package com.android.calculator2.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import com.android.calculator2.Calculator;
import com.android.calculator2.util.AnimationUtil;

/**
 * A collection of buttons that occupy the same space, only one of which is visible at a time
 */
public class MultiButton extends FrameLayout {

    private static final String TAG = Calculator.TAG;

    private int mActiveViewId = View.NO_ID;

    public MultiButton(Context context) {
        super(context);
    }

    public MultiButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MultiButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MultiButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        for (int i=0; i < getChildCount(); ++i) {
            getChildAt(i).setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Disable the currently active button and enable the one with the given resId
     *
     * @param resId
     */
    public void setEnabled(int resId) {
        if (mActiveViewId == resId) {
            return;
        }

        View newView = findViewById(resId);
        if (newView == null) {
            Log.w(TAG, "Cannot enable MultiButton view by resId " + resId);
            return;
        }

        if (mActiveViewId != View.NO_ID) {
            View oldView = findViewById(mActiveViewId);
            AnimationUtil.shrinkAndGrow(oldView, newView);
        } else {
            newView.setVisibility(View.VISIBLE);
        }

        mActiveViewId = resId;
    }

    /**
     * Gets currently enabled view
     *
     * @return enabled view or null if none
     */
    public View getEnabledView() {
        return findViewById(mActiveViewId);
    }
}
