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

import org.achartengine.GraphicalView;

import com.xlythe.slider.Slider;
import com.xlythe.slider.Slider.Direction;
import com.xlythe.slider.Slider.OnSlideListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

public class Calculator extends Activity implements PanelSwitcher.Listener, Logic.Listener,
        OnClickListener, OnMenuItemClickListener, OnLongClickListener {
    EventListener mListener = new EventListener();
    private CalculatorDisplay mDisplay;
    private Persist mPersist;
    private History mHistory;
    private LinearLayout mHistoryView;
    private Logic mLogic;
    private ViewPager mPager;
    private ViewPager mSmallPager;
    private ViewPager mLargePager;
    private View mClearButton;
    private View mBackspaceButton;
    private View mOverflowMenuButton;
    private Slider mPulldown;
    private Graph mGraph;

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

    private static final String LOG_TAG = "Calculator";
    private static final boolean LOG_ENABLED = false;
    private static final String STATE_CURRENT_VIEW = "state-current-view";
    private static final String STATE_CURRENT_VIEW_SMALL = "state-current-view-small";
    private static final String STATE_CURRENT_VIEW_LARGE = "state-current-view-large";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Disable IME for this application
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        setContentView(R.layout.main);
        mPager = (ViewPager) findViewById(R.id.panelswitch);
        mSmallPager = (ViewPager) findViewById(R.id.smallPanelswitch);
        mLargePager = (ViewPager) findViewById(R.id.largePanelswitch);

        if (mClearButton == null) {
            mClearButton = findViewById(R.id.clear);
            mClearButton.setOnClickListener(mListener);
            mClearButton.setOnLongClickListener(mListener);
        }
        if (mBackspaceButton == null) {
            mBackspaceButton = findViewById(R.id.del);
            mBackspaceButton.setOnClickListener(mListener);
            mBackspaceButton.setOnLongClickListener(mListener);
        }

        mPersist = new Persist(this);
        mPersist.load();

        mHistory = mPersist.history;

        mDisplay = (CalculatorDisplay) findViewById(R.id.display);

        mPulldown = (Slider) findViewById(R.id.pulldown);
        int barHeight = getResources().getInteger(R.integer.barHeight);
        mPulldown.setBarHeight((int) LogicalDensity.convertDpToPixel(barHeight, this));
        mPulldown.setSlideDirection(Direction.DOWN);
        mPulldown.setOnSlideListener(new OnSlideListener() {
            @Override
            public void onSlide(Direction d) {
                if(d.equals(Direction.UP)) {
                    setUpHistory();
                }
            }
        });
        mPulldown.setBackgroundResource(R.color.background);
        mHistoryView = (LinearLayout) mPulldown.findViewById(R.id.history);

        mLogic = new Logic(this, mHistory, mDisplay);
        mLogic.setListener(this);
        if(mPersist.getMode() != null) mLogic.setMode(mPersist.getMode());

        mLogic.setDeleteMode(mPersist.getDeleteMode());
        mLogic.setLineLength(mDisplay.getMaxDigits());

        HistoryAdapter historyAdapter = new HistoryAdapter(this, mHistory, mLogic);
        mHistory.setObserver(historyAdapter);

        if (mPager != null) {
            mPager.setAdapter(new PageAdapter(mPager));
            mPager.setCurrentItem(state == null ? Panel.BASIC.getOrder() : state.getInt(STATE_CURRENT_VIEW, Panel.BASIC.getOrder()));
        }
        else if (mSmallPager != null && mLargePager != null) {
            //Expanded UI
            mSmallPager.setAdapter(new SmallPageAdapter(mSmallPager));
            mLargePager.setAdapter(new LargePageAdapter(mLargePager));
            mSmallPager.setCurrentItem(state == null ? SmallPanel.ADVANCED.getOrder() : state.getInt(STATE_CURRENT_VIEW_SMALL, SmallPanel.ADVANCED.getOrder()));
            mLargePager.setCurrentItem(state == null ? LargePanel.BASIC.getOrder() : state.getInt(STATE_CURRENT_VIEW_LARGE, LargePanel.BASIC.getOrder()));
        }
        mListener.setHandler(this, mLogic, mPager);
        mDisplay.setOnKeyListener(mListener);

        if (!ViewConfiguration.get(this).hasPermanentMenuKey()) {
            createFakeMenu();
        }

        mLogic.resumeWithHistory();
        updateDeleteMode();

        mGraph = new Graph(mLogic);
        mPulldown.bringToFront();
    }

    private void updateDeleteMode() {
        if (mLogic.getDeleteMode() == Logic.DELETE_MODE_BACKSPACE) {
            mClearButton.setVisibility(View.GONE);
            mBackspaceButton.setVisibility(View.VISIBLE);
        } else {
            mClearButton.setVisibility(View.VISIBLE);
            mBackspaceButton.setVisibility(View.GONE);
        }
    }

    void setOnClickListener(View root, int id) {
        final View target = root != null ? root.findViewById(id) : findViewById(id);
        target.setOnClickListener(mListener);
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

        MenuItem mShowHistory = menu.findItem(R.id.show_history);
        mShowHistory.setVisible(!mPulldown.isSliderOpen());

        MenuItem mHideHistory = menu.findItem(R.id.hide_history);
        mHideHistory.setVisible(mPulldown.isSliderOpen());

        MenuItem mMatrixPanel = menu.findItem(R.id.matrix);
        if(mMatrixPanel != null) mMatrixPanel.setVisible(!getMatrixVisibility() && CalculatorSettings.matrixPanel(getContext()));

        MenuItem mGraphPanel = menu.findItem(R.id.graph);
        if(mGraphPanel != null) mGraphPanel.setVisible(!getGraphVisibility() && CalculatorSettings.graphPanel(getContext()));

        MenuItem mFunctionPanel = menu.findItem(R.id.function);
        if(mFunctionPanel != null) mFunctionPanel.setVisible(!getFunctionVisibility() && CalculatorSettings.functionPanel(getContext()));

        MenuItem mBasicPanel = menu.findItem(R.id.basic);
        if(mBasicPanel != null) mBasicPanel.setVisible(!getBasicVisibility() && CalculatorSettings.basicPanel(getContext()));

        MenuItem mAdvancedPanel = menu.findItem(R.id.advanced);
        if(mAdvancedPanel != null) mAdvancedPanel.setVisible(!getAdvancedVisibility() && CalculatorSettings.advancedPanel(getContext()));

        MenuItem mHexPanel = menu.findItem(R.id.hex);
        if(mHexPanel != null) mHexPanel.setVisible(!getHexVisibility() && CalculatorSettings.hexPanel(getContext()));

        return true;
    }

    private void createFakeMenu() {
        mOverflowMenuButton = findViewById(R.id.overflow_menu);
        if (mOverflowMenuButton != null) {
            mOverflowMenuButton.setVisibility(View.VISIBLE);
            mOverflowMenuButton.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.overflow_menu:
                PopupMenu menu = constructPopupMenu();
                if (menu != null) {
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
//        if(mPager != null) {
//            return mPager.getCurrentItem() == Panel.FUNCTION.getOrder() && CalculatorSettings.functionPanel(getContext());
//        }
//        else if(mSmallPager != null) {
//            return mSmallPager.getCurrentItem() == SmallPanel.FUNCTION.getOrder() && CalculatorSettings.functionPanel(getContext());
//        }
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
        switch (item.getItemId()) {
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
                if (!getBasicVisibility()) {
                    if(mPager!=null) mPager.setCurrentItem(Panel.BASIC.getOrder());
                    else if(mLargePager!=null) mLargePager.setCurrentItem(LargePanel.BASIC.getOrder());
                }
                break;

            case R.id.advanced:
                if (!getAdvancedVisibility()) {
                    if(mPager!=null) mPager.setCurrentItem(Panel.ADVANCED.getOrder());
                    else if(mSmallPager!=null) mSmallPager.setCurrentItem(SmallPanel.ADVANCED.getOrder());
                }
                break;

            case R.id.function:
                if (!getFunctionVisibility()) {
                    if(mPager!=null) mPager.setCurrentItem(Panel.FUNCTION.getOrder());
                    else if(mSmallPager!=null) mSmallPager.setCurrentItem(SmallPanel.FUNCTION.getOrder());
                }
                break;

            case R.id.graph:
                if (!getGraphVisibility()) {
                    if(mPager!=null) mPager.setCurrentItem(Panel.GRAPH.getOrder());
                    else if(mLargePager!=null) mLargePager.setCurrentItem(LargePanel.GRAPH.getOrder());
                }
                break;

            case R.id.matrix:
                if (!getMatrixVisibility()) {
                    if(mPager!=null) mPager.setCurrentItem(Panel.MATRIX.getOrder());
                    else if(mLargePager!=null) mLargePager.setCurrentItem(LargePanel.MATRIX.getOrder());
                }
                break;
                
            case R.id.hex:
                if (!getHexVisibility()) {
                    if(mPager!=null) mPager.setCurrentItem(Panel.HEX.getOrder());
                    else if(mSmallPager!=null) mSmallPager.setCurrentItem(SmallPanel.HEX.getOrder());
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

        if (mPager != null) {
            state.putInt(STATE_CURRENT_VIEW, mPager.getCurrentItem());
        }

        if (mSmallPager != null) {
            state.putInt(STATE_CURRENT_VIEW_SMALL, mSmallPager.getCurrentItem());
        }

        if (mLargePager != null) {
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
        if(keyCode == KeyEvent.KEYCODE_BACK && mPulldown.isSliderOpen()) {
            mPulldown.animateSliderClosed();
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK && mPager != null && (getAdvancedVisibility() || getFunctionVisibility() || getGraphVisibility() || getMatrixVisibility() || getHexVisibility()) && CalculatorSettings.basicPanel(getContext())) {
            mPager.setCurrentItem(Panel.BASIC.getOrder());
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK && mSmallPager != null && mLargePager != null && (getFunctionVisibility() || getGraphVisibility() || getMatrixVisibility() || getHexVisibility()) && CalculatorSettings.basicPanel(getContext()) && CalculatorSettings.advancedPanel(getContext())) {
            mSmallPager.setCurrentItem(SmallPanel.ADVANCED.getOrder());
            mLargePager.setCurrentItem(LargePanel.BASIC.getOrder());
            return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    static void log(String message) {
        if (LOG_ENABLED) {
            Log.v(LOG_TAG, message);
        }
    }

    @Override
    public void onChange() {
        invalidateOptionsMenu();
    }

    @Override
    public void onDeleteModeChange() {
        updateDeleteMode();
    }

    private void setUpHistory() {
        mHistoryView.removeAllViews();
        final LayoutInflater inflater = LayoutInflater.from(this);
        for(HistoryEntry he : mHistory.mEntries) {
            if(!he.getBase().isEmpty()) {
                HistoryLine entry = (HistoryLine) inflater.inflate(R.layout.history_entry, null);
                entry.setHistoryEntry(he);
                entry.setHistory(mHistory);
                TextView base = (TextView) entry.findViewById(R.id.base);
                base.setOnLongClickListener(this);
                base.setMaxWidth(mPulldown.getWidth()/2);
                base.setText(he.getBase());
                TextView edited = (TextView) entry.findViewById(R.id.edited);
                edited.setOnLongClickListener(this);
                edited.setText(he.getEdited());
                edited.setMaxWidth(mPulldown.getWidth()-base.getWidth()-entry.getChildAt(1).getWidth());
                mHistoryView.addView(entry);
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    class PageAdapter extends PagerAdapter {
        private View mGraphPage;
        private View mFunctionPage;
        private View mSimplePage;
        private View mAdvancedPage;
        private View mHexPage;
        private View mMatrixPage;
        private GraphicalView mChartView;

        private int count = 0;

        public PageAdapter(ViewPager parent) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final View graphPage = inflater.inflate(R.layout.graph_pad, parent, false);
            final View functionPage = inflater.inflate(R.layout.function_pad, parent, false);
            final View simplePage = inflater.inflate(R.layout.simple_pad, parent, false);
            final View advancedPage = inflater.inflate(R.layout.advanced_pad, parent, false);
            final View hexPage = inflater.inflate(R.layout.hex_pad, parent, false);
            final View matrixPage = inflater.inflate(R.layout.matrix_pad, parent, false);

            mGraphPage = graphPage;
            mFunctionPage = functionPage;
            mHexPage = hexPage;
            mSimplePage = simplePage;
            mAdvancedPage = advancedPage;
            mMatrixPage = matrixPage;
            setOrder();

            final View clearButton = simplePage.findViewById(R.id.clear);
            if (clearButton != null) {
                mClearButton = clearButton;
            }

            final View backspaceButton = simplePage.findViewById(R.id.del);
            if (backspaceButton != null) {
                mBackspaceButton = backspaceButton;
            }

            switch(mLogic.getMode()) {
            case BINARY:
                mHexPage.findViewById(R.id.bin).setBackgroundResource(R.color.pressed_color);
                break;
            case DECIMAL:
                mHexPage.findViewById(R.id.dec).setBackgroundResource(R.color.pressed_color);
                break;
            case HEXADECIMAL:
                mHexPage.findViewById(R.id.hex).setBackgroundResource(R.color.pressed_color);
                break;
            }
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public void startUpdate(View container) {
        }

        @Override
        public Object instantiateItem(View container, int position) {
            if(position == Panel.GRAPH.getOrder() && CalculatorSettings.graphPanel(getContext())) {
                if (mChartView == null) {
                    mChartView = mGraph.getGraph(Calculator.this);
                    mChartView.setId(R.id.graphView);
                    ((LinearLayout) mGraphPage).addView(mChartView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                } 
                else {
                    mChartView.repaint();
                }
                ((ViewGroup) container).addView(mGraphPage);
                return mGraphPage;
            }
            else if(position == Panel.FUNCTION.getOrder() && CalculatorSettings.functionPanel(getContext())) {
                ((ViewGroup) container).addView(mFunctionPage);
                return mFunctionPage;
            }
            else if(position == Panel.BASIC.getOrder() && CalculatorSettings.basicPanel(getContext())) {
                ((ViewGroup) container).addView(mSimplePage);
                return mSimplePage;
            }
            else if(position == Panel.ADVANCED.getOrder() && CalculatorSettings.advancedPanel(getContext())) {
                ((ViewGroup) container).addView(mAdvancedPage);
                return mAdvancedPage;
            }
            else if(position == Panel.HEX.getOrder() && CalculatorSettings.hexPanel(getContext())) {
                ((ViewGroup) container).addView(mHexPage);
                return mHexPage;
            }
            else if(position == Panel.MATRIX.getOrder() && CalculatorSettings.matrixPanel(getContext())) {
                ((ViewGroup) container).addView(mMatrixPage);
                return mMatrixPage;
            }
            return null;
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            ((ViewGroup) container).removeView((View) object);
        }

        @Override
        public void finishUpdate(View container) {
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            
            setOrder();
        }

        private void setOrder() {
            count = 0;
            if(CalculatorSettings.graphPanel(getContext())) {
                Panel.GRAPH.setOrder(count);
                count++;
            }
            if(CalculatorSettings.functionPanel(getContext())) {
                Panel.FUNCTION.setOrder(count);
                count++;
            }
            if(CalculatorSettings.hexPanel(getContext())) {
                Panel.HEX.setOrder(count);
                count++;
            }
            if(CalculatorSettings.basicPanel(getContext())) {
                Panel.BASIC.setOrder(count);
                count++;
            }
            if(CalculatorSettings.advancedPanel(getContext())) {
                Panel.ADVANCED.setOrder(count);
                count++;
            }
            if(CalculatorSettings.matrixPanel(getContext())) {
                Panel.MATRIX.setOrder(count);
                count++;
            }
        }
    }

    class SmallPageAdapter extends PagerAdapter {
        private View mHexPage;
        private View mFunctionPage;
        private View mAdvancedPage;

        private int count = 0;

        public SmallPageAdapter(ViewPager parent) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final View hexPage = inflater.inflate(R.layout.hex_pad, parent, false);
            final View functionPage = inflater.inflate(R.layout.function_pad, parent, false);
            final View advancedPage = inflater.inflate(R.layout.advanced_pad, parent, false);
            
            mHexPage = hexPage;
            mFunctionPage = functionPage;
            mAdvancedPage = advancedPage;
            setOrder();

            switch(mLogic.getMode()) {
            case BINARY:
                mHexPage.findViewById(R.id.bin).setBackgroundResource(R.color.pressed_color);
                break;
            case DECIMAL:
                mHexPage.findViewById(R.id.dec).setBackgroundResource(R.color.pressed_color);
                break;
            case HEXADECIMAL:
                mHexPage.findViewById(R.id.hex).setBackgroundResource(R.color.pressed_color);
                break;
            }
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public void startUpdate(View container) {
        }

        @Override
        public Object instantiateItem(View container, int position) {
            if(position == SmallPanel.FUNCTION.getOrder() && CalculatorSettings.functionPanel(getContext())) {
                ((ViewGroup) container).addView(mFunctionPage);
                return mFunctionPage;
            }
            else if(position == SmallPanel.ADVANCED.getOrder() && CalculatorSettings.advancedPanel(getContext())) {
                ((ViewGroup) container).addView(mAdvancedPage);
                return mAdvancedPage;
            }
            else if(position == SmallPanel.HEX.getOrder() && CalculatorSettings.hexPanel(getContext())) {
                ((ViewGroup) container).addView(mHexPage);
                return mHexPage;
            }
            return null;
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            ((ViewGroup) container).removeView((View) object);
        }

        @Override
        public void finishUpdate(View container) {
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            
            setOrder();
        }

        private void setOrder() {
            count = 0;
            if(CalculatorSettings.hexPanel(getContext())) {
                SmallPanel.HEX.setOrder(count);
                count++;
            }
            if(CalculatorSettings.advancedPanel(getContext())) {
                SmallPanel.ADVANCED.setOrder(count);
                count++;
            }
            if(CalculatorSettings.functionPanel(getContext())) {
                SmallPanel.FUNCTION.setOrder(count);
                count++;
            }
        }
    }

    class LargePageAdapter extends PagerAdapter {
        private View mGraphPage;
        private View mSimplePage;
        private View mMatrixPage;
        private GraphicalView mChartView;

        private int count = 0;

        public LargePageAdapter(ViewPager parent) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final View graphPage = inflater.inflate(R.layout.graph_pad, parent, false);
            final View simplePage = inflater.inflate(R.layout.simple_pad, parent, false);
            final View matrixPage = inflater.inflate(R.layout.matrix_pad, parent, false);

            mGraphPage = graphPage;
            mSimplePage = simplePage;
            mMatrixPage = matrixPage;
            setOrder();

            final View clearButton = simplePage.findViewById(R.id.clear);
            if (clearButton != null) {
                mClearButton = clearButton;
            }

            final View backspaceButton = simplePage.findViewById(R.id.del);
            if (backspaceButton != null) {
                mBackspaceButton = backspaceButton;
            }
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public void startUpdate(View container) {
        }

        @Override
        public Object instantiateItem(View container, int position) {
            if(position == LargePanel.GRAPH.getOrder() && CalculatorSettings.graphPanel(getContext())) {
                if (mChartView == null) {
                    mChartView = mGraph.getGraph(Calculator.this);
                    mChartView.setId(R.id.graphView);
                    ((LinearLayout) mGraphPage).addView(mChartView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                } 
                else {
                    mChartView.repaint();
                }
                ((ViewGroup) container).addView(mGraphPage);
                return mGraphPage;
            }
            else if(position == LargePanel.BASIC.getOrder() && CalculatorSettings.basicPanel(getContext())) {
                ((ViewGroup) container).addView(mSimplePage);
                return mSimplePage;
            }
            else if(position == LargePanel.MATRIX.getOrder() && CalculatorSettings.matrixPanel(getContext())) {
                ((ViewGroup) container).addView(mMatrixPage);
                return mMatrixPage;
            }
            return null;
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            ((ViewGroup) container).removeView((View) object);
        }

        @Override
        public void finishUpdate(View container) {
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            
            setOrder();
        }

        private void setOrder() {
            count = 0;
            if(CalculatorSettings.graphPanel(getContext())) {
                LargePanel.GRAPH.setOrder(count);
                count++;
            }
            if(CalculatorSettings.basicPanel(getContext())) {
                LargePanel.BASIC.setOrder(count);
                count++;
            }
            if(CalculatorSettings.matrixPanel(getContext())) {
                LargePanel.MATRIX.setOrder(count);
                count++;
            }
        }
    }

    private Context getContext() {
        return Calculator.this;
    }

    static class CalculatorSettings {
        static boolean graphPanel(Context context) {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Panel.GRAPH.toString(), context.getResources().getBoolean(R.bool.GRAPH));
        }

        static boolean hexPanel(Context context) {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Panel.HEX.toString(), context.getResources().getBoolean(R.bool.HEX));
        }

        static boolean functionPanel(Context context) {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Panel.FUNCTION.toString(), context.getResources().getBoolean(R.bool.FUNCTION));
        }

        static boolean basicPanel(Context context) {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Panel.BASIC.toString(), context.getResources().getBoolean(R.bool.BASIC));
        }

        static boolean advancedPanel(Context context) {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Panel.ADVANCED.toString(), context.getResources().getBoolean(R.bool.ADVANCED));
        }

        static boolean matrixPanel(Context context) {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Panel.MATRIX.toString(), context.getResources().getBoolean(R.bool.MATRIX));
        }
    }
}
