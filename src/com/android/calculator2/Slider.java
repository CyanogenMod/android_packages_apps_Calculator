package com.android.calculator2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

@SuppressLint("NewApi")
public class Slider extends LinearLayout implements OnClickListener, OnTouchListener, AnimationListener {
    private boolean minimizeOnCreate = true;

    private OnSlideListener slideListener;
    private ImageButton slider;
    private LinearLayout body;
    private boolean sliderOpen;
    private int distance;
    private int offset;
    private int height;
    private int multiplier = 1;
    private int barHeight = 62;
    private RelativeLayout.LayoutParams params;
    public Slider(Context context) {
        super(context);
        setupView(context, null);
    }

    public Slider(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupView(context, attrs);
    }

    public Slider(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupView(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if(minimizeOnCreate) {
            height = getHeight();
            params = new RelativeLayout.LayoutParams(getWidth(), getHeight());
            body.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getHeight()-barHeight));
            minimizeSlider();
            minimizeOnCreate = !minimizeOnCreate;
        }
    }

    private void setupView(Context context, AttributeSet attrs) {
        setOrientation(LinearLayout.VERTICAL);
        setScrollContainer(false);
        setHorizontalScrollBarEnabled(false);
        slider = new ImageButton(context);
        if(attrs != null) {
            int[] attrsArray = new int[] { android.R.attr.scrollbarThumbHorizontal };
            TypedArray ta = context.obtainStyledAttributes(attrs, attrsArray);
            Drawable background = ta.getDrawable(0);
            slider.setBackgroundDrawable(background);
        }
        slider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        slider.setOnTouchListener(this);
        body = new LinearLayout(context);
        body.setBackgroundDrawable(getBackground());
        body.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        body.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        setBackgroundResource(android.R.color.transparent);
        
        addView(slider);
        addView(body);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            offset = (int) event.getY();
            if(slideListener != null && isSliderOpen()) slideListener.onSlide(Direction.DOWN);
            else if(slideListener != null && !isSliderOpen()) slideListener.onSlide(Direction.UP);
            break;
        case MotionEvent.ACTION_UP:
            if(sliderOpen) {
                if(distance*multiplier < (height-barHeight)/6) {
                    animateSliderUp();
                }
                else{
                    animateSliderDown();
                }
            }
            else{
                if(distance*multiplier < 5*(height-barHeight)/6) {
                    animateSliderUp();
                }
                else{
                    animateSliderDown();
                }
            }
            break;
        case MotionEvent.ACTION_MOVE:
            distance += event.getY()-offset;
            if(distance*multiplier < 0) distance = 0;
            if(distance*multiplier > height - barHeight) distance = (height - barHeight)*multiplier;
            translate();
            break;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if(sliderOpen) {
            minimizeSlider();
        } else {
            maximizeSlider();
        }
    }

    private void translate() {
        if(android.os.Build.VERSION.SDK_INT < 11) {
            params.topMargin = distance;
            setLayoutParams(params);
        } else{
            setTranslationY(distance);
        }
    }

    private void minimizeSlider() {
        distance = (height - barHeight)*multiplier;
        translate();
        if(slideListener != null) slideListener.onSlide(Direction.DOWN);
        sliderOpen = false;
    }

    private void maximizeSlider() {
        distance = 0;
        translate();
        bringToFront();
        if(slideListener != null) slideListener.onSlide(Direction.UP);
        sliderOpen = true;
    }

    public void animateSliderUp() {
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setInterpolator(new DecelerateInterpolator());
        animationSet.setFillAfter(false);
        animationSet.setFillEnabled(true);

        TranslateAnimation r = new TranslateAnimation(0, 0, distance, 0); 
        r.setDuration(500);
        r.setFillAfter(false);
        animationSet.addAnimation(r);

        maximizeSlider();
        startAnimation(animationSet);
    }

    public void animateSliderDown() {
        if(android.os.Build.VERSION.SDK_INT < 11) {
            AnimationSet animationSet = new AnimationSet(true);
            animationSet.setAnimationListener(this);
            animationSet.setInterpolator(new DecelerateInterpolator());
            animationSet.setFillAfter(false);
            animationSet.setFillEnabled(true);

            TranslateAnimation r = new TranslateAnimation(0, 0, distance, (height-barHeight)*multiplier); 
            r.setDuration(500);
            r.setFillAfter(false);
            animationSet.addAnimation(r);

            maximizeSlider();
            startAnimation(animationSet);
        } 
        else{
            AnimationSet animationSet = new AnimationSet(true);
            animationSet.setInterpolator(new DecelerateInterpolator());
            animationSet.setFillAfter(true);
            animationSet.setFillEnabled(true);

            TranslateAnimation r = new TranslateAnimation(0, 0, distance-((height-barHeight)*multiplier), 0); 
            r.setDuration(500);
            r.setFillAfter(true);
            animationSet.addAnimation(r);

            minimizeSlider();
            startAnimation(animationSet);
        }
    }

    public void addViewToBody(View v) {
        body.addView(v);
    }

    @Override
    public void onAnimationEnd(Animation a) {
        minimizeSlider();
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }

    @Override
    public void onAnimationStart(Animation animation) {
        super.onAnimationStart();
    }

    public boolean isSliderOpen() {
        return sliderOpen;
    }

    public void setOnSlideListener(OnSlideListener slideListener) {
        this.slideListener = slideListener;
    }

    public enum Direction{
        UP, DOWN
    }

    public static interface OnSlideListener{
        public void onSlide(Direction d);
    }

    @Override
    public void addView(View v) {
        if(v == slider || v == body) {
            super.addView(v);
        } else{
            body.addView(v);
        }
    }

    public void setSlideDirection(Direction d) {
        switch(d) {
        case UP:
            removeAllViews();
            addView(slider);
            addView(body);
            multiplier = 1;
            break;
        case DOWN:
            removeAllViews();
            addView(body);
            addView(slider);
            multiplier = -1;
            break;
        }
    }

    public void setClickToOpen(boolean clickToOpen) {
        if(clickToOpen) slider.setOnClickListener(this);
        else slider.setOnClickListener(null);
    }

    public void setBarHeight(int height) {
        barHeight = height;
        slider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, barHeight));
    }

    public void setBarBackground(Drawable background) {
        slider.setBackgroundDrawable(background);
    }

    public void setBarBackgroundResource(int background) {
        slider.setBackgroundResource(background);
    }

    public void setBodyBackground(Drawable background) {
        body.setBackgroundDrawable(background);
    }

    public void setBodyBackgroundResource(int background) {
        body.setBackgroundResource(background);
    }

    public View getBar() {
        return slider;
    }

    public LinearLayout getBody() {
        return body;
    }
}
