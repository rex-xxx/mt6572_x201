package com.android.camera.ui;

import android.content.Context;
import android.util.AttributeSet;

import com.android.camera.Camera;
import com.android.camera.R;
import com.android.camera.Util;

public class SelfRotateImageView extends RotateImageView
        implements Camera.OnOrientationListener {

    private int mOrientation;

    public SelfRotateImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        registerOrientationListener(context);
    }

    public SelfRotateImageView(Context context) {
        this(context, null);
        registerOrientationListener(context);
    }

    private void registerOrientationListener(Context context) {
        if (context instanceof Camera) {
            Camera camera = (Camera)context;
            camera.addOnOrientationListener(this);
        }
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (mOrientation != orientation) {
            mOrientation = orientation;
            Util.setOrientation(this, mOrientation, true);
        }
    }

}
