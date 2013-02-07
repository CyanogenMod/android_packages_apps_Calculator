/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Window;
import android.widget.FrameLayout;

import com.android.calculator2.Calculator;
import com.android.calculator2.R;

public class Cling extends FrameLayout {
    public static final int SHOW_CLING_DURATION = 550;
    public static final int DISMISS_CLING_DURATION = 250;

    public static final String SIMPLE_CLING_DISMISSED_KEY = "cling.simple.dismissed";
    public static final String MATRIX_CLING_DISMISSED_KEY = "cling.matrix.dismissed";
    public static final String HEX_CLING_DISMISSED_KEY = "cling.hex.dismissed";
    public static final String GRAPH_CLING_DISMISSED_KEY = "cling.graph.dismissed";

    private Calculator mCalculator;
    private boolean mIsInitialized;
    private Drawable mBackground;
    private Drawable mPunchThroughGraphic;
    private Drawable mHandTouchGraphic;
    private int mPunchThroughGraphicCenterRadius;
    private float mRevealRadius;
    private int[] mPositionData;
    private boolean mShowHand;
    private boolean mDismissed;

    private Paint mErasePaint;

    public Cling(Context context) {
        this(context, null, 0);
    }

    public Cling(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Cling(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init(Calculator c, int[] positionData, float revealRadius, boolean showHand) {
        if(!mIsInitialized) {
            mCalculator = c;
            mPositionData = positionData;
            mShowHand = showHand;
            mDismissed = false;

            Resources r = getContext().getResources();
            mPunchThroughGraphic = r.getDrawable(R.drawable.cling);
            mPunchThroughGraphicCenterRadius = r.getDimensionPixelSize(R.dimen.clingPunchThroughGraphicCenterRadius);
            mRevealRadius = revealRadius;

            mErasePaint = new Paint();
            mErasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
            mErasePaint.setColor(0xFFFFFF);
            mErasePaint.setAlpha(0);

            mIsInitialized = true;
        }
    }

    public void dismiss() {
        mDismissed = true;
    }

    boolean isDismissed() {
        return mDismissed;
    }

    public void cleanup() {
        mBackground = null;
        mPunchThroughGraphic = null;
        mHandTouchGraphic = null;
        mIsInitialized = false;
    }

    private int[] getPunchThroughPosition() {
        if(mPositionData != null) {
            return mPositionData;
        }
        return new int[] { -1, -1, -1 };
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        int[] pos = getPunchThroughPosition();
        double diff = Math.sqrt(Math.pow(event.getX() - pos[0], 2) + Math.pow(event.getY() - pos[1], 2));
        if(diff < mRevealRadius) {
            return false;
        }
        return true;
    };

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if(mIsInitialized) {
            DisplayMetrics metrics = new DisplayMetrics();
            mCalculator.getWindowManager().getDefaultDisplay().getMetrics(metrics);

            // Initialize the draw buffer (to allow punching through)
            Bitmap b = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);

            // Draw the background
            if(mBackground == null) {
                mBackground = getResources().getDrawable(R.drawable.bg_cling);
            }
            if(mBackground != null) {
                mBackground.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
                mBackground.draw(c);
            }
            else {
                c.drawColor(0x99000000);
            }

            int cx = -1;
            int cy = -1;
            int cz = -1;
            float scale = mRevealRadius / mPunchThroughGraphicCenterRadius;
            int dw = (int) (scale * mPunchThroughGraphic.getIntrinsicWidth());
            int dh = (int) (scale * mPunchThroughGraphic.getIntrinsicHeight());

            // Determine where to draw the punch through graphic
            Rect rect = new Rect();
            Window window = ((Activity) getContext()).getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rect);
            int statusBarHeight = rect.top;
            int[] pos = getPunchThroughPosition();
            cx = pos[0];
            cy = pos[1] - statusBarHeight;
            cz = pos[2];
            if(cx > -1 && cy > -1 && scale > 0) {
                c.drawCircle(cx, cy, mRevealRadius, mErasePaint);
                mPunchThroughGraphic.setBounds(cx - dw / 2, cy - dh / 2, cx + dw / 2, cy + dh / 2);
                mPunchThroughGraphic.draw(c);
            }

            // Draw the hand graphic
            if(mShowHand) {
                if(mHandTouchGraphic == null) {
                    mHandTouchGraphic = getResources().getDrawable(R.drawable.hand);
                }
                int offset = cz;
                mHandTouchGraphic.setBounds(cx + offset, cy + offset, cx + mHandTouchGraphic.getIntrinsicWidth() + offset,
                        cy + mHandTouchGraphic.getIntrinsicHeight() + offset);
                mHandTouchGraphic.draw(c);
            }

            canvas.drawBitmap(b, 0, 0, null);
            c.setBitmap(null);
            b = null;
        }

        // Draw the rest of the cling
        super.dispatchDraw(canvas);
    };
}
