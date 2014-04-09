package com.mediatek.gallery3d.video;

import android.content.Context;
import com.mediatek.drm.OmaDrmClient;
import com.mediatek.drm.OmaDrmStore;
import com.mediatek.drm.OmaDrmUtils;
import com.mediatek.drm.OmaDrmUtils.DrmProfile;
import com.mediatek.drm.OmaDrmUiUtils;
import com.mediatek.drm.OmaDrmUiUtils.DrmOperationListener;

import android.net.Uri;

import com.mediatek.gallery3d.ext.IMovieDrmExtension.IMovieDrmCallback;
import com.mediatek.gallery3d.ext.IMovieItem;
import com.mediatek.gallery3d.ext.MovieDrmExtension;
import com.mediatek.gallery3d.ext.MtkLog;

public class MovieDrmExtensionImpl extends MovieDrmExtension {
    private static final String TAG = "Gallery2/VideoPlayer/MovieDrmExtensionImpl";
    private static final boolean LOG = true;
    
    @Override
    public boolean handleDrmFile(final Context context, final IMovieItem item, final IMovieDrmCallback callback) {
        boolean handle = false;
        if (handleDrmFile(context, item.getUri(), new DrmOperationListener() {

            public void onOperated(int type) {
                if (LOG) {
                    MtkLog.v(TAG, "onOperated(" + type + ")");
                }
                switch (type) {
                case DrmOperationListener.CONTINUE:
                        consume(context, item.getUri(), OmaDrmStore.Action.PLAY);
                    if (callback != null) {
                        callback.onContinue();
                    }
                    break;
                case DrmOperationListener.STOP:
                    if (callback != null) {
                        callback.onStop();
                    }
                    break;
                default:
                    break;
                }
            }

        })) {
            handle = true;
        }
        return handle;
    }
    
    @Override
    public boolean canShare(final Context context, final IMovieItem item) {
        return canShare(context, item.getOriginalUri());
    }
    
    private static OmaDrmClient sDrmClient;
    private static OmaDrmClient ensureDrmClient(final Context context) {
        if (sDrmClient == null) {
            sDrmClient = new OmaDrmClient(context.getApplicationContext());
        }
        return sDrmClient;
    }
    // used for movie player to check for videos. Action type PLAY
    private static boolean handleDrmFile(final Context context, final Uri uri,
            final DrmOperationListener listener) {
        if (LOG) {
            MtkLog.v(TAG, "handleDrmFile(" + uri + ", " + listener + ")");
        }
        final OmaDrmClient client = ensureDrmClient(context);
        boolean result = false;
        final DrmProfile info = OmaDrmUtils.getDrmProfile(context, uri, client);
        if (info != null && info.isDrm() && info.getMethod() != OmaDrmStore.DrmMethod.METHOD_FL) {
            int rightsStatus = OmaDrmStore.RightsStatus.RIGHTS_INVALID;
            try {
                rightsStatus = client.checkRightsStatusForTap(uri, OmaDrmStore.Action.PLAY);
            } catch (final IllegalArgumentException e) {
                MtkLog.w(TAG, "handleDrmFile() : raise exception, we assume invalid rights");
            }
            switch (rightsStatus) {
                case OmaDrmStore.RightsStatus.RIGHTS_VALID:
                    OmaDrmUiUtils.showConsumeRights(client, context, listener);
                result = true;
                break;
                case OmaDrmStore.RightsStatus.RIGHTS_INVALID:
                    OmaDrmUiUtils.showRefreshLicense(client, context, uri, listener);
                result = true;
                break;
                case OmaDrmStore.RightsStatus.SECURE_TIMER_INVALID:
                    OmaDrmUiUtils.showSecureTimerInvalid(client, context, listener);
                result = true;
                break;
            default:
                break;
            }
        }
        if (LOG) {
            MtkLog.v(TAG, "handleDrmFile() return " + result);
        }
        return result;
    }
    
    private static int consume(final Context context, final Uri uri, final int action) {
        final OmaDrmClient client = ensureDrmClient(context);
        final int result = client.consumeRights(uri, action);
        if (LOG) {
            MtkLog.v(TAG, "consume(" + uri + ", action=" + action + ") return " + result);
        }
        return result;
    }
    
    private static boolean canShare(final Context context, final Uri uri) {
        if (LOG) {
            MtkLog.v(TAG, "canShare(" + uri + ")");
        }
        final OmaDrmClient client = ensureDrmClient(context);
        boolean share = false;
        boolean isDrm = false;
        try {
            isDrm = client.canHandle(uri, null);
        } catch (final IllegalArgumentException e) {
            MtkLog.w(TAG, "canShare() : raise exception, we assume it's not a OMA DRM file");
        }

        if (isDrm) {
            int rightsStatus = OmaDrmStore.RightsStatus.RIGHTS_INVALID;
            try {
                rightsStatus = client.checkRightsStatus(uri, OmaDrmStore.Action.TRANSFER);
            } catch (final IllegalArgumentException e) {
                MtkLog.w(TAG, "canShare() : raise exception, we assume it has no rights to be shared");
            }
            share = (OmaDrmStore.RightsStatus.RIGHTS_VALID == rightsStatus);
            if (LOG) {
                MtkLog.v(TAG, "canShare(" + uri + "), rightsStatus=" + rightsStatus);
            }
        } else {
            share = true;
        }
        if (LOG) {
            MtkLog.v(TAG, "canShare(" + uri + "), share=" + share);
        }
        return share;
    }
}
