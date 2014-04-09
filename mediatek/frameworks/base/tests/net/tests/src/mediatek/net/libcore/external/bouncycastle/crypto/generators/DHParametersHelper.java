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

package org.bouncycastle.crypto.generators;

import java.math.BigInteger;
import java.security.SecureRandom;

// BEGIN android-added
import java.util.logging.Logger;
// END android-added
import org.bouncycastle.util.BigIntegers;

class DHParametersHelper
{
    // BEGIN android-added
    private static final Logger logger = Logger.getLogger(DHParametersHelper.class.getName());
    // END android-added

    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final BigInteger TWO = BigInteger.valueOf(2);

    /*
     * Finds a pair of prime BigInteger's {p, q: p = 2q + 1}
     * 
     * (see: Handbook of Applied Cryptography 4.86)
     */
    static BigInteger[] generateSafePrimes(int size, int certainty, SecureRandom random)
    {
        // BEGIN android-added
        logger.info("Generating safe primes. This may take a long time.");
        long start = System.currentTimeMillis();
        int tries = 0;
        // END android-added
        BigInteger p, q;
        int qLength = size - 1;

        for (;;)
        {
            // BEGIN android-added
            tries++;
            // END android-added
            q = new BigInteger(qLength, 2, random);

            // p <- 2q + 1
            p = q.shiftLeft(1).add(ONE);

            if (p.isProbablePrime(certainty) && (certainty <= 2 || q.isProbablePrime(certainty)))
            {
                break;
            }
        }
        // BEGIN android-added
        long end = System.currentTimeMillis();
        long duration = end - start;
        logger.info("Generated safe primes: " + tries + " tries took " + duration + "ms");
        // END android-added

        return new BigInteger[] { p, q };
    }

    /*
     * Select a high order element of the multiplicative group Zp*
     * 
     * p and q must be s.t. p = 2*q + 1, where p and q are prime (see generateSafePrimes)
     */
    static BigInteger selectGenerator(BigInteger p, BigInteger q, SecureRandom random)
    {
        BigInteger pMinusTwo = p.subtract(TWO);
        BigInteger g;

        /*
         * (see: Handbook of Applied Cryptography 4.80)
         */
//        do
//        {
//            g = BigIntegers.createRandomInRange(TWO, pMinusTwo, random);
//        }
//        while (g.modPow(TWO, p).equals(ONE) || g.modPow(q, p).equals(ONE));


        /*
         * RFC 2631 2.2.1.2 (and see: Handbook of Applied Cryptography 4.81)
         */
        do
        {
            BigInteger h = BigIntegers.createRandomInRange(TWO, pMinusTwo, random);

            g = h.modPow(TWO, p);
        }
        while (g.equals(ONE));


        return g;
    }
}
