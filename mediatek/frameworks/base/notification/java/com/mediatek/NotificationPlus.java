/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.notification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.Set;

/**
 * A class that represents how a persistent notification is to be presented to
 * the user using <br>
 * Note: this is different from Notification, it just been shown if server care
 * it.
 */
public class NotificationPlus {
    private static final String TAG = "NotificationPlus";
    private static final boolean LOG = true;
    /**
     * @internal
     *
     * Broadcast Action: notification has been sent. Apps, who cares this
     * scenario, will show notification dialog automatically.
     */
    /* package */static final String ACTION_FULL_SCREEN_NOTIFY = "mediatek.intent.action.FULL_SCRENN_NOTIFY";
    /**
     * @internal
     *
     * Used as a string value for {@link #ACTION_FULL_SCREEN_NOTIFY} to told
     * NotificationManangerPlus the title of dialog.
     */
    /* package */static final String EXTRA_CONTENT_TITLE = "mediatek.intent.extra.content.title";
    /**
     * @internal
     *
     * Used as a string value for {@link #ACTION_FULL_SCREEN_NOTIFY} to told
     * NotificationManagerPlus the message of dialog.
     */
    /* package */static final String EXTRA_CONTENT_TEXT = "mediatek.intent.extra.content.text";
    /**
     * @internal
     *
     * Used as a String value for {@link #ACTION_FULL_SCREEN_NOTIFY} to told
     * NotificationManagerPlus how to show positive button's name. Note: will
     * not be used if manager diable it.
     */
    /* package */static final String EXTRA_BUTTON_NAME_POSITIVE = "mediatek.intent.extra.button.name.positive";
    /**
     * @internal
     *
     * Used as a String value for {@link #ACTION_FULL_SCREEN_NOTIFY} to told
     * NotificationManagerPlus how to show negative button's name. Note: will
     * not be used if manager diable it.
     */
    /* package */static final String EXTRA_BUTTON_NAME_NEGATIVE = "mediatek.intent.extra.button.name.negative";
    /**
     * @internal
     *
     * Used as a String value for {@link #ACTION_FULL_SCREEN_NOTIFY} to told
     * NotificationManagerPlus how to show neutral button's name. Note: will not
     * be used if manager diable it.
     */
    /* package */static final String EXTRA_BUTTON_NAME_NEUTRAL = "mediatek.intent.extra.button.name.neutral";
    /**
     * @internal
     *
     * Used as a PendingIntent value for {@link #ACTION_FULL_SCREEN_NOTIFY} to
     * told NotificationManagerPlus what will be done after user click the
     * positive button. Note: will not be used if manager diable it.
     */
    /* package */static final String EXTRA_BUTTON_INTENT_POSITIVE = "mediatek.intent.extra.button.intent.positive";
    /**
     * @internal
     *
     * Used as a PendingIntent value for {@link #ACTION_FULL_SCREEN_NOTIFY} to
     * told NotificationManagerPlus what will be done after user click the
     * negative button. Note: will not be used if manager diable it.
     */
    /* package */static final String EXTRA_BUTTON_INTENT_NEGATIVE = "mediatek.intent.extra.button.intent.negative";
    /**
     * @internal
     *
     * Used as a PendingIntent value for {@link #ACTION_FULL_SCREEN_NOTIFY} to
     * told NotificationManagerPlus what will be done after user click the
     * neutral button. Note: will not be used if manager diable it.
     */
    /* package */static final String EXTRA_BUTTON_INTENT_NEUTRAL = "mediatek.intent.extra.button.intent.neutral";
    /**
     * @internal
     *
     * Used as a PendingIntent value for {@link #ACTION_FULL_SCREEN_NOTIFY} to
     * told NotificationManagerPlus what will be done after user cancel the
     * notification. Note: will not be used if manager diable it.
     */
    /* package */static final String EXTRA_CANCEL_INTENT = "mediatek.intent.extra.cancel.intent";
    /**
     * @internal
     *
     * Used as a boolean value for {@link #ACTION_FULL_SCREEN_NOTIFY} to told
     * NotificationManagerPlus let user cancel the dialog or not. Note: will not
     * be used if manager diable it.
     */
    /* package */static final String EXTRA_CANCELABLE = "mediatek.intent.extra.cancel.enable";
    /* package */static final int TYPE_NOTIFY = 1;
    /* package */static final int TYPE_CANCEL = 2;
    /* package */static final int TYPE_UNKNOWN = -1;
    /* package */static final int ID_UNKNOWN = -1;
    /* package */static final String EXTRA_TYPE = "mediatek.intent.extra.type";
    /* package */static final String EXTRA_ID = "mediatek.intent.extra.id";
    /* package */static final String EXTRA_PACKAGE_NAME = "mediatek.intent.extra.package";

    /* package */Context mContext;
    /* package */Intent mIntent;

    /* package */NotificationPlus(Context context) {
        mContext = context;
        mIntent = new Intent(ACTION_FULL_SCREEN_NOTIFY);
        mIntent.addCategory(Intent.CATEGORY_DEFAULT);
        mIntent.putExtra(EXTRA_PACKAGE_NAME, mContext.getPackageName());
    }

    /* package */void setType(int type) {
        mIntent.putExtra(EXTRA_TYPE, type);
    }

    /* package */void setId(int id) {
        mIntent.putExtra(EXTRA_ID, id);
    }

    /* package */void send() {
        mContext.sendBroadcast(mIntent);
        if (LOG) {
            Log.i(TAG, "send() " + mIntent);
        }
        if (LOG) {
            Bundle extras = mIntent.getExtras();
            Set<String> keys = extras.keySet();
            for (String key : keys) {
                Log.i(TAG, "send() key=" + key + ", value=" + extras.get(key));
            }
        }
    }

    /**
     * @internal
     *
     * Helper class for building plus notification and notifing server.
     */
    public static class Builder {
        private NotificationPlus mNotification;

        /**
         * @internal
         *
         * Init the notification builder.
         */
        public Builder(Context context) {
            mNotification = new NotificationPlus(context);
        }

        /**
         * @internal
         *
         * Set the title for plus notification.
         * 
         * @param title Set the title of notification.
         * @return This Builder object to allow for chaining of calls to set
         *         methods.
         */
        public Builder setTitle(String title) {
            mNotification.mIntent.putExtra(EXTRA_CONTENT_TITLE, title);
            return this;
        }

        /**
         * @internal
         *
         * Set the message for plus notification.
         * 
         * @param message Set the message of notification.
         * @return This Builder object to allow for chaining of calls to set
         *         methods.
         */
        public Builder setMessage(String message) {
            mNotification.mIntent.putExtra(EXTRA_CONTENT_TEXT, message);
            return this;
        }

        /**
         * @internal
         *
         * Set the button text and pending action for positive button. <br>
         * Note: this will enable positive button function for server, unless
         * server don't disable this function.
         * 
         * @param name The display text of positive button.
         * @param pendingIntent The intent to be sent when positive button is clicked.
         * @return This Builder object to allow for chaining of calls to set
         *         methods.
         */
        public Builder setPositiveButton(String name, PendingIntent pendingIntent) {
            mNotification.mIntent.putExtra(EXTRA_BUTTON_NAME_POSITIVE, name);
            mNotification.mIntent.putExtra(EXTRA_BUTTON_INTENT_POSITIVE, pendingIntent);
            return this;
        }

        /**
         * @internal
         *
         * Set the button text and pending action for neutral button. <br>
         * Note: this will enable neutral button function for server, unless
         * server don't disable this function.
         * 
         * @param name The display text of neutral button.
         * @param pendingIntent The intent to be sent when neutral button is clicked.
         * @return This Builder object to allow for chaining of calls to set
         *         methods.
         */
        public Builder setNeutralButton(String name, PendingIntent pendingIntent) {
            mNotification.mIntent.putExtra(EXTRA_BUTTON_NAME_NEUTRAL, name);
            mNotification.mIntent.putExtra(EXTRA_BUTTON_INTENT_NEUTRAL, pendingIntent);
            return this;
        }

        /**
         * @internal
         *
         * Set the button text and pending action for negative button. <br>
         * Note: this will enable negative button function for server, unless
         * server don't disable this function.
         * 
         * @param name The display text of negative button.
         * @param pendingIntent The intent to be sent when negative button is clicked.
         * @return This Builder object to allow for chaining of calls to set
         *         methods.
         */
        public Builder setNegativeButton(String name, PendingIntent pendingIntent) {
            mNotification.mIntent.putExtra(EXTRA_BUTTON_NAME_NEGATIVE, name);
            mNotification.mIntent.putExtra(EXTRA_BUTTON_INTENT_NEGATIVE, pendingIntent);
            return this;
        }

        /**
         * @internal
         *
         * Sets whether the plus notification is cancelable or not. Default is
         * true. <br>
         * Note: this will enable cancelable function for server, unless server
         * don't disable this function.
         * 
         * @param cancelable Indicate if the notification item is cancelable or not.
         * @return This Builder object to allow for chaining of calls to set
         *         methods.
         */
        public Builder setCancelable(boolean cancelable) {
            mNotification.mIntent.putExtra(EXTRA_CANCELABLE, cancelable);
            return this;
        }

        /**
         * @internal
         *
         * Sets the callback that will be called if the plus notification is
         * canceled. <br>
         * Note: this will enable cancelable function for server, unless server
         * don't disable this function.
         * 
         * @see #setCancelable(boolean)
         * @param pendingIntent The intent to be sent when the notification is cancelled.
         * @return This Builder object to allow for chaining of calls to set
         *         methods.
         */
        public Builder setOnCancelListener(PendingIntent pendingIntent) {
            mNotification.mIntent.putExtra(EXTRA_CANCEL_INTENT, pendingIntent);
            return this;
        }

        /**
         * @internal
         *
         * Build the NotificationPlus object which was set up by setter functions.
         *
         * @return the done notifcation.
         */
        public NotificationPlus create() {
            return mNotification;
        }
    }

}
