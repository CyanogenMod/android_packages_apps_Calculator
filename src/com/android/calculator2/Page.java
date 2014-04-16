/*
 * Copyright (C) 2014 The CyanogenMod Project
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

package com.android.calculator2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import com.android.calculator2.view.CalculatorViewPager;
import com.android.calculator2.view.GraphView;
import com.xlythe.engine.theme.App;

public class Page {
    private final String mName;
    private final String mKey;
    private final boolean mDefaultValue;
    private final boolean mHasTutorial;
    private final Panel mPanel;
    private final boolean mIsSmall;
    private View mView;

    public Page(Context context, Panel panel) {
        mName = context.getString(panel.getName());
        mKey = panel.name();
        mDefaultValue = context.getResources().getBoolean(panel.getDefaultValue());
        mHasTutorial = panel.hasTutorial();
        mPanel = panel;
        mIsSmall = panel.getClass().isAssignableFrom(SmallPanel.class);
    }

    Page(App app) {
        mName = app.getName();
        mKey = app.getPackageName();
        mDefaultValue = true;
        mHasTutorial = false;
        mPanel = null;
        mIsSmall = false;
    }

    Page(Page page) {
        mName = page.getName();
        mKey = page.getKey();
        mDefaultValue = page.getDefaultValue();
        mHasTutorial = page.hasTutorial();
        mPanel = page.mPanel;
        mIsSmall = page.isSmall();
    }

    public static List<Page> getAllPages(Context context) {
        ArrayList<Page> list = new ArrayList<Page>();
        for (Panel p : NormalPanel.values()) {
            Page page = new Page(context, p);
            list.add(page);
        }

        /* TODO: Readd when extensions are added
        List<App> extensions = Extension.getApps(context);
        for (App a : extensions) {
            Page page = new Page(a);
            list.add(page);
        } */

        Collections.sort(list, new PageSort(context));
        return list;
    }

    public static List<Page> getPages(Context context) {
        ArrayList<Page> list = new ArrayList<Page>();
        for (Panel p : NormalPanel.values()) {
            Page page = new Page(context, p);
            if (CalculatorSettings.isPageEnabled(context, page)) {
                list.add(page);
            }
        }

        /* TODO: Readd when extensions are added
        List<App> extensions = Extension.getApps(context);
        for (App a : extensions) {
            Page page = new Page(a);
            if (CalculatorSettings.isPageEnabled(context, page)) {
                list.add(page);
            }
        } */

        Collections.sort(list, new PageSort(context));
        while (list.size() != 0 && list.size() < 4
                && CalculatorSettings.useInfiniteScrolling(context)) {
            // Double the records to avoid using the same view twice
            int size = list.size();
            for (int i = 0; i < size; i++) {
                list.add(new Page(list.get(i)));
            }
        }

        return list;
    }

    public static List<Page> getSmallPages(Context context) {
        ArrayList<Page> list = new ArrayList<Page>();
        for (Panel p : SmallPanel.values()) {
            Page page = new Page(context, p);
            if (CalculatorSettings.isPageEnabled(context, page)) {
                list.add(page);
            }
        }

        Collections.sort(list, new PageSort(context));
        while (list.size() != 0 && list.size() < 4
                && CalculatorSettings.useInfiniteScrolling(context)) {
            // Double the records to avoid using the same view twice
            int size = list.size();
            for (int i = 0; i < size; i++) {
                list.add(new Page(list.get(i)));
            }
        }

        return list;
    }

    public static List<Page> getLargePages(Context context) {
        ArrayList<Page> list = new ArrayList<Page>();
        for (Panel p : LargePanel.values()) {
            Page page = new Page(context, p);
            if (CalculatorSettings.isPageEnabled(context, page)) {
                list.add(page);
            }
        }

        /* TODO Readd when extensions are added
        List<App> extensions = Extension.getApps(context);
        for (App a : extensions) {
            Page page = new Page(a);
            if (CalculatorSettings.isPageEnabled(context, page)) {
                list.add(page);
            }
        } */

        Collections.sort(list, new PageSort(context));
        while (list.size() != 0 && list.size() < 4
                && CalculatorSettings.useInfiniteScrolling(context)) {
            // Double the records to avoid using the same view twice
            int size = list.size();
            for (int i = 0; i < size; i++) {
                list.add(new Page(list.get(i)));
            }
        }

        return list;
    }

    public static Page getCurrentPage(CalculatorViewPager pager) {
        List<Page> pages = ((CalculatorPageAdapter) pager.getAdapter()).getPages();
        if (pages.size() != 0) {
            return pages.get(pager.getCurrentItem() % pages.size());
        } else {
            return null;
        }
    }

    public static Page getPage(List<Page> pages, String name) {
        for (Page p : pages) {
            if (p.getName().equals(name)) {
                return p;
            }
        }

        return null;
    }

    public static int getOrder(List<Page> pages, Page page) {
        for (int i = 0; i < pages.size(); i++) {
            Page p = pages.get(i);
            if (p.equals(page)) {
                return i;
            }
        }

        return -1;
    }

    public static List<Page> removeDuplicates(List<Page> pages) {
        ArrayList<Page> clean = new ArrayList<Page>();
        for (Page p : pages) {
            if (clean.contains(p)) {
                continue;
            }

            clean.add(p);
        }

        return clean;
    }

    public String getName() {
        return mName;
    }

    public String getKey() {
        return mKey;
    }

    public boolean getDefaultValue() {
        return mDefaultValue;
    }

    public boolean hasTutorial() {
        return mHasTutorial;
    }

    public boolean isGraph() {
        return (mPanel != null)
                && (NormalPanel.GRAPH.equals(mPanel) || LargePanel.GRAPH.equals(mPanel));
    }

    public boolean isBasic() {
        return (mPanel != null)
                && (NormalPanel.BASIC.equals(mPanel) || LargePanel.BASIC.equals(mPanel));
    }

    public boolean isAdvanced() {
        return (mPanel != null)
                && (NormalPanel.ADVANCED.equals(mPanel) || SmallPanel.ADVANCED.equals(mPanel));
    }

    public boolean isSmall() {
        return mIsSmall;
    }

    public boolean isLarge() {
        return !mIsSmall;
    }

    public void showTutorial(Calculator calc, boolean animate) {
        if (mPanel != null && mPanel.hasTutorial()) {
            mPanel.showTutorial(calc, animate);
        }
    }

    public View getView(Context context) {
        return getView(context, null, null, null);
    }

    public View getView(Context context, EventListener listener, Graph graph, Logic logic) {
        if (mPanel != null) {
            if (mView == null) {
                mView = mPanel.getView(context);
                mView.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        mView = null;
                    }

                    @Override
                    public void onViewAttachedToWindow(View v) {
                        // Do nothing here
                    }
                });
            }

            if (logic != null) {
                mPanel.refresh(context, mView, listener, graph, logic);
            }
        } else {
            mView = null;
        }

        return mView;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null) {
            if (obj.getClass().isAssignableFrom(Page.class)) {
                Page p = (Page) obj;
                if (p.getKey() == null || getKey() == null) {
                    return p.getKey() == null && getKey() == null;
                } else {
                    return getKey().equals(p.getKey());
                }
            }
        }

        return false;
    }

    public enum NormalPanel implements Panel {
        GRAPH(R.string.graph, R.bool.GRAPH, true),
        HEX(R.string.hexPanel, R.bool.HEX, true),
        BASIC(R.string.basic, R.bool.BASIC, true),
        ADVANCED(R.string.advanced, R.bool.ADVANCED, false),
        MATRIX(R.string.matrix, R.bool.MATRIX, true);
        final int mName;
        final int mDefaultValue;
        final boolean mHasTutorial;
        private View mMatrixView;
        private boolean mLaunchMatrixCling = false;
        private Map<View, GraphView> mGraphHolder = new HashMap<View, GraphView>();

        NormalPanel(int name, int defaultValue, boolean hasTutorial) {
            mName = name;
            mDefaultValue = defaultValue;
            mHasTutorial = hasTutorial;
        }

        public int getName() {
            return mName;
        }

        public int getDefaultValue() {
            return mDefaultValue;
        }

        public boolean hasTutorial() {
            return mHasTutorial;
        }

        public View getView(Context context) {
            View v = null;
            switch (this) {
                case BASIC:
                    v = View.inflate(context, R.layout.simple_pad, null);
                    break;
                case GRAPH:
                    v = View.inflate(context, R.layout.graph_pad, null);
                    break;
                case MATRIX:
                    v = View.inflate(context, R.layout.matrix_pad, null);
                    break;
                case ADVANCED:
                    v = View.inflate(context, R.layout.advanced_pad, null);
                    break;
                case HEX:
                    v = View.inflate(context, R.layout.hex_pad, null);
                    break;
            }

            return v;
        }

        public void showTutorial(final Calculator calc, boolean animate) {
            switch (this) {
                case BASIC:
                    calc.showFirstRunSimpleCling(animate);
                    break;
                case GRAPH:
                    calc.showFirstRunGraphCling(animate);
                    break;
                case MATRIX:
                    if (mMatrixView == null || !mMatrixView.isEnabled()) {
                        mLaunchMatrixCling = true;
                    } else {
                        calc.showFirstRunMatrixCling(animate, mMatrixView);
                    }
                    break;
                case HEX:
                    calc.showFirstRunHexCling(animate);
                    break;
            }
        }

        @Override
        public void refresh(Context context, final View view, EventListener listener,
                final Graph graph, final Logic logic) {
            if (NormalPanel.GRAPH.equals(this)) {
                if (!mGraphHolder.containsKey(view)) {
                    final GraphView graphView = logic.mGraphView = graph.createGraph(context);
                    mGraphHolder.put(view, graphView);
                    graphView.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                        @Override
                        public void onViewDetachedFromWindow(View v) {
                            mGraphHolder.remove(view);
                        }

                        @Override
                        public void onViewAttachedToWindow(View v) {
                            // Do nothing here
                        }
                    });

                    graphView.getViewTreeObserver().addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    logic.getGraphModule().updateGraph(graph);
                                }
                            });

                    logic.setGraphDisplay(graphView);
                    LinearLayout l = (LinearLayout) view.findViewById(R.id.graph);
                    l.addView(graphView, new LayoutParams(LayoutParams.MATCH_PARENT,
                            LayoutParams.MATCH_PARENT));

                    listener.setGraphDisplay(graphView);
                    View zoomIn = view.findViewById(R.id.zoomIn);
                    zoomIn.setOnClickListener(listener);

                    View zoomOut = view.findViewById(R.id.zoomOut);
                    zoomOut.setOnClickListener(listener);

                    View zoomReset = view.findViewById(R.id.zoomReset);
                    zoomReset.setOnClickListener(listener);

                    logic.getGraphModule().updateGraph(graph);
                } else {
                    mGraphHolder.get(view).invalidate();
                }
            } else if(NormalPanel.HEX.equals(this)) {
                if (logic != null) {
                    switch (logic.getBaseModule().getMode()) {
                        case BINARY:
                            view.findViewById(R.id.bin).setSelected(true);
                            break;
                        case DECIMAL:
                            view.findViewById(R.id.dec).setSelected(true);
                            break;
                        case HEXADECIMAL:
                            view.findViewById(R.id.hex).setSelected(true);
                            break;
                    }
                }
            } else if (NormalPanel.MATRIX.equals(this)) {
                mMatrixView = view;
                mMatrixView.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {
                        v.setEnabled(true);
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        v.setEnabled(false);
                    }
                });

                if (mLaunchMatrixCling) {
                    mMatrixView.getViewTreeObserver().addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @SuppressLint("NewApi")
                                @SuppressWarnings("deprecation")
                                @Override
                                public void onGlobalLayout() {
                                    if (android.os.Build.VERSION.SDK_INT < 16) {
                                        mMatrixView.getViewTreeObserver()
                                                .removeGlobalOnLayoutListener(this);
                                    } else {
                                        mMatrixView.getViewTreeObserver()
                                                .removeOnGlobalLayoutListener(this);
                                    }

                                    ((Calculator) mMatrixView.getContext())
                                            .showFirstRunMatrixCling(false, mMatrixView);
                                }
                            });
                }

                View easterEgg = view.findViewById(R.id.easter);
                if (easterEgg != null && listener != null) {
                    easterEgg.setOnClickListener(listener);
                    easterEgg.setOnLongClickListener(listener);
                }
            }
        }
    }

    public enum SmallPanel implements Panel {
        HEX(R.string.hexPanel, R.bool.HEX, true),
        ADVANCED(R.string.advanced, R.bool.ADVANCED, false);
        final int mName;
        final int mDefaultValue;
        final boolean mHasTutorial;

        SmallPanel(int name, int defaultValue, boolean hasTutorial) {
            mName = name;
            mDefaultValue = defaultValue;
            mHasTutorial = hasTutorial;
        }

        public int getName() {
            return mName;
        }

        public int getDefaultValue() {
            return mDefaultValue;
        }

        public boolean hasTutorial() {
            return mHasTutorial;
        }

        public View getView(Context context) {
            View v = null;
            switch (this) {
                case ADVANCED:
                    v = View.inflate(context, R.layout.advanced_pad, null);
                    break;
                case HEX:
                    v = View.inflate(context, R.layout.hex_pad, null);
                    break;
            }

            return v;
        }

        public void showTutorial(Calculator calc, boolean animate) {
            if (this == HEX) {
                calc.showFirstRunHexCling(animate);
            }
        }

        @Override
        public void refresh(Context context, View view, EventListener listener, Graph graph,
                Logic logic) {
            if (SmallPanel.HEX.equals(this)) {
                if (logic != null) {
                    switch (logic.getBaseModule().getMode()) {
                        case BINARY:
                            view.findViewById(R.id.bin).setSelected(true);
                            break;
                        case DECIMAL:
                            view.findViewById(R.id.dec).setSelected(true);
                            break;
                        case HEXADECIMAL:
                            view.findViewById(R.id.hex).setSelected(true);
                            break;
                    }
                }
            }
        }
    }

    public enum LargePanel implements Panel {
        GRAPH(R.string.graph, R.bool.GRAPH, true),
        BASIC(R.string.basic, R.bool.BASIC, true),
        MATRIX(R.string.matrix, R.bool.MATRIX, true);
        final int mName;
        final int mDefaultValue;
        final boolean mHasTutorial;
        private View mMatrixView;
        private boolean mLaunchMatrixCling = false;
        private Map<View, GraphView> mGraphHolder = new HashMap<View, GraphView>();

        LargePanel(int name, int defaultValue, boolean hasTutorial) {
            mName = name;
            mDefaultValue = defaultValue;
            mHasTutorial = hasTutorial;
        }

        public int getName() {
            return mName;
        }

        public int getDefaultValue() {
            return mDefaultValue;
        }

        public boolean hasTutorial() {
            return mHasTutorial;
        }

        public View getView(Context context) {
            View v = null;
            switch (this) {
                case BASIC:
                    v = View.inflate(context, R.layout.simple_pad, null);
                    break;
                case GRAPH:
                    v = View.inflate(context, R.layout.graph_pad, null);
                    break;
                case MATRIX:
                    v = View.inflate(context, R.layout.matrix_pad, null);
                    break;
            }

            return v;
        }

        public void showTutorial(Calculator calc, boolean animate) {
            switch(this) {
                case BASIC:
                    calc.showFirstRunSimpleCling(animate);
                    break;
                case GRAPH:
                    calc.showFirstRunGraphCling(animate);
                    break;
                case MATRIX:
                    if (mMatrixView == null || !mMatrixView.isEnabled()) {
                        mLaunchMatrixCling = true;
                    } else {
                        calc.showFirstRunMatrixCling(animate, mMatrixView);
                    }
                    break;
            }
        }

        @Override
        public void refresh(Context context, final View view, EventListener listener,
                final Graph graph, final Logic logic) {
            if (LargePanel.GRAPH.equals(this)) {
                if (!mGraphHolder.containsKey(view)) {
                    final GraphView graphView = logic.mGraphView = graph.createGraph(context);
                    mGraphHolder.put(view, graphView);
                    graphView.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                        @Override
                        public void onViewDetachedFromWindow(View v) {
                            mGraphHolder.remove(view);
                        }

                        @Override
                        public void onViewAttachedToWindow(View v) {
                            // Do nothing here
                        }
                    });

                    graphView.getViewTreeObserver().addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    logic.getGraphModule().updateGraph(graph);
                                }
                            });

                    logic.setGraphDisplay(graphView);
                    LinearLayout l = (LinearLayout) view.findViewById(R.id.graph);
                    l.addView(graphView, new LayoutParams(LayoutParams.MATCH_PARENT,
                            LayoutParams.MATCH_PARENT));

                    listener.setGraphDisplay(graphView);
                    View zoomIn = view.findViewById(R.id.zoomIn);
                    zoomIn.setOnClickListener(listener);

                    View zoomOut = view.findViewById(R.id.zoomOut);
                    zoomOut.setOnClickListener(listener);

                    View zoomReset = view.findViewById(R.id.zoomReset);
                    zoomReset.setOnClickListener(listener);

                    logic.getGraphModule().updateGraph(graph);
                } else {
                    mGraphHolder.get(view).invalidate();
                }
            } else if (LargePanel.MATRIX.equals(this)) {
                mMatrixView = view;
                mMatrixView.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {
                        v.setEnabled(true);
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        v.setEnabled(false);
                    }
                });

                if (mLaunchMatrixCling) {
                    mMatrixView.getViewTreeObserver().addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @SuppressLint("NewApi")
                                @SuppressWarnings("deprecation")
                                @Override
                                public void onGlobalLayout() {
                                    if (android.os.Build.VERSION.SDK_INT < 16) {
                                        mMatrixView.getViewTreeObserver()
                                                .removeGlobalOnLayoutListener(this);
                                    } else {
                                        mMatrixView.getViewTreeObserver()
                                                .removeOnGlobalLayoutListener(this);
                                    }

                                    ((Calculator) mMatrixView.getContext())
                                            .showFirstRunMatrixCling(false, mMatrixView);
                                }
                            });
                }

                View easterEgg = view.findViewById(R.id.easter);
                if(easterEgg != null && listener != null) {
                    easterEgg.setOnClickListener(listener);
                    easterEgg.setOnLongClickListener(listener);
                }
            }
        }
    }

    public interface Panel {
        public String name();

        public int getName();

        public int getDefaultValue();

        public boolean hasTutorial();

        public View getView(Context context);

        public void showTutorial(Calculator calc, boolean animate);

        public void refresh(Context context, View view, EventListener listener,
                Graph graph, Logic logic);
    }

    private static class PageSort implements Comparator<Page> {
        private final Context context;

        private PageSort(Context context) {
            this.context = context;
        }

        @Override
        public int compare(Page lhs, Page rhs) {
            int rhsOrder = CalculatorSettings.getPageOrder(context, rhs);
            int lhsOrder = CalculatorSettings.getPageOrder(context, lhs);
            return lhsOrder - rhsOrder;
        }
    }
}
