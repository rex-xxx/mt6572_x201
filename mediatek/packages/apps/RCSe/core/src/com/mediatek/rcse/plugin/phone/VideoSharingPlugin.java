/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.rcse.plugin.phone;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mediatek.rcse.activities.widgets.TimeBar;
import com.mediatek.rcse.api.Logger;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.client.ClientApiException;
import com.orangelabs.rcs.service.api.client.SessionState;
import com.orangelabs.rcs.service.api.client.media.IMediaPlayer;
import com.orangelabs.rcs.service.api.client.media.IMediaRenderer;
import com.orangelabs.rcs.service.api.client.media.video.IVideoPlayerEventListener;
import com.orangelabs.rcs.service.api.client.media.video.IVideoSurfaceViewListener;
import com.orangelabs.rcs.service.api.client.media.video.VideoSurfaceView;
import com.orangelabs.rcs.service.api.client.media.video.VideoSurfaceView.OnReceiveDataListener;
import com.orangelabs.rcs.service.api.client.richcall.IVideoSharingEventListener;
import com.orangelabs.rcs.service.api.client.richcall.IVideoSharingSession;
import com.orangelabs.rcs.utils.PhoneUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class defined to implement the function interface of ICallScreenPlugIn,
 * and archive the main function here.When user start a video share the method
 * {@link #start(String)} will be called. If client receive a video share, it
 * will go to {@link VideoSharingInvitationReceiver#onReceive(Context, Intent)}.
 */
public class VideoSharingPlugin extends SharingPlugin implements SurfaceHolder.Callback,
        DialogInterface.OnClickListener, TimeBar.Listener {
    private static final String TAG = "VideoSharingPlugin";
    /* package */static final String VIDEO_SHARING_INVITATION_ACTION =
            "com.orangelabs.rcs.richcall.VIDEO_SHARING_INVITATION";
    /* package */static final String VIDEO_SHARING_ACCEPT_ACTION =
            "com.mediatek.phone.plugin.VIDEO_SHARING_ACCEPT_ACTION";
    /* package */static final String VIDEO_SHARING_DECLINE_ACTION =
            "com.mediatek.phone.plugin.VIDEO_SHARING_DECLINE_ACTION";
    /* package */static final String VIDEO_SHARING_START_ACTION =
            "com.mediatek.phone.plugin.VIDEO_SHARING_START_ACTION";
    /* package */static final String SERVICE_STATUS = "com.orangelabs.rcs.SERVICE_STATUS";
    /* package */static final String SERVICE_REGISTRATION =
            "com.orangelabs.rcs.SERVICE_REGISTRATION";
    /* package */static final String VIDEO_NAME = "videoName";
    /* package */static final String VIDEO_DURATION = "videoDuration";
    private static final String QVGA = "QVGA";
    private static final String TEL_URI_SCHEMA = "tel:";
    private static final String CAMERA_ID = "camera-id";
    private static final String DEFAULT_VIEDO_FORMAT = "h263-200";
    private static final String DEFAULT_VIEDO_FORMAT_STORED = "h263-2000";
    private static final int QVGA_WIDTH = 480;
    private static final int QVGA_HEIGHT = 320;
    private static final int QCIF_WIDTH = 176;
    private static final int QCIF_HEIGHT = 144;
    private static final int MIN_WINDOW_WIDTH = 83;
    private static final int MIN_WINDOW_HEIGHT = 133;
    private static final int SWITCH_BUTTON_WIDTH = 70;
    private static final int SWITCH_BUTTON_HEIGHT = 70;
    private static final int SWITCH_BUTTON_MARGIN_TOP = 20;
    private static final int SWITCH_BUTTON_MARGIN_BOTTOM = 0;
    private static final int SWITCH_BUTTON_MARGIN_LEFT = 0;
    private static final int SWITCH_BUTTON_MARGIN_RIGHT = 20;
    private static final int LOCAL_CAMERA_MARGIN_TOP = 10;
    private static final int LOCAL_CAMERA_MARGIN_BOTTOM = 0;
    private static final int LOCAL_CAMERA_MARGIN_LEFT = 0;
    private static final int LOCAL_CAMERA_MARGIN_RIGHT = 10;
    private static final int DELAY_TIME = 500;
    private static final String CMA_MODE = "mtk-cam-mode";
    private static final int ROTATION_BY_HW = 3;
    private static final int SINGLE_CAMERA = 1;
    private static final int CONTROLL_START = 1;
    private static final int CONTROLL_STOP = 2;
    private int mShareStatus = ShareStatus.UNKNOWN;

    // Indicate whether click start button
    private final AtomicBoolean mIsStarted = new AtomicBoolean(false);
    private final AtomicInteger mVideoSharingState =
            new AtomicInteger(Constants.SHARE_VIDEO_STATE_IDLE);
    private RelativeLayout mOutgoingDisplayArea = null;
    private ViewGroup mIncomingDisplayArea = null;
    // In current phase ,just care live video share, as a result it's value
    // never false.
    // Surface holder for video preview
    private SurfaceHolder mPreviewSurfaceHolder = null;
    private Camera mCamera = null;
    private final Object mLock = new Object();
    // Camera preview started flag
    private boolean mCameraPreviewRunning = false;
    private IVideoSharingSession mOutgoingVideoSharingSession = null;
    private IMediaPlayer mOutgoingVideoPlayer = null;
    private VideoSurfaceView mOutgoingLocalVideoSurfaceView = null;
    private IMediaRenderer mOutgoingLocalVideoRenderer = null;
    private VideoSurfaceView mOutgoingRemoteVideoSurfaceView = null;
    private OnReceiveDataListener mOutgoingRemoteVideoSurfaceViewOnReceiveDataListener = null;
    private IMediaRenderer mOutgoingRemoteVideoRenderer = null;
    private String mOutgoingVideoFormat = null;
    private int mVideoWidth = 176;
    private int mVideoHeight = 144;
    // Video surface holder
    private SurfaceHolder mSurfaceHolder;
    private final AtomicBoolean mIsVideoSharingSender = new AtomicBoolean(false);
    private final AtomicBoolean mIsVideoSharingReceiver = new AtomicBoolean(false);

    // For incoming video share information
    private volatile IVideoSharingSession mIncomingVideoSharingSession = null;
    private VideoSurfaceView mIncomingLocalVideoSurfaceView = null;
    private IMediaRenderer mIncomingLocalVideoRenderer = null;
    private VideoSurfaceView mIncomingRemoteVideoSurfaceView = null;
    private OnReceiveDataListener mIncomingRemoteVideoSurfaceViewOnReceiveDataListener = null;
    private IMediaRenderer mIncomingRemoteVideoRenderer = null;
    private int mCamerNumber = 0;
    private int mOpenedCameraId = 0;
    String mIncomingSessionId = null;
    String mIncomingVideoFormat = null;
    private CompoundButton mAudioButton = null;
    private BluetoothHeadset mBluetoothHeadset = null;

    private ImageView mSwitchCamerImageView = null;
    private final AtomicBoolean mGetIncomingSessionWhenApiConnected = new AtomicBoolean(false);
    private final AtomicBoolean mStartOutgoingSessionWhenApiConnected = new AtomicBoolean(false);
    // Save a set of number to be listened, the number was passed from phone
    private final CopyOnWriteArraySet<String> mNumbersToBeListened =
            new CopyOnWriteArraySet<String>();
    private final CopyOnWriteArraySet<CallScreenDialog> mCallScreenDialogSet =
            new CopyOnWriteArraySet<CallScreenDialog>();
    private WaitingProgressDialog mWaitingProgressDialog = null;
    private VideoSharingDialogManager mVideoSharingDialogManager = null;
    private IVideoSurfaceViewListener mVideoSurfaceViewListener = null;

    private String mVideoFileName = null;
    private long mVideoDuration;
    public static final String VIDEO_LIVE = "videolive";
    private static final String AUDIO_BUTTON = "audioButton";
    private static final String VIEW_ID = "id";
    private boolean mHeadsetConnected = false;

    private ControllButton mControllButton = null;
    private ControllProgress mTimeBar = null;
    private StatusImageView mStoredVideoStatusView;
    private static final int STORED_VIDEO_STATUS_PLAY = 0;
    private static final int STORED_VIDEO_STATUS_PAUSE = 1;
    private VideoSharingInvitationReceiver mVideoSharingInvitationReceiver = new VideoSharingInvitationReceiver();

    private BluetoothProfile.ServiceListener mBluetoothProfileServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Logger.d(TAG, "mBluetoothProfileServiceListener onServiceConnected");
            mBluetoothHeadset = (BluetoothHeadset) proxy;
        }

        @Override
        public void onServiceDisconnected(int profile) {
            Logger.d(TAG, "mBluetoothProfileServiceListener onServiceDisconnected");
            mBluetoothHeadset = null;
        }
    };

    private ISharingPlugin mVideoSharingCallBack = new ISharingPlugin() {

        @Override
        public void onApiConnected() {
            Logger.d(TAG, "onApiConnected() entry!");
            mCountDownLatch.countDown();
            if (mGetIncomingSessionWhenApiConnected.compareAndSet(true, false)) {
                Logger.v(TAG, "onApiConnected(), " +
                        "Richcall api connected, and need to get incoming video share session");
                getIncomingVideoSharingSession();
            } else {
                Logger.v(TAG, "onApiConnected(), " +
                        "Richcall api connected, but need not to get incoming video share session");
            }
            if (mStartOutgoingSessionWhenApiConnected.compareAndSet(true, false)) {
                Logger.v(TAG, "onApiConnected(), " +
                        "Richcall api connected, and need to start outgoing video share session");
                startOutgoingVideoShareSession();
            } else {
                Logger.v(TAG, "onApiConnected(), " +
                        "Richcall api connected, but need not to start outgoing video share session");
            }
        }

        @Override
        public void onFinishSharing() {
            destroy();
        }

        @Override
        public void displayStoredVideoUI(boolean isShow) {
            Logger.d(TAG, "displayStoredVideoUI(), isShow: " + isShow + " mShareStatus: "
                    + mShareStatus);
            boolean isStored = (mShareStatus == ShareStatus.STORED_IN);
            if (isShow) {
                if (mTimeBar != null) {
                    mTimeBar.show();
                } else {
                    Logger.w(TAG, "displayStoredVideoUI(), mTimeBar is null!");
                }
                if (mControllButton != null) {
                    mControllButton.showButton();
                } else {
                    Logger.w(TAG, "displayStoredVideoUI(), mControllButton is null!");
                }
                if (mStoredVideoStatusView != null && isStored) {
                    mStoredVideoStatusView.show();
                } else {
                    Logger.w(TAG, "displayStoredVideoUI(), mStoredVideoStatusView is null!");
                }
            } else {
                if (mTimeBar != null) {
                    mTimeBar.hide();
                } else {
                    Logger.w(TAG, "displayStoredVideoUI(), mTimeBar is null!");
                }
                if (mControllButton != null) {
                    mControllButton.hideButton();
                } else {
                    Logger.w(TAG, "displayStoredVideoUI(), mControllButton is null!");
                }
                if (mStoredVideoStatusView != null) {
                    mStoredVideoStatusView.hide();
                } else {
                    Logger.w(TAG, "displayStoredVideoUI(), mStoredVideoStatusView is null!");
                }
            }
        }
    };

    /**
     * Constructor
     * 
     * @param ctx Application context
     */
    public VideoSharingPlugin(Context context) {
        super(context);
        Logger.v(TAG, "VideoSharingPlugin constructor. context = " + context);
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(mContext,
                mBluetoothProfileServiceListener, BluetoothProfile.HEADSET);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(VIDEO_SHARING_INVITATION_ACTION);
        intentFilter.addAction(VIDEO_SHARING_ACCEPT_ACTION);
        intentFilter.addAction(VIDEO_SHARING_DECLINE_ACTION);
        intentFilter.addAction(VIDEO_SHARING_START_ACTION);
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(mVideoSharingInvitationReceiver, intentFilter);
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                Logger.v(TAG, "CallScreenPlugin constructor. Thread start run.");
                connectRichCallApi();
                PhoneUtils.initialize(mContext);
                if (RcsSettings.getInstance() != null) {
                    mOutgoingVideoFormat = RcsSettings.getInstance().getCShVideoFormat();
                    String cshVideoSize = RcsSettings.getInstance().getCShVideoSize();
                    Logger.v(TAG, "cshVideoSize = " + cshVideoSize);
                    if (QVGA.equals(cshVideoSize)) {
                        // QVGA
                        mVideoWidth = QVGA_WIDTH;
                        mVideoHeight = QVGA_HEIGHT;
                    } else {
                        // QCIF
                        mVideoWidth = QCIF_WIDTH;
                        mVideoHeight = QCIF_HEIGHT;
                    }
                } else {
                    Logger.e(TAG, "RcsSettings.getInstance() return null");
                }
                Logger.v(TAG, "mOutgoingVideoFormat = " + mOutgoingVideoFormat);
                mOutgoingVideoFormat = "h264";
                Logger.v(TAG, "mOutgoingVideoFormat = " + mOutgoingVideoFormat);
                AndroidFactory.setApplicationContext(mContext);
                // Get the number of the camera
                mCamerNumber = Utils.getCameraNums();
                Logger.v(TAG, "mCamerNumber = " + mCamerNumber);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                Logger.v(TAG, "onPostExecute() ");
                Logger.v(TAG, "After register mVideoSharingInvitationReceiver");
                if (mCallScreenHost != null) {
                    boolean isSupportVideoShare = getCapability(mNumber);
                    mCallScreenHost.onCapabilityChange(mNumber, isSupportVideoShare);
                }
            }

        }.execute();

        mSwitchCamerImageView = new ImageView(mContext);
        mSwitchCamerImageView.setImageResource(R.drawable.ic_rotate_camera_disabled_holo_dark);
        mSwitchCamerImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });
        mVideoSharingDialogManager = new VideoSharingDialogManager();
        mInterface = mVideoSharingCallBack;
    }

    private void initAudioButton() {
        Logger.d(TAG, "initAudioButton entry, mAudioButton: " + mAudioButton + " mCallScreenHost: "
                + mCallScreenHost);
        if (mAudioButton == null) {
            if (mCallScreenHost != null) {
                Activity inCallScreen = mCallScreenHost.getCallScreenActivity();
                String packageName = inCallScreen.getPackageName();
                Resources resource = inCallScreen.getResources();
                mAudioButton = (CompoundButton) inCallScreen.findViewById(resource.getIdentifier(
                        AUDIO_BUTTON, VIEW_ID, packageName));
            } else {
                Logger.d(TAG, "initAudioButton mCallScreenHost is null");
            }
        }
        Logger.d(TAG, "initAudioButton exit: " + mAudioButton);
    }

    private void turnOnSpeaker() {
        boolean on = isSpeakerOn();
        Logger.d(TAG, "turnOnSpeaker() entry, on: " + on);
        if (!on) {
            if (mAudioButton != null) {
                mAudioButton.performClick();
            } else {
                Logger.d(TAG, "turnOnSpeaker() mAudioButton is null, retry to init");
                initAudioButton();
                if (mAudioButton != null) {
                    mAudioButton.performClick();
                } else {
                    Logger.d(TAG, "turnOnSpeaker() mAudioButton is null");
                }
            }
        }
        Logger.d(TAG, "turnOnSpeaker() exit");
    }

    private boolean isSpeakerOn() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.isSpeakerphoneOn();
    }

    protected boolean isBluetoothAvailable() {
        Logger.d(TAG, "isBluetoothAvailable() entry");
        boolean isConnected = false;
        if (mBluetoothHeadset != null) {
            List<BluetoothDevice> deviceList = mBluetoothHeadset.getConnectedDevices();
            int size = deviceList.size();
            Logger.d(TAG, "isBluetoothAvailable() getConnectedDevices size: " + size);
            if (size > 0) {
                isConnected = true;
            }
        }
        Logger.d(TAG, "isBluetoothAvailable() isConnected: " + isConnected);
        return isConnected;
    }

    // Implements ICallScreenPlugIn
    @Override
    public void start(String number) {
        Logger.d(TAG, "start() entry, number: " + number + " mShareStatus: " + mShareStatus);
        super.start(number);
        // Image share is ongoing.
        if (Utils.isInImageSharing()) {
            alreadyOnGoing();
            return;
        }
        // Video share is of full status.
        if (mShareStatus == ShareStatus.STORED_IN || mShareStatus == ShareStatus.STORED_OUT
                || mShareStatus == ShareStatus.LIVE_OUT || mShareStatus == ShareStatus.LIVE_TWOWAY) {
            alreadyOnGoing();
            return;
        }
        if (!isVideoShareSupported(number)) {
            videoShareNotSupported();
            return;
        }
        mVideoSharingDialogManager.showSelectVideoDialog();
    }

    @Override
    public void stop() {
        Logger.v(TAG, "stop button is clicked");
        destroy();
    }

    @Override
    public int getState() {
        Logger.v(TAG, "getState(), getState() = " + mVideoSharingState.get());
        return mVideoSharingState.get();
    }

    private void startVideoSharePickDialog() {
        Intent intent = new Intent(RichcallProxyActivity.IMAGE_SHARING_SELECTION);
        intent.putExtra(RichcallProxyActivity.SELECT_TYPE, RichcallProxyActivity.SELECT_TYPE_VIDEO);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mContext.startActivity(intent);
    }

    private void startVideoShare() {
        Logger.v(TAG, "startVideoShare(), number = " + mNumber + ", then request a view group");
        if (mNumber == null) {
            Logger.e(TAG, "startVideoShare() number is null");
            return;
        }
        if (!Utils.isInImageSharing() && mIsStarted.compareAndSet(false, true)) {
            int networkType = Utils.getNetworkType(mContext);
            startByNetwork(networkType);
            // Tell that it is the vs sender if start vs before receive vs
            // invitation
            if (!mIsVideoSharingReceiver.get()) {
                mIsVideoSharingSender.set(true);
            }
            if (mCallScreenHost != null) {
                mOutgoingDisplayArea = (RelativeLayout) mCallScreenHost.requestAreaForDisplay();
            } else {
                Logger.w(TAG, "startVideoShare() mCallScreenHost is null");
            }
            showLocalView();
        } else {
            Logger.w(TAG, "startVideoShare() Start has been clicked, please wait until the session failed");
            alreadyOnGoing();
        }
    }

    private void startByNetwork(int networkType) {
            // 2G & 2.5
            if (networkType == Utils.NETWORK_TYPE_GSM || networkType == Utils.NETWORK_TYPE_GPRS
                    || networkType == Utils.NETWORK_TYPE_UMTS) {
                Logger.v(TAG, "startVideoShare()-2G or 2.5G mobile network, fibbiden video share");
                Resources resources = mContext.getResources();
                String message = resources.getString(R.string.now_allowed_video_share_by_network);
                showToast(message);
                mIsStarted.set(false);
                return;
        } else if (networkType == Utils.NETWORK_TYPE_EDGE || networkType == Utils.NETWORK_TYPE_UMTS
                || networkType == Utils.NETWORK_TYPE_HSUPA || networkType == Utils.NETWORK_TYPE_HSDPA
                || networkType == Utils.NETWORK_TYPE_1XRTT || networkType == Utils.NETWORK_TYPE_EHRPD) { // 2.75G
                                                                                                         // or
                                                                                                         // 3G
            Logger.v(TAG, "startVideoShare()-2.75G or 3G mobile network, " + "allow single line video share");
                if (mIncomingVideoSharingSession != null) {
                    try {
                        if (mIncomingVideoSharingSession.getSessionState() == SessionState.ESTABLISHED
                                || mIncomingVideoSharingSession.getSessionState() == SessionState.PENDING) {
                            Resources resources = mContext.getResources();
                            String message = resources.getString(R.string.now_allowed_video_share_by_network);
                            showToast(message);
                            return;
                        }
                    } catch (RemoteException e) {
                        Logger.e(TAG, e.toString());
                    }
                } else {
                Logger.d(TAG, "startByNetwork(), mIncomingVideoSharingSession is null");
                            }
            } else if (networkType == Utils.NETWORK_TYPE_HSPA || networkType == Utils.NETWORK_TYPE_LTE
                    || networkType == Utils.NETWORK_TYPE_UMB) { // 4G
                Logger.v(TAG, "startVideoShare()-4G mobile network, allow two-way video share");
            } else if (networkType == Utils.NETWORK_TYPE_WIFI) { // WI-FI
                Logger.v(TAG, "startVideoShare()-WI-FI network, allow two-way video share");
            } else { // Unknown
                Logger.v(TAG, "startVideoShare()-Unknown network, default to allow two-way video share");
            }
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
                Logger.v(TAG, "startVideoShare() when start, acquire a wake lock");
            } else {
            Logger.v(TAG, "startVideoShare() when start, the wake lock has been acquired," + " so do not acquire");
            }
            mNumbersToBeListened.add(mNumber);

            if (mShareStatus == ShareStatus.LIVE_OUT || mShareStatus == ShareStatus.LIVE_TWOWAY) {
                // Create the live video player
            Logger.v(TAG, "startVideoShare() start createLiveVideoPlayer mOutgoingVideoFormat is: "
                                + mOutgoingVideoFormat);
                try {
                mOutgoingVideoPlayer = mRichCallApi
                                    .createLiveVideoPlayer(mOutgoingVideoFormat != null ? mOutgoingVideoFormat
                                            : DEFAULT_VIEDO_FORMAT);
                } catch (ClientApiException e) {
                    e.printStackTrace();
                    mIsStarted.set(false);
                    return;
                }
            } else {
                // Create the prerecorded video player
                IVideoPlayerEventListener listener = new IVideoPlayerEventListener.Stub() {
                    public void updateDuration(long progress) {
                    }

                    public void endOfStream() {
                    }
                };

                try {
                mOutgoingVideoPlayer = mRichCallApi.createPrerecordedVideoPlayer(DEFAULT_VIEDO_FORMAT_STORED,
                                    mVideoFileName, listener);
                } catch (ClientApiException e) {
                    e.printStackTrace();
                }
            }
    }

    private void showLocalView() {
        Logger.d(TAG, "showLocalView(), mShareStatus: " + mShareStatus + " mIsVideoSharingSender: "
                + mIsVideoSharingSender);
        if (mShareStatus == ShareStatus.LIVE_OUT || mShareStatus == ShareStatus.LIVE_IN
                || mShareStatus == ShareStatus.LIVE_TWOWAY) {
            if (mIsVideoSharingSender.get()) {
                Logger.v(TAG, "startVideoShare() First send vs invitation");
                showSenderLocalView();
                showWaitRemoteAcceptMessage();
            } else if (mIsVideoSharingReceiver.get()) {
                Logger.v(TAG,
                        "startVideoShare() After recevie vs invitation, then send vs invitation");
                showReceiverLocalView();
                showWaitRemoteAcceptMessage();
            }
        } else {
            if (mIsVideoSharingSender.get()) {
                Logger.v(TAG, "startVideoShare() First send vs invitation");
                showSenderStoredView();
                showWaitRemoteAcceptMessage();
            } else if (mIsVideoSharingReceiver.get()) {
                Logger.v(TAG,
                        "startVideoShare() After recevie vs invitation, then send vs invitation");
                showReceiverLocalView();
                showWaitRemoteAcceptMessage();
            }
        }
        mVideoSharingState.set(Constants.SHARE_VIDEO_STATE_SHARING);
        Utils.setInVideoSharing(true);
        if (mRichCallStatus == RichCallStatus.CONNECTED) {
            Logger.v(TAG, "startVideoShare(), then call startOutgoingVideoShareSession()");
            startOutgoingVideoShareSession();
        } else {
            Logger
                    .v(TAG,
                            "startVideoShare(), call startOutgoingVideoShareSession() when richcall api connected.");
            mStartOutgoingSessionWhenApiConnected.set(true);
        }
    }

    private void alreadyOnGoing() {
        Logger.v(TAG, "alreadyOnGoing entry");
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                String message =
                        mContext.getResources().getString(R.string.video_sharing_is_on_going);
                showToast(message);
            }
        });
        Logger.v(TAG, "alreadyOnGoing exit");
    }

    public boolean dismissDialog() {
        Logger.v(TAG, "dismissDialog()");
        dismissAllDialogs();
        return false;
    }

    private void dismissAllDialogs() {
        Logger.v(TAG, "dismissAllDialog()");
        for (CallScreenDialog dialog : mCallScreenDialogSet) {
            dialog.dismissDialog();
            mCallScreenDialogSet.remove(dialog);
            Logger.v(TAG, "have dismissed a dialog: " + dialog);
        }
        mVideoSharingDialogManager.dismissWaitingInitializeConextProgressDialog();
    }

    /**
     * Start the outgoing session
     */
    private void startOutgoingVideoShareSession() {
        Logger.v(TAG, "startOutgoingVideoShareSession");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Initiate sharing
                    Logger.d(TAG, "startOutgoingVideoShareSession(), mShareStatus: " + mShareStatus);
                    if (mShareStatus == ShareStatus.LIVE_OUT
                            || mShareStatus == ShareStatus.LIVE_TWOWAY) {
                        String vodafoneAccount = getVodafoneAccount(mNumber);
                        if (vodafoneAccount != null) {
                            mOutgoingVideoSharingSession =
                                    mRichCallApi.initiateLiveVideoSharing(TEL_URI_SCHEMA
                                            + getVodafoneAccount(mNumber), mOutgoingVideoPlayer);
                        } else {
                            Logger.w(TAG, "vodafoneAccount is null");
                        }
                    } else {
                        Logger.v(TAG, "Initiate a stored video sharing");
                        mOutgoingVideoSharingSession =
                                mRichCallApi.initiateVideoSharing(TEL_URI_SCHEMA
                                        + getVodafoneAccount(mNumber), mVideoFileName,
                                        mOutgoingVideoPlayer);
                    }
                    if (mOutgoingVideoSharingSession != null) {
                        mOutgoingVideoSharingSession
                                .addSessionListener(mOutgoingSessionEventListener);
                    } else {
                        Logger.w(TAG, "mOutgoingVideoSharingSession is null.");
                        handleStartVideoSharingFailed();
                    }
                } catch (ClientApiException e) {
                    e.printStackTrace();
                    handleStartVideoSharingFailed();
                } catch (RemoteException e) {
                    e.printStackTrace();
                    handleStartVideoSharingFailed();
                }
            }
        });
    }

    private void handleStartVideoSharingFailed() {
        Logger.d(TAG, "handleStartVideoSharingFailed entry");
        if (mIsVideoSharingSender.get()) {
            Logger.v(TAG, "handleStartVideoSharingFailed sender fail");
            destroy();
        } else {
            Logger.v(TAG, "handleStartVideoSharingFailed receiver fail");
            // share status here can be: LIVE_TWOWAY LIVE_OUT STORED_OUT
            if (mShareStatus == ShareStatus.LIVE_TWOWAY) {
                mShareStatus = ShareStatus.LIVE_IN;
            } else {
                mShareStatus = ShareStatus.UNKNOWN;
            }
            destroyOutgoingViewOnly();
        }
        Logger.d(TAG, "handleStartVideoSharingFailed exit");
    }

    /**
     * Outgoing video sharing session event listener
     */
    private IVideoSharingEventListener mOutgoingSessionEventListener = new IVideoSharingEventListener.Stub() {
        private static final String TAG = "OutgoingSessionEventListener";

        // Session is started
        public void handleSessionStarted() {
            Logger.v(TAG, "handleSessionStarted(), mShareStatus: " + mShareStatus
                    + " mHeadsetConnected: " + mHeadsetConnected);
            try {
                if (mOutgoingVideoSharingSession != null) {
                    if (is3GMobileNetwork()) {
                        if (mIncomingVideoSharingSession != null) {
                            Logger.d(TAG,"Reject the incoming session because the device is upder " +
                                    "3G network and the outgoing video share session is established");
                            mIncomingVideoSharingSession.cancelSession();
                        }
                    } else {
                        mOutgoingVideoSharingSession.setMediaRenderer(mOutgoingLocalVideoRenderer);
                        // Tell the host to update
                        mVideoSharingState.set(Constants.SHARE_VIDEO_STATE_SHARING);
                        mCallScreenHost.onStateChange(Constants.SHARE_VIDEO_STATE_SHARING);
                        showRemoteAcceptMessage();
                    }
                } else {
                    Logger.w(TAG, "mOutgoingVideoSharingSession is null");
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if ((mShareStatus == ShareStatus.STORED_OUT) && !mHeadsetConnected
                            && !isBluetoothAvailable()) {
                        turnOnSpeaker();
                    }
                }
            });
        }

        // Session has been aborted
        public void handleSessionAborted() {
            Logger.v(TAG, "handleSessionAborted()");
            mIsStarted.set(false);
            if (mIsVideoSharingReceiver.get()) {
                // At this time we are receiving vs from remote, so
                // should only remove outgoing view
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        removeOutgoingVideoSharingVews();
                    }
                });
            } else {
                mVideoSharingDialogManager.showTerminatedByNetworkDialog();
            }
        }

        // Session has been terminated by remote
        public void handleSessionTerminatedByRemote() {
            Logger.v(TAG, "handleSessionTerminatedByRemote()");
            if (mIsVideoSharingReceiver.get()) {
                // At this time we are receiving vs from remote, so
                // should only remove outgoing view
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        removeOutgoingVideoSharingVews();
                    }
                });
            } else {
                mVideoSharingDialogManager.showTerminatedByRemoteDialog();
            }
        }

        // Content sharing error
        public void handleSharingError(final int error) {
            Logger.v(TAG, "handleSharingError(), error = " + error);
            switch (error) {
                case ImsServiceError.SESSION_INITIATION_CANCELLED:
                    Logger.v(TAG, "SESSION_INITIATION_CANCELLED");
                    break;
                case ImsServiceError.SESSION_INITIATION_DECLINED:
                    Logger.v(TAG, "SESSION_INITIATION_DECLINED");
                    mVideoSharingDialogManager.showRejectedByRemoteDialog();
                    return;
                case ImsServiceError.SESSION_INITIATION_FAILED:
                    Logger.v(TAG,
                            "SESSION_INITIATION_FAILED, at most case it is a 408 error(time out)");
                    if (mIsVideoSharingReceiver.get()) {
                        // At this time we are receiving vs from remote,
                        // so should only remove outgoing view
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                removeOutgoingVideoSharingVews();
                                mVideoSharingDialogManager.showTerminatedByNetworkDialog();
                            }
                        });
                    } else {
                        mVideoSharingDialogManager.showTerminatedByNetworkDialog();
                    }
                    return;
                case ContentSharingError.SESSION_INITIATION_TIMEOUT:
                    Logger.v(TAG, "SESSION_INITIATION_TIMEOUT)");
                    // status can be LIVE_OUT, STORED_OUT, LIVE_TWOWAY
                    if (mShareStatus == ShareStatus.LIVE_TWOWAY) {
                        mShareStatus = ShareStatus.LIVE_IN;
                    } else {
                        mShareStatus = ShareStatus.UNKNOWN;
                    }
                    if (mIsVideoSharingReceiver.get()) {
                        // At this time we are receiving vs from remote,
                        // so should only remove outgoing view
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                removeOutgoingVideoSharingVews();
                                mVideoSharingDialogManager.showTimeOutDialog();
                            }
                        });
                    } else {
                        mVideoSharingDialogManager.showTimeOutDialog();
                    }
                    return;
                default:
                    break;
            }
            if (mIsVideoSharingReceiver.get()) {
                // At this time we are receiving vs from remote, so
                // should only remove outgoing view
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        removeOutgoingVideoSharingVews();
                    }
                });
            } else {
                mVideoSharingDialogManager.showTerminatedByRemoteDialog();
            }
        }
    };

    /**
     * Incoming video sharing session event listener
     */
    private IVideoSharingEventListener mIncomingSessionEventListener = new IVideoSharingEventListener.Stub() {
        private static final String TAG = "IncomingSessionEventListener";

        // Session is started
        public void handleSessionStarted() {
            Logger.v(TAG, "handleSessionStarted(), mShareStatus: " + mShareStatus
                    + " mHeadsetConnected: " + mHeadsetConnected);
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if ((mShareStatus == ShareStatus.STORED_IN) && !mHeadsetConnected
                            && !isBluetoothAvailable()) {
                        turnOnSpeaker();
                    }
                }
            });
        }

        // Session has been aborted
        public void handleSessionAborted() {
            Logger.v(TAG, "handleSessionAborted()");
            // status can be LIVE_IN, STORED_IN, LIVE_TWOWAY
            if (mShareStatus == ShareStatus.LIVE_TWOWAY) {
                mShareStatus = ShareStatus.LIVE_OUT;
            } else {
                mShareStatus = ShareStatus.UNKNOWN;
            }
            mVideoSharingDialogManager.showTimeOutDialog();
        }

        // Session has been terminated by remote
        public void handleSessionTerminatedByRemote() {
            Logger.v(TAG, "handleSessionTerminatedByRemote()");
            mVideoSharingDialogManager.showTerminatedByRemoteDialog();
        }

        // Sharing error
        public void handleSharingError(final int error) {
            Logger.v(TAG, "handleSharingError(), error = " + error);
            switch (error) {
                case ImsServiceError.SESSION_INITIATION_CANCELLED:
                    Logger.v(TAG, "SESSION_INITIATION_CANCELLED");
                    break;
                case ImsServiceError.SESSION_INITIATION_DECLINED:
                    Logger.v(TAG, "SESSION_INITIATION_DECLINED");
                    break;
                case ImsServiceError.SESSION_INITIATION_FAILED:
                    Logger.v(TAG, "SESSION_INITIATION_FAILED");
                    break;
                default:
                    break;
            }
            mVideoSharingDialogManager.showTerminatedByRemoteDialog();
        }
    };

    // Implements SurfaceHolder.Callback
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        synchronized (mLock) {
            boolean isLive = (mShareStatus == ShareStatus.LIVE_IN
                    || mShareStatus == ShareStatus.LIVE_OUT || mShareStatus == ShareStatus.LIVE_TWOWAY);
            Logger.d(TAG, "surfaceCreated(), mCamerNumber: " + mCamerNumber + ", mCamera: "
                    + mCamera + ", mShareStatus: " + mShareStatus);
            if (mCamera == null && isLive) {
                // Start camera preview
                if (mCamerNumber > SINGLE_CAMERA) {
                    // Try to open the front camera
                    Logger.v(TAG, "surfaceCreated(), try to open the front camera");
                    mCamera = Camera.open(CameraInfo.CAMERA_FACING_FRONT);
                } else {
                    Logger.v(TAG, "surfaceCreated(), try to open the front camera");
                    openDefaultCamera();
                }
                addPreviewCallback();
            }
        }
    }

    private void addPreviewCallback() {
        Logger.v(TAG, "addPreviewCallback entry");
        if (mCamera == null) {
            Logger.v(TAG, "addPreviewCallback mCamera is null");
            return;
        }
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {

            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                onCamreaPreviewFrame(data, camera);
            }
        });
        Logger.v(TAG, "addPreviewCallback exit");
    }

    private void onCamreaPreviewFrame(byte[] data, Camera camera) {
        try {
            if (mOutgoingVideoPlayer != null) {
                mOutgoingVideoPlayer.onPreviewFrame(data);
            } else {
                Logger.d(TAG, "onPreviewFrame(), addPreviewCallback mOutgoingLiveVideoPlayer is null");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Logger.v(TAG, "surfaceChanged(), mShareStatus: " + mShareStatus);
        mPreviewSurfaceHolder = holder;
        synchronized (mLock) {
            if (mCamera != null) {
                if (mCameraPreviewRunning) {
                    mCameraPreviewRunning = false;
                    mCamera.stopPreview();
                }
            } else {
                Logger.w(TAG, "mCamera is null");
            }
        }
        boolean isLive = (mShareStatus == ShareStatus.LIVE_IN
                || mShareStatus == ShareStatus.LIVE_OUT || mShareStatus == ShareStatus.LIVE_TWOWAY);
        if (isLive) {
            startCameraPreview();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Logger.v(TAG, "surfaceDestroyed(), holder = " + holder + ",set holder to null");
    }

    /**
     * Start the camera preview
     */
    private void startCameraPreview() {
        Logger.v(TAG, "startCameraPreview()");
        synchronized (mLock) {
            if (mCamera != null) {
                Camera.Parameters p = mCamera.getParameters();
                // Init Camera
                p.setPreviewSize(mVideoWidth, mVideoHeight);
                p.setPreviewFormat(PixelFormat.YCbCr_420_SP);
                // Try to set front camera if back camera doesn't support size
                List<Camera.Size> sizes = p.getSupportedPreviewSizes();
                if (sizes != null && !sizes.contains(mCamera.new Size(mVideoWidth, mVideoHeight))) {
                    Logger.v(TAG, "Does not contain");
                    String camId = p.get(CAMERA_ID);
                    if (camId != null) {
                        p.set(CAMERA_ID, 2);
                        p.set(CMA_MODE, ROTATION_BY_HW);
                    } else {
                        Logger.v(TAG, "cam_id is null");
                    }
                } else {
                    Logger.v(TAG, "startCameraPreview(), sizes object = " + sizes + ". contains the size object. "
                            + "mOpenedCameraId = " + mOpenedCameraId);
                    p.set(CMA_MODE, ROTATION_BY_HW);
                }
                mCamera.setParameters(p);
                try {
                    mCamera.setPreviewDisplay(mPreviewSurfaceHolder);
                    mCamera.startPreview();
                    mCameraPreviewRunning = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    mCamera = null;
                }
            } else {
                Logger.e(TAG, "mCamera is null");
            }
        }
    }

    private void stopCameraPreview() {
        Logger.v(TAG, "stopCameraPreview()");
        // Release the camera
        synchronized (mLock) {
            if (mCamera != null) {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                mCameraPreviewRunning = false;
            } else {
                Logger.v(TAG, "mCamera is null, so do not release");
            }
        }
    }

    private void removeIncomingVideoSharingVews() {
        Logger.v(TAG, "removeIncomingVideoSharingVews");
        if (mIncomingDisplayArea != null) {
            mIncomingDisplayArea.removeView(mIncomingRemoteVideoSurfaceView);
            mIncomingDisplayArea.removeView(mIncomingLocalVideoSurfaceView);
            mIncomingDisplayArea.removeView(mSwitchCamerImageView);
            if (mTimeBar != null && mStoredVideoStatusView != null) {
                mTimeBar.hide();
                mStoredVideoStatusView.hide();
            } else {
                Logger.d(TAG, "removeIncomingVideoSharingVews(), mTimeBar is null");
            }
        } else {
            Logger.w(TAG, "mIncomingDisplayArea is null");
        }
    }

    private void removeIncomingVideoSharingRemoteVews() {
        Logger.v(TAG, "removeIncomingVideoSharingRemoteVews");
        if (mIncomingDisplayArea != null) {
            mIncomingDisplayArea.removeView(mIncomingRemoteVideoSurfaceView);
        } else {
            Logger.w(TAG, "mIncomingDisplayArea is null");
        }
    }

    private void removeIncomingVideoSharingLocalVews() {
        Logger.v(TAG, "removeIncomingVideoSharingLocalVews");
        if (mIncomingDisplayArea != null) {
            mIncomingDisplayArea.removeView(mIncomingLocalVideoSurfaceView);
            mIncomingDisplayArea.removeView(mSwitchCamerImageView);
        } else {
            Logger.w(TAG, "mIncomingDisplayArea is null");
        }
    }

    private void removeOutgoingVideoSharingVews() {
        Logger.v(TAG, "removeOutgoingVideoSharingVews");
        if (mOutgoingDisplayArea != null) {
            mOutgoingDisplayArea.removeView(mOutgoingLocalVideoSurfaceView);
            mOutgoingDisplayArea.removeView(mOutgoingRemoteVideoSurfaceView);
            mOutgoingDisplayArea.removeView(mSwitchCamerImageView);
            mOutgoingDisplayArea.removeView(mControllButton);
            mOutgoingDisplayArea.removeView(mTimeBar);
            mControllButton = null;
            mTimeBar = null;
        } else {
            Logger.w(TAG, "mOutgoingDisplayArea is null");
        }
    }

    // This is only called by the first sender.
    private void showSenderLocalView() {
        Logger.v(TAG, "showSenderLocalView() entry, mOutgoingVideoFormat is: "
                + mOutgoingVideoFormat);
        removeOutgoingVideoSharingVews();
        mOutgoingLocalVideoSurfaceView = new VideoSurfaceView(mContext);
        int width = mOutgoingDisplayArea.getWidth();
        int height = mOutgoingDisplayArea.getHeight();
        Logger.v(TAG, "showSenderLocalView(), width = " + width + ",height = " + height
                + "mVideoWidth = " + mVideoWidth + ",mVideoHeight = " + mVideoHeight);
        LayoutParams params =
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        mOutgoingLocalVideoSurfaceView.setLayoutParams(params);

        mSurfaceHolder = mOutgoingLocalVideoSurfaceView.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(VideoSharingPlugin.this);
        try {
            mOutgoingLocalVideoRenderer =
                    mRichCallApi
                            .createVideoRenderer(mOutgoingVideoFormat != null ? mOutgoingVideoFormat
                                    : DEFAULT_VIEDO_FORMAT);
        } catch (ClientApiException e) {
            e.printStackTrace();
        }
        RelativeLayout.LayoutParams layoutParams =
                new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        mOutgoingDisplayArea.addView(mOutgoingLocalVideoSurfaceView, layoutParams);

        layoutParams =
                new RelativeLayout.LayoutParams(Utils.dip2px(mContext, SWITCH_BUTTON_WIDTH), Utils
                        .dip2px(mContext, SWITCH_BUTTON_HEIGHT));
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams.setMargins(SWITCH_BUTTON_MARGIN_LEFT, Utils.dip2px(mContext,
                SWITCH_BUTTON_MARGIN_TOP), Utils.dip2px(mContext, SWITCH_BUTTON_MARGIN_RIGHT),
                SWITCH_BUTTON_MARGIN_BOTTOM);
        mOutgoingDisplayArea.addView(mSwitchCamerImageView, layoutParams);
        mVideoSurfaceViewListener = new IVideoSurfaceViewListener.Stub() {
            public void onSampleReceived(Bitmap frame, long duration) {
                mOutgoingLocalVideoSurfaceView.setImage(frame, false);
            }

            public void onResumed(long timestamp) {

            }

            public void onPaused(long timestamp) {

            }
        };
        try {
            mOutgoingLocalVideoRenderer.addVideoSurfaceViewListener(mVideoSurfaceViewListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mCallScreenHost.onStateChange(Constants.SHARE_VIDEO_STATE_SHARING);
    }

    private void showSenderStoredView() {
        Logger.d(TAG, "showSenderStoredView entry");
        mOutgoingLocalVideoSurfaceView = new VideoSurfaceView(mContext);
        RelativeLayout.LayoutParams layoutParams =
                new RelativeLayout.LayoutParams(QVGA_WIDTH, QVGA_HEIGHT);
        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
        mOutgoingDisplayArea.setGravity(Gravity.CENTER);
        mOutgoingDisplayArea.addView(mOutgoingLocalVideoSurfaceView, layoutParams);
        try {
            mOutgoingLocalVideoRenderer =
                    mRichCallApi.createVideoRenderer(DEFAULT_VIEDO_FORMAT_STORED);
        } catch (ClientApiException e) {
            e.printStackTrace();
        }

        mVideoSurfaceViewListener = new IVideoSurfaceViewListener.Stub() {
            public void onSampleReceived(Bitmap frame, long duration) {
                mOutgoingLocalVideoSurfaceView.setImage(frame, true);
                Logger.d(TAG, "onSampleReceived(), progress = " + duration);
                if (mTimeBar != null) {
                    updatePlayingProgress(duration);
                } else {
                    Logger.w(TAG, "onSampleReceived(), mTimeBar is null!");
                }
            }

            public void onResumed(long timestamp) {

            }

            public void onPaused(long timestamp) {

            }
        };

        try {
            mOutgoingVideoPlayer.addVideoSurfaceViewListener(mVideoSurfaceViewListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        mCallScreenHost.onStateChange(Constants.SHARE_VIDEO_STATE_SHARING);
        Bitmap bitmap = loadThumbnail(mVideoFileName);
        if (bitmap != null) {
            Logger.w(TAG, "showSenderStoredView(), mOutgoingLocalVideoSurfaceView.isEnabled() = "
                    + mOutgoingLocalVideoSurfaceView.isEnabled());
            mOutgoingLocalVideoSurfaceView.setStoredVideoFrame(bitmap);
            mOutgoingLocalVideoSurfaceView.setImage(bitmap, true);
        } else {
            Logger
                    .w(TAG,
                            "showSenderStoredView(), get the first frame of the stored video failed!");
        }
        Logger.d(TAG, "showStoredVideoSharing exit");
    }

    // When first sender receive invitation
    private void updateFirstSenderView() {
        Logger.v(TAG, "updateFirstSenderView() entry, mOutgoingVideoFormat is: "
                + mOutgoingVideoFormat);
        removeOutgoingVideoSharingVews();
        mOutgoingRemoteVideoSurfaceView = new VideoSurfaceView(mContext);
        LayoutParams params =
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        mOutgoingRemoteVideoSurfaceView.setLayoutParams(params);
        try {
            mOutgoingRemoteVideoRenderer =
                    mRichCallApi
                            .createVideoRenderer(mOutgoingVideoFormat != null ? mOutgoingVideoFormat
                                    : DEFAULT_VIEDO_FORMAT);
        } catch (ClientApiException e) {
            e.printStackTrace();
        }
        mOutgoingRemoteVideoSurfaceViewOnReceiveDataListener = new OnReceiveDataListener() {
            @Override
            public void onFirstTimeRecevie() {
                Logger.v(TAG, "first sender onFirstTimeRecevie()");
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mVideoSharingDialogManager.dismissWaitingInitializeConextProgressDialog();
                    }
                });
            }
        };
        mOutgoingRemoteVideoSurfaceView
                .registerListener(mOutgoingRemoteVideoSurfaceViewOnReceiveDataListener);
        // Remote video view
        RelativeLayout.LayoutParams layoutParams =
                new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        mOutgoingDisplayArea.addView(mOutgoingRemoteVideoSurfaceView, layoutParams);
        Logger.v(TAG, "mOutgoingRemoteVideoSurfaceView = " + mOutgoingRemoteVideoSurfaceView
                + ", mOutgoingRemoteVideoSurfaceView.height = "
                + mOutgoingRemoteVideoSurfaceView.getHeight()
                + ", mOutgoingRemoteVideoSurfaceView.width = "
                + mOutgoingRemoteVideoSurfaceView.getWidth());
        // Local video view
        // mOutgoingLocalVideoSurfaceView.setAspectRatio(mVideoWidth,
        // mVideoHeight);
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        mOutgoingLocalVideoSurfaceView.setZOrderMediaOverlay(true);
        layoutParams =
                new RelativeLayout.LayoutParams(Utils.dip2px(mContext, MIN_WINDOW_WIDTH), Utils
                        .dip2px(mContext, MIN_WINDOW_HEIGHT));
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams.setMargins(LOCAL_CAMERA_MARGIN_LEFT, Utils.dip2px(mContext,
                LOCAL_CAMERA_MARGIN_TOP), Utils.dip2px(mContext, LOCAL_CAMERA_MARGIN_RIGHT),
                LOCAL_CAMERA_MARGIN_BOTTOM);
        mOutgoingDisplayArea.addView(mOutgoingLocalVideoSurfaceView, layoutParams);
        // Switch button
        layoutParams = new RelativeLayout.LayoutParams(SWITCH_BUTTON_WIDTH, SWITCH_BUTTON_HEIGHT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams.setMargins(SWITCH_BUTTON_MARGIN_LEFT, Utils.dip2px(mContext,
                SWITCH_BUTTON_MARGIN_TOP), Utils.dip2px(mContext, SWITCH_BUTTON_MARGIN_RIGHT),
                SWITCH_BUTTON_MARGIN_BOTTOM);
        mOutgoingDisplayArea.addView(mSwitchCamerImageView, layoutParams);
        mVideoSurfaceViewListener = new IVideoSurfaceViewListener.Stub() {
            public void onSampleReceived(Bitmap frame, long duration) {
                mOutgoingRemoteVideoSurfaceView.setImage(frame, false);
            }

            public void onResumed(long timestamp) {

            }

            public void onPaused(long timestamp) {

            }
        };
        try {
            mOutgoingRemoteVideoRenderer.addVideoSurfaceViewListener(mVideoSurfaceViewListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mVideoSharingState.set(Constants.SHARE_VIDEO_STATE_SHARING);
        mCallScreenHost.onStateChange(Constants.SHARE_VIDEO_STATE_SHARING);
    }

    private void initializeReceiverRemoteView() {
        Logger.v(TAG, "initializeReceiverRemoteView() entry, mIncomingVideoFormat: "
                + mIncomingVideoFormat + "mShareStatus: " + mShareStatus);
        removeIncomingVideoSharingRemoteVews();
        if (mIncomingDisplayArea != null) {
            try {
                mIncomingRemoteVideoRenderer =
                        mRichCallApi.createVideoRenderer(mIncomingVideoFormat != null ? mIncomingVideoFormat
                                        : DEFAULT_VIEDO_FORMAT);
            } catch (ClientApiException e) {
                e.printStackTrace();
            }

            mIncomingRemoteVideoSurfaceView = new VideoSurfaceView(mContext);
            mIncomingRemoteVideoSurfaceView.setVisibility(View.VISIBLE);
            LayoutParams params =
                    new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
            mIncomingRemoteVideoSurfaceView.setLayoutParams(params);
            mIncomingRemoteVideoSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
            mIncomingRemoteVideoSurfaceViewOnReceiveDataListener = new OnReceiveDataListener() {
                @Override
                public void onFirstTimeRecevie() {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Logger.d(TAG, "onFirstTimeRecevie(), mShareStatus: " + mShareStatus);
                            mVideoSharingDialogManager
                                    .dismissWaitingInitializeConextProgressDialog();
                            if ((mShareStatus == ShareStatus.STORED_IN)) {
                                showPlayingProgress(true);
                                mTimeBar.setShowScrubber(false);

                                if (mStoredVideoStatusView == null) {
                                    mStoredVideoStatusView = new StatusImageView(VideoSharingPlugin.this.mContext);
                                } else {
                                    Logger.w(TAG, "onFirstTimeRecevie(), mStoredVideoStatusView is not null!");
                                }
                                mStoredVideoStatusView.setStatus(STORED_VIDEO_STATUS_PLAY);
                                mStoredVideoStatusView.show();

                            } else {
                                Logger.d(TAG, "onFirstTimeRecevie(), is incoming live video share!");
                            }
                        }
                    });
                }
            };
            mIncomingRemoteVideoSurfaceView
                    .registerListener(mIncomingRemoteVideoSurfaceViewOnReceiveDataListener);
            mVideoSurfaceViewListener = new IVideoSurfaceViewListener.Stub() {
                public void onSampleReceived(Bitmap frame, long duration) {
                    Logger.d(TAG, "onSampleReceived(), received sample duration: " + duration);
                    boolean isStoredIn = (mShareStatus == ShareStatus.STORED_IN);
                    mIncomingRemoteVideoSurfaceView.setImage(frame, isStoredIn);
                    if (isStoredIn) {
                        if (mTimeBar != null) {
                            updatePlayingProgress(duration);
                        } else {
                            Logger.w(TAG, "onSampleReceived(), mTimeBar is null!");
                        }
                    } else {
                        Logger.d(TAG, "onSampleReceived(), is incoming live video share!");
                    }
                }

                public void onResumed(long timestamp) {
                    Logger.d(TAG, "onResumed(), timestamp = " + timestamp);
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mTimeBar != null) {
                                showReceivedStatus(STORED_VIDEO_STATUS_PLAY);
                            } else {
                                Logger.w(TAG, "onResumed(), mTimeBar is null!");
                            }
                        }
                    });
                }

                public void onPaused(long timestamp) {
                    Logger.d(TAG, "onPaused(), timestamp = " + timestamp);
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mTimeBar != null) {
                                showReceivedStatus(STORED_VIDEO_STATUS_PAUSE);
                            } else {
                                Logger.w(TAG, "onPaused(), mTimeBar is null!");
                            }
                        }
                    });
                }
            };
            try {
                mIncomingRemoteVideoRenderer.addVideoSurfaceViewListener(mVideoSurfaceViewListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Logger.w(TAG, "mIncomingDisplayArea is null");
        }
        if (mOutgoingDisplayArea != null) {
            mOutgoingDisplayArea.removeView(mOutgoingLocalVideoSurfaceView);
        } else {
            Logger.w(TAG, "mOutgoingDisplayArea is null");
        }
    }

    // When receive video share invitation
    private void showReceiverRemoteView() {
        Logger.v(TAG, "showReceiverRemoteView(), mShareStatus: " + mShareStatus);
        if (mIncomingDisplayArea != null) {
            RelativeLayout.LayoutParams layoutParams = null;
            if (!(mShareStatus == ShareStatus.STORED_IN)) {
                Logger.d(TAG, "showReceiverRemoteView, is live show!");
                layoutParams =
                        new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT);
            } else {
                Logger.d(TAG, "showReceiverRemoteView, is stored show!");
                layoutParams = new RelativeLayout.LayoutParams(QVGA_WIDTH, QVGA_HEIGHT);
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            }
            mIncomingDisplayArea.addView(mIncomingRemoteVideoSurfaceView, layoutParams);
            Logger.v(TAG, "mIncomingRemoteVideoSurfaceView = " + mIncomingRemoteVideoSurfaceView
                    + ", mIncomingRemoteVideoSurfaceView.height = "
                    + mIncomingRemoteVideoSurfaceView.getHeight()
                    + ", mIncomingRemoteVideoSurfaceView.width = "
                    + mIncomingRemoteVideoSurfaceView.getWidth());
            mVideoSharingState.set(Constants.SHARE_VIDEO_STATE_SHARING);
            mCallScreenHost.onStateChange(Constants.SHARE_VIDEO_STATE_SHARING);
        } else {
            Logger.w(TAG, "mIncomingDisplayArea is null");
        }

    }

    // When the receiver send invitation, this method will be called
    private void showReceiverLocalView() {
        Logger.v(TAG, "showReceiverLocalView() entry, mIncomingVideoFormat is: "
                + mIncomingVideoFormat);
        removeIncomingVideoSharingLocalVews();
        if (mIncomingDisplayArea != null) {
            try {
                mIncomingLocalVideoRenderer =
                        mRichCallApi
                                .createVideoRenderer(mIncomingVideoFormat != null ? mIncomingVideoFormat
                                        : DEFAULT_VIEDO_FORMAT);
            } catch (ClientApiException e) {
                e.printStackTrace();
            }
            mIncomingLocalVideoSurfaceView = new VideoSurfaceView(mContext);
            // mIncomingLocalVideoSurfaceView.setAspectRatio(mVideoWidth,
            // mVideoHeight);
            mSurfaceHolder = mIncomingLocalVideoSurfaceView.getHolder();
            mSurfaceHolder.addCallback(VideoSharingPlugin.this);
            mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
            mIncomingLocalVideoSurfaceView.setZOrderMediaOverlay(true);

            RelativeLayout.LayoutParams layoutParams = null;
            // Local video view
            layoutParams =
                    new RelativeLayout.LayoutParams(Utils.dip2px(mContext, MIN_WINDOW_WIDTH), Utils
                            .dip2px(mContext, MIN_WINDOW_HEIGHT));
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            layoutParams.setMargins(LOCAL_CAMERA_MARGIN_LEFT, Utils.dip2px(mContext,
                    LOCAL_CAMERA_MARGIN_TOP), Utils.dip2px(mContext, LOCAL_CAMERA_MARGIN_RIGHT),
                    LOCAL_CAMERA_MARGIN_BOTTOM);
            mIncomingDisplayArea.addView(mIncomingLocalVideoSurfaceView, layoutParams);
            // switch button
            layoutParams =
                    new RelativeLayout.LayoutParams(SWITCH_BUTTON_WIDTH, SWITCH_BUTTON_HEIGHT);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            layoutParams.setMargins(SWITCH_BUTTON_MARGIN_LEFT, Utils.dip2px(mContext,
                    SWITCH_BUTTON_MARGIN_TOP), Utils.dip2px(mContext, SWITCH_BUTTON_MARGIN_RIGHT),
                    SWITCH_BUTTON_MARGIN_BOTTOM);
            mIncomingDisplayArea.addView(mSwitchCamerImageView, layoutParams);
            mVideoSurfaceViewListener = new IVideoSurfaceViewListener.Stub() {
                public void onSampleReceived(Bitmap frame, long duration) {
                    mIncomingLocalVideoSurfaceView.setImage(frame, (mShareStatus == ShareStatus.STORED_IN));
                }

                public void onResumed(long timestamp) {

                }

                public void onPaused(long timestamp) {

                }
            };
            try {
                mIncomingLocalVideoRenderer.addVideoSurfaceViewListener(mVideoSurfaceViewListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mVideoSharingState.set(Constants.SHARE_VIDEO_STATE_SHARING);
            mCallScreenHost.onStateChange(Constants.SHARE_VIDEO_STATE_SHARING);
        } else {
            Logger.w(TAG, "mIncomingDisplayArea is null");
        }
        if (mOutgoingDisplayArea != null) {
            mOutgoingDisplayArea.removeView(mOutgoingLocalVideoSurfaceView);
        } else {
            Logger.w(TAG, "mOutgoingDisplayArea is null");
        }
    }

    private void showRemoteAcceptMessage() {
        Logger.v(TAG, "showRemoteAcceptMessage()");
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                String message = mContext.getResources().getString(R.string.remote_has_accepted);
                showToast(message);
                if (mShareStatus == ShareStatus.STORED_IN || mShareStatus == ShareStatus.STORED_OUT) {
                    showControllButton(CONTROLL_START, true);
                    showPlayingProgress(true);
                } else {
                    Logger.d(TAG, "showRemoteAcceptMessage(), is live video share.");
                }
            }
        });
    }

    private void updatePlayingProgress(final long currentDuration) {
        Logger.v(TAG, "updatePlayingProgress(), currentDuration = " + currentDuration);
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mTimeBar != null) {
                    mTimeBar.setTime((int) currentDuration, (int) mVideoDuration);
                }
            }
        });
    }

    private void showWaitRemoteAcceptMessage() {
        Logger.v(TAG, "showWaitRemoteAcceptMessage()");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String message = mContext.getResources().getString(R.string.wait_remote_to_accept);
                showToast(message);
            }
        };
        mMainHandler.postDelayed(runnable, DELAY_TIME);
    }

    private void resetAtomicBolean() {
        Logger.v(TAG, "resetAtomicBolean()");
        mIsVideoSharingSender.set(false);
        mIsVideoSharingReceiver.set(false);
        mIsStarted.set(false);
    }

    /**
     * Release resource. Please call me at any thread including UI thread
     */
    private void destroy() {
        Logger.v(TAG, "destroy() entry");
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Logger.v(TAG, "destroy in a background thread.");
                resetAtomicBolean();
                // fix NE
                stopCameraPreview();
                try {
                    if (mOutgoingVideoPlayer != null) {
                        mOutgoingVideoPlayer.stop();
                        mOutgoingVideoPlayer = null;
                    }
                } catch (RemoteException e) {
                    Logger.w(TAG, "destroy() RemoteException:" + e.getMessage());
                    e.printStackTrace();
                }
                cancelSession();
                removeSurfaceViewListener();
                return null;
            }
            
        }.execute();
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                removeIncomingVideoSharingVews();
                removeOutgoingVideoSharingVews();
                dismissAllDialogs();
                if (mSurfaceHolder != null) {
                    mSurfaceHolder.removeCallback(VideoSharingPlugin.this);
                } else {
                    Logger.w(TAG, "mSurfaceHolder is null");
                }
            }
        });
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
            Logger.v(TAG, "when destroy, release the wake lock");
        }
        resetVideoShaingState();
        Logger.d(TAG, "destroy() exit");
    }

    private boolean cancelSession() {
        // Should make sure cancelSession run a different thread with
        // RemoteCallback
        Logger.d(TAG, "cancelSession()");
        if (mOutgoingVideoSharingSession != null) {
            try {
                mOutgoingVideoSharingSession.removeSessionListener(mOutgoingSessionEventListener);
                mOutgoingVideoSharingSession.cancelSession();
            } catch (RemoteException e) {
                e.printStackTrace();
            } finally {
                mOutgoingVideoSharingSession = null;
            }
        }
        if (mIncomingVideoSharingSession != null) {
            try {
                mIncomingVideoSharingSession.removeSessionListener(mIncomingSessionEventListener);
                mIncomingVideoSharingSession.cancelSession();
            } catch (RemoteException e) {
                e.printStackTrace();
            } finally {
                mIncomingVideoSharingSession = null;
            }
        }
        return true;
    }

    private boolean removeSurfaceViewListener() {
        if (mIncomingRemoteVideoSurfaceView != null) {
            mIncomingRemoteVideoSurfaceView.unregisterListener();
            mIncomingRemoteVideoSurfaceViewOnReceiveDataListener = null;
        }
        if (mOutgoingRemoteVideoSurfaceView != null) {
            mOutgoingRemoteVideoSurfaceView.unregisterListener();
            mOutgoingRemoteVideoSurfaceViewOnReceiveDataListener = null;
        }
        return true;
    }

    private void destroyIncomingSessionOnly() {
        Logger.v(TAG, "destroyIncomingSession()");
        if (mIncomingVideoSharingSession != null) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Logger.v(TAG, "reject session in backgroun thread.");
                        mIncomingVideoSharingSession
                                .removeSessionListener(mIncomingSessionEventListener);
                        mIncomingVideoSharingSession.rejectSession();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    } finally {
                        mIncomingVideoSharingSession = null;
                        mIsVideoSharingReceiver.set(false);
                    }
                }
            });
        }
    }

    // Sometime a receiver send a viedo sharing invitation may be failed.
    // This method run on ui thread, so you can call it in any thread
    private void destroyOutgoingViewOnly() {
        Logger.v(TAG, "destroyOutgoingViewOnly()");
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mIncomingDisplayArea != null) {
                    mIncomingDisplayArea.removeView(mIncomingLocalVideoSurfaceView);
                    mIncomingDisplayArea.removeView(mSwitchCamerImageView);
                } else {
                    Logger.w(TAG, "mIncomingDisplayArea is null.");
                }
            }
        });
    }

    private void resetVideoShaingState() {
        Logger.v(TAG, "resetVideoShaingState");
        mVideoSharingState.set(Constants.SHARE_VIDEO_STATE_IDLE);
        Utils.setInVideoSharing(false);
        mShareStatus = ShareStatus.UNKNOWN;
        if (mCallScreenHost != null) {
            mCallScreenHost.onStateChange(Constants.SHARE_VIDEO_STATE_IDLE);
        } else {
            Logger.w(TAG, "mCallScreenHost is null");
        }
    }

    private void switchCamera() {
        Logger.v(TAG, "switchCamera");
        if (mCamerNumber == SINGLE_CAMERA) {
            Logger.w(TAG, "The device only has one camera, so can not switch");
            return;
        }
        // release camera
        stopCameraPreview();
        // open the other camera
        if (mOpenedCameraId == CameraInfo.CAMERA_FACING_BACK) {
            mCamera = Camera.open(CameraInfo.CAMERA_FACING_FRONT);
            mOpenedCameraId = CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCamera = Camera.open(CameraInfo.CAMERA_FACING_BACK);
            mOpenedCameraId = CameraInfo.CAMERA_FACING_BACK;
        }
        // restart the preview
        startCameraPreview();
        addPreviewCallback();
    }

    private void openDefaultCamera() {
        Logger.v(TAG, "openDefaultCamera");
        mCamera = Camera.open();
        mOpenedCameraId = CameraInfo.CAMERA_FACING_BACK;
    }

    /**
     * Video sharing invitation receiver.
     */
    private class VideoSharingInvitationReceiver extends BroadcastReceiver {
        private static final String TAG = "VideoSharingInvitationReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.v(TAG, "onReceive(), context = " + context + ", intent = " + intent);
            if (intent != null) {
                String action = intent.getAction();
                Logger.v(TAG, "action = " + action);
                if (VIDEO_SHARING_INVITATION_ACTION.equals(action)) {
                    initAudioButton();
                    handleVideoSharingInvitation(context, intent);
                } else if (VIDEO_SHARING_START_ACTION.equals(action)) {
                    mShareStatus = ShareStatus.STORED_OUT;
                    mVideoFileName = intent.getStringExtra(VIDEO_NAME);
                    mVideoDuration = intent.getLongExtra(VIDEO_DURATION, 0);
                    Logger.d(TAG, "onReceive(), mVideoFileName: " + mVideoFileName);
                    initAudioButton();
                    startVideoShare();
                } else if (intent.ACTION_HEADSET_PLUG.equals(action)) {
                    mHeadsetConnected = (intent.getIntExtra("state", 0) == 1);
                    Logger.d(TAG, "onReceive() ACTION_HEADSET_PLUG: " + mHeadsetConnected);
                } else if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                    int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,
                            BluetoothHeadset.STATE_DISCONNECTED);
                    Logger.d(TAG, "onReceive() ACTION_CONNECTION_STATE_CHANGED: " + state);
                } else {
                    Logger.w(TAG, "Unknown action");
                }
            } else {
                Logger.w(TAG, "intent is null");
            }
        }
    }// end VideoSharingInvitationReceiver

    private void handleVideoSharingInvitation(Context context, Intent intent) {
        Logger.v(TAG, "handleVideoSharingInvitation()");
        // Display invitation dialog
        mIncomingSessionId = intent.getStringExtra(RichcallProxyActivity.SESSION_ID);
        mIncomingVideoFormat = intent.getStringExtra(RichcallProxyActivity.VIDEO_TYPE);
        mVideoDuration = intent.getLongExtra("videosize", -1);
        String mediaType = intent.getStringExtra(RichcallProxyActivity.MEDIA_TYPE);
        // Video share is of full status.
        boolean canShare = (mShareStatus == ShareStatus.UNKNOWN || mShareStatus == ShareStatus.LIVE_OUT);
        boolean supported = getCapability(mNumber);
        Logger.d(TAG, "handleVideoSharingInvitation() mShareStatus: " + mShareStatus
                + " supported: " + supported);
        if (!canShare || !supported) {
            try {
                IVideoSharingSession sharingSession = mRichCallApi
                        .getVideoSharingSession(mIncomingSessionId);
                if (sharingSession != null) {
                    try {
                        sharingSession.rejectSession();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            } catch (ClientApiException e) {
                e.printStackTrace();
            }
            return;
        }
        if (VIDEO_LIVE.equals(mediaType)) {
            if (mShareStatus == ShareStatus.LIVE_OUT) {
                if (is3GMobileNetwork() && mOutgoingVideoSharingSession != null) {
                    try {
                        if (mOutgoingVideoSharingSession.getSessionState() == SessionState.ESTABLISHED
                                || mOutgoingVideoSharingSession.getSessionState() == SessionState.PENDING) {
                            Logger.d(TAG,"Reject the incoming session because the device " +
                                    "is upder 3G network and the outgoing video share " +
                                    "session is established or on pending");
                            try {
                                IVideoSharingSession sharingSession = mRichCallApi
                                        .getVideoSharingSession(mIncomingSessionId);
                                if (sharingSession != null) {
                                    try {
                                        sharingSession.rejectSession();
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } catch (ClientApiException e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                    } catch (RemoteException e) {
                        Logger.e(TAG, e.toString());
                    }
                } else {
                    mShareStatus = ShareStatus.LIVE_TWOWAY;
                }
            } else {
                mShareStatus = ShareStatus.LIVE_IN;
            }
        } else {
            mShareStatus = ShareStatus.STORED_IN;
        }
        // Tell that it is vs receiver if recevie a vs invitation before
        // start vs
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
            Logger.v(TAG, "when start, acquire a wake lock");
        } else {
            Logger.v(TAG, "when start, the wake lock has been acquired, so do not acquire");
        }
        if (!mIsVideoSharingSender.get()) {
            mIsVideoSharingReceiver.set(true);
        }
        if (mCallScreenHost != null) {
            mIncomingDisplayArea = mCallScreenHost.requestAreaForDisplay();
        } else {
            Logger.w(TAG, "mCallScreenHost is null");
        }
        // Set application context
        AndroidFactory.setApplicationContext(context);
        if (mRichCallStatus == RichCallStatus.CONNECTED) {
            // Initialize a vs session
            mGetIncomingSessionWhenApiConnected.set(false);
            getIncomingVideoSharingSession();
        } else {
            Logger.v(TAG, "Richcall api not connected, and then connect api");
            mGetIncomingSessionWhenApiConnected.set(true);
            mRichCallApi.connectApi();
        }
    }
    
    protected boolean is3GMobileNetwork() {
        int networkType = Utils.getNetworkType(mContext);
        boolean is3G = false;
        if (networkType == Utils.NETWORK_TYPE_UMTS || networkType == Utils.NETWORK_TYPE_EDGE
                || networkType == Utils.NETWORK_TYPE_HSPA
                || networkType == Utils.NETWORK_TYPE_HSUPA
                || networkType == Utils.NETWORK_TYPE_HSDPA
                || networkType == Utils.NETWORK_TYPE_1XRTT
                || networkType == Utils.NETWORK_TYPE_EHRPD) {
            is3G = true;
        }
        return is3G;
    }

    private void handleUserAcceptVideoSharing() {
        Logger.v(TAG, "handleUserAcceptVideoSharing()");
        if (mIncomingVideoSharingSession != null) {
            Utils.setInVideoSharing(true);
            if (mIsVideoSharingReceiver.get()) {
                // First receive vs invitation and accept
                Logger.v(TAG, "First receive vs invitation and accept");
                handleFirstReceiveInvitation();
            } else if (mIsVideoSharingSender.get()) {
                // Receive vs invitation after invitation other
                Logger.v(TAG, "Receive vs invitation after invitation other");
                handleSecondReceiveInvitation();
            }
        } else {
            Logger.w(TAG, "mIncomingVideoSharingSession is null");
        }
    }

    private void handleUserDeclineVideoSharing() {
        Logger.v(TAG, "handleUserDeclineVideoSharing() mShareStatus: " + mShareStatus);
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
            Logger.v(TAG, "handleUserDeclineVideoSharing() release wake lock");
        } else {
            Logger.v(TAG, "handleUserDeclineVideoSharing() no need to release wake lock");
        }
        // status here can be: LIVE_TWOWAY, LIVE_IN, STORED_IN.
        if (mShareStatus == ShareStatus.LIVE_TWOWAY) {
            mShareStatus = ShareStatus.LIVE_OUT;
        } else {
            mShareStatus = ShareStatus.UNKNOWN;
        }
        if (mIncomingVideoSharingSession != null) {
            // In such case, only need remove incoming ui
            destroyIncomingSessionOnly();
        } else {
            Logger.w(TAG, "vmIncomingVideoSharingSession is null");
        }
    }

    // Receive invitaiton before send invitation
    private void handleFirstReceiveInvitation() {
        Logger.v(TAG, "handleFirstReceiveInvitation");
        try {
            initializeReceiverRemoteView();
            mIncomingVideoSharingSession.setMediaRenderer(mIncomingRemoteVideoRenderer);
            mIncomingVideoSharingSession.acceptSession();
            mVideoSharingDialogManager.showWaitingInitializeConextProgressDialog();
            showReceiverRemoteView();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // Receive invitation after send invitation
    private void handleSecondReceiveInvitation() {
        Logger.v(TAG, "handleSecondReceiveInvitation");
        try {
            Logger.v(TAG, "Receive invitation after send invitation");
            mVideoSharingDialogManager.showWaitingInitializeConextProgressDialog();
            updateFirstSenderView();
            mIncomingVideoSharingSession.setMediaRenderer(mOutgoingRemoteVideoRenderer);
            mIncomingVideoSharingSession.acceptSession();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Logger.v(TAG, "onClick(), which = " + which);
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                handleUserAcceptVideoSharing();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                handleUserDeclineVideoSharing();
                break;
            default:
                Logger.v(TAG, "Unknown option");
                break;
        }
    }

    private void getIncomingVideoSharingSession() {
        Logger.v(TAG, "getIncomingVideoSharingSession()");
        try {
            mIncomingVideoSharingSession = mRichCallApi.getVideoSharingSession(mIncomingSessionId);
            if (mIncomingVideoSharingSession != null) {
                mIncomingVideoSharingSession.addSessionListener(mIncomingSessionEventListener);
            } else {
                Logger.w(TAG, "mIncomingVideoSharingSession is null");
            }
        } catch (ClientApiException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mVideoSharingDialogManager.showInvitationDialog();
    }

    /**
     * Wait to receive live video from remote Progress dialog
     */
    public static class WaitingProgressDialog extends DialogFragment {
        public static final String TAG = "WaitingProgressDialog";
        private static Context sContext = null;
        private static Activity sActivity = null;

        public static WaitingProgressDialog newInstance(Context context, Activity activity) {
            WaitingProgressDialog f = new WaitingProgressDialog();
            sContext = context;
            sActivity = activity;
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Logger.v(TAG, "onCreateDialog()");
            ProgressDialog dialog = new ProgressDialog(sActivity);
            dialog.setIndeterminate(true);
            dialog.setMessage(sContext.getString(R.string.wait_for_video));
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
        }
    }

    private class VideoSharingDialogManager {
        private static final String TAG = "VideoSharingDialogManager";
        private static final int CAMERA_POSITION = 0;
        private static final int GALLERY_POSITION = 1;

        private DialogInterface.OnClickListener mOnClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Logger.v(TAG, "onClick(), which = " + which);
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Logger.v(TAG, "onClick() stop video sharing");
                        destroy();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        Logger.v(TAG, "onClick() Do nothing");
                        break;
                    default:
                        Logger.v(TAG, "onClick() Unknown option");
                        break;
                }
            }
        };

        private class SelectionItem {
            public String text;
        }

        public void showTerminatedByRemoteDialog() {
            Logger.v(TAG, "showTerminatedByRemoteDialog() entry");
            destroy();
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    dismissAllDialogs();
                    String msg = mContext.getString(R.string.remote_terminated);
                    Logger.v(TAG, "showTerminatedByRemoteDialog msg = " + msg);
                    createAndShowAlertDialog(msg, null);
                }
            });
            Logger.v(TAG, "showTerminatedByRemoteDialog() exit");
        }

        private void showTerminatedByNetworkDialog() {
            Logger.d(TAG, "showTerminatedByNetworkDialog() entry");
            destroy();
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    dismissAllDialogs();
                    String msg =
                            mContext.getString(R.string.video_sharing_terminated_due_to_network);
                    Logger.v(TAG, "showTerminatedByNetworkDialog msg = " + msg);
                    createAndShowAlertDialog(msg, mOnClickListener);
                }
            });
            Logger.d(TAG, "showTerminatedByNetworkDialog() exit");
        }

        public void showWaitingInitializeConextProgressDialog() {
            Logger.d(TAG, "showWaitingInitializeConextProgressDialog() entry");
            if (mCallScreenHost != null) {
                dismissAllDialogs();
                Activity activity = mCallScreenHost.getCallScreenActivity();
                mWaitingProgressDialog =
                        WaitingProgressDialog.newInstance(mContext, mCallScreenHost
                                .getCallScreenActivity());
                mWaitingProgressDialog.show(activity.getFragmentManager(), TAG);
            } else {
                Logger.d(TAG, "mCallScreenHost is null");
            }
            Logger.d(TAG, "showWaitingInitializeConextProgressDialog() exit");
        }

        public void dismissWaitingInitializeConextProgressDialog() {
            Logger.d(TAG, "dismissWaitingInitializeConextProgressDialog");
            if (mWaitingProgressDialog != null) {
                mWaitingProgressDialog.dismiss();
            } else {
                Logger.d(TAG, "mWaitingProgressDialog is null");
            }
        }

        public void showRejectedByRemoteDialog() {
            Logger.v(TAG, "showRejectedByRemoteDialog() mShareStatus: " + mShareStatus);
            if (mIsVideoSharingSender.get()) {
                Logger.v(TAG, "showRejectedByRemoteDialog(), sender is rejected by remote");
                destroy();
            } else {
                Logger.v(TAG, "showRejectedByRemoteDialog(), receiver is rejected by remote");
                // share status here can be: LIVE_TWOWAY LIVE_OUT STORED_OUT
                if (mShareStatus == ShareStatus.LIVE_TWOWAY) {
                    mShareStatus = ShareStatus.LIVE_IN;
                } else {
                    mShareStatus = ShareStatus.UNKNOWN;
                }
                mIsStarted.set(false);
            }
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    dismissAllDialogs();
                    String msg = mContext.getString(R.string.remote_reject);
                    Logger.v(TAG, "showRejectedByRemoteDialog msg = " + msg);
                    createAndShowAlertDialog(msg, mRejectDialogListener);
                }
            });
        }

        DialogInterface.OnClickListener mRejectDialogListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Logger.v(TAG, "onClick(), which = " + which);
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Logger.v(TAG, "remvoe local video view");
                        removeIncomingVideoSharingLocalVews();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        Logger.v(TAG, "Do nothing");
                        break;
                    default:
                        Logger.v(TAG, "Unknown option");
                        break;
                }
            }
        };

        public void showTimeOutDialog() {
            Logger.d(TAG, "showTimeOutDialog entry");
            dismissAllDialogs();
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Logger.d(TAG, "showTimeOutDialog() create time out dialog!");
                    String msg = mContext.getString(R.string.video_sharing_invitation_time_out);
                    createAndShowAlertDialog(msg, mOnClickListener);
                }
            });
            Logger.d(TAG, "showTimeOutDialog exit");
        }

        public void showSelectVideoDialog() {
            final List<SelectionItem> list = new ArrayList<SelectionItem>();
            // Enter here only when status is LIVE_IN or UNKNOWN.
            Logger.d(TAG, "showSelectVideoDialog() mShareStatus: " + mShareStatus);
            SelectionItem cameraItem = new SelectionItem();
            cameraItem.text = mContext.getString(R.string.camera_item);
            list.add(cameraItem);
            if (mShareStatus == ShareStatus.UNKNOWN) {
                SelectionItem galleryItem = new SelectionItem();
                galleryItem.text = mContext.getString(R.string.gallery_item);
                list.add(galleryItem);
            }
            if (mCallScreenHost != null) {
                final Context context = mCallScreenHost.getCallScreenActivity();
                if (context != null) {
                    Logger.d(TAG, "showSelectVideoDialog() getCallScreenActivity is not null");
                    final LayoutInflater dialogInflater = (LayoutInflater) context
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    final ArrayAdapter<SelectionItem> adapter = new ArrayAdapter<SelectionItem>(context,
                            R.layout.image_selection_layout, list) {
                        @Override
                        public View getView(int position, final View convertView, ViewGroup parent) {
                            Logger.d(TAG, "getView(), convertView = " + convertView);
                            View itemView = convertView;
                            if (itemView == null) {
                                itemView = dialogInflater.inflate(mContext.getResources().getLayout(
                                        R.layout.image_selection_layout), parent, false);
                            }
                            final TextView text = (TextView) itemView.findViewById(R.id.item_text);
                            SelectionItem item = getItem(position);
                            text.setText(item.text);
                            return itemView;
                        }
                    };
                    if (mSelectionDialog == null) {
                        mSelectionDialog = new CallScreenDialog(context);
                    }
                    mSelectionDialog.setTitle(mContext.getString(R.string.share_image));
                    mSelectionDialog.setSingleChoiceItems(adapter, 0, mSingleChoiceListener);
                    mSelectionDialog.setCancelable(true);
                    mSelectionDialog.show();
                    mCallScreenDialogSet.add(mSelectionDialog);
                }
            }
        }
        
        CallScreenDialog mSelectionDialog;

        DialogInterface.OnClickListener mSingleChoiceListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (CAMERA_POSITION == which) {
                    if (mShareStatus == ShareStatus.LIVE_IN) {
                        mShareStatus = ShareStatus.LIVE_TWOWAY;
                    } else {
                        mShareStatus = ShareStatus.LIVE_OUT;
                    }
                    startVideoShare();
                    dialog.dismiss();
                } else if (GALLERY_POSITION == which) {
                    startVideoSharePickDialog();
                    dialog.dismiss();
                } else {
                    dialog.dismiss();
                }
                mCallScreenDialogSet.remove(mSelectionDialog);
                mSelectionDialog = null;
            }
        };

        private CallScreenDialog createAndShowAlertDialog(String msg,
                DialogInterface.OnClickListener posiviteListener) {
            Logger.v(TAG, "createAlertDialog(), msg = " + msg);
            if (mCallScreenHost != null && mCallScreenHost.getCallScreenActivity() != null) {
                Logger.v(TAG, "createAndShowAlertDialog(), call screen host entry!");
                Activity activity = mCallScreenHost.getCallScreenActivity();
                final CallScreenDialog callScreenDialog = new CallScreenDialog(activity);
                callScreenDialog.setIcon(android.R.attr.alertDialogIcon);
                callScreenDialog.setTitle(mContext.getString(R.string.attention_title));
                callScreenDialog.setMessage(msg);
                callScreenDialog.setPositiveButton(mContext
                        .getString(R.string.rcs_dialog_positive_button), posiviteListener);
                callScreenDialog.show();
                mCallScreenDialogSet.add(callScreenDialog);
                return callScreenDialog;
            } else {
                Logger.w(TAG, "mCallScreenHost is null or activity is null.");
                return null;
            }
        }
        /**
         * Show the video sharing invitation dialog.
         * 
         * @return True if show dialog successfully, otherwise return false.
         */
        private boolean showInvitationDialog() {
            Logger.v(TAG, "showInvitationDialog()");
            dismissAllDialogs();
            // Vibrate
            Utils.vibrate(mContext, Utils.MIN_VIBRATING_TIME);
            CallScreenDialog callScreenDialog = createInvitationDialog();
            if (callScreenDialog != null) {
                callScreenDialog.show();
                return true;
            }
            return false;
        }

        /**
         * Create a video sharing invitation dialog, and add it to the set
         * 
         * @return The dialog created.
         */
        private CallScreenDialog createInvitationDialog() {
            Logger.v(TAG, "createInvitationDialog()");
            CallScreenDialog callScreenDialog = null;
            if (mCallScreenHost != null) {
                callScreenDialog = new CallScreenDialog(mCallScreenHost.getCallScreenActivity());
                callScreenDialog.setPositiveButton(
                        mContext.getString(R.string.rcs_dialog_positive_button),
                        VideoSharingPlugin.this);
                callScreenDialog.setNegativeButton(
                        mContext.getString(R.string.rcs_dialog_negative_button),
                        VideoSharingPlugin.this);
                callScreenDialog.setTitle(mContext
                        .getString(R.string.video_sharing_invitation_dialog_title));
                callScreenDialog.setMessage(mContext
                        .getString(R.string.video_sharing_invitation_dialog_content));
                callScreenDialog.setCancelable(false);
                mCallScreenDialogSet.add(callScreenDialog);
            }
            Logger.d(TAG, "createInvitationDialog() exit, callScreenDialog = " + callScreenDialog);
            return callScreenDialog;
        }
    }

    private Bitmap loadThumbnail(String videoName) {
        Logger.d(TAG, "loadThumbnail(), videoName is " + videoName);
        if (videoName == null) {
            return null;
        }
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(videoName);
        bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_NEXT_SYNC);
        return bitmap;
    }

    private void showControllButton(int controllType, boolean isShow) {
        if (mControllButton == null) {
            mControllButton = new ControllButton(mContext);
        } else {
            Logger.d(TAG, "showControllButton(), mControllButton is not null!");
        }
        if (isShow) {
            if (controllType == CONTROLL_START) {
                mControllButton.showStartButton();
            } else if (controllType == CONTROLL_STOP) {
                mControllButton.showStopButton();
            }
        } else {
            mControllButton.hideButton();
        }
    }

    private void showPlayingProgress(boolean isShow) {
        if (isShow) {
            if (mTimeBar == null) {
                mTimeBar = new ControllProgress(mContext, this);
            } else {
                mTimeBar.show();
                Logger.d(TAG, "showPlayingProgress(), mControllButton is not null!");
            }
        } else {
            if (mTimeBar != null) {
                mTimeBar.hide();
            } else {
                Logger.d(TAG, "showPlayingProgress(), mControllButton is null!");
            }
        }
    }

    private class ControllButton extends ImageView {

        private boolean mIsStart;
        private OnClickListener mListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.d(TAG, "ControllButton:onClick() mIsStart is " + mIsStart);
                try {
                    if (mIsStart) {
                        mOutgoingVideoPlayer.pause();
                        showStartButton();
                    } else {
                        mOutgoingVideoPlayer.resume();
                        showStopButton();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        };

        public ControllButton(Context context) {
            super(context);
            setOnClickListener(mListener);
            super.setImageResource(R.drawable.ic_vidcontrol_play);
            showButton();
        }

        public void showStartButton() {
            Logger.d(TAG, "ControllButton:showStartButton() entry!");
            super.setImageResource(R.drawable.ic_vidcontrol_play);
            mIsStart = false;
            invalidate();
        }

        public void showStopButton() {
            Logger.d(TAG, "ControllButton:showStopButton() entry!");
            super.setImageResource(R.drawable.ic_vidcontrol_pause);
            mIsStart = true;
            invalidate();
        }

        public void hideButton() {
            Logger.d(TAG, "ControllButton:hideButton() entry!");
            if (mOutgoingDisplayArea != null) {
                mOutgoingDisplayArea.removeView(this);
            } else {
                Logger.e(TAG, "hideButton(), mOutgoingDisplayArea is null!");
            }
        }

        private void showButton() {
            Logger.d(TAG, "ControllButton:showButton() entry!");
            super.setBackgroundResource(R.drawable.bg_vidcontrol);
            super.setScaleType(ScaleType.CENTER);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            if (mOutgoingDisplayArea != null) {
                mOutgoingDisplayArea.addView(this, params);
            } else {
                Logger.e(TAG, "mOutgoingDisplayArea is null!");
            }
        }
    }
    
    private void showReceivedStatus(int status) {
        Logger.d(TAG, "showReceivedStatus() status = " + status);
        if (mStoredVideoStatusView == null) {
            mStoredVideoStatusView = new StatusImageView(VideoSharingPlugin.this.mContext);
        } else {
            Logger.d(TAG, "showReceivedStatus(), mStoredVideoStatusView is not null!");
        }
        mStoredVideoStatusView.setStatus(status);
        mStoredVideoStatusView.show();
    }

    private class ControllProgress extends TimeBar {

        private boolean mIsShown = false;

        public ControllProgress(Context context, Listener listener) {
            super(context, listener);
            setScrubbing(true);
            show();
        }

        public void hide() {
            Logger.d(TAG, "ControllProgress:hide() entry!");
            if (mIsShown) {
                if (mIsVideoSharingSender.get()) {
                    if (mOutgoingDisplayArea != null) {
                        mOutgoingDisplayArea.removeView(this);
                    } else {
                        Logger.e(TAG, "hide() mOutgoingDisplayArea is null!");
                    }
                } else {
                    if (mIncomingDisplayArea != null) {
                        mIncomingDisplayArea.removeView(this);
                    } else {
                        Logger.e(TAG, "hide() mIncomingDisplayArea is null!");
                    }
                }
                mIsShown = false;
            } else {
                Logger.d(TAG, "hide() no need to hide!");
            }
        }

        private void show() {
            Logger.d(TAG, "ControllProgress:show() entry!");
            if (!mIsShown) {
                RelativeLayout.LayoutParams params =
                        new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.leftMargin = getResources().getDimensionPixelOffset(R.dimen.control_progress_margin);
                params.rightMargin = getResources().getDimensionPixelOffset(R.dimen.control_progress_margin);
                if (mIsVideoSharingSender.get()) {
                    if (mOutgoingDisplayArea != null) {
                        mOutgoingDisplayArea.addView(this, params);
                    } else {
                        Logger.e(TAG, "show(), mOutgoingDisplayArea is null!");
                    }
                } else {
                    if (mIncomingDisplayArea != null) {
                        mIncomingDisplayArea.addView(this, params);
                    } else {
                        Logger.e(TAG, "show(), mOutgoingDisplayArea is null!");
                    }
                }
                mIsShown = true;
            } else {
                Logger.d(TAG, "show() no need to show!");
            }
        }
    }
    
    private class StatusImageView extends ImageView {
        
        private int mStatus = STORED_VIDEO_STATUS_PLAY;
        private boolean mIsShown = false;

        public StatusImageView(Context context) {
            super(context);
        }
        
        private void setStatus(int status) {
            Logger.d(TAG, "setStatus() status = " + status);
            mStatus = status;
        }
        
        private void show() {
            Logger.d(TAG, "show() status = " + mStatus);
            if (!mIsShown) {
                RelativeLayout.LayoutParams playingStatus =
                        new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                playingStatus.leftMargin = getResources().getDimensionPixelOffset(R.dimen.stored_video_margin_left);
                playingStatus.topMargin = getResources().getDimensionPixelOffset(R.dimen.stored_video_margin_top);
                
                if (mStatus == STORED_VIDEO_STATUS_PLAY) {
                    mStoredVideoStatusView.setImageResource(R.drawable.stored_video_status_play);
                } else {
                    mStoredVideoStatusView.setImageResource(R.drawable.stored_video_status_pause);
                }
                if (mIncomingDisplayArea != null) {
                    mIncomingDisplayArea.addView(mStoredVideoStatusView, playingStatus);
                } else {
                    Logger.e(TAG, "show(), mIncomingDisplayArea is null!");
                }
                mIsShown = true;
            } else {
                if (mStatus == STORED_VIDEO_STATUS_PLAY) {
                    mStoredVideoStatusView.setImageResource(R.drawable.stored_video_status_play);
                } else {
                    mStoredVideoStatusView.setImageResource(R.drawable.stored_video_status_pause);
                }
                mStoredVideoStatusView.refreshDrawableState();
                Logger.d(TAG, "show() no need to show!");
            }
        }
        
        private void hide() {
            Logger.d(TAG, "hide() status = " + mStatus);
            if (mIsShown) {
                if (mIncomingDisplayArea != null) {
                    mIncomingDisplayArea.removeView(this);
                } else {
                    Logger.e(TAG, "hide(), mOutgoingDisplayArea is null!");
                }
                mIsShown = false;
            } else {
                Logger.d(TAG, "hide() no need to hide!");
            }
        }

    }

    @Override
    public void onScrubbingEnd(int time) {
        seekToStoredVideo(time);
    }

    @Override
    public void onScrubbingMove(int time) {
        seekToStoredVideo(time);
    }

    @Override
    public void onScrubbingStart() {
        // TODO Auto-generated method stub

    }

    private void seekToStoredVideo(int time) {
        Logger.d(TAG, "seekToStoredVideo() entry");
        if (mIsVideoSharingSender.get()) {
            if (mOutgoingVideoPlayer != null) {
                try {
                    mOutgoingVideoPlayer.seekTo(time);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                Logger.w(TAG, "seekToStoredVideo(), mOutgoingVideoPlayer is null!");
            }
        } else {
            Logger.d(TAG, "seekToStoredVideo() is receiver!");
        }
    }

    /**
     * Share status.
     */
    private static final class ShareStatus {
        public static final int UNKNOWN = 0;
        public static final int LIVE_OUT = 1;
        public static final int LIVE_IN = 2;
        public static final int LIVE_TWOWAY = 3;
        public static final int STORED_IN = 4;
        public static final int STORED_OUT = 5;
    }
}
