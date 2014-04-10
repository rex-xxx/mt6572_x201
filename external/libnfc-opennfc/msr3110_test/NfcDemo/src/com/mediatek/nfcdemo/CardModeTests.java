package com.mediatek.nfcdemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.BroadcastReceiver;
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
import com.mediatek.nfcdemo.R;


import java.util.ArrayList;
import com.mediatek.nfcdemo.nfc.NativeNfcManager;

public class CardModeTests extends Activity implements OnClickListener {

    private static final String TAG = "NfcDemo";
    private static final boolean DBG = true;
    private NativeNfcManager mNfcManager;
    
    private boolean mNfcEnabled = false;
    private Button mStartButton;
    private Button mStopButton;
    private Button mPollingInfoStart;
    private RadioGroup mRadioGpType;
    private ArrayList<RadioButton> mTypeRadioItems = new ArrayList<RadioButton>(); 
    private TextView mResultText;
    
    private int mSelectionTypeId;

    private ProgressDialog mProgressDialog;
    private IntentFilter mIntentFilter;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(action)) {
                handleNfcStateChanged(intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF));
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //init state
        mNfcManager = new NativeNfcManager();
        //new InitTask().execute();
        //init UI
        setContentView(R.layout.card_mode_tests);
        initUI();
    }

    @Override
    protected void onDestroy() {
        if (mNfcManager != null) {
            mNfcManager.deinitialize();
        }
        //enableNfc();    
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.card_mode_test_start:
                if (DBG) Log.d(TAG, "Card Mode START");
                runStartTest();
                break;
                
            case R.id.card_mode_test_stop:
                if (DBG) Log.d(TAG, "Card Mode STOP");
                runStopTest();
                break;

            case R.id.get_polling_info:
                if (DBG) Log.d(TAG, "Card Mode Get Polling Info");
                runGetPollingInfo();
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
                mProgressDialog = ProgressDialog.show(CardModeTests.this, "Disable Nfc", "Please wait ...", true);
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

    private void initUI() {
        //Button
        mStartButton = (Button) findViewById(R.id.card_mode_test_start);
        mStartButton.setOnClickListener(this);
        mStopButton = (Button) findViewById(R.id.card_mode_test_stop);
        mStopButton.setOnClickListener(this);
        mPollingInfoStart = (Button) findViewById(R.id.get_polling_info);
        mPollingInfoStart.setOnClickListener(this);
        
        //Radio Group
        //Card Type
        mRadioGpType = (RadioGroup) findViewById(R.id.card_mode_type_group);
        mTypeRadioItems.add((RadioButton) findViewById(R.id.card_mode_type_group_item1));
        mTypeRadioItems.add((RadioButton) findViewById(R.id.card_mode_type_group_item2));
        mTypeRadioItems.add((RadioButton) findViewById(R.id.card_mode_type_group_item3));
        mRadioGpType
            .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    mSelectionTypeId = getSelectionId(checkedId);
                }

                private int getSelectionId(int radioId) {
                    final int[] idxs =
                        { R.id.card_mode_type_group_item1,
                          R.id.card_mode_type_group_item2,
                          R.id.card_mode_type_group_item3};
                    final int[] ids =
                        { Utility.CARD_TYPE_A,
                          Utility.CARD_TYPE_B,
                          Utility.CARD_TYPE_AB};

                    for (int i = 0; i < idxs.length; i++) {
                        if (idxs[i] == radioId) {
                            return ids[i];
                        }
                    }
                    Log.e(TAG, "Ghost RadioGroup checkId " + radioId);
                    return Utility.CARD_TYPE_A;
                }
            });

        //Text
        mResultText = (TextView) findViewById(R.id.card_mode_result);
        
        //default settings
        mRadioGpType.check(R.id.card_mode_type_group_item1);
        mStopButton.setEnabled(false);
        mTypeRadioItems.get(2).setEnabled(false);
        
    }

    private void runStartTest() {
        if (DBG) Log.d(TAG, "[Start] : type = " + mSelectionTypeId);
        setInProgress(true);
        new StartCardModeTask().execute(mSelectionTypeId);         
    }

    private void runStopTest() {
        if (DBG) Log.d(TAG, "[Stop] :");
        new StopCardModeTask().execute(0);      
    }

    private void runGetPollingInfo() {
        if (DBG) Log.d(TAG, "Get Polling Info.");
        new StartGetPollingInfoTask().execute(0);
    }
    
    private void setInProgress(boolean inProgress) {
        mStartButton.setEnabled(!inProgress);
        mStopButton.setEnabled(inProgress);
    }


    class StartCardModeTask extends AsyncTask<Integer,String,Integer> {
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
            result = mNfcManager.cardMode( mSelectionTypeId);
            return successCounts;
        }
        
        @Override
        protected void onPreExecute(){
            mProgressDialog = ProgressDialog.show(CardModeTests.this, "StartCardModeTask", "Please wait ...", true);
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


    class StopCardModeTask extends AsyncTask<Integer,String,Integer> {
        private ProgressDialog mProgressDialog;
                
        @Override
        protected Integer doInBackground(Integer... count) {
            
            Integer successCounts = new Integer(0);
            //int result = mNfcManager.stopCardMode();
            int result = mNfcManager.deinitialize();
            return successCounts;
        }
        
        @Override
        protected void onPreExecute(){
            mProgressDialog = ProgressDialog.show(CardModeTests.this, "StopCardModeTask", "Please wait ...", true);
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
            //int result = mNfcManager.deinitialize();
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
            mProgressDialog = ProgressDialog.show(CardModeTests.this, "Write SWP Req ", "Please wait ...", true);
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


    class StartGetPollingInfoTask extends AsyncTask<Integer,String,String> {
        private ProgressDialog mProgressDialog;
                
        @Override
        protected String doInBackground(Integer... count) {
        
            int length = mNfcManager.getPollingInfo();
            byte[] response = mNfcManager.getResponse();
            String responseString = "";
            if (length > 0) {
                responseString = Utility.parsePollingInfo( response, length);
            } else {
                responseString = "Error Response , Try again.";
            }
            return responseString;            
        }
        
        @Override
        protected void onPreExecute(){
            mProgressDialog = ProgressDialog.show( CardModeTests.this, "Get Polling Info.", "Please wait ...", true);
            mProgressDialog.show();
        }
        @Override
        protected void onProgressUpdate(String... progress) {
        }  
        
        @Override
        protected void onPostExecute(String response) {
            if(mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            mResultText.setText("" + response);
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



