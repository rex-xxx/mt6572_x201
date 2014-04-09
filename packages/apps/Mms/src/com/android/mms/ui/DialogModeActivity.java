/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.mms.ui;

import android.app.Activity;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper; 
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.data.WorkingMessage.MessageStatusListener;
import com.android.mms.data.WorkingMessage;
import com.android.mms.model.FileAttachmentModel;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.model.TextModel;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.ui.MsgContentSlideView.MsgContentSlideListener;
import com.android.mms.ui.MsgNumSlideview.MsgNumBarSlideListener;
import com.android.mms.util.AddressUtils;
import com.android.mms.util.SmileyParser2;
import com.android.mms.widget.MmsWidgetProvider;

import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPersister;

import com.mediatek.encapsulation.android.provider.EncapsulatedSettings;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSmsManager;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedTelephonyService;
import com.mediatek.encapsulation.com.google.android.mms.EncapsulatedContentType;
import com.mediatek.encapsulation.com.mediatek.CellConnService.EncapsulatedCellConnMgr;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.com.mediatek.telephony.EncapsulatedTelephonyManagerEx;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.ipmsg.util.IpMessageUtils;

/// M: add for ipmessage
import android.media.ThumbnailUtils;
import android.provider.MediaStore.Video.Thumbnails;
import android.widget.ProgressBar;

import com.mediatek.ipmsg.ui.GifView;
import com.mediatek.mms.ipmessage.INotificationsListener;
import com.mediatek.mms.ipmessage.IpMessageConsts;
import com.mediatek.mms.ipmessage.IpMessageConsts.DownloadAttachStatus;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageSendMode;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageStatus;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageType;
import com.mediatek.mms.ipmessage.IpMessageConsts.RemoteActivities;
import com.mediatek.mms.ipmessage.message.IpAttachMessage;
import com.mediatek.mms.ipmessage.message.IpImageMessage;
import com.mediatek.mms.ipmessage.message.IpLocationMessage;
import com.mediatek.mms.ipmessage.message.IpMessage;
import com.mediatek.mms.ipmessage.message.IpTextMessage;
import com.mediatek.mms.ipmessage.message.IpVCalendarMessage;
import com.mediatek.mms.ipmessage.message.IpVCardMessage;
import com.mediatek.mms.ipmessage.message.IpVideoMessage;
import com.mediatek.mms.ipmessage.message.IpVoiceMessage;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** M:
 * Dialog mode
 */
public class DialogModeActivity extends Activity implements
    MsgNumBarSlideListener,
    MsgContentSlideListener,
    OnClickListener,
    SlideViewInterface,
    INotificationsListener,
    MessageStatusListener {

    private int mCurUriIdx;
    private Uri mCurUri;
    //private MsgNumSlideview mMsgNumBar;
    private ImageButton mLeftArrow;
    private ImageButton mRightArrow;
    private TextView mMsgNumText;
    private TextView mSender;
    //private MsgContentSlideView mContentLayout;
    private TextView mSmsContentText;
    /// M: fix bug ALPS00434826, show emotion in DialogModeActivity SMS TextView
    private SmileyParser2 mParser = SmileyParser2.getInstance();
    private TextView mRecvTime;
    private TextView mSimName;
    private ImageView mContactImage;
    private View mMmsView;
    /// M: fix for bug ALPS00434945, add for vcard and vcalendar.{
    private View mMmsAttachView;
    private ImageView mMmsAttachImageView;
    private TextView mAttachName;
    private TextView mAttachSize;
    /// @}
    private ImageView mMmsImageView;
    private ImageButton mMmsPlayButton;
    private EditText mReplyEditor;
    private ImageButton mSendButton;
    private TextView mTextCounter;
    private Button mMarkAsReadBtn;
    private Button mDeleteBtn;
    //private ImageButton mCloseBtn;
    private AsyncDialog mAsyncDialog;   // Used for background tasks.
    private Cursor mCursor;
    //private boolean mWaitingForSubActivity;
    private DialogModeReceiver mReceiver;
    private boolean mContentViewSet;
    //private AlertDialog mSIMSelectDialog;
    private int mAssociatedSimId;
    private int mSelectedSimId;
    private WorkingMessage mWorkingMessage;
    private boolean mSendingMessage;
    private boolean mWaitingForSendMessage;
    private EncapsulatedCellConnMgr mCellMgr = null;
    private int mSimCount;
    private List<SIMInfo> mSimInfoList;
    
    private static Drawable sDefaultContactImage;
    
    private int mPage = MmsConfig.getSmsToMmsTextThreshold();

    private static final String TAG = "Mms/DialogMode";
    private static final int SMS_ID = 0;
    private static final int SMS_TID = 1;
    private static final int SMS_ADDR = 2;
    private static final int SMS_DATE = 3;
    private static final int SMS_READ = 4;
    private static final int SMS_BODY = 5;
    private static final int SMS_SIM = 6;
    private static final int SMS_TYPE = 7;
    private static final String TYPE_MMS = "mms";
    private static final String TYPE_SMS = "sms";
    private static final int REQUEST_CODE_ECM_EXIT_DIALOG = 107;
    private static final String EXIT_ECM_RESULT = "exit_ecm_result";
    private static final String SELECT_TYPE = "Select_type";
    private static final int SIM_SELECT_FOR_SEND_MSG = 1;
    
    /// M: fix bug ALPS00446919, merge back from ALPS.JB2.MP to ALPS.JB2
    //private final ArrayList<Uri> mUris;
    private ArrayList<Uri> mUris;

    Runnable mResetMessageRunnable = new Runnable() {
        public void run() {
            MmsLog.d(TAG, "mResetMessageRunnable.run");
            resetMessage();
        }
    };

    Runnable mMessageSentRunnable = new Runnable() {
        public void run() {
            MmsLog.d(TAG, "mMessageSentRunnable.run");
            String body = getString(R.string.strOk);
            MmsLog.d(TAG, "string=" + body);
            Toast.makeText(getApplicationContext(), body, Toast.LENGTH_SHORT).show();
        }
    };

    private final TextWatcher mTextEditorWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            return;
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            MmsLog.d(TAG, "mTextEditorWatcher.onTextChanged");
            // mWorkingMessage.setText(s);
            updateSendButtonState();
            updateCounter(s, start, before, count);
        }

        public void afterTextChanged(Editable s) {
            return;
        }
    };

    /// M:
    private ArrayList<Uri> mReadedUris;
    /** M: this variable is used for a special issue.
     *  when use click mms's play button, MmsPlayerActivity will be started, and close this activity.
     *  but the invocation is async, so this activity's finish will be call first.
     *  when framework started MmsPlayerActivity it will check the invoking activity,
     *  because this activity is finishing, the new created activity will not put in front.
     *  so add this flag to control finish activity in onStop.
     */
    private boolean mNeedFinish = false;

    /// M: add for ipmessage
    private TextView mGroupSender;
    private final ArrayList<Uri> mIpMessageUris;
    private TextView mBodyTextView;
    /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms
    private TextView mGroupMmsSender;
    private boolean mIsGroupMms;
    /// M: add for image and video
    private View mIpImageView; // ip_image
    private ImageView mImageContent; // image_content
    private View mIpImageSizeBg; // image_size_bg
    private ImageView mActionButton; // action_btn
    private TextView mContentSize; // content_size
    private ProgressBar mImageDownloadProgressBar; // image_downLoad_progress
    private View mCaptionSeparator; // caption_separator
    private TextView mCaption; // text_caption
    private ImageView mMediaPlayView;
    /// M: add for audio
    private View mIpAudioView; // ip_audio
    private ImageView mAudioIcon; // ip_audio_icon
    private TextView mAudioInfo; // audio_info
    private ProgressBar mAudioDownloadProgressBar; // audio_downLoad_progress
    /// M: add for vCard
    private View mIpVCardView;
    private ImageView mVCardIcon;
    private TextView mVCardInfo;
    /// M: add for vcalendar
    private View mIpVCalendarView;
    private TextView mVCalendarInfo;
    /// M: add for location
    private View mIpLocationView; // ip_location
    private ImageView mImageLocation; // img_location
    private TextView mLocationAddr; // location_addr
    /// M: add for emoticon
    private View mIpEmoticonView; // ip_emoticon
    private GifView mGifView; // gif_content

    public DialogModeActivity() {
        mUris = new ArrayList<Uri>();
        mReadedUris = new ArrayList<Uri>();
        mIpMessageUris = new ArrayList<Uri>();
        mCurUriIdx = 0;
        mCurUri = null;
        mCursor = null;
        mMmsView = null;
        mMmsImageView = null;
        mMmsPlayButton = null;
        mCellMgr = null;
        mReceiver = null;
        mContentViewSet = false;
        mWorkingMessage = null;
        //mWaitingForSubActivity = false;
        /// M: fix for bug ALPS00434945, add for vcard and vcalendar.{
        mMmsAttachView = null;
        mMmsAttachImageView = null;
        mAttachName = null;
        mAttachSize = null;
        /// @}
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /// M: fix bug ALPS00446919, merge back from ALPS.JB2.MP to ALPS.JB2
        if(savedInstanceState != null){
            if(!savedInstanceState.isEmpty()){
                mUris = savedInstanceState.getParcelableArrayList("uris");
                mCurUriIdx = savedInstanceState.getInt("cururiidx");
                if(mUris != null && mCurUriIdx <= mUris.size() - 1){
                    mUris.remove(mCurUriIdx);
                }
            }
        }
        /// M: do not finish activity when touch outside
        setFinishOnTouchOutside(false);
        MmsLog.d(TAG, "DialogModeActivity.onCreate");
        if (!isHome()) {
            MmsLog.d(TAG, "not at Home, just finish");
            finish();
            return;
        }

        MmsLog.d(TAG, "at Home");
        registerReceiver();
        /// M: fix bug ALPS00446919, merge back from ALPS.JB2.MP to ALPS.JB2
        //addNewUri(getIntent());
        addNewUriforReCreate(getIntent());
        if (loadCurMsg() == null) {
            return;
        }
        initDialogView();
        setDialogView();

        if (mCellMgr == null) {
            mCellMgr = new EncapsulatedCellConnMgr();
            mCellMgr.register(getApplication());
        }
        /// M: add for ipmessage, notification listener
        IpMessageUtils.addIpMsgNotificationListeners(this, this);

        resetMessage();
    }
    
    /// M: fix bug ALPS00446919, merge back from ALPS.JB2.MP to ALPS.JB2
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("uris", mUris);
        outState.putInt("cururiidx", mCurUriIdx);
    }

    private void registerReceiver() {
        MmsLog.d(TAG, "DialogModeActivity.registerReceiver");
        if (mReceiver != null) {
            return;
        }
        MmsLog.d(TAG, "register receiver");
        mReceiver = new DialogModeReceiver();
        IntentFilter filter = new IntentFilter("com.android.mms.dialogmode.VIEWED");
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        MmsLog.d(TAG, "DialogModeActivity.onDestroy");
        if (mCellMgr != null) {
            mCellMgr.unregister();
            mCellMgr = null;
        }
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        IpMessageUtils.removeIpMsgNotificationListeners(this, this);

        super.onDestroy();
    }

    @Override
    public void onNewIntent(Intent intent) {
        MmsLog.d(TAG, "DialogModeActivity.onNewIntent");
        super.onNewIntent(intent);
        setIntent(intent);

        registerReceiver();
        addNewUri(intent);
        loadCurMsg();
        initDialogView();
        setDialogView();
        if (mCellMgr == null) {
            mCellMgr = new EncapsulatedCellConnMgr();
            mCellMgr.register(getApplication());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MmsLog.d(TAG, "DialogModeActivity.onActivityResult, requestCode=" + requestCode
            + ", resultCode=" + resultCode + ", data=" + data);
        //mWaitingForSubActivity = false;

        if (resultCode != RESULT_OK) {
            MmsLog.d(TAG, "fail due to resultCode=" + resultCode);
            return;
        }

        if (requestCode == REQUEST_CODE_ECM_EXIT_DIALOG) {
                boolean outOfEmergencyMode = data.getBooleanExtra(EXIT_ECM_RESULT, false);
                MmsLog.d(TAG, "REQUEST_CODE_ECM_EXIT_DIALOG, mode=" + outOfEmergencyMode);
                if (outOfEmergencyMode) {
                    sendMessage(false);
                }
        } else {
                MmsLog.d(TAG, "bail due to unknown requestCode=" + requestCode);
        }
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        MmsLog.d(TAG, "DialogModeActivity.startActivityForResult");
        /*
         *if (requestCode >= 0) {
         *mWaitingForSubActivity = true;
         *}*/
        super.startActivityForResult(intent, requestCode);
    }

    private AsyncDialog getAsyncDialog() {
        if (mAsyncDialog == null) {
            mAsyncDialog = new AsyncDialog(this);
        }
        return mAsyncDialog;
    }

    private void resetMessage() {
        MmsLog.d(TAG, "DialogModeActivity.resetMessage");

        // mReplyEditor.requestFocus();

        // We have to remove the text change listener while the text editor gets cleared and
        // we subsequently turn the message back into SMS. When the listener is listening while
        // doing the clearing, it's fighting to update its counts and itself try and turn
        // the message one way or the other.
        mReplyEditor.removeTextChangedListener(mTextEditorWatcher);

        // Clear the text box.
        TextKeyListener.clear(mReplyEditor.getText());

        if (mWorkingMessage != null) {
            MmsLog.d(TAG, "clear working message");
            //mWorkingMessage.clearConversation(getConversation(), false);
            // mWorkingMessage = WorkingMessage.createEmpty(this);
            // mWorkingMessage.setConversation(getConversation());
            mWorkingMessage = null;
        }

        updateSendButtonState();
        mReplyEditor.addTextChangedListener(mTextEditorWatcher);
        mReplyEditor.setText("");
        mSendingMessage = false;
    }

    public void onPreMessageSent() {
        MmsLog.d(TAG, "DialogModeActivity.onPreMessageSent");
        runOnUiThread(mResetMessageRunnable);
    }

    public void onMessageSent() {
        MmsLog.d(TAG, "DialogModeActivity.onMessageSent");
        mWaitingForSendMessage = false;
        // String body = getString(R.string.strOk);
        // MmsLog.d(TAG, "string=" + body);
        // Toast.makeText(DialogModeActivity.this, body, Toast.LENGTH_SHORT).show();
        runOnUiThread(mMessageSentRunnable);
    }

    public void onProtocolChanged(boolean mms, boolean needToast) {
        MmsLog.d(TAG, "DialogModeActivity.onProtocolChanged");
    }

    public void onAttachmentChanged() {
        MmsLog.d(TAG, "DialogModeActivity.onAttachmentChanged");
    }

    public void onPreMmsSent() {
        MmsLog.d(TAG, "DialogModeActivity.onPreMmsSent");
    }

    public void onMaxPendingMessagesReached() {
        MmsLog.d(TAG, "DialogModeActivity.onMaxPendingMessagesReached");
    }

    public void onAttachmentError(int error) {
        MmsLog.d(TAG, "DialogModeActivity.onAttachmentError");
    }
    
    /// M: fix bug ALPS00446919, merge back from ALPS.JB2.MP to ALPS.JB2
    private void addNewUriforReCreate(Intent intent) {
        if (intent == null) {
            return;
        }
        String newString = intent.getStringExtra("com.android.mms.transaction.new_msg_uri");
        MmsLog.d(TAG, "DialogModeActivity.addNewUri, new uri=" + newString);
        Uri newUri = Uri.parse(newString);
        mUris.add(mUris.size(), newUri);
        //mCurUriIdx = mUris.size() - 1;
        MmsLog.d(TAG, "new index=" + mCurUriIdx);
        if (intent.getBooleanExtra("ipmessage", false)) {
            MmsLog.d(TAG, "receiver a ipmessage,uri:" + newUri.toString());
            mIpMessageUris.add(newUri);
        }
    }

    private void addNewUri(Intent intent) {
        if (intent == null) {
            return;
        }

        String newString = intent.getStringExtra("com.android.mms.transaction.new_msg_uri");
        MmsLog.d(TAG, "DialogModeActivity.addNewUri, new uri=" + newString);
        Uri newUri = Uri.parse(newString);
        mUris.add(mUris.size(), newUri);
        mCurUriIdx = mUris.size() - 1;
        MmsLog.d(TAG, "new index=" + mCurUriIdx);
        if (intent.getBooleanExtra("ipmessage", false)) {
            MmsLog.d(TAG, "receiver a ipmessage,uri:" + newUri.toString());
            mIpMessageUris.add(newUri);
        }
    }

    private void initDialogView() {
        MmsLog.d(TAG, "DialogModeActivity.initDialogView");

        if (mContentViewSet) {
            MmsLog.d(TAG, "have init");
            return;
        }

        // initDislogSize();
        setContentView(R.layout.msg_dlg_activity);
        mContentViewSet = true;
        getSimInfoList();

        // Msg number bar
        //mMsgNumBar = (MsgNumSlideview)findViewById(R.id.msg_number_bar_linear);
        mLeftArrow = (ImageButton)findViewById(R.id.previous);
        mLeftArrow.setOnClickListener(this);
        mRightArrow = (ImageButton)findViewById(R.id.next);
        mRightArrow.setOnClickListener(this);
        mMsgNumText = (TextView) findViewById(R.id.msg_counter);
        // mMsgNumBar.registerFlingListener(this);

        mSender = (TextView) findViewById(R.id.recepient_name);
        Typeface tf = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        if (tf != null) {
            mSender.setTypeface(tf);
        }
        //mCloseBtn = (ImageButton)findViewById(R.id.close_button);
        //mCloseBtn.setOnClickListener(this);

        MsgContentSlideView contentLayout;
        contentLayout = (MsgContentSlideView) findViewById(R.id.content_scroll_view);
        contentLayout.registerFlingListener(this);
        mGroupSender = (TextView)findViewById(R.id.group_sender);
        /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms
        mGroupMmsSender = (TextView)findViewById(R.id.group_mms_sender);
        mSmsContentText = (TextView) findViewById(R.id.msg_content);
        mBodyTextView = mSmsContentText;
        //mSmsContentText.setClickable(true);
        //mSmsContentText.setOnClickListener(this);
        if (tf != null) {
            mSmsContentText.setTypeface(tf);
        }
        mRecvTime = (TextView) findViewById(R.id.msg_recv_timer);
        if (tf != null) {
            mRecvTime.setTypeface(tf);
        }

        LinearLayout simInfo = (LinearLayout) findViewById(R.id.sim_info_linear);
        simInfo.setVisibility(View.VISIBLE);

        mSimName = (TextView) findViewById(R.id.sim_name);
        mSimName.setVisibility(View.VISIBLE);

        // mSimVia = (TextView)findViewById(R.id.sim_via_text);
        // mSimVia.setVisibility(View.VISIBLE);

        if (tf != null) {
            mSimName.setTypeface(tf);
            // mSimVia.setTypeface(tf);
        }

        mContactImage = (ImageView)findViewById(R.id.contact_img);
        sDefaultContactImage = getApplicationContext().getResources().getDrawable(
            R.drawable.ic_contact_picture);
        /*
         * mMmsView = findViewById(R.id.msg_dlg_mms_view); mMmsView.setVisibility(View.GONE); mMmsImageView =
         * (ImageView) findViewById(R.id.msg_dlg_image_view); mMmsImageView.setVisibility(View.GONE); mMmsPlayButton =
         * (ImageButton) findViewById(R.id.msg_dlg_play_slideshow_button); mMmsPlayButton.setVisibility(View.GONE);
         */
        mReplyEditor = (EditText) findViewById(R.id.embedded_reply_text_editor);
        mReplyEditor.addTextChangedListener(mTextEditorWatcher);
        mReplyEditor
                .setFilters(new InputFilter[] {new TextLengthFilter(MmsConfig.getMaxTextLimit())});
        /// M: if ipmessage is enabled. show another hint
        if (MmsConfig.isServiceEnabled(this)) {
            mReplyEditor.setHint(IpMessageUtils.getResourceManager(this)
                .getSingleString(IpMessageConsts.string.ipmsg_type_to_compose_text));
        }
        mSendButton = (ImageButton) findViewById(R.id.reply_send_button);
        mSendButton.setOnClickListener(this);
        mTextCounter = (TextView) findViewById(R.id.text_counter);

        mMarkAsReadBtn = (Button) findViewById(R.id.mark_as_read_btn);
        mMarkAsReadBtn.setOnClickListener(this);

        mDeleteBtn = (Button) findViewById(R.id.delete_btn);
        mDeleteBtn.setOnClickListener(this);

        mReplyEditor.setText("");

        /// M: add for ipmessage
        /// M: add for image and video
        mIpImageView = (View) findViewById(R.id.ip_image);
        mImageContent = (ImageView) findViewById(R.id.image_content);
        mIpImageSizeBg = (View) findViewById(R.id.image_size_bg);
        mActionButton = (ImageView) findViewById(R.id.action_btn);
        mContentSize = (TextView) findViewById(R.id.content_size);
        mImageDownloadProgressBar = (ProgressBar) findViewById(R.id.image_downLoad_progress);
        mCaptionSeparator = (View) findViewById(R.id.caption_separator);
        mCaption = (TextView) findViewById(R.id.text_caption);
        mMediaPlayView = (ImageView) findViewById(R.id.video_media_play);
        /// M: add for audio
        mIpAudioView = (View) findViewById(R.id.ip_audio);
        mAudioIcon = (ImageView) findViewById(R.id.ip_audio_icon);
        mAudioInfo = (TextView) findViewById(R.id.audio_info);
        mAudioDownloadProgressBar = (ProgressBar) findViewById(R.id.audio_downLoad_progress);
        /// M: add for vCard
        mIpVCardView = (View) findViewById(R.id.ip_vcard);
        mVCardIcon = (ImageView)findViewById(R.id.ip_vcard_icon);
        mVCardInfo = (TextView) findViewById(R.id.vcard_info);
        /// M: add for vCalendar
        mIpVCalendarView = (View) findViewById(R.id.ip_vcalendar);
        mVCalendarInfo = (TextView) findViewById(R.id.vcalendar_info);
        /// M: add for location
        mIpLocationView = (View) findViewById(R.id.ip_location);
        mImageLocation = (ImageView) findViewById(R.id.img_location);
        mLocationAddr = (TextView) findViewById(R.id.location_addr);
        /// M: add for emoticon
        mIpEmoticonView = (View) findViewById(R.id.ip_emoticon);
        mGifView = (GifView) findViewById(R.id.gif_content);
    }

    private void setDialogView() {
        MmsLog.d(TAG, "DialogModeActivity.setDialogView");

        // Msg count bar
        int msgNum = mUris.size();
        if (msgNum <= 1) {
            mLeftArrow.setVisibility(View.INVISIBLE);
            mRightArrow.setVisibility(View.INVISIBLE);
            mMsgNumText.setVisibility(View.INVISIBLE);
        } else {
            mLeftArrow.setVisibility(View.VISIBLE);
            mRightArrow.setVisibility(View.VISIBLE);
            mMsgNumText.setVisibility(View.VISIBLE);
            StringBuilder msgNumStrBuilder = new StringBuilder("");
            msgNumStrBuilder.append(mCurUriIdx + 1);
            msgNumStrBuilder.append('/');
            msgNumStrBuilder.append(msgNum);
            String msgNumStr = msgNumStrBuilder.toString();
            mMsgNumText.setText(msgNumStr);
        }

        mSender.setText(getSenderString());
        /// M: fix bug ALPS00434826, show emotion in DialogModeActivity SMS TextView
        mSmsContentText.setText(mParser.addSmileySpans(getSmsContent()));
        mRecvTime.setText(getReceivedTime());

        mSimName.setText(MessageUtils.getSimInfo(this,(int)getCurrentSimId()));

        /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms
        if (mIsGroupMms) {
            mContactImage.setImageDrawable(sDefaultContactImage);
        } else {
            Drawable image = getContactImage();
            if (image != null) {
                mContactImage.setImageDrawable(image);
            }
        }

        // add for ipmessage
        if (isCurIpMessage()) {
            showIpMessage();
            // change send button to blue
            updateSendButtonState();
            return;
        } else {
            // hide ipmessage views and show sms view.
            mGroupSender.setVisibility(View.GONE);
            mIpImageView.setVisibility(View.GONE);
            mCaptionSeparator.setVisibility(View.GONE);
            mCaption.setVisibility(View.GONE);
            /// M: add for ipmessage, hide audio or vcard view
            mIpAudioView.setVisibility(View.GONE);
            mIpVCardView.setVisibility(View.GONE);
            mIpVCalendarView.setVisibility(View.GONE);
            /// M: add for ipmessage, hide location view
            mIpLocationView.setVisibility(View.GONE);
            mIpEmoticonView.setVisibility(View.GONE);
            mBodyTextView.setVisibility(View.VISIBLE);
            updateSendButtonState();
        }
        if (isCurSMS()) {
            if (mMmsView != null) {
                MmsLog.d(TAG, "Hide MMS views");
                mMmsView.setVisibility(View.GONE);
            }
            if (mMmsAttachView != null) {
                MmsLog.d(TAG, "Hide MMS vcard or vcalendar views");
                mMmsAttachView.setVisibility(View.GONE);
            }
        } else {
           MmsLog.d(TAG, "a MMS");
           loadMmsView(); 
        }

    }

    private void initDislogSize() {
        // Display display = getWindowManager().getDefaultDisplay();
        // setTheme(R.style.MmsHoloTheme);
        // setTheme(R.style.SmsDlgScreen);
        setContentView(R.layout.msg_dlg_activity);
        // LayoutParams p = getWindow().getAttributes();
        // p.height = (int)(display.getHeight() / 3);
        // getWindow().setAttributes(p);
    }

    private List<String> getHomes() {
        MmsLog.d(TAG, "DialogModeActivity.getHomes");

        List<String> names = new ArrayList<String>();
        PackageManager packageManager = this.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent,
            PackageManager.MATCH_DEFAULT_ONLY);

        for (ResolveInfo ri : resolveInfo) {
            names.add(ri.activityInfo.packageName);
            // System.out.println(ri.activityInfo.packageName);
            // System.out.println(ri.activityInfo.name);
            MmsLog.d(TAG, "package name=" + ri.activityInfo.packageName);
            MmsLog.d(TAG, "class name=" + ri.activityInfo.name);
        }
        return names;
    }

    public boolean isHome() {
        List<String> homePackageNames = getHomes();

        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> rti = activityManager.getRunningTasks(2);

        int rtiSize = rti.size();
        if (rtiSize <= 0) {
            MmsLog.d(TAG, "rti is empty!");
            return false;
        } else if (rtiSize == 1) {
            /* Only one task, check it is launcher or not*/
            RunningTaskInfo info = rti.get(0);
            String packageName0 = info.topActivity.getPackageName();
            String className0 = info.topActivity.getClassName();
            int num0 = info.numActivities;
            MmsLog.d(TAG, "package0= " + packageName0 + " class0=" + className0 + " num0=" + num0);

            return homePackageNames.contains(packageName0);
        }
        /* Else there are tasks > 1*/
        RunningTaskInfo info = rti.get(0);
        String packageName0 = info.topActivity.getPackageName();
        String className0 = info.topActivity.getClassName();
        //String baseClass = info.get(0).baseActivity.getClassName();
        int num0 = info.numActivities;
        MmsLog.d(TAG, "package0=" + packageName0 + " class0=" + className0 + " num0=" + num0);

        info = rti.get(1);
        String packageName1 = info.topActivity.getPackageName();
        String className1 = info.topActivity.getClassName();
        MmsLog.d(TAG, "package1=" + packageName1 + "class1=" + className1);

        boolean ret;
        /* Below is Launcher?*/
        ret = homePackageNames.contains(packageName1);
        if (ret) {
            if ("com.android.mms.ui.DialogModeActivity".equals(className0) &&
                num0 == 1) {
                ret = true;
            } else {
                ret = false;
            }
        }
        return ret;
    }

    public void onSlideToPrev() {
        int msgNum = mUris.size();

        MmsLog.d(TAG, "DialogModeActivity.onSlideToPrev, num=" + msgNum);

        if (msgNum <= 1) {
            return;
        }
        if (mCurUriIdx == 0) {
            return;
        }
        if (mCurUri != null && !mReadedUris.contains(mCurUri)) {
            mReadedUris.add(mCurUri);
        }
        mCurUriIdx--;
        loadCurMsg();
        setDialogView();
    }

    public void onSlideToNext() {
        int msgNum = mUris.size();

        MmsLog.d(TAG, "DialogModeActivity.onSlideToNext, num=" + msgNum);

        if (msgNum <= 1) {
            return;
        }
        if (mCurUriIdx == (msgNum - 1)) {
            return;
        }
        if (mCurUri != null && !mReadedUris.contains(mCurUri)) {
            mReadedUris.add(mCurUri);
        }
        mCurUriIdx++;
        loadCurMsg();
        setDialogView();
    }

    private boolean isCurSMS() {
        MmsLog.d(TAG, "DialogModeActivity.isCurSMS");
        mCurUri = (Uri) mUris.get(mCurUriIdx);

        if (mCurUri == null) {
            MmsLog.d(TAG, "no uri available");
            mCursor = null;
            return true;
        }

        // List<String> segs = mCurUri.getPathSegments();
        // String type = segs.get(0);
        String type = mCurUri.getAuthority();
        MmsLog.d(TAG, "type=" + type);
        if (type.equals(TYPE_SMS)) {
            return true;
        }

        return false;
    }

    private Cursor loadCurMsg() {
        MmsLog.d(TAG, "DialogModeActivity.loadCurMsg, idx=" + mCurUriIdx);

        if (mCurUriIdx >= mUris.size()) {
            MmsLog.d(TAG, "index out of size. exit dialog");
            mCursor = null;
            mCurUri = null;
            mCurUriIdx = 0;
            finish();
            return null;
        }

        mCurUri = (Uri) mUris.get(mCurUriIdx);
        if (mCurUri == null) {
            MmsLog.d(TAG, "no uri available");
            if (mCursor != null) {
                mCursor.close();
                mCursor = null;
            }
            finish();
            return null;
        }

        MmsLog.d(TAG, "uri=" + mCurUri.toString());
        String projection[];
        if (isCurSMS()) {
            projection = new String[] {"_id", "thread_id", "address", "date", "read", "body",
                "sim_id"};
        } else {
            projection = new String[] {"_id", "thread_id", "null as address", "date", "read",
                "sub", "sim_id", "m_type"};
        }

        if (mCursor != null) {
            mCursor.close();
        }

        Cursor cursor = getContentResolver().query(mCurUri, projection, null, null, null);

        if (cursor == null) {
            MmsLog.d(TAG, "no msg found");
            mCursor = null;
            finish();
            return null;
        }
        /// M: this is a invalid uri, load the next.
        if (cursor.getCount() == 0) {
            cursor.close();
            mUris.remove(mCurUriIdx);
            if (mUris.size() > 0) {
                return loadCurMsg();
            }
            finish();
            return null;
        }
        /*
         * if (cursor.moveToFirst()) { long id = cursor.getLong(SMS_ID); MmsLog.d(TAG, "id=" + id); long tid
         * =cursor.getLong(SMS_TID); MmsLog.d(TAG, "tid=" + tid); String addr = cursor.getString(SMS_ADDR); MmsLog.d(TAG,
         * "addr=" + addr); long date = cursor.getLong(SMS_DATE); MmsLog.d(TAG, "date=" + date); int read =
         * cursor.getInt(SMS_READ); MmsLog.d(TAG, "read=" + read); String body = cursor.getString(SMS_BODY); MmsLog.d(TAG,
         * "body=" + body); int sim = cursor.getInt(SMS_SIM); MmsLog.d(TAG, "sim=" + sim); } else { MmsLog.d(TAG,
         * "moveToFirst fail"); }
         */
        mCursor = cursor;
        return cursor;
    }

    private void deleteCurMsg() {
        MmsLog.d(TAG, "DialogModeActivity.deleteCurMsg");

        mCurUri = (Uri) mUris.get(mCurUriIdx);
        if (mCurUri == null) {
            MmsLog.d(TAG, "no uri available");
            // mCursor = null;
            return;
        }

        MmsLog.d(TAG, "uri=" + mCurUri.toString());

        DeleteMessageListener l = new DeleteMessageListener(mCurUri);
        confirmDeleteDialog(l);
    }

    private void confirmDeleteDialog(android.content.DialogInterface.OnClickListener listener) {
        MmsLog.d(TAG, "DialogModeActivity.confirmDeleteDialog");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.confirm_dialog_title);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setCancelable(true);
        builder.setMessage(R.string.confirm_delete_message);
        builder.setPositiveButton(R.string.delete, listener);
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    private class DeleteMessageListener implements android.content.DialogInterface.OnClickListener {
        private final Uri mDeleteUri;

        public DeleteMessageListener(Uri uri) {
            mDeleteUri = uri;
        }

        /*
         * public DeleteMessageListener(long msgId, String type) { if ("mms".equals(type)) { mDeleteUri =
         * ContentUris.withAppendedId(Mms.CONTENT_URI, msgId); } else { mDeleteUri =
         * ContentUris.withAppendedId(Sms.CONTENT_URI, msgId); } }
         */
        public void onClick(DialogInterface dialog, int whichButton) {
            MmsLog.d(TAG, "DeleteMessageListener.onClick, " + mDeleteUri.toString());
            // mBackgroundQueryHandler.startDelete(DELETE_MESSAGE_TOKEN,
            // null, mDeleteUri, null, null);
            SqliteWrapper.delete(getApplicationContext(), getContentResolver(), mDeleteUri, null,
                null);

            DialogModeActivity.this.removeMsg(mDeleteUri);
            MessagingNotification.nonBlockingUpdateNewMessageIndicator(DialogModeActivity.this,
                MessagingNotification.THREAD_NONE, false);
            dialog.dismiss();
        }
    }

    private void removeCurMsg() {
        MmsLog.d(TAG, "DialogModeActivity.removeCurMsg");

        mCurUri = (Uri) mUris.get(mCurUriIdx);
        if (mCurUri == null) {
            MmsLog.d(TAG, "no uri available");
            // mCursor = null;
            return;
        }
        MmsLog.d(TAG, "uri=" + mCurUri.toString());
        mUris.remove(mCurUriIdx);
        if (mUris.isEmpty()) {
            MmsLog.d(TAG, "no msg");
            finish();
            return;
        }

        if (mCurUriIdx != 0) {
            mCurUriIdx--;
        }

        loadCurMsg();
        setDialogView();
    }

    private void removeMsg(Uri deleteUri) {
        MmsLog.d(TAG, "DialogModeActivity.removeMsg + " + deleteUri);

        int idx = mUris.indexOf(deleteUri);
        if (idx == mCurUriIdx) {
            removeCurMsg();
            return;
        }

        mUris.remove(idx);
        if (mUris.isEmpty()) {
            MmsLog.d(TAG, "no msg");
            finish();
            return;
        }

        if (mCurUriIdx != 0) {
            mCurUriIdx--;
        }

        loadCurMsg();
        setDialogView();
    }
    
    private String getSenderString() {
        MmsLog.d(TAG, "DialogModeActivity.getSenderString");

        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return "";
        }

        if (mCursor.moveToFirst()) {
            if (isCurSMS()) {
                if (isCurGroupIpMessage()) {
                    return getCurGroupIpMessageName();
                }
                String recipientIds = mCursor.getString(SMS_ADDR);
                ContactList recipients;
                recipients = ContactList.getByNumbers(recipientIds, false, true);
                // MmsLog.d(TAG, "recipients=" + recipientIds);
                MmsLog.d(TAG, "recipients=" + recipients.formatNames(", "));
                return recipients.formatNames(", ");
            } else {
                Conversation conv = Conversation.get(this, getThreadId(), true);
                if (conv == null) {
                    MmsLog.d(TAG, "conv null");
                    return "";
                }
                ContactList recipients = conv.getRecipients();
                /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms
                mIsGroupMms = MmsPreferenceActivity.getIsGroupMmsEnabled(DialogModeActivity.this)
                                            && recipients.size() > 1;
                MmsLog.d(TAG, "recipients=" + recipients.formatNames(", "));
                return recipients.formatNames(", ");
            }
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            return "";
        }
    }

    private String getSenderNumber() {
        MmsLog.d(TAG, "DialogModeActivity.getSenderNumber");

        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return "";
        }

        if (mCursor.moveToFirst()) {
            if (isCurSMS()) {
                if (isCurGroupIpMessage()) {
                    return getCurGroupIpMessageNumber();
                }
                String addr = mCursor.getString(SMS_ADDR);
                MmsLog.d(TAG, "addr=" + addr);
                return addr;
            } else {
                Conversation conv = Conversation.get(this, getThreadId(), true);
                if (conv == null) {
                    MmsLog.d(TAG, "conv null");
                    return "";
                }
                ContactList recipients = conv.getRecipients();
                String[] numbers = recipients.getNumbers();

                if (numbers == null) {
                    MmsLog.d(TAG, "empty number");
                    return "";
                } else {
                    MmsLog.d(TAG, "number0=" + numbers[0]);
                    return numbers[0];
                }
            }
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            return "";
        }
    }

    private String getSmsContent() {
        MmsLog.d(TAG, "DialogModeActivity.getSmsContent");

        if (!isCurSMS()) {
            return "";
        }

        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return "";
        }

        if (mCursor.moveToFirst()) {
            String content = mCursor.getString(SMS_BODY);
            MmsLog.d(TAG, "content=" + content);
            return content;
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            return "";
        }
    }

    private String getReceivedTime() {
        MmsLog.d(TAG, "DialogModeActivity.getReceivedTime");

        StringBuilder builder = new StringBuilder("");
        //builder.append(getString(R.string.received_header));
        //builder.append(" ");

        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return builder.toString();
        }

        if (mCursor.moveToFirst()) {
            long date = mCursor.getLong(SMS_DATE);
            String strDate;

            if (isCurSMS()) {
                strDate = MessageUtils.formatTimeStampString(getApplicationContext(), date);
            } else {
                strDate = MessageUtils.formatTimeStampString(getApplicationContext(), date * 1000L);
            }

            MmsLog.d(TAG, "date=" + strDate);
            builder.append(strDate);
            return builder.toString();
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            return builder.toString();
        }
    }

    private long getCurrentSimId() {
        long simId = -1;
        if (mCursor == null) {
            return simId;
        }
        if (mCursor.moveToFirst()) {
            simId = mCursor.getLong(SMS_SIM);
        }
        MmsLog.d(TAG, "getCurrentSimId:" + simId);
        return simId;
    }

    private String getSIMName() {
        MmsLog.d(TAG, "DialogModeActivity.getSIMName");

        StringBuilder builder = new StringBuilder("");
        // builder.append(" ");

        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return builder.toString();
        }

        if (mCursor.moveToFirst()) {
            long simId = mCursor.getLong(SMS_SIM);
            MmsLog.d(TAG, "sim=" + simId);
            SIMInfo simInfo = SIMInfo.getSIMInfoById(getApplicationContext(), simId);
            builder.append(simInfo.getDisplayName());
            return builder.toString();
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            return builder.toString();
        }
    }

    private int getSIMColor() {
        MmsLog.d(TAG, "DialogModeActivity.getSIMColor");

        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return com.mediatek.internal.R.drawable.sim_background_locked;
        }

        if (mCursor.moveToFirst()) {
            long simId = mCursor.getLong(SMS_SIM);
            MmsLog.d(TAG, "sim=" + simId);
            SIMInfo simInfo = SIMInfo.getSIMInfoById(getApplicationContext(), simId);
            MmsLog.d(TAG, "color=" + simInfo.getSimBackgroundLightRes());
            MmsLog.d(TAG, "color=" + simInfo.getColor());
            return simInfo.getSimBackgroundLightRes();
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            return com.mediatek.internal.R.drawable.sim_background_locked;
        }
    }

    private Drawable getContactImage() {
        MmsLog.d(TAG, "DialogModeActivity.getContactImage");

        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return sDefaultContactImage;
        }

        if (mCursor.moveToFirst()) {
            ContactList recipients;
            boolean isGroup = false;
            if (isCurGroupIpMessage()) {
                isGroup = true;
                recipients = getConversation().getRecipients();
            } else if (isCurSMS()) {
                String recipientIds = mCursor.getString(SMS_ADDR);
                recipients = ContactList.getByNumbers(recipientIds, false, true);
            } else {
                Conversation conv = Conversation.get(this, getThreadId(), true);
                if (conv == null) {
                    MmsLog.d(TAG, "conv null");
                    return sDefaultContactImage;
                }
                recipients = conv.getRecipients();
                if (recipients == null) {
                    return sDefaultContactImage;
                }
            }
            Contact contact = recipients.get(0);
            if (contact == null) {
                MmsLog.d(TAG, "no contact");
                return sDefaultContactImage;
            }
            if (isGroup) {
                return contact.getGroupAvatar(getApplicationContext(), getThreadId());
            }
            return contact.getAvatar(getApplicationContext(), sDefaultContactImage, getThreadId());
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            return sDefaultContactImage;
        }
    }

    private long getThreadId() {
        MmsLog.d(TAG, "DialogModeActivity.getThreadId");

        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return -1;
        }

        if (mCursor.moveToFirst()) {
            long tid = mCursor.getLong(SMS_TID);
            MmsLog.d(TAG, "tid=" + tid);
            return tid;
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            return -1;
        }
    }

    private Conversation getConversation() {
        MmsLog.d(TAG, "DialogModeActivity.getConversation");
        long tid = getThreadId();
        if (tid < 0) {
            MmsLog.d(TAG, "invalid tid");
            return null;
        }

        MmsLog.d(TAG, "tid=" + tid);
        Conversation conv = Conversation.get(this, tid, true); // new Conversation(this, tid, true);
        if (conv == null) {
            MmsLog.d(TAG, "conv null");
            return null;
        }

        return conv;
    }

    // Implements OnClickListener
    public void onClick(View v) {
        MmsLog.d(TAG, "DialogModeActivity.onClick");
        if (v.equals(mSmsContentText)) {
            MmsLog.d(TAG, "Clicent content view");
            openThread(getThreadId());
        } else if (v.equals(mMmsPlayButton)) { // PLay MMS
            MmsLog.d(TAG, "View this MMS");
            MessageUtils.viewMmsMessageAttachment(this, mCurUri, null, getAsyncDialog());
            if (mCurUri != null && !mReadedUris.contains(mCurUri)) {
                mReadedUris.add(mCurUri);
            }
            markAsRead(mReadedUris);
            /// M: see this variable's note
            mNeedFinish = true;
        } else if (v.equals(mSendButton)) {
            MmsLog.d(TAG, "Send SMS");
            sendReplySms();
        } else if (v.equals(mMarkAsReadBtn)) {
            // change the mark as read button to close button.
            if (mCursor != null) {
                mCursor.close();
                mCursor = null;
            }
            if (mCurUri != null && !mReadedUris.contains(mCurUri)) {
                mReadedUris.add(mCurUri);
            }
            markAsRead(mReadedUris);
            /// M: update widget
            MmsWidgetProvider.notifyDatasetChanged(getApplicationContext());
            finish();
        } else if (v == mDeleteBtn) {
            // change the delete button to view button.
            MmsLog.d(TAG, "view the message thread");
            markAsSeen(mReadedUris);
            if (isCurGroupIpMessage()) {
                /// M: open group chat
                openIpMsgThread(getThreadId(), false);
            } else {
                openThread(getThreadId());
            }
            //deleteCurMsg();
        } else if (v == mLeftArrow) {
            onSlideToPrev();
        } else if (v == mRightArrow) {
            onSlideToNext();
        }
    }

    private void openThread(long threadId) {
        MmsLog.d(TAG, "DialogModeActivity.openThread " + threadId);

        if (MmsConfig.getMmsDirMode()) {
            MmsLog.d(TAG, "go to inbox");

            Intent it = new Intent(this, FolderViewList.class);
            it.putExtra("floderview_key", FolderViewList.OPTION_INBOX);
            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(it);
        } else {
            if (threadId < 0) {
                return;
            }
            startActivity(ComposeMessageActivity.createIntent(this, threadId));
        }
        finish();
    }

    private void loadMmsView() {
        MmsLog.d(TAG, "DialogModeActivity.loadMmsView ");

        if (mMmsView == null) {
            MmsLog.d(TAG, "set Mms views visible");
            // findViewById(R.id.mms_thumbnail_stub).setVisibility(View.VISIBLE);

            mMmsView = findViewById(R.id.msg_dlg_mms_view);
            mMmsImageView = (ImageView) findViewById(R.id.msg_dlg_image_view);
            mMmsPlayButton = (ImageButton) findViewById(R.id.msg_dlg_play_slideshow_button);

            mMmsPlayButton.setVisibility(View.VISIBLE);
        }
        mMmsImageView.setVisibility(View.GONE);
        if (mMmsAttachView == null) {
            mMmsAttachView = findViewById(R.id.vca_dlg_image_view);
            mMmsAttachImageView = (ImageView) findViewById(R.id.vca_image_view);
            mAttachName = (TextView)findViewById(R.id.file_attachment_name_info);
            mAttachSize = (TextView)findViewById(R.id.file_attachment_size_info);
        }
        loadMmsContents();
    }

    private void sendReplySms() {
        MmsLog.d(TAG, "DialogModeActivity.sendReplySms");
        /// M: if this is a group ipmessage, just reply with the card it from.
        if (isCurGroupIpMessage()) {
            String body = mReplyEditor.getText().toString();
            int simId = (int)mCursor.getLong(SMS_SIM);
            String to = getSenderNumber();
            MmsLog.d(TAG, "sendIpTextMessage[group], to:" + to + ",simId:" + simId + ",body:" + body);
            sendIpTextMessage(body, simId, to);
            mCurUri = (Uri) mUris.get(mCurUriIdx);
            markAsRead(mCurUri);
            return;
        } else {
            simSelection();
        }
    }

    private void simSelection() {
        MmsLog.d(TAG, "DialogModeActivity.simSelection");

        if (!EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            MmsLog.d(TAG, "non GEMINI");
            confirmSendMessageIfNeeded();
        } else if (mSimCount == 0) {
            MmsLog.d(TAG, "no card");
            // SendButton can't click in this case
        } else if (mSimCount == 1) {
            MmsLog.d(TAG, "1 card");
            mSelectedSimId = (int) mSimInfoList.get(0).getSimId();
            confirmSendMessageIfNeeded();
        } else if (mSimCount > 1) {
            MmsLog.d(TAG, "multi cards");
            Intent intent = new Intent();
            intent.putExtra(SELECT_TYPE, SIM_SELECT_FOR_SEND_MSG);
            // getContactSIM
            String number = getSenderNumber();
            if (number == null || number.length() == 0) {
                mAssociatedSimId = -1;
            } else {
                mAssociatedSimId = getContactSIM(number);
            }
            MmsLog.d(TAG, "mAssociatedSimId=" + mAssociatedSimId);

            long messageSimId;
            messageSimId = EncapsulatedSettings.System.getLong(getContentResolver(),
                EncapsulatedSettings.System.SMS_SIM_SETTING, EncapsulatedSettings.System.DEFAULT_SIM_NOT_SET);
            MmsLog.d(TAG, "messageSimId=" + messageSimId);

            if (messageSimId == EncapsulatedSettings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK) {
                // always ask, show SIM selection dialog
                showSimSelectedDialog(intent);
                updateSendButtonState();
            } else if (messageSimId == EncapsulatedSettings.System.DEFAULT_SIM_NOT_SET) {
                if (mAssociatedSimId == -1) {
                    showSimSelectedDialog(intent);
                    updateSendButtonState();
                } else {
                    mSelectedSimId = mAssociatedSimId;
                    confirmSendMessageIfNeeded();
                }
            } else if (messageSimId == EncapsulatedSettings.System.SMS_SIM_SETTING_AUTO && MmsConfig.getFolderModeEnabled()) {
                long simId = getCurrentSimId();
                for (int index = 0; index < mSimCount; index ++) {
                    if (mSimInfoList.get(index).getSimId() == simId){
                        mSelectedSimId = (int)simId;
                        break;
                    }
                }
                if (mSelectedSimId == simId) {
                    confirmSendMessageIfNeeded();
                }else {
                    showSimSelectedDialog(intent);
                    updateSendButtonState();
                }
            } else {
                if (mAssociatedSimId == -1 || (messageSimId == mAssociatedSimId)) {
                    mSelectedSimId = (int) messageSimId;
                    confirmSendMessageIfNeeded();
                } else {
                    showSimSelectedDialog(intent);
                    updateSendButtonState();
                }
            }
        }
    }

    private int getContactSIM(String number) {
        MmsLog.d(TAG, "DialogModeActivity.getContactSIM, " + number);

        int simId = -1;
        Cursor associateSIMCursor = DialogModeActivity.this.getContentResolver().query(
            Data.CONTENT_URI,
            new String[] {Data.SIM_ASSOCIATION_ID},
            Data.MIMETYPE + "='" + CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "' AND (" + Data.DATA1
                + "='" + number + "') AND (" + Data.SIM_ASSOCIATION_ID + "!= -1)", null, null);
        try {
            if (null == associateSIMCursor) {
                MmsLog.i(TAG, " queryContactInfo : associateSIMCursor is null");
            } else {
                MmsLog.i(TAG, " queryContactInfo : associateSIMCursor is not null. Count["
                    + associateSIMCursor.getCount() + "]");
            }
    
            if ((null != associateSIMCursor) && (associateSIMCursor.getCount() > 0)) {
                associateSIMCursor.moveToFirst();
                // Get only one record is OK
                simId = (Integer) associateSIMCursor.getInt(0);
            } else {
                simId = -1;
            }
            MmsLog.d(TAG, "simId=" + simId);
        } finally {
            if (associateSIMCursor != null) {
                associateSIMCursor.close();
            }
        }
        return simId;
    }

    private void showSimSelectedDialog(Intent intent) {
        MmsLog.d(TAG, "DialogModeActivity.showSimSelectedDialog");

        // TODO get default SIM and get contact SIM
        final Intent it = intent;
        List<Map<String, ?>> entries = new ArrayList<Map<String, ?>>();
        for (int i = 0; i < EncapsulatedPhone.GEMINI_SIM_NUM; i++) {
            SIMInfo simInfo = SIMInfo.getSIMInfoBySlot(this, i);
            if (simInfo == null) {
                continue;
            }
            HashMap<String, Object> entry = new HashMap<String, Object>();

            entry.put("simIcon", simInfo.getSimBackgroundLightRes());
            int state = MessageUtils.getSimStatus(i, mSimInfoList, EncapsulatedTelephonyManagerEx.getDefault());
            entry.put("simStatus", MessageUtils.getSimStatusResource(state));
            /// M:
            if (MmsConfig.isServiceEnabled(this, (int) simInfo.getSimId())) {
                MmsLog.d(TAG, "show ipmessage icon, simId = " + simInfo.getSimId());
                entry.put("ipmsg_indicator", IpMessageConsts.drawable.ipmsg_sim_indicator);
            } else {
                MmsLog.d(TAG, "hide ipmessage icon, simId = " + simInfo.getSimId());
                entry.put("ipmsg_indicator", 0);
            }
            String simNumber = "";
            if (!TextUtils.isEmpty(simInfo.getNumber())) {
                switch (simInfo.getDispalyNumberFormat()) {
                    // case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_DEFAULT:
                    case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_FIRST:
                        if (simInfo.getNumber().length() <= 4) {
                            simNumber = simInfo.getNumber();
                        } else {
                            simNumber = simInfo.getNumber().substring(0, 4);
                        }
                        break;
                    case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_LAST:
                        if (simInfo.getNumber().length() <= 4) {
                            simNumber = simInfo.getNumber();
                        } else {
                            simNumber = simInfo.getNumber().substring(simInfo.getNumber().length() - 4);
                        }
                        break;
                    case 0:// android.provider.Telephony.SimInfo.DISPLAY_NUMBER_NONE:
                        simNumber = "";
                        break;
                    default:
                        break;
                }
            }
            if (TextUtils.isEmpty(simNumber)) {
                entry.put("simNumberShort", "");
            } else {
                entry.put("simNumberShort", simNumber);
            }

            entry.put("simName", simInfo.getDisplayName());
            if (TextUtils.isEmpty(simInfo.getNumber())) {
                entry.put("simNumber", "");
            } else {
                entry.put("simNumber", simInfo.getNumber());
            }
            if (mAssociatedSimId == (int) simInfo.getSimId()) {
                // if this SIM is contact SIM, set "Suggested"
                entry.put("suggested", getString(R.string.suggested));
            } else {
                entry.put("suggested", "");// not suggested
            }
            entries.add(entry);
        }

        final SimpleAdapter a = MessageUtils.createSimpleAdapter(entries, this);
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(getString(R.string.sim_selected_dialog_title));
        b.setCancelable(true);
        b.setAdapter(a, new DialogInterface.OnClickListener() {
            @SuppressWarnings("unchecked")
            public final void onClick(DialogInterface dialog, int which) {
                updateSendButtonState();
                mSelectedSimId = (int) mSimInfoList.get(which).getSimId();
                if (it.getIntExtra(SELECT_TYPE, -1) == SIM_SELECT_FOR_SEND_MSG) {
                    confirmSendMessageIfNeeded();
                }
                dialog.dismiss();
            }
        });
        AlertDialog simSelectDialog = b.create();
        simSelectDialog.show();
    }

    private void confirmSendMessageIfNeeded() {
        MmsLog.d(TAG, "DialogModeActivity.confirmSendMessageIfNeeded");
        checkConditionsAndSendMessage(true);
    }

    private void checkConditionsAndSendMessage(boolean bCheckEcmMode) {
        MmsLog.d(TAG, "DialogModeActivity.checkConditionsAndSendMessage");
        // check pin
        // convert sim id to slot id
         int requestType = EncapsulatedCellConnMgr.REQUEST_TYPE_SIMLOCK;
        final int slotId;
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            MmsLog.d(TAG, "GEMINI");
            requestType = EncapsulatedCellConnMgr.REQUEST_TYPE_ROAMING;
            slotId = SIMInfo.getSlotById(this, mSelectedSimId);
            MmsLog.d(TAG, "check pin and...: simId=" + mSelectedSimId + "\t slotId=" + slotId);
        } else {
            MmsLog.d(TAG, "none GEMILNI");
            slotId = 0;
            SIMInfo si = SIMInfo.getSIMInfoBySlot(this, slotId);
            if (si == null) {
                MmsLog.e(TAG, "si is null");
            } else {
                MmsLog.d(TAG, "simid=" + si.getSimId());
                mSelectedSimId = (int)si.getSimId();
            }
        }
        final boolean bCEM = bCheckEcmMode;
        if (mCellMgr == null) {
            MmsLog.d(TAG, "mCellMgr is null!");
        } else {
            mCellMgr.handleCellConn(slotId, requestType, new Runnable() {
                public void run() {
                    MmsLog.d(TAG, "mCellMgr.run");

                    int nRet = mCellMgr.getResult();
                    MmsLog.d(TAG, "serviceComplete result = " + EncapsulatedCellConnMgr.resultToString(nRet));
                    if (mCellMgr.RESULT_ABORT == nRet || mCellMgr.RESULT_OK == nRet) {
                        updateSendButtonState();
                        return;
                    }
                    if (slotId != mCellMgr.getPreferSlot()) {
                        MmsLog.d(TAG, "111");
                        SIMInfo si = SIMInfo.getSIMInfoBySlot(DialogModeActivity.this, mCellMgr
                                .getPreferSlot());
                        if (si == null) {
                            MmsLog.e(TAG, "serviceComplete siminfo is null");
                            updateSendButtonState();
                            return;
                        }
                        mSelectedSimId = (int) si.getSimId();
                    }
                    sendMessage(bCEM);
                }
            });
        }
    }

    private void sendMessage(boolean bCheckEcmMode) {
        MmsLog.d(TAG, "DialogModeActivity.sendMessage," + bCheckEcmMode);

        if (bCheckEcmMode) {
            MmsLog.d(TAG, "bCheckEcmMode=" + bCheckEcmMode);

            // TODO: expose this in telephony layer for SDK build
            String inEcm = SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE);
            if (Boolean.parseBoolean(inEcm)) {
                try {
                    MmsLog.d(TAG, "show notice to block others");
                    startActivityForResult(new Intent(
                            TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                        REQUEST_CODE_ECM_EXIT_DIALOG);
                    return;
                } catch (ActivityNotFoundException e) {
                    // continue to send message
                    MmsLog.e(TAG, "Cannot find EmergencyCallbackModeExitDialog", e);
                }
            }
        }
        /*
         * ContactList contactList = isRecipientsEditorVisible() ? mRecipientsEditor.constructContactsFromInput(false) :
         * getRecipients(); mDebugRecipients = contactList.serialize();
         */
        /// M: add for ipmessage
        if (isCurIpMessage() && MmsConfig.isServiceEnabled(this, mSelectedSimId)) {
            String body = mReplyEditor.getText().toString();
            int simId = mSelectedSimId;
            String to = getSenderNumber();
            MmsLog.d(TAG, "sendIpTextMessage, to:" + to + ",simId:" + simId + ",body:" + body);
            sendIpTextMessage(body, simId, to);
            mCurUri = (Uri) mUris.get(mCurUriIdx);
            markAsRead(mCurUri);
            return;
        }

        if (!mSendingMessage) {
            /*
             * if (LogTag.SEVERE_WARNING) { String sendingRecipients = mConversation.getRecipients().serialize(); if
             * (!sendingRecipients.equals(mDebugRecipients)) { String workingRecipients =
             * mWorkingMessage.getWorkingRecipients(); if (!mDebugRecipients.equals(workingRecipients)) {
             * LogTag.warnPossibleRecipientMismatch("ComposeMessageActivity.sendMessage" + " recipients in window: \"" +
             * mDebugRecipients + "\" differ from recipients from conv: \"" + sendingRecipients +
             * "\" and working recipients: " + workingRecipients, this); } } sanityCheckConversation(); }
             */

            // send can change the recipients. Make sure we remove the listeners first and then add
            // them back once the recipient list has settled.
            // removeRecipientsListeners();
            MmsLog.d(TAG, "new working message");
            mWorkingMessage = WorkingMessage.createEmpty(this, this);
            // mWorkingMessage.setMessageStatusListener(this);
            mWorkingMessage.setConversation(getConversation());
            mWorkingMessage.setText(mReplyEditor.getText());

            mWorkingMessage.send("", mSelectedSimId);
            mSendingMessage = true;
            mWaitingForSendMessage = true;
            mCurUri = (Uri) mUris.get(mCurUriIdx);
            markAsRead(mCurUri);
        }
    }

    private void loadMmsContents() {
        MmsLog.d(TAG, "DialogModeActivity.loadMmsContents");

        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return;
        }

        if (!mCursor.moveToFirst()) {
            MmsLog.d(TAG, "moveToFirst fail");
            return;
        }

        MmsLog.d(TAG, "cursor ok");
            // check msg type
            int type = mCursor.getInt(SMS_TYPE);
            MmsLog.d(TAG, "type=" + type);

            if (PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND == type) {
                MmsLog.d(TAG, "mms nofity");
                String content;
                content = getNotificationContentString(mCurUri);
                /// M: fix bug ALPS00434826, show emotion in DialogModeActivity SMS TextView
                mSmsContentText.setText(mParser.addSmileySpans(content));
                return;
            }

            // get MMS pdu
            PduPersister p = PduPersister.getPduPersister(this);
            MultimediaMessagePdu msg;
        SlideshowModel slideshow = null;

            try {
                msg = (MultimediaMessagePdu) p.load(mCurUri);
            } catch (MmsException e) {
                MmsLog.d(TAG, e.toString());
                msg = null;
            }

            if (msg == null) {
                MmsLog.d(TAG, "msg null");
                return;
            }

            // get slideshow
        try {
            slideshow = SlideshowModel.createFromPduBody(this, msg.getBody());
        } catch (MmsException e) {
            slideshow = null;
            e.printStackTrace();
        }
            if (slideshow == null) {
                MmsLog.d(TAG, "loadMmsContents(); slideshow null");
            } else {
                MmsLog.d(TAG, "loadMmsContents(); slideshow ok");
            }

            // set Mms content text
            EncodedStringValue subObj = msg.getSubject();
            String subject = null;

            if (subObj != null) {
                subject = subObj.getString();
                MmsLog.d(TAG, "sub=" + subject);
            }

            SpannableStringBuilder buf = new SpannableStringBuilder();
            boolean hasSubject = false;

            // init set a empty string
            buf.append("");

            // add subject
            if ((subject != null) && (subject.length() > 0)) {
                hasSubject = true;

                CharSequence smilizedSubject = mParser.addSmileySpans(subject);

                buf.append(TextUtils.replace(getResources().getString(R.string.inline_subject),
                    new String[] {"%s"}, new CharSequence[] {smilizedSubject}));
                buf.replace(0, buf.length(), mParser.addSmileySpans(buf));
            }

            MmsLog.d(TAG, "with sub=" + buf.toString());

        if (slideshow == null) {
            MmsLog.d(TAG, "slideshow null");
            mMmsView.setVisibility(View.GONE);
            mMmsAttachView.setVisibility(View.GONE);
            if (buf.length() == 0) {
                mSmsContentText.setText("        ");
            }
        } else {
                // append first text to content
                SlideModel slide = slideshow.get(0);
                String body;

                if ((slide != null) && slide.hasText()) {
                    TextModel tm = slide.getText();
                    body = tm.getText();
                    MmsLog.d(TAG, "body=" + body);

                    if (hasSubject) {
                        buf.append(" - ");
                    }
                    buf.append(mParser.addSmileySpans(body));
                } else { // First slide no text
                    if (!hasSubject) {
                        buf.append("        ");
                    }
                }
                MmsLog.d(TAG, "with cont=" + buf.toString());
                mSmsContentText.setText(buf);

                // Set Mms play button
                boolean needPresent = false;
                for (int i = 0; i < slideshow.size(); i++) {
                    MmsLog.d(TAG, "check slide" + i);
                    slide = slideshow.get(i);
                    if (slide.hasImage() || slide.hasVideo() || slide.hasAudio()) {
                        MmsLog.d(TAG, "found");
                        needPresent = true;
                        break;
                    }
                }

                if (!needPresent) {
                if (slideshow.size() > 1) {
                        needPresent = true;
                }
            }
                if (needPresent) {
                        MmsLog.d(TAG, "present slidehsow");
                        Presenter presenter = PresenterFactory.getPresenter(
                            "MmsThumbnailPresenter", this, this, slideshow);
                        presenter.present(null);
                        mMmsPlayButton.setOnClickListener(this);
                        mMmsAttachView.setVisibility(View.GONE);
                        setSmsContentTextView();
                } else {
                    MmsLog.d(TAG, "no media");
                    mMmsView.setVisibility(View.GONE);
                    /// M: fix for bug ALPS00434945, add for vcard and vcalendar.{
                    setVcardOrVcalendar(slideshow);
                }
        }

        /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms @{
        if (mIsGroupMms) {
            mGroupMmsSender.setVisibility(View.VISIBLE);
            String name = interpretFrom(msg.getFrom(), mCurUri) + ":";
            mGroupMmsSender.setText(name);
        } else {
            mGroupMmsSender.setVisibility(View.GONE);
        }
        /// @}
    }

    private String getNotificationContentString(Uri uri) {
        MmsLog.d(TAG, "DialogModeActivity.getNotificationContentString");

        PduPersister p = PduPersister.getPduPersister(this);
        NotificationInd msg;

        try {
            msg = (NotificationInd) p.load(mCurUri);
            /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms @{
            if (mIsGroupMms) {
                mGroupMmsSender.setVisibility(View.VISIBLE);
                String name = interpretFrom(msg.getFrom(), mCurUri) + ":";
                mGroupMmsSender.setText(name);
            } else {
                mGroupMmsSender.setVisibility(View.GONE);
            }
            /// @}
        } catch (MmsException e) {
            MmsLog.d(TAG, e.toString());
            return "";
        }
        if (msg == null) {
            MmsLog.d(TAG, "msg null");
            return "";
        }

        String msgSizeText = this.getString(R.string.message_size_label)
            + String.valueOf((msg.getMessageSize() + 1023) / 1024)
            + this.getString(R.string.kilobyte);

        String timestamp = this.getString(R.string.expire_on, MessageUtils.formatTimeStampString(
            this, msg.getExpiry() * 1000L));

        String ret = msgSizeText + "\r\n" + timestamp;
        MmsLog.d(TAG, "ret=" + ret);

        return ret;
    }

    // SlideshowModel mSlideshow;
    private SlideshowModel getSlideShow() {
        MmsLog.d(TAG, "DialogModeActivity.getSlideShow ");
        if (mCursor == null) {
            MmsLog.d(TAG, "mCursor null");
            return null;
        }

        if (mCursor.moveToFirst()) {
            MmsLog.d(TAG, "cursor ok");

                PduPersister p = PduPersister.getPduPersister(this);
                int type = mCursor.getInt(SMS_TYPE);
                MmsLog.d(TAG, "type=" + type);

                if (PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND == type) {
                    MmsLog.d(TAG, "mms nofity");
                    return null;
                }

                MultimediaMessagePdu msg;
                try {
                    msg = (MultimediaMessagePdu) p.load(mCurUri);
                } catch (MmsException e) {
                    MmsLog.d(TAG, e.toString());
                e.printStackTrace();
                    msg = null;
                }

                if (msg != null) {
                SlideshowModel slideshow;
                try {
                    slideshow = SlideshowModel
                            .createFromPduBody(this, msg.getBody());
                } catch (MmsException e) {
                    MmsLog.d(TAG, e.toString());
                    e.printStackTrace();
                    slideshow = null;
                }
                    if (slideshow == null) {
                        MmsLog.d(TAG, "getSlideShow(); slideshow null");
                    } else {
                        MmsLog.d(TAG, "getSlideShow(); slideshow ok");
                    }
                    return slideshow;
                }
                MmsLog.d(TAG, "msg null");
        
            return null;
        } else {
            MmsLog.d(TAG, "moveToFirst fail");
            return null;
        }
    }

    @Override
    public void startAudio() {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.startAudio");
    }

    @Override
    public void startVideo() {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.startVideo");
    }

    @Override
    public void setAudio(Uri audio, String name, Map<String, ?> extras) {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.setAudio");
    }

    @Override
    public void setTextVisibility(boolean visible) {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.setTextVisibility");
    }

    @Override
    public void setText(String name, String text) {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.setText");
    }

    @Override
    public void setImage(String name, Bitmap bitmap) {
        MmsLog.d(TAG, "DialogModeActivity.setImage " + name);
        // inflateMmsView();

        try {
            Bitmap image = bitmap;
            if (null == image) {
                MmsLog.d(TAG, "bitmap null");
                image = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_missing_thumbnail_picture);
            }
            MmsLog.d(TAG, "set bitmap to mMmsImageView");
            mMmsImageView.setImageBitmap(image);
            mMmsImageView.setVisibility(View.VISIBLE);
            mMmsView.setVisibility(View.VISIBLE);
        } catch (java.lang.OutOfMemoryError e) {
            MmsLog.d(TAG, "setImage: out of memory:" + e.toString());
        }
    }

    @Override
    public void setImage(Uri mUri) {
        MmsLog.d(TAG, "DialogModeActivity.setImage(uri) ");
        try {
            Bitmap bitmap = null;
            if (null == mUri) {
                bitmap = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_missing_thumbnail_picture);
            } else {
                //String mScheme = mUri.getScheme();
                InputStream mInputStream = null;
                try {
                    mInputStream = getApplicationContext().getContentResolver().openInputStream(
                        mUri);
                    if (mInputStream != null) {
                        bitmap = BitmapFactory.decodeStream(mInputStream);
                    }
                } catch (FileNotFoundException e) {
                    bitmap = null;
                } finally {
                    if (mInputStream != null) {
                        mInputStream.close();
                    }
                }
            }
                setImage("", bitmap);
        } catch (java.lang.OutOfMemoryError e) {
            MmsLog.d(TAG, "setImage(Uri): out of memory: ", e);
        } catch (Exception e) {
            MmsLog.d(TAG, "setImage(uri) error." + e);
        }
    }

    @Override
    public void reset() {
        MmsLog.d(TAG, "DialogModeActivity.reset");

        if (mMmsView != null) {
            mMmsView.setVisibility(View.GONE);
        }
    }

    @Override
    public void setVisibility(boolean visible) {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.setVisibility");
        mMmsView.setVisibility(View.VISIBLE);
    }

    @Override
    public void pauseAudio() {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.pauseAudio");

    }

    @Override
    public void pauseVideo() {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.pauseVideo");

    }

    @Override
    public void seekAudio(int seekTo) {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.seekAudio");

    }

    @Override
    public void seekVideo(int seekTo) {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.seekVideo");

    }

    @Override
    public void setVideoVisibility(boolean visible) {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.setVideoVisibility");
    }

    @Override
    public void stopAudio() {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.stopAudio");
    }

    @Override
    public void stopVideo() {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.stopVideo");
    }

    @Override
    public void setVideo(String name, Uri video) {
        MmsLog.d(TAG, "DialogModeActivity.setVideo");
        // inflateMmsView();

        try {
            Bitmap bitmap = VideoAttachmentView.createVideoThumbnail(this, video);
            if (null == bitmap) {
                MmsLog.d(TAG, "bitmap null");
                bitmap = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_missing_thumbnail_video);
            }
            MmsLog.d(TAG, "set bitmap to mMmsImageView");
            mMmsImageView.setImageBitmap(bitmap);
            mMmsImageView.setVisibility(View.VISIBLE);
            mMmsView.setVisibility(View.VISIBLE);
        } catch (java.lang.OutOfMemoryError e) {
            MmsLog.d(TAG, "setImage: out of memory:" + e.toString());
        }
    }

    @Override
    public void setVideoThumbnail(String name, Bitmap bitmap) {
        MmsLog.d(TAG, "setVideoThumbnail");
    }

    @Override
    public void setImageRegionFit(String fit) {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.setImageRegionFit");
    }

    @Override
    public void setImageVisibility(boolean visible) {
        // TODO Auto-generated method stub
        MmsLog.d(TAG, "DialogModeActivity.setImageVisibility");
        mMmsView.setVisibility(View.VISIBLE);
    }

    @Override
    public int getWidth() {
        MmsLog.d(TAG, "DialogModeActivity.getWidth" + mMmsImageView.getWidth());
        return mMmsImageView.getWidth();
    }

    @Override
    public int getHeight() {
        MmsLog.d(TAG, "DialogModeActivity.getHeight" + mMmsImageView.getHeight());
        return mMmsImageView.getHeight();
    }

    private void updateSendButtonState() {
        boolean enable = false;
        int len = mReplyEditor.getText().toString().length();

        MmsLog.d(TAG, "DialogModeActivity.updateSendButtonState(): len = " + len);

        if (mSendButton != null) {
            if (len > 0) {
                MmsLog.d(TAG, "updateSendButtonState(): mSimCount = " + mSimCount);

                /** M: MTK Encapsulation ITelephony */
                // ITelephony phone = ITelephony.Stub
                //        .asInterface(ServiceManager.checkService("phone"));
                EncapsulatedTelephonyService phone = EncapsulatedTelephonyService.getInstance();
                if (phone != null) {
                    if (isAnySimInsert()) { // check SIM state
                        enable = true;
                    }
                }
            }

            // View sendButton = showSmsOrMmsSendButton(mWorkingMessage.requiresMms());
            mSendButton.setEnabled(enable);
            mSendButton.setFocusable(enable);
            /// M: add for ipmessage
            if (enable) {
                if (isCurIpMessage()) {
                    mSendButton.setImageResource(R.drawable.ic_send_ipmsg);
                } else {
                    mSendButton.setImageResource(R.drawable.ic_send_sms);
                }
            } else {
                mSendButton.setImageResource(R.drawable.ic_send_holo_light);
            }
        }
    }

    private boolean isAnySimInsert() {
        MmsLog.d(TAG, "DialogModeActivity.isAnySimInsert,mSimCount=" + mSimCount);
        if (mSimCount > 0) {
            return true;
        }
        return false;
    }

    private void getSimInfoList() {
        MmsLog.d(TAG, "DialogModeActivity.getSimInfoList");

        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            MmsLog.d(TAG, "GEMINI");
            mSimInfoList = SIMInfo.getInsertedSIMList(this);
            mSimCount = mSimInfoList.isEmpty() ? 0 : mSimInfoList.size();
            MmsLog.d(TAG, "ComposeMessageActivity.getSimInfoList(): mSimCount = " + mSimCount);
        } else { // single SIM
            MmsLog.d(TAG, "non GEMINI");
            /** M: MTK Encapsulation ITelephony */
            // ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            EncapsulatedTelephonyService phone = EncapsulatedTelephonyService.getInstance();
            if (phone != null) {
                try {
                    mSimCount = phone.isSimInsert(0) ? 1 : 0;
                    MmsLog.d(TAG, "sim count=" + mSimCount);
                } catch (RemoteException e) {
                    MmsLog.e(TAG, "check sim insert status failed");
                    mSimCount = 0;
                }
            }
        }
    }

    private void updateCounter(CharSequence text, int start, int before, int count) {
        MmsLog.d(TAG, "DialogModeActivity.updateCounter");

        int[] params = null;
        params = SmsMessage.calculateLength(text, false);
        /*
         * SmsMessage.calculateLength returns an int[4] with: int[0] being the number of SMS's required, int[1] the
         * number of code units used, int[2] is the number of code units remaining until the next message. int[3] is the
         * encoding type that should be used for the message.
         */
        int msgCount = params[0];
        int remainingInCurrentMessage = params[2];
        //int unitesUsed = params[1];

        // mWorkingMessage.setLengthRequiresMms(
        // msgCount >= MmsConfig.getSmsToMmsTextThreshold(), true);
        // Show the counter
        // Update the remaining characters and number of messages required.
        // if (mWorkingMessage.requiresMms()) {
        // mTextCounter.setVisibility(View.GONE);
        // } else {
        // mTextCounter.setVisibility(View.VISIBLE);
        // }
        String counterText = remainingInCurrentMessage + "/" + msgCount;
        MmsLog.d(TAG, "counterText=" + counterText);
        mTextCounter.setText(counterText);
        // m1
    }

    class TextLengthFilter implements InputFilter {
        private final Toast mExceedMessageSizeToast;
        private final int mMaxLength;

        public TextLengthFilter(int max) {
            mMaxLength = max;
            mExceedMessageSizeToast = Toast.makeText(DialogModeActivity.this,
                R.string.exceed_message_size_limitation, Toast.LENGTH_SHORT);
        }

        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                int dstart, int dend) {
            /// M: re-compute max sms number count
            String text = "";
            String destString = dest.toString();
            String headString = destString.substring(0, dstart);
            if (headString != null) {
                text += headString;
            }
            String middleString = source.toString().substring(start, end);
            if (middleString != null) {
                text += middleString;
            }
            String tailString = destString.substring(dend);
            if (tailString != null) {
                text += tailString;
            }
            int page = mPage - 1;
            int maxLength = mMaxLength;
            ArrayList<String> list = EncapsulatedSmsManager.divideMessage(text);
            if (list.size() > page) {
                maxLength = 0;
                for (int i = 0; i < page; i++) {
                    maxLength += list.get(i).length();
                }
                MmsLog.d(TAG, "get maxLength:" + maxLength);
            }

            int keep = maxLength - (dest.length() - (dend - dstart));

            if (keep < (end - start)) {
                mExceedMessageSizeToast.show();
            }

            if (keep <= 0) {
                return "";
            } else if (keep >= end - start) {
                return null; // keep original
            } else {
                return source.subSequence(start, start + keep);
            }
        }
    }

    private void markAsRead(final Uri uri) {
        MmsLog.d(TAG, "DialogModeActivity.markAsRead, " + uri.toString());

        new Thread(new Runnable() {
            public void run() {
                final ContentValues values = new ContentValues(2);
                values.put("read", 1);
                values.put("seen", 1);
                SqliteWrapper.update(getApplicationContext(), getContentResolver(), uri, values,
                    null, null);
                MessagingNotification.blockingUpdateNewMessageIndicator(DialogModeActivity.this,
                        MessagingNotification.THREAD_NONE, false);
            }
        }).start();
        removeCurMsg();
    }

    private void markAsRead(final ArrayList<Uri> uris) {
        final Object[] uriArray = uris.toArray();
        new Thread(new Runnable() {
            public void run() {
                final ContentValues values = new ContentValues(2);
                values.put("read", 1);
                values.put("seen", 1);
                for (Object uriObject : uriArray) {
                    Uri uri = (Uri)uriObject;
                    MmsLog.d(TAG, "markasread a:" + uri.toString());
                    SqliteWrapper.update(getApplicationContext(), getContentResolver(), uri, values,
                            null, null);
                }
                MessagingNotification.blockingUpdateNewMessageIndicator(DialogModeActivity.this,
                        MessagingNotification.THREAD_NONE, false);
            }
        }).start();
    }

    private void markAsSeen(final ArrayList<Uri> uris) {
        final Object[] uriArray = uris.toArray();
        new Thread(new Runnable() {
            public void run() {
                final ContentValues values = new ContentValues(2);
                values.put("seen", 1);
                for (Object uriObject : uriArray) {
                    Uri uri = (Uri)uriObject;
                    MmsLog.d(TAG, "markasseen a:" + uri.toString());
                    SqliteWrapper.update(getApplicationContext(), getContentResolver(), uri, values,
                            null, null);
                }
                MessagingNotification.blockingUpdateNewMessageIndicator(DialogModeActivity.this,
                        MessagingNotification.THREAD_NONE, false);
            }
        }).start();
    }

    public class DialogModeReceiver extends BroadcastReceiver {

        private static final String MSG_VIEWED_ACTION = "com.android.mms.dialogmode.VIEWED";

        public void onReceive(Context context, Intent intent) {
            MmsLog.d(TAG, "DialogModeActivity.DialogModeReceiver.onReceive");

            // TODO Auto-generated method stub
            if (intent != null) {
                String action = intent.getAction();
                if (action == null) {
                    return;
                }
                MmsLog.d(TAG, "action=" + action);
                DialogModeActivity.this.finish();
            }
        }
    }

    /// M: add for ipmessage
    private boolean isCurIpMessage() {
        boolean result = false;
        mCurUri = (Uri)mUris.get(mCurUriIdx);
        if (mCurUri != null) {
            result = mIpMessageUris.contains(mCurUri);
            MmsLog.d(TAG, "check uri:" + mCurUri.toString());
        }
        MmsLog.d(TAG, "result:" + result);
        return result;
    }

    private long getCurIpMessageId() {
        long id = 0;
        mCurUri = (Uri)mUris.get(mCurUriIdx);
        if (mCurUri != null) {
            id = Long.parseLong(mCurUri.getLastPathSegment());
        } else {
            MmsLog.w(TAG, "mCurUri is null!");
        }
        MmsLog.d(TAG, "id:" + id);
        return id;
    }

    private void showIpMessage() {
        if (mMmsView != null) {
            MmsLog.d(TAG, "Hide MMS views");
            mMmsView.setVisibility(View.GONE);
        }
        if (isCurGroupIpMessage() && mCursor != null) {
            String name = mCursor.getString(SMS_ADDR);
            MmsLog.d(TAG, "group sender address:" + name);
            name = IpMessageUtils.getContactManager(this).getNameByNumber(name) + ":";
            MmsLog.d(TAG, "group sender name:" + name);
            mGroupSender.setText(name);
            mGroupSender.setVisibility(View.VISIBLE);
        } else {
            mGroupSender.setVisibility(View.GONE);
        }
        long id = getCurIpMessageId();
        IpMessage ipMessage = IpMessageUtils.getMessageManager(this).getIpMsgInfo(id);
        MmsLog.d(TAG, "showIpMessage. id:" + id + ",type:" + ipMessage.getType());
        switch (ipMessage.getType()) {
        case IpMessageType.TEXT:
            setIpTextItem((IpTextMessage) ipMessage);
            break;
        case IpMessageType.PICTURE:
            setIpImageItem((IpImageMessage) ipMessage);
            break;
        case IpMessageType.VOICE:
            setIpVoiceItem((IpVoiceMessage) ipMessage);
            break;
        case IpMessageType.VCARD:
            setIpVCardItem((IpVCardMessage) ipMessage);
            break;
        case IpMessageType.LOCATION:
            setIpLocationItem((IpLocationMessage) ipMessage);
            break;
        case IpMessageType.SKETCH:
            setIpImageItem((IpImageMessage) ipMessage);
            break;
        case IpMessageType.VIDEO:
            setIpVideoItem((IpVideoMessage) ipMessage);
            break;
        case IpMessageType.CALENDAR:
            setIpVCalendarItem((IpVCalendarMessage)ipMessage);
            break;
        case IpMessageType.UNKNOWN_FILE:
        case IpMessageType.COUNT:
            MmsLog.w(TAG, "Unknown IP message type. type = " + ipMessage.getType());
            break;
        case IpMessageType.GROUP_CREATE_CFG:
        case IpMessageType.GROUP_ADD_CFG:
        case IpMessageType.GROUP_QUIT_CFG:
            /// M: group chat type
            MmsLog.w(TAG, "Group IP message type. type = " + ipMessage.getType());
            break;
        default:
            MmsLog.w(TAG, "Error IP message type. type = " + ipMessage.getType());
            break;
        }
    }

    private void setIpTextItem(IpTextMessage textMessage) {
        MmsLog.d(TAG, "setIpTextItem()");
        if (TextUtils.isEmpty(textMessage.getBody())) {
            MmsLog.w(TAG, "setIpTextItem(): No message content!");
            return;
        }

        mIpImageView.setVisibility(View.GONE);
        mCaptionSeparator.setVisibility(View.GONE);
        mCaption.setVisibility(View.GONE);
        /// M: add for ipmessage, hide audio or vcard view
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mIpVCalendarView.setVisibility(View.GONE);
        /// M: add for ipmessage, hide location view
        mIpLocationView.setVisibility(View.GONE);

        int resId = mParser.getDynamicRes(textMessage.getBody());
        if (resId <= 0) {
            resId = mParser.getAdRes(textMessage.getBody());
        }
        if (resId <= 0) {
            resId = mParser.getXmRes(textMessage.getBody());
        }
        if (resId > 0) {
            MmsLog.d(TAG, "set dynamic pic res.");
            mBodyTextView.setVisibility(View.GONE);
            mIpImageView.setVisibility(View.GONE);
            mGifView.setSource(resId);
            mIpEmoticonView.setVisibility(View.VISIBLE);
        } else {
            resId = mParser.getLargeRes(textMessage.getBody());
            if (resId > 0) {
                MmsLog.d(TAG, "set static pic res.");
                mBodyTextView.setVisibility(View.GONE);
                mIpEmoticonView.setVisibility(View.GONE);
                /// M: add resource from external apk
                Drawable image = IpMessageUtils.getResourceManager(this).getSingleDrawable(resId);
                mImageContent.setImageDrawable(image);
                mIpImageSizeBg.setVisibility(View.GONE);
                mIpImageView.setVisibility(View.VISIBLE);
            } else {
                MmsLog.d(TAG, "set text res.");
                CharSequence formattedMessage = formatMessage(textMessage.getBody());
                /// M: add for ipmessage, hide emoticon view
                mIpImageView.setVisibility(View.GONE);
                mIpEmoticonView.setVisibility(View.GONE);
                mBodyTextView.setText(formattedMessage);
                mBodyTextView.setVisibility(View.VISIBLE);
            }
        }
    }

    private CharSequence formatMessage(String body) {
        SpannableStringBuilder buf = new SpannableStringBuilder();
//        SmileyParser parser = SmileyParser.getInstance();
        buf.append(mParser.addSmileySpans(body));
        return buf;
    }

    private void updateIpMessageImageOrVideoView(IpAttachMessage message, long msgId) {
            mActionButton.setVisibility(View.VISIBLE);
            mActionButton.setImageResource(R.drawable.ipmsg_chat_download_selector);
            if (IpMessageUtils.getMessageManager(this).isDownloading(msgId)) {
                mActionButton.setImageResource(R.drawable.ipmsg_chat_stop_selector);
                if (null != mImageDownloadProgressBar) {
                    mImageDownloadProgressBar.setVisibility(View.VISIBLE);
                }
                int progress = IpMessageUtils.getMessageManager(this).getDownloadProcess(msgId);
                if (null != mImageDownloadProgressBar) {
                    mImageDownloadProgressBar.setProgress(progress);
                }
                mContentSize.setVisibility(View.GONE);
            } else {
                if (null != mImageDownloadProgressBar) {
                    mImageDownloadProgressBar.setVisibility(View.GONE);
                    mActionButton.setVisibility(View.GONE);
                }
                mContentSize.setVisibility(View.VISIBLE);
            mContentSize.setText(IpMessageUtils.formatFileSize(message.getSize()));
        }
    }

    private void setIpImageItem(IpImageMessage imageMessage) {
        MmsLog.d(TAG, "setIpImageItem()");
        mIpImageView.setVisibility(View.VISIBLE);
        mMediaPlayView.setVisibility(View.INVISIBLE);
        final long msgId = getCurIpMessageId();
        if (imageMessage.isInboxMsgDownloalable()) {
            updateIpMessageImageOrVideoView(imageMessage, msgId);
            final IpImageMessage imageMsg = imageMessage;
            mActionButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!imageMsg.isInboxMsgDownloalable()) {
                        return;
                    }
                    if (IpMessageUtils.getMessageManager(DialogModeActivity.this).isDownloading(msgId)) {
                        IpMessageUtils.getMessageManager(DialogModeActivity.this).cancelDownloading(msgId);
                    } else {
                        IpMessageUtils.getMessageManager(DialogModeActivity.this).downloadAttach(msgId);
                    }
                }
            });
            if (!setPicView(imageMessage.getThumbPath())) {
                setPicView(imageMessage.getPath());
            }
        } else {
            if (!setPicView(imageMessage.getThumbPath())) {
                setPicView(imageMessage.getPath());
            }
            mIpImageSizeBg.setBackgroundDrawable(null);
            mActionButton.setVisibility(View.GONE);
            if (null != mImageDownloadProgressBar) {
                mImageDownloadProgressBar.setVisibility(View.GONE);
            }
            mActionButton.setClickable(false);
            mContentSize.setVisibility(View.GONE);
        }

        if (TextUtils.isEmpty(imageMessage.getCaption())) {
            mCaptionSeparator.setVisibility(View.GONE);
            mCaption.setVisibility(View.GONE);
        } else {
            mCaptionSeparator.setVisibility(View.VISIBLE);
            mCaption.setVisibility(View.VISIBLE);
            CharSequence caption = "";
            caption = mParser.addSmileySpans(imageMessage.getCaption());
            mCaption.setText(caption);
        }

        /// M: add for ipmessage, hide text view
        mBodyTextView.setVisibility(View.GONE);
        mIpEmoticonView.setVisibility(View.GONE);
        /// M: add for ipmessage, hide audio or vcard view
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mIpVCalendarView.setVisibility(View.GONE);
        /// M: add for ipmessage, hide location view
        mIpLocationView.setVisibility(View.GONE);
    }

    private void setIpVoiceItem(IpVoiceMessage voiceMessage) {
        long msgId = getCurIpMessageId();
        MmsLog.d(TAG, "setIpVoiceItem(): message Id = " + msgId);
        //mAudioIcon.setImageResource(R.drawable.ic_soundrecorder);
        if (voiceMessage.isInboxMsgDownloalable()) {
            if (IpMessageUtils.getMessageManager(this).isDownloading(msgId)) {
                if (null != mAudioDownloadProgressBar) {
                    mAudioDownloadProgressBar.setVisibility(View.VISIBLE);
                }
                int progress = IpMessageUtils.getMessageManager(this).getDownloadProcess(msgId);
                if (null != mAudioDownloadProgressBar) {
                    mAudioDownloadProgressBar.setProgress(progress);
                }
                mAudioInfo.setVisibility(View.GONE);
            } else {
                if (null != mAudioDownloadProgressBar) {
                    mAudioDownloadProgressBar.setVisibility(View.GONE);
                }
                mAudioInfo.setVisibility(View.VISIBLE);
                mAudioInfo.setText(IpMessageUtils.formatFileSize(voiceMessage.getSize()));
            }
        } else {
            if (null != mAudioDownloadProgressBar) {
                mAudioDownloadProgressBar.setVisibility(View.GONE);
            }
            mAudioInfo.setVisibility(View.VISIBLE);
            mAudioInfo.setText(IpMessageUtils.formatAudioTime(voiceMessage.getDuration()));
        }

        if (TextUtils.isEmpty(voiceMessage.getCaption())) {
            mCaptionSeparator.setVisibility(View.GONE);
            mCaption.setVisibility(View.GONE);
        } else {
            mCaptionSeparator.setVisibility(View.VISIBLE);
            mCaption.setVisibility(View.VISIBLE);
            CharSequence caption = "";
            caption = mParser.addSmileySpans(voiceMessage.getCaption());
            mCaption.setText(caption);
        }

        /// M: add for ipmessage, show audio view
        mIpAudioView.setVisibility(View.VISIBLE);
        mIpVCardView.setVisibility(View.GONE);
        mIpVCalendarView.setVisibility(View.GONE);
        /// M: add for ipmessage, hide text view
        mBodyTextView.setVisibility(View.GONE);
        mIpEmoticonView.setVisibility(View.GONE);
        /// M: add for ipmessage, hide image view or video view
        mIpImageView.setVisibility(View.GONE);
        /// M: add for ipmessage, hide location view
        mIpLocationView.setVisibility(View.GONE);
    }

    private void setIpVCardItem(IpVCardMessage vCardMessage) {
        long msgId = getCurIpMessageId();
        MmsLog.d(TAG, "setIpVCardItem(): message Id = " + msgId);
        String name = vCardMessage.getName();
        if (name != null && name.lastIndexOf(".") != -1) {
            name = name.substring(0, name.lastIndexOf("."));
        }
        mVCardInfo.setText(name);
        mIpVCardView.setVisibility(View.VISIBLE);
        mIpAudioView.setVisibility(View.GONE);
        /// M: add for ipmessage, hide text view
        mBodyTextView.setVisibility(View.GONE);
        mIpEmoticonView.setVisibility(View.GONE);
        /// M: add for ipmessage, hide image view or video view
        mIpImageView.setVisibility(View.GONE);
        mCaptionSeparator.setVisibility(View.GONE);
        mCaption.setVisibility(View.GONE);
        mIpVCalendarView.setVisibility(View.GONE);
        /// M: add for ipmessage, hide location view
        mIpLocationView.setVisibility(View.GONE);
    }

    private void setIpVCalendarItem(IpVCalendarMessage vCalendarMessage) {
        long msgId = getCurIpMessageId();
        MmsLog.d(TAG, "setIpVCalendarItem(): message Id = " + msgId);
        String summary = vCalendarMessage.getSummary();
        if (summary != null && summary.lastIndexOf(".") != -1) {
            summary = summary.substring(0, summary.lastIndexOf("."));
        }
        mVCalendarInfo.setText(summary);
        mIpVCalendarView.setVisibility(View.VISIBLE);
        mBodyTextView.setVisibility(View.GONE);
        mIpEmoticonView.setVisibility(View.GONE);
        mIpImageView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mCaptionSeparator.setVisibility(View.GONE);
        mCaption.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mIpLocationView.setVisibility(View.GONE);
    }

    private void setIpLocationItem(IpLocationMessage locationMessage) {
        long msgId = getCurIpMessageId();
        MmsLog.d(TAG, "setIpLocationItem(): message Id = " + msgId);
        mLocationAddr.setText(locationMessage.getAddress());
        String path = locationMessage.getPath();
        if (IpMessageUtils.isExistsFile(path)) {
            Bitmap bm = BitmapFactory.decodeFile(path);
            mImageLocation.setImageBitmap(bm);
        } else {
            mImageLocation.setImageResource(R.drawable.default_map_small);
        }
        mIpLocationView.setVisibility(View.VISIBLE);

        /// M: add for ipmessage, hide text view
        mBodyTextView.setVisibility(View.GONE);
        mIpEmoticonView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        /// M: add for ipmessage, hide image view or video view
        mIpImageView.setVisibility(View.GONE);
        mCaptionSeparator.setVisibility(View.GONE);
        mCaption.setVisibility(View.GONE);
        /// M: add for ipmessage, hide audio or vcard view
        mIpVCardView.setVisibility(View.GONE);
        mIpVCalendarView.setVisibility(View.GONE);
    }

    private void setIpVideoItem(IpVideoMessage videoMessage) {
        final long msgId = getCurIpMessageId();
        MmsLog.d(TAG, "setIpVideoItem(): message Id = " + msgId);
        mIpImageView.setVisibility(View.VISIBLE);
        mMediaPlayView.setVisibility(View.VISIBLE);
        if (videoMessage.isInboxMsgDownloalable()) {
            updateIpMessageImageOrVideoView(videoMessage, msgId);
            final IpVideoMessage videoMsg = videoMessage;
            mActionButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!videoMsg.isInboxMsgDownloalable()) {
                        return;
                    }
                    if (IpMessageUtils.getMessageManager(DialogModeActivity.this).isDownloading(msgId)) {
                        IpMessageUtils.getMessageManager(DialogModeActivity.this).cancelDownloading(msgId);
                    } else {
                        IpMessageUtils.getMessageManager(DialogModeActivity.this).downloadAttach(msgId);
                    }
                }
            });
            if (!setPicView(videoMessage.getThumbPath())) {
                setVideoView(videoMessage.getPath());
            }
        } else {
            if (!setPicView(videoMessage.getThumbPath())) {
                setVideoView(videoMessage.getPath());
            }
            mIpImageSizeBg.setBackgroundDrawable(null);
            mActionButton.setVisibility(View.GONE);
            if (null != mImageDownloadProgressBar) {
                mImageDownloadProgressBar.setVisibility(View.GONE);
            }
            mActionButton.setClickable(false);
            mContentSize.setVisibility(View.GONE);
        }

        if (TextUtils.isEmpty(videoMessage.getCaption())) {
            mCaptionSeparator.setVisibility(View.GONE);
            mCaption.setVisibility(View.GONE);
        } else {
            mCaptionSeparator.setVisibility(View.VISIBLE);
            mCaption.setVisibility(View.VISIBLE);
            CharSequence caption = "";
            caption = mParser.addSmileySpans(videoMessage.getCaption());
            mCaption.setText(caption);
        }

        /// M: add for ipmessage, hide text view
        mBodyTextView.setVisibility(View.GONE);
        mIpEmoticonView.setVisibility(View.GONE);
        /// M: add for ipmessage, hide audio or vcard view
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mIpVCalendarView.setVisibility(View.GONE);
        /// M: add for ipmessage, hide location view
        mIpLocationView.setVisibility(View.GONE);
    }

    private void setIpEmoticonItem(IpTextMessage textMessage) {
        MmsLog.d(TAG, "setIpEmoticonItem");
        setIpTextItem(textMessage);
    }

    private boolean setPicView(String filePath) {
        MmsLog.d(TAG, "setPicView(): filePath = " + filePath + ", imageView = " + mImageContent);
        if (TextUtils.isEmpty(filePath) || null == mImageContent) {
            return false;
        }
        mIpImageSizeBg.setVisibility(View.GONE);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        options.inJustDecodeBounds = false;

        int l = Math.max(options.outHeight, options.outWidth);
        int be = (int) (l / 500);
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;
        bitmap = BitmapFactory.decodeFile(filePath, options);

        int mWidth = getResources().getDimensionPixelOffset(R.dimen.img_minwidth);
        MmsLog.d(TAG, "setPicView(): before set layout IpImageSizeBg.width = " + mIpImageSizeBg.getWidth());
        if (bitmap != null) {
            bitmap = IpMessageUtils.resizeImage(bitmap, mWidth, bitmap.getHeight() * mWidth / bitmap.getWidth(), true);
            mImageContent.setImageBitmap(bitmap);
            mIpImageSizeBg.setVisibility(View.VISIBLE);
            return true;
        } else {
            mImageContent.setImageResource(R.drawable.ic_missing_thumbnail_picture);
            mIpImageSizeBg.setVisibility(View.GONE);
            return false;
        }
    }

    private boolean setVideoView(String filePath) {
        MmsLog.d(TAG, "setVideoView(): filePath = " + filePath + ", imageView = " + mImageContent);
        if (TextUtils.isEmpty(filePath) || null == mImageContent) {
            return false;
        }
        mMediaPlayView.setVisibility(View.VISIBLE);
        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(filePath, Thumbnails.MICRO_KIND);
        if (null != bitmap) {
            mImageContent.setImageBitmap(bitmap);
            mIpImageSizeBg.setVisibility(View.VISIBLE);
            return true;
        } else {
            mImageContent.setImageResource(R.drawable.ic_missing_thumbnail_picture);
            mIpImageSizeBg.setVisibility(View.GONE);
            return false;
        }
    }

    private void sendIpTextMessage(String body, int simId, String to) {
        final IpTextMessage ipMessage = new IpTextMessage();
        ipMessage.setBody(body);
        ipMessage.setType(IpMessageType.TEXT);
        ipMessage.setSimId(simId);
        ipMessage.setTo(to);
        int sendModeType = IpMessageSendMode.AUTO;
        final int sendMode = sendModeType;
        onPreMessageSent();
        new Thread(new Runnable() {
            @Override
            public void run() {
                MmsLog.d(TAG, "sendIpTextMessage(): calling API: saveIpMsg().");
                int ret = -1;
                ret = IpMessageUtils.getMessageManager(DialogModeActivity.this).saveIpMsg(ipMessage, sendMode);
                if (ret == -1) {
                    MmsLog.w(TAG, "sendIpTextMessage failed! ");
                } else {
                    // success.
                    onMessageSent();
                }
            }
        }).start();
    }

    private boolean isCurGroupIpMessage() {
        boolean result = false;
        if (isCurIpMessage()) {
            Conversation conv = getConversation();
            if (conv != null) {
                String number = conv.getRecipients().get(0).getNumber();
                MmsLog.d(TAG, "number:" + number);
                if (number != null && number.startsWith(IpMessageConsts.GROUP_START)) {
                    // this is group message
                    result = true;
                }
            }
        }
        MmsLog.d(TAG, "is group message:" + result);
        return result;
    }

    private String getCurGroupIpMessageNumber() {
        String number = "";
        if (isCurIpMessage()) {
            Conversation conv = getConversation();
            if (conv != null) {
                String num = conv.getRecipients().get(0).getNumber();
                MmsLog.d(TAG, "number:" + num);
                if (num != null && num.startsWith(IpMessageConsts.GROUP_START)) {
                    // this is group message
                    number = num;
                }
            }
        }
        MmsLog.d(TAG, "group message number:" + number);
        return number;
    }

    private String getCurGroupIpMessageName() {
        String name = "";
        if (isCurIpMessage()) {
            Conversation conv = getConversation();
            if (conv != null) {
                String nam = conv.getRecipients().get(0).getName();
                MmsLog.d(TAG, "name:" + nam);
                if (nam != null) {
                    // this is group message name
                    name = IpMessageUtils.getContactManager(this).getNameByThreadId(getThreadId());
                }
            }
        }
        MmsLog.d(TAG, "group message name:" + name);
        return name;
    }

    @Override
    public void notificationsReceived(Intent intent) {
        MmsLog.d(TAG, "DialogModeActivity, notificationReceived: intent = " + intent);
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return;
        }
        if (isFinishing()) {
            MmsLog.d(TAG, "activity is being destroied, ignore notification.");
            return;
        }
        MmsLog.d(TAG, "DialogModeActivity, action:" + action);
        if (!isCurIpMessage()) {
            MmsLog.d(TAG, "showing one is not ipmessage, ignore action.");
            return;
        }
        long msgId = 0L;
        switch (IpMessageUtils.getActionTypeByAction(action)) {
        case IpMessageUtils.IPMSG_DOWNLOAD_ATTACH_STATUS_ACTION:
        case IpMessageUtils.IPMSG_IP_MESSAGE_STATUS_ACTION:
            try {
                if (IpMessageUtils.getActionTypeByAction(action) == IpMessageUtils.IPMSG_DOWNLOAD_ATTACH_STATUS_ACTION) {
                    msgId = intent.getLongExtra(DownloadAttachStatus.DOWNLOAD_MSG_ID, 0);
                } else {
                    msgId = intent.getLongExtra(IpMessageStatus.IP_MESSAGE_ID, 0);
                }
                if (getCurIpMessageId() != msgId) {
                    MmsLog.d(TAG, "current ipmessage is not this:" + msgId + ",current:" + getCurIpMessageId());
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showIpMessage();//just refresh all element.
                    }
                });
            } catch (NullPointerException e) {
                // TODO: handle exception
                MmsLog.d(TAG, "catch a NullPointerExcpetion?");
            }
            break;
        default :
            MmsLog.d(TAG, "DialogModeActivity. ignore notification.");
            return;
        }
    }

    private void openIpMsgThread(final long threadId, boolean isImportant) {
        Intent intent = new Intent(RemoteActivities.CHAT_DETAILS_BY_THREAD_ID);
        intent.putExtra(RemoteActivities.KEY_THREAD_ID, threadId);
        intent.putExtra(RemoteActivities.KEY_BOOLEAN, isImportant);
        IpMessageUtils.startRemoteActivity(this, intent);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mNeedFinish) {
            mNeedFinish = false;
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        /// M: take press back just as press close button
        onClick(mMarkAsReadBtn);
    }

    /// M: fix for bug ALPS00434945, add for vcard and vcalendar.{
    private void setVcardOrVcalendar(SlideshowModel slideshow) {
        if (slideshow == null || slideshow.getAttachFiles() == null
                || slideshow.getAttachFiles().size() == 0) {
            return;
        }
        FileAttachmentModel attach = slideshow.getAttachFiles().get(0);
        String contentType = attach.getContentType();
        String src = attach.getSrc();
        long size = attach.getAttachSize();
        if (contentType.equalsIgnoreCase(EncapsulatedContentType.TEXT_VCARD)
                || contentType.equalsIgnoreCase(EncapsulatedContentType.TEXT_VCALENDAR)) {
            mMmsAttachView.setVisibility(View.VISIBLE);
            MmsLog.d(TAG, "set vcard or vcarlendar to mMmsImageView");
            String nameText = "";
            if (contentType.equalsIgnoreCase(EncapsulatedContentType.TEXT_VCARD)) {
                mMmsAttachImageView.setImageResource(R.drawable.ic_vcard_attach);
                nameText = getResources().getString(R.string.file_attachment_vcard_name, src);
            } else {
                mMmsAttachImageView.setImageResource(R.drawable.ic_vcalendar_attach);
                nameText = getResources().getString(R.string.file_attachment_vcalendar_name, src);
            }
            mAttachName.setText(nameText);
            mAttachSize.setText(MessageUtils.getHumanReadableSize(size));
            setSmsContentTextView();
        }
    }

    private void setSmsContentTextView() {
        if (mSmsContentText != null) {
            CharSequence contentString = mSmsContentText.getText();
            if (contentString == null || contentString.toString().trim().length() == 0) {
                mSmsContentText.setVisibility(View.GONE);
            }
        }
    }
    /// @}

    /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms
    private String interpretFrom(EncodedStringValue from, Uri messageUri) {
        String address;
        if (from != null) {
            address = from.getString();
        } else {
            address = AddressUtils.getFrom(DialogModeActivity.this, messageUri);
        }
        String contact = TextUtils.isEmpty(address) ?
                            DialogModeActivity.this.getString(android.R.string.unknownName)
                            : Contact.get(address, false).getName();
        return contact;
    }

}
