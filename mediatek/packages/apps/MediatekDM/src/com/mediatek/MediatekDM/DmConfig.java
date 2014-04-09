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
import android.net.Proxy;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.conn.DmDatabase;
import com.mediatek.MediatekDM.mdm.MdmConfig;
import com.mediatek.MediatekDM.mdm.MdmConfig.DmAccConfiguration;
import com.mediatek.MediatekDM.mdm.MdmConfig.HttpAuthLevel;
import com.mediatek.MediatekDM.mdm.MdmException;
import com.mediatek.MediatekDM.option.Options;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DmConfig {
    public DmConfig(Context ctx) {
        context = ctx;
        mConfig = new MdmConfig();
        try {
            InputStream cfgStream = new FileInputStream(mConfigFile);

            if (cfgStream != null) {
                mParamTable = new Properties();
                mParamTable.loadFromXML(cfgStream);
                cfgStream.close();
            }
        } catch (IOException e) {
            Log.w(TAG.Common, "MdmcConfig:Caught exception " + e.getMessage());
        }
    }

    public void configure() {
        try {
            if (Options.UseDirectInternet) {
                // for ZTE DM server(via wifi/net), MUST not be set!
                Log.i(TAG.Common, "[DMConfig] skip setting proxy for direct internet.");
            } else {
                // for cmcc/cu DM server, proxy MUST be set!
                Log.i(TAG.Common, "[DMConfig] setting proxy for WAP.");

                DmDatabase dmDB = new DmDatabase(context);
                String proxyAddr = dmDB.getApnProxyFromSettings();
                int proxyPort = dmDB.getApnProxyPortFromSettings();
                Log.i(TAG.Common, "Proxy addr = " + proxyAddr + ", port = " + proxyPort);

                if (Proxy.getDefaultHost() != null) {
                    mConfig.setDmProxy("http://" + Proxy.getDefaultHost() + ":"
                            + Proxy.getDefaultPort());
                    mConfig.setDlProxy("http://" + Proxy.getDefaultHost() + ":"
                            + Proxy.getDefaultPort());

                } else if (proxyAddr != null && proxyPort > 0) {
                    mConfig.setDmProxy("http://" + proxyAddr + ":" + proxyPort);
                    mConfig.setDlProxy("http://" + proxyAddr + ":" + proxyPort);
                } else {
                    Log.w(TAG.Common, "DM_PROXY not configed");
                }
            }

            mConfig.setEncodeWBXMLMsg(false);
            // Use application level retry
            mConfig.setMaxNetRetries(3);
            mConfig.setDDVersionCheck(false);
            mConfig.setIgnoreMissingETag();
            mConfig.setNotificationVerificationMode(MdmConfig.NotifVerificationMode.DISABLED);
            if (mParamTable != null) {

                configureDMAcc();

                if (mParamTable.containsKey(DM_HEXSID)) {
                    String dmServer = mParamTable.getProperty(DM_HEXSID);
                    if (dmServer != null) {
                        if (dmServer.equals("true")) {
                            mConfig.setSessionIDAsDec(false);
                        } else {
                            mConfig.setSessionIDAsDec(true);
                        }
                    }
                } else {
                    mConfig.setSessionIDAsDec(true);
                    Log.w(TAG.Common, "DM_HEXSID not configed");
                }

                if (mParamTable.containsKey(DM_PROXY)) {
                    String dmProxy = mParamTable.getProperty(DM_PROXY);
                    mConfig.setDmProxy(dmProxy);
                } else {
                    Log.w(TAG.Common, "DM_PROXY not configed");
                }

                if (mParamTable.containsKey(DL_PROXY)) {
                    String dlProxy = mParamTable.getProperty(DL_PROXY);
                    mConfig.setDlProxy(dlProxy);
                } else {
                    Log.w(TAG.Common, "DL_PROXY not configed");
                }

                if (mParamTable.containsKey(DL_EXT)) {
                    String dl_ext = mParamTable.getProperty(DL_EXT);
                    if (dl_ext != null) {
                        if (dl_ext.equals(DDL)) {
                            mIsDDLExtSet = true;
                        }
                    }
                } else {
                    Log.w(TAG.Common, "DL_EXT not configed");
                }

                if (mParamTable.containsKey(ENCODE)) {
                    String encode = mParamTable.getProperty(ENCODE);
                    if (encode != null) {
                        if (encode.compareTo(XML) == 0) {
                            Log.i(TAG.Common, "Call mConfig.setEncodeWBXMLMsg(false)");
                            mConfig.setEncodeWBXMLMsg(false);
                        }
                    }
                } else {
                    Log.w(TAG.Common, "ENCODE not configed");
                }

                if (mParamTable.containsKey(SERVER_202_UNSUPPORTED)) {
                    String server202NotSupported = mParamTable.getProperty(SERVER_202_UNSUPPORTED);
                    if (server202NotSupported != null) {
                        if (server202NotSupported.charAt(0) == 'T'
                                || server202NotSupported.charAt(0) == 't') {
                            mConfig.set202statusCodeNotSupportedByServer(true);
                        }
                    }
                } else {
                    Log.w(TAG.Common, "SERVER_202_UNSUPPORTED not configed");
                }

                if (mParamTable.containsKey(INSTALL_NOTIFY_SUCCESS_ONLY)) {
                    String installNotify = mParamTable.getProperty(INSTALL_NOTIFY_SUCCESS_ONLY);
                    if (installNotify != null) {
                        if (installNotify.charAt(0) == 'T' || installNotify.charAt(0) == 't') {
                            mConfig.setInstallNotifySuccessOnly(true);
                        }
                    }
                } else {
                    Log.w(TAG.Common, "INSTALL_NOTIFY_SUCCESS_ONLY not configed");
                }

                configureHttpAuth(true); // DM HTTP Authentication
                configureHttpAuth(false); // DL HTTP Authentication
                configureProxyAuth(true); // DM Proxy Authentication
                configureProxyAuth(false); // DL Proxy Authentication
            } else {
                mConfig.setSessionIDAsDec(true);
            }
        } catch (MdmException e) {
            Log.w(TAG.Common, "MdmcConfig:Caught exception " + e.getMessage());
        }
    }

    public boolean isDDLExtSet() {
        return mIsDDLExtSet;
    }

    private void configureDMAcc() throws MdmException {
        DmAccConfiguration dmacc = mConfig.new DmAccConfiguration();
        dmacc.activeAccountDmVersion = MdmConfig.DmVersion.DM_1_2;
        dmacc.dm12root = "./DMAcc/OMSAcc";
        dmacc.isExclusive = false;
        dmacc.updateInactiveDmAccount = false;
        mConfig.setDmAccConfiguration(dmacc);
    }

    private void configureHttpAuth(boolean isDM) throws MdmException {

        String[] auth = isDM ? DM_HTTP_AUTH : DL_HTTP_AUTH;
        String DMDL = isDM ? "DM" : "DL";

        if (mParamTable.containsKey(auth[HTTP_AUTH_LEVEL])) {

            String level = mParamTable.getProperty(auth[HTTP_AUTH_LEVEL]);
            if (level == null) {
                return;
            }
            if (level.equalsIgnoreCase("none")) {
                if (isDM) {
                    mConfig.setDmHttpAuthentication(HttpAuthLevel.NONE, null, null);
                } else {
                    mConfig.setDlHttpAuthentication(HttpAuthLevel.NONE, null, null);
                }
            } else {
                String username = mParamTable.getProperty(auth[HTTP_AUTH_UNAME]);
                String password = mParamTable.getProperty(auth[HTTP_AUTH_PWD]);

                if (username == null || password == null) {
                    Log.w(TAG.Common, "Missing credentials for " + DMDL + " HTTP Authentication");
                    throw new MdmException(MdmException.MdmError.BAD_INPUT);
                }
                if (level.equalsIgnoreCase("digest")) {
                    Log.w(TAG.Common, DMDL + " HTTP Authentication 'Digest' is not supported");
                    throw new MdmException(MdmException.MdmError.NOT_IMPLEMENTED);
                }
                if (level.equalsIgnoreCase("basic") == false) {
                    Log.w(TAG.Common, "Invalid " + DMDL + " HTTP Authentication " + level);
                    throw new MdmException(MdmException.MdmError.BAD_INPUT);
                }

                if (isDM) {
                    mConfig.setDmHttpAuthentication(HttpAuthLevel.BASIC, username, password);
                } else {
                    mConfig.setDlHttpAuthentication(HttpAuthLevel.BASIC, username, password);
                }
            }
        }
    }

    private void configureProxyAuth(boolean isDM) throws MdmException {

        String[] auth = isDM ? DM_PROXY_AUTH : DL_PROXY_AUTH;
        String DMDL = isDM ? "DM" : "DL";

        if (mParamTable.containsKey(auth[HTTP_AUTH_LEVEL])) {

            Log.w(TAG.Common, "auth[HTTP_AUTH_LEVEL] was configed");

            String level = mParamTable.getProperty(auth[HTTP_AUTH_LEVEL]);
            if (level == null) {
                return;
            }

            if (level.equalsIgnoreCase("none")) {
                Log.w(TAG.Common, "level was configed");
                if (isDM) {
                    mConfig.setDmProxyAuthentication(HttpAuthLevel.NONE, null, null);
                } else {
                    mConfig.setDlProxyAuthentication(HttpAuthLevel.NONE, null, null);
                }
            } else {
                Log.w(TAG.Common, "level was NOT configed");
                String username = mParamTable.getProperty(auth[HTTP_AUTH_UNAME]);
                String password = mParamTable.getProperty(auth[HTTP_AUTH_PWD]);

                if (username == null || password == null) {
                    Log.w(TAG.Common, "Missing credentials for " + DMDL + " HTTP Authentication");
                    throw new MdmException(MdmException.MdmError.BAD_INPUT);
                }
                if (level.equalsIgnoreCase("digest")) {
                    Log.w(TAG.Common, DMDL + " Proxy Authentication 'Digest' is not supported");
                    throw new MdmException(MdmException.MdmError.NOT_IMPLEMENTED);
                }
                if (level.equalsIgnoreCase("basic") == false) {
                    Log.w(TAG.Common, "Invalid " + DMDL + " Proxy Authentication " + level);
                    throw new MdmException(MdmException.MdmError.BAD_INPUT);
                }

                if (isDM) {
                    mConfig.setDmProxyAuthentication(HttpAuthLevel.BASIC, username, password);
                } else {
                    mConfig.setDlProxyAuthentication(HttpAuthLevel.BASIC, username, password);
                }
            }
        } else {
            Log.w(TAG.Common, "auth not configed");
        }
    }

    private Context context;
    private MdmConfig mConfig;
    private Properties mParamTable;
    private String mConfigFile = "/system/etc/dm/config.xml";
    private boolean mIsDDLExtSet = false;

    private static final String DL_EXT = "dl_ext";
    private static final String DDL = "ddl";
    private static final String ENCODE = "encode";
    private static final String XML = "xml";
    private static final String DL_PROXY = "dlproxy";
    private static final String DM_PROXY = "dmproxy";
    private static final String DM_VERSION = "dmversion";
    private static final String ACC_ROOT = "12accountroot";
    private static final String EXCLUSIVE = "exclusive";
    private static final String UPDATE_INACTIVE_ACC = "updateinactiveaccount";
    private static final String DM_SERVER = "dmserver";
    private static final String INSTALL_NOTIFY_SUCCESS_ONLY = "installnotifysuccessonly";
    private static final String SERVER_202_UNSUPPORTED = "server_202_unsupported";
    private static final String DM_HEXSID = "hexsid";

    // HTTP/Proxy Authentication
    private static final int HTTP_AUTH_LEVEL = 0;
    private static final int HTTP_AUTH_UNAME = 1;
    private static final int HTTP_AUTH_PWD = 2;
    private static final String DM_HTTP_AUTH[] = {
            "dm_http_auth_level", "dm_http_auth_username", "dm_http_auth_password"
    };
    private static final String DL_HTTP_AUTH[] = {
            "dl_http_auth_level", "dl_http_auth_username", "dl_http_auth_password"
    };
    private static final String DM_PROXY_AUTH[] = {
            "dm_proxy_auth_level", "dm_proxy_auth_username", "dm_proxy_auth_password"
    };
    private static final String DL_PROXY_AUTH[] = {
            "dl_proxy_auth_level", "dl_proxy_auth_username", "dl_proxy_auth_password"
    };
}
