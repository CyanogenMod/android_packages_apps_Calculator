package com.android.calculator2;

import org.achartengine.GraphicalView;

import java.util.List;

import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.android.calculator2.BaseModule.Mode;
import com.android.calculator2.Calculator.Panel;
import com.android.calculator2.view.CalculatorViewPager;

public class PageAdapter extends PagerAdapter {
    private final ViewGroup mGraphPage;
    private final ViewGroup mFunctionPage;
    private final ViewGroup mSimplePage;
    private final ViewGroup mAdvancedPage;
    private final ViewGroup mHexPage;
    final ViewGroup mMatrixPage;
    private final CalculatorViewPager mParent;
    private GraphicalView mGraphDisplay;

    private final Graph mGraph;
    private final Logic mLogic;

    private int count = 0;

    public PageAdapter(CalculatorViewPager parent, EventListener listener, Graph graph, Logic logic) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        mGraphPage = (ViewGroup)inflater.inflate(R.layout.graph_pad, parent, false);
        mFunctionPage = (ViewGroup)inflater.inflate(R.layout.function_pad, parent, false);
        mSimplePage = (ViewGroup)inflater.inflate(R.layout.simple_pad, parent, false);
        mAdvancedPage = (ViewGroup)inflater.inflate(R.layout.advanced_pad, parent, false);
        mHexPage = (ViewGroup)inflater.inflate(R.layout.hex_pad, parent, false);
        mMatrixPage = (ViewGroup)inflater.inflate(R.layout.matrix_pad, parent, false);

        mParent = parent;
        mGraph = graph;
        mLogic = logic;
        setOrder();

        switch(mLogic.mBaseModule.getMode()) {
        case BINARY:
            mHexPage.findViewById(R.id.bin).setBackgroundResource(R.color.pressed_color);
            applyBannedResources(Mode.BINARY);
            break;
        case DECIMAL:
            mHexPage.findViewById(R.id.dec).setBackgroundResource(R.color.pressed_color);
            applyBannedResources(Mode.DECIMAL);
            break;
        case HEXADECIMAL:
            mHexPage.findViewById(R.id.hex).setBackgroundResource(R.color.pressed_color);
            applyBannedResources(Mode.HEXADECIMAL);
            break;
        }

        View easterEgg = mMatrixPage.findViewById(R.id.easter);
        if(easterEgg != null) {
            easterEgg.setOnClickListener(listener);
            easterEgg.setOnLongClickListener(listener);
        }
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public void startUpdate(View container) {}

    @Override
    public Object instantiateItem(View container, int position) {
        if(position == Panel.GRAPH.getOrder() && CalculatorSettings.graphPanel(mParent.getContext())) {
            if(mGraphDisplay == null) {
                mGraphDisplay = mGraph.getGraph(mParent.getContext());
                mLogic.setGraphDisplay(mGraphDisplay);
                LinearLayout l = (LinearLayout) mGraphPage.findViewById(R.id.graph);
                l.addView(mGraphDisplay, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

                View zoomIn = mGraphPage.findViewById(R.id.zoomIn);
                zoomIn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mGraphDisplay.zoomIn();
                    }
                });

                View zoomOut = mGraphPage.findViewById(R.id.zoomOut);
                zoomOut.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mGraphDisplay.zoomOut();
                    }
                });

                View zoomReset = mGraphPage.findViewById(R.id.zoomReset);
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
            ((ViewGroup) container).addView(mGraphPage);
            applyBannedResourcesByPage(mGraphPage, mLogic.mBaseModule.getMode());
            return mGraphPage;
        }
        else if(position == Panel.FUNCTION.getOrder() && CalculatorSettings.functionPanel(mParent.getContext())) {
            ((ViewGroup) container).addView(mFunctionPage);
            applyBannedResourcesByPage(mFunctionPage, mLogic.mBaseModule.getMode());
            return mFunctionPage;
        }
        else if(position == Panel.BASIC.getOrder() && CalculatorSettings.basicPanel(mParent.getContext())) {
            ((ViewGroup) container).addView(mSimplePage);
            applyBannedResourcesByPage(mSimplePage, mLogic.mBaseModule.getMode());
            return mSimplePage;
        }
        else if(position == Panel.ADVANCED.getOrder() && CalculatorSettings.advancedPanel(mParent.getContext())) {
            ((ViewGroup) container).addView(mAdvancedPage);
            applyBannedResourcesByPage(mAdvancedPage, mLogic.mBaseModule.getMode());
            return mAdvancedPage;
        }
        else if(position == Panel.HEX.getOrder() && CalculatorSettings.hexPanel(mParent.getContext())) {
            ((ViewGroup) container).addView(mHexPage);
            applyBannedResourcesByPage(mHexPage, mLogic.mBaseModule.getMode());
            return mHexPage;
        }
        else if(position == Panel.MATRIX.getOrder() && CalculatorSettings.matrixPanel(mParent.getContext())) {
            ((ViewGroup) container).addView(mMatrixPage);
            applyBannedResourcesByPage(mMatrixPage, mLogic.mBaseModule.getMode());
            return mMatrixPage;
        }
        return null;
    }

    @Override
    public void destroyItem(View container, int position, Object object) {
        ((ViewGroup) container).removeView((View) object);
    }

    @Override
    public void finishUpdate(View container) {}

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {}

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();

        setOrder();
    }

    private void setOrder() {
        count = 0;
        if(CalculatorSettings.graphPanel(mParent.getContext())) {
            Panel.GRAPH.setOrder(count);
            count++;
        }
        if(CalculatorSettings.functionPanel(mParent.getContext())) {
            Panel.FUNCTION.setOrder(count);
            count++;
        }
        if(CalculatorSettings.hexPanel(mParent.getContext())) {
            Panel.HEX.setOrder(count);
            count++;
        }
        if(CalculatorSettings.basicPanel(mParent.getContext())) {
            Panel.BASIC.setOrder(count);
            count++;
        }
        if(CalculatorSettings.advancedPanel(mParent.getContext())) {
            Panel.ADVANCED.setOrder(count);
            count++;
        }
        if(CalculatorSettings.matrixPanel(mParent.getContext())) {
            Panel.MATRIX.setOrder(count);
            count++;
        }
    }

    private void applyBannedResources(Mode baseMode) {
        applyBannedResourcesByPage(mGraphPage, baseMode);
        applyBannedResourcesByPage(mFunctionPage, baseMode);
        applyBannedResourcesByPage(mSimplePage, baseMode);
        applyBannedResourcesByPage(mAdvancedPage, baseMode);
        applyBannedResourcesByPage(mHexPage, baseMode);
        applyBannedResourcesByPage(mMatrixPage, baseMode);
    }

    private void applyBannedResourcesByPage(ViewGroup page, Mode baseMode) {
        final String mode = getBaseMode(baseMode);
        // Enable
        for (String key : mLogic.mBaseModule.mBannedResources.keySet()) {
            if (mode.compareTo(key) != 0) {
                List<Integer> resources = mLogic.mBaseModule.mBannedResources.get(key);
                for (Integer resource : resources) {
                    final int resId = resource.intValue();
                    View v = page.findViewById(resId);
                    if (v != null) v.setEnabled(true);
                }
            }
        }
        // Disable
        List<Integer> resources = mLogic.mBaseModule.mBannedResources.get(mode);
        for (Integer resource : resources) {
            final int resId = resource.intValue();
            View v = page.findViewById(resId);
            if (v != null) v.setEnabled(false);
        }
    }

    private String getBaseMode(Mode baseMode) {
        if (baseMode.compareTo(Mode.BINARY) == 0) return "bin";
        if (baseMode.compareTo(Mode.HEXADECIMAL) == 0) return "hex";
        return "dec";
    }
}
