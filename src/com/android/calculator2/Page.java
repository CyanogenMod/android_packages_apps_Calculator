package com.android.calculator2;

import android.content.Context;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.android.calculator2.view.CalculatorViewPager;
import com.xlythe.engine.theme.App;

import org.achartengine.GraphicalView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        // TODO Readd when extensions are added
        // List<App> extensions = Extension.getApps(context);
        // for(App a : extensions) {
        // Page page = new Page(a);
        // list.add(page);
        // }
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
        // TODO Readd when extensions are added
        // List<App> extensions = Extension.getApps(context);
        // for(App a : extensions) {
        // Page page = new Page(a);
        // if(CalculatorSettings.isPageEnabled(context, page)) {
        // list.add(page);
        // }
        // }
        Collections.sort(list, new PageSort(context));
        while (list.size() != 0 && list.size() < 4 && CalculatorSettings.useInfiniteScrolling(context)) {
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
        while (list.size() != 0 && list.size() < 4 && CalculatorSettings.useInfiniteScrolling(context)) {
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
        // TODO Readd when extensions are added
        // List<App> extensions = Extension.getApps(context);
        // for(App a : extensions) {
        // Page page = new Page(a);
        // if(CalculatorSettings.isPageEnabled(context, page)) {
        // list.add(page);
        // }
        // }
        Collections.sort(list, new PageSort(context));
        while (list.size() != 0 && list.size() < 4 && CalculatorSettings.useInfiniteScrolling(context)) {
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
            if (p.getName().equals(name)) return p;
        }
        return null;
    }

    public static int getOrder(List<Page> pages, Page page) {
        for (int i = 0; i < pages.size(); i++) {
            Page p = pages.get(i);
            if (p.equals(page)) return i;
        }
        return -1;
    }

    public static List<Page> removeDuplicates(List<Page> pages) {
        ArrayList<Page> clean = new ArrayList<Page>();
        for (Page p : pages) {
            if (clean.contains(p)) continue;
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
        return (mPanel != null) && (NormalPanel.GRAPH.equals(mPanel) || LargePanel.GRAPH.equals(mPanel));
    }

    public boolean isBasic() {
        return (mPanel != null) && (NormalPanel.BASIC.equals(mPanel) || LargePanel.BASIC.equals(mPanel));
    }

    public boolean isAdvanced() {
        return (mPanel != null) && (NormalPanel.ADVANCED.equals(mPanel) || SmallPanel.ADVANCED.equals(mPanel));
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
                    }
                });
            }
            if (logic != null) mPanel.refresh(context, mView, listener, graph, logic);
        } else mView = null;
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
        private Map<View, GraphicalView> mGraphHolder = new HashMap<View, GraphicalView>();
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

        public void showTutorial(Calculator calc, boolean animate) {
            switch (this) {
                case BASIC:
                    calc.showFirstRunSimpleCling(animate);
                    break;
                case GRAPH:
                    calc.showFirstRunGraphCling(animate);
                    break;
                case MATRIX:
                    calc.showFirstRunMatrixCling(animate, getView(calc));
                    break;
                case ADVANCED:
                    break;
                case HEX:
                    calc.showFirstRunHexCling(animate);
                    break;
            }
        }

        @Override
        public void refresh(Context context, final View view, EventListener listener, Graph graph, Logic logic) {
            if (NormalPanel.GRAPH.equals(this)) {
                if (!mGraphHolder.containsKey(view)) {
                    final GraphicalView graphDisplay = graph.getGraph(context);
                    mGraphHolder.put(view, graphDisplay);
                    graphDisplay.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                        @Override
                        public void onViewDetachedFromWindow(View v) {
                            mGraphHolder.remove(view);
                        }

                        @Override
                        public void onViewAttachedToWindow(View v) {
                        }
                    });
                    logic.setGraphDisplay(graphDisplay);
                    LinearLayout l = (LinearLayout) view.findViewById(R.id.graph);
                    l.addView(graphDisplay, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

                    listener.setGraphDisplay(graphDisplay);
                    View zoomIn = view.findViewById(R.id.zoomIn);
                    zoomIn.setOnClickListener(listener);

                    View zoomOut = view.findViewById(R.id.zoomOut);
                    zoomOut.setOnClickListener(listener);

                    View zoomReset = view.findViewById(R.id.zoomReset);
                    zoomReset.setOnClickListener(listener);
                } else {
                    mGraphHolder.get(view).repaint();
                }
            } else if (NormalPanel.HEX.equals(this)) {
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
            switch (this) {
                case ADVANCED:
                    break;
                case HEX:
                    calc.showFirstRunHexCling(animate);
                    break;
            }
        }

        @Override
        public void refresh(Context context, View view, EventListener listener, Graph graph, Logic logic) {
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
        private Map<View, GraphicalView> mGraphHolder = new HashMap<View, GraphicalView>();
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
            switch (this) {
                case BASIC:
                    calc.showFirstRunSimpleCling(animate);
                    break;
                case GRAPH:
                    calc.showFirstRunGraphCling(animate);
                    break;
                case MATRIX:
                    calc.showFirstRunMatrixCling(animate, getView(calc));
                    break;
            }
        }

        @Override
        public void refresh(Context context, final View view, EventListener listener, Graph graph, Logic logic) {
            if (LargePanel.GRAPH.equals(this)) {
                if (!mGraphHolder.containsKey(view)) {
                    final GraphicalView graphDisplay = graph.getGraph(context);
                    mGraphHolder.put(view, graphDisplay);
                    graphDisplay.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                        @Override
                        public void onViewDetachedFromWindow(View v) {
                            mGraphHolder.remove(view);
                        }

                        @Override
                        public void onViewAttachedToWindow(View v) {
                        }
                    });
                    logic.setGraphDisplay(graphDisplay);
                    LinearLayout l = (LinearLayout) view.findViewById(R.id.graph);
                    l.addView(graphDisplay, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

                    listener.setGraphDisplay(graphDisplay);
                    View zoomIn = view.findViewById(R.id.zoomIn);
                    zoomIn.setOnClickListener(listener);

                    View zoomOut = view.findViewById(R.id.zoomOut);
                    zoomOut.setOnClickListener(listener);

                    View zoomReset = view.findViewById(R.id.zoomReset);
                    zoomReset.setOnClickListener(listener);
                } else {
                    mGraphHolder.get(view).repaint();
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

        public void refresh(Context context, View view, EventListener listener, Graph graph, Logic logic);
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
