package com.mediatek.vt.loopback;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.graphics.PixelFormat;
import android.view.Window;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewStub;
import android.content.Intent;
import android.test.AndroidTestCase;

import com.android.phone.Constants;
import com.android.phone.PhoneGlobals;

import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;
import com.mediatek.vt.loopback.VTInCallScreenLoopback;
import com.mediatek.vt.VTManager;
import com.mediatek.vt.VTManager.State;
import com.mediatek.settings.VTSettingUtils;

import com.android.phone.R;

public class VTLoopbackActivity extends Activity {

	private static final String LOG_TAG = "VTLoopbackActivity";
	private VTInCallScreenLoopback mVTInCallScreenLoopback;
    
	void log(String msg) {
		Log.w(LOG_TAG, msg);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    log("VTLoopbackActivity:onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vt_loopback);

		mVTInCallScreenLoopback = new VTInCallScreenLoopback(this);
		
        ViewStub stub = (ViewStub) this.findViewById(R.id.vtInCallScreenStubLoopback);
        stub.inflate();
		mVTInCallScreenLoopback = (VTInCallScreenLoopback) this.findViewById(R.id.VTInCallCanvas);
        mVTInCallScreenLoopback.setVTLoopBackInstance(this);
        mVTInCallScreenLoopback.initVTInCallScreen();

        // 0, simId
        VTSettingUtils.getInstance().updateVTSettingState(0);
        VTSettingUtils.getInstance().updateVTEngineerModeValues();        
        
        mVTInCallScreenLoopback.internalAnswerVTCallPre();
        mVTInCallScreenLoopback.setVTScreenMode(Constants.VTScreenMode.VT_SCREEN_OPEN);

	}

	@Override
    public void onStop(){
        super.onStop();
        log ("VTLoopbackActivity:onStop");        
        mVTInCallScreenLoopback.onStop();
        finish();
    }
    
    
    public void onDestroy() {
		log("VTLoopbackActivity:onDestroy()");
		super.onDestroy();

        if (FeatureOption.MTK_VT3G324M_SUPPORT == true) {
            mVTInCallScreenLoopback.dismissVTDialogs();
            mVTInCallScreenLoopback.onDestroy();
        }
	}

	public void onResume(){
        super.onResume();
        log ("VTLoopbackActivity:onResume");
		if (FeatureOption.MTK_VT3G324M_SUPPORT == true) {
			mVTInCallScreenLoopback.updateVTScreen(mVTInCallScreenLoopback.getVTScreenMode());
			mVTInCallScreenLoopback.dismissVTDialogs();
		}
	}
}
