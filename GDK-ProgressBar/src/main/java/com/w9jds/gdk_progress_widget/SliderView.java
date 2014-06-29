package com.w9jds.gdk_progress_widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class SliderView extends FrameLayout
{
    private float animatedCount = 0.0F;
    private int count = 0;
    private ObjectAnimator countAnimator;

    private Runnable hideSliderRunnable = new Runnable()
    {
        public void run()
        {
            SliderView.this.hideSlider(true);
        }
    };

    private final ImageView indeterminateSlider;
    private float index = 0.0F;
    private ViewPropertyAnimator progressAnimator;
    private float slideableScale = 1.0F;
    private final View slider;
    private boolean sliderShowing = true;
    private boolean sliderWasShowing = false;

    public SliderView(Context paramContext)
    {
        this(paramContext, null);
    }

    public SliderView(Context paramContext, AttributeSet paramAttributeSet)
    {
        this(paramContext, paramAttributeSet, 0);
    }

    public SliderView(Context paramContext, AttributeSet paramAttributeSet, int paramInt)
    {
        super(paramContext, paramAttributeSet, paramInt);
        LayoutInflater.from(getContext()).inflate(R.layout.slider, this);
        this.slider = findViewById(R.id.slider_control);
        this.indeterminateSlider = ((ImageView)findViewById(R.id.indeterminate_slider));
        hideSlider(false);
        hideIndeterminateSlider(false);
    }

    private void animateCountTo(float paramFloat)
    {
        if ((this.countAnimator != null) && (this.countAnimator.isRunning()))
            this.countAnimator.cancel();

        float[] arrayOfFloat = new float[2];
        arrayOfFloat[0] = this.animatedCount;
        arrayOfFloat[1] = paramFloat;
        this.countAnimator = ObjectAnimator.ofFloat(this, "animatedCount", arrayOfFloat);
        this.countAnimator.setDuration(300L);
        this.countAnimator.start();
    }

    private int getBaseSliderWidth()
    {
        return Math.max((int)(getResources().getDisplayMetrics().widthPixels / this.animatedCount), 40);
    }

    private void hideIndeterminateSlider(boolean paramBoolean)
    {
        int i = getResources().getDimensionPixelSize(R.dimen.slider_bar_height);

        if (paramBoolean)
        {
            this.indeterminateSlider.animate().translationY(i).setDuration(getResources().getInteger(R.integer.slider_in_out_animation_duration_ms));
            return;
        }

        this.indeterminateSlider.setTranslationY(i);
    }

    private void hideSlider(boolean paramBoolean)
    {
        if (!this.sliderShowing)
            return;

        int i = getResources().getDimensionPixelSize(R.dimen.slider_bar_height);
        this.slider.animate().cancel();

        if (paramBoolean)
            this.slider.animate().translationY(i).setDuration(getResources().getInteger(R.integer.slider_in_out_animation_duration_ms));

        while (true)
        {
            this.sliderShowing = false;
            this.slider.setTranslationY(i);
            return;
        }
    }

    private void hideSliderAfterTimeout()
    {
        removeCallbacks(this.hideSliderRunnable);
        postDelayed(this.hideSliderRunnable, 2000L);
    }

    private void setProportionalIndex(float paramFloat, int paramInt, boolean paramBoolean)
    {
        if (this.count < 2)
            hideSlider(true);

        while (true)
        {
            this.index = paramFloat;
            float f1 = 1.0F / this.slideableScale;
            float f2 = (0.5F + this.index - f1 / 2.0F) * (getResources().getDisplayMetrics().widthPixels / this.count);
            if (paramInt != 0)
            this.slider.animate().translationX(f2).setDuration(paramInt).setInterpolator(new AccelerateDecelerateInterpolator());

            while (paramBoolean)
            {
                showSlider(true);
                hideSliderAfterTimeout();
                this.slider.setTranslationX(f2);
            }
        }
    }

    private void showIndeterminateSlider(boolean paramBoolean)
    {
        if (paramBoolean)
        {
            this.indeterminateSlider.animate().translationY(0.0F).setDuration(getResources().getInteger(R.integer.slider_in_out_animation_duration_ms));
            return;
        }

        this.indeterminateSlider.setTranslationY(0.0F);
    }

    private void showSlider(boolean paramBoolean)
    {
        removeCallbacks(this.hideSliderRunnable);

        if (this.sliderShowing)
            return;

        this.slider.animate().cancel();

        if (paramBoolean)
            this.slider.animate().translationY(0.0F).setDuration(getResources().getInteger(R.integer.slider_in_out_animation_duration_ms));

        while (true)
        {
            this.sliderShowing = true;
            this.slider.setTranslationY(0.0F);
            return;
        }
    }

    private void updateSliderWidth(boolean paramBoolean)
    {
        if (this.count < 2)
        {
            hideSlider(true);
            return;
        }

        FrameLayout.LayoutParams localLayoutParams = (FrameLayout.LayoutParams)this.slider.getLayoutParams();
        localLayoutParams.width = ((int)(1.0F / this.slideableScale * getBaseSliderWidth()));
        localLayoutParams.leftMargin = 0;
        this.slider.setLayoutParams(localLayoutParams);

        if (paramBoolean)
            showSlider(true);

        setProportionalIndex(this.index, 0, paramBoolean);
    }

    public void dismissManualProgress()
    {
        hideSlider(true);
    }

    float getAnimatedCount()
    {
    return this.animatedCount;
    }

    void setAnimatedCount(float paramFloat)
    {
        setAnimatedCount(paramFloat, true);
    }

    void setAnimatedCount(float paramFloat, boolean paramBoolean)
    {
        this.animatedCount = paramFloat;
        updateSliderWidth(paramBoolean);
    }

    public void setCount(int paramInt)
    {
        setCount(paramInt, true);
    }

    public void setCount(int paramInt, boolean paramBoolean)
    {
        hideIndeterminateSlider(true);
        hideSlider(true);
        this.count = paramInt;
        this.index = Math.max(Math.min(this.index, paramInt - 1), 0.0F);

        if (paramBoolean)
        {
            animateCountTo(paramInt);
            return;
        }

        setAnimatedCount(paramInt, false);
    }

    public void setManualProgress(float paramFloat)
    {
        setManualProgress(paramFloat, false);
    }

    public void setManualProgress(float paramFloat, boolean paramBoolean)
    {
        hideIndeterminateSlider(true);
        showSlider(false);
        int i = getResources().getDisplayMetrics().widthPixels;
        FrameLayout.LayoutParams localLayoutParams = (FrameLayout.LayoutParams)this.slider.getLayoutParams();
        localLayoutParams.width = i;
        localLayoutParams.setMargins(-i, 0, 0, 0);
        this.slider.setLayoutParams(localLayoutParams);

        if (paramBoolean)
        {
            this.slider.animate().translationX(paramFloat * i);
            return;
        }

        this.slider.setTranslationX(paramFloat * i);
    }

    public void setProportionalIndex(float paramFloat)
    {
        setProportionalIndex(paramFloat, 0, true);
    }

    public void setProportionalIndex(float paramFloat, int paramInt)
    {
        setProportionalIndex(paramFloat, paramInt, true);
    }

    public void setScale(float paramFloat)
    {
        this.slideableScale = paramFloat;
        updateSliderWidth(true);
    }

    public void startIndeterminate()
    {
        int i = getResources().getDisplayMetrics().widthPixels;
        FrameLayout.LayoutParams localLayoutParams = (FrameLayout.LayoutParams)this.slider.getLayoutParams();
        localLayoutParams.width = i;
        localLayoutParams.setMargins(0, 0, 0, 0);
        this.slider.setLayoutParams(localLayoutParams);

        if (this.sliderShowing)
        {
            this.sliderWasShowing = true;
            hideSlider(true);
        }

        showIndeterminateSlider(true);
        ((AnimationDrawable)this.indeterminateSlider.getBackground()).start();
    }

    public void startProgress(long paramLong)
    {
        startProgress(paramLong, new AccelerateDecelerateInterpolator());
    }

    public void startProgress(long paramLong, Animator.AnimatorListener paramAnimatorListener)
    {
        startProgress(paramLong, new AccelerateDecelerateInterpolator(), paramAnimatorListener);
    }

    public void startProgress(long paramLong, TimeInterpolator paramTimeInterpolator)
    {
        startProgress(paramLong, paramTimeInterpolator, null);
    }

    public void startProgress(long paramLong, TimeInterpolator paramTimeInterpolator, Animator.AnimatorListener paramAnimatorListener)
    {
        hideIndeterminateSlider(true);
        this.slider.setTranslationX(0.0F);
        showSlider(false);
        int i = getResources().getDisplayMetrics().widthPixels;
        FrameLayout.LayoutParams localLayoutParams = (FrameLayout.LayoutParams)this.slider.getLayoutParams();
        localLayoutParams.width = i;
        localLayoutParams.setMargins(-i, 0, 0, 0);
        this.slider.setLayoutParams(localLayoutParams);
        this.progressAnimator = this.slider.animate().translationX(i).setDuration(paramLong).setInterpolator(paramTimeInterpolator).setListener(paramAnimatorListener);
    }

    public void stopIndeterminate()
    {
        if (this.sliderWasShowing)
            showSlider(true);

        ((AnimationDrawable)this.indeterminateSlider.getBackground()).stop();
        hideIndeterminateSlider(true);
    }

    public void stopProgress()
    {
        if (this.progressAnimator != null)
            this.progressAnimator.cancel();

        hideSlider(true);
    }
}