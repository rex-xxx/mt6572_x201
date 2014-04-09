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

package com.android.contacts.activities;

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
//The following lines are provided and maintained by Mediatek Inc.
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
//The previous lines are provided and maintained by Mediatek Inc.
import android.content.Context;
import android.content.Intent;
//The following lines are provided and maintained by Mediatek Inc.
import android.content.IntentFilter;
//The previous lines are provided and maintained by Mediatek Inc.
import android.content.SharedPreferences;
/** M: New Feature Phone Landscape UI @{ */
import android.content.res.Configuration;
/** @ }*/
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents.UI;
//The following lines are provided and maintained by Mediatek Inc.
import android.provider.Settings;
//The previous lines are provided and maintained by Mediatek Inc.
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
//The following lines are provided and maintained by Mediatek Inc.
import android.view.KeyEvent;
//The previous lines are provided and maintained by Mediatek Inc.

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.view.WindowManager;
/** M: New Feature Phone Landscape UI @{ */
import android.widget.ImageView;
/** @ }*/
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;

import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.calllog.CallLogFragment;
import com.android.contacts.calllog.DefaultVoicemailNotifier;
import com.android.contacts.dialpad.DialpadFragment;
import com.android.contacts.ext.ContactPluginDefault;
import com.android.contacts.interactions.PhoneNumberInteraction;
import com.android.contacts.list.ContactListFilterController;
import com.android.contacts.list.ContactListFilterController.ContactListFilterListener;
import com.android.contacts.list.ContactListItemView;
import com.android.contacts.list.OnPhoneNumberPickerActionListener;
import com.android.contacts.list.PhoneFavoriteFragment;
import com.android.contacts.list.PhoneNumberPickerFragment;
//The following lines are provided and maintained by Mediatek Inc.
import com.android.contacts.list.ProviderStatusWatcher;
import com.android.contacts.list.ProviderStatusWatcher.ProviderStatusListener;
//The previous lines are provided and maintained by Mediatek Inc.
import com.android.contacts.util.AccountFilterUtil;
import com.android.contacts.util.Constants;
import com.android.internal.telephony.ITelephony;

// The following lines are provided and maintained by Mediatek Inc.
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.Profiler;
import com.mediatek.contacts.SpecialCharSequenceMgrProxy;
import com.mediatek.contacts.simcontact.AbstractStartSIMService;
import com.mediatek.contacts.simcontact.SlotUtils;
import com.mediatek.contacts.util.SetIndicatorUtils;
import com.mediatek.phone.HyphonManager;
import com.mediatek.xlog.Xlog;

import java.util.List;
import java.util.Locale;
// The previous lines are provided and maintained by Mediatek Inc.
/**
 * The dialer activity that has one tab with the virtual 12key
 * dialer, a tab with recent calls in it, a tab with the contacts and
 * a tab with the favorite. This is the container and the tabs are
 * embedded using intents.
 * The dialer tab's title is 'phone', a more common name (see strings.xml).
 */
/*
 * Bug Fix by Mediatek Begin. 
 * Original Android's code: 
 * public class DialtactsActivity extends TransactionSafeActivity
        implements View.OnClickListener {
   CR ID: ALPS00115673
 * Descriptions: add wait cursor
 */
public class DialtactsActivity extends TransactionSafeActivity
        implements View.OnClickListener, ProviderStatusListener {
    /*
     * Bug Fix by Mediatek End.
     */
    private static final String TAG = "DialtactsActivity";

    public static final boolean DEBUG = true;

    /** Used to open Call Setting */
    private static final String PHONE_PACKAGE = "com.android.phone";
    /** M: New Feature xxx @{ */
    /* original code
    private static final String CALL_SETTINGS_CLASS_NAME =
            "com.android.phone.CallFeaturesSetting";
    */
    private static final String CALL_SETTINGS_CLASS_NAME = "com.mediatek.settings.CallSettings";
    /** @} */

    /**
     * Copied from PhoneApp. See comments in Phone app for more detail.
     */
    public static final String EXTRA_CALL_ORIGIN = "com.android.phone.CALL_ORIGIN";
    /** @see #getCallOrigin() */
    private static final String CALL_ORIGIN_DIALTACTS =
            "com.android.contacts.activities.DialtactsActivity";

    /**
     * Just for backward compatibility. Should behave as same as {@link Intent#ACTION_DIAL}.
     */
    private static final String ACTION_TOUCH_DIALER = "com.android.phone.action.TOUCH_DIALER";

    /** Used both by {@link ActionBar} and {@link ViewPagerAdapter} */
    private static final int TAB_INDEX_DIALER = 0;
    private static final int TAB_INDEX_CALL_LOG = 1;
    private static final int TAB_INDEX_FAVORITES = 2;

    private static final int TAB_INDEX_COUNT = 3;

    private SharedPreferences mPrefs;

    /** Last manually selected tab index */
    private static final String PREF_LAST_MANUALLY_SELECTED_TAB =
            "DialtactsActivity_last_manually_selected_tab";
    private static final int PREF_LAST_MANUALLY_SELECTED_TAB_DEFAULT = TAB_INDEX_DIALER;

    private static final int SUBACTIVITY_ACCOUNT_FILTER = 1;
    /** M: New Feature Phone Landscape UI @{ */
    private static boolean  sIsLandscape; //device is landscape or not
    /** @ }*/

    public class ViewPagerAdapter extends FragmentPagerAdapter {
        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case TAB_INDEX_DIALER:
                    return new DialpadFragment();
                case TAB_INDEX_CALL_LOG:
                    return new CallLogFragment();
                case TAB_INDEX_FAVORITES:
                    return new PhoneFavoriteFragment();
            }
            throw new IllegalStateException("No fragment at position " + position);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            // The parent's setPrimaryItem() also calls setMenuVisibility(), so we want to know
            // when it happens.
            if (DEBUG) {
                Log.d(TAG, "FragmentPagerAdapter#setPrimaryItem(), position: " + position);
            }
            super.setPrimaryItem(container, position, object);
        }

        @Override
        public int getCount() {
            return TAB_INDEX_COUNT;
        }
    }

    /**
     * True when the app detects user's drag event. This variable should not become true when
     * mUserTabClick is true.
     *
     * During user's drag or tab click, we shouldn't show fake buttons but just show real
     * ActionBar at the bottom of the screen, for transition animation.
     */
    boolean mDuringSwipe = false;
    /**
     * True when the app detects user's tab click (at the top of the screen). This variable should
     * not become true when mDuringSwipe is true.
     *
     * During user's drag or tab click, we shouldn't show fake buttons but just show real
     * ActionBar at the bottom of the screen, for transition animation.
     */
    boolean mUserTabClick = false;

    private class PageChangeListener implements OnPageChangeListener {
        private int mCurrentPosition = -1;
        /**
         * Used during page migration, to remember the next position {@link #onPageSelected(int)}
         * specified.
         */
        private int mNextPosition = -1;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            /** M: If the conditions is met, there would be a right place to control visible of the menus @{ */
            if (mTabListener.getFromPosition() == TAB_INDEX_CALL_LOG && mTabListener.getToPosition() == TAB_INDEX_DIALER) {
                disableAllMenus(mMenu);
                Log.d(TAG, "onPageScrolled--disableAllMenus");
            }
            /** @} */
        }

        @Override
        public void onPageSelected(int position) {
            if (DEBUG) Log.d(TAG, "onPageSelected: position: " + position);
            final ActionBar actionBar = getActionBar();
            if (mDialpadFragment != null) {
                if (mDuringSwipe && position == TAB_INDEX_DIALER) {
                    // TODO: Figure out if we want this or not. Right now
                    // - with this call, both fake buttons and real action bar overlap
                    // - without this call, there's tiny flicker happening to search/menu buttons.
                    // If we can reduce the flicker without this call, it would be much better.
                    // updateFakeMenuButtonsVisibility(true);
                }
            }

            if (mCurrentPosition == position) {
                Log.w(TAG, "Previous position and next position became same (" + position + ")");
            }

            actionBar.selectTab(actionBar.getTabAt(position));
            mNextPosition = position;

            /** M: Performance tuning for display number/name slow after a call @{ */
            if (null == mPhoneFavoriteFragment) {
                return;
            }
            boolean bEnabled = false;
            if (TAB_INDEX_FAVORITES == position) {
                bEnabled = true;
            }
            Log.d(TAG, "onPageSelected: position:" + position + " Notified Enabled:" + bEnabled);
            mPhoneFavoriteFragment.setDataSetChangedNotifyEnable(bEnabled);
            /** @} */
        }

        public void setCurrentPosition(int position) {
            mCurrentPosition = position;
        }

        public int getCurrentPosition() {
            return mCurrentPosition;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            switch (state) {
                case ViewPager.SCROLL_STATE_IDLE: {
                    if (mNextPosition == -1) {
                        // This happens when the user drags the screen just after launching the
                        // application, and settle down the same screen without actually swiping it.
                        // At that moment mNextPosition is apparently -1 yet, and we expect it
                        // being updated by onPageSelected(), which is *not* called if the user
                        // settle down the exact same tab after the dragging.
                        if (DEBUG) {
                            Log.d(TAG, "Next position is not specified correctly. Use current tab ("
                                    + mViewPager.getCurrentItem() + ")");
                        }
                        mNextPosition = mViewPager.getCurrentItem();
                    }
                    if (DEBUG) {
                        Log.d(TAG, "onPageScrollStateChanged() with SCROLL_STATE_IDLE. "
                                + "mCurrentPosition: " + mCurrentPosition
                                + ", mNextPosition: " + mNextPosition);
                    }
                    // Interpret IDLE as the end of migration (both swipe and tab click)

                    /**M: To judge if we have drag the screen to the border. CR: ALPS00399346 @{*/
                    if (mNextPosition == mCurrentPosition
                            && mNextPosition != mViewPager.getCurrentItem()) {
                        mNextPosition = mViewPager.getCurrentItem();
                        Log.i(TAG, "Draging to border,mCurrentPosition= " + mCurrentPosition
                                + ", mNextPosition= " + mNextPosition);
                        final ActionBar actionBar = getActionBar();
                        actionBar.selectTab(actionBar.getTabAt(mNextPosition));
                    }
                    /**@}*/

                    mDuringSwipe = false;
                    mUserTabClick = false;

                    updateFakeMenuButtonsVisibility(mNextPosition == TAB_INDEX_DIALER);
                    sendFragmentVisibilityChange(mCurrentPosition, false);
                    sendFragmentVisibilityChange(mNextPosition, true);

                    invalidateOptionsMenu();

                    mCurrentPosition = mNextPosition;
                    break;
                }
                case ViewPager.SCROLL_STATE_DRAGGING: {
                    if (DEBUG) Log.d(TAG, "onPageScrollStateChanged() with SCROLL_STATE_DRAGGING");
                    mDuringSwipe = true;
                    mUserTabClick = false;
                    break;
                }
                case ViewPager.SCROLL_STATE_SETTLING: {
                    if (DEBUG) Log.d(TAG, "onPageScrollStateChanged() with SCROLL_STATE_SETTLING");
                    mDuringSwipe = true;
                    mUserTabClick = false;
                    break;
                }
                default:
                    break;
            }
        }
    }

    private String mFilterText;

    /** Enables horizontal swipe between Fragments. */
    private ViewPager mViewPager;
    private final PageChangeListener mPageChangeListener = new PageChangeListener();
    private DialpadFragment mDialpadFragment;
    private CallLogFragment mCallLogFragment;
    private PhoneFavoriteFragment mPhoneFavoriteFragment;

    //private View mSearchButton;
    //private View mMenuButton;

    private final ContactListFilterListener mContactListFilterListener =
            new ContactListFilterListener() {
        @Override
        public void onContactListFilterChanged() {
            boolean doInvalidateOptionsMenu = false;

            if (mPhoneFavoriteFragment != null && mPhoneFavoriteFragment.isAdded()) {
                mPhoneFavoriteFragment.setFilter(mContactListFilterController.getFilter());
                doInvalidateOptionsMenu = true;
            }

            if (mSearchFragment != null && mSearchFragment.isAdded()) {
                mSearchFragment.setFilter(mContactListFilterController.getFilter());
                doInvalidateOptionsMenu = true;
            } else {
                Log.w(TAG, "Search Fragment isn't available when ContactListFilter is changed");
            }

            if (doInvalidateOptionsMenu) {
                invalidateOptionsMenu();
            }
        }
    };

    private class DialerTabListener implements TabListener {
        /** M: Store the positions from where(fromPosition) to where(toPosition) @ { */
        private int fromPosition = -1;
        private int toPosition = -1;
        /** @ } */
        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (DEBUG) {
                Log.d(TAG, "onTabUnselected(). tab: " + tab);
            }
            /** M: Record "fromPosition" @ { */
            fromPosition = tab.getPosition();
            Log.d(TAG, "set fromPostion---  " + fromPosition);
            /** @ } */
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            /** M: Record "toPosition" @ { */
            toPosition = tab.getPosition();
            Log.d(TAG, "set toPostion---  " + toPosition);
            /** @ } */
            if (DEBUG) {
                Log.d(TAG, "onTabSelected(). tab: " + tab + ", mDuringSwipe: " + mDuringSwipe);
            }
            // When the user swipes the screen horizontally, this method will be called after
            // ViewPager.SCROLL_STATE_DRAGGING and ViewPager.SCROLL_STATE_SETTLING events, while
            // when the user clicks a tab at the ActionBar at the top, this will be called before
            // them. This logic interprets the order difference as a difference of the user action.
            if (!mDuringSwipe) {
                if (DEBUG) {
                    Log.d(TAG, "Tab select. from: " + mPageChangeListener.getCurrentPosition()
                            + ", to: " + tab.getPosition());
                }
                if (mDialpadFragment != null) {
                    updateFakeMenuButtonsVisibility(tab.getPosition() == TAB_INDEX_DIALER);
                }
                mUserTabClick = true;
            }

            if (mViewPager.getCurrentItem() != tab.getPosition()) {
                mViewPager.setCurrentItem(tab.getPosition(), true);
            }

            // During the call, we don't remember the tab position.
            if (!DialpadFragment.phoneIsInUse()) {
                // Remember this tab index. This function is also called, if the tab is set
                // automatically in which case the setter (setCurrentTab) has to set this to its old
                // value afterwards
                mLastManuallySelectedFragment = tab.getPosition();
            }
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            if (DEBUG) Log.d(TAG, "onTabReselected");
        }
        /** M: Get the value of "fromPosition" and "toPosition" @ { */
        public int getFromPosition() {
            return fromPosition;
        }

        public int getToPosition() {
            return toPosition;
        }
        /** @ } */
    }

    /** M: TabListener for pageViewer @ { */
    private final DialerTabListener mTabListener = new DialerTabListener();
    /** @ } */

    /**
     * Fragment for searching phone numbers. Unlike the other Fragments, this doesn't correspond
     * to tab but is shown by a search action.
     */
    private PhoneNumberPickerFragment mSearchFragment;
    /**
     * True when this Activity is in its search UI (with a {@link SearchView} and
     * {@link PhoneNumberPickerFragment}).
     */
    private boolean mInSearchUi;
    private SearchView mSearchView;

    /** M:  delete:unused function @ { */
    /**
     * 
    private final OnClickListener mFilterOptionClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            final PopupMenu popupMenu = new PopupMenu(DialtactsActivity.this, view);
            final Menu menu = popupMenu.getMenu();
            popupMenu.inflate(R.menu.dialtacts_search_options);
            final MenuItem filterOptionMenuItem = menu.findItem(R.id.filter_option);
            filterOptionMenuItem.setOnMenuItemClickListener(mFilterOptionsMenuItemClickListener);
            final MenuItem addContactOptionMenuItem = menu.findItem(R.id.add_contact);
            addContactOptionMenuItem.setIntent(
                    new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI));
            popupMenu.show();
        }
    };
     */
    /** @ } */

    /**
     * The index of the Fragment (or, the tab) that has last been manually selected.
     * This value does not keep track of programmatically set Tabs (e.g. Call Log after a Call)
     */
    private int mLastManuallySelectedFragment;

    private ContactListFilterController mContactListFilterController;
    private OnMenuItemClickListener mFilterOptionsMenuItemClickListener =
            new OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            AccountFilterUtil.startAccountFilterActivityForResult(
                    DialtactsActivity.this, SUBACTIVITY_ACCOUNT_FILTER,
                    mContactListFilterController.getFilter());
            return true;
        }
    };

    private OnMenuItemClickListener mSearchMenuItemClickListener =
            new OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            enterSearchUi();
            return true;
        }
    };

    /**
     * Listener used when one of phone numbers in search UI is selected. This will initiate a
     * phone call using the phone number.
     */
    private final OnPhoneNumberPickerActionListener mPhoneNumberPickerActionListener =
            new OnPhoneNumberPickerActionListener() {
                @Override
                public void onPickPhoneNumberAction(Uri dataUri) {
                    // Specify call-origin so that users will see the previous tab instead of
                    // CallLog screen (search UI will be automatically exited).
                    PhoneNumberInteraction.startInteractionForPhoneCall(
                            DialtactsActivity.this, dataUri, getCallOrigin());
                }

                @Override
                public void onShortcutIntentCreated(Intent intent) {
                    Log.w(TAG, "Unsupported intent has come (" + intent + "). Ignoring.");
                }

                @Override
                public void onHomeInActionBarSelected() {
                    exitSearchUi();
                }
    };

    /**
     * Listener used to send search queries to the phone search fragment.
     */
    private final OnQueryTextListener mPhoneSearchQueryTextListener =
            new OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    View view = getCurrentFocus();
                    if (view != null) {
                        hideInputMethod(view);
                        view.clearFocus();
                    }
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    // Show search result with non-empty text. Show a bare list otherwise.
                    if (mSearchFragment != null) {
                        mSearchFragment.setQueryString(newText, true);
                    }
                    return true;
                }
    };

    /**
     * Listener used to handle the "close" button on the right side of {@link SearchView}.
     * If some text is in the search view, this will clean it up. Otherwise this will exit
     * the search UI and let users go back to usual Phone UI.
     *
     * This does _not_ handle back button.
     */
    private final OnCloseListener mPhoneSearchCloseListener =
            new OnCloseListener() {
                @Override
                public boolean onClose() {
                    if (!TextUtils.isEmpty(mSearchView.getQuery())) {
                        mSearchView.setQuery(null, true);
                    }
                    return true;
                }
    };

    private final View.OnLayoutChangeListener mFirstLayoutListener
            = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                int oldTop, int oldRight, int oldBottom) {
            v.removeOnLayoutChangeListener(this); // Unregister self.
            addSearchFragment();
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        /** M: New Feature Phone Landscape UI @{ */
        sIsLandscape = isLandscapeView();
        /** @ }*/
        /**
         * add by mediatek .inc
         * description register the sim indicator changed broadcast receiver
         */
        SetIndicatorUtils.getInstance().registerReceiver(this);
        /**
         * add by mediatek .inc end
         */
        /** M: New Feature xxx @{ */
        Profiler.trace(Profiler.DialtactsActivityEnterOnCreate);
        /** @} */
        final Intent intent = getIntent();
        fixIntent(intent);
        setContentView(R.layout.dialtacts_activity);

        mContactListFilterController = ContactListFilterController.getInstance(this);
        mContactListFilterController.addListener(mContactListFilterListener);

        findViewById(R.id.dialtacts_frame).addOnLayoutChangeListener(mFirstLayoutListener);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(new ViewPagerAdapter(getFragmentManager()));
        mViewPager.setOnPageChangeListener(mPageChangeListener);
        mViewPager.setOffscreenPageLimit(2);
        addDialpadScrollingThreshold(true);
        // Do same width calculation as ActionBar does
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int minCellSize = getResources().getDimensionPixelSize(R.dimen.fake_menu_button_min_width);
        int cellCount = dm.widthPixels / minCellSize;
        int fakeMenuItemWidth = dm.widthPixels / cellCount;
        if (DEBUG) Log.d(TAG, "The size of fake menu buttons (in pixel): " + fakeMenuItemWidth);

        // Soft menu button should appear only when there's no hardware menu button.
        //MTK add start
        /*
        //MTK add end
        mMenuButton = findViewById(R.id.overflow_menu);
        if (mMenuButton != null) {
            mMenuButton.setMinimumWidth(fakeMenuItemWidth);
            if (ViewConfiguration.get(this).hasPermanentMenuKey()) {
                // This is required for dialpad button's layout, so must not use GONE here.
                mMenuButton.setVisibility(View.INVISIBLE);
            } else {
                mMenuButton.setOnClickListener(this);
            }
        }
        //MTK add start
        */
        //MTK add end
        /*mSearchButton = findViewById(R.id.searchButton);
        if (mSearchButton != null) {
            mSearchButton.setMinimumWidth(fakeMenuItemWidth);
            mSearchButton.setOnClickListener(this);
        }*/

        // Setup the ActionBar tabs (the order matches the tab-index contants TAB_INDEX_*)
        setupDialer();
        setupCallLog();
        setupFavorites();
        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().setDisplayShowHomeEnabled(false);

        // Load the last manually loaded tab
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mLastManuallySelectedFragment = mPrefs.getInt(PREF_LAST_MANUALLY_SELECTED_TAB,
                PREF_LAST_MANUALLY_SELECTED_TAB_DEFAULT);
        if (mLastManuallySelectedFragment >= TAB_INDEX_COUNT) {
            // Stored value may have exceeded the number of current tabs. Reset it.
            mLastManuallySelectedFragment = PREF_LAST_MANUALLY_SELECTED_TAB_DEFAULT;
        }

        /** M:  Bug fix for ALPS00421431@ { */
        if (null != icicle) {
            mPresetPageIndex = icicle.getInt(PAGE_INDEX, -1);
        }
        /** @ } */
        setCurrentTab(intent);

        if (UI.FILTER_CONTACTS_ACTION.equals(intent.getAction())
                && icicle == null) {
            setupFilterText(intent);
        }
        IntentFilter phbLoadIntentFilter = new IntentFilter(
                (AbstractStartSIMService.ACTION_PHB_LOAD_FINISHED));
        this.registerReceiver(mReceiver, phbLoadIntentFilter);
        mLaunched = true;
        mProviderStatusWatcher.addListener(this);
        Profiler.trace(Profiler.DialtactsActivityLeaveOnCreate);
    }
    /** M: New Feature Phone Landscape UI @{ */
      /**
         * return true if  the device is landscape 
         */
    public boolean isLandscapeView() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return true;
        } else {
            return false;
        }
    }
    /** @ }*/

    @Override
    public void onResume() {
        super.onResume();

        /**
         * add by mediatek .inc
         * description : show the sim indicator when 
         * Activity onResume
         */
        SetIndicatorUtils.getInstance().showIndicator(true, this);
        /*
         * Bug Fix by Mediatek Begin. Original Android's code: CR ID:
         * ALPS00115673 Descriptions: add wait cursor
         */

        mProviderStatusWatcher.start();
        /*
         * Bug Fix by Mediatek End.
         */

    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPhoneFavoriteFragment != null) {
            mPhoneFavoriteFragment.setFilter(mContactListFilterController.getFilter());
        }
        if (mSearchFragment != null) {
            mSearchFragment.setFilter(mContactListFilterController.getFilter());
        }

        if (mDuringSwipe || mUserTabClick) {
            if (DEBUG) Log.d(TAG, "reset buggy flag state..");
            mDuringSwipe = false;
            mUserTabClick = false;
        }

        final int currentPosition = mPageChangeListener.getCurrentPosition();
        if (DEBUG) {
            Log.d(TAG, "onStart(). current position: " + mPageChangeListener.getCurrentPosition()
                    + ". Reset all menu visibility state.");
        }
        updateFakeMenuButtonsVisibility(currentPosition == TAB_INDEX_DIALER && !mInSearchUi);
        for (int i = 0; i < TAB_INDEX_COUNT; i++) {
            if (mSearchFragment != null && mSearchFragment.isVisible()) {
                sendFragmentVisibilityChange(i, false);
            } else {
                sendFragmentVisibilityChange(i, i == currentPosition);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /** M: New Feature xxx @{ */
        SpecialCharSequenceMgrProxy.dismissDialog();
        /** @} */
        mContactListFilterController.removeListener(mContactListFilterListener);

        /**
         * add by mediatek .inc
         * description : unregister the sim indicator changed broadcast receiver
         */
        SetIndicatorUtils.getInstance().unregisterReceiver(this);
        /**
         * add by mediatek .inc end
         */
        unregisterReceiver(mReceiver);

        /** M: comment new feature*/
        mProviderStatusWatcher.removeListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            /*case R.id.searchButton: {
                enterSearchUi();
                break;
            }*/
            //MTK add start
            /*
            //MTK add end
            case R.id.overflow_menu: {
                if (mDialpadFragment != null) {
                    PopupMenu popup = mDialpadFragment.constructPopupMenu(view);
                    if (popup != null) {
                        popup.show();
                    }
                } else {
                    Log.w(TAG, "DialpadFragment is null during onClick() event for " + view);
                }
                break;
            }
            //MTK add start
            */
            //MTK add end
            default: {
                Log.wtf(TAG, "Unexpected onClick event from " + view);
                break;
            }
        }
    }

    /**
     * Add search fragment.  Note this is called during onLayout, so there's some restrictions,
     * such as executePendingTransaction can't be used in it.
     */
    private void addSearchFragment() {
        // In order to take full advantage of "fragment deferred start", we need to create the
        // search fragment after all other fragments are created.
        // The other fragments are created by the ViewPager on the first onMeasure().
        // We use the first onLayout call, which is after onMeasure().

        // Just return if the fragment is already created, which happens after configuration
        // changes.
        if (mSearchFragment != null) return;

        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment searchFragment = new PhoneNumberPickerFragment();

        searchFragment.setUserVisibleHint(false);
        /** M: Performance tuning for display number/name slow after a call @{ */
        ((PhoneNumberPickerFragment) searchFragment).setDataSetChangedNotifyEnable(false);
        /** @} */

        ft.add(R.id.dialtacts_frame, searchFragment);
        ft.hide(searchFragment);
        ft.commitAllowingStateLoss();
    }

    private void prepareSearchView() {
        final View searchViewLayout =
                getLayoutInflater().inflate(R.layout.dialtacts_custom_action_bar, null);
        mSearchView = (SearchView) searchViewLayout.findViewById(R.id.search_view);
        mSearchView.setOnQueryTextListener(mPhoneSearchQueryTextListener);
        mSearchView.setOnCloseListener(mPhoneSearchCloseListener);
        // Since we're using a custom layout for showing SearchView instead of letting the
        // search menu icon do that job, we need to manually configure the View so it looks
        // "shown via search menu".
        // - it should be iconified by default
        // - it should not be iconified at this time
        // See also comments for onActionViewExpanded()/onActionViewCollapsed()
        mSearchView.setIconifiedByDefault(true);
        mSearchView.setQueryHint(getString(R.string.hint_findContacts));
        mSearchView.setIconified(false);
        mSearchView.setOnQueryTextFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    showInputMethod(view.findFocus());
                }
            }
        });

        // turn off search_option in searchView and use filterOptionMenuItem
        // in actionbar to replace it
        /*
        if (!ViewConfiguration.get(this).hasPermanentMenuKey()) {
            // Filter option menu should be shown on the right side of SearchView.
            final View filterOptionView = searchViewLayout.findViewById(R.id.search_option);
            filterOptionView.setVisibility(View.VISIBLE);
            filterOptionView.setOnClickListener(mFilterOptionClickListener);
        }
        */

        getActionBar().setCustomView(searchViewLayout,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        // This method can be called before onCreate(), at which point we cannot rely on ViewPager.
        // In that case, we will setup the "current position" soon after the ViewPager is ready.
        final int currentPosition = mViewPager != null ? mViewPager.getCurrentItem() : -1;

        if (fragment instanceof DialpadFragment) {
            mDialpadFragment = (DialpadFragment) fragment;
        } else if (fragment instanceof CallLogFragment) {
            mCallLogFragment = (CallLogFragment) fragment;
        } else if (fragment instanceof PhoneFavoriteFragment) {
            mPhoneFavoriteFragment = (PhoneFavoriteFragment) fragment;
            mPhoneFavoriteFragment.setListener(mPhoneFavoriteListener);
            if (mContactListFilterController != null
                    && mContactListFilterController.getFilter() != null) {
                mPhoneFavoriteFragment.setFilter(mContactListFilterController.getFilter());
            }
        } else if (fragment instanceof PhoneNumberPickerFragment) {
            mSearchFragment = (PhoneNumberPickerFragment) fragment;
            mSearchFragment.setOnPhoneNumberPickerActionListener(mPhoneNumberPickerActionListener);
            mSearchFragment.setQuickContactEnabled(true);
            mSearchFragment.setDarkTheme(true);
            mSearchFragment.setPhotoPosition(ContactListItemView.PhotoPosition.LEFT);
            mSearchFragment.setUseCallableUri(true);
            if (mContactListFilterController != null
                    && mContactListFilterController.getFilter() != null) {
                mSearchFragment.setFilter(mContactListFilterController.getFilter());
            }
            // Here we assume that we're not on the search mode, so let's hide the fragment.
            //
            // We get here either when the fragment is created (normal case), or after configuration
            // changes.  In the former case, we're not in search mode because we can only
            // enter search mode if the fragment is created.  (see enterSearchUi())
            // In the latter case we're not in search mode either because we don't retain
            // mInSearchUi -- ideally we should but at this point it's not supported.
            mSearchFragment.setUserVisibleHint(false);
            // After configuration changes fragments will forget their "hidden" state, so make
            // sure to hide it.
            if (!mSearchFragment.isHidden()) {
                final FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.hide(mSearchFragment);
                transaction.commitAllowingStateLoss();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The following lines are provided and maintained by Mediatek Inc.
        Profiler.trace(Profiler.DialtactsActivityEnterOnPause);

        // MTK81281 add null pointer protect for Cr:ALPS00119664 start
        if (mCallLogFragment != null) {
            if (mCallLogFragment.mSelectResDialog != null) {
                if (mCallLogFragment.mSelectResDialog.isShowing()) {
                    mCallLogFragment.mSelectResDialog.dismiss();
                    mCallLogFragment.mSelectResDialog = null;
                }
            }
        }
        //MTK81281 add null pointer protect for Cr:ALPS00119664 end
        // The previous lines are provided and maintained by Mediatek Inc.
        
        mPrefs.edit().putInt(PREF_LAST_MANUALLY_SELECTED_TAB, mLastManuallySelectedFragment)
                .apply();

        /**
         * add by mediatek .inc
         * description : hide the sim indicator when
         * activity onPause
         */
        SetIndicatorUtils.getInstance().showIndicator(false, this);

        Profiler.trace(Profiler.DialtactsActivityLeaveOnPause);

        /*
         * Bug Fix by Mediatek Begin. Original Android's code: CR ID:
         * ALPS00115673 Descriptions: add wait cursor
         */
         
        mProviderStatusWatcher.stop();
        /*
         * Bug Fix by Mediatek End.
         */
    }

    private void fixIntent(Intent intent) {
        // This should be cleaned up: the call key used to send an Intent
        // that just said to go to the recent calls list.  It now sends this
        // abstract action, but this class hasn't been rewritten to deal with it.
        if (Intent.ACTION_CALL_BUTTON.equals(intent.getAction())) {
            intent.setDataAndType(Calls.CONTENT_URI, Calls.CONTENT_TYPE);
            intent.putExtra("call_key", true);
            setIntent(intent);
        }
    }

    private void setupDialer() {
        /** M: New Feature Phone Landscape UI @{ */
        if (sIsLandscape) {
            final Tab tab = getActionBar().newTab();
            tab.setContentDescription(R.string.dialerIconLabel);
            tab.setTabListener(mTabListener);
            ImageView tabView = new ImageView(this.getApplication());
            tabView.setPadding(0, 10, 0, 0);
            tabView.setImageResource(R.drawable.ic_tab_dialer);     
            //int tabWidth = getResources().getDimensionPixelSize(
            //    R.dimen.phone_tab_width);      
            tabView.setLayoutParams(new ViewGroup.LayoutParams(120,LayoutParams.WRAP_CONTENT));
            tab.setCustomView(tabView);
            getActionBar().addTab(tab);
            /*
             final Tab tab = getActionBar().newTab();
            tab.setContentDescription(R.string.dialerIconLabel);
            tab.setTabListener(mTabListener);
            tab.setIcon(R.drawable.ic_tab_dialer);
            getActionBar().addTab(tab);
            */
            
        } else {
        /** @ }*/
        final Tab tab = getActionBar().newTab();
        tab.setContentDescription(R.string.dialerIconLabel);
        tab.setTabListener(mTabListener);
        tab.setIcon(R.drawable.ic_tab_dialer);
        getActionBar().addTab(tab);
        }

    }

    private void setupCallLog() {
        /** M: New Feature Phone Landscape UI @{ */
        if (sIsLandscape) {
            final Tab tab = getActionBar().newTab();
            tab.setContentDescription(R.string.recentCallsIconLabel);
            tab.setIcon(R.drawable.ic_tab_recent);
            tab.setTabListener(mTabListener);
            getActionBar().addTab(tab);
        } else {
            /** @ } */
        final Tab tab = getActionBar().newTab();
        tab.setContentDescription(R.string.recentCallsIconLabel);
        tab.setIcon(R.drawable.ic_tab_recent);
        tab.setTabListener(mTabListener);
        getActionBar().addTab(tab);
        }

    }

    private void setupFavorites() {
        /** M: New Feature Phone Landscape UI @{ */
        if (sIsLandscape) {
            final Tab tab = getActionBar().newTab();
            tab.setContentDescription(R.string.contactsFavoritesLabel);
            tab.setIcon(R.drawable.ic_tab_all);
            tab.setTabListener(mTabListener);
            getActionBar().addTab(tab);
        } else {
        /** @ }*/
        final Tab tab = getActionBar().newTab();
        tab.setContentDescription(R.string.dialerAllContactsLabel);
        tab.setIcon(R.drawable.ic_tab_all);
        tab.setTabListener(mTabListener);
        getActionBar().addTab(tab);
        }

    }

    /**
     * Returns true if the intent is due to hitting the green send key while in a call.
     *
     * @param intent the intent that launched this activity
     * @param recentCallsRequest true if the intent is requesting to view recent calls
     * @return true if the intent is due to hitting the green send key while in a call
     */
    private boolean isSendKeyWhileInCall(final Intent intent,
            final boolean recentCallsRequest) {
        // If there is a call in progress go to the call screen
        if (recentCallsRequest) {
            final boolean callKey = intent.getBooleanExtra("call_key", false);

            try {
                ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                if (callKey && phone != null && phone.showCallScreen()) {
                    return true;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to handle send while in call", e);
            }
        }

        return false;
    }

    /**
     * Sets the current tab based on the intent's request type
     *
     * @param intent Intent that contains information about which tab should be selected
     */
    private void setCurrentTab(Intent intent) {
        // If we got here by hitting send and we're in call forward along to the in-call activity
        final boolean recentCallsRequest = Calls.CONTENT_TYPE.equals(intent.getType());
        if (isSendKeyWhileInCall(intent, recentCallsRequest)) {
            finish();
            return;
        }

        // Remember the old manually selected tab index so that it can be restored if it is
        // overwritten by one of the programmatic tab selections
        final int savedTabIndex = mLastManuallySelectedFragment;

        final int tabIndex;
        if (DialpadFragment.phoneIsInUse() || isDialIntent(intent)) {
            tabIndex = TAB_INDEX_DIALER;
        } else if (recentCallsRequest || isMissedCallExist() ||
                DefaultVoicemailNotifier.getInstance(getApplicationContext()).hasNewVoicemails()) {
            tabIndex = TAB_INDEX_CALL_LOG;
        } else {
            tabIndex = mLastManuallySelectedFragment;
        }

        final int previousItemIndex = mViewPager.getCurrentItem();
        mViewPager.setCurrentItem(tabIndex, false /* smoothScroll */);
        if (previousItemIndex != tabIndex) {
            sendFragmentVisibilityChange(previousItemIndex, false /* not visible */ );
        }
        mPageChangeListener.setCurrentPosition(tabIndex);
        sendFragmentVisibilityChange(tabIndex, true /* visible */ );

        // Restore to the previous manual selection
        mLastManuallySelectedFragment = savedTabIndex;
        mDuringSwipe = false;
        mUserTabClick = false;
        mPresetPageIndex = -1;
    }

    @Override
    public void onNewIntent(Intent newIntent) {
        /** M: update dialerSearch results only when "data" is not the same @{ */
        if (newIntent.getData() != null) {
            if (!newIntent.getData().equals(getIntent().getData())) {
                if (mDialpadFragment != null)
                    mDialpadFragment.forceUpdateDialerSearch();
            }
        }
        /** @ } */
        setIntent(newIntent);
        fixIntent(newIntent);
        setCurrentTab(newIntent);
        final String action = newIntent.getAction();
        if (UI.FILTER_CONTACTS_ACTION.equals(action)) {
            setupFilterText(newIntent);
        }
        if (mInSearchUi || (mSearchFragment != null && mSearchFragment.isVisible())) {
            exitSearchUi();
        }

        if (mViewPager.getCurrentItem() == TAB_INDEX_DIALER) {
            if (mDialpadFragment != null) {
                mDialpadFragment.setStartedFromNewIntent(true);
            } else {
                Log.e(TAG, "DialpadFragment isn't ready yet when the tab is already selected.");
            }
        } else if (mViewPager.getCurrentItem() == TAB_INDEX_CALL_LOG) {
            if (mCallLogFragment != null) {
                mCallLogFragment.configureScreenFromIntent(newIntent);
            } else {
                Log.e(TAG, "CallLogFragment isn't ready yet when the tab is already selected.");
            }
        }
        invalidateOptionsMenu();
    }

    /** Returns true if the given intent contains a phone number to populate the dialer with */
    private boolean isDialIntent(Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_DIAL.equals(action) || ACTION_TOUCH_DIALER.equals(action)) {
            return true;
        }
        if (Intent.ACTION_VIEW.equals(action)) {
            final Uri data = intent.getData();
            if (data != null && Constants.SCHEME_TEL.equals(data.getScheme())) {
                return true;
            }
        }
        /** M: New Feature Easy Porting @{ */
        return ExtensionManager.getInstance().getDialtactsExtension().checkComponentName(intent,
                ContactPluginDefault.COMMD_FOR_OP01);
        /** @} */
       
    }

    /**
     * Returns an appropriate call origin for this Activity. May return null when no call origin
     * should be used (e.g. when some 3rd party application launched the screen. Call origin is
     * for remembering the tab in which the user made a phone call, so the external app's DIAL
     * request should not be counted.)
     */
    public String getCallOrigin() {
        return !isDialIntent(getIntent()) ? CALL_ORIGIN_DIALTACTS : null;
    }

    /**
     * Retrieves the filter text stored in {@link #setupFilterText(Intent)}.
     * This text originally came from a FILTER_CONTACTS_ACTION intent received
     * by this activity. The stored text will then be cleared after after this
     * method returns.
     *
     * @return The stored filter text
     */
    public String getAndClearFilterText() {
        String filterText = mFilterText;
        mFilterText = null;
        return filterText;
    }

    /**
     * Stores the filter text associated with a FILTER_CONTACTS_ACTION intent.
     * This is so child activities can check if they are supposed to display a filter.
     *
     * @param intent The intent received in {@link #onNewIntent(Intent)}
     */
    private void setupFilterText(Intent intent) {
        // If the intent was relaunched from history, don't apply the filter text.
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            return;
        }
        String filter = intent.getStringExtra(UI.FILTER_TEXT_EXTRA_KEY);
        if (filter != null && filter.length() > 0) {
            mFilterText = filter;
        }
    }

    @Override
    public void onBackPressed() {
        /** M: New Feature xxx @{ */
        Profiler.trace(Profiler.DialtactsActivityOnBackPressed);
        boolean bAutoRejectedFilter = false;
        if (null != mCallLogFragment) {
            bAutoRejectedFilter = mCallLogFragment.isAutoRejectedFilterMode();
        }

        Log.i(TAG, "onBackPressed() Mode:" + bAutoRejectedFilter + " Tab:"
                + mViewPager.getCurrentItem());
        /** @} */
        if (mInSearchUi) {
            // We should let the user go back to usual screens with tabs.
            exitSearchUi();
        }
        /** M: New Feature xxx @{ */
        else if (bAutoRejectedFilter && TAB_INDEX_CALL_LOG == mViewPager.getCurrentItem()) {
            mCallLogFragment.onBackHandled();
        } else if (isTaskRoot()) {
            /** @} */
            // Instead of stopping, simply push this to the back of the stack.
            // This is only done when running at the top of the stack;
            // otherwise, we have been launched by someone else so need to
            // allow the user to go back to the caller.
            moveTaskToBack(false);
            /** M: New Feature xxx @{ */
            if (null != mCallLogFragment) {
                mCallLogFragment.onBackHandled();
            }
            /** @} */
        } else {
            super.onBackPressed();
        }
    }

    private final PhoneFavoriteFragment.Listener mPhoneFavoriteListener =
            new PhoneFavoriteFragment.Listener() {
        @Override
        public void onContactSelected(Uri contactUri) {
            PhoneNumberInteraction.startInteractionForPhoneCall(
                    DialtactsActivity.this, contactUri, getCallOrigin());
        }

        @Override
        public void onCallNumberDirectly(String phoneNumber) {
            Intent intent = ContactsUtils.getCallIntent(phoneNumber, getCallOrigin());
            startActivity(intent);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dialtacts_options, menu);
        /** M: add */
        mMenu = menu;
        /** @ } */
        // set up intents and onClick listeners
        final MenuItem callSettingsMenuItem = menu.findItem(R.id.menu_call_settings);
        final MenuItem searchMenuItem = menu.findItem(R.id.search_on_action_bar);
        final MenuItem filterOptionMenuItem = menu.findItem(R.id.filter_option);
        final MenuItem addContactOptionMenuItem = menu.findItem(R.id.add_contact);

        callSettingsMenuItem.setIntent(DialtactsActivity.getCallSettingsIntent());
        searchMenuItem.setOnMenuItemClickListener(mSearchMenuItemClickListener);
        filterOptionMenuItem.setOnMenuItemClickListener(mFilterOptionsMenuItemClickListener);
        addContactOptionMenuItem.setIntent(
                new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI));
        /** M: add */
        final MenuItem chooseResoucesMenuItem = menu.findItem(R.id.choose_resources);
        chooseResoucesMenuItem.setOnMenuItemClickListener(mChooseResoucesItemClickListener);
        /** @ } */
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mInSearchUi) {
            prepareOptionsMenuInSearchMode(menu);
        } else {
            // get reference to the currently selected tab
            final Tab tab = getActionBar().getSelectedTab();
            if (tab != null) {
                switch(tab.getPosition()) {
                    case TAB_INDEX_DIALER:
                        prepareOptionsMenuForDialerTab(menu);
                        break;
                    case TAB_INDEX_CALL_LOG:
                        prepareOptionsMenuForCallLogTab(menu);
                        break;
                    case TAB_INDEX_FAVORITES:
                        prepareOptionsMenuForFavoritesTab(menu);
                        break;
                }
            }
        }
        return true;
    }

    private void prepareOptionsMenuInSearchMode(Menu menu) {
        // get references to menu items
        final MenuItem searchMenuItem = menu.findItem(R.id.search_on_action_bar);
        final MenuItem filterOptionMenuItem = menu.findItem(R.id.filter_option);
        final MenuItem addContactOptionMenuItem = menu.findItem(R.id.add_contact);
        final MenuItem callSettingsMenuItem = menu.findItem(R.id.menu_call_settings);
        final MenuItem emptyRightMenuItem = menu.findItem(R.id.empty_right_menu_item);
        /** M: add */
        final MenuItem chooseResoucesMenuItem = menu.findItem(R.id.choose_resources);

        // prepare the menu items
        searchMenuItem.setVisible(false);
        // change from hasPermanentMenuKey to true to use menu actionbar to show the menu icon
        // this modification is related with filterOptionView of
        // searchViewLayout off in prepareSearchView function
        filterOptionMenuItem.setVisible(false);
        addContactOptionMenuItem.setVisible(false);
        callSettingsMenuItem.setVisible(false);
        emptyRightMenuItem.setVisible(false);
        /** M: add  */
        chooseResoucesMenuItem.setVisible(false);
    }

    /** M: add  */
    private Menu mMenu;
    private void disableAllMenus(Menu menu) {
        // get references to menu items
        final MenuItem callSettingsMenuItem = menu.findItem(R.id.menu_call_settings);
        final MenuItem searchMenuItem = menu.findItem(R.id.search_on_action_bar);
        final MenuItem chooseResoucesMenuItem = menu.findItem(R.id.choose_resources);

        callSettingsMenuItem.setVisible(false);
        searchMenuItem.setVisible(false);
        chooseResoucesMenuItem.setVisible(false);
        sendFragmentVisibilityChange(TAB_INDEX_CALL_LOG, false);
    }
    /** @ } */

    private void prepareOptionsMenuForDialerTab(Menu menu) {
        if (DEBUG) {
            Log.d(TAG, "onPrepareOptionsMenu(dialer). swipe: " + mDuringSwipe
                    + ", user tab click: " + mUserTabClick);
        }

        // get references to menu items
        final MenuItem searchMenuItem = menu.findItem(R.id.search_on_action_bar);
        final MenuItem filterOptionMenuItem = menu.findItem(R.id.filter_option);
        final MenuItem addContactOptionMenuItem = menu.findItem(R.id.add_contact);
        final MenuItem callSettingsMenuItem = menu.findItem(R.id.menu_call_settings);
        final MenuItem emptyRightMenuItem = menu.findItem(R.id.empty_right_menu_item);
        /** M: add */
        final MenuItem chooseResoucesMenuItem = menu.findItem(R.id.choose_resources);

        // prepare the menu items
        filterOptionMenuItem.setVisible(false);
        addContactOptionMenuItem.setVisible(false);
        /** M: add  */
        chooseResoucesMenuItem.setVisible(false);
        if (mDuringSwipe || mUserTabClick) {
            // During horizontal movement, the real ActionBar menu items are shown
            searchMenuItem.setVisible(true);
            callSettingsMenuItem.setVisible(true);
            // When there is a permanent menu key, there is no overflow icon on the right of
            // the action bar which would force the search menu item (if it is visible) to the
            // left.  This is the purpose of showing the emptyRightMenuItem.
            emptyRightMenuItem.setVisible(ViewConfiguration.get(this).hasPermanentMenuKey());
        } else {
            // This is when the user is looking at the dialer pad.  In this case, the real
            // ActionBar is hidden and fake menu items are shown.
            searchMenuItem.setVisible(false);
            // If a permanent menu key is available, then we need to show the call settings item
            // so that the call settings item can be invoked by the permanent menu key.
            callSettingsMenuItem.setVisible(ViewConfiguration.get(this).hasPermanentMenuKey());
            emptyRightMenuItem.setVisible(false);
        }
    }

    private void prepareOptionsMenuForCallLogTab(Menu menu) {
        // get references to menu items
        final MenuItem searchMenuItem = menu.findItem(R.id.search_on_action_bar);
        final MenuItem filterOptionMenuItem = menu.findItem(R.id.filter_option);
        final MenuItem addContactOptionMenuItem = menu.findItem(R.id.add_contact);
        final MenuItem callSettingsMenuItem = menu.findItem(R.id.menu_call_settings);
        final MenuItem emptyRightMenuItem = menu.findItem(R.id.empty_right_menu_item);
        
        // prepare the menu items
        searchMenuItem.setVisible(true);
        filterOptionMenuItem.setVisible(false);
        addContactOptionMenuItem.setVisible(false);
        callSettingsMenuItem.setVisible(true);
        /** M: modify @ { */
        // emptyRightMenuItem.setVisible(ViewConfiguration.get(this).hasPermanentMenuKey());
        emptyRightMenuItem.setVisible(false);
        /** @ } */
        /** M: add @ { */
        final MenuItem chooseResoucesMenuItem = menu.findItem(R.id.choose_resources);
        if (SlotUtils.isGeminiEnabled()) {
            chooseResoucesMenuItem.setVisible(true);
        } else {
            chooseResoucesMenuItem.setVisible(false);
        }
        /** M: Control the visible of the menus @ { */
        Log.d(TAG, "fromPosition: " + mTabListener.getFromPosition());
        Log.d(TAG, "toPosition: " + mTabListener.getToPosition());

        if (mTabListener.getFromPosition() == TAB_INDEX_CALL_LOG && mTabListener.getToPosition() == TAB_INDEX_DIALER) {
            sendFragmentVisibilityChange(TAB_INDEX_CALL_LOG, false);
            searchMenuItem.setVisible(false);
            callSettingsMenuItem.setVisible(false);
            chooseResoucesMenuItem.setVisible(false);
        } else {
            sendFragmentVisibilityChange(TAB_INDEX_CALL_LOG, true);
            searchMenuItem.setVisible(true);
            callSettingsMenuItem.setVisible(true);
            if (SlotUtils.isGeminiEnabled()) {
                chooseResoucesMenuItem.setVisible(true);
            } else {
                chooseResoucesMenuItem.setVisible(false);
            }
        }
        /** @ } */
    }

    private void prepareOptionsMenuForFavoritesTab(Menu menu) {
        // get references to menu items
        final MenuItem searchMenuItem = menu.findItem(R.id.search_on_action_bar);
        final MenuItem filterOptionMenuItem = menu.findItem(R.id.filter_option);
        final MenuItem addContactOptionMenuItem = menu.findItem(R.id.add_contact);
        final MenuItem callSettingsMenuItem = menu.findItem(R.id.menu_call_settings);
        final MenuItem emptyRightMenuItem = menu.findItem(R.id.empty_right_menu_item);

        // prepare the menu items
        searchMenuItem.setVisible(true);
        filterOptionMenuItem.setVisible(true);
        addContactOptionMenuItem.setVisible(true);
        callSettingsMenuItem.setVisible(true);
        emptyRightMenuItem.setVisible(false);
        /** M: add @ { */
        final MenuItem chooseResoucesMenuItem = menu.findItem(R.id.choose_resources);
        chooseResoucesMenuItem.setVisible(false);
    }

    @Override
    public void startSearch(String initialQuery, boolean selectInitialQuery,
            Bundle appSearchData, boolean globalSearch) {
        if (mSearchFragment != null && mSearchFragment.isAdded() && !globalSearch) {
            if (mInSearchUi) {
                if (mSearchView.hasFocus()) {
                    showInputMethod(mSearchView.findFocus());
                } else {
                    mSearchView.requestFocus();
                }
            } else {
                enterSearchUi();
            }
        } else {
            super.startSearch(initialQuery, selectInitialQuery, appSearchData, globalSearch);
        }
    }

    /**
     * Hides every tab and shows search UI for phone lookup.
     */
    private void enterSearchUi() {
        if (mSearchFragment == null) {
            // We add the search fragment dynamically in the first onLayoutChange() and
            // mSearchFragment is set sometime later when the fragment transaction is actually
            // executed, which means there's a window when users are able to hit the (physical)
            // search key but mSearchFragment is still null.
            // It's quite hard to handle this case right, so let's just ignore the search key
            // in this case.  Users can just hit it again and it will work this time.
            return;
        }

        /** M: Performance tuning for display number/name slow after a call @{ */
        mSearchFragment.setDataSetChangedNotifyEnable(true);
        /** @} */

        if (mSearchView == null) {
            prepareSearchView();
        }

        final ActionBar actionBar = getActionBar();

        final Tab tab = actionBar.getSelectedTab();

        // User can search during the call, but we don't want to remember the status.
        if (tab != null && !DialpadFragment.phoneIsInUse()) {
            mLastManuallySelectedFragment = tab.getPosition();
        }

        mSearchView.setQuery(null, true);

        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        updateFakeMenuButtonsVisibility(false);

        for (int i = 0; i < TAB_INDEX_COUNT; i++) {
            sendFragmentVisibilityChange(i, false /* not visible */ );
        }

        // Show the search fragment and hide everything else.
        mSearchFragment.setUserVisibleHint(true);
        final FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.show(mSearchFragment);
        transaction.commitAllowingStateLoss();
        mViewPager.setVisibility(View.GONE);

        // We need to call this and onActionViewCollapsed() manually, since we are using a custom
        // layout instead of asking the search menu item to take care of SearchView.
        mSearchView.onActionViewExpanded();
        mInSearchUi = true;
    }

    private void showInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (!imm.showSoftInput(view, 0)) {
                Log.w(TAG, "Failed to show soft input method.");
            }
        }
    }

    private void hideInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Goes back to usual Phone UI with tags. Previously selected Tag and associated Fragment
     * should be automatically focused again.
     */
    private void exitSearchUi() {
        final ActionBar actionBar = getActionBar();

        // Hide the search fragment, if exists.
        if (mSearchFragment != null) {
            /** M: Performance tuning for display number/name slow after a call @{ */
            mSearchFragment.setDataSetChangedNotifyEnable(false);
            /** @} */
            mSearchFragment.setUserVisibleHint(false);

            final FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.hide(mSearchFragment);
            transaction.commitAllowingStateLoss();
        }

        // We want to hide SearchView and show Tabs. Also focus on previously selected one.
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        for (int i = 0; i < TAB_INDEX_COUNT; i++) {
            sendFragmentVisibilityChange(i, i == mViewPager.getCurrentItem());
        }

        // Before exiting the search screen, reset swipe state.
        mDuringSwipe = false;
        mUserTabClick = false;

        mViewPager.setVisibility(View.VISIBLE);

        hideInputMethod(getCurrentFocus());

        // Request to update option menu.
        invalidateOptionsMenu();

        // See comments in onActionViewExpanded()
        mSearchView.onActionViewCollapsed();
        mInSearchUi = false;
    }

    private Fragment getFragmentAt(int position) {
        switch (position) {
            case TAB_INDEX_DIALER:
                return mDialpadFragment;
            case TAB_INDEX_CALL_LOG:
                return mCallLogFragment;
            case TAB_INDEX_FAVORITES:
                return mPhoneFavoriteFragment;
            default:
                throw new IllegalStateException("Unknown fragment index: " + position);
        }
    }

    private void sendFragmentVisibilityChange(int position, boolean visibility) {
        if (DEBUG) {
            Log.d(TAG, "sendFragmentVisibiltyChange(). position: " + position
                    + ", visibility: " + visibility);
        }
        // Position can be -1 initially. See PageChangeListener.
        if (position >= 0) {
            final Fragment fragment = getFragmentAt(position);
            if (fragment != null) {
                fragment.setMenuVisibility(visibility);
                fragment.setUserVisibleHint(visibility);
            }
        }
    }

    /**
     * Update visibility of the search button and menu button at the bottom.
     * They should be invisible when bottom ActionBar's real items are available, and be visible
     * otherwise.
     *
     * @param visible True when visible.
     */
    private void updateFakeMenuButtonsVisibility(boolean visible) {
        if (DEBUG) {
            Log.d(TAG, "updateFakeMenuButtonVisibility(" + visible + ")");
        }

        /*if (mSearchButton != null) {
            if (visible) {
                mSearchButton.setVisibility(View.VISIBLE);
            } else {
                mSearchButton.setVisibility(View.INVISIBLE);
            }
        }*/
        //MTK add start
        /*
        //MTK add end
        if (mMenuButton != null) {
            if (visible && !ViewConfiguration.get(this).hasPermanentMenuKey()) {
                mMenuButton.setVisibility(View.VISIBLE);
            } else {
                mMenuButton.setVisibility(View.INVISIBLE);
            }
        }
        //MTK add start
        */
        //MTK add end
    }

    /** Returns an Intent to launch Call Settings screen */
    public static Intent getCallSettingsIntent() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(PHONE_PACKAGE, CALL_SETTINGS_CLASS_NAME);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case SUBACTIVITY_ACCOUNT_FILTER: {
                AccountFilterUtil.handleAccountFilterResult(
                        mContactListFilterController, resultCode, data);
            }
            break;
        }
    }

    /* below ared added by mediatek .inc */
    private BroadcastReceiver mReceiver = new DialtactsBroadcastReceiver();
    private StatusBarManager mStatusBarMgr;

    private boolean mShowSimIndicator = false;
    private boolean mLaunched = false;
    /** M:  Bug fix for ALPS00421431@ { */
    private int mPresetPageIndex = -1;
    private static final String PAGE_INDEX = "page_index";
    /** @ } */

    private class DialtactsBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            log("DialtactsBroadcastReceiver, onReceive action = " + action);

            if (action.equals(AbstractStartSIMService.ACTION_PHB_LOAD_FINISHED)) {
                if (mDialpadFragment != null) {
                    mDialpadFragment.updateDialerSearch();
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        if (mViewPager.getCurrentItem() == TAB_INDEX_DIALER) {
            handled = mDialpadFragment.onKeyDown(keyCode, event);
        }

        if (handled) {
            return handled;
        }

        return super.onKeyDown(keyCode, event);
    }
    
    public int getCurrentFragmentId() {
        return mViewPager.getCurrentItem();
    }

    void log(String msg) {
        Log.d(TAG, msg);
    }

    // The following lines are provided and maintained by Mediatek Inc.
    private ProviderStatusWatcher mProviderStatusWatcher;
    private ProviderStatusWatcher.Status mCachedStatus;

    public DialtactsActivity() {
        mProviderStatusWatcher = ProviderStatusWatcher.getInstance(this);
    }

    @Override
    public void onProviderStatusChange() {
        ProviderStatusWatcher.Status providerStatus = mProviderStatusWatcher.getProviderStatus();
        Log.d(TAG, "[onProviderStatusChange]providerStatus:" + providerStatus.status
                + "||mDialpadFragment is null:" + (mDialpadFragment == null));
        if (mDialpadFragment != null) {
            mDialpadFragment.updateProviderStatus(mCachedStatus, providerStatus);
        }

        Log.i(TAG, "onProviderStatusChange mCallLogFragment is null:" + (null == mCallLogFragment));
        if (null != mCallLogFragment) {
            mCallLogFragment.updateProviderStauts(providerStatus);
        }

        mCachedStatus = providerStatus;
    }

    public ProviderStatusWatcher.Status getProviderStatus() {
        return mCachedStatus;
    }

    private OnMenuItemClickListener mChooseResoucesItemClickListener =
        new OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            mCallLogFragment.showChoiceResourceDialog();
            return true;
        }
    };

    private boolean isMissedCallExist() {
        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            if (null != phone && phone.getMissedCallCount() > 0) {
                return true;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to call ITelephony getMissedCallsCount()", e);
        }
        return false;
    }
    /** M:  Bug fix for ALPS00421431@ { */
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(PAGE_INDEX, mViewPager.getCurrentItem());
        super.onSaveInstanceState(outState);
    }
    /** @ } */


    public void addDialpadScrollingThreshold(boolean enabled) {
        if (!enabled) {
            mViewPager.setRectSlopScaleInTab(0, 0, 0, 0, 1.0f, 0);
            return;
        }
        int mNumButtonHeight = getResources().getDimensionPixelSize(R.dimen.button_grid_layout_button_height);
        int mDialButtonHeight = getResources().getDimensionPixelSize(R.dimen.dialpad_additional_button_height);
        int mDialpadDividerHeight = getResources().getDimensionPixelSize(R.dimen.dialpad_vertical_margin);
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(dm);
        Rect dialpadRect = new Rect();
        dialpadRect.left = 0;
        dialpadRect.top = dm.heightPixels - mDialButtonHeight - mNumButtonHeight * 4
                - mDialpadDividerHeight * 2;
        dialpadRect.right = dm.widthPixels;
        dialpadRect.bottom = dm.heightPixels - mDialButtonHeight;
        log("dialpadRect.top " + dialpadRect.top + " dialpadRect.bottom " + dialpadRect.bottom + " dialpadRect.right " + dialpadRect.right);
        mViewPager.setRectSlopScaleInTab(dialpadRect.left, dialpadRect.top, dialpadRect.right, dialpadRect.bottom, 5.0f, 0);
    }
    // The previous lines are provided and maintained by Mediatek Inc.
}
