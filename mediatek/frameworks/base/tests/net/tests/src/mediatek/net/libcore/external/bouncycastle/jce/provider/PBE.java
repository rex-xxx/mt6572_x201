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

package org.bouncycastle.jce.provider;

import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.PBEParametersGenerator;
// BEGIN android-removed
// import org.bouncycastle.crypto.digests.MD2Digest;
// END android-removed
import org.bouncycastle.crypto.digests.MD5Digest;
// BEGIN android-removed
// import org.bouncycastle.crypto.digests.RIPEMD160Digest;
// END android-removed
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
// BEGIN android-removed
// import org.bouncycastle.crypto.digests.TigerDigest;
// END android-removed
import org.bouncycastle.crypto.generators.OpenSSLPBEParametersGenerator;
import org.bouncycastle.crypto.generators.PKCS12ParametersGenerator;
import org.bouncycastle.crypto.generators.PKCS5S1ParametersGenerator;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.DESParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public interface PBE
{
    //
    // PBE Based encryption constants - by default we do PKCS12 with SHA-1
    //
    static final int        MD5         = 0;
    static final int        SHA1        = 1;
    static final int        RIPEMD160   = 2;
    static final int        TIGER       = 3;
    static final int        SHA256      = 4;
    static final int        MD2         = 5;

    static final int        PKCS5S1     = 0;
    static final int        PKCS5S2     = 1;
    static final int        PKCS12      = 2;
    static final int        OPENSSL     = 3;

    /**
     * uses the appropriate mixer to generate the key and IV if necessary.
     */
    static class Util
    {
        static private PBEParametersGenerator makePBEGenerator(
            int                     type,
            int                     hash)
        {
            PBEParametersGenerator  generator;
    
            if (type == PKCS5S1)
            {
                switch (hash)
                {
                // BEGIN android-removed
                // case MD2:
                //     generator = new PKCS5S1ParametersGenerator(new MD2Digest());
                //     break;
                // END android-removed
                case MD5:
                    generator = new PKCS5S1ParametersGenerator(new MD5Digest());
                    break;
                case SHA1:
                    generator = new PKCS5S1ParametersGenerator(new SHA1Digest());
                    break;
                default:
                    throw new IllegalStateException("PKCS5 scheme 1 only supports MD2, MD5 and SHA1.");
                }
            }
            else if (type == PKCS5S2)
            {
                generator = new PKCS5S2ParametersGenerator();
            }
            else if (type == PKCS12)
            {
                switch (hash)
                {
                // BEGIN android-removed
                // case MD2:
                //     generator = new PKCS12ParametersGenerator(new MD2Digest());
                //     break;
                // END android-removed
                case MD5:
                    generator = new PKCS12ParametersGenerator(new MD5Digest());
                    break;
                case SHA1:
                    generator = new PKCS12ParametersGenerator(new SHA1Digest());
                    break;
                // BEGIN android-removed
                // case RIPEMD160:
                //     generator = new PKCS12ParametersGenerator(new RIPEMD160Digest());
                //     break;
                // case TIGER:
                //     generator = new PKCS12ParametersGenerator(new TigerDigest());
                //     break;
                // END android-removed
                case SHA256:
                    generator = new PKCS12ParametersGenerator(new SHA256Digest());
                    break;
                default:
                    throw new IllegalStateException("unknown digest scheme for PBE encryption.");
                }
            }
            else
            {
                generator = new OpenSSLPBEParametersGenerator();
            }
    
            return generator;
        }

        /**
         * construct a key and iv (if necessary) suitable for use with a 
         * Cipher.
         */
        static CipherParameters makePBEParameters(
            JCEPBEKey               pbeKey,
            AlgorithmParameterSpec  spec,
            String                  targetAlgorithm)
        {
            if ((spec == null) || !(spec instanceof PBEParameterSpec))
            {
                throw new IllegalArgumentException("Need a PBEParameter spec with a PBE key.");
            }
    
            PBEParameterSpec        pbeParam = (PBEParameterSpec)spec;
            PBEParametersGenerator  generator = makePBEGenerator(pbeKey.getType(), pbeKey.getDigest());
            byte[]                  key = pbeKey.getEncoded();
            CipherParameters        param;
    
            if (pbeKey.shouldTryWrongPKCS12())
            {
                key = new byte[2];
            }
            
            generator.init(key, pbeParam.getSalt(), pbeParam.getIterationCount());

            if (pbeKey.getIvSize() != 0)
            {
                param = generator.generateDerivedParameters(pbeKey.getKeySize(), pbeKey.getIvSize());
            }
            else
            {
                param = generator.generateDerivedParameters(pbeKey.getKeySize());
            }

            if (targetAlgorithm.startsWith("DES"))
            {
                if (param instanceof ParametersWithIV)
                {
                    KeyParameter    kParam = (KeyParameter)((ParametersWithIV)param).getParameters();

                    DESParameters.setOddParity(kParam.getKey());
                }
                else
                {
                    KeyParameter    kParam = (KeyParameter)param;

                    DESParameters.setOddParity(kParam.getKey());
                }
            }

            for (int i = 0; i != key.length; i++)
            {
                key[i] = 0;
            }

            return param;
        }

        /**
         * generate a PBE based key suitable for a MAC algorithm, the
         * key size is chosen according the MAC size, or the hashing algorithm,
         * whichever is greater.
         */
        static CipherParameters makePBEMacParameters(
            JCEPBEKey               pbeKey,
            AlgorithmParameterSpec  spec)
        {
            if ((spec == null) || !(spec instanceof PBEParameterSpec))
            {
                throw new IllegalArgumentException("Need a PBEParameter spec with a PBE key.");
            }
    
            PBEParameterSpec        pbeParam = (PBEParameterSpec)spec;
            PBEParametersGenerator  generator = makePBEGenerator(pbeKey.getType(), pbeKey.getDigest());
            byte[]                  key = pbeKey.getEncoded();
            CipherParameters        param;
    
            if (pbeKey.shouldTryWrongPKCS12())
            {
                key = new byte[2];
            }
            
            generator.init(key, pbeParam.getSalt(), pbeParam.getIterationCount());

            param = generator.generateDerivedMacParameters(pbeKey.getKeySize());
    
            for (int i = 0; i != key.length; i++)
            {
                key[i] = 0;
            }

            return param;
        }
    
        /**
         * construct a key and iv (if necessary) suitable for use with a 
         * Cipher.
         */
        static CipherParameters makePBEParameters(
            PBEKeySpec              keySpec,
            int                     type,
            int                     hash,
            int                     keySize,
            int                     ivSize)
        {    
            PBEParametersGenerator  generator = makePBEGenerator(type, hash);
            byte[]                  key;
            CipherParameters        param;
    
            if (type == PKCS12)
            {
                key = PBEParametersGenerator.PKCS12PasswordToBytes(keySpec.getPassword());
            }
            else
            {   
                key = PBEParametersGenerator.PKCS5PasswordToBytes(keySpec.getPassword());
            }
            
            generator.init(key, keySpec.getSalt(), keySpec.getIterationCount());
    
            if (ivSize != 0)
            {
                param = generator.generateDerivedParameters(keySize, ivSize);
            }
            else
            {
                param = generator.generateDerivedParameters(keySize);
            }
    
            for (int i = 0; i != key.length; i++)
            {
                key[i] = 0;
            }
    
            return param;
        }
    
        /**
         * generate a PBE based key suitable for a MAC algorithm, the
         * key size is chosen according the MAC size, or the hashing algorithm,
         * whichever is greater.
         */
        static CipherParameters makePBEMacParameters(
            PBEKeySpec              keySpec,
            int                     type,
            int                     hash,
            int                     keySize)
        {
            PBEParametersGenerator  generator = makePBEGenerator(type, hash);
            byte[]                  key;
            CipherParameters        param;
    
            if (type == PKCS12)
            {
                key = PBEParametersGenerator.PKCS12PasswordToBytes(keySpec.getPassword());
            }
            else
            {   
                key = PBEParametersGenerator.PKCS5PasswordToBytes(keySpec.getPassword());
            }
            
            generator.init(key, keySpec.getSalt(), keySpec.getIterationCount());
    
            param = generator.generateDerivedMacParameters(keySize);
    
            for (int i = 0; i != key.length; i++)
            {
                key[i] = 0;
            }
    
            return param;
        }
    }
}
