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

package com.android.calculator2;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * Button with click-animation effect.
 */
class ColorButton extends Button {
    EventListener mListener;

    public ColorButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        Calculator calc = (Calculator) context;
        mListener = calc.mListener;
        setOnClickListener(mListener);
        setOnLongClickListener(mListener);
        setSingleLine();
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        adjustTextSize();
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int before, int after) {
        adjustTextSize();
    }

    private void adjustTextSize() {
        Paint paint = getPaint();
        float textWidth = paint.measureText(getText().toString());
        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        if (textWidth > width) {
            paint.setTextSize(getTextSize() * width / textWidth);
        }
    }

}
