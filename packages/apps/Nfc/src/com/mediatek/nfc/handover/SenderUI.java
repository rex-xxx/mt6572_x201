package com.mediatek.nfc.handover;

import android.content.Context;
import android.util.Log;

import com.mediatek.nfc.handover.FileTransfer.IClient;
import com.mediatek.nfc.handover.FileTransfer.ISenderUI;
import com.mediatek.nfc.handover.ui.BeamPlusUi;

import android.app.PendingIntent;
import android.content.Intent;

/**
 * Sender UI
 * 
 */
public class SenderUI extends BeamPlusUi implements ISenderUI {

	/**
	 * Send UI constructor
	 * 
	 * @param context
	 * @param ISender
	 */
	public SenderUI(Context context, IClient ISender) {
		super(context);
	}

	// TAG
	private static final String TAG = "SenderUI";

	/**
	 * On Progress update
	 */
	@Override
	public void onProgressUpdate(int id, String filename, int progress,
			int total, int position) {

		Log.d(TAG, "onProgressUpdate(), ID = " + id + ", total = "
				+ total + ",posistion = " + position + "," + progress + "%");

		updateProgressUI(id, "Sending: " + filename, progress);
	}

	/**
	 * On a beaming start to transmit
	 */
	public void onPrepared(int id, int total) {
		
		Log.d(TAG, "onPrepared(), ID = " + id + ", total = " + total);
		Intent cancelIntent = new Intent(ISenderUI.BROADCAST_CANCEL_BY_USER);
		cancelIntent.putExtra(ISenderUI.EXTRA_ID, id);
		/**
		 *	Notice:
		 * 	We should use request_code (2nd parameter for PendingIntenet.getBroadcast(),
		 *	otherwise the PendingIntent will be the same instance (see the SDK descriptoin)
		 */
		prepareNotification(id, "Outgoing beam sending", "Cancel", PendingIntent.getBroadcast(mContext, id, cancelIntent, 0), true);
	}

	/**
	 * On Transmit error
	 */
	public void onError(int id, int total, int success) {
		Log.d(TAG, "onError(), ID = " + id + ", total = "
				+ total + ",success = " + success);

		completeNotification(id, success + "/" + total + " files are successfully transmited", null);
	}

	/**
	 * On file transmit complete
	 */
	public void onCompleted(int id, int total, int success) {
		Log.d(TAG, "onComplete, ID = " + id + ", total = " + total
				+ ",success = " + success);
		
		completeNotification(id, success + " files are successfully transmited", null);
	}

	public void onCaneceled(int id, int total, int success) {
		Log.d(TAG, "onCanecel, ID = " + id + ", total = " + total
				+ ",success = " + success);
				
		completeNotification(id, success + "/" + total + " files are successfully transmited", null);
	}

}// end of SenderUI

