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
import java.io.Reader;

/**
 * An implementation of {@code Reader} that should serve as the
 * underlying writer for classes to be tested.
 * In particular this implementation allows to have IOExecptions thrown on demand.
 * For simplicity of use and understanding all fields are public.
 */
public class Support_ASimpleReader extends Reader {

    public static final int DEFAULT_BUFFER_SIZE = 32;

    public char[] buf;

    public int pos;

    public int len;

    // Set to true when exception is wanted:
    public boolean throwExceptionOnNextUse = false;

    public Support_ASimpleReader() {
        this("BEGIN Bla bla, some text...END");
    }

    public Support_ASimpleReader(boolean throwException) {
        this();
        throwExceptionOnNextUse = throwException;
    }

    public Support_ASimpleReader(String input) {
        buf = input.toCharArray();
        pos = 0;
        len = buf.length;
    }

    @Override
    public void close() throws IOException {
        if (throwExceptionOnNextUse) {
            throw new IOException("Exception thrown for testing purpose.");
        }
    }

    @Override
    public boolean ready() throws IOException {
        if (throwExceptionOnNextUse) {
            throw new IOException("Exception thrown for testing purpose.");
        }
        return len > pos;
    }

    @Override
    public int read(char[] dest, int offset, int count) throws IOException {
        if (throwExceptionOnNextUse) {
            throw new IOException("Exception thrown for testing purpose.");
        }
        int available = len - pos;
        if (available > 0) {
            int readable = (available < count ? available : count);
            System.arraycopy(buf, pos, dest, offset, readable);
            pos += readable;
            return readable;
        } else {
            return -1;
        }
    }
}
