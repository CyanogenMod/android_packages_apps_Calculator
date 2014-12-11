package com.android.calculator2.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.calculator2.viewpager.PagerAdapter;
import com.android.calculator2.viewpager.VerticalViewPager;

public class CalculatorVerticalViewPager extends VerticalViewPager {

    private PagerAdapter mPagerAdapter = new PagerAdapter() {
        @Override
        public int getCount() {
            return getChildCount();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            return getChildAt(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            removeViewAt(position);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    };

    public CalculatorVerticalViewPager(Context context) {
        this(context, null);
    }

    public CalculatorVerticalViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAdapter(mPagerAdapter);
    }
}
