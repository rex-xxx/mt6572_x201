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

package com.mediatek.MediatekDM.mdm;

import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.mdm.MdmException.MdmError;

public class MdmTree {
    // TODO
    public static class DFProperties {
        public static final int ACCESS_TYPE_ADD = 1;
        public static final int ACCESS_TYPE_COPY = 2;
        public static final int ACCESS_TYPE_DELETE = 4;
        public static final int ACCESS_TYPE_EXEC = 8;
        public static final int ACCESS_TYPE_GET = 16;
        public static final int ACCESS_TYPE_REPLACE = 32;
        public static final int ACCESS_TYPE_BEHAVIOR = 255;
    }

    private TreeManagerAgent mTMA;
    private static final String URI_SEPERATOR = "/";

    public MdmTree() {
        try {
            mTMA = TreeManagerAgent.getInstance();
        } catch (MdmException e) {
            e.printStackTrace();
        }
    }

    /*
     * Node operations
     */
    public void addLeafNode(String nodeUri, String format, String type, byte[] data)
            throws MdmException {
        if (false == mTMA.addLeaf(nodeUri, format, type, data)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    public void addChildLeafNode(String parentUri, String nodeName, String format, String type,
            byte[] data) throws MdmException {
        if (false == mTMA.addLeaf(parentUri + URI_SEPERATOR + nodeName, type, format, data)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    public void addInteriorNode(String nodeUri, String type) throws MdmException {
        if (false == mTMA.addInterior(nodeUri, type)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    public void addInteriorChildNode(String parentUri, String nodeName, String type)
            throws MdmException {
        if (false == mTMA.addInterior(parentUri + URI_SEPERATOR + nodeName, type)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    public void deleteNode(String nodeUri) throws MdmException {
        if (false == mTMA.deleteNode(nodeUri)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    /*
     * Handler operations
     */
    public void registerNodeIoHandler(String nodeUri, NodeIoHandler handler) throws MdmException {
        if (false == mTMA.registerNodeIoHandler(nodeUri, handler)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    public void unregisterNodeIoHandler(String nodeUri) throws MdmException {
        if (false == mTMA.unregisterNodeIoHandler(nodeUri)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    public void registerSubTreeIoHandler(String nodeUri, NodeIoHandler handler) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    public void unregisterSubTreeIoHandler(NodeIoHandler handler) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    public void registerExecute(String nodeUri, NodeExecuteHandler handler) throws MdmException {
        if (false == mTMA.registerNodeExecHandler(nodeUri, handler)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    public void unregisterExecute(String nodeUri) throws MdmException {
        if (false == mTMA.unregisterNodeExecHandler(nodeUri)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    public void registerAdd(String nodeUri, NodeAddHandler handler) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    public void unregisterAdd(NodeAddHandler handler) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    public void registerAddSubTree(String nodeUri, NodeAddHandler handler) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    public void unregisterAddSubTree(NodeAddHandler handler) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    public void registerDelete(String nodeUri, NodeDeleteHandler handler) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    public void unregisterDelete(NodeDeleteHandler handler) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    public void registerDeleteSubTree(String nodeUri, NodeDeleteHandler handler)
            throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    public void unregisterDeleteSubTree(NodeDeleteHandler handler) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    public void registerPreExecNotify(NodePreExecuteHandler handler) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    /*
     * ACL operations
     */
    public String getACL(String nodeUri) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    public void replaceACL(String nodeUri, String newAcl) throws MdmException {
        // TODO
        throw new NotImplementedError();
    }

    /*
     * Property operations
     */
    public String getProperty(String nodeUri, String propertyName) throws MdmException {
        String result = mTMA.getProperty(nodeUri, propertyName);
        if (null == result) {
            throw new MdmException(MdmError.INTERNAL);
        }
        return result;
    }

    public void replaceProperty(String nodeUri, String propertyName, String value)
            throws MdmException {
        mTMA.replaceProperty(nodeUri, propertyName, value);
    }

    /*
     * Persistent storage
     */
    public void writeToPersistentStorage() throws MdmException {
        if (false == mTMA.writeTree()) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    /*
     * Value operations
     */
    public int getBinValue(String nodeUri, byte[] buffer) throws MdmException {
        return mTMA.getBinValue(nodeUri, buffer);
    }

    public boolean getBoolValue(String nodeUri) throws MdmException {
        return mTMA.getBoolValue(nodeUri);
    }

    public int getIntValue(String nodeUri) throws MdmException {
        return mTMA.getIntValue(nodeUri);
    }

    public String getStringValue(String nodeUri) throws MdmException {
        return mTMA.getStringValue(nodeUri);
    }

    public void replaceBinValue(String nodeUri, byte[] value) throws MdmException {
        mTMA.replaceBinValue(nodeUri, value);
    }

    public void replaceBoolValue(String nodeUri, boolean value) throws MdmException {
        mTMA.replaceBoolValue(nodeUri, value);
    }

    public void replaceIntValue(String nodeUri, int value) throws MdmException {
        mTMA.replaceIntValue(nodeUri, value);
    }

    public void replaceStringValue(String nodeUri, String value) throws MdmException {
        mTMA.replaceStringValue(nodeUri, value);
    }

    /**
     * Internal representation for native tree manager agent.
     * 
     * @author mtk81226
     */
    private static class TreeManagerAgent {
        private static TreeManagerAgent instance;
        static {
            System.loadLibrary("jni_mdm");
        }

        private TreeManagerAgent() throws MdmException {
            boolean result = false;
            try {
                result = initialize();
            } catch (UnsatisfiedLinkError e) {
                Log.e(TAG.Service, e.getMessage());
            }
            if (!result) {
                throw new MdmException(MdmError.INTERNAL);
            }
        }

        public static TreeManagerAgent getInstance() throws MdmException {
            synchronized (TreeManagerAgent.class) {
                if (instance == null) {
                    instance = new TreeManagerAgent();
                }
                return instance;
            }
        }

        public native boolean initialize();

        public native void destroy();

        /* TODO Node add/delete */
        public native boolean addInterior(String nodeUri, String type);

        public native boolean addLeaf(String nodeUri, String format, String type, byte[] data);

        public native boolean deleteNode(String nodeUri);

        /* TODO Property */
        public native String getProperty(String nodeUri, String propertyName);

        public native void replaceProperty(String nodeUri, String propertyName, String value);

        /* TODO Value */
        public native int getBinValue(String nodeUri, byte[] buffer);

        public native boolean getBoolValue(String nodeUri);

        public native int getIntValue(String nodeUri) throws MdmException;

        public native String getStringValue(String nodeUri);

        public native void replaceBinValue(String nodeUri, byte[] value);

        public native void replaceBoolValue(String nodeUri, boolean value);

        public native void replaceIntValue(String nodeUri, int value);

        public native void replaceStringValue(String nodeUri, String value);

        /* Handler */
        public native boolean registerNodeIoHandler(String nodeUri, NodeIoHandler handler);

        public native boolean unregisterNodeIoHandler(String nodeUri);

        public native boolean registerNodeExecHandler(String nodeUri, NodeExecuteHandler handler);

        public native boolean unregisterNodeExecHandler(String nodeUri);

        /* Execute node */
        public native boolean execNode(String nodeUri, byte[] data);

        /* Persistent storage */
        public native boolean readTree();

        public native boolean writeTree();
    }
}
