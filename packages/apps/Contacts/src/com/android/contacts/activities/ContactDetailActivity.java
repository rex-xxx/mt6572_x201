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
 * limitations under the License
 */

package com.android.contacts.activities;

import android.app.ActionBar;
import android.app.Fragment;
/** M: New Feature xxx @{ */
import android.app.StatusBarManager;
/** @} */
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
/** M: New Feature xxx @{ */
import android.graphics.drawable.Drawable;
/** @} */
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
/** M: New Feature xxx @{ */
import android.provider.Telephony.SIMInfo;
/** @} */
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
/** M: New Feature xxx @{ */
import android.view.LayoutInflater;
/** @} */
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
/** M: New Feature xxx @{ */
import android.widget.ImageView;
/** @} */
import android.widget.Toast;

import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.detail.ContactDetailDisplayUtils;
import com.android.contacts.detail.ContactDetailFragment;
import com.android.contacts.detail.ContactDetailLayoutController;
import com.android.contacts.detail.ContactLoaderFragment;
import com.android.contacts.detail.ContactLoaderFragment.ContactLoaderFragmentListener;
import com.android.contacts.interactions.ContactDeletionInteraction;
import com.android.contacts.model.Contact;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.util.PhoneCapabilityTester;

/** M: New Feature xxx @{ */
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SlotUtils;
import com.mediatek.contacts.util.SetIndicatorUtils;
import com.mediatek.phone.SIMInfoWrapper;
/** @} */

import java.util.ArrayList;
/** M: New Feature xxx @{ */
import java.util.List;
/** @} */

public class ContactDetailActivity extends ContactsActivity {
    private static final String TAG = "ContactDetailActivity";

    /** Shows a toogle button for hiding/showing updates. Don't submit with true */
    private static final boolean DEBUG_TRANSITIONS = false;

    private Contact mContactData;
    private Uri mLookupUri;

    private ContactDetailLayoutController mContactDetailLayoutController;
    private ContactLoaderFragment mLoaderFragment;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        /** M: Bug Fix for ALPS00393950 @{ */
        boolean isUsingTwoPanes = PhoneCapabilityTester.isUsingTwoPanes(this);
        if (!isUsingTwoPanes) {
            SetIndicatorUtils.getInstance().registerReceiver(this);
        }
        /** @} */
        if (PhoneCapabilityTester.isUsingTwoPanes(this)) {
            // This activity must not be shown. We have to select the contact in the
            // PeopleActivity instead ==> Create a forward intent and finish
            final Intent originalIntent = getIntent();
            Intent intent = new Intent();
            intent.setAction(originalIntent.getAction());
            intent.setDataAndType(originalIntent.getData(), originalIntent.getType());

            // If we are launched from the outside, we should create a new task, because the user
            // can freely navigate the app (this is different from phones, where only the UP button
            // kicks the user into the full app)
            if (shouldUpRecreateTask(intent)) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                        Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            } else {
                intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                        Intent.FLAG_ACTIVITY_FORWARD_RESULT | Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }

            intent.setClass(this, PeopleActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.contact_detail_activity);

        mContactDetailLayoutController = new ContactDetailLayoutController(this, savedState,
                getFragmentManager(), null, findViewById(R.id.contact_detail_container),
                mContactDetailFragmentListener);

        // We want the UP affordance but no app icon.
        // Setting HOME_AS_UP, SHOW_TITLE and clearing SHOW_HOME does the trick.
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            ///@Modify for add Customer view{
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE
                    | ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE
                    | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_CUSTOM);
            ///@}
            actionBar.setTitle("");
        }

        Log.i(TAG, getIntent().getData().toString());
        /** M: New Feature xxx @{ */
        mSimOrPhoneUri = getIntent().getData();

    }
    
    @Override
    protected void onResume() {
        super.onResume();
        /*
         * New Feature by Mediatek Begin. Original Android's code: CR ID:
         * ALPS00308657 Descriptions: RCS
         */
        Uri contactLoopupUri = getIntent().getData();
        Log.i(TAG, "contactLoopupUri : " + contactLoopupUri);

        ExtensionManager.getInstance().getContactDetailExtension().onContactDetailOpen(
                contactLoopupUri, ExtensionManager.COMMD_FOR_RCS);
    /*
     * New Feature by Mediatek End.
     */

        SetIndicatorUtils.getInstance().showIndicator(true, this);
        /*
         * Bug Fix by Mediatek Begin.
         *   Original Android's code:
         *     xxx
         *   CR ID: ALPS00273970
         *   Descriptions: 
         */
        if (sIsNeedFinish) {
            sIsNeedFinish = false;
            finish();
        }
        /*
         * Bug Fix by Mediatek End.
         */
    }     
    
    @Override
    protected void onPause() {
        super.onPause();
        
        SetIndicatorUtils.getInstance().showIndicator(false, this);
    }
    

    @Override
    public void onAttachFragment(Fragment fragment) {
         if (fragment instanceof ContactLoaderFragment) {
            mLoaderFragment = (ContactLoaderFragment) fragment;
            mLoaderFragment.setListener(mLoaderFragmentListener);
            mLoaderFragment.loadUri(getIntent().getData());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.star, menu);
        if (DEBUG_TRANSITIONS) {
            final MenuItem toggleSocial =
                    menu.add(mLoaderFragment.getLoadStreamItems() ? "less" : "more");
            toggleSocial.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            toggleSocial.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    mLoaderFragment.toggleLoadStreamItems();
                    invalidateOptionsMenu();
                    return false;
                }
            });
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem starredMenuItem = menu.findItem(R.id.menu_star);
        /*
         * Bug Fix by Mediatek Begin.
         *   Original Android's code:

        starredMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Toggle "starred" state
                // Make sure there is a contact
                if (mLookupUri != null) {
                    // Read the current starred value from the UI instead of using the last
                    // loaded state. This allows rapid tapping without writing the same
                    // value several times
                    final boolean isStarred = starredMenuItem.isChecked();

                    // To improve responsiveness, swap out the picture (and tag) in the UI already
                    ContactDetailDisplayUtils.configureStarredMenuItem(starredMenuItem,
                            mContactData.isDirectoryEntry(), mContactData.isUserProfile(),
                            !isStarred);

                    // Now perform the real save
                    Intent intent = ContactSaveService.createSetStarredIntent(
                            ContactDetailActivity.this, mLookupUri, !isStarred);
                    ContactDetailActivity.this.startService(intent);
                }
                return true;
            }
        });
        // If there is contact data, update the starred state
        if (mContactData != null) {
            ContactDetailDisplayUtils.configureStarredMenuItem(starredMenuItem,
                    mContactData.isDirectoryEntry(), mContactData.isUserProfile(),
                    mContactData.getStarred());
        }
         *   CR ID: ALPS00115684
         */
        if (this.mContactData != null && this.mContactData.getIndicate() < 0) {
        
        starredMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Toggle "starred" state
                // Make sure there is a contact
                if (mLookupUri != null) {
                    // Read the current starred value from the UI instead of using the last
                    // loaded state. This allows rapid tapping without writing the same
                    // value several times
                    final boolean isStarred = starredMenuItem.isChecked();

                    // To improve responsiveness, swap out the picture (and tag) in the UI already
                    ContactDetailDisplayUtils.configureStarredMenuItem(starredMenuItem,
                            mContactData.isDirectoryEntry(), mContactData.isUserProfile(),
                            !isStarred);

                    // Now perform the real save
                    Intent intent = ContactSaveService.createSetStarredIntent(
                            ContactDetailActivity.this, mLookupUri, !isStarred);
                    ContactDetailActivity.this.startService(intent);
                }
                return true;
            }
        });
            ContactDetailDisplayUtils.configureStarredMenuItem(starredMenuItem,
                    mContactData.isDirectoryEntry(), mContactData.isUserProfile(),
                    mContactData.getStarred());
      } else {
          Log.i(TAG,"it is sim contact");
          starredMenuItem.setVisible(false);
      }
        /*
         * Bug Fix by Mediatek End.
         */

        /*
         * New Feature by Mediatek Begin. set this if show new association menu
         */
        setAssociationMenu(menu, true);
        /*
         * New Feature by Mediatek End.
         */
        
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // First check if the {@link ContactLoaderFragment} can handle the key
        if (mLoaderFragment != null && mLoaderFragment.handleKeyDown(keyCode)) return true;

        // Otherwise find the correct fragment to handle the event
        FragmentKeyListener mCurrentFragment = mContactDetailLayoutController.getCurrentPage();
        if (mCurrentFragment != null && mCurrentFragment.handleKeyDown(keyCode)) return true;

        // In the last case, give the key event to the superclass.
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mContactDetailLayoutController != null) {
            mContactDetailLayoutController.onSaveInstanceState(outState);
        }
    }

    private final ContactLoaderFragmentListener mLoaderFragmentListener =
            new ContactLoaderFragmentListener() {
        @Override
        public void onContactNotFound() {
            finish();
        }

        @Override
        public void onDetailsLoaded(final Contact result) {
            if (result == null) {
                return;
            }
            // Since {@link FragmentTransaction}s cannot be done in the onLoadFinished() of the
            // {@link LoaderCallbacks}, then post this {@link Runnable} to the {@link Handler}
            // on the main thread to execute later.
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // If the activity is destroyed (or will be destroyed soon), don't update the UI
                    if (isFinishing()) {
                        return;
                    }
                    mContactData = result;
                    mLookupUri = result.getLookupUri();
                    invalidateOptionsMenu();
                    setupTitle();
                    mContactDetailLayoutController.setContactData(mContactData);
                    updateRCSIcon(mContactData.getId());
                }
            });
        }

        @Override
        public void onEditRequested(Uri contactLookupUri) {
            Intent intent = new Intent(Intent.ACTION_EDIT, contactLookupUri);
            intent.putExtra(
                    ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, true);
            // Don't finish the detail activity after launching the editor because when the
            // editor is done, we will still want to show the updated contact details using
            // this activity.
            startActivity(intent);
        }

        @Override
        public void onDeleteRequested(Uri contactUri) {
            // The following lines are provided and maintained by Mediatek Inc.
            if (mContactData.getIndicate() < 0) {
                // The previous lines are provided and maintained by Mediatek Inc.
                ContactDeletionInteraction.start(ContactDetailActivity.this, contactUri, true);
                // The following lines are provided and maintained by Mediatek Inc.
            } else {
                int simIndex = mContactData.getSimIndex();
                Uri simUri = SimCardUtils.SimUri.getSimUri(SIMInfo.getSlotById(
                        ContactDetailActivity.this, mContactData.getIndicate()));
                Log.d(TAG, "onDeleteRequested contact indicate = " + mContactData.getIndicate());
                Log.d(TAG, "onDeleteRequested slot id = "
                        + SIMInfo.getSlotById(ContactDetailActivity.this, mContactData
                                .getIndicate()));
                Log.d(TAG, "onDeleteRequested simUri = " + simUri);
                ContactDeletionInteraction.start(ContactDetailActivity.this, contactUri, true,
                        simUri, ("index = " + simIndex));
            }
            // The previous lines are provided and maintained by Mediatek Inc.
        }
    };

    /**
     * Setup the activity title and subtitle with contact name and company.
     */
    private void setupTitle() {
        CharSequence displayName = ContactDetailDisplayUtils.getDisplayName(this, mContactData);
        String company =  ContactDetailDisplayUtils.getCompany(this, mContactData);

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(displayName);
        actionBar.setSubtitle(company);

        if (!TextUtils.isEmpty(displayName)) {
            AccessibilityManager accessibilityManager =
                    (AccessibilityManager) this.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (accessibilityManager.isEnabled()) {
                View decorView = getWindow().getDecorView();
                decorView.setContentDescription(displayName);
                decorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
            }
        }
    }

    private final ContactDetailFragment.Listener mContactDetailFragmentListener =
            new ContactDetailFragment.Listener() {
        @Override
        public void onItemClicked(Intent intent) {
            if (intent == null) {
                return;
            }
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "No activity found for intent: " + intent);
            }
        }

        @Override
        public void onCreateRawContactRequested(
                ArrayList<ContentValues> values, AccountWithDataSet account) {
            Toast.makeText(ContactDetailActivity.this, R.string.toast_making_personal_copy,
                    Toast.LENGTH_LONG).show();
            Intent serviceIntent = ContactSaveService.createNewRawContactIntent(
                    ContactDetailActivity.this, values, account,
                    ContactDetailActivity.class, Intent.ACTION_VIEW);
            startService(serviceIntent);

        }
    };

    /**
     * This interface should be implemented by {@link Fragment}s within this
     * activity so that the activity can determine whether the currently
     * displayed view is handling the key event or not.
     */
    public interface FragmentKeyListener {
        /**
         * Returns true if the key down event will be handled by the implementing class, or false
         * otherwise.
         */
        public boolean handleKeyDown(int keyCode);
    }
    // The following lines are provided and maintained by Mediatek Inc.
    private Uri mSimOrPhoneUri;
    public StatusBarManager mStatusBarMgr;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            /** M: New Feature @{ */
            case android.R.id.home:
                // do nothing
                break;
            /*
             * if (mFinishActivityOnUpSelected) { finish(); return true; }
             * Intent intent = new Intent(this, PeopleActivity.class);
             * intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
             * startActivity(intent); finish(); return true;
             */
            /** @} */
            case R.id.menu_association_sim:
                ContactDetailFragment detailFragment = mContactDetailLayoutController
                        .getDetailFragment();
                if (detailFragment != null) {
                    detailFragment.handleAssociationSimOptionMenu();
                }
                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /*
     * New Feature by Mediatek Begin.            
     * set this if show new association menu        
     */
    public void setAssociationMenu(Menu menu, boolean fromOptionsMenu) {
        if (fromOptionsMenu) {
            MenuItem associationMenuItem = menu.findItem(R.id.menu_association_sim);
            if (associationMenuItem != null) {
                /*
                 * Bug Fix by Mediatek Begin.
                 *   Original Android's code:
                 *     if (isHasPhoneItem()) { 
                 *   CR ID: ALPS00116397
                 */
                if (SlotUtils.isGeminiEnabled() && isHasPhoneItem() && !isMe()) {
                    /*
                     * Bug Fix by Mediatek End.
                     */
                    int simInfoSize = SIMInfoWrapper.getDefault().getInsertedSimCount();

                    associationMenuItem.setVisible(!this.mContactData.isDirectoryEntry());
                    associationMenuItem.setEnabled(simInfoSize > 0);
                } else {
                    associationMenuItem.setVisible(false);
                }
            }
           
        }
    }
    /*
     * New Feature  by Mediatek End.
    */
    
    /*
     * New Feature by Mediatek Begin.            
     * get if has phone number item        
     */
    public boolean isHasPhoneItem() {
        ContactDetailFragment detailFragment = mContactDetailLayoutController.getDetailFragment();
        if (detailFragment != null && detailFragment.hasPhoneEntry(this.mContactData)) {
            return true;
        }
        return false;
    }
    /*
     * New Feature  by Mediatek End.
    */
    
    /*
     * Bug Fix by Mediatek Begin.
     *   CR ID: ALPS00116397
     */
    public boolean isMe() {
        ContactDetailFragment detailFragment = mContactDetailLayoutController.getDetailFragment();
        if (detailFragment != null) {
            return detailFragment.isMe();
        }
        return false;
    }
    /*
     * New Feature  by Mediatek End.
    */

    /*
     * Bug Fix by Mediatek Begin.
     *   Original Android's code:
     *     xxx
     *   CR ID: ALPS00273970
     *   Descriptions: 
     */       
    public static boolean sIsNeedFinish = false;

    public static void finishMyself(boolean result) {
        sIsNeedFinish = result;
    }

    /*
     * Bug Fix by Mediatek End.
     */
    /*
     * Bug Fix by Mediatek Begin.
     *   Original Android's code:
     *     xxx
     *   CR ID: ALPS00307025
     *   Descriptions: add new receiver
     */
    private boolean mShowSimIndicator = false;
    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*
         * Bug Fix by Mediatek Begin. Original Android's code: xxx CR ID:
         * ALPS00311243 Descriptions: if it's tablet not use receiver
         */
        boolean isUsingTwoPanes = PhoneCapabilityTester.isUsingTwoPanes(this);
        if (!isUsingTwoPanes) {
            SetIndicatorUtils.getInstance().unregisterReceiver(this);
        }
        
        /*
         * Bug Fix by Mediatek End.
         */
    }

    /**
     * M:update rcs-e icon
     */
    public void updateRCSIcon(long id) {
        ///@ update the rcs-e icon{
        ActionBar actionBar = getActionBar();
        Drawable drawable = ExtensionManager.getInstance().getContactDetailExtension().getRCSIcon(id);
        if (drawable != null) {
            View view = LayoutInflater.from(this).inflate(com.android.contacts.R.layout.action_bar_rcs_icon, null);
            ImageView iv = (ImageView) view.findViewById(com.android.contacts.R.id.rcs_icon);
            iv.setImageDrawable(drawable);
            actionBar.setCustomView(view);
        }
    }

    // The previous lines are provided and maintained by Mediatek Inc.
}
