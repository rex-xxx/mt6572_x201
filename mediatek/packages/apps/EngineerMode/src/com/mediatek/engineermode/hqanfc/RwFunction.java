package com.mediatek.engineermode.hqanfc;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.R;
import com.mediatek.engineermode.hqanfc.NfcCommand.CommandType;
import com.mediatek.engineermode.hqanfc.NfcCommand.DataConvert;
import com.mediatek.engineermode.hqanfc.NfcCommand.EmOptAction;
import com.mediatek.engineermode.hqanfc.NfcCommand.ReaderModeRspResult;
import com.mediatek.engineermode.hqanfc.NfcCommand.RspResult;
import com.mediatek.engineermode.hqanfc.NfcEmReqRsp.NfcEmAlsReadermNtf;
import com.mediatek.engineermode.hqanfc.NfcEmReqRsp.NfcEmAlsReadermOptReq;
import com.mediatek.engineermode.hqanfc.NfcEmReqRsp.NfcEmAlsReadermOptRsp;

import java.nio.ByteBuffer;

public class RwFunction extends Activity {

    protected static final int HANDLER_MSG_GET_RSP = 300;
    private static final int HANDLER_MSG_GET_NTF = 100;
    private TextView mTvUid;
    private Button mBtnRead;
    private Button mBtnWrite;
    private Button mBtnFormat;
    private byte[] mReadermRspArray;
    private NfcEmAlsReadermNtf mTransferReadermNtf;
    private NfcEmAlsReadermNtf mReceivedReadermNtf;
    private byte[] mRspArray;
    private NfcEmAlsReadermOptRsp mOptRsp;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Elog.v(NfcMainPage.TAG, "[RwFunction]mReceiver onReceive");
            String action = intent.getAction();
            mRspArray = intent.getExtras().getByteArray(NfcCommand.MESSAGE_CONTENT_KEY);
            if ((NfcCommand.ACTION_PRE + CommandType.MTK_NFC_EM_ALS_READER_MODE_OPT_RSP)
                    .equals(action)) {
                if (null != mRspArray) {
                    ByteBuffer buffer = ByteBuffer.wrap(mRspArray);
                    mOptRsp = new NfcEmAlsReadermOptRsp();
                    mOptRsp.readRaw(buffer);
                    mHandler.sendEmptyMessage(HANDLER_MSG_GET_RSP);
                }
            } else if ((NfcCommand.ACTION_PRE + CommandType.MTK_NFC_EM_ALS_READER_MODE_NTF)
                    .equals(action)) {
                if (null != mRspArray) {
                    ByteBuffer buffer = ByteBuffer.wrap(mRspArray);
                    mReceivedReadermNtf = new NfcEmAlsReadermNtf();
                    mReceivedReadermNtf.readRaw(buffer);
                    mHandler.sendEmptyMessage(HANDLER_MSG_GET_NTF);
                }
            } else {
                Elog.v(NfcMainPage.TAG, "[RwFunction]Other response");
            }
        }
    };

    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String toastMsg = null;
            if (HANDLER_MSG_GET_RSP == msg.what) {
                switch (mOptRsp.mResult) {
                    case RspResult.SUCCESS:
                        toastMsg = "Rw Format Rsp Result: SUCCESS";
                        mBtnFormat.setEnabled(false);
                        mBtnRead.setEnabled(true);
                        mBtnWrite.setEnabled(true);
                        break;
                    case RspResult.FAIL:
                        toastMsg = "Rw Format Rsp Result: FAIL";
                        break;
                    default:
                        toastMsg = "Rw Format Rsp Result: ERROR";
                        break;
                }

            } else if (HANDLER_MSG_GET_NTF == msg.what) {
                switch (mReceivedReadermNtf.mResult) {
                    case ReaderModeRspResult.CONNECT:
                        toastMsg = "ReaderMode Ntf Result: CONNECT";
                        if (mReceivedReadermNtf.mIsNdef == 0 || mReceivedReadermNtf.mIsNdef == 1
                                || mReceivedReadermNtf.mIsNdef == 2) {
                            Intent intent = new Intent();
                            intent.putExtra(ReaderMode.KEY_READER_MODE_RSP_ARRAY, mRspArray);
                            intent.putExtra(ReaderMode.KEY_READER_MODE_RSP_NDEF, mReceivedReadermNtf.mIsNdef);
                            intent.setClass(RwFunction.this, RwFunction.class);
                            startActivity(intent);
                            RwFunction.this.finish();
                        }
                        break;
                    case ReaderModeRspResult.DISCONNECT:
                        toastMsg = "ReaderMode Ntf Result: DISCONNECT";
                        RwFunction.this.onBackPressed();
                        break;
                    case ReaderModeRspResult.FAIL:
                        toastMsg = "ReaderMode Ntf Result: FAIL";
                        break;
                    default:
                        toastMsg = "ReaderMode Ntf Result: ERROR";
                        break;
                }
            }
            Toast.makeText(RwFunction.this, toastMsg, Toast.LENGTH_SHORT).show();
        }
    };

    private final Button.OnClickListener mClickListener = new Button.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            Elog.v(NfcMainPage.TAG, "[RwFunction]onClick button view is "
                    + ((Button) arg0).getText());
            Intent intent = new Intent();
            if (arg0.equals(mBtnRead)) {
                intent.setClass(RwFunction.this, FunctionRead.class);
                intent.putExtra(FunctionRead.PARENT_EXTRA_STR, 0);
                startActivity(intent);
            } else if (arg0.equals(mBtnWrite)) {
                intent.setClass(RwFunction.this, FunctionWrite.class);
                startActivity(intent);
            } else if (arg0.equals(mBtnFormat)) {
                doFormat();
            } else {
                Elog.v(NfcMainPage.TAG, "[RwFunction]onClick noting.");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hqa_nfc_rw_function);
        mTvUid = (TextView) findViewById(R.id.hqa_nfc_rw_tv_uid);
        mBtnRead = (Button) findViewById(R.id.hqa_nfc_rw_btn_read);
        mBtnWrite = (Button) findViewById(R.id.hqa_nfc_rw_btn_write);
        mBtnFormat = (Button) findViewById(R.id.hqa_nfc_rw_btn_format);
        mBtnRead.setOnClickListener(mClickListener);
        mBtnWrite.setOnClickListener(mClickListener);
        mBtnFormat.setOnClickListener(mClickListener);
        Intent intent = getIntent();
        int ndefType = intent.getIntExtra(ReaderMode.KEY_READER_MODE_RSP_NDEF, 1);
        if (ndefType == 1) { // ndef
            mBtnFormat.setEnabled(false);
        } else if (ndefType == 0) { // format
            mBtnRead.setEnabled(false);
            mBtnWrite.setEnabled(false);
        } else if (ndefType == 2) { // read function only
            mBtnFormat.setEnabled(false);
            mBtnWrite.setEnabled(false);
        }
        mReadermRspArray = intent.getByteArrayExtra(ReaderMode.KEY_READER_MODE_RSP_ARRAY);
        if (null == mReadermRspArray) {
            Toast.makeText(this, "Not get the response", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (null == mTransferReadermNtf) {
            mTransferReadermNtf = new NfcEmAlsReadermNtf();
        }
        mTransferReadermNtf.readRaw(ByteBuffer.wrap(mReadermRspArray));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTvUid
                .setText("UID: "
                        + DataConvert.printHexString(mTransferReadermNtf.mUid,
                                mTransferReadermNtf.mUidLen));
        IntentFilter filter = new IntentFilter();
        filter.addAction(NfcCommand.ACTION_PRE + CommandType.MTK_NFC_EM_ALS_READER_MODE_OPT_RSP);
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

    private void doFormat() {
        NfcEmAlsReadermOptReq request = new NfcEmAlsReadermOptReq();
        request.mAction = EmOptAction.FORMAT;
        NfcClient.getInstance()
                .sendCommand(CommandType.MTK_NFC_EM_ALS_READER_MODE_OPT_REQ, request);
    }

}
