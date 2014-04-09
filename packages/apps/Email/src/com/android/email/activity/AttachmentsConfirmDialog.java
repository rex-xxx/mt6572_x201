package com.android.email.activity;

import com.android.email.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * If the number of attachments added in UI is more than Email.ATTACHMENT_MAX_NUMBER
 * this dialog will pop up.
 *
 */
public class AttachmentsConfirmDialog extends DialogFragment
             implements DialogInterface.OnClickListener{

    // Argument bundle keys
    private static final String BUNDLE_KEY_COUNT = "NotAddMoreAttachmentsConfirmDialog.MAXCOUNT";
    public static final String TAG = "AttachmentsConfirmDialog";
    /**
     * Create the dialog with parameters
     */
    public static AttachmentsConfirmDialog newInstance(int maxCount) {
        AttachmentsConfirmDialog f = new AttachmentsConfirmDialog();
        Bundle b = new Bundle();
        b.putInt(BUNDLE_KEY_COUNT, maxCount);
        f.setArguments(b);
        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        int maxAccount = getArguments().getInt(BUNDLE_KEY_COUNT,-1);
        return new AlertDialog.Builder(context)
        .setTitle(getString(R.string.attachment_overflow))
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setMessage(getString(R.string.not_add_more_attachments,maxAccount))
        .setPositiveButton(getString(android.R.string.ok),this)
        .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
            dismissAllowingStateLoss();
            break;
        default:
            return;
        }
    }
}
