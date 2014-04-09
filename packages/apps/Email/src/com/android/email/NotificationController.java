/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.email;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.Log;

import com.android.email.activity.ContactStatusLoader;
import com.android.email.activity.Welcome;
import com.android.email.activity.setup.AccountSecurity;
import com.android.email.activity.setup.AccountSettings;
import com.android.emailcommon.Logging;
import com.android.emailcommon.mail.Address;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.AccountColumns;
import com.android.emailcommon.provider.EmailContent.Attachment;
import com.android.emailcommon.provider.EmailContent.MailboxColumns;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.EmailContent.MessageColumns;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.utility.Utility;
import com.google.common.annotations.VisibleForTesting;
import com.mediatek.email.emailvip.VipMemberCache;
import com.mediatek.email.emailvip.VipNotificationStyle;
import com.mediatek.notification.NotificationManagerPlus;
import com.mediatek.notification.NotificationPlus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Class that manages notifications.
 */
public class NotificationController {
    private static final int NOTIFICATION_ID_SECURITY_NEEDED = 1;
    /** Reserved for {@link com.android.exchange.CalendarSyncEnabler} */
    @SuppressWarnings("unused")
    private static final int NOTIFICATION_ID_EXCHANGE_CALENDAR_ADDED = 2;
    private static final int NOTIFICATION_ID_ATTACHMENT_WARNING = 3;
    private static final int NOTIFICATION_ID_PASSWORD_EXPIRING = 4;
    private static final int NOTIFICATION_ID_PASSWORD_EXPIRED = 5;

    private static final int NOTIFICATION_ID_BASE_NEW_MESSAGES = 0x10000000;
    private static final int NOTIFICATION_ID_BASE_LOGIN_WARNING = 0x20000000;
    private static final int NOTIFICATION_ID_BASE_SEND_WARNING = 0x30000000;
    private static final int NOTIFICATION_ID_BASE_VIP_MESSAGES = 0x40000000;

    /// M: selection to retrieve new coming messages @{
    private final static String MESSAGE_SELECTION =
            MessageColumns.MAILBOX_KEY + "=? AND " + MessageColumns.ID + ">? AND "
            + MessageColumns.FLAG_READ + "=0 AND " + Message.FLAG_LOADED_SELECTION;
    // @}

    private static NotificationThread sNotificationThread;
    private static Handler sNotificationHandler;
    private static NotificationController sInstance;
    ///M: Modified for testing
    private Context mContext;
    private final NotificationManager mNotificationManager;
    private final AudioManager mAudioManager;
    private final Bitmap mGenericSenderIcon;
    private final Bitmap mGenericMultipleSenderIcon;
    private final Clock mClock;
    // TODO We're maintaining all of our structures based upon the account ID. This is fine
    // for now since the assumption is that we only ever look for changes in an account's
    // INBOX. We should adjust our logic to use the mailbox ID instead.
    /** Maps account id to the message data */
    private final HashMap<Long, ContentObserver> mNotificationMap;
    private ContentObserver mAccountObserver;
    private ContentObserver mUnreadCountObserver;
    /**
     * Suspend notifications for this account. If {@link Account#NO_ACCOUNT}, no
     * account notifications are suspended. If {@link Account#ACCOUNT_ID_COMBINED_VIEW},
     * notifications for all accounts are suspended.
     */
    private long mSuspendAccountId = Account.NO_ACCOUNT;

    /**
     * Timestamp indicating when the last message notification sound was played.
     * Used for throttling.
     */
    private long mLastMessageNotifyTime;

    /**
     * Minimum interval between notification sounds.
     * Since a long sync (either on account setup or after a long period of being offline) can cause
     * several notifications consecutively, it can be pretty overwhelming to get a barrage of
     * notification sounds. Throttle them using this value.
     */
    private static final long MIN_SOUND_INTERVAL_MS = 15 * 1000; // 15 seconds
    private static final String TAG = "NotificationController";

    private static boolean isRunningJellybeanOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    /** M: Used for VIP notification. @{ */
    private final HashMap<Long, ArrayList<Long>> mNeedNotifyVIPMails = new HashMap<Long, ArrayList<Long>>();
    private long mLastVipMessageNotifyTime = 0;
    private long mVipNotifiedMessageId;
    private int mVipNotifiedMessageCount;
    /** @} */

    /** Constructor */
    @VisibleForTesting
    NotificationController(Context context, Clock clock) {
        mContext = context.getApplicationContext();
        mNotificationManager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mGenericSenderIcon = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.ic_contact_picture);
        mGenericMultipleSenderIcon = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.ic_notification_multiple_mail_holo_dark);
        mClock = clock;
        mNotificationMap = new HashMap<Long, ContentObserver>();
    }

    /**
     * For testing only: Inject context into already-created instance
     */
    @VisibleForTesting
    /* package */void setContext(Context context) {
        mContext = context;
    }
    /** Singleton access */
    public static synchronized NotificationController getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new NotificationController(context, Clock.INSTANCE);
        }
        return sInstance;
    }

    /**
     * Return whether or not a notification, based on the passed-in id, needs to be "ongoing"
     * @param notificationId the notification id to check
     * @return whether or not the notification must be "ongoing"
     */
    private boolean needsOngoingNotification(int notificationId) {
        // "Security needed" must be ongoing so that the user doesn't close it; otherwise, sync will
        // be prevented until a reboot.  Consider also doing this for password expired.
        return notificationId == NOTIFICATION_ID_SECURITY_NEEDED;
    }

    /**
     * Returns a {@link Notification.Builder} for an event with the given account. The account
     * contains specific rules on ring tone usage and these will be used to modify the notification
     * behaviour.
     *
     * @param account The account this notification is being built for.
     * @param ticker Text displayed when the notification is first shown. May be {@code null}.
     * @param title The first line of text. May NOT be {@code null}.
     * @param contentText The second line of text. May NOT be {@code null}.
     * @param intent The intent to start if the user clicks on the notification.
     * @param largeIcon A large icon. May be {@code null}
     * @param number A number to display using {@link Builder#setNumber(int)}. May
     *        be {@code null}.
     * @param enableAudio If {@code false}, do not play any sound. Otherwise, play sound according
     *        to the settings for the given account.
     * @return A {@link Notification} that can be sent to the notification service.
     */
    private Notification.Builder createBaseAccountNotificationBuilder(Account account,
            String ticker, CharSequence title, String contentText, Intent intent, Bitmap largeIcon,
            Integer number, boolean enableAudio, boolean ongoing) {
        // Pending Intent
        PendingIntent pending = null;
        if (intent != null) {
            pending = PendingIntent.getActivity(
                    mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        // NOTE: the ticker is not shown for notifications in the Holo UX
        final Notification.Builder builder = new Notification.Builder(mContext)
                .setContentTitle(title)
                .setContentText(contentText)
                .setContentIntent(pending)
                .setLargeIcon(largeIcon)
                .setNumber(number == null ? 0 : number)
                .setSmallIcon(R.drawable.stat_notify_email_generic)
                .setWhen(mClock.getTime())
                .setTicker(ticker)
                .setOngoing(ongoing);

        if (enableAudio) {
            Logging.d(Logging.LOG_TAG, "[Notification] createAccountNotification for account: "
                    + account.mId);
            setupSoundAndVibration(builder, account);
        }

        return builder;
    }

    /**
     * Generic notifier for any account.  Uses notification rules from account.
     *
     * @param account The account this notification is being built for.
     * @param ticker Text displayed when the notification is first shown. May be {@code null}.
     * @param title The first line of text. May NOT be {@code null}.
     * @param contentText The second line of text. May NOT be {@code null}.
     * @param intent The intent to start if the user clicks on the notification.
     * @param notificationId The ID of the notification to register with the service.
     */
    private void showAccountNotification(Account account, String ticker, String title,
            String contentText, Intent intent, int notificationId) {
        Notification.Builder builder = createBaseAccountNotificationBuilder(account, ticker, title,
                contentText, intent, null, null, true, needsOngoingNotification(notificationId));
        mNotificationManager.notify(notificationId, builder.getNotification());
    }

    /**
     * Returns a notification ID for new message notifications for the given account.
     */
    private int getNewMessageNotificationId(long accountId) {
        // We assume accountId will always be less than 0x0FFFFFFF; is there a better way?
        return (int) (NOTIFICATION_ID_BASE_NEW_MESSAGES + accountId);
    }

    /**
     * Tells the notification controller if it should be watching for changes to the message table.
     * This is the main life cycle method for message notifications. When we stop observing
     * database changes, we save the state [e.g. message ID and count] of the most recent
     * notification shown to the user. And, when we start observing database changes, we restore
     * the saved state.
     * @param watch If {@code true}, we register observers for all accounts whose settings have
     *              notifications enabled. Otherwise, all observers are unregistered.
     */
    public void watchForMessages(final boolean watch) {
        if (Email.DEBUG) {
            Log.i(Logging.LOG_TAG, "Notifications being toggled: " + watch);
        }
        // Don't create the thread if we're only going to stop watching
        if (!watch && sNotificationThread == null) return;

        ensureHandlerExists();
        // Run this on the message notification handler
        sNotificationHandler.post(new Runnable() {
            @Override
            public void run() {
                ContentResolver resolver = mContext.getContentResolver();
                if (!watch) {
                    unregisterUnreadCountObserver();
                    unregisterMessageNotification(Account.ACCOUNT_ID_COMBINED_VIEW);
                    if (mAccountObserver != null) {
                        resolver.unregisterContentObserver(mAccountObserver);
                        mAccountObserver = null;
                    }

                    // tear down the event loop
                    sNotificationThread.quit();
                    sNotificationThread = null;
                    return;
                }

                registerUnreadCountObserver();
                // otherwise, start new observers for all notified accounts
                registerMessageNotification(Account.ACCOUNT_ID_COMBINED_VIEW);
                // If we're already observing account changes, don't do anything else
                if (mAccountObserver == null) {
                    if (Email.DEBUG) {
                        Log.i(Logging.LOG_TAG, "Observing account changes for notifications");
                    }
                    mAccountObserver = new AccountContentObserver(sNotificationHandler, mContext);
                    resolver.registerContentObserver(Account.NOTIFIER_URI, true, mAccountObserver);
                }
            }
        });
    }

    /**
     * Temporarily suspend a single account from receiving notifications. NOTE: only a single
     * account may ever be suspended at a time. So, if this method is invoked a second time,
     * notifications for the previously suspended account will automatically be re-activated.
     * @param suspend If {@code true}, suspend notifications for the given account. Otherwise,
     *              re-activate notifications for the previously suspended account.
     * @param accountId The ID of the account. If this is the special account ID
     *              {@link Account#ACCOUNT_ID_COMBINED_VIEW},  notifications for all accounts are
     *              suspended. If {@code suspend} is {@code false}, the account ID is ignored.
     */
    public void suspendMessageNotification(boolean suspend, long accountId) {
        if (mSuspendAccountId != Account.NO_ACCOUNT) {
            // we're already suspending an account; un-suspend it
            mSuspendAccountId = Account.NO_ACCOUNT;
        }
        if (suspend && accountId != Account.NO_ACCOUNT && accountId > 0L) {
            mSuspendAccountId = accountId;
            if (accountId == Account.ACCOUNT_ID_COMBINED_VIEW) {
                // Only go onto the notification handler if we really, absolutely need to
                ensureHandlerExists();
                sNotificationHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (long accountId : mNotificationMap.keySet()) {
                            mNotificationManager.cancel(getNewMessageNotificationId(accountId));
                        }
                    }
                });
            } else {
                mNotificationManager.cancel(getNewMessageNotificationId(accountId));
            }
            /// M: Suspend the VIP notification
            postVipMailNotification(true, accountId);
        }
    }

    /**
     * Ensures the notification handler exists and is ready to handle requests.
     */
    private static synchronized void ensureHandlerExists() {
        if (sNotificationThread == null) {
            sNotificationThread = new NotificationThread();
            sNotificationHandler = new Handler(sNotificationThread.getLooper());
        }
    }

    /**
     * Registers an observer for changes to the INBOX for the given account. Since accounts
     * may only have a single INBOX, we will never have more than one observer for an account.
     * NOTE: This must be called on the notification handler thread.
     * @param accountId The ID of the account to register the observer for. May be
     *                  {@link Account#ACCOUNT_ID_COMBINED_VIEW} to register observers for all
     *                  accounts that allow for user notification.
     */
    private void registerMessageNotification(long accountId) {
        ContentResolver resolver = mContext.getContentResolver();
        if (accountId == Account.ACCOUNT_ID_COMBINED_VIEW) {
            /** M: Support for vip notification. the necessary of notification
             *  would be decided before sending it. @{ */
            Cursor c = resolver.query(
                    Account.CONTENT_URI, EmailContent.ID_PROJECTION,
                    null, null, null);
            /** @} */
            try {
                while (c.moveToNext()) {
                    long id = c.getLong(EmailContent.ID_PROJECTION_COLUMN);
                    registerMessageNotification(id);
                }
            } finally {
                c.close();
            }
        } else {
            ContentObserver obs = mNotificationMap.get(accountId);
            if (obs != null) return;  // we're already observing; nothing to do

            Mailbox mailbox = Mailbox.restoreMailboxOfType(mContext, accountId, Mailbox.TYPE_INBOX);
            if (mailbox == null) {
                Log.w(Logging.LOG_TAG, "Could not load INBOX for account id: " + accountId);
                return;
            }
            if (Email.DEBUG) {
                Log.i(Logging.LOG_TAG, "Registering for notifications for account " + accountId);
            }
            ContentObserver observer = new MessageContentObserver(
                    sNotificationHandler, mContext, mailbox.mId, accountId);
            resolver.registerContentObserver(Message.NOTIFIER_URI, true, observer);
            mNotificationMap.put(accountId, observer);
            // Now, ping the observer for any initial notifications
            observer.onChange(true);
        }
    }

    /**
     * Unregisters the observer for the given account. If the specified account does not have
     * a registered observer, no action is performed. This will not clear any existing notification
     * for the specified account. Use {@link NotificationManager#cancel(int)}.
     * NOTE: This must be called on the notification handler thread.
     * @param accountId The ID of the account to unregister from. To unregister all accounts that
     *                  have observers, specify an ID of {@link Account#ACCOUNT_ID_COMBINED_VIEW}.
     */
    private void unregisterMessageNotification(long accountId) {
        ContentResolver resolver = mContext.getContentResolver();
        if (accountId == Account.ACCOUNT_ID_COMBINED_VIEW) {
            if (Email.DEBUG) {
                Log.i(Logging.LOG_TAG, "Unregistering notifications for all accounts");
            }
            // cancel all existing message observers
            for (ContentObserver observer : mNotificationMap.values()) {
                resolver.unregisterContentObserver(observer);
            }
            mNotificationMap.clear();
        } else {
            if (Email.DEBUG) {
                Log.i(Logging.LOG_TAG, "Unregistering notifications for account " + accountId);
            }
            ContentObserver observer = mNotificationMap.remove(accountId);
            if (observer != null) {
                resolver.unregisterContentObserver(observer);
            }
        }
    }

    private void registerUnreadCountObserver() {
        ContentResolver resolver = mContext.getContentResolver();

        if (mUnreadCountObserver != null) {
            return; // we're already observing; nothing to do
        }

        if (Email.DEBUG) {
            Log.i(Logging.LOG_TAG, "Registering unread count observer for launcher ");
        }
        ContentObserver observer = new UnreadMessagesCountObserver(
                sNotificationHandler, mContext);
        resolver.registerContentObserver(Message.NOTIFIER_URI, true, observer);
        mUnreadCountObserver = observer;
        // Now, ping the observer for any initial notifications
        observer.onChange(true);
    }

    private void unregisterUnreadCountObserver() {
        ContentResolver resolver = mContext.getContentResolver();

        if (Email.DEBUG) {
            Log.i(Logging.LOG_TAG,
                    "Unregistering unread count observer for launcher ");
        }
        if (mUnreadCountObserver != null) {
            resolver.unregisterContentObserver(mUnreadCountObserver);
            mUnreadCountObserver = null;
        }
    }

    /**
     * Returns a picture of the sender of the given message. If no picture is available, returns
     * {@code null}.
     *
     * NOTE: DO NOT CALL THIS METHOD FROM THE UI THREAD (DATABASE ACCESS)
     */
    private Bitmap getSenderPhoto(Message message) {
        Address sender = Address.unpackFirst(message.mFrom);
        if (sender == null) {
            return null;
        }
        String email = sender.getAddress();
        if (TextUtils.isEmpty(email)) {
            return null;
        }
        Bitmap photo = ContactStatusLoader.getContactInfo(mContext, email).mPhoto;

        if (photo != null) {
            final Resources res = mContext.getResources();
            final int idealIconHeight =
                    res.getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
            final int idealIconWidth =
                    res.getDimensionPixelSize(android.R.dimen.notification_large_icon_width);

            if (photo.getHeight() < idealIconHeight) {
                // We should scale this image to fit the intended size
                photo = Bitmap.createScaledBitmap(
                        photo, idealIconWidth, idealIconHeight, true);
            }
        }
        return photo;
    }

    /**
     * Returns a "new message" notification for the given account.
     *
     * NOTE: DO NOT CALL THIS METHOD FROM THE UI THREAD (DATABASE ACCESS)
     */
    @VisibleForTesting
    Notification createNewMessageNotification(long accountId, long mailboxId, Cursor messageCursor,
            long newestMessageId, int unseenMessageCount, int unreadCount, boolean quietUpdate) {
        final Account account = Account.restoreAccountWithId(mContext, accountId);
        if (account == null) {
            return null;
        }
        /** M: If this account not allow sent notification return null. @{ */
        boolean notifyNewMail = (account.mFlags & Account.FLAGS_NOTIFY_NEW_MAIL) != 0;
        if (!notifyNewMail) {
            return null;
        }
        /** @} */
        // Get the latest message
        final Message message = Message.restoreMessageWithId(mContext, newestMessageId);
        if (message == null) {
            return null; // no message found???
        }

        String senderName = Address.toFriendly(Address.unpack(message.mFrom));
        if (senderName == null) {
            senderName = ""; // Happens when a message has no from.
        }
        final boolean multipleUnseen = unseenMessageCount > 1;
        final Bitmap senderPhoto = multipleUnseen
                ? mGenericMultipleSenderIcon
                : getSenderPhoto(message);
        final SpannableString title = getNewMessageTitle(senderName, unseenMessageCount);
        // TODO: add in display name on the second line for the text, once framework supports
        // multiline texts.
        final String text = multipleUnseen
                ? account.mDisplayName
                : message.mSubject;
        final Bitmap largeIcon = senderPhoto != null ? senderPhoto : mGenericSenderIcon;
        final Integer number = unreadCount > 1 ? unreadCount : null;
        final Intent intent;
        if (unseenMessageCount > 1) {
            intent = Welcome.createOpenAccountInboxIntent(mContext, accountId);
        } else {
            intent = Welcome.createOpenMessageIntent(
                    mContext, accountId, mailboxId, newestMessageId);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        long now = mClock.getTime();
        /// M: Use a common method the create the notification.
        boolean enableAudio = !quietUpdate && (now - mLastMessageNotifyTime) > MIN_SOUND_INTERVAL_MS;
        String ticker = quietUpdate ? null : title.toString();
        Notification n = createNotification(account, messageCursor, multipleUnseen, message,
                ticker, title, text, intent, largeIcon, number, enableAudio, false);
        mLastMessageNotifyTime = now;
        return n;
    }

    /// M: Extract the creating notification process to be a common method.
    /// Create the Email's notification
    private Notification createNotification(Account account, Cursor messageCursor, boolean multipleUnseen,
            Message message, String ticker, CharSequence title, String contentText, Intent intent,
            Bitmap largeIcon, Integer number, boolean enableAudio, boolean ongoing) {
        final Notification.Builder builder = createBaseAccountNotificationBuilder(
                account, ticker, title, contentText,
                intent, largeIcon, number, enableAudio, false);
        if (isRunningJellybeanOrLater()) {
            // For a new-style notification
            if (multipleUnseen) {
                if (messageCursor != null) {
                    final int maxNumDigestItems = mContext.getResources().getInteger(
                            R.integer.max_num_notification_digest_items);
                    // The body of the notification is the account name, or the label name.
                    builder.setSubText(contentText);

                    Notification.InboxStyle digest = new Notification.InboxStyle(builder);

                    digest.setBigContentTitle(title);

                    int numDigestItems = 0;
                    // We can assume that the current position of the cursor is on the
                    // newest message
                    do {
                        final long messageId =
                                messageCursor.getLong(EmailContent.ID_PROJECTION_COLUMN);

                        // Get the latest message
                        final Message digestMessage =
                                Message.restoreMessageWithId(mContext, messageId);
                        if (digestMessage != null) {
                            final CharSequence digestLine =
                                    getSingleMessageInboxLine(mContext, digestMessage);
                            digest.addLine(digestLine);
                            numDigestItems++;
                        }
                    } while (numDigestItems <= maxNumDigestItems && messageCursor.moveToNext());

                    // We want to clear the content text in this case. The content text would have
                    // been set in createBaseAccountNotificationBuilder, but since the same string
                    // was set in as the subtext, we don't want to show a duplicate string.
                    builder.setContentText(null);
                }
            } else {
                // The notification content will be the subject of the conversation.
                builder.setContentText(getSingleMessageLittleText(mContext, message.mSubject));

                // The notification subtext will be the subject of the conversation for inbox
                // notifications, or will based on the the label name for user label notifications.
                builder.setSubText(account.mDisplayName);

                final Notification.BigTextStyle bigText = new Notification.BigTextStyle(builder);
                bigText.bigText(getSingleMessageBigText(mContext, message));
            }
        }
        return builder.getNotification();
    }

    /**
     * Sets the bigtext for a notification for a single new conversation
     * @param context
     * @param message New message that triggered the notification.
     * @return a {@link CharSequence} suitable for use in {@link Notification.BigTextStyle}
     */
    private static CharSequence getSingleMessageInboxLine(Context context, Message message) {
        final String subject = message.mSubject;
        final String snippet = message.mSnippet;
        final String senders = Address.toFriendly(Address.unpack(message.mFrom));

        final String subjectSnippet = !TextUtils.isEmpty(subject) ? subject : snippet;

        final TextAppearanceSpan notificationPrimarySpan =
                new TextAppearanceSpan(context, R.style.NotificationPrimaryText);

        if (TextUtils.isEmpty(senders)) {
            // If the senders are empty, just use the subject/snippet.
            return subjectSnippet;
        }
        else if (TextUtils.isEmpty(subjectSnippet)) {
            // If the subject/snippet is empty, just use the senders.
            final SpannableString spannableString = new SpannableString(senders);
            spannableString.setSpan(notificationPrimarySpan, 0, senders.length(), 0);

            return spannableString;
        } else {
            final String formatString = context.getResources().getString(
                    R.string.multiple_new_message_notification_item);
            final TextAppearanceSpan notificationSecondarySpan =
                    new TextAppearanceSpan(context, R.style.NotificationSecondaryText);

            final String instantiatedString = String.format(formatString, senders, subjectSnippet);

            final SpannableString spannableString = new SpannableString(instantiatedString);

            final boolean isOrderReversed = formatString.indexOf("%2$s") <
                    formatString.indexOf("%1$s");
            final int primaryOffset =
                    (isOrderReversed ? instantiatedString.lastIndexOf(senders) :
                     instantiatedString.indexOf(senders));
            final int secondaryOffset =
                    (isOrderReversed ? instantiatedString.lastIndexOf(subjectSnippet) :
                     instantiatedString.indexOf(subjectSnippet));
            spannableString.setSpan(notificationPrimarySpan,
                    primaryOffset, primaryOffset + senders.length(), 0);
            spannableString.setSpan(notificationSecondarySpan,
                    secondaryOffset, secondaryOffset + subjectSnippet.length(), 0);
            return spannableString;
        }
    }

    /**
     * Sets the bigtext for a notification for a single new conversation
     * @param context
     * @param subject Subject of the new message that triggered the notification
     * @return a {@link CharSequence} suitable for use in {@link Notification.ContentText}
     */
    private static CharSequence getSingleMessageLittleText(Context context, String subject) {
        if (subject == null) {
            return null;
        }
        final TextAppearanceSpan notificationSubjectSpan = new TextAppearanceSpan(
                context, R.style.NotificationPrimaryText);

        final SpannableString spannableString = new SpannableString(subject);
        spannableString.setSpan(notificationSubjectSpan, 0, subject.length(), 0);

        return spannableString;
    }


    /**
     * Sets the bigtext for a notification for a single new conversation
     * @param context
     * @param message New message that triggered the notification
     * @return a {@link CharSequence} suitable for use in {@link Notification.BigTextStyle}
     */
    private static CharSequence getSingleMessageBigText(Context context, Message message) {
        final TextAppearanceSpan notificationSubjectSpan = new TextAppearanceSpan(
                context, R.style.NotificationPrimaryText);

        final String subject = message.mSubject;
        final String snippet = message.mSnippet;

        if (TextUtils.isEmpty(subject)) {
            // If the subject is empty, just use the snippet.
            return snippet;
        }
        else if (TextUtils.isEmpty(snippet)) {
            // If the snippet is empty, just use the subject.
            final SpannableString spannableString = new SpannableString(subject);
            spannableString.setSpan(notificationSubjectSpan, 0, subject.length(), 0);

            return spannableString;
        } else {
            final String notificationBigTextFormat = context.getResources().getString(
                    R.string.single_new_message_notification_big_text);

            // Localizers may change the order of the parameters, look at how the format
            // string is structured.
            final boolean isSubjectFirst = notificationBigTextFormat.indexOf("%2$s") >
                    notificationBigTextFormat.indexOf("%1$s");
            final String bigText = String.format(notificationBigTextFormat, subject, snippet);
            final SpannableString spannableString = new SpannableString(bigText);

            final int subjectOffset =
                    (isSubjectFirst ? bigText.indexOf(subject) : bigText.lastIndexOf(subject));
            spannableString.setSpan(notificationSubjectSpan,
                    subjectOffset, subjectOffset + subject.length(), 0);

            return spannableString;
        }
    }

    /**
     * Creates a notification title for a new message. If there is only a single message,
     * show the sender name. Otherwise, show "X new messages".
     */
    @VisibleForTesting
    SpannableString getNewMessageTitle(String sender, int unseenCount) {
        String title;
        if (unseenCount > 1) {
            title = String.format(
                    mContext.getString(R.string.notification_multiple_new_messages_fmt),
                    unseenCount);
        } else {
            title = sender;
        }
        return new SpannableString(title);
    }

    /** Returns the system's current ringer mode */
    @VisibleForTesting
    int getRingerMode() {
        return mAudioManager.getRingerMode();
    }

    /** Sets up the notification's sound and vibration based upon account details. */
    @VisibleForTesting
    void setupSoundAndVibration(Notification.Builder builder, Account account) {
        final int flags = account.mFlags;
        final String ringtoneUri = account.mRingtoneUri;
        final boolean vibrate = (flags & Account.FLAGS_VIBRATE_ALWAYS) != 0;
        final boolean vibrateWhenSilent = (flags & Account.FLAGS_VIBRATE_WHEN_SILENT) != 0;
        final boolean isRingerSilent = getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
        Logging.d(Logging.LOG_TAG, "[Notification] setupSoundAndVibration vibrate is " + vibrate);
        int defaults = Notification.DEFAULT_LIGHTS;
        if (vibrate || (vibrateWhenSilent && isRingerSilent)) {
            defaults |= Notification.DEFAULT_VIBRATE;
        }
        Logging.d(Logging.LOG_TAG, "[Notification] setupSoundAndVibration default flag "
                + defaults);
        builder.setSound(TextUtils.isEmpty(ringtoneUri) ? null : Uri.parse(ringtoneUri))
            .setDefaults(defaults);
    }

    /**
     * Show (or update) a notification that the given attachment could not be forwarded. This
     * is a very unusual case, and perhaps we shouldn't even send a notification. For now,
     * it's helpful for debugging.
     *
     * NOTE: DO NOT CALL THIS METHOD FROM THE UI THREAD (DATABASE ACCESS)
     */
    public void showDownloadForwardFailedNotification(Attachment attachment) {
        final Account account = Account.restoreAccountWithId(mContext, attachment.mAccountKey);
        if (account == null) return;
        showAccountNotification(account,
                mContext.getString(R.string.forward_download_failed_ticker),
                mContext.getString(R.string.forward_download_failed_title),
                attachment.mFileName,
                null,
                NOTIFICATION_ID_ATTACHMENT_WARNING);
    }

    /**
     * Returns a notification ID for login failed notifications for the given account account.
     */
    private int getLoginFailedNotificationId(long accountId) {
        return NOTIFICATION_ID_BASE_LOGIN_WARNING + (int)accountId;
    }

    /**
     * Returns a notification ID for email sending failed notifications for the given account
     * account.
     */
    private int getSendFailedNotificationId(long accountId) {
        return NOTIFICATION_ID_BASE_SEND_WARNING + (int)accountId;
    }

    /**
     * Show (or update) a notification that there was a login failure for the given account.
     *
     * NOTE: DO NOT CALL THIS METHOD FROM THE UI THREAD (DATABASE ACCESS)
     */
    public void showLoginFailedNotification(long accountId) {
        final Account account = Account.restoreAccountWithId(mContext, accountId);
        if (account == null) return;
        showAccountNotification(account,
                mContext.getString(R.string.login_failed_ticker, account.mDisplayName),
                mContext.getString(R.string.login_failed_title),
                account.getDisplayName(),
                AccountSettings.createAccountSettingsIntent(mContext, accountId,
                        account.mDisplayName),
                getLoginFailedNotificationId(accountId));
    }

    public void showSendFailedNotification(long accountId) {
        final Account account = Account.restoreAccountWithId(mContext, accountId);
        if (account == null) {
            return;
        }
        showAccountNotification(account,
                mContext.getString(R.string.send_failed_ticker, account.mDisplayName),
                mContext.getString(R.string.send_failed_title),
                account.getDisplayName(),
                Welcome.createOpenAccountInboxIntent(mContext, accountId),
                getSendFailedNotificationId(accountId));
    }

    public void cancelSendFailedNotification(long accountId) {
        mNotificationManager.cancel(getSendFailedNotificationId(accountId));
    }

    /**
     * Cancels the login failed notification for the given account.
     */
    public void cancelLoginFailedNotification(long accountId) {
        mNotificationManager.cancel(getLoginFailedNotificationId(accountId));
    }

    /**
     * Show (or update) a notification that the user's password is expiring. The given account
     * is used to update the display text, but, all accounts share the same notification ID.
     *
     * NOTE: DO NOT CALL THIS METHOD FROM THE UI THREAD (DATABASE ACCESS)
     */
    public void showPasswordExpiringNotification(long accountId) {
        Account account = Account.restoreAccountWithId(mContext, accountId);
        if (account == null) return;

        Intent intent = AccountSecurity.actionDevicePasswordExpirationIntent(mContext,
                accountId, false);
        String accountName = account.getDisplayName();
        String ticker =
            mContext.getString(R.string.password_expire_warning_ticker_fmt, accountName);
        String title = mContext.getString(R.string.password_expire_warning_content_title);
        showAccountNotification(account, ticker, title, accountName, intent,
                NOTIFICATION_ID_PASSWORD_EXPIRING);
    }

    /**
     * Show (or update) a notification that the user's password has expired. The given account
     * is used to update the display text, but, all accounts share the same notification ID.
     *
     * NOTE: DO NOT CALL THIS METHOD FROM THE UI THREAD (DATABASE ACCESS)
     */
    public void showPasswordExpiredNotification(long accountId) {
        Account account = Account.restoreAccountWithId(mContext, accountId);
        if (account == null) return;

        Intent intent = AccountSecurity.actionDevicePasswordExpirationIntent(mContext,
                accountId, true);
        String accountName = account.getDisplayName();
        String ticker = mContext.getString(R.string.password_expired_ticker);
        String title = mContext.getString(R.string.password_expired_content_title);
        showAccountNotification(account, ticker, title, accountName, intent,
                NOTIFICATION_ID_PASSWORD_EXPIRED);
    }

    /**
     * Cancels any password expire notifications [both expired & expiring].
     */
    public void cancelPasswordExpirationNotifications() {
        mNotificationManager.cancel(NOTIFICATION_ID_PASSWORD_EXPIRING);
        mNotificationManager.cancel(NOTIFICATION_ID_PASSWORD_EXPIRED);
    }

    /**
     * Show (or update) a security needed notification. The given account is used to update
     * the display text, but, all accounts share the same notification ID.
     */
    public void showSecurityNeededNotification(Account account) {
        Intent intent = AccountSecurity.actionUpdateSecurityIntent(mContext, account.mId, true);
        String accountName = account.getDisplayName();
        String ticker =
            mContext.getString(R.string.security_notification_ticker_fmt, accountName);
        String title = mContext.getString(R.string.security_notification_content_title);
        showAccountNotification(account, ticker, title, accountName, intent,
                NOTIFICATION_ID_SECURITY_NEEDED);
    }

    /**
     * Cancels the security needed notification.
     */
    public void cancelSecurityNeededNotification() {
        mNotificationManager.cancel(NOTIFICATION_ID_SECURITY_NEEDED);
    }

    /**
     * Observer using for notify launcher change unread icon.
     * invoked whenever a message we're notifying the user about changes.
     */
    private static class UnreadMessagesCountObserver extends ContentObserver {
        private final Context mContext;
        private int mUnreadNumOfAllInbox = 0;

        public UnreadMessagesCountObserver(Handler handler, Context context) {
           super(handler);
           mContext = context;
        }

        @Override
        public void onChange(boolean selfChange) {
            int unreadCount = Mailbox.getUnreadCountByMailboxType(mContext, Mailbox.TYPE_INBOX);
            if (mUnreadNumOfAllInbox != unreadCount) {

                mUnreadNumOfAllInbox = unreadCount;
                /** M: MTK Dependence */
                notifyEmailUnreadNumber(mContext, mUnreadNumOfAllInbox);
            }
        }
    }
    /**
     * Observer invoked whenever a message we're notifying the user about changes.
     */
    private static class MessageContentObserver extends ContentObserver {
        private final static String[] MESSAGE_ID_AND_FROM_PROJECTION =
                new String[] {MessageColumns.ID, MessageColumns.FROM_LIST};
        /** A selection to get messages the user hasn't seen before */
        private final static String MESSAGE_SELECTION =
                MessageColumns.MAILBOX_KEY + "=? AND "
                + MessageColumns.ID + ">? AND "
                + MessageColumns.FLAG_READ + "=0 AND "
                + Message.FLAG_LOADED_SELECTION;
        private final Context mContext;
        private final long mMailboxId;
        private final long mAccountId;

        public MessageContentObserver(
                Handler handler, Context context, long mailboxId, long accountId) {
            super(handler);
            mContext = context;
            mMailboxId = mailboxId;
            mAccountId = accountId;
        }

        @Override
        public void onChange(boolean selfChange) {
            if (mAccountId == sInstance.mSuspendAccountId
                    || sInstance.mSuspendAccountId == Account.ACCOUNT_ID_COMBINED_VIEW) {
                /** M: Remove the VIP mails from the VIP notification
                 *  when the account has been suspended. @{ */
                sInstance.postVipMailNotification(true, mAccountId);
                /** @} */
                return;
            }

            ContentObserver observer = sInstance.mNotificationMap.get(mAccountId);
            if (observer == null) {
                // Notification for a mailbox that we aren't observing; account is probably
                // being deleted.
                Log.w(Logging.LOG_TAG, "Received notification when observer data was null");
                return;
            }
            Account account = Account.restoreAccountWithId(mContext, mAccountId);
            if (account == null) {
                Log.w(Logging.LOG_TAG, "Couldn't find account for changed message notification");
                return;
            }
            long oldMessageId = account.mNotifiedMessageId;
            int oldMessageCount = account.mNotifiedMessageCount;

            ContentResolver resolver = mContext.getContentResolver();
            Long lastSeenMessageId = Utility.getFirstRowLong(
                    mContext, ContentUris.withAppendedId(Mailbox.CONTENT_URI, mMailboxId),
                    new String[] { MailboxColumns.LAST_SEEN_MESSAGE_KEY },
                    null, null, null, 0);
            if (lastSeenMessageId == null) {
                // Mailbox got nuked. Could be that the account is in the process of being deleted
                Log.w(Logging.LOG_TAG, "Couldn't find mailbox for changed message notification");
                return;
            }

            Cursor c = resolver.query(
                    Message.CONTENT_URI, MESSAGE_ID_AND_FROM_PROJECTION,
                    MESSAGE_SELECTION,
                    new String[] { Long.toString(mMailboxId), Long.toString(lastSeenMessageId) },
                    MessageColumns.ID + " DESC");
            if (c == null) {
                // Couldn't find message info - things may be getting deleted in bulk.
                Log.w(Logging.LOG_TAG, "#onChange(); NULL response for message id query");
                return;
            }
            try {
                int newMessageCount = c.getCount();
                /// M: If support VIP notification, Filter the VIP mails out off the new mails,
                // So, the normal notification only notify the normal mails. And the VIP mails
                // will be notified latter.
                // Else clear the need notify VIP mails.
                Cursor nonVipCurosr = c;
                boolean notifyVip = Preferences.getPreferences(mContext).getVipNotification();
                if (notifyVip) {
                    nonVipCurosr = sInstance.filterVipMessages(c, mAccountId);
                } else {
                    sInstance.mNeedNotifyVIPMails.clear();
                }

                long newMessageId = 0L;
                if (nonVipCurosr.moveToFirst()) {
                    newMessageId = nonVipCurosr.getLong(EmailContent.ID_PROJECTION_COLUMN);
                }
                int newNonVipMessageCount = nonVipCurosr.getCount();
                if (newNonVipMessageCount == 0) {
                    // No messages to notify for; clear the notification
                    int notificationId = sInstance.getNewMessageNotificationId(mAccountId);
                    sInstance.mNotificationManager.cancel(notificationId);
                } else if (newMessageCount != oldMessageCount
                        || (newMessageId != 0 && newMessageId != oldMessageId)) {
                    // Either the count or last message has changed; update the notification
                    Integer unreadCount = Utility.getFirstRowInt(
                            mContext, ContentUris.withAppendedId(Mailbox.CONTENT_URI, mMailboxId),
                            new String[] { MailboxColumns.UNREAD_COUNT },
                            null, null, null, 0);
                    if (unreadCount == null) {
                        Log.w(Logging.LOG_TAG, "Couldn't find unread count for mailbox");
                        return;
                    }

                    /// M: post new notifications for new message @{
                    /// If the newMessageId less or equal oldMessageId, the newMessageCount must not equal
                    /// oldMessageCount, that means we not receive new non VIP mails but the unread
                    /// mail count has changed, so we only update the unread mails count quietly.
                    boolean quietUpdate = false;
                    if (newMessageId <= oldMessageId) {
                        quietUpdate = true;
                    }
                    sInstance.postNotificationForNewMessage(mAccountId, mMailboxId, nonVipCurosr, newMessageId,
                            newNonVipMessageCount, unreadCount, quietUpdate);
                    /// @}
                }
                // Save away the new values
                ContentValues cv = new ContentValues();
                cv.put(AccountColumns.NOTIFIED_MESSAGE_ID, newMessageId);
                cv.put(AccountColumns.NOTIFIED_MESSAGE_COUNT, newMessageCount);
                resolver.update(ContentUris.withAppendedId(Account.CONTENT_URI, mAccountId), cv,
                        null, null);
                /// M: Post the VIP mails notification
                sInstance.postVipMailNotification(false, Account.ACCOUNT_ID_COMBINED_VIEW);
            } finally {
                c.close();
            }
        }
    }

    /**
     * Observer invoked whenever an account is modified. This could mean the user changed the
     * notification settings.
     */
    private static class AccountContentObserver extends ContentObserver {
        private final Context mContext;
        public AccountContentObserver(Handler handler, Context context) {
            super(handler);
            mContext = context;
        }

        @Override
        public void onChange(boolean selfChange) {
            final ContentResolver resolver = mContext.getContentResolver();
            /** M: Support for vip notification. the necessary of notification
             *  would be decided before sending it. @{ */
            final Cursor c = resolver.query(
                Account.CONTENT_URI, EmailContent.ID_PROJECTION,
                null, null, null);
            /** @} */
            final HashSet<Long> newAccountList = new HashSet<Long>();
            final HashSet<Long> removedAccountList = new HashSet<Long>();
            if (c == null) {
                // Suspender time ... theoretically, this will never happen
                Log.wtf(Logging.LOG_TAG, "#onChange(); NULL response for account id query");
                return;
            }
            try {
                while (c.moveToNext()) {
                    long accountId = c.getLong(EmailContent.ID_PROJECTION_COLUMN);
                    newAccountList.add(accountId);
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
            // NOTE: Looping over three lists is not necessarily the most efficient. However, the
            // account lists are going to be very small, so, this will not be necessarily bad.
            // Cycle through existing notification list and adjust as necessary
            for (long accountId : sInstance.mNotificationMap.keySet()) {
                if (!newAccountList.remove(accountId)) {
                    // account id not in the current set of notifiable accounts
                    removedAccountList.add(accountId);
                }
            }
            // A new account was added to the notification list
            for (long accountId : newAccountList) {
                sInstance.registerMessageNotification(accountId);
            }
            // An account was removed from the notification list
            for (long accountId : removedAccountList) {
                sInstance.unregisterMessageNotification(accountId);
                int notificationId = sInstance.getNewMessageNotificationId(accountId);
                sInstance.mNotificationManager.cancel(notificationId);
                /// M: Also remove this account from VIP notification
                sInstance.postVipMailNotification(true, accountId);
            }
        }
    }

    /**
     * Thread to handle all notification actions through its own {@link Looper}.
     */
    private static class NotificationThread implements Runnable {
        /** Lock to ensure proper initialization */
        private final Object mLock = new Object();
        /** The {@link Looper} that handles messages for this thread */
        private Looper mLooper;

        NotificationThread() {
            new Thread(null, this, "EmailNotification").start();
            synchronized (mLock) {
                while (mLooper == null) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }

        @Override
        public void run() {
            synchronized (mLock) {
                Looper.prepare();
                mLooper = Looper.myLooper();
                mLock.notifyAll();
            }
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            Looper.loop();
        }
        void quit() {
            mLooper.quit();
        }
        Looper getLooper() {
            return mLooper;
        }
    }

    /**
     * M: MTK Dependence
     * 
     * Returns a "new message" notification for the given account.
     *
     * NOTE: IT ONLY POP UP WHEN USER PLAY A LIVE STREAM.
     * DO NOT CALL THIS METHOD FROM THE UI THREAD (DATABASE ACCESS).
     */
    NotificationPlus createNewMessageNotificationPlus(long accountId, long mailboxId,
            long messageId, int unseenMessageCount, int unreadCount) {
        final Account account = Account.restoreAccountWithId(mContext, accountId);
        if (account == null) {
            return null;
        }
        // Get the latest message
        final Message message = Message.restoreMessageWithId(mContext, messageId);
        if (message == null) {
            return null; // no message found???
        }

        String senderName = Address.toFriendly(Address.unpack(message.mFrom));
        if (senderName == null) {
            senderName = ""; // Happens when a message has no from.
        }
        final String title = String.format(mContext.getString(
                R.string.notification_multiple_new_messages_fmt), unseenMessageCount);
        final String text = message.mSubject;
        final Intent intent;
        if (unseenMessageCount > 1) {
            intent = Welcome.createOpenAccountInboxIntent(mContext, accountId);
        } else {
            intent = Welcome.createOpenMessageIntent(mContext, accountId, mailboxId, messageId);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pending = PendingIntent.getActivity(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationPlus notification = new NotificationPlus.Builder(mContext)
                .setTitle(title.toString())
                .setMessage(text)
                .setPositiveButton(mContext.getString(android.R.string.ok), pending).create();
        Logging.d(TAG, "Have created a new NotificationPlus for title : " + title.toString());
        return notification;
    }

    public static void notifyEmailUnreadNumber(Context context, int unreadNumber) {
        Intent i = new Intent();
        /** M: MTK Dependence @} */
        i.setAction(Intent.MTK_ACTION_UNREAD_CHANGED);
        i.putExtra(Intent.MTK_EXTRA_UNREAD_NUMBER, unreadNumber);
        i.putExtra(Intent.MTK_EXTRA_UNREAD_COMPONENT, new ComponentName("com.android.email",
                "com.android.email.activity.Welcome"));
        context.sendBroadcast(i);
        /** @} */
        Log.d(Logging.LOG_TAG,
                "!!!notifyEmailUnreadNumber: send broadcast to laucher : unreadCount = "
                        + unreadNumber);

        android.provider.Settings.System.putInt(context.getContentResolver(),
                "com_android_email_mtk_unread",
                    unreadNumber);
    }

    /**
     * M: post notifications for new messages
     * @param accountId
     * @param mailboxId
     * @param c
     * @param messageId
     * @param unseenMessageCount
     * @param unreadCount
     * @param quietUpdate only update the notification content but not pop up the ticker
     */
    void postNotificationForNewMessage(long accountId, long mailboxId, Cursor c, long messageId,
            int unseenMessageCount, int unreadCount, boolean quietUpdate) {
        Notification n = sInstance.createNewMessageNotification( accountId, mailboxId, c,
                messageId, unseenMessageCount, unreadCount, quietUpdate);
        if (n != null) {
            Logging.d(Logging.LOG_TAG, "[Notification] notify default flag "
                    + n.defaults);
            // Make the notification visible
            sInstance.mNotificationManager.notify(
                    sInstance.getNewMessageNotificationId(accountId), n);
        }
        /** M: MTK Dependence @} */
        NotificationPlus notiPlus =
                sInstance.createNewMessageNotificationPlus(accountId, mailboxId, messageId,
                        unseenMessageCount, unreadCount);
        if (notiPlus == null) {
            Logging.w(TAG, " NotificationPlus create failed ");
        } else {
            NotificationManagerPlus.notify(1, notiPlus);
            Logging.d(TAG, " NotificationManagerPlus will popup NotificationPlus ");
        }
        /** @} */
    }

    /**
     * M: Notify there are some new messages after boot complete, which means these messages arrived
     * before this boot time, and were not seen yet
     */
    public static void notifyOnBootCompleted() {
        ensureHandlerExists();
        sNotificationHandler.post(new Runnable() {
            @Override
            public void run() {
             // notify new messages for every account
                for (long accountId : sInstance.mNotificationMap.keySet()) {
                    if (accountId == sInstance.mSuspendAccountId
                            || sInstance.mSuspendAccountId == Account.ACCOUNT_ID_COMBINED_VIEW) {
                        return;
                    }

                    ContentObserver observer = sInstance.mNotificationMap.get(accountId);
                    if (observer == null) {
                        // Notification for a mailbox that we aren't observing; account is probably
                        // being deleted.
                        Logging.w(TAG, "Received notification when observer data was null");
                        return;
                    }
                    Account account = Account.restoreAccountWithId(sInstance.mContext, accountId);
                    if (account == null) {
                        Logging.w(TAG, "Couldn't find account for changed message notification");
                        return;
                    }
                    long messageId = account.mNotifiedMessageId;
                    int messageCount = account.mNotifiedMessageCount;
                    if (messageCount > 0) {
                        Mailbox mailbox = Mailbox.restoreMailboxOfType(sInstance.mContext, accountId,
                                Mailbox.TYPE_INBOX);
                        if (mailbox == null) {
                            Logging.w(TAG, "Could not load INBOX for account id: " + accountId);
                            return;
                        }
                        long mailboxId = mailbox.mId;

                        ContentResolver resolver = sInstance.mContext.getContentResolver();
                        Long lastSeenMessageId = Utility.getFirstRowLong(sInstance.mContext,
                                ContentUris.withAppendedId(Mailbox.CONTENT_URI, mailboxId),
                                new String[] { MailboxColumns.LAST_SEEN_MESSAGE_KEY },
                                null, null, null, 0);
                        if (lastSeenMessageId == null) {
                            // Mailbox got nuked. Could be that the account is in the process of being deleted
                            Logging.w(TAG, "Couldn't find mailbox for changed message notification");
                            return;
                        }

                        Cursor c = resolver.query(
                                Message.CONTENT_URI, EmailContent.ID_PROJECTION,
                                MESSAGE_SELECTION,
                                new String[] { Long.toString(mailboxId),Long.toString(lastSeenMessageId) },
                                MessageColumns.ID + " DESC");
                        if (c == null) {
                            // Couldn't find message info - things may be getting deleted in bulk.
                            Logging.w(TAG, "#notifyOnBootCompleted(); NULL response for message id query");
                            return;
                        }
                        try {
                            Integer unreadCount = Utility.getFirstRowInt(
                                    sInstance.mContext, ContentUris.withAppendedId(Mailbox.CONTENT_URI, mailboxId),
                                    new String[] { MailboxColumns.UNREAD_COUNT },
                                    null, null, null, 0);
                            if (unreadCount == null) {
                                Logging.w(TAG, "Couldn't find unread count for mailbox");
                                return;
                            }
                            if (c.moveToFirst()) {
                                sInstance.postNotificationForNewMessage(accountId, mailboxId, c, messageId,
                                        messageCount, unreadCount, false);
                            }
                        } finally {
                            c.close();
                        }
                    }
                }
            }
        });
    }

    /// M: Post the show notification action into handler. And temporarily suspend a
    /// single account from receiving VIP notifications if the account has been suspended.
    public void postVipMailNotification(final boolean isSuspend, final long accountId) {
        if (accountId == Account.NO_ACCOUNT) {
            return;
        }
        ensureHandlerExists();
        Runnable runnable;
        if (accountId == Account.ACCOUNT_ID_COMBINED_VIEW) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    if (isSuspend) {
                        mNeedNotifyVIPMails.clear();
                    }
                    sendOrCancelVipNotification();
                }
            };
        } else {
            runnable = new Runnable() {
                @Override
                public void run() {
                    if (!isSuspend || mNeedNotifyVIPMails.remove(accountId) != null) {
                        sendOrCancelVipNotification();
                    }
                }
            };
        }
        sNotificationHandler.post(runnable);
    }

    /// M: Send or Cancel VIP notification according is there any VIP mails
    /// existed in the mNeedNotifyVIPMails.
    void sendOrCancelVipNotification() {
        /// If there were VIP mails, send the VIP notification
        if (mNeedNotifyVIPMails.size() > 0) {
            Notification n = sInstance.createVipMessageNotification();
            Logging.d(TAG, "Post VIP Notification: " + n);
            if (n != null) {
                // Make the notification visible
                sInstance.mNotificationManager.notify(NOTIFICATION_ID_BASE_VIP_MESSAGES, n);
            }
        } else {
            Logging.d(TAG, "Cancel VIP Notification");
            mVipNotifiedMessageCount = 0;
            mVipNotifiedMessageId = 0;
            sInstance.mNotificationManager.cancel(NOTIFICATION_ID_BASE_VIP_MESSAGES);
        }
    }

    /// M: Put all the VIP mails of mNeedNotifyVIPMails into a cursor and order in descending
    private Cursor getVipMessageCursor() {
        ArrayList<Long> idList = new ArrayList<Long>();
        for (ArrayList<Long> msgIds : mNeedNotifyVIPMails.values()) {
            idList.addAll(msgIds);
        }
        Collections.sort(idList, Collections.reverseOrder());
        MatrixCursor vipMessageCursor = new MatrixCursor(Message.ID_COLUMN_PROJECTION);
        for (long id : idList) {
            vipMessageCursor.newRow().add(id);
        }
        return vipMessageCursor;
    }

    /// M: Create the VIP notification. The VIP mails come from the mNeedNotifyVIPMails.
    Notification createVipMessageNotification() {
        int vipAccountCount = mNeedNotifyVIPMails.size();
        if (vipAccountCount <= 0) {
            Logging.d(TAG, "createVipMessageNotification no VIP mails, return null");
            /// No VIP mails
            return null;
        }

        Account account = null;
        if (vipAccountCount == 1) {
            long accountId = (Long)mNeedNotifyVIPMails.keySet().toArray()[0];
            account = Account.restoreAccountWithId(mContext, accountId);
        } else {
            account = new Account();
            account.mId = Account.ACCOUNT_ID_COMBINED_VIEW;
            account.mDisplayName = String.format(
                    mContext.getString(R.string.vip_emails_from_accounts_fmt),
                    vipAccountCount);
        }
        if (account == null) {
            return null;
        }
        VipNotificationStyle.updateVipSoundAndVibration(mContext, account);
        // Get the latest message
        Cursor messageCursor = getVipMessageCursor();
        if (!messageCursor.moveToFirst()) {
            return null;
        }
        long newestMessageId = messageCursor.getLong(Message.ID_COLUMNS_ID_COLUMN);
        int unseenMessageCount = messageCursor.getCount();
        if (unseenMessageCount == mVipNotifiedMessageCount &&
                (newestMessageId == 0 || newestMessageId == mVipNotifiedMessageId)) {
            Logging.d(TAG, "createVipMessageNotification same as previous, do not notify again");
            return null;
        }
        // If the new VIP mails less then last notified, we only update the notification
        // content, do not show the ticker view.
        boolean quietUpdate = false;
        if (unseenMessageCount < mVipNotifiedMessageCount) {
            quietUpdate = true;
        }
        // If user disabled VIP notification function now, but the VIP unseen messages changed,
        // should also update the VIP notification.
        boolean notifyVip = Preferences.getPreferences(mContext).getVipNotification();
        if (!notifyVip && !quietUpdate) {
            return null;
        }
        mVipNotifiedMessageCount = unseenMessageCount;
        mVipNotifiedMessageId = newestMessageId;
        final Message message = Message.restoreMessageWithId(mContext, newestMessageId);
        if (message == null) {
            return null;
        }

        int unreadCount = VipMemberCache.getVipMessagesCount(mContext, account.mId, true);

        String senderName = Address.toFriendly(Address.unpack(message.mFrom));
        if (senderName == null) {
            senderName = ""; // Happens when a message has no from.
        }
        final boolean multipleUnseen = unseenMessageCount > 1;
        final Bitmap senderPhoto = multipleUnseen
                ? mGenericMultipleSenderIcon
                : getSenderPhoto(message);
        final SpannableString title = getNewVipMessageTitle(senderName, unseenMessageCount);

        final String text = multipleUnseen
                ? account.mDisplayName
                : message.mSubject;
        final Bitmap largeIcon = senderPhoto != null ? senderPhoto : mGenericSenderIcon;
        final Integer number = unreadCount > 1 ? unreadCount : null;
        final Intent intent;
        if (unseenMessageCount > 1) {
            intent = Welcome.createOpenMessageIntent(
                    mContext, account.mId, Mailbox.QUERY_ALL_VIPS, Message.NO_MESSAGE);
        } else {
            intent = Welcome.createOpenMessageIntent(
                    mContext, account.mId, message.mMailboxKey, newestMessageId);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        long now = mClock.getTime();
        boolean enableAudio = !quietUpdate && (now - mLastVipMessageNotifyTime) > MIN_SOUND_INTERVAL_MS;
        String ticker = quietUpdate ? null : title.toString();
        Notification n = createNotification(account, messageCursor, multipleUnseen, message,
                ticker, title, text, intent, largeIcon, number, enableAudio, false);
        mLastVipMessageNotifyTime = now;
        /// Update the VIP icon
        VipNotificationStyle vipNotificationStyle = new VipNotificationStyle();
        vipNotificationStyle.addVipTitle(title);
        vipNotificationStyle.updateVipIcon(mContext, n);
        return n;
    }

    /// M: Get the VIP notification title. e.g, Emails from 2 accounts
    private SpannableString getNewVipMessageTitle(String sender, int unseenCount) {
        String title;
        if (unseenCount > 1) {
            title = String.format(
                    mContext.getString(R.string.notification_multiple_new_vip_messages_fmt),
                    unseenCount);
        } else {
            title = sender;
        }
        return new SpannableString(title);
    }

    /// M: Filter the VIP messages
    private Cursor filterVipMessages(Cursor newMessagesCursor, long accountId) {
        MatrixCursor nonVipCurosr = new MatrixCursor(EmailContent.ID_PROJECTION);
        sInstance.mNeedNotifyVIPMails.remove(accountId);
        ArrayList<Long> vipMails = new ArrayList<Long>();
        newMessagesCursor.moveToPosition(-1);
        while (newMessagesCursor.moveToNext()) {
            long messageId = newMessagesCursor.getLong(EmailContent.ID_PROJECTION_COLUMN);
            String fromList = newMessagesCursor.getString(1);
            if (VipMemberCache.isVIP(fromList)) {
                vipMails.add(messageId);
            } else {
                nonVipCurosr.newRow().add(messageId);
            }
        }
        if (vipMails.size() > 0) {
            sInstance.mNeedNotifyVIPMails.put(accountId, vipMails);
        }
        return nonVipCurosr;
    }
}
