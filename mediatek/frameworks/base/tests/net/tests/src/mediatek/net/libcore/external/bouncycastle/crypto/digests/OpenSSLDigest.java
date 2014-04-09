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

/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bouncycastle.crypto.digests;

import org.apache.harmony.xnet.provider.jsse.NativeCrypto;
import org.bouncycastle.crypto.ExtendedDigest;

/**
 * Implements the BouncyCastle Digest interface using OpenSSL's EVP API.
 */
public class OpenSSLDigest implements ExtendedDigest {

    /**
     * Holds the standard name of the hashing algorithm, e.g. "SHA-1";
     */
    private final String algorithm;

    /**
     * Holds the EVP_MD for the hashing algorithm, e.g. EVP_get_digestbyname("sha1");
     */
    private final int evp_md;

    /**
     * Holds the output size of the message digest.
     */
    private final int size;

    /**
     * Holds the block size of the message digest.
     */
    private final int blockSize;

    /**
     * Holds a pointer to the native message digest context. It is
     * lazily initialized to avoid having to reallocate on reset when
     * its unlikely to be reused.
     */
    private int ctx;

    /**
     * Holds a dummy buffer for writing single bytes to the digest.
     */
    private final byte[] singleByte = new byte[1];

    /**
     * Creates a new OpenSSLMessageDigest instance for the given algorithm
     * name.
     */
    private OpenSSLDigest(String algorithm, int evp_md, int size, int blockSize) {
        this.algorithm = algorithm;
        this.evp_md = evp_md;
        this.size = size;
        this.blockSize = blockSize;
    }

    public String getAlgorithmName() {
        return algorithm;
    }

    public int getDigestSize() {
        return size;
    }

    public int getByteLength() {
        return blockSize;
    }

    public void reset() {
        free();
    }

    public void update(byte in) {
        singleByte[0] = in;
        update(singleByte, 0, 1);
    }

    public void update(byte[] in, int inOff, int len) {
        NativeCrypto.EVP_DigestUpdate(getCtx(), in, inOff, len);
    }

    public int doFinal(byte[] out, int outOff) {
        int i = NativeCrypto.EVP_DigestFinal(getCtx(), out, outOff);
        ctx = 0; // EVP_DigestFinal frees the context as a side effect
        reset();
        return i;
    }

    private int getCtx() {
        if (ctx == 0) {
            ctx = NativeCrypto.EVP_DigestInit(evp_md);
        }
        return ctx;
    }

    private void free() {
        if (ctx != 0) {
            NativeCrypto.EVP_MD_CTX_destroy(ctx);
            ctx = 0;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            free();
        } finally {
            super.finalize();
        }
    }

    public static class MD5 extends OpenSSLDigest {
        private static final int EVP_MD = NativeCrypto.EVP_get_digestbyname("md5");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);
        private static final int BLOCK_SIZE = NativeCrypto.EVP_MD_block_size(EVP_MD);
        public MD5() { super("MD5", EVP_MD, SIZE, BLOCK_SIZE); }
    }

    public static class SHA1 extends OpenSSLDigest {
        private static final int EVP_MD = NativeCrypto.EVP_get_digestbyname("sha1");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);
        private static final int BLOCK_SIZE = NativeCrypto.EVP_MD_block_size(EVP_MD);
        public SHA1() { super("SHA-1", EVP_MD, SIZE, BLOCK_SIZE); }
    }

    public static class SHA256 extends OpenSSLDigest {
        private static final int EVP_MD = NativeCrypto.EVP_get_digestbyname("sha256");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);
        private static final int BLOCK_SIZE = NativeCrypto.EVP_MD_block_size(EVP_MD);
        public SHA256() { super("SHA-256", EVP_MD, SIZE, BLOCK_SIZE); }
    }

    public static class SHA384 extends OpenSSLDigest {
        private static final int EVP_MD = NativeCrypto.EVP_get_digestbyname("sha384");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);
        private static final int BLOCK_SIZE = NativeCrypto.EVP_MD_block_size(EVP_MD);
        public SHA384() { super("SHA-384", EVP_MD, SIZE, BLOCK_SIZE); }
    }

    public static class SHA512 extends OpenSSLDigest {
        private static final int EVP_MD = NativeCrypto.EVP_get_digestbyname("sha512");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);
        private static final int BLOCK_SIZE = NativeCrypto.EVP_MD_block_size(EVP_MD);
        public SHA512() { super("SHA-512", EVP_MD, SIZE, BLOCK_SIZE); }
    }
}
