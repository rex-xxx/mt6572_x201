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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.engineermode.R;
import com.mediatek.xlog.Xlog;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class NetworkInfoInfomation extends Activity implements OnClickListener {

    private static final String TAG = "NetworkInfo";
    private static final String NW_INFO_FILE_NAME = "NetworkInfo.urc";
    private static final int FILE_NOT_FOUND = 100;
    private static final int IO_EXCEPTION_ID = 101;
    private static final int EVENT_NW_INFO = 1;
    private static final int EVENT_NW_INFO_AT = 2;
    private static final int EVENT_NW_INFO_INFO_AT = 3;
    private static final int EVENT_NW_INFO_CLOSE_AT = 4;
    private static final int EVENT_PAGEUP_INFO = 0;
    private static final int EVENT_PAGEDOWN_INFO = 1;
    private static final int EVENT_MENU_INFO = 2;
    private static final int BUF_SIZE_2G = 2204;
    private static final int BUF_SIZE_3G_FDD = 2192;
    private static final int BUF_SIZE_3G_TDD = 364;
    private static final int BUF_SIZE_XG_IDX48 = 456 * 2;
    private static final int TOTAL_TIMER = 1000;
    private static final int FLAG_OR_DATA = 0xF7;
    private static final int FLAG_OFFSET_BIT = 0x08;
    private static final int FLAG_DATA_BIT = 8;
    private static int sTotalBufSize = 0;

    private Button mPageUp;
    private Button mPageDown;
    private TextView mInfo;
    private int mItemCount = 0;
    private int mCurrentItem = 0;
    private int mItem[];
    private int mSimType;
    private NetworkInfoUrcParser mNetworkUrcParse = null;
    private Phone mPhone = null;
    private GeminiPhone mGeminiPhone = null;
    private Timer mTimer = null;

    // should be offset.,.
    private int mCellSelSize = 0;
    private int mChDscrSize = 0;
    private int mCtrlchanSize = 0;
    private int mRACHCtrlSize = 0;
    private int mLAIInfoSize = 0;
    private int mRadioLinkSize = 0;
    private int mMeasRepSize = 0;
    private int mCaListSize = 0;
    private int mControlMsgSize = 0;
    private int mSI2QInfoSize = 0;
    private int mMIInfoSize = 0;
    private int mBLKInfoSize = 0;
    private int mTBFInfoSize = 0;
    private int mGPRSGenSize = 0;

    // LXO, continue these stupid code.
    // FDD TDD code
    private int mMmEmInfoSize3G = 0;
    private int mTcmMmiEmInfoSize3G = 0;
    private int mCsceEMServCellsStatusIndSize3G = 0;
    private int mCsceEmInfoMultiPlmnSize3G = 0;
    private int mMemeEmInfoUmtsCellStatusSize3G = 0;
    private int mMemeEmPeriodicBlerReportIndSize3G = 0;
    private int mUrrUmtsSrncIdSize3G = 0;
    private int mSlceEmPsDataRateStatusIndSize3G = 0;
    private int mMemeEmInfoHServCellIndSize3G = 0;

    // TDD code
    private int mGHandoverSequenceIndStuctSize3G = 0; // alignment enabled
    private int mUl2EmAdmPoolStatusIndStructSize3G = 0;
    private int mUl2EmPsDataRateStatusIndStructSize3G = 0;
    private int mUl2EmHsdschReconfigStatusIndStructSize3G = 0;
    private int mUl2EmUrlcEventStatusIndStructSize3G = 0;
    private int mUl2EmPeriodicBlerReportIndSize3G = 0;

    private int mCsceEMNeighCellSStatusIndStructSizexG = 0;
    private int mFlag = 0;
    private Handler mATCmdHander = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
            case EVENT_NW_INFO_AT:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    String data[] = (String[]) ar.result;
                    Xlog.v(TAG, "data[0] is : " + data[0]);
                    Xlog.v(TAG, "flag is : " + data[0].substring(FLAG_DATA_BIT));
                    mFlag = Integer.valueOf(data[0].substring(FLAG_DATA_BIT));
                    mFlag = mFlag | FLAG_OFFSET_BIT;
                    Xlog.v(TAG, "flag change is : " + mFlag);
                    for (int j = 0; j < mItemCount; j++) {
                        String[] atCommand = new String[2];
                        atCommand[0] = "AT+EINFO=" + mFlag + "," + mItem[j] + ",0";
                        atCommand[1] = "+EINFO";
                        sendATCommand(atCommand, EVENT_NW_INFO_INFO_AT);
                    }

                } else {
                    Toast.makeText(NetworkInfoInfomation.this, getString(R.string.send_at_fail), Toast.LENGTH_LONG);
                }
                break;
            case EVENT_NW_INFO_INFO_AT:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    Toast.makeText(NetworkInfoInfomation.this, getString(R.string.send_at_fail), Toast.LENGTH_LONG);
                }
                break;
            case EVENT_NW_INFO_CLOSE_AT:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    Toast.makeText(NetworkInfoInfomation.this, getString(R.string.send_at_fail), Toast.LENGTH_LONG);
                }
                break;
            default:
                break;
            }
        }
    };

    // simple and naive, get max buffer.
    /**
     * @return size the max buffer
     */
    public int calcBufferSize() {
        int size = 0;
        size = BUF_SIZE_2G + BUF_SIZE_3G_FDD + BUF_SIZE_3G_TDD + BUF_SIZE_XG_IDX48;
        return size;
    }

    /**
     * calcOffset is to calculate the items offset bit
     */
    private void calcOffset() {
        mCellSelSize += Content.CELL_SEL_SIZE;
        mChDscrSize += Content.CH_DSCR_SIZE + mCellSelSize;
        mCtrlchanSize += Content.CTRL_CHAN_SIZE + mChDscrSize;
        mRACHCtrlSize += Content.RACH_CTRL_SIZE + mCtrlchanSize;
        mLAIInfoSize += Content.LAI_INFO_SIZE + mRACHCtrlSize;
        mRadioLinkSize += Content.RADIO_LINK_SIZE + mLAIInfoSize;
        mMeasRepSize += Content.MEAS_REP_SIZE + mRadioLinkSize;
        mCaListSize += Content.CAL_LIST_SIZE + mMeasRepSize;
        mControlMsgSize += Content.CONTROL_MSG_SIZE + mCaListSize;
        mSI2QInfoSize += Content.SI2Q_INFO_SIZE + mControlMsgSize;
        mMIInfoSize += Content.MI_INFO_SIZE + mSI2QInfoSize;
        mBLKInfoSize += Content.BLK_INFO_SIZE + mMIInfoSize;
        mTBFInfoSize += Content.TBF_INFO_SIZE + mBLKInfoSize;
        mGPRSGenSize += Content.GPRS_GEN_SIZE + mTBFInfoSize;

        // union
        mCsceEMNeighCellSStatusIndStructSizexG += Content.XGCSCE_NEIGH_CELL_STATUS_SIZE + mGPRSGenSize;

        // LXO, continue these stupid code.
        // FDD TDD code
        mMmEmInfoSize3G += Content.M3G_MM_EMINFO_SIZE + mCsceEMNeighCellSStatusIndStructSizexG;
        mTcmMmiEmInfoSize3G += Content.M_3G_TCMMMI_INFO_SIZE + mMmEmInfoSize3G;
        mCsceEMServCellsStatusIndSize3G += Content.CSCE_SERV_CELL_STATUS_SIZE + mTcmMmiEmInfoSize3G;
        mCsceEmInfoMultiPlmnSize3G += Content.CSCE_MULTI_PLMN_SIZE + mCsceEMServCellsStatusIndSize3G;
        mMemeEmInfoUmtsCellStatusSize3G += Content.UMTS_CELL_STATUS_SIZE + mCsceEmInfoMultiPlmnSize3G;
        mMemeEmPeriodicBlerReportIndSize3G += Content.PERIOD_IC_BLER_REPORT_SIZE + mMemeEmInfoUmtsCellStatusSize3G;
        mUrrUmtsSrncIdSize3G += Content.URR_UMTS_SRNC_SIZE + mMemeEmPeriodicBlerReportIndSize3G;
        mSlceEmPsDataRateStatusIndSize3G += Content.SLCE_PS_DATA_RATE_STATUS_SIZE + mUrrUmtsSrncIdSize3G;
        mMemeEmInfoHServCellIndSize3G += Content.MEME_HSERV_CELL_SIZE + mSlceEmPsDataRateStatusIndSize3G;

        // TDD code
        mGHandoverSequenceIndStuctSize3G += Content.HANDOVER_SEQUENCE_SIZE 
                                            + mCsceEMNeighCellSStatusIndStructSizexG; // alignment
        // enabled
        mUl2EmAdmPoolStatusIndStructSize3G += Content.ADM_POOL_STATUS_SIZE + mGHandoverSequenceIndStuctSize3G;
        mUl2EmPsDataRateStatusIndStructSize3G += Content.UL2_PSDATA_RATE_STATUS_SIZE + mUl2EmAdmPoolStatusIndStructSize3G;
        mUl2EmHsdschReconfigStatusIndStructSize3G += Content.UL_HSDSCH_RECONFIG_STATUS_SIZE
                + mUl2EmPsDataRateStatusIndStructSize3G;
        mUl2EmUrlcEventStatusIndStructSize3G += Content.URLC_EVENT_STATUS_SIZE + mUl2EmHsdschReconfigStatusIndStructSize3G;
        mUl2EmPeriodicBlerReportIndSize3G += Content.UL_PERIOD_IC_BLER_REPORT_SIZE + mUl2EmUrlcEventStatusIndStructSize3G;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.networkinfo_info);

        sTotalBufSize = calcBufferSize();
        byte initial[] = new byte[sTotalBufSize];
        for (int r = 0; r < sTotalBufSize; r++) {
            initial[r] = '0';
        }
        try {
            // due to use of file data storage, must follow the following way
            FileOutputStream outputStream = this.openFileOutput(NW_INFO_FILE_NAME, MODE_PRIVATE);
            outputStream.write(initial);
            outputStream.close();
        } catch (FileNotFoundException e) {
            showDialog(FILE_NOT_FOUND);
            return;
        } catch (IOException e) {
            removeDialog(FILE_NOT_FOUND);
            showDialog(IO_EXCEPTION_ID);
            return;
        }

        int modemType = NetworkInfo.getModemType();
        if (modemType == NetworkInfo.MODEM_TD) {
            mCsceEMServCellsStatusIndSize3G -= Content.MI_INFO_SIZE;
        }
        calcOffset();
        Xlog.v(TAG, "The total data size is : " + mGPRSGenSize);

        // get the selected item and store its ID into the mItem array
        mItem = new int[NetworkInfo.TOTAL_ITEM_NUM];
        Intent intent = getIntent();
        int[] checked = intent.getIntArrayExtra("mChecked");
        mSimType = intent.getIntExtra("mSimType", Content.DEFAULT_PHONE);
        if (null != checked) {
            for (int i = 0; i < checked.length; i++) {
                if (1 == checked[i]) {
                    mItem[mItemCount] = i;
                    mItemCount++;
                }
            }
        }
        registerNetwork();
        mInfo = (TextView) findViewById(R.id.NetworkInfo_Info);
        mPageUp = (Button) findViewById(R.id.NetworkInfo_PageUp);
        mPageDown = (Button) findViewById(R.id.NetworkInfo_PageDown);

        mPageUp.setOnClickListener(this);
        mPageDown.setOnClickListener(this);

        // initial info display
        mNetworkUrcParse = new NetworkInfoUrcParser(this);
        mInfo.setText("<" + (mCurrentItem + 1) + "/" + mItemCount + ">\n" + mNetworkUrcParse.getInfo(mItem[mCurrentItem]));

    }

    private void registerNetwork() {
        String[] atCommand = { "AT+EINFO?", "+EINFO" };
        if (mSimType == Content.DEFAULT_PHONE) {
            mPhone = PhoneFactory.getDefaultPhone();
            mPhone.registerForNetworkInfo(mResponseHander, EVENT_NW_INFO, null);
            sendATCommand(atCommand, EVENT_NW_INFO_AT);
        } else {
            mGeminiPhone = (GeminiPhone) PhoneFactory.getDefaultPhone();
            if (mSimType == Content.GEMINI_SIM_1) {
                mGeminiPhone.registerForNetworkInfoGemini(mResponseHander, EVENT_NW_INFO, null, PhoneConstants.GEMINI_SIM_1);
                sendATCommand(atCommand, EVENT_NW_INFO_AT);
            } else {
                mGeminiPhone.registerForNetworkInfoGemini(mResponseHander, EVENT_NW_INFO, null, PhoneConstants.GEMINI_SIM_2);
                sendATCommand(atCommand, EVENT_NW_INFO_AT);
            }
        }
    }

    private void sendATCommand(String[] atCommand, int msg) {
        if (mSimType == Content.GEMINI_SIM_1) {
            mGeminiPhone.invokeOemRilRequestStringsGemini(atCommand,
                    mATCmdHander.obtainMessage(msg),
                    PhoneConstants.GEMINI_SIM_1);
        } else if (mSimType == Content.GEMINI_SIM_2) {
            mGeminiPhone.invokeOemRilRequestStringsGemini(atCommand,
                    mATCmdHander.obtainMessage(msg),
                    PhoneConstants.GEMINI_SIM_2);
        } else {
            mPhone.invokeOemRilRequestStrings(atCommand, mATCmdHander
                    .obtainMessage(msg));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI(EVENT_MENU_INFO);
    }

    @Override
    public void onStop() {
        if (mTimer != null) {
            mTimer.cancel();
        }
        mPhone.unregisterForNetworkInfo(mResponseHander);
        mFlag = mFlag & FLAG_OR_DATA;
        Xlog.v(TAG, "The close flag is :" + mFlag);
        String[] atCloseCmd = new String[2];
        atCloseCmd[0] = "AT+EINFO=" + mFlag;
        atCloseCmd[1] = "";
        sendATCommand(atCloseCmd, EVENT_NW_INFO_CLOSE_AT);
        super.onStop();
    }

    /*
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    public void onClick(View arg0) {

        if (arg0.getId() == mPageUp.getId()) {
            mCurrentItem = (mCurrentItem - 1 + mItemCount) % mItemCount;
            updateUI(EVENT_PAGEUP_INFO);
        }
        if (arg0.getId() == mPageDown.getId()) {
            mCurrentItem = (mCurrentItem + 1) % mItemCount;
            updateUI(EVENT_PAGEDOWN_INFO);
        }
    }

    /**
     * @param messageWhat
     *            is pageup or pagedown
     */
    public void updateUI(int messageWhat) {
        final int wihichMsg = messageWhat;

        if (mTimer != null) {
            mTimer.cancel();
        }
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                Message msg = new Message();
                msg.what = wihichMsg;
                mUpUiHandler.sendMessage(msg);

            }
        }, TOTAL_TIMER, TOTAL_TIMER);

    }

    private final Handler mResponseHander = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
            case EVENT_NW_INFO:
                ar = (AsyncResult) msg.obj;
                String data[] = (String[]) ar.result;

                byte[] fileData = new byte[sTotalBufSize];
                FileInputStream inputStream = null;
                try {
                    // due to use of file data storage, must follow the
                    // following way
                    inputStream = NetworkInfoInfomation.this
                            .openFileInput(NW_INFO_FILE_NAME);
                    int mTotalDataSize = inputStream.read(fileData, 0,
                            sTotalBufSize);
                } catch (FileNotFoundException e) {
                    showDialog(FILE_NOT_FOUND);
                    return;
                } catch (IOException e) {
                    showDialog(IO_EXCEPTION_ID);
                    return;
                } finally {
                    if (null != inputStream) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            Xlog.w(TAG, "inputStream.close: " + e.getMessage());
                        }
                    }
                }
                String fileString = new String(fileData);
                Xlog.v(TAG, "Ret Type: " + data[0]);
                Xlog.v(TAG, "Ret Data: " + data[1]);
                switch (Integer.valueOf(data[0])) {
                case Content.CELL_INDEX:
                    fileString = data[1] + fileString.substring(mCellSelSize, sTotalBufSize);
                    break;
                case Content.CHANNEL_INDEX:
                    fileString = fileString.substring(0, mCellSelSize) + data[1]
                            + fileString.substring(mChDscrSize, sTotalBufSize);
                    break;
                case Content.CTRL_INDEX:
                    fileString = fileString.substring(0, mChDscrSize) + data[1]
                            + fileString.substring(mCtrlchanSize, sTotalBufSize);
                    break;
                case Content.RACH_INDEX:
                    fileString = fileString.substring(0, mCtrlchanSize) + data[1]
                            + fileString.substring(mRACHCtrlSize, sTotalBufSize);
                    break;
                case Content.LAI_INDEX:
                    fileString = fileString.substring(0, mRACHCtrlSize) + data[1]
                            + fileString.substring(mLAIInfoSize, sTotalBufSize);
                    break;
                case Content.RADIO_INDEX:
                    fileString = fileString.substring(0, mLAIInfoSize) + data[1]
                            + fileString.substring(mRadioLinkSize, sTotalBufSize);
                    break;
                case Content.MEAS_INDEX:
                    fileString = fileString.substring(0, mRadioLinkSize) + data[1]
                            + fileString.substring(mMeasRepSize, sTotalBufSize);
                    break;
                case Content.CA_INDEX:
                    fileString = fileString.substring(0, mMeasRepSize) + data[1]
                            + fileString.substring(mCaListSize, sTotalBufSize);
                    break;
                case Content.CONTROL_INDEX:
                    fileString = fileString.substring(0, mCaListSize) + data[1]
                            + fileString.substring(mControlMsgSize, sTotalBufSize);
                    break;
                case Content.SI2Q_INDEX:
                    fileString = fileString.substring(0, mControlMsgSize) + data[1]
                            + fileString.substring(mSI2QInfoSize, sTotalBufSize);
                    break;
                case Content.MI_INDEX:
                    fileString = fileString.substring(0, mSI2QInfoSize) + data[1]
                            + fileString.substring(mMIInfoSize, sTotalBufSize);
                    break;
                case Content.BLK_INDEX:
                    fileString = fileString.substring(0, mMIInfoSize) + data[1]
                            + fileString.substring(mBLKInfoSize, sTotalBufSize);
                    break;
                case Content.TBF_INDEX:
                    fileString = fileString.substring(0, mBLKInfoSize) + data[1]
                            + fileString.substring(mTBFInfoSize, sTotalBufSize);
                    break;
                case Content.GPRS_INDEX:
                    fileString = fileString.substring(0, mTBFInfoSize) + data[1]
                            + fileString.substring(mGPRSGenSize, sTotalBufSize);
                    break;
                case Content.MM_INFO_INDEX:
                    fileString = fileString.substring(0, mCsceEMNeighCellSStatusIndStructSizexG) + data[1]
                            + fileString.substring(mMmEmInfoSize3G, sTotalBufSize);
                    break;
                case Content.TCM_MMI_INDEX:
                    fileString = fileString.substring(0, mMmEmInfoSize3G) + data[1]
                            + fileString.substring(mTcmMmiEmInfoSize3G, sTotalBufSize);
                    break;
                case Content.CSCE_SERV_CELL_STATUS_INDEX:
                    Xlog.v(TAG, "data[1].length()=" + data[1].length());
                    Xlog.v(TAG, "start offset " + mTcmMmiEmInfoSize3G);
                    Xlog.v(TAG, "end offset " + mCsceEMServCellsStatusIndSize3G);
                    fileString = fileString.substring(0, mTcmMmiEmInfoSize3G) + data[1]
                            + fileString.substring(mCsceEMServCellsStatusIndSize3G, sTotalBufSize);
                    break;
                case Content.CSCE_MULTIPLMN_INDEX:
                    fileString = fileString.substring(0, mCsceEMServCellsStatusIndSize3G) + data[1]
                            + fileString.substring(mCsceEmInfoMultiPlmnSize3G, sTotalBufSize);
                    break;
                case Content.PERIOD_IC_BLER_REPORT_INDEX:
                    fileString = fileString.substring(0, mMemeEmInfoUmtsCellStatusSize3G) + data[1]
                            + fileString.substring(mMemeEmPeriodicBlerReportIndSize3G, sTotalBufSize);
                    break;
                case Content.URR_UMTS_SRNC_INDEX:
                    fileString = fileString.substring(0, mMemeEmPeriodicBlerReportIndSize3G) + data[1]
                            + fileString.substring(mUrrUmtsSrncIdSize3G, sTotalBufSize);
                    break;

                case Content.CSCE_NEIGH_CELL_STATUS_INDEX:
                    Xlog.v(TAG, "data[1].length()=" + data[1].length());
                    Xlog.v(TAG, "2G size should be " + BUF_SIZE_XG_IDX48);
                    Xlog.v(TAG, "start offset " + mGPRSGenSize);
                    Xlog.v(TAG, "end offset " + mCsceEMNeighCellSStatusIndStructSizexG);
                    // 2G size > 3G size
                    if (data[1].length() >= BUF_SIZE_XG_IDX48) {
                        fileString = fileString.substring(0, mGPRSGenSize) + data[1]
                                + fileString.substring(mCsceEMNeighCellSStatusIndStructSizexG, sTotalBufSize);
                    } else {
                        char[] padding = new char[BUF_SIZE_XG_IDX48 - data[1].length()];

                        fileString = fileString.substring(0, mGPRSGenSize) + data[1] + new String(padding)
                                + fileString.substring(mCsceEMNeighCellSStatusIndStructSizexG, sTotalBufSize);
                    }

                    break;
                default:
                    break;
                }

                int modemType = NetworkInfo.getModemType();
                if (modemType == NetworkInfo.MODEM_FDD) {
                    switch (Integer.valueOf(data[0])) {

                    case Content.UMTS_CELL_STATUS_INDEX:
                        fileString = fileString.substring(0, mCsceEmInfoMultiPlmnSize3G) + data[1]
                                + fileString.substring(mMemeEmInfoUmtsCellStatusSize3G, sTotalBufSize);
                        break;

                    case Content.PSDATA_RATE_STATUS_INDEX:
                        fileString = fileString.substring(0, mUrrUmtsSrncIdSize3G) + data[1]
                                + fileString.substring(mSlceEmPsDataRateStatusIndSize3G, sTotalBufSize);
                        break;
                    default:
                        break;
                    }
                } else if (modemType == NetworkInfo.MODEM_TD) {
                    switch (Integer.valueOf(data[0])) {
                    case Content.HANDOVER_SEQUENCE_INDEX:
                        fileString = fileString.substring(0, mGPRSGenSize) + data[1]
                                + fileString.substring(mGHandoverSequenceIndStuctSize3G, sTotalBufSize);
                        break;
                    case Content.UL_ADM_POOL_STATUS_INDEX:
                        fileString = fileString.substring(0, mGHandoverSequenceIndStuctSize3G) + data[1]
                                + fileString.substring(mUl2EmAdmPoolStatusIndStructSize3G, sTotalBufSize);
                        break;
                    case Content.UL_PSDATA_RATE_STATUS_INDEX:
                        fileString = fileString.substring(0, mUl2EmAdmPoolStatusIndStructSize3G) + data[1]
                                + fileString.substring(mUl2EmPsDataRateStatusIndStructSize3G, sTotalBufSize);
                        break;

                    case Content.UL_HSDSCH_RECONFIG_STATUS_INDEX:
                        fileString = fileString.substring(0, mUl2EmPsDataRateStatusIndStructSize3G) + data[1]
                                + fileString.substring(mUl2EmHsdschReconfigStatusIndStructSize3G, sTotalBufSize);
                        break;
                    case Content.UL_URLC_EVENT_STATUS_INDEX:
                        fileString = fileString.substring(0, mUl2EmHsdschReconfigStatusIndStructSize3G) + data[1]
                                + fileString.substring(mUl2EmUrlcEventStatusIndStructSize3G, sTotalBufSize);
                        break;
                    case Content.UL_PERIOD_IC_BLER_REPORT_INDEX:
                        fileString = fileString.substring(0, mUl2EmUrlcEventStatusIndStructSize3G) + data[1];
                        break;
                    default:
                        break;
                    }
                }

                try {
                    // due to use of file data storage, must follow the
                    // following way
                    FileOutputStream outputStream = NetworkInfoInfomation.this.openFileOutput(NW_INFO_FILE_NAME,
                            MODE_PRIVATE);
                    byte[] modifyData = fileString.getBytes();
                    outputStream.write(modifyData);
                    outputStream.close();
                } catch (FileNotFoundException e) {
                    showDialog(FILE_NOT_FOUND);
                    return;
                } catch (IOException e) {
                    showDialog(IO_EXCEPTION_ID);
                    return;
                }

                // if the data update is the current page, then update the
                // display content at once
                if (mCurrentItem == Integer.valueOf(data[0])) {
                    mNetworkUrcParse = new NetworkInfoUrcParser(NetworkInfoInfomation.this);
                    mInfo.setText("<" + (mCurrentItem + 1) + "/" + mItemCount + ">\n"
                            + mNetworkUrcParse.getInfo(mItem[mCurrentItem]));
                }
                break;
            default:
                break;
            }
        }
    };
    private final Handler mUpUiHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case EVENT_PAGEUP_INFO:
                mNetworkUrcParse = new NetworkInfoUrcParser(NetworkInfoInfomation.this);
                mInfo.setText("<" + (mCurrentItem + 1) + "/" + mItemCount + ">\n"
                        + mNetworkUrcParse.getInfo(mItem[mCurrentItem]));
                break;
            case EVENT_PAGEDOWN_INFO:
                mNetworkUrcParse = new NetworkInfoUrcParser(NetworkInfoInfomation.this);
                mInfo.setText("<" + (mCurrentItem + 1) + "/" + mItemCount + ">\n"
                        + mNetworkUrcParse.getInfo(mItem[mCurrentItem]));
                break;
            case EVENT_MENU_INFO:
                mNetworkUrcParse = new NetworkInfoUrcParser(NetworkInfoInfomation.this);
                mInfo.setText("<" + (mCurrentItem + 1) + "/" + mItemCount + ">\n"
                        + mNetworkUrcParse.getInfo(mItem[mCurrentItem]));
                break;
            default:
                break;
            }
        }
    };

    @Override
    protected Dialog onCreateDialog(int id) {
        if (FILE_NOT_FOUND == id) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(NetworkInfoInfomation.this);
            builder.setTitle(R.string.open_file_title);
            builder.setMessage(R.string.open_file_fail);
            builder.setPositiveButton(R.string.network_btn_text, null);
            builder.create().show();
        } else if (IO_EXCEPTION_ID == id) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(NetworkInfoInfomation.this);
            builder.setTitle(R.string.io_exception_title);
            builder.setMessage(R.string.io_exception_fail);
            builder.setPositiveButton(R.string.network_btn_text, null);
            builder.create().show();
        }
        return super.onCreateDialog(id);
    }
}
