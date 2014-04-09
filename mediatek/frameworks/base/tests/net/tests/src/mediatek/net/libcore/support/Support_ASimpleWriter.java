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

package tests.support;

import java.io.IOException;
import java.io.Writer;

/**
 * An implementation of {@code OutputStream} that should serve as the
 * underlying stream for classes to be tested.
 * In particular this implementation allows to have IOExecptions thrown on demand.
 * For simplicity of use and understanding all fields are public.
 */
public class Support_ASimpleWriter extends Writer {

    public static final int DEFAULT_BUFFER_SIZE = 32;

    public char[] buf;

    public int pos;

    public int size;

    // Set to true when exception is wanted:
    public boolean throwExceptionOnNextUse = false;

    public Support_ASimpleWriter() {
        this(DEFAULT_BUFFER_SIZE);
    }

    public Support_ASimpleWriter(boolean throwException) {
        this(DEFAULT_BUFFER_SIZE);
        throwExceptionOnNextUse = throwException;
    }

    public Support_ASimpleWriter(int bufferSize) {
        buf = new char[bufferSize];
        pos = 0;
        size = bufferSize;
    }

    @Override
    public void close() throws IOException {
        if (throwExceptionOnNextUse) {
            throw new IOException("Exception thrown for testing purpose.");
        }
    }

    @Override
    public void flush() throws IOException {
        if (throwExceptionOnNextUse) {
            throw new IOException("Exception thrown for testing purpose.");
        }
    }

    @Override
    public void write(char[] src, int offset, int count) throws IOException {
        if (throwExceptionOnNextUse) {
            throw new IOException("Exception thrown for testing purpose.");
        }
        if (offset < 0 || count < 0 || (offset + count) > buf.length) {
            throw new IndexOutOfBoundsException();
        }
        try {
            System.arraycopy(src, offset, buf, pos, count);
            pos += count;
        } catch (IndexOutOfBoundsException e) {
            pos = size;
            throw new IOException("Internal Buffer Overflow");
        }
    }

    public byte[] toByteArray() {
        byte[] toReturn = new byte[pos];
        System.arraycopy(buf, 0, toReturn, 0, pos);
        return toReturn;
    }

    public String toString() {
        return new String(buf, 0, pos);
    }
}
