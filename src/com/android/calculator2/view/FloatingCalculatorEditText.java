/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.text.Selection;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.Toast;

import com.android.calculator2.R;

public class FloatingCalculatorEditText extends CalculatorEditText {
    public FloatingCalculatorEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public static int LONG_PRESS_TIME = ViewConfiguration.getLongPressTimeout();
    private long mPressedTime;
    private final Handler mHandler = new Handler();
    private Runnable mOnLongPressed = new Runnable() {
        public void run() {
            copyContent(getAdvancedDisplay().getText());
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mPressedTime = System.currentTimeMillis();
            mHandler.postDelayed(mOnLongPressed, LONG_PRESS_TIME);
        }
        if(event.getAction() == MotionEvent.ACTION_UP) {
            if (System.currentTimeMillis() - mPressedTime < LONG_PRESS_TIME) {
                mHandler.removeCallbacks(mOnLongPressed);
            }

            final int offset = getOffsetForPosition(event.getX(), event.getY());
            Selection.setSelection(getText(), offset);
        }

        return true;
    }

    @Override
    public void setDefaultFont() {
        // Do nothing here
    }

    @Override
    public void setFont(String font) {
        // Do nothing here
    }

    private void copyContent(String text) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(
                Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, text));

        String toastText = String.format(
                getContext().getResources().getString(R.string.text_copied_toast), text);
        Toast.makeText(getContext(), toastText, Toast.LENGTH_SHORT).show();
    }
}
