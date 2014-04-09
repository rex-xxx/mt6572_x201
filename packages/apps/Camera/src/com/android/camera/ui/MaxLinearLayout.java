package com.android.camera.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View.MeasureSpec;
import android.widget.LinearLayout;

import com.android.camera.Log;
import com.android.camera.R;

public class MaxLinearLayout extends LinearLayout {
    private static final String TAG = "MaxLinearLayout";
    private static final boolean LOG = Log.LOGV;
    
    private final int mMaxHeight;
    private final int mMaxWidth;
    
    public MaxLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.MaxLinearLayout, 0, 0);
        mMaxHeight = a.getDimensionPixelSize(R.styleable.MaxLinearLayout_maxHeight, Integer.MAX_VALUE);
        mMaxWidth = a.getDimensionPixelSize(R.styleable.MaxLinearLayout_maxWidth, Integer.MAX_VALUE);
        if (LOG) {
            Log.v(TAG, "MaxLinearLayout() mMaxHeight=" + mMaxHeight + ", mMaxWidth=" + mMaxWidth);
        }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(buildMeasureSpec(widthMeasureSpec, mMaxWidth),
                buildMeasureSpec(heightMeasureSpec, mMaxHeight));
    }
    
    private int buildMeasureSpec(int spec, int max) {
        int specMode = MeasureSpec.getMode(spec);
        int specSize = MeasureSpec.getSize(spec);
        specSize = max < specSize ? max : specSize;
        int result = MeasureSpec.makeMeasureSpec(specMode, specSize);
        return result;
    }
}
