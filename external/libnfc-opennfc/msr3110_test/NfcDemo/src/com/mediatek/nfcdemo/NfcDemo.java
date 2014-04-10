package com.mediatek.nfcdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.util.Log;


public class NfcDemo extends Activity implements OnClickListener {
    
    private static final String TAG = "NfcDemo";
    private static final boolean DBG = true;   

    private Button mNfcTestsButton;
    private Button mFirmwareInfoButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nfcdemo);
        initUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }    

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.nfc_tests:
                if (DBG) Log.d(TAG, "run nfc tests");
                runNfcTests();
                break;

            case R.id.firmware_info:
                if (DBG) Log.d(TAG, "run firmware info");
                runFirmwareInfo();
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

    private void initUI() {
        mNfcTestsButton = (Button) findViewById(R.id.nfc_tests);
        mNfcTestsButton.setOnClickListener(this);
        mFirmwareInfoButton = (Button) findViewById(R.id.firmware_info);
        mFirmwareInfoButton.setOnClickListener(this);      
    }

    private void runNfcTests() {
        if (DBG) Log.d(TAG, "runNfcTests");
        Intent intent = new Intent();
        intent.setClass(NfcDemo.this, NfcTests.class);
        startActivity(intent); 
    }

    private void runFirmwareInfo() {
        if (DBG) Log.d(TAG, "runFirmwareInfo");
        Intent intent = new Intent();
        intent.setClass(NfcDemo.this, FirmwareInfo.class);
        startActivity(intent);
    }
   
}
