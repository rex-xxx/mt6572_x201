package com.mediatek.nfc.handover.ui;

import java.util.HashMap;
import java.util.Hashtable;

import com.mediatek.nfc.handover.FileTransfer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;
import android.view.View;
import java.util.HashMap;
import com.android.nfc.R;

public abstract class BeamPlusUi {

	/**
	 * Task Notification
	 * 
	 */
	private class TaskNotification {
		Notification beaming;
		Notification progress;
        PendingIntent cancelIntent;
		boolean isUpload;
	}

	public BeamPlusUi(Context context) {
		mContext = context;
		mManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	// Android Context
	protected Context mContext;
	// Notification Manager
	private NotificationManager mManager;
	// Notification
	private HashMap<Integer, TaskNotification> mNotificationMap = new HashMap<Integer, TaskNotification>(); 
	//private TaskNotification mNotification;

	/**
	 * Create a set of notification
	 * 
	 * @param id
	 */
	protected void prepareNotification(int id, CharSequence title,
			CharSequence cancelWord, PendingIntent intent, boolean isUpload) {

		TaskNotification noti = new TaskNotification();
		noti.beaming = createBeamNotification(title, cancelWord, intent, isUpload);
		noti.progress = createProressNotification("", isUpload, false, intent);
		noti.isUpload = isUpload;
        noti.cancelIntent = intent;
		
		//mNotification = noti;
		mNotificationMap.put(id, noti);
		mManager.notify(id * 2, noti.beaming);
	}

	/**
	 * Update progress
	 * 
	 * @param id
	 * @param title
	 * @param progress
	 */
	protected void updateProgressUI(int id, CharSequence title, int progress) {

		TaskNotification noti = mNotificationMap.get(id);
		if (noti != null) {
        
            Notification n = new Notification.Builder(mContext)
                    .setContentTitle(mContext.getString(R.string.beam_progress))
                    .setContentText("[" + progress + "%] " + title)
					.setSmallIcon(android.R.drawable.stat_sys_download)
					.setDeleteIntent(noti.cancelIntent)
					.addAction(R.drawable.ic_menu_cancel_holo_dark, mContext.getString(R.string.cancel), noti.cancelIntent)
                    .setProgress(0, 0, true).setPriority(100).build();

			mManager.notify(id * 2, n);

            /*
			noti.progress.contentView
					.setTextViewText(android.R.id.title, title);

			noti.progress.contentView.setTextViewText(
					com.android.internal.R.id.info, progress + "%");

			noti.progress.contentView.setProgressBar(android.R.id.progress,
					FileTransfer.MAX_PROGRESS_VALUE, progress, false);

			mManager.notify(id * 2 + 1, noti.progress);
			if (progress > 0) {
				mManager.cancel(id * 2);
			}
            */

		} else {
			Log.d("Beam+",
					"[BeamPlusUi] updateProgress(), No matched notification items");
		}
	}

	/**
	 * Complete notification
	 * 
	 * @param id
	 * @param completeWord
	 * @param isUpload
	 */
	protected void completeNotification(int id, CharSequence completeWord, PendingIntent pendingIntent) {

		TaskNotification noti = mNotificationMap.get(id);
		if (noti != null) {

			Notification complete = createProressNotification(completeWord,
					noti.isUpload, true, pendingIntent);

			mManager.notify(id * 2 + 1, complete);
			mManager.cancel(id * 2);

		} else {
			Log.d("Beam+",
					"[BeamPlusUi] updateProgress(), No matched notification items");
		}
	}

	/**
	 * Create Beam Notification object
	 * 
	 * @param context
	 * @param title
	 * @param cancelWord
	 * @param intent
	 * @return
	 */
	private Notification createBeamNotification(CharSequence title,
			CharSequence cancelWord, PendingIntent cancelIntent, boolean isUpload) {

		if (isUpload) {
			return new Notification.Builder(mContext).setContentTitle(title)
					.setSmallIcon(android.R.drawable.stat_sys_upload)
					.addAction(R.drawable.ic_menu_cancel_holo_dark, cancelWord, cancelIntent)
					.setProgress(0, 0, true).setPriority(100).build();
		} else {
			return new Notification.Builder(mContext).setContentTitle(title)
					.setSmallIcon(android.R.drawable.stat_sys_download)
					.addAction(R.drawable.ic_menu_cancel_holo_dark, cancelWord, cancelIntent)
					.setProgress(0, 0, true).setPriority(100).build();
		}
	}

	/**
	 * Create Progress Notification
	 * 
	 * @param context
	 * @param title
	 * @param isUpload
	 * @return
	 */
	private Notification createProressNotification(CharSequence title,
			boolean isUpload, boolean isComplete, PendingIntent intent) {

		Notification.Builder builder = new Notification.Builder(mContext);
		builder.setContentTitle(title);		

		if (isUpload) {
			if (isComplete) {
				builder.setSmallIcon(android.R.drawable.stat_sys_upload_done);				
				builder.setAutoCancel(true);
			} else {
				builder.setContentInfo("0 %");
				builder.setSmallIcon(android.R.drawable.stat_sys_upload);

				builder.setProgress(FileTransfer.MAX_PROGRESS_VALUE,
						FileTransfer.MIN_PROGRESS_VALUE, false);
			}
		} else {
			if (isComplete) {
				builder.setSmallIcon(android.R.drawable.stat_sys_download_done);				
				builder.setAutoCancel(true);
			} else {
				builder.setContentInfo("0 %");
				builder.setSmallIcon(android.R.drawable.stat_sys_download);
				builder.setProgress(FileTransfer.MAX_PROGRESS_VALUE,
						FileTransfer.MIN_PROGRESS_VALUE, false);
			}
		}
		
		if (intent != null) {
			builder.setContentIntent(intent);
		}

		return builder.build();
	}
}
