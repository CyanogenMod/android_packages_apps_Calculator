package com.android.calculator2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.calculator2.BaseModule.Mode;
import com.android.calculator2.Calculator.SmallPanel;
import com.android.calculator2.view.CalculatorViewPager;

public class SmallPageAdapter extends CalculatorPageAdapter {
    private final ViewGroup mHexPage;
    private final ViewGroup mFunctionPage;
    private final ViewGroup mAdvancedPage;
    private final CalculatorViewPager mParent;
    private final Logic mLogic;
    private int mCount = 0;

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
        return mCount;
    }

    @Override
    public View getViewAt(int position) {
        if(position == SmallPanel.FUNCTION.getOrder() && CalculatorSettings.functionPanel(mParent.getContext())) {
            return mFunctionPage;
        }
        else if(position == SmallPanel.ADVANCED.getOrder() && CalculatorSettings.advancedPanel(mParent.getContext())) {
            return mAdvancedPage;
        }
        else if(position == SmallPanel.HEX.getOrder() && CalculatorSettings.hexPanel(mParent.getContext())) {
            return mHexPage;
        }
        return null;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();

        setOrder();
    }

    private void setOrder() {
        mCount = 0;
        if(CalculatorSettings.hexPanel(mParent.getContext())) {
            SmallPanel.HEX.setOrder(mCount);
            mCount++;
        }
        if(CalculatorSettings.advancedPanel(mParent.getContext())) {
            SmallPanel.ADVANCED.setOrder(mCount);
            mCount++;
        }
        if(CalculatorSettings.functionPanel(mParent.getContext())) {
            SmallPanel.FUNCTION.setOrder(mCount);
            mCount++;
        }
    }

    private void applyBannedResources(Mode baseMode) {
        applyBannedResourcesByPage(mLogic, mFunctionPage, baseMode);
        applyBannedResourcesByPage(mLogic, mAdvancedPage, baseMode);
        applyBannedResourcesByPage(mLogic, mHexPage, baseMode);
    }
}
