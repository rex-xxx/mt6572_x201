package com.mediatek.phone.recording;

interface IPhoneRecordStateListener {
    void onStateChange(int state);
    void onError(int iError);
}
