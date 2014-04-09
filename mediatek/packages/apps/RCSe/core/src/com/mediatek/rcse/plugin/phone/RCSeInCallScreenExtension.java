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
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Connection;

import com.mediatek.phone.ext.IInCallControlState;
import com.mediatek.phone.ext.IInCallScreen;
import com.mediatek.phone.ext.InCallScreenExtension;

public class RCSeInCallScreenExtension extends InCallScreenExtension {

    private static final String LOG_TAG = "RCSeInCallScreenExtension";
    private static final boolean DBG = true;

    private Context mPluginContext;
    private RCSePhonePlugin mRCSePhonePlugin;
    private IInCallScreen mInCallScreenHost;
    private Activity mActivity;
    private CallManager mCM;

    private ShareFileCallScreenHost mShareFileHost;
    private ShareVideoCallScreenHost mShareVideoHost;

    private ICallScreenPlugIn mShareFilePlugIn;
    private ICallScreenPlugIn mShareVideoPlugIn;
    private static RCSeInCallScreenExtension sInstance = null;

    public static void initialize(Context context, RCSePhonePlugin rcsePhonePlugin) {
        sInstance = new RCSeInCallScreenExtension(context, rcsePhonePlugin);
    }

    public static RCSeInCallScreenExtension getInstance() {
        return sInstance;
    }

    protected RCSeInCallScreenExtension(Context context, RCSePhonePlugin rcsePhonePlugin) {
        mPluginContext = context;
        mRCSePhonePlugin = rcsePhonePlugin;
    }

    protected Resources getHostResources() {
        return mActivity.getResources();
    }

    protected String getHostPackageName() {
        return mActivity.getPackageName();
    }

    public void onCreate(Bundle icicle, Activity activity, IInCallScreen inCallScreenHost,
            CallManager cm) {
        if (DBG) {
            log("onCreate(), icicle = " + icicle + ", " + "InCallScreen activity = " + activity
                    + "InCallScreen host = " + inCallScreenHost + ", cm = " + cm);
        }
        mActivity = activity;
        mRCSePhonePlugin.setInCallScreenActivity(activity);
        mInCallScreenHost = inCallScreenHost;
        mCM = cm;
        mRCSePhonePlugin.setCallManager(cm);
        mShareFilePlugIn = new ImageSharingPlugin(mPluginContext);
        mShareVideoPlugIn = new VideoSharingPlugin(mPluginContext);
        mShareFileHost = new ShareFileCallScreenHost();
        mShareVideoHost = new ShareVideoCallScreenHost();
        // set host to plug-in
        if (null != mShareFilePlugIn) {
            mShareFilePlugIn.setCallScreenHost(mShareFileHost);
        }
        if (null != mShareVideoPlugIn) {
            mShareVideoPlugIn.setCallScreenHost(mShareVideoHost);
        }
    }

    public void onDestroy(Activity activity) {
        if (DBG) {
            log("onDestroy(), inCallScreen is " + activity);
        }
        if (mActivity == activity) {
            mActivity = null;
        }
        if (mShareFileHost == mShareFilePlugIn.getCallScreenHost()) {
            mShareFilePlugIn.setCallScreenHost(null);
        }
        if (mShareVideoHost == mShareVideoPlugIn.getCallScreenHost()) {
            mShareVideoPlugIn.setCallScreenHost(null);
        }
    }

    // The function called by onPrepareOptionsMenu() to set visibility of menu
    // items
    public void setupMenuItems(Menu menu, IInCallControlState inCallControlState) {

        if (DBG) {
            log("setupMenuItems()");
        }
        Resources resource = getHostResources();
        String packageName = getHostPackageName();
        final MenuItem addMenu =
                menu.findItem(resource.getIdentifier("menu_add_call", "id", packageName));
        final MenuItem holdMenu =
                menu.findItem(resource.getIdentifier("menu_hold_voice", "id", packageName));

        final Call ringingCall = mCM.getFirstActiveRingingCall();

        if (RCSeUtils.canShare(mCM)) {
            if (DBG) {
                log("setupMenuItems(), can share");
            }
            if (isSharingVideo()) {
                if (DBG) {
                    log("setupMenuItems(), is sharing video");
                }
                // share video
                if (ringingCall != null && ringingCall.getState() == Call.State.IDLE) {
                    int size = menu.size();
                    for (int i = 0; i < size; ++i) {
                        menu.getItem(i).setVisible(false);
                    }
                    holdMenu.setVisible(inCallControlState.canHold());
                    if (inCallControlState.canHold()) {
                        String title = null;
                        if (inCallControlState.onHold()) {
                            title = resource.getString(resource.getIdentifier("incall_toast_unhold",
                                    "string", packageName));
                        } else {
                            title = resource.getString(resource.getIdentifier("incall_toast_hold",
                                    "string", packageName));
                        }
                        holdMenu.setTitle(title);
                    }
                    if (!ViewConfiguration.get(mActivity).hasPermanentMenuKey()) {
                        if (inCallControlState.canAddCall()) {
                            if (addMenu != null) {
                                addMenu.setVisible(true);
                            }
                        }
                    }
                }
            } else {
                if (DBG) {
                    log("setupMenuItems(), not share video");
                }
                holdMenu.setVisible(inCallControlState.canHold());
                String title = null;
                    if (inCallControlState.onHold()) {
                    title = resource.getString(resource.getIdentifier("incall_toast_unhold",
                            "string", packageName));
                    } else {
                    title = resource.getString(resource.getIdentifier("incall_toast_hold",
                            "string", packageName));
                }
                holdMenu.setTitle(title);
            }
        }
    }

    // The function called to handle option menu item clicking event
    public boolean handleOnScreenMenuItemClick(MenuItem menuItem) {
        Resources resource = getHostResources();
        String packageName = getHostPackageName();

        if (menuItem.getItemId() == resource.getIdentifier("menu_hold_voice", "id", packageName)) {
            if (DBG) {
                log("hold voice menu item is clicked");
            }
            if ((isTransferingFile())) {
                mShareFilePlugIn.stop();
            } else if (isSharingVideo()) {
                mShareVideoPlugIn.stop();
            }
        }
        return false;
    }

    // The function called when phone state is changed
    public boolean onPhoneStateChanged(CallManager cm) {
        if (DBG) {
            log("onPhoneStateChanged(), cm = " + cm);
        }
        if (RCSeUtils.canShareFromCallState(cm)) {
            String number = RCSeUtils.getRCSePhoneNumber(cm);
            if (null != number) {
                if (null != mShareFilePlugIn) {
                    mShareFilePlugIn.registerForCapabilityChange(number);
                }
                if (null != mShareVideoPlugIn) {
                    mShareVideoPlugIn.registerForCapabilityChange(number);
                }
            }
        }
        if (RCSeUtils.shouldStop(cm)) {
            if ((isTransferingFile())) {
                mShareFilePlugIn.stop();
            } else if (isSharingVideo()) {
                mShareVideoPlugIn.stop();
            }
        }
        if (null != mShareFilePlugIn) {
            mShareFilePlugIn.onPhoneStateChange(cm);
        }
        if (null != mShareVideoPlugIn) {
            mShareVideoPlugIn.onPhoneStateChange(cm);
        }
        return false;
    }

    public boolean dismissDialogs() {
        if (null != mShareFilePlugIn) {
            mShareFilePlugIn.dismissDialog();
        }
        if (null != mShareVideoPlugIn) {
            mShareVideoPlugIn.dismissDialog();
        }
        return false;
    }

    public static ICallScreenPlugIn getShareFilePlugIn() {
        return getInstance().mShareFilePlugIn;
    }

    public static ICallScreenPlugIn getShareVideoPlugIn() {
        return getInstance().mShareVideoPlugIn;
    }

    public static boolean isCapabilityToShare(String number) {
        if (DBG) {
            log("isCapabilityToShare(), number = " + number);
        }
        ICallScreenPlugIn filePlugin = getShareFilePlugIn();
        ICallScreenPlugIn videoPlugin = getShareVideoPlugIn();
        if (null == filePlugin && null == videoPlugin) {
            if (DBG) {
                log("both plug-in are null, no capability");
            }
            return false;
        } else if (null != filePlugin && filePlugin.getCapability(number)) {
            if (DBG) {
                log("Share file plugIn has capability");
            }
            return true;
        } else if (null != videoPlugin && videoPlugin.getCapability(number)) {
            if (DBG) {
                log("Share video plugIn has capability");
            }
            return true;
        } else {
            if (DBG) {
                log("Neither plug-ins have capability");
            }
            return false;
        }
    }

    public static boolean isTransferingFile() {
        ICallScreenPlugIn plugin = getShareFilePlugIn();
        if (null == plugin) {
            return false;
        }
        return Constants.SHARE_FILE_STATE_TRANSFERING == plugin.getState();
    }

    public static boolean isDisplayingFile() {
        ICallScreenPlugIn plugin = getShareFilePlugIn();
        if (null == plugin) {
            return false;
        }
        return Constants.SHARE_FILE_STATE_DISPLAYING == plugin.getState();
    }

    public static boolean isSharingVideo() {
        ICallScreenPlugIn plugin = getShareVideoPlugIn();
        if (null == plugin) {
            return false;
        }
        return Constants.SHARE_VIDEO_STATE_SHARING == plugin.getState();
    }

    public boolean onDisconnect(Connection cn) {
        if (DBG) {
            log("onDisconnect(), cn = " + cn);
        }
        if (null != mShareFilePlugIn) {
            mShareFilePlugIn.unregisterForCapabilityChange(cn.getAddress());
        }
        if (null != mShareVideoPlugIn) {
            mShareVideoPlugIn.unregisterForCapabilityChange(cn.getAddress());
        }
        return false;
    }

    public boolean updateScreen(CallManager callManager, boolean isForegroundActivity) {
        if (RCSeUtils.canShare(callManager)) {
            if (isSharingVideo() || isDisplayingFile()) {
                return false;
            }
        }
        Resources resource = getHostResources();
        String packageName = getHostPackageName();
        View inCallTouchUi =
                (View) mActivity.findViewById(resource.getIdentifier("inCallTouchUi", "id",
                        packageName));
        if (null != inCallTouchUi) {
            inCallTouchUi.setVisibility(View.VISIBLE);
        }
        return false;
    }

    /*
     * public boolean handleOnscreenButtonClick(int id) { switch (id) { case
     * R.id.endSharingVideo: if (DBG)
     * log("end sharing video button is clicked"); if (null !=
     * mShareVideoPlugIn) { mShareVideoPlugIn.stop(); } return true; case
     * R.id.shareFileButton: if (DBG) log("share file button is clicked"); if
     * (null != mShareFilePlugIn) { String phoneNumber =
     * RCSeUtils.getRCSePhoneNumber(mCM); if (null != phoneNumber) {
     * mShareFilePlugIn.start(phoneNumber); } } return true; case
     * R.id.shareVideoButton: if (DBG) log("share video button is clicked"); if
     * (null != mShareVideoPlugIn) { String phoneNumber =
     * RCSeUtils.getRCSePhoneNumber(mCM); if (null != phoneNumber) {
     * mShareVideoPlugIn.start(phoneNumber); } } return true; } return false; }
     */

    public class ShareFileCallScreenHost implements ICallScreenHost {

        public ShareFileCallScreenHost() {
        }

        public ViewGroup requestAreaForDisplay() {
            if (DBG) {
                log("ShareFileCallScreenHost::requestAreaForDisplay()");
            }
            Resources resource = getHostResources();
            String packageName = getHostPackageName();
            return (ViewGroup) mActivity.findViewById(resource.getIdentifier(
                    "centerAreaForSharing", "id", packageName));
        }

        public void onStateChange(final int state) {
            if (DBG) {
                log("ShareFileCallScreenHost::onStateChange(), state = " + state);
            }
            if (null != mInCallScreenHost) {
                mInCallScreenHost.requestUpdateScreen();
            }
        }

        public void onCapabilityChange(String number, boolean isSupport) {
            if (DBG) {
                log("ShareFileCallScreenHost::onCapabilityChange(), number = " + number
                        + ", isSupport = " + isSupport);
            }
            if (null != mInCallScreenHost) {
                mInCallScreenHost.requestUpdateScreen();
            }
        }

        public Activity getCallScreenActivity() {
            return mActivity;
        }
    }

    public class ShareVideoCallScreenHost implements ICallScreenHost {

        public ShareVideoCallScreenHost() {
        }

        public ViewGroup requestAreaForDisplay() {
            if (DBG) {
                log("ShareVideoCallScreenHost::requestAreaForDisplay()");
            }
            Resources resource = getHostResources();
            String packageName = getHostPackageName();
            return (ViewGroup) mActivity.findViewById(resource.getIdentifier("largeAreaForSharing",
                    "id", packageName));
        }

        public void onStateChange(final int state) {
            if (DBG) {
                log("ShareVideoCallScreenHost::onStateChange(), state = " + state);
            }
            if (null != mInCallScreenHost) {
                mInCallScreenHost.requestUpdateScreen();
            }
        }

        public void onCapabilityChange(String number, boolean isSupport) {
            if (DBG) {
                log("ShareVideoCallScreenHost::onCapabilityChange(), number = " + number
                        + ", isSupport = " + isSupport);
            }
            if (null != mInCallScreenHost) {
                mInCallScreenHost.requestUpdateScreen();
            }
        }

        public Activity getCallScreenActivity() {
            return mActivity;
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
