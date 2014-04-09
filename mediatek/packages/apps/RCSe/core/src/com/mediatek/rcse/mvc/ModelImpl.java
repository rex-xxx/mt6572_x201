/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.rcse.mvc;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcse.activities.ChatScreenActivity;
import com.mediatek.rcse.activities.PluginProxyActivity;
import com.mediatek.rcse.activities.SettingsFragment;
import com.mediatek.rcse.activities.widgets.ContactsListManager;
import com.mediatek.rcse.activities.widgets.UnreadMessagesContainer;
import com.mediatek.rcse.api.CapabilityApi;
import com.mediatek.rcse.api.CapabilityApi.ICapabilityListener;
import com.mediatek.rcse.api.Logger;
import com.mediatek.rcse.api.Participant;
import com.mediatek.rcse.api.RegistrationApi;
import com.mediatek.rcse.api.RegistrationApi.IRegistrationStatusListener;
import com.mediatek.rcse.fragments.ChatFragment;
import com.mediatek.rcse.interfaces.ChatController;
import com.mediatek.rcse.interfaces.ChatModel.IChat;
import com.mediatek.rcse.interfaces.ChatModel.IChatManager;
import com.mediatek.rcse.interfaces.ChatModel.IChatMessage;
import com.mediatek.rcse.interfaces.ChatView;
import com.mediatek.rcse.interfaces.ChatView.IChatEventInformation.Information;
import com.mediatek.rcse.interfaces.ChatView.IChatWindow;
import com.mediatek.rcse.interfaces.ChatView.IFileTransfer;
import com.mediatek.rcse.interfaces.ChatView.IFileTransfer.Status;
import com.mediatek.rcse.interfaces.ChatView.IGroupChatWindow;
import com.mediatek.rcse.interfaces.ChatView.IOne2OneChatWindow;
import com.mediatek.rcse.plugin.message.PluginGroupChatWindow;
import com.mediatek.rcse.service.ApiManager;
import com.mediatek.rcse.service.RcsNotification;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.messaging.RichMessagingData;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.client.ClientApiException;
import com.orangelabs.rcs.service.api.client.capability.Capabilities;
import com.orangelabs.rcs.service.api.client.eventslog.EventsLogApi;
import com.orangelabs.rcs.service.api.client.messaging.IChatSession;
import com.orangelabs.rcs.service.api.client.messaging.IFileTransferEventListener;
import com.orangelabs.rcs.service.api.client.messaging.IFileTransferSession;
import com.orangelabs.rcs.service.api.client.messaging.InstantMessage;
import com.orangelabs.rcs.service.api.client.messaging.MessagingApi;
import com.orangelabs.rcs.service.api.client.messaging.MessagingApiIntents;
import com.orangelabs.rcs.utils.PhoneUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is the virtual Module part in the MVC pattern
 */
public class ModelImpl implements IChatManager {

    public static final String TAG = "ModelImpl";
    private static final String CONTACT_NAME = "contactDisplayname";
    private static final String INTENT_MESSAGE = "messages";
    private static final String EMPTY_STRING = "";
    private static final String COMMA = ", ";
    private static final String SEMICOLON = ";";
    public static final Timer TIMER = new Timer();
    public static final int INDEXT_ONE = 1;
    private static int sIdleTimeout = 0;
    private static final IChatManager INSTANCE = new ModelImpl();
    private static final HandlerThread CHAT_WORKER_THREAD = new HandlerThread("Chat Worker");
    static {
        CHAT_WORKER_THREAD.start();
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... arg0) {
                sIdleTimeout = RcsSettings.getInstance().getIsComposingTimeout() * 1000;
                return null;
            }
        };
        task.execute();
    }

    // The map retains Object&IChat&List<Participant>
    private final Map<Object, IChat> mChatMap = new HashMap<Object, IChat>();

    public static IChatManager getInstance() {
        return INSTANCE;
    }

    @Override
    public IChat addChat(List<Participant> participants, Object chatTag) {
        Logger.d(TAG, "addChat() entry, participants: " + participants + " chatTag: " + chatTag);
        int size = 0;
        IChat chat = null;
        ParcelUuid parcelUuid = null;
        if (chatTag == null) {
            UUID uuid = UUID.randomUUID();
            parcelUuid = new ParcelUuid(uuid);
        } else {
            parcelUuid = (ParcelUuid) chatTag;
        }
        Logger.d(TAG, "addChat() parcelUuid: " + parcelUuid + ",participants = " + participants);
        if (null != participants && participants.size() > 0) {
            size = participants.size();
            if (size > 1) {
                chat = new GroupChat(this, null, participants, parcelUuid);
                mChatMap.put(parcelUuid, chat);

                IGroupChatWindow chatWindow = ViewImpl.getInstance().addGroupChatWindow(parcelUuid,
                        ((GroupChat) chat).getParticipantInfos());
                ((GroupChat) chat).setChatWindow(chatWindow);
            } else {
                chat = getOne2OneChat(participants.get(0));
                if (null == chat) {
                    Logger.i(TAG, "addChat() The one-2-one chat with " + participants
                            + "doesn't exist.");
                    Participant participant = participants.get(0);
                    chat = new One2OneChat(this, null, participant, parcelUuid);
                    mChatMap.put(parcelUuid, chat);
                    String number = participant.getContact();
                    if (ContactsListManager.getInstance().isLocalContact(number)
                            || ContactsListManager.getInstance().isStranger(number)) {
                        Logger.d(TAG, "addChat() the number is local or stranger" + number);
                    } else {
                        CapabilityApi capabilityApi = ApiManager.getInstance()
                                .getCapabilityApi();
                        Logger.d(TAG,
                                "addChat() the number is not local or stranger"
                                        + number + ", capabilityApi= "
                                        + capabilityApi);
                        if (capabilityApi != null) {
                            Capabilities capability = capabilityApi
                                    .getContactCapabilities(number);
                            Logger.d(TAG, "capability = " + capability);
                            if (capability != null) {
                                if (capability.isSupportedRcseContact()) {
                                    ContactsListManager.getInstance()
                                            .setStrangerList(number, true);
                                } else {
                                    ContactsListManager.getInstance()
                                            .setStrangerList(number, false);
                                }
                            }
                        }
                    }
                    IOne2OneChatWindow chatWindow = ViewImpl.getInstance().addOne2OneChatWindow(
                            parcelUuid, participants.get(0));
                    ((One2OneChat) chat).setChatWindow(chatWindow);
                } else {
                    Logger.i(TAG, "addChat() The one-2-one chat with " + participants
                            + "has existed.");
                    ViewImpl.getInstance().switchChatWindowByTag(
                            (ParcelUuid) (((One2OneChat) chat).mTag));
                }
            }
        }
        Logger.d(TAG, "addChat(),the chat is " + chat);
        return chat;
    }

    private IChat addChat(List<Participant> participants) {
        return addChat(participants, null);
    }

    public IChat getOne2oneChatByContact(String contact) {
        Logger.i(TAG, "getOne2oneChatByContact() contact: " + contact);
        IChat chat = null;
        if (TextUtils.isEmpty(contact)) {
            return chat;
        }
        ArrayList<Participant> participant = new ArrayList<Participant>();
        participant.add(new Participant(contact, contact));
        chat = addChat(participant, null);
        return chat;
    }

    private IChat getOne2OneChat(Participant participant) {
        Logger.i(TAG, "getOne2OneChat() entry the participant is " + participant);
        Collection<IChat> chats = mChatMap.values();
        for (IChat chat : chats) {
            if (chat instanceof One2OneChat && ((One2OneChat) chat).isDuplicated(participant)) {
                Logger.d(TAG, "getOne2OneChat() find the 1-2-1 chat with " + participant
                        + " has existed");
                return chat;
            }
        }
        Logger.d(TAG, "getOne2OneChat() could not find the 1-2-1 chat with " + participant);
        return null;
    }

    private void switchGroupChat(IChat chat) {
        Logger.d(TAG, "switchGroupChat() entry, chat: " + chat);
        ParcelUuid tag = (ParcelUuid) (((GroupChat) chat).mTag);
        ViewImpl.getInstance().switchChatWindowByTag(tag);
        Logger.d(TAG, "switchGroupChat() exit, tag: " + tag);
    }

    @Override
    public IChat getChat(Object tag) {
        Logger.v(TAG, "getChat(),tag = " + tag);
        if (tag instanceof ParcelUuid) {
            Logger.v(TAG, "tgetChat(),tag instanceof ParcelUuid");
        } else {
            Logger.v(TAG, "tgetChat(),tag not instanceof ParcelUuid");
        }
        if (tag == null) {
            Logger.v(TAG, "tag is null so return null");
            return null;
        }
        if (mChatMap.isEmpty()) {
            Logger.v(TAG, "mChatMap is empty so no chat exist");
            return null;
        }
        IChat chat = mChatMap.get(tag);
        Logger.v(TAG, "return chat = " + chat);
        return chat;
    }

    @Override
    public List<IChat> listAllChat() {
        List<IChat> list = new ArrayList<IChat>();
        if (mChatMap.isEmpty()) {
            Logger.w(TAG, "mChatMap is empty");
            return list;
        }
        Collection<IChat> collection = mChatMap.values();
        Iterator<IChat> iter = collection.iterator();
        while (iter.hasNext()) {
            IChat chat = iter.next();
            if (chat != null) {
                Logger.w(TAG, "listAllChat()-IChat is empty");
                list.add(chat);
            }
        }
        return list;
    }

    @Override
    public boolean removeChat(Object tag) {
        if (tag == null) {
            Logger.w(TAG, "removeChat()-The tag is null");
            return false;
        }

        ParcelUuid uuid = new ParcelUuid(UUID.fromString(tag.toString()));
        IChat chat = mChatMap.remove(uuid);
        if (chat != null) {
            ((ChatImpl) chat).onDestroy();
            mOutGoingFileTransferManager.onChatDestroy(tag);
            ViewImpl.getInstance().removeChatWindow(((ChatImpl) chat).getChatWindow());
            return true;
        } else {
            Logger.d(TAG, "removeChat()-The chat is null");
            return false;
        }
    }

    /**
     * Remove group chat, but does not close the window associated with the
     * chat.
     * 
     * @param tag The tag of the group chat to be removed.
     * @return True if success, else false.
     */
    public boolean quitGroupChat(Object tag) {
        Logger.d(TAG, "quitGroupChat() entry, with tag is " + tag);
        if (tag == null) {
            Logger.w(TAG, "quitGroupChat() tag is null");
            return false;
        }
        IChat chat = getChat(tag);
        if (chat instanceof GroupChat) {
            ((GroupChat) chat).onQuit();
            mOutGoingFileTransferManager.onChatDestroy(tag);
            return true;
        } else {
            Logger.d(TAG, "quitGroupChat() chat is null");
            return false;
        }
    }

    /**
     * when there is not not file transfer capability, cancel all the file
     * transfer
     * 
     * @param tag The tag of the chat.
     * @param reason the reason for file transfer not available.
     */
    public void handleFileTransferNotAvailable(Object tag, int reason) {
        Logger.d(TAG, "handleFileTransferNotAvailable() entry, with tag is " + tag + " reason is "
                + reason);
        if (tag == null) {
            Logger.w(TAG, "handleFileTransferNotAvailable() tag is null");
        } else {
            mOutGoingFileTransferManager.clearTransferWithTag(tag, reason);
        }
    }

    /**
     * remove chat according the relevant participant
     * 
     * @param the participant of the chat
     */
    public void removeChatByContact(Participant participant) {
        IChat chat = getOne2OneChat(participant);
        Logger.v(TAG, "removeChatByContact(),participant = " + participant
                + ", chat = " + chat);
        if (chat != null) {
            removeChat((ChatImpl) chat);
        }
    }

    private boolean removeChat(final ChatImpl chat) {
        if (mChatMap.isEmpty()) {
            Logger.w(TAG, "removeChat()-mChatMap is empty");
            return false;
        }
        if (!mChatMap.containsValue(chat)) {
            Logger.w(TAG, "removeChat()-mChatMap didn't contains this IChat");
            return false;
        } else {
            Logger.v(TAG, "mchatMap size is " + mChatMap.size());
            mChatMap.values().remove(chat);
            Logger.v(TAG, "After removded mchatMap size is " + mChatMap.size() + ", chat = "
                    + chat);
            if (chat != null) {
                chat.onDestroy();
                mOutGoingFileTransferManager.onChatDestroy(chat.mTag);
                ViewImpl.getInstance().removeChatWindow(chat.getChatWindow());
            }
            return true;
        }
    }

    /**
     * Clear all the chat messages. Include clear all the messages in date base
     * ,clear messages in all the chat window and clear the last message in each
     * chat list item.
     */
    public void clearAllChatHistory() {
        ContentResolver contentResolver =
                ApiManager.getInstance().getContext().getContentResolver();
        contentResolver.delete(RichMessagingData.CONTENT_URI, null, null);
        List<IChat> list = INSTANCE.listAllChat();
        int size = list.size();
        Logger.d(TAG, "clearAllChatHistory(), the size of the chat list is " + size);
        for (int i = 0; i < size; i++) {
            IChat chat = list.get(i);
            if (chat != null) {
                ((ChatImpl) chat).clearChatWindowAndList();
            }
        }
    }

    /**
     * This class is the implementation of a chat message used in model part
     */
    private static class ChatMessage implements IChatMessage {
        private InstantMessage mInstantMessage;

        public ChatMessage(InstantMessage instantMessage) {
            mInstantMessage = instantMessage;
        }

        @Override
        public InstantMessage getInstantMessage() {
            return mInstantMessage;
        }
    }

    /**
     * This class is the implementation of a sent chat message used in model
     * part
     */
    public static class ChatMessageSent extends ChatMessage {
        public ChatMessageSent(InstantMessage instantMessage) {
            super(instantMessage);
        }
    }

    /**
     * This class is the implementation of a received chat message used in model
     * part
     */
    public static class ChatMessageReceived extends ChatMessage {
        public ChatMessageReceived(InstantMessage instantMessage) {
            super(instantMessage);
        }
    }

    /**
     * The call-back method of the interface called when the unread message
     * number changed
     */
    public interface UnreadMessageListener {
        /**
         * The call-back method to update the unread message when the number of
         * unread message changed
         * 
         * @param chatWindowTag The chat window tag indicates which to update
         * @param clear Whether cleared all unread message
         */
        void onUnreadMessageNumberChanged(Object chatWindowTag, boolean clear);
    }

    /**
     * It's a implementation of IChat, it indicates a specify chat model.
     */
    public abstract class ChatImpl implements IChat, IRegistrationStatusListener,
            ICapabilityListener {

        private static final String TAG = "ChatImpl";
        // Chat message list
        private final List<IChatMessage> mMessageList = new LinkedList<IChatMessage>();

        protected final AtomicReference<IChatSession> mCurrentSession =
                new AtomicReference<IChatSession>();
        protected ComposingManager mComposingManager = new ComposingManager();
        protected Object mTag = null;
        protected boolean mIsInBackground = true;

        protected IChatWindow mChatWindow = null;

        protected List<InstantMessage> mReceivedInBackgroundToBeDisplayed =
                new ArrayList<InstantMessage>();
        protected List<InstantMessage> mReceivedInBackgroundToBeRead =
                new ArrayList<InstantMessage>();

        protected RegistrationApi mRegistrationApi = null;
        protected Thread mWorkerThread = CHAT_WORKER_THREAD;
        protected Handler mWorkerHandler = new Handler(CHAT_WORKER_THREAD.getLooper());

        /**
         * Clear all the messages in chat Window and the latest message in chat
         * list.
         */
        public void clearChatWindowAndList() {
            Logger.d(TAG, "clearChatWindowAndList() entry");
            mChatWindow.removeAllMessages();
            Logger.d(TAG, "clearChatWindowAndList() exit");
        }

        /**
         * Set chat window for this chat.
         * 
         * @param chatWindow The chat window to be set.
         */
        public void setChatWindow(IChatWindow chatWindow) {
            Logger.d(TAG, "setChatWindow entry");
            mChatWindow = chatWindow;
        }

        /**
         * Add the unread message of this chat
         * 
         * @param message The unread message to add
         */
        protected void addUnreadMessage(InstantMessage message) {
            if (message.isImdnDisplayedRequested()) {
                Logger.d(TAG, "mReceivedInBackgroundToBeDisplayed = "
                        + mReceivedInBackgroundToBeDisplayed);
                if (mReceivedInBackgroundToBeDisplayed != null) {
                    mReceivedInBackgroundToBeDisplayed.add(message);
                    if (mTag instanceof UUID) {
                        ParcelUuid parcelUuid = new ParcelUuid((UUID) mTag);
                        UnreadMessagesContainer.getInstance().add(parcelUuid);
                    } else {
                        UnreadMessagesContainer.getInstance().add((ParcelUuid) mTag);
                    }
                    UnreadMessagesContainer.getInstance().loadLatestUnreadMessage();
                }
            } else {
                Logger.d(TAG, "mReceivedInBackgroundToBeRead = "
                        + mReceivedInBackgroundToBeRead);
                if (mReceivedInBackgroundToBeRead != null) {
                    mReceivedInBackgroundToBeRead.add(message);
                    if (mTag instanceof UUID) {
                        ParcelUuid parcelUuid = new ParcelUuid((UUID) mTag);
                        UnreadMessagesContainer.getInstance().add(parcelUuid);
                    } else {
                        UnreadMessagesContainer.getInstance().add(
                                (ParcelUuid) mTag);
                    }
                    UnreadMessagesContainer.getInstance()
                            .loadLatestUnreadMessage();
                }
            }
        }

        /**
         * Get unread messages of this chat.
         * 
         * @return The unread messages.
         */
        public List<InstantMessage> getUnreadMessages() {
            if (mReceivedInBackgroundToBeDisplayed != null
                    && mReceivedInBackgroundToBeDisplayed.size() > 0) {
                return mReceivedInBackgroundToBeDisplayed;
            } else {
                return mReceivedInBackgroundToBeRead;
            }
        }

        /**
         * Clear all the unread message of this chat
         */
        protected void clearUnreadMessage() {
            Logger.v(TAG,
                    "clearUnreadMessage(): mReceivedInBackgroundToBeDisplayed = "
                            + mReceivedInBackgroundToBeDisplayed
                            + ", mReceivedInBackgroundToBeRead = "
                            + mReceivedInBackgroundToBeRead);
            if (null != mReceivedInBackgroundToBeDisplayed) {
                mReceivedInBackgroundToBeDisplayed.clear();
                UnreadMessagesContainer.getInstance().remove((ParcelUuid) mTag);
                UnreadMessagesContainer.getInstance().loadLatestUnreadMessage();
            } 
            if (null != mReceivedInBackgroundToBeRead) {
                mReceivedInBackgroundToBeRead.clear();
                UnreadMessagesContainer.getInstance().remove((ParcelUuid) mTag);
                UnreadMessagesContainer.getInstance().loadLatestUnreadMessage();
            }
        }

        protected ChatImpl(Object tag) {
            mTag = tag;
            // register IRegistrationStatusListener
            ApiManager apiManager = ApiManager.getInstance();
            Logger.v(TAG, "ChatImpl() entry: apiManager = " + apiManager);
            if (null != apiManager) {
                mRegistrationApi = ApiManager.getInstance().getRegistrationApi();
                Logger.v(TAG, "mRegistrationApi = " + mRegistrationApi);
                if (mRegistrationApi != null) {
                    mRegistrationApi.addRegistrationStatusListener(this);
                }
                CapabilityApi capabilityApi = ApiManager.getInstance().getCapabilityApi();
                Logger.v(TAG, "capabilityApi = " + capabilityApi);
                if (capabilityApi != null) {
                    capabilityApi.registerCapabilityListener(this);
                }
            }
        }

        /**
         * Get the chat tag of current chat
         * 
         * @return The chat tag of current chat
         */
        public Object getChatTag() {
            return mTag;
        }

        protected void onPause() {
            Logger.v(TAG, "onPause() entry, tag: " + mTag);
            mIsInBackground = true;
        }

        protected void onResume() {
            Logger.v(TAG, "onResume() entry, tag: " + mTag);
            mIsInBackground = false;
            if (mChatWindow != null) {
                mChatWindow.updateAllMsgAsRead();
            }
            markUnreadMessageDisplayed();
            loadChatMessages(One2OneChat.LOAD_ZERO_SHOW_HEADER);
        }

        protected synchronized void onDestroy() {
            this.queryCapabilities();
            ApiManager apiManager = ApiManager.getInstance();
            Logger.v(TAG, "onDestroy() apiManager = " + apiManager);
            if (null != apiManager) {
                CapabilityApi capabilityApi = ApiManager.getInstance().getCapabilityApi();
                Logger.v(TAG, "onDestroy() capabilityApi = " + capabilityApi);
                if (capabilityApi != null) {
                    capabilityApi.unregisterCapabilityListener(this);
                }
            } 
            Logger.v(TAG, "onDestroy() mRegistrationApi = " + mRegistrationApi
                    + ",mCurrentSession = " + mCurrentSession.get());
            if (mRegistrationApi != null) {
                mRegistrationApi.removeRegistrationStatusListener(this);
            }
            if (mCurrentSession.get() != null) {
                try {
                    terminateSession();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Return the IChatWindow
         * 
         * @return IChatWindow
         */
        IChatWindow getChatWindow() {
            return mChatWindow;
        }

        protected void markMessageAsDisplayed(InstantMessage msg) {
            Logger.d(TAG, "markMessageAsDisplayed() entry");
            if (msg == null) {
                Logger.d(TAG, "markMessageAsDisplayed(),msg is null");
                return;
            }
            try {
                IChatSession tmpChatSession = mCurrentSession.get();
                SharedPreferences settings =
                        PreferenceManager.getDefaultSharedPreferences(ApiManager.getInstance()
                                .getContext());
                boolean isSendReadReceiptChecked =
                        settings.getBoolean(SettingsFragment.RCS_SEND_READ_RECEIPT, true);
                Logger.d(TAG, "markMessageAsDisplayed() ,the value of isSendReadReceiptChecked is "
                        + isSendReadReceiptChecked);
                if (tmpChatSession == null) {
                    Logger.d(TAG, "markMessageAsDisplayed() ,tmpChatSession is null");
                    return;
                }
                Logger.d(TAG,
                        "markMessageAsDisplayed() ,the value of isSendReadReceiptChecked is "
                                + isSendReadReceiptChecked);
                if (isSendReadReceiptChecked) {
                    if (tmpChatSession.isStoreAndForward()) {
                        Logger.v(TAG,
                                "markMessageAsDisplayed(),send displayed message by sip message");
                        tmpChatSession.setMessageDisplayedStatusBySipMessage(tmpChatSession
                                .getReferredByHeader(), msg.getMessageId(),
                                ImdnDocument.DELIVERY_STATUS_DISPLAYED);
                    } else {
                        Logger.v(TAG,
                                "markMessageAsDisplayed(),send displayed message by msrp message");
                        tmpChatSession.setMessageDeliveryStatus(msg.getMessageId(),
                                ImdnDocument.DELIVERY_STATUS_DISPLAYED);
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            Logger.d(TAG, "markMessageAsDisplayed() exit");
        }

        protected void markMessageAsRead(InstantMessage msg) {
            Logger.d(TAG, "markMessageAsRead() entry");
            EventsLogApi events = null;
            if (ApiManager.getInstance() != null) {
                events = ApiManager.getInstance().getEventsLogApi();
                events.markChatMessageAsRead(msg.getMessageId(), true);
            }
            Logger.d(TAG, "markMessageAsRead() exit");
        }

        protected void markUnreadMessageDisplayed() {
            Logger.v(TAG, "markUnreadMessageDisplayed() entry");
            int size = mReceivedInBackgroundToBeDisplayed.size();
            for (int i = 0; i < size; i++) {
                InstantMessage msg = mReceivedInBackgroundToBeDisplayed.get(i);
                markMessageAsDisplayed(msg);
                Logger.v(TAG, "The message " + msg.getTextMessage() + " is displayed");
            }
            size = mReceivedInBackgroundToBeRead.size();
            for (int i = 0; i < size; i++) {
                InstantMessage msg = mReceivedInBackgroundToBeRead.get(i);
                markMessageAsRead(msg);
                Logger.v(TAG, "The message " + msg.getTextMessage() + " is read");
            }
            clearUnreadMessage();
            Logger.v(TAG, "markUnreadMessageDisplayed() exit");
        }

        private void terminateSession() throws RemoteException {
            if (mCurrentSession.get() != null) {
                mCurrentSession.get().cancelSession();
                mCurrentSession.set(null);
                Logger.i(TAG, "terminateSession()---mCurrentSession cancel and is null");
            }
        }

        protected void reloadMessage(InstantMessage message, int messageType, int status) {
            Logger.w(TAG, "reloadMessage() sub-class needs to override this method");
        }

        protected void reloadFileTransfer(FileStruct fileStruct, int transferType, int status) {
            Logger.w(TAG, "reloadFileTransfer() sub-class needs to override this method");
        }

        protected class ComposingManager {
            private static final int ACT_STATE_TIME_OUT = 60 * 1000;
            private boolean mIsComposing = false;
            private static final int STARTING_COMPOSING = 1;
            private static final int STILL_COMPOSING = 2;
            private static final int MESSAGE_WAS_SENT = 3;
            private static final int ACTIVE_MESSAGE_REFRESH = 4;
            private static final int IS_IDLE = 5;
            private ComposingHandler mWorkerHandler =
                    new ComposingHandler(CHAT_WORKER_THREAD.getLooper());

            public ComposingManager() {
            }

            protected class ComposingHandler extends Handler {
                public static final String TAG = "ComposingHandler";

                public ComposingHandler(Looper looper) {
                    super(looper);
                }

                @Override
                public void handleMessage(Message msg) {
                    Logger.i(TAG, "handleMessage() the msg is:" + msg.what);
                    switch (msg.what) {
                        case STARTING_COMPOSING: 
                            if (setIsComposing(true)) {
                                mIsComposing = true;
                                mWorkerHandler.sendEmptyMessageDelayed(IS_IDLE, sIdleTimeout);
                                mWorkerHandler.sendEmptyMessageDelayed(ACTIVE_MESSAGE_REFRESH,
                                        ACT_STATE_TIME_OUT);
                            } else {
                                Logger.d(TAG,
                                        "STARTING_COMPOSING-> failed to set isComposing to true");
                            }
                            break;
                        
                        case STILL_COMPOSING: 
                            mWorkerHandler.removeMessages(IS_IDLE);
                            mWorkerHandler.sendEmptyMessageDelayed(IS_IDLE, sIdleTimeout);
                            break;
                        
                        case MESSAGE_WAS_SENT: 
                            if (setIsComposing(false)) {
                                mComposingManager.hasNoText();
                                mWorkerHandler.removeMessages(IS_IDLE);
                                mWorkerHandler.removeMessages(ACTIVE_MESSAGE_REFRESH);
                            } else {
                                Logger.d(TAG,
                                        "MESSAGE_WAS_SENT-> failed to set isComposing to false");
                            }
                            break;
                        
                        case ACTIVE_MESSAGE_REFRESH: 
                            if (setIsComposing(true)) {
                                mWorkerHandler.sendEmptyMessageDelayed(ACTIVE_MESSAGE_REFRESH,
                                        ACT_STATE_TIME_OUT);
                            } else {
                                Logger
                                        .d(TAG,
                                                "ACTIVE_MESSAGE_REFRESH-> failed to set isComposing to true");
                            }
                            break;
                        
                        case IS_IDLE: 
                            if (setIsComposing(false)) {
                                mComposingManager.hasNoText();
                                mWorkerHandler.removeMessages(ACTIVE_MESSAGE_REFRESH);
                            } else {
                                Logger.d(TAG, "IS_IDLE-> failed to set isComposing to false");
                            }
                            break;
                        
                        default: 
                            Logger.i(TAG, "handlemessage()--message" + msg.what);
                            break;
                        
                    }
                }
            }

            public void hasText(Boolean isEmpty) {
                Logger.d(TAG, "hasText() entry the edit is " + isEmpty);
                if (isEmpty) {
                    mWorkerHandler.sendEmptyMessage(MESSAGE_WAS_SENT);
                } else {
                    if (!mIsComposing) {
                        mWorkerHandler.sendEmptyMessage(STARTING_COMPOSING);
                    } else {
                        mWorkerHandler.sendEmptyMessage(STILL_COMPOSING);
                    }
                }
            }

            public void hasNoText() {
                mIsComposing = false;
            }

            public void messageWasSent() {
                mWorkerHandler.sendEmptyMessage(MESSAGE_WAS_SENT);
            }
        }

        protected boolean setIsComposing(boolean isComposing) {
            if (mCurrentSession.get() == null) {
                Logger.e(TAG, "setIsComposing() -- The chat with the tag " + " doesn't exist!");
                return false;
            } else {
                try {
                    mCurrentSession.get().setIsComposingStatus(isComposing);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }

        @Override
        public IChatMessage getSentChatMessage(int index) {
            if (index < 0 || index > mMessageList.size()) {
                return null;
            }
            return mMessageList.get(index);
        }

        @Override
        public int getChatMessageCount() {
            return mMessageList.size();
        }

        @Override
        public List<IChatMessage> listAllChatMessages() {
            return mMessageList;
        }

        @Override
        public abstract void loadChatMessages(int count);

        @Override
        public boolean removeMessage(int index) {
            if (index < 0 || index > mMessageList.size()) {
                return false;
            }
            mMessageList.remove(index);
            return true;
        }

        @Override
        public boolean removeMessages(int start, int end) {
            if (start < 0 || start > mMessageList.size()) {
                return false;
            }
            return true;
        }

        /**
         * Check capabilities before inviting a chat
         */
        protected abstract void checkCapabilities();

        /**
         * Query capabilities after terminating a chat
         */
        protected abstract void queryCapabilities();

        @Override
        public void hasTextChanged(boolean isEmpty) {
            mComposingManager.hasText(isEmpty);
        }

        @Override
        public abstract void onCapabilityChanged(String contact, Capabilities capabilities);

        @Override
        public abstract void onStatusChanged(boolean status);
    }

    private boolean isChatExisted(Participant participant) {
        Logger.v(TAG, "isHaveChat() The participant is " + participant);
        boolean bIsHaveChat = false;
        // Find the chat in the chat list
        Collection<IChat> chatList = mChatMap.values();
        if (chatList != null) {
            for (IChat chat : chatList) {
                if (chat instanceof One2OneChat) {
                    if (null != participant && (((One2OneChat) chat).isDuplicated(participant))) {
                        bIsHaveChat = true;
                    }
                }
            }
        }
        Logger.i(TAG, "isHaveChat() end, bIsHaveChat = " + bIsHaveChat);
        return bIsHaveChat;
    }

    @Override
    public boolean handleInvitation(Intent intent, boolean isCheckSessionExist) {
        Logger.v(TAG, "handleInvitation entry");
        String action = null;
        if (intent == null) {
            Logger.d(TAG, "handleInvitation intent is null");
            return false;
        } else {
            action = intent.getAction();
        }
        if (MessagingApiIntents.CHAT_SESSION_REPLACED.equalsIgnoreCase(action)) {
            Logger.d(TAG, " handleInvitation() the action is CHAT_SESSION_REPLACED ");
        } else if (MessagingApiIntents.CHAT_INVITATION.equalsIgnoreCase(action)) {
            return handleChatInvitation(intent, isCheckSessionExist);
        }
        Logger.v(TAG, "handleInvitation exit: action = " + action);
        return false;
    }

    private boolean handleChatInvitation(Intent intent, boolean isCheckSessionExist) {
        ApiManager instance = ApiManager.getInstance();
        String sessionId = intent.getStringExtra("sessionId");
        if (instance != null) {
            MessagingApi messageApi = instance.getMessagingApi();
            if (messageApi != null) {
                try {
                    IChatSession chatSession = messageApi.getChatSession(sessionId);
                    if (chatSession == null) {
                        Logger.e(TAG, "The getChatSession is null");
                        return false;
                    }
                    List<String> participants = chatSession.getInivtedParticipants();
                    if (participants == null || participants.size() == 0) {
                        Logger.e(TAG, "The getParticipants is null, or size is 0");
                        return false;
                    }
                    int participantCount = participants.size();
                    ArrayList<InstantMessage> messages =
                            intent.getParcelableArrayListExtra(INTENT_MESSAGE);
                    ArrayList<IChatMessage> chatMessages = new ArrayList<IChatMessage>();
                    if (null != messages) {
                        int size = messages.size();
                        for (int i = 0; i < size; i++) {
                            InstantMessage msg = messages.get(i);
                            Logger.d(TAG, "InstantMessage:" + msg);
                            if (msg != null) {
                                chatMessages.add(new ChatMessageReceived(msg));
                            }
                        }
                    }
                    if (participantCount == 1) {
                        List<Participant> participantsList = new ArrayList<Participant>();
                        String remoteParticipant = participants.get(0);
                        String number = PhoneUtils.extractNumberFromUri(remoteParticipant);
                        String name = intent.getStringExtra(CONTACT_NAME);
                        Participant fromSessionParticipant = new Participant(number, name);
                        participantsList.add(fromSessionParticipant);
                        if (!isCheckSessionExist) {
                            IChat currentChat = addChat(participantsList);
                            currentChat.handleInvitation(chatSession, chatMessages);
                            return true;
                        } else {
                            if (isChatExisted(fromSessionParticipant)) {
                                IChat currentChat = addChat(participantsList);
                                currentChat.handleInvitation(chatSession, chatMessages);
                                Logger.v(TAG, "handleInvitation exit with true");
                                return true;
                            } else {
                                Logger.v(TAG, "handleInvitation exit with false");
                                return false;
                            }
                        }
                    } else if (participantCount > 1) {
                        List<Participant> participantsList = new ArrayList<Participant>();
                        for (int i = 0; i < participantCount; i++) {
                            String remoteParticipant = participants.get(i);
                            String number = PhoneUtils.extractNumberFromUri(remoteParticipant);
                            String name = number;
                            if (PhoneUtils.isANumber(number)) {
                                name = ContactsListManager.getInstance().getDisplayNameByPhoneNumber(number);
                            } else {
                                Logger
                                        .e(TAG, "the participant " + number
                                                + " is not a real number");
                            }
                            Participant fromSessionParticipant = new Participant(number, name);
                            participantsList.add(fromSessionParticipant);
                        }
                        String chatId = intent.getStringExtra(RcsNotification.CHAT_ID);
                        ParcelUuid tag = (ParcelUuid) intent
                                .getParcelableExtra(ChatScreenActivity.KEY_CHAT_TAG);
                        IChat chat = getGroupChat(chatId);
                        if (chat == null) {
                            chat = addChat(participantsList, tag);
                        } else {
                            // restart chat.
                            chatMessages.clear();
                        }
                        chat.handleInvitation(chatSession, chatMessages);
                        return true;
                    } else {
                        Logger.e(TAG, "Illegal paticipants");
                        return false;
                    }
                } catch (ClientApiException e) {
                    Logger.e(TAG, "getChatSession fail");
                    e.printStackTrace();
                } catch (RemoteException e) {
                    Logger.e(TAG, "getParticipants fail");
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    public boolean handleFileTransferInvitation(String sessionId) {
        ApiManager instance = ApiManager.getInstance();
        if (instance != null) {
            MessagingApi messageApi = instance.getMessagingApi();
            if (messageApi != null) {
                try {
                    IFileTransferSession fileTransferSession =
                            messageApi.getFileTransferSession(sessionId);
                    if (fileTransferSession == null) {
                        Logger.e(TAG,
                                "handleFileTransferInvitation-The getFileTransferSession is null");
                        return false;
                    }

                    List<Participant> participantsList = new ArrayList<Participant>();
                    String number =
                            PhoneUtils.extractNumberFromUri(fileTransferSession.getRemoteContact());

                    // Get the contact name from contact list
                    String name = EMPTY_STRING;
                    Logger.e(TAG, "handleFileTransferInvitation, number = " + name);
                    if (null != number) {
                        String tmpContact = ContactsListManager.getInstance().getDisplayNameByPhoneNumber(number);
                        if (tmpContact != null) {
                            name = tmpContact;
                        } else {
                            name = number;
                        }
                    }

                    Participant fromSessionParticipant = new Participant(number, name);
                    participantsList.add(fromSessionParticipant);
                    if (isChatExisted(fromSessionParticipant)) {
                        IChat chat = addChat(participantsList);
                        ((One2OneChat) chat).addReceiveFileTransfer(fileTransferSession);
                        if (!((One2OneChat) chat).mIsInBackground) {
                            Logger.v(TAG, "handleFileTransferInvitation()" +
                                     "-handleInvitation exit with true, is the current chat!");
                            return true;
                        } else {
                            Logger.v(TAG, "handleFileTransferInvitation()" +
                                     "-handleInvitation exit with true, is not the current chat!");
                            return false;
                        }

                    } else {
                        Logger.v(TAG,
                                "handleFileTransferInvitation-handleInvitation exit with false");
                        IChat chat = addChat(participantsList);
                        ((One2OneChat) chat).addReceiveFileTransfer(fileTransferSession);
                        return false;
                    }
                } catch (ClientApiException e) {
                    Logger.e(TAG, "handleFileTransferInvitation-getChatSession fail");
                    e.printStackTrace();
                } catch (RemoteException e) {
                    Logger.e(TAG, "handleFileTransferInvitation-getParticipants fail");
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    public void handleMessageDeliveryStatus(String contact, String msgId,
            String status, long timeStamp) {
        IChat chat = getOne2OneChat(new Participant(contact, contact));
        Logger.d(TAG, "handleMessageDeliveryStatus() entry, contact:" + contact
                + " msgId:" + msgId + " status:" + status + " ,timeStamp: "
                + timeStamp + ",chat = " + chat);
        if (null != chat) {
            ((One2OneChat) chat).onMessageDelivered(msgId, status, timeStamp);
        }
    }

    /**
     * Called by the controller when the user needs to cancel an on-going file
     * transfer
     * 
     * @param tag The chat window tag where the file transfer is in
     * @param fileTransferTag The tag indicating which file transfer will be
     *            canceled
     */
    public void handleCancelFileTransfer(Object tag, Object fileTransferTag) {
        Logger.v(TAG, "handleCancelFileTransfer(),tag = " + tag + ",fileTransferTag = "
                + fileTransferTag);
        IChat chat = getChat(tag);
        if (chat == null) {
            chat = getOne2oneChatByContact((String) tag);
        }
        if (chat instanceof One2OneChat) {
            One2OneChat oneOneChat = ((One2OneChat) chat);
            oneOneChat.handleCancelFileTransfer(fileTransferTag);
        }
        Logger.d(TAG, "handleCancelFileTransfer() it's a sent file transfer");
        mOutGoingFileTransferManager.cancelFileTransfer(fileTransferTag);
    }

    /**
     * Called by the controller when the user needs to resend a file
     * 
     * @param fileTransferTag The tag of file which the user needs to resend
     */
    public void handleResendFileTransfer(Object fileTransferTag) {
        mOutGoingFileTransferManager.resendFileTransfer(fileTransferTag);
    }

    /**
     * This class represents a file structure used to be shared
     */
    public static class FileStruct {
        public static final String TAG = "FileStruct";

        /**
         * Generate a file struct instance using a session and a path, this
         * method should only be called for Received File Transfer
         * 
         * @param fileTransferSession The session of the file transfer
         * @param filePath The path of the file
         * @return The file struct instance
         * @throws RemoteException
         */
        public static FileStruct from(IFileTransferSession fileTransferSession, String filePath)
                throws RemoteException {
            FileStruct fileStruct = null;
            String fileName = fileTransferSession.getFilename();
            long fileSize = fileTransferSession.getFilesize();
            String sessionId = fileTransferSession.getSessionID();
            Date date = new Date();
            fileStruct = new FileStruct(filePath, fileName, fileSize, sessionId, date);
            return fileStruct;
        }

        /**
         * Generate a file struct instance using a path, this method should only
         * be called for Sent File Transfer
         * 
         * @param filePath The path of the file
         * @return The file struct instance
         */
        public static FileStruct from(String filePath) {
            FileStruct fileStruct = null;
            File file = new File(filePath);
            if (file.exists()) {
                Date date = new Date();
                fileStruct =
                        new FileStruct(filePath, file.getName(), file.length(), new ParcelUuid(UUID
                                .randomUUID()), date);
            }
            Logger.d(TAG, "from() fileStruct: " + fileStruct);
            return fileStruct;
        }

        public FileStruct(String filePath, String name, long size, Object fileTransferTag, Date date) {
            mFilePath = filePath;
            mName = name;
            mSize = size;
            mFileTransferTag = fileTransferTag;
            mDate = (Date) date.clone();
        }

        public FileStruct(String filePath, String name, long size, Object fileTransferTag, Date date, String remote) {
            mFilePath = filePath;
            mName = name;
            mSize = size;
            mFileTransferTag = fileTransferTag;
            mDate = (Date) date.clone();
            mRemote = remote;
        }

        public String mFilePath = null;

        public String mName = null;

        public long mSize = -1;

        public Object mFileTransferTag = null;

        public Date mDate = null;

        public String mRemote = null;

        public String toString() {
            return TAG + "file path is " + mFilePath + " file name is " + mName + " size is "
                    + mSize + " FileTransferTag is " + mFileTransferTag + " date is " + mDate;
        }
    }

    /**
     * This class represents a chat event structure used to be shared
     */
    public static class ChatEventStruct {
        public static final String TAG = "ChatEventStruct";

        public ChatEventStruct(Information info, Object relatedInfo, Date dt) {
            information = info;
            relatedInformation = relatedInfo;
            date = (Date) dt.clone();
        }

        public Information information = null;

        public Object relatedInformation = null;

        public Date date = null;

        public String toString() {
            return TAG + "information is " + information + " relatedInformation is "
                    + relatedInformation + " date is " + date;
        }
    }

    /**
     * Clear all history of the chats, include both one2one chat and group chat.
     * 
     * @return True if successfully clear, false otherwise.
     */
    public boolean clearAllHistory() {
        Logger.d(TAG, "clearAllHistory() entry");
        ControllerImpl controller = ControllerImpl.getInstance();
        boolean result = false;
        if (null != controller) {
            Message controllerMessage = controller.obtainMessage(
                    ChatController.EVENT_CLEAR_CHAT_HISTORY, null, null);
            controllerMessage.sendToTarget();
            result = true;
        }
        Logger.d(TAG, "clearAllHistory() exit with result = " + result);
        return result;
    }

    /**
     * Called by the controller when the user needs to start a file transfer
     * 
     * @param target The tag of the chat window in which the file transfer
     *            starts or the contact to receive this file
     * @param filePath The path of the file to be transfered
     */
    public void handleSendFileTransferInvitation(Object target, String filePath,
            Object fileTransferTag) {
        Logger.d(TAG, "handleSendFileTransferInvitation() user starts to send file " + filePath
                + " to target " + target + ", fileTransferTag: " + fileTransferTag);
        IChat chat = null;
        if (target instanceof String) {
            String contact = (String) target;
            List<Participant> participants = new ArrayList<Participant>();
            participants.add(new Participant(contact,contact));
            chat = addChat(participants);
        } else {
            chat = getChat(target);
        }

        Logger.d(TAG, "handleSendFileTransferInvitation() chat: " + chat);
        if (chat instanceof One2OneChat) {
            Participant participant = ((One2OneChat) chat).getParticipant();
            Logger.d(TAG, "handleSendFileTransferInvitation() user starts to send file " + filePath
                    + " to " + participant + ", fileTransferTag: " + fileTransferTag);
            mOutGoingFileTransferManager.onAddSentFileTransfer(((One2OneChat) chat).generateSentFileTransfer(
                    filePath, fileTransferTag));
        } 
    }

    /**
     * This class describe one single out-going file transfer, and control the
     * status itself
     */
    public static class SentFileTransfer {
        private static final String TAG = "SentFileTransfer";
        public static final String KEY_FILE_TRANSFER_TAG = "file transfer tag";

        protected Object mChatTag = null;

        protected Object mFileTransferTag = null;

        protected FileStruct mFileStruct = null;

        protected IFileTransfer mFileTransfer = null;

        protected IFileTransferSession mFileTransferSession = null;

        protected IFileTransferEventListener mFileTransferListener = null;

        protected Participant mParticipant = null;

        public SentFileTransfer(Object chatTag, IOne2OneChatWindow one2OneChat,
                String filePath, Participant participant, Object fileTransferTag) {
            Logger.d(TAG, "SentFileTransfer() constructor chatTag is "
                    + chatTag + " one2OneChat is " + one2OneChat
                    + " filePath is " + filePath + "fileTransferTag: "
                    + fileTransferTag);
            if (null != chatTag && null != one2OneChat && null != filePath
                    && null != participant) {
                mChatTag = chatTag;
                mFileTransferTag = (fileTransferTag != null ? fileTransferTag
                        : UUID.randomUUID());
                mFileStruct = new FileStruct(filePath,
                        extractFileNameFromPath(filePath), 0, mFileTransferTag,
                        new Date());
                mFileTransfer = one2OneChat.addSentFileTransfer(mFileStruct);
                mFileTransfer.setStatus(Status.PENDING);
                mParticipant = participant;
            }
        }

        protected void send() {
            ApiManager instance = ApiManager.getInstance();
            if (instance != null) {
                MessagingApi messageApi = instance.getMessagingApi();
                if (messageApi != null) {
                    try {
                        mFileTransferSession =
                                messageApi.transferFile(mParticipant.getContact(),
                                        mFileStruct.mFilePath);
                        if (null != mFileTransferSession) {
                            mFileStruct.mSize = mFileTransferSession.getFilesize();
                            String sessionId = mFileTransferSession.getSessionID();
                            mFileTransfer.updateTag(sessionId,
                                    mFileStruct.mSize);
                            mFileTransferTag = sessionId;
                            mFileStruct.mFileTransferTag = sessionId;
                            mFileTransferListener = new FileTransferSenderListener();
                            mFileTransferSession.addSessionListener(mFileTransferListener);
                            mFileTransfer.setStatus(Status.WAITING);
                            setNotification();
                        } else {
                            Logger.e(TAG,
                                    "send() failed, mFileTransferSession is null, filePath is "
                                            + mFileStruct.mFilePath);
                            onFailed();
                            onFileTransferFinished(IOnSendFinishListener.Result.REMOVABLE);
                        }
                    } catch (ClientApiException e) {
                        e.printStackTrace();
                        onFailed();
                        onFileTransferFinished(IOnSendFinishListener.Result.REMOVABLE);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        onFailed();
                        onFileTransferFinished(IOnSendFinishListener.Result.REMOVABLE);
                    }
                }
            }
        }

        private void onPrepareResend() {
            Logger.d(TAG, "onPrepareResend() file " + mFileStruct.mFilePath
                    + " to " + mParticipant);
            if (null != mFileTransfer) {
                mFileTransfer.setStatus(Status.PENDING);
            }
        }

        private void onCancel() {
            Logger.d(TAG, "onCancel() entry: mFileTransfer = " + mFileTransfer);
            if (null != mFileTransfer) {
                mFileTransfer.setStatus(Status.CANCEL);
            }
        }

        private void onFailed() {
            Logger.d(TAG, "onFailed() entry: mFileTransfer = " + mFileTransfer);
            if (null != mFileTransfer) {
                mFileTransfer.setStatus(Status.FAILED);
            }
        }

        private void onNotAvailable(int reason) {
            Logger.d(TAG, "onNotAvailable() reason is " + reason);
            if (One2OneChat.FILETRANSFER_ENABLE_OK == reason) {
                return;
            } else {
                switch (reason) {
                    case One2OneChat.FILETRANSFER_DISABLE_REASON_REMOTE:
                        onFailed();
                        break;
                    case One2OneChat.FILETRANSFER_DISABLE_REASON_CAPABILITY_FAILED:
                    case One2OneChat.FILETRANSFER_DISABLE_REASON_NOT_REGISTER:
                        onCancel();
                        break;
                    default:
                        Logger.w(TAG, "onNotAvailable() unknown reason " + reason);
                        break;
                }
            }
        }

        private void onDestroy() {
            Logger.d(TAG, "onDestroy() sent file transfer mFilePath "
                    + ((null == mFileStruct) ? null : mFileStruct.mFilePath)
                    + " mFileTransferSession = " + mFileTransferSession
                    + ", mFileTransferListener = " + mFileTransferListener);
            if (null != mFileTransferSession) {
                try {
                    if (null != mFileTransferListener) {
                        mFileTransferSession
                                .removeSessionListener(mFileTransferListener);
                    }
                    cancelNotification();
                    mFileTransferSession.cancelSession();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        protected void onFileTransferFinished(
                IOnSendFinishListener.Result result) {
            Logger.d(TAG, "onFileTransferFinished() mFileStruct = "
                    + mFileStruct + ", file = "
                    + ((null == mFileStruct) ? null : mFileStruct.mFilePath)
                    + ", mOnSendFinishListener = " + mOnSendFinishListener
                    + ", mFileTransferListener = " + mFileTransferListener
                    + ", result = " + result);
            if (null != mOnSendFinishListener) {
                mOnSendFinishListener.onSendFinish(SentFileTransfer.this,
                        result);
                if (null != mFileTransferSession) {
                    try {
                        if (null != mFileTransferListener) {
                            mFileTransferSession
                                    .removeSessionListener(mFileTransferListener);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public static String extractFileNameFromPath(String filePath) {
            if (null != filePath) {
                int lastDashIndex = filePath.lastIndexOf("/");
                if (-1 != lastDashIndex && lastDashIndex < filePath.length() - 1) {
                    String fileName = filePath.substring(lastDashIndex + 1);
                    return fileName;
                } else {
                    Logger.e(TAG, "extractFileNameFromPath() invalid file path:" + filePath);
                    return null;
                }
            } else {
                Logger.e(TAG, "extractFileNameFromPath() filePath is null");
                return null;
            }
        }

        /**
         * File transfer session event listener
         */
        private class FileTransferSenderListener extends IFileTransferEventListener.Stub {
            private static final String TAG = "FileTransferSenderListener";

            // Session is started
            @Override
            public void handleSessionStarted() {
                Logger.d(TAG, "handleSessionStarted() this file is " + mFileStruct.mFilePath);
            }

            // Session has been aborted
            @Override
            public void handleSessionAborted() {
                Logger.v(TAG,
                        "File transfer handleSessionAborted(): mFileTransfer = "
                                + mFileTransfer);
                if (mParticipant == null) {
                    Logger.d(TAG,
                            "FileTransferSenderListener handleSessionAborted mParticipant is null");
                    return;
                }
                if (mFileTransfer != null) {
                    mFileTransfer.setStatus(Status.CANCEL);
                    IChat chat = ModelImpl.getInstance().getChat(mChatTag);
                    Logger.v(TAG, "handleSessionAborted(): chat = " + chat);
                    if (chat instanceof ChatImpl) {
                        ((ChatImpl) chat).checkCapabilities();
                    }
                }
                try {
                    mFileTransferSession.removeSessionListener(this);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                mFileTransferSession = null;
                onFileTransferFinished(IOnSendFinishListener.Result.REMOVABLE);
            }

            // Session has been terminated by remote
            @Override
            public void handleSessionTerminatedByRemote() {
                Logger.v(TAG,
                        "File transfer handleSessionTerminatedByRemote(): mFileTransfer = "
                                + mFileTransfer);
                if (mFileTransfer != null) {
                    mFileTransfer.setStatus(Status.CANCELED);
                    IChat chat = ModelImpl.getInstance().getChat(mChatTag);
                    Log.v(TAG, "chat = " + chat);
                    if (chat instanceof ChatImpl) {
                        ((ChatImpl) chat).checkCapabilities();
                    }
                }
                mFileTransferSession = null;
                onFileTransferFinished(IOnSendFinishListener.Result.REMOVABLE);
            }

            // File transfer progress
            @Override
            public void handleTransferProgress(final long currentSize, final long totalSize) {
                // Because of can't received file transfered callback, so add
                // current size equals total size change status to finished
                Logger.d(TAG, "handleTransferProgress() the file "
                        + mFileStruct.mFilePath + " with " + mParticipant
                        + " is transferring, currentSize is " + currentSize
                        + " total size is " + totalSize + ", mFileTransfer = "
                        + mFileTransfer);
                if (mFileTransfer != null) {
                    if (currentSize < totalSize) {
                        mFileTransfer
                                .setStatus(com.mediatek.rcse.interfaces.ChatView.IFileTransfer.Status.TRANSFERING);
                        mFileTransfer.setProgress(currentSize);
                        updateNotification(currentSize);
                    } else {
                        mFileTransfer
                                .setStatus(com.mediatek.rcse.interfaces.ChatView.IFileTransfer.Status.FINISHED);
                        onFileTransferFinished(IOnSendFinishListener.Result.REMOVABLE);
                    }
                } 
            }

            // File transfer error
            @Override
            public void handleTransferError(final int error) {
                Logger.v(TAG, "handleTransferError(),error = " + error + ", mFileTransfer ="
                        + mFileTransfer);
                switch (error) {
                    case FileSharingError.SESSION_INITIATION_FAILED:
                        Logger.d(TAG,
                                "handleTransferError(), the file transfer invitation is failed.");
                        if (mFileTransfer != null) {
                            mFileTransfer.setStatus(ChatView.IFileTransfer.Status.FAILED);
                        }
                        onFileTransferFinished(IOnSendFinishListener.Result.REMOVABLE);
                        break;
                        
                    case FileSharingError.SESSION_INITIATION_TIMEOUT:
                        Logger.d(TAG,
                                "handleTransferError(), the file transfer invitation is failed.");
                        if (mFileTransfer != null) {
                            mFileTransfer.setStatus(ChatView.IFileTransfer.Status.TIMEOUT);
                        }
                        onFileTransferFinished(IOnSendFinishListener.Result.REMOVABLE);
                        break;
                        
                    case FileSharingError.SESSION_INITIATION_DECLINED:
                        Logger.d(TAG,
                                "handleTransferError(), your file transfer invitation has been rejected");
                        if (mFileTransfer != null) {
                            mFileTransfer.setStatus(ChatView.IFileTransfer.Status.REJECTED);
                        }
                        onFileTransferFinished(IOnSendFinishListener.Result.RESENDABLE);
                        break;
                    case FileSharingError.SESSION_INITIATION_CANCELLED:
                        Logger.d(TAG,
                                "handleTransferError(), your file transfer invitation has been rejected");
                        if (mFileTransfer != null) {
                            mFileTransfer.setStatus(ChatView.IFileTransfer.Status.TIMEOUT);
                        }
                        onFileTransferFinished(IOnSendFinishListener.Result.REMOVABLE);
                        break;
                    default:
                        Logger.e(TAG, "handleTransferError() unknown error " + error);
                        onFailed();
                        onFileTransferFinished(IOnSendFinishListener.Result.REMOVABLE);
                        break;
                }
            }

            // File has been transfered
            @Override
            public void handleFileTransfered(final String fileName) {
                Logger.d(TAG, "handleFileTransfered() entry, fileName is "
                        + fileName + ", mFileTransfer = " + mFileTransfer);
                if (mFileTransfer != null) {
                    mFileTransfer
                            .setStatus(com.mediatek.rcse.interfaces.ChatView.IFileTransfer.Status.FINISHED);
                }
                onFileTransferFinished(IOnSendFinishListener.Result.REMOVABLE);
            }
        }

        private NotificationManager getNotificationManager() {
            ApiManager apiManager = ApiManager.getInstance();
            if (null != apiManager) {
                Context context = apiManager.getContext();
                if (null != context) {
                    Object systemService = context.getSystemService(Context.NOTIFICATION_SERVICE);
                    return (NotificationManager) systemService;
                } else {
                    Logger.e(TAG, "getNotificationManager() context is null");
                    return null;
                }
            } else {
                Logger.e(TAG, "getNotificationManager() apiManager is null");
                return null;
            }
        }

        private int getNotificationId() {
            if (null != mFileTransferTag) {
                return mFileTransferTag.hashCode();
            } else {
                Logger.w(TAG, "getNotificationId() mFileTransferTag: " + mFileTransferTag);
                return 0;
            }
        }

        private void setNotification() {
            updateNotification(0);
        }

        private void updateNotification(long currentSize) {
            Logger.d(TAG, "updateNotification() entry, currentSize is "
                    + currentSize + ", mFileTransferTag is " + mFileTransferTag
                    + ", mFileStruct = " + mFileStruct);
            NotificationManager notificationManager = getNotificationManager();
            if (null != notificationManager) {
                if (null != mFileStruct) {
                    Context context = ApiManager.getInstance().getContext();
                    Notification.Builder builder = new Notification.Builder(context);
                    builder
                            .setProgress((int) mFileStruct.mSize, (int) currentSize,
                                    currentSize < 0);
                    String title =
                            context.getResources().getString(
                                    R.string.ft_progress_bar_title,
                                            ContactsListManager.getInstance().getDisplayNameByPhoneNumber(
                                                    mParticipant
                                            .getContact()));
                    builder.setContentTitle(title);
                    builder.setAutoCancel(false);
                    builder.setContentText(extractFileNameFromPath(mFileStruct.mFilePath));
                    builder.setContentInfo(buildPercentageLabel(context, mFileStruct.mSize,
                            currentSize));
                    builder.setSmallIcon(R.drawable.rcs_notify_file_transfer);
                    PendingIntent pendingIntent;
                    if (Logger.getIsIntegrationMode()) {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_SENDTO);
                        Uri uri = Uri.parse(PluginProxyActivity.MMSTO + mParticipant.getContact());
                        intent.setData(uri);
                        pendingIntent =
                            PendingIntent.getActivity(context, 0, intent,
                                    PendingIntent.FLAG_UPDATE_CURRENT);
                    } else {
                        Intent intent = new Intent(context, ChatScreenActivity.class);
                        intent.putExtra(ChatScreenActivity.KEY_CHAT_TAG, (ParcelUuid) mChatTag);
                        pendingIntent = PendingIntent.getActivity(context, 0, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT);
                    }
                    builder.setContentIntent(pendingIntent);
                    notificationManager.notify(getNotificationId(), builder.getNotification());
                }
            }
        }

        private void cancelNotification() {
            NotificationManager notificationManager = getNotificationManager();
            Logger.d(TAG, "cancelNotification() entry, mFileTransferTag is "
                    + mFileTransferTag + ",notificationManager = "
                    + notificationManager);
            if (null != notificationManager) {
                notificationManager.cancel(getNotificationId());
            } 
        }

        private static String buildPercentageLabel(Context context, long totalBytes,
                long currentBytes) {
            if (totalBytes <= 0) {
                return null;
            } else {
                final int percent = (int) (100 * currentBytes / totalBytes);
                return context.getString(R.string.ft_percent, percent);
            }
        }

        protected IOnSendFinishListener mOnSendFinishListener = null;

        protected interface IOnSendFinishListener {
            static enum Result {
                REMOVABLE, // This kind of result indicates that this File
                // transfer should be removed from the manager
                RESENDABLE
                // This kind of result indicates that this File transfer will
                // have a chance to be resent in the future
            };

            void onSendFinish(SentFileTransfer sentFileTransfer, Result result);
        }
    }

    private SentFileTransferManager mOutGoingFileTransferManager = new SentFileTransferManager();

    /**
     * This class is used to manage the whole sent file transfers and make it
     * work in queue
     */
    private static class SentFileTransferManager implements SentFileTransfer.IOnSendFinishListener {
        private static final String TAG = "SentFileTransferManager";

        private static final int MAX_ACTIVATED_SENT_FILE_TRANSFER_NUM = 1;

        private ConcurrentLinkedQueue<SentFileTransfer> mPendingList =
                new ConcurrentLinkedQueue<SentFileTransfer>();

        private CopyOnWriteArrayList<SentFileTransfer> mActiveList =
                new CopyOnWriteArrayList<SentFileTransfer>();

        private CopyOnWriteArrayList<SentFileTransfer> mResendableList =
                new CopyOnWriteArrayList<SentFileTransfer>();

        private synchronized void checkNext() {
            int activatedNum = mActiveList.size();
            if (activatedNum < MAX_ACTIVATED_SENT_FILE_TRANSFER_NUM) {
                Logger.d(TAG, "checkNext() current activatedNum is " + activatedNum
                        + " will find next file transfer to send");
                SentFileTransfer nextFileTransfer = mPendingList.poll();
                if (null != nextFileTransfer) {
                    Logger.d(TAG, "checkNext() next file transfer found, just send it!");
                    nextFileTransfer.send();
                    mActiveList.add(nextFileTransfer);
                } else {
                    Logger.d(TAG, "checkNext() next file transfer not found, pending list is null");
                }
            } else {
                Logger.d(TAG, "checkNext() current activatedNum is " + activatedNum
                        + " MAX_ACTIVATED_SENT_FILE_TRANSFER_NUM is "
                        + MAX_ACTIVATED_SENT_FILE_TRANSFER_NUM
                        + " so no need to find next pending file transfer");
            }
        }

        public void onAddSentFileTransfer(SentFileTransfer sentFileTransfer) {
            Logger.d(TAG, "onAddSentFileTransfer() entry, sentFileTransfer =  "
                    + sentFileTransfer);
            if (null != sentFileTransfer) {
                Logger.d(TAG, "onAddSentFileTransfer() entry, file "
                        + sentFileTransfer.mFileStruct + " is going to be sent");
                sentFileTransfer.mOnSendFinishListener = this;
                mPendingList.add(sentFileTransfer);
                checkNext();
            }
        }

        public void onChatDestroy(Object tag) {
            Logger.d(TAG, "onChatDestroy entry, tag is " + tag);
            clearTransferWithTag(tag, One2OneChat.FILETRANSFER_ENABLE_OK);
        }

        public void clearTransferWithTag(Object tag, int reason) {
            Logger.d(TAG, "onFileTransferNotAvalible() entry tag is " + tag + " reason is "
                    + reason);
            if (null != tag) {
                Logger.d(TAG, "onFileTransferNotAvalible() tag is " + tag);
                ArrayList<SentFileTransfer> toBeDeleted = new ArrayList<SentFileTransfer>();
                for (SentFileTransfer fileTransfer : mActiveList) {
                    if (tag.equals(fileTransfer.mChatTag)) {
                        Logger.d(TAG,
                                "onFileTransferNotAvalible() sent file transfer with chatTag "
                                        + tag + " found in activated list");
                        fileTransfer.onNotAvailable(reason);
                        fileTransfer.onDestroy();
                        toBeDeleted.add(fileTransfer);
                    }
                }
                if (toBeDeleted.size() > 0) {
                    Logger
                            .d(TAG,
                                    "onFileTransferNotAvalible() need to remove some file transfer from activated list");
                    mActiveList.removeAll(toBeDeleted);
                    toBeDeleted.clear();
                }
                for (SentFileTransfer fileTransfer : mPendingList) {
                    if (tag.equals(fileTransfer.mChatTag)) {
                        Logger.d(TAG,
                                "onFileTransferNotAvalible() sent file transfer with chatTag "
                                        + tag + " found in pending list");
                        fileTransfer.onNotAvailable(reason);
                        toBeDeleted.add(fileTransfer);
                    }
                }
                if (toBeDeleted.size() > 0) {
                    Logger
                            .d(TAG,
                                    "onFileTransferNotAvalible() need to remove some file transfer from pending list");
                    mPendingList.removeAll(toBeDeleted);
                    toBeDeleted.clear();
                }
                for (SentFileTransfer fileTransfer : mResendableList) {
                    if (tag.equals(fileTransfer.mChatTag)) {
                        Logger.d(TAG,
                                "onFileTransferNotAvalible() sent file transfer with chatTag "
                                        + tag + " found in mResendableList list");
                        fileTransfer.onNotAvailable(reason);
                        toBeDeleted.add(fileTransfer);
                    }
                }
                if (toBeDeleted.size() > 0) {
                    Logger.d(TAG, "onFileTransferNotAvalible() " +
                            "need to remove some file transfer from mResendableList list");
                    mResendableList.removeAll(toBeDeleted);
                    toBeDeleted.clear();
                }
            }
        }

        public void resendFileTransfer(Object targetFileTransferTag) {
            SentFileTransfer fileTransfer = findResendableFileTransfer(targetFileTransferTag);
            Logger.d(TAG, "resendFileTransfer() the file transfer with tag "
                    + targetFileTransferTag + " is " + fileTransfer);
            if (null != fileTransfer) {
                fileTransfer.onPrepareResend();
                Logger.d(TAG, "resendFileTransfer() the file transfer with tag "
                        + targetFileTransferTag
                        + " found, remove it from resendable list and add it into pending list");
                mResendableList.remove(fileTransfer);
                mPendingList.add(fileTransfer);
                checkNext();
            }
        }

        public void cancelFileTransfer(Object targetFileTransferTag) {
            Logger.d(TAG, "cancelFileTransfer() begin to cancel file transfer with tag "
                    + targetFileTransferTag);
            SentFileTransfer fileTransfer = findPendingFileTransfer(targetFileTransferTag);
            if (null != fileTransfer) {
                Logger.d(TAG, "cancelFileTransfer() the target file transfer with tag "
                        + targetFileTransferTag + " found in pending list");
                fileTransfer.onCancel();
                mPendingList.remove(fileTransfer);
            } else {
                fileTransfer = findActiveFileTransfer(targetFileTransferTag);
                Logger.d(TAG,
                        "cancelFileTransfer() the target file transfer with tag "
                                + targetFileTransferTag
                                + " found in active list is " + fileTransfer);
                if (null != fileTransfer) {
                    Logger.d(TAG,
                            "cancelFileTransfer() the target file transfer with tag "
                                    + targetFileTransferTag
                                    + " found in active list");
                    fileTransfer.onCancel();
                    fileTransfer.onDestroy();
                    onSendFinish(fileTransfer, Result.REMOVABLE);
                }
            }
        }

        @Override
        public void onSendFinish(final SentFileTransfer sentFileTransfer, final Result result) {
            Logger.d(TAG, "onSendFinish(): sentFileTransfer = "
                    + sentFileTransfer + ", result = " + result);
            if (mActiveList.contains(sentFileTransfer)) {
                sentFileTransfer.cancelNotification();
                Logger.d(TAG, "onSendFinish() file transfer " + sentFileTransfer.mFileStruct
                        + " with " + sentFileTransfer.mParticipant + " finished with " + result
                        + " remove it from activated list");
                switch (result) {
                    case RESENDABLE:
                        mResendableList.add(sentFileTransfer);
                        mActiveList.remove(sentFileTransfer);
                        break;
                    case REMOVABLE:
                        mActiveList.remove(sentFileTransfer);
                        break;
                    default:
                        break;
                }
                checkNext();
            }
        }

        private SentFileTransfer findActiveFileTransfer(Object targetTag) {
            Logger.d(TAG, "findActiveFileTransfer entry, targetTag is " + targetTag);
            return findFileTransferByTag(mActiveList, targetTag);
        }

        private SentFileTransfer findPendingFileTransfer(Object targetTag) {
            Logger.d(TAG, "findPendingFileTransfer entry, targetTag is " + targetTag);
            return findFileTransferByTag(mPendingList, targetTag);
        }

        private SentFileTransfer findResendableFileTransfer(Object targetTag) {
            Logger.d(TAG, "findResendableFileTransfer entry, targetTag is " + targetTag);
            return findFileTransferByTag(mResendableList, targetTag);
        }

        private SentFileTransfer findFileTransferByTag(Collection<SentFileTransfer> whereToFind,
                Object targetTag) {
            if (null != whereToFind && null != targetTag) {
                for (SentFileTransfer sentFileTransfer : whereToFind) {
                    Object fileTransferTag = sentFileTransfer.mFileTransferTag;
                    if (targetTag.equals(fileTransferTag)) {
                        Logger.d(TAG, "findFileTransferByTag() the file transfer with targetTag "
                                + targetTag + " found");
                        return sentFileTransfer;
                    }
                }
                Logger.d(TAG, "findFileTransferByTag() not found targetTag " + targetTag);
                return null;
            } else {
                Logger.e(TAG, "findFileTransferByTag() whereToFind is " + whereToFind
                        + " targetTag is " + targetTag);
                return null;
            }
        }
    }

    @Override
    public void reloadMessages(String tag, List<Integer> messageIds) {
        Logger.d(TAG, "reloadMessages() messageIds: " + messageIds + " tag is " + tag);
        ContentResolver contentResolver = AndroidFactory.getApplicationContext()
                .getContentResolver();
        if (tag != null) {
            reloadGroupMessage(tag, messageIds, contentResolver);
        } else {
            reloadOne2OneMessages(messageIds, contentResolver);
        }

    }
    
    private void reloadGroupMessage(String tag, List<Integer> messageIds,
            ContentResolver contentResolver) {
        Logger.d(TAG, "reloadGroupMessage() entry");
        int length = PluginGroupChatWindow.GROUP_CONTACT_STRING_BEGINNER.length();
        String realTag = tag.substring(length);
        ParcelUuid parcelUuid = ParcelUuid.fromString(realTag);
        
        HashSet<Participant> participantList = new HashSet<Participant>();
        ArrayList<ReloadMessageInfo> messageList = new ArrayList<ReloadMessageInfo>();
        TreeSet<Long> sessionIdList = new TreeSet<Long>();
        for (Integer messageId : messageIds) {
            ReloadMessageInfo messageInfo =
                    loadMessageFromId(sessionIdList, messageId, contentResolver);
            if (null != messageInfo) {
                Object obj = messageInfo.getMessage();
                messageList.add(messageInfo);
                if (obj instanceof InstantMessage) {
                    InstantMessage message = (InstantMessage) obj;
                    String contact = message.getRemote();
                    contact = PhoneUtils.extractNumberFromUri(contact);
                    if (!ContactsManager.getInstance().isRcsValidNumber(contact)) {
                        Logger.d(TAG, "reloadGroupMessage() the contact is not valid user "
                                + contact);
                        continue;
                    }
                    Logger.d(TAG, "reloadGroupMessage() the contact is " + contact);
                    if (!TextUtils.isEmpty(contact)) {
                        Participant participant = new Participant(contact, contact);
                        participantList.add(participant);
                    }
                }
            }
        }
        Logger.d(TAG, "reloadGroupMessage() the sessionIdList is " + sessionIdList);
        fillParticipantList(sessionIdList, participantList, contentResolver);
        Logger.d(TAG, "reloadGroupMessage() participantList is " + participantList);
        if (participantList.size() < ChatFragment.GROUP_MIN_MEMBER_NUM) {
            Logger.d(TAG, "reloadGroupMessage() not group");
            return;
        }
        IChat chat = mChatMap.get(parcelUuid);
        if (chat != null) {
            Logger.d(TAG, "reloadGroupMessage() the chat already exist chat is " + chat);
        } else {
            chat = new GroupChat(this, null, new ArrayList<Participant>(participantList), parcelUuid);
            mChatMap.put(parcelUuid, chat);
            IGroupChatWindow chatWindow = ViewImpl.getInstance().addGroupChatWindow(parcelUuid,
                    ((GroupChat) chat).getParticipantInfos());
            ((GroupChat) chat).setChatWindow(chatWindow);
        }
        for (ReloadMessageInfo messageInfo : messageList) {
            if (null != messageInfo) {
                InstantMessage message = (InstantMessage) messageInfo.getMessage();
                int messageType = messageInfo.getMessageType();
                ((ChatImpl) chat).reloadMessage(message, messageType, -1);
            }
        }
    }

    private void fillParticipantList(TreeSet<Long> sessionIdList,
            HashSet<Participant> participantList, ContentResolver contentResolver) {
        Logger.d(TAG, "fillParticipantList() entry the  sessionIdList is " + sessionIdList
                + " participantList is " + participantList);
        for (Long sessionId : sessionIdList) {
            Cursor cursor = null;
            String[] selectionArg = {
                    Long.toString(sessionId),
                    Integer.toString(EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE)
            };
            try {
                cursor = contentResolver.query(RichMessagingData.CONTENT_URI, null,
                        RichMessagingData.KEY_CHAT_SESSION_ID + "=? AND "
                                + RichMessagingData.KEY_TYPE + "=?", selectionArg, null);
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String remote = cursor.getString(cursor
                                .getColumnIndex(RichMessagingData.KEY_CONTACT));
                        Logger.d(TAG, "fillParticipantList() the remote is " + remote);
                        if (remote.length() - INDEXT_ONE <= INDEXT_ONE) {
                            Logger.e(TAG, "fillParticipantList() the remote is no content");
                            continue;
                        }
                        if (remote.contains(COMMA)) {
                            Logger.d(TAG, "fillParticipantList() the remote has COMMA ");
                            String subString = remote.substring(INDEXT_ONE, remote.length()
                                    - INDEXT_ONE);
                            Logger.d(TAG, "fillParticipantList() the remote is " + subString);
                            String[] contacts = subString.split(COMMA);
                            for (String contact : contacts) {
                                Logger.d(TAG, "fillParticipantList() arraylist the contact is "
                                        + contact);
                                Participant participant = new Participant(contact, contact);
                                participantList.add(participant);
                            }
                        } else if (remote.contains(SEMICOLON)) {
                            Logger.d(TAG, "fillParticipantList() the remote has SEMICOLON ");
                            String[] contacts = remote.split(SEMICOLON);
                            for (String contact : contacts) {
                                Logger.d(TAG, "fillParticipantList() arraylist the contact is "
                                        + contact);
                                Participant participant = new Participant(contact, contact);
                                participantList.add(participant);
                            }
                        } else {
                            Logger.d(TAG, "fillParticipantList() remote is single ");
                            Participant participant = new Participant(remote, remote);
                            participantList.add(participant);
                        }
                    } while (cursor.moveToNext());
                } else {
                    Logger.e(TAG, "fillParticipantList() the cursor is null");
                }
            } finally {
                if (null != cursor) {
                    cursor.close();
                }
            }
        }
    }
    
    private void reloadOne2OneMessages(List<Integer> messageIds, ContentResolver contentResolver) {
        Logger.d(TAG, "reloadOne2OneMessages() entry");
        TreeSet<Long> sessionIdList = new TreeSet<Long>();
        for (Integer messageId : messageIds) {
            ReloadMessageInfo info = loadMessageFromId(sessionIdList, messageId, contentResolver);
            if (null != info) {
                Object obj = info.getMessage();
                int messageType = info.getMessageType();
                if (obj instanceof InstantMessage) {
                    InstantMessage message = (InstantMessage) obj;
                    int messageStatus = info.getMessageStatus();
                    String contact = message.getRemote();
                    contact = PhoneUtils.extractNumberFromUri(contact);
                    Logger.v(TAG, "reloadOne2OneMessages() : contact = " + contact);
                    if (!TextUtils.isEmpty(contact)) {
                        ArrayList<Participant> participantList = new ArrayList<Participant>();
                        participantList.add(new Participant(contact, contact));
                        IChat chat = addChat(participantList);
                        ((ChatImpl) chat).reloadMessage(message, messageType, messageStatus);
                    }
                } else if (obj instanceof FileStruct) {
                    FileStruct fileStruct = (FileStruct) obj;
                    String contact = fileStruct.mRemote;
                    Logger.v(TAG, "reloadOne2OneMessages() : contact = " + contact);
                    if (!TextUtils.isEmpty(contact)) {
                        ArrayList<Participant> participantList = new ArrayList<Participant>();
                        participantList.add(new Participant(contact, contact));
                        IChat chat = addChat(participantList);
                        ((ChatImpl) chat).reloadFileTransfer(fileStruct, messageType, info.getMessageStatus());
                    }
                }
            }
        }
    }

    private ReloadMessageInfo loadMessageFromId(TreeSet<Long> sessionIdList, Integer msgId,
            ContentResolver contentResolver) {
        Logger.d(TAG, "loadMessageFronId() msgId: " + msgId);
        Cursor cursor = null;
        String[] selectionArg = {msgId.toString()};
        try {
            cursor = contentResolver.query(RichMessagingData.CONTENT_URI, 
                    null, RichMessagingData.KEY_ID + "=?", selectionArg, null);
            if (cursor.moveToFirst()) {
                int messageType = cursor.getInt(cursor.getColumnIndex(RichMessagingData.KEY_TYPE));
                String messageId = cursor.getString(cursor
                        .getColumnIndex(RichMessagingData.KEY_MESSAGE_ID));
                String remote = cursor.getString(cursor
                        .getColumnIndex(RichMessagingData.KEY_CONTACT));
                String text = cursor.getString(cursor.getColumnIndex(RichMessagingData.KEY_DATA));
                long sessionId =
                        cursor
                                .getLong(cursor
                                        .getColumnIndex(RichMessagingData.KEY_CHAT_SESSION_ID));
                int messageStatus =
                        cursor.getInt(cursor.getColumnIndex(RichMessagingData.KEY_STATUS));
                sessionIdList.add(sessionId);
                Date date = new Date();
                long timeStamp = cursor.getLong(cursor
                        .getColumnIndex(RichMessagingData.KEY_TIMESTAMP));
                date.setTime(timeStamp);
                if (EventsLogApi.TYPE_INCOMING_CHAT_MESSAGE == messageType
                        || EventsLogApi.TYPE_OUTGOING_CHAT_MESSAGE == messageType
                        || EventsLogApi.TYPE_INCOMING_GROUP_CHAT_MESSAGE == messageType
                        || EventsLogApi.TYPE_OUTGOING_GROUP_CHAT_MESSAGE == messageType) {
                    InstantMessage message = new InstantMessage(messageId, remote, text, false);
                    message.setDate(date);
                    Logger.d(TAG, "loadMessageFronId() messageId: " + messageId + " , remote: "
                            + remote + " , text: " + text + " , timeStamp: " + timeStamp
                            + " , messageType: " + messageType + " , messageStatus: "
                            + messageStatus);
                    ReloadMessageInfo messageInfo =
                            new ReloadMessageInfo(message, messageType, messageStatus);
                    return messageInfo;
                } else if (EventsLogApi.TYPE_INCOMING_FILE_TRANSFER == messageType
                        || EventsLogApi.TYPE_OUTGOING_FILE_TRANSFER == messageType) {
                    String fileName = cursor.getString(cursor
                            .getColumnIndex(RichMessagingData.KEY_NAME));
                    long fileSize = cursor.getLong(cursor
                            .getColumnIndex(RichMessagingData.KEY_TOTAL_SIZE));
                    FileStruct fileStruct = new FileStruct(text, fileName, fileSize, messageId,
                            date, remote);
                    ReloadMessageInfo fileInfo = new ReloadMessageInfo(fileStruct, messageType, messageStatus);
                    return fileInfo;
                }
                return null;
            } else {
                Logger.w(TAG, "loadMessageFronId() empty cursor");
                return null;
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }
    
    @Override
    public void closeAllChat() {
        Logger.d(TAG, "closeAllChat()");
        Collection<IChat> chatSet = mChatMap.values();
        List<Object> tagList = new ArrayList<Object>();
        for (IChat iChat : chatSet) {
            tagList.add(((ChatImpl) iChat).getChatTag());
        }
        for (Object object : tagList) {
            removeChat(object);
        }
    }

    /**
     * Get group chat with chat id.
     * 
     * @param chatId The chat id.
     * @return The group chat.
     */
    public IChat getGroupChat(String chatId) {
        Logger.d(TAG, "getGroupChat() entry, chatId: " + chatId);
        Collection<IChat> chats = mChatMap.values();
        IChat result = null;
        for (IChat chat : chats) {
            if (chat instanceof GroupChat) {
                String id = ((GroupChat) chat).getChatId();
                if (id != null && id.equals(chatId)) {
                    result = chat;
                    break;
                }
            }
        }
        Logger.d(TAG, "getGroupChat() exit, result: " + result);
        return result;
    }
    
    /**
     * Used to reload message information
     */
    private static class ReloadMessageInfo {
        private Object mMessage;
        private int mMessageType;
        private int mMessageStatus;

        /**
         * Constructor
         * 
         * @param message The message
         * @param type The message type
         * @param status The message status
         */
        public ReloadMessageInfo(Object message, int type, int status) {
            this.mMessage = message;
            this.mMessageType = type;
            this.mMessageStatus = status;
        }

        /**
         * Constructor
         * 
         * @param message The message
         * @param type The message type
         */
        public ReloadMessageInfo(Object message, int type) {
            this.mMessage = message;
            this.mMessageType = type;
        }

        /**
         * Get the message
         * 
         * @return The message
         */
        public Object getMessage() {
            return mMessage;
        }

        /**
         * Get the message type
         * 
         * @return The message type
         */
        public int getMessageType() {
            return mMessageType;
        }

        /**
         * Get the message status
         * 
         * @return The message status
         */
        public int getMessageStatus() {
            return mMessageStatus;
        }
    }
}
