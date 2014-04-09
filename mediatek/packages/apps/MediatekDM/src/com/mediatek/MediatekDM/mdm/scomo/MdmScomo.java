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

import com.mediatek.MediatekDM.mdm.MdmException;
import com.mediatek.MediatekDM.mdm.MdmException.MdmError;
import com.mediatek.MediatekDM.mdm.MdmTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * SCOMO manager. There can be multiple instances of MdmScomo as long as the
 * sub-root URI of any instance are not the prefix of every other instances.
 * 
 * @author ye.jiao@mediatek.com
 */
public class MdmScomo {
    private static Map<String, MdmScomo> sInstances = new HashMap<String, MdmScomo>();

    private Map<String, MdmScomoDp> mDps;
    private Map<String, MdmScomoDc> mDcs;
    private boolean mAutoAddDPChildNodes;
    private String mAlertType;
    private boolean mDestroyed;
    private String mRootURI;
    private MdmScomoHandler mHandler;
    private MdmTree mTree;

    /**
     * Get the single instance of SCOMO manager.
     * 
     * @param scomoRootURI Root URI in DM tree.
     * @param h MdmScomoHandler instance.
     * @return Single instance of SCOMO manager.
     * @throws MdmException
     */
    public static MdmScomo getInstance(String scomoRootURI, MdmScomoHandler h) throws MdmException {
        synchronized (MdmScomo.class) {
            if (!sInstances.containsKey(scomoRootURI)) {
                // sanity test
                for (String uri : sInstances.keySet()) {
                    if (uri.startsWith(scomoRootURI) || scomoRootURI.startsWith(uri)) {
                        throw new MdmException(MdmError.BAD_INPUT,
                                "Sub-root URI cannot prefix or be the prefix of existing instances' URI");
                    }
                }
                sInstances.put(scomoRootURI, new MdmScomo(scomoRootURI, h));
            }
            return sInstances.get(scomoRootURI);
        }
    }

    private MdmScomo(String scomoRootURI, MdmScomoHandler h) throws MdmException {
        if (scomoRootURI == null) {
            throw new MdmException(MdmException.MdmError.BAD_INPUT, "scomoRootURI can NOT be null.");
        }
        mRootURI = scomoRootURI;
        mHandler = h;
        mDps = new HashMap<String, MdmScomoDp>();
        mDcs = new HashMap<String, MdmScomoDc>();
        // TODO check these two fields
        mAutoAddDPChildNodes = true;
        mAlertType = "";
        mTree = new MdmTree();
    }

    public void destroy() {
        mDps = null;
        sInstances = null;
        mDestroyed = true;
        // TODO check this method, destroy something
    }

    /**
     * Create a MdmScomoDp instance.
     * 
     * @param dpName SCOMO DP name
     * @param h SCOMO DP Handler
     * @return
     */
    public MdmScomoDp createDP(String dpName, MdmScomoDpHandler h) {
        MdmScomoDp dp = new MdmScomoDp(dpName, h, this);
        mDps.put(dpName, dp);
        // TODO Add node to DM tree
        if (mAutoAddDPChildNodes) {
            // TODO add sub nodes in DM tree
        }
        // TODO is this right?
        if (mHandler != null) {
            mHandler.newDpAdded(dpName);
        }
        return dp;
    }

    /**
     * Create a VdmScomoDc instance. This constructor allows the user to use a
     * different implementation for ScomoFactory. <b>Note</b>: You must call
     * VdmScomoDc.destroy() when the client terminates to allow for graceful
     * exit the SCOMO DC instance.
     * 
     * @param dcName
     * @param h
     * @param inventory
     * @param factory
     * @return
     */
    public MdmScomoDc createDC(String dcName, MdmScomoDcHandler h, PLInventory inventory,
            ScomoFactory factory) {
        MdmScomoDc dc = new MdmScomoDc(dcName, h, inventory, factory, this);
        mDcs.put(dcName, dc);
        return dc;
    }

    /**
     * Create a VdmScomoDc instance. This constructor allows the user to use a
     * different implementation for ScomoFactory. <b>Note</b>: You must call
     * VdmScomoDc.destroy() when the client terminates to allow for graceful
     * exit the SCOMO DC instance.
     * 
     * @param dcName
     * @param h
     * @param inventory
     * @return
     */
    public MdmScomoDc createDC(String dcName, MdmScomoDcHandler h, PLInventory inventory) {
        MdmScomoDc dc = new MdmScomoDc(dcName, h, inventory, this);
        mDcs.put(dcName, dc);
        return dc;
    }

    protected MdmScomoDc searchDc(String dcName) {
        // TODO should we search node in tree?
        return mDcs.get(dcName);
    }

    /**
     * return the first DP with name equals to dpName.
     * 
     * @param dpName
     * @return The DP found or null if nothing found.
     */
    protected MdmScomoDp searchDp(String dpName) {
        // TODO should we search node in tree?
        return mDps.get(dpName);
    }

    protected void removeDp(String dpName) {
        mDps.remove(dpName);
        // TODO remove node in tree
    }

    protected void removeDc(String dcName) {
        mDcs.remove(dcName);
        // TODO remove node in tree
    }

    /**
     * Set whether, when a new delivery package is added to the DM Tree, its
     * child nodes should be added automatically by SCOMO. Default is false (the
     * server is expected to add the nodes).
     * 
     * @param autoAdd
     * @throws MdmException
     */
    public void setAutoAddDPChildNodes(boolean autoAdd) throws MdmException {
        mAutoAddDPChildNodes = autoAdd;
    }

    /**
     * Get whether, when a new delivery package is added to the DM Tree, its
     * child nodes should be added automatically by SCOMO.
     * 
     * @return
     * @throws MdmException
     */
    public boolean getAutoAddDPChildNodes() throws MdmException {
        return mAutoAddDPChildNodes;
    }

    /**
     * Set alert type string to be used on SCOMO generic alerts.
     * 
     * @param alertType Alert type string.
     * @throws MdmException
     */
    public void setAlertType(String alertType) throws MdmException {
        mAlertType = alertType;
    }

    /**
     * Get alert type string to be used on SCOMO generic alerts.
     * 
     * @return Alert type string.
     * @throws MdmException
     */
    public String getAlertType() throws MdmException {
        return mAlertType;
    }

    /**
     * Get an ArrayList of currently available delivery packages.
     * 
     * @return ArrayList of DPs.
     */
    public ArrayList<MdmScomoDp> getDps() {
        return new ArrayList<MdmScomoDp>(mDps.values());
    }

    /**
     * Get an ArrayList of currently available deployment components.
     * 
     * @return ArrayList of DCs.
     */
    public ArrayList<MdmScomoDc> getDcs() {
        return new ArrayList<MdmScomoDc>(mDcs.values());
    }

    /**
     * Query which actions that are relevant for this SCOMO instance were
     * performed last session.
     * 
     * @return Bit flags describing the session actions.
     */
    public int querySessionActions() {
        // TODO retrieve info from DM Tree
        return 0;
    }

    protected void finalize() throws MdmException {
        if (!mDestroyed) {
            throw new MdmException(MdmException.MdmError.INTERNAL,
                    "MdmScomo.destroy() must be invoked before this object is freed.");
        }
    }
}
