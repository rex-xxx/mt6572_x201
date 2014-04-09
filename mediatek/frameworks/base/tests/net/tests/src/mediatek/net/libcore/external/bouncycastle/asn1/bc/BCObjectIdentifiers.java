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

package org.bouncycastle.asn1.bc;

import org.bouncycastle.asn1.DERObjectIdentifier;

public interface BCObjectIdentifiers
{
    /**
     *  iso.org.dod.internet.private.enterprise.legion-of-the-bouncy-castle
     *
     *  1.3.6.1.4.1.22554
     */
    public static final DERObjectIdentifier bc = new DERObjectIdentifier("1.3.6.1.4.1.22554");

    /**
     * pbe(1) algorithms
     */
    public static final DERObjectIdentifier bc_pbe = new DERObjectIdentifier(bc.getId() + ".1");

    /**
     * SHA-1(1)
     */
    public static final DERObjectIdentifier bc_pbe_sha1 = new DERObjectIdentifier(bc_pbe.getId() + ".1");

    /**
     * SHA-2(2) . (SHA-256(1)|SHA-384(2)|SHA-512(3)|SHA-224(4))
     */
    public static final DERObjectIdentifier bc_pbe_sha256 = new DERObjectIdentifier(bc_pbe.getId() + ".2.1");
    public static final DERObjectIdentifier bc_pbe_sha384 = new DERObjectIdentifier(bc_pbe.getId() + ".2.2");
    public static final DERObjectIdentifier bc_pbe_sha512 = new DERObjectIdentifier(bc_pbe.getId() + ".2.3");
    public static final DERObjectIdentifier bc_pbe_sha224 = new DERObjectIdentifier(bc_pbe.getId() + ".2.4");

    /**
     * PKCS-5(1)|PKCS-12(2)
     */
    public static final DERObjectIdentifier bc_pbe_sha1_pkcs5 = new DERObjectIdentifier(bc_pbe_sha1.getId() + ".1");
    public static final DERObjectIdentifier bc_pbe_sha1_pkcs12 = new DERObjectIdentifier(bc_pbe_sha1.getId() + ".2");

    public static final DERObjectIdentifier bc_pbe_sha256_pkcs5 = new DERObjectIdentifier(bc_pbe_sha256.getId() + ".1");
    public static final DERObjectIdentifier bc_pbe_sha256_pkcs12 = new DERObjectIdentifier(bc_pbe_sha256.getId() + ".2");

    /**
     * AES(1) . (CBC-128(2)|CBC-192(22)|CBC-256(42))
     */
    public static final DERObjectIdentifier bc_pbe_sha1_pkcs12_aes128_cbc = new DERObjectIdentifier(bc_pbe_sha1_pkcs12.getId() + ".1.2");
    public static final DERObjectIdentifier bc_pbe_sha1_pkcs12_aes192_cbc = new DERObjectIdentifier(bc_pbe_sha1_pkcs12.getId() + ".1.22");
    public static final DERObjectIdentifier bc_pbe_sha1_pkcs12_aes256_cbc = new DERObjectIdentifier(bc_pbe_sha1_pkcs12.getId() + ".1.42");

    public static final DERObjectIdentifier bc_pbe_sha256_pkcs12_aes128_cbc = new DERObjectIdentifier(bc_pbe_sha256_pkcs12.getId() + ".1.2");
    public static final DERObjectIdentifier bc_pbe_sha256_pkcs12_aes192_cbc = new DERObjectIdentifier(bc_pbe_sha256_pkcs12.getId() + ".1.22");
    public static final DERObjectIdentifier bc_pbe_sha256_pkcs12_aes256_cbc = new DERObjectIdentifier(bc_pbe_sha256_pkcs12.getId() + ".1.42");
}
