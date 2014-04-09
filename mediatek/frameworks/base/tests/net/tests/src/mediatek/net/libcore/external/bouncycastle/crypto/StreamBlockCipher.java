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

package org.bouncycastle.crypto;

/**
 * a wrapper for block ciphers with a single byte block size, so that they
 * can be treated like stream ciphers.
 */
public class StreamBlockCipher
    implements StreamCipher
{
    private BlockCipher  cipher;

    private byte[]  oneByte = new byte[1];

    /**
     * basic constructor.
     *
     * @param cipher the block cipher to be wrapped.
     * @exception IllegalArgumentException if the cipher has a block size other than
     * one.
     */
    public StreamBlockCipher(
        BlockCipher cipher)
    {
        if (cipher.getBlockSize() != 1)
        {
            throw new IllegalArgumentException("block cipher block size != 1.");
        }

        this.cipher = cipher;
    }

    /**
     * initialise the underlying cipher.
     *
     * @param forEncryption true if we are setting up for encryption, false otherwise.
     * @param params the necessary parameters for the underlying cipher to be initialised.
     */
    public void init(
        boolean forEncryption,
        CipherParameters params)
    {
        cipher.init(forEncryption, params);
    }

    /**
     * return the name of the algorithm we are wrapping.
     *
     * @return the name of the algorithm we are wrapping.
     */
    public String getAlgorithmName()
    {
        return cipher.getAlgorithmName();
    }

    /**
     * encrypt/decrypt a single byte returning the result.
     *
     * @param in the byte to be processed.
     * @return the result of processing the input byte.
     */
    public byte returnByte(
        byte    in)
    {
        oneByte[0] = in;

        cipher.processBlock(oneByte, 0, oneByte, 0);

        return oneByte[0];
    }

    /**
     * process a block of bytes from in putting the result into out.
     * 
     * @param in the input byte array.
     * @param inOff the offset into the in array where the data to be processed starts.
     * @param len the number of bytes to be processed.
     * @param out the output buffer the processed bytes go into.   
     * @param outOff the offset into the output byte array the processed data stars at.
     * @exception DataLengthException if the output buffer is too small.
     */
    public void processBytes(
        byte[]  in,
        int     inOff,
        int     len,
        byte[]  out,
        int     outOff)
        throws DataLengthException
    {
        if (outOff + len > out.length)
        {
            throw new DataLengthException("output buffer too small in processBytes()");
        }

        for (int i = 0; i != len; i++)
        {
                cipher.processBlock(in, inOff + i, out, outOff + i);
        }
    }

    /**
     * reset the underlying cipher. This leaves it in the same state
     * it was at after the last init (if there was one).
     */
    public void reset()
    {
        cipher.reset();
    }
}
