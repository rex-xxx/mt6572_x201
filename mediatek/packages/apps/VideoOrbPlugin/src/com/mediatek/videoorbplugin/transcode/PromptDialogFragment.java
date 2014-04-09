package com.mediatek.videoorbplugin.transcode;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import com.mediatek.videoorbplugin.R;

public class PromptDialogFragment extends DialogFragment {
    static final String KEY_TITLE = "title";
    static final String KEY_PROMPT = "prompt";
    private DialogInterface.OnClickListener mListener;

    public static PromptDialogFragment newInstance(
            int idTitle, int idPrompt, DialogInterface.OnClickListener listener) {
        PromptDialogFragment frag = new PromptDialogFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_TITLE, idTitle);
        args.putInt(KEY_PROMPT, idPrompt);
        frag.setArguments(args);
        frag.setOnClickListener(listener);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setTitle(getArguments().getInt(KEY_TITLE));
        builder.setMessage(getString(getArguments().getInt(KEY_PROMPT)));
        builder.setPositiveButton(R.string.ok, mListener);
        builder.setNegativeButton(R.string.cancel, mListener);
        Dialog dlg = builder.create();
        dlg.setCanceledOnTouchOutside(false);
        dlg.setCancelable(false);
        return dlg;
    }

    public void setOnClickListener(DialogInterface.OnClickListener listener) {
        mListener = listener;
    }
}