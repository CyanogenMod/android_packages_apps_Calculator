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
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

public class Calculator extends Activity implements PanelSwitcher.Listener, Logic.Listener,
        OnClickListener, OnMenuItemClickListener, OnTouchListener, OnLongClickListener {
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
    private ImageButton mPulldown;
    private View mWindow;
    private Graph mGraph;
    private int mDistance;
    private int mDefaultDisplayHeight;
    private int mDefaultPulldownHeight;
    private int mWindowHeight;
    private int mWindowWidth;
    private boolean heightSet = false;
    private boolean showingHistory = false;
    private SharedPreferences mPreferences;

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

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.main);
        mWindow = findViewById(R.id.window);
        heightSet = false;
        mHistoryView = (LinearLayout) findViewById(R.id.history);
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

        mPulldown = (ImageButton) findViewById(R.id.pulldown);
        mPulldown.setOnTouchListener(this);
        mPulldown.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(showingHistory) {
                    minimizeHistory();
                } else {
                    maximizeHistory();
                }
            }
        });

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

        if(showingHistory) {
            maximizeHistory();
        }
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
        mShowHistory.setVisible(!showingHistory);

        MenuItem mHideHistory = menu.findItem(R.id.hide_history);
        mHideHistory.setVisible(showingHistory);

        MenuItem mMatrixPanel = menu.findItem(R.id.matrix);
        if(mMatrixPanel != null) mMatrixPanel.setVisible(!getMatrixVisibility() && mPreferences.getBoolean(Panel.MATRIX.toString(), getResources().getBoolean(R.bool.MATRIX)));
        
        MenuItem mGraphPanel = menu.findItem(R.id.graph);
        if(mGraphPanel != null) mGraphPanel.setVisible(!getGraphVisibility() && mPreferences.getBoolean(Panel.GRAPH.toString(), getResources().getBoolean(R.bool.GRAPH)));
        
        MenuItem mFunctionPanel = menu.findItem(R.id.function);
        if(mFunctionPanel != null) mFunctionPanel.setVisible(!getFunctionVisibility() && mPreferences.getBoolean(Panel.FUNCTION.toString(), getResources().getBoolean(R.bool.FUNCTION)));
        
        MenuItem mBasicPanel = menu.findItem(R.id.basic);
        if(mBasicPanel != null) mBasicPanel.setVisible(!getBasicVisibility() && mPreferences.getBoolean(Panel.BASIC.toString(), getResources().getBoolean(R.bool.BASIC)));
        
        MenuItem mAdvancedPanel = menu.findItem(R.id.advanced);
        if(mAdvancedPanel != null) mAdvancedPanel.setVisible(!getAdvancedVisibility() && mPreferences.getBoolean(Panel.ADVANCED.toString(), getResources().getBoolean(R.bool.ADVANCED)));
        
        MenuItem mHexPanel = menu.findItem(R.id.hex);
        if(mHexPanel != null) mHexPanel.setVisible(!getHexVisibility() && mPreferences.getBoolean(Panel.HEX.toString(), getResources().getBoolean(R.bool.HEX)));
        
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
            return mPager.getCurrentItem() == Panel.GRAPH.getOrder() && mPreferences.getBoolean(Panel.GRAPH.toString(), getResources().getBoolean(R.bool.GRAPH));
        }
        else if(mLargePager != null) {
            return mLargePager.getCurrentItem() == LargePanel.GRAPH.getOrder() && mPreferences.getBoolean(Panel.GRAPH.toString(), getResources().getBoolean(R.bool.GRAPH));
        }
        return false;
    }
    
    private boolean getFunctionVisibility() {
//        if(mPager != null) {
//            return mPager.getCurrentItem() == Panel.FUNCTION.getOrder() && mPreferences.getBoolean(Panel.FUNCTION.toString(), getResources().getBoolean(R.bool.FUNCTION));
//        }
//        else if(mSmallPager != null) {
//            return mSmallPager.getCurrentItem() == SmallPanel.FUNCTION.getOrder() && mPreferences.getBoolean(Panel.FUNCTION.toString(), getResources().getBoolean(R.bool.FUNCTION));
//        }
        return false;
    }
    
    private boolean getBasicVisibility() {
        if(mPager != null) {
            return mPager.getCurrentItem() == Panel.BASIC.getOrder() && mPreferences.getBoolean(Panel.BASIC.toString(), getResources().getBoolean(R.bool.BASIC));
        }
        else if(mLargePager != null) {
            return mLargePager.getCurrentItem() == LargePanel.BASIC.getOrder() && mPreferences.getBoolean(Panel.BASIC.toString(), getResources().getBoolean(R.bool.BASIC));
        }
        return false;
    }

    private boolean getAdvancedVisibility() {
        if(mPager != null) {
            return mPager.getCurrentItem() == Panel.ADVANCED.getOrder() && mPreferences.getBoolean(Panel.ADVANCED.toString(), getResources().getBoolean(R.bool.ADVANCED));
        }
        else if(mSmallPager != null) {
            return mSmallPager.getCurrentItem() == SmallPanel.ADVANCED.getOrder() && mPreferences.getBoolean(Panel.ADVANCED.toString(), getResources().getBoolean(R.bool.ADVANCED));
        }
        return false;
    }
    
    private boolean getHexVisibility() {
        if(mPager != null) {
            return mPager.getCurrentItem() == Panel.HEX.getOrder() && mPreferences.getBoolean(Panel.HEX.toString(), getResources().getBoolean(R.bool.HEX));
        }
        else if(mSmallPager != null) {
            return mSmallPager.getCurrentItem() == SmallPanel.HEX.getOrder() && mPreferences.getBoolean(Panel.HEX.toString(), getResources().getBoolean(R.bool.HEX));
        }
        return false;
    }
    
    private boolean getMatrixVisibility() {
        if(mPager != null) {
            return mPager.getCurrentItem() == Panel.MATRIX.getOrder() && mPreferences.getBoolean(Panel.MATRIX.toString(), getResources().getBoolean(R.bool.MATRIX));
        }
        else if(mLargePager != null) {
            return mLargePager.getCurrentItem() == LargePanel.MATRIX.getOrder() && mPreferences.getBoolean(Panel.MATRIX.toString(), getResources().getBoolean(R.bool.MATRIX));
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
                maximizeHistory();
                break;

            case R.id.hide_history:
                minimizeHistory();
                break;

            case R.id.basic:
                if (!getBasicVisibility()) {
                    if(mPager!=null) mPager.setCurrentItem(Panel.BASIC.getOrder(), true);
                    else if(mLargePager!=null) mLargePager.setCurrentItem(LargePanel.BASIC.getOrder(), true);
                }
                break;

            case R.id.advanced:
                if (!getAdvancedVisibility()) {
                    if(mPager!=null) mPager.setCurrentItem(Panel.ADVANCED.getOrder(), true);
                    else if(mSmallPager!=null) mSmallPager.setCurrentItem(SmallPanel.ADVANCED.getOrder(), true);
                }
                break;

            case R.id.function:
                if (!getFunctionVisibility()) {
                    if(mPager!=null) mPager.setCurrentItem(Panel.FUNCTION.getOrder(), true);
                    else if(mSmallPager!=null) mSmallPager.setCurrentItem(SmallPanel.FUNCTION.getOrder(), true);
                }
                break;

            case R.id.graph:
                if (!getGraphVisibility()) {
                    if(mPager!=null) mPager.setCurrentItem(Panel.GRAPH.getOrder(), true);
                    else if(mLargePager!=null) mLargePager.setCurrentItem(LargePanel.GRAPH.getOrder(), true);
                }
                break;

            case R.id.matrix:
                if (!getMatrixVisibility()) {
                    if(mPager!=null) mPager.setCurrentItem(Panel.MATRIX.getOrder(), true);
                    else if(mLargePager!=null) mLargePager.setCurrentItem(LargePanel.MATRIX.getOrder(), true);
                }
                break;
                
            case R.id.hex:
                if (!getHexVisibility()) {
                    if(mPager!=null) mPager.setCurrentItem(Panel.HEX.getOrder(), true);
                    else if(mSmallPager!=null) mSmallPager.setCurrentItem(SmallPanel.HEX.getOrder(), true);
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
        if(keyCode == KeyEvent.KEYCODE_BACK && showingHistory) {
            minimizeHistory();
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK && mPager != null && (getAdvancedVisibility() || getFunctionVisibility() || getGraphVisibility() || getMatrixVisibility() || getHexVisibility()) && mPreferences.getBoolean(Panel.BASIC.toString(), getResources().getBoolean(R.bool.BASIC))) {
            mPager.setCurrentItem(Panel.BASIC.getOrder(), true);
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK && mSmallPager != null && mLargePager != null && (getFunctionVisibility() || getGraphVisibility() || getMatrixVisibility() || getHexVisibility()) && mPreferences.getBoolean(Panel.BASIC.toString(), getResources().getBoolean(R.bool.BASIC)) && mPreferences.getBoolean(Panel.ADVANCED.toString(), getResources().getBoolean(R.bool.ADVANCED))) {
            mSmallPager.setCurrentItem(SmallPanel.ADVANCED.getOrder(), true);
            mLargePager.setCurrentItem(LargePanel.BASIC.getOrder(), true);
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            if(showingHistory) {
                mPulldown.setImageResource(R.drawable.calculator_up_handle_press);
            } else {
                mPulldown.setImageResource(R.drawable.calculator_down_handle_press);
            }
            break;
        case MotionEvent.ACTION_UP:
            if(((View) mDisplay.getParent().getParent()).getHeight() > mWindowHeight/2) {
                maximizeHistory();
            } else{
                minimizeHistory();
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if(mDistance == mWindowHeight-mDefaultPulldownHeight && event.getY() > 0) break;
            if(mDistance == mDefaultDisplayHeight && event.getY() < 0) break;
            mDistance += event.getY();
            if(mDistance > mWindowHeight-mDefaultPulldownHeight && event.getY() > 0) mDistance = mWindowHeight-mDefaultPulldownHeight;
            if(mDistance < mDefaultDisplayHeight && event.getY() < 0) mDistance = mDefaultDisplayHeight;
            ((View) mDisplay.getParent().getParent()).setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, mDistance));
            break;
        }
        return false;
    }
    
    private void minimizeHistory() {
        ((View) mDisplay.getParent().getParent()).setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, mDefaultDisplayHeight));
        mDistance = mDefaultDisplayHeight;
        ((View) mDisplay.getParent()).setVisibility(View.VISIBLE);
        ((View) mHistoryView.getParent()).setVisibility(View.GONE);
        showingHistory = false;
        mPulldown.setImageResource(R.drawable.calculator_down_handle);
    }
    
    private void maximizeHistory() {
        setUpHistory();
        ((View) mDisplay.getParent().getParent()).setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, mWindowHeight-mDefaultPulldownHeight));
        mDistance = mWindowHeight-mDefaultPulldownHeight;
        ((View) mDisplay.getParent()).setVisibility(View.GONE);
        ((View) mHistoryView.getParent()).setVisibility(View.VISIBLE);
        showingHistory = true;
        mPulldown.setImageResource(R.drawable.calculator_up_handle);
    }
    
    private void setUpHistory() {
        mHistoryView.removeAllViews();
        final LayoutInflater inflater = LayoutInflater.from(this);
        for(HistoryEntry he : mHistory.mEntries) {
            if(!he.getBase().isEmpty()) {
                View entry = inflater.inflate(R.layout.history_entry, null);
                TextView base = (TextView) entry.findViewById(R.id.base);
                base.setOnLongClickListener(this);
                base.setMaxWidth(mWindowWidth/2);
                TextView edited = (TextView) entry.findViewById(R.id.edited);
                edited.setOnLongClickListener(this);
                base.setText(he.getBase());
                edited.setText(he.getEdited());
                edited.setMaxWidth(mWindowWidth/2);
                mHistoryView.addView(entry);
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(!showingHistory && !heightSet) {
            mWindowHeight = mWindow.getHeight();
            mWindowWidth = mWindow.getWidth();
            mDefaultPulldownHeight = mPulldown.getHeight();
            mDefaultDisplayHeight = ((View) mDisplay.getParent().getParent()).getMeasuredHeight();
            mDistance = mDefaultDisplayHeight;
            heightSet = true;
        }
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
            if(position == Panel.GRAPH.getOrder() && mPreferences.getBoolean(Panel.GRAPH.toString(), getResources().getBoolean(R.bool.GRAPH))) {
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
            else if(position == Panel.FUNCTION.getOrder() && mPreferences.getBoolean(Panel.FUNCTION.toString(), getResources().getBoolean(R.bool.FUNCTION))) {
                ((ViewGroup) container).addView(mFunctionPage);
                return mFunctionPage;
            }
            else if(position == Panel.BASIC.getOrder() && mPreferences.getBoolean(Panel.BASIC.toString(), getResources().getBoolean(R.bool.BASIC))) {
                ((ViewGroup) container).addView(mSimplePage);
                return mSimplePage;
            }
            else if(position == Panel.ADVANCED.getOrder() && mPreferences.getBoolean(Panel.ADVANCED.toString(), getResources().getBoolean(R.bool.ADVANCED))) {
                ((ViewGroup) container).addView(mAdvancedPage);
                return mAdvancedPage;
            }
            else if(position == Panel.HEX.getOrder() && mPreferences.getBoolean(Panel.HEX.toString(), getResources().getBoolean(R.bool.HEX))) {
                ((ViewGroup) container).addView(mHexPage);
                return mHexPage;
            }
            else if(position == Panel.MATRIX.getOrder() && mPreferences.getBoolean(Panel.MATRIX.toString(), getResources().getBoolean(R.bool.MATRIX))) {
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
            if(mPreferences.getBoolean(Panel.GRAPH.toString(), getResources().getBoolean(R.bool.GRAPH))) {
                Panel.GRAPH.setOrder(count);
                count++;
            }
            if(mPreferences.getBoolean(Panel.FUNCTION.toString(), getResources().getBoolean(R.bool.FUNCTION))) {
                Panel.FUNCTION.setOrder(count);
                count++;
            }
            if(mPreferences.getBoolean(Panel.HEX.toString(), getResources().getBoolean(R.bool.HEX))) {
                Panel.HEX.setOrder(count);
                count++;
            }
            if(mPreferences.getBoolean(Panel.BASIC.toString(), getResources().getBoolean(R.bool.BASIC))) {
                Panel.BASIC.setOrder(count);
                count++;
            }
            if(mPreferences.getBoolean(Panel.ADVANCED.toString(), getResources().getBoolean(R.bool.ADVANCED))) {
                Panel.ADVANCED.setOrder(count);
                count++;
            }
            if(mPreferences.getBoolean(Panel.MATRIX.toString(), getResources().getBoolean(R.bool.MATRIX))) {
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
            if(position == SmallPanel.FUNCTION.getOrder() && mPreferences.getBoolean(Panel.FUNCTION.toString(), getResources().getBoolean(R.bool.FUNCTION))) {
                ((ViewGroup) container).addView(mFunctionPage);
                return mFunctionPage;
            }
            else if(position == SmallPanel.ADVANCED.getOrder() && mPreferences.getBoolean(Panel.ADVANCED.toString(), getResources().getBoolean(R.bool.ADVANCED))) {
                ((ViewGroup) container).addView(mAdvancedPage);
                return mAdvancedPage;
            }
            else if(position == SmallPanel.HEX.getOrder() && mPreferences.getBoolean(Panel.HEX.toString(), getResources().getBoolean(R.bool.HEX))) {
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
            if(mPreferences.getBoolean(Panel.HEX.toString(), getResources().getBoolean(R.bool.HEX))) {
                SmallPanel.HEX.setOrder(count);
                count++;
            }
            if(mPreferences.getBoolean(Panel.ADVANCED.toString(), getResources().getBoolean(R.bool.ADVANCED))) {
                SmallPanel.ADVANCED.setOrder(count);
                count++;
            }
            if(mPreferences.getBoolean(Panel.FUNCTION.toString(), getResources().getBoolean(R.bool.FUNCTION))) {
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
            if(position == LargePanel.GRAPH.getOrder() && mPreferences.getBoolean(Panel.GRAPH.toString(), getResources().getBoolean(R.bool.GRAPH))) {
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
            else if(position == LargePanel.BASIC.getOrder() && mPreferences.getBoolean(Panel.BASIC.toString(), getResources().getBoolean(R.bool.BASIC))) {
                ((ViewGroup) container).addView(mSimplePage);
                return mSimplePage;
            }
            else if(position == LargePanel.MATRIX.getOrder() && mPreferences.getBoolean(Panel.MATRIX.toString(), getResources().getBoolean(R.bool.MATRIX))) {
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
            if(mPreferences.getBoolean(Panel.GRAPH.toString(), getResources().getBoolean(R.bool.GRAPH))) {
                LargePanel.GRAPH.setOrder(count);
                count++;
            }
            if(mPreferences.getBoolean(Panel.BASIC.toString(), getResources().getBoolean(R.bool.BASIC))) {
                LargePanel.BASIC.setOrder(count);
                count++;
            }
            if(mPreferences.getBoolean(Panel.MATRIX.toString(), getResources().getBoolean(R.bool.MATRIX))) {
                LargePanel.MATRIX.setOrder(count);
                count++;
            }
        }
    }
}
