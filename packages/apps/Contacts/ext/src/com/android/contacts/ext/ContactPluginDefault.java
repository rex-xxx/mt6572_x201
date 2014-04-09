package com.android.contacts.ext;

public class ContactPluginDefault implements IContactPlugin {
    public static final String COMMD_FOR_OP01 = "ExtensionForOP01";
    public static final String COMMD_FOR_AppGuideExt = "ExtensionForAppGuideExt";

    public CallDetailExtension createCallDetailExtension() {
        return new CallDetailExtension();
    }

    public CallListExtension createCallListExtension() {
        return new CallListExtension();
    }

    public ContactAccountExtension createContactAccountExtension() {
        return new ContactAccountExtension();
    }

    public ContactDetailExtension createContactDetailExtension() {
        return new ContactDetailExtension();
    }

    public ContactListExtension createContactListExtension() {
        return new ContactListExtension();
    }

    public DialPadExtension createDialPadExtension() {
        return new DialPadExtension();
    }

    public DialtactsExtension createDialtactsExtension() {
        return new DialtactsExtension();
    }

    public SimPickExtension createSimPickExtension() {
        return new SimPickExtension();
    }

    public SpeedDialExtension createSpeedDialExtension() {
        return new SpeedDialExtension();
    }

    public QuickContactExtension createQuickContactExtension() {
        return new QuickContactExtension();
    }
}
