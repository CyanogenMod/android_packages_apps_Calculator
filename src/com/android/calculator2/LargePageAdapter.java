package com.android.calculator2;

import org.achartengine.GraphicalView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.android.calculator2.BaseModule.Mode;
import com.android.calculator2.Calculator.LargePanel;
import com.android.calculator2.view.CalculatorViewPager;

public class LargePageAdapter extends CalculatorPageAdapter {
    private final ViewGroup mGraphPage;
    private final ViewGroup mSimplePage;
    final ViewGroup mMatrixPage;
    private final CalculatorViewPager mParent;
    private GraphicalView mGraphDisplay;
    private final Graph mGraph;
    private final Logic mLogic;
    private int mCount = 0;

    public LargePageAdapter(CalculatorViewPager parent, Graph graph, Logic logic) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        mGraphPage = (ViewGroup) inflater.inflate(R.layout.graph_pad, parent, false);
        mSimplePage = (ViewGroup) inflater.inflate(R.layout.simple_pad, parent, false);
        mMatrixPage = (ViewGroup) inflater.inflate(R.layout.matrix_pad, parent, false);

        mParent = parent;
        mGraph = graph;
        mLogic = logic;
        setOrder();

        applyBannedResources(mLogic.mBaseModule.getMode());
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public View getViewAt(int position) {
        if(position == LargePanel.GRAPH.getOrder() && CalculatorSettings.graphPanel(mParent.getContext())) {
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
            return mGraphPage;
        }
        else if(position == LargePanel.BASIC.getOrder() && CalculatorSettings.basicPanel(mParent.getContext())) {
            return mSimplePage;
        }
        else if(position == LargePanel.MATRIX.getOrder() && CalculatorSettings.matrixPanel(mParent.getContext())) {
            return mMatrixPage;
        }
        return null;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();

        setOrder();
    }

    private void setOrder() {
        mCount = 0;
        if(CalculatorSettings.graphPanel(mParent.getContext())) {
            LargePanel.GRAPH.setOrder(mCount);
            mCount++;
        }
        if(CalculatorSettings.basicPanel(mParent.getContext())) {
            LargePanel.BASIC.setOrder(mCount);
            mCount++;
        }
        if(CalculatorSettings.matrixPanel(mParent.getContext())) {
            LargePanel.MATRIX.setOrder(mCount);
            mCount++;
        }
    }

    private void applyBannedResources(Mode baseMode) {
        applyBannedResourcesByPage(mLogic, mGraphPage, baseMode);
        applyBannedResourcesByPage(mLogic, mSimplePage, baseMode);
        applyBannedResourcesByPage(mLogic, mMatrixPage, baseMode);
    }
}
