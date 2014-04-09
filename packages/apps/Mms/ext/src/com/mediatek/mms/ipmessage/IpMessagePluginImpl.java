/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.mms.ipmessage;

import android.content.Context;
import android.content.ContextWrapper;


public class IpMessagePluginImpl extends ContextWrapper implements IIpMessagePlugin {

    protected ActivitiesManager mActivityManager = null;
    protected ChatManager mChatManager = null;
    protected ContactManager mContactManager = null;
    protected GroupManager mGroupManager = null;
    protected MessageManager mMessageManager = null;
    protected NotificationsManager mNotificationsManager = null;
    protected ServiceManager mServiceManager = null;
    protected SettingsManager mSettingsManager = null;
    protected ResourceManager mResourceManager = null;

    public IpMessagePluginImpl(Context context) {
        super(context);
    }

    /**
    * Get plugin activity manager.
    * @param the context
    * @return ActivitiesManager
    */
    public ActivitiesManager getActivitiesManager(Context context) {
        if (mActivityManager == null) {
            mActivityManager = new ActivitiesManager(context);
        }
        return mActivityManager;
    }

    /**
    * Get IP message chat manager.
    * @param the context
    * @return ChatManager
    */
    public ChatManager getChatManager(Context context) {
        if (mChatManager == null) {
            mChatManager = new ChatManager(context);
        }
        return mChatManager;
    }

    /**
    * Get IP message contact manager.
    * @param the context
    * @return ContactManager
    */
    public ContactManager getContactManager(Context context) {
        if (mContactManager == null) {
            mContactManager = new ContactManager(context);
        }
        return mContactManager;
    }

    /**
    * Get IP message group manager.
    * @param the context
    * @return GroupManager
    */
    public GroupManager getGroupManager(Context context) {
        if (mGroupManager == null) {
            mGroupManager = new GroupManager(context);
        }
        return mGroupManager;
    }

    /**
    * Get IP message manager.
    * @param the context
    * @return MessageManager
    */
    public MessageManager getMessageManager(Context context) {
        if (mMessageManager == null) {
            mMessageManager = new MessageManager(context);
        }
        return mMessageManager;
    }

    /**
    * Get IP message notifications manager.
    * @param the context
    * @return NotificationsManager
    */
    public NotificationsManager getNotificationsManager(Context context) {
        if (mNotificationsManager == null) {
            mNotificationsManager = new NotificationsManager(context);
        }
        return mNotificationsManager;
    }

    /**
    * Get IP message service manager.
    * @param the context
    * @return ServiceManager
    */
    public ServiceManager getServiceManager(Context context) {
        if (mServiceManager == null) {
            mServiceManager = new ServiceManager(context);
        }
        return mServiceManager;
    }

    /**
    * Get IP message settings manager.
    * @param the context
    * @return SettingsManager
    */
    public SettingsManager getSettingsManager(Context context) {
        if (mSettingsManager == null) {
            mSettingsManager = new SettingsManager(context);
        }
        return mSettingsManager;
    }

    /**
    * Get IP message resource manager.
    * @param the context
    * @return ResourceManager
    */
    public ResourceManager getResourceManager(Context context) {
        if (mResourceManager == null) {
            mResourceManager = new ResourceManager(context);
        }
        return mResourceManager;
    }

    /**
    * Check if this is an implemented plugin or default plugin.
    * @return boolean true for actual plugin and false for default
    */
    public boolean isActualPlugin() {
        return false;
    }
}

