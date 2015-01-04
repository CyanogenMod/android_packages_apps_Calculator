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
import android.text.Editable;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import com.android.calculator2.R;
import com.android.calculator2.view.display.EventListener;
import com.xlythe.math.Constants;

public class EqualsImageButton extends ImageButton {
    private static final int[] STATE_EQUALS = { R.attr.state_equals };
    private static final int[] STATE_GRAPH = { R.attr.state_graph };
    private static final int[] STATE_NEXT = { R.attr.state_next };

    public enum State {
        EQUALS, GRAPH, NEXT;
    }

    private State mState = State.EQUALS;

    public EqualsImageButton(Context context) {
        super(context);
        setup();
    }

    public EqualsImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public EqualsImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    public EqualsImageButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup();
    }

    private void setup() {
        setState(State.EQUALS);
    }

    public void setState(State state) {
        mState = state;
        refreshDrawableState();
    }

    public State getState() {
        return mState;
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        int[] state = super.onCreateDrawableState(extraSpace + 1);
        if(mState == null) return state;

        switch(mState) {
            case EQUALS:
                mergeDrawableStates(state, STATE_EQUALS);
                break;
            case GRAPH:
                mergeDrawableStates(state, STATE_GRAPH);
                break;
            case NEXT:
                mergeDrawableStates(state, STATE_NEXT);
                break;
        }
        return state;
    }
}
