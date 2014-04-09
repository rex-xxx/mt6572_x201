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

package com.mediatek.phone.ext;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Connection;

public class InCallScreenExtension {

    /**
     * 
     * @param menu 
     * @param inCallControlState 
     */
    public void setupMenuItems(Menu menu, IInCallControlState inCallControlState) {
    }

    /**
     * 
     * @param menuItem 
     * @return 
     */
    public boolean handleOnScreenMenuItemClick(MenuItem menuItem) {
        return false;
    }

    /**
     * 
     * @param callManager 
     * @param isForegroundActivity 
     * @return 
     */
    public boolean updateScreen(CallManager callManager, boolean isForegroundActivity) {
        return false;
    }

    /**
     * 
     * @param icicle 
     * @param inCallScreenActivity 
     * @param inCallScreenHost 
     * @param cm 
     */
    public void onCreate(Bundle icicle, Activity inCallScreenActivity,
                         IInCallScreen inCallScreenHost, CallManager cm) {
        
    }

    /**
     * 
     * @param inCallScreen 
     */
    public void onDestroy(Activity inCallScreen) {

    }

    /**
     * 
     * @param cn 
     * @return 
     */
    public boolean onDisconnect(Connection cn) {
        return false;
    }

    /**
     * 
     * @param cm 
     * @return 
     */
    public boolean onPhoneStateChanged(CallManager cm) {
        return false;
    }

    /**
     * 
     * @param id 
     * @return 
     */
    public boolean handleOnscreenButtonClick(int id) {
        return false;
    }

    /**
     * 
     * @return 
     */
    public boolean dismissDialogs() {
        return false;
    }
}
