package com.android.calculator2;

import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.calculator2.BaseModule.Mode;
import com.android.calculator2.Calculator.SmallPanel;
import com.android.calculator2.view.CalculatorViewPager;

import java.util.List;

public class SmallPageAdapter extends PagerAdapter {
    private final ViewGroup mHexPage;
    private final ViewGroup mFunctionPage;
    private final ViewGroup mAdvancedPage;

    private final CalculatorViewPager mParent;
    private final Logic mLogic;

    private int count = 0;

    public SmallPageAdapter(CalculatorViewPager parent, Logic logic) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        mHexPage = (ViewGroup)inflater.inflate(R.layout.hex_pad, parent, false);
        mFunctionPage = (ViewGroup)inflater.inflate(R.layout.function_pad, parent, false);
        mAdvancedPage = (ViewGroup)inflater.inflate(R.layout.advanced_pad, parent, false);

        mParent = parent;
        mLogic = logic;
        setOrder();

        switch(mLogic.mBaseModule.getMode()) {
        case BINARY:
            mHexPage.findViewById(R.id.bin).setBackgroundResource(R.color.pressed_color);
            applyBannedResources(Mode.BINARY);
            break;
        case DECIMAL:
            mHexPage.findViewById(R.id.dec).setBackgroundResource(R.color.pressed_color);
            applyBannedResources(Mode.DECIMAL);
            break;
        case HEXADECIMAL:
            mHexPage.findViewById(R.id.hex).setBackgroundResource(R.color.pressed_color);
            applyBannedResources(Mode.HEXADECIMAL);
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
            applyBannedResourcesByPage(mFunctionPage, mLogic.mBaseModule.getMode());
            return mFunctionPage;
        }
        else if(position == SmallPanel.ADVANCED.getOrder() && CalculatorSettings.advancedPanel(mParent.getContext())) {
            ((ViewGroup) container).addView(mAdvancedPage);
            applyBannedResourcesByPage(mAdvancedPage, mLogic.mBaseModule.getMode());
            return mAdvancedPage;
        }
        else if(position == SmallPanel.HEX.getOrder() && CalculatorSettings.hexPanel(mParent.getContext())) {
            ((ViewGroup) container).addView(mHexPage);
            applyBannedResourcesByPage(mHexPage, mLogic.mBaseModule.getMode());
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
        final String mode = getBaseMode(baseMode);
        // Enable
        for (String key : mLogic.mBaseModule.mBannedResources.keySet()) {
            if (mode.compareTo(key) != 0) {
                List<Integer> resources = mLogic.mBaseModule.mBannedResources.get(key);
                for (Integer resource : resources) {
                    final int resId = resource.intValue();
                    View v = page.findViewById(resId);
                    if (v != null) v.setEnabled(true);
                }
            }
        }
        // Disable
        List<Integer> resources = mLogic.mBaseModule.mBannedResources.get(mode);
        for (Integer resource : resources) {
            final int resId = resource.intValue();
            View v = page.findViewById(resId);
            if (v != null) v.setEnabled(false);
        }
    }

    private String getBaseMode(Mode baseMode) {
        if (baseMode.compareTo(Mode.BINARY) == 0) return "bin";
        if (baseMode.compareTo(Mode.HEXADECIMAL) == 0) return "hex";
        return "dec";
    }
}
