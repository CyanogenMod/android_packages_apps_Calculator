package com.android2.calculator3;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;

public class FloatingCalculator extends Service {
    private static final int ANIMATION_FRAME_RATE = 30; // Animation frame rate per second.

    // View variables
    private WindowManager mWindowManager;
    private FloatingView mDraggableIcon;
    private WindowManager.LayoutParams mParams;

    // Animation variables
    private List<Float> mDeltaXArray;
    private List<Float> mDeltaYArray;
    private Timer mAnimationTimer;
    private AnimationTimerTask mTimerTask;
    private Handler mAnimationHandler = new Handler();

    // Open/Close variables
    int mPrevX = -1;
    int mPrevY = -1;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void addView(FloatingView v) {
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

        mParams.gravity = Gravity.TOP | Gravity.LEFT;
        mParams.x = 0;
        mParams.y = 100;

        mWindowManager.addView(v, mParams);
    }

    @Override
    public void onCreate() {
        super.onCreate();

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
                    if(mTimerTask != null) {
                        mTimerTask.cancel();
                        mAnimationTimer.cancel();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if(!mDragged) {
                        // TODO Open calc
                        if(mPrevX == -1) {
                            openCalculator();
                        }
                        else {
                            closeCalculator();
                        }
                    }
                    else {
                        // Animate the icon
                        mTimerTask = new AnimationTimerTask();
                    }
                    mAnimationTimer = new Timer();
                    mAnimationTimer.schedule(mTimerTask, 0, ANIMATION_FRAME_RATE);
                    break;
                case MotionEvent.ACTION_MOVE:
                    // Calculate position of the whole tray according to the drag, and update layout.
                    float deltaX = event.getRawX() - mPrevDragX;
                    float deltaY = event.getRawY() - mPrevDragY;
                    mParams.x += deltaX;
                    mParams.y += deltaY;
                    mPrevDragX = event.getRawX();
                    mPrevDragY = event.getRawY();
                    mWindowManager.updateViewLayout(mDraggableIcon, mParams);

                    mDragged = mDragged || Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5;
                    if(mDragged) {
                        mPrevX = -1;
                        mPrevY = -1;
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
        addView(mDraggableIcon);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mDraggableIcon != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mDraggableIcon);
            mDraggableIcon = null;
        }
    }

    private void openCalculator() {
        mPrevX = mParams.x;
        mPrevY = mParams.y;
        mTimerTask = new AnimationTimerTask((int) (getResources().getDisplayMetrics().widthPixels - mDraggableIcon.getWidth() * 1.5), 100);
    }

    private void closeCalculator() {
        mTimerTask = new AnimationTimerTask(mPrevX, mPrevY);
        mPrevX = -1;
        mPrevY = -1;
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

    // Timer for animation/automatic movement of the tray.
    private class AnimationTimerTask extends TimerTask {
        // Ultimate destination coordinates toward which the tray will move
        int mDestX;
        int mDestY;
        boolean mOvershoot;

        public AnimationTimerTask(int x, int y) {
            super();

            mDestX = x;
            mDestY = y;
        }

        public AnimationTimerTask() {
            super();

            float velocityX = calculateVelocityX();
            float velocityY = calculateVelocityY();
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            mDestX = (mParams.x + mDraggableIcon.getWidth() / 2 > screenWidth / 2) ? screenWidth : 0;
            if(Math.abs(velocityX) > 50) mDestX = (velocityX > 0) ? screenWidth : 0;
            mDestY = (int) (mParams.y + velocityY * 0.6);
            mDestY = Math.max(mDestY, 0);
        }

        // This function is called after every frame.
        @Override
        public void run() {

            // handler is used to run the function on main UI thread in order to
            // access the layouts and UI elements.
            mAnimationHandler.post(new Runnable() {
                @Override
                public void run() {

                    // Update coordinates of the tray
                    mParams.x = (2 * (mParams.x - mDestX)) / 3 + mDestX;
                    mParams.y = (2 * (mParams.y - mDestY)) / 3 + mDestY;
                    mWindowManager.updateViewLayout(mDraggableIcon, mParams);

                    // Cancel animation when the destination is reached
                    if(Math.abs(mParams.x - mDestX) < 2 && Math.abs(mParams.y - mDestY) < 2) {
                        AnimationTimerTask.this.cancel();
                        mAnimationTimer.cancel();
                    }
                }
            });
        }
    }
}

class FloatingView extends ImageView {
    public FloatingView(Context context) {
        super(context);
    }

    @Override
    protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {}
}
