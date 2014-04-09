
package com.mediatek.contacts.list.service;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.android.contacts.R;

import com.mediatek.contacts.util.ContactsIntent;

/**
 * The Activity for canceling process of deleting/coping.
 */
public class MultiChoiceConfirmActivity extends Activity implements ServiceConnection {
    private static final String LOG_TAG = MultiChoiceConfirmActivity.class.getSimpleName();

    public static final String JOB_ID = "job_id";
    public static final String ACCOUNT_INFO = "account_info";

    /**
     * Type of the process to be canceled. Only used for choosing appropriate
     * title/message. Must be {@link MultiChoiceService#TYPE_DELETE} or
     * {@link MultiChoiceService#TYPE_COPY}.
     */
    public static final String TYPE = "type";

    public static final String REPORTDIALOG = "report_dialog";
    public static final String REPORT_TITLE = "report_title";
    public static final String REPORT_CONTENT = "report_content";

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ContactsIntent.MULTICHOICE.ACTION_MULTICHOICE_PROCESS_FINISH.equals(intent
                    .getAction())) {
                finish();
            }
        }
    };

    private class RequestCancelListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            bindService(new Intent(MultiChoiceConfirmActivity.this, MultiChoiceService.class),
                    MultiChoiceConfirmActivity.this, Context.BIND_AUTO_CREATE);
        }
    }

    private class CancelListener implements DialogInterface.OnClickListener,
            DialogInterface.OnCancelListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            finish();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            finish();
        }
    }

    private final CancelListener mCancelListener = new CancelListener();
    private int mJobId;
    private String mAccountInfo;
    private int mType;

    private String mReportTitle;
    private String mReportContent;
    private Boolean mReportDialog = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG, "******* onCreate savedInstanceState: " + savedInstanceState);
        /*
         * Bug Fix by Mediatek Begin.
         *   Original Android's code:
         *     xxx
         *   CR ID: ALPS00251890
         *   Descriptions: 
         */
        if (savedInstanceState != null) {
            mReportDialog = savedInstanceState.getBoolean(REPORTDIALOG, false);
            if (mReportDialog) {
                mReportTitle = savedInstanceState.getString(REPORT_TITLE);
                mReportContent = savedInstanceState.getString(REPORT_CONTENT);
            } else {
                mJobId = savedInstanceState.getInt(JOB_ID, -1);
                mAccountInfo = savedInstanceState.getString(ACCOUNT_INFO);
                mType = savedInstanceState.getInt(TYPE, 0);
            }
        }
        /*
         * Bug Fix by Mediatek End.
         */
        
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();

        mReportDialog = intent.getBooleanExtra(REPORTDIALOG, false);
        Log.i(LOG_TAG, "*******onResume mReportDialog : " + mReportDialog);
        if (mReportDialog) {
            mReportTitle = intent.getStringExtra(REPORT_TITLE);
            mReportContent = intent.getStringExtra(REPORT_CONTENT);
        } else {
            mJobId = intent.getIntExtra(JOB_ID, -1);
            mAccountInfo = intent.getStringExtra(ACCOUNT_INFO);
            mType = intent.getIntExtra(TYPE, 0);
        }

        IntentFilter itFilter = new IntentFilter();
        itFilter.addAction(ContactsIntent.MULTICHOICE.ACTION_MULTICHOICE_PROCESS_FINISH);
        registerReceiver(mIntentReceiver, itFilter);
        Log.i(LOG_TAG, "mReportTitle : " + mReportTitle + " | mReportContent : " + mReportContent);
        if (mReportDialog) {
            showDialog(R.id.multichoice_report_dialog);
        } else {
            showDialog(R.id.multichoice_confirm_dialog);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle bundle) {
        Log.i(LOG_TAG, "*******onCreateDialog id : " + id);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (id) {
            case R.id.multichoice_confirm_dialog:
                final String title;
                final String message;
                if (mType == MultiChoiceService.TYPE_DELETE) {
                    title = getString(R.string.multichoice_confirmation_title_delete);
                    message = getString(R.string.multichoice_confirmation_message_delete);
                } else {
                    title = getString(R.string.multichoice_confirmation_title_copy);
                    message = getString(R.string.multichoice_confirmation_message_copy);
                }
                builder.setTitle(title).setMessage(message).setPositiveButton(android.R.string.ok,
                        new RequestCancelListener()).setOnCancelListener(mCancelListener).setNegativeButton(
                        android.R.string.cancel, mCancelListener);
                return builder.create();
            
            case R.id.multichoice_report_dialog:
                builder.setTitle(mReportTitle).setMessage(mReportContent).setPositiveButton(android.R.string.ok,
                        mCancelListener).setOnCancelListener(mCancelListener).setNegativeButton(android.R.string.cancel,
                        mCancelListener);
                return builder.create();

            default:
                Log.w(LOG_TAG, "Unknown dialog id: " + id);
                break;
        }
        return super.onCreateDialog(id, bundle);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        MultiChoiceService service = ((MultiChoiceService.MyBinder) binder).getService();

        try {
            final MultiChoiceCancelRequest request = new MultiChoiceCancelRequest(mJobId);
            service.handleCancelRequest(request);
        } finally {
            unbindService(this);
        }

        finish();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // do nothing
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mIntentReceiver);
        super.onDestroy();
    }
    /*
     * Bug Fix by Mediatek Begin.
     *   Original Android's code:
     *     xxx
     *   CR ID: ALPS00251890
     *   Descriptions: 
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i(LOG_TAG, "*********onSaveInstanceState");
        outState.putBoolean(REPORTDIALOG, mReportDialog);
        outState.putString(REPORT_TITLE, mReportTitle);
        outState.putString(REPORT_CONTENT, mReportContent);
        outState.putInt(JOB_ID, mJobId);
        outState.putString(ACCOUNT_INFO, mAccountInfo);
        outState.putInt(TYPE, mType);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        // TODO Auto-generated method stub.
        Log.i(LOG_TAG, "onPrepareDialog********** mReportContent : " + mReportContent
                + " | mReportTitle : " + mReportTitle);
        super.onPrepareDialog(id, dialog, args);
        if (id == R.id.multichoice_report_dialog) {
            AlertDialog alertDialog = (AlertDialog) dialog;
            alertDialog.setMessage(mReportContent);
            alertDialog.setTitle(mReportTitle);
        }
    }
    /*
     * Bug Fix by Mediatek End.
     */
}
