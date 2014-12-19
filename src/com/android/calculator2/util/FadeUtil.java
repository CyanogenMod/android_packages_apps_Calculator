package com.android.calculator2.util;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.view.View;

/**
 * Utility for fading a view in and out
 */
public class FadeUtil {

    public static final int DEFAULT_FADE_DURATION = 200;

    /**
     * Makes view visible and transitions alpha from 0 to 1.  Does nothing if view is
     * already visible.
     *
     * @param view
     * @param duration
     */
    public static void fadeIn(View view, int duration) {
        if (view.getVisibility() == View.VISIBLE) {
            return;
        }
        view.setAlpha(0);
        view.setVisibility(View.VISIBLE);
        ObjectAnimator anim = ObjectAnimator.ofFloat(view, "alpha", 0, 1);
        anim.setDuration(DEFAULT_FADE_DURATION);
        anim.start();
    }

    /**
     * Fade in with default duration
     *
     * @param view
     */
    public static void fadeIn(View view) {
        fadeIn(view, DEFAULT_FADE_DURATION);
    }

    /**
     * Transitions alpha from 1 to 0 and then sets visibility to gone
     *
     * @param view
     * @param duration
     */
    public static void fadeOut(final View view, int duration) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(view, "alpha", 1, 0);
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        anim.setDuration(DEFAULT_FADE_DURATION);
        anim.start();
    }

    /**
     * Fade out with default duration
     *
     * @param view
     */
    public static void fadeOut(View view) {
        fadeOut(view, DEFAULT_FADE_DURATION);
    }
}
