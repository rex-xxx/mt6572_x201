package com.android.mms;

import android.content.Context;
import android.util.AndroidException;

import com.mediatek.mms.ext.IMmsCompose;
import com.mediatek.mms.ext.MmsComposeImpl;
import com.mediatek.encapsulation.com.mediatek.pluginmanager.EncapsulatedPluginManager;
import com.mediatek.mms.ext.DefaultAppGuideExt;
import com.mediatek.mms.ext.IAppGuideExt;
import com.mediatek.mms.ext.IMmsDialogNotify;
import com.mediatek.mms.ext.MmsDialogNotifyImpl;
import com.mediatek.mms.ext.IMmsTextSizeAdjust;
import com.mediatek.mms.ext.MmsTextSizeAdjustImpl;
import com.mediatek.mms.ext.IMmsTextSizeAdjustHost;
import com.mediatek.mms.ext.ISmsReceiver;
import com.mediatek.mms.ext.SmsReceiverImpl;
///M: add for Mms transaction plugin
import com.mediatek.mms.ext.IMmsTransaction;
import com.mediatek.mms.ext.MmsTransactionImpl;

import com.mediatek.encapsulation.MmsLog;

import com.mediatek.mms.ext.IMmsAttachmentEnhance;
import com.mediatek.mms.ext.MmsAttachmentEnhanceImpl;
public class MmsPluginManager {
    
    private static String TAG = "MmsPluginManager";

    public static final int MMS_PLUGIN_TYPE_DIALOG_NOTIFY = 0X0001;
    public static final int MMS_PLUGIN_TYPE_TEXT_SIZE_ADJUST = 0X0002;
    // M: fix bug ALPS00352897
    public static final int MMS_PLUGIN_TYPE_SMS_RECEIVER = 0X0003;
    
    public static final int MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE = 0X0005;
    ///M: add for Mms transaction plugin
    public static final int MMS_PLUGIN_TYPE_MMS_TRANSACTION = 0X0007;

    public static final int MMS_PLUGIN_TYPE_MMS_COMPOSE = 0X0008;

    private static IMmsTextSizeAdjust mMmsTextSizeAdjustPlugin = null;
    private static IMmsDialogNotify mMmsDialogNotifyPlugin = null;
    // M: fix bug ALPS00352897
    private static ISmsReceiver mSmsReceiverPlugin = null;
    private static IAppGuideExt mAppGuideExt = null;
    public static final int MMS_PLUGIN_TYPE_APPLICATION_GUIDE = 0X0004;
    private static IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = null;
    ///M: add for Mms transaction plugin
    private static IMmsTransaction mMmsTransactionPlugin = null;
    private static IMmsCompose mMmsComposePlugin = null;

    public static void initPlugins(Context context){

        //Dialog Notify
        try {
            mMmsDialogNotifyPlugin = (IMmsDialogNotify)EncapsulatedPluginManager.createPluginObject(context, IMmsDialogNotify.class.getName());
            MmsLog.d(TAG, "operator mMmsDialogNotifyPlugin = " + mMmsDialogNotifyPlugin);
        } catch (AndroidException e) {
            mMmsDialogNotifyPlugin = new MmsDialogNotifyImpl(context);
            MmsLog.d(TAG, "default mMmsDialogNotifyPlugin = " + mMmsDialogNotifyPlugin);
        }
        
        //TextSizeAdjust plugin
        try{
            mMmsTextSizeAdjustPlugin = (IMmsTextSizeAdjust)EncapsulatedPluginManager.createPluginObject(context, IMmsTextSizeAdjust.class.getName());
            MmsLog.d(TAG, "operator mMmsTextSizeAdjustPlugin = " + mMmsTextSizeAdjustPlugin);
        }catch(AndroidException e) {
            mMmsTextSizeAdjustPlugin = new MmsTextSizeAdjustImpl(context);
            MmsLog.d(TAG, "default mMmsTextSizeAdjustPlugin = " + mMmsTextSizeAdjustPlugin);
        }

        // M: fix bug ALPS00352897
        //SmsReceiver plugin
        try{
            mSmsReceiverPlugin = (ISmsReceiver)EncapsulatedPluginManager.createPluginObject(context, ISmsReceiver.class.getName());
            MmsLog.d(TAG, "operator mSmsReceiverPlugin = " + mSmsReceiverPlugin);
        }catch(AndroidException e) {
            mSmsReceiverPlugin = new SmsReceiverImpl(context);
            MmsLog.d(TAG, "default mSmsReceiverPlugin = " + mSmsReceiverPlugin);
        }
        /// M: add for application guide. @{
        try {
            mAppGuideExt = (IAppGuideExt)EncapsulatedPluginManager.createPluginObject(context,
                    IAppGuideExt.class.getName());
        } catch (AndroidException e) {
            mAppGuideExt = new DefaultAppGuideExt();
            MmsLog.d(TAG,"default mAppGuideExt = " + mAppGuideExt);
        }
        /// @}
        //Mms attachment enhance plugin
        try {
            mMmsAttachmentEnhancePlugin =
            (IMmsAttachmentEnhance)EncapsulatedPluginManager.createPluginObject(context, IMmsAttachmentEnhance.class.getName());
            MmsLog.d(TAG, "operator mMmsAttachmentEnhancePlugin = " + mMmsAttachmentEnhancePlugin);
        } catch (AndroidException e) {
            mMmsAttachmentEnhancePlugin = new MmsAttachmentEnhanceImpl(context);
            MmsLog.d(TAG, "default mMmsAttachmentEnhancePlugin = " + mMmsAttachmentEnhancePlugin);
        }
        ///M: add for Mms transaction plugin
        try{
            mMmsTransactionPlugin = (IMmsTransaction)EncapsulatedPluginManager.createPluginObject(context, IMmsTransaction.class.getName());
            MmsLog.d(TAG, "operator mMmsTransactionPlugin = " + mMmsTransactionPlugin);
        } catch(AndroidException e) {
            mMmsTransactionPlugin = new MmsTransactionImpl(context);
            MmsLog.d(TAG, "default mMmsTransactionPlugin = " + mMmsTransactionPlugin);
        }
        ///@}
        /// M: add for Mms Compose plugin
        try {
            mMmsComposePlugin = (IMmsCompose)EncapsulatedPluginManager.createPluginObject(context,
                                 IMmsCompose.class.getName());
            MmsLog.d(TAG, "operator mMmsComposePlugin = " + mMmsComposePlugin);
        } catch (AndroidException e) {
            mMmsComposePlugin = new MmsComposeImpl(context);
        }
        MmsLog.d(TAG, "default mMmsComposePlugin = " + mMmsComposePlugin);
        ///@}
    }


    public static Object getMmsPluginObject(int type){
        Object obj = null;
        MmsLog.d(TAG,"getMmsPlugin, type = " + type);
        switch(type){
            
            case MMS_PLUGIN_TYPE_DIALOG_NOTIFY:
                obj = mMmsDialogNotifyPlugin;
                break;

            case MMS_PLUGIN_TYPE_TEXT_SIZE_ADJUST:
                obj = mMmsTextSizeAdjustPlugin;
                break;

            // M: fix bug ALPS00352897
            case MMS_PLUGIN_TYPE_SMS_RECEIVER:
                obj = mSmsReceiverPlugin;
                break;
            case MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE:
                obj = mMmsAttachmentEnhancePlugin;
                break;

            case MMS_PLUGIN_TYPE_APPLICATION_GUIDE:
                obj = mAppGuideExt;
                break;

            ///M: add for Mms transaction plugin
            case MMS_PLUGIN_TYPE_MMS_TRANSACTION:
                obj = mMmsTransactionPlugin;
                break;
            ///@}
            case MMS_PLUGIN_TYPE_MMS_COMPOSE:
                obj = mMmsComposePlugin;
                break;
            default:
                MmsLog.e(TAG, "getMmsPlugin, type = " + type + " don't exist");
        }
        return obj;
            
    }
}
