
package com.android.internal.policy.impl.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Point;
import android.util.Log;
import android.view.View;

import java.util.HashMap;

// Class which represents the reorder hint animations. These animations show that an item is
// in a temporary state, and hint at where the item will return to.
public class UnReadHintAnimation {
    View child;
    Animator a;
    
    private static final int DURATION = 220;
    
    public UnReadHintAnimation(LockScreenNewEventView child) {
        child.setPivotY(child.getMeasuredHeight() * 0.5f);
        child.setPivotX(child.getMeasuredWidth() * 0.5f);
        this.child = child;
    }

    public void animate() {
        ValueAnimator va = ValueAnimator.ofObject(new DoubleEvaluator(), Double.valueOf(0), Double.valueOf(Math.PI) * 2);
        a = va;
        va.setRepeatCount(2);
        va.setDuration(DURATION);
        va.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                double r = ((Double) animation.getAnimatedValue()).floatValue();
                // Formula is -1/2*angle + x*angle, so that rotation is [-1/2angle - 1/2angle]
                float rotation = (float)(10 * Math.sin(r));
                child.setRotation(rotation);
            }
        });
        va.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                completeAnimationImmediately();
            }
        });
        va.start();
    }

    private void cancel() {
        if (a != null) {
            a.cancel();
        }
    }

    public void completeAnimationImmediately() {
        if (a != null) {
            a.cancel();
        }
        a = ObjectAnimator.ofFloat(child, "rotation", 0.0f);
        a.setDuration(DURATION);
        a.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
        a.start();
    }
    
    class DoubleEvaluator implements TypeEvaluator<Double> {
        public Double evaluate(float fraction, Double startValue, Double endValue) {
            return startValue + fraction * (endValue - startValue); 
        }
    }
}
