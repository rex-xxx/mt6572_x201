package com.mediatek.nfcdemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.util.Log;
import android.nfc.NfcAdapter;
import android.content.DialogInterface;
import android.os.Vibrator;
import java.util.ArrayList;

import com.mediatek.nfcdemo.nfc.NativeNfcManager;

public class P2PTests extends Activity implements OnClickListener {

    private static final String TAG = "NfcDemo";
    private static final boolean DBG = true;

    private boolean mNfcEnabled = false;
    private Button mStartButton;
    private Button mStopButton;
    private RadioGroup mRadioGpRole;
    private RadioGroup mRadioGpLength;
    private EditText mLoopsEdit;
    private ArrayList<RadioButton> mRoleRadioItems = new ArrayList<RadioButton>();
    private ArrayList<RadioButton> mLengthRadioItems = new ArrayList<RadioButton>();
    private TextView mResultText;
    private int mSelectionRoleId;
    private int mSelectionLengthId;
    private int mLoops;
    private NativeNfcManager mNfcManager;

    private ProgressDialog mProgressDialog;
    private IntentFilter mIntentFilter;

    private String mATR;
    private String mResultString;
    private int mDepDataLength;

    private View mLoopsView;
    
    private Vibrator mVibrator;
    static final long[] VIBRATION_PATTERN = {0, 100, 10000};

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(action)) {
                handleNfcStateChanged(intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE,
                        NfcAdapter.STATE_OFF));
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);    
        // init state
        mNfcManager = new NativeNfcManager();
        //init Sound
        mNfcManager.initSoundPool(this);
        //new InitTask().execute();
        // init UI
        setContentView(R.layout.p2p_tests);
        initUI();         
    }

    @Override
    protected void onDestroy() {
        if (mNfcManager != null) {
            mNfcManager.releaseSoundPool();
            mNfcManager.deinitialize();
        }
        //enableNfc();
        super.onDestroy();        
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.p2p_test_start:
                if (DBG) Log.d(TAG, "Reader START");
                runStartTest();
                break;
            case R.id.p2p_test_stop:
                if (DBG) Log.d(TAG, "Reader STOP");
                runStopTest();
                break;
            default:
                Log.d(TAG, "ghost button");
                break;
        }
    }

    private void disableNfc(){
        if (DBG) Log.d(TAG, "disable Nfc");
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        mNfcEnabled = adapter.isEnabled();
        if (mNfcEnabled) {
            if (DBG) Log.d(TAG, "Nfc is on");
            mIntentFilter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
            registerReceiver(mReceiver, mIntentFilter);
            if (adapter.disable()) {
                mProgressDialog = ProgressDialog.show(P2PTests.this, "Disable Nfc", "Please wait ...", true);
                mProgressDialog.show(); 
            }
        } else {
            if (DBG) Log.d(TAG, "Nfc is off");
        }
    }

    private void enableNfc() {
        if (DBG) Log.d(TAG, "enable Nfc");
        if (mNfcEnabled) {
            NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
            adapter.enable();
        }
    }

    private void runStartTest() {
        
        //Get Data length   
        switch(mSelectionLengthId){
            case Utility.DATA_LENGTH_1:
                if(DBG)Log.d(TAG, "2 byte");
                mDepDataLength = 2;
                break;

            case Utility.DATA_LENGTH_2:
                if(DBG)Log.d(TAG, "32 byte");
                mDepDataLength = 32;
                break;

            case Utility.DATA_LENGTH_3:
                if(DBG)Log.d(TAG, "64 byte");
                mDepDataLength = 64;
                break;

            case Utility.DATA_LENGTH_4:
                if(DBG)Log.d(TAG, "128 byte");
                mDepDataLength = 128;
                break;
                
            case Utility.DATA_LENGTH_5:
                if(DBG)Log.d(TAG, "190 byte");
                mDepDataLength = 190;
                break;
                
            default:    
                if(DBG)Log.d(TAG, "ghost ...");
                mDepDataLength = 2;
                break;               
        }
        //Get loops
        if (mSelectionRoleId == Utility.TYPE_ID_P2P_I) { //get times
            String loops = mLoopsEdit.getText().toString();
            if (loops == null || loops.equals("")) {
                mLoops = 50;
            } else {
                mLoops = Integer.valueOf(loops);
            } 
        }
        
        //SDD
        new StartSingleDeviceDetectTask().execute();
    }

    private void runStopTest() {
        
    }

    private void runSetReceiveOnlyMode() {
        new SetReceiveOnlyModeTask().execute();
    }

    private void runDataExchange() {
        //DEP
        new DataExchangeTask().execute();
    }
    
    private void initUI() {
        //Button
        mStartButton = (Button) findViewById(R.id.p2p_test_start);
        mStartButton.setOnClickListener(this);
        mStopButton = (Button) findViewById(R.id.p2p_test_stop);
        mStopButton.setOnClickListener(this);
        //Radio Group
        //Type
        mRadioGpRole = (RadioGroup) findViewById(R.id.role_group);
        mRoleRadioItems.add((RadioButton) findViewById(R.id.role_group_item1));
        mRoleRadioItems.add((RadioButton) findViewById(R.id.role_group_item2));
        mRadioGpRole
            .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    mSelectionRoleId = getSelectionId(checkedId);

                    if (mSelectionRoleId == Utility.TYPE_ID_P2P_I) {
                       mLoopsView.setVisibility(View.VISIBLE); 
                    } else if (mSelectionRoleId == Utility.TYPE_ID_P2P_T) {
                       mLoopsView.setVisibility(View.GONE); 
                    }                   
                }

                private int getSelectionId(int radioId) {
                    final int[] idxs =
                        { R.id.role_group_item1,
                          R.id.role_group_item2 };
                    final int[] ids =
                        { Utility.TYPE_ID_P2P_I,
                          Utility.TYPE_ID_P2P_T };

                    for (int i = 0; i < idxs.length; i++) {
                        if (idxs[i] == radioId) {
                            return ids[i];
                        }
                    }
                    Log.e(TAG, "Ghost RadioGroup checkId " + radioId);
                    return Utility.TYPE_ID_P2P_I;
                }
            });

        //Length
        mRadioGpLength = (RadioGroup) findViewById(R.id.data_length_group);
        mLengthRadioItems.add((RadioButton) findViewById(R.id.data_length_group_item1));
        mLengthRadioItems.add((RadioButton) findViewById(R.id.data_length_group_item2));
        mLengthRadioItems.add((RadioButton) findViewById(R.id.data_length_group_item3));
        mLengthRadioItems.add((RadioButton) findViewById(R.id.data_length_group_item4));
        mLengthRadioItems.add((RadioButton) findViewById(R.id.data_length_group_item5));
        mRadioGpLength
            .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    mSelectionLengthId = getSelectionId(checkedId);
                }

                private int getSelectionId(int radioId) {
                    final int[] idxs =
                        { R.id.data_length_group_item1,
                          R.id.data_length_group_item2,
                          R.id.data_length_group_item3,
                          R.id.data_length_group_item4,
                          R.id.data_length_group_item5 };
                    final int[] ids =
                        { Utility.DATA_LENGTH_1,
                          Utility.DATA_LENGTH_2,
                          Utility.DATA_LENGTH_3,
                          Utility.DATA_LENGTH_4,
                          Utility.DATA_LENGTH_5};

                    for (int i = 0; i < idxs.length; i++) {
                        if (idxs[i] == radioId) {
                            return ids[i];
                        }
                    }
                    Log.e(TAG, "Ghost RadioGroup checkId " + radioId);
                    return Utility.DATA_LENGTH_1;
                }
            });        
        //Text
        mResultText = (TextView) findViewById(R.id.p2p_result);

        //View
        mLoopsView = (View) findViewById(R.id.loops_view);
        
        //EditText
        mLoopsEdit = (EditText) findViewById(R.id.loops_edit);
        
        //default settings
        mRadioGpRole.check(R.id.role_group_item1);
        mRadioGpLength.check(R.id.data_length_group_item1);
        mStopButton.setEnabled(false);   
        //vibrator
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }


    class InitTask extends AsyncTask<Integer,String,Integer> {
        private ProgressDialog mProgressDialog;
                
        @Override
        protected Integer doInBackground(Integer... count) {
            
            Integer successCounts = new Integer(0);

            int result = mNfcManager.writeSwpReg();
            return successCounts;
        }
        
        @Override
        protected void onPreExecute(){
            mProgressDialog = ProgressDialog.show(P2PTests.this, "Write SWP Req ", "Please wait ...", true);
            mProgressDialog.setCancelable(true);
            mProgressDialog.show();
        }
        
        @Override
        protected void onProgressUpdate(String... progress) {
        }   
        
        @Override
        protected void onPostExecute(Integer counts) {
            if(mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            disableNfc();
        }           
    }



    class StartSingleDeviceDetectTask extends AsyncTask<Integer,String,Integer> {
        private ProgressDialog mProgressDialog;
        private String resultMessage =  "---- SDD ----\n";
        private boolean isCanceled = false;
        
        @Override
        protected Integer doInBackground(Integer... count) {

            Integer success = new Integer(0);
            mNfcManager.deinitialize();
            int result = mNfcManager.initialize();
            if (result > 0) {
                if (DBG) Log.d(TAG, "init success. result fd = " + result);
            } else {
                if (DBG) Log.d(TAG, "init fail.");
                return success;
            }

            String progressArray = "";
            for (int i = 0; i < 10; i++){  //retry 10
                if (isCanceled) {
                    break;
                }
                progressArray = "Try : = " +  i + " ...\n";
                publishProgress(progressArray);	 //update 
                
                result = mNfcManager.singleDeviceDetect(mSelectionRoleId);
                if (result < 0) {
                    if (DBG) Log.d(TAG, "SDD Failed");
                    ///----
                    //mNfcManager.deinitialize();
                    ///----                   
                } else {
                    Log.d(TAG, "!!" + mNfcManager.readUidSuccess());
                    if (mNfcManager.readUidSuccess()) {
                        if (DBG) Log.d(TAG, "readUidSuccess");
                        byte[] atr = mNfcManager.getUid();
                        mATR = Utility.binToHex(atr, result);
                        success = 1; 
                        progressArray = "Success !";
                        publishProgress(progressArray);	 //update 
                        resultMessage += "retry : " + i + " \n";
                        break;
                    } else {
                        progressArray = "Fail ...";
                        publishProgress(progressArray);	 //update 
                    }
                }    
                   
            }

            return success;
        }
        
        @Override
        protected void onPreExecute(){
            mProgressDialog = ProgressDialog.show(P2PTests.this, "SDD...", "Please wait ...", true);
            //isCanceled = false;
            
            //mProgressDialog = new ProgressDialog(P2PTests.this);
            //mProgressDialog.setMessage("SDD...");
            //mProgressDialog.setCancelable(false);
            //mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            //    @Override
            //    public void onClick(DialogInterface dialog, int which) {
            //        doCancel();
            //        dialog.dismiss();
            //    }
            //});            
            mProgressDialog.show();
        }
        @Override
        protected void onProgressUpdate(String... progress) {
            mProgressDialog.setMessage( progress[0]); 
        }       
        
        @Override
        protected void onPostExecute(Integer success) {
            if(mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            //String result = "---- SDD ----\n";
            byte [] rawData = mNfcManager.getRawData();
            if (success == 1) {
                //Sound
                mNfcManager.playSound(NativeNfcManager.SOUND_START);
                //Vibrator
                mVibrator.vibrate(VIBRATION_PATTERN, -1);
                
                if (mSelectionRoleId == Utility.TYPE_ID_P2P_I) {
                    //goto DEP
                    resultMessage +=  "Initiator : receive ATR : \n" + mATR + " \n";
                    runDataExchange();
                } else if (mSelectionRoleId == Utility.TYPE_ID_P2P_T) {
                    //goto Receive only mode
                    resultMessage +=  "Target : receive ATR : \n" + mATR + " \n";
                    runSetReceiveOnlyMode();
                }                
            } else {
                //fail
                //Sound
                mNfcManager.playSound(NativeNfcManager.SOUND_ERROR);  
                resultMessage += "Fail \n";
                if (rawData != null) {
                    resultMessage += "---- Raw Data ----\n" +
                              Utility.binToHex( rawData, rawData.length) + "\n";
                }                
            }
            mResultText.setText(resultMessage);
        } 

        public void doCancel(){
            isCanceled = true;
            mNfcManager.deinitialize();            
        }
    }

    class SetReceiveOnlyModeTask extends AsyncTask<Integer,String,Integer> {
        private ProgressDialog mProgressDialog;
                
        @Override
        protected Integer doInBackground(Integer... count) {

            Integer success = new Integer(0);

            int result = mNfcManager.setReceiveOnlyMode();
            if (result < 0) {
                if (DBG) Log.d(TAG, "SDD Failed");
                ///----
                //Sound
                mNfcManager.playSound(NativeNfcManager.SOUND_ERROR);  
                mNfcManager.deinitialize();
                ///----                
            } else {
                success = 1;
            }

            return success;
        }
        
        @Override
        protected void onPreExecute(){
            mProgressDialog = ProgressDialog.show(P2PTests.this, "SetReceiveOnlyMode...", "Please wait ...", true);
            mProgressDialog.show();
        }
        @Override
        protected void onProgressUpdate(String... progress) {
        
        }       
        @Override
        protected void onPostExecute(Integer success) {
            if(mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            String result = "---- Set receive only mode ----\n";
            byte [] rawData = mNfcManager.getRawData();
            if (success == 1) {
                if (mSelectionRoleId == Utility.TYPE_ID_P2P_T) {
                    result += "Success \n";
                } else {
                    Log.d(TAG, "Wrong state");
                    result += "Wrong state \n";
                }                
            } else {
                mNfcManager.playSound(NativeNfcManager.SOUND_ERROR);
                result += "Fail \n";
                if (rawData != null) {
                    result += "---- Raw Data ----\n" +
                              Utility.binToHex( rawData, rawData.length) + "\n";                
                }                  
            }
            mResultText.setText(mResultText.getText() + result);
            runDataExchange();
        }           
    }


    class DataExchangeTask extends AsyncTask<Integer,String,Integer> {
        private ProgressDialog mProgressDialog;
        private boolean isCanceled = false;
                
        @Override
        protected Integer doInBackground(Integer... count) {
            int result;
            Integer success = new Integer(0);
            String progressArray = "";
            mResultString = "";
            byte headerCounter = 0x00;
            int depCounter = 0;
            int errorCode = 0;
            int runCounter = 0;
            byte [] byteArray;
            byte [] dataPayload = createDataPayload(mDepDataLength);
            
            if (DBG) Log.d(TAG, ">>> " + Utility.binToHex( dataPayload, dataPayload.length));
            
            //------------- P2P - I
            if (mSelectionRoleId == Utility.TYPE_ID_P2P_I ) {
                headerCounter = 0x03;
                depCounter=0;
                for(int i=0 ; i<mLoops; i++){
                    try {
                        if (isCanceled) {
                            Log.d(TAG, "Cancel!!");
                            break;
                        }                        
                        /// ----------------------
                        result = mNfcManager.dataExchange(mSelectionRoleId, headerCounter, mDepDataLength, dataPayload);
                        if (result < 0) {
                            if (DBG) Log.d(TAG, "DEP Failed"); 
                            errorCode = mNfcManager.getErrorCode();
                            
                            if (errorCode == Utility.I2C_SEND_ERROR || errorCode == Utility.I2C_RECEIVE_ERROR) {
                                mNfcManager.deinitialize();
                                mNfcManager.initialize();
                                Log.d(TAG, "DEP reset i2c");
                            }
                            
                            byteArray = mNfcManager.getRawData();

                            progressArray = "---- DEP ----\n" +   
                                            "SUCCESS: " + depCounter + " \n" +
                                            "TOTAL: " + mLoops + " \n" + 
                                            "---- ERROR REASON ----\n" +
                                            Utility.getErrorMessage( errorCode ) + "\n" +
                                            "---- Raw Data ----\n";
                            if (byteArray != null) {
                                progressArray += Utility.binToHex( byteArray, byteArray.length) + "\n";
                            }
                            //mResultString += progressArray;
                        } else {

                            byte [] temp = mNfcManager.getDepHeader();
                            byteArray = mNfcManager.getRawData();
                            headerCounter = temp[2];
                            depCounter++;
                            
                            progressArray = "==== TOTAL: " + mLoops + " , CURRENT: " + i + " ====\n" +
                                            "==== SUCCESS: " + depCounter + " ====\n" +
                                            "Header: " + Utility.binToHex(temp, temp.length) + " \n" ;
                            temp = mNfcManager.getDepDataPayload();
                            progressArray += "DataPayload: " + Utility.binToHex( temp, temp.length) + " \n" + 
                                             "---- Raw Data ----\n";
                            if (byteArray != null) {
                                progressArray += Utility.binToHex( byteArray, byteArray.length) + "\n";
                            }
                            //publishProgress(progressArray);	 //update
                        }
                        publishProgress(progressArray);	 //update    
                        ///-----------------------
                    } catch (Exception e) {
                        Log.d(TAG, "" + e);
                    }
                }

                mResultString += progressArray;
            //---------- P2P - T    
            } else if (mSelectionRoleId == Utility.TYPE_ID_P2P_T) {
                while(true) {
                    try {
                        if (isCanceled) {
                            Log.d(TAG, "Cancel!!");
                            mResultString += progressArray;
                            break;
                        }
                        ///---------------------
                        result = mNfcManager.dataExchange(mSelectionRoleId, headerCounter, mDepDataLength, dataPayload);
                        runCounter++;
                        if (result < 0) {
                            if (DBG) Log.d(TAG, "DEP Failed");
                            byteArray = mNfcManager.getRawData();
                            errorCode = mNfcManager.getErrorCode();
                            if (errorCode == Utility.I2C_SEND_ERROR || errorCode == Utility.I2C_RECEIVE_ERROR) {
                                mNfcManager.deinitialize();
                                mNfcManager.initialize();
                            }
                            
                            progressArray = "---- DEP ----\n" +
                                            "SUCCESS: " + depCounter + "\n" +
                                            "TOTAL: " + runCounter + "\n" +
                                            "---- ERROR REASON ----\n" +
                                            Utility.getErrorMessage( errorCode ) + "\n" +
                                            "---- Raw Data ----\n";
                            if (byteArray != null) {
                                progressArray += Utility.binToHex( byteArray, byteArray.length) + "\n";
                            }

                            if (errorCode == Utility.FIELD_OFF /* || errorCode == Utility.DEP_FAIL*/) {
                                mResultString += progressArray;
                                if (DBG) Log.d(TAG, "Target fail and break;");
                                break;
                            }
                        
                        } else {

                            byte [] temp = mNfcManager.getDepHeader();
                            byteArray = mNfcManager.getRawData();
                            headerCounter = temp[2];
                            depCounter++;
                            
                            progressArray = "==== " + depCounter + " ====\n" +
                                            "Header: " + Utility.binToHex(temp, temp.length) + " \n" ;
                            temp = mNfcManager.getDepDataPayload();
                            progressArray += "DataPayload: " + Utility.binToHex( temp, temp.length) + " \n" + 
                                             "---- Raw Data ----\n";
                            if (byteArray != null) {
                                progressArray += Utility.binToHex( byteArray, byteArray.length) + "\n";
                            }                        
                            //publishProgress(progressArray);	 //update
                        }
                        publishProgress(progressArray);	 //update
                        ///---------------------
                    } catch ( Exception e) {
                        Log.d(TAG, "" + e);
                    }
                }
            } else {
                Log.d(TAG, "ghost role...");
            }
        
            return success;
            
        }
        
        @Override
        protected void onPreExecute(){
            //mProgressDialog = ProgressDialog.show(P2PTests.this, "DEP...", "Please wait ...", true);    
            isCanceled = false;
            
            mProgressDialog = new ProgressDialog(P2PTests.this);
            mProgressDialog.setMessage("DEP...");
            mProgressDialog.setCancelable(false);
            mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    doCancel();
                    dialog.dismiss();
                }
            });
           

            mProgressDialog.show();
        }
        
        @Override
        protected void onProgressUpdate(String... progress) {
            mProgressDialog.setMessage( progress[0]);        
        }       
        
        @Override
        protected void onPostExecute(Integer success) {
            if(mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            ///----
            //mNfcManager.deinitialize();
            ///----   
            //Sound
            mNfcManager.playSound(NativeNfcManager.SOUND_ERROR);
            //Vibrator
            mVibrator.vibrate(VIBRATION_PATTERN, -1);
            
            mResultText.setText( mResultText.getText() + "\n" +
                                 mResultString );   
            
        } 

        public void doCancel(){
            isCanceled = true;
            mNfcManager.deinitialize();
        }
        
    }

    private byte [] createDataPayload(int dataLength) {
        byte [] data = new byte [dataLength];
        byte count = 0x00;
        for(int i=0; i < dataLength; i++, count++) {
            data[i] = count;
        }
        return data;
    }

    private void handleNfcStateChanged(int newState) {
        switch (newState) {
        case NfcAdapter.STATE_OFF:
            if(mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            unregisterReceiver(mReceiver);
            break;
            
        case NfcAdapter.STATE_ON:
            break;
            
        case NfcAdapter.STATE_TURNING_ON:
            break;
            
        case NfcAdapter.STATE_TURNING_OFF:
            break;
            
        }
    }
}


