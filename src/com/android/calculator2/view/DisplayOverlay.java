package com.android.calculator2.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.calculator2.R;

public class DisplayOverlay extends FrameLayout {
    /**
     * Closing the history with a fling will finish at least this fast (ms)
     */
    private static final float MIN_SETTLE_DURATION = 200f;

    /**
     * Do not settle overlay if velocity is less than this
     */
    private static float VELOCITY_SLOP = 0.1f;

    private static boolean DEBUG = true;
    private static final String TAG = "DisplayOverlay";

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private float mInitialMotionY;
    private float mLastMotionY;
    private float mLastDeltaY;
    private int mTouchSlop;
    private int mHistoryViewHeight;
    private int mMaxTranslationInParent = -1;
    private VelocityTracker mVelocityTracker;
    private float mMinVelocity = -1;
    private int mParentHeight = -1;

    public DisplayOverlay(Context context) {
        this(context, null);
    }

    public DisplayOverlay(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public DisplayOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, -1);
    }

    public DisplayOverlay(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
    }

    private enum TranslateState {
        EXPANDED, COLLAPSED, PARTIAL
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mRecyclerView = (RecyclerView)findViewById(R.id.historyRecycler);
        mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mHistoryViewHeight =
                getContext().getResources().getDimensionPixelSize(R.dimen.history_view_height);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = MotionEventCompat.getActionMasked(ev);
        float y = ev.getRawY();
        TranslateState state = getTranslateState();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mInitialMotionY = y;
                mLastMotionY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = y - mInitialMotionY;
                if (Math.abs(dy) < mTouchSlop) {
                    return false;
                }

                if (dy < 0) {
                    return isScrolledToEnd() && state != TranslateState.COLLAPSED;
                } else if (dy > 0) {
                    return state != TranslateState.EXPANDED;
                }

                break;
        }

        return false;
    }

    private boolean isScrolledToEnd() {
        return mLayoutManager.findLastCompletelyVisibleItemPosition() ==
                mRecyclerView.getAdapter().getItemCount() - 1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        initVelocityTrackerIfNotExists();
        adjustHistoryViewHeight();
        mVelocityTracker.addMovement(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                handleMove(event);
                break;
            case MotionEvent.ACTION_UP:
                handleUp(event);
                recycleVelocityTracker();
                break;
            case MotionEvent.ACTION_CANCEL:
                recycleVelocityTracker();
                break;
        }

        return true;
    }

    private void handleMove(MotionEvent event) {
        TranslateState state = getTranslateState();
        float y = event.getRawY();
        float dy = y - mLastMotionY;
        if (DEBUG) {
            Log.v(TAG, "handleMove y=" + y + ", dy=" + dy);
        }

        if (dy < 0 && state != TranslateState.COLLAPSED) {
            updateTranslation(dy);
        } else if (dy > 0 && state != TranslateState.EXPANDED) {
            updateTranslation(dy);
        }
        mLastMotionY = y;
        mLastDeltaY = dy;
    }

    private void handleUp(MotionEvent event) {
        mVelocityTracker.computeCurrentVelocity(1);
        float yvel = mVelocityTracker.getYVelocity();
        if (DEBUG) {
            Log.v(TAG, "handleUp yvel=" + yvel + ", mLastDeltaY=" + mLastDeltaY);
        }

        if (Math.abs(yvel) > VELOCITY_SLOP) {
            // the sign on velocity seems unreliable, so use last delta to determine direction
            float destTx = mLastDeltaY > 0 ? getMaxTranslation() : 0;
            float velocity = Math.max(Math.abs(yvel), Math.abs(mMinVelocity));
            settleAt(destTx, velocity);
        }
    }

    /**
     * Smoothly translates the display overlay to the given target
     *
     * @param destTx target translation
     * @param yvel velocity at point of release
     */
    private void settleAt(float destTx, float yvel) {
        if (yvel != 0) {
            float dist = destTx - getTranslationY();
            float dt = Math.abs(dist / yvel);

            ObjectAnimator anim =
                    ObjectAnimator.ofFloat(this, "translationY",
                            getTranslationY(), destTx);
            anim.setDuration((long)dt);
            anim.start();
        }
    }

    private int getMaxTranslation() {
        if (mMaxTranslationInParent < 0) {
            int bottomPadding = getContext().getResources()
                    .getDimensionPixelOffset(R.dimen.history_view_bottom_margin);
            int displayHeight = getHeight() - mHistoryViewHeight;
            int txMax = getParentHeight() - displayHeight - bottomPadding;
            mMaxTranslationInParent = Math.min(txMax, mHistoryViewHeight);
            if (DEBUG) {
                Log.v(TAG, "mMaxTranslationInParent = " + mMaxTranslationInParent);
            }
        }
        return mMaxTranslationInParent;
    }

    private void updateTranslation(float dy) {
        float txY = getTranslationY() + dy;
        float clampedY = Math.min(Math.max(txY, 0), getMaxTranslation());
        setTranslationY(clampedY);
    }

    private TranslateState getTranslateState() {
        float txY = getTranslationY();
        if (txY <= 0) {
            return TranslateState.COLLAPSED;
        } else if (txY >= getMaxTranslation()) {
            return TranslateState.EXPANDED;
        } else {
            return TranslateState.PARTIAL;
        }
    }

    public RecyclerView getHistoryView() {
        return mRecyclerView;
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        if (mMinVelocity < 0) {
            int txDist = getMaxTranslation();
            mMinVelocity = txDist / MIN_SETTLE_DURATION;
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private int getParentHeight() {
        if (mParentHeight < 0) {
            ViewGroup parent = (ViewGroup)getParent();
            mParentHeight = parent.getHeight();
        }
        return mParentHeight;
    }

    private void adjustHistoryViewHeight() {
        int maxTx = getMaxTranslation();
        if (mRecyclerView.getLayoutParams().height != maxTx) {
            MarginLayoutParams params = (MarginLayoutParams)mRecyclerView.getLayoutParams();
            params.height = maxTx;
            if (DEBUG) {
                Log.v(TAG, "Set history height to " + maxTx);
            }
            mRecyclerView.requestLayout();
        }
    }
}
