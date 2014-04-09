package com.mediatek.contacts;

import android.content.Context;
import android.util.Log;

import com.android.contacts.ContactsApplication;
import com.android.contacts.ext.CallDetailExtension;
import com.android.contacts.ext.CallListExtension;
import com.android.contacts.ext.ContactAccountExtension;
import com.android.contacts.ext.ContactDetailExtension;
import com.android.contacts.ext.ContactListExtension;
import com.android.contacts.ext.DialPadExtension;
import com.android.contacts.ext.DialtactsExtension;
import com.android.contacts.ext.IContactPlugin;
import com.android.contacts.ext.QuickContactExtension;
import com.android.contacts.ext.SimPickExtension;
import com.android.contacts.ext.SpeedDialExtension;

import com.mediatek.contacts.extension.ContactPluginExtensionContainer;
import com.mediatek.pluginmanager.Plugin;
import com.mediatek.pluginmanager.Plugin.ObjectCreationException;
import com.mediatek.pluginmanager.PluginManager;


public class ExtensionManager {
    private static final String TAG = "ExtensionManager";

    private static ExtensionManager sInstance;

    public static final String RCS_CONTACT_PRESENCE_CHANGED = "android.intent.action.RCS_CONTACT_PRESENCE_CHANGED";
    public static final String COMMD_FOR_RCS = "ExtenstionForRCS";
    public static final String COMMD_FOR_AAS = "ExtensionForAAS";
    public static final String COMMD_FOR_SNE = "ExtensionForSNE";
    /**
     * Command and name for SNS plugin.
     */
    public static final String COMMD_FOR_SNS = "ExtensionForSNS";

    private boolean mHasPlugin = false;

    private ContactPluginExtensionContainer mContactPluinContainer;

    private ExtensionManager() {
        mContactPluinContainer = new ContactPluginExtensionContainer();
        mHasPlugin = false;
        getPlugin();
    }

    public static ExtensionManager getInstance() {
        if (sInstance == null) {
            sInstance = new ExtensionManager();
        }
        return sInstance;
    }

    private void getPlugin() {
        Context applicationContext = ContactsApplication.getInstance();
        Log.i(TAG, "getPlugin applicationContext : " + applicationContext);
        PluginManager<IContactPlugin> pm = PluginManager.<IContactPlugin> create(
                applicationContext, IContactPlugin.class.getName());
        Log.i(TAG, "get pluginManager");
        int num = pm.getPluginCount();
        if (num == 0) {
            Log.e(TAG, "no plugin apk");
            return;
        }
        Log.i(TAG, "num : " + num);
        try {
            for (int i = 0; i < num; i++) {
                Plugin<IContactPlugin> contactPlugin = pm.getPlugin(i);
                if (null != contactPlugin) {
                    mContactPluinContainer.addExtensions(contactPlugin.createObject());
                } else {
                    Log.e(TAG, "contactPlugin is null");
                }
            }
        } catch (ObjectCreationException e) {
            Log.e(TAG, "getPlugin is error " + e);
            e.printStackTrace();
        }
    }

    public CallDetailExtension getCallDetailExtension() {
        return mContactPluinContainer.getCallDetailExtension();
    }

    public CallListExtension getCallListExtension() {
        return mContactPluinContainer.getCallListExtension();
    }

    public ContactAccountExtension getContactAccountExtension() {
        return mContactPluinContainer.getContactAccountExtension();
    }

    public ContactDetailExtension getContactDetailExtension() {
        return mContactPluinContainer.getContactDetailExtension();
    }

    public ContactListExtension getContactListExtension() {
        return mContactPluinContainer.getContactListExtension();
    }

    public DialPadExtension getDialPadExtension() {
        return mContactPluinContainer.getDialPadExtension();
    }

    public DialtactsExtension getDialtactsExtension() {
        return mContactPluinContainer.getDialtactsExtension();
    }

    public SpeedDialExtension getSpeedDialExtension() {
        return mContactPluinContainer.getSpeedDialExtension();
    }

    public SimPickExtension getSimPickExtension() {
        return mContactPluinContainer.getSimPickExtension();
    }

    public QuickContactExtension getQuickContactExtension() {
        return mContactPluinContainer.getQuickContactExtension();
    }

    public boolean HasPlugin() {
        Log.i(TAG, "return mHasPlugin : " + mHasPlugin);
        return mHasPlugin;
    }

}
