package com.android.contacts.ext;

public interface IContactPlugin {
    SimPickExtension createSimPickExtension();

    SpeedDialExtension createSpeedDialExtension();

    DialtactsExtension createDialtactsExtension();

    DialPadExtension createDialPadExtension();

    ContactListExtension createContactListExtension();

    ContactDetailExtension createContactDetailExtension();

    ContactAccountExtension createContactAccountExtension();

    CallListExtension createCallListExtension();

    CallDetailExtension createCallDetailExtension();

    QuickContactExtension createQuickContactExtension();

}
