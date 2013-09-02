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
import com.android.calculator2.Calculator.LargePanel;
import com.android.calculator2.view.CalculatorViewPager;

public class LargePageAdapter extends PagerAdapter {
    private final ViewGroup mGraphPage;
    private final ViewGroup mSimplePage;
    final ViewGroup mMatrixPage;
    private final CalculatorViewPager mParent;
    private GraphicalView mGraphDisplay;

    private final Graph mGraph;
    private final Logic mLogic;

    private int count = 0;

    public LargePageAdapter(CalculatorViewPager parent, Graph graph, Logic logic) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        mGraphPage = (ViewGroup)inflater.inflate(R.layout.graph_pad, parent, false);
        mSimplePage = (ViewGroup)inflater.inflate(R.layout.simple_pad, parent, false);
        mMatrixPage = (ViewGroup)inflater.inflate(R.layout.matrix_pad, parent, false);

        mParent = parent;
        mGraph = graph;
        mLogic = logic;
        setOrder();

        switch(mLogic.mBaseModule.getMode()) {
        case BINARY:
            applyBannedResources(Mode.BINARY);
            break;
        case DECIMAL:
            applyBannedResources(Mode.DECIMAL);
            break;
        case HEXADECIMAL:
            applyBannedResources(Mode.HEXADECIMAL);
            break;
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
            ((ViewGroup) container).addView(mGraphPage);
            applyBannedResourcesByPage(mGraphPage, mLogic.mBaseModule.getMode());
            return mGraphPage;
        }
        else if(position == LargePanel.BASIC.getOrder() && CalculatorSettings.basicPanel(mParent.getContext())) {
            ((ViewGroup) container).addView(mSimplePage);
            applyBannedResourcesByPage(mSimplePage, mLogic.mBaseModule.getMode());
            return mSimplePage;
        }
        else if(position == LargePanel.MATRIX.getOrder() && CalculatorSettings.matrixPanel(mParent.getContext())) {
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
            LargePanel.GRAPH.setOrder(count);
            count++;
        }
        if(CalculatorSettings.basicPanel(mParent.getContext())) {
            LargePanel.BASIC.setOrder(count);
            count++;
        }
        if(CalculatorSettings.matrixPanel(mParent.getContext())) {
            LargePanel.MATRIX.setOrder(count);
            count++;
        }
    }

    private void applyBannedResources(Mode baseMode) {
        applyBannedResourcesByPage(mGraphPage, baseMode);
        applyBannedResourcesByPage(mSimplePage, baseMode);
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
