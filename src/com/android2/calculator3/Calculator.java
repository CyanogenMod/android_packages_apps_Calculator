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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.android2.calculator3.Page.LargePanel;
import com.android2.calculator3.Page.NormalPanel;
import com.android2.calculator3.Page.SmallPanel;
import com.android2.calculator3.view.CalculatorDisplay;
import com.android2.calculator3.view.CalculatorViewPager;
import com.android2.calculator3.view.Cling;
import com.android2.calculator3.view.HistoryLine;
import com.xlythe.engine.theme.Theme;
import com.xlythe.slider.Slider;
import com.xlythe.slider.Slider.Direction;

public class Calculator extends Activity implements Logic.Listener, OnClickListener, OnMenuItemClickListener, CalculatorViewPager.OnPageChangeListener {
    public EventListener mListener = new EventListener();
    private CalculatorDisplay mDisplay;
    private Persist mPersist;
    private History mHistory;
    private ListView mHistoryView;
    private BaseAdapter mHistoryAdapter;
    private Logic mLogic;
    private CalculatorViewPager mPager;
    private CalculatorViewPager mSmallPager;
    private CalculatorViewPager mLargePager;
    private View mClearButton;
    private View mBackspaceButton;
    private View mOverflowMenuButton;
    private Slider mHistorySlider;
    private Graph mGraph;

    private boolean clingActive = false;

    private static final String STATE_CURRENT_VIEW = "state-current-view";
    private static final String STATE_CURRENT_VIEW_SMALL = "state-current-view-small";
    private static final String STATE_CURRENT_VIEW_LARGE = "state-current-view-large";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Disable IME for this application
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM, WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        Theme.buildResourceMap(R.color.class, R.drawable.class, R.raw.class);
        Theme.setPackageName(CalculatorSettings.getTheme(getContext()));
        int customTheme = Theme.getTheme(getContext());
        if(customTheme != 0) {
            super.setTheme(customTheme);
        }

        setContentView(R.layout.main);

        mPager = (CalculatorViewPager) findViewById(R.id.panelswitch);
        mSmallPager = (CalculatorViewPager) findViewById(R.id.smallPanelswitch);
        mLargePager = (CalculatorViewPager) findViewById(R.id.largePanelswitch);

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

        mHistory = mPersist.mHistory;

        mDisplay = (CalculatorDisplay) findViewById(R.id.display);

        mLogic = new Logic(this, mHistory, mDisplay);
        mLogic.setListener(this);
        if(mPersist.getMode() != null) mLogic.mBaseModule.setMode(mPersist.getMode());

        mLogic.setDeleteMode(mPersist.getDeleteMode());
        mLogic.setLineLength(mDisplay.getMaxDigits());

        mHistoryAdapter = new HistoryAdapter(this, mHistory);
        mHistory.setObserver(mHistoryAdapter);

        mHistorySlider = (Slider) findViewById(R.id.pulldown);
        mHistorySlider.setBarHeight(getResources().getDimensionPixelSize(R.dimen.history_bar_height));
        mHistorySlider.setSlideDirection(Direction.DOWN);
        if(CalculatorSettings.clickToOpenHistory(this)) {
            mHistorySlider.enableClick(true);
            mHistorySlider.enableTouch(false);
        }
        mHistorySlider.setBarBackground(Theme.getDrawable(getContext(), R.drawable.btn_slider));
        mHistorySlider.setBackgroundColor(Theme.getColor(getContext(), R.color.slider_bg));
        mHistoryView = (ListView) mHistorySlider.findViewById(R.id.history);
        setUpHistory();

        mGraph = new Graph(mLogic);

        if(mPager != null) {
            mPager.setAdapter(new PageAdapter(getContext(), mListener, mGraph, mLogic));
            mPager.scrollToMiddle();
            if(state != null) {
                mPager.setCurrentItem(state.getInt(STATE_CURRENT_VIEW, mPager.getCurrentItem()));
            }
            else {
                scrollToBasicPage();
            }
            mPager.setOnPageChangeListener(this);
            runCling(false);
            mListener.setHandler(this, mLogic, mPager);
        }
        else if(mSmallPager != null && mLargePager != null) {
            // Expanded UI
            mSmallPager.setAdapter(new SmallPageAdapter(getContext(), mLogic));
            mLargePager.setAdapter(new LargePageAdapter(getContext(), mGraph, mLogic));
            mSmallPager.setCurrentItem(state == null ? Page.getSmallOrder(getContext(), new Page(getContext(), SmallPanel.ADVANCED)) : state.getInt(STATE_CURRENT_VIEW_SMALL, Page.getSmallOrder(getContext(), new Page(getContext(), SmallPanel.ADVANCED))));
            mLargePager.setCurrentItem(state == null ? Page.getLargeOrder(getContext(), new Page(getContext(), LargePanel.BASIC)) : state.getInt(STATE_CURRENT_VIEW_LARGE, Page.getLargeOrder(getContext(), new Page(getContext(), LargePanel.BASIC))));
            mSmallPager.setOnPageChangeListener(this);
            mLargePager.setOnPageChangeListener(this);
            runCling(false);
            mListener.setHandler(this, mLogic, mSmallPager, mLargePager);
        }

        mDisplay.setOnKeyListener(mListener);

        if(!ViewConfiguration.get(this).hasPermanentMenuKey()) {
            createFakeMenu();
        }

        mLogic.resumeWithHistory();
        updateDeleteMode();

        mHistorySlider.bringToFront();
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
        getMenuInflater().inflate(R.menu.menu_top, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        Page page = mPager == null ? Page.getCurrentLargePage(mLargePager) : Page.getCurrentPage(mPager);

        for(int i = 0; i < menu.size(); i++) {
            MenuItem m = menu.getItem(i);
            m.setVisible(!m.getTitle().toString().equals(page.getName()));
        }

        MenuItem mClearHistory = menu.findItem(R.id.clear_history);
        mClearHistory.setVisible(mHistorySlider.isSliderOpen());

        MenuItem mShowHistory = menu.findItem(R.id.show_history);
        mShowHistory.setVisible(!mHistorySlider.isSliderOpen());

        MenuItem mHideHistory = menu.findItem(R.id.hide_history);
        mHideHistory.setVisible(mHistorySlider.isSliderOpen());

        MenuItem mLock = menu.findItem(R.id.lock);
        if(mLock != null) {
            mLock.setVisible(page.isGraph() && getPagingEnabled());
        }

        MenuItem mUnlock = menu.findItem(R.id.unlock);
        if(mUnlock != null) {
            mUnlock.setVisible(page.isGraph() && !getPagingEnabled());
        }

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

        popupMenu.inflate(R.menu.menu_top);

        if(mPager != null) {
            for(Page p : Page.getPages(getContext())) {
                menu.add(p.getName());
            }
        }
        else {
            for(Page p : Page.getSmallPages(getContext())) {
                menu.add(p.getName());
            }
            for(Page p : Page.getLargePages(getContext())) {
                menu.add(p.getName());
            }
        }

        popupMenu.inflate(R.menu.menu_bottom);

        popupMenu.setOnMenuItemClickListener(this);
        onPrepareOptionsMenu(menu);
        return popupMenu;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case R.id.clear_history:
            mHistory.clear();
            mLogic.onClear();
            mHistoryAdapter.notifyDataSetInvalidated();
            break;

        case R.id.show_history:
            mHistorySlider.animateSliderOpen();
            break;

        case R.id.hide_history:
            mHistorySlider.animateSliderClosed();
            break;

        case R.id.lock:
            setPagingEnabled(false);
            break;

        case R.id.unlock:
            setPagingEnabled(true);
            break;

        case R.id.settings:
            startActivity(new Intent(this, Preferences.class));
            finish();
            break;
        default:
            // Menu item is for switching pages
            if(mPager != null) {
                mPager.setCurrentItem(Page.getOrder(getContext(), Page.getPage(getContext(), item.getTitle().toString())));
            }
            else {
                Page page = Page.getSmallPage(getContext(), item.getTitle().toString());
                if(page != null) {
                    mSmallPager.setCurrentItem(Page.getSmallOrder(getContext(), page));
                }
                else {
                    page = Page.getLargePage(getContext(), item.getTitle().toString());
                    mLargePager.setCurrentItem(Page.getLargeOrder(getContext(), page));
                }
            }
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
        mPersist.setMode(mLogic.mBaseModule.getMode());
        mPersist.save();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if(keyCode == KeyEvent.KEYCODE_BACK && mHistorySlider.isSliderOpen() && !clingActive) {
            mHistorySlider.animateSliderClosed();
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK && mPager != null && !Page.getCurrentPage(mPager).isBasic() && CalculatorSettings.isPageEnabled(getContext(), new Page(getContext(), NormalPanel.BASIC)) && !clingActive) {
            // Infinite scrolling makes this tricky
            scrollToBasicPage();
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK && mSmallPager != null && mLargePager != null && !(Page.getCurrentPage(mSmallPager).isAdvanced() && Page.getCurrentPage(mLargePager).isBasic()) && CalculatorSettings.isPageEnabled(getContext(), new Page(getContext(), LargePanel.BASIC)) && CalculatorSettings.isPageEnabled(getContext(), new Page(getContext(), SmallPanel.ADVANCED)) && !clingActive) {
            mSmallPager.setCurrentItem(Page.getSmallOrder(getContext(), new Page(getContext(), SmallPanel.ADVANCED)));
            mLargePager.setCurrentItem(Page.getLargeOrder(getContext(), new Page(getContext(), LargePanel.BASIC)));
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    @Override
    public void onDeleteModeChange() {
        updateDeleteMode();
    }

    private void setUpHistory() {
        registerForContextMenu(mHistoryView);
        mHistoryView.setAdapter(mHistoryAdapter);
        mHistoryView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        mHistoryView.setStackFromBottom(true);
        mHistoryView.setFocusable(false);
        mHistoryView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int deleteMode = mLogic.getDeleteMode();
                if(mDisplay.getText().isEmpty()) deleteMode = Logic.DELETE_MODE_CLEAR;
                mDisplay.insert(((HistoryLine) view).getHistoryEntry().getEdited());
                mLogic.setDeleteMode(deleteMode);
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        View history = mHistoryAdapter.getView(info.position, null, null);
        if(history instanceof HistoryLine) ((HistoryLine) history).onCreateContextMenu(menu);
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
        setPagingEnabled(false);
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
        setPagingEnabled(true);
        clingActive = false;

        if(cling != null) {
            cling.dismiss();
            ObjectAnimator anim = ObjectAnimator.ofFloat(cling, "alpha", 0f);
            anim.setDuration(duration);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
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
        setPagingEnabled(true);
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

    public void showFirstRunMatrixCling(boolean animate, View matrixPage) {
        // Enable the clings only if they have not been dismissed before
        if(isClingsEnabled() && !CalculatorSettings.isDismissed(getContext(), Cling.MATRIX_CLING_DISMISSED_KEY)) {
            View v = matrixPage.findViewById(R.id.matrix);
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
        Page largePage = mPager == null ? Page.getCurrentLargePage(mLargePager) : Page.getCurrentPage(mPager);
        Page smallPage = mPager == null ? Page.getCurrentSmallPage(mLargePager) : null;
        largePage.showTutorial(this, animate);
        if(smallPage != null) smallPage.showTutorial(this, animate);
    }

    private void setPagingEnabled(boolean enabled) {
        if(mPager != null) mPager.setPagingEnabled(enabled);
        if(mSmallPager != null) mSmallPager.setPagingEnabled(enabled);
        if(mLargePager != null) mLargePager.setPagingEnabled(enabled);
    }

    private boolean getPagingEnabled() {
        if(mPager != null) return mPager.getPagingEnabled();
        if(mSmallPager != null) return mSmallPager.getPagingEnabled();
        if(mLargePager != null) return mLargePager.getPagingEnabled();
        return true;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if(state == 0) {
            setPagingEnabled(true);
            runCling(true);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageSelected(int position) {}

    private void scrollToBasicPage() {
        int order = Page.getOrder(getContext(), new Page(getContext(), NormalPanel.BASIC));
        if(CalculatorSettings.useInfiniteScrolling(getContext())) {
            int pagesSize = Page.getPages(getContext()).size();
            int currentItem = mPager.getCurrentItem();
            int currentItemUp = mPager.getCurrentItem();
            int currentItemDown = mPager.getCurrentItem();
            while(currentItemUp % pagesSize != order && currentItemDown % pagesSize != order) {
                currentItemUp++;
                currentItemDown--;
            }
            if(currentItemUp % pagesSize == order) {
                while(currentItemUp != currentItem) {
                    currentItem++;
                    mPager.setCurrentItem(currentItem);
                }
            }
            else {
                while(currentItemDown != currentItem) {
                    currentItem--;
                    mPager.setCurrentItem(currentItem);
                }
            }
        }
        else {
            mPager.setCurrentItem(order);
        }
    }
}
