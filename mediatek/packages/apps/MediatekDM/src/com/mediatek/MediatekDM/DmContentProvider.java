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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class DmContentProvider extends ContentProvider {

    private Document docTree = null;
    private Document docAPN = null;
    private UriMatcher sUriMatcher;

    private String DM_CONTENT_URI = "com.mediatek.providers.mediatekdm";

    private String treeFile = DmConst.Path.PathInSystem + "/tree.xml";
    private String apnFile = DmConst.Path.PathInSystem + "/DmApnInfo.xml";

    private final int DM_CONTENT_CMWAP0 = 0;
    private final int DM_CONTENT_CMWAP1 = 1;
    private final int DM_CONTENT_DEVINFO = 2;
    private final int DM_CONTENT_OMSACC = 3;

    private String[] queryMask = {
            "cmwap0", "cmwap1", "DevInfo", "OMSAcc"
    };
    private String[] cmwapProj = {
            "name", "numeric", "mcc", "mnc", "apn", "server", "proxy", "port"
    };
    private String[] DevInfoProj = {
            "Mod", "Man"
    };
    private String[] OMSAccProj = {
            "Addr"
    };

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri arg0, ContentValues arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean onCreate() {

        Log.i(TAG.Provider, "DmContentProvider onCreate..");

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        for (int i = 0; i < queryMask.length; i++) {
            sUriMatcher.addURI(DM_CONTENT_URI, queryMask[i], i);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();

            Log.i(TAG.Provider, "DmContentProvider start to parse xml file : " + treeFile);
            File fTree = new File(treeFile);
            docTree = builder.parse(fTree);

            Log.i(TAG.Provider, "DmContentProvider start to parse xml file : " + apnFile);
            File fApn = new File(apnFile);
            docAPN = builder.parse(fApn);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return false;
        } catch (SAXException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3, String arg4) {

        Log.i(TAG.Provider, "DmContentProvider query..URI : " + arg0);

        switch (sUriMatcher.match(arg0)) {
            case DM_CONTENT_CMWAP0:
                return queryDMApn("0");
            case DM_CONTENT_CMWAP1:
                return queryDMApn("1");
            case DM_CONTENT_DEVINFO:
                return queryDMDevInfo("DevInfo");
            case DM_CONTENT_OMSACC:
                return queryDMOmsacc("OMSAcc");
            default:
                throw new IllegalArgumentException("Illegal URI : " + arg0);
        }
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        // TODO Auto-generated method stub
        return 0;
    }

    private Document getDocument(String xmlFile) {
        Document doc = null;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();

            Log.i(TAG.Provider, "DmContentProvider start to parse xml file : " + xmlFile);
            File xfile = new File(xmlFile);
            doc = builder.parse(xfile);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return doc;
    }

    private MatrixCursor queryDMApn(String id) {

        Log.i(TAG.Provider, "DmContentProvider queryDMApn..cmwap" + id);
        /*
         * if (docAPN == null) docAPN = getDocument(apnFile);
         */
        if (docAPN == null) {
            Log.i(TAG.Provider, "-------docAPN is null!!!!!!" + id);
            return null;
        }

        NodeList nl = docAPN.getElementsByTagName("id");
        if (nl == null) {
            Log.i(TAG.Provider, "-------node list of tag <id> is null!!!!!!" + id);
            return null;
        }

        Node cmcc = nl.item(Integer.parseInt(id)).getParentNode();

        nl = cmcc.getChildNodes();
        int len = nl.getLength();
        Bundle bundle = new Bundle();

        for (int i = 0; i < len; i++) {
            String name = nl.item(i).getNodeName();
            String value = nl.item(i).getNodeValue();
            Log.i(TAG.Provider, "attributes " + i + " : name - " + name + ", value - " + value);
            bundle.putString(name, value);
        }

        String[] row = new String[cmwapProj.length];
        for (int i = 0; i < cmwapProj.length; i++) {
            row[i] = bundle.getString(cmwapProj[i]);
        }

        MatrixCursor cur = new MatrixCursor(cmwapProj);
        cur.addRow(row);
        return cur;
    }

    private MatrixCursor queryDMDevInfo(String id) {

        Log.i(TAG.Provider, "DmContentProvider queryDMDevInfo..");
        /*
         * if (docTree == null) docTree = getDocument(treeFile);
         */
        if (docTree == null) {
            Log.i(TAG.Provider, "-------docTree is null!!!!!!" + id);
            return null;
        }

        NodeList nl = docTree.getElementsByTagName("name");
        if (nl == null) {
            Log.i(TAG.Provider, "-------node list of tag <name> is null!!!!!!" + id);
            return null;
        }

        int len = nl.getLength();
        String[] row = new String[DevInfoProj.length];

        for (int i = 0; i < len; i++) {
            Node node = nl.item(i);
            for (int j = 0; j < DevInfoProj.length; j++) {
                if (DevInfoProj[j].equals(node.getNodeValue())) {
                    row[j] = node.getParentNode().getLastChild().getNodeValue();
                }
            }
        }

        MatrixCursor cur = new MatrixCursor(DevInfoProj);
        cur.addRow(row);
        return cur;
    }

    private MatrixCursor queryDMOmsacc(String id) {

        Log.i(TAG.Provider, "DmContentProvider queryDMOmsacc..");
        /*
         * if (docTree == null) docTree = getDocument(treeFile);
         */
        if (docTree == null) {
            Log.i(TAG.Provider, "-------docTree is null!!!!!!" + id);
            return null;
        }

        NodeList nodeList = docTree.getElementsByTagName("value");
        if (nodeList == null) {
            Log.i(TAG.Provider, "-------node list of tag <value> is null!!!!!!" + id);
            return null;
        }

        int length = nodeList.getLength();
        Log.i(TAG.Provider, "-------there are " + length + " nodes with tag <value>");

        String[] row = {
                ""
        };
        String tc;
        for (int i = 0; i < length; i++) {
            tc = nodeList.item(i).getTextContent();
            Log.i(TAG.Provider, "-------<value> [ " + i + " ] getTextContent : " + tc);
            if (tc.contains("http")) {
                row[0] = tc;
                break;
            }
        }

        MatrixCursor cur = new MatrixCursor(OMSAccProj);
        cur.addRow(row);
        return cur;
    }

}
