package com.android.browser;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class DataConnectionDialog extends AlertActivity implements
        DialogInterface.OnClickListener {

    public static final String PREF_NOT_REMIND = "pref_not_remind";
    private CheckBox mCb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the "dialog"
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = android.R.drawable.ic_dialog_alert;
        p.mTitle = getString(R.string.dialog_title);
        p.mView = createView();
        p.mPositiveButtonText = getString(android.R.string.ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(android.R.string.cancel);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    private View createView() {
        View view = getLayoutInflater().inflate(R.layout.confirm_dialog, null);
        TextView contentView = (TextView)view.findViewById(R.id.content);
        contentView.setText(getString(R.string.wifi_failover_gprs_content));
        mCb = (CheckBox)view.findViewById(R.id.setPrimary);
        return view;
    }

    public void onClick(DialogInterface dialog, int which) {
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:

                if (cm != null) {
                    cm.setMobileDataEnabled(true);
                }
                
                if (mCb.isChecked()) {
                    SharedPreferences sh = this.getSharedPreferences("data_connection", this.MODE_WORLD_READABLE);
                    Editor editor = sh.edit();
                    editor.putBoolean(PREF_NOT_REMIND, true);
                    editor.commit();
                }
                
                finish();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                if (cm != null) {
                    cm.setMobileDataEnabled(false);
                }
                if (mCb.isChecked()) {
                    SharedPreferences sh = this.getSharedPreferences("data_connection", this.MODE_WORLD_READABLE);
                    Editor editor = sh.edit();
                    editor.putBoolean(PREF_NOT_REMIND, true);
                    editor.commit();
                }

                finish();
                break;
            default:
                /// do nothing.
        }
    }
}
