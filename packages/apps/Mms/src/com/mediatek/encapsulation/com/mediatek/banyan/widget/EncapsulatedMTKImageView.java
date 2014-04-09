
package com.mediatek.encapsulation.com.mediatek.banyan.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.mediatek.banyan.widget.MTKImageView;
import com.mediatek.encapsulation.EncapsulationConstant;


public class EncapsulatedMTKImageView extends MTKImageView {

    /**
     * @param context The Context to attach
     */
    public EncapsulatedMTKImageView(Context context) {
        super(context);
    }

    /**
     * @param context The Context to attach
     * @param attrs The attribute set
     */
    public EncapsulatedMTKImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @param context The Context to attach
     * @param attrs The attribute set
     * @param defStyle The used style
     */
    public EncapsulatedMTKImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
