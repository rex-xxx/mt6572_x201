package com.android.settings.applications;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ScrollView;

/*** M: add to fix the issue : when scroll the running view , it will popup
   @ author:mtk54093
   @ date : 2012-7-26
*/

public class CustomizedScrollView extends ScrollView {
    private final int mOverscrollDistance;    
    /**
     * @param context Context
     * @param attrs AttributeSet
     */
    public CustomizedScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final ViewConfiguration configuration = ViewConfiguration.get(mContext);
        mOverscrollDistance = configuration.getScaledOverscrollDistance();
    }
    
    @Override
    public void scrollTo(int x, int y) {
        // we rely on the fact the View.scrollBy calls scrollTo.
        int x1 = x;
        int y1 = y;
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            final int range = getScrollRange();
            final int overscrollMode = getOverScrollMode();
            final boolean canOverscroll = overscrollMode == OVER_SCROLL_ALWAYS ||
                    (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);
            if (!canOverscroll || mOverscrollDistance <= 0) {
                x1 = clamp(x1, getWidth() - mPaddingRight - mPaddingLeft, child.getWidth());
                y1 = clamp(y1, getHeight() - mPaddingBottom - mPaddingTop, child.getHeight());
            }            
            if (x1 != mScrollX || y1 != mScrollY) {
                super.scrollTo(x1, y1);
            }
        }
    }
    // get the scroll range
    private int getScrollRange() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            scrollRange = Math.max(0,
                    child.getHeight() - (getHeight() - mPaddingBottom - mPaddingTop));
        }
        return scrollRange;
    }
    
    // get the clamp
    private int clamp(int n, int my, int child) {
        if (my >= child || n < 0) {
            return 0;
        }
        if ((my + n) > child) {
            return child - my;
        }
        return n;
    }
}
