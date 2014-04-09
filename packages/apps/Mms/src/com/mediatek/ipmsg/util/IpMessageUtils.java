package com.mediatek.ipmsg.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.Drawable;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.provider.Telephony.Sms;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.data.Conversation;
import com.android.mms.data.WorkingMessage;
import com.android.mms.ui.MessageUtils;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedTelephonyService;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.com.mediatek.telephony.EncapsulatedTelephonyManagerEx;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.mms.ipmessage.ActivitiesManager;
import com.mediatek.mms.ipmessage.ChatManager;
import com.mediatek.mms.ipmessage.ContactManager;
import com.mediatek.mms.ipmessage.GroupManager;
import com.mediatek.mms.ipmessage.IIpMessagePlugin;
import com.mediatek.mms.ipmessage.INotificationsListener;
import com.mediatek.mms.ipmessage.IpMessageConfig;
import com.mediatek.mms.ipmessage.IpMessageConsts;
import com.mediatek.mms.ipmessage.IpMessageConsts.ActivationStatus;
import com.mediatek.mms.ipmessage.IpMessageConsts.BackupMsgStatus;
import com.mediatek.mms.ipmessage.IpMessageConsts.DownloadAttachStatus;
import com.mediatek.mms.ipmessage.IpMessageConsts.ImStatus;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageServiceId;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageStatus;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageType;
import com.mediatek.mms.ipmessage.IpMessageConsts.NewMessageAction;
import com.mediatek.mms.ipmessage.IpMessageConsts.RefreshContactList;
import com.mediatek.mms.ipmessage.IpMessageConsts.RefreshGroupList;
import com.mediatek.mms.ipmessage.IpMessageConsts.RemoteActivities;
import com.mediatek.mms.ipmessage.IpMessageConsts.RestoreMsgStatus;
import com.mediatek.mms.ipmessage.IpMessageConsts.SaveHistroy;
import com.mediatek.mms.ipmessage.IpMessageConsts.ServiceStatus;
import com.mediatek.mms.ipmessage.IpMessageConsts.SetProfileResult;
import com.mediatek.mms.ipmessage.IpMessageConsts.SpecialSimId;
import com.mediatek.mms.ipmessage.IpMessagePluginImpl;
import com.mediatek.mms.ipmessage.message.IpImageMessage;
import com.mediatek.mms.ipmessage.message.IpMessage;
import com.mediatek.mms.ipmessage.message.IpVideoMessage;
import com.mediatek.mms.ipmessage.message.IpVoiceMessage;
import com.mediatek.mms.ipmessage.MessageManager;
import com.mediatek.mms.ipmessage.NotificationsManager;
import com.mediatek.mms.ipmessage.SettingsManager;
import com.mediatek.pluginmanager.Plugin;
import com.mediatek.pluginmanager.PluginManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IpMessageUtils {
    private static final String TAG = "Mms/ipmsg/utils";

    private static final String[] SMS_BODY_PROJECTION_WITH_IPMSG_ID = { Sms._ID, Sms.IPMSG_ID };
    private static final String SMS_DRAFT_WHERE = Sms.TYPE + "=" + Sms.MESSAGE_TYPE_DRAFT;
    private static final int SMS_ID_INDEX = 0;
    private static final int SMS_IPMSG_ID_INDEX = 1;

    /// M: add for ipmessage {@
    public static IIpMessagePlugin sIpMessagePlugin = null;
    public static final String SELECTION_CONTACT_RESULT = "contactId";
    public static final String IPMSG_NOTIFICATION_TAG = "Mms/noti";
    public static final String WALLPAPER_PATH = "/data/data/com.android.providers.telephony/app_wallpaper";

    public static synchronized IIpMessagePlugin getIpMessagePlugin(Context context) {
        if (sIpMessagePlugin == null) {
            initIpMessagePlugin(context);
        }
        return sIpMessagePlugin;
    }

    private static synchronized void initIpMessagePlugin(Context context) {
        if (sIpMessagePlugin == null) {
            try {
                sIpMessagePlugin = (IIpMessagePlugin) PluginManager.createPluginObject(context,
                        IIpMessagePlugin.class.getName(), PLUGIN_VERSION, PLUGIN_METANAME);
                MmsLog.d(TAG, "sIpMessagePlugin = " + sIpMessagePlugin);
            } catch (Plugin.ObjectCreationException e) {
                sIpMessagePlugin = new IpMessagePluginImpl(context);
                MmsLog.d(TAG, "default sIpMessagePlugin = " + sIpMessagePlugin);
            }
        }
    }

    /**
     * Get plugin activity manager.
     * @param the context
     * @return ActivitiesManager
     */
    private static synchronized ActivitiesManager getActivitiesManager(Context context) {
        return getIpMessagePlugin(context).getActivitiesManager(context.getApplicationContext());
    }

     /**
     * Get IP message chat manager.
     * @return ChatManager
     */
    public static synchronized ChatManager getChatManager(Context context) {
        return getIpMessagePlugin(context).getChatManager(context.getApplicationContext());
    }

     /**
     * Get IP message contact manager.
     * @return ContactManager
     */
    public static synchronized ContactManager getContactManager(Context context) {
        return getIpMessagePlugin(context).getContactManager(context.getApplicationContext());
    }

     /**
     * Get IP message notifications manager.
     * @param the context
     * @return NotificationsManager
     */
    public static synchronized NotificationsManager getNotificationsManager(Context context) {
        return getIpMessagePlugin(context).getNotificationsManager(context.getApplicationContext());
    }

     /**
     * Get IP message group manager.
     * @param the context
     * @return GroupManager
     */
    public static synchronized GroupManager getGroupManager(Context context) {
        return getIpMessagePlugin(context).getGroupManager(context.getApplicationContext());
    }

     /**
     * Get IP message manager.
     * @return MessageManager
     */
    public static synchronized MessageManager getMessageManager(Context context) {
        return getIpMessagePlugin(context).getMessageManager(context.getApplicationContext());
    }

     /**
     * Get IP message service manager.
     * @return ServiceManager
     */
    public static synchronized com.mediatek.mms.ipmessage.ServiceManager getServiceManager(Context context) {
        return getIpMessagePlugin(context).getServiceManager(context.getApplicationContext());
    }

    /**
     * Get IP message resource manager.
     * 
     * @return ResourceManager
     */
    public static synchronized com.mediatek.mms.ipmessage.ResourceManager getResourceManager(Context context) {
        return getIpMessagePlugin(context).getResourceManager(context.getApplicationContext());
    }

     /**
     * Get IP message settings manager.
     * @return SettingsManager
     */
    public static synchronized SettingsManager getSettingsManager(Context context) {
        return getIpMessagePlugin(context).getSettingsManager(context.getApplicationContext());
    }

    public static void startRemoteActivity(Context context, Intent intent) {
        getActivitiesManager(context).startRemoteActivity(context, intent);
    }

    public static void startRemoteActivityForResult(Context context, Intent intent) {
        getActivitiesManager(context).startRemoteActivity(context, intent);
    }

    public static final int IPMSG_ERROR_ACTION                  = 0;
    public static final int IPMSG_NEW_MESSAGE_ACTION            = 1;
    public static final int IPMSG_REFRESH_CONTACT_LIST_ACTION   = 2;
    public static final int IPMSG_REFRESH_GROUP_LIST_ACTION     = 3;
    public static final int IPMSG_SERCIVE_STATUS_ACTION         = 4;
    public static final int IPMSG_IM_STATUS_ACTION              = 5;
    public static final int IPMSG_SAVE_HISTORY_ACTION           = 6;
    public static final int IPMSG_ACTIVATION_STATUS_ACTION      = 7;
    public static final int IPMSG_IP_MESSAGE_STATUS_ACTION      = 8;
    public static final int IPMSG_DOWNLOAD_ATTACH_STATUS_ACTION = 9;
    public static final int IPMSG_SET_PROFILE_RESULT_ACTION     = 10;
    public static final int IPMSG_BACKUP_MSG_STATUS_ACTION      = 11;
    public static final int IPMSG_RESTORE_MSG_STATUS_ACTION     = 12;
    public static final int IPMSG_UPDATE_GROUP_INFO             = 13;
    public static final int IPMSG_IPMESSAGE_CONTACT_UPDATE      = 14;
    public static final int IPMSG_SIM_INFO_ACTION               = 15;

    public static final String PLUGIN_VERSION = "2.0.0";
    public static final String PLUGIN_METANAME = "class";

    public static int getActionTypeByAction(String action) {
        if (TextUtils.isEmpty(action)) {
            return IPMSG_ERROR_ACTION;
        }
        if (action.equals(NewMessageAction.ACTION_NEW_MESSAGE)) {
            return IPMSG_NEW_MESSAGE_ACTION;
        }
        if (action.equals(RefreshContactList.ACTION_REFRESH_CONTACTS_LIST)) {
            return IPMSG_REFRESH_CONTACT_LIST_ACTION;
        }
        if (action.equals(RefreshGroupList.ACTION_REFRESH_GROUP_LIST)) {
            return IPMSG_REFRESH_GROUP_LIST_ACTION;
        }
        if (action.equals(ServiceStatus.ACTION_SERVICE_STATUS)) {
            return IPMSG_SERCIVE_STATUS_ACTION;
        }
        if (action.equals(ImStatus.ACTION_IM_STATUS)) {
            return IPMSG_IM_STATUS_ACTION;
        }
        if (action.equals(SaveHistroy.ACTION_SAVE_HISTROY)) {
            return IPMSG_SAVE_HISTORY_ACTION;
        }
        if (action.equals(ActivationStatus.ACTION_ACTIVATION_STATUS)) {
            return IPMSG_ACTIVATION_STATUS_ACTION;
        }
        if (action.equals(IpMessageStatus.ACTION_MESSAGE_STATUS)) {
            return IPMSG_IP_MESSAGE_STATUS_ACTION;
        }
        if (action.equals(DownloadAttachStatus.ACTION_DOWNLOAD_ATTACH_STATUS)) {
            return IPMSG_DOWNLOAD_ATTACH_STATUS_ACTION;
        }
        if (action.equals(SetProfileResult.ACTION_SET_PROFILE_RESULT)) {
            return IPMSG_SET_PROFILE_RESULT_ACTION;
        }
        if (action.equals(BackupMsgStatus.ACTION_BACKUP_MSG_STATUS)) {
            return IPMSG_BACKUP_MSG_STATUS_ACTION;
        }
        if (action.equals(RestoreMsgStatus.ACTION_RESTORE_MSG_STATUS)) {
            return IPMSG_RESTORE_MSG_STATUS_ACTION;
        }
        if (action.equals(IpMessageConsts.UpdateGroup.UPDATE_GROUP_ACTION)) {
            return IPMSG_UPDATE_GROUP_INFO;
        }
        if (action.equals(IpMessageConsts.ContactStatus.CONTACT_UPDATE)) {
            return IPMSG_IPMESSAGE_CONTACT_UPDATE;
        }
        if (action.equals(IpMessageConsts.SimInfoChanged.SIM_INFO_ACTION)) {
            return IPMSG_SIM_INFO_ACTION;
        }

        MmsLog.w(TAG, "getActionTypeByAction(): Unknown ipmessage action.");
        return IPMSG_ERROR_ACTION;
    }

    public static void addIpMsgNotificationListeners(Context context, INotificationsListener notiListener) {
        getIpMessagePlugin(context.getApplicationContext()).getNotificationsManager(context.getApplicationContext())
                .registerNotificationsListener(notiListener);
    }

    public static void removeIpMsgNotificationListeners(Context context, INotificationsListener notiListener) {
        getIpMessagePlugin(context.getApplicationContext()).getNotificationsManager(context.getApplicationContext())
                .unregisterNotificationsListener(notiListener);
    }

    public static void showInviteIpMsgConfirmDialog(Context context,
            OnClickListener listener) {
        new AlertDialog.Builder(context)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(R.string.discard_message)
                .setMessage(IpMessageUtils.getResourceManager(context)
                    .getSingleString(IpMessageConsts.string.ipmsg_invite_friends_to_ipmsg_dialog_msg))
                .setPositiveButton(R.string.yes, listener)
                .setNegativeButton(R.string.no, null)
                .show();
    }
    // @}

    public static final int WELCOME = 0;
    public static final int ACTIVATE = 1;
    private static final int SIM_SELECTION_MODE_ACTIVATION = 0;
    private static final int SIM_SELECTION_MODE_ENABLED = 1;

    private static final int SINGLE_SIM = -1;
    private static final int DUAL_SIM = 0;
    private static AlertDialog sActivationDialog = null;
    private static final MyURLSpan MY_URL_SPAN = new MyURLSpan();
    /**
     * Check current IP Message Service status is ready or not.
     * @param activityContext
     * @return
     */
    public static boolean checkCurrentIpMessageServiceStatus(final Activity activityContext, final boolean allowSwitchCard,
            final Handler ipMsgHandler) {
        MmsLog.d(TAG, "checkCurrentIpMessageServiceStatus(): context = " + activityContext);
        if (activityContext == null) {
            MmsLog.e(TAG, "checkCurrentIpMessageServiceStatus(): context = " + activityContext, new Exception());
            return false;
        }
        if (MmsConfig.getIpMessagServiceId(activityContext) == IpMessageServiceId.NO_SERVICE) {
            MmsLog.d(TAG, "checkCurrentIpMessageServiceStatus(): service is not exist. return false!");
            return false;
        }

        boolean isServiceEnabled = MmsConfig.isServiceEnabled(activityContext);
        boolean isServiceActivated = MmsConfig.isActivated(activityContext);
        MmsLog.d(TAG, "checkCurrentIpMessageServiceStatus(): isServiceEnabled = " + isServiceEnabled
            + ", isServiceActivated = " + isServiceActivated);
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            MmsLog.d(TAG, "checkCurrentIpMessageServiceStatus(): Dual SIM.");
            /// M: add for IP message, dual SIM
            List<SIMInfo> simInfoList = new ArrayList<SIMInfo>();
            SIMInfo sim1Info = SIMInfo.getSIMInfoBySlot(activityContext, EncapsulatedPhone.GEMINI_SIM_1);
            SIMInfo sim2Info = SIMInfo.getSIMInfoBySlot(activityContext, EncapsulatedPhone.GEMINI_SIM_2);
            if (sim1Info != null) {
                simInfoList.add(sim1Info);
            }
            if (sim2Info != null) {
                simInfoList.add(sim2Info);
            }
            if (simInfoList.size() == 0) {
                /// M: add for IP message, No SIM
                MmsLog.d(TAG, "checkCurrentIpMessageServiceStatus(): No SIM card!");
                String content = IpMessageUtils.getResourceManager(activityContext)
                                                .getSingleString(IpMessageConsts.string.ipmsg_no_sim_card);
                Toast.makeText(activityContext, content, Toast.LENGTH_LONG).show();
            } else if (simInfoList.size() == 1) {
                /// M: add for IP message, one SIM
                MmsLog.d(TAG, "checkCurrentIpMessageServiceStatus(): One SIM card!");
                if (isServiceEnabled) {
                    return true;
                } else if (isServiceActivated) {
                    showActivateOrEnableIpMessageServiceDialog(activityContext, null, (int) simInfoList
                            .get(0).getSimId(), SIM_SELECTION_MODE_ENABLED, ipMsgHandler);
                } else {
                    showActivitionDlg(activityContext, ACTIVATE, (int) simInfoList.get(0).getSimId());
                }
            } else {
                /// M: add for IP message, two or more SIM
                MmsLog.d(TAG, "checkCurrentIpMessageServiceStatus(): Two SIM card!");
                long defaultSimId = Settings.System.getLong(activityContext.getContentResolver(),
                    Settings.System.SMS_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
                if (defaultSimId != Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK
                        && defaultSimId != Settings.System.DEFAULT_SIM_NOT_SET) {
                    // default SIM card
                    if (isServiceEnabled) {
                        if (MmsConfig.isServiceEnabled(activityContext, (int) defaultSimId)) {
                            return true;
                        } else if (MmsConfig.isActivated(activityContext, (int) defaultSimId)) {
                            showActivateOrEnableIpMessageServiceDialog(activityContext, null, (int) defaultSimId,
                                SIM_SELECTION_MODE_ENABLED, ipMsgHandler);
                        } else if (allowSwitchCard) {
                            long switchSimId = 0L;
                            if (simInfoList.get(0).getSimId() == defaultSimId) {
                                switchSimId = simInfoList.get(1).getSimId();
                            } else {
                                switchSimId = simInfoList.get(0).getSimId();
                            }
                            showSwitchSimDialog(activityContext, switchSimId);
                        } else {
                            showActivateOrEnableIpMessageServiceDialog(activityContext, null,
                                (int) simInfoList.get(0).getSimId(), SIM_SELECTION_MODE_ENABLED, ipMsgHandler);
                        }
                    } else if (isServiceActivated && MmsConfig.isActivated(activityContext, (int) defaultSimId)) {
                        showActivateOrEnableIpMessageServiceDialog(activityContext, simInfoList,
                            (int) defaultSimId, SIM_SELECTION_MODE_ENABLED, ipMsgHandler);
                    } else {
                        showActivitionDlg(activityContext, ACTIVATE, (int) defaultSimId);
                    }
                } else {
                    // No default SIM card
                    if (isServiceEnabled && ipMsgHandler == null) {
                        return true;
                    } else if (isServiceActivated) {
                     // one SIM didn't activated, one SIM disabled
                        boolean isSimActivated0 = MmsConfig.isActivated(activityContext,
                                                                        (int) simInfoList.get(0).getSimId());
                        boolean isSimActivated1 = MmsConfig.isActivated(activityContext,
                                                                        (int) simInfoList.get(1).getSimId());
                        if (isSimActivated0 && isSimActivated1) {
                            showActivateOrEnableIpMessageServiceDialog(activityContext, simInfoList,
                                DUAL_SIM, SIM_SELECTION_MODE_ENABLED, ipMsgHandler);
                        } else if (isSimActivated0) {
                            showActivateOrEnableIpMessageServiceDialog(activityContext, simInfoList,
                                (int) simInfoList.get(0).getSimId(), SIM_SELECTION_MODE_ENABLED, ipMsgHandler);
                        } else if (isSimActivated1) {
                            showActivateOrEnableIpMessageServiceDialog(activityContext, simInfoList,
                                (int) simInfoList.get(1).getSimId(), SIM_SELECTION_MODE_ENABLED, ipMsgHandler);
                        }
                    } else {
                        showSimSelectionDialog(activityContext, simInfoList, SIM_SELECTION_MODE_ACTIVATION, ipMsgHandler);
                    }
                }
            }
        } else {
            MmsLog.d(TAG, "checkCurrentIpMessageServiceStatus(): Single SIM.");
            /// M: add for IP message, single SIM
//            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            EncapsulatedTelephonyService phone = EncapsulatedTelephonyService.getInstance();
            boolean hasSimCard = false;
            if (phone != null) {
                try {
                    hasSimCard = phone.isSimInsert(0);
                } catch (RemoteException e) {
                    MmsLog.e(TAG, "check sim insert status failed");
                }
            }
            if (isServiceEnabled) {
                return true;
            } else if (isServiceActivated) {
                showActivateOrEnableIpMessageServiceDialog(activityContext, null, SINGLE_SIM,
                    SIM_SELECTION_MODE_ENABLED, ipMsgHandler);
            } else if (hasSimCard) {
                showActivitionDlg(activityContext, ACTIVATE, SpecialSimId.ALL_SIM_ID);
            } else {
                /// M: add for IP message, No SIM
                MmsLog.d(TAG, "checkCurrentIpMessageServiceStatus(): No SIM card!");
                String content = IpMessageUtils.getResourceManager(activityContext)
                                                .getSingleString(IpMessageConsts.string.ipmsg_no_sim_card);
                Toast.makeText(activityContext, content, Toast.LENGTH_LONG).show();
            }
        }
        return false;
    }

    private static void showActivitionDlg(final Activity activityContext, int mode, final int simId) {
        MmsLog.d(TAG, "showActivitionDlg(): context = " + activityContext + ", mode = " + mode
            + ", simId = " + simId);
        LayoutInflater factory = LayoutInflater.from(activityContext);
        final View view = factory.inflate(R.layout.alert_dialog_text_view, null);
        TextView textView = (TextView) view.findViewById(R.id.term_textview);
        String termContent = IpMessageUtils.getResourceManager(activityContext)
            .getSingleString(WELCOME == mode ? IpMessageConsts.string.ipmsg_term_warn_welcome
                : IpMessageConsts.string.ipmsg_term_warn_activate);
        SpannableString ss = new SpannableString(termContent);
        ss.setSpan(new URLSpan("noting"),
                    termContent.indexOf(IpMessageUtils.getResourceManager(activityContext)
                                                .getSingleString(IpMessageConsts.string.ipmsg_term_key)),
                    termContent.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(ss);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        CharSequence text = textView.getText();
        if (text instanceof Spannable) {
            int end = text.length();
            Spannable sp = (Spannable) textView.getText();
            URLSpan[] urls = sp.getSpans(0, end, URLSpan.class);
            SpannableStringBuilder style = new SpannableStringBuilder(text);
            style.clearSpans();// should clear old spans
            for (URLSpan url : urls) {
                MY_URL_SPAN.setActivity(activityContext);
                MY_URL_SPAN.setSimId(simId);
                style.setSpan(MY_URL_SPAN, sp.getSpanStart(url), sp.getSpanEnd(url), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            textView.setText(style);
        }
        sActivationDialog = new AlertDialog.Builder(activityContext)
            .setTitle(IpMessageUtils.getResourceManager(activityContext)
                .getSingleString(WELCOME == mode ? IpMessageConsts.string.ipmsg_welcome_active
                    : IpMessageConsts.string.ipmsg_active))
            .setView(view)
            .setPositiveButton(IpMessageUtils.getResourceManager(activityContext)
                    .getSingleString(IpMessageConsts.string.ipmsg_agree_and_continue),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        MmsLog.d(TAG, "showActivitionDlg(): agree and continue, simId = " + simId);
                        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                            Intent intent = new Intent(RemoteActivities.ACTIVITION);
                            intent.putExtra(RemoteActivities.KEY_SIM_ID, simId);
                            startRemoteActivity(activityContext, intent);
                        } else {
                            Intent intent = new Intent(RemoteActivities.ACTIVITION);
                            intent.putExtra(RemoteActivities.KEY_SIM_ID, SpecialSimId.ALL_SIM_ID);
                            startRemoteActivity(activityContext, intent);
                        }
                    }
                }
            )
            .setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    MmsLog.d(TAG, "showActivitionDlg(): cancel.");
                }
            })
            .create();
        sActivationDialog.show();
    }

    private static class MyURLSpan extends ClickableSpan {
        private Activity mActivity;
        private int mSimId = 0;

        public void setActivity(Activity activity) {
            mActivity = activity;
        }
        public void setSimId(int simId) {
            mSimId = simId;
        }
        @Override
        public void onClick(View widget) {
            MmsLog.d(TAG, "MyURLSpan.onClick(): context = " + mActivity + ", simId = " + mSimId);
            if (sActivationDialog != null) {
                sActivationDialog.dismiss();
                sActivationDialog = null;
            }
            /// M: start term activity
            Intent intent = new Intent(RemoteActivities.TERM);
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                MmsLog.d(TAG, "MyURLSpan.onClick(): dual SIM");
                intent.putExtra(RemoteActivities.KEY_SIM_ID, mSimId);
            } else {
                MmsLog.d(TAG, "MyURLSpan.onClick(): single SIM");
                intent.putExtra(RemoteActivities.KEY_SIM_ID, SpecialSimId.ALL_SIM_ID);
            }
            MmsLog.d(TAG, "MyURLSpan.onClick(): start term activity. action = " + RemoteActivities.TERM
                + ", simId = " + mSimId);
            startRemoteActivity(mActivity, intent);
        }
    }

    private static void showSimSelectionDialog(final Activity activityContext, List<SIMInfo> simInfoList,
            final int mode, final Handler ipMsgHandler) {
        MmsLog.d(TAG, "showSimSelectionDialogForActivation(): context = " + activityContext + ", mode = " + mode);
        if (simInfoList.size() == 0) {
            MmsLog.d(TAG, "showSimSelectionDialogForActivation(): No SIM card!");
            String content = IpMessageUtils.getResourceManager(activityContext)
                                            .getSingleString(IpMessageConsts.string.ipmsg_no_sim_card);
            Toast.makeText(activityContext, content, Toast.LENGTH_LONG).show();
            return;
        } else if (simInfoList.size() == 1) {
            MmsLog.d(TAG, "showSimSelectionDialogForActivation(): One SIM card!");
            Intent intent = new Intent(RemoteActivities.ACTIVITION);
            intent.putExtra(RemoteActivities.KEY_SIM_ID, (int) simInfoList.get(0).getSimId());
            startRemoteActivity(activityContext, intent);
            return;
        }
        MmsLog.d(TAG, "showSimSelectionDialogForActivation(): Two SIM cards! Show SIM selection dialog.");
        List<Map<String, ?>> entries = new ArrayList<Map<String, ?>>();
        for (int i = 0; i < simInfoList.size(); i++) {
            SIMInfo simInfo = simInfoList.get(i);
            HashMap<String, Object> entry = new HashMap<String, Object>();

            entry.put("simIcon", simInfo.getSimBackgroundLightRes());
            int state = MessageUtils.getSimStatus(i, simInfoList, EncapsulatedTelephonyManagerEx.getDefault());
            entry.put("simStatus", MessageUtils.getSimStatusResource(state));
            if (MmsConfig.isServiceEnabled(activityContext, (int) simInfo.getSimId())) {
                MmsLog.d(TAG, "show ipmessage icon, simId = " + simInfo.getSimId());
                entry.put("ipmsg_indicator", IpMessageConsts.drawable.ipmsg_sim_indicator);
            } else {
                MmsLog.d(TAG, "hide ipmessage icon, simId = " + simInfo.getSimId());
                entry.put("ipmsg_indicator", 0);
            }
            String simNumber = "";
            if (!TextUtils.isEmpty(simInfo.getNumber())) {
                switch (simInfo.getDispalyNumberFormat()) {
                    //case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_DEFAULT:
                    case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_FIRST:
                        if (simInfo.getNumber().length() <= 4) {
                            simNumber = simInfo.getNumber();
                        } else {
                            simNumber = simInfo.getNumber().substring(0, 4);
                        }
                        break;
                    case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_LAST:
                        if (simInfo.getNumber().length() <= 4) {
                            simNumber = simInfo.getNumber();
                        } else {
                            simNumber = simInfo.getNumber().substring(simInfo.getNumber().length() - 4);
                        }
                        break;
                    case 0://android.provider.Telephony.SimInfo.DISPLAY_NUMBER_NONE:
                        simNumber = "";
                        break;
                    default:
                        break;
                }
            }
            if (!TextUtils.isEmpty(simNumber)) {
                entry.put("simNumberShort",simNumber);
            } else {
                entry.put("simNumberShort", "");
            }
            entry.put("simName", simInfo.getDisplayName());
            if (!TextUtils.isEmpty(simInfo.getNumber())) {
                entry.put("simNumber", simInfo.getNumber());
            } else {
                entry.put("simNumber", "");
            }
            entry.put("suggested", "");
            entries.add(entry);
        }
        final SimpleAdapter a = MessageUtils.createSimpleAdapter(entries, activityContext);
        final List<SIMInfo> currentSimInfoList = simInfoList;
        AlertDialog.Builder b = new AlertDialog.Builder(activityContext);
        b.setTitle(mode == SIM_SELECTION_MODE_ACTIVATION ?
            IpMessageUtils.getResourceManager(activityContext)
                .getSingleString(IpMessageConsts.string.ipmsg_sim_selected_dialog_title_for_activate) :
            IpMessageUtils.getResourceManager(activityContext)
                .getSingleString(IpMessageConsts.string.ipmsg_sim_selected_dialog_title_for_enable));
        b.setCancelable(true);
        b.setAdapter(a, new DialogInterface.OnClickListener() {
            @SuppressWarnings("unchecked")
            public final void onClick(DialogInterface dialog, int which) {
                switch (mode) {
                case SIM_SELECTION_MODE_ACTIVATION:
                    showActivitionDlg(activityContext, ACTIVATE, (int) currentSimInfoList.get(which).getSimId());
                    break;
                case SIM_SELECTION_MODE_ENABLED:
                    getServiceManager(activityContext).enableIpService((int) currentSimInfoList.get(which).getSimId());
                    Toast.makeText(activityContext, IpMessageUtils.getResourceManager(activityContext)
                            .getSingleString(IpMessageConsts.string.ipmsg_current_sim_enabled),
                        Toast.LENGTH_LONG).show();
                    if (ipMsgHandler != null) {
                        Message message = new Message();
                        message.what = MessageUtils.UPDATE_SENDBUTTON;
                        ipMsgHandler.sendMessage(message);
                    }
                    break;
                default:
                    break;
                }
                dialog.dismiss();
            }
        });
        b.create().show();
    }

    private static void showActivateOrEnableIpMessageServiceDialog(final Activity activityContext,
            final List<SIMInfo> simInfoList, final int simId, final int mode, final Handler ipMsgHandler) {
        MmsLog.d(TAG, "showEnableIpMessageServiceDialog(): context = " + activityContext
            + ", simInfoList = " + simInfoList + ", simId = " + simId);
        String title = mode == SIM_SELECTION_MODE_ACTIVATION ?
            IpMessageUtils.getResourceManager(activityContext)
                .getSingleString(IpMessageConsts.string.ipmsg_activate_title) :
            IpMessageUtils.getResourceManager(activityContext)
                .getSingleString(IpMessageConsts.string.ipmsg_enable_title);
        String message = mode == SIM_SELECTION_MODE_ACTIVATION ?
            IpMessageUtils.getResourceManager(activityContext)
                .getSingleString(IpMessageConsts.string.ipmsg_activate_message) :
            IpMessageUtils.getResourceManager(activityContext)
                .getSingleString(IpMessageConsts.string.ipmsg_enable_message);
        AlertDialog enableDialog = new AlertDialog.Builder(activityContext)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(IpMessageUtils.getResourceManager(activityContext)
                    .getSingleString(IpMessageConsts.string.ipmsg_agree_and_continue),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        switch (mode) {
                        case SIM_SELECTION_MODE_ACTIVATION:
                            if (simId > 0) {
                                showActivitionDlg(activityContext, ACTIVATE, simId);
                            } else if (simId == SINGLE_SIM) {
                                showActivitionDlg(activityContext, ACTIVATE, SINGLE_SIM);
                            } else if (simId == DUAL_SIM) {
                                showSimSelectionDialog(activityContext, simInfoList, SIM_SELECTION_MODE_ACTIVATION,
                                    ipMsgHandler);
                            }
                            break;
                        case SIM_SELECTION_MODE_ENABLED:
                            if (simId > 0) {
                                getServiceManager(activityContext).enableIpService(simId);
                                Toast.makeText(activityContext, IpMessageUtils.getResourceManager(activityContext)
                                        .getSingleString(IpMessageConsts.string.ipmsg_current_sim_enabled),
                                    Toast.LENGTH_LONG).show();
                            } else if (simId == SINGLE_SIM) {
                                getServiceManager(activityContext).enableIpService();
                                Toast.makeText(activityContext, IpMessageUtils.getResourceManager(activityContext)
                                        .getSingleString(IpMessageConsts.string.ipmsg_current_sim_enabled),
                                    Toast.LENGTH_LONG).show();
                            } else if (simId == DUAL_SIM) {
                                showSimSelectionDialog(activityContext, simInfoList, SIM_SELECTION_MODE_ENABLED,
                                    ipMsgHandler);
                            }
                            if (ipMsgHandler != null) {
                                Message message = new Message();
                                message.what = MessageUtils.UPDATE_SENDBUTTON;
                                ipMsgHandler.sendMessage(message);
                            }
                            break;
                        default:
                            break;
                        }
                    }
            })
            .setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                }
            })
            .create();
        enableDialog.show();
    }

    private static void showSwitchSimDialog(final Activity activityContext, final long simId) {
        MmsLog.d(TAG, "showSwitchSimDialog(): context = " + activityContext + ", simId = " + simId);
        AlertDialog switchDialog = new AlertDialog.Builder(activityContext)
            .setTitle(IpMessageUtils.getResourceManager(activityContext)
                .getSingleString(IpMessageConsts.string.ipmsg_switch_sim_title))
            .setMessage(IpMessageUtils.getResourceManager(activityContext)
                .getSingleString(IpMessageConsts.string.ipmsg_switch_sim_message))
            .setPositiveButton(IpMessageUtils.getResourceManager(activityContext)
                .getSingleString(IpMessageConsts.string.ipmsg_switch_sim_button),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Settings.System.putLong(activityContext.getContentResolver(),
                            Settings.System.SMS_SIM_SETTING, simId);
                        Intent intent = new Intent(Intent.ACTION_SMS_DEFAULT_SIM_CHANGED);
                        intent.putExtra("simid", simId);
                        activityContext.sendBroadcast(intent);
                        Toast.makeText(MmsApp.getApplication(), IpMessageUtils.getResourceManager(activityContext)
                                .getSingleString(IpMessageConsts.string.ipmsg_switch_sim_successfully),
                            Toast.LENGTH_LONG).show();
                    }
                }
            )
            .setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                }
            })
            .create();
        switchDialog.show();
    }

    public static String getThreadListString(Collection<Long> threads) {
        StringBuilder threadList = new StringBuilder();
        for (long thread : threads) {
            threadList.append(thread);
            threadList.append(",");
        }
        MmsLog.d(TAG, "threadList:" + threadList);
        return threadList.substring(0, threadList.length() - 1);
    }

    public static void deleteIpMessage(Context ct, Collection<Long> threads, boolean delImportant, int maxSmsId) {
        long[] ids;
        String threadList;
        String selection = Sms.IPMSG_ID + " > 0 AND " + Sms._ID + " <= " + maxSmsId;
        if (threads != null) {
            if (threads.size() < 1) {
                MmsLog.w(TAG, "threads list is empty!");
                return;
            }
            threadList = getThreadListString(threads);
            MmsLog.d(TAG, "threadList:" + threadList);
            selection += " AND " + Sms.THREAD_ID + " IN (" + threadList + ")";
        }
        Cursor cursor = null;
        cursor = SqliteWrapper.query(ct, ct.getContentResolver(),
                Sms.CONTENT_URI, new String[]{Sms._ID}, selection, null, null);
        if (cursor != null) {
            ids = new long[cursor.getCount()];
            int i = 0;
            while (cursor.moveToNext()) {
                ids[i++] = cursor.getLong(0);
                MmsLog.d(TAG, "id" + (i - 1) + ":" + ids[i - 1]);
            }
            cursor.close();
        } else {
            MmsLog.w(TAG, "delete ipmessage query get cursor null!");
            return;
        }
        getMessageManager(ct).deleteIpMsg(ids, delImportant, false);
    }

    public static boolean isValidAttach(String path, boolean inspectSize) {
        if (!isExistsFile(path) || getFileSize(path) == 0) {
            MmsLog.e(TAG, "isValidAttach: file is not exist, or size is 0");
            return false;
        }
        if (inspectSize && getFileSize(path) > IpMessageConfig.MAX_ATTACH_SIZE) {
            MmsLog.e(TAG, "file size is too large");
            return false;
        }
        return true;
    }

    public static void createLoseSDCardNotice(Context context, int resId) {
        new AlertDialog.Builder(context)
                .setTitle(getResourceManager(context).getSingleString(IpMessageConsts.string.ipmsg_no_sdcard))
                .setMessage(resId)
                .setPositiveButton(getResourceManager(context).getSingleString(IpMessageConsts.string.ipmsg_cancel), null)
                .create().show();
        
    }

    public static String formatFileSize(int size) {
        String result = "";
        int oneMb = 1024 * 1024;
        int oneKb = 1024;
        if (size > oneMb) {
            int s = size % oneMb / 100;
            if (s == 0) {
                result = size / oneMb + "MB";
            } else {
                result = size / oneMb + "." + s + "MB";
            }
        } else if (size > oneKb) {
            int s = size % oneKb / 100;
            if (s == 0) {
                result = size / oneKb + "KB";
            } else {
                result = size / oneKb + "." + s + "KB";
            }
        } else if (size > 0) {
            result = size + "B";
        } else {
            result = "invalid size";
        }
        return result;
    }

    public static String formatAudioTime(int duration) {
        String result = "";
        if (duration > 60) {
            if (duration % 60 == 0) {
                result = duration / 60 + "'";
            } else {
                result = duration / 60 + "'" + duration % 60 + "\"";
            }
        } else if (duration > 0) {
            result = duration + "\"";
        } else {
            // TODO IP message replace this string with resource
            result = "no duration";
        }
        return result;
    }

    public static boolean shouldShowTimeDivider(long curTime, long nextTime) {
        Date curDate = new Date(curTime);
        Date nextDate = new Date(nextTime);
        Date cur = new Date(curDate.getYear(), curDate.getMonth(), curDate.getDate(), 0, 0, 0);
        Date next = new Date(nextDate.getYear(), nextDate.getMonth(), nextDate.getDate(), 0, 0, 0);
        return (cur.getTime() != next.getTime());
    }

    public static String getShortTimeString(Context context, long time) {
        int formatFlags = DateUtils.FORMAT_NO_NOON_MIDNIGHT
                | DateUtils.FORMAT_CAP_AMPM;
        formatFlags |= DateUtils.FORMAT_SHOW_TIME;
        return DateUtils.formatDateTime(context, time, formatFlags);

    }

    public static String getTimeDividerString(Context context, long when) {
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();

        // Basic settings for formatDateTime() we want for all cases.
        int formatFlags = DateUtils.FORMAT_NO_NOON_MIDNIGHT | DateUtils.FORMAT_ABBREV_ALL
                | DateUtils.FORMAT_CAP_AMPM;

        // If the message is from a different year, show the date and year.
        if (then.year != now.year) {
            formatFlags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            // If it is from a different day than today, show only the date.
            formatFlags |= DateUtils.FORMAT_SHOW_DATE;
            Date curDate = new Date();
            Date cur = new Date(curDate.getYear(), curDate.getMonth(), curDate.getDate(), 0, 0, 0);
            long oneDay = 24 * 60 * 60 * 1000;
            long elapsedTime = cur.getTime() - when;
            if (elapsedTime < oneDay && elapsedTime > 0) {
                return context.getResources().getString(R.string.str_ipmsg_yesterday);
            }
        } else {
            return context.getString(R.string.str_ipmsg_today);
        }
        return DateUtils.formatDateTime(context, when, formatFlags);
    }

    private static String getDefaultFM(Context context, boolean bThisYear) {
        String fm;
        char[] order = DateFormat.getDateFormatOrder(context);
        if (order != null) {
            if (bThisYear) {
                if (order[0] == 'y' || order[0] == 'Y') {
                    fm = "" + order[1] + order[1] + "/" + order[2] + order[2];
                } else {
                    fm = "" + order[0] + order[0] + "/" + order[1] + order[1];
                }
            } else {
                fm = "" + order[0] + order[0] + "/" + order[1] + order[1] + "/" + order[2]
                        + order[2];
            }
        } else {
            fm = "MM/DD";
        }
        return fm;
    }


    public static boolean isFileStatusOk(Context context, String path) {
        if (TextUtils.isEmpty(path)) {
            Toast.makeText(context, getResourceManager(context)
                .getSingleString(IpMessageConsts.string.ipmsg_no_such_file), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!isExistsFile(path)) {
            Toast.makeText(context, getResourceManager(context)
                .getSingleString(IpMessageConsts.string.ipmsg_no_such_file), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (getFileSize(path) > (2 * 1024 * 1024)) {
            Toast.makeText(context, getResourceManager(context)
                .getSingleString(IpMessageConsts.string.ipmsg_over_file_limit), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public static boolean isPic(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        String path = name.toLowerCase();
        if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg")
                || path.endsWith(".bmp") || path.endsWith(".gif")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isVideo(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        String path = name.toLowerCase();
        if (path.endsWith(".mp4") || path.endsWith(".3gp")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isAudio(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        String extArrayString[] = {".amr", ".ogg", ".mp3", ".aac", ".ape", ".flac", ".wma", ".wav", ".mp2", ".mid"};
        String path = name.toLowerCase();
        for (String ext : extArrayString) {
            if (path.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public static IpMessage readIpMessageDraft(Context context, Conversation conv, WorkingMessage workingMessage) {
        MmsLog.d(TAG, "readIpMessageDraft(): threadId = " + conv.getThreadId());
        long threadId = conv.getThreadId();

        // If it's an invalid thread or we know there's no draft, don't bother.
        if (threadId <= 0) {
            MmsLog.d(TAG, "readDraftIpMessage(): no draft, threadId = " + threadId);
            return null;
        }

        Uri threadUri = ContentUris.withAppendedId(Sms.Conversations.CONTENT_URI, threadId);
        String body = "";

        Cursor c = SqliteWrapper.query(context, context.getContentResolver(), threadUri,
            SMS_BODY_PROJECTION_WITH_IPMSG_ID, SMS_DRAFT_WHERE, null, null);
        long msgId = 0L;
        long ipMsgId = 0L;
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    ipMsgId = c.getLong(SMS_IPMSG_ID_INDEX);
                    if (ipMsgId > 0) {
                        msgId = c.getLong(SMS_ID_INDEX);
                    }
                }
            } finally {
                c.close();
            }
        }

        if (msgId > 0 && ipMsgId > 0) {
            IpMessage ipMessage = getMessageManager(context).getIpMsgInfo(msgId);
            /// M: a draft sms must be deleted after loaded. a new record will created when save.
            workingMessage.asyncDeleteDraftSmsMessage(conv);
            //getChatManager(context).deleteDraftMessageInThread(conv.getThreadId());
            if (ipMessage != null) {
                ipMessage.setStatus(IpMessageStatus.OUTBOX);
                workingMessage.clearConversation(conv, true);
                MmsLog.d(TAG, "readIpMessageDraft(): Get IP message draft, msgId = " + msgId);
                return ipMessage;
            }
        }
        MmsLog.d(TAG, "readIpMessageDraft(): No IP message draft, msgId = " + msgId);
        return null;
    }

    public static boolean deleteIpMessageDraft(Context context, Conversation conv, WorkingMessage workingMessage) {
        MmsLog.d(TAG, "deleteIpMessageDraft(): threadId = " + conv.getThreadId());
        long threadId = conv.getThreadId();

        // If it's an invalid thread or we know there's no draft, don't bother.
        if (threadId <= 0) {
            MmsLog.d(TAG, "deleteIpMessageDraft(): no draft, threadId = " + threadId);
            return false;
        }

        Uri threadUri = ContentUris.withAppendedId(Sms.Conversations.CONTENT_URI, threadId);
        Cursor c = SqliteWrapper.query(context, context.getContentResolver(), threadUri, SMS_BODY_PROJECTION_WITH_IPMSG_ID,
            SMS_DRAFT_WHERE, null, null);
        long msgId = 0L;
        long ipMsgId = 0L;
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    ipMsgId = c.getLong(SMS_IPMSG_ID_INDEX);
                    if (ipMsgId > 0) {
                        msgId = c.getLong(SMS_ID_INDEX);
                    }
                }
            } finally {
                c.close();
            }
        }

        if (msgId > 0) {
            if (ipMsgId > 0) {
                getMessageManager(context).deleteIpMsg(new long[]{ msgId }, true, true);
                MmsLog.d(TAG, "deleteIpMessageDraft(): Delete IP message draft, msgId = " + msgId);
            } else {
                workingMessage.asyncDeleteDraftSmsMessage(conv);
            }
            return true;
        }
        MmsLog.d(TAG, "deleteIpMessageDraft(): No IP message draft, msgId = " + msgId);
        return false;
    }

    public static String getIpMessageCaption(IpMessage ipMessage) {
        MmsLog.d(TAG, "getIpMessageCaption()");
        String caption = "";
        int type = ipMessage.getType();
        switch (type) {
        case IpMessageType.PICTURE:
            caption = ((IpImageMessage) ipMessage).getCaption();
            MmsLog.d(TAG, "getIpMessageCaption(): Get pic caption, caption = " + caption);
            break;
        case IpMessageType.VOICE:
            caption = ((IpVoiceMessage) ipMessage).getCaption();
            MmsLog.d(TAG, "getIpMessageCaption(): Get audio caption, caption = " + caption);
            break;
        case IpMessageType.VIDEO:
            caption = ((IpVideoMessage) ipMessage).getCaption();
            MmsLog.d(TAG, "getIpMessageCaption(): Get video caption, caption = " + caption);
            break;
        case IpMessageType.TEXT:
        case IpMessageType.VCARD:
        case IpMessageType.LOCATION:
        case IpMessageType.SKETCH:
        case IpMessageType.CALENDAR:
        case IpMessageType.GROUP_CREATE_CFG:
        case IpMessageType.GROUP_ADD_CFG:
        case IpMessageType.GROUP_QUIT_CFG:
        case IpMessageType.UNKNOWN_FILE:
        case IpMessageType.COUNT:
            break;
        default:
            break;
        }
        return caption;
    }

    public static IpMessage setIpMessageCaption(IpMessage ipMessage, String caption) {
        int type = ipMessage.getType();
        switch (type) {
        case IpMessageType.PICTURE:
            ((IpImageMessage) ipMessage).setCaption(caption);
            break;
        case IpMessageType.VOICE:
            ((IpVoiceMessage) ipMessage).setCaption(caption);
            break;
        case IpMessageType.VIDEO:
            ((IpVideoMessage) ipMessage).setCaption(caption);
            break;
        case IpMessageType.TEXT:
        case IpMessageType.VCARD:
        case IpMessageType.LOCATION:
        case IpMessageType.SKETCH:
        case IpMessageType.CALENDAR:
        case IpMessageType.GROUP_CREATE_CFG:
        case IpMessageType.GROUP_ADD_CFG:
        case IpMessageType.GROUP_QUIT_CFG:
        case IpMessageType.UNKNOWN_FILE:
        case IpMessageType.COUNT:
            break;
        default:
            break;
        }
        return ipMessage;
    }

    public static int getIpMessageStatusResourceId(int status) {
        int id = 0;
        if (status == IpMessageStatus.OUTBOX) {
            id = R.drawable.im_meg_status_sending;
        } else if (status == IpMessageStatus.SENT) {
            id = R.drawable.im_meg_status_out;
        } else if (status == IpMessageStatus.DELIVERED) {
            id = R.drawable.im_meg_status_reach;
        } else if (status == IpMessageStatus.FAILED) {
            id = R.drawable.ic_list_alert_sms_failed;
        } else if (status == IpMessageStatus.VIEWED) {
            id = R.drawable.im_meg_status_read;
        } else if (status == IpMessageStatus.NOT_DELIVERED) {
            id = R.drawable.ic_list_alert_sms_failed;
        }
        return id;
    }

    public static final int UNCONSTRAINED = -1;
    public static final String IP_MESSAGE_FILE_PATH = File.separator + IpMessageConfig.ROOTDIRECTORY + File.separator;
    public static final String CACHE_PATH = File.separator + IpMessageConfig.ROOTDIRECTORY + "/Cache/";
    public static String getCachePath(Context c) {
        String path = null;
        String sdCardPath = getSDCardPath(c);
        if (!TextUtils.isEmpty(sdCardPath)) {
            path = sdCardPath + CACHE_PATH;
        }
        return path;
    }

    public static String getMemPath(Context c) {
        return c.getFilesDir().getAbsolutePath();
    }

    public static String getSDCardPath(Context c) {
        File sdDir = null;
        String sdStatus = Environment.getExternalStorageState();

        if (TextUtils.isEmpty(sdStatus)) {
            return c.getFilesDir().getAbsolutePath();
        }

        boolean sdCardExist = sdStatus.equals(android.os.Environment.MEDIA_MOUNTED);

        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
            return sdDir.toString();
        }

        return c.getFilesDir().getAbsolutePath();
    }

    public static boolean getSDCardStatus() {
        boolean ret = false;
        String sdStatus = Environment.getExternalStorageState();
        MmsLog.d(TAG, "getSDCardStatus(): sdStatus = " + sdStatus);
        if (sdStatus.equals(Environment.MEDIA_MOUNTED)) {
            ret = true;
        }
        return ret;
    }

    public static int getExifOrientation(String filepath) {
        int degree = 0;
        ExifInterface exif = null;

        try {
            exif = new ExifInterface(filepath);
        } catch (IOException ex) {
            MmsLog.e(TAG, "getExifOrientation():", ex);
        }

        if (exif != null) {
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            if (orientation != -1) {
                // We only recognize a subset of orientation tag values.
                switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
                default:
                    break;
                }
            }
        }

        return degree;
    }

    public static Bitmap rotate(Bitmap b, int degrees) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) b.getWidth() / 2, (float) b.getHeight() / 2);
            try {
                Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b != b2) {
                    b.recycle();
                    b = b2;
                }
            } catch (OutOfMemoryError ex) {
                // We have no memory to rotate. Return the original bitmap.
                MmsLog.w(TAG, "OutOfMemoryError.");
            }
        }

        return b;
    }

    public static Bitmap resizeImage(Bitmap bitmap, int w, int h, boolean needRecycle) {
        if (null == bitmap) {
            return null;
        }

        Bitmap bitmapOrg = bitmap;
        int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();
        int newWidth = w;
        int newHeight = h;

        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, width, height, matrix, true);
        if (needRecycle && !bitmapOrg.isRecycled() && bitmapOrg != resizedBitmap) {
            bitmapOrg.recycle();
        }
        return resizedBitmap;
    }

    public static byte[] resizeImg(String path, float maxLength) {
        int d = getExifOrientation(path);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        options.inJustDecodeBounds = false;

        int l = Math.max(options.outHeight, options.outWidth);
        int be = (int) (l / maxLength);
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;

        bitmap = BitmapFactory.decodeFile(path, options);
        if (null == bitmap) {
            return null;
        }
        if (d != 0) {
            bitmap = rotate(bitmap, d);
        }

        String[] tempStrArry = path.split("\\.");
        String filePostfix = tempStrArry[tempStrArry.length - 1];
        CompressFormat formatType = null;
        if (filePostfix.equalsIgnoreCase("PNG")) {
            formatType = Bitmap.CompressFormat.PNG;
        } else if (filePostfix.equalsIgnoreCase("JPG") || filePostfix.equalsIgnoreCase("JPEG")) {
            formatType = Bitmap.CompressFormat.JPEG;
        } else if (filePostfix.equalsIgnoreCase("GIF")) {
            formatType = Bitmap.CompressFormat.PNG;
        } else if (filePostfix.equalsIgnoreCase("BMP")) {
            formatType = Bitmap.CompressFormat.PNG;
        } else {
            MmsLog.e(TAG, "resizeImg(): Can't compress the image,because can't support the format:" + filePostfix);
            return null;
        }

        int quality = 100;
        if (be == 1) {
            if (getFileSize(path) > 50 * 1024) {
                quality = 30;
            }
        } else {
            quality = 30;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(formatType, quality, baos);
        final byte[] tempArry = baos.toByteArray();
        if (baos != null) {
            try {
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            baos = null;
        }

        return tempArry;
    }

    public static boolean isExistsFile(String filepath) {
        try {
            if (TextUtils.isEmpty(filepath)) {
                return false;
            }
            File file = new File(filepath);
            return file.exists();
        } catch (Exception e) {
            MmsLog.e(TAG, "isExistsFile():", e);
            return false;
        }
    }

    public static int getFileSize(String filepath) {
        try {
            if (TextUtils.isEmpty(filepath)) {
                return -1;
            }
            File file = new File(filepath);
            return (int) file.length();
        } catch (Exception e) {
            MmsLog.e(TAG, "getFileSize():", e);
            return -1;
        }
    }

    public static void copy(String src, String dest) {
        InputStream is = null;
        OutputStream os = null;

        File out = new File(dest);
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }

        try {
            is = new BufferedInputStream(new FileInputStream(src));
            os = new BufferedOutputStream(new FileOutputStream(dest));

            byte[] b = new byte[256];
            int len = 0;
            try {
                while ((len = is.read(b)) != -1) {
                    os.write(b, 0, len);

                }
                os.flush();
            } catch (IOException e) {
                MmsLog.e(TAG, "", e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        MmsLog.e(TAG, "", e);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            MmsLog.e(TAG, "", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    MmsLog.e(TAG, "", e);
                }
            }
        }
    }

    /**
     *
     *
     * @param stream
     * @return
     */
    public static void nmsStream2File(byte[] stream, String filepath) throws Exception {
        FileOutputStream outStream = null;
        try {
            File f = new File(filepath);
            if (!f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            outStream = new FileOutputStream(f);
            outStream.write(stream);
            outStream.flush();
        } catch (IOException e) {
            MmsLog.e(TAG, "nmsStream2File():", e);
            throw new RuntimeException(e.getMessage());
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                    outStream = null;
                } catch (IOException e) {
                    MmsLog.e(TAG, "nmsStream2File():", e);
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
    }

    private static Rect getScreenRegion(int width, int height) {
        return new Rect(0, 0, width, height);
    }

    /**
     * get inSampleSize.
     *
     * @param options
     * @param minSideLength
     * @param maxNumOfPixels
     * @return
     */
    public static int computeSampleSize(BitmapFactory.Options options, int minSideLength,
            int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);

        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength,
            int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = (maxNumOfPixels == UNCONSTRAINED) ? 1 : (int) Math.ceil(Math.sqrt(w * h
                / maxNumOfPixels));
        int upperBound = (minSideLength == UNCONSTRAINED) ? 128 : (int) Math.min(
                Math.floor(w / minSideLength), Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            return lowerBound;
        }

        if ((maxNumOfPixels == UNCONSTRAINED) && (minSideLength == UNCONSTRAINED)) {
            return 1;
        } else if (minSideLength == UNCONSTRAINED) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    /**
     * Get bitmap
     *
     * @param path
     * @param options
     * @return
     */
    public static Bitmap getBitmapByPath(String path, Options options, int width, int height) {
        if (TextUtils.isEmpty(path) || width <= 0 || height <= 0) {
            MmsLog.w(TAG, "parm is error.");
            return null;
        }

        File file = new File(path);
        if (!file.exists()) {
            MmsLog.w(TAG, "file not exist!");
            return null;
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            MmsLog.e(TAG, "FileNotFoundException:" + e.toString());
        }
        if (options != null) {
            Rect r = getScreenRegion(width, height);
            int w = r.width();
            int h = r.height();
            int maxSize = w > h ? w : h;
            int inSimpleSize = computeSampleSize(options, maxSize, w * h);
            options.inSampleSize = inSimpleSize;
            options.inJustDecodeBounds = false;
        }
        Bitmap bm = null;
        try {
            bm = BitmapFactory.decodeStream(in, null, options);
        } catch (java.lang.OutOfMemoryError e) {
            MmsLog.e(TAG, "bitmap decode failed, catch outmemery error");
        }
        try {
            in.close();
        } catch (IOException e) {
            MmsLog.e(TAG, "IOException:" + e.toString());
        }
        return bm;
    }

    public static Options getOptions(String path) {
        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        return options;
    }
}
