package com.mediatek.gallery3d.mav;

import android.content.Context;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class FirstRunLayout extends FrameLayout {

    public FirstRunLayout(Context context) {
        super(context);
    }
    
    public FirstRunLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // consume all touch events and avoid transferring to other layouts
        return true;
    }

}
