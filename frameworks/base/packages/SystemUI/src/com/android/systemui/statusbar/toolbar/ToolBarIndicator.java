package com.android.systemui.statusbar.toolbar;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.systemui.R;

/**
 * M: Support "Notification toolbar".
 */
public class ToolBarIndicator extends LinearLayout implements ToolBarView.ScrollToScreenCallback {
    private Drawable mNormalDrawable;
    private Drawable mFocusedDrawable;
    
    private static final int VIEW_PADDING_HORIZONTAL = 4;
    private int mCount;
    private float mDensity;

    public ToolBarIndicator(Context context) {
        this(context, null);
    }

    public ToolBarIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mDensity = metrics.density;
        mNormalDrawable = this.getResources().getDrawable(R.drawable.toolbar_indicator);
        mFocusedDrawable = this.getResources().getDrawable(R.drawable.toolbar_indicator_focused);
    }

    public void setCount(int count) {
        this.mCount = count;
        generateIndicators();
        updateIndicator(0);
    }

    public void onScrollFinish(int currentIndex) {
        updateIndicator(currentIndex);
    }

    public void generateIndicators() {
        this.removeAllViews();
        for (int i = 0; i < this.mCount; i++) {
            ImageView imageView = new ImageView(mContext);
            int padddingHorizontal = (int) (mDensity * VIEW_PADDING_HORIZONTAL);
            imageView.setPadding(padddingHorizontal, 0, padddingHorizontal, 0);
            this.addView(imageView);
        }
    }

    public void updateIndicator(int currentIndex) {
        for (int i = 0; i < this.mCount; i++) {
            ImageView imageView = (ImageView) this.getChildAt(i);
            if (currentIndex == i) {
                imageView.setImageDrawable(mFocusedDrawable);
            } else {
                imageView.setImageDrawable(mNormalDrawable);
            }
        }
    }
}
