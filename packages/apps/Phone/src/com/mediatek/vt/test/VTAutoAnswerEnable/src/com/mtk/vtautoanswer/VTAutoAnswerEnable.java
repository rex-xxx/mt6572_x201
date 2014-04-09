package com.mtk.vtautoanswer;

import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;

public class VTAutoAnswerEnable extends Activity {
	private static final String TAG = "VTAutoAnswerEnable";
	private static final String SHREDPRE_NAME = "engineermode_vt_preferences";
	private static final String AUTO_ANSWER_FLAG = "auto_answer";
	private static final String AUTO_ANSWER_TIMER_FLAG = "auto_answer_time";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		try {
			final Context friendContext = createPackageContext("com.mediatek.engineermode", Context.CONTEXT_IGNORE_SECURITY);
			final SharedPreferences autoAnswerSh = friendContext.getSharedPreferences(SHREDPRE_NAME, 0);
		    final SharedPreferences.Editor editor = autoAnswerSh.edit();
		    
		    editor.putBoolean(AUTO_ANSWER_FLAG, true);
		    editor.putString(AUTO_ANSWER_TIMER_FLAG, "1000");
		    
		    editor.commit();
		} catch (Exception e) {
            Log.e(TAG, "Exception object: " + e);
		}
		
		Timer timer = new Timer();
		TimerTask finishTask = new CustomTimerTask(this);
		timer.schedule(finishTask, 2000);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	private class CustomTimerTask extends TimerTask {
		private Activity activity;
		
		public CustomTimerTask(Activity activity) {
			this.activity = activity;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			activity.finish();
		}
	}

}
