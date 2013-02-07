package com.android.calculator2;

import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.calculator2.Calculator.SmallPanel;
import com.android.calculator2.view.CalculatorViewPager;

public class SmallPageAdapter extends PagerAdapter {
    private View mHexPage;
    private View mFunctionPage;
    private View mAdvancedPage;
    private CalculatorViewPager mParent;

    private Logic mLogic;

    private int count = 0;

    public SmallPageAdapter(CalculatorViewPager parent, Logic logic) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final View hexPage = inflater.inflate(R.layout.hex_pad, parent, false);
        final View functionPage = inflater.inflate(R.layout.function_pad, parent, false);
        final View advancedPage = inflater.inflate(R.layout.advanced_pad, parent, false);

        mHexPage = hexPage;
        mFunctionPage = functionPage;
        mAdvancedPage = advancedPage;
        mParent = parent;
        mLogic = logic;
        setOrder();

        switch(mLogic.getMode()) {
        case BINARY:
            mHexPage.findViewById(R.id.bin).setBackgroundResource(R.color.pressed_color);
            break;
        case DECIMAL:
            mHexPage.findViewById(R.id.dec).setBackgroundResource(R.color.pressed_color);
            break;
        case HEXADECIMAL:
            mHexPage.findViewById(R.id.hex).setBackgroundResource(R.color.pressed_color);
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
}
