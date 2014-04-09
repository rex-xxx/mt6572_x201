
package com.mediatek.contacts.plugin;

import com.android.contacts.ext.ContactPluginDefault;
import com.android.contacts.ext.SimPickExtension;


public class OP02ContactsPlugin extends ContactPluginDefault {
    public SimPickExtension createSimPickExtension() {
        return new OP02SimPickExtension();
    }


}
