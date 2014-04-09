package com.android.music;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
  
import java.util.Locale;

import com.mediatek.drm.OmaDrmStore;
import com.mediatek.drm.OmaDrmClient;
import com.mediatek.drm.OmaDrmUiUtils;
/**
 * M: AudioPreviewStarter is an Activity which is used to check the DRM file
 * and decide launch the AudioPreview or not.
 */
public class AudioPreviewStarter extends Activity
        implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    
    private static final String TAG = "AudioPreStarter";
    private Intent mIntent;

    /**
        * M: onCreate to check the DRM file
        * and decide launch the AudioPreview or show DRM dialog.
        *
        * @param icicle If the activity is being re-initialized after
        *     previously being shut down then this Bundle contains the data it most
        *     recently supplied in 
        */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        MusicLogUtils.v(TAG, ">> onCreate");
        Uri uri = getIntent().getData();
        if (uri == null) {
            finish();
            return;
        }

        MusicLogUtils.v(TAG, "uri=" + uri);
        mIntent = new Intent(getIntent());
        mIntent.setClass(this, AudioPreview.class);
        if (!MusicFeatureOption.IS_SUPPORT_DRM) {
            MusicLogUtils.v(TAG, "DRM is off");
            startActivity(mIntent);
            finish();
            return;
        }
        processForDrm(uri);
        MusicLogUtils.v(TAG, "<< onCreate");
    }

    /**
     * M: handle the DRM dialog click event.
     * 
     * @param dialog
     *            The dialog that was dismissed will be passed into the method.
     * @param which
     *            The button that was clicked.
     */
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            /// M: continue to play
            MusicLogUtils.v(TAG, "onClick: BUTTON_POSITIVE");
            startActivity(mIntent);
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            /// M: do nothing but finish itself
            MusicLogUtils.v(TAG, "onClick: BUTTON_NEGATIVE");
        } else {
            MusicLogUtils.w(TAG, "undefined button on DRM consume dialog!");
        }
    }

    /**
     * M: finish itself when dialog dismiss.
     * 
     * @param dialog
     *            The dialog that was dismissed will be passed into the method.
     */
    public void onDismiss(DialogInterface dialog) {
        MusicLogUtils.v(TAG, "onDismiss");
        finish();
    }

    /**
     * M: the method is to do DRM process by uri.
     * 
     * @param uri
     *            the uri of the playing file
     */
    private void processForDrm(Uri uri) {
        final String schemeContent = "content";
        final String schemeFile = "file";
        final String hostMedia = "media";
        final String drmFileSuffix = ".dcf";
        final int isDrmIndex = 1;
        final int drmMethonIndex = 2;
        String scheme = uri.getScheme();
        String host = uri.getHost();
        MusicLogUtils.v(TAG, "scheme=" + scheme + ", host=" + host);
        if (schemeContent.equals(scheme) && hostMedia.equals(host)) {
            /// M: query DB for drm info
            ContentResolver resolver = getContentResolver();
            Cursor c = resolver.query(uri,
                    new String[] {MediaStore.Audio.Media._ID, 
                        MediaStore.Audio.Media.IS_DRM, MediaStore.Audio.Media.DRM_METHOD}, 
                    null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        /// M: cursor is valid
                        int isDrm = c.getInt(isDrmIndex);
                        MusicLogUtils.v(TAG, "isDrm=" + isDrm);
                        if (isDrm == 1) {
                            /// M: is a DRM file
                            checkDrmRightStatus(uri, c.getInt(drmMethonIndex));
                            return;
                        }
                    }
                } finally {
                    c.close();
                }
            }
        } else if (schemeFile.equals(scheme)) {
            /// M: a file opened from FileManager/ other app
            String path = uri.getPath();
            MusicLogUtils.v(TAG, "file path=" + path);
            if (path == null) {
                finish();
                return;
            }
            if (path.toLowerCase(Locale.ENGLISH).endsWith(drmFileSuffix)) {
                /// M: we consider this to be a DRM file
                checkDrmRightStatus(uri, -1);
                return;
            }
        }
        startActivity(mIntent);
        finish();
    }

    /**
     * M: the method is to check the drm right of the playing file.
     * 
     * @param uri
     *            the uri of the playing file
     * @param drmMethod
     *            the drm method of the playing file, it will retrive by drm client if the value is -1
     */
    private void checkDrmRightStatus(Uri uri, int drmMethod) {
        OmaDrmClient drmClient = new OmaDrmClient(this);
        int rightsStatus = -1;
        int method = drmMethod;
        if (method == -1) {
            method = drmClient.getMethod(uri);
        }
        MusicLogUtils.v(TAG, "drmMethod=" + method);
        if (method == OmaDrmStore.DrmMethod.METHOD_FL) {
            /// M: FL does not have constraints
            startActivity(mIntent);
            finish();
            return;
        }
        try {
            rightsStatus = drmClient.checkRightsStatusForTap(uri, OmaDrmStore.Action.PLAY);
        } catch (IllegalArgumentException e) {
            MusicLogUtils.e(TAG, "checkRightsStatusForTap throw IllegalArgumentException " + e);
        }
        MusicLogUtils.v(TAG, "checkDrmRightStatus: rightsStatus=" + rightsStatus);
        switch (rightsStatus) {
            case OmaDrmStore.RightsStatus.RIGHTS_VALID:
                OmaDrmUiUtils.showConsumeDialog(this, this, this);
                break;
            case OmaDrmStore.RightsStatus.RIGHTS_INVALID:
                OmaDrmUiUtils.showRefreshLicenseDialog(drmClient, this, uri, this);
                if (method == OmaDrmStore.DrmMethod.METHOD_CD) {
                    finish();
                }
                break;
            case OmaDrmStore.RightsStatus.SECURE_TIMER_INVALID:
                OmaDrmUiUtils.showSecureTimerInvalidDialog(this, null, this);
                break;
            default:
                break;
        }
    }
}
