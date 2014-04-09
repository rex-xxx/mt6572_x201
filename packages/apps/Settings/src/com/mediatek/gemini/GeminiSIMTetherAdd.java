package com.mediatek.gemini;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony.SIMInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.R;

import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.List;

public class GeminiSIMTetherAdd extends Activity implements OnItemClickListener {
    private static final String TAG = "GeminiSIMTetherAdd";
    private static final int DIALOG_WAITING = 1001;
    private static final int DIALOG_LOADING = DIALOG_WAITING + 1;
    private static final int DIALOG_NO_CONTACT = DIALOG_WAITING + 2;
    private static final int MESSAGE_SAVE_FINISHED = 1002;
    private static boolean sIsNeedSave = false;
    private static boolean sIsSaving = false;
    private static GeminiSIMTetherAdapter sAdapter;
    private ArrayList<GeminiSIMTetherItem> mDataList = new ArrayList<GeminiSIMTetherItem>();
    private GeminiSIMTetherMamager mManager;
    private ListView mListView;
    private ContactDataAsyTask mAsyncTask;
    private volatile boolean mIsRefresh = false;
    private volatile boolean mNeedRefresh = false;
    private final Context mContext = this;
    private int mTotalConNum = 0;
    private TextView mActionBarTextView;
    private int mNumSelected;
    ///M: add for sim hot swap {
    private long mSimId;
    ///@}

    private ContentObserver mContactObserver = new ContentObserver(
            new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Xlog.d(TAG, "ContentObserver contact data changed");
            if (sIsSaving) {
                Xlog.d(TAG, "sIsSaving=" + sIsSaving);
                return;
            }
            if (mIsRefresh) {
                Xlog.d(TAG, "mIsRefresh=" + mIsRefresh);
                mNeedRefresh = true;
            } else {
                Xlog.d(TAG, "mIsRefresh=" + mIsRefresh);
                if (mAsyncTask != null) {
                    boolean isCanceld = mAsyncTask.cancel(true);
                    Xlog.d(TAG, "ContentObserver---isCanceld=" + isCanceld);
                }
                ContactDataAsyTask mySync = new ContactDataAsyTask(mContext);
                mAsyncTask = (ContactDataAsyTask) mySync.execute();
            }
        }
    };
    ///M: add for sim hot swap @{
    private BroadcastReceiver mSimReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                Xlog.d(TAG, "receive ACTION_SIM_INFO_UPDATE");
                List<SIMInfo> simList = SIMInfo.getInsertedSIMList(GeminiSIMTetherAdd.this);
                if (simList != null) {
                    if (simList.size() == 0) {
                        // Hot swap and no card so go to settings
                        Xlog.d(TAG, "Hot swap_simList.size()=" + simList.size());
                        goBackSettings();
                    } else if (simList.size() == 1) {
                        if (simList.get(0).mSimId != mSimId) {
                            finish();
                        }
                    }
                }
            }
        }
    };
    ///@}
    private void updateTitle(int num) {
        mActionBarTextView.setText(this.getString(R.string.selected_item_count,
                num));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Xlog.d(TAG,"onCreate");
        setContentView(R.layout.gemini_sim_tether_info_add);
        mManager = GeminiSIMTetherMamager.getInstance(this);

        String simDisplayName = "";
        String mCurrSIMId = mManager.getCurrSIMID();
        mSimId = Integer.parseInt(mCurrSIMId);
        SIMInfo simInfo = SIMInfo.getSIMInfoById(this, mSimId);
        int simCount = SIMInfo.getInsertedSIMCount(this);
        if (simCount > 1 && simInfo != null) {
            simDisplayName = simInfo.mDisplayName;
        }
        if (simDisplayName != null && !simDisplayName.equals("")) {
            this.setTitle(simDisplayName);
        }
        showActionBar();
        mListView = (ListView) findViewById(android.R.id.list);
        sIsNeedSave = false;
        ContactDataAsyTask mySync = new ContactDataAsyTask(this);
        mAsyncTask = (ContactDataAsyTask) mySync.execute();
        registerReceiver(mSimReceiver, new IntentFilter(TelephonyIntents.ACTION_SIM_INFO_UPDATE));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gemini_contact_menu, menu);
        menu.findItem(R.id.delete_selected).setVisible(false);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_select_all:
            setAllContactSelected(true);
            break;
        case R.id.action_unselect_all:
            setAllContactSelected(false);
            break;
        case R.id.add_contact:
            saveTetherConfigs();
            break;
        default:
            break;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onResume() {
        super.onResume();
        Xlog.d(TAG, "onResume");
        mListView.invalidateViews();
        this.getApplicationContext().getContentResolver()
                .registerContentObserver(
                        GeminiSIMTetherMamager.GEMINI_TETHER_URI, true,
                        mContactObserver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        boolean isCanceled = false;
        if (mAsyncTask != null) {
            isCanceled = mAsyncTask.cancel(true);
        }
        unregisterReceiver(mSimReceiver);
        Xlog.d(TAG, "onDestroy---isCanceled=" + isCanceled);
    }
    // M: add for hot swap
    private void goBackSettings() {
        Intent intent = new Intent(this, com.android.settings.Settings.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
    private Handler mSaveProgressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_SAVE_FINISHED) {
                Xlog.d(TAG, "tether info save finished");
                removeDialog(DIALOG_WAITING);
                sIsSaving = false;
                Xlog.d(TAG, "saveTetherConfigs(), end");
                setResult(RESULT_OK);
                finish();
            }
        }
    };

    class ContactDataAsyTask extends AsyncTask<Void, Void, Void> {
        private Context mContext;

        /*
         * Construct of AsyTask
         * 
         * @ct Context
         */
        public ContactDataAsyTask(Context ct) {
            Xlog.d(TAG, "ContactDataAsyTask constructor");
            mContext = ct;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Xlog.d(TAG, "onPreExecute");
            showDialog(DIALOG_LOADING);
        }

        @Override
        protected void onPostExecute(Void result) {
            Xlog.d(TAG, "onPostExecute");
            removeDialog(DIALOG_LOADING);
            super.onPostExecute(result);
            boolean isHaveContact = false;
            if (mDataList != null) {
                int contactSize = mDataList.size();
                if (contactSize > 0) {
                    isHaveContact = true;
                } else {
                    showDialog(DIALOG_NO_CONTACT);
                }
            }

            if (isHaveContact) {
                mListView.setVisibility(View.VISIBLE);
            } else {
                mListView.setVisibility(View.GONE);
            }
            sAdapter = new GeminiSIMTetherAdapter(mContext, mDataList, true);
            sAdapter.setShowCheckBox(true);
            if (mListView != null) {
                mListView.setAdapter(sAdapter);
                mListView
                        .setOnItemClickListener((OnItemClickListener) mContext);
            }
            mIsRefresh = false;
        }
        
        @Override
        protected void onCancelled() {
            super.onCancelled();
            Xlog.d(TAG,"ContactDataAsyTask is cancelled");
        }

        @Override
        protected Void doInBackground(Void... params) {
            mIsRefresh = true;
            do {
                mNeedRefresh = false;
                Xlog.d(TAG, "before---mNeedRefresh=" + mNeedRefresh);
                mDataList = mManager.getAllContactData();
                mTotalConNum = mManager.getTotalContactNum();
                Xlog.d(TAG, "after---mNeedRefresh=" + mNeedRefresh);
            } while (mNeedRefresh && !isCancelled());

            return null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Xlog.d(TAG, "onPause");
        this.getApplicationContext().getContentResolver()
                .unregisterContentObserver(mContactObserver);
    }

    /**
     * /** select all contact list in the adapter
     * 
     * @param checked
     *            true for check
     */
    public void setAllContactSelected(boolean checked) {
        sIsNeedSave = true;
        if (mListView != null) {
            mNumSelected = 0;
            int count = mDataList.size();
            for (int i = 0; i < count; i++) {
                mDataList
                        .get(i)
                        .setCheckedStatus(
                                checked ? GeminiSIMTetherAdapter.FLAG_CHECKBOX_STSTUS_CHECKED
                                        : GeminiSIMTetherAdapter.FLAG_CHECKBOX_STSTUS_UNCHECKED);
                if (checked) {
                    mNumSelected++;
                }
            }
            mListView.invalidateViews();
        }
        updateTitle(mNumSelected);
    }

    /**
     *Save the selection into database
     * 
     * 
     */
    private void saveTetherConfigs() {
        Xlog.d(TAG, "saveTetherConfigs(), begin");
        if (sIsNeedSave && mListView != null) {
            showDialog(DIALOG_WAITING);
            sIsSaving = true;
            new Thread() {
                @Override
                public void run() {
                    ArrayList<Integer> tetheredContactList = new ArrayList<Integer>();
                    int count = mDataList.size();
                    for (int i = 0; i < count; i++) {
                        GeminiSIMTetherItem item = (GeminiSIMTetherItem) mDataList
                                .get(i);
                        int checkedStatus = item.getCheckedStatus();
                        if (checkedStatus == GeminiSIMTetherAdapter.FLAG_CHECKBOX_STSTUS_CHECKED) {
                            int contactId = item.getContactId();
                            tetheredContactList.add(new Integer(contactId));
                        }
                    }
                    mManager.setCurrTetheredNum(tetheredContactList, false);
                    mSaveProgressHandler
                            .sendEmptyMessage(MESSAGE_SAVE_FINISHED);
                }
            }.start();
        }
    }

    private void showActionBar() {
        final ActionBar actionBar = getActionBar();
        // Inflate a custom action bar that contains the "done" button for
        // multi-choice
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customActionBarView = inflater.inflate(
                R.layout.multichoice_custom_action_bar, null);
        ImageButton doneMenuItem = (ImageButton) customActionBarView
                .findViewById(R.id.done_menu_item);
        mActionBarTextView = (TextView) customActionBarView
                .findViewById(R.id.select_items);
        mActionBarTextView.setText(this.getString(R.string.selected_item_count,
                mNumSelected));
        doneMenuItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                GeminiSIMTetherAdd.this.finish();
            }
        });

        // Show the custom action bar but hide the home icon and title
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                        | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setCustomView(customActionBarView);

    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_WAITING:
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getResources().getString(
                    R.string.gemini_tether_saving_progress_message));
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            return progressDialog;
        case DIALOG_LOADING:
            ProgressDialog progressDialog1 = new ProgressDialog(this);
            progressDialog1.setMessage(getResources().getString(
                    R.string.settings_license_activity_loading));
            progressDialog1.setIndeterminate(true);
            progressDialog1.setCancelable(false);
            return progressDialog1;
        case DIALOG_NO_CONTACT:
            int msg = mTotalConNum == 0 ? R.string.gemini_sim_tether_nocontacts
                    : R.string.gemini_sim_tether_no_assc_contacts;
            Dialog d = new AlertDialog.Builder(this).setMessage(
                    this.getString(msg)).setTitle(
                    R.string.gemini_sim_tether_revert_title).setIcon(
                    android.R.drawable.ic_dialog_alert).setPositiveButton(
                    android.R.string.ok, null)
                    .create();
            d.setOnDismissListener(new OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
            }
                
            });
            return d;
        default:
            return null;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        // mDataList.get(position).setCheckedStatus(checkBoxNewState);
        CheckBox checkBox = (CheckBox) view
                .findViewById(R.id.gemini_contact_check_btn);
        if (checkBox != null) {
            boolean isChecked = checkBox.isChecked();
            checkBox.setChecked(!isChecked);
            int checkBoxNewState;
            if (checkBox.isChecked()) {
                mNumSelected++;
                checkBoxNewState = GeminiSIMTetherAdapter.FLAG_CHECKBOX_STSTUS_CHECKED;
            } else {
                mNumSelected--;
                checkBoxNewState = GeminiSIMTetherAdapter.FLAG_CHECKBOX_STSTUS_UNCHECKED;
            }
            mDataList.get(position).setCheckedStatus(checkBoxNewState);
            sIsNeedSave = true;
            updateTitle(mNumSelected);
        }
    }
}
