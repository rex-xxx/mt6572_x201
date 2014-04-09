/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.encapsulation.android.app;

/// M: ALPS00436165, cancel NotificationManagerPlus shown in other AP @{
import android.content.Context;
/// @}

import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.encapsulation.android.app.EncapsulatedNotificationPlus;
import com.mediatek.notification.NotificationManagerPlus;
import com.mediatek.notification.NotificationPlus;

/**
 * Plus notification manager. It will receive
 * {@link NotificationManagerPlus#ACTION_FULL_SCREEN_NOTIFY} and notify user via
 * a dialog.
 */

public class EncapsulatedNotificationManagerPlus {

    /**
     * @param id An identifier for this notification unique within your
     *            application.
     * @param notification Must not be null.
     */
    public static void notify(int id, EncapsulatedNotificationPlus notification) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            NotificationManagerPlus.notify(id, notification.mNotificationPlus);
        } else {
            //// M:implement notify() in class NotificationManagerPlus again @{
            notification.setId(id);
            notification.setType(EncapsulatedNotificationPlus.TYPE_NOTIFY);
            notification.send();
            //// @}
        }
    }

    /// M: ALPS00436165, cancel NotificationManagerPlus shown in other AP @{
    /**
     * Cancel a previously shown notification in server.
     *
     * @param context must not be null.
     * @param id
     */
    public static void cancel(Context context, int id) {
        //Log.d("Mms/Notify", "NotificationManagerPlus.cancel, id=" + id);
        NotificationManagerPlus.cancel(context, id);
    }
    /// @}
}
