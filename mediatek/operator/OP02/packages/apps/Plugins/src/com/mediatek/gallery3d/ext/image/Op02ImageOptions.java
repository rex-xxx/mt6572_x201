package com.mediatek.gallery3d.ext.image;

import com.mediatek.gallery3d.ext.IImageOptions;
import com.mediatek.gallery3d.ext.ImageOptions;

public class Op02ImageOptions extends ImageOptions implements IImageOptions {

    @Override
    public boolean shouldReturnTopWhenBack() {
        return true;
    }

    @Override
    public boolean cameraRollEnable(){
        return false;
    }
}
