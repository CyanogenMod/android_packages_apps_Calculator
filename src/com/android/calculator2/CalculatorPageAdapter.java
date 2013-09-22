package com.android.calculator2;

import java.util.List;

import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.android.calculator2.BaseModule.Mode;

public abstract class CalculatorPageAdapter extends PagerAdapter {
    @Override
    public void startUpdate(View container) {}

    public abstract View getViewAt(int position);

    @Override
    public Object instantiateItem(View container, int position) {
        View v = getViewAt(position);
        ((ViewGroup) container).addView(v);

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

    protected void applyBannedResourcesByPage(Logic logic, ViewGroup page, Mode baseMode) {
        // Enable
        for(Mode key : logic.mBaseModule.mBannedResources.keySet()) {
            if(baseMode.compareTo(key) != 0) {
                List<Integer> resources = logic.mBaseModule.mBannedResources.get(key);
                for(Integer resource : resources) {
                    final int resId = resource.intValue();
                    View v = page.findViewById(resId);
                    if(v != null) v.setEnabled(true);
                }
            }
        }
        // Disable
        List<Integer> resources = logic.mBaseModule.mBannedResources.get(baseMode);
        for(Integer resource : resources) {
            final int resId = resource.intValue();
            View v = page.findViewById(resId);
            if(v != null) v.setEnabled(false);
        }
    }
}
