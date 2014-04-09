
package com.mediatek.contacts.list;

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;

import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.list.ContactsRequest;
import com.mediatek.contacts.activities.ContactImportExportActivity;
import com.mediatek.contacts.list.DropMenu.DropDownMenu;

/**
 * Displays a list of contacts (or phone numbers or postal addresses) for the
 * purposes of selecting multiple contacts.
 */

public class ContactListMultiChoiceActivity extends ContactsActivity implements
        View.OnCreateContextMenuListener, OnQueryTextListener, OnClickListener,
        OnCloseListener, OnFocusChangeListener {
    private static final String TAG = "ContactsMultiChoiceActivity";

    private static final int SUBACTIVITY_ADD_TO_EXISTING_CONTACT = 0;
    public static final int CONTACTGROUPLISTACTIVITY_RESULT_CODE = 1;

    private static final String KEY_ACTION_CODE = "actionCode";
    private static final int DEFAULT_DIRECTORY_RESULT_LIMIT = 20;
    
    public static final String RESTRICT_LIST = "restrictlist";

    private ContactsIntentResolverEx mIntentResolverEx;
    protected AbstractPickerFragment mListFragment;

    private int mActionCode = -1;

    private ContactsRequest mRequest;
    private SearchView mSearchView;

    // the dropdown menu with "Select all" and "Deselect all"
    private DropDownMenu mSelectionMenu;
    private boolean mIsSelectedAll = true;
    private boolean mIsSelectedNone = true;
    // if Search Mode now, decide the menu display or not. 
    private boolean mIsSearchMode = false;

    private enum SelectionMode {
        SearchMode,
        ListMode,
        GroupMode
    };

    public ContactListMultiChoiceActivity() {
        mIntentResolverEx = new ContactsIntentResolverEx(this);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof AbstractPickerFragment) {
            mListFragment = (AbstractPickerFragment) fragment;
        }
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        if (savedState != null) {
            mActionCode = savedState.getInt(KEY_ACTION_CODE);
        }

        // Extract relevant information from the intent
        mRequest = mIntentResolverEx.resolveIntent(getIntent());
        if (!mRequest.isValid()) {
            Log.d(TAG, "Request is invalid!");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        Intent redirect = mRequest.getRedirectIntent();
        if (redirect != null) {
            // Need to start a different activity
            startActivity(redirect);
            finish();
            return;
        }

        setContentView(R.layout.contact_picker);

        configureListFragment();

        // Disable Search View in listview
        SearchView searchViewInListview = (SearchView) findViewById(R.id.search_view);
        searchViewInListview.setVisibility(View.GONE);

        // Disable create new contact button
        View createNewContactButton = (View) findViewById(R.id.new_contact);
        if (createNewContactButton != null) {
            createNewContactButton.setVisibility(View.GONE);
        }

        showActionBar(SelectionMode.ListMode);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "[onDestroy]");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ACTION_CODE, mActionCode);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_multichoice, menu);

        MenuItem optionItem = menu.findItem(R.id.search_menu_item);
        optionItem.setTitle(R.string.menu_search);
        return true;
    }

    @Override
    public void onClick(View v) {
        final int resId = v.getId();

        switch (resId) {
            case R.id.search_menu_item:
                mListFragment.updateSelectedItemsView();
                showActionBar(SelectionMode.SearchMode);
                closeOptionsMenu();
                break;

            case R.id.cancel:
            case R.id.contact_home:
                setResult(RESULT_CANCELED);
                finish();
                break;

            case R.id.menu_option:
                if (mListFragment instanceof MultiContactsDuplicationFragment) {
                    Log.d(TAG, "Send result for copy action");
                    setResult(ContactImportExportActivity.RESULT_CODE);
                }
                mListFragment.onOptionAction();
                break;

            case R.id.select_items:
                View parent = (View) v.getParent();
                updateSelectionMenu(parent);
                mSelectionMenu.show();
                break;

            default:
                break;
        }
        return;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int itemId = item.getItemId();
        //if click the search menu, into the SearchMode and disable the search menu
        if (itemId == R.id.search_menu_item) {
            mListFragment.updateSelectedItemsView();
            mIsSelectedNone = mListFragment.isSelectedNone();
            showActionBar(SelectionMode.SearchMode);
            item.setVisible(false);
            return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * Creates the fragment based on the current request.
     */
    public void configureListFragment() {
        if (mActionCode == mRequest.getActionCode()) {
            return;
        }

        Bundle bundle = new Bundle();
        mActionCode = mRequest.getActionCode();
        Log.d(TAG, "configureListFragment action code is " + mActionCode);

        switch (mActionCode) {

            case ContactsRequest.ACTION_PICK_MULTIPLE_CONTACTS:
                mListFragment = new MultiContactsPickerBaseFragment();
                break;

            case ContactsRequest.ACTION_PICK_MULTIPLE_CONTACTS
                    | ContactsIntentResolverEx.MODE_MASK_VCARD_PICKER:
                mListFragment = new ContactsVCardPickerFragment();
                break;

            case ContactsRequest.ACTION_PICK_MULTIPLE_CONTACTS
                    | ContactsIntentResolverEx.MODE_MASK_IMPORT_EXPORT_PICKER:
                mListFragment = new MultiContactsDuplicationFragment();
                bundle.putParcelable(MultiContactsPickerBaseFragment.FRAGMENT_ARGS, getIntent());
                mListFragment.setArguments(bundle);
                break;

            case ContactsRequest.ACTION_PICK_MULTIPLE_EMAILS:
                mListFragment = new MultiEmailsPickerFragment();
                break;

            case ContactsRequest.ACTION_PICK_MULTIPLE_PHONES:
                mListFragment = new MultiPhoneNumbersPickerFragment();
                break;

            case ContactsRequest.ACTION_PICK_MULTIPLE_DATAS:
                mListFragment = new MultiDataItemsPickerFragment();
                bundle.putParcelable(MultiContactsPickerBaseFragment.FRAGMENT_ARGS, getIntent());
                mListFragment.setArguments(bundle);
                break;

            case ContactsRequest.ACTION_DELETE_MULTIPLE_CONTACTS:
                mListFragment = new ContactsMultiDeletionFragment();
                break;

            case ContactsRequest.ACTION_GROUP_MOVE_MULTIPLE_CONTACTS:
                mListFragment = new ContactsGroupMultiPickerFragment();
                bundle.putParcelable(MultiContactsPickerBaseFragment.FRAGMENT_ARGS, getIntent());
                mListFragment.setArguments(bundle);
                break;

            case ContactsRequest.ACTION_PICK_MULTIPLE_PHONEANDEMAILS:
                mListFragment = new MultiPhoneAndEmailsPickerFragment();
                break;

            case ContactsRequest.ACTION_SHARE_MULTIPLE_CONTACTS:
                mListFragment = new MultiContactsShareFragment();
                break;

            case ContactsRequest.ACTION_GROUP_ADD_MULTIPLE_CONTACTS:
                mListFragment = new ContactsGroupAddMultiContactsFragment();
                bundle.putParcelable(MultiContactsPickerBaseFragment.FRAGMENT_ARGS, getIntent());
                mListFragment.setArguments(bundle);
                break;

            default:
                throw new IllegalStateException("Invalid action code: " + mActionCode);
        }

        mListFragment.setLegacyCompatibilityMode(mRequest.isLegacyCompatibilityMode());
        mListFragment.setQueryString(mRequest.getQueryString(), false);
        mListFragment.setDirectoryResultLimit(DEFAULT_DIRECTORY_RESULT_LIMIT);
        mListFragment.setVisibleScrollbarEnabled(true);

        getFragmentManager().beginTransaction().replace(R.id.list_container, mListFragment)
                .commitAllowingStateLoss();
    }

    public void startActivityAndForwardResult(final Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

        // Forward extras to the new activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            intent.putExtras(extras);
        }
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mListFragment.startSearch(newText);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onClose() {
        if (mSearchView == null) {
            return false;
        }
        if (!TextUtils.isEmpty(mSearchView.getQuery())) {
            mSearchView.setQuery(null, true);
        }
        showActionBar(SelectionMode.ListMode);
        mListFragment.updateSelectedItemsView();
        return true;
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view.getId() == R.id.search_view) {
            if (hasFocus) {
                showInputMethod(mSearchView.findFocus());
            }
        }
    }

    private void showInputMethod(View view) {
        final InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (!imm.showSoftInput(view, 0)) {
                Log.w(TAG, "Failed to show soft input method.");
            }
        }
    }

    public void returnPickerResult(Uri data) {
        Intent intent = new Intent();
        intent.setData(data);
        returnPickerResult(intent);
    }

    public void returnPickerResult(Intent intent) {
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SUBACTIVITY_ADD_TO_EXISTING_CONTACT) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    startActivity(data);
                }
                finish();
            }
        }

        if (resultCode == ContactImportExportActivity.RESULT_CODE) {
            finish();
        }

        if (resultCode == CONTACTGROUPLISTACTIVITY_RESULT_CODE) {
            long[] ids = data.getLongArrayExtra("checkedids");
            if (mListFragment instanceof MultiPhoneAndEmailsPickerFragment) {
                MultiPhoneAndEmailsPickerFragment fragment = (MultiPhoneAndEmailsPickerFragment) mListFragment;
                fragment.markItemsAsSelectedForCheckedGroups(ids);
            }
        }
    }

    public void onBackPressed() {
        if (mSearchView != null && !mSearchView.isFocused()) {
            if (!TextUtils.isEmpty(mSearchView.getQuery())) {
                mSearchView.setQuery(null, true);
            }
            showActionBar(SelectionMode.ListMode);
            mListFragment.updateSelectedItemsView();
            return;
        }
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.i(TAG, "[onConfigurationChanged]" + newConfig);
        super.onConfigurationChanged(newConfig);
        // do nothing
    }

    private void showActionBar(SelectionMode mode) {
        ActionBar actionBar = getActionBar();
        switch (mode) {
            case SearchMode:
                mIsSearchMode = true;
                invalidateOptionsMenu();
                final View searchViewContainer = LayoutInflater.from(actionBar.getThemedContext())
                        .inflate(R.layout.multichoice_custom_action_bar, null);
                // in SearchMode,disable the doneMenu and selectView.
                ImageButton contactHome = (ImageButton) searchViewContainer
                        .findViewById(R.id.contact_home);
                contactHome.setVisibility(View.GONE);
                Button selectView = (Button) searchViewContainer.findViewById(R.id.select_items);
                selectView.setVisibility(View.GONE);

                mSearchView = (SearchView) searchViewContainer.findViewById(R.id.search_view);
                mSearchView.setVisibility(View.VISIBLE);
                mSearchView.setIconifiedByDefault(true);
                mSearchView.setQueryHint(getString(R.string.hint_findContacts));
                mSearchView.setIconified(false);
                mSearchView.setOnQueryTextListener(this);
                mSearchView.setOnCloseListener(this);
                mSearchView.setOnQueryTextFocusChangeListener(this);

                // when no Query String,do not display the "X"
                mSearchView.onActionViewExpanded();

                actionBar.setCustomView(searchViewContainer, new LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                actionBar.setDisplayShowCustomEnabled(true);
                actionBar.setDisplayShowHomeEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);

                //display the "CANCEL" button.
                Button cancelView = (Button) searchViewContainer
                        .findViewById(R.id.cancel);
                String cancelText = cancelView.getText().toString();
                if ("Cancel".equalsIgnoreCase(cancelText)) {
                    cancelText = cancelText.toUpperCase();
                    cancelView.setText(cancelText);
                    cancelView.setTextSize(12);
                }
                cancelView.setTypeface(Typeface.DEFAULT_BOLD);
                cancelView.setOnClickListener(this);

                //display the "OK" button.
                Button optionView = (Button) searchViewContainer.findViewById(R.id.menu_option);
                optionView.setTypeface(Typeface.DEFAULT_BOLD);
                if (mIsSelectedNone) {
                    // if there is no item selected, the "OK" button is disable.
                    optionView.setEnabled(false);
                    optionView.setTextColor(Color.LTGRAY);
                } else {
                    optionView.setEnabled(true);
                    optionView.setTextColor(Color.WHITE);
                }
                optionView.setOnClickListener(this);
                break;

            case ListMode:
                mIsSearchMode = false;
                invalidateOptionsMenu();
                // Inflate a custom action bar that contains the "done" button for multi-choice
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View customActionBarView = inflater.inflate(R.layout.multichoice_custom_action_bar,
                        null);

                ImageView homeView = (ImageView) findViewById(android.R.id.home);

                // in the listMode,disable the SearchView
                mSearchView = (SearchView) customActionBarView
                        .findViewById(R.id.search_view);
                mSearchView.setVisibility(View.GONE);

                ImageButton contactHomeView = (ImageButton) customActionBarView
                        .findViewById(R.id.contact_home);
                // set the Image same as the Image of searchView
                contactHomeView.setImageDrawable(homeView.getDrawable());
                contactHomeView.setOnClickListener(this);

                // set dropDown menu on selectItems.
                Button selectItems = (Button) customActionBarView
                        .findViewById(R.id.select_items);
                selectItems.setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.dropdown_normal_holo_dark));
                selectItems.setOnClickListener(this);

                Button cancel = (Button) customActionBarView
                        .findViewById(R.id.cancel);
                String listCancelText = cancel.getText().toString();
                if ("Cancel".equalsIgnoreCase(listCancelText)) {
                    listCancelText = listCancelText.toUpperCase();
                    cancel.setText(listCancelText);
                    cancel.setTextSize(12);
                }
                cancel.setTypeface(Typeface.DEFAULT_BOLD);
                cancel.setOnClickListener(this);

                Button menuOption = (Button) customActionBarView
                        .findViewById(R.id.menu_option);
                menuOption.setTypeface(Typeface.DEFAULT_BOLD);
                String optionText = menuOption.getText().toString();
                menuOption.setOnClickListener(this);

                // Show the custom action bar but hide the home icon and title
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                                | ActionBar.DISPLAY_SHOW_TITLE);
                actionBar.setCustomView(customActionBarView);
                // in onBackPressed() used. If mSearchView is null,return prePage.
                mSearchView = null;
                break;

            case GroupMode:
                break;

            default:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (item.getItemId() == R.id.groups) {
            startActivityForResult(new Intent(ContactListMultiChoiceActivity.this,
                    ContactGroupListActivity.class), CONTACTGROUPLISTACTIVITY_RESULT_CODE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * add dropDown menu on the selectItems.The menu is "Select all" or "Deselect all"
     * @param customActionBarView
     */
    public void updateSelectionMenu(View customActionBarView) {
        DropMenu dropMenu = new DropMenu(this);
        // new and add a menu.
        mSelectionMenu = dropMenu.addDropDownMenu((Button) customActionBarView
                .findViewById(R.id.select_items), R.menu.selection);

        Button selectView = (Button) customActionBarView
                .findViewById(R.id.select_items);
        selectView.setBackgroundDrawable(getResources().getDrawable(
                R.drawable.dropdown_normal_holo_dark));
        // when click the selectView button, display the dropDown menu.
        selectView.setOnClickListener(this);
        MenuItem item = mSelectionMenu.findItem(R.id.action_select_all);

        // get mIsSelectedAll from fragment.
        mListFragment.updateSelectedItemsView();
        mIsSelectedAll = mListFragment.isSelectedAll();
        // if select all items, the menu is "Deselect all"; else the menu is "Select all".
        if (mIsSelectedAll) {
            // dropDown menu title is "Deselect all".
            item.setTitle(R.string.menu_select_none);
            // click the menu, deselect all items
            dropMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    showActionBar(SelectionMode.ListMode);
                    // clear select all items
                    mListFragment.onClearSelect();
                    return false;
                }
            });
        } else {
            // dropDown Menu title is "Select all"
            item.setTitle(R.string.menu_select_all);
            // click the menu, select all items.
            dropMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    showActionBar(SelectionMode.ListMode);
                    // select all of itmes
                    mListFragment.onSelectAll();
                    return false;
                }
            });
        }
        return;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.search_menu_item);
        if (mIsSearchMode) {
            // if SearchMode, search Menu is disable.
            menuItem.setVisible(false);
            return false;
        } else {
            // if ListMode, search Menu is display.
            menuItem.setVisible(true);
            if (mListFragment instanceof MultiPhoneAndEmailsPickerFragment) {
                MenuItem groupsItem = menu.findItem(R.id.groups);
                groupsItem.setVisible(true);
            }
            return super.onPrepareOptionsMenu(menu);
        }
    }

}
