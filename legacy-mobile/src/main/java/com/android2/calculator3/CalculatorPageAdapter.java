package com.android2.calculator3;

import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.xlythe.math.Base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class CalculatorPageAdapter extends PagerAdapter {
    Map<Base, List<Integer>> mBannedResources;

    public CalculatorPageAdapter() {
        super();

        mBannedResources = new HashMap<Base, List<Integer>>(3);
        mBannedResources.put(Base.DECIMAL, Arrays.asList(R.id.A, R.id.B, R.id.C, R.id.D, R.id.E, R.id.F));
        mBannedResources.put(Base.BINARY, Arrays.asList(R.id.A, R.id.B, R.id.C, R.id.D, R.id.E, R.id.F,
                R.id.digit2, R.id.digit3, R.id.digit4, R.id.digit5, R.id.digit6, R.id.digit7, R.id.digit8, R.id.digit9));
        mBannedResources.put(Base.HEXADECIMAL, new ArrayList<Integer>());
    }

    @Override
    public void startUpdate(View container) {
    }

    @Override
    public Object instantiateItem(View container, int position) {
        View v = getViewAt(position);
        ((ViewGroup) container).addView(v);

        return v;
    }

    public abstract View getViewAt(int position);

    @Override
    public void destroyItem(View container, int position, Object object) {
        ((ViewGroup) container).removeView((View) object);
    }

    @Override
    public void finishUpdate(View container) {
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
    }

    protected void applyBannedResources(Base baseMode) {
        Iterable<View> iterator = getViewIterator();
        for(View child : iterator) {
            if(child != null) {
                applyBannedResourcesByPage(child, baseMode);
            }
        }
    }

    protected void applyBannedResourcesByPage(View page, Base baseMode) {
        // Enable
        for(Base key : mBannedResources.keySet()) {
            if(baseMode.compareTo(key) != 0) {
                List<Integer> resources = mBannedResources.get(key);
                for(Integer resource : resources) {
                    final int resId = resource.intValue();
                    View v = page.findViewById(resId);
                    if(v != null) v.setEnabled(true);
                }
            }
        }
        // Disable
        List<Integer> resources = mBannedResources.get(baseMode);
        for(Integer resource : resources) {
            final int resId = resource.intValue();
            View v = page.findViewById(resId);
            if(v != null) v.setEnabled(false);
        }
    }

    public abstract Iterable<View> getViewIterator();

    public abstract List<Page> getPages();
}
