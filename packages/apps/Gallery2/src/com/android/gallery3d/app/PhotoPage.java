/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.app;

import java.util.LinkedList;

import android.annotation.TargetApi;
import android.app.ActionBar.OnMenuVisibilityListener;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateBeamUrisCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ShareActionProvider;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.android.camera.Camera;
import com.android.gallery3d.app.Gallery;
import com.android.gallery3d.R;
import com.android.gallery3d.app.PhotoDataAdapter.MavListener;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ComboAlbum;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.FilterDeleteSet;
import com.android.gallery3d.data.FilterSource;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaObject.PanoramaSupportCallback;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.MtpSource;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.data.SecureAlbum;
import com.android.gallery3d.data.SecureSource;
import com.android.gallery3d.data.SnailAlbum;
import com.android.gallery3d.data.SnailItem;
import com.android.gallery3d.data.SnailSource;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.DetailsHelper.CloseListener;
import com.android.gallery3d.ui.DetailsHelper.DetailsSource;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRoot.OnGLIdleListener;
import com.android.gallery3d.ui.GestureRecognizer.Listener;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.ImportCompleteListener;
import com.android.gallery3d.ui.MenuExecutor;
import com.android.gallery3d.ui.PhotoView;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.MediaSetUtils;

import android.bluetooth.BluetoothAdapter;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.LocalVideo;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;
import com.mediatek.drm.OmaDrmStore;
import com.mediatek.drm.OmaDrmClient;
import com.mediatek.drm.OmaDrmUiUtils;
import android.content.DialogInterface;
//import android.content.res.Configuration;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.app.ProgressDialog;
import android.os.Handler;
import android.widget.ShareActionProvider.OnShareTargetSelectedListener;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.gallery3d.drm.DrmHelper;
import com.mediatek.gallery3d.mpo.MpoHelper;
import com.mediatek.gallery3d.pq.PictureQualityTool;
import com.mediatek.gallery3d.stereo.StereoConvertor;
import com.mediatek.gallery3d.stereo.StereoHelper;
import com.mediatek.gallery3d.ui.MavSeekBar;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MediatekMMProfile;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MtkUtils;

public class PhotoPage extends ActivityState implements
        PhotoView.Listener, OrientationManager.Listener, AppBridge.Server,
        PhotoPageBottomControls.Delegate, GalleryActionBar.OnAlbumModeSelectedListener,
        PhotoView.StereoModeChangeListener, SensorEventListener {
    private static final String TAG = "Gallery2/PhotoPage";

    private static final int MSG_HIDE_BARS = 1;
    private static final int MSG_LOCK_ORIENTATION = 2;
    private static final int MSG_UNLOCK_ORIENTATION = 3;
    private static final int MSG_ON_FULL_SCREEN_CHANGED = 4;
    private static final int MSG_UPDATE_ACTION_BAR = 5;
    private static final int MSG_UNFREEZE_GLROOT = 6;
    private static final int MSG_WANT_BARS = 7;
    private static final int MSG_REFRESH_BOTTOM_CONTROLS = 8;
    private static final int MSG_ON_CAMERA_CENTER = 9;
    private static final int MSG_ON_PICTURE_CENTER = 10;
    private static final int MSG_REFRESH_IMAGE = 11;
    private static final int MSG_UPDATE_PHOTO_UI = 12;
    private static final int MSG_UPDATE_PROGRESS = 13;
    private static final int MSG_UPDATE_DEFERRED = 14;
    private static final int MSG_UPDATE_SHARE_URI = 15;
    private static final int MSG_UPDATE_PANORAMA_UI = 16;
    // added for stereo display feature
    private static final int MSG_UPDATE_MENU = 129;
    // added for mav playback feature                              
    private static final int MSG_UPDATE_MAV_PROGRESS = 130;    
    private static final int MSG_UPDATE_MAVSEEKBAR = 131;      
    private static final int MSG_RELOAD_MAVSEEKBAR = 132;      
    
    private static final int HIDE_BARS_TIMEOUT = 3500;
    private static final int UNFREEZE_GLROOT_TIMEOUT = 250;

    private static final int REQUEST_SLIDESHOW = 1;
    private static final int REQUEST_CROP = 2;
    private static final int REQUEST_CROP_PICASA = 3;
    private static final int REQUEST_EDIT = 4;
    private static final int REQUEST_PLAY_VIDEO = 5;
    private static final int REQUEST_TRIM = 6;
    private static final int REQUEST_PQ = 7;

    public static final String KEY_MEDIA_SET_PATH = "media-set-path";
    public static final String KEY_MEDIA_ITEM_PATH = "media-item-path";
    public static final String KEY_INDEX_HINT = "index-hint";
    public static final String KEY_OPEN_ANIMATION_RECT = "open-animation-rect";
    public static final String KEY_APP_BRIDGE = "app-bridge";
    public static final String KEY_TREAT_BACK_AS_UP = "treat-back-as-up";
    public static final String KEY_START_IN_FILMSTRIP = "start-in-filmstrip";
    public static final String KEY_RETURN_INDEX_HINT = "return-index-hint";
    public static final String KEY_SHOW_WHEN_LOCKED = "show_when_locked";
    public static final String KEY_IN_CAMERA_ROLL = "in_camera_roll";

    public static final String KEY_ALBUMPAGE_TRANSITION = "albumpage-transition";
    public static final int MSG_ALBUMPAGE_NONE = 0;
    public static final int MSG_ALBUMPAGE_STARTED = 1;
    public static final int MSG_ALBUMPAGE_RESUMED = 2;
    public static final int MSG_ALBUMPAGE_PICKED = 4;

    public static final String ACTION_NEXTGEN_EDIT = "action_nextgen_edit";

    //added to support Mediatek features
    private static final boolean mIsDrmSupported = 
                                          MediatekFeature.isDrmSupported();
    private static int mDrmMicroThumbDim;
    private int mMtkInclusion = 0;

    // added for stereo 3D switching
    private static final boolean mIsStereoDisplaySupported = 
            MediatekFeature.isStereoDisplaySupported();

    private static final int STEREO_MODE_2D = 0;
    private static final int STEREO_MODE_3D = 1;
    private int mStereoMode = STEREO_MODE_3D;
    private ProgressDialog mProgressDialog;
    private Future<?> mConvertIntentTask;
    private Future<?> mConvertEditTask;
    

    private GalleryApp mApplication;
    private SelectionManager mSelectionManager;

    private PhotoView mPhotoView;
    private PhotoPage.Model mModel;
    private DetailsHelper mDetailsHelper;
    private boolean mShowDetails;

    // mMediaSet could be null if there is no KEY_MEDIA_SET_PATH supplied.
    // E.g., viewing a photo in gmail attachment
    private FilterDeleteSet mMediaSet;

    // The mediaset used by camera launched from secure lock screen.
    private SecureAlbum mSecureAlbum;

    private int mCurrentIndex = 0;
    private Handler mHandler;
    private boolean mShowBars = true;
    private volatile boolean mActionBarAllowed = true;
    private GalleryActionBar mActionBar;
    private boolean mIsMenuVisible;
    private boolean mHaveImageEditor;
    private PhotoPageBottomControls mBottomControls;
    private PhotoPageProgressBar mProgressBar;
    private MediaItem mCurrentPhoto = null;
    private MenuExecutor mMenuExecutor;
    private boolean mIsActive;
    private boolean mShowSpinner;
    private String mSetPathString;
    // This is the original mSetPathString before adding the camera preview item.
    private String mOriginalSetPathString;
    private AppBridge mAppBridge;
    private SnailItem mScreenNailItem;
    private SnailAlbum mScreenNailSet;
    private OrientationManager mOrientationManager;
    private boolean mTreatBackAsUp;
    private boolean mStartInFilmstrip;
    private boolean mHasCameraScreennailOrPlaceholder = false;
    private boolean mRecenterCameraOnResume = false;//true;
    private boolean mCanSlideToPrePicture = true;

    // These are only valid after the panorama callback
    private boolean mIsPanorama;
    private boolean mIsPanorama360;

    private long mCameraSwitchCutoff = 0;
    private boolean mSkipUpdateCurrentPhoto = false;
    private static final long CAMERA_SWITCH_CUTOFF_THRESHOLD_MS = 300;

    private static final long DEFERRED_UPDATE_MS = 250;
    private boolean mDeferredUpdateWaiting = false;
    private long mDeferUpdateUntil = Long.MAX_VALUE;

    // The item that is deleted (but it can still be undeleted before commiting)
    private Path mDeletePath;
    private boolean mDeleteIsFocus;  // whether the deleted item was in focus

    private Uri[] mNfcPushUris = new Uri[1];
    
    private MavSeekBar mMavSeekBar;
    private boolean mIsMavSeekBarAllowed = true;

    private final MyMenuVisibilityListener mMenuVisibilityListener =
            new MyMenuVisibilityListener();
    private UpdateProgressListener mProgressListener;

    private final PanoramaSupportCallback mUpdatePanoramaMenuItemsCallback = new PanoramaSupportCallback() {
        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama,
                boolean isPanorama360) {
            if (mediaObject == mCurrentPhoto) {
                mHandler.obtainMessage(MSG_UPDATE_PANORAMA_UI, isPanorama360 ? 1 : 0, 0,
                        mediaObject).sendToTarget();
            }
        }
    };

    private final PanoramaSupportCallback mRefreshBottomControlsCallback = new PanoramaSupportCallback() {
        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama,
                boolean isPanorama360) {
            if (mediaObject == mCurrentPhoto) {
                mHandler.obtainMessage(MSG_REFRESH_BOTTOM_CONTROLS, isPanorama ? 1 : 0, isPanorama360 ? 1 : 0,
                        mediaObject).sendToTarget();
            }
        }
    };

    private final PanoramaSupportCallback mUpdateShareURICallback = new PanoramaSupportCallback() {
        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama,
                boolean isPanorama360) {
            if (mediaObject == mCurrentPhoto) {
                mHandler.obtainMessage(MSG_UPDATE_SHARE_URI, isPanorama360 ? 1 : 0, 0, mediaObject)
                        .sendToTarget();
            }
        }
    };

    public static interface Model extends PhotoView.Model {
        public void resume();
        public void pause();
        public boolean isEmpty();
        public void setCurrentPhoto(Path path, int indexHint);
        //added to support DRM rights consumption
        public void enterConsumeMode();
        public boolean enteredConsumeMode();
        // add for mav
        public int getTotalFrameCount();
        public void setImageBitmap(int index);
        public void updateMavStereoMode(boolean isMavStereoMode);
        public boolean isMavLoadingFinished();
        public void cancelCurrentMavDecodeTask();
    }

    private class MyMenuVisibilityListener implements OnMenuVisibilityListener {
        @Override
        public void onMenuVisibilityChanged(boolean isVisible) {
            mIsMenuVisible = isVisible;
            refreshHidingMessage();
        }
    }

    private class UpdateProgressListener implements StitchingChangeListener {

        @Override
        public void onStitchingResult(Uri uri) {
            sendUpdate(uri, MSG_REFRESH_IMAGE);
        }

        @Override
        public void onStitchingQueued(Uri uri) {
            sendUpdate(uri, MSG_UPDATE_PROGRESS);
        }

        @Override
        public void onStitchingProgress(Uri uri, final int progress) {
            sendUpdate(uri, MSG_UPDATE_PROGRESS);
        }

        private void sendUpdate(Uri uri, int message) {
            MediaObject currentPhoto = mCurrentPhoto;
            boolean isCurrentPhoto = currentPhoto instanceof LocalImage
                    && currentPhoto.getContentUri().equals(uri);
            if (isCurrentPhoto) {
                mHandler.sendEmptyMessage(message);
            }
        }
    };

    @Override
    protected int getBackgroundColorId() {
        return R.color.photo_background;
    }

    private final GLView mRootPane = new GLView() {
        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {
            mPhotoView.layout(0, 0, right - left, bottom - top);
            if (mShowDetails) {
                mDetailsHelper.layout(left, mActionBar.getHeight(), right, bottom);
            }
        }
    };

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        MediatekMMProfile.startProfilePhotoPageOnCreate();
        super.onCreate(data, restoreState);
        mActionBar = mActivity.getGalleryActionBar();
        mSelectionManager = new SelectionManager(mActivity, false);
        mMenuExecutor = new MenuExecutor(mActivity, mSelectionManager);

        mPhotoView = new PhotoView(mActivity);
        mPhotoView.setListener(this);
        if (mIsStereoDisplaySupported) {
            mPhotoView.setStereoModeChangeListener(this);
        }
        if (mIsDrmSupported || mIsStereoDisplaySupported) {
            mMtkInclusion = data.getInt(DrmHelper.DRM_INCLUSION, 
                                        DrmHelper.NO_DRM_INCLUSION);
            mDrmMicroThumbDim = DrmHelper.getDrmMicroThumbDim((Activity) mActivity);
        }
        mRootPane.addComponent(mPhotoView);
        mApplication = (GalleryApp) ((Activity) mActivity).getApplication();
        mOrientationManager = mActivity.getOrientationManager();
        ///M: roll back to JB4.2.1 @{
        mOrientationManager.addListener(this);
        mActivity.getGLRoot().setOrientationSource(mOrientationManager);

        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_HIDE_BARS: {
                        hideBars();
                        break;
                    }

                    case MSG_LOCK_ORIENTATION: {
                        mOrientationManager.lockOrientation();
                        break;
                    }

                    case MSG_UNLOCK_ORIENTATION: {
                        mOrientationManager.unlockOrientation();
                        break;
                    }

                    case MSG_REFRESH_BOTTOM_CONTROLS: {
                        if (mCurrentPhoto == message.obj && mBottomControls != null) {
                            mIsPanorama = message.arg1 == 1;
                            mIsPanorama360 = message.arg2 == 1;
                            mBottomControls.refresh();
                        }
                        break;
                    }
                    case MSG_ON_FULL_SCREEN_CHANGED: {
                        if(mAppBridge != null) {
                            mAppBridge.onFullScreenChanged(message.arg1 == 1, message.arg2);
                        }
                        break;
                    }
                    case MSG_UPDATE_ACTION_BAR: {
                        updateBars();
                        break;
                    }
                    case MSG_WANT_BARS: {
                        wantBars();
                        break;
                    }
                    case MSG_UNFREEZE_GLROOT: {
                        mActivity.getGLRoot().unfreeze();
                        break;
                    }
                    case MSG_UPDATE_DEFERRED: {
                        long nextUpdate = mDeferUpdateUntil - SystemClock.uptimeMillis();
                        if (nextUpdate <= 0) {
                            mDeferredUpdateWaiting = false;
                            updateUIForCurrentPhoto();
                        } else {
                            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DEFERRED, nextUpdate);
                        }
                        break;
                    }
                    case MSG_ON_CAMERA_CENTER: {
                        mSkipUpdateCurrentPhoto = false;
                        boolean stayedOnCamera = false;
                        if (!mPhotoView.getFilmMode()) {
                            stayedOnCamera = true;
                        } else if (SystemClock.uptimeMillis() < mCameraSwitchCutoff &&
                                mMediaSet.getMediaItemCount() > 1) {
                            mPhotoView.switchToImage(1);
                        } else {
                            if (mAppBridge != null) mPhotoView.setFilmMode(false);
                            stayedOnCamera = true;
                        }

                        if (stayedOnCamera) {
                            if (mAppBridge == null) {
                                launchCamera();
                                /* We got here by swiping from photo 1 to the
                                   placeholder, so make it be the thing that
                                   is in focus when the user presses back from
                                   the camera app */
                                mPhotoView.switchToImage(1);
                            } else {
                                updateBars();
                                updateCurrentPhoto(mModel.getMediaItem(0));
                            }
                        }
                        break;
                    }
                    case MSG_ON_PICTURE_CENTER: {
                        if (!mPhotoView.getFilmMode() && mCurrentPhoto != null
                                && (mCurrentPhoto.getSupportedOperations() & MediaObject.SUPPORT_ACTION) != 0) {
                            mPhotoView.setFilmMode(true);
                        }
                        break;
                    }
                    case MSG_REFRESH_IMAGE: {
                        final MediaItem photo = mCurrentPhoto;
                        mCurrentPhoto = null;
                        updateCurrentPhoto(photo);
                        break;
                    }
                    case MSG_UPDATE_PHOTO_UI: {
                        updateUIForCurrentPhoto();
                        break;
                    }
                    case MSG_UPDATE_PROGRESS: {
                        updateProgressBar();
                        break;
                    }
                    case MSG_UPDATE_SHARE_URI: {
                        if (mCurrentPhoto == message.obj) {
                            boolean isPanorama360 = message.arg1 != 0;
                            Uri contentUri = mCurrentPhoto.getContentUri();
                            Intent panoramaIntent = null;
                            if (isPanorama360) {
                                panoramaIntent = createSharePanoramaIntent(contentUri);
                            }
                            Intent shareIntent = createShareIntent(mCurrentPhoto);

                            mActionBar.setShareIntents(panoramaIntent, shareIntent);
                            setNfcBeamPushUri(contentUri);
                        }
                        break;
                    }
                    case MSG_UPDATE_PANORAMA_UI: {
                        if (mCurrentPhoto == message.obj) {
                            boolean isPanorama360 = message.arg1 != 0;
                            updatePanoramaUI(isPanorama360);
                        }
                        break;
                    }
                    case MSG_UPDATE_MENU: {
                        //added for stereo display feature;
                        if (mIsStereoDisplaySupported) {
                            Log.v(TAG,"handleMessage:update menu operations()");
                            updateMenuOperations();
                            break;
                        }
                    }
                    case MSG_UPDATE_MAV_PROGRESS:
                        // added for mav playback feature
                        mMavSeekBar.setProgress(message.arg1);
                        break;
                    case MSG_UPDATE_MAVSEEKBAR:
                        updateMavSeekBar();
                        break;
                    case MSG_RELOAD_MAVSEEKBAR:
                        reloadMavSeekBar();
                        break;
                    default: throw new AssertionError(message.what);
                }
            }
        };

        mSetPathString = data.getString(KEY_MEDIA_SET_PATH);
        mOriginalSetPathString = mSetPathString;
        MtkLog.i(TAG, "onCreate: original set path=" + mOriginalSetPathString);
        setupNfcBeamPush();
        String itemPathString = data.getString(KEY_MEDIA_ITEM_PATH);
        Path itemPath = null; 
        if (mIsDrmSupported || mIsStereoDisplaySupported) {
            itemPath = itemPathString != null ?
                    Path.fromString(data.getString(KEY_MEDIA_ITEM_PATH), mMtkInclusion) :
                        null;
        } else {
            itemPath = itemPathString != null ?
                    Path.fromString(data.getString(KEY_MEDIA_ITEM_PATH)) :
                        null;
        }
        mTreatBackAsUp = data.getBoolean(KEY_TREAT_BACK_AS_UP, false);
        mStartInFilmstrip = data.getBoolean(KEY_START_IN_FILMSTRIP, false);
        boolean inCameraRoll = data.getBoolean(KEY_IN_CAMERA_ROLL, false);
        mCurrentIndex = data.getInt(KEY_INDEX_HINT, 0);
        
        // M: added for mav feature
        // the mMavSeekBar will be used by PhotoDataAdapter and
        // SinglePhotoDataAdapter,
        // so place it there
        mMavSeekBar = (MavSeekBar) ((Activity) mActivity)
                .findViewById(R.id.seekbar_mav);
        reloadMavSeekBar();
        if(mMavSeekBar != null) {
            mMavSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
    
                public void onStopTrackingTouch(SeekBar seekBar) {
                    MtkLog.d("mavseekbar", "onStopTrackingTouch");
                }
    
                public void onStartTrackingTouch(SeekBar seekBar) {
                    MtkLog.d("mavseekbar", "onStartTrackingTouch");
                }
    
                public void onProgressChanged(SeekBar seekBar, int progress,
                        boolean fromUser) {
                    // if the state of MavSeekBar is sliding mode, and the change  
                    // event is from user, update frame when change progress
                    if (mMavSeekBar.getState() == MavSeekBar.STATE_SLIDING && fromUser) {
                        MtkLog.d(TAG, "show frame, [NO. " + progress + "]");
                        mModel.setImageBitmap(progress);
                    }
                }
            });
        }
        // init gyro sensor for mav
        initGyroSensor();
        
        if (mSetPathString != null) {
            mShowSpinner = true;
            mAppBridge = (AppBridge) data.getParcelable(KEY_APP_BRIDGE);
            if (mAppBridge != null) {
                MtkLog.d(TAG, "onCreate: app bridge not null, is from camera!");
                mShowBars = false;
                mHasCameraScreennailOrPlaceholder = true;
                mAppBridge.setServer(this);

                mOrientationManager.lockOrientation();

                // Get the ScreenNail from AppBridge and register it.
                int id = SnailSource.newId();
                Path screenNailSetPath = SnailSource.getSetPath(id);
                Path screenNailItemPath = SnailSource.getItemPath(id);
                mScreenNailSet = (SnailAlbum) mActivity.getDataManager()
                        .getMediaObject(screenNailSetPath);
                mScreenNailItem = (SnailItem) mActivity.getDataManager()
                        .getMediaObject(screenNailItemPath);
                mScreenNailItem.setScreenNail(mAppBridge.attachScreenNail());

                if (data.getBoolean(KEY_SHOW_WHEN_LOCKED, false)) {
                    // Set the flag to be on top of the lock screen.
                    mFlags |= FLAG_SHOW_WHEN_LOCKED;
                }

                // Don't display "empty album" action item for capture intents.
                if (!mSetPathString.equals("/local/all/0")) {
                    // Check if the path is a secure album.
                    if (SecureSource.isSecurePath(mSetPathString)) {
                        mSecureAlbum = (SecureAlbum) mActivity.getDataManager()
                                .getMediaSet(mSetPathString);
                        mShowSpinner = false;
                    }
                    mSetPathString = "/filter/empty/{"+mSetPathString+"}";
                }

                // Combine the original MediaSet with the one for ScreenNail
                // from AppBridge.
                mSetPathString = "/combo/item/{" + screenNailSetPath +
                        "," + mSetPathString + "}";

                // Start from the screen nail.
                itemPath = screenNailItemPath;
            } else if (inCameraRoll && GalleryUtils.isCameraAvailable(mActivity)) {
                mSetPathString = "/combo/item/{" + FilterSource.FILTER_CAMERA_SHORTCUT +
                        "," + mSetPathString + "}";
                mCurrentIndex++;
                mHasCameraScreennailOrPlaceholder = true;
            }

            MediaSet originalSet;
            if (mIsDrmSupported || mIsStereoDisplaySupported) {
                originalSet = mActivity.getDataManager()
                        .getMediaSet(mSetPathString, mMtkInclusion);
            } else {
                originalSet = mActivity.getDataManager()
                        .getMediaSet(mSetPathString);
            }
            if (mHasCameraScreennailOrPlaceholder && originalSet instanceof ComboAlbum) {
                // Use the name of the camera album rather than the default
                // ComboAlbum behavior
                ((ComboAlbum) originalSet).useNameOfChild(1);
            }
            mSelectionManager.setSourceMediaSet(originalSet);
            mSetPathString = "/filter/delete/{" + mSetPathString + "}";

            if (mIsDrmSupported || mIsStereoDisplaySupported) {
                mMediaSet = (FilterDeleteSet) mActivity.getDataManager()
                        .getMediaSet(mSetPathString, mMtkInclusion);
            } else {
                mMediaSet = (FilterDeleteSet) mActivity.getDataManager()
                        .getMediaSet(mSetPathString);
            }

            if (mMediaSet == null) {
                Log.w(TAG, "failed to restore " + mSetPathString);
            }
            if (itemPath == null) {
                int mediaItemCount = mMediaSet.getMediaItemCount();
                if (mediaItemCount > 0) {
                    if (mCurrentIndex >= mediaItemCount) mCurrentIndex = 0;
                    itemPath = mMediaSet.getMediaItem(mCurrentIndex, 1)
                        .get(0).getPath();
                } else {
                    // Bail out, PhotoPage can't load on an empty album
                    return;
                }
            }
            PhotoDataAdapter pda = new PhotoDataAdapter(
                    mActivity, mPhotoView, mMediaSet, itemPath, mCurrentIndex,
                    mAppBridge == null ? -1 : 0,
                    mAppBridge == null ? false : mAppBridge.isPanorama(),
                    mAppBridge == null ? false : mAppBridge.isStaticCamera());
            mModel = pda;
            mPhotoView.setModel(mModel);

            pda.setDataListener(new PhotoDataAdapter.DataListener() {

                @Override
                public void onPhotoChanged(int index, Path item) {
                    int oldIndex = mCurrentIndex;
                    mCurrentIndex = index;

                    if (mHasCameraScreennailOrPlaceholder) {
                        if (mCurrentIndex > 0) {
                            mSkipUpdateCurrentPhoto = false;
                        }

                        if (oldIndex == 0 && mCurrentIndex > 0) {
                            onActionBarAllowed(true);
                            mPhotoView.setFilmMode(false);
                        } else if (oldIndex == 2 && mCurrentIndex == 1) {
                            mCameraSwitchCutoff = SystemClock.uptimeMillis() +
                                    CAMERA_SWITCH_CUTOFF_THRESHOLD_MS;
                            mPhotoView.stopScrolling();
                        } else if (oldIndex >= 1 && mCurrentIndex == 0) {
                            mPhotoView.setWantPictureCenterCallbacks(true);
                            mSkipUpdateCurrentPhoto = true;
                        }
                    }
                    if (!mSkipUpdateCurrentPhoto) {
                        if (item != null) {
                            MediaItem photo = mModel.getMediaItem(0);
                            if (photo != null) updateCurrentPhoto(photo);
                        }
                        updateBars();
                    }
                    // Reset the timeout for the bars after a swipe
                    refreshHidingMessage();
                }

                @Override
                public void onLoadingFinished(boolean loadingFailed) {
                    // M: for performance auto test
                    mLoadingFinished = true;
                    
                    if (!mModel.isEmpty()) {
                        MediaItem photo = mModel.getMediaItem(0);
                        if (photo != null) updateCurrentPhoto(photo);
                        //added to start consume drm dialog, if needed
                        if (mIsDrmSupported) {
                            tryConsumeDrmRights(photo);
                        }
                    } else if (mIsActive) {
                        // We only want to finish the PhotoPage if there is no
                        // deletion that the user can undo.
                        if (mMediaSet.getNumberOfDeletions() == 0) {
                            mActivity.getStateManager().finishState(
                                    PhotoPage.this);
                        }
                    }
                    //M: it has camera screenNail, we will finish the photopage if
                    // delete all image.
                    if(mMediaSet.getMediaItemCount() <= 1 && mAppBridge == null &&
                            mHasCameraScreennailOrPlaceholder){
                        mActivity.getStateManager().finishState(PhotoPage.this); 
                    }
                }

                @Override
                public void onLoadingStarted() {
                    // M: for performance auto test
                    mLoadingFinished = false;
                }
            });
            
            if(mMavSeekBar != null) {
                pda.setMavListener(new PhotoDataAdapter.MavListener() {
    
                    public void setStatus(boolean isEnable) {
                        mMavSeekBar.setEnabled(isEnable);
                    }
    
                    public void setSeekBar(int max, int progress) {
                        mMavSeekBar.setMax(max);
                        mMavSeekBar.setProgress(progress);
                    }
    
                    public void setProgress(int progress) {
                        Message m = mHandler.obtainMessage(MSG_UPDATE_MAV_PROGRESS,
                                progress, 0);
                        m.sendToTarget();
                    }
                });
            }
        } else {
            // Get default media set by the URI
            MediaItem mediaItem = null;
            try {
                mediaItem = (MediaItem)
                        mActivity.getDataManager().getMediaObject(itemPath);
            } catch (Exception e) {
                Log.e(TAG, "Exception in getMediaObject(): ", e);
                Log.e(TAG, "quitting PhotoPage!");
                mActivity.getStateManager().finishState(this);
                return;
            }

            /// M: fix JE when mediaItem is deleted @{
            if (mediaItem == null) {
                Toast.makeText(((Activity) mActivity), R.string.no_such_item,
                        Toast.LENGTH_LONG).show();
                mActivity.getStateManager().finishState(this);
                return;
            }
            /// @}

            SinglePhotoDataAdapter spda = new SinglePhotoDataAdapter(mActivity, mPhotoView, mediaItem);
            mModel = spda;
            mPhotoView.setModel(mModel);
            if(mMavSeekBar != null) {
                spda.setMavListener(new PhotoDataAdapter.MavListener() {
    
                    public void setStatus(boolean isEnable) {
                        mMavSeekBar.setEnabled(isEnable);
                    }
    
                    public void setSeekBar(int max, int progress) {
                        mMavSeekBar.setMax(max);
                        mMavSeekBar.setProgress(progress);
                    }
    
                    public void setProgress(int progress) {
                        Message m = mHandler.obtainMessage(MSG_UPDATE_MAV_PROGRESS,
                                progress, 0);
                        m.sendToTarget();
                    }
                });
            }
            updateCurrentPhoto(mediaItem);
            mShowSpinner = false;
        }

        mPhotoView.setFilmMode(mStartInFilmstrip && mMediaSet.getMediaItemCount() > 1);
        RelativeLayout galleryRoot = (RelativeLayout) ((Activity) mActivity)
                .findViewById(mAppBridge != null ? R.id.content : R.id.gallery_root);
        if (galleryRoot != null) {
            if (mSecureAlbum == null) {
                mBottomControls = new PhotoPageBottomControls(this, mActivity, galleryRoot);
            }
            StitchingProgressManager progressManager = mApplication.getStitchingProgressManager();
            if (progressManager != null) {
                mProgressBar = new PhotoPageProgressBar(mActivity, galleryRoot);
                mProgressListener = new UpdateProgressListener();
                progressManager.addChangeListener(mProgressListener);
                if (mSecureAlbum != null) {
                    progressManager.addChangeListener(mSecureAlbum);
                }
            }
        }
        MediatekMMProfile.stopProfilePhotoPageOnCreate();
    }

    @Override
    public void onPictureCenter(boolean isCamera) {
        isCamera = isCamera || (mHasCameraScreennailOrPlaceholder && mAppBridge == null);
        mPhotoView.setWantPictureCenterCallbacks(false);
        mHandler.removeMessages(MSG_ON_CAMERA_CENTER);
        mHandler.removeMessages(MSG_ON_PICTURE_CENTER);
        mHandler.sendEmptyMessage(isCamera ? MSG_ON_CAMERA_CENTER : MSG_ON_PICTURE_CENTER);
    }

    @Override
    public boolean canDisplayBottomControls() {
        return mIsActive && !mPhotoView.canUndo();
    }

    @Override
    public boolean canDisplayBottomControl(int control) {
        if (mCurrentPhoto == null) {
            return false;
        }
        switch(control) {
            case R.id.photopage_bottom_control_edit:
                return mHaveImageEditor && mShowBars
                        && !mPhotoView.getFilmMode()
                        && (mCurrentPhoto.getSupportedOperations() & MediaItem.SUPPORT_EDIT) != 0
                        && mCurrentPhoto.getMediaType() == MediaObject.MEDIA_TYPE_IMAGE;
            case R.id.photopage_bottom_control_panorama:
                return mIsPanorama;
            case R.id.photopage_bottom_control_tiny_planet:
                return mHaveImageEditor && mShowBars
                        && mIsPanorama360 && !mPhotoView.getFilmMode();
            default:
                return false;
        }
    }

    @Override
    public void onBottomControlClicked(int control) {
        switch(control) {
            case R.id.photopage_bottom_control_edit:
                launchPhotoEditor();
                return;
            case R.id.photopage_bottom_control_panorama:
                mActivity.getPanoramaViewHelper()
                        .showPanorama(mCurrentPhoto.getContentUri());
                return;
            case R.id.photopage_bottom_control_tiny_planet:
                launchTinyPlanet();
                return;
            default:
                return;
        }
    }

    @TargetApi(ApiHelper.VERSION_CODES.JELLY_BEAN)
    private void setupNfcBeamPush() {
        if (!ApiHelper.HAS_SET_BEAM_PUSH_URIS) return;

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(mActivity);
        if (adapter != null) {
            adapter.setBeamPushUris(null, mActivity);
            /// M: mediatek nfc modification @{
            if (FeatureOption.MTK_BEAM_PLUS_SUPPORT) {
                adapter.setMtkBeamPushUrisCallback(new CreateBeamUrisCallback() {
                    @Override
                    public Uri[] createBeamUris(NfcEvent event) {
                        return mNfcPushUris;
                    }
                }, mActivity);
            } else {
                adapter.setBeamPushUrisCallback(new CreateBeamUrisCallback() {
                    @Override
                    public Uri[] createBeamUris(NfcEvent event) {
                        return mNfcPushUris;
                    }
                }, mActivity);
            }
//            adapter.setBeamPushUrisCallback(new CreateBeamUrisCallback() {
//                @Override
//                public Uri[] createBeamUris(NfcEvent event) {
//                    return mNfcPushUris;
//                }
//            }, mActivity);
            /// @}
        }
    }

    private void setNfcBeamPushUri(Uri uri) {
        mNfcPushUris[0] = uri;
    }

    private static Intent createShareIntent(MediaObject mediaObject) {
        int type = mediaObject.getMediaType();
        return new Intent(Intent.ACTION_SEND)
                .setType(MenuExecutor.getMimeType(type))
                .putExtra(Intent.EXTRA_STREAM, mediaObject.getContentUri())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    private static Intent createSharePanoramaIntent(Uri contentUri) {
        return new Intent(Intent.ACTION_SEND)
                .setType(GalleryUtils.MIME_TYPE_PANORAMA360)
                .putExtra(Intent.EXTRA_STREAM, contentUri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    private void overrideTransitionToEditor() {
        ((Activity) mActivity).overridePendingTransition(android.R.anim.slide_in_left,
                android.R.anim.fade_out);
    }

    private void launchTinyPlanet() {
        // Deep link into tiny planet
        MediaItem current = mModel.getMediaItem(0);
        Intent intent = new Intent(FilterShowActivity.TINY_PLANET_ACTION);
        intent.setClass(mActivity, FilterShowActivity.class);
        intent.setDataAndType(current.getContentUri(), current.getMimeType())
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(FilterShowActivity.LAUNCH_FULLSCREEN,
                mActivity.isFullscreen());
        mActivity.startActivityForResult(intent, REQUEST_EDIT);
        overrideTransitionToEditor();
    }

    private void launchCamera() {
        //M: to resolve problem that
        //user need press back key two times to exit camera @{
        //Intent intent = new Intent(mActivity, Camera.class)
        //    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //mActivity.startActivity(intent);
        GalleryUtils.startCameraActivity(mActivity);
        // }@
        mRecenterCameraOnResume = false;
        //M: it doesn't allowed slide to previous picture
        //after launch camera
        mCanSlideToPrePicture = false;
    }

    private void launchPhotoEditor() {
        MediaItem current = mModel.getMediaItem(0);
        if (current == null || (current.getSupportedOperations()
                & MediaObject.SUPPORT_EDIT) == 0) {
            return;
        }

        Intent intent = new Intent(ACTION_NEXTGEN_EDIT);

        intent.setDataAndType(current.getContentUri(), current.getMimeType())
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (mActivity.getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() == 0) {
            intent.setAction(Intent.ACTION_EDIT);
        }
        intent.putExtra(FilterShowActivity.LAUNCH_FULLSCREEN,
                mActivity.isFullscreen());
        ((Activity) mActivity).startActivityForResult(Intent.createChooser(intent, null),
                REQUEST_EDIT);
        overrideTransitionToEditor();
    }

    private void requestDeferredUpdate() {
        mDeferUpdateUntil = SystemClock.uptimeMillis() + DEFERRED_UPDATE_MS;
        if (!mDeferredUpdateWaiting) {
            mDeferredUpdateWaiting = true;
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DEFERRED, DEFERRED_UPDATE_MS);
        }
    }

    private void updateUIForCurrentPhoto() {
        if (mCurrentPhoto == null) return;

        // If by swiping or deletion the user ends up on an action item
        // and zoomed in, zoom out so that the context of the action is
        // more clear
        if ((mCurrentPhoto.getSupportedOperations() & MediaObject.SUPPORT_ACTION) != 0
                && !mPhotoView.getFilmMode()) {
            mPhotoView.setWantPictureCenterCallbacks(true);
        }

        updateMenuOperations();
        refreshBottomControlsWhenReady();
        if (mShowDetails) {
            mDetailsHelper.reloadDetails();
        }
        if ((mSecureAlbum == null)
                && (mCurrentPhoto.getSupportedOperations() & MediaItem.SUPPORT_SHARE) != 0) {
            mCurrentPhoto.getPanoramaSupport(mUpdateShareURICallback);
        }
        updateProgressBar();
        
        /// M: added for mav feature, show or hide MavSeekBar
        updateMavSeekBar();
        // enable or disable some gestures
        updateScaleGesture();
    }

    private void updateCurrentPhoto(MediaItem photo) {
        if (mCurrentPhoto == photo) return;
        mCurrentPhoto = photo;
        if (mIsStereoDisplaySupported) {
            int supportedOperations = mCurrentPhoto.getSupportedOperations();
            if ((supportedOperations & MediaObject.SUPPORT_STEREO_DISPLAY) != 0) {
                mPhotoView.allowStereoMode(true);
            } else {
                mPhotoView.allowStereoMode(false);
            }
        }
        if (mPhotoView.getFilmMode()) {
            requestDeferredUpdate();
        } else {
            updateUIForCurrentPhoto();
        }
    }

    private void updateProgressBar() {
        if (mProgressBar != null) {
            mProgressBar.hideProgress();
            StitchingProgressManager progressManager = mApplication.getStitchingProgressManager();
            if (progressManager != null && mCurrentPhoto instanceof LocalImage) {
                Integer progress = progressManager.getProgress(mCurrentPhoto.getContentUri());
                if (progress != null) {
                    mProgressBar.setProgress(progress);
                }
            }
        }
    }

    private void updateMenuOperations() {
        Menu menu = mActionBar.getMenu();

        // it could be null if onCreateActionBar has not been called yet
        if (menu == null) return;

        MenuItem item = menu.findItem(R.id.action_slideshow);
        if (item != null) {
            item.setVisible((mSecureAlbum == null) && canDoSlideShow());
        }
        if (mCurrentPhoto == null) return;

        int supportedOperations = mCurrentPhoto.getSupportedOperations();
        if (mSecureAlbum != null) {
            supportedOperations &= MediaObject.SUPPORT_DELETE;
        } else if (!mHaveImageEditor) {
            supportedOperations &= ~MediaObject.SUPPORT_EDIT;
        }
        
        // added for stereo 3D display
        if (MediaObject.MEDIA_TYPE_IMAGE == mCurrentPhoto.getMediaType()) {
            //for image, we can display switch mode menu item
            //if (mIsDisplay2dAs3dSupported) {
            //    //force display switch mode menu
            //    supportedOperations |= MediaObject.SUPPORT_STEREO_DISPLAY;
            //}
            if (true == MtkLog.SUPPORT_PQ) {
                supportedOperations |= MediaObject.SUPPORT_PQ;
            }
            if (mIsStereoDisplaySupported &&  
                (supportedOperations & MediaObject.SUPPORT_STEREO_DISPLAY) != 0) {
                supportedOperations |= (mStereoMode == STEREO_MODE_3D) ? 
                    MediaObject.SUPPORT_SWITCHTO_2D : MediaObject.SUPPORT_SWITCHTO_3D;
            }
        } else {
            //for video, we hide the switch mode menu item according to spec
            supportedOperations &= ~MediaObject.SUPPORT_STEREO_DISPLAY;
        }
        
        /// M: if file is not mp4, disable trim
        if(mCurrentPhoto.getMimeType() == null 
                || !mCurrentPhoto.getMimeType().toLowerCase().equals("video/mp4")){
            supportedOperations &= ~MediaObject.SUPPORT_TRIM;
        }
        MenuExecutor.updateMenuOperation(menu, supportedOperations);
        mCurrentPhoto.getPanoramaSupport(mUpdatePanoramaMenuItemsCallback);
    }

    private boolean canDoSlideShow() {
        if (mMediaSet == null || mCurrentPhoto == null) {
            return false;
        }
        /// M: slide show video thumbnails
        if (mCurrentPhoto.getMediaType() != MediaObject.MEDIA_TYPE_IMAGE
                && mCurrentPhoto.getMediaType() != MediaObject.MEDIA_TYPE_VIDEO) {
            return false;
        }
        if (MtpSource.isMtpPath(mOriginalSetPathString)) {
            return false;
        }
        return true;
    }

    //////////////////////////////////////////////////////////////////////////
    //  Action Bar show/hide management
    //////////////////////////////////////////////////////////////////////////

    private void showBars() {
        // M: for performance auto test
        if (mDisableBarChanges) {
            return;
        }
        
        if (mShowBars) return;
        mShowBars = true;
        mOrientationManager.unlockOrientation();
        mActionBar.show();
        mActivity.getGLRoot().setLightsOutMode(false);
        refreshHidingMessage();
        refreshBottomControlsWhenReady();
    }

    private void hideBars() {
        // M: for performance auto test
        if (mDisableBarChanges) {
            return;
        }
        
        if (!mShowBars) return;
        mShowBars = false;
        mActionBar.hide();
        mActivity.getGLRoot().setLightsOutMode(true);
        mHandler.removeMessages(MSG_HIDE_BARS);
        refreshBottomControlsWhenReady();
    }

    private void refreshHidingMessage() {
        mHandler.removeMessages(MSG_HIDE_BARS);
        if (!mIsMenuVisible && !mPhotoView.getFilmMode()) {
            mHandler.sendEmptyMessageDelayed(MSG_HIDE_BARS, HIDE_BARS_TIMEOUT);
        }
    }

    private boolean canShowBars() {
        // No bars if we are showing camera preview.
        if (mAppBridge != null && mCurrentIndex == 0
                && !mPhotoView.getFilmMode()) return false;

        // No bars if it's not allowed.
        if (!mActionBarAllowed) return false;

        return true;
    }

    private void wantBars() {
        if (canShowBars()) showBars();
    }

    private void toggleBars() {
        if (mShowBars) {
            hideBars();
        } else {
            if (canShowBars()) showBars();
        }
    }

    private void updateBars() {
        if (!canShowBars()) {
            hideBars();
        }
        updateActionBarTitle();
    }
    
    /// M: add for mav feature support, update MavSeekBar status
    private boolean canShowMavSeekBar() {
        // No bars if we are showing camera preview.
        if (mAppBridge != null && mCurrentIndex == 0) return false;
        // No bars if it's not allowed.
        if (!mIsMavSeekBarAllowed) return false;

        return true;
    }
    
    private void updateMavSeekBar() {
        if (mCurrentPhoto == null) {
            return;
        }
        if (mMavSeekBar == null) {
            return;
        }
        if (mCurrentPhoto.getSubType() != MediaObject.SUBTYPE_MPO_MAV || !canShowMavSeekBar()) {
            mMavSeekBar.setVisibility(View.INVISIBLE);
        } else {
            mMavSeekBar.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateScaleGesture() {
        if (mCurrentPhoto == null) {
            return;
        }
        if (mCurrentPhoto.getMediaType() == MediaObject.MEDIA_TYPE_VIDEO 
                || mCurrentPhoto.getSubType() == MediaObject.SUBTYPE_MPO_MAV) {
            mPhotoView.setScaleGestureEnabled(false);
        } else {
            mPhotoView.setScaleGestureEnabled(true);
        }
    } 

    @Override
    protected void onBackPressed() {
        if (mShowDetails) {
            hideDetails();
        } else if (mAppBridge == null || !switchWithCaptureAnimation(-1)) {
            // We are leaving this page. Set the result now.
            setResult();
            if (mStartInFilmstrip && !mPhotoView.getFilmMode()) {
                mPhotoView.setFilmMode(true);
            } else if (mTreatBackAsUp) {
                onUpPressed();
            } else {
                super.onBackPressed();
            }
        }
    }

    private void onUpPressed() {
        if ((mStartInFilmstrip || mAppBridge != null)
                && !mPhotoView.getFilmMode()) {
            mPhotoView.setFilmMode(true);
            return;
        }

        if (mActivity.getStateManager().getStateCount() > 1) {
            setResult();
            super.onBackPressed();
            return;
        }

        if (mOriginalSetPathString == null) return;

        if (mAppBridge == null) {
            // We're in view mode so set up the stacks on our own.
            Bundle data = new Bundle(getData());
            data.putString(AlbumPage.KEY_MEDIA_PATH, mOriginalSetPathString);
            data.putString(AlbumPage.KEY_PARENT_MEDIA_PATH,
                    mActivity.getDataManager().getTopSetPath(
                            DataManager.INCLUDE_ALL));
            mActivity.getStateManager().switchState(this, AlbumPage.class, data);
        } else {
            // Start the real gallery activity to view the camera roll.
            // M: to support multi-storage feature, we need to try parsing
            // bucket id from original set path instead of
            // directly using hard-coded bucket id;
            int bucketId = 0;
            if (mOriginalSetPathString != null) {
                // M: try to parse an bucket id from this string
                String[] segments = Path.split(mOriginalSetPathString);
                if (segments != null && segments.length > 0) {
                    try {
                        bucketId = Integer.parseInt(segments[segments.length - 1]);
                    } catch (NumberFormatException e) {
                        MtkLog.e(TAG, "onUpPressed: cannot parse a valid id from original set path(" 
                            + mOriginalSetPathString + ")");
                    }
                }
            }
            MtkLog.i(TAG, "onUpPressed: orig set path=" + mOriginalSetPathString 
                + ", parsed bucketId=" + bucketId);
            Uri uri = null;
            if (bucketId != 0) {
                uri = Uri.parse("content://media/external/file?bucketId="
                        + bucketId);
            } else {
                uri = Uri.parse("content://media/external/file?bucketId="
                        + MediaSetUtils.CAMERA_BUCKET_ID);
            }
            Intent intent = null;
            //M: return to the top of gallery when press back key
            intent = new Intent(mActivity.getAndroidContext(), Gallery.class);
            ((Activity) mActivity).startActivity(intent);
        }
    }

    private void setResult() {
        Intent result = null;
        result = new Intent();
        result.putExtra(KEY_RETURN_INDEX_HINT, mCurrentIndex);
        setStateResult(Activity.RESULT_OK, result);
    }

    //////////////////////////////////////////////////////////////////////////
    //  AppBridge.Server interface
    //////////////////////////////////////////////////////////////////////////

    @Override
    public void setCameraRelativeFrame(Rect frame) {
        mPhotoView.setCameraRelativeFrame(frame);
    }

    @Override
    public boolean switchWithCaptureAnimation(int offset) {
        Log.d(TAG, "switchWithCaptureAnimation: offset=" + offset + 
            ", orig set path=" + mOriginalSetPathString);
        ///M: from camera to gallery
        ///first box must be ready @{
        if(1 == offset && !mPhotoView.isFirstBoxReady()) {
            return false;
        }
        ///}@
        return mPhotoView.switchWithCaptureAnimation(offset);
    }

    @Override
    public void setSwipingEnabled(boolean enabled) {
        mPhotoView.setSwipingEnabled(enabled);
    }

    @Override
    public void notifyScreenNailChanged() {
        mScreenNailItem.setScreenNail(mAppBridge.attachScreenNail());
        mScreenNailSet.notifyChange();
    }

    @Override
    public Listener setGestureListener(Listener listener) {
        return mPhotoView.setGestureListener(listener);
    }

    @Override
    public void addSecureAlbumItem(boolean isVideo, int id) {
        mSecureAlbum.addMediaItem(isVideo, id);
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        mActionBar.createActionBarMenu(R.menu.photo, menu);
        mHaveImageEditor = GalleryUtils.isEditorAvailable(mActivity, "image/*");
        /// M: mark for build pass
        //added to support stereo display feature
        //addShareSelectedListener();
        updateMenuOperations();
        updateActionBarTitle();
        return true;
    }

    private MenuExecutor.ProgressListener mConfirmDialogListener =
            new MenuExecutor.ProgressListener() {
        @Override
        public void onProgressUpdate(int index) {}

        @Override
        public void onProgressComplete(int result) {}

        @Override
        public void onConfirmDialogShown() {
            mHandler.removeMessages(MSG_HIDE_BARS);
        }

        @Override
        public void onConfirmDialogDismissed(boolean confirmed) {
            refreshHidingMessage();
        }

        @Override
        public void onProgressStart() {}
    };

    private void switchToGrid() {
        if (mActivity.getStateManager().hasStateClass(AlbumPage.class)) {
            onUpPressed();
        } else {
            if (mOriginalSetPathString == null) return;
            if (mProgressBar != null) {
                updateCurrentPhoto(null);
                mProgressBar.hideProgress();
            }
            Bundle data = new Bundle(getData());
            data.putString(AlbumPage.KEY_MEDIA_PATH, mOriginalSetPathString);
            data.putString(AlbumPage.KEY_PARENT_MEDIA_PATH,
                    mActivity.getDataManager().getTopSetPath(
                            DataManager.INCLUDE_ALL));

            // We only show cluster menu in the first AlbumPage in stack
            // TODO: Enable this when running from the camera app
            boolean inAlbum = mActivity.getStateManager().hasStateClass(AlbumPage.class);
            data.putBoolean(AlbumPage.KEY_SHOW_CLUSTER_MENU, !inAlbum
                    && mAppBridge == null);

            data.putBoolean(PhotoPage.KEY_APP_BRIDGE, mAppBridge != null);

            // Account for live preview being first item
            mActivity.getTransitionStore().put(KEY_RETURN_INDEX_HINT,
                    mAppBridge != null ? mCurrentIndex - 1 : mCurrentIndex);

            if (mHasCameraScreennailOrPlaceholder && mAppBridge != null) {
                mActivity.getStateManager().startState(AlbumPage.class, data);
            } else {
                mActivity.getStateManager().switchState(this, AlbumPage.class, data);
            }
        }
    }

    @Override
    protected boolean onItemSelected(MenuItem item) {
        if (mModel == null) return true;
        refreshHidingMessage();
        MediaItem current = mModel.getMediaItem(0);

        if (current == null) {
            // item is not ready, ignore
            return true;
        }

        int currentIndex = mModel.getCurrentIndex();
        Path path = current.getPath();

        DataManager manager = mActivity.getDataManager();
        int action = item.getItemId();
        String confirmMsg = null;
        switch (action) {
            case android.R.id.home: {
                onUpPressed();
                return true;
            }
            case R.id.action_slideshow: {
                Bundle data = new Bundle();
                data.putString(SlideshowPage.KEY_SET_PATH, mMediaSet.getPath().toString());
                data.putString(SlideshowPage.KEY_ITEM_PATH, path.toString());
                data.putInt(SlideshowPage.KEY_PHOTO_INDEX, currentIndex);
                data.putBoolean(SlideshowPage.KEY_REPEAT, true);
                //add for DRM feature: pass drm inclusio info to next ActivityState
                if (mIsDrmSupported || mIsStereoDisplaySupported) {
                    data.putInt(DrmHelper.DRM_INCLUSION, mMtkInclusion);
                }
                mActivity.getStateManager().startStateForResult(
                        SlideshowPage.class, REQUEST_SLIDESHOW, data);
                return true;
            }
            case R.id.action_crop: {
                if (mIsStereoDisplaySupported && StereoHelper.isStereoImage(current)) {
                    Log.i(TAG,"onItemSelected:for stereo image, show dialog");
                    showConvertCropDialog(current);
                    return true;
                }
                Activity activity = mActivity;
                Intent intent = new Intent(FilterShowActivity.CROP_ACTION);
                intent.setClass(activity, FilterShowActivity.class);
                Uri uri = manager.getContentUri(path);
                uri = MediatekFeature.addMtkInclusion(uri, path);
                intent.setDataAndType(uri, current.getMimeType())
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activity.startActivityForResult(intent, PicasaSource.isPicasaImage(current)
                        ? REQUEST_CROP_PICASA
                        : REQUEST_CROP);
                return true;
            }
            case R.id.action_trim: {
                Intent intent = new Intent(mActivity, TrimVideo.class);
                intent.setData(manager.getContentUri(path));
                // We need the file path to wrap this into a RandomAccessFile.
                intent.putExtra(KEY_MEDIA_ITEM_PATH, current.getFilePath());
                mActivity.startActivityForResult(intent, REQUEST_TRIM);
                return true;
            }
            case R.id.action_picture_quality: {
                Activity activity = (Activity) mActivity;
                Intent intent = new Intent(PictureQualityTool.ACTION_PQ);
                intent.setClass(activity, PictureQualityTool.class);
                intent.setData(manager.getContentUri(path));
                Bundle pqBundle = new Bundle();
                pqBundle.putString("PQUri", manager.getContentUri(path).toString());
                pqBundle.putString("PQMineType", current.getMimeType());
                pqBundle.putInt("PQViewWidth", mPhotoView.getWidth());
                pqBundle.putInt("PQViewHeight", mPhotoView.getHeight());
                pqBundle.putInt("PQLevelCount", mModel.getLevelCount());
                intent.putExtras(pqBundle);
                MtkLog.i(TAG, "startActivity PQ");
                activity.startActivityForResult(intent, REQUEST_PQ);
                return true;
            }
            case R.id.action_switch_stereo_mode: {
                switchStereoMode();
                updateMenuOperations();
                refreshHidingMessage();
                return true;
            }
            case R.id.action_protect_info: {
                //add for drm protection info
                if (!mIsDrmSupported) return true;
                    Log.d(TAG,"onItemSelected:call manager to show info for " + path);
                    OmaDrmUiUtils.showProtectionInfoDialog((Activity)mActivity,
                                              manager.getContentUri(path));
                return true;
            }
            case R.id.action_edit: {
                if (mIsStereoDisplaySupported && StereoHelper.isStereoImage(current)) {
                    Log.i(TAG,"onItemSelected:for stereo image, show dialog");
                    showConvertEditDialog(current);
                    return true;
                }
                launchPhotoEditor();
                return true;
            }
            case R.id.action_details: {
                if (mShowDetails) {
                    hideDetails();
                } else {
                    showDetails();
                }
                return true;
            }

            case R.id.action_delete:
                confirmMsg = mActivity.getResources().getQuantityString(
                        R.plurals.delete_selection, 1);
            case R.id.action_setas:
            case R.id.action_rotate_ccw:
            case R.id.action_rotate_cw:
            case R.id.action_show_on_map:

            case R.id.action_print:
                mSelectionManager.deSelectAll();
                mSelectionManager.toggle(path);
                mMenuExecutor.onMenuClicked(item, confirmMsg, mConfirmDialogListener);
                return true;
            case R.id.action_import:
                mSelectionManager.deSelectAll();
                mSelectionManager.toggle(path);
                mMenuExecutor.onMenuClicked(item, confirmMsg,
                        new ImportCompleteListener(mActivity));
                return true;
            default :
                return false;
        }
    }

    private void hideDetails() {
        mShowDetails = false;
        mDetailsHelper.hide();
    }

    private void showDetails() {
        mShowDetails = true;
        if (mDetailsHelper == null) {
            mDetailsHelper = new DetailsHelper(mActivity, mRootPane, new MyDetailsSource());
            mDetailsHelper.setCloseListener(new CloseListener() {
                @Override
                public void onClose() {
                    hideDetails();
                }
            });
        }
        mDetailsHelper.show();
    }

    ////////////////////////////////////////////////////////////////////////////
    //  Callbacks from PhotoView
    ////////////////////////////////////////////////////////////////////////////
    @Override
    public void onSingleTapUp(int x, int y) {
        MtkLog.i(TAG, "onSingleTapUp()");
        if (mAppBridge != null) {
            if (mAppBridge.onSingleTapUp(x, y)) return;
        }

        MediaItem item = mModel.getMediaItem(0);
        if (item == null || item == mScreenNailItem) {
            // item is not ready or it is camera preview, ignore
            return;
        }

        int supported = item.getSupportedOperations();
        boolean playVideo = ((supported & MediaItem.SUPPORT_PLAY) != 0);
        boolean unlock = ((supported & MediaItem.SUPPORT_UNLOCK) != 0);
        boolean goBack = ((supported & MediaItem.SUPPORT_BACK) != 0);
        boolean launchCamera = ((supported & MediaItem.SUPPORT_CAMERA_SHORTCUT) != 0);

        if (playVideo) {
            // determine if the point is at center (1/6) of the photo view.
            // (The position of the "play" icon is at center (1/6) of the photo)
            int w = mPhotoView.getWidth();
            int h = mPhotoView.getHeight();
            playVideo = (Math.abs(x - w / 2) * 12 <= w)
                && (Math.abs(y - h / 2) * 12 <= h);
        }

        if (playVideo) {
            if (mSecureAlbum == null && !MediatekFeature.handleMavPlayback(mActivity.getAndroidContext(), item)) {
                playVideo(mActivity, item.getPlayUri(), item.getName());
            } else {
                mActivity.getStateManager().finishState(this);
            }
        } else if (goBack) {
            onBackPressed();
        } else if (unlock) {
            Intent intent = new Intent(mActivity, Gallery.class);
            intent.putExtra(Gallery.KEY_DISMISS_KEYGUARD, true);
            mActivity.startActivity(intent);
        } else if (launchCamera) {
            launchCamera();
        } else {
            if (mIsDrmSupported) {
                boolean consume = false;
                // determine if the point is at drm micro thumb of the photo view.
                int w = mPhotoView.getWidth();
                int h = mPhotoView.getHeight();
                consume = (Math.abs(x - w / 2) * 2 <= mDrmMicroThumbDim)
                       && (Math.abs(y - h / 2) * 2 <= mDrmMicroThumbDim);
                if (consume && tryConsumeDrmRights(item)) {
                    return;
                }
            }
            toggleBars();
        }
    }

    public void lockOrientation() {
        mHandler.sendEmptyMessage(MSG_LOCK_ORIENTATION);
    }

    public void unlockOrientation() {
        mHandler.sendEmptyMessage(MSG_UNLOCK_ORIENTATION);
    }

    @Override
    public void onActionBarAllowed(boolean allowed) {
        mActionBarAllowed = allowed;
        mHandler.sendEmptyMessage(MSG_UPDATE_ACTION_BAR);
    }

    @Override
    public void onActionBarWanted() {
        mHandler.sendEmptyMessage(MSG_WANT_BARS);
    }

    @Override
    public void onFullScreenChanged(boolean full) {
        Message m = mHandler.obtainMessage(
                MSG_ON_FULL_SCREEN_CHANGED, full ? 1 : 0, 0);
        m.sendToTarget();
    }

    /// M: for more info @{
    @Override
    public void onFullScreenChanged(boolean full, int type) {
        Message m = mHandler.obtainMessage(
                MSG_ON_FULL_SCREEN_CHANGED, full ? 1 : 0, type);
        m.sendToTarget();
    }
    /// @}

    // How we do delete/undo:
    //
    // When the user choose to delete a media item, we just tell the
    // FilterDeleteSet to hide that item. If the user choose to undo it, we
    // again tell FilterDeleteSet not to hide it. If the user choose to commit
    // the deletion, we then actually delete the media item.
    @Override
    public void onDeleteImage(Path path, int offset) {
        onCommitDeleteImage();  // commit the previous deletion
        mDeletePath = path;
        mDeleteIsFocus = (offset == 0);
        /// M: use latest current index from PhotoDataAdapter @{
        // mCurrentIndex would always be 0 if you never slide medias after you enter
        // Gallery by clicking one media from other applications like file manager.
        // Another example can be found in ALPS00419381
        // mMediaSet.addDeletion(path, mCurrentIndex + offset);
        mMediaSet.addDeletion(path, mModel.getCurrentIndex() + offset);
        /// @}
    }

    @Override
    public void onUndoDeleteImage() {
        if (mDeletePath == null) return;
        // If the deletion was done on the focused item, we want the model to
        // focus on it when it is undeleted.
        if (mDeleteIsFocus) mModel.setFocusHintPath(mDeletePath);
        mMediaSet.removeDeletion(mDeletePath);
        mDeletePath = null;
    }

    @Override
    public void onCommitDeleteImage() {
        if (mDeletePath == null) return;
        mSelectionManager.deSelectAll();
        mSelectionManager.toggle(mDeletePath);
        mMenuExecutor.onMenuClicked(R.id.action_delete, null, true, false);
        mDeletePath = null;
    }
    
    @Override
    public void onMavSeekBarAllowed(boolean allowed) {
        mIsMavSeekBarAllowed = allowed;
        mHandler.sendEmptyMessage(MSG_UPDATE_MAVSEEKBAR);
    }

    public void playVideo(Activity activity, Uri uri, String title) {
        MtkLog.i(TAG, "playVideo()");
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "video/*")
                    .putExtra(Intent.EXTRA_TITLE, title)
                    .putExtra(MovieActivity.KEY_TREAT_UP_AS_BACK, true);
            intent.putExtra(MediatekFeature.EXTRA_ENABLE_VIDEO_LIST, true);
            activity.startActivityForResult(intent, REQUEST_PLAY_VIDEO);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, activity.getString(R.string.video_err),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public boolean tryConsumeDrmRights(MediaItem item) {
        if (item == null || item instanceof LocalVideo) {
            return false;
        }
        if (mIsDrmSupported && !mModel.enteredConsumeMode() && 
            (item.getSupportedOperations() & MediaItem.SUPPORT_CONSUME_DRM) != 0) {
            Log.i(TAG,"tryConsumeDrmRights:show drm dialog");
            showDrmDialog((Context) mActivity, item);
            return true;
        }
        return false;
    }

    public void showDrmDialog(Context context, MediaItem item) {
        if (!mIsDrmSupported || !item.isDrm()) {
            Log.w(TAG, "showDrmDialog() is call for non-drm media!");
            return;
        }
        if (item instanceof LocalVideo) {
            Log.v(TAG, "showDrmDialog:encoutered LocalVideo, ignor");
            return;
        }

        final LocalImage imageItem = (LocalImage)item;

        int rights = DrmHelper.checkRightsStatusForTap(
                         context, imageItem.filePath, OmaDrmStore.Action.DISPLAY);
        final OmaDrmClient drmManagerClient =
                                    DrmHelper.getDrmManagerClient(context);

        if (OmaDrmStore.RightsStatus.RIGHTS_VALID == rights){
            OmaDrmUiUtils.showConsumeDialog(context,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int which) {
                        if (DialogInterface.BUTTON_POSITIVE == which) {
                            drmManagerClient.consumeRights(imageItem.filePath, 
                                             OmaDrmStore.Action.DISPLAY);
                            //consume
                            mModel.enterConsumeMode();
                            //hide bar
                            hideBars();
                        }
                        dialog.dismiss();
                    }
                },
                new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {}
                }
            );
        } else {
            if (OmaDrmStore.RightsStatus.SECURE_TIMER_INVALID == rights) {
                OmaDrmUiUtils.showSecureTimerInvalidDialog(context,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int which) {
                            dialog.dismiss();
                        }
                    }, 
                    new DialogInterface.OnDismissListener() {
                        public void onDismiss(DialogInterface dialog) {}
                    }
                );
            } else {
                OmaDrmUiUtils.showRefreshLicenseDialog(drmManagerClient, 
                                            context, imageItem.filePath);
            }
        }

    }

    private void setCurrentPhotoByIntent(Intent intent) {
        if (intent == null) return;
        Path path = mApplication.getDataManager()
                .findPathByUri(intent.getData(), intent.getType());
        if (path != null) {
            Path albumPath = mApplication.getDataManager().getDefaultSetOf(path);
            if (!albumPath.equalsIgnoreCase(mOriginalSetPathString)) {
                // If the edited image is stored in a different album, we need
                // to start a new activity state to show the new image
                Bundle data = new Bundle(getData());
                data.putString(KEY_MEDIA_SET_PATH, albumPath.toString());
                data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH, path.toString());
                mActivity.getStateManager().startState(PhotoPage.class, data);
                return;
            }
            mModel.setCurrentPhoto(path, mCurrentIndex);
        }
    }

    private void setMTKCurrentPhotoByIntent(Intent intent) {
        if (intent == null) return;
            Path photoEditPath = mApplication.getDataManager()
                .findPathByUri(intent.getData(), intent.getType());
        if (photoEditPath != null) {
            String string = photoEditPath.toString();
            if (string != null) {
                if (mIsDrmSupported || mIsStereoDisplaySupported) {
                    mModel.setCurrentPhoto(
                            Path.fromString(string, mMtkInclusion), mCurrentIndex);
                } else {
                    mModel.setCurrentPhoto(Path.fromString(string), mCurrentIndex);
                }
            }
        }
    }

    @Override
    protected void onStateResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED && requestCode != REQUEST_PLAY_VIDEO) {
            // This is a reset, not a canceled
            return;
        }
        /// M: mark for build pass
        /*if (resultCode == ProxyLauncher.RESULT_USER_CANCELED) {
            // Unmap reset vs. canceled
            resultCode = Activity.RESULT_CANCELED;
        }*/
        mRecenterCameraOnResume = false;
        /// M: In JB2, crop intent change from 'com.android.camera.action.CROP'
        //to 'com.android.camera.action.EDITOR_CROP'.
        switch (requestCode) {
            case REQUEST_EDIT:
                setMTKCurrentPhotoByIntent(data);
                break;
            case REQUEST_CROP:
                if (resultCode == Activity.RESULT_OK) {
                    setMTKCurrentPhotoByIntent(data);
                }
                break;
            case REQUEST_CROP_PICASA: {
                if (resultCode == Activity.RESULT_OK) {
                    Context context = mActivity.getAndroidContext();
                    String message = context.getString(R.string.crop_saved,
                            context.getString(R.string.folder_edited_online_photos));
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case REQUEST_SLIDESHOW: {
                if (data == null) break;
                String path = data.getStringExtra(SlideshowPage.KEY_ITEM_PATH);
                int index = data.getIntExtra(SlideshowPage.KEY_PHOTO_INDEX, 0);
                if (path != null) {
                    if (mIsDrmSupported || mIsStereoDisplaySupported) {
                        mModel.setCurrentPhoto(
                                  Path.fromString(path, mMtkInclusion), index);
                    } else {
                        mModel.setCurrentPhoto(Path.fromString(path), index);
                    }
                }
            }
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        mIsActive = false;

        mActivity.getGLRoot().unfreeze();
        mHandler.removeMessages(MSG_UNFREEZE_GLROOT);

        if (mIsStereoDisplaySupported) {
            //exit stereo mode
            Log.i(TAG,"onPause:exit stero mode");
//            mActivity.getGLRoot().setStereoMode(false, true);
            mPhotoView.allowStereoMode(false);
            mPhotoView.setStereoMode(false);
        }
        mActivity.getGLRoot().unfreeze();
        mHandler.removeMessages(MSG_UNFREEZE_GLROOT);

        DetailsHelper.pause();
        // Hide the detail dialog on exit
        if (mShowDetails) hideDetails();
        if (mModel != null) {
            mModel.pause();
        }
        mPhotoView.pause();
        mHandler.removeMessages(MSG_HIDE_BARS);
        mHandler.removeMessages(MSG_REFRESH_BOTTOM_CONTROLS);
        refreshBottomControlsWhenReady();
        mActionBar.removeOnMenuVisibilityListener(mMenuVisibilityListener);
        if (mShowSpinner) {
            mActionBar.disableAlbumModeMenu(true);
        }
        onCommitDeleteImage();
        mMenuExecutor.pause();
        if (mMediaSet != null) {
            // M: ContentListener had been removed,so should reset deletion 
            mMediaSet.clearDeletion();
            mMediaSet.resetDeletion();
        }
        if (mIsStereoDisplaySupported) {
            //remove message
            mHandler.removeMessages(MSG_UPDATE_MENU);
            //cancel convert task if needed
            if (mConvertIntentTask != null) {
                mConvertIntentTask.cancel();
                mConvertIntentTask = null;
            }
        }
        
        // M: tell ActivityState not to automatically turn lights on
        // when resume PhotoPage
        mShouldKeepLightsOutWhenResume = !mShowBars;
        
        if (mMavSeekBar != null) {
            mMavSeekBar.setVisibility(View.GONE);
            mMavSeekBar.restore();
        }
        
        unregisterGyroSensorListener();
    }

    @Override
    public void onCurrentImageUpdated() {
        mActivity.getGLRoot().unfreeze();
    }

    @Override
    public void onFilmModeChanged(boolean enabled) {
        refreshBottomControlsWhenReady();
        if (mShowSpinner) {
            if (enabled) {
                mActionBar.enableAlbumModeMenu(
                        GalleryActionBar.ALBUM_FILMSTRIP_MODE_SELECTED, this);
            } else {
                mActionBar.disableAlbumModeMenu(true);
            }
        }
        if (enabled) {
            mHandler.removeMessages(MSG_HIDE_BARS);
            // M: if current is mav, cancel mav decode task
            if (mModel.isMav(0)) {
                mModel.cancelCurrentMavDecodeTask();
            }
        } else {
            refreshHidingMessage();
        }
        updateActionBarTitle();
    }

    private void transitionFromAlbumPageIfNeeded() {
        TransitionStore transitions = mActivity.getTransitionStore();

        int albumPageTransition = transitions.get(
                KEY_ALBUMPAGE_TRANSITION, MSG_ALBUMPAGE_NONE);

        if (albumPageTransition == MSG_ALBUMPAGE_NONE && mAppBridge != null
                && mRecenterCameraOnResume) {
            // Generally, resuming the PhotoPage when in Camera should
            // reset to the capture mode to allow quick photo taking
            mCurrentIndex = 0;
            mPhotoView.resetToFirstPicture();
        } else {
            int resumeIndex = transitions.get(KEY_INDEX_HINT, -1);
            if (resumeIndex >= 0) {
                if (mHasCameraScreennailOrPlaceholder) {
                    // Account for preview/placeholder being the first item
                    resumeIndex++;
                }
                if (resumeIndex < mMediaSet.getMediaItemCount()) {
                    mCurrentIndex = resumeIndex;
                    mModel.moveTo(mCurrentIndex);
                }
            }
        }

        if (albumPageTransition == MSG_ALBUMPAGE_RESUMED) {
            mPhotoView.setFilmMode(mStartInFilmstrip || mAppBridge != null);
        } else if (albumPageTransition == MSG_ALBUMPAGE_PICKED) {
            mPhotoView.setFilmMode(false);
        }
    }

    @Override
    protected void onResume() {
        MediatekMMProfile.startProfilePhotoPageOnResume();
        super.onResume();
        ///M:
        mCanSlideToPrePicture = true;

        if (mModel == null) {
            mActivity.getStateManager().finishState(this);
            return;
        }
        transitionFromAlbumPageIfNeeded();

        mActivity.getGLRoot().freeze();
        mIsActive = true;
        setContentPane(mRootPane);

        mModel.resume();
        mPhotoView.resume();
        mActionBar.addOnMenuVisibilityListener(mMenuVisibilityListener);
        updateActionBarTitle();
        refreshBottomControlsWhenReady();
        if (mShowSpinner && mPhotoView.getFilmMode()) {
            mActionBar.enableAlbumModeMenu(
                    GalleryActionBar.ALBUM_FILMSTRIP_MODE_SELECTED, this);
        }
        if (!mShowBars) {
            mActionBar.hide();
            if (mAppBridge != null && mCurrentIndex == 0
                    && !mPhotoView.getFilmMode()) {
                mActivity.getGLRoot().setLightsOutMode(false);
            } else {
                mActivity.getGLRoot().setLightsOutMode(true);
            }
        }
        boolean haveImageEditor = GalleryUtils.isEditorAvailable(mActivity, "image/*");
        if (haveImageEditor != mHaveImageEditor) {
            mHaveImageEditor = haveImageEditor;
            updateMenuOperations();
        }

        mRecenterCameraOnResume = false;//true;
        mHandler.sendEmptyMessageDelayed(MSG_UNFREEZE_GLROOT, UNFREEZE_GLROOT_TIMEOUT);
        
        // M: allow action bar auto-hide
        refreshHidingMessage();
        /// M: show or hide MavSeekBar, and enable or disable some gestures
        updateMavSeekBar();
        registerGyroSensorListener();
        MediatekMMProfile.stopProfilePhotoPageOnResume();
    }

    @Override
    protected void onDestroy() {
        if (mAppBridge != null) {
            mAppBridge.setServer(null);
            mScreenNailItem.setScreenNail(null);
            mAppBridge.detachScreenNail();
            mAppBridge = null;
            mScreenNailSet = null;
            mScreenNailItem = null;
        }
        mActivity.getGLRoot().setOrientationSource(null);
        if (mBottomControls != null) mBottomControls.cleanup();

        // Remove all pending messages.
        /// M: null pointer protection
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }

    private class MyDetailsSource implements DetailsSource {

        @Override
        public MediaDetails getDetails() {
            return mModel.getMediaItem(0).getDetails();
        }

        @Override
        public int size() {
            return mMediaSet != null ? mMediaSet.getMediaItemCount() : 1;
        }

        @Override
        public int setIndex() {
            return mModel.getCurrentIndex();
        }
    }

    @Override
    public void onAlbumModeSelected(int mode) {
        if (mode == GalleryActionBar.ALBUM_GRID_MODE_SELECTED) {
            switchToGrid();
        }
    }

    @Override
    public void refreshBottomControlsWhenReady() {
        if (mBottomControls == null) {
            return;
        }
        MediaObject currentPhoto = mCurrentPhoto;
        if (currentPhoto == null) {
            mHandler.obtainMessage(MSG_REFRESH_BOTTOM_CONTROLS, 0, 0, currentPhoto).sendToTarget();
        } else {
            currentPhoto.getPanoramaSupport(mRefreshBottomControlsCallback);
        }
    }

    private void updatePanoramaUI(boolean isPanorama360) {
        Menu menu = mActionBar.getMenu();

        // it could be null if onCreateActionBar has not been called yet
        if (menu == null) {
            return;
        }

        MenuExecutor.updateMenuForPanorama(menu, isPanorama360, isPanorama360);

        if (isPanorama360) {
            MenuItem item = menu.findItem(R.id.action_share);
            if (item != null) {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                item.setTitle(mActivity.getResources().getString(R.string.share_as_photo));
            }
        } else if ((mCurrentPhoto.getSupportedOperations() & MediaObject.SUPPORT_SHARE) != 0) {
            MenuItem item = menu.findItem(R.id.action_share);
            if (item != null) {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                item.setTitle(mActivity.getResources().getString(R.string.share));
            }
        }
    }

    @Override
    public void onUndoBarVisibilityChanged(boolean visible) {
        refreshBottomControlsWhenReady();
    }

    //the Stereo display feature adds a 3D/2D converting icon.
    //Displaying 3D icon meaning that when clicked, it will enter 3D
    //display mode.
    //Displaying 2D icon meaning that when clicked, it will enter 2D
    //display mode.

    //The principle of 3D/2D icon and 3D/2D display mode with second image
    //loaded is that:
    //  When picture is displayed in 3D mode, icon is 2D, clickable
    //  When picture is displayed in 2D mode, icon is 3D, clickable

    //The principle of 3D/2D icon and 3D/2D display mode without second 
    //image loaded is that:
    //  When current picture is a truely 3D picture, icon is 2D, not clickable
    //  When current picture is a truely 2D picture, icon is 3D, not clickable

    //The click behavior of 3D/2D icon is that:
    // If it is not clickable: do nothing
    // If it is clickable:
    //    If icon is 3D, change picutre display mode to 3D, and show 2D icon,
    //        and Zoom out if needed.
    //    If icon is 2D, change picutre display mode to 2D, and show 3D icon

    //The behavior of double click picture and 3D/2D icon is that:
    // When picture is displayed in 3D mode, double click will cause it changed
    //     to 2D mode, and change icon to 3D.
    // When picture is displayed in 2D mode, double click will follow exactly
    //     routin.
    // When picture is displayed in zoomed 2D mode (currently it should be 2D,
    //     logic may change in the future), if double click cause picture zoomed
    //     out to original position, the display mode depends on previous state
    //     before we changed to zoomed in state:
    //    When previous state is 2D display mode, we will remain in 2D mode
    //    When previous state is 3D display mdoe, we will return to 3D mode

    public void onChangedToStereoMode(boolean stereoMode) {
        Log.i(TAG, "onChangedToStereoMode(stereoMode="+stereoMode+")");
        //if (!isStereoStateReady()) {
        //    return;
        //}
        if (STEREO_MODE_3D == mStereoMode && stereoMode ||
            STEREO_MODE_2D == mStereoMode && !stereoMode) return;
        //zoom out if needed
        if (STEREO_MODE_2D == mStereoMode) {
            mPhotoView.onResetZoomedState();
        }
        mStereoMode = 1 - mStereoMode;
        updateMenuOperationsInViewThread();
    }

    public void updateMenuOperationsInViewThread() {
        mHandler.removeMessages(MSG_UPDATE_MENU);
        mHandler.sendEmptyMessage(MSG_UPDATE_MENU);
    }

    private boolean isStereoStateReady() {
        if (mCurrentPhoto == null || !mIsStereoDisplaySupported ||
            null == mModel.getStereoScreenNail(2)) {
            return false;
        } else {
            return true;
        }
    }
    
    // added for supporting stereo 3D/2D mode switching
    private void switchStereoMode() {
        if (!isStereoStateReady()) {
            return;
        }
        //if in 2D mode, and user want swith to 3D mode, we should 
        //firstly zoom to original state
        if (STEREO_MODE_2D == mStereoMode) {
            mPhotoView.onResetZoomedState();
        }
        mStereoMode = 1 - mStereoMode;
        if(null != mModel) {
            mModel.updateMavStereoMode(STEREO_MODE_3D == mStereoMode);
        }
        int supportedOperations = mCurrentPhoto.getSupportedOperations();
        setPhotoViewStereoMode(mStereoMode, supportedOperations);
    }

    private void setPhotoViewStereoMode(int stereoMode, int supportedOperations) {
        if (STEREO_MODE_3D == stereoMode && ((supportedOperations & 
            MediaObject.SUPPORT_STEREO_DISPLAY) != 0)) {
            Log.i(TAG,"setPhotoViewStereoMode:now in 3D mode, show stereo");
            mPhotoView.allowStereoMode(true);
            mPhotoView.setStereoMode(true);
        } else {
            Log.i(TAG,"setPhotoViewStereoMode:now in 2D mode, stop stereo");
            mPhotoView.allowStereoMode(false);
            mPhotoView.setStereoMode(false);
        }
    }

    private void showConvertCropDialog(final MediaItem item) {
        final AlertDialog.Builder builder =
                            new AlertDialog.Builder((Context)mActivity);

        DialogInterface.OnClickListener clickListener =
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (DialogInterface.BUTTON_POSITIVE == which) {
                        //we don't convert here because CropImage can
                        //help do the same thing
                        startCropIntent(item);
                    }
                    dialog.dismiss();
                }
            };

        String crop = ((Activity) mActivity).getString(R.string.crop_action);
        String convertCrop = ((Activity) mActivity).getString(
                         R.string.stereo3d_convert2d_dialog_text,crop);

        builder.setPositiveButton(android.R.string.ok, clickListener);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setTitle(R.string.stereo3d_convert2d_dialog_title)
               .setMessage(convertCrop);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void startCropIntent(final MediaItem item) {
        Activity activity = (Activity) mActivity;
        Intent intent = new Intent(CropImage.CROP_ACTION);
        intent.setClass(activity, CropImage.class);
        Uri uri = item.getContentUri();
        uri = MediatekFeature.addMtkInclusion(uri, item.getPath());
        intent.setData(uri);
        activity.startActivityForResult(intent, PicasaSource.isPicasaImage(item)
                ? REQUEST_CROP_PICASA
                : REQUEST_CROP);
    }

    private void showConvertEditDialog(final MediaItem item) {
        final AlertDialog.Builder builder =
                            new AlertDialog.Builder((Context)mActivity);

        DialogInterface.OnClickListener clickListener =
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (DialogInterface.BUTTON_POSITIVE == which) {
                        convertAndEdit(item);
                    }
                    dialog.dismiss();
                }
            };

        String edit = ((Activity) mActivity).getString(R.string.edit);
        String convertEdit = ((Activity) mActivity).getString(
                         R.string.stereo3d_convert2d_dialog_text,edit);

        builder.setPositiveButton(android.R.string.ok, clickListener);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setTitle(R.string.stereo3d_convert2d_dialog_title)
               .setMessage(convertEdit);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void convertAndEdit(final MediaItem item) {
        Log.i(TAG,"convertAndEdit(item="+item+")");
        if (mConvertEditTask != null) {
            mConvertEditTask.cancel();
        }
        //show converting dialog
        int messageId = R.string.stereo3d_convert2d_progress_text;
        mProgressDialog = ProgressDialog.show((Activity)mActivity, null, 
                ((Activity)mActivity).getString(messageId), true, false);
        //create a job that convert intents and start sharing intent.
        mConvertEditTask = mActivity.getThreadPool().submit(new Job<Void>() {
            public Void run(JobContext jc) {
                //the majer process!
                Uri convertedUri = StereoConvertor.convertSingle(jc, (Context)mActivity,
                             item.getContentUri(), item.getMimeType());
                //dismis progressive dialog when we done
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mConvertIntentTask = null;
                        if (null != mProgressDialog) {
                            Log.v(TAG,"mConvertEditTask:dismis ProgressDialog");
                            mProgressDialog.dismiss();
                        }
                    }
                });
                //start new intent
                if (!jc.isCancelled()) {
                    Log.i(TAG,"mConvertEditTask:start new edit intent!");
                    startEditIntent(convertedUri, item.getContentUri());
                }
                return null;
            }
        });
    }
    
    private void startEditIntent(Uri uri, Uri originUri) {
        if (null == uri || null == originUri) return;
        Intent intent = new Intent(Intent.ACTION_EDIT)
                .setData(uri)
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(MtkUtils.URI_FOR_SAVING, originUri.toString());

        ((Activity) mActivity).startActivityForResult(Intent.createChooser(intent, null),
                REQUEST_EDIT);
    }

    /*private void addShareSelectedListener() {
        if (null == mShareActionProvider) return;
        OnShareTargetSelectedListener listener = new OnShareTargetSelectedListener() {
            public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
                //when set share intent, we should first check if there is stereo
                //image inside, and set this info into to Bundle inside the intent
                //When this function runs, we should check that whether we should
                //prompt a dialog. If No, returns false and continue original
                //rountin. If Yes, change the content of Bundle inside intent, and
                //re-start the intent by ourselves.
                if (mIsStereoDisplaySupported) {
Log.i(TAG,"addShareSelectedListener:intent="+intent);
                    return checkIntent(intent);
                }
Log.w(TAG,"onShareTargetSelected:follow original routin..");
                return false;
            }
        };

        mShareActionProvider.setOnShareTargetSelectedListener(listener);
    }*/

    private boolean checkIntent(Intent intent) {
        if (null == intent) return false;
        if (intent.getAction() != Intent.ACTION_SEND) {
            Log.w(TAG, "checkIntent: unintented action type");
            return false;
        }
        Uri uri = (Uri)intent.getExtra(Intent.EXTRA_STREAM);
Log.i(TAG,"checkIntent:uri="+uri);
        if (null == uri) {
            Log.e(TAG, "checkIntent:got null uri");
            return false;
        }
        DataManager manager = mActivity.getDataManager();
        Path itemPath = manager.findPathByUri(uri, intent.getType());
Log.v(TAG,"checkIntent:itemPath="+itemPath);
        // M: for temp images which we cannot get a proper path, 
        // just follow the original routine
        if (itemPath == null) {
            return false;
        }
        MediaItem item = (MediaItem) manager.getMediaObject(itemPath);
Log.v(TAG,"checkIntent:item="+item);
        // M: for path which we cannot get a proper MediaItem, for klocwork issue
        if (item == null) {
            return false;
        }
        int support = manager.getSupportedOperations(itemPath);
Log.i(TAG,"checkIntent:support:"+support);
        if ((support & MediaObject.SUPPORT_STEREO_DISPLAY) != 0 &&
            (support & MediaObject.SUPPORT_CONVERT_TO_3D) == 0 &&
            MediaObject.MEDIA_TYPE_IMAGE == item.getMediaType()) {
Log.i(TAG,"checkIntent:found a stereo image");
            checkIntent(intent, item);
            StereoHelper.makeShareProviderIgnorAction(intent);
            return true;
        } else {
            //for normal image or video, follow original routin
            return false;
        }
    }

    private void checkIntent(Intent intent, MediaItem item) {
        if (null == intent || null == intent.getComponent()) {
            Log.e(TAG,"checkStereoIntent:invalid intent:"+intent);
            return;
        }

        String packageName = intent.getComponent().getPackageName();
        Log.d(TAG,"checkStereoIntent:packageName="+packageName);
        //this judgement is very simple, need to enhance in the future
        boolean onlyShareAs2D = "com.android.mms".equals(packageName);
        showStereoShareDialog(intent, item, onlyShareAs2D);
    }

    private void showStereoShareDialog(Intent intent, 
                    final MediaItem item, boolean shareAs2D) {
        int positiveCap = 0;
        int negativeCap = 0;
        int title = 0;
        int message = 0;
        if (shareAs2D) {
            positiveCap = android.R.string.ok;
            negativeCap = android.R.string.cancel;
            title = R.string.stereo3d_convert2d_dialog_title;
            message = R.string.stereo3d_share_convert_text_single;
        } else {
            positiveCap = R.string.stereo3d_share_dialog_button_2d;
            negativeCap = R.string.stereo3d_share_dialog_button_3d;
            title = R.string.stereo3d_share_dialog_title;
            message = R.string.stereo3d_share_dialog_text_single;
        }
        final Intent shareIntent = intent;
        final boolean onlyShareAs2D = shareAs2D;
        final AlertDialog.Builder builder =
                        new AlertDialog.Builder((Context)mActivity);

        DialogInterface.OnClickListener clickListener =
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (DialogInterface.BUTTON_POSITIVE == which) {
Log.i(TAG,"showStereoShareDialog:convert to 2D clicked!");
                        convertAndShare(shareIntent, item);
                    } else {
                        if (!onlyShareAs2D) {
Log.i(TAG,"showStereoShareDialog:start original intent!");
                            safeStartIntent(shareIntent);
                        }
                    }
                    dialog.dismiss();
                }
            };
        builder.setPositiveButton(positiveCap, clickListener);
        builder.setNegativeButton(negativeCap, clickListener);
        builder.setTitle(title)
               .setMessage(message);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void safeStartIntent(Intent intent) {
        try {
            ((Activity)mActivity).startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            android.widget.Toast.makeText(((Activity)mActivity), 
                ((Activity)mActivity).getString(R.string.activity_not_found),
                android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void convertAndShare(final Intent intent, final MediaItem item) {
        Log.i(TAG,"convertAndShare(intent="+intent+",item="+item+")");
        if (mConvertIntentTask != null) {
            mConvertIntentTask.cancel();
        }
        //show converting dialog
        int messageId = R.string.stereo3d_convert2d_progress_text;
        mProgressDialog = ProgressDialog.show((Activity)mActivity, null, 
                ((Activity)mActivity).getString(messageId), true, false);
        //create a job that convert intents and start sharing intent.
        mConvertIntentTask = mActivity.getThreadPool().submit(new Job<Void>() {
            public Void run(JobContext jc) {
                //the majer process!
                Uri convertedUri = StereoConvertor.convertSingle(jc, (Context)mActivity,
                             item.getContentUri(), item.getMimeType());
                intent.putExtra(Intent.EXTRA_STREAM, convertedUri);
                //dismis progressive dialog when we done
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mConvertIntentTask = null;
                        if (null != mProgressDialog) {
                            Log.v(TAG,"mConvertIntentTask:dismis ProgressDialog");
                            mProgressDialog.dismiss();
                        }
                    }
                });
                //start new intent
                if (!jc.isCancelled()) {
Log.i(TAG,"showStereoShareDialog:start new intent!");
                    safeStartIntent(intent);
                }
                return null;
            }
        });
    }
    
    @Override
    protected void onConfigurationChanged(Configuration config) {
        mHandler.sendEmptyMessage(MSG_RELOAD_MAVSEEKBAR);
    }
    
    // M: added for performance auto test
    public boolean mLoadingFinished = false;
    private boolean mDisableBarChanges = false;
    
    public void disableBarChanges(boolean disable) {
        mDisableBarChanges = disable;
    }
    
    // M: added for Camera to control PhotoView render process
    @Override
    public void renderFullPictureOnly(boolean fullPictureOnly) {
        mPhotoView.renderFullPictureOnly(fullPictureOnly);
    }

    /// M: @{
    protected void onSaveState(Bundle outState) {
        // keep record of current index and current photo
        mData.putInt(KEY_INDEX_HINT, mCurrentIndex);
        if (mCurrentPhoto != null) {
            Path photoPath = mCurrentPhoto.getPath();
            if (photoPath != null) {
                mData.putString(KEY_MEDIA_ITEM_PATH, photoPath.toString());
            }
        }
    }
    /// @}

    // invoked when change screen direction
    // change width of MavSeekBar
    private void reloadMavSeekBar() {
        if(mMavSeekBar == null) return;
        Config.PhotoPage config = Config.PhotoPage.get((Context)mActivity);
        LayoutParams params = mMavSeekBar.getLayoutParams();
        if(mActivity.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT) {
            params.width = config.mavSeekBarWidthInPortrait;
        } else {
            params.width = config.mavSeekBarWidthInLandscape;
        }
        mMavSeekBar.setLayoutParams(params);
    }

    // M: show photo name on ActionBar when not film mode
    // and show album name onActionBar when film mode
    private void updateActionBarTitle() {
        if (mPhotoView == null || mActionBar == null) {
            return;
        }

        try {
            if (mActivity.getStateManager().getTopState() != this) {
                return;
            }
        } catch (AssertionError e) {
            Log.v(TAG, "no state in State Manager when updates actionbar title");
            return;
        }

        if (mPhotoView.getFilmMode()) {
            mActionBar.setDisplayOptions(((mSecureAlbum == null) && (mSetPathString != null)),
                    false);
            mActionBar.setTitle(mMediaSet != null ? mMediaSet.getName() : "");
        } else {
            mActionBar
                    .setDisplayOptions(((mSecureAlbum == null) && (mSetPathString != null)), true);
            mActionBar.setTitle(mCurrentPhoto != null ? mCurrentPhoto.getName() : "");
        }
    }

    /// M: {@
    /// the gyro listener is used to play mav in Photopage
    private SensorManager mSensorManager;
    private Sensor mGyroSensor;
    private boolean mHasGyroSensor;
    
    private Display mDisplay;
    private int mOrientation = -1;
    private float mValue = 0;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp = 0;
    private float angle[] = {0,0,0};
    private static final float TH = 0.001f;
    private static final float OFFSET = 0.0f;
    private static int BASE_ANGLE = 15;
    private boolean mFirstTime = true;
    private int mLastIndex = 0xFFFF;
    
    private void initGyroSensor() {
        mSensorManager = (SensorManager)mActivity.getAndroidContext()
                                .getSystemService(Context.SENSOR_SERVICE);
        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mHasGyroSensor = (mGyroSensor != null);
        if (!mHasGyroSensor) {
            // TODO 
            // show MavSeekBar if has gyro sensor
            // hide MavSeekBar if no gyro sensor,
            // maybe this action should be do when onProgressChanged()
            MtkLog.d(TAG, "not has gyro sensor");
        }
        
        mDisplay = ((Activity)mActivity).getWindowManager().getDefaultDisplay();
    }
    
    private void registerGyroSensorListener() {
        if (mHasGyroSensor) {
            MtkLog.d(TAG, "register gyro sensor listener");
            mSensorManager.registerListener(this, mGyroSensor,
                SensorManager.SENSOR_DELAY_GAME);
            initValueBuffer();
        }
    }
    
    private void unregisterGyroSensorListener() {
        if (mHasGyroSensor) {
            MtkLog.d(TAG, "unregister gyro listener");
            mSensorManager.unregisterListener(this);
        }
    }
    
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private LinkedList<Float> valueBuffer = new LinkedList<Float>();
    private static final int SIZE = 5;
    private void initValueBuffer() {
        for(int i = 0; i < SIZE; i++) {
            valueBuffer.add(new Float(0.0f));
        }
    }
    
    private float updateBufferAndGet(float newValue) {
        valueBuffer.remove();
        valueBuffer.add(new Float(newValue));
        float total = 0f;
        for(int i = 0; i < SIZE; i++) {
            total += valueBuffer.get(i);
        }
        return total / SIZE;
    }
    
    public void onSensorChanged(SensorEvent event) {
        if(mCurrentPhoto == null || mCurrentPhoto.getSubType() != MediaObject.SUBTYPE_MPO_MAV) {
            return;
        }
        
        // ignore sensor before mav frames ready
        if (mModel == null || !mModel.isMavLoadingFinished()) {
            return;
        }
        
        //workaround for Gyro sensor HW limitation.
        //As sensor continues to report small movement, wrongly
        //indicating that the phone is slowly moving, we should
        //filter the small movement.
        final float xSmallRotateTH = 0.05f;
        //xSmallRotateTH indicating the threshold of max "small
        //rotation". This varible is determined by experiments
        //based on MT6575 platform. May be adjusted on other chips.
        
        float valueToUse = 0;
        int newRotation = mDisplay.getRotation();
        if (mOrientation != newRotation) {
            // orientation has changed, reset calculations
            mOrientation = newRotation;
            mValue = 0;
            angle[0] = 0;
            angle[1] = 0;
            angle[2] = 0;
            mFirstTime = true;
        }
        switch (mOrientation) {
        case Surface.ROTATION_0:
            valueToUse = event.values[1];
            break;
        case Surface.ROTATION_90:
            // no need to re-map
            valueToUse = event.values[0];
            break;
        case Surface.ROTATION_180:
            // we do not have this rotation on our device
            valueToUse = -event.values[1];
            break;
        case Surface.ROTATION_270:
            valueToUse = -event.values[0];
            break;
        default:
            valueToUse = event.values[0];
        }
        if (updateBufferAndGet(Math.abs(valueToUse)) < xSmallRotateTH) {
            return;
        }
        
        mValue = valueToUse + OFFSET;
        if (timestamp != 0 && Math.abs(mValue) > TH) {
            final float dT = (event.timestamp - timestamp) * NS2S;

            angle[1] += mValue * dT * 180 / Math.PI;
            if (mFirstTime) {
                angle[0] = angle[1] - BASE_ANGLE;
                angle[2] = angle[1] + BASE_ANGLE;
                mFirstTime = false;
            } else if (angle[1] <= angle[0]) {
                angle[0] = angle[1];
                angle[2] = angle[0] + 2 * BASE_ANGLE;
            } else if (angle[1] >= angle[2]) {
                angle[2] = angle[1];
                angle[0] = angle[2] - 2 * BASE_ANGLE;
            }
            
            int totalCount = mModel.getTotalFrameCount();
            
            if (totalCount != 0) {
                int index = (int) (angle[1] - angle[0]) * totalCount / (2 * BASE_ANGLE);
                if (index >= 0 && index < totalCount) {
                    if (mLastIndex == 0xFFFF || mLastIndex != index) {
                        mModel.setImageBitmap(index);
                        mLastIndex = index;
                        mMavSeekBar.syncProgressByGyroSensor(index);
                    }
                }
            }
        }
        timestamp = event.timestamp;
    }
    /// @}
    //M: 
    public boolean canSlideToPrePicture() {
        return mCanSlideToPrePicture;
    }
    ///M: roll back to JB4.2.1 @{
    //enter camera will lock orientation, so need update orientation compensation
    //and request layout
    public void onOrientationCompensationChanged() {
        mActivity.getGLRoot().requestLayoutContentPane();
    }
    ///}@
}
