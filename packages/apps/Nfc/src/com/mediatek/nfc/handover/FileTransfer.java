package com.mediatek.nfc.handover;

import java.util.List;

import android.content.Context;
import android.net.Uri;

public class FileTransfer {

	/**
	 * Private constructor to avoid make a instance
	 */
	private FileTransfer() {
		// Dummy
	}

	// Message
	public static final int MESSAGE_OK = 0;
	public static final int MESSAGE_TRANSMIT_ERROR = 0x1001;
	public static final int MESSAGE_CANCEL_TRANSMIT = 0x1002;
	public static final int MESSAGE_PENDING = 0x2000;

	// OK
	public static final String PROTOCOL_MESSAGE_OK = "Accept: Next";
	// ERROR
	public static final String PROTOCOL_MESSAGE_ERR = "Deny: Error";
	// ACCEPT
	public static final String PROTOCOL_MESSAGE_ACCEPT = "Accept: Beam+";
	// Min Progress Value
	public static final int MIN_PROGRESS_VALUE = 0;
	// Max Progress Value
	public static final int MAX_PROGRESS_VALUE = 100;

	/**
	 * create a sender class
	 * 
	 * @param context
	 * @return
	 */
	public static IClient createDefaultSender(Context context) {
		FilePushClient client = new FilePushClient(context);
		ISenderUI ui = new FakeSenderUI(context, client);
		client.setUIHandler(ui);

		return client;
	}

	/**
	 * Factory to create receiver
	 * 
	 * @param context
	 * @return
	 */
	public static IServer createDefaultReceiver(Context context) {
		FilePushServer server = new FilePushServer(context);
		ReceiverUI ui = new ReceiverUI(context, server);
		server.setUIHandler(ui);

		return server;
	}

	/**
	 * Client UI Handler
	 * 
	 */
	public interface ISenderUI {
	
		static final String BROADCAST_CANCEL_BY_USER = "beamplus.sender.cancel";
		static final String EXTRA_ID = "beamplus.sender.extra.id";

		void onProgressUpdate(int id, String filename, int progress, int total,
				int position);

		void onPrepared(int id, int total);

		void onCompleted(int id, int total, int success);

		void onCaneceled(int id, int total, int success);

		void onError(int id, int total, int success);

	}
	
	/**
	 *	IRecvRecord
	 */
	public interface IRecvRecord {
		String getFullPath();
		boolean getResult();
	};

	/**
	 * Server UI Handler
	 * 
	 * @author Mazda
	 * 
	 */
	public interface IReceiverUI {
	
		static final String BROADCAST_CANCEL_BY_USER = "beamplus.receiver.cancel";
		static final String EXTRA_ID = "beamplus.receiver.extra.id";

		void onPrepared(int id);

		void onProgressUpdate(int id, String filename, int progress);

		void onCompleted(int id, String savedDirectory, List<IRecvRecord> RecvRecords);

		void onCanceled(int id);

		void onError(int id);
	}

	/**
	 * Interface of file transfer client
	 * 
	 */
	public interface IClient {

		void connect(String host);

		void transferFiles(Uri[] uris);

		void transferFiles(List<Uri> uris);

		void disconnect();

		void setClientEventListener(IClientEventListener listener);

		void setUIHandler(ISenderUI uiHandler);
        
        boolean isAnySessionOngoing();
	}

	/**
	 * Interface of File Transfer Server
	 * 
	 */
	public interface IServer {

		void start();

		void stop();

		void setServerEventListener(IServerEventListener listener);

		void setUIHandler(IReceiverUI uiHandler);
        
        boolean isAnySessionOngoing();
	}

	public interface IClientEventListener {

		void onDisconnected(int message);
	}

	public interface IServerEventListener {

		void onServerStarted();

		void onServerShutdown();

		void onDisconnected();
	}
}
