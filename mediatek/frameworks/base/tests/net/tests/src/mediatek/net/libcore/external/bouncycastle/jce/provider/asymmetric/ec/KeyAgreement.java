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

package org.bouncycastle.jce.provider.asymmetric.ec;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Hashtable;

import javax.crypto.KeyAgreementSpi;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.BasicAgreement;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DerivationFunction;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
// BEGIN android-removed
// import org.bouncycastle.crypto.agreement.ECDHCBasicAgreement;
// import org.bouncycastle.crypto.agreement.ECMQVBasicAgreement;
// import org.bouncycastle.crypto.agreement.kdf.DHKDFParameters;
// import org.bouncycastle.crypto.agreement.kdf.ECDHKEKGenerator;
// END android-removed
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
// BEGIN android-removed
// import org.bouncycastle.crypto.params.MQVPrivateParameters;
// import org.bouncycastle.crypto.params.MQVPublicParameters;
// END android-removed
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
// BEGIN android-removed
// import org.bouncycastle.jce.interfaces.MQVPrivateKey;
// import org.bouncycastle.jce.interfaces.MQVPublicKey;
// END android-removed

/**
 * Diffie-Hellman key agreement using elliptic curve keys, ala IEEE P1363
 * both the simple one, and the simple one with cofactors are supported.
 *
 * Also, MQV key agreement per SEC-1
 */
public class KeyAgreement
    extends KeyAgreementSpi
{
    private static final X9IntegerConverter converter = new X9IntegerConverter();
    private static final Hashtable algorithms = new Hashtable();

    static
    {
        // BEGIN android-changed
        Integer i128 = Integer.valueOf(128);
        Integer i192 = Integer.valueOf(192);
        Integer i256 = Integer.valueOf(256);
        // END android-changed

        algorithms.put(NISTObjectIdentifiers.id_aes128_CBC.getId(), i128);
        algorithms.put(NISTObjectIdentifiers.id_aes192_CBC.getId(), i192);
        algorithms.put(NISTObjectIdentifiers.id_aes256_CBC.getId(), i256);
        algorithms.put(NISTObjectIdentifiers.id_aes128_wrap.getId(), i128);
        algorithms.put(NISTObjectIdentifiers.id_aes192_wrap.getId(), i192);
        algorithms.put(NISTObjectIdentifiers.id_aes256_wrap.getId(), i256);
        algorithms.put(PKCSObjectIdentifiers.id_alg_CMS3DESwrap.getId(), i192);
    }

    private String                 kaAlgorithm;
    private BigInteger             result;
    private ECDomainParameters     parameters;
    private BasicAgreement         agreement;
    // BEGIN android-removed
    // private DerivationFunction     kdf;
    // END android-removed

    private byte[] bigIntToBytes(
        BigInteger    r)
    {
        return converter.integerToBytes(r, converter.getByteLength(parameters.getG().getX()));
    }

    protected KeyAgreement(
        String              kaAlgorithm,
        BasicAgreement      agreement,
        DerivationFunction  kdf)
    {
        this.kaAlgorithm = kaAlgorithm;
        this.agreement = agreement;
        // BEGIN android-removed
        // this.kdf = kdf;
        // END android-removed
    }

    protected Key engineDoPhase(
        Key     key,
        boolean lastPhase) 
        throws InvalidKeyException, IllegalStateException
    {
        if (parameters == null)
        {
            throw new IllegalStateException(kaAlgorithm + " not initialised.");
        }

        if (!lastPhase)
        {
            throw new IllegalStateException(kaAlgorithm + " can only be between two parties.");
        }

        CipherParameters pubKey;        
        // BEGIN android-removed
        // if (agreement instanceof ECMQVBasicAgreement)
        // {
        //     if (!(key instanceof MQVPublicKey))
        //     {
        //         throw new InvalidKeyException(kaAlgorithm + " key agreement requires "
        //             + getSimpleName(MQVPublicKey.class) + " for doPhase");
        //     }
        //
        //     MQVPublicKey mqvPubKey = (MQVPublicKey)key;
        //     ECPublicKeyParameters staticKey = (ECPublicKeyParameters)
        //         ECUtil.generatePublicKeyParameter(mqvPubKey.getStaticKey());
        //     ECPublicKeyParameters ephemKey = (ECPublicKeyParameters)
        //         ECUtil.generatePublicKeyParameter(mqvPubKey.getEphemeralKey());
        //
        //     pubKey = new MQVPublicParameters(staticKey, ephemKey);
        //
        //     // TODO Validate that all the keys are using the same parameters?
        // }
        // else
        // END android-removed
        {
            if (!(key instanceof ECPublicKey))
            {
                throw new InvalidKeyException(kaAlgorithm + " key agreement requires "
                    + getSimpleName(ECPublicKey.class) + " for doPhase");
            }

            pubKey = ECUtil.generatePublicKeyParameter((PublicKey)key);

            // TODO Validate that all the keys are using the same parameters?
        }

        result = agreement.calculateAgreement(pubKey);

        return null;
    }

    protected byte[] engineGenerateSecret()
        throws IllegalStateException
    {
        // BEGIN android-removed
        // if (kdf != null)
        // {
        //     throw new UnsupportedOperationException(
        //         "KDF can only be used when algorithm is known");
        // }
        // END android-removed

        return bigIntToBytes(result);
    }

    protected int engineGenerateSecret(
        byte[]  sharedSecret,
        int     offset) 
        throws IllegalStateException, ShortBufferException
    {
        byte[] secret = engineGenerateSecret();

        if (sharedSecret.length - offset < secret.length)
        {
            throw new ShortBufferException(kaAlgorithm + " key agreement: need " + secret.length + " bytes");
        }

        System.arraycopy(secret, 0, sharedSecret, offset, secret.length);
        
        return secret.length;
    }

    protected SecretKey engineGenerateSecret(
        String algorithm)
        throws NoSuchAlgorithmException
    {
        byte[] secret = bigIntToBytes(result);

        // BEGIN android-removed
        // if (kdf != null)
        // {
        //     if (!algorithms.containsKey(algorithm))
        //     {
        //         throw new NoSuchAlgorithmException("unknown algorithm encountered: " + algorithm);
        //     }
        //  
        //     int    keySize = ((Integer)algorithms.get(algorithm)).intValue();
        //
        //     DHKDFParameters params = new DHKDFParameters(new DERObjectIdentifier(algorithm), keySize, secret);
        //
        //     byte[] keyBytes = new byte[keySize / 8];
        //     kdf.init(params);
        //     kdf.generateBytes(keyBytes, 0, keyBytes.length);
        //     secret = keyBytes;
        // }
        // else
        // END android-removed
        {
            // TODO Should we be ensuring the key is the right length?
        }

        return new SecretKeySpec(secret, algorithm);
    }

    protected void engineInit(
        Key                     key,
        AlgorithmParameterSpec  params,
        SecureRandom            random) 
        throws InvalidKeyException, InvalidAlgorithmParameterException
    {
        initFromKey(key);
    }

    protected void engineInit(
        Key             key,
        SecureRandom    random) 
        throws InvalidKeyException
    {
        initFromKey(key);
    }

    private void initFromKey(Key key)
        throws InvalidKeyException
    {
        // BEGIN android-removed
        // if (agreement instanceof ECMQVBasicAgreement)
        // {
        //     if (!(key instanceof MQVPrivateKey))
        //     {
        //         throw new InvalidKeyException(kaAlgorithm + " key agreement requires "
        //             + getSimpleName(MQVPrivateKey.class) + " for initialisation");
        //     }
        //
        //     MQVPrivateKey mqvPrivKey = (MQVPrivateKey)key;
        //     ECPrivateKeyParameters staticPrivKey = (ECPrivateKeyParameters)
        //         ECUtil.generatePrivateKeyParameter(mqvPrivKey.getStaticPrivateKey());
        //     ECPrivateKeyParameters ephemPrivKey = (ECPrivateKeyParameters)
        //         ECUtil.generatePrivateKeyParameter(mqvPrivKey.getEphemeralPrivateKey());
        //
        //     ECPublicKeyParameters ephemPubKey = null;
        //     if (mqvPrivKey.getEphemeralPublicKey() != null)
        //     {
        //         ephemPubKey = (ECPublicKeyParameters)
        //             ECUtil.generatePublicKeyParameter(mqvPrivKey.getEphemeralPublicKey());
        //     }
        //
        //     MQVPrivateParameters localParams = new MQVPrivateParameters(staticPrivKey, ephemPrivKey, ephemPubKey);
        //     this.parameters = staticPrivKey.getParameters();
        //
        //     // TODO Validate that all the keys are using the same parameters?
        //
        //     agreement.init(localParams);
        // }
        // else
        // END android-removed
        {
            if (!(key instanceof ECPrivateKey))
            {
                throw new InvalidKeyException(kaAlgorithm + " key agreement requires "
                    + getSimpleName(ECPrivateKey.class) + " for initialisation");
            }

            ECPrivateKeyParameters privKey = (ECPrivateKeyParameters)ECUtil.generatePrivateKeyParameter((PrivateKey)key);
            this.parameters = privKey.getParameters();

            agreement.init(privKey);
        }
    }

    private static String getSimpleName(Class clazz)
    {
        String fullName = clazz.getName();

        return fullName.substring(fullName.lastIndexOf('.') + 1);
    }

    public static class DH
        extends KeyAgreement
    {
        public DH()
        {
            super("ECDH", new ECDHBasicAgreement(), null);
        }
    }

    // BEGIN android-removed
    // public static class DHC
    //     extends KeyAgreement
    // {
    //     public DHC()
    //     {
    //         super("ECDHC", new ECDHCBasicAgreement(), null);
    //     }
    // }
    //
    // public static class MQV
    //     extends KeyAgreement
    // {
    //     public MQV()
    //     {
    //         super("ECMQV", new ECMQVBasicAgreement(), null);
    //     }
    // }
    //
    // public static class DHwithSHA1KDF
    //     extends KeyAgreement
    // {
    //     public DHwithSHA1KDF()
    //     {
    //         super("ECDHwithSHA1KDF", new ECDHBasicAgreement(), new ECDHKEKGenerator(new SHA1Digest()));
    //     }
    // }
    //
    // public static class MQVwithSHA1KDF
    //     extends KeyAgreement
    // {
    //     public MQVwithSHA1KDF()
    //     {
    //         super("ECMQVwithSHA1KDF", new ECMQVBasicAgreement(), new ECDHKEKGenerator(new SHA1Digest()));
    //     }
    // }
    // END android-removed
}
