package com.android.calculator2;

import org.achartengine.GraphicalView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.android.calculator2.BaseModule.Mode;
import com.android.calculator2.Calculator.Panel;
import com.android.calculator2.view.CalculatorViewPager;

public class PageAdapter extends CalculatorPageAdapter {
    private final ViewGroup mGraphPage;
    private final ViewGroup mFunctionPage;
    private final ViewGroup mSimplePage;
    private final ViewGroup mAdvancedPage;
    private final ViewGroup mHexPage;
    ViewGroup mMatrixPage;
    private final CalculatorViewPager mParent;
    private GraphicalView mGraphDisplay;
    private final Graph mGraph;
    private final Logic mLogic;
    private int mCount = 0;

    public PageAdapter(CalculatorViewPager parent, EventListener listener, Graph graph, Logic logic) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        mGraphPage = (ViewGroup) inflater.inflate(R.layout.graph_pad, parent, false);
        mFunctionPage = (ViewGroup) inflater.inflate(R.layout.function_pad, parent, false);
        mSimplePage = (ViewGroup) inflater.inflate(R.layout.simple_pad, parent, false);
        mAdvancedPage = (ViewGroup) inflater.inflate(R.layout.advanced_pad, parent, false);
        mHexPage = (ViewGroup) inflater.inflate(R.layout.hex_pad, parent, false);
        mMatrixPage = (ViewGroup) inflater.inflate(R.layout.matrix_pad, parent, false);

        mParent = parent;
        mGraph = graph;
        mLogic = logic;
        setOrder();

        applyBannedResources(mLogic.mBaseModule.getMode());
        switch(mLogic.mBaseModule.getMode()) {
        case BINARY:
            mHexPage.findViewById(R.id.bin).setSelected(true);
            break;
        case DECIMAL:
            mHexPage.findViewById(R.id.dec).setSelected(true);
            break;
        case HEXADECIMAL:
            mHexPage.findViewById(R.id.hex).setSelected(true);
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
        return mCount;
    }

    @Override
    public View getViewAt(int position) {
        View v = null;
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
            v = mGraphPage;
        }
        else if(position == Panel.FUNCTION.getOrder() && CalculatorSettings.functionPanel(mParent.getContext())) {
            v = mFunctionPage;
        }
        else if(position == Panel.BASIC.getOrder() && CalculatorSettings.basicPanel(mParent.getContext())) {
            v = mSimplePage;
        }
        else if(position == Panel.ADVANCED.getOrder() && CalculatorSettings.advancedPanel(mParent.getContext())) {
            v = mAdvancedPage;
        }
        else if(position == Panel.HEX.getOrder() && CalculatorSettings.hexPanel(mParent.getContext())) {
            v = mHexPage;
        }
        else if(position == Panel.MATRIX.getOrder() && CalculatorSettings.matrixPanel(mParent.getContext())) {
            return mMatrixPage;
        }

        return v;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();

        setOrder();
    }

    private void setOrder() {
        mCount = 0;
        if(CalculatorSettings.graphPanel(mParent.getContext())) {
            Panel.GRAPH.setOrder(mCount);
            mCount++;
        }
        if(CalculatorSettings.functionPanel(mParent.getContext())) {
            Panel.FUNCTION.setOrder(mCount);
            mCount++;
        }
        if(CalculatorSettings.hexPanel(mParent.getContext())) {
            Panel.HEX.setOrder(mCount);
            mCount++;
        }
        if(CalculatorSettings.basicPanel(mParent.getContext())) {
            Panel.BASIC.setOrder(mCount);
            mCount++;
        }
        if(CalculatorSettings.advancedPanel(mParent.getContext())) {
            Panel.ADVANCED.setOrder(mCount);
            mCount++;
        }
        if(CalculatorSettings.matrixPanel(mParent.getContext())) {
            Panel.MATRIX.setOrder(mCount);
            mCount++;
        }
    }

    private void applyBannedResources(Mode baseMode) {
        applyBannedResourcesByPage(mLogic, mGraphPage, baseMode);
        applyBannedResourcesByPage(mLogic, mFunctionPage, baseMode);
        applyBannedResourcesByPage(mLogic, mSimplePage, baseMode);
        applyBannedResourcesByPage(mLogic, mAdvancedPage, baseMode);
        applyBannedResourcesByPage(mLogic, mHexPage, baseMode);
        applyBannedResourcesByPage(mLogic, mMatrixPage, baseMode);
    }
}
