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

package com.mediatek.MediatekDM;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.mdm.MdmException;
import com.mediatek.MediatekDM.mdm.MdmTree;
import com.mediatek.MediatekDM.mdm.NodeIoHandler;
import com.mediatek.MediatekDM.xml.DmXMLParser;

import org.w3c.dom.Node;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class DmRegister {

    private NodeIoHandlerFactory mHandlerFactory = null;
    private String mMccMnc = null;

    private MdmTree mDmTree = new MdmTree();

    public DmRegister(Context context, String mccMnc) {
        mHandlerFactory = new NodeIoHandlerFactory(context);
        mMccMnc = mccMnc;
    }

    /**
     * Register Node IO Handlers for nodes under "./Settings/".
     * 
     * @param treeFileName
     */
    public void registerCCNodeIoHandler(String treeFileName) {
        Log.d(TAG.NodeIOHandler, "DmRegister->registerCCNodeIoHandler():" + treeFileName);
        DmXMLParser treeParser = new DmXMLParser(treeFileName);

        ArrayList<String> uriList = new ArrayList<String>();
        uriList = (ArrayList<String>) getCCUriString(treeParser, getSettingNode(treeParser),
                DmConst.TagName.Name);
        uriList.add(DmModNodeIoHandler.uStr);
        uriList.add(DmManNodeIoHandler.uStr);
        String uriStr = null;
        String itemStr = null;
        for (int i = 0; i < uriList.size(); i++) {
            uriStr = uriList.get(i);
            // Log.w(TAG.Service, "uriStr: " + uriStr);
            String[] uriStrArray = uriStr.split("/");
            // -1 is for such case ./Setting
            for (int j = 0; j < uriStrArray.length - 1; j++) {
                if (uriStrArray[j].equals(DmConst.TagName.Setting)
                        || uriStrArray[j].equals("DevInfo")) {
                    if ((itemStr != uriStrArray[j + 1])) {
                        itemStr = uriStrArray[j + 1];
                    }
                    break;
                }
            }
            NodeIoHandler ioHandler = null;
            Log.i(TAG.NodeIOHandler, "registering item:" + itemStr + "(" + uriStr + ")");
            if (itemStr != null) {
                ioHandler = mHandlerFactory.createNodeHandler(itemStr, Uri.parse(uriStr), mMccMnc);
            }
            if (ioHandler != null) {
                try {
                    mDmTree.registerNodeIoHandler(uriStr, ioHandler);
                } catch (MdmException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private List<String> getCCUriString(DmXMLParser xmlParser, Node node, String nodeName) {
        List<Node> nodeList = new ArrayList<Node>();
        List<String> uriStrList = new ArrayList<String>();
        xmlParser.getChildNode(node, nodeList, nodeName);
        for (int i = 0; i < nodeList.size(); i++) {
            Node tempnode = nodeList.get(i).getParentNode();
            String uriStr = getDmTreeNodeName(xmlParser, tempnode);
            while (tempnode != null) {
                tempnode = tempnode.getParentNode();
                String parentsName = getDmTreeNodeName(xmlParser, tempnode);
                if (parentsName != null && parentsName.equals(".")) {
                    uriStr = "./" + uriStr;
                    break;
                }
                uriStr = parentsName + "/" + uriStr;
            }
            if (uriStr != null) {
                if (uriStrList.size() > 0 && uriStr.contains(uriStrList.get(uriStrList.size() - 1))) {
                    uriStrList.remove(uriStrList.size() - 1);
                }
                uriStrList.add(uriStr);
            }
        }
        return uriStrList;
    }

    private String getDmTreeNodeName(DmXMLParser xmlParser, Node node) {
        if (node == null) {
            return null;
        }
        List<Node> nodeList = new ArrayList<Node>();
        xmlParser.getLeafNode(node, nodeList, DmConst.TagName.Name);
        if (nodeList != null && nodeList.size() > 0) {
            return nodeList.get(0).getFirstChild().getNodeValue();
        } else {
            return null;
        }
    }

    private Node getSettingNode(DmXMLParser xmlParser) {
        // Here the outside loop is for the change of Setting node's
        // level start from 1 is to reduce recursive times
        // Anyway, the level should be less than 15, if reasonable
        for (int level = 1; level < 0x000F; level++) {
            List<Node> nodeList = new ArrayList<Node>();
            xmlParser.getChildNodeAtLevel(nodeList, level);
            for (int i = 0; i < nodeList.size(); i++) {
                String nodeName = getDmTreeNodeName(xmlParser, nodeList.get(i));
                if (nodeName != null && nodeName.equals(DmConst.TagName.Setting)) {
                    return nodeList.get(i);
                }
            }
        }
        return null;
    }

}

class NodeIoHandlerFactory {
    private Context mContext = null;

    // It seems no need to singleton this.
    // private static NodeIoHandlerFactory nodeIoHandlerFact = null;

    // public static NodeIoHandlerFactory creatInstance(Context context,String
    // operator){
    // if(nodeIoHandlerFact == null){
    // nodeIoHandlerFact = new NodeIoHandlerFactory(context, operator);
    // }
    // return nodeIoHandlerFact;
    // }

    public NodeIoHandlerFactory(Context context) {
        mContext = context;
    }

    public NodeIoHandler createNodeHandler(String configItem, Uri uri, String parameterString) {
        Class ioHandlerClass = null;
        String className = "com.mediatek.MediatekDM.Dm" + configItem + "NodeIoHandler";
        Log.d(TAG.NodeIOHandler, "[NodeHandlerFactory]loading cls:" + className);
        try {
            ioHandlerClass = Class.forName(className);
        } catch (ClassNotFoundException e1) {
            Log.e(TAG.NodeIOHandler, "[NodeHandlerFactory]cls not found:" + className);
            return null;
        }
        Constructor conWith3Args = null;
        Constructor conWith2Args = null;
        try {
            conWith3Args = ioHandlerClass.getConstructor(Context.class, Uri.class, String.class);
        } catch (SecurityException e) {

            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            try {
                conWith2Args = ioHandlerClass.getConstructor(Context.class, Uri.class);
            } catch (SecurityException e1) {
                e1.printStackTrace();
                Log.e(TAG.NodeIOHandler, "constructor with 2 args security exception");
                return null;
            } catch (NoSuchMethodException e1) {
                e1.printStackTrace();
                Log.e(TAG.NodeIOHandler, "constructor with 2 args no such method");
                return null;
            }
        }
        try {
            if (conWith3Args != null) {
                Log.d(TAG.NodeIOHandler, "[NodeHandlerFactory]created: " + configItem + "(" + uri
                        + ","
                        + parameterString + ")");
                return (NodeIoHandler) (conWith3Args.newInstance(mContext, uri, parameterString));
            } else {
                if (conWith2Args != null) {
                    Log.d(TAG.NodeIOHandler, "[NodeHandlerFactory]created: " + configItem + "("
                            + uri + ")");
                    return (NodeIoHandler) (conWith2Args.newInstance(mContext, uri));
                }
            }
        } catch (IllegalArgumentException e) {

            e.printStackTrace();
        } catch (InstantiationException e) {

            e.printStackTrace();
        } catch (IllegalAccessException e) {

            e.printStackTrace();
        } catch (InvocationTargetException e) {

            e.printStackTrace();
        }
        // fatal error, not found corresponding IoHandler class;
        Log.w(TAG.NodeIOHandler, "[NodeHandlerFactory]creating IoHandler failed.");
        return null;
    }
}
