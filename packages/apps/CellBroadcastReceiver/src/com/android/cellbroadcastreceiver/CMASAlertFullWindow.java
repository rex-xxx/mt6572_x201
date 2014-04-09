package com.android.cellbroadcastreceiver;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony;
import android.telephony.CellBroadcastMessage;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import com.mediatek.cmas.ext.ICmasMessageInitiation;

public class CMASAlertFullWindow {

    private static CMASAlertFullWindow sInstance = null;
    public static HashSet<View> sViewHashSet = null;
    public static HashSet<Long> sMsgRowId = null;

    private static final String TAG = "[CMAS][CMASAlertFullWindow]";

    protected Context mContext;
    ArrayList<CellBroadcastMessage> mMessageList;

    /** Handler to add and remove screen on flags for emergency alerts. */
    private final ScreenOffHandler mScreenOffHandler = new ScreenOffHandler();

    //wheather this dialog is start autoly(true) or start from message list(false)
    boolean mStartAuto;

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;

    private static final int CLEAR_SCREEN_FLAG = 1;

    private static final int ALERT_TIME_LENGTH = 10500;//10.5s

    private class ScreenOffHandler extends Handler {

        /** Package local constructor (called from outer class). */
        ScreenOffHandler() {}

        /** Clear the screen on window flags. */
        private void clearWindowFlags(View view) {
            Log.i(TAG, "enter clearWindowFlags");
            if (mLayoutParams != null && mWindowManager != null && sViewHashSet.contains(view)) {
                mLayoutParams.flags = mLayoutParams.flags & (~(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON));
                mLayoutParams.flags = mLayoutParams.flags & (~(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));

                mWindowManager.updateViewLayout(view, mLayoutParams);
            }
        }

        @Override
        public void handleMessage(Message msg) {
            int msgWhat = msg.what;
            Log.i(TAG, "HandleMessage msgWhat = " + msgWhat);
            switch (msgWhat) {
                case CLEAR_SCREEN_FLAG:
                    clearWindowFlags((View)msg.obj);
                    break;

                default:
                    break;
            }
        }
    }

    public static synchronized CMASAlertFullWindow getInstance(Context ctx) {
        if (sInstance == null) {
            Log.d(TAG, "mInstance == null");
            sInstance = new CMASAlertFullWindow(ctx);
        } else {
            Log.d(TAG, "mInstance != null");
        }

        if (sViewHashSet == null) {
            sViewHashSet = new HashSet<View>();
        }

        if (sMsgRowId == null) {
            sMsgRowId = new HashSet<Long>();
        }

        return sInstance;
    }

    public CMASAlertFullWindow(Context ctx) {
        mContext = ctx;

        mWindowManager = (WindowManager)ctx.getSystemService(Activity.WINDOW_SERVICE);

        initWinParameters();
    }

    private void initWinParameters() {
        //init window parameters
        Log.d(TAG, "initWinParameters ++");
        mLayoutParams = new WindowManager.LayoutParams();
        mLayoutParams.type = WindowManager.LayoutParams.TYPE_TOP_MOST; //ok

        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

//        mLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mLayoutParams.width = getMin(mWindowManager.getDefaultDisplay().getWidth(), mWindowManager.getDefaultDisplay()
                .getHeight()) - 64;
        mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParams.gravity = Gravity.CENTER | Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
        mLayoutParams.alpha = 1;
    }

    private View initViewToShow(final CellBroadcastMessage message, final long msgRowId, final boolean bAutoStart) {

        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(getLayoutResId(), null);

        ((Button)view.findViewById(R.id.dismissButton)).setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                clearNotification(bAutoStart);

                dismissAndMarkRead(view, message);
                sViewHashSet.remove(view);
                sMsgRowId.remove(msgRowId);
            }
        });

        updateAlertText(view, message, msgRowId);
        updateAlertIcon(view, message);

        return view;
    }

    public void showView(final CellBroadcastMessage message, final long msgRowId, final boolean bAutoStart) {
        Log.d(TAG, "showView ++ new");
        if (message == null) {
            Log.d(TAG, "showView: mMessageList == null, return");
            return;
        }

        if (sMsgRowId.contains(msgRowId)) {
            Log.d(TAG, "message alread showed");
            return;
        }

        View view = initViewToShow(message, msgRowId, bAutoStart);

        sViewHashSet.add(view);
        sMsgRowId.add(msgRowId);

        if (mWindowManager == null) {
            mWindowManager = (WindowManager)mContext.getSystemService(Activity.WINDOW_SERVICE);
        }

        mWindowManager.addView(view, mLayoutParams);

        Message msg = new Message();
        msg.what = CLEAR_SCREEN_FLAG;
        msg.obj = view;
        mScreenOffHandler.sendMessageDelayed(msg, ALERT_TIME_LENGTH);
    }

    public void dismissAll() {
        Log.d(TAG, "dismissAll ++, sViewHashSet.size() = " + sViewHashSet.size());
        mContext.stopService(new Intent(mContext, CellBroadcastAlertAudio.class));
        Iterator<View> it = sViewHashSet.iterator();
        while (it.hasNext()) {
            View tmpView = (View)it.next();
            /*sViewHashSet.remove(tmpView);*/
            mWindowManager.removeView(tmpView);
        }

        sViewHashSet.clear();
        sMsgRowId.clear();
    }

    /** Returns the resource ID for either the full screen or dialog layout. */
    protected int getLayoutResId() {
//        return R.layout.cell_broadcast_alert_fullscreen;
        return R.layout.cell_broadcast_alert;
    }

    private void updateAlertText(View view, CellBroadcastMessage message, long msgId) {
        Log.d(TAG, "enter updateAlertText");

        int titleId = CellBroadcastResources.getDialogTitleResource(message);

        TextView titleTextView = (TextView) view.findViewById(R.id.alertTitle);
        if (Comparer.getUpdateNumOfCb(message) > 0) { 
            // >0 update message
            Log.d(TAG, "updateAlertText::this is update message");
            titleTextView.setText(mContext.getString(titleId) + " "
                    + mContext.getString(R.string.have_updated));
        } else {
            Log.d(TAG, "updateAlertIcon::this is normal message");
            titleTextView.setText(titleId);
        }

        TextView textViewMsgBody = (TextView) view.findViewById(R.id.message);
        //textViewMsgBody.setAutoLinkMask(Linkify.ALL);
        String content = CellBroadcastResources.getMessageDetails(mContext, message) + "\r\n"
        + message.getMessageBody();
        //textViewMsgBody.setText(content);

        //set typeface
        textViewMsgBody.setTypeface(Typeface.SANS_SERIF);

        //plugin set textview autolink
        Log.i(TAG, "before set autolink");
        if (getSetTextViewAutoLink()) {
            setTextAutoLink(textViewMsgBody, content, view, message, msgId);
        } else {
            textViewMsgBody.setText(content);
        }

    }

    private void setTextAutoLink(TextView tv, String msgContent, final View view, final CellBroadcastMessage msg,
            final long msgId) {
        //plugin

        IAutoLinkClick autoLinkClick = new IAutoLinkClick() {

            public void onAutoLinkClicked() {
                // dismiss top dialog
                Log.d(TAG, "autolink is clicked, dismiss the dialog and mark read");
                dismissAndMarkRead(view, msg);

                //remove view
                sViewHashSet.remove(view);
                sMsgRowId.remove(msgId);
            }
        };

        SpannableString text = new SpannableString(msgContent);
        CMASLinkify.addLinks(text, CMASLinkify.ALL, autoLinkClick);

        tv.setText(text);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void updateAlertIcon(View view, CellBroadcastMessage message) {
        ImageView alertIcon = (ImageView)view.findViewById(R.id.icon);

        if (Comparer.getUpdateNumOfCb(message) > 0) { 
            // >0 update message
            Log.d(TAG, "updateAlertIcon::this is update message");
            alertIcon.setImageDrawable(mContext.getResources().getDrawable(
                    R.drawable.ic_dialog_update_alarm));
        } else {
            Log.d(TAG, "updateAlertIcon::this is normal message");
            alertIcon.setImageDrawable(mContext.getResources().getDrawable(
                    R.drawable.ic_dialog_alarm));
        }

    }

    //get boolean value whether we need to set TextView from plug-in
    private boolean getSetTextViewAutoLink() {

        ICmasMessageInitiation optSetAutoLink = (ICmasMessageInitiation)
        CellBroadcastPluginManager.getCellBroadcastPluginObject(
        CellBroadcastPluginManager.CELLBROADCAST_PLUGIN_TYPE_MESSAGE_INITIATION);
        if (optSetAutoLink != null) {
            return optSetAutoLink.initMessage();
        }

        return false;
    }

    /** Returns the currently displayed message. */
    CellBroadcastMessage getLatestMessage() {
        int index = mMessageList.size() - 1;
        if (index >= 0) {
            return mMessageList.get(index);
        } else {
            return null;
        }
    }

    /** Removes and returns the currently displayed message. */
    private CellBroadcastMessage removeLatestMessage() {
        int index = mMessageList.size() - 1;
        if (index >= 0) {
            return mMessageList.remove(index);
        } else {
            return null;
        }
    }


    private void dismissAndMarkRead(View view, CellBroadcastMessage msg) {
        Log.d(TAG, "dismissAndMarkRead ++");
        mContext.stopService(new Intent(mContext, CellBroadcastAlertAudio.class));

     // Mark the alert as read.
        markRead(msg);

        if(sViewHashSet.contains(view)) {
            mWindowManager.removeView(view);
        }

        //mWindowManager = null;

        return;
    }

    private void markRead(final CellBroadcastMessage msg) {
        // Mark broadcast as read on a background thread.
        new CellBroadcastContentProvider.AsyncCellBroadcastTask(mContext.getContentResolver())
                .execute(new CellBroadcastContentProvider.CellBroadcastOperation() {
                    @Override
                    public boolean execute(CellBroadcastContentProvider provider) {
                        return provider.markBroadcastRead(
                                Telephony.CellBroadcasts.DELIVERY_TIME, msg.getDeliveryTime());
                    }
                });
        
        return;
    }

    private void clearNotification(boolean bClear) {
        Log.i(TAG, "enter clearNotification");
        if (bClear) {
            Log.d(TAG, "Dimissing notification");
            NotificationManager notificationManager = (NotificationManager) mContext
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(CMASPresentationService.NOTIFICATION_ID);
        }
    }

    private int getMin(int width, int height) {
        if (width < height) {
            return width;
        }

        return height;
    }

    public interface IAutoLinkClick {
        void onAutoLinkClicked();//dismiss the dialog
    }
}
