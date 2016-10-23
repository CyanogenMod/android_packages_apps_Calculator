package com.xlythe.floatingview;

import android.animation.Animator;

public abstract class AnimationFinishedListener implements Animator.AnimatorListener {
    @Override
    public void onAnimationCancel(Animator animation) {}

    @Override
    public void onAnimationRepeat(Animator animation) {}

    @Override
    public void onAnimationStart(Animator animation) {}

    @Override
    public void onAnimationEnd(Animator animation) {
        onAnimationFinished();
    }

    public abstract void onAnimationFinished();
}
