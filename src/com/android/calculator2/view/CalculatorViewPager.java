/*
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2011 The Android Open Source Project
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

import java.util.List;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.android.calculator2.CalculatorPageAdapter;
import com.android.calculator2.CalculatorSettings;
import com.android.calculator2.Page;
import com.android.calculator2.Page.NormalPanel;

public class CalculatorViewPager extends ViewPager {
    // Usually we use a huge constant, but ViewPager crashes when the size is too big
    public static int MAX_SIZE_CONSTANT = 100;
    private boolean mIsEnabled;

    public CalculatorViewPager(Context context) {
        this(context, null);
    }

    public CalculatorViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        mIsEnabled = true;
    }

    /**
     * ViewPager inherits ViewGroup's default behavior of delayed clicks
     * on its children, but in order to make the calc buttons more
     * responsive we disable that here.
     */
    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mIsEnabled) {
            return super.onTouchEvent(event);
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mIsEnabled) {
            return super.onInterceptTouchEvent(event);
        }

        return false;
    }

    public boolean getPagingEnabled() {
        return mIsEnabled;
    }

    public void setPagingEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    public void scrollToMiddle() {
        if (CalculatorSettings.useInfiniteScrolling(getContext())) {
            List<Page> pages = ((CalculatorPageAdapter) getAdapter()).getPages();
            if (pages.size() != 0) {
                int halfwayDownTheInfiniteList = (MAX_SIZE_CONSTANT / pages.size()) / 2
                        * pages.size()
                        + Page.getOrder(pages, new Page(getContext(), NormalPanel.BASIC));
                setCurrentItem(halfwayDownTheInfiniteList);
            }
        }
    }
}
