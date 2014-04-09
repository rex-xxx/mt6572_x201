package com.mediatek.appguide.plugin.contacts;

import com.android.contacts.ext.ContactAccountExtension;
import com.android.contacts.ext.ContactPluginDefault;

public class ContactsPlugin extends ContactPluginDefault {

    public ContactAccountExtension createContactAccountExtension() {
        return new SwitchSimContactsExt(); 
    }

}
