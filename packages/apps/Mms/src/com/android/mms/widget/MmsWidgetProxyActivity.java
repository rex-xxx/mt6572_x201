package com.android.mms.widget;

import com.android.mms.data.Conversation;
import com.android.mms.ui.CBMessageListActivity;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.ConversationList;
import com.android.mms.ui.FolderViewList;
import com.android.mms.ui.MessageUtils;
import com.android.mms.ui.WPMessageActivity;
import com.mediatek.encapsulation.MmsLog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import android.provider.Telephony.Threads;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.ipmsg.util.IpMessageUtils;
import com.mediatek.mms.ipmessage.IpMessageConsts.RemoteActivities;

public class MmsWidgetProxyActivity extends Activity {
    
    private static final String TAG = "MmsWidgetProxyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Context context = getApplicationContext();
        finish();
        if (MmsWidgetService.ACTION_CONVERSATION_MODE.equals(intent.getAction())) {
            startActivityForConv(context, intent);
        } else if (MmsWidgetService.ACTION_FOLDER_MODE.equals(intent.getAction())) {
            startActivityForFolder(context, intent);
        } else if (MmsWidgetProvider.ACTION_COMPOSE_NEW_MESSAGE.equals(intent.getAction())) {
            startActivityForNew(context);
        } else {
            return;
        }
    }
    
    private void startActivityForNew(Context context) {
        MmsLog.d(TAG, "compose new message");
        Intent ic = new Intent(context, ComposeMessageActivity.class);
        startActivityInternal(context, ic);
    }

    private void startActivityForConv(Context context, Intent intent) {
        Intent ic = null;
        int msgType = intent.getIntExtra(MmsWidgetService.EXTRA_KEY_CONVSATION_TYPE, 0);
        long threadId = intent.getLongExtra("thread_id", 0);
        MmsLog.d(TAG, "conversation mode -- msgType=" + msgType + "  thread_id=" + threadId);
        switch (msgType) {
        case Threads.CELL_BROADCAST_THREAD:
            MmsLog.d(TAG, "conversation mode -- CB");
            ic = CBMessageListActivity.createIntent(context, threadId);
            break;
        case Threads.WAPPUSH_THREAD:
            MmsLog.d(TAG, "conversation mode -- push");
            ic = WPMessageActivity.createIntent(context, threadId);
            break;
        /// M: add for ipmessage guide thread
        case Threads.IP_MESSAGE_GUIDE_THREAD:
            MmsLog.d(TAG, "conversation mode -- ipmessage guide");
            Intent it = new Intent(RemoteActivities.SERVICE_CENTER);
            IpMessageUtils.startRemoteActivity(context, it);
            return;
        /// M: add for ipmessage group thread
        case Conversation.TYPE_GROUP:
            MmsLog.d(TAG, "conversation mode -- ipmessage group");
            Intent it2 = new Intent(RemoteActivities.CHAT_DETAILS_BY_THREAD_ID);
            it2.putExtra(RemoteActivities.KEY_THREAD_ID, threadId);
            it2.putExtra(RemoteActivities.KEY_BOOLEAN, false);
            IpMessageUtils.startRemoteActivity(context, it2);
            return;
        case MmsWidgetService.MORE_MESSAGES:
            ic = new Intent(context, ConversationList.class);
            break;
        default:
            MmsLog.d(TAG, "conversation mode -- normal message");
            ic = ComposeMessageActivity.createIntent(context, threadId);
            break;
        }
        startActivityInternal(context, ic);
    }

    private void startActivityForFolder(Context context, Intent intent) {
        Intent ic = new Intent(context, FolderViewList.class);
        boolean shouldStartActivity = true;
        int boxType = FolderViewList.OPTION_INBOX;
        switch (intent.getIntExtra(MmsWidgetService.EXTRA_KEY_FOLDER_TYPE, 0)) {

        case MmsWidgetService.FOLDER_HAS_UNREAD:
            MmsLog.d(TAG, "folder mode -- has unread");
            long threadId = intent.getLongExtra("thread_id", 0);
            if (threadId > 0) {
                shouldStartActivity = false;
                getUnreadInfo(getQueryUri(threadId), context);
            }
            break;

        case MmsWidgetService.FOLDER_HAS_DRAFT:
            MmsLog.d(TAG, "folder mode -- has draft message");
            boxType = FolderViewList.OPTION_DRAFTBOX;
            break;
        
        case MmsWidgetService.FOLDER_HAS_ERROR:
            MmsLog.d(TAG, "folder mode -- has error");
            boxType = FolderViewList.OPTION_OUTBOX;
            break;

        case MmsWidgetService.FOLDER_NORMAL:
            MmsLog.d(TAG, "folder mode -- normal message");
            threadId = intent.getLongExtra("thread_id", 0);
            if (threadId > 0) {
                shouldStartActivity = false;
                getThreadInfo(getQueryUri(threadId), context);
            }
            break;

        default:
            MmsLog.d(TAG, "folder mode -- CB or PUSH or default");
            break;
        }

        ic.putExtra(FolderViewList.FOLDERVIEW_KEY, boxType);
        MmsLog.d(TAG, "folder mode -- boxType = " + boxType);
        if (shouldStartActivity) {
            startActivityInternal(context, ic);
        }
    }

    private void getUnreadInfo(final Uri uri, final Context context) {
        MmsLog.d(TAG, "folder mode -- has unread");
        new Thread(new Runnable() {

            public void run() {
                Intent ic = new Intent(context, FolderViewList.class);
                Cursor cursor = context.getContentResolver().query(uri, null, " read=0 ", null, null);
                int boxType = FolderViewList.OPTION_INBOX;
                if (cursor != null) {
                    try {
                        while (cursor.moveToNext()) {
                            int msgBox = cursor.getInt(1);
                            if (msgBox == 1) {
                                boxType = FolderViewList.OPTION_INBOX;
                                break;
                            } else if (msgBox >= 4) {
                                boxType = FolderViewList.OPTION_OUTBOX;
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
                ic.putExtra(FolderViewList.FOLDERVIEW_KEY, boxType);
                startActivityInternal(context, ic);
            }
        }).start();
    }

    private void getThreadInfo(final Uri uri, final Context context) {
        MmsLog.d(TAG, "getThreadInfo, uri = " + uri);
        new Thread(new Runnable() {

            public void run() {
                Intent it = new Intent(context, FolderViewList.class);
                Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                int msgBox = 0;
                int boxType = FolderViewList.OPTION_INBOX;
                if (cursor != null && cursor.moveToFirst()) {
                    msgBox = cursor.getInt(1);
                    MmsLog.d(TAG, "getThreadInfo, thread msgBox = " + msgBox);
                    if (2 == msgBox) {
                        boxType = FolderViewList.OPTION_SENTBOX;
                    } else if (3 == msgBox) {
                        boxType = FolderViewList.OPTION_DRAFTBOX;
                    } else if (4 <= msgBox) {
                        boxType = FolderViewList.OPTION_OUTBOX;
                    }
                } else {
                    MmsLog.d(TAG, "getThreadInfo, cursor is null or cursor count is 0");
                }
                if (cursor != null) {
                    cursor.close();
                }
                it.putExtra(FolderViewList.FOLDERVIEW_KEY, boxType);
                startActivityInternal(context, it);
            }
        }).start();
    }

    private Uri getQueryUri(long threadId) {
        return Uri.parse("content://mms-sms/widget/thread/" + threadId);
    }

    private void startActivityInternal(Context context, Intent intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            MmsLog.d(TAG, "Failed to start intent activity", e);
        }
    }


}
