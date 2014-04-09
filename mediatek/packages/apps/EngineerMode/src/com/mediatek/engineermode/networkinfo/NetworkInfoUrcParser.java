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

package com.mediatek.engineermode.networkinfo;

import android.content.Context;

import com.mediatek.xlog.Xlog;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class NetworkInfoUrcParser {

    private static final String TAG = "NetworkInfo";
    private static final String NW_INFO_FILE_NAME = "NetworkInfo.urc";
    private static final int TOTAL_BUF_SIZE = 5672;
    private static final int DATA_OFFSET_TWO_BIT = 2;
    private static final int DATA_OFFSET_FOUR_BIT = 4;
    private static final int DATA_OFFSET_SIX_BIT = 6;
    private static final int DATA_OFFSET_SEVEN_BIT = 7;
    private static final int DATA_OFFSET_EIGHT_BIT = 8;
    private static final int DATA_OFFSET_SIXTEEN_BIT = 16;
    private static final int DATA_OFFSET_19BIT = 19;
    private static final int DATA_OFFSET_44BIT = 44;
    private static final int DATA_OFFSET_64BIT = 64;
    private static final int DATA_OFFSET_128BIT = 128;
    private static final int DATA_OFFSET_256BIT = 256;
    private static final int DATA_OFFSET_4096BIT = 4096;
    private static final Boolean ALIGN_MENT_ENABLE = true;
    private static final Boolean GPRS_MODE_ENABLE = true;
    private static final Boolean AMR_SUPPORT_ENABLE = true;
    private static final Boolean FWPNC_LAI_INFO_ENABLE = false;

    private String mDataString;
    private int mCsceEMServCellSStatusIndSize = DATA_OFFSET_44BIT * 2;

    /**
     * @param context
     *            the NetworkInfoInfomation content
     */
    public NetworkInfoUrcParser(Context context) {
        byte[] data = new byte[TOTAL_BUF_SIZE];
        for (int i = 0; i < TOTAL_BUF_SIZE; i++) {
            data[i] = 0;
        }
        FileInputStream inputStream = null;
        try {
            // due to use of file data storage, must follow the following way,
            // so we should have a Context parameter
            inputStream = context.openFileInput(NW_INFO_FILE_NAME);
            inputStream.read(data, 0, TOTAL_BUF_SIZE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Xlog.v(TAG, "FileNotFoundException");
        } catch (IOException e) {
            e.printStackTrace();
            Xlog.v(TAG, "IOException");
        } finally {
            try {
                if (null != inputStream) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Xlog.w(TAG, "inputStream.close(): " + e.getMessage());
            }
        }
        mDataString = new String(data);
        Xlog.v(TAG, mDataString);

        int modemType = NetworkInfo.getModemType();
        if (modemType == NetworkInfo.MODEM_TD) {
            mCsceEMServCellSStatusIndSize -= DATA_OFFSET_FOUR_BIT * DATA_OFFSET_TWO_BIT;
        }

    }

    /**
     * @param data
     *            the value of the bit
     * @param start
     *            the integer of the start position
     * @param signed
     *            the boolean of the signed is false
     * @return the value of the String for every item
     */
    private String getValueFromByte(String data, int start, boolean signed) {
        try {
            String sub = data.substring(start, start + DATA_OFFSET_TWO_BIT);
            // String value = Integer.valueOf(sub,16).toString();
            String value = null;
            if (signed) {
                short s = Short.valueOf(sub, DATA_OFFSET_SIXTEEN_BIT);
                Byte b = (byte) s;
                value = b.toString();
            } else {
                value = Short.valueOf(sub, DATA_OFFSET_SIXTEEN_BIT).toString();
            }
            return value;
        } catch (NumberFormatException e) {
            return "Error.";
        }
    }

    /**
     * @param data
     *            the value of the bit
     * @param start
     *            the integer of the start position
     * @param signed
     *            the boolean of the signed is false
     * @return the value of the String for every item
     */
    private String getValueFrom2Byte(String data, int start, boolean signed) {
        try {
            String low = data.substring(start, start + DATA_OFFSET_TWO_BIT);
            String high = data.substring(start + DATA_OFFSET_TWO_BIT, start + DATA_OFFSET_FOUR_BIT);
            String reverse = high + low;
            // String value = Integer.valueOf(reverse,16).toString();
            String value = null;
            if (signed) {
                int i = Integer.valueOf(reverse, DATA_OFFSET_SIXTEEN_BIT);
                Short s = (short) i;
                value = s.toString();
            } else {
                value = Integer.valueOf(reverse, DATA_OFFSET_SIXTEEN_BIT).toString();
            }
            return value;
        } catch (NumberFormatException e) {
            return "Error.";
        }
    }

    /**
     * @param data
     *            the value of the bit
     * @param start
     *            the integer of the start position
     * @param signed
     *            the boolean of the signed is false
     * @return the value of the String for every item
     */
    private String getValueFrom4Byte(String data, int start, boolean signed) {
        try {
            String byte1 = data.substring(start, start + DATA_OFFSET_TWO_BIT);
            String byte2 = data.substring(start + DATA_OFFSET_TWO_BIT, start + DATA_OFFSET_FOUR_BIT);
            String byte3 = data.substring(start + DATA_OFFSET_FOUR_BIT, start + DATA_OFFSET_SIX_BIT);
            String byte4 = data.substring(start + DATA_OFFSET_SIX_BIT, start + DATA_OFFSET_EIGHT_BIT);
            String reverse = byte4 + byte3 + byte2 + byte1;
            // String value = Long.valueOf(reverse,16).toString();
            String value = null;
            if (signed) {
                long lg = Long.valueOf(reverse, DATA_OFFSET_SIXTEEN_BIT);
                Integer i = (int) lg;
                value = i.toString();
            } else {
                value = Long.valueOf(reverse, DATA_OFFSET_SIXTEEN_BIT).toString();
            }
            return value;
        } catch (NumberFormatException e) {
            return "Error.";
        }
    }

    /**
     * @param label
     *            the String of the network item information label
     * @param data
     *            the value of he network item information label
     * @param start
     *            the start of the position bit
     * @param signed
     *            the define value is false
     * @return the label value to display
     */
    private String oneElementByte(String label, String data, int start, boolean signed) {
        String element = "";
        element += label;
        element += getValueFromByte(data, start, signed);
        element += "\n";
        return element;
    }

    /**
     * @param label
     *            the String of the network item information label
     * @param data
     *            the value of he network item information label
     * @param start
     *            the start of the position bit
     * @param signed
     *            the define value is false
     * @return the label value to display
     */
    private String oneElement2Byte(String label, String data, int start, boolean signed) {
        String element = "";
        element += label;
        element += getValueFrom2Byte(data, start, signed);
        element += "\n";
        return element;
    }

    /**
     * @param label
     *            the String of the network item information label
     * @param data
     *            the value of he network item information label
     * @param start
     *            the start of the position bit
     * @param signed
     *            the define value is false
     * @return the label value to display
     */
    private String oneElement4Byte(String label, String data, int start, boolean signed) {
        String element = "";
        element += label;
        element += getValueFrom4Byte(data, start, signed);
        element += "\n";
        return element;
    }

    /**
     * @param data
     *            the String of the network item information
     * @param start
     *            the value of the network item start position bit
     * @param dataLength
     *            the block total bit
     * @param signed
     *            the define value is false
     * @return the block value to display
     */
    private String oneBlockFromByte(String data, int start, int dataLength, boolean signed) {
        String block = "";
        for (int i = 0; i < dataLength; i++) {
            if (0 == i % DATA_OFFSET_SEVEN_BIT) {
                block += "\n";
            }
            block += getValueFromByte(data, start, signed);
            start += DATA_OFFSET_TWO_BIT;
            if (i != dataLength - 1) {
                block += ", ";
            }
        }
        block += "\n";
        return block;
    }

    /**
     * @param data
     *            the String of the network item information
     * @param start
     *            the value of the network item start position bit
     * @param dataLength
     *            the block total bit
     * @param signed
     *            the define value is false
     * @return the block value to display
     */
    private String oneBlockFrom2Byte(String data, int start, int dataLength, boolean signed) {
        String block = "";
        for (int i = 0; i < dataLength; i++) {
            if (0 == i % DATA_OFFSET_SEVEN_BIT) {
                block += "\n";
            }
            block += getValueFrom2Byte(data, start, signed);
            start += DATA_OFFSET_FOUR_BIT;
            if (i != dataLength - 1) {
                block += ", ";
            }
        }
        block += "\n";
        return block;
    }

    /**
     * @param data
     *            the String of the network item information
     * @param start
     *            the value of the network item start position bit
     * @param dataLength
     *            the block total bit
     * @param signed
     *            the define value is false
     * @return the block value to display
     */
    private String oneBlockFrom4Byte(String data, int start, int dataLength, boolean signed) {
        String block = "";
        for (int i = 0; i < dataLength; i++) {
            if (0 == i % DATA_OFFSET_SEVEN_BIT) {
                block += "\n";
            }
            block += getValueFrom4Byte(data, start, signed);
            start += DATA_OFFSET_EIGHT_BIT;
            if (i != dataLength - 1) {
                block += ", ";
            }
        }
        block += "\n";
        return block;
    }

    /**
     * @param type
     *            the integer of the network item to view
     * @return the value of the network item to display
     */
    public String getInfo(int type) {
        Xlog.v(TAG, "Get Info type: " + type);
        switch (type) {
        case Content.CELL_INDEX:
            return getCellSelInfo();
        case Content.CHANNEL_INDEX:
            return getChDscrInfo();
        case Content.CTRL_INDEX:
            return getCtrlchanInfo();
        case Content.RACH_INDEX:
            return getRACHCtrlInfo();
        case Content.LAI_INDEX:
            return getLAIInfo();
        case Content.RADIO_INDEX:
            return getRadioLinkInfo();
        case Content.MEAS_INDEX:
            return getMeasRepInfo();
        case Content.CA_INDEX:
            return getCaListInfo();
        case Content.CONTROL_INDEX:
            return getControlMsgInfo();
        case Content.SI2Q_INDEX:
            return getSI2QInfo();
        case Content.MI_INDEX:
            return getMIInfo();
        case Content.BLK_INDEX:
            return getBLKInfo();
        case Content.TBF_INDEX:
            return getTBFInfo();
        case Content.GPRS_INDEX:
            return getGPRSGenInfo();
        case Content.MM_INFO_INDEX:
            return get3GMmEmInfo();
        case Content.TCM_MMI_INDEX:
            return get3GTcmMmiEmInfo();
        case Content.CSCE_MULTIPLMN_INDEX:
            return get3GCsceEmInfoMultiPlmn();
        case Content.PERIOD_IC_BLER_REPORT_INDEX:
            return get3GMemeEmPeriodicBlerReportInd();
        case Content.URR_UMTS_SRNC_INDEX:
            return get3GUrrUmtsSrncId();
        case Content.HSERV_CELL_INDEX:
            return get3GMemeEmInfoHServCellInd();
        case Content.CSCE_NEIGH_CELL_STATUS_INDEX:
            return getxGCsceEMNeighCellSStatusIndStructSize();
        default:
            int modemType = NetworkInfo.getModemType();
            if (modemType == NetworkInfo.MODEM_FDD) {
                switch (type) {
                case Content.CSCE_SERV_CELL_STATUS_INDEX:
                    return get3GCsceEMServCellSStatusInd(false);
                case Content.UMTS_CELL_STATUS_INDEX:
                    return get3GMemeEmInfoUmtsCellStatus();
                case Content.PSDATA_RATE_STATUS_INDEX:
                    return get3GSlceEmPsDataRateStatusInd();
                default:
                    return null;
                }
            } else if (modemType == NetworkInfo.MODEM_TD) {
                switch (type) {
                case Content.CSCE_SERV_CELL_STATUS_INDEX:
                    return get3GCsceEMServCellSStatusInd(true);
                case Content.HANDOVER_SEQUENCE_INDEX:
                    return get3GHandoverSequenceIndStuct();
                case Content.UL_ADM_POOL_STATUS_INDEX:
                    return get3GUl2EmAdmPoolStatusIndStruct();
                case Content.UL_PSDATA_RATE_STATUS_INDEX:
                    return get3GUl2EmPsDataRateStatusIndStruct();
                case Content.UL_HSDSCH_RECONFIG_STATUS_INDEX:
                    return get3Gul2EmHsdschReconfigStatusIndStruct();
                case Content.UL_URLC_EVENT_STATUS_INDEX:
                    return get3GUl2EmUrlcEventStatusIndStruct();
                case Content.UL_PERIOD_IC_BLER_REPORT_INDEX:
                    return get3GUl2EmPeriodicBlerReportInd();
                default:
                    return null;
                }
            }
            return null;
        }

    }

    /**
     * @return the cellSel information
     */
    public String getCellSelInfo() {
        int start = 0;
        int end = Content.CELL_SEL_SIZE;
        String cellSelData = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------CellSelData is:\n");
        Xlog.v(TAG, cellSelData + "\n");
        Xlog.v(TAG, "NetworkInfo ------CellSelInfo---------------------");

        int index = 0;
        String cellInfoString = "[RR Cell Sel]\n";
        cellInfoString += oneElementByte("crh: ", cellSelData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        cellInfoString += oneElementByte("ms_txpwr: ", cellSelData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        cellInfoString += oneElementByte("rxlev_access_min: ", cellSelData, index, false);
        return cellInfoString;
    }

    /**
     * @return the ChDscr information
     */
    public String getChDscrInfo() {
        int start = Content.CELL_SEL_SIZE;
        int end = start + Content.CH_DSCR_SIZE;
        String chDscrData = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------ChDscrData is:\n");
        Xlog.v(TAG, chDscrData + "\n");
        Xlog.v(TAG, "NetworkInfo ------ChDscrInfo---------------------");

        int index = 0;
        String chDscrString = "[RR Ch Dscr]\n";
        chDscrString += oneElementByte("channel_type: ", chDscrData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        chDscrString += oneElementByte("tn: ", chDscrData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        chDscrString += oneElementByte("tsc: ", chDscrData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        chDscrString += oneElementByte("hopping_flag: ", chDscrData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        chDscrString += oneElementByte("maio: ", chDscrData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        chDscrString += oneElementByte("hsn: ", chDscrData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        chDscrString += oneElementByte("num_of_carriers: ", chDscrData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_TWO_BIT;// alignment
        }
        chDscrString += "arfcn:";
        final int arfcnSize = DATA_OFFSET_64BIT;
        chDscrString += oneBlockFrom2Byte(chDscrData, index, arfcnSize, false);
        index += DATA_OFFSET_256BIT;
        chDscrString += oneElementByte("is_BCCH_arfcn_valid: ", chDscrData, index, true);
        index += DATA_OFFSET_TWO_BIT;
        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_TWO_BIT;// alignment
        }
        chDscrString += oneElement2Byte("BCCH_arfcn: ", chDscrData, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        chDscrString += oneElementByte("cipher_algo: ", chDscrData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        if (AMR_SUPPORT_ENABLE) {
            chDscrString += oneElementByte("amr_valid: ", chDscrData, index, true);
            index += DATA_OFFSET_TWO_BIT;
            chDscrString += oneElementByte("mr_ver: ", chDscrData, index, false);
            index += DATA_OFFSET_TWO_BIT;
            chDscrString += oneElementByte("nscb: ", chDscrData, index, true);
            index += DATA_OFFSET_TWO_BIT;
            chDscrString += oneElementByte("icmi: ", chDscrData, index, true);
            index += DATA_OFFSET_TWO_BIT;
            chDscrString += oneElementByte("start_codec_mode: ", chDscrData, index, false);
            index += DATA_OFFSET_TWO_BIT;
            chDscrString += oneElementByte("acs: ", chDscrData, index, false);
            index += DATA_OFFSET_TWO_BIT;
            chDscrString += "threshold:";
            final int thresholdSize = 3;
            chDscrString += oneBlockFromByte(chDscrData, index, thresholdSize, false);
            index += DATA_OFFSET_SIX_BIT;
            chDscrString += "hysteresis:";
            final int hysteresisSize = 3;
            chDscrString += oneBlockFromByte(chDscrData, index, hysteresisSize, false);
        }
        return chDscrString;
    }

    /**
     * @return the Control channel information
     */
    public String getCtrlchanInfo() {
        int start = Content.CELL_SEL_SIZE + Content.CH_DSCR_SIZE;
        int end = start + Content.CTRL_CHAN_SIZE;
        String ctrlchanData = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------CtrlchanData is:\n");
        Xlog.v(TAG, ctrlchanData + "\n");
        Xlog.v(TAG, "NetworkInfo ------CtrlchanInfo---------------------");

        int index = 0;
        String ctrlchanString = "[RR Ctrl chan]\n";
        ctrlchanString += oneElementByte("mscr: ", ctrlchanData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        ctrlchanString += oneElementByte("att: ", ctrlchanData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        ctrlchanString += oneElementByte("bs_ag_blks_res: ", ctrlchanData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        ctrlchanString += oneElementByte("ccch_conf: ", ctrlchanData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        ctrlchanString += oneElementByte("cbq2: ", ctrlchanData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        ctrlchanString += oneElementByte("bs_pa_mfrms: ", ctrlchanData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        ctrlchanString += oneElementByte("t3212: ", ctrlchanData, index, false);
        return ctrlchanString;
    }

    /**
     * @return the RACHCtrl information
     */
    public String getRACHCtrlInfo() {
        int start = Content.CELL_SEL_SIZE + Content.CH_DSCR_SIZE + Content.CTRL_CHAN_SIZE;
        int end = start + Content.RACH_CTRL_SIZE;
        String raCHCtrlData = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------RACHCtrlData is:\n");
        Xlog.v(TAG, raCHCtrlData + "\n");
        Xlog.v(TAG, "NetworkInfo ------RACHCtrlInfo---------------------");

        int index = 0;
        String raCHCtrlString = "[RR RACH Ctrl]\n";
        raCHCtrlString += oneElementByte("max_retrans: ", raCHCtrlData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        raCHCtrlString += oneElementByte("tx_integer: ", raCHCtrlData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        raCHCtrlString += oneElementByte("cba: ", raCHCtrlData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        raCHCtrlString += oneElementByte("re: ", raCHCtrlData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        raCHCtrlString += "acc_class:";
        final int accClassSize = DATA_OFFSET_TWO_BIT;
        raCHCtrlString += oneBlockFromByte(raCHCtrlData, index, accClassSize, false);
        index += DATA_OFFSET_FOUR_BIT;
        raCHCtrlString += oneElementByte("CB_supported: ", raCHCtrlData, index, true);
        return raCHCtrlString;
    }

    /**
     * @return the LAI information
     */
    public String getLAIInfo() {
        int start = Content.CELL_SEL_SIZE + Content.CH_DSCR_SIZE + Content.CTRL_CHAN_SIZE + Content.RACH_CTRL_SIZE;
        int end = start + Content.LAI_INFO_SIZE;
        String laiInfoData = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------LAIInfoData is:\n");
        Xlog.v(TAG, laiInfoData + "\n");
        Xlog.v(TAG, "NetworkInfo ------LAIInfo---------------------");

        int index = 0;
        String laiInfoString = "[RR LAI Info]\n";
        laiInfoString += "mcc:";
        final int mccSize = 3;
        laiInfoString += oneBlockFromByte(laiInfoData, index, mccSize, false);
        index += DATA_OFFSET_SIX_BIT;
        laiInfoString += "mnc:";
        final int mncSize = 3;
        laiInfoString += oneBlockFromByte(laiInfoData, index, mncSize, false);
        index += DATA_OFFSET_SIX_BIT;
        laiInfoString += "lac:";
        final int lacSize = 2;
        laiInfoString += oneBlockFromByte(laiInfoData, index, lacSize, false);
        index += DATA_OFFSET_FOUR_BIT;
        laiInfoString += oneElement2Byte("cell_id: ", laiInfoData, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        laiInfoString += oneElementByte("nc_info_index: ", laiInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        laiInfoString += oneElementByte("nmo: ", laiInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        laiInfoString += oneElementByte("supported_Band: ", laiInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_TWO_BIT;// alignment
        }
        return laiInfoString;
    }

    /**
     * @return the Radio Link information
     */
    public String getRadioLinkInfo() {
        int start = Content.CELL_SEL_SIZE + Content.CH_DSCR_SIZE + Content.CTRL_CHAN_SIZE + Content.RACH_CTRL_SIZE
                + Content.LAI_INFO_SIZE;
        int end = start + Content.RADIO_LINK_SIZE;
        String radioLinkData = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------RadioLinkData is:\n");
        Xlog.v(TAG, radioLinkData + "\n");
        Xlog.v(TAG, "NetworkInfo ------RadioLinkInfo---------------------");

        int index = 0;
        String radioLinkString = "[RR Radio Link]\n";
        radioLinkString += oneElement2Byte("max_value: ", radioLinkData, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        radioLinkString += oneElement2Byte("current_value: ", radioLinkData, index, true);
        index += DATA_OFFSET_FOUR_BIT;
        radioLinkString += oneElementByte("dtx_ind: ", radioLinkData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        radioLinkString += oneElementByte("dtx_used: ", radioLinkData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        radioLinkString += oneElementByte("is_dsf: ", radioLinkData, index, true);
        return radioLinkString;
    }

    /**
     * @return the MeasRep information
     */
    public String getMeasRepInfo() {
        int start = Content.CELL_SEL_SIZE + Content.CH_DSCR_SIZE + Content.CTRL_CHAN_SIZE + Content.RACH_CTRL_SIZE
                + Content.LAI_INFO_SIZE + Content.RADIO_LINK_SIZE;
        int end = start + Content.MEAS_REP_SIZE;
        String measRepData = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------MeasRepData is:\n");
        Xlog.v(TAG, measRepData + "\n");
        Xlog.v(TAG, "NetworkInfo ------MeasRepInfo---------------------");

        int index = 0;
        String measRepString = "[RR Meas Rep]\n";
        measRepString += oneElementByte("rr_state: ", measRepData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_TWO_BIT;// alignment
        }
        measRepString += oneElement2Byte("serving_arfcn: ", measRepData, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        measRepString += oneElementByte("serving_bsic: ", measRepData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        measRepString += oneElementByte("serving_current_band: ", measRepData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        measRepString += oneElementByte("serv_gprs_supported: ", measRepData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_TWO_BIT;// alignment
        }
        measRepString += oneElement2Byte("serv_rla_in_quarter_dbm: ", measRepData, index, true);
        index += DATA_OFFSET_FOUR_BIT;
        measRepString += oneElementByte("is_serv_BCCH_rla_valid: ", measRepData, index, true);
        index += DATA_OFFSET_TWO_BIT;
        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_TWO_BIT;// alignment
        }
        measRepString += oneElement2Byte("serv_BCCH_rla_in_dedi_state: ", measRepData, index, true);
        index += DATA_OFFSET_FOUR_BIT;
        measRepString += oneElementByte("quality: ", measRepData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        measRepString += oneElementByte("gprs_pbcch_present: ", measRepData, index, true);
        index += DATA_OFFSET_TWO_BIT;
        measRepString += oneElementByte("gprs_c31_c32_enable: ", measRepData, index, true);
        index += DATA_OFFSET_TWO_BIT;
        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_TWO_BIT;// alignment
        }
        measRepString += "c31:";
        final int c31Size = 32;
        measRepString += oneBlockFrom2Byte(measRepData, index, c31Size, true);
        index += DATA_OFFSET_128BIT;
        measRepString += oneElement2Byte("c1_serv_cell: ", measRepData, index, true);
        index += DATA_OFFSET_FOUR_BIT;
        measRepString += oneElement2Byte("c2_serv_cell: ", measRepData, index, true);
        index += DATA_OFFSET_FOUR_BIT;
        measRepString += oneElement2Byte("c31_serv_cell: ", measRepData, index, true);
        index += DATA_OFFSET_FOUR_BIT;
        measRepString += oneElementByte("num_of_carriers: ", measRepData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_TWO_BIT;// alignment
        }
        measRepString += "nc_arfcn:";
        final int ncarfcnSize = 32;
        measRepString += oneBlockFrom2Byte(measRepData, index, ncarfcnSize, false);
        index += DATA_OFFSET_128BIT;
        measRepString += "rla_in_quarter_dbm:";
        final int rlainquarterdbmSize = 32;
        measRepString += oneBlockFrom2Byte(measRepData, index, rlainquarterdbmSize, true);
        index += DATA_OFFSET_128BIT;
        measRepString += "nc_info_status:";
        final int ncinfostatusSize = 32;
        measRepString += oneBlockFromByte(measRepData, index, ncinfostatusSize, false);
        index += DATA_OFFSET_64BIT;
        measRepString += "nc_bsic:";
        final int ncbsicSize = 32;
        measRepString += oneBlockFromByte(measRepData, index, ncbsicSize, false);
        index += DATA_OFFSET_64BIT;
        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_FOUR_BIT;// alignment
        }
        measRepString += "frame_offset:";
        final int frameoffsetSize = 32;
        measRepString += oneBlockFrom4Byte(measRepData, index, frameoffsetSize, true);
        index += DATA_OFFSET_256BIT;
        measRepString += "ebit_offset:";
        final int ebitoffsetSize = 32;
        measRepString += oneBlockFrom4Byte(measRepData, index, ebitoffsetSize, true);
        index += DATA_OFFSET_256BIT;
        measRepString += "c1:";
        final int c1Size = 32;
        measRepString += oneBlockFrom2Byte(measRepData, index, c1Size, true);
        index += DATA_OFFSET_128BIT;
        measRepString += "c2:";
        final int c2Size = 32;
        measRepString += oneBlockFrom2Byte(measRepData, index, c2Size, true);
        index += DATA_OFFSET_128BIT;
        measRepString += oneElementByte("multiband_report: ", measRepData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        measRepString += oneElementByte("timing_advance: ", measRepData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        measRepString += oneElement2Byte("tx_power_level: ", measRepData, index, true);
        index += DATA_OFFSET_FOUR_BIT;
        measRepString += oneElement2Byte("serv_rla_full_value_in_quater_dbm: ", measRepData, index, true);
        index += DATA_OFFSET_FOUR_BIT;
        measRepString += oneElementByte("nco: ", measRepData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        measRepString += oneElementByte("rxqual_sub: ", measRepData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        measRepString += oneElementByte("rxqual_full: ", measRepData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        measRepString += oneElementByte("amr_info_valid: ", measRepData, index, true);
        index += DATA_OFFSET_TWO_BIT;
        measRepString += oneElementByte("cmr_cmc_cmiu_cmid: ", measRepData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        measRepString += oneElementByte("c_i: ", measRepData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        measRepString += oneElement2Byte("icm: ", measRepData, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        measRepString += oneElement2Byte("acs: ", measRepData, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        if (FWPNC_LAI_INFO_ENABLE) {
            measRepString += oneElementByte("num_of_nc_lai: ", measRepData, index, false);
            index += DATA_OFFSET_TWO_BIT;
            measRepString += "nc_lai:\n";
            for (int i = 0; i < DATA_OFFSET_SIX_BIT; i++) {
                measRepString += "nc_lai[" + i + "]:\n";
                String laiInfoString = "";
                laiInfoString += "mcc:";
                final int mccSize = 3;
                laiInfoString += oneBlockFromByte(measRepData, index, mccSize, false);
                index += DATA_OFFSET_SIX_BIT;
                laiInfoString += "mnc:";
                final int mncSize = 3;
                laiInfoString += oneBlockFromByte(measRepData, index, mncSize, false);
                index += DATA_OFFSET_SIX_BIT;
                laiInfoString += "lac:";
                final int lacSize = 2;
                laiInfoString += oneBlockFromByte(measRepData, index, lacSize, false);
                index += DATA_OFFSET_FOUR_BIT;
                laiInfoString += oneElement2Byte("cell_id: ", measRepData, index, false);
                index += DATA_OFFSET_FOUR_BIT;
                laiInfoString += oneElementByte("nc_info_index: ", measRepData, index, false);
                index += DATA_OFFSET_TWO_BIT;
                measRepString += laiInfoString;
            }
        }
        return measRepString;
    }

    /**
     * @return the Calist information
     */
    public String getCaListInfo() {
        int start = Content.CELL_SEL_SIZE + Content.CH_DSCR_SIZE + Content.CTRL_CHAN_SIZE + Content.RACH_CTRL_SIZE
                + Content.LAI_INFO_SIZE + Content.RADIO_LINK_SIZE + Content.MEAS_REP_SIZE;
        int end = start + Content.CAL_LIST_SIZE;
        String caListData = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------CaListData is:\n");
        Xlog.v(TAG, caListData + "\n");
        Xlog.v(TAG, "NetworkInfo ------CaListInfo---------------------");

        int index = 0;
        String caListString = "[RR Ca List]\n";
        caListString += oneElementByte("valid: ", caListData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        caListString += oneElementByte("number_of_channels: ", caListData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        caListString += "arfcn_list:";
        final int arfcnlistSize = 64;
        caListString += oneBlockFrom2Byte(caListData, index, arfcnlistSize, false);
        return caListString;
    }

    /**
     * @return the ControlMsg information
     */
    public String getControlMsgInfo() {
        int start = Content.CELL_SEL_SIZE + Content.CH_DSCR_SIZE + Content.CTRL_CHAN_SIZE + Content.RACH_CTRL_SIZE
                + Content.LAI_INFO_SIZE + Content.RADIO_LINK_SIZE + Content.MEAS_REP_SIZE + Content.CAL_LIST_SIZE;
        int end = start + Content.CONTROL_MSG_SIZE;
        String controlMsgData = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------ControlMsgData is:\n");
        Xlog.v(TAG, controlMsgData + "\n");
        Xlog.v(TAG, "NetworkInfo ------ControlMsgInfo---------------------");

        int index = 0;
        String controlMsgString = "[RR Control Msg]\n";
        controlMsgString += oneElementByte("msg_type: ", controlMsgData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        controlMsgString += oneElementByte("rr_cause: ", controlMsgData, index, false);
        return controlMsgString;
    }

    /**
     * @return the SI2Q information
     */
    public String getSI2QInfo() {
        int start = Content.CELL_SEL_SIZE + Content.CH_DSCR_SIZE + Content.CTRL_CHAN_SIZE + Content.RACH_CTRL_SIZE
                + Content.LAI_INFO_SIZE + Content.RADIO_LINK_SIZE + Content.MEAS_REP_SIZE + Content.CAL_LIST_SIZE
                + Content.CONTROL_MSG_SIZE;
        int end = start + Content.SI2Q_INFO_SIZE;
        String si2QInfoData = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------SI2QInfoData is:\n");
        Xlog.v(TAG, si2QInfoData + "\n");
        Xlog.v(TAG, "NetworkInfo ------SI2QInfo---------------------");

        int index = 0;
        String si2QInfoString = "[RR SI2Q Info]\n";
        si2QInfoString += oneElementByte("present: ", si2QInfoData, index, true);
        index += DATA_OFFSET_TWO_BIT;
        si2QInfoString += oneElementByte("no_of_instance: ", si2QInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        si2QInfoString += oneElementByte("emr_report: ", si2QInfoData, index, true);
        index += DATA_OFFSET_TWO_BIT;
        si2QInfoString += oneElementByte("pemr_report: ", si2QInfoData, index, true);
        index += DATA_OFFSET_TWO_BIT;
        si2QInfoString += oneElementByte("umts_parameter_exist: ", si2QInfoData, index, true);
        return si2QInfoString;
    }

    /**
     * @return the MI information
     */
    public String getMIInfo() {
        int start = Content.CELL_SEL_SIZE + Content.CH_DSCR_SIZE + Content.CTRL_CHAN_SIZE + Content.RACH_CTRL_SIZE
                + Content.LAI_INFO_SIZE + Content.RADIO_LINK_SIZE + Content.MEAS_REP_SIZE + Content.CAL_LIST_SIZE
                + Content.CONTROL_MSG_SIZE + Content.SI2Q_INFO_SIZE;
        int end = start + Content.MI_INFO_SIZE;
        String miInfoData = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------MIInfoData is:\n");
        Xlog.v(TAG, miInfoData + "\n");
        Xlog.v(TAG, "NetworkInfo ------MIInfo---------------------");

        int index = 0;
        String miInfoString = "[RR MI Info]\n";
        miInfoString += oneElementByte("present: ", miInfoData, index, true);
        index += DATA_OFFSET_TWO_BIT;
        miInfoString += oneElementByte("no_of_instance: ", miInfoData, index, true);
        index += DATA_OFFSET_TWO_BIT;
        miInfoString += oneElementByte("emr_report: ", miInfoData, index, true);
        index += DATA_OFFSET_TWO_BIT;
        miInfoString += oneElementByte("umts_parameter_exist: ", miInfoData, index, true);
        return miInfoString;
    }

    /**
     * @return the BLK information
     */
    public String getBLKInfo() {
        int start = Content.CELL_SEL_SIZE + Content.CH_DSCR_SIZE + Content.CTRL_CHAN_SIZE + Content.RACH_CTRL_SIZE
                + Content.LAI_INFO_SIZE + Content.RADIO_LINK_SIZE + Content.MEAS_REP_SIZE + Content.CAL_LIST_SIZE
                + Content.CONTROL_MSG_SIZE + Content.SI2Q_INFO_SIZE + Content.MI_INFO_SIZE;
        int end = start + Content.BLK_INFO_SIZE;
        String blkInfoData = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------BLKInfoData is:\n");
        Xlog.v(TAG, blkInfoData + "\n");
        Xlog.v(TAG, "NetworkInfo ------BLKInfo---------------------");

        int index = 0;
        String blkInfoString = "[RR BLK Info]\n";
        blkInfoString += oneElementByte("ul_coding_scheme: ", blkInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        blkInfoString += oneElementByte("ul_cv: ", blkInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        blkInfoString += oneElementByte("ul_tlli: ", blkInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_TWO_BIT;// alignment
        }
        blkInfoString += oneElement2Byte("ul_bsn1: ", blkInfoData, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        if (GPRS_MODE_ENABLE) {
            blkInfoString += oneElement2Byte("ul_bsn2: ", blkInfoData, index, false);
            index += DATA_OFFSET_FOUR_BIT;
            blkInfoString += oneElementByte("ul_cps: ", blkInfoData, index, false);
            index += DATA_OFFSET_TWO_BIT;
            blkInfoString += oneElementByte("ul_rsb: ", blkInfoData, index, false);
            index += DATA_OFFSET_TWO_BIT;
            blkInfoString += oneElementByte("ul_spb: ", blkInfoData, index, false);
            index += DATA_OFFSET_TWO_BIT;
        }
        blkInfoString += oneElementByte("dl_c_value_in_rx_level: ", blkInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        blkInfoString += oneElementByte("dl_rxqual: ", blkInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        blkInfoString += oneElementByte("dl_sign_var: ", blkInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        blkInfoString += oneElementByte("dl_coding_scheme: ", blkInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        blkInfoString += oneElementByte("dl_fbi: ", blkInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        blkInfoString += oneElement2Byte("dl_bsn1: ", blkInfoData, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        if (GPRS_MODE_ENABLE) {
            blkInfoString += oneElement2Byte("dl_bsn2: ", blkInfoData, index, false);
            index += DATA_OFFSET_FOUR_BIT;
            blkInfoString += oneElementByte("dl_cps: ", blkInfoData, index, false);
            index += DATA_OFFSET_TWO_BIT;
            blkInfoString += oneElementByte("dl_gmsk_mean_bep_lev: ", blkInfoData, index, false);
            index += DATA_OFFSET_TWO_BIT;
            blkInfoString += oneElementByte("dl_8psk_mean_bep_lev: ", blkInfoData, index, false);
            index += DATA_OFFSET_TWO_BIT;
            blkInfoString += "dl_tn_mean_bep_lev:";
            final int dltnmeanbeplevSize = 8;
            blkInfoString += oneBlockFromByte(blkInfoData, index, dltnmeanbeplevSize, false);
            index += DATA_OFFSET_SIXTEEN_BIT;
        }
        blkInfoString += "dl_tn_interference_lev:";
        final int dltninterferencelevSize = 8;
        blkInfoString += oneBlockFromByte(blkInfoData, index, dltninterferencelevSize, false);
        return blkInfoString;
    }

    /**
     * @return the TBF information
     */
    public String getTBFInfo() {
        int start = Content.CELL_SEL_SIZE + Content.CH_DSCR_SIZE + Content.CTRL_CHAN_SIZE + Content.RACH_CTRL_SIZE
                + Content.LAI_INFO_SIZE + Content.RADIO_LINK_SIZE + Content.MEAS_REP_SIZE + Content.CAL_LIST_SIZE
                + Content.CONTROL_MSG_SIZE + Content.SI2Q_INFO_SIZE + Content.MI_INFO_SIZE + Content.BLK_INFO_SIZE;
        int end = start + Content.TBF_INFO_SIZE;
        String tbfInfoData = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------TBFInfoData is:\n");
        Xlog.v(TAG, tbfInfoData + "\n");
        Xlog.v(TAG, "NetworkInfo ------TBFInfo---------------------");

        int index = 0;
        String tbfInfoString = "[RR TBF Info]\n";
        tbfInfoString += oneElementByte("tbf_mode: ", tbfInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        tbfInfoString += oneElementByte("ul_tbf_status: ", tbfInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        tbfInfoString += oneElementByte("ul_rel_cause: ", tbfInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        tbfInfoString += oneElementByte("ul_ts_allocation: ", tbfInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        tbfInfoString += oneElementByte("ul_rlc_mode: ", tbfInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        tbfInfoString += oneElementByte("ul_mac_mode: ", tbfInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        tbfInfoString += oneElement2Byte("number_rlc_octect: ", tbfInfoData, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        tbfInfoString += oneElementByte("ul_tfi: ", tbfInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        tbfInfoString += oneElementByte("ul_granularity: ", tbfInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        tbfInfoString += oneElementByte("ul_usf: ", tbfInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        tbfInfoString += oneElementByte("ul_tai: ", tbfInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        tbfInfoString += oneElement2Byte("ul_tqi: ", tbfInfoData, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        tbfInfoString += oneElement2Byte("ul_window_size: ", tbfInfoData, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        tbfInfoString += oneElementByte("dl_tbf_status: ", tbfInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        tbfInfoString += oneElementByte("dl_rel_cause: ", tbfInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        tbfInfoString += oneElementByte("dl_ts_allocation: ", tbfInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        tbfInfoString += oneElementByte("dl_rlc_mode: ", tbfInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        tbfInfoString += oneElementByte("dl_mac_mode: ", tbfInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        tbfInfoString += oneElementByte("dl_tfi: ", tbfInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        tbfInfoString += oneElementByte("dl_tai: ", tbfInfoData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_TWO_BIT;// alignment
        }
        tbfInfoString += oneElement2Byte("dl_window_size: ", tbfInfoData, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        if (GPRS_MODE_ENABLE) {
            tbfInfoString += oneElementByte("dl_out_of_memory: ", tbfInfoData, index, false);
        }
        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_TWO_BIT;// alignment
        }
        return tbfInfoString;
    }

    /**
     * @return the GPRS GEN information
     */
    public String getGPRSGenInfo() {
        int start = Content.CELL_SEL_SIZE + Content.CH_DSCR_SIZE + Content.CTRL_CHAN_SIZE + Content.RACH_CTRL_SIZE
                + Content.LAI_INFO_SIZE + Content.RADIO_LINK_SIZE + Content.MEAS_REP_SIZE + Content.CAL_LIST_SIZE
                + Content.CONTROL_MSG_SIZE + Content.SI2Q_INFO_SIZE + Content.MI_INFO_SIZE + Content.BLK_INFO_SIZE
                + Content.TBF_INFO_SIZE;
        int end = start + Content.GPRS_GEN_SIZE;
        String gprsGenData = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------GPRSGenData is:\n");
        Xlog.v(TAG, gprsGenData + "\n");
        Xlog.v(TAG, "NetworkInfo ------GPRSGenInfo---------------------");

        int index = 0;
        String gprsGenString = "[RR GPRS Gen]\n";
        gprsGenString += oneElement4Byte("t3192: ", gprsGenData, index, false);
        index += DATA_OFFSET_EIGHT_BIT;
        gprsGenString += oneElement4Byte("t3168: ", gprsGenData, index, false);
        index += DATA_OFFSET_EIGHT_BIT;
        gprsGenString += oneElementByte("rp: ", gprsGenData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        gprsGenString += oneElementByte("gprs_support: ", gprsGenData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        gprsGenString += oneElementByte("egprs_support: ", gprsGenData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        gprsGenString += oneElementByte("sgsn_r: ", gprsGenData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        gprsGenString += oneElementByte("pfc_support: ", gprsGenData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        gprsGenString += oneElementByte("epcr_support: ", gprsGenData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        gprsGenString += oneElementByte("bep_period: ", gprsGenData, index, false);
        index += DATA_OFFSET_TWO_BIT;
        return gprsGenString;
    }

    /**
     * @return the 2g item total bit size
     */
    int calc2GSize() {
        int sz = Content.CELL_SEL_SIZE + Content.CH_DSCR_SIZE + Content.CTRL_CHAN_SIZE + Content.RACH_CTRL_SIZE
                + Content.LAI_INFO_SIZE + Content.RADIO_LINK_SIZE + Content.MEAS_REP_SIZE + Content.CAL_LIST_SIZE
                + Content.CONTROL_MSG_SIZE + Content.SI2Q_INFO_SIZE + Content.MI_INFO_SIZE + Content.BLK_INFO_SIZE
                + Content.TBF_INFO_SIZE + Content.GPRS_GEN_SIZE;
        return sz;
    }

    /**
     * @return the 3G memory information
     */
    public String get3GMmEmInfo() {
        int start = calc2GSize() + Content.XGCSCE_NEIGH_CELL_STATUS_SIZE;

        int end = start + Content.M3G_MM_EMINFO_SIZE;
        String data = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------Get3GMmEmInfo is:\n");
        Xlog.v(TAG, data + "\n");
        Xlog.v(TAG, "NetworkInfo ------Get3GMmEmInfo---------------------");

        int index = 0;
        String ss = "[RR 3G MM EM Info]\n";
        ss += oneElementByte("t3212: ", data, index, false);
        index += DATA_OFFSET_TWO_BIT;
        ss += oneElementByte("ATT_flag: ", data, index, false);
        index += DATA_OFFSET_TWO_BIT;
        ss += oneElementByte("MM_reject_cause: ", data, index, false);
        index += DATA_OFFSET_TWO_BIT;
        ss += oneElementByte("MM_state: ", data, index, false);
        index += DATA_OFFSET_TWO_BIT;

        ss += "MCC:";
        final int mccSize = 3;
        ss += oneBlockFromByte(data, index, mccSize, false);
        index += DATA_OFFSET_SIX_BIT;

        ss += "MNC:";
        final int mncSize = 3;
        ss += oneBlockFromByte(data, index, mncSize, false);
        index += DATA_OFFSET_SIX_BIT;

        ss += "LOC:";
        final int locSize = 2;
        ss += oneBlockFromByte(data, index, locSize, false);
        index += DATA_OFFSET_FOUR_BIT;

        ss += oneElementByte("rac: ", data, index, false);
        index += DATA_OFFSET_TWO_BIT;

        ss += "TMSI:";
        final int tmsiSize = 4;
        ss += oneBlockFromByte(data, index, tmsiSize, false);
        index += DATA_OFFSET_EIGHT_BIT;

        ss += oneElementByte("is_t3212_running:", data, index, false);
        index += DATA_OFFSET_TWO_BIT;

        ss += oneElement2Byte("t3212_timer_value:", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;

        ss += oneElement2Byte("t3212_passed_time:", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;

        ss += oneElementByte("common_access_class: ", data, index, false);
        index += DATA_OFFSET_TWO_BIT;
        ss += oneElementByte("cs_access_class: ", data, index, false);
        index += DATA_OFFSET_TWO_BIT;
        ss += oneElementByte("ps_access_class: ", data, index, false);
        index += DATA_OFFSET_TWO_BIT;

        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_TWO_BIT;// alignment 1 byte
        }

        return ss;
    }

    /**
     * @return the 3G Tcm information
     */
    public String get3GTcmMmiEmInfo() {
        int start = calc2GSize() + Content.XGCSCE_NEIGH_CELL_STATUS_SIZE + Content.M3G_MM_EMINFO_SIZE;

        int end = start + Content.M_3G_TCMMMI_INFO_SIZE;
        String data = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------Get3GTcmMmiEmInfo is:\n");
        Xlog.v(TAG, data + "\n");
        Xlog.v(TAG, "NetworkInfo ------Get3GTcmMmiEmInfo---------------------");

        int index = 0;
        String ss = "[RR 3G TCM MMI EM Info]\n";
        ss += oneElementByte("num_of_valid_entries: ", data, index, false);
        index += DATA_OFFSET_TWO_BIT;

        final int gprsmaxpdpsupporrt = 3;
        for (int i = 0; i < gprsmaxpdpsupporrt; i++) {
            ss += oneElementByte("nsapi" + i + ":", data, index, false);
            index += DATA_OFFSET_TWO_BIT;
            ss += oneElementByte("data_speed_value" + i + ":", data, index, false);
            index += DATA_OFFSET_TWO_BIT;
        }

        return ss;
    }


    /**
     * @return the 3G CsceEMServCellSStatusInd information
     */
    public String get3GCsceEMServCellSStatusInd(boolean isTdd) {
        int start = calc2GSize() + Content.XGCSCE_NEIGH_CELL_STATUS_SIZE
                + Content.M3G_MM_EMINFO_SIZE + Content.M_3G_TCMMMI_INFO_SIZE;
        int end = start + mCsceEMServCellSStatusIndSize;
        String data = mDataString.substring(start, end);
        int index = 0;
        String ss = "[RR 3G CsceEMServCellSStatusInd]\n";
        ss += oneElementByte("ref_count: ", data, index, false);
        index += DATA_OFFSET_TWO_BIT;
        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_TWO_BIT;// alignment 1 byte
        }
        ss += oneElement2Byte("msg_len: ", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        ss += oneElementByte("cell_idx: ", data, index, false);
        index += DATA_OFFSET_TWO_BIT;
        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_TWO_BIT;// alignment 1 byte
        }
        ss += oneElement2Byte("uarfacn_DL: ", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        ss += oneElement2Byte("psc: ", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        ss += oneElementByte("is_s_criteria_satisfied: ", data, index, false);
        index += DATA_OFFSET_TWO_BIT;
        ss += oneElementByte("qQualmin: ", data, index, true);
        index += DATA_OFFSET_TWO_BIT;
        ss += oneElementByte("qRxlevmin: ", data, index, true);
        index += DATA_OFFSET_TWO_BIT;
        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_SIX_BIT;// alignment 3 byte
        }
        ss += oneElement4Byte("srxlev: ", data, index, true);
        index += DATA_OFFSET_EIGHT_BIT;
        ss += oneElement4Byte("spual: ", data, index, true);
        index += DATA_OFFSET_EIGHT_BIT;
        String strRscp = getValueFrom4Byte(data, index, true);
        index += DATA_OFFSET_EIGHT_BIT;
        long rscp = 0;
        try {
            rscp = Long.valueOf(strRscp) / DATA_OFFSET_4096BIT;
        } catch (NumberFormatException e) {
            Xlog.v(TAG, "rscp = Long.valueOf(strRscp)/4096 exp.");
        }
        ss += "rscp: " + rscp + "\n";
        if (!isTdd) {
            String strEcno = getValueFrom4Byte(data, index, true);
            index += DATA_OFFSET_EIGHT_BIT;
            float ecno = 0;
            try {
                ecno = Float.valueOf(strEcno) / DATA_OFFSET_4096BIT;
            } catch (NumberFormatException e) {
                Xlog.e(TAG, "ecno = Long.valueOf(strEcno)/4096 exp.");
            }
            ss += "ec_no: " + ecno + "\n";
        }
        ss += oneElement2Byte("cycle_len: ", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        if (!isTdd) {
            ss += oneElementByte("quality_measure: ", data, index, false);
            index += DATA_OFFSET_TWO_BIT;
        }
        ss += oneElementByte("band: ", data, index, false);
        index += DATA_OFFSET_TWO_BIT;
        if (isTdd) {
            if (ALIGN_MENT_ENABLE) {
                index += DATA_OFFSET_TWO_BIT;// alignment 1 byte
            }
        }
        ss += oneElement4Byte("rssi: ", data, index, true);
        index += DATA_OFFSET_EIGHT_BIT;
        ss += oneElement4Byte("cell_identity: ", data, index, false);
        index += DATA_OFFSET_EIGHT_BIT;
        return ss;
    }

    /**
     * @return the 3G CsceEmInfoMultiPlmn information
     */
    public String get3GCsceEmInfoMultiPlmn() {
        int start = calc2GSize() + Content.XGCSCE_NEIGH_CELL_STATUS_SIZE + Content.M3G_MM_EMINFO_SIZE
                + Content.M_3G_TCMMMI_INFO_SIZE + mCsceEMServCellSStatusIndSize;

        int end = start + Content.CSCE_MULTI_PLMN_SIZE;
        String data = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------Get3GCsceEmInfoMultiPlmn is:\n");
        Xlog.v(TAG, data + "\n");
        Xlog.v(TAG, "NetworkInfo ------Get3GCsceEmInfoMultiPlmn---------------------");

        int index = 0;
        String ss = "[RR 3G CsceEmInfoMultiPlmn]\n";
        ss += oneElementByte("multi_plmn_count: ", data, index, false);
        index += DATA_OFFSET_TWO_BIT;

        for (int i = 0; i < DATA_OFFSET_SIX_BIT; i++) {
            ss += oneElementByte("mcc1_" + i + ":", data, index, false);
            index += DATA_OFFSET_TWO_BIT;
            ss += oneElementByte("mcc2_" + i + ":", data, index, false);
            index += DATA_OFFSET_TWO_BIT;
            ss += oneElementByte("mcc3_" + i + ":", data, index, false);
            index += DATA_OFFSET_TWO_BIT;
            ss += oneElementByte("mnc1_" + i + ":", data, index, false);
            index += DATA_OFFSET_TWO_BIT;
            ss += oneElementByte("mnc2_" + i + ":", data, index, false);
            index += DATA_OFFSET_TWO_BIT;
            ss += oneElementByte("mnc3_" + i + ":", data, index, false);
            index += DATA_OFFSET_TWO_BIT;
        }

        return ss;
    }

    /**
     * @return the 3G MemeEmInfoUmtsCellStatus information
     */
    public String get3GMemeEmInfoUmtsCellStatus() {
        int start = calc2GSize() + Content.XGCSCE_NEIGH_CELL_STATUS_SIZE + Content.M3G_MM_EMINFO_SIZE
                + Content.M_3G_TCMMMI_INFO_SIZE + mCsceEMServCellSStatusIndSize + Content.CSCE_MULTI_PLMN_SIZE;

        int end = start + Content.UMTS_CELL_STATUS_SIZE;
        String data = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------Get3GMemeEmInfoUmtsCellStatus is:\n");
        Xlog.v(TAG, data + "\n");
        Xlog.v(TAG, "NetworkInfo ------Get3GMemeEmInfoUmtsCellStatus---------------------");

        int index = 0;
        String ss = "[RR 3G MemeEmInfoUmtsCellStatus]\n";
        ss += oneElementByte("tx_power: ", data, index, true);
        index += DATA_OFFSET_TWO_BIT;
        ss += oneElementByte("num_cells: ", data, index, false);
        index += DATA_OFFSET_TWO_BIT;

        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_FOUR_BIT;// alignment 2 byte
        }

        for (int i = 0; i < DATA_OFFSET_19BIT; i++) {
            ss += oneElement2Byte("UARFCN" + i + ":", data, index, false);
            index += DATA_OFFSET_FOUR_BIT;
            ss += oneElement2Byte("PSC" + i + ":", data, index, false);
            index += DATA_OFFSET_FOUR_BIT;
            ss += oneElement4Byte("RSCP" + i + ":", data, index, true);
            index += DATA_OFFSET_EIGHT_BIT;
            ss += oneElement4Byte("ECNO" + i + ":", data, index, true);
            index += DATA_OFFSET_EIGHT_BIT;
            ss += oneElementByte("cell_type" + i + ":", data, index, false);
            index += DATA_OFFSET_TWO_BIT;
            ss += oneElementByte("Band" + i + ":", data, index, false);
            index += DATA_OFFSET_TWO_BIT;

            if (ALIGN_MENT_ENABLE) {
                index += DATA_OFFSET_FOUR_BIT;// alignment 2 byte
            }
            ss += oneElement4Byte("RSSI" + i + ":", data, index, true);
            index += DATA_OFFSET_EIGHT_BIT;
            ss += oneElement4Byte("Cell_identity" + i + ":", data, index, false);
            index += DATA_OFFSET_EIGHT_BIT;
        }

        return ss;

    }

    /**
     * @return the 3G MemeEmPeriodicBlerReport information
     */
    public String get3GMemeEmPeriodicBlerReportInd() {
        int start = calc2GSize() + Content.XGCSCE_NEIGH_CELL_STATUS_SIZE + Content.M3G_MM_EMINFO_SIZE
                + Content.M_3G_TCMMMI_INFO_SIZE + mCsceEMServCellSStatusIndSize + Content.CSCE_MULTI_PLMN_SIZE
                + Content.UMTS_CELL_STATUS_SIZE;

        int end = start + Content.PERIOD_IC_BLER_REPORT_SIZE;
        String data = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------Get3GMemeEmPeriodicBlerReportInd is:\n");
        Xlog.v(TAG, data + "\n");
        Xlog.v(TAG, "NetworkInfo ------Get3GMemeEmPeriodicBlerReportInd---------------------");

        int index = 0;
        String ss = "[RR 3G MemeEmPeriodicBlerReportInd]\n";
        ss += oneElementByte("num_trch: ", data, index, false);
        index += DATA_OFFSET_TWO_BIT;

        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_SIX_BIT;// alignment 3 byte
        }

        for (int i = 0; i < DATA_OFFSET_EIGHT_BIT; i++) {
            ss += oneElementByte("TrCHId" + i + ":", data, index, false);
            index += DATA_OFFSET_TWO_BIT;

            if (ALIGN_MENT_ENABLE) {
                index += DATA_OFFSET_SIX_BIT;// alignment 3 byte
            }

            ss += oneElement4Byte("TotalCRC" + i + ":", data, index, false);
            index += DATA_OFFSET_EIGHT_BIT;
            ss += oneElement4Byte("BadCRC" + i + ":", data, index, false);
            index += DATA_OFFSET_EIGHT_BIT;
        }

        return ss;
    }

    /**
     * @return the 3G UrrUmtsSrnc information
     */
    public String get3GUrrUmtsSrncId() {
        int start = calc2GSize() + Content.XGCSCE_NEIGH_CELL_STATUS_SIZE + Content.M3G_MM_EMINFO_SIZE
                + Content.M_3G_TCMMMI_INFO_SIZE + mCsceEMServCellSStatusIndSize + Content.CSCE_MULTI_PLMN_SIZE
                + Content.UMTS_CELL_STATUS_SIZE + Content.PERIOD_IC_BLER_REPORT_SIZE;

        int end = start + Content.URR_UMTS_SRNC_SIZE;
        String data = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------Get3GUrrUmtsSrncId is:\n");
        Xlog.v(TAG, data + "\n");
        Xlog.v(TAG, "NetworkInfo ------Get3GUrrUmtsSrncId---------------------");

        int index = 0;
        String ss = "[RR 3G UrrUmtsSrncId]\n";
        ss += oneElement2Byte("srnc: ", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;

        return ss;
    }

    /**
     * @return the 3G SlceEmPsDataRateStatus information
     */
    public String get3GSlceEmPsDataRateStatusInd() {
        int start = calc2GSize() + Content.XGCSCE_NEIGH_CELL_STATUS_SIZE + Content.M3G_MM_EMINFO_SIZE
                + Content.M_3G_TCMMMI_INFO_SIZE + mCsceEMServCellSStatusIndSize + Content.CSCE_MULTI_PLMN_SIZE
                + Content.UMTS_CELL_STATUS_SIZE + Content.PERIOD_IC_BLER_REPORT_SIZE + Content.URR_UMTS_SRNC_SIZE;

        int end = start + Content.SLCE_PS_DATA_RATE_STATUS_SIZE;
        String data = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------Get3GSlceEmPsDataRateStatusInd is:\n");
        Xlog.v(TAG, data + "\n");
        Xlog.v(TAG, "NetworkInfo ------Get3GSlceEmPsDataRateStatusInd---------------------");

        int index = 0;
        String ss = "[RR 3G SlceEmPsDataRateStatusInd]\n";
        ss += oneElementByte("ps_number: ", data, index, false);
        index += DATA_OFFSET_TWO_BIT;

        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_SIX_BIT;// alignment 3 byte
        }

        for (int i = 0; i < DATA_OFFSET_EIGHT_BIT; i++) {
            ss += oneElementByte("RAB_ID" + i + ":", data, index, false);
            index += DATA_OFFSET_TWO_BIT;
            ss += oneElementByte("RB_UD" + i + ":", data, index, true);
            index += DATA_OFFSET_TWO_BIT;

            if (ALIGN_MENT_ENABLE) {
                index += DATA_OFFSET_FOUR_BIT;// alignment 2 byte
            }

            ss += oneElement4Byte("DL_rate" + i + ":", data, index, false);
            index += DATA_OFFSET_EIGHT_BIT;
            ss += oneElement4Byte("UL_rate" + i + ":", data, index, false);
            index += DATA_OFFSET_EIGHT_BIT;
        }

        return ss;
    }

    /**
     * @return the 3G MemeEmInfoHServCell information
     */
    public String get3GMemeEmInfoHServCellInd() {
        int start = calc2GSize() + Content.XGCSCE_NEIGH_CELL_STATUS_SIZE + Content.M3G_MM_EMINFO_SIZE
                + Content.M_3G_TCMMMI_INFO_SIZE + mCsceEMServCellSStatusIndSize + Content.CSCE_MULTI_PLMN_SIZE
                + Content.UMTS_CELL_STATUS_SIZE + Content.PERIOD_IC_BLER_REPORT_SIZE + Content.URR_UMTS_SRNC_SIZE
                + Content.SLCE_PS_DATA_RATE_STATUS_SIZE;

        int end = start + Content.MEME_HSERV_CELL_SIZE;
        String data = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------Get3GMemeEmInfoHServCellInd is:\n");
        Xlog.v(TAG, data + "\n");
        Xlog.v(TAG, "NetworkInfo ------Get3GMemeEmInfoHServCellInd---------------------");

        int index = 0;
        String ss = "[RR 3G MemeEmInfoHServCellInd]\n";
        ss += oneElement2Byte("HSDSCH_Serving_UARFCN: ", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        ss += oneElement2Byte("HSDSCH_Serving_PSC: ", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        ss += oneElement2Byte("EDCH_Serving_UARFCN: ", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        ss += oneElement2Byte("EDCH_Serving_PSC: ", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;

        return ss;

    }

    /**
     * @return the 3G HandoverSequence information
     */
    public String get3GHandoverSequenceIndStuct() {
        int start = calc2GSize() + Content.XGCSCE_NEIGH_CELL_STATUS_SIZE;

        int end = start + Content.HANDOVER_SEQUENCE_SIZE;
        String data = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------Get3GHandoverSequenceIndStuct is:\n");
        Xlog.v(TAG, data + "\n");
        Xlog.v(TAG, "NetworkInfo ------Get3GHandoverSequenceIndStuct---------------------");

        int index = 0;
        String ss = "[RR 3G HandoverSequenceIndStuct]\n";
        ss += oneElementByte("service_status: ", data, index, false);
        index += DATA_OFFSET_TWO_BIT;

        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_SIX_BIT;// alignment 3 byte
        }

        ss += "[old_cell_info:-----]\n";
        ss += oneElement2Byte("primary_uarfcn_DL: ", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        ss += oneElement2Byte("working_uarfcn: ", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        ss += oneElement2Byte("physicalCellId: ", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;

        ss += "[target_cell_info:-----]\n";
        ss += oneElement2Byte("primary_uarfcn_DL: ", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        ss += oneElement2Byte("working_uarfcn: ", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        ss += oneElement2Byte("physicalCellId: ", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;

        return ss;
    }

    /**
     * @return the 3G Ul2EmAdmPoolStatus information
     */
    public String get3GUl2EmAdmPoolStatusIndStruct() {
        int start = calc2GSize() + Content.XGCSCE_NEIGH_CELL_STATUS_SIZE + Content.HANDOVER_SEQUENCE_SIZE;

        int end = start + Content.ADM_POOL_STATUS_SIZE;
        String data = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------Get3GUl2EmAdmPoolStatusIndStruct is:\n");
        Xlog.v(TAG, data + "\n");
        Xlog.v(TAG, "NetworkInfo ------Get3GUl2EmAdmPoolStatusIndStruct---------------------");

        int index = 0;
        String ss = "[RR 3G Ul2EmAdmPoolStatusIndStruct]\n";
        ss += "[dl_adm_poll_info:-----]\n";

        for (int i = 0; i < DATA_OFFSET_FOUR_BIT; i++) {
            ss += oneElement2Byte("max_usage_kbytes" + i + ":", data, index, false);
            index += DATA_OFFSET_FOUR_BIT;
            ss += oneElement2Byte("avg_usage_kbytes" + i + ":", data, index, false);
            index += DATA_OFFSET_FOUR_BIT;
        }

        ss += "[ul_adm_poll_info:-----]\n";

        for (int i = 0; i < DATA_OFFSET_FOUR_BIT; i++) {
            ss += oneElement2Byte("max_usage_kbytes" + i + ":", data, index, false);
            index += DATA_OFFSET_FOUR_BIT;
            ss += oneElement2Byte("avg_usage_kbytes" + i + ":", data, index, false);
            index += DATA_OFFSET_FOUR_BIT;
        }
        return ss;
    }

    /**
     * @return the 3G Ul2EmPsDataRateStatus information
     */
    public String get3GUl2EmPsDataRateStatusIndStruct() {
        int start = calc2GSize() + Content.XGCSCE_NEIGH_CELL_STATUS_SIZE + Content.HANDOVER_SEQUENCE_SIZE
                + Content.ADM_POOL_STATUS_SIZE;

        int end = start + Content.UL2_PSDATA_RATE_STATUS_SIZE;
        String data = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------Get3GUl2EmPsDataRateStatusIndStruct is:\n");
        Xlog.v(TAG, data + "\n");
        Xlog.v(TAG, "NetworkInfo ------Get3GUl2EmPsDataRateStatusIndStruct---------------------");

        int index = 0;
        String ss = "[RR 3G Ul2EmPsDataRateStatusIndStruct]\n";
        ss += oneElement2Byte("rx_mac_data_rate:", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        ss += oneElement2Byte("rx_pdcp_data_rate:", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        ss += oneElement2Byte("tx_mac_data_rate:", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        ss += oneElement2Byte("tx_pdcp_data_rate:", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;

        return ss;
    }

    /**
     * @return the 3G ul2EmHsdschReconfigStatus information
     */
    public String get3Gul2EmHsdschReconfigStatusIndStruct() {
        int start = calc2GSize() + Content.XGCSCE_NEIGH_CELL_STATUS_SIZE + Content.HANDOVER_SEQUENCE_SIZE
                + Content.ADM_POOL_STATUS_SIZE + Content.UL2_PSDATA_RATE_STATUS_SIZE;

        int end = start + Content.UL_HSDSCH_RECONFIG_STATUS_SIZE;
        String data = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------Get3Gul2EmHsdschReconfigStatusIndStruct is:\n");
        Xlog.v(TAG, data + "\n");
        Xlog.v(TAG, "NetworkInfo Get3Gul2EmHsdschReconfigStatusIndStruct");

        int index = 0;
        String ss = "[RR 3G Ul2EmHsdschReconfigStatusIndStruct]\n";
        for (int i = 0; i < DATA_OFFSET_EIGHT_BIT; i++) {
            ss += oneElementByte("reconfig_info" + i + ":", data, index, false);
            index += DATA_OFFSET_TWO_BIT;
        }
        return ss;
    }

    /**
     * @return the 3G Ul2EmUrlcEventStatus information
     */
    public String get3GUl2EmUrlcEventStatusIndStruct() {
        int start = calc2GSize() + Content.XGCSCE_NEIGH_CELL_STATUS_SIZE + Content.HANDOVER_SEQUENCE_SIZE
                + Content.ADM_POOL_STATUS_SIZE + Content.UL2_PSDATA_RATE_STATUS_SIZE
                + Content.UL_HSDSCH_RECONFIG_STATUS_SIZE;

        int end = start + Content.URLC_EVENT_STATUS_SIZE;
        String data = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------Get3GUl2EmUrlcEventStatusIndStruct is:\n");
        Xlog.v(TAG, data + "\n");
        Xlog.v(TAG, "NetworkInfo ------Get3GUl2EmUrlcEventStatusIndStruct---------------------");

        int index = 0;
        String ss = "[RR 3G Ul2EmUrlcEventStatusIndStruct]\n";
        ss += oneElementByte("rb_id:", data, index, false);
        index += DATA_OFFSET_TWO_BIT;
        ss += oneElementByte("rlc_action:", data, index, false);
        index += DATA_OFFSET_TWO_BIT;

        ss += "rb_info:--- \n";
        ss += oneElementByte("is_srb:", data, index, false);
        index += DATA_OFFSET_TWO_BIT;
        ss += oneElementByte("cn_domain:", data, index, false);
        index += DATA_OFFSET_TWO_BIT;

        ss += "rlc_info:--- \n";
        ss += oneElementByte("rlc_mode:", data, index, false);
        index += DATA_OFFSET_TWO_BIT;
        ss += oneElementByte("direction:", data, index, false);
        index += DATA_OFFSET_TWO_BIT;

        ss += "rlc_parameter:--- \n";
        ss += oneElement2Byte("pdu_Size:", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        ss += oneElement2Byte("tx_window_size:", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        ss += oneElement2Byte("rx_window_size:", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;
        ss += oneElementByte("discard_mode:", data, index, false);
        index += DATA_OFFSET_TWO_BIT;
        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_TWO_BIT;// alignment 1 byte
        }
        ss += oneElement2Byte("discard_value:", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;

        ss += oneElementByte("flush_data_indicator:", data, index, false);
        index += DATA_OFFSET_TWO_BIT;
        ss += oneElementByte("reset_cause:", data, index, false);
        index += DATA_OFFSET_TWO_BIT;

        return ss;

    }

    /**
     * @return the 3G Ul2EmPeriodicBlerReport information
     */
    public String get3GUl2EmPeriodicBlerReportInd() {
        int start = calc2GSize() + Content.XGCSCE_NEIGH_CELL_STATUS_SIZE + Content.HANDOVER_SEQUENCE_SIZE
                + Content.ADM_POOL_STATUS_SIZE + Content.UL2_PSDATA_RATE_STATUS_SIZE
                + Content.UL_HSDSCH_RECONFIG_STATUS_SIZE + Content.URLC_EVENT_STATUS_SIZE;

        int end = start + Content.UL_PERIOD_IC_BLER_REPORT_SIZE;
        String data = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------Get3GUl2EmPeriodicBlerReportInd is:\n");
        Xlog.v(TAG, data + "\n");
        Xlog.v(TAG, "NetworkInfo ------Get3GUl2EmPeriodicBlerReportInd---------------------");

        int index = 0;
        String ss = "[RR 3G Ul2EmPeriodicBlerReportInd]\n";

        ss += oneElementByte("num_trch:", data, index, false);
        index += DATA_OFFSET_TWO_BIT;

        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_SIX_BIT;// alignment 3 byte
        }

        ss += "TrCHBler:--------";

        for (int i = 0; i < DATA_OFFSET_EIGHT_BIT; i++) {
            ss += oneElementByte("TrCHId" + i + ":", data, index, false);
            index += DATA_OFFSET_TWO_BIT;
            if (ALIGN_MENT_ENABLE) {
                index += DATA_OFFSET_SIX_BIT;// alignment 3 byte
            }
            ss += oneElement4Byte("TotalCRC" + i + ":", data, index, false);
            index += DATA_OFFSET_EIGHT_BIT;
            ss += oneElement4Byte("BadCRC" + i + ":", data, index, false);
            index += DATA_OFFSET_EIGHT_BIT;
        }
        return ss;
    }

    /**
     * @return the 3G CsceEMNeighCellSStatus information
     */
    public String getxGCsceEMNeighCellSStatusIndStructSize() {
        int start = calc2GSize();

        int end = start + Content.XGCSCE_NEIGH_CELL_STATUS_SIZE;

        Xlog.v(TAG, "Read start offset " + start);
        Xlog.v(TAG, "Read end offset " + end);

        String data = mDataString.substring(start, end);

        Xlog.v(TAG, "NetworkInfo ------GetxGCsceEMNeighCellSStatusIndStructSize is:\n");
        Xlog.v(TAG, data + "\n");
        Xlog.v(TAG, "NetworkInfo  GetxGCsceEMNeighCellSStatusIndStructSize");

        int index = 0;
        String ss = "[RR xG CsceEMNeighCellSStatusIndStructSize]\n";

        ss += oneElementByte("ref_count:", data, index, false);
        index += DATA_OFFSET_TWO_BIT;

        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_TWO_BIT;// alignment 1 byte
        }

        ss += oneElement2Byte("msg_len", data, index, false);
        index += DATA_OFFSET_FOUR_BIT;

        ss += oneElementByte("neigh_cell_count:", data, index, false);
        index += DATA_OFFSET_TWO_BIT;

        String xgType = oneElementByte("RAT_type:", data, index, false);
        index += DATA_OFFSET_TWO_BIT;

        ss += xgType;

        if (ALIGN_MENT_ENABLE) {
            index += DATA_OFFSET_FOUR_BIT;
        }
        if (xgType.equalsIgnoreCase("1")) {
            ss += "----GSM_neigh_cells----";
            for (int i = 0; i < DATA_OFFSET_SIXTEEN_BIT; i++) {
                ss += oneElementByte("cellidx" + i + ":", data, index, false);
                index += DATA_OFFSET_TWO_BIT;
                if (ALIGN_MENT_ENABLE) {
                    index += DATA_OFFSET_TWO_BIT;// alignment 1 byte
                }
                ss += oneElement2Byte("arfcn", data, index, false);
                index += DATA_OFFSET_FOUR_BIT;
                ss += oneElementByte("bsic" + i + ":", data, index, false);
                index += DATA_OFFSET_TWO_BIT;
                ss += oneElementByte("is_bsic_verified" + i + ":", data, index, false);
                index += DATA_OFFSET_TWO_BIT;
                ss += oneElementByte("is_s_criteria_saticified" + i + ":", data, index, false);
                index += DATA_OFFSET_TWO_BIT;
                ss += oneElementByte("freq_band" + i + ":", data, index, false);
                index += DATA_OFFSET_TWO_BIT;
                ss += oneElementByte("qRxlevmin" + i + ":", data, index, true);
                index += 2;
                if (ALIGN_MENT_ENABLE) {
                    index += DATA_OFFSET_SIX_BIT;// alignment 3 byte
                }

                ss += oneElement4Byte("srxlev" + i + ":", data, index, true);
                index += DATA_OFFSET_EIGHT_BIT;
                ss += oneElement4Byte("rssi" + i + ":", data, index, true);
                index += DATA_OFFSET_EIGHT_BIT;
            }
        } else {
            ss += "----3G_neigh_cells----";
            for (int i = 0; i < DATA_OFFSET_SIXTEEN_BIT; i++) {
                ss += oneElementByte("cellidx" + i + ":", data, index, false);
                index += DATA_OFFSET_TWO_BIT;
                if (ALIGN_MENT_ENABLE) {
                    index += DATA_OFFSET_TWO_BIT;// alignment 1 byte
                }
                ss += oneElement2Byte("arfcn_DL", data, index, false);
                index += DATA_OFFSET_FOUR_BIT;
                ss += oneElement2Byte("psc", data, index, false);
                index += DATA_OFFSET_FOUR_BIT;
                ss += oneElementByte("is_s_criteria_saticified" + i + ":", data, index, false);
                index += DATA_OFFSET_TWO_BIT;
                ss += oneElementByte("qQualmin" + i + ":", data, index, true);
                index += DATA_OFFSET_TWO_BIT;
                ss += oneElementByte("qRxlevmin" + i + ":", data, index, true);
                index += DATA_OFFSET_TWO_BIT;

                if (ALIGN_MENT_ENABLE) {
                    index += DATA_OFFSET_SIX_BIT;// alignment 3 byte
                }

                ss += oneElement4Byte("srxlev" + i + ":", data, index, true);
                index += DATA_OFFSET_EIGHT_BIT;
                ss += oneElement4Byte("squal" + i + ":", data, index, true);
                index += DATA_OFFSET_EIGHT_BIT;
                String strRscp = getValueFrom4Byte(data, index, true);
                index += DATA_OFFSET_EIGHT_BIT;

                long rscp = 0;
                try {
                    rscp = Long.valueOf(strRscp) / DATA_OFFSET_4096BIT;
                } catch (NumberFormatException e) {
                    Xlog.v(TAG, "rscp = Long.valueOf(strRscp)/4096 exp.");
                }
                ss += "rscp: " + rscp + "\n";

                ss += oneElement4Byte("ec_no" + i + ":", data, index, true);
                index += DATA_OFFSET_EIGHT_BIT;
            }
        }

        return ss;
    }
}
