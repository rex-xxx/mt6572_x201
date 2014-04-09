package com.mediatek.nfc.handover;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.mediatek.nfc.handover.FileTransfer.IClient;
import com.mediatek.nfc.handover.FileTransfer.IClientEventListener;
import com.mediatek.nfc.handover.FileTransfer.ISenderUI;
import com.mediatek.nfc.handover.FilePushRecord;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.provider.MediaStore;
import android.database.Cursor;

public class FilePushClient implements IClient {

	/**
	 * Constructor
	 */
	public FilePushClient(Context context) {
		mExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
		IntentFilter filter = new IntentFilter();
		filter.addAction(ISenderUI.BROADCAST_CANCEL_BY_USER);
		context.registerReceiver(mBroadcastReceiver, filter);	
        mContext = context;
	}
    
	// TAG
	private static final String TAG = "FilePushClient";
	// Max Send buffer size
	private static final int MAX_SEND_BUFFER = 4096;
	// Max Waiting Connection Time
	private static final int MAX_WAITING_CONNECT_TIME = 10000;
	// Max Notification ID
	private static final int MAX_NOTIFICATION_ID = 200000;
	// Message to UI (Prepare Progress)
	private static final int MESSAGE_PREPARE_PROGRESS = 0x01;
	// Message to UI (Progress Update)
	private static final int MESSAGE_PROGRESS_UPDATE = 0x02;
	/// R: @ {
	// Message to UI (auto progress update)
	private static final int MESSAGE_PROGRESS_AUTO_UPDATE = 0x13;
	private static final int MESSAGE_PROGRESS_STOP_UPDATE = 0x14;
	private static final int MESSAGE_CANCEL_BROADCAST_RECEIVED = 0x15;
	/// }
	// Message to UI (Progress Finish)
	private static final int MESSAGE_PROGRESS_FINISH = 0x03;
	// Message to UI (Cancel by User)
	private static final int MESSAGE_CANCEL_BY_USER = 0x04;
	// Message to UI (Exception Occur)
	private static final int MESSAGE_EXCEPTION_OCCUR = 0x05;
	// Message to UI (Disconnect)
	private static final int MESSAGE_DISCONNECT = 0x06;

    // Context
    private Context mContext;
	// Now Max Notification ID
	private int mNowNotification = 0;
	// Working Thread
	private ClientSession mWorkingSession;
	// Flag is connect
	private boolean mIsConnect;
	// Current Job
	private Job mJob;
	// Queue
	private LinkedList<Job> mQueue = new LinkedList<Job>();
	// Address
	private InetSocketAddress mNetworkAddress;
	// Share buffer
	private ByteBuffer mBuffer = ByteBuffer.allocate(MAX_SEND_BUFFER);
	// Listener
	private IClientEventListener mListener;
	// UI Handler
	private UIHandler mHandler = new UIHandler(this);
	// UI Handler
	private ISenderUI mUI;
	// Thread Pool
	private ThreadPoolExecutor mExecutor;

	/**
	 * Data Structure
	 * 
	 */
	private class Job {
		Uri[] uris;
		int id;
		int cursor;
		int passCount;
	}

	/**
	 * UI Handler
	 * 
	 */
	private class UIHandler extends Handler {//no needs to be a static class

		private int mId;
		private int mTotal;
		private WeakReference<FilePushClient> mController;
		

		public UIHandler(FilePushClient controller) {
			mController = new WeakReference<FilePushClient>(controller);
		}
		
		/// R: @{
		private String mFileName;
		private int mProgress;
		private int mPosition;
		
		public void setFileName(String fileName) {
			mFileName = fileName;
		}
		
		public void setPosition(int pos) {
			mPosition = pos;
		}
		
		public void setProgress(int progress) {
			mProgress = progress;
		}
		/// }

		/**
		 * Handler
		 */
		@Override
		public void handleMessage(Message msg) {

			FilePushClient client = mController.get();
			if (client == null) {
				Log.e(TAG, "handleMessage(),Client is null");
				return;
			}

			if (msg.what == MESSAGE_DISCONNECT) {
				client.triggerDisconnectEvent(msg.arg1);
				return;
			}

			if (client.mUI == null) {
				return;
			}

			switch (msg.what) {
			case MESSAGE_PREPARE_PROGRESS:
				client.mUI.onPrepared(msg.arg1, msg.arg2);
				break;
				
			/// R: @ {
			case MESSAGE_PROGRESS_AUTO_UPDATE:
				Job ongoingJob = (Job) msg.obj;
				client.mUI.onProgressUpdate(ongoingJob.id, mFileName, mProgress,
						ongoingJob.uris.length, mPosition);
				sendMessageDelayed(obtainMessage(MESSAGE_PROGRESS_AUTO_UPDATE, ongoingJob), 300);
				break;
				
			case MESSAGE_PROGRESS_STOP_UPDATE:
				removeMessages(MESSAGE_PROGRESS_AUTO_UPDATE);
				break;
				
			case MESSAGE_CANCEL_BROADCAST_RECEIVED:
				int idToCancel = msg.arg1;
				Log.d(TAG, "MESSAGE_CANCEL_BROADCAST_RECEIVED, cancal job id = " + idToCancel);
				synchronized (FilePushClient.this) {
					if (mJob != null && idToCancel == mJob.id) {
						if (mWorkingSession != null) {
							mWorkingSession.cancel();
						}
					} else {
						Job jobToCancel = null;
						for (Job j : mQueue) {
							if (j.id == idToCancel) {
								jobToCancel = j;
							}
						}
						if (jobToCancel != null) {
							mQueue.remove(jobToCancel);
							client.mUI.onCaneceled(jobToCancel.id, jobToCancel.uris.length, jobToCancel.passCount);
						}
					}
				}
				break;
			/// }

			case MESSAGE_PROGRESS_UPDATE:
				client.mUI.onProgressUpdate(mId, (String) msg.obj, msg.arg1,
						mTotal, msg.arg2);
				break;

			case MESSAGE_PROGRESS_FINISH:
				Job finishJob = (Job) msg.obj;
				client.mUI.onCompleted(finishJob.id, finishJob.uris.length, finishJob.passCount);
				break;

			case MESSAGE_CANCEL_BY_USER:
				Job cancelJob = (Job) msg.obj;
				client.mUI.onCaneceled(cancelJob.id, cancelJob.uris.length, cancelJob.passCount);
				break;

			case MESSAGE_EXCEPTION_OCCUR:
				Job errorJob = (Job) msg.obj;
				client.mUI.onError(errorJob.id, errorJob.uris.length, errorJob.passCount);
				break;
			}
		}
	}

	/**
	 * Client Thread
	 * 
	 */
	private class ClientSession implements Runnable {

		// Is Running
		private boolean mIsRunning;

		/**
		 * Run
		 */
		@Override
		public void run() {
			mIsRunning = true;
			sendDisconnectOrContinue(doSend());
		}

		/**
		 * Cancel the thread
		 */
		public void cancel() {
			mIsRunning = false;
		}

		/**
		 * Send a single file using JAVA(TM) NIO
		 * 
		 * @param uri
		 * @param selector
		 * @param buffer
		 * @return
		 */

		private boolean sendFile(Uri uri, SocketChannel channel,
				int totalFileCount, int filePos, Job job) throws IOException {

			int sendBytes = 0;
			int sendRate = 0;
			File sendFile = getFile(uri);
			if (sendFile == null) {
				return true;
			}

			String fileName = sendFile.getName();
			ByteBuffer buffer = mBuffer;
			buffer.clear();

			byte[] fileNameBytes = fileName.getBytes();
			long fileSize = sendFile.length();
			buffer.putShort((short) fileNameBytes.length);
			buffer.putLong(fileSize);
			buffer.put(fileNameBytes);
			buffer.flip();
			channel.write(buffer);
			buffer.clear();

			if (!mIsRunning) {
				sendCancelOrError(job, false);
				return false;
			}

			/// R: @ {
			mHandler.setFileName(fileName);
			mHandler.setPosition(job.cursor);
			mHandler.setProgress(0);
			mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_PROGRESS_AUTO_UPDATE, job));
			/// }

			FileInputStream fin = new FileInputStream(sendFile);
			FileChannel fChannel = fin.getChannel();
			try {
				while ((fChannel.read(buffer)) > 0) {

					buffer.flip();
					while (buffer.remaining() > 0) {
						sendBytes += channel.write(buffer);
						sendRate = (int) (((double) sendBytes / (double) fileSize) * (double) 100);
						/// R: @ {
						mHandler.setProgress(sendRate);
						/// }

                        if (!mIsRunning) {
                            break;
                        }
					}

					buffer.clear();
				}
			} catch (Exception e) {
                Log.d(TAG, "exception during file sending");
                e.printStackTrace();
                mIsRunning = false;
            } finally {
				mHandler.sendEmptyMessage(MESSAGE_PROGRESS_STOP_UPDATE);
				fChannel.close();
				fin.close();
			}

            if (!mIsRunning) {
                sendCancelOrError(job, false);
                FilePushRecord.getInstance().insertWifiOutgoingRecord(uri, false, sendBytes, fileSize);
                return false;
            } else {
                FilePushRecord.getInstance().insertWifiOutgoingRecord(uri, true, sendBytes, fileSize);
                Log.d(TAG, "File send complete.");
                return true;
            }
		}

		/**
		 * Convert byte message to string
		 * 
		 * @param buffer
		 * @return
		 */
		private String convertToMessage(ByteBuffer buffer) {
			byte[] data = buffer.array();
			try {
				int offset = buffer.arrayOffset();
				return new String(data, offset, buffer.limit() - offset);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}

		}

		/**
		 * Handler Selector
		 * 
		 * @param skey
		 * @param channel
		 * @param selector
		 * @return
		 * @throws IOException
		 */
		private int handleSelector(SelectionKey skey, SocketChannel channel,
				Selector selector) throws IOException {

			if (skey.isConnectable()) {
				Log.d(TAG, "current select : CONNECT");

				if (channel.isConnectionPending()) {
					channel.finishConnect();
				}

				channel.register(selector, SelectionKey.OP_READ);

			} else if (skey.isReadable()) {
				Log.d(TAG, "current select : READ, mIsConnect = " + mIsConnect);

				mBuffer.clear();
				if (channel.read(mBuffer) <= 0) {
					return FileTransfer.MESSAGE_TRANSMIT_ERROR;
				}

				mBuffer.flip();
				String message = convertToMessage(mBuffer);
				Log.d(TAG, "Message from server: "
						+ message);
				if (mIsConnect) {

					if (FileTransfer.PROTOCOL_MESSAGE_OK.equals(message)) {
						mJob.passCount++;
					} else if (FileTransfer.PROTOCOL_MESSAGE_ERR
							.equals(message)) {
					}

					if (mJob.cursor == mJob.uris.length) {
						sendJobCompleted(mJob);
                        return FileTransfer.MESSAGE_OK;
					}

					channel.register(selector, SelectionKey.OP_WRITE);

				} else {

					mIsConnect = true;
					if (!FileTransfer.PROTOCOL_MESSAGE_ACCEPT.equals(message)) {
						sendCancelOrError(mJob, true);
						return FileTransfer.MESSAGE_TRANSMIT_ERROR;
					}
					
					mJob = popJob();				

					channel.register(selector, SelectionKey.OP_WRITE);
				}

			} else if (skey.isWritable()) {
				Log.d(TAG, "current select : WRITE");
			
				Job job = mJob;
				Uri uri = job.uris[job.cursor++];
				if (sendFile(uri, channel, job.uris.length, job.cursor, job)) {
					channel.register(selector, SelectionKey.OP_READ);
					Log.d(TAG, "File send complete.");
				} else {
					sendCancelOrError(job, true);
					return FileTransfer.MESSAGE_CANCEL_TRANSMIT;
				}
			}

			return FileTransfer.MESSAGE_PENDING;
		}

		/**
		 * Do Send
		 */
		private int doSend() {

			int message = FileTransfer.MESSAGE_OK;
            int retryCount = 3;
            while (retryCount > 0) {
			    try {

			    	mIsConnect = false;
			    	SocketChannel channel = SocketChannel.open();
			    	channel.configureBlocking(false);
			    	channel.connect(mNetworkAddress);
			    	try {
			    		Selector selector = Selector.open();
			    		try {

			    			channel.register(selector, SelectionKey.OP_CONNECT);
			    			while (mIsRunning) {

			    				if (selector.select(MAX_WAITING_CONNECT_TIME) == 0) {
			    					sendCancelOrError(mJob, true);
			    					return FileTransfer.MESSAGE_TRANSMIT_ERROR;
			    				}

			    				Set<SelectionKey> skeys = selector.selectedKeys();
			    				for (SelectionKey skey : skeys) {
			    					skeys.remove(skey);
			    					message = handleSelector(skey, channel,
			    							selector);
			    					if (FileTransfer.MESSAGE_PENDING != message) {
			    						return message;
			    					}
			    				}
			    			}

			    			if (!mIsRunning) {
			    				sendCancelOrError(mJob, false);
			    			}

                            retryCount = 0;
			    		} finally {
			    			selector.close();
			    		}
			    	} finally {
			    		channel.close();
			    	}
			    } catch (IOException e) {
			    	e.printStackTrace();
                    retryCount--;
                    if (retryCount > 0) {
                        Log.d(TAG, " remote server not ready, give it one more shot!");
                        try {
                            Thread.sleep(500);
                        } catch (Exception ex) {
                            Log.d(TAG, " wtf!! exception during my sleep?");
                            retryCount = 0;
                        }
                    } else {
                        Log.d(TAG, " no more shot for retry!");
			    	    sendCancelOrError(mJob, true);
                    }
			    }
            }

			return message;
		}
	}
    
	private String getFilePathByContentUri(Uri uri) {
        Log.d(TAG, "getFilePathByContentUri(), uri.toString() = " + uri.toString());
	    Uri filePathUri = uri;
	    if (uri.getScheme().toString().compareTo("content")==0) {    
            Cursor cursor = null;
            try {
                cursor = mContext.getContentResolver().query(uri, null, null, null, null);
                if (cursor.moveToFirst()) {
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);//Instead of "MediaStore.Images.Media.DATA" can be used "_data"
                    filePathUri = Uri.parse(cursor.getString(column_index));
                    Log.d(TAG, "getFilePathByContentUri : " + filePathUri.getPath());
                    return filePathUri.getPath();
                }
            } catch (Exception e) {
                Log.d(TAG, "exception...");
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
	    } 
        Log.d(TAG, "getFilePathByContentUri doesn't work, try direct getPath");
	    return uri.getPath();
	}

	/**
	 * Only file by URI
	 * 
	 * @param uri
	 * @return
	 */
	private File getFile(Uri uri) {
		try {
			File file = new File(getFilePathByContentUri(uri));            
			if (file.exists() && file.isFile()) {
				return file;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Send all job is finished
	 */
	private void sendDisconnectOrContinue(int message) {
        synchronized (this) {
            if (mQueue.isEmpty()) {
                mWorkingSession = null;
                Message msg = mHandler.obtainMessage(MESSAGE_DISCONNECT, message, 0);
                msg.sendToTarget();
            } else {
                mWorkingSession = new ClientSession();
                mExecutor.execute(mWorkingSession);
            }
        }
	}

	/**
	 * Send job complete
	 * 
	 * @param job
	 */
	private void sendJobCompleted(Job job) {

		Message message = mHandler.obtainMessage(MESSAGE_PROGRESS_FINISH,
				job.id, 0, job);

		message.sendToTarget();
	}

	/**
	 * Send cancel event
	 */
	private void sendCancelOrError(Job job, boolean isError) {

		int command = isError ? MESSAGE_EXCEPTION_OCCUR
				: MESSAGE_CANCEL_BY_USER;

		if (job != null) {
			Message message = mHandler.obtainMessage(command, job.id, 0, job);
			message.sendToTarget();
		}

		Job jobInQueue = null;
		while (null != (jobInQueue = popJob())) {
			Message message = mHandler.obtainMessage(command, jobInQueue.id, 0,
					jobInQueue);

			message.sendToTarget();
		}
	}

	/**
	 * Set disconnect event
	 */
	private void triggerDisconnectEvent(int message) {

		if (mListener != null) {
			mListener.onDisconnected(message);
		}
	}

	/**
	 * Pop file need to send
	 * 
	 * @return
	 */
	private Job popJob() {
		synchronized (this) {
			Job job = mQueue.poll();
			if (job == null) {
				mWorkingSession = null;
			} else {
				return job;
			}
		}

		return null;
	}

	/**
	 * Connect to server
	 * 
	 * @param host
	 */
	public void connect(String host, int port) {
		mNetworkAddress = new InetSocketAddress(host, port);
	}

	/**
	 * Transfer Files
	 * 
	 * @param uris
	 */
	public void transferFiles(Uri[] uris) {

		if (uris == null || uris.length == 0) {
			Log.e(TAG,
					"transferFiles(), uris arg is invalid");
			return;
		}

		Job job = new Job();
		job.cursor = 0;
		job.uris = uris;

		synchronized (this) {

			if (mNowNotification >= MAX_NOTIFICATION_ID) {
				mNowNotification = 0;
			}

			job.id = mNowNotification++;
			
			mQueue.add(job);
			if (mWorkingSession == null) {
				mWorkingSession = new ClientSession();
				mExecutor.execute(mWorkingSession);
			}
		}

		Message message = mHandler.obtainMessage(MESSAGE_PREPARE_PROGRESS, job.id,
		uris.length);

		message.sendToTarget();
	}

	// Disconnect
	public void disconnect() {
		synchronized (this) {
			if (mWorkingSession != null) {
				mWorkingSession.cancel();
			}
		}
	}

	@Override
	public void connect(String host) {
		mNetworkAddress = new InetSocketAddress(host, 3128);
	}

	/**
	 * Set Client listener
	 */
	@Override
	public void setClientEventListener(IClientEventListener listener) {
		mListener = listener;
	}

	/**
	 * Transfer files
	 * 
	 * @param URIs
	 */
	@Override
	public void transferFiles(List<Uri> uris) {

		if (uris == null || uris.size() == 0) {
			return;
		}

		transferFiles(uris.toArray(new Uri[uris.size()]));
	}

	@Override
	public void setUIHandler(ISenderUI uiHandler) {
		mUI = uiHandler;
	}
	
	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
			if (action.equals(ISenderUI.BROADCAST_CANCEL_BY_USER)) {				
				int id = intent.getIntExtra(ISenderUI.EXTRA_ID, -1);
				Log.d(TAG, "ISenderUI.BROADCAST_CANCEL_BY_USER, id = " + id);
				if (id == -1) {
					Log.d(TAG, "invalid id...");
				} else {			
					mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_CANCEL_BROADCAST_RECEIVED, id, 0));
				}
			}
		}
	};

    public boolean isAnySessionOngoing() {
        synchronized (this) {
            boolean isOngoing = mWorkingSession != null ? true : false;
            Log.d(TAG, "isAnySessionOngoing(), isOngoing = " + isOngoing);
            return isOngoing;
        }
    }

}
