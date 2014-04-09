/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.phone.vt;

import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewStub;

import com.android.internal.telephony.Connection;
import com.android.phone.Constants;
import com.android.phone.DTMFTwelveKeyDialer;
import com.android.phone.InCallScreen;
import com.android.phone.R;

public class VTInCallScreenProxy implements IVTInCallScreen {

    private static final String LOG_TAG = "VTInCallScreenProxy";
    private static final boolean DBG = true;

    private boolean mIsInflate;
    private boolean mIsLocaleChanged;
    private VTInCallScreen mVTInCallScreen;
    private InCallScreen mInCallScreen;
    private DTMFTwelveKeyDialer mDialer;

    /**
     * Constructor function
     * @param inCallScreen    InCallScreen activity
     * @param dialer          DTMF twelve key dialer
     */
    public VTInCallScreenProxy(InCallScreen inCallScreen,
                               DTMFTwelveKeyDialer dialer) {
        mInCallScreen = inCallScreen;
        mDialer = dialer;
    }

    /**
     * The function to initialize video call GUI
     */
    public void initVTInCallScreen() {
        if (null == mInCallScreen) {
            if (DBG) {
                log("mInCallScreen is null, just return");
            }
            return;
        }
        if (mIsInflate) {
            if (DBG) {
                log("already inflate, just return");
            }
            return;
        }
        // Inflate the ViewStub, look up and initialize the UI elements.
        ViewStub stub = (ViewStub) mInCallScreen.findViewById(R.id.vtInCallScreenStub);
        stub.inflate();
        mVTInCallScreen = (VTInCallScreen) mInCallScreen.findViewById(R.id.VTInCallCanvas);
        mVTInCallScreen.setInCallScreenInstance(mInCallScreen);
        mVTInCallScreen.setDialer(mDialer);
        mVTInCallScreen.initVTInCallScreen();
        if (mIsLocaleChanged) {
            mVTInCallScreen.notifyLocaleChange();
            mIsLocaleChanged = false;
        }
        mVTInCallScreen.registerForVTPhoneStates();
        mIsInflate = true;
        VTInCallScreenFlags.getInstance().mVTIsInflate = true;
    }

    /**
     * The function to update video call GUI
     * @param mode    open or close
     */
    public void updateVTScreen(Constants.VTScreenMode mode) {
        if (!isInflated()) {
            return;
        }
        mVTInCallScreen.updateVTScreen(mode);
    }

    /**
     * The function to set video call mode
     * @param mode    open or close
     */
    public void setVTScreenMode(Constants.VTScreenMode mode) {
        if (!isInflated()) {
            return;
        }
        mVTInCallScreen.setVTScreenMode(mode);
    }

    /**
     * The function to get video call mode
     * @return open or close
     */
    public Constants.VTScreenMode getVTScreenMode() {
        if (!isInflated()) {
            return Constants.VTScreenMode.VT_SCREEN_CLOSE;
        }
        return mVTInCallScreen.getVTScreenMode();
    }

    /**
     * The function to initilize video call related
     * process before answer incoming call
     */
    public void internalAnswerVTCallPre() {
        initVTInCallScreen();
        if (!isInflated()) {
            if (DBG) {
                log("inflate failed");
            }
            return;
        }
        mVTInCallScreen.internalAnswerVTCallPre();
    }

    /**
     * The function to reset video call flags
     * when MO or MT video call
     */
    public void resetVTFlags() {
        if (!isInflated()) {
            return;
        }
        mVTInCallScreen.resetVTFlags();
    }

    /**
     * The function to dismiss all video call dialogs
     */
    public void dismissVTDialogs() {
        if (!isInflated()) {
            return;
        }
        mVTInCallScreen.dismissVTDialogs();
    }

    /**
     * The function to update video call recording state
     * @param state     recording or not recording
     */
    public void updateVideoCallRecordState(int state) {
        if (!isInflated()) {
            return;
        }
        mVTInCallScreen.updateVideoCallRecordState(state);
    }

    /**
     * The function to set video call visible to VT Manager
     * @param bIsVisible    true or false
     */
    public void setVTVisible(final boolean bIsVisible) {
        if (null == mVTInCallScreen || !mIsInflate) {
            return;
        }
        mVTInCallScreen.setVTVisible(bIsVisible);
    }

    /**
     * The function called when disconnect video call
     * @param connection      disconnected connection
     * @param slotId          slot id
     * @param isForeground    is video call GUI in foreground
     * @return true           disconnect finish
     *         false          disconnect continue
     */
    public boolean onDisconnectVT(final Connection connection,
                                  final int slotId, 
                                  final boolean isForeground) {
        if (!isInflated()) {
            return false;
        }
        return mVTInCallScreen.onDisconnectVT(connection, slotId, isForeground);
    }

    /**
     * The function to update call elapsed time
     * @param elapsedTime    elapsed time
     */
    public void updateElapsedTime(final long elapsedTime) {
        if (!isInflated()) {
            return;
        }
        mVTInCallScreen.updateElapsedTime(elapsedTime);
    }

    /**
     * The function called before activity destroyed
     */
    public void onDestroy() {
        if (!isInflated()) {
            return;
        }
        mVTInCallScreen.onDestroy();
    }

    /**
     * The function called when setup option menu
     * @param menu    the menu for setup
     */
    public void setupMenuItems(Menu menu) {
        if (!isInflated()) {
            return;
        }
        mVTInCallScreen.setupMenuItems(menu);
    }

    /**
     * The function called to prepare option menu
     * @param item      the menu item clicked
     * @return true     handled
     *         false    not handled
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!isInflated()) {
            return false;
        }
        return mVTInCallScreen.onOptionsItemSelected(item);
    }

    /**
     * Then function to handle menu item click event
     * @param menuItem    clicked menu item
     * @return true       handled
     *         false      not handled
     */
    public boolean handleOnScreenMenuItemClick(MenuItem menuItem) {
        if (!isInflated()) {
            return false;
        }
        return mVTInCallScreen.handleOnScreenMenuItemClick(menuItem);
    }

    /**
     * The function called in MO process in video call condition
     */
    public void initCommonVTState() {
        if (!isInflated()) {
            return;
        }
        mVTInCallScreen.initCommonVTState();
    }

    /**
     * The function called in MO process when MO success
     */
    public void initDialingSuccessVTState() {
        if (!isInflated()) {
            return;
        }
        mVTInCallScreen.initDialingSuccessVTState();
    }

    /**
     * The function called in MO process
     */
    public void initDialingVTState() {
        if (!isInflated()) {
            return;
        }
        mVTInCallScreen.initDialingVTState();
    }

    /**
     * The function to refresh audio mode button
     * according to latest state
     */
    public void refreshAudioModePopup() {
        if (!isInflated()) {
            return;
        }
        mVTInCallScreen.refreshAudioModePopup();
    }

    /**
     * The function to stop video call recording
     */
    public void stopRecord() {
        if (DBG) {
            log("stopRecord");
        }
        if (!isInflated()) {
            return;
        }
        mVTInCallScreen.stopRecord();
    }

    /**
     * The function called before activity stop
     */
    public void onStop() {
        if (DBG) {
            log("onStop");
        }
        if (!isInflated()) {
            return;
        }
        mVTInCallScreen.onStop();
    }

    /**
     * The function to notify language change
     */
    public void notifyLocaleChange() {
        if (DBG) {
            log("NotifyLocaleChange");
        }
        if (!isInflated()) {
            mIsLocaleChanged = true;
            return;
        }
        mVTInCallScreen.notifyLocaleChange();
    }

    /**
     * The function to handle event of time counter start
     */
    public void onReceiveVTManagerStartCounter() {
        if (DBG) {
            log("onReceiveVTManagerStartCounter");
        }
        if (!isInflated()) {
            return;
        }
        mVTInCallScreen.onReceiveVTManagerStartCounter();
    }

    /**
     * The function to show voice call re-dial dialog
     * @param resid     resource id
     * @param number    phone number
     * @param slot      slot id
     */
    public void showReCallDialog(final int resid, final String number, final int slot) {
        if (DBG) {
            log("showReCallDialog");
        }
        if (!isInflated()) {
            return;
        }
        mVTInCallScreen.showReCallDialog(resid, number, slot);
    }

    /**
     * The function to handle key down event
     * @param keyCode    key code 
     * @param event      key event
     * @return true      handled
     *         false     not handled
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (DBG) {
            log("onKeyDown");
        }
        if (!isInflated()) {
            return false;
        }
        return mVTInCallScreen.onKeyDown(keyCode, event);
    }

    /**
     * The function called to prepare option menu
     * @param menu      the menu to prepare
     * @return true     handled
     *         false    not handled
     */
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (DBG) {
            log("onPrepareOptionsMenu");
        }
        if (!isInflated()) {
            return false;
        }
        return mVTInCallScreen.onPrepareOptionsMenu(menu);
    }

    private boolean isInflated() {
        return (null != mVTInCallScreen && mIsInflate);
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
