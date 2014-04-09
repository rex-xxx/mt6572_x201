package com.mediatek.beam;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.mediatek.beam.BeamShareTask.BeamShareTaskMetaData;
import com.mediatek.xlog.Xlog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BeamShareHistory extends SettingsPreferenceFragment implements
        DialogInterface.OnClickListener, Callback {

    private static final String TAG = "BeamShareHistory";

    private LayoutInflater mInflater;

    private Handler mHandler;
    private Cursor mCursor = null;

    private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

    private static final String OUTGOING_TAB_TAG = "Outgoing";
    private static final String INCOMING_TAB_TAG = "Incoming";

    private static final int UPDATE_MENU_MSG = 1;

    private static final int FILE_NOT_SUPPORTED_DIALOG_ID = 1;
    private static final int FILE_RESEND_DIALOG_ID = 2;
    private static final int CLEAR_ALL_CONFIRM_DIALOG_ID = 3;
    private static final int FILE_RECEIVE_FAIL_DIALOG_ID = 4;
    private static final int CLEAR_ITEM_CONFIRM_DIALOG_ID = 5;

    private static final int MENU_ID_CLEAR = 0;

    private int mCurrentDialogId = -1;
    private String mCilckedFileName;
    private int mCurrentTabIndex = 0;
    private ContentResolver mContentReslover;
    private Activity mActivity;
    private BeamShareTabAdapter mAdapter;
    private Uri mLongClickUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Xlog.d(TAG, "onCreate()");
        mActivity = getActivity();
        if (mActivity instanceof PreferenceActivity) {
            mActivity.getActionBar()
                    .setTitle(R.string.beam_share_history_title);
        }

        mContentReslover = mActivity.getContentResolver();

        TabInfo tab = new TabInfo(this,
                mActivity.getString(R.string.beam_mgmt_tab_download_title),
                true);
        mTabs.add(tab);
        tab = new TabInfo(this,
                mActivity.getString(R.string.beam_mgmt_tab_upload_title), false);
        mTabs.add(tab);

        // create handler
        if (mHandler == null) {
            mHandler = new Handler(this);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        int size = mTabs.size();
        for (int i = 0; i < size; i++) {
            Cursor cursor = mTabs.get(i).getCursor();
            if(cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        super.onDestroy();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mActivity.invalidateOptionsMenu();
        getContentResolver().registerContentObserver(BeamShareTaskMetaData.CONTENT_URI, false, mSettingsObserver);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        Xlog.d(TAG, "onPause");
        getContentResolver().unregisterContentObserver(mSettingsObserver);
        Xlog.d(TAG, "unregister content obsever");
    }

    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Xlog.d(TAG, "ContentObserver, onChange()");
            mActivity.invalidateOptionsMenu();
        }
    };
    
    public boolean handleMessage(Message msg) {

        Xlog.d(TAG, "handleMessage: " + msg.what);
        if (msg.what == UPDATE_MENU_MSG) {
            mActivity.invalidateOptionsMenu();
            return true;
        } else {
            return false;
        }

        /*
         * switch (msg.what) { case UPDATE_MENU_MSG:
         * 
         * // update share task /*ContentValues values = new ContentValues();
         * values.put(BeamShareTaskMetaData.TASK_TYPE, 0);
         * values.put(BeamShareTaskMetaData.TASK_STATE, 0);
         * values.put(BeamShareTaskMetaData.TASK_OBJECT_FILE, "abcd.png");
         * values.put(BeamShareTaskMetaData.TASK_MIMETYPE, "image/*");
         * values.put(BeamShareTaskMetaData.TASK_PEER_ADDR, "123456");
         * values.put(BeamShareTaskMetaData.TASK_PEER_NAME, "Sanguang");
         * values.put(BeamShareTaskMetaData.TASK_DONE_BYTES, 0);
         * values.put(BeamShareTaskMetaData.TASK_TOTAL_BYTES, 0);
         * values.put(BeamShareTaskMetaData.TASK_MODIFIED_DATE,
         * System.currentTimeMillis());
         * 
         * mContentReslover.insert(BeamShareTaskMetaData.CONTENT_URI, values);
         */
        // mContentReslover.delete((Uri) msg.obj, null, null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // initialize the inflater
        Xlog.d(TAG, "onCreateView");

        mInflater = inflater;

        View rootView = mInflater.inflate(R.layout.beam_share_mgmt, container,
                false);
        TabHost tabHost = (TabHost) (rootView
                .findViewById(android.R.id.tabhost));
        tabHost.setup();
        tabHost.setOnTabChangedListener(mTabListener);

        int size = mTabs.size();
        for (int i = 0; i < size; i++) {
            mTabs.get(i).build(mInflater, tabHost);
        }

        mCursor = mTabs.get(mCurrentTabIndex).getCursor();
        getActivity().invalidateOptionsMenu();

        return rootView;
    }

    private OnTabChangeListener mTabListener = new OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            Xlog.d(TAG, "OnTabChanged");
            if (tabId.equals(INCOMING_TAB_TAG)) {
                mCurrentTabIndex = 0;
            } else if (tabId.equals(OUTGOING_TAB_TAG)) {
                mCurrentTabIndex = 1;
            }
            Xlog.d(TAG, "mCurrentTabIndex" + mCurrentTabIndex);

            mCursor = mTabs.get(mCurrentTabIndex).getCursor();
            mActivity.invalidateOptionsMenu();
        }
    };

    @Override
    public Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        mCurrentDialogId = id;
        switch (id) {
        case FILE_NOT_SUPPORTED_DIALOG_ID:
            dialog = new AlertDialog.Builder(mActivity)
                    .setTitle(R.string.beam_share_open_file_dialog_title)
                    .setMessage(
                            getString(R.string.beam_share_open_file_unsupported_message))
                    .setPositiveButton(android.R.string.yes, null).create();
            break;
        case FILE_RESEND_DIALOG_ID:
            dialog = new AlertDialog.Builder(mActivity)
                    .setTitle(R.string.beam_share_transfer_fail_dialog_title)
                    .setMessage(
                            getString(
                                    R.string.beam_share_resend_dialog_message,
                                    mCilckedFileName))
                    .setPositiveButton(android.R.string.yes, null).create();
            break;
        case CLEAR_ALL_CONFIRM_DIALOG_ID:
            dialog = new AlertDialog.Builder(mActivity)
                    .setTitle(R.string.beam_share_clear_confirm_dialog_title)
                    .setMessage(
                            getString(R.string.beam_share_clear_confirm_dialog_message))
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.no, null).create();
            break;
        case FILE_RECEIVE_FAIL_DIALOG_ID:
            dialog = new AlertDialog.Builder(mActivity)
                    .setTitle(R.string.beam_share_transfer_fail_dialog_title)
                    .setMessage(
                            getString(
                                    R.string.beam_share_receive_fail_dialog_message,
                                    mCilckedFileName))
                    .setPositiveButton(android.R.string.ok, null).create();
            break;
        case CLEAR_ITEM_CONFIRM_DIALOG_ID:
            dialog = new AlertDialog.Builder(mActivity)
                    .setTitle(R.string.beam_share_clear_item_dailog_title)
                    .setIcon(com.android.internal.R.drawable.ic_dialog_alert)
                    .setMessage(
                            getString(R.string.beam_share_clear_item_dailog_message))
                    .setPositiveButton(
                            R.string.beam_share_clear_item_dailog_title, this)
                    .setNegativeButton(android.R.string.no, null).create();
            break;
        default:
            break;
        }
        return dialog;
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button != DialogInterface.BUTTON_POSITIVE) {
            Xlog.d(TAG, "DialogInterface onClick return");
            return;
        }
        if (mCurrentDialogId == CLEAR_ALL_CONFIRM_DIALOG_ID) {
            // new a async task to clear the db
            new ClearTask().execute();
        } else if (mCurrentDialogId == CLEAR_ITEM_CONFIRM_DIALOG_ID) {
            Xlog.d(TAG, "Long click uri: " + mLongClickUri);
            mContentReslover.delete(mLongClickUri, null, null);
            mHandler.sendEmptyMessage(UPDATE_MENU_MSG);
        }
    }

    public class TabInfo implements OnItemClickListener,
            OnItemLongClickListener {
        public final BeamShareHistory mOwner;
        public View mRootView;
        public final CharSequence mLabel;
        private ListView mListView;
        private Cursor mTabCursor = null;
        private boolean mIsIncoming;

        public TabInfo(BeamShareHistory owner, CharSequence label,
                boolean isIncoming) {
            mOwner = owner;
            mLabel = label;
            mIsIncoming = isIncoming;
        }

        public BeamShareHistory getOwner() {
            return mOwner;
        }

        public void build(LayoutInflater inflater, TabHost tabHost) {
            Xlog.d(TAG, "build");
            mInflater = inflater;
            String tabTag = mIsIncoming ? INCOMING_TAB_TAG : OUTGOING_TAB_TAG;
            TabHost.TabSpec tabSpec = tabHost.newTabSpec(tabTag)
                    .setIndicator(mLabel).setContent(mEmptyTabContent);
            tabHost.addTab(tabSpec);
        }

        private TabContentFactory mEmptyTabContent = new TabContentFactory() {
            @Override
            public View createTabContent(String tag) {
                return buildTabSpec();
            }
        };

        private View buildTabSpec() {
            if (mRootView != null) {
                return mRootView;
            }

            mRootView = mInflater.inflate(R.layout.beam_share_mgmt_tab, null);
            mListView = (ListView) mRootView.findViewById(R.id.listView);

            try {
                mTabCursor = mOwner.mActivity.getContentResolver().query(
                        BeamShareTaskMetaData.CONTENT_URI,
                        null,
                        mIsIncoming ? BeamShareTask.SC_INCOMING_TASK
                                : BeamShareTask.SC_OUTGOING_TASK, null,
                        BeamShareTaskMetaData._ID + " DESC");
            } catch (CursorIndexOutOfBoundsException ex) {
                Xlog.e(TAG, "check empty share list error:", ex);
            }

            // create list adapter
            if (mTabCursor != null) {
                Xlog.d(TAG, "tab " + mLabel + ": cursor.getCount() "
                                + mTabCursor.getCount());
                mAdapter = new BeamShareTabAdapter(mOwner.mActivity,
                        R.layout.beam_share_mgmt_item, mTabCursor);
                mListView.setAdapter(mAdapter);
                mListView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
                mListView.setOnItemClickListener(this);
                mListView.setOnItemLongClickListener(this);
            }
            return mRootView;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            // find the item that is clicked
            mTabCursor.moveToPosition(position);

            final BeamShareTask task = new BeamShareTask(mTabCursor);

            String fileData = task.getData();
            if (fileData == null) {
                Xlog.d(TAG, "the file name is null");
                mCilckedFileName = fileData;
            } else {
                // resource exists
                File file = new File(fileData);
                mCilckedFileName = file.getName();
            }

            if (task.getState() == BeamShareTask.STATE_SUCCESS) {
                openTransferSuccessFile(task.getData(), task.getMimeType());
            } else {
                if (task.getDirection() == BeamShareTask.Direction.out) {
                    // show resend dialog
                    mOwner.showDialog(FILE_RESEND_DIALOG_ID);
                } else {
                    // show file not received dialog
                    mOwner.showDialog(FILE_RECEIVE_FAIL_DIALOG_ID);
                }
            }
        }

        public boolean onItemLongClick(AdapterView<?> parent, View view,
                int position, long id) {
            // find the item that is clicked
            Xlog.d(TAG, "onItemLongClick");
            mTabCursor.moveToPosition(position);

            final BeamShareTask task = new BeamShareTask(mTabCursor);
            mLongClickUri = task.getTaskUri();
            Xlog.d(TAG, "Click uri: " + mLongClickUri);

            showDialog(CLEAR_ITEM_CONFIRM_DIALOG_ID);
            return false;
        }

        private void openTransferSuccessFile(String filename, String mimeType) {

            Xlog.d(TAG, "openTransferSuccessFile(): filename=" + filename
                    + " mimetype=" + mimeType);

            if (filename == null) {
                Xlog.d(TAG, "the file name is null");
                return;
            }

            // resource exists
            File file = new File(filename);
            String name = file.getName();
            Xlog.d(TAG, "file name is " + name);

            // supported data type
            Uri path = Uri.parse(filename);
            path = (path.getScheme() == null) ? Uri.fromFile(file) : path;
            if (!isSupportedDataType(path, mimeType)) {
                mOwner.showDialog(FILE_NOT_SUPPORTED_DIALOG_ID);
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(path, mimeType);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mOwner.mActivity.startActivity(intent);
            }
        }

        /**
         * check if the data type is supported by system
         * 
         * @param context
         * @param data
         * @param mimeType
         * @return
         */
        private boolean isSupportedDataType(Uri data, String mimeType) {

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(data, mimeType);
            List<ResolveInfo> list = mOwner.mActivity.getPackageManager()
                    .queryIntentActivities(intent,
                            PackageManager.MATCH_DEFAULT_ONLY);
            if (list.size() == 0) {

                Xlog.d(TAG,
                        "cannot find proper Activity to handle Intent: mime["
                                + mimeType + "], data[" + data + "]");
                return false;
            }
            return true;
        }

        public Cursor getCursor() {
            return mTabCursor;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_ID_CLEAR, 0, R.string.beam_share_mgmt_tab_menu_clear)
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM
                                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    // disable options menu (clear) according to the item count
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Xlog.d(TAG, "onPrepareOptionsMenu");

        if (mCursor != null) {
            boolean menuEnabled = mCursor.getCount() > 0;
            Xlog.d(TAG, "Menu enabled status is " + mCursor.getCount());
            menu.findItem(MENU_ID_CLEAR).setEnabled(menuEnabled);
        }
    }

    // implement menu action: "clear"
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_ID_CLEAR) {
            showDialog(CLEAR_ALL_CONFIRM_DIALOG_ID);
            return true;
        }
        return false;
    }

    private class ClearTask extends AsyncTask<String, Void, Integer> {
        private static final int CLEAR_SUCCESS = 0;
        private static final int CLEAR_ONGOING = 1;

        /**
         * call frmework to reset the profile
         * 
         * @param arg
         * @return the reset result
         */
        @Override
        protected Integer doInBackground(String... arg) {
            int result = CLEAR_ONGOING;
            clearAllTasks();
            result = CLEAR_SUCCESS;
            return result;
        }

        /**
         * clear all tasks in the current list
         */
        private void clearAllTasks() {

            int columnIndex = mCursor
                    .getColumnIndexOrThrow(BeamShareTaskMetaData._ID);
            ArrayList<Uri> uris = new ArrayList<Uri>();
            for (mCursor.moveToFirst(); !mCursor.isAfterLast(); mCursor
                    .moveToNext()) {
                // compose Uri for the task and clear it
                int id = mCursor.getInt(columnIndex);
                Uri uri = Uri
                        .withAppendedPath(BeamShareTaskMetaData.CONTENT_URI,
                                Integer.toString(id));
                uris.add(uri);
                Xlog.d(TAG,
                        "clearAllTasks-----mCursor.getCount(): "
                                + mCursor.getCount());
            }

            for (Uri uri : uris) {
                mContentReslover.delete(uri, null, null);
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == CLEAR_SUCCESS) {
                mHandler.sendEmptyMessage(UPDATE_MENU_MSG);
            }
        }
    }
}
