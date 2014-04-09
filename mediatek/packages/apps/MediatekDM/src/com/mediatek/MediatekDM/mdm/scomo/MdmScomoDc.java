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

import com.mediatek.MediatekDM.mdm.MdmEngine;
import com.mediatek.MediatekDM.mdm.MdmException;
import com.mediatek.MediatekDM.mdm.MdmLog;

/**
 * Deployment Component Management Object.
 * 
 * @author ye.jiao@mediatek.com
 */
public class MdmScomoDc {
    /**
     * Deployment status.
     * 
     * @author mtk81226
     */
    public static enum DeployStatus {
        ACTIVATION_DONE, ACTIVATION_START_POSTPONED, ACTIVATION_STARTED, DEACTIVATION_DONE, DEACTIVATION_START_POSTPONED, DEACTIVATION_STARTED,
    }

    private String mName;
    private MdmScomoDcHandler mHandler;
    private PLInventory mInventory;
    private MdmScomo mScomo;
    private ScomoFactory mFactory;
    private ScomoComponent mSC;

    private boolean mDestroyed;

    protected MdmScomoDc(String dcName, MdmScomoDcHandler h, PLInventory inventory, MdmScomo scomo) {
        this(dcName, h, inventory, new ScomoFactory(), scomo);
    }

    protected MdmScomoDc(String dcName, MdmScomoDcHandler h, PLInventory inventory,
            ScomoFactory factory, MdmScomo scomo) {
        mName = dcName;
        mHandler = h;
        mInventory = inventory;
        mScomo = scomo;
        mFactory = factory;
    }

    public String getName() {
        return mName;
    }

    public void setHandler(MdmScomoDcHandler h) {
        mHandler = h;
    }

    public MdmScomoDcHandler getHandler() {
        return mHandler;
    }

    public void setPLInventory(PLInventory inventory) {
        mInventory = inventory;
    }

    public void setFactory(ScomoFactory factory) {
        mFactory = factory;
    }

    public void destroy() {
        // TODO release resources
        try {
            deleteFromInventory();
        } catch (MdmException e) {
            MdmLog.e(MdmEngine.TAG, "MdmScomoDc.deleteFromInventory() error.");
        }
        mName = null;
        mHandler = null;
        mInventory = null;
        mScomo = null;
        mFactory = null;
        mDestroyed = true;
    }

    public void addToInventory(String id, String name, String pkgId, String version,
            String description,
            String envType, boolean isActive) throws MdmException {
        ScomoComponent sc = mFactory.getComponent();
        sc.create(id, name, version, description, envType, isActive);
        mInventory.addComponent(sc);
        mSC = sc;
        // TODO deal with pkgId
    }

    /**
     * Trigger report session (generic alert). Should be called by the client
     * application after remove, activate, deactivate, were executed. The report
     * will send the result set by setDeploymentResult(VdmScomoResult)
     * 
     * @throws MdmException
     */
    public void triggerReportSession() throws MdmException {
        // TODO
    }

    public void setDeploymentResult(MdmScomoResult resultCode) throws MdmException {
        // TODO
    }

    public MdmScomoDc.DeployStatus getDeployStatus() throws MdmException {
        return null;
    }

    public void executeRemove() throws MdmException {
        // TODO
    }

    public void executeActivate() throws MdmException {
        // TODO
    }

    public void executeDeactivate() throws MdmException {
        // TODO
    }

    public void deleteFromInventory() throws MdmException {
        mInventory.deleteComponent(mSC.getId());
        mSC = null;
    }

    protected void finalize() throws MdmException {
        if (!mDestroyed) {
            throw new MdmException(MdmException.MdmError.INTERNAL,
                    "MdmScomoDc.destroy() must be invoked before this object is freed.");
        }
    }
}
