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

/**
 * Handle SCOMO DC notifications. An instance of this interface should be
 * registered to MdmScomoDc.
 * 
 * @author mtk81226
 */
public interface MdmScomoDcHandler {
    /**
     * Notification the Engine is awaiting command to start uninstall.
     * 
     * @param scomoDc the deployment component to be uninstalled
     * @return true to launch the Installer to execute uninstall, or false to
     *         postpone uninstallation execution until either
     *         MdmScomoDc.executeRemove() is called or after device reboot
     */
    boolean confirmRemove(MdmScomoDc scomoDc);

    /**
     * Request the installer to start with the execution of uninstallation.
     * 
     * @param scomoDc the deployment component to be uninstalled
     * @return the uninstallation result
     */
    ScomoOperationResult executeRemove(MdmScomoDc scomoDc);

    /**
     * Notification the Engine is awaiting command to start activation.
     * 
     * @param scomoDc the deployment component to be activated
     * @return true to launch the Installer to execute activation, or false to
     *         postpone activation execution until either
     *         MdmScomoDc.executeActivate() is called or after device reboot
     */
    boolean confirmActivate(MdmScomoDc scomoDc);

    /**
     * Request the installer to start with the execution of activation.
     * 
     * @param scomoDc the deployment component to be activated
     * @return the activation result
     */
    ScomoOperationResult executeActivate(MdmScomoDc scomoDc);

    /**
     * Notification the Engine is awaiting command to start deactivation.
     * 
     * @param scomoDc the deployment component to be deactivated
     * @return true to launch the Installer to execute deactivation, or false to
     *         postpone deactivation execution until either
     *         MdmScomoDc.executeDeactivate() is called or after device reboot
     */
    boolean confirmDeactivate(MdmScomoDc scomoDc);

    /**
     * Request the installer to start with the execution of deactivation.
     * 
     * @param scomoDc the deployment component to be deactivated
     * @return the deactivation result
     */
    ScomoOperationResult executeDeactivate(MdmScomoDc scomoDc);
}
