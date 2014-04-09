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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.KeyGeneratorSpi;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.generators.DESKeyGenerator;

public class JCEKeyGenerator
    extends KeyGeneratorSpi
{
    protected String                algName;
    protected int                   keySize;
    protected int                   defaultKeySize;
    protected CipherKeyGenerator    engine;

    protected boolean               uninitialised = true;

    protected JCEKeyGenerator(
        String              algName,
        int                 defaultKeySize,
        CipherKeyGenerator  engine)
    {
        this.algName = algName;
        this.keySize = this.defaultKeySize = defaultKeySize;
        this.engine = engine;
    }

    protected void engineInit(
        AlgorithmParameterSpec  params,
        SecureRandom            random)
    throws InvalidAlgorithmParameterException
    {
        throw new InvalidAlgorithmParameterException("Not Implemented");
    }

    protected void engineInit(
        SecureRandom    random)
    {
        if (random != null)
        {
            engine.init(new KeyGenerationParameters(random, defaultKeySize));
            uninitialised = false;
        }
    }

    protected void engineInit(
        int             keySize,
        SecureRandom    random)
    {
        try
        {
            // BEGIN android-added
            if (random == null) {
                random = new SecureRandom();
            }
            // END android-added
            engine.init(new KeyGenerationParameters(random, keySize));
            uninitialised = false;
        }
        catch (IllegalArgumentException e)
        {
            throw new InvalidParameterException(e.getMessage());
        }
    }

    protected SecretKey engineGenerateKey()
    {
        if (uninitialised)
        {
            engine.init(new KeyGenerationParameters(new SecureRandom(), defaultKeySize));
            uninitialised = false;
        }

        return new SecretKeySpec(engine.generateKey(), algName);
    }

    /**
     * the generators that are defined directly off us.
     */

    /**
     * DES
     */
    public static class DES
        extends JCEKeyGenerator
    {
        public DES()
        {
            super("DES", 64, new DESKeyGenerator());
        }
    }

    // BEGIN android-removed
    // /**
    //  * RC2
    //  */
    // public static class RC2
    //     extends JCEKeyGenerator
    // {
    //     public RC2()
    //     {
    //         super("RC2", 128, new CipherKeyGenerator());
    //     }
    // }
    //
    // /**
    //  * GOST28147
    //  */
    // public static class GOST28147
    //     extends JCEKeyGenerator
    // {
    //     public GOST28147()
    //     {
    //         super("GOST28147", 256, new CipherKeyGenerator());
    //     }
    // }
    // END android-removed

    // HMAC Related secret keys..
  
    // BEGIN android-removed
    // /**
    //  * MD2HMAC
    //  */
    // public static class MD2HMAC
    //     extends JCEKeyGenerator
    // {
    //     public MD2HMAC()
    //     {
    //         super("HMACMD2", 128, new CipherKeyGenerator());
    //     }
    // }
    //
    //
    // /**
    //  * MD4HMAC
    //  */
    // public static class MD4HMAC
    //     extends JCEKeyGenerator
    // {
    //     public MD4HMAC()
    //     {
    //         super("HMACMD4", 128, new CipherKeyGenerator());
    //     }
    // }
    // END android-removed

    /**
     * MD5HMAC
     */
    public static class MD5HMAC
        extends JCEKeyGenerator
    {
        public MD5HMAC()
        {
            super("HMACMD5", 128, new CipherKeyGenerator());
        }
    }


    // /**
    //  * RIPE128HMAC
    //  */
    // public static class RIPEMD128HMAC
    //     extends JCEKeyGenerator
    // {
    //     public RIPEMD128HMAC()
    //     {
    //         super("HMACRIPEMD128", 128, new CipherKeyGenerator());
    //     }
    // }

    // /**
    //  * RIPE160HMAC
    //  */
    // public static class RIPEMD160HMAC
    //     extends JCEKeyGenerator
    // {
    //     public RIPEMD160HMAC()
    //     {
    //         super("HMACRIPEMD160", 160, new CipherKeyGenerator());
    //     }
    // }


    /**
     * HMACSHA1
     */
    public static class HMACSHA1
        extends JCEKeyGenerator
    {
        public HMACSHA1()
        {
            super("HMACSHA1", 160, new CipherKeyGenerator());
        }
    }

    // BEGIN android-removed
    // /**
    //  * HMACSHA224
    //  */
    // public static class HMACSHA224
    //     extends JCEKeyGenerator
    // {
    //     public HMACSHA224()
    //     {
    //         super("HMACSHA224", 224, new CipherKeyGenerator());
    //     }
    // }
    // END android-removed
    
    /**
     * HMACSHA256
     */
    public static class HMACSHA256
        extends JCEKeyGenerator
    {
        public HMACSHA256()
        {
            super("HMACSHA256", 256, new CipherKeyGenerator());
        }
    }
    
    /**
     * HMACSHA384
     */
    public static class HMACSHA384
        extends JCEKeyGenerator
    {
        public HMACSHA384()
        {
            super("HMACSHA384", 384, new CipherKeyGenerator());
        }
    }
    
    /**
     * HMACSHA512
     */
    public static class HMACSHA512
        extends JCEKeyGenerator
    {
        public HMACSHA512()
        {
            super("HMACSHA512", 512, new CipherKeyGenerator());
        }
    }
    
    // BEGIN android-removed
    // /**
    //  * HMACTIGER
    //  */
    // public static class HMACTIGER
    //     extends JCEKeyGenerator
    // {
    //     public HMACTIGER()
    //     {
    //         super("HMACTIGER", 192, new CipherKeyGenerator());
    //     }
    // }
    // END android-removed
}
