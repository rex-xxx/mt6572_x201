/*
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

package com.android.email.activity;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Directory;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.common.contacts.DataUsageStatUpdater;
import com.android.email.Controller;
import com.android.email.Email;
import com.android.email.EmailAddressAdapter;
import com.android.email.EmailAddressValidator;
import com.android.email.Preferences;
import com.android.email.R;
import com.android.email.RecipientAdapter;
import com.android.email.activity.setup.AccountSettings;
import com.android.email.mail.internet.EmailHtmlUtil;
import com.android.emailcommon.Logging;
import com.android.emailcommon.internet.MimeUtility;
import com.android.emailcommon.mail.Address;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.Attachment;
import com.android.emailcommon.provider.EmailContent.Body;
import com.android.emailcommon.provider.EmailContent.BodyColumns;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.EmailContent.MessageColumns;
import com.android.emailcommon.provider.EmailContent.QuickResponseColumns;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.provider.QuickResponse;
import com.android.emailcommon.utility.AttachmentUtilities;
import com.android.emailcommon.utility.DataCollectUtils;
import com.android.emailcommon.utility.EmailAsyncTask;
import com.android.emailcommon.utility.HtmlConverter;
import com.android.emailcommon.utility.TextUtilities;
import com.android.emailcommon.utility.Utility;
import com.android.ex.chips.AccountSpecifier;
import com.android.ex.chips.ChipsUtil;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.drm.OmaDrmClient;
import com.mediatek.drm.OmaDrmStore;
import com.mediatek.drm.OmaDrmUtils;
import com.mediatek.email.ui.SendWithoutSubjectConfirmDialog;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;


/**
 * Activity to compose a message.
 *
 * TODO Revive shortcuts command for removed menu options.
 * C: add cc/bcc
 * N: add attachment
 */
public class MessageCompose extends Activity implements OnClickListener, OnFocusChangeListener,
        DeleteMessageConfirmationDialog.Callback, InsertQuickResponseDialog.Callback, 
        LoadAttachmentsConfirmDialog.Callback, SendWithoutSubjectConfirmDialog.Callback {

    private static final String ACTION_REPLY = "com.android.email.intent.action.REPLY";
    private static final String ACTION_REPLY_ALL = "com.android.email.intent.action.REPLY_ALL";
    private static final String ACTION_FORWARD = "com.android.email.intent.action.FORWARD";
    private static final String ACTION_EDIT_DRAFT = "com.android.email.intent.action.EDIT_DRAFT";

    private static final String EXTRA_ACCOUNT_ID = "account_id";
    private static final String EXTRA_MESSAGE_ID = "message_id";
    /** If the intent is sent from the email app itself, it should have this boolean extra. */
    public static final String EXTRA_FROM_WITHIN_APP = "from_within_app";
    /** If the intent is sent from thw widget. */
    public static final String EXTRA_FROM_WIDGET = "from_widget";

    private static final String STATE_KEY_CC_SHOWN =
        "com.android.email.activity.MessageCompose.ccShown";
    private static final String STATE_KEY_QUOTED_TEXT_SHOWN =
        "com.android.email.activity.MessageCompose.quotedTextShown";
    private static final String STATE_KEY_DRAFT_ID =
        "com.android.email.activity.MessageCompose.draftId";
    private static final String STATE_KEY_LAST_SAVE_TASK_ID =
        "com.android.email.activity.MessageCompose.requestId";
    private static final String STATE_KEY_ACTION =
        "com.android.email.activity.MessageCompose.action";
    private static final String STATE_KEY_NO_RECIPIENT_ERROR =
        "com.android.email.activity.MessageCompose.recipient.error";

    private static final String[] ATTACHMENT_META_SIZE_PROJECTION = {
        OpenableColumns.SIZE
    };
    private static final int ATTACHMENT_META_SIZE_COLUMN_SIZE = 0;
    private static final String TAG = "MessageCompose";
    private boolean mIsShowRecipientError = false;

    private static final String CONTACT_URI_PREFIX = "content://com.android.contacts/contacts/";
    /**
     * A registry of the active tasks used to save messages.
     */
    private static final ConcurrentHashMap<Long, SendOrSaveMessageTask> sActiveSaveTasks =
            new ConcurrentHashMap<Long, SendOrSaveMessageTask>();

    private static long sNextSaveTaskId = 1;

    /**
     * The ID of the latest save or send task requested by this Activity.
     */
    private long mLastSaveTaskId = -1;

    private Account mAccount;

    /**
     * Flag the text changed caused by auto_bcc_myself, 
     * in this case not mark the message as changed.
     */
    private boolean mAddBccBySetting = false;

    /**
     * The contents of the current message being edited. This is not always in sync with what's
     * on the UI. {@link #updateMessage(Message, Account, boolean, boolean)} must be called to sync
     * the UI values into this object.
     */
    private Message mDraft = new Message();

    /**
     * A collection of attachments the user is currently wanting to attach to this message.
     */
    private final CopyOnWriteArrayList<Attachment> mAttachments 
            = new CopyOnWriteArrayList<Attachment>();

    /**
     * The source message for a reply, reply all, or forward. This is asynchronously loaded.
     */
    private Message mSource;

    /**
     * The attachments associated with the source attachments. Usually included in a forward.
     */
    private ArrayList<Attachment> mSourceAttachments = new ArrayList<Attachment>();

    /**
     * The action being handled by this activity. This is initially populated from the
     * {@link Intent}, but can switch between reply/reply all/forward where appropriate.
     * This value is nullable (a null value indicating a regular "compose").
     */
    private String mAction;

    private TextView mFromView;
    private MultiAutoCompleteTextView mToView;
    private MultiAutoCompleteTextView mCcView;
    private MultiAutoCompleteTextView mBccView;
    private View mCcBccContainer;
    private EditText mSubjectView;
    private EditText mMessageContentView;
    private View mAttachmentContainer;
    private ViewGroup mAttachmentContentView;
    private View mQuotedTextArea;
    private CheckBox mIncludeQuotedTextCheckBox;
    private WebView mQuotedText;
    private ActionSpinnerAdapter mActionSpinnerAdapter;

    private Controller mController;
    private boolean mDraftNeedsSaving;
    private boolean mMessageLoaded;
    private boolean mPickingAttachment = false;
    private boolean mIsBackground = false;
    private Boolean mQuickResponsesAvailable = false;
    private final EmailAsyncTask.Tracker mTaskTracker = new EmailAsyncTask.Tracker();

    private boolean mNeedResetDropDownWidth;
    private boolean mCcBccNeedResetDropDownWidth;

    private int mToOriginalOffset = Integer.MIN_VALUE;
    private int mCcOriginalOffset = Integer.MIN_VALUE;

    private AccountSpecifier mAddressAdapterTo;
    private AccountSpecifier mAddressAdapterCc;
    private AccountSpecifier mAddressAdapterBcc;

    // Add attachment request code.
    public static final int REQUEST_CODE_ATTACH_IMAGE     = 12;
    public static final int REQUEST_CODE_ATTACH_VIDEO     = 13;
    public static final int REQUEST_CODE_ATTACH_SOUND     = 14;
    public static final int REQUEST_CODE_ATTACH_CONTACT   = 15;
    public static final int REQUEST_CODE_ATTACH_FILE      = 16;
    public static final int REQUEST_CODE_ATTACH_CALENDAR  = 17;
    /**
     * @TODO:The interface between Email and Contact need be checked later.
     */
    private static final String ITEXTRA_CONTACTS = "com.mediatek.contacts.list.pickcontactsresult";
    // Add attachment dialog and adapter
    private AlertDialog.Builder mDialogBuilder = null;
    private AttachmentTypeSelectorAdapter mAttachmentTypeSelectorAdapter;
    private LoadingAttachProgressDialog mProgressDialog = null;
    private ScrollView mScrollView;
    //switch account 
    private LoaderManager mLoaderManager;
    private View mAccountSpinner;
    private AccountDropdownPopup mAccountDropdown;
    private SwitchAccountSelectorAdapter mAccountsSelectorAdapter;
    private static final int LOADER_ID_ACCOUNT_LIST = 465;
    private Cursor mCursor;

    // If the number of the recipients is more than the threshold value(currently 50),
    // then add the recipients to the view asynchronously for avoiding possible ANR
    private static final int RECIPIENT_THRES = 50;

    // Support edit quoted text.
    private TextView mEditQuotedText;

    //Support auto scroll to top for Reply/ReplyALL
    private View mRecipientsView;
    private int mRecipientsViewHeight;
    private int mRecipientsScrollCounter;
    private OnGlobalLayoutListener mGlobalLayoutListener;
    private static final int PADDING_HEIGHT_REPLY = 5;
    private static final int RECIPIENTVIEW_SHRINK_MAX_TIME = 5;

    /// M: minline for message body. @{
    private static final int MINLINE_NUMBER_LAND_MODE     = 8;
    private static final int MINLINE_NUMBER_PORTRAIT_MODE = 14;
    private static final int MINLINE_NUMBER_DEFAULT       = 1;
    /// @}

    // Flag to identify if running test case now , not save draft in this case.
    private boolean mRunTestCase = false;

    ///M: Delay 1000ms to ignore RecipientTextView trigered TextChange callback.
    ///This only used for Edit Draft Action. @{
    protected static final long DELAY_RECIPIENT_CHANGEING_TIME = 1000;
    ///@}

    public void runTestCase() {
        mRunTestCase = true;
    }

    /**
     * Watches the to, cc, bcc, subject, and message body fields.
     */
    private final TextWatcher mWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start,
                                      int before, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            if (mNeedResetDropDownWidth) {
                resetDropDownWidth();
            }

            if (mCcBccNeedResetDropDownWidth && (mCcBccContainer.getVisibility() == View.VISIBLE)) {
                resetCcBccDropDownWidth();
            }

            if ((count > 0 || before > 0) && count != before) {
                if (mAddBccBySetting) {
                    // ignore the text changed caused by auto bcc myself.
                    mAddBccBySetting = false;
                } else {
                    Logging.d(TAG, "onTextChanged .... setMessageChanged  ");
                    setMessageChanged(true);
                }
            }
        }

        @Override
        public void afterTextChanged(android.text.Editable s) { }
    };

    private static Intent getBaseIntent(Context context) {
        return new Intent(context, MessageCompose.class);
    }

    /**
     * Create an {@link Intent} that can start the message compose activity. If accountId -1,
     * the default account will be used; otherwise, the specified account is used.
     */
    public static Intent getMessageComposeIntent(Context context, long accountId) {
        Intent i = getBaseIntent(context);
        i.putExtra(EXTRA_ACCOUNT_ID, accountId);
        return i;
    }

    /**
     * Creates an {@link Intent} that can start the message compose activity from the Email
     * Widget.
     */
    public static Intent getWidgetIntent(Context context, long accountId) {
        Intent i = getBaseIntent(context);
        i.putExtra(EXTRA_ACCOUNT_ID, accountId);
        i.putExtra(EXTRA_FROM_WIDGET, true);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return i;
    }

    /**
     * Creates an {@link Intent} that can start the message compose activity from the main Email
     * activity. This should not be used for Intents to be fired from outside of the main Email
     * activity, such as from widgets, as the behavior of the compose screen differs subtly from
     * those cases.
     */
    private static Intent getMainAppIntent(Context context, long accountId) {
        Intent result = getMessageComposeIntent(context, accountId);
        result.putExtra(EXTRA_FROM_WITHIN_APP, true);
        return result;
    }

    /**
     * Compose a new message using the given account. If account is {@link Account#NO_ACCOUNT}
     * the default account will be used.
     * This should only be called from the main Email application.
     * @param context
     * @param accountId
     */
    public static void actionCompose(Context context, long accountId) {
       try {
           EmailActivity.sRecordOpening = false;
           Intent i = getMainAppIntent(context, accountId);
           context.startActivity(i);
       } catch (ActivityNotFoundException anfe) {
           // Swallow it - this is usually a race condition, especially under automated test.
           // (The message composer might have been disabled)
           Email.log(anfe.toString());
       }
    }

    /**
     * Compose a new message using a uri (mailto:) and a given account.  If account is -1 the
     * default account will be used.
     * This should only be called from the main Email application.
     * @param context
     * @param uriString
     * @param accountId
     * @return true if startActivity() succeeded
     */
    public static boolean actionCompose(Context context, String uriString, long accountId) {
        try {
            EmailActivity.sRecordOpening = false;
            Intent i = getMainAppIntent(context, accountId);
            i.setAction(Intent.ACTION_SEND);
            i.setData(Uri.parse(uriString));
            context.startActivity(i);
            return true;
        } catch (ActivityNotFoundException anfe) {
            // Swallow it - this is usually a race condition, especially under automated test.
            // (The message composer might have been disabled)
            Email.log(anfe.toString());
            return false;
        }
    }

    /**
     * Compose a new message as a reply to the given message. If replyAll is true the function
     * is reply all instead of simply reply.
     * @param context
     * @param messageId
     * @param replyAll
     */
    public static void actionReply(Context context, long messageId, boolean replyAll) {
        EmailActivity.sRecordOpening = false;
        startActivityWithMessage(context, replyAll ? ACTION_REPLY_ALL : ACTION_REPLY, messageId);
    }

    /**
     * Compose a new message as a forward of the given message.
     * @param context
     * @param messageId
     */
    public static void actionForward(Context context, long messageId) {
        EmailActivity.sRecordOpening = false;
        startActivityWithMessage(context, ACTION_FORWARD, messageId);
    }

    /**
     * Continue composition of the given message. This action modifies the way this Activity
     * handles certain actions.
     * Save will attempt to replace the message in the given folder with the updated version.
     * Discard will delete the message from the given folder.
     * @param context
     * @param messageId the message id.
     */
    public static void actionEditDraft(Context context, long messageId) {
        EmailActivity.sRecordOpening = false;
        startActivityWithMessage(context, ACTION_EDIT_DRAFT, messageId);
    }

    /**
     * Starts a compose activity with a message as a reference message .
     * Note: Only used email inner app (e.g. for edit draft\reply\forward)
     */
    private static void startActivityWithMessage(Context context, String action, long messageId) {
        Intent i = getBaseIntent(context);
        i.putExtra(EXTRA_MESSAGE_ID, messageId);
        //M: add flag start from Email app itself.@{
        i.putExtra(EXTRA_FROM_WITHIN_APP, true);
        //@}
        i.setAction(action);
        context.startActivity(i);
    }

    private boolean setAccount(Intent intent) {
        long accountId = intent.getLongExtra(EXTRA_ACCOUNT_ID, -1);
        Account account = null;
        if (accountId != Account.NO_ACCOUNT) {
            account = Account.restoreAccountWithId(this, accountId);
        }
        if (accountId == Account.NO_ACCOUNT || account == null) {
            accountId = Account.getDefaultAccountId(this);
            if (accountId != Account.NO_ACCOUNT) {
                account = Account.restoreAccountWithId(this, accountId);
            }
        }
        if (account == null) {
            // There are no accounts set up. This should not have happened. Prompt the
            // user to set up an account as an acceptable bailout.
            Email.setStartComposeAfterSetupAccountFlag(true);
            Email.setSetupAccountFinishedFlag(false);
            Email.setStartComposeIntent(getIntent());
            Logging.d(TAG, "No account Email need setup a new account for ACTION: " + mAction);
            Welcome.actionStart(this);
            finish();
            return false;
        } else {
            setAccount(account);
        }
        
        return true;
    }

    private void setAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException();
        }
        mAccount = account;
        mFromView.setText(account.mEmailAddress);
        mAddressAdapterTo
                .setAccount(new android.accounts.Account(account.mEmailAddress, "unknown"));
        mAddressAdapterCc
                .setAccount(new android.accounts.Account(account.mEmailAddress, "unknown"));
        mAddressAdapterBcc
                .setAccount(new android.accounts.Account(account.mEmailAddress, "unknown"));

        new QuickResponseChecker(mTaskTracker).executeParallel((Void) null);
    }

    private int getMaxWidthPortraitMode() {
        return getWindowManager().getDefaultDisplay().getWidth() / 3;
    }

    private int getMaxWidthLandMode() {
        return getWindowManager().getDefaultDisplay().getHeight() / 2;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mNeedResetDropDownWidth = true;
        mCcBccNeedResetDropDownWidth = true;
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //set the MaxWidth() for the mEidtQuotedText in the portrait mode
            mEditQuotedText.setMaxWidth(getMaxWidthPortraitMode());
        } else {
            //set the MaxWidth() for the mEidtQuotedText in the land mode
            mEditQuotedText.setMaxWidth(getMaxWidthLandMode());
        }
        mAccountDropdown.dismiss();

        /// M: refresh the context view style. @{
        setContentViewStyle(mMessageContentView.isFocused());
        /// @}
        /// M: Clear recipientsView's popup view. @{
        if (mToView.isFocused() && mToView.isPopupShowing()) {
            mToView.dismissDropDown();
        }
        if (mCcView.isFocused() && mCcView.isPopupShowing()) {
            mCcView.dismissDropDown();
        }
        if (mBccView.isFocused() && mBccView.isPopupShowing()) {
            mBccView.dismissDropDown();
        }
        /// @}
    }

    /**
     * M: Set message context style:
     * 1) If focused, show multi-line mode and scroll to top.
     * 2) Not focused, show one line if empty.
     */
    private void setContentViewStyle(boolean focus) {
        Editable context = mMessageContentView.getText();
        if (focus) {
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                mMessageContentView.setMinLines(MINLINE_NUMBER_PORTRAIT_MODE);
            } else {
                mMessageContentView.setMinLines(MINLINE_NUMBER_LAND_MODE);
            }
            scrollViewTop(mMessageContentView);
        } else {
            unRegisteGlobalLayoutListener();
            if (context == null || TextUtils.isEmpty(context.toString())) {
                mMessageContentView.setMinLines(MINLINE_NUMBER_DEFAULT);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHelper.debugSetWindowFlags(this);
        setContentView(R.layout.message_compose);

        mController = Controller.getInstance(getApplication());
        initViews();
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT){
            //set the MaxWidth() for the mEidtQuotedText in the portrait mode
            mEditQuotedText.setMaxWidth(getMaxWidthPortraitMode());
        } else {
            //set the MaxWidth() for the mEidtQuotedText in the land mode
            mEditQuotedText.setMaxWidth(getMaxWidthLandMode());
        }

        // Show the back arrow on the action bar.
        getActionBar().setDisplayOptions(
                ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);

        if (savedInstanceState != null) {
            long draftId = savedInstanceState.getLong(STATE_KEY_DRAFT_ID, Message.NOT_SAVED);
            long existingSaveTaskId = savedInstanceState.getLong(STATE_KEY_LAST_SAVE_TASK_ID, -1);
            setAction(savedInstanceState.getString(STATE_KEY_ACTION));
            SendOrSaveMessageTask existingSaveTask = sActiveSaveTasks.get(existingSaveTaskId);

            if ((draftId != Message.NOT_SAVED) || (existingSaveTask != null)) {
                // Restoring state and there was an existing message saved or in the process of
                // being saved.
                resumeDraft(draftId, existingSaveTask, false /* don't restore views */);
            } else {
                // Restoring state but there was nothing saved - probably means the user rotated
                // the device immediately - just use the Intent.
                resolveIntent(getIntent());
            }
        } else {
            Intent intent = getIntent();
            setAction(intent.getAction());
            resolveIntent(intent);
        }
        //init switch account function. Not switch account for reply/reply all/forward action.
        if (null == mAction || (!mAction.equals(ACTION_FORWARD) && 
                !mAction.equals(ACTION_REPLY) && !mAction.equals(ACTION_REPLY_ALL))) {
            loadAccountInfo();
        } else {
            Logging.d(TAG, "Disable change account for action :" + mAction);
        }
    }

    private void resolveIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(mAction)
                || Intent.ACTION_SENDTO.equals(mAction)
                || Intent.ACTION_SEND.equals(mAction)
                || Intent.ACTION_SEND_MULTIPLE.equals(mAction)) {
            initFromIntent(intent);
            setMessageChanged(true);
            setMessageLoaded(true);
        } else if (ACTION_REPLY.equals(mAction)
                || ACTION_REPLY_ALL.equals(mAction)
                || ACTION_FORWARD.equals(mAction)) {
            long sourceMessageId = getIntent().getLongExtra(EXTRA_MESSAGE_ID, Message.NOT_SAVED);
            loadSourceMessage(sourceMessageId, true);

        } else if (ACTION_EDIT_DRAFT.equals(mAction)) {
            // Assert getIntent.hasExtra(EXTRA_MESSAGE_ID)
            long draftId = getIntent().getLongExtra(EXTRA_MESSAGE_ID, Message.NOT_SAVED);
            resumeDraft(draftId, null, true /* restore views */);

        } else {
            // Normal compose flow for a new message.
            setAccount(intent);
            addBccMySelf(mAccount);
            showCcBccFieldsIfFilled(false);
            setInitialComposeText(null, getAccountSignature(mAccount));
            setMessageLoaded(true);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // Temporarily disable onTextChanged listeners while restoring the fields
        removeListeners();
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.getBoolean(STATE_KEY_CC_SHOWN)) {
            showCcBccFields(false);
        }
        mQuotedTextArea.setVisibility(savedInstanceState.getBoolean(STATE_KEY_QUOTED_TEXT_SHOWN)
                ? View.VISIBLE : View.GONE);
        mQuotedText.setVisibility(savedInstanceState.getBoolean(STATE_KEY_QUOTED_TEXT_SHOWN)
                ? View.VISIBLE : View.GONE);
        addListeners();
        mIsShowRecipientError = savedInstanceState.getBoolean(STATE_KEY_NO_RECIPIENT_ERROR);
    }

    // needed for unit tests
    @Override
    public void setIntent(Intent intent) {
        super.setIntent(intent);
        setAction(intent.getAction());
    }

    private void setQuickResponsesAvailable(boolean quickResponsesAvailable) {
        if (mQuickResponsesAvailable != quickResponsesAvailable) {
            mQuickResponsesAvailable = quickResponsesAvailable;
            invalidateOptionsMenu();
        }
    }

    /**
     * Given an accountId and context, finds if the database has any QuickResponse
     * entries and returns the result to the Callback.
     */
    private class QuickResponseChecker extends EmailAsyncTask<Void, Void, Boolean> {
        public QuickResponseChecker(EmailAsyncTask.Tracker tracker) {
            super(tracker);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            EmailAsyncTask.printStartLog("QuickResponseChecker#doInBackground");
            boolean b = EmailContent.count(MessageCompose.this, QuickResponse.CONTENT_URI,
                    QuickResponseColumns.ACCOUNT_KEY + "=?",
                    new String[] {Long.toString(mAccount.mId)}) > 0;
            EmailAsyncTask.printStopLog("QuickResponseChecker#doInBackground");
            return b;
        }

        @Override
        protected void onSuccess(Boolean quickResponsesAvailable) {
            setQuickResponsesAvailable(quickResponsesAvailable);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        /** M: Start a new duration recording when user compose/reply/forward an email @{ */
        if (mAccount == null) {
            long sourceMessageId = getIntent().getLongExtra(EXTRA_MESSAGE_ID, Message.NOT_SAVED);
            Message message = Message.restoreMessageWithId(this, sourceMessageId);
            if (message != null) {
                DataCollectUtils.startRecord(this, message.mAccountKey, false);
            }
        } else {
            DataCollectUtils.startRecord(this, mAccount.mId, false);
        }
        /** @} */

        /** M: If user go to home screen by pressing "Home" key at MailboxSettings UI
        and then come back to Email(MailboxSettings::onResume called), we should
        record the current account being opened again when backing to EmailActivity. @{ */
        if (!EmailActivity.sEmailActivityResumed) {
            EmailActivity.sRecordOpening = true;
            DataCollectUtils.clearRecordedList();
        }
        /** @} */
        mIsBackground = false;
        mNeedResetDropDownWidth = true;
        mCcBccNeedResetDropDownWidth = true;

        // Exit immediately if the accounts list has changed (e.g. externally deleted)
        if (Email.getNotifyUiAccountsChanged()) {
            //exit immediately only when the account used for composing be deleted
            if (mAccount != null) {
                Account account = Account.restoreAccountWithId(this, mAccount.mId);
                if (account == null) {
                    Welcome.actionStart(this);
                    finish();
                    return;
                }
            }
        }

        // If activity paused and quick responses are removed/added, possibly update options menu
        if (mAccount != null) {
            new QuickResponseChecker(mTaskTracker).executeParallel((Void) null);
        }
        if (mIsShowRecipientError) {
            mToView.setError(getString(R.string.message_compose_error_no_recipients));
            mIsShowRecipientError = false;
        }
    }

    private void resetDropDownWidth() {
        runOnUiThread(new Runnable(){
            public void run() {
                int toContentWidth = findViewById(R.id.to_content).getWidth();
                int toLabelWidth = findViewById(R.id.to_label).getWidth();
                /// M: Discarding the width resetting if the view has not be prepared yet.
                if (toContentWidth == 0) {
                    return;
                }
                int offset = mToView.getDropDownHorizontalOffset();
                if (mToOriginalOffset == Integer.MIN_VALUE) {
                    mToView.setDropDownHorizontalOffset(offset - toLabelWidth);
                }
                mToView.setDropDownWidth(toContentWidth);

                mToOriginalOffset = offset;
                mNeedResetDropDownWidth = false;
            }
        });
    }

    private void resetCcBccDropDownWidth() {
        runOnUiThread(new Runnable(){
            public void run() {
                int ccContentWidth = findViewById(R.id.cc_content).getWidth();
                int ccLabelWidth = findViewById(R.id.cc_label).getWidth();
                /// M: Discarding the width resetting if the view has not be prepared yet.
                if (ccContentWidth == 0) {
                    return;
                }
                int offset = mCcView.getDropDownHorizontalOffset();
                if (mCcOriginalOffset == Integer.MIN_VALUE) {
                    mCcView.setDropDownHorizontalOffset(offset - ccLabelWidth);
                }
                mCcView.setDropDownWidth(ccContentWidth);

                int bccContentWidth = findViewById(R.id.cc_content).getWidth();
                int bccLabelWidth = findViewById(R.id.cc_label).getWidth();
                offset = mBccView.getDropDownHorizontalOffset();
                if (mCcOriginalOffset == Integer.MIN_VALUE) {
                    mBccView.setDropDownHorizontalOffset(offset - bccLabelWidth);
                }
                mBccView.setDropDownWidth(bccContentWidth);

                mCcOriginalOffset = offset;
                mCcBccNeedResetDropDownWidth = false;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        DataCollectUtils.stopRecord(this);
        EmailActivity.sEmailActivityResumed = false;

        if (mRunTestCase) {
            Logging.d(TAG, "Run testcase not save draft to database.");
            return;
        }

        mIsBackground = true;
        saveIfNeeded();
        mIsShowRecipientError = (mToView.getError() != null);
        mToView.setError(null);
    }

    /**
     * We override onDestroy to make sure that the WebView gets explicitly destroyed.
     * Otherwise it can leak native references.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mQuotedText.destroy();
        mQuotedText = null;

        mTaskTracker.cancellAllInterrupt();

        if (mAddressAdapterTo != null && mAddressAdapterTo instanceof EmailAddressAdapter) {
            ((EmailAddressAdapter) mAddressAdapterTo).close();
        }
        if (mAddressAdapterCc != null && mAddressAdapterCc instanceof EmailAddressAdapter) {
            ((EmailAddressAdapter) mAddressAdapterCc).close();
        }
        if (mAddressAdapterBcc != null && mAddressAdapterBcc instanceof EmailAddressAdapter) {
            ((EmailAddressAdapter) mAddressAdapterBcc).close();
        }

        mDialogBuilder = null;
        mAttachmentTypeSelectorAdapter = null;
    }

    /**
     * The framework handles most of the fields, but we need to handle stuff that we
     * dynamically show and hide:
     * Cc field,
     * Bcc field,
     * Quoted text,
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        long draftId = mDraft.mId;
        if (draftId != Message.NOT_SAVED) {
            outState.putLong(STATE_KEY_DRAFT_ID, draftId);
        }
        outState.putBoolean(STATE_KEY_CC_SHOWN, mCcBccContainer.getVisibility() == View.VISIBLE);
        outState.putBoolean(STATE_KEY_QUOTED_TEXT_SHOWN,
                mQuotedTextArea.getVisibility() == View.VISIBLE);
        outState.putString(STATE_KEY_ACTION, mAction);

        // If there are any outstanding save requests, ensure that it's noted in case it hasn't
        // finished by the time the activity is restored.
        outState.putLong(STATE_KEY_LAST_SAVE_TASK_ID, mLastSaveTaskId);
        outState.putBoolean(STATE_KEY_NO_RECIPIENT_ERROR, mIsShowRecipientError);
    }

    @Override
    public void onBackPressed() {
        onBack(true /* systemKey */);
    }

    /**
     * Whether or not the current message being edited has a source message (i.e. is a reply,
     * or forward) that is loaded.
     */
    private boolean hasSourceMessage() {
        return mSource != null;
    }

    /**
     * @return true if the activity was opened by the email app itself.
     */
    private boolean isOpenedFromWithinApp() {
        Intent i = getIntent();
        return (i != null && i.getBooleanExtra(EXTRA_FROM_WITHIN_APP, false));
    }

    private boolean isOpenedFromWidget() {
        Intent i = getIntent();
        return (i != null && i.getBooleanExtra(EXTRA_FROM_WIDGET, false));
    }

    /**
     * Sets message as loaded and then initializes the TextWatchers.
     * @param isLoaded - value to which to set mMessageLoaded
     */
    private void setMessageLoaded(boolean isLoaded) {
        if (mMessageLoaded != isLoaded) {
            mMessageLoaded = isLoaded;
            addListeners();
        }
    }

    private void setMessageChanged(boolean messageChanged) {
        boolean needsSaving = messageChanged && !areViewsEmpty();
        if (mDraftNeedsSaving != needsSaving) {
            mDraftNeedsSaving = needsSaving;
            invalidateOptionsMenu();
        }
    }

    /**
     * @return whether or not all text fields are empty (i.e. the entire compose message is empty)
     */
    private boolean areViewsEmpty() {
        /// M: Use a common method to check the recipient views
        return isRecipientEmpty(mToView)
                && isRecipientEmpty(mCcView)
                && isBccViewEmpty()
                && (mSubjectView.length() == 0)
                && isBodyEmpty()
                && mAttachments.isEmpty();
    }

    /**
     * M: Check if the recipient view has valid recipients
     * @param recipientView the recipient text view
     * @return true if the view has no valid recipient, else false
     */
    private boolean isRecipientEmpty(TextView recipientView) {
        String address = getPackedAddresses(recipientView);
        if (TextUtils.isEmpty(address)) {
            return true;
        }
        return false;
    }

    private boolean isBodyEmpty() {
        return (mMessageContentView.length() == 0)
                || mMessageContentView.getText()
                        .toString().equals("\n" + getAccountSignature(mAccount));
    }

    //Ignore system changed case: e.g: auto_bcc_myself.
    //TODO: What if bccView only contains one invalid address? 
    //The invalid address will not save to database as the current design.
    private boolean isBccViewEmpty() {
        /// M: Use a common method to check the BCC view
        if (isRecipientEmpty(mBccView)) {
            return true;
        } else {
            boolean bccMySelf = Preferences.getSharedPreferences(this)
                    .getBoolean(Preferences.BCC_MYSELF_KEY, Preferences.BCC_MYSELF_DEFAULT);
            String bccText = mBccView.getText().toString().trim();
            if (bccMySelf) {
                Address[] bcc = Address.parse(bccText, false);
                if (bcc.length == 1
                        && bcc[0].getAddress().equals(mAccount.mEmailAddress)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setFocusShifter(int fromViewId, final int targetViewId) {
        View label = findViewById(fromViewId); // xlarge only
        if (label != null) {
            final View target = UiUtilities.getView(this, targetViewId);
            label.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    target.requestFocus();
                }
            });
        }
    }

    /**
     * An {@link InputFilter} that implements special address cleanup rules.
     * The first space key entry following an "@" symbol that is followed by any combination
     * of letters and symbols, including one+ dots and zero commas, should insert an extra
     * comma (followed by the space).
     */
    @VisibleForTesting
    static final InputFilter RECIPIENT_FILTER = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                int dstart, int dend) {

            // Quick check - did they enter a single space?
            if (end-start != 1 || source.charAt(start) != ' ') {
                return null;
            }

            // determine if the characters before the new space fit the pattern
            // follow backwards and see if we find a comma, dot, or @
            int scanBack = dstart;
            boolean dotFound = false;
            while (scanBack > 0) {
                char c = dest.charAt(--scanBack);
                switch (c) {
                    case '.':
                        dotFound = true;    // one or more dots are req'd
                        break;
                    case ',':
                        return null;
                    case '@':
                        if (!dotFound) {
                            return null;
                        }

                        // we have found a comma-insert case.  now just do it
                        // in the least expensive way we can.
                        if (source instanceof Spanned) {
                            SpannableStringBuilder sb = new SpannableStringBuilder(",");
                            sb.append(source);
                            return sb;
                        } else {
                            return ", ";
                        }
                    default:
                        // just keep going
                }
            }

            // no termination cases were found, so don't edit the input
            return null;
        }
    };

    private void initViews() {
        ViewGroup toParent = UiUtilities.getViewOrNull(this, R.id.to_content);
        if (toParent != null) {
            mToView = (MultiAutoCompleteTextView) toParent.findViewById(R.id.to);
            ViewGroup ccParent;
            ViewGroup bccParent;
            ccParent = (ViewGroup) findViewById(R.id.cc_content);
            mCcView = (MultiAutoCompleteTextView) ccParent.findViewById(R.id.cc);
            bccParent = (ViewGroup) findViewById(R.id.bcc_content);
            mBccView = (MultiAutoCompleteTextView) bccParent.findViewById(R.id.bcc);
        } else {
            mToView = UiUtilities.getView(this, R.id.to);
            mCcView = UiUtilities.getView(this, R.id.cc);
            mBccView = UiUtilities.getView(this, R.id.bcc);
        }

        mScrollView = UiUtilities.getView(this, R.id.compose_scroll);
        mFromView = UiUtilities.getView(this, R.id.from);
        mCcBccContainer = UiUtilities.getView(this, R.id.cc_bcc_wrapper);
        mSubjectView = UiUtilities.getView(this, R.id.subject);
        mMessageContentView = UiUtilities.getView(this, R.id.body_text);
        mAttachmentContentView = UiUtilities.getView(this, R.id.attachments);
        mAttachmentContainer = UiUtilities.getView(this, R.id.attachment_container);
        mQuotedTextArea = UiUtilities.getView(this, R.id.quoted_text_area);
        mIncludeQuotedTextCheckBox = UiUtilities.getView(this, R.id.include_quoted_text);
        mQuotedText = UiUtilities.getView(this, R.id.quoted_text);
        mQuotedText.setOverScrollMode(View.OVER_SCROLL_NEVER);

        InputFilter[] recipientFilters = new InputFilter[] { RECIPIENT_FILTER };

        // NOTE: assumes no other filters are set
        mToView.setFilters(recipientFilters);
        mCcView.setFilters(recipientFilters);
        mBccView.setFilters(recipientFilters);

        //Edit quoted text
        mEditQuotedText = (TextView)findViewById(R.id.edit_quoted_text);
        mEditQuotedText.setOnClickListener(this);

        /*
         * We set this to invisible by default. Other methods will turn it back on if it's
         * needed.
         */
        mQuotedTextArea.setVisibility(View.GONE);
        setIncludeQuotedText(false, false);

        mIncludeQuotedTextCheckBox.setOnClickListener(this);

        EmailAddressValidator addressValidator = new EmailAddressValidator();

        setupAddressAdapters();
        mToView.setTokenizer(new Rfc822Tokenizer());
        mToView.setValidator(addressValidator);

        mCcView.setTokenizer(new Rfc822Tokenizer());
        mCcView.setValidator(addressValidator);

        mBccView.setTokenizer(new Rfc822Tokenizer());
        mBccView.setValidator(addressValidator);

        final View addCcBccView = UiUtilities.getViewOrNull(this, R.id.add_cc_bcc);
        if (addCcBccView != null) {
            // Tablet view.
            addCcBccView.setOnClickListener(this);
        }

        final View addAttachmentView = UiUtilities.getViewOrNull(this, R.id.add_attachment);
        if (addAttachmentView != null) {
            // Tablet view.
            addAttachmentView.setOnClickListener(this);
        }

        setFocusShifter(R.id.to_label, R.id.to);
        setFocusShifter(R.id.cc_label, R.id.cc);
        setFocusShifter(R.id.bcc_label, R.id.bcc);
        setFocusShifter(R.id.composearea_tap_trap_bottom, R.id.body_text);

        mMessageContentView.setOnFocusChangeListener(this);

        updateAttachmentContainer();
        mToView.requestFocus();
        
        // switch account drop down view
        mLoaderManager = this.getLoaderManager();
        mAccountsSelectorAdapter = new SwitchAccountSelectorAdapter(this);
        mAccountDropdown = new AccountDropdownPopup(this);
        mAccountDropdown.setAdapter(mAccountsSelectorAdapter);
        mAccountSpinner = UiUtilities.getView(this, R.id.account_spinner);
        //init input text length filter.
        initLengthFilter();
        mRecipientsView = findViewById(R.id.compose_recipients_wrapper);
    }

    /**
     * Initializes listeners. Should only be called once initializing of views is complete to
     * avoid unnecessary draft saving.
     */
    private void addListeners() {
        mToView.addTextChangedListener(mWatcher);
        mCcView.addTextChangedListener(mWatcher);
        mBccView.addTextChangedListener(mWatcher);
        mSubjectView.addTextChangedListener(mWatcher);
        mMessageContentView.addTextChangedListener(mWatcher);
    }
    
    /**
     * This function used to remove the error notification of mToView.
     * The notification of 'You must add at least one recipient' should
     * be remove when change the action to REPLY or REPLY ALL
     */
    private void removeErroInfo() {
        if (Objects.equal(ACTION_REPLY, mAction) || Objects.equal(ACTION_REPLY_ALL, mAction)) {
            mToView.setError(null);
        }
    }

    /**
     * Removes listeners from the user-editable fields. Can be used to temporarily disable them
     * while resetting fields (such as when changing from reply to reply all) to avoid
     * unnecessary saving.
     */
    private void removeListeners() {
        mToView.removeTextChangedListener(mWatcher);
        mCcView.removeTextChangedListener(mWatcher);
        mBccView.removeTextChangedListener(mWatcher);
        mSubjectView.removeTextChangedListener(mWatcher);
        mMessageContentView.removeTextChangedListener(mWatcher);
    }

    /**
     * Set up address auto-completion adapters.
     */
    private void setupAddressAdapters() {
        boolean supportsChips = ChipsUtil.supportsChipsUi();

        if (supportsChips && mToView instanceof ChipsAddressTextView) {
            mAddressAdapterTo = new RecipientAdapter(this, (ChipsAddressTextView) mToView);
            mToView.setAdapter((RecipientAdapter) mAddressAdapterTo);
            ((ChipsAddressTextView)mToView).setGalSearchDelayer();
        } else {
            mAddressAdapterTo = new EmailAddressAdapter(this);
            mToView.setAdapter((EmailAddressAdapter) mAddressAdapterTo);
        }
        if (supportsChips && mCcView instanceof ChipsAddressTextView) {
            mAddressAdapterCc = new RecipientAdapter(this, (ChipsAddressTextView) mCcView);
            mCcView.setAdapter((RecipientAdapter) mAddressAdapterCc);
            ((ChipsAddressTextView)mToView).setGalSearchDelayer();
        } else {
            mAddressAdapterCc = new EmailAddressAdapter(this);
            mCcView.setAdapter((EmailAddressAdapter) mAddressAdapterCc);
        }
        if (supportsChips && mBccView instanceof ChipsAddressTextView) {
            mAddressAdapterBcc = new RecipientAdapter(this, (ChipsAddressTextView) mBccView);
            mBccView.setAdapter((RecipientAdapter) mAddressAdapterBcc);
            ((ChipsAddressTextView)mToView).setGalSearchDelayer();
        } else {
            mAddressAdapterBcc = new EmailAddressAdapter(this);
            mBccView.setAdapter((EmailAddressAdapter) mAddressAdapterBcc);
        }
    }

    /** M: Modify the flag read of draft @{ */
    private void updateReadFlag(Message message) {
        new EmailAsyncTask<Message, Void, Void>(null) {
            @Override
            protected Void doInBackground(Message... params) {
                Message message = params[0];
                ContentValues cv = new ContentValues();
                cv.put(MessageColumns.FLAG_READ, true);
                message.update(getApplicationContext(), cv);
                return null;
            }
        }.executeParallel(message);
    }
    /** @} */

    /**
     * Asynchronously loads a draft message for editing.
     * This may or may not restore the view contents, depending on whether or not callers want,
     * since in the case of screen rotation, those are restored automatically.
     */
    private void resumeDraft(
            long draftId,
            SendOrSaveMessageTask existingSaveTask,
            final boolean restoreViews) {
        // Note - this can be Message.NOT_SAVED if there is an existing save task in progress
        // for the draft we need to load.
        mDraft.mId = draftId;

        new LoadMessageTask(draftId, existingSaveTask, new OnMessageLoadHandler() {
            @Override
            public void onMessageLoaded(Message message, Body body) {
                message.mHtml = body.mHtmlContent;
                message.mText = body.mTextContent;
                message.mHtmlReply = body.mHtmlReply;
                message.mTextReply = body.mTextReply;
                message.mIntroText = body.mIntroText;
                message.mSourceKey = body.mSourceKey;
                /** M: Using for modifying the read flag of draft @{ */
                if (!message.mFlagRead) {
                    updateReadFlag(message);
                }
                /** @} */
                mDraft = message;
                processDraftMessage(message, restoreViews);

                // Load attachments related to the draft.
                loadAttachments(message.mId, mAccount, new AttachmentLoadedCallback() {
                    @Override
                    public void onAttachmentLoaded(Attachment[] attachments) {
                        if(null != attachments && attachments.length >0){
                            List<Attachment> attachList = new ArrayList<Attachment>();
                            for (Attachment attachment: attachments) {
                                attachList.add(attachment);
                            }
                            addAttachments(attachList);
                        }
                    }
                });

                ///M: Delay 1000ms to ignore RecipientTextView trigered TextChange callback.
                ///It will cause save draft operation even the mail not changed.
                ///This only used for Edit Draft Action. @{
                Utility.getMainThreadHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Logging.d(TAG, "postDelayed 1000ms for Edit draft action, to ignore TextChange" +
                                 " callback trigered by RecipientView ");
                        setMessageChanged(false);
                    }
                }, DELAY_RECIPIENT_CHANGEING_TIME);
                ///@}

                // If we're resuming an edit of a reply, reply-all, or forward, re-load the
                // source message if available so that we get more information.
                //if (message.mSourceKey != Message.NOT_SAVED) {
                    // Not use ICS change action new feature, it will cause the original information
                    // lost when opening a draft types of Reply|Reply All.
                    // loadSourceMessage(message.mSourceKey, false /* restore views */);
                //}
            }

            @Override
            public void onLoadFailed() {
                Utility.showToast(MessageCompose.this, R.string.error_loading_message_body);
                finish();
            }
        }).executeParallel((Void[]) null);
    }

    @VisibleForTesting
    void processDraftMessage(Message message, boolean restoreViews) {
        if (restoreViews) {
            mSubjectView.setText(message.mSubject);
            Address[] to = Address.unpack(message.mTo);
            addAddresses(mToView, to);
            Address[] cc = Address.unpack(message.mCc);
            if (cc.length > 0) {
                addAddresses(mCcView, cc);
            }
            Address[] bcc = Address.unpack(message.mBcc);
            if (bcc.length > 0) {
                addAddresses(mBccView, bcc);
            }

            mMessageContentView.setText(message.mText);
            addBccMySelf(mAccount);
            showCcBccFieldsIfFilled(false);
            setNewMessageFocus();
        }
        setMessageChanged(false);

        // The quoted text must always be restored.
        displayQuotedText(message.mId, message.mTextReply, message.mHtmlReply);
        setIncludeQuotedText(
                (mDraft.mFlags & Message.FLAG_NOT_INCLUDE_QUOTED_TEXT) == 0, false);
    }

    /**
     * Asynchronously loads a source message (to be replied or forwarded in this current view),
     * populating text fields and quoted text fields when the load finishes, if requested.
     */
    private void loadSourceMessage(long sourceMessageId, final boolean restoreViews) {
        new LoadMessageTask(sourceMessageId, null, new OnMessageLoadHandler() {
            @Override
            public void onMessageLoaded(Message message, Body body) {
                message.mHtml = body.mHtmlContent;
                message.mText = body.mTextContent;
                message.mHtmlReply = null;
                message.mTextReply = null;
                message.mIntroText = null;
                mSource = message;
                mSourceAttachments = new ArrayList<Attachment>();

                if (restoreViews) {
                    setInitialComposeText(null, getAccountSignature(mAccount));
                    processSourceMessage(mSource, mAccount);
                    setMessageChanged(true);
                }

                loadAttachments(message.mId, mAccount, new AttachmentLoadedCallback() {
                    @Override
                    public void onAttachmentLoaded(Attachment[] attachments) {
                        final boolean supportsSmartForward =
                            (mAccount.mFlags & Account.FLAGS_SUPPORTS_SMART_FORWARD) != 0;
                        Logging.d(TAG, "loadSourceMessage loadAttachments onAttachmentLoaded "
                                + "supportsSmartForward = " +supportsSmartForward );
                        // Process the attachments to have the appropriate smart forward flags.
                        for (Attachment attachment : attachments) {
                            if (supportsSmartForward) {
                                attachment.mFlags |= Attachment.FLAG_SMART_FORWARD;
                                /// M: The FLAG_DOWNLOAD_USER_REQUEST flag should be removed
                                /// because the mail can use smart forward
                                attachment.mFlags &= ~Attachment.FLAG_DOWNLOAD_USER_REQUEST;
                            }
                            mSourceAttachments.add(attachment);
                        }
                        if (isForward() && restoreViews) {
                            Logging.d(TAG, "loadSourceMessage loadAttachments "
                                    + "processSourceMessageAttachments");
                            if (processSourceMessageAttachments(
                                    mAttachments, mSourceAttachments, true)) {
                                updateAttachmentUi();
                                setMessageChanged(true);
                            }
                        }
                    }
                });

                if (mAction.equals(ACTION_EDIT_DRAFT)) {
                    // Resuming a draft may in fact be resuming a reply/reply all/forward.
                    // Use a best guess and infer the action here.
                    String inferredAction = inferAction();
                    Logging.d(TAG, "ACTION_EDIT_DRAFT action inferredAction result : "
                            + inferredAction);
                    if (inferredAction != null) {
                        setAction(inferredAction);
                        // No need to update the action selector as switching actions should do it.
                        return;
                    }
                }

                updateActionSelector();
            }

            @Override
            public void onLoadFailed() {
                // The loading of the source message is only really required if it is needed
                // immediately to restore the view contents. In the case of resuming draft, it
                // is only needed to gather additional information.
                if (restoreViews) {
                    Utility.showToast(MessageCompose.this, R.string.error_loading_message_body);
                    finish();
                }
            }
        }).executeParallel((Void[]) null);
    }

    /**
     * Infers whether or not the current state of the message best reflects either a reply,
     * reply-all, or forward.
     */
    @VisibleForTesting
    String inferAction() {
        String subject = mSubjectView.getText().toString();
        if (subject == null) {
            return null;
        }
        //Add the language support for subject prefix. 
        String prefixFWD = getForwardSubjectPrefix() + ":";
        String prefixRep = getReplyOrReplyAllSubjectPrefix() + ":";
        String subjectLowerCase = subject.toLowerCase();
        if (subjectLowerCase.startsWith("fwd:")
                || subjectLowerCase.startsWith(prefixFWD)
                || subject.startsWith(prefixFWD)) {
            return ACTION_FORWARD;
        } else if (subjectLowerCase.startsWith("re:")
                || subjectLowerCase.startsWith(prefixRep)
                || subject.startsWith(prefixRep)) {
            int numRecipients = getAddresses(mToView).length
                    + getAddresses(mCcView).length
                    + getAddresses(mBccView).length;
            if (numRecipients > 1) {
                return ACTION_REPLY_ALL;
            } else {
                return ACTION_REPLY;
            }
        } else {
            // Others for Edit draft
            return ACTION_EDIT_DRAFT;
        }
    }

    private interface OnMessageLoadHandler {
        /**
         * Handles a load to a message (e.g. a draft message or a source message).
         */
        void onMessageLoaded(Message message, Body body);

        /**
         * Handles a failure to load a message.
         */
        void onLoadFailed();
    }

    /**
     * Asynchronously loads a message and the account information.
     * This can be used to load a reference message (when replying) or when restoring a draft.
     */
    private class LoadMessageTask extends EmailAsyncTask<Void, Void, Object[]> {
        /**
         * The message ID to load, if available.
         */
        private long mMessageId;

        /**
         * A future-like reference to the save task which must complete prior to this load.
         */
        private final SendOrSaveMessageTask mSaveTask;

        /**
         * A callback to pass the results of the load to.
         */
        private final OnMessageLoadHandler mCallback;

        public LoadMessageTask(
                long messageId, SendOrSaveMessageTask saveTask, OnMessageLoadHandler callback) {
            super(mTaskTracker);
            mMessageId = messageId;
            mSaveTask = saveTask;
            mCallback = callback;
        }

        private long getIdToLoad() throws InterruptedException, ExecutionException {
            if (mMessageId == -1) {
                mMessageId = mSaveTask.get();
            }
            return mMessageId;
        }

        @Override
        protected Object[] doInBackground(Void... params) {
            Logging.d(">>>> EmailAsyncTask#excuteParallel LoadMessageTask#doInBackground");
            long messageId;
            try {
                messageId = getIdToLoad();
            } catch (InterruptedException e) {
                // Don't have a good message ID to load - bail.
                Logging.e(TAG,
                        "Unable to load draft message since existing save task failed: " + e);
                return null;
            } catch (ExecutionException e) {
                // Don't have a good message ID to load - bail.
                Logging.e(TAG,
                        "Unable to load draft message since existing save task failed: " + e);
                return null;
            }
            Message message = Message.restoreMessageWithId(MessageCompose.this, messageId);
            if (message == null) {
                return null;
            }
            long accountId = message.mAccountKey;
            Account account = Account.restoreAccountWithId(MessageCompose.this, accountId);
            Body body;
            try {
                body = Body.restoreBodyWithMessageId(MessageCompose.this, message.mId);
            } catch (RuntimeException e) {
                Logging.d(TAG, "Exception while loading message body: " + e);
                return null;
            }
            Logging.d("<<<< EmailAsyncTask#excuteParallel LoadMessageTask#doInBackground");
            return new Object[] {message, body, account};
        }

        @Override
        protected void onSuccess(Object[] results) {
            if ((results == null) || (results.length != 3)) {
                mCallback.onLoadFailed();
                return;
            }

            final Message message = (Message) results[0];
            final Body body = (Body) results[1];
            final Account account = (Account) results[2];
            if ((message == null) || (body == null) || (account == null)) {
                mCallback.onLoadFailed();
                return;
            }

            setAccount(account);
            mCallback.onMessageLoaded(message, body);
            setMessageLoaded(true);
        }
    }

    private interface AttachmentLoadedCallback {
        /**
         * Handles completion of the loading of a set of attachments.
         * Callback will always happen on the main thread.
         */
        void onAttachmentLoaded(Attachment[] attachment);
    }

    private void loadAttachments(
            final long messageId,
            final Account account,
            final AttachmentLoadedCallback callback) {
        new EmailAsyncTask<Void, Void, Attachment[]>(mTaskTracker) {
            @Override
            protected Attachment[] doInBackground(Void... params) {
                Logging.d(">>>> EmailAsyncTask#excuteParallel MessageCompose#loadAttachments");
                Attachment[] atts = Attachment.restoreAttachmentsWithMessageId(
                        MessageCompose.this, messageId);
                Logging.d("<<<< EmailAsyncTask#excuteParallel MessageCompose#loadAttachments");
                return atts;
            }

            @Override
            protected void onSuccess(Attachment[] attachments) {
                if (attachments == null) {
                    attachments = new Attachment[0];
                }
                callback.onAttachmentLoaded(attachments);
            }
        }.executeParallel((Void[]) null);
    }

    @Override
    public void onFocusChange(View view, boolean focused) {
        switch (view.getId()) {
        case R.id.body_text:
            /// M: change the message context view style: single-line or multi-line. @{
            setContentViewStyle(focused);
            /// @}
            if (focused) {
                // When focusing on the message content via tabbing to it, or
                // other means of auto focusing, move the cursor to the end of the body (before
                // the signature).
                if (mMessageContentView.getSelectionStart() == 0
                        && mMessageContentView.getSelectionEnd() == 0) {
                    // There is no way to determine if the focus change was
                    // programmatic or due to keyboard event, or if it was due to a tap/restore. Use
                    // a best-guess by using the fact that auto-focus/keyboard tabs set the
                    // selection to 0.
                    setMessageContentSelection(getAccountSignature(mAccount));
                }
            }
        }
    }

    private static void addAddresses(MultiAutoCompleteTextView view, Address[] addresses) {
        if (addresses == null) {
            return;
        }
        for (Address address : addresses) {
            addAddress(view, address.toString());
        }
    }

    /**
     * M: Use this method to limit the number of recipients, when adding hundreds
     * of recipients at one time.
     * Note: After finishing this operation, you can also add extra recipients until the
     * whole to/cc/bcc string more than the LengthFilter.
     */
    private void addAddresses(MultiAutoCompleteTextView view, String[] addresses) {
        if (addresses == null) {
            return;
        }
        int count = 0;
        for (String oneAddress : addresses) {
            if (count >= Email.RECIPIENT_MAX_NUMBER) {
                Utility.showToast(this, getString(R.string.not_add_more_recipients,
                        Email.RECIPIENT_MAX_NUMBER));
                Logging.d(TAG,
                        "Not add more recipient, added address length is "
                                + addresses.length);
                return;
            }
            addAddress(view, oneAddress);
            count++;
        }
    }

    private static void addAddress(MultiAutoCompleteTextView view, String address) {
        view.append((address != null ? address.trim() : address) + ", ");
    }

    private static String getPackedAddresses(TextView view) {
        Address[] addresses = Address.parse(view.getText().toString().trim(), false);
        return Address.pack(addresses);
    }

    private static Address[] getAddresses(TextView view) {
        Address[] addresses = Address.parse(view.getText().toString().trim(), false);
        return addresses;
    }

    /*
     * Computes a short string indicating the destination of the message based on To, Cc, Bcc.
     * If only one address appears, returns the friendly form of that address.
     * Otherwise returns the friendly form of the first address appended with "and N others".
     */
    private String makeDisplayName(String packedTo, String packedCc, String packedBcc) {
        Address first = null;
        int nRecipients = 0;
        for (String packed: new String[] {packedTo, packedCc, packedBcc}) {
            Address[] addresses = Address.unpack(packed);
            nRecipients += addresses.length;
            if (first == null && addresses.length > 0) {
                first = addresses[0];
            }
        }
        if (nRecipients == 0) {
            return "";
        }
        String friendly = first.toFriendly();
        if (nRecipients == 1) {
            return friendly;
        }
        return this.getString(R.string.message_compose_display_name, friendly, nRecipients - 1);
    }

    private ContentValues getUpdateContentValues(Message message) {
        ContentValues values = new ContentValues();
        values.put(MessageColumns.TIMESTAMP, message.mTimeStamp);
        values.put(MessageColumns.FROM_LIST, message.mFrom);
        values.put(MessageColumns.TO_LIST, message.mTo);
        values.put(MessageColumns.CC_LIST, message.mCc);
        values.put(MessageColumns.BCC_LIST, message.mBcc);
        values.put(MessageColumns.SUBJECT, message.mSubject);
        values.put(MessageColumns.DISPLAY_NAME, message.mDisplayName);
        values.put(MessageColumns.FLAG_READ, message.mFlagRead);
        values.put(MessageColumns.FLAG_LOADED, message.mFlagLoaded);
        values.put(MessageColumns.FLAG_ATTACHMENT, message.mFlagAttachment);
        values.put(MessageColumns.FLAGS, message.mFlags);
        /** M: The below values also should be updated @{ */
        values.put(MessageColumns.ACCOUNT_KEY, message.mAccountKey);
        values.put(MessageColumns.MAILBOX_KEY, message.mMailboxKey);
        values.put(MessageColumns.SNIPPET, message.mSnippet);
        /** @} */
        return values;
    }

    /**
     * Updates the given message using values from the compose UI.
     *
     * @param message The message to be updated.
     * @param account the account (used to obtain From: address).
     * @param hasAttachments true if it has one or more attachment.
     * @param sending set true if the message is about to sent, in which case we perform final
     *        clean up;
     */
    private void updateMessage(Message message, Account account, boolean hasAttachments,
            boolean sending) {
        if (message.mMessageId == null || message.mMessageId.length() == 0) {
            message.mMessageId = Utility.generateMessageId();
        }
        message.mTimeStamp = System.currentTimeMillis();
        message.mFrom = new Address(account.getEmailAddress(), account.getSenderName()).pack();
        message.mTo = getPackedAddresses(mToView);
        message.mCc = getPackedAddresses(mCcView);
        message.mBcc = getPackedAddresses(mBccView);
        message.mSubject = mSubjectView.getText().toString();
        message.mText = mMessageContentView.getText().toString();
        /** M: The mSnippet should also be updated @{ */
        if (message.mText != null) {
            message.mSnippet = TextUtilities.makeSnippetFromPlainText(message.mText);
        } else {
            message.mSnippet = null;
        }
        /** @} */
        message.mAccountKey = account.mId;
        /** M: The account may be switched, so update the mailbox key here @{ */
        message.mMailboxKey = mController.findOrCreateMailboxOfType(account.mId, Mailbox.TYPE_DRAFTS);
        /** @} */
        message.mDisplayName = makeDisplayName(message.mTo, message.mCc, message.mBcc);
        message.mFlagRead = true;
        message.mFlagLoaded = Message.FLAG_LOADED_COMPLETE;
        message.mFlagAttachment = hasAttachments;
        // Use the Intent to set flags saying this message is a reply or a forward and save the
        // unique id of the source message
        if (mSource != null ) {
            // we need set source key no matter mQuotedTex is null or not.
            message.mSourceKey = mSource.mId;
            message.mFlags |= isForward() ? Message.FLAG_TYPE_FORWARD
                    : Message.FLAG_TYPE_REPLY;
            if (mQuotedTextArea.getVisibility() == View.VISIBLE) {
                // If the quote bar is visible; this must either be a reply or forward
                // Get the body of the source message here
                message.mHtmlReply = mSource.mHtml;
                message.mTextReply = mSource.mText;
                String fromAsString = Address.unpackToString(mSource.mFrom);
                if (isForward()) {
                    String subject = mSource.mSubject;
                    String to = Address.unpackToString(mSource.mTo);
                    String cc = Address.unpackToString(mSource.mCc);
                    message.mIntroText =
                        getString(R.string.message_compose_fwd_header_fmt, subject, fromAsString,
                                to != null ? to : "", cc != null ? cc : "");
                } else {
                    message.mIntroText =
                        getString(R.string.message_compose_reply_header_fmt, fromAsString);
                }
            }
        }

        if (includeQuotedText()) {
            message.mFlags &= ~Message.FLAG_NOT_INCLUDE_QUOTED_TEXT;
        } else {
            message.mFlags |= Message.FLAG_NOT_INCLUDE_QUOTED_TEXT;
            if (sending) {
                // If we are about to send a message, and not including the original message,
                // clear the related field.
                // We can't do this until the last minutes, so that the user can change their
                // mind later and want to include it again.
                mDraft.mIntroText = null;
                mDraft.mTextReply = null;
                mDraft.mHtmlReply = null;

                // Note that mSourceKey is not cleared out as this is still considered a
                // reply/forward.
            }
        }
    }

    private class SendOrSaveMessageTask extends EmailAsyncTask<Void, Void, Long> {
        private final boolean mSend;
        private final long mTaskId;

        /** A context that will survive even past activity destruction. */
        private final Context mContext;

        public SendOrSaveMessageTask(long taskId, boolean send) {
            super(null /* DO NOT cancel in onDestroy */);
            /**
             * /// M: Comment the below block out for testing tools.
             * if (send && ActivityManager.isUserAMonkey()) {
             *     Logging.d(TAG, "Inhibiting send while monkey is in charge.");
             *     send = false;
             * }
             */
            mTaskId = taskId;
            mSend = send;
            mContext = getApplicationContext();

            sActiveSaveTasks.put(mTaskId, this);
        }

        @Override
        protected Long doInBackground(Void... params) {
            synchronized (mDraft) {
                updateMessage(mDraft, mAccount, mAttachments.size() > 0, mSend);
                ContentResolver resolver = getContentResolver();
                if (mDraft.isSaved()) {
                    // Update the message
                    Uri draftUri =
                        ContentUris.withAppendedId(Message.SYNCED_CONTENT_URI, mDraft.mId);
                    resolver.update(draftUri, getUpdateContentValues(mDraft), null, null);
                    // Update the body
                    ContentValues values = new ContentValues();
                    values.put(BodyColumns.TEXT_CONTENT, mDraft.mText);
                    values.put(BodyColumns.TEXT_REPLY, mDraft.mTextReply);
                    values.put(BodyColumns.HTML_REPLY, mDraft.mHtmlReply);
                    values.put(BodyColumns.INTRO_TEXT, mDraft.mIntroText);
                    values.put(BodyColumns.SOURCE_MESSAGE_KEY, mDraft.mSourceKey);
                    Body.updateBodyWithMessageId(MessageCompose.this, mDraft.mId, values);
                } else {
                    // mDraft.mId is set upon return of saveToMailbox()
                    mController.saveToMailbox(mDraft, Mailbox.TYPE_DRAFTS);
                }
                // For any unloaded attachment, set the flag saying we need it loaded
                boolean hasUnloadedAttachments = false;
                for (Attachment attachment : mAttachments) {
                    /**
                     * M: we should judge whether this attachment file exists by use of
                     * {@link Utility#attachmentExists(Context, Attachment) attachmentExists}
                     * @NOTE we should query the reference synchronously, to make write/read DB in a
                     * right order
                     */
                    synchronized (AttachmentUtilities.SYNCHRONIZE_LOCK_FOR_FORWARD_ATTACHMENT) {
                        if (!Utility.attachmentExists(mContext, attachment) &&
                                ((attachment.mFlags & Attachment.FLAG_SMART_FORWARD) == 0)) {
                            attachment.mFlags |= Attachment.FLAG_DOWNLOAD_FORWARD;
                            /// M: we should null the content uri @{
                            attachment.mContentUri = null;
                            /// @}
                            hasUnloadedAttachments = true;
                            Logging.d(TAG,
                                        "Requesting download of attachment #" + attachment.mId);
                        }
                        // Make sure the UI version of the attachment has the now-correct id; we will
                        // use the id again when coming back from picking new attachments
                        if (!attachment.isSaved()) {
                            // this attachment is new so save it to DB.
                            attachment.mMessageKey = mDraft.mId;
                            attachment.save(MessageCompose.this);
                        } else if (attachment.mMessageKey != mDraft.mId) {
                            // We clone the attachment and save it again; otherwise, it will
                            // continue to point to the source message.  From this point forward,
                            // the attachments will be independent of the original message in the
                            // database; however, we still need the message on the server in order
                            // to retrieve unloaded attachments
                            attachment.mMessageKey = mDraft.mId;
                            /// M: create a new attachment and update the mAttachmentList.
                            ///    otherwise the UI still show the source attachment id, which will
                            ///    cause delete UI attachment exception. @{
                            attachment.mId = EmailContent.NOT_SAVED;
                            attachment.save(MessageCompose.this);
                            /// @}
                        }
                    }
                }

                if (mSend) {
                    // Let the user know if message sending might be delayed by background
                    // downlading of unloaded attachments
                    if (hasUnloadedAttachments) {
                        Utility.showToast(MessageCompose.this,
                                R.string.message_view_attachment_background_load);
                    }
                    /** M: show sending attachment fail when some attachments not exist now. @{ */
                    if (Utility.isSomeAttachmentsLost(mContext, mDraft.mId)) {
                        Utility.showToast(mContext, R.string.send_attachment_fail);
                    }
                    /** @} */
                    mController.sendMessage(mDraft);

                    ArrayList<CharSequence> addressTexts = new ArrayList<CharSequence>();
                    addressTexts.add(mToView.getText());
                    addressTexts.add(mCcView.getText());
                    addressTexts.add(mBccView.getText());
                    DataUsageStatUpdater updater = new DataUsageStatUpdater(mContext);
                    updater.updateWithRfc822Address(addressTexts);
                }
                return mDraft.mId;
            }
        }

        private boolean shouldShowSaveToast() {
            // Don't show the toast when rotating, or when opening an Activity on top of this one.
            return !isChangingConfigurations() && !mPickingAttachment;
        }

        @Override
        protected void onSuccess(Long draftId) {
            // Note that send or save tasks are always completed, even if the activity
            // finishes earlier.
            sActiveSaveTasks.remove(mTaskId);
            // Don't display the toast if the user is just changing the orientation
            if (!mSend && shouldShowSaveToast() && mIsBackground) {
                Toast.makeText(mContext, R.string.message_saved_toast, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Send or save a message:
     * - out of the UI thread
     * - write to Drafts
     * - if send, invoke Controller.sendMessage()
     * - when operation is complete, display toast
     */
    private void sendOrSaveMessage(boolean send) {
        if (!mMessageLoaded) {
            Logging.w(TAG, "Attempted to save draft message prior to the state being fully loaded");
            return;
        }
        synchronized (sActiveSaveTasks) {
            mLastSaveTaskId = sNextSaveTaskId++;

            SendOrSaveMessageTask task = new SendOrSaveMessageTask(mLastSaveTaskId, send);

            // Ensure the tasks are executed serially so that rapid scheduling doesn't result
            // in inconsistent data.
            task.executeSerial();
        }
   }

    private void saveIfNeeded() {
        if (!mDraftNeedsSaving) {
            return;
        }
        setMessageChanged(false);
        sendOrSaveMessage(false);
    }

    /**
     * Checks whether all the email addresses listed in TO, CC, BCC are valid.
     */
    @VisibleForTesting
    boolean isAddressAllValid() {
        boolean supportsChips = ChipsUtil.supportsChipsUi();
        for (TextView view : new TextView[]{mToView, mCcView, mBccView}) {
            String addresses = view.getText().toString().trim();
            if (!Address.isAllValid(addresses)) {
                // Don't show an error message if we're using chips as the chips have
                // their own error state.
                if (!supportsChips || !(view instanceof ChipsAddressTextView)) {
                    view.setError(getString(R.string.message_compose_error_invalid_email));
                }
                return false;
            }
        }
        return true;
    }

    private void onSend() {
        // Check if low storage at the onSend.
        Preferences pref = Preferences.getPreferences(this);
        pref.checkLowStorage();
        if (pref.getLowStorage()) {
            Toast.makeText(this, R.string.low_storage_service_stop, Toast.LENGTH_SHORT).show();
            Toast.makeText(this, R.string.low_storage_hint_delete_mail, Toast.LENGTH_SHORT).show();
        }
        if (!isAddressAllValid()) {
            Toast.makeText(this, getString(R.string.message_compose_error_invalid_email),
                           Toast.LENGTH_LONG).show();
        } else if (getAddresses(mToView).length == 0 &&
                getAddresses(mCcView).length == 0 &&
                getAddresses(mBccView).length == 0) {
            mToView.setError(getString(R.string.message_compose_error_no_recipients));
            mToView.requestFocus();
            // We should not show the same toast twice.
            //Toast.makeText(this, getString(R.string.message_compose_error_no_recipients),
            //       Toast.LENGTH_LONG).show();
        /** M: Add a confirm for sending mail without subject @{ */
        } else if (mSubjectView.length() == 0) {
            FragmentManager fm = getFragmentManager();
            SendWithoutSubjectConfirmDialog dialog = (SendWithoutSubjectConfirmDialog) fm
                    .findFragmentByTag(SendWithoutSubjectConfirmDialog.TAG);
            if (dialog == null) {
                new SendWithoutSubjectConfirmDialog()
                        .show(getFragmentManager(), SendWithoutSubjectConfirmDialog.TAG);
            }
        } else {
            triggerSend();
        }
        /** @} */
    }

    /**
     * M: Override the SendWithoutSubjectConfirmDialog callback method
     */
    @Override
    public void onOkPressed() {
        triggerSend();
    }

    @Override
    public void onCancelPressed() {
        mSubjectView.requestFocus();
    }

    /**
     * M: Extract these code to a common method
     */
    private void triggerSend() {
        sendOrSaveMessage(true);
        setMessageChanged(false);
        finish();
    }

    private void showQuickResponseDialog() {
        if (mAccount == null) {
            // Load not finished, bail.
            return;
        }
        InsertQuickResponseDialog.newInstance(null, mAccount)
                .show(getFragmentManager(), null);
    }

    /**
     * Inserts the selected QuickResponse into the message body at the current cursor position.
     */
    @Override
    public void onQuickResponseSelected(CharSequence text) {
        int start = mMessageContentView.getSelectionStart();
        int end = mMessageContentView.getSelectionEnd();
        mMessageContentView.getEditableText().replace(start, end, text);
    }

    private void onDiscard() {
        DeleteMessageConfirmationDialog.newInstance(1, null).show(
                getFragmentManager(), DeleteMessageConfirmationDialog.TAG);
    }

    /**
     * Called when ok on the "discard draft" dialog is pressed.  Actually delete the draft.
     */
    @Override
    public void onDeleteMessageConfirmationDialogOkPressed() {
        if (mDraft.mId > 0) {
            // By the way, we can't pass the message ID from onDiscard() to here (using a
            // dialog argument or whatever), because you can rotate the screen when the dialog is
            // shown, and during rotation we save & restore the draft.  If it's the
            // first save, we give it an ID at this point for the first time (and last time).
            // Which means it's possible for a draft to not have an ID in onDiscard(),
            // but here.
            mController.deleteMessage(mDraft.mId);
        }
        Utility.showToast(MessageCompose.this, R.string.message_discarded_toast);
        setMessageChanged(false);
        finish();
    }

    /**
     * Handles an explicit user-initiated action to save a draft.
     */
    private void onSave() {
        saveIfNeeded();
    }

    private void showCcBccFieldsIfFilled(boolean focusCc) {
        /// M: Fix issue caused by MTKRecipientEditTextView : append text to
        /// the MTKRecipientEditTextView, but the length still is 0. @{
        if ((getRecipientLength(mCcView) > 0)
                || (getRecipientLength(mBccView) > 0)) {
            showCcBccFields(focusCc);
        }
        /// @}
    }

    private void showCcBccFields(boolean focusCc) {
        if (mCcBccContainer.getVisibility() != View.VISIBLE) {
            mCcBccContainer.setVisibility(View.VISIBLE);
            if (focusCc) {
                mCcView.requestFocus();
            }
            UiUtilities.setVisibilitySafe(this, R.id.add_cc_bcc, View.INVISIBLE);
            invalidateOptionsMenu();
        }
    }

    /**
     * Kick off a dialog to choose types of attachments: image, music and video.
     */
    private void onAddAttachment() {
        //check if can add more attachments
        if (getAvailableAttachSize(1) < 0) {
             showAttachmentConfirmDialog(Email.ATTACHMENT_MAX_NUMBER);
             return;
        }
        if (mAttachmentTypeSelectorAdapter == null) {
            mAttachmentTypeSelectorAdapter = new AttachmentTypeSelectorAdapter(this);
        }
        mDialogBuilder = new AlertDialog.Builder(this);
        mDialogBuilder.setIcon(R.drawable.ic_dialog_attach);
        mDialogBuilder.setTitle(R.string.choose_attachment_dialog_title);
        if (mAttachmentTypeSelectorAdapter == null) {
            mAttachmentTypeSelectorAdapter = new AttachmentTypeSelectorAdapter(this);
        }
        mDialogBuilder.setAdapter(mAttachmentTypeSelectorAdapter,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mPickingAttachment = true;
                        addAttachment(mAttachmentTypeSelectorAdapter.buttonToCommand(which));
                        dialog.dismiss();
                    }
                });
        mDialogBuilder.show();
    }

    private void addAttachment(int type) {
        switch (type) {
        case AttachmentTypeSelectorAdapter.ADD_IMAGE:
            try {
                AttachmentUtilities.selectImage(this, REQUEST_CODE_ATTACH_IMAGE);
            } catch (ActivityNotFoundException anf) {
                showError();
                Logging.w(TAG," ActivityNotFoundException happend in attach Image :"
                                + anf.toString());
            }
            break;
        case AttachmentTypeSelectorAdapter.ADD_MUSIC:
            try {
                AttachmentUtilities.selectAudio(this, REQUEST_CODE_ATTACH_SOUND);
            } catch (ActivityNotFoundException anf) {
                showError();
                Logging.w(TAG," ActivityNotFoundException happend in attach Music :"
                                + anf.toString());
            }
            break;
        case AttachmentTypeSelectorAdapter.ADD_VIDEO:
            try {
                AttachmentUtilities.selectVideo(this, REQUEST_CODE_ATTACH_VIDEO);
            } catch (ActivityNotFoundException anf) {
                showError();
                Logging.w(TAG," ActivityNotFoundException happend in attach Video :"
                                + anf.toString());
            }
            break;
        case AttachmentTypeSelectorAdapter.ADD_CONTACT:
            try {
                AttachmentUtilities.selectContact(this,REQUEST_CODE_ATTACH_CONTACT);
            } catch (ActivityNotFoundException anf) {
                showError();
                Logging.w(TAG," ActivityNotFoundException happend in attach Contact :"
                                + anf.toString());
            }
            break;
        case AttachmentTypeSelectorAdapter.ADD_FILE:
            try {
                AttachmentUtilities.selectFile(this, REQUEST_CODE_ATTACH_FILE);
            } catch (ActivityNotFoundException anf) {
                showError();
                Logging.w(TAG," ActivityNotFoundException happend in attach Contact :"
                                + anf.toString());
            }
            break;
        case AttachmentTypeSelectorAdapter.ADD_CALENDAR:
            try {
                AttachmentUtilities.selectCalendar(this, REQUEST_CODE_ATTACH_CALENDAR);
            } catch (ActivityNotFoundException anf) {
                showError();
                Logging.w(TAG," ActivityNotFoundException happend in attach Contact :"
                                + anf.toString());
            }
            break;    
        default:
            Logging.w(TAG, "Can not handle attachment types of " + type);
        }
    }

    private void showError() {
        Toast.makeText(this, this.getString(R.string.attach_error_occurred),
                Toast.LENGTH_SHORT).show();
    }

    private Attachment loadAttachmentInfo(Uri uri) {
        Logging.d(TAG, "loadAttachmentInfo uri : " + uri);
        long size = -1;
        ContentResolver contentResolver = getContentResolver();

        // Load name & size independently, because not all providers support both
        final String name = Utility.getContentFileName(this, uri);
        Logging.d(TAG, "loadAttachmentInfo name : " + name);
        String mimeType = null;
        try {
            // For vcard attach , have to call openAssetFileDescriptor get size information.
            if (uri.toString().startsWith(CONTACT_URI_PREFIX)) {
                uri = uri.buildUpon().appendQueryParameter( ContactsContract.DIRECTORY_PARAM_KEY,
                        String.valueOf(Directory.DEFAULT)).build();
                size = Utility.getAttachSize(this, uri);
                Logging.d(TAG, "read the file size from provider, size : " + size);
            } else {
                Cursor metadataCursor = contentResolver.query(uri,
                        ATTACHMENT_META_SIZE_PROJECTION, null, null, null);
                if (metadataCursor != null) {
                    try {
                        if (metadataCursor.moveToFirst()) {
                            size = metadataCursor .getLong(ATTACHMENT_META_SIZE_COLUMN_SIZE);
                        }
                    } finally {
                        metadataCursor.close();
                    }
                }
            }
            Logging.d(TAG, "loadAttachmentInfo size : " + size);
            mimeType = AttachmentUtilities.inferMimeTypeForUri(this, uri);
            Logging.d(TAG, "loadAttachmentInfo mimeType : " + mimeType);
        } catch (SQLiteException e) {
            Logging.e(TAG, "Query attachment infor from contentUri  " + uri
                    + " failed : " + e.toString());
        } catch (RuntimeException se) {
            Logging.e(TAG, "Have no Permission Query attachment infor from contentUri  "
                            + uri + " : " + se.toString());
        }
        // When the size is not provided, we need to determine it locally.
        if (size < 0) {
            // if the URI is a file: URI, ask file system for its size
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                String path = uri.getPath();
                if (path != null) {
                    File file = new File(path);
                    size = file.length();  // Returns 0 for file not found
                    Logging.d(TAG, "loadAttachmentInfo the attach is file and size is " + size);
                }
            }

            if (size <= 0) {
                // The size was not measurable;  This attachment is not safe to use.
                size = AttachmentUtilities.ATTACHMENT_UNAVAILABLE_SIZE;
                Logging.d(TAG, "loadAttachmentInfo the attach is unavailable set size as "
                        + "'ATTACHMENT_UNAVAILABLE_SIZE'.");
            }
        }

        Attachment attachment = new Attachment();
        attachment.mFileName = name;
        attachment.mContentUri = uri.toString();
        attachment.mSize = size;
        attachment.mMimeType = mimeType;
        if (attachment.mMimeType == null) {
            Logging.d(TAG, "cannot add attachment with uri:" + uri);
            return null;
        }
        Logging.d(TAG, "loadAttachmentInfo attachment : " + attachment.toString());
        return attachment;
    }

    private void addAttachment(Attachment attachment) {
        // Before attaching the attachment, make sure it meets any other pre-attach criteria
        if (attachment.mSize > AttachmentUtilities.MAX_ATTACHMENT_UPLOAD_SIZE) {
            Toast.makeText(this, R.string.message_compose_attachment_size, Toast.LENGTH_LONG)
                    .show();
            return;
        } else if (attachment.mSize == AttachmentUtilities.ATTACHMENT_UNAVAILABLE_SIZE) {
            showCannotAddAttachmentToast();
            return;
        }

        mAttachments.add(attachment);
        updateAttachmentUi();
    }

    private void addAttachments(List<Attachment> attachments) {
        //Check the attachment size in background thread, make sure attachments list have meet the
        //pre-attach criteria
        for (Attachment attach : attachments) {
            mAttachments.add(attach);
        }
        updateAttachmentUi();
    }

    private void updateAttachmentUi() {
        mAttachmentContentView.removeAllViews();

        for (Attachment attachment : mAttachments) {
            // Note: allowDelete is set in two cases:
            // 1. First time a message (w/ attachments) is forwarded,
            //    where action == ACTION_FORWARD
            // 2. 1 -> Save -> Reopen
            //    but FLAG_SMART_FORWARD is already set at 1.
            // Even if the account supports smart-forward, attachments added
            // manually are still removable.
            final boolean allowDelete = (attachment.mFlags & Attachment.FLAG_SMART_FORWARD) == 0;

            View view = getLayoutInflater().inflate(R.layout.mtk_message_compose_attachment,
                    mAttachmentContentView, false);
            TextView nameView = UiUtilities.getView(view, R.id.attachment_name);
            ImageView delete = UiUtilities.getView(view, R.id.remove_attachment);
            TextView sizeView = UiUtilities.getView(view, R.id.attachment_size);

            nameView.setText(attachment.mFileName);
            if (attachment.mSize > 0) {
                sizeView.setText(UiUtilities.formatSize(this, attachment.mSize));
            } else {
                sizeView.setVisibility(View.GONE);
            }
            if (allowDelete) {
                delete.setOnClickListener(this);
                delete.setTag(view);
            } else {
                delete.setVisibility(View.INVISIBLE);
            }
            view.setTag(attachment);
            mAttachmentContentView.addView(view);
        }
        updateAttachmentContainer();
    }

    private void updateAttachmentContainer() {
        mAttachmentContainer.setVisibility(mAttachmentContentView.getChildCount() == 0
                ? View.GONE : View.VISIBLE);
    }

    // M: Support to testcase.
    @VisibleForTesting
    void addAttachmentFromUri(Uri uri) {
        /** M: MTK Dependence @{ */
        if (FeatureOption.MTK_DRM_APP) {
            OmaDrmClient drmClient = new OmaDrmClient(this);
            OmaDrmUtils.DrmProfile profile = OmaDrmUtils.getDrmProfile(this, uri,drmClient);

            // Only normal file and SD type drm file can be forwarded
            if (profile.isDrm() && profile.getMethod() != OmaDrmStore.DrmMethod.METHOD_SD
                    && profile.getMethod() != OmaDrmStore.DrmMethod.METHOD_NONE) {
                Toast toast = Toast.makeText(this,
                        com.mediatek.internal.R.string.drm_can_not_forward, Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
        }
        /** @} */
        // Start asyncTask to do db query.
        startAsyncTaskLoadOneAttachments(uri);
    }

    private void showCannotAddAttachmentToast() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    Toast.makeText(MessageCompose.this,
                            R.string.cannot_add_this_attachment, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showAttachmentSizeTooLargeToast() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    Toast.makeText(MessageCompose.this, 
                            R.string.message_compose_attachment_size, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mPickingAttachment = false;
        if (data == null) {
            return;
        }
        switch (requestCode) {
        case REQUEST_CODE_ATTACH_IMAGE:
            addAttachmentFromUri(data.getData());
            break;
        case REQUEST_CODE_ATTACH_VIDEO:
            addAttachmentFromUri(data.getData());
            break;
        case REQUEST_CODE_ATTACH_SOUND:
            addAttachmentFromUri(data.getData());
            break;
        case REQUEST_CODE_ATTACH_CONTACT:
            Bundle extras = data.getExtras();
            if (extras != null) {
                Uri uri = (Uri)extras.get(ITEXTRA_CONTACTS);
                if (uri != null) {
                    addAttachmentFromUri(uri);
                }
            } else {
                Logging.e(TAG, "Can not get extras data from the attaching contact");
            }
            break;
        case REQUEST_CODE_ATTACH_CALENDAR:
            // handle calendar
            addAttachmentFromUri(data.getData());
            break;
        case REQUEST_CODE_ATTACH_FILE:
            addAttachmentFromUri(data.getData());
            break;
        default:
            Logging.w(TAG, "Can not handle the requestCode [" + requestCode
                    + "] in onActivityResult method");
        }
        setMessageChanged(true);
    }

    private boolean includeQuotedText() {
        return mIncludeQuotedTextCheckBox.isChecked();
    }

    @Override
    public void onClick(View view) {
        if (handleCommand(view.getId())) {
            return;
        }
        switch (view.getId()) {
            case R.id.remove_attachment:
                onDeleteAttachmentIconClicked(view);
                break;
            case R.id.edit_quoted_text:
                onEditQuotedText();
                break;
            default:
                return;
        }
    }

    private void setIncludeQuotedText(boolean include, boolean updateNeedsSaving) {
        mIncludeQuotedTextCheckBox.setChecked(include);
        mQuotedText.setVisibility(mIncludeQuotedTextCheckBox.isChecked()
                ? View.VISIBLE : View.GONE);
        if (updateNeedsSaving) {
            setMessageChanged(true);
        }
        boolean isShowEditQuoted = mIncludeQuotedTextCheckBox.isChecked();
        findViewById(R.id.divider_bar_1).setVisibility(isShowEditQuoted
                ? View.VISIBLE: View.INVISIBLE);
        mEditQuotedText.setVisibility(isShowEditQuoted ? View.VISIBLE: View.INVISIBLE);
    }

    private void onDeleteAttachmentIconClicked(View delButtonView) {
        View attachmentView = (View) delButtonView.getTag();
        Attachment attachment = (Attachment) attachmentView.getTag();
        deleteAttachment(mAttachments, attachment);
        updateAttachmentUi();
        setMessageChanged(true);
    }

    /**
     * Removes an attachment from the current message.
     * If the attachment has previous been saved in the db (i.e. this is a draft message which
     * has previously been saved), then the draft is deleted from the db.
     *
     * This does not update the UI to remove the attachment view.
     * @param attachments the list of attachments to delete from. Injected for tests.
     * @param attachment the attachment to delete
     */
    private void deleteAttachment(List<Attachment> attachments, Attachment attachment) {
        attachments.remove(attachment);
        if ((attachment.mMessageKey == mDraft.mId) && attachment.isSaved()) {
            final long attachmentId = attachment.mId;
            ///M: after resuming draft, change action from reply to forward, attachment will disappear
            attachment.mId = Attachment.NOT_SAVED;
            EmailAsyncTask.runAsyncParallel(new Runnable() {
                @Override
                public void run() {
                    EmailAsyncTask.printStartLog("MessageCompose#deleteAttachment");
                    mController.deleteAttachment(attachmentId);
                    EmailAsyncTask.printStopLog("MessageCompose#deleteAttachment");
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (handleCommand(item.getItemId())) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean handleCommand(int viewId) {
        switch (viewId) {
        case android.R.id.home:
            onBack(false /* systemKey */);
            return true;
        case R.id.send:
            onSend();
            return true;
        case R.id.save:
            // Ensure to show the toast to user in manual saving
            mIsBackground = true;
            onSave();
            return true;
        case R.id.show_quick_text_list_dialog:
            showQuickResponseDialog();
            return true;
        case R.id.discard:
            onDiscard();
            return true;
        case R.id.include_quoted_text:
            // The checkbox is already toggled at this point.
            setIncludeQuotedText(mIncludeQuotedTextCheckBox.isChecked(), true);
            return true;
        case R.id.add_cc_bcc:
            showCcBccFields(true);
            return true;
        case R.id.add_attachment:
            onAddAttachment();
            return true;
        case R.id.settings:
            if (null != mAccount) {
                AccountSettings.actionSettings(this, mAccount.mId);
            }
            return true;
        }
        return false;
    }

    /**
     * Handle a tap to the system back key, or the "app up" button in the action bar.
     * @param systemKey whether or not the system key was pressed
     */
    private void onBack(boolean systemKey) {
        // In the case of "reply/forward", mAccount may not be loaded yet by LoadMessageTask
        // as user pressing the top-left back button, thus ignore this pressing event for 
        // ensuring the correct Ux behavior
        if (mAccount == null) {
            return;
        }
        discardMessageIfNeeded();
        finish();
        if (isOpenedFromWithinApp()) {
            // If opened from within the app, we just close it.
            return;
        }

        if ((isOpenedFromWidget() || !systemKey) && (mAccount != null)) {
            // Otherwise, need to open the main screen for the appropriate account.
            // Note that mAccount should always be set by the time the action bar is set up.
            startActivity(Welcome.createOpenAccountInboxIntent(this, mAccount.mId));
        }
    }

    //Discard message if it is empty.
    private void discardMessageIfNeeded() {
         if(areViewsEmpty()){
             if (mDraft.mId > 0) {
                 mController.deleteMessage(mDraft.mId);
                 Logging.d(TAG, "Message is empty, it will be delete from db , messageId: "
                        + mDraft.mId);
                 Utility.showToast(MessageCompose.this, R.string.message_discarded_toast);
                 setMessageChanged(false);
             }
         }
    }

    private void setAction(String action) {
        if (Objects.equal(action, mAction)) {
            return;
        }

        mAction = action;
        onActionChanged();
    }

    /**
     * Handles changing from reply/reply all/forward states. Note: this activity cannot transition
     * from a standard compose state to any of the other three states.
     */
    private void onActionChanged() {
        if (!hasSourceMessage()) {
            return;
        }
        // Temporarily remove listeners so that changing action does not invalidate and save message
        removeListeners();
        removeErroInfo();

        processSourceMessage(mSource, mAccount);

        // Note that the attachments might not be loaded yet, but this will safely noop
        // if that's the case, and the attachments will be processed when they load.
        if (processSourceMessageAttachments(mAttachments, mSourceAttachments, isForward())) {
            updateAttachmentUi();
            setMessageChanged(true);
        }

        updateActionSelector();
        addListeners();
    }

    /**
     * Updates UI components that allows the user to switch between reply/reply all/forward.
     */
    private void updateActionSelector() {
        ActionBar actionBar = getActionBar();
        // Spinner based mode switching.
        if (mActionSpinnerAdapter == null) {
            mActionSpinnerAdapter = new ActionSpinnerAdapter(this);
            actionBar.setListNavigationCallbacks(mActionSpinnerAdapter, ACTION_SPINNER_LISTENER);
        }
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setSelectedNavigationItem(ActionSpinnerAdapter.getActionPosition(mAction));
        actionBar.setDisplayShowTitleEnabled(false);
    }

    private final OnNavigationListener ACTION_SPINNER_LISTENER = new OnNavigationListener() {
        @Override
        public boolean onNavigationItemSelected(int itemPosition, long itemId) {
            setAction(ActionSpinnerAdapter.getAction(itemPosition));
            return true;
        }
    };

    private static class ActionSpinnerAdapter extends ArrayAdapter<String> {
        public ActionSpinnerAdapter(final Context context) {
            super(context,
                    android.R.layout.simple_spinner_dropdown_item,
                    android.R.id.text1,
                    Lists.newArrayList(ACTION_REPLY, ACTION_REPLY_ALL, ACTION_FORWARD));
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View result = super.getDropDownView(position, convertView, parent);
            ((TextView) result.findViewById(android.R.id.text1)).setText(getDisplayValue(position));
            return result;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View result = super.getView(position, convertView, parent);
            ((TextView) result.findViewById(android.R.id.text1)).setText(getDisplayValue(position));
            return result;
        }

        private String getDisplayValue(int position) {
            switch (position) {
                case 0:
                    return getContext().getString(R.string.reply_action);
                case 1:
                    return getContext().getString(R.string.reply_all_action);
                case 2:
                    return getContext().getString(R.string.forward_action);
                default:
                    throw new IllegalArgumentException("Invalid action type for spinner");
            }
        }

        public static String getAction(int position) {
            switch (position) {
                case 0:
                    return ACTION_REPLY;
                case 1:
                    return ACTION_REPLY_ALL;
                case 2:
                    return ACTION_FORWARD;
                default:
                    throw new IllegalArgumentException("Invalid action type for spinner");
            }
        }

        public static int getActionPosition(String action) {
            if (ACTION_REPLY.equals(action)) {
                return 0;
            } else if (ACTION_REPLY_ALL.equals(action)) {
                return 1;
            } else if (ACTION_FORWARD.equals(action)) {
                return 2;
            }
            Log.w(Logging.LOG_TAG, "Invalid action type for spinner");
            return -1;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.message_compose_option, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.save).setEnabled(mDraftNeedsSaving);
        MenuItem addCcBcc = menu.findItem(R.id.add_cc_bcc);
        if (addCcBcc != null) {
            // Only available on phones.
            addCcBcc.setVisible(
                    (mCcBccContainer == null) || (mCcBccContainer.getVisibility() != View.VISIBLE));
        }
        MenuItem insertQuickResponse = menu.findItem(R.id.show_quick_text_list_dialog);
        insertQuickResponse.setVisible(mQuickResponsesAvailable);
        insertQuickResponse.setEnabled(mQuickResponsesAvailable);
        return true;
    }

    /**
     * Set a message body and a signature when the Activity is launched.
     *
     * @param text the message body
     */
    @VisibleForTesting
    void setInitialComposeText(CharSequence text, String signature) {
        mMessageContentView.setText("");
        int textLength = 0;
        if (text != null) {
            mMessageContentView.append(text);
            textLength = text.length();
        }
        if (!TextUtils.isEmpty(signature)) {
            if (textLength == 0 || text.charAt(textLength - 1) != '\n') {
                mMessageContentView.append("\n");
            }
            mMessageContentView.append(signature);

            // Reset cursor to right before the signature.
            mMessageContentView.setSelection(textLength);
        }
    }

    /**
     * Fill all the widgets with the content found in the Intent Extra, if any.
     *
     * Note that we don't actually check the intent action  (typically VIEW, SENDTO, or SEND).
     * There is enough overlap in the definitions that it makes more sense to simply check for
     * all available data and use as much of it as possible.
     *
     * With one exception:  EXTRA_STREAM is defined as only valid for ACTION_SEND.
     *
     * @param intent the launch intent
     */
    @VisibleForTesting
    void initFromIntent(Intent intent) {
        if (!setAccount(intent)) {
            return;
        }

        // First, add values stored in top-level extras
        String[] extraStrings = intent.getStringArrayExtra(Intent.EXTRA_EMAIL);
        if (extraStrings != null) {
            addAddresses(mToView, extraStrings);
        }
        extraStrings = intent.getStringArrayExtra(Intent.EXTRA_CC);
        if (extraStrings != null) {
            addAddresses(mCcView, extraStrings);
        }
        extraStrings = intent.getStringArrayExtra(Intent.EXTRA_BCC);
        if (extraStrings != null) {
            addAddresses(mBccView, extraStrings);
        }
        String extraString = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (extraString != null) {
            mSubjectView.setText(extraString);
        }

        // Next, if we were invoked with a URI, try to interpret it
        // We'll take two courses here.  If it's mailto:, there is a specific set of rules
        // that define various optional fields.  However, for any other scheme, we'll simply
        // take the entire scheme-specific part and interpret it as a possible list of addresses.
        final Uri dataUri = intent.getData();
        if (dataUri != null) {
            if ("mailto".equals(dataUri.getScheme())) {
                initializeFromMailTo(dataUri.toString());
            } else {
                String toText = dataUri.getSchemeSpecificPart();
                if (toText != null) {
                    if (Address.isAllValid(toText)) {
                        addAddresses(mToView, toText.split(","));
                    } else {
                        Logging.w(TAG, "unavailable email address " + toText
                                + " not add to mToView ");
                    }
                }
            }
        }

        // Next, fill in the plaintext (note, this will override mailto:?body=)
        CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (TextUtils.isEmpty(mMessageContentView.getText().toString())) {
            setInitialComposeText(text, getAccountSignature(mAccount));
        }

        // Next, convert EXTRA_STREAM into an attachment
        if (Intent.ACTION_SEND.equals(mAction) && intent.hasExtra(Intent.EXTRA_STREAM)) {
            Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                startAsyncTaskLoadOneAttachments(uri);
            }
        }

        if (Intent.ACTION_SEND_MULTIPLE.equals(mAction)
                && intent.hasExtra(Intent.EXTRA_STREAM)) {
            ArrayList<Parcelable> list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (null != list) {
                int size = list.size();
                Logging.d(TAG, "Attachments size is "  + size);
                if (size > 0) {
                    int availableSize = getAvailableAttachSize(size);
                    if (availableSize >= 0) {
                        // directly start a asynctask to load attachments
                        Logging.d(TAG, "loadingAttachments runing runing .. add "
                                + size + "attachments");
                        startAsyncTaskLoadMoreAttachments(list, size);
                    } else {
                        // show dialog
                        Logging.d(TAG, "show dialog .. add " + (size + availableSize)
                                + " attachments");
                        Bundle bundle = new Bundle();
                        bundle.putInt(LoadAttachmentsConfirmDialog.EXTRA_ATTACHMENT_ADD_NUMBER,
                                (size + availableSize));
                        bundle.putParcelableArrayList(
                                LoadAttachmentsConfirmDialog.EXTRA_ATTACHMENT_URIS, list);
                        showLoadMoreAttachmentDialog(bundle);
                    }
                }
            }
        }
        // For outside interface
        addBccMySelf(mAccount);
        // Finally - expose fields that were filled in but are normally hidden, and set focus
        showCcBccFieldsIfFilled(false);
        setNewMessageFocus();
    }

    /**
     * When we are launched with an intent that includes a mailto: URI, we can actually
     * gather quite a few of our message fields from it.
     *
     * @param mailToString the href (which must start with "mailto:").
     */
    private void initializeFromMailTo(String mailToString) {

        // Chop up everything between mailto: and ? to find recipients
        int index = mailToString.indexOf("?");
        int length = "mailto".length() + 1;

        /** M: If the mailToString from Intent's uri data didn't be encoded(it
         * should be encoded), the decode method could occur JE. so we catch it
         * and do some workaround @{*/
        String encoded = null;
        String to = null;

        // Extract the recipient after mailto:
        if (index == -1) {
            encoded = mailToString.substring(length);
        } else {
            encoded = mailToString.substring(length, index);
        }
        try {
            to = decode(encoded);
        } catch (UnsupportedEncodingException e) {
            Logging.e(TAG, e.getMessage() + " while decoding '" + mailToString + "'");
        } catch (IllegalArgumentException e) {
            Logging.e(TAG, e.getMessage() + " while decoding '" + mailToString + "'");
            to = encoded;
            mailToString = Uri.encode(mailToString);
        }
        if (to != null && to.length() > 0) {
            addAddresses(mToView, to.split(","));
        }
        /** M: @}*/

        // Extract the other parameters

        // We need to disguise this string as a URI in order to parse it
        Uri uri = Uri.parse("foo://" + mailToString);

        List<String> cc = uri.getQueryParameters("cc");
        addAddresses(mCcView, cc.toArray(new String[cc.size()]));

        List<String> otherTo = uri.getQueryParameters("to");
        addAddresses(mCcView, otherTo.toArray(new String[otherTo.size()]));

        List<String> bcc = uri.getQueryParameters("bcc");
        addAddresses(mBccView, bcc.toArray(new String[bcc.size()]));

        List<String> subject = uri.getQueryParameters("subject");
        if (subject.size() > 0) {
            mSubjectView.setText(subject.get(0));
        }

        List<String> body = uri.getQueryParameters("body");
        if (body.size() > 0) {
            setInitialComposeText(body.get(0), getAccountSignature(mAccount));
        }
    }

    private String decode(String s) throws UnsupportedEncodingException {
        return URLDecoder.decode(s, "UTF-8");
    }

    /**
     * Displays quoted text from the original email
     */
    private void displayQuotedText(Long messageId, String textBody, String htmlBody) {
        // Only use plain text if there is no HTML body
        boolean plainTextFlag = TextUtils.isEmpty(htmlBody);
        String text = plainTextFlag ? textBody : htmlBody;
        if (text != null) {
            text = plainTextFlag ? EmailHtmlUtil.escapeCharacterToDisplay(text) : text;
            if(!plainTextFlag) {
                Attachment[] atts = Attachment.restoreAttachmentsWithMessageId(
                        MessageCompose.this, messageId);
                for(Attachment attachment : atts) {
                    if(attachment.mContentId != null && attachment.mContentUri != null) {
                        Logging.d(TAG, "++++ displayQuotedText mContentId: "
                                + attachment.mContentId);
                        text = AttachmentUtilities.refactorHtmlTextRaw(text, attachment);
                    }
                }
            }
            mQuotedTextArea.setVisibility(View.VISIBLE);
            if (mQuotedText != null && text != null) {
                mQuotedText.loadDataWithBaseURL("email://", text, "text/html", "utf-8", null);
            }
        }
    }

    /**
     * Given a packed address String, the address of our sending account, a view, and a list of
     * addressees already added to other addressing views, adds unique addressees that don't
     * match our address to the passed in view
     */
    private static boolean safeAddAddresses(String addrs, String ourAddress,
            MultiAutoCompleteTextView view, ArrayList<Address> addrList) {
        boolean added = false;
        Address[] addresses = Address.unpack(addrs);
        if (addresses.length < RECIPIENT_THRES) {
            for (Address address : addresses) {
                // Don't send to ourselves or already-included addresses
                if (!address.getAddress().equalsIgnoreCase(ourAddress)
                        && !addrList.contains(address)) {
                    addrList.add(address);
                    addAddress(view, address.toString());
                    added = true;
                }
            }
        } else {
            safeAddAddressesAsync(addresses, ourAddress, view, addrList);
            added = true;
        }

        return added;
    }

    private static void safeAddAddressesAsync(final Address[] addrs, final String ourAddress,
            final MultiAutoCompleteTextView view, final ArrayList<Address> addrList) {
        EmailAsyncTask.runAsyncParallel(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                final int requestNumToSleep = 10;
                Handler handler = new Handler(Looper.getMainLooper());
                for (final Address address : addrs) {
                    // Don't send to ourselves or already-included addresses
                    if (!address.getAddress().equalsIgnoreCase(ourAddress)
                            && !addrList.contains(address)) {
                        addrList.add(address);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                addAddress(view, address.toString());
                            }
                        });

                        if (count++ > Email.RECIPIENT_MAX_NUMBER) {
                            break;
                        }

                        try {
                            // When the queued "add address" requests reached requestNumToSleep,
                            // make this task sleep for a while, give way to main thread
                            // to cope with those queued "add address" requests, thus the
                            // queued requests would not be too many through the course.
                            if (count % requestNumToSleep == 0) {
                                Thread.sleep(100);
                            }
                        } catch (InterruptedException e) {
                            // Nothing to do here
                        }
                    }
                }
            }
        });
    }

    /**
     * Set up the to and cc views properly for the "reply" and "replyAll" cases.  What's important
     * is that we not 1) send to ourselves, and 2) duplicate addressees.
     * @param message the message we're replying to
     * @param account the account we're sending from
     * @param replyAll whether this is a replyAll (vs a reply)
     */
    @VisibleForTesting
    void setupAddressViews(Message message, Account account, boolean replyAll) {
        // Start clean.
        clearAddressViews();

        // If Reply-to: addresses are included, use those; otherwise, use the From: address.
        Address[] replyToAddresses = Address.unpack(message.mReplyTo);
        if (replyToAddresses.length == 0) {
            replyToAddresses = Address.unpack(message.mFrom);
        }
        addAddresses(mToView, replyToAddresses);

        if (replyAll) {
            String ourAddress = account.mEmailAddress;
            // Keep a running list of addresses we're sending to
            ArrayList<Address> allAddresses = new ArrayList<Address>();
            for (Address address: replyToAddresses) {
                allAddresses.add(address);
            }
            safeAddAddresses(message.mTo, ourAddress, mToView, allAddresses);
            safeAddAddresses(message.mCc, ourAddress, mCcView, allAddresses);
        }
        showCcBccFieldsIfFilled(false);
    }

    private void clearAddressViews() {
        mToView.setText("");
        mCcView.setText("");
        mBccView.setText("");
    }

    /**
     * Pull out the parts of the now loaded source message and apply them to the new message
     * depending on the type of message being composed.
     */
    @VisibleForTesting
    void processSourceMessage(Message message, Account account) {
        String subject = message.mSubject;
        if (subject == null) {
            subject = "";
        }

        if (ACTION_REPLY.equals(mAction) || ACTION_REPLY_ALL.equals(mAction)) {
            setupAddressViews(message, account, ACTION_REPLY_ALL.equals(mAction));
            // set the reply prefix as multi language support
            setReplyOrReplyAllSubjectPrefix(subject);
            displayQuotedText(message.mId, message.mText, message.mHtml);
            setIncludeQuotedText(true, false);
        } else if (ACTION_FORWARD.equals(mAction)) {
            // If we had previously filled the recipients from a draft, don't erase them here!
            if (!ACTION_EDIT_DRAFT.equals(getIntent().getAction())) {
                clearAddressViews();
            }
            // set the forward prefix as multi language support
            setForwardSubjectPrefix(subject);
            displayQuotedText(message.mId, message.mText, message.mHtml);
            setIncludeQuotedText(true, false);
        } else {
            Logging.w(TAG, "Unexpected action for a call to processSourceMessage " + mAction);
        }

        addBccMySelf(account);
        showCcBccFieldsIfFilled(false);
        setNewMessageFocus();
    }

    private void addBccMySelf(Account account) {
        boolean bccMySelf = Preferences.getSharedPreferences(this).getBoolean(
                Preferences.BCC_MYSELF_KEY, Preferences.BCC_MYSELF_DEFAULT);
        if (account == null) {
            Logging.e(TAG, "Current account is null, can't do bcc myself opertation.");
            return;
        }
        if (bccMySelf) {
            if (!getRecipientText(mBccView).toString().contains(
                    account.mEmailAddress)) {
                Address a = new Address(account.mEmailAddress);
                ///M: The displayName is not come from server and not encode,
                // so no need to decode here, set to "false".
                a.setPersonal(account.mDisplayName, false);
                addAddress(mBccView, a.toString());
                mAddBccBySetting = true;
                Logging.d(TAG, "add bcc myself " + a.toString());
            }
        }
    }

    /**
     * When user chose forward operation,use this method to get the default prefix 
     * for email subject.
     * The prefix is loaded from res/string.xml named 'message_compose_foward_profix'
     * If not find in the language strings.xml, by default use "Fwd"
     * For example if original subject is 'How to learn English', 
     * the result is 'Fwd: How to learn English' 
     * @param suject the original email subject
     * 
     */
    private void setForwardSubjectPrefix(String subject) {
        // set default prefix as Fwd
        String prefix = getForwardSubjectPrefix();
        if (null != subject
                && !subject.toLowerCase().startsWith(prefix.toLowerCase())) {
            mSubjectView.setText(prefix + ": " + subject);
        } else {
            mSubjectView.setText(subject);
        }
    }

    /**
     * When use chose reply or reply all operation, use this method
     * to get the default prefix for email subject.
     * The prefix is loaded from res/string.xml named 'message_compose_Reply_profix'
     * For example if original subject is 'How to learn English',
     * the result is 'Re: How to learn English'
     * @param suject the original email subject
     * 
     */
    private void setReplyOrReplyAllSubjectPrefix(String subject) {
        // set default prefix as Re
        String prefix = getReplyOrReplyAllSubjectPrefix();
        if (null != subject
                && !subject.toLowerCase().startsWith(prefix.toLowerCase())) {
            mSubjectView.setText(prefix + ": " + subject);
        } else {
            mSubjectView.setText(subject);
        }
    }

    private String getForwardSubjectPrefix() {
        return getString(R.string.message_compose_foward_profix);
    }

    private String getReplyOrReplyAllSubjectPrefix(){
        return getString(R.string.message_compose_reply_profix);
    }

    /**
     * Processes the source attachments and ensures they're either included or excluded from
     * a list of active attachments. This can be used to add attachments for a forwarded message, or
     * to remove them if going from a "Forward" to a "Reply"
     * Uniqueness is based on attachmentId.
     * Note: Mostly the attachmentId isn't same to each other, but draft attachments are an exception
     * and both are -1. This exception is never happened for user only can add one attachment at a time.
     * when user add another attachment, the attachment which id is -1 will be changed an available id.
     * There only one attachment with id -1.
     *
     *
     * @param current the list of active attachments on the current message. Injected for tests.
     * @param sourceAttachments the list of attachments related with the source message. Injected
     *     for tests.
     * @param include whether or not the sourceMessages should be included or excluded from the
     *     current list of active attachments
     * @return whether or not the current attachments were modified
     */
    @VisibleForTesting
    boolean processSourceMessageAttachments(
            List<Attachment> current, List<Attachment> sourceAttachments, boolean include) {

        // Build a map of filename to the active attachments.
        HashMap<Long, Attachment> currentIds = new HashMap<Long, Attachment>();
        for (Attachment attachment : current) {
            currentIds.put(attachment.mId, attachment);
        }

        boolean dirty = false;
        if (include) {
            // Needs to make sure it's in the list.
            for (Attachment attachment : sourceAttachments) {
                if (!currentIds.containsKey(attachment.mId)) {
                    current.add(attachment);
                    dirty = true;
                }
            }
        } else {
            // Need to remove the source attachments.
            HashSet<String> sourceNames = new HashSet<String>();
            for (Attachment attachment : sourceAttachments) {
                if (currentIds.containsKey(attachment.mId)) {
                    deleteAttachment(current, currentIds.get(attachment.mId));
                    /** M: because changed attachment id to mDraft.mId when SendOrSaveMessageTask,
                     * should change messageKey to another at here, otherwise the attachment
                     * will can't be forward, because it will be filtered out by
                     * SendOrSaveMessageTask. @{ */
                    if (attachment.mMessageKey == mDraft.mId) {
                        attachment.mMessageKey = -1;
                    }
                    /** @} */
                    dirty = true;
                }
            }
        }

        return dirty;
    }

    /**
     * Set a cursor to the end of a body except a signature.
     */
    @VisibleForTesting
    void setMessageContentSelection(String signature) {
        int selection = mMessageContentView.length();
        if (!TextUtils.isEmpty(signature)) {
            int signatureLength = signature.length();
            int estimatedSelection = selection - signatureLength;
            if (estimatedSelection >= 0) {
                CharSequence text = mMessageContentView.getText();
                int i = 0;
                while (i < signatureLength
                       && text.charAt(estimatedSelection + i) == signature.charAt(i)) {
                    ++i;
                }
                if (i == signatureLength) {
                    selection = estimatedSelection;
                    while (selection > 0 && text.charAt(selection - 1) == '\n') {
                        --selection;
                    }
                }
            }
        }
        mMessageContentView.setSelection(selection, selection);
    }

    /**
     * In order to accelerate typing, position the cursor in the first empty field,
     * or at the end of the body composition field if none are empty.  Typically, this will
     * play out as follows:
     *   Reply / Reply All - put cursor in the empty message body
     *   Forward - put cursor in the empty To field
     *   Edit Draft - put cursor in whatever field still needs entry
     */
    private void setNewMessageFocus() {
        if (mToView.length() == 0) {
            mToView.requestFocus();
        } else if (mSubjectView.length() == 0) {
            mSubjectView.requestFocus();
        } else {
            mMessageContentView.requestFocus();
            scrollViewTop(mMessageContentView);
        }
    }

    private boolean isForward() {
        return ACTION_FORWARD.equals(mAction);
    }

    /**
     * @return the signature for the specified account, if non-null. If the account specified is
     *     null or has no signature, {@code null} is returned.
     */
    private static String getAccountSignature(Account account) {
        return (account == null) ? null : account.mSignature;
    }

    /**
     * Not use UI thread to loading attachment,it may cause ANR.
     * Note: it is only used to loading one attachment.
     * @param uri
     */
    private void startAsyncTaskLoadOneAttachments(Uri uri) {
        new EmailAsyncTask<Uri, Void, Attachment>(mTaskTracker) {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showLoadAttachmentProgressDialog(this);
            }

            @Override
            protected Attachment doInBackground(Uri... uri) {
                Logging.d(">>>> EmailAsyncTask#executeSerial "
                        + "startAsyncTaskLoadOneAttachments#doInBackground");
                Attachment att = loadAttachmentInfo(uri[0]);
                Logging.d("<<<< EmailAsyncTask#executeSerial "
                        + "startAsyncTaskLoadOneAttachments#doInBackground");
                return att;
            }

            @Override
            protected void onSuccess(Attachment attachment) {
                releaseProgressDialog();
                if (attachment == null) {
                    showCannotAddAttachmentToast();
                    return;
                }
                String mimeType = attachment.mMimeType;
                if (!TextUtils.isEmpty(mimeType) && MimeUtility.mimeTypeMatches(mimeType,
                        AttachmentUtilities.ACCEPTABLE_ATTACHMENT_SEND_INTENT_TYPES)) {
                    addAttachment(attachment);
                    setMessageChanged(true);
                    return;
                }else {
                    showCannotAddAttachmentToast();
                }
            }

            @Override
            protected void onCancelled(Attachment result) {
                super.onCancelled(result);
                releaseProgressDialog();
            }
            
        /** M: change executeSerial() to executeParallel().
        * To avoid that when a task which needs a long time to finish do not finish,
        * another task cannot execute.*/
        }.executeParallel(uri);
    }

    /**
     * Not use UI thread to loading attachments,it may cause ANR.
     * Note: it is only used to loading more attachments.
     * @param list    ArrayList uri 
     * @param addMumber the current available size can be added. 
     */
    private void startAsyncTaskLoadMoreAttachments(ArrayList<Parcelable> list, int addMumber){
         final ArrayList<Parcelable> attachUris = list;
         final int length = addMumber;
         new EmailAsyncTask<Void, Void, List<Attachment>>(mTaskTracker) {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showLoadAttachmentProgressDialog(this);
            }

            @Override
            protected List<Attachment> doInBackground(Void... params) {
                Logging.d(">>>> EmailAsyncTask#executeSerial "
                        + "startAsyncTaskLoadMoreAttachments#doInBackground");
                List<Attachment> attachments = new ArrayList<Attachment>();
                int s = 1;
                for (Object parcelable : attachUris) {
                    Uri uri = (Uri) parcelable;
                    if (uri != null && s <= length) {
                        Attachment attachment = loadAttachmentInfo(uri);
                        // Before attaching the attachment, make sure it meets any other pre-attach
                        // criteria, it is much better check in background thread not in UI.
                        if (null != attachment 
                                && !TextUtils.isEmpty(attachment.mMimeType)
                                && MimeUtility.mimeTypeMatches(attachment.mMimeType,
                                   AttachmentUtilities.ACCEPTABLE_ATTACHMENT_SEND_INTENT_TYPES)
                                && attachment.mSize <= AttachmentUtilities
                                   .MAX_ATTACHMENT_UPLOAD_SIZE
                                && attachment.mSize != AttachmentUtilities
                                   .ATTACHMENT_UNAVAILABLE_SIZE) {
                            attachments.add(attachment);
                        }else if(null != attachment) {
                            if (attachment.mSize > AttachmentUtilities.MAX_ATTACHMENT_UPLOAD_SIZE) {
                                showAttachmentSizeTooLargeToast();
                                Logging.d(TAG,
                                        "Attachment is larger than the MAX_ATTACHMENT_UPLOAD_SIZE "
                                        + attachment.toString());
                            } else {
                                showCannotAddAttachmentToast();
                                Logging.d(TAG,
                                        "Attachment is not ACCEPTABLE_ATTACHMENT_SEND_INTENT_TYPES "
                                        + attachment.toString());
                            }
                        }
                        s++;
                    }
                }
                Logging.d("<<<< EmailAsyncTask#executeSerial "
                        + "startAsyncTaskLoadMoreAttachments#doInBackground");
                return attachments;
            }

            @Override
            protected void onSuccess(List<Attachment> attachments) {
                releaseProgressDialog();
                if (null == attachments || attachments.size() == 0) {
                    return;
                }
                //Add attachments list to UI
                addAttachments(attachments);
                setMessageChanged(true);
            }

            @Override
            protected void onCancelled(List<Attachment> result) {
                super.onCancelled(result);
                releaseProgressDialog();
            }

        }.executeSerial((Void [])null);
    }

    private void releaseProgressDialog(){
        if (mProgressDialog != null) {
            mProgressDialog.dismissAllowingStateLoss();
            mProgressDialog = null;
        }
    }

    /**
     * @param addNumber 
     * @return the available size that user can add.
     */
    private int getAvailableAttachSize(int addSize) {
        int currentNumber = (mAttachments == null) ? 0 : mAttachments.size();
        Logging.d(TAG, "Current attachment size : " + currentNumber);
        return Email.ATTACHMENT_MAX_NUMBER -(currentNumber + addSize);
    }

    /**
     * When send attachment size > Email.ATTACHMENT_MAX_NUMBER,
     * show confirm dialog
     */
    private void showLoadMoreAttachmentDialog(Bundle bundle){
        LoadAttachmentsConfirmDialog.newInstance(bundle, null).show(
                getFragmentManager(), LoadAttachmentsConfirmDialog.TAG);
    }

    @Override
    public void onLoadAttachmentsConfirmDialogOkPressed(Bundle bundle){
        if(bundle==null){
            Logging.e(TAG, "LoadAttachmentsConfirmDialog get Bundle is null ");
            return;
        }
        ArrayList<Parcelable> uris = 
            bundle.getParcelableArrayList(LoadAttachmentsConfirmDialog.EXTRA_ATTACHMENT_URIS);
        int addNumber = bundle.getInt(LoadAttachmentsConfirmDialog.EXTRA_ATTACHMENT_ADD_NUMBER,-1);
        if (null != uris) {
            startAsyncTaskLoadMoreAttachments(uris,addNumber);
        }
    }

    /**
     * When have added Email.ATTACHMENT_MAX_NUMBER+ attachment, 
     * show confirm dialog  
     */
    private void showAttachmentConfirmDialog(int maxCount){
        AttachmentsConfirmDialog.newInstance(maxCount).show(
                getFragmentManager(), AttachmentsConfirmDialog.TAG);
    }

    /**
     * When have added Email.ATTACHMENT_MAX_NUMBER+ attachment, 
     * show confirm dialog  
     */
    private void showLoadAttachmentProgressDialog(EmailAsyncTask task){
        FragmentManager fm = getFragmentManager();
        mProgressDialog = LoadingAttachProgressDialog.newInstance(task,null);
        fm.beginTransaction()
        .add(mProgressDialog,LoadingAttachProgressDialog.TAG)
        .commit();
    }

    /**
     * Loading attachment Progress dialog
     */
    public static class LoadingAttachProgressDialog extends DialogFragment {
        @SuppressWarnings("hiding")
        public static final String TAG = "LoadingAttachProgressDialog";
        static EmailAsyncTask sLoadingTask = null;
        /**
         * Create a dialog for Loading attachment asynctask.
         * @param mTask Loading attachment asynctask
         */
        public static LoadingAttachProgressDialog newInstance(EmailAsyncTask task,
                Fragment parentFragment ) {
            LoadingAttachProgressDialog f = new LoadingAttachProgressDialog();
            f.setTargetFragment(parentFragment, 0);
            sLoadingTask = task;
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            ProgressDialog dialog = new ProgressDialog(context);
            dialog.setIndeterminate(true);
            dialog.setMessage(getString(R.string.loading_attachment));
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }

        /**
         * Listen for cancellation, which can happen from places other than the
         * negative button (e.g. touching outside the dialog), and stop the
         * checker
         */
        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            Logging.d(MessageCompose.TAG, "LoadingAttachProgressDialog is onCancel "
                    + "and mLoadingTask will be canceled too");
            sLoadingTask.cancel(true);
        }
    }

   private class AccountDropdownPopup extends ListPopupWindow {
       public AccountDropdownPopup(Context context) {
           super(context);
           setAnchorView(mFromView);
           setModal(true);
           setPromptPosition(POSITION_PROMPT_ABOVE);
           setOnItemClickListener(new OnItemClickListener() {
               public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                   onAccountSpinnerItemClicked(position);
                   Logging.d(TAG, "AccountDropdownPopup click position: " + position);
                   dismiss();
               }
           });
       }

       @Override
       public void show() {
           setWidth(getResources().getDimensionPixelSize(
                   R.dimen.account_dropdown_dropdownwidth));
           setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
           super.show();
           // List view is instantiated in super.show(), so we need to do this after...
           getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
       }
   }

   private void onAccountSpinnerItemClicked(int position) {
       if (mAccountsSelectorAdapter == null) { // just in case
           return;
       }
       final long accountId = mAccountsSelectorAdapter.getAccountId(position);
       if(mAccount == null || accountId == mAccount.mId){
           return;
       }
       String address = mAccountsSelectorAdapter.getAccountEmailAddress(position);
       mFromView.setText(address);
       Account account = Account.restoreAccountWithId(this, accountId);
       if(accountId == mAccount.mId){
           //Account not changed 
           return;
       }
       Logging.d(TAG, "Switch account " + (account == null? "null" :account.toString()));
       Account preAccount = mAccount;
       // M: check the message body to indicate that whether it is empty
       boolean isEmptyBefSwitchAccount = isBodyEmpty();
       setAccount(account);

       // M: if message body is empty before account switching, then replace the body with new
       // account's signature
       if (isEmptyBefSwitchAccount) {
           setInitialComposeText(null, getAccountSignature(account));
           setMessageChanged(true);
           setMessageLoaded(true);
       }

       // Replace the pre_account address with new account in bccview
       // when matching the following conditions:
       // 1) only one account in bccview 
       // 2) address is pre_account
       // else add new switched account to bcc view.
       boolean bccMySelf = Preferences.getSharedPreferences(this).getBoolean(
                Preferences.BCC_MYSELF_KEY, Preferences.BCC_MYSELF_DEFAULT);
       if (bccMySelf) {
            Address[] bcc = Address.parse(mBccView.getText().toString().trim(), false);
            if (bcc.length == 1
                    && bcc[0].getAddress().equals(preAccount.mEmailAddress)) {
                mBccView.setText("");
                Address a = new Address(account.mEmailAddress);
                ///M: The displayName is not come from server and not encode,
                // so no need to decode here, set to "false".
                a.setPersonal(account.mDisplayName, false);
                addAddress(mBccView, a.toString());
                mAddBccBySetting = true;
            } else {
                addBccMySelf(account);
            }
       }
   }

    /**
     * Load account info.
     */
    private void loadAccountInfo() {
        // lookup account info
        mLoaderManager.restartLoader(LOADER_ID_ACCOUNT_LIST, null,
                new LoaderCallbacks<Cursor>() {
                    @Override
                    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                        return SwitchAccountSelectorAdapter
                                .createLoader(MessageCompose.this);
                    }

                    @Override
                    public void onLoadFinished(Loader<Cursor> loader,
                            Cursor data) {
                        mCursor = data;
                        Logging.d(TAG, " onLoadFinished account number : "
                                + (data != null ? data .getCount() : 0));
                        updateChangeAccountSpitter();
                    }

                    @Override
                    public void onLoaderReset(Loader<Cursor> loader) {
                        mCursor = null;
                        Logging.d(TAG, " onLoaderReset ... " );
                    }
                });
    }

   //Update UI
   private void updateChangeAccountSpitter() {
        mAccountsSelectorAdapter.swapCursor(mCursor);
        View switchAccountIcon =UiUtilities.getView(this, R.id.switch_account_icon);
        if (mCursor != null && mCursor.getCount() > 1 ) {
            switchAccountIcon.setVisibility(View.VISIBLE);
            mAccountSpinner.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (mAccountsSelectorAdapter.getCount() > 0) {
                        mAccountDropdown.show();
                    }else{
                        Logging.d(TAG, "mAccountsSelectorAdapter.getCount = 0");
                    }
                }
            });
            mAccountSpinner.setClickable(true);
        } else {
            mAccountSpinner.setClickable(false);
            switchAccountIcon.setVisibility(View.GONE);
        }
   }

   private void initLengthFilter(){
       UiUtilities.setupLengthFilter(mSubjectView, this,
               Email.EDITVIEW_MAX_LENGTH_1, true);
       UiUtilities.setupLengthFilter(mMessageContentView,
               this, Email.EDITVIEW_MAX_LENGTH_3, true);
       UiUtilities.setupLengthFilter(mToView,
               this, Email.EDITVIEW_MAX_LENGTH_4, true);
       UiUtilities.setupLengthFilter(mCcView,
               this, Email.EDITVIEW_MAX_LENGTH_4, true);
       UiUtilities.setupLengthFilter(mBccView,
               this, Email.EDITVIEW_MAX_LENGTH_4, true);
   }

    /// M: New feature to support auto scroll to top, when message content get focus. @{
    private int getContentViewTop() {
        int values = 0;
        // add FromView height
        values += mAccountSpinner.getHeight();
        // add To/Cc/Bcc/Subject height
        values += mRecipientsViewHeight;
        // add Attachment height
        values += (mAttachmentContentView.VISIBLE != View.VISIBLE
                ? 0 : mAttachmentContentView.getHeight());
        // add padding height
        values += PADDING_HEIGHT_REPLY;
        Logging.d(TAG, "getContentViewTop :  " + values);
        return values;
    }

    private void scrollViewTop(final View view) {
        // skip in portrait mode
        if (UiUtilities.useTwoPane(this)) {
            Logging.d(TAG, " Skip auto scroll if isTwoPane " + UiUtilities.useTwoPane(this));
            return;
        }
        // reset the default values
        unRegisteGlobalLayoutListener();
        registeGlobalLayoutListener();
    }

    private void setRecipientsViewHeight(int height) {
        mRecipientsScrollCounter++;
        if (mRecipientsViewHeight != height) {
            mRecipientsViewHeight = height;
            // Only scroll and handle the changed event.
            Logging.d(TAG, "smoothScrollTo ");
            mScrollView.smoothScrollTo(0, getContentViewTop());
        }
        /*
         * It is much different to get the recipientView height, because it will shrink many times.
         * So try to observer the changed. Unregister the observer after it changed 5 times,
         * mostly we can get the ultimate height.
         */
        if (mRecipientsScrollCounter >= RECIPIENTVIEW_SHRINK_MAX_TIME) {
            unRegisteGlobalLayoutListener();
        }
    }

    private void unRegisteGlobalLayoutListener() {
        if (mGlobalLayoutListener != null) {
            Logging.d(TAG, "unRegisteGlobalLayoutListener");
            ViewTreeObserver vto = mRecipientsView.getViewTreeObserver();
            vto.removeGlobalOnLayoutListener(mGlobalLayoutListener);
            mGlobalLayoutListener = null;
            mRecipientsScrollCounter = 0;
            mRecipientsViewHeight = 0;
        }
    }

    private void registeGlobalLayoutListener() {
        ViewTreeObserver vto = mRecipientsView.getViewTreeObserver();
        if( mGlobalLayoutListener == null ){
            mGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int height = mRecipientsView.getMeasuredHeight();
                    setRecipientsViewHeight(height);
                }
            };
            vto.addOnGlobalLayoutListener(mGlobalLayoutListener);
            Logging.d(TAG, "registeGlobalLayoutListener ");
        }
    }
    /// New feature to support auto scroll to top, end @{

    private void onEditQuotedText() {
        // 1) make itself invisible
        mQuotedTextArea.setVisibility(View.GONE);
        mIncludeQuotedTextCheckBox.setChecked(false);
        // 2) add quoted text to content view.
        composeQuotedText();
        // 3) update attachment list.
        onResetAttachmentList();
    }

    /**
     * For EAS account, we have to discard smartForward/ smartReply if user edit the quoted text.
     * Have two solutions to handle the source attachment for this case.
     * 1) Delete the source attachments since the attachments can't send successfully.
     * 2) Try to download them before sending the source attachment.
     * This method is responsible for solution 2.
     */
    private void onResetAttachmentList(){
        final boolean supportsSmartForward =
            (mAccount.mFlags & Account.FLAGS_SUPPORTS_SMART_FORWARD) != 0;
        if(supportsSmartForward){
            //Only for SmartForward account
            Logging.d(TAG,"Reset the SmartForward source attachments");
            for (Attachment attachment : mAttachments) {
                if((attachment.mFlags & Attachment.FLAG_SMART_FORWARD) != 0) {
                    Logging.d(TAG,"FLAG_SMART_FORWARD attachments" + attachment.toString());
                    if(attachment.mContentId != null){
                        Logging.d(TAG,"Delete inline attachment: " + attachment.toString());
                        // Delete all the inline attachments
                        deleteAttachment(mAttachments, attachment);
                    } else {
                        Logging.d(TAG,"Change attachments falg :" + attachment.toString());
                        // Download the normal attachments
                        attachment.mFlags &= ~Attachment.FLAG_SMART_FORWARD;
                    }
                }
            }
            updateAttachmentUi();
        }
    }

    private void composeQuotedText(){
        String htmlBody = "";
        String textBody = "";
        String introText = "";
        if (ACTION_REPLY.equals(mAction) || ACTION_REPLY_ALL.equals(mAction)
                || ACTION_FORWARD.equals(mAction)) {
            if (mSource != null) {
                Logging.d(TAG, "appendQuotedText from source message " + mSource.mId);
                // get text/html source message body
                htmlBody = mSource.mHtml;
                textBody = mSource.mText;

                // construct a quoted message content
                String fromAsString = Address.unpackToString(mSource.mFrom);
                if (isForward()) {
                    String subject = mSource.mSubject;
                    String to = Address.unpackToString(mSource.mTo);
                    String cc = Address.unpackToString(mSource.mCc);
                    introText = getString(
                            R.string.message_compose_fwd_header_fmt, subject,
                            fromAsString, to != null ? to : "", cc != null ? cc : "");
                } else {
                    introText = getString( R.string.message_compose_reply_header_fmt, fromAsString);
                }
            }
        } else if (ACTION_EDIT_DRAFT.equals(mAction)) {
            htmlBody = mDraft.mHtmlReply;
            textBody = mDraft.mTextReply;
            introText = mDraft.mIntroText;
        } else {
            // This should not happen.
            Logging.d(TAG, "ACTION which should not happen have happened,ACTION type: " + mAction);
        }
        appendQuotedText(htmlBody, textBody, introText);
    }

    private void appendQuotedText(String htmlBody, String textBody, String introText) {
        // save current content text length to support a appropriate focus
        int textLength = mMessageContentView.getText().toString().length();

        // Get quoted text content.
        boolean plainTextFlag = TextUtils.isEmpty(htmlBody);
        String quotedText = plainTextFlag ? textBody : HtmlConverter.htmlToText(htmlBody);

        //append introText and quotedText
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(introText)) {
            sb.append(introText);
        }
        if (!TextUtils.isEmpty(quotedText)) {
            sb.append(quotedText);
        }

        // Disable the LengthFilter
        UiUtilities.setupLengthFilter(mMessageContentView, this, Integer.MAX_VALUE, false);

        // Append to the message content.
        mMessageContentView.append(sb.toString());

        // Enable the LengthFilter again, but enlarge it.
        UiUtilities.setupLengthFilter(mMessageContentView, this,
                (sb.toString().length() + Email.EDITVIEW_MAX_LENGTH_3), true);

        // Reset cursor to right before the quoted text.
        mMessageContentView.requestFocus();
        mMessageContentView.setSelection(textLength);
    }

    /// M: add for Testing. @{
    @VisibleForTesting
    Message getSourceMessage() {
        return mSource;
    }

    @VisibleForTesting
    ArrayList<Attachment> getSourceAttachment() {
        return mSourceAttachments;
    }

    @VisibleForTesting
    Message getDraft() {
        return mDraft;
    }

    @VisibleForTesting
    CopyOnWriteArrayList<Attachment> getAttachment() {
        return mAttachments;
    }

    @VisibleForTesting
    AlertDialog.Builder getAttachmentDialog() {
        return mDialogBuilder;
    }

    @VisibleForTesting
    AccountDropdownPopup getAccountDropdown() {
        return mAccountDropdown;
    }

    @VisibleForTesting
    String getAction() {
        return mAction;
    }
    /// }@

    /// M: Fix issue caused by MTKRecipientEditTextView: append text to
    /// the MTKRecipientEditTextView and try to get the text length immediately,
    /// but the length still is 0.
    /// Use a new interface/handleAndGetText to get a stable text and length. @{
    private CharSequence getRecipientText(
            MultiAutoCompleteTextView recipientView) {
        if (recipientView instanceof ChipsAddressTextView) {
            return ((ChipsAddressTextView) recipientView).handleAndGetText();
        } else {
            return recipientView.getText();
        }
    }

    private int getRecipientLength(MultiAutoCompleteTextView recipientView) {
        if (recipientView instanceof ChipsAddressTextView) {
            return ((ChipsAddressTextView) recipientView).handleAndGetText().length();
        } else {
            return recipientView.length();
        }
    }
    /// @}
}
