package com.mediatek.engineermode.hqanfc;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.R;
import com.mediatek.engineermode.hqanfc.NfcCommand.BitMapValue;
import com.mediatek.engineermode.hqanfc.NfcCommand.CommandType;
import com.mediatek.engineermode.hqanfc.NfcCommand.EmAction;
import com.mediatek.engineermode.hqanfc.NfcCommand.P2pDisableCardM;
import com.mediatek.engineermode.hqanfc.NfcCommand.RspResult;
import com.mediatek.engineermode.hqanfc.NfcEmReqRsp.NfcEmAlsCardmRsp;
import com.mediatek.engineermode.hqanfc.NfcEmReqRsp.NfcEmAlsP2pNtf;
import com.mediatek.engineermode.hqanfc.NfcEmReqRsp.NfcEmAlsReadermNtf;
import com.mediatek.engineermode.hqanfc.NfcEmReqRsp.NfcEmPollingNty;
import com.mediatek.engineermode.hqanfc.NfcEmReqRsp.NfcEmPollingReq;
import com.mediatek.engineermode.hqanfc.NfcEmReqRsp.NfcEmPollingRsp;

import java.nio.ByteBuffer;

public class PollingLoopMode extends Activity {

    private static final int HANDLER_MSG_GET_RSP = 200;
    private static final int HANDLER_MSG_GET_NTF = 100;
    private static final int DIALOG_ID_RESULT = 0;
    private static final int DIALOG_ID_WAIT = 1;

    private static final int CHECKBOX_READER_MODE = 0;
    private static final int CHECKBOX_READER_TYPEA = 1;
    private static final int CHECKBOX_READER_TYPEA_106 = 7;
    private static final int CHECKBOX_READER_TYPEA_212 = 8;
    private static final int CHECKBOX_READER_TYPEA_424 = 9;
    private static final int CHECKBOX_READER_TYPEA_848 = 10;
    private static final int CHECKBOX_READER_TYPEB = 2;
    private static final int CHECKBOX_READER_TYPEB_106 = 11;
    private static final int CHECKBOX_READER_TYPEB_212 = 12;
    private static final int CHECKBOX_READER_TYPEB_424 = 13;
    private static final int CHECKBOX_READER_TYPEB_848 = 14;
    private static final int CHECKBOX_READER_TYPEF = 3;
    private static final int CHECKBOX_READER_TYPEF_212 = 15;
    private static final int CHECKBOX_READER_TYPEF_424 = 16;
    private static final int CHECKBOX_READER_TYPEV = 4;
    private static final int CHECKBOX_READER_TYPEV_166 = 17;
    private static final int CHECKBOX_READER_TYPEV_2648 = 18;
    private static final int CHECKBOX_READER_TYPEB2 = 5;
    private static final int CHECKBOX_READER_KOVIO = 6;

    private static final int CHECKBOX_P2P_MODE = 19;
    private static final int CHECKBOX_P2P_TYPEA = 20;
    private static final int CHECKBOX_P2P_TYPEA_106 = 27;
    private static final int CHECKBOX_P2P_TYPEA_212 = 28;
    private static final int CHECKBOX_P2P_TYPEA_424 = 29;
    private static final int CHECKBOX_P2P_TYPEA_848 = 30;
    private static final int CHECKBOX_P2P_TYPEF = 21;
    private static final int CHECKBOX_P2P_TYPEF_212 = 31;
    private static final int CHECKBOX_P2P_TYPEF_424 = 32;
    private static final int CHECKBOX_P2P_PASSIVE_MODE = 22;
    private static final int CHECKBOX_P2P_ACTIVE_MODE = 23;
    private static final int CHECKBOX_P2P_INITIATOR = 24;
    private static final int CHECKBOX_P2P_TARGET = 25;
    private static final int CHECKBOX_P2P_DISABLE_CARD = 26;

    private static final int CHECKBOX_CARD_MODE = 33;
    private static final int CHECKBOX_CARD_SWIO1 = 34;
    private static final int CHECKBOX_CARD_SWIO2 = 35;
    private static final int CHECKBOX_CARD_SWIOSE = 36;
    private static final int CHECKBOX_CARD_TYPEA = 37;
    private static final int CHECKBOX_CARD_TYPEB = 38;
    private static final int CHECKBOX_CARD_TYPEB2 = 39;
    private static final int CHECKBOX_CARD_TYPEF = 40;
    private static final int CHECKBOX_CARD_VITRUAL_CARD = 41;
    private static final int CHECKBOXS_NUMBER = 42;

    private EditText mTvPeriod;
    private CheckBox[] mSettingsCkBoxs = new CheckBox[CHECKBOXS_NUMBER];
    private RadioGroup mRgPollingSelect;
    private RadioButton mRbPollingSelectListen;
    private RadioButton mRbPollingSelectPause;
    private RadioGroup mRgTypeVRadioGroup;
    private RadioButton mRbTypeVSubcarrier;
    private RadioButton mRbTypeVDualSubcarrier;
    private Button mBtnSelectAll;
    private Button mBtnClearAll;
    private Button mBtnStart;
    private Button mBtnReturn;
    private Button mBtnRunInBack;

    private NfcEmPollingRsp mPollingRsp;
    private NfcEmPollingNty mPollingNty;
    private byte[] mRspArray;
    private String mNtfContent;
    private boolean mEnableBackKey = true;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Elog.v(NfcMainPage.TAG, "[PollingLoopMode]mReceiver onReceive: " + action);
            mRspArray = intent.getExtras().getByteArray(NfcCommand.MESSAGE_CONTENT_KEY);
            if ((NfcCommand.ACTION_PRE + CommandType.MTK_NFC_EM_POLLING_MODE_RSP).equals(action)) {
                if (null != mRspArray) {
                    ByteBuffer buffer = ByteBuffer.wrap(mRspArray);
                    mPollingRsp = new NfcEmPollingRsp();
                    mPollingRsp.readRaw(buffer);
                    mHandler.sendEmptyMessage(HANDLER_MSG_GET_RSP);
                }
            } else if ((NfcCommand.ACTION_PRE + CommandType.MTK_NFC_EM_POLLING_MODE_NTF).equals(action)) {
                if (null != mRspArray) {
                    ByteBuffer buffer = ByteBuffer.wrap(mRspArray);
                    mPollingNty = new NfcEmPollingNty();
                    mPollingNty.readRaw(buffer);
                    mHandler.sendEmptyMessage(HANDLER_MSG_GET_NTF);
                }
            } else {
                Elog.v(NfcMainPage.TAG, "[PollingLoopMode]Other response");
            }
        }
    };

    private final Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String toastMsg = null;
            if (HANDLER_MSG_GET_RSP == msg.what) {
                dismissDialog(DIALOG_ID_WAIT);
                switch (mPollingRsp.mResult) {
                    case RspResult.SUCCESS:
                        toastMsg = "Poling Loop Mode Rsp Result: SUCCESS";
                        if (mBtnStart.getText().equals(PollingLoopMode.this.getString(R.string.hqa_nfc_start))) {
                            setButtonsStatus(false);
                        } else {
                            setButtonsStatus(true);
                        }
                        break;
                    case RspResult.FAIL:
                        toastMsg = "Poling Loop Mode Rsp Result: FAIL";
                        break;
                    default:
                        toastMsg = "Poling Loop Mode Rsp Result: ERROR";
                        break;
                }
            } else if (HANDLER_MSG_GET_NTF == msg.what) {
                switch (mPollingNty.mDetectType) {
                    case NfcCommand.EM_ENABLE_FUNC_P2P_MODE:
                        NfcEmAlsP2pNtf alsP2pNtf = new NfcEmAlsP2pNtf();
                        alsP2pNtf.readRaw(ByteBuffer.wrap(mPollingNty.mData));
                        if (RspResult.SUCCESS == alsP2pNtf.mResult) {
                            toastMsg = "P2P Data Exchange is terminated";
                            // mNtfContent = new String(alsP2pNtf.mData);
                            // showDialog(DIALOG_ID_RESULT);
                        } else if (RspResult.FAIL == alsP2pNtf.mResult) {
                            toastMsg = "P2P Data Exchange is On-going";
                        } else {
                            toastMsg = "P2P Data Exchange is ERROR";
                        }
                        break;
                    case NfcCommand.EM_ENABLE_FUNC_READER_MODE:
                        // show RW function ui
                        NfcEmAlsReadermNtf readermNtf = new NfcEmAlsReadermNtf();
                        readermNtf.readRaw(ByteBuffer.wrap(mPollingNty.mData));
                        Intent intent = new Intent();
                        intent.putExtra(ReaderMode.KEY_READER_MODE_RSP_ARRAY, mPollingNty.mData);
                        intent.putExtra(ReaderMode.KEY_READER_MODE_RSP_NDEF, readermNtf.mIsNdef);
                        intent.setClass(PollingLoopMode.this, RwFunction.class);
                        startActivity(intent);
                        break;
                    case NfcCommand.EM_ENABLE_FUNC_RCARDR_MODE:
                        NfcEmAlsCardmRsp alsCardRsp = new NfcEmAlsCardmRsp();
                        alsCardRsp.readRaw(ByteBuffer.wrap(mPollingNty.mData));
                        if (RspResult.SUCCESS == alsCardRsp.mResult) {
                            toastMsg = "CardEmulation Rsp Result: SUCCESS";
                        } else if (RspResult.FAIL == alsCardRsp.mResult) {
                            toastMsg = "CardEmulation Rsp Result: FAIL";
                        } else {
                            toastMsg = "CardEmulation Rsp Result: ERROR";
                        }
                        break;
                    default:
                        break;
                }
            }
            Toast.makeText(PollingLoopMode.this, toastMsg, Toast.LENGTH_SHORT).show();
        }
    };

    private final CheckBox.OnCheckedChangeListener mCheckedListener = new CheckBox.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
            Elog.v(NfcMainPage.TAG, "[PollingLoopMode]onCheckedChanged view is " + buttonView.getText() + " value is "
                    + checked);
            if (buttonView.equals(mSettingsCkBoxs[CHECKBOX_READER_MODE])) {
                for (int i = CHECKBOX_READER_TYPEA; i < CHECKBOX_READER_TYPEA_106; i++) {
                    mSettingsCkBoxs[i].setEnabled(checked);
                }
            } else if (buttonView.equals(mSettingsCkBoxs[CHECKBOX_READER_TYPEA])) {
                for (int i = CHECKBOX_READER_TYPEA_106; i < CHECKBOX_READER_TYPEB_106; i++) {
                    mSettingsCkBoxs[i].setEnabled(checked);
                }
            } else if (buttonView.equals(mSettingsCkBoxs[CHECKBOX_READER_TYPEB])) {
                for (int i = CHECKBOX_READER_TYPEB_106; i < CHECKBOX_READER_TYPEF_212; i++) {
                    mSettingsCkBoxs[i].setEnabled(checked);
                }
            } else if (buttonView.equals(mSettingsCkBoxs[CHECKBOX_READER_TYPEF])) {
                for (int i = CHECKBOX_READER_TYPEF_212; i < CHECKBOX_READER_TYPEV_166; i++) {
                    mSettingsCkBoxs[i].setEnabled(checked);
                }
            } else if (buttonView.equals(mSettingsCkBoxs[CHECKBOX_READER_TYPEV])) {
                for (int i = CHECKBOX_READER_TYPEV_166; i < CHECKBOX_P2P_MODE; i++) {
                    mSettingsCkBoxs[i].setEnabled(checked);
                }
                mRbTypeVSubcarrier.setEnabled(checked);
                mRbTypeVDualSubcarrier.setEnabled(checked);
            } else if (buttonView.equals(mSettingsCkBoxs[CHECKBOX_P2P_MODE])) {
                for (int i = CHECKBOX_P2P_TYPEA; i < CHECKBOX_P2P_TYPEA_106; i++) {
                    mSettingsCkBoxs[i].setEnabled(checked);
                }
            } else if (buttonView.equals(mSettingsCkBoxs[CHECKBOX_P2P_TYPEA])) {
                for (int i = CHECKBOX_P2P_TYPEA_106; i < CHECKBOX_P2P_TYPEF_212; i++) {
                    mSettingsCkBoxs[i].setEnabled(checked);
                }
            } else if (buttonView.equals(mSettingsCkBoxs[CHECKBOX_P2P_TYPEF])) {
                for (int i = CHECKBOX_P2P_TYPEF_212; i < CHECKBOX_CARD_MODE; i++) {
                    mSettingsCkBoxs[i].setEnabled(checked);
                }
            } else if (buttonView.equals(mSettingsCkBoxs[CHECKBOX_CARD_MODE])) {
                for (int i = CHECKBOX_CARD_SWIO1; i < CHECKBOXS_NUMBER; i++) {
                    mSettingsCkBoxs[i].setEnabled(checked);
                }
            }
        }
    };

    private final Button.OnClickListener mClickListener = new Button.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            Elog.v(NfcMainPage.TAG, "[PollingLoopMode]onClick button view is " + ((Button) arg0).getText());
            if (arg0.equals(mBtnStart)) {
                if (!checkRoleSelect()) {
                    Toast.makeText(PollingLoopMode.this, R.string.hqa_nfc_p2p_role_tip, Toast.LENGTH_LONG).show();
                } else {
                    showDialog(DIALOG_ID_WAIT);
                    doTestAction(mBtnStart.getText().equals(PollingLoopMode.this.getString(R.string.hqa_nfc_start)));
                }
            } else if (arg0.equals(mBtnSelectAll)) {
                changeAllSelect(true);
            } else if (arg0.equals(mBtnClearAll)) {
                changeAllSelect(false);
            } else if (arg0.equals(mBtnReturn)) {
                PollingLoopMode.this.onBackPressed();
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
        setContentView(R.layout.hqa_nfc_pollingloop_mode);
        initComponents();
        changeAllSelect(true);
        mSettingsCkBoxs[CHECKBOX_CARD_VITRUAL_CARD].setChecked(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(NfcCommand.ACTION_PRE + CommandType.MTK_NFC_EM_POLLING_MODE_RSP);
        filter.addAction(NfcCommand.ACTION_PRE + CommandType.MTK_NFC_EM_POLLING_MODE_NTF);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
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
        Elog.v(NfcMainPage.TAG, "[PollingLoopMode]initComponents");
        // reader mode
        mRgPollingSelect = (RadioGroup) findViewById(R.id.hqa_pollingmode_rg_polling_select);
        mRbPollingSelectListen = (RadioButton) findViewById(R.id.hqa_pollingmode_rb_polling_listen);
        mRbPollingSelectPause = (RadioButton) findViewById(R.id.hqa_pollingmode_rb_polling_pause);
        mTvPeriod = (EditText) findViewById(R.id.hqa_pollingmode_et_polling_period);
        mSettingsCkBoxs[CHECKBOX_READER_MODE] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_reader_mode);
        mSettingsCkBoxs[CHECKBOX_READER_MODE].setOnCheckedChangeListener(mCheckedListener);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEA] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_typea);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEA].setOnCheckedChangeListener(mCheckedListener);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEA_106] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_typea_106);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEA_212] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_typea_212);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEA_424] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_typea_424);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEA_848] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_typea_848);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEB] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_typeb);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEB].setOnCheckedChangeListener(mCheckedListener);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEB_106] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_typeb_106);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEB_212] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_typeb_212);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEB_424] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_typeb_424);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEB_848] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_typeb_848);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEF] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_typef);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEF].setOnCheckedChangeListener(mCheckedListener);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEF_212] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_typef_212);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEF_424] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_typef_424);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEV] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_typev);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEV].setOnCheckedChangeListener(mCheckedListener);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEV_166] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_typev_166);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEV_2648] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_typev_2648);
        mSettingsCkBoxs[CHECKBOX_READER_TYPEB2] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_typeb2);
        mSettingsCkBoxs[CHECKBOX_READER_KOVIO] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_kovio);
        mRgTypeVRadioGroup = (RadioGroup) findViewById(R.id.hqa_pollingmode_rg_typev);
        mRbTypeVSubcarrier = (RadioButton) findViewById(R.id.hqa_pollingmode_rb_typev_subcarrier);
        mRbTypeVDualSubcarrier = (RadioButton) findViewById(R.id.hqa_pollingmode_rb_typev_dual_subcarrier);

        // p2p
        mSettingsCkBoxs[CHECKBOX_P2P_MODE] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_p2p_mode);
        mSettingsCkBoxs[CHECKBOX_P2P_MODE].setOnCheckedChangeListener(mCheckedListener);
        mSettingsCkBoxs[CHECKBOX_P2P_TYPEA] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_p2p_typea);
        mSettingsCkBoxs[CHECKBOX_P2P_TYPEA].setOnCheckedChangeListener(mCheckedListener);
        mSettingsCkBoxs[CHECKBOX_P2P_TYPEA_106] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_p2p_typea_106);
        mSettingsCkBoxs[CHECKBOX_P2P_TYPEA_212] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_p2p_typea_212);
        mSettingsCkBoxs[CHECKBOX_P2P_TYPEA_424] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_p2p_typea_424);
        mSettingsCkBoxs[CHECKBOX_P2P_TYPEA_848] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_p2p_typea_848);
        mSettingsCkBoxs[CHECKBOX_P2P_TYPEF] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_p2p_typef);
        mSettingsCkBoxs[CHECKBOX_P2P_TYPEF].setOnCheckedChangeListener(mCheckedListener);
        mSettingsCkBoxs[CHECKBOX_P2P_TYPEF_212] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_p2p_typef_212);
        mSettingsCkBoxs[CHECKBOX_P2P_TYPEF_424] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_p2p_typef_424);
        mSettingsCkBoxs[CHECKBOX_P2P_PASSIVE_MODE] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_p2p_passive_mode);
        mSettingsCkBoxs[CHECKBOX_P2P_ACTIVE_MODE] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_p2p_active_mode);
        mSettingsCkBoxs[CHECKBOX_P2P_INITIATOR] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_p2p_initiator);
        mSettingsCkBoxs[CHECKBOX_P2P_TARGET] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_p2p_target);
        mSettingsCkBoxs[CHECKBOX_P2P_DISABLE_CARD] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_p2p_disable_card_emu);

        // card mode
        mSettingsCkBoxs[CHECKBOX_CARD_MODE] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_card_emu_mode);
        mSettingsCkBoxs[CHECKBOX_CARD_MODE].setOnCheckedChangeListener(mCheckedListener);
        mSettingsCkBoxs[CHECKBOX_CARD_SWIO1] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_card_emu_swio1);
        mSettingsCkBoxs[CHECKBOX_CARD_SWIO2] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_card_emu_swio2);
        mSettingsCkBoxs[CHECKBOX_CARD_SWIOSE] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_card_emu_swiose);
        mSettingsCkBoxs[CHECKBOX_CARD_TYPEA] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_card_emu_typea);
        mSettingsCkBoxs[CHECKBOX_CARD_TYPEB] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_emu_typeb);
        mSettingsCkBoxs[CHECKBOX_CARD_TYPEB2] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_card_emu_typeb2);
        mSettingsCkBoxs[CHECKBOX_CARD_TYPEF] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_emu_typef);
        mSettingsCkBoxs[CHECKBOX_CARD_VITRUAL_CARD] = (CheckBox) findViewById(R.id.hqa_pollingmode_cb_emu_virtual_card);
        mBtnSelectAll = (Button) findViewById(R.id.hqa_pollingmode_btn_select_all);
        mBtnSelectAll.setOnClickListener(mClickListener);
        mBtnClearAll = (Button) findViewById(R.id.hqa_pollingmode_btn_clear_all);
        mBtnClearAll.setOnClickListener(mClickListener);
        mBtnStart = (Button) findViewById(R.id.hqa_pollingmode_btn_start_stop);
        mBtnStart.setOnClickListener(mClickListener);
        mBtnReturn = (Button) findViewById(R.id.hqa_pollingmode_btn_return);
        mBtnReturn.setOnClickListener(mClickListener);
        mBtnRunInBack = (Button) findViewById(R.id.hqa_pollingmode_btn_run_back);
        mBtnRunInBack.setOnClickListener(mClickListener);
        mBtnRunInBack.setEnabled(false);
        mRgTypeVRadioGroup.check(R.id.hqa_pollingmode_rb_typev_subcarrier);
        mRgPollingSelect.check(R.id.hqa_pollingmode_rb_polling_listen);
        mTvPeriod.setText("500");
        mTvPeriod.setSelection(mTvPeriod.getText().toString().length());
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
        Elog.v(NfcMainPage.TAG, "[PollingLoopMode]changeDisplay status is " + checked);
        for (int i = 0; i < mSettingsCkBoxs.length; i++) {
            mSettingsCkBoxs[i].setChecked(checked);
        }
        if (checked) {
            mRgTypeVRadioGroup.check(R.id.hqa_pollingmode_rb_typev_dual_subcarrier);
            mRgPollingSelect.check(R.id.hqa_pollingmode_rb_polling_pause);
            mRgTypeVRadioGroup.check(R.id.hqa_pollingmode_rb_typev_subcarrier);
            mRgPollingSelect.check(R.id.hqa_pollingmode_rb_polling_listen);
        } else {
            mRbPollingSelectListen.setChecked(checked);
            mRbPollingSelectPause.setChecked(checked);
            mRbTypeVDualSubcarrier.setChecked(checked);
            mRbTypeVSubcarrier.setChecked(checked);
        }
    }

    private void doTestAction(Boolean bStart) {
        sendCommand(bStart);
    }

    private void sendCommand(Boolean bStart) {
        NfcEmPollingReq requestCmd = new NfcEmPollingReq();
        fillRequest(bStart, requestCmd);
        NfcClient.getInstance().sendCommand(CommandType.MTK_NFC_EM_POLLING_MODE_REQ, requestCmd);
    }

    private void fillRequest(Boolean bStart, NfcEmPollingReq requestCmd) {
        if (null == bStart) {
            requestCmd.mAction = EmAction.ACTION_RUNINBG;
            requestCmd.mP2pmReq.mAction = EmAction.ACTION_RUNINBG;
            requestCmd.mReadermReq.mAction = EmAction.ACTION_RUNINBG;
            requestCmd.mCardmReq.mAction = EmAction.ACTION_RUNINBG;
        } else if (bStart.booleanValue()) {
            requestCmd.mAction = EmAction.ACTION_START;
            requestCmd.mP2pmReq.mAction = EmAction.ACTION_START;
            requestCmd.mReadermReq.mAction = EmAction.ACTION_START;
            requestCmd.mCardmReq.mAction = EmAction.ACTION_START;
        } else {
            requestCmd.mAction = EmAction.ACTION_STOP;
            requestCmd.mP2pmReq.mAction = EmAction.ACTION_STOP;
            requestCmd.mReadermReq.mAction = EmAction.ACTION_STOP;
            requestCmd.mCardmReq.mAction = EmAction.ACTION_STOP;
        }
        requestCmd.mPhase = mRbPollingSelectListen.isChecked() ? 0 : 1;
        try {
            requestCmd.mPeriod = Integer.valueOf(mTvPeriod.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please input the right Period.", Toast.LENGTH_SHORT).show();
        }
        CheckBox[] functionBoxs = { mSettingsCkBoxs[CHECKBOX_READER_MODE], mSettingsCkBoxs[CHECKBOX_CARD_MODE],
                mSettingsCkBoxs[CHECKBOX_P2P_MODE] };
        requestCmd.mEnableFunc = BitMapValue.getFunctionValue(functionBoxs);
        // p2p
        int p2pTemp = 0;
        p2pTemp |= mSettingsCkBoxs[CHECKBOX_P2P_TYPEA].isChecked() ? NfcCommand.EM_ALS_READER_M_TYPE_A : 0;
        p2pTemp |= mSettingsCkBoxs[CHECKBOX_P2P_TYPEF].isChecked() ? NfcCommand.EM_ALS_READER_M_TYPE_F : 0;
        requestCmd.mP2pmReq.mSupportType = p2pTemp;
        CheckBox[] typeADateRateBoxs = { mSettingsCkBoxs[CHECKBOX_P2P_TYPEA_106], mSettingsCkBoxs[CHECKBOX_P2P_TYPEA_212],
                mSettingsCkBoxs[CHECKBOX_P2P_TYPEA_424], mSettingsCkBoxs[CHECKBOX_P2P_TYPEA_848] };
        requestCmd.mP2pmReq.mTypeADataRate = BitMapValue.getTypeAbDataRateValue(typeADateRateBoxs);
        CheckBox[] typeFDateRateBoxs = { mSettingsCkBoxs[CHECKBOX_P2P_TYPEF_212], mSettingsCkBoxs[CHECKBOX_P2P_TYPEF_424] };
        requestCmd.mP2pmReq.mTypeFDataRate = BitMapValue.getTypeFDataRateValue(typeFDateRateBoxs);
        requestCmd.mP2pmReq.mIsDisableCardM = mSettingsCkBoxs[CHECKBOX_P2P_DISABLE_CARD].isChecked() ? 
                P2pDisableCardM.DISABLE : P2pDisableCardM.NOT_DISABLE;
        p2pTemp = 0;
        p2pTemp |= mSettingsCkBoxs[CHECKBOX_P2P_INITIATOR].isChecked() ? NfcCommand.EM_P2P_ROLE_INITIATOR_MODE : 0;
        p2pTemp |= mSettingsCkBoxs[CHECKBOX_P2P_TARGET].isChecked() ? NfcCommand.EM_P2P_ROLE_TARGET_MODE : 0;
        requestCmd.mP2pmReq.mRole = p2pTemp;
        p2pTemp = 0;
        p2pTemp |= mSettingsCkBoxs[CHECKBOX_P2P_PASSIVE_MODE].isChecked() ? NfcCommand.EM_P2P_MODE_PASSIVE_MODE : 0;
        p2pTemp |= mSettingsCkBoxs[CHECKBOX_P2P_ACTIVE_MODE].isChecked() ? NfcCommand.EM_P2P_MODE_ACTIVE_MODE : 0;
        requestCmd.mP2pmReq.mMode = p2pTemp;
        // reader mode
        CheckBox[] typeBoxs = { mSettingsCkBoxs[CHECKBOX_READER_TYPEA], mSettingsCkBoxs[CHECKBOX_READER_TYPEB],
                mSettingsCkBoxs[CHECKBOX_READER_TYPEF], mSettingsCkBoxs[CHECKBOX_READER_TYPEV],
                mSettingsCkBoxs[CHECKBOX_READER_TYPEB2], mSettingsCkBoxs[CHECKBOX_READER_KOVIO] };
        requestCmd.mReadermReq.mSupportType = BitMapValue.getTypeValue(typeBoxs);
        CheckBox[] readerADateRateBoxs = { mSettingsCkBoxs[CHECKBOX_READER_TYPEA_106],
                mSettingsCkBoxs[CHECKBOX_READER_TYPEA_212], mSettingsCkBoxs[CHECKBOX_READER_TYPEA_424],
                mSettingsCkBoxs[CHECKBOX_READER_TYPEA_848] };
        requestCmd.mReadermReq.mTypeADataRate = BitMapValue.getTypeAbDataRateValue(readerADateRateBoxs);
        CheckBox[] readerBDateRateBoxs = { mSettingsCkBoxs[CHECKBOX_READER_TYPEB_106],
                mSettingsCkBoxs[CHECKBOX_READER_TYPEB_212], mSettingsCkBoxs[CHECKBOX_READER_TYPEB_424],
                mSettingsCkBoxs[CHECKBOX_READER_TYPEB_848] };
        requestCmd.mReadermReq.mTypeBDataRate = BitMapValue.getTypeAbDataRateValue(readerBDateRateBoxs);
        CheckBox[] readerFDateRateBoxs = { mSettingsCkBoxs[CHECKBOX_READER_TYPEF_212],
                mSettingsCkBoxs[CHECKBOX_READER_TYPEF_424] };
        requestCmd.mReadermReq.mTypeFDataRate = BitMapValue.getTypeFDataRateValue(readerFDateRateBoxs);
        CheckBox[] readerVDateRateBoxs = { mSettingsCkBoxs[CHECKBOX_READER_TYPEV_166],
                mSettingsCkBoxs[CHECKBOX_READER_TYPEV_2648] };
        requestCmd.mReadermReq.mTypeVDataRate = BitMapValue.getTypeVDataRateValue(readerVDateRateBoxs);
        requestCmd.mReadermReq.mTypeVSubcarrier = mRbTypeVSubcarrier.isChecked() ? 0 : 1;
        // card mode
        int cardTemp = 0;
        cardTemp |= mSettingsCkBoxs[CHECKBOX_CARD_TYPEA].isChecked() ? NfcCommand.EM_ALS_READER_M_TYPE_A : 0;
        cardTemp |= mSettingsCkBoxs[CHECKBOX_CARD_TYPEB].isChecked() ? NfcCommand.EM_ALS_READER_M_TYPE_B : 0;
        cardTemp |= mSettingsCkBoxs[CHECKBOX_CARD_TYPEF].isChecked() ? NfcCommand.EM_ALS_READER_M_TYPE_F : 0;
        cardTemp |= mSettingsCkBoxs[CHECKBOX_CARD_TYPEB2].isChecked() ? NfcCommand.EM_ALS_READER_M_TYPE_BPRIME : 0;
        requestCmd.mCardmReq.mSupportType = cardTemp;
        cardTemp = 0;
        cardTemp |= mSettingsCkBoxs[CHECKBOX_CARD_SWIO1].isChecked() ? NfcCommand.EM_ALS_CARD_M_SW_NUM_SWIO1 : 0;
        cardTemp |= mSettingsCkBoxs[CHECKBOX_CARD_SWIO2].isChecked() ? NfcCommand.EM_ALS_CARD_M_SW_NUM_SWIO2 : 0;
        cardTemp |= mSettingsCkBoxs[CHECKBOX_CARD_SWIOSE].isChecked() ? NfcCommand.EM_ALS_CARD_M_SW_NUM_SWIOSE : 0;
        requestCmd.mCardmReq.mSwNum = cardTemp;
        requestCmd.mCardmReq.mFgVirtualCard = mSettingsCkBoxs[CHECKBOX_CARD_VITRUAL_CARD].isChecked() ? 1 : 0;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (DIALOG_ID_WAIT == id) {
            ProgressDialog dialog = null;
            dialog = new ProgressDialog(this);
            dialog.setMessage(getString(R.string.hqa_nfc_dialog_wait_message));
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCancelable(false);
            return dialog;
        } else if (DIALOG_ID_RESULT == id) {
            AlertDialog alertDialog = null;
            alertDialog = new AlertDialog.Builder(PollingLoopMode.this).setTitle(R.string.hqa_nfc_p2p_mode_ntf_title)
                    .setMessage(mNtfContent).setPositiveButton(android.R.string.ok, null).create();
            return alertDialog;
        }
        return null;
    }

    private boolean checkRoleSelect() {
        boolean result = true;
        if (!mSettingsCkBoxs[CHECKBOX_P2P_INITIATOR].isChecked() && !mSettingsCkBoxs[CHECKBOX_P2P_TARGET].isChecked()) {
            result = false;
        }
        if (!mSettingsCkBoxs[CHECKBOX_P2P_PASSIVE_MODE].isChecked()
                && !mSettingsCkBoxs[CHECKBOX_P2P_ACTIVE_MODE].isChecked()) {
            result = false;
        }
        return result;
    }
}
