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
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Browser;
import android.provider.ContactsContract;
import android.provider.ContactsContract.QuickContact;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.email.AttachmentInfo;
import com.android.email.Controller;
import com.android.email.ControllerResultUiThreadWrapper;
import com.android.email.Email;
import com.android.email.Preferences;
import com.android.email.RefreshManager;
import com.android.email.R;
import com.android.email.Throttle;
import com.android.email.mail.internet.EmailHtmlUtil;
import com.android.email.service.AttachmentDownloadService;
import com.android.email.view.NonLockingScrollView;
import com.android.email.view.NonLockingScrollView.OnOverScrollListener;
import com.android.emailcommon.Configuration;
import com.android.emailcommon.Logging;
import com.android.emailcommon.internet.MimeUtility;
import com.android.emailcommon.mail.Address;
import com.android.emailcommon.mail.MessagingException;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.VipMember;
import com.android.emailcommon.provider.EmailContent.Attachment;
import com.android.emailcommon.provider.EmailContent.Body;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.utility.AttachmentUtilities;
import com.android.emailcommon.utility.EmailAsyncTask;
import com.android.emailcommon.utility.LinkParserTask;
import com.android.emailcommon.utility.TextUtilities;
import com.android.emailcommon.utility.Utility;

import com.google.common.collect.Maps;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.drm.OmaDrmStore;
import com.mediatek.email.emailvip.VipMemberCache;
import com.mediatek.email.emailvip.utils.MessageViewUtils;
import com.mediatek.email.emailvip.utils.MessageViewUtils.TempAddress;
import com.mediatek.storage.StorageManagerEx;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// TODO Better handling of config changes.
// - Retain the content; don't kick 3 async tasks every time

/**
 * Base class for {@link MessageViewFragment} and {@link MessageFileViewFragment}.
 */
public abstract class MessageViewFragmentBase extends Fragment implements View.OnClickListener {
    /** Argument name(s) */
    public static final String ARG_MESSAGE_ID = "messageId";
    private static final String BUNDLE_KEY_CURRENT_TAB = "MessageViewFragmentBase.currentTab";
    private static final String BUNDLE_KEY_PICTURE_LOADED = "MessageViewFragmentBase.pictureLoaded";
    private static final String BUNDLE_KEY_PARTIAL_DOWNLOAD = "MessageViewFragmentBase.particalDownload";
    private static final String BUNDLE_KEY_MESSAGE_KEY = "MessageViewFragmentBase.messageKey";
    /// M: VIP feature: The count of {@link VipTextView}, as soon as added to 100 views,
    // set the mDetailsLayout visible. If the number of recipients is more than 100,
    // keep adding the rest views after show the parent layout.
    private static final int VIEWS_NUMBER_TO_SHOW = 100;
    private static final int PHOTO_LOADER_ID = 1;
    /// M: VIP feature: Loader id for photo loading of VipTextView's pop up window.
    public static final int VIP_VIEW_PHOTO_LOADER_ID = 2;
    protected Context mContext;

    // Regex that matches start of img tag. '<(?i)img\s+'.
    private static final Pattern IMG_TAG_START_REGEX = Pattern.compile("<(?i)img\\s+");

    private static final String DEFAULT_EMAIL_PREFIX = "email://";
    private static final String HTTP_PREFIX = "http://";

    private static final int PREVIEW_ICON_WIDTH = 62;
    private static final int PREVIEW_ICON_HEIGHT = 62;

    // The different levels of zoom: read from the Preferences.
    private static String[] sZoomSizes = null;
    private int mLastZoomSize;

    private TextView mSubjectView;
    /// M: Make it visible for its sub class
    protected TextView mFromNameView;
    private TextView mFromAddressView;
    /// M: VIP feature: VIP add/remove ImageButton of sender. @{
    private ImageView mVipSwitcher;
    private Drawable mAddVipIcon;
    private Drawable mRemoveVipIcon;
    /// @}
    private TextView mDateTimeView;
    private TextView mAddressesView;
    private WebView mMessageContentView;
    private LinearLayout mAttachments;
    private LinearLayout mRemainBtnView;
    private View mTabSection;
    private ImageView mFromBadge;
    private ImageView mSenderPresenceView;
    private NonLockingScrollView mMainView;
    private View mLoadingProgress;
    private View mDetailsCollapsed;
    private View mDetailsExpanded;
    private boolean mDetailsFilled;

    private TextView mMessageTab;
    private TextView mAttachmentTab;
    private TextView mInviteTab;
    // It is not really a tab, but looks like one of them.
    private TextView mShowPicturesTab;
    private View mAlwaysShowPicturesButton;

    private View mAttachmentsScroll;
    private View mInviteScroll;

    private long mAccountId = Account.NO_ACCOUNT;
    protected long mMessageId = Message.NO_MESSAGE;
    private Message mMessage;

    private Controller mController;
    private ControllerResultUiThreadWrapper<ControllerResults> mControllerCallback;

    // contains the HTML body. Is used by LoadAttachmentTask to display inline images.
    // is null most of the time, is used transiently to pass info to LoadAttachementTask
    private String mHtmlTextRaw;

    // contains the HTML content as set in WebView.
    private String mHtmlTextWebView;

    private boolean mIsMessageLoadedForTest;

    private MessageObserver mMessageObserver;

    // arraylist for background downloading attachments
    private ArrayList<Long> mDownloadAttachmentList = new ArrayList<Long>();

    private static final int CONTACT_STATUS_STATE_UNLOADED = 0;
    private static final int CONTACT_STATUS_STATE_UNLOADED_TRIGGERED = 1;
    private static final int CONTACT_STATUS_STATE_LOADED = 2;

    private int mContactStatusState;
    private Uri mQuickContactLookupUri;

    /** Flag for {@link #mTabFlags}: Message has attachment(s) */
    protected static final int TAB_FLAGS_HAS_ATTACHMENT = 1;

    /**
     * Flag for {@link #mTabFlags}: Message contains invite.  This flag is only set by
     * {@link MessageViewFragment}.
     */
    protected static final int TAB_FLAGS_HAS_INVITE = 2;

    /** Flag for {@link #mTabFlags}: Message contains pictures */
    protected static final int TAB_FLAGS_HAS_PICTURES = 4;

    /** Flag for {@link #mTabFlags}: "Show pictures" has already been pressed */
    protected static final int TAB_FLAGS_PICTURE_LOADED = 8;

    private LoadMessageTask mLoadMessageTask;
    private LoadBodyTask mLoadBodyTask;
    private LoadAttachmentsTask mLoadAttachmentsTask;
    private LinkParserTask mLinkParserTask;

    /**
     * Flags to control the tabs.
     * @see #updateTabs(int)
     */
    private int mTabFlags;

    /** # of attachments in the current message */
    private int mAttachmentCount;

    // Use (random) large values, to avoid confusion with TAB_FLAGS_*
    protected static final int TAB_MESSAGE = 101;
    protected static final int TAB_INVITE = 102;
    protected static final int TAB_ATTACHMENT = 103;
    private static final int TAB_NONE = 0;

    /** Current tab */
    private int mCurrentTab = TAB_NONE;
    /**
     * Tab that was selected in the previous activity instance.
     * Used to restore the current tab after screen rotation.
     */
    private int mRestoredTab = TAB_NONE;
    /// M: When rotation with non message tab, webview need to reload message content.
    private boolean mNeedReloadMessageContent = false;

    private boolean mRestoredPictureLoaded;
    
    private boolean mPartialDownload = false;

    private final EmailAsyncTask.Tracker mTaskTracker = new EmailAsyncTask.Tracker();
    
    private AsyncTask mRenderAttachmentTask = null;
    private static final String TAG = "MessageViewFragmentBase";
    
    // Currently just used for constraining "move to older/newer" 
    // while a message is loading
    private boolean mMessageLoading = false;

    public interface Callback {
        /**
         * Called when a link in a message is clicked.
         *
         * @param url link url that's clicked.
         * @return true if handled, false otherwise.
         */
        public boolean onUrlInMessageClicked(String url);

        /**
         * Called when the message specified doesn't exist, or is deleted/moved.
         */
        public void onMessageNotExists();

        /** Called when it starts loading a message. */
        public void onLoadMessageStarted();

        /** Called when it successfully finishes loading a message. */
        public void onLoadMessageFinished();

        /** Called when an error occurred during loading a message. */
        public void onLoadMessageError(String errorMessage);

        /** Called when it successfully finishes loading a message. */
        public void onNeedUpdateAtionBarTitle();

        /** M: Called when need to highlight query term on loading message body */
        public String onGetQueryTerm();
    }

    public static class EmptyCallback implements Callback {
        public static final Callback INSTANCE = new EmptyCallback();
        @Override public void onLoadMessageError(String errorMessage) {}
        @Override public void onLoadMessageFinished() {}
        @Override public void onLoadMessageStarted() {}
        @Override public void onMessageNotExists() {}
        @Override
        public boolean onUrlInMessageClicked(String url) {
            return false;
        }
        @Override
        public void onNeedUpdateAtionBarTitle(){}
        @Override
        public String onGetQueryTerm() {return null;}
    }

    private Callback mCallback = EmptyCallback.INSTANCE;
    protected boolean mMessageIsReload = false;

    @Override
    public void onAttach(Activity activity) {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, this + " onAttach");
        }
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, this + " onCreate");
        }
        super.onCreate(savedInstanceState);

        mContext = getActivity().getApplicationContext();

        // Initialize components, but don't "start" them.  Registering the controller callbacks
        // and starting MessageObserver, should be done in onActivityCreated or later and be stopped
        // in onDestroyView to prevent from getting callbacks when the fragment is in the back
        // stack, but they'll start again when it's back from the back stack.
        mController = Controller.getInstance(mContext);
        mControllerCallback = new ControllerResultUiThreadWrapper<ControllerResults>(
                new Handler(), new ControllerResults());
        mMessageObserver = new MessageObserver(new Handler(), mContext);

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }
        /** M: Adjust the UI layout and logic to support VIP new feature @{ */
        Resources res = mContext.getResources();
        mRemoveVipIcon = res.getDrawable(R.drawable.ic_email_remove_vip_holo_dark);
        mAddVipIcon = res.getDrawable(R.drawable.ic_email_add_vip_holo_dark);
        /** @} */
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, this + " onCreateView");
        }
        final View view = inflater.inflate(R.layout.message_view_fragment, container, false);

        cleanupDetachedViews();

        mSubjectView = (TextView) UiUtilities.getView(view, R.id.subject);
        mFromNameView = (TextView) UiUtilities.getView(view, R.id.from_name);
        mFromAddressView = (TextView) UiUtilities.getView(view, R.id.from_address);
        /// M: VIP feature: VIP add/remove ImageButton of sender.
        mVipSwitcher = (ImageView) UiUtilities.getView(view, R.id.vip_switcher);
        mAddressesView = (TextView) UiUtilities.getView(view, R.id.addresses);
        mDateTimeView = (TextView) UiUtilities.getView(view, R.id.datetime);
        mMessageContentView = (WebView) UiUtilities.getView(view, R.id.message_content);
        /** M: MTK Dependence */
        mMessageContentView.enableEmailUsingFlag();
        mAttachments = (LinearLayout) UiUtilities.getView(view, R.id.attachments);
        mRemainBtnView = (LinearLayout) UiUtilities.getView(view, R.id.msg_remain);
        mTabSection = UiUtilities.getView(view, R.id.message_tabs_section);
        mFromBadge = (ImageView) UiUtilities.getView(view, R.id.badge);
        mSenderPresenceView = (ImageView) UiUtilities.getView(view, R.id.presence);
        mMainView = UiUtilities.getView(view, R.id.main_panel);
        mLoadingProgress = UiUtilities.getView(view, R.id.loading_progress);
        mDetailsCollapsed = UiUtilities.getView(view, R.id.sub_header_contents_collapsed);
        mDetailsExpanded = UiUtilities.getView(view, R.id.sub_header_contents_expanded);

        mFromNameView.setOnClickListener(this);
        mFromAddressView.setOnClickListener(this);
        mFromBadge.setOnClickListener(this);
        mSenderPresenceView.setOnClickListener(this);
        /** M: Get the vip switcher button @{ */
        mVipSwitcher.setOnClickListener(this);
        /** @} */

        mMessageTab = UiUtilities.getView(view, R.id.show_message);
        mAttachmentTab = UiUtilities.getView(view, R.id.show_attachments);
        mShowPicturesTab = UiUtilities.getView(view, R.id.show_pictures);
        mAlwaysShowPicturesButton = UiUtilities.getView(view, R.id.always_show_pictures_button);
        // Invite is only used in MessageViewFragment, but visibility is controlled here.
        mInviteTab = UiUtilities.getView(view, R.id.show_invite);

        mMessageTab.setOnClickListener(this);
        mAttachmentTab.setOnClickListener(this);
        mShowPicturesTab.setOnClickListener(this);
        mAlwaysShowPicturesButton.setOnClickListener(this);
        mInviteTab.setOnClickListener(this);
        mDetailsCollapsed.setOnClickListener(this);
        mDetailsExpanded.setOnClickListener(this);

        mAttachmentsScroll = UiUtilities.getView(view, R.id.attachments_scroll);
        mInviteScroll = UiUtilities.getView(view, R.id.invite_scroll);

        WebSettings webSettings = mMessageContentView.getSettings();
        boolean supportMultiTouch = mContext.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH);
        webSettings.setDisplayZoomControls(!supportMultiTouch);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        mMessageContentView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mMessageContentView.setWebViewClient(new CustomWebViewClient());
        mMainView.setScrollbarFadingEnabled(true);
        mMainView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, this + " onActivityCreated");
        }
        super.onActivityCreated(savedInstanceState);
        mController.addResultCallback(mControllerCallback);

        loadMessage();

        UiUtilities.installFragment(this);
    }

    public void loadMessage() {
        mMainView.fling(0);
        resetView();
        mLoadMessageTask = new LoadMessageTask(true);
        mLoadMessageTask.executeParallel();

    }

    /**
     * Stop all loading AsyncTask
     */
    public void stopLoading() {
        if (mLoadMessageTask != null) {
            mLoadMessageTask.cancel(true);
            mLoadMessageTask = null;
        }

        if (mLoadBodyTask != null) {
            mLoadBodyTask.cancel(true);
            mLoadBodyTask = null;
        }

        if (mLoadAttachmentsTask != null) {
            mLoadAttachmentsTask.cancel(true);
            mLoadAttachmentsTask = null;
        }
        
        if(mLinkParserTask != null){
            mLinkParserTask.cancel(true);
            mLinkParserTask.stopWebView();
            mLinkParserTask = null;
        }

        if(mRenderAttachmentTask != null){
            mRenderAttachmentTask.cancel(true);
            mRenderAttachmentTask = null;
        }

        mTaskTracker.cancellAllInterrupt();
        Logging.d(TAG, "Stop all loading AsyncTask...");
    }

    /*
     * Click "new/older " and open a new message , set the mPartialDownload as false.
     */
    public void resetPartialLoading(){
        mPartialDownload = false;
    }

    @Override
    public void onStart() {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, this + " onStart");
        }
        super.onStart();
    }

    @Override
    public void onResume() {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, this + " onResume");
        }
        super.onResume();

        if (mMessageContentView != null && mLastZoomSize > 0
                && mLastZoomSize != getWebViewZoom()) {
            loadMessage();
        }
        // We might have comes back from other full-screen activities.  If so, we need to update
        // the attachment tab as system settings may have been updated that affect which
        // options are available to the user.
        updateAttachmentTab();

        /// M: VIP feature: If the vip information is changed,
        // we need to update all views involved the vip feature.
        updateVipInformation(null);
    }

    @Override
    public void onPause() {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, this + " onPause");
        }
        mLastZoomSize = getWebViewZoom();
        super.onPause();
    }

    @Override
    public void onStop() {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, this + " onStop");
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, this + " onDestroyView");
        }
        UiUtilities.uninstallFragment(this);
        mController.removeResultCallback(mControllerCallback);
        cancelAllTasks();
        mPartialDownload = false;

        // We should clean up the Webview here, but it can't release resources until it is
        // actually removed from the view tree.

        super.onDestroyView();
    }

    private void cleanupDetachedViews() {
        // WebView cleanup must be done after it leaves the rendering tree, according to
        // its contract
        // WebView will be destroyed and the mLinkParserTask must be canceled.
        Logging.d(TAG, "cleanupDetachedViews mLinkParserTask and webview will be destoryed.");
        if(mLinkParserTask != null){
            mLinkParserTask.cancel(true);
            mLinkParserTask.stopWebView();
            mLinkParserTask = null;
        }
        if (mMessageContentView != null) {
            mMessageContentView.destroy();
            mMessageContentView = null;
        }
    }

    @Override
    public void onDestroy() {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, this + " onDestroy");
        }
        stopLoading();
        cleanupDetachedViews();
        super.onDestroy();
        /// M: Clear the fragment instance refs by other objects. @{
        final Activity a = this.getActivity();
        if (a instanceof FragmentInstallable) {
            ((FragmentInstallable) a).onRemoveFragment(this);
        }
        /// @}
    }

    @Override
    public void onDetach() {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, this + " onDetach");
        }
        super.onDetach();
    }

    /**
     * The framework handles most of the fields, but we need to handle stuff :
     * BUNDLE_KEY_PARTIAL_DOWNLOAD: whether show partialdownload progress.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, this + " onSaveInstanceState");
        }
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_KEY_CURRENT_TAB, mCurrentTab);
        outState.putBoolean(BUNDLE_KEY_PICTURE_LOADED, (mTabFlags & TAB_FLAGS_PICTURE_LOADED) != 0);
        outState.putBoolean(BUNDLE_KEY_PARTIAL_DOWNLOAD, mPartialDownload);
        outState.putLong(BUNDLE_KEY_MESSAGE_KEY, mMessageId);
        Logging.d(TAG, "onSaveInstanceState BUNDLE_KEY_PARTIAL_DOWNLOAD" + " = " + mPartialDownload
                + " BUNDLE_KEY_MESSAGE_KEY" + " = " + mMessageId);
    }

    private void restoreInstanceState(Bundle state) {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, this + " restoreInstanceState");
        }
        // At this point (in onCreate) no tabs are visible (because we don't know if the message has
        // an attachment or invite before loading it).  We just remember the tab here.
        // We'll make it current when the tab first becomes visible in updateTabs().
        mRestoredTab = state.getInt(BUNDLE_KEY_CURRENT_TAB);
        mRestoredPictureLoaded = state.getBoolean(BUNDLE_KEY_PICTURE_LOADED);
        // Restore the state of the partial download progress and current message key.
        mPartialDownload = state.getBoolean(BUNDLE_KEY_PARTIAL_DOWNLOAD);
        mMessageId = state.getLong(BUNDLE_KEY_MESSAGE_KEY);
        getArguments().putLong(ARG_MESSAGE_ID, mMessageId);
        Logging.d(TAG, "restoreInstanceState BUNDLE_KEY_PARTIAL_DOWNLOAD" + " = " + mPartialDownload
                + "restoreInstanceState BUNDLE_KEY_mMessageId" + " = " + mMessageId);
    }

    public void setCallback(Callback callback) {
        mCallback = (callback == null) ? EmptyCallback.INSTANCE : callback;
    }

    private void cancelAllTasks() {
        mMessageObserver.unregister();
        mTaskTracker.cancellAllInterrupt();
    }

    protected final Controller getController() {
        return mController;
    }

    protected final Callback getCallback() {
        return mCallback;
    }

    public final Message getMessage() {
        return mMessage;
    }

    protected final boolean isMessageOpen() {
        return mMessage != null;
    }

    /**
     * Returns the account id of the current message, or -1 if unknown (message not open yet, or
     * viewing an EML message).
     */
    public long getAccountId() {
        return mAccountId;
    }

    /**
     * Show/hide the content.  We hide all the content (except for the bottom buttons) when loading,
     * to avoid flicker.
     */
    private void showContent(boolean showContent, boolean showProgressWhenHidden) {
        makeVisible(mMainView, showContent);
        makeVisible(mLoadingProgress, !showContent && showProgressWhenHidden);
    }

    // TODO: clean this up - most of this is not needed since the WebView and Fragment is not
    // reused for multiple messages.
    protected void resetView() {
        showContent(false, false);
        hideDetails();
        mDetailsFilled = false;
        updateTabs(0);
        /** M: Directly set the tab to mRestoredTab when user rotate the screen.
         * Google default is to set the tab to tab_message firstly, then set to mRestoredTab. {@ */
        mNeedReloadMessageContent = false;
        if (mRestoredTab != TAB_NONE ) {
            setCurrentTab(mRestoredTab);
            if (mRestoredTab != TAB_MESSAGE) {
                mNeedReloadMessageContent = true;
            }
            mRestoredTab = TAB_NONE;
        } else {
            setCurrentTab(TAB_MESSAGE);
        }
        /** @} */
        if (mMessageContentView != null) {
            blockNetworkLoads(true);
            mMessageContentView.flingScroll(0, 0);
            mMessageContentView.measure(0, 0);
            mMessageContentView.removeAllViews();
            mMessageContentView.clearView();
            mMessageContentView.scrollTo(0, 0);
            /// M: lose focus to avoid view slide to top when click message tap.
            mMessageContentView.setFocusable(false);
            mMessageContentView.loadData("<html><body bgcolor=\"white\"/></html>",
                    "text/html", "utf-8");

            // Dynamic configuration of WebView
            final WebSettings settings = mMessageContentView.getSettings();
            settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
            settings.setTextZoom(getWebViewZoom());
//            mMessageContentView.setInitialScale(getWebViewZoom());
        }
        mAttachmentsScroll.scrollTo(0, 0);
        mInviteScroll.scrollTo(0, 0);
        mMainView.scrollFreeze();
        mMainView.scrollTo(0, 0);

        mMainView.setOnOverScrollListener(new OnOverScrollListener() {
            @Override
            public void onOverScrolled() {
                Logging.d("mMainView onOverScrolled ready to load remaining !!!");
                /** M: Even if it's a POP message, enable this function to it
                 * because POP support partial download now. */
                if (mMessage.mFlagLoaded == Message.FLAG_LOADED_PARTIAL && !mPartialDownload
                        && Preferences.getPreferences(mContext).getAutoDownloadRemaining()) {
                    mPartialDownload = true;
                    Logging.d("mMainView onOverScrolled doing load remaining !!!");
                    doLoadMsgBackground(mMessageId);
                }
            }
        });

        mAttachments.removeAllViews();
        mAttachments.setVisibility(View.GONE);
        initContactStatusViews();
    }

    /**
     * Returns the zoom scale (in percent) which is a combination of the user setting
     * (tiny, small, normal, large, huge) and the device density. The intention
     * is for the text to be physically equal in size over different density
     * screens.
     */
    private int getWebViewZoom() {
        int zoom = Preferences.getPreferences(mContext).getTextZoom();
        if (sZoomSizes == null) {
            sZoomSizes = mContext.getResources()
                    .getStringArray(R.array.general_preference_text_zoom_size);
        }
//        return (int)(Float.valueOf(sZoomSizes[zoom]) * density * 100);
        return (int)(Float.valueOf(sZoomSizes[zoom]) * 100);
    }

    private void initContactStatusViews() {
        mContactStatusState = CONTACT_STATUS_STATE_UNLOADED;
        mQuickContactLookupUri = null;
        showDefaultQuickContactBadgeImage();
    }

    private void showDefaultQuickContactBadgeImage() {
        mFromBadge.setImageResource(R.drawable.ic_contact_picture);
    }

    protected final void addTabFlags(int tabFlags) {
        updateTabs(mTabFlags | tabFlags);
    }

    private final void clearTabFlags(int tabFlags) {
        updateTabs(mTabFlags & ~tabFlags);
    }

    private void setAttachmentCount(int count) {
        mAttachmentCount = count;
        if (mAttachmentCount > 0) {
            addTabFlags(TAB_FLAGS_HAS_ATTACHMENT);
        } else {
            /// M: Jump to Message Tab when a Message only have inline attachments
            if (mCurrentTab != TAB_MESSAGE && mCurrentTab != TAB_INVITE) {
                Logging.d("mAttachmentCount == 0 and switch to MessageTab");
                setCurrentTab(TAB_MESSAGE);
            }
            clearTabFlags(TAB_FLAGS_HAS_ATTACHMENT);
        }
    }

    private static void makeVisible(View v, boolean visible) {
        final int visibility = visible ? View.VISIBLE : View.GONE;
        if ((v != null) && (v.getVisibility() != visibility)) {
            v.setVisibility(visibility);
        }
    }

    private static boolean isVisible(View v) {
        return (v != null) && (v.getVisibility() == View.VISIBLE);
    }

    /**
     * Update the visual of the tabs.  (visibility, text, etc)
     */
    private void updateTabs(int tabFlags) {
        mTabFlags = tabFlags;

        if (getView() == null) {
            return;
        }

        boolean messageTabVisible = (tabFlags & (TAB_FLAGS_HAS_INVITE | TAB_FLAGS_HAS_ATTACHMENT))
                != 0;
        makeVisible(mMessageTab, messageTabVisible);
        makeVisible(mInviteTab, (tabFlags & TAB_FLAGS_HAS_INVITE) != 0);
        makeVisible(mAttachmentTab, (tabFlags & TAB_FLAGS_HAS_ATTACHMENT) != 0);

        final boolean hasPictures = (tabFlags & TAB_FLAGS_HAS_PICTURES) != 0;
        final boolean pictureLoaded = (tabFlags & TAB_FLAGS_PICTURE_LOADED) != 0;
        makeVisible(mShowPicturesTab, hasPictures && !pictureLoaded);

        mAttachmentTab.setText(mContext.getResources().getQuantityString(
                R.plurals.message_view_show_attachments_action,
                mAttachmentCount, mAttachmentCount));

        // Hide the entire section if no tabs are visible.
        makeVisible(mTabSection, isVisible(mMessageTab) || isVisible(mInviteTab)
                || isVisible(mAttachmentTab) || isVisible(mShowPicturesTab)
                || isVisible(mAlwaysShowPicturesButton));

        // Restore previously selected tab after rotation
        if (mRestoredTab != TAB_NONE && isVisible(getTabViewForFlag(mRestoredTab))) {
            setCurrentTab(mRestoredTab);
            mRestoredTab = TAB_NONE;
        }
    }

    /**
     * Set the current tab.
     *
     * @param tab any of {@link #TAB_MESSAGE}, {@link #TAB_ATTACHMENT} or {@link #TAB_INVITE}.
     */
    private void setCurrentTab(int tab) {
        mCurrentTab = tab;

        // Hide & unselect all tabs
        makeVisible(getTabContentViewForFlag(TAB_MESSAGE), false);
        makeVisible(getTabContentViewForFlag(TAB_ATTACHMENT), false);
        makeVisible(getTabContentViewForFlag(TAB_INVITE), false);
        getTabViewForFlag(TAB_MESSAGE).setSelected(false);
        getTabViewForFlag(TAB_ATTACHMENT).setSelected(false);
        getTabViewForFlag(TAB_INVITE).setSelected(false);

        makeVisible(mRemainBtnView, (tab == TAB_MESSAGE));

        makeVisible(getTabContentViewForFlag(mCurrentTab), true);
        getTabViewForFlag(mCurrentTab).setSelected(true);
        /** M: When the visibility of web view is GONE, it will not load message data indeed,
         * so we need to reload the message content When rotation with non message tab. {@ */
        if (mNeedReloadMessageContent && tab == TAB_MESSAGE && mMessageContentView != null) {
            setMessageHtml(mHtmlTextWebView);
            mNeedReloadMessageContent = false;
        }
        /** @} */
    }

    private View getTabViewForFlag(int tabFlag) {
        switch (tabFlag) {
            case TAB_MESSAGE:
                return mMessageTab;
            case TAB_ATTACHMENT:
                return mAttachmentTab;
            case TAB_INVITE:
                return mInviteTab;
        }
        throw new IllegalArgumentException();
    }

    private View getTabContentViewForFlag(int tabFlag) {
        switch (tabFlag) {
            case TAB_MESSAGE:
                return mMessageContentView;
            case TAB_ATTACHMENT:
                return mAttachmentsScroll;
            case TAB_INVITE:
                return mInviteScroll;
        }
        throw new IllegalArgumentException();
    }

    private void blockNetworkLoads(boolean block) {
        if (mMessageContentView != null) {
            mMessageContentView.getSettings().setBlockNetworkLoads(block);
        }
    }

    private void loadsImagesAutomatically(boolean flag) {
        if (mMessageContentView != null) {
            mMessageContentView.getSettings().setLoadsImagesAutomatically(flag);
        }
    }

    private void setMessageHtml(String html) {
        if (html == null) {
            html = "";
        }
        boolean picturesAlreadyLoaded = (mTabFlags & TAB_FLAGS_PICTURE_LOADED) != 0;
        if (picturesAlreadyLoaded) {
            loadsImagesAutomatically(true);
        } else {
            loadsImagesAutomatically(false);
            mMessageContentView.clearCache(false);
        }
        if (mMessageContentView != null) {
            /// M: some html content would not update such as 163's reject mail
            /// when partial-download.
            mMessageContentView.clearView();
            mMessageContentView.loadDataWithBaseURL("email://", html, "text/html", "utf-8", null);
            doLinkParse(html);
        }
    }

    /**
     * Handle clicks on sender, which shows {@link QuickContact} or prompts to add
     * the sender as a contact.
     */
    private void onClickSender() {
        if (!isMessageOpen()) {
            return;
        }
        final Address senderEmail = Address.unpackFirst(mMessage.mFrom);
        if (senderEmail == null) {
            return;
        }

        if (mContactStatusState == CONTACT_STATUS_STATE_UNLOADED) {
            // Status not loaded yet.
            mContactStatusState = CONTACT_STATUS_STATE_UNLOADED_TRIGGERED;
            return;
        }
        if (mContactStatusState == CONTACT_STATUS_STATE_UNLOADED_TRIGGERED) {
            return; // Already clicked, and waiting for the data.
        }

        /// M:Safely start Contacts, toast if catch ActivityNotFoundException. @{
        UiUtilities.showContacts(getActivity(), mFromBadge,
                mQuickContactLookupUri, senderEmail);
        /// @}
    }

    private static class ContactStatusLoaderCallbacks
            implements LoaderCallbacks<ContactStatusLoader.Result> {
        private static final String BUNDLE_EMAIL_ADDRESS = "email";
        private final MessageViewFragmentBase mFragment;

        public ContactStatusLoaderCallbacks(MessageViewFragmentBase fragment) {
            mFragment = fragment;
        }

        public static Bundle createArguments(String emailAddress) {
            Bundle b = new Bundle();
            b.putString(BUNDLE_EMAIL_ADDRESS, emailAddress);
            return b;
        }

        @Override
        public Loader<ContactStatusLoader.Result> onCreateLoader(int id, Bundle args) {
            return new ContactStatusLoader(mFragment.mContext,
                    args.getString(BUNDLE_EMAIL_ADDRESS));
        }

        @Override
        public void onLoadFinished(Loader<ContactStatusLoader.Result> loader,
                ContactStatusLoader.Result result) {
            boolean triggered =
                    (mFragment.mContactStatusState == CONTACT_STATUS_STATE_UNLOADED_TRIGGERED);
            mFragment.mContactStatusState = CONTACT_STATUS_STATE_LOADED;
            mFragment.mQuickContactLookupUri = result.mLookupUri;

            if (result.isUnknown()) {
                mFragment.mSenderPresenceView.setVisibility(View.GONE);
            } else {
                mFragment.mSenderPresenceView.setVisibility(View.VISIBLE);
                mFragment.mSenderPresenceView.setImageResource(result.mPresenceResId);
            }
            /**
             * M: If we couldn't find any photo information, set default image @{
             */
            if (result.mPhoto != null) { // photo will be null if unknown.
                mFragment.mFromBadge.setImageBitmap(result.mPhoto);
            } else {
                mFragment.showDefaultQuickContactBadgeImage();
            }
            /** @} */
            if (triggered) {
                mFragment.onClickSender();
            }
        }

        @Override
        public void onLoaderReset(Loader<ContactStatusLoader.Result> loader) {
        }
    }

    /**
     * M: Deal with the click event of save button, to check is the attachment file name valid
     * @param view the clicked view
     */
    private void onSaveClicked(View view) {
        MessageViewAttachmentInfo info = (MessageViewAttachmentInfo) view.getTag();
        if (RenameAttachmentFileDialog.isFileNameValid(info.mName)) {
            onSaveAttachment(info);
        } else {
            Logging.d("Open rename dialog, attachment original name: + " + info.mName);
            RenameAttachmentFileDialog.newInstance(info, new RenameAttachmentFileDialog.Callback(){

                @Override
                public void onOkayClicked(AttachmentInfo info) {
                    onSaveAttachment((MessageViewAttachmentInfo)info);
                }

            }).show(getFragmentManager(), "RenameAttachmentFileDialog");
        }
    }

    private void onSaveAttachment(MessageViewAttachmentInfo info) {
        /**
         * M: When SD card is unavailable or its size is not enough for an
         * attachment, the attachment can not been saved. @{
         */
        long sdCardSize = AttachmentUtilities.getExternalStorageAvailableSize(mContext);
        if (sdCardSize == AttachmentUtilities.SDCARD_NOT_AVAILABLE) {
            Utility.showToast(getActivity(), R.string.message_view_status_attachment_not_saved);
            return;
        }
        if (sdCardSize < info.mSize) {
            Utility.showToast(getActivity(), R.string.message_view_status_sdcard_not_enough);
            return;
        }
        /** @} */
        if (info.isFileSaved()) {
            // Nothing to do - we have the file saved.
            return;
        }

        File savedFile = performAttachmentSave(info);
        if (savedFile != null) {
            Utility.showToast(getActivity(), String.format(
                    mContext.getString(R.string.message_view_status_attachment_saved),
                    savedFile.getName()));
        } else {
            Utility.showToast(getActivity(), R.string.message_view_status_attachment_not_saved);
        }
    }

    /**
     * Use to update the UI buttons of the deleted attachment
     * @param info
     */
    private void updateDeletedAttachment(MessageViewAttachmentInfo info) {
        info.mDenyFlags = info.DENY_DELETED;
        info.mAllowView = false;
        info.mAllowSave = false;
        info.mAllowInstall = false;
        updateAttachmentButtons(info, false);
        Logging.d(TAG, "Attachment " +info.mId + " can't be found in DB, reset UI buttons");
    }

    private File performAttachmentSave(MessageViewAttachmentInfo info) {
        // Checking compulsory fields of the attachment info for avoiding
        // sequential DownloadManager exceptions
        if (info.mSize == 0 || TextUtils.isEmpty(info.mName)
                || TextUtils.isEmpty(info.mContentType)) {
            return null;
        }

        Attachment attachment = Attachment.restoreAttachmentWithId(mContext, info.mId);
        if (null == attachment) {
            //This attachment may be deleted. Update the UI button.
            updateDeletedAttachment(info);
            return null;
        }

        Uri attachmentUri = AttachmentUtilities.getAttachmentUri(mAccountId, attachment.mId);

        // Using the default volume path, cause we could have many external volumes.
        /** M: MTK Dependence */
        String attsSavedPath = StorageManagerEx.getDefaultPath();
        try {
            File downloads = new File(attsSavedPath, Environment.DIRECTORY_DOWNLOADS);
            if (! downloads.mkdirs()) {
                Logging.w("MessageViewFragmentBase performAttachmentSave mkdirs failed");
            }
            /// M: Attachment should be save as info.mName
            File file = Utility.createUniqueFile(downloads, info.mName);
            Logging.d("MessageViewFragmentBase performAttachmentSave save attachment"
                    + " to external storage. attachment.mid=" + attachment.mId 
                    + ", attachment.mFileName=" + attachment.mFileName);
            Uri contentUri = null;
            /// M: inject a mock attachment URI when run testcase. @{
            if (!Configuration.mIsRunTestcase) {
                contentUri = AttachmentUtilities
                        .resolveAttachmentIdToContentUri(mContext
                                .getContentResolver(), attachmentUri);
            } else {
                contentUri = createMockAttachment(info.mName);
            }
            /// @}
            InputStream in = mContext.getContentResolver().openInputStream(contentUri);
            OutputStream out = new FileOutputStream(file);
            int copyLength = IOUtils.copy(in, out);
            out.flush();
            out.close();
            in.close();
            Logging.d("MessageViewFragmentBase performAttachmentSave attachment saved"
                    + " as " + file.getName() + " ,length=" + copyLength);
            String absolutePath = file.getAbsolutePath();

            // Although the download manager can scan media files, scanning only happens after the
            // user clicks on the item in the Downloads app. So, we run the attachment through
            // the media scanner ourselves so it gets added to gallery / music immediately.
            MediaScannerConnection.scanFile(mContext, new String[] {absolutePath},
                    null, null);

            DownloadManager dm =
                    (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);

            String mimeType;
            /** M: MTK Dependence. For sd drm file, use proper mime type in adding to downloads database
              * system, then the file can be opened @{ */
            if (FeatureOption.MTK_DRM_APP && AttachmentInfo.MIME_DCF.equalsIgnoreCase(info.mContentType)) {
                mimeType = OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT;
            } else {
                mimeType = info.mContentType;
            }
            /** @} */
            dm.addCompletedDownload(info.mName, info.mName,
                    false /* do not use media scanner */,
                    mimeType, absolutePath, info.mSize,
                    true /* show notification */);

            // Cache the stored file information.
            info.setSavedPath(absolutePath);

            // Update our buttons.
            updateAttachmentButtons(info, true);

            return file;

        } catch (IOException ioe) {
            // Ignore. Callers will handle it from the return code.
            Logging.d("MessageViewFragmentBase performAttachmentSave exception: " + ioe.getMessage());
        }

        return null;
    }

    private void onOpenAttachment(MessageViewAttachmentInfo info) {
        Attachment attachment = Attachment.restoreAttachmentWithId(mContext, info.mId);
        if (attachment == null) {
            Utility.showToast(getActivity(), R.string.message_view_display_attachment_toast);
            updateDeletedAttachment(info);
            return;
        }

        if (info.mSize == 0) {
            Utility.showToast(getActivity(), R.string.message_view_display_attachment_toast);
            return;
        }

        /** M: if the attachments is not exists now in storage, just return. @{ */
        if(!Utility.attachmentExists(mContext, attachment) ) {
            Utility.showToast(getActivity(), R.string.attachment_info_deleted);
            return;
        }
        /** @} */

        if (info.mAllowInstall) {
            if (!Utility.isExternalStorageMounted()) {
                Utility.showToast(getActivity(), R.string.message_view_status_attachment_not_saved);
                return;
            }
            if (!info.mIsLocal) {
                // The package installer is unable to install files from a content URI; it must be
                // given a file path. Therefore, we need to save it first in order to proceed
                if (!info.mAllowSave || !Utility.isExternalStorageMounted()) {
                    Utility.showToast(getActivity(),
                            R.string.message_view_status_attachment_not_saved);
                    return;
                }

                if (!info.isFileSaved()) {
                    if (performAttachmentSave(info) == null) {
                        // Saving failed for some reason - bail.
                        Utility.showToast(getActivity(),
                                R.string.message_view_status_attachment_not_saved);
                        return;
                    }
                }
            }
        }
        try {
            Intent intent = info.getAttachmentIntent(mContext, mAccountId);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Utility.showToast(getActivity(), R.string.message_view_display_attachment_toast);
        }
    }

    private void onInfoAttachment(final MessageViewAttachmentInfo attachment) {
        AttachmentInfoDialog dialog =
            AttachmentInfoDialog.newInstance(getActivity(), attachment.mDenyFlags);
        dialog.show(getActivity().getFragmentManager(), null);
    }

    private void onLoadAttachment(final MessageViewAttachmentInfo attachment) {
        // Check if low storage at the very beginning
        final Preferences pref = Preferences.getPreferences(mContext);
        if (pref.getLowStorage()) {
            Toast.makeText(mContext,
                    R.string.low_storage_attachment_cannot_download, Toast.LENGTH_LONG).show();
            Toast.makeText(mContext,
                    R.string.low_storage_hint_delete_mail, Toast.LENGTH_LONG).show();
            Logging.d("onLoadAttachment canceled due to low storage");
            return;
        }

        if (!Utility.hasConnectivity(getActivity())) {
            ConnectionAlertDialog.newInstance().show(
                    getActivity().getFragmentManager(), "connectionalertdialog");
            return;
        }

        attachment.mLoadButton.setVisibility(View.GONE);
        // If there's nothing in the download queue, we'll probably start right away so wait a
        // second before showing the cancel button
        if (AttachmentDownloadService.getQueueSize() == 0) {
            // Set to invisible; if the button is still in this state one second from now, we'll
            // assume the download won't start right away, and we make the cancel button visible
            attachment.mCancelButton.setVisibility(View.GONE);
            // Create the timed task that will change the button state
            new EmailAsyncTask<Void, Void, Void>(mTaskTracker) {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) { }
                    return null;
                }
                @Override
                protected void onSuccess(Void result) {
                    // If the timeout completes and the attachment has not loaded, show cancel
                    if (!attachment.mLoaded) {
                        // The loadButton and cancelButton should not display at the same time.
                        if (attachment.mLoadButton.getVisibility() == View.GONE) {
                           attachment.mCancelButton.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }.executeParallel();
        } else {
            attachment.mCancelButton.setVisibility(View.VISIBLE);
        }

        // The "stop" button may be disabled in the case of the auto-downloading,
        // thus enable it here
        attachment.mIsUserCancelled = false;
        attachment.mCancelButton.setEnabled(true);
        attachment.showProgressIndeterminate();
        mController.loadAttachment(attachment.mId, mMessageId, mAccountId);
    }

    private void onCancelAttachment(MessageViewAttachmentInfo attachment) {
        attachment.mIsUserCancelled = true;
        attachment.mLoadButton.setVisibility(View.VISIBLE);
        attachment.mCancelButton.setVisibility(View.GONE);
        attachment.hideProgress();

        mController.cancelLoadAttachment(attachment.mId);
    }

    /**
     * Called by ControllerResults. Show the "View" and "Save" buttons; hide "Load" and "Stop"
     *
     * @param attachmentId the attachment that was just downloaded
     */
    private void doFinishLoadAttachment(long attachmentId) {
        MessageViewAttachmentInfo info = findAttachmentInfo(attachmentId);
        if (info != null) {
            info.mLoaded = true;
            updateAttachmentButtons(info , true);
        }
    }

    private void showPicturesInHtml() {
        boolean picturesAlreadyLoaded = (mTabFlags & TAB_FLAGS_PICTURE_LOADED) != 0;
        if ((mMessageContentView != null) && !picturesAlreadyLoaded) {
            blockNetworkLoads(false);

            // Prompt the user to always show images from this sender.
            makeVisible(UiUtilities.getView(getView(), R.id.always_show_pictures_button), true);

            addTabFlags(TAB_FLAGS_PICTURE_LOADED);
            // TODO: why is this calling setMessageHtml just because the images can load now?
            setMessageHtml(mHtmlTextWebView);
        }
    }

    private void showDetails() {
        if (!isMessageOpen()) {
            return;
        }

        /** M: VIP feature: Show per recipient in a {@link VipTextView} @{ */
        boolean done = false;
        if (!mDetailsFilled) {
            String date = formatDate(mMessage.mTimeStamp, true);
            setDetailsRow(mDetailsExpanded, date, R.id.date, R.id.date_row);

            MessageViewUtils.setRowVisibility(mMessage.mTo, mDetailsExpanded, R.id.to_row);
            MessageViewUtils.setRowVisibility(mMessage.mCc, mDetailsExpanded, R.id.cc_row);
            MessageViewUtils.setRowVisibility(mMessage.mBcc, mDetailsExpanded, R.id.bcc_row);

            TempAddress[] allAddresses = MessageViewUtils.getAllAddresses(mMessage);
            if (allAddresses.length == 0) {
                return;
            }
            LinearLayout[] layout = new LinearLayout[3];
            layout[0] = (LinearLayout) mDetailsExpanded.findViewById(R.id.to_layout);
            layout[1] = (LinearLayout) mDetailsExpanded.findViewById(R.id.cc_layout);
            layout[2] = (LinearLayout) mDetailsExpanded.findViewById(R.id.bcc_layout);

            layout[0].removeAllViews();
            layout[1].removeAllViews();
            layout[2].removeAllViews();
            if (allAddresses.length <= VIEWS_NUMBER_TO_SHOW) {
                MessageViewUtils.setDetailsLayout(this, allAddresses, layout, 0,
                        allAddresses.length);
                done = true;
            } else {
                MessageViewUtils.setDetailsLayout(this, allAddresses, layout, 0,
                        VIEWS_NUMBER_TO_SHOW);
            }

            mDetailsFilled = true;
            mDetailsCollapsed.setVisibility(View.GONE);
            mDetailsExpanded.setVisibility(View.VISIBLE);
            if (!done) {
                MessageViewUtils.setDetailsLayout(this, allAddresses, layout, VIEWS_NUMBER_TO_SHOW,
                        allAddresses.length);
            }
        } else {
            mDetailsCollapsed.setVisibility(View.GONE);
            mDetailsExpanded.setVisibility(View.VISIBLE);
            MessageViewUtils.updateDetailsExpanded(mContext, mDetailsExpanded, null);
        }
        /** @} */
    }

    private void hideDetails() {
        /// M: VIP feature: update the UI state of mAddressView.
        MessageViewUtils.updateAddressesView(mMessage, mContext, mAddressesView);
        mDetailsCollapsed.setVisibility(View.VISIBLE);
        mDetailsExpanded.setVisibility(View.GONE);
    }

    private static void setDetailsRow(View root, String text, int textViewId, int rowViewId) {
        if (TextUtils.isEmpty(text)) {
            root.findViewById(rowViewId).setVisibility(View.GONE);
            return;
        }
        root.findViewById(rowViewId).setVisibility(View.VISIBLE);
        ((TextView) UiUtilities.getView(root, textViewId)).setText(text);
    }

    @Override
    public void onClick(View view) {
        if (!isMessageOpen()) {
            return; // Ignore.
        }
        switch (view.getId()) {
            case R.id.badge:
                onClickSender();
                break;
            case R.id.load:
                onLoadAttachment((MessageViewAttachmentInfo) view.getTag());
                break;
            case R.id.info:
                onInfoAttachment((MessageViewAttachmentInfo) view.getTag());
                break;
            case R.id.save:
                /// M: Deal this case within onSaveClick
                onSaveClicked(view);
                break;
            case R.id.open:
                onOpenAttachment((MessageViewAttachmentInfo) view.getTag());
                break;
            case R.id.cancel:
                onCancelAttachment((MessageViewAttachmentInfo) view.getTag());
                break;
            case R.id.show_message:
                setCurrentTab(TAB_MESSAGE);
                break;
            case R.id.show_invite:
                setCurrentTab(TAB_INVITE);
                break;
            case R.id.show_attachments:
                setCurrentTab(TAB_ATTACHMENT);
                break;
            case R.id.show_pictures:
                showPicturesInHtml();
                break;
            case R.id.always_show_pictures_button:
                setShowImagesForSender();
                break;
            case R.id.sub_header_contents_collapsed:
                showDetails();
                break;
            case R.id.sub_header_contents_expanded:
                hideDetails();
                break;
            case R.id.msg_remain_btn:
                doLoadMsgBackground(mMessageId);
                break;
                /** M: Adjust the UI layout and logic to support VIP new feature @{ */
            case R.id.vip_switcher:
                onVipSwitcherClick();
                break;
            /** @} */
        }
    }

    /**
     * Start loading contact photo and presence.
     */
    private void queryContactStatus() {
        if (!isMessageOpen()) {
            return;
        }
        initContactStatusViews(); // Initialize the state, just in case.

        // Find the sender email address, and start presence check.
        Address sender = Address.unpackFirst(mMessage.mFrom);
        if (sender != null) {
            String email = sender.getAddress();
            if (email != null) {
                getLoaderManager().restartLoader(PHOTO_LOADER_ID,
                        ContactStatusLoaderCallbacks.createArguments(email),
                        new ContactStatusLoaderCallbacks(this));
            }
        }
    }

    /**
     * Called by {@link LoadMessageTask} and {@link ReloadMessageTask} to load a message in a
     * subclass specific way.
     *
     * NOTE This method is called on a worker thread!  Implementations must properly synchronize
     * when accessing members.
     *
     * @param activity the parent activity.  Subclass use it as a context, and to show a toast.
     */
    protected abstract Message openMessageSync(Activity activity);

    /**
     * Called in a background thread to reload a new copy of the Message in case something has
     * changed.
     */
    protected Message reloadMessageSync(Activity activity) {
        return openMessageSync(activity);
    }

    // Doing load message background
    // TODO: increase priority of this view
    private void doLoadMsgBackground(long msgId) {

        /** M: Check if low storage when start download the whole message @{ */
        if (RefreshManager.getInstance(mContext).checkIsLowStorage(mContext)) {
            return;
        }
        /** @} */

        if (!Utility.hasConnectivity(getActivity())) {
            mPartialDownload = false;
            ConnectionAlertDialog.newInstance().show(
                    getActivity().getFragmentManager(), "connectionalertdialog");
            return;
        }

        mPartialDownload = true;
        updateRemainProgress();

        Logging.d(Logging.LOG_TAG, "+++ Load partial Message id = " + msgId);
        mControllerCallback.getWrappee().setWaitForLoadMessageId(msgId);
        mController.loadMessageForView(msgId);
    }

    /**
     * Async task for loading a single message outside of the UI thread
     */
    private class LoadMessageTask extends EmailAsyncTask<Void, Void, Message> {

        private final boolean mOkToFetch;
        private Mailbox mMailbox;

        /**
         * Special constructor to cache some local info
         */
        public LoadMessageTask(boolean okToFetch) {
            super(mTaskTracker);
            mOkToFetch = okToFetch;
            mMessageLoading = false;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mMessageLoading = true;
        }
        
        @Override
        protected Message doInBackground(Void... params) {
            EmailAsyncTask.printStartLog("LoadMessageTask#doInBackground");
            Activity activity = getActivity();
            Message message = null;
            if (activity != null) {
                message = openMessageSync(activity);
            }
            if (message != null) {
                mMailbox = Mailbox.restoreMailboxWithId(mContext, message.mMailboxKey);
                if (mMailbox == null) {
                    message = null; // mailbox removed??
                }
            } else if (MessageViewFragmentBase.this instanceof MessageFileViewFragment
                    && !isCancelled()){
                Utility.showToast(activity, R.string.message_view_display_attachment_toast);
            }
            EmailAsyncTask.printStopLog("LoadMessageTask#doInBackground");
            return message;
        }

        @Override
        protected void onSuccess(Message message) {
            mLoadMessageTask = null;
            if (message == null) {
                resetView();
                mCallback.onMessageNotExists();
                mMessageLoading = false;
                return;
            }
            /* M: if the flags contains FLAG_BODY_TOO_LARGE, the message body must be truncated.
             * @{*/
            if ((message.mFlags & Message.FLAG_BODY_TOO_LARGE) == Message.FLAG_BODY_TOO_LARGE) {
                Toast.makeText(mContext, R.string.message_body_too_large,
                        Toast.LENGTH_SHORT).show();
            }
            /*@}*/
            mCallback.onNeedUpdateAtionBarTitle();
            mMessageId = message.mId;
            Logging.d(TAG, "Loading message : " + mMessageId);
            mMessageIsReload = false;
            reloadUiFromMessage(message, mOkToFetch);
            queryContactStatus();
            onMessageShown(mMessageId, mMailbox);
            RecentMailboxManager.getInstance(mContext).touch(mAccountId, message.mMailboxKey);
            mMessageLoading = false;
        }

        @Override
        protected void onCancelled(Message result) {
            mMessageLoading = false;
        }
    }

    /**
     * Kicked by {@link MessageObserver}.  Reload the message and update the views.
     */
    private class ReloadMessageTask extends EmailAsyncTask<Void, Void, Message> {
        public ReloadMessageTask() {
            super(mTaskTracker);
        }

        @Override
        protected Message doInBackground(Void... params) {
            Activity activity = getActivity();
            if (activity == null) {
                return null;
            } else {
                return reloadMessageSync(activity);
            }
        }

        @Override
        protected void onSuccess(Message message) {
            if (message == null || message.mMailboxKey != mMessage.mMailboxKey) {
                if (MessageViewFragmentBase.this instanceof MessageFileViewFragment
                        && !isCancelled()) {
                    Activity activity = getActivity();
                    Utility.showToast(activity, R.string.message_view_display_attachment_toast);
                }
                // Message deleted or moved.
                mCallback.onMessageNotExists();
                return;
            }
            mMessageIsReload = true;
            mMessage = message;
            updateHeaderView(mMessage);
        }
    }

    /**
     * Called when a message is shown to the user.
     */
    protected void onMessageShown(long messageId, Mailbox mailbox) {
    }

    /**
     * Called when the message body is loaded.
     */
    protected void onPostLoadBody() {
    }

    /**
     * Async task for loading a single message body outside of the UI thread
     */
    private class LoadBodyTask extends EmailAsyncTask<Void, Void, String[]> {

        private final long mId;
        private boolean mErrorLoadingMessageBody;
        private final boolean mAutoShowPictures;

        /**
         * Special constructor to cache some local info
         */
        public LoadBodyTask(long messageId, boolean autoShowPictures) {
            super(mTaskTracker);
            mId = messageId;
            mAutoShowPictures = autoShowPictures;
        }

        @Override
        protected String[] doInBackground(Void... params) {
            EmailAsyncTask.printStartLog("LoadBodyTask#doInBackground");
            try {
                String text = null;
                String html = Body.restoreBodyHtmlWithMessageId(mContext, mId);
                if (html == null) {
                    text = Body.restoreBodyTextWithMessageId(mContext, mId);
                }
                EmailAsyncTask.printStopLog("LoadBodyTask#doInBackground");
                return new String[] { text, html };
            } catch (RuntimeException re) {
                // This catches SQLiteException as well as other RTE's we've seen from the
                // database calls, such as IllegalStateException
                Log.d(Logging.LOG_TAG, "Exception while loading message body", re);
                mErrorLoadingMessageBody = true;
                EmailAsyncTask.printStopLog("LoadBodyTask#doInBackground");
                return null;
            }
        }

        @Override
        protected void onSuccess(String[] results) {
            mLoadBodyTask = null;
            if (results == null) {
                if (mErrorLoadingMessageBody) {
                    Utility.showToast(getActivity(), R.string.error_loading_message_body);
                }
                resetView();
                return;
            }
            showContent(true, false);
            reloadUiFromBody(results[0], results[1], mAutoShowPictures);    // text, html
            updateRemainMsgInfo(mMessage);
            onPostLoadBody();
            if (mPartialDownload) {
                Logging.d(TAG," mPartialDownload is true and start Loading for message " 
                        + mMessageId );
                doLoadMsgBackground(mMessageId);
            }
        }
    }

    private void triggerDownload(long attachmentId) {
        synchronized (mDownloadAttachmentList) {
            long id = mDownloadAttachmentList.remove(0);
            Logging.d(Logging.LOG_TAG, "++++++++ try to download attachmentId: "
                    + id + " in background!!!");
            mController.loadAttachment(id, mMessageId, mAccountId);
        }
    }

    /**
     * This function used to download inline objects.
     * 
     * @param attachmentId
     */
    private void onDownloadAttachmentSilently(long attachmentId) {
        // Prompt that the storage is low and will not sync Email
        final Preferences pref = Preferences.getPreferences(this.getActivity());
        pref.checkLowStorage();
        if (pref.getLowStorage()) {
            return;
        }

        if (!Utility.hasConnectivity(getActivity())) {
            return;
        }

        synchronized (mDownloadAttachmentList) {
            mDownloadAttachmentList.add(attachmentId);
        }
        triggerDownload(attachmentId);
    }

    /**
     * Async task for loading attachments
     *
     * Note:  This really should only be called when the message load is complete - or, we should
     * leave open a listener so the attachments can fill in as they are discovered.  In either case,
     * this implementation is incomplete, as it will fail to refresh properly if the message is
     * partially loaded at this time.
     */
    private class LoadAttachmentsTask extends EmailAsyncTask<Long, Void, Attachment[]> {

        public LoadAttachmentsTask() {
            super(mTaskTracker);
        }

        @Override
        protected Attachment[] doInBackground(Long... messageIds) {
            EmailAsyncTask.printStartLog("LoadAttachmentsTask#doInBackground");
            Attachment[] att = Attachment.restoreAttachmentsWithMessageId(mContext, messageIds[0]);
            EmailAsyncTask.printStopLog("LoadAttachmentsTask#doInBackground");
            return att;
        }

        @Override
        protected void onSuccess(Attachment[] attachments) {
            mLoadAttachmentsTask = null;
            if (attachments == null) {
                return;
            }
            boolean htmlChanged = false;
            int numDisplayedAttachments = 0;
            List<Attachment> attachList = new ArrayList<Attachment>();
            String contentId = null;
            String contentUri = null;
            for (Attachment attachment : attachments) {
                contentId = attachment.mContentId;
                contentUri = attachment.mContentUri;
                long id = attachment.mId;
                boolean isAttachmentInline = false;
                Logging.d(TAG, "++++  mContentId: " + contentId + " contentUri: " + contentUri);

                isAttachmentInline = isInlineAttachment(attachment, mMessage, mHtmlTextRaw);

                if (mHtmlTextRaw != null && contentId != null
                        && contentUri != null && isAttachmentInline) {
                    // for html body, replace CID for inline images
                    mHtmlTextRaw = AttachmentUtilities
                            .refactorHtmlTextRaw(mHtmlTextRaw, attachment);
                    htmlChanged = true;
                } else if (contentId != null && contentUri == null && isAttachmentInline) {
                    Logging.d(TAG, "+++ Load Attachments Silently contentId: "
                            + contentId);
                    onDownloadAttachmentSilently(id);
                } else {
                    if (findAttachmentInfoFromView(id) == null) {
                        attachList.add(attachment);
                    }
                    numDisplayedAttachments++;
                    /** M: once html doesn't have img tag, we need also disable picture tab @{ */
                    if (attachment.mContentId != null && (mHtmlTextRaw != null)
                            && IMG_TAG_START_REGEX.matcher(mHtmlTextRaw).find()) {
                        mHtmlTextRaw = AttachmentUtilities.refactorHtmlTextRaw(mHtmlTextRaw,
                                attachment);
                        if (!IMG_TAG_START_REGEX.matcher(mHtmlTextRaw).find()) {
                            updateTabs(mTabFlags & ~TAB_FLAGS_HAS_PICTURES
                                    & ~TAB_FLAGS_PICTURE_LOADED);
                        }
                        htmlChanged = true;
                    }
                    /** @}*/
                }
            }
            addAttachments(attachList);
            setAttachmentCount(numDisplayedAttachments);
            mHtmlTextWebView = mHtmlTextRaw;
            mHtmlTextRaw = null;
            if (htmlChanged) {
                setMessageHtml(mHtmlTextWebView);
            }
        }
    }

    private static Bitmap getPreviewIcon(Context context, AttachmentInfo attachment) {
        try {
            return BitmapFactory.decodeStream(
                    context.getContentResolver().openInputStream(
                            AttachmentUtilities.getAttachmentThumbnailUri(
                                    attachment.mAccountKey, attachment.mId,
                                    PREVIEW_ICON_WIDTH,
                                    PREVIEW_ICON_HEIGHT)));
        } catch (Exception e) {
            Log.d(Logging.LOG_TAG, "Attachment preview failed with exception " + e.getMessage());
            return null;
        }
    }

    /**
     * Subclass of AttachmentInfo which includes our views and buttons related to attachment
     * handling, as well as our determination of suitability for viewing (based on availability of
     * a viewer app) and saving (based upon the presence of external storage)
     */
    public static class MessageViewAttachmentInfo extends AttachmentInfo {
        private Button mOpenButton;
        private Button mSaveButton;
        private Button mLoadButton;
        private Button mInfoButton;
        private Button mCancelButton;
        private ImageView mIconView;
        private boolean mIsUserCancelled = false;

        private static Map<AttachmentInfo, String> sSavedFileInfos = Maps.newHashMap();

        // Don't touch it directly from the outer class.
        private final ProgressBar mProgressView;
        private boolean mLoaded;

        private MessageViewAttachmentInfo(Context context, Attachment attachment,
                ProgressBar progressView) {
            super(context, attachment);
            mProgressView = progressView;
        }

        /**
         * Create a new attachment info based upon an existing attachment info. Display
         * related fields (such as views and buttons) are copied from old to new.
         */
        private MessageViewAttachmentInfo(Context context, MessageViewAttachmentInfo oldInfo) {
            super(context, oldInfo);
            mOpenButton = oldInfo.mOpenButton;
            mSaveButton = oldInfo.mSaveButton;
            mLoadButton = oldInfo.mLoadButton;
            mInfoButton = oldInfo.mInfoButton;
            mCancelButton = oldInfo.mCancelButton;
            mIconView = oldInfo.mIconView;
            mProgressView = oldInfo.mProgressView;
            mLoaded = oldInfo.mLoaded;
            mIsUserCancelled = oldInfo.mIsUserCancelled;
        }

        public void hideProgress() {
            // Don't use GONE, which'll break the layout.
            if (mProgressView.getVisibility() != View.INVISIBLE) {
                mProgressView.setVisibility(View.INVISIBLE);
            }
        }

        public void showProgress(int progress) {
            // M: Hide on completion don't call setProgress.
            if (progress == 100) {
                hideProgress();
                return;
            }

            if (mProgressView.getVisibility() != View.VISIBLE) {
                mProgressView.setVisibility(View.VISIBLE);
            }
            if (mProgressView.isIndeterminate()) {
                mProgressView.setIndeterminate(false);
            }
            mProgressView.setProgress(progress);
        }

        public void showProgressIndeterminate() {
            if (mProgressView.getVisibility() != View.VISIBLE) {
                mProgressView.setVisibility(View.VISIBLE);
            }
            if (!mProgressView.isIndeterminate()) {
                mProgressView.setIndeterminate(true);
            }
        }

        /**
         * Determines whether or not this attachment has a saved file in the external storage. That
         * is, the user has at some point clicked "save" for this attachment.
         *
         * Note: this is an approximation and uses an in-memory cache that can get wiped when the
         * process dies, and so is somewhat conservative. Additionally, the user can modify the file
         * after saving, and so the file may not be the same (though this is unlikely).
         */
        public boolean isFileSaved() {
            String path = getSavedPath();
            if (path == null) {
                return false;
            }
            boolean savedFileExists = new File(path).exists();
            if (!savedFileExists) {
                // Purge the cache entry.
                setSavedPath(null);
            }
            return savedFileExists;
        }

        private void setSavedPath(String path) {
            if (path == null) {
                sSavedFileInfos.remove(this);
            } else {
                sSavedFileInfos.put(this, path);
            }
        }

        /**
         * Returns an absolute file path for the given attachment if it has been saved. If one is
         * not found, {@code null} is returned.
         *
         * Clients are expected to validate that the file at the given path is still valid.
         */
        private String getSavedPath() {
            return sSavedFileInfos.get(this);
        }

        @Override
        protected Uri getUriForIntent(Context context, long accountId) {
            // Prefer to act on the saved file for intents.
            String path = getSavedPath();
            Uri uri = null;
            if (path != null) {
                try {
                    uri = Uri.fromFile(new File(path));
                } catch (Exception ex) {
                    uri =  Uri.parse("file://" + path);
                }
            } else {
                uri = super.getUriForIntent(context, accountId);
            }
            return uri;
        }

        /// M: Support to testcase load attachment status. @{
        public Button getOpenButton() {
            return mOpenButton;
        }

        public Button getSaveButton() {
            return mSaveButton;
        }

        public Button getLoadButton() {
            return mLoadButton;
        }

        public Button getCancelButton() {
            return mCancelButton;
        }

        public ProgressBar getProgressView() {
            return mProgressView;
        }

        public Button getInfoButton() {
            return mInfoButton;
        }

        public boolean isLoaded() {
            return mLoaded;
        }
        /// @}
    }

    /**
     * Updates all current attachments on the attachment tab.
     */
    private void updateAttachmentTab() {
        for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
            View view = mAttachments.getChildAt(i);
            MessageViewAttachmentInfo oldInfo = (MessageViewAttachmentInfo)view.getTag();
            MessageViewAttachmentInfo newInfo =
                    new MessageViewAttachmentInfo(getActivity(), oldInfo);
            //No need start asynctask to update thumbnail since it have loaded.
            updateAttachmentButtons(newInfo , false);
            view.setTag(newInfo);
        }
    }

    /**
     * Updates the attachment buttons. Adjusts the visibility of the buttons as well
     * as updating any tag information associated with the buttons.
     * @param isUpdateIcon 
     *        true: start a asynctask update the icon;
     *        false: not update icon immediately.
     */
    private void updateAttachmentButtons(MessageViewAttachmentInfo attachmentInfo ,
            boolean isUpdateIcon) {
        ImageView attachmentIcon = attachmentInfo.mIconView;
        Button openButton = attachmentInfo.mOpenButton;
        Button saveButton = attachmentInfo.mSaveButton;
        Button loadButton = attachmentInfo.mLoadButton;
        Button infoButton = attachmentInfo.mInfoButton;
        Button cancelButton = attachmentInfo.mCancelButton;

        if (!attachmentInfo.mAllowView) {
            openButton.setVisibility(View.GONE);
        }
        if (!attachmentInfo.mAllowSave) {
            saveButton.setVisibility(View.GONE);
        }

        if (!attachmentInfo.mAllowView && !attachmentInfo.mAllowSave) {
            // This attachment may never be viewed or saved, so block everything
            attachmentInfo.hideProgress();
            openButton.setVisibility(View.GONE);
            saveButton.setVisibility(View.GONE);
            loadButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
            infoButton.setVisibility(View.VISIBLE);
        } else if (attachmentInfo.mLoaded) {
            // If the attachment is loaded, show 100% progress
            // Note that for POP3 messages, the user will only see "Open" and "Save",
            // because the entire message is loaded before being shown.
            // Hide "Load" and "Info", show "View" and "Save"
            attachmentInfo.showProgress(100);
            if (attachmentInfo.mAllowSave) {
                saveButton.setVisibility(View.VISIBLE);

                boolean isFileSaved = attachmentInfo.isFileSaved();
                saveButton.setEnabled(!isFileSaved);
                if (!isFileSaved) {
                    saveButton.setText(R.string.message_view_attachment_save_action);
                } else {
                    saveButton.setText(R.string.message_view_attachment_saved);
                }
            }
            if (attachmentInfo.mAllowView) {
                // Set the attachment action button text accordingly
                if (attachmentInfo.mContentType.startsWith("audio/") ||
                        attachmentInfo.mContentType.startsWith("video/")) {
                    openButton.setText(R.string.message_view_attachment_play_action);
                } else if (attachmentInfo.mAllowInstall) {
                    openButton.setText(R.string.message_view_attachment_install_action);
                } else {
                    openButton.setText(R.string.message_view_attachment_view_action);
                }
                openButton.setVisibility(View.VISIBLE);
            }
            if (attachmentInfo.mDenyFlags == AttachmentInfo.ALLOW) {
                infoButton.setVisibility(View.GONE);
            } else {
                infoButton.setVisibility(View.VISIBLE);
            }
            loadButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE); 
            if (isUpdateIcon) {
                updatePreviewIcon(attachmentInfo);
            }
        } else {
            // The attachment is not loaded, so present UI to start downloading it
            // Show "Load"; hide "View", "Save" and "Info"
            saveButton.setVisibility(View.GONE);
            openButton.setVisibility(View.GONE);
            infoButton.setVisibility(View.GONE);

            // If the attachment is queued, show the indeterminate progress bar.  From this point,.
            // any progress changes will cause this to be replaced by the normal progress bar
            if (AttachmentDownloadService.isAttachmentQueued(attachmentInfo.mId)) {
                attachmentInfo.showProgressIndeterminate();
                loadButton.setVisibility(View.GONE);
                cancelButton.setVisibility(View.VISIBLE);
            } else {
                loadButton.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.GONE);
                attachmentInfo.hideProgress();
            }
        }
        openButton.setTag(attachmentInfo);
        saveButton.setTag(attachmentInfo);
        loadButton.setTag(attachmentInfo);
        infoButton.setTag(attachmentInfo);
        cancelButton.setTag(attachmentInfo);
    }

    /**
     * Copy data from a cursor-refreshed attachment into the UI.  Called from UI thread.
     *
     * @param attachment A single attachment loaded from the provider
     * @param isUpdateIcon update the attachment icon or not.
     */
    private void addAttachment(Attachment attachment , boolean isUpdateIcon) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.message_view_attachment, null);

        TextView attachmentName = (TextView) UiUtilities.getView(view, R.id.attachment_name);
        TextView attachmentInfoView = (TextView) UiUtilities.getView(view, R.id.attachment_info);
        ImageView attachmentIcon = (ImageView) UiUtilities.getView(view, R.id.attachment_icon);
        Button openButton = (Button) UiUtilities.getView(view, R.id.open);
        Button saveButton = (Button) UiUtilities.getView(view, R.id.save);
        Button loadButton = (Button) UiUtilities.getView(view, R.id.load);
        Button infoButton = (Button) UiUtilities.getView(view, R.id.info);
        Button cancelButton = (Button) UiUtilities.getView(view, R.id.cancel);
        ProgressBar attachmentProgress = (ProgressBar) UiUtilities.getView(view, R.id.progress);

        MessageViewAttachmentInfo attachmentInfo = new MessageViewAttachmentInfo(
                mContext, attachment, attachmentProgress);

        // Check whether the attachment already exists or the content uri is for a
        // local file regardless of whether this file is still existed
        if ((attachment != null && !TextUtils.isEmpty(attachment.mContentUri)
                && !attachment.mContentUri.contains(AttachmentUtilities.AUTHORITY))
                    || Utility.attachmentExists(mContext, attachment)) {
            attachmentInfo.mLoaded = true;
        }

        attachmentInfo.mOpenButton = openButton;
        attachmentInfo.mSaveButton = saveButton;
        attachmentInfo.mLoadButton = loadButton;
        attachmentInfo.mInfoButton = infoButton;
        attachmentInfo.mCancelButton = cancelButton;
        attachmentInfo.mIconView = attachmentIcon;

        updateAttachmentButtons(attachmentInfo ,isUpdateIcon);

        view.setTag(attachmentInfo);
        openButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);
        loadButton.setOnClickListener(this);
        infoButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);

        /// M: Add (msg) suffix for all of message/rfc822 attachment to avoid confuse
        attachmentName.setText(MimeUtility.generateAttName(attachmentInfo.mName,
                attachment.mMimeType));
        attachmentInfoView.setText(UiUtilities.formatSize(mContext, attachmentInfo.mSize));

        mAttachments.addView(view);
        mAttachments.setVisibility(View.VISIBLE);
    }

    private MessageViewAttachmentInfo findAttachmentInfoFromView(long attachmentId) {
        for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
            MessageViewAttachmentInfo attachmentInfo =
                    (MessageViewAttachmentInfo) mAttachments.getChildAt(i).getTag();
            if (attachmentInfo.mId == attachmentId) {
                return attachmentInfo;
            }
        }
        return null;
    }

    private void updateRemainProgress() {
        mRemainBtnView.removeAllViews();

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.mtk_message_view_fragment_remain_btn, null);
        TextView msgRemainBtn = (TextView) v.findViewById(R.id.msg_remain_btn);
        msgRemainBtn.setVisibility(View.GONE);

        ProgressBar msgProgress = (ProgressBar) v.findViewById(R.id.msg_remain_loading_progress);
        TextView msgLoading = (TextView) v.findViewById(R.id.msg_remain_loading_text);

        makeVisible(msgProgress, true);
        makeVisible(msgLoading, true);

        mRemainBtnView.addView(v);
    }

    private void updateRemainMsgInfo(Message message) {
        mRemainBtnView.removeAllViews();
        if (message == null) {
            return;
        }

        /** M: If the message is a POP message and didn't downloaded completely, let's shown the
         * mRemainBtnView as well as IMAP and EXCHANGE. We will download the whole message when
         * user click the mRemainBtnView.*/
        if (message.mFlagLoaded == Message.FLAG_LOADED_PARTIAL) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View v = inflater.inflate(R.layout.mtk_message_view_fragment_remain_btn, null);
            TextView msgRemainBtn = (TextView) v.findViewById(R.id.msg_remain_btn);
            msgRemainBtn.setOnClickListener(this);

            Log.d(Logging.LOG_TAG, "++++ remaining message id is: " + mMessageId);

//            long remainSize = 0;
//            Cursor c = getContentResolver().query(Message.CONTENT_URI,
//                    MESSAGE_REMAIN_SIZE_PROJECTION, Message.MessageColumns.ID
//                    + "=?", new String[] {
//                        Long.toString(mMessageId)
//                    }, null);
//            try {
//                if (c != null && c.moveToFirst()) {
//                    remainSize = c.getLong(0);
//                }
//            } finally {
//                if (c != null) {
//                    c.close();
//                }
//            }
//
//            Log.d(Logging.LOG_TAG, "message remain size is: " + remainSize);
//
//            String message_remain_text = getString(R.string.message_view_remain_size,
//                    AttachmentUtils.formatSize((float) remainSize));
//            msgRemainBtn.setText(message_remain_text);

            makeVisible(msgRemainBtn, message.mFlagLoaded == Message.FLAG_LOADED_PARTIAL);
            mRemainBtnView.addView(v);
        }
    }

    /**
     * Reload the UI from a provider cursor.  {@link LoadMessageTask#onSuccess} calls it.
     *
     * Update the header views, and start loading the body.
     *
     * @param message A copy of the message loaded from the database
     * @param okToFetch If true, and message is not fully loaded, it's OK to fetch from
     * the network.  Use false to prevent looping here.
     */
    protected void reloadUiFromMessage(Message message, boolean okToFetch) {
        mMessage = message;
        mAccountId = message.mAccountKey;

        mMessageObserver.register(ContentUris.withAppendedId(Message.CONTENT_URI, mMessage.mId));

        showContent(true, false);
        updateHeaderView(mMessage);
        if (message.mFlagLoaded == Message.FLAG_LOADED_ENVELOPE) {
            if (mMessageContentView != null) {
                mMessageContentView.loadUrl("file:///android_asset/loading.html");
            }
        }
//        updateRemainMsgInfo(mMessage.mFlagLoaded);
        // Handle partially-loaded email, as follows:
        // 1. Check value of message.mFlagLoaded
        // 2. If != LOADED, ask controller to load it
        // 3. Controller callback (after loaded) should trigger LoadBodyTask & LoadAttachmentsTask
        // 4. Else start the loader tasks right away (message already loaded)

        /** M: Check the storage before download the whole message, if the storage is OK and this
         * message just loaded it's envelope, we will download it. Otherwise if it's a partial
         * downloaded message, lets show out the downloaded parts, this looks like pop support the
         * partial download as well. @{ */
        final Preferences pref = Preferences.getPreferences(mContext);
        if (!pref.getLowStorage() && message.mFlagLoaded == Message.FLAG_LOADED_ENVELOPE) {
            if (mMessageContentView != null) {
                mMessageContentView.loadUrl("file:///android_asset/loading.html");
            }
            mControllerCallback.getWrappee().setWaitForLoadMessageId(message.mId);
            mController.loadMessageForView(message.mId);
            Logging.d(Logging.LOG_TAG, "+++ download unloaded Message: " + message.mId);
            return;
        }
        /** @} */

        Address[] fromList = Address.unpack(mMessage.mFrom);
        boolean autoShowImages = false;
        for (Address sender : fromList) {
            String email = sender.getAddress();
            if (shouldShowImagesFor(email)) {
                autoShowImages = true;
                break;
            }
        }
        mControllerCallback.getWrappee().setWaitForLoadMessageId(Message.NO_MESSAGE);
        // Ask for body
        Logging.d(Logging.LOG_TAG,
                "+++ Load complete/partial Message flag: " + message.mFlagLoaded);
        mLoadBodyTask = new LoadBodyTask(message.mId, autoShowImages);
        mLoadBodyTask.executeParallel();

//        if (okToFetch && message.mFlagLoaded != Message.FLAG_LOADED_COMPLETE) {
//            mControllerCallback.getWrappee().setWaitForLoadMessageId(message.mId);
//            mController.loadMessageForView(message.mId);
//            Logging.d(Logging.LOG_TAG, "+++ Load partial Message: " + message.mId);
//        } else {
//            Address[] fromList = Address.unpack(mMessage.mFrom);
//            boolean autoShowImages = false;
//            for (Address sender : fromList) {
//                String email = sender.getAddress();
//                if (shouldShowImagesFor(email)) {
//                    autoShowImages = true;
//                    break;
//                }
//            }
//            mControllerCallback.getWrappee().setWaitForLoadMessageId(Message.NO_MESSAGE);
//            // Ask for body
//            Logging.d(Logging.LOG_TAG, "+++ Load complete Message: " + message.mId);
//            new LoadBodyTask(message.mId, autoShowImages).executeParallel();
//        }
    }

    protected void updateHeaderView(Message message) {
        mSubjectView.setText(message.mSubject);
        final Address from = Address.unpackFirst(message.mFrom);

        // Set sender address/display name
        // Note we set " " for empty field, so TextView's won't get squashed.
        // Otherwise their height will be 0, which breaks the layout.
        if (from != null) {
            final String fromFriendly = from.toFriendly();
            final String fromAddress = from.getAddress();
            mFromNameView.setText(fromFriendly);
            mFromAddressView.setText(fromFriendly.equals(fromAddress) ? " " : fromAddress);
        } else {
            mFromNameView.setText(" ");
            mFromAddressView.setText(" ");
        }
        mDateTimeView.setText(DateUtils.getRelativeTimeSpanString(mContext, message.mTimeStamp)
                .toString());

        /// M: VIP feature:
        // Convert the recipients String to SpannableString, then set it to addressView.
        MessageViewUtils.updateAddressesView(mMessage, mContext, mAddressesView);
        /// M: update the vip icon @{
        updateVipIcon(VipMemberCache.isVIP(message.mFrom));
        /// @}
    }

    /**
     * @return the given date/time in a human readable form.  The returned string always have
     *     month and day (and year if {@code withYear} is set), so is usually long.
     *     Use {@link DateUtils#getRelativeTimeSpanString} instead to save the screen real estate.
     */
    private String formatDate(long millis, boolean withYear) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        DateUtils.formatDateRange(mContext, formatter, millis, millis,
                DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_ABBREV_ALL
                | DateUtils.FORMAT_SHOW_TIME
                | (withYear ? DateUtils.FORMAT_SHOW_YEAR : DateUtils.FORMAT_NO_YEAR));
        return sb.toString();
    }

    /**
     * Reload the body from the provider cursor.  This must only be called from the UI thread.
     *
     * @param bodyText text part
     * @param bodyHtml html part
     *
     * TODO deal with html vs text and many other issues <- WHAT DOES IT MEAN??
     */
    private void reloadUiFromBody(String bodyText, String bodyHtml, boolean autoShowPictures) {
        String text = null;
        mHtmlTextRaw = null;
        boolean hasImages = false;
        String query = null;

        query = mCallback.onGetQueryTerm();
        if (bodyHtml == null) {
            text = bodyText;
            // Escape any inadvertent HTML in the text message
            if (text != null) {
                text = EmailHtmlUtil.escapeCharacterToDisplay(text);
            }
            /*
             * Convert the plain text to HTML
             */
            StringBuffer sb = new StringBuffer("<html><body>");
            if (text != null) {
                sb.append(text);
            }
            sb.append("</body></html>");
            text = sb.toString();
        } else {
            text = bodyHtml;
            mHtmlTextRaw = bodyHtml;
            hasImages = IMG_TAG_START_REGEX.matcher(text).find();
        }

        // Highlight the query terms, if we are opening an searching result message.
        if (query != null && text != null) {
            text = TextUtilities.highlightTermsInHtml(text, query);
            /// M: Highlight original html content to make sure following action base on it
            if (mHtmlTextRaw != null) {
                mHtmlTextRaw = text;
            }
        }
        // TODO this is not really accurate.
        // - Images aren't the only network resources.  (e.g. CSS)
        // - If images are attached to the email and small enough, we download them at once,
        //   and won't need network access when they're shown.
        makeVisible(mAlwaysShowPicturesButton, false);
        if (hasImages) {
            if (mRestoredPictureLoaded || autoShowPictures) {
                blockNetworkLoads(false);
                addTabFlags(TAB_FLAGS_PICTURE_LOADED); // Set for next onSaveInstanceState

                makeVisible(mAlwaysShowPicturesButton, !autoShowPictures);
                // Make sure to reset the flag -- otherwise this will keep taking effect even after
                // moving to another message.
                mRestoredPictureLoaded = false;
            } else {
                addTabFlags(TAB_FLAGS_HAS_PICTURES);
            }
        }
        if (mMainView.isScrollFreeze()) {
            mMainView.scrollTo(0, 0);
        }
        setMessageHtml(text);
        // Ask for attachments after body
        mLoadAttachmentsTask = new LoadAttachmentsTask();
        mLoadAttachmentsTask.executeParallel(mMessage.mId);
        mIsMessageLoadedForTest = true;
    }

    /**
     * Overrides for WebView behaviors.
     */
    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Not handle some unreasonable link.
            if (url != null && url.toLowerCase().startsWith("sip:")) {
                return true;
            }
            // Handle most uri's via intent launch
            url = url.replaceFirst(DEFAULT_EMAIL_PREFIX, HTTP_PREFIX);
            boolean result = false;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, mContext.getPackageName());
            //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            try {
                startActivity(intent);
                result = true;
            } catch (ActivityNotFoundException ex) {
                // If no application can handle the URL, assume that the
                // caller can handle it.
            }
            return result;
            //return mCallback.onUrlInMessageClicked(url);
        }
    }

    private View findAttachmentView(long attachmentId) {
        for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
            View view = mAttachments.getChildAt(i);
            MessageViewAttachmentInfo attachment = (MessageViewAttachmentInfo) view.getTag();
            if (attachment.mId == attachmentId) {
                return view;
            }
        }
        return null;
    }

    private MessageViewAttachmentInfo findAttachmentInfo(long attachmentId) {
        View view = findAttachmentView(attachmentId);
        if (view != null) {
            return (MessageViewAttachmentInfo)view.getTag();
        }
        return null;
    }

    /**
     * Controller results listener.  We wrap it with {@link ControllerResultUiThreadWrapper},
     * so all methods are called on the UI thread.
     */
    private class ControllerResults extends Controller.Result {
        private long mWaitForLoadMessageId;

        public void setWaitForLoadMessageId(long messageId) {
            mWaitForLoadMessageId = messageId;
        }

        @Override
        public void loadMessageForViewCallback(MessagingException result, long accountId,
                long messageId, int progress) {
            final int progressStart = 0;
            final int progressComplete = 100;

            if (messageId != mWaitForLoadMessageId) {
                // We are not waiting for this message to load, so exit quickly
                return;
            }
            if (result == null) {
                switch (progress) {
                    case progressStart:
                        mCallback.onLoadMessageStarted();
                        // Loading from network -- show the progress icon.
                        //showContent(false, true);
                        break;
                    case progressComplete:
                        mWaitForLoadMessageId = -1;
                        mCallback.onLoadMessageFinished();
                        // reload UI and reload everything else too
                        // pass false to LoadMessageTask to prevent looping here
                        cancelAllTasks();
                        mPartialDownload = false;
                        new LoadMessageTask(false).executeParallel();
                        break;
                    default:
                        // do nothing - we don't have a progress bar at this time
                        mPartialDownload = false;
                        break;
                }
            } else {
                mPartialDownload = false;
                if (messageId == mWaitForLoadMessageId) {
                    Message msg = Message.restoreMessageWithId(mContext, messageId);
                    updateRemainMsgInfo(msg);
                    mWaitForLoadMessageId = Message.NO_MESSAGE;
                    String error = mContext.getString(R.string.status_network_error);
                    mCallback.onLoadMessageError(error);
                    //resetView();
                }
            }
        }

        @Override
        public void loadAttachmentCallback(MessagingException result, long accountId,
                long messageId, long attachmentId, int progress) {
            Logging.d(TAG, " MessageView loadAttachmentCallback messageId: " + messageId +
                           " attachmentId: " + attachmentId +
                           " progress: " + progress + " MessagingException: " +
                           (result == null ? "null" : result.getExceptionType()));
            if (messageId == mMessageId) {
                if (result == null) {
                    showAttachmentProgress(attachmentId, progress);
                    switch (progress) {
                        case 100:
                            /// M: Update attachment preview icon and attachment view
                            attachmentDownloaded(attachmentId);
                            break;
                        default:
                            // do nothing - we don't have a progress bar at this time
                            break;
                    }
                } else {
                    MessageViewAttachmentInfo attachment = findAttachmentInfo(attachmentId);
                    if (attachment == null) {
                        // Called before LoadAttachmentsTask finishes.
                        // (Possible if you quickly close & re-open a message)
                        return;
                    }

                    // AttachmentDownloadService will retry to download the attachment
                    // in case of IOERROR, thus do not change the UI in this case.
                    /// M: But if still failed after trying 5 times, we will still
                     // change the UI
                    if ((result.getExceptionType() != MessagingException.IOERROR
                            && !(result.getCause() instanceof IOException))
                            || AttachmentDownloadService.mayDownloadFailed(attachmentId)) {
                        attachment.mCancelButton.setVisibility(View.GONE);
                        attachment.mLoadButton.setVisibility(View.VISIBLE);
                        attachment.hideProgress();
                    }

                    final String error;
                    if (result.getCause() instanceof IOException) {
                        error = mContext.getString(R.string.status_network_error);
                    } else {
                        error = mContext.getString(
                                R.string.message_view_load_attachment_failed_toast,
                                attachment.mName);
                    }
                    mCallback.onLoadMessageError(error);
                }
            }
        }

        private void showAttachmentProgress(long attachmentId, int progress) {
            MessageViewAttachmentInfo attachment = findAttachmentInfo(attachmentId);
            // EAS AttachmentLoader readChunked might do a progress callback after
            // user clicking the "stop" button. We ignore this call back, or the 
            // progress bar will show again at once after hiding
            if (attachment != null && progress > 0 && (!attachment.mIsUserCancelled
                    || AttachmentDownloadService.isAutoDownload(attachmentId))) {
                // This is the case of the auto-downloading, show a disabled "stop"
                // button and the progress bar in this case
                if (attachment.mLoadButton.isShown() || attachment.mInfoButton.isShown()) {
                    attachment.mLoadButton.setVisibility(View.GONE);
                    attachment.mInfoButton.setVisibility(View.GONE);
                    attachment.mCancelButton.setEnabled(false);
                    attachment.mCancelButton.setVisibility(View.VISIBLE);
                }

                attachment.showProgress(progress);
            }
        }
    }

    /**
     * Class to detect update on the current message (e.g. toggle star).  When it gets content
     * change notifications, it kicks {@link ReloadMessageTask}.
     */
    private class MessageObserver extends ContentObserver implements Runnable {
        private final Throttle mThrottle;
        private final ContentResolver mContentResolver;

        private boolean mRegistered;

        public MessageObserver(Handler handler, Context context) {
            super(handler);
            mContentResolver = context.getContentResolver();
            mThrottle = new Throttle("MessageObserver", this, handler);
        }

        public void unregister() {
            if (!mRegistered) {
                return;
            }
            mThrottle.cancelScheduledCallback();
            mContentResolver.unregisterContentObserver(this);
            mRegistered = false;
        }

        public void register(Uri notifyUri) {
            unregister();
            mContentResolver.registerContentObserver(notifyUri, true, this);
            mRegistered = true;
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            if (mRegistered) {
                mThrottle.onEvent();
            }
        }

        /** This method is delay-called by {@link Throttle} on the UI thread. */
        @Override
        public void run() {
            // This method is delay-called, so need to make sure if it's still registered.
            if (mRegistered) {
                new ReloadMessageTask().cancelPreviousAndExecuteParallel();
            }
        }
    }

    private void updatePreviewIcon(MessageViewAttachmentInfo attachmentInfo) {
        new UpdatePreviewIconTask(attachmentInfo).executeParallel();
    }

    private class UpdatePreviewIconTask extends EmailAsyncTask<Void, Void, Bitmap> {
        @SuppressWarnings("hiding")
        private final Context mContext;
        private final MessageViewAttachmentInfo mAttachmentInfo;

        public UpdatePreviewIconTask(MessageViewAttachmentInfo attachmentInfo) {
            super(mTaskTracker);
            mContext = getActivity();
            mAttachmentInfo = attachmentInfo;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            EmailAsyncTask.printStartLog("UpdatePreviewIconTask#doInBackground");
            Bitmap bitmap = getPreviewIcon(mContext, mAttachmentInfo);
            EmailAsyncTask.printStopLog("UpdatePreviewIconTask#doInBackground");
            return bitmap;
        }

        @Override
        protected void onSuccess(Bitmap result) {
            if (result == null) {
                return;
            }
            mAttachmentInfo.mIconView.setImageBitmap(result);
        }
    }

    private boolean shouldShowImagesFor(String senderEmail) {
        return Preferences.getPreferences(getActivity()).shouldShowImagesFor(senderEmail);
    }

    private void setShowImagesForSender() {
        makeVisible(UiUtilities.getView(getView(), R.id.always_show_pictures_button), false);
        Utility.showToast(getActivity(), R.string.message_view_always_show_pictures_confirmation);

        // Force redraw of the container.
        updateTabs(mTabFlags);

        Address[] fromList = Address.unpack(mMessage.mFrom);
        Preferences prefs = Preferences.getPreferences(getActivity());
        for (Address sender : fromList) {
            String email = sender.getAddress();
            prefs.setSenderAsTrusted(email);
        }
    }

    public boolean isMessageLoadedForTest() {
        return mIsMessageLoadedForTest;
    }

    public void clearIsMessageLoadedForTest() {
        mIsMessageLoadedForTest = true;
    }

    /**
     * Start an asynctask to load attachment in background,separated from UI thread.
     * It will improve the performance when the mail contains many attachments.
     * 
     * @param attachList attachment list from DB.
     */
    private void addAttachments(List<Attachment> attachList){
        final List<Attachment> attachs =attachList ;
        if (null == attachList || attachList.size() == 0) {
            return;
        }
        if (mRenderAttachmentTask != null) {
            mRenderAttachmentTask.cancel(true);
            mRenderAttachmentTask = null;
        }
        /**
         * M: We should make this AsyncTask execute parallel, cause this task only create and add
         * views according to attachList, otherwise, the UI update will delay when you have some
         * other very expensive tasks running on the background in serial way @{
         */
        mRenderAttachmentTask =  new AsyncTask<Void,Object[],Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Logging.d(TAG, "AddAttachmentsTask#doInBackground start working");
                for (Attachment attachment : attachs) {
                    // update attachment thumbnail at the same time.
                    View view = createAttachmentView(attachment, true);
                    if (null != view) {
                        publishProgress(new Object[] { view });
                    } else {
                        Logging.d(TAG, "Render attachment failed : " + attachment.toString());
                    }
                }
                Logging.d(TAG, "AddAttachmentsTask#doInBackground stop working");
                return null;
            }

            @Override
            protected void onProgressUpdate(Object[]... progress) {
                 super.onProgressUpdate(progress);
                 View attachment = (View) progress[0][0];
                 mAttachments.addView(attachment);
                 mAttachments.setVisibility(View.VISIBLE);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        /** @} */
    }

    /**
     * Copy data from a cursor-refreshed attachment into the UI.
     * It will return a MessageViewAttachmentInfo view which contains
     * status buttons and thumbnail.
     * 
     * @param attachment A single attachment loaded from the provider
     * @param isUpdateIcon update the attachment icon or not.
     */
    private View createAttachmentView(Attachment attachment , boolean isUpdateIcon) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.message_view_attachment, null);

        TextView attachmentName = (TextView) UiUtilities.getView(view, R.id.attachment_name);
        TextView attachmentInfoView = (TextView) UiUtilities.getView(view, R.id.attachment_info);
        ImageView attachmentIcon = (ImageView) UiUtilities.getView(view, R.id.attachment_icon);
        Button openButton = (Button) UiUtilities.getView(view, R.id.open);
        Button saveButton = (Button) UiUtilities.getView(view, R.id.save);
        Button loadButton = (Button) UiUtilities.getView(view, R.id.load);
        Button infoButton = (Button) UiUtilities.getView(view, R.id.info);
        Button cancelButton = (Button) UiUtilities.getView(view, R.id.cancel);
        ProgressBar attachmentProgress = (ProgressBar) UiUtilities.getView(view, R.id.progress);

        MessageViewAttachmentInfo attachmentInfo = new MessageViewAttachmentInfo(
                mContext, attachment, attachmentProgress);

        // Check whether the attachment already exists or the content uri is for a
        // local file regardless of whether this file is still existed
        if ((attachment != null && !TextUtils.isEmpty(attachment.mContentUri) 
                && !attachment.mContentUri.contains(AttachmentUtilities.AUTHORITY))
                    || Utility.attachmentExists(mContext, attachment)) {
            attachmentInfo.mLoaded = true;
        }

        attachmentInfo.mOpenButton = openButton;
        attachmentInfo.mSaveButton = saveButton;
        attachmentInfo.mLoadButton = loadButton;
        attachmentInfo.mInfoButton = infoButton;
        attachmentInfo.mCancelButton = cancelButton;
        attachmentInfo.mIconView = attachmentIcon;

        updateAttachmentButtons(attachmentInfo ,isUpdateIcon);

        view.setTag(attachmentInfo);
        openButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);
        loadButton.setOnClickListener(this);
        infoButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);

        /// M: Add (msg) suffix for all of message/rfc822 attachment to avoid confuse
        attachmentName.setText(MimeUtility.generateAttName(attachmentInfo.mName,
                attachment.mMimeType));
        attachmentInfoView.setText(UiUtilities.formatSize(mContext, attachmentInfo.mSize));
        return view;
    }

    private synchronized void doLinkParse(String text) {
        /** M: Adding link to WEBURL which text contained */
        if(TextUtils.isEmpty(text)) {
            Logging.d(TAG, "text is null!");
            return ;
        }

        if (mLinkParserTask != null) {
            mLinkParserTask.cancel(true);
            mLinkParserTask.stopWebView();
            Logging.d(TAG, "doLinkParse canceled pre LinkParseTask");
        }
        mLinkParserTask = new LinkParserTask(mMessageContentView);
        mLinkParserTask.execute(new String[] {
            text.trim()
        });
    }

    public boolean isMessageLoading() {
        return mMessageLoading;
    }

    /**
     * M: Deal with the attachment which has been downloaded
     * @param attachmentId the id of downloaded attachment
     */
    private void attachmentDownloaded(long attachmentId) {
        doFinishLoadAttachment(attachmentId);

        Attachment attachment = Attachment.restoreAttachmentWithId(mContext, attachmentId);
        /** M: Check the attachment exist or not @{ */
        if (attachment == null) {
            Logging.d(Logging.LOG_TAG, "Attachment has been delete.");
            return;
        }
        /** @}*/
        View attView = findAttachmentView(attachmentId);
        /// M: if attachment's mime type isn't image, we also need update the attachment view
        if (attachment.mContentId == null
                || !isInlineAttachment(attachment, mMessage, mHtmlTextWebView)) {
            if (attView != null) {
                final MessageViewAttachmentInfo attachmentInfo =
                        (MessageViewAttachmentInfo) attView.getTag();
                updatePreviewIcon(attachmentInfo);
            }
        } else {
            /// M: Remove attachment view if it's a inline attachment
            mAttachments.removeView(attView);
            Logging.d("Reloading UI for inline attachment loaded");
            mHtmlTextWebView = AttachmentUtilities.refactorHtmlTextRaw(mHtmlTextWebView,
                    attachment);
            if (mMessageContentView != null) {
                mMessageContentView.loadDataWithBaseURL("email://", mHtmlTextWebView,
                        "text/html", "utf-8", null);
                doLinkParse(mHtmlTextWebView);
            }
        }
    }

    /**
     * M: VIP feature: If the vip information is changed, update all view UI involved this thing.
     */
    public void updateVipInformation(TempAddress singleAddress) {
        MessageViewUtils.updateVipInformation(mContext, mMessage, mDetailsExpanded, mAddressesView,
                singleAddress);
    }

    /**
     * M: VIP feature: After user modified the vip state of one email address in VipTextView,
     * if the email address is same with the sender of this message, need to
     * update the vip icon of this sender.
     *
     * @param changedAddress the {@link Address} which vip state is changed.
     * @return
     */
    public boolean needUpdateVipIcon(Address changedAddress) {
        if (changedAddress == null) {
            return false;
        }
        Address address = Address.unpackFirst(mMessage.mFrom);
        if (address == null) {
            return false;
        }
        String emailAddress = changedAddress.getAddress();
        if (emailAddress != null &&  emailAddress.equals(address.getAddress())) {
            return true;
        }
        return false;
    }

    /**
     * M: VIP feature: Update the sender vip icon on the sub header.
     * @param isVip Whether this sender is vip now.
     */
    public void updateSenderVipIcon(boolean isVip) {
        Resources res = mContext.getResources();
        if (isVip) {
            mFromNameView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_email_vip_holo_dark, 0, 0, 0);
            mVipSwitcher.setImageDrawable(res.getDrawable(R.drawable.ic_email_remove_vip_holo_dark));
            mVipSwitcher.setContentDescription(
                    mContext.getResources().getString(R.string.vip_remove_title));
        } else {
            mFromNameView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            mVipSwitcher.setImageDrawable(res.getDrawable(R.drawable.ic_email_add_vip_holo_dark));
            mVipSwitcher.setContentDescription(
                    mContext.getResources().getString(R.string.vip_choose_contact));
        }
    }

    /// M: Support testcase. @{
    public MessageViewAttachmentInfo getAttachmentInfo(long attachmentId) {
        return findAttachmentInfo(attachmentId);
    }

    public LinearLayout getAttachmentsView() {
        return mAttachments;
    }

    public void injectMockController(Controller mockController) {
        mController = mockController;
        mController.addResultCallback(mControllerCallback);
    }

    /**
     * Create a URI to mock download attachment from Server.
     * It only run in testcase model {Configuration.mRunTestcase is true}.
     */
    private Uri createMockAttachment(String fileName) {
        File directory = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        directory.mkdirs();
        File file = new File(directory, fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
                FileWriter fw = new FileWriter(file);
                fw.write("MessageComposeTest This is a attachment for email testcase");
                fw.flush();
                fw.close();
            } catch (IOException e) {
                // ignore
            }
        }
        return Uri.fromFile(file);
    }
    /// @}

    /**
     * M: Check if the attachment is an inline attachment. Normally, a good way of judging whether
     * an attachment is inline is that the contentId is not null, but the MMS email obeys this rule,
     * so we use the method Utility.isMaybeAttchmentInline() to verify it, but in case the
     * MMS email is partial download and contains some attachments, the below if-else block is powerless.
     * TO-DO: Find a better way to do this job.
     * @param attachment the attachment
     * @param message use to check message is loaded completely or not.
     * @param htmlText using to check attachment was really inline.
     * @return true if were inline attachment, false else.
     */
    private boolean isInlineAttachment(Attachment attachment, Message message, String htmlText) {
        // If message was loaded complete, we using a more strict method to check it.
        // Otherwise, we follow the original ways.
        if (message != null && message.mFlagLoaded == Message.FLAG_LOADED_COMPLETE) {
            if (htmlText != null && Utility.isAttchmentInlineStrictly(htmlText, attachment)) {
                return true;
            }
        } else {
            if (htmlText != null && attachment.mContentId != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * M: The action of clicking the vip switcher button
     */
    private void onVipSwitcherClick() {
        if (!isMessageOpen()) {
            return;
        }
        Message message = getMessage();
        Address address = Address.unpackFirst(message.mFrom);
        if (address == null) {
            return;
        }
        if (VipMemberCache.isVIP(message.mFrom)) {
            removeVip(address);
        } else {
            addVip(address);
        }
    }

    /**
     * M: Remove the address from vips
     * @param address  the address that contains email address and display name
     */
    private void removeVip(Address address) {
        mVipSwitcher.setClickable(false);
        new EmailAsyncTask<Address, Void, Boolean>(null) {

            @Override
            protected Boolean doInBackground(Address... addresses) {
                if (addresses == null || addresses.length == 0) {
                    return false;
                }
                boolean result = VipMember.deleteVipMembers(mContext,
                        Account.ACCOUNT_ID_COMBINED_VIEW,
                        addresses[0].getAddress()) >= 0;
                VipMemberCache.updateVipMemberCache();
                return result;
            }

            @Override
            protected void onSuccess(Boolean result) {
                if (result) {
                    updateVipIcon(false);
                    updateVipInformation(null);
                }
                mVipSwitcher.setClickable(true);
            };

        }.executeParallel(address);
    }

    /**
     * M: Add the address as VIP
     * @param address the address that contains email address and display name
     */
    private void addVip(Address address) {
        mVipSwitcher.setClickable(false);
        new EmailAsyncTask<Address, Void, Boolean>(null) {
            private boolean mIsDuplicateVip = false;

            @Override
            protected Boolean doInBackground(Address... addresses) {
                boolean result = VipMember.addVIP(mContext, addresses[0],
                        new VipMember.AddVipsCallback() {
                    @Override
                    public void tryToAddDuplicateVip() {
                        Utility.showToast(mContext, R.string.not_add_duplicate_vip);
                        mIsDuplicateVip = true;
                    }
                    @Override
                    public void addVipOverMax() {
                        Utility.showToast(mContext, R.string.can_not_add_vip_over_99);
                    }
                });
                VipMemberCache.updateVipMemberCache();
                return result;
            }

            @Override
            protected void onSuccess(Boolean result) {
                updateVipIcon(result || mIsDuplicateVip);
                if (result) {
                    updateVipInformation(null);
                }
                mVipSwitcher.setClickable(true);
            };

        }.executeParallel(address);
    }

    /**
     * M: Update the vip icon on the sub action bar
     * @param isVip is the VIP sender
     */
    private void updateVipIcon(boolean isVip) {
        if (isVip) {
            mFromNameView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_email_vip_holo_dark, 0, 0, 0);
            mVipSwitcher.setImageDrawable(mRemoveVipIcon);
            mVipSwitcher.setContentDescription(
                    mContext.getResources().getString(R.string.vip_remove_title));
        } else {
            mFromNameView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            mVipSwitcher.setImageDrawable(mAddVipIcon);
            mVipSwitcher.setContentDescription(
                    mContext.getResources().getString(R.string.vip_choose_contact));
        }
    }
}
