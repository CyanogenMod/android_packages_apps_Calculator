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
    private final List<Page> mPages;

    public SmallPageAdapter(Context context, Logic logic) {
        mContext = context;
        mGraph = null;
        mLogic = logic;
        mListener = null;
        mPages = Page.getSmallPages(mContext);
    }

    protected Context getContext() {
        return mContext;
    }

    @Override
    public int getCount() {
        return CalculatorSettings.useInfiniteScrolling(mContext) ? Integer.MAX_VALUE : mPages.size();
    }

    @Override
    public View getViewAt(int position) {
        position = position % mPages.size();
        View v = mPages.get(position).getView(mContext, mListener, mGraph, mLogic);
        if(v.getParent() != null) {
            ((ViewGroup) v.getParent()).removeView(v);
        }
        applyBannedResourcesByPage(mLogic, v, mLogic.getBaseModule().getMode());
        return v;
    }

    @Override
    public List<Page> getPages() {
        return mPages;
    }

    @Override
    public Iterable<View> getViewIterator() {
        return new CalculatorIterator(this);
    }

    private static class CalculatorIterator implements Iterator<View>, Iterable<View> {
        int mCurrentPosition = 0;
        List<Page> mPages;
        Context mContext;

        CalculatorIterator(SmallPageAdapter adapter) {
            super();
            mPages = adapter.mPages;
            mContext = adapter.getContext();
        }

        @Override
        public boolean hasNext() {
            return mCurrentPosition < mPages.size();
        }

        @Override
        public View next() {
            View v = mPages.get(mCurrentPosition).getView(mContext);
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
