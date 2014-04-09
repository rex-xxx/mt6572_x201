/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 */

/*
 * Copyright (C) 2007 Esmertec AG.
 * Copyright (C) 2007 The Android Open Source Project
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

package com.google.android.mms.pdu;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import com.mediatek.xlog.Xlog;

public class CharacterSets {
    /**
     * IANA assigned MIB enum numbers.
     *
     * From wap-230-wsp-20010705-a.pdf
     * Any-charset = <Octet 128>
     * Equivalent to the special RFC2616 charset value "*"
     */
    public static final int ANY_CHARSET = 0x00;
    public static final int US_ASCII    = 0x03;
    public static final int ISO_8859_1  = 0x04;
    public static final int ISO_8859_2  = 0x05;
    public static final int ISO_8859_3  = 0x06;
    public static final int ISO_8859_4  = 0x07;
    public static final int ISO_8859_5  = 0x08;
    public static final int ISO_8859_6  = 0x09;
    public static final int ISO_8859_7  = 0x0A;
    public static final int ISO_8859_8  = 0x0B;
    public static final int ISO_8859_9  = 0x0C;
    public static final int SHIFT_JIS   = 0x11;
    public static final int UTF_8       = 0x6A;
    public static final int BIG5        = 0x07EA;
    public static final int UCS2        = 0x03E8;
    public static final int UTF_16      = 0x03F7;

    /**
     * If the encoding of given data is unsupported, use UTF_8 to decode it.
     */
    public static final int DEFAULT_CHARSET = UTF_8;

    /**
     * Array of MIB enum numbers.
     */
    private static final int[] MIBENUM_NUMBERS = {
        ANY_CHARSET,
        US_ASCII,
        ISO_8859_1,
        ISO_8859_2,
        ISO_8859_3,
        ISO_8859_4,
        ISO_8859_5,
        ISO_8859_6,
        ISO_8859_7,
        ISO_8859_8,
        ISO_8859_9,
        SHIFT_JIS,
        UTF_8,
        BIG5,
        UCS2,
        UTF_16,
    };

    /**
     * The Well-known-charset Mime name.
     */
    public static final String MIMENAME_ANY_CHARSET = "*";
    public static final String MIMENAME_US_ASCII    = "us-ascii";
    public static final String MIMENAME_ISO_8859_1  = "iso-8859-1";
    public static final String MIMENAME_ISO_8859_2  = "iso-8859-2";
    public static final String MIMENAME_ISO_8859_3  = "iso-8859-3";
    public static final String MIMENAME_ISO_8859_4  = "iso-8859-4";
    public static final String MIMENAME_ISO_8859_5  = "iso-8859-5";
    public static final String MIMENAME_ISO_8859_6  = "iso-8859-6";
    public static final String MIMENAME_ISO_8859_7  = "iso-8859-7";
    public static final String MIMENAME_ISO_8859_8  = "iso-8859-8";
    public static final String MIMENAME_ISO_8859_9  = "iso-8859-9";
    public static final String MIMENAME_SHIFT_JIS   = "shift_JIS";
    public static final String MIMENAME_UTF_8       = "utf-8";
    public static final String MIMENAME_BIG5        = "big5";
    public static final String MIMENAME_UCS2        = "iso-10646-ucs-2";
    public static final String MIMENAME_UTF_16      = "utf-16";

    public static final String DEFAULT_CHARSET_NAME = MIMENAME_UTF_8;

    /**
     * Array of the names of character sets.
     */
    private static final String[] MIME_NAMES = {
        MIMENAME_ANY_CHARSET,
        MIMENAME_US_ASCII,
        MIMENAME_ISO_8859_1,
        MIMENAME_ISO_8859_2,
        MIMENAME_ISO_8859_3,
        MIMENAME_ISO_8859_4,
        MIMENAME_ISO_8859_5,
        MIMENAME_ISO_8859_6,
        MIMENAME_ISO_8859_7,
        MIMENAME_ISO_8859_8,
        MIMENAME_ISO_8859_9,
        MIMENAME_SHIFT_JIS,
        MIMENAME_UTF_8,
        MIMENAME_BIG5,
        MIMENAME_UCS2,
        MIMENAME_UTF_16,
    };

    private static final HashMap<Integer, String> MIBENUM_TO_NAME_MAP;
    private static final HashMap<String, Integer> NAME_TO_MIBENUM_MAP;

    static {
        // Create the HashMaps.
        MIBENUM_TO_NAME_MAP = new HashMap<Integer, String>();
        NAME_TO_MIBENUM_MAP = new HashMap<String, Integer>();
        assert(MIBENUM_NUMBERS.length == MIME_NAMES.length);
        int count = MIBENUM_NUMBERS.length - 1;
        for(int i = 0; i <= count; i++) {
            MIBENUM_TO_NAME_MAP.put(MIBENUM_NUMBERS[i], MIME_NAMES[i]);
            NAME_TO_MIBENUM_MAP.put(MIME_NAMES[i], MIBENUM_NUMBERS[i]);
        }
    }

    private CharacterSets() {} // Non-instantiatable

    /**
     * Map an MIBEnum number to the name of the charset which this number
     * is assigned to by IANA.
     *
     * @param mibEnumValue An IANA assigned MIBEnum number.
     * @return The name string of the charset.
     * @throws UnsupportedEncodingException
     */
    public static String getMimeName(int mibEnumValue)
            throws UnsupportedEncodingException {
        String name = MIBENUM_TO_NAME_MAP.get(mibEnumValue);
        if (name == null) {
            /// M:Code analyze 007,add for ALPS00353101,if the charset can not be found in original map,
            /// try to find it in extensional map @{
            name = MIBENUM_TO_NAME_MAP_EXTENDS.get(mibEnumValue);
            if (name == null) {
                return MIMENAME_UTF_16;
            }
            /// @}
        }
        return name;
    }

    /**
     * Map a well-known charset name to its assigned MIBEnum number.
     *
     * @param mimeName The charset name.
     * @return The MIBEnum number assigned by IANA for this charset.
     * @throws UnsupportedEncodingException
     */
    public static int getMibEnumValue(String mimeName)
            throws UnsupportedEncodingException {
        if(null == mimeName) {
            return -1;
        }
        /// M:Code analyze 008,add for ALPS00353101,make the string into lowercase @{
        mimeName = mimeName.toLowerCase();
        /// @}
        Integer mibEnumValue = NAME_TO_MIBENUM_MAP.get(mimeName);
        if (mibEnumValue == null) {
            Xlog.i(LOG_TAG, "getMibEnumValue failed, mimeName is: " + mimeName);
            throw new UnsupportedEncodingException();
        }
        return mibEnumValue;
    }

    /// new variable and methods
    /// M:Code analyze 001,add for ALPS00353101,add log tag @{
    private static final String LOG_TAG = "CharacterSets";
    /// @}
    /// M:Code analyze 002,add for ALPS00353101  extends charsets and reference:
    /// http://www.iana.org/assignments/character-sets/ @{
    public static final int BIG5_HKSCS = 0x0835;//2101
    public static final int BOCU_1 = 0x03FC; //1020
    public static final int CESU_8 = 0x03F8;//1016
    public static final int CP864 = 0x0803;//2051
    public static final int EUC_JP = 0x12;//18
    public static final int EUC_KR = 0x26;//38
    public static final int GB18030 = 0x72;//114
    public static final int GBK = 0x71;//113
    public static final int HZ_GB_2312 = 0x0825;//2085
    public static final int GB_2312 = 0x07E9;//2025
    public static final int ISO_2022_CN = 0x68;//104
    public static final int ISO_2022_CN_EXT = 0x69;//105
    public static final int ISO_2022_JP = 0x27;//39
    public static final int ISO_2022_KR = 0x25;//37
    public static final int ISO_8859_10 = 0x0D;//13
    public static final int ISO_8859_13 = 0x6D;//109
    public static final int ISO_8859_14 = 0x6E;//110
    public static final int ISO_8859_15 = 0x6F;//111
    public static final int ISO_8859_16 = 0x70;//112
    public static final int KOI8_R = 0x0824;//2084
    public static final int KOI8_U = 0x0828;//2088
    public static final int MACINTOSH = 0x07EB;//2027
    public static final int SCSU = 0x03F3;//1011
    public static final int TIS_620 = 0x08D3;//2259
    public static final int UTF_16BE = 0x03F5;//1013
    public static final int UTF_16LE = 0x03F6;//1014
    public static final int UTF_32 = 0x03F9;//1017
    public static final int UTF_32BE= 0x03FA;//1018
    public static final int UTF_32LE = 0x03FB;//1019
    public static final int UTF_7 = 0x03F4;//1012
    public static final int WINDOWS_1250 = 0x08CA;//2250
    public static final int WINDOWS_1251 = 0x08CB;//2251
    public static final int WINDOWS_1252 = 0x08CC;//2252
    public static final int WINDOWS_1253 = 0x08CD;//2253
    public static final int WINDOWS_1254 = 0x08CE;//2254
    public static final int WINDOWS_1255 = 0x08CF;//2255
    public static final int WINDOWS_1256 = 0x08D0;//2256
    public static final int WINDOWS_1257 = 0x08D1;//2257
    public static final int WINDOWS_1258 = 0x08D2;//2258
    /// @}
    /// M:Code analyze 003,add for ALPS00353101, Extend array of MIB enum numbers @{
    private static final int[] MIBENUM_NUMBERS_EXTENDS = {
       BIG5_HKSCS,
       BOCU_1,
       CESU_8,
       CP864,
       EUC_JP,
       EUC_KR,
       GB18030,
       GBK,
       HZ_GB_2312,
       GB_2312,
       ISO_2022_CN,
       ISO_2022_CN_EXT,
       ISO_2022_JP,
       ISO_2022_KR,
       ISO_8859_10,
       ISO_8859_13,
       ISO_8859_14,
       ISO_8859_15,
       ISO_8859_16,
       KOI8_R,
       KOI8_U,
       MACINTOSH,
       SCSU,
       TIS_620,
       UTF_16BE,
       UTF_16LE,
       UTF_32,
       UTF_32BE,
       UTF_32LE,
       UTF_7,
       WINDOWS_1250,
       WINDOWS_1251,
       WINDOWS_1252,
       WINDOWS_1253,
       WINDOWS_1254,
       WINDOWS_1255,
       WINDOWS_1256,
       WINDOWS_1257,
       WINDOWS_1258,
    };
    /// @}
    /// M:Code analyze 004,add for ALPS00353101, extends charsets and reference:
    /// http://www.iana.org/assignments/character-sets/ @{
    public static final String MIMENAME_BIG5_HKSCS = "Big5-HKSCS";
    public static final String MIMENAME_BOCU_1 = "BOCU-1";
    public static final String MIMENAME_CESU_8 = "CESU-8";
    public static final String MIMENAME_CP864 = "cp864";
    public static final String MIMENAME_EUC_JP = "EUC-JP";
    public static final String MIMENAME_EUC_KR = "EUC-KR";
    public static final String MIMENAME_GB18030 = "GB18030";
    public static final String MIMENAME_GBK = "GBK";
    public static final String MIMENAME_HZ_GB_2312 = "HZ-GB-2312";
    public static final String MIMENAME_GB_2312 = "GB2312";
    public static final String MIMENAME_ISO_2022_CN = "ISO-2022-CN";
    public static final String MIMENAME_ISO_2022_CN_EXT = "ISO-2022-CN-EXT";
    public static final String MIMENAME_ISO_2022_JP = "ISO-2022-JP";
    public static final String MIMENAME_ISO_2022_KR = "ISO-2022-KR";
    public static final String MIMENAME_ISO_8859_10 = "ISO-8859-10";
    public static final String MIMENAME_ISO_8859_13 = "ISO-8859-13";
    public static final String MIMENAME_ISO_8859_14 = "ISO-8859-14";
    public static final String MIMENAME_ISO_8859_15 = "ISO-8859-15";
    public static final String MIMENAME_ISO_8859_16 = "ISO-8859-16";
    public static final String MIMENAME_KOI8_R = "KOI8-R";
    public static final String MIMENAME_KOI8_U = "KOI8-U";
    public static final String MIMENAME_MACINTOSH = "macintosh";
    public static final String MIMENAME_SCSU = "SCSU";
    public static final String MIMENAME_TIS_620 = "TIS-620";
    public static final String MIMENAME_UTF_16BE = "UTF-16BE";
    public static final String MIMENAME_UTF_16LE = "UTF-16LE";
    public static final String MIMENAME_UTF_32 = "UTF-32";
    public static final String MIMENAME_UTF_32BE = "UTF-32BE";
    public static final String MIMENAME_UTF_32LE = "UTF-32LE";
    public static final String MIMENAME_UTF_7 = "UTF-7";
    public static final String MIMENAME_WINDOWS_1250 = "windows-1250";
    public static final String MIMENAME_WINDOWS_1251 = "windows-1251";
    public static final String MIMENAME_WINDOWS_1252 = "windows-1252";
    public static final String MIMENAME_WINDOWS_1253 = "windows-1253";
    public static final String MIMENAME_WINDOWS_1254 = "windows-1254";
    public static final String MIMENAME_WINDOWS_1255 = "windows-1255";
    public static final String MIMENAME_WINDOWS_1256 = "windows-1256";
    public static final String MIMENAME_WINDOWS_1257 = "windows-1257";
    public static final String MIMENAME_WINDOWS_1258 = "windows-1258";
    /// @}
    /// M:Code analyze 005,add for ALPS00353101, extends MIMINAME @}
    private static final String[] MIME_NAMES_EXTENDS = {
        MIMENAME_BIG5_HKSCS,
        MIMENAME_BOCU_1,
        MIMENAME_CESU_8,
        MIMENAME_CP864,
        MIMENAME_EUC_JP,
        MIMENAME_EUC_KR,
        MIMENAME_GB18030,
        MIMENAME_GBK,
        MIMENAME_HZ_GB_2312,
        MIMENAME_GB_2312,
        MIMENAME_ISO_2022_CN,
        MIMENAME_ISO_2022_CN_EXT,
        MIMENAME_ISO_2022_JP,
        MIMENAME_ISO_2022_KR,
        MIMENAME_ISO_8859_10,
        MIMENAME_ISO_8859_13,
        MIMENAME_ISO_8859_14,
        MIMENAME_ISO_8859_15,
        MIMENAME_ISO_8859_16,
        MIMENAME_KOI8_R,
        MIMENAME_KOI8_U,
        MIMENAME_MACINTOSH,
        MIMENAME_SCSU,
        MIMENAME_TIS_620,
        MIMENAME_UTF_16BE,
        MIMENAME_UTF_16LE,
        MIMENAME_UTF_32,
        MIMENAME_UTF_32BE,
        MIMENAME_UTF_32LE,
        MIMENAME_UTF_7,
        MIMENAME_WINDOWS_1250,
        MIMENAME_WINDOWS_1251,
        MIMENAME_WINDOWS_1252,
        MIMENAME_WINDOWS_1253,
        MIMENAME_WINDOWS_1254,
        MIMENAME_WINDOWS_1255,
        MIMENAME_WINDOWS_1256,
        MIMENAME_WINDOWS_1257,
        MIMENAME_WINDOWS_1258,
        };
    /// @}
    /// M:Code analyze 006,add for ALPS00353101,create a new hash map for extending encoding @{
    private static final HashMap<Integer, String> MIBENUM_TO_NAME_MAP_EXTENDS;
    private static final HashMap<String, Integer> NAME_TO_MIBENUM_MAP_EXTENDS;
    /// @}

    static {
        /// M:Code analyze 006,add for ALPS00353101,create a new hash map for extending encoding @{
        MIBENUM_TO_NAME_MAP_EXTENDS = new HashMap<Integer, String>();
        NAME_TO_MIBENUM_MAP_EXTENDS = new HashMap<String, Integer>();
        /// @}

        /// M:Code analyze 006,add for ALPS00353101,create a new hash map for extending encoding,
        /// set values into the new hash map @{
        assert (MIBENUM_NUMBERS_EXTENDS.length == MIME_NAMES_EXTENDS.length);
        int len = MIBENUM_NUMBERS_EXTENDS.length - 1;
        for (int i = 0; i <= len; i++) {
            MIBENUM_TO_NAME_MAP_EXTENDS.put(MIBENUM_NUMBERS_EXTENDS[i], MIME_NAMES_EXTENDS[i]);
            NAME_TO_MIBENUM_MAP_EXTENDS.put(MIME_NAMES_EXTENDS[i], MIBENUM_NUMBERS_EXTENDS[i]);
        }
        /// @}
    }
}
