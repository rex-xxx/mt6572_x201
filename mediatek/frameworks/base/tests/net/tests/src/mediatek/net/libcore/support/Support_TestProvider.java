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
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tests.support;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Provider;

/**
 * This class implements a dummy provider.
 */
public class Support_TestProvider extends Provider {
    private static final long serialVersionUID = 1L;

    // Provider name
    private static final String NAME = "TestProvider";

    // Version of the services provided
    private static final double VERSION = 1.0;

    private static final String INFO = NAME
            + " DSA key, parameter generation and signing; SHA-1 digest; "
            + "SHA1PRNG SecureRandom; PKCS#12/Netscape KeyStore";

    /**
     * Constructs a new instance of the dummy provider.
     */
    public Support_TestProvider() {
        super(NAME, VERSION, INFO);
        registerServices();
    }

    /**
     * Register the services the receiver provides.
     */
    private void registerServices() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                // Digest engine
                put("MessageDigest.SHA",
                        "made.up.provider.name.MessageDigestSHA");
                put("MessageDigest.MD5",
                        "made.up.provider.name.MessageDigestMD5");
                // aliases
                put("Alg.Alias.MessageDigest.SHA1", "SHA");
                put("Alg.Alias.MessageDigest.SHA-1", "SHA");
                put("Alg.Alias.MessageDigest.OID.1.3.14.3.2.26", "SHA");
                put("Alg.Alias.MessageDigest.1.3.14.3.2.26", "SHA");

                // Algorithm parameter generator
                put("AlgorithmParameterGenerator.DSA",
                        "made.up.provider.name.AlgorithmParameterGeneratorDSA");

                // Algorithm parameters
                put("AlgorithmParameters.DSA",
                        "made.up.provider.name.AlgorithmParametersDSA");
                // aliases
                put("Alg.Alias.AlgorithmParameters.1.2.840.10040.4.1", "DSA");
                put("Alg.Alias.AlgorithmParameters.1.3.14.3.2.12", "DSA");

                // Key pair generator
                put("KeyPairGenerator.DSA",
                        "made.up.provider.name.KeyPairGeneratorDSA");
                // aliases
                put("Alg.Alias.KeyPairGenerator.OID.1.2.840.10040.4.1", "DSA");
                put("Alg.Alias.KeyPairGenerator.1.2.840.10040.4.1", "DSA");
                put("Alg.Alias.KeyPairGenerator.1.3.14.3.2.12", "DSA");

                // Key factory
                put("KeyFactory.DSA", "made.up.provider.name.KeyFactoryDSA");
                put("KeyFactory.RSA", "made.up.provider.name.KeyFactoryRSA");
                // aliases
                put("Alg.Alias.KeyFactory.1.2.840.10040.4.1", "DSA");
                put("Alg.Alias.KeyFactory.1.3.14.3.2.12", "DSA");

                // Signature algorithm
                put("Signature.SHA1withDSA",
                        "made.up.provider.name.SignatureDSA");

                // aliases
                put("Alg.Alias.Signature.DSA", "SHA1withDSA");
                put("Alg.Alias.Signature.DSS", "SHA1withDSA");
                put("Alg.Alias.Signature.SHA/DSA", "SHA1withDSA");
                put("Alg.Alias.Signature.SHA1/DSA", "SHA1withDSA");
                put("Alg.Alias.Signature.SHA-1/DSA", "SHA1withDSA");
                put("Alg.Alias.Signature.SHAwithDSA", "SHA1withDSA");
                put("Alg.Alias.Signature.DSAwithSHA1", "SHA1withDSA");
                put("Alg.Alias.Signature.DSAWithSHA1", "SHA1withDSA");
                put("Alg.Alias.Signature.SHA-1withDSA", "SHA1withDSA");
                put("Alg.Alias.Signature.OID.1.2.840.10040.4.3", "SHA1withDSA");
                put("Alg.Alias.Signature.1.2.840.10040.4.3", "SHA1withDSA");
                put("Alg.Alias.Signature.1.3.14.3.2.13", "SHA1withDSA");
                put("Alg.Alias.Signature.1.3.14.3.2.27", "SHA1withDSA");
                put("Alg.Alias.Signature.OID.1.3.14.3.2.13", "SHA1withDSA");
                put("Alg.Alias.Signature.OID.1.3.14.3.2.27", "SHA1withDSA");

                put("KeyStore.PKCS#12/Netscape",
                        "tests.support.Support_DummyPKCS12Keystore");

                // Certificate
                put("CertificateFactory.X509",
                        "made.up.provider.name.CertificateFactoryX509");
                // aliases
                put("Alg.Alias.CertificateFactory.X.509", "X509");

                return null;
            }
        });
    }
}