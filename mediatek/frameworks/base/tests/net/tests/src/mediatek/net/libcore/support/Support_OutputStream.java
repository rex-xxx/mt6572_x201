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
import java.io.OutputStream;

/**
 * An implementation of {@code OutputStream} that stores the written data in a
 * byte array of fixed size. As a special feature, instances of this class can
 * be instructed to throw an {@code IOException} whenever a method is called.
 * This is used to test the {@code IOException} handling of classes that write
 * to an {@code OutputStream}.
 */
public class Support_OutputStream extends OutputStream {

    private static final int DEFAULT_BUFFER_SIZE = 32;

    private byte[] buffer;

    private int position;

    private int size;

    private boolean throwsException;

    public Support_OutputStream() {
        this(DEFAULT_BUFFER_SIZE);
    }

    public Support_OutputStream(boolean throwException) {
        this(DEFAULT_BUFFER_SIZE);
        throwsException = throwException;
    }

    public Support_OutputStream(int bufferSize) {
        buffer = new byte[bufferSize];
        position = 0;
        size = bufferSize;
        throwsException = false;
    }

    @Override
    public void close() throws IOException {
        if (throwsException) {
            throw new IOException("Exception thrown for testing purposes.");
        }
        super.close();
    }

    @Override
    public void flush() throws IOException {
        if (throwsException) {
            throw new IOException("Exception thrown for testing purposes.");
        }
        super.flush();
    }

    @Override
    public void write(byte buffer[]) throws IOException {
        if (throwsException) {
            throw new IOException("Exception thrown for testing purposes.");
        }
        for (int i = 0; i < buffer.length; i++) {
            write(buffer[i]);
        }
    }

    @Override
    public void write(byte buffer[], int offset, int count) throws IOException {
        if (throwsException) {
            throw new IOException("Exception thrown for testing purposes.");
        }
        if (offset < 0 || count < 0 || (offset + count) > buffer.length) {
            throw new IndexOutOfBoundsException();
        }
        for (int i = offset; i < offset + count; i++) {
            write(buffer[i]);
        }
    }

    @Override
    public void write(int oneByte) throws IOException {
        if (throwsException) {
            throw new IOException("Exception thrown for testing purposes.");
        }
        if (position < size) {
            buffer[position] = (byte)(oneByte & 255);
            position++;
        } else {
            throw new IOException("Internal buffer overflow.");
        }
    }

    public byte[] toByteArray() {
        byte[] toReturn = new byte[position];
        System.arraycopy(buffer, 0, toReturn, 0, position);
        return toReturn;
    }

    public String toString() {
        return new String(buffer, 0, position);
    }

    public int size() {
        return position;
    }

    public void setThrowsException(boolean newValue) {
        throwsException = newValue;
    }
}
