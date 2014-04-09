package com.android.internal.telephony;

public interface ICellBroadcastFwkExt {
    void openEtwsChannel(EtwsNotification newEtwsNoti);
    
    void closeEtwsChannel(EtwsNotification newEtwsNoti);
    
    boolean containDuplicatedEtwsNotification(EtwsNotification newEtwsNoti);
}