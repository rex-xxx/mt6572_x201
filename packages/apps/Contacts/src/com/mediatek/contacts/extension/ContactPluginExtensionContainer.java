
package com.mediatek.contacts.extension;

import android.util.Log;

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

public class ContactPluginExtensionContainer {
    private static final String TAG = "ContactPluginExtensionContainer";

    private CallDetailExtensionContainer mCallDetailExtensionContainer;
    private CallListExtensionContainer mCallListExtensionContainer;
    private ContactAccountExtensionContainer mContactAccountExtensionContainer;
    private ContactDetailExtensionContainer mContactDetailExtensionContainer;
    private ContactListExtensionContainer mContactListExtensionContainer;
    private QuickContactExtensionContainer mQuickContactExtensionContainer;
    private DialPadExtensionContainer mDialPadExtensionContainer;
    private DialtactsExtensionContainer mDialtactsExtensionContainer;
    private SpeedDialExtensionContainer mSpeedDialExtensionContainer;
    private SimPickExtensionContainer mSimPickExtensionContainer;

    public ContactPluginExtensionContainer() {
        mCallDetailExtensionContainer = new CallDetailExtensionContainer();
        mCallListExtensionContainer = new CallListExtensionContainer();
        mContactAccountExtensionContainer = new ContactAccountExtensionContainer();
        mContactDetailExtensionContainer = new ContactDetailExtensionContainer();
        mContactListExtensionContainer = new ContactListExtensionContainer();
        mDialPadExtensionContainer = new DialPadExtensionContainer();
        mDialtactsExtensionContainer = new DialtactsExtensionContainer();
        mSpeedDialExtensionContainer = new SpeedDialExtensionContainer();
        mSimPickExtensionContainer = new SimPickExtensionContainer();
        mQuickContactExtensionContainer = new QuickContactExtensionContainer();
    }

    public CallDetailExtension getCallDetailExtension() {
        Log.i(TAG, "return CallDetailExtension ");
        return mCallDetailExtensionContainer;
    }

    public CallListExtension getCallListExtension() {
        Log.i(TAG, "return CallListExtension ");
        return mCallListExtensionContainer;
    }

    public ContactAccountExtension getContactAccountExtension() {
        Log.i(TAG, "return ContactAccountExtension " + mContactAccountExtensionContainer);
        return mContactAccountExtensionContainer;
    }

    public ContactDetailExtension getContactDetailExtension() {
        Log.i(TAG, "return ContactDetailExtension ");
        return mContactDetailExtensionContainer;
    }

    public ContactListExtension getContactListExtension() {
        Log.i(TAG, "return ContactListExtension ");
        return mContactListExtensionContainer;
    }

    public DialPadExtension getDialPadExtension() {
        Log.i(TAG, "return DialPadExtension ");
        return mDialPadExtensionContainer;
    }

    public DialtactsExtension getDialtactsExtension() {
        Log.i(TAG, "return DialtactsExtension ");
        return mDialtactsExtensionContainer;
    }

    public SpeedDialExtension getSpeedDialExtension() {
        Log.i(TAG, "return SpeedDialExtension ");
        return mSpeedDialExtensionContainer;
    }

    public SimPickExtension getSimPickExtension() {
        Log.i(TAG, "return SimPickExtension ");
        return mSimPickExtensionContainer;
    }

    public QuickContactExtension getQuickContactExtension() {
        Log.i(TAG, "return QuickContactExtension");
        return mQuickContactExtensionContainer;
    }

    public void addExtensions(IContactPlugin contactPlugin) {
        Log.i(TAG, "contactPlugin : " + contactPlugin);
        mCallDetailExtensionContainer.add(contactPlugin.createCallDetailExtension());
        mCallListExtensionContainer.add(contactPlugin.createCallListExtension());
        mContactAccountExtensionContainer.add(contactPlugin.createContactAccountExtension());
        mContactDetailExtensionContainer.add(contactPlugin.createContactDetailExtension());
        mContactListExtensionContainer.add(contactPlugin.createContactListExtension());
        mDialPadExtensionContainer.add(contactPlugin.createDialPadExtension());
        mDialtactsExtensionContainer.add(contactPlugin.createDialtactsExtension());
        mSpeedDialExtensionContainer.add(contactPlugin.createSpeedDialExtension());
        mSimPickExtensionContainer.add(contactPlugin.createSimPickExtension());
        mQuickContactExtensionContainer.add(contactPlugin.createQuickContactExtension());

    }

}
