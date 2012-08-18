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

import org.achartengine.GraphicalView;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

public class Calculator extends Activity implements PanelSwitcher.Listener, Logic.Listener,
        OnClickListener, OnMenuItemClickListener {
    EventListener mListener = new EventListener();
    private CalculatorDisplay mDisplay;
    private Persist mPersist;
    private History mHistory;
    private Logic mLogic;
    private ViewPager mPager;
    private ViewPager mSmallPager;
    private ViewPager mLargePager;
    private View mClearButton;
    private View mBackspaceButton;
    private View mOverflowMenuButton;
    private Graph mGraph;

    static final int GRAPH_PANEL    = 0;
    static final int FUNCTION_PANEL = 1;
    static final int BASIC_PANEL    = 2;
    static final int ADVANCED_PANEL = 3;
    static final int HEX_PANEL      = 4;
    static final int MATRIX_PANEL   = 5;

    static final int SMALL_HEX_PANEL      = 0;
    static final int SMALL_ADVANCED_PANEL = 1;
    static final int SMALL_FUNCTION_PANEL = 2;
    
    static final int LARGE_GRAPH_PANEL    = 0;
    static final int LARGE_BASIC_PANEL    = 1;
    static final int LARGE_MATRIX_PANEL   = 2;

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
        if (mPager != null) {
            mPager.setAdapter(new PageAdapter(mPager));
        } 
        else if(mSmallPager != null && mLargePager != null) {
            //Expanded UI
        	mSmallPager.setAdapter(new SmallPageAdapter(mSmallPager));
        	mLargePager.setAdapter(new LargePageAdapter(mLargePager));
        }
        else {
            // Single page UI
            final TypedArray buttons = getResources().obtainTypedArray(R.array.buttons);
            for (int i = 0; i < buttons.length(); i++) {
                setOnClickListener(null, buttons.getResourceId(i, 0));
            }
            buttons.recycle();
        }

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

        mLogic = new Logic(this, mHistory, mDisplay);
        mLogic.setListener(this);
        if(mPersist.getMode() != null) mLogic.setMode(mPersist.getMode());

        mLogic.setDeleteMode(mPersist.getDeleteMode());
        mLogic.setLineLength(mDisplay.getMaxDigits());

        HistoryAdapter historyAdapter = new HistoryAdapter(this, mHistory, mLogic);
        mHistory.setObserver(historyAdapter);

        if (mPager != null) {
            mPager.setCurrentItem(state == null ? BASIC_PANEL : state.getInt(STATE_CURRENT_VIEW, BASIC_PANEL));
            
            switch(mLogic.getMode()){
        	case BINARY:
        		((PageAdapter) mPager.getAdapter()).mHexPage.findViewById(R.id.bin).setBackgroundResource(R.color.pressed_color);
        		break;
        	case DECIMAL:
        		((PageAdapter) mPager.getAdapter()).mHexPage.findViewById(R.id.dec).setBackgroundResource(R.color.pressed_color);
        		break;
        	case HEXADECIMAL:
        		((PageAdapter) mPager.getAdapter()).mHexPage.findViewById(R.id.hex).setBackgroundResource(R.color.pressed_color);
        		break;
        	}
        }
        else if (mSmallPager != null && mLargePager != null) {
        	mSmallPager.setCurrentItem(state == null ? SMALL_ADVANCED_PANEL : state.getInt(STATE_CURRENT_VIEW_SMALL, SMALL_ADVANCED_PANEL));
        	mLargePager.setCurrentItem(state == null ? LARGE_BASIC_PANEL : state.getInt(STATE_CURRENT_VIEW_LARGE, LARGE_BASIC_PANEL));
        	
        	switch(mLogic.getMode()){
        	case BINARY:
        		((SmallPageAdapter) mSmallPager.getAdapter()).mHexPage.findViewById(R.id.bin).setBackgroundResource(R.color.pressed_color);
        		break;
        	case DECIMAL:
        		((SmallPageAdapter) mSmallPager.getAdapter()).mHexPage.findViewById(R.id.dec).setBackgroundResource(R.color.pressed_color);
        		break;
        	case HEXADECIMAL:
        		((SmallPageAdapter) mSmallPager.getAdapter()).mHexPage.findViewById(R.id.hex).setBackgroundResource(R.color.pressed_color);
        		break;
        	}
        }

        mListener.setHandler(this, mLogic, mPager);
        mDisplay.setOnKeyListener(mListener);

        if (!ViewConfiguration.get(this).hasPermanentMenuKey()) {
            createFakeMenu();
        }

        mLogic.resumeWithHistory();
        updateDeleteMode();
        
        mGraph = new Graph(mLogic);
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
        
        MenuItem mMatrixPanel = menu.findItem(R.id.matrix);
        if(mMatrixPanel != null) mMatrixPanel.setVisible(!getMatrixVisibility());
        
        MenuItem mGraphPanel = menu.findItem(R.id.graph);
        if(mGraphPanel != null) mGraphPanel.setVisible(!getGraphVisibility());
        
        MenuItem mFunctionPanel = menu.findItem(R.id.function);
        if(mFunctionPanel != null) mFunctionPanel.setVisible(!getFunctionVisibility());
        
        MenuItem mBasicPanel = menu.findItem(R.id.basic);
        if(mBasicPanel != null) mBasicPanel.setVisible(!getBasicVisibility());
        
        MenuItem mAdvancedPanel = menu.findItem(R.id.advanced);
        if(mAdvancedPanel != null) mAdvancedPanel.setVisible(!getAdvancedVisibility());
        
        MenuItem mHexPanel = menu.findItem(R.id.hex);
        if(mHexPanel != null) mHexPanel.setVisible(!getHexVisibility());
        
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
        	return mPager.getCurrentItem() == GRAPH_PANEL;
        }
        else if(mLargePager != null){
        	return mLargePager.getCurrentItem() == LARGE_GRAPH_PANEL;
        }
        return false;
    }
    
    private boolean getFunctionVisibility() {
    	if(mPager != null) {
        	return mPager.getCurrentItem() == FUNCTION_PANEL;
        }
        else if(mSmallPager != null){
        	return mSmallPager.getCurrentItem() == SMALL_FUNCTION_PANEL;
        }
        return false;
    }
    
    private boolean getBasicVisibility() {
    	if(mPager != null) {
        	return mPager.getCurrentItem() == BASIC_PANEL;
        }
        else if(mLargePager != null){
        	return mLargePager.getCurrentItem() == LARGE_BASIC_PANEL;
        }
        return false;
    }

    private boolean getAdvancedVisibility() {
    	if(mPager != null) {
        	return mPager.getCurrentItem() == ADVANCED_PANEL;
        }
        else if(mSmallPager != null){
        	return mSmallPager.getCurrentItem() == SMALL_ADVANCED_PANEL;
        }
        return false;
    }
    
    private boolean getHexVisibility() {
    	if(mPager != null) {
        	return mPager.getCurrentItem() == HEX_PANEL;
        }
        else if(mSmallPager != null){
        	return mSmallPager.getCurrentItem() == SMALL_HEX_PANEL;
        }
        return false;
    }
    
    private boolean getMatrixVisibility() {
    	if(mPager != null) {
        	return mPager.getCurrentItem() == MATRIX_PANEL;
        }
        else if(mLargePager != null){
        	return mLargePager.getCurrentItem() == LARGE_MATRIX_PANEL;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_history:
                mHistory.clear();
                mLogic.onClear();
                break;

            case R.id.basic:
                if (!getBasicVisibility()) {
                    if(mPager!=null) mPager.setCurrentItem(BASIC_PANEL);
                    else if(mLargePager!=null) mLargePager.setCurrentItem(LARGE_BASIC_PANEL);
                }
                break;

            case R.id.advanced:
                if (!getAdvancedVisibility()) {
                	if(mPager!=null) mPager.setCurrentItem(ADVANCED_PANEL);
                	else if(mSmallPager!=null) mSmallPager.setCurrentItem(SMALL_ADVANCED_PANEL);
                }
                break;

            case R.id.function:
                if (!getFunctionVisibility()) {
                	if(mPager!=null) mPager.setCurrentItem(FUNCTION_PANEL);
                	else if(mSmallPager!=null) mSmallPager.setCurrentItem(SMALL_FUNCTION_PANEL);
                }
                break;

            case R.id.graph:
                if (!getGraphVisibility()) {
                	if(mPager!=null) mPager.setCurrentItem(GRAPH_PANEL);
                	else if(mLargePager!=null) mLargePager.setCurrentItem(LARGE_GRAPH_PANEL);
                }
                break;

            case R.id.matrix:
                if (!getMatrixVisibility()) {
                	if(mPager!=null) mPager.setCurrentItem(MATRIX_PANEL);
                	else if(mLargePager!=null) mLargePager.setCurrentItem(LARGE_MATRIX_PANEL);
                }
                break;
                
            case R.id.hex:
                if (!getHexVisibility()) {
                	if(mPager!=null) mPager.setCurrentItem(HEX_PANEL);
                	else if(mSmallPager!=null) mSmallPager.setCurrentItem(SMALL_HEX_PANEL);
                }
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
        if(keyCode == KeyEvent.KEYCODE_BACK && mPager != null && (getAdvancedVisibility() || getFunctionVisibility() || getGraphVisibility() || getMatrixVisibility() || getHexVisibility())) {
            mPager.setCurrentItem(BASIC_PANEL);
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK && mSmallPager != null && mLargePager != null && (getFunctionVisibility() || getGraphVisibility() || getMatrixVisibility() || getHexVisibility())){
        	mSmallPager.setCurrentItem(SMALL_ADVANCED_PANEL);
            mLargePager.setCurrentItem(LARGE_BASIC_PANEL);
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

    class PageAdapter extends PagerAdapter {
        private View mGraphPage;
        private View mFunctionPage;
        private View mSimplePage;
        private View mAdvancedPage;
        private View mHexPage;
        private View mMatrixPage;
        private GraphicalView mChartView;

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
            mSimplePage = simplePage;
            mAdvancedPage = advancedPage;
            mHexPage = hexPage;
            mMatrixPage = matrixPage;

            final Resources res = getResources();
            final TypedArray simpleButtons = res.obtainTypedArray(R.array.simple_buttons);
            for (int i = 0; i < simpleButtons.length(); i++) {
                setOnClickListener(simplePage, simpleButtons.getResourceId(i, 0));
            }
            simpleButtons.recycle();

            final TypedArray advancedButtons = res.obtainTypedArray(R.array.advanced_buttons);
            for (int i = 0; i < advancedButtons.length(); i++) {
                setOnClickListener(advancedPage, advancedButtons.getResourceId(i, 0));
            }
            advancedButtons.recycle();
            
            final TypedArray functionButtons = res.obtainTypedArray(R.array.function_buttons);
            for (int i = 0; i < functionButtons.length(); i++) {
                setOnClickListener(functionPage, functionButtons.getResourceId(i, 0));
            }
            functionButtons.recycle();
            
            final TypedArray hexButtons = res.obtainTypedArray(R.array.hex_buttons);
            for (int i = 0; i < hexButtons.length(); i++) {
                setOnClickListener(hexPage, hexButtons.getResourceId(i, 0));
            }
            hexButtons.recycle();

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
            return 6;
        }

        @Override
        public void startUpdate(View container) {
        }

        @Override
        public Object instantiateItem(View container, int position) {
            if(position == GRAPH_PANEL){
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
            else if(position == FUNCTION_PANEL){
                ((ViewGroup) container).addView(mFunctionPage);
                return mFunctionPage;
            }
            else if(position == BASIC_PANEL){
                ((ViewGroup) container).addView(mSimplePage);
                return mSimplePage;
            }
            else if(position == ADVANCED_PANEL){
                ((ViewGroup) container).addView(mAdvancedPage);
                return mAdvancedPage;
            }
            else if(position == HEX_PANEL){
                ((ViewGroup) container).addView(mHexPage);
                return mHexPage;
            }
            else if(position == MATRIX_PANEL){
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
    }
    
    class SmallPageAdapter extends PagerAdapter {
    	private View mHexPage;
        private View mFunctionPage;
        private View mAdvancedPage;

        public SmallPageAdapter(ViewPager parent) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final View hexPage = inflater.inflate(R.layout.hex_pad, parent, false);
            final View functionPage = inflater.inflate(R.layout.function_pad, parent, false);
            final View advancedPage = inflater.inflate(R.layout.advanced_pad, parent, false);
            
            mHexPage = hexPage;
            mFunctionPage = functionPage;
            mAdvancedPage = advancedPage;

            final Resources res = getResources();

            final TypedArray advancedButtons = res.obtainTypedArray(R.array.advanced_buttons);
            for (int i = 0; i < advancedButtons.length(); i++) {
                setOnClickListener(advancedPage, advancedButtons.getResourceId(i, 0));
            }
            advancedButtons.recycle();
            
            final TypedArray functionButtons = res.obtainTypedArray(R.array.function_buttons);
            for (int i = 0; i < functionButtons.length(); i++) {
                setOnClickListener(functionPage, functionButtons.getResourceId(i, 0));
            }
            functionButtons.recycle();
            
            final TypedArray hexButtons = res.obtainTypedArray(R.array.hex_buttons);
            for (int i = 0; i < hexButtons.length(); i++) {
                setOnClickListener(hexPage, hexButtons.getResourceId(i, 0));
            }
            hexButtons.recycle();
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public void startUpdate(View container) {
        }

        @Override
        public Object instantiateItem(View container, int position) {
            if(position == SMALL_FUNCTION_PANEL){
                ((ViewGroup) container).addView(mFunctionPage);
                return mFunctionPage;
            }
            else if(position == SMALL_ADVANCED_PANEL){
                ((ViewGroup) container).addView(mAdvancedPage);
                return mAdvancedPage;
            }
            else if(position == SMALL_HEX_PANEL){
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
    }
    
    class LargePageAdapter extends PagerAdapter {
        private View mGraphPage;
        private View mSimplePage;
        private View mMatrixPage;
        private GraphicalView mChartView;

        public LargePageAdapter(ViewPager parent) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final View graphPage = inflater.inflate(R.layout.graph_pad, parent, false);
            final View simplePage = inflater.inflate(R.layout.simple_pad, parent, false);
            final View matrixPage = inflater.inflate(R.layout.matrix_pad, parent, false);
            
            mGraphPage = graphPage;
            mSimplePage = simplePage;
            mMatrixPage = matrixPage;

            final Resources res = getResources();
            final TypedArray simpleButtons = res.obtainTypedArray(R.array.simple_buttons);
            for (int i = 0; i < simpleButtons.length(); i++) {
                setOnClickListener(simplePage, simpleButtons.getResourceId(i, 0));
            }
            simpleButtons.recycle();

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
            return 3;
        }

        @Override
        public void startUpdate(View container) {
        }

        @Override
        public Object instantiateItem(View container, int position) {
            if(position == LARGE_GRAPH_PANEL){
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
            else if(position == LARGE_BASIC_PANEL){
                ((ViewGroup) container).addView(mSimplePage);
                return mSimplePage;
            }
            else if(position == LARGE_MATRIX_PANEL){
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
    }
}
