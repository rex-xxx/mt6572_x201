package com.mediatek.email.outofoffice;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.android.email.Email;
import com.android.email.R;
import com.android.email.activity.UiUtilities;


public class AutoReplyDialogFragment extends DialogFragment {
    public static final String TAG = "AutoReplyDialogFragment";
    private static final String OOF_REPLY = "oof_reply";
    private static long mAccountId;

    public static AutoReplyDialogFragment newInstance(long accountId) {
        AutoReplyDialogFragment frag = new AutoReplyDialogFragment();
        mAccountId = accountId;
        return frag;
    }

    /**
     * Use {@link #newInstance} This public constructor is still required so
     * that DialogFragment state can be automatically restored by the
     * framework.
     */
    public AutoReplyDialogFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final LayoutInflater layoutInflater = LayoutInflater.from(builder.getContext());
        final View view = layoutInflater.inflate(R.layout.auto_reply_dialog, null);
        final EditText editText = (EditText) view.findViewById(R.id.reply_label);

        builder.setTitle(R.string.account_settings_oof_auto_reply_label);
        builder.setView(view);
        editText.requestFocus();
        builder.setPositiveButton(R.string.okay_action,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int whichButton) {
                        setReplyText(editText.getText().toString().trim());
                    }
                }
            );

        builder.setNegativeButton(android.R.string.cancel, null);
        editText.setText(getReplyText());
        final AlertDialog dialog = builder.create();

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                okButton.setEnabled(true);
            }
        });
        UiUtilities.setupLengthFilter(editText, builder.getContext(),
                Email.EDITVIEW_MAX_LENGTH_3, true);
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    protected String getReplyText() {
        SharedPreferences pre = getActivity().getSharedPreferences(OOF_REPLY,
                android.content.Context.MODE_WORLD_READABLE);
        return pre.getString(OOF_REPLY + mAccountId, "");
    }

    protected void setReplyText(String replyText) {
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(OOF_REPLY,
                android.content.Context.MODE_WORLD_WRITEABLE).edit();
        editor.putString(OOF_REPLY + mAccountId, replyText);
        editor.commit();
    }
}