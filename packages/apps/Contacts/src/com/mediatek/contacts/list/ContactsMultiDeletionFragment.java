
package com.mediatek.contacts.list;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.android.contacts.R;

import com.mediatek.contacts.list.MultiContactsBasePickerAdapter.PickListItemCache;
import com.mediatek.contacts.list.MultiContactsBasePickerAdapter.PickListItemCache.PickListItemData;
import com.mediatek.contacts.list.service.MultiChoiceHandlerListener;
import com.mediatek.contacts.list.service.MultiChoiceRequest;
import com.mediatek.contacts.list.service.MultiChoiceService;

import java.util.ArrayList;
import java.util.List;

public class ContactsMultiDeletionFragment extends MultiContactsPickerBaseFragment {

    public static final String TAG = "ContactsMultiDeletion";
    public static final boolean DEBUG = true;

    private SendRequestHandler mRequestHandler;
    private HandlerThread mHandlerThread;

    private DeleteRequestConnection mConnection;

    private int mRetryCount = 20;

    @Override
    public CursorLoader createCursorLoader() {
        return new CursorLoader(getContext(), null, null, null, null, null);
    }

    @Override
    public void onOptionAction() {

        if (getCheckedItemIds().length == 0) {
            Toast.makeText(this.getContext(), R.string.multichoice_no_select_alert,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ConfirmDialog cDialog = new ConfirmDialog();
        cDialog.setTargetFragment(this, 0);
        cDialog.setArguments(this.getArguments());
        cDialog.show(this.getFragmentManager(), "cDialog");
    }

    public static class ConfirmDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity()).setTitle(
                    R.string.multichoice_delete_confirm_title).setIconAttribute(
                    android.R.attr.alertDialogIcon).setMessage(
                    R.string.multichoice_delete_confirm_message).setNegativeButton(
                    android.R.string.cancel, null).setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            final ContactsMultiDeletionFragment target = (ContactsMultiDeletionFragment) getTargetFragment();
                            if (target != null) {
                                target.handleDelete();
                            }
                        }
                    });
            return builder.create();

        }
    }

    private void handleDelete() {
        if (mConnection != null) {
            return ;
        }
        startDeleteService();

        if (mHandlerThread == null) {
            mHandlerThread = new HandlerThread(TAG);
            mHandlerThread.start();
            mRequestHandler = new SendRequestHandler(mHandlerThread.getLooper());
        }

        List<MultiChoiceRequest> requests = new ArrayList<MultiChoiceRequest>();

        final MultiContactsBasePickerAdapter adapter = (MultiContactsBasePickerAdapter) getAdapter();
        final PickListItemCache listItemCacher = adapter.getListItemCache();
        final long[] checkedIds = getCheckedItemIds();
        for (long id : checkedIds) {
            PickListItemData item = listItemCacher.getItemData(id);
            requests.add(new MultiChoiceRequest(item.contactIndicator, item.simIndex, (int) id, item.displayName));
        }

        /*
         * Bug Fix by Mediatek Begin.
         * 
         * CR ID: ALPS00233127
         */
        if (requests.size() > 0) {
            mRequestHandler.sendMessage(mRequestHandler.obtainMessage(SendRequestHandler.MSG_REQUEST, requests));
        } else {
            mRequestHandler.sendMessage(mRequestHandler.obtainMessage(SendRequestHandler.MSG_END));
        }
        /*
         * Bug Fix by Mediatek End.
         */
    }

    private class DeleteRequestConnection implements ServiceConnection {
        private MultiChoiceService mService;

        public boolean sendDeleteRequest(final List<MultiChoiceRequest> requests) {
            Log.d(TAG, "Send an delete request");
            if (mService == null) {
                Log.i(TAG, "mService is not ready");
                return false;
            }
            mService.handleDeleteRequest(requests, new MultiChoiceHandlerListener(mService));
            return true;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(TAG, "onServiceConnected");
            mService = ((MultiChoiceService.MyBinder) binder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Disconnected from MultiChoiceService");
        }
    }

    private class SendRequestHandler extends Handler {

        public static final int MSG_REQUEST = 100;
        public static final int MSG_END = 200;

        public SendRequestHandler(Looper looper) {
            super(looper);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_REQUEST) {
                if (!mConnection.sendDeleteRequest((List<MultiChoiceRequest>) msg.obj)) {
                    if (mRetryCount-- > 0) {
                        sendMessageDelayed(obtainMessage(msg.what, msg.obj), 500);
                    } else {
                        sendMessage(obtainMessage(MSG_END));
                    }
                } else {
                    sendMessage(obtainMessage(MSG_END));
                }
                return;
            } else if (msg.what == MSG_END) {
                destroyMyself();
                return;
            }
            super.handleMessage(msg);
        }

    }

    void startDeleteService() {
        mConnection = new DeleteRequestConnection();

        Log.i(TAG, "Bind to MultiChoiceService.");
        // We don't want the service finishes itself just after this connection.
        Intent intent = new Intent(this.getActivity(), MultiChoiceService.class);
        getContext().startService(intent);
        getContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    void destroyMyself() {
        getContext().unbindService(mConnection);
        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

}
