package com.android2.calculator3;

import android.animation.Animator;

/**
 * Created by Will on 4/12/2014.
 */
public abstract class AnimationFinishedListener implements Animator.AnimatorListener {
    @Override
    public void onAnimationCancel(Animator animation) {}

    @Override
    public void onAnimationRepeat(Animator animation) {}

    @Override
    public void onAnimationStart(Animator animation) {}
}
