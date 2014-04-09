/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2006-2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.internal.telephony.cat;

import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.IccUtils;
import com.android.internal.telephony.cat.Duration.TimeUnit;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.mediatek.common.telephony.OtherAddress;
import com.mediatek.common.telephony.BipUtils;
import com.mediatek.common.telephony.TransportProtocol;
import com.mediatek.common.telephony.BearerDesc;

abstract class ValueParser {

    /**
     * Search for a Command Details object from a list.
     * 
     * @param ctlvs List of ComprehensionTlv objects used for search
     * @return An CtlvCommandDetails object found from the objects. If no
     *         Command Details object is found, ResultException is thrown.
     * @throws ResultException
     */
    static CommandDetails retrieveCommandDetails(ComprehensionTlv ctlv)
            throws ResultException {

        CommandDetails cmdDet = new CommandDetails();
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            cmdDet.compRequired = ctlv.isComprehensionRequired();
            cmdDet.commandNumber = rawValue[valueIndex] & 0xff;
            cmdDet.typeOfCommand = rawValue[valueIndex + 1] & 0xff;
            cmdDet.commandQualifier = rawValue[valueIndex + 2] & 0xff;
            return cmdDet;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    /**
     * Search for a Device Identities object from a list.
     * 
     * @param ctlvs List of ComprehensionTlv objects used for search
     * @return An CtlvDeviceIdentities object found from the objects. If no
     *         Command Details object is found, ResultException is thrown.
     * @throws ResultException
     */
    static DeviceIdentities retrieveDeviceIdentities(ComprehensionTlv ctlv)
            throws ResultException {

        DeviceIdentities devIds = new DeviceIdentities();
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            devIds.sourceId = rawValue[valueIndex] & 0xff;
            devIds.destinationId = rawValue[valueIndex + 1] & 0xff;
            return devIds;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
    }

    /**
     * Retrieves Duration information from the Duration COMPREHENSION-TLV
     * object.
     * 
     * @param ctlv A Text Attribute COMPREHENSION-TLV object
     * @return A Duration object
     * @throws ResultException
     */
    static Duration retrieveDuration(ComprehensionTlv ctlv) throws ResultException {
        int timeInterval = 0;
        TimeUnit timeUnit = TimeUnit.SECOND;

        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();

        try {
            timeUnit = TimeUnit.values()[(rawValue[valueIndex] & 0xff)];
            timeInterval = rawValue[valueIndex + 1] & 0xff;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
        return new Duration(timeInterval, timeUnit);
    }

    /**
     * Retrieves Item information from the COMPREHENSION-TLV object.
     * 
     * @param ctlv A Text Attribute COMPREHENSION-TLV object
     * @return An Item
     * @throws ResultException
     */
    static Item retrieveItem(ComprehensionTlv ctlv) throws ResultException {
        Item item = null;

        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();

        if (length != 0) {
            int textLen = length - 1;

            try {
                int id = rawValue[valueIndex] & 0xff;
                // textLen = checkItemString(rawValue, valueIndex + 1, textLen);
                textLen = removeInvalidCharInItemTextString(rawValue, valueIndex, textLen);
                String text = IccUtils.adnStringFieldToString(rawValue,
                        valueIndex + 1, textLen);
                item = new Item(id, text);
            } catch (IndexOutOfBoundsException e) {
                //throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                CatLog.d("ValueParser", "retrieveItem fail");
            }
        }

        return item;
    }

    static int removeInvalidCharInItemTextString(byte[] rawValue, int valueIndex, int textLen)
    {
        Boolean isucs2 = false;
        int len = textLen;
        CatLog.d("ValueParser", "Try to remove invalid raw data 0xf0, valueIndex: " + valueIndex + ", textLen: "+ textLen);
        if (textLen >= 1 && rawValue[valueIndex+1] == (byte) 0x80 ||
            textLen >= 3 && rawValue[valueIndex+1] == (byte) 0x81 ||
            textLen >= 4 && rawValue[valueIndex+1] == (byte) 0x82)
        {
    	    /* The text string format is UCS2 */
            isucs2 = true;
        }
        CatLog.d("ValueParser", "Is the text string format UCS2? " + isucs2);
        if (!isucs2 && textLen > 0)
        {
            /* Remove invalid char only when it is not UCS2 format */
            for(int i = textLen; i > 0; i -= 1) {
                if(rawValue[valueIndex+i] == (byte) 0xF0) {
                    CatLog.d("ValueParser", "find invalid raw data 0xf0");
                    len -= 1;
                }
                else
                    break;
            }
        }
		CatLog.d("ValueParser", "new textLen: " + len);
        return len;
    }
	
    static int checkItemString(byte[] raw, int offset, int length) {
        if ((raw[offset] & 0xff) != 0x80) {
            // non-ucs2
            CatLog.d("ValueParser", "don't do check for non-ucs2 raw data");
            return length;
        }

        int len = length;
        CatLog.d("ValueParser", "given length is " + length);
        for (int i = raw.length - 1; i > offset; i -= 2) {
            if ((raw[i] & 0xff) == 0 && (raw[i - 1] & 0xff) == 0) {
                CatLog.d("ValueParser", "find invalid raw data 0x00");
                len -= 2;
            }
        }

        CatLog.d("ValueParser", "useful length is " + length);
        return len;
    }

    /**
     * Retrieves Item id information from the COMPREHENSION-TLV object.
     * 
     * @param ctlv A Text Attribute COMPREHENSION-TLV object
     * @return An Item id
     * @throws ResultException
     */
    static int retrieveItemId(ComprehensionTlv ctlv) throws ResultException {
        int id = 0;

        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();

        try {
            id = rawValue[valueIndex] & 0xff;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        return id;
    }

    /**
     * Retrieves icon id from an Icon Identifier COMPREHENSION-TLV object
     * 
     * @param ctlv An Icon Identifier COMPREHENSION-TLV object
     * @return IconId instance
     * @throws ResultException
     */
    static IconId retrieveIconId(ComprehensionTlv ctlv) throws ResultException {
        IconId id = new IconId();

        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            id.selfExplanatory = (rawValue[valueIndex++] & 0xff) == 0x00;
            id.recordNumber = rawValue[valueIndex] & 0xff;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        return id;
    }

    /**
     * Retrieves item icons id from an Icon Identifier List COMPREHENSION-TLV
     * object
     * 
     * @param ctlv An Item Icon List Identifier COMPREHENSION-TLV object
     * @return ItemsIconId instance
     * @throws ResultException
     */
    static ItemsIconId retrieveItemsIconId(ComprehensionTlv ctlv)
            throws ResultException {
        CatLog.d("ValueParser", "retrieveItemsIconId:");
        ItemsIconId id = new ItemsIconId();

        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int numOfItems = ctlv.getLength() - 1;
        id.recordNumbers = new int[numOfItems];

        try {
            // get icon self-explanatory
            id.selfExplanatory = (rawValue[valueIndex++] & 0xff) == 0x00;

            for (int index = 0; index < numOfItems;) {
                id.recordNumbers[index++] = rawValue[valueIndex++];
            }
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
        return id;
    }

    /**
     * Retrieves text attribute information from the Text Attribute
     * COMPREHENSION-TLV object.
     * 
     * @param ctlv A Text Attribute COMPREHENSION-TLV object
     * @return A list of TextAttribute objects
     * @throws ResultException
     */
    static List<TextAttribute> retrieveTextAttribute(ComprehensionTlv ctlv)
            throws ResultException {
        ArrayList<TextAttribute> lst = new ArrayList<TextAttribute>();

        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();

        if (length != 0) {
            // Each attribute is consisted of four bytes
            int itemCount = length / 4;

            try {
                for (int i = 0; i < itemCount; i++, valueIndex += 4) {
                    int start = rawValue[valueIndex] & 0xff;
                    int textLength = rawValue[valueIndex + 1] & 0xff;
                    int format = rawValue[valueIndex + 2] & 0xff;
                    int colorValue = rawValue[valueIndex + 3] & 0xff;

                    int alignValue = format & 0x03;
                    TextAlignment align = TextAlignment.fromInt(alignValue);

                    int sizeValue = (format >> 2) & 0x03;
                    FontSize size = FontSize.fromInt(sizeValue);
                    if (size == null) {
                        // Font size value is not defined. Use default.
                        size = FontSize.NORMAL;
                    }

                    boolean bold = (format & 0x10) != 0;
                    boolean italic = (format & 0x20) != 0;
                    boolean underlined = (format & 0x40) != 0;
                    boolean strikeThrough = (format & 0x80) != 0;

                    TextColor color = TextColor.fromInt(colorValue);

                    TextAttribute attr = new TextAttribute(start, textLength,
                            align, size, bold, italic, underlined,
                            strikeThrough, color);
                    lst.add(attr);
                }

                return lst;

            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        return null;
    }

    /**
     * Retrieves alpha identifier from an Alpha Identifier COMPREHENSION-TLV
     * object.
     * 
     * @param ctlv An Alpha Identifier COMPREHENSION-TLV object
     * @return String corresponding to the alpha identifier
     * @throws ResultException
     */
    static String retrieveAlphaId(ComprehensionTlv ctlv) throws ResultException {

        if (ctlv != null) {
            byte[] rawValue = ctlv.getRawValue();
            int valueIndex = ctlv.getValueIndex();
            int length = ctlv.getLength();
            if (length != 0) {
                try {
                    return IccUtils.adnStringFieldToString(rawValue, valueIndex,
                            length);
                } catch (IndexOutOfBoundsException e) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

    /**
     * Retrieves text from the Text COMPREHENSION-TLV object, and decodes it
     * into a Java String.
     * 
     * @param ctlv A Text COMPREHENSION-TLV object
     * @return A Java String object decoded from the Text object
     * @throws ResultException
     */
    static String retrieveTextString(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        byte codingScheme = 0x00;
        String text = null;
        int textLen = ctlv.getLength();

        // In case the text length is 0, return a null string.
        if (textLen == 0) {
            return text;
        } else {
            // one byte is coding scheme
            textLen -= 1;
        }

        try {
            codingScheme = (byte) (rawValue[valueIndex] & 0x0c);

            if (codingScheme == 0x00) { // GSM 7-bit packed
                text = GsmAlphabet.gsm7BitPackedToString(rawValue,
                        valueIndex + 1, (textLen * 8) / 7);
            } else if (codingScheme == 0x04) { // GSM 8-bit unpacked
                text = GsmAlphabet.gsm8BitUnpackedToString(rawValue,
                        valueIndex + 1, textLen);
            } else if (codingScheme == 0x08) { // UCS2
                text = new String(rawValue, valueIndex + 1, textLen, "UTF-16");
            } else {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }

            return text;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        } catch (UnsupportedEncodingException e) {
            // This should never happen.
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    // Add by Huibin Mao Mtk80229
    // ICS Migration start

    static BearerDesc retrieveBearerDesc(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        BearerDesc bearerDesc = new BearerDesc();

        try {
            int bearerType = rawValue[valueIndex++] & 0xff;
            bearerDesc.bearerType = bearerType;

            if (BipUtils.BEARER_TYPE_GPRS == bearerType) {
                bearerDesc.precedence = rawValue[valueIndex++] & 0xff;
                bearerDesc.delay = rawValue[valueIndex++] & 0xff;
                bearerDesc.reliability = rawValue[valueIndex++] & 0xff;
                bearerDesc.peak = rawValue[valueIndex++] & 0xff;
                bearerDesc.mean = rawValue[valueIndex++] & 0xff;
                bearerDesc.pdpType = rawValue[valueIndex++] & 0xff;
            } else if (BipUtils.BEARER_TYPE_CSD == bearerType) {
                CatLog.d("CAT", "retrieveBearerDesc: unsupport CSD");
                throw new ResultException(ResultCode.BEYOND_TERMINAL_CAPABILITY);
            } else {
                CatLog.d("CAT", "retrieveBearerDesc: un-understood bearer type");
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("CAT", "retrieveBearerDesc: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        return bearerDesc;
    }

    static int retrieveBufferSize(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int size = 0;

        try {
            size = ((rawValue[valueIndex] & 0xff) << 8) + (rawValue[valueIndex + 1] & 0xff);
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("CAT", "retrieveBufferSize: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        return size;
    }

        static String retrieveNetworkAccessName(ComprehensionTlv ctlv) throws ResultException {
            byte[] rawValue = ctlv.getRawValue();
            int valueIndex = ctlv.getValueIndex();
            String networkAccessName = null;
    
            try {
            // int len = ctlv.getLength() - ctlv.getValueIndex() + 1;
                int totalLen = ctlv.getLength();
                String stkNetworkAccessName = new String(rawValue, valueIndex, totalLen);
                String stkNetworkIdentifier = null;
                String stkOperatorIdentifier = null;
            
                if(stkNetworkAccessName != null && totalLen > 0){
                //Get network identifier
                    int len = rawValue[valueIndex++];
                    if(totalLen > len){
                          stkNetworkIdentifier = new String(rawValue, valueIndex, len);
                          valueIndex+=len;
                    }
                    CatLog.d("CAT", "totalLen:" + totalLen + ";" + valueIndex + ";" + len);
               
                    //Get operator identififer
                    String tmp_string = null;
                    while (totalLen > (len+1)){
                        totalLen -= (len+1);
                        len = rawValue[valueIndex++];
                        CatLog.d("CAT", "next len: " + len);
                        if(totalLen > len){
                            tmp_string = new String(rawValue, valueIndex, len);
                            if (stkOperatorIdentifier == null)
                                stkOperatorIdentifier = tmp_string;
                            else
                                stkOperatorIdentifier = stkOperatorIdentifier + "." + tmp_string;
                            tmp_string = null;
                        }
                        valueIndex+=len;
                        CatLog.d("CAT", "totalLen:" + totalLen + ";" + valueIndex + ";" + len);
                    }

                    if(stkNetworkIdentifier != null && stkOperatorIdentifier != null){
                        networkAccessName = stkNetworkIdentifier + "." + stkOperatorIdentifier;
                    }else if(stkNetworkIdentifier != null){
                        networkAccessName = stkNetworkIdentifier;
                    }
                           
                    CatLog.d("CAT", "nw:" + stkNetworkIdentifier + ";" + stkOperatorIdentifier);
                }
            
            
            } catch (IndexOutOfBoundsException e) {
                CatLog.d("CAT", "retrieveNetworkAccessName: out of bounds");
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }

            return networkAccessName;
        }

    static TransportProtocol retrieveTransportProtocol(ComprehensionTlv ctlv)
            throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int protocolType = 0;
        int portNumber = 0;

        try {
            protocolType = rawValue[valueIndex++];
            portNumber = ((rawValue[valueIndex] & 0xff) << 8) + (rawValue[valueIndex + 1] & 0xff);
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("CAT", "retrieveTransportProtocol: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        return new TransportProtocol(protocolType, portNumber);
    }

    static OtherAddress retrieveOtherAddress(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int addressType = 0;
        OtherAddress otherAddress = null;

        try {
            addressType = rawValue[valueIndex++];
            if (BipUtils.ADDRESS_TYPE_IPV4 == addressType) {
                otherAddress = new OtherAddress(addressType, rawValue, valueIndex);
            } else if (BipUtils.ADDRESS_TYPE_IPV6 == addressType) {
                return null;
                // throw new
                // ResultException(ResultCode.BEYOND_TERMINAL_CAPABILITY);
            } else {
                return null;
                // throw new
                // ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("CAT", "retrieveOtherAddress: out of bounds");
            return null;
            // throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        } catch (UnknownHostException e2) {
            CatLog.d("CAT", "retrieveOtherAddress: unknown host");
            return null;
            // throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        return otherAddress;
    }

    static int retrieveChannelDataLength(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = 0;

        CatLog.d("CAT", "valueIndex:" + valueIndex);

        try {
            length = rawValue[valueIndex] & 0xFF;
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("CAT", "retrieveTransportProtocol: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        return length;
    }

    static byte[] retrieveChannelData(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        byte[] channelData = null;

        try {
            channelData = new byte[ctlv.getLength()];
            System.arraycopy(rawValue, valueIndex, channelData, 0, channelData.length);
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("CAT", "retrieveChannelData: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        return channelData;
    }

    static byte[] retrieveNextActionIndicator(ComprehensionTlv ctlv) throws ResultException {
        byte[] nai;

        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();

        nai = new byte[length];
        try {
            for (int index = 0; index < length;) {
                nai[index++] = rawValue[valueIndex++];
            }
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        return nai;
    }
    // ICS Migration end

}
