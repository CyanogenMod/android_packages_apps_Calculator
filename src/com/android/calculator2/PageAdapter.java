/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.android.calculator2.view.CalculatorViewPager;

public class PageAdapter extends CalculatorPageAdapter {
    private final Context mContext;
    private final Logic mLogic;
    private final EventListener mListener;
    private final Graph mGraph;
    private final List<Page> mPages;

    public PageAdapter(Context context, EventListener listener, Graph graph, Logic logic) {
        mContext = context;
        mGraph = graph;
        mLogic = logic;
        mListener = listener;
        mPages = Page.getPages(mContext);
    }

    protected Context getContext() {
        return mContext;
    }

    @Override
    public int getCount() {
        return CalculatorSettings.useInfiniteScrolling(mContext)
                ? CalculatorViewPager.MAX_SIZE_CONSTANT
                * mPages.size()
                : mPages.size();
    }

    @Override
    public View getViewAt(int position) {
        position = position % mPages.size();
        View v = mPages.get(position).getView(mContext, mListener, mGraph, mLogic);
        if (v.getParent() != null) {
            ((ViewGroup) v.getParent()).removeView(v);
        }

        applyBannedResourcesByPage(mLogic, v, mLogic.getBaseModule().getMode());

        return v;
    }

    @Override
    public View getViewAtDontDetach(int position) {
        position = position % mPages.size();
        View v = mPages.get(position).getView(mContext, mListener, mGraph, mLogic);
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

        CalculatorIterator(PageAdapter adapter) {
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
        public void remove() {
            // Do nothing here
        }

        @Override
        public Iterator<View> iterator() {
            return this;
        }
    }
}
