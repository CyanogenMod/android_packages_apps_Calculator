package com.android2.calculator3;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public class LargePageAdapter extends CalculatorPageAdapter {
    private final Graph mGraph;
    private final Logic mLogic;
    private final Context mContext;
    private final EventListener mListener;
    private final int mCount;

    public LargePageAdapter(Context context, Graph graph, Logic logic) {
        mContext = context;
        mGraph = graph;
        mLogic = logic;
        mListener = null;
        mCount = Page.getPages(mContext).size();
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public View getViewAt(int position) {
        List<Page> pages = Page.getPages(mContext);
        View v = pages.get(position).getView(mContext, mListener, mGraph, mLogic);
        if(v.getParent() != null) {
            ((ViewGroup) v.getParent()).removeView(v);
        }
        applyBannedResourcesByPage(mLogic, v, mLogic.mBaseModule.getMode());

        return v;
    }
}
