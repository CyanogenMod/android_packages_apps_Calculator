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

package com.android.calculator2.view;

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

import com.android.calculator2.Calculator;
import com.android.calculator2.EventListener;
import com.android.calculator2.R;

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
    Paint mHintPaint = new Paint();
    Rect bounds = new Rect();
    float mTextSize = 0f;

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
        mHintPaint.setColor(res.getColor(R.color.button_hint_text));

        mAnimStart = -1;
    }

    private void layoutText() {
        Paint paint = getPaint();
        if(mTextSize != 0f) paint.setTextSize(mTextSize);
        float textWidth = paint.measureText(getText().toString());
        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        float textSize = getTextSize();
        if(textWidth > width) {
            paint.setTextSize(textSize * width / textWidth);
            mTextX = getPaddingLeft();
            mTextSize = textSize;
        }
        else {
            mTextX = (getWidth() - textWidth) / 2;
        }
        mTextY = (getHeight() - paint.ascent() - paint.descent()) / 2;
        if(mHintPaint != null) mHintPaint.setTextSize(paint.getTextSize() * 0.8f);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int before, int after) {
        layoutText();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if(changed) layoutText();
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

        CharSequence hint = getHint();
        if(hint != null) {
            String[] exponents = hint.toString().split(Pattern.quote("^"));
            int offsetX = getContext().getResources().getDimensionPixelSize(R.dimen.button_hint_offset_x);
            int offsetY = (int) ((mTextY + getContext().getResources().getDimensionPixelSize(R.dimen.button_hint_offset_y) - getTextHeight(mHintPaint,
                    hint.toString())) / 2)
                    - getPaddingTop();

            float textWidth = mHintPaint.measureText(hint.toString());
            float width = getWidth() - getPaddingLeft() - getPaddingRight() - mTextX - offsetX;
            float textSize = mHintPaint.getTextSize();
            if(textWidth > width) {
                mHintPaint.setTextSize(textSize * width / textWidth);
            }

            for(String str : exponents) {
                if(str == exponents[0]) {
                    canvas.drawText(str, 0, str.length(), mTextX + offsetX, mTextY - offsetY, mHintPaint);
                    offsetY += getContext().getResources().getDimensionPixelSize(R.dimen.button_hint_exponent_jump);
                    offsetX += mHintPaint.measureText(str);
                }
                else {
                    canvas.drawText(str, 0, str.length(), mTextX + offsetX, mTextY - offsetY, mHintPaint);
                    offsetY += getContext().getResources().getDimensionPixelSize(R.dimen.button_hint_exponent_jump);
                    offsetX += mHintPaint.measureText(str);
                }
            }
        }

        CharSequence text = getText();
        canvas.drawText(text, 0, text.length(), mTextX, mTextY, getPaint());
    }

    private int getTextHeight(Paint paint, String text) {
        mHintPaint.getTextBounds(text, 0, text.length(), bounds);
        int height = bounds.height();
        String[] exponents = text.split(Pattern.quote("^"));
        for(int i = 1; i < exponents.length; i++) {
            height += getContext().getResources().getDimensionPixelSize(R.dimen.button_hint_exponent_jump);
        }
        return height;
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
