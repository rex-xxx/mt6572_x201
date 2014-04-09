package com.android.soundrecorder;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;

/** M: use DialogFragment to show Dialog */
public class ProgressDialogFragment extends DialogFragment {
    private static final String TAG = "SR/ProgressDialogFragment";

    /**
     * M: create a instance of ProgressDialogFragment
     * 
     * @return the instance of ProgressDialogFragment
     */
    public static ProgressDialogFragment newInstance() {
        return new ProgressDialogFragment();
    }

    @Override
    /**
     * M: create a progress dialog
     */
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogUtils.i(TAG, "<onCreateDialog>");
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setTitle(getString(R.string.delete));
        dialog.setMessage(getString(R.string.deleting));
        dialog.setCancelable(false);
        return dialog;
    }
}