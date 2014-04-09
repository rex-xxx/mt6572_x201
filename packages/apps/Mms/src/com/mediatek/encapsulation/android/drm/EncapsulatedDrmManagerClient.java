
package com.mediatek.encapsulation.android.drm;

import android.content.Context;
import com.mediatek.drm.OmaDrmClient;
import android.util.Log;

import com.mediatek.encapsulation.android.drm.EncapsulatedDrmStore;
import com.mediatek.encapsulation.EncapsulationConstant;

/**
 * The main programming interface for the DRM framework. An application must
 * instantiate this class to access DRM agents through the DRM framework.
 */
public class EncapsulatedDrmManagerClient extends OmaDrmClient {

    /**
     * Creates a <code>DrmManagerClient</code>.
     * 
     * @param context Context of the caller.
     */

    public EncapsulatedDrmManagerClient(Context context) {
        super(context);
    }

    protected void finalize() {
        super.finalize();
    }

    /**
     * Checks whether the given rights-protected content has valid rights for
     * the specified {@link DrmStore.Action}.
     * 
     * @param path Path to the rights-protected content.
     * @param action The {@link DrmStore.Action} to perform.
     * @return An <code>int</code> representing the
     *         {@link DrmStore.RightsStatus} of the content.
     */
    /** M: MTK ADD */
    public int checkRightsStatus(String path, int action) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return super.checkRightsStatus(path, action);
        } else {
            /**
             * M:can't complete this branch 
             */
            return EncapsulatedDrmStore.EncapsulatedRightsStatus.RIGHTS_VALID;
        }
    }

}
