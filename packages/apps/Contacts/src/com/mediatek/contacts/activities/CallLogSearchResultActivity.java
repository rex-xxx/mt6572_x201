/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.mediatek.contacts.activities;

import android.app.ListActivity;
import android.app.SearchManager;
import android.app.StatusBarManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.contacts.CallDetailActivity;
import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.calllog.CallLogAdapter;
import com.android.contacts.calllog.CallLogQueryHandler;
import com.android.contacts.calllog.ContactInfoHelper;
import com.android.contacts.calllog.IntentProvider;
import com.android.contacts.util.Constants;
import com.android.contacts.util.EmptyLoader;

import com.mediatek.contacts.calllog.CallLogListItemView;
import com.mediatek.contacts.util.SetIndicatorUtils;

/**
 * Displays a list of call log entries.
 */
public class CallLogSearchResultActivity extends ListActivity implements
        CallLogQueryHandler.Listener, CallLogAdapter.CallFetcher {
    private static final String TAG = "CallLogSearchResultActivity";

    public StatusBarManager mStatusBarMgr;

    private CallLogQueryHandler mCallLogQueryHandler;

    private CallLogAdapter mAdapter;
    private boolean mScrollToTop;
    private boolean mEmptyLoaderRunning;
    private boolean mCallLogFetched;

    private ViewGroup mSearchResult;
    private TextView mSearchResultFor;
    private TextView mSearchResultFound;
    private TextView mEmptyView;
    private String mData;

    private static final int MENU_ITEM_DELETE_ALL = 1;
    private static final int EMPTY_LOADER_ID = 0;

    public CallLogSearchResultActivity() {

    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.call_log_google_search);

        // Typing here goes to the dialer
        // setDefaultKeyMode(DEFAULT_KEYS_DIALER);
        mCallLogQueryHandler = new CallLogQueryHandler(this.getContentResolver(), this);



        final Intent intent = this.getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            Uri uri = getIntent().getData();
            Intent newIntent = new Intent(this, CallDetailActivity.class);
            newIntent.setData(uri);
            startActivity(newIntent);
            finish();
        }

        mData = intent.getStringExtra(SearchManager.USER_QUERY);
        mSearchResult = (LinearLayout) findViewById(R.id.calllog_search_result);
        mSearchResult.setVisibility(View.VISIBLE);
        mSearchResultFor = (TextView) findViewById(R.id.calllog_search_results_for);
        mSearchResultFor.setText(Html.fromHtml(getString(R.string.search_results_for, "<b>" + mData
                + "</b>")));
        String searching = getResources().getString(R.string.search_results_searching);
        mSearchResultFound = (TextView) findViewById(R.id.calllog_search_results_found);
        mSearchResultFound.setText(searching);

        String currentCountryIso = ContactsUtils.getCurrentCountryIso(this);
        mAdapter = new CallLogAdapter(this, this, new ContactInfoHelper(this, currentCountryIso));
        setListAdapter(mAdapter);
        getListView().setItemsCanFocus(true);

    }

    @Override
    public void onResume() {
        super.onResume();

        refreshData();

        mSearchResultFor.setText(Html.fromHtml(getString(R.string.search_results_for, "<b>" + mData
                + "</b>")));

        SetIndicatorUtils.getInstance().showIndicator(true, this);
    }

//    CallLogAdapter getAdapter() {
//        return mAdapter;
//    }

    @Override
    public void onStart() {
        mScrollToTop = true;
        getLoaderManager().initLoader(EMPTY_LOADER_ID, null, new EmptyLoader.Callback(this));
        mEmptyLoaderRunning = true;
        super.onStart();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.setIntent(intent);
    }

    /** Requests updates to the data to be shown. */
    private void refreshData() {
        // Mark all entries in the contact info cache as out of date, so they
        // will be looked up
        // again once being shown.
        log("CallLogSearchResultActivity refreshData()");
        // mAdapter.invalidateCache();
        startCallsQuery();
        // Deleted by Mediatek Inc to close Google default Voicemail function.
        // startVoicemailStatusQuery();
        // updateOnEntry();
    }

    public void startCallsQuery() {
        log("CallLogSearchResultActivity startCallsQuery()");
        mAdapter.setLoading(true);
        mEmptyView = (TextView) getListView().getEmptyView();
        Intent intent = getIntent();
        mEmptyView.setText(R.string.noMatchingCalllogs);
        mData = intent.getStringExtra(SearchManager.USER_QUERY);
        Uri uri = Uri.withAppendedPath(Constants.CALLLOG_SEARCH_URI_BASE, mData);
        mCallLogQueryHandler.fetchSearchCalls(uri);
        return;
    }

    @Override
    protected void onPause() {


        SetIndicatorUtils.getInstance().showIndicator(false, this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * 
    private void configureActionBar() {
        log("configureActionBar()");
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME);
        }
    }
     */



    private void log(final String log) {
        Log.i(TAG, log);
    }

    public void onCallsDeleted() {
        // TODO Auto-generated method stub

    }

    /**
     * Called by the CallLogQueryHandler when the list of calls has been fetched
     * or updated.
     */
    public void onCallsFetched(Cursor cursor) {
        log(" CallLogSearchResultActivity onCallsFetched(), cursor = " + cursor);
        mAdapter.setLoading(false);
        mAdapter.changeCursor(cursor);
        // This will update the state of the "Clear call log" menu item.
        this.invalidateOptionsMenu();

        if (cursor == null) {
            setSearchResultCount(0);
        } else {
            setSearchResultCount(cursor.getCount());
        }

        if (mScrollToTop) {
            final ListView listView = getListView();
            if (listView.getFirstVisiblePosition() > 5) {
                listView.setSelection(5);
            }
            listView.smoothScrollToPosition(0);
            mScrollToTop = false;
        }
        mCallLogFetched = true;
        destroyEmptyLoaderIfAllDataFetched();
    }

    private void destroyEmptyLoaderIfAllDataFetched() {
        if (mCallLogFetched && mEmptyLoaderRunning) {
            mEmptyLoaderRunning = false;
            getLoaderManager().destroyLoader(EMPTY_LOADER_ID);
        }
    }

    public void onVoicemailStatusFetched(Cursor statusCursor) {
        // TODO Auto-generated method stub

    }

    public void fetchCalls() {
        // TODO Auto-generated method stub
        Intent intent = this.getIntent();
        mData = intent.getStringExtra(SearchManager.USER_QUERY);
        Uri uri = Uri.withAppendedPath(Constants.CALLLOG_SEARCH_URI_BASE, mData);
        mCallLogQueryHandler.fetchSearchCalls(uri);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ITEM_DELETE_ALL, 0, R.string.recentCalls_delete).setIcon(
                android.R.drawable.ic_menu_close_clear_cancel);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Cursor c = mAdapter.getCursor();
        boolean enable = c == null || c.getCount() <= 0;
        menu.findItem(MENU_ITEM_DELETE_ALL).setEnabled(!enable);
        menu.findItem(MENU_ITEM_DELETE_ALL).setVisible(!enable);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ITEM_DELETE_ALL:
                final Intent intent = new Intent(this, CallLogMultipleDeleteActivity.class);
                intent.putExtra(Constants.IS_GOOGLE_SEARCH, "true");
                intent.putExtra(SearchManager.USER_QUERY, mData);
                this.startActivity(intent);
                return true;
            default:
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    public void setSearchResultCount(int count) {
        String text = getQuantityText(count, R.string.listFoundAllCalllogZero,
                R.plurals.searchFoundCalllogs);
        mSearchResultFound.setText(text);
    }

    private String getQuantityText(int count, int zeroResourceId, int pluralResourceId) {
        if (count == 0) {
            return getResources().getString(zeroResourceId);
        } else {
            String format = getResources().getQuantityText(pluralResourceId, count).toString();
            return String.format(format, count);
        }
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        // TODO Auto-generated method stub
        super.onListItemClick(l, v, position, id);
        if ((null == v) || (!(v instanceof CallLogListItemView))) {
            new Exception("CallLogFragment exception").printStackTrace();
            return;
        }
        IntentProvider intentProvider = (IntentProvider) v.getTag();
        if (intentProvider != null) {
            this.startActivity(intentProvider.getIntent(this).putExtra(
                    Constants.EXTRA_FOLLOW_SIM_MANAGEMENT, true));
        }
    }
}
