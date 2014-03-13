package com.android2.calculator3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.achartengine.GraphicalView;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.android2.calculator3.view.CalculatorViewPager;
import com.xlythe.engine.extension.Extension;
import com.xlythe.engine.theme.App;

public class Page {
    private final String mName;
    private final String mKey;
    private final boolean mDefaultValue;
    private final boolean mHasTutorial;
    private final Panel mPanel;
    private final boolean mIsSmall;
    private View mView;

    public interface Panel {
        public String name();

        public int getName();

        public int getDefaultValue();

        public boolean hasTutorial();

        public View getView(Context context);

        public void showTutorial(Calculator calc, boolean animate);

        public void refresh(Context context, View view, EventListener listener, Graph graph, Logic logic);
    }

    public enum NormalPanel implements Panel {
        GRAPH(R.string.graph, R.bool.GRAPH, true),
        HEX(R.string.hexPanel, R.bool.HEX, true),
        BASIC(R.string.basic, R.bool.BASIC, true),
        ADVANCED(R.string.advanced, R.bool.ADVANCED, false),
        MATRIX(R.string.matrix, R.bool.MATRIX, true);

        NormalPanel(int name, int defaultValue, boolean hasTutorial) {
            mName = name;
            mDefaultValue = defaultValue;
            mHasTutorial = hasTutorial;
        }

        final int mName;
        final int mDefaultValue;
        final boolean mHasTutorial;
        private GraphicalView mGraphDisplay;

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
            switch(this) {
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
            switch(this) {
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
        public void refresh(Context context, View view, EventListener listener, Graph graph, Logic logic) {
            if(NormalPanel.GRAPH.equals(this)) {
                if(mGraphDisplay == null) {
                    mGraphDisplay = graph.getGraph(context);
                    logic.setGraphDisplay(mGraphDisplay);
                    LinearLayout l = (LinearLayout) view.findViewById(R.id.graph);
                    l.addView(mGraphDisplay, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

                    View zoomIn = view.findViewById(R.id.zoomIn);
                    zoomIn.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mGraphDisplay.zoomIn();
                        }
                    });

                    View zoomOut = view.findViewById(R.id.zoomOut);
                    zoomOut.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mGraphDisplay.zoomOut();
                        }
                    });

                    View zoomReset = view.findViewById(R.id.zoomReset);
                    zoomReset.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mGraphDisplay.zoomReset();
                        }
                    });
                }
                else {
                    mGraphDisplay.repaint();
                }
            }
            else if(NormalPanel.HEX.equals(this)) {
                if(logic != null) {
                    switch(logic.mBaseModule.getMode()) {
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
            else if(NormalPanel.MATRIX.equals(this)) {
                View easterEgg = view.findViewById(R.id.easter);
                if(easterEgg != null && listener != null) {
                    easterEgg.setOnClickListener(listener);
                    easterEgg.setOnLongClickListener(listener);
                }
            }
        }
    }

    public enum SmallPanel implements Panel {
        HEX(R.string.hexPanel, R.bool.HEX, true),
        ADVANCED(R.string.advanced, R.bool.ADVANCED, false);

        SmallPanel(int name, int defaultValue, boolean hasTutorial) {
            mName = name;
            mDefaultValue = defaultValue;
            mHasTutorial = hasTutorial;
        }

        final int mName;
        final int mDefaultValue;
        final boolean mHasTutorial;

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
            switch(this) {
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
            switch(this) {
            case ADVANCED:
                break;
            case HEX:
                calc.showFirstRunHexCling(animate);
                break;
            }
        }

        @Override
        public void refresh(Context context, View view, EventListener listener, Graph graph, Logic logic) {
            if(SmallPanel.HEX.equals(this)) {
                if(logic != null) {
                    switch(logic.mBaseModule.getMode()) {
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

        LargePanel(int name, int defaultValue, boolean hasTutorial) {
            mName = name;
            mDefaultValue = defaultValue;
            mHasTutorial = hasTutorial;
        }

        final int mName;
        final int mDefaultValue;
        final boolean mHasTutorial;
        private GraphicalView mGraphDisplay;

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
            switch(this) {
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
                calc.showFirstRunMatrixCling(animate, getView(calc));
                break;
            }
        }

        @Override
        public void refresh(Context context, View view, EventListener listener, Graph graph, Logic logic) {
            if(LargePanel.GRAPH.equals(this)) {
                if(mGraphDisplay == null) {
                    mGraphDisplay = graph.getGraph(context);
                    logic.setGraphDisplay(mGraphDisplay);
                    LinearLayout l = (LinearLayout) view.findViewById(R.id.graph);
                    l.addView(mGraphDisplay, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

                    View zoomIn = view.findViewById(R.id.zoomIn);
                    zoomIn.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mGraphDisplay.zoomIn();
                        }
                    });

                    View zoomOut = view.findViewById(R.id.zoomOut);
                    zoomOut.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mGraphDisplay.zoomOut();
                        }
                    });

                    View zoomReset = view.findViewById(R.id.zoomReset);
                    zoomReset.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mGraphDisplay.zoomReset();
                        }
                    });
                }
                else {
                    mGraphDisplay.repaint();
                }
            }
        }
    }

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
        List<App> extensions = Extension.getApps(context);
        ArrayList<Page> list = new ArrayList<Page>();
        for(Panel p : NormalPanel.values()) {
            Page page = new Page(context, p);
            list.add(page);
        }
        for(App a : extensions) {
            Page page = new Page(a);
            list.add(page);
        }
        Collections.sort(list, new PageSort(context));
        return list;
    }

    public static List<Page> getPages(Context context) {
        List<App> extensions = Extension.getApps(context);
        ArrayList<Page> list = new ArrayList<Page>();
        for(Panel p : NormalPanel.values()) {
            Page page = new Page(context, p);
            if(CalculatorSettings.isPageEnabled(context, page)) {
                list.add(page);
            }
        }
        for(App a : extensions) {
            Page page = new Page(a);
            if(CalculatorSettings.isPageEnabled(context, page)) {
                list.add(page);
            }
        }
        Collections.sort(list, new PageSort(context));
        while(list.size() != 0 && list.size() < 4 && CalculatorSettings.useInfiniteScrolling(context)) {
            // Double the records to avoid using the same view twice
            int size = list.size();
            for(int i = 0; i < size; i++) {
                list.add(new Page(list.get(i)));
            }
        }
        return list;
    }

    public static List<Page> getSmallPages(Context context) {
        ArrayList<Page> list = new ArrayList<Page>();
        for(Panel p : SmallPanel.values()) {
            Page page = new Page(context, p);
            if(CalculatorSettings.isPageEnabled(context, page)) {
                list.add(page);
            }
        }
        Collections.sort(list, new PageSort(context));
        while(list.size() != 0 && list.size() < 4 && CalculatorSettings.useInfiniteScrolling(context)) {
            // Double the records to avoid using the same view twice
            int size = list.size();
            for(int i = 0; i < size; i++) {
                list.add(new Page(list.get(i)));
            }
        }
        return list;
    }

    public static List<Page> getLargePages(Context context) {
        List<App> extensions = Extension.getApps(context);
        ArrayList<Page> list = new ArrayList<Page>();
        for(Panel p : LargePanel.values()) {
            Page page = new Page(context, p);
            if(CalculatorSettings.isPageEnabled(context, page)) {
                list.add(page);
            }
        }
        for(App a : extensions) {
            Page page = new Page(a);
            if(CalculatorSettings.isPageEnabled(context, page)) {
                list.add(page);
            }
        }
        Collections.sort(list, new PageSort(context));
        while(list.size() != 0 && list.size() < 4 && CalculatorSettings.useInfiniteScrolling(context)) {
            // Double the records to avoid using the same view twice
            int size = list.size();
            for(int i = 0; i < size; i++) {
                list.add(new Page(list.get(i)));
            }
        }
        return list;
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
        if(mPanel != null && mPanel.hasTutorial()) {
            mPanel.showTutorial(calc, animate);
        }
    }

    public View getView(Context context, EventListener listener, Graph graph, Logic logic) {
        if(mView == null) {
            if(mPanel != null) {
                mView = mPanel.getView(context);
                mPanel.refresh(context, mView, listener, graph, logic);
            }
            else mView = null;
        }
        return mView;
    }

    public static Page getCurrentPage(CalculatorViewPager pager) {
        List<Page> pages = ((CalculatorPageAdapter) pager.getAdapter()).getPages();
        return pages.get(pager.getCurrentItem() % pages.size());
    }

    public static Page getPage(List<Page> pages, String name) {
        for(Page p : pages) {
            if(p.getName().equals(name)) return p;
        }
        return null;
    }

    public static int getOrder(List<Page> pages, Page page) {
        for(int i = 0; i < pages.size(); i++) {
            Page p = pages.get(i);
            if(p.equals(page)) return i;
        }
        return -1;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj != null) {
            if(obj.getClass().isAssignableFrom(Page.class)) {
                Page p = (Page) obj;
                if(p.getKey() == null || getKey() == null) {
                    return p.getKey() == null && getKey() == null;
                }
                else {
                    return getKey().equals(p.getKey());
                }
            }
        }
        return false;
    }
}
