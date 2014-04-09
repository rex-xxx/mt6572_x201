package com.mediatek.engineermode.hqanfc;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.R;
import com.mediatek.engineermode.hqanfc.NfcCommand.BitMapValue;
import com.mediatek.engineermode.hqanfc.NfcCommand.CommandType;
import com.mediatek.engineermode.hqanfc.NfcCommand.EmAction;
import com.mediatek.engineermode.hqanfc.NfcCommand.ReaderModeRspResult;
import com.mediatek.engineermode.hqanfc.NfcCommand.RspResult;
import com.mediatek.engineermode.hqanfc.NfcEmReqRsp.NfcEmAlsReadermNtf;
import com.mediatek.engineermode.hqanfc.NfcEmReqRsp.NfcEmAlsReadermReq;
import com.mediatek.engineermode.hqanfc.NfcEmReqRsp.NfcEmAlsReadermRsp;

import java.nio.ByteBuffer;

public class ReaderMode extends Activity {

    protected static final String KEY_READER_MODE_RSP_ARRAY = "reader_mode_rsp_array";
    protected static final String KEY_READER_MODE_RSP_NDEF = "reader_mode_rsp_ndef";

    private static final int HANDLER_MSG_GET_RSP = 200;
    private static final int HANDLER_MSG_GET_NTF = 100;
    private static final int DIALOG_ID_WAIT = 0;

    private static final int CHECKBOXS_NUMBER = 18;
    private static final int CHECKBOX_READER_TYPEA = 0;
    private static final int CHECKBOX_READER_TYPEA_106 = 1;
    private static final int CHECKBOX_READER_TYPEA_212 = 2;
    private static final int CHECKBOX_READER_TYPEA_424 = 3;
    private static final int CHECKBOX_READER_TYPEA_848 = 4;
    private static final int CHECKBOX_READER_TYPEB = 5;
    private static final int CHECKBOX_READER_TYPEB_106 = 6;
    private static final int CHECKBOX_READER_TYPEB_212 = 7;
    private static final int CHECKBOX_READER_TYPEB_424 = 8;
    private static final int CHECKBOX_READER_TYPEB_848 = 9;
    private static final int CHECKBOX_READER_TYPEF = 10;
    private static final int CHECKBOX_READER_TYPEF_212 = 11;
    private static final int CHECKBOX_READER_TYPEF_424 = 12;
    private static final int CHECKBOX_READER_TYPEV = 13;
    private static final int CHECKBOX_READER_TYPEV_166 = 14;
    private static final int CHECKBOX_READER_TYPEV_2648 = 15;
    private static final int CHECKBOX_READER_TYPEB2 = 16;
    private static final int CHECKBOX_READER_KOVIO = 17;

    private CheckBox[] mSettingsCkBoxs = new CheckBox[CHECKBOXS_NUMBER];
    private RadioGroup mRgTypeVRadioGroup;
    private RadioButton mRbTypeVSubcarrier;
    private RadioButton mRbTypeVDualSubcarrier;
    private Button mBtnSelectAll;
    private Button mBtnClearAll;
    private Button mBtnStart;
    private Button mBtnReturn;
    private Button mBtnRunInBack;
    private NfcEmAlsReadermRsp mResponse;
    private NfcEmAlsReadermNtf mReadermNtf;
    private byte[] mRspArray;
    private boolean mEnableBackKey = true; // can or can not press back key

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Elog.v(NfcMainPage.TAG, "[ReaderMode]mReceiver onReceive: " + action);
            mRspArray = intent.getExtras().getByteArray(NfcCommand.MESSAGE_CONTENT_KEY);
            if ((NfcCommand.ACTION_PRE + CommandType.MTK_NFC_EM_ALS_READER_MODE_RSP).equals(action)) {
                if (null != mRspArray) {
                    ByteBuffer buffer = ByteBuffer.wrap(mRspArray);
                    mResponse = new NfcEmAlsReadermRsp();
                    mResponse.readRaw(buffer);
                    mHandler.sendEmptyMessage(HANDLER_MSG_GET_RSP);
                }
            } else if ((NfcCommand.ACTION_PRE + CommandType.MTK_NFC_EM_ALS_READER_MODE_NTF)
                    .equals(action)) {
                if (null != mRspArray) {
                    ByteBuffer buffer = ByteBuffer.wrap(mRspArray);
                    mReadermNtf = new NfcEmAlsReadermNtf();
                    mReadermNtf.readRaw(buffer);
                    mHandler.sendEmptyMessage(HANDLER_MSG_GET_NTF);
                }
            } else {
                Elog.v(NfcMainPage.TAG, "[ReaderMode]Other response");
            }
        }
    };

    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String toastMsg = null;
            if (HANDLER_MSG_GET_RSP == msg.what) {
                dismissDialog(DIALOG_ID_WAIT);
                switch (mResponse.mResult) {
                    case RspResult.SUCCESS:
                        toastMsg = "ReaderMode Rsp Result: SUCCESS";
                        if (mBtnStart.getText().equals(
                                ReaderMode.this.getString(R.string.hqa_nfc_start))) {
                            setButtonsStatus(false);
                        } else {
                            setButtonsStatus(true);
                        }
                        break;
                    case RspResult.FAIL:
                        toastMsg = "ReaderMode Rsp Result: FAIL";
                        break;
                    case RspResult.NFC_STATUS_INVALID_FORMAT:
                        toastMsg = "ReaderMode Rsp Result: INVALID_FORMAT";
                        break;
                    case RspResult.NFC_STATUS_INVALID_NDEF_FORMAT:
                        toastMsg = "ReaderMode Rsp Result: INVALID_NDEF_FORMAT";
                        break;
                    case RspResult.NFC_STATUS_NDEF_EOF_REACHED:
                        toastMsg = "ReaderMode Rsp Result: NDEF_EOF_REACHED";
                        break;
                    case RspResult.NFC_STATUS_NOT_SUPPORT:
                        toastMsg = "ReaderMode Rsp Result: NOT_SUPPORT";
                        break;
                    default:
                        toastMsg = "ReaderMode Rsp Result: ERROR";
                        break;
                }
            } else if (HANDLER_MSG_GET_NTF == msg.what) {
                switch (mReadermNtf.mResult) {
                    case ReaderModeRspResult.CONNECT:
                        toastMsg = "ReaderMode Ntf Result: CONNECT";
                        if (mReadermNtf.mIsNdef == 0 || mReadermNtf.mIsNdef == 1 || mReadermNtf.mIsNdef == 2) {
                            Intent intent = new Intent();
                            intent.putExtra(KEY_READER_MODE_RSP_ARRAY, mRspArray);
                            intent.putExtra(KEY_READER_MODE_RSP_NDEF, mReadermNtf.mIsNdef);
                            intent.setClass(ReaderMode.this, RwFunction.class);
                            startActivity(intent);
                        }
                        break;
                    case ReaderModeRspResult.DISCONNECT:
                        toastMsg = "ReaderMode Ntf Result: DISCONNECT";
                        break;
                    case ReaderModeRspResult.FAIL:
                        toastMsg = "ReaderMode Ntf Result: FAIL";
                        break;
                    default:
                        toastMsg = "ReaderMode Ntf Result: ERROR";
                        break;
                }
            }
            Toast.makeText(ReaderMode.this, toastMsg, Toast.LENGTH_SHORT).show();
        }
    };

    private final CheckBox.OnCheckedChangeListener mCheckedListener = new CheckBox.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
            Elog.v(NfcMainPage.TAG, "[ReaderMode]onCheckedChanged view is " + buttonView.getText()
                    + " value is " + checked);
            if (buttonView.equals(mSettingsCkBoxs[CHECKBOX_READER_TYPEA])) {
                for (int i = CHECKBOX_READER_TYPEA_106; i < CHECKBOX_READER_TYPEB; i++) {
                    mSettingsCkBoxs[i].setChecked(checked);
                }
            } else if (buttonView.equals(mSettingsCkBoxs[CHECKBOX_READER_TYPEB])) {
                for (int i = CHECKBOX_READER_TYPEB_106; i < CHECKBOX_READER_TYPEF; i++) {
                    mSettingsCkBoxs[i].setChecked(checked);
                }
            } else if (buttonView.equals(mSettingsCkBoxs[CHECKBOX_READER_TYPEF])) {
                for (int i = CHECKBOX_READER_TYPEF_212; i < CHECKBOX_READER_TYPEV; i++) {
                    mSettingsCkBoxs[i].setChecked(checked);
                }
            } else if (buttonView.equals(mSettingsCkBoxs[CHECKBOX_READER_TYPEV])) {
                for (int i = CHECKBOX_READER_TYPEV_166; i < CHECKBOX_READER_TYPEB2; i++) {
                    mSettingsCkBoxs[i].setChecked(checked);
                }
            }
        }
    };

    private final Button.OnClickListener mClickListener = new Button.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            Elog.v(NfcMainPage.TAG, "[ReaderMode]onClick button view is "
                    + ((Button) arg0).getText());
            if (arg0.equals(mBtnStart)) {
                showDialog(DIALOG_ID_WAIT);
                doTestAction(mBtnStart.getText().equals(
                        ReaderMode.this.getString(R.string.hqa_nfc_start)));
            } else if (arg0.equals(mBtnSelectAll)) {
                changeAllSelect(true);
            } else if (arg0.equals(mBtnClearAll)) {
                changeAllSelect(false);
            } else if (arg0.equals(mBtnReturn)) {
                ReaderMode.this.onBackPressed();
            } else if (arg0.equals(mBtnRunInBack)) {
                doTestAction(null);
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addCategory(Intent.CATEGORY_HOME);
                startActivity(intent);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hqa_nfc_reader_mode);
        initComponents();
        changeAllSelect(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(NfcCommand.ACTION_PRE + CommandType.MTK_NFC_EM_ALS_READER_MODE_RSP);
        filter.addAction(NfcCommand.ACTION_PRE + CommandType.MTK_NFC_EM_ALS_READER_MODE_NTF);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onStop() {
        unregisterReceiver(mReceiver);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!mEnableBackKey) {
            return;
        }
        super.onBackPressed();
    }

    private void initComponents() {
        Elog.v(NfcMainPage.TAG, "[ReaderMode]initComponents");
        mSettingsCkBoxs[CHECKBOX_READER_TYPEA] = (CheckBox) findViewById(R.id.hqa_readermode_cb_typea);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEA].setOnCheckedChangeListener(mCheckedListener);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEA_106] = (CheckBox) findViewById(R.id.hqa_readermode_cb_typea_106);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEA_212] = (CheckBox) findViewById(R.id.hqa_readermode_cb_typea_212);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEA_424] = (CheckBox) findViewById(R.id.hqa_readermode_cb_typea_424);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEA_848] = (CheckBox) findViewById(R.id.hqa_readermode_cb_typea_848);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEB] = (CheckBox) findViewById(R.id.hqa_readermode_cb_typeb);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEB].setOnCheckedChangeListener(mCheckedListener);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEB_106] = (CheckBox) findViewById(R.id.hqa_readermode_cb_typeb_106);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEB_212] = (CheckBox) findViewById(R.id.hqa_readermode_cb_typeb_212);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEB_424] = (CheckBox) findViewById(R.id.hqa_readermode_cb_typeb_424);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEB_848] = (CheckBox) findViewById(R.id.hqa_readermode_cb_typeb_848);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEF] = (CheckBox) findViewById(R.id.hqa_readermode_cb_typef);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEF].setOnCheckedChangeListener(mCheckedListener);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEF_212] = (CheckBox) findViewById(R.id.hqa_readermode_cb_typef_212);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEF_424] = (CheckBox) findViewById(R.id.hqa_readermode_cb_typef_424);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEV] = (CheckBox) findViewById(R.id.hqa_readermode_cb_typev);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEV].setOnCheckedChangeListener(mCheckedListener);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEV_166] = (CheckBox) findViewById(R.id.hqa_readermode_cb_typev_166);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEV_2648] = (CheckBox) findViewById(R.id.hqa_readermode_cb_typev_2648);
        mRgTypeVRadioGroup = (RadioGroup) findViewById(R.id.hqa_readermode_rg_typev);
        mRbTypeVSubcarrier = (RadioButton) findViewById(R.id.hqa_readermode_rb_typev_subcarrier);
        mRbTypeVDualSubcarrier = (RadioButton) findViewById(R.id.hqa_readermode_rb_typev_dual_subcarrier);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEB2] = (CheckBox) findViewById(R.id.hqa_readermode_cb_typeb2);
        mSettingsCkBoxs[CHECKBOX_READER_KOVIO] = (CheckBox) findViewById(R.id.hqa_readermode_cb_kovio);
        mBtnSelectAll = (Button) findViewById(R.id.hqa_readermode_btn_select_all);
        mBtnSelectAll.setOnClickListener(mClickListener);
        mBtnClearAll = (Button) findViewById(R.id.hqa_readermode_btn_clear_all);
        mBtnClearAll.setOnClickListener(mClickListener);
        mBtnStart = (Button) findViewById(R.id.hqa_readermode_btn_start_stop);
        mBtnStart.setOnClickListener(mClickListener);
        mBtnReturn = (Button) findViewById(R.id.hqa_readermode_btn_return);
        mBtnReturn.setOnClickListener(mClickListener);
        mBtnRunInBack = (Button) findViewById(R.id.hqa_readermode_btn_run_back);
        mBtnRunInBack.setOnClickListener(mClickListener);
        mRgTypeVRadioGroup.check(R.id.hqa_readermode_rb_typev_subcarrier);
        mBtnRunInBack.setEnabled(false);
    }

    private void setButtonsStatus(boolean b) {
        if (b) {
            mBtnStart.setText(R.string.hqa_nfc_start);
        } else {
            mBtnStart.setText(R.string.hqa_nfc_stop);
        }
        mBtnRunInBack.setEnabled(!b);
        mEnableBackKey = b;
        mBtnReturn.setEnabled(b);
        mBtnSelectAll.setEnabled(b);
        mBtnClearAll.setEnabled(b);
    }

    private void changeAllSelect(boolean checked) {
        Elog.v(NfcMainPage.TAG, "[ReaderMode]changeDisplay status is " + checked);
        for (int i = CHECKBOX_READER_TYPEA; i < mSettingsCkBoxs.length; i++) {
            mSettingsCkBoxs[i].setChecked(checked);
        }
        if (checked) {
            mRgTypeVRadioGroup.check(R.id.hqa_readermode_rb_typev_dual_subcarrier);
            mRgTypeVRadioGroup.check(R.id.hqa_readermode_rb_typev_subcarrier);
        } else {
            mRbTypeVDualSubcarrier.setChecked(checked);
            mRbTypeVSubcarrier.setChecked(checked);
        }
    }

    private void doTestAction(Boolean bStart) {
        sendCommand(bStart);
    }

    private void sendCommand(Boolean bStart) {
        NfcEmAlsReadermReq requestCmd = new NfcEmAlsReadermReq();
        fillRequest(bStart, requestCmd);
        NfcClient.getInstance().sendCommand(CommandType.MTK_NFC_EM_ALS_READER_MODE_REQ, requestCmd);
    }

    private void fillRequest(Boolean bStart, NfcEmAlsReadermReq requestCmd) {
        if (null == bStart) {
            requestCmd.mAction = EmAction.ACTION_RUNINBG;
        } else if (bStart.booleanValue()) {
            requestCmd.mAction = EmAction.ACTION_START;
        } else {
            requestCmd.mAction = EmAction.ACTION_STOP;
        }
        CheckBox[] typeBoxs = { mSettingsCkBoxs[CHECKBOX_READER_TYPEA],
                mSettingsCkBoxs[CHECKBOX_READER_TYPEB], mSettingsCkBoxs[CHECKBOX_READER_TYPEF],
                mSettingsCkBoxs[CHECKBOX_READER_TYPEV], mSettingsCkBoxs[CHECKBOX_READER_TYPEB2],
                mSettingsCkBoxs[CHECKBOX_READER_KOVIO] };
        requestCmd.mSupportType = BitMapValue.getTypeValue(typeBoxs);
        CheckBox[] typeADateRateBoxs = { mSettingsCkBoxs[CHECKBOX_READER_TYPEA_106],
                mSettingsCkBoxs[CHECKBOX_READER_TYPEA_212],
                mSettingsCkBoxs[CHECKBOX_READER_TYPEA_424],
                mSettingsCkBoxs[CHECKBOX_READER_TYPEA_848] };
        requestCmd.mTypeADataRate = BitMapValue.getTypeAbDataRateValue(typeADateRateBoxs);
        CheckBox[] typeBDateRateBoxs = { mSettingsCkBoxs[CHECKBOX_READER_TYPEB_106],
                mSettingsCkBoxs[CHECKBOX_READER_TYPEB_212],
                mSettingsCkBoxs[CHECKBOX_READER_TYPEB_424],
                mSettingsCkBoxs[CHECKBOX_READER_TYPEB_848] };
        requestCmd.mTypeBDataRate = BitMapValue.getTypeAbDataRateValue(typeBDateRateBoxs);
        CheckBox[] typeFDateRateBoxs = { mSettingsCkBoxs[CHECKBOX_READER_TYPEF_212],
                mSettingsCkBoxs[CHECKBOX_READER_TYPEF_424] };
        requestCmd.mTypeFDataRate = BitMapValue.getTypeFDataRateValue(typeFDateRateBoxs);
        CheckBox[] typeVDateRateBoxs = { mSettingsCkBoxs[CHECKBOX_READER_TYPEV_166],
                mSettingsCkBoxs[CHECKBOX_READER_TYPEV_2648] };
        requestCmd.mTypeVDataRate = BitMapValue.getTypeVDataRateValue(typeVDateRateBoxs);
        requestCmd.mTypeVSubcarrier = mRbTypeVSubcarrier.isChecked() ? 0 : 1;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        ProgressDialog dialog = null;
        if (id == DIALOG_ID_WAIT) {
            dialog = new ProgressDialog(this);
            dialog.setMessage(getString(R.string.hqa_nfc_dialog_wait_message));
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCancelable(false);
            return dialog;
        }
        return dialog;
    }

}
