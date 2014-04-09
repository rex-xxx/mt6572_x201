package com.android.settings.bluetooth;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.android.settings.R;

public final class ErrorDialogFragment extends DialogFragment
        implements DialogInterface.OnClickListener {

    private static final String KEY_ERROR = "errorMessage";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String message = getArguments().getString(KEY_ERROR);

        return new  AlertDialog.Builder(getActivity())
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.bluetooth_error_title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, null)
        .show();
    }

    public void onClick(DialogInterface dialog, int which) {
        // TODO Auto-generated method stub
        
    }
}
