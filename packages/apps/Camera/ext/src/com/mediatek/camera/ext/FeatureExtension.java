package com.mediatek.camera.ext;

import android.media.CamcorderProfile;

import java.util.ArrayList;
import java.util.Iterator;

public class FeatureExtension implements IFeatureExtension {

    public boolean isDelayRestartPreview() {
        return false;
    }

    public void updateWBStrings(CharSequence[] entries) {
        // nothing happen here
    }

    public void updateSceneStrings(ArrayList<CharSequence> entries, ArrayList<CharSequence> entryValues) {
        //"normal" same as "auto", remove "normal" for common case.
        for (Iterator<CharSequence> iter = entryValues.iterator(); iter.hasNext();) {
            if ("normal".equals(String.valueOf(iter.next()))) {
                iter.remove();
                break;
            }
        }
    }

    public void checkMMSVideoCodec(int quality, CamcorderProfile profile) {
        
    }
    
    @Override
    public boolean isPrioritizePreviewSize() {
        return false;
    }
}
