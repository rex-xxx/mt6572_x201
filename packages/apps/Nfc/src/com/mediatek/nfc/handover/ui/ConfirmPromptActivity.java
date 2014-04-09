package com.mediatek.nfc.handover.ui;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import com.android.nfc.R;

public class ConfirmPromptActivity extends Activity {
	private static final String TAG = "BeamPlusCancelPrompt";
	
	public static final String EXTRA_PENDINTINTENT = "com.mediatek.nfc.handover.prompt.extra.pintent";
	
	private PendingIntent mPendingIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mPendingIntent = intent.getParcelableExtra(EXTRA_PENDINTINTENT);
    	new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(getString(R.string.mtk_beam_cancel_title))
        .setMessage(getString(R.string.mtk_beam_cancel))
        .setPositiveButton(getString(R.string.mtk_yes), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) {
	        	try {
	        		Log.d(TAG, "fire pending intent");
					mPendingIntent.send();
				} catch (CanceledException e) {
					Log.d(TAG, "wtf! send exception!!");
					e.printStackTrace();
				}
	        	finish();
	        }	
	    })
	    .setNegativeButton(getString(R.string.mtk_no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Log.d(TAG, "don't fire pending intent");
				finish();
			}
		})
	    .show();
    }
}
