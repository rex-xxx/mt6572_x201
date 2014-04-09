/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.email.activity;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.email.Email;
import com.android.email.R;
import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.EmailContent.Message;

/**
 * A {@link MessageViewFragmentBase} subclass for file based messages. (aka EML files)
 */
public class MessageFileViewFragment extends MessageViewFragmentBase {
    /**
     * URI of message to open.
     */
    private Uri mFileEmailUri;

    /**
     * # of instances of this class.  When it gets 0, and the last one is not destroying for
     * a config change, we delete all the EML files.
     */
    private static int sFragmentCount;
    
    private LoadingFileMessageProgressDialog mProgressDialog;
    private OpenFileMessageCallback mCallBack;
    private MessageViewHandler mHandler;
    private static final String TAG = "MessageFileViewFragment";
    public static final int MSG_START_LOADING = 0;
    public static final int MSG_PARSE_MESSAGE = 1;
    public static final int MSG_UPDATE_DATABASE = 2;
    public static final int MSG_UPDATE_UI = 3;
    private boolean mIsLoadingFinished = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sFragmentCount++;
        mCallBack = new MessageFileViewCallback();
        mHandler =  new MessageViewHandler();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = super.onCreateView(inflater, container, savedInstanceState);

        // Actions are not available in this view.
        UiUtilities.setVisibilitySafe(result, R.id.favorite, View.GONE);
        UiUtilities.setVisibilitySafe(result, R.id.reply, View.GONE);
        UiUtilities.setVisibilitySafe(result, R.id.reply_all, View.GONE);
        UiUtilities.setVisibilitySafe(result, R.id.forward, View.GONE);
        UiUtilities.setVisibilitySafe(result, R.id.more, View.GONE);

        return result;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mFileEmailUri == null) { // sanity check.  setFileUri() must have been called.
            throw new IllegalStateException();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // If this is the last fragment of its kind, delete any/all attachment messages
        sFragmentCount--;
        if ((sFragmentCount == 0) && !getActivity().isChangingConfigurations()) {
            getController().deleteAttachmentMessages();
        }
        mCallBack = null;
        mHandler = null;
    }

    /**
     * Called by the host activity to set the URL to the EML file to open.
     * Must be called before {@link #onActivityCreated(Bundle)}.
     *
     * Note: We don't use the fragment transaction for this fragment, so we can't use
     * {@link #getArguments()} to pass arguments.
     */
    public void setFileUri(Uri fileEmailUri) {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, this + " openMessage");
        }
        if (mFileEmailUri != null) {
            throw new IllegalStateException();
        }
        if (fileEmailUri == null) {
            throw new IllegalArgumentException();
        }
        mFileEmailUri = fileEmailUri;
    }

    /**
     * NOTE See the comment on the super method.  It's called on a worker thread.
     * You can't do any UI operation in this method for it only run in background thread.
     */
    @Override
    protected Message openMessageSync(Activity activity) {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, this + " openMessageSync");
        }
        // Put up loading dialog this can take a little while...
        showLoadAttachmentProgressDialog(activity);
        Message msg = getController().loadMessageFromUri(mFileEmailUri, mCallBack);
        releaseProgressDialog();
        return msg;
    }

    @Override
    protected Message reloadMessageSync(Activity activity) {
        // EML files will never change, so just return the same copy.
        return getMessage();
    }

    /**
     * {@inheritDoc}
     *
     * Does exactly same as the super class method, but does an extra sanity check.
     */
    @Override
    protected void reloadUiFromMessage(Message message, boolean okToFetch) {
        // EML file should never be partially loaded.
        if (message.mFlagLoaded != Message.FLAG_LOADED_COMPLETE) {
            throw new IllegalStateException();
        }
        super.reloadUiFromMessage(message, okToFetch);
    }

    private void releaseProgressDialog(){
        mIsLoadingFinished = true;
        if (mProgressDialog != null) {
            mProgressDialog.dismissAllowingStateLoss();
            mProgressDialog = null;
        }
    }

    /**
     * When have added Email.ATTACHMENT_MAX_NUMBER+ attachment, 
     * show confirm dialog  
     */
    private void showLoadAttachmentProgressDialog(Activity activity){
        FragmentManager fm = getFragmentManager();
        mProgressDialog = LoadingFileMessageProgressDialog.newInstance(activity,this);
        fm.beginTransaction()
        .add(mProgressDialog,LoadingFileMessageProgressDialog.TAG)
        .commitAllowingStateLoss();
    }

    /**
     * Loading File Message Progress dialog
     */
    public static class LoadingFileMessageProgressDialog extends DialogFragment {
        public static final String TAG = "LoadingFileMessageProgressDialog";
        private static Activity sActivity = null;
        private static Fragment sFragment;
        // UI
        private String mProgressString;
        /**
         * Create a dialog for Loading attachment asynctask.
         * @param mTask Loading attachment asynctask
         */
        public static LoadingFileMessageProgressDialog newInstance(Activity activity,
                Fragment parentFragment ) {
            LoadingFileMessageProgressDialog f = new LoadingFileMessageProgressDialog();
            f.setTargetFragment(parentFragment, 0);
            sFragment = parentFragment;
            sActivity = activity;
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = sActivity;
            final MessageFileViewFragment target =
                (MessageFileViewFragment) getTargetFragment();
            ProgressDialog dialog = new ProgressDialog(context);
            dialog.setIndeterminate(true);
            dialog.setMessage(context.getString(R.string.message_view_parse_message_toast));
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            final MessageFileViewFragment target =
                (MessageFileViewFragment) getTargetFragment();
            if (!target.mIsLoadingFinished) {
                target.onCheckingDialogCancel();
            }
        }

        /**
         * Update the progress of an existing dialog
         * @param progress latest progress to be displayed
         */
        public void updateProgress(int progress) {
            ProgressDialog dialog = (ProgressDialog) getDialog();
            if (dialog != null) {
                dialog.setMessage(getProgressString(progress));
            }
        }

        /**
         * Convert progress to message
         */
        private String getProgressString(int progress) {
            int stringId = 0;
            switch (progress) {
                case MSG_START_LOADING:
                    stringId = R.string.message_view_parse_message_toast;
                    break;
                case MSG_PARSE_MESSAGE:
                    stringId = R.string.open_message_parsing;
                    break;
                case MSG_UPDATE_DATABASE:
                    stringId = R.string.open_message_update_db;
                    break;
                case MSG_UPDATE_UI:
                    stringId = R.string.open_message_update_ui;
                    break;
                default:
            }
            return sActivity.getString(stringId);
        }
        
    }

    private void reportProgress(int newState){
        FragmentManager fm = getFragmentManager();
        if (null != fm) {
            mProgressDialog = (LoadingFileMessageProgressDialog) fm
                    .findFragmentByTag(LoadingFileMessageProgressDialog.TAG);
            if (mProgressDialog == null) {
                mProgressDialog = LoadingFileMessageProgressDialog.newInstance(getActivity(), null);
                fm.beginTransaction()
                        .add(mProgressDialog, LoadingFileMessageProgressDialog.TAG)
                        .commitAllowingStateLoss();
                fm.executePendingTransactions();
            } else {
                mProgressDialog.updateProgress(newState);
            }
        } else {
            Logging.d(TAG, "reportProgress failed : " + newState);
        }
    }

    protected void onCheckingDialogCancel() {
        Logging.d(TAG, "User canceled the loading dialog ...");
        mIsLoadingFinished = true;
        finish();
    }

    public interface OpenFileMessageCallback {
        public void updateProgress(int pregress);
    }

    public class MessageFileViewCallback implements OpenFileMessageCallback{

        @Override
        public void updateProgress(int progress) {
            if (null != mHandler && !mIsLoadingFinished) {
                mHandler.updateProgress(progress);
            }else{
                Logging.d(TAG, "mHandler is null ,can not updateProgress type: " + progress);
            }
        }
        
    }

    private class MessageViewHandler extends Handler {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case MSG_START_LOADING:
            case MSG_PARSE_MESSAGE:
            case MSG_UPDATE_DATABASE:
            case MSG_UPDATE_UI:
                reportProgress(msg.what);
                break;
            default:
                super.handleMessage(msg);
                break;
            }
        }
        public void updateProgress(int progress) {
            Logging.d(TAG, "updateProgress progreee : " + progress);
            android.os.Message msg = android.os.Message.obtain();
            msg.what = progress;
            sendMessage(msg);
        }
    }

    /**
     * Release the progress dialog and call the super method to release this fragment.
     * It will close the current fragment and go back to parent fragment, e.g. MessageViewFragment.
     */
    private void finish() {
        releaseProgressDialog();
        super.getCallback().onMessageNotExists();
    }
}
