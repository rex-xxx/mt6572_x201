/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.android.email.R;
import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.service.SearchParams;
import com.android.emailcommon.utility.DelayedOperations;
import com.android.emailcommon.utility.Utility;

import java.util.ArrayList;

/**
 * Manages the account name and the custom view part on the action bar.
 */
public class ActionBarController {
    private static final String BUNDLE_KEY_MODE = "ActionBarController.BUNDLE_KEY_MODE";
    private static final String BUNDLE_KEY_FILTER = "ActionBarController.FILTER_STRING";
    private static final String BUNDLE_SEARCH_HINT = "ActionBarController.BUNDLE_SEARCH_HINT";

    /**
     * Constants for {@link #mSearchMode}.
     *
     * In {@link #MODE_NORMAL} mode, we don't show the search box.
     * In {@link #MODE_LOCAL_SEARCH} mode, we do show the search box.
     * In {@link #MODE_REMOTE_SEARCH} mode, we don't show the search box, 
     * but show a custom search title.
     * The action bar doesn't really care if the activity is showing search results.
     * If the activity is showing search results, and the {@link Callback#onSearchExit} is called,
     * the activity probably wants to close itself, but this class doesn't make the desision.
     */
    private static final int MODE_NORMAL = 0;
    private static final int MODE_LOCAL_SEARCH = 1;
    private static final int MODE_REMOTE_SEARCH = 2;
    
    private static final int LOADER_ID_ACCOUNT_LIST
            = EmailActivity.ACTION_BAR_CONTROLLER_LOADER_ID_BASE + 0;

    private static final int LOADER_ID_ACCOUNT_TITLE
    = EmailActivity.ACTION_BAR_CONTROLLER_LOADER_ID_BASE + 1;
 
    private static final int DISPLAY_TITLE_MULTIPLE_LINES = 0x20;
    private static final int ACTION_BAR_MASK =
            ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_CUSTOM
                    | ActionBar.DISPLAY_SHOW_TITLE | DISPLAY_TITLE_MULTIPLE_LINES;
    private static final int CUSTOM_ACTION_BAR_OPTIONS =
            ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_CUSTOM;

    private final Context mContext;
    private final LoaderManager mLoaderManager;
    private final ActionBar mActionBar;
    private final DelayedOperations mDelayedOperations;

    /** "Folders" label shown with account name on 1-pane mailbox list */
    private final String mAllFoldersLabel;

    private final ViewGroup mActionBarCustomView;
    private final ViewGroup mAccountSpinnerContainer;
    private final View mAccountSpinner;
    private final Drawable mAccountSpinnerDefaultBackground;
    private final TextView mAccountSpinnerLine1View;
    private final TextView mAccountSpinnerLine2View;
    private final TextView mAccountSpinnerCountView;
    /// M: Image view for indicating smart push sync interval
    private final ImageView mAccountSyncIntervalView;

    private TextView mSearchResultCountView;

    /** M: Adjust the UI layout and logic to support VIP new feature @{ */
    private final ImageButton mSwitchNewerView;
    private final ImageButton mSwitchOlderView;
    private final LinearLayout mMailSwitcher;
    private final ActionBar.LayoutParams mDefaultLayoutParams;
    /** @} */

    private View mSearchContainer;
    private SearchView mSearchView;
    private View mRemoteSearchContainer;
    private TextView mRemoteSearchTitle;

    private final AccountDropdownPopup mAccountDropdown;

    private final AccountSelectorAdapter mAccountsSelectorAdapter;

    private AccountSelectorAdapter.CursorWithExtras mCursor;

    /** The current account ID; used to determine if the account has changed. */
    private long mLastAccountIdForDirtyCheck = Account.NO_ACCOUNT;

    /** The current mailbox ID; used to determine if the mailbox has changed. */
    private long mLastMailboxIdForDirtyCheck = Mailbox.NO_MAILBOX;

    /** Either {@link #MODE_NORMAL} or {@link #MODE_LOCAL_SEARCH}. */
    private int mSearchMode = MODE_NORMAL;

    /** The current title mode, which should be one of {@code Callback TITLE_MODE_*} */
    private int mTitleMode;

    public final Callback mCallback;

    private boolean mIsTitleUpdated = false;
    
    /// M: Should clear the focus on search view after backing from MessageView
    private boolean mShouldClearSearchFocus = false;
    
    /** M: The screen mode was transit from portrait to landscape 
        and then backing from MessageView, need to trigger
        a re-filter */
    private boolean mBackFromMessageView = false;

    public interface SearchContext {
        public long getTargetMailboxId();
    }

    private static final int TITLE_MODE_SPINNER_ENABLED = 0x10;

    /** M: support for message local search UI @{ */
    private static final String BUNDLE_KEY_ACTION_BAR_SELECTED_FIELD 
            = "ActionBarController.ACTION_BAR_SELECTED_TAB";
    private TabListener mTabListener = new TabListener();
    private String mFilter;
    private boolean mTabChanged = false;
    private String mActionBarSelectedTab;
    private String mRestoredSelectedTab;
    // Because onSaveInstanceState() would not be called when pressing
    // "BACK" key from EmailActivity to HomeScreen, thus use this variable
    // to save and restore the selected tab in this scenario
    private static String sActionBarSelectedTab;
    private ArrayList<String> mFieldList = new ArrayList<String>();
    private static final int INDEX_SENDER = 1;
    /** @} */

    public static final String TAG = "ActionBarController";

    public interface Callback {
        /** Values for {@link #getTitleMode}.  Show only account name */
        public static final int TITLE_MODE_ACCOUNT_NAME_ONLY = 0 | TITLE_MODE_SPINNER_ENABLED;

        /**
         * Show the current account name with "Folders"
         * The account spinner will be disabled in this mode.
         */
        public static final int TITLE_MODE_ACCOUNT_WITH_ALL_FOLDERS_LABEL = 1;

        /**
         * Show the current account name and the current mailbox name.
         */
        public static final int TITLE_MODE_ACCOUNT_WITH_MAILBOX = 2 | TITLE_MODE_SPINNER_ENABLED;
        /**
         * Show the current message subject.  Actual subject is obtained via
         * {@link #getMessageSubject()}.
         *
         * The account spinner will be disabled in this mode.
         */
        public static final int TITLE_MODE_MESSAGE_SUBJECT = 3;

        /** @return true if an account is selected. */
        public boolean isAccountSelected();

        /**
         * @return currently selected account ID, {@link Account#ACCOUNT_ID_COMBINED_VIEW},
         * or -1 if no account is selected.
         */
        public long getUIAccountId();

        /**
         * @return currently selected mailbox ID, or {@link Mailbox#NO_MAILBOX} if no mailbox is
         * selected.
         */
        public long getMailboxId();

        /**
         * @return constants such as {@link #TITLE_MODE_ACCOUNT_NAME_ONLY}.
         */
        public int getTitleMode();

        /** @see #TITLE_MODE_MESSAGE_SUBJECT */
        public String getMessageSubject();

        /** @return the "UP" arrow should be shown. */
        public boolean shouldShowUp();

        /**
         * Called when an account is selected on the account spinner.
         * @param accountId ID of the selected account, or {@link Account#ACCOUNT_ID_COMBINED_VIEW}.
         */
        public void onAccountSelected(long accountId);

        /**
         * Invoked when a recent mailbox is selected on the account spinner.
         *
         * @param accountId ID of the selected account, or {@link Account#ACCOUNT_ID_COMBINED_VIEW}.
         * @param mailboxId The ID of the selected mailbox, or {@link Mailbox#NO_MAILBOX} if the
         *          special option "show all mailboxes" was selected.
         */
        public void onMailboxSelected(long accountId, long mailboxId);

        /** Called when no accounts are found in the database. */
        public void onNoAccountsFound();

        /**
         * Retrieves the hint text to be shown for when a search entry is being made.
         */
        public String getSearchHint();

        /**
         * Called when the action bar initially shows the search entry field.
         */
        public void onSearchStarted();

        /**
         * Called when a search is submitted.
         *
         * @param queryTerm query string
         * @param queryField the field to query
         */
        public void onSearchSubmit(String queryTerm, String queryField);
        
        public void onLocalSearchSubmit(String queryTerm, String queryField);

        /**
         * Called when the search box is closed.
         */
        public void onSearchExit();

        /**
         * M: Switch mail to the newer/older when newer/older button be clicked
         * @param toNewer to the newer if true, else to the older
         */
        public void switchMail(boolean toNewer);

        public void onUpPressed();
    }

    public ActionBarController(Context context, LoaderManager loaderManager,
            ActionBar actionBar, Callback callback) {
        mContext = context;
        mLoaderManager = loaderManager;
        mActionBar = actionBar;
        mCallback = callback;
        mDelayedOperations = new DelayedOperations(Utility.getMainThreadHandler());
        mAllFoldersLabel = mContext.getResources().getString(
                R.string.action_bar_mailbox_list_title);
        mAccountsSelectorAdapter = new AccountSelectorAdapter(mContext);

        // Configure action bar.
        enterCustomActionBarMode();

        // Prepare the custom view
        mActionBar.setCustomView(R.layout.action_bar_custom_view);
        mActionBarCustomView = (ViewGroup) mActionBar.getCustomView();
        /// M: cache layout parameters.
        mDefaultLayoutParams = new ActionBar.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        // Account spinner
        mAccountSpinnerContainer =
                UiUtilities.getView(mActionBarCustomView, R.id.account_spinner_container);
        mAccountSpinner = UiUtilities.getView(mActionBarCustomView, R.id.account_spinner);
        mAccountSpinnerDefaultBackground = mAccountSpinner.getBackground();

        mAccountSpinnerLine1View = UiUtilities.getView(mActionBarCustomView, R.id.spinner_line_1);
        mAccountSpinnerLine2View = UiUtilities.getView(mActionBarCustomView, R.id.spinner_line_2);
        mAccountSpinnerCountView = UiUtilities.getView(mActionBarCustomView, R.id.spinner_count);
        mAccountSyncIntervalView = UiUtilities.getView(mActionBarCustomView, R.id.sync_interval);

        /** M: Adjust the UI layout and logic to support VIP new feature @{ */
        mMailSwitcher = UiUtilities.getViewOrNull(mActionBarCustomView, R.id.mail_switcher);
        mSwitchNewerView = UiUtilities.getViewOrNull(mActionBarCustomView, R.id.switch_newer);
        mSwitchOlderView = UiUtilities.getViewOrNull(mActionBarCustomView, R.id.switch_older);
        if (mSwitchNewerView != null) {
            mSwitchNewerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCallback.switchMail(true);
                }
            });
        }
        if (mSwitchOlderView != null) {
            mSwitchOlderView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCallback.switchMail(false);
                }
            });
        }
        /** @} */

        // Account dropdown
        mAccountDropdown = new AccountDropdownPopup(mContext);
        mAccountDropdown.setAdapter(mAccountsSelectorAdapter);

        mAccountSpinner.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (mAccountsSelectorAdapter.getCount() > 0) {
                    mAccountDropdown.show();
                }
            }
        });

        // pick a sane default. later enabled in updateTitle().
        mActionBarCustomView.setClickable(false);
    }

    /**
     * M: use dropdownlist-styled UI for local search if returned true,
     *    tab-styled UI otherwise
     */
    private boolean useListMode() {
        int orientation = mContext.getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }
    
    private void initSearchFieldsUI() {
        mSearchResultCountView = UiUtilities.getView(mSearchContainer, R.id.result_count);
        mSearchResultCountView.setText("0");

        if (useListMode()) {
            initSpinner();
        } else {
            initTabs();
        }
    }
    
    /**
     * M: initialize the tab-styled local search UI
     */
    private void initTabs() {
        Tab tab = mActionBar
                .newTab()
                .setText(R.string.search_field_all)
                .setTabListener(mTabListener);
        tab.setTag(SearchParams.SEARCH_FIELD_ALL);
        // select the "All" tab as default
        mActionBar.addTab(tab, true);
        tab = mActionBar
                .newTab()
                .setText(R.string.search_field_sender)
                .setTabListener(mTabListener);
        tab.setTag(SearchParams.SEARCH_FIELD_FROM);
        mActionBar.addTab(tab);
        tab = mActionBar
                .newTab()
                .setText(R.string.search_field_receiver)
                .setTabListener(mTabListener);
        tab.setTag(SearchParams.SEARCH_FIELD_TO);
        mActionBar.addTab(tab);
        tab = mActionBar
                .newTab()
                .setText(R.string.search_field_subject)
                .setTabListener(mTabListener);
        tab.setTag(SearchParams.SEARCH_FIELD_SUBJECT);
        mActionBar.addTab(tab);
        tab = mActionBar
                .newTab()
                .setText(R.string.search_field_body)
                .setTabListener(mTabListener);
        tab.setTag(SearchParams.SEARCH_FIELD_BODY);
        mActionBar.addTab(tab);

        String selectedTab;
        if (!TextUtils.isEmpty(mRestoredSelectedTab)) {
            selectedTab = mRestoredSelectedTab;
        } else if (!TextUtils.isEmpty(sActionBarSelectedTab)){
            selectedTab = sActionBarSelectedTab;
        } else {
            // Default set Item "Sender" selected
            mActionBar.selectTab(mActionBar.getTabAt(INDEX_SENDER));
            return;
        }

        int tabCount = mActionBar.getTabCount();
        for (int i = 0; i < tabCount; i++) {
            String tabTag = (String)mActionBar.getTabAt(i).getTag();
            if (tabTag != null && tabTag.equalsIgnoreCase(selectedTab)) {
                mActionBar.selectTab(mActionBar.getTabAt(i));
                break;
            }
        }
    }

    class DropDownListener implements OnNavigationListener {
        ArrayList<String> mItems;

        public DropDownListener(ArrayList<String> fields) {
            mItems = fields;
        }
        public boolean onNavigationItemSelected(int itemPosition, long itemId) {
            String text = mFieldList.get(itemPosition);
            mActionBarSelectedTab = text;
            mTabChanged = true;
            mOnQueryText.onQueryTextChange(mSearchView.getQuery().toString());
            mTabChanged = false;
            return true;
        }
    }

    /**
     * M: initialize the dropdownlist-styled local search UI
     */
    private void initSpinner() {
        ArrayList<String> items = new ArrayList<String>();
        Resources resources = mContext.getResources();
        mFieldList.clear();
        items.add(resources.getString(R.string.search_field_all));
        mFieldList.add(SearchParams.SEARCH_FIELD_ALL);
        items.add(resources.getString(R.string.search_field_sender));
        mFieldList.add(SearchParams.SEARCH_FIELD_FROM);
        items.add(resources.getString(R.string.search_field_receiver));
        mFieldList.add(SearchParams.SEARCH_FIELD_TO);
        items.add(resources.getString(R.string.search_field_subject));
        mFieldList.add(SearchParams.SEARCH_FIELD_SUBJECT);
        items.add(resources.getString(R.string.search_field_body));
        mFieldList.add(SearchParams.SEARCH_FIELD_BODY);

        SpinnerAdapter adapter = new ArrayAdapter<String>(mContext,
                android.R.layout.simple_spinner_dropdown_item, items);
        mActionBar.setListNavigationCallbacks(adapter, new DropDownListener(
                items));

        String selectedTab;
        if (!TextUtils.isEmpty(mRestoredSelectedTab)) {
            selectedTab = mRestoredSelectedTab;
        } else if (!TextUtils.isEmpty(sActionBarSelectedTab)) {
            selectedTab = sActionBarSelectedTab;
        } else {
            // Default set Item "Sender" selected
            mActionBar.setSelectedNavigationItem(INDEX_SENDER);
            return;
        }

        int i = 0;
        if (!TextUtils.isEmpty(selectedTab)) {
            for (String item : mFieldList) {
                if (item.equalsIgnoreCase(selectedTab)) {
                    mActionBar.setSelectedNavigationItem(i);
                    break;
                }
                i++;
            }
        }
    }

    private void initSearchViews() {
        if (useListMode()) {
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        } else {
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        }

        if (mSearchContainer == null) {
            final LayoutInflater inflater = LayoutInflater.from(mContext);
            mSearchContainer = inflater.inflate(R.layout.action_bar_search, null);
            mSearchView = UiUtilities.getView(mSearchContainer, R.id.search_view);
            mSearchView.setSubmitButtonEnabled(false);
            mSearchView.setOnQueryTextListener(mOnQueryText);
            mSearchView.onActionViewExpanded();
            mActionBarCustomView.addView(mSearchContainer);

            initSearchFieldsUI();
        }
    }

    /**
     * M: Support remote search view. 
     * Only show a search title and search count from server. 
     */
    private void initRemoteSearchViews() {
        //1)Reset ActionBar type
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        mAccountSpinnerContainer.setVisibility(View.GONE);
        mAccountSpinnerLine1View.setVisibility(View.GONE);
        mAccountSpinnerLine2View.setVisibility(View.GONE);
        mAccountSpinnerCountView.setVisibility(View.GONE);
        mAccountSyncIntervalView.setVisibility(View.GONE);
        /// M: Adjust the UI layout and logic to support VIP new feature
        setMailSwitcherVisible(false);
        //2)Update Title
        if(null == mRemoteSearchContainer){
            final LayoutInflater inflater = LayoutInflater.from(mContext);
            mRemoteSearchContainer = inflater.inflate(R.layout.action_bar_remote_search, null);
            mActionBarCustomView.addView(mRemoteSearchContainer);
            mRemoteSearchTitle = UiUtilities.getView(mRemoteSearchContainer,
                    R.id.remote_search_title);
            mSearchResultCountView = UiUtilities.getView(mRemoteSearchContainer, 
                    R.id.remote_result_count);
            mSearchResultCountView.setText("0");
        }
        mRemoteSearchContainer.setVisibility(View.VISIBLE);
        mRemoteSearchTitle.setVisibility(View.VISIBLE);
        mSearchResultCountView.setVisibility(View.VISIBLE);

    }


    /** Must be called from {@link UIControllerBase#onActivityCreated()} */
    public void onActivityCreated() {
        refresh();
    }

    /** Must be called from {@link UIControllerBase#onActivityDestroy()} */
    public void onActivityDestroy() {
        if (mAccountDropdown.isShowing()) {
            mAccountDropdown.dismiss();
        }
        /// M: take down the user selected tab, then it can be restored
        sActionBarSelectedTab = mActionBarSelectedTab;
    }

    /** Must be called from {@link UIControllerBase#onSaveInstanceState} */
    public void onSaveInstanceState(Bundle outState) {
        mDelayedOperations.removeCallbacks(); // Remove all pending operations
        outState.putInt(BUNDLE_KEY_MODE, mSearchMode);
        outState.putString(BUNDLE_SEARCH_HINT, mCallback.getSearchHint());
        outState.putString(BUNDLE_KEY_ACTION_BAR_SELECTED_FIELD, mActionBarSelectedTab);
        outState.putString(BUNDLE_KEY_FILTER, mFilter);
    }

    /** Must be called from {@link UIControllerBase#onRestoreInstanceState} */
    public void onRestoreInstanceState(Bundle savedState) {
        int mode = savedState.getInt(BUNDLE_KEY_MODE);
        mRestoredSelectedTab = savedState.getString(BUNDLE_KEY_ACTION_BAR_SELECTED_FIELD,
                null);
        mFilter = savedState.getString(BUNDLE_KEY_FILTER, null);
        if (mode == MODE_LOCAL_SEARCH) {
            // No need to re-set the initial query, as the View tree restoration does that
            enterLocalSearchMode(null);
        }
        if (mode == MODE_REMOTE_SEARCH) {
            enterRemoteSearchMode(mFilter);
        }
        if (TextUtils.isEmpty(mCallback.getSearchHint())) {
            setSearchHint(savedState.getString(BUNDLE_SEARCH_HINT));
        }
    }

    /**
     * M: Local search mode
     * @return true if the search box is shown.
     * 
     */
    public boolean isLocalSearchMode() {
        return mSearchMode == MODE_LOCAL_SEARCH;
    }

    /**
     * M: Remote search mode
     * @return true if the current is remote research mode.
     */
    public boolean isRemoteSearchMode() {
        return mSearchMode == MODE_REMOTE_SEARCH;
    }

    /**
     * @return Whether or not the search bar should be shown. This is a function of whether or not a
     *     search is active, and if the current layout supports it.
     */
    private boolean shouldShowSearchBar() {
        return isLocalSearchMode() && (mTitleMode != Callback.TITLE_MODE_MESSAGE_SUBJECT);
    }

    /**
     * M: @return Whether or not the remote search tile should show.
     */
    private boolean shouldShowRemoteSearchTitle() {
        return isRemoteSearchMode() && (mTitleMode != Callback.TITLE_MODE_MESSAGE_SUBJECT);
    }

    /**
     * Show the search box.
     *
     * @param initialQueryTerm if non-empty, set to the search box.
     */
    public void enterLocalSearchMode(String initialQueryTerm) {
        initSearchViews();
        if (isLocalSearchMode()) {
            return;
        }
        mSearchMode = MODE_LOCAL_SEARCH;

        if (!TextUtils.isEmpty(initialQueryTerm)) {
            mSearchView.setQuery(initialQueryTerm, false);
            mShouldClearSearchFocus = true;
        } else {
            mSearchView.setQuery("", false);
        }
        mSearchView.setQueryHint(mCallback.getSearchHint());

        // Focus on the search input box and throw up the IME if specified.
        // TODO: HACK. this is a workaround IME not popping up.
        mSearchView.setIconified(false);

        refresh();
        mCallback.onSearchStarted();
    }

    /**
     * Show the search box.
     *
     * @param initialQueryTerm if non-empty, set to the search box.
     */
    public void enterRemoteSearchMode(String initialQueryTerm) {
        initRemoteSearchViews();
        if(!TextUtils.isEmpty(initialQueryTerm)){
            mRemoteSearchTitle.setText(mContext.getString(R.string.searching_on_server_title, 
                    initialQueryTerm));
            mFilter = initialQueryTerm;
        } else {
            // TODO: What if the initialQueryTerm is null. Define a suitable title string
            Logging.d(TAG, "enterRemoteSearchMode initialQueryTerm is null ");
        }
        mSearchMode = MODE_REMOTE_SEARCH;
    }

    public void setSearchHint(String hint) {
        if (mSearchView != null) {
            mSearchView.setQueryHint(hint);
        }
    }

    public void exitSearchMode() {
        /** M: If we were in remote search mode, exit search mode would destroy
         *  the current activity, So, it is not necessary to refresh the action bar.
         *  Otherwise the action bar would flash before the activity be destroyed. @{ */
        boolean isRemoteSearch = isRemoteSearchMode();
        if (!isLocalSearchMode() && !isRemoteSearch) {
            return;
        }
        mSearchMode = MODE_NORMAL;

        if (!isRemoteSearch) {
            refresh();
        }
        /** @} */
        mCallback.onSearchExit();
    }

    /**
     * Performs the back action.
     *
     * @param isSystemBackKey <code>true</code> if the system back key was pressed.
     * <code>false</code> if it's caused by the "home" icon click on the action bar.
     */
    public boolean onBackPressed(boolean isSystemBackKey) {
        if (shouldShowSearchBar()) {
            exitSearchMode();
            return true;
        }
        return false;
    }

    /** Refreshes the action bar display. */
    public void refresh() {
        // The actual work is in refreshInernal(), but we don't call it directly here, because:
        // 1. refresh() is called very often.
        // 2. to avoid nested fragment transaction.
        //    refresh is often called during a fragment transaction, but updateTitle() may call
        //    a callback which would initiate another fragment transaction.
        mDelayedOperations.removeCallbacks(mRefreshRunnable);
        mDelayedOperations.post(mRefreshRunnable);
    }

    private final Runnable mRefreshRunnable = new Runnable() {
        @Override public void run() {
            refreshInernal();
        }
    };
    private void refreshInernal() {
        final boolean showUp = isLocalSearchMode() || mCallback.shouldShowUp();
        mActionBar.setDisplayOptions(showUp
                ? ActionBar.DISPLAY_HOME_AS_UP : 0, ActionBar.DISPLAY_HOME_AS_UP);
        mActionBar.setHomeButtonEnabled(showUp);

        final long accountId = mCallback.getUIAccountId();
        final long mailboxId = mCallback.getMailboxId();
        if ((mLastAccountIdForDirtyCheck != accountId)
                || (mLastMailboxIdForDirtyCheck != mailboxId)) {
            mLastAccountIdForDirtyCheck = accountId;
            mLastMailboxIdForDirtyCheck = mailboxId;

            if (accountId != Account.NO_ACCOUNT) {
                loadAccountMailboxInfo(accountId, mailboxId);
            }
        }

        updateTitle();
    }

    /**
     * Load account/mailbox info, and account/recent mailbox list.
     */
    private void loadAccountMailboxInfo(final long accountId, final long mailboxId) {
        mIsTitleUpdated = false;
        // To display title more faster,lookup title info first.
        mLoaderManager.restartLoader(LOADER_ID_ACCOUNT_TITLE, null,
                new LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                return AccountSelectorAdapter.createLoader(mContext, accountId, mailboxId, true);
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                if (!mIsTitleUpdated) {
                    mCursor = (AccountSelectorAdapter.CursorWithExtras) data;
                    updateTitle();
                }
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                mCursor = null;
                updateTitle();
            }
        });
        // lookup completely info
        mLoaderManager.restartLoader(LOADER_ID_ACCOUNT_LIST, null,
                new LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                return AccountSelectorAdapter.createLoader(mContext, accountId, mailboxId);
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                mIsTitleUpdated = true;
                mCursor = (AccountSelectorAdapter.CursorWithExtras) data;
                updateTitle();
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                mCursor = null;
                updateTitle();
            }
        });
    }

    /**
     * Update the "title" part.
     */
    private void updateTitle() {
        mAccountsSelectorAdapter.swapCursor(mCursor);

        enterCustomActionBarMode();
        if (mCursor == null) {
            // Initial load not finished.
            mActionBarCustomView.setVisibility(View.GONE);
            return;
        }
        mActionBarCustomView.setVisibility(View.VISIBLE);
        /// M: recover to default UI layout.
        mActionBar.setCustomView(mActionBarCustomView, mDefaultLayoutParams);

        if (mCursor.getAccountCount() == 0 && !mCursor.getLookUpTitleOnly()) {
            mCallback.onNoAccountsFound();
            return;
        }

        if ((mCursor.getAccountId() != Account.NO_ACCOUNT) && !mCursor.accountExists()) {
            // Account specified, but does not exist.
            if (isLocalSearchMode()) {
                exitSearchMode();
            }

            long accountId = Account.getDefaultAccountId(mContext);

            if (accountId != Account.NO_ACCOUNT) {
                // Switch to the default account.
                mCallback.onAccountSelected(accountId);
            }
            return;
        }

        mTitleMode = mCallback.getTitleMode();

        /*
         * M: For remote search mode, only show search title "Search 'xxx' on server" and
         * search result. Ignore the case of open a mail in the remote search result
         * @{
         */
        if(shouldShowRemoteSearchTitle()){
            initRemoteSearchViews();
            return;
        }
        /* @}*/

        if (shouldShowSearchBar()) {
            initSearchViews();
            // In search mode, the search box is a replacement of the account spinner, so ignore
            // the work needed to update that. It will get updated when it goes visible again.
            mAccountSpinnerContainer.setVisibility(View.GONE);
            mSearchContainer.setVisibility(View.VISIBLE);
            if (mShouldClearSearchFocus) {
                mSearchView.clearFocus();
                mShouldClearSearchFocus = false;
            }
            /** M: The screen mode was transit from portrait to landscape 
                and then backing from MessageView, need to trigger
                a re-filter since it would not do automatically as in 
                portrait mode @{ */
            if (mBackFromMessageView && useListMode()) {
                mTabChanged = true;
                mOnQueryText.onQueryTextChange(mSearchView.getQuery().toString());
                mTabChanged = false;
            }
            mBackFromMessageView = false;
            /** @} */
            return;
        } else {
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        }

        // Account spinner visible.
        mAccountSpinnerContainer.setVisibility(View.VISIBLE);
        UiUtilities.setVisibilitySafe(mSearchContainer, View.GONE);
        UiUtilities.setVisibilitySafe(mRemoteSearchContainer, View.GONE);

        if (mTitleMode == Callback.TITLE_MODE_MESSAGE_SUBJECT) {
            /** M: Adjust the UI layout and logic to support VIP new feature. Use the custom view
             *  to show the subject. Because the title mode may conflict with the mail switcher
             *  which on the custom view. @{ */
            mAccountSpinnerLine1View.setVisibility(View.GONE);
            mAccountSpinnerLine2View.setVisibility(View.GONE);

            mAccountSpinnerCountView.setVisibility(View.GONE);
            mAccountSyncIntervalView.setVisibility(View.GONE);
            setMailSwitcherVisible(true);
            if (isLocalSearchMode()) {
                mBackFromMessageView = true;
                mShouldClearSearchFocus = true;
            }
            /// M: Use two line title and custom view action bar mode @{
            enterMultiLineTitleCustomActionBarMode();
            int customViewWidth;
            if (mMailSwitcher != null) {
                int widthSpec = MeasureSpec.makeMeasureSpec(mActionBarCustomView.getWidth(), MeasureSpec.AT_MOST);
                mMailSwitcher.measure(widthSpec, 0);
                customViewWidth = mMailSwitcher.getMeasuredWidth();
            } else {
                customViewWidth = 0;
            }
            ActionBar.LayoutParams layout = new ActionBar.LayoutParams(customViewWidth,
                    LayoutParams.MATCH_PARENT, android.view.Gravity.RIGHT);
            mActionBar.setCustomView(mActionBarCustomView, layout);
            /** @} */
            String subject = mCallback.getMessageSubject();
            if (subject == null) {
                subject = "";
            }
            final SpannableString title = new SpannableString(subject);
            final SpannableStringBuilder builder = new SpannableStringBuilder();
            title.setSpan(new TextAppearanceSpan(mContext, R.style.subject_action_bar_title_text),
                    0, subject.length(), 0);
            builder.append(title);
            mActionBar.setTitle(builder);
            mActionBar.setSubtitle(null);
        } else if (mTitleMode == Callback.TITLE_MODE_ACCOUNT_WITH_ALL_FOLDERS_LABEL) {
            enterSingleLineTitleActionBarMode();
            mActionBar.setTitle(mAllFoldersLabel);
            mActionBar.setSubtitle(mCursor.getAccountDisplayName());
        } else {
            // Get mailbox name
            final String mailboxName;
            if (mTitleMode == Callback.TITLE_MODE_ACCOUNT_WITH_MAILBOX) {
                mailboxName = mCursor.getMailboxDisplayName();
            } else {
                mailboxName = null;
            }

            // Note - setSingleLine is needed as well as setMaxLines since they set different
            // flags on the view.
            /** M: The mAccountSpinnerLine1View would show the subject when view mail.
             *  But it be set visible gone for supporting VIP new feature.
             *  So, we need make it back to visible here. @{ */
            mAccountSpinnerLine1View.setVisibility(View.VISIBLE);
            /** @} */
            mAccountSpinnerLine1View.setSingleLine();
            mAccountSpinnerLine1View.setMaxLines(1);
            if (TextUtils.isEmpty(mailboxName)) {
                mAccountSpinnerLine1View.setText(mCursor.getAccountDisplayName());

                // Change the visibility of line 2, so line 1 will be vertically-centered.
                mAccountSpinnerLine2View.setVisibility(View.GONE);
            } else {
                mAccountSpinnerLine1View.setText(mailboxName);
                mAccountSpinnerLine2View.setVisibility(View.VISIBLE);
                mAccountSpinnerLine2View.setText(mCursor.getAccountDisplayName());
            }

            /** M: In eng mode, if the mailbox is the inbox of a smart push account,
                   show the image view which indicate the current sync interval @{ */
            mAccountSyncIntervalView.setVisibility(View.GONE);
            long accountId = mCursor.getAccountId();
            long mailboxId = mCursor.getMailboxId();
            if (Account.isNormalAccount(accountId) && mailboxId > 0L) {
                Account acct = Account.restoreAccountWithId(mContext, accountId);
                Mailbox mailbox = Mailbox.restoreMailboxWithId(mContext, mailboxId);
                if (Build.TYPE.equals("eng") && acct != null && acct.mSyncInterval == Account.CHECK_INTERVAL_SMART_PUSH
                        && mailbox != null && mailbox.mType == Mailbox.TYPE_INBOX) {
                    mAccountSyncIntervalView.setVisibility(View.VISIBLE);
                    switch (mailbox.mSyncInterval) {
                        case Mailbox.CHECK_INTERVAL_PING:
                        case Mailbox.CHECK_INTERVAL_PUSH:
                        case Mailbox.CHECK_INTERVAL_PUSH_HOLD:
                            mAccountSyncIntervalView.setImageResource(R.drawable.jog_tab_target_green);
                            break;
                        case Mailbox.CHECK_INTERVAL_NEVER:
                            mAccountSyncIntervalView.setImageResource(R.drawable.jog_tab_target_red);
                            break;
                        default:
                            mAccountSyncIntervalView.setImageResource(R.drawable.jog_tab_target_yellow);
                    }
                }
            }
            /** @} */

            /// M: Adjust the UI layout and logic to support VIP new feature
            setMailSwitcherVisible(false);
            mAccountSpinnerCountView.setVisibility(View.VISIBLE);
            mAccountSpinnerCountView.setText(UiUtilities.getMessageCountForUi(
                    mContext, mCursor.getMailboxMessageCount(), true));
        }

        boolean spinnerEnabled =
            ((mTitleMode & TITLE_MODE_SPINNER_ENABLED) != 0) && mCursor.shouldEnableSpinner();

        setSpinnerEnabled(spinnerEnabled);
    }

    /**
     * M: Update the visibility of mail switcher
     * @param visible is visible
     */
    private void setMailSwitcherVisible(boolean visible) {
        if (visible) {
            UiUtilities.setVisibilitySafe(mSwitchNewerView, View.VISIBLE);
            UiUtilities.setVisibilitySafe(mSwitchOlderView, View.VISIBLE);
        } else {
            UiUtilities.setVisibilitySafe(mSwitchNewerView, View.GONE);
            UiUtilities.setVisibilitySafe(mSwitchOlderView, View.GONE);
        }
    }

    /**
     * M: Set the newer mail button enable
     * @param enable is enable
     */
    public void setSwitchNewerEnable(boolean enable) {
        if (mSwitchNewerView != null) {
            mSwitchNewerView.setEnabled(enable);
        }
    }

    /**
     * Set the older mail button enable
     * @param enable is enable
     */
    public void setSwitchOlderEnable(boolean enable) {
        if (mSwitchOlderView != null) {
            mSwitchOlderView.setEnabled(enable);
        }
    }

    private void enterCustomActionBarMode() {
        mActionBar.setDisplayOptions(CUSTOM_ACTION_BAR_OPTIONS, ACTION_BAR_MASK);
    }

    /**
     * M: display tile and custom view in MessageView, custom view using for mail switcher.
     */
    private void enterMultiLineTitleCustomActionBarMode() {
        mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE
                | ActionBar.DISPLAY_SHOW_CUSTOM | DISPLAY_TITLE_MULTIPLE_LINES, ACTION_BAR_MASK);
    }

    private void enterSingleLineTitleActionBarMode() {
        mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE,
                ACTION_BAR_MASK);
    }

    private void setSpinnerEnabled(boolean enabled) {
        if (enabled == mAccountSpinner.isEnabled()) {
            return;
        }

        mAccountSpinner.setEnabled(enabled);
        mAccountSpinner.setClickable(enabled);
        mActionBarCustomView.setClickable(!enabled);
        if (enabled) {
            mAccountSpinner.setBackgroundDrawable(mAccountSpinnerDefaultBackground);
        } else {
            mAccountSpinner.setBackgroundDrawable(null);
        }

        // For some reason, changing the background mucks with the padding so we have to manually
        // reset vertical padding here (also specified in XML, but it seems to be ignored for
        // some reason.
        mAccountSpinner.setPadding(
                mAccountSpinner.getPaddingLeft(),
                0,
                mAccountSpinner.getPaddingRight(),
                0);
    }


    private final SearchView.OnQueryTextListener mOnQueryText
            = new SearchView.OnQueryTextListener() {
        private String mOldText = "";
        @Override
        public boolean onQueryTextChange(String newText) {
            // Event not handled.  Let the search do the default action.
            if(isLocalSearchMode() && !TextUtils.isEmpty(mActionBarSelectedTab) 
                    && (!newText.trim().equalsIgnoreCase(mOldText.trim()) || mTabChanged)) {
                mCallback.onLocalSearchSubmit(newText, mActionBarSelectedTab);
                mOldText = newText;
            }
           return true;
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            /** M: Search has started after text changed, so hide the input method
             *  to show the search results while search key be pressed @{*/
            if (mSearchView != null && isLocalSearchMode()) {
                InputMethodManager imm = (InputMethodManager)mContext.
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && imm.isActive()) {
                    imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
                }
                mSearchView.clearFocus();
            }
            /** @}*/
            return false;
        }
    };

    private void onAccountSpinnerItemClicked(int position) {
        if (mAccountsSelectorAdapter == null) { // just in case...
            return;
        }
        final long accountId = mAccountsSelectorAdapter.getAccountId(position);

        if (mAccountsSelectorAdapter.isAccountItem(position)) {
            mCallback.onAccountSelected(accountId);
        } else if (mAccountsSelectorAdapter.isMailboxItem(position)) {
            mCallback.onMailboxSelected(accountId,
                    mAccountsSelectorAdapter.getId(position));
        }
    }

    // Based on Spinner.DropdownPopup
    private class AccountDropdownPopup extends ListPopupWindow {
        public AccountDropdownPopup(Context context) {
            super(context);
            setAnchorView(mAccountSpinner);
            setModal(true);
            setPromptPosition(POSITION_PROMPT_ABOVE);
            setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    onAccountSpinnerItemClicked(position);
                    dismiss();
                }
            });
        }

        @Override
        public void show() {
            setWidth(mContext.getResources().getDimensionPixelSize(
                    R.dimen.account_dropdown_dropdownwidth));
            setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
            super.show();
            // List view is instantiated in super.show(), so we need to do this after...
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
    }

    class TabListener implements ActionBar.TabListener {
        /* The following are each of the ActionBar.TabListener callbacks */
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            mActionBarSelectedTab = (String)tab.getTag();
            mTabChanged = true;
            mOnQueryText.onQueryTextChange(mSearchView.getQuery().toString());
            mTabChanged = false;
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            // User selected the already selected tab. Usually do nothing.
        }
    }

    public void updateSearchCount(int count) {
        mSearchResultCountView.setText(UiUtilities.getMessageCountForUi(
                mContext, count, false /* replaceZeroWithBlank */));
    }

    /**
     * M: get the query term if current search field were "body" or "all",
     *    otherwise returns null
     */
    public String getQueryTermIfSearchBody() {
        String selectedTab;
        if (!TextUtils.isEmpty(mActionBarSelectedTab)) {
            selectedTab = mActionBarSelectedTab;
        } else if (!TextUtils.isEmpty(sActionBarSelectedTab)){
            selectedTab = sActionBarSelectedTab;
        } else {
            return null;
        }

        return (selectedTab.equalsIgnoreCase(SearchParams.SEARCH_FIELD_BODY)
               || selectedTab.equalsIgnoreCase(SearchParams.SEARCH_FIELD_ALL)) ?
               mSearchView.getQuery().toString() : null;
    }
}
