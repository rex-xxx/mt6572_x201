
package com.mediatek.nfcdemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Button;
import android.util.Log;
import android.nfc.NfcAdapter;
import com.mediatek.nfcdemo.nfc.NativeNfcManager;

public class NfcTests extends Activity implements OnClickListener {
    
    private static final String TAG = "NfcDemo";
    private static final boolean DBG = true;   
    private boolean mNfcEnabled = false;
    private Button mReaderButton;
    private Button mCardModeButton;
    private Button mP2PTestButton;

    private ProgressDialog mProgressDialog;
    private IntentFilter mIntentFilter;
    private NativeNfcManager mNfcManager;
    
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
        new InitTask().execute();
        // init UI
        setContentView(R.layout.nfc_tests);
        initUI();         
    }

    @Override
    protected void onDestroy() {
        if (mNfcManager != null) {
            mNfcManager.deinitialize();
        }
        enableNfc();
        super.onDestroy();        
    }    

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.reader:
                if (DBG) Log.d(TAG, "run reader tests");
                runReaderTests();
                break;

            case R.id.card_mode:
                if (DBG) Log.d(TAG, "run card mode tests");
                runCardModeTests();
                break;

            case R.id.p2p:
                if (DBG) Log.d(TAG, "run p2p test");
                runP2PTest();
                break;

            default:
                Log.d(TAG, "ghost button.");
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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
                mProgressDialog = ProgressDialog.show(NfcTests.this, "Disable Nfc", "Please wait ...", true);
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
        mReaderButton = (Button) findViewById(R.id.reader);
        mReaderButton.setOnClickListener(this);
        mCardModeButton = (Button) findViewById(R.id.card_mode);
        mCardModeButton.setOnClickListener(this); 
        mP2PTestButton = (Button) findViewById(R.id.p2p);
        mP2PTestButton.setOnClickListener(this);         
    }

    private void runReaderTests() {
        if (DBG) Log.d(TAG, "runReaderTests");
        Intent intent = new Intent();
        intent.setClass(NfcTests.this, ReaderTests.class);
        startActivity(intent); 
    }

    private void runCardModeTests() {
        if (DBG) Log.d(TAG, "runCardModeTests");
        Intent intent = new Intent();
        intent.setClass(NfcTests.this, CardModeTests.class);
        startActivity(intent);         
    }

    private void runP2PTest() {
        if (DBG) Log.d(TAG, "runP2PTest");
        Intent intent = new Intent();
        intent.setClass(NfcTests.this, P2PTests.class);
        startActivity(intent);         
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
            mProgressDialog = ProgressDialog.show(NfcTests.this, "Write SWP Req ", "Please wait ...", true);
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



