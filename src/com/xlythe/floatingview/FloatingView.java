package com.xlythe.floatingview;

import android.animation.ValueAnimator;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.android.calculator2.R;

public abstract class FloatingView extends Service implements OnTouchListener {
    public static FloatingView ACTIVE_VIEW;
    private static int MARGIN_VERTICAL; // Margin around the phone.
    private static int MARGIN_HORIZONTAL; // Margin around the phone.
    private static int MARGIN_VIEW;
    private static int CLOSE_ANIMATION_DISTANCE;
    private static int CLOSE_OFFSET;
    private static int DRAG_DELTA;
    private static int STARTING_POINT_Y;
    private static int DELETE_BOX_WIDTH;
    private static int DELETE_BOX_HEIGHT;
    private static int FLOATING_WINDOW_ICON_SIZE;
    private static int MAGIC_OFFSET;
    // Drag variables
    float mPrevDragX;
    float mPrevDragY;
    float mOrigX;
    float mOrigY;
    boolean mDragged;
    // View variables
    private ViewGroup mRootView;
    private BroadcastReceiver mBroadcastReceiver;
    private WindowManager mWindowManager;
    private ViewGroup mDraggableIcon;
    private ViewGroup mInactiveButton;
    private WindowManager.LayoutParams mInactiveParams;
    private ViewGroup mView;
    private ViewGroup mDeleteView;
    private View mDeleteBoxView;
    private boolean mDeleteBoxVisible = false;
    private boolean mIsDestroyed = false;
    private boolean mIsBeingDestroyed = false;
    private int mCurrentPosX = -1;
    private int mCurrentPosY = -1;
    private int mCalcParamsX;
    private int mCalcParamsY;
    // Animation variables
    private LimitedQueue<Float> mDeltaXArray;
    private LimitedQueue<Float> mDeltaYArray;
    private AnimationTask mAnimationTask;
    // Open/Close variables
    private boolean mIsViewOpen = false;
	private Point mWiggle;
    private boolean mAnimateWiggle = false;
    private boolean mWiggleAnimating = false;
    private boolean mEnableWiggle = false;
    // Close logic
    private boolean mIsInDeleteMode = false;
    private View mDeleteIcon;
    private View mDeleteIconHolder;
    private boolean mIsAnimationLocked = false;
    private boolean mDontVibrate = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void addView() {
        mRootView = new RelativeLayout(getContext());
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        mWindowManager.addView(mRootView, params);
        mRootView.setVisibility(View.GONE);

        mInactiveButton = new FrameLayout(getContext());
        View icon = inflateButton();
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams((int) getResources().getDimension(R.dimen.floating_window_icon), (int) getResources().getDimension(R.dimen.floating_window_icon));
        mInactiveButton.addView(icon, iconParams);
        params = mInactiveParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        mWindowManager.addView(mInactiveButton, params);
        mInactiveButton.setOnTouchListener(this);
    }

    protected abstract View inflateButton();

    private void updateIconPosition(int x, int y) {
        mCurrentPosX = x;
        mCurrentPosY = y;
        if(!mIsDestroyed) {
            mDraggableIcon.setTranslationX(mCurrentPosX);
            mDraggableIcon.setTranslationY(mCurrentPosY);
        }
    }

    private boolean isDeleteMode() {
        return isDeleteMode(mCurrentPosX, mCurrentPosY);
    }

    private boolean isDeleteMode(int x, int y) {
        int screenWidth = getScreenWidth();
        int screenHeight = getScreenHeight();
        int boxWidth = DELETE_BOX_WIDTH;
        int boxHeight = DELETE_BOX_HEIGHT;
        boolean horz = x + (mDraggableIcon == null ? 0 : mDraggableIcon.getWidth()) > (screenWidth / 2 - boxWidth / 2) && x < (screenWidth / 2 + boxWidth / 2);
        boolean vert = y + (mDraggableIcon == null ? 0 : mDraggableIcon.getHeight()) > (screenHeight - boxHeight);

        return horz && vert;
    }

    private void showDeleteBox() {
        if(!mDeleteBoxVisible) {
            mDeleteBoxVisible = true;
            if(mDeleteView == null) {
                mDeleteView = new FrameLayout(getContext());
                View.inflate(getContext(), R.layout.floating_delete_box, mDeleteView);
                mDeleteIcon = mDeleteView.findViewById(R.id.delete_icon);
                mDeleteIconHolder = mDeleteView.findViewById(R.id.delete_icon_holder);
                mDeleteBoxView = mDeleteView.findViewById(R.id.box);
                mRootView.addView(mDeleteView);
                ((RelativeLayout.LayoutParams) mDeleteView.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            }
            else {
                mDeleteView.setVisibility(View.VISIBLE);
            }
            mEnableWiggle = false;
            mDeleteIconHolder.setTranslationX(0);
            mDeleteIconHolder.setTranslationY(0);
            mDeleteBoxView.setAlpha(0);
            mDeleteBoxView.animate().alpha(1);
            mDeleteIconHolder.setTranslationY(CLOSE_ANIMATION_DISTANCE);
            mDeleteIconHolder.animate().translationYBy(-1 * (CLOSE_ANIMATION_DISTANCE + CLOSE_OFFSET)).setListener(new AnimationFinishedListener() {
                @Override
                public void onAnimationFinished() {
                    mEnableWiggle = true;
                    mAnimateWiggle = true;
                }
            });
            View child = mDeleteView.getChildAt(0);
            child.findViewById(R.id.box).getLayoutParams().width = getScreenWidth();
        }
    }

    public void hideDeleteBox() {
        if(mDeleteBoxVisible) {
            mDeleteBoxVisible = false;
            if(mDeleteView != null) {
                mDeleteBoxView.animate().alpha(0);
                mDeleteIconHolder.animate().translationYBy(CLOSE_ANIMATION_DISTANCE).setListener(new AnimationFinishedListener() {
                    @Override
                    public void onAnimationFinished() {
                        if(mDeleteView != null) mDeleteView.setVisibility(View.GONE);
                    }
                });
            }
        }
    }

    private void adjustInactivePosition() {
        if(mRootView == null) return;

        mInactiveButton.setVisibility(View.VISIBLE);
        mRootView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mRootView != null && !mIsViewOpen) {
                    mRootView.setVisibility(View.GONE);
                }
            }
        }, 30);
        int x = mCurrentPosX;
        int y = mCurrentPosY;
        View v = mInactiveButton.getChildAt(0);
        v.setTranslationX(0);
        if(x < 0) {
            v.setTranslationX(x);
            x = 0;
        }
        if(x > getScreenWidth() - FLOATING_WINDOW_ICON_SIZE) {
            v.setTranslationX(x - getScreenWidth() + FLOATING_WINDOW_ICON_SIZE);
            x = getScreenWidth() - FLOATING_WINDOW_ICON_SIZE;
        }
        v.setTranslationY(0);
        if(y < 0) {
            v.setTranslationY(y);
            y = 0;
        }
        if(y > getScreenHeight() - FLOATING_WINDOW_ICON_SIZE) {
            v.setTranslationY(y - getScreenHeight() + FLOATING_WINDOW_ICON_SIZE);
            y = getScreenHeight() - FLOATING_WINDOW_ICON_SIZE;
        }
        mInactiveParams.x = x;
        mInactiveParams.y = y;
        if(!mIsDestroyed) mWindowManager.updateViewLayout(mInactiveButton, mInactiveParams);
    }

    private void vibrate() {
        if(mDontVibrate) return;
        Vibrator vi = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if(!vi.hasVibrator()) return;
        vi.vibrate(25);
    }

    private void close(boolean animate) {
        if(mIsBeingDestroyed) return;
        mIsBeingDestroyed = true;

        if(animate) {
            animateToDeleteBoxCenter(new AnimationFinishedListener() {
                @Override
                public void onAnimationFinished() {
                    hideDeleteBox();
                    mDeleteIconHolder.animate().scaleX(0.3f).scaleY(0.3f);
                    mDraggableIcon.animate().scaleX(0.3f).scaleY(0.3f).translationYBy(CLOSE_ANIMATION_DISTANCE).setDuration(mDeleteIconHolder.animate().getDuration()).setInterpolator(new ValueAnimator().getInterpolator()).setListener(new AnimationFinishedListener() {
                        @Override
                        public void onAnimationFinished() {
                            stopSelf();
                        }
                    });
                }
            });
        }
        else {
            stopSelf();
        }
    }

    private void animateToDeleteBoxCenter(final AnimationFinishedListener l) {
        if(mIsAnimationLocked || mWiggle == null || mRootView == null || mDraggableIcon == null) return;
        mIsInDeleteMode = true;
        if(mAnimationTask != null) mAnimationTask.cancel();
        mAnimationTask = new AnimationTask(mWiggle.x + getScreenWidth() / 2 - mDraggableIcon.getWidth() / 2, mWiggle.y + mRootView.getHeight() - DELETE_BOX_HEIGHT / 2 - mDraggableIcon.getHeight() / 2+MAGIC_OFFSET);
        mAnimationTask.setDuration(150);
        mAnimationTask.setAnimationFinishedListener(l);
        mAnimationTask.run();
        vibrate();
        mDeleteIcon.animate().scaleX(1.4f).scaleY(1.4f).setDuration(100);
    }

    @Override
    public void onCreate() {
        super.onCreate();

		// Set up a static callback for the FloatingActivity
		ACTIVE_VIEW = this;

        // Load margins, distances, etc
        MARGIN_VERTICAL = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
        MARGIN_HORIZONTAL = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -10, getResources().getDisplayMetrics());
        MARGIN_VIEW = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, getResources().getDisplayMetrics());
        CLOSE_ANIMATION_DISTANCE = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 250, getResources().getDisplayMetrics());
        CLOSE_OFFSET = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        DRAG_DELTA = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        STARTING_POINT_Y = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
        DELETE_BOX_WIDTH = (int) getResources().getDimension(R.dimen.floating_window_delete_box_width);
        DELETE_BOX_HEIGHT = (int) getResources().getDimension(R.dimen.floating_window_delete_box_height);
        FLOATING_WINDOW_ICON_SIZE = (int) getResources().getDimension(R.dimen.floating_window_icon);
        MAGIC_OFFSET = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());

        addView();
        mDraggableIcon = new FrameLayout(this);
        mDraggableIcon.setOnTouchListener(this);
        View icon = inflateButton();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int) getResources().getDimension(R.dimen.floating_window_icon), (int) getResources().getDimension(R.dimen.floating_window_icon));
        mDraggableIcon.addView(icon, params);
        mRootView.addView(mDraggableIcon);
        updateIconPosition(MARGIN_HORIZONTAL, STARTING_POINT_Y);
        adjustInactivePosition();

        // Actionbar changes heights based on orientation
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent myIntent) {
                int x = mCurrentPosX <= 0 ? 0 + MARGIN_HORIZONTAL : getScreenWidth() - FLOATING_WINDOW_ICON_SIZE - MARGIN_HORIZONTAL;
                int y = mCurrentPosY;
                if(y <= 0) y = MARGIN_VERTICAL;
                if(y >= getScreenHeight() - mDraggableIcon.getHeight()) y = getScreenHeight() - mDraggableIcon.getHeight() - MARGIN_VERTICAL;
                updateIconPosition(x, y);
                adjustInactivePosition();
            }
        };
        registerReceiver(mBroadcastReceiver, filter);
    }

    private void calculateWiggle(int x, int y) {
        Point closeIcon = new Point(getScreenWidth() / 2, mRootView.getHeight() - DELETE_BOX_HEIGHT / 2);
        int wiggleX = (x-closeIcon.x)/10;
        int wiggleY = Math.max(-1 * DELETE_BOX_HEIGHT / 8, (y-closeIcon.y)/10);
        mWiggle = new Point(wiggleX, wiggleY);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mRootView.setVisibility(View.VISIBLE);
        mInactiveButton.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mInactiveButton != null) mInactiveButton.setVisibility(View.INVISIBLE);
            }
        }, 30);
        switch(event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            mPrevDragX = mOrigX = event.getRawX();
            mPrevDragY = mOrigY = event.getRawY();

            mDragged = false;

            mDeltaXArray = new LimitedQueue<Float>(5);
            mDeltaYArray = new LimitedQueue<Float>(5);

            mDraggableIcon.setScaleX(0.92f);
            mDraggableIcon.setScaleY(0.92f);

            // Cancel any currently running animations
            if(mAnimationTask != null) {
                mAnimationTask.cancel();
            }
            break;
        case MotionEvent.ACTION_UP:
            mIsAnimationLocked = false;
            if(mAnimationTask != null) mAnimationTask.cancel();
            if(!mDragged) {
                if(!mIsViewOpen) {
                    openView();
                }
                else {
                    closeView();
                }
            }
            else {
                // Animate the icon
                mAnimationTask = new AnimationTask();
                mAnimationTask.run();
            }

            if(mIsInDeleteMode) {
                close(true);
            }
            else {
                hideDeleteBox();
                mDraggableIcon.setScaleX(1f);
                mDraggableIcon.setScaleY(1f);
            }

            break;
        case MotionEvent.ACTION_MOVE:
            int x = (int) (event.getRawX() - mDraggableIcon.getWidth() / 2);
            int y = (int) (event.getRawY() - mDraggableIcon.getHeight());
            if(mDeleteIconHolder != null) {
                calculateWiggle(x, y);
                if(!mEnableWiggle) {
                    mEnableWiggle = true;
                    mAnimateWiggle = true;
                }
                if(mEnableWiggle) {
                    if(mAnimateWiggle) {
                        mAnimateWiggle = false;
                        mWiggleAnimating = true;
                        mDeleteIconHolder.animate().translationX(mWiggle.x).translationY(mWiggle.y).setListener(new AnimationFinishedListener() {
                            @Override
                            public void onAnimationFinished() {
                                mWiggleAnimating = false;
                            }
                        });
                    }
                    else if(!mWiggleAnimating) {
                        mDeleteIconHolder.setTranslationX(mWiggle.x);
                        mDeleteIconHolder.setTranslationY(mWiggle.y);
                    }
                    if(mIsInDeleteMode && isDeleteMode(x, y)) {
                        mDraggableIcon.setTranslationX(getScreenWidth() / 2 - mDraggableIcon.getWidth() / 2 + mWiggle.x);
                        mDraggableIcon.setTranslationY(mRootView.getHeight() - DELETE_BOX_HEIGHT / 2 - mDraggableIcon.getHeight() / 2 + mWiggle.y + MAGIC_OFFSET);
                    }
                }
            }
            if(isDeleteMode(x, y)) {
                if(!mIsInDeleteMode) animateToDeleteBoxCenter(new AnimationFinishedListener() {
                    @Override
                    public void onAnimationFinished() {
                        mDontVibrate = true;
                    }
                });
            }
            else if(isDeleteMode() && !mIsAnimationLocked) {
                mIsInDeleteMode = false;
                if(mAnimationTask != null) mAnimationTask.cancel();
                mAnimationTask = new AnimationTask(x, y);
                mAnimationTask.setDuration(50);
                mAnimationTask.setInterpolator(new LinearInterpolator());
                mAnimationTask.setAnimationFinishedListener(new AnimationFinishedListener() {
                    @Override
                    public void onAnimationFinished() {
                        mIsAnimationLocked = false;
                    }
                });
                mAnimationTask.run();
                mIsAnimationLocked = true;
                mDeleteIcon.animate().scaleX(1f).scaleY(1f).setDuration(100);
            }
            else {
                if(mIsInDeleteMode) {
                    mDeleteIcon.animate().scaleX(1f).scaleY(1f).setDuration(100);
                    mIsInDeleteMode = false;
                }
                if(!mIsAnimationLocked && mDragged) {
                    if(mAnimationTask != null) mAnimationTask.cancel();
                    updateIconPosition(x, y);
                    mDontVibrate = false;
                }
            }

            float deltaX = event.getRawX() - mPrevDragX;
            float deltaY = event.getRawY() - mPrevDragY;

            mDeltaXArray.add(deltaX);
            mDeltaYArray.add(deltaY);

            mPrevDragX = event.getRawX();
            mPrevDragY = event.getRawY();

            deltaX = event.getRawX() - mOrigX;
            deltaY = event.getRawY() - mOrigY;
            mDragged = mDragged || Math.abs(deltaX) > DRAG_DELTA || Math.abs(deltaY) > DRAG_DELTA;
            if(mDragged) {
                closeView(false);
                showDeleteBox();
            }
            break;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsDestroyed = true;
        if(mRootView != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mRootView);
            mRootView = null;
        }
        if(mInactiveButton != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mInactiveButton);
            mInactiveButton = null;
        }
        if(mAnimationTask != null) {
            mAnimationTask.cancel();
            mAnimationTask = null;
        }
        if(mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
		ACTIVE_VIEW = null;
    }

    public void openView() {
        if(!mIsViewOpen) {
            if(mIsAnimationLocked) return;
            mIsViewOpen = true;
            mAnimationTask = new AnimationTask(getOpenX(), getOpenY());
            mAnimationTask.setAnimationFinishedListener(new AnimationFinishedListener() {
                @Override
                public void onAnimationFinished() {
                    show();
                }
            });
            mAnimationTask.run();
            mRootView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    closeView();
                    return true;
                }
            });
            Intent intent = new Intent(getContext(), FloatingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(FloatingActivity.EXTRA_HIDE_STATUS_BAR, mRootView.getHeight() != getScreenHeight());
            startActivity(intent);
        }
    }

    private int getOpenX() {
        return (int) (getScreenWidth() - mDraggableIcon.getWidth() * 1.2);
    }

    private int getOpenY() {
        return STARTING_POINT_Y;
    }

    public void closeView() {
        closeView(true);
    }

    public void closeView(boolean returnToOrigin) {
        mRootView.setOnTouchListener(null);
        if(mIsViewOpen) {
            mIsViewOpen = false;
            if(returnToOrigin) {
                if(mIsAnimationLocked) return;
                mAnimationTask = new AnimationTask(mCurrentPosX, mCurrentPosY);
                mAnimationTask.setAnimationFinishedListener(new AnimationFinishedListener() {
                    @Override
                    public void onAnimationFinished() {
                        adjustInactivePosition();
                    }
                });
                mAnimationTask.run();
            }
            if(FloatingActivity.ACTIVE_ACTIVITY != null) FloatingActivity.ACTIVE_ACTIVITY.finish();
            hide();
        }
    }

    protected abstract View inflateView();

    private void show() {
        if(mView == null) {
            View child = inflateView();
            mView = new FrameLayout(getContext());
            mView.addView(child);
            mRootView.addView(mView);

        }
        else {
            mView.setVisibility(View.VISIBLE);
        }
        // Adjust calc location
        int screenWidth = getScreenWidth();
        int screenHeight = getScreenHeight();
        int calcWidth = 4 * (int) getResources().getDimension(R.dimen.floating_window_button_height);
        mCalcParamsX = screenWidth - calcWidth - MARGIN_VIEW;
        int attemptedY = (int) (STARTING_POINT_Y * 1.1) + mDraggableIcon.getHeight() + mView
                .getHeight();
        if (attemptedY > screenHeight) {
            mCalcParamsX = mCalcParamsX - mDraggableIcon.getHeight();
            mCalcParamsY = (int) (STARTING_POINT_Y * 1.5) - mDraggableIcon.getHeight();
        } else {
            mCalcParamsY = (int) (STARTING_POINT_Y * 1.1) + mDraggableIcon.getHeight();
        }

        if(!mIsDestroyed) {
            mView.setTranslationX(mCalcParamsX);
            mView.setTranslationY(mCalcParamsY);
        }

        // Animate calc in
        View child = mView.getChildAt(0);
        child.setAlpha(0);
        if (attemptedY > screenHeight) {
            child.setBackgroundResource(0);
        } else {
            child.setBackgroundResource(R.drawable.floating_calc_background);
        }
        child.animate().setDuration(150).alpha(1).setListener(null);

		onShow();
    }

	public void onShow() {}

	public void onHide() {}

    private void hide() {
        if(mView != null) {
            View child = mView.getChildAt(0);
            child.setAlpha(1);
            child.animate().setDuration(150).alpha(0).setListener(new AnimationFinishedListener() {
                @Override
                public void onAnimationFinished() {
                    mView.setVisibility(View.GONE);
                }
            });

			onHide();
        }
    }

    private float calculateVelocityX() {
        int depreciation = mDeltaXArray.size() + 1;
        float sum = 0;
        for(Float f : mDeltaXArray) {
            depreciation--;
            if(depreciation > 5) continue;
            sum += f / depreciation;
        }
        return sum;
    }

    private float calculateVelocityY() {
        int depreciation = mDeltaYArray.size() + 1;
        float sum = 0;
        for(Float f : mDeltaYArray) {
            depreciation--;
            if(depreciation > 5) continue;
            sum += f / depreciation;
        }
        return sum;
    }

    protected Context getContext() {
        return this;
    }

    private int getScreenWidth() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    private int getScreenHeight() {
        return getResources().getDisplayMetrics().heightPixels - getStatusBarHeight();
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if(resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private float sqr(float f) {
        return f * f;
    }

    // Timer for animation/automatic movement of the tray.
    private class AnimationTask {
        // Ultimate destination coordinates toward which the view will move
        int mDestX;
        int mDestY;
        long mDuration = 450;
        float mTension = 1.4f;
        Interpolator mInterpolator = new OvershootInterpolator(mTension);
        AnimationFinishedListener mAnimationFinishedListener;

        public AnimationTask(int x, int y) {
            setup(x, y);
        }

        public AnimationTask() {
            setup(calculateX(), calculateY());
            setAnimationFinishedListener(new AnimationFinishedListener() {
                @Override
                public void onAnimationFinished() {
                    adjustInactivePosition();
                }
            });

            float velocityX = calculateVelocityX();
            float velocityY = calculateVelocityY();
            mTension += Math.sqrt(sqr(velocityX) + sqr(velocityY)) / 200;
            mInterpolator = new OvershootInterpolator(mTension);

            mCurrentPosX = mDestX;
            mCurrentPosY = mDestY;
        }

        private void setup(int x, int y) {
            if(mIsAnimationLocked) throw new RuntimeException("Returning to user's finger. Avoid animations while mIsAnimationLocked flag is set.");
            mDestX = x;
            mDestY = y;
        }

        public void setDuration(long duration) {
            mDuration = duration;
            setup(mDestX, mDestY);
        }

        public void setAnimationFinishedListener(AnimationFinishedListener l) {
            mAnimationFinishedListener = l;
        }

        public void setInterpolator(Interpolator interpolator) {
            mInterpolator = interpolator;
        }

        private int calculateX() {
            float velocityX = calculateVelocityX();
            int screenWidth = getScreenWidth();
            int destX = (mCurrentPosX + mDraggableIcon.getWidth() / 2 > screenWidth / 2) ? screenWidth - mDraggableIcon.getWidth() - MARGIN_HORIZONTAL : 0 + MARGIN_HORIZONTAL;
            if(Math.abs(velocityX) > 50) {
                destX = (velocityX > 0) ? screenWidth - mDraggableIcon.getWidth() - MARGIN_HORIZONTAL : 0 + MARGIN_HORIZONTAL;
            }
            return destX;
        }

        private int calculateY() {
            float velocityY = calculateVelocityY();
            int screenHeight = getScreenHeight();
            int destY = mCurrentPosY + (int) (velocityY * 3);
            if(destY <= 0) destY = MARGIN_VERTICAL;
            if(destY >= screenHeight - mDraggableIcon.getHeight()) destY = screenHeight - mDraggableIcon.getHeight() - MARGIN_VERTICAL;
            return destY;
        }

        public void run() {
            mDraggableIcon.animate().translationX(mDestX).translationY(mDestY).setDuration(mDuration).setInterpolator(mInterpolator).setListener(mAnimationFinishedListener);
        }

        public void cancel() {
            mDraggableIcon.animate().cancel();
        }
    }
}
