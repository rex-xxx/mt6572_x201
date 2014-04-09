 package com.mediatek.cmmb.app;
 
 import android.app.Activity;
 import android.content.Intent; 
 import android.os.Bundle;    
 import android.util.Log;     
 
 
/**
 * M: BootCmmbActivity
 */
public class BootCmmbActivity extends Activity {
    private static final String TAG = "CMMB::BootCmmbActivity";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, " saveinstance" + savedInstanceState);
        if (savedInstanceState == null ) {
            this.finish();
            Intent intent = new Intent();
            intent.setClass(this, MainScreen.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(intent);
        }
        
    }
}