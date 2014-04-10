
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

public class FieldDetectInfo extends Activity implements OnClickListener {

    private static final String TAG = "NfcDemo";
    private static final boolean DBG = true;
    private Button mStartButton;
    private Button mStopButton;
    private TextView mResultText;
    private String mReceiveString;
    private EditText mCountDownEdit;
    private int mPeriod;
    private Long mCountDownMicroSecond;

    private NativeNfcManager mNfcManager;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.field_detect_info);
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
            case R.id.field_detect_start:
                if (DBG) Log.d(TAG, "Field Detect START");
                runFieldDetectStart();
                break;
            case R.id.field_detect_stop:
                if (DBG) Log.d(TAG, "Field Detect STOP");
               runFieldDetectStop();
                break;
            default:
                Log.d(TAG, "ghost button");
                break;
        }
    }

    private void initUI(){
         //Button
        mStartButton = (Button) findViewById(R.id.field_detect_start);
        mStartButton.setOnClickListener(this);
        mStopButton = (Button) findViewById(R.id.field_detect_stop);
        mStopButton.setOnClickListener(this);
        //Text
        mResultText = (TextView) findViewById(R.id.field_detect_result);
        //EditText
        mCountDownEdit = (EditText)findViewById(R.id.countdown_edit);
        //default settings
        mStopButton.setEnabled(false);
    }

    public void runFieldDetectStart(){
        if (DBG) Log.d(TAG, "runFieldDetectStart");

        String period = mCountDownEdit.getText().toString();
        if (period == null || period.equals("")) {
            mPeriod = 1;
        } else {
            mPeriod = Integer.valueOf(period);
        } 

        mCountDownMicroSecond = Long.valueOf(mPeriod) * 60 * 1000 ;
        
        new StartFieldDetectInfoTask().execute();
    }

    public void runFieldDetectStop(){
        if (DBG) Log.d(TAG, "runFieldDetectStop");
    }


    class StartFieldDetectInfoTask extends AsyncTask<Integer,String,String> {
        private ProgressDialog mProgressDialog;
        Long startTime;
        Long spentTime;
        int card;
        int reader;
        int target;
        int cardFieldDetect;
        int readerFieldDetect;
        int targetFieldDetect;
        
        @Override
        protected String doInBackground(Integer... count) {
            String responseString = "";
            int length; 
            byte[] response; 
            int type;
            int fieldDetect;
            
            while(true){
                spentTime = System.currentTimeMillis() - startTime;
                if (spentTime > mCountDownMicroSecond) {
                    Log.d(TAG, "Timeout, break.");
                    break;
                }
                length = mNfcManager.getPollingInfo();
                response = mNfcManager.getResponse();
                responseString = "CountDown : " + (mCountDownMicroSecond - spentTime) + "(ms) \n";
                responseString += Utility.rawDataPollingInfo( response, length);
                if (length == Utility.POLLING_INFO_LENGTH_MAX) {                    
                    //responseString = Utility.parsePollingInfo( response, length);
                    type = Utility.parsePollingLoopStatusType( response);
                    fieldDetect = Utility.parsePollingLoopStatusField( response);
                    if (DBG) Log.d(TAG, "Type = " + type + ", Field Detect = " + fieldDetect);
                    
                    responseString += "---- PollingLoop Status ----\n";                    
                    switch(type) {
                        case Utility.POLLING_CATEGORY_CARD:
                            responseString += "Polling Type : Card \n";
                            card++;
                            if (fieldDetect == 0x01) {
                                cardFieldDetect++;
                                responseString += "Field : Yes \n";
                            } else {
                                responseString += "Field : None \n";
                            }

                            break;
                            
                        case Utility.POLLING_CATEGORY_READER:
                            responseString += "Polling Type : Reader \n";
                            reader++;
                            if (fieldDetect == 0x01) {
                                readerFieldDetect++;
                                responseString += "Field : Yes \n";
                            } else {
                                responseString += "Field : None \n";
                            }                            
                            break;
                            
                        case Utility.POLLING_CATEGORY_TARGET:
                            responseString += "Polling Type : Target \n";
                            target++;
                            if (fieldDetect == 0x01) {
                                targetFieldDetect++;
                                responseString += "Field : Yes \n";
                            } else {
                                responseString += "Field : None \n";
                            }                              
                            break;
                            
                        default:
                            Log.d(TAG, "Exception Type");
                            break;
                    }
                } else {
                    //exception result length
                    //responseString += Utility.rawDataPollingInfo( response, length);
                }
                publishProgress(responseString);	 //update
            }            
            return responseString;
        }
        
        @Override
        protected void onPreExecute(){
            mReceiveString = "";
            mProgressDialog = ProgressDialog.show(FieldDetectInfo.this, "Field Detect Test ...", "Please wait ...", true);
            //mProgressDialog.setCancelable(true);
            mProgressDialog.show();
            startTime = System.currentTimeMillis();  //get start time
            card = 0;
            reader = 0;
            target = 0;
            cardFieldDetect = 0;
            readerFieldDetect = 0;
            targetFieldDetect = 0;
        }
        
        @Override
        protected void onProgressUpdate(String... progress) {
            mProgressDialog.setMessage("" + progress[0]);        
        }  
        
        @Override
        protected void onPostExecute(String response) {
            if(mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            mResultText.setText("----- Statistics -----\n" +
                                "[Type], [Field]\n" +
                                "Card  :"+  card + ", " + cardFieldDetect + "\n" +
                                "Reader:"+  reader + ", " + readerFieldDetect + "\n" +
                                "Target:"+  target + ", " + targetFieldDetect + "\n" +
                                "Total:"+  (card + reader + target) + ", " + (cardFieldDetect + readerFieldDetect + targetFieldDetect) + "\n"
                                );
        }           
    }    
}


