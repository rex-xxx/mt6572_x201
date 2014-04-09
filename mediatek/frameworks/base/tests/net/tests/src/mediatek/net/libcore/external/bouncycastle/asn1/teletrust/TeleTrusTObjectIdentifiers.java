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

package org.bouncycastle.asn1.teletrust;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;

public interface TeleTrusTObjectIdentifiers
{
    static final ASN1ObjectIdentifier teleTrusTAlgorithm = new ASN1ObjectIdentifier("1.3.36.3");

    static final ASN1ObjectIdentifier    ripemd160           = teleTrusTAlgorithm.branch("2.1");
    static final ASN1ObjectIdentifier    ripemd128           = teleTrusTAlgorithm.branch("2.2");
    static final ASN1ObjectIdentifier    ripemd256           = teleTrusTAlgorithm.branch("2.3");

    static final ASN1ObjectIdentifier teleTrusTRSAsignatureAlgorithm = teleTrusTAlgorithm.branch("3.1");

    static final ASN1ObjectIdentifier    rsaSignatureWithripemd160           = teleTrusTRSAsignatureAlgorithm.branch("2");
    static final ASN1ObjectIdentifier    rsaSignatureWithripemd128           = teleTrusTRSAsignatureAlgorithm.branch("3");
    static final ASN1ObjectIdentifier    rsaSignatureWithripemd256           = teleTrusTRSAsignatureAlgorithm.branch("4");

    static final ASN1ObjectIdentifier    ecSign = teleTrusTAlgorithm.branch("3.2");

    static final ASN1ObjectIdentifier    ecSignWithSha1  = ecSign.branch("1");
    static final ASN1ObjectIdentifier    ecSignWithRipemd160  = ecSign.branch("2");

    static final ASN1ObjectIdentifier ecc_brainpool = teleTrusTAlgorithm.branch("3.2.8");
    static final ASN1ObjectIdentifier ellipticCurve = ecc_brainpool.branch("1");
    static final ASN1ObjectIdentifier versionOne = ellipticCurve.branch("1");

    static final ASN1ObjectIdentifier brainpoolP160r1 = versionOne.branch("1");
    static final ASN1ObjectIdentifier brainpoolP160t1 = versionOne.branch("2");
    static final ASN1ObjectIdentifier brainpoolP192r1 = versionOne.branch("3");
    static final ASN1ObjectIdentifier brainpoolP192t1 = versionOne.branch("4");
    static final ASN1ObjectIdentifier brainpoolP224r1 = versionOne.branch("5");
    static final ASN1ObjectIdentifier brainpoolP224t1 = versionOne.branch("6");
    static final ASN1ObjectIdentifier brainpoolP256r1 = versionOne.branch("7");
    static final ASN1ObjectIdentifier brainpoolP256t1 = versionOne.branch("8");
    static final ASN1ObjectIdentifier brainpoolP320r1 = versionOne.branch("9");
    static final ASN1ObjectIdentifier brainpoolP320t1 = versionOne.branch("10");
    static final ASN1ObjectIdentifier brainpoolP384r1 = versionOne.branch("11");
    static final ASN1ObjectIdentifier brainpoolP384t1 = versionOne.branch("12");
    static final ASN1ObjectIdentifier brainpoolP512r1 = versionOne.branch("13");
    static final ASN1ObjectIdentifier brainpoolP512t1 = versionOne.branch("14");
}
