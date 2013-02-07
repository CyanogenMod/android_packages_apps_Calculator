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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.text.Html;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.calculator2.view.CalculatorDisplay;
import com.android.calculator2.view.CalculatorViewPager;
import com.android.calculator2.view.Cling;
import com.android.calculator2.view.HistoryLine;
import com.xlythe.slider.Slider;
import com.xlythe.slider.Slider.Direction;
import com.xlythe.slider.Slider.OnSlideListener;

public class Calculator extends Activity implements Logic.Listener, OnClickListener, OnMenuItemClickListener, CalculatorViewPager.OnPageChangeListener {
    public EventListener mListener = new EventListener();
    private CalculatorDisplay mDisplay;
    private Persist mPersist;
    private History mHistory;
    private LinearLayout mHistoryView;
    private ScrollView mHistoryViewParent;
    private Logic mLogic;
    private CalculatorViewPager mPager;
    private CalculatorViewPager mSmallPager;
    private CalculatorViewPager mLargePager;
    private View mClearButton;
    private View mBackspaceButton;
    private View mOverflowMenuButton;
    private Slider mPulldown;
    private Graph mGraph;

    private EquationFormatter mEquationFormatter;

    private boolean clingActive = false;
    private Direction previousDirection = Direction.DOWN;

    public enum Panel {
        GRAPH, FUNCTION, HEX, BASIC, ADVANCED, MATRIX;

        int order;

        public void setOrder(int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    }

    public enum SmallPanel {
        HEX, ADVANCED, FUNCTION;

        int order;

        public void setOrder(int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    }

    public enum LargePanel {
        GRAPH, BASIC, MATRIX;

        int order;

        public void setOrder(int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    }

    private static final String STATE_CURRENT_VIEW = "state-current-view";
    private static final String STATE_CURRENT_VIEW_SMALL = "state-current-view-small";
    private static final String STATE_CURRENT_VIEW_LARGE = "state-current-view-large";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Disable IME for this application
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM, WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        setContentView(R.layout.main);
        mPager = (CalculatorViewPager) findViewById(R.id.panelswitch);
        mSmallPager = (CalculatorViewPager) findViewById(R.id.smallPanelswitch);
        mLargePager = (CalculatorViewPager) findViewById(R.id.largePanelswitch);

        mEquationFormatter = new EquationFormatter();

        if(mClearButton == null) {
            mClearButton = findViewById(R.id.clear);
            mClearButton.setOnClickListener(mListener);
            mClearButton.setOnLongClickListener(mListener);
        }
        if(mBackspaceButton == null) {
            mBackspaceButton = findViewById(R.id.del);
            mBackspaceButton.setOnClickListener(mListener);
            mBackspaceButton.setOnLongClickListener(mListener);
        }

        mPersist = new Persist(this);
        mPersist.load();

        mHistory = mPersist.history;

        mDisplay = (CalculatorDisplay) findViewById(R.id.display);

        mPulldown = (Slider) findViewById(R.id.pulldown);
        mPulldown.setBarHeight(getResources().getDimensionPixelSize(R.dimen.history_bar_height));
        mPulldown.setSlideDirection(Direction.DOWN);
        mPulldown.setOnSlideListener(new OnSlideListener() {
            @Override
            public void onSlide(Direction d) {
                if(!previousDirection.equals(d) && d.equals(Direction.UP)) {
                    setUpHistory();
                }
                previousDirection = d;
            }
        });
        mPulldown.setBackgroundResource(R.color.background);
        mHistoryView = (LinearLayout) mPulldown.findViewById(R.id.history);
        mHistoryViewParent = (ScrollView) mHistoryView.getParent();
        setUpHistory();

        mLogic = new Logic(this, mHistory, mDisplay);
        mLogic.setListener(this);
        if(mPersist.getMode() != null) mLogic.setMode(mPersist.getMode());

        mLogic.setDeleteMode(mPersist.getDeleteMode());
        mLogic.setLineLength(mDisplay.getMaxDigits());

        HistoryAdapter historyAdapter = new HistoryAdapter(this, mHistory, mLogic);
        mHistory.setObserver(historyAdapter);

        mGraph = new Graph(mLogic);

        if(mPager != null) {
            mPager.setAdapter(new PageAdapter(mPager, mListener, mGraph, mLogic));
            mPager.setCurrentItem(state == null ? Panel.BASIC.getOrder() : state.getInt(STATE_CURRENT_VIEW, Panel.BASIC.getOrder()));
            mPager.setOnPageChangeListener(this);
            runCling(false);
        }
        else if(mSmallPager != null && mLargePager != null) {
            // Expanded UI
            mSmallPager.setAdapter(new SmallPageAdapter(mSmallPager, mLogic));
            mLargePager.setAdapter(new LargePageAdapter(mLargePager, mGraph, mLogic));
            mSmallPager.setCurrentItem(state == null ? SmallPanel.ADVANCED.getOrder() : state.getInt(STATE_CURRENT_VIEW_SMALL, SmallPanel.ADVANCED.getOrder()));
            mLargePager.setCurrentItem(state == null ? LargePanel.BASIC.getOrder() : state.getInt(STATE_CURRENT_VIEW_LARGE, LargePanel.BASIC.getOrder()));
            mSmallPager.setOnPageChangeListener(this);
            mLargePager.setOnPageChangeListener(this);
            runCling(false);
        }

        mListener.setHandler(this, mLogic, mPager);
        mDisplay.setOnKeyListener(mListener);

        if(!ViewConfiguration.get(this).hasPermanentMenuKey()) {
            createFakeMenu();
        }

        mLogic.resumeWithHistory();
        updateDeleteMode();

        mPulldown.bringToFront();
    }

    private void updateDeleteMode() {
        if(mLogic.getDeleteMode() == Logic.DELETE_MODE_BACKSPACE) {
            mClearButton.setVisibility(View.GONE);
            mBackspaceButton.setVisibility(View.VISIBLE);
        }
        else {
            mClearButton.setVisibility(View.VISIBLE);
            mBackspaceButton.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem mClearHistory = menu.findItem(R.id.clear_history);
        mClearHistory.setVisible(mPulldown.isSliderOpen());

        MenuItem mShowHistory = menu.findItem(R.id.show_history);
        mShowHistory.setVisible(!mPulldown.isSliderOpen());

        MenuItem mHideHistory = menu.findItem(R.id.hide_history);
        mHideHistory.setVisible(mPulldown.isSliderOpen());

        MenuItem mMatrixPanel = menu.findItem(R.id.matrix);
        if(mMatrixPanel != null) mMatrixPanel.setVisible(!getMatrixVisibility() && CalculatorSettings.matrixPanel(getContext()) && !mPulldown.isSliderOpen());

        MenuItem mGraphPanel = menu.findItem(R.id.graph);
        if(mGraphPanel != null) mGraphPanel.setVisible(!getGraphVisibility() && CalculatorSettings.graphPanel(getContext()) && !mPulldown.isSliderOpen());

        MenuItem mFunctionPanel = menu.findItem(R.id.function);
        if(mFunctionPanel != null) mFunctionPanel.setVisible(!getFunctionVisibility() && CalculatorSettings.functionPanel(getContext())
                && !mPulldown.isSliderOpen());

        MenuItem mBasicPanel = menu.findItem(R.id.basic);
        if(mBasicPanel != null) mBasicPanel.setVisible(!getBasicVisibility() && CalculatorSettings.basicPanel(getContext()) && !mPulldown.isSliderOpen());

        MenuItem mAdvancedPanel = menu.findItem(R.id.advanced);
        if(mAdvancedPanel != null) mAdvancedPanel.setVisible(!getAdvancedVisibility() && CalculatorSettings.advancedPanel(getContext())
                && !mPulldown.isSliderOpen());

        MenuItem mHexPanel = menu.findItem(R.id.hex);
        if(mHexPanel != null) mHexPanel.setVisible(!getHexVisibility() && CalculatorSettings.hexPanel(getContext()) && !mPulldown.isSliderOpen());

        return true;
    }

    private void createFakeMenu() {
        mOverflowMenuButton = findViewById(R.id.overflow_menu);
        if(mOverflowMenuButton != null) {
            mOverflowMenuButton.setVisibility(View.VISIBLE);
            mOverflowMenuButton.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
        case R.id.overflow_menu:
            PopupMenu menu = constructPopupMenu();
            if(menu != null) {
                menu.show();
            }
            break;
        }
    }

    private PopupMenu constructPopupMenu() {
        final PopupMenu popupMenu = new PopupMenu(this, mOverflowMenuButton);
        final Menu menu = popupMenu.getMenu();
        popupMenu.inflate(R.menu.menu);
        popupMenu.setOnMenuItemClickListener(this);
        onPrepareOptionsMenu(menu);
        return popupMenu;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    private boolean getGraphVisibility() {
        if(mPager != null) {
            return mPager.getCurrentItem() == Panel.GRAPH.getOrder() && CalculatorSettings.graphPanel(getContext());
        }
        else if(mLargePager != null) {
            return mLargePager.getCurrentItem() == LargePanel.GRAPH.getOrder() && CalculatorSettings.graphPanel(getContext());
        }
        return false;
    }

    private boolean getFunctionVisibility() {
        // if(mPager != null) {
        // return mPager.getCurrentItem() == Panel.FUNCTION.getOrder() &&
        // CalculatorSettings.functionPanel(getContext());
        // }
        // else if(mSmallPager != null) {
        // return mSmallPager.getCurrentItem() == SmallPanel.FUNCTION.getOrder()
        // && CalculatorSettings.functionPanel(getContext());
        // }
        return false;
    }

    private boolean getBasicVisibility() {
        if(mPager != null) {
            return mPager.getCurrentItem() == Panel.BASIC.getOrder() && CalculatorSettings.basicPanel(getContext());
        }
        else if(mLargePager != null) {
            return mLargePager.getCurrentItem() == LargePanel.BASIC.getOrder() && CalculatorSettings.basicPanel(getContext());
        }
        return false;
    }

    private boolean getAdvancedVisibility() {
        if(mPager != null) {
            return mPager.getCurrentItem() == Panel.ADVANCED.getOrder() && CalculatorSettings.advancedPanel(getContext());
        }
        else if(mSmallPager != null) {
            return mSmallPager.getCurrentItem() == SmallPanel.ADVANCED.getOrder() && CalculatorSettings.advancedPanel(getContext());
        }
        return false;
    }

    private boolean getHexVisibility() {
        if(mPager != null) {
            return mPager.getCurrentItem() == Panel.HEX.getOrder() && CalculatorSettings.hexPanel(getContext());
        }
        else if(mSmallPager != null) {
            return mSmallPager.getCurrentItem() == SmallPanel.HEX.getOrder() && CalculatorSettings.hexPanel(getContext());
        }
        return false;
    }

    private boolean getMatrixVisibility() {
        if(mPager != null) {
            return mPager.getCurrentItem() == Panel.MATRIX.getOrder() && CalculatorSettings.matrixPanel(getContext());
        }
        else if(mLargePager != null) {
            return mLargePager.getCurrentItem() == LargePanel.MATRIX.getOrder() && CalculatorSettings.matrixPanel(getContext());
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case R.id.clear_history:
            mHistory.clear();
            mLogic.onClear();
            mHistoryView.removeAllViews();
            break;

        case R.id.show_history:
            mPulldown.animateSliderOpen();
            break;

        case R.id.hide_history:
            mPulldown.animateSliderClosed();
            break;

        case R.id.basic:
            if(!getBasicVisibility()) {
                if(mPager != null) mPager.setCurrentItem(Panel.BASIC.getOrder());
                else if(mLargePager != null) mLargePager.setCurrentItem(LargePanel.BASIC.getOrder());
            }
            break;

        case R.id.advanced:
            if(!getAdvancedVisibility()) {
                if(mPager != null) mPager.setCurrentItem(Panel.ADVANCED.getOrder());
                else if(mSmallPager != null) mSmallPager.setCurrentItem(SmallPanel.ADVANCED.getOrder());
            }
            break;

        case R.id.function:
            if(!getFunctionVisibility()) {
                if(mPager != null) mPager.setCurrentItem(Panel.FUNCTION.getOrder());
                else if(mSmallPager != null) mSmallPager.setCurrentItem(SmallPanel.FUNCTION.getOrder());
            }
            break;

        case R.id.graph:
            if(!getGraphVisibility()) {
                if(mPager != null) mPager.setCurrentItem(Panel.GRAPH.getOrder());
                else if(mLargePager != null) mLargePager.setCurrentItem(LargePanel.GRAPH.getOrder());
            }
            break;

        case R.id.matrix:
            if(!getMatrixVisibility()) {
                if(mPager != null) mPager.setCurrentItem(Panel.MATRIX.getOrder());
                else if(mLargePager != null) mLargePager.setCurrentItem(LargePanel.MATRIX.getOrder());
            }
            break;

        case R.id.hex:
            if(!getHexVisibility()) {
                if(mPager != null) mPager.setCurrentItem(Panel.HEX.getOrder());
                else if(mSmallPager != null) mSmallPager.setCurrentItem(SmallPanel.HEX.getOrder());
            }
            break;

        case R.id.settings:
            Intent intent = new Intent(this, Preferences.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);

        if(mPager != null) {
            state.putInt(STATE_CURRENT_VIEW, mPager.getCurrentItem());
        }

        if(mSmallPager != null) {
            state.putInt(STATE_CURRENT_VIEW_SMALL, mSmallPager.getCurrentItem());
        }

        if(mLargePager != null) {
            state.putInt(STATE_CURRENT_VIEW_LARGE, mLargePager.getCurrentItem());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mLogic.updateHistory();
        mPersist.setDeleteMode(mLogic.getDeleteMode());
        mPersist.setMode(mLogic.getMode());
        mPersist.save();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if(keyCode == KeyEvent.KEYCODE_BACK && mPulldown.isSliderOpen() && !clingActive) {
            mPulldown.animateSliderClosed();
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK && mPager != null && !getBasicVisibility() && CalculatorSettings.basicPanel(getContext()) && !clingActive) {
            mPager.setCurrentItem(Panel.BASIC.getOrder());
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK && mSmallPager != null && mLargePager != null && !(getAdvancedVisibility() && getBasicVisibility())
                && CalculatorSettings.basicPanel(getContext()) && CalculatorSettings.advancedPanel(getContext()) && !clingActive) {
            mSmallPager.setCurrentItem(SmallPanel.ADVANCED.getOrder());
            mLargePager.setCurrentItem(LargePanel.BASIC.getOrder());
            return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    @Override
    public void onDeleteModeChange() {
        updateDeleteMode();
    }

    private void setUpHistory() {
        mHistoryView.removeAllViews();
        for(HistoryEntry he : mHistory.mEntries) {
            if(!he.getBase().isEmpty()) {
                HistoryLine entry = (HistoryLine) View.inflate(getContext(), R.layout.history_entry, null);
                entry.setHistoryEntry(he);
                entry.setHistory(mHistory);
                TextView base = (TextView) entry.findViewById(R.id.base);
                base.setText(Html.fromHtml(mEquationFormatter.insertSupscripts(he.getBase())));
                TextView edited = (TextView) entry.findViewById(R.id.edited);
                edited.setText(he.getEdited());
                mHistoryView.addView(entry);
            }
        }
        mHistoryViewParent.post(new Runnable() {
            @Override
            public void run() {
                mHistoryViewParent.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private Context getContext() {
        return Calculator.this;
    }

    /* Cling related */
    private boolean isClingsEnabled() {
        // disable clings when running in a test harness
        if(ActivityManager.isRunningInTestHarness()) return false;
        return true;
    }

    private Cling initCling(int clingId, int[] positionData, float revealRadius, boolean showHand, boolean animate) {
        if(mPager != null) mPager.setPagingEnabled(false);
        if(mSmallPager != null) mSmallPager.setPagingEnabled(false);
        if(mLargePager != null) mLargePager.setPagingEnabled(false);
        clingActive = true;

        Cling cling = (Cling) findViewById(clingId);
        if(cling != null) {
            cling.init(this, positionData, revealRadius, showHand);
            cling.setVisibility(View.VISIBLE);
            cling.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            if(animate) {
                cling.buildLayer();
                cling.setAlpha(0f);
                cling.animate().alpha(1f).setInterpolator(new AccelerateInterpolator()).setDuration(Cling.SHOW_CLING_DURATION).setStartDelay(0).start();
            }
            else {
                cling.setAlpha(1f);
            }
        }
        return cling;
    }

    private void dismissCling(final Cling cling, final String flag, int duration) {
        if(mPager != null) mPager.setPagingEnabled(true);
        if(mSmallPager != null) mSmallPager.setPagingEnabled(true);
        if(mLargePager != null) mLargePager.setPagingEnabled(true);
        clingActive = false;

        if(cling != null) {
            cling.dismiss();
            ObjectAnimator anim = ObjectAnimator.ofFloat(cling, "alpha", 0f);
            anim.setDuration(duration);
            anim.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    cling.setVisibility(View.GONE);
                    cling.cleanup();
                    CalculatorSettings.saveKey(getContext(), flag, true);
                };
            });
            anim.start();
        }
    }

    private void removeCling(int id) {
        if(mPager != null) mPager.setPagingEnabled(true);
        if(mSmallPager != null) mSmallPager.setPagingEnabled(true);
        if(mLargePager != null) mLargePager.setPagingEnabled(true);
        clingActive = false;

        final View cling = findViewById(id);
        if(cling != null) {
            final ViewGroup parent = (ViewGroup) cling.getParent();
            parent.post(new Runnable() {
                @Override
                public void run() {
                    parent.removeView(cling);
                }
            });
        }
    }

    public void showFirstRunSimpleCling(boolean animate) {
        // Enable the clings only if they have not been dismissed before
        if(isClingsEnabled() && !CalculatorSettings.isDismissed(getContext(), Cling.SIMPLE_CLING_DISMISSED_KEY)) {
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int[] location = new int[3];
            location[0] = 0;
            location[1] = size.y / 2;
            location[2] = 10;
            initCling(R.id.simple_cling, location, 0, true, animate);
        }
        else {
            removeCling(R.id.simple_cling);
        }
    }

    public void showFirstRunMatrixCling(boolean animate) {
        // Enable the clings only if they have not been dismissed before
        if(isClingsEnabled() && !CalculatorSettings.isDismissed(getContext(), Cling.MATRIX_CLING_DISMISSED_KEY)) {
            View v;
            if(mPager != null) v = ((PageAdapter) mPager.getAdapter()).mMatrixPage.findViewById(R.id.matrix);
            else if(mLargePager != null) v = ((LargePageAdapter) mLargePager.getAdapter()).mMatrixPage.findViewById(R.id.matrix);
            else v = null;
            int[] location = new int[3];

            v.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onClick(v);
                    dismissMatrixCling(v);
                    v.setOnClickListener(mListener);
                }
            });

            v.getLocationOnScreen(location);
            location[0] = location[0] + v.getWidth() / 2;
            location[1] = location[1] + v.getHeight() / 2;
            location[2] = -1;
            initCling(R.id.matrix_cling, location, v.getWidth() / 2, false, animate);
        }
        else {
            removeCling(R.id.matrix_cling);
        }
    }

    public void showFirstRunHexCling(boolean animate) {
        // Enable the clings only if they have not been dismissed before
        if(isClingsEnabled() && !CalculatorSettings.isDismissed(getContext(), Cling.HEX_CLING_DISMISSED_KEY)) {
            initCling(R.id.hex_cling, null, 0, false, animate);
        }
        else {
            removeCling(R.id.hex_cling);
        }
    }

    public void showFirstRunGraphCling(boolean animate) {
        // Enable the clings only if they have not been dismissed before
        if(isClingsEnabled() && !CalculatorSettings.isDismissed(getContext(), Cling.GRAPH_CLING_DISMISSED_KEY)) {
            initCling(R.id.graph_cling, null, 0, false, animate);
        }
        else {
            removeCling(R.id.graph_cling);
        }
    }

    public void dismissSimpleCling(View v) {
        Cling cling = (Cling) findViewById(R.id.simple_cling);
        dismissCling(cling, Cling.SIMPLE_CLING_DISMISSED_KEY, Cling.DISMISS_CLING_DURATION);
    }

    public void dismissMatrixCling(View v) {
        Cling cling = (Cling) findViewById(R.id.matrix_cling);
        dismissCling(cling, Cling.MATRIX_CLING_DISMISSED_KEY, Cling.DISMISS_CLING_DURATION);
    }

    public void dismissHexCling(View v) {
        Cling cling = (Cling) findViewById(R.id.hex_cling);
        dismissCling(cling, Cling.HEX_CLING_DISMISSED_KEY, Cling.DISMISS_CLING_DURATION);
    }

    public void dismissGraphCling(View v) {
        Cling cling = (Cling) findViewById(R.id.graph_cling);
        dismissCling(cling, Cling.GRAPH_CLING_DISMISSED_KEY, Cling.DISMISS_CLING_DURATION);
    }

    private void runCling(boolean animate) {
        if(getBasicVisibility()) {
            showFirstRunSimpleCling(animate);
        }
        if(getMatrixVisibility()) {
            showFirstRunMatrixCling(animate);
        }
        if(getHexVisibility()) {
            showFirstRunHexCling(animate);
        }
        if(getGraphVisibility()) {
            showFirstRunGraphCling(animate);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if(state == 0) runCling(true);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageSelected(int position) {}
}
