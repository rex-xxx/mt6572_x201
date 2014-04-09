package com.mediatek.camera.ext;

public class CameraExtension implements ICameraExtension {
    private IFeatureExtension mFeatureExtension;

    public IFeatureExtension getFeatureExtension() {
        if (mFeatureExtension == null) {
            mFeatureExtension = new FeatureExtension();
        }
        return mFeatureExtension;
    }
}
