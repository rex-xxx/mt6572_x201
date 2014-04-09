/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

import static android.content.res.Configuration.KEYBOARDHIDDEN_NO;
import static com.android.mms.transaction.ProgressCallbackEntity.PROGRESS_ABORT;
import static com.android.mms.transaction.ProgressCallbackEntity.PROGRESS_COMPLETE;
import static com.android.mms.transaction.ProgressCallbackEntity.PROGRESS_START;
import static com.android.mms.transaction.ProgressCallbackEntity.PROGRESS_STATUS_ACTION;
import static com.android.mms.ui.MessageListAdapter.COLUMN_ID;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MSG_TYPE;
import static com.android.mms.ui.MessageListAdapter.PROJECTION;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.provider.Settings;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.content.ClipboardManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.TempFileProvider;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.data.Conversation.ConversationQueryHandler;
import com.android.mms.data.WorkingMessage;
import com.android.mms.data.WorkingMessage.MessageStatusListener;
import com.android.mms.drm.DrmUtils;
import com.mediatek.encapsulation.com.google.android.mms.EncapsulatedContentType;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduPart;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.SendReq;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.model.VCalendarModel;
import com.android.mms.model.VCardModel;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.ui.MessageUtils.ResizeImageResultCallback;
import com.android.mms.ui.RecipientsEditor.RecipientContextMenuInfo;
import com.android.mms.util.AddressUtils;
import com.android.mms.util.DraftCache.OnDraftChangedListener;
import com.android.mms.util.PhoneNumberFormatter;
import com.android.mms.util.SendingProgressTokenManager;
import com.android.mms.util.SmileyParser;
import com.android.mms.util.ThumbnailManager;

import android.text.InputFilter.LengthFilter;

/// M: import @{
import android.view.MotionEvent;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.view.LayoutInflater;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.app.ProgressDialog;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.CamcorderProfile;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.provider.Browser;
import android.provider.Browser.BookmarkColumns;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.MediaStore.Audio;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.provider.Telephony.MmsSms;
import android.telephony.SmsManager;
import android.text.style.AbsoluteSizeSpan;
import android.util.Config;
import android.view.ActionMode;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.RelativeLayout;
import android.widget.AbsListView.OnScrollListener;
import android.view.ViewGroup;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.preference.PreferenceManager;
import android.widget.LinearLayout;
import android.provider.MediaStore;
import android.util.AndroidException;

import com.android.mms.transaction.SendTransaction;
import com.android.mms.ExceedMessageSizeException;
import com.android.mms.ui.ConversationList.BaseProgressQueryHandler;
import com.android.mms.util.ThreadCountManager;

import com.android.internal.telephony.IccCard;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedTelephonyService;
import com.mediatek.encapsulation.com.mediatek.CellConnService.EncapsulatedCellConnMgr;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.mms.ext.IMmsCompose;
import com.mediatek.mms.ext.MmsComposeImpl;
import com.mediatek.mms.ext.IMmsComposeHost;
import com.android.mms.MmsPluginManager;
import com.mediatek.mms.ext.IMmsDialogNotify;
import com.mediatek.mms.ext.IMmsTextSizeAdjust;
import com.mediatek.mms.ext.MmsTextSizeAdjustImpl;
import com.mediatek.mms.ext.IMmsTextSizeAdjustHost;
import com.mediatek.encapsulation.com.mediatek.telephony.EncapsulatedTelephonyManagerEx;
import com.android.mms.MmsApp;
import com.android.internal.telephony.PhoneFactory;
import com.android.mms.transaction.MmsSystemEventReceiver.OnShutDownListener;
import com.android.mms.transaction.MmsSystemEventReceiver.OnSimInforChangedListener;
import com.mediatek.encapsulation.MmsLog;
import com.android.mms.transaction.TransactionService;
import com.android.mms.transaction.Transaction;
import com.google.android.mms.pdu.PduHeaders;
import com.android.vcard.VCardComposer;
import com.android.vcard.VCardConfig;
import com.android.vcard.exception.VCardException;
import com.android.mms.model.FileAttachmentModel;
import com.android.mms.model.TextModel;
import com.android.mms.ui.EmoticonPanel;
import com.android.mms.ui.EmoticonPanel.EditEmoticonListener;
import com.android.mms.ui.HeightChangedLinearLayout;
import com.android.mms.ui.HeightChangedLinearLayout.LayoutSizeChangedListener;
import com.android.mms.util.DraftCache;
import com.android.mms.widget.MmsWidgetProvider;
import com.android.mms.ui.VCardAttachment;
import com.android.mms.ui.PduBodyCache;
import com.android.mms.util.SmileyParser2;
import com.mediatek.encapsulation.android.os.storage.EncapsulatedStorageManager;
import com.mediatek.encapsulation.com.mediatek.pluginmanager.EncapsulatedPluginManager;
import com.mediatek.encapsulation.android.telephony.gemini.EncapsulatedGeminiSmsManager;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSmsManager;
import com.mediatek.encapsulation.android.drm.EncapsulatedDrmStore;
import com.mediatek.encapsulation.android.app.EncapsulatedStatusBarManager;
import com.mediatek.encapsulation.android.provider.EncapsulatedSettings;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;
import com.mediatek.ipmsg.util.IpMessageUtils;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/// @}
/// M: add for ip message
import android.media.ThumbnailUtils;
import android.provider.MediaStore.Video.Thumbnails;
import android.provider.Telephony.TextBasedSmsColumns;
import android.provider.Telephony.ThreadSettings;
import android.provider.Telephony.Mms.Draft;
import android.text.style.ImageSpan;

import com.android.mms.util.MessageConsts;
import com.android.mms.model.AudioModel;
import com.android.mms.model.ImageModel;
import com.android.mms.model.VideoModel;

import com.mediatek.ipmsg.ui.SharePanel;
import com.mediatek.mms.ipmessage.INotificationsListener;
import com.mediatek.mms.ipmessage.message.IpAttachMessage;
import com.mediatek.mms.ipmessage.message.IpImageMessage;
import com.mediatek.mms.ipmessage.message.IpLocationMessage;
import com.mediatek.mms.ipmessage.message.IpMessage;
import com.mediatek.mms.ipmessage.IpMessageConfig;
import com.mediatek.mms.ipmessage.IpMessageConsts;
import com.mediatek.mms.ipmessage.IpMessageConsts.ContactStatus;
import com.mediatek.mms.ipmessage.IpMessageConsts.FeatureId;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageMediaTypeFlag;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageSendMode;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageServiceId;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageStatus;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageType;
import com.mediatek.mms.ipmessage.IpMessageConsts.ReminderType;
import com.mediatek.mms.ipmessage.IpMessageConsts.RemoteActivities;
import com.mediatek.mms.ipmessage.IpMessageConsts.SelectContactType;
import com.mediatek.mms.ipmessage.IpMessageConsts.SpecialSimId;
import com.mediatek.mms.ipmessage.message.IpTextMessage;
import com.mediatek.mms.ipmessage.message.IpVCalendarMessage;
import com.mediatek.mms.ipmessage.message.IpVCardMessage;
import com.mediatek.mms.ipmessage.message.IpVideoMessage;
import com.mediatek.mms.ipmessage.message.IpVoiceMessage;
/// M: import @{

//add for attachment enhance
import com.google.android.mms.ContentType;
import com.mediatek.mms.ext.IMmsAttachmentEnhance;
import com.mediatek.mms.ext.MmsAttachmentEnhanceImpl;
import com.mediatek.pluginmanager.Plugin;
import com.mediatek.pluginmanager.PluginManager;
///@}

/**
 * This is the main UI for:
 * 1. Composing a new message;
 * 2. Viewing/managing message history of a conversation.
 *
 * This activity can handle following parameters from the intent
 * by which it's launched.
 * thread_id long Identify the conversation to be viewed. When creating a
 *         new message, this parameter shouldn't be present.
 * msg_uri Uri The message which should be opened for editing in the editor.
 * address String The addresses of the recipients in current conversation.
 * exit_on_sent boolean Exit this activity after the message is sent.
 */
public class ComposeMessageActivity extends Activity
        implements View.OnClickListener, TextView.OnEditorActionListener,
        MessageStatusListener, Contact.UpdateListener,OnShutDownListener,
        INotificationsListener,
        OnSimInforChangedListener, IMmsComposeHost, IMmsTextSizeAdjustHost {
    public static final int REQUEST_CODE_ATTACH_IMAGE     = 100;
    public static final int REQUEST_CODE_TAKE_PICTURE     = 101;
    public static final int REQUEST_CODE_ATTACH_VIDEO     = 102;
    public static final int REQUEST_CODE_TAKE_VIDEO       = 103;
    public static final int REQUEST_CODE_ATTACH_SOUND     = 104;
    public static final int REQUEST_CODE_RECORD_SOUND     = 105;
    public static final int REQUEST_CODE_CREATE_SLIDESHOW = 106;
    public static final int REQUEST_CODE_ECM_EXIT_DIALOG  = 107;
    public static final int REQUEST_CODE_ADD_CONTACT      = 108;
    public static final int REQUEST_CODE_PICK             = 109;

    /// M: fix bug ALPS00490684, update group mms state from GROUP_PARTICIPANTS to setting @{
    public static final int REQUEST_CODE_GROUP_PARTICIPANTS = 130;
    /// @}
    /// M: fix bug ALPS00448677, update or delete Contact Chip
    public static final int REQUEST_CODE_VIEW_CONTACT     = 111;
    private Contact mInViewContact;
    /// @}

    private static final String TAG = "Mms/compose";
    /// M: add for ip message
    private static final String TAG_DIVIDER = "Mms/divider";

    private static final boolean DEBUG = false;
    private static final boolean TRACE = false;
    private static final boolean LOCAL_LOGV = false;

    // Menu ID
    private static final int MENU_ADD_SUBJECT           = 0;
    private static final int MENU_DELETE_THREAD         = 1;
    private static final int MENU_ADD_ATTACHMENT        = 2;
    private static final int MENU_DISCARD               = 3;
    private static final int MENU_SEND                  = 4;
    private static final int MENU_CALL_RECIPIENT        = 5;
    private static final int MENU_CONVERSATION_LIST     = 6;
    private static final int MENU_DEBUG_DUMP            = 7;
    /// M: add for ip message, option menu
    private static final int MENU_INVITE_FRIENDS_TO_CHAT = 100;
    private static final int MENU_SELECT_MESSAGE        = 101;
    private static final int MENU_MARK_AS_SPAM          = 102;
    private static final int MENU_REMOVE_SPAM           = 103;
    private static final int MENU_ADD_SHORTCUT          = 104;
    private static final int MENU_VIEW_ALL_MEDIA        = 105;
    private static final int MENU_VIEW_ALL_LOCATION     = 106;
    private static final int MENU_INVITE_FRIENDS_TO_IPMSG = 107;

    // Context menu ID
    private static final int MENU_VIEW_CONTACT          = 12;
    private static final int MENU_ADD_TO_CONTACTS       = 13;

    private static final int MENU_EDIT_MESSAGE          = 14;
    private static final int MENU_VIEW_SLIDESHOW        = 16;
    private static final int MENU_VIEW_MESSAGE_DETAILS  = 17;
    private static final int MENU_DELETE_MESSAGE        = 18;
    private static final int MENU_SEARCH                = 19;
    private static final int MENU_DELIVERY_REPORT       = 20;
    private static final int MENU_FORWARD_MESSAGE       = 21;
    private static final int MENU_CALL_BACK             = 22;
    private static final int MENU_SEND_EMAIL            = 23;
    private static final int MENU_COPY_MESSAGE_TEXT     = 24;
    private static final int MENU_COPY_TO_SDCARD        = 25;
    private static final int MENU_INSERT_SMILEY         = 26;
    private static final int MENU_ADD_ADDRESS_TO_CONTACTS = 27;
    private static final int MENU_LOCK_MESSAGE          = 28;
    private static final int MENU_UNLOCK_MESSAGE        = 29;
    private static final int MENU_SAVE_RINGTONE         = 30;
    private static final int MENU_PREFERENCES           = 31;
    /// M: google jb.mr1 patch
    private static final int MENU_GROUP_PARTICIPANTS    = 32;

    private static final int MENU_CHAT_SETTING          = 137; // add for chat setting

    /// M: add for ip message, context menu
    private static final int MENU_RETRY                 = 200;
    private static final int MENU_DELETE_IP_MESSAGE     = 201;
    private static final int MENU_SHARE                 = 202;
    private static final int MENU_MARK_AS_IMPORTANT     = 203;
    private static final int MENU_REMOVE_FROM_IMPORTANT = 204;
    private static final int MENU_SAVE_ATTACHMENT       = 205;
    private static final int MENU_VIEW_IP_MESSAGE       = 206;
    private static final int MENU_COPY = 207;
    private static final int MENU_SEND_VIA_TEXT_MSG = 208;
    private static final int MENU_EXPORT_SD_CARD = 209;
    private static final int MENU_FORWARD_IPMESSAGE = 210;
    private static final int RECIPIENTS_MAX_LENGTH = 312;

    private static final int MESSAGE_LIST_QUERY_TOKEN = 9527;
    private static final int MESSAGE_LIST_QUERY_AFTER_DELETE_TOKEN = 9528;

    private static final int DELETE_MESSAGE_TOKEN  = 9700;
    /// M: add for ip message, query unread message
    private static final int MESSAGE_LIST_UNREAD_QUERY_TOKEN   = 9800;

    private static final int CHARS_REMAINING_BEFORE_COUNTER_SHOWN = 10;

    private static final long NO_DATE_FOR_DIALOG = -1L;

    private static final String EXIT_ECM_RESULT = "exit_ecm_result";

    // When the conversation has a lot of messages and a new message is sent, the list is scrolled
    // so the user sees the just sent message. If we have to scroll the list more than 20 items,
    // then a scroll shortcut is invoked to move the list near the end before scrolling.
    private static final int MAX_ITEMS_TO_INVOKE_SCROLL_SHORTCUT = 20;

    // Any change in height in the message list view greater than this threshold will not
    // cause a smooth scroll. Instead, we jump the list directly to the desired position.
    private static final int SMOOTH_SCROLL_THRESHOLD = 200;

    ///M: ALPS00592380: status of detection
    private static final int DETECT_INIT = 0;
    private static final int DETECT_ANGLE_BRACKETS = 1;
    private static final int DETECT_ANGLE_BRACKETS_WITH_WORD = 2;

    private ContentResolver mContentResolver;

    private BackgroundQueryHandler mBackgroundQueryHandler;

    private Conversation mConversation;     // Conversation we are working in

    private boolean mExitOnSent;            // Should we finish() after sending a message?
                                            // TODO: mExitOnSent is obsolete -- remove

    private View mTopPanel;                 // View containing the recipient and subject editors
    private View mBottomPanel;              // View containing the text editor, send button, ec.
    private EditText mTextEditor;           // Text editor to type your message into
    private TextView mTextCounter;          // Shows the number of characters used in text editor
    private TextView mSendButtonMms;        // Press to send mms
    private ImageButton mSendButtonSms;     // Press to send sms
    private EditText mSubjectTextEditor;    // Text editor for MMS subject

    private AttachmentEditor mAttachmentEditor;
    private View mAttachmentEditorScrollView;

    private MessageListView mMsgListView;        // ListView for messages in this conversation
    /// M: @{
    public MessageListAdapter mMsgListAdapter = null;  // and its corresponding ListAdapter    
    /// @}
    private RecipientsEditor mRecipientsEditor;  // UI control for editing recipients
    private ImageButton mRecipientsPicker;       // UI control for recipients picker

    private boolean mIsKeyboardOpen;             // Whether the hardware keyboard is visible
    /// M: fix bug ALPS00419856, set TextEditor Height = four when unlock screen @{
    private boolean mIsSoftKeyBoardShow;
    private static final int SOFT_KEY_BOARD_MIN_HEIGHT = 150;
    /// @}
    private boolean mIsLandscape;                // Whether we're in landscape mode

    private boolean mPossiblePendingNotification;   // If the message list has changed, we may have
                                                    // a pending notification to deal with.

    private boolean mToastForDraftSave;   // Whether to notify the user that a draft is being saved

    private boolean mSentMessage;       // true if the user has sent a message while in this
                                        // activity. On a new compose message case, when the first
                                        // message is sent is a MMS w/ attachment, the list blanks
                                        // for a second before showing the sent message. But we'd
                                        // think the message list is empty, thus show the recipients
                                        // editor thinking it's a draft message. This flag should
                                        // help clarify the situation.

    private WorkingMessage mWorkingMessage;         // The message currently being composed.

    private AlertDialog mSmileyDialog;

    private boolean mWaitingForSubActivity;
    private int mLastRecipientCount;            // Used for warning the user on too many recipients.
    private AttachmentTypeSelectorAdapter mAttachmentTypeSelectorAdapter;

    private boolean mSendingMessage;    // Indicates the current message is sending, and shouldn't send again.

    private Intent mAddContactIntent;   // Intent used to add a new contact

    private Uri mTempMmsUri;            // Only used as a temporary to hold a slideshow uri
    private long mTempThreadId;         // Only used as a temporary to hold a threadId

    private AsyncDialog mAsyncDialog;   // Used for background tasks.

    private String mDebugRecipients;
    private int mLastSmoothScrollPosition;
    private boolean mScrollOnSend;      // Flag that we need to scroll the list to the end.

    private int mSavedScrollPosition = -1;  // we save the ListView's scroll position in onPause(),
                                            // so we can remember it after re-entering the activity.
                                            // If the value >= 0, then we jump to that line. If the
                                            // value is maxint, then we jump to the end.
    private long mLastMessageId;

    private boolean  mErrorDialogShown = true;
    /**
     * Whether this activity is currently running (i.e. not paused)
     */
    private boolean mIsRunning;
    // key for extras and icicles
    public static final String THREAD_ID = "thread_id";

    ///M: add for fix issue ALPS00380788
    private ProgressDialog mCellProgressDialog;

    private ImageView mWallPaper; //wall_paper

    /// M: fix bug ALPS00397146, removeThumbnailManager uri
    // (Content://media/external/images/media/) when it rotated
    private static final HashMap<String, Integer> sDegreeMap = new HashMap<String, Integer>();
    /// @}

/// M: add for ip message {@
    private static final String IPMSG_TAG = "Mms/ipmsg/compose";

    private static final int REQUEST_CODE_IPMSG_TAKE_PHOTO        = 200;
    private static final int REQUEST_CODE_IPMSG_RECORD_VIDEO      = 201;
    private static final int REQUEST_CODE_IPMSG_DRAW_SKETCH       = 202;
    private static final int REQUEST_CODE_IPMSG_SHARE_CONTACT     = 203;
    private static final int REQUEST_CODE_IPMSG_CHOOSE_PHOTO      = 204;
    private static final int REQUEST_CODE_IPMSG_CHOOSE_VIDEO      = 205;
    private static final int REQUEST_CODE_IPMSG_RECORD_AUDIO      = 206;
    private static final int REQUEST_CODE_IPMSG_SHARE_LOCATION    = 207;
    private static final int REQUEST_CODE_IPMSG_CHOOSE_AUDIO      = 208;
    private static final int REQUEST_CODE_IPMSG_SHARE_VCALENDAR   = 209;

    public static final int REQUEST_CODE_IPMSG_PICK_CONTACT      = 210;
    public static final int REQUEST_CODE_INVITE_FRIENDS_TO_CHAT  = 211;

    private static final int SMS_CONVERT                            = 0;
    private static final int MMS_CONVERT                            = 1;
    private static final int SERVICE_IS_NOT_ENABLED                 = 0;
    private static final int RECIPIENTS_ARE_NOT_IP_MESSAGE_USER     = 1;

    ///M: for forward ipMsg
    public static final String FORWARD_IPMESSAGE = "forwarded_ip_message";
    public static final String IP_MESSAGE_ID = "ip_msg_id";

    //public static final String SELECTED_ID = "SELECTID";

    private String mPhotoFilePath = "";
    private String mDstPath = "";
    private int mDuration = 0;
    private String mCalendarSummary = "";

    // chat mode number
    private String mChatModeNumber = "";
    private String mBeforeTextChangeString = "";

    // ipmessage status
    private boolean mIsIpMessageRecipients = false;
    private boolean mIsIpServiceEnabled = false;
    private boolean mIsMessageDefaultSimIpServiceEnabled = false;
    private boolean mIsCaptionOn = false;
    private boolean mIsImageCaptionOn = false;
    private boolean mIsVideoCaptionOn = false;
    private boolean mIsAudioCaptionOn = false;
    private boolean mIsEditingCaption = false;

    // keyboard for share and emoticon
    private boolean mShowKeyBoardFromShare = false;
    private boolean mShowKeyBoardFromEmoticon = false;
    private Object mWaitingImeChangedObject = new Object();

    ///M: fix for bug ALPS00439844 @{
    private Editable mLatestText;
    private boolean isAddSmileySpans = false;

    // unread divider
    private boolean mShowUnreadDivider = true;
    private boolean mIsMarkMsgAsRead = true;

    ///M: for check whether or not can convert IpMessage to Common message.
    ///M: can not support translating media through common message , -1;
    private static final int MESSAGETYPE_UNSUPPORT = -1;
    private static final int MESSAGETYPE_COMMON = 0;
    private static final int MESSAGETYPE_TEXT = 1;
    private static final int MESSAGETYPE_MMS = 2;

    /// M: add for ip message, important thread
    private boolean mIsImportantThread = false;
    private long mEnterImportantTimestamp = 0L;

    private String mIpMessageVcardName = "";

    private InputMethodManager mInputMethodManager = null;

    private ImageButton mEmoticonButton;
    private ImageButton mShareButton;
    private SharePanel mSharePanel;
    private EmoticonPanel mEmoticonPanel;
    private ImageView mIpMessageThumbnail;
    private ImageButton mSendButtonIpMessage;     // press to send ipmessage

    private TextView mTypingStatus;
    private LinearLayout mTypingStatusView;
    private boolean mIsDestroyTypingThread = false;
    private long mLastSendTypingStatusTimestamp = 0L;

    ///M: for indentify that just send common message.
    private boolean mJustSendMsgViaCommonMsgThisTime = false;
    ///M: whether or not show invite friends to use ipmessage interface.
    private boolean mShowInviteMsg = false;

    ///M: working IP message
    private int mIpMessageDraftId = 0;
    private IpMessage mIpMessageDraft;
    private IpMessage mIpMessageForSend;
    private boolean mIsClearTextDraft = false;
    private AlertDialog mReplaceDialog = null;

    ///M: ipmessage invite panel
    private View mInviteView;
    private TextView mInviteMsg;
    private Button mInvitePostiveBtn;
    private Button mInviteNegativeBtn;
    private static final String KEY_SELECTION_SIMID = "SIMID";

    ///M: actionbar customer view
    private View mActionBarCustomView;
    private TextView mTopTitle;
    private ImageView mMuteLogo;
    private TextView mTopSubtitle;

    private static final int MAX_SPAN_NUMBER = 24;
    private int mEmoticonNumber = 0;

    /// M: fix bug ALPS00600816, re-create SlideshowModel
    private long mEditSlideshowStart;

    private Runnable mHideTypingStatusRunnable = new Runnable() {
        @Override
        public void run() {
            MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "notificationsReceived(): hide typing status.");
            if (null != mTypingStatusView) {
                mTypingStatusView.setVisibility(View.GONE);
            }
        }
    };

    private Runnable mShowTypingStatusRunnable = new Runnable() {
        @Override
        public void run() {
            MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "notificationsReceived(): show IM status view.");
            if (null != mTypingStatusView) {
                mTypingStatusView.setVisibility(View.VISIBLE);
            }
        }
    };

    private Object mShowTypingLockObject = new Object();
    private Thread mShowTypingThread = new Thread(new Runnable() {
        @Override
        public void run() {
            String showingStr = IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_typing_text);
            final String displayString0 = showingStr + ".    ";
            final String displayString1 = showingStr + "..   ";
            final String displayString2 = showingStr + "...  ";
            int i = 0;
            while (!mIsDestroyTypingThread) {
                while (null != mTypingStatusView && mTypingStatusView.getVisibility() != View.GONE) {
                    if (ComposeMessageActivity.this.isFinishing()) {
                        break;
                    }
                    switch (i % 3) {
                    case 0:
                        mIpMsgHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "mShowTypingThread: display 0.");
                                mTypingStatus.setText(displayString0);
                            }
                        });
                        i++;
                        break;
                    case 1:
                        mIpMsgHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "mShowTypingThread: display 1.");
                                mTypingStatus.setText(displayString1);
                            }
                        });
                        i++;
                        break;
                    case 2:
                        mIpMsgHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "mShowTypingThread: display 2.");
                                mTypingStatus.setText(displayString2);
                            }
                        });
                        i++;
                        break;
                    default:
                        break;
                    }
                    synchronized (this) {
                        try {
                            this.wait(1000);
                        } catch (InterruptedException e) {
                            MmsLog.d(TAG, "InterruptedException");
                        }
                    }
                }
                synchronized (mShowTypingLockObject) {
                    try {
                        mShowTypingLockObject.wait();
                    } catch (InterruptedException e) {
                        MmsLog.d(TAG, "InterruptedException");
                    }
                }
            }
            MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "mShowTypingThread: destroy thread.");
        }
    }, "showTypingThread");

    public Handler mIpMsgHandler = new Handler() {
        public void handleMessage(Message msg) {
            MmsLog.d(IPMSG_TAG, "handler msg type is: " + msg.what);
            MmsLog.d(IPMSG_TAG, "MmsConfig.getIpMessagServiceId() = "
                + MmsConfig.getIpMessagServiceId(ComposeMessageActivity.this)
                + ", MmsConfig.isServiceEnabled() = " + MmsConfig.isServiceEnabled(ComposeMessageActivity.this)
                + ", isNetworkConnected = " + isNetworkConnected(ComposeMessageActivity.this)
                + ", has SD = " + IpMessageUtils.getSDCardStatus());
            switch (msg.what) {
            case MessageConsts.ACTION_SHARE:
                if (!IpMessageUtils.getIpMessagePlugin(ComposeMessageActivity.this).isActualPlugin()) {
                    doMmsAction(msg);
                } else if (MmsConfig.isServiceEnabled(ComposeMessageActivity.this)
                        && isNetworkConnected(ComposeMessageActivity.this)
                        && IpMessageUtils.getSDCardStatus()) {
                    doMoreAction(msg);
                } else {
                    doMoreActionForMms(msg);
                }
                break;
            case MessageUtils.SHOW_INVITE_PANEL:
                showInvitePanel(msg.arg1);
                break;
            case MessageUtils.UPDATE_SENDBUTTON:
                mMessageSimId = Settings.System.getLong(getContentResolver(),
                    Settings.System.SMS_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
                if (mMessageSimId != Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK
                        && mMessageSimId != Settings.System.DEFAULT_SIM_NOT_SET) {
                    mIsMessageDefaultSimIpServiceEnabled = MmsConfig.isServiceEnabled(ComposeMessageActivity.this,
                        (int) mMessageSimId);
                } else {
                    mIsMessageDefaultSimIpServiceEnabled = MmsConfig.isServiceEnabled(ComposeMessageActivity.this);
                }
                updateSendButtonState();
                break;
            default:
                MmsLog.d(IPMSG_TAG, "msg type: " + msg.what + "not handler");
                break;
            }
            super.handleMessage(msg);
        }
    };
/// @}

    @SuppressWarnings("unused")
    public static void log(String logMsg) {
        Thread current = Thread.currentThread();
        long tid = current.getId();
        StackTraceElement[] stack = current.getStackTrace();
        String methodName = stack[3].getMethodName();
        // Prepend current thread ID and name of calling method to the message.
        logMsg = "[" + tid + "] [" + methodName + "] " + logMsg;
        MmsLog.d(TAG, logMsg);
    }

    //==========================================================
    // Inner classes
    //==========================================================

    private void editSlideshow() {
        // The user wants to edit the slideshow. That requires us to persist the slideshow to
        // disk as a PDU in saveAsMms. This code below does that persisting in a background
        // task. If the task takes longer than a half second, a progress dialog is displayed.
        // Once the PDU persisting is done, another runnable on the UI thread get executed to start
        // the SlideshowEditActivity.

        /// M: fix bug ALPS00520531, Do not load draft when compose is going to edit slideshow
        mContentResolver.unregisterContentObserver(mDraftChangeObserver);

        getAsyncDialog().runAsync(new Runnable() {
            @Override
            public void run() {
                // This runnable gets run in a background thread.
                mTempMmsUri = mWorkingMessage.saveAsMms(false);
            }
        }, new Runnable() {
            @Override
            public void run() {
                // Once the above background thread is complete, this runnable is run
                // on the UI thread.
                if (mTempMmsUri == null) {
                    return;
                }
                Intent intent = new Intent(ComposeMessageActivity.this,
                        SlideshowEditActivity.class);
                intent.setData(mTempMmsUri);
                startActivityForResult(intent, REQUEST_CODE_CREATE_SLIDESHOW);

                /// M: fix bug ALPS00600816, re-create SlideshowModel
                mEditSlideshowStart = System.currentTimeMillis();
                mWorkingMessage.setReCreateSlideshow(true);
            }
        // M: fix bug ALPS00351027
        }, R.string.sync_mms_to_db);
    }

    private final Handler mAttachmentEditorHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            /// M: Code analyze 026, If the two clicks are too close. @{
            if (isTwoClickClose(mLastButtonClickTime, 2000)) {
                MmsLog.d("MmsTest", "ignore a close click");
                return;
            }
           /// @}

            switch (msg.what) {
                case AttachmentEditor.MSG_EDIT_SLIDESHOW: {
                    /// M: Code analyze 024, If the click operator can be responsed. @{
                    if (mClickCanResponse) {
                        mClickCanResponse = false;
                        /// M: Code analyze 038, If the user is editing slideshow now.
                        /// Do not allow the activity finish but return directly when back key is entered. @{
                        mIsEditingSlideshow = true;
                        /// @}
                        editSlideshow();
                    }
                    /// @}
                    break;
                }
                case AttachmentEditor.MSG_SEND_SLIDESHOW: {
                    if (isPreparedForSending()) {
                        /// M: Code analyze 028, Before sending message,check the recipients count
                        /// and add sim card selection dialog if multi sim cards exist.@{
                        // ComposeMessageActivity.this.confirmSendMessageIfNeeded();
                        checkRecipientsCount();
                        /// @}
                    }
                    break;
                }
                case AttachmentEditor.MSG_VIEW_IMAGE:
                case AttachmentEditor.MSG_PLAY_VIDEO:
                case AttachmentEditor.MSG_PLAY_AUDIO:
                case AttachmentEditor.MSG_PLAY_SLIDESHOW:
                    /// M: Code analyze 024, If the click operator can be responsed. @{
                    if (mClickCanResponse) {
                        mClickCanResponse = false;
                        viewMmsMessageAttachment(msg.what);
                    }
                    /// @}
                    /// M: Code analyze 051, Hide input keyboard.@{
                    hideInputMethod();
                    /// @}
                    break;

                case AttachmentEditor.MSG_REPLACE_IMAGE:
                    /// M: @{
                    getSharedPreferences("SetDefaultLayout", 0).edit().putBoolean("SetDefaultLayout", false).commit();
                    /// @}
                case AttachmentEditor.MSG_REPLACE_VIDEO:
                case AttachmentEditor.MSG_REPLACE_AUDIO:
                    /// M: Code analyze 051, Hide input keyboard.@{
                    hideInputMethod();
                    /// @}
                    showAddAttachmentDialog(false);
                    break;

                case AttachmentEditor.MSG_REMOVE_ATTACHMENT:
                    mWorkingMessage.removeAttachment(true);
                    break;

                    //add for attachment enhance
                    case AttachmentEditor.MSG_REMOVE_EXTERNAL_ATTACHMENT:
                        mWorkingMessage.removeExternalAttachment();
                        break;
                    case AttachmentEditor.MSG_REMOVE_SLIDES_ATTACHMENT:
                        mWorkingMessage.removeSlidesAttachment();
                        break;
                default:
                    break;
            }
        }
    };


    private void viewMmsMessageAttachment(final int requestCode) {
        final SlideshowModel slideshow = mWorkingMessage.getSlideshow();
        if (slideshow == null) {
            throw new IllegalStateException("mWorkingMessage.getSlideshow() == null");
        }
        /// M: Code analyze 035, The audio becomes "simple" slideshow.
        /// Launch the slideshow activity or MmsPlayerActivity to play/view media attachment. @{
        SlideModel slideOne = slideshow.get(0);
        if (slideshow.isSimple() && slideOne != null && !slideOne.hasAudio()) {
            MessageUtils.viewSimpleSlideshow(this, slideshow);
        } else {
            // The user wants to view the slideshow. That requires us to persist the slideshow to
            // disk as a PDU in saveAsMms. This code below does that persisting in a background
            // task. If the task takes longer than a half second, a progress dialog is displayed.
            // Once the PDU persisting is done, another runnable on the UI thread get executed to
            // start the SlideshowActivity.
            getAsyncDialog().runAsync(new Runnable() {
                @Override
                public void run() {
                    // This runnable gets run in a background thread.
                    mTempMmsUri = mWorkingMessage.saveAsMms(false);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    // Once the above background thread is complete, this runnable is run
                    // on the UI thread.
                    // Launch the slideshow activity to play/view.
                    Intent intent;
                    SlideModel slide = slideshow.get(0);
                    /// M: play the only audio directly
                    if ((slideshow.isSimple() && slide != null && slide.hasAudio())
                        || (requestCode == AttachmentEditor.MSG_PLAY_AUDIO)) {
                        intent = new Intent(ComposeMessageActivity.this, SlideshowActivity.class);
                    } else {
                        intent = new Intent(ComposeMessageActivity.this, MmsPlayerActivity.class);
                    }
                    intent.setData(mTempMmsUri);
                    if (mTempMmsUri == null) {
                        return;
                    }
                    if (requestCode > 0) {
                        startActivityForResult(intent, requestCode);
                    } else {
                        startActivity(intent);
                    }
                    //MessageUtils.launchSlideshowActivity(ComposeMessageActivity.this, mTempMmsUri,
                    //         requestCode);
                    /// @}
                }
            // M: fix bug ALPS00351027
            }, R.string.sync_mms_to_db);
        }
    }


    private final Handler mMessageListItemHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MessageItem msgItem = (MessageItem) msg.obj;
                /// M: move if into switch, we have item not use it.
                /// M: Code analyze, fix bug ALPS00358964
                switch (msg.what) {
                    case MessageListItem.MSG_LIST_DETAILS:
                        if (msgItem != null) {
                            showMessageDetails(msgItem);
                        }
                        break;

                    case MessageListItem.MSG_LIST_EDIT:
                        if (msgItem != null) {
                            editMessageItem(msgItem);
                            updateSendButtonState();
                            drawBottomPanel();
                        }
                        break;

                    case MessageListItem.MSG_LIST_PLAY:
                        if (msgItem != null) {
                            switch (msgItem.mAttachmentType) {
                                case WorkingMessage.IMAGE:
                                case WorkingMessage.VIDEO:
                                case WorkingMessage.AUDIO:
                                case WorkingMessage.SLIDESHOW:
                                    MessageUtils.viewMmsMessageAttachment(ComposeMessageActivity.this,
                                            msgItem.mMessageUri, msgItem.mSlideshow,
                                            getAsyncDialog());
                                    break;
                            }
                        }
                        break;

                    /// M: Code analyze 039, When the cache add new item,
                    /// notifiy ComposeMessageAcitivity the data has been changed .@{
                    case MessageListAdapter.MSG_LIST_NEED_REFRASH: {
                        boolean isClearCache = msg.arg1 == MessageListAdapter.MESSAGE_LIST_REFRASH_WITH_CLEAR_CACHE;
                        MmsLog.d(MessageListAdapter.CACHE_TAG, "mMessageListItemHandler.handleMessage(): " +
                                    "run adapter notify in mMessageListItemHandler. isClearCache = " + isClearCache);
                        mMsgListAdapter.setClearCacheFlag(isClearCache);
                        mMsgListAdapter.notifyDataSetChanged();
                        return;
                    }
                    /// @
                    /// M:,Support messages multi-delete opeartor. @{
                    case MessageListItem.ITEM_CLICK: { // can be deleted!!!
                        mMsgListAdapter.changeSelectedState(msg.arg1);
                        return;
                    }
                    /// @}
                    case MessageListItem.MSG_LIST_RESEND_IPMSG:
                        long[][] allFailedIpMsgIds = getAllFailedIpMsgByThreadId(mConversation.getThreadId());
                        MmsLog.d(IPMSG_TAG, "mMessageListItemHandler.handleMessage(): Msg_list_reand_ipmsg, " +
                            "allFailedIpMsg len:" + allFailedIpMsgIds.length);
                        showResendConfirmDialg(msg.getData().getLong("MSG_ID"), msg.getData().getInt(
                            "SIM_ID"), allFailedIpMsgIds);
                        return;

                    case MessageListItem.LOAD_ALL_MESSAGES:
                        mIsImportantThread = false;
                        mEnterImportantTimestamp = 0L;
                        mMsgListAdapter.setIsImportantThread(false);
                        mMsgListAdapter.setEnterImportantThreadTime(0L);
                        startMsgListQuery();
                        return;
                    default:
                        MmsLog.w(TAG, "Unknown message: " + msg.what);
                        return;
                }
        }
    };

    private boolean showMessageDetails(MessageItem msgItem) {
        /// M: Code analyze 040, The function getMessageDetails use MessageItem but not cursor now.@{
        /*
        Cursor cursor = mMsgListAdapter.getCursorForItem(msgItem);
        if (cursor == null) {
            return false;
        }
        */
        String messageDetails = MessageUtils.getMessageDetails(
               //ComposeMessageActivity.this, cursor, msgItem.mMessageSize);
                ComposeMessageActivity.this, msgItem);
        /// @}
        MmsLog.d(TAG,"showMessageDetails. messageDetails:" + messageDetails);
        new AlertDialog.Builder(ComposeMessageActivity.this)
                .setTitle(R.string.message_details_title)
                .setMessage(messageDetails)
                .setCancelable(true)
                .show();
        return true;
    }

    private final OnKeyListener mSubjectKeyListener = new OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }

            // When the subject editor is empty, press "DEL" to hide the input field.
            if ((keyCode == KeyEvent.KEYCODE_DEL) && (mSubjectTextEditor.length() == 0)) {
                showSubjectEditor(false);
                mWorkingMessage.setSubject(null, true);
                /// M: add for character counter
                updateCounter(mWorkingMessage.getText(), 0, 0, 0);
                return true;
            }
            return false;
        }
    };

    /**
     * Return the messageItem associated with the type ("mms" or "sms") and message id.
     * @param type Type of the message: "mms" or "sms"
     * @param msgId Message id of the message. This is the _id of the sms or pdu row and is
     * stored in the MessageItem
     * @param createFromCursorIfNotInCache true if the item is not found in the MessageListAdapter's
     * cache and the code can create a new MessageItem based on the position of the current cursor.
     * If false, the function returns null if the MessageItem isn't in the cache.
     * @return MessageItem or null if not found and createFromCursorIfNotInCache is false
     */
    private MessageItem getMessageItem(String type, long msgId,
            boolean createFromCursorIfNotInCache) {
        return mMsgListAdapter.getCachedMessageItem(type, msgId,
                createFromCursorIfNotInCache ? mMsgListAdapter.getCursor() : null);
    }

    private boolean isCursorValid() {
        // Check whether the cursor is valid or not.
        Cursor cursor = mMsgListAdapter.getCursor();
        if (cursor.isClosed() || cursor.isBeforeFirst() || cursor.isAfterLast()) {
            MmsLog.e(TAG, "Bad cursor.", new RuntimeException());
            return false;
        }
        return true;
    }

    /*** M: remove Google default code
    private void resetCounter() {
        /// M: Code analyze 032, According to the message state to update text counter.@{
        mTextEditor.setText(mWorkingMessage.getText());
        /// M: once updateCounter.
        updateCounter(mWorkingMessage.getText(), 0, 0, 0);
        if (mWorkingMessage.requiresMms()) {
            mTextCounter.setVisibility(View.GONE);
        } else {
            mTextCounter.setVisibility(View.VISIBLE);
        }
        /// @}
    }
    */

    private void updateCounter(CharSequence text, int start, int before, int count) {
        if (text == null) {
            MmsLog.w(TAG, "updateCounter(): text is null!");
            return;
        }
        MmsLog.d(TAG, "updateCounter(): text = " + text);
        if (IpMessageUtils.getIpMessagePlugin(this).isActualPlugin()
                && updateIpMessageCounter(text, start, before, count)) {
            return;
        }
        MmsLog.d(TAG, "updateCounter(): common message");
        /// M: Code analyze 031, Add encode type for calculating message lenght and always show
        /// text counter if it is in sms mode.@{
        /*
        WorkingMessage workingMessage = mWorkingMessage;
        if (workingMessage.requiresMms()) {
            // If we're not removing text (i.e. no chance of converting back to SMS
            // because of this change) and we're in MMS mode, just bail out since we
            // then won't have to calculate the length unnecessarily.
            final boolean textRemoved = (before > count);
            if (!textRemoved) {
                showSmsOrMmsSendButton(workingMessage.requiresMms());
                return;
            }
        }
        */
        int[] params = null;

        int encodingType = SmsMessage.ENCODING_UNKNOWN;

        if (MmsConfig.getSmsEncodingTypeEnabled()) {
            encodingType = MessageUtils.getSmsEncodingType(ComposeMessageActivity.this);
        }

        params = SmsMessage.calculateLength(text, false, encodingType);
            /* SmsMessage.calculateLength returns an int[4] with:
             *   int[0] being the number of SMS's required,
             *   int[1] the number of code units used,
             *   int[2] is the number of code units remaining until the next message.
             *   int[3] is the encoding type that should be used for the message.
             */
        final int msgCount = params[0];
        final int remainingInCurrentMessage = params[2];
        /*
        if (!MmsConfig.getMultipartSmsEnabled()) {
            mWorkingMessage.setLengthRequiresMms(
                    msgCount >= MmsConfig.getSmsToMmsTextThreshold(), true);
        }

        // Show the counter only if:
        // - We are not in MMS mode
        // - We are going to send more than one message OR we are getting close
        boolean showCounter = false;
        if (!workingMessage.requiresMms() &&
                (msgCount > 1 ||
                 remainingInCurrentMessage <= CHARS_REMAINING_BEFORE_COUNTER_SHOWN)) {
            showCounter = true;
        }

        showSmsOrMmsSendButton(workingMessage.requiresMms());

        if (showCounter) {
            // Update the remaining characters and number of messages required.
            String counterText = msgCount > 1 ? remainingInCurrentMessage + " / " + msgCount
                    : String.valueOf(remainingInCurrentMessage);
            mTextCounter.setText(counterText);
            mTextCounter.setVisibility(View.VISIBLE);
        } else {
            mTextCounter.setVisibility(View.GONE);
        }
         */
        mWorkingMessage.setLengthRequiresMms(
            msgCount >= MmsConfig.getSmsToMmsTextThreshold(), true);
        /// M: Show the counter
        /// M: Update the remaining characters and number of messages required.
        mUiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mWorkingMessage.requiresMms() || mTextEditor.getLineCount() <= 1) {
                    mTextCounter.setVisibility(View.GONE);
                    return;
                }
                mTextCounter.setVisibility(View.VISIBLE);
                String counterText = remainingInCurrentMessage + "/" + msgCount;
                mTextCounter.setText(counterText);
            }
        }, 100);
        /// @}
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        // requestCode >= 0 means the activity in question is a sub-activity.
        if (requestCode >= 0) {
            mWaitingForSubActivity = true;
        }
        if (mIsKeyboardOpen) {
            hideKeyboard();     // camera and other activities take a long time to hide the keyboard
        }

        /// M: Code analyze 041, Add exception handling for starting activity.@{
        if (null != intent && null != intent.getData()
                && intent.getData().getScheme().equals("mailto")) {
            try {
                super.startActivityForResult(intent, requestCode);
            } catch (ActivityNotFoundException e) {
                MmsLog.e(TAG, "Failed to startActivityForResult: " + intent);
                Intent i = new Intent().setClassName("com.android.email",
                           "com.android.email.activity.setup.AccountSetupBasics");
                this.startActivity(i);
                finish();
            } catch (Exception e) {
                MmsLog.e(TAG, "Failed to startActivityForResult: " + intent);
                Toast.makeText(this,getString(R.string.message_open_email_fail),
                      Toast.LENGTH_SHORT).show();
          }
        } else {
            try {
                super.startActivityForResult(intent, requestCode);
            } catch (ActivityNotFoundException e) {
                /// M: modify for ip message
                if (requestCode == REQUEST_CODE_PICK || requestCode == REQUEST_CODE_IPMSG_PICK_CONTACT) {
                   misPickContatct = false;
                   mShowingContactPicker = false;
                }
                Intent mchooserIntent = Intent.createChooser(intent, null);
                super.startActivityForResult(mchooserIntent, requestCode);
            }
        }
        /// @}
    }

    private void toastConvertInfo(boolean toMms) {
        final int resId = toMms ? R.string.converting_to_picture_message
                : R.string.converting_to_text_message;
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    private class DeleteMessageListener implements OnClickListener {
        private final MessageItem mMessageItem;

        public DeleteMessageListener(MessageItem messageItem) {
            mMessageItem = messageItem;
        }

        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            dialog.dismiss();

            new AsyncTask<Void, Void, Void>() {
                protected Void doInBackground(Void... none) {
                    if (mMessageItem.isMms()) {
                        WorkingMessage.removeThumbnailsFromCache(mMessageItem.getSlideshow());

                        MmsApp.getApplication().getPduLoaderManager()
                            .removePdu(mMessageItem.mMessageUri);
                        // Delete the message *after* we've removed the thumbnails because we
                        // need the pdu and slideshow for removeThumbnailsFromCache to work.
                    } else if (mMessageItem.mIpMessageId > 0) { /// M: add for ipmessage
                        /// M: delete ipmessage recorded in external db.
                        long [] ids = new long[1];
                        ids[0] = mMessageItem.mMsgId;
                        MmsLog.d(TAG, "delete ipmessage, id:" + ids[0]);
                        IpMessageUtils.getMessageManager(ComposeMessageActivity.this)
                                        .deleteIpMsg(ids, mMessageItem.mImportant, false);
                    }
                    /// M: google jb.mr1 patch, Conversation should scroll to the bottom
                    /// when incoming received @{
                    Boolean deletingLastItem = false;
                    Cursor cursor = mMsgListAdapter != null ? mMsgListAdapter.getCursor() : null;
                    if (cursor != null) {
                        cursor.moveToLast();
                        long msgId = cursor.getLong(COLUMN_ID);
                        deletingLastItem = msgId == mMessageItem.mMsgId;
                    }
                    Uri deleteUri = mMessageItem.mMessageUri;
                    MmsLog.d(TAG, "deleteUri" +deleteUri);
                    MmsLog.d(TAG, "deleteUri.host" +deleteUri.getHost());
                    if(deleteUri.getHost().equals("mms")){
                        MmsLog.d(TAG, "unregister mDraftChangeObserver");
                        mContentResolver.unregisterContentObserver(mDraftChangeObserver);
                    }
                    mBackgroundQueryHandler.startDelete(DELETE_MESSAGE_TOKEN,
                            deletingLastItem, mMessageItem.mMessageUri,
                            mMessageItem.mLocked ? null : "locked=0", null);
                    /// @}
                    return null;
                }
            }.execute();
        }
    }

    private class DiscardDraftListener implements OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            mWorkingMessage.discard();
            dialog.dismiss();
            /// M: Code analyze 042, If you discard the draft message manually.@{
            mHasDiscardWorkingMessage = true;
            /// @}
            /// M: clear IP message draft.@{
            if (IpMessageUtils.getIpMessagePlugin(ComposeMessageActivity.this).isActualPlugin() && mIpMessageDraft != null) {
                if (mIpMessageDraft.getId() > 0) {
                    IpMessageUtils.getMessageManager(ComposeMessageActivity.this).deleteIpMsg(
                        new long[] { mIpMessageDraft.getId() }, true, true);
                }
                mIpMessageDraft = null;
            }
            /// @}
            finish();
        }
    }

    private class SendIgnoreInvalidRecipientListener implements OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            /// M: Code analyze 030, Check condition before sending message.@{
            checkConditionsAndSendMessage(true);
            /// @}
            dialog.dismiss();
        }
    }

    private class CancelSendingListener implements OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            if (isRecipientsEditorVisible()) {
                mRecipientsEditor.requestFocus();
            }
            dialog.dismiss();
            /// M: @{
            updateSendButtonState(true);
            /// @}
        }
    }
    private ContactList mCutRecipients = null;

    private void confirmSendMessageIfNeeded() {
        if (!isRecipientsEditorVisible()) {
            /// M: Code analyze 030, Check condition before sending message.@{
            checkConditionsAndSendMessage(true);
            /// @}
            return;
        }

        boolean isMms = mWorkingMessage.requiresMms();
        if (mRecipientsEditor.hasInvalidRecipient(isMms)) {
            /// M: Code analyze 054, Even if there are some invalid recipients , we also try to
            /// send messag.Now, We do not disgingush there are some or all invalid recipients. @{
            updateSendButtonState();
                String title = getResourcesString(R.string.has_invalid_recipient,
                        mRecipientsEditor.formatInvalidNumbers(isMms));
                new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(title)
                    .setMessage(R.string.invalid_recipient_message)
                    .setPositiveButton(R.string.try_to_send,
                            new SendIgnoreInvalidRecipientListener())
                    .setNegativeButton(R.string.no, new CancelSendingListener())
                    .show();
            } else {
                 /// M: Code analyze 030, Check condition before sending message.(All recipients are valid.)@{
                checkConditionsAndSendMessage(true);
                 /// @}
            /// @}
        }
    }

    private final TextWatcher mRecipientsWatcher = new TextWatcher() {

        private Runnable mGroupMmsRunnable = new Runnable() {
            @Override
            public void run() {
                List<String> numbers = mRecipientsEditor.getNumbers();
                mWorkingMessage.setWorkingRecipients(numbers);
                /// M: google JB.MR1 patch, group mms
                boolean multiRecipients = numbers != null && numbers.size() > 1;
                boolean isGroupMms = MmsPreferenceActivity.getIsGroupMmsEnabled(ComposeMessageActivity.this)
                                                            && multiRecipients;
                mMsgListAdapter.setIsGroupConversation(isGroupMms);

                mWorkingMessage.setHasMultipleRecipients(multiRecipients, true);
                mWorkingMessage.setHasEmail(mRecipientsEditor.containsEmail(), true);

                checkForTooManyRecipients();
            }
        };

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // This is a workaround for bug 1609057.  Since onUserInteraction() is
            // not called when the user touches the soft keyboard, we pretend it was
            // called when textfields changes.  This should be removed when the bug
            // is fixed.
            onUserInteraction();
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Bug 1474782 describes a situation in which we send to
            // the wrong recipient.  We have been unable to reproduce this,
            // but the best theory we have so far is that the contents of
            // mRecipientList somehow become stale when entering
            // ComposeMessageActivity via onNewIntent().  This assertion is
            // meant to catch one possible path to that, of a non-visible
            // mRecipientsEditor having its TextWatcher fire and refreshing
            // mRecipientList with its stale contents.
            MmsLog.i(TAG,"mRecipientsWatcher afterTextChanged. begin~~~ \ns=" + s);
            if (!isRecipientsEditorVisible()) {
                IllegalStateException e = new IllegalStateException(
                        "afterTextChanged called with invisible mRecipientsEditor");
                // Make sure the crash is uploaded to the service so we
                // can see if this is happening in the field.
                MmsLog.w(TAG,
                     "RecipientsWatcher: afterTextChanged called with invisible mRecipientsEditor");
                return;
            }

            mUiHandler.removeCallbacks(mGroupMmsRunnable);
            mUiHandler.postDelayed(mGroupMmsRunnable, 50);

            // Walk backwards in the text box, skipping spaces.  If the last
            // character is a comma, update the title bar.
            for (int pos = s.length() - 1; pos >= 0; pos--) {
                char c = s.charAt(pos);
                if (c == ' ')
                    continue;
                /// M: Code analyze 055, If the user is inputting the follow any char,
                /// start to get numbers from the input sting.  Then use these numbers to update title. @{
                /// M: 65292 for Chinese ',' and 65307 for Chinese ';'

                /// M: fix bug ALPS00350660
                if ((c == ',') || (c == ';')) {
                    /// M: fix bug ALPS00432236, updateTitle only 10 contact in PORTRAIT @{
                    boolean isPortrait = getResources().getConfiguration().orientation
                                                         == Configuration.ORIENTATION_PORTRAIT;
                    int updateLimit = 0;
                    if (isPortrait) {
                        updateLimit = UPDATE_LIMIT_PORTRAIT;
                    } else {
                        updateLimit = UPDATE_LIMIT_LANDSCAPE;
                    }
                    /// @}
                    ContactList contacts = mRecipientsEditor.constructContactsFromInputWithLimit(false, updateLimit);
                    MmsLog.i(TAG,"mRecipientsWatcher afterTextChanged. contacts=" + (contacts.size() > 0 ? contacts.get(0).getNumber() : "null"));
                    updateTitle(contacts);
                }

                break;
            }
            /// M: Code analyze 055, If the user is inputting the follow any char,
            /// start to get numbers from the input sting.Then use these numbers to update title.
            /// If the input is empty, set the string of new message as the title.@{
            if (s.length() == 0) {
                updateTitle(new ContactList());
            }
            /// @}
            // If we have gone to zero recipients, disable send button.
            updateSendButtonState();
            MmsLog.i(TAG,"mRecipientsWatcher afterTextChanged. end~~~ ");
        }
    };

    ///M: ALPS00592380:
    ///M: check String in textEditer end with <xxxx>, ignore ',' ' 'or ';' at end
    private boolean checkStringFormatted(){
        String recipient = null;
        if (mRecipientsEditor != null) {
            recipient = mRecipientsEditor.getText().toString();
            char c = ' ';
            int detectedChar = DETECT_INIT;
            for (int i = recipient.length() - 1; i >= 0; i--) {
                c = recipient.charAt(i);
                if (detectedChar == DETECT_INIT) {
                    if ((c == ' ') || (c == ',') || (c == ';')) {
                        continue;
                    } else if (c == '>') {//find key word ">"
                        detectedChar = DETECT_ANGLE_BRACKETS;
                    } else {
                        return true;//not end with '>'
                    }
                } else if (detectedChar == DETECT_ANGLE_BRACKETS){//string end with ">"
                    if ((c == '<') || (c == ',') || (c == ';') || (c == '>')) {
                        return true;//string end with ",>" or "<>" or ">>"
                    } else {
                        //found string end with 'x>'
                        detectedChar = DETECT_ANGLE_BRACKETS_WITH_WORD;
                    }
                } else {//string end with "x>"
                    if (c == '<') {
                        return false;//string end with "<xxxx>"
                    } else if ((c == ',') || (c == ';')) {
                        return true;
                    }
                }
            }
        }
        return true;
    }

    private void checkForTooManyRecipients() {
        /// M: Code analyze 056,Now,the sms recipient limit is different from mms.
        /// We can set limit for sms or mms individually. @{
        final int recipientLimit = MmsConfig.getSmsRecipientLimit();
        /// @}
        if (recipientLimit != Integer.MAX_VALUE) {

            ///M: ALPS00592380:
            /*
             * we hope check recipients after formatted,
             * before formatted: name<number>,
             * after formatted: number,
             * so we can check if the string end with "<xxx>"
            */
            if (!checkStringFormatted()) {
                return;
            }
            final int recipientCount = recipientCount();
            boolean tooMany = recipientCount > recipientLimit;

            if (recipientCount != mLastRecipientCount) {
                // Don't warn the user on every character they type when they're over the limit,
                // only when the actual # of recipients changes.
                mLastRecipientCount = recipientCount;
                if (tooMany) {
                    String tooManyMsg = getString(R.string.too_many_recipients, recipientCount,
                            recipientLimit);
                    Toast.makeText(ComposeMessageActivity.this,
                            tooManyMsg, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private final OnCreateContextMenuListener mRecipientsMenuCreateListener =
        new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) {
            if (menuInfo != null) {
                Contact c = ((RecipientContextMenuInfo) menuInfo).recipient;
                RecipientsMenuClickListener l = new RecipientsMenuClickListener(c);

                menu.setHeaderTitle(c.getName());

                if (c.existsInDatabase()) {
                    menu.add(0, MENU_VIEW_CONTACT, 0, R.string.menu_view_contact)
                            .setOnMenuItemClickListener(l);
                /// M: Code analyze 043, Whether the address can be added to contacts app. @{
                } else if (MessageUtils.canAddToContacts(c)){
                /// @}
                    menu.add(0, MENU_ADD_TO_CONTACTS, 0, R.string.menu_add_to_contacts)
                            .setOnMenuItemClickListener(l);
                }
            }
        }
    };

    private final class RecipientsMenuClickListener implements MenuItem.OnMenuItemClickListener {
        private final Contact mRecipient;

        RecipientsMenuClickListener(Contact recipient) {
            mRecipient = recipient;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                // Context menu handlers for the recipients editor.
                case MENU_VIEW_CONTACT: {
                    Uri contactUri = mRecipient.getUri();
                    /// M: fix bug ALPS00448677, update or delete Contact Chip
                    mInViewContact = mRecipient;
                    /// @}
                    Intent intent = new Intent(Intent.ACTION_VIEW, contactUri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    startActivityForResult(intent, REQUEST_CODE_VIEW_CONTACT);
                    return true;
                }
                case MENU_ADD_TO_CONTACTS: {
                    /// M: fix bug ALPS00448677, update or delete Contact Chip
                    mInViewContact = null;
                    /// @}
                    mAddContactIntent = ConversationList.createAddContactIntent(
                            mRecipient.getNumber());
                    ComposeMessageActivity.this.startActivityForResult(mAddContactIntent,
                            REQUEST_CODE_ADD_CONTACT);
                    return true;
                }
            }
            return false;
        }
    }

    private boolean canAddToContacts(Contact contact) {
        // There are some kind of automated messages, like STK messages, that we don't want
        // to add to contacts. These names begin with special characters, like, "*Info".
        final String name = contact.getName();
        if (!TextUtils.isEmpty(contact.getNumber())) {
            char c = contact.getNumber().charAt(0);
            if (isSpecialChar(c)) {
                return false;
            }
        }
        if (!TextUtils.isEmpty(name)) {
            char c = name.charAt(0);
            if (isSpecialChar(c)) {
                return false;
            }
        }
        if (!(Mms.isEmailAddress(name) ||
                Telephony.Mms.isPhoneNumber(name) ||
                contact.isMe())) {
            return false;
        }
        return true;
    }

    private boolean isSpecialChar(char c) {
        return c == '*' || c == '%' || c == '$';
    }

    private void addPositionBasedMenuItems(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;

        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            MmsLog.e(TAG, "bad menuInfo");
            return;
        }
        final int position = info.position;

        addUriSpecificMenuItems(menu, v, position);
    }

    private Uri getSelectedUriFromMessageList(ListView listView, int position) {
        // If the context menu was opened over a uri, get that uri.
        MessageListItem msglistItem = (MessageListItem) listView.getChildAt(position);
        if (msglistItem == null) {
            // FIXME: Should get the correct view. No such interface in ListView currently
            // to get the view by position. The ListView.getChildAt(position) cannot
            // get correct view since the list doesn't create one child for each item.
            // And if setSelection(position) then getSelectedView(),
            // cannot get corrent view when in touch mode.
            return null;
        }

        TextView textView;
        CharSequence text = null;
        int selStart = -1;
        int selEnd = -1;

        //check if message sender is selected
        textView = (TextView) msglistItem.findViewById(R.id.text_view);
        if (textView != null) {
            text = textView.getText();
            selStart = textView.getSelectionStart();
            selEnd = textView.getSelectionEnd();
        }

        /// M: Code analyze 044,If sender is not being selected, it may be within the message body.@{
        if (selStart == -1) {
            textView = (TextView) msglistItem.findViewById(R.id.body_text_view);
            if (textView != null) {
                text = textView.getText();
                selStart = textView.getSelectionStart();
                selEnd = textView.getSelectionEnd();
            }
        }
        /// @}

        // Check that some text is actually selected, rather than the cursor
        // just being placed within the TextView.
        if (selStart != selEnd) {
            int min = Math.min(selStart, selEnd);
            int max = Math.max(selStart, selEnd);

            URLSpan[] urls = ((Spanned) text).getSpans(min, max,
                                                        URLSpan.class);

            if (urls.length == 1) {
                return Uri.parse(urls[0].getURL());
            }
        }

        //no uri was selected
        return null;
    }

    private void addUriSpecificMenuItems(ContextMenu menu, View v, int position) {
        Uri uri = getSelectedUriFromMessageList((ListView) v, position);

        if (uri != null) {
            Intent intent = new Intent(null, uri);
            intent.addCategory(Intent.CATEGORY_SELECTED_ALTERNATIVE);
            menu.addIntentOptions(0, 0, 0,
                    new android.content.ComponentName(this, ComposeMessageActivity.class),
                    null, intent, 0, null);
        }
    }

    private final void addCallAndContactMenuItems(
            ContextMenu menu, MsgListMenuClickListener l, MessageItem msgItem) {
        if (TextUtils.isEmpty(msgItem.mBody)) {
            return;
        }
        SpannableString msg = new SpannableString(msgItem.mBody);
        Linkify.addLinks(msg, Linkify.ALL);
        ArrayList<String> uris =
            MessageUtils.extractUris(msg.getSpans(0, msg.length(), URLSpan.class));
        /// M: Code analyze 022, Add bookmark. Clear the List.@{
        mURLs.clear();
        /// @}
        // Remove any dupes so they don't get added to the menu multiple times
        HashSet<String> collapsedUris = new HashSet<String>();
        for (String uri : uris) {
            collapsedUris.add(uri.toLowerCase());
        }
        for (String uriString : collapsedUris) {
            String prefix = null;
            int sep = uriString.indexOf(":");
            if (sep >= 0) {
                prefix = uriString.substring(0, sep);
                /// M: Code analyze 022, Add bookmark. @{
                if ("mailto".equalsIgnoreCase(prefix) || "tel".equalsIgnoreCase(prefix)) {
                    uriString = uriString.substring(sep + 1);
                }
                /// @}
            }
            Uri contactUri = null;
            boolean knownPrefix = true;
            if ("mailto".equalsIgnoreCase(prefix))  {
                contactUri = getContactUriForEmail(uriString);
            } else if ("tel".equalsIgnoreCase(prefix)) {
                contactUri = getContactUriForPhoneNumber(uriString);
            } else {
                knownPrefix = false;

                /// M: Code analyze 022, Add bookmark. Maybe exist multi URL address @{
                if (msgItem.isSms() && mURLs.size() <= 0) {
                    menu.add(0, MENU_ADD_TO_BOOKMARK, 0, R.string.menu_add_to_bookmark)
                    .setOnMenuItemClickListener(l);
                }
                /// @}

                /// M: Code analyze 001, Plugin opeartor(add  MMS URL to book mark). @{
                if (mMmsComposePlugin.isAddMmsUrlToBookMark() && msgItem.isMms() && mURLs.size() <= 0) {
                    menu.add(0, MENU_ADD_TO_BOOKMARK, 0, R.string.menu_add_to_bookmark)
                    .setOnMenuItemClickListener(l);
                }
                /// @}
                /// M: Code analyze 022, Add bookmark. @{
                mURLs.add(uriString);
                /// @}
            }
            if (knownPrefix && contactUri == null) {
                Intent intent = ConversationList.createAddContactIntent(uriString);

                String addContactString = getString(R.string.menu_add_address_to_contacts,
                        uriString);
                menu.add(0, MENU_ADD_ADDRESS_TO_CONTACTS, 0, addContactString)
                    .setOnMenuItemClickListener(l)
                    .setIntent(intent);
            }
        }
    }

    private Uri getContactUriForEmail(String emailAddress) {
        Cursor cursor = SqliteWrapper.query(this, getContentResolver(),
                Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(emailAddress)),
                new String[] { Email.CONTACT_ID, Contacts.DISPLAY_NAME }, null, null, null);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(1);
                    if (!TextUtils.isEmpty(name)) {
                        return ContentUris.withAppendedId(Contacts.CONTENT_URI, cursor.getLong(0));
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    private Uri getContactUriForPhoneNumber(String phoneNumber) {
        Contact contact = Contact.get(phoneNumber, true);
        if (contact.existsInDatabase()) {
            return contact.getUri();
        }
        return null;
    }

    private final OnCreateContextMenuListener mMsgListMenuCreateListener =
        new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            if (!isCursorValid()) {
                return;
            }
            Cursor cursor = mMsgListAdapter.getCursor();
            String type = cursor.getString(COLUMN_MSG_TYPE);
            long msgId = cursor.getLong(COLUMN_ID);

            MmsLog.i(TAG, "onCreateContextMenu(): msgId=" + msgId);
            addPositionBasedMenuItems(menu, v, menuInfo);

            MessageItem msgItem = mMsgListAdapter.getCachedMessageItem(type, msgId, cursor);
            if (msgItem == null) {
                MmsLog.e(TAG, "Cannot load message item for type = " + type
                        + ", msgId = " + msgId);
                return;
            }

            menu.setHeaderTitle(R.string.message_options);

            MsgListMenuClickListener l = new MsgListMenuClickListener(msgItem);

            if ((MmsConfig.isActivated(ComposeMessageActivity.this)
                    || MmsConfig.isServiceEnabled(ComposeMessageActivity.this)) && msgItem.mIpMessageId > 0) {
                int ipStatus = IpMessageUtils.getMessageManager(ComposeMessageActivity.this).getStatus(msgItem.mMsgId);
                if (ipStatus == IpMessageStatus.FAILED || ipStatus == IpMessageStatus.NOT_DELIVERED) {
                    IpMessage ipMessage = IpMessageUtils.getMessageManager(
                            ComposeMessageActivity.this).getIpMsgInfo(msgItem.mMsgId);
                    int commonType = canConvertIpMessageToMessage(ipMessage);
                    if (commonType == MESSAGETYPE_TEXT) {
                        menu.add(0, MENU_SEND_VIA_TEXT_MSG, 0,
                                    IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                                .getSingleString(IpMessageConsts.string.ipmsg_send_via_text_msg))
                                .setOnMenuItemClickListener(l);
                    } else if (commonType == MESSAGETYPE_MMS) {
                        menu.add(0, MENU_SEND_VIA_TEXT_MSG, 0,
                                    IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                                .getSingleString(IpMessageConsts.string.ipmsg_send_via_mms))
                                .setOnMenuItemClickListener(l);
                    }
                    menu.add(0, MENU_RETRY, 0,
                                    IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                                .getSingleString(IpMessageConsts.string.ipmsg_retry))
                                .setOnMenuItemClickListener(l);
                }
            }

            if (msgItem.isSms()) {
                if (msgItem.mIpMessageId > 0) {
                    IpMessage ipMessage = IpMessageUtils.getMessageManager(
                            ComposeMessageActivity.this).getIpMsgInfo(msgItem.mMsgId);
                    if (ipMessage != null && (ipMessage.getType() == IpMessageType.TEXT)) {
                        menu.add(0, MENU_COPY, 0, R.string.ipmsg_copy)
                                .setOnMenuItemClickListener(l);
                    }
                } else {
                    menu.add(0, MENU_COPY, 0, R.string.ipmsg_copy)
                    .setOnMenuItemClickListener(l);
                }
            }

            // Forward is not available for undownloaded messages.
            if (msgItem.isSms() || (msgItem.isDownloaded() && isForwardable(msgId))) {
                if (msgItem.mIpMessageId > 0) {
                    if (msgItem.mIpMessage instanceof IpAttachMessage) {
                        IpAttachMessage ipAttachMessage = (IpAttachMessage) msgItem.mIpMessage;
                        if (!ipAttachMessage.isInboxMsgDownloalable()) {
                            menu.add(0, MENU_FORWARD_IPMESSAGE, 0, R.string.menu_forward).setOnMenuItemClickListener(l);
                        }
                    } else {
                        menu.add(0, MENU_FORWARD_IPMESSAGE, 0, R.string.menu_forward).setOnMenuItemClickListener(l);
                    }
                } else {
                    menu.add(0, MENU_FORWARD_MESSAGE, 0, R.string.menu_forward).setOnMenuItemClickListener(l);
                }
            }

            if (msgItem.mIpMessageId > 0) {
                if (msgItem.mIpMessage instanceof IpAttachMessage) {
                    IpAttachMessage ipAttachMessage = (IpAttachMessage) msgItem.mIpMessage;
                    if (!ipAttachMessage.isInboxMsgDownloalable()) {
                        menu.add(0, MENU_SHARE, 0,
                             IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_share))
                        .setOnMenuItemClickListener(l);
                    }
                } else {
                    menu.add(0, MENU_SHARE, 0,
                            IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_share))
                        .setOnMenuItemClickListener(l);
                }
                menu.add(0, MENU_DELETE_IP_MESSAGE, 0,
                            IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_delete))
                        .setOnMenuItemClickListener(l);
            } else {
                menu.add(0, MENU_DELETE_MESSAGE, 0, R.string.delete_message)
                        .setOnMenuItemClickListener(l);
            }

            if (MmsConfig.getIpMessagServiceId(ComposeMessageActivity.this) > IpMessageServiceId.NO_SERVICE) {
                if (msgItem.mLocked) {
                    menu.add(0, MENU_REMOVE_FROM_IMPORTANT, 0,
                                IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_remove_from_important))
                            .setOnMenuItemClickListener(l);
                } else {
                    menu.add(0, MENU_MARK_AS_IMPORTANT, 0,
                                IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_mark_as_important))
                            .setOnMenuItemClickListener(l);
                }
            } else {
                if (msgItem.mLocked) {
                    menu.add(0, MENU_UNLOCK_MESSAGE, 0, R.string.menu_unlock)
                            .setOnMenuItemClickListener(l);
                } else {
                    menu.add(0, MENU_LOCK_MESSAGE, 0, R.string.menu_lock)
                            .setOnMenuItemClickListener(l);
                }
            }

            if (msgItem.isMms()) {
                switch (msgItem.mAttachmentType) {
                    case WorkingMessage.SLIDESHOW:
                        menu.add(0, MENU_VIEW_SLIDESHOW, 0, R.string.view_slideshow)
                                .setOnMenuItemClickListener(l);
                        if (haveSomethingToCopyToSDCard(msgItem.mMsgId)) {
                            menu.add(0, MENU_COPY_TO_SDCARD, 0, R.string.copy_to_sdcard)
                                    .setOnMenuItemClickListener(l);
                        }
                        break;
                    default:
                        if (haveSomethingToCopyToSDCard(msgItem.mMsgId)) {
                            menu.add(0, MENU_COPY_TO_SDCARD, 0, R.string.copy_to_sdcard)
                                    .setOnMenuItemClickListener(l);
                        }
                        break;
                }
            }

            addCallAndContactMenuItems(menu, l, msgItem);

            menu.add(0, MENU_VIEW_MESSAGE_DETAILS, 0, R.string.view_message_details)
                .setOnMenuItemClickListener(l);

            if (msgItem.mDeliveryStatus != MessageItem.DeliveryStatus.NONE || msgItem.mReadReport) {
                menu.add(0, MENU_DELIVERY_REPORT, 0, R.string.view_delivery_report)
                        .setOnMenuItemClickListener(l);
            }

            /// M: Code analyze 016, Add for select text copy. @{
            if (!TextUtils.isEmpty(msgItem.mBody)
                    && !IpMessageUtils.getIpMessagePlugin(ComposeMessageActivity.this).isActualPlugin()
                    && msgItem.mMessageType != PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) {
                MmsLog.i(TAG, "onCreateContextMenu(): add select text menu");
                menu.add(0, MENU_SELECT_TEXT, 0, R.string.select_text)
                        .setOnMenuItemClickListener(l);
            }
            /// @}

            if ((MmsConfig.isActivated(ComposeMessageActivity.this) || MmsConfig
                    .isServiceEnabled(ComposeMessageActivity.this))
                && msgItem.mIpMessageId > 0) {
                int ipMsgType = IpMessageUtils.getMessageManager(ComposeMessageActivity.this).getType(msgItem.mMsgId);
                if (ipMsgType >= IpMessageType.PICTURE && ipMsgType < IpMessageType.UNKNOWN_FILE
                    && ipMsgType != IpMessageType.LOCATION) {
                    MmsLog.d(IPMSG_TAG, "onCreateContextMenu(): add MENU_EXPORT_SD_CARD. msgId = " + msgItem.mMsgId
                        + ", type = " + ipMsgType);
                    menu.add(0, MENU_EXPORT_SD_CARD, 0,
                            IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_export_to_sdcard))
                        .setOnMenuItemClickListener(l);
                }
            }

            if (msgItem.isSms() && msgItem.mIpMessageId <= 0) {
                if (mSimCount > 0 && !msgItem.isSending()) {
                    menu.add(0, MENU_SAVE_MESSAGE_TO_SIM, 0, R.string.save_message_to_sim)
                            .setOnMenuItemClickListener(l);
                }
            }
        }
    };

    private void editMessageItem(MessageItem msgItem) {
        if ("sms".equals(msgItem.mType)) {
            editSmsMessageItem(msgItem);
        } else {
            editMmsMessageItem(msgItem);
            mWorkingMessage.setHasMmsDraft(true);
        }
        /// M: @{
        if ((msgItem.isFailedMessage() || msgItem.isSending()) && mMsgListAdapter.getCount() <= 1) {
            // For messages with bad addresses, let the user re-edit the recipients.
            initRecipientsEditor(null);
            /// M: Code analyze 046, Whether the recipientedit control has been initialized. @{
            isInitRecipientsEditor = true;
            /// @}
            mMsgListAdapter.changeCursor(null);
            invalidateOptionsMenu();
        }
    }

    private void editSmsMessageItem(MessageItem msgItem) {
        // When the message being edited is the only message in the conversation, the delete
        // below does something subtle. The trigger "delete_obsolete_threads_pdu" sees that a
        // thread contains no messages and silently deletes the thread. Meanwhile, the mConversation
        // object still holds onto the old thread_id and code thinks there's a backing thread in
        // the DB when it really has been deleted. Here we try and notice that situation and
        // clear out the thread_id. Later on, when Conversation.ensureThreadId() is called, we'll
        // create a new thread if necessary.
        synchronized (mConversation) {
            /// M: @{
            //if (mConversation.getMessageCount() <= 1) {
            if (mMsgListAdapter.getCursor().getCount() <= 1) {
            /// @}
                mConversation.clearThreadId();
                MessagingNotification.setCurrentlyDisplayedThreadId(
                    MessagingNotification.THREAD_NONE);
            }
        }
        // Delete the old undelivered SMS and load its content.
        Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, msgItem.mMsgId);
        SqliteWrapper.delete(ComposeMessageActivity.this,
                mContentResolver, uri, null, null);

        mWorkingMessage.setConversation(mConversation);
        mWorkingMessage.setText(msgItem.mBody);
    }

    private void editMmsMessageItem(MessageItem msgItem) {
        /// M: Code analyze 026, If the two clicks are too close. @{
        if (isTwoClickClose(clickTime,500)) {
            return ;
        }
        /// @}

        /// M: Discard the current message in progress.
        mWorkingMessage.discard();

        // Load the selected message in as the working message.
        WorkingMessage newWorkingMessage = WorkingMessage.load(this, msgItem.mMessageUri);
        if (newWorkingMessage == null) {
            MmsLog.e(TAG, "editMmsMessageItem, load returns null message");
            return;
        }
        mWorkingMessage = newWorkingMessage;
        mWorkingMessage.setConversation(mConversation);
        invalidateOptionsMenu();
        /// M: @{
        mAttachmentEditor.update(mWorkingMessage);
        updateTextEditorHeightInFullScreen();
        /// @}
        drawTopPanel(false);

        // WorkingMessage.load() above only loads the slideshow. Set the
        // subject here because we already know what it is and avoid doing
        // another DB lookup in load() just to get it.
        mWorkingMessage.setSubject(msgItem.mSubject, false);

        if (mWorkingMessage.hasSubject()) {
            showSubjectEditor(true);
        }

        /// M: fix bug ALPS00433858, update read==1(readed) when reload failed-mms
        final MessageItem item = msgItem;
        new Thread(new Runnable() {
            public void run() {
                // TODO Auto-generated method stub
                Uri uri = ContentUris.withAppendedId(Mms.CONTENT_URI, item.mMsgId);
                ContentValues values = new ContentValues(1);
                values.put(Mms.READ, 1);
                SqliteWrapper.update(ComposeMessageActivity.this,
                        mContentResolver, uri, values, null, null);
            }
        }).start();
    }

    private void copyToClipboard(String str) {
        ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, str));
    }

    private void forwardMessage(final MessageItem msgItem) {
        /// M: Code analyze 051, Hide input keyboard (add for input method covered Compose UI issue).@{
        hideInputMethod();
        /// @}

        mTempThreadId = 0;
        // The user wants to forward the message. If the message is an mms message, we need to
        // persist the pdu to disk. This is done in a background task.
        // If the task takes longer than a half second, a progress dialog is displayed.
        // Once the PDU persisting is done, another runnable on the UI thread get executed to start
        // the ForwardMessageActivity.
        getAsyncDialog().runAsync(new Runnable() {
            @Override
            public void run() {
                // This runnable gets run in a background thread.
                if (msgItem.mType.equals("mms")) {
                    SendReq sendReq = new SendReq();
                    String subject = getString(R.string.forward_prefix);
                    if (msgItem.mSubject != null) {
                        subject += msgItem.mSubject;
                    }
                    sendReq.setSubject(new EncodedStringValue(subject));
                    sendReq.setBody(msgItem.mSlideshow.makeCopy());

                    mTempMmsUri = null;
                    try {
                        PduPersister persister =
                                PduPersister.getPduPersister(ComposeMessageActivity.this);
                        // Copy the parts of the message here.
                        /// M: google jb.mr1 patch, group mms
                        mTempMmsUri = persister.persist(sendReq, Mms.Draft.CONTENT_URI, true,
                                MmsPreferenceActivity
                                    .getIsGroupMmsEnabled(ComposeMessageActivity.this));
                        mTempThreadId = MessagingNotification.getThreadId(
                                ComposeMessageActivity.this, mTempMmsUri);
                    } catch (MmsException e) {
                        MmsLog.e(TAG, "Failed to copy message: " + msgItem.mMessageUri);
                        Toast.makeText(ComposeMessageActivity.this,
                                R.string.cannot_save_message, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
        }, new Runnable() {
            @Override
            public void run() {
                // Once the above background thread is complete, this runnable is run
                // on the UI thread.
                Intent intent = createIntent(ComposeMessageActivity.this, 0);
                if (MmsConfig.isNeedExitComposerAfterForward()) {
                    intent.putExtra("exit_on_sent", true);
                }
                intent.putExtra("forwarded_message", true);
                if (mTempThreadId > 0) {
                    intent.putExtra("thread_id", mTempThreadId);
                }

                if (msgItem.mType.equals("sms")) {
                    /// M: Code analyze 001, Plugin opeartor. @{
                    String smsBody = msgItem.mBody;
                    if (mMmsComposePlugin.isAppendSender()) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ComposeMessageActivity.this);
                        boolean smsForwardWithSender = prefs.getBoolean(SmsPreferenceActivity.SMS_FORWARD_WITH_SENDER, true);
                        MmsLog.d(TAG, "forwardMessage(): SMS Forward With Sender ?= " + smsForwardWithSender);
                        if (smsForwardWithSender) {
                            if (msgItem.mBoxId == Mms.MESSAGE_BOX_INBOX) {
                                smsBody += "\n" + getString(R.string.forward_from);
                                Contact contact = Contact.get(msgItem.mAddress, false);
                                MmsLog.d(TAG, "forwardMessage(): Contact's name and number="
                                        + Contact.formatNameAndNumber(contact.getName(), contact.getNumber(), ""));
                                smsBody += Contact.formatNameAndNumber(contact.getName(), contact.getNumber(), "");
                            }
                        }
                    }
                    intent.putExtra("sms_body", smsBody);
                    /// @}
                } else {
                    intent.putExtra("msg_uri", mTempMmsUri);
                    String subject = getString(R.string.forward_prefix);
                    if (msgItem.mSubject != null) {
                        subject += msgItem.mSubject;
                    }
                    intent.putExtra("subject", subject);
                }
                // ForwardMessageActivity is simply an alias in the manifest for
                // ComposeMessageActivity. We have to make an alias because ComposeMessageActivity
                // launch flags specify singleTop. When we forward a message, we want to start a
                // separate ComposeMessageActivity. The only way to do that is to override the
                // singleTop flag, which is impossible to do in code. By creating an alias to the
                // activity, without the singleTop flag, we can launch a separate
                // ComposeMessageActivity to edit the forward message.
                intent.setClassName(ComposeMessageActivity.this,
                        "com.android.mms.ui.ForwardMessageActivity");
                startActivityForResult(intent,REQUEST_CODE_FOR_FORWARD);
            }
        }, R.string.sync_mms_to_db);
    }

    /**
     * Context menu handlers for the message list view.
     */
    private final class MsgListMenuClickListener implements MenuItem.OnMenuItemClickListener {
        private MessageItem mMsgItem;

        public MsgListMenuClickListener(MessageItem msgItem) {
            mMsgItem = msgItem;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            if (mMsgItem == null) {
                return false;
            }
            if (mMsgItem.mIpMessageId > 0 && onIpMessageMenuItemClick(item, mMsgItem)) {
                return true;
            }

            switch (item.getItemId()) {
                case MENU_EDIT_MESSAGE:
                    editMessageItem(mMsgItem);
                    drawBottomPanel();
                    return true;
                /// M: for ipmessage copy
                case MENU_COPY:
                case MENU_COPY_MESSAGE_TEXT:
                    /// M: Code analyze 023, Delete the char value of '\r' . @{
                    //  copyToClipboard(msgItem.mBody);
                    if (mMsgItem.mBody != null) {
                        String copyBody = mMsgItem.mBody.replaceAll(STR_RN, STR_CN);
                        copyToClipboard(copyBody);
                    } else {
                        MmsLog.i(TAG, "onMenuItemClick, mMsgItem.mBody == null");
                        return false;
                    }
                    /// @}
                    return true;

                case MENU_FORWARD_MESSAGE:
                    /// M: @{
                    final MessageItem mRestrictedItem = mMsgItem;
                    if (WorkingMessage.sCreationMode == 0 ||
                      !MessageUtils.isRestrictedType(ComposeMessageActivity.this,mMsgItem.mMsgId)) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                forwardMessage(mRestrictedItem);
                            }
                        });
                    } else if (WorkingMessage.sCreationMode == WorkingMessage.WARNING_TYPE) {
                        new AlertDialog.Builder(ComposeMessageActivity.this)
                        .setTitle(R.string.restricted_forward_title)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setMessage(R.string.restricted_forward_message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    int createMode = WorkingMessage.sCreationMode;
                                    WorkingMessage.sCreationMode = 0;
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            forwardMessage(mRestrictedItem);
                                        }
                                    });
                                    WorkingMessage.sCreationMode = createMode;
                                }
                            })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                    }
                    /// @}
                    return true;

                case MENU_VIEW_SLIDESHOW:
                    /// M: Code analyze 024, If the click operator can be responsed. @{
                    if (mClickCanResponse) {
                        mClickCanResponse = false;
                        MessageUtils.viewMmsMessageAttachment(ComposeMessageActivity.this,
                                ContentUris.withAppendedId(Mms.CONTENT_URI, mMsgItem.mMsgId), null,
                                getAsyncDialog());
                    return true;
                    }
                    /// @}
                case MENU_VIEW_MESSAGE_DETAILS:
                    return showMessageDetails(mMsgItem);

                case MENU_DELETE_MESSAGE: {
                    DeleteMessageListener l = new DeleteMessageListener(mMsgItem);
                    
                    /// M: Code analyze 027,Add for deleting one message.@{
                    // (Why only query sms table?)
                    String where = Telephony.Mms._ID + "=" + mMsgItem.mMsgId;
                    String[] projection = new String[] { Sms.Inbox.THREAD_ID };
                    MmsLog.d(TAG, "where:" + where);
                    Cursor queryCursor = Sms.query(getContentResolver(),// query uri: content://sms
                            projection, where, null);
                    if (queryCursor.moveToFirst()) {
                        mThreadId = queryCursor.getLong(0);
                    }
                    if (queryCursor != null) {
                        queryCursor.close();
                    }
                    /// @}
                    confirmDeleteDialog(l, mMsgItem.mLocked);
                    return true;
                }
                case MENU_DELIVERY_REPORT:
                    showDeliveryReport(mMsgItem.mMsgId, mMsgItem.mType);
                    return true;

                case MENU_COPY_TO_SDCARD: {
                    /// M: Code analyze 021, Copy all valid parts of the attachment(pdu) to SD card.
                    /// This opeartor will be removed to a separate activity.  @{
                    /// M: new feature, change default disk when storage is full
                    EncapsulatedStorageManager storageManager =
                       new EncapsulatedStorageManager(getApplicationContext());
                    long availSize = MessageUtils.getAvailableBytesInFileSystemAtGivenRoot
                                                    (EncapsulatedStorageManager.getDefaultPath());
                    boolean haveExSD = MessageUtils.existingSD(storageManager, true);
                    boolean haveInSD = MessageUtils.existingSD(storageManager, false);

                    /// M: fix bug ALPS00574679, modify toast string when haven't SD Card @{
                    if (!haveExSD && !haveInSD) {
                        Toast.makeText(ComposeMessageActivity.this,
                                getString(R.string.no_sdcard_suggestion),
                                Toast.LENGTH_LONG).show();
                        return false;
                    }
                    /// @}

                    if (mMsgItem.mMessageSize > availSize) {
                        if ((haveInSD && !haveExSD) || (!haveInSD && haveExSD)) {
                            Toast.makeText(ComposeMessageActivity.this,
                                    getString(R.string.export_disk_problem),
                                    Toast.LENGTH_LONG).show();
                            return false;
                        } else {
                            new AlertDialog.Builder(ComposeMessageActivity.this)
                            .setTitle(R.string.copy_to_sdcard_fail)
                            .setIconAttribute(android.R.attr.alertDialogIcon)
                            .setMessage(R.string.change_default_disk)
                            .setPositiveButton(R.string.change, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent("android.settings.MEMORY_CARD_SETTINGS");
                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                        }
                    } else {
                        Intent i = new Intent(ComposeMessageActivity.this, MultiSaveActivity.class);
                        i.putExtra("msgid", mMsgItem.mMsgId);
                        IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin =
                                        (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(
                                                                MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);

                        MmsLog.i(TAG, "mMmsAttachmentEnhancePlugin = " + mMmsAttachmentEnhancePlugin);
                        if (mMmsAttachmentEnhancePlugin != null) {
                            mMmsAttachmentEnhancePlugin.setSaveAttachIntent(i,IMmsAttachmentEnhance.MMS_SAVE_ALL_ATTACHMENT);
                            MmsLog.i(TAG, "mMmsAttachmentEnhancePlugin.setSaveAttachIntent");
                        }
                        startActivityForResult(i, REQUEST_CODE_MULTI_SAVE);
                        // @}
                     }
                    return true;
                }

                case MENU_SAVE_RINGTONE: {
                    int resId = getDrmMimeSavedStringRsrc(mMsgItem.mMsgId,
                            saveRingtone(mMsgItem.mMsgId));
                    Toast.makeText(ComposeMessageActivity.this, resId, Toast.LENGTH_SHORT).show();
                    return true;
                }
                /// M: for mark as important
                case MENU_MARK_AS_IMPORTANT:
                case MENU_LOCK_MESSAGE: {
                    lockMessage(mMsgItem, true);
                    return true;
                }
                case MENU_REMOVE_FROM_IMPORTANT:
                case MENU_UNLOCK_MESSAGE: {
                    lockMessage(mMsgItem, false);
                    return true;
                }
                
                /// M: Code analyze 022, Add bookmark. Maybe exist multi URL addresses. @{
                case MENU_ADD_TO_BOOKMARK: {
                    if (mURLs.size() == 1) {
                        Browser.saveBookmark(ComposeMessageActivity.this, null, mURLs.get(0));
                    } else if (mURLs.size() > 1) {
                        CharSequence[] items = new CharSequence[mURLs.size()];
                        for (int i = 0; i < mURLs.size(); i++) {
                            items[i] = mURLs.get(i);
                        }
                        new AlertDialog.Builder(ComposeMessageActivity.this)
                            .setTitle(R.string.menu_add_to_bookmark)
                            .setIcon(com.mediatek.R.drawable.ic_dialog_menu_generic)
                            .setItems(items, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Browser.saveBookmark(ComposeMessageActivity.this, null, mURLs.get(which));
                                    }
                                })
                            .show();
                    }
                    return true;
                }

                /// M: Code analyze 007, Get information from Sim or save message to Sim. @{
                case MENU_SAVE_MESSAGE_TO_SIM: {
                    mSaveMsgThread = new SaveMsgThread(mMsgItem.mType, mMsgItem.mMsgId);
                    mSaveMsgThread.start();
                    return true;
                }
                /// @}

                /// M: Code analyze 016, Add for select text copy. @{
                case MENU_SELECT_TEXT: {
                    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                    MmsLog.i(TAG, "onMenuItemClick(): info.position = " + info.position);
                    mMsgListAdapter.getItemId(info.position);
                    MessageListItem msglistItem = (MessageListItem) info.targetView;
                    if (msglistItem != null) {
                        MmsLog.i(TAG, "msglistItem != null");
                        TextView textView = (TextView) msglistItem.findViewById(R.id.text_view);
                        AlertDialog.Builder builder = new AlertDialog.Builder(ComposeMessageActivity.this);
                        LayoutInflater factory = LayoutInflater.from(builder.getContext());
                        final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
                        EditText contentSelector = (EditText)textEntryView.findViewById(R.id.content_selector);
                        contentSelector.setText(textView.getText());
                        builder.setTitle(R.string.select_text)
                               .setView(textEntryView)
                               .setPositiveButton(R.string.yes, null)
                               .show();
                    }
                    return true;
                }
                case MENU_ADD_ADDRESS_TO_CONTACTS: {
                    mAddContactIntent = item.getIntent();
                    startActivityForResult(mAddContactIntent, REQUEST_CODE_ADD_CONTACT);
                    return true;
                }
                /// @}
                
                default:
                    return false;
            }
        }
    }

    private void lockMessage(final MessageItem msgItem, final boolean locked) {
        Uri uri;
        if ("sms".equals(msgItem.mType)) {
            uri = Sms.CONTENT_URI;
        } else {
            uri = Mms.CONTENT_URI;
        }
        final Uri lockUri = ContentUris.withAppendedId(uri, msgItem.mMsgId);

        final ContentValues values = new ContentValues(1);
        values.put("locked", locked ? 1 : 0);

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (locked) {
                    IpMessageUtils.getMessageManager(ComposeMessageActivity.this)
                        .addMessageToImportantList(new long[] {msgItem.mMsgId});
                } else {
                    IpMessageUtils.getMessageManager(ComposeMessageActivity.this)
                        .deleteMessageFromImportantList(new long[] {msgItem.mMsgId});
                }
                getContentResolver().update(lockUri, values, null, null);
            }
        }, "ComposeMessageActivity.lockMessage").start();
    }

    /**
     * Looks to see if there are any valid parts of the attachment that can be copied to a SD card.
     * @param msgId
     */
    private boolean haveSomethingToCopyToSDCard(long msgId) {
        PduBody body = null;
        try {
            body = SlideshowModel.getPduBody(this,
                        ContentUris.withAppendedId(Mms.CONTENT_URI, msgId));
        } catch (MmsException e) {
            MmsLog.e(TAG, "haveSomethingToCopyToSDCard can't load pdu body: " + msgId);
        }
        if (body == null) {
            return false;
        }

        boolean result = false;
        int partNum = body.getPartsNum();
        for (int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            // M: fix bug ALPS00355917
            byte[] fileName = part.getFilename();
            String mSrc = null;
            if (fileName == null) {
                fileName = part.getContentLocation();
            }
            if (fileName != null) {
                mSrc = new String(fileName);
            }
            String type =  MessageUtils.getContentType(new String(part.getContentType()),mSrc);
            part.setContentType(type.getBytes());
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                log("[CMA] haveSomethingToCopyToSDCard: part[" + i + "] contentType=" + type);
            }
            /// M: Code analyze 057,Now, if the pdu type is "application/ogg" or
            /// vcard and vcalender attachment can be saved to sdcard.
            if (EncapsulatedContentType.isImageType(type) || EncapsulatedContentType.isVideoType(type) ||
                    EncapsulatedContentType.isAudioType(type) || DrmUtils.isDrmType(type) ||
                    "application/ogg".equalsIgnoreCase(type) || FileAttachmentModel.isSupportedFile(part)
                    /// M: fix bug ALPS00446644, support dcf (0ct-stream) file to save
                    || (mSrc != null && mSrc.toLowerCase().endsWith(".dcf"))) {
            /// @}
                result = true;
                break;
            }
        }


             ///M: add for attachment enhance

             // Justify weather there are attachments in parts but not in slides
             //SlideshowModel mSlideShowModel = mWorkingMessage.getSlideshow();
            
            IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin =
                                    (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(
                                            MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);

            if (mMmsAttachmentEnhancePlugin != null) {
                if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance()) {
                    //OP01
                    SlideshowModel mSlideShow = null;
                    try {
                         mSlideShow = SlideshowModel.createFromPduBody(this, body);
                    } catch (MmsException e) {
                         MmsLog.e(TAG, "Create from pdubody exception!");
                    }

                    if (mSlideShow != null) {
                        MmsLog.i(TAG, "YF: mSlideShowModel != null");
                        if (mSlideShow.getAttachFiles() != null) {
                            MmsLog.i(TAG, "YF: mSlideShowModel.getAttachFiles() != null");
                            if (mSlideShow.getAttachFiles().size() != 0) {
                                result = true;
                            }
                        }
                    }
                }
            }
        return result;
    }

    /**
     * Copies media from an Mms to the DrmProvider
     * @param msgId
     */
    private boolean saveRingtone(long msgId) {
        boolean result = true;
        PduBody body = null;
        try {
            body = SlideshowModel.getPduBody(this,
                        ContentUris.withAppendedId(Mms.CONTENT_URI, msgId));
        } catch (MmsException e) {
            MmsLog.e(TAG, "copyToDrmProvider can't load pdu body: " + msgId);
        }
        if (body == null) {
            return false;
        }

        int partNum = body.getPartsNum();
        for (int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            String type = new String(part.getContentType());

            if (DrmUtils.isDrmType(type)) {
                // All parts (but there's probably only a single one) have to be successful
                // for a valid result.
                result &= copyPart(part, Long.toHexString(msgId));
            }
        }
        return result;
    }

    /**
     * Returns true if any part is drm'd audio with ringtone rights.
     * @param msgId
     * @return true if one of the parts is drm'd audio with rights to save as a ringtone.
     */
    private boolean isDrmRingtoneWithRights(long msgId) {
        PduBody body = null;
        try {
            body = SlideshowModel.getPduBody(this,
                        ContentUris.withAppendedId(Mms.CONTENT_URI, msgId));
        } catch (MmsException e) {
            MmsLog.e(TAG, "isDrmRingtoneWithRights can't load pdu body: " + msgId);
        }
        if (body == null) {
            return false;
        }

        int partNum = body.getPartsNum();
        for (int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            String type = new String(part.getContentType());

            if (DrmUtils.isDrmType(type)) {
                String mimeType = MmsApp.getApplication().getDrmManagerClient()
                        .getOriginalMimeType(part.getDataUri());
                if (EncapsulatedContentType.isAudioType(mimeType) && DrmUtils.haveRightsForAction(part.getDataUri(),
                        EncapsulatedDrmStore.EncapsulatedAction.RINGTONE)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if all drm'd parts are forwardable.
     * @param msgId
     * @return true if all drm'd parts are forwardable.
     */
    private boolean isForwardable(long msgId) {
        PduBody body = null;
        try {
            body = SlideshowModel.getPduBody(this,
                        ContentUris.withAppendedId(Mms.CONTENT_URI, msgId));
        } catch (MmsException e) {
            MmsLog.e(TAG, "getDrmMimeType can't load pdu body: " + msgId);
        }
        if (body == null) {
            return false;
        }

        int partNum = body.getPartsNum();
        for (int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            String type = new String(part.getContentType());

            if (DrmUtils.isDrmType(type) && !DrmUtils.haveRightsForAction(part.getDataUri(),
                    EncapsulatedDrmStore.EncapsulatedAction.TRANSFER)) {
                    return false;
            }
        }
        return true;
    }

    private int getDrmMimeMenuStringRsrc(long msgId) {
        if (isDrmRingtoneWithRights(msgId)) {
            return R.string.save_ringtone;
        }
        return 0;
    }

    private int getDrmMimeSavedStringRsrc(long msgId, boolean success) {
        if (isDrmRingtoneWithRights(msgId)) {
            return success ? R.string.saved_ringtone : R.string.saved_ringtone_fail;
        }
        return 0;
    }

    /**
     * Copies media from an Mms to the "download" directory on the SD card. If any of the parts
     * are audio types, drm'd or not, they're copied to the "Ringtones" directory.
     * @param msgId
     */
    private boolean copyMedia(long msgId) {
        boolean result = true;
        PduBody body = null;
        try {
            body = SlideshowModel.getPduBody(this,
                        ContentUris.withAppendedId(Mms.CONTENT_URI, msgId));
        } catch (MmsException e) {
            MmsLog.e(TAG, "copyMedia can't load pdu body: " + msgId);
        }
        if (body == null) {
            return false;
        }

        int partNum = body.getPartsNum();
        for (int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);

            // all parts have to be successful for a valid result.
            result &= copyPart(part, Long.toHexString(msgId));
        }
        return result;
    }

    private boolean copyPart(PduPart part, String fallback) {
        Uri uri = part.getDataUri();
        String type = new String(part.getContentType());
        boolean isDrm = DrmUtils.isDrmType(type);
        if (isDrm) {
            type = MmsApp.getApplication().getDrmManagerClient()
                    .getOriginalMimeType(part.getDataUri());
        }
        if (!EncapsulatedContentType.isImageType(type) && !EncapsulatedContentType.isVideoType(type) &&
                !EncapsulatedContentType.isAudioType(type)) {
            return true;    // we only save pictures, videos, and sounds. Skip the text parts,
                            // the app (smil) parts, and other type that we can't handle.
                            // Return true to pretend that we successfully saved the part so
                            // the whole save process will be counted a success.
        }
        InputStream input = null;
        FileOutputStream fout = null;
        try {
            input = mContentResolver.openInputStream(uri);
            if (input instanceof FileInputStream) {
                FileInputStream fin = (FileInputStream) input;

                byte[] location = part.getName();
                if (location == null) {
                    location = part.getFilename();
                }
                if (location == null) {
                    location = part.getContentLocation();
                }

                String fileName;
                if (location == null) {
                    // Use fallback name.
                    fileName = fallback;
                } else {
                    // For locally captured videos, fileName can end up being something like this:
                    //      /mnt/sdcard/Android/data/com.android.mms/cache/.temp1.3gp
                    fileName = new String(location);
                }
                File originalFile = new File(fileName);
                fileName = originalFile.getName();  // Strip the full path of where the "part" is
                                                    // stored down to just the leaf filename.

                // Depending on the location, there may be an
                // extension already on the name or not. If we've got audio, put the attachment
                // in the Ringtones directory.
                String dir = Environment.getExternalStorageDirectory() + "/"
                                + (EncapsulatedContentType.isAudioType(type) ? Environment.DIRECTORY_RINGTONES :
                                    Environment.DIRECTORY_DOWNLOADS)  + "/";
                String extension;
                int index = fileName.lastIndexOf('.');
                if (index == -1) {
                    extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
                } else {
                    extension = fileName.substring(index + 1, fileName.length());
                    fileName = fileName.substring(0, index);
                }
                MmsLog.i(TAG, "Save part extension name is: " + extension);
                if (isDrm) {
                    extension += DrmUtils.getConvertExtension(type);
                }
                File file = getUniqueDestination(dir + fileName, extension);

                // make sure the path is valid and directories created for this file.
                File parentFile = file.getParentFile();
                if (!parentFile.exists() && !parentFile.mkdirs()) {
                    MmsLog.e(TAG, "[MMS] copyPart: mkdirs for " + parentFile.getPath() + " failed!");
                    return false;
                }

                fout = new FileOutputStream(file);

                byte[] buffer = new byte[8000];
                int size = 0;
                while ((size = fin.read(buffer)) != -1) {
                    fout.write(buffer, 0, size);
                }

                // Notify other applications listening to scanner events
                // that a media file has been added to the sd card
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(file)));
            }
        } catch (IOException e) {
            // Ignore
            MmsLog.e(TAG, "IOException caught while opening or reading stream", e);
            return false;
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore
                    MmsLog.e(TAG, "IOException caught while closing stream", e);
                    return false;
                }
            }
            if (null != fout) {
                try {
                    fout.close();
                } catch (IOException e) {
                    // Ignore
                    MmsLog.e(TAG, "IOException caught while closing stream", e);
                    return false;
                }
            }
        }
        return true;
    }

    private File getUniqueDestination(String base, String extension) {
        File file = new File(base + "." + extension);

        for (int i = 2; file.exists(); i++) {
            file = new File(base + "_" + i + "." + extension);
        }
        return file;
    }

    private void showDeliveryReport(long messageId, String type) {
        Intent intent = new Intent(this, DeliveryReportActivity.class);
        intent.putExtra("message_id", messageId);
        intent.putExtra("message_type", type);

        startActivity(intent);
    }

    private final IntentFilter mHttpProgressFilter = new IntentFilter(PROGRESS_STATUS_ACTION);

    private final BroadcastReceiver mHttpProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (PROGRESS_STATUS_ACTION.equals(intent.getAction())) {
                long token = intent.getLongExtra("token",
                                    SendingProgressTokenManager.NO_TOKEN);
                if (token != mConversation.getThreadId()) {
                    return;
                }

                int progress = intent.getIntExtra("progress", 0);
                switch (progress) {
                    case PROGRESS_START:
                        setProgressBarVisibility(true);
                        break;
                    case PROGRESS_ABORT:
                    case PROGRESS_COMPLETE:
                        setProgressBarVisibility(false);
                        break;
                    default:
                        setProgress(100 * progress);
                }
            }
        }
    };

    private static ContactList sEmptyContactList;

    private ContactList getRecipients() {
        // If the recipients editor is visible, the conversation has
        // not really officially 'started' yet.  Recipients will be set
        // on the conversation once it has been saved or sent.  In the
        // meantime, let anyone who needs the recipient list think it
        // is empty rather than giving them a stale one.
        if (isRecipientsEditorVisible()) {
            if (sEmptyContactList == null) {
                sEmptyContactList = new ContactList();
            }
            return sEmptyContactList;
        }
        return mConversation.getRecipients();
    }

    private void updateTitle(ContactList list) {
        String title = null;
        String subTitle = null;
        Drawable avatarIcon = null;
        int cnt = list.size();
        MmsLog.d(TAG, "updateTitle(): list.size()" + list.size());
        switch (cnt) {
            case 0: {
                String recipient = null;
                if (mRecipientsEditor != null) {
                    recipient = mRecipientsEditor.getText().toString();
                    recipient = recipient.replaceAll(";", ",").trim();
                }
                /// M: Code analyze 045,Set title and subtitle when there is no contact exists.@{
                if (TextUtils.isEmpty(recipient) || "".equals(recipient.replaceAll(",", "").trim())) {
                    title = getString(R.string.new_message);
                } else {
                    /// M: remove trailing separtors
                    if (recipient.endsWith(", ") || recipient.endsWith(",")) {
                        title = recipient.substring(0, recipient.lastIndexOf(","));
                    } else {
                        title = recipient;
                    }
                    final int c = mRecipientsEditor.getRecipientCount();
                    if (c > 1) {
                        subTitle = getResources().getQuantityString(R.plurals.recipient_count, c, c);
                    }
                }
                /// @}
                break;
            }
            case 1: {
                title = list.get(0).getName();      // get name returns the number if there's no
                                                    // name available.
                Drawable sDefaultContactImage = this.getResources().getDrawable(
                    R.drawable.ic_contact_picture);
                avatarIcon = list.get(0).getAvatar(this, sDefaultContactImage,
                                                    mConversation != null ? mConversation.getThreadId() : -1);
                String number = list.get(0).getNumber();
                String numberAfterFormat = MessageUtils.formatNumber(number,
                        this.getApplicationContext());
                if (!title.equals(number) && !title.equals(numberAfterFormat)) {
                    subTitle = numberAfterFormat;
                }
                /// M: fix bug ALPS00488976, group mms @{
                if (mMsgListAdapter.isGroupConversation()) {
                    mMsgListAdapter.setIsGroupConversation(false);
                }
                /// @}
                break;
            }
            default: {
                // Handle multiple recipients
                title = list.formatNames(", ");
                subTitle = getResources().getQuantityString(R.plurals.recipient_count, cnt, cnt);
                break;
            }
        }
        mDebugRecipients = list.serialize();

        ActionBar actionBar = getActionBar();
        if (avatarIcon != null) {
            actionBar.setIcon(avatarIcon);
        }

        actionBar.setCustomView(R.layout.actionbar_message_title);
        mActionBarCustomView = actionBar.getCustomView();
        mTopTitle = (TextView) mActionBarCustomView.findViewById(R.id.tv_top_title);
        mTopSubtitle = (TextView) mActionBarCustomView.findViewById(R.id.tv_top_subtitle);
        asyncUpdateThreadMuteIcon();
        mTopTitle.setText(title);
        if (TextUtils.isEmpty(subTitle)) {
            if (cnt == 0) {
                actionBar.setIcon(R.drawable.ic_launcher_smsmms);
            }
            mTopSubtitle.setVisibility(View.GONE);
        } else {
            mTopSubtitle.setText(subTitle);
            mTopSubtitle.setVisibility(View.VISIBLE);
        }
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
    }
    
    private void asyncUpdateThreadMuteIcon () {
        MmsLog.d(TAG, "asyncUpdateThreadMuteIcon");
        new Thread (new Runnable() {
            @Override
            public void run() {
                boolean value = false;
                if (mConversation != null && mConversation.getThreadId() > 0) {
                    value = MessageUtils.checkNeedNotify(ComposeMessageActivity.this, mConversation.getThreadId(), null);
                } else {
                    value = MessageUtils.checkNeedNotify(ComposeMessageActivity.this, 0, null);
                }
                final boolean needNotify = value;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MmsLog.d(TAG, "asyncUpdateThreadMuteIcon: meedNotify is " + needNotify);
                        ActionBar actionBar = getActionBar();
                        mActionBarCustomView = actionBar.getCustomView();
                        mMuteLogo = (ImageView) mActionBarCustomView.findViewById(R.id.iv_silent);
                        mMuteLogo.setVisibility(needNotify ? View.INVISIBLE : View.VISIBLE);
                    }
                });
            }
        }, "check and update mute icon").start();
    }

    // Get the recipients editor ready to be displayed onscreen.
    private void initRecipientsEditor(Bundle bundle) {
        /// M: Code analyze 046, Whether the recipientedit control has been initialized. @{
        if (isRecipientsEditorVisible() && isInitRecipientsEditor) {
        /// @}
            return;
        }
        // Must grab the recipients before the view is made visible because getRecipients()
        // returns empty recipients when the editor is visible.
        ContactList recipients = getRecipients();
        /// M: Code analyze 058, Remove exceed recipients.
        while (!recipients.isEmpty() && recipients.size() > RECIPIENTS_LIMIT_FOR_SMS) {
            recipients.remove(RECIPIENTS_LIMIT_FOR_SMS);
        }
        /// @}

        ViewStub stub = (ViewStub)findViewById(R.id.recipients_editor_stub);
        if (stub != null) {
            View stubView = stub.inflate();
            mRecipientsEditor = (RecipientsEditor) stubView.findViewById(R.id.recipients_editor);
            mRecipientsPicker = (ImageButton) stubView.findViewById(R.id.recipients_picker);
        } else {
            mRecipientsEditor = (RecipientsEditor)findViewById(R.id.recipients_editor);
            mRecipientsEditor.setVisibility(View.VISIBLE);
            mRecipientsPicker = (ImageButton)findViewById(R.id.recipients_picker);
            /// M: Code analyze 059, Set the pick button visible or
            /// invisible the same as recipient editor.
            mRecipientsPicker.setVisibility(View.VISIBLE);
            /// @}
        }
        mRecipientsPicker.setOnClickListener(this);

        // M: indicate contain email address or not in RecipientsEditor candidates. @{
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ComposeMessageActivity.this);
        boolean showEmailAddress = prefs.getBoolean(GeneralPreferenceActivity.SHOW_EMAIL_ADDRESS, true);
        MmsLog.d(TAG, "initRecipientsEditor(), showEmailAddress = " + showEmailAddress);
        /// M: fix bug ALPS00380930
        if (mRecipientsEditor.getAdapter() == null) {
            ChipsRecipientAdapter chipsAdapter = new ChipsRecipientAdapter(this);
            chipsAdapter.setShowEmailAddress(showEmailAddress);
            mRecipientsEditor.setAdapter(chipsAdapter);
        } else {
            ((ChipsRecipientAdapter)mRecipientsEditor.getAdapter()).setShowEmailAddress(showEmailAddress);
        }
        // @}
        if (bundle == null) {
            mRecipientsEditor.populate(new ContactList());
            mRecipientsEditor.populate(recipients);
        }
        mRecipientsEditor.setOnCreateContextMenuListener(mRecipientsMenuCreateListener);
        mRecipientsEditor.removeTextChangedListener(mRecipientsWatcher);
        mRecipientsEditor.addTextChangedListener(mRecipientsWatcher);
        // TODO : Remove the max length limitation due to the multiple phone picker is added and the
        // user is able to select a large number of recipients from the Contacts. The coming
        // potential issue is that it is hard for user to edit a recipient from hundred of
        // recipients in the editor box. We may redesign the editor box UI for this use case.
        // mRecipientsEditor.setFilters(new InputFilter[] {
        //         new InputFilter.LengthFilter(RECIPIENTS_MAX_LENGTH) });

        mRecipientsEditor.setOnSelectChipRunnable(new Runnable() {
            public void run() {
                // After the user selects an item in the pop-up contacts list, move the
                // focus to the text editor if there is only one recipient.  This helps
                // the common case of selecting one recipient and then typing a message,
                // but avoids annoying a user who is trying to add five recipients and
                // keeps having focus stolen away.
                if (mRecipientsEditor.getRecipientCount() == 1) {
                    // if we're in extract mode then don't request focus
                    final InputMethodManager inputManager = mInputMethodManager;
                    if (inputManager == null || !inputManager.isFullscreenMode()) {
                        mTextEditor.requestFocus();
                    }
                }
            }
        });

        mRecipientsEditor.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    RecipientsEditor editor = (RecipientsEditor) v;
                    /// M: fix bug ALPS00432236, updateTitle only 10 contact in PORTRAIT @{
                    boolean isPortrait = getResources().getConfiguration().orientation
                                                         == Configuration.ORIENTATION_PORTRAIT;
                    int updateLimit = 0;
                    if (isPortrait) {
                        updateLimit = UPDATE_LIMIT_PORTRAIT;
                    } else {
                        updateLimit = UPDATE_LIMIT_LANDSCAPE;
                    }
                    /// @}
                    ContactList contacts = editor.constructContactsFromInputWithLimit(false, updateLimit);
                    updateTitle(contacts);
                } else { /// M: add for ip message
                    MmsLog.d(IPMSG_TAG, "onFocusChange(): mRecipientsEditor get focus.");
                    showSharePanel(false);
                    showEmoticonPanel(false);
                    if (mIsLandscape) {
                        mTextEditor.setMaxHeight(
                                mReferencedTextEditorTwoLinesHeight * mCurrentMaxHeight / mReferencedMaxHeight);
                    } else {
                        mTextEditor.setMaxHeight(
                                mReferencedTextEditorFourLinesHeight * mCurrentMaxHeight / mReferencedMaxHeight);
                    }
                }
            }
        });

        /// M: add for ipmessage
        mRecipientsEditor.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                showSharePanel(false);
                showEmoticonPanel(false);
                return false;
            }
        });

        // M: fix bug ALPS00355897
        // PhoneNumberFormatter.setPhoneNumberFormattingTextWatcher(this, mRecipientsEditor);

        mTopPanel.setVisibility(View.VISIBLE);
        /// M: Code analyze 013, Get contacts from Contact app . @{
        if (mIsRecipientHasIntentNotHandle && (mIntent != null)) {
            processPickResult(mIntent);
            mIsRecipientHasIntentNotHandle = false;
            mIntent = null;
        }
        /// @}

        /// M: add for ip message
        mCurrentNumberString = "";
    }

    //==========================================================
    // Activity methods
    //==========================================================

    public static boolean cancelFailedToDeliverNotification(Intent intent, Context context) {
        if (MessagingNotification.isFailedToDeliver(intent)) {
            // Cancel any failed message notifications
            MessagingNotification.cancelNotification(context,
                        MessagingNotification.MESSAGE_FAILED_NOTIFICATION_ID);
            return true;
        }
        return false;
    }

    public static boolean cancelFailedDownloadNotification(Intent intent, Context context) {
        if (MessagingNotification.isFailedToDownload(intent)) {
            // Cancel any failed download notifications
            MessagingNotification.cancelNotification(context,
                        MessagingNotification.DOWNLOAD_FAILED_NOTIFICATION_ID);
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /// M: Code analyze 001, Plugin opeartor. @{
        initPlugin(this);
        /// @}

        /// M: add for ipmessage
        if (MmsConfig.getIpMessagServiceId(this) == IpMessageServiceId.ISMS_SERVICE) {
            boolean isServiceReady = IpMessageUtils.getServiceManager(this).serviceIsReady();
            MmsLog.d(IPMSG_TAG, "onCreate(): is ip service ready ?= " + isServiceReady);
            if (!isServiceReady) {
                MmsLog.d(IPMSG_TAG, "Turn on ipmessage service by Composer.");
                IpMessageUtils.getServiceManager(this).startIpService();
            }
        }
        mInputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

        /// M: Code analyze 002,  If a new ComposeMessageActivity is created, kill old one
        Activity compose = sCompose == null ? null : sCompose.get();
        if (compose != null && !compose.isFinishing() && savedInstanceState == null) {
            if (!MmsConfig.isNeedExitComposerAfterForward()) {
                compose.finish();
            }
        }
        sCompose = new WeakReference(this);
        /// @}
        /// M: Code analyze 003,  Set or get max mms size.
        initMessageSettings();
        /// @}
        resetConfiguration(getResources().getConfiguration());
        /// M: Code analyze 004, Set max height for text editor. @{
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (mIsLandscape) {
            mCurrentMaxHeight = windowManager.getDefaultDisplay().getWidth();
        } else {
            mCurrentMaxHeight = windowManager.getDefaultDisplay().getHeight();
        }
        MmsLog.d(TAG, "onCreate(): mCurrentMaxHeight = " + mCurrentMaxHeight);
        /// @}
        setContentView(R.layout.compose_message_activity);
        setProgressBarVisibility(false);

        /// M: Code analyze 005, Set input mode. @{
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        /// @}

        // Initialize members for UI elements.
        initResourceRefs();
        initShareAndEmoticonRessource();
        /// M: add for ip message
        if (MmsConfig.getIpMessagServiceId(this) == IpMessageServiceId.ISMS_SERVICE) {
            initIpMessageResourceRefs();
        }

        /// M: Code analyze 001, Plugin opeartor. @{
        IMmsDialogNotify dialogPlugin =
                (IMmsDialogNotify)MmsPluginManager.getMmsPluginObject(
                            MmsPluginManager.MMS_PLUGIN_TYPE_DIALOG_NOTIFY);
        dialogPlugin.closeMsgDialog();
        /// @}

        /// M: Code analyze 006, Control SIM indicator on status bar. @{
        mStatusBarManager = new EncapsulatedStatusBarManager(getApplicationContext());
        mComponentName = getComponentName();
        /// @}

        /// M: Code analyze 007, Get information from Sim or save message to Sim. @{
        mSimCount = 0;
        /// @}

        mContentResolver = getContentResolver();
        mBackgroundQueryHandler = new BackgroundQueryHandler(mContentResolver);

        initialize(savedInstanceState, 0);

        if (TRACE) {
            android.os.Debug.startMethodTracing("compose");
        }
        /// M: Code analyze 008,unkown . @{
        mDestroy = false;
        if (mCellMgr == null) {
            mCellMgr = new EncapsulatedCellConnMgr();
        }
        mCellMgr.register(getApplication());
        mCellMgrRegisterCount++;
        /// @}
        /// M: Code analyze 009,Show attachment dialog . @{
        mSoloAlertDialog = new SoloAlertDialog(this);
        /// @}
        /// M: Code analyze 007, Get information from Sim or save message to Sim.(Get all SIM info) @{
        mGetSimInfoRunnable.run();
        /// M:
        changeWallPaper();
        /// M: add for update sim state dynamically. @{
        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        this.registerReceiver(mSimReceiver, intentFilter);
        /// @}
    }

    private void showSubjectEditor(boolean show) {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("showSubjectEditor: " + show);
        }

        if (mSubjectTextEditor == null) {
            // Don't bother to initialize the subject editor if
            // we're just going to hide it.
            if (show == false) {
                return;
            }
            mSubjectTextEditor = (EditText)findViewById(R.id.subject);
            /// M: Code analyze 068, Unknown. Why delete these code? @{
            /// mSubjectTextEditor.setFilters(new InputFilter[] {
            ///     new LengthFilter(MmsConfig.getMaxSubjectLength())});
            /// @}
            /// M: Code analyze 001, Plugin opeartor. @{
            mMmsComposePlugin.configSubjectEditor(mSubjectTextEditor);
            /// @}
       }

        mSubjectTextEditor.setOnKeyListener(show ? mSubjectKeyListener : null);

        mSubjectTextEditor.removeTextChangedListener(mSubjectEditorWatcher);
        if (show) {
            mSubjectTextEditor.addTextChangedListener(mSubjectEditorWatcher);
        }

        mSubjectTextEditor.setText(show ? mWorkingMessage.getSubject() : null);
        mSubjectTextEditor.setVisibility(show ? View.VISIBLE : View.GONE);
        mSubjectTextEditor.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                showSharePanel(false);
                showEmoticonPanel(false);
                return false;
            }
        });
        mSubjectTextEditor.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && isSharePanelOrEmoticonPanelShow()) {
                    showSharePanel(false);
                    showEmoticonPanel(false);
                }
            }
        });
        hideOrShowTopPanel();
    }

    private void hideOrShowTopPanel() {
        boolean anySubViewsVisible = (isSubjectEditorVisible() || isRecipientsEditorVisible());
        mTopPanel.setVisibility(anySubViewsVisible ? View.VISIBLE : View.GONE);
    }

    public void initialize(Bundle savedInstanceState, long originalThreadId) {
        /// M: Code analyze 010, Support dirtory mode. @{
        Intent intent = getIntent();
        boolean showInput = false;
        boolean hiderecipient = false;
        boolean isMustRecipientEditable = false;
        if (MmsConfig.getMmsDirMode()) {
            mHomeBox = intent.getIntExtra("folderbox", 0);
            showInput = intent.getBooleanExtra("showinput", false);
            hiderecipient = intent.getBooleanExtra("hiderecipient", false);
            isMustRecipientEditable = true;
        }
        /// @}

        // Create a new empty working message.
        mWorkingMessage = WorkingMessage.createEmpty(this);

        // Read parameters or previously saved state of this activity. This will load a new
        // mConversation
        initActivityState(savedInstanceState);

        /// M: add for ip message, check recipients is hesine User
        if (null != mConversation.getRecipients() && mConversation.getRecipients().size() == 1) {
            mIsIpMessageRecipients = isIpMessageRecipients(mConversation.getRecipients().get(0).getNumber());
        }

        if (LogTag.SEVERE_WARNING && originalThreadId != 0 &&
                originalThreadId == mConversation.getThreadId()) {
            LogTag.warnPossibleRecipientMismatch("ComposeMessageActivity.initialize: " +
                    " threadId didn't change from: " + originalThreadId, this);
        }

        log("savedInstanceState = " + savedInstanceState +
            ", intent = " + intent +
            ", originalThreadId = " + originalThreadId +
            ", mConversation = " + mConversation);

        /// M: Code analyze 010, Support dirtory mode. @{
        if (!MmsConfig.getMmsDirMode()) {
            if (cancelFailedToDeliverNotification(getIntent(), this)) {
                // Show a pop-up dialog to inform user the message was
                // failed to deliver.
                undeliveredMessageDialog(getMessageDate(null));
            }
            cancelFailedDownloadNotification(getIntent(), this);
        }
        ///  @}
        // Set up the message history ListAdapter
        initMessageList();

        // Load the draft for this thread, if we aren't already handling
        // existing data, such as a shared picture or forwarded message.
        boolean isForwardedMessage = false;
        // We don't attempt to handle the Intent.ACTION_SEND when saveInstanceState is non-null.
        // saveInstanceState is non-null when this activity is killed. In that case, we already
        // handled the attachment or the send, so we don't try and parse the intent again.
        boolean intentHandled = savedInstanceState == null &&
        /// M: unknown @{
            (handleSendIntent() || handleForwardedMessage() );
        /// @}
        if (!intentHandled && mConversation.hasDraft() ) {
            MmsLog.d(TAG, "Composer init load Draft.");
            loadDraft();
        }

        // Let the working message know what conversation it belongs to
        mWorkingMessage.setConversation(mConversation);

        // Show the recipients editor if we don't have a valid thread. Hide it otherwise.
        /// M: @{
        //  if (mConversation.getThreadId() <= 0) {
        if (mConversation.getThreadId() <= 0L
            || (mConversation.getMessageCount() <= 0 && (intent.getAction() != null || mConversation.hasDraft()))
            || (mConversation.getThreadId() > 0L && mConversation.getMessageCount() <= 0)
            || isMustRecipientEditable) {
         /// @}
            // Hide the recipients editor so the call to initRecipientsEditor won't get
            // short-circuited.
            hideRecipientEditor();
            initRecipientsEditor(savedInstanceState);
            /// M: Code analyze 046, Whether the recipientedit control has been initialized. @{
            isInitRecipientsEditor = true;
            /// @}

            // Bring up the softkeyboard so the user can immediately enter recipients. This
            // call won't do anything on devices with a hard keyboard.
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } else {
            hideRecipientEditor();
             /// M: @{
//            mConversation.markAsRead();
            /// @}
            /// M: add for ip message
            mIsImportantThread = intent.getBooleanExtra("load_important", false);
            mEnterImportantTimestamp = System.currentTimeMillis();
            mMsgListAdapter.setIsImportantThread(mIsImportantThread);
            mMsgListAdapter.setEnterImportantThreadTime(mEnterImportantTimestamp);
        }
        /// M: Code analyze 010, Support dirtory mode. @{
        if (MmsConfig.getMmsDirMode()) {
            if (showInput) {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            } else {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            }
            if (hiderecipient) {
                if (isRecipientsEditorVisible()) {
                    hideRecipientEditor();
                }
            }
        }
        /// M: @{

        invalidateOptionsMenu();    // do after show/hide of recipients editor because the options
                                    // menu depends on the recipients, which depending upon the
                                    // visibility of the recipients editor, returns a different
                                    // value (see getRecipients()).

        updateSendButtonState();

        drawTopPanel(false);
        if (intentHandled) {
            // We're not loading a draft, so we can draw the bottom panel immediately.
            drawBottomPanel();
        }

        onKeyboardStateChanged(true);

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("update title, mConversation=" + mConversation.toString());
        }

        if (isForwardedMessage && isRecipientsEditorVisible()) {
            // The user is forwarding the message to someone. Put the focus on the
            // recipient editor rather than in the message editor.
            mRecipientsEditor.requestFocus();
        }

        /// M: google JB.MR1 patch, group mms
        boolean isGroupMms = MmsPreferenceActivity.getIsGroupMmsEnabled(ComposeMessageActivity.this)
                                                && mConversation.getRecipients().size() > 1;
        mMsgListAdapter.setIsGroupConversation(isGroupMms);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        MmsLog.d(TAG, "onNewIntent: intent = " + intent.toString());
        setIntent(intent);

        if (mQuickTextDialog != null && mQuickTextDialog.isShowing()) {
            mQuickTextDialog.dismiss();
            mQuickTextDialog = null;
        }

        if (mSIMSelectDialog != null && mSIMSelectDialog.isShowing()) {
            mSIMSelectDialog.dismiss();
            mSIMSelectDialog = null;
        }

        Conversation conversation = null;
        mSentMessage = false;

        // If we have been passed a thread_id, use that to find our
        // conversation.

        // Note that originalThreadId might be zero but if this is a draft and we save the
        // draft, ensureThreadId gets called async from WorkingMessage.asyncUpdateDraftSmsMessage
        // the thread will get a threadId behind the UI thread's back.
        long originalThreadId = mConversation.getThreadId();
        long threadId = intent.getLongExtra("thread_id", 0);
        Uri intentUri = intent.getData();

        boolean sameThread = false;
        if (threadId > 0) {
            conversation = Conversation.get(getApplicationContext(), threadId, false);
        } else {
            if (mConversation.getThreadId() == 0) {
                // We've got a draft. Make sure the working recipients are synched
                // to the conversation so when we compare conversations later in this function,
                // the compare will work.
                mWorkingMessage.syncWorkingRecipients();
            }
            // Get the "real" conversation based on the intentUri. The intentUri might specify
            // the conversation by a phone number or by a thread id. We'll typically get a threadId
            // based uri when the user pulls down a notification while in ComposeMessageActivity and
            // we end up here in onNewIntent. mConversation can have a threadId of zero when we're
            // working on a draft. When a new message comes in for that same recipient, a
            // conversation will get created behind CMA's back when the message is inserted into
            // the database and the corresponding entry made in the threads table. The code should
            // use the real conversation as soon as it can rather than finding out the threadId
            // when sending with "ensureThreadId".
            conversation = Conversation.get(getApplicationContext(), intentUri, false);
        }

        /// M: Fix bug: ALPS00444760, The keyboard display under the MMS after
        // you tap shortcut enter this thread.
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        mIsKeyboardOpen = false;
        /// @}
        if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("onNewIntent: data=" + intentUri + ", thread_id extra is " + threadId +
                    ", new conversation=" + conversation + ", mConversation=" + mConversation);
        }

        // this is probably paranoid to compare both thread_ids and recipient lists,
        // but we want to make double sure because this is a last minute fix for Froyo
        // and the previous code checked thread ids only.
        // (we cannot just compare thread ids because there is a case where mConversation
        // has a stale/obsolete thread id (=1) that could collide against the new thread_id(=1),
        // even though the recipient lists are different)
        sameThread = ((conversation.getThreadId() == mConversation.getThreadId() ||
                mConversation.getThreadId() == 0) &&
                conversation.equals(mConversation));

        if (sameThread) {
            log("onNewIntent: same conversation");
            if (mConversation.getThreadId() == 0) {
                mConversation = conversation;
                mWorkingMessage.setConversation(mConversation);
                updateThreadIdIfRunning();
                invalidateOptionsMenu();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (MmsConfig.getIpMessagServiceId(ComposeMessageActivity.this)
                            != IpMessageServiceId.NO_SERVICE && isCurrentRecipientIpMessageUser()) {
                        IpMessageUtils.getMessageManager(ComposeMessageActivity.this)
                                .setThreadAsViewed(mConversation.getThreadId());
                    }
                }
            }).start();
        } else {
            if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                log("onNewIntent: different conversation");
            }
            /// M: @{
            /// M: Don't let any markAsRead DB updates occur before we've loaded the messages for
            /// M: the thread.
            conversation.blockMarkAsRead(true);
            /// @}
            if ((!isRecipientsEditorVisible())
                    || (mRecipientsEditor.hasValidRecipient(mWorkingMessage.requiresMms()))) {
                    //For ALPS00457252
                    if (!mWorkingMessage.isWorthSaving()) {
                        mWorkingMessage.discard();
                    } else {
                        saveDraft(false);// if we've got a draft, save it first
                    }
            }
            /// M: @{
            mMsgListAdapter.changeCursor(null);
            mConversation = conversation;
            /// @}
            /// M: add for ip message
            mCurrentNumberString = "";
            initialize(null, originalThreadId);
            /// M: add for attach dialog do not dismiss when enter other thread. @{
            if (!mSoloAlertDialog.needShow()) {
                mSoloAlertDialog.dismiss();
            }
            /// @}
        }
        loadMessageContent();
        /// M: Code analyze 048, Add this can send msg from a marked sim card
        /// which is delivered in Intent.@{
        send_sim_id = intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, -1);
        MmsLog.d(TAG, "onNewIntent get simId from intent = " + send_sim_id);
        /// @}
        /// M: Code analyze 001, Plugin opeartor. @{
        IMmsDialogNotify dialogPlugin =
                (IMmsDialogNotify)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_DIALOG_NOTIFY);
        dialogPlugin.closeMsgDialog();
        /// @}
        /// M:
        changeWallPaper();
   }

    private void sanityCheckConversation() {
        if (mWorkingMessage.getConversation() != mConversation) {
            LogTag.warnPossibleRecipientMismatch(
                    "ComposeMessageActivity: mWorkingMessage.mConversation=" +
                    mWorkingMessage.getConversation() + ", mConversation=" +
                    mConversation + ", MISMATCH!", this);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

         /// M: For fix bug ALPS00542156 The "_" still display under the SIMA after you change it to the "Always ask" or "SIMB.@{
        sCompose = null;
        sCompose = new WeakReference(this);
        /// @}

        if (mWorkingMessage.isDiscarded()) {
            // If the message isn't worth saving, don't resurrect it. Doing so can lead to
            // a situation where a new incoming message gets the old thread id of the discarded
            // draft. This activity can end up displaying the recipients of the old message with
            // the contents of the new message. Recognize that dangerous situation and bail out
            // to the ConversationList where the user can enter this in a clean manner.
            mWorkingMessage.unDiscard();    // it was discarded in onStop().
            if (mWorkingMessage.isWorthSaving()) {
                if (LogTag.VERBOSE) {
                    log("onRestart: mWorkingMessage.unDiscard()");
                }
                //mWorkingMessage.unDiscard();    // it was discarded in onStop().

                sanityCheckConversation();
            } else if (isRecipientsEditorVisible()) {
                if (LogTag.VERBOSE) {
                    log("onRestart: goToConversationList");
                }
                goToConversationList();
            } else {
                if (LogTag.VERBOSE) {
                    log("onRestart: loadDraft");
                }
                /// M: @{
                //loadDraft();
                /// @}
                mWorkingMessage.setConversation(mConversation);
                mAttachmentEditor.update(mWorkingMessage);
                updateTextEditorHeightInFullScreen();
                invalidateOptionsMenu();
            }
        }
        /// M: For fix bug ALPS00537320 The chat wallpaper will change after you tap "OK".@{
        if (!isRecipientsEditorVisible()) {
            changeWallPaper();
        }
        /// @}
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        /// M: Code analyze 036, Change text size if adjust font size.@{
        if (MmsConfig.getAdjustFontSizeEnabled()) {
            float textSize =
              MessageUtils.getPreferenceValueFloat(this, MessagingPreferenceActivity.TEXT_SIZE, 18);
            setTextSize(textSize);
        }
        if (mMmsTextSizeAdjustPlugin != null) {
            mMmsTextSizeAdjustPlugin.init(this, this);
            mMmsTextSizeAdjustPlugin.refresh();
        }
        /// @}

        /// M: Code analyze 013, Get contacts from Contact app . @{
        misPickContatct = false;
        /// @}

        initFocus();

        // Register a BroadcastReceiver to listen on HTTP I/O process.
        registerReceiver(mHttpProgressReceiver, mHttpProgressFilter);

        loadMessageContent();

        // Update the fasttrack info in case any of the recipients' contact info changed
        // while we were paused. This can happen, for example, if a user changes or adds
        // an avatar associated with a contact.
        /// M: @{
        if (mConversation.getThreadId() == 0) {
            mWorkingMessage.syncWorkingRecipients();
        }
        /// @}

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("onStart: update title, mConversation=" + mConversation.toString());
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        /// M: Code analyze 047, Extra uri from message body and get number from uri.
        /// Then use this number to update contact cache. @{
        mNeedUpdateContactForMessageContent = true;
        /// @}
        /// M: add for mark message as read
        mIsMarkMsgAsRead = true;

        /// M: ALPS00440523, print mms mem @{
        MmsConfig.printMmsMemStat(this, "ComposeMessageActivity.onStart");
        /// @}
    }

    public void loadMessageContent() {
        // Don't let any markAsRead DB updates occur before we've loaded the messages for
        // the thread. Unblocking occurs when we're done querying for the conversation
        // items.
        MmsLog.d(TAG, "loadMessageContent()");
        mConversation.blockMarkAsRead(true);
        mConversation.markAsRead();         // dismiss any notifications for this convo
        /// M: mark conversation as seen, update new messages notification.
        mConversation.markAsSeen();
        startMsgListQuery(MESSAGE_LIST_QUERY_TOKEN, 0);
        updateSendFailedNotification();
        drawBottomPanel();
    }

    private void updateSendFailedNotification() {
        final long threadId = mConversation.getThreadId();
        if (threadId <= 0)
            return;

        // updateSendFailedNotificationForThread makes a database call, so do the work off
        // of the ui thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                MessagingNotification.updateSendFailedNotificationForThread(
                        ComposeMessageActivity.this, threadId);
            }
        }, "ComposeMessageActivity.updateSendFailedNotification").start();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        /// M: Code analyze 033, Save some useful information in order to restore the draft when
        /// activity restarting.@{
        // super is commented for a bug work around see MessageUtils.showErrorDialog()
        super.onSaveInstanceState(outState); //why delete this line?

        // save recipients of this coversation
        if (mRecipientsEditor != null && isRecipientsEditorVisible()) {
            // TODO need re-coding for below code
            //outState.putString("recipients", mRecipientsEditor.allNumberToString());
            /// M: We are compressing the image, so save the thread id in order to restore the
            /// M: draft when activity restarting.
            if (mCompressingImage) {
                outState.putLong("thread", mConversation.ensureThreadId());
            } else if (mRecipientsEditor.getRecipientCount() < 1) {
                outState.putLong("thread",mConversation.ensureThreadId());
            } else if (mRecipientsEditor.getRecipientCount() > 0) {
                ArrayList<String> numbers = (ArrayList<String>)(mRecipientsEditor.getNumbers());
                outState.putString("recipients", TextUtils.join(";", numbers.toArray()));
            }
        } else {
            /// M: save the current thread id
            outState.putLong("thread", mConversation.getThreadId());
            MmsLog.i(TAG, "saved thread id:" + mConversation.getThreadId());
        }
        outState.putBoolean("compressing_image", mCompressingImage);
        /// @}
        mWorkingMessage.writeStateToBundle(outState);

        if (mExitOnSent) {
            outState.putBoolean("exit_on_sent", mExitOnSent);
        }
        /// M: save ipmessage draft if needed.
        outState.putBoolean("saved_ipmessage_draft", mIpMessageDraft != null);
        saveIpMessageDraft();
    }

    @Override
    protected void onResume() {
        /// M: fix bug ALPS00444752, set false to enable to Show ContactPicker
        mShowingContactPicker = false;

        super.onResume();

        getAsyncDialog().resetShowProgressDialog();
         /// M: Code analyze 005, Set input mode. @{
        Configuration config = getResources().getConfiguration();
        MmsLog.d(TAG, "onResume - config.orientation = " + config.orientation);
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            MmsLog.e(TAG, "onResume Set setSoftInputMode to 0x" +
                    Integer.toHexString(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN));
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
        /// @}
        /// M: Code analyze 006, Control SIM indicator on status bar. @{
        mIsShowSIMIndicator = true;
        mStatusBarManager.hideSIMIndicator(mComponentName);
        mStatusBarManager.showSIMIndicator(mComponentName, EncapsulatedSettings.System.SMS_SIM_SETTING);
        /// @}
        /// M: Code analyze 024, If the click operator can be responsed. @{
        //  button can response to start other activity
        mClickCanResponse = true;
        /// @}
        /// M: Code analyze 038, If the user is editing slideshow now.
        /// Do not allow the activity finish but return directly when back key is entered. @{
        mIsEditingSlideshow = false;
        /// @}
        if (mDrawBottomPanel) {
            drawBottomPanel();
        }
        // OLD: get notified of presence updates to update the titlebar.
        // NEW: we are using ContactHeaderWidget which displays presence, but updating presence
        //      there is out of our control.
        //Contact.startPresenceObserver();

        addRecipientsListeners();

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("onResume: update title, mConversation=" + mConversation.toString());
        }

        // There seems to be a bug in the framework such that setting the title
        // here gets overwritten to the original title.  Do this delayed as a
        // workaround.
        new Thread(new Runnable() {
            public void run() {
                /// M: Fix CR ALPS00558119
                /// When the Contact phone is long enough ,after add audio,
                /// The title will show only phony number @{
                final ContactList recipients = isRecipientsEditorVisible() ? mRecipientsEditor
                        .constructContactsFromInputWithLimit(false, UPDATE_LIMIT_PORTRAIT)
                        : getRecipients();
                int min = Math.min(UPDATE_LIMIT_PORTRAIT, recipients.size());

                for (int i = 0; i < min; i++) {
                    recipients.get(i).reload();
                }

                mMessageListItemHandler.postDelayed(new Runnable() {
                    public void run() {
                        updateTitle(recipients);
                    }
                }, 10);
            }
        }).start();

        mIsRunning = true;
        updateThreadIdIfRunning();

        /// M:
        if (mIpMessageDraft != null) {
            MmsLog.d(TAG,"show IpMsg saveIpMessageForAWhile");
            saveIpMessageForAWhile(mIpMessageDraft);
            mSendButtonCanResponse = true;
        }
        /// M: add for ip message, notification listener
        IpMessageUtils.addIpMsgNotificationListeners(this, this);
        mIsIpServiceEnabled = MmsConfig.isServiceEnabled(this);
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            mMessageSimId = Settings.System.getLong(getContentResolver(), Settings.System.SMS_SIM_SETTING,
                Settings.System.DEFAULT_SIM_NOT_SET);
            if (mMessageSimId != Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK
                    && mMessageSimId != Settings.System.DEFAULT_SIM_NOT_SET) {
                mIsMessageDefaultSimIpServiceEnabled = MmsConfig.isServiceEnabled(this, (int) mMessageSimId);
            } else {
                mIsMessageDefaultSimIpServiceEnabled = MmsConfig.isServiceEnabled(this);
            }
        } else {
            mIsMessageDefaultSimIpServiceEnabled = MmsConfig.isServiceEnabled(this);
        }

        MmsLog.d(IPMSG_TAG, "onResume(): IpServiceEnabled = " + mIsIpServiceEnabled
            + ", DefaultSimIpServiceEnabled = " + mIsMessageDefaultSimIpServiceEnabled);
        if (mIsMessageDefaultSimIpServiceEnabled && isNetworkConnected(getApplicationContext())) {
            if (mConversation.getThreadId() > 0 && mConversation.getRecipients() != null
                    && mConversation.getRecipients().size() == 1) {
                mChatModeNumber = mConversation.getRecipients().get(0).getNumber();
                if (IpMessageUtils.getContactManager(this).isIpMessageNumber(mChatModeNumber)) {
                    MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG,
                                "onResume(): enter Chat Mode. number = " + mChatModeNumber);
                    IpMessageUtils.getChatManager(this).enterChatMode(mChatModeNumber);
                } else {
                    mChatModeNumber = "";
                }
            }
            mIsCaptionOn = IpMessageUtils.getSettingsManager(this).isCaptionOn();
            mIsImageCaptionOn = IpMessageUtils.getSettingsManager(this).isPhotoCaptionOn();
            mIsVideoCaptionOn = IpMessageUtils.getSettingsManager(this).isVideoCaptionOn();
            mIsAudioCaptionOn = IpMessageUtils.getSettingsManager(this).isAudioCaptionOn();
            /// M: remove "typing" feature for a while
//            if (!TextUtils.isEmpty(mTextEditor.getText().toString())) {
//                mIpMsgHandler.post(mSendTypingRunnable);
//            }
        } else {
            mIsCaptionOn = false;
            mIsImageCaptionOn = false;
            mIsVideoCaptionOn = false;
            mIsAudioCaptionOn = false;
        }
        if (mIpMessageDraft != null && mIsCaptionOn && (
                (mIsAudioCaptionOn && mIpMessageDraft.getType() == IpMessageType.VOICE) ||
                (mIsImageCaptionOn && mIpMessageDraft.getType() == IpMessageType.PICTURE) ||
                (mIsVideoCaptionOn && mIpMessageDraft.getType() == IpMessageType.VIDEO))) {
            mIsEditingCaption = true;
        }
        MmsLog.d(IPMSG_TAG, "onResume(): mChatModeNumber = " + mChatModeNumber + ", mIsCaptionOn = " + mIsCaptionOn
            + ", mIsImageCaptionOn = " + mIsImageCaptionOn + ", mIsVideoCaptionOn = " + mIsVideoCaptionOn
            + ", mIsAudioCaptionOn = " + mIsAudioCaptionOn);
        mWorkingMessage.updateStateForGroupMmsChanged();
        updateSendButtonState();
        if ((isRecipientsEditorVisible() && mRecipientsEditor.hasFocus())
                || (isSubjectEditorVisible() && mSubjectTextEditor.hasFocus())
                || ((mTextEditor != null && mTextEditor.getVisibility() == View.VISIBLE) && mTextEditor.hasFocus())) {
            showSharePanel(false);
            showEmoticonPanel(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        /// M: fix bug ALPS00421362. Allow any blocked calls to update the thread's read status.
        if (this.isFinishing()) {
            mConversation.blockMarkAsRead(false);
            mConversation.markAsRead();
        }

        mDrawBottomPanel = true;

        // OLD: stop getting notified of presence updates to update the titlebar.
        // NEW: we are using ContactHeaderWidget which displays presence, but updating presence
        //      there is out of our control.
        //Contact.stopPresenceObserver();
        /// M: Code analyze 006, Control SIM indicator on status bar. @{
        mIsShowSIMIndicator = false;
        mStatusBarManager.hideSIMIndicator(mComponentName);
        /// @}

        removeRecipientsListeners();

        // remove any callback to display a progress spinner
        if (mAsyncDialog != null) {
            mAsyncDialog.clearPendingProgressDialog();
        }

        MessagingNotification.setCurrentlyDisplayedThreadId(MessagingNotification.THREAD_NONE);

        // Remember whether the list is scrolled to the end when we're paused so we can rescroll
        // to the end when resumed.
        if (mMsgListAdapter != null &&
                mMsgListView.getLastVisiblePosition() >= mMsgListAdapter.getCount() - 1) {
            mSavedScrollPosition = Integer.MAX_VALUE;
        } else {
            mSavedScrollPosition = mMsgListView.getFirstVisiblePosition();
        }
        if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            MmsLog.v(TAG, "onPause: mSavedScrollPosition=" + mSavedScrollPosition);
        }

        mIsRunning = false;
        mErrorDialogShown = true;
        
        /// M: we thought that the contacts data can be changed as long as the user leave.
        Contact.invalidateCache();

        /// M: add for ip message, notification listener
        IpMessageUtils.removeIpMsgNotificationListeners(this, this);
        if (!TextUtils.isEmpty(mChatModeNumber)) {
            MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "onPause(): exit Chat Mode. number = " + mChatModeNumber);
            IpMessageUtils.getChatManager(this).exitFromChatMode(mChatModeNumber);
        }

        /// M: Stop in conversation notification sound
        MessagingNotification.stopInConversationNotificationSound();
        /// @}
    }

    @Override
    protected void onStop() {
        super.onStop();
        mContentResolver.unregisterContentObserver(mDraftChangeObserver);
        if (mDraftChanged != null) {
            DraftCache.getInstance().removeOnDraftChangedListener(mDraftChanged);
        }
        /// M: Code analyze 013, Get contacts from Contact app . @{
        if (misPickContatct) {
            return;
        }
        /// @}
        // Allow any blocked calls to update the thread's read status.
        mConversation.blockMarkAsRead(false);
        mConversation.markAsRead();

        /// M: add for ip message
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (MmsConfig.getIpMessagServiceId(ComposeMessageActivity.this)
                        != IpMessageServiceId.NO_SERVICE && isCurrentRecipientIpMessageUser()) {
                    IpMessageUtils.getMessageManager(ComposeMessageActivity.this)
                            .setThreadAsViewed(mConversation.getThreadId());
                }
            }
        }).start();

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("onStop: save draft");
        }

        /// M: If image is being compressed, wait for it
        if (isFinishing()) {
            waitForCompressing();
        }
        /// M: @{
        if ((!isRecipientsEditorVisible()) ||
                (mRecipientsEditor.hasValidRecipient(mWorkingMessage.requiresMms()))) {
            saveDraft(true);
        }
        /// @}
        /// M: @{
        MmsLog.v(TAG,"update MmsWidget");
        MmsWidgetProvider.notifyDatasetChanged(getApplicationContext());
        MmsLog.i(TAG, "onStop(): mWorkingMessage.isDiscarded() == " + mWorkingMessage.isDiscarded());
        /// @}
        // Cleanup the BroadcastReceiver.
        unregisterReceiver(mHttpProgressReceiver);

        /// M: remove "typing" feature for a while
//        if (mIsIpServiceEnabled) {
//            mIpMsgHandler.post(mSendStopTypingRunnable);
//        }
        /// M: add for mark message as read
        mIsMarkMsgAsRead = false;
        /// M: fix bug ALPS00380930, fix RecipientsAdapter cursor leak @{
        // RecipientsAdapter can not close the last cursor which returned by runQueryOnBackgroundThread
        //if (mRecipientsEditor != null && isFinishing()) {
        //    CursorAdapter recipientsAdapter = (CursorAdapter)mRecipientsEditor.getAdapter();
        //    if (recipientsAdapter != null) {
        //        recipientsAdapter.changeCursor(null);
        //    }
        //}
        /// @}

        // / M: fix bug ALPS00451836, remove FLAG_DISMISS_KEYGUARD flags
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                                   WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }

    @Override
    protected void onDestroy() {
        /// M: fix bug ALPS00473488, delete ObsoleteThread through threadID when discard()
        if (mConversation.getMessageCount() == 0 && !mWorkingMessage.isWorthSaving()
                && isRecipientsEditorVisible() && mConversation.getDiscardThreadId() > 0) {
            Conversation.asyncDeleteObsoleteThreadID(mBackgroundQueryHandler, mConversation.getDiscardThreadId());
            mConversation.setDiscardThreadId(0);
        }
        /// @}
        MmsLog.d(TAG, "onDestroy()");
        if (TRACE) {
            android.os.Debug.stopMethodTracing();
        }

        unregisterReceiver(mSimReceiver);

        /// M: Code analyze 008,unkown . @{
        if (mCellMgrRegisterCount == 1) {
            mCellMgr.unregister();
        }
        mCellMgrRegisterCount--;
        /// @}
        mDestroy = true;
        mScrollListener.destroyThread();
        /// M: Stop not started queries @{
        if (mBackgroundQueryHandler != null) {
            MmsLog.d(TAG, "clear pending queries in onDestroy");
            mBackgroundQueryHandler.cancelOperation(MESSAGE_LIST_QUERY_TOKEN);
            mBackgroundQueryHandler.cancelOperation(MESSAGE_LIST_UNREAD_QUERY_TOKEN);
        }
        /// @}
        if (mMsgListAdapter != null) {
            mMsgListAdapter.destroyTaskStack();
            /// M: we need unregister cursor, so no more callback
            mMsgListAdapter.changeCursor(null);
        /// M: Remove listener @{
            mMsgListAdapter.setOnDataSetChangedListener(null);
        /// @}
        }

        /// M: add for ipmessage
        mIsDestroyTypingThread = true;
        synchronized (mShowTypingLockObject) {
            mShowTypingLockObject.notifyAll();
        }
        /// M: recycle share and emoticon view
        recycleShareAndEmoticonView();

        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        MmsLog.d(TAG, "onConfigurationChanged-Start");
        super.onConfigurationChanged(newConfig);
        if (mSharePanel != null) {
            mSharePanel.resetShareItem();
        }
        if (mEmoticonPanel != null) {
            mEmoticonPanel.resetShareItem();
        }
        if (LOCAL_LOGV) {
            MmsLog.v(TAG, "onConfigurationChanged: " + newConfig);
        }

        if (resetConfiguration(newConfig)) {
            // Have to re-layout the attachment editor because we have different layouts
            // depending on whether we're portrait or landscape.
            drawTopPanel(isSubjectEditorVisible());
        }
        onKeyboardStateChanged(mIsKeyboardOpen);
        MmsLog.d(TAG, "onConfigurationChanged-End");
    }

    // returns true if landscape/portrait configuration has changed
    private boolean resetConfiguration(Configuration config) {
        MmsLog.d(TAG, "resetConfiguration-Start");
        mIsKeyboardOpen = config.keyboardHidden == KEYBOARDHIDDEN_NO;
        boolean isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE;
        MmsLog.d(TAG, "resetConfiguration: isLandscape = " + isLandscape);
        /// M: Code analyze 004, Set max height for text editor. @{
        if ((mTextEditor != null) && (mTextEditor.getVisibility() == View.VISIBLE) && isLandscape) {
            mUiHandler.postDelayed(new Runnable() {
                public void run() {
                    MmsLog.d(TAG, "resetConfiguration(): mTextEditor.setMaxHeight: "
                            + mReferencedTextEditorThreeLinesHeight);
                    mTextEditor.setMaxHeight(mReferencedTextEditorTwoLinesHeight * mCurrentMaxHeight
                            / mReferencedMaxHeight);
                }
            }, 100);
        }
        /// @}

        MmsLog.d(IPMSG_TAG, "resetConfiguration(): isLandscape = " + isLandscape + ", mIsKeyboardOpen = " + mIsKeyboardOpen);
        if (!isLandscape && mIsKeyboardOpen) {
            showSharePanel(false);
            showEmoticonPanel(false);
        }

        if (mIsLandscape != isLandscape) {
            mIsLandscape = isLandscape;
            MmsLog.d(TAG, "resetConfiguration-End");
            return true;
        }
        MmsLog.d(TAG, "resetConfiguration-End");
        return false;
    }

    private void onKeyboardStateChanged(boolean isKeyboardOpen) {
        // If the keyboard is hidden, don't show focus highlights for
        // things that cannot receive input.
        if (isKeyboardOpen) {
            if (mRecipientsEditor != null) {
                mRecipientsEditor.setFocusableInTouchMode(true);
            }
            if (mSubjectTextEditor != null) {
                mSubjectTextEditor.setFocusableInTouchMode(true);
            }
            mTextEditor.setFocusableInTouchMode(true);
            /// M: add for ip message
            updateTextEditorHint();
        } else {
            if (mRecipientsEditor != null) {
                mRecipientsEditor.setFocusable(false);
            }
            if (mSubjectTextEditor != null) {
                mSubjectTextEditor.setFocusable(false);
            }
            mTextEditor.setFocusable(false);
            mTextEditor.setHint(R.string.open_keyboard_to_compose_message);
        }
    }

    @Override
    public void onUserInteraction() {
        checkPendingNotification();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        MmsLog.d(TAG, "onKeyDown(): keyCode = " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DEL:
                if ((mMsgListAdapter != null) && mMsgListView.isFocused()) {
                    Cursor cursor;
                    try {
                        cursor = (Cursor) mMsgListView.getSelectedItem();
                    } catch (ClassCastException e) {
                        MmsLog.e(TAG, "Unexpected ClassCastException.", e);
                        return super.onKeyDown(keyCode, event);
                    }

                    if (cursor != null) {
                        String type = cursor.getString(COLUMN_MSG_TYPE);
                        long msgId = cursor.getLong(COLUMN_ID);
                        MessageItem msgItem = mMsgListAdapter.getCachedMessageItem(type, msgId,
                                cursor);
                        if (msgItem != null) {
                            DeleteMessageListener l = new DeleteMessageListener(msgItem);
                            confirmDeleteDialog(l, msgItem.mLocked);
                        }
                        return true;
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                /// M: @{
                break;
                /// @}
            case KeyEvent.KEYCODE_ENTER:
                /// M: Code analyze 028, Before sending message,check the recipients count
                /// and add sim card selection dialog if multi sim cards exist.@{
                if (isPreparedForSending()) {
                    checkRecipientsCount();
                    return true;
                } else {
                    unpreparedForSendingAlert();
                /// @}
                }
                break;
            case KeyEvent.KEYCODE_BACK:
                /// M:
                if (isSharePanelOrEmoticonPanelShow()) {
                    hideSharePanelOrEmoticonPanel();
                    return true;
                }
                /// M: Code analyze 038, If the user is editing slideshow now.
                /// Do not allow the activity finish but return directly when back key is entered. @{
                if (mIsEditingSlideshow) {
                    return true;
                }
                /// @}

                // M: when out of composemessageactivity,try to send read report
                if (EncapsulatedFeatureOption.MTK_SEND_RR_SUPPORT) {
                    checkAndSendReadReport();
                }
                /// @}
                exitComposeMessageActivity(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
                return true;
            case KeyEvent.KEYCODE_MENU:
                invalidateOptionsMenu();
                return false;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void unpreparedForSendingAlert() {
        if (!isHasRecipientCount()) {
            new AlertDialog.Builder(this).setIconAttribute(
                    android.R.attr.alertDialogIcon).setTitle(
                    R.string.cannot_send_message).setMessage(
                    R.string.cannot_send_message_reason).setPositiveButton(
                    R.string.yes, new CancelSendingListener()).show();
        } else {
            new AlertDialog.Builder(this).setIconAttribute(
                    android.R.attr.alertDialogIcon).setTitle(
                    R.string.cannot_send_message).setMessage(
                    R.string.cannot_send_message_reason_no_content)
                    .setPositiveButton(R.string.yes,
                            new CancelSendingListener()).show();
        }
    }

    private void exitComposeMessageActivity(final Runnable exit) {
          VideoThumbnailCache.clear();
        // If the message is empty, just quit -- finishing the
        // activity will cause an empty draft to be deleted.
        if (!mWorkingMessage.isWorthSaving() && mIpMessageDraft == null) {
            /// M: Code analyze 042, If you discard the draft message manually.@{
            mWorkingMessage.discard();
            mHasDiscardWorkingMessage = true;
            /// @}
            exit.run();
            return;
        }

        if (isRecipientsEditorVisible() &&
                !mRecipientsEditor.hasValidRecipient(mWorkingMessage.requiresMms())) {
            MessageUtils.showDiscardDraftConfirmDialog(this, new DiscardDraftListener());
            return;
        }

        if (needSaveDraft()) {
            /// M: for requery searchactivity.
            SearchActivity.setNeedRequery();
            DraftCache.getInstance().setSavingDraft(true);
        }
        mWorkingMessage.setNeedDeleteOldMmsDraft(true);
        mToastForDraftSave = true;
        exit.run();
    }

    private void goToConversationList() {
        finish();
        /// M: Code analyze 010, Support dirtory mode. @{
        if (MmsConfig.getMmsDirMode()) {
            Intent it = new Intent(this, FolderViewList.class);
            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            it.putExtra("floderview_key", mHomeBox);
            startActivity(it);
        } else {
        ///  @}
        /// M: add extra flags
        Intent it = new Intent(this, ConversationList.class);
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(it);
        }
    }

    private void hideRecipientEditor() {
        if (mRecipientsEditor != null) {
            mRecipientsEditor.removeTextChangedListener(mRecipientsWatcher);
            mRecipientsEditor.setVisibility(View.GONE);
            /// M: Code analyze 059, Set the pick button visible or
            /// invisible the same as recipient editor.
            mRecipientsPicker.setVisibility(View.GONE);
            /// @}
            hideOrShowTopPanel();
        }
    }

    private boolean isRecipientsEditorVisible() {
        return (null != mRecipientsEditor)
                    && (View.VISIBLE == mRecipientsEditor.getVisibility());
    }

    private boolean isSubjectEditorVisible() {
        return (null != mSubjectTextEditor)
                    && (View.VISIBLE == mSubjectTextEditor.getVisibility());
    }

    @Override
    public void onAttachmentChanged() {
        // Have to make sure we're on the UI thread. This function can be called off of the UI
        // thread when we're adding multi-attachments
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                drawBottomPanel();
                updateSendButtonState();
                drawTopPanel(isSubjectEditorVisible());
                if (null != mRecipientsEditor) {
                    if (mWorkingMessage.hasSlideshow()) {
                        mRecipientsEditor.setImeActionLabel(getString(com.android.internal.R.string.ime_action_done),
                                EditorInfo.IME_ACTION_DONE);
                        mRecipientsEditor.setImeOptions(EditorInfo.IME_ACTION_DONE);
                    } else {
                        mRecipientsEditor.setImeActionLabel(getString(com.android.internal.R.string.ime_action_next),
                                EditorInfo.IME_ACTION_NEXT);
                        mRecipientsEditor.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                    }
                }

                mInputMethodManager.restartInput(mRecipientsEditor);
            }
        });
    }

    /// M: Code analyze 060, For bug ALPS00050082, When the protocol has been changed,
    /// whether show a toast . @{
    @Override
    public void onProtocolChanged(final boolean mms, final boolean needToast) {
        // Have to make sure we're on the UI thread. This function can be called off of the UI
        // thread when we're adding multi-attachments
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //toastConvertInfo(mms);
                showSmsOrMmsSendButton(mms);

                if (mms) {
                    // In the case we went from a long sms with a counter to an mms because
                    // the user added an attachment or a subject, hide the counter --
                    // it doesn't apply to mms.
                    mTextCounter.setVisibility(View.GONE);
                } else if (mTextEditor.getLineCount() > 1) {
                    mTextCounter.setVisibility(View.VISIBLE);
                }
                updateSendButtonState();
                if (needToast) {
                    toastConvertInfo(mms);
                }
            }
        });
    }
    /// @}

    // Show or hide the Sms or Mms button as appropriate. Return the view so that the caller
    // can adjust the enableness and focusability.
    private View showSmsOrMmsSendButton(boolean isMms) {
        View showButton;
        View hideButton1;
        View hideButton2;
        MmsLog.d(IPMSG_TAG, "showSmsOrMmsSendButton(): SendCommonMsgThisTime = " + mJustSendMsgViaCommonMsgThisTime
            + ", mIsMessageDefaultSimIpServiceEnabled = " + mIsMessageDefaultSimIpServiceEnabled
            + ", isNetworkConnected(getApplicationContext() = " + isNetworkConnected(getApplicationContext())
            + ", mIpMessageDraft is not null ?= " + (mIpMessageDraft != null)
            + ", isCurrentRecipientIpMessageUser() = " + isCurrentRecipientIpMessageUser());
        if (isMms) {
            if (mSimCount == 0 || (isRecipientsEditorVisible() && TextUtils.isEmpty(mRecipientsEditor.getText()))
                    /// M: fix bug ALPS00563318, show gray mms_send_button
                    /// when haven't subject, text and attachment
                    || ((mSubjectTextEditor == null || (mSubjectTextEditor != null
                            && TextUtils.isEmpty(mSubjectTextEditor.getText().toString().trim())))
                            && mTextEditor != null
                            && TextUtils.isEmpty(mTextEditor.getText().toString().trim())
                            && !mWorkingMessage.hasAttachment())) {
                mSendButtonMms.setCompoundDrawablesWithIntrinsicBounds(null, null, null,
                    getResources().getDrawable(R.drawable.ic_send_sms_unsend));
            } else {
                mSendButtonMms.setCompoundDrawablesWithIntrinsicBounds(null, null, null,
                    getResources().getDrawable(R.drawable.ic_send_sms));
            }
            showButton = mSendButtonMms;
            hideButton1 = mSendButtonSms;
            hideButton2 = mSendButtonIpMessage;
        } else if (!mJustSendMsgViaCommonMsgThisTime && mIsMessageDefaultSimIpServiceEnabled
                && isNetworkConnected(getApplicationContext())
                && ((mIpMessageDraft != null && recipientCount() > 0) || isCurrentRecipientIpMessageUser())) {
            showButton = mSendButtonIpMessage;
            hideButton1 = mSendButtonSms;
            hideButton2 = mSendButtonMms;
        } else {
            if (TextUtils.isEmpty(mTextEditor.getText().toString().trim()) || mSimCount == 0
                    || (isRecipientsEditorVisible() && TextUtils.isEmpty(mRecipientsEditor.getText()))
                    || recipientCount() > MmsConfig.getSmsRecipientLimit()) {
                mSendButtonSms.setImageResource(R.drawable.ic_send_sms_unsend);
            } else {
                mSendButtonSms.setImageResource(R.drawable.ic_send_sms);
            }
            showButton = mSendButtonSms;
            hideButton1 = mSendButtonIpMessage;
            hideButton2 = mSendButtonMms;
        }
        if (showButton != null) {
            showButton.setVisibility(View.VISIBLE);
        }
        if (hideButton1 != null) {
            hideButton1.setVisibility(View.GONE);
        }
        if (hideButton2 != null) {
            hideButton2.setVisibility(View.GONE);
        }
        /// M: add for ip message, update text editor hint
        updateTextEditorHint();
        return showButton;
    }

    Runnable mResetMessageRunnable = new Runnable() {
        @Override
        public void run() {
            resetMessage();
        }
    };

    @Override
    public void onPreMessageSent() {
        runOnUiThread(mResetMessageRunnable);
    }

    @Override
    public void onMessageSent() {
        // This callback can come in on any thread; put it on the main thread to avoid
        // concurrency problems
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                /// M: @{
                mWaitingForSendMessage = false;
                /// @}
                // If we already have messages in the list adapter, it
                // will be auto-requerying; don't thrash another query in.
                // TODO: relying on auto-requerying seems unreliable when priming an MMS into the
                // outbox. Need to investigate.
//                if (mMsgListAdapter.getCount() == 0) {
                    if (LogTag.VERBOSE) {
                        log("onMessageSent");
                    }
                    startMsgListQuery(MESSAGE_LIST_QUERY_TOKEN, 0);
//                }

                // The thread ID could have changed if this is a new message that we just inserted
                // into the database (and looked up or created a thread for it)
                updateThreadIdIfRunning();
            }
        });
    }

    @Override
    public void onMaxPendingMessagesReached() {
        saveDraft(false);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ComposeMessageActivity.this, R.string.too_many_unsent_mms,
                        Toast.LENGTH_LONG).show();
                mSendingMessage = false;
                updateSendButtonState();
            }
        });
    }

    @Override
    public void onAttachmentError(final int error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                handleAddAttachmentError(error, R.string.type_picture);
                onMessageSent();        // now requery the list of messages
            }
        });
    }

    // We don't want to show the "call" option unless there is only one
    // recipient and it's a phone number.
    private boolean isRecipientCallable() {
        ContactList recipients = getRecipients();
        return (recipients.size() == 1 && !recipients.containsEmail());
    }
    /// M: Code analyze 061, Add video call menu.
    private void dialRecipient(Boolean isVideoCall) {
        if (isRecipientCallable()) {
            String number = getRecipients().get(0).getNumber();
            Intent dialIntent ;
            if (isVideoCall) {
                dialIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
                dialIntent.putExtra("com.android.phone.extra.video", true);
            } else {
                dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
            }
            hideInputMethod();
            startActivity(dialIntent);
        }
    }
    /// @}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu) ;

        menu.clear();
        /// M: google JB.MR1 patch, group mms
        if (getRecipients().size() > 1) {
            menu.add(0, MENU_GROUP_PARTICIPANTS, 0, R.string.menu_group_participants);
        }

        if (mWorkingMessage.hasSlideshow()) {
            menu.add(0, MENU_ADD_ATTACHMENT, 0, R.string.add_attachment);
        }
        int serviceId = MmsConfig.getIpMessagServiceId(this);
        /// M: whether has IPMsg APK or not. ture: has ; false: no;
        boolean hasIpMsgApk = (serviceId != IpMessageServiceId.NO_SERVICE);
        /// M: the identity of whether the current users all are ipmessage user or not.
        boolean hasIpMsgUser = isCurrentRecipientIpMessageUser();
        /// M: true: the host has been activated.
        boolean hasActivatedHost = MmsConfig.isActivated(this);

        if (hasIpMsgUser && hasActivatedHost && !isRecipientsEditorVisible()) {
            menu.add(0, MENU_INVITE_FRIENDS_TO_CHAT, 0,
                    IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                .getSingleString(IpMessageConsts.string.ipmsg_invite_friends_to_chat));
        } else if (!hasIpMsgUser && mIsIpServiceEnabled && mShowInviteMsg) {
            menu.add(0, MENU_INVITE_FRIENDS_TO_IPMSG, 0,
                IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                            .getSingleString(IpMessageConsts.string.ipmsg_invite_friends_to_ipmsg));
        }

        if (!isRecipientsEditorVisible()) {
            if (getRecipients().size() == 1) {
                Contact contact = getRecipients().get(0);
                if (contact.existsInDatabase()) {
                    menu.add(0, MENU_SHOW_CONTACT, 0, R.string.menu_view_contact)
                        .setIcon(R.drawable.ic_menu_recipients)
                        .setTitle(R.string.menu_view_contact)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    mQuickContact.assignContactUri(contact.getUri());
                } else if (MessageUtils.canAddToContacts(contact)){
                    menu.add(0, MENU_CREATE_CONTACT, 0, R.string.menu_add_to_contacts)
                        .setIcon(R.drawable.ic_menu_recipients)
                        .setTitle(R.string.menu_add_to_contacts)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    String number = contact.getNumber();
                    if (Mms.isEmailAddress(number)) {
                        mQuickContact.assignContactFromEmail(number, true);
                    } else {
                        mQuickContact.assignContactFromPhone(number, true);
                    }
                }
            }

            menu.add(0, MENU_SELECT_MESSAGE, 0, R.string.select_message);
            if (hasActivatedHost) {
                MmsLog.d(IPMSG_TAG, "onPrepareOptionsMenu(): spam = " + mConversation.isSpam());
                if (mConversation.isSpam()) {
                    menu.add(0, MENU_REMOVE_SPAM, 0, R.string.remove_frome_spam);
                } else {
                    menu.add(0, MENU_MARK_AS_SPAM, 0, R.string.mark_as_spam);
                }
            }
            menu.add(0, MENU_ADD_SHORTCUT, 0, R.string.add_shortcut);
            if (hasActivatedHost && mConversation.getThreadId() > 0) {
                if (IpMessageUtils.getServiceManager(this).isFeatureSupported(FeatureId.ALL_MEDIA)
                        && IpMessageUtils.getChatManager(this).getIpMessageCountOfTypeInThread(
                            mConversation.getThreadId(), IpMessageMediaTypeFlag.PICTURE | IpMessageMediaTypeFlag.VOICE
                            | IpMessageMediaTypeFlag.VIDEO) > 0) {
                    menu.add(0, MENU_VIEW_ALL_MEDIA, 0,
                            IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_view_all_media));
                }
                if (IpMessageUtils.getServiceManager(this).isFeatureSupported(FeatureId.ALL_LOCATION)
                        && IpMessageUtils.getChatManager(this).getIpMessageCountOfTypeInThread(
                            mConversation.getThreadId(), IpMessageMediaTypeFlag.LOCATION) > 0) {
                    menu.add(0, MENU_VIEW_ALL_LOCATION, 0,
                            IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_view_all_location));
                }
            }
        }

        /// M: Code analyze 061, Add video call menu.
        TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephony != null && telephony.isVoiceCapable() && isRecipientCallable()) {
            MenuItem item = menu.add(0, MENU_CALL_RECIPIENT, 0, R.string.menu_call)
                .setIcon(R.drawable.ic_menu_call)
                .setTitle(R.string.menu_call);
            if (!isRecipientsEditorVisible()) {
                // If we're not composing a new message, show the call icon in the actionbar
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
            if (EncapsulatedFeatureOption.MTK_VT3G324M_SUPPORT) {
                menu.add(0, MENU_CALL_RECIPIENT_BY_VT, 0, R.string.call_video_call)
                        .setIcon(R.drawable.ic_video_call).setTitle(R.string.call_video_call);
            }
        /// @}
        }

        /// M: Code analyze 014, Add quick text. @{
        if (!mWorkingMessage.hasSlideshow() || (mSubjectTextEditor != null && mSubjectTextEditor.isFocused())) {
            menu.add(0, MENU_ADD_QUICK_TEXT, 0, R.string.menu_insert_quick_text).setIcon(
                R.drawable.ic_menu_quick_text);
        }
        /// @}

        /// M: Code analyze 015, Add text vcard. @{
        if (!mWorkingMessage.hasSlideshow() && !hasIpMsgApk) {
            menu.add(0, MENU_ADD_TEXT_VCARD, 0, R.string.menu_insert_text_vcard);
        }
        /// @}

        if (!isSubjectEditorVisible()) {
            menu.add(0, MENU_ADD_SUBJECT, 0, R.string.add_subject).setIcon(R.drawable.ic_menu_edit);
        }
        if (!hasIpMsgApk) {
            buildAddAddressToContactMenuItem(menu);
        }

        if (LogTag.DEBUG_DUMP) {
            menu.add(0, MENU_DEBUG_DUMP, 0, R.string.menu_debug_dump);
        }

        if (!isRecipientsEditorVisible()) {
            menu.add(0, MENU_CHAT_SETTING, 0, R.string.pref_setting_chat).setIcon(
                android.R.drawable.ic_menu_preferences);
        }

        if (isRecipientsEditorVisible()) {
            menu.add(0, MENU_DISCARD, 0, R.string.discard).setIcon(
                android.R.drawable.ic_menu_delete);
        }

        return true;
    }

    private void buildAddAddressToContactMenuItem(Menu menu) {
        // Look for the first recipient we don't have a contact for and create a menu item to
        // add the number to contacts.
        for (Contact c : getRecipients()) {
            /// M: Code analyze 043, Whether the address can be added to contacts app. @{
            if (!c.existsInDatabase() && MessageUtils.canAddToContacts(c)) {
            /// @}
                Intent intent = ConversationList.createAddContactIntent(c.getNumber());
                menu.add(0, MENU_ADD_ADDRESS_TO_CONTACTS, 0, R.string.menu_add_to_contacts)
                    .setIcon(android.R.drawable.ic_menu_add)
                    .setIntent(intent);
                break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onIpMsgOptionsItemSelected(item);
        switch (item.getItemId()) {
            case MENU_ADD_SUBJECT:
                showSubjectEditor(true);
                mWorkingMessage.setSubject("", true);
                /// M: Code analyze 052, Show input keyboard.@{
                mInputMethodManager.showSoftInput(getCurrentFocus(), InputMethodManager.SHOW_IMPLICIT);
                /// @}
                updateSendButtonState();
                mSubjectTextEditor.requestFocus();
                break;
            case MENU_ADD_ATTACHMENT:
                // Launch the add-attachment list dialog
                /// M: Code analyze 051, Hide input keyboard.@{
                hideInputMethod();
                /// @}
                /// M: Vcard and slides can be in same screen
                //add for attachment enhance
                IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin =
                        (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(
                                MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);
                if (mMmsAttachmentEnhancePlugin != null) {
                    if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance()) {
                        //OP01
                        showAddAttachmentDialog(true);
                        MmsLog.e(TAG, "Attach: showAddAttachmentDialog(true)");
                    } else {
                        //Not OP01
                        showAddAttachmentDialog(!mWorkingMessage.hasAttachedFiles());
                        MmsLog.e(TAG, "Attach: showAddAttachmentDialog(!mWorkingMessage.hasAttachedFiles())");
                    }
                } else {
                    showAddAttachmentDialog(!mWorkingMessage.hasAttachedFiles());
                    MmsLog.e(TAG, "YF attach: showAddAttachmentDialog(true)");
                }
                /// @}
                //showAddAttachmentDialog(!mWorkingMessage.hasAttachedFiles());
                break;
            /// M: Code analyze 014, Add quick text. @{
            case MENU_ADD_QUICK_TEXT:
                showQuickTextDialog();
                break;
            /// @}
            /// M: Code analyze 015, Add text vcard. @{
            case MENU_ADD_TEXT_VCARD: {
                Intent intent = new Intent("android.intent.action.contacts.list.PICKMULTICONTACTS");
                intent.setType(Contacts.CONTENT_TYPE);
                startActivityForResult(intent, REQUEST_CODE_TEXT_VCARD);
                break;
            }
            /// @}
            case MENU_DISCARD:
                mWorkingMessage.discard();
                finish();
                break;
            case MENU_SEND:
                if (isPreparedForSending()) {
                    /// M: add for ip message, unread divider
                    mShowUnreadDivider = false;
                    /// M: Code analyze 028, Before sending message,check the recipients count
                    /// and add sim card selection dialog if multi sim cards exist.@{
                    updateSendButtonState(false);
                    checkRecipientsCount();
                    mSendButtonCanResponse = true;
                    /// @}
                }
                break;
            case MENU_SEARCH:
                onSearchRequested();
                break;
            case MENU_DELETE_THREAD:
                /// M: Code analyze 012, add for multi-delete @{
                Intent it = new Intent(this, MultiDeleteActivity.class);
                it.putExtra("thread_id", mConversation.getThreadId());
                startActivityForResult(it, REQUEST_CODE_FOR_MULTIDELETE);
                /// @}
                break;

            case android.R.id.home:
            case MENU_CONVERSATION_LIST:
                exitComposeMessageActivity(new Runnable() {
                    @Override
                    public void run() {
                        goToConversationList();
                    }
                });
                break;
            case MENU_CALL_RECIPIENT:
                dialRecipient(false);
                break;
            /// M: Code analyze 061, Add video call menu.
            case MENU_CALL_RECIPIENT_BY_VT:
                dialRecipient(true);
                break;
            /// @}
            case MENU_INSERT_SMILEY:
                showSmileyDialog();
                break;
            /// M: google jb.mr1 patch, group mms
            case MENU_GROUP_PARTICIPANTS: {
                Intent intent = new Intent(this, RecipientListActivity.class);
                intent.putExtra(THREAD_ID, mConversation.getThreadId());
                startActivityForResult(intent, REQUEST_CODE_GROUP_PARTICIPANTS);
                break;
            }
            case MENU_VIEW_CONTACT: {
                // View the contact for the first (and only) recipient.
                ContactList list = getRecipients();
                if (list.size() == 1 && list.get(0).existsInDatabase()) {
                    Uri contactUri = list.get(0).getUri();
                    Intent intent = new Intent(Intent.ACTION_VIEW, contactUri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    startActivity(intent);
                }
                break;
            }
            case MENU_ADD_ADDRESS_TO_CONTACTS:
                mAddContactIntent = item.getIntent();
                startActivityForResult(mAddContactIntent, REQUEST_CODE_ADD_CONTACT);
                break;
            case MENU_PREFERENCES: {
                Intent intent = new Intent(this, SettingListActivity.class);
                startActivityIfNeeded(intent, -1);
                break;
            }
            case MENU_DEBUG_DUMP:
                mWorkingMessage.dump();
                Conversation.dump();
                LogTag.dumpInternalTables(this);
                break;
            case MENU_ADD_TO_CONTACTS: {
                mAddContactIntent = ConversationList.createAddContactIntent(getRecipients().get(0).getNumber());
                startActivityForResult(mAddContactIntent, REQUEST_CODE_ADD_CONTACT);
                break;
            }
            /// M: show contact detail or create new contact. @{
            case MENU_SHOW_CONTACT:
            case MENU_CREATE_CONTACT:
                hideInputMethod();
                mQuickContact.onClick(mActionBarCustomView);
                break;
            /// @}
            default:
                MmsLog.d(TAG, "unkown option.");
                break;
        }

        return true;
    }

    private void confirmDeleteThread(long threadId) {
        Conversation.startQueryHaveLockedMessages(mBackgroundQueryHandler,
                threadId, ConversationList.HAVE_LOCKED_MESSAGES_TOKEN);
    }

//    static class SystemProperties { // TODO, temp class to get unbundling working
//        static int getInt(String s, int value) {
//            return value;       // just return the default value or now
//        }
//    }

    private void addAttachment(int type, boolean append) {
        // Calculate the size of the current slide if we're doing a replace so the
        // slide size can optionally be used in computing how much room is left for an attachment.
        int currentSlideSize = 0;
        SlideshowModel slideShow = mWorkingMessage.getSlideshow();

        /// M: Code analyze 025, Add video or audio attachment and check the attachment size.@{
        if (append) {
            mAppendAttachmentSign = true;
        } else {
            mAppendAttachmentSign = false;
        }
        /// @}

        if (slideShow != null) {
            WorkingMessage.removeThumbnailsFromCache(slideShow);
            SlideModel slide = slideShow.get(0);
            currentSlideSize = slide == null ? 0 : slide.getSlideSize();
        }
        /// M: Code analyze 025, Add video or audio attachment and check the attachment size.@{
        if ((type != AttachmentTypeSelectorAdapter.ADD_SLIDESHOW) && (type != AttachmentTypeSelectorAdapter.ADD_VCARD)
            && (!checkSlideCount(mAppendAttachmentSign))) {
            return;
        }
        /// @}
        switch (type) {
            case AttachmentTypeSelectorAdapter.ADD_IMAGE:
                MessageUtils.selectImage(this, REQUEST_CODE_ATTACH_IMAGE);
                break;

            case AttachmentTypeSelectorAdapter.TAKE_PICTURE: {
                MessageUtils.capturePicture(this, REQUEST_CODE_TAKE_PICTURE);
                break;
            }

            case AttachmentTypeSelectorAdapter.ADD_VIDEO:
                MessageUtils.selectVideo(this, REQUEST_CODE_ATTACH_VIDEO);
                break;

            case AttachmentTypeSelectorAdapter.RECORD_VIDEO: {
                /// M: Code analyze 025, Add video or audio attachment and check the attachment size.@{
                long sizeLimit = 0;
                if (mAppendAttachmentSign) {
                    sizeLimit = computeAttachmentSizeLimitForAppen(slideShow);
                } else {
                    sizeLimit = computeAttachmentSizeLimit(slideShow, currentSlideSize);
                }
                if (sizeLimit > MIN_SIZE_FOR_CAPTURE_VIDEO) {
                    MessageUtils.recordVideo(this, REQUEST_CODE_TAKE_VIDEO, sizeLimit);
                } else {
                    Toast.makeText(this,
                            getString(R.string.space_not_enough),
                            Toast.LENGTH_SHORT).show();
                }
                /// @}
            }
            break;

            case AttachmentTypeSelectorAdapter.ADD_SOUND:
                /// M: Code analyze 018, Add ringtone for sound attachment.  @{
                //MessageUtils.selectAudio(this, REQUEST_CODE_ATTACH_SOUND);
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setTitle(getString(R.string.add_music));
                String[] items = new String[2];
                items[0] = getString(R.string.attach_ringtone);
                items[1] = getString(R.string.attach_sound);
                alertBuilder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                MessageUtils.selectRingtone(ComposeMessageActivity.this, REQUEST_CODE_ATTACH_RINGTONE);
                                break;
                            case 1:
                                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                                    Toast.makeText(ComposeMessageActivity.this,
                                                    getString(R.string.Insert_sdcard), Toast.LENGTH_LONG).show();
                                    return;
                                }
                                MessageUtils.selectAudio(ComposeMessageActivity.this, REQUEST_CODE_ATTACH_SOUND);
                                break;
                            default:
                                break;
                        }
                    }
                });
                alertBuilder.create().show();
               /// @}
                break;

            case AttachmentTypeSelectorAdapter.RECORD_SOUND:
                /// M: Code analyze 025, Add video or audio attachment and check the attachment size.@{
                long sizeLimit = 0;
                if (mAppendAttachmentSign) {
                    sizeLimit = computeAttachmentSizeLimitForAppen(slideShow);
                } else {
                    sizeLimit = computeAttachmentSizeLimit(slideShow, currentSlideSize);
                }
                if (sizeLimit > ComposeMessageActivity.MIN_SIZE_FOR_RECORD_AUDIO) {
                    MessageUtils.recordSound(this, REQUEST_CODE_RECORD_SOUND, sizeLimit);
                } else {
                    Toast.makeText(this, getString(R.string.space_not_enough_for_audio), Toast.LENGTH_SHORT).show();
                }
                /// @}
                break;

            case AttachmentTypeSelectorAdapter.ADD_SLIDESHOW:
                editSlideshow();
                break;
            /// M: Code analyze 019, Add vcard attachment.@{
            case AttachmentTypeSelectorAdapter.ADD_VCARD:
                Intent intent = new Intent("android.intent.action.contacts.list.PICKMULTICONTACTS");
                intent.setType(Contacts.CONTENT_TYPE);
                startActivityForResult(intent, REQUEST_CODE_ATTACH_VCARD);
                break;
            /// @}
            /// M: Code analyze 020, Add vcalendar attachment.  @{
            case AttachmentTypeSelectorAdapter.ADD_VCALENDAR:
                Intent i = new Intent("android.intent.action.CALENDARCHOICE");
                i.setType("text/x-vcalendar");
                i.putExtra("request_type", 0);
                startActivityForResult(i, REQUEST_CODE_ATTACH_VCALENDAR);
                break;
            /// @}
            default:
                break;
        }
    }

    public static long computeAttachmentSizeLimit(SlideshowModel slideShow, int currentSlideSize) {
        // Computer attachment size limit. Subtract 1K for some text.
        /// M: Code analyze 003,  Set or get max mms size.
        long sizeLimit = MmsConfig.getUserSetMmsSizeLimit(true) - SlideshowModel.SLIDESHOW_SLOP;
        /// @}
        if (slideShow != null) {
            sizeLimit -= slideShow.getCurrentSlideshowSize();

            // We're about to ask the camera to capture some video (or the sound recorder
            // to record some audio) which will eventually replace the content on the current
            // slide. Since the current slide already has some content (which was subtracted
            // out just above) and that content is going to get replaced, we can add the size of the
            // current slide into the available space used to capture a video (or audio).
            sizeLimit += currentSlideSize;
        }
        return sizeLimit;
    }

    private void showAddAttachmentDialog(final boolean append) {
        /// M: Code analyze 009,Show attachment dialog . Create a new class to @{
        mSoloAlertDialog.show(append);
        /// @}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (LogTag.VERBOSE) {
            log("requestCode=" + requestCode + ", resultCode=" + resultCode + ", data=" + data);
        }
        /// M: add this to avoid invoke multi times
        boolean needSaveDraft = true;
        mWaitingForSubActivity = false;          // We're back!
        // boolean mNeedAppendAttachment = false;
        /// M: Code analyze 025, Add video or audio attachment and check the attachment size.@{
        // if (mAppendAttachmentSign) {
        //     mNeedAppendAttachment = true;
        //     mAppendAttachmentSign = false;
        // }

        // M: fix bug ALPS00354728
        boolean mNeedAppendAttachment = true;

        if (mAppendAttachmentSign) {
            mNeedAppendAttachment = true;
        } else if (!mAppendAttachmentSign) {
            mNeedAppendAttachment = false;
        }

        /// @}
        if (mWorkingMessage.isFakeMmsForDraft()) {
            // We no longer have to fake the fact we're an Mms. At this point we are or we aren't,
            // based on attachments and other Mms attrs.
            mWorkingMessage.removeFakeMmsForDraft();
        }
        if (requestCode == REQUEST_CODE_FOR_FORWARD) {
            DraftCache.getInstance().addOnDraftChangedListener(mDraftChanged);
        }

        /// M: add for ipmessage
        if (IpMessageUtils.getIpMessagePlugin(this).isActualPlugin()) {
            onIpMsgActivityResult(ComposeMessageActivity.this, requestCode, resultCode, data);
        }

         /// M: Code analyze 012, add for multi-delete @{
         if (requestCode == REQUEST_CODE_FOR_MULTIDELETE && resultCode == RESULT_OK) {
             ContactList recipients = mConversation.getRecipients();
             mConversation = Conversation.upDateThread(ComposeMessageActivity.this, mConversation.getThreadId(), false);
             if (mConversation.getMessageCount() <= 0 || mConversation.getThreadId() <= 0) {
                 mMsgListAdapter.changeCursor(null);
                 if (needSaveDraft() && (recipients != null)) {
                     makeDraftEditable(recipients);
                 } else {
                     mWorkingMessage.discard();

                     /// M: Rebuild the contacts cache now that a thread and its associated unique
                     /// M: recipients have been deleted.
                     Contact.init(getApplicationContext());

                     /// M: Make sure the conversation cache reflects the threads in the DB.
                     Conversation.init(getApplicationContext());
                     finish();
                 }
             }
            return;
        }
        /// @}
        if (requestCode == REQUEST_CODE_PICK) {
            mWorkingMessage.asyncDeleteDraftSmsMessage(mConversation);
        }

        /// M: fix bug ALPS00490684, update group mms state from GROUP_PARTICIPANTS to setting @{
        if (requestCode == REQUEST_CODE_GROUP_PARTICIPANTS) {
            boolean multiRecipients = mConversation.getRecipients().size() > 1;
            boolean isGroupMms = MmsPreferenceActivity.getIsGroupMmsEnabled(ComposeMessageActivity.this)
                && multiRecipients;
            mMsgListAdapter.setIsGroupConversation(isGroupMms);
            mWorkingMessage.setHasMultipleRecipients(multiRecipients, true);
        }
        /// @}

        if (requestCode == REQUEST_CODE_ADD_CONTACT) {
            // The user might have added a new contact. When we tell contacts to add a contact
            // and tap "Done", we're not returned to Messaging. If we back out to return to
            // messaging after adding a contact, the resultCode is RESULT_CANCELED. Therefore,
            // assume a contact was added and get the contact and force our cached contact to
            // get reloaded with the new info (such as contact name). After the
            // contact is reloaded, the function onUpdate() in this file will get called
            // and it will update the title bar, etc.
            if (mAddContactIntent != null) {
                String address =
                    mAddContactIntent.getStringExtra(ContactsContract.Intents.Insert.EMAIL);
                if (address == null) {
                    address =
                        mAddContactIntent.getStringExtra(ContactsContract.Intents.Insert.PHONE);
                }
                if (address != null) {
                    Contact contact = Contact.get(address, false);
                    if (contact != null) {
                        contact.reload();
                        /// M: long press an unsaved number and save it, update the chip in recipient control.
                        commitToChipIfNeeded();
                    }
                }
            }
        }

        if (requestCode == REQUEST_CODE_VIEW_CONTACT) {
            /// M: fix bug ALPS00448677, update or delete Contact Chip
            if (isRecipientsEditorVisible() && mInViewContact != null
                    && mRecipientsEditor.getRecipientCount() > UPDATE_LIMIT_PORTRAIT) {
                mInViewContact.reload();
            }
            /// @}
        }

        if (resultCode != RESULT_OK) {
            if (LogTag.VERBOSE) log("bail due to resultCode=" + resultCode);
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_CREATE_SLIDESHOW:
                if (data != null) {
                    MmsLog.v(TAG, "begin REQUEST_CODE_CREATE_SLIDESHOW ");
                    WorkingMessage newMessage = WorkingMessage.load(this, data.getData());
                    if (newMessage != null && newMessage.getSlideshow() != null) {
                        MmsLog.v(TAG, "REQUEST_CODE_CREATE_SLIDESHOW newWorkingMessage Slideshow num = " + newMessage.getSlideshow().size());
                    } else {
                        MmsLog.v(TAG, "REQUEST_CODE_CREATE_SLIDESHOW newWorkingMessage Slideshow = null or newMessage = null");
                    }

                    if (newMessage != null) {
                        /// M: fix bug ALPS00600816, re-create SlideshowModel
                        if (System.currentTimeMillis() - mEditSlideshowStart < 10000) {
                            Log.d(TAG, "REQUEST_CODE_CREATE_SLIDESHOW: < 10000");
                            newMessage.setReCreateSlideshow(true);
                        }

                        /// M: Code analyze 053, If exist vcard attachment, move it before
                        /// creating a new slideshow.Because the Workingmessage object  has been
                        /// changed, reset subject and tell the convertion to user.@{
                        /// M: add for vcard, vcard is exclusive with other attaches, so remove them
                        if (newMessage.hasMediaAttachments()) {
                            newMessage.removeAllFileAttaches();
                        }
                        boolean isMmsBefore = mWorkingMessage.requiresMms();
                        newMessage.setSubject(mWorkingMessage.getSubject(), false);

                        mWorkingMessage = newMessage;
                        //mWorkingMessage.setConversation(mConversation);  //move to load of WorkingMessage.
                        updateThreadIdIfRunning();                        
                        drawTopPanel(isSubjectEditorVisible());
                        updateSendButtonState();
                        invalidateOptionsMenu();

                        boolean isMmsAfter = mWorkingMessage.requiresMms();
                        if (isMmsAfter && !isMmsBefore) {
                            toastConvertInfo(true);
                        } else if (!isMmsAfter && isMmsBefore) {
                            toastConvertInfo(false);
                            /// M: fix bug ALPS00557600, mms draft with subject should be deleted When send sms. @{
                            mWorkingMessage.deleteDraftMmsMessage(mConversation.getThreadId());
                            /// @}
                        }
                        /// @}
                    }
                }
                // M: fix bug ALPS00354728
                MmsLog.e(TAG, "In REQUEST_CODE_CREATE_SLIDESHOW ");
                MmsLog.e(TAG, "mNeedAppendAttachment = " + mNeedAppendAttachment);

                break;

            case REQUEST_CODE_TAKE_PICTURE: {
                // create a file based uri and pass to addImage(). We want to read the JPEG
                // data directly from file (using UriImage) instead of decoding it into a Bitmap,
                // which takes up too much memory and could easily lead to OOM.
                /// M: Code analyze 001, Plugin opeartor. @{
                if (mMmsComposePlugin.getCapturePicMode() == IMmsCompose.CAPTURE_PIC_NORMAL) {
                    Uri uri = data.getData();
                    // Remove the old captured picture's thumbnail from the cache
                    MmsApp.getApplication().getThumbnailManager().removeThumbnail(uri);
                    addImageAsync(uri, data.getType(), mNeedAppendAttachment);
                /// @}
                } else {
                    /// M: fix bug ALPS00408589
                    String scrappath = TempFileProvider.getScrapPath(this);
                    if (scrappath != null) {
                        File file = new File(scrappath);
                        Uri uri = Uri.fromFile(file);

                        // Remove the old captured picture's thumbnail from the cache
                        MmsApp.getApplication().getThumbnailManager().removeThumbnail(uri);

                        addImageAsync(uri, null, mNeedAppendAttachment);
                    }
                }
                needSaveDraft = false;
                break;
            }

            case REQUEST_CODE_ATTACH_IMAGE: {
                if (data != null) {
                    addImageAsync(data.getData(),data.getType(), mNeedAppendAttachment);
                }
                needSaveDraft = false;
                break;
            }

            case REQUEST_CODE_TAKE_VIDEO:
                Uri videoUri = TempFileProvider.renameScrapVideoFile(System.currentTimeMillis() + ".3gp", null, this);
                // Remove the old captured video's thumbnail from the cache
                MmsApp.getApplication().getThumbnailManager().removeThumbnail(videoUri);

                addVideoAsync(videoUri, mNeedAppendAttachment);      // can handle null videoUri
                needSaveDraft = false;
                break;

            case REQUEST_CODE_ATTACH_VIDEO:
                if (data != null) {
                    addVideoAsync(data.getData(), mNeedAppendAttachment);
                }
                needSaveDraft = false;
                break;

            case REQUEST_CODE_ATTACH_SOUND: {
                Uri uri = (Uri) data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (EncapsulatedSettings.System.DEFAULT_RINGTONE_URI.equals(uri)) {
                    break;
                }
                //addAudio(uri);
                addAudioAsync(data.getData(),mNeedAppendAttachment);
                needSaveDraft = false;
                /// @}
                break;
            }

            case REQUEST_CODE_RECORD_SOUND:
                if (data != null) {
                    addAudioAsync(data.getData(),mNeedAppendAttachment);
                }
                needSaveDraft = false;
                break;

            /// M: @{
            case REQUEST_CODE_ATTACH_RINGTONE:
                Uri uri = (Uri) data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (EncapsulatedSettings.System.DEFAULT_RINGTONE_URI.equals(uri)) {
                    break;
                }
                addAudioAsync(uri,mNeedAppendAttachment);
                needSaveDraft = false;
                break;
            /// @}

            case REQUEST_CODE_ECM_EXIT_DIALOG:
                boolean outOfEmergencyMode = data.getBooleanExtra(EXIT_ECM_RESULT, false);
                if (outOfEmergencyMode) {
                    sendMessage(false);
                }
                needSaveDraft = false;
                break;

            case REQUEST_CODE_PICK:
                /// M: Code analyze 013, Get contacts from Contact app . @{
                if (data != null) {
                    if (mRecipientsEditor != null) {
                        processPickResult(data);
                    } else {
                        mIsRecipientHasIntentNotHandle = true;
                        mIntent = data;
                    }
                }
                misPickContatct = false;
                return;
                /// @}
            /// M: Code analyze 015, Add text vcard. @{
            case REQUEST_CODE_TEXT_VCARD:
                if (data != null) {
                    long[] contactIds = data.getLongArrayExtra("com.mediatek.contacts.list.pickcontactsresult");
                    addTextVCardAsync(contactIds);
                } else {
                    MmsLog.e(TAG, "data should not be null," + "requestCode=" + requestCode
                            + ", resultCode=" + resultCode + ", data=" + data);
                }
                misPickContatct = false;
                return;
            /// @}
            /// M: Code analyze 019, Add vcard attachment.@{
            case REQUEST_CODE_ATTACH_VCARD:
                asyncAttachVCardByContactsId(data, false);
                misPickContatct = false;
                isInitRecipientsEditor = false;
                return;
            /// @}
            /// M: Code analyze 020, Add vcalendar attachment.  @{
            case REQUEST_CODE_ATTACH_VCALENDAR:
                asyncAttachVCalendar(data.getData());
                break;
            /// @}
            /// M: Code analyze 021, Copy all valid parts of the attachment(pdu) to SD card.
            /// This opeartor will be removed to a separate activity.  @{
            case REQUEST_CODE_MULTI_SAVE:
                boolean succeeded = false;
                if (data != null && data.hasExtra("multi_save_result")) {
                    succeeded = data.getBooleanExtra("multi_save_result", false);
                    int resId = succeeded ? R.string.copy_to_sdcard_success : R.string.copy_to_sdcard_fail;
                    Toast.makeText(ComposeMessageActivity.this, resId, Toast.LENGTH_SHORT).show();
                }
                return;
            /// @}
            default:
                if (LogTag.VERBOSE) log("bail due to unknown requestCode=" + requestCode);
                break;
        }
        /// M: @{
        isInitRecipientsEditor = false; /// why add this variable here???
        /// M: 181 add for 121871
        if (needSaveDraft) {
            mWorkingMessage.saveDraft(false);
        }
        /// @}
    }

    private void processPickResult(final Intent data) {
        // The EXTRA_PHONE_URIS stores the phone's urls that were selected by user in the
        // multiple phone picker.
        /// M: Code analyze 013, Get contacts from Contact app . @{
        /*final Parcelable[] uris =
            data.getParcelableArrayExtra(Intents.EXTRA_PHONE_URIS);

        final int recipientCount = uris != null ? uris.length : 0;*/
        
        final long[] contactsId = data.getLongArrayExtra("com.mediatek.contacts.list.pickdataresult");
        /// M: add for ip message {@
        final String mSelectContactsNumbers = data.getStringExtra(IpMessageUtils.SELECTION_CONTACT_RESULT);
        if ((contactsId == null || contactsId.length <= 0) && TextUtils.isEmpty(mSelectContactsNumbers)) {
            return;
        }
        int recipientCount = mRecipientsEditor.getRecipientCount();
        if (!TextUtils.isEmpty(mSelectContactsNumbers)) {
            recipientCount += mSelectContactsNumbers.split(";").length;
        } else {
            recipientCount += contactsId.length;
        }
        /// @}
        /// M: Code analyze 056,Now,the sms recipient limit is different from mms.
        /// We can set limit for sms or mms individually. @{
        final int recipientLimit = MmsConfig.getSmsRecipientLimit();
        /// @}
        if (recipientLimit != Integer.MAX_VALUE && recipientCount > recipientLimit) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.pick_too_many_recipients)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(getString(R.string.too_many_recipients, recipientCount, recipientLimit))
                    .setPositiveButton(android.R.string.ok, null)
                    .create().show();
            return;
        }

        /// M: @{
        final Handler handler = new Handler();
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getText(R.string.adding_recipients));
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);

        final Runnable showProgress = new Runnable() {
            public void run() {
                progressDialog.show();
            }
        };

        // Only show the progress dialog if we can not finish off parsing the return data in 1s,
        // otherwise the dialog could flicker.
        handler.postDelayed(showProgress, 1000);

        new Thread(new Runnable() {
            public void run() {
                final ContactList list = new ContactList();
                final ContactList allList = new ContactList();
                 try {
                    /// M: @{
                    //list = ContactList.blockingGetByUris(uris);
                    /// M: add for ip message
                    ContactList selected = TextUtils.isEmpty(mSelectContactsNumbers) ?
                         ContactList.blockingGetByIds(contactsId) :
                             ContactList.getByNumbers(mSelectContactsNumbers, false, false);
                    final List<String> numbers = mRecipientsEditor.getNumbers();

                    /** M: better merge strategy.
                     * Avoid the use of mRecipientsEditor.contrcutionContactsFromInput()
                     * all Contacts in selected list should be added.
                     * */
                    /// M: remove duplicated numbers and format
                    List<String> selectedNumbers = Arrays.asList(selected.getProtosomaitcNumbers());
                    String selectedNumberAfterFormat = "";
                    if (numbers.size() > 0) {
                        for (String number : numbers) {
                            if (!number.trim().equals("")) {
                                Contact c = Contact.get(number, false);
                                allList.add(c);
                            }
                        }
                        /// M: format existing numbers(remove "-" and " ")
                        List<String> formatedNumbers = Arrays.asList(allList.getNumbers(true));
                        for (String selectedNumber : selectedNumbers) {
                            selectedNumberAfterFormat = MessageUtils.parseMmsAddress(selectedNumber);
                            if (selectedNumberAfterFormat != null && !selectedNumberAfterFormat.trim().equals("") && !formatedNumbers.contains(selectedNumberAfterFormat)) {
                                Contact c = Contact.get(selectedNumber, false);
                                list.add(c);
                            }
                        }
                        allList.addAll(list);
                    } else {
                        for (String selectedNumber : selectedNumbers) {
                            selectedNumberAfterFormat = MessageUtils.parseMmsAddress(selectedNumber);
                            if (selectedNumberAfterFormat != null && !selectedNumber.trim().equals("")) {
                                Contact c = Contact.get(selectedNumber, false);
                                list.add(c);
                            }
                        }
                        allList.addAll(list);
                    }
                    /// @}
                } finally {
                    handler.removeCallbacks(showProgress);
                    progressDialog.dismiss();
                }
                // TODO: there is already code to update the contact header widget and recipients
                // editor if the contacts change. we can re-use that code.
                final Runnable populateWorker = new Runnable() {
                    public void run() {
                        mConversation.setRecipients(allList);
                        if (list.size() > 0) {
                            /// M: before append selected contacts, construct number
                            /// which already in control to a chip.
                            commitToChipIfNeeded();
                            mRecipientsEditor.populate(list);
                        }
                        updateTitle(allList);
                        if (mRecipientsEditor != null && isRecipientsEditorVisible()) {
                            mRecipientsEditor.requestFocus();
                        }
                    }
                };
                handler.post(populateWorker);
            }
        }, "ComoseMessageActivity.processPickResult").start();
    }

    /// M: Code analyze 062, Resize image. @{
    private final ResizeImageResultCallback mResizeImageCallback = new ResizeImageResultCallback() {
        // TODO: make this produce a Uri, that's what we want anyway
        @Override
        public void onResizeResult(PduPart part, boolean append) {
            mNeedSaveAsMms = false;
            if (part == null) {
                notifyCompressingDone();
                handleAddAttachmentError(WorkingMessage.UNKNOWN_ERROR, R.string.type_picture);
                return;
            }

            mWorkingMessage.setmResizeImage(true);
            Context context = ComposeMessageActivity.this;
            PduPersister persister = PduPersister.getPduPersister(context);
            int result;
            if (mWorkingMessage.isDiscarded()) {
                notifyCompressingDone();
                return;
            }
            Uri messageUri = mWorkingMessage.getMessageUri();   
            if (null == messageUri) {
                try {
                    messageUri = mWorkingMessage.saveAsMms(true);
                } catch (IllegalStateException e) {
                    notifyCompressingDone();
                    MmsLog.e(TAG, e.getMessage() + ", go to ConversationList!");
                    goToConversationList();
                }
            }
            if (messageUri == null) {
                result = WorkingMessage.UNKNOWN_ERROR;
            } else {
                try {
                    /// M: it is modifying the mms draft, maybe interlaced with WorkingMessage.saveDraft!
                    Uri dataUri;
                    int mode;
                    synchronized (WorkingMessage.sDraftMmsLock) {
                        dataUri = persister.persistPart(part, ContentUris.parseId(messageUri));
                        mode = mWorkingMessage.sCreationMode;
                        mWorkingMessage.sCreationMode = 0;
                        result = mWorkingMessage.setAttachment(WorkingMessage.IMAGE, dataUri, append);
                    }
                    mWorkingMessage.sCreationMode = mode;
                    if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                        log("ResizeImageResultCallback: dataUri=" + dataUri);
                    }
                } catch (MmsException e) {
                    result = WorkingMessage.UNKNOWN_ERROR;
                }
            }

            /// M:added for bug ALPS00317889 begin,for not pop up alertDialog if
            // attachment size is reaching limited size
            if (!mShowDialogForMultiImage) {
                handleAddAttachmentError(result, R.string.type_picture);
            }
            if (result == WorkingMessage.MESSAGE_SIZE_EXCEEDED) {
                mShowDialogForMultiImage = true;
            }
            /// M:added for bug ALPS00317889 end
            if (result == WorkingMessage.OK) {
                try {
                    if (mWorkingMessage.saveAsMms(false) != null) {
                        mHasDiscardWorkingMessage = true;
                    }
                } catch (IllegalStateException e) {
                    MmsLog.e(TAG, e.getMessage() + ", go to ConversationList!");
                    notifyCompressingDone();
                    goToConversationList();
                } 
            }
            notifyCompressingDone();
        }
    };
    /// @}

    private void handleAddAttachmentError(final int error, final int mediaTypeStringId) {
        if (error == WorkingMessage.OK) {
            return;
        }
        MmsLog.d(TAG, "handleAddAttachmentError: " + error);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Resources res = getResources();
                String mediaType = res.getString(mediaTypeStringId);
                String title, message;
                mWorkingMessage.removeFakeMmsForDraft();
                updateSendButtonState();

                MmsLog.d(TAG, "Error Code:" + error);
                switch(error) {
                /// M: Code analyze 063, For new feature ALPS00233419, Creation mode . @{
                case WorkingMessage.WARNING_TYPE:
                /// @}
                case WorkingMessage.UNKNOWN_ERROR:
                    message = res.getString(R.string.error_add_attachment, mediaType);
                    Toast.makeText(ComposeMessageActivity.this, message, Toast.LENGTH_SHORT).show();
                    return;
                case WorkingMessage.UNSUPPORTED_TYPE:
                /// M: Code analyze 063, For new feature ALPS00233419, Creation mode . @{
                case WorkingMessage.RESTRICTED_TYPE:
                /// @}
                    title = res.getString(R.string.unsupport_media_type);
                    message = res.getString(R.string.select_different_media_type);
                    break;
                case WorkingMessage.MESSAGE_SIZE_EXCEEDED:
                    title = res.getString(R.string.exceed_message_size_limitation, mediaType);
                    message = res.getString(R.string.failed_to_add_image, mediaType);
                    break;
                case WorkingMessage.IMAGE_TOO_LARGE:
                    title = res.getString(R.string.failed_to_resize_image);
                    message = res.getString(R.string.resize_image_error_information);
                    break;
                 /// M: Code analyze 063, For new feature ALPS00233419, Creation mode . @{
                case WorkingMessage.RESTRICTED_RESOLUTION:
                    title = res.getString(R.string.select_different_media_type);
                    message = res.getString(R.string.image_resolution_too_large);
                    break;
                /// @}
                default:
                    throw new IllegalArgumentException("unknown error " + error);
                }
                if (mErrorDialogShown) {
                    MessageUtils.showErrorDialog(ComposeMessageActivity.this, title, message);
                    mErrorDialogShown = false;
                }
            }
        });
    }

    /// M: Code analyze 064, Add image attachment. @{
    private void addImageAsync(final Uri uri, final String mimeType, final boolean append) {
        mCompressingImage = true;
        getAsyncDialog().runAsync(new Runnable() {
            @Override
            public void run() {
                mShowDialogForMultiImage = false;/// M:added for bug ALPS00317889
                addImage(mimeType, uri, append);
                saveAsMms(false);
             }
        }, null, R.string.adding_attachments_title);
    }

    private void addImage(String mimeType, final Uri uri, final boolean append) {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("addImage: append=" + append + ", uri=" + uri);
        }
        mNeedSaveAsMms = true;
        int result = WorkingMessage.OK;
        try {
            if (append) {
                mWorkingMessage.checkSizeBeforeAppend();
            }
        } catch (ExceedMessageSizeException e) {
            result = WorkingMessage.MESSAGE_SIZE_EXCEEDED;
            notifyCompressingDone();
            handleAddAttachmentError(result, R.string.type_picture);
            mNeedSaveAsMms = false;
            return;
        }

        result = mWorkingMessage.setAttachment(WorkingMessage.IMAGE, uri, append,mimeType);

        if (result == WorkingMessage.IMAGE_TOO_LARGE ||
            result == WorkingMessage.MESSAGE_SIZE_EXCEEDED) {
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                log("addImage: resize image " + uri);
            }
            //MessageUtils.resizeImageAsync(this,
            //uri, mAttachmentEditorHandler, mResizeImageCallback, append);
            
            /// M: Adjust whether its a DRM IMAGE
            if (EncapsulatedFeatureOption.MTK_DRM_APP) {
                if (!MessageUtils.checkUriContainsDrm(this, uri)) {
                    mToastCountForResizeImage++;
                    if (mToastCountForResizeImage == 1) {
                        MessageUtils.resizeImage(this, uri, mAttachmentEditorHandler, mResizeImageCallback, append,
                            true);
                    } else {
                        MessageUtils.resizeImage(this, uri, mAttachmentEditorHandler, mResizeImageCallback, append,
                            false);
                    }
                } else {
                    notifyCompressingDone();
                    handleAddAttachmentError(result, R.string.type_picture);
                    mNeedSaveAsMms = false;
                }
            } else {
                mToastCountForResizeImage++;
                if (mToastCountForResizeImage == 1) {
                    MessageUtils.resizeImage(this, uri, mAttachmentEditorHandler, mResizeImageCallback, append, true);
                } else {
                    MessageUtils.resizeImage(this, uri, mAttachmentEditorHandler, mResizeImageCallback, append, false);
                }
            }
            return;
        } else if (result == WorkingMessage.WARNING_TYPE) {
            mNeedSaveAsMms = false;
            notifyCompressingDone();
            runOnUiThread(new Runnable() {
                public void run() {
                    showConfirmDialog(uri, append, WorkingMessage.IMAGE, R.string.confirm_restricted_image);
                }
            });
            return;
        }
        notifyCompressingDone();
        if (result != WorkingMessage.OK) {
            mNeedSaveAsMms = false;
        }
        handleAddAttachmentError(result, R.string.type_picture);
    }
    /// @}

    /// M: Code analyze 065, Add video attachment. @{
    private void addVideoAsync(final Uri uri, final boolean append) {
        getAsyncDialog().runAsync(new Runnable() {
            @Override
            public void run() {
                addVideo(uri, append);
                saveAsMms(false);
           }
        }, null, R.string.adding_attachments_title);
    }

    private void addVideo(final Uri uri, final boolean append) {
        if (uri != null) {
            mNeedSaveAsMms = true;
            int result = WorkingMessage.OK;
            try {
                if (append) {
                    mWorkingMessage.checkSizeBeforeAppend();
                }
            } catch (ExceedMessageSizeException e) {
                result = WorkingMessage.MESSAGE_SIZE_EXCEEDED;
                handleAddAttachmentError(result, R.string.type_video);
                mNeedSaveAsMms = false;
                return;
            }
            result = mWorkingMessage.setAttachment(WorkingMessage.VIDEO, uri, append);
            if (result == WorkingMessage.WARNING_TYPE) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        showConfirmDialog(uri, append, WorkingMessage.VIDEO, R.string.confirm_restricted_video);
                    }
                });
            } else {
                handleAddAttachmentError(result, R.string.type_video);
                if (result != WorkingMessage.OK) {
                    mNeedSaveAsMms = false;
                }
            }
        }
    }
    /// @}

    private void addAudioAsync(final Uri uri, final boolean append) {
        getAsyncDialog().runAsync(new Runnable() {
            @Override
            public void run() {
                addAudio(uri, append);
                saveAsMms(false);
           }
        }, null, R.string.adding_attachments_title);
    }

    /// M: remove unused method @{
    /*
    ///: Code analyze 067, Add audio attachment. @{
    private void addAudio(final Uri uri) {
        int result = WorkingMessage.OK;
        result = mWorkingMessage.setAttachment(WorkingMessage.AUDIO, uri, false);
        if (result == WorkingMessage.WARNING_TYPE) {
            runOnUiThread(new Runnable() {
                public void run() {
                    showConfirmDialog(uri, false, WorkingMessage.AUDIO, R.string.confirm_restricted_audio);
                }
            });
            return;
        }
        handleAddAttachmentError(result, R.string.type_audio);
    }
    /// @}
    */
    /// @}

    AsyncDialog getAsyncDialog() {
        if (mAsyncDialog == null) {
            mAsyncDialog = new AsyncDialog(this);
        }
        return mAsyncDialog;
    }

    /// M: Code analyze 017, Handle forwarded message.(see:forwardMessage())@{
    private boolean handleForwardedMessage() {
        Intent intent = getIntent();

        // If this is a forwarded message, it will have an Intent extra
        // indicating so.  If not, bail out.
        if (intent.getBooleanExtra(FORWARD_MESSAGE, false) == false) {
            return false;
        }

        Uri uri = intent.getParcelableExtra("msg_uri");

        if (Log.isLoggable(LogTag.APP, Log.DEBUG)) {
            log("handle forwarded message " + uri);
        }

        if (uri != null) {
            mWorkingMessage = WorkingMessage.load(this, uri);
            mWorkingMessage.setSubject(intent.getStringExtra("subject"), false);

            SlideshowModel mSlideshowModel = mWorkingMessage.getSlideshow();
            if (mSlideshowModel != null) {
                int mSsmSize = mSlideshowModel.size();
                for (int index = 0; index < mSsmSize; index++) {
                    SlideModel mSlideModel = mSlideshowModel.get(index);
                    if (mSlideModel != null) {
                        if (mSlideModel.hasText()) {
                            TextModel mTextModel = mSlideModel.getText();
                            String textChar = mTextModel.getText();
                            long textLength = textChar.length();
                            if (textLength > MmsConfig.getMaxTextLimit()) {
                                mTextModel.setText(textChar.substring(0, MmsConfig.getMaxTextLimit()));
                            }
                        }
                    }
                }
            }
        } else {
            String smsAddress = null;
            if (intent.hasExtra(SMS_ADDRESS)) {
                smsAddress = intent.getStringExtra(SMS_ADDRESS);
                if (smsAddress != null){
                   //TODO need re-coding
                   //mRecipientsEditor.addRecipient(smsAddress, true);
                }
            }
            mWorkingMessage.setText(intent.getStringExtra(SMS_BODY));
        }
        /// M:
        if (intent.getBooleanExtra(FORWARD_IPMESSAGE, false)) {
            long ipMsgId = intent.getLongExtra(IP_MESSAGE_ID, 0);
            mIpMessageDraft = IpMessageUtils.getMessageManager(this).getIpMsgInfo(ipMsgId);
            mIpMessageDraft.setId(0);
            if (mIpMessageDraft.getType() == IpMessageType.PICTURE
                    && !TextUtils.isEmpty(((IpImageMessage) mIpMessageDraft).getCaption())) {
                mWorkingMessage.setText(((IpImageMessage) mIpMessageDraft).getCaption());
            } else if (mIpMessageDraft.getType() == IpMessageType.VOICE
                    && !TextUtils.isEmpty(((IpVoiceMessage) mIpMessageDraft).getCaption())) {
                mWorkingMessage.setText(((IpVoiceMessage) mIpMessageDraft).getCaption());
            } else if (mIpMessageDraft.getType() == IpMessageType.VIDEO
                    && !TextUtils.isEmpty(((IpVideoMessage) mIpMessageDraft).getCaption())) {
                mWorkingMessage.setText(((IpVideoMessage) mIpMessageDraft).getCaption());
            }
            saveIpMessageForAWhile(mIpMessageDraft);
        }

        // let's clear the message thread for forwarded messages
        mMsgListAdapter.changeCursor(null);

        return true;
    }
    /// @}

    // Handle send actions, where we're told to send a picture(s) or text.
    private boolean handleSendIntent() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return false;
        }
        /// M: Code analyze 066,Handle intent. @{
        /// M: add for saveAsMms
        mWorkingMessage.setConversation(mConversation);
        final String mimeType = intent.getType();
        String action = intent.getAction();
        MmsLog.i(TAG, "Get mimeType: " + mimeType);
        MmsLog.i(TAG, "Get action: " + action);
        if (Intent.ACTION_SEND.equals(action)) {
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                final Uri uri = (Uri)extras.getParcelable(Intent.EXTRA_STREAM);
                if (mimeType.equals("text/plain")) {
                    String fileName = "";
                    if (uri != null) {
                        String mUriStr = Uri.decode(uri.toString());
                        fileName = mUriStr.substring(mUriStr.lastIndexOf("/") + 1, mUriStr.length());
                    }
                    String mMessage = this.getString(R.string.failed_to_add_media, fileName);
                    Toast.makeText(this, mMessage, Toast.LENGTH_SHORT).show();
                    return false;
                }
                getAsyncDialog().runAsync(new Runnable() {
                    @Override
                    public void run() {
                        /// M: fix bug ALPS00397146, removeThumbnailManager uri
                        // (Content://media/external/images/media/) when it rotated
                        String fileName = "";
                        int degree = 0;
                        String uriStr = uri.toString();
                        if (uriStr.startsWith("content://media/external/images/media")) {
                            Cursor c = mContentResolver.query(uri, null, null, null, null);
                            if (c != null && c.getCount() == 1 && c.moveToFirst()) {
                                try {
                                    fileName = c.getString(c.getColumnIndex(Images.Media.DISPLAY_NAME));
                                    degree = c.getInt(c.getColumnIndex(Images.Media.ORIENTATION));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    c.close();
                                }
                            }

                            if (sDegreeMap != null && sDegreeMap.containsKey(uriStr)) {
                                if (sDegreeMap.get(uriStr).intValue() != degree) {
                                    Uri thumbnailUri =
                                        Uri.parse(uriStr + ThumbnailManager.FLAG_FNAME + fileName);
                                    MmsApp.getApplication().getThumbnailManager()
                                                                    .removeThumbnail(thumbnailUri);
                                    sDegreeMap.remove(uriStr);
                                    sDegreeMap.put(uriStr, degree);
                                }
                            } else if (sDegreeMap != null) {
                                sDegreeMap.put(uriStr, degree);
                            }
                        }
                        /// @}
                        addAttachment(mimeType, uri, false);
                        SlideshowModel mSlideShowModel = mWorkingMessage.getSlideshow();
                        if (mSlideShowModel != null && mSlideShowModel.getCurrentSlideshowSize() > 0) {
                            mWorkingMessage.saveAsMms(false);
                        }
                    }
                }, null, R.string.adding_attachments_title);
                intent.setAction(SIGN_CREATE_AFTER_KILL_BY_SYSTEM);
                return true;
            } else if (extras.containsKey(Intent.EXTRA_TEXT)) {
                mWorkingMessage.setText(extras.getString(Intent.EXTRA_TEXT));
                intent.setAction(SIGN_CREATE_AFTER_KILL_BY_SYSTEM);
                return true;
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) &&
                extras.containsKey(Intent.EXTRA_STREAM)) {
            SlideshowModel slideShow = mWorkingMessage.getSlideshow();
            final ArrayList<Parcelable> uris = extras.getParcelableArrayList(Intent.EXTRA_STREAM);
            int currentSlideCount = slideShow != null ? slideShow.size() : 0;
            int importCount = uris.size();
            if (importCount + currentSlideCount > SlideshowEditor.MAX_SLIDE_NUM) {
                importCount = Math.min(SlideshowEditor.MAX_SLIDE_NUM - currentSlideCount,
                        importCount);
            }

            // Attach all the pictures/videos asynchronously off of the UI thread.
            // Show a progress dialog if adding all the slides hasn't finished
            // within half a second.
            final int numberToImport = importCount;
            MmsLog.i(TAG, "numberToImport: " + numberToImport);
            final WorkingMessage msg = mWorkingMessage;
            getAsyncDialog().runAsync(new Runnable() {
                @Override
                public void run() {
                    mToastCountForResizeImage = 0;
                    for (int i = 0; i < numberToImport; i++) {
                        Parcelable uri = uris.get(i);

                        String scheme = ((Uri)uri).getScheme();
                        if (scheme != null && scheme.equals("file")) {
                            // change "file://..." Uri to "Content://...., and attemp to add this attachment"
                            /// M: fix bug ALPS604911, change ContentType when share multi-file from FileManager @{
                            String type = MessageUtils.getContentType((Uri)uri);
                            if (type == null || type.isEmpty()) {
                                type = mimeType;
                            }
                            /// @}
                            addFileAttachment(type, (Uri)uri, true);
                        } else {
                            addAttachment(mimeType, (Uri) uri, true);
                        }
                    }
                    mToastCountForResizeImage = 0;
                    SlideshowModel mSlideShowModel = mWorkingMessage.getSlideshow();
                    if (mSlideShowModel != null && mSlideShowModel.size() > 0) {
                        mWorkingMessage.saveAsMms(false);
                    }
                }
            }, null, R.string.adding_attachments_title);
            intent.setAction(SIGN_CREATE_AFTER_KILL_BY_SYSTEM);
            return true;
        } else if (SIGN_CREATE_AFTER_KILL_BY_SYSTEM.equals(action)) {
        /// @}
            return true;
        }
        return false;
    }

    // mVideoUri will look like this: content://media/external/video/media
    private static final String mVideoUri = Video.Media.getContentUri("external").toString();
    // mImageUri will look like this: content://media/external/images/media
    private static final String mImageUri = Images.Media.getContentUri("external").toString();

    private void addAttachment(String type, Uri uri, boolean append) {
        if (uri != null) {
            // When we're handling Intent.ACTION_SEND_MULTIPLE, the passed in items can be
            // videos, and/or images, and/or some other unknown types we don't handle. When
            // a single attachment is "shared" the type will specify an image or video. When
            // there are multiple types, the type passed in is "*/*". In that case, we've got
            // to look at the uri to figure out if it is an image or video.
            boolean wildcard = "*/*".equals(type);
            MmsLog.i(TAG, "Got send intent mimeType :" + type);
            if (type.startsWith("image/") || (wildcard && uri.toString().startsWith(mImageUri))) {
                addImage(type,uri, append);
            } else if (type.startsWith("video/") ||
                    (wildcard && uri.toString().startsWith(mVideoUri))) {
                addVideo(uri, append);
            }
             /// M: Code analyze 067, Add audio attachment. @{
            else if (type.startsWith("audio/") || type.equals("application/ogg")
                || (wildcard && uri.toString().startsWith(mAudioUri))) {
                addAudio(uri, append);
            /// @}
            /// M: Code analyze 019, Add vcard attachment.  @{
            } else if (type.equalsIgnoreCase("text/x-vcard")) {
                //attachVCardByUri(uri);
                VCardAttachment va = new VCardAttachment(ComposeMessageActivity.this);
                String fileName = va.getVCardFileNameByUri(uri);
                setFileAttachment(fileName, WorkingMessage.VCARD, false);
             /// @}
             /// M: Code analyze 020, Add vcalendar attachment.  @{
            } else if (type.equalsIgnoreCase("text/x-vcalendar")) {
                attachVCalendar(uri);
            } else {
                handleAddAttachmentError(WorkingMessage.UNSUPPORTED_TYPE, R.string.type_audio);
            }
            /// @}
        }
    }

    private String getResourcesString(int id, String mediaName) {
        Resources r = getResources();
        return r.getString(id, mediaName);
    }

    private void drawBottomPanel() {
        // Reset the counter for text editor.
        /// M: @{
        mDrawBottomPanel = false;
        /// M: remove Google default code
        // Reset the counter for text editor.
        //resetCounter();

        if (mWorkingMessage.hasSlideshow()) {
            mBottomPanel.setVisibility(View.GONE);
            mAttachmentEditor.update(mWorkingMessage);
            mAttachmentEditor.requestFocus();
            return;
        }
        mAttachmentEditor.update(mWorkingMessage);
        updateTextEditorHeightInFullScreen();
        /// @}
        mBottomPanel.setVisibility(View.VISIBLE);

        CharSequence text = mWorkingMessage.getText();

        // TextView.setTextKeepState() doesn't like null input.
        if (text != null) {
            mTextEditor.setTextKeepState(text);
            /// M: @{
            try {
                mTextEditor.setSelection(mTextEditor.getText().toString().length());
            } catch (IndexOutOfBoundsException e) {
                mTextEditor.setSelection(mTextEditor.getText().toString().length() - 1);
            }
            /// @}
        } else {
            mTextEditor.setText("");
        }
        /// M: add for character counter
        // Reset the counter for text editor.
        updateCounter(mWorkingMessage.getText(), 0, 0, 0);
    }

    private void drawTopPanel(boolean showSubjectEditor) {
        /// M: why ? @{
        //boolean showingAttachment = mAttachmentEditor.update(mWorkingMessage);
        //mAttachmentEditorScrollView.setVisibility(showingAttachment ? View.VISIBLE : View.GONE);
        boolean isHasSubject = false;
        if (mWorkingMessage == null) {
            isHasSubject = false;
        } else {
            isHasSubject = mWorkingMessage.hasSubject();
        }
        boolean isDeleteMode = false;
        if (mMsgListAdapter == null) {
            isDeleteMode = false;
        } else {
            isDeleteMode = mMsgListAdapter.mIsDeleteMode;
        }
        showSubjectEditor((showSubjectEditor || isHasSubject) && !isDeleteMode);
        mAttachmentEditor.update(mWorkingMessage);
        updateTextEditorHeightInFullScreen();
        /// @}
    }

    //==========================================================
    // Interface methods
    //==========================================================

    @Override
    public void onClick(View v) {
        /// M: Code analyze 028, Before sending message,check the recipients count
        /// and add sim card selection dialog if multi sim cards exist.@{
        /*if ((v == mSendButtonSms || v == mSendButtonMms) && isPreparedForSending()){
            confirmSendMessageIfNeeded();
        }
        */
        /// M: add for ip message send button
        if (v == mSendButtonSms || v == mSendButtonMms || v == mSendButtonIpMessage) {
            if (mSendButtonCanResponse) {
                mSendButtonCanResponse = false;
                if (isPreparedForSending()) {
                    /// M: add for ip message, unread divider
                    mShowUnreadDivider = false;
                    /// M: Since sending message here, why not disable button 'Send'??
                    updateSendButtonState(false);
                    checkRecipientsCount();
                    mUiHandler.sendEmptyMessageDelayed(MSG_RESUME_SEND_BUTTON, RESUME_BUTTON_INTERVAL);
                } else {
                    mSendButtonCanResponse = true;
                    unpreparedForSendingAlert();
                }
            }
        /// @}
        } else if ((v == mRecipientsPicker)) {
             /// M: Code analyze 013, Get contacts from Contact app . @{
             //launchMultiplePhonePicker();
            if (recipientCount() >= RECIPIENTS_LIMIT_FOR_SMS) {
                Toast.makeText(ComposeMessageActivity.this, R.string.cannot_add_recipient, Toast.LENGTH_SHORT).show();
            } else if (IpMessageUtils.getServiceManager(this).isFeatureSupported(FeatureId.CONTACT_SELECTION)) {
                try {
                    Intent intent = new Intent(RemoteActivities.CONTACT);
                    intent.putExtra(RemoteActivities.KEY_REQUEST_CODE, REQUEST_CODE_IPMSG_PICK_CONTACT);
                    intent.putExtra(RemoteActivities.KEY_TYPE, SelectContactType.ALL);
                    IpMessageUtils.startRemoteActivityForResult(this, intent);
                } catch (ActivityNotFoundException e) {
                    misPickContatct = false;
                    Toast.makeText(this, this.getString(R.string.no_application_response), Toast.LENGTH_SHORT).show();
                    MmsLog.e(IPMSG_TAG, e.getMessage());
                }
            } else {
                /// M: fix bug ALPS00444752, dis-clickble when showing ContactPicker
                if (!mShowingContactPicker) {
                    addContacts(-1);
                }
            }
             /// @}
        }
    }

    /// M: fix bug ALPS00444752, set false to enable to Show ContactPicker
    private boolean mShowingContactPicker = false;

    private void launchMultiplePhonePicker() {
        Intent intent = new Intent(Intents.ACTION_GET_MULTIPLE_PHONES);
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setType(Phone.CONTENT_TYPE);
        // We have to wait for the constructing complete.
        ContactList contacts = mRecipientsEditor.constructContactsFromInput(true);
        int urisCount = 0;
        Uri[] uris = new Uri[contacts.size()];
        urisCount = 0;
        for (Contact contact : contacts) {
            if (Contact.CONTACT_METHOD_TYPE_PHONE == contact.getContactMethodType()) {
                    uris[urisCount++] = contact.getPhoneUri();
            }
        }
        if (urisCount > 0) {
            intent.putExtra(Intents.EXTRA_PHONE_URIS, uris);
        }
        startActivityForResult(intent, REQUEST_CODE_PICK);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (event != null) {
            // if shift key is down, then we want to insert the '\n' char in the TextView;
            // otherwise, the default action is to send the message.
            if (!event.isShiftPressed()) {
                if(event.getAction() == KeyEvent.ACTION_DOWN) {
                    return false;
                }
                /// M: Code analyze 028, Before sending message,check the recipients count
                /// and add sim card selection dialog if multi sim cards exist.@{
                if (isPreparedForSending()) {
                    checkRecipientsCount();
                } else {
                    unpreparedForSendingAlert();
                    }
                /// @}
                return true;
            }
            return false;
        }
        /// M: Code analyze 028, Before sending message,check the recipients count
        /// and add sim card selection dialog if multi sim cards exist.@{
        if (isPreparedForSending()) {
            //confirmSendMessageIfNeeded();
            checkRecipientsCount();
        } else {
            unpreparedForSendingAlert();
        }
        /// @}
        return true;
    }

    private final TextWatcher mTextEditorWatcher = new TextWatcher() {
        private int mStart;

        /// M: fix bug ALPS00612093, postDelay for ANR
        private Runnable mUpdateRunnable = new Runnable() {
            public void run() {
                updateSendButtonState();
            }
        };

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            mBeforeTextChangeString = s.toString();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // This is a workaround for bug 1609057. Since onUserInteraction() is
            // not called when the user touches the soft keyboard, we pretend it was
            // called when textfields changes.  This should be removed when the bug
            // is fixed.
            onUserInteraction();

            mWorkingMessage.setText(s);
            /// M: @{
            mAttachmentEditor.onTextChangeForOneSlide(s);
            /// @}

            /// M: fix bug ALPS00612093, postDelay for ANR
            mUiHandler.removeCallbacks(mUpdateRunnable);
            mUiHandler.postDelayed(mUpdateRunnable, 100);

            updateCounter(s, start, before, count);
            mStart = start;
            /// M: @{
            //ensureCorrectButtonHeight();
            /// @}
        }

        @Override
        public void afterTextChanged(Editable s) {
            mLatestText = s;
            setEmoticon(mStart);
            /// M: remove "typing" feature for a while
//            if (mIsIpServiceEnabled && !TextUtils.isEmpty(mChatModeNumber)) {
//                if (TextUtils.isEmpty(mBeforeTextChangeString) && s.length() > 0) {
//                    mIpMsgHandler.post(mSendTypingRunnable);
//                    mLastSendTypingStatusTimestamp = System.currentTimeMillis();
//                } else {
//                    long currentTimeStamp = System.currentTimeMillis();
//                    if ((currentTimeStamp - mLastSendTypingStatusTimestamp) > 3000) {
//                        mIpMsgHandler.post(mSendTypingRunnable);
//                        mLastSendTypingStatusTimestamp = currentTimeStamp;
//                    }
//                }
//
//                mIpMsgHandler.removeCallbacks(mSendStopTypingRunnable);
//                if (s.length() == 0) {
//                    mIpMsgHandler.postDelayed(mSendStopTypingRunnable, 3000);
//                } else {
//                    mIpMsgHandler.postDelayed(mSendStopTypingRunnable, 20000);
//                }
//            }
        }
    };

    /**
     * Ensures that if the text edit box extends past two lines then the
     * button will be shifted up to allow enough space for the character
     * counter string to be placed beneath it.
     */
    /*** M: remove Google default code
    private void ensureCorrectButtonHeight() {
        int currentTextLines = mTextEditor.getLineCount();
        if (currentTextLines <= 2) {
            mTextCounter.setVisibility(View.GONE);
        }
        else if (currentTextLines > 2 && mTextCounter.getVisibility() == View.GONE) {
            // Making the counter invisible ensures that it is used to correctly
            // calculate the position of the send button even if we choose not to
            // display the text.
            mTextCounter.setVisibility(View.INVISIBLE);
        }
    }
    */

    private final TextWatcher mSubjectEditorWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mWorkingMessage.setSubject(s, true);
            if (s != null && TextUtils.getTrimmedLength(s) > 0) {
                /// M: Code analyze 032, According to the message state to update text counter.@{
                mTextCounter.setVisibility(View.GONE);
                /// @}
                // updateSendButtonState();
            }
            // / M: for fixed ALPS00562999,when change subject to null,update send button.@{
            updateSendButtonState();
            // / @}

        }

        @Override
        public void afterTextChanged(Editable s) { }
    };

    //==========================================================
    // Private methods
    //==========================================================

    /**
     * Initialize all UI elements from resources.
     */
    private void initResourceRefs() {
        /// M: Code analyze 004, Set max height for text editor. @{
        mHeightChangedLinearLayout = (HeightChangedLinearLayout) findViewById(R.id.changed_linear_layout);
        mHeightChangedLinearLayout.setLayoutSizeChangedListener(mLayoutSizeChangedListener);
        /// @}
        mMsgListView = (MessageListView) findViewById(R.id.history);
        mMsgListView.setDivider(null);      // no divider so we look like IM conversation.
        mMsgListView.setDividerHeight(getResources().getDimensionPixelOffset(R.dimen.ipmsg_message_list_divier_height));

        // called to enable us to show some padding between the message list and the
        // input field but when the message list is scrolled that padding area is filled
        // in with message content
        mMsgListView.setClipToPadding(false);

        /** M: 4.1  used this code.
        mMsgListView.setOnSizeChangedListener(new OnSizeChangedListener() {
            public void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
                // The message list view changed size, most likely because the keyboard
                // appeared or disappeared or the user typed/deleted chars in the message
                // box causing it to change its height when expanding/collapsing to hold more
                // lines of text.
                smoothScrollToEnd(false, height - oldHeight);
            }
        });
        */

        /// M: turn off children clipping because we draw the border outside of our own
        /// M: bounds at the bottom.  The background is also drawn in code to avoid drawing
        /// M: the top edge.
        mMsgListView.setClipChildren(false);

        mBottomPanel = findViewById(R.id.bottom_panel);
        mTextEditor = (EditText) findViewById(R.id.embedded_text_editor);
        /// M: @{
        //  mTextEditor.setOnEditorActionListener(this);
        /// @}
        mTextEditor.removeTextChangedListener(mTextEditorWatcher);
        mTextEditor.addTextChangedListener(mTextEditorWatcher);
        mTextEditor.setFilters(new InputFilter[] {
                new TextLengthFilter(MmsConfig.getMaxTextLimit())});
        mTextEditor.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                MmsLog.d(IPMSG_TAG, "mTextEditor.onKey(): is keyCode KEYCODE_DEL ?= " + (keyCode == KeyEvent.KEYCODE_DEL)
                    + ", is event ACTION_DOWN ?= " + (event.getAction() == KeyEvent.ACTION_DOWN));
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    String s = ((EditText) v).getText().toString();
                    int index = ((EditText) v).getSelectionStart();
                    MmsLog.d(IPMSG_TAG, "mTextEditor.onKey(): is text empty ?=" + TextUtils.isEmpty(s)
                        + ", index = " + index);
                    if (TextUtils.isEmpty(s) || index == 0) {
                        clearIpMessageDraft();
                    }
                }
                return false;
            }
        });
        mTextEditor.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mShowKeyBoardFromShare) {
                    showSharePanel(false);
                }
                if (mShowKeyBoardFromEmoticon) {
                    showEmoticonPanel(false);
                }
                if (mShowKeyBoardFromShare || mShowKeyBoardFromEmoticon) {
                    updateFullScreenTextEditorHeight();
                }
                return false;
            }
        });

        mTextCounter = (TextView) findViewById(R.id.text_counter);
        mSendButtonMms = (TextView) findViewById(R.id.send_button_mms);
        mSendButtonSms = (ImageButton) findViewById(R.id.send_button_sms);
        mSendButtonMms.setOnClickListener(this);
        mSendButtonSms.setOnClickListener(this);
        mTopPanel = findViewById(R.id.recipients_subject_linear);
        mTopPanel.setFocusable(false);
        mAttachmentEditor = (AttachmentEditor) findViewById(R.id.attachment_editor);
        mAttachmentEditor.setHandler(mAttachmentEditorHandler);
        //mAttachmentEditorScrollView = findViewById(R.id.attachment_editor_scroll_view);
        mQuickContact = (MmsQuickContactBadge) findViewById(R.id.avatar);
        mWallPaper = (ImageView) findViewById(R.id.wall_paper);
    }

    private void confirmDeleteDialog(OnClickListener listener, boolean locked) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        /// M: Code analyze 027,Add for deleting one message.@{
        // Set different title and icon for locked message.
        builder.setTitle(locked ? R.string.confirm_dialog_locked_title :
            R.string.confirm_dialog_title);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        /// @}
        builder.setCancelable(true);
        builder.setMessage(locked ? R.string.confirm_delete_locked_message :
                    R.string.confirm_delete_message);
        builder.setPositiveButton(R.string.delete, listener);
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    void undeliveredMessageDialog(long date) {
        String body;

        if (date >= 0) {
            body = getString(R.string.undelivered_msg_dialog_body,
                    MessageUtils.formatTimeStampString(this, date));
        } else {
            // FIXME: we can not get sms retry time.
            body = getString(R.string.undelivered_sms_dialog_body);
        }

        Toast.makeText(this, body, Toast.LENGTH_LONG).show();
    }

    private void startMsgListQuery() {
        startMsgListQuery(MESSAGE_LIST_QUERY_TOKEN, 500);
    }

    private void startMsgListQuery(final int token, int delay) {
        MmsLog.d(TAG, "startMsgListQuery, timeout=" + delay + ", isImportantThread = " + mIsImportantThread);
        /// M: Code analyze 010, Support dirtory mode. @{
        if (MmsConfig.getMmsDirMode()) {
            return;
        }
        /// @}
        if (isRecipientsEditorVisible()) {
            return;
        }
        final Uri conversationUri = mConversation.getUri();
        MmsLog.d(TAG, "startMsgListQuery, uri=" + conversationUri);
        if (conversationUri == null) {
            log("##### startMsgListQuery: conversationUri is null, bail!");
            return;
        }

        final long threadId = mConversation.getThreadId();
        if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("startMsgListQuery for " + conversationUri + ", threadId=" + threadId);
        }

        // Cancel any pending queries
        /// M: add for ip message, query unread message
        if (token == MESSAGE_LIST_QUERY_TOKEN) {
            mBackgroundQueryHandler.cancelOperation(MESSAGE_LIST_UNREAD_QUERY_TOKEN);

        }
        mBackgroundQueryHandler.cancelOperation(MESSAGE_LIST_QUERY_TOKEN);
        try {
            /// M: add for ipmessage
            if (token == MESSAGE_LIST_QUERY_TOKEN) {
                /// M: add for ip message, query unread message
                if (mShowUnreadDivider) {
                    final String unreadSelection = "read = 0";
                    mBackgroundQueryHandler.postDelayed(new Runnable() {
                        public void run() {
                            mBackgroundQueryHandler.startQuery(
                                    MESSAGE_LIST_UNREAD_QUERY_TOKEN, threadId, conversationUri,
                                    PROJECTION, unreadSelection, null, null);
                        }
                    }, delay);
                }
                final String order = mShowUnreadDivider ? "read DESC" : null;
    //            long smsQueryTimestamp = mEnterImportantTimestamp;
    //            long mmsQueryTimestamp = mEnterImportantTimestamp / 1000;
                final String importantSelection = mIsImportantThread ? "(locked = 1 OR read = 0 OR date >= "
                    + mEnterImportantTimestamp + ")" : null;
                MmsLog.d(TAG, "startMsgListQuery(): postDelayed for " + delay + "ms, threadId = " + threadId
                    + ", conversationUri = " + conversationUri + ", importantSelection = " + importantSelection
                    + ", order = " + order);
                mBackgroundQueryHandler.postDelayed(new Runnable() {
                    public void run() {
                        mBackgroundQueryHandler.startQuery(
                                MESSAGE_LIST_QUERY_TOKEN, threadId, conversationUri,
                                PROJECTION, importantSelection, null, order);
                    }
                }, delay);
                 return;
            }

            // Kick off the new query
            /// M: @{
            // mBackgroundQueryHandler.startQuery(
            mBackgroundQueryHandler.postDelayed(new Runnable() {
                public void run() {
                    /// M: If no listener, no need query anymore @{
                        MmsLog.d(TAG, "mListQueryRunnable, to query, " + "activity=" + ComposeMessageActivity.this);
                        if (mMsgListAdapter.getOnDataSetChangedListener() == null) {
                            MmsLog.d(TAG, "mListQueryRunnable, no listener");
                            return;
                        }
                    /// @}
                    mBackgroundQueryHandler.startQuery(
                            token, 
                            threadId /* cookie */,
                            conversationUri,
                            PROJECTION, 
                            null, null, null);
                }
            }, delay);
            /// @}
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private void initMessageList() {
        if (mMsgListAdapter != null) {
            return;
        }

        String highlightString = getIntent().getStringExtra("highlight");
        Pattern highlight = highlightString == null
            ? null
            : Pattern.compile("\\b" + Pattern.quote(highlightString), Pattern.CASE_INSENSITIVE);

        // Initialize the list adapter with a null cursor.
        mMsgListAdapter = new MessageListAdapter(this, null, mMsgListView, true, highlight);
        /// M: Code analyze 010, Support dirtory mode. @{
        if (MmsConfig.getMmsDirMode()) {
            mMsgListView.setVisibility(View.GONE);
            return;
        }
        /// @}
        mMsgListAdapter.setOnDataSetChangedListener(mDataSetChangedListener);
        mMsgListAdapter.setMsgListItemHandler(mMessageListItemHandler);
        mMsgListView.setAdapter(mMsgListAdapter);
        mMsgListView.setItemsCanFocus(false);
        mMsgListView.setVisibility(View.VISIBLE);
        mMsgListView.setOnCreateContextMenuListener(mMsgListMenuCreateListener);
        mMsgListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MmsLog.d(MessageListItem.TAG_DIVIDER, "OnItemClickListener(): view = " + view);
                if (view != null) {
                    ((MessageListItem) view).onMessageListItemClick();
                }
            }
        });
        /// M: Code analyze 050, Add scroll listener and touch listener for MessageListView.@{
        mMsgListView.setOnScrollListener(mScrollListener);
        mMsgListView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideInputMethod();
                return false;
            }
        });
        /// @}
        /// M: add for ip message, online divider
        if (MmsConfig.getIpMessagServiceId(this) == IpMessageServiceId.ISMS_SERVICE
                && mIsIpMessageRecipients && mConversation.getRecipients().size() == 1) {
            mMsgListAdapter.setOnlineDividerString(getOnlineDividerString(IpMessageUtils.getContactManager(this)
                .getStatusByNumber(mConversation.getRecipients().get(0).getNumber())));
        }
    }

    private void loadDraft() {
        if (mWorkingMessage.isWorthSaving()) {
            MmsLog.w(TAG, "loadDraft() called with non-empty working message");
            return;
        }

        /// M: add for load IP message draft.@{
        if (IpMessageUtils.getIpMessagePlugin(this).isActualPlugin()) {
            if (mIpMessageDraft != null) {
                MmsLog.w(TAG, "loadDraft() called with non-empty IP message draft");
                return;
            } else if (loadIpMessagDraft()) {
                return;
            }
        }
        /// @}

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("loadDraft() call WorkingMessage.loadDraft");
        }

        /// M:[Just Comment] 4.1 Code.  There may be a bug.
        mWorkingMessage = WorkingMessage.loadDraft(this, mConversation,
                new Runnable() {
                    @Override
                    public void run() {
                        drawTopPanel(false);
                        drawBottomPanel();
                        updateSendButtonState();
                    }
                });
        mContentResolver.registerContentObserver(
                Mms.CONTENT_URI, true, mDraftChangeObserver);
    }

    private void saveDraft(boolean isStopping) {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            LogTag.debug("saveDraft");
        }
        /// M: add for save IP message draft.@{
        if (IpMessageUtils.getIpMessagePlugin(this).isActualPlugin() && isFinishing()) {
            if (mIpMessageDraft != null) {
                saveIpMessageDraft();
                return;
            }
            if (mIpMessageDraftId > 0) {
                IpMessageUtils.deleteIpMessageDraft(this, mConversation, mWorkingMessage);
            }
        }
        /// @}
        // TODO: Do something better here.  Maybe make discard() legal
        // to call twice and make isEmpty() return true if discarded
        // so it is caught in the clause above this one?
        if (mWorkingMessage.isDiscarded()) {
            return;
        }

        if (!mWaitingForSubActivity &&
                (!mWorkingMessage.isWorthSaving() && mIpMessageDraft == null) &&
                (!isRecipientsEditorVisible() || recipientCount() == 0)) {
            if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                log("not worth saving, discard WorkingMessage and bail");
            }
            mWorkingMessage.discard(false);
            return;
        }

        mWorkingMessage.saveDraft(isStopping);

        if (mToastForDraftSave) {
            Toast.makeText(this, R.string.message_saved_as_draft,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isPreparedForSending() {
        /// M: Code analyze 029,Add sim card state as a ready condition. @{
        /*
        int recipientCount = recipientCount();

        return recipientCount > 0 && recipientCount <= MmsConfig.getRecipientLimit() &&
            (mWorkingMessage.hasAttachment() ||
                    mWorkingMessage.hasText() ||
                    mWorkingMessage.hasSubject());
        */
        if (isRecipientsEditorVisible()) {
            String recipientText = mRecipientsEditor.getText() == null ? "" : mRecipientsEditor.getText().toString();

            /// M: add for ip message
            return mSimCount > 0 && !TextUtils.isEmpty(recipientText)
                    && (mWorkingMessage.hasAttachment() || mWorkingMessage.hasText()
                            || mWorkingMessage.hasSubject() || mIpMessageDraft != null);
        } else {
            return mSimCount > 0 && (mWorkingMessage.hasAttachment() || mWorkingMessage.hasText()
                            || mWorkingMessage.hasSubject() || mIpMessageDraft != null);
        }
        /// @}
    }

    private int recipientCount() {
        int recipientCount;

        // To avoid creating a bunch of invalid Contacts when the recipients
        // editor is in flux, we keep the recipients list empty.  So if the
        // recipients editor is showing, see if there is anything in it rather
        // than consulting the empty recipient list.
        if (isRecipientsEditorVisible()) {
            recipientCount = mRecipientsEditor.getRecipientCount();
        } else {
            recipientCount = getRecipients().size();
        }
        return recipientCount;
    }

    private void sendMessage(boolean bCheckEcmMode) {
      /// M: @{
        if (mWorkingMessage.requiresMms() && (mWorkingMessage.hasSlideshow() || mWorkingMessage.hasAttachment())) {
            /// M: @{for Fixed ALPS00528150
            int attachmentSize = 0;
            int messageSize = 0;
            IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance) MmsPluginManager
                    .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);
            if (!(mMmsAttachmentEnhancePlugin != null && mMmsAttachmentEnhancePlugin
                    .isSupportAttachmentEnhance() == true)) {
                MmsLog.d(TAG, "Compose.sendMessage(): isSupportAttachmentEnhance is false");
                for (FileAttachmentModel fileAttachment : mWorkingMessage.getSlideshow()
                        .getAttachFiles()) {
                    attachmentSize += fileAttachment.getAttachSize();
                }
            }
            messageSize = mWorkingMessage.getCurrentMessageSize() + attachmentSize;
            MmsLog.d(TAG, "Compose.sendMessage(): messageSize=" + messageSize);
            if (messageSize > MmsConfig.getUserSetMmsSizeLimit(true)) {
            ///@}
                MessageUtils.showErrorDialog(ComposeMessageActivity.this,
                        getResourcesString(R.string.exceed_message_size_limitation),
                        getResourcesString(R.string.exceed_message_size_limitation));
                updateSendButtonState();
                return;
            }
        }
        /// @}
        if (bCheckEcmMode) {
            // TODO: expose this in telephony layer for SDK build
            String inEcm = SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE);
            if (Boolean.parseBoolean(inEcm)) {
                try {
                    startActivityForResult(
                            new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                            REQUEST_CODE_ECM_EXIT_DIALOG);
                    return;
                } catch (ActivityNotFoundException e) {
                    // continue to send message
                    MmsLog.e(TAG, "Cannot find EmergencyCallbackModeExitDialog", e);
                }
            }
        }

        /// M: Fix bug ALPS00407718
        if (mExitOnSent) {
            hideKeyboard();
        }
        /// M: Code analyze 011, use another method for performance.(update mDebugRecipients)@{
        ContactList contactList = isRecipientsEditorVisible() ?
                mRecipientsEditor.constructContactsFromInputWithLimit(false, 0) : getRecipients();
        mDebugRecipients = contactList.serialize();
        /// @}

        if (!mSendingMessage) {
            if (LogTag.SEVERE_WARNING) {
                String sendingRecipients = mConversation.getRecipients().serialize();
                if (!sendingRecipients.equals(mDebugRecipients)) {
                    String workingRecipients = mWorkingMessage.getWorkingRecipients();
                    if (!mDebugRecipients.equals(workingRecipients)) {
                        LogTag.warnPossibleRecipientMismatch("ComposeMessageActivity.sendMessage" +
                                " recipients in window: \"" +
                                mDebugRecipients + "\" differ from recipients from conv: \"" +
                                sendingRecipients + "\" and working recipients: " +
                                workingRecipients, this);
                    }
                }
                sanityCheckConversation();
            }

            // send can change the recipients. Make sure we remove the listeners first and then add
            // them back once the recipient list has settled.
            removeRecipientsListeners();

            /// M:the method is extend to support gemini @{
            mWorkingMessage.send(mDebugRecipients, mSelectedSimId);
            MmsLog.d(TAG, "Compose.sendMessage(): after sendMessage. mConversation.ThreadId=" + mConversation.getThreadId()
                    + ", MessageCount=" + mConversation.getMessageCount());
            /// @}
            /** M:
             *   If message count is 0, it should be a new message.
             *   After tap send button, the sent message will have draft flag for a short time.
             *   That means, the message count will be 0 for a short time.
             *   If user tap home key in this short time, it will change the conversation id to 0 in the method savedraft().
             *   When the screen is back to Message Composer, it will query database with thread(conversation) id 0.
             *   So, it will query for nothing. The screen is always blank.
             *   Fix this issue by force to set message count with 1.
             */
            if (mConversation.getMessageCount() == 0) {
                mConversation.setMessageCount(1);
            }
            /// M: @{
            mWaitingForSendMessage = true;
            /// M: when tap fail icon, don't add recipients
            isInitRecipientsEditor = false; 
            mMsgListView.setVisibility(View.VISIBLE);
            /// @}

            mSentMessage = true;
            mSendingMessage = true;
            addRecipientsListeners();

            mScrollOnSend = true;   // in the next onQueryComplete, scroll the list to the end.
            ///M: message has been sent via common message , so change the sign.
            mJustSendMsgViaCommonMsgThisTime = false;
        }
        // But bail out if we are supposed to exit after the message is sent.
        if (mExitOnSent) {
            ///M: add for guarantee the message sent. @{
            mUiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 150);
            /// @}
        }
    }

    private void resetMessage() {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("resetMessage");
        }
        updateTitle(mConversation.getRecipients());

        if (mIpMessageForSend == null || mIsClearTextDraft) {
            // Make the attachment editor hide its view.
            mAttachmentEditor.hideView();
//            mAttachmentEditorScrollView.setVisibility(View.GONE);
            ///M: change the order between Editor.requestFocus & showSubjectEditor
            /// for fix issue ALPS00569570 @{
            // Focus to the text editor.
            mTextEditor.requestFocus();

            // Hide the subject editor.
            showSubjectEditor(false);
            /// @}

            // We have to remove the text change listener while the text editor gets cleared and
            // we subsequently turn the message back into SMS. When the listener is listening while
            // doing the clearing, it's fighting to update its counts and itself try and turn
            // the message one way or the other.
            mTextEditor.removeTextChangedListener(mTextEditorWatcher);

            // Clear the text box.
            TextKeyListener.clear(mTextEditor.getText());

            mWorkingMessage.clearConversation(mConversation, false);
            mWorkingMessage = WorkingMessage.createEmpty(this);
            mWorkingMessage.setConversation(mConversation);

            /// M: add for ip message, clear IP message draft
            mIpMessageDraftId = 0;
            clearIpMessageDraft();
            /// M: reset emoticon counter
            mEmoticonNumber = 0;
        }

        if (IpMessageUtils.getIpMessagePlugin(this).isActualPlugin()) {
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT
                    && (mMessageSimId != Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK
                            && mMessageSimId != Settings.System.DEFAULT_SIM_NOT_SET)) {
                mIsMessageDefaultSimIpServiceEnabled = MmsConfig.isServiceEnabled(this, (int) mMessageSimId);
            } else {
                mIsMessageDefaultSimIpServiceEnabled = MmsConfig.isServiceEnabled(this);
            }
            
        }

        /// M: add for ip message, update online divider
        if (null != mConversation.getRecipients() && mConversation.getRecipients().size() == 1) {
            mIsIpMessageRecipients = isIpMessageRecipients(mConversation.getRecipients().get(0).getNumber());
            if (mIsMessageDefaultSimIpServiceEnabled && isNetworkConnected(getApplicationContext())
                    && mIsIpMessageRecipients) {
                if (!TextUtils.isEmpty(mChatModeNumber)
                        && isChatModeNumber(mConversation.getRecipients().get(0).getNumber())) {
                    MmsLog.d(IPMSG_TAG, "resetMessage(): update mChatModeNumber from " + mChatModeNumber + " to "
                        + mConversation.getRecipients().get(0).getNumber());
                    IpMessageUtils.getChatManager(this).exitFromChatMode(mChatModeNumber);
                    mChatModeNumber = mConversation.getRecipients().get(0).getNumber();
                    IpMessageUtils.getChatManager(this).enterChatMode(mChatModeNumber);
                } else if (TextUtils.isEmpty(mChatModeNumber)) {
                    MmsLog.d(IPMSG_TAG, "resetMessage(): update mChatModeNumber after send message, mChatModeNumber = "
                        + mChatModeNumber);
                    mChatModeNumber = mConversation.getRecipients().get(0).getNumber();
                    IpMessageUtils.getChatManager(this).enterChatMode(mChatModeNumber);
                }
            }
        }

        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT && (mMessageSimId == Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK
                || mMessageSimId == Settings.System.DEFAULT_SIM_NOT_SET)) {
            mSelectedSimId = 0;
        }

        hideRecipientEditor();
        drawBottomPanel();

        // "Or not", in this case.
        updateSendButtonState();

        if (mIpMessageForSend == null || mIsClearTextDraft) {
            // Our changes are done. Let the listener respond to text changes once again.
            mTextEditor.removeTextChangedListener(mTextEditorWatcher);
            mTextEditor.addTextChangedListener(mTextEditorWatcher);
        }

        // Close the soft on-screen keyboard if we're in landscape mode so the user can see the
        // conversation.
        if (mIsLandscape) {
            hideKeyboard();
        }

        mLastRecipientCount = 0;
        mSendingMessage = false;
        invalidateOptionsMenu();
        /// M: update list, this must put after hideRecipientEditor(); to avoid a bug.
        startMsgListQuery(MESSAGE_LIST_QUERY_TOKEN,0);
   }

    private void hideKeyboard() {
        MmsLog.d(TAG, "hideKeyboard()");
        InputMethodManager inputMethodManager =
            (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mTextEditor.getWindowToken(), 0);
        mIsKeyboardOpen = false;
    }

    private void updateSendButtonState() {
        boolean enable = false;
        MmsLog.v(TAG, "updateSendButtonState(): isPreparedForSending = " + isPreparedForSending());
        if (isPreparedForSending()) {
            /// M: Code analyze 049, Update send button or attachment editor state.@{
            MmsLog.v(TAG, "updateSendButtonState(): mSimCount = " + mSimCount + ", Gemini is support = "
                + EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT);
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT == true && mSimCount > 0) {
                // When the type of attachment is slideshow, we should
                // also hide the 'Send' button since the slideshow view
                // already has a 'Send' button embedded.
                MmsLog.v(TAG, "updateSendButtonState(): hasSlideshow = " + mWorkingMessage.hasSlideshow());
                if (!mWorkingMessage.hasSlideshow()) {
                    enable = true;
                } else {
                    mAttachmentEditor.setCanSend(true);
                }
            } else {
                /** M: MTK Encapsulation ITelephony */
                // ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                EncapsulatedTelephonyService phone = EncapsulatedTelephonyService.getInstance();
                if (phone != null) {
                    try {
                         /// M: check SIM state
                        if (phone.isSimInsert(0)) {
                            if (!mWorkingMessage.hasSlideshow()) {
                                enable = true;
                            } else {
                                mAttachmentEditor.setCanSend(true);
                            }
                        } else {
                            mAttachmentEditor.setCanSend(false);
                        }
                    } catch (RemoteException e) {
                        MmsLog.w(TAG, "compose.updateSendButton()_singleSIM");
                    }
                }
            }
        } else {
        /// @}
        mAttachmentEditor.setCanSend(false);
        }

        boolean requiresMms = mWorkingMessage.requiresMms();
        View sendButton = showSmsOrMmsSendButton(requiresMms);
        if (!requiresMms && (recipientCount() > MmsConfig.getSmsRecipientLimit())) {
            enable = false;
        }
        sendButton.setEnabled(enable);
        sendButton.setFocusable(enable);
    }

    private long getMessageDate(Uri uri) {
        if (uri != null) {
            Cursor cursor = SqliteWrapper.query(this, mContentResolver,
                    uri, new String[] { Mms.DATE }, null, null, null);
            if (cursor != null) {
                try {
                    if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                        return cursor.getLong(0) * 1000L;
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return NO_DATE_FOR_DIALOG;
    }

/// M: @{
//    private void initActivityState(Intent intent) {
//        // If we have been passed a thread_id, use that to find our conversation.
//        long threadId = intent.getLongExtra("thread_id", 0);
//        if (threadId > 0) {
//            if (LogTag.VERBOSE) log("get mConversation by threadId " + threadId);
//            mConversation = Conversation.get(this, threadId, false);
//        } else {
//            Uri intentData = intent.getData();
//            if (intentData != null) {
//                // try to get a conversation based on the data URI passed to our intent.
//                if (LogTag.VERBOSE) log("get mConversation by intentData " + intentData);
//                mConversation = Conversation.get(this, intentData, false);
//                mWorkingMessage.setText(getBody(intentData));
//            } else {
//                // special intent extra parameter to specify the address
//                String address = intent.getStringExtra("address");
//                if (!TextUtils.isEmpty(address)) {
//                    if (LogTag.VERBOSE) log("get mConversation by address " + address);
//                    mConversation = Conversation.get(this, ContactList.getByNumbers(address,
//                            false /* don't block */, true /* replace number */), false);
//                } else {
//                    if (LogTag.VERBOSE) log("create new conversation");
//                    mConversation = Conversation.createNew(this);
//                }
//            }
//        }
//        addRecipientsListeners();
//
//        mExitOnSent = intent.getBooleanExtra("exit_on_sent", false);
//        if (intent.hasExtra("sms_body")) {
//            mWorkingMessage.setText(intent.getStringExtra("sms_body"));
//        }
//        mWorkingMessage.setSubject(intent.getStringExtra("subject"), false);
//    }
    private void initActivityState(Bundle bundle) {
        Intent intent = getIntent();
        /// M: Code analyze 033, Save some useful information in order to restore the draft when
        /// activity restarting.@{
        mIsTooManyRecipients = false;
        if (bundle != null) {
            mCompressingImage = bundle.getBoolean("compressing_image", false);
            String recipientsStr = bundle.getString("recipients");
            MmsLog.d(TAG, "get recipinents from bundle, recipientsStr=" + recipientsStr);
            int recipientCount = 0;
            if(recipientsStr != null){
                recipientCount = recipientsStr.split(";").length;
                mConversation = Conversation.get(getApplicationContext(),
                    ContactList.getByNumbers(this, recipientsStr,
                            false /* don't block */, true /* replace number */), false);
            } else {
                Long threadId = bundle.getLong("thread", 0);
                MmsLog.d(TAG, "get threadId from bundle, threadId=" + threadId);
                mConversation = Conversation.get(getApplicationContext(), threadId, false);
            }
            // M: fix bug ALPS00352078
            mWorkingMessage.setConversation(mConversation);
            
            mExitOnSent = bundle.getBoolean("exit_on_sent", false);
            mWorkingMessage.readStateFromBundle(bundle);
            /// M: Code analyze 010, Support dirtory mode. @{
            if (MmsConfig.getMmsDirMode()) {
                mExitOnSent = true;
            }
            /// @}
            /// M: if there is ipmessage draft, we need the threadid to get it.
            boolean hasIpMessageDraft = bundle.getBoolean("saved_ipmessage_draft", false);
            if (!mCompressingImage && mConversation.hasDraft() && mConversation.getMessageCount() == 0 && !hasIpMessageDraft) {
                if (!mWorkingMessage.requiresMms()) {
                    MmsLog.w(TAG, "delete sms draft");
                    mWorkingMessage.asyncDeleteDraftSmsMessage(mConversation);
                } else {
                    if (mWorkingMessage.getSlideshow() != null && mWorkingMessage.getSlideshow().size() == 1
                            && !mWorkingMessage.getSlideshow().get(0).hasAudio()
                            && !mWorkingMessage.getSlideshow().get(0).hasImage()
                            && !mWorkingMessage.getSlideshow().get(0).hasVideo()
                            && mWorkingMessage.getSlideshow().sizeOfFilesAttach() == 0) {
                        mWorkingMessage.asyncDeleteDraftMmsMessage(mConversation);
                        mWorkingMessage.removeAllFileAttaches();
                        mWorkingMessage.removeAttachment(false);
                    }
                }
                mWorkingMessage.clearConversation(mConversation, true);
            }
            if (recipientCount > RECIPIENTS_LIMIT_FOR_SMS) {
                mIsTooManyRecipients = true;
            }
            mCompressingImage = false;
            /// @}
            return;
        }
        /// M: Code analyze 019, Add vcard attachment.  @{
        String vCardContactsIds = intent.getStringExtra("multi_export_contacts");
        long[] contactsIds = null;
        if (vCardContactsIds != null && !vCardContactsIds.equals("")) {
            String[] vCardConIds = vCardContactsIds.split(",");
            MmsLog.e(TAG, "ComposeMessage.initActivityState(): vCardConIds.length" + vCardConIds.length);
            contactsIds = new long[vCardConIds.length];
            try {
                for (int i = 0; i < vCardConIds.length; i++) {
                    contactsIds[i] = Long.parseLong(vCardConIds[i]);
                }
            } catch (NumberFormatException e) {
                contactsIds = null;
            }
        }
        /// @}
        // If we have been passed a thread_id, use that to find our conversation.
        long threadId = intent.getLongExtra("thread_id", 0);
        if (threadId > 0) {
            if (LogTag.VERBOSE) log("get mConversation by threadId " + threadId);
            mConversation = Conversation.get(getApplicationContext(), threadId, false);
        /// M: Code analyze 019, Add vcard attachment.  @{
        } else if (contactsIds != null && contactsIds.length > 0) {
            //addTextVCard(contactsIds);
            addTextVCardAsync(contactsIds);
            mConversation = Conversation.createNew(getApplicationContext());
            return;
        /// @}
        } else {
            Uri intentData = intent.getData();
            /// M: Code analyze 034, If intent is SEND,just create a new empty thread,
            /// otherwise Conversation.get() will throw exception.
            String action = intent.getAction();
            if (intentData != null && (TextUtils.isEmpty(action) ||
                            !action.equals(Intent.ACTION_SEND))) {
                /// M: group-contact send message
                // try to get a conversation based on the data URI passed to our intent.
                if (intentData.getPathSegments().size() < 2) {
                    mConversation = mConversation.get(getApplicationContext(),ContactList.getByNumbers(
                           getStringForMultipleRecipients(Conversation.getRecipients(intentData)),
                                 false /* don't block */, true /* replace number */),false);
                } else {
                    mConversation = Conversation.get(getApplicationContext(), intentData, false);
                }
                /// @}
                mWorkingMessage.setText(getBody(intentData));
            } else {
                // special intent extra parameter to specify the address
                String address = intent.getStringExtra("address");
                if (!TextUtils.isEmpty(address)) {
                    if (LogTag.VERBOSE) log("get mConversation by address " + address);
                    mConversation = Conversation.get(getApplicationContext(), ContactList.getByNumbers(address,
                            false /* don't block */, true /* replace number */), false);
                } else {
                    if (LogTag.VERBOSE) log("create new conversation");
                    mConversation = Conversation.createNew(getApplicationContext());
                }
            }
        }
        //addRecipientsListeners();
        updateThreadIdIfRunning();

        mExitOnSent = intent.getBooleanExtra("exit_on_sent", false);
        if (intent.hasExtra("sms_body")) {
            /// M: Code analyze 017, Handle forwarded message.(see:forwardMessage()).
            /// Forward sms message and set sms body.@{
            String sms_body = intent.getStringExtra("sms_body");
            if (sms_body != null && sms_body.length() > MmsConfig.getMaxTextLimit()) {
                mWorkingMessage.setText(sms_body.subSequence(0, MmsConfig.getMaxTextLimit()));
            } else {
                mWorkingMessage.setText(sms_body);
            }
            /// @}
        }
        mWorkingMessage.setSubject(intent.getStringExtra("subject"), false);

        /// M: Code analyze 010, Support dirtory mode. @{
        if (MmsConfig.getMmsDirMode()) {
            mExitOnSent = true;
        }
        /// @}
        /// M: Code analyze 048, Add this can send msg from a marked sim card
        /// which is delivered in Intent.@{
        send_sim_id = intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY,-1);
        MmsLog.d(TAG, "init get simId from intent = " + send_sim_id);
        /// @}
    }

    private void initFocus() {
        if (!mIsKeyboardOpen) {
            return;
        }

        // If the recipients editor is visible, there is nothing in it,
        // and the text editor is not already focused, focus the
        // recipients editor.
        if (isRecipientsEditorVisible()
                && TextUtils.isEmpty(mRecipientsEditor.getText())
                && !mTextEditor.isFocused()
                /// M: fix bug ALPS00451156, mRecipientsEditor.populate is optimized by MTK, which is
                /// asynchronized, and it cause mRecipientsEditor.getText() return null. So we double
                /// check mConversation.getRecipients().
                && mConversation.getRecipients().isEmpty()) {
            mRecipientsEditor.requestFocus();
            return;
        }

        // If we decided not to focus the recipients editor, focus the text editor.
        mTextEditor.requestFocus();
    }

    private final MessageListAdapter.OnDataSetChangedListener
                    mDataSetChangedListener = new MessageListAdapter.OnDataSetChangedListener() {
        @Override
        public void onDataSetChanged(MessageListAdapter adapter) {
            mPossiblePendingNotification = true;
        }

        @Override
        public void onContentChanged(MessageListAdapter adapter) {
        /// M: @{
            if (mMsgListAdapter != null &&
                mMsgListAdapter.getOnDataSetChangedListener() != null) {
                MmsLog.d(TAG, "OnDataSetChangedListener is not cleared");
                /// M: add for ip message, unread divider
                mShowUnreadDivider = false;
                startMsgListQuery();
            } else {
                MmsLog.d(TAG, "OnDataSetChangedListener is cleared");
            }
        /// @}
        }
    };

    private void checkPendingNotification() {
        if (mPossiblePendingNotification && hasWindowFocus()) {
            /// M: add for ip message, remove mark as read
//            mConversation.markAsRead();
            mPossiblePendingNotification = false;
        }
    }

    /**
     * smoothScrollToEnd will scroll the message list to the bottom if the list is already near
     * the bottom. Typically this is called to smooth scroll a newly received message into view.
     * It's also called when sending to scroll the list to the bottom, regardless of where it is,
     * so the user can see the just sent message. This function is also called when the message
     * list view changes size because the keyboard state changed or the compose message field grew.
     *
     * @param force always scroll to the bottom regardless of current list position
     * @param listSizeChange the amount the message list view size has vertically changed
     */
    private void smoothScrollToEnd(boolean force, int listSizeChange) {
        int lastItemVisible = mMsgListView.getLastVisiblePosition();
        int lastItemInList = mMsgListAdapter.getCount() - 1;
        if (lastItemVisible < 0 || lastItemInList < 0) {
            if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                MmsLog.v(TAG, "smoothScrollToEnd: lastItemVisible=" + lastItemVisible +
                        ", lastItemInList=" + lastItemInList +
                        ", mMsgListView not ready");
            }
            return;
        }

        View lastChildVisible =
                mMsgListView.getChildAt(lastItemVisible - mMsgListView.getFirstVisiblePosition());
        int lastVisibleItemBottom = 0;
        int lastVisibleItemHeight = 0;
        if (lastChildVisible != null) {
            lastVisibleItemBottom = lastChildVisible.getBottom();
            lastVisibleItemHeight = lastChildVisible.getHeight();
        }

        if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            MmsLog.v(TAG, "smoothScrollToEnd newPosition: " + lastItemInList +
                    " mLastSmoothScrollPosition: " + mLastSmoothScrollPosition +
                    " first: " + mMsgListView.getFirstVisiblePosition() +
                    " lastItemVisible: " + lastItemVisible +
                    " lastVisibleItemBottom: " + lastVisibleItemBottom +
                    " lastVisibleItemBottom + listSizeChange: " +
                    (lastVisibleItemBottom + listSizeChange) +
                    " mMsgListView.getHeight() - mMsgListView.getPaddingBottom(): " +
                    (mMsgListView.getHeight() - mMsgListView.getPaddingBottom()) +
                    " listSizeChange: " + listSizeChange);
        }
        // Only scroll if the list if we're responding to a newly sent message (force == true) or
        // the list is already scrolled to the end. This code also has to handle the case where
        // the listview has changed size (from the keyboard coming up or down or the message entry
        // field growing/shrinking) and it uses that grow/shrink factor in listSizeChange to
        // compute whether the list was at the end before the resize took place.
        // For example, when the keyboard comes up, listSizeChange will be negative, something
        // like -524. The lastChild listitem's bottom value will be the old value before the
        // keyboard became visible but the size of the list will have changed. The test below
        // add listSizeChange to bottom to figure out if the old position was already scrolled
        // to the bottom. We also scroll the list if the last item is taller than the size of the
        // list. This happens when the keyboard is up and the last item is an mms with an
        // attachment thumbnail, such as picture. In this situation, we want to scroll the list so
        // the bottom of the thumbnail is visible and the top of the item is scroll off the screen.
        int listHeight = mMsgListView.getHeight();
        boolean lastItemTooTall = lastVisibleItemHeight > listHeight;
        boolean willScroll = force ||
                ((listSizeChange != 0 || lastItemInList != mLastSmoothScrollPosition) &&
                lastVisibleItemBottom + listSizeChange <=
                    listHeight - mMsgListView.getPaddingBottom());
        if (willScroll || (lastItemTooTall && lastItemInList == lastItemVisible)) {
            if (Math.abs(listSizeChange) > SMOOTH_SCROLL_THRESHOLD) {
                // When the keyboard comes up, the window manager initiates a cross fade
                // animation that conflicts with smooth scroll. Handle that case by jumping the
                // list directly to the end.
                if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                    MmsLog.v(TAG, "keyboard state changed. setSelection=" + lastItemInList);
                }
                if (lastItemTooTall) {
                    // If the height of the last item is taller than the whole height of the list,
                    // we need to scroll that item so that its top is negative or above the top of
                    // the list. That way, the bottom of the last item will be exposed above the
                    // keyboard.
                    mMsgListView.setSelectionFromTop(lastItemInList,
                            listHeight - lastVisibleItemHeight);
                } else {
                    mMsgListView.setSelection(lastItemInList);
                }
            } else if (lastItemInList - lastItemVisible > MAX_ITEMS_TO_INVOKE_SCROLL_SHORTCUT) {
                if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                    MmsLog.v(TAG, "too many to scroll, setSelection=" + lastItemInList);
                }
                mMsgListView.setSelection(lastItemInList);
            } else {
                if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                    MmsLog.v(TAG, "smooth scroll to " + lastItemInList);
                }
                if (lastItemTooTall) {
                    // If the height of the last item is taller than the whole height of the list,
                    // we need to scroll that item so that its top is negative or above the top of
                    // the list. That way, the bottom of the last item will be exposed above the
                    // keyboard. We should use smoothScrollToPositionFromTop here, but it doesn't
                    // seem to work -- the list ends up scrolling to a random position.
                    mMsgListView.setSelectionFromTop(lastItemInList,
                            listHeight - lastVisibleItemHeight);
                } else {
                    mMsgListView.smoothScrollToPosition(lastItemInList);
                }
                mLastSmoothScrollPosition = lastItemInList;
            }
        }
    }

    private final class BackgroundQueryHandler extends ConversationQueryHandler {
        public BackgroundQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            /// M: @{
            MmsLog.d(TAG, "onQueryComplete, token=" + token + "activity=" + ComposeMessageActivity.this);
            /// @}
            switch(token) {
                case MESSAGE_LIST_QUERY_TOKEN:
                    /// M: add for ip message, unread divider
                    if (!mShowUnreadDivider && mIsMarkMsgAsRead) {
                        mConversation.blockMarkAsRead(false);
                        mConversation.markAsRead();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (MmsConfig.getIpMessagServiceId(ComposeMessageActivity.this)
                                        != IpMessageServiceId.NO_SERVICE && isCurrentRecipientIpMessageUser()) {
                                    IpMessageUtils.getMessageManager(ComposeMessageActivity.this)
                                            .setThreadAsViewed(mConversation.getThreadId());
                                }
                            }
                        }).start();
                    }
                    /// M: reset unread count to 0 if not need to show unread divider. @{
                    if (!mShowUnreadDivider) {
                        mMsgListAdapter.setUnreadCount(0);
                    }
                    /// @}
                    if (cursor == null) {
                        MmsLog.w(TAG, "onQueryComplete, cursor is null.");
                        return;
                    }
                    /// M: If adapter or listener has been cleared, just close this cursor@{
                    if (mMsgListAdapter == null) {
                        MmsLog.w(TAG, "onQueryComplete, mMsgListAdapter is null.");
                        cursor.close();
                        return;
                    }
                    if (mMsgListAdapter.getOnDataSetChangedListener() == null) {
                        MmsLog.d(TAG, "OnDataSetChangedListener is cleared");
                        cursor.close();
                        return;
                    }
                    /// @}
                    // check consistency between the query result and 'mConversation'
                    long tid = (Long) cookie;

                    if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                        log("##### onQueryComplete: msg history result for threadId " + tid);
                    }
                    if (tid != mConversation.getThreadId()) {
                        log("onQueryComplete: msg history query result is for threadId " +
                                tid + ", but mConversation has threadId " +
                                mConversation.getThreadId() + " starting a new query");
                        if (cursor != null) {
                            cursor.close();
                        }
                        startMsgListQuery();
                        return;
                    }

                    // check consistency b/t mConversation & mWorkingMessage.mConversation
                    ComposeMessageActivity.this.sanityCheckConversation();

                    int newSelectionPos = -1;
                    long targetMsgId = getIntent().getLongExtra("select_id", -1);
                    if (targetMsgId != -1) {
                        cursor.moveToPosition(-1);
                        while (cursor.moveToNext()) {
                            long msgId = cursor.getLong(COLUMN_ID);
                            if (msgId == targetMsgId) {
                                newSelectionPos = cursor.getPosition();
                                break;
                            }
                        }
                    } else if (mSavedScrollPosition != -1) {
                        // mSavedScrollPosition is set when this activity pauses. If equals maxint,
                        // it means the message list was scrolled to the end. Meanwhile, messages
                        // could have been received. When the activity resumes and we were
                        // previously scrolled to the end, jump the list so any new messages are
                        // visible.
                        if (mSavedScrollPosition == Integer.MAX_VALUE) {
                            int cnt = mMsgListAdapter.getCount();
                            if (cnt > 0) {
                                // Have to wait until the adapter is loaded before jumping to
                                // the end.
                                newSelectionPos = cnt - 1;
                                mSavedScrollPosition = -1;
                            }
                        } else {
                            // remember the saved scroll position before the activity is paused.
                            // reset it after the message list query is done
                            newSelectionPos = mSavedScrollPosition;
                            mSavedScrollPosition = -1;
                        }
                    }
                    /// M: Code analyze 047, Extra uri from message body and get number from uri.
                    /// Then use this number to update contact cache. @{
                    if (mNeedUpdateContactForMessageContent) {
                        updateContactCache(cursor);
                        mNeedUpdateContactForMessageContent = false;
                    }
                    /// @}

                    mMsgListAdapter.changeCursor(cursor);

                    if (newSelectionPos != -1) {
                        /// M: fix bug ALPS00404266, keep item top @{
                        //mMsgListView.setSelection(newSelectionPos);     // jump the list to the pos
                        View child = mMsgListView.getChildAt(newSelectionPos);
                        int top = 0;
                        if (child != null) {
                            top = child.getTop();
                        }
                        mMsgListView.setSelectionFromTop(newSelectionPos, top);
                        /// @}
                    } else {
                        /// M: google jb.mr1 patch, Conversation should scroll to the bottom
                        /// when incoming received @{
                        int count = mMsgListAdapter.getCount();
                        long lastMsgId = 0;
                        if (count > 0) {
                            cursor.moveToLast();
                            lastMsgId = cursor.getLong(COLUMN_ID);
                        }
                        // mScrollOnSend is set when we send a message. We always want to scroll
                        // the message list to the end when we send a message, but have to wait
                        // until the DB has changed. We also want to scroll the list when a
                        // new message has arrived.
                        smoothScrollToEnd(mScrollOnSend || lastMsgId != mLastMessageId, 0);
                        mLastMessageId = lastMsgId;
                        /// @}
                        mScrollOnSend = false;
                    }
                    // Adjust the conversation's message count to match reality. The
                    // conversation's message count is eventually used in
                    // WorkingMessage.clearConversation to determine whether to delete
                    // the conversation or not.
                    if (mMsgListAdapter.getCount() == 0 && mWaitingForSendMessage) {
                        mConversation.setMessageCount(1);
                    } else {
                        mConversation.setMessageCount(mMsgListAdapter.getCount());
                    }
                    cursor.moveToPosition(-1);
                    while (cursor.moveToNext()) {
                        int read = cursor.getInt(MessageListAdapter.COLUMN_MMS_READ);
                        read += cursor.getInt(MessageListAdapter.COLUMN_SMS_READ);
                        if (read == 0) {
                            mConversation.setHasUnreadMessages(true);
                            break;
                        }
                    }
                    MmsLog.d(TAG, "onQueryComplete(): Conversation.ThreadId=" + mConversation.getThreadId()
                            + ", MessageCount=" + mConversation.getMessageCount());

                    /// M: ipmessage: we are in important list, and remove the only important message flag.
                    if (mIsImportantThread && cursor.getCount() == 0) {
                        finish();
                        return;
                    }
                    // Once we have completed the query for the message history, if
                    // there is nothing in the cursor and we are not composing a new
                    // message, we must be editing a draft in a new conversation (unless
                    // mSentMessage is true).
                    // Show the recipients editor to give the user a chance to add
                    // more people before the conversation begins.
                    if (cursor.getCount() == 0 && !isRecipientsEditorVisible() && !mSentMessage) {
                        initRecipientsEditor(null);
                    }

                    // FIXME: freshing layout changes the focused view to an unexpected
                    // one, set it back to TextEditor forcely.
                    if(mSubjectTextEditor == null || (mSubjectTextEditor != null && !mSubjectTextEditor.isFocused()))
                    {
                        mTextEditor.requestFocus();
                    }

                    invalidateOptionsMenu();    // some menu items depend on the adapter's count

                    showInvitePanel();
                    return;
                case MESSAGE_LIST_UNREAD_QUERY_TOKEN:
                    MmsLog.d(TAG, "onQueryComplete(): unread cursor = " + cursor +
                            ", show divider ?= " + mShowUnreadDivider);
                    MmsLog.d(TAG_DIVIDER, "compose.onQueryComplete(): unread cursor = " + cursor +
                            ", show divider ?=" + mShowUnreadDivider);
                    if (cursor != null) {
                        MmsLog.d(TAG, "onQueryComplete(): unread cursor count = " + cursor.getCount());
                        MmsLog.d(TAG_DIVIDER, "compose.onQueryComplete(): unread cursor count = " + cursor.getCount());
                    }
                    if (mShowUnreadDivider) {
                        if (cursor == null) {
                            MmsLog.w(TAG, "onQueryComplete(): case MESSAGE_LIST_UNREAD_QUERY_TOKEN, cursor is null.");
                            return;
                        }
                        MmsLog.d(TAG_DIVIDER, "compose.onQueryComplete(): unread cursor count = " + cursor.getCount());
                        mMsgListAdapter.setUnreadCount(cursor.getCount());
                        cursor.close();
                    }
                    ///M add for avoid cusor leak. fix issueALPS00442806 @{
                    else {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                    /// @}
                    return;
                case ConversationList.HAVE_LOCKED_MESSAGES_TOKEN:
                    @SuppressWarnings("unchecked")
                    ArrayList<Long> threadIds = (ArrayList<Long>)cookie;
                    ConversationList.confirmDeleteThreadDialog(
                            new ConversationList.DeleteThreadListener(threadIds,
                                mBackgroundQueryHandler, ComposeMessageActivity.this),
                            threadIds,
                            cursor != null && cursor.getCount() > 0,
                            ComposeMessageActivity.this);
                    if (cursor != null) {
                        cursor.close();
                    }
                    break;

                case MESSAGE_LIST_QUERY_AFTER_DELETE_TOKEN:
                    // check consistency between the query result and 'mConversation'
                    tid = (Long) cookie;

                    if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                        log("##### onQueryComplete (after delete): msg history result for threadId "
                                + tid);
                    }
                    if (cursor == null) {
                        return;
                    }
                    if (tid > 0 && cursor.getCount() == 0) {
                        // We just deleted the last message and the thread will get deleted
                        // by a trigger in the database. Clear the threadId so next time we
                        // need the threadId a new thread will get created.
                        log("##### MESSAGE_LIST_QUERY_AFTER_DELETE_TOKEN clearing thread id: "
                                + tid);
                        Conversation conv = Conversation.get(getApplicationContext(), tid,
                                false);
                        if (conv != null) {
                            conv.clearThreadId();
                            conv.setDraftState(false);
                        }
                    }
                    cursor.close();
                    break;
                default:
                    MmsLog.d(TAG, "unknown token.");
                    break;
            }
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            super.onDeleteComplete(token, cookie, result);
            /// M: fix bug ALPS00351620; for requery searchactivity.
            SearchActivity.setNeedRequery();
            switch(token) {
                case ConversationList.DELETE_CONVERSATION_TOKEN:
                    /// M: @{
                    /*
                    mConversation.setMessageCount(0);
                    // fall through
                    */
                    try {
                        /** M: MTK Encapsulation ITelephony */
                        // ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                        EncapsulatedTelephonyService phone = EncapsulatedTelephonyService.getInstance();
                        if(phone != null) {
                            if(phone.isTestIccCard()) {
                                MmsLog.d(TAG, "All messages has been deleted, send notification...");
                                EncapsulatedSmsManager.setSmsMemoryStatus(true);
                            }
                        } else {
                            MmsLog.d(TAG, "Telephony service is not available!");
                        }
                    } catch(Exception ex) {
                        MmsLog.e(TAG, "" + ex.getMessage());
                    }
                    // Update the notification for new messages since they
                    // may be deleted.
                    MessagingNotification.nonBlockingUpdateNewMessageIndicator(
                            ComposeMessageActivity.this, MessagingNotification.THREAD_NONE, false);
                    // Update the notification for failed messages since they
                    // may be deleted.
                    updateSendFailedNotification();
                    MessagingNotification.updateDownloadFailedNotification(ComposeMessageActivity.this);
                    break;
                    /// @}
                case DELETE_MESSAGE_TOKEN:
                    /// M: google jb.mr1 patch, Conversation should scroll to the bottom
                    /// when incoming received @{
                    if (cookie instanceof Boolean && ((Boolean)cookie).booleanValue()) {
                        // If we just deleted the last message, reset the saved id.
                        mLastMessageId = 0;
                    }
                    /// @}
                    /// M: Code analyze 027,Add for deleting one message.@{
                    MmsLog.d(TAG, "onDeleteComplete(): before update mConversation, ThreadId = " + mConversation.getThreadId());
                    ContactList recipients = getRecipients();
                    mConversation = Conversation.upDateThread(ComposeMessageActivity.this, mConversation.getThreadId(), false);
                    mThreadCountManager.isFull(mThreadId, ComposeMessageActivity.this,
                            ThreadCountManager.OP_FLAG_DECREASE);
                    /// @}
                    // Update the notification for new messages since they
                    // may be deleted.
                    MessagingNotification.nonBlockingUpdateNewMessageIndicator(
                            ComposeMessageActivity.this, MessagingNotification.THREAD_NONE, false);
                    // Update the notification for failed messages since they
                    // may be deleted.
                    updateSendFailedNotification();
                    /// M: Code analyze 027,Add for deleting one message.@{
                    MessagingNotification.updateDownloadFailedNotification(ComposeMessageActivity.this);
                    MmsLog.d(TAG, "onDeleteComplete(): MessageCount = " + mConversation.getMessageCount() + 
                            ", ThreadId = " + mConversation.getThreadId());
                    if (mConversation.getMessageCount() <= 0 || mConversation.getThreadId() <= 0l) {
                        mMsgListAdapter.changeCursor(null);
                        if (needSaveDraft() && (recipients != null)) {
                            makeDraftEditable(recipients);
                        } else {
                            finish();
                        }
                    }
                    /// @}
                    break;
            }
            // If we're deleting the whole conversation, throw away
            // our current working message and bail.
            if (token == ConversationList.DELETE_CONVERSATION_TOKEN) {
                ContactList recipients = mConversation.getRecipients();
                mWorkingMessage.discard();

                // Remove any recipients referenced by this single thread from the
                // contacts cache. It's possible for two or more threads to reference
                // the same contact. That's ok if we remove it. We'll recreate that contact
                // when we init all Conversations below.
                if (recipients != null) {
                    for (Contact contact : recipients) {
                        contact.removeFromCache();
                    }
                }

                // Make sure the conversation cache reflects the threads in the DB.
                Conversation.init(getApplicationContext());
                finish();
            } else if (token == DELETE_MESSAGE_TOKEN) {
                /// M: Code analyze 027,Add for deleting one message.@{
                // Check to see if we just deleted the last message
                MmsLog.d(TAG, "register mDraftChangeObserver again after delete compeletely");
                mContentResolver.registerContentObserver(
                        Mms.CONTENT_URI, true, mDraftChangeObserver);
                startMsgListQuery(MESSAGE_LIST_QUERY_AFTER_DELETE_TOKEN, 0);
                /// @}
            }
        }
    }

    private void showSmileyDialog() {
        if (mSmileyDialog == null) {
            int[] icons = SmileyParser.DEFAULT_SMILEY_RES_IDS;
            String[] names = getResources().getStringArray(
                    SmileyParser.DEFAULT_SMILEY_NAMES);
            final String[] texts = getResources().getStringArray(
                    SmileyParser.DEFAULT_SMILEY_TEXTS);

            final int N = names.length;

            List<Map<String, ?>> entries = new ArrayList<Map<String, ?>>();
            for (int i = 0; i < N; i++) {
                // We might have different ASCII for the same icon, skip it if
                // the icon is already added.
                boolean added = false;
                for (int j = 0; j < i; j++) {
                    if (icons[i] == icons[j]) {
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    HashMap<String, Object> entry = new HashMap<String, Object>();

                    entry. put("icon", icons[i]);
                    entry. put("name", names[i]);
                    entry.put("text", texts[i]);

                    entries.add(entry);
                }
            }

            final SimpleAdapter a = new SimpleAdapter(
                    this,
                    entries,
                    R.layout.smiley_menu_item,
                    new String[] {"icon", "name", "text"},
                    new int[] {R.id.smiley_icon, R.id.smiley_name, R.id.smiley_text});
            SimpleAdapter.ViewBinder viewBinder = new SimpleAdapter.ViewBinder() {
                @Override
                public boolean setViewValue(View view, Object data, String textRepresentation) {
                    if (view instanceof ImageView) {
                        Drawable img = getResources().getDrawable((Integer)data);
                        ((ImageView)view).setImageDrawable(img);
                        return true;
                    }
                    return false;
                }
            };
            a.setViewBinder(viewBinder);

            AlertDialog.Builder b = new AlertDialog.Builder(this);

            b.setTitle(getString(R.string.menu_insert_smiley));

            b.setCancelable(true);
            b.setAdapter(a, new DialogInterface.OnClickListener() {
                @Override
                @SuppressWarnings("unchecked")
                public final void onClick(DialogInterface dialog, int which) {
                    HashMap<String, Object> item = (HashMap<String, Object>) a.getItem(which);

                    String smiley = (String)item.get("text");
                    if (mSubjectTextEditor != null && mSubjectTextEditor.hasFocus()) {
                        insertText(mSubjectTextEditor, smiley);
                    } else {
                        insertText(mTextEditor, smiley);
                    }

                    dialog.dismiss();
                }
            });

            mSmileyDialog = b.create();
        }

        mSmileyDialog.show();
    }

    @Override
    public void onUpdate(final Contact updated) {
        /** M:
         * In a bad case ANR will happen. When many contact is update, onUpdate will be 
         * invoked very frequently,and the code here will run many times. In mRecipientsEditor,
         * if there are 100[can be more?] recipients, 
         * mRecipientsEditor.constructContactsFromInput is time cost.
         * ANR may happen if process many this message consequently.
         * so reduce the frequence, and touch event message have more changces to process.
         */
        if (mPrevRunnable != null) {
            mMessageListItemHandler.removeCallbacks(mPrevRunnable);
        }
        mPrevRunnable = new Runnable() {
            public void run() {
                /// M: fix bug ALPS00432236, updateTitle only 10 contact in PORTRAIT @{
                boolean isPortrait = getResources().getConfiguration().orientation
                                                     == Configuration.ORIENTATION_PORTRAIT;
                int updateLimit = 0;
                if (isPortrait) {
                    updateLimit = UPDATE_LIMIT_PORTRAIT;
                } else {
                    updateLimit = UPDATE_LIMIT_LANDSCAPE;
                }
                /// @}
                ContactList recipients = isRecipientsEditorVisible() ?
                        mRecipientsEditor.constructContactsFromInputWithLimit(false, updateLimit) : getRecipients();
                if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                    log("[CMA] onUpdate contact updated: " + updated);
                    log("[CMA] onUpdate recipients: " + recipients);
                }
                updateTitle(recipients);

                // The contact information for one (or more) of the recipients has changed.
                // Rebuild the message list so each MessageItem will get the last contact info.
                ComposeMessageActivity.this.mMsgListAdapter.notifyDataSetChanged();

                // Don't do this anymore. When we're showing chips, we don't want to switch from
                // chips to text.
//                if (mRecipientsEditor != null) {
//                    mRecipientsEditor.populate(recipients);
//                }

                /// M: fix bug ALPS00448677, update or delete Contact Chip
                if (isRecipientsEditorVisible() && mInViewContact != null) {
                    if (!mInViewContact.existsInDatabase()) {
                        mRecipientsEditor.updatePressedChip(RecipientsEditor.UpdatePressedChipType.DELETE_CONTACT);
                    } else {
                        mRecipientsEditor.updatePressedChip(RecipientsEditor.UpdatePressedChipType.UPDATE_CONTACT);
                    }
                    mInViewContact = null;
                }
                /// @}
            }
        };
        /// M: Using an existing handler for the post, rather than conjuring up a new one.
        mMessageListItemHandler.postDelayed(mPrevRunnable, UPDATE_DELAY);
    }

    private void addRecipientsListeners() {
        Contact.addListener(this);
    }

    private void removeRecipientsListeners() {
        Contact.removeListener(this);
    }

    public static Intent createIntent(Context context, long threadId) {
        Intent intent = new Intent(context, ComposeMessageActivity.class);

        if (threadId > 0) {
            intent.setData(Conversation.getUri(threadId));
        }

        return intent;
    }

    private String getBody(Uri uri) {
        if (uri == null) {
            return null;
        }
        String urlStr = uri.getSchemeSpecificPart();
        if (!urlStr.contains("?")) {
            return null;
        }
        urlStr = urlStr.substring(urlStr.indexOf('?') + 1);
        String[] params = urlStr.split("&");
        for (String p : params) {
            if (p.startsWith("body=")) {
                try {
                    return URLDecoder.decode(p.substring(5), "UTF-8");
                } catch (UnsupportedEncodingException e) { }
            }
        }
        return null;
    }

    private void updateThreadIdIfRunning() {
        if (mIsRunning && mConversation != null) {
            MessagingNotification.setCurrentlyDisplayedThreadId(mConversation.getThreadId());
        }
        // If we're not running, but resume later, the current thread ID will be set in onResume()
    }

     //////////////////////////////////////////////////////////////////////////////////////
     // MTK add

     /// M: Code analyze 012, add for multi-delete @{
     public static final int REQUEST_CODE_FOR_MULTIDELETE  = 110;
     /// @}
     public static final int REQUEST_CODE_FOR_FORWARD = 119;
     /// M: Code analyze 025, Add video or audio attachment and check the attachment size.@{
     public static final int MIN_SIZE_FOR_CAPTURE_VIDEO    = 1024 * 10;  // 10K
     public static final int MIN_SIZE_FOR_RECORD_AUDIO = 1024 * 5; // 5K
     // M: fix bug ALPS00354728
     private boolean mAppendAttachmentSign = true;
     /// @}

     /// M: Code analyze 014, Add quick text. @{
     private static final int MENU_ADD_QUICK_TEXT         = 8;
     private AlertDialog mQuickTextDialog;
     /// @}

     /// M: Code analyze 015, Add text vcard. @{
     private static final int MENU_ADD_TEXT_VCARD         = 9;
     public static final int REQUEST_CODE_TEXT_VCARD       = 22;
     /// @}

     private static final int MENU_CALL_RECIPIENT_BY_VT  = 10;
     /// M: Code analyze 016, Add for select text copy. @{
     private static final int MENU_SELECT_TEXT             = 36;
     /// @}

     private static final String SIGN_CREATE_AFTER_KILL_BY_SYSTEM = "ForCreateAfterKilledBySystem";

     /// M: Code analyze 017, Handle forwarded message.(see:forwardMessage())@{
     public static final String SMS_ADDRESS = "sms_address";
     public static final String SMS_BODY = "sms_body";
     public static final String FORWARD_MESSAGE = "forwarded_message";
     /// @}

     // State variable indicating an image is being compressed, which may take a while.
     private boolean mCompressingImage = false;
     private int mToastCountForResizeImage = 0; // For indicate whether show toast message for
                        //resize image or not. If mToastCountForResizeImage equals 0, show toast.
     /// M: Code analyze 010, Support dirtory mode. @{
     private int mHomeBox = 0;
     /// @}
     private Toast mExceedMessageSizeToast = null;

     /// M: Code analyze 009,Show attachment dialog . @{
     private SoloAlertDialog mSoloAlertDialog;
     /// @}

     /// M: Code analyze 047, Extra uri from message body and get number from uri.
     /// Then use this number to update contact cache. @{
     private boolean mNeedUpdateContactForMessageContent = true;
     /// @}

     private boolean  mDrawBottomPanel = false;

     /// M: the member is only used by onUpdate
     private static final long UPDATE_DELAY = 100L;

     /// M: Code analyze 011, use another method for performance
     ///(use this to limit the contact query count) @{
     private static final int UPDATE_LIMIT_LANDSCAPE = 20;
     private static final int UPDATE_LIMIT_PORTRAIT = 10;
     /// @}

     private Runnable mPrevRunnable;
     private boolean mNeedSaveAsMms = false;

     /// M: Code analyze 038, If the user is editing slideshow now.
     /// Do not allow the activity finish but return directly when back key is entered. @{
     private boolean mIsEditingSlideshow = false;
     /// @}

     /// M: Code analyze 026, If the two clicks are too close. @{
     private long mLastButtonClickTime = 0;
     private long clickTime = 0;
     /// @}

     // mAudioUri will look like this: content://media/external/images/media
     private static final String mAudioUri = Audio.Media.getContentUri("external").toString();

     /// M: Code analyze 050, Add scroll listener and touch listener for MessageListView.@{
     private static int CHANGE_SCROLL_LISTENER_MIN_CURSOR_COUNT = 100;
     private MyScrollListener mScrollListener = new MyScrollListener(
             CHANGE_SCROLL_LISTENER_MIN_CURSOR_COUNT, "MessageList_Scroll_Tread");
     /// @}

     /// M: Code analyze 018, Add ringtone for sound attachment.  @{
     public static final int REQUEST_CODE_ATTACH_RINGTONE  = 20;
     /// @}

     /// M: Code analyze 019, Add vcard attachment.  @{
     public static final int REQUEST_CODE_ATTACH_VCARD       = 21;
     /// @}

     /// M: Code analyze 020, Add vcalendar attachment.  @{
     public static final int REQUEST_CODE_ATTACH_VCALENDAR = 25;
     /// @}

     /// M: Code analyze 021, Copy all valid parts of the attachment(pdu) to SD card.
     /// This opeartor will be removed to a separate activity.    @{
     public static final int REQUEST_CODE_MULTI_SAVE       = 23;
     /// @}

     /// M: Code analyze 022, Add bookmark. @{
     private static final int MENU_ADD_TO_BOOKMARK         = 35;
     private ArrayList<String> mURLs = new ArrayList<String>();
     /// @}

     /// M: Code analyze 007, Get information from Sim or save message to Sim. @{
     private static final int MENU_SAVE_MESSAGE_TO_SIM     = 32;
     private static final int SIM_SELECT_FOR_SEND_MSG                     = 1;
     private static final int SIM_SELECT_FOR_SAVE_MSG_TO_SIM             = 2;
     private static final int MSG_QUIT_SAVE_MESSAGE_THREAD                 = 100;
     private static final int MSG_SAVE_MESSAGE_TO_SIM                     = 102;
     private static final int MSG_SAVE_MESSAGE_TO_SIM_AFTER_SELECT_SIM     = 104;
     private static final int MSG_SAVE_MESSAGE_TO_SIM_SUCCEED             = 106;
     private static final int MSG_SAVE_MESSAGE_TO_SIM_FAILED_GENERIC     = 108;
     private static final int MSG_SAVE_MESSAGE_TO_SIM_FAILED_SIM_FULL     = 110;

     private static final String SELECT_TYPE    = "Select_type";
     private int mSimCount;//The count of current sim cards.  0/1/2
     private List<SIMInfo> mSimInfoList;
     private Handler mSaveMsgHandler = null;
     private Thread mSaveMsgThread = null;
     private AlertDialog mSIMSelectDialog;
     private long mMessageSimId;
     private int mAssociatedSimId;
     private int mSelectedSimId;
     /// @}

     /// M: Code analyze 048, Add this can send msg from a marked sim card
     /// which is delivered in Intent.@{
     private int send_sim_id = -1;
     /// @}

     /// M: Code analyze 006, Control SIM indicator on status bar. @{
     private EncapsulatedStatusBarManager mStatusBarManager;
     private ComponentName mComponentName;
     private boolean mIsShowSIMIndicator = true;
     /// @}

     /// M: Code analyze 056,Now,the sms recipient limit is different from mms.
     /// We can set limit for sms or mms individually. @{
     private static final int RECIPIENTS_LIMIT_FOR_SMS     = MmsConfig.getSmsRecipientLimit();
     /// @}
     private boolean mIsTooManyRecipients;     // Whether the recipients are too many

     /// M: Code analyze 046, Whether the recipientedit control has been initialized. @{
     private boolean isInitRecipientsEditor = true;// true, init mRecipientsEditor and add recipients;
                                                   // false, init mRecipientsEditor, but recipients
     /// @}

     private boolean mWaitingForSendMessage;

     /// M: Code analyze 023, Delete the char value of '\r' . @{
     private static final String STR_RN = "\\r\\n"; // for "\r\n"
     private static final String STR_CN = "\n"; // the char value of '\n'
     /// @}
     public static boolean mDestroy = false;

     /// M: Code analyze 027,Add for deleting one message.@{
     private ThreadCountManager mThreadCountManager = ThreadCountManager.getInstance();
     private Long mThreadId = -1l;
     /// @}

     /// M: Code analyze 008,unkown . @{
     private static EncapsulatedCellConnMgr mCellMgr = null;
     private static int mCellMgrRegisterCount = 0;
     /// @}

     /// M: Code analyze 002,  If a new ComposeMessageActivity is created, kill old one
     private static WeakReference<ComposeMessageActivity> sCompose = null;
     /// @}

     private boolean mSendButtonCanResponse = true;    // can click send button
     private static final long RESUME_BUTTON_INTERVAL = 1000;
     private static final int MSG_RESUME_SEND_BUTTON  = 112;

     /// M: Code analyze 024, If the click operator can be responsed. @{
     boolean mClickCanResponse = true;           // can click button or some view items
     /// @}

     /// M: Code analyze 013, Get contacts from Contact app . @{
     // handle NullPointerException in onActivityResult() for pick up recipients
     private boolean mIsRecipientHasIntentNotHandle = false;
     private Intent mIntent = null;
     private boolean misPickContatct = false;
     /// @}

     /// M: Code analyze 004, Set max height for text editor. @{
     private HeightChangedLinearLayout mHeightChangedLinearLayout;
     private static final int mReferencedTextEditorTwoLinesHeight = 72;
     private static final int mReferencedTextEditorThreeLinesHeight = 110;
     private static final int mReferencedTextEditorFourLinesHeight    = 140;
     private static final int mReferencedTextEditorSevenLinesHeight = 224;
     private static final int mReferencedAttachmentEditorHeight     = 266;
     private static final int mReferencedMaxHeight                    = 800;
     private int mCurrentMaxHeight                                    = 800;
     /// @}

     /// M: Code analyze 001, Plugin opeartor. @{
     private IMmsCompose mMmsComposePlugin = null;
     /// @}

     /// M: Code analyze 036, Change text size if adjust font size.@{
     private IMmsTextSizeAdjust mMmsTextSizeAdjustPlugin = null;
     /// @}

     /// M: Code analyze 042, If you discard the draft message manually.@{
     private boolean mHasDiscardWorkingMessage = false;
     /// @}

     boolean isTwoClickClose(long lastClickTime , long interval){
        long oldTime = lastClickTime;
        lastClickTime = SystemClock.elapsedRealtime();
        if ((lastClickTime-oldTime < interval)&&(lastClickTime-oldTime > 0)) {
            return true;
        }
        return false;
    }

    /// M: fix bug ALPS00572383, delay show "suggested" on the SelectSimDialog @{
    private final HashMap<Integer, Integer> mHashSim = new HashMap<Integer, Integer>();
    private static final int MSG_SELECT_SIM_DIALOG_SHOW = 500;
    private int mAssociatedSimQueryDone;
    private int mSelectSimType;
    /// @}

    /// M:
    private Handler mUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_SAVE_MESSAGE_TO_SIM_SUCCEED:
                Toast.makeText(ComposeMessageActivity.this, R.string.save_message_to_sim_successful, Toast.LENGTH_SHORT).show();
                break;

            case MSG_SAVE_MESSAGE_TO_SIM_FAILED_GENERIC:
                Toast.makeText(ComposeMessageActivity.this, R.string.save_message_to_sim_unsuccessful, Toast.LENGTH_SHORT).show();
                break;

            case MSG_SAVE_MESSAGE_TO_SIM_FAILED_SIM_FULL:
                Toast.makeText(ComposeMessageActivity.this,
                        getString(R.string.save_message_to_sim_unsuccessful) + ". " + getString(R.string.sim_full_title), 
                        Toast.LENGTH_SHORT).show();
                break;
            /// M: Code analyze 007, Get information from Sim or save message to Sim. @{
            case MSG_SAVE_MESSAGE_TO_SIM: //multi sim cards
                String type = (String)msg.obj;
                long msgId = msg.arg1;
                saveMessageToSim(type, msgId);
                break;
            /// @}
            case MSG_RESUME_SEND_BUTTON:
                mSendButtonCanResponse = true;
                break;
            case MessageUtils.UPDATE_SENDBUTTON:
                updateSendButtonState();
                break;
            case MSG_SELECT_SIM_DIALOG_SHOW:
                /// M: fix bug ALPS00572383, delay show "suggested" on the SelectSimDialog @{
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        int position = -1;
                        if (mHashSim != null && mSelectSimType == SIM_SELECT_FOR_SEND_MSG) {
                            for (int i = 0; i < mHashSim.size(); i++) {
                                if (mHashSim != null && mHashSim.get(i) == mAssociatedSimQueryDone) {
                                    position = i;
                                    break;
                                }
                            }

                            if (position != -1 && mSIMSelectDialog != null
                                                        && mSIMSelectDialog.isShowing()) {
                                ListView listView = mSIMSelectDialog.getListView();
                                if (listView != null) {
                                    View view = listView.getChildAt(position);
                                    if (view != null) {
                                       TextView textView =
                                           (TextView)view.findViewById(R.id.sim_suggested);
                                       if (textView != null) {
                                           textView.setText(getString(R.string.suggested));
                                       }
                                    }
                                }
                                mHashSim.clear();
                            }
                        }
                    }
                });
                break;
                /// @}
            default:
                MmsLog.d(TAG, "inUIHandler msg unhandled.");
                break;
            }
        }
    };

    /// M: Code analyze 007, Get information from Sim or save message to Sim. @{
    private final class SaveMsgThread extends Thread {
        private String msgType = null;
        private long msgId = 0;
        public SaveMsgThread(String type, long id) {
            msgType = type;
            msgId = id;
        }
        public void run() {
            Looper.prepare();
            if (null != Looper.myLooper()) {
                mSaveMsgHandler = new SaveMsgHandler(Looper.myLooper());
            }
            Message msg = mSaveMsgHandler.obtainMessage(MSG_SAVE_MESSAGE_TO_SIM);
            msg.arg1 = (int)msgId;
            msg.obj = msgType;
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT && mSimCount > 1) {//multi sim cards
                mUiHandler.sendMessage(msg);
            } else {
                mSaveMsgHandler.sendMessage(msg);//single sim card
            }
            Looper.loop();
        }
    }

    private final class SaveMsgHandler extends Handler {
        public SaveMsgHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_QUIT_SAVE_MESSAGE_THREAD: {
                    MmsLog.v(MmsApp.TXN_TAG, "exit save message thread");
                    getLooper().quit();
                    break;
                }

                case MSG_SAVE_MESSAGE_TO_SIM: {//single sim card
                    String type = (String)msg.obj;
                    long msgId = msg.arg1;
                    //saveMessageToSim(type, msgId);
                    getMessageAndSaveToSim(type, msgId);
                    break;
                }

                case MSG_SAVE_MESSAGE_TO_SIM_AFTER_SELECT_SIM: {
                    Intent it = (Intent)msg.obj;
                    getMessageAndSaveToSim(it);
                    break;
                }

                default:
                    break;
            }
        }
    }

    private void saveMessageToSim(String msgType, long msgId) {//multi sim cards exist
        MmsLog.d(MmsApp.TXN_TAG, "save message to sim, message type:" + msgType 
                + "; message id:" + msgId + "; sim count:" + mSimCount);

        Intent intent = new Intent();
        intent.putExtra("message_type", msgType);
        intent.putExtra("message_id", msgId);
        intent.putExtra(SELECT_TYPE, SIM_SELECT_FOR_SAVE_MSG_TO_SIM);
        mSelectSimType = SIM_SELECT_FOR_SAVE_MSG_TO_SIM;
        showSimSelectedDialog(intent);
    }

    private void getMessageAndSaveToSim(Intent intent) {
        MmsLog.v(MmsApp.TXN_TAG, "get message and save to sim, selected sim id = " + mSelectedSimId);
        String msgType = intent.getStringExtra("message_type");
        long msgId = intent.getLongExtra("message_id", 0);
        if (msgType == null) {
            //mSaveMsgHandler.sendEmptyMessage(MSG_SAVE_MESSAGE_TO_SIM_FAILED_GENERIC);
            mUiHandler.sendEmptyMessage(MSG_SAVE_MESSAGE_TO_SIM_FAILED_GENERIC);            
            return;
        }
        getMessageAndSaveToSim(msgType, msgId);
    }

    private void getMessageAndSaveToSim(String msgType, long msgId){
        int result = 0;
        MessageItem msgItem = getMessageItem(msgType, msgId, true);
        if (msgItem == null || msgItem.mBody == null) {
            MmsLog.e(MmsApp.TXN_TAG, "getMessageAndSaveToSim, can not get Message Item.");
            return;
        }
        
        String scAddress = null;

        ArrayList<String> messages = null;
        messages = EncapsulatedSmsManager.divideMessage(msgItem.mBody);

        int smsStatus = 0;
        long timeStamp = 0;
        if (msgItem.isReceivedMessage()) {
            smsStatus = SmsManager.STATUS_ON_ICC_READ;
            timeStamp = msgItem.mSmsDate;
            scAddress = msgItem.getServiceCenter();
        } else if (msgItem.isSentMessage()) {
            smsStatus = SmsManager.STATUS_ON_ICC_SENT;
        } else if (msgItem.isFailedMessage()) {
            smsStatus = SmsManager.STATUS_ON_ICC_UNSENT;
        } else {
            MmsLog.w(MmsApp.TXN_TAG, "Unknown sms status");
        }

        EncapsulatedTelephonyService teleService = EncapsulatedTelephonyService.getInstance();
        if (scAddress == null) {
            try {
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    scAddress = teleService.getScAddressGemini(SIMInfo.getSlotById(this, mSelectedSimId));
                } else {
                    scAddress = teleService.getScAddressGemini(0);
                }
            } catch (RemoteException e) {
                MmsLog.e(MmsApp.TXN_TAG, "getScAddressGemini is failed.\n" + e.toString());
            }
        }

        MmsLog.d(MmsApp.TXN_TAG, "\t scAddress\t= " + scAddress);
        MmsLog.d(MmsApp.TXN_TAG, "\t Address\t= " + msgItem.mAddress);
        MmsLog.d(MmsApp.TXN_TAG, "\t msgBody\t= " + msgItem.mBody);
        MmsLog.d(MmsApp.TXN_TAG, "\t smsStatus\t= " + smsStatus);
        MmsLog.d(MmsApp.TXN_TAG, "\t timeStamp\t= " + timeStamp);


        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            int slotId = -1;
            if (mSimCount == 1) {
                slotId = mSimInfoList.get(0).getSlot();
            } else {
                slotId = SIMInfo.getSlotById(this, mSelectedSimId);
            }
            MmsLog.d(MmsApp.TXN_TAG, "\t slot Id\t= " + slotId);

            result = EncapsulatedGeminiSmsManager.copyTextMessageToIccCardGemini(scAddress,
                    msgItem.mAddress, messages, smsStatus, timeStamp, slotId);
        } else {
            
            result = EncapsulatedSmsManager.copyTextMessageToIccCard(scAddress,
                    msgItem.mAddress, messages, smsStatus, timeStamp);
        }

        if (result == EncapsulatedSmsManager.RESULT_ERROR_SUCCESS) {
            MmsLog.d(MmsApp.TXN_TAG, "save message to sim succeed.");
            mUiHandler.sendEmptyMessage(MSG_SAVE_MESSAGE_TO_SIM_SUCCEED);            
        } else if (result == EncapsulatedSmsManager.RESULT_ERROR_SIM_MEM_FULL) {
            MmsLog.w(MmsApp.TXN_TAG, "save message to sim failed: sim memory full.");
            mUiHandler.sendEmptyMessage(MSG_SAVE_MESSAGE_TO_SIM_FAILED_SIM_FULL);
        } else {
            MmsLog.w(MmsApp.TXN_TAG, "save message to sim failed: generic error.");
            mUiHandler.sendEmptyMessage(MSG_SAVE_MESSAGE_TO_SIM_FAILED_GENERIC);
        }
        mSaveMsgHandler.sendEmptyMessageDelayed(MSG_QUIT_SAVE_MESSAGE_THREAD, 5000);
    }

    Runnable mGetSimInfoRunnable = new Runnable() {
        public void run() {
            getSimInfoList();
            mUiHandler.sendEmptyMessage(MessageUtils.UPDATE_SENDBUTTON);
        }
    };

     private void getSimInfoList() {
        mSimInfoList = SIMInfo.getInsertedSIMList(this);
        mSimCount = mSimInfoList.isEmpty()? 0: mSimInfoList.size();
        MmsLog.v(TAG, "ComposeMessageActivity.getSimInfoList(): mSimCount = " + mSimCount);
    }

     private void simSelection() {
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT == false) {
            confirmSendMessageIfNeeded();
        } else if (mSimCount == 0) {
            // SendButton can't click in this case
        /// M: @{
        } else if (send_sim_id >= 1) {
            mSelectedSimId = send_sim_id;
            send_sim_id = -1;
            MmsLog.d(TAG, "send msg from send_sim_id = " + mSelectedSimId);
            confirmSendMessageIfNeeded();
        /// @}
        } else if (mSimCount == 1) {
            mSelectedSimId = (int) mSimInfoList.get(0).getSimId();
            confirmSendMessageIfNeeded();
        } else if (mSimCount > 1) {// multi sim cards exist.
            Intent intent = new Intent();
            mSelectSimType = SIM_SELECT_FOR_SEND_MSG;
            intent.putExtra(SELECT_TYPE, SIM_SELECT_FOR_SEND_MSG);
            // getContactSIM
            if (isRecipientsEditorVisible()) {
                if (mRecipientsEditor.getRecipientCount() == 1/*isOnlyOneRecipient()*/) {
                    mAssociatedSimId = getContactSIM(mRecipientsEditor.getNumbers().get(0)); // 152188888888 is a contact number
                } else {
                    mAssociatedSimId = -1;
                }
            } else {
                if (getRecipients().size() == 1/*isOnlyOneRecipient()*/) {
                    mAssociatedSimId = getContactSIM(getRecipients().get(0).getNumber()); // 152188888888 is a contact number
                } else {
                    mAssociatedSimId = -1;
                }
            }
            MmsLog.d(TAG, "mAssociatedSimId = " + mAssociatedSimId);
            // getDefaultSIM()
            mMessageSimId = EncapsulatedSettings.System.getLong(getContentResolver(), EncapsulatedSettings.System.SMS_SIM_SETTING, EncapsulatedSettings.System.DEFAULT_SIM_NOT_SET);
            if (mMessageSimId == EncapsulatedSettings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK ||
                    (mMessageSimId == EncapsulatedSettings.System.SMS_SIM_SETTING_AUTO && MmsConfig.getFolderModeEnabled())) {
                // always ask, show SIM selection dialog
                showSimSelectedDialog(intent);
                updateSendButtonState();
            } else if (mMessageSimId == EncapsulatedSettings.System.DEFAULT_SIM_NOT_SET) {
                /*
                 * not set default SIM:
                 * if recipients are morn than 2,or there is no associated SIM,
                 * show SIM selection dialog
                 * else send message via associated SIM
                 */
                if (mAssociatedSimId == -1) {
                    showSimSelectedDialog(intent);
                    updateSendButtonState();
                } else {
                    mSelectedSimId = mAssociatedSimId;
                    confirmSendMessageIfNeeded();
                }
            } else {
                /*
                 * default SIM:
                 * if recipients are morn than 2,or there is no associated SIM,
                 * send message via default SIM
                 * else show SIM selection dialog
                 */
                if (mAssociatedSimId == -1 || (mMessageSimId == mAssociatedSimId)) {
                    mSelectedSimId = (int) mMessageSimId;
                    confirmSendMessageIfNeeded();
                } else {
                    showSimSelectedDialog(intent);
                    updateSendButtonState();
                }
            }
        }
    }

    private void showSimSelectedDialog(Intent intent) {
        // TODO get default SIM and get contact SIM
        final Intent it = intent;
        List<Map<String, ?>> entries = new ArrayList<Map<String, ?>>();
        final List<SIMInfo> simListInfo = new ArrayList<SIMInfo>();

        mHashSim.clear();

        for (int slotId = 0; slotId < EncapsulatedPhone.GEMINI_SIM_NUM; slotId++) {
            SIMInfo simInfo = SIMInfo.getSIMInfoBySlot(ComposeMessageActivity.this, slotId);
            if (simInfo != null) {
                simListInfo.add(simInfo);
                HashMap<String, Object> entry = new HashMap<String, Object>();

                entry.put("simIcon", simInfo.getSimBackgroundLightRes());
                int state = EncapsulatedTelephonyManagerEx.getDefault().getSimIndicatorStateGemini(slotId);
                entry.put("simStatus", MessageUtils.getSimStatusResource(state));
                if (mIsIpServiceEnabled && MmsConfig.isServiceEnabled(this, (int) simInfo.getSimId())) {
                    MmsLog.d(IPMSG_TAG, "show ipmessage icon, simId = " + simInfo.getSimId());
                    entry.put("ipmsg_indicator", IpMessageConsts.drawable.ipmsg_sim_indicator);
                } else {
                    MmsLog.d(IPMSG_TAG, "hide ipmessage icon, simId = " + simInfo.getSimId());
                    entry.put("ipmsg_indicator", 0);
                }
                String simNumber = "";
                if (!TextUtils.isEmpty(simInfo.getNumber())) {
                    switch(simInfo.getDispalyNumberFormat()) {
                        case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_LAST:
                            if(simInfo.getNumber().length() <= 4)
                                simNumber = simInfo.getNumber();
                            else
                                simNumber = simInfo.getNumber().substring(simInfo.getNumber().length() - 4);
                            break;
                        //case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_DEFAULT:
                        case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_FIRST:
                            if(simInfo.getNumber().length() <= 4)
                                simNumber = simInfo.getNumber();
                            else
                                simNumber = simInfo.getNumber().substring(0, 4);
                            break;
                        case 0://android.provider.Telephony.SimInfo.DISPLAY_NUMBER_NONE:
                            simNumber = "";
                            break;
                        default:
                            break;
                    }
                }
                if (!TextUtils.isEmpty(simNumber)) {
                    entry.put("simNumberShort",simNumber);
                } else {
                    entry.put("simNumberShort", "");
                }

                entry.put("simName", simInfo.getDisplayName());
                if (!TextUtils.isEmpty(simInfo.getNumber())) {
                    entry.put("simNumber", simInfo.getNumber());
                } else {
                    entry.put("simNumber", "");
                }
                if (mAssociatedSimId == (int) simInfo.getSimId()
                    && it.getIntExtra(SELECT_TYPE, -1) != SIM_SELECT_FOR_SAVE_MSG_TO_SIM) {
                    // if this SIM is contact SIM, set "Suggested"
                    entry.put("suggested", getString(R.string.suggested));
                } else {
                    entry.put("suggested", "");// not suggested
                }
                entries.add(entry);
                /// M: fix bug ALPS00572383, delay show "suggested" on the SelectSimDialog @{
                mHashSim.put(entries.size() - 1, (int)simInfo.getSimId());
            }
        }

        final SimpleAdapter a = MessageUtils.createSimpleAdapter(entries, this);
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(getString(R.string.sim_selected_dialog_title));
        b.setCancelable(true);
        b.setAdapter(a, new DialogInterface.OnClickListener() {
            @SuppressWarnings("unchecked")
            public final void onClick(DialogInterface dialog, int which) {
                updateSendButtonState(false);
                /// M: fix bug ALPS00428800, fix JE when async update mSinInfoList @{
                if (mSimInfoList == null || which >= mSimInfoList.size()) {
                    MmsLog.d(TAG, "which >= mSimInfoList.size(); Async update mSinInfoList.");
                    dialog.dismiss();
                    return;
                }
                /// @}
                mSelectedSimId = (int) simListInfo.get(which).getSimId();
                if (it.getIntExtra(SELECT_TYPE, -1) == SIM_SELECT_FOR_SEND_MSG) {
                    confirmSendMessageIfNeeded();
                } else if (it.getIntExtra(SELECT_TYPE, -1) == SIM_SELECT_FOR_SAVE_MSG_TO_SIM) {
                    //getMessageAndSaveToSim(it);
                    Message msg = mSaveMsgHandler.obtainMessage(MSG_SAVE_MESSAGE_TO_SIM_AFTER_SELECT_SIM);
                    msg.obj = it;
                    //mSaveMsgHandler.sendMessageDelayed(msg, 60);
                    mSaveMsgHandler.sendMessage(msg);
                }
                dialog.dismiss();
            }
        });
        mSIMSelectDialog = b.create();
        /// M: fix bug ALPS00497704, remove extra Recipients Chip when back SIMSelectDialog @{
        mSIMSelectDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!isRecipientsEditorVisible() && mCutRecipients != null) {
                    mWorkingMessage.syncWorkingRecipients();
                    Conversation conv = mWorkingMessage.getConversation();
                    if (conv!= null) {
                        conv.ensureThreadId();
                    }
                    // /M:fix bug ALPS00595715@{
                    mWorkingMessage.saveDraft(false);
                    startActivity(createIntent(getApplicationContext(), conv.getThreadId()));
                    // startMsgListQuery(MESSAGE_LIST_QUERY_TOKEN, 0);
                    ///@}
                    mCutRecipients = null;
                }
                dialog.dismiss();
            }
        });
        /// @}
        mSIMSelectDialog.show();
        /// M: fix bug ALPS00572383, delay show "suggested" on the SelectSimDialog @{
        if (mAssociatedSimQueryDone != -1) {
            mUiHandler.sendEmptyMessage(MSG_SELECT_SIM_DIALOG_SHOW);
        }
        /// @}
    }
    /// @}
    /// M: Code analyze 003,  Set or get max mms size.
    private void initMessageSettings() {
        MessageUtils.setMmsLimitSize(this);
    }
    /// @}

    private void showConfirmDialog(Uri uri, boolean append, int type, int messageId) {
        if (isFinishing()) {
            return;
        }

        final Uri mRestrictedMidea = uri;
        final boolean mRestrictedAppend = append;
        final int mRestrictedType = type;
         
        new AlertDialog.Builder(ComposeMessageActivity.this)
        .setTitle(R.string.unsupport_media_type)
        .setIconAttribute(android.R.attr.alertDialogIcon)
        .setMessage(messageId)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public final void onClick(DialogInterface dialog, int which) {
                    if (mRestrictedMidea == null || mRestrictedType == WorkingMessage.TEXT
                        || mWorkingMessage.isDiscarded()) {
                        return;
                    }
                    getAsyncDialog().runAsync(new Runnable() {
                        public void run() {
                            int createMode = WorkingMessage.sCreationMode;
                            WorkingMessage.sCreationMode = 0;
                            int result = mWorkingMessage.setAttachment(mRestrictedType, mRestrictedMidea,
                                mRestrictedAppend);
                            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                                log("Restricted Midea: dataUri=" + mRestrictedMidea);
                            }
                            if (mRestrictedType == WorkingMessage.IMAGE
                                && (result == WorkingMessage.IMAGE_TOO_LARGE || result == WorkingMessage.MESSAGE_SIZE_EXCEEDED)) {
                                if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                                    log("addImage: resize image " + mRestrictedMidea);
                                }
                                MessageUtils.resizeImage(ComposeMessageActivity.this, mRestrictedMidea, mAttachmentEditorHandler, mResizeImageCallback, mRestrictedAppend,
                                    true);
//                                MessageUtils.resizeImageAsync(ComposeMessageActivity.this, mRestrictedMidea,
//                                    mAttachmentEditorHandler, mResizeImageCallback, mRestrictedAppend);
                                WorkingMessage.sCreationMode = createMode;
                                return;
                            }
                            WorkingMessage.sCreationMode = createMode;
                            int typeId = R.string.type_picture;
                            if (mRestrictedType == WorkingMessage.AUDIO) {
                                typeId = R.string.type_audio;
                            } else if (mRestrictedType == WorkingMessage.VIDEO) {
                                typeId = R.string.type_video;
                            }
                            handleAddAttachmentError(result, typeId);
                        }
                    }, null, R.string.adding_attachments_title);
            }
        })
        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public final void onClick(DialogInterface dialog, int which) {
                mWorkingMessage.removeFakeMmsForDraft();
                updateSendButtonState();
            }
        })
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface arg0) {
                mWorkingMessage.removeFakeMmsForDraft();
                updateSendButtonState();
            }
        })
        .show();
    }

     /// M: Code analyze 067, Add audio attachment. @{
    private void addAudio(final Uri uri, final boolean append) {
        if (uri != null) {
            mNeedSaveAsMms = true;
            int result = WorkingMessage.OK;
            try {
                if (append) {
                    mWorkingMessage.checkSizeBeforeAppend();
                }
            } catch (ExceedMessageSizeException e) {
                result = WorkingMessage.MESSAGE_SIZE_EXCEEDED;
                handleAddAttachmentError(result, R.string.type_audio);
                mNeedSaveAsMms = false;
                return;
            }
            result = mWorkingMessage.setAttachment(WorkingMessage.AUDIO, uri, append);
            if (result == WorkingMessage.WARNING_TYPE) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        showConfirmDialog(uri, append, WorkingMessage.AUDIO, R.string.confirm_restricted_audio);
                    }
                });
            } else {
                handleAddAttachmentError(result, R.string.type_audio);
                if (result != WorkingMessage.OK) {
                    mNeedSaveAsMms = false;
                }
            }
        }
    }
    /// @}

    /// M: Code analyze 015, Add text vcard. @{
    private void addTextVCardAsync(final long[] contactsIds) {
        MmsLog.i(TAG, "compose.addTextVCardAsync(): contactsIds.length() = " + contactsIds.length);
        getAsyncDialog().runAsync(new Runnable() {
            public void run() {
                //addTextVCard(contactsIds);
               String textVCard = TextUtils.isEmpty(mTextEditor.getText())? "": "\n";
               VCardAttachment tvc = new VCardAttachment(ComposeMessageActivity.this);
               final String textString = tvc.getTextVCardString(contactsIds,textVCard);
               runOnUiThread(new Runnable() {
               public void run() {
                   insertText(mTextEditor, textString);
               }

             });
           }
        }, null, R.string.menu_insert_text_vcard);// the string is ok for reuse[or use a new string].
    }
    /// @}

    private void addFileAttachment(String type, Uri uri, boolean append) {
        
        if (!addFileAttachment(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, type, uri, append)) {
            if (!addFileAttachment(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, type, uri, append)) {
                if (!addFileAttachment(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, type, uri, append)) {
                    MmsLog.i(TAG, "This file is not in media store(audio, video or image)," +
                            "attemp to add it like file uri");
                    addAttachment(type, (Uri) uri, append);
                }
            }
        } 
    }

    private boolean addFileAttachment(Uri mediaStoreUri, String type, Uri uri, boolean append) {
        String path = uri.getPath();
        if (path != null) {
            Cursor c = getContentResolver().query(mediaStoreUri, 
                    new String[] {MediaStore.MediaColumns._ID, Audio.Media.MIME_TYPE}, MediaStore.MediaColumns.DATA + "=?",
                    new String[] {path}, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        Uri contentUri = Uri.withAppendedPath(mediaStoreUri, c.getString(0));
                        MmsLog.i(TAG, "Get id in MediaStore:" + c.getString(0));
                        MmsLog.i(TAG, "Get content type in MediaStore:" + c.getString(1));
                        MmsLog.i(TAG, "Get uri in MediaStore:" + contentUri);
                        
                        String contentType = c.getString(1);
                        addAttachment(contentType, contentUri, append);
                        return true;
                    } else {
                        MmsLog.i(TAG, "MediaStore:" + mediaStoreUri.toString() + " has not this file");
                    }
                } finally {
                    c.close();
                }
            }
        }
        return false;
    }

    private boolean isHasRecipientCount(){
        int recipientCount = recipientCount();
        return (recipientCount > 0 && recipientCount < RECIPIENTS_LIMIT_FOR_SMS);
    }

    private String getResourcesString(int id) {
        Resources r = getResources();
        return r.getString(id);
    }

    /// M: Code analyze 030, Check condition before sending message.@{
    private void checkConditionsAndSendMessage(boolean bCheckEcmMode){
        // check pin
        // convert sim id to slot id
        int requestType = EncapsulatedCellConnMgr.REQUEST_TYPE_SIMLOCK;
        final int slotId;
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            requestType = EncapsulatedCellConnMgr.REQUEST_TYPE_ROAMING;
            slotId = SIMInfo.getSlotById(this, mSelectedSimId);
            MmsLog.d(MmsApp.TXN_TAG, "check pin and...: simId=" + mSelectedSimId + "\t slotId=" + slotId);
        } else {
            slotId = 0;
            MmsLog.d(MmsApp.TXN_TAG, "slotId=" + slotId);
            SIMInfo si = SIMInfo.getSIMInfoBySlot(ComposeMessageActivity.this, slotId);
            if (si == null) {
                MmsLog.e(MmsApp.TXN_TAG, "si null");
            } else {
                MmsLog.e(MmsApp.TXN_TAG, "simid=" + si.getSimId());
                mSelectedSimId = (int)si.getSimId();
            }
        }

        if (mIsMessageDefaultSimIpServiceEnabled && isNetworkConnected(getApplicationContext())
                && isCurrentRecipientIpMessageUser()
                && (mIpMessageDraft != null || mIpMessageForSend != null
                        || (mIpMessageDraft == null && !mWorkingMessage.requiresMms()))) {
            checkIpMessageBeforeSendMessage(bCheckEcmMode);
            return;
        }
        final boolean bCEM = bCheckEcmMode;
        ///M: Modify for fix issue ALPS00380788
        final int result = mCellMgr.handleCellConn(slotId, requestType, new Runnable() {
            public void run() {

                removeDialog(CELL_PROGRESS_DIALOG);

                int nRet = mCellMgr.getResult();
                MmsLog.d(MmsApp.TXN_TAG, "serviceComplete result = " + EncapsulatedCellConnMgr.resultToString(nRet));
                if (mCellMgr.RESULT_ABORT == nRet || mCellMgr.RESULT_OK == nRet) {
                    updateSendButtonState();
                    return;
                }
                if (slotId != mCellMgr.getPreferSlot()) {
                    SIMInfo si = SIMInfo.getSIMInfoBySlot(ComposeMessageActivity.this, mCellMgr.getPreferSlot());
                    if (si == null) {
                        MmsLog.e(MmsApp.TXN_TAG, "serviceComplete siminfo is null");
                        updateSendButtonState();
                        return;
                    }
                    mSelectedSimId = (int)si.getSimId();
                }
                checkIpMessageBeforeSendMessage(bCEM);
            }
        });
        /// @}
        ///M: add for fix issue ALPS00380788 {@
        MmsLog.d(MmsApp.TXN_TAG, "result = " + result);
        if (result == mCellMgr.RESULT_WAIT) {
            showDialog(CELL_PROGRESS_DIALOG);
        }
        /// @}
   }
   /// @}

    /// M: Code analyze 049, Update send button or attachment editor state.@{
    private void updateSendButtonState(final boolean enabled) {
        if (!mWorkingMessage.hasSlideshow()) {
            View sendButton = showSmsOrMmsSendButton(mWorkingMessage.requiresMms());
            sendButton.setEnabled(enabled);
            sendButton.setFocusable(enabled);
        } else {
            mAttachmentEditor.setCanSend(enabled);
        }
    }
    /// @}

    private void insertText(EditText edit, String insertText){
        int where = edit.getSelectionStart();

        if (where == -1) {
            edit.append(insertText);
        } else {
            edit.getText().insert(where, insertText);
        }
    }

    /**
     * This filter will constrain edits not to make the length of the text
     * greater than the specified length.
     */  
    class TextLengthFilter implements InputFilter {
        public TextLengthFilter(int max) {
            mMaxLength = max;
            mExceedMessageSizeToast = (mMaxLength == IpMessageConfig.CAPTION_MAX_LENGTH )
                ? Toast.makeText(ComposeMessageActivity.this, R.string.exceed_message_size_limitation,
                    Toast.LENGTH_SHORT)
                : Toast.makeText(ComposeMessageActivity.this, R.string.exceed_message_size_limitation,
                    Toast.LENGTH_SHORT);
        }

        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            
            int keep = mMaxLength - (dest.length() - (dend - dstart));
            
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

        private int mMaxLength;
    }

    /// M: Code analyze 051, Hide input keyboard.@{
    private void hideInputMethod() {
        MmsLog.d(TAG, "hideInputMethod()");
        if (this.getWindow() != null && this.getWindow().getCurrentFocus() != null) {
            mInputMethodManager.hideSoftInputFromWindow(this.getWindow().getCurrentFocus().getWindowToken(), 0);
            mIsKeyboardOpen = false;
        }
    }
    /// @}

    // toast there are too many recipients.
    private void toastTooManyRecipients(int recipientCount) {
        String tooManyRecipients = getString(R.string.too_many_recipients, recipientCount, RECIPIENTS_LIMIT_FOR_SMS);
        Toast.makeText(ComposeMessageActivity.this, tooManyRecipients, Toast.LENGTH_LONG).show();
    }

    /// M: Code analyze 013, Get contacts from Contact app . @{
    private void addContacts(int pickCount) {
        /// M: @{
        /*Intent intent = new Intent("android.intent.action.CONTACTSMULTICHOICE");
          intent.setType(Phone.CONTENT_ITEM_TYPE);
          intent.putExtra("request_email", true);
          intent.putExtra("pick_count", pickCount);
          misPickContatct = true;
          startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);*/
        try {
            /// M: fix bug ALPS00444752, set true to disable to Show ContactPicker
            mShowingContactPicker = true;
            misPickContatct = true;
            Intent intent = new Intent(MessageUtils.ACTION_CONTACT_SELECTION);
            intent.setType(Phone.CONTENT_TYPE);
            startActivityForResult(intent, REQUEST_CODE_PICK);
        } catch (ActivityNotFoundException e) {
            mShowingContactPicker = false;
            misPickContatct = false;
            Toast.makeText(this, this.getString(R.string.no_application_response), Toast.LENGTH_SHORT).show();
            MmsLog.e(TAG, e.getMessage());
        }
        /// @}
    }

    private int getContactSIM(final String num) {
        class Int {
            private int value = -1;
            public void  set(int n) {
                value = n;
            }
            public int get() {
                return value;
            }
        }
        final Int simID = new Int();
        final Object dbQueryLock = new Object();
        final Context mContextTemp = this.getApplicationContext();
        // query the db in another thread.
        new Thread(new Runnable() {
            public void run() {
                int simId = -1;
                String number = num;
                String formatNumber = MessageUtils.formatNumber(number,mContextTemp);
                Cursor associateSIMCursor = ComposeMessageActivity.this.getContentResolver().query(
                    Data.CONTENT_URI, 
                    new String[]{Data.SIM_ASSOCIATION_ID}, 
                    Data.MIMETYPE + "='" + CommonDataKinds.Phone.CONTENT_ITEM_TYPE 
                    + "' AND (" + Data.DATA1 + "=?" +  
                    " OR " + Data.DATA1 +"=?" + 
                    ") AND (" + Data.SIM_ASSOCIATION_ID + "!= -1)", 
                    new String[]{number,formatNumber},
                    null
                );

                if ((null != associateSIMCursor) && (associateSIMCursor.getCount() > 0)) {
                    associateSIMCursor.moveToFirst();
                    // Get only one record is OK
                    simId = (Integer) associateSIMCursor.getInt(0);
                } else {
                    simId = -1;
                }
                if (associateSIMCursor != null) {
                    associateSIMCursor.close();
                }
                synchronized (dbQueryLock) {
                    simID.set(simId);
                    dbQueryLock.notify();
                    /// M: fix bug ALPS00572383, delay show "suggested" on the SelectSimDialog @{
                    mAssociatedSimQueryDone = simId;
                }
                /// @}
            }
        }).start();
        // UI thread wait 500ms at most.
        synchronized (dbQueryLock) {
            try {
                dbQueryLock.wait(500);
            } catch(InterruptedException e) {
                //time out
            }
            return simID.get();
        }
    }

    private void checkRecipientsCount() {
        //if (isRecipientsEditorVisible()) {
        //mRecipientsEditor.structLastRecipient();
        //}
//        hideInputMethod();
        /// M: add for ip message
//        hideSharePanelOrEmoticonPanel();
        final int mmsLimitCount = MmsConfig.getMmsRecipientLimit();
        if (mWorkingMessage.requiresMms() && recipientCount() > mmsLimitCount) {
            String message = getString(R.string.max_recipients_message, mmsLimitCount);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.max_recipients_title);
            builder.setIconAttribute(android.R.attr.alertDialogIcon);
            builder.setCancelable(true);
            builder.setMessage(message);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            /*
                             * If entering an existing thread, #mRecipientsEditor never gets initialized.
                             * So, when mRecipientsEditor is not visible, it might be null.
                             */
                            List<String> recipientsList;
                            if (isRecipientsEditorVisible()) {
                                recipientsList = mRecipientsEditor.getNumbers();
                            } else {
                                recipientsList = new ArrayList<String>(Arrays.asList(getRecipients().getNumbers()));
                            }
                            List<String> newRecipientsList = new ArrayList<String>();

                            if (recipientCount() > mmsLimitCount * 2) {
                                for (int i = 0; i < mmsLimitCount; i++) {
                                    newRecipientsList.add(recipientsList.get(i));
                                }
                                mWorkingMessage.setWorkingRecipients(newRecipientsList);
                            } else {
                                for (int i = recipientCount() - 1; i >= mmsLimitCount; i--) {
                                    recipientsList.remove(i);
                                }
                                mWorkingMessage.setWorkingRecipients(recipientsList);
                                /// M: fix bug ALPS00432629
                                newRecipientsList = recipientsList;
                            }
                            simSelection();

                            /// M: fix bug ALPS00432629, update title
                            /// when recipientsList cut to 20 @{
                            ContactList list = ContactList.getByNumbers(newRecipientsList, false);
                            /// M: fix bug ALPS00439789, check NPE
                            if (isRecipientsEditorVisible()) {
                                mRecipientsEditor.removeTextChangedListener(mRecipientsWatcher);
                                mRecipientsEditor.populate(new ContactList());
                                mRecipientsEditor.addTextChangedListener(mRecipientsWatcher);
                                mRecipientsEditor.populate(list);
                            }
                            updateTitle(list);
                            mCutRecipients = list;
                        }
                    });
                }
            });
            builder.setNegativeButton(R.string.no, null);
            builder.show();
            updateSendButtonState();
        } else {
            /** M:
             * fix CR ALPS00069541
             * if the message copy from sim card with unknown recipient
             * the recipient will be ""
             */
            if (isRecipientsEditorVisible() && "".equals(mRecipientsEditor.getText().toString().replaceAll(";", "").replaceAll(",", "")
                    )) {
                new AlertDialog.Builder(this)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setTitle(R.string.cannot_send_message)
                        .setMessage(R.string.cannot_send_message_reason)
                        .setPositiveButton(R.string.yes, new CancelSendingListener())
                        .show();
            } else if (!isRecipientsEditorVisible() && "".equals(mConversation.getRecipients().serialize().replaceAll(";", "").replaceAll(",", ""))) {
                new AlertDialog.Builder(this)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setTitle(R.string.cannot_send_message)
                        .setMessage(R.string.cannot_send_message_reason)
                        .setPositiveButton(R.string.yes, new CancelSendingListener())
                        .show();
            } else {
                /// M: cmcc feature, reply message with the card directly if only one card related in conversation
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT && MmsConfig.getFolderModeEnabled()) {
                    send_sim_id = checkConversationSingleCardRelated();
                    MmsLog.d(TAG, "send_sim_id="+send_sim_id);
                }
                simSelection();
            }
        }
    }

    private int checkConversationSingleCardRelated() {
        int simId = -1;
        boolean isValid = false;
        long simIdinSetting = EncapsulatedSettings.System.getLong(getContentResolver(),
                             EncapsulatedSettings.System.SMS_SIM_SETTING, 
                             EncapsulatedSettings.System.DEFAULT_SIM_NOT_SET);
        if (simIdinSetting == EncapsulatedSettings.System.SMS_SIM_SETTING_AUTO) {
            if (MmsConfig.getMmsDirMode()) {
                simId = getIntent().getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, -1);;
            } else {
                if (mMsgListAdapter != null) {
                    Cursor c = mMsgListAdapter.getCursor();
                    if (c != null && c.moveToLast()) {
                        int count = c.getCount();
                        MmsLog.d(TAG, "count = " + count);
                        simId = mMsgListAdapter.getSimIdFromCursor(c);
                    }
                }
            }
            for (int i =0; i< mSimCount; i++ ) {
                if (mSimInfoList.get(i).getSimId() == simId) {
                    isValid = true;
                    break;
                }
            }
            if (!isValid) {
                simId = -1;
            }
        }
        return simId;
    }

    /// M: Code analyze 002,  If a new ComposeMessageActivity is created, kill old one
    public static Activity getComposeContext() {
        return sCompose == null ? null : sCompose.get();
    }
    /// @}

   @Override
    public void onShutDown() {
        saveDraft(false);
    }

    /*
    this function is add for read report
    */
    private final int READ_REPORT_DISABLED                      = 0;
    private final int READ_REPORT_SINGLE_MODE_ENABLED           = 1;
    private final int READ_REPORT_GEMINI_MODE_ENABLED           = 2;
//    private final int READ_REPORT_GEMINI_MODE_ENABLED_SLOT_1    = 4;
//    private final int READ_REPORT_GEMINI_MODE_ENABLED_BOTH      = READ_REPORT_GEMINI_MODE_ENABLED_SLOT_0|READ_REPORT_GEMINI_MODE_ENABLED_SLOT_1;
    
    private void checkAndSendReadReport() {
        final Context ct = ComposeMessageActivity.this;
        final long threadId = mConversation.getThreadId();        
        MmsLog.d(MmsApp.TXN_TAG,"checkAndSendReadReport,threadId:" + threadId);
        new Thread(new Runnable() {
            public void run() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ComposeMessageActivity.this);
                int rrAllowed = READ_REPORT_DISABLED;
                /// M: [ALPS00465911] [3G Gemini+]JE when Message -> settings -> cell center -> back to idle @{
                ArrayList<Boolean> allowed = new ArrayList<Boolean>();
                ArrayList<Long> simId = new ArrayList<Long>();
                /// @}
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    /// M: [ALPS00465911] [3G Gemini+]JE when Message -> settings -> cell center -> back to idle @{
                    int slotCount = SIMInfo.getAllSIMCount(ComposeMessageActivity.this);
                    /// @}
                    SIMInfo si = SIMInfo.getSIMInfoBySlot(ComposeMessageActivity.this, 0);
                    for(int i=0; i< slotCount; i++){
                        si = null;
                        si = SIMInfo.getSIMInfoBySlot(ComposeMessageActivity.this, i);
                        if (si != null) {
                            if (prefs.getBoolean(Long.toString(si.getSimId())+ "_" + MmsPreferenceActivity.READ_REPORT_AUTO_REPLY, false) == true) {
                                /// M: [ALPS00465911] [3G Gemini+]JE when Message -> settings -> cell center -> back to idle @{
                                allowed.add(true);
                            } else {
                                allowed.add(false);
                                /// @}
                            }
                            /// M: [ALPS00465911] [3G Gemini+]JE when Message -> settings -> cell center -> back to idle @{
                            simId.add(si.getSimId());
                            MmsLog.d(MmsApp.TXN_TAG,"slot1 simId is:"+si.getSimId());
                            /// @}
                        }
                    }
                    rrAllowed = READ_REPORT_GEMINI_MODE_ENABLED;
                } else {
                    if (prefs.getBoolean(MmsPreferenceActivity.READ_REPORT_AUTO_REPLY, false) == true) {
                        rrAllowed = READ_REPORT_SINGLE_MODE_ENABLED;
                    }
                }
                MmsLog.d(MmsApp.TXN_TAG,"rrAllowed=" + rrAllowed);
                // if read report is off, mark the mms read report status readed.
                if (rrAllowed == READ_REPORT_DISABLED) {
                    ContentValues values = new ContentValues(1);
                    String where = Mms.THREAD_ID + " = " + threadId + " and " + Mms.READ_REPORT + " = 128";
                    // update uri inbox is not used, must indicate here.
                    where += " and " + Mms.MESSAGE_BOX + " = " + Mms.MESSAGE_BOX_INBOX;
                    values.put(Mms.READ_REPORT, PduHeaders.READ_STATUS__DELETED_WITHOUT_BEING_READ);
                    SqliteWrapper.update(ct, ct.getContentResolver(), Mms.Inbox.CONTENT_URI,
                                        values,
                                        where,
                                        null);
                    return;
                }
                if (rrAllowed > READ_REPORT_DISABLED) {
                    StringBuilder suffix = new StringBuilder();
                    switch (rrAllowed) {
                    case READ_REPORT_SINGLE_MODE_ENABLED:
                        //nothing to do in single card mode
                        break;
                    case READ_REPORT_GEMINI_MODE_ENABLED:
                        boolean isAppendAnd = true;
                        for (int i = 0; i < allowed.size(); i++) {
                            if (allowed.get(i)) {
                                // slot i has card and read report on
                                if (isAppendAnd) {
                                    suffix.append(EncapsulatedTelephony.Mms.SIM_ID
                                            + " = " + simId.get(i));
                                    isAppendAnd = false;
                                } else {
                                    suffix.append(" or " + EncapsulatedTelephony.Mms.SIM_ID
                                            + " = " + simId.get(i));
                                }
                            } else {
                                MmsLog.e(MmsApp.TXN_TAG, "mark slot" + i + " card readed");
                                markReadReportProcessed(ct, threadId, simId.get(i));
                            }
                        }
                        if (!TextUtils.isEmpty(suffix.toString())) {
                            suffix.insert(0," and (");
                            suffix.append(") ");
                        }
                    default:
                        MmsLog.e(MmsApp.TXN_TAG,"impossible value for rrAllowed.");
                        break;
                    }
                    boolean networkOk = ((ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE))
                                        .getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS).isAvailable();
                    int airplaneMode = EncapsulatedSettings.System.getInt(ct.getContentResolver(), EncapsulatedSettings.System.AIRPLANE_MODE_ON, 0);                    
                    //network not ok.next time will try.
                    if ((networkOk != true)||(airplaneMode == 1)) {
                        MmsLog.d(MmsApp.TXN_TAG, "networkok:"+networkOk+",airplaneMode:"+airplaneMode);
                        return;
                    }
                    Cursor cs = null;
                    try {
                        String where = Mms.THREAD_ID + " = " + threadId + " and " + Mms.READ_REPORT + " = 128" + suffix.toString();
                        cs = SqliteWrapper.query(ct, ct.getContentResolver(),Mms.Inbox.CONTENT_URI,
                                                new String[]{Mms._ID, EncapsulatedTelephony.Mms.SIM_ID},
                                                where,
                                                null, null);
                        if (cs != null) {
                            final int count = cs.getCount();
                            if (count > 0) {
                                //mark the ones need send read report status to pending as 130.
                                ContentValues values = new ContentValues(1);
                                values.put(Mms.READ_REPORT, 130);
                                // update uri inbox is not used, must indicate here.
                                where += " and " + Mms.MESSAGE_BOX + " = " + Mms.MESSAGE_BOX_INBOX;
                                SqliteWrapper.update(ct, ct.getContentResolver(), Mms.Inbox.CONTENT_URI,
                                                    values,
                                                    where,
                                                    null);
                                //show a toast.
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast.makeText(ComposeMessageActivity.this,
                                                    ct.getResources().getQuantityString(R.plurals.read_report_toast_msg, count, count),
                                                    Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            startSendReadReport(cs);
                        }
                    } catch (Exception e) {
                        MmsLog.e(MmsApp.TXN_TAG,"exception happend when scan read report!:"+e.getMessage());
                    } finally {
                        if (cs != null) {
                            cs.close();
                        }
                    }
                }
            }

            private void markReadReportProcessed(Context ct, long threadId, long simId) {
                ContentValues values = new ContentValues(1);
                values.put(Mms.READ_REPORT, PduHeaders.READ_STATUS__DELETED_WITHOUT_BEING_READ);
                String where = Mms.THREAD_ID + " = " + threadId + " and " + Mms.READ_REPORT + " = 128"
                                    + " and " + EncapsulatedTelephony.Mms.SIM_ID + " = " + simId;
                // update uri inbox is not used, must indicate here.
                where += " and " + Mms.MESSAGE_BOX + " = " + Mms.MESSAGE_BOX_INBOX;                
                SqliteWrapper.update(ct, ct.getContentResolver(), Mms.Inbox.CONTENT_URI,
                                    values,
                                    where,
                                    null);
            }
            
            private void startSendReadReport(final Cursor cursor) {
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    MmsLog.d(MmsApp.TXN_TAG,"send an intent for read report.");
                    long msgId = cursor.getLong(0);
                    Intent rrIntent = new Intent(ct, TransactionService.class);
                    rrIntent.putExtra("uri",Mms.Inbox.CONTENT_URI+"/"+msgId);//the uri of mms that need send rr
                    rrIntent.putExtra("type",Transaction.READREC_TRANSACTION);
                    if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                        int simId = cursor.getInt(1);
                        MmsLog.d(MmsApp.TXN_TAG,"simId:"+simId);
                        rrIntent.putExtra("simId", simId);
                    }
                    ct.startService(rrIntent);
                }
            }
        }).start();
    }
    
    /**
     * Remove the number which is the same as any one before;
     * When the count of recipients over the limit, make a toast and remove the recipients over the limit.
     * @param recipientsString the numbers slipt by ','.
     * @return recipientsString the numbers slipt by ',' after modified.
     */
    private String getStringForMultipleRecipients(String recipientsString) {
        recipientsString = recipientsString.replaceAll(",", ";");
        String[] recipients_all = recipientsString.split(";");
        List<String> recipientsList = new ArrayList<String>();
        for (String recipient : recipients_all) {
            recipientsList.add(recipient);
        }

        Set<String> recipientsSet = new HashSet<String>();
        recipientsSet.addAll(recipientsList);

        if (recipientsSet.size() > RECIPIENTS_LIMIT_FOR_SMS) {
            toastTooManyRecipients(recipients_all.length);
        }

        recipientsList.clear();
        recipientsList.addAll(recipientsSet);

        recipientsString = "";
        int count = recipientsList.size() > RECIPIENTS_LIMIT_FOR_SMS ? RECIPIENTS_LIMIT_FOR_SMS : recipientsList.size();
        StringBuffer buf = new StringBuffer();
        buf.append(recipientsString);
        for (int i = 0; i < count; i++) {
            if (i == (count - 1)) {
                buf.append(recipientsList.get(i));
            } else {
                buf.append(recipientsList.get(i) + ";");
            }
        }
        recipientsString = buf.toString();
        return recipientsString;
    }

    public Conversation getConversation() {
        return mConversation;
    }
    
    /// M: Code analyze 014, Add quick text. @{
    private void showQuickTextDialog() {
        mQuickTextDialog = null;
        //if (mQuickTextDialog == null) {
            List<String> quickTextsList = new ArrayList<String>();

            /// M: new feature, add default quick text when frist "insert quick text" @{
            if (MmsConfig.getInitQuickText()) {
                String[] defaultQuickTexts = getResources().getStringArray(R.array.default_quick_texts);
                for (int i = 0; i < defaultQuickTexts.length; i++) {
                    quickTextsList.add(defaultQuickTexts[i]);
                }
            } else {
                // add user's quick text
                Cursor cursor = getContentResolver().query(EncapsulatedTelephony.MmsSms.CONTENT_URI_QUICKTEXT,
                                                        null, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        quickTextsList.add(cursor.getString(1));
                    }
                    cursor.close();
                }
            }
            /// @}

            List<Map<String, ?>> entries = new ArrayList<Map<String, ?>>();
            for (String text : quickTextsList) {
                HashMap<String, Object> entry = new HashMap<String, Object>();
                entry.put("text", text);
                entries.add(entry);
            }

            final SimpleAdapter qtAdapter = new SimpleAdapter(this, entries, R.layout.quick_text_list_item,
                    new String[] {"text"}, new int[] {R.id.quick_text});
            
            AlertDialog.Builder qtBuilder = new AlertDialog.Builder(this);

            qtBuilder.setTitle(getString(R.string.select_quick_text));
            qtBuilder.setCancelable(true);
            qtBuilder.setAdapter(qtAdapter, new DialogInterface.OnClickListener() {
                @SuppressWarnings("unchecked")
                public final void onClick(DialogInterface dialog, int which) {
                    HashMap<String, Object> item = (HashMap<String, Object>) qtAdapter.getItem(which);
                    if (mSubjectTextEditor != null && mSubjectTextEditor.isFocused()) {
                        insertText(mSubjectTextEditor, (String)item.get("text"));
                    } else {
                        insertText(mTextEditor, (String)item.get("text"));
                    }
                    dialog.dismiss();
                }
            });
            mQuickTextDialog = qtBuilder.create();
        //}
        mQuickTextDialog.show();
    }
    /// @}

    /// M: Code analyze 006, Control SIM indicator on status bar. @{
    @Override
    public void onSimInforChanged() {
        MmsLog.i(MmsApp.LOG_TAG, "onSimInforChanged(): Composer");
        // show SMS indicator
        if (!isFinishing() && mIsShowSIMIndicator) {
            MmsLog.i(MmsApp.LOG_TAG, "Hide current indicator and show new one.");
            mStatusBarManager.hideSIMIndicator(mComponentName);
            mStatusBarManager.showSIMIndicator(mComponentName, EncapsulatedSettings.System.SMS_SIM_SETTING);
        }
        /// M: add for ipmessage
        mMessageSimId = Settings.System.getLong(getContentResolver(), Settings.System.SMS_SIM_SETTING,
            Settings.System.DEFAULT_SIM_NOT_SET);
        if (mMessageSimId != Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK
                && mMessageSimId != Settings.System.DEFAULT_SIM_NOT_SET) {
            mIsMessageDefaultSimIpServiceEnabled = MmsConfig.isServiceEnabled(this, (int) mMessageSimId);
            mSelectedSimId = (int) mMessageSimId;
        } else {
            mIsMessageDefaultSimIpServiceEnabled = MmsConfig.isServiceEnabled(this);
            mSelectedSimId = 0;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateSendButtonState();
            }
        });
    }
    /// @}

    /// M: Code analyze 004, Set max height for text editor. @{
    private final HeightChangedLinearLayout.LayoutSizeChangedListener mLayoutSizeChangedListener = 
            new HeightChangedLinearLayout.LayoutSizeChangedListener() {
        private int mMaxHeight = 0;
        @Override
        public void onLayoutSizeChanged(int w, int h, int oldw, int oldh) {
            /// M: fix bug ALPS00419856, set TextEditor Height = four when unlock screen @{
            if (h - oldh > SOFT_KEY_BOARD_MIN_HEIGHT) {
                mIsSoftKeyBoardShow = false;
            } else {
                mIsSoftKeyBoardShow = true;
            }
            /// @}
            mMaxHeight = (h > mMaxHeight) ? h : mMaxHeight;
            synchronized (mWaitingImeChangedObject) {
                mWaitingImeChangedObject.notifyAll();
                MmsLog.d(TAG, "onLayoutSizeChanged(): object notified.");
            }
            if (h == oldh || mTextEditor == null || mTextEditor.getVisibility() == View.GONE) {
                return;
            }
            MmsLog.d(TAG, "onLayoutSizeChanged(): mIsLandscape = " + mIsLandscape);
            if (!mIsLandscape) {
                if (h > oldh && !isSharePanelOrEmoticonPanelShow() && !mShowKeyBoardFromShare
                        && !mShowKeyBoardFromEmoticon && (h == mMaxHeight)) {
                    updateTextEditorHeightInFullScreen();
                } else {
                    mUiHandler.postDelayed(new Runnable() {
                        public void run() {
                            MmsLog.d(TAG, "onLayoutSizeChanged(): mTextEditor.setMaxHeight: "
                                    + mReferencedTextEditorFourLinesHeight);
                            mTextEditor.setMaxHeight(mReferencedTextEditorFourLinesHeight
                                    * mCurrentMaxHeight / mReferencedMaxHeight);
                        }
                    }, 100);
                }
            }
        }
    };

    private void updateTextEditorHeightInFullScreen() {
        if (mIsLandscape || mTextEditor == null || mTextEditor.getVisibility() == View.GONE) {
            return;
        }
        mUiHandler.postDelayed(new Runnable() {
            public void run() {
                MmsLog.d(TAG, "updateTextEditorHeightInFullScreen()");
                updateFullScreenTextEditorHeight();
            }
        }, 100);
    }

    private void updateFullScreenTextEditorHeight() {
        if (mAttachmentEditor.getVisibility() == View.VISIBLE
                && mAttachmentEditor.getHeight() > 0
                && !mWorkingMessage.hasSlideshow()) {
            MmsLog.d(TAG, "updateTextEditorHeight(): mTextEditor.setMaxHeight: "
                    + (mReferencedTextEditorSevenLinesHeight
                            * mCurrentMaxHeight / mReferencedMaxHeight));
            if (mIsLandscape) {
                mTextEditor.setMaxHeight(
                        mReferencedTextEditorTwoLinesHeight * mCurrentMaxHeight / mReferencedMaxHeight);
            } else {
                mTextEditor.setMaxHeight(mReferencedTextEditorSevenLinesHeight
                        * mCurrentMaxHeight / mReferencedMaxHeight);
            }
        } else {
            /// M: fix bug ALPS00419856, set TextEditor Height = four when unlock screen @{
            if (mIsSoftKeyBoardShow && !mIsLandscape) {
                MmsLog.d(TAG, "updateFullScreenTextEditorHeight() updateTextEditorHeight()" +
                            ": mTextEditor.setMaxHeight: " + (mReferencedTextEditorFourLinesHeight
                                * mCurrentMaxHeight / mReferencedMaxHeight));
                mTextEditor.setMaxHeight(mReferencedTextEditorFourLinesHeight
                        * mCurrentMaxHeight / mReferencedMaxHeight);
            /// @}
            } else if (!mIsSoftKeyBoardShow && !mIsLandscape) {
                MmsLog.d(TAG, "updateTextEditorHeight(): mTextEditor.setMaxHeight: "
                    + ((mReferencedTextEditorSevenLinesHeight + mReferencedAttachmentEditorHeight)
                            * mCurrentMaxHeight / mReferencedMaxHeight));
                mTextEditor.setMaxHeight(
                        (mReferencedTextEditorSevenLinesHeight + mReferencedAttachmentEditorHeight)
                        * mCurrentMaxHeight / mReferencedMaxHeight);
            } else {
                MmsLog.d(TAG, "updateTextEditorHeight(): mTextEditor.setMaxHeight: "
                    + (mReferencedTextEditorTwoLinesHeight * mCurrentMaxHeight
                            / mReferencedMaxHeight));
                mTextEditor.setMaxHeight(mReferencedTextEditorTwoLinesHeight * mCurrentMaxHeight
                            / mReferencedMaxHeight);
            }
        }
    }
    /// @}

   @Override
    public void startActivity(Intent intent) {
        try {
            super.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent mChooserIntent = Intent.createChooser(intent, null);
            super.startActivity(mChooserIntent);
        }
    }

    /**
     * Simple cache to prevent having to load the same PduBody again and again for the same uri.
     */

    private boolean needSaveDraft() {
        return ((!isRecipientsEditorVisible())
                    || (mRecipientsEditor.hasValidRecipient(mWorkingMessage.requiresMms())))
                && !mWorkingMessage.isDiscarded()
                && (mWorkingMessage.isWorthSaving() || mIpMessageDraft != null);
    }

    public void onPreMmsSent() {
        startMsgListQuery(MESSAGE_LIST_QUERY_TOKEN,0);
    }

    private boolean checkSlideCount(boolean append) {
        String mMsg = this.getString(R.string.cannot_add_slide_anymore);
        Toast mToast = Toast.makeText(this, mMsg, Toast.LENGTH_SHORT);
        int mSlideCount = 0;
        SlideshowModel slideShow = mWorkingMessage.getSlideshow();
        if (slideShow != null) {
            mSlideCount = slideShow.size();
        }
        if (mSlideCount >= SlideshowEditor.MAX_SLIDE_NUM && append) {
            mToast.show();
            return false;
        }
        return true;
    }

    /// M: Code analyze 003,  Set or get max mms size.
    public static long computeAttachmentSizeLimitForAppen(SlideshowModel slideShow) {
        long sizeLimit = MmsConfig.getUserSetMmsSizeLimit(true) - SlideshowModel.MMS_SLIDESHOW_INIT_SIZE;
        if (slideShow != null) {
            sizeLimit -= slideShow.getCurrentSlideshowSize();
        }
        if (sizeLimit > 0) {
            return sizeLimit;
        }
        return 0;
    }
    /// @}

    /// M: Code analyze 009,Show attachment dialog . @{
    private class SoloAlertDialog extends AlertDialog {
        private AlertDialog mAlertDialog;

        private SoloAlertDialog(Context context) {
            super(context);
        }

        public boolean needShow() {
            return mAlertDialog == null || !mAlertDialog.isShowing();
        }

        public void show(final boolean append) {
            if (!needShow()) {
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setIcon(R.drawable.ic_dialog_attach);
            builder.setTitle(R.string.add_attachment);

            // if (mAttachmentTypeSelectorAdapter == null) {
            // add for vcard, if there is a real slideshow, hide vCard
            int mode = AttachmentTypeSelectorAdapter.MODE_WITH_SLIDESHOW;
            if (mWorkingMessage.hasSlideshow()) {
                mode |= AttachmentTypeSelectorAdapter.MODE_WITHOUT_FILE_ATTACHMENT;
            } else {
                mode |= AttachmentTypeSelectorAdapter.MODE_WITH_FILE_ATTACHMENT;
            }
            if (MessageUtils.isVCalendarAvailable(ComposeMessageActivity.this)) {
                mode |= AttachmentTypeSelectorAdapter.MODE_WITH_VCALENDAR;
            }
            mAttachmentTypeSelectorAdapter = new AttachmentTypeSelectorAdapter(getContext(), mode);
            // }
            builder.setAdapter(mAttachmentTypeSelectorAdapter,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            addAttachment(mAttachmentTypeSelectorAdapter.buttonToCommand(which), append);
                            dialog.dismiss();
                        }
                    });
            mAlertDialog = builder.show();
        }

        public void dismiss() {
            if (mAlertDialog != null && mAlertDialog.isShowing()) {
                mAlertDialog.dismiss();
            }
            super.dismiss();
        }
    }
    /// @}

    private void waitForCompressing() {
        synchronized (ComposeMessageActivity.this) {
            while (mCompressingImage) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    MmsLog.e(TAG, "intterrupted exception e ", e);
                }
            }
        }
    }

    private void notifyCompressingDone() {
        synchronized (ComposeMessageActivity.this) {
            mCompressingImage = false;
            notify();
        }
    }

    private void setFileAttachment(final String fileName, final int type, final boolean append) {
        final File attachFile = getFileStreamPath(fileName);
        MmsLog.d(TAG, "setFileAttachment(): attachFile.exists()?=" + attachFile.exists() +
                        ", attachFile.length()=" + attachFile.length());
        final Resources res = getResources();
        if (attachFile.exists() && attachFile.length() > 0) {
            Uri attachUri = Uri.fromFile(attachFile);
            int result = WorkingMessage.OK;
            try {
                if (append) {
                    mWorkingMessage.checkSizeBeforeAppend();
                }
            } catch (ExceedMessageSizeException e) {
                result = WorkingMessage.MESSAGE_SIZE_EXCEEDED;
                handleAddAttachmentError(result, R.string.type_common_file);
                return;
            }
            result = mWorkingMessage.setAttachment(type, attachUri, append);
            handleAddAttachmentError(result, R.string.type_common_file);
        } else {
            mUiHandler.post(new Runnable() {
                public void run() {
                    Toast.makeText(ComposeMessageActivity.this,
                            res.getString(R.string.failed_to_add_media, fileName), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    private void asyncAttachVCalendar(final Uri eventUri) {
        if (eventUri == null) {
            return;
        }
        getAsyncDialog().runAsync(new Runnable() {
            public void run() {
                attachVCalendar(eventUri);
                mWorkingMessage.saveDraft(false);
            }
        }, null, R.string.adding_attachments_title);
    }

    private void attachVCalendar(Uri eventUri) {
        if (eventUri == null) {
            MmsLog.e(TAG, "attachVCalendar, oops uri is null");
            return;
        }
        int result = WorkingMessage.OK;

        //result = mWorkingMessage.setAttachment(WorkingMessage.VCALENDAR, eventUri, false);
        /// M:Modify ALPS00474719 @{
        IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin =
            (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);
        if (mAppendAttachmentSign && mMmsAttachmentEnhancePlugin != null &&
                           mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true)
        {
            //append vcalender and OP01
            /// M fix CR ALPS00587132
            /// for OP01, append vcalender, first check size limitation before append @{
            try {
                mWorkingMessage.checkSizeBeforeAppend();
            } catch (ExceedMessageSizeException e) {
                result = WorkingMessage.MESSAGE_SIZE_EXCEEDED;
                handleAddAttachmentError(result, R.string.type_picture);
                return;
            }
            /// @}
            result = mWorkingMessage.setAttachment(WorkingMessage.VCALENDAR, eventUri, true);
        }else {
            //replace or Not OP01
            result = mWorkingMessage.setAttachment(WorkingMessage.VCALENDAR, eventUri, false);
        }
        /// @}
        handleAddAttachmentError(result, R.string.type_common_file);
    }

    /// M: Code analyze 019, Add vcard attachment.  @{
    private void asyncAttachVCardByContactsId(final Intent data, final boolean isAddingIpMsgVCardFile) {
        if (data == null) {
            return;
        }
        getAsyncDialog().runAsync(new Runnable() {
            public void run() {
                long[] contactsId = data.getLongArrayExtra("com.mediatek.contacts.list.pickcontactsresult");
                VCardAttachment va = new VCardAttachment(ComposeMessageActivity.this);
                if (isAddingIpMsgVCardFile) {
                    mIpMessageVcardName = va.getVCardFileNameByContactsId(contactsId, true);
                    mDstPath = IpMessageUtils.getCachePath(ComposeMessageActivity.this) + mIpMessageVcardName;
                    MmsLog.d(IPMSG_TAG, "asyncAttachVCardByContactsId(): mIpMessageVcardName = " + mIpMessageVcardName
                        + ", mDstPath = " + mDstPath);
                    mIpMsgHandler.postDelayed(mSendVcard, 100);
                } else {
                    mWorkingMessage.setIsDeleteVcardFile(false);
                    String fileName = va.getVCardFileNameByContactsId(contactsId, false);
                    /// M: Modify ALPS00474719 @{
                    IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);
                    if (mAppendAttachmentSign && mMmsAttachmentEnhancePlugin != null &&
                           mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true)
                    {
                        //add vcard and OP01
                        setFileAttachment(fileName, WorkingMessage.VCARD, true);
                    } else {
                        //replace or Not OP01
                    setFileAttachment(fileName, WorkingMessage.VCARD, false);
                    }
                    //setFileAttachment(fileName, WorkingMessage.VCARD, false);
                    /// @}
                    mWorkingMessage.saveDraft(false);
                    mWorkingMessage.setIsDeleteVcardFile(true);
                }
            }
        }, null, R.string.adding_attachments_title);
    }
    /// @}

    /// M: Code analyze 047, Extra uri from message body and get number from uri.
    /// Then use this number to update contact cache. @{
    private void updateContactCache(Cursor cursor) {
        if (cursor != null) {
            Set<SpannableString> msgs = new HashSet<SpannableString>();
            while (cursor.moveToNext()) {
                String smsBody = cursor.getString(MessageListAdapter.COLUMN_SMS_BODY);

                if (smsBody == null) {
                    continue;
                }

                SpannableString msg = new SpannableString(smsBody);
                msgs.add(msg);
            }
            // update the contact cache in an async thread to avoid ANR
            updateContactCacheAsync(msgs);
        }
    }

    private void updateContactCacheAsync(final Set<SpannableString> msgs) {
        new Thread(new Runnable() {
            public void run() {
                Set<String> uriSet = new HashSet<String>();
                for (SpannableString msg : msgs) {
                    Linkify.addLinks(msg, Linkify.ALL);
                    List<String> uris = MessageUtils.extractUris(msg.getSpans(0, msg.length(),
                        URLSpan.class));
                    for (String uri : uris) {
                        uriSet.add(uri);
                    }
                }
                for (String uri : uriSet) {
                    String[] body = uri.toLowerCase().split("tel:");
                    if (body.length > 1) {
                        Contact.get(body[1].trim(), false);
                    }
                }
            }
        }).start();
    }
    /// @}

    /// M: Code analyze 001, Plugin opeartor. @{
    private void initPlugin(Context context) {
        mMmsComposePlugin = (IMmsCompose) MmsPluginManager
                .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_COMPOSE);
        mMmsComposePlugin.init(this);
        /// M: Code analyze 036, Change text size if adjust font size.@{
        mMmsTextSizeAdjustPlugin = (IMmsTextSizeAdjust)
            MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_TEXT_SIZE_ADJUST);
        /// @}
    }
    /// @}

    /// M: Code analyze 036, Change text size if adjust font size.@{
    public void setTextSize(float size) {
        if (mTextEditor != null) {
            mTextEditor.setTextSize(size);
        }
        if (mMsgListAdapter != null) {
            mMsgListAdapter.setTextSize(size);
        }

        if (mMsgListView != null && mMsgListView.getVisibility() == View.VISIBLE) {
            int count = mMsgListView.getChildCount();
            for (int i = 0; i < count; i++) {
                MessageListItem item =  (MessageListItem)mMsgListView.getChildAt(i);
                if (item != null) {
                    item.setBodyTextSize(size);
                }
            }
        }
    }
    /// @}

    public boolean  dispatchTouchEvent(MotionEvent ev) {
        boolean ret = false;
        /// M: Code analyze 001, Plugin opeartor. @{
        if (mMmsTextSizeAdjustPlugin != null) {
                ret = mMmsTextSizeAdjustPlugin.dispatchTouchEvent(ev);
            }
        /// @}
        if (!ret) {
            ret = super.dispatchTouchEvent(ev);
        }
        return ret;
    }

    /// M: add for common
    private void initShareAndEmoticonRessource() {
        mShareButton = (ImageButton) findViewById(R.id.share_button);
        mShareButton.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mShowKeyBoardFromShare) {
                    showSharePanelOrEmoticonPanel(false, false, true);
                } else {
                    showSharePanelOrEmoticonPanel(true, false, false);
                    mTextEditor.requestFocus();
                }
            }
        });
        mSharePanel = (SharePanel) findViewById(R.id.share_panel);
        mSharePanel.setHandler(mIpMsgHandler);

        mEmoticonButton = (ImageButton) findViewById(R.id.emoticon_button);
        mEmoticonButton.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mShowKeyBoardFromEmoticon) {
                    showSharePanelOrEmoticonPanel(false, false, true);
                } else {
                    showSharePanelOrEmoticonPanel(false, true, false);
                    mTextEditor.requestFocus();
                }
            }
        });

        LinearLayout container = (LinearLayout)findViewById(R.id.panel_container);
        LayoutInflater factory = LayoutInflater.from(this);
        if (MmsConfig.getIpMessagServiceId(this) == IpMessageServiceId.NO_SERVICE) {
            factory.inflate(R.layout.emoticon_panel, container);
        } else { /// M: ipmessage emoticon panel
            factory.inflate(R.layout.emoticon_panel_ipmessage, container);
        }
        mEmoticonPanel = (EmoticonPanel) container.findViewById(R.id.emoticon_panel);

        mEmoticonPanel.setHandler(mIpMsgHandler);
        mEmoticonPanel.setEditEmoticonListener(new EditEmoticonListener() {
            @Override
            public void doAction(int type, String emotionName) {
                switch (type) {
                case EditEmoticonListener.addEmoticon:
                    insertEmoticon(emotionName);
                    break;
                case EditEmoticonListener.delEmoticon:
                    deleteEmoticon();
                    break;
                case EditEmoticonListener.sendEmoticon:
                    if (IpMessageUtils.checkCurrentIpMessageServiceStatus(ComposeMessageActivity.this, true, null)) {
                        sendEmoticon(emotionName);
                    }
                    break;
                default:
                    break;
                }
            }
        });
        showSharePanelOrEmoticonPanel(false, false, false);
    }
    /// M: add for ip message
    private class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            MmsLog.d(TAG, "MessageReceiver.onReceive(): get broadcast action is: " + action);
            if (TextUtils.isEmpty(action)) {
                return;
            }
            int i = 0;
            switch (i/*action*/) {
            case 1:
//                if (NmsIpMessageConsts.NmsNewMessageAction.NMS_NEW_MESSAGE_ACTION.equals(action)) {
//                    SNmsMsgKey msgKey = (SNmsMsgKey) intent.getSerializableExtra(SNmsMsgKey.MsgKeyName);
//                    if (null == msgKey || msgKey.contactRecId != mContactId) {
//                        return;
//                    }
//                    setMessageAsRead();
//                    if (mMarkState) {
//                        onlyRefreshList();
//                    } else {
//                        refreshAndScrollList();
//                    }
//                }
                break;
            case 2:
//                if (NmsIpMessageConsts.NmsRefreshMsgList.NMS_REFRESH_MSG_LIST.equals(action)) {
//                    refreshAndScrollList();
//                }
                break;
            case 3:
//                if (NmsIpMessageConsts.NmsIpMessageStatus.NMS_MESSAGE_STATUS_ACTION.equals(action)) {
//                    onlyRefreshList();
//                }
                break;
            case 4:
//                if (NmsIpMessageConsts.NmsDownloadAttachStatus.NMS_DOWNLOAD_ATTACH_STATUS_ACTION
//                        .equals(action)) {
//                    int status = intent
//                            .getIntExtra(
//                                    NmsIpMessageConsts.NmsDownloadAttachStatus.NMS_DOWNLOAD_ATTACH_STATUS_ACTION,
//                                    -2);
//                    if (status == NmsIpMessageConsts.NmsDownloadAttachStatus.DONE) {
//                        onlyRefreshList();
//                    } else {
//                        if (status == NmsIpMessageConsts.NmsDownloadAttachStatus.FAILED) {
//                            Toast.makeText(mContext, R.string.STR_NMS_DOWNLOAD_FAILED,
//                                    Toast.LENGTH_SHORT).show();
//                            onlyRefreshList();
//                        } else {
//                            mChatListAdapter.updateDownLoadPercentage();
//                        }
//                    }
//                }
                break;
            case 5:
//                if (NmsIpMessageConsts.NMS_INTENT_DEL_IP_MSG_DONE.equals(action)) {
//                    if (mDialog != null && mDialog.isShowing()) {
//                        mDialog.dismiss();
//                    }
//                    refreshAndScrollList();
//                }
                break;
            case 6:
//                if (NmsIpMessageConsts.NmsUpdateGroupAction.NMS_UPDATE_GROUP.equals(action)) {
//                    int groupId = intent.getIntExtra(
//                            NmsIpMessageConsts.NmsUpdateGroupAction.NMS_GROUP_ID, -1);
//                    if (groupId == mContactId) {
//                        initialize();
//                        invalidateOptionsMenu();
//                    }
//                }
                break;
            case 7:
                break;
            default:
                break;
            }
        }
    }

    private void showSharePanel(boolean isShow) {
        if (null != mSharePanel) {
            if (isShow) {
                mSharePanel.setVisibility(View.VISIBLE);
                mShareButton.setImageResource(R.drawable.ipmsg_keyboard);
            } else {
                mSharePanel.setVisibility(View.GONE);
                mShareButton.setImageResource(R.drawable.ipmsg_share);
            }
            mShowKeyBoardFromShare = isShow;
        }
    }

    private void showEmoticonPanel(boolean isShow) {
        if (null != mEmoticonPanel) {
            if (isShow) {
                mEmoticonPanel.setVisibility(View.VISIBLE);
                mEmoticonButton.setImageResource(R.drawable.ipmsg_keyboard);
            } else {
                mEmoticonPanel.setVisibility(View.GONE);
                mEmoticonButton.setImageResource(R.drawable.ipmsg_emoticon);
            }
            mShowKeyBoardFromEmoticon = isShow;
        }
    }

    private void showKeyBoard(boolean isShow) {
        if (isShow) {
            mTextEditor.requestFocus();
            mInputMethodManager.showSoftInput(mTextEditor, 0);
        } else {
            hideInputMethod();
        }
    }

    public boolean isSharePanelOrEmoticonPanelShow() {
        if (null != mSharePanel && mSharePanel.isShown()) {
            return true;
        }
        if (null != mEmoticonPanel && mEmoticonPanel.isShown()) {
            return true;
        }
        return false;
    }

    public void showSharePanelOrEmoticonPanel(final boolean isShowShare, final boolean isShowEmoticon,
            final boolean isShowKeyboard) {
        if (isShowShare && isShowEmoticon) {
            MmsLog.w(IPMSG_TAG, "Can not show both SharePanel and EmoticonPanel");
            return;
        }

        MmsLog.d(TAG, "showSharePanelOrEmoticonPanel(): isShowShare = " + isShowShare
            + ", isShowEmoticon = " + isShowEmoticon + ", isShowKeyboard = " + isShowKeyboard
            + ", mIsSoftKeyBoardShow = " + mIsSoftKeyBoardShow);
        if (!isShowKeyboard && mIsSoftKeyBoardShow) {
            if (!isShowShare && mShowKeyBoardFromShare) {
                showSharePanel(isShowShare);
            }
            if (!isShowEmoticon && mShowKeyBoardFromEmoticon) {
                showEmoticonPanel(isShowEmoticon);
            }
            mShowKeyBoardFromShare = isShowShare;
            mShowKeyBoardFromEmoticon = isShowEmoticon;
            showKeyBoard(isShowKeyboard);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (mWaitingImeChangedObject) {
                        try {
                            /// M: fix bug ALPS00447850, wait HideSoftKeyBoard longer
                            int waitTime = 300;
                            MmsLog.d(TAG, "showSharePanelOrEmoticonPanel(): object start wait.");
                            mWaitingImeChangedObject.wait(waitTime);
                            MmsLog.d(TAG, "showSharePanelOrEmoticonPanel(): object end wait.");
                        } catch (InterruptedException e) {
                            MmsLog.d(TAG, "InterruptedException");
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isShowShare) {
                                showSharePanel(isShowShare);
                            }
                            if (isShowEmoticon) {
                                showEmoticonPanel(isShowEmoticon);
                            }
                            if (isShowKeyboard) {
                                showKeyBoard(isShowKeyboard);
                            }
                            if (isShowShare || isShowEmoticon || isShowKeyboard) {
                                if (mIsLandscape) {
                                    mTextEditor.setMaxHeight(mReferencedTextEditorTwoLinesHeight * mCurrentMaxHeight / mReferencedMaxHeight);
                                } else {
                                    mTextEditor.setMaxHeight(mReferencedTextEditorFourLinesHeight * mCurrentMaxHeight / mReferencedMaxHeight);
                                }
                            } else {
                                MmsLog.d(TAG, "showSharePanelOrEmoticonPanel(): new thread.");
                                updateFullScreenTextEditorHeight();
                            }
                        }
                    });
                }
            }).start();
        } else {
            showSharePanel(isShowShare);
            showEmoticonPanel(isShowEmoticon);
            showKeyBoard(isShowKeyboard);
            if (isShowShare || isShowEmoticon || isShowKeyboard) {
                if (mIsLandscape) {
                    mTextEditor.setMaxHeight(
                            mReferencedTextEditorTwoLinesHeight * mCurrentMaxHeight / mReferencedMaxHeight);
                } else {
                    mTextEditor.setMaxHeight(mReferencedTextEditorFourLinesHeight * mCurrentMaxHeight / mReferencedMaxHeight);
                }
            } else {
                MmsLog.d(TAG, "showSharePanelOrEmoticonPanel()");
                updateFullScreenTextEditorHeight();
            }
        }
    }

    public void hideSharePanelOrEmoticonPanel() {
        MmsLog.d(TAG, "hideSharePanelOrEmoticonPanel()");
        showSharePanelOrEmoticonPanel(false, false, false);
        updateFullScreenTextEditorHeight();
    }

    private void insertEmoticon(String text) {
        mTextEditor.requestFocus();
        int index = mTextEditor.getSelectionStart();
        Editable edit = mTextEditor.getEditableText();
        ///M: modify for ALPS00435510, show toast when insert emotion == MaxTextLimit @{
        if (text.length() > MmsConfig.getMaxTextLimit() - edit.length()) {
            if (mExceedMessageSizeToast != null) {
                mExceedMessageSizeToast.show();
            }
            return;
        }
        if (mEmoticonNumber >= MAX_SPAN_NUMBER) {
            Toast.makeText(this, R.string.meet_emoticon_limit, Toast.LENGTH_SHORT).show();
            return;
        }
        if (index < 0 || index >= edit.length()) {
            edit.append(text);
        } else {
            edit.insert(index, text);
        }
    }

    private void deleteEmoticon() {
        Editable edit = mTextEditor.getEditableText();
        int length = edit.length();
        int cursor = mTextEditor.getSelectionStart();
        if (length == 0 || cursor == 0) {
            return;
        }
        ImageSpan[] spans = edit.getSpans(0, cursor, ImageSpan.class);
        ImageSpan span = null;
        int index = 0;
        if (null != spans && spans.length != 0) {
            span = spans[spans.length - 1];
            index = edit.getSpanEnd(span);
        }

        if (index == cursor) {
            int start = edit.getSpanStart(span);
            edit.delete(start, cursor);
        } else {
            edit.delete(cursor - 1, cursor);
        }
        mEmoticonNumber = SmileyParser2.getInstance().addSmileySpans(edit, MAX_SPAN_NUMBER);
    }

    private void sendEmoticon(String body) {
        if (mSimCount == 0) {
            toastNoSimCard(this);
            return;
        }
        if (isRecipientsEditorVisible() && TextUtils.isEmpty(mRecipientsEditor.getText().toString())) {
            toastNoRecipients(this);
            return;
        }
        if (TextUtils.isEmpty(body)) {
            MmsLog.w(IPMSG_TAG, "sendEmoticon(): No content for sending!");
            return;
        }
        IpTextMessage msg = new IpTextMessage();
        msg.setBody(body);
        msg.setType(IpMessageType.TEXT);
        mIpMessageForSend = msg;
        mIsClearTextDraft = false;
        sendMessageForIpMsg(mIpMessageForSend, "send emoticon msg failed", false, false);
    }

    private static void toastNoRecipients(Context context) {
        Toast.makeText(context, IpMessageUtils.getResourceManager(context)
            .getSingleString(IpMessageConsts.string.ipmsg_need_input_recipients), Toast.LENGTH_SHORT).show();
    }

    public void setHandler(Handler handler) {
        mIpMsgHandler = handler;
        if (null != mEmoticonPanel) {
            mEmoticonPanel.setHandler(mIpMsgHandler);
        }
        if (null != mSharePanel) {
            mSharePanel.setHandler(mIpMsgHandler);
        }
    }

    private void initIpMessageResourceRefs() {
        mIpMessageThumbnail = (ImageView) findViewById(R.id.ip_message_thumbnail);
        mSendButtonIpMessage = (ImageButton) findViewById(R.id.send_button_ipmsg);
        mSendButtonIpMessage.setOnClickListener(this);

        mTypingStatus = (TextView) findViewById(R.id.ip_message_typing_status);
        mTypingStatusView = (LinearLayout) findViewById(R.id.ip_message_typing_status_view);

        mInviteView = findViewById(R.id.invitation_linear);
        mInviteMsg = (TextView)findViewById(R.id.tv_invite_msg);
        mInvitePostiveBtn = (Button)findViewById(R.id.bt_invite_postive);
        mInviteNegativeBtn = (Button)findViewById(R.id.bt_invite_negative);
    }

    private void doMmsAction(Message msg) {
        int commonAttachmentType = 0;
        Bundle bundle = msg.getData();
        int action = bundle.getInt(SharePanel.SHARE_ACTION);
        switch (action) {
        case SharePanel.TAKE_PICTURE:
            commonAttachmentType = AttachmentTypeSelectorAdapter.TAKE_PICTURE;
            break;

        case SharePanel.RECORD_VIDEO:
            commonAttachmentType = AttachmentTypeSelectorAdapter.RECORD_VIDEO;
            break;

        case SharePanel.RECORD_SOUND:
            commonAttachmentType = AttachmentTypeSelectorAdapter.RECORD_SOUND;
            break;

        case SharePanel.ADD_VCARD:
            commonAttachmentType = AttachmentTypeSelectorAdapter.ADD_VCARD;
            break;

        case SharePanel.ADD_IMAGE:
            commonAttachmentType = AttachmentTypeSelectorAdapter.ADD_IMAGE;
            break;

        case SharePanel.ADD_VIDEO:
            commonAttachmentType = AttachmentTypeSelectorAdapter.ADD_VIDEO;
            break;


        case SharePanel.ADD_SOUND:
            commonAttachmentType = AttachmentTypeSelectorAdapter.ADD_SOUND;
            break;

        case SharePanel.ADD_VCALENDAR:
            commonAttachmentType = AttachmentTypeSelectorAdapter.ADD_VCALENDAR;
            break;

        case SharePanel.ADD_SLIDESHOW:
            commonAttachmentType = AttachmentTypeSelectorAdapter.ADD_SLIDESHOW;
            break;

        default:
            MmsLog.e(IPMSG_TAG, "doMoreActionForMms(): invalid share action type: " + action);
            hideSharePanelOrEmoticonPanel();
            return;
        }
       /// M: Vcard and slides can be in same screen
       //add for attachment enhance
        IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin =
                    (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(
                                MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);
        if (mMmsAttachmentEnhancePlugin != null) {
           if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance()) {
               //OP01
               addAttachment(commonAttachmentType, true);
               MmsLog.e(TAG, "attach: addAttachment(commonAttachmentType, true)");
           } else {
               //Not OP01
               addAttachment(commonAttachmentType, !mWorkingMessage.hasAttachedFiles());
               MmsLog.e(TAG, "attach: addAttachment(commonAttachmentType, !mWorkingMessage.hasAttachedFiles())");
           }
        } else {
           addAttachment(commonAttachmentType, !mWorkingMessage.hasAttachedFiles());
           MmsLog.e(TAG, "attach: addAttachment(commonAttachmentType, !mWorkingMessage.hasAttachedFiles())");
        }
        /// @}
       // addAttachment(commonAttachmentType, !mWorkingMessage.hasAttachedFiles());
        hideSharePanelOrEmoticonPanel();
    }

    private void doMoreAction(Message msg) {
        Bundle bundle = msg.getData();
        int action = bundle.getInt(SharePanel.SHARE_ACTION);
        if (mWorkingMessage.requiresMms()
                && action != SharePanel.IPMSG_DRAW_SKETCH && action != SharePanel.IPMSG_SHARE_LOCATION) {
            doMoreActionForMms(msg);
            return;
        }
        boolean isNoRecipient = isRecipientsEditorVisible() && TextUtils.isEmpty(mRecipientsEditor.getText().toString());
        switch (action) {
        case SharePanel.IPMSG_TAKE_PHOTO:
            if (isNoRecipient && (!mIsCaptionOn || !mIsImageCaptionOn)) {
                toastNoRecipients(this);
                return;
            }
            takePhoto();
            break;

        case SharePanel.IPMSG_RECORD_VIDEO:
            if (isNoRecipient && (!mIsCaptionOn || !mIsVideoCaptionOn)) {
                toastNoRecipients(this);
                return;
            }
            recordVideo();
            break;

        case SharePanel.IPMSG_DRAW_SKETCH:
            if (isNoRecipient) {
                toastNoRecipients(this);
                return;
            }
            drawSketch();
            break;

        case SharePanel.IPMSG_SHARE_CONTACT:
            if (isNoRecipient) {
                toastNoRecipients(this);
                return;
            }
            shareContact();
            break;

        case SharePanel.IPMSG_CHOOSE_PHOTO:
            if (isNoRecipient && (!mIsCaptionOn || !mIsImageCaptionOn)) {
                toastNoRecipients(this);
                return;
            }
            choosePhoto();
            break;

        case SharePanel.IPMSG_CHOOSE_VIDEO:
            if (isNoRecipient && (!mIsCaptionOn || !mIsVideoCaptionOn)) {
                toastNoRecipients(this);
                return;
            }
            chooseVideo();
            break;

        case SharePanel.IPMSG_RECORD_AUDIO:
            if (isNoRecipient && (!mIsCaptionOn || !mIsAudioCaptionOn)) {
                toastNoRecipients(this);
                return;
            }
            recordAudio();
            break;

        case SharePanel.IPMSG_SHARE_LOCATION:
            if (isNoRecipient) {
                toastNoRecipients(this);
                return;
            }
            shareLocation();
            break;

        case SharePanel.IPMSG_CHOOSE_AUDIO:
            if (isNoRecipient && (!mIsCaptionOn || !mIsAudioCaptionOn)) {
                toastNoRecipients(this);
                return;
            }
            chooseAudio();
            break;

        case SharePanel.IPMSG_SHARE_CALENDAR:
            if (isNoRecipient) {
                toastNoRecipients(this);
                return;
            }
            shareCalendar();
//            showSharePanelOrEmoticonPanel(false, false, false);
//            addAttachment(AttachmentTypeSelectorAdapter.ADD_VCALENDAR, false);
            break;

        case SharePanel.IPMSG_SHARE_SLIDESHOW:
            showSharePanelOrEmoticonPanel(false, false, false);
            mIsEditingSlideshow = true;
            addAttachment(AttachmentTypeSelectorAdapter.ADD_SLIDESHOW, !mWorkingMessage.hasAttachedFiles());
            break;

        default:
            MmsLog.e(IPMSG_TAG, "doMoreAction(): invalid share action type: " + action);
            break;
        }
        hideSharePanelOrEmoticonPanel();
    }

    private void doMoreActionForMms(Message msg) {
        int commonAttachmentType = 0;
        Bundle bundle = msg.getData();
        int action = bundle.getInt(SharePanel.SHARE_ACTION);
        switch (action) {
        case SharePanel.IPMSG_TAKE_PHOTO:
            commonAttachmentType = AttachmentTypeSelectorAdapter.TAKE_PICTURE;
            break;

        case SharePanel.IPMSG_RECORD_VIDEO:
            commonAttachmentType = AttachmentTypeSelectorAdapter.RECORD_VIDEO;
            break;

        case SharePanel.IPMSG_SHARE_CONTACT:
            commonAttachmentType = AttachmentTypeSelectorAdapter.ADD_VCARD;
            break;

        case SharePanel.IPMSG_CHOOSE_PHOTO:
            commonAttachmentType = AttachmentTypeSelectorAdapter.ADD_IMAGE;
            break;

        case SharePanel.IPMSG_CHOOSE_VIDEO:
            commonAttachmentType = AttachmentTypeSelectorAdapter.ADD_VIDEO;
            break;

        case SharePanel.IPMSG_RECORD_AUDIO:
            commonAttachmentType = AttachmentTypeSelectorAdapter.RECORD_SOUND;
            break;

        case SharePanel.IPMSG_CHOOSE_AUDIO:
            commonAttachmentType = AttachmentTypeSelectorAdapter.ADD_SOUND;
            break;

        case SharePanel.IPMSG_SHARE_CALENDAR:
            commonAttachmentType = AttachmentTypeSelectorAdapter.ADD_VCALENDAR;
            break;

        case SharePanel.IPMSG_SHARE_SLIDESHOW:
            commonAttachmentType = AttachmentTypeSelectorAdapter.ADD_SLIDESHOW;
            break;

        case SharePanel.IPMSG_DRAW_SKETCH: /// M: unsupport sketch
        case SharePanel.IPMSG_SHARE_LOCATION: /// M: unsupport location
            MmsLog.e(IPMSG_TAG, "doMoreActionForMms(): unsupport action type: " + action);
            if (mSimCount == 0) {
                toastNoSimCard(this);
                return;
            }
            if (!IpMessageUtils.getSDCardStatus()) {
                MessageUtils.createLoseSDCardNotice(this,
                        IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_cant_share));
                return;
            }
            if (!isNetworkConnected(ComposeMessageActivity.this)) {
                Toast.makeText(this, R.string.nointernet, Toast.LENGTH_SHORT).show();
                return;
            }
            IpMessageUtils.checkCurrentIpMessageServiceStatus(this, true,null);
            hideSharePanelOrEmoticonPanel();
            return;
        default:
            MmsLog.e(IPMSG_TAG, "doMoreActionForMms(): invalid share action type: " + action);
            hideSharePanelOrEmoticonPanel();
            return;
        }
        addAttachment(commonAttachmentType, !mWorkingMessage.hasAttachedFiles());
        hideSharePanelOrEmoticonPanel();
    }

    public void takePhoto() {
        Intent imageCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        String fileName = System.currentTimeMillis() + ".jpg";
        mPhotoFilePath = MmsConfig.getPicTempPath(this) + File.separator + fileName;
        mDstPath = mPhotoFilePath;
        File out = new File(mPhotoFilePath);
        Uri uri = Uri.fromFile(out);
        imageCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        imageCaptureIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        try {
            startActivityForResult(imageCaptureIntent, REQUEST_CODE_IPMSG_TAKE_PHOTO);
        } catch (Exception e) {
            Toast.makeText(this,
                            IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_no_app),
                            Toast.LENGTH_SHORT).show();
            MmsLog.e(IPMSG_TAG, "takePhoto()", e);
        }
    }

    public void choosePhoto() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        String fileName = System.currentTimeMillis() + ".jpg";
        mPhotoFilePath = MmsConfig.getPicTempPath(this) + File.separator + fileName;
        mDstPath = mPhotoFilePath;
        try {
            startActivityForResult(intent, REQUEST_CODE_IPMSG_CHOOSE_PHOTO);
        } catch (Exception e) {
            Toast.makeText(this,
                            IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_no_app),
                            Toast.LENGTH_SHORT).show();
            MmsLog.e(IPMSG_TAG, "choosePhoto()", e);
        }
    }

    public void recordVideo() {
        int durationLimit = MessageUtils.getVideoCaptureDurationLimit();
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, (long) 300 * 1024);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, durationLimit);
        try {
            startActivityForResult(intent, REQUEST_CODE_IPMSG_RECORD_VIDEO);
        } catch (Exception e) {
            Toast.makeText(this,
                            IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_no_app),
                            Toast.LENGTH_SHORT).show();
            MmsLog.e(IPMSG_TAG, "recordVideo()", e);
        }
    }

    public void chooseVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        try {
            startActivityForResult(intent, REQUEST_CODE_IPMSG_CHOOSE_VIDEO);
        } catch (Exception e) {
            Toast.makeText(this,
                            IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_no_app),
                            Toast.LENGTH_SHORT).show();
            MmsLog.e(IPMSG_TAG, "chooseVideo()", e);
        }
    }

    public void recordAudio() {
        Intent intent = new Intent(RemoteActivities.AUDIO);
        intent.putExtra(RemoteActivities.KEY_REQUEST_CODE, REQUEST_CODE_IPMSG_RECORD_AUDIO);
        intent.putExtra(IpMessageConsts.RemoteActivities.KEY_SIZE, 300L * 1024);
        IpMessageUtils.startRemoteActivityForResult(this, intent);
    }

    public void drawSketch() {
        Intent intent = new Intent(RemoteActivities.SKETCH);
        intent.putExtra(RemoteActivities.KEY_REQUEST_CODE, REQUEST_CODE_IPMSG_DRAW_SKETCH);
        IpMessageUtils.startRemoteActivityForResult(this, intent);
    }

    public void shareContact() {
        Intent intent = new Intent("android.intent.action.contacts.list.PICKMULTICONTACTS");
        intent.setType(Contacts.CONTENT_TYPE);
        startActivityForResult(intent, REQUEST_CODE_IPMSG_SHARE_CONTACT);
    }

    public void shareLocation() {
        Intent intent = new Intent(RemoteActivities.LOCATION);
        intent.putExtra(RemoteActivities.KEY_REQUEST_CODE, REQUEST_CODE_IPMSG_SHARE_LOCATION);
        IpMessageUtils.startRemoteActivityForResult(this, intent);
    }

    public void chooseAudio() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle(getString(R.string.add_music));
        String[] items = new String[2];
        items[0] = getString(R.string.attach_ringtone);
        items[1] = getString(R.string.attach_sound);
        alertBuilder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        MessageUtils.selectRingtone(ComposeMessageActivity.this, REQUEST_CODE_IPMSG_CHOOSE_AUDIO);
                        break;
                    case 1:
                        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                            Toast.makeText(getApplicationContext(),
                                getString(R.string.Insert_sdcard), Toast.LENGTH_LONG).show();
                            return;
                        }
                        MessageUtils.selectAudio(ComposeMessageActivity.this, REQUEST_CODE_IPMSG_CHOOSE_AUDIO);
                        break;
                   default:
                        break;
                }
            }
        });
        alertBuilder.create().show();
    }

    public void shareCalendar() {
        Intent intent = new Intent("android.intent.action.CALENDARCHOICE");
        intent.setType("text/x-vcalendar");
        intent.putExtra("request_type", 0);
        try {
            startActivityForResult(intent, REQUEST_CODE_IPMSG_SHARE_VCALENDAR);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this,
                            IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_no_app),
                            Toast.LENGTH_SHORT).show();
            MmsLog.e(IPMSG_TAG, "shareCalendar()", e);
        }
    }

    private void setEmoticon(int startPosition) {
        /// M: Fix bug ALPS00563527, optimize emotion drawing.
        ImageSpan[] emoticonList = mLatestText.getSpans(0, mLatestText.length(), ImageSpan.class);
        if (emoticonList != null && emoticonList.length > 0) {
            int spanEnd = mLatestText.getSpanEnd(emoticonList[emoticonList.length - 1]);
            MmsLog.v(TAG, "spanEnd  " + spanEnd + " startPosition " + startPosition);
            if (mEmoticonNumber >= MAX_SPAN_NUMBER && startPosition > spanEnd) {
                return;
            }
        }
        if (mEmoticonNumber >= MAX_SPAN_NUMBER) {
            if (!isAddSmileySpans) {
                isAddSmileySpans = true;
                mUiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        String lastText = mLatestText.toString();
                        mEmoticonNumber = SmileyParser2.getInstance().addSmileySpans(mLatestText, MAX_SPAN_NUMBER);
                        isAddSmileySpans = false;
                        if (mLatestText != null && !lastText.equals(mLatestText.toString())) {
                            setEmoticon(0);
                        }
                    }
                }, 150);
            }
        } else {
            mEmoticonNumber = SmileyParser2.getInstance().addSmileySpans(mLatestText, MAX_SPAN_NUMBER);
        }
    }

    private void onIpMsgActivityResult(Context context, int requestCode, int resultCode, Intent data) {
        MmsLog.d(IPMSG_TAG, "onIpMsgActivityResult(): requestCode = " + requestCode + ", resultCode = " + resultCode
                + ", data = " + data);
        if (resultCode != RESULT_OK) {
            MmsLog.d(IPMSG_TAG, "bail due to resultCode=" + resultCode);
            return;
        }

        /// M: add for ip message
        switch (requestCode) {
            case REQUEST_CODE_INVITE_FRIENDS_TO_CHAT:
                List<String> allList = new ArrayList<String>();
                ContactList contactList = mConversation.getRecipients();
                if (contactList != null && contactList.size() > 0) {
                    for (Iterator it = contactList.iterator(); it.hasNext();) {
                        Contact c = (Contact) it.next();
                        allList.add(IpMessageUtils.getContactManager(this).getContactIdByNumber(c.getNumber()) + "");
                    }
                }

                String[] mSelectContactsIds = data.getStringArrayExtra(IpMessageUtils.SELECTION_CONTACT_RESULT);
                if (mSelectContactsIds != null && mSelectContactsIds.length > 0) {
                    String idStr = "";
                    for (int index = 0; index < mSelectContactsIds.length; index++) {
                        idStr = mSelectContactsIds[index];
                        if (!allList.contains(idStr)) {
                            allList.add(idStr);
                        }
                    }
                }

                if (allList != null && allList.size() > 0) {
                    Intent intent = new Intent(RemoteActivities.NEW_GROUP_CHAT);
                    intent.putExtra(RemoteActivities.KEY_SIM_ID, data.getIntExtra(KEY_SELECTION_SIMID, 0));
                    intent.putExtra(RemoteActivities.KEY_ARRAY, allList.toArray(new String[allList
                            .size()]));
                    IpMessageUtils.startRemoteActivity(this, intent);
                    allList = null;
                } else {
                    MmsLog.d(IPMSG_TAG, "onActivityResult(): SELECT_CONTACT get contact id is NULL!");
                }
                return;
        case REQUEST_CODE_IPMSG_TAKE_PHOTO:
            if (!IpMessageUtils.isValidAttach(mDstPath, false)) {
                Toast.makeText(this,
                                IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_err_file),
                                Toast.LENGTH_SHORT).show();
                return;
            }
            if (!IpMessageUtils.isPic(mDstPath)) {
                Toast.makeText(this,
                                IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_invalid_file_type),
                                Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(mResizePic, "ipmessage_resize_pic").start();
            return;

        case REQUEST_CODE_IPMSG_RECORD_VIDEO:
            if (!getVideoOrPhoto(data, requestCode)) {
                Toast.makeText(this,
                                IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_err_file),
                                Toast.LENGTH_SHORT).show();
                return;
            }
            if (!IpMessageUtils.isVideo(mDstPath)) {
                Toast.makeText(this,
                                IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_invalid_file_type),
                                Toast.LENGTH_SHORT).show();
                return;
            }
            if (!IpMessageUtils.isFileStatusOk(this, mDstPath)) {
                MmsLog.e(TAG, "onIpMsgActivityResult(): record video failed, invalid file");
                return;
            }
            mIpMsgHandler.postDelayed(mSendVideo, 100);
            return;

        case REQUEST_CODE_IPMSG_DRAW_SKETCH:
            if (!getVideoOrPhoto(data, requestCode)) {
                Toast.makeText(this,
                                IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_err_file),
                                Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(mResizeSketch).start();
            return;

        case REQUEST_CODE_IPMSG_SHARE_CONTACT:
            asyncAttachVCardByContactsId(data, true);
            return;

        case REQUEST_CODE_IPMSG_CHOOSE_PHOTO:
            if (!getVideoOrPhoto(data, requestCode)) {
                Toast.makeText(this,
                                IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_err_file),
                                Toast.LENGTH_SHORT).show();
                return;
            }
            if (!IpMessageUtils.isPic(mDstPath)) {
                Toast.makeText(this,
                                IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_invalid_file_type),
                                Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(mResizePic).start();
            return;

        case REQUEST_CODE_IPMSG_CHOOSE_VIDEO:
            if (!getVideoOrPhoto(data, requestCode)) {
                if (IpMessageUtils.getFileSize(mDstPath) > IpMessageConfig.MAX_ATTACH_SIZE) {
                    Toast.makeText(this,
                                    IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                                .getSingleString(IpMessageConsts.string.ipmsg_file_limit),
                                    Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this,
                                    IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                                .getSingleString(IpMessageConsts.string.ipmsg_err_file),
                                    Toast.LENGTH_SHORT).show();
                }
                return;
            }
            if (!IpMessageUtils.isVideo(mDstPath)) {
                Toast.makeText(this,
                                IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_invalid_file_type),
                                Toast.LENGTH_SHORT).show();
                return;
            }
            if (!IpMessageUtils.isFileStatusOk(this, mDstPath)) {
                MmsLog.e(TAG, "onIpMsgActivityResult(): choose video failed, invalid file");
                return;
            }
            int mmsSizeLimit = 300 * 1024;
            if (IpMessageUtils.getFileSize(mDstPath) > mmsSizeLimit) {
                Toast.makeText(this,
                                IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_file_limit),
                                Toast.LENGTH_SHORT).show();
                return;
            }
            mIpMsgHandler.postDelayed(mSendVideo, 100);
            return;

        case REQUEST_CODE_IPMSG_RECORD_AUDIO:
            if (!getVideoOrPhoto(data, requestCode)) {
                return;
            }
            if (!IpMessageUtils.isAudio(mDstPath)) {
                Toast.makeText(this,
                                IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_invalid_file_type),
                                Toast.LENGTH_SHORT).show();
                return;
            }
            if (!IpMessageUtils.isFileStatusOk(this, mDstPath)) {
                MmsLog.e(TAG, "onIpMsgActivityResult(): record audio failed, invalid file");
                return;
            }
            mIpMsgHandler.postDelayed(mSendAudio, 100);
            return;

        case REQUEST_CODE_IPMSG_SHARE_LOCATION:
            sendLocationMsg(data);
            return;
        case REQUEST_CODE_IPMSG_CHOOSE_AUDIO:
            if (data != null) {
                Uri uri = (Uri) data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (EncapsulatedSettings.System.DEFAULT_RINGTONE_URI.equals(uri)) {
                    return;
                }
                if (getAudio(data)) {
                    mIpMsgHandler.postDelayed(mSendAudio, 100);
                }
            }
            return;
        case REQUEST_CODE_IPMSG_SHARE_VCALENDAR:
            String calendar = data.getDataString();
            if (TextUtils.isEmpty(calendar)) {
                return;
            }
            getCalendar(this, calendar);
            mIpMsgHandler.postDelayed(mSendCalendar, 100);
            return;
        case REQUEST_CODE_IPMSG_PICK_CONTACT:
            if (null != data) {
                processPickResult(data);
            } else {
              mIsRecipientHasIntentNotHandle = true;
              mIntent = data;
            }
            misPickContatct = false;
            return;
        default:
            break;
        }
    }

    private Runnable mResizePic = new Runnable() {
        @Override
        public void run() {
            MmsLog.d(IPMSG_TAG, "mResizePic(): start resize pic.");
            byte[] img = IpMessageUtils.resizeImg(mPhotoFilePath, (float) 500);
            if (null == img) {
                return;
            }
            MmsLog.d(IPMSG_TAG, "mResizePic(): put stream to file.");
            try {
                IpMessageUtils.nmsStream2File(img, mDstPath);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            MmsLog.d(IPMSG_TAG, "mResizePic(): post send pic.");
            mIpMsgHandler.postDelayed(mSendPic, 100);
        }
    };

    private Runnable mResizeSketch = new Runnable() {
        @Override
        public void run() {
            byte[] img = IpMessageUtils.resizeImg(mPhotoFilePath, (float) 500);
            if (null == img) {
                return;
            }
            try {
                IpMessageUtils.nmsStream2File(img, mDstPath);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            mIpMsgHandler.postDelayed(mSendSketch, 100);
        }
    };

    private boolean sendMessageForIpMsg(final IpMessage ipMessage, final String log,
                                    boolean isSendSecondTextMessage, final boolean isDelDraft) {
        MmsLog.d(IPMSG_TAG, "sendMessageForIpMsg(): start.");
        if (mWorkingMessage.requiresMms() && mIpMessageForSend == null) {
            mIpMessageDraft = ipMessage;
            convertIpMessageToMmsOrSms(true);
            updateSendButtonState();
            return false;
        }
        int simId = 0;
        if (mSimInfoList.size() > 1) {
            MmsLog.d(IPMSG_TAG, "sendMessageForIpMsg(): More than two SIM card!");
            if (mSelectedSimId > 0) {
                MmsLog.d(IPMSG_TAG, "sendMessageForIpMsg(): has selected SIM, simId = " + mSelectedSimId);
                simId = mSelectedSimId;
            } else {
                MmsLog.d(IPMSG_TAG, "sendMessageForIpMsg(): no selected SIM, check recipients count, " +
                        "and show SIM selection dialog if need.");
                if (mIpMessageForSend == null) {
                    mIpMessageDraft = ipMessage;
                }
                checkRecipientsCount();
                return false;
            }
        } else if (mSimInfoList.size() == 1) {
            simId = (int) mSimInfoList.get(0).getSimId();
        } else {
            MmsLog.d(IPMSG_TAG, "sendMessageForIpMsg(): No SIM card! Convert to MMS!");
            if (mIpMessageForSend == null) {
                mIpMessageDraft = ipMessage;
                convertIpMessageToMmsOrSms(true);
                updateSendButtonState();
                return false;
            } else {
                toastNoSimCard(this);
                return false;
            }
        }
        String to = getCurrentNumberString();
        MmsLog.d(IPMSG_TAG, "sendMessageForIpMsg(): simId = " + simId + ", to = " + to);
        ipMessage.setSimId(simId);
        ipMessage.setTo(to);
        if (TextUtils.isEmpty(ipMessage.getTo()) && mIpMessageForSend == null) {
            saveIpMessageForAWhile(ipMessage);
            updateSendButtonState();
            mSendButtonCanResponse = true;
            return false;
        }

        if ((EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT && !MmsConfig.isServiceEnabled(this, simId))
                || (!EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT && !MmsConfig.isServiceEnabled(this))) {
            if (mIpMessageForSend == null) {
                mIpMessageDraft = ipMessage;
            }
            if (ipMessage.getType() == IpMessageType.TEXT) {
                showIpMessageConvertToMmsOrSmsDialog(SMS_CONVERT, SERVICE_IS_NOT_ENABLED);
            } else {
                showIpMessageConvertToMmsOrSmsDialog(MMS_CONVERT, SERVICE_IS_NOT_ENABLED);
            }
            return false;
        }
        if (!isCurrentRecipientIpMessageUser()) {
            if (mIpMessageForSend == null) {
                mIpMessageDraft = ipMessage;
            }

            if (ipMessage.getType() == IpMessageType.TEXT) {
                showIpMessageConvertToMmsOrSmsDialog(SMS_CONVERT, RECIPIENTS_ARE_NOT_IP_MESSAGE_USER);
            } else {
                showIpMessageConvertToMmsOrSmsDialog(MMS_CONVERT, RECIPIENTS_ARE_NOT_IP_MESSAGE_USER);
            }
            return false;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                MmsLog.d(IPMSG_TAG, "sendMessageForIpMsg(): calling API: saveIpMsg().");
                int ret = -1;
                ipMessage.setStatus(IpMessageStatus.OUTBOX);

                ret = IpMessageUtils.getMessageManager(ComposeMessageActivity.this)
                        .saveIpMsg(ipMessage, IpMessageSendMode.AUTO);

                if (isDelDraft) {
                    IpMessageUtils.getChatManager(ComposeMessageActivity.this).deleteDraftMessageInThread(mConversation.getThreadId()) ;
                }

                if (ret < 0) {
                    MmsLog.w(IPMSG_TAG, "sendMessageForIpMsg(): " + log);
                }
                mScrollOnSend = true;   // in the next onQueryComplete, scroll the list to the end.
            }
        }).start();

        if (isSendSecondTextMessage && TextUtils.isEmpty(IpMessageUtils.getIpMessageCaption(ipMessage))
                && mTextEditor != null && mTextEditor.getVisibility() == View.VISIBLE
                && !TextUtils.isEmpty(mTextEditor.getText().toString())) {
            IpTextMessage ipTextMessage = new IpTextMessage();
            ipTextMessage.setBody(mTextEditor.getText().toString());
            ipTextMessage.setSimId(simId);
            ipTextMessage.setTo(to);
            ipTextMessage.setStatus(IpMessageStatus.OUTBOX);

            final IpMessage ipTextMessageForSend = ipTextMessage;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    MmsLog.d(IPMSG_TAG, "sendMessageForIpMsg(): send second text IP message, calling API: saveIpMsg().");
                    int ret = -1;
                    ret = IpMessageUtils.getMessageManager(ComposeMessageActivity.this)
                            .saveIpMsg(ipTextMessageForSend, IpMessageSendMode.AUTO);
                    if (ret < 0) {
                        MmsLog.w(IPMSG_TAG, "sendMessageForIpMsg(): send second text IP message failed!");
                    }
                    mScrollOnSend = true;   // in the next onQueryComplete, scroll the list to the end.
                }
            }).start();
        }
        mWorkingMessage.syncWorkingRecipients();
        mConversation.guaranteeThreadId();
        if (mIpMessageForSend == null) {
            onPreMessageSent();
            MmsLog.d(IPMSG_TAG, "sendMessageForIpMsg(): after guaranteeThreadId(), threadId = " +
                                mConversation.getThreadId());
        } else {
            resetMessage();
            mIpMessageForSend = null;
        }
        onMessageSent();
        return true;
    }

    private void sendIpTextMessage() {
        String body = mTextEditor.getText().toString();
        if (TextUtils.isEmpty(body)) {
            MmsLog.w(IPMSG_TAG, "sendIpTextMessage(): No content for sending!");
            return;
        }
        IpTextMessage msg = new IpTextMessage();
        msg.setBody(body);
        msg.setType(IpMessageType.TEXT);
        mIpMessageForSend = msg;
        mIsClearTextDraft = true;
        sendMessageForIpMsg(msg, "send text msg failed", false, false);
    }

    private Runnable mSendAudio = new Runnable() {
        public void run() {
            /// M: cracks, wait activity resume, ensure dialog context valid.
            if (!isFinishing() && !mIsRunning) {
                mIpMsgHandler.postDelayed(mSendAudio, 100);
                MmsLog.d(IPMSG_TAG, "mSendAudio, wait activity resume.");
                return;
            }

            if (IpMessageUtils.isExistsFile(mDstPath) && IpMessageUtils.getFileSize(mDstPath) != 0) {
                IpVoiceMessage msg = new IpVoiceMessage();
                msg.setPath(mDstPath);
                msg.setDuration(mDuration);
                msg.setType(IpMessageType.VOICE);
                mIpMessageForSend = msg;
                resetSelectSimId();
                if (mIsCaptionOn && mIsAudioCaptionOn) {
                    mWorkingMessage.asyncDeleteDraftSmsMessage(mConversation);
                    if (mTextEditor.getText().toString().length() > IpMessageConfig.CAPTION_MAX_LENGTH) {
                        mIpMessageDraft = msg;
                        mIpMessageForSend = null;
                        convertIpMessageToMmsOrSms(true);
                        updateSendButtonState();
                        return;
                    }
                    mIsEditingCaption = true;
                    saveIpMessageForAWhile(msg);
                    updateSendButtonState();
                    mSendButtonCanResponse = true;
                } else {
                    sendMessageForIpMsg(msg, "send Audio msg failed", false, false);
                }
                mIpMsgHandler.removeCallbacks(mSendAudio);
            }
        }
    };

    private Runnable mSendPic = new Runnable() {
        public void run() {
            /// M: cracks, wait activity resume, ensure dialog context valid.
            if (!isFinishing() && !mIsRunning) {
                mIpMsgHandler.postDelayed(mSendPic, 100);
                MmsLog.d(IPMSG_TAG, "mSendPic, wait activity resume.");
                return;
            }

            MmsLog.d(IPMSG_TAG, "mSendPic(): start.");
            if (IpMessageUtils.isExistsFile(mDstPath) && IpMessageUtils.getFileSize(mDstPath) != 0) {
                MmsLog.d(IPMSG_TAG, "mSendPic(): start send image.");
                sendImage(REQUEST_CODE_IPMSG_TAKE_PHOTO);
                mIpMsgHandler.removeCallbacks(mSendPic);
//                refreshAndScrollList();
            }
            MmsLog.d(IPMSG_TAG, "mSendPic(): end.");
        }
    };

    private Runnable mSendVideo = new Runnable() {
        public void run() {
            /// M: cracks, wait activity resume, ensure dialog context valid.
            if (!isFinishing() && !mIsRunning) {
                mIpMsgHandler.postDelayed(mSendVideo, 100);
                MmsLog.d(IPMSG_TAG, "mSendVideo, wait activity resume.");
                return;
            }

            MmsLog.d(IPMSG_TAG, "mSendVideo(): start send video. Path = " + mDstPath);
            if (IpMessageUtils.isExistsFile(mDstPath) && IpMessageUtils.getFileSize(mDstPath) != 0) {
                IpVideoMessage msg = new IpVideoMessage();
                msg.setPath(mDstPath);
                msg.setDuration(mDuration);
                msg.setType(IpMessageType.VIDEO);
                mIpMessageForSend = msg;
                resetSelectSimId();
                if (mIsCaptionOn && mIsVideoCaptionOn) {
                    mWorkingMessage.asyncDeleteDraftSmsMessage(mConversation);
                    if (mTextEditor.getText().toString().length() > IpMessageConfig.CAPTION_MAX_LENGTH) {
                        mIpMessageDraft = msg;
                        mIpMessageForSend = null;
                        convertIpMessageToMmsOrSms(true);
                        updateSendButtonState();
                        return;
                    }
                    mIsEditingCaption = true;
                    saveIpMessageForAWhile(msg);
                    updateSendButtonState();
                    mSendButtonCanResponse = true;
                } else {
                    sendMessageForIpMsg(msg, "send Video msg failed", false, false);
                }
                mIpMsgHandler.removeCallbacks(mSendVideo);
//                refreshAndScrollList();
            }
        }
    };

    private Runnable mSendVcard = new Runnable() {
        public void run() {
            /// M: cracks, wait activity resume, ensure dialog context valid.
            if (!isFinishing() && !mIsRunning) {
                mIpMsgHandler.postDelayed(mSendVcard, 100);
                MmsLog.d(IPMSG_TAG, "mSendVcard, wait activity resume.");
                return;
            }

            if (IpMessageUtils.isExistsFile(mDstPath) && IpMessageUtils.getFileSize(mDstPath) != 0) {
                IpVCardMessage msg = new IpVCardMessage();
                msg.setPath(mDstPath);
                msg.setName(mIpMessageVcardName);
                msg.setType(IpMessageType.VCARD);

                resetSelectSimId();
                mIpMessageForSend = msg;
                sendMessageForIpMsg(msg, "send vcard msg failed", false, false);
                mIpMsgHandler.removeCallbacks(mSendVcard);
//                refreshAndScrollList();
            }
        }
    };

    private Runnable mSendSketch = new Runnable() {
        public void run() {
            /// M: cracks, wait activity resume, ensure dialog context valid.
            if (!isFinishing() && !mIsRunning) {
                mIpMsgHandler.postDelayed(mSendSketch, 100);
                MmsLog.d(IPMSG_TAG, "mSendSketch, wait activity resume.");
                return;
            }

            if (IpMessageUtils.isExistsFile(mDstPath) && IpMessageUtils.getFileSize(mDstPath) != 0) {
                sendImage(REQUEST_CODE_IPMSG_DRAW_SKETCH);
                mIpMsgHandler.removeCallbacks(mSendSketch);
//                refreshAndScrollList();
            }
        }
    };

    private Runnable mSendCalendar = new Runnable() {
        public void run() {
            /// M: cracks, wait activity resume, ensure dialog context valid.
            if (!isFinishing() && !mIsRunning) {
                mIpMsgHandler.postDelayed(mSendCalendar, 100);
                MmsLog.d(IPMSG_TAG, "mSendCalendar, wait activity resume.");
                return;
            }

            if (IpMessageUtils.isExistsFile(mDstPath) && IpMessageUtils.getFileSize(mDstPath) != 0) {
                IpVCalendarMessage msg = new IpVCalendarMessage();
                msg.setPath(mDstPath);
                msg.setSummary(mCalendarSummary);
                msg.setType(IpMessageType.CALENDAR);

                resetSelectSimId();
                mIpMessageForSend = msg;
                sendMessageForIpMsg(mIpMessageForSend, "send vCalendar msg failed", false, false);
                mIpMsgHandler.removeCallbacks(mSendCalendar);
            }
        }
    };

    public boolean getVideoOrPhoto(Intent data, int requestCode) {
        if (null == data) {
            MmsLog.e(IPMSG_TAG, "getVideoOrPhoto(): take video error, result intent is null.");
            return false;
        }

        Uri uri = data.getData();
        Cursor cursor = null;
        if (requestCode == REQUEST_CODE_IPMSG_TAKE_PHOTO || requestCode == REQUEST_CODE_IPMSG_CHOOSE_PHOTO) {
            final String[] selectColumn = { "_data" };
            cursor = mContentResolver.query(uri, selectColumn, null, null, null);
        } else {
            final String[] selectColumn = { "_data", "duration" };
            cursor = mContentResolver.query(uri, selectColumn, null, null, null);
        }
        if (null == cursor) {
            if (requestCode == REQUEST_CODE_IPMSG_RECORD_AUDIO) {
                mDstPath = uri.getEncodedPath();
                mDuration = data.getIntExtra("audio_duration", 0);
                mDuration = mDuration / 1000 == 0 ? 1 : mDuration / 1000;
            } else {
                mPhotoFilePath = uri.getEncodedPath();
                mDstPath = mPhotoFilePath;
            }
            return true;
        }
        if (0 == cursor.getCount()) {
            cursor.close();
            MmsLog.e(IPMSG_TAG, "getVideoOrPhoto(): take video cursor getcount is 0");
            return false;
        }
        cursor.moveToFirst();
        if (requestCode == REQUEST_CODE_IPMSG_TAKE_PHOTO || requestCode == REQUEST_CODE_IPMSG_CHOOSE_PHOTO) {
            mPhotoFilePath = cursor.getString(cursor.getColumnIndex("_data"));
            mDstPath = mPhotoFilePath;
        } else {
            mDstPath = cursor.getString(cursor.getColumnIndex("_data"));
            mDuration = cursor.getInt(cursor.getColumnIndex("duration"));
            mDuration = mDuration / 1000 == 0 ? 1 : mDuration / 1000;
        }
        if (null != cursor && !cursor.isClosed()) {
            cursor.close();
        }
        return true;
    }

    private boolean getAudio(Intent data) {
        Uri uri = (Uri) data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (EncapsulatedSettings.System.DEFAULT_RINGTONE_URI.equals(uri)) {
            return false;
        }
        if (null == uri) {
            uri = data.getData();
        }
        if (null == uri) {
            MmsLog.e(IPMSG_TAG, "getAudio(): choose audio failed, uri is null");
            return false;
        }
        final String scheme = uri.getScheme();
        if (scheme.equals("file")) {
            mDstPath = uri.getEncodedPath();
        } else {
            ContentResolver cr = getContentResolver();
            Cursor c = cr.query(uri, null, null, null, null);
            c.moveToFirst();
            mDstPath = c.getString(c.getColumnIndexOrThrow(Audio.Media.DATA));
            c.close();
        }

        if (!IpMessageUtils.isAudio(mDstPath)) {
            Toast.makeText(this,
                            IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_invalid_file_type),
                            Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!IpMessageUtils.getServiceManager(this).isFeatureSupported(FeatureId.FILE_TRANSACTION)
                && !IpMessageUtils.isFileStatusOk(this, mDstPath)) {
            MmsLog.e(IPMSG_TAG, "getAudio(): choose audio failed, invalid file");
            return false;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, uri);
            String dur = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (dur != null) {
                mDuration = Integer.parseInt(dur);
                mDuration = mDuration / 1000 == 0 ? 1 : mDuration / 1000;
            }
        } catch (Exception ex) {
            MmsLog.e(IPMSG_TAG, "getAudio(): MediaMetadataRetriever failed to get duration for " + uri.getPath(), ex);
            return false;
        } finally {
            retriever.release();
        }
        return true;
    }

    private void sendImage(int requestCode) {
        IpImageMessage msg = new IpImageMessage();
        if (requestCode == REQUEST_CODE_IPMSG_DRAW_SKETCH) {
            msg.setType(IpMessageType.SKETCH);
        } else {
            msg.setType(IpMessageType.PICTURE);
        }
        msg.setPath(mDstPath);
        mIpMessageForSend = msg;
        MmsLog.d(IPMSG_TAG, "sendImage(): start send message.");
        resetSelectSimId();
        if (mIsCaptionOn && mIsImageCaptionOn && msg.getType() == IpMessageType.PICTURE) {
            mWorkingMessage.asyncDeleteDraftSmsMessage(mConversation);
            if (mTextEditor.getText().toString().length() > IpMessageConfig.CAPTION_MAX_LENGTH) {
                mIpMessageDraft = msg;
                mIpMessageForSend = null;
                convertIpMessageToMmsOrSms(true);
                updateSendButtonState();
                return;
            }
            mIsEditingCaption = true;
            saveIpMessageForAWhile(msg);
            updateSendButtonState();
            mSendButtonCanResponse = true;
        } else {
            sendMessageForIpMsg(msg, "sendImage(): send pic msg failed", false, false);
        }
    }

    private void sendLocationMsg(Intent intent) {
        IpLocationMessage msg = (IpLocationMessage) IpMessageUtils.getMessageManager(this).getIpMessageFromIntent(intent);
        msg.setType(IpMessageType.LOCATION);
        resetSelectSimId();
        mIpMessageForSend = msg;
        sendMessageForIpMsg(mIpMessageForSend, "sendLocationMsg(): send location msg failed", false, false);
    }

    public void getCalendar(Context context, String calendar) {
        Uri calendarUri = Uri.parse(calendar);
        InputStream is = null;
        OutputStream os = null;
        Cursor cursor = getContentResolver().query(calendarUri, null, null, null, null);
        if (null != cursor) {
            if (0 == cursor.getCount()) {
                MmsLog.e(IPMSG_TAG, "getCalendar(): take calendar cursor getcount is 0");
            } else {
                cursor.moveToFirst();
                mCalendarSummary = cursor.getString(0);
                if (mCalendarSummary != null) {
                    int sub = mCalendarSummary.lastIndexOf(".");
                    mCalendarSummary = mCalendarSummary.substring(0, sub);
                }
            }
            cursor.close();

            String fileName = System.currentTimeMillis() + ".vcs";
            mDstPath = MmsConfig.getVcalendarTempPath(this) + File.separator + fileName;

            File file = new File(mDstPath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.delete();
            try {
                if (!file.createNewFile()) {
                    return;
                }
            } catch (IOException e) {
                MmsLog.e(IPMSG_TAG, "getCalendar()", e);
                return;
            }
            try {
                is = context.getContentResolver().openInputStream(calendarUri);
                os = new BufferedOutputStream(new FileOutputStream(file));
            } catch (FileNotFoundException e) {
                MmsLog.e(IPMSG_TAG, "getCalendar()", e);
            }
            byte[] buffer = new byte[256];
            try {
                for (int len = 0; (len = is.read(buffer)) != -1;) {
                    os.write(buffer, 0, len);
                }
                is.close();
                os.close();
            } catch (IOException e) {
                MmsLog.e(IPMSG_TAG, "getCalendar()", e);
            }
        }
    }

    private void resetSelectSimId() {
        MmsLog.d(IPMSG_TAG, "resetSelectSimId()");
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT && mSimInfoList.size() > 1) {
            long defaultSimId = Settings.System.getLong(getContentResolver(),
                Settings.System.SMS_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
            if (mSelectedSimId != (int) defaultSimId) {
                mSelectedSimId = 0;
            }
        }
    }

    private String mCurrentNumberString = "";
    private String getCurrentNumberString() {
        if (isRecipientsEditorVisible()) {
            return TextUtils.join(",", mRecipientsEditor.getNumbers());
        }
        if (TextUtils.isEmpty(mCurrentNumberString)) {
            mCurrentNumberString = TextUtils.join(",", mConversation.getRecipients().getNumbers());
        }
        return mCurrentNumberString;
    }

    private String getNameViaContactId(long contactId) {
        if (contactId <= 0) {
            MmsLog.w(IPMSG_TAG, "getNameViaContactId(): contactId is invalid!");
            return null;
        }

        String displayName = "";

        Cursor cursor = mContentResolver.query(Contacts.CONTENT_URI, new String[] { Contacts.DISPLAY_NAME },
                Contacts._ID + "=?", new String[] { String.valueOf(contactId) }, null);
        if (cursor != null && cursor.moveToFirst()) {
            displayName = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME));
        }
        if (cursor != null) {
            cursor.close();
        }

        return displayName == null ? "" : displayName;
    }

    private void onIpMsgOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_INVITE_FRIENDS_TO_CHAT:
            Intent intent = new Intent(RemoteActivities.CONTACT);
            intent.putExtra(RemoteActivities.KEY_REQUEST_CODE, REQUEST_CODE_INVITE_FRIENDS_TO_CHAT);
            intent.putExtra(RemoteActivities.KEY_TYPE, SelectContactType.IP_MESSAGE_USER);
            intent.putExtra(RemoteActivities.KEY_ARRAY, mConversation.getRecipients().getNumbers());
            IpMessageUtils.startRemoteActivityForResult(this, intent);
             break;
        case MENU_INVITE_FRIENDS_TO_IPMSG:
            String contentText = mTextEditor.getText().toString();
            if (TextUtils.isEmpty(contentText)) {
                mTextEditor.setText(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                                .getSingleString(IpMessageConsts.string.ipmsg_invite_friends_content));
            } else {
                IpMessageUtils.showInviteIpMsgConfirmDialog(this, new InviteFriendsToIpMsgListener());
            }
            break;
        case MENU_SELECT_MESSAGE:
            Intent intentSelectMessage = new Intent(this, MultiDeleteActivity.class);
            intentSelectMessage.putExtra("thread_id", mConversation.getThreadId());
            startActivityForResult(intentSelectMessage, REQUEST_CODE_FOR_MULTIDELETE);
            break;
        case MENU_MARK_AS_SPAM:
            final Conversation conversationForMarkSpam = mConversation;
            final boolean isSpamFromMark = mConversation.isSpam();
            mConversation.setSpam(true);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String numbers = TextUtils.join(",", conversationForMarkSpam.getRecipients().getNumbers());
                    int contactId = IpMessageUtils.getContactManager(ComposeMessageActivity.this)
                                                    .getContactIdByNumber(numbers);
                    int[] contactIds = {contactId};
                    if (!IpMessageUtils.getContactManager(ComposeMessageActivity.this)
                            .addContactToSpamList(contactIds)) {
                        MmsLog.w(IPMSG_TAG, "onIpMsgOptionsItemSelected(): Mark as spam failed!");
                        mConversation.setSpam(isSpamFromMark);
                    }
                }
            }).start();
            break;
        case MENU_REMOVE_SPAM:
            final Conversation conversationForRemoveSpam = mConversation;
            final boolean isSpamFromRemove = mConversation.isSpam();
            mConversation.setSpam(false);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String numbers = TextUtils.join(",", conversationForRemoveSpam.getRecipients().getNumbers());
                    int contactId = IpMessageUtils.getContactManager(ComposeMessageActivity.this)
                        .getContactIdByNumber(numbers);
                    int[] contactIds = {contactId};
                    if (!IpMessageUtils.getContactManager(ComposeMessageActivity.this)
                            .deleteContactFromSpamList(contactIds)) {
                        MmsLog.w(IPMSG_TAG, "onIpMsgOptionsItemSelected(): Remove spam failed!");
                        mConversation.setSpam(isSpamFromRemove);
                    }
                }
            }).start();
            break;
        case MENU_ADD_SHORTCUT:
            HashSet<Long> threadIds = new HashSet<Long>();
            threadIds.add(mConversation.getThreadId());
            MessageUtils.addShortcutToLauncher(ComposeMessageActivity.this, threadIds);
            break;
        case MENU_VIEW_ALL_MEDIA:
            Intent intentViewAllMedia = new Intent(RemoteActivities.ALL_MEDIA);
            intentViewAllMedia.putExtra(RemoteActivities.KEY_THREAD_ID, mConversation.getThreadId());
            IpMessageUtils.startRemoteActivity(ComposeMessageActivity.this, intentViewAllMedia);
            break;
        case MENU_VIEW_ALL_LOCATION:
            Intent intentViewAllLocation = new Intent(RemoteActivities.ALL_LOCATION);
            intentViewAllLocation.putExtra(RemoteActivities.KEY_THREAD_ID, mConversation.getThreadId());
            IpMessageUtils.startRemoteActivity(ComposeMessageActivity.this, intentViewAllLocation);
            break;
        case MENU_CHAT_SETTING:
            long chatThreadId = -1l;
            if (!isRecipientsEditorVisible()) {
                chatThreadId = mConversation.getThreadId();
            }
            if (chatThreadId != -1l) {
                Intent i = new Intent(ComposeMessageActivity.this, ChatPreferenceActivity.class);
                i.putExtra("chatThreadId", chatThreadId);
                startActivity(i);
            }
            break;
        default:
            break;
        }
    }

    private void updateTextEditorHint() {
        if (mIsMessageDefaultSimIpServiceEnabled && isNetworkConnected(getApplicationContext())) {
            if (null != mIpMessageDraft && mIsCaptionOn && (
                    (mIpMessageDraft.getType() == IpMessageType.PICTURE && mIsImageCaptionOn) ||
                    (mIpMessageDraft.getType() == IpMessageType.VOICE && mIsAudioCaptionOn) ||
                    (mIpMessageDraft.getType() == IpMessageType.VIDEO && mIsVideoCaptionOn))) {
                mTextEditor.setHint(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                                .getSingleString(IpMessageConsts.string.ipmsg_caption_hint));
                if (mIsEditingCaption) {
                    mTextEditor.setFilters(new InputFilter[] {
                        new TextLengthFilter(IpMessageConfig.CAPTION_MAX_LENGTH)});
                    updateCounter(mWorkingMessage.getText(), 0, 0, 0);
                }
                return;
            } else if (null != mIpMessageDraft || (!mWorkingMessage.requiresMms() && isCurrentRecipientIpMessageUser())) {
                mTextEditor.setHint(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                                .getSingleString(IpMessageConsts.string.ipmsg_hint));
                mTextEditor.setFilters(new InputFilter[] {
                    new TextLengthFilter(MmsConfig.getMaxTextLimit())});
                updateCounter(mWorkingMessage.getText(), 0, 0, 0);
                return;
            }
        }
        mTextEditor.setHint(R.string.ipmsg_sms_hint);
        mTextEditor.setFilters(new InputFilter[] {
            new TextLengthFilter(MmsConfig.getMaxTextLimit())});
        updateCounter(mWorkingMessage.getText(), 0, 0, 0);
    }

    private void saveIpMessageForAWhile(IpMessage ipMessage) {
        MmsLog.d(IPMSG_TAG, "saveIpMessageForAWhile(): mIpMessageDraft = " + mIpMessageDraft
            + ", mIpMessageForSend = " + mIpMessageForSend);
        if (mIpMessageDraft != null && mIpMessageForSend != null) {
            showReplaceAttachDialog();
            return;
        }
        mIpMessageDraft = ipMessage;
        mIpMessageForSend = null;
        if (mWorkingMessage.requiresMms()) {
            convertIpMessageToMmsOrSms(true);
            updateSendButtonState();
            return;
        }
        switch (mIpMessageDraft.getType()) {
        case IpMessageType.TEXT:
            IpTextMessage textMessage = (IpTextMessage) mIpMessageDraft;
            mTextEditor.setText(textMessage.getBody());
            break;
        case IpMessageType.PICTURE:
            IpImageMessage imageMessage = (IpImageMessage) mIpMessageDraft;
            Bitmap imageBitmap = BitmapFactory.decodeFile(imageMessage.getPath());
            createThumbnailForIpMessage(imageBitmap, 0, null, null);
            break;
        case IpMessageType.VOICE:
            createThumbnailForIpMessage(null, R.drawable.ic_launcher_record_audio, null, null);
            break;
        case IpMessageType.VCARD:
            createThumbnailForIpMessage(null, R.drawable.ic_vcard_attach, null, null);
            break;
        case IpMessageType.LOCATION:
            createThumbnailForIpMessage(null, R.drawable.default_map_small, null, null);
            break;
        case IpMessageType.SKETCH:
            IpImageMessage sketchMessage = (IpImageMessage) mIpMessageDraft;
            Bitmap sketchBitmap = BitmapFactory.decodeFile(sketchMessage.getPath());
            createThumbnailForIpMessage(sketchBitmap, 0, null, null);
            break;
        case IpMessageType.VIDEO:
            IpVideoMessage videoMessage = (IpVideoMessage) mIpMessageDraft;
            Bitmap videoBitmap = ThumbnailUtils.createVideoThumbnail(videoMessage.getPath(), Thumbnails.MICRO_KIND);
            createThumbnailForIpMessage(videoBitmap, 0, null, null);
            break;
        case IpMessageType.CALENDAR:
            createThumbnailForIpMessage(null, R.drawable.ic_vcalendar_attach, null, null);
            break;
        case IpMessageType.UNKNOWN_FILE:
        case IpMessageType.COUNT:
            MmsLog.e(IPMSG_TAG, "saveIpMessageForAWhile(): Unknown IP message type. type = " + mIpMessageDraft.getType());
            break;
        case IpMessageType.GROUP_CREATE_CFG:
        case IpMessageType.GROUP_ADD_CFG:
        case IpMessageType.GROUP_QUIT_CFG:
            /// M: group chat type
            MmsLog.e(IPMSG_TAG, "saveIpMessageForAWhile(): Group IP message type. type = " + mIpMessageDraft.getType());
            break;
        default:
            MmsLog.e(IPMSG_TAG, "saveIpMessageForAWhile(): Error IP message type. type = " + mIpMessageDraft.getType());
            break;
        }
        mJustSendMsgViaCommonMsgThisTime = false;
        updateTextEditorHint();
    }

    private void createThumbnailForIpMessage(Bitmap bitmap, int resId, Drawable drawable, Uri uri) {
        mIpMessageThumbnail.setVisibility(View.VISIBLE);
        if (bitmap != null) {
            mIpMessageThumbnail.setImageBitmap(bitmap);
        } else if (resId > 0) {
            mIpMessageThumbnail.setImageResource(resId);
        } else if (drawable != null) {
            mIpMessageThumbnail.setImageDrawable(drawable);
        } else if (uri != null) {
            mIpMessageThumbnail.setImageURI(uri);
        }
        return;
    }

    private String getOnlineDividerString(int currentRecipientStatus) {
        MmsLog.d(TAG_DIVIDER, "compose.getOnlineDividerString(): currentRecipientStatus = " + currentRecipientStatus);
        if (MmsConfig.isActivated(this) && mIsIpMessageRecipients) {
            switch (currentRecipientStatus) {
            case ContactStatus.OFFLINE:
                MmsLog.d(TAG_DIVIDER, "compose.getOnlineDividerString(): OFFLINE");
                int time = IpMessageUtils.getContactManager(this)
                        .getOnlineTimeByNumber(mConversation.getRecipients().get(0).getNumber());
                String onlineTimeString = time > 0 ?
                    String.format(
                            IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_divider_offline),
                            formatLastOnlineTime(time))
                    : IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_divider_never_online);
                MmsLog.d(TAG_DIVIDER, "compose.getOnlineDividerString(): OFFLINE, time = " + time +
                        ", onlineTimeString = " + onlineTimeString);
                return onlineTimeString;
            case ContactStatus.ONLINE:
            case ContactStatus.TYPING:
            case ContactStatus.RECORDING:
            case ContactStatus.SKETCHING:
                return String.format(
                        IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_divider_online),
                        mConversation.getRecipients().get(0).getName());
            case ContactStatus.STATUSCOUNT:
                MmsLog.d(TAG_DIVIDER, "compose.getOnlineDividerString(): STATUSCOUNT");
                break;
            default:
                MmsLog.w(TAG_DIVIDER, "compose.getOnlineDividerString(): unknown user status!");
                break;
            }
        }
        return "";
    }

    private String formatLastOnlineTime(int time) {
        return MessageUtils.formatTimeStampString(this, (long) (time * 1000L), true);
    }

    private void checkIpMessageBeforeSendMessage(boolean bCheckEcmMode) {
        mIsMessageDefaultSimIpServiceEnabled = EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT ?
            MmsConfig.isServiceEnabled(ComposeMessageActivity.this, mSelectedSimId)
                : MmsConfig.isServiceEnabled(ComposeMessageActivity.this);
        mIsIpMessageRecipients = isCurrentRecipientIpMessageUser();
        MmsLog.d(IPMSG_TAG, "checkIpMessageBeforeSendMessage(): isServiceEnable = " +
                                mIsMessageDefaultSimIpServiceEnabled +
                                ", mIsIpMessageRecipients = " + mIsIpMessageRecipients +
                                ", mIpMessageDraft is null ?= " + (null == mIpMessageDraft) +
                                ", mIpMessageForSend is null ?= " + (null == mIpMessageForSend));
        if (mIpMessageForSend != null) {
            sendMessageForIpMsg(mIpMessageForSend, "send fail from new ip message", false, true);
            return;
        }

        if (null != mIpMessageDraft) {
            // Has IP message draft
            if (!mIsMessageDefaultSimIpServiceEnabled || !isNetworkConnected(getApplicationContext())) {
                /// M: disabled service
                if (mIpMessageDraft.getType() == IpMessageType.TEXT) {
                    showIpMessageConvertToMmsOrSmsDialog(SMS_CONVERT, SERVICE_IS_NOT_ENABLED);
                } else {
                    showIpMessageConvertToMmsOrSmsDialog(MMS_CONVERT, SERVICE_IS_NOT_ENABLED);
                }
                return;
            }
            if (!mIsIpMessageRecipients) {
                /// M: non-IP message User
                if (mIpMessageDraft.getType() == IpMessageType.TEXT) {
                    showIpMessageConvertToMmsOrSmsDialog(SMS_CONVERT, RECIPIENTS_ARE_NOT_IP_MESSAGE_USER);
                } else {
                    showIpMessageConvertToMmsOrSmsDialog(MMS_CONVERT, RECIPIENTS_ARE_NOT_IP_MESSAGE_USER);
                }
                return;
            }

            if (mIsMessageDefaultSimIpServiceEnabled && isNetworkConnected(getApplicationContext()) && mIsCaptionOn) {
                String captionText = mTextEditor.getText().toString();
                if (!TextUtils.isEmpty(captionText)) {
                    if (captionText.length() > IpMessageConfig.CAPTION_MAX_LENGTH) {
                        captionText = captionText.substring(0, IpMessageConfig.CAPTION_MAX_LENGTH);
                    }
                    switch (mIpMessageDraft.getType()) {
                    case IpMessageType.PICTURE:
                        if (mIsImageCaptionOn) {
                            MmsLog.e(IPMSG_TAG, "checkIpMessageBeforeSendMessage(): Have caption. type = PICTURE");
                            ((IpImageMessage) mIpMessageDraft).setCaption(captionText);
                        }
                        break;
                    case IpMessageType.VOICE:
                        if (mIsAudioCaptionOn) {
                            MmsLog.e(IPMSG_TAG, "checkIpMessageBeforeSendMessage(): Have caption. type = VOICE");
                            ((IpVoiceMessage) mIpMessageDraft).setCaption(captionText);
                        }
                        break;
                    case IpMessageType.VIDEO:
                        if (mIsVideoCaptionOn) {
                            MmsLog.e(IPMSG_TAG, "checkIpMessageBeforeSendMessage(): Have caption. type = VIDEO");
                            ((IpVideoMessage) mIpMessageDraft).setCaption(captionText);
                        }
                        break;
                    default:
                        MmsLog.e(IPMSG_TAG, "checkIpMessageBeforeSendMessage(): No caption. type = " +
                                            mIpMessageDraft.getType());
                        break;
                    }
                }
            }

            if (sendMessageForIpMsg(mIpMessageDraft, "send fail from draft ip message", true, true)) {
                mIpMessageDraftId = 0;
                clearIpMessageDraft();
            }
            return;
        } else {
            // No IP message draft
            if (!mJustSendMsgViaCommonMsgThisTime && mIsMessageDefaultSimIpServiceEnabled
                    && isNetworkConnected(getApplicationContext()) && mIsIpMessageRecipients) {
                boolean isSmsCanConvertToIpmessage = !mWorkingMessage.requiresMms();
                boolean isMmsCanConvertToIpmessage = canMmsConvertToIpMessage();
                if (isSmsCanConvertToIpmessage) {
                    /// M: show convert to ipmessage dialog
//                    showMmsOrSmsConvertToIpMessageDialog(SMS_CONVERT, bCheckEcmMode);

                    /// M: send IP text message
                    sendIpTextMessage();
                    return;
                }
                if (isMmsCanConvertToIpmessage) {
                    showMmsOrSmsConvertToIpMessageDialog(MMS_CONVERT, bCheckEcmMode);
                    return;
                }
            }
            sendMessage(bCheckEcmMode);
        }
    }

    private void showIpMessageConvertToMmsOrSmsDialog(int mode, int convertReason) {
        String message = "";
        if (convertReason == SERVICE_IS_NOT_ENABLED) {
            message = mode == SMS_CONVERT ?
                IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                            .getSingleString(IpMessageConsts.string.ipmsg_convert_to_sms_for_service)
                : IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                            .getSingleString(IpMessageConsts.string.ipmsg_convert_to_mms_for_service);
        } else if (convertReason == RECIPIENTS_ARE_NOT_IP_MESSAGE_USER) {
            message = mode == SMS_CONVERT ?
                IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                            .getSingleString(IpMessageConsts.string.ipmsg_convert_to_sms_for_recipients)
                : IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                .getSingleString(IpMessageConsts.string.ipmsg_convert_to_mms_for_recipients);
        }
        new AlertDialog.Builder(this)
            .setTitle(mode == SMS_CONVERT ?
                        IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_convert_to_sms)
                        : IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_convert_to_mms))
            .setMessage(message)
            .setPositiveButton(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_continue),
            new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mIpMessageDraft != null) {
                        convertIpMessageToMmsOrSms(true);
                    }
                    if (mIpMessageForSend != null) {
                        if (mIpMessageForSend.getType() == IpMessageType.TEXT) {
                            MmsLog.e(IPMSG_TAG, "convertIpMessageToMmsOrSms(): convert emoticon IP message to SMS.");
                            insertEmoticon(((IpTextMessage) mIpMessageForSend).getBody());
                            if (mIpMessageForSend.getSimId() > 0) {
                                mSelectedSimId = mIpMessageForSend.getSimId();
                            }
                            mIpMessageForSend = null;
                            checkConditionsAndSendMessage(true);
                        } else {
                            mIpMessageDraft = mIpMessageForSend;
                            mIpMessageForSend = null;
                            convertIpMessageToMmsOrSms(true);
                        }
                    }
                    dialog.dismiss();
                }
            })
            .setNegativeButton(android.R.string.cancel,
                    new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MmsLog.d(IPMSG_TAG, "showIpMessageConvertToMmsOrSmsDialog(): cancel.");
                    if (mIpMessageForSend != null) {
                        mIpMessageForSend = null;
                    } else {
                        saveIpMessageForAWhile(mIpMessageDraft);
                    }
                    mSelectedSimId = 0;
                    updateSendButtonState();
                    mSendButtonCanResponse = true;
                    dialog.dismiss();
                }
            })
            .setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        updateSendButtonState();
                    }
                    return false;
                }
            })
            .show();
    }

    /**
     * M: for checking which type IpMessage can be converted to.
     * @param ipMessage
     * @return
     */
    private int canConvertIpMessageToMessage(IpMessage ipMessage) {
        if (ipMessage == null) {
            return MESSAGETYPE_UNSUPPORT;
        }
        switch (ipMessage.getType()) {
            case IpMessageType.TEXT:
                return MESSAGETYPE_TEXT;
            case IpMessageType.PICTURE:
            case IpMessageType.VOICE:
            case IpMessageType.VCARD:
            case IpMessageType.LOCATION:
            case IpMessageType.SKETCH:
            case IpMessageType.VIDEO:
                return MESSAGETYPE_MMS;
            case IpMessageType.UNKNOWN_FILE:
            case IpMessageType.COUNT:
            case IpMessageType.GROUP_CREATE_CFG:
            case IpMessageType.GROUP_ADD_CFG:
            case IpMessageType.GROUP_QUIT_CFG:
                return MESSAGETYPE_UNSUPPORT;
            default:
                return MESSAGETYPE_UNSUPPORT;
        }
    }

    private boolean convertIpMessageToMmsOrSms(boolean isAppend) {
        MmsLog.d(IPMSG_TAG, "convertIpMessageToMmsOrSms(): IP message type = " + mIpMessageDraft.getType());

        switch (mIpMessageDraft.getType()) {
        case IpMessageType.TEXT:
            MmsLog.d(IPMSG_TAG, "convertIpMessageToMmsOrSms(): convert to SMS.");
            IpTextMessage textMessage = (IpTextMessage) mIpMessageDraft;
            mWorkingMessage.setText(textMessage.getBody());
            break;
        case IpMessageType.PICTURE:
            MmsLog.d(IPMSG_TAG, "convertIpMessageToMmsOrSms(): convert to MMS from image.");
            IpImageMessage imageMessage = (IpImageMessage) mIpMessageDraft;
            MmsLog.d(IPMSG_TAG, "convertIpMessageToMmsOrSms(): imagePath = " + imageMessage.getPath());
            if (!TextUtils.isEmpty(imageMessage.getCaption())) {
                mWorkingMessage.setText(imageMessage.getCaption());
                if (mTextEditor != null && mTextEditor.getVisibility() == View.VISIBLE) {
                    mTextEditor.setText(imageMessage.getCaption());
                }
            }
            File imageFile = new File(imageMessage.getPath());
            Uri imageUri = Uri.fromFile(imageFile);
//            addImageAsync(imageUri, EncapsulatedContentType.IMAGE_JPG, true);
            addImage(EncapsulatedContentType.IMAGE_JPG, imageUri, isAppend);
            saveAsMms(true);
            break;
        case IpMessageType.VOICE:
            MmsLog.d(IPMSG_TAG, "convertIpMessageToMmsOrSms(): convert to MMS from voice.");
            IpVoiceMessage voiceMessage = (IpVoiceMessage) mIpMessageDraft;
            if (!TextUtils.isEmpty(voiceMessage.getCaption())) {
                mWorkingMessage.setText(voiceMessage.getCaption());
                if (mTextEditor != null && mTextEditor.getVisibility() == View.VISIBLE) {
                    mTextEditor.setText(voiceMessage.getCaption());
                }
            }
            File voiceFile = new File(voiceMessage.getPath());
            Uri voiceUri = Uri.fromFile(voiceFile);
            addAudio(voiceUri, isAppend);
            saveAsMms(true);
            break;
        case IpMessageType.VCARD:
            MmsLog.d(IPMSG_TAG, "convertIpMessageToMmsOrSms(): convert to MMS from vCard.");
            IpVCardMessage vCardMessage = (IpVCardMessage) mIpMessageDraft;
            File vCardFile = new File(vCardMessage.getPath());
            Uri vCardUri = Uri.fromFile(vCardFile);
            VCardAttachment va = new VCardAttachment(ComposeMessageActivity.this);
            String fileName = va.getVCardFileNameByUri(vCardUri);
            setFileAttachment(fileName, WorkingMessage.VCARD, false);
            saveAsMms(true);
            break;
        case IpMessageType.LOCATION:
            MmsLog.d(IPMSG_TAG, "convertIpMessageToMmsOrSms(): convert to MMS from location.");
            IpLocationMessage locationMessage = (IpLocationMessage) mIpMessageDraft;
            String locationImgPath = locationMessage.getPath();
            if (!TextUtils.isEmpty(locationImgPath)) {
                File locationFile = new File(locationImgPath);
                Uri locationUri = Uri.fromFile(locationFile);
                addImage(EncapsulatedContentType.IMAGE_JPG, locationUri, true);
                saveAsMms(true);
            } else {
                String tempFileName = "default_map_small.png";
                boolean convertResult = MessageUtils.createFileForResource(this, tempFileName,
                    R.drawable.default_map_small);
                if (convertResult) {
                    File locationTempFile = this.getFileStreamPath(tempFileName);
                    Uri locationUri = Uri.fromFile(locationTempFile);
                    addImage(EncapsulatedContentType.IMAGE_JPG, locationUri, true);
                    saveAsMms(true);
                }
            }
            mTextEditor.setText(locationMessage.getAddress());
            break;
        case IpMessageType.SKETCH:
            MmsLog.d(IPMSG_TAG, "convertIpMessageToMmsOrSms(): convert to MMS from sketch.");
            IpImageMessage sketchMessage = (IpImageMessage) mIpMessageDraft;
            File sketchFile = new File(sketchMessage.getPath());
            Uri sketchUri = Uri.fromFile(sketchFile);
            addImage(EncapsulatedContentType.IMAGE_JPG, sketchUri, true);
            saveAsMms(true);
            break;
        case IpMessageType.VIDEO:
            MmsLog.d(IPMSG_TAG, "convertIpMessageToMmsOrSms(): convert to MMS from video.");
            IpVideoMessage videoMessage = (IpVideoMessage) mIpMessageDraft;
            if (!TextUtils.isEmpty(videoMessage.getCaption())) {
                mWorkingMessage.setText(videoMessage.getCaption());
                if (mTextEditor != null && mTextEditor.getVisibility() == View.VISIBLE) {
                    mTextEditor.setText(videoMessage.getCaption());
                }
            }
            File videoFile = new File(videoMessage.getPath());
            Uri videoUri = Uri.fromFile(videoFile);
            addVideo(videoUri, isAppend);
            saveAsMms(true);
            break;
        case IpMessageType.CALENDAR:
            MmsLog.d(IPMSG_TAG, "convertIpMessageToMmsOrSms(): convert to MMS from vCalendar.");
            IpVCalendarMessage vCalendarMessage = (IpVCalendarMessage) mIpMessageDraft;
            File vCalendarFile = new File(vCalendarMessage.getPath());
            Uri vCalendarUri = Uri.fromFile(vCalendarFile);
            attachVCalendar(vCalendarUri);
            saveAsMms(true);
            break;
        case IpMessageType.UNKNOWN_FILE:
        case IpMessageType.COUNT:
            MmsLog.w(IPMSG_TAG, "convertIpMessageToMmsOrSms(): Unknown IP message type. type = " +
                                mIpMessageDraft.getType());
            return false;
        case IpMessageType.GROUP_CREATE_CFG:
        case IpMessageType.GROUP_ADD_CFG:
        case IpMessageType.GROUP_QUIT_CFG:
            /// M: group chat type
            MmsLog.w(IPMSG_TAG, "convertIpMessageToMmsOrSms(): Group IP message type. type = " +
                                mIpMessageDraft.getType());
            return false;
        default:
            MmsLog.w(IPMSG_TAG, "convertIpMessageToMmsOrSms(): Error IP message type. type = " +
                                mIpMessageDraft.getType());
            return false;
        }
        if (mIpMessageDraft.getSimId() > 0) {
            mSelectedSimId = mIpMessageDraft.getSimId();
        }
        IpMessageUtils.deleteIpMessageDraft(this, mConversation, mWorkingMessage);
        mIpMessageDraftId = 0;
        clearIpMessageDraft();
//        checkConditionsAndSendMessage(true);
        return true;
    }

    private void showMmsOrSmsConvertToIpMessageDialog(int mode, final boolean bCheckEcmMode) {
        new AlertDialog.Builder(this)
            .setTitle(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_convert_to_ipmsg))
            .setMessage(mode == SMS_CONVERT ?
                IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                            .getSingleString(IpMessageConsts.string.ipmsg_sms_convert_to_ipmsg)
                : IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                            .getSingleString(IpMessageConsts.string.ipmsg_mms_convert_to_ipmsg))
            .setPositiveButton(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_switch),
                     new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    convertMmsOrSmsToIpMessage();
                    dialog.dismiss();
                }
            })
//                    .setNeutralButton(R.string.delete, new DeleteButtonListener())
            .setNegativeButton(mode == SMS_CONVERT ?
                            IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_keep_sms)
                            : IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_keep_mms),
                    new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    sendMessage(bCheckEcmMode);
                    dialog.dismiss();
                }
            })
            .setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        updateSendButtonState();
                    }
                    return false;
                }
            })
            .show();
    }

    private void convertMmsOrSmsToIpMessage() {
        MmsLog.w(IPMSG_TAG, "convertMmsOrSmsToIpMessage()");
        if (!mWorkingMessage.isWorthSaving()) {
            MmsLog.w(IPMSG_TAG, "convertMmsOrSmsToIpMessage(): No content for convert!");
            return;
        }
        if (mWorkingMessage.requiresMms()) {
            SlideshowModel slideshowModel = mWorkingMessage.getSlideshow();
            if (slideshowModel.size() == 1 &&
                    !(slideshowModel.get(0).hasAudio() && slideshowModel.get(0).hasImage()) &&
                    (slideshowModel.get(0).hasAudio() || slideshowModel.get(0).hasImage()
                            || slideshowModel.get(0).hasVideo())) {
                MmsLog.w(IPMSG_TAG, "convertMmsOrSmsToIpMessage(): Convert slide media!");
                if (mSubjectTextEditor != null && mSubjectTextEditor.getVisibility() == View.VISIBLE) {
                    showSubjectEditor(false);
                }
                convertSlideToIpMessage(slideshowModel.get(0));
                mWorkingMessage.removeAttachment(false);
                drawBottomPanel();
                updateSendButtonState();
            } else if (slideshowModel.sizeOfFilesAttach() == 1) {
                MmsLog.w(IPMSG_TAG, "convertMmsOrSmsToIpMessage(): Convert file attachment!");
                if (convertAttachmentToIpMessage(slideshowModel.getAttachFiles().get(0))) {
                    if (mSubjectTextEditor != null && mSubjectTextEditor.getVisibility() == View.VISIBLE) {
                        showSubjectEditor(false);
                    }
                    mWorkingMessage.removeAttachment(false);
                    drawBottomPanel();
                    updateSendButtonState();
                } else {
                    MmsLog.w(IPMSG_TAG, "");
                }
            } else if (slideshowModel.size() > 1) {
                MmsLog.w(IPMSG_TAG, "convertMmsOrSmsToIpMessage(): More than 1 slide for convert!");
            } else {
                MmsLog.w(IPMSG_TAG, "convertMmsOrSmsToIpMessage(): No slide! for convert");
            }
        } else {
            sendIpTextMessage();
            mTextEditor.setText("");
        }
    }

    private void convertSlideToIpMessage(SlideModel slideModel) {
        if (null == slideModel) {
            MmsLog.w(IPMSG_TAG, "convertSlideToIpMessage(): No content for convert! slide is null!");
            return;
        }
        MmsLog.w(IPMSG_TAG, "convertSlideToIpMessage(): start");
        if (slideModel.hasVideo()) {
            if (convertVideoToIpMessage(slideModel.getVideo())) {
                mIpMsgHandler.postDelayed(mSendVideo, 100);
            }
            return;
        }
        if (slideModel.hasAudio()) {
            if (convertAudioToIpMessage(slideModel.getAudio())) {
                mIpMsgHandler.postDelayed(mSendAudio, 100);
            }
            return;
        }
        if (slideModel.hasImage()) {
            if (convertImageToIpMessage(slideModel.getImage())) {
                mIpMsgHandler.postDelayed(mSendPic, 100);
            }
            return;
        }
        if (slideModel.hasText()) {
            // do nothings
            return;
        }
    }

    private boolean convertVideoToIpMessage(VideoModel videoModel) {
        if (null == videoModel) {
            MmsLog.w(IPMSG_TAG, "convertVideoToIpMessage(): video is null.");
            return false;
        }
        InputStream is = null;
        FileOutputStream os = null;
        try {
            MmsLog.d(IPMSG_TAG, "convertVideoToIpMessage(): contentType = " + videoModel.getContentType()
                    + ", mediaSize = " + videoModel.getMediaSize() + ", duration = " + videoModel.getDuration()
                    + ", src = " + videoModel.getSrc() + ", uri = " + videoModel.getUri());

            String fileName = "";
            if (videoModel.getSrc().lastIndexOf(".") > -1) {
                fileName = videoModel.getSrc();
            } else {
                fileName = videoModel.getSrc() + "." + MimeTypeMap.getSingleton()
                        .getExtensionFromMimeType(videoModel.getContentType());
            }

            mPhotoFilePath = MmsConfig.getVideoTempPath(this) + File.separator + fileName;
            mDstPath = mPhotoFilePath;
            mDuration = videoModel.getDuration();
            MmsLog.d(IPMSG_TAG, "convertVideoToIpMessage(): mDstPath = " + mDstPath);

            File file = new File(mDstPath);
            is = this.getContentResolver().openInputStream(videoModel.getUri());
            os = new FileOutputStream(file);
            byte[] buffer = new byte[2048];
            for (int len = 0; (len = is.read(buffer)) != -1; ) {
                os.write(buffer, 0, len);
            }
        } catch (FileNotFoundException e) {
            MmsLog.w(IPMSG_TAG, "convertVideoToIpMessage(): can not found this part file!", e);
            return false;
        } catch (IOException e) {
            MmsLog.w(IPMSG_TAG, "convertVideoToIpMessage(): IOException happened.", e);
            return false;
        } finally {
            try {
                if (null != is) {
                    is.close();
                }
                if (null != os) {
                    os.close();
                }
            } catch (IOException e) {
                MmsLog.w(IPMSG_TAG, "convertVideoToIpMessage(): IOException happened.2", e);
            }
        }
        return true;
    }

    private boolean convertAudioToIpMessage(AudioModel audioModel) {
        if (null == audioModel) {
            MmsLog.w(IPMSG_TAG, "convertAudioToIpMessage(): audio is null.");
            return false;
        }
        InputStream is = null;
        FileOutputStream os = null;
        try {
            MmsLog.d(IPMSG_TAG, "convertAudioToIpMessage(): contentType = " + audioModel.getContentType()
                    + ", mediaSize = " + audioModel.getMediaSize() + ", duration = " + audioModel.getDuration()
                    + ", src = " + audioModel.getSrc() + ", uri = " + audioModel.getUri());

            String fileName = "";
            if (audioModel.getSrc().lastIndexOf(".") > -1) {
                fileName = audioModel.getSrc();
            } else {
                fileName = audioModel.getSrc() + "." + MimeTypeMap.getSingleton()
                        .getExtensionFromMimeType(audioModel.getContentType());
            }

            mPhotoFilePath = MmsConfig.getAudioTempPath(this) + File.separator + fileName;
            mDstPath = mPhotoFilePath;
            int oneKiloMillisecond = 1000;
            mDuration = audioModel.getDuration() / oneKiloMillisecond;
            MmsLog.d(IPMSG_TAG, "convertAudioToIpMessage(): mDstPath = " + mDstPath);

            File file = new File(mDstPath);
            is = this.getContentResolver().openInputStream(audioModel.getUri());
            os = new FileOutputStream(file);
            byte[] buffer = new byte[2048];
            for (int len = 0; (len = is.read(buffer)) != -1; ) {
                os.write(buffer, 0, len);
            }
        } catch (FileNotFoundException e) {
            MmsLog.w(IPMSG_TAG, "convertAudioToIpMessage(): can not found this part file!", e);
            return false;
        } catch (IOException e) {
            MmsLog.w(IPMSG_TAG, "convertAudioToIpMessage(): IOException happened.", e);
            return false;
        } finally {
            try {
                if (null != is) {
                    is.close();
                }
                if (null != os) {
                    os.close();
                }
            } catch (IOException e) {
                MmsLog.w(IPMSG_TAG, "convertAudioToIpMessage(): IOException happened.2", e);
            }
        }
        return true;
    }

    private boolean convertImageToIpMessage(ImageModel imageModel) {
        if (null == imageModel) {
            MmsLog.w(IPMSG_TAG, "convertImageToIpMessage(): video is null.");
            return false;
        }
        InputStream is = null;
        FileOutputStream os = null;
        try {
            MmsLog.d(IPMSG_TAG, "convertImageToIpMessage(): contentType = " + imageModel.getContentType()
                    + ", mediaSize = " + imageModel.getMediaSize() + ", duration = " + imageModel.getDuration()
                    + ", src = " + imageModel.getSrc() + ", uri = " + imageModel.getUri());

            String fileName = "";
            if (imageModel.getSrc().lastIndexOf(".") > -1) {
                fileName = imageModel.getSrc();
            } else {
                fileName = imageModel.getSrc() + "." + MimeTypeMap.getSingleton()
                        .getExtensionFromMimeType(imageModel.getContentType());
            }

            mPhotoFilePath = MmsConfig.getPicTempPath(this) + File.separator + fileName;
            mDstPath = mPhotoFilePath;
            mDuration = imageModel.getDuration();
            MmsLog.d(IPMSG_TAG, "convertImageToIpMessage(): mDstPath = " + mDstPath);

            File file = new File(mDstPath);
            is = this.getContentResolver().openInputStream(imageModel.getUri());
            os = new FileOutputStream(file);
            byte[] buffer = new byte[2048];
            for (int len = 0; (len = is.read(buffer)) != -1; ) {
                os.write(buffer, 0, len);
            }
        } catch (FileNotFoundException e) {
            MmsLog.w(IPMSG_TAG, "convertImageToIpMessage(): can not found this part file!", e);
            return false;
        } catch (IOException e) {
            MmsLog.w(IPMSG_TAG, "convertImageToIpMessage(): IOException happened.", e);
            return false;
        } finally {
            try {
                if (null != is) {
                    is.close();
                }
                if (null != os) {
                    os.close();
                }
            } catch (IOException e) {
                MmsLog.w(IPMSG_TAG, "convertImageToIpMessage(): IOException happened.2", e);
            }
        }
        return true;
    }

    private boolean convertAttachmentToIpMessage(FileAttachmentModel fileAttachmentModel) {
        if (null == fileAttachmentModel) {
            MmsLog.w(IPMSG_TAG, "convertSlideToIpMessage(): No content for convert! fileAttachment is null!");
            return false;
        }

        if (fileAttachmentModel.getContentType().equals(EncapsulatedContentType.TEXT_VCARD)) {
            MmsLog.w(IPMSG_TAG, "convertSlideToIpMessage(): vCard");
            if (convertVCardToIpMessage((VCardModel) fileAttachmentModel)) {
                mIpMsgHandler.postDelayed(mSendVcard, 100);
                return true;
            } else {
                return false;
            }
        }

        if (fileAttachmentModel.getContentType().equals(EncapsulatedContentType.TEXT_VCALENDAR)) {
            MmsLog.w(IPMSG_TAG, "convertSlideToIpMessage(): vCalendar");
            if (convertVCalendarToIpMessage((VCalendarModel) fileAttachmentModel)) {
                mIpMsgHandler.postDelayed(mSendCalendar, 100);
                return true;
            } else {
                return false;
            }
        }
        MmsLog.w(IPMSG_TAG, "convertSlideToIpMessage(): Unsupport file attachment. content-type = "
            + fileAttachmentModel.getContentType());
        return false;
    }

    private boolean convertVCardToIpMessage(VCardModel vCardModel) {
        if (null == vCardModel) {
            MmsLog.w(IPMSG_TAG, "convertVCardToIpMessage(): vCard is null.");
            return false;
        }
        InputStream is = null;
        FileOutputStream os = null;
        try {
            MmsLog.d(IPMSG_TAG, "convertVCardToIpMessage(): contentType = " + vCardModel.getContentType()
                    + ", attachSize = " + vCardModel.getAttachSize()
                    + ", src = " + vCardModel.getSrc() + ", uri = " + vCardModel.getUri());

            String fileName = "";
            if (vCardModel.getSrc().lastIndexOf(".") > -1) {
                fileName = vCardModel.getSrc();
            } else {
                fileName = vCardModel.getSrc() + "." + MimeTypeMap.getSingleton()
                        .getExtensionFromMimeType(vCardModel.getContentType());
            }
            mIpMessageVcardName = fileName;
            mDstPath = MmsConfig.getVcardTempPath(this) + File.separator + fileName;
            MmsLog.d(IPMSG_TAG, "convertVCardToIpMessage(): mDstPath = " + mDstPath);

            File file = new File(mDstPath);
            is = this.getContentResolver().openInputStream(vCardModel.getUri());
            os = new FileOutputStream(file);
            byte[] buffer = new byte[2048];
            for (int len = 0; (len = is.read(buffer)) != -1; ) {
                os.write(buffer, 0, len);
            }
        } catch (FileNotFoundException e) {
            MmsLog.w(IPMSG_TAG, "convertVCardToIpMessage(): can not found this part file!", e);
            return false;
        } catch (IOException e) {
            MmsLog.w(IPMSG_TAG, "convertVCardToIpMessage(): IOException happened.", e);
            return false;
        } finally {
            try {
                if (null != is) {
                    is.close();
                }
                if (null != os) {
                    os.close();
                }
            } catch (IOException e) {
                MmsLog.w(IPMSG_TAG, "convertVCardToIpMessage(): IOException happened.2", e);
            }
        }
        return true;
    }

    private boolean convertVCalendarToIpMessage(VCalendarModel vCalendarModel) {
        if (null == vCalendarModel) {
            MmsLog.w(IPMSG_TAG, "convertVCalendarToIpMessage(): vCard is null.");
            return false;
        }
        InputStream is = null;
        FileOutputStream os = null;
        try {
            MmsLog.d(IPMSG_TAG, "convertVCalendarToIpMessage(): contentType = " + vCalendarModel.getContentType()
                    + ", attachSize = " + vCalendarModel.getAttachSize()
                    + ", src = " + vCalendarModel.getSrc() + ", uri = " + vCalendarModel.getUri());

            String fileName = "";
            if (vCalendarModel.getSrc().lastIndexOf(".") > -1) {
                fileName = vCalendarModel.getSrc();
            } else {
                fileName = vCalendarModel.getSrc() + "." + MimeTypeMap.getSingleton()
                        .getExtensionFromMimeType(vCalendarModel.getContentType());
            }
            mCalendarSummary = fileName;
            mDstPath = MmsConfig.getVcalendarTempPath(this) + File.separator + fileName;
            MmsLog.d(IPMSG_TAG, "convertVCalendarToIpMessage(): mDstPath = " + mDstPath);

            File file = new File(mDstPath);
            is = this.getContentResolver().openInputStream(vCalendarModel.getUri());
            os = new FileOutputStream(file);
            byte[] buffer = new byte[2048];
            for (int len = 0; (len = is.read(buffer)) != -1; ) {
                os.write(buffer, 0, len);
            }
        } catch (FileNotFoundException e) {
            MmsLog.w(IPMSG_TAG, "convertVCalendarToIpMessage(): can not found this part file!", e);
            return false;
        } catch (IOException e) {
            MmsLog.w(IPMSG_TAG, "convertVCalendarToIpMessage(): IOException happened.", e);
            return false;
        } finally {
            try {
                if (null != is) {
                    is.close();
                }
                if (null != os) {
                    os.close();
                }
            } catch (IOException e) {
                MmsLog.w(IPMSG_TAG, "convertVCalendarToIpMessage(): IOException happened.2", e);
            }
        }
        return true;
    }

    /** M:
     * whether the current recipient(s) are ipmessage user or not
     * @return
     */
    private boolean isCurrentRecipientIpMessageUser() {
        if (isRecipientsEditorVisible()) {
            List<String> numbers = mRecipientsEditor.getNumbers();
            if (numbers != null && numbers.size() > 0) {
                for (String number : numbers) {
                    if (!IpMessageUtils.getContactManager(this).isIpMessageNumber(number)) {
                        return false;
                    }
                }
                return true;
            }
        } else {
            for (String number : mConversation.getRecipients().getNumbers()) {
                if (!IpMessageUtils.getContactManager(this).isIpMessageNumber(number)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean isIpMessageRecipients(String number) {
        mIsIpMessageRecipients = IpMessageUtils.getContactManager(this).isIpMessageNumber(number);
        return mIsIpMessageRecipients;
    }

    @Override
    public void notificationsReceived(Intent intent) {
        MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "compose.notificationsReceived(): start, intent = " + intent);
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return;
        }
        switch (IpMessageUtils.getActionTypeByAction(action)) {
        case IpMessageUtils.IPMSG_IM_STATUS_ACTION:
            MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "notificationsReceived(): IM status, mChatModeNumber = "
                + mChatModeNumber);
            if (!mIsMessageDefaultSimIpServiceEnabled || !isNetworkConnected(getApplicationContext())
                    || isRecipientsEditorVisible() || mConversation.getRecipients().size() != 1
                    || !mIsIpMessageRecipients || TextUtils.isEmpty(mChatModeNumber)) {
                return;
            }
            String number = intent.getStringExtra(IpMessageConsts.NUMBER);
            MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "notificationsReceived(): number = " + number
                + ", mChatModeNumber = " + mChatModeNumber);
            if (!TextUtils.isEmpty(number) && isChatModeNumber(number)) {
                int status = IpMessageUtils.getContactManager(this).getStatusByNumber(mChatModeNumber);
                MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "notificationsReceived(): IM status. number = " + number
                    + ", status = " + status);
                switch (status) {
                case ContactStatus.TYPING:
                case ContactStatus.RECORDING:
                case ContactStatus.SKETCHING:
                case ContactStatus.STOP_TYPING:
                case ContactStatus.STOP_RECORDING:
                case ContactStatus.STOP_SKETCHING:
                    return;
                case ContactStatus.ONLINE:
                case ContactStatus.OFFLINE:
                    MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "notificationsReceived(): user online status changed, "
                        + "number = " + number + ", mChatModeNumber = " + mChatModeNumber);
                    mMsgListAdapter.setOnlineDividerString(getOnlineDividerString(IpMessageUtils.getContactManager(this)
                        .getStatusByNumber(mConversation.getRecipients().get(0).getNumber())));
                    mMsgListAdapter.updateOnlineDividerTime();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMsgListAdapter.notifyDataSetChanged();
                        }
                    });
                    return;
                case ContactStatus.STATUSCOUNT:
                default:
                    return;
                }
            }
            break;
        case IpMessageUtils.IPMSG_ERROR_ACTION:
        case IpMessageUtils.IPMSG_NEW_MESSAGE_ACTION:
        case IpMessageUtils.IPMSG_REFRESH_CONTACT_LIST_ACTION:
        case IpMessageUtils.IPMSG_REFRESH_GROUP_LIST_ACTION:
        case IpMessageUtils.IPMSG_SERCIVE_STATUS_ACTION:
        case IpMessageUtils.IPMSG_SAVE_HISTORY_ACTION:
        case IpMessageUtils.IPMSG_ACTIVATION_STATUS_ACTION:
            break;
        case IpMessageUtils.IPMSG_IP_MESSAGE_STATUS_ACTION:
            if (mMsgListAdapter != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMsgListAdapter.setClearCacheFlag(true);
                        mMsgListAdapter.notifyDataSetChanged();
                    }
                });
            }
            break;
        case IpMessageUtils.IPMSG_DOWNLOAD_ATTACH_STATUS_ACTION:
        case IpMessageUtils.IPMSG_SET_PROFILE_RESULT_ACTION:
        case IpMessageUtils.IPMSG_BACKUP_MSG_STATUS_ACTION:
        case IpMessageUtils.IPMSG_RESTORE_MSG_STATUS_ACTION:
        default:
            return;
        }
    }

    private boolean onIpMessageMenuItemClick(MenuItem menuItem, MessageItem msgItem) {
        MmsLog.d(IPMSG_TAG, "onIpMessageMenuItemClick(): ");
        switch (menuItem.getItemId()) {

        case MENU_SEND_VIA_TEXT_MSG:
            if (mWorkingMessage.requiresMms() || !TextUtils.isEmpty(mTextEditor.getText().toString())
                    || mIpMessageDraft != null) {
                showDiscardCurrentMessageDialog(msgItem.mMsgId);
            } else {
                sendViaMmsOrSms(msgItem.mMsgId);
            }
            return true;

        case MENU_RETRY:
            IpMessageUtils.getMessageManager(this).resendMessage(msgItem.mMsgId,msgItem.getSimId());
            return true;

        case MENU_FORWARD_IPMESSAGE:
            MmsLog.d(TAG,"MENU_FORWARD_IPMESSAGE");
            hideInputMethod();
            forwardIpMsg(ComposeMessageActivity.this, msgItem.mMsgId);
            return true;

        case MENU_DELETE_IP_MESSAGE:
            MmsLog.d(IPMSG_TAG, "onIpMessageMenuItemClick(): Delete IP message");
            DeleteMessageListener delMsgListener = new DeleteMessageListener(msgItem);
            String where = Telephony.Mms._ID + "=" + msgItem.mMsgId;
            String[] projection = new String[] { Sms.Inbox.THREAD_ID };
            MmsLog.d(IPMSG_TAG, "onIpMessageMenuItemClick(): where = " + where);
            Cursor queryCursor = Sms.query(getContentResolver(), projection, where, null);
            if (queryCursor.moveToFirst()) {
                mThreadId = queryCursor.getLong(0);
                queryCursor.close();
            }
            confirmDeleteDialog(delMsgListener, msgItem.mLocked);
            return true;

        case MENU_SHARE:
            IpMessage ipMsp = IpMessageUtils.getMessageManager(this).getIpMsgInfo(msgItem.mMsgId);
            shareIpMsg(ipMsp);
            return true;

        case MENU_EXPORT_SD_CARD:
            IpMessage ipMessageForSave = IpMessageUtils.getMessageManager(this).getIpMsgInfo(msgItem.mMsgId);
            MmsLog.d(IPMSG_TAG, "onIpMessageMenuItemClick(): Save IP message. msgId = " + msgItem.mMsgId
                + ", type = " + ipMessageForSave.getType());
            if (ipMessageForSave.getType() >= IpMessageType.PICTURE
                    && ipMessageForSave.getType() < IpMessageType.UNKNOWN_FILE) {
                saveMsgInSDCard((IpAttachMessage) ipMessageForSave);
            }
            return true;

        case MENU_VIEW_IP_MESSAGE:
            IpMessage ipMessageForView = IpMessageUtils.getMessageManager(this).getIpMsgInfo(msgItem.mMsgId);
            MmsLog.d(IPMSG_TAG, "onIpMessageMenuItemClick(): View IP message. msgId = " + msgItem.mMsgId
                + ", type = " + ipMessageForView.getType());
            if (ipMessageForView.getType() >= IpMessageType.PICTURE
                    && ipMessageForView.getType() < IpMessageType.UNKNOWN_FILE) {
                openMedia(msgItem.mMsgId, (IpAttachMessage) ipMessageForView);
            }
            return true;

        default:
            break;
        }
        return false;
    }

    private void saveMsgInSDCard(IpAttachMessage ipAttachMessage) {
        if (!IpMessageUtils.getSDCardStatus()) {
            MmsApp.getToastHandler().sendEmptyMessage(MmsApp.MSG_RETRIEVE_FAILURE_DEVICE_MEMORY_FULL);
            MessageUtils.createLoseSDCardNotice(this,
                                    IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                                .getSingleString(IpMessageConsts.string.ipmsg_cant_save));
            return;
        }
        String source = ipAttachMessage.getPath();
        if (TextUtils.isEmpty(source)) {
            MmsLog.e(IPMSG_TAG, "saveMsgInSDCard(): save vcard failed.");
            Toast.makeText(this, getString(R.string.copy_to_sdcard_fail), Toast.LENGTH_SHORT).show();
            return;
        }
        String attName = source.substring(source.lastIndexOf("/") + 1);
        String dstFile = "";
        dstFile = IpMessageUtils.getCachePath(this) + attName;
        int i = 1;
        while (IpMessageUtils.isExistsFile(dstFile)) {
            dstFile = IpMessageUtils.getCachePath(this) + "(" + i + ")" + attName;
            i++;
        }
        IpMessageUtils.copy(source, dstFile);
        String saveSuccess = String.format(
                                        IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                                    .getSingleString(IpMessageConsts.string.ipmsg_save_file),
                                        dstFile);
        Toast.makeText(this, saveSuccess, Toast.LENGTH_SHORT).show();
    }

    private void openMedia(long msgId, IpAttachMessage ipAttachMessage) {
        if (ipAttachMessage == null) {
            MmsLog.e(IPMSG_TAG, "openMedia(): ipAttachMessage is null!", new Exception());
            return;
        }
        MmsLog.d(IPMSG_TAG, "openMedia(): ipAttachMessage type = " + ipAttachMessage.getType());

        if (ipAttachMessage.getType() == IpMessageType.VCARD) {
            IpVCardMessage msg = (IpVCardMessage) ipAttachMessage;
            if (TextUtils.isEmpty(msg.getPath())) {
                MmsLog.e(IPMSG_TAG, "openMedia(): open vCard failed.");
                return;
            }
            if (!IpMessageUtils.getSDCardStatus()) {
                MessageUtils.createLoseSDCardNotice(this,
                        IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_cant_share));
                return;
            }
            if (MessageUtils.getAvailableBytesInFileSystemAtGivenRoot(EncapsulatedStorageManager.getDefaultPath())
                    < msg.getSize()) {
                Toast.makeText(this, getString(R.string.export_disk_problem), Toast.LENGTH_LONG).show();
            }
            String dest = IpMessageUtils.getCachePath(this) + "temp"
                + msg.getPath().substring(msg.getPath().lastIndexOf(".vcf"));
            IpMessageUtils.copy(msg.getPath(), dest);
            File vcardFile = new File(dest);
            Uri vcardUri = Uri.fromFile(vcardFile);
            Intent i = new Intent();
            i.setAction(android.content.Intent.ACTION_VIEW);
            i.setDataAndType(vcardUri, "text/x-vcard");
            startActivity(i);
        } else if (ipAttachMessage.getType() == IpMessageType.CALENDAR) {
            IpVCalendarMessage msg = (IpVCalendarMessage) ipAttachMessage;
            if (TextUtils.isEmpty(msg.getPath())) {
                MmsLog.e(IPMSG_TAG, "openMedia(): open vCalendar failed.");
                return;
            }
            if (!IpMessageUtils.getSDCardStatus()) {
                MessageUtils.createLoseSDCardNotice(this,
                        IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_cant_share));
                return;
            }
            if (MessageUtils.getAvailableBytesInFileSystemAtGivenRoot(EncapsulatedStorageManager.getDefaultPath())
                    < msg.getSize()) {
                Toast.makeText(this, getString(R.string.export_disk_problem),
                    Toast.LENGTH_LONG).show();
            }
            String dest = IpMessageUtils.getCachePath(this) + "temp"
                + msg.getPath().substring(msg.getPath().lastIndexOf(".vcs"));
            IpMessageUtils.copy(msg.getPath(), dest);
            File calendarFile = new File(dest);
            Uri calendarUri = Uri.fromFile(calendarFile);
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(calendarUri, "text/x-vcalendar");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                MmsLog.e(IPMSG_TAG, "can't open calendar");
            }
        } else {
            Intent intent = new Intent(RemoteActivities.MEDIA_DETAIL);
            intent.putExtra(RemoteActivities.KEY_MESSAGE_ID, msgId);
            IpMessageUtils.startRemoteActivity(this, intent);
        }
    }

    private class InviteFriendsToIpMsgListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int whichButton) {
            mTextEditor.setText(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_invite_friends_content));

        }
    }

    private void changeWallPaper() {
        MmsLog.d(IPMSG_TAG, "changeWallPaper(): thread id = " + mConversation.getThreadId());
        if ((mConversation.getThreadId() > 0 && setWallPaper(mConversation.getThreadId()))
                || setWallPaper(0L)) {
            return;
        }
//        mHeightChangedLinearLayout.setBackgroundColor(getResources().getColor(R.color.list_background));
        mWallPaper.setImageResource(R.color.list_background);
    }

    private boolean setWallPaper(long threadId) {
        MmsLog.d(IPMSG_TAG, "setWallPaper(): threadId = " + threadId);
        Cursor cursor = getContentResolver().query(EncapsulatedTelephony.ThreadSettings.CONTENT_URI,
            new String[] {ThreadSettings._ID, ThreadSettings.WALLPAPER},
            ThreadSettings.THREAD_ID + " = " + threadId, null, null);
        long threadSettingId = 0L;
        String filePath = null;
        if (null != cursor) {
            if (cursor.moveToFirst()) {
                threadSettingId = cursor.getLong(0);
                filePath = cursor.getString(1);
            }
            cursor.close();
        }
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        MmsLog.d(IPMSG_TAG, "setWallPaper(): threadSettingId = " + threadSettingId + ", filePath = " + filePath);
        if (filePath.startsWith("content://") || filePath.startsWith("file://")) {
            Uri imageUri = Uri.parse(filePath);
            InputStream is = null;
            try {
                is = this.getContentResolver().openInputStream(imageUri);
                BitmapDrawable bd = new BitmapDrawable(is);
//                mHeightChangedLinearLayout.setBackgroundDrawable(bd);
                mWallPaper.setImageDrawable(bd);
                return true;
            } catch (FileNotFoundException e) {
                MmsLog.w(IPMSG_TAG, "Unknown wall paper resource! filePath = " + filePath);
            } finally {
                if (null != is) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        MmsLog.w(IPMSG_TAG, "IOException happened when close InputStream.", e);
                    }
                }
            }
        } else if (filePath.startsWith(IpMessageUtils.WALLPAPER_PATH)) {
            Uri imageUri = ContentUris.withAppendedId(EncapsulatedTelephony.ThreadSettings.CONTENT_URI, threadId);
            InputStream is = null;
            try {
                is = this.getContentResolver().openInputStream(imageUri);
                BitmapDrawable bd = new BitmapDrawable(is);
//                mHeightChangedLinearLayout.setBackgroundDrawable(bd);
                mWallPaper.setImageDrawable(bd);
                return true;
            } catch (FileNotFoundException e) {
                MmsLog.w(IPMSG_TAG, "Unknown wall paper resource! filePath = " + filePath, e);
            } catch (NullPointerException e) {
                MmsLog.w(IPMSG_TAG, "Unknown wall paper resource! filePath = " + filePath, e);
            } finally {
                if (null != is) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        MmsLog.w(IPMSG_TAG, "IOException happened when close InputStream.", e);
                    }
                    MmsLog.w(IPMSG_TAG, "Unknown wall paper resource! filePath = " + filePath);
                }
            }
        } else if (TextUtils.isDigitsOnly(filePath)) {
            int resId = 0;
            try {
                resId = Integer.parseInt(filePath);
                if (resId > 0) {
//                    mHeightChangedLinearLayout.setBackgroundResource(resId);
                    mWallPaper.setImageResource(resId);
                    return true;
                }
            } catch (NumberFormatException e) {
                MmsLog.w(IPMSG_TAG, "Unknown wall paper resource id! resourceId = " + resId);
            }
            MmsLog.w(IPMSG_TAG, "Unknown wall paper resource! filePath = " + filePath);
        }
        return false;
    }

    /**
     * M:
     * @param ipMessage
     */
    private void shareIpMsg(IpMessage ipMessage) {
        if (null == ipMessage) {
            MmsLog.d(IPMSG_TAG, "shareIpMsg(): message item is null!");
            return;
        }
        if (ipMessage instanceof IpAttachMessage) {
            if (!IpMessageUtils.getSDCardStatus()) {
                MessageUtils.createLoseSDCardNotice(this,
                        IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_cant_share));
                return;
            }
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent = setIntent(intent, ipMessage);
        intent.putExtra(Intent.EXTRA_SUBJECT, IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                                            .getSingleString(IpMessageConsts.string.ipmsg_logo));
        try {
            startActivity(Intent.createChooser(intent,
                    IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                .getSingleString(IpMessageConsts.string.ipmsg_share_title)));
        } catch (Exception e) {
            MmsLog.d(IPMSG_TAG, "shareIpMsg(): Exception:" + e.toString());
        }
    }

    private Intent setIntent(Intent intent, IpMessage ipMessage) {
        if (ipMessage.getType() == IpMessageType.TEXT) {
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, ((IpTextMessage) ipMessage).getBody());
        } else if (ipMessage.getType() == IpMessageType.PICTURE
                || ipMessage.getType() == IpMessageType.SKETCH) {
            IpImageMessage msg = (IpImageMessage) ipMessage;
            int index = msg.getPath().lastIndexOf(".");
            if (msg.getType() == IpMessageType.SKETCH) {
                index = msg.getPath().indexOf(".ske.png");
            }
            String end = msg.getPath().substring(index);
            String dest = IpMessageUtils.getCachePath(this) + "temp" + end;
            IpMessageUtils.copy(msg.getPath(), dest);
            intent.setType("image/*");
            File f = new File(dest);
            Uri u = Uri.fromFile(f);
            intent.putExtra(Intent.EXTRA_STREAM, u);
            if (!TextUtils.isEmpty(msg.getCaption())) {
                intent.putExtra(SMS_BODY, msg.getCaption());
                intent.putExtra(Intent.EXTRA_TEXT, msg.getCaption());
            }
        } else if (ipMessage.getType() == IpMessageType.VOICE) {
            IpVoiceMessage msg = (IpVoiceMessage) ipMessage;
            int index = msg.getPath().lastIndexOf("/");
            String name = msg.getPath().substring(index);
            String dest = IpMessageUtils.getCachePath(this) + name;
            IpMessageUtils.copy(msg.getPath(), dest);
            intent.setType("audio/*");
            File f = new File(dest);
            Uri u = Uri.fromFile(f);
            intent.putExtra(Intent.EXTRA_STREAM, u);
            if (!TextUtils.isEmpty(msg.getCaption())) {
                intent.putExtra(SMS_BODY, msg.getCaption());
                intent.putExtra(Intent.EXTRA_TEXT, msg.getCaption());
            }
        } else if (ipMessage.getType() == IpMessageType.VCARD) {
            IpVCardMessage msg = (IpVCardMessage) ipMessage;
            int index = msg.getPath().lastIndexOf("/");
            String name = msg.getPath().substring(index);
            String dest = IpMessageUtils.getCachePath(this) + name;
            IpMessageUtils.copy(msg.getPath(), dest);
            File f = new File(dest);
            Uri u = Uri.fromFile(f);
            intent.setDataAndType(u, "text/x-vcard");
            intent.putExtra(Intent.EXTRA_STREAM, u);
        } else if (ipMessage.getType() == IpMessageType.VIDEO) {
            IpVideoMessage msg = (IpVideoMessage) ipMessage;
            int index = msg.getPath().lastIndexOf("/");
            String name = msg.getPath().substring(index);
            String dest = IpMessageUtils.getCachePath(this) + name;
            IpMessageUtils.copy(msg.getPath(), dest);
            intent.setType("video/*");
            File f = new File(dest);
            Uri u = Uri.fromFile(f);
            intent.putExtra(Intent.EXTRA_STREAM, u);
            if (!TextUtils.isEmpty(msg.getCaption())) {
                intent.putExtra(SMS_BODY, msg.getCaption());
                intent.putExtra(Intent.EXTRA_TEXT, msg.getCaption());
            }
        } else if (ipMessage.getType() == IpMessageType.LOCATION) {
            IpLocationMessage msg = (IpLocationMessage) ipMessage;
            if (TextUtils.isEmpty(msg.getPath())) {
                intent.setType("text/plain");
                intent.putExtra(SMS_BODY, msg.getAddress());
                intent.putExtra(Intent.EXTRA_TEXT, msg.getAddress());
            } else {
                int index = msg.getPath().lastIndexOf(".map.png");
                String end = msg.getPath().substring(index);
                String dest = IpMessageUtils.getCachePath(this) + "temp" + end;
                IpMessageUtils.copy(msg.getPath(), dest);
                intent.setType("image/*");
                File f = new File(dest);
                Uri uri = Uri.fromFile(f);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                if (!TextUtils.isEmpty(msg.getAddress())) {
                    intent.putExtra(SMS_BODY, msg.getAddress());
                    intent.putExtra(Intent.EXTRA_TEXT, msg.getAddress());
                }
            }
        } else if (ipMessage.getType() == IpMessageType.CALENDAR) {
            IpVCalendarMessage msg = (IpVCalendarMessage) ipMessage;
            int index = msg.getPath().lastIndexOf("/");
            String name = msg.getPath().substring(index);
            String dest = IpMessageUtils.getCachePath(this) + name;
            IpMessageUtils.copy(msg.getPath(), dest);
            File f = new File(dest);
            Uri uri = Uri.fromFile(f);
            intent.setType("text/x-vcalendar");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
        } else {
            intent.setType("unknown");
        }
        return intent;
    }

    /**
     * M:
     * @param ids
     */
    private void showResendConfirmDialg(final long currentMsgId, final long currentSimId,
            final long[][] allFailedIpMsgIds) {
        IpMessage ipMessage = IpMessageUtils.getMessageManager(this).getIpMsgInfo(currentMsgId);
        if (ipMessage == null) {
            MmsLog.e(IPMSG_TAG, "showResendConfirmDialg(): ipMessage is null.", new Exception());
            return;
        }
        String title = "";
        if (ipMessage.getStatus() == IpMessageStatus.FAILED) {
            title = IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                .getSingleString(IpMessageConsts.string.ipmsg_failed_title);
        } else {
            title = IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                .getSingleString(IpMessageConsts.string.ipmsg_not_delivered_title);
        }
        String sendViaMsg = "";
        if (ipMessage.getType() == IpMessageType.TEXT) {
            sendViaMsg = IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_resend_via_sms);
        } else {
            sendViaMsg = IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_resend_via_mms);
        }
        List<String> buttonList = new ArrayList<String>();
        buttonList.add(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_try_again));
        if (allFailedIpMsgIds != null && allFailedIpMsgIds.length > 1) {
            buttonList.add(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_try_all_again));
        }
        buttonList.add(sendViaMsg);
        final int buttonCount = buttonList.size();
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(title);
        ArrayAdapter<String> resendAdapter = new ArrayAdapter<String>(this, R.layout.resend_dialog_item,
                R.id.resend_item, buttonList);
        builder.setAdapter(resendAdapter, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final int tryAgain = 0;
                final int tryAllAgain = 1;
                final int sendViaMmsOrSms = 2;
                switch (which) {
                case tryAgain:
                    IpMessageUtils.getMessageManager(ComposeMessageActivity.this).resendMessage(currentMsgId,
                        (int) currentSimId);
                    break;
                case tryAllAgain:
                    if (buttonCount == 3) {
                        for (int index = 0; index < allFailedIpMsgIds.length; index++) {
                            IpMessageUtils.getMessageManager(ComposeMessageActivity.this)
                                    .resendMessage(allFailedIpMsgIds[index][0],
                                        (int) allFailedIpMsgIds[index][1]);
                        }
                        break;
                    } else if (buttonCount != 2) {
                        break;
                    } else {
                        /// M: listSize == 2, run case sendViaMmsOrSms.
                        // fall through
                    }
                case sendViaMmsOrSms:
                    if (mWorkingMessage.requiresMms() || !TextUtils.isEmpty(mTextEditor.getText().toString())
                        || mIpMessageDraft != null) {
                        showDiscardCurrentMessageDialog(currentMsgId);
                    } else {
                        sendViaMmsOrSms(currentMsgId);
                    }
                    break;
                default:
                    break;
                }
            }
        });
        builder.show();

    }

    private void sendViaMmsOrSms(long currentMsgId) {
        mJustSendMsgViaCommonMsgThisTime = true;
        final IpMessage ipMessage = IpMessageUtils.getMessageManager(
                ComposeMessageActivity.this).getIpMsgInfo(currentMsgId);
        if (ipMessage != null) {
            mIpMessageDraft = ipMessage;
            if (convertIpMessageToMmsOrSms(false)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (isSubjectEditorVisible()) {
                            showSubjectEditor(false);
                            mWorkingMessage.setSubject(null, true);
                        }
                        drawBottomPanel();
                        boolean isMms = mWorkingMessage.requiresMms();
                        showSmsOrMmsSendButton(isMms  ? true : false);
                    }
                });
                IpMessageUtils.getMessageManager(ComposeMessageActivity.this)
                        .deleteIpMsg(new long[] {currentMsgId}, true, true);
            }
        }
    }

    private void showDiscardCurrentMessageDialog(final long currentMsgId) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.discard)
            .setMessage(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_resend_discard_message))
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            })
            .setPositiveButton(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_continue),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendViaMmsOrSms(currentMsgId);
                    }
                }
            ).show();
    }
    /**
     * M:
     * @param threadId
     * @return
     */
    private long[][] getAllFailedIpMsgByThreadId(long threadId) {
        Cursor cursor = getContentResolver()
                .query(Sms.CONTENT_URI, new String[] {Sms._ID, TextBasedSmsColumns.SIM_ID},
                    "thread_id = " + threadId + " and ipmsg_id > 0 and type = " + Sms.MESSAGE_TYPE_FAILED, null, null);
        try {
            if (cursor == null) {
                return null;
            }
            long[][] count = new long[cursor.getCount()][2];
            int index = 0;
            while (cursor.moveToNext()) {
                count[index][0] = cursor.getLong(0);
                count[index][1] = cursor.getLong(1);
                index++;
            }
            return count;
        } finally {
            cursor.close();
        }
    }

    /**
     * M:
     *
     */
    public void hiddeInvitePanel() {
        if (mInviteView != null) {
            mInviteView.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * M:
     *
     */
    private void showInvitePanel() {
        if (mInviteView == null ||
            mConversation == null ||
            mConversation.getThreadId() <= 0 ||
            isRecipientsEditorVisible()) {
            hiddeInvitePanel();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                int needShowInvite = IpMessageUtils.getChatManager(ComposeMessageActivity.this)
                        .needShowReminderDlg(mConversation.getThreadId());
                Message msg = new Message();
                msg.what = MessageUtils.SHOW_INVITE_PANEL;
                msg.arg1 = needShowInvite;
                mIpMsgHandler.sendMessage(msg);
            }
        }).start();
    }

    private void showInvitePanel(int inviteType) {
        switch (inviteType) {
            case ReminderType.REMINDER_INVITE:
                if (mShowInviteMsg) {
                    // / M: invite user to use ipmessage
                    mInviteView.setVisibility(View.VISIBLE);
                    ContactList contactList1 = mConversation.getRecipients();
                    Contact contact1 = contactList1.get(0);
                    String inviteStr1 = String.format(
                        IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_invite_chat_frequently),
                        contact1.getName(),
                        contact1.getName());
                    mInvitePostiveBtn.setText(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                                            .getSingleString(IpMessageConsts.string.ipmsg_invite));
                    mInvitePostiveBtn.setOnClickListener(mInviteClickListener);
                    mInvitePostiveBtn.setClickable(true);
                    mInviteNegativeBtn.setText(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                                            .getSingleString(IpMessageConsts.string.ipmsg_dismiss));
                    mInviteNegativeBtn.setOnClickListener(mDismissInviteClickListener);
                    mInviteNegativeBtn.setClickable(true);
                    mInviteMsg.setText(inviteStr1);
                } else {
                    hiddeInvitePanel();
                }
                return;

            case ReminderType.REMINDER_ACTIVATE:
                /// M: activate ipmessage
                mInviteView.setVisibility(View.VISIBLE);
                String inviteStr2 = IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                                .getSingleString(IpMessageConsts.string.ipmsg_activate_chat_frequently);
                mInvitePostiveBtn.setText(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                                        .getSingleString(IpMessageConsts.string.ipmsg_activate));
                mInvitePostiveBtn.setOnClickListener(mActivateIpMsgListener);
                mInvitePostiveBtn.setClickable(true);
                mInviteNegativeBtn.setText(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                                        .getSingleString(IpMessageConsts.string.ipmsg_dismiss));
                mInviteNegativeBtn.setOnClickListener(mDismissInviteClickListener);
                mInviteNegativeBtn.setClickable(true);
                mInviteMsg.setText(inviteStr2);
                return;

            case ReminderType.REMINDER_ENABLE:
                /// M: enable ipmessage Service
                mInviteView.setVisibility(View.VISIBLE);
                String inviteStr = String.format(
                    IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                .getSingleString(IpMessageConsts.string.ipmsg_enable_chat_frequently),
                    MessageUtils.getMainCardDisplayName());
                mInvitePostiveBtn.setText(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                                        .getSingleString(IpMessageConsts.string.ipmsg_enable));
                mInvitePostiveBtn.setOnClickListener(mEnableIPMsgListener);
                mInvitePostiveBtn.setClickable(true);
                mInviteNegativeBtn.setText(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                                        .getSingleString(IpMessageConsts.string.ipmsg_dismiss));
                mInviteNegativeBtn.setOnClickListener(mDismissInviteClickListener);
                mInviteNegativeBtn.setClickable(true);
                mInviteMsg.setText(inviteStr);
                return;

            case ReminderType.REMINDER_INVALID:
            case ReminderType.REMINDER_SWITCH:
            default:
                // /M: hidde invite panel
                hiddeInvitePanel();
        }
    }

    /// M: activate ipMsg listener
    private View.OnClickListener mActivateIpMsgListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new AlertDialog.Builder(ComposeMessageActivity.this)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setTitle(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_activate))
            .setMessage(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_activate_note))
            .setPositiveButton(R.string.yes, new OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    hiddeInvitePanel();
                    IpMessageUtils.checkCurrentIpMessageServiceStatus(
                            ComposeMessageActivity.this, false,null);
                }
            }).setNegativeButton(R.string.no, null).show();
        }
    };

    /// M: enable ipmessage service listener
    private View.OnClickListener mEnableIPMsgListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new AlertDialog.Builder(ComposeMessageActivity.this)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setTitle(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_enable))
            .setMessage(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_enable_notice))
            .setPositiveButton(R.string.yes, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        hiddeInvitePanel();
                        IpMessageUtils.checkCurrentIpMessageServiceStatus(
                                ComposeMessageActivity.this, false, mIpMsgHandler);
                    }
            }).setNegativeButton(R.string.no, null).show();
        }
    };

    /// M: inivte frends to use ipmessage
    private View.OnClickListener mInviteClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            IpMessageUtils.showInviteIpMsgConfirmDialog(ComposeMessageActivity.this,
                new OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mTextEditor.setText(
                                IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                            .getSingleString(IpMessageConsts.string.ipmsg_invite_friends_content));
                        hiddeInvitePanel();
                        IpMessageUtils.getChatManager(ComposeMessageActivity.this).handleInviteDlg(
                            mConversation.getThreadId());
                    }
                });
        }
    };

    /// M: dismiss
    private View.OnClickListener mDismissInviteClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            new AlertDialog.Builder(ComposeMessageActivity.this)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(R.string.discard_message)
                .setMessage(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                        .getSingleString(IpMessageConsts.string.ipmsg_dismiss_content))
                .setPositiveButton(R.string.yes, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        hiddeInvitePanel();
                        IpMessageUtils.getChatManager(ComposeMessageActivity.this)
                            .handleInviteDlgLater(mConversation.getThreadId());
                    }
                }).setNegativeButton(R.string.no, null).show();

        }
    };

    /**
     * M:
     * @param msgId
     * @return
     */
    private boolean forwardIpMsg(Context context, long msgId) {
        if (msgId <= 0) {
            return false;
        }
        Intent intent = ComposeMessageActivity.createIntent(context, 0);
        intent.putExtra(FORWARD_MESSAGE, true);//"forwarded_message", boolean
        intent.putExtra(FORWARD_IPMESSAGE, true);//"ip_msg_media_path", boolean
        intent.putExtra(IP_MESSAGE_ID, msgId);//"ip_msg_id", long
        intent.setClassName(context, "com.android.mms.ui.ForwardMessageActivity");
        context.startActivity(intent);
        return true;
    }

    private Runnable mSendTypingRunnable = new Runnable() {
        @Override
        public void run() {
            MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "send chat mode, typing");
            if (!TextUtils.isEmpty(mChatModeNumber)) {
                IpMessageUtils.getChatManager(ComposeMessageActivity.this)
                    .sendChatMode(mChatModeNumber, ContactStatus.TYPING);
            }
        }
    };

    private Runnable mSendStopTypingRunnable = new Runnable() {
        @Override
        public void run() {
            MmsLog.d(IpMessageUtils.IPMSG_NOTIFICATION_TAG, "send chat mode, stop typing");
            if (!TextUtils.isEmpty(mChatModeNumber)) {
                IpMessageUtils.getChatManager(ComposeMessageActivity.this)
                    .sendChatMode(mChatModeNumber, ContactStatus.STOP_TYPING);
            }
        }
    };

    private void clearIpMessageDraft() {
        MmsLog.d(IPMSG_TAG, "clearIpMessageDraft()");
        mIpMessageDraft = null;
        if (null != mIpMessageThumbnail) {
            mIpMessageThumbnail.setVisibility(View.GONE);
        }
        mIsEditingCaption = false;
        updateSendButtonState();
    }

    private static void toastNoSimCard(Context context) {
        Toast.makeText(context,
                        IpMessageUtils.getResourceManager(context)
                                    .getSingleString(IpMessageConsts.string.ipmsg_no_sim_card),
                        Toast.LENGTH_LONG).show();
    }

    private boolean isChatModeNumber(String number) {
        if (TextUtils.isEmpty(mChatModeNumber) || TextUtils.isEmpty(number)) {
            return false;
        }
        if (mChatModeNumber.equals(number) || mChatModeNumber.endsWith(number) || number.endsWith(mChatModeNumber)) {
            return true;
        }
        return false;
    }

    /// M:added for bug ALPS00317889
    private boolean mShowDialogForMultiImage = false;

    private boolean isNetworkConnected(Context context) {
        boolean isNetworkConnected = false;
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(
            ComposeMessageActivity.this.CONNECTIVITY_SERVICE);
        State state = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        if (State.CONNECTED == state) {
            isNetworkConnected = true;
        }
        if (!isNetworkConnected) {
            state = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
            if (State.CONNECTED == state) {
                isNetworkConnected = true;
            }
        }
        return isNetworkConnected;
    }

    private NetworkInfo getActiveNetworkInfo(Context context) {
        ConnectivityManager connectivity =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return null;
        }
        return connectivity.getActiveNetworkInfo();
    }

    private boolean canMmsConvertToIpMessage() {
        if (!mWorkingMessage.requiresMms()) {
            return false;
        }
        if (mWorkingMessage.getSlideshow() == null) {
            return false;
        }
        if (mWorkingMessage.hasSubject()) {
            return false;
        }
        if (mWorkingMessage.getSlideshow().getAttachFiles() != null &&
                mWorkingMessage.getSlideshow().getAttachFiles().size() == 1) {
            MmsLog.d(IPMSG_TAG, "canMmsConvertToIpMessage(): Can not convert file attachment,"
                + " but vCard");
            String contenttype = mWorkingMessage.getSlideshow().getAttachFiles().get(0).getContentType();
            return (contenttype.equals(EncapsulatedContentType.TEXT_VCARD)
                    || contenttype.equals(EncapsulatedContentType.TEXT_VCALENDAR));
        }
        boolean isMmsCanConvertToIpmessage = !mWorkingMessage.hasSubject()
            && mWorkingMessage.getSlideshow() != null
            && mWorkingMessage.getSlideshow().size() == 1;
        if (isMmsCanConvertToIpmessage && mWorkingMessage.getSlideshow().get(0) != null) {
            SlideModel slideModel = mWorkingMessage.getSlideshow().get(0);
            if (slideModel.hasAudio() && slideModel.hasImage()) {
                return false;
            }
            boolean hasOverCaptionLimit = slideModel.hasText()
                && slideModel.getText().getText().length() > IpMessageConfig.CAPTION_MAX_LENGTH;
            if (slideModel.hasAudio()
                    && ((mIsCaptionOn && mIsAudioCaptionOn && !hasOverCaptionLimit)
                            || (!slideModel.hasText()))) {
                return true;
            } else if (slideModel.hasImage()
                    && ((mIsCaptionOn && mIsImageCaptionOn && !hasOverCaptionLimit)
                            || (!slideModel.hasText()))) {
                return true;
            } else if (slideModel.hasVideo()
                    && ((mIsCaptionOn && mIsVideoCaptionOn && !hasOverCaptionLimit)
                            || (!slideModel.hasText()))) {
                return true;
            }
        }
        return false;
    }

    private void showReplaceAttachDialog() {
        if (mReplaceDialog != null) {
            return; /// M: shown already.
        }
        mReplaceDialog = new AlertDialog.Builder(this)
            .setTitle(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_replace_attach))
            .setMessage(IpMessageUtils.getResourceManager(ComposeMessageActivity.this)
                                    .getSingleString(IpMessageConsts.string.ipmsg_replace_attach_msg))
            .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mIpMessageDraft = null;
                        mReplaceDialog = null;
                        saveIpMessageForAWhile(mIpMessageForSend);
                    }
                }
            )
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mIpMessageForSend = null;
                        mReplaceDialog = null;
                    }
                }
            )
            .create();
        mReplaceDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mReplaceDialog = null;
            }
        });
        mReplaceDialog.show();
    }
    /// M: show contact detail or create new contact. @{
    private static final int MENU_SHOW_CONTACT          = 121;
    private static final int MENU_CREATE_CONTACT        = 122;
    private MmsQuickContactBadge mQuickContact;
    /// @}

    private boolean updateIpMessageCounter(CharSequence text, int start, int before, int count) {
        MmsLog.d(TAG, "updateIpMessageCounter()");
        /// M: add for caption text counter
        if (mIsEditingCaption) {
            if (text.length() > IpMessageConfig.CAPTION_MAX_LENGTH) {
                MmsLog.d(IPMSG_TAG, "updateIpMessageCounter(): Caption text over limit, count = " + text.length());
                return false;
            }
            MmsLog.d(IPMSG_TAG, "updateIpMessageCounter(): Caption");
            mTextCounter.setVisibility(View.VISIBLE);
            mTextCounter.setText(text.length() + "/" + IpMessageConfig.CAPTION_MAX_LENGTH);
            return true;
        }
        if (mIsMessageDefaultSimIpServiceEnabled && isNetworkConnected(getApplicationContext())
                && IpMessageUtils.getSDCardStatus() && isCurrentRecipientIpMessageUser()
                && !mWorkingMessage.requiresMms() && !mJustSendMsgViaCommonMsgThisTime
                && text.length() > 0) {
            MmsLog.d(IPMSG_TAG, "updateIpMessageCounter(): IP text message, mTextEditor.getLineCount() = "
                + mTextEditor.getLineCount());
            if (mTextEditor.getLineCount() > 1) {
                mTextCounter.setVisibility(View.VISIBLE);
                mTextCounter.setText(text.length() + "/" + MmsConfig.getMaxTextLimit());
            } else {
                mTextCounter.setVisibility(View.GONE);
                mTextCounter.setText(text.length() + "/" + MmsConfig.getMaxTextLimit());
            }
            return true;
        }
        return false;
    }

    /// M: fix bug ALPS00414023, update sim state dynamically. @{
    private BroadcastReceiver mSimReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED)) {
                    /// M: fix bug ALPS00429274, dismiss Dialog when SIM_CHANGED @{
                    if (mSIMSelectDialog != null && mSIMSelectDialog.isShowing()) {
                        mSIMSelectDialog.dismiss();
                    }
                    /// @}
                    new Thread(mGetSimInfoRunnable).start();
                    MessageUtils.simInfoMap.clear();
                    mMsgListAdapter.notifyDataSetChanged();
            }
        }
    };

    private void saveAsMms(boolean notify) {
        if (mNeedSaveAsMms) {
            mNeedSaveAsMms = false;
            if (mWorkingMessage.saveAsMms(notify) != null) {
                mHasDiscardWorkingMessage = true;
            }
        }
    }

    private void recycleShareAndEmoticonView() {
        mEmoticonPanel.recycleView();
        mSharePanel.recycleView();
    }

    /// M: add for IP message draft. @{
    private void saveIpMessageDraft() {
        if (mIpMessageDraft == null) {
            MmsLog.e(IPMSG_TAG, "saveIpMessageDraft(): mIpMessageDraft is null!", new Exception());
            return;
        }
        mIpMessageDraft.setStatus(IpMessageStatus.DRAFT);
        mIpMessageDraft = IpMessageUtils.setIpMessageCaption(mIpMessageDraft, mWorkingMessage.getText().toString());

        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            if (mSimInfoList.size() > 0) {
                for (SIMInfo simInfo : mSimInfoList) {
                    if (MmsConfig.isServiceEnabled(this, (int) simInfo.getSimId())) {
                        mIpMessageDraft.setSimId((int) simInfo.getSimId());
                        MmsLog.d(IPMSG_TAG, "saveIpMessageDraft(): Set IP message draft SIM. simId = "
                            + simInfo.getSimId());
                        break;
                    }
                }
                if (mIpMessageDraft.getSimId() <= 0) {
                    MmsLog.e(IPMSG_TAG, "saveIpMessageDraft(): No enabled service SIM card!", new Exception());
                    return;
                }
            } else {
                MmsLog.e(IPMSG_TAG, "saveIpMessageDraft(): No SIM card.", new Exception());
                return;
            }
        } else {
            mIpMessageDraft.setSimId(SpecialSimId.SINGLE_LOAD_SIM_ID);
        }

        mIpMessageDraft.setTo(getCurrentNumberString());
        mConversation.setDraftState(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                MmsLog.d(IPMSG_TAG, "saveIpMessageDraft(): calling API: saveIpMsg().");
                int ret = -1;
                ret = IpMessageUtils.getMessageManager(ComposeMessageActivity.this)
                    .saveIpMsg(mIpMessageDraft, IpMessageSendMode.AUTO);
                if (ret < 0) {
                    MmsLog.w(IPMSG_TAG, "saveIpMessageDraft(): save IP message draft failed.");
                    mConversation.setDraftState(false);
                    if (mToastForDraftSave) {
                        MmsApp.getToastHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MmsApp.getApplication().getApplicationContext(),
                                    R.string.cannot_save_message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    MmsLog.d(IPMSG_TAG, "saveIpMessageDraft(): save IP message draft successfully.");
                    if (mToastForDraftSave) {
                        MmsApp.getToastHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MmsApp.getApplication().getApplicationContext(),
                                    R.string.message_saved_as_draft, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    private boolean loadIpMessagDraft() {
        MmsLog.d(IPMSG_TAG, "loadIpMessagDraft()");
        if (mIpMessageDraft != null) {
            MmsLog.w(IPMSG_TAG, "loadIpMessagDraft(): mIpMessageDraft is not null!", new Exception());
            return false;
        }
        IpMessage ipMessage = IpMessageUtils.readIpMessageDraft(this, mConversation, mWorkingMessage);
        if (ipMessage != null) {
            mIpMessageDraftId = ipMessage.getId();
            String caption = IpMessageUtils.getIpMessageCaption(ipMessage);
            MmsLog.d(IPMSG_TAG, "loadIpMessagDraft(): ipMessage is not null, mIpMessageDraftId = " + mIpMessageDraftId
                + "caption = " + caption);
            if (!TextUtils.isEmpty(caption)) {
                mWorkingMessage.setText(caption);
                mTextEditor.setText(caption);
            }
            saveIpMessageForAWhile(ipMessage);
            return true;
        }
        MmsLog.w(IPMSG_TAG, "loadIpMessagDraft(): ipMessage is null!");
        return false;
    }
    /// @}

    /// M: after delete the last message of one thread, don't finish this activity if have draft. @{
    private void makeDraftEditable(ContactList recipients) {
        if (!mConversation.getRecipients().equals(recipients)) {
            mConversation.setRecipients(recipients);
            MmsLog.d(TAG, "makeDraftEditable, do not equal");
        } else {
            MmsLog.d(TAG, "makeDraftEditable, equal");
        }
        mWorkingMessage.setConversation(mConversation);
        updateThreadIdIfRunning();
        invalidateOptionsMenu();
        hideRecipientEditor();
        initRecipientsEditor(null);
        isInitRecipientsEditor = true;
    }
    /// @}

    /// M: add a comma to the end of recipients editor, commit to a chip. @{
    private void commitToChipIfNeeded() {
        String recipient = null;
        if (mRecipientsEditor != null) {
            recipient = mRecipientsEditor.getText().toString();
        }
        if (!TextUtils.isEmpty(recipient)) {
            if (!recipient.endsWith(", ") && !recipient.endsWith(",")) {
                mRecipientsEditor.constructPressedChip();
            }
            mRecipientsEditor.updatePressedChip(RecipientsEditor.UpdatePressedChipType.ADD_CONTACT);
        }
    }
    /// @}

    private static final int CELL_PROGRESS_DIALOG = 1;

    @Override
    protected Dialog onCreateDialog(int id) {
        ProgressDialog dialog = null;
        if (id == CELL_PROGRESS_DIALOG) {
            dialog = new ProgressDialog(ComposeMessageActivity.this);
            // mProgressDialog.setTitle(getText(titleResId));
            dialog.setMessage(getString(R.string.sum_search_networks));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            // / M: fix bug ALPS00451836, remove FLAG_DISMISS_KEYGUARD flags
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
        return dialog;
    }

    /// M: Fix bug ALPS00451378, It's a workaround to load draft later.
    private final ContentObserver mDraftChangeObserver = new ContentObserver(new Handler()) {
        private static final int MAX_WAITING_TIME = 8 * 1000;
        private long mWaitingStartTime = System.currentTimeMillis();
        @Override
        public void onChange(boolean selfUpdate) {
            mContentResolver.unregisterContentObserver(mDraftChangeObserver);
            if (System.currentTimeMillis() - mWaitingStartTime > MAX_WAITING_TIME) {
                MmsLog.d(TAG, "mDraftChangeObserver, just return for timeout");
                return;
            }
            /// M: fix bug ALPS00520531, Do not load draft when compose is going to edit slideshow
            if (mConversation.hasDraft() && !mWaitingForSubActivity) {
                MmsLog.d(TAG, "mDraftChangeObserver, loadDraft" + " selfUpdate: " + selfUpdate);
                mWorkingMessage = WorkingMessage.loadDraft(ComposeMessageActivity.this,
                        mConversation, new Runnable() {
                            @Override
                            public void run() {
                                drawTopPanel(false);
                                drawBottomPanel();
                                updateSendButtonState();
                            }
                        });
            }
        }
    };

    private final OnDraftChangedListener mDraftChanged = new OnDraftChangedListener() {
        public void onDraftChanged(long threadId, boolean hasDraft) {
            MmsLog.d(TAG, "mDraftChanged, start~~~");
            if (mConversation.getThreadId() == threadId) {
                if (mConversation.hasDraft() && !mWaitingForSubActivity) {
                    MmsLog.d(TAG, "mDraftChanged, loadDraft");
                    mWorkingMessage = WorkingMessage.loadDraft(ComposeMessageActivity.this,
                            mConversation, new Runnable() {
                                @Override
                                public void run() {
                                    drawTopPanel(false);
                                    drawBottomPanel();
                                    updateSendButtonState();
                                }
                            });
                }
            }
        }
    };
}
