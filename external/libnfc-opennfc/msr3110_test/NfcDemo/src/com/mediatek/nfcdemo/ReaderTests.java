
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

import java.util.ArrayList;
import com.mediatek.nfcdemo.nfc.NativeNfcManager;

public class ReaderTests extends Activity implements OnClickListener, NativeNfcManager.Callback{

    private static final String TAG = "NfcDemo";
    private static final boolean DBG = true;

    private boolean mNfcEnabled = false;
    private Button mStartButton;
    private Button mStopButton;
    private RadioGroup mRadioGpType;
    private RadioGroup mRadioGpCommand;
    private ArrayList<RadioButton> mTypeRadioItems = new ArrayList<RadioButton>(); 
    private ArrayList<RadioButton> mCommandRadioItems = new ArrayList<RadioButton>();
    private View mSinglePollingTimeView;
    private EditText mTimesEdit;
    private TextView mResultText;

    private static final int COMMAND_ID_POLLING_LOOP = 2001;
    private static final int COMMAND_ID_SINGLE_POLLING = 2002;

    private static final int MSG_DETECT_CARD = 3001;

    private int mSelectionTypeId;
    private int mSelectionCommandId;
    private int mTimes;
    private NativeNfcManager mNfcManager;
    private String mReceiveString;
    private ArrayList<String> mUIDList = new ArrayList<String>();
    private Long startTime;
    private Long spentTime;

    private ProgressDialog mProgressDialog;
    private IntentFilter mIntentFilter;

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


    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DETECT_CARD:
                    if(DBG)Log.d(TAG, "detect card.");
                    byte[] uid = mNfcManager.getUid();
                    String uidString = Utility.binToHex(uid, mNfcManager.getLength());
                    if (mSelectionTypeId != Utility.TYPE_ID_P2P_I && mSelectionTypeId != Utility.TYPE_ID_P2P_T) {
                        mResultText.setText("UID :" + uidString);
                    } else {
                        mResultText.setText("ATR :" + uidString);
                    }
                    setInProgress(false);
                    mNfcManager.deinitialize();
                    break;
            }
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // init state
        mNfcManager = new NativeNfcManager();
        //new InitTask().execute();
        // init UI
        setContentView(R.layout.reader_tests);
        initUI();        
    }

    @Override
    protected void onDestroy() {
        if (mNfcManager != null) {
            mNfcManager.stopPollingLoop();
            mNfcManager.deinitialize();
        }
        //enableNfc();
        super.onDestroy();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.reader_test_start:
                if (DBG) Log.d(TAG, "Reader START");
                runStartTest();
                break;
            case R.id.reader_test_stop:
                if (DBG) Log.d(TAG, "Reader STOP");
                runStopTest();
                break;
            default:
                Log.d(TAG, "ghost button");
                break;
        }
    }


    @Override
    public void onDetectCard(){
        Message msg = mHandler.obtainMessage();
        msg.what = ReaderTests.MSG_DETECT_CARD;
        msg.obj = null;
        mHandler.sendMessage(msg);
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
                mProgressDialog = ProgressDialog.show(ReaderTests.this, "Disable Nfc", "Please wait ...", true);
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

    //@Override
    //public void onSaveInstanceState(Bundle outState) {
    //    super.onSaveInstanceState(outState);
    //}

    private void initUI() {
        //Button
        mStartButton = (Button) findViewById(R.id.reader_test_start);
        mStartButton.setOnClickListener(this);
        mStopButton = (Button) findViewById(R.id.reader_test_stop);
        mStopButton.setOnClickListener(this);
        //Radio Group
        //Type
        mRadioGpType = (RadioGroup) findViewById(R.id.reader_type_group);
        mTypeRadioItems.add((RadioButton) findViewById(R.id.reader_type_group_item1));
        mTypeRadioItems.add((RadioButton) findViewById(R.id.reader_type_group_item2));
        mTypeRadioItems.add((RadioButton) findViewById(R.id.reader_type_group_item3));
        mTypeRadioItems.add((RadioButton) findViewById(R.id.reader_type_group_item4));
        mTypeRadioItems.add((RadioButton) findViewById(R.id.reader_type_group_item5));
        mTypeRadioItems.add((RadioButton) findViewById(R.id.reader_type_group_item6));
        mRadioGpType
            .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    mSelectionTypeId = getSelectionId(checkedId);
                }

                private int getSelectionId(int radioId) {
                    final int[] idxs =
                        { R.id.reader_type_group_item1, R.id.reader_type_group_item2,
                            R.id.reader_type_group_item3, R.id.reader_type_group_item4,
                            R.id.reader_type_group_item5, R.id.reader_type_group_item6};
                    final int[] ids =
                        { Utility.TYPE_ID_FELICA, Utility.TYPE_ID_14443A,
                          Utility.TYPE_ID_TYPE1, Utility.TYPE_ID_15693, Utility.TYPE_ID_P2P_I, Utility.TYPE_ID_P2P_T};

                    for (int i = 0; i < idxs.length; i++) {
                        if (idxs[i] == radioId) {
                            return ids[i];
                        }
                    }
                    Log.e(TAG, "Ghost RadioGroup checkId " + radioId);
                    return Utility.TYPE_ID_FELICA;
                }
            });
        //Command
        mRadioGpCommand = (RadioGroup) findViewById(R.id.reader_command_group);
        mCommandRadioItems.add((RadioButton) findViewById(R.id.reader_type_group_item1));
        mCommandRadioItems.add((RadioButton) findViewById(R.id.reader_type_group_item2));
        mRadioGpCommand
            .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    mSelectionCommandId = getSelectionId(checkedId);
                    if (DBG) Log.d(TAG, "mRadioGpCommand checked Id " + mSelectionCommandId);
                    addAppendix(mSelectionCommandId);
                }

                private int getSelectionId(int radioId) {
                    final int[] idxs =
                        { R.id.reader_command_group_item1, R.id.reader_command_group_item2 };
                    final int[] ids =
                        { COMMAND_ID_POLLING_LOOP, COMMAND_ID_SINGLE_POLLING };

                    for (int i = 0; i < idxs.length; i++) {
                        if (idxs[i] == radioId) {
                            return ids[i];
                        }
                    }
                    Log.e(TAG, "Ghost RadioGroup checkId " + radioId);
                    return COMMAND_ID_SINGLE_POLLING;
                }
            });
        //view
        mSinglePollingTimeView = (View) findViewById(R.id.single_polling_times_view);
        //EditText
        mTimesEdit = (EditText) findViewById(R.id.single_polling_times_edit);
        //Text
        mResultText = (TextView) findViewById(R.id.single_polling_result);
        
        //default settings
        mRadioGpType.check(R.id.reader_type_group_item1);
        mRadioGpCommand.check(R.id.reader_command_group_item2);
        mStopButton.setEnabled(false);        
    }


    private void addAppendix(int selId) {
        if (selId == COMMAND_ID_SINGLE_POLLING) {
           mSinglePollingTimeView.setVisibility(View.VISIBLE); 
        } else if (selId == COMMAND_ID_POLLING_LOOP) {
           mSinglePollingTimeView.setVisibility(View.GONE); 
        }
    }


    private void cleanResultText() {
        mResultText.setText("");
    }

    
    private void setInProgress(boolean inProgress) {
        mStartButton.setEnabled(!inProgress);
        mStopButton.setEnabled(inProgress);
    }

    
    private void runStartTest() {
        cleanResultText();
        if (mSelectionCommandId == COMMAND_ID_SINGLE_POLLING) { //get times
            String times = mTimesEdit.getText().toString();
            if (times == null || times.equals("")) {
                mTimes = 1;
            } else {
                mTimes = Integer.valueOf(times);
            } 
        }
        if (DBG) Log.d(TAG, "[Start]: Type:" + mSelectionTypeId + ", Command :" + mSelectionCommandId + ", Times:" + mTimes);
        switch(mSelectionCommandId) {
            case COMMAND_ID_POLLING_LOOP:
                setInProgress(true);
                new StartPollingLoopTask().execute(mTimes);
                break;   
                
            case COMMAND_ID_SINGLE_POLLING:
                new StartSinglePollingTask().execute(mTimes);
                break;
                
            default :
                Log.d(TAG, "ghost command.");
                break;
        }
    }


    private void runStopTest() {
        if (DBG) Log.d(TAG, "[Stop] :");
        new StopPollingLoopTask().execute(0);
    }


    class StartSinglePollingTask extends AsyncTask<Integer,String,Integer> {
        private ProgressDialog mProgressDialog;
                
        @Override
        protected Integer doInBackground(Integer... count) {
            
            String [] progressArray = new String [4];
            Integer successCounts = new Integer(0);
            mUIDList.clear();
            int errorCount = 0;
            int result = mNfcManager.initialize();
            if (result > 0) {
                if (DBG) Log.d(TAG, "init success. result fd = " + result);
            } else {
                if (DBG) Log.d(TAG, "init fail.");
            }
            if (DBG) Log.d(TAG, "count = " + count[0]);
            progressArray[0] = count[0].toString(); //total
            startTime = System.currentTimeMillis();
            for(int i = 0; i < count[0]; i++) {
                try {
                    result = mNfcManager.singlePolling(mSelectionTypeId);
                    progressArray[1] = new Integer(i).toString(); //current
                    if (result < 0) {
                        if (DBG) Log.d(TAG, "SinglePolling Failed");
                        errorCount++;
                        if (errorCount > 10) {
                            //reset
                            mNfcManager.deinitialize();
                            mNfcManager.initialize();
                            errorCount = 0;
                        }
                    } else {
                        Log.d(TAG, "!!" + mNfcManager.readUidSuccess());
                        if (mNfcManager.readUidSuccess()) {
                            if (DBG) Log.d(TAG, "readUidSuccess");
                            successCounts++;
                            progressArray[2] = successCounts.toString(); //success
                            byte[] uid = mNfcManager.getUid();
                            String uidString = Utility.binToHex(uid, result);
                            progressArray[3]= uidString;
                            if (!mUIDList.contains(uidString)) {
                                mUIDList.add(uidString);
                            }
                            if (mSelectionTypeId == Utility.TYPE_ID_P2P_I) {
                                try {Thread.sleep(50);} catch (InterruptedException e) {}
                            }
                        }
                    }
                    publishProgress(progressArray);	 //update
                }catch(Exception e) {
                    Log.d(TAG, "" + e);
                }
			}
			spentTime = System.currentTimeMillis() - startTime;
            return successCounts;
        }
        
        @Override
        protected void onPreExecute(){
            mReceiveString = "";
            mProgressDialog = ProgressDialog.show(ReaderTests.this, "StartSinglePollingTask", "Please wait ...", true);
            //mProgressDialog.setCancelable(true);
            mProgressDialog.show();
        }
        @Override
        protected void onProgressUpdate(String... progress) {
            mProgressDialog.setMessage("Total = " + progress[0] + "\n" + 
                                       "Current = " + progress[1] + "\n" +
                                       "Success = " + progress[2] + "\n" +
                                       "UID = " + progress[3]);
        }       
        @Override
        protected void onPostExecute(Integer counts) {
            if(mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            if (mSelectionTypeId != Utility.TYPE_ID_P2P_I && mSelectionTypeId != Utility.TYPE_ID_P2P_T) {
                mResultText.setText("Total: " + mTimes + "\n" +
                                    "Success: " + counts+ "\n" +
                                    "Fail: " + (mTimes - counts) + "\n" +
                                    "-----UID List----\n" +
                                    mUIDList.toString() + "\n" +
                                    "-----------------\n" +
                                    Utility.formatTime(spentTime));
            } else {
                 mResultText.setText("Total: " + mTimes + "\n" +
                                    "Success: " + counts+ "\n" +
                                    "Fail: " + (mTimes - counts) + "\n" +
                                    "-----ATR List----\n" +
                                    mUIDList.toString() + "\n" +
                                    "-----------------\n" +
                                    Utility.formatTime(spentTime));           
            }
            mUIDList.clear();
            int result = mNfcManager.deinitialize();
        }           
    }


    class StartPollingLoopTask extends AsyncTask<Integer,String,Integer> {
        private ProgressDialog mProgressDialog;
                
        @Override
        protected Integer doInBackground(Integer... count) {
            
            Integer successCounts = new Integer(0);

            int result = mNfcManager.initialize();
            if (result > 0) {
                if (DBG) Log.d(TAG, "init success. result fd = " + result);
            } else {
                if (DBG) Log.d(TAG, "init fail.");
            }
            result = mNfcManager.pollingLoop( mSelectionTypeId, ReaderTests.this);
            return successCounts;
        }
        
        @Override
        protected void onPreExecute(){
            mReceiveString = "";
            mProgressDialog = ProgressDialog.show(ReaderTests.this, "StartPollingLoopTask", "Please wait ...", true);
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
        }           
    }


    class StopPollingLoopTask extends AsyncTask<Integer,String,Integer> {
        private ProgressDialog mProgressDialog;
                
        @Override
        protected Integer doInBackground(Integer... count) {
            
            Integer successCounts = new Integer(0);
            int result = mNfcManager.stopPollingLoop();
            return successCounts;
        }
        
        @Override
        protected void onPreExecute(){
            mReceiveString = "";
            mProgressDialog = ProgressDialog.show(ReaderTests.this, "StopPollingLoopTask", "Please wait ...", true);
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
            int result = mNfcManager.deinitialize();
            setInProgress(false);
        }           
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
            mReceiveString = "";
            mProgressDialog = ProgressDialog.show(ReaderTests.this, "Write SWP Req ", "Please wait ...", true);
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

