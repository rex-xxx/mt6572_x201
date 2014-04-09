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

package com.mediatek.MediatekDM.mdm.scomo;

import com.mediatek.MediatekDM.mdm.DownloadDescriptor;

/**
 * Handle SCOMO DP notifications. An instance of this interface should be
 * registered to MdmScomoDp.
 * 
 * @author mtk81226
 */
public interface MdmScomoDpHandler {
    /**
     * Notification that the delivery package is available for download and the
     * Engine is awaiting command to start download.
     * 
     * @param scomoDpInstance SCOMO DP instance.
     * @param dd Download descriptor.
     * @return true to indicate to start downloading the package package, or
     *         false to postpone download execution until
     *         MdmScomoDp.resumeDLSession() is called.
     */
    boolean confirmDownload(MdmScomoDp scomoDpInstance, DownloadDescriptor dd);

    /**
     * Notification that the delivery package has been downloaded and the Engine
     * is awaiting command to start install.
     * 
     * @param scomoDpInstance SCOMO DP instance.
     * @return true to launch the Installer to execute install, or false to
     *         postpone installation execution until either:
     *         MdmScomoDp.executeInstall() is called, or after device reboot.
     */
    boolean confirmInstall(MdmScomoDp scomoDpInstance);

    /**
     * Request the installer to start with the execution of the delivery
     * package.
     * 
     * @param scomoDpInstance SCOMO DP instance.
     * @param deliveryPkgPath Path to the downloaded delivery package.
     * @param isActive A flag to indicate whether to install DCs in their active
     *            state.
     * @return The return value is ignored.
     */
    ScomoOperationResult executeInstall(MdmScomoDp scomoDpInstance, String deliveryPkgPath,
            boolean isActive);
}
