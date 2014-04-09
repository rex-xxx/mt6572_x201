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

/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package libcore.javax.net.ssl;

import java.io.PrintStream;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import libcore.java.io.NullPrintStream;
import libcore.java.security.StandardNames;

/**
 * TestTrustManager is a simple proxy class that wraps an existing
 * X509TrustManager to provide debug logging and recording of
 * values.
 */
public final class TestTrustManager implements X509TrustManager {

    private static final boolean LOG = false;
    private static final PrintStream out = LOG ? System.out : new NullPrintStream();

    private final X509TrustManager trustManager;

    public static TrustManager[] wrap(TrustManager[] trustManagers) {
        TrustManager[] result = trustManagers.clone();
        for (int i = 0; i < result.length; i++) {
            result[i] = wrap(result[i]);
        }
        return result;
    }

    public static TrustManager wrap(TrustManager trustManager) {
        if (!(trustManager instanceof X509TrustManager)) {
            return trustManager;
        }
        return new TestTrustManager((X509TrustManager) trustManager);
    }

    public TestTrustManager(X509TrustManager trustManager) {
        out.println("TestTrustManager.<init> trustManager=" + trustManager);
        this.trustManager = trustManager;
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        out.print("TestTrustManager.checkClientTrusted "
                  + "chain=" + chain.length + " "
                  + "authType=" + authType + " ");
        try {
            assertClientAuthType(authType);
            trustManager.checkClientTrusted(chain, authType);
            out.println("OK");
        } catch (CertificateException e) {
            e.printStackTrace(out);
            throw e;
        }
    }

    private void assertClientAuthType(String authType) {
        if (!StandardNames.CLIENT_AUTH_TYPES.contains(authType)) {
            throw new AssertionError("Unexpected client auth type " + authType);
        }
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        out.print("TestTrustManager.checkServerTrusted "
                  + "chain=" + chain.length + " "
                  + "authType=" + authType + " ");
        try {
            assertServerAuthType(authType);
            trustManager.checkServerTrusted(chain, authType);
            out.println("OK");
        } catch (CertificateException e) {
            e.printStackTrace(out);
            throw e;
        }
    }

    private void assertServerAuthType(String authType) {
        if (!StandardNames.SERVER_AUTH_TYPES.contains(authType)) {
            throw new AssertionError("Unexpected server auth type " + authType);
        }
    }

    /**
     * Returns the list of certificate issuer authorities which are trusted for
     * authentication of peers.
     *
     * @return the list of certificate issuer authorities which are trusted for
     *         authentication of peers.
     */
    public X509Certificate[] getAcceptedIssuers() {
        X509Certificate[] result = trustManager.getAcceptedIssuers();
        out.print("TestTrustManager.getAcceptedIssuers result=" + result.length);
        return result;
    }
}

