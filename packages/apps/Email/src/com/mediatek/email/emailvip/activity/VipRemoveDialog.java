package com.mediatek.email.emailvip.activity;

import com.android.email.R;
import com.android.emailcommon.utility.EmailAsyncTask;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

public class VipRemoveDialog extends DialogFragment {
    ///M: Add TAG for VipRemoveDialog. @{
    @SuppressWarnings("hiding")
    public static final String TAG = "VipRemoveDialog";
    /// @}
    private static String sVipAddress;
    private static EmailAsyncTask<Void, Void, Void> sRemoveTask;

    public static VipRemoveDialog newInstance(EmailAsyncTask<Void, Void, Void> removeTask,
            String address) {
        VipRemoveDialog frag = new VipRemoveDialog();
        sRemoveTask = removeTask;
        sVipAddress= address;
        return frag;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String message = String.format(
                this.getString(R.string.vip_remove_prompt), sVipAddress);
        return new AlertDialog.Builder(getActivity())
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(R.string.vip_remove_title)
                .setMessage(message)
                .setPositiveButton(getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                if (sRemoveTask != null) {
                                    sRemoveTask.executeSerial();
                                }
                            }
                        })
                .setNegativeButton(getString(R.string.cancel_action), null)
                .create();
    }
}