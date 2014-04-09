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

import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.android.internal.telephony.Connection;
import com.android.phone.Constants;

public interface IVTInCallScreen {

    /**
     * The function to initialize video call GUI
     */
    void initVTInCallScreen();

    /**
     * The function to update video call GUI
     * @param mode    open or close
     */
    void updateVTScreen(Constants.VTScreenMode mode);

    /**
     * The function to set video call mode
     * @param mode    open or close
     */
    void setVTScreenMode(Constants.VTScreenMode mode);

    /**
     * The function to get video call mode
     * @return open or close
     */
    Constants.VTScreenMode getVTScreenMode();

    /**
     * The function to initilize video call related
     * process before answer incoming call
     */
    void internalAnswerVTCallPre();

    /**
     * The function to reset video call flags
     * when MO or MT video call
     */
    void resetVTFlags();

    /**
     * The function to dismiss all video call dialogs
     */
    void dismissVTDialogs();

    /**
     * The function to update video call recording state
     * @param state     recording or not recording
     */
    void updateVideoCallRecordState(final int state);

    /**
     * The function to set video call visible to VT Manager
     * @param bIsVisible    true or false
     */
    void setVTVisible(final boolean bIsVisible);

    /**
     * The function called when disconnect video call
     * @param connection      disconnected connection
     * @param slotId          slot id
     * @param isForeground    is video call GUI in foreground
     * @return true           disconnect finish
     *         false          disconnect continue
     */
    boolean onDisconnectVT(final Connection connection, final int slotId,
                           final boolean isForeground);

    /**
     * The function to update call elapsed time
     * @param elapsedTime    elapsed time
     */
    void updateElapsedTime(final long elapsedTime);

    /**
     * The function called before activity destroyed
     */
    void onDestroy();

    /**
     * The function called when setup option menu
     * @param menu    the menu to setup
     */
    void setupMenuItems(Menu menu);

    /**
     * The function called to prepare option menu
     * @param menu      the menu to prepare
     * @return true     handled
     *         false    not handled
     */
    boolean onPrepareOptionsMenu(Menu menu);

    /**
     * The function called when option menu item clicked
     * @param item     clicked menu item
     * @return true    handled
     *         false   not handled
     */
    boolean onOptionsItemSelected(MenuItem item);

    /**
     * Then function to handle menu item click event
     * @param menuItem    clicked menu item
     * @return true       handled
     *         false      not handled
     */
    boolean handleOnScreenMenuItemClick(MenuItem menuItem);

    /**
     * The function called in MO process in video call condition
     */
    void initCommonVTState();

    /**
     * The function called in MO process
     */
    void initDialingVTState();

    /**
     * The function called in MO process when MO success
     */
    void initDialingSuccessVTState();

    /**
     * The function to refresh audio mode button
     * according to latest state
     */
    void refreshAudioModePopup();

    /**
     * The function to stop video call recording
     */
    void stopRecord();

    /**
     * The function called before activity stop
     */
    void onStop();

    /**
     * The function to notify language change
     */
    void notifyLocaleChange();

    /**
     * The function to handle event of time counter start
     */
    void onReceiveVTManagerStartCounter();

    /**
     * The function to handle key down event
     * @param keyCode    key code
     * @param event      event for handling
     * @return true      handled
     *         false     not handled
     */
    boolean onKeyDown(int keyCode, KeyEvent event);

    /**
     * The function to show voice call re-dial dialog
     * @param resid     resource id
     * @param number    phone number
     * @param slot      slot id
     */
    void showReCallDialog(final int resid, final String number, final int slot);
}
