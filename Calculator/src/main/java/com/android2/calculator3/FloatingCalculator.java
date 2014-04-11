package com.android2.calculator3;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.animation.Animator;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android2.calculator3.view.CalculatorDisplay;

import org.javia.arity.SyntaxException;

public class FloatingCalculator extends Service {
    public static FloatingCalculator ACTIVE_CALCULATOR;
    private static final int ANIMATION_FRAME_RATE = 120; // Animation frame rate per second.
    private static int MARGIN; // Margin around the phone.
    private static int DELETE_BOX_WIDTH;
    private static int DELETE_BOX_HEIGHT;

    // View variables
    private WindowManager mWindowManager;
    private FloatingView mDraggableIcon;
    private WindowManager.LayoutParams mParams;
    private FloatingCalc mCalcView;
    private FloatingCalc mDeleteBoxView;
    private boolean mDeleteBoxVisible = false;

    // Animation variables
    private List<Float> mDeltaXArray;
    private List<Float> mDeltaYArray;
    private AnimationTask mAnimationTask;
    private Handler mAnimationHandler = new Handler();
    private OnAnimationFinishedListener mAnimationFinishedListener;

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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private WindowManager.LayoutParams addView(View v, int x, int y) {
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = x;
        params.y = y;

        mWindowManager.addView(v, params);

        return params;
    }

    private void updateIconPosition(int x, int y) {
        mParams.x = x;
        mParams.y = y;
        if(isDeleteMode()) showDeleteBox();
        else hideDeleteBox();
        mWindowManager.updateViewLayout(mDraggableIcon, mParams);
    }

    private boolean isDeleteMode() {
        int screenWidth = getScreenWidth();
        int screenHeight = getScreenHeight();
        int boxWidth = (int) getResources().getDimension(R.dimen.floating_window_delete_box_width);
        int boxHeight = (int) getResources().getDimension(R.dimen.floating_window_delete_box_height);
        boolean horz = mParams.x > (screenWidth/2-boxWidth/2) && mParams.x < (screenWidth/2+boxWidth/2);
        boolean vert = mParams.y > (screenHeight-boxHeight);

        return horz && vert;
    }

    private void showDeleteBox() {
        if(!mDeleteBoxVisible) {
            mDeleteBoxVisible = true;
            if (mDeleteBoxView == null) {
                View child = View.inflate(getContext(), R.layout.floating_calculator_delete_box, null);
                mDeleteBoxView = new FloatingCalc(getContext());
                mDeleteBoxView.addView(child);
                int screenWidth = getScreenWidth();
                int screenHeight = getScreenHeight();
                int boxWidth = (int) getResources().getDimension(R.dimen.floating_window_delete_box_width);
                int boxHeight = (int) getResources().getDimension(R.dimen.floating_window_delete_box_height);
                addView(mDeleteBoxView, screenWidth / 2 - boxWidth / 2, screenHeight - boxHeight);
            } else {
                mDeleteBoxView.setVisibility(View.VISIBLE);
            }
            View child = mDeleteBoxView.getChildAt(0);
            child.setAlpha(0);
            child.animate().setDuration(150).alpha(1).setListener(null);
        }
    }

    public void hideDeleteBox(){
        if(mDeleteBoxVisible) {
            mDeleteBoxVisible = false;
            if (mDeleteBoxView != null) {
                View child = mDeleteBoxView.getChildAt(0);
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
                        mDeleteBoxView.setVisibility(View.GONE);
                    }
                });
            }
        }
    }

    private void updateIconPositionByDelta(int deltaX, int deltaY) {
        updateIconPosition(mParams.x + deltaX, mParams.y + deltaY);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ACTIVE_CALCULATOR = this;
        MARGIN = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
        DELETE_BOX_WIDTH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
        DELETE_BOX_HEIGHT = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 75, getResources().getDisplayMetrics());

        OnTouchListener dragListener = new OnTouchListener() {
            float mPrevDragX;
            float mPrevDragY;

            boolean mDragged;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mPrevDragX = event.getRawX();
                    mPrevDragY = event.getRawY();

                    mDragged = false;

                    mDeltaXArray = new LinkedList<Float>();
                    mDeltaYArray = new LinkedList<Float>();

                    // Cancel any currently running animations
                    if(mAnimationTask != null) {
                        mAnimationTask.cancel();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if(mDeleteBoxVisible) {
                        // Kill the service
                        stopSelf();
                    }
                    else if(!mDragged) {
                        if(mPrevX == -1) {
                            openCalculator();
                        }
                        else {
                            closeCalculator();
                        }
                    }
                    else {
                        // Animate the icon
                        mAnimationTask = new AnimationTask();
                        mAnimationTask.run();
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    // Calculate position of the whole tray according to the drag, and update layout.
                    float deltaX = event.getRawX() - mPrevDragX;
                    float deltaY = event.getRawY() - mPrevDragY;
                    updateIconPositionByDelta((int) deltaX, (int) deltaY);
                    mPrevDragX = event.getRawX();
                    mPrevDragY = event.getRawY();

                    mDragged = mDragged || Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5;
                    if(mDragged) {
                        closeCalculator(false);
                    }

                    mDeltaXArray.add(deltaX);
                    mDeltaYArray.add(deltaY);
                    break;
                }
                return true;
            }
        };
        mDraggableIcon = new FloatingView(this);
        mDraggableIcon.setImageResource(R.drawable.ic_launcher_calculator);
        mDraggableIcon.setOnTouchListener(dragListener);
        mParams = addView(mDraggableIcon, MARGIN, 100);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mDraggableIcon != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mDraggableIcon);
            mDraggableIcon = null;
        }
        if(mDeleteBoxView != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mDeleteBoxView);
            mDeleteBoxView = null;
        }
        if(mCalcView != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mCalcView);
            mCalcView = null;
        }
        ACTIVE_CALCULATOR = null;
    }

    public void openCalculator() {
        if(!mIsCalcOpen) {
            mIsCalcOpen = true;
            mPrevX = mParams.x;
            mPrevY = mParams.y;
            mAnimationTask = new AnimationTask((int) (getScreenWidth() - mDraggableIcon.getWidth() * 1.5), 100);
            mAnimationTask.run();
            Intent intent = new Intent(getContext(), FloatingCalculatorActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            mAnimationFinishedListener = new OnAnimationFinishedListener() {
                @Override
                public void onAnimationFinished() {
                    showCalculator();
                }
            };
        }
    }

    public void closeCalculator() {
        closeCalculator(true);
    }

    public void closeCalculator(boolean returnToOrigin) {
        if(mIsCalcOpen) {
            mIsCalcOpen = false;
            if (returnToOrigin) {
                mAnimationTask = new AnimationTask(mPrevX, mPrevY);
                mAnimationTask.run();
            }
            mPrevX = -1;
            mPrevY = -1;
            if (FloatingCalculatorActivity.ACTIVE_ACTIVITY != null)
                FloatingCalculatorActivity.ACTIVE_ACTIVITY.finish();
            hideCalculator();
            mAnimationFinishedListener = null;
        }
    }

    public void showCalculator() {
        if(mCalcView == null) {
            View child = View.inflate(getContext(), R.layout.floating_calculator, null);
            mCalcView = new FloatingCalc(getContext());
            mCalcView.addView(child);
            int screenWidth = getScreenWidth();
            int calcWidth = 4*(int) getResources().getDimension(R.dimen.floating_window_button_height);
            addView(mCalcView, screenWidth - calcWidth, 100 + mDraggableIcon.getHeight());

            mPersist = new Persist(this);
            mPersist.load();

            mHistory = mPersist.mHistory;

            mDisplay = (CalculatorDisplay) mCalcView.findViewById(R.id.display);
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
                    if(v instanceof Button) {
                        if(((Button) v).getText().toString().equals("=")){
                            mLogic.onEnter();
                        }
                        else {
                            mLogic.insert(((Button) v).getText().toString());
                        }
                    }
                    else if(v instanceof ImageButton) {
                        mLogic.onDelete();
                    }
                }
            };
            applyListener(mCalcView);
        }
        else {
            mCalcView.setVisibility(View.VISIBLE);
        }
        View child = mCalcView.getChildAt(0);
        child.setAlpha(0);
        child.animate().setDuration(150).alpha(1).setListener(null);
    }

    public void hideCalculator(){
        if(mCalcView != null) {
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
        if(view instanceof ViewGroup) {
            for(int i=0;i<((ViewGroup) view).getChildCount();i++){
                applyListener(((ViewGroup) view).getChildAt(i));
            }
        }
        else if(view instanceof Button) {
            view.setOnClickListener(mListener);
        }
        else if(view instanceof ImageButton) {
            view.setOnClickListener(mListener);
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
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    // Timer for animation/automatic movement of the tray.
    private class AnimationTask {
        // Ultimate destination coordinates toward which the view will move
        int mDestX;
        int mDestY;
        float mVelocityY;
        long mDuration = 250;
        Interpolator mInterpolator;
        long mSteps;
        long mCurrentStep;
        int mDistX;
        int mOrigX;
        int mDistY;
        int mOrigY;

        public AnimationTask(int x, int y) {
            mDestX = x;
            mDestY = y;

            mInterpolator = new OvershootInterpolator(1.2f);
            mSteps = (int) (((float)mDuration) / 1000 * ANIMATION_FRAME_RATE);
            mCurrentStep = 1;
            mDistX = mParams.x - mDestX;
            mOrigX = mParams.x;
            mDistY = mParams.y - mDestY;
            mOrigY = mParams.y;
        }

        public AnimationTask() {
            float velocityX = calculateVelocityX();
            mVelocityY = calculateVelocityY();
            int screenWidth = getScreenWidth();
            int screenHeight = getScreenHeight();
            mDestX = (mParams.x + mDraggableIcon.getWidth() / 2 > screenWidth / 2) ? screenWidth-mDraggableIcon.getWidth() - MARGIN : 0 + MARGIN;
            if(Math.abs(velocityX) > 50) mDestX = (velocityX > 0) ? screenWidth-mDraggableIcon.getWidth() - MARGIN : 0 + MARGIN;
            mDestY = mParams.y + (int) (mVelocityY * 3);
            if(mDestY <= 0) mDestY = MARGIN;
            if(mDestY >= screenHeight-mDraggableIcon.getHeight()) mDestY = screenHeight-mDraggableIcon.getHeight()-MARGIN;

            mInterpolator = new OvershootInterpolator(1.2f);
            mSteps = (int) (((float)mDuration) / 1000 * ANIMATION_FRAME_RATE);
            mCurrentStep = 1;
            mOrigX = mParams.x;
            mDistX = mOrigX - mDestX;
            mOrigY = mParams.y;
            mDistY = mOrigY - mDestY;
        }

        public void run() {
            for(mCurrentStep=1;mCurrentStep<=mSteps;mCurrentStep++) {
                long delay = mCurrentStep * mDuration / mSteps;
                final float currentStep = mCurrentStep;
                mAnimationHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Update coordinates of the view
                        float percent = mInterpolator.getInterpolation(currentStep/mSteps);
                        updateIconPosition(mOrigX - (int) (percent * mDistX), mOrigY - (int) (percent * mDistY));

                        // Notify the animation has ended
                        if(currentStep >= mSteps) {
                            if(mAnimationFinishedListener != null) mAnimationFinishedListener.onAnimationFinished();
                        }
                    }
                }, delay);
            }
        }

        public void cancel() {
            mAnimationHandler.removeCallbacksAndMessages(null);
        }
    }

    private static class FloatingView extends ImageView {
        public FloatingView(Context context) {
            super(context);
        }

        @Override
        protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {}
    }

    private static class FloatingCalc extends LinearLayout {
        public FloatingCalc(Context context) {
            super(context);
        }
    }

    private static interface OnAnimationFinishedListener {
        public void onAnimationFinished();
    }
}
