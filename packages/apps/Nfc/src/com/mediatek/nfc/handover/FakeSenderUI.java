package com.mediatek.nfc.handover;

import android.content.Context;
import android.util.Log;

import com.mediatek.nfc.handover.FileTransfer.IClient;
import com.mediatek.nfc.handover.FileTransfer.ISenderUI;
import com.mediatek.nfc.handover.ui.BeamPlusUi;

import android.app.PendingIntent;
import android.content.Intent;

public class FakeSenderUI extends BeamPlusUi implements ISenderUI {

	public FakeSenderUI(Context context, IClient ISender) {
		super(context);
	}

	// TAG
	private static final String TAG = "FakeSenderUI";

	@Override
	public void onProgressUpdate(int id, String filename, int progress,
			int total, int position) {
		Log.d(TAG, "onProgressUpdate(), ID = " + id + ", total = "
				+ total + ",posistion = " + position + "," + progress + "%");
	}

	public void onPrepared(int id, int total) {		
		Log.d(TAG, "onPrepared(), ID = " + id + ", total = " + total);
	}

	public void onError(int id, int total, int success) {
		Log.d(TAG, "onError(), ID = " + id + ", total = "
				+ total + ",success = " + success);
	}

	public void onCompleted(int id, int total, int success) {
		Log.d(TAG, "onComplete, ID = " + id + ", total = " + total
				+ ",success = " + success);
	}

	public void onCaneceled(int id, int total, int success) {
		Log.d(TAG, "onCanecel, ID = " + id + ", total = " + total
				+ ",success = " + success);
	}

}