/**
 * Copyright (c) 2009, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.ui;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.mms.MmsApp;
import com.android.mms.R;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;

import android.provider.Telephony;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.MessageUtils;
/// M:
import android.content.ContentUris;
import com.android.mms.util.DraftCache;
import android.provider.Telephony.Threads;
import android.util.Log;

import com.android.mms.MmsConfig;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.ipmsg.util.IpMessageUtils;

import java.io.UnsupportedEncodingException;

import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.PduPersister;
/// M: add for ipmessage
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.mediatek.mms.ipmessage.IpMessageConsts;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageServiceId;
import com.mediatek.mms.ipmessage.IpMessageConsts.RemoteActivities;
/***
 * Presents a List of search results.  Each item in the list represents a thread which
 * matches.  The item contains the contact (or phone number) as the "title" and a
 * snippet of what matches, below.  The snippet is taken from the most recent part of
 * the conversation that has a match.  Each match within the visible portion of the
 * snippet is highlighted.
 */

public class SearchActivity extends ListActivity implements DraftCache.OnDraftChangedListener {
    private AsyncQueryHandler mQueryHandler;

    // Track which TextView's show which Contact objects so that we can update
    // appropriately when the Contact gets fully loaded.
    private HashMap<Contact, TextView> mContactMap = new HashMap<Contact, TextView>();


    /*
     * Subclass of TextView which displays a snippet of text which matches the full text and
     * highlights the matches within the snippet.
     */
    private static final String WP_TAG = "Mms/WapPush";
    private static final String TAG = "Mms/SearchActivity";
    private String searchString;

    private boolean mIsQueryComplete = true;
    private static boolean sNeedRequery = false;
    // M: fix bug ALPS00351620
    private NewProgressDialog searchProgressDialog;

    public static class TextViewSnippet extends TextView {
        private static String sEllipsis = "\u2026";

        private static int sTypefaceHighlight = Typeface.BOLD;

        private String mFullText;
        private String mTargetString;
        private Pattern mPattern;

        public TextViewSnippet(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public TextViewSnippet(Context context) {
            super(context);
        }

        public TextViewSnippet(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        /**
         * We have to know our width before we can compute the snippet string.  Do that
         * here and then defer to super for whatever work is normally done.
         */
        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            String fullTextLower = mFullText.toLowerCase();
            String targetStringLower = mTargetString.toLowerCase();

            int startPos = 0;
            int searchStringLength = targetStringLower.length();
            int bodyLength = fullTextLower.length();

            Matcher m = mPattern.matcher(mFullText);
            if (m.find(0)) {
                startPos = m.start();
            }

            TextPaint tp = getPaint();

            float searchStringWidth = tp.measureText(mTargetString);
            float textFieldWidth = getWidth();
            MmsLog.d(TAG, "onLayout startPos = " + startPos + " searchStringWidth = " + searchStringWidth 
                    + " textFieldWidth = " + textFieldWidth);

            /// M: google jb.mr1 patch, Modify to take Ellipsis for avoiding JE
            /// assume we'll need one on both ends @{
            float ellipsisWidth = tp.measureText(sEllipsis);
            textFieldWidth -= (2F * ellipsisWidth);
            /// @}
            String snippetString = null;
            /// M: add "=".
            if (searchStringWidth >= textFieldWidth) {
                /// M: Code analyze 006, For fix bug ALPS00280615, The tips mms
                // has stopped show and JE happen after clicking the longer
                // search suggestion. @{
                try {
                     snippetString = mFullText.substring(startPos, startPos + searchStringLength);
                } catch (Exception e){
                     MmsLog.w(TAG, " StringIndexOutOfBoundsException ");
                     e.printStackTrace();
                     /// M: for search je.
                     snippetString = mFullText;
                }
                /// @}
            } else {
                int offset = -1;
                int start = -1;
                int end = -1;
                /* TODO: this code could be made more efficient by only measuring the additional
                 * characters as we widen the string rather than measuring the whole new
                 * string each time.
                 */
                while (true) {
                    offset += 1;

                    int newstart = Math.max(0, startPos - offset);
                    int newend = Math.min(bodyLength, startPos + searchStringLength + offset);

                    if (newstart == start && newend == end) {
                        // if we couldn't expand out any further then we're done
                        break;
                    }
                    start = newstart;
                    end = newend;

                    // pull the candidate string out of the full text rather than body
                    // because body has been toLower()'ed
                    String candidate = mFullText.substring(start, end);
                    if (tp.measureText(candidate) > textFieldWidth) {
                        // if the newly computed width would exceed our bounds then we're done
                        // do not use this "candidate"
                        break;
                    }

                    snippetString = String.format(
                            "%s%s%s",
                            start == 0 ? "" : sEllipsis,
                            candidate,
                            end == bodyLength ? "" : sEllipsis);
                }
            }
            if (snippetString == null) {
               if (textFieldWidth >= mFullText.length()) {
                   snippetString = mFullText;
               } else {
                   snippetString = mFullText.substring(0, (int)textFieldWidth);
               }
            }
            SpannableString spannable = new SpannableString(snippetString);
            int start = 0;

            m = mPattern.matcher(snippetString);
            while (m.find(start)) {
                spannable.setSpan(new StyleSpan(sTypefaceHighlight), m.start(), m.end(), 0);
                start = m.end();
            }
            setText(spannable);
            // do this after the call to setText() above
            super.onLayout(changed, left, top, right, bottom);
        }

        public void setText(String fullText, String target) {
            // Use a regular expression to locate the target string
            // within the full text.  The target string must be
            // found as a word start so we use \b which matches
            // word boundaries.
            String patternString = Pattern.quote(target);
            mPattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);

            mFullText = fullText;
            mTargetString = target;
            requestLayout();
        }
    }

    Contact.UpdateListener mContactListener = new Contact.UpdateListener() {
        public void onUpdate(Contact updated) {
            TextView tv = mContactMap.get(updated);
            if (tv != null) {
                tv.setText(updated.getNameAndNumber());
            }
        }
    };

    @Override
    public void onStop() {
        super.onStop();
        MmsLog.d(TAG,"onStop");
        Contact.removeListener(mContactListener);
    }

    private long getThreadId(long sourceId, long which) {
        Uri.Builder b = Uri.parse("content://mms-sms/messageIdToThread").buildUpon();
        b = b.appendQueryParameter("row_id", String.valueOf(sourceId));
        b = b.appendQueryParameter("table_to_use", String.valueOf(which));
        String s = b.build().toString();

        Cursor c = getContentResolver().query(
                Uri.parse(s),
                null,
                null,
                null,
                null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    return c.getLong(c.getColumnIndex("thread_id"));
                }
            } finally {
                c.close();
            }
        }
        return -1;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        MmsLog.d(TAG,"onCreate");
        /// M: Code analyze 003, For fix bug ALPS00243326, Search for different
        // words, it still display the previous result. Query new string if
        // Search activity is re-launched. @{
        sNeedRequery = true;
        searchString = getSearchString();
        /// @}

        // If we're being launched with a source_id then just go to that particular thread.
        // Work around the fact that suggestions can only launch the search activity, not some
        // arbitrary activity (such as ComposeMessageActivity).
        final Uri u = getIntent().getData();
        if (u != null && u.getQueryParameter("source_id") != null) {
            /// M: Code analyze 003, For fix bug ALPS00243326, Search for different
            // words, it still display the previous result. Query new string if
            // Search activity is re-launched. @{
            gotoComposeMessageActivity(u);
            /// @}
            return;
        }

        setContentView(R.layout.search_activity);
        ContentResolver cr = getContentResolver();

        final ListView listView = getListView();
        listView.setItemsCanFocus(true);
        listView.setFocusable(true);
        listView.setClickable(true);
        final TextView tv = (TextView)findViewById(android.R.id.empty);
        // tv.setText(getString(R.string.menu_search) + "...");
        // I considered something like "searching..." but typically it will
        // flash on the screen briefly which I found to be more distracting
        // than beneficial.
        // This gets updated when the query completes.
        setTitle("");

        Contact.addListener(mContactListener);

        // When the query completes cons up a new adapter and set our list adapter to that.
        mQueryHandler = new AsyncQueryHandler(cr) {
            protected void onQueryComplete(int token, Object cookie, Cursor c) {
                /// M: Code analyze 002, For fix bug ALPS00120575, Messages
                // does not support the storage by folder. Add the cmcc dir mode
                // code. @{
                try {
                    if (searchProgressDialog != null && searchProgressDialog.isShowing()) {
                        searchProgressDialog.setDismiss(true);
                        searchProgressDialog.dismiss();
                    }
                } catch (IllegalArgumentException ex) {
                    MmsLog.d(TAG,"Dialog.dismiss() IllegalArgumentException");
                }

                mIsQueryComplete = true;
                /// @}

                if (c == null) {
                    /// M: google JB.MR1 patch, we have to set title When cursor is null
                    setTitle(getResources().getQuantityString(
                        R.plurals.search_results_title,
                        0,
                        0,
                        searchString));
                    return;
                }
                final int threadIdPos = c.getColumnIndex("thread_id");
                final int addressPos  = c.getColumnIndex("address");
                final int bodyPos     = c.getColumnIndex("body");
                final int rowidPos    = c.getColumnIndex("_id");
                // M: fix bug ALPS00351620
                final int typeIndex   = c.getColumnIndex("index_text");
                // M: fix bug ALPS00378385
                final int charsetPos  = c.getColumnIndex("charset");
                /// M: fix bug ALPS00417470
                final int m_typePos = c.getColumnIndex("m_type");
                /// M: Code analyze 004, For fix bug ALPS00246438, the draft
                // shows abnormal in search results In folder mode, the draft
                // should be displayed in compose activity. @{
                final int msgTypePos;
                final int msgBoxPos;
                if(c.getColumnIndex("msg_type") > 0){
                    msgTypePos = c.getColumnIndex("msg_type");
                } else {
                    msgTypePos = 0;
                }
                if(c.getColumnIndex("msg_box") > 0){
                    msgBoxPos = c.getColumnIndex("msg_box");
                } else {
                    msgBoxPos = 0;
                }
                /// @}
                MmsLog.d(TAG, "onQueryComplete msgTypePos = " + msgTypePos);
                int cursorCount = c.getCount();
                MmsLog.d(TAG, "cursorCount =: " + cursorCount);
                setTitle(getResources().getQuantityString(
                        R.plurals.search_results_title,
                        cursorCount,
                        cursorCount,
                        searchString));
                if (cursorCount == 0){
                    tv.setText(getString(R.string.search_empty));
                }
                // Note that we're telling the CursorAdapter not to do auto-requeries. If we
                // want to dynamically respond to changes in the search results,
                // we'll have have to add a setOnDataSetChangedListener().
                setListAdapter(new CursorAdapter(SearchActivity.this,
                        c, false /* no auto-requery */) {
                    @Override
                    public void bindView(View view, Context context, Cursor cursor) {
                        final TextView title = (TextView)(view.findViewById(R.id.title));
                        final TextViewSnippet snippet = (TextViewSnippet)(view.findViewById(R.id.subtitle));

                        /// M: fix bug ALPS378385, use given charset to avoid invaild number @{
                        int addrCharset = cursor.getInt(charsetPos);
                        String address = cursor.getString(addressPos);
                        if (cursor.getInt(6) == 0) {
                            EncodedStringValue v = new EncodedStringValue(addrCharset, PduPersister.getBytes(address));
                            address = v.getString();
                        }
                        Contact contact = address != null ? Contact.get(address, false) : null;

                        String titleString = contact != null ? contact.getNameAndNumber() : "";
                        /// @}
                        title.setText(titleString);
                        /// M: Code analyze 005, For fix bug ALPS00278132,
                        // Search the mms which hasn't subject or text. @{
                        //if the type is mms, set the subject as title.
                        String body = cursor.getString(bodyPos);
                        if (cursor.getInt(6) == 0){
                            if (body == null || body.equals("")){
                                body = context.getString(R.string.no_subject_view);
                            } else {
                                try {
                                    body = new String(body.getBytes("ISO-8859-1"), "UTF-8");
                                } catch (UnsupportedEncodingException e){
                                     MmsLog.w(TAG, "onQueryComplete UnsupportedEncodingException");
                                     e.printStackTrace();
                                }
                            }
                        }
                        snippet.setText(body, searchString);
                        /// @}
                        // if the user touches the item then launch the compose message
                        // activity with some extra parameters to highlight the search
                        // results and scroll to the latest part of the conversation
                        // that has a match.
                        final long threadId = cursor.getLong(threadIdPos);
                        final long rowid = cursor.getLong(rowidPos);
                        final int msgType = cursor.getInt(msgTypePos);
                        final int msgBox = cursor.getInt(msgBoxPos);
                        // M: fix bug ALPS00351620
                        final int threadType = cursor.getInt(typeIndex);
                        /// M: fix bug ALPS00417470
                        final int m_type = cursor.getInt(m_typePos);

                        MmsLog.d(TAG, "onQueryComplete msgType = " + msgType + "rowid =" + rowid);

                        view.setOnClickListener(new View.OnClickListener() {
                            private void initializeClickIntent(Intent onClickIntent, int msgType, int msgBox, long rowid) {
                                if (msgType == 1) {
                                    if (msgBox == 3){//draft
                                        onClickIntent = new Intent(SearchActivity.this, ComposeMessageActivity.class);
                                    } else {
                                        final Uri SMS_URI = Uri.parse("content://sms/");
                                        onClickIntent.setClass(SearchActivity.this,FolderModeSmsViewer.class);
                                        onClickIntent.setData(ContentUris.withAppendedId(SMS_URI,rowid));
                                        onClickIntent.putExtra("msg_type", 1);
                                    }
                                } else if (msgType == 2) {
                                    if (msgBox == 3){//draft
                                          onClickIntent = new Intent(SearchActivity.this, ComposeMessageActivity.class);
                                      } else {
                                          if (msgBox == 1 && m_type == 130) {
                                              Toast.makeText(SearchActivity.this,
                                                      R.string.view_mms_notification, Toast.LENGTH_SHORT).show();
                                              return;
                                          } else {
                                              final Uri MMS_URI = Uri.parse("content://mms/");
                                              onClickIntent.setClass(SearchActivity.this,MmsPlayerActivity.class);
                                              onClickIntent.setData(ContentUris.withAppendedId(MMS_URI,rowid));
                                              onClickIntent.putExtra("dirmode", true);
                                          }
                                      }
                                } else if (msgType == 4) {
                                    final Uri CB_URI = Uri.parse("content://cb/messages/");
                                    onClickIntent.setClass(SearchActivity.this,FolderModeSmsViewer.class);
                                    onClickIntent.setData(ContentUris.withAppendedId(CB_URI,rowid));
                                    onClickIntent.putExtra("msg_type", 4);
                                }
                            }

                            public void onClick(View v) {
                                //special check for ipmessage group chat thread.
                                if (MmsConfig.getIpMessagServiceId(SearchActivity.this) == IpMessageServiceId.ISMS_SERVICE) {
                                    Conversation conv = Conversation.get(SearchActivity.this, threadId, false);
                                    String number = conv.getRecipients().get(0).getNumber();
                                    if (conv.getRecipients().size() == 1 && number.startsWith(IpMessageConsts.GROUP_START)) {
                                        MmsLog.d(TAG, "a group chat thread is clicked.threadId:" + threadId);
                                        Intent intent = new Intent(RemoteActivities.CHAT_DETAILS_BY_THREAD_ID);
                                        intent.putExtra(RemoteActivities.KEY_THREAD_ID, threadId);
                                        intent.putExtra(RemoteActivities.KEY_BOOLEAN, false);
                                        IpMessageUtils.startRemoteActivity(SearchActivity.this, intent);
                                        return;
                                    }
                                }
                                Intent onClickIntent= null;
                                if(EncapsulatedFeatureOption.MTK_WAPPUSH_SUPPORT == true){
                                    boolean dirMode = MmsConfig.getMmsDirMode();
                                    /// M: open guide if it is a guide sms.
                                    if (threadType == Threads.IP_MESSAGE_GUIDE_THREAD) {
                                        Intent it = new Intent(RemoteActivities.SERVICE_CENTER);
                                        IpMessageUtils.startRemoteActivity(SearchActivity.this, it);
                                        Conversation conv = Conversation.get(SearchActivity.this, threadId, false);
                                        if (conv != null) {
                                            conv.markAsRead();
                                        }
                                        return;
                                    }
                                    MmsLog.d(TAG, "onClickIntent1 dirMode =" + dirMode);
                                    if (!dirMode) {
                                        if(threadType == 1){
                                            MmsLog.i(WP_TAG, "SearchActivity: " + "onClickIntent WPMessageActivity.");
                                            onClickIntent = new Intent(SearchActivity.this, WPMessageActivity.class);
                                        }else{
                                            MmsLog.i(WP_TAG, "SearchActivity: " + "onClickIntent ComposeMessageActivity.");
                                            onClickIntent = new Intent(SearchActivity.this, ComposeMessageActivity.class);
                                        }
                                    } else {
                                        onClickIntent = new Intent();
                                        if (msgType == 3) {
                                            final Uri WP_URI = Uri.parse("content://wappush/");
                                            onClickIntent.setClass(SearchActivity.this,FolderModeSmsViewer.class);
                                            onClickIntent.setData(ContentUris.withAppendedId(WP_URI,rowid));
                                            onClickIntent.putExtra("msg_type", 3);
                                        } else {
                                            initializeClickIntent(onClickIntent, msgType, msgBox, rowid);
                                        }
                                    }
                            	}else{
                                    boolean dirMode = MmsConfig.getMmsDirMode();
                                    MmsLog.d(TAG, "onClickIntent2 dirMode =" + dirMode);
                                    if (!dirMode){
                                        onClickIntent = new Intent(SearchActivity.this, ComposeMessageActivity.class);
                                    } else {
                                        MmsLog.d(TAG, "onClickIntent2 msgType =" + msgType);
                                        onClickIntent = new Intent();
                                        initializeClickIntent(onClickIntent, msgType, msgBox, rowid);
                                    }
                                    /// @}
                            	}
                                /// @}
                                if (msgBox == 1 && m_type == 130) {
                                    return;
                                } else {
                                    onClickIntent.putExtra("thread_id", threadId);
                                    onClickIntent.putExtra("highlight", searchString);
                                    onClickIntent.putExtra("select_id", rowid);
                                    startActivity(onClickIntent);
                                }
                            }
                        });
                    }

                    @Override
                    public View newView(Context context, Cursor cursor, ViewGroup parent) {
                        LayoutInflater inflater = LayoutInflater.from(context);
                        View v = inflater.inflate(R.layout.search_item, parent, false);
                        return v;
                    }

                });

                // ListView seems to want to reject the setFocusable until such time
                // as the list is not empty.  Set it here and request focus.  Without
                // this the arrow keys (and trackball) fail to move the selection.
                listView.setFocusable(true);
                listView.setFocusableInTouchMode(true);
                listView.requestFocus();

                // Remember the query if there are actual results
                if (cursorCount > 0) {
                    SearchRecentSuggestions recent = ((MmsApp)getApplication()).getRecentSuggestions();
                    if (recent != null) {
                        recent.saveRecentQuery(searchString, getString(R.string.search_history, cursorCount, searchString));
                    }
                }
            }
        };


        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // The user clicked on the Messaging icon in the action bar. Take them back from
                // wherever they came from
                finish();
                return true;
        }
        return false;
    }

    /// M: Code analyze 003, For fix bug ALPS00243326, Search for different
    // words, it still display the previous result. Query new string if
    // Search activity is re-launched. @{
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);

        searchString = getSearchString();
        /** M: If we're being launched with a source_id then just go to that particular thread.
         * Work around the fact that suggestions can only launch the search activity, not some
         * arbitrary activity (such as ComposeMessageActivity).
         */
        final Uri u = getIntent().getData();
        if (u != null && u.getQueryParameter("source_id") != null) {
            gotoComposeMessageActivity(u);
            return;
        }
        // M: fix bug ALPS00351620
        sNeedRequery = true;
        super.onNewIntent(intent);
    }
    /// @}

    @Override
    public void onResume() {
        super.onResume();
        MmsLog.d(TAG, "onResume");
        // M: fix bug ALPS00351620
        if (mIsQueryComplete && sNeedRequery) {
            sNeedRequery = false;
            mIsQueryComplete = false;

            searchProgressDialog = SearchProgressDialogUtil.getProgressDialog(this);
            searchProgressDialog.show();

            boolean dirMode = MmsConfig.getMmsDirMode();
            /// M: don't pass a projection since the search uri ignores it
            Uri uri = null;
            if (!dirMode) {
                uri = Telephony.MmsSms.SEARCH_URI.buildUpon().appendQueryParameter("pattern", searchString).build();
            } else {
                uri = Uri.parse("content://mms-sms/searchFolder").buildUpon().appendQueryParameter("pattern", searchString).build();
            }
            mQueryHandler.startQuery(0, null, uri, null, null, null, null);
            DraftCache.getInstance().addOnDraftChangedListener(this);
        }

    }

    public void onDraftChanged(final long threadId, final boolean hasDraft) {
        MmsLog.d(TAG, "onDraftChanged hasDraft = " + hasDraft + " threadId = " + threadId);
        mQueryHandler.cancelOperation(0);
        mQueryHandler.postDelayed(new Runnable() {
            public void run() {
                boolean dirMode = MmsConfig.getMmsDirMode();
                // don't pass a projection since the search uri ignores it
                Uri uri = null;
                if (!dirMode) {
                    uri = Telephony.MmsSms.SEARCH_URI.buildUpon()
                            .appendQueryParameter("pattern", searchString).build();
                } else {
                    uri = Uri.parse("content://mms-sms/searchFolder").buildUpon()
                            .appendQueryParameter("pattern", searchString).build();
                }
                mQueryHandler.startQuery(0, null, uri, null, null, null, null);
            }
        }, 100);
    }

    public static void setNeedRequery() {
         sNeedRequery = true;
    }

    /// M: Code analyze 003, For fix bug ALPS00243326, Search for different
    // words, it still display the previous result. Query new string if
    // Search activity is re-launched. @{
    private void gotoComposeMessageActivity(final Uri u) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    long sourceId = Long.parseLong(u.getQueryParameter("source_id"));
                    long whichTable = Long.parseLong(u.getQueryParameter("which_table"));
                    long threadId = getThreadId(sourceId, whichTable);

                    final Intent onClickIntent = new Intent(SearchActivity.this,
                            ComposeMessageActivity.class);
                    onClickIntent.putExtra("highlight", searchString);
                    onClickIntent.putExtra("select_id", sourceId);
                    onClickIntent.putExtra("thread_id", threadId);
                    startActivity(onClickIntent);
                    finish();
                    return;
                } catch (NumberFormatException ex) {
                   MmsLog.e(TAG, "OK, we do not have a thread id so continue", ex);
                }
            }
        }, "Search thread");
        t.start();
    }

    private String getSearchString() {
        String searchStringParameter = getIntent().getStringExtra(SearchManager.QUERY);
        if (searchStringParameter == null) {
            searchStringParameter = getIntent().getStringExtra("intent_extra_data_key" /*SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA*/);
        }
        return searchStringParameter != null ? searchStringParameter.trim() : searchStringParameter;
    }
    /// @}

    protected void onPause() {
        super.onPause();
        MmsLog.d(TAG, "onPause");
        DraftCache.getInstance().removeOnDraftChangedListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        /// M: Fix ALPS00443377, query contact to update contact info and avoid data is changed in background
        CursorAdapter adapter = (CursorAdapter) getListAdapter();
        if (adapter != null) {
            Cursor c = adapter.getCursor();
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                do {
                    String address = c.getString(c.getColumnIndex("address"));
                    if (!TextUtils.isEmpty(address)) {
                        Contact contact = Contact.get(address, false);
                        if (contact != null) {
                            contact.reload();
                        }
                    }
                } while (c.moveToNext());
                setNeedRequery();
            }
        }
    }
}
