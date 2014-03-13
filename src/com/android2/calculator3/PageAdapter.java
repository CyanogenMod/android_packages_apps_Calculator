package com.android2.calculator3;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.android2.calculator3.view.CalculatorViewPager;

public class PageAdapter extends CalculatorPageAdapter {
    private final Graph mGraph;
    private final Logic mLogic;
    private final Context mContext;
    private final EventListener mListener;
    private final int mCount;

    public PageAdapter(Context context, EventListener listener, Graph graph, Logic logic) {
        mContext = context;
        mGraph = graph;
        mLogic = logic;
        mListener = listener;
        mCount = Page.getPages(mContext).size();
    }

    protected Context getContext() {
        return mContext;
    }

    @Override
    public int getCount() {
        return CalculatorSettings.useInfiniteScrolling(mContext) ? CalculatorViewPager.MAX_SIZE_CONSTANT * mCount : mCount;
    }

    @Override
    public View getViewAt(int position) {
        position = position % mCount;
        List<Page> pages = Page.getPages(mContext);
        View v = pages.get(position).getView(mContext, mListener, mGraph, mLogic);
        if(v.getParent() != null) {
            ((ViewGroup) v.getParent()).removeView(v);
        }
        applyBannedResourcesByPage(mLogic, v, mLogic.mBaseModule.getMode());

        return v;
    }
}
