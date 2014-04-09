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

package com.android.email.activity;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.android.email.Controller;
import com.android.email.Email;
import com.android.email.Preferences;
import com.android.email.R;
import com.android.emailcommon.Logging;
import com.android.emailcommon.mail.Address;
import com.android.emailcommon.mail.MeetingInfo;
import com.android.emailcommon.mail.PackedString;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.provider.VipMember;
import com.android.emailcommon.service.EmailServiceConstants;
import com.android.emailcommon.utility.EmailAsyncTask;
import com.android.emailcommon.utility.Utility;

/**
 * A {@link MessageViewFragmentBase} subclass for regular email messages.  (regular as in "not eml
 * files").
 */
public class MessageViewFragment extends MessageViewFragmentBase
        implements MoveMessageToDialog.Callback, OnMenuItemClickListener, 
        DeleteMessageConfirmationDialog.Callback {

    private static final String TAG = "MessageViewFragment";

    private ImageView mFavoriteIcon;
    ///M: Make it visible for Favorite message
    private ImageView mFavoriteView;

    // calendar meeting invite answers
    private View mMeetingYes;
    private View mMeetingMaybe;
    private View mMeetingNo;
    private Drawable mFavoriteIconOn;
    private Drawable mFavoriteIconOff;
    /** M: Adjust the UI layout and logic to support VIP new feature @{ */
    private MenuItem mFavoriteMenu;
    private MenuItem mReplyMenu;
    private MenuItem mReplyAllMenu;
    private MenuItem mForwardMenu;
    /**@} */

    /** Default to ReplyAll if true. Otherwise Reply. */
    boolean mDefaultReplyAll;

    /** Whether or not to enable Reply/ReplyAll and Forward buttons */
    boolean mEnableReplyForwardButtons;

    /** Whether or not the message can be moved from the mailbox it's in. */
    private boolean mSupportsMove;

    private int mPreviousMeetingResponse = EmailServiceConstants.MEETING_REQUEST_NOT_RESPONDED;

    /**
     * This class has more call backs than {@link MessageViewFragmentBase}.
     *
     * - EML files can't be "mark unread".
     * - EML files can't have the invite buttons or the view in calender link.
     *   Note EML files can have ICS (calendar invitation) files, but we don't treat them as
     *   invites.  (Only exchange provider sets the FLAG_INCOMING_MEETING_INVITE
     *   flag.)
     *   It'd be weird to respond to an invitation in an EML that might not be addressed to you...
     */
    public interface Callback extends MessageViewFragmentBase.Callback {
        /** Called when the "view in calendar" link is clicked. */
        void onCalendarLinkClicked(long epochEventStartTime);

        /**
         * Called when a calender response button is clicked.
         *
         * @param response one of {@link EmailServiceConstants#MEETING_REQUEST_ACCEPTED},
         * {@link EmailServiceConstants#MEETING_REQUEST_DECLINED}, or
         * {@link EmailServiceConstants#MEETING_REQUEST_TENTATIVE}.
         */
        void onRespondedToInvite(int response);

        /** Called when the current message is set unread. */
        void onMessageSetUnread();

        /**
         * Called right before the current message will be deleted or moved to another mailbox.
         *
         * Callees will usually close the fragment.
         */
        void onBeforeMessageGone();

        /** Called when the forward button is pressed. */
        void onForward();
        /** Called when the reply button is pressed. */
        void onReply();
        /** Called when the reply-all button is pressed. */
        void onReplyAll();
        /**M: Called when on create. */
        boolean onCheckIsEasRemoteMessage();
    }

    public static final class EmptyCallback extends MessageViewFragmentBase.EmptyCallback
            implements Callback {
        @SuppressWarnings("hiding")
        public static final Callback INSTANCE = new EmptyCallback();

        @Override public void onCalendarLinkClicked(long epochEventStartTime) { }
        @Override public void onMessageSetUnread() { }
        @Override public void onRespondedToInvite(int response) { }
        @Override public void onBeforeMessageGone() { }
        @Override public void onForward() { }
        @Override public void onReply() { }
        @Override public void onReplyAll() { }
        @Override public String onGetQueryTerm() { return null; }
        /**
         * M: call to check whether this message is eas remote message when on create.
         */
        @Override public boolean onCheckIsEasRemoteMessage() { return false; }
    }

    private Callback mCallback = EmptyCallback.INSTANCE;
    private boolean mMessageFlagFavorite = false;

    /**
     * Create a new instance with initialization parameters.
     *
     * This fragment should be created only with this method.  (Arguments should always be set.)
     *
     * @param messageId ID of the message to open
     */
    public static MessageViewFragment newInstance(long messageId) {
        if (messageId == Message.NO_MESSAGE) {
            throw new IllegalArgumentException();
        }
        final MessageViewFragment instance = new MessageViewFragment();
        /**
         * M: records the message id synchronously @{
         */
        Controller controller = Controller.getInstance(instance.mContext);
        controller.recordMessageIdSync(messageId);
        /** @} */
        final Bundle args = new Bundle();
        args.putLong(ARG_MESSAGE_ID, messageId);
        instance.setArguments(args);
        return instance;
    }

    /**
     * We will display the message for this ID. This must never be a special message ID such as
     * {@link Message#NO_MESSAGE}. Do NOT use directly; instead, use {@link #getMessageId()}.
     * <p><em>NOTE:</em> Although we cannot force these to be immutable using Java language
     * constructs, this <em>must</em> be considered immutable.
     */
    private Long mImmutableMessageId;

    public void setMessageId(Bundle b) {
        mImmutableMessageId = b.getLong(ARG_MESSAGE_ID);
        mMessageId = mImmutableMessageId;
    }

    private void initializeArgCache() {
        if (mImmutableMessageId != null) {
            return;
        }
        mImmutableMessageId = getArguments().getLong(ARG_MESSAGE_ID);
    }

    /**
     * @return the message ID passed to {@link #newInstance}.  Safe to call even before onCreate.
     */
    public long getMessageId() {
        initializeArgCache();
        return mImmutableMessageId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        final Resources res = getActivity().getResources();
        mFavoriteIconOn = res.getDrawable(R.drawable.ic_menu_star_off_holo_light);
        mFavoriteIconOff = res.getDrawable(R.drawable.ic_menu_star_holo_light);
    }

    @Override
    public void onResume() {
        super.onResume();
        /// M: Adjust the UI layout and logic to support VIP new feature
        mDefaultReplyAll = Preferences.getSharedPreferences(mContext).getBoolean(
                Preferences.REPLY_ALL, Preferences.REPLY_ALL_DEFAULT);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);

        mMeetingYes = UiUtilities.getView(view, R.id.accept);
        mMeetingMaybe = UiUtilities.getView(view, R.id.maybe);
        mMeetingNo = UiUtilities.getView(view, R.id.decline);
        mMeetingYes.setOnClickListener(this);
        mMeetingMaybe.setOnClickListener(this);
        mMeetingNo.setOnClickListener(this);
        UiUtilities.getView(view, R.id.invite_link).setOnClickListener(this);

        enableReplyForwardButtons(false);
        ///M: Get the favorite star imageview
        mFavoriteView = (ImageView) UiUtilities.getView(view, R.id.favorite_star);

        return view;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        /// M: use isEasRemoteMode to decide whether show out some function.
        boolean isEasRemoteMode = mCallback.onCheckIsEasRemoteMessage();
        MenuItem move = menu.findItem(R.id.move);
        if (move != null) {
            menu.findItem(R.id.move).setVisible(mSupportsMove);
        }
        /** M: Adjust the UI layout and logic to support VIP new feature @{ */

        ///M: disable not supported function like DeleteMenu, UnreadMenu and
        // FavoriteMenu when message is from remote search and account is EAS. @{
        mReplyMenu = menu.findItem(R.id.reply);
        mForwardMenu = menu.findItem(R.id.forward);
        mFavoriteMenu = menu.findItem(R.id.favorite);
        if (isEasRemoteMode) {
            setMenuVisibleSafe(mFavoriteMenu, false);
            setMenuVisibleSafe(menu.findItem(R.id.delete), false);
            setMenuVisibleSafe(menu.findItem(R.id.mark_as_unread), false);
            mForwardMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        } else {
            updateFavoriteMenu(mMessageFlagFavorite);
        }
        mReplyAllMenu = menu.findItem(R.id.reply_all);
        /// @}
        enableReplyForwardButtons(mEnableReplyForwardButtons);
        if (mEnableReplyForwardButtons && mDefaultReplyAll) {
            // Reverse the reply and reply all menu
            mReplyMenu.setIcon(R.drawable.ic_reply_all);
            mReplyMenu.setTitle(R.string.reply_all_action);
            mReplyAllMenu.setIcon(R.drawable.ic_reply);
            mReplyAllMenu.setTitle(R.string.reply_action);
        }
        /** @} */
    }

    /**
     * M: Update the visibility of the menu, if the menu not null
     * @param menu the menu
     * @param visibility the new visibility of the menu
     */
    private void setMenuVisibleSafe(MenuItem menu, boolean visibility) {
        if (menu != null) {
            menu.setVisible(visibility);
        }
    }

    private void enableReplyForwardButtons(boolean enabled) {
        mEnableReplyForwardButtons = enabled;
        // We don't have disabled button assets, so let's hide them for now
        /** M: Adjust the UI layout and logic to support VIP new feature @{ */
        setMenuVisibleSafe(mReplyAllMenu, enabled);
        setMenuVisibleSafe(mReplyMenu, enabled);
        setMenuVisibleSafe(mForwardMenu, enabled);
        /** @} */
    }

    public void setCallback(Callback callback) {
        mCallback = (callback == null) ? EmptyCallback.INSTANCE : callback;
        super.setCallback(mCallback);
    }

    @Override
    protected void resetView() {
        super.resetView();
        mPreviousMeetingResponse = EmailServiceConstants.MEETING_REQUEST_NOT_RESPONDED;
    }

    /**
     * NOTE See the comment on the super method.  It's called on a worker thread.
     */
    @Override
    protected Message openMessageSync(Activity activity) {
        return Message.restoreMessageWithId(activity, getMessageId());
    }

    @Override
    protected void onMessageShown(long messageId, Mailbox mailbox) {
        super.onMessageShown(messageId, mailbox);
        onMarkMessageAsRead(true);
        Account account = Account.restoreAccountWithId(mContext, getAccountId());
        boolean supportsMove = account.supportsMoveMessages(mContext)
                && mailbox.canHaveMessagesMoved();
        if (mSupportsMove != supportsMove) {
            mSupportsMove = supportsMove;
            Activity host = getActivity();
            if (host != null) {
                host.invalidateOptionsMenu();
            }
        }

        // Disable forward/reply buttons as necessary.
        enableReplyForwardButtons(Mailbox.isMailboxTypeReplyAndForwardable(mailbox.mType));
    }

    /**
     * Sets the content description for the star icon based on whether it's currently starred.
     */
    private void setStarContentDescription(boolean isFavorite) {
        /** M: Adjust the UI layout and logic to support VIP new feature @{ */
        if (mFavoriteMenu == null) {
            return;
        }
        if (isFavorite) {
            mFavoriteMenu.setTitle(mContext.getResources().getString(R.string.remove_star_action));
        } else {
            mFavoriteMenu.setTitle(mContext.getResources().getString(R.string.set_star_action));
        }
        /** @} */
    }

    /**
     * Toggle favorite status and write back to provider
     */
    private void onClickFavorite() {
        if (!isMessageOpen()) {
            return;
        }
        Message message = getMessage();

        // Update UI
        boolean newFavorite = ! mMessageFlagFavorite;
        /// M: Adjust the UI layout and logic to support VIP new feature
        updateFavoriteMenu(newFavorite);
        // Handle accessibility event
        setStarContentDescription(newFavorite);

        // Update provider
        message.mFlagFavorite = newFavorite;
        mMessageFlagFavorite = newFavorite;
        getController().setMessageFavorite(message.mId, newFavorite);
    }

    /**
     * Update the favorite icon on the menu
     * @param newFavorite the favorite status
     */
    private void updateFavoriteMenu(Boolean newFavorite) {
        if (mFavoriteMenu != null) {
            Resources res = getActivity().getResources();
            mFavoriteMenu.setIcon(newFavorite ? mFavoriteIconOn : mFavoriteIconOff);
            mFavoriteMenu.setTitle(res.getString(newFavorite ?
                    R.string.remove_star_action : R.string.set_star_action));
        }
        ///M: add for show favorite star icon if newFavorite is true
        int visibility = newFavorite ? View.VISIBLE : View.GONE;
        if ((mFavoriteView != null) && (mFavoriteView.getVisibility() != visibility)) {
            Logging.v("Message favorite state changed" + visibility);
            mFavoriteView.setVisibility(visibility);
        }
    }

    /**
     * Set message read/unread.
     */
    public void onMarkMessageAsRead(boolean isRead) {
        if (!isMessageOpen()) {
            return;
        }
        Message message = getMessage();
        if (message.mFlagRead != isRead) {
            message.mFlagRead = isRead;
            getController().setMessageRead(message.mId, isRead);
            if (!isRead) { // Became unread.  We need to close the message.
                mCallback.onMessageSetUnread();
            }
        }
    }

    /**
     * Send a service message indicating that a meeting invite button has been clicked.
     */
    private void onRespondToInvite(int response, int toastResId) {
        if (!isMessageOpen()) {
            return;
        }
        Message message = getMessage();
        // do not send twice in a row the same response
        if (mPreviousMeetingResponse != response) {
            getController().sendMeetingResponse(message.mId, response);
            mPreviousMeetingResponse = response;
        }
        Utility.showToast(getActivity(), toastResId);
        mCallback.onRespondedToInvite(response);
    }

    private void onInviteLinkClicked() {
        if (!isMessageOpen()) {
            return;
        }
        Message message = getMessage();
        String startTime = new PackedString(message.mMeetingInfo).get(MeetingInfo.MEETING_DTSTART);
        if (startTime != null) {
            long epochTimeMillis = Utility.parseEmailDateTimeToMillis(startTime);
            mCallback.onCalendarLinkClicked(epochTimeMillis);
        } else {
            Email.log("meetingInfo without DTSTART " + message.mMeetingInfo);
        }
    }

    @Override
    public void onClick(View view) {
        if (!isMessageOpen()) {
            return; // Ignore.
        }
        switch (view.getId()) {
            case R.id.invite_link:
                onInviteLinkClicked();
                return;

            case R.id.accept:
                onRespondToInvite(EmailServiceConstants.MEETING_REQUEST_ACCEPTED,
                        R.string.message_view_invite_toast_yes);
                return;
            case R.id.maybe:
                onRespondToInvite(EmailServiceConstants.MEETING_REQUEST_TENTATIVE,
                        R.string.message_view_invite_toast_maybe);
                return;
            case R.id.decline:
                onRespondToInvite(EmailServiceConstants.MEETING_REQUEST_DECLINED,
                        R.string.message_view_invite_toast_no);
                return;
        }
        super.onClick(view);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (isMessageOpen()) {
            switch (item.getItemId()) {
                case R.id.reply:
                    mCallback.onReply();
                    return true;
                case R.id.reply_all:
                    mCallback.onReplyAll();
                    return true;
                case R.id.forward:
                    mCallback.onForward();
                    return true;
            }
        }
        return false;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Sanity check; MessageViewFragment might have been paused?
        // This is work around of a framework bug:
        // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState.
        if (!isResumed()) {
            return true;
        }
        switch (item.getItemId()) {
            /** M: Adjust the UI layout and logic to support VIP new feature @{ */
            case R.id.reply:
                // The reply and reply all menu may be reversed
                if (mDefaultReplyAll) {
                    mCallback.onReplyAll();
                } else {
                    mCallback.onReply();
                }
                return true;
            case R.id.reply_all:
                // The reply and reply all menu may be reversed
                if (mDefaultReplyAll) {
                    mCallback.onReply();
                } else {
                    mCallback.onReplyAll();
                }
                return true;
            case R.id.forward:
                mCallback.onForward();
                return true;
            case R.id.favorite:
                onClickFavorite();
                return true;
            /** @} */
            case R.id.move:
                onMove();
                return true;
            case R.id.delete:
                /// M: new feature, ask before delete. @{
                showConfirmIfNeeded();
                /// @}
                return true;
            case R.id.mark_as_unread:
                onMarkAsUnread();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onMove() {
        MoveMessageToDialog dialog = MoveMessageToDialog.newInstance(new long[] {getMessageId()},
                this);
        dialog.show(getFragmentManager(), MoveMessageToDialog.TAG);
    }

    // MoveMessageToDialog$Callback
    @Override
    public void onMoveToMailboxSelected(long newMailboxId, long[] messageIds) {
        mCallback.onBeforeMessageGone();
        ActivityHelper.moveMessages(mContext, newMailboxId, messageIds);
    }

    private void onDelete() {
        stopLoading();
        showWaitingDialogIfNeeded();
        Logging.d(TAG, " onDelete message, messageID = " + getMessageId());
        ActivityHelper.deleteMessage(mContext, getMessageId());
        mCallback.onBeforeMessageGone();
    }

    private void onMarkAsUnread() {
        onMarkMessageAsRead(false);
    }

    public void showWaitingDialogIfNeeded() {
        if (EmailAsyncTask.isAsyncTaskBusy()) {
            EmailActivity activity = (EmailActivity) getActivity();
            activity.showWaitingDialog();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Mark the current as unread.
     */
    @Override
    protected void onPostLoadBody() {
        onMarkMessageAsRead(true);

        // Initialize star content description for accessibility
        setStarContentDescription(mMessageFlagFavorite);
    }

    @Override
    protected void updateHeaderView(Message message) {
        super.updateHeaderView(message);
        if (!mMessageIsReload) {
            mMessageFlagFavorite = message.mFlagFavorite;
            /// M: Update the favorite menu
            updateFavoriteMenu(mMessageFlagFavorite);
        }

        // Enable the invite tab if necessary
        if ((message.mFlags & Message.FLAG_INCOMING_MEETING_INVITE) != 0) {
            addTabFlags(TAB_FLAGS_HAS_INVITE);
        }
    }

    @Override
    public void onDeleteMessageConfirmationDialogOkPressed() {
        onDelete();
    }

    /// M: add ask before deleting new feature, show confirm dialog if needed.
    private void showConfirmIfNeeded() {
        boolean askBeforeDeleting = Preferences.getPreferences(mContext)
                .isAskBeforeDelete();
        if (askBeforeDeleting) {
            DeleteMessageConfirmationDialog.newInstance(1, this).show(
                    getFragmentManager(), DeleteMessageConfirmationDialog.TAG);
        } else {
            onDelete();
        }
    }
}
