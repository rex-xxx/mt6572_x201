
package com.android.email.service;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.android.email.Controller;
import com.android.email.Email;
import com.android.email.LegacyConversions;
import com.android.email.R;
import com.android.emailcommon.Logging;
import com.android.emailcommon.internet.MimeMessage;
import com.android.emailcommon.internet.MimeUtility;
import com.android.emailcommon.mail.Address;
import com.android.emailcommon.mail.MessagingException;
import com.android.emailcommon.mail.Part;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.service.EmailExternalCalls;
import com.android.emailcommon.service.EmailExternalConstants;
import com.android.emailcommon.service.EmailExternalUtils;
import com.android.emailcommon.utility.ConversionUtilities;
import com.android.emailcommon.utility.Utility;

/**
 * This service is used to support BT-MAP send mail background.
 * Handle tree types of action:
 * 1) Only Save Message to a Special mail box:
 *    ACTION_DIRECT_SEND + (EXTRA_FLAG=2) + EXTRA_MAILBOX_TYPE (inbox;draft;sent;trash;juck) 
 * 2) Send Message and save to the SENT Box:
 *    ACTION_DIRECT_SEND + (EXTRA_FLAG=0) :Send and Save to SENT BOX (sent)
 * 3) Send Message but not save to the SENT Box:
 *    ACTION_DIRECT_SEND + (EXTRA_FLAG=1) :Send not Save to SENT BOX (sent)
 * 4) Update a given Mail box:
 *    ACTION_UPDATE_INBOX 
 */
public class EmailExternalService extends IntentService {

    private static final String TAG = "EmailExternalService";

    /** 0 Save message to SENT; 1 Do not save message to SENT ; 2 save message without sending */
    private static final String EXTRA_FLAG = "com.android.email.extra.FLAG";
    
    /** 0 inbox; 1 mail; 2 parent; 3 drafts; 4 outbox; 5 sent; 6 trash; 7 junk */
    private static final String EXTRA_MAILBOX_TYPE = "com.android.email.extra.MAILBOX_TYPE";
     
    /** just save the message to special mailbox without sending */
    private static final int SAVE_MESSAGE_WITHOUT_SENDING = 2;
    
    /** send the message and save to SEND box */
    private static final int SENDING_MESSAGE_WITH_SENDBOX = 0;
    
    /** A file URI of email package contain whole message */
    private static final String EXTRA_STREAM = "com.android.email.extra.STREAM";

//    /** Email account id */
//    private static final String EXTRA_ACCOUNT = "com.android.email.extra.ACCOUNT";

    /** A long array contain account id */
    private static final String EXTRA_ACCOUNT_ARRAY = "com.android.email.extra.ACCOUNT_ARRAY";

    /** A boolean argument indicate if need feedback */
    private static final String EXTRA_NEED_RESULT = "com.android.email.extra.NEED_RESULT";

//    /** Success(0) or Fail(1) */
//    private static final String EXTRA_RESULT = "com.android.email.extra.RESULT";
    
    private Controller mController = Controller.getInstance(this);

    /// M: modified for tesing
    private EmailExternalCalls.Stub mControllerCallback = null;
    private ExternalHandler mExternalHandler = null;
    
    //For send mail action
    private boolean mSavetoSent = false;
    
    private Uri mUri = null;
    
    //The update account ACCOUNT_ARRAY is null, use this account to send result 
    private static final long NO_UPDATE_ACCOUNT = 0;

    /** M: use for testing @{*/
    public void injectTestController(Controller controller) {
        mController = controller;
    }
    /** @} */
    public EmailExternalService() {
        // Class name will be the thread name.
        super(EmailExternalService.class.getName());

        /**
         * Intent should be redelivered if the process gets killed before
         * completing the job.
         */
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final Intent extraIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        final String action = extraIntent.getAction();
        if (Email.DEBUG) {
            Logging.d(TAG, "onHandleIntent action:" + action);
        }
        
        if (EmailExternalConstants.ACTION_DIRECT_SEND.equals(action)) {
            int flag = extraIntent.getIntExtra(EXTRA_FLAG, -1);
            if (SAVE_MESSAGE_WITHOUT_SENDING == flag) {
                handleSaveMail(extraIntent);
            } else {
                resetTag();
                handleSendMail(extraIntent);
            }
        } else if (EmailExternalConstants.ACTION_UPDATE_INBOX.equals(action)) {
            handleUpdateMail(extraIntent);
        }
    }

    //Only for Send Mail Action.
    private void resetTag(){
        mSavetoSent = false;
        mUri = null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mControllerCallback = new ControllerCallbacks();
        mExternalHandler = new ExternalHandler();
        if (Email.DEBUG) {
            Logging.d(TAG, "onCreate");
        }
    }

    /**
     * Update special folder according to the account id from intent.
     * each of account will receive a update result.
     * @param intent This intent exist an account id,may be contain mailbox id.
     */
    private void handleUpdateMail(Intent intent) {
        boolean needCallback = intent.getBooleanExtra(EXTRA_NEED_RESULT, false);
        long[] accountIdArray = intent.getLongArrayExtra(EXTRA_ACCOUNT_ARRAY);

        // check account array whether or not is valid
        if (accountIdArray == null || accountIdArray.length == 0) {
            Logging.d(TAG, "UpdateInbox-->extra accout array is null, needCallback:"
                    + needCallback);
            if (needCallback) {
                boradUpdateResult(NO_UPDATE_ACCOUNT, false);
            }
            return;
        }

        //Loop handle 
        int size = accountIdArray.length;
        for (int i = 0; i < size; i++) {
            long accountId = accountIdArray[i];
            if (Email.DEBUG) {
                Logging.d(TAG, " UpdateInbox-->accountId:" + accountId);
            }
            Account account = Account.restoreAccountWithId(this, accountId);
            if (null == account) {
                Logging.d(TAG, "AccountID[" + accountId + "] not exist in DB");
                if (needCallback) {
                    boradUpdateResult(accountId, false);
                }
                continue;
            }
            long inboxId = mController.findOrCreateMailboxOfType(accountIdArray[i],
                    Mailbox.TYPE_INBOX);
            if (needCallback) {
                /**
                 * Calculate counts of mailbox update operations
                 */
                EmailExternalUtils.updateMail(inboxId, true);
            }
            try{
                mController.updateMailbox(accountId, inboxId, mControllerCallback);
            }catch (RemoteException e) {
                Logging.e(TAG, "handleUpdateMail catch RemoteException : " + e.toString());
                if (needCallback) {
                    boradUpdateResult(accountId, false);
                }
            }
        }
    }

    /**
     * Send an entire email package from intent
     * 
     * @param intent
     */
    private void handleSendMail(Intent intent) {
        int flag = intent.getIntExtra(EXTRA_FLAG, -1);
        Uri uri = (Uri) intent.getParcelableExtra(EXTRA_STREAM);
        mUri = uri;
        long accountId = intent.getLongExtra(EmailExternalConstants.EXTRA_ACCOUNT, -1);
        // 1 check account id
        if (-1 == accountId) {
            Logging.d(TAG, "EXTRA_ACCOUNT not exist in intent.");
            broadSendResult(accountId, false);
            return;
        }
        
        //2 check account exist
        Account account = Account.restoreAccountWithId(this, accountId);
        if (null == account) {
            Logging.d(TAG, "Account not exist in DB.");
            broadSendResult(accountId, false);
            return;
        }
        
        boolean saveToSent = (flag == SENDING_MESSAGE_WITH_SENDBOX) ? true : false;
        mSavetoSent = saveToSent;
        Logging.d(TAG, "call Controller sendMail accountId = " + accountId +
                " ,uri = " + uri + ", saveToSent = " + saveToSent);
        mController.sendMailForBT(accountId, uri, mControllerCallback, saveToSent);
    }

    /**
     * just save a mail to the corresponding mail(without sending)
     * Note : 
     *  1. Save to the following mailbox: inbox,draft,sent,trash,juck.
     *  2. Email need not any feedback and MAP will detect the email database by itself.
     * @param intent
     */
    private void handleSaveMail(Intent intent) {
        int mailboxType = intent.getIntExtra(EXTRA_MAILBOX_TYPE, -1);
        long accountId = intent.getLongExtra(EmailExternalConstants.EXTRA_ACCOUNT, -1);
        if ((-1 == mailboxType)
                || (Mailbox.TYPE_MAIL == mailboxType)
                || (Mailbox.TYPE_PARENT == mailboxType)
                || (Mailbox.TYPE_OUTBOX == mailboxType)) {
            Logging.d(TAG, "Not get the MailBoxType " + mailboxType);
            broadSaveResult(accountId, false);
            return;
        }
        
        Uri uri = (Uri) intent.getParcelableExtra(EXTRA_STREAM);
        Logging.d(TAG, "Savemail Stream:" + uri + ",accountId:" + accountId);

        // 1 check account id
        if (-1 == accountId) {
            Logging.d(TAG, "EXTRA_ACCOUNT not exist in intent.");
            broadSaveResult(accountId, false);
            return;
        }
        Account account = Account.restoreAccountWithId(this, accountId);
        if (null == account) {
            Logging.d(TAG, "AccountID[" + accountId + "] not exist in DB.");
            broadSaveResult(accountId, false);
            return;
        }

        InputStream inputStream = null;
        MimeMessage mimeMessage = null;

        // 2 Get inputStream from URI,and Parse inputStream to mimeMessage
        int result = EmailExternalConstants.RESULT_FAIL;
        try {
            inputStream = getContentResolver().openInputStream(uri);
            mimeMessage = new MimeMessage(inputStream);
            result = EmailExternalConstants.RESULT_SUCCESS;
        } catch (FileNotFoundException e) {
            Logging.e(TAG, "1 Open file failed,uri:" + uri, e);
        } catch (Exception e) {
            Logging.e(TAG, "Error while pasring inputstream:" + e.getMessage());
        } finally {
            try {
                if (null != inputStream) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Logging.e(TAG, "1 Error while closing fie.", e);
            }
            if (result == EmailExternalConstants.RESULT_FAIL) {
                broadSaveResult(accountId, false);
                return;
            }
        }
        
        // 3 save messages
        try {
            saveMessage(mimeMessage, accountId, mailboxType);
        } catch (MessagingException me) {
            Logging.e(TAG, "Error while copying downloaded message."
                    + me.getMessage());
        } catch (RuntimeException rte) {
            Logging.e(TAG, "Error while storing downloaded message."
                    + rte.getMessage());
        } catch (IOException ioe) {
            Logging.e(TAG, "Error while storing attachment." + ioe.getMessage());
        }
    }

    private boolean saveMessage(com.android.emailcommon.mail.Message mimeMessage, long accountId,
            int mailboxType) throws MessagingException, IOException {
        boolean isSuccess = true;
        // Construct an empty Message
        EmailContent.Message localMessage = new EmailContent.Message();
        long mailboxId = -1;
        mailboxId = Mailbox.findMailboxOfType(this, accountId, mailboxType);
        fillMessage(localMessage, mimeMessage, accountId, mailboxId);
        Logging.d(TAG, "Save Message to local database begin.");
       
        mController.saveToMailbox(localMessage, mailboxType);
        //mController.saveToMailbox(localMessage, Mailbox.TYPE_SENT);
        
        Logging.d(TAG, "Save Message to local database end.");

        ArrayList<Part> viewables = new ArrayList<Part>();
        ArrayList<Part> attachments = new ArrayList<Part>();
        MimeUtility.collectParts(mimeMessage, viewables, attachments);

        /** mMailboxKey has been set up by above saveToMailbox() */
        /*
        // set Mailbox ID for Body Update
        Mailbox folder = Mailbox.restoreMailboxOfType(this, accountId,
                mailboxType);
        localMessage.mMailboxKey = folder.mId;
        */
        EmailContent.Body body = new EmailContent.Body();
        ConversionUtilities.updateBodyFields(body, localMessage, viewables);

        boolean hasAttachments = attachments.size() > 0;
        localMessage.mFlagAttachment = hasAttachments;
        localMessage.mText = body.mTextContent;
        localMessage.mHtmlReply = body.mHtmlReply;
        localMessage.mTextReply = body.mTextReply;
        localMessage.mHtmlReply = body.mHtmlReply;
        localMessage.mHtmlReply = body.mHtmlReply;
        Logging.d(TAG, "Update Message to local database begin.");
        
        // Commit the message & body to the local store immediately
        saveOrUpdate(localMessage);
        saveOrUpdate(body);
        Logging.d(TAG, "Update Message to local database end. accountId = " + accountId
                + " mailboxId = " + mailboxId);
        Logging.d(TAG, "MessageInfor " + (null ==localMessage? "NUll": localMessage.mId));
        // process (and save) attachments
        if (hasAttachments) {
            try {
                ConversionUtilities.updateAttachments(this, localMessage, attachments);
                Logging.d(TAG, "Update Message attachments. " );
            } catch (IOException ioe) {
                isSuccess = false;
                Logging.e(TAG, "Error while copying message attachment." + ioe);
                throw ioe;
            }
        }

        ContentValues cv = new ContentValues();
        cv.put(EmailContent.MessageColumns.FLAG_ATTACHMENT, localMessage.mFlagAttachment);
        cv.put(EmailContent.MessageColumns.FLAG_LOADED, localMessage.mFlagLoaded);
        Uri uri = ContentUris.withAppendedId(EmailContent.Message.CONTENT_URI, localMessage.mId);
        getContentResolver().update(uri, cv, null, null);
        
        return isSuccess;
    }

    /**
     * @param localMessage The message we'd like to write into the DB
     * @param mimeMessage The message must be a MimeMessage
     * @param accountId
     * @param mailboxId
     * @throws MessagingException
     */
    private void fillMessage(EmailContent.Message localMessage,
            com.android.emailcommon.mail.Message mimeMessage, long accountId, long mailboxId)
            throws MessagingException {
        // Set message Field from a MimeMessage
        LegacyConversions.updateMessageFields(localMessage, mimeMessage, accountId, mailboxId);

        // reset field
        localMessage.mFlagRead = true;
        localMessage.mFlagLoaded = EmailContent.Message.FLAG_LOADED_COMPLETE;

        localMessage.mDisplayName = makeDisplayName(localMessage.mTo, localMessage.mCc,
                localMessage.mBcc);
        // localMessage.mDisplayName = from[0].toFriendly();

        if (localMessage.mMessageId == null || localMessage.mMessageId.length() == 0) {
            localMessage.mMessageId = Utility.generateMessageId();
        }
        if (localMessage.mTimeStamp == 0) {
            localMessage.mTimeStamp = System.currentTimeMillis();
        }
    }

    /*
     * Computes a short string indicating the destination of the message based
     * on To, Cc, Bcc. If only one address appears, returns the friendly form of
     * that address. Otherwise returns the friendly form of the first address
     * appended with "and N others".
     */
    private String makeDisplayName(String packedTo, String packedCc, String packedBcc) {
        Address first = null;
        int nRecipients = 0;
        for (String packed : new String[] {
                packedTo, packedCc, packedBcc
        }) {
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
        return getString(R.string.message_compose_display_name, friendly, nRecipients - 1);
    }

    private void saveOrUpdate(EmailContent content) {
        if (content.isSaved()) {
            content.update(this, content.toContentValues());
        } else {
            content.save(this);
        }
    }

    private final class ExternalHandler extends Handler {
        private static final int MSG_UPDATE_CALLBACK = 0x10;
        private static final int MSG_SEND_CALLBACK = 0x11;

        @Override
        public void handleMessage(Message msg) {
            int result = Integer.valueOf((Integer) msg.obj);
            switch (msg.what) {
                case MSG_SEND_CALLBACK:
                    handleSendCallback(result, msg.arg1, msg.arg2);
                    break;
                case MSG_UPDATE_CALLBACK:
                    handleUpdateCallback(result, msg.arg1, msg.arg2);
                    break;
                default:
                    break;
            }
        }

        private void postSendCallbackMessage(int result, long accountId, int resultType) {
            android.os.Message msg = obtainMessage(MSG_SEND_CALLBACK);
            msg.arg1 = (int) accountId;
            msg.arg2 = (int) resultType;
            msg.obj = result;
            sendMessage(msg);
        }

        private void postUpdateCallbackMessage(int result, long accountId, long mailboxId) {
            android.os.Message msg = obtainMessage(MSG_UPDATE_CALLBACK);
            msg.arg1 = (int) accountId;
            msg.arg2 = (int) mailboxId;
            msg.obj = result;
            sendMessage(msg);
        }
    }

    private class ControllerCallbacks extends EmailExternalCalls.Stub {

        public void sendCallback(int result, long accountId, int resultType) {
            mExternalHandler.postSendCallbackMessage(result, accountId, resultType);
        }

        public void updateCallback(int result, long accountId, long mailboxId) {
            mExternalHandler.postUpdateCallbackMessage(result, accountId, mailboxId);
        }
    }

    public void setControllerCallbacks(EmailExternalCalls.Stub callbacks) {
        mControllerCallback = callbacks;
    }

    private void handleUpdateCallback(int result, long accountId, long mailboxId) { 
        boolean canSend = EmailExternalUtils.canSendBroadcast(mailboxId);
        Logging.d(TAG, "+++++handleUpdateCallback result = " + result +
                       "canSend:" + canSend + ", account:" + accountId + ",mailbox:" + mailboxId);
        if (canSend) {
            EmailExternalUtils.updateMail(mailboxId, false);
            boradUpdateResult(accountId, result == EmailExternalConstants.RESULT_SUCCESS);
        }
    }

    private void handleSendCallback(int result, long accountId, int resultType) {
        /**
         * Only send success and need to save, do save operation. TODO: Only
         * save message for SEND result and the DELIVER result need be check. if
         * not this message may be saved twice.
         */
        if (mSavetoSent && (EmailExternalConstants.RESULT_SUCCESS == result)
                && (EmailExternalConstants.TYPE_SEND == resultType)) {
            InputStream inputStream = null;
            MimeMessage mimeMessage = null;
            try {
                inputStream = getContentResolver().openInputStream(mUri);
                mimeMessage = new MimeMessage(inputStream);
                saveMessage(mimeMessage, accountId, Mailbox.TYPE_SENT);
            } catch (MessagingException me) {
                Logging.e(TAG, "Error while copying downloaded message." + me.getMessage());
            } catch (RuntimeException rte) {
                Logging.e(TAG, "Error while storing downloaded message." + rte.getMessage());
            } catch (IOException ioe) {
                Logging.e(TAG, "Error while storing attachment." + ioe.getMessage());
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            Logging.d(TAG, "Not send message success or not need save . result = " + result
                    + " mSavetoSent = " + mSavetoSent);
        }
        //Send result
        Logging.d(TAG, "*****Handler sendCallback: result = " + result + ", account = " + accountId
                + ",resultType = " + resultType);
        String action = EmailExternalConstants.ACTION_SEND_RESULT;
        if (resultType == EmailExternalConstants.TYPE_DELIVER) {
            action = EmailExternalConstants.ACTION_DELIVER_RESULT;
        }
        sendResult(action, accountId, result);
    }

    /**
     * Send broadcast mail updating result:
     * account + result(0: success ;1: not success )
     * @param accountId
     * @param success
     */
    public void boradUpdateResult(long accountId, boolean success) {
        int result = success ? EmailExternalConstants.RESULT_SUCCESS
                : EmailExternalConstants.RESULT_FAIL;
        sendResult(EmailExternalConstants.ACTION_UPDATE_INBOX_RESULT, accountId, result);
    }

    /**
     * Start broadcast mail sending result:
     * account + result(0: success ;1: not success )
     * @param accountId
     * @param success
     */
    public void broadSendResult(long accountId, boolean success) {
        int result = success ? EmailExternalConstants.RESULT_SUCCESS
                : EmailExternalConstants.RESULT_FAIL;
        sendResult(EmailExternalConstants.ACTION_SEND_RESULT, accountId, result);
    }

    /**
     * Start broadcast mail saving result:
     * @param accountId
     * @param success
     */
    public void broadSaveResult(long accountId, boolean success) {
        //Don't do any feedback for Save Message Action.
    }
    
    public void sendResult(String action, long accountId, int result) {
        Logging.d(TAG, ">>> sendResult accountID = " + accountId + " result = " + result
                + "action = " + action);
        Intent intent = new Intent(action);
        intent.putExtra(EmailExternalConstants.EXTRA_ACCOUNT, accountId);
        intent.putExtra(EmailExternalConstants.EXTRA_RESULT, result);
        this.sendBroadcast(intent);
    }
    
}
