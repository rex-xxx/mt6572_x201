package com.android.soundrecorder;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

/** M: use DialogFragment to show Dialog */
public class ErrorDialogFragment extends DialogFragment {
    private static final String TAG = "SR/ErrorDialogFragment";
    private static final String KEY_TITLE = "title";
    private static final String KEY_MESSAGE = "message";

    /**
     * M: create a instance of ErrorDialogFragment
     * 
     * @param titleID
     *            the resource id of title string
     * @param messageID
     *            the resource id of message string
     * @return the instance of ErrorDialogFragment
     */
    public static ErrorDialogFragment newInstance(int titleID, int messageID) {
        LogUtils.i(TAG, "<newInstance> begin");
        ErrorDialogFragment frag = new ErrorDialogFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_TITLE, titleID);
        args.putInt(KEY_MESSAGE, messageID);
        frag.setArguments(args);
        LogUtils.i(TAG, "<newInstance> end");
        return frag;
    }

    @Override
    /**
     * M: create a dialog
     */
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogUtils.i(TAG, "<onCreateDialog> begin");
        Bundle args = getArguments();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setPositiveButton(R.string.button_ok, null).setCancelable(false);
        if (args.getInt(KEY_TITLE) > 0) {
            builder.setTitle(getString(args.getInt(KEY_TITLE)));
        }
        if (args.getInt(KEY_MESSAGE) > 0) {
            builder.setMessage(getString(args.getInt(KEY_MESSAGE)));
        }
        LogUtils.i(TAG, "<onCreateDialog> end");
        Dialog res = builder.create();
        LogUtils.i(TAG, "<onCreateDialog> dialog is " + res.toString());
        return res;
    }
}