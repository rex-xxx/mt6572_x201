package com.hissage.ui.activity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

import com.hissage.R;
import com.hissage.api.NmsStartActivityApi;
import com.hissage.api.NmsiSMSApi;
import com.hissage.message.ip.NmsIpLocationMessage;
import com.hissage.message.ip.NmsIpMessage;
import com.hissage.message.ip.NmsIpSessionMessage;
import com.hissage.message.ip.NmsHesineApiConsts.NmsImFlag;
import com.hissage.message.ip.NmsHesineApiConsts.NmsImReadMode;
import com.hissage.message.ip.NmsIpMessageConsts.NmsIpMessageType;
import com.hissage.ui.adapter.NmsGetAssetsResourceFileAdapter;
import com.hissage.util.data.NmsConsts;
import com.hissage.util.log.NmsLog;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class NmsTermActivity extends Activity implements OnClickListener {

    private static final String TAG = "NmsTermActivity";
    private final static int HANDLER_READ_FILE = 0;
   
    private long mSim_id = NmsConsts.INVALID_SIM_ID;
    private Button mReject = null;
    private Button mAgree = null;
    private TextView mMainTitle = null;
    private StringBuffer mStringBuffer = new StringBuffer();
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            NmsLog.trace(TAG, "handler received msg type: " + msg.what);
            switch (msg.what) {
            case HANDLER_READ_FILE:
            	mMainTitle.setText(mStringBuffer);
                return;
            default:
            	//do nothing
                break;
            }
        }

    };
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        switch (id) {
        case android.R.id.home: {
            finish();
            break;
        }
        }
        return super.onMenuItemSelected(featureId, item);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setLogo(R.drawable.isms);
        getActionBar().setTitle(R.string.STR_NMS_MENU_LICENSE_AGREEMENT);

        setContentView(R.layout.term);

        mMainTitle= (TextView) findViewById(R.id.detail_content);
       
        try{
        	getFileContentForSetText();
        }catch (Exception e) {
            NmsLog.error(TAG, NmsLog.nmsGetStactTrace(e));
        } finally {
            //
        }
        
        Intent i = getIntent();
        if (i == null
                || NmsConsts.INVALID_SIM_ID == (mSim_id = i.getLongExtra(NmsConsts.SIM_ID,
                        NmsConsts.INVALID_SIM_ID))) {
            NmsLog.error(TAG, "not find sim_id at intent: " + i);
            finish();
            return;
        }
        
        mAgree = (Button)findViewById(R.id.agree);
        mAgree.setOnClickListener(this);
        mReject = (Button)findViewById(R.id.reject);
        mReject.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if(v == mAgree){
            NmsStartActivityApi.nmsStartActivitionActivity(
                    this, mSim_id);
            finish();
        }else if(v == mReject){
            finish();
        }else{
            // do nothing
        }
    }
    
    private void getFileContentForSetText() {
        NmsLog.trace(TAG, "thread to get file content");

        new Thread() {
            @Override
            public void run() {
                super.run();
                NmsLog.trace(TAG, "handler send msg, msg type: " + HANDLER_READ_FILE);
                readFileContent();
            	mHandler.sendEmptyMessage(HANDLER_READ_FILE);
            	/*
                if(mMainTitle != null && mMainTitle.getWidth()>0 && mMainTitle.getHeight()>0){
                	readFileContent();
                	mHandler.sendEmptyMessage(HANDLER_READ_FILE);
                }else{
            		mHandler.postDelayed(this, 50);
    			}
    			*/
                return;
            }
        }.start();
    }
    
    private void readFileContent(){
    	try{
	        Locale locale = getResources().getConfiguration().locale;
	        String languageCode = locale.getLanguage();
	        String countryCode = locale.getCountry();
	        String languageTypeFileName = null;
	        if (languageCode.endsWith("zh") && countryCode.endsWith("CN")){ // zh_CN not include zh_TW.
	        	languageTypeFileName = "NMS_LICENSE_AGREEMENT_DETAIL_zh_CN.txt";
	        }else if(languageCode.endsWith("zh") && countryCode.endsWith("TW")){
	        	languageTypeFileName = "NMS_LICENSE_AGREEMENT_DETAIL_zh_TW.txt";
	        }else if(languageCode.endsWith("en")){
	        	languageTypeFileName = "NMS_LICENSE_AGREEMENT_DETAIL_en.txt";
	        }else{
	        	languageTypeFileName = "NMS_LICENSE_AGREEMENT_DETAIL_en.txt";
	        }
	        NmsGetAssetsResourceFileAdapter gLFile = new NmsGetAssetsResourceFileAdapter(getResources());
	        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gLFile.getFile(languageTypeFileName)));
            String res = null;
            while ((res = bufferedReader.readLine()) != null) {
            	mStringBuffer.append(res);
            	mStringBuffer.append("\n");
            }
	     }catch (Exception e) {
	            NmsLog.error(TAG, NmsLog.nmsGetStactTrace(e));
	     }finally {
	            //
	     }
    }
}
