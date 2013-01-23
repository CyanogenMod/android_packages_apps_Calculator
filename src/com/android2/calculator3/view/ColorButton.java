/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android2.calculator3.view;

import java.util.regex.Pattern;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Button;

import com.android2.calculator3.Calculator;
import com.android2.calculator3.EventListener;
import com.android2.calculator3.R;

/**
 * Button with click-animation effect.
 */
class ColorButton extends Button {
    int CLICK_FEEDBACK_COLOR;
    static final int CLICK_FEEDBACK_INTERVAL = 10;
    static final int CLICK_FEEDBACK_DURATION = 350;

    float mTextX;
    float mTextY;
    long mAnimStart;
    EventListener mListener;
    Paint mFeedbackPaint;
    Paint mHintPaint;
    Rect bounds = new Rect();

    public ColorButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        Calculator calc = (Calculator) context;
        init(calc);
        mListener = calc.mListener;
        setOnClickListener(mListener);
        setOnLongClickListener(mListener);
    }

    private void init(Calculator calc) {
        Resources res = getResources();

        CLICK_FEEDBACK_COLOR = res.getColor(R.color.magic_flame);
        mFeedbackPaint = new Paint();
        mFeedbackPaint.setStyle(Style.STROKE);
        mFeedbackPaint.setStrokeWidth(2);
        getPaint().setColor(res.getColor(R.color.button_text));
        mHintPaint = new Paint();
        mHintPaint.setColor(res.getColor(R.color.grey));
        mHintPaint.setTextSize((float) (getTextSize() * .8));

        mAnimStart = -1;
    }

    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        layoutText();
    }

    private void layoutText() {
        Paint paint = getPaint();
        float textWidth = paint.measureText(getText().toString());
        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        float textSize = getTextSize();
        if(textWidth > width) {
            paint.setTextSize(textSize * width / textWidth);
            mTextX = getPaddingLeft();
        }
        else {
            mTextX = (getWidth() - textWidth) / 2;
        }
        mTextY = (getHeight() - paint.ascent() - paint.descent()) / 2;
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int before, int after) {
        layoutText();
    }

    private void drawMagicFlame(int duration, Canvas canvas) {
        int alpha = 255 - 255 * duration / CLICK_FEEDBACK_DURATION;
        int color = CLICK_FEEDBACK_COLOR | (alpha << 24);

        mFeedbackPaint.setColor(color);
        canvas.drawRect(1, 1, getWidth() - 1, getHeight() - 1, mFeedbackPaint);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if(mAnimStart != -1) {
            int animDuration = (int) (System.currentTimeMillis() - mAnimStart);

            if(animDuration >= CLICK_FEEDBACK_DURATION) {
                mAnimStart = -1;
            }
            else {
                drawMagicFlame(animDuration, canvas);
                postInvalidateDelayed(CLICK_FEEDBACK_INTERVAL);
            }
        }
        else if(isPressed()) {
            drawMagicFlame(0, canvas);
        }

        CharSequence text = getText();
        canvas.drawText(text, 0, text.length(), mTextX, mTextY, getPaint());

        CharSequence hint = getHint();
        if(hint != null) {
            String[] exponents = hint.toString().split(Pattern.quote("^"));
            int offsetX = 20;
            int offsetY = 45;
            for(String str : exponents) {
                canvas.drawText(str, 0, str.length(), mTextX + offsetX, mTextY - offsetY, mHintPaint);
                mHintPaint.getTextBounds(str, 0, str.length(), bounds);
                offsetY += 15;
                offsetX += bounds.width();
            }
        }
    }

    public void animateClickFeedback() {
        mAnimStart = System.currentTimeMillis();
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);

        switch(event.getAction()) {
        case MotionEvent.ACTION_UP:
            if(isPressed()) {
                animateClickFeedback();
            }
            else {
                invalidate();
            }
            break;
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_CANCEL:
            mAnimStart = -1;
            invalidate();
            break;
        }

        return result;
    }
}
