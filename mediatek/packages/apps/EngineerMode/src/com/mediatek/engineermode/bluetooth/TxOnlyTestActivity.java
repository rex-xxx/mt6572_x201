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

package com.mediatek.engineermode.bluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.mediatek.engineermode.R;
import com.mediatek.xlog.Xlog;

/**
 * Do BT tx mode test.
 * 
 * @author mtk54040
 * 
 */
public class TxOnlyTestActivity extends Activity implements
        DialogInterface.OnCancelListener {

    private BluetoothAdapter mAdapter;

    private Spinner mPattern = null;
    private Spinner mChannels = null;
    private Spinner mPktTypes = null;

    private BtTest mBtTest = null;

    private static final int BT_TEST_0 = 0;
    private static final int BT_TEST_3 = 3;
    private static final int RETURN_FAIL = -1;

    // dialog ID and MSG ID
    private static final int CHECK_BT_STATE = 1;
//    private static final int CHECK_BT_DEVEICE = 2;
    private static final int TEST_TX = 3;

    private boolean mHasInit = false;
    private boolean mIniting = false;
    private boolean mNonModulate = false;
    private boolean mPocketType = false;

    private static final int MAP_TO_PATTERN = 0;
    private static final int MAP_TO_CHANNELS = 1;
    private static final int MAP_TO_POCKET_TYPE = 2;
    private static final int MAP_TO_FREQ = 3;
    private static final int MAP_TO_POCKET_TYPE_LEN = 4;

    private static final String TAG = "TxOnlyTest";

    private int mStateBt;
    // added by chaozhong @2010-10-10
    private Handler mWorkHandler = null; // used to handle the
    // "done"
    // button clicked action
    private boolean mDoneTest = true; // used to record weather the
    // "done" button clicked
    // event has been finished
    // private HandlerThread mHandlerThread = null;
    public static final int DLGID_OP_IN_PROCESS = 1;
    public static final int OP_IN_PROCESS = 2;
    public static final int OP_FINISH = 0;
    public static final int OP_TX_FAIL = 4;

    // private static Handler mUiHandler = null;
    // ProgressDialog mDialogSearchProgress = null;

    // end added
    @Override
    public void onCreate(Bundle savedInstanceState) {
//        Xlog.v(TAG, "-->onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tx_only_test);
        // Initialize the UI component
        setValuesSpinner();

        HandlerThread workThread = new HandlerThread(TAG);
        workThread.start();
        Looper looper = workThread.getLooper();
        mWorkHandler = new Handler(looper);

    }

    private Handler mUiHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case OP_IN_PROCESS:
                // {
                Xlog.w(TAG, "OP_IN_PROCESS");
                showDialog(TEST_TX);
                break;
            case OP_FINISH:
                Xlog.i(TAG, "OP_FINISH");
                removeDialog(TEST_TX);
                break;
            case OP_TX_FAIL:
                Xlog.i(TAG, "OP_TX_FAIL");
                // showDialog(DIALOG_CHECK_BT_DEVEICE);
                removeDialog(TEST_TX);
                break;
            default:
                break;
            }
        }
    };

    private class WorkRunnable implements Runnable {
        // public Handler mHandler;

        public void run() {
            mUiHandler.sendEmptyMessage(OP_IN_PROCESS);
            mDoneTest = false;
            doSendCommandAction();
            mDoneTest = true;
            mUiHandler.sendEmptyMessage(OP_FINISH);
        }
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem doneItem = menu.getItem(Menu.FIRST - 1);
        if (null != doneItem) {
            if (!mDoneTest) {
                doneItem.setEnabled(false);
                menu.close();
            } else {
                doneItem.setEnabled(true);
            }
        } else {
            Xlog.i(TAG, "menu_done is not found.");
        }
        return true;
    }

    /**
     * Initialize the UI component
     * 
     * 
     */
    protected void setValuesSpinner() {
        // for TX pattern
        mPattern = (Spinner) findViewById(R.id.PatternSpinner);
        ArrayAdapter<CharSequence> adapterPattern = ArrayAdapter
                .createFromResource(this, R.array.tx_pattern,
                        android.R.layout.simple_spinner_item);
        adapterPattern
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPattern.setAdapter(adapterPattern);

        // for TX channels
        mChannels = (Spinner) findViewById(R.id.ChannelsSpinner);
        ArrayAdapter<CharSequence> adapterChannels = ArrayAdapter
                .createFromResource(this, R.array.tx_channels,
                        android.R.layout.simple_spinner_item);
        adapterChannels
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mChannels.setAdapter(adapterChannels);

        // for TX pocket type
        mPktTypes = (Spinner) findViewById(R.id.PocketTypeSpinner);
        ArrayAdapter<CharSequence> adapterPocketType = ArrayAdapter
                .createFromResource(this, R.array.tx_Pocket_type,
                        android.R.layout.simple_spinner_item);
        adapterPocketType
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPktTypes.setAdapter(adapterPocketType);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_show, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_done: 
            // edited by chaozhogn @ 2010-10-10
            // return doSendCommandAction();
            Xlog.i(TAG, "menu_done is clicked.");
            if (mDoneTest) {
                // if the last click action has been handled, send another event
                // request
                mWorkHandler.post(new WorkRunnable());
            } else {
                Xlog.i(TAG, "last click is not finished yet.");
            }
            Xlog.i(TAG, "menu_done is handled.");
            return true;
            // edit end

        case R.id.menu_discard: 
            return doRevertAction();
        default:
            break;
        }
        return false;
    }

    /**
     * Send command the user has made, and finish the activity.
     */
    private boolean doSendCommandAction() {
        getBtState();
        enableBluetooth(false);
        getValuesAndSend();
        return true;
    }

    // implemented for DialogInterface.OnCancelListener
    public void onCancel(DialogInterface dialog) {
        // request that the service stop the query with this callback
        // mBtTestect.
        Xlog.v(TAG, "-->onCancel");
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Xlog.v(TAG, "-->onStart");
        if (mAdapter == null) {
            mAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (mAdapter.getState() != BluetoothAdapter.STATE_OFF) {
            showDialog(CHECK_BT_STATE);
        }
//        if (mAdapter != null) {
//            if (mAdapter.getState() != BluetoothAdapter.STATE_OFF) {
//                showDialog(CHECK_BT_STATE);
//            }
//        } else {
//            showDialog(CHECK_BT_DEVEICE);
//        }
    }


    @Override
    protected void onDestroy() {
        Xlog.v(TAG, "TXOnlyTest onDestroy.");

        removeDialog(TEST_TX);
        if (mBtTest != null) {
            // if (-1 == mBtTest.doBtTest(3)) {
            if (RETURN_FAIL == mBtTest.doBtTest(BT_TEST_3)) {
                Xlog.i(TAG, "stop failed.");

            }
            mBtTest = null;
        }
//        else {
//            Xlog.i(TAG, "BtTest does not start yet.");
//        }

        super.onDestroy();
        // mDialogSearchProgress = null;

    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Xlog.i(TAG, "-->onCreateDialog");
        if (id == TEST_TX) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getString(R.string.BT_init_dev));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            Xlog.i(TAG, "new ProgressDialog succeed");
            return dialog;
        } else if (id == CHECK_BT_STATE) {
            AlertDialog dialog = null;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setTitle(R.string.Error);
            builder.setMessage(R.string.BT_turn_bt_off);
            builder.setPositiveButton(R.string.OK,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            dialog = builder.create();
            return dialog;
        }
        return null;
    }

    private void getBtState() {
        Xlog.v(TAG, "Enter GetBtState().");
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (null == btAdapter) {
            Xlog.i(TAG, "we can not find a bluetooth adapter.");
            // Toast.makeText(getApplicationContext(),
            // "We can not find a bluetooth adapter.", Toast.LENGTH_SHORT)
            // .show();
            mUiHandler.sendEmptyMessage(OP_TX_FAIL);
            return;
        }
        mStateBt = btAdapter.getState();
        Xlog.i(TAG, "Leave GetBtState().");
    }

    private void enableBluetooth(boolean enable) {
        Xlog.v(TAG, "Enter EnableBluetooth().");
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (null == btAdapter) {
            Xlog.i(TAG, "we can not find a bluetooth adapter.");
            return;
        }
        // need to enable
        if (enable) {
            Xlog.i(TAG, "Bluetooth is enabled");
            btAdapter.enable();
        } else {
            // need to disable
            Xlog.i(TAG, "Bluetooth is disabled");
            btAdapter.disable();
        }
        Xlog.i(TAG, "Leave EnableBluetooth().");
    }

    /**
     * Revert any changes the user has made, and finish the activity.
     */
    private boolean doRevertAction() {
        finish();
        return true;
    }

    // private BtTest mBtTest = null;

    public void getValuesAndSend() {
        Xlog.i(TAG, "Enter GetValuesAndSend().");
        mBtTest = new BtTest();

        getSpinnerValue(mPattern, MAP_TO_PATTERN);
        getSpinnerValue(mChannels, MAP_TO_CHANNELS);
        getSpinnerValue(mPktTypes, MAP_TO_POCKET_TYPE);

        getEditBoxValue(R.id.edtFrequency, MAP_TO_FREQ);
        getEditBoxValue(R.id.edtPocketLength, MAP_TO_POCKET_TYPE_LEN);
        
        // send command to....
        // new issue added by mtk54040 Shuaiqiang @2011-10-12
        Xlog.i(TAG, "PocketType().+" + mBtTest.getPocketType());
        Xlog.i(TAG, "edtFrequency+" + mBtTest.getFreq());
        if (27 == mBtTest.getPocketType()) {
            Xlog.i(TAG, "enter handleNonModulated(mBtTest)");
            Xlog.i(TAG, "mbIsNonModulate--" + mNonModulate
                    + "   mbIsPocketType--" + mPocketType);
            if (mPocketType) { // mbIsPocketType for avoid mBtTest is null
                runHCIResetCmd();
            }
            if (initBtTestOjbect()) {
                mNonModulate = true;
                mPocketType = false;
//                    handleNonModulated(mBtTest);
                handleNonModulated();
            }

        } else {
            if (mNonModulate) {
                runHCIResetCmd();
                mNonModulate = false;
            }
            mPocketType = true;
            if (RETURN_FAIL == mBtTest.doBtTest(BT_TEST_0)) {
                Xlog.i(TAG, "transmit data failed.");
                if ((BluetoothAdapter.STATE_TURNING_ON == mStateBt)
                        || (BluetoothAdapter.STATE_ON == mStateBt)) {
                    enableBluetooth(true);
                }
                // Toast.makeText(getApplicationContext(),
                // "transmit data failed.", Toast.LENGTH_SHORT).show();
                mUiHandler.sendEmptyMessage(OP_TX_FAIL);
            }
        }
        Xlog.i(TAG, "Leave getValuesAndSend().");
    }

    private void handleNonModulated() {
        Xlog.i(TAG, "-->handleNonModulated TX first");
        /*
         * If pressing "Stop" button Tx: 01 0C 20 02 00 PP 0xPP = Filter
         * Duplicate (00 = Disable Duplicate Filtering, 01 = Enable Duplicate
         * Filtering) Rx: 04 0E 04 01 0C 20 00
         */

        /*
         * TX: 01 15 FC 01 00 RX: 04 0E 04 01 15 FC 00 TX: 01 D5 FC 01 XX (XX =
         * Channel) RX: 04 0E 04 01 D5 FC 00
         */
        int cmdLen = 5;
        char[] cmd = new char[cmdLen];
        char[] response = null;
        int i = 0;
        cmd[0] = 0x01;
        cmd[1] = 0x15;
        cmd[2] = 0xFC;
        cmd[3] = 0x01;
        cmd[4] = 0x00;
        response = mBtTest.hciCommandRun(cmd, cmdLen);
        if (response != null) {
            String s = null;
            for (i = 0; i < response.length; i++) {
                s = String.format("response[%d] = 0x%x", i, (long) response[i]);
                Xlog.i(TAG, s);
            }
        } else {
            Xlog.i(TAG, "HCICommandRun failed");
        }
        response = null;

        Xlog.i(TAG, "-->handleNonModulated TX second");
        cmdLen = 5;
        cmd[0] = 0x01;
        cmd[1] = 0xD5;
        cmd[2] = 0xFC;
        cmd[3] = 0x01;
        cmd[4] = (char) mBtTest.getFreq();
        response = mBtTest.hciCommandRun(cmd, cmdLen);
        if (response != null) {
            String s = null;
            for (i = 0; i < response.length; i++) {
                s = String.format("response[%d] = 0x%x", i, (long) response[i]);
                Xlog.i(TAG, s);
            }
        } else {
            Xlog.i(TAG, "HCICommandRun failed");
        }
        response = null;
    }

    // init BtTest -call init function of BtTest
    private boolean initBtTestOjbect() {
        Xlog.i(TAG, "-->initBtTestOjbect");
        if (mIniting) {
            return false;
        }
        if (mHasInit) {
            return mHasInit;
        }
        if (mBtTest == null) {
            mBtTest = new BtTest();
        }
        if (mBtTest != null && !mHasInit) {
            mIniting = true;
            if (mBtTest.init() != 0) {
                mHasInit = false;
                Xlog.i(TAG, "mBtTest initialization failed");
            } else {
                runHCIResetCmd();
                mHasInit = true;
            }
        }
        mIniting = false;
        return mHasInit;
    }

    // clear BtTest mBtTestect -call deInit function of BtTest
    // private boolean uninitBtTestOjbect() {
    // Xlog.i(TAG, "-->uninitBtTestOjbect");
    // if (mBtTest != null && mbIsInit) {
    // runHCIResetCmd();
    // if (mBtTest.unInit() != 0) {
    // Xlog.i(TAG, "mBtTest un-initialization failed");
    // }
    // }
    // mBtTest = null;
    // mbIsInit = false;
    // return true;
    // }

    // run "HCI Reset" command
    private void runHCIResetCmd() {
        /*
         * If pressing "HCI Reset" button Tx: 01 03 0C 00 Rx: 04 0E 04 01 03 0C
         * 00 After pressing "HCI Reset" button, all state will also be reset
         */
        int cmdLen = 4;
        char[] cmd = new char[cmdLen];

        char[] response = null;
        int i = 0;
        Xlog.i(TAG, "-->runHCIResetCmd");
        cmd[0] = 0x01;
        cmd[1] = 0x03;
        cmd[2] = 0x0C;
        cmd[3] = 0x00;
        if (mBtTest == null) {
            mBtTest = new BtTest();
        }
        response = mBtTest.hciCommandRun(cmd, cmdLen);
        if (response != null) {
            String s = null;
            for (i = 0; i < response.length; i++) {
                s = String.format("response[%d] = 0x%x", i, (long) response[i]);
                Xlog.i(TAG, s);
            }
        } else {
            Xlog.i(TAG, "HCICommandRun failed");
        }
        response = null;
    }

    private boolean getSpinnerValue(Spinner sSpinner, int flag) {
        boolean bSuccess = false;
        int index = sSpinner.getSelectedItemPosition();
        if (0 > index) {
            return bSuccess;
        }

        switch (flag) {
        case MAP_TO_PATTERN: // Pattern
            mBtTest.setPatter(index);
            break;
        case MAP_TO_CHANNELS: // Channels
            mBtTest.setChannels(index);
            break;
        case MAP_TO_POCKET_TYPE: // Pocket type
            mBtTest.setPocketType(index);
            break;
        default:
            bSuccess = false;
            break;
        }
        bSuccess = true;
        return bSuccess;
    }

    private boolean getEditBoxValue(int id, int flag) {
        boolean bSuccess = false;

        TextView text = (TextView) findViewById(id);
        String str = null;
        if (null != text) {
            str = text.getText().toString();
        }
        if ((null == str) || str.equals("")) {
            return bSuccess;
        }
        int iLen = 0;
        try {
            iLen = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            Xlog.i(TAG, "parseInt failed--invalid number!");
            return bSuccess;
        }
        // frequency
        if (MAP_TO_FREQ == flag) {
            mBtTest.setFreq(iLen);
            bSuccess = true;
        } else if (MAP_TO_POCKET_TYPE_LEN == flag) {
            // pocket type length
            mBtTest.setPocketTypeLen(iLen);
            bSuccess = true;
        }
        return bSuccess;
    }
}
