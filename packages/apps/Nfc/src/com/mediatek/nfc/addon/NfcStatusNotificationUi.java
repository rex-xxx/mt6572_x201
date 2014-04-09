package com.mediatek.nfc.addon;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.nfc.R;

import com.mediatek.nfc.addon.MtkNfcAddonSequence;

public class NfcStatusNotificationUi {
	private static final String TAG = "NfcStatusNotificationUi";
	
	private static final int UNIQUE_NOTIF_ID = 13;
	private Context mContext;
	private NotificationManager mNotificationManager;
	
	private NfcStatusNotificationUi(Context context) {
		mContext = context;
		mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	private static NfcStatusNotificationUi mSingleton;

	public static void createSingleton(Context context) {
		if (mSingleton == null) {
			mSingleton = new NfcStatusNotificationUi(context);
		}
	}
	
	public static NfcStatusNotificationUi getInstance() {
		return mSingleton;
	}
	
	public void showNotification() {
		String msg = "";
		String title = "";
		title = mContext.getString(R.string.nfcUserLabel);
		msg = mContext.getString(R.string.mtk_notification_string);
		PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, new Intent("android.settings.NFC_SETTINGS"), 0);
		Notification n = createNotification(title, msg, pendingIntent);
        n.flags |= Notification.FLAG_NO_CLEAR;//NFC status bar is persistent
		mNotificationManager.notify(UNIQUE_NOTIF_ID, n);
	}
	
	public void hideNotification() {
		mNotificationManager.cancel(UNIQUE_NOTIF_ID);
	}
	
	private Notification createNotification(CharSequence title,
			CharSequence infoWord, PendingIntent intent) {

		return new Notification.Builder(mContext)
				.setContentTitle(title)
				.setContentText(infoWord)
				.setSmallIcon(R.drawable.nfc)
				//.addAction(android.R.drawable.ic_delete, cancelWord, intent)
				.setContentIntent(intent)
				//.setProgress(0, 0, true)
				//.setPriority(100)
				.build();
	}
}
