package com.android2.calculator3;

import org.achartengine.GraphicalView;

import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.android2.calculator3.Calculator.Panel;
import com.android2.calculator3.view.CalculatorViewPager;

public class PageAdapter extends PagerAdapter {
    private final View mGraphPage;
    private final View mFunctionPage;
    private final View mSimplePage;
    private final View mAdvancedPage;
    private final View mHexPage;
    View mMatrixPage;
    private final CalculatorViewPager mParent;
    private GraphicalView mGraphDisplay;

    private final Graph mGraph;
    private final Logic mLogic;

    private int count = 0;

    public PageAdapter(CalculatorViewPager parent, EventListener listener, Graph graph, Logic logic) {
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
        mParent = parent;
        mGraph = graph;
        mLogic = logic;
        setOrder();

        switch(mLogic.mBaseModule.getMode()) {
        case BINARY:
            mHexPage.findViewById(R.id.bin).setSelected(true);
            for(int i : mLogic.mBaseModule.bannedResourceInBinary) {
                View v = mSimplePage.findViewById(i);
                if(v == null) v = mHexPage.findViewById(i);
                v.setEnabled(false);
            }
            break;
        case DECIMAL:
            mHexPage.findViewById(R.id.dec).setSelected(true);
            for(int i : mLogic.mBaseModule.bannedResourceInDecimal) {
                View v = mSimplePage.findViewById(i);
                if(v == null) v = mHexPage.findViewById(i);
                v.setEnabled(false);
            }
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
        return count;
    }

    @Override
    public void startUpdate(View container) {}

    @Override
    public Object instantiateItem(View container, int position) {
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
            ((ViewGroup) container).addView(mGraphPage);
            v = mGraphPage;
        }
        else if(position == Panel.FUNCTION.getOrder() && CalculatorSettings.functionPanel(mParent.getContext())) {
            ((ViewGroup) container).addView(mFunctionPage);
            v = mFunctionPage;
        }
        else if(position == Panel.BASIC.getOrder() && CalculatorSettings.basicPanel(mParent.getContext())) {
            ((ViewGroup) container).addView(mSimplePage);
            v = mSimplePage;
        }
        else if(position == Panel.ADVANCED.getOrder() && CalculatorSettings.advancedPanel(mParent.getContext())) {
            ((ViewGroup) container).addView(mAdvancedPage);
            v = mAdvancedPage;
        }
        else if(position == Panel.HEX.getOrder() && CalculatorSettings.hexPanel(mParent.getContext())) {
            ((ViewGroup) container).addView(mHexPage);
            v = mHexPage;
        }
        else if(position == Panel.MATRIX.getOrder() && CalculatorSettings.matrixPanel(mParent.getContext())) {
            ((ViewGroup) container).addView(mMatrixPage);
            return mMatrixPage;
        }

        return v;
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
}
