package com.mediatek.camera.ext;

import android.media.CamcorderProfile;

import java.util.ArrayList;

public interface IFeatureExtension {
    boolean isDelayRestartPreview();
    void updateWBStrings(CharSequence[] entries);
    void updateSceneStrings(ArrayList<CharSequence> entries,
            ArrayList<CharSequence> entryValues);
    void checkMMSVideoCodec(int quality, CamcorderProfile profile);
    boolean isPrioritizePreviewSize();
}
