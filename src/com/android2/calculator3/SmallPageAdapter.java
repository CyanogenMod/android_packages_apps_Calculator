package com.android2.calculator3;

import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

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
        mCount = Page.getSmallPages(mContext).size();
    }

    @Override
    public int getCount() {
        return CalculatorSettings.useInfiniteScrolling(mContext) ? Integer.MAX_VALUE : mCount;
    }

    @Override
    public View getViewAt(int position) {
        position = position % mCount;
        List<Page> pages = Page.getSmallPages(mContext);
        View v = pages.get(position).getView(mContext, mListener, mGraph, mLogic);
        if(v.getParent() != null) {
            ((ViewGroup) v.getParent()).removeView(v);
        }
        applyBannedResourcesByPage(mLogic, v, mLogic.mBaseModule.getMode());
        return v;
    }

    @Override
    public Iterable<View> getViewIterator(Context context) {
        return new CalculatorIterator(context);
    }

    private static class CalculatorIterator implements Iterator<View>, Iterable<View> {
        int mCurrentPosition = 0;
        List<Page> mPages;
        Context mContext;

        CalculatorIterator(Context context) {
            super();
            mPages = Page.getSmallPages(context);
            mContext = context;
        }

        @Override
        public boolean hasNext() {
            return mCurrentPosition < mPages.size();
        }

        @Override
        public View next() {
            View v = mPages.get(mCurrentPosition).getView(mContext, null, null, null);
            mCurrentPosition++;
            return v;
        }

        @Override
        public void remove() {}

        @Override
        public Iterator<View> iterator() {
            return this;
        }
    }
}
