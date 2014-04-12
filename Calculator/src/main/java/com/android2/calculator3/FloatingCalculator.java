package com.android2.calculator3;

import android.animation.Animator;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android2.calculator3.view.CalculatorDisplay;

import java.util.LinkedList;
import java.util.List;

public class FloatingCalculator extends Service {
    private static final int ANIMATION_FRAME_RATE = 60; // Animation frame rate per second.
    public static FloatingCalculator ACTIVE_CALCULATOR; // A copy of the service for the floating activity
    private static int MARGIN_VERTICAL; // Margin around the phone.
    private static int MARGIN_HORIZONTAL; // Margin around the phone.
    private static int DELETE_BOX_WIDTH;
    private static int DELETE_BOX_HEIGHT;
    private static int FLOATING_WINDOW_ICON_SIZE;

    // View variables
    private WindowManager mWindowManager;
    private FloatingView mDraggableIcon;
    private WindowManager.LayoutParams mParams;
    private WindowManager.LayoutParams mCalcParams;
    private FloatingView mCalcView;
    private FloatingView mDeleteBoxView;
    private boolean mDeleteBoxVisible = false;
    private boolean mIsDestroyed = false;
    private boolean mIsBeingDestroyed = false;

    // Animation variables
    private List<Float> mDeltaXArray;
    private List<Float> mDeltaYArray;
    private AnimationTask mAnimationTask;
    private Handler mAnimationHandler = new Handler();
    private long mAnimationLastOverwritten;

    // Open/Close variables
    private int mPrevX = -1;
    private int mPrevY = -1;
    private boolean mIsCalcOpen = false;

    // Calc logic
    private View.OnClickListener mListener;
    private CalculatorDisplay mDisplay;
    private Persist mPersist;
    private History mHistory;
    private Logic mLogic;

    // Close logic
    private int mCurrentX;
    private int mCurrentY;
    private boolean mIsInDeleteMode = false;
    private View mDeleteIcon;
    private View mDeleteIconHolder;
    private boolean mIsZoomingBack = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private WindowManager.LayoutParams addView(View v, int x, int y) {
        return addView(v, x, y, Gravity.TOP | Gravity.LEFT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private WindowManager.LayoutParams addView(View v, int x, int y, int gravity, int width, int height) {
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

        params.gravity = gravity;
        params.x = x;
        params.y = y;

        mWindowManager.addView(v, params);

        return params;
    }

    private void updateIconPosition(int x, int y) {
        View v = mDraggableIcon.findViewById(R.id.icon);
        v.setTranslationX(0);
        if(x < 0) {
            v.setTranslationX(x);
            x = 0;
        }
        if(x > getScreenWidth()-FLOATING_WINDOW_ICON_SIZE) {
            v.setTranslationX(x-getScreenWidth()+FLOATING_WINDOW_ICON_SIZE);
            x = getScreenWidth()-FLOATING_WINDOW_ICON_SIZE;
        }
        v.setTranslationY(0);
        if(y < 0) {
            v.setTranslationY(y);
            y = 0;
        }
        if(y > getScreenHeight()-FLOATING_WINDOW_ICON_SIZE) {
            v.setTranslationY(y - getScreenHeight() + FLOATING_WINDOW_ICON_SIZE);
            y = getScreenHeight()-FLOATING_WINDOW_ICON_SIZE;
        }
        mParams.x = x;
        mParams.y = y;
        if (!mIsDestroyed) mWindowManager.updateViewLayout(mDraggableIcon, mParams);
    }

    private boolean isDeleteMode() {
        return isDeleteMode(mParams.x, mParams.y);
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
        if (!mDeleteBoxVisible) {
            mDeleteBoxVisible = true;
            if (mDeleteBoxView == null) {
                View child = View.inflate(getContext(), R.layout.floating_calculator_delete_box, null);
                mDeleteBoxView = new FloatingView(getContext());
                mDeleteBoxView.addView(child);
                mDeleteIcon = (ImageView) mDeleteBoxView.findViewById(R.id.delete_icon);
                mDeleteIconHolder = mDeleteBoxView.findViewById(R.id.delete_icon_holder);
                addView(mDeleteBoxView, 0, 0, Gravity.BOTTOM | Gravity.CENTER_VERTICAL, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            } else {
                mDeleteBoxView.setVisibility(View.VISIBLE);
            }
            mDeleteIconHolder.setTranslationY(200);
            mDeleteIconHolder.animate().translationYBy(-200).setListener(null);
            View child = mDeleteBoxView.getChildAt(0);
            child.findViewById(R.id.box).getLayoutParams().width = getScreenWidth();
        }
    }

    public void hideDeleteBox() {
        if (mDeleteBoxVisible) {
            mDeleteBoxVisible = false;
            if (mDeleteBoxView != null) {
                mDeleteIconHolder.animate().translationYBy(200).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }

                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (mDeleteBoxView != null) mDeleteBoxView.setVisibility(View.GONE);
                    }
                });
            }
        }
    }

    private void vibrate() {
        Vibrator vi = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (!vi.hasVibrator()) return;
        vi.vibrate(25);
    }

    private void close(boolean animate) {
        if (mIsBeingDestroyed) return;
        mIsBeingDestroyed = true;

        if (animate) {
            animateToDeleteBoxCenter(new OnAnimationFinishedListener() {
                @Override
                public void onAnimationFinished() {
                    mAnimationHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            stopSelf();
                        }
                    }, 150);
                }
            });
        } else {
            stopSelf();
        }
    }

    private void animateToDeleteBoxCenter(final OnAnimationFinishedListener l) {
        if(mIsZoomingBack) return;
        mIsInDeleteMode = true;
        if (mAnimationTask != null) mAnimationTask.cancel();
        mAnimationTask = new AnimationTask(getScreenWidth() / 2 - mDraggableIcon.getWidth() / 2, getScreenHeight() - DELETE_BOX_HEIGHT / 2 - mDraggableIcon.getHeight() / 2);
        mAnimationTask.setDuration(150);
        mAnimationTask.setAnimationFinishedListener(l);
        mAnimationTask.run();
        vibrate();
        mDeleteIcon.animate().scaleX(1.25f).scaleY(1.25f).setDuration(100);
    }

    private void updateIconPositionByDelta(int deltaX, int deltaY) {
        updateIconPosition(mParams.x + deltaX, mParams.y + deltaY);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ACTIVE_CALCULATOR = this;
        MARGIN_VERTICAL = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
        MARGIN_HORIZONTAL = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -10, getResources().getDisplayMetrics());
        DELETE_BOX_WIDTH = (int) getResources().getDimension(R.dimen.floating_window_delete_box_width);
        DELETE_BOX_HEIGHT = (int) getResources().getDimension(R.dimen.floating_window_delete_box_height);
        FLOATING_WINDOW_ICON_SIZE = (int) getResources().getDimension(R.dimen.floating_window_icon);

        OnTouchListener dragListener = new OnTouchListener() {
            float mPrevDragX;
            float mPrevDragY;

            boolean mDragged;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mPrevDragX = event.getRawX();
                        mPrevDragY = event.getRawY();

                        mDragged = false;

                        mDeltaXArray = new LinkedList<Float>();
                        mDeltaYArray = new LinkedList<Float>();

                        mCurrentX = mParams.x;
                        mCurrentY = mParams.y;

                        // Cancel any currently running animations
                        if (mAnimationTask != null) {
                            mAnimationTask.cancel();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (!mDragged) {
                            if (mPrevX == -1) {
                                openCalculator();
                            } else {
                                closeCalculator();
                            }
                        } else {
                            // Animate the icon
                            mIsZoomingBack = false;
                            if (mAnimationTask != null) mAnimationTask.cancel();
                            mAnimationTask = new AnimationTask();
                            mAnimationTask.run();
                        }

                        if(mIsInDeleteMode) {
                            close(true);
                        }
                        else {
                            hideDeleteBox();
                        }

                        break;
                    case MotionEvent.ACTION_MOVE:
                        // Calculate position of the whole tray according to the drag, and update layout.
                        float deltaX = event.getRawX() - mPrevDragX;
                        float deltaY = event.getRawY() - mPrevDragY;
                        mCurrentX = (int) (event.getRawX()-mDraggableIcon.getWidth()/2);
                        mCurrentY = (int) (event.getRawY()-mDraggableIcon.getHeight());
                        if (isDeleteMode(mCurrentX, mCurrentY)) {
                            if(!mIsInDeleteMode) animateToDeleteBoxCenter(null);
                        }
                        else if(isDeleteMode() && !mIsZoomingBack) {
                            mIsInDeleteMode = false;
                            if (mAnimationTask != null) mAnimationTask.cancel();
                            mAnimationTask = new AnimationTask(mCurrentX, mCurrentY);
                            mAnimationTask.setDuration(50);
                            mAnimationTask.setInterpolator(new LinearInterpolator());
                            mAnimationTask.setAnimationFinishedListener(new OnAnimationFinishedListener() {
                                @Override
                                public void onAnimationFinished() {
                                    mIsZoomingBack = false;
                                }
                            });
                            mAnimationTask.run();
                            mIsZoomingBack = true;
                            mDeleteIcon.animate().scaleX(1f).scaleY(1f).setDuration(100);
                        }
                        else {
                            if(!mIsZoomingBack) {
                                if (mAnimationTask != null) mAnimationTask.cancel();
                                updateIconPosition(mCurrentX, mCurrentY);
                            }
                        }
                        mPrevDragX = event.getRawX();
                        mPrevDragY = event.getRawY();

                        mDragged = mDragged || Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5;
                        if (mDragged) {
                            closeCalculator(false);
                            showDeleteBox();
                        }

                        mDeltaXArray.add(deltaX);
                        mDeltaYArray.add(deltaY);
                        break;
                }
                return true;
            }
        };
        mDraggableIcon = new FloatingView(this);
        mDraggableIcon.setOnTouchListener(dragListener);
        View.inflate(getContext(), R.layout.floating_calculator_icon, mDraggableIcon);
        mParams = addView(mDraggableIcon, 0, 0);
        updateIconPosition(MARGIN_HORIZONTAL, 100);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsDestroyed = true;
        if (mDraggableIcon != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mDraggableIcon);
            mDraggableIcon = null;
        }
        if (mDeleteBoxView != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mDeleteBoxView);
            mDeleteBoxView = null;
        }
        if (mCalcView != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mCalcView);
            mCalcView = null;
        }
        if (mAnimationTask != null) {
            mAnimationTask.cancel();
            mAnimationTask = null;
        }
        ACTIVE_CALCULATOR = null;
    }

    public void openCalculator() {
        if (!mIsCalcOpen) {
            if(mIsZoomingBack) return;
            mIsCalcOpen = true;
            mPrevX = mParams.x;
            mPrevY = mParams.y;
            mAnimationTask = new AnimationTask((int) (getScreenWidth() - mDraggableIcon.getWidth() * 1.5), 100);
            mAnimationTask.setAnimationFinishedListener(new OnAnimationFinishedListener() {
                @Override
                public void onAnimationFinished() {
                    showCalculator();
                }
            });
            mAnimationTask.run();
            Intent intent = new Intent(getContext(), FloatingCalculatorActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    public void closeCalculator() {
        closeCalculator(true);
    }

    public void closeCalculator(boolean returnToOrigin) {
        if (mIsCalcOpen) {
            mIsCalcOpen = false;
            if (returnToOrigin) {
                if(mIsZoomingBack) return;
                mAnimationTask = new AnimationTask(mPrevX, mPrevY);
                mAnimationTask.run();
            }
            mPrevX = -1;
            mPrevY = -1;
            if (FloatingCalculatorActivity.ACTIVE_ACTIVITY != null)
                FloatingCalculatorActivity.ACTIVE_ACTIVITY.finish();
            hideCalculator();
        }
    }

    public void showCalculator() {
        if (mCalcView == null) {
            View child = View.inflate(getContext(), R.layout.floating_calculator, null);
            mCalcView = new FloatingView(getContext());
            mCalcView.addView(child);
            mCalcParams = addView(mCalcView, 0, 0);

            mPersist = new Persist(this);
            mPersist.load();

            mHistory = mPersist.mHistory;

            mDisplay = (CalculatorDisplay) mCalcView.findViewById(R.id.display);
            mDisplay.setEditTextLayout(R.layout.view_calculator_edit_text_floating);
            mDisplay.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    copyContent(mDisplay.getText());
                    return true;
                }
            });

            mLogic = new Logic(this, mHistory, mDisplay);
            mLogic.setDeleteMode(mPersist.getDeleteMode());
            mLogic.setLineLength(mDisplay.getMaxDigits());
            final ImageButton del = (ImageButton) mCalcView.findViewById(R.id.delete);
            del.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mLogic.onClear();
                    return true;
                }
            });
            mListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v instanceof Button) {
                        if (((Button) v).getText().toString().equals("=")) {
                            mLogic.onEnter();
                        } else {
                            mLogic.insert(((Button) v).getText().toString());
                        }
                    } else if (v instanceof ImageButton) {
                        mLogic.onDelete();
                    }
                }
            };
            applyListener(mCalcView);
        } else {
            mCalcView.setVisibility(View.VISIBLE);
        }
        // Adjust calc location
        int screenWidth = getScreenWidth();
        int calcWidth = 4 * (int) getResources().getDimension(R.dimen.floating_window_button_height);
        mCalcParams.x = screenWidth - calcWidth;
        mCalcParams.y = 110 + mDraggableIcon.getHeight();
        if (!mIsDestroyed) mWindowManager.updateViewLayout(mCalcView, mCalcParams);

        // Animate calc in
        View child = mCalcView.getChildAt(0);
        child.setAlpha(0);
        child.animate().setDuration(150).alpha(1).setListener(null);
    }

    public void hideCalculator() {
        if (mCalcView != null) {
            View child = mCalcView.getChildAt(0);
            child.setAlpha(1);
            child.animate().setDuration(150).alpha(0).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }

                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mCalcView.setVisibility(View.GONE);
                }
            });
        }
    }

    private void copyContent(String text) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, text));
        String toastText = String.format(getResources().getString(R.string.text_copied_toast), text);
        Toast.makeText(getContext(), toastText, Toast.LENGTH_SHORT).show();
    }


    private void applyListener(View view) {
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                applyListener(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof Button) {
            view.setOnClickListener(mListener);
        } else if (view instanceof ImageButton) {
            view.setOnClickListener(mListener);
        }
    }

    private float calculateVelocityX() {
        int depreciation = mDeltaXArray.size() + 1;
        float sum = 0;
        for (Float f : mDeltaXArray) {
            depreciation--;
            if (depreciation > 5) continue;
            sum += f / depreciation;
        }
        return sum;
    }

    private float calculateVelocityY() {
        int depreciation = mDeltaYArray.size() + 1;
        float sum = 0;
        for (Float f : mDeltaYArray) {
            depreciation--;
            if (depreciation > 5) continue;
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
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private static interface OnAnimationFinishedListener {
        public void onAnimationFinished();
    }

    private static class FloatingView extends LinearLayout {
        public FloatingView(Context context) {
            super(context);
        }
    }

    // Timer for animation/automatic movement of the tray.
    private class AnimationTask {
        // Ultimate destination coordinates toward which the view will move
        int mDestX;
        int mDestY;
        long mDuration = 350;
        long mStartTime;
        Interpolator mInterpolator;
        long mSteps;
        long mCurrentStep;
        int mDistX;
        int mOrigX;
        int mDistY;
        int mOrigY;
        OnAnimationFinishedListener mAnimationFinishedListener;

        public AnimationTask(int x, int y) {
            setup(x, y);
        }

        public AnimationTask() {
            setup(calculateX(), calculateY());
        }

        private void setup(int x, int y) {
            if(mIsZoomingBack) throw new RuntimeException("Returning to user's finger. Avoid animations while mIsZoomingBack flag is set.");
            mDestX = x;
            mDestY = y;

            mInterpolator = new OvershootInterpolator(1.4f);
            mSteps = (int) (((float) mDuration) / 1000 * ANIMATION_FRAME_RATE);
            mCurrentStep = 1;
            mDistX = mParams.x - mDestX;
            mOrigX = mParams.x;
            mDistY = mParams.y - mDestY;
            mOrigY = mParams.y;
        }

        public void setDuration(long duration) {
            mDuration = duration;
            setup(mDestX, mDestY);
        }

        public long getDuration() {
            return mDuration;
        }

        public long getRemainingDuration() {
            long elapsedTime = System.currentTimeMillis() - mStartTime;
            long remainingDuration = mDuration - elapsedTime;
            if(remainingDuration < 0) remainingDuration = 0;
            return remainingDuration;
        }

        public void setAnimationFinishedListener(OnAnimationFinishedListener l) {
            mAnimationFinishedListener = l;
        }

        public OnAnimationFinishedListener getAnimationFinishedListener() {
            return mAnimationFinishedListener;
        }

        public void setInterpolator(Interpolator interpolator) {
            mInterpolator = interpolator;
        }

        public Interpolator getInterpolator() {
            return mInterpolator;
        }

        private int calculateX() {
            float velocityX = calculateVelocityX();
            int screenWidth = getScreenWidth();
            int destX = (mParams.x + mDraggableIcon.getWidth() / 2 > screenWidth / 2) ? screenWidth - mDraggableIcon.getWidth() - MARGIN_HORIZONTAL : 0 + MARGIN_HORIZONTAL;
            if (Math.abs(velocityX) > 50)
                destX = (velocityX > 0) ? screenWidth - mDraggableIcon.getWidth() - MARGIN_HORIZONTAL : 0 + MARGIN_HORIZONTAL;
            return destX;
        }

        private int calculateY() {
            float velocityY = calculateVelocityY();
            int screenHeight = getScreenHeight();
            int destY = mParams.y + (int) (velocityY * 3);
            if (destY <= 0) destY = MARGIN_VERTICAL;
            if (destY >= screenHeight - mDraggableIcon.getHeight())
                destY = screenHeight - mDraggableIcon.getHeight() - MARGIN_VERTICAL;
            return destY;
        }

        public void run() {
            mStartTime = System.currentTimeMillis();
            for (mCurrentStep = 1; mCurrentStep <= mSteps; mCurrentStep++) {
                long delay = mCurrentStep * mDuration / mSteps;
                final float currentStep = mCurrentStep;
                mAnimationHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Update coordinates of the view
                        float percent = mInterpolator.getInterpolation(currentStep / mSteps);
                        updateIconPosition(mOrigX - (int) (percent * mDistX), mOrigY - (int) (percent * mDistY));

                        // Notify the animation has ended
                        if (currentStep >= mSteps) {
                            if (mAnimationFinishedListener != null)
                                mAnimationFinishedListener.onAnimationFinished();
                        }
                    }
                }, delay);
            }
        }

        public void cancel() {
            mAnimationHandler.removeCallbacksAndMessages(null);
            mAnimationTask = null;
        }
    }
}
