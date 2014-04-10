package com.mediatek.nfcdemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.util.Log;

import java.util.ArrayList;
import com.mediatek.nfcdemo.nfc.NativeNfcManager;

public class PollingInfo extends Activity implements OnClickListener {

    private static final String TAG = "NfcDemo";
    private static final boolean DBG = true;
    private Button mStartButton;
    private Button mStopButton;
    private TextView mResultText;
    private String mReceiveString;

    private NativeNfcManager mNfcManager;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.polling_info);
        initUI();
        mNfcManager = new NativeNfcManager();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.firmware_info_start:
                if (DBG) Log.d(TAG, "Reader START");
                runGetPollingInfo();
                break;
            case R.id.firmware_info_stop:
                if (DBG) Log.d(TAG, "Reader STOP");
               
                break;
            default:
                Log.d(TAG, "ghost button");
                break;
        }
    }

    private void initUI(){
         //Button
        mStartButton = (Button) findViewById(R.id.firmware_info_start);
        mStartButton.setOnClickListener(this);
        mStopButton = (Button) findViewById(R.id.firmware_info_stop);
        mStopButton.setOnClickListener(this);   
        //Text
        mResultText = (TextView) findViewById(R.id.polling_info_result);
        //default settings
        mStopButton.setEnabled(false);          
    }

    public void runGetPollingInfo(){
        if (DBG) Log.d(TAG, "runGetPollingInfo");
        new StartGetPollingInfoTask().execute();        
    }

    class StartGetPollingInfoTask extends AsyncTask<Integer,String,String> {
        private ProgressDialog mProgressDialog;
                
        @Override
        protected String doInBackground(Integer... count) {  
            String responseString = "";
            try {
                int length = mNfcManager.getPollingInfo();
                byte[] response = mNfcManager.getResponse();
                
                if (length > 0) {
                    responseString = Utility.parsePollingInfo( response, length);
                } else {
                    responseString = "Error Response , Try again.";
                }
            } catch (Exception e) {
                responseString = "" + e;
                Log.d(TAG, "" + e);
            }
            return responseString;
        }
        
        @Override
        protected void onPreExecute(){
            mReceiveString = "";
            mProgressDialog = ProgressDialog.show(PollingInfo.this, "Execute command", "Please wait ...", true);
            //mProgressDialog.setCancelable(true);
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
}



