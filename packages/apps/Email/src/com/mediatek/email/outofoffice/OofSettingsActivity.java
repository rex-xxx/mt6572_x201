package com.mediatek.email.outofoffice;

import com.android.emailcommon.Logging;
import com.android.email.Controller;
import com.android.email.R;
import com.mediatek.email.outofoffice.AccountSettingsOutOfOfficeFragment.Callback;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;

public class OofSettingsActivity extends Activity implements Callback{
    public static final String OOF_SETTING_ACTION = "android.intent.action.OOF_SETTING_ACTION";
    private static final String ACCOUNT_ID = "account_id";

    /* package */ Fragment mCurrentFragment;
    private long mAccountId;
    private ActionBar mActionBar;

    @Override
    public void onSettingFinished() {
        onBackPressed();
    }

    public static Intent createIntent(Context context, long accountId) {
        Intent i = new Intent();
        i.setAction(OOF_SETTING_ACTION);
        i.putExtra(ACCOUNT_ID, accountId);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.oof_settings_activity);

        Intent i = getIntent();
        mAccountId = i.getLongExtra(ACCOUNT_ID, -1);
        if (savedInstanceState == null && mAccountId != -1) {
            // First-time init; create fragment to embed in activity.
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment newFragment = AccountSettingsOutOfOfficeFragment.newInstance(mAccountId);
            mCurrentFragment = newFragment;
            ft.add(R.id.fragment_holder, newFragment);
            ft.commit();
        }
        mActionBar = getActionBar();
        // Configure action bar.
        mActionBar.setDisplayOptions(
                ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setTitle(R.string.account_settings_oof_label);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            // The app icon on the action bar is pressed.  Just emulate a back press.
            // TODO: this should navigate to the main screen, even if a sub-setting is open.
            // But we shouldn't just finish(), as we want to show "discard changes?" dialog
            // when necessary.
            onBackPressed();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        Controller controller = Controller.getInstance(this);
        controller.stopOof(mAccountId);
        mCurrentFragment = null;
        Button btn = (Button)AccountSettingsOutOfOfficeFragment.sSavebutton;
        if (btn.isEnabled()) {
            FragmentManager fm = getFragmentManager();
            AlertDialogFragment adf = (AlertDialogFragment)fm
                    .findFragmentByTag(AccountSettingsOutOfOfficeFragment.CANCEL_ALERT_TAG);
            if (adf == null) {
                adf = AlertDialogFragment.newInstance(
                        R.string.account_settings_oof_cancel,
                        R.string.account_settings_oof_cancel_summary);
                adf.show(getFragmentManager(),
                        AccountSettingsOutOfOfficeFragment.CANCEL_ALERT_TAG);
            }
            return;
        }
        super.onBackPressed();
    }

    public static class AlertDialogFragment extends DialogFragment {
        private static final String TITLE_ID = "titleId";
        private static final String MSG_ID = "messageId";

        private int mTitleId;
        private int mMessageId;

        public AlertDialogFragment() {
        }

        public AlertDialogFragment(int titleId, int messageId) {
            super();
            this.mTitleId = titleId;
            this.mMessageId = messageId;
        }

        public static AlertDialogFragment newInstance(int titleId,int messageId) {
            final AlertDialogFragment dialog = new AlertDialogFragment(titleId, messageId);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
            if (savedInstanceState != null) {
                mTitleId = savedInstanceState.getInt(TITLE_ID);
                mMessageId = savedInstanceState.getInt(MSG_ID);
            }
            dialogBuilder.setTitle(mTitleId)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setMessage(mMessageId)
            .setPositiveButton(R.string.okay_action,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Button btn = (Button)AccountSettingsOutOfOfficeFragment.sSavebutton;
                    btn.setEnabled(false);
                    getActivity().onBackPressed();
                }
            })
            .setNegativeButton(R.string.cancel_action,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            return alertDialog;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putInt(TITLE_ID, mTitleId);
            outState.putInt(MSG_ID, mMessageId);
        }
    }
}
