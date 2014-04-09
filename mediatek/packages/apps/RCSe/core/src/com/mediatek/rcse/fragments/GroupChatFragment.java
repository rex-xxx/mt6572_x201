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

package com.mediatek.rcse.fragments;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mediatek.rcse.activities.widgets.AsyncImageView;
import com.mediatek.rcse.activities.widgets.ContactsListManager;
import com.mediatek.rcse.api.Logger;
import com.mediatek.rcse.api.Participant;
import com.mediatek.rcse.interfaces.ChatController;
import com.mediatek.rcse.interfaces.ChatView;
import com.mediatek.rcse.interfaces.ChatView.IChatEventInformation;
import com.mediatek.rcse.interfaces.ChatView.IChatEventInformation.Information;
import com.mediatek.rcse.interfaces.ChatView.IChatWindowMessage;
import com.mediatek.rcse.interfaces.ChatView.IReceivedChatMessage;
import com.mediatek.rcse.interfaces.ChatView.ISentChatMessage;
import com.mediatek.rcse.interfaces.ChatView.ISentChatMessage.Status;
import com.mediatek.rcse.mvc.ControllerImpl;
import com.mediatek.rcse.mvc.ModelImpl.ChatEventStruct;
import com.mediatek.rcse.mvc.ParticipantInfo;
import com.mediatek.rcse.plugin.message.PluginGroupChatActivity;
import com.mediatek.rcse.service.Utils;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.core.ims.service.im.chat.event.User;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.client.messaging.InstantMessage;
import com.orangelabs.rcs.utils.PhoneUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

public class GroupChatFragment extends ChatFragment implements ChatView.IGroupChatWindow,
        View.OnClickListener {

    private static final String TAG = "GroupChatFragment";
    private int mGroupMemberHorMarginLeft = 6;
    private int mGroupMemberHorMarginRight = 6;
    private int mGroupMemberHorWidth = 48;
    private int mGroupMemberHorHeight = 48;
    private static final int ZERO_PARTICIPANT = 0;
    public static final String SHOW_REJOING_MESSAGE_REMINDER = "d";
    private List<String> mDateList = new ArrayList<String>();
    private List<IChatWindowMessage> mMessageList = new Vector<IChatWindowMessage>();
    private Activity mActivity = null;
    private String mPreviousDate = null;
    private static final String COLON = ": ";
    private final CopyOnWriteArrayList<Participant> mParticipantComposList =
            new CopyOnWriteArrayList<Participant>();
    protected TextView mRejoiningText = null;
    private List<ParticipantInfo> mGroupChatParticipantList = new ArrayList<ParticipantInfo>();
    // For test case to test whether max group chat participant mechanism work
    // when current participants is already the max number
    private boolean mIsMaxGroupChatParticipantsWork = false;
    private final Object mLock = new Object();
    private int mMessageSequenceOrder = -1 ;

    public void setTag(Object tag) {
        mTag = tag;
    }

    public void setParticipantList(final CopyOnWriteArrayList<ParticipantInfo> participantList) {
        Logger.d(TAG, "setParticipantList() entry the participants is " + participantList);
        mGroupChatParticipantList = participantList;
        List<Participant> participants = new ArrayList<Participant>();
        for (ParticipantInfo participantInfo : participantList) {
            participants.add(participantInfo.getParticipant());
        }
        mParticipantList = participants;
    }

    /**
     * Get the participants list in the group chat fragment
     * @return participants list in the group chat fragment
     */
    public List<Participant> getParticipants() {
        return mParticipantList;
    }

    @Override
    public void onAttach(Activity activity) {
        mActivity = activity;
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        mTag = arguments.getParcelable(Utils.CHAT_TAG);
        mGroupChatParticipantList = arguments.getParcelableArrayList(Utils.CHAT_PARTICIPANTS);
        List<Participant> participants = new ArrayList<Participant>();
        for (ParticipantInfo participantInfo : mGroupChatParticipantList) {
            participants.add(participantInfo.getParticipant());
        }
        mParticipantList = participants;
        loadDimension(getResources());
        Configuration configuration = getResources().getConfiguration();
        mParticipantListDisplayer.onFragmentCreate(configuration.orientation);
        mRejoiningText = (TextView) mContentView.findViewById(R.id.text_rejoining_prompt);
    }

    @Override
    public void onDestroy() {
        Logger.d(TAG, "onDestroy");
        mPreMessageMap.clear();
        mPreviousDate = null;
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        mActivity = null;
        super.onDetach();
    }

    @Override
    protected void onSend(String message) {
        Logger.d(TAG, "onSend() The message is " + message);
        int messageTag = onSentMessage(message);
        Message controllerMessage = ControllerImpl.getInstance().obtainMessage(
                ChatController.EVENT_SEND_MESSAGE, mTag, message);
        controllerMessage.arg1 = messageTag;
        controllerMessage.sendToTarget();
    }

    private int onSentMessage(String content) {
        int messageTag = Utils.RANDOM.nextInt();
        InstantMessage message = new InstantMessage("", Utils.DEFAULT_REMOTE, content, true);
        message.setDate(new Date());
        ISentChatMessage sentChatMessage = addSentMessage(message, messageTag);
        if (sentChatMessage != null) {
            mPreMessageMap.put(messageTag, (SentMessage) sentChatMessage);
        }
        return messageTag;
    }

    protected boolean handleShowReminder(String reminder) {
        Logger.d(TAG, "handleShowReminder() reminder is " + reminder);
        if (SHOW_REJOING_MESSAGE_REMINDER.equals(reminder)) {
            if (mRejoiningText != null) {
                mRejoiningText.setVisibility(View.VISIBLE);
                super.handleClearReminder();
            } else {
                Logger.e(TAG, "handleShowReminder() the mRejoiningText is null");
            }
            return true;
        } else {
            if (mRejoiningText != null) {
                mRejoiningText.setVisibility(View.GONE);
            } else {
                Logger.e(TAG, "handleClearReminder() mTypingText is null");
            }
            return super.handleShowReminder(reminder);
        }
    }

    protected void handleClearReminder() {
        Logger.d(TAG, "handleClearReminder() entry");
        if (mRejoiningText != null) {
            mRejoiningText.setVisibility(View.GONE);
        } else {
            Logger.e(TAG, "showReminderList() mRejoiningText is null");
        }
        super.handleClearReminder();
    }

    @Override
    public IReceivedChatMessage addReceivedMessage(InstantMessage message, boolean isRead) {
        Logger.d(TAG, "addReceivedMessage() mIsBottom: " + mIsBottom + " message: " + message
                + " isRead: " + isRead);
        if (message != null) {
            final IReceivedChatMessage msg = new ReceivedMessage(message);
            Date date = message.getDate();
            if (mMessageAdapter == null) {
                Logger.d(TAG, "addReceivedMessage mMessageAdapter is null");
                return null;
            }
            addMessageDate(date);
            if (!mIsBottom) {
                String remote = ((ReceivedMessage) msg).getMessageSender();
                Logger.d(TAG, "addReceivedMessage() mIsBottom: " + mIsBottom + " remote: " + remote
                        + " mParticipantList: " + mParticipantList);
                if (remote != null) {
                    remote = PhoneUtils.extractNumberFromUri(remote);
                    String name = null;
                    if (mParticipantList != null) {
                        for (Participant participant : mParticipantList) {
                            Logger.d(TAG, "participant  = " + participant.toString());
                            if (participant.getContact().equals(remote)) {
                                name = participant.getDisplayName();
                                break;
                            }
                        }
                        Logger.d(TAG, "addReceivedMessage  the remote name is " + name);
                        Thread currentThread = Thread.currentThread();
                        if (THREAD_ID_MAIN == currentThread.getId()) {
                            mMessageReminderText.setText(SPACE);
                            if (name != null) {
                                mMessageReminderText.append(name);
                            } else {
                                if (getActivity() != null) {
                                    mMessageReminderText.append(getResources().getString(
                                            R.string.group_chat_stranger));
                                }
                                Logger.w(TAG, "name is null, so append stranger getActivity: "
                                        + getActivity());
                            }
                            mMessageReminderText.append(COLON);
                            String rcvMsg = ((ReceivedMessage) msg).getMessageText();
                            if (rcvMsg != null) {
                                mMessageReminderText.append(rcvMsg);
                            }
                            mTextReminderSortedSet.add(SHOW_NEW_MESSAGE_REMINDER);
                            showReminderList();
                        } else {
                            final String contactName = name;
                            mUiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mMessageReminderText.setText(SPACE);
                                    if (contactName != null) {
                                        mMessageReminderText.append(contactName);
                                    }
                                    mMessageReminderText.append(COLON);
                                    String rcvMsg = ((ReceivedMessage) msg).getMessageText();
                                    if (rcvMsg != null) {
                                        mMessageReminderText.append(rcvMsg);
                                    }
                                    mTextReminderSortedSet.add(SHOW_NEW_MESSAGE_REMINDER);
                                    showReminderList();
                                }
                            });
                        }
                        mIsNewMessageNotify = Boolean.TRUE;
                    }
                }
            }
            addMessageAtomic(msg);
            return msg;
        } else {
            Logger.d(TAG, "The received chat message is null");
            return null;
        }
    }

    @Override
    public ISentChatMessage addSentMessage(InstantMessage message, int messageTag) {
        Logger.d(TAG, "addSentMessage(), message: " + message);
        if (message != null) {
            SentMessage msg = (SentMessage) mPreMessageMap.get(messageTag);
            if (null == msg) {
                msg = new SentMessage(message);
                Date date = message.getDate();
                if (mMessageAdapter == null) {
                    Logger.d(TAG, "addSentMessage mMessageAdapter is null");
                    return null;
                }
                addMessageDate(date);
                addMessageAtomic(msg);
                return msg;
            } else {
                String messageRemote = message.getRemote();
                Logger.d(TAG, "addSentMessage(), messageRemote: " + messageRemote);
                if (!Utils.DEFAULT_REMOTE.equals(messageRemote)) {
                    mPreMessageMap.remove(messageTag);
                    msg.updateMessage(message);
                    return msg;
                }
            }
        }
        return null;
    }

    /**
     * Add message date in the ListView.Messages sent and received in the same
     * day have only one date.
     * 
     * @param date The date of the messages. Each date stands for a section in
     *            fastScroll.
     */
    public void addMessageDate(Date date) {
        Logger.d(TAG, "updateDateList() entry, the size of dateList is " + mDateList.size()
                + " and the date is " + date);
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING);
        String currentDate = dateFormat.format(date);
        Logger.d(TAG, "currentDate is " + currentDate);
        if (mPreviousDate == null || !mPreviousDate.equals(currentDate)) {
            if (mMessageAdapter == null) {
                Logger.d(TAG, "addMessageDate mMessageAdapter is null");
                return;
            }
            synchronized (mLock) {
                mMessageSequenceOrder = mMessageSequenceOrder + 1;
                int position = mMessageSequenceOrder;
                mMessageAdapter.addMessage(date, position);                
                mDateList.add(currentDate);
            }
        }
        mPreviousDate = currentDate;
    }

    @Override
    public void removeAllMessages() {
        Logger.d(TAG, "removeAllMessages() entry, mMessageAdapter: " + mMessageAdapter);
        if (mMessageAdapter != null) {
            mMessageAdapter.removeAllItems();
        }
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                mMessageList.clear();
                mDateList.clear();
            }
        });
        mPreviousDate = null;
        Logger.d(TAG, "removeAllMessages exit");
    }

    @Override
    public IChatWindowMessage getSentChatMessage(String messageId) {
        for (IChatWindowMessage message : mMessageList) {
            if (message.getId().equals(messageId)) {
                return message;
            }
        }
        return null;
    }

    @Override
    public void updateParticipants(final List<ParticipantInfo> participantList) {
        Logger.d(TAG, "updateParticipants entry");
        if (mActivity == null) {
            Logger.d(TAG, "updateParticipants mActivity is null");
            return;
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Participant> participants = new ArrayList<Participant>();
                mGroupChatParticipantList.clear();
                mGroupChatParticipantList.addAll(participantList);
                Logger.d(TAG, "updateParticipants() mGroupChatParticipantList: "
                        + mGroupChatParticipantList);
                for (ParticipantInfo info : participantList) {
                    participants.add(info.getParticipant());
                }
                Logger.d(TAG, "updateParticipants() participants: " + participants);
                mParticipantList = participants;
                updateChatUi();
                if (Logger.getIsIntegrationMode()) {
                    ((PluginGroupChatActivity) mActivity).updateParticipants(mParticipantList);
                } else {
                    Logger.d(TAG, "updateParticipants() it is not integration mode.");
                }
            }
        });
        Logger.d(TAG, "updateParticipants exit");
    }

    /**
     * Notify controller participants has been added.
     * 
     * @param participantList The participants to be added.
     */
    public void addParticipants(List<Participant> participantList) {
        Logger.d(TAG, "addParticipants entry");
        ControllerImpl controller = ControllerImpl.getInstance();
        Message controllerMessage = controller.obtainMessage(
                ChatController.EVENT_GROUP_ADD_PARTICIPANT, mTag, participantList);
        controllerMessage.sendToTarget();
    }

    @Override
    public void setIsComposing(boolean isComposing, final Participant participant) {
        Logger.v(TAG, "setIsComposing status is " + isComposing + "participant is" + participant);
        if (mTypingText != null) {
            if (isComposing) {
                if (participant != null) {
                    mParticipantComposList.add(participant);
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            showComposingInformation();
                        }
                    });
                } else {
                    Logger.e(TAG, "setIsComposing the participant is null");
                }
            } else {
                if (participant != null) {
                    int listSize = mParticipantComposList.size();
                    if (listSize > 0) {
                        mParticipantComposList.remove(participant);
                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                showComposingInformation();
                            }
                        });
                    } else {
                        Logger.e(TAG, "setIsComposing false the listSize is " + listSize);
                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mTextReminderSortedSet.remove(SHOW_IS_TYPING_REMINDER);
                                showReminderList();
                            }
                        });
                    }
                } else {
                    Logger.e(TAG, "setIsComposing false participant is null");
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mTextReminderSortedSet.remove(SHOW_IS_TYPING_REMINDER);
                            showReminderList();
                        }
                    });
                }
            }
        } else {
            Logger.e(TAG, "setIsComposing the typingtext is null");
        }
    }

    private void showComposingInformation() {
        String moreTyping = getResources().getString(R.string.label_contact_imore_composing);
        ArrayList<Participant> tmpList = new ArrayList<Participant>(mParticipantComposList);
        int listSize = tmpList.size();
        Logger.w(TAG, "setIsComposing + listSize" + listSize);
        if (listSize == 0) {
            mTextReminderSortedSet.remove(SHOW_IS_TYPING_REMINDER);
            showReminderList();
        } else if (listSize == 1) {
            Participant participant = tmpList.get(ZERO_PARTICIPANT);
            final String isTyping =
                    getResources().getString(
                            R.string.label_contact_is_composing,
                            ContactsListManager.getInstance().getDisplayNameByPhoneNumber(participant
                                    .getContact()));
            mTypingText.setText(isTyping);
            mTextReminderSortedSet.add(SHOW_IS_TYPING_REMINDER);
            showReminderList();
        } else if (listSize > 1) {
            mTypingText.setText(moreTyping);
            mTextReminderSortedSet.add(SHOW_IS_TYPING_REMINDER);
            showReminderList();
        }
    }

    private void updateChatUi() {
        Logger.d(TAG, "updateChatUi entry");
        Activity activity = getActivity();
        if (activity != null) {
            LayoutInflater inflater = LayoutInflater.from(activity.getApplicationContext());
            View customView = inflater.inflate(R.layout.group_chat_screen_title, null);
            activity.getActionBar().setCustomView(customView);
            setChatScreenTitle();
            ImageButton expandGroupChat =
                    (ImageButton) activity.findViewById(R.id.group_chat_expand);
            expandGroupChat.setOnClickListener(this);
            ImageButton collapaseGroupChat =
                    (ImageButton) activity.findViewById(R.id.group_chat_collapse);
            collapaseGroupChat.setOnClickListener(this);
            RelativeLayout groupChatTitleLayout =
                    (RelativeLayout) activity.findViewById(R.id.group_chat_title_layout);
            groupChatTitleLayout.setOnClickListener(this);
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.show(this);
            fragmentTransaction.commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
            addGroupChatMembersIcon();
            activity.invalidateOptionsMenu();
            Logger.d(TAG, "updateGroupChatUi exit");
        } else {
            Logger.w(TAG, "activity is null.");
        }
    }

    /**
     * When switch ChatFragment this method should be called to remove ui.
     */
    public void removeChatUi() {
        Activity activity = getActivity();
        if (activity != null) {
            HorizontalScrollView groupChatBannerScroller =
                    (HorizontalScrollView) activity.findViewById(R.id.group_chat_banner_scroller);
            if (groupChatBannerScroller != null) {
                Logger.v(TAG, "groupChatBannerScroller is not null");
                groupChatBannerScroller.setVisibility(View.GONE);
            } else {
                Logger.v(TAG, "groupChatBannerScroller is null.");
            }
            activity.getActionBar().setCustomView(null);
        } else {
            Logger.w(TAG, "activity is null.");
        }
    }

    public void addGroupChatMembersIcon() {
        List<ParticipantInfo> participantInfos =
                new ArrayList<ParticipantInfo>(mGroupChatParticipantList);
        mParticipantListDisplayer.updateBanner(participantInfos);
    }

    /**
     * Set chat screen's title.
     */
    public void setChatScreenTitle() {
        Logger.v(TAG, "setChatScreenTitle()");
        Activity activity = getActivity();
        if (activity != null) {
            int num = getParticipantsNum();
            if (num > ChatFragment.ONE) {
                setGroupChatTitleNumbers(num + ChatFragment.ONE);
            }
            TextView titleView = (TextView) activity.findViewById(R.id.peer_name);
            Logger.w(TAG, "setChatScreenTitle() num: " + num + " titleView: " + titleView);
            if (titleView != null) {
                if (null != mParticipantList && mParticipantList.size() > 0) {
                    titleView.setText(getParticipantsName(mParticipantList
                            .toArray(new Participant[1])));
                }
            }
        }
    }

    /**
     * Set group chat members's number.
     * 
     * @param num The participants number in your group chat.
     */
    private void setGroupChatTitleNumbers(int num) {
        Logger.v(TAG, "setGroupChatTitleNumbers(),num = " + num);
        Activity activity = getActivity();
        if (activity != null) {
            TextView numView = (TextView) activity.findViewById(R.id.peer_number);
            Logger.w(TAG, "setGroupChatTitleNumbers() numView: " + numView);
            if (numView != null) {
                String numStr = ChatFragment.OPEN_PAREN + num + ChatFragment.CLOSE_PAREN;
                numView.setText(numStr);
            }
        }
    }

    public void expandGroupChat() {
        Logger.v(TAG, "expandGroupChat() entry");
        mParticipantListDisplayer.expand();
    }

    public void collapseGroupChat() {
        Logger.v(TAG, "collapseGroupChat() entry");
        mParticipantListDisplayer.collapse();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.v(TAG, "onActivityResult() requestCode = " + requestCode + ",resultCode = "
                + resultCode + ",data = " + data);
        if (requestCode == RESULT_CODE_ADD_CONTACTS) {
            if (data != null) {
                ArrayList<Participant> participantList = ContactsListManager.getInstance()
                        .parseParticipantsFromIntent(data);
                Logger.d(TAG, "onActivityResult() participantList is " + participantList);
                if (participantList != null && participantList.size() != 0) {
                    participantList.addAll(0, mParticipantList);
                    addContactsToGroupChat(participantList);
                } else {
                    Logger.w(TAG,
                            "onActivityResult() participantList size is 0,so do not add member to group chat");
                }
            }
        }
    }

    private void addContactsToGroupChat(ArrayList<Participant> participantList) {
        Logger.v(TAG, "addContactsToGroupChat");
        ControllerImpl controller = ControllerImpl.getInstance();
        Message controllerMessage = controller.obtainMessage(
                ChatController.EVENT_GROUP_ADD_PARTICIPANT, mTag, participantList);
        controllerMessage.sendToTarget();
    }

    protected void addContactsToExistChatWindow(List<Participant> participantList) {
        int size = participantList == null ? 0 : participantList.size();
        Logger.w(TAG, "size = " + size);
        if (size == 0) {
            Logger.w(TAG, "participantList is null");
            return;
        }
        addParticipants(participantList);
    }

    /**
     * This is an Information for chat event
     */
    public static class ChatEventInformation implements IChatEventInformation {
        protected ChatEventStruct mChatEventStruct = null;

        public ChatEventInformation(ChatEventStruct chatEventStruct) {
            mChatEventStruct = chatEventStruct;
        }

        public Information getInformation() {
            if (mChatEventStruct != null) {
                return mChatEventStruct.information;
            } else {
                Logger.e(TAG, "getInformation the mChatEventStruct is null");
                return null;
            }
        }

        public Object getRelatedInfo() {
            if (mChatEventStruct != null) {
                return mChatEventStruct.relatedInformation;
            } else {
                Logger.e(TAG, "getInformation the mChatEventStruct is null");
                return null;
            }
        }

        public Date getDate() {
            if (mChatEventStruct != null) {
                return mChatEventStruct.date;
            } else {
                Logger.e(TAG, "getInformation the mChatEventStruct is null");
                return null;
            }
        }
    }

    public class SentMessage implements ISentChatMessage {
        private InstantMessage mMessage = null;

        private Status mStatus = Status.SENDING;

        public SentMessage(InstantMessage msg) {
            mMessage = msg;
        }

        @Override
        public String getId() {
            if (mMessage == null) {
                Logger.w(TAG, "mMessage is null, as a result no id returned");
                return null;
            }
            return mMessage.getMessageId();
        }

        @Override
        public void updateStatus(Status s) {
            final Status status = s;
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mStatus = status;
                    if (mMessageAdapter != null) {
                        mMessageAdapter.notifyDataSetChanged();
                    }
                }
            });
        }

        /**
         * Get message text.
         * 
         * @return Message text.
         */
        public String getMessageText() {
            if (mMessage == null) {
                return null;
            }
            return mMessage.getTextMessage();
        }

        /**
         * Get status of message.
         * 
         * @return status of message.
         */
        public Status getStatus() {
            return mStatus;
        }

        /**
         * Return the time when then message was sent.
         * 
         * @return Message sent time.
         */
        public Date getMessageDate() {
            if (mMessage != null) {
                return mMessage.getDate();
            } else {
                Logger.d(TAG, "getMessageText mMessage is null");
                return null;
            }
        }

        protected void updateMessage(InstantMessage message) {
            Logger.d(TAG, "updateMessage() message: " + message);
            mMessage = message;
        }

        @Override
        public void updateDate(Date date) {
            //Do nothing
        }
    }

    /**
     * ReceivedMessage provided for window to get message.
     */
    public static class ReceivedMessage implements IReceivedChatMessage {
        private Status mStatus = Status.SENDING;

        private InstantMessage mMessage = null;

        public ReceivedMessage(InstantMessage msg) {
            mMessage = msg;
        }

        @Override
        public String getId() {
            if (mMessage == null) {
                Logger.w(TAG, "mMessage is null, as a result no id returned");
                return null;
            }
            return mMessage.getMessageId();
        }

        /**
         * Return the message text.
         * 
         * @return Message text.
         */
        public String getMessageText() {
            if (mMessage == null) {
                Logger.w(TAG, "mMessage is null, as a result no text returned");
                return null;
            }
            return mMessage.getTextMessage();
        }

        /**
         * Return the time when then message was sent.
         * 
         * @return Message sent time.
         */
        public Date getMessageDate() {
            if (mMessage != null) {
                return mMessage.getDate();
            } else {
                Logger.d(TAG, "getMessageText mMessage is null");
                return null;
            }
        }

        /**
         * Return who send this message.
         * 
         * @return The sender.
         */
        public String getMessageSender() {
            if (mMessage == null) {
                Logger.w(TAG, "mMessage is null, as a result no remote returned");
                return null;
            }
            return mMessage.getRemote();
        }

        /**
         * Get status of message.
         * 
         * @return status of message.
         */
        public Status getStatus() {
            return mStatus;
        }
    }

    @Override
    public void addLoadHistoryHeader(boolean showHeader) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void onAdapterPrepared() {
        // TODO Auto-generated method stub

    }

    @Override
    protected int getFragmentResource() {
        return R.layout.chat_fragment_group;
    }

    @Override
    public IChatEventInformation addChatEventInformation(ChatEventStruct chatEventStruct) {
        if (chatEventStruct != null) {
            Logger.d(TAG, "addChatEventInformation chatEventStruct is " + chatEventStruct);
            Date date = chatEventStruct.date;
            addMessageDate(date);
            IChatEventInformation chatEvent = new ChatEventInformation(chatEventStruct);
            Information information = ((ChatEventInformation) chatEvent).getInformation();
            switch (information) {
                case LEFT:
                case JOIN:
                    addChatEventInfo(chatEvent);
                    break;
                default:
                    Logger.e(TAG,
                            "addChatEventInformation the information is not defined and it is "
                                    + information);
                    break;
            }
            return null;
        } else {
            Logger.d(TAG, "The sent chat message is null");
            return null;
        }
    }

    private IChatEventInformation addChatEventInfo(IChatEventInformation chatEvent) {
        Logger.d(TAG, "addChatEventInfo entry");
        if (mMessageAdapter != null) {
            mMessageAdapter.addMessage(chatEvent);
            return chatEvent;
        } else {
            Logger.d(TAG, "addChatEventInformation mMessageAdapter is null");
            return null;
        }
    }

    private void enableSendButton(final boolean enable) {
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                Activity activity = getActivity();
                if (activity != null && mBtnSend != null) {
                    mBtnSend.setClickable(enable);
                } else {
                    Logger.e(TAG, "enableSendButton() activity: " + activity + " mBtnSend: "
                            + mBtnSend);
                }
            }
        });
    }

    @Override
    public void setIsRejoining(boolean isRejoining) {
        Logger.d(TAG, "setIsRejoining() isRejoining: " + isRejoining);
        enableSendButton(!isRejoining);
    }

    @Override
    public void updateAllMsgAsRead() {
        // Do Nothing
    }

    private void loadDimension(Resources resources) {
        mGroupMemberHorMarginLeft =
                resources.getDimensionPixelSize(R.dimen.group_member_hor_margin_left);
        mGroupMemberHorMarginRight =
                resources.getDimensionPixelSize(R.dimen.group_member_hor_margin_right);
        mGroupMemberHorHeight = resources.getDimensionPixelSize(R.dimen.group_member_hor_width);
        mGroupMemberHorWidth = resources.getDimensionPixelSize(R.dimen.group_member_hor_height);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mParticipantListDisplayer.onScreenSwitched(newConfig.orientation);
    }

    private ParticipantListDisplayer mParticipantListDisplayer = new ParticipantListDisplayer();

    /**
     * Defines a common interface for ParticipantListDisplayer
     */
    private interface IDisplayStrategy {
        void expand();

        void collapse();

        void show();

        void dismiss();

        void updateBanner(List<ParticipantInfo> participantInfos);
    }

    /**
     * This is a helper class, to manage the participant list banner
     */
    private class ParticipantListDisplayer {
        private static final String TAG = "ParticipantListDisplayer";

        private IDisplayStrategy mCurrentStrategy = null;

        private final LandscapeStrategy mLandscapeStrategy = new LandscapeStrategy();
        private final PortraitStrategy mPortraitStrategy = new PortraitStrategy();

        private List<ParticipantInfo> mParticipantInfos = null;

        public void onFragmentCreate(int orientation) {
            Logger.d(TAG, "onActivityCreate entry, orientation: " + orientation);
            onStatusUpdated(orientation, false);
        }

        public void expand() {
            if (null != mCurrentStrategy) {
                mCurrentStrategy.expand();
            } else {
                Logger.w(TAG, "expand() mCurrentStrategy is null");
            }
        }

        public void collapse() {
            if (null != mCurrentStrategy) {
                mCurrentStrategy.collapse();
            } else {
                Logger.w(TAG, "collapse() mCurrentStrategy is null");
            }
        }

        /**
         * This method should only be called from the main thread to update the
         * banner
         * 
         * @param participantInfos The latest participant list
         */
        public void updateBanner(List<ParticipantInfo> participantInfos) {
            mParticipantInfos = participantInfos;
            if (null != mCurrentStrategy) {
                mCurrentStrategy.updateBanner(participantInfos);
                if (mCurrentStrategy.equals(mPortraitStrategy)) {
                    mPortraitStrategy.show();
                    mLandscapeStrategy.dismiss();
                } else {
                    mPortraitStrategy.dismiss();
                    mLandscapeStrategy.show();
                }
            } else {
                Logger.w(TAG, "updateBanner() mCurrentStrategy is null");
            }
        }

        public void onScreenSwitched(int orientation) {
            onStatusUpdated(orientation, true);
        }

        public void onStatusUpdated(int orientation, boolean isNeedUpdate) {
            Logger.d(TAG, "onScreenSwitched entry, orientation: " + orientation);
            switch (orientation) {
                case Configuration.ORIENTATION_LANDSCAPE:
                    mPortraitStrategy.dismiss();
                    mLandscapeStrategy.show();
                    mCurrentStrategy = mLandscapeStrategy;
                    break;
                case Configuration.ORIENTATION_PORTRAIT:
                    mPortraitStrategy.show();
                    mLandscapeStrategy.dismiss();
                    mCurrentStrategy = mPortraitStrategy;
                    break;
                default:
                    Logger.w(TAG, "onScreenSwitched() unknown orientation: " + orientation);
                    break;
            }
            if (isNeedUpdate) {
                mCurrentStrategy.updateBanner(mParticipantInfos);
            }
        }
    }

    /**
     * The strategy used in Landscape screen
     */
    private class LandscapeStrategy extends BaseAdapter implements IDisplayStrategy {
        private static final String TAG = "LandscapeStrategy";

        private ListView mBanner = null;
        private View mArea = null;
        private LayoutInflater mInflator = null;
        private final List<ParticipantInfo> mParticipantInfoList = new ArrayList<ParticipantInfo>();

        private void checkArea() {
            if (null != mBanner) {
                Logger.d(TAG, "checkArea() already initialized");
            } else {
                Logger.d(TAG, "checkArea() not initialized");
                mArea = mContentView.findViewById(R.id.participant_list_area);
                mBanner = (ListView) mContentView.findViewById(R.id.participant_banner);
                mBanner.setAdapter(this);
                mInflator = LayoutInflater.from(getActivity().getApplicationContext());
            }
        }

        @Override
        public void collapse() {
            Logger.v(TAG, "collapse() entry");
            // Do nothing
        }

        @Override
        public void dismiss() {
            Logger.v(TAG, "dismiss() entry");
            checkArea();
            mArea.setVisibility(View.GONE);
        }

        @Override
        public void expand() {
            Logger.v(TAG, "expand() entry");
            // Do nothing
        }

        @Override
        public void show() {
            Logger.v(TAG, "show() entry");
            checkArea();
            mArea.setVisibility(View.VISIBLE);
        }

        @Override
        public void updateBanner(List<ParticipantInfo> participantInfos) {
            Logger.d(TAG, "updateBanner() entry, participantInfos: " + participantInfos);
            mParticipantInfoList.clear();
            mParticipantInfoList.addAll(participantInfos);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mParticipantInfoList.size();
        }

        @Override
        public Object getItem(int position) {
            return mParticipantInfoList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, final View convertView, ViewGroup parent) {
            Logger.v(TAG, "getView() entry, position: " + position);
            View itemView = convertView;
            if (null == itemView) {
                Logger.d(TAG, "getView() inflate a new item view");
                itemView = mInflator.inflate(R.layout.participant_list_item_vertical, null);
            } else {
                Logger.d(TAG, "getView() use convertView");
            }
            bindView(itemView, mParticipantInfoList.get(position));
            Logger.v(TAG, "getView() exit");
            return itemView;
        }

        private void bindView(View itemView, ParticipantInfo info) {
            Logger.d(TAG, "bindView() info:" + info);
            String contact = info.getContact();
            AsyncImageView avatar = (AsyncImageView) itemView.findViewById(R.id.peer_avatar);
            boolean isGrey = !(User.STATE_CONNECTED.equals(info.getState()));
            avatar.setAsyncContact(contact, isGrey);
            TextView remoteName = (TextView) itemView.findViewById(R.id.remote_name);
            remoteName.setText(ContactsListManager.getInstance().getDisplayNameByPhoneNumber(contact));
        }
    }

    /**
     * The strategy used in Portrait screen
     */
    private class PortraitStrategy implements IDisplayStrategy {

        private static final String TAG = "PortraitStrategy";
        private boolean mIsExpand = false;

        @Override
        public void collapse() {
            Logger.v(TAG, "collapse() entry");
            Activity activity = getActivity();
            if (null != activity) {
                setGroupChatBannerScrollerVisibility(activity, View.GONE);
                setGroupChatCollapseVisibility(activity, View.GONE);
                setGroupChatExpandVisibility(activity, View.VISIBLE);
            } else {
                Logger.w(TAG, "collapse() activity is null");
            }
            mIsExpand = false;

        }

        @Override
        public void dismiss() {
            Logger.v(TAG, "dismiss() entry");
            Activity activity = getActivity();
            if (null != activity) {
                setGroupChatBannerScrollerVisibility(activity, View.GONE);
                setGroupChatCollapseVisibility(activity, View.GONE);
                setGroupChatExpandVisibility(activity, View.GONE);
            } else {
                Logger.w(TAG, "dismiss() activity is null");
            }

        }

        @Override
        public void expand() {
            Logger.v(TAG, "expand() entry");
            Activity activity = getActivity();
            if (null != activity) {
                setGroupChatBannerScrollerVisibility(activity, View.VISIBLE);
                setGroupChatCollapseVisibility(activity, View.VISIBLE);
                setGroupChatExpandVisibility(activity, View.GONE);
            } else {
                Logger.w(TAG, "expand() activity is null");
            }
            mIsExpand = true;

        }

        @Override
        public void show() {
            if (mIsExpand) {
                Logger.d(TAG, "show() mIsExpand is true");
                expand();
            } else {
                Logger.d(TAG, "show() mIsExpand is false");
                collapse();
            }
        }

        @Override
        public void updateBanner(List<ParticipantInfo> participantInfos) {
            Logger.d(TAG, "updateBanner() entry, participantInfos: " + participantInfos);
            final Activity activity = getActivity();
            if (activity != null) {
                LinearLayout grouChatMemberIconsLayout =
                        (LinearLayout) activity.findViewById(R.id.group_chat_banner_container);
                if (grouChatMemberIconsLayout == null) {
                    Logger.w(TAG, "updateBanner() grouChatMemberIconsLayout is null");
                    return;
                }
                final int childCount = grouChatMemberIconsLayout.getChildCount();
                LayoutParams layoutParams =
                        new LayoutParams(mGroupMemberHorWidth, mGroupMemberHorHeight);
                layoutParams
                        .setMargins(mGroupMemberHorMarginLeft, 0, mGroupMemberHorMarginRight, 0);
                int num = 0;

                if (participantInfos != null) {
                    num = participantInfos.size();
                    Logger.d(TAG, "updateBanner() num: " + num + " , childCount: " + childCount);
                    int i = 0;
                    for (; i < num; ++i) {
                        Logger.d(TAG, "updateBanner() current i: " + i);
                        ParticipantInfo participant = participantInfos.get(i);
                        if (i < childCount) {
                            Logger.d(TAG, "updateBanner() use an existing view");
                            getView(activity, participant,
                                    (AsyncImageView) grouChatMemberIconsLayout.getChildAt(i));
                        } else {
                            Logger.d(TAG, "updateBanner() need to inflate a new view");
                            View itemView = getView(activity, participant, null);
                            if (null != itemView) {
                                grouChatMemberIconsLayout.addView(itemView, layoutParams);
                            } else {
                                Logger.w(TAG, "updateBanner() inflate failed");
                            }
                        }
                    }
                    Logger.d(TAG, "updateBanner() add view done, i: " + i);
                    int invalidItemCount = childCount - i;
                    if (invalidItemCount > 0) {
                        Logger.d(TAG, "updateBanner() need to remove child view from " + i
                                + " count " + invalidItemCount);
                        grouChatMemberIconsLayout.removeViews(i, invalidItemCount);
                    } else {
                        Logger.d(TAG, "updateBanner() no need to remove child view");
                    }
                } else {
                    Logger.e(TAG, "updateBanner() the participantInfos is null");
                }
            } else {
                Logger.w(TAG, "updateBanner() activity is null.");
            }
        }

        private View getView(final Context context, final ParticipantInfo info,
                final AsyncImageView convertView) {
            Logger.d(TAG, "getView() info: " + info + " , convertView: " + convertView);
            if (null == info) {
                Logger.w(TAG, "getView() info is null");
                return null;
            }
            AsyncImageView itemView = convertView;
            if (null == itemView) {
                itemView = inflateView(context);
            }
            String state = info.getState();
            String contact = info.getContact();
            Logger.d(TAG, "getView(), contact: " + contact + ", state: " + state);
            boolean isGrey = !(User.STATE_CONNECTED.equals(info.getState()));
            itemView.setAsyncContact(contact, isGrey);
            return itemView;
        }

        private AsyncImageView inflateView(Context context) {
            return new AsyncImageView(context);
        }

        private void setGroupChatBannerScrollerVisibility(Activity activity, int visible) {
            HorizontalScrollView groupChatBannerScrollerLayout =
                    (HorizontalScrollView) activity.findViewById(R.id.group_chat_banner_scroller);
            if (groupChatBannerScrollerLayout != null) {
                Logger
                        .v(TAG,
                                "setGroupChatBannerScrollerVisibility() groupChatBannerScroller is not null");
                groupChatBannerScrollerLayout.setVisibility(visible);
            } else {
                Logger.v(TAG,
                        "setGroupChatBannerScrollerVisibility() groupChatBannerScroller is null.visible = "
                                + visible);
            }
        }

        private void setGroupChatExpandVisibility(Activity activity, int visible) {
            ImageButton groupChatExpandView =
                    (ImageButton) activity.findViewById(R.id.group_chat_expand);
            if (groupChatExpandView != null) {
                Logger.v(TAG,
                        "setGroupChatExpandVisibility() groupChatExpandView is not null.visible = "
                                + visible);
                groupChatExpandView.setVisibility(visible);
            } else {
                Logger.v(TAG, "setGroupChatExpandVisibility() groupChatExpandView is null");
            }
        }

        private void setGroupChatCollapseVisibility(Activity activity, int visible) {
            ImageView groupChatCollapseiew =
                    (ImageView) activity.findViewById(R.id.group_chat_collapse);
            if (groupChatCollapseiew != null) {
                Logger.v(TAG,
                        "setGroupChatCollapseVisibility() groupChatExpandView is not null.visible = "
                                + visible);
                groupChatCollapseiew.setVisibility(visible);
            } else {
                Logger.v(TAG, "setGroupChatCollapseVisibility() groupChatExpandView is null");
            }
        }
    }

    @Override
    public void onClick(View v) {
        Logger.d(TAG, "onClick() entry");
        Activity activity = getActivity();
        if (activity != null) {
            if (v.getId() == R.id.group_chat_expand) {
                expandGroupChat();
            } else if (v.getId() == R.id.group_chat_collapse) {
                collapseGroupChat();
            } else if (v.getId() == R.id.group_chat_title_layout) {
                Logger.v(TAG, "onClick() group_chat_title_layout clicked");
                // Now should judge current status is collapsed or expanded.
                HorizontalScrollView groupChatBannerScroller =
                        (HorizontalScrollView) activity
                                .findViewById(R.id.group_chat_banner_scroller);
                if (groupChatBannerScroller != null
                        && groupChatBannerScroller.getVisibility() == View.VISIBLE) {
                    // Now it is in expanded.
                    collapseGroupChat();
                } else {
                    // Now it is in collapsed.
                    expandGroupChat();
                }
            }
        } else {
            Logger.w(TAG, "onClick() activity is null");
        }
    }
    
    /**
     * Add contacts to current chat fragment.
     */
    @Override
    public boolean addContacts() {
        Logger.d(TAG, "addContacts()");
        int currentParticipantsNum = getParticipantsNum();
        int maxNum = RcsSettings.getInstance().getMaxChatParticipants();
        Logger.d(TAG, "currentParticipantsNum = " + currentParticipantsNum + ", maxNum = " + maxNum);
        if (currentParticipantsNum >= maxNum) {
            mIsMaxGroupChatParticipantsWork = true;
            showToast(R.string.cannot_add_any_more_member);
            return false;
        }
        return super.addContacts();
    }

    /**
     * Check whether current participants is already max. It's used by test case
     * @return True if current participants is already max, otherwise return false.
     */
    public boolean isMaxGroupChatParticipantsWork() {
        return mIsMaxGroupChatParticipantsWork;
    }

    /**
     * Add message to chat adpater and chat list atomic
     */
    private void addMessageAtomic(IChatWindowMessage  msg) {
        synchronized (mLock) {
            mMessageSequenceOrder = mMessageSequenceOrder +1;
            int position = mMessageSequenceOrder;
            // Adding this new message on this position 
            mMessageAdapter.addMessage(msg, position);
            mMessageList.add(position, msg);
        }
    }
}
