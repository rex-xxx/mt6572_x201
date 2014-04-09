
package com.mediatek.contacts.list.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.contacts.R;
import com.android.contacts.activities.PeopleActivity;

import com.mediatek.contacts.util.ContactsIntent;
import com.mediatek.contacts.util.ErrorCause;

public class MultiChoiceHandlerListener {

    private static final String TAG = MultiChoiceHandlerListener.class.getSimpleName();

    static final String DEFAULT_NOTIFICATION_TAG = "MultiChoiceServiceProgress";

    static final String FAILURE_NOTIFICATION_TAG = "MultiChoiceServiceFailure";

    private final NotificationManager mNotificationManager;

    // context should be the object of MultiChoiceService
    private final Service mContext;

    public MultiChoiceHandlerListener(Service service) {
        mContext = service;
        mNotificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    void onProcessed(final int requestType, final int jobId, final int currentCount,
            final int totalCount, final String contactName) {

        if (currentCount % 10 != 0 && currentCount != 1 && currentCount != totalCount) {
            return;
        }

        final String totalCountString = String.valueOf(totalCount);
        final String tickerText;
        final String description;
        int statIconId = 0;
        if (requestType == MultiChoiceService.TYPE_DELETE) {
            tickerText = mContext.getString(R.string.notifier_progress_delete_message, String
                    .valueOf(currentCount), totalCountString, contactName);
            if (totalCount == -1) {
                description = mContext.getString(R.string.notifier_progress__delete_will_start_message);
            } else {
                description = mContext.getString(R.string.notifier_progress_delete_description,
                        contactName);
            }
            statIconId = android.R.drawable.ic_menu_delete;
        } else {
            tickerText = mContext.getString(R.string.notifier_progress_copy_message, String
                    .valueOf(currentCount), totalCountString, contactName);
            if (totalCount == -1) {
                description = mContext.getString(R.string.notifier_progress__copy_will_start_message);
            } else {
                description = mContext.getString(R.string.notifier_progress_copy_description,
                        contactName);
            }
            statIconId = R.drawable.ic_menu_copy_holo_dark;
        }

        final Notification notification = constructProgressNotification(mContext
                .getApplicationContext(), requestType, description, tickerText, jobId, totalCount,
                currentCount, statIconId);
        mNotificationManager.notify(DEFAULT_NOTIFICATION_TAG, jobId, notification);
    }

    void onFinished(final int requestType, final int jobId, final int total) {
        Log.d(TAG, "onFinished jobId = " + jobId + " total = " + total + " requestType = "
                + requestType);

        // Dismiss MultiChoiceConfirmActivity
        mContext.sendBroadcast(new Intent()
                .setAction(ContactsIntent.MULTICHOICE.ACTION_MULTICHOICE_PROCESS_FINISH));

        final String title;
        final String description;
        final int statIconId;

        if (requestType == MultiChoiceService.TYPE_DELETE) {
            // A good experience is to cache the resource.
            title = mContext.getString(R.string.notifier_finish_delete_title);
            description = mContext.getString(R.string.notifier_finish_delete_content, total);
            // statIconId = R.drawable.ic_stat_delete;
            statIconId = android.R.drawable.ic_menu_delete;
        } else {
            title = mContext.getString(R.string.notifier_finish_copy_title);
            description = mContext.getString(R.string.notifier_finish_copy_content, total);
            statIconId = R.drawable.ic_menu_copy_holo_dark;
        }

        final Intent intent = new Intent(mContext, PeopleActivity.class);
        final Notification notification = constructFinishNotification(mContext, title, description,
                intent, statIconId);
        mNotificationManager.notify(DEFAULT_NOTIFICATION_TAG, jobId, notification);
    }

    void onFailed(final int requestType, final int jobId, final int total, final int succeeded,
            final int failed) {
        Log.d(TAG, "onFailed requestType =" + requestType + " jobId = " + jobId + " total = "
                + total + " succeeded = " + succeeded + " failed = " + failed);
        final String title;
        final String content;
        if (requestType == MultiChoiceService.TYPE_DELETE) {
            title = mContext.getString(R.string.notifier_fail_delete_title, total);
            content = mContext.getString(R.string.notifier_multichoice_process_report, succeeded,
                    failed);
        } else {
            title = mContext.getString(R.string.notifier_fail_copy_title, total);
            content = mContext.getString(R.string.notifier_multichoice_process_report, succeeded,
                    failed);
        }
        /*
         * Bug Fix by Mediatek Begin.
         *   Original Android's code:
         *     xxx
         *   CR ID: ALPS00251890
         *   Descriptions: 
         */
        final Notification notification = constructReportNotification(mContext, title, content, jobId);
        /*
         * Bug Fix by Mediatek End.
         */
        mNotificationManager.notify(DEFAULT_NOTIFICATION_TAG, jobId, notification);

    }

    void onFailed(final int requestType, final int jobId, final int total, final int succeeded,
            final int failed, final int errorCause) {
        Log.d(TAG, "onFailed requestType =" + requestType + " jobId = " + jobId + " total = "
                + total + " succeeded = " + succeeded + " failed = " + failed + " errorCause = "
                + errorCause + " ");
        String title;
        final String content;
        if (requestType == MultiChoiceService.TYPE_DELETE) {
            title = mContext.getString(R.string.notifier_fail_delete_title, total);
            content = mContext.getString(R.string.notifier_multichoice_process_report, succeeded,
                    failed);
        } else {
            title = mContext.getString(R.string.notifier_fail_copy_title, total);
            if (errorCause == ErrorCause.SIM_NOT_READY) {
                content = mContext.getString(R.string.notifier_failure_sim_notready, succeeded,
                        failed);
            } else if (errorCause == ErrorCause.SIM_STORAGE_FULL) {
                content = mContext.getString(R.string.notifier_failure_by_sim_full, succeeded,
                        failed);
            } else if (errorCause == ErrorCause.ERROR_USIM_EMAIL_LOST) {
                if (failed == 0) {
                    title = mContext.getString(R.string.notifier_finish_copy_title);
                }
                content = mContext.getString(R.string.error_import_usim_contact_email_lost,
                        succeeded, failed);
            } else {
                content = mContext.getString(R.string.notifier_multichoice_process_report,
                        succeeded, failed);
            }
        }
        /*
         * Bug Fix by Mediatek Begin.
         *   Original Android's code:
         *     xxx
         *   CR ID: ALPS00251890
         *   Descriptions: 
         */
        final Notification notification = constructReportNotification(mContext, title, content, jobId);
        /*
         * Bug Fix by Mediatek End.
         */
        mNotificationManager.notify(DEFAULT_NOTIFICATION_TAG, jobId, notification);

    }

    void onCanceled(final int requestType, final int jobId, final int total, final int succeeded,
            final int failed) {
        Log.d(TAG, "onCanceled requestType =" + requestType + " jobId = " + jobId + " total = "
                + total + " succeeded = " + succeeded + " failed = " + failed);
        final String title;
        final String content;
        if (requestType == MultiChoiceService.TYPE_DELETE) {
            title = mContext.getString(R.string.notifier_cancel_delete_title, total);
        } else {
            title = mContext.getString(R.string.notifier_cancel_copy_title, total);
        }
        if (total != -1) {
            content = mContext.getString(R.string.notifier_multichoice_process_report, succeeded,
                    failed);
        } else {
            content = "";
        }
        /*
         * Bug Fix by Mediatek Begin.
         *   Original Android's code:
         *     xxx
         *   CR ID: ALPS00251890
         *   Descriptions: 
         */
        final Notification notification = constructReportNotification(mContext, title, content, jobId);
        /*
         * Bug Fix by Mediatek End.
         */
        mNotificationManager.notify(DEFAULT_NOTIFICATION_TAG, jobId, notification);
    }
    /*
     * Bug Fix by Mediatek Begin.
     *   Original Android's code:
     *     xxx
     *   CR ID: ALPS00249590
     *   Descriptions: 
     */
    void onCanceling(final int requestType, final int jobId) {
        Log.i(TAG, "[onCanceling] requestType : " + requestType + " | jobId : " + jobId);
        final String description;
        int statIconId = 0;
        if (requestType == MultiChoiceService.TYPE_DELETE) {
            description = mContext.getString(R.string.multichoice_confirmation_title_delete);
            statIconId = android.R.drawable.ic_menu_delete;
        } else {
            description = "";
        }

        final Notification notification = constructCancelingNotification(mContext, description,
                jobId, statIconId);
        mNotificationManager.notify(DEFAULT_NOTIFICATION_TAG, jobId, notification);
    }
    /*
     * Bug Fix by Mediatek End.
     */
    /**
     * Constructs a Notification telling users the process is finished.
     * 
     * @param context
     * @param title
     * @param description Content of the Notification
     * @param intent Intent to be launched when the Notification is clicked. Can
     *            be null.
     * @param statIconId
     */
    public static Notification constructFinishNotification(Context context, String title,
            String description, Intent intent, final int statIconId) {
        return new Notification.Builder(context).setAutoCancel(true).setSmallIcon(statIconId)
                .setContentTitle(title).setContentText(description).setTicker(
                        title + "\n" + description).setContentIntent(
                        PendingIntent.getActivity(context, 0, (intent != null ? intent
                                : new Intent()), 0)).getNotification();
    }

    /**
     * Constructs a {@link Notification} showing the current status of
     * import/export. Users can cancel the process with the Notification.
     * 
     * @param context The service of MultichoiceService
     * @param requestType delete
     * @param description Content of the Notification.
     * @param tickerText
     * @param jobId
     * @param totalCount The number of vCard entries to be imported. Used to
     *            show progress bar. -1 lets the system show the progress bar
     *            with "indeterminate" state.
     * @param currentCount The index of current vCard. Used to show progress
     *            bar.
     * @param statIconId
     */
    public static Notification constructProgressNotification(Context context, int requestType,
            String description, String tickerText, int jobId, int totalCount, int currentCount,
            int statIconId) {

        Intent cancelIntent = new Intent(context, MultiChoiceConfirmActivity.class);
        cancelIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        cancelIntent.putExtra(MultiChoiceConfirmActivity.JOB_ID, jobId);
        cancelIntent.putExtra(MultiChoiceConfirmActivity.ACCOUNT_INFO, "TODO finish");
        cancelIntent.putExtra(MultiChoiceConfirmActivity.TYPE, requestType);

        final Notification.Builder builder = new Notification.Builder(context);
//        builder.setOngoing(true).setProgress(totalCount, currentCount, totalCount == -1).setTicker(
//                tickerText).setContentTitle(description).setSmallIcon(statIconId).setContentIntent(
//                PendingIntent.getActivity(context, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        builder.setOngoing(true).setProgress(totalCount, currentCount, totalCount == -1)
                .setContentTitle(description).setSmallIcon(statIconId).setContentIntent(
                        PendingIntent.getActivity(context, jobId, cancelIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT));
        if (totalCount > 0) {
            builder.setContentText(context.getString(R.string.percentage, String
                    .valueOf(currentCount * 100 / totalCount)));
        }
        return builder.getNotification();
    }

    /**
     * Constructs a Notification telling users the process is canceled.
     * 
     * @param context
     * @param description Content of the Notification
     */
    /*
     * Bug Fix by Mediatek Begin.
     *   Original Android's code:
     *     xxx
     *   CR ID: ALPS00251890
     *   Descriptions: add int jobId 
     */
    public static Notification constructReportNotification(Context context, String title,
            String content, int jobId) {

        Intent reportIntent = new Intent(context, MultiChoiceConfirmActivity.class);
        reportIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        reportIntent.putExtra(MultiChoiceConfirmActivity.REPORTDIALOG, true);
        reportIntent.putExtra(MultiChoiceConfirmActivity.REPORT_TITLE, title);
        reportIntent.putExtra(MultiChoiceConfirmActivity.REPORT_CONTENT, content);

        if (content == null || content.isEmpty()) {
            return new Notification.Builder(context).setAutoCancel(true).setSmallIcon(
                    android.R.drawable.stat_notify_error).setContentTitle(title).setTicker(title)
                    .setContentIntent(
                            PendingIntent.getActivity(context, jobId, new Intent(),
                                    PendingIntent.FLAG_UPDATE_CURRENT)).getNotification();
        } else {
            return new Notification.Builder(context).setAutoCancel(true).setSmallIcon(
                    android.R.drawable.stat_notify_error).setContentTitle(title).setContentText(
                    content).setTicker(title + "\n" + content).setContentIntent(
                    PendingIntent.getActivity(context, jobId, reportIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT)).getNotification();
        }
    }
    /*
     * Bug Fix by Mediatek End.
     */
    /*
     * Bug Fix by Mediatek Begin.
     *   Original Android's code:
     *     xxx
     *   CR ID: ALPS00249590
     *   Descriptions: 
     */
    public static Notification constructCancelingNotification(Context context, String description,
            int jobId, int statIconId) {

        final Notification.Builder builder = new Notification.Builder(context);
        builder.setOngoing(true).setProgress(-1, -1, true).setContentTitle(description)
                .setSmallIcon(statIconId).setContentIntent(
                        PendingIntent.getActivity(context, jobId, new Intent(),
                                PendingIntent.FLAG_UPDATE_CURRENT));

        return builder.getNotification();
    }
    /*
     * Bug Fix by Mediatek End.
     */
    
    //visible for test
    public void  cancelAllNotifition() {
        mNotificationManager.cancelAll();
    }
    
}
