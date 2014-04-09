package com.mediatek.nfc.handover;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import com.mediatek.nfc.handover.FileTransfer;
import com.mediatek.nfc.handover.FileTransfer.IReceiverUI;
import com.mediatek.nfc.handover.FileTransfer.IServer;
import com.mediatek.nfc.handover.FileTransfer.IServerEventListener;
import com.mediatek.nfc.handover.FileTransfer.IRecvRecord;
import com.mediatek.nfc.handover.exceptions.JobCancelException;
import com.mediatek.nfc.handover.exceptions.ReadEofException;
import com.mediatek.nfc.handover.FilePushRecord;

public class FilePushServer implements IServer {

	/**
	 * Constructor
	 */
	public FilePushServer(Context context) {
		mExecutor = new ThreadPoolExecutor(2, 4, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());

		mHandler = new UIHandler(this);
		mContext = context;
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(IReceiverUI.BROADCAST_CANCEL_BY_USER);
		mContext.registerReceiver(mBroadcastReceiver, filter);
	}

	// Tag
	private static final String TAG = "Beam+";
	// Listening Port
	private static final int LISTENING_PORT = 3128;
	// Max Server waiting time
	private static final int MAX_SERVER_WAITING_TIME = 1000;
	// Max Server waiting time
	private static final int MAX_PEER_WAITING_TIME = 10000;
	// Max Notification ID
	private static final int MIN_NOTIFICATION_ID = 30000;
	private static final int MAX_NOTIFICATION_ID = 40000;

	// Message
	private static final int MESSAGE_PREPARE_PROGRESS = 0x01;
	private static final int MESSAGE_PROGRESS_FINISH = 0x03;
	private static final int MESSAGE_CANCEL_BY_USER = 0x04;
	private static final int MESSAGE_EXCEPTION_OCCUR = 0x05;
	/// R: @ {
	private static final int MESSAGE_PROGRESS_AUTO_UPDATE = 0x13;
	private static final int MESSAGE_PROGRESS_STOP_UPDATE = 0x14;
	/// }

	// Buffer size
	private static final int MAX_BUFFER_SIZE = 2048;

	// Storage Path

	// Welcome message
	private static final byte[] PROTOCOL_ACCEPT = FileTransfer.PROTOCOL_MESSAGE_ACCEPT
			.getBytes();

	// OK message
	private static final byte[] PROTOCOL_OK = FileTransfer.PROTOCOL_MESSAGE_OK
			.getBytes();

	// ERROR message
	private static final byte[] PROTOCOL_ERR = FileTransfer.PROTOCOL_MESSAGE_ERR
			.getBytes();

	// Repository Path
	private static String mRepositoryPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "beam";
	// Is Task Running
	private boolean mIsServerRunning;
	// Executor
	private ThreadPoolExecutor mExecutor;
	// UIHandler
	private UIHandler mHandler;
	// Listener
	private IServerEventListener mListener;
	// UI Handler
	private IReceiverUI mUI;
	// Session Map
	private Map<Integer, Session> mSessionMap = new HashMap<Integer, Session>();
	// Now Max Notification ID
	private int mNowNotification = MIN_NOTIFICATION_ID;
	// context
	private Context mContext;
    // Session Count Mutex
    private Object mSessionMutex = new Object();
    // Session Count
    private int mSessionCount;
	
	/**
	 * Session
	 */
	private class Session implements Runnable {

		/**
		 * Constructor
		 * 
		 * @param channel
		 */
		public Session(SocketChannel channel) {
			mChannel = channel;
            
			mNowNotification++;
			if (mNowNotification >= MAX_NOTIFICATION_ID) {
				mNowNotification = MIN_NOTIFICATION_ID;
			}
			
			mKey = mNowNotification;

            mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_PREPARE_PROGRESS, mKey, 0, null));
		}

		// Socket channel
		private SocketChannel mChannel;
		// Byte Buffer
		private ByteBuffer mBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);
		// Received records
		private List<IRecvRecord> mRecvRecords = new ArrayList<IRecvRecord>();
		// id
		private int mKey;
		// isRunning
		private boolean mIsRunning;
		

		/**
		 * Run
		 */
		public void run() {
		
			mIsRunning = true;

			synchronized (mSessionMutex) {
				mSessionCount++;
			}

			addSessionToMapSynced(mKey, this);

			doTransceive();
			
			removeSessionFromMapSynced(mKey);

			synchronized (mSessionMutex) {
				mSessionCount--;
				if (mSessionCount <= 0) {
					triggerDisconnectedEvent();//R: no use, the mSessionCount isn't static
				}
			}
		}
		
		public void cancel() {
			Log.d(TAG, "cancel, Session id = " + mKey);
			mIsRunning = false;
		}

		/**
		 * doReceive
		 * 
		 * @return
		 * @throws IOException
		 */
		private void doTransceive() {

			Log.d(TAG, "[FilePushServer] doTransceive(), Accept: "
					+ mChannel.socket().getRemoteSocketAddress());

			try {

				Selector selector = Selector.open();
				try {

					mChannel.configureBlocking(false);
					mChannel.register(selector, SelectionKey.OP_WRITE);

					boolean isSendWelcome = false;
					boolean isReadSuccess = false;
					while (mIsRunning) {

                        int numOfReadyChannel = selector.select(MAX_PEER_WAITING_TIME);
						if (numOfReadyChannel <= 0) {
                            Log.d(TAG, "[FilePushServer] numOfReadyChannel = " + numOfReadyChannel);
                            if (mIsRunning) {
                                throw new IOException();
                            } else {
                                throw new JobCancelException();
                            }
						}

						Set<SelectionKey> sKeys = selector.selectedKeys();
						for (SelectionKey sKey : sKeys) {

							sKeys.remove(sKey);
							if (sKey.isReadable()) {

								Log.d(TAG,
										"[FilePushServer] doTransceive(), read ...");

								isReadSuccess = doDownload(mRepositoryPath);
								mChannel.register(selector,
										SelectionKey.OP_WRITE);

							} else if (sKey.isWritable()) {

								if (mChannel.isConnected()) {

									Log.d(TAG,
											"[FilePushServer] doTransceive(), write ...");
									mBuffer.clear();

									if (isSendWelcome) {
										if (isReadSuccess) {
											mBuffer.put(PROTOCOL_OK);
										} else {
											mBuffer.put(PROTOCOL_ERR);
										}
									} else {
										isSendWelcome = true;
										mBuffer.put(PROTOCOL_ACCEPT);
									}

									mBuffer.flip();
									mChannel.write(mBuffer);
									mChannel.register(selector,
											SelectionKey.OP_READ);

								} else {
									throw new ReadEofException();
								}
							}
						}
					}

					sendCancel(mKey);

				} finally {
					selector.close();
					mChannel.close();
				}
			} catch (JobCancelException e) {
				sendCancel(mKey);
			} catch (ReadEofException e) {
				sendJobCompleted(mKey, mRecvRecords);
			} catch (IOException e) {
				e.printStackTrace();
				sendError(mKey);
			}
		}

		/**
		 * Find file handle
		 * 
		 * @param fileDir
		 * @param fileName
		 * @return
		 */
		private File prepareFile(File fileDir, String fileName) {

			if (!fileDir.exists()) {
				fileDir.mkdirs();
			}

			File file = new File(fileDir, fileName);
			if (file.exists()) {
				StringBuilder fileNameBuilder = new StringBuilder();

				int dot = fileName.indexOf(".");
				String subFileName = fileName.substring(dot);

				fileNameBuilder.append(fileName.substring(0, dot));
				int fileMainNameLen = fileNameBuilder.length();
				int i = 1;
				File newFile = null;
				while (true) {

					fileNameBuilder.append("(").append(i++).append(")")
							.append(subFileName);

					newFile = new File(fileDir, fileNameBuilder.toString());
					if (!newFile.exists()) {
						return newFile;
					}

					fileNameBuilder.setLength(fileMainNameLen);
				}
			} else {
				return file;
			}
		}

		/**
		 * Do Download
		 * 
		 * @param path
		 * @return
		 * @throws JobCancelException
		 * @throws ReadEofException
		 */
		private boolean doDownload(String rootPath) throws IOException,
				JobCancelException, ReadEofException {
			
			Log.d(TAG, "Download Start: " + System.currentTimeMillis());

			int readLen = 0;
			File fileDir = new File(rootPath);

			mBuffer.clear();
			mBuffer.limit(10);
			readLen = mChannel.read(mBuffer);
			if (readLen == -1) {
				throw new ReadEofException();
			}

			if (readLen != 10) {
				throw new IOException("Data format invalid");
			}

			mBuffer.flip();
			int fileNameLen = mBuffer.getShort();
			long fileSize = mBuffer.getLong();
			mBuffer.clear();

			mBuffer.limit(fileNameLen);
			readLen = mChannel.read(mBuffer);
			if (readLen != fileNameLen) {
				throw new IOException("Data format invalid");
			}

			mBuffer.flip();
			String fileName = new String(mBuffer.array(), 0, fileNameLen);
			File file = prepareFile(fileDir, fileName);

			mBuffer.clear();
			FileOutputStream fout = new FileOutputStream(file);
			FileChannel foutChannel = fout.getChannel();

			/// R: @ {
			mHandler.setID(mKey);
			mHandler.setFileName(fileName);
			mHandler.setProgress(0);
			mHandler.sendEmptyMessage(MESSAGE_PROGRESS_AUTO_UPDATE);
			/// }
			
	        int recvBytes = 0;
			try {

				Log.d(TAG, "[FilePushServer] doDownload(), file size = "
						+ fileSize);

				int nowProgress = 0;
				while (mIsRunning) {

					readLen = mChannel.read(mBuffer);
					if (readLen == -1
							|| (readLen == 0 && fileSize == recvBytes)) {

						if (fileSize != recvBytes) {
							Log.e(TAG,
									"[FilePushServer] download data is invalid");
							file.delete();
							mRecvRecords.add(new RecvRecord(rootPath + "/" + fileName, false));
                            FilePushRecord.getInstance().insertWifiIncomingRecord(rootPath + "/" + fileName,
                                false, recvBytes, fileSize);
							return false;
						}
						mRecvRecords.add(new RecvRecord(rootPath + "/" + fileName, true));
                        FilePushRecord.getInstance().insertWifiIncomingRecord(rootPath + "/" + fileName, 
                            true, recvBytes, fileSize);
						return true;
					}

					recvBytes += readLen;
					mBuffer.flip();
					foutChannel.write(mBuffer);
					mBuffer.clear();

					nowProgress = (int) (((double) recvBytes / (double) fileSize) * (double) 100);
					mHandler.setProgress(nowProgress);
				}

			} finally {
				/// R: @ {
				mHandler.sendEmptyMessage(MESSAGE_PROGRESS_STOP_UPDATE);
				/// }
				foutChannel.close();
				fout.close();
				
				Log.d(TAG, "Download END: " + System.currentTimeMillis());
			}

			throw new JobCancelException();
		}
	}
	
	public class RecvRecord implements IRecvRecord {
		private String mFullPath;
		private boolean mResult;
		
		public RecvRecord(String fullPath, boolean result) {
			mFullPath = fullPath;
			mResult = result;
		}
		
		public String getFullPath() {
			return mFullPath;
		}
		
		public boolean getResult() {
			return mResult;
		}
	};


	/**
	 * Handle UI
	 * 
	 */
	private static class UIHandler extends Handler {

		/**
		 * Constructor
		 * 
		 * @param server
		 */
		public UIHandler(FilePushServer server) {
			mController = new WeakReference<FilePushServer>(server);
		}

		// Controller
		private WeakReference<FilePushServer> mController;
		
		/// R: @ {
		private int mID;
		private String mFileName;
		private int mProgress;
		public void setID(int ID) {
			mID = ID;
		}
		public void setFileName(String fileName) {
			mFileName = fileName;
		}
		public void setProgress(int progress) {
			mProgress = progress;
		}
		/// }

		@Override
		public void handleMessage(Message msg) {

			FilePushServer server = mController.get();
			if (server == null || server.mUI == null) {
				Log.e(TAG,
						"[FilePushServer] handleMessage(), it cannot dispatch event.");
				return;
			}

			// UI Handler
			IReceiverUI ui = server.mUI;

			switch (msg.what) {
			/// R: @ {
			case MESSAGE_PROGRESS_AUTO_UPDATE:
				ui.onProgressUpdate(mID, mFileName, mProgress);
				sendEmptyMessageDelayed(MESSAGE_PROGRESS_AUTO_UPDATE, 2000);
				break;
				
			case MESSAGE_PROGRESS_STOP_UPDATE:
				removeMessages(MESSAGE_PROGRESS_AUTO_UPDATE);
				break;
			/// }
			case MESSAGE_CANCEL_BY_USER:
				ui.onCanceled(msg.arg1);
				break;
			case MESSAGE_PREPARE_PROGRESS:
				ui.onPrepared(msg.arg1);
				break;
			case MESSAGE_PROGRESS_FINISH:
				ui.onCompleted(msg.arg1, mRepositoryPath + File.separator, (List<IRecvRecord>)msg.obj);
				break;
			case MESSAGE_EXCEPTION_OCCUR:
				ui.onError(msg.arg1);
				break;
			}
		}
	}

	/**
	 * Server Thread
	 * 
	 */
	private class ServerThread implements Runnable {

		/**
		 * Do Accept
		 * 
		 * @return
		 * @throws IOException
		 */
		private void doAccept() {

			try {
				Selector selector = Selector.open();
				try {
					ServerSocketChannel channel = ServerSocketChannel.open();
					channel.configureBlocking(false);

					try {

						InetSocketAddress address = new InetSocketAddress(
								LISTENING_PORT);

						// Prepare a Server Socket Channel
						channel.socket().bind(address);

						// Register a channel to listen the ACCEPT Operation
						channel.register(selector, SelectionKey.OP_ACCEPT);
						Log.d(TAG, "[FilePushServer] listening "
								+ LISTENING_PORT);

						// Send started message
						triggerServerEvent(true);

						while (mIsServerRunning) {
							if (selector.select(MAX_SERVER_WAITING_TIME) == 0) {
								continue;
							}

							Set<SelectionKey> sKeys = selector.selectedKeys();
							for (SelectionKey sKey : sKeys) {
								sKeys.remove(sKey);
								if (sKey.isAcceptable()) {

									Session session = new Session(
											channel.accept());

									mExecutor.execute(session);
								}
							}
						}

						triggerServerEvent(false);

					} finally {
						channel.close();
						Log.d(TAG,
								"[FilePushServer] doAccept(),Server is down.");
					}
				} finally {
					selector.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
				triggerServerEvent(false);
			}
		}

		@Override
		public void run() {
			doAccept();
		}
	}

	/**
	 * Trigger disconnected callback
	 */
	private void triggerDisconnectedEvent() {
		if (mListener == null) {
			return;
		}

		mListener.onDisconnected();
	}

	/**
	 * Set disconnect event
	 */
	private void triggerServerEvent(boolean isStart) {

		if (mListener == null) {
			return;
		}

		if (isStart) {
			mListener.onServerStarted();
		} else {
			mListener.onServerShutdown();
		}
	}

	/**
	 * Send job complete
	 * 
	 * @param job
	 */
	private void sendJobCompleted(int id, List<IRecvRecord> recvRecords) {	
		mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_PROGRESS_FINISH, id, 0, recvRecords));
	}

	/**
	 * Send cancel event
	 */
	private void sendCancel(int id) {
		mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_CANCEL_BY_USER, id, 0, null));
	}

	/**
	 * Send cancel event
	 */
	private void sendError(int id) {
		mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_EXCEPTION_OCCUR, id, 0, null));
	}

	@Override
	public void start() {

		synchronized (this) {

			mIsServerRunning = true;
			mExecutor.execute(new ServerThread());

		}
	}

	@Override
	public void stop() {
		removeAllFromMapSynced();
		mIsServerRunning = false;
	}

	@Override
	public void setServerEventListener(IServerEventListener listener) {
		mListener = listener;
	}

	@Override
	public void setUIHandler(IReceiverUI uiHandler) {
		mUI = uiHandler;
	}
	
	private void addSessionToMapSynced(int key, Session session) {
		synchronized (mSessionMap) {
			mSessionMap.put(key, session);
		}
	}
	
	private void removeSessionFromMapSynced(int key) {
		synchronized (mSessionMap) {
			Session s = mSessionMap.get(key);
			if (s != null) {
				s.cancel();
				mSessionMap.remove(key);
			}
		}	
	}
	
	private void removeAllFromMapSynced() {
		synchronized (mSessionMap) {
			for (Session s : mSessionMap.values()) {
				s.cancel();
			}
			mSessionMap.clear();
		}	
	}
	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(IReceiverUI.BROADCAST_CANCEL_BY_USER)) {
				int key = intent.getIntExtra(IReceiverUI.EXTRA_ID, -1);
				Log.d(TAG, "IReceiverUI.BROADCAST_CANCEL_BY_USER, key = " + key);
				removeSessionFromMapSynced(key);
			}
		}    	

	};
    
    public boolean isAnySessionOngoing() {
        synchronized (mSessionMutex) {
            Log.d(TAG, "isAnySessionOngoing(), mSessionCount = " + mSessionCount);
            return (mSessionCount > 0) ? true : false;
        }
    }
}
