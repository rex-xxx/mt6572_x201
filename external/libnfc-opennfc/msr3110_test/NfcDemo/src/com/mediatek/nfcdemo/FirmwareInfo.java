package com.mediatek.nfcdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.util.Log;

public class FirmwareInfo extends Activity implements OnClickListener {
    
    private static final String TAG = "NfcDemo";
    private static final boolean DBG = true;
    private Button mPollingInfoButton;
    private Button mFieldDetectInfoButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.firmware_info);
        initUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }    

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.polling_info:
                if (DBG) Log.d(TAG, "run polling info");
                runPollingInfo();
                break;

            case R.id.field_detect_info:
                 if (DBG) Log.d(TAG, "run field detect info");
                runFieldDetectInfo();               
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
        mPollingInfoButton = (Button) findViewById(R.id.polling_info);
        mPollingInfoButton.setOnClickListener(this); 
        mFieldDetectInfoButton = (Button) findViewById(R.id.field_detect_info);
        mFieldDetectInfoButton.setOnClickListener(this);        
    }

    private void runPollingInfo() {
        if (DBG) Log.d(TAG, "runPollingInfo");
        Intent intent = new Intent();
        intent.setClass(FirmwareInfo.this, PollingInfo.class);
        startActivity(intent);
    }

    private void runFieldDetectInfo() {
        if (DBG) Log.d(TAG, "runFieldDetectInfo");
        Intent intent = new Intent();
        intent.setClass(FirmwareInfo.this, FieldDetectInfo.class);
        startActivity(intent);        
    }
    
}

