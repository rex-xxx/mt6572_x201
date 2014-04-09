package com.mediatek.nfc.handover;

import android.content.Context;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import com.mediatek.nfc.handover.FileTransfer.IReceiverUI;
import com.mediatek.nfc.handover.FileTransfer.IServer;
import com.mediatek.nfc.handover.FileTransfer.IRecvRecord;
import com.mediatek.nfc.handover.ui.BeamPlusUi;
import com.mediatek.nfc.handover.ui.ConfirmPromptActivity;
import android.content.Intent;
import android.app.PendingIntent;
import android.net.Uri;
import android.media.MediaScannerConnection;
import android.content.ContentResolver;
import android.webkit.MimeTypeMap;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;
import com.android.nfc.R;

public class ReceiverUI extends BeamPlusUi 
	implements IReceiverUI {

	// TAG
	private static final String TAG = "Beam+";
	private static final String INTENT_USER_WANT_CANCEL = "beamplus.receiverui.user.cancel";

	/**
	 * Receiver UI
	 * 
	 * @param context
	 * @param server
	 */
	public ReceiverUI(Context context, IServer server) {
		super(context);
		IntentFilter filter = new IntentFilter();
		filter.addAction(INTENT_USER_WANT_CANCEL);
		mContext.registerReceiver(mBroadcastReceiver, filter);
	}

	@Override
	public void onProgressUpdate(int id, String filename, int progress) {
		Log.d(TAG, "[ReceiverUI] Recv(" + id + ") " + filename + " in "
				+ progress + "%");
        updateProgressUI(id, filename, progress);
	}

	@Override
	public void onPrepared(int id) {
		Log.d(TAG, "[ReceiverUI] onPrepared(), currentID = " + id);
		Intent intent = new Intent(INTENT_USER_WANT_CANCEL);
		intent.putExtra(IReceiverUI.EXTRA_ID, id);
        String sw = mContext.getString(R.string.beam_progress);
        String sc = mContext.getString(R.string.cancel);
		prepareNotification(id, sw, sc, PendingIntent.getBroadcast(mContext, id, intent, 0),
				false);
	}

	@Override
	public synchronized void onCompleted(int id, String beamRoot, List<IRecvRecord> recvRecords) {
		Log.d(TAG, "[ReceiverUI] onCompleted(), id = " + id + ", recvRecords = " + recvRecords);
		
		String[] arrayPaths = new String[recvRecords.size()];
		if (recvRecords.size() == 1) {
			arrayPaths[0] = recvRecords.get(0).getFullPath();
		} else if (recvRecords.size() > 1) {
			File directory = generateUniqueDirectory(beamRoot);
			if (!directory.exists() && !directory.mkdir()) {
				Log.d(TAG, "[ReceiverUI] onCompleted(), fail to mkdir: " + directory.getAbsolutePath()); 
				for (int i = 0; i < recvRecords.size(); i++) {
					arrayPaths[i] = recvRecords.get(i).getFullPath();
				}
			} else {
				for (int i = 0; i < recvRecords.size(); i++) {
					try {
						File oldFile = new File(recvRecords.get(i).getFullPath());
						File newFile = generateUniqueFile(directory.getAbsolutePath(), oldFile.getName());
						if (!oldFile.renameTo(newFile)) {
							Log.d(TAG, "[ReceiverUI] fail to rename from oldFile: " + oldFile + " to " + newFile);
						} else {
							arrayPaths[i] = newFile.getAbsolutePath();
						}
					} catch (Exception e) {
						Log.d(TAG, "[ReceiverUI] onCompleted() exception !!");
						e.printStackTrace();						
					}
				}
			}
		}
		
		MediaScannerConnection.scanFile(mContext, arrayPaths, null, new MediaScanningJob(id, recvRecords));				
	}

	@Override
	public void onCanceled(int id) {
		Log.d(TAG, "[ReceiverUI] onCanceled(), id = " + id);
		completeNotification(id, mContext.getString(R.string.beam_canceled), null);
	}

	@Override
	public void onError(int id) {
		Log.d(TAG, "[ReceiverUI] onError(), id = " + id);
		completeNotification(id, mContext.getString(R.string.beam_failed), null);
	}
	
	private File generateUniqueDirectory(String beamRoot) {       
		String format = "yyyy-MM-dd";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		String newPath = beamRoot + "beam-" + sdf.format(new Date());
		File newFile = new File(newPath);
		int count = 0;
		while (newFile.exists()) {
			newPath = beamRoot + "beam-" + sdf.format(new Date()) + "-" +
					Integer.toString(count);
			newFile = new File(newPath);
			count++;
		}
		return newFile;
	}

	private File generateUniqueFile(String path, String fileName) {
		int dotIndex = fileName.lastIndexOf(".");
		String extension = null;
		String fileNameWithoutExtension = null;
		if (dotIndex < 0) {
			extension = "";
			fileNameWithoutExtension = fileName;
		} else {
			extension = fileName.substring(dotIndex);
			fileNameWithoutExtension = fileName.substring(0, dotIndex);
		}
		File dstFile = new File(path + File.separator + fileName);
		int count = 0;
		while (dstFile.exists()) {
			dstFile = new File(path + File.separator + fileNameWithoutExtension + "-" +
					Integer.toString(count) + extension);
			count++;
		}
		return dstFile;
	}
	
	private class MediaScanningJob implements MediaScannerConnection.OnScanCompletedListener {	
		int mId;
		int mUrisScanned;
		Map<String, Uri> mMediaUris;
		List<IRecvRecord> mRecvRecords;
		MediaScanningJob(int id, List<IRecvRecord> recvRecords) {
			mId = id;
			mRecvRecords = new ArrayList<IRecvRecord>(recvRecords);
			mMediaUris = new HashMap<String, Uri>();
		}
		
		@Override
		public synchronized void onScanCompleted(String path, Uri uri) {
			Log.d(TAG, "[ReceiverUI] Scan completed, path: " + path + ", uri: " + uri);
			
			if (uri != null) {
				mMediaUris.put(path, uri);
			}
			
			mUrisScanned++;
			if (mUrisScanned == mRecvRecords.size()) {
				Intent viewIntent = buildViewIntent();
				PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, viewIntent, 0);
				completeNotification(mId, mContext.getString(R.string.beam_complete), contentIntent);
			}
		}
		
		private String getMimeType(String url) {
			String type = "";
			String extension = MimeTypeMap.getFileExtensionFromUrl(url);
			if (extension != null) {
				MimeTypeMap mime = MimeTypeMap.getSingleton();
				type = mime.getMimeTypeFromExtension(extension);
			}
			return type;
		}

		private Intent buildViewIntent() {
			if (mRecvRecords.size() == 0) {
				return null;
			}

			for (String filePath : mMediaUris.keySet()) {
				Uri uri = mMediaUris.get(filePath);
				if (uri != null) {
					Intent viewIntent = new Intent(Intent.ACTION_VIEW);			
					String mimeType = getMimeType(filePath);
					Log.d(TAG, "[ReceiverUI] uri = " + uri + ", mimeType = " + mimeType + ", filePath = " + filePath);
					viewIntent.setDataAndTypeAndNormalize(uri, mimeType);
					return viewIntent;
				} 
			}
			
			Log.d(TAG, "[ReceiverUI] nothing to launch, activate beam history");
			Intent intent = new Intent("android.settings.BEAMPLUS_HISTORY");
			return intent;			
		}
	}
	
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(INTENT_USER_WANT_CANCEL)) {
				int id = intent.getIntExtra(IReceiverUI.EXTRA_ID, -1);
				Log.d(TAG, "INTENT_USER_WANT_CANCEL, id = " + id);
				if (id != -1) {
					Intent innerIntent = new Intent(IReceiverUI.BROADCAST_CANCEL_BY_USER);
					innerIntent.putExtra(IReceiverUI.EXTRA_ID, id);
                    Log.d(TAG, "fire pending intent");
                    try {
                        PendingIntent.getBroadcast(mContext, id, innerIntent, 0).send();
                    } catch (Exception e) {
                        Log.d(TAG, "fail to fire pending intent");
                        e.printStackTrace();
                    }
                    //mContext.sendBroadcast(innerIntent);
                    /*
					Intent directIntent = new Intent(mContext, ConfirmPromptActivity.class);
					directIntent.putExtra(ConfirmPromptActivity.EXTRA_PENDINTINTENT, PendingIntent.getBroadcast(mContext, id, innerIntent, 0));
					directIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);// | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					mContext.startActivity(directIntent);
                    */
				}
			}			
		}    	
    };

}
