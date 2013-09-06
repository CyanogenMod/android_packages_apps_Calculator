package com.android2.calculator3;

import java.util.List;

import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android2.calculator3.BaseModule.Mode;
import com.android2.calculator3.Calculator.SmallPanel;
import com.android2.calculator3.view.CalculatorViewPager;

public class SmallPageAdapter extends PagerAdapter {
    private final ViewGroup mHexPage;
    private final ViewGroup mFunctionPage;
    private final ViewGroup mAdvancedPage;
    private final CalculatorViewPager mParent;

    private final Logic mLogic;

    private int count = 0;

    public SmallPageAdapter(CalculatorViewPager parent, Logic logic) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        mHexPage = (ViewGroup) inflater.inflate(R.layout.hex_pad, parent, false);
        mFunctionPage = (ViewGroup) inflater.inflate(R.layout.function_pad, parent, false);
        mAdvancedPage = (ViewGroup) inflater.inflate(R.layout.advanced_pad, parent, false);

        mParent = parent;
        mLogic = logic;
        setOrder();

        applyBannedResources(mLogic.mBaseModule.getMode());
        switch(mLogic.mBaseModule.getMode()) {
        case BINARY:
            mHexPage.findViewById(R.id.bin).setSelected(true);
            break;
        case DECIMAL:
            mHexPage.findViewById(R.id.dec).setSelected(true);
            break;
        case HEXADECIMAL:
            mHexPage.findViewById(R.id.hex).setSelected(true);
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
        if(position == SmallPanel.FUNCTION.getOrder() && CalculatorSettings.functionPanel(mParent.getContext())) {
            ((ViewGroup) container).addView(mFunctionPage);
            return mFunctionPage;
        }
        else if(position == SmallPanel.ADVANCED.getOrder() && CalculatorSettings.advancedPanel(mParent.getContext())) {
            ((ViewGroup) container).addView(mAdvancedPage);
            return mAdvancedPage;
        }
        else if(position == SmallPanel.HEX.getOrder() && CalculatorSettings.hexPanel(mParent.getContext())) {
            ((ViewGroup) container).addView(mHexPage);
            return mHexPage;
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
        if(CalculatorSettings.hexPanel(mParent.getContext())) {
            SmallPanel.HEX.setOrder(count);
            count++;
        }
        if(CalculatorSettings.advancedPanel(mParent.getContext())) {
            SmallPanel.ADVANCED.setOrder(count);
            count++;
        }
        if(CalculatorSettings.functionPanel(mParent.getContext())) {
            SmallPanel.FUNCTION.setOrder(count);
            count++;
        }
    }

    private void applyBannedResources(Mode baseMode) {
        applyBannedResourcesByPage(mFunctionPage, baseMode);
        applyBannedResourcesByPage(mAdvancedPage, baseMode);
        applyBannedResourcesByPage(mHexPage, baseMode);
    }

    private void applyBannedResourcesByPage(ViewGroup page, Mode baseMode) {
        // Enable
        for(Mode key : mLogic.mBaseModule.mBannedResources.keySet()) {
            if(baseMode.compareTo(key) != 0) {
                List<Integer> resources = mLogic.mBaseModule.mBannedResources.get(key);
                for(Integer resource : resources) {
                    final int resId = resource.intValue();
                    View v = page.findViewById(resId);
                    if(v != null) v.setEnabled(true);
                }
            }
        }
        // Disable
        List<Integer> resources = mLogic.mBaseModule.mBannedResources.get(baseMode);
        for(Integer resource : resources) {
            final int resId = resource.intValue();
            View v = page.findViewById(resId);
            if(v != null) v.setEnabled(false);
        }
    }
}
