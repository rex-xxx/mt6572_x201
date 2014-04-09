package com.hissage.pn;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.hissage.hpe.SDK;
import com.hissage.service.NmsService;
import com.hissage.util.log.NmsLog;
import com.hissage.util.preference.NmsPreferences;

public class HpnsApplication extends Application{
    
    public static Context mGlobalContext = null;
    
    public void onCreate(){
        super.onCreate();
        mGlobalContext = this;
        NmsPreferences.initPreferences(mGlobalContext);
        NmsLog.init(this);
		if(Config.PN){
			SDK.startService(this);
	        SDK.onRegister(this);
		}
    }
    
}
