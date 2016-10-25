/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.Context;
import android.text.Html;
import android.text.InputType;
import android.widget.TextView;

import com.android.calculator2.R;
import com.android.calculator2.view.display.AdvancedDisplay;

public class MatrixTransposeView extends TextView {
    public final static String PATTERN = "^T";

    public MatrixTransposeView(Context context) {
        super(context);
    }

    public MatrixTransposeView(final AdvancedDisplay display) {
        super(display.getContext());
        setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        setText(Html.fromHtml("<sup><small>T</small></sup>"));
        setTextAppearance(display.getContext(), R.style.Theme_Calculator_Display);
        setPadding(0, 0, 0, 0);
    }

    @Override
    public String toString() {
        return PATTERN;
    }
}
