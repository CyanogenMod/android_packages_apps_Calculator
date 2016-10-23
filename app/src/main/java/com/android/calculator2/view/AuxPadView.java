package com.android.calculator2.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.calculator2.R;
import com.android.calculator2.viewpager.PagerAdapter;
import com.android.calculator2.viewpager.VerticalViewPager;

/**
 * Container for a vertical view pager that adds indicator dots to show the currently
 * selected page.
 */
public class AuxPadView extends FrameLayout {

    private ViewGroup mIndicatorView;

    static class AuxPagerAdapter extends PagerAdapter {

        private VerticalViewPager mViewPager;

        public AuxPagerAdapter(VerticalViewPager viewPager) {
            mViewPager = viewPager;
        }

        @Override
        public int getCount() {
            return mViewPager.getChildCount();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            return mViewPager.getChildAt(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            mViewPager.removeViewAt(position);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

    class PageChangeListener implements VerticalViewPager.OnPageChangeListener {
        private int mSelectedIndex = 0;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            mIndicatorView.getChildAt(mSelectedIndex).setSelected(false);
            mIndicatorView.getChildAt(position).setSelected(true);
            mSelectedIndex = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    public AuxPadView(Context context) {
        super(context);
    }

    public AuxPadView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AuxPadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AuxPadView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        VerticalViewPager viewPager = (VerticalViewPager)findViewById(R.id.viewPager);
        viewPager.setAdapter(new AuxPagerAdapter(viewPager));
        viewPager.setOnPageChangeListener(new PageChangeListener());

        int margin = getContext().getResources()
                .getDimensionPixelSize(R.dimen.viewpager_indicator_margin_size);

        mIndicatorView = (ViewGroup)findViewById(R.id.pageIndicators);
        for (int i=0; i < viewPager.getChildCount(); ++i) {
            ImageView imageView = new ImageView(getContext());
            imageView.setImageResource(R.drawable.view_pager_indicator);
            imageView.setSelected(i == 0);
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(margin, margin, margin, margin);
            imageView.setLayoutParams(lp);
            mIndicatorView.addView(imageView);
        }
    }
}
