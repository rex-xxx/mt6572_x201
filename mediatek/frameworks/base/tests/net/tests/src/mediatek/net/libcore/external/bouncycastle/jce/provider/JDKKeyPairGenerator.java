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

import org.bouncycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.DHBasicKeyPairGenerator;
import org.bouncycastle.crypto.generators.DHParametersGenerator;
import org.bouncycastle.crypto.generators.DSAKeyPairGenerator;
import org.bouncycastle.crypto.generators.DSAParametersGenerator;
// BEGIN android-removed
// import org.bouncycastle.crypto.generators.ElGamalKeyPairGenerator;
// import org.bouncycastle.crypto.generators.ElGamalParametersGenerator;
// import org.bouncycastle.crypto.generators.GOST3410KeyPairGenerator;
// END android-removed
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.DHKeyGenerationParameters;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.crypto.params.DHPrivateKeyParameters;
import org.bouncycastle.crypto.params.DHPublicKeyParameters;
import org.bouncycastle.crypto.params.DSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.DSAParameters;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
// BEGIN android-removed
// import org.bouncycastle.crypto.params.ElGamalKeyGenerationParameters;
// import org.bouncycastle.crypto.params.ElGamalParameters;
// import org.bouncycastle.crypto.params.ElGamalPrivateKeyParameters;
// import org.bouncycastle.crypto.params.ElGamalPublicKeyParameters;
// import org.bouncycastle.crypto.params.GOST3410KeyGenerationParameters;
// import org.bouncycastle.crypto.params.GOST3410Parameters;
// import org.bouncycastle.crypto.params.GOST3410PrivateKeyParameters;
// import org.bouncycastle.crypto.params.GOST3410PublicKeyParameters;
// END android-removed
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
// BEGIN android-removed
// import org.bouncycastle.jce.spec.ElGamalParameterSpec;
// import org.bouncycastle.jce.spec.GOST3410ParameterSpec;
// import org.bouncycastle.jce.spec.GOST3410PublicKeyParameterSetSpec;
// END android-removed

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.DSAParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Hashtable;

import javax.crypto.spec.DHParameterSpec;

public abstract class JDKKeyPairGenerator
    extends KeyPairGenerator
{
    public JDKKeyPairGenerator(
        String              algorithmName)
    {
        super(algorithmName);
    }

    public abstract void initialize(int strength, SecureRandom random);

    public abstract KeyPair generateKeyPair();

    public static class RSA
        extends JDKKeyPairGenerator
    {
        final static BigInteger defaultPublicExponent = BigInteger.valueOf(0x10001);
        final static int defaultTests = 12;

        RSAKeyGenerationParameters  param;
        RSAKeyPairGenerator         engine;

        public RSA()
        {
            super("RSA");

            engine = new RSAKeyPairGenerator();
            param = new RSAKeyGenerationParameters(defaultPublicExponent,
                            new SecureRandom(), 2048, defaultTests);
            engine.init(param);
        }

        public void initialize(
            int             strength,
            SecureRandom    random)
        {
            param = new RSAKeyGenerationParameters(defaultPublicExponent,
                            random, strength, defaultTests);

            engine.init(param);
        }

        public void initialize(
            AlgorithmParameterSpec  params,
            SecureRandom            random)
            throws InvalidAlgorithmParameterException
        {
            if (!(params instanceof RSAKeyGenParameterSpec))
            {
                throw new InvalidAlgorithmParameterException("parameter object not a RSAKeyGenParameterSpec");
            }
            RSAKeyGenParameterSpec     rsaParams = (RSAKeyGenParameterSpec)params;

            param = new RSAKeyGenerationParameters(
                            rsaParams.getPublicExponent(),
                            random, rsaParams.getKeysize(), defaultTests);

            engine.init(param);
        }

        public KeyPair generateKeyPair()
        {
            AsymmetricCipherKeyPair     pair = engine.generateKeyPair();
            RSAKeyParameters            pub = (RSAKeyParameters)pair.getPublic();
            RSAPrivateCrtKeyParameters  priv = (RSAPrivateCrtKeyParameters)pair.getPrivate();

            return new KeyPair(new JCERSAPublicKey(pub),
                               new JCERSAPrivateCrtKey(priv));
        }
    }

    public static class DH
        extends JDKKeyPairGenerator
    {
        private static Hashtable   params = new Hashtable();

        DHKeyGenerationParameters  param;
        DHBasicKeyPairGenerator    engine = new DHBasicKeyPairGenerator();
        int                        strength = 1024;
        int                        certainty = 20;
        SecureRandom               random = new SecureRandom();
        boolean                    initialised = false;

        public DH()
        {
            super("DH");
        }

        public void initialize(
            int             strength,
            SecureRandom    random)
        {
            this.strength = strength;
            this.random = random;
        }

        public void initialize(
            AlgorithmParameterSpec  params,
            SecureRandom            random)
            throws InvalidAlgorithmParameterException
        {
            if (!(params instanceof DHParameterSpec))
            {
                throw new InvalidAlgorithmParameterException("parameter object not a DHParameterSpec");
            }
            DHParameterSpec     dhParams = (DHParameterSpec)params;

            param = new DHKeyGenerationParameters(random, new DHParameters(dhParams.getP(), dhParams.getG(), null, dhParams.getL()));

            engine.init(param);
            initialised = true;
        }

        public KeyPair generateKeyPair()
        {
            if (!initialised)
            {
                // BEGIN android-changed
                Integer paramStrength = Integer.valueOf(strength);
                // END android-changed

                if (params.containsKey(paramStrength))
                {
                    param = (DHKeyGenerationParameters)params.get(paramStrength);
                }
                else
                {
                    DHParametersGenerator   pGen = new DHParametersGenerator();

                    pGen.init(strength, certainty, random);

                    param = new DHKeyGenerationParameters(random, pGen.generateParameters());

                    params.put(paramStrength, param);
                }

                engine.init(param);

                initialised = true;
            }

            AsymmetricCipherKeyPair pair = engine.generateKeyPair();
            DHPublicKeyParameters   pub = (DHPublicKeyParameters)pair.getPublic();
            DHPrivateKeyParameters  priv = (DHPrivateKeyParameters)pair.getPrivate();

            return new KeyPair(new JCEDHPublicKey(pub),
                               new JCEDHPrivateKey(priv));
        }
    }

    public static class DSA
        extends JDKKeyPairGenerator
    {
        DSAKeyGenerationParameters param;
        DSAKeyPairGenerator        engine = new DSAKeyPairGenerator();
        int                        strength = 1024;
        int                        certainty = 20;
        SecureRandom               random = new SecureRandom();
        boolean                    initialised = false;

        public DSA()
        {
            super("DSA");
        }

        public void initialize(
            int             strength,
            SecureRandom    random)
        {
            if (strength < 512 || strength > 1024 || strength % 64 != 0)
            {
                throw new InvalidParameterException("strength must be from 512 - 1024 and a multiple of 64");
            }

            this.strength = strength;
            this.random = random;
        }

        public void initialize(
            AlgorithmParameterSpec  params,
            SecureRandom            random)
            throws InvalidAlgorithmParameterException
        {
            if (!(params instanceof DSAParameterSpec))
            {
                throw new InvalidAlgorithmParameterException("parameter object not a DSAParameterSpec");
            }
            DSAParameterSpec     dsaParams = (DSAParameterSpec)params;

            param = new DSAKeyGenerationParameters(random, new DSAParameters(dsaParams.getP(), dsaParams.getQ(), dsaParams.getG()));

            engine.init(param);
            initialised = true;
        }

        public KeyPair generateKeyPair()
        {
            if (!initialised)
            {
                DSAParametersGenerator   pGen = new DSAParametersGenerator();

                pGen.init(strength, certainty, random);
                param = new DSAKeyGenerationParameters(random, pGen.generateParameters());
                engine.init(param);
                initialised = true;
            }

            AsymmetricCipherKeyPair   pair = engine.generateKeyPair();
            DSAPublicKeyParameters     pub = (DSAPublicKeyParameters)pair.getPublic();
            DSAPrivateKeyParameters priv = (DSAPrivateKeyParameters)pair.getPrivate();

            return new KeyPair(new JDKDSAPublicKey(pub),
                               new JDKDSAPrivateKey(priv));
        }
    }

    // BEGIN android-removed
    // public static class ElGamal
    //     extends JDKKeyPairGenerator
    // {
    //     ElGamalKeyGenerationParameters  param;
    //     ElGamalKeyPairGenerator         engine = new ElGamalKeyPairGenerator();
    //     int                             strength = 1024;
    //     int                             certainty = 20;
    //     SecureRandom                    random = new SecureRandom();
    //     boolean                         initialised = false;
    //
    //     public ElGamal()
    //     {
    //         super("ElGamal");
    //     }
    //
    //     public void initialize(
    //         int             strength,
    //         SecureRandom    random)
    //     {
    //         this.strength = strength;
    //         this.random = random;
    //     }
    //
    //     public void initialize(
    //         AlgorithmParameterSpec  params,
    //         SecureRandom            random)
    //         throws InvalidAlgorithmParameterException
    //     {
    //         if (!(params instanceof ElGamalParameterSpec) && !(params instanceof DHParameterSpec))
    //         {
    //             throw new InvalidAlgorithmParameterException("parameter object not a DHParameterSpec or an ElGamalParameterSpec");
    //         }
    //
    //         if (params instanceof ElGamalParameterSpec)
    //         {
    //             ElGamalParameterSpec     elParams = (ElGamalParameterSpec)params;

    //             param = new ElGamalKeyGenerationParameters(random, new ElGamalParameters(elParams.getP(), elParams.getG()));
    //         }
    //         else
    //         {
    //             DHParameterSpec     dhParams = (DHParameterSpec)params;
    //
    //             param = new ElGamalKeyGenerationParameters(random, new ElGamalParameters(dhParams.getP(), dhParams.getG(), dhParams.getL()));
    //         }
    //
    //         engine.init(param);
    //         initialised = true;
    //     }
    //
    //     public KeyPair generateKeyPair()
    //     {
    //         if (!initialised)
    //         {
    //             ElGamalParametersGenerator   pGen = new ElGamalParametersGenerator();
    //
    //             pGen.init(strength, certainty, random);
    //             param = new ElGamalKeyGenerationParameters(random, pGen.generateParameters());
    //             engine.init(param);
    //             initialised = true;
    //         }
    //
    //         AsymmetricCipherKeyPair         pair = engine.generateKeyPair();
    //         ElGamalPublicKeyParameters      pub = (ElGamalPublicKeyParameters)pair.getPublic();
    //         ElGamalPrivateKeyParameters     priv = (ElGamalPrivateKeyParameters)pair.getPrivate();
    //
    //         return new KeyPair(new JCEElGamalPublicKey(pub),
    //                            new JCEElGamalPrivateKey(priv));
    //     }
    // }
    // END android-removed

   // BEGIN android-removed
   //  public static class GOST3410
   //      extends JDKKeyPairGenerator
   //  {
   //      GOST3410KeyGenerationParameters param;
   //      GOST3410KeyPairGenerator        engine = new GOST3410KeyPairGenerator();
   //      GOST3410ParameterSpec           gost3410Params;
   //      int                             strength = 1024;
   //      SecureRandom                    random = null;
   //      boolean                         initialised = false;
   //
   //      public GOST3410()
   //      {
   //          super("GOST3410");
   //      }
   //
   //      public void initialize(
   //          int             strength,
   //          SecureRandom    random)
   //      {
   //          this.strength = strength;
   //          this.random = random;
   //      }
   //
   //      private void init(
   //          GOST3410ParameterSpec gParams,
   //          SecureRandom          random)
   //      {
   //          GOST3410PublicKeyParameterSetSpec spec = gParams.getPublicKeyParameters();
   //
   //          param = new GOST3410KeyGenerationParameters(random, new GOST3410Parameters(spec.getP(), spec.getQ(), spec.getA()));
   //
   //          engine.init(param);
   //
   //          initialised = true;
   //          gost3410Params = gParams;
   //      }
   //
   //      public void initialize(
   //          AlgorithmParameterSpec  params,
   //          SecureRandom            random)
   //          throws InvalidAlgorithmParameterException
   //      {
   //          if (!(params instanceof GOST3410ParameterSpec))
   //          {
   //              throw new InvalidAlgorithmParameterException("parameter object not a GOST3410ParameterSpec");
   //          }
   //
   //          init((GOST3410ParameterSpec)params, random);
   //      }
   //
   //      public KeyPair generateKeyPair()
   //      {
   //          if (!initialised)
   //          {
   //              init(new GOST3410ParameterSpec(CryptoProObjectIdentifiers.gostR3410_94_CryptoPro_A.getId()), new SecureRandom());
   //          }
   //         
   //          AsymmetricCipherKeyPair   pair = engine.generateKeyPair();
   //          GOST3410PublicKeyParameters  pub = (GOST3410PublicKeyParameters)pair.getPublic();
   //          GOST3410PrivateKeyParameters priv = (GOST3410PrivateKeyParameters)pair.getPrivate();
   //
   //          return new KeyPair(new JDKGOST3410PublicKey(pub, gost3410Params), new JDKGOST3410PrivateKey(priv, gost3410Params));
   //      }
   // }
   // END android-removed
}
