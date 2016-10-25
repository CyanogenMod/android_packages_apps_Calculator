/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

import com.android.calculator2.util.DigitLabelHelper;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class CalculatorNumericPadLayout extends CalculatorPadLayout {

    public CalculatorNumericPadLayout(Context context) {
        this(context, null);
    }

    public CalculatorNumericPadLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalculatorNumericPadLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        DigitLabelHelper.getInstance().getTextForDigits(getContext(),
                new DigitLabelHelper.DigitLabelHelperCallback() {
                    @Override
                    public void setDigitText(int id, String text) {
                        Button button = (Button) findViewById(id);
                        button.setText(text);
                    }
                });
    }
}

