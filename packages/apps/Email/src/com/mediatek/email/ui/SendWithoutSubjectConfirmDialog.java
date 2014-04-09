package com.mediatek.email.ui;

import com.android.email.R;
import com.android.email.activity.MessageCompose;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;

/**
 * M: Confirmation dialog for sending mail without subject.
 */
public class SendWithoutSubjectConfirmDialog extends DialogFragment
        implements DialogInterface.OnClickListener {
    public static final String TAG = "SendWithoutSubjectConfirmDialog";

    public interface Callback {
        public void onOkPressed();
        public void onCancelPressed();
    }

    /**
     * Create a new dialog.
     */
    public SendWithoutSubjectConfirmDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        final Resources res = context.getResources();
        final AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle(res.getString(R.string.message_is_empty_description))
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(res.getString(R.string.send_mail_without_subject_confirm))
                .setPositiveButton(R.string.send_action, this)
                .setNegativeButton(R.string.cancel_action, this);
        return b.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (getActivity() == null) {
            return;
        }
        if (which == DialogInterface.BUTTON_POSITIVE) {
            Callback callback = (Callback)getActivity();
            callback.onOkPressed();
        } else {
            Callback callback = (Callback)getActivity();
            callback.onCancelPressed();
        }
    }
}
