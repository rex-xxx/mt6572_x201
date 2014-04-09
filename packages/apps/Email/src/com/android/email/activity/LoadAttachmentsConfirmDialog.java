package com.android.email.activity;

import com.android.email.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Confirmation dialog for loading 100+ messages.
 */
public class LoadAttachmentsConfirmDialog extends DialogFragment
        implements DialogInterface.OnClickListener {
    public static final String TAG = "LoadAttachmentsConfirmDialog";
    public static final String EXTRA_ATTACHMENT_ADD_NUMBER =  "com.email.attachment.addnumber";
    public static final String EXTRA_ATTACHMENT_URIS = "com.email.attachment.uris";
    private static Bundle sBundle;
    public interface Callback {
        public void onLoadAttachmentsConfirmDialogOkPressed(Bundle bundle);
    }

    /**
     * Create a new dialog.
     *
     * @param bundle 
     * @param callbackFragment fragment that implements {@link Callback}.  Or null, in which case
     * the parent activity must implement {@link Callback}.
     */
    public static LoadAttachmentsConfirmDialog newInstance(
            Bundle bundle,
            Fragment callbackFragment) {
        final LoadAttachmentsConfirmDialog dialog = new LoadAttachmentsConfirmDialog();
        sBundle = bundle;
        dialog.setArguments(bundle);
        if (callbackFragment != null) {
            dialog.setTargetFragment(callbackFragment, 0);
        }
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        final int addNumber = getArguments().getInt(EXTRA_ATTACHMENT_ADD_NUMBER,-1);
        final Context context = getActivity();
        final AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle(getString(R.string.attachment_overflow))
        .setIconAttribute(android.R.attr.alertDialogIcon)
        .setMessage(getString(R.string.too_many_attachments,addNumber))
        .setPositiveButton(getString(R.string.okay_action),this)
        .setNegativeButton(getString(R.string.cancel_action),null);
        return b.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                getCallback().onLoadAttachmentsConfirmDialogOkPressed(sBundle);
                dismissAllowingStateLoss();
                break;
            default:
                return;
        }
    }

    private Callback getCallback() {
        Fragment targetFragment = getTargetFragment();
        if (targetFragment != null) {
            // If a target is set, it MUST implement Callback.
            return (Callback) targetFragment;
        }
        // If not the parent activity MUST implement Callback.
        return (Callback) getActivity();
    }
}
