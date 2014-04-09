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

package org.bouncycastle.jce.provider.asymmetric;

import java.util.HashMap;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
// BEGIN android-removed
// import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
// import org.bouncycastle.asn1.teletrust.TeleTrusTObjectIdentifiers;
// END android-removed
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;

public class EC
{
    public static class Mappings
        extends HashMap
    {
        public Mappings()
        {
            put("KeyAgreement.ECDH", "org.bouncycastle.jce.provider.asymmetric.ec.KeyAgreement$DH");
            // BEGIN android-removed
            // put("KeyAgreement.ECDHC", "org.bouncycastle.jce.provider.asymmetric.ec.KeyAgreement$DHC");
            // put("KeyAgreement.ECMQV", "org.bouncycastle.jce.provider.asymmetric.ec.KeyAgreement$MQV");
            // put("KeyAgreement." + X9ObjectIdentifiers.dhSinglePass_stdDH_sha1kdf_scheme, "org.bouncycastle.jce.provider.asymmetric.ec.KeyAgreement$DHwithSHA1KDF");
            // put("KeyAgreement." + X9ObjectIdentifiers.mqvSinglePass_sha1kdf_scheme, "org.bouncycastle.jce.provider.asymmetric.ec.KeyAgreement$MQVwithSHA1KDF");
            // END android-removed

            put("KeyFactory.EC", "org.bouncycastle.jce.provider.asymmetric.ec.KeyFactory$EC");
            // BEGIN android-removed
            // put("KeyFactory.ECDSA", "org.bouncycastle.jce.provider.asymmetric.ec.KeyFactory$ECDSA");
            // put("KeyFactory.ECDH", "org.bouncycastle.jce.provider.asymmetric.ec.KeyFactory$ECDH");
            // put("KeyFactory.ECDHC", "org.bouncycastle.jce.provider.asymmetric.ec.KeyFactory$ECDHC");
            // put("KeyFactory.ECMQV", "org.bouncycastle.jce.provider.asymmetric.ec.KeyFactory$ECMQV");
            // END android-removed
            put("Alg.Alias.KeyFactory." + X9ObjectIdentifiers.id_ecPublicKey, "EC");
            // TODO Should this be an alias for ECDH?
            put("Alg.Alias.KeyFactory." + X9ObjectIdentifiers.dhSinglePass_stdDH_sha1kdf_scheme, "EC");
            // BEGIN android-removed
            // put("Alg.Alias.KeyFactory." + X9ObjectIdentifiers.mqvSinglePass_sha1kdf_scheme, "ECMQV");
            //
            // put("KeyFactory.ECGOST3410", "org.bouncycastle.jce.provider.asymmetric.ec.KeyFactory$ECGOST3410");
            // put("Alg.Alias.KeyFactory.GOST-3410-2001", "ECGOST3410");
            // put("Alg.Alias.KeyFactory.ECGOST-3410", "ECGOST3410");
            // put("Alg.Alias.KeyFactory." + CryptoProObjectIdentifiers.gostR3410_2001, "ECGOST3410");
            // END android-removed

            put("KeyPairGenerator.EC", "org.bouncycastle.jce.provider.asymmetric.ec.KeyPairGenerator$EC");
            // BEGIN android-removed
            // put("KeyPairGenerator.ECDSA", "org.bouncycastle.jce.provider.asymmetric.ec.KeyPairGenerator$ECDSA");
            // put("KeyPairGenerator.ECDH", "org.bouncycastle.jce.provider.asymmetric.ec.KeyPairGenerator$ECDH");
            // put("KeyPairGenerator.ECDHC", "org.bouncycastle.jce.provider.asymmetric.ec.KeyPairGenerator$ECDHC");
            // put("KeyPairGenerator.ECIES", "org.bouncycastle.jce.provider.asymmetric.ec.KeyPairGenerator$ECDH");
            // put("KeyPairGenerator.ECMQV", "org.bouncycastle.jce.provider.asymmetric.ec.KeyPairGenerator$ECMQV");
            // END android-removed
            // TODO Should this be an alias for ECDH?
            put("Alg.Alias.KeyPairGenerator." + X9ObjectIdentifiers.dhSinglePass_stdDH_sha1kdf_scheme, "EC");
            // BEGIN android-removed
            // put("Alg.Alias.KeyPairGenerator." + X9ObjectIdentifiers.mqvSinglePass_sha1kdf_scheme, "ECMQV");
            //
            // put("KeyPairGenerator.ECGOST3410", "org.bouncycastle.jce.provider.asymmetric.ec.KeyPairGenerator$ECGOST3410");
            // put("Alg.Alias.KeyPairGenerator.ECGOST-3410", "ECGOST3410");
            // put("Alg.Alias.KeyPairGenerator.GOST-3410-2001", "ECGOST3410");
            // END android-removed

            put("Signature.ECDSA", "org.bouncycastle.jce.provider.asymmetric.ec.Signature$ecDSA");
            put("Signature.NONEwithECDSA", "org.bouncycastle.jce.provider.asymmetric.ec.Signature$ecDSAnone");

            put("Alg.Alias.Signature.SHA1withECDSA", "ECDSA");
            put("Alg.Alias.Signature.ECDSAwithSHA1", "ECDSA");
            put("Alg.Alias.Signature.SHA1WITHECDSA", "ECDSA");
            put("Alg.Alias.Signature.ECDSAWITHSHA1", "ECDSA");
            put("Alg.Alias.Signature.SHA1WithECDSA", "ECDSA");
            put("Alg.Alias.Signature.ECDSAWithSHA1", "ECDSA");
            put("Alg.Alias.Signature.1.2.840.10045.4.1", "ECDSA");
            // BEGIN android-removed
            // put("Alg.Alias.Signature." + TeleTrusTObjectIdentifiers.ecSignWithSha1, "ECDSA");
            //
            // addSignatureAlgorithm("SHA224", "ECDSA", "org.bouncycastle.jce.provider.asymmetric.ec.Signature$ecDSA224", X9ObjectIdentifiers.ecdsa_with_SHA224);
            // END android-removed
            addSignatureAlgorithm("SHA256", "ECDSA", "org.bouncycastle.jce.provider.asymmetric.ec.Signature$ecDSA256", X9ObjectIdentifiers.ecdsa_with_SHA256);
            addSignatureAlgorithm("SHA384", "ECDSA", "org.bouncycastle.jce.provider.asymmetric.ec.Signature$ecDSA384", X9ObjectIdentifiers.ecdsa_with_SHA384);
            addSignatureAlgorithm("SHA512", "ECDSA", "org.bouncycastle.jce.provider.asymmetric.ec.Signature$ecDSA512", X9ObjectIdentifiers.ecdsa_with_SHA512);
            // BEGIN android-removed
            // addSignatureAlgorithm("RIPEMD160", "ECDSA", "org.bouncycastle.jce.provider.asymmetric.ec.Signature$ecDSARipeMD160",TeleTrusTObjectIdentifiers.ecSignWithRipemd160);
            //
            // put("Signature.SHA1WITHECNR", "org.bouncycastle.jce.provider.asymmetric.ec.Signature$ecNR");
            // put("Signature.SHA224WITHECNR", "org.bouncycastle.jce.provider.asymmetric.ec.Signature$ecNR224");
            // put("Signature.SHA256WITHECNR", "org.bouncycastle.jce.provider.asymmetric.ec.Signature$ecNR256");
            // put("Signature.SHA384WITHECNR", "org.bouncycastle.jce.provider.asymmetric.ec.Signature$ecNR384");
            // put("Signature.SHA512WITHECNR", "org.bouncycastle.jce.provider.asymmetric.ec.Signature$ecNR512");
            //
            // addSignatureAlgorithm("SHA1", "CVC-ECDSA", "org.bouncycastle.jce.provider.asymmetric.ec.Signature$ecCVCDSA", EACObjectIdentifiers.id_TA_ECDSA_SHA_1);
            // addSignatureAlgorithm("SHA224", "CVC-ECDSA", "org.bouncycastle.jce.provider.asymmetric.ec.Signature$ecCVCDSA224", EACObjectIdentifiers.id_TA_ECDSA_SHA_224);
            // addSignatureAlgorithm("SHA256", "CVC-ECDSA", "org.bouncycastle.jce.provider.asymmetric.ec.Signature$ecCVCDSA256", EACObjectIdentifiers.id_TA_ECDSA_SHA_256);
            // END android-removed
        }

        private void addSignatureAlgorithm(
            String digest,
            String algorithm,
            String className,
            DERObjectIdentifier oid)
        {
            String mainName = digest + "WITH" + algorithm;
            String jdk11Variation1 = digest + "with" + algorithm;
            String jdk11Variation2 = digest + "With" + algorithm;
            String alias = digest + "/" + algorithm;

            put("Signature." + mainName, className);
            put("Alg.Alias.Signature." + jdk11Variation1, mainName);
            put("Alg.Alias.Signature." + jdk11Variation2, mainName);
            put("Alg.Alias.Signature." + alias, mainName);
            put("Alg.Alias.Signature." + oid, mainName);
            put("Alg.Alias.Signature.OID." + oid, mainName);
        }
    }
}
