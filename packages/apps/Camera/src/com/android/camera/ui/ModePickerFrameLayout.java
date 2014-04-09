package com.android.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.android.camera.Log;
import com.android.camera.R;

public class ModePickerFrameLayout extends FrameLayout {
    private static final String TAG = "ModePickerFrameLayout";
    private static final boolean LOG = Log.LOGV;
    
    public ModePickerFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        View background = findViewById(R.id.mode_picker_background);
        ModePickerScrollable scroller = (ModePickerScrollable)findViewById(R.id.mode_picker_scroller);
        if (background != null && scroller != null) {
            scroller.setBackgroundView(background);
        }
    }
}
