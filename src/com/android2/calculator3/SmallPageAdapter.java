package com.android2.calculator3;

import java.util.List;

import android.content.Context;
import android.view.View;

public class SmallPageAdapter extends CalculatorPageAdapter {
    private final Graph mGraph;
    private final Logic mLogic;
    private final Context mContext;
    private final EventListener mListener;
    private final int mCount;

    public SmallPageAdapter(Context context, Logic logic) {
        mContext = context;
        mGraph = null;
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
        applyBannedResourcesByPage(mLogic, v, mLogic.mBaseModule.getMode());
        return v;
    }
}
