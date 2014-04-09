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

package org.bouncycastle.asn1.cryptopro;

import org.bouncycastle.asn1.DERObjectIdentifier;

public interface CryptoProObjectIdentifiers
{
    // GOST Algorithms OBJECT IDENTIFIERS :
    // { iso(1) member-body(2) ru(643) rans(2) cryptopro(2)}
    static final String                 GOST_id              = "1.2.643.2.2";

    static final DERObjectIdentifier    gostR3411          = new DERObjectIdentifier(GOST_id+".9");
    
    static final DERObjectIdentifier    gostR28147_cbc     = new DERObjectIdentifier(GOST_id+".21");

    static final DERObjectIdentifier    gostR3410_94       = new DERObjectIdentifier(GOST_id+".20");
    static final DERObjectIdentifier    gostR3410_2001     = new DERObjectIdentifier(GOST_id+".19");
    static final DERObjectIdentifier    gostR3411_94_with_gostR3410_94   = new DERObjectIdentifier(GOST_id+".4");
    static final DERObjectIdentifier    gostR3411_94_with_gostR3410_2001 = new DERObjectIdentifier(GOST_id+".3");

    // { iso(1) member-body(2) ru(643) rans(2) cryptopro(2) hashes(30) }
    static final DERObjectIdentifier    gostR3411_94_CryptoProParamSet = new DERObjectIdentifier(GOST_id+".30.1");

    // { iso(1) member-body(2) ru(643) rans(2) cryptopro(2) signs(32) }
    static final DERObjectIdentifier    gostR3410_94_CryptoPro_A     = new DERObjectIdentifier(GOST_id+".32.2");
    static final DERObjectIdentifier    gostR3410_94_CryptoPro_B     = new DERObjectIdentifier(GOST_id+".32.3");
    static final DERObjectIdentifier    gostR3410_94_CryptoPro_C     = new DERObjectIdentifier(GOST_id+".32.4");
    static final DERObjectIdentifier    gostR3410_94_CryptoPro_D     = new DERObjectIdentifier(GOST_id+".32.5");

    // { iso(1) member-body(2) ru(643) rans(2) cryptopro(2) exchanges(33) }
    static final DERObjectIdentifier    gostR3410_94_CryptoPro_XchA  = new DERObjectIdentifier(GOST_id+".33.1");
    static final DERObjectIdentifier    gostR3410_94_CryptoPro_XchB  = new DERObjectIdentifier(GOST_id+".33.2");
    static final DERObjectIdentifier    gostR3410_94_CryptoPro_XchC  = new DERObjectIdentifier(GOST_id+".33.3");

    //{ iso(1) member-body(2)ru(643) rans(2) cryptopro(2) ecc-signs(35) }
    static final DERObjectIdentifier    gostR3410_2001_CryptoPro_A = new DERObjectIdentifier(GOST_id+".35.1");
    static final DERObjectIdentifier    gostR3410_2001_CryptoPro_B = new DERObjectIdentifier(GOST_id+".35.2");
    static final DERObjectIdentifier    gostR3410_2001_CryptoPro_C = new DERObjectIdentifier(GOST_id+".35.3");

    // { iso(1) member-body(2) ru(643) rans(2) cryptopro(2) ecc-exchanges(36) }
    static final DERObjectIdentifier    gostR3410_2001_CryptoPro_XchA  = new DERObjectIdentifier(GOST_id+".36.0");
    static final DERObjectIdentifier    gostR3410_2001_CryptoPro_XchB  = new DERObjectIdentifier(GOST_id+".36.1");
    
    static final DERObjectIdentifier    gost_ElSgDH3410_default    = new DERObjectIdentifier(GOST_id+".36.0");
    static final DERObjectIdentifier    gost_ElSgDH3410_1          = new DERObjectIdentifier(GOST_id+".36.1");
}
