package com.mediatek.contacts.list.service;

import android.accounts.Account;

public class MultiChoiceRequest {

    // Using to identify this item phone or SIM
    public long mIndicator;

    // Index in SIM for SIM contacts 
    public int mSimIndex;

    // Contacts Id in the database
    public int mContactId;

    // Contacts display name
    public String mContactName;

    // Account source
    public Account mAccountSrc;

    // Account destination
    public Account mAccountDst;

    // Target account
    public Account mTargetAccount;

    public MultiChoiceRequest(long indicator, int simIndex, int contactId, String displayName) {
        mIndicator = indicator;
        mSimIndex = simIndex;
        mContactId = contactId;
        mContactName = displayName;
    }

    public MultiChoiceRequest(long indicator, int simIndex, int contactId, String displayName, Account targetAccount) {
        mIndicator = indicator;
        mSimIndex = simIndex;
        mContactId = contactId;
        mContactName = displayName;
        mTargetAccount = targetAccount;
    }

    public MultiChoiceRequest(long indicator, int simIndex, int contactId, String displayName, Account source,
            Account destination) {
        mIndicator = indicator;
        mSimIndex = simIndex;
        mContactId = contactId;
        mContactName = displayName;
        mAccountSrc = source;
        mAccountDst = destination;
    }
}
