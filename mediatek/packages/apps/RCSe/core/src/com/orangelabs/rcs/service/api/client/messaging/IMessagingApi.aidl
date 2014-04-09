package com.orangelabs.rcs.service.api.client.messaging;

import com.orangelabs.rcs.service.api.client.messaging.IFileTransferSession;
import com.orangelabs.rcs.service.api.client.messaging.IChatSession;
import com.orangelabs.rcs.service.api.client.messaging.IMessageDeliveryListener;

/**
 * Messaging API
 */
interface IMessagingApi {
	// Transfer a file
	IFileTransferSession transferFile(in String contact, in String file);

	// Get a file transfer session from its session ID
	IFileTransferSession getFileTransferSession(in String id);

	// Get list of file transfer sessions with a contact
	List<IBinder> getFileTransferSessionsWith(in String contact);

	// Get list of current established  file transfer sessions
	List<IBinder> getFileTransferSessions();

	// Initiate a one-to-one chat session
	IChatSession initiateOne2OneChatSession(in String contact, in String firstMsg);

	// Initiate an ad-hoc group chat session
	IChatSession initiateAdhocGroupChatSession(in List<String> participants, in String subject);

	// Rejoin a group chat session
	IChatSession rejoinGroupChatSession(in String chatId);

	// Restart a group chat session
	IChatSession restartGroupChatSession(in String chatId);

	// Get a chat session from its session ID
	IChatSession getChatSession(in String id);
	
	// Get list of chat sessions with a contact
	List<IBinder> getChatSessionsWith(in String contact);

	// Get list of current established chat sessions
	List<IBinder> getChatSessions();

	// Set message delivery status
	void setMessageDeliveryStatus(in String contact, in String msgId, in String status);

	// Add message delivery listener
	void addMessageDeliveryListener(in IMessageDeliveryListener listener);

	// Remove message delivery listener
	void removeMessageDeliveryListener(in IMessageDeliveryListener listener);	

}
