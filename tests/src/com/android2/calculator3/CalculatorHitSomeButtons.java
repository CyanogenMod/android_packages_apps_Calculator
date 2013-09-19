/**
 * Copyright (c) 2008, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.android2.calculator3;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.android2.calculator3.Calculator.LargePanel;
import com.android2.calculator3.Calculator.Panel;
import com.android2.calculator3.Calculator.SmallPanel;
import com.android2.calculator3.view.CalculatorDisplay;
import com.android2.calculator3.view.CalculatorViewPager;

/**
 * Instrumentation tests for poking some buttons
 * 
 */

public class CalculatorHitSomeButtons extends ActivityInstrumentationTestCase2<Calculator> {
    public boolean setup = false;
    private static final String TAG = "CalculatorTests";
    Calculator mActivity = null;
    Instrumentation mInst = null;

    public CalculatorHitSomeButtons() {
        super(Calculator.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mActivity = getActivity();
        mInst = getInstrumentation();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @LargeTest
    public void testPressSomeKeys() {
        Log.v(TAG, "Pressing some keys!");

        // Make sure that we clear the output
        press(KeyEvent.KEYCODE_ENTER);
        press(KeyEvent.KEYCODE_CLEAR);

        // 3 + 4 * 5 => 23
        press(KeyEvent.KEYCODE_3);
        press(KeyEvent.KEYCODE_PLUS);
        press(KeyEvent.KEYCODE_4);
        press(KeyEvent.KEYCODE_9 | KeyEvent.META_SHIFT_ON);
        press(KeyEvent.KEYCODE_5);
        press(KeyEvent.KEYCODE_ENTER);

        assertEquals("23", displayVal());
    }

    @LargeTest
    public void testTapSomeButtons() {
        Log.v(TAG, "Tapping some buttons!");

        // Make sure that we clear the output
        tap(R.id.equal);
        tap(R.id.del);

        // 567 / 3 => 189
        tap(R.id.digit5);
        tap(R.id.digit6);
        tap(R.id.digit7);
        tap(R.id.div);
        tap(R.id.digit3);
        tap(R.id.equal);

        assertEquals("189", displayVal());

        // make sure we can continue calculations also
        // 189 - 789 => -600
        tap(R.id.minus);
        tap(R.id.digit7);
        tap(R.id.digit8);
        tap(R.id.digit9);
        tap(R.id.equal);

        // Careful: the first digit in the expected value is \u2212, not "-" (a
        // hyphen)
        assertEquals(mActivity.getString(R.string.minus) + "600", displayVal());
    }

    @LargeTest
    public void testTapSomeMatrixButtons() {
        Log.v(TAG, "Making some matrices!");

        swipe(Panel.MATRIX);
        swipe(LargePanel.MATRIX);

        // Make sure that we clear the output
        tap(R.id.equal);
        tap(R.id.del);

        // 567 + 3 => 570
        tap(R.id.digit5);
        tap(R.id.digit6);
        tap(R.id.digit7);
        tap(R.id.plus);
        tap(R.id.digit3);
        tap(R.id.equal);

        assertEquals("570", displayVal());
    }

    // helper functions
    private void press(int keycode) {
        mInst.sendKeyDownUpSync(keycode);
    }

    private boolean tap(int id) {
        CalculatorViewPager pager = (CalculatorViewPager) mActivity.findViewById(R.id.panelswitch);
        CalculatorViewPager smallPager = (CalculatorViewPager) mActivity.findViewById(R.id.smallPanelswitch);
        CalculatorViewPager largePager = (CalculatorViewPager) mActivity.findViewById(R.id.largePanelswitch);

        // Phone
        if(pager != null) {
            // Find the view on the current page
            View v = ((CalculatorPageAdapter) pager.getAdapter()).getViewAt(pager.getCurrentItem()).findViewById(id);
            if(v != null) {
                TouchUtils.clickView(this, v);
                return true;
            }
        }
        // Tablet
        else {
            // Find the view on the current pages
            View v = ((CalculatorPageAdapter) smallPager.getAdapter()).getViewAt(smallPager.getCurrentItem()).findViewById(id);
            if(v != null) {
                TouchUtils.clickView(this, v);
                return true;
            }
            v = ((CalculatorPageAdapter) largePager.getAdapter()).getViewAt(largePager.getCurrentItem()).findViewById(id);
            if(v != null) {
                TouchUtils.clickView(this, v);
                return true;
            }
        }

        // Find the view in the entire app (if it wasn't on the pager)
        View view = mActivity.findViewById(id);
        if(view != null) {
            TouchUtils.clickView(this, view);
            return true;
        }
        return false;
    }

    protected boolean swipe(final Panel page) {
        final CalculatorViewPager pager = (CalculatorViewPager) mActivity.findViewById(R.id.panelswitch);

        // On a phone
        if(pager != null) {
            pager.post(new Runnable() {
                @Override
                public void run() {
                    pager.setCurrentItem(page.getOrder());
                }
            });
        }
        return false;
    }

    protected boolean swipe(final SmallPanel page) {
        final CalculatorViewPager smallPager = (CalculatorViewPager) mActivity.findViewById(R.id.smallPanelswitch);

        if(smallPager != null) {
            smallPager.post(new Runnable() {
                @Override
                public void run() {
                    smallPager.setCurrentItem(page.getOrder());
                }
            });
        }
        return false;
    }

    protected boolean swipe(final LargePanel page) {
        final CalculatorViewPager largePager = (CalculatorViewPager) mActivity.findViewById(R.id.largePanelswitch);

        if(largePager != null) {
            largePager.post(new Runnable() {
                @Override
                public void run() {
                    largePager.setCurrentItem(page.getOrder());
                }
            });
        }
        return false;
    }

    private String displayVal() {
        CalculatorDisplay display = (CalculatorDisplay) mActivity.findViewById(R.id.display);
        assertNotNull(display);

        return display.getText();
    }
}
