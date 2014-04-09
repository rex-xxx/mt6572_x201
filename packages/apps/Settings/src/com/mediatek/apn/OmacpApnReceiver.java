package com.mediatek.apn;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;

import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.net.Uri;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Telephony;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;

import com.mediatek.settings.ext.IReplaceApnProfileExt;
import com.android.settings.R;
import com.android.settings.Utils;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * @author mtk54093 , start an intent service to process omacp apn install operation
 * 
 */
public class OmacpApnReceiver extends BroadcastReceiver {
    private static final String TAG = "OmacpApnReceiver";
    //action
    private static final String ACTION_OMACP = "com.mediatek.omacp.settings";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        
        String action = intent.getAction();
        Xlog.d(TAG, "get action = " + action);
        
        if (context.getContentResolver() == null) {
            Xlog.e(TAG, "FAILURE unable to get content resolver..");
            return;
        }
        
        if (ACTION_OMACP.equals(action)) {
            OmacpApnReceiverService.processBroadcastIntent(context, intent);
      }
    }

   
}
