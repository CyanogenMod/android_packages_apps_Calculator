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

package com.android2.calculator3;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

class ResizingTextView extends TextView {
    private Paint mPaint;
    private Rect mRect = new Rect();

    public ResizingTextView(Context context) {
        super(context);
        initialise();
    }

    public ResizingTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialise();
    }

    private void initialise() {
        mPaint = new Paint();
        mPaint.set(getPaint());
    }

    private void refitText(String text, int textWidth, int textHeight) {
        if(textWidth <= 0 || textHeight <= 0) return;

        int targetWidth = textWidth - getPaddingLeft() - getPaddingRight();
        int targetHeight = textHeight - getPaddingTop() - getPaddingBottom();

        float high = targetHeight;
        float low = 0;
        final float threshold = 0.5f;

        mPaint.set(getPaint());

        while((high - low) > threshold) {
            float size = (high + low) / 2;
            mPaint.setTextSize(size);
            mPaint.getTextBounds(text, 0, text.length(), mRect);
            if(mRect.width() >= targetWidth) {
                // Too big
                high = size;
            }
            else if(mRect.height() >= targetHeight) {
                // Too big
                high = size;
            }
            else {
                // Too small
                low = size;
            }
        }
        // Use lo so that we undershoot rather than overshoot
        setTextSize(TypedValue.COMPLEX_UNIT_PX, low);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        refitText(getText().toString(), parentWidth, parentHeight);
        setMeasuredDimension(parentWidth, parentHeight);
    }

    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int before, final int after) {
        refitText(text.toString(), getWidth(), getHeight());
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        if(w != oldw || h != oldh) {
            refitText(getText().toString(), w, h);
        }
    }
}