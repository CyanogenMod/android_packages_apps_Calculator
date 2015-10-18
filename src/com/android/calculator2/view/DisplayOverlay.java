package com.android.calculator2.view;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.calculator2.R;
import com.android.calculator2.view.display.AdvancedDisplay;
import com.android.calculator2.util.AnimationUtil;

/**
 * The display overlay is a container that intercepts touch events on top of:
 *      1. the display, i.e. the formula and result views
 *      2. the history view, which is revealed by dragging down on the display
 *
 * This overlay passes vertical scrolling events down to the history recycler view
 * when applicable.  If the user attempts to scroll up and the recycler is already
 * scrolled all the way up, then we intercept the event and collapse the history.
 */
public class DisplayOverlay extends FrameLayout {
    /**
     * Closing the history with a fling will finish at least this fast (ms)
     */
    private static final float MIN_SETTLE_DURATION = 200f;

    /**
     * Do not settle overlay if velocity is less than this
     */
    private static float VELOCITY_SLOP = 0.1f;

    private static boolean DEBUG = false;
    private static final String TAG = "DisplayOverlay";

    public static enum DisplayMode { FORMULA, GRAPH };

    private RecyclerView mRecyclerView;
    private AdvancedDisplay mFormula;
    private View mResult;
    private View mGraphLayout;
    private View mCloseGraphHandle;
    private View mMainDisplay;
    private DisplayMode mMode;
    private LinearLayoutManager mLayoutManager;
    private float mInitialMotionY;
    private float mLastMotionY;
    private float mLastDeltaY;
    private int mTouchSlop;
    private int mMaxTranslationInParent = -1;
    private VelocityTracker mVelocityTracker;
    private float mMinVelocity = -1;
    private int mParentHeight = -1;

    /**
     * Reports when state changes to expanded or collapsed (partial is ignored)
     */
    public static interface TranslateStateListener {
        public void onTranslateStateChanged(TranslateState newState);
    }
    private TranslateStateListener mTranslateStateListener;

    public DisplayOverlay(Context context) {
        super(context);
        setup();
    }

    public DisplayOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public DisplayOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    public DisplayOverlay(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup();
    }

    private void setup() {
        ViewConfiguration vc = ViewConfiguration.get(getContext());
        mTouchSlop = vc.getScaledTouchSlop();
    }

    public static enum TranslateState {
        EXPANDED, COLLAPSED, PARTIAL
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mRecyclerView = (RecyclerView)findViewById(R.id.historyRecycler);
        mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mLayoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mFormula = (AdvancedDisplay)findViewById(R.id.formula);
        mResult = findViewById(R.id.result);
        mGraphLayout = findViewById(R.id.graphLayout);
        mMainDisplay = findViewById(R.id.mainDisplay);
        mCloseGraphHandle = findViewById(R.id.closeGraphHandle);
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

                // in graph mode let move events apply to the graph,
                // unless the touch is on the "close handle"
                if (mMode == DisplayMode.GRAPH) {
                    return isInBounds(ev.getX(), ev.getY(), mCloseGraphHandle);
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

        TranslateState curState = getTranslateState();
        if (curState != TranslateState.PARTIAL) {
            // already settled
            if (mTranslateStateListener != null) {
                mTranslateStateListener.onTranslateStateChanged(curState);
            }
        } else if (Math.abs(yvel) > VELOCITY_SLOP) {
            // the sign on velocity seems unreliable, so use last delta to determine direction
            float destTx = mLastDeltaY > 0 ? getMaxTranslation() : 0;
            float velocity = Math.max(Math.abs(yvel), Math.abs(mMinVelocity));
            settleAt(destTx, velocity);
        }
    }

    public void expandHistory() {
        settleAt(getMaxTranslation(), mMinVelocity);
    }

    public void collapseHistory() {
        settleAt(0, mMinVelocity);
    }

    public int getDisplayHeight() {
        return mFormula.getHeight() + mResult.getHeight();
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
            if (DEBUG) {
                Log.v(TAG, "settle display overlay yvel=" + yvel +
                        ", dt = " + dt);
            }

            ObjectAnimator anim =
                    ObjectAnimator.ofFloat(this, "translationY",
                            getTranslationY(), destTx);
            anim.setDuration((long)dt);
            anim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {}

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mTranslateStateListener != null) {
                        mTranslateStateListener.onTranslateStateChanged(getTranslateState());
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {}

                @Override
                public void onAnimationRepeat(Animator animation) {}
            });
            anim.start();
        }
    }

    /**
     * The distance that we are able to pull down the display to reveal history.
     */
    private int getMaxTranslation() {
        if (mMaxTranslationInParent < 0) {
            int bottomPadding = getContext().getResources()
                    .getDimensionPixelOffset(R.dimen.history_view_bottom_margin);
            mMaxTranslationInParent = getParentHeight() - getDisplayHeight() - bottomPadding;
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

    /**
     * Set the size and offset of the history view / graph view
     *
     * We want the display+history to take up the full height of the parent minus some
     * predefined padding.  The normal way to do this would be to give the overlay a height
     * of match_parent minus some margin, and set an initial translation.  The issue with
     * this is that the display has a height of wrap content and the keypad fills the
     * remaining space, so we cannot determine the proper height for the history view until
     * after layout completes.
     *
     * To account for this, we make this method available to setup the history and graph
     * views after layout completes.
     */
    public void initializeHistoryAndGraphView() {
        int maxTx = getMaxTranslation();
        if (mRecyclerView.getLayoutParams().height <= 0
                || mGraphLayout.getLayoutParams().height <= 0) {
            MarginLayoutParams historyParams = (MarginLayoutParams)mRecyclerView.getLayoutParams();
            historyParams.height = maxTx;

            MarginLayoutParams graphParams = (MarginLayoutParams)mGraphLayout.getLayoutParams();
            graphParams.height = maxTx + getDisplayHeight();
            if (DEBUG) {
                Log.v(TAG, "Set history height to " + maxTx
                        + ", graph height to " + graphParams.height);
            }

            MarginLayoutParams overlayParams =
                    (MarginLayoutParams)getLayoutParams();
            overlayParams.topMargin = -maxTx;
            requestLayout();
            scrollToMostRecent();
        }

        if (mMinVelocity < 0) {
            int txDist = getMaxTranslation();
            mMinVelocity = txDist / MIN_SETTLE_DURATION;
        }
    }

    public void scrollToMostRecent() {
        mRecyclerView.scrollToPosition(mRecyclerView.getAdapter().getItemCount()-1);
    }

    public void setTranslateStateListener(TranslateStateListener listener) {
        mTranslateStateListener = listener;
    }

    public TranslateStateListener getTranslateStateListener() {
        return mTranslateStateListener;
    }

    private boolean isInBounds(float x, float y, View v) {
        return y >= v.getTop() && y <= v.getBottom() &&
                x >= v.getLeft() && x <= v.getRight();
    }

    public void animateModeTransition() {
        switch (mMode) {
            case GRAPH:
                expandHistory();
                AnimationUtil.fadeOut(mMainDisplay);
                AnimationUtil.fadeIn(mGraphLayout);
                break;
            case FORMULA:
                collapseHistory();
                AnimationUtil.fadeIn(mMainDisplay);
                AnimationUtil.fadeOut(mGraphLayout);
                break;
        }
    }

    public void setMode(DisplayMode mode) {
        mMode = mode;
    }

    public DisplayMode getMode() {
        return mMode;
    }
}
