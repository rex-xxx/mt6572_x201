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

package com.mediatek.rcse.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.media.MediaFile;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.mediatek.rcse.activities.widgets.ContactsListManager;
import com.mediatek.rcse.api.Logger;
import com.mediatek.rcse.api.Participant;
import com.mediatek.rcse.interfaces.ChatModel.IChatManager;
import com.mediatek.rcse.mvc.ModelImpl;
import com.mediatek.rcse.mvc.One2OneChat;
import com.mediatek.rcse.plugin.message.PluginGroupChatActivity;
import com.mediatek.rcse.plugin.message.PluginGroupChatWindow;
import com.mediatek.rcse.service.ApiManager;
import com.mediatek.rcse.service.RcsNotification;
import com.mediatek.rcse.service.UnreadMessageManager;
import com.mediatek.rcse.service.Utils;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.service.api.client.ClientApiException;
import com.orangelabs.rcs.service.api.client.SessionState;
import com.orangelabs.rcs.service.api.client.messaging.IChatSession;
import com.orangelabs.rcs.service.api.client.messaging.MessagingApi;
import com.orangelabs.rcs.service.api.client.messaging.MessagingApiIntents;

import java.util.ArrayList;
import java.util.List;

/**
 * Provide the activity to show the dialog for user to accept or decline the
 * invitation.
 */
public class InvitationDialog extends Activity {

    private static final String TAG = "InvitationDialog";

    public static final String ACTION = "com.mediatek.rcse.action.INVITE_DIALOG";

    public static final String KEY_STRATEGY = "strategy";

    public static final String KEY_IS_FROM_CHAT_SCREEN = "from";

    public static final int STRATEGY_GROUP_INVITATION = 0;

    public static final int STRATEGY_FILE_TRANSFER_INVITATION = 1;

    public static final int STRATEGY_FILE_TRANSFER_SIZE_WARNING = 2;
    
    public static final int STRATEGY_IPMES_GROUP_INVITATION = 3;

    public static final String SESSION_ID = "sessionId";


    private IInvitationStrategy mCurrentStrategy = null;
    // specific view for dialog.
    private View mContentView = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = this.getIntent();
        int strategyType = intent.getIntExtra(KEY_STRATEGY, -1);
        Logger.d(TAG, "onCreate entry, with strategy: " + strategyType);
        switch (strategyType) {
            case STRATEGY_GROUP_INVITATION:
                mCurrentStrategy = new GroupInvitationStrategy(intent);
                break;
            case STRATEGY_FILE_TRANSFER_INVITATION:
                mCurrentStrategy = new FileTransferInvitationStrategy(intent);
                break;
            case STRATEGY_FILE_TRANSFER_SIZE_WARNING:
                mCurrentStrategy = new FileSizeWarningStrategy(intent);
                break;
            case STRATEGY_IPMES_GROUP_INVITATION:
                mCurrentStrategy = new IpMesPluginGroupInvitationStrategy(intent);
                break;
            default:
                Logger.e(TAG, "onCreate unknown strategy: " + strategyType);
                break;
        }
        if (mCurrentStrategy instanceof GroupInvitationStrategy
                && ((GroupInvitationStrategy) mCurrentStrategy).onUserBehavior() == null) {
            Logger.d(TAG, "onCreate GroupInvitationStrategy time out");
            // Should decrease the number of unread message
            Logger.d(TAG, "Has read the group chat invitation");
            UnreadMessageManager.getInstance().changeUnreadMessageNum(
                    UnreadMessageManager.MIN_STEP_UNREAD_MESSAGE_NUM, false);
            TimeoutDialog dialog = new TimeoutDialog();
            dialog.show(getFragmentManager(), TimeoutDialog.TAG);
            if (mCurrentStrategy instanceof IpMesPluginGroupInvitationStrategy) {
                Logger.d(TAG, "onCreate IpMesPluginGroupInvitationStrategy time out");
                ((IpMesPluginGroupInvitationStrategy) mCurrentStrategy).removeGroupChatInvitationInMms(intent);
            } else {
                Logger.d(TAG, "onCreate not IpMesPluginGroupInvitationStrategy time out");
            }
        } else if (mCurrentStrategy instanceof FileSizeWarningStrategy) {
            FileSizeWarningDialog dialog = new FileSizeWarningDialog();
            dialog.show(getFragmentManager(), FileSizeWarningDialog.TAG);
        } else if (mCurrentStrategy instanceof GroupInvitationStrategy) {
            // Should decrease the number of unread message
            Logger.d(TAG, "Has read the group chat invitation");
            UnreadMessageManager.getInstance().changeUnreadMessageNum(
                    UnreadMessageManager.MIN_STEP_UNREAD_MESSAGE_NUM, false);
            GroupInvitationDialog dialog = new GroupInvitationDialog();
            String content = intent.getStringExtra(RcsNotification.NOTIFY_CONTENT);
            dialog.setContent(content);
            dialog.show(getFragmentManager(), GroupInvitationDialog.TAG);
        } else {
            FileTransferDialog dialog = new FileTransferDialog();
            String fileName = intent.getStringExtra(RcsNotification.NOTIFY_FILE_NAME);
            String fileSize = intent.getStringExtra(RcsNotification.NOTIFY_SIZE);
            String title = intent.getStringExtra(RcsNotification.NOTIFY_TITLE);
            String formatSize = Utils.formatFileSizeToString(Long.parseLong(fileSize),
                    Utils.SIZE_TYPE_TOTAL_SIZE);
            View view = LayoutInflater.from(this).inflate(R.layout.ft_invitation_content, null);
            TextView imageName = (TextView) view.findViewById(R.id.image_name);
            TextView imageSize = (TextView) view.findViewById(R.id.image_size);
            TextView warningMessage = (TextView) view.findViewById(R.id.warning_message);
            long maxFileSize = ApiManager.getInstance().getMaxSizeforFileThransfer();
            long warningFileSize = ApiManager.getInstance().getWarningSizeforFileThransfer();
            Logger.w(TAG, "onCreate() maxFileSize is " + maxFileSize);
            Logger.w(TAG, "onCreate() warningFileSize is " + warningFileSize);
            SharedPreferences sPrefer = PreferenceManager.getDefaultSharedPreferences(InvitationDialog.this);
            Boolean isRemind = sPrefer.getBoolean(SettingsFragment.RCS_REMIND, false);
            Logger.w(TAG, "onCreate(), WarningDialog onCreateDialog the remind status is " + isRemind);
            if (((FileTransferInvitationStrategy) mCurrentStrategy).mFileSize >= warningFileSize
                    && isRemind && warningFileSize != 0) {
                warningMessage.setVisibility(View.VISIBLE);
            } else {
                warningMessage.setVisibility(View.GONE);
            }
            ImageView imageType = (ImageView) view.findViewById(R.id.image_type);
            String mimeType = MediaFile.getMimeTypeForFile(fileName);
            if (mimeType == null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        Utils.getFileExtension(fileName));
            }
            String type = getString(R.string.file_type_file);
            if (mimeType != null) {
                if (mimeType.contains(Utils.FILE_TYPE_IMAGE)) {
                    type = getString(R.string.file_type_image);
                } else if (mimeType.contains(Utils.FILE_TYPE_AUDIO)) {
                    type = getString(R.string.file_type_audio);
                } else if (mimeType.contains(Utils.FILE_TYPE_VIDEO)) {
                    type = getString(R.string.file_type_video);
                } else if (mimeType.contains(Utils.FILE_TYPE_TEXT)) {
                    type = getString(R.string.file_type_text);
                } else if (mimeType.contains(Utils.FILE_TYPE_APP)) {
                    type = getString(R.string.file_type_app);
                }
            }
            Intent it = new Intent(Intent.ACTION_VIEW);
            it.setDataAndType(Utils.getFileNameUri(fileName), mimeType);
            PackageManager packageManager = getPackageManager();
            List<ResolveInfo> list = packageManager.queryIntentActivities(it,
                    PackageManager.MATCH_DEFAULT_ONLY);
            int size = list.size();
            Drawable drawable = getResources().getDrawable(R.drawable.rcs_ic_ft_default_preview);
            if (size > 0) {
                drawable = list.get(0).activityInfo.loadIcon(packageManager);
            }
            imageType.setImageDrawable(drawable);
            imageName.setText(fileName);
            imageSize.setText(formatSize);
            mContentView = view;
            Bundle arguments = new Bundle();
            arguments.putString(Utils.TITLE, type + title);
            dialog.setArguments(arguments);
            dialog.show(getFragmentManager(), FileTransferDialog.TAG);
        }
    }

    private class TimeoutDialog extends DialogFragment implements DialogInterface.OnClickListener {
        private static final String TAG = "TimeoutDialog";

        @Override
        public void onCancel(DialogInterface dialog) {
            dismissAllowingStateLoss();
            finish();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final AlertDialog alertDialog;
            alertDialog =
                    new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT)
                            .setIconAttribute(android.R.attr.alertDialogIcon).create();
            alertDialog.setTitle(R.string.invitation_timeout_title);
            alertDialog.setMessage(getString(R.string.invitation_timeout_message));
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    getString(R.string.rcs_dialog_positive_button), this);
            return alertDialog;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            Activity activity = getActivity();
            if (activity != null) {
                Intent data = new Intent(activity.getIntent());
                String extas = data.getStringExtra(SESSION_ID);
                if (!TextUtils.isEmpty(extas)) {
                    RcsNotification.getInstance().removeGroupInvite(extas);
                } else {
                    Logger.d(TAG, "onClick(),extas is null");
                }
                dismissAllowingStateLoss();
                finish();
            } else {
                dismissAllowingStateLoss();
                Logger.d(TAG, "activity is null");
            }
        }
    }

    private class GroupInvitationDialog extends DialogFragment implements
            DialogInterface.OnClickListener {
        private static final String TAG = "GroupInvitationDialog";
        private String mContent = null;

        @Override
        public void onCancel(DialogInterface dialog) {
            dismissAllowingStateLoss();
            finish();
        }

        public void setContent(String content) {
            mContent = content;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final AlertDialog alertDialog;
            alertDialog = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT)
                    .create();
            alertDialog.setTitle(R.string.chat_invitation_title);
            alertDialog.setMessage(mContent);
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    getString(R.string.file_transfer_button_accept), this);
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                    getString(R.string.file_transfer_button_reject), this);
            return alertDialog;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                if (mCurrentStrategy != null) {
                    mCurrentStrategy.onUserAccept();
                } else {
                    Logger.e(TAG, "onClick accpect, but mCurrentStrategy is null");
                }
            } else {
                if (mCurrentStrategy != null) {
                    mCurrentStrategy.onUserDecline();
                } else {
                    Logger.e(TAG, "onClick decline, but mCurrentStrategy is null");
                }
            }
            dismissAllowingStateLoss();
            finish();
        }
    }

    private class FileTransferDialog extends DialogFragment implements
            DialogInterface.OnClickListener {
        private static final String TAG = "FileTransferDialog";
        private String mTitle = null;

        @Override
        public void onCancel(DialogInterface dialog) {
            dismissAllowingStateLoss();
            finish();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle arguments = getArguments();
            mTitle = arguments.getString(Utils.TITLE);
            final AlertDialog alertDialog;
            alertDialog = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT)
                    .setView(mContentView).create();
            if (mTitle != null) {
                alertDialog.setTitle(mTitle);
            } else {
                alertDialog.setTitle(getString(R.string.file_transfer_title));
            }
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    getString(R.string.file_transfer_button_accept), this);
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                    getString(R.string.file_transfer_button_reject), this);
            return alertDialog;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                if (mCurrentStrategy != null) {
                    mCurrentStrategy.onUserAccept();
                } else {
                    Logger.e(TAG, "onClick accpect, but mCurrentStrategy is null");
                }
            } else {
                if (mCurrentStrategy != null) {
                    mCurrentStrategy.onUserDecline();
                } else {
                    Logger.e(TAG, "onClick decline, but mCurrentStrategy is null");
                }
            }
            dismissAllowingStateLoss();
            finish();
        }
    }

    private class FileSizeWarningDialog extends DialogFragment implements
            DialogInterface.OnClickListener {
        private static final String TAG = "FileSizeWarningDialog";
        private CheckBox mCheckRemind = null;
        private Activity mActivity = null;

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            finish();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final AlertDialog alertDialog;
            mActivity = getActivity();
            alertDialog =
                    new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT).create();
            alertDialog.setTitle(R.string.file_size_warning);
            if (mActivity != null) {
                LayoutInflater inflater = LayoutInflater.from(mActivity.getApplicationContext());
                View customView = inflater.inflate(R.layout.warning_dialog, null);
                mCheckRemind = (CheckBox) customView.findViewById(R.id.remind_notification);
                alertDialog.setView(customView);
            } else {
                Logger.e(TAG, "onCreateDialog activity is null");
            }
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    getString(R.string.rcs_dialog_positive_button), this);
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                    getString(R.string.rcs_dialog_negative_button), this);
            return alertDialog;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                if (mCurrentStrategy != null) {
                    mCurrentStrategy.onUserAccept();
                } else {
                    Logger.e(TAG, "onClick accpect, but mCurrentStrategy is null");
                }
                if (mCheckRemind != null) {
                    boolean isCheck = mCheckRemind.isChecked();
                    SharedPreferences sPrefer =
                            PreferenceManager.getDefaultSharedPreferences(mActivity);
                    Editor remind = sPrefer.edit();
                    remind.putBoolean(SettingsFragment.RCS_REMIND, !isCheck);
                    remind.commit();
                }
            } else {
                if (mCurrentStrategy != null) {
                    mCurrentStrategy.onUserDecline();
                } else {
                    Logger.e(TAG, "onClick decline, but mCurrentStrategy is null");
                }
            }
            dismissAllowingStateLoss();
            finish();
        }
    }

    private interface IInvitationStrategy {
        void onUserDecline();

        void onUserAccept();
    }

    private class GroupInvitationStrategy implements IInvitationStrategy {
        private static final String TAG = "GroupInvitationStrategy";
        protected Intent mIntent = null;
        protected String mSessionId = null;

        public GroupInvitationStrategy(Intent intent) {
            mIntent = intent;
        }

        public IChatSession onUserBehavior() {
            // ApiManager
            ApiManager instance = ApiManager.getInstance();
            if (instance == null) {
                Logger.d(TAG, "onUserBehavior() The ApiManager instance is null");
                finish();
                return null;
            }

            // MessagingApi
            MessagingApi messageApi = instance.getMessagingApi();
            if (messageApi == null) {
                Logger.d(TAG, "onUserBehavior() The messageApi is null");
                finish();
                return null;
            }

            // Invitation intent list
            ArrayList<Intent> invitations = new ArrayList<Intent>();
            invitations.add(mIntent);

            // session id
            String sessionId = invitations.get(invitations.size() - 1).getStringExtra(SESSION_ID);
            Logger.d(TAG, "onUserBehavior() sessionId + " + sessionId);
            mSessionId = sessionId;
            if (sessionId == null) {
                Logger.d(TAG, "onUserBehavior() The sessionId is null");
                return null;
            }

            // ChatSession
            IChatSession chatSession = null;
            try {
                chatSession = messageApi.getChatSession(sessionId);
            } catch (ClientApiException e) {
                e.printStackTrace();
            } finally {
                if (chatSession == null) {
                    Logger.d(TAG, "onUserBehavior() The chatSession is null");
                    return null;
                }
            }
            return chatSession;
        }

        @Override
        public void onUserAccept() {
            IChatSession chatSession = onUserBehavior();
            if (TextUtils.isEmpty(mSessionId)) {
                Logger.d(TAG, "onUserAccept() mSessionId is empty");
                return;
            }
            if (null != chatSession) {
                Logger.d(TAG, "onUserAccept() chatSession is not null");
                RcsNotification.getInstance().removeGroupInvite(mSessionId);
                try {
                    int state = chatSession.getSessionState();
                    Logger.v(TAG, "onUserAccept() state = " + state);
                    if (state == SessionState.TERMINATED) {
                        Logger.d(TAG, "onUserAccept() This group chat invitation has been timeout");
                        chatSession.cancelSession();
                        return;
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                mIntent.setClass(InvitationDialog.this, ChatScreenActivity.class);
                startActivity(mIntent);
            } else {
                Logger.e(TAG,
                        "onUserAccept() chatSession is null.Use session id delete group chat invite");
                RcsNotification.getInstance().removeGroupInvite(mSessionId);
            }
        }

        @Override
        public void onUserDecline() {
            final IChatSession chatSession = onUserBehavior();
            if (null != chatSession) {
                RcsNotification.getInstance().removeGroupInvite(mSessionId);
                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... arg0) {
                        try {
                            chatSession.rejectSession();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                };
                task.execute();
            } else {
                Logger
                        .e(TAG,
                                "onUserDecline() chatSession is null.Use session id delete group chat invite");
                RcsNotification.getInstance().removeGroupInvite(mSessionId);
            }
        }

    }
    
    private class IpMesPluginGroupInvitationStrategy extends GroupInvitationStrategy {

        private static final String TAG = "IpMesPluginGroupInvitationStrategy";

        public IpMesPluginGroupInvitationStrategy(Intent intent) {
            super(intent);
        }

        @Override
        public void onUserAccept() {
            removeGroupChatInvitationInMms(mIntent);

            IChatSession chatSession = onUserBehavior();
            if (TextUtils.isEmpty(mSessionId)) {
                Logger.d(TAG, "onUserAccept() mSessionId is empty");
                return;
            }
            if (null != chatSession) {
                Logger.d(TAG, "onUserAccept() chatSession is not null");
                RcsNotification.getInstance().removeGroupInvite(mSessionId);
                try {
                    int state = chatSession.getSessionState();
                    Logger.v(TAG, "onUserAccept() state = " + state);
                    if (state == SessionState.TERMINATED) {
                        Logger.d(TAG, "onUserAccept() This group chat invitation has been timeout");
                        chatSession.cancelSession();
                        return;
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                String action = mIntent.getAction();

                if (Logger.getIsIntegrationMode()) {
                    Logger.d(TAG, "onUserAccept() action is " + action);
                    if (ACTION.equals(action)) {
                        Logger.d(TAG, "onUserAccept() accept from conversation list");
                        Intent invitation = new Intent();
                        invitation.setClass(InvitationDialog.this, PluginGroupChatActivity.class);
                        invitation.setAction(MessagingApiIntents.CHAT_INVITATION);
                        invitation.putExtras(mIntent.getExtras());
                        invitation.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(invitation);
                    } else {
                        Logger.d(TAG, "onUserAccept() accept from notification");
                        mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        mIntent.setClass(InvitationDialog.this, PluginGroupChatActivity.class);
                        startActivity(mIntent);
                    }
                } else {
                    Logger.d(TAG, "onUserAccept() is chat app mode");
                    mIntent.setClass(InvitationDialog.this, PluginGroupChatActivity.class);
                    startActivity(mIntent);
                }
                Logger.d(TAG, "onUserAccept() action is " + mIntent.getAction());
            } else {
                Logger.e(TAG,
                        "onUserAccept() chatSession is null.Use session id delete group chat invite");
                RcsNotification.getInstance().removeGroupInvite(mSessionId);
            }
        }
        
        @Override
        public void onUserDecline() {
            Logger.d(TAG, "onUserDecline entry mIntent is " + mIntent);
            removeGroupChatInvitationInMms(mIntent);
            super.onUserDecline();
        }
        
        /**
         * Remove group chat invitation message in mms
         * @param intent The invitation intent
         */
        public void removeGroupChatInvitationInMms(Intent intent) {
            Logger.d(TAG, "removeGroupChatInvitationInMms in InvitationDialog entry");
            String contact = intent.getStringExtra(PluginGroupChatWindow.GROUP_CHAT_CONTACT);
            if (null != contact) {
                PluginGroupChatWindow.removeGroupChatInvitationInMms(contact);
            } else {
                Logger.w(TAG, "removeGroupChatInvitationInMms contact is null");
            }
        }
    }

    /**
     * This is a strategy for File Transfer invitations
     */
    private class FileTransferInvitationStrategy implements IInvitationStrategy {
        private static final String TAG = "FileTransferInvitationStrategy";
        private long mFileSize = 0;
        private String mContactName = null;
        private String mContactNumber = null;
        Intent mIntent = null;
        private boolean mIsFromChatScreen = false;

        public FileTransferInvitationStrategy(Intent intent) {
            Logger.d(TAG, "FileTransferInvitationStrategy() entry with intent is " + intent);
            mIntent = intent;
            String fileSize = intent.getStringExtra(RcsNotification.NOTIFY_SIZE);
            mContactNumber = intent.getStringExtra(RcsNotification.CONTACT);
            mContactName = ContactsListManager.getInstance().getDisplayNameByPhoneNumber(mContactNumber);
            Logger.d(TAG, "FileTransferInvitationStrategy(), mContactName is " + mContactName + " mContactNumber is "
                    + mContactNumber);
            mIsFromChatScreen = intent.getBooleanExtra(KEY_IS_FROM_CHAT_SCREEN, false);
            if (fileSize != null) {
                mFileSize = Long.parseLong(fileSize);
                Logger.i(TAG, "FileTransferInvitationStrategy() mFileSize is " + mFileSize);
            } else {
                Logger.e(TAG, "FileTransferInvitationStrategy() filesize is null");
            }
        }

        private String getSessionId() {
            String sessionId = null;
            if (mIntent == null) {
                Logger.d(TAG, "getSessionId(), mIntent is null");
            } else {
                sessionId = mIntent.getStringExtra(RcsNotification.SESSION_ID);
            }
            Logger.d(TAG, "getSessionId(), sessionId is " + sessionId);
            return sessionId;
        }

        private One2OneChat getChat() {
            One2OneChat chat = null;
            if (mIntent == null) {
                Logger.d(TAG, "getChat(), mIntent is null");
                return null;
            }
            Participant fromSessionParticipant = new Participant(mContactNumber, mContactName);
            List<Participant> participantList = new ArrayList<Participant>();
            participantList.add(fromSessionParticipant);
            IChatManager modelImpl = ModelImpl.getInstance();
            if (modelImpl != null) {
                chat = (One2OneChat) ModelImpl.getInstance().addChat(participantList, null);
            } else {
                Logger.e(TAG, "getChat(), modelImpl is null");
            }
            Logger.d(TAG, "getChat(), chat is " + chat);
            return chat;
        }

        private void accessToChatWindow(One2OneChat chat) {
            if (chat == null || mIntent == null) {
                Logger.d(TAG, "AccessToChatWindow)(), chat is null or mIntent is null");
                return;
            }
            Intent intent = new Intent();
            intent.setClass(InvitationDialog.this, ChatScreenActivity.class);
            intent.putExtra(ChatScreenActivity.KEY_CHAT_TAG, (ParcelUuid) chat.getChatTag());
            startActivity(intent);
        }

        @Override
        public void onUserAccept() {
            Logger.d(TAG, "onUserAccept() entry!");
            handleAcceptInvitation();
        }

        protected void handleAcceptInvitation() {
            final One2OneChat chat = getChat();
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... arg0) {
                    if (null != chat) {
                        chat.handleAcceptFileTransfer(getSessionId());
                    } else {
                        Logger.e(TAG, "handleAcceptInvitation(), chat is null!");
                    }
                    return null;
                }
            };
            task.execute();
            if (!mIsFromChatScreen) {
                accessToChatWindow(chat);
            } else {
                Logger.d(TAG, "handleAcceptInvitation, The chat is in the foreground!");
            }
        }

        @Override
        public void onUserDecline() {
            final One2OneChat chat = getChat();
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... arg0) {
                    if (chat != null) {
                        chat.handleRejectFileTransfer(getSessionId());
                    } else {
                        Logger.e(TAG, "onUserDecline(), chat is null!");
                    }
                    return null;
                }
            };
            task.execute();
            RcsNotification.getInstance().cancelFileTransferNotificationWithContact(mContactNumber,
                    mFileSize);
        }
    }

    private class FileSizeWarningStrategy extends FileTransferInvitationStrategy {
        public FileSizeWarningStrategy(Intent intent) {
            super(intent);
        }

        @Override
        public void onUserAccept() {
            InvitationDialog.this.finish();
            handleAcceptInvitation();
        }
    }
}
